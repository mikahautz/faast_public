package at.ac.uibk.scheduler.api;

import at.ac.uibk.core.Workflow;
import at.ac.uibk.scheduler.api.node.FunctionNode;
import org.jgrapht.graph.DefaultDirectedWeightedGraph;

import java.util.function.Supplier;

public class Scheduler {

    public static Workflow schedule(final Workflow workflow,
                                    final Supplier<SchedulingAlgorithm> algorithmSupplier,
                                    final Predictor predictor) {

        final DefaultDirectedWeightedGraph<FunctionNode, Communication> graph = GraphConverter.buildGraph(workflow, predictor);

        final SchedulingAlgorithm algorithm = algorithmSupplier.get();

        algorithm.schedule(graph);

        WorkflowConverter.writeSchedule(workflow, graph);

        return workflow;
    }

}
