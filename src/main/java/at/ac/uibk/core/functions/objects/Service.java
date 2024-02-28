package at.ac.uibk.core.functions.objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.Objects;

/**
 * This class describes a service which provides information about the used services of a function.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "name",
        "serviceType",
        "workPerUnit",
        "amountOfUnits",
        "source",
        "target",
        "dataInRef",
        "dataOutRef",
        "dbServiceRegion",
})
public class Service {

    /**
     * The name/identifier of the service
     */
    @JsonProperty("name")
    private String name;

    /**
     * The type of the service
     */
    @JsonProperty("serviceType")
    private String serviceType;

    /**
     * Specifies how much work is done per unit
     */
    @JsonProperty("workPerUnit")
    private Double workPerUnit;

    /**
     * Specifies how many units are processed
     */
    @JsonProperty("amountOfUnits")
    private Integer amountOfUnits;

    /**
     * The source region of the service
     */
    @JsonProperty("source")
    private String source;

    /**
     * The target region of the service
     */
    @JsonProperty("target")
    private String target;

    /**
     * Optional. References a dataIn if a service needs to specify additional data for the dataIn
     */
    @JsonProperty("dataInRef")
    private String dataInRef;

    /**
     * Optional. References a dataOut if a service needs to specify additional data for the dataOut
     */
    @JsonProperty("dataOutRef")
    private String dataOutRef;

    /**
     * Optional. Specifies which region was used for the RTT of a service stored in the DB. Used to subtract the service
     * time from the stored time
     */
    @JsonProperty("dbServiceRegion")
    private String dbServiceRegion;

    public Service() {
    }

    public Service(Service original) {
        this.name = original.name;
        this.serviceType = original.serviceType;
        this.workPerUnit = original.workPerUnit;
        this.amountOfUnits = original.amountOfUnits;
        this.source = original.source;
        this.target = original.target;
        this.dataInRef = original.dataInRef;
        this.dataOutRef = original.dataOutRef;
        this.dbServiceRegion = original.dbServiceRegion;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public Double getWorkPerUnit() {
        return workPerUnit;
    }

    public void setWorkPerUnit(Double workPerUnit) {
        this.workPerUnit = workPerUnit;
    }

    public Integer getAmountOfUnits() {
        return amountOfUnits;
    }

    public void setAmountOfUnits(Integer amountOfUnits) {
        this.amountOfUnits = amountOfUnits;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getDataInRef() {
        return dataInRef;
    }

    public void setDataInRef(String dataInRef) {
        this.dataInRef = dataInRef;
    }

    public String getDataOutRef() {
        return dataOutRef;
    }

    public void setDataOutRef(String dataOutRef) {
        this.dataOutRef = dataOutRef;
    }

    public String getDbServiceRegion() {
        return dbServiceRegion;
    }

    public void setDbServiceRegion(String dbServiceRegion) {
        this.dbServiceRegion = dbServiceRegion;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Service service = (Service) o;
        return Objects.equals(name, service.name) && Objects.equals(serviceType, service.serviceType) && Objects.equals(workPerUnit, service.workPerUnit) && Objects.equals(amountOfUnits, service.amountOfUnits) && Objects.equals(source, service.source) && Objects.equals(target, service.target) && Objects.equals(dataInRef, service.dataInRef) && Objects.equals(dataOutRef, service.dataOutRef) && Objects.equals(dbServiceRegion, service.dbServiceRegion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, serviceType, workPerUnit, amountOfUnits, source, target, dataInRef, dataOutRef, dbServiceRegion);
    }
}

