package at.ac.uibk.scheduler.api.node;

import at.ac.uibk.core.functions.AtomicFunction;
import at.ac.uibk.core.functions.objects.DataIns;
import at.ac.uibk.core.functions.objects.DataOutsAtomic;
import at.ac.uibk.metadata.api.model.functions.FunctionDeployment;

import javax.xml.crypto.Data;
import java.util.List;

public class AtomicFunctionNode extends FunctionNode {

    private AtomicFunction atomicFunction;

    private FunctionDeployment schedulingDecision;

    private List<DataIns> scheduledDataIns;

    private List<DataOutsAtomic> scheduledDataOuts;

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

    public List<DataOutsAtomic> getScheduledDataOuts() {
        return scheduledDataOuts;
    }

    public void setScheduledDataOuts(List<DataOutsAtomic> scheduledDataOuts) {
        this.scheduledDataOuts = scheduledDataOuts;
    }
}
