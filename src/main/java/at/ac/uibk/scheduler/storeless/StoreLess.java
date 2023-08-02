package at.ac.uibk.scheduler.storeless;

import at.ac.uibk.core.functions.objects.DataIns;
import at.ac.uibk.core.functions.objects.PropertyConstraint;
import at.ac.uibk.metadata.api.model.DetailedProvider;
import at.ac.uibk.metadata.api.model.FunctionType;
import at.ac.uibk.metadata.api.model.Region;
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
import org.jgrapht.graph.DefaultDirectedWeightedGraph;

import java.util.*;
import java.util.function.Supplier;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class StoreLess implements SchedulingAlgorithm {

    private static final boolean EXTENDED_LOGGING = false;

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

        DecisionLogger decisionLogger = null;
        if (StoreLess.EXTENDED_LOGGING) {
            decisionLogger = new DecisionLogger(Logger.getLogger(Logger.GLOBAL_LOGGER_NAME));
        }

        HeftUtil.initializeBRankMap(this, graph, this.bRankMap);

        final Iterator<AtomicFunctionNode> bRankIterator = HeftUtil.getBRankIterator(this.bRankMap);

        double maxEft = 0;

        //schedule each function on the fastest resource, w/o considering concurrency limit
        while (bRankIterator.hasNext()) {

            final AtomicFunctionNode toSchedule = bRankIterator.next();

            final String functionTypeName = toSchedule.getAtomicFunction().getType();

            final FunctionType functionType = MetadataCache.get().getFunctionTypesByName().get(functionTypeName);

            double minEst = Double.MAX_VALUE;
            double minEft = Double.MAX_VALUE;

            final Supplier<SchedulingException> noScheduleFoundException =
                    () -> new SchedulingException("cannot schedule function for name " + toSchedule.getAtomicFunction().getName());

            RegionResource schedulingDecision = null;
            FunctionDeployment scheduledFunctionDeployment = null;
            List<DataIns> scheduledDataIns = null;

            for (final FunctionDeployment fd : functionDeploymentsByFunctionType.get(functionType)) {
                final Region region = MetadataCache.get().getRegionFor(fd).orElseThrow();
                RegionResource resource = getBestRegionResource(region);

                // no suitable resource could be found for this deployment
                if (resource == null) {
                    continue;
                }

                List<DataIns> dataIns = toSchedule.getAtomicFunction().getDataIns();
                List<DataIns> toDownloadInputs = extractDownloadDataIns(dataIns);

                Double downloadTime = 0D;
                for (DataIns dataIn : toDownloadInputs) {
                    int fileAmount = extractFileAmount(dataIn);
                    double fileSize = extractFileSize(dataIn);

                    String url = null;

                    if (dataIn.getValue() != null && !dataIn.getValue().isEmpty()) {
                        url = dataIn.getValue();
                    } else {
                        url = DataFlowStore.getDataInValue(dataIn.getSource(), false);
                    }

                    downloadTime += DataTransferTimeModel.calculateDownloadTime(fd.getRegionId(), url, fileAmount, fileSize);
                }

                List<DataIns> toUploadInputs = extractUploadDataIns(dataIns); // list of dataIns that specify where the output should be stored
                Double uploadTime = 0D;
                // TODO loop through storage regions, calculate in each iteration the upload time
                // perform all of the steps below within the loop, so compare in each iteration if it is the min for the current fd
                // at the end of the loop we have to compare the intermediate result of the fd + DTT with the fastest

                for (DataIns dataIn : toUploadInputs) {
                    Double upMin = Double.MAX_VALUE;
                    // loop through storages
                    for (int i = 0; i < 2; i++) {
                        // get region
                        // calculate upload time
                        // check if upload time is < than output_min
                        // if yes, update time and set element of toLookAtOutput with new value and write back to list
                        dataIn.setValue("TODO-INSERT-URL-HERE");
                    }
                    // upload time += output_min
                }

                double RTT = fd.getAvgRTT() + downloadTime + uploadTime;

                final double currentEst = HeftUtil.calculateEarliestStartTimeOnResource(resource, graph, toSchedule,
                        fd, RTT, this.regionConcurrencyChecker);
                final double currentEft = currentEst + RTT;


                if (decisionLogger != null) {
                    decisionLogger.saveEntry(fd.getKmsArn(), currentEst, currentEft);
                }

                if (schedulingDecision == null || currentEft < minEft) {
                    schedulingDecision = resource;
                    scheduledFunctionDeployment = fd;
                    minEst = currentEst;
                    minEft = currentEft;
                    scheduledDataIns = dataIns;
                    DataFlowStore.updateValuesInStore(toSchedule.getAtomicFunction().getName(), toUploadInputs,
                            toSchedule.getAtomicFunction().getDataOuts(), toUploadInputs.size() == 1);
                }
            }

            if (minEst >= Double.MAX_VALUE) {
                throw noScheduleFoundException.get();
            }

            if (decisionLogger != null) {
                decisionLogger.log(scheduledFunctionDeployment.getKmsArn(), toSchedule);
            }

            schedulingDecision.getPlannedExecutions().add(new PlannedExecution(minEst, minEft));
            toSchedule.setSchedulingDecision(scheduledFunctionDeployment);
            toSchedule.setScheduledDataIns(scheduledDataIns);
            toSchedule.setAlgorithmInfo(new PlannedExecution(minEst, minEft));

            final Region region = MetadataCache.get().getRegionFor(scheduledFunctionDeployment).orElseThrow();
            this.regionConcurrencyChecker.scheduleFunction(region, minEst, minEft);

            maxEft = Math.max(maxEft, minEft);
        }

        System.out.println("Calculated makespan:" + maxEft);

        if (decisionLogger != null) {
            decisionLogger.getLogger().info("Calculated makespan: " + maxEft);
        }

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
     * Extracts the value for the {@code fileamount} property of a {@link DataIns}.
     *
     * @param dataIn to extract the property
     *
     * @return the extracted number
     */
    private int extractFileAmount(DataIns dataIn) {
        for (PropertyConstraint property : dataIn.getProperties()) {
            if (property.getName().equalsIgnoreCase("fileamount")) {
                try {
                    return Integer.parseInt(property.getValue());
                } catch (NumberFormatException e) {
                    throw new SchedulingException(dataIn.getName() + ": Property 'fileamount' has to be an Integer!");
                }
            }
        }
        throw new SchedulingException(dataIn.getName() + ": Property 'fileamount' is missing!");
    }

    /**
     * Extracts the value for the {@code filesize} property of a {@link DataIns}.
     *
     * @param dataIn to extract the property
     *
     * @return the extracted number
     */
    private double extractFileSize(DataIns dataIn) {
        for (PropertyConstraint property : dataIn.getProperties()) {
            if (property.getName().equalsIgnoreCase("filesize")) {
                String value = property.getValue();
                value = value.replace(",", ".");
                try {
                    return Double.parseDouble(value);
                } catch (NumberFormatException e) {
                    throw new SchedulingException("Property 'filesize' has to be a Double!");
                }
            }
        }
        throw new SchedulingException(dataIn.getName() + ": Property 'filesize' is missing!");
    }

    /**
     * Checks if the {@code dataIn} has the property {@code datatransfer} set to {@code download}.
     *
     * @param dataIn to check
     *
     * @return true if it has it set, false otherwise
     */
    private boolean hasDownloadPropertySet(DataIns dataIn) {
        return hasPropertySet(dataIn, "datatransfer", "download");
    }

    /**
     * Checks if the {@code dataIn} has the property {@code datatransfer} set to {@code upload}.
     *
     * @param dataIn to check
     *
     * @return true if it has it set, false otherwise
     */
    private boolean hasUploadPropertySet(DataIns dataIn) {
        return hasPropertySet(dataIn, "datatransfer", "upload");
    }

    /**
     * Checks if the {@code dataIn} has the property {@code propertyName} set to {@code propertyValue}.
     *
     * @param dataIn        to check
     * @param propertyName  the name of the property
     * @param propertyValue the value of the property
     *
     * @return true if it has it set, false otherwise
     */
    private boolean hasPropertySet(DataIns dataIn, String propertyName, String propertyValue) {
        List<PropertyConstraint> properties = dataIn.getProperties();
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
