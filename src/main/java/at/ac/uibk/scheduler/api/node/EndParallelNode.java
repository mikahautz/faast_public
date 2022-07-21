package at.ac.uibk.scheduler.api.node;

public class EndParallelNode extends FunctionNode {

    private BeginParallelNode beginParallelNode;

    public EndParallelNode(final BeginParallelNode beginParallelNode) {
        super();
        this.beginParallelNode = beginParallelNode;
    }

    public BeginParallelNode getBeginParallelNode() {
        return beginParallelNode;
    }

    public void setBeginParallelNode(final BeginParallelNode beginParallelNode) {
        this.beginParallelNode = beginParallelNode;
    }
}
