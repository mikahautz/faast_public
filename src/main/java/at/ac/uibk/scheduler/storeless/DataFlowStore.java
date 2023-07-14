package at.ac.uibk.scheduler.storeless;

import at.ac.uibk.core.Function;
import at.ac.uibk.core.Workflow;
import at.ac.uibk.core.functions.*;
import at.ac.uibk.core.functions.objects.DataIns;
import at.ac.uibk.core.functions.objects.DataOuts;
import at.ac.uibk.core.functions.objects.DataOutsAtomic;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Class that is used to store all dataIns and dataOuts of all nodes and the workflow.
 */
public class DataFlowStore {

    private static final Map<String, DataIns> dataIns = new HashMap<>();

    private static final Map<String, DataOuts> dataOuts = new HashMap<>();
    ;

    public static void storeInputsAndOutputs(Workflow wf) {
        addDataIns(wf.getName(), wf.getDataIns());
        addDataOuts(wf.getName(), wf.getDataOuts());
        wf.getWorkflowBody().forEach(DataFlowStore::handleFunction);
    }

    private static void handleFunction(Function f) {
        if (f instanceof AtomicFunction) {
            addDataIns(f.getName(), ((AtomicFunction) f).getDataIns());
            addDataOuts(f.getName(), ((AtomicFunction) f).getDataOuts().stream()
                    .map(DataOutsAtomic::toDataOuts)
                    .collect(Collectors.toList()));
        } else if (f instanceof Compound) {
            addDataIns(f.getName(), ((Compound) f).getDataIns());
            addDataOuts(f.getName(), ((Compound) f).getDataOuts());

            if (f instanceof IfThenElse) {
                ((IfThenElse) f).getThen().forEach(DataFlowStore::handleFunction);
                ((IfThenElse) f).getElse().forEach(DataFlowStore::handleFunction);
            } else if (f instanceof Parallel) {
                ((Parallel) f).getParallelBody().forEach(s -> s.getSection().forEach(DataFlowStore::handleFunction));
            } else if (f instanceof ParallelFor) {
                ((ParallelFor) f).getLoopBody().forEach(DataFlowStore::handleFunction);
            } else if (f instanceof Sequence) {
                ((Sequence) f).getSequenceBody().forEach(DataFlowStore::handleFunction);
            } else if (f instanceof Switch) {
                ((Switch) f).getDefault().forEach(DataFlowStore::handleFunction);
                ((Switch) f).getCases().forEach(c -> c.getFunctions().forEach(DataFlowStore::handleFunction));
            }
        }
    }

    private static void addDataIns(String name, List<DataIns> dataInsList) {
        if (dataInsList != null) {
            for (DataIns d : dataInsList) {
                dataIns.put(name + "/" + d.getName(), d);
            }
        }
    }

    private static void addDataOuts(String name, List<DataOuts> dataOutsList) {
        if (dataOutsList != null) {
            for (DataOuts d : dataOutsList) {
                dataOuts.put(name + "/" + d.getName(), d);
            }
        }
    }

    public static Map<String, DataIns> getDataIns() {
        return dataIns;
    }

    public static Map<String, DataOuts> getDataOuts() {
        return dataOuts;
    }
}
