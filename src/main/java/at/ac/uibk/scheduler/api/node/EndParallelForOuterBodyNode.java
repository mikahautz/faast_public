package at.ac.uibk.scheduler.api.node;

public class EndParallelForOuterBodyNode extends FunctionNode {

    private final BeginParallelForOuterBodyNode beginParallelForOuterBodyNode;

    public EndParallelForOuterBodyNode(BeginParallelForOuterBodyNode beginParallelForOuterBodyNode) {
        this.beginParallelForOuterBodyNode = beginParallelForOuterBodyNode;
    }

    public BeginParallelForOuterBodyNode getBeginParallelForOuterBodyNode() {
        return beginParallelForOuterBodyNode;
    }
}
