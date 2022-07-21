package at.ac.uibk.scheduler.api;

import at.ac.uibk.core.Function;
import at.ac.uibk.core.Workflow;
import at.ac.uibk.core.functions.*;
import at.ac.uibk.core.functions.objects.Case;
import at.ac.uibk.core.functions.objects.Section;
import at.ac.uibk.scheduler.api.node.*;
import org.jgrapht.graph.DefaultDirectedWeightedGraph;
import org.jgrapht.graph.builder.GraphBuilder;

import java.util.ArrayList;
import java.util.List;

public class GraphConverter {

    private final GraphBuilder<FunctionNode, Communication, ? extends DefaultDirectedWeightedGraph<FunctionNode, Communication>> graphBuilder;

    private final Predictor predictor;

    public static DefaultDirectedWeightedGraph<FunctionNode, Communication> buildGraph(final Workflow workflow) {
        return GraphConverter.buildGraph(workflow, new DefaultPredictor());
    }

    public static DefaultDirectedWeightedGraph<FunctionNode, Communication> buildGraph(final Workflow workflow,
                                                                                       final Predictor predictor) {
        final GraphConverter graphConverter = new GraphConverter(predictor);
        graphConverter.convert(workflow);
        return graphConverter.graphBuilder.build();
    }

    private GraphConverter(final Predictor predictor) {
        this.graphBuilder = DefaultDirectedWeightedGraph.createBuilder(Communication.class);
        this.predictor = predictor;
    }

    private void convert(final Workflow workflow) {
        List<FunctionNode> iterating = new ArrayList<>();
        final SimpleBeginNode startNode = new SimpleBeginNode();
        iterating.add(startNode);

        for (final Function fn : workflow.getWorkflowBody()) {
            iterating = this.functionConverter.convert(fn, iterating);
        }

        final FunctionNode end = new SimpleEndNode(startNode);
        iterating.forEach(e -> this.graphBuilder.addEdge(e, end));
    }

    private final ConvertingFunction<Function> functionConverter = (fn, previousNodes) -> {

        if (fn instanceof AtomicFunction) {
            return this.atomicFunctionConverter.convert((AtomicFunction) fn, previousNodes);
        } else if (fn instanceof IfThenElse) {
            return this.ifThenElseConverter.convert((IfThenElse) fn, previousNodes);
        } else if (fn instanceof Parallel) {
            return this.parallelConverter.convert((Parallel) fn, previousNodes);
        } else if (fn instanceof ParallelFor) {
            return this.parallelForConverter.convert((ParallelFor) fn, previousNodes);
        } else if (fn instanceof Sequence) {
            return this.sequenceConverter.convert((Sequence) fn, previousNodes);
        } else if (fn instanceof SequentialFor) {
            return this.sequentialForConverter.convert((SequentialFor) fn, previousNodes);
        } else if (fn instanceof SequentialWhile) {
            return this.sequentialWhileConverter.convert((SequentialWhile) fn, previousNodes);
        } else if (fn instanceof Switch) {
            return this.switchConverter.convert((Switch) fn, previousNodes);
        } else {
            throw new UnsupportedOperationException("unsupported function type");
        }
    };

    private final ConvertingFunction<ParallelFor> parallelForConverter = (parallelFor, previousNodes) -> {

        List<FunctionNode> outerIterationNodes = previousNodes;

        for (Integer iterations : GraphConverter.this.predictor.predict(parallelFor)) {

            BeginParallelForNode startNode = new BeginParallelForNode(parallelFor);
            outerIterationNodes.forEach(p -> GraphConverter.this.graphBuilder.addEdge(p, startNode, new Communication(Communication.Type.PARALLEL_FOR)));

            FunctionNode endNode = new EndParallelForNode(startNode);
            final List<EndParallelForBodyNode> endBodyNodes = new ArrayList<>();

            for (int i = 0; i < iterations; i++) {
                BeginParallelForBodyNode startBodyNode = new BeginParallelForBodyNode(parallelFor);
                EndParallelForBodyNode endBodyNode = new EndParallelForBodyNode(startBodyNode);
                GraphConverter.this.graphBuilder.addEdge(startNode, startBodyNode, new Communication(Communication.Type.PARALLEL_FOR));
                List<FunctionNode> iteratingNodes = List.of(startBodyNode);

                for (final Function fn : parallelFor.getLoopBody()) {
                    iteratingNodes = this.functionConverter.convert(fn, iteratingNodes);
                }

                iteratingNodes.forEach(s -> GraphConverter.this.graphBuilder.addEdge(s, endBodyNode, new Communication(Communication.Type.PARALLEL_FOR)));
                endBodyNodes.add(endBodyNode);
            }

            endBodyNodes.forEach(s -> GraphConverter.this.graphBuilder.addEdge(s, endNode, new Communication(Communication.Type.PARALLEL_FOR)));
            outerIterationNodes = new ArrayList<>(endBodyNodes);
        }

        return outerIterationNodes;
    };

    private final ConvertingFunction<AtomicFunction> atomicFunctionConverter = (atomicFn, predecessors) -> {

        final FunctionNode functionNode = new AtomicFunctionNode(atomicFn);

        predecessors.forEach(p -> GraphConverter.this.graphBuilder.addEdge(p, functionNode));

        return List.of(functionNode);
    };

    private final ConvertingFunction<Switch> switchConverter = (switchStatement, predecessors) -> {

        BeginConditionalNode startNode = new BeginConditionalNode(switchStatement);

        predecessors.forEach(p -> GraphConverter.this.graphBuilder.addEdge(p, startNode, new Communication(Communication.Type.CONDITIONAL)));

        final List<FunctionNode> successors = new ArrayList<>();
        for (final Case caseStatement : switchStatement.getCases()) {
            List<FunctionNode> iterating = predecessors;
            for (final Function fn : caseStatement.getFunctions()) {
                iterating = this.functionConverter.convert(fn, iterating);
            }
            successors.addAll(iterating);
        }

        List<FunctionNode> iterating = predecessors;
        for (final Function fn : switchStatement.getDefault()) {
            iterating = this.functionConverter.convert(fn, iterating);
        }

        successors.addAll(iterating);

        FunctionNode endNode = new EndConditionalNode(startNode);

        successors.forEach(s -> GraphConverter.this.graphBuilder.addEdge(s, endNode, new Communication(Communication.Type.CONDITIONAL)));

        return List.of(endNode);
    };


    private final ConvertingFunction<IfThenElse> ifThenElseConverter = (ifThenElse, predecessors) -> {

        BeginConditionalNode startNode = new BeginConditionalNode(ifThenElse);

        predecessors.forEach(p -> GraphConverter.this.graphBuilder.addEdge(p, startNode, new Communication(Communication.Type.CONDITIONAL)));

        List<FunctionNode> iterating = List.of(startNode);
        for (final Function fn : ifThenElse.getThen()) {
            iterating = this.functionConverter.convert(fn, iterating);
        }

        final List<FunctionNode> successors = new ArrayList<>(iterating);

        iterating = predecessors;
        for (final Function fn : ifThenElse.getElse()) {
            iterating = this.functionConverter.convert(fn, iterating);
        }
        successors.addAll(iterating);

        FunctionNode endNode = new EndConditionalNode(startNode);

        successors.forEach(s -> GraphConverter.this.graphBuilder.addEdge(s, endNode, new Communication(Communication.Type.CONDITIONAL)));

        return List.of(endNode);
    };

    private final ConvertingFunction<Parallel> parallelConverter = (parallel, predecessors) -> {

        BeginParallelNode startNode = new BeginParallelNode(parallel);
        predecessors.forEach(p -> GraphConverter.this.graphBuilder.addEdge(p, startNode, new Communication(Communication.Type.PARALLEL)));

        List<FunctionNode> successors = new ArrayList<>();
        for (final Section section : parallel.getParallelBody()) {
            List<FunctionNode> iterating = List.of(startNode);
            for (final Function fn : section.getSection()) {
                iterating = this.functionConverter.convert(fn, iterating);
            }
            successors.addAll(iterating);
        }

        FunctionNode endNode = new EndParallelNode(startNode);
        successors.forEach(s -> GraphConverter.this.graphBuilder.addEdge(s, endNode, new Communication(Communication.Type.PARALLEL)));

        return List.of(endNode);
    };

    private final ConvertingFunction<SequentialWhile> sequentialWhileConverter = (sequentialWhile, predecessors) -> {

        List<FunctionNode> successors = predecessors;
        for (int i = 0; i < GraphConverter.this.predictor.predict(sequentialWhile.getCondition()); i++) {
            for (final Function fn : sequentialWhile.getLoopBody()) {
                successors = this.functionConverter.convert(fn, successors);
            }
        }

        return successors;

    };

    private final ConvertingFunction<SequentialFor> sequentialForConverter = (sequentialFor, predecessors) -> {

        List<FunctionNode> successors = predecessors;
        for (int i = 0; i < GraphConverter.this.predictor.predict(sequentialFor); i++) {
            for (final Function fn : sequentialFor.getLoopBody()) {
                successors = this.functionConverter.convert(fn, successors);
            }
        }

        return successors;
    };

    private final ConvertingFunction<Sequence> sequenceConverter = (sequence, predecessors) -> {
        List<FunctionNode> successors = predecessors;
        for (final Function fn : sequence.getSequenceBody()) {
            successors = this.functionConverter.convert(fn, successors);
        }
        return successors;
    };

    @FunctionalInterface
    private interface ConvertingFunction<F extends Function> {
        List<FunctionNode> convert(F function, List<FunctionNode> predecessors);
    }
}
