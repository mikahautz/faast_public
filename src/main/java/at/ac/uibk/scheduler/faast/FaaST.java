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
import org.apache.commons.collections4.ComparatorUtils;
import org.jgrapht.graph.DefaultDirectedWeightedGraph;
import org.jgrapht.graph.EdgeReversedGraph;
import org.jgrapht.traverse.BreadthFirstIterator;
import org.jgrapht.traverse.TopologicalOrderIterator;

import java.util.*;
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

    private void initializeBRankMap(final DefaultDirectedWeightedGraph<FunctionNode, Communication> graph) {
        graph.vertexSet()
                .forEach(node -> {
                    if (node instanceof AtomicFunctionNode) {
                        this.bRankMap.put(node, this.calculateAverageTime((AtomicFunctionNode) node));
                    } else {
                        this.bRankMap.put(node, 0D);
                    }
                });

        final EdgeReversedGraph<FunctionNode, Communication> reversedGraph = new EdgeReversedGraph<>(graph);

        final TopologicalOrderIterator<FunctionNode, Communication> backwardsIterator =
                new TopologicalOrderIterator<>(reversedGraph);

        if (!backwardsIterator.hasNext()) {
            throw new SchedulingException("Graph is empty");
        }

        //calculate brank
        while (backwardsIterator.hasNext()) {
            final FunctionNode currentNode = backwardsIterator.next();

            final double maxCostOfParents = reversedGraph.incomingEdgesOf(currentNode)
                    .stream()
                    .map(reversedGraph::getEdgeSource)
                    .mapToDouble(this.bRankMap::get)
                    .max().orElse(0D);

            final double newCost = this.bRankMap.get(currentNode) + maxCostOfParents;
            this.bRankMap.put(currentNode, newCost);
        }
    }

    private double calculateEarliestStartTimeOnResource(
            final FunctionDeploymentResource resource,
            final DefaultDirectedWeightedGraph<FunctionNode, Communication> graph,
            final AtomicFunctionNode atomicFunctionNode) {

        final FunctionNode startNodeOfGraph = new BreadthFirstIterator<>(graph).next();

        final PlannedExecution plannedExecution = startNodeOfGraph.getAlgorithmInfoTyped();

        if (plannedExecution == null) {
            startNodeOfGraph.setAlgorithmInfo(new PlannedExecution(0D, 0D));
        }

        final double latestEFT = graph.incomingEdgesOf(atomicFunctionNode)
                .stream()
                .map(graph::getEdgeSource)
                .flatMap(p -> p.<PlannedExecution>getFirstAlgorithmInfosTypedFromAllPredecessors(graph))
                .mapToDouble(PlannedExecution::getEndTime)
                .max().orElse(0D);

        final Optional<Region> region = MetadataCache.get().getRegionFor(resource.getDeployment());

        final var res = resource.getEligibleRunTimesAfter(latestEFT)
                .stream()
                .filter(runtime -> Math.abs(runtime.getLeft() - runtime.getRight()) > resource.getDeployment().getAvgRTT())
                .map(runtime ->
                        region.flatMap(r ->
                                        this.regionConcurrencyChecker.getEarliestRuntimeInRegionBetween(r, runtime.getLeft(),
                                                runtime.getRight(), resource.getDeployment().getAvgRTT()))
                                .orElse(Double.MAX_VALUE))
                .mapToDouble(f -> f)
                .min();

        if (res.isEmpty() || res.getAsDouble() >= Double.MAX_VALUE) {
            throw new SchedulingException("unexpected error when searching for eligible runtimes");
        } else {
            return res.getAsDouble();
        }
    }

    @Override
    public void schedule(final DefaultDirectedWeightedGraph<FunctionNode, Communication> graph) {

        DecisionLogger decisionLogger = null;
        if (FaaST.EXTENDED_LOGGING) {
            decisionLogger = new DecisionLogger(Logger.getLogger(Logger.GLOBAL_LOGGER_NAME));
        }

        this.initializeBRankMap(graph);

        final Iterator<AtomicFunctionNode> bRankIterator =
                this.bRankMap.entrySet()
                        .stream()
                        .sorted(ComparatorUtils.reversedComparator(Map.Entry.comparingByValue()))
                        .map(Map.Entry::getKey)
                        .filter(AtomicFunctionNode.class::isInstance)
                        .map(AtomicFunctionNode.class::cast)
                        .iterator();

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
                final double currentEst = this.calculateEarliestStartTimeOnResource(resource, graph, toSchedule);
                final double currentEft = currentEst + resource.getDeployment().getAvgRTT();
                if (decisionLogger != null) {
                    decisionLogger.saveEntry(resource, currentEst, currentEft);
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
                decisionLogger.log(schedulingDecision, toSchedule);
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

    private double calculateAverageTime(final AtomicFunctionNode node) {

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
