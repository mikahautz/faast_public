package at.ac.uibk.scheduler.api.node;

public class EndParallelForBodyNode extends FunctionNode {

    private BeginParallelForBodyNode beginParallelForBodyNode;

    public EndParallelForBodyNode(BeginParallelForBodyNode beginParallelForBodyNode) {
        this.beginParallelForBodyNode = beginParallelForBodyNode;
    }

    public BeginParallelForBodyNode getBeginParallelForBodyNode() {
        return beginParallelForBodyNode;
    }

    public void setBeginParallelForBodyNode(BeginParallelForBodyNode beginParallelForBodyNode) {
        this.beginParallelForBodyNode = beginParallelForBodyNode;
    }
}
