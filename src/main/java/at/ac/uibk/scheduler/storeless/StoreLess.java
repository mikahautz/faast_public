package at.ac.uibk.scheduler.storeless;

import at.ac.uibk.core.functions.objects.DataIns;
import at.ac.uibk.core.functions.objects.DataOutsAtomic;
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

//            final Set<FunctionDeploymentResource> resources = this.scaledResourcesByFunctionType.get(functionType);

            // get set of resources per region (up to concurrency limit), so e.g. 100 resources in region A
            // get set of FDs per type
            // loop through FDs
            // per FD, get region of deployment, and query set of region-resources for that region, and return only
            //     those resources that are distinct (deployment != deployment and plannedExecutions != plannedExecutions)

            double minEst = Double.MAX_VALUE;
            double minEft = Double.MAX_VALUE;

            final Supplier<SchedulingException> noScheduleFoundException =
                    () -> new SchedulingException("cannot schedule function for name " + toSchedule.getAtomicFunction().getName());

//            FunctionDeploymentResource schedulingDecision = null;
            RegionResource schedulingDecision = null;
            FunctionDeployment scheduledFunctionDeployment = null;
            List<DataIns> scheduledDataIns = null;
            List<DataOutsAtomic> scheduledDataOuts = null;

            for (final FunctionDeployment fd : functionDeploymentsByFunctionType.get(functionType)) {
                final Region region = MetadataCache.get().getRegionFor(fd).orElseThrow();
                RegionResource resource = getBestRegionResource(region);

                if (resource == null) {
                    continue;
                }

                // TODO dataOuts dont have to be updated, since the outputDestination has to be given in the input anyways,
                // so we only need to update the input field that specifies the output destination???
                // TODO but the DataFlowStore.dataOuts need to be updated with the value for the dataOut storage

                List<DataIns> dataIns = toSchedule.getAtomicFunction().getDataIns();
                List<DataIns> toDownloadInputs = extractDownloadDataIns(dataIns); // list of dataIns where DLs have to occur
                List<DataIns> toUploadInputs = extractUploadDataIns(dataIns); // list of dataIns that specify where the output should be stored

                Double downloadTime = 0D;
                for (DataIns dataIn : toDownloadInputs) {
                    int fileAmount = extractFileAmount(dataIn);
                    double fileSize = extractFilSize(dataIn);

                    String url = null;

                    if (dataIn.getValue() != null && !dataIn.getValue().isEmpty()) {
                        url = dataIn.getValue();
                    } else {
                        url = DataFlowStore.getDataInValue(dataIn.getSource(), false);
                    }

                    // TODO apply model here
                    double dlTime = 0;
                    downloadTime += dlTime;
                }


                Double uploadTime = 0D;
                // TODO loop through storage regions, calculate in each iteration the upload time
                // perform all of the steps below within the loop, so compare in each iteration if it is the min for the current fd
                // at the end of the loop we have to compare the intermediate result of the fd + DTT with the fastest

                // upload time = 0
                // loop through toLookAtOutput
                for (int j = 0; j < 2; j++) {
                    // output_min = double.max
                    // loop through storages
                    for (int i = 0; i < 2; i++) {
                        // get region
                        // calculate upload time
                        // check if upload time is < than output_min
                        // if yes, update time and set element of toLookAtOutput with new value and write back to list
                    }
                    // upload time += output_min
                }


                // is dataOuts even needed, or can everything be controlled by dataIns?
                // TODO maybe delete value and values field again
                List<DataOutsAtomic> dataOuts = toSchedule.getAtomicFunction().getDataOuts();
                // dataOuts need #files and size of files
                // generate list of available storage outputs
                // calculate UT by calculating from resource region to storage region
                // TODO extend dataOuts to include field for value (or include in properties?)
                // would need to modify EE/FunctionNode.java line 205 to add constant value to functionOutputs
                // TODO or set the value of the input of the next function that consumes this dataOut?
                // set storage bucket and region to dataOut by writing to 'value' field
                // replace dataOut in list of dataOuts
                // set modified list of dataOuts to scheduling decision/resource

                double RTT = fd.getAvgRTT() + downloadTime + uploadTime;

                final double currentEst = HeftUtil.calculateEarliestStartTimeOnResource(resource, graph, toSchedule,
                        fd, RTT, this.regionConcurrencyChecker);
                final double currentEft = currentEst + RTT;


                if (decisionLogger != null) {
                    decisionLogger.saveEntry(fd.getKmsArn(), currentEst, currentEft);
                }

                // compare if current eft < min eft
                // if yes, update all values as below, write back toLookAtOutputs to dataIns and update dataflowstore dataout value
                if (schedulingDecision == null || currentEft < minEft) {
                    schedulingDecision = resource;
                    scheduledFunctionDeployment = fd;
                    minEst = currentEst;
                    minEft = currentEft;
                    // dataIns.addBack(toLookAtOutputs)
                    scheduledDataIns = dataIns;
                    scheduledDataOuts = dataOuts; // TODO is needed?
                    // DataFlowStore.updateDataOutValue
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
            toSchedule.setScheduledDataOuts(scheduledDataOuts);
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

    private List<DataIns> extractDownloadDataIns(List<DataIns> dataIns) {
        List<DataIns> dlDataIns = new ArrayList<>();
        for (DataIns dataIn : dataIns) {
            if (hasDownloadPropertySet(dataIn)) {
                dlDataIns.add(dataIn);
            }
        }
        return dlDataIns;
    }

    private List<DataIns> extractUploadDataIns(List<DataIns> dataIns) {
        List<DataIns> upDataIns = new ArrayList<>();
        for (DataIns dataIn : dataIns) {
            if (hasUploadPropertySet(dataIn)) {
                upDataIns.add(dataIn);
            }
        }
        return upDataIns;
    }

    private int extractFileAmount(DataIns dataIn) {
        List<PropertyConstraint> properties = dataIn.getProperties();
        for (PropertyConstraint property : properties) {
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

    private double extractFilSize(DataIns dataIn) {
        List<PropertyConstraint> properties = dataIn.getProperties();
        for (PropertyConstraint property : properties) {
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

    private boolean hasDownloadPropertySet(DataIns dataIn) {
        return hasPropertySet(dataIn, "datatransfer", "download");
    }

    private boolean hasUploadPropertySet(DataIns dataIn) {
        return hasPropertySet(dataIn, "datatransfer", "upload");
    }

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
