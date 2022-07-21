package at.ac.uibk.scheduler.api.node;

import at.ac.uibk.core.functions.ParallelFor;

public class BeginParallelForBodyNode extends FunctionNode {

    private ParallelFor parallelFor;

    public BeginParallelForBodyNode(ParallelFor parallelFor) {
        this.parallelFor = parallelFor;
    }

    public ParallelFor getParallelFor() {
        return parallelFor;
    }

    public void setParallelFor(ParallelFor parallelFor) {
        this.parallelFor = parallelFor;
    }
}
