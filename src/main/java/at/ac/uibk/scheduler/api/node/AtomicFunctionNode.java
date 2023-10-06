package at.ac.uibk.scheduler.api.node;

import at.ac.uibk.core.functions.AtomicFunction;
import at.ac.uibk.core.functions.objects.DataIns;
import at.ac.uibk.metadata.api.model.functions.FunctionDeployment;

import java.util.List;

public class AtomicFunctionNode extends FunctionNode {

    private AtomicFunction atomicFunction;

    private FunctionDeployment schedulingDecision;

    private List<DataIns> scheduledDataIns;

    private Boolean forceSessionOverhead = null;

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

    public List<DataIns> getScheduledDataIns() {
        return scheduledDataIns;
    }

    public void setScheduledDataIns(List<DataIns> scheduledDataIns) {
        this.scheduledDataIns = scheduledDataIns;
    }

    public Boolean forceSessionOverhead() {
        return forceSessionOverhead;
    }

    public void setForceSessionOverhead(Boolean forceSessionOverhead) {
        this.forceSessionOverhead = forceSessionOverhead;
    }
}
