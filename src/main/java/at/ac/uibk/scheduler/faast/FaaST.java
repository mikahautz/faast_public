package at.ac.uibk.scheduler.faast;

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
import at.ac.uibk.util.DecisionLogger;
import at.ac.uibk.util.HeftUtil;
import org.jgrapht.graph.DefaultDirectedWeightedGraph;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class FaaST implements SchedulingAlgorithm {

    private static final boolean EXTENDED_LOGGING = false;

    public FaaST() {
        bRankMap = new HashMap<>();
        scaledResourcesByFunctionType = new HashMap<>();
        regionConcurrencyChecker = new RegionConcurrencyChecker();
    }

    private final Map<FunctionNode, Double> bRankMap;
    private final Map<FunctionType, Set<FunctionDeploymentResource>> scaledResourcesByFunctionType;
    private final RegionConcurrencyChecker regionConcurrencyChecker;

    private static Set<FunctionDeploymentResource> getAllScaledByMaxConcurrencyFor(final FunctionType functionType) {

        return MetadataCache.get().getDeploymentsByFunctionType().get(functionType)
                .stream()
                .flatMap(fd ->
                        MetadataCache.get().getRegionFor(fd)
                                .flatMap(MetadataCache.get()::getDetailedProviderFor)
                                .map(DetailedProvider::getMaxConcurrency)
                                .map(maxConcurrency ->
                                        IntStream.range(0, maxConcurrency)
                                                .mapToObj(unused -> new FunctionDeploymentResource(fd, functionType)))
                                .orElse(Stream.empty()))
                .collect(Collectors.toSet());
    }


    @Override
    public void schedule(final DefaultDirectedWeightedGraph<FunctionNode, Communication> graph) {

        DecisionLogger decisionLogger = null;
        if (FaaST.EXTENDED_LOGGING) {
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

            final Set<FunctionDeploymentResource> resources = this.scaledResourcesByFunctionType.get(functionType);

            double minEst = Double.MAX_VALUE;
            double minEft = Double.MAX_VALUE;

            final Supplier<SchedulingException> noScheduleFoundException =
                    () -> new SchedulingException("cannot schedule function for name " + toSchedule.getAtomicFunction().getName());

            FunctionDeploymentResource schedulingDecision = null;

            for (final FunctionDeploymentResource resource : resources) {
                final double currentEst = HeftUtil.calculateEarliestStartTimeOnResource(resource, graph, toSchedule,
                        resource.getDeployment(), resource.getDeployment().getAvgRTT(), this.regionConcurrencyChecker);
                final double currentEft = currentEst + resource.getDeployment().getAvgRTT();

                if (decisionLogger != null) {
                    decisionLogger.saveEntry(resource.getDeployment().getKmsArn(), currentEst, currentEft);
                }

                if (schedulingDecision == null) {
                    schedulingDecision = resource;
                    minEst = currentEst;
                    minEft = currentEft;

                } else if (currentEft < minEft) {
                    minEft = currentEft;
                    minEst = currentEst;
                    schedulingDecision = resource;
                }
            }

            if (minEst >= Double.MAX_VALUE) {
                throw noScheduleFoundException.get();
            }

            if (decisionLogger != null) {
                decisionLogger.log(schedulingDecision.getDeployment().getKmsArn(), toSchedule);
            }

            schedulingDecision.getPlannedExecutions().add(new PlannedExecution(minEst, minEft));
            toSchedule.setSchedulingDecision(schedulingDecision.getDeployment());
            toSchedule.setAlgorithmInfo(new PlannedExecution(minEst, minEft));

            final Region region = MetadataCache.get().getRegionFor(schedulingDecision.getDeployment()).orElseThrow();
            this.regionConcurrencyChecker.scheduleFunction(region, minEst, minEft);

            maxEft = Math.max(maxEft, minEft);
        }

        System.out.println("Calculated makespan:" + maxEft);

        if (decisionLogger != null) {
            decisionLogger.getLogger().info("Calculated makespan: " + maxEft);
        }

    }

    @Override
    public double calculateAverageTime(final AtomicFunctionNode node) {

        final String atomicFunctionTypeName = node.getAtomicFunction().getType();

        final FunctionType typeForFunction = MetadataCache.get().getFunctionTypesByName().get(atomicFunctionTypeName);

        if (typeForFunction == null) {
            throw new SchedulingException("Unknown Functiontype");
        }

        final Set<FunctionDeploymentResource> allResources =
                this.scaledResourcesByFunctionType.computeIfAbsent(typeForFunction, FaaST::getAllScaledByMaxConcurrencyFor);

        if (allResources.isEmpty()) {
            return Double.MAX_VALUE;
        }

        return allResources
                .stream()
                .map(FunctionDeploymentResource::getDeployment)
                .mapToDouble(FunctionDeployment::getAvgRTT)
                .sum() / allResources.size();
    }

}
