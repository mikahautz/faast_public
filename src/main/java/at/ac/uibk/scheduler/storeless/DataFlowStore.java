package at.ac.uibk.scheduler.storeless;

import at.ac.uibk.core.Function;
import at.ac.uibk.core.Workflow;
import at.ac.uibk.core.functions.*;
import at.ac.uibk.core.functions.objects.DataIns;
import at.ac.uibk.core.functions.objects.DataOuts;
import at.ac.uibk.core.functions.objects.DataOutsAtomic;
import at.ac.uibk.core.functions.objects.PropertyConstraint;
import at.ac.uibk.scheduler.api.SchedulingException;
import at.ac.uibk.util.HeftUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jgrapht.alg.util.Triple;

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

    /**
     * A map containing all dataIns of the whole workflow.
     */
    private static final Map<String, DataIns> dataIns = new HashMap<>();

    /**
     * A map containing all dataOuts of the whole workflow.
     */
    private static final Map<String, DataOuts> dataOuts = new HashMap<>();

    private static String bucketPrefix;

    private static String bucketSuffix;

    /**
     * Stores all data flow inputs and outputs of the workflow and the input json file in a map. The map is used to
     * access the storage destinations of (intermediate) data.
     *
     * @param wf    the workflow to get the dataIns and dataOuts
     * @param input the path to the input json
     *
     * @throws Exception if the input json path is not found
     */
    public static void storeInputsAndOutputs(Workflow wf, Path input) throws Exception {
        addDataIns(wf.getName(), wf.getDataIns());
        addDataOuts(wf.getName(), wf.getDataOuts());
        wf.getWorkflowBody().forEach(DataFlowStore::handleFunction);
        storeInputJson(input, wf.getName());
    }

    public static void setBucketPrefixAndSuffix(String prefix, String suffix) {
        bucketPrefix = prefix;
        bucketSuffix = suffix;
    }

    /**
     * Get the concrete value of a {@link DataIns} object identified by the {@code source}.
     *
     * @param source      to identify the {@link DataIns} object
     * @param isRecursive determines if the method is called recursively
     *
     * @return the value of the {@link DataIns} object as a string
     */
    public static Triple<List<String>, List<Integer>, List<Double>> getDataInValue(String source, boolean isRecursive) {
        // if the source is a list of sources
        if (source != null && source.contains(",")) {
            List<Triple<List<String>, List<Integer>, List<Double>>> results = new ArrayList<>();
            if (source.startsWith("[") && source.endsWith("]")) {
                source = source.substring(1, source.length() - 1);
            }
            String[] sources = source.split(",");
            for (String s : sources) {
                results.add(getDataInValue(s, true));
            }
            // if any value is null, meaning no value could be found for that source
            if (results.stream().anyMatch(Objects::isNull)) {
                throw new SchedulingException("Could not find values for all sources: '" + source + "'");
            } else {
                List<String> values = new ArrayList<>();
                List<Integer> fileAmounts = new ArrayList<>();
                List<Double> fileSizes = new ArrayList<>();
                for (Triple<List<String>, List<Integer>, List<Double>> entry : results) {
                    values.addAll(entry.getFirst());
                    if (entry.getSecond() != null && entry.getSecond().stream().noneMatch(Objects::isNull)) {
                        fileAmounts.addAll(entry.getSecond());
                    }
                    if (entry.getThird() != null && entry.getThird().stream().noneMatch(Objects::isNull)) {
                        fileSizes.addAll(entry.getThird());
                    }
                }

                return new Triple<>(values, fileAmounts, fileSizes);
            }
        }

        if (dataOuts.containsKey(source)) {
            DataOuts dataOut = dataOuts.get(source);
            if (dataOut.getValue() != null && !dataOut.getValue().isEmpty()) {
                return new Triple<>(List.of(dataOut.getValue()), HeftUtil.extractFileAmount(dataOut), HeftUtil.extractFileSize(dataOut));
            } else {
                // go through all possible dataOuts, if we get a value then use it, otherwise continue with the dataIns
                Triple<List<String>, List<Integer>, List<Double>> result = getDataInValue(dataOut.getSource(), true);
                if (result != null) {
                    return result;
                }
            }
        }

        if (dataIns.containsKey(source)) {
            DataIns dataIn = dataIns.get(source);
            if (dataIn.getValue() != null && !dataIn.getValue().isEmpty()) {
                return new Triple<>(List.of(dataIn.getValue()), HeftUtil.extractFileAmount(dataIn, true), HeftUtil.extractFileSize(dataIn, true));
            } else {
                Triple<List<String>, List<Integer>, List<Double>> result = getDataInValue(dataIn.getSource(), true);
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

    /**
     * Updates the {@link #dataOuts} map with concrete values given by {@code dataIns}.
     *
     * @param functionName         the name of the function whose dataOuts should be updated
     * @param dataIns              the dataIns that contain the values to be updated
     * @param dataOuts             the dataOuts to be updated
     * @param universalDestination if all dataOuts should be updated with the same destination, or if individual
     *                             destinations are given
     */
    public static void updateValuesInStore(String functionName, List<DataIns> dataIns, List<DataOutsAtomic> dataOuts, boolean universalDestination) {

        for (DataIns dataIn : dataIns) {
            String value = dataIn.getValue();
            DataFlowStore.getDataIns().get(functionName + "/" + dataIn.getName()).setValue(value);

            if (universalDestination) {
                for (DataOutsAtomic dataOut : dataOuts) {
                    DataOuts tmp = DataFlowStore.getDataOuts().get(functionName + "/" + dataOut.getName());
                    tmp.setValue(value);
                    if (tmp.getProperties() == null) tmp.setProperties(new ArrayList<>());
                    if (dataIn.getProperties() != null) tmp.getProperties().addAll(dataIn.getProperties());
                }
            } else {
                // we have multiple dataIns that specify an output destination, therefore we have to check which dataOut uses which output destination
                final Supplier<SchedulingException> propertyIsMissing =
                        () -> new SchedulingException(dataIn.getName() + ": Multiple different outputs are defined, but property 'uploadId' is missing!");

                // TODO check if names are the same of dataIn and dataOut
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
                        DataOuts tmp = DataFlowStore.getDataOuts().get(functionName + "/" + dataOut.getName());
                        tmp.setValue(value);
                        if (tmp.getProperties() == null) tmp.setProperties(new ArrayList<>());
                        if (dataIn.getProperties() != null) tmp.getProperties().addAll(dataIn.getProperties());
                    }
                }
            }
        }
    }

    /**
     * Checks if a {@link DataOutsAtomic} object has a property {@code uploadId} with the given id.
     *
     * @param dataOut the object to check
     * @param id      the id to check for
     *
     * @return true if the object has the id, false otherwise
     */
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

    /**
     * Check the given function for its type and add all dataIns and dataOuts to the respective maps.
     *
     * @param f the function to check
     */
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

    /**
     * Add the given dataIns to the map of dataIns.
     *
     * @param name        the name of the parent
     * @param dataInsList the list of dataIns to add to the map
     */
    private static void addDataIns(String name, List<DataIns> dataInsList) {
        if (dataInsList != null) {
            for (DataIns d : dataInsList) {
                dataIns.put(name + "/" + d.getName(), d);
            }
        }
    }

    /**
     * Add the given dataOuts to the map of dataOuts.
     *
     * @param name         the name of the parent
     * @param dataOutsList the list of dataOuts to add to the map
     */
    private static void addDataOuts(String name, List<DataOuts> dataOutsList) {
        if (dataOutsList != null) {
            for (DataOuts d : dataOutsList) {
                dataOuts.put(name + "/" + d.getName(), d);
            }
        }
    }

    /**
     * Stores the input json in the {@link #dataIns} map.
     *
     * @param input        the path to the input json file
     * @param workflowName the name of the workflow
     *
     * @throws Exception if the input json file is not found
     */
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

    /**
     * Adds a concrete value to an object in {@link #dataIns} identified by {@code key}.
     *
     * @param workflowName the name of the workflow
     * @param key          the key to identify the object in the map
     * @param value        the concrete value to set
     */
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

    public static String getBucketPrefix() {
        return bucketPrefix;
    }

    public static String getBucketSuffix() {
        return bucketSuffix;
    }
}
