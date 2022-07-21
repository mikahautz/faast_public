package at.ac.uibk.scheduler.api.node;

import at.ac.uibk.core.functions.Parallel;

public class BeginParallelNode extends FunctionNode {

    private Parallel parallel;

    public BeginParallelNode(final Parallel parallel) {
        super();
        this.parallel = parallel;
    }

    public Parallel getParallel() {
        return parallel;
    }

    public void setParallel(final Parallel parallel) {
        this.parallel = parallel;
    }
}
