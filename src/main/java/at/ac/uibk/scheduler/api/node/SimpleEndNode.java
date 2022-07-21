package at.ac.uibk.scheduler.api.node;

public class SimpleEndNode extends FunctionNode {

    private SimpleBeginNode simpleBeginNode;

    public SimpleEndNode(final SimpleBeginNode simpleBeginNode) {
        super();
        this.simpleBeginNode = simpleBeginNode;
    }

    public SimpleBeginNode getSimpleBeginNode() {
        return simpleBeginNode;
    }

    public void setSimpleBeginNode(final SimpleBeginNode simpleBeginNode) {
        this.simpleBeginNode = simpleBeginNode;
    }
}
