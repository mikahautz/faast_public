package at.ac.uibk.scheduler.api.node;

import at.ac.uibk.core.functions.AtomicFunction;
import at.ac.uibk.metadata.api.model.functions.FunctionDeployment;

public class AtomicFunctionNode extends FunctionNode {

    private AtomicFunction atomicFunction;

    private FunctionDeployment schedulingDecision;

    public AtomicFunctionNode(final AtomicFunction function) {
        super();
        this.atomicFunction = function;
    }

    public AtomicFunction getAtomicFunction() {
        return atomicFunction;
    }

    public void setAtomicFunction(final AtomicFunction atomicFunction) {
        this.atomicFunction = atomicFunction;
    }

    public FunctionDeployment getSchedulingDecision() {
        return schedulingDecision;
    }

    public void setSchedulingDecision(FunctionDeployment schedulingDecision) {
        this.schedulingDecision = schedulingDecision;
    }
}
