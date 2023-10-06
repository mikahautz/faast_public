package at.ac.uibk.scheduler.storeless;

import at.ac.uibk.core.functions.objects.DataIns;
import at.ac.uibk.core.functions.objects.DataOutsAtomic;
import at.ac.uibk.core.functions.objects.PropertyConstraint;
import at.ac.uibk.metadata.api.model.DataTransfer;
import at.ac.uibk.metadata.api.model.DetailedProvider;
import at.ac.uibk.metadata.api.model.FunctionType;
import at.ac.uibk.metadata.api.model.Region;
import at.ac.uibk.metadata.api.model.enums.Provider;
import at.ac.uibk.metadata.api.model.functions.FunctionDeployment;
import at.ac.uibk.scheduler.api.Communication;
import at.ac.uibk.scheduler.api.MetadataCache;
import at.ac.uibk.scheduler.api.SchedulingAlgorithm;
import at.ac.uibk.scheduler.api.SchedulingException;
import at.ac.uibk.scheduler.api.node.AtomicFunctionNode;
import at.ac.uibk.scheduler.api.node.FunctionNode;
import at.ac.uibk.scheduler.faast.PlannedExecution;
import at.ac.uibk.scheduler.faast.RegionConcurrencyChecker;
import at.ac.uibk.util.DecisionLogger;
import at.ac.uibk.util.HeftUtil;
import at.ac.uibk.util.LogEntry;
import org.jgrapht.alg.util.Pair;
import org.jgrapht.alg.util.Triple;
import org.jgrapht.graph.DefaultDirectedWeightedGraph;

import java.util.*;
import java.util.function.Supplier;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class StoreLess implements SchedulingAlgorithm {

    private static final boolean EXTENDED_LOGGING = false;

    private static final boolean TIME_LOGGING = true;

    public StoreLess() {
        bRankMap = new HashMap<>();
        regionConcurrencyChecker = new RegionConcurrencyChecker();
        functionDeploymentsByFunctionType = MetadataCache.get().getDeploymentsByFunctionType();
        scaledResourcesByRegion = initializeRegionResources();
    }

    private final Map<FunctionNode, Double> bRankMap;
    private final Map<FunctionType, Set<FunctionDeployment>> functionDeploymentsByFunctionType;
    private final Map<Region, List<RegionResource>> scaledResourcesByRegion;
    private final RegionConcurrencyChecker regionConcurrencyChecker;

    private Map<Region, List<RegionResource>> initializeRegionResources() {
        Map<Region, List<RegionResource>> map = new HashMap<>();
        for (Region r : MetadataCache.get().getRegions()) {
            map.put(r, getAllScaledByMaxConcurrencyFor(r));
        }
        return map;
    }

    private static List<RegionResource> getAllScaledByMaxConcurrencyFor(Region region) {
        return MetadataCache.get().getDetailedProviderFor(region)
                .map(DetailedProvider::getMaxConcurrency)
                .map(maxConcurrency ->
                        IntStream.range(0, maxConcurrency)
                                .mapToObj(unused -> new RegionResource(region)))
                .orElse(Stream.empty())
                .collect(Collectors.toList());
    }

    @Override
    public void schedule(final DefaultDirectedWeightedGraph<FunctionNode, Communication> graph) {

        List<LogEntry> logEntries = new ArrayList<>();
        DecisionLogger decisionLogger = null;
        if (StoreLess.EXTENDED_LOGGING) {
            decisionLogger = new DecisionLogger(Logger.getLogger(Logger.GLOBAL_LOGGER_NAME));
        }

        HeftUtil.initializeBRankMap(this, graph, this.bRankMap);

        final Iterator<AtomicFunctionNode> bRankIterator = HeftUtil.getBRankIterator(this.bRankMap);

        double maxEft = 0;

        // for the first function that is executed on AWS, a session overhead has to be added
        boolean useAwsSessionOverhead = true;
        int awsSessionOverheadValue = MetadataCache.get().getDetailedProviders().stream()
                .filter(provider -> provider.getName().equals("AWS"))
                .findFirst()
                .get()
                .getSessionOverheadms();
        // the threshold in ms that indicates how much time difference in the EST there may be for other functions
        // to still have the SO added
        int sessionOverheadThreshold = 300;
        // stores the EST and the function node for which the SO is needed
        Map<AtomicFunctionNode, Double> usedAwsEst = new HashMap<>();
        // stores the function node, the EST and EFT as a pair as a backup for the SO if multiple nodes have the same b-rank
        Map<AtomicFunctionNode, Pair<Double, Double>> backupAwsFunctionForSessionOverhead = new HashMap<>();

        //schedule each function on the fastest resource, w/o considering concurrency limit
        while (bRankIterator.hasNext()) {

            final AtomicFunctionNode toSchedule = bRankIterator.next();

            // if the flag is null, then all functions with the same b-rank have been checked and none of the others
            // are deployed to AWS, meaning we have to add the SO to the backup function
            if (!backupAwsFunctionForSessionOverhead.isEmpty() && toSchedule.forceSessionOverhead() == null) {
                useAwsSessionOverhead = false;
                maxEft = useBackupFunctionForSessionOverhead(backupAwsFunctionForSessionOverhead, usedAwsEst, maxEft,
                        awsSessionOverheadValue, logEntries);
            }

            final String functionTypeName = toSchedule.getAtomicFunction().getType();
            final FunctionType functionType = MetadataCache.get().getFunctionTypesByName().get(functionTypeName);

            List<DataOutsAtomic> originalDataOuts = null;
            if (toSchedule.getAtomicFunction().getDataOuts() != null) {
                originalDataOuts = toSchedule.getAtomicFunction().getDataOuts().stream()
                        .map(DataOutsAtomic::new)
                        .collect(Collectors.toList());
            }

            double minEst = Double.MAX_VALUE;
            double minEft = Double.MAX_VALUE;
            double finalRTT = 0;
            double finalDownloadTime = 0;
            double finalUploadTime = 0;
            boolean addedSessionOverhead = false;

            final Supplier<SchedulingException> noScheduleFoundException =
                    () -> new SchedulingException("cannot schedule function for name " + toSchedule.getAtomicFunction().getName());

            RegionResource schedulingDecision = null;
            FunctionDeployment scheduledFunctionDeployment = null;
            List<DataIns> scheduledDataIns = null;
            Map<DataIns, DataTransfer> scheduledDataUpload = null;
            List<DataIns> toUploadInputs = null;
            Map<AtomicFunctionNode, Double> functionsWithSameBRank = new HashMap<>();
            boolean earlierFunctionEstExists = false;
            double earlierFunctionEst = Double.MAX_VALUE;

            for (final FunctionDeployment fd : functionDeploymentsByFunctionType.get(functionType)) {
                final Region region = MetadataCache.get().getRegionFor(fd).orElseThrow();
                RegionResource resource = getBestRegionResource(region);

                // no suitable resource could be found for this deployment
                if (resource == null) {
                    continue;
                }

                List<DataIns> dataIns = toSchedule.getAtomicFunction().getDataIns();
                List<DataIns> toDownloadInputs = extractDownloadDataIns(dataIns);
                toUploadInputs = extractUploadDataIns(dataIns);

                // check if any data needs to be down- or uploaded, and if the function region exists in the data transfer entries
                if ((!toDownloadInputs.isEmpty() || !toUploadInputs.isEmpty()) && !dataTransferEntryExistsFor(fd)) {
                    continue;
                }

                Map<DataIns, DataTransfer> bestOptions = new HashMap<>();
                double downloadTime = calculateDownloadTime(toDownloadInputs, fd.getRegionId());
                double uploadTime = calculateUploadTime(toUploadInputs, bestOptions, fd.getRegionId());

                double RTT = fd.getAvgRTT() + downloadTime + uploadTime;

                final double currentEst = HeftUtil.calculateEarliestStartTimeOnResource(resource, graph, toSchedule,
                        fd, RTT, this.regionConcurrencyChecker);

                // stores all function nodes and their EST that have the same b-rank as the current node
                Map<AtomicFunctionNode, Double> functionsEst = new HashMap<>();
                Triple<Integer, Boolean, Double> sessionResult = handleSessionOverhead(toSchedule, region, resource, fd,
                        this.bRankMap, functionsEst, usedAwsEst, graph, currentEst, awsSessionOverheadValue,
                        sessionOverheadThreshold, useAwsSessionOverhead);

                RTT += sessionResult.getFirst();
                // specifies if there exists a function node with the same b-rank, but with an earlier EST than the current
                boolean earlierEstExists = sessionResult.getSecond();
                // stores the earliest EST of all nodes with the same b-rank
                double tmpMinEst = sessionResult.getThird();

                final double currentEft = currentEst + RTT;

                if (decisionLogger != null) {
                    decisionLogger.saveEntry(fd.getKmsArn(), currentEst, currentEft);
                }

                if (schedulingDecision == null || currentEft < minEft) {
                    schedulingDecision = resource;
                    scheduledFunctionDeployment = fd;
                    minEst = currentEst;
                    minEft = currentEft;

                    finalRTT = RTT;
                    finalDownloadTime = downloadTime;
                    finalUploadTime = uploadTime;

                    scheduledDataUpload = bestOptions;
                    scheduledDataIns = dataIns;
                    addedSessionOverhead = sessionResult.getFirst() != 0;
                    functionsWithSameBRank = functionsEst;
                    earlierFunctionEstExists = earlierEstExists;
                    earlierFunctionEst = tmpMinEst;
                }
            }

            if (minEst >= Double.MAX_VALUE) {
                throw noScheduleFoundException.get();
            }

            if (decisionLogger != null) {
                decisionLogger.log(scheduledFunctionDeployment.getKmsArn(), toSchedule);
            }

            // set the value of the dataIns to the storage bucket url for the identified region
            scheduledDataUpload.forEach((dataIn, dataTransfer) -> {
                if (dataIn.getValue() == null || dataIn.getValue().isEmpty()) {
                    dataIn.setValue(buildStorageBucketUrl(MetadataCache.get().getRegionsById()
                            .get(dataTransfer.getStorageRegionID().intValue())
                    ));
                }
            });

            List<DataOutsAtomic> uploadDataOuts = null;
            if (toSchedule.getAtomicFunction().getDataOuts() != null) {
                uploadDataOuts = toSchedule.getAtomicFunction().getDataOuts().stream()
                        .filter(this::hasUploadPropertySet)
                        .collect(Collectors.toList());
            }

            if (uploadDataOuts != null && !uploadDataOuts.isEmpty()) {
                DataFlowStore.updateValuesInStore(toSchedule.getAtomicFunction().getName(), toUploadInputs,
                        uploadDataOuts, toUploadInputs.size() == 1);
            }

            schedulingDecision.getPlannedExecutions().add(new PlannedExecution(minEst, minEft));
            toSchedule.setSchedulingDecision(scheduledFunctionDeployment);
            toSchedule.setScheduledDataIns(scheduledDataIns);
            toSchedule.setAlgorithmInfo(new PlannedExecution(minEst, minEft));
            // write back the original non-modified dataOuts
            toSchedule.getAtomicFunction().setDataOuts(originalDataOuts);

            // if the SO was added
            if (addedSessionOverhead) {
                useAwsSessionOverhead = false;
                // if a function is started within 300ms, the SO is still applied
                for (Map.Entry<AtomicFunctionNode, Double> entry : functionsWithSameBRank.entrySet()) {
                    entry.getKey().setForceSessionOverhead(Math.abs(minEst - entry.getValue()) <= sessionOverheadThreshold);
                }
                usedAwsEst.put(toSchedule, minEst);
            }

            // if other functions with the same b-rank have an earlier EST than the current, store the current as a backup
            // this is needed if the others are not using AWS, then the backup function will get the SO added
            if (earlierFunctionEstExists) {
                backupAwsFunctionForSessionOverhead.put(toSchedule, new Pair<>(minEst, minEft));

                // set the flag which function node needs to set the SO
                for (Map.Entry<AtomicFunctionNode, Double> entry : functionsWithSameBRank.entrySet()) {
                    entry.getKey().setForceSessionOverhead(Math.abs(earlierFunctionEst - entry.getValue()) <= sessionOverheadThreshold);
                }
            }

            final Region region = MetadataCache.get().getRegionFor(scheduledFunctionDeployment).orElseThrow();
            this.regionConcurrencyChecker.scheduleFunction(region, minEst, minEft);

            maxEft = Math.max(maxEft, minEft);

            // if a function that has the same b-rank (=has the flag set) and the AWS SO was added, we can remove
            // the backup function from the map
            if (!backupAwsFunctionForSessionOverhead.isEmpty() && toSchedule.forceSessionOverhead() != null
                    && toSchedule.forceSessionOverhead() && addedSessionOverhead) {
                backupAwsFunctionForSessionOverhead.clear();
            }

            if (TIME_LOGGING) {
                logEntries.add(new LogEntry(toSchedule, finalRTT, finalDownloadTime, finalUploadTime));
            }
        }

        if (TIME_LOGGING) {
            for (LogEntry entry : logEntries) {
                System.out.printf("Function %s: %.2fms (Computation time: %.2fms, DL: %.2fms, UP: %.2fms)%n",
                        entry.getFunctionNode().getAtomicFunction().getName(), entry.getRTT(),
                        entry.getRTT() - entry.getDownloadTime() - entry.getUploadTime(), entry.getDownloadTime(), entry.getUploadTime());
            }
        }

        System.out.println("Calculated makespan:" + maxEft);

        if (decisionLogger != null) {
            decisionLogger.getLogger().info("Calculated makespan: " + maxEft);
        }

    }

    /**
     * Handles the session overhead for the function node and returns a triple, consisting of the session overhead, a
     * boolean flag that indicates if another function with the same b-rank starts earlier than this one, and the
     * earliest start time of the functions that have the same b-rank as the passed function node.
     *
     * @param toSchedule               the function node to perform the actions for
     * @param region                   the region of the current deployment
     * @param resource                 the region resource to schedule the function on
     * @param fd                       the current function deployment
     * @param bRankMap                 the b-rank map of the workflow
     * @param functionsEst             the map storing all function nodes and their EST that have the same b-rank
     * @param usedAwsEst               the map storing the function node and EST that has the SO set
     * @param graph                    the graph of the workflow
     * @param currentEst               the current EST of the function node
     * @param awsSessionOverheadValue  the value for the SO
     * @param sessionOverheadThreshold the value for the SO threshold
     * @param useSO                    the flag indicating if SO should be added
     *
     * @return a triple containing the SO, a boolean flag if an earlier EST exists, and the min EST
     */
    private Triple<Integer, Boolean, Double> handleSessionOverhead(AtomicFunctionNode toSchedule,
                                                                   Region region,
                                                                   RegionResource resource,
                                                                   FunctionDeployment fd,
                                                                   Map<FunctionNode, Double> bRankMap,
                                                                   Map<AtomicFunctionNode, Double> functionsEst,
                                                                   Map<AtomicFunctionNode, Double> usedAwsEst,
                                                                   DefaultDirectedWeightedGraph<FunctionNode, Communication> graph,
                                                                   double currentEst,
                                                                   int awsSessionOverheadValue,
                                                                   int sessionOverheadThreshold,
                                                                   boolean useSO) {
        int sessionOverhead = 0;
        boolean earlierEstExists = false;
        double tmpMinEst = Double.MAX_VALUE;

        if (toSchedule.forceSessionOverhead() == null && useSO && region.getProvider() == Provider.AWS) {
            // check if there are any functions with the same b-rank
            double bRank = this.bRankMap.get(toSchedule);
            List<FunctionNode> fNodes = this.bRankMap.entrySet()
                    .stream()
                    .filter(entry -> entry.getValue().equals(bRank) && !entry.getKey().equals(toSchedule))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

            // if there are functions with the same b-rank, put those functions and their EST into a map and
            // store the smallest found EST
            if (!fNodes.isEmpty()) {
                for (FunctionNode f : fNodes) {
                    if (f instanceof AtomicFunctionNode) {
                        double fAvgTime = calculateAverageTime(((AtomicFunctionNode) f));
                        // TODO fd is currently the same as the current function since we do not know which one will be chosen
                        double fEst = HeftUtil.calculateEarliestStartTimeOnResource(resource, graph, ((AtomicFunctionNode) f), fd,
                                fAvgTime, this.regionConcurrencyChecker);
                        functionsEst.put(((AtomicFunctionNode) f), fEst);
                        if (fEst < tmpMinEst) {
                            tmpMinEst = fEst;
                        }
                    }
                }

                // if the current EST is with 300ms of the smallest EST, add SO as well
                if (Math.abs(currentEst - tmpMinEst) <= sessionOverheadThreshold) {
                    sessionOverhead = awsSessionOverheadValue;
                } else {
                    // otherwise, set the flag to signal that there is another function with the same b-rank
                    // that has an earlier EST
                    earlierEstExists = true;
                }
            } else {
                // if no other functions with the same b-rank exist, add the SO
                sessionOverhead = awsSessionOverheadValue;
            }
        } else if (toSchedule.forceSessionOverhead() != null && toSchedule.forceSessionOverhead()
                && region.getProvider() == Provider.AWS) {
            // if the flag is set due to having the same b-rank as another function and an earlier EST, add the SO
            sessionOverhead = awsSessionOverheadValue;
        } else if (toSchedule.forceSessionOverhead() == null && region.getProvider() == Provider.AWS
                && !usedAwsEst.isEmpty()) {
            // if the SO was already set for a function before, check if the current EST is within 300ms
            // of the function that has the SO set, if it does then set the SO for this function as well
            double minMapEst = usedAwsEst.values().stream()
                    .min(Double::compareTo)
                    .orElse(0D);

            if (Math.abs(currentEst - minMapEst) <= sessionOverheadThreshold) {
                sessionOverhead = awsSessionOverheadValue;
            }
        }

        return new Triple<>(sessionOverhead, earlierEstExists, tmpMinEst);
    }

    /**
     * Sets the SO to the backup function node, stores the new EST and clears the backup map.
     *
     * @param backup          the map containing the backup function node, and a pair of EST and EFT
     * @param usedAwsEst      the map storing the function node that has the SO set and its EST
     * @param maxEft          the current EFT of the workflow
     * @param sessionOverhead the value for the session overhead
     * @param logEntries      the list containing all log entries that will be updated with the RTT + SO
     *
     * @return the new max EFT of the workflow
     */
    private double useBackupFunctionForSessionOverhead(Map<AtomicFunctionNode, Pair<Double, Double>> backup,
                                                       Map<AtomicFunctionNode, Double> usedAwsEst, double maxEft,
                                                       int sessionOverhead, List<LogEntry> logEntries) {
        AtomicFunctionNode backupFunction = backup.keySet().stream().findFirst().get();
        Pair<Double, Double> timePair = backup.values().stream().findFirst().get();
        // store the new EST and the function node that has the SO set
        usedAwsEst.put(backupFunction, timePair.getFirst());
        // update the log entry to the new RTT that contains the SO
        Optional<LogEntry> logEntry = logEntries.stream()
                .filter(node -> node.getFunctionNode().getAtomicFunction().getName().equals(
                        backupFunction.getAtomicFunction().getName()))
                .findFirst();
        logEntry.ifPresent(entry -> entry.addToRTT(sessionOverhead));
        backup.clear();
        // return the new max EFT of the workflow
        return Math.max(maxEft, timePair.getSecond() + sessionOverhead);
    }

    /**
     * Calculates the download time for the specified inputs.
     *
     * @param toDownloadInputs list of dataIns to get the files to download
     * @param fdRegionId       the region of the function deployment
     *
     * @return the calculated download time
     */
    private double calculateDownloadTime(List<DataIns> toDownloadInputs, Long fdRegionId) {
        double downloadTime = 0;

        for (DataIns dataIn : toDownloadInputs) {
            List<String> urls = null;
            List<Integer> fileAmounts = HeftUtil.extractFileAmount(dataIn, true);
            List<Double> fileSizes = fileSizes = HeftUtil.extractFileSize(dataIn, true);

            if (dataIn.getValue() != null && !dataIn.getValue().isEmpty()) {
                urls = List.of(dataIn.getValue());
            } else {
                Triple<List<String>, List<Integer>, List<Double>> result = DataFlowStore.getDataInValue(dataIn.getSource(), false);
                urls = result.getFirst();
                // only set the stored values if they were not explicitly given in the yaml file
                if (fileAmounts == null) fileAmounts = result.getSecond();
                if (fileSizes == null) fileSizes = result.getThird();
            }

            if (urls.size() != fileAmounts.size() || urls.size() != fileSizes.size()) {
                throw new SchedulingException("Amount of storage urls does not match with the amount of specified fileamounts or filesizes!");
            }

            downloadTime += DataTransferTimeModel.calculateDownloadTime(fdRegionId, urls, fileAmounts, fileSizes);
        }

        return downloadTime;
    }

    /**
     * Calculates the download time for the specified inputs.
     *
     * @param toUploadInputs list of dataIns to get the files to upload
     * @param bestOptions    a map storing the best option for the dataIns to upload and its data transfer entry
     * @param fdRegionId     the region of the function deployment
     *
     * @return the calculated upload time
     */
    private double calculateUploadTime(List<DataIns> toUploadInputs, Map<DataIns, DataTransfer> bestOptions, Long fdRegionId) {
        // get all data transfer entries for the upload that have the current function region
        List<DataTransfer> uploadDataTransfers = MetadataCache.get().getDataTransfersUpload().stream()
                .filter(entry -> Objects.equals(entry.getFunctionRegionID(), fdRegionId))
                .collect(Collectors.toList());

        double uploadTime = 0;

        for (DataIns dataIn : toUploadInputs) {
            double upMin = Double.MAX_VALUE;

            // if an output bucket is specified to be used explicitly, use that one
            if (dataIn.getValue() != null && !dataIn.getValue().isEmpty()) {
                Long regionId = DataTransferTimeModel.getRegionId(dataIn.getValue());
                DataTransfer dataTransfer = uploadDataTransfers.stream()
                        .filter(entry -> Objects.equals(entry.getStorageRegionID(), regionId))
                        .findFirst()
                        .orElseThrow(() -> new SchedulingException("No Data Transfer entry exists for the region of given bucket: " + dataIn.getValue()));

                List<Integer> fileAmount = HeftUtil.extractFileAmount(dataIn, false);
                List<Double> fileSize = HeftUtil.extractFileSize(dataIn, false);
                upMin = DataTransferTimeModel.calculateUploadTime(dataTransfer, fileAmount.get(0), fileSize.get(0));
                bestOptions.put(dataIn, dataTransfer);
            } else {
                // loop through storages
                for (DataTransfer dataTransfer : uploadDataTransfers) {
                    List<Integer> fileAmount = HeftUtil.extractFileAmount(dataIn, false);
                    List<Double> fileSize = HeftUtil.extractFileSize(dataIn, false);
                    // for the upload, only a single number for the fileamount and filesize may be given,
                    // that's why we can simply take the first element of the list
                    double upTime = DataTransferTimeModel.calculateUploadTime(dataTransfer, fileAmount.get(0), fileSize.get(0));

                    if (upTime < upMin) {
                        upMin = upTime;
                        bestOptions.put(dataIn, dataTransfer);
                    }
                }
            }

            uploadTime += upMin;
        }

        return uploadTime;
    }


    /**
     * Checks if the given function deployment is in a region for which an entry in the data transfer entries exists.
     *
     * @param fd to check the region for
     *
     * @return true if it exists, false otherwise
     */
    private boolean dataTransferEntryExistsFor(FunctionDeployment fd) {
        return MetadataCache.get().getDataTransfers().stream()
                .anyMatch(entry -> Objects.equals(entry.getFunctionRegionID(), fd.getRegionId()));
    }

    /**
     * Builds the storage bucket url based on the given region in the format 's3|gs://[prefix][region-name][suffix]/'
     *
     * @param region to build the url from
     *
     * @return a string containing the built storage bucket url
     */
    private String buildStorageBucketUrl(Region region) {
        String url = "";
        if (region.getProvider() == Provider.AWS) {
            url = "s3://";
        } else if (region.getProvider() == Provider.Google) {
            url = "gs://";
        } else {
            throw new SchedulingException("Currently only AWS and GCP are supported!");
        }

        String prefix = DataFlowStore.getBucketPrefix();
        String suffix = DataFlowStore.getBucketSuffix();

        if (prefix != null && !prefix.isEmpty()) {
            url += prefix;
        }
        url += region.getRegion();
        if (suffix != null && !suffix.isEmpty()) {
            url += suffix;
        }
        // TODO organize with subfolders like /tmp/ and /output/?
        url += "/";

        return url;
    }

    /**
     * Get all dataIns that have the {@code datatransfer} property set to {@code download}
     *
     * @param dataIns the list of dataIns to check
     *
     * @return a list of dataIns that should be downloaded
     */
    private List<DataIns> extractDownloadDataIns(List<DataIns> dataIns) {
        List<DataIns> dlDataIns = new ArrayList<>();
        for (DataIns dataIn : dataIns) {
            if (hasDownloadPropertySet(dataIn)) {
                dlDataIns.add(dataIn);
            }
        }
        return dlDataIns;
    }

    /**
     * Get all dataIns that have the {@code datatransfer} property set to {@code upload}
     *
     * @param dataIns the list of dataIns to check
     *
     * @return a list of dataIns that should be uploaded
     */
    private List<DataIns> extractUploadDataIns(List<DataIns> dataIns) {
        List<DataIns> upDataIns = new ArrayList<>();
        for (DataIns dataIn : dataIns) {
            if (hasUploadPropertySet(dataIn)) {
                upDataIns.add(dataIn);
            }
        }
        return upDataIns;
    }

    /**
     * Checks if the {@code dataIn} has the property {@code datatransfer} set to {@code download}.
     *
     * @param dataIn to check
     *
     * @return true if it has it set, false otherwise
     */
    private boolean hasDownloadPropertySet(DataIns dataIn) {
        return hasPropertySet(dataIn.getProperties(), "datatransfer", "download");
    }

    /**
     * Checks if the {@code dataIn} has the property {@code datatransfer} set to {@code upload}.
     *
     * @param dataIn to check
     *
     * @return true if it has it set, false otherwise
     */
    private boolean hasUploadPropertySet(DataIns dataIn) {
        return hasPropertySet(dataIn.getProperties(), "datatransfer", "upload");
    }

    /**
     * Checks if the {@code dataOut} has the property {@code datatransfer} set to {@code upload}.
     *
     * @param dataOut to check
     *
     * @return true if it has it set, false otherwise
     */
    private boolean hasUploadPropertySet(DataOutsAtomic dataOut) {
        return hasPropertySet(dataOut.getProperties(), "datatransfer", "upload");
    }

    /**
     * Checks if the {@code dataIn} has the property {@code propertyName} set to {@code propertyValue}.
     *
     * @param properties    the list of properties to check
     * @param propertyName  the name of the property
     * @param propertyValue the value of the property
     *
     * @return true if it has it set, false otherwise
     */
    private boolean hasPropertySet(List<PropertyConstraint> properties, String propertyName, String propertyValue) {
        if (properties != null && !properties.isEmpty()) {
            for (PropertyConstraint property : properties) {
                if (property.getName().equalsIgnoreCase(propertyName) &&
                        property.getValue().equalsIgnoreCase(propertyValue)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Finds the most suitable {@link RegionResource} for a given {@link Region}, that has the lowest starting time.
     *
     * @param region to check for
     *
     * @return the resource with the lowest starting time
     */
    private RegionResource getBestRegionResource(Region region) {
        final Set<RegionResource> resources = new HashSet<>(scaledResourcesByRegion.get(region));
        RegionResource resource = null;
        double earliestStartTime = Double.MAX_VALUE;

        for (RegionResource r : resources) {
            double latestEndTime = r.getLatestEndTime();

            if (latestEndTime < earliestStartTime) {
                earliestStartTime = latestEndTime;
                resource = r;
            }
        }

        return resource;
    }

    @Override
    public double calculateAverageTime(final AtomicFunctionNode node) {

        final String atomicFunctionTypeName = node.getAtomicFunction().getType();

        final FunctionType typeForFunction = MetadataCache.get().getFunctionTypesByName().get(atomicFunctionTypeName);

        if (typeForFunction == null) {
            throw new SchedulingException("Unknown Functiontype");
        }

        final Set<FunctionDeployment> functionDeployments = functionDeploymentsByFunctionType.get(typeForFunction);

        if (functionDeployments.isEmpty()) {
            return Double.MAX_VALUE;
        }

        return functionDeployments
                .stream()
                .mapToDouble(FunctionDeployment::getAvgRTT)
                .sum() / functionDeployments.size();
    }

}
