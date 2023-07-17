package at.ac.uibk.scheduler.storeless;

import at.ac.uibk.core.Function;
import at.ac.uibk.core.Workflow;
import at.ac.uibk.core.functions.*;
import at.ac.uibk.core.functions.objects.DataIns;
import at.ac.uibk.core.functions.objects.DataOuts;
import at.ac.uibk.core.functions.objects.DataOutsAtomic;
import at.ac.uibk.core.functions.objects.PropertyConstraint;
import at.ac.uibk.scheduler.api.SchedulingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Class that is used to store all dataIns and dataOuts of all nodes and the workflow.
 */
public class DataFlowStore {

    private static final Map<String, DataIns> dataIns = new HashMap<>();

    private static final Map<String, DataOuts> dataOuts = new HashMap<>();

    public static void storeInputsAndOutputs(Workflow wf, Path input) throws Exception {
        addDataIns(wf.getName(), wf.getDataIns());
        addDataOuts(wf.getName(), wf.getDataOuts());
        wf.getWorkflowBody().forEach(DataFlowStore::handleFunction);
        storeInputJson(input, wf.getName());
    }

    public static String getDataInValue(String source, boolean isRecursive) {
        // if the source is a list of sources
        if (source.contains(",")) {
            List<String> values = new ArrayList<>();
            if (source.startsWith("[") && source.endsWith("]")) {
                source = source.substring(1, source.length() - 1);
            }
            String[] sources = source.split(",");
            for (String s : sources) {
                values.add(getDataInValue(source, true));
            }
            // if any value is null, meaning no value could be found for that source
            if (values.stream().anyMatch(Objects::isNull)) {
                throw new SchedulingException("Could not find values for all sources: '" + source + "'");
            } else {
                String result = values.toString();
                return result.substring(1, result.length() - 1);
            }
        }

        if (dataOuts.containsKey(source)) {
            DataOuts dataOut = dataOuts.get(source);
            if (dataOut.getValue() != null && !dataOut.getValue().isEmpty()) {
                return dataOut.getValue();
            } else {
                // go through all possible dataOuts, if we get a value then use it, otherwise continue with the dataIns
                String result = getDataInValue(dataOut.getSource(), true);
                if (result != null) {
                    return result;
                }
            }
        }

        if (dataIns.containsKey(source)) {
            DataIns dataIn = dataIns.get(source);
            if (dataIn.getValue() != null && !dataIn.getValue().isEmpty()) {
                return dataIn.getValue();
            } else {
                String result = getDataInValue(dataIn.getSource(), true);
                if (result != null) {
                    return result;
                }
            }
        }

        // if it is a recursive call, we just might have finished looping through all possibilities of one path
        if (isRecursive) {
            return null;
            // if it is the initial call, we were unable to find a value for the source
        } else {
            throw new SchedulingException("Unable to find value for '" + source + "'");
        }
    }

    public static void updateValuesInStore(String functionName, DataIns dataIn, List<DataOutsAtomic> dataOuts, boolean universalDestination) {
        String value = dataIn.getValue();
        DataFlowStore.getDataIns().get(functionName + "/" + dataIn.getName()).setValue(value);

        if (universalDestination) {
            for (DataOutsAtomic dataOut : dataOuts) {
                DataFlowStore.getDataOuts().get(functionName + "/" + dataOut.getName()).setValue(value);
            }
        // we have multiple dataIns that specify an output destination, therefore we have to check which dataOut uses which output destination
        } else {
            final Supplier<SchedulingException> propertyIsMissing =
                    () -> new SchedulingException(dataIn.getName() + ": Multiple different outputs are defined, but property 'uploadId' is missing!");

            Integer uploadId = null;
            List<PropertyConstraint> properties = dataIn.getProperties();
            if (properties != null && !properties.isEmpty()) {
                for (PropertyConstraint property : properties) {
                    if (property.getName().equalsIgnoreCase("uploadId")) {
                        try {
                            uploadId = Integer.parseInt(property.getValue());
                        } catch (NumberFormatException e) {
                            throw new SchedulingException(dataIn.getName() + ": Property 'uploadId' has to be an Integer!");
                        }
                    }
                }
                if (uploadId == null) {
                    throw propertyIsMissing.get();
                }
            } else {
                throw propertyIsMissing.get();
            }

            for (DataOutsAtomic dataOut : dataOuts) {
                if (hasUploadId(dataOut, uploadId)) {
                    DataFlowStore.getDataOuts().get(functionName + "/" + dataOut.getName()).setValue(value);
                }
            }
        }
    }

    private static boolean hasUploadId(DataOutsAtomic dataOut, Integer id) {
        List<PropertyConstraint> properties = dataOut.getProperties();
        try {
            if (properties != null && !properties.isEmpty()) {
                for (PropertyConstraint property : properties) {
                    if (property.getName().equalsIgnoreCase("uploadId") &&
                            Integer.parseInt(property.getValue()) == id) {
                        return true;
                    }
                }
            }
        } catch (NumberFormatException e) {
            throw new SchedulingException(dataOut.getName() + ": Property 'uploadId' has to be an Integer!");
        }
        return false;
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

    private static void storeInputJson(Path input, String workflowName) throws Exception {
        if (input != null) {
            try (final InputStream is = Files.newInputStream(input)) {
                ObjectMapper objectMapper = new ObjectMapper();
                Map<String, Object> jsonMap = objectMapper.readValue(is, new TypeReference<Map<String, Object>>() {
                });

                for (Map.Entry<String, Object> entry : jsonMap.entrySet()) {
                    String key = entry.getKey();
                    Object value = entry.getValue();

                    if (value instanceof String) {
                        addValueToDataIns(workflowName, key, value.toString());
                    } else if (value instanceof List) {
                        String stringValue = value.toString();
                        stringValue = stringValue.substring(1, stringValue.length() - 1);
                        addValueToDataIns(workflowName, key, stringValue);
                    }
                }
            }
        }
    }

    private static void addValueToDataIns(String workflowName, String key, String value) {
        key = workflowName + "/" + key;
        if (dataIns.containsKey(key)) {
            DataIns dataIn = dataIns.get(key);
            dataIn.setValue(value);

            dataIns.put(key, dataIn);
        }
    }

    public static Map<String, DataIns> getDataIns() {
        return dataIns;
    }

    public static Map<String, DataOuts> getDataOuts() {
        return dataOuts;
    }
}
