package at.ac.uibk.scheduler.api.node;

import at.ac.uibk.core.functions.Compound;

public class BeginConditionalNode extends FunctionNode {

    private Compound compound;

    public BeginConditionalNode(final Compound compound) {
        super();
        this.compound = compound;
    }

    public Compound getCompound() {
        return compound;
    }

    public void setCompound(final Compound compound) {
        this.compound = compound;
    }
}
