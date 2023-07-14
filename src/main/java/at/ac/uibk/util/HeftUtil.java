package at.ac.uibk.util;

import at.ac.uibk.metadata.api.model.Region;
import at.ac.uibk.metadata.api.model.functions.FunctionDeployment;
import at.ac.uibk.scheduler.api.*;
import at.ac.uibk.scheduler.api.node.AtomicFunctionNode;
import at.ac.uibk.scheduler.api.node.FunctionNode;
import at.ac.uibk.scheduler.faast.PlannedExecution;
import at.ac.uibk.scheduler.faast.RegionConcurrencyChecker;
import org.apache.commons.collections4.ComparatorUtils;
import org.jgrapht.graph.DefaultDirectedWeightedGraph;
import org.jgrapht.graph.EdgeReversedGraph;
import org.jgrapht.traverse.BreadthFirstIterator;
import org.jgrapht.traverse.TopologicalOrderIterator;

import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

public class HeftUtil {

    public static void initializeBRankMap(SchedulingAlgorithm algorithm, final DefaultDirectedWeightedGraph<FunctionNode, Communication> graph, Map<FunctionNode, Double> bRankMap) {
        graph.vertexSet()
                .forEach(node -> {
                    if (node instanceof AtomicFunctionNode) {
                        bRankMap.put(node, algorithm.calculateAverageTime((AtomicFunctionNode) node));
                    } else {
                        bRankMap.put(node, 0D);
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
                    .mapToDouble(bRankMap::get)
                    .max().orElse(0D);

            final double newCost = bRankMap.get(currentNode) + maxCostOfParents;
            bRankMap.put(currentNode, newCost);
        }
    }

    public static Iterator<AtomicFunctionNode> getBRankIterator(Map<FunctionNode, Double> bRankMap) {
        return bRankMap.entrySet()
                .stream()
                .sorted(ComparatorUtils.reversedComparator(Map.Entry.comparingByValue()))
                .map(Map.Entry::getKey)
                .filter(AtomicFunctionNode.class::isInstance)
                .map(AtomicFunctionNode.class::cast)
                .iterator();
    }

    public static double calculateEarliestStartTimeOnResource(
            final AbstractResource resource,
            final DefaultDirectedWeightedGraph<FunctionNode, Communication> graph,
            final AtomicFunctionNode atomicFunctionNode,
            final FunctionDeployment functionDeployment,
            final Double RTT,
            final RegionConcurrencyChecker regionConcurrencyChecker) {

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

        final Optional<Region> region = MetadataCache.get().getRegionFor(functionDeployment);

        final var res = resource.getEligibleRunTimesAfter(latestEFT)
                .stream()
                .filter(runtime -> Math.abs(runtime.getLeft() - runtime.getRight()) > RTT)
                .map(runtime ->
                        region.flatMap(r ->
                                        regionConcurrencyChecker.getEarliestRuntimeInRegionBetween(r, runtime.getLeft(),
                                                runtime.getRight(), RTT))
                                .orElse(Double.MAX_VALUE))
                .mapToDouble(f -> f)
                .min();

        if (res.isEmpty() || res.getAsDouble() >= Double.MAX_VALUE) {
            throw new SchedulingException("unexpected error when searching for eligible runtimes");
        } else {
            return res.getAsDouble();
        }
    }

}
