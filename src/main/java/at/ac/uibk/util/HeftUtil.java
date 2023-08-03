package at.ac.uibk.util;

import at.ac.uibk.core.functions.objects.DataIns;
import at.ac.uibk.core.functions.objects.DataOuts;
import at.ac.uibk.core.functions.objects.PropertyConstraint;
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

import java.util.*;
import java.util.stream.Collectors;

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

    /**
     * Extracts the value for the {@code fileamount} property of a {@link DataIns}.
     *
     * @param dataIn   to extract the property
     * @param optional specifies if the fileamount is optional, if it is not an error will be thrown if it is not found
     *
     * @return the list of extracted fileamount(s)
     */
    public static List<Integer> extractFileAmount(DataIns dataIn, boolean optional) {
        List<Integer> result = extractFileAmountHelper(dataIn.getProperties(), dataIn.getName());
        if (result != null && !result.isEmpty()) {
            return result;
        } else if (optional) {
            return null;
        } else {
            throw new SchedulingException(dataIn.getName() + ": Property 'fileamount' is missing!");
        }
    }

    /**
     * Extracts the value for the {@code fileamount} property of a {@link DataOuts}.
     *
     * @param dataOut to extract the property
     *
     * @return the list of extracted fileamount(s)
     */
    public static List<Integer> extractFileAmount(DataOuts dataOut) {
        return extractFileAmountHelper(dataOut.getProperties(), dataOut.getName());
    }

    /**
     * Helper method to extract the fileamount of the given list of properties.
     *
     * @param properties to check for the fileamount property
     * @param name       the name of the dataIn or dataOut
     *
     * @return the list of extracted fileamount(s)
     */
    private static List<Integer> extractFileAmountHelper(List<PropertyConstraint> properties, String name) {
        if (properties != null) {
            for (PropertyConstraint property : properties) {
                if (property.getName().equalsIgnoreCase("fileamount")) {
                    String value = property.getValue();
                    try {
                        if (value.contains(",")) {
                            return Arrays.stream(value.split(","))
                                    .map(Integer::parseInt)
                                    .collect(Collectors.toList());
                        } else {
                            return List.of(Integer.parseInt(value));
                        }
                    } catch (NumberFormatException e) {
                        throw new SchedulingException(name + ": Property 'fileamount' has to be an Integer or a list of Integers!");
                    }
                }
            }
        }
        return null;
    }

    /**
     * Extracts the value for the {@code filesize} property of a {@link DataIns}.
     *
     * @param dataIn   to extract the property
     * @param optional specifies if the filesize is optional, if it is not an error will be thrown if it is not found
     *
     * @return the list of extracted filesize(s)
     */
    public static List<Double> extractFileSize(DataIns dataIn, boolean optional) {
        if (dataIn.getProperties() != null) {
            for (PropertyConstraint property : dataIn.getProperties()) {
                if (property.getName().equalsIgnoreCase("filesize")) {
                    String value = property.getValue();
                    try {
                        if (value.contains(",")) {
                            return Arrays.stream(value.split(","))
                                    .map(Double::parseDouble)
                                    .collect(Collectors.toList());
                        } else {
                            return List.of(Double.parseDouble(value));
                        }
                    } catch (NumberFormatException e) {
                        throw new SchedulingException("Property 'filesize' has to be a Double!");
                    }
                }
            }
        }

        List<Double> result = extractFileSizeHelper(dataIn.getProperties(), dataIn.getName());
        if (result != null && !result.isEmpty()) {
            return result;
        } else if (optional) {
            return null;
        } else {
            throw new SchedulingException(dataIn.getName() + ": Property 'filesize' is missing!");
        }
    }

    /**
     * Extracts the value for the {@code filesize} property of a {@link DataOuts}.
     *
     * @param dataOut to extract the property
     *
     * @return the list of extracted filesize(s)
     */
    public static List<Double> extractFileSize(DataOuts dataOut) {
        return extractFileSizeHelper(dataOut.getProperties(), dataOut.getName());
    }

    /**
     * Helper method to extract the filesize of the given list of properties.
     *
     * @param properties to check for the filesize property
     * @param name       the name of the dataIn or dataOut
     *
     * @return the list of extracted filesize(s)
     */
    private static List<Double> extractFileSizeHelper(List<PropertyConstraint> properties, String name) {
        if (properties != null) {
            for (PropertyConstraint property : properties) {
                if (property.getName().equalsIgnoreCase("filesize")) {
                    String value = property.getValue();
                    try {
                        if (value.contains(",")) {
                            return Arrays.stream(value.split(","))
                                    .map(Double::parseDouble)
                                    .collect(Collectors.toList());
                        } else {
                            return List.of(Double.parseDouble(value));
                        }
                    } catch (NumberFormatException e) {
                        throw new SchedulingException(name + ": Property 'filesize' has to be a Double or a list of Doubles!");
                    }
                }
            }
        }
        return null;
    }

}
