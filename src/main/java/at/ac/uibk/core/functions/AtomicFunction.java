package at.ac.uibk.core.functions;

import at.ac.uibk.core.Function;
import at.ac.uibk.core.functions.objects.DataIns;
import at.ac.uibk.core.functions.objects.DataOuts;
import at.ac.uibk.core.functions.objects.DataOutsAtomic;
import com.fasterxml.jackson.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * This class describes an atomic function
 *
 * @author stefanpedratscher
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "name",
        "type",
        "deployment",
        "dataIns",
        "dataOuts"
})
@JsonTypeName("function")
@JsonTypeInfo(include = JsonTypeInfo.As.WRAPPER_OBJECT, use = JsonTypeInfo.Id.NAME)
public class AtomicFunction extends Function {

    /**
     * Function type of the Atomic Function. Represents an abstract description of a group of function implementations.
     */
    @JsonProperty("type")
    private String type;

    /**
     * Function deployment of the Atomic Function. Represents the name of the deployed function, the provider, the
     * region and memory specification.
     */
    @JsonProperty("deployment")
    private String deployment;

    /**
     * Data input ports of the atomic function ({@link DataIns})
     */
    @JsonProperty("dataIns")
    private List<DataIns> dataIns;

    /**
     * Data output ports of the atomic function ({@link DataOuts})
     */
    @JsonProperty("dataOuts")
    private List<DataOutsAtomic> dataOuts;

    @JsonIgnore
    private Map<String, Object> additionalPropertiesAtomicFunction = new HashMap<>();

    public AtomicFunction() {
    }

    /**
     * Constructor for atomic function
     *
     * @param name       Unique identifier of the compound
     * @param type       Function type of the Atomic Function
     * @param deployment Function deployment of the Atomic Function
     * @param dataIns    Data input ports ({@link DataIns})
     * @param dataOuts   Data output ports ({@link DataOuts})
     */
    public AtomicFunction(String name, String type, String deployment, List<DataIns> dataIns, List<DataOutsAtomic> dataOuts) {
        this.name = name;
        this.type = type;
        this.deployment = deployment;
        this.dataIns = dataIns;
        this.dataOuts = dataOuts;
    }

    /**
     * Getter and Setter
     */

    @JsonProperty("type")
    public String getType() {
        return type;
    }

    @JsonProperty("type")
    public void setType(String type) {
        this.type = type;
    }

    @JsonProperty("deployment")
    public String getDeployment() {
        return deployment;
    }

    @JsonProperty("deployment")
    public void setDeployment(String deployment) {
        this.deployment = deployment;
    }

    @JsonProperty("dataIns")
    public List<DataIns> getDataIns() {
        return dataIns;
    }

    @JsonProperty("dataIns")
    public void setDataIns(List<DataIns> dataIns) {
        this.dataIns = dataIns;
    }

    @JsonProperty("dataOuts")
    public List<DataOutsAtomic> getDataOuts() {
        return dataOuts;
    }

    @JsonProperty("dataOuts")
    public void setDataOuts(List<DataOutsAtomic> dataOuts) {
        this.dataOuts = dataOuts;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return additionalPropertiesAtomicFunction;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        additionalPropertiesAtomicFunction.put(name, value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AtomicFunction that = (AtomicFunction) o;
        return super.equals(o) &&
                Objects.equals(type, that.type) &&
                Objects.equals(deployment, that.deployment) &&
                Objects.equals(dataIns, that.dataIns) &&
                Objects.equals(dataOuts, that.dataOuts) &&
                Objects.equals(additionalPropertiesAtomicFunction, that.additionalPropertiesAtomicFunction);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), type, deployment, dataIns, dataOuts, additionalPropertiesAtomicFunction);
    }
}
