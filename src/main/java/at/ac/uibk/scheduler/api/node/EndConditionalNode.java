package at.ac.uibk.scheduler.api.node;

public class EndConditionalNode extends FunctionNode {

    private BeginConditionalNode beginConditionalNode;

    public EndConditionalNode(final BeginConditionalNode beginConditionalNode) {
        super();
        this.beginConditionalNode = beginConditionalNode;
    }

    public BeginConditionalNode getBeginConditionalNode() {
        return beginConditionalNode;
    }

    public void setBeginConditionalNode(final BeginConditionalNode beginConditionalNode) {
        this.beginConditionalNode = beginConditionalNode;
    }
}
