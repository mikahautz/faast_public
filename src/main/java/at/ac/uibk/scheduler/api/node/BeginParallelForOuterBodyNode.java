package at.ac.uibk.scheduler.api.node;

import at.ac.uibk.core.functions.ParallelFor;

public class BeginParallelForOuterBodyNode extends FunctionNode {

    private final ParallelFor parallelFor;

    public BeginParallelForOuterBodyNode(ParallelFor parallelFor) {
        this.parallelFor = parallelFor;
    }

    public ParallelFor getParallelFor() {
        return parallelFor;
    }
}
