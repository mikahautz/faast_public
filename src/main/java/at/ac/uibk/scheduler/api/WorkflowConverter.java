package at.ac.uibk.scheduler.api;

import at.ac.uibk.core.Function;
import at.ac.uibk.core.Workflow;
import at.ac.uibk.core.functions.AtomicFunction;
import at.ac.uibk.core.functions.Parallel;
import at.ac.uibk.core.functions.ParallelFor;
import at.ac.uibk.core.functions.objects.PropertyConstraint;
import at.ac.uibk.metadata.api.model.DetailedProvider;
import at.ac.uibk.metadata.api.model.functions.FunctionDeployment;
import at.ac.uibk.scheduler.api.node.*;
import org.apache.commons.collections4.ComparatorUtils;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.AllDirectedPaths;
import org.jgrapht.graph.DefaultDirectedWeightedGraph;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class WorkflowConverter {


    public static void writeSchedule(Workflow workflow, final DefaultDirectedWeightedGraph<FunctionNode, Communication> graph) {

        // write each scheduling decision just in a dumb way to the file
        graph.vertexSet()
                .stream()
                .filter(AtomicFunctionNode.class::isInstance)
                .map(AtomicFunctionNode.class::cast)
                .forEach(WorkflowConverter::writeSchedulingDecision);

        // partition for loops
        // first list iterates through all parallel fors
        // second list iterates through all loop bodies, a parallelFor with 3 iteration has 3 identical bodies
        // third list iterates through all paths in one loop body

        final List<List<List<GraphPath<FunctionNode, Communication>>>> parallelFors =
                graph.vertexSet()
                        .stream()
                        .filter(EndParallelForNode.class::isInstance)
                        .map(EndParallelForNode.class::cast)
                        .map(endParallelForNode -> WorkflowConverter.getEndBodyNodes(graph, endParallelForNode))
                        .map(bodies -> bodies.stream().map(endparallelBodyNode -> new AllDirectedPaths<>(graph).getAllPaths(endparallelBodyNode.getBeginParallelForBodyNode(),
                                        endparallelBodyNode, true, null))
                                .collect(Collectors.toList()))
                        .collect(Collectors.toList());


        for (final List<List<GraphPath<FunctionNode, Communication>>> loopBodies : parallelFors) {

            final List<List<FunctionNode>> loop = loopBodies.stream()
                    .map(WorkflowConverter::flatten)
                    .collect(Collectors.toList());

            final Map<Integer, List<List<GraphPath<FunctionNode, Communication>>>> partitions =
                    loopBodies.stream()
                            .collect(Collectors.groupingBy(WorkflowConverter::hash));

            if (partitions.values().size() > 1) {

                final List<Map<String, Object>> partitioningMappings = new LinkedList<>();
                int begin = 0;

                final BeginParallelForBodyNode startNode = loop.get(0)
                        .stream().filter(BeginParallelForBodyNode.class::isInstance)
                        .map(BeginParallelForBodyNode.class::cast)
                        .findFirst().orElseThrow();

                final String parallelForNodeName = startNode.getParallelFor().getName();


                for (final List<List<GraphPath<FunctionNode, Communication>>> partition : partitions.values()) {
                    final int partitionSize = partition.size();

                    final Map<String, Object> partitionMapping = new HashMap<>();
                    partitionMapping.put("from", String.valueOf(begin));
                    partitionMapping.put("to", String.valueOf(begin + partitionSize));
                    partitionMapping.put("step", "1");
                    partitioningMappings.add(partitionMapping);
                    begin += partitionSize;
                }

                workflow = WorkflowConversionService.getAdaptedWorkflow(workflow, Map.of(parallelForNodeName,
                        partitioningMappings));

                int idx = 0;
                for (final List<List<GraphPath<FunctionNode, Communication>>> partition : partitions.values()) {

                    final List<GraphPath<FunctionNode, Communication>> branch = partition.get(0);
                    final List<FunctionNode> flattenedBranch = flatten(partition.get(0));

                    final ParallelFor parallelFor = (ParallelFor) WorkflowConversionService.getFunctionByName(workflow,
                            parallelForNodeName + "_" + idx);

                    int minConcurrency = Integer.MAX_VALUE;
                    for (final AtomicFunction workFlowFn : WorkflowConverter.flatMapToAtomicFunctions(parallelFor.getLoopBody())) {

                        final AtomicFunctionNode graphFn = flattenedBranch.stream()
                                .filter(AtomicFunctionNode.class::isInstance)
                                .map(AtomicFunctionNode.class::cast)
                                .filter(an -> workFlowFn.getName().startsWith(an.getAtomicFunction().getName()))
                                .findAny().orElseThrow();

                        minConcurrency = Math.min(minConcurrency,
                                MetadataCache.get().getRegionFor(graphFn.getSchedulingDecision())
                                        .flatMap(MetadataCache.get()::getDetailedProviderFor)
                                        .map(DetailedProvider::getMaxConcurrency)
                                        .orElse(Integer.MAX_VALUE));

                        WorkflowConverter.writeSchedulingDecision(workFlowFn, graphFn.getSchedulingDecision());
                    }
                    idx++;

                    calculateAndApplyConcurrencyConstraint(parallelFor, partition);
                }

            } else {

                final List<List<GraphPath<FunctionNode, Communication>>> singlePartition = partitions.values().iterator().next();

                final BeginParallelForBodyNode startNode = loop.get(0)
                        .stream().filter(BeginParallelForBodyNode.class::isInstance)
                        .map(BeginParallelForBodyNode.class::cast)
                        .findFirst().orElseThrow();

                startNode.getParallelFor().getLoopCounter().setType("number");
                startNode.getParallelFor().getLoopCounter().setFrom("0");
                startNode.getParallelFor().getLoopCounter().setStep("1");
                startNode.getParallelFor().getLoopCounter().setTo(String.valueOf(singlePartition.size()));

                calculateAndApplyConcurrencyConstraint(startNode.getParallelFor(), singlePartition);
            }

        }
    }

    private static void calculateAndApplyConcurrencyConstraint(ParallelFor parallelFor,
                                                               List<List<GraphPath<FunctionNode, Communication>>> loopBodies) {
        final List<GraphPath<FunctionNode, Communication>> loopBody = loopBodies.get(0);

        final var paths = loopBody.stream()
                .map(GraphPath::getVertexList)
                .map(vl -> vl.stream()
                        .filter(AtomicFunctionNode.class::isInstance)
                        .map(AtomicFunctionNode.class::cast)
                        .collect(Collectors.collectingAndThen(Collectors.toList(), l -> {
                            Collections.reverse(l);
                            return l;
                        })))
                .sorted(ComparatorUtils.reversedComparator(Comparator.comparing(List::size)))
                .collect(Collectors.toList());

        final List<AtomicFunctionNode> longestPath = paths.get(0);

        long globalMaxRegions = -1L;

        for (int i = 0; i < longestPath.size(); i++) {

            final int currentIndex = i;
            final Map<Long, Long> regionCount = paths.stream()
                    .map(path -> path.size() >= currentIndex
                            ? path.get(currentIndex)
                            : null)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.groupingBy(afn -> afn.getSchedulingDecision().getRegionId(), Collectors.counting()));

            long currentMax = regionCount.values()
                    .stream()
                    .mapToLong(f -> f)
                    .max()
                    .orElse(-1L);

            globalMaxRegions = Math.max(globalMaxRegions, currentMax);
        }

        final int maxConcurrency = MetadataCache.get().getDetailedProviders()
                .stream()
                .map(DetailedProvider::getMaxConcurrency)
                .filter(Objects::nonNull)
                .mapToInt(f -> f)
                .min()
                .orElse(-1);

        if (maxConcurrency > 0 && globalMaxRegions > 0) {
            writeConcurrencyConstraint(parallelFor, Math.floorDiv(maxConcurrency, (int) globalMaxRegions));
        }
    }

    private static int hash(final List<GraphPath<FunctionNode, Communication>> nodes) {
        int hashCode = 1;
        for (final FunctionNode next : WorkflowConverter.flatten(nodes)) {
            if (next instanceof AtomicFunctionNode) {
                hashCode = 31 * hashCode + ((AtomicFunctionNode) next).getSchedulingDecision().getKmsArn().hashCode();
            }
        }
        return hashCode;
    }

    private static List<EndParallelForBodyNode> getEndBodyNodes(final DefaultDirectedWeightedGraph<FunctionNode, Communication> graph,
                                                                final EndParallelForNode endParallelForNode) {

        return graph.incomingEdgesOf(endParallelForNode)
                .stream()
                .map(graph::getEdgeSource)
                .map(EndParallelForBodyNode.class::cast)
                .collect(Collectors.toList());
    }

    private static void writeConcurrencyConstraint(final ParallelFor function, final int maxConcurrency) {
        final PropertyConstraint propertyConstraint = new PropertyConstraint();
        propertyConstraint.setName("concurrency");
        propertyConstraint.setValue(String.valueOf(maxConcurrency));
        WorkflowConverter.writeConstraint(function, propertyConstraint);
    }

    private static void writeSchedulingDecision(final AtomicFunction function, final FunctionDeployment deployment) {
        final PropertyConstraint propertyConstraint = new PropertyConstraint();
        propertyConstraint.setName("resource");
        propertyConstraint.setValue(deployment.getKmsArn());
        WorkflowConverter.writeProperty(function, propertyConstraint);
    }

    private static void writeConstraint(final Function function, final PropertyConstraint constraint) {
        if (function.getConstraints() != null) {
            function.getConstraints().removeIf(pc -> constraint.getName().equals(pc.getName()));
            function.getConstraints().add(constraint);
        } else {
            final var propertiesList = new ArrayList<PropertyConstraint>();
            propertiesList.add(constraint);
            function.setConstraints(propertiesList);
        }
    }

    private static void writeProperty(final Function function, final PropertyConstraint constraint) {
        if (function.getProperties() != null) {
            function.getProperties().removeIf(pc -> constraint.getName().equals(pc.getName()));
            function.getProperties().add(constraint);
        } else {
            final var propertiesList = new ArrayList<PropertyConstraint>();
            propertiesList.add(constraint);
            function.setProperties(propertiesList);
        }
    }

    private static void writeSchedulingDecision(final AtomicFunctionNode atomicFunctionNode) {
        WorkflowConverter.writeSchedulingDecision(atomicFunctionNode.getAtomicFunction(), atomicFunctionNode.getSchedulingDecision());
    }

    private static List<FunctionNode> flatten(final List<GraphPath<FunctionNode, Communication>> loopBody) {
        return loopBody.stream()
                .map(GraphPath::getVertexList)
                .flatMap(Collection::stream)
                .sorted(Comparator.comparing(FunctionNode::getId))
                .collect(Collectors.toList());
    }

    private static List<AtomicFunction> flatMapToAtomicFunctions(final List<at.ac.uibk.core.Function> functions) {

        return functions.stream()
                .flatMap(f -> {
                    if (f instanceof AtomicFunction) {
                        return Stream.of((AtomicFunction) f);
                    } else if (f instanceof ParallelFor) {
                        return WorkflowConverter.flatMapToAtomicFunctions(((ParallelFor) f).getLoopBody()).stream();
                    } else if (f instanceof Parallel) {
                        return ((Parallel) f).getParallelBody().stream()
                                .flatMap(s -> WorkflowConverter.flatMapToAtomicFunctions(s.getSection()).stream());
                    }

                    throw new UnsupportedOperationException();
                })
                .collect(Collectors.toList());


    }

}
