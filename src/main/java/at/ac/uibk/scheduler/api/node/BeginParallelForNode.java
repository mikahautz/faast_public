package at.ac.uibk.scheduler.api.node;

import at.ac.uibk.core.functions.ParallelFor;

public class BeginParallelForNode extends FunctionNode {

    private ParallelFor parallelFor;

    public BeginParallelForNode(final ParallelFor parallelFor) {
        this.parallelFor = parallelFor;
    }

    public ParallelFor getParallelFor() {
        return parallelFor;
    }

    public void setParallelFor(final ParallelFor parallelFor) {
        this.parallelFor = parallelFor;
    }
}
