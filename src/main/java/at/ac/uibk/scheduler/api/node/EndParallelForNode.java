package at.ac.uibk.scheduler.api.node;

public class EndParallelForNode extends FunctionNode {

    private BeginParallelForNode beginParallelForNode;

    public EndParallelForNode(final BeginParallelForNode beginNode) {
        super();
        this.beginParallelForNode = beginNode;
    }

    public BeginParallelForNode getBeginParallelForNode() {
        return beginParallelForNode;
    }

    public void setBeginParallelForNode(final BeginParallelForNode beginParallelForNode) {
        this.beginParallelForNode = beginParallelForNode;
    }
}
