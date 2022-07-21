package at.ac.uibk.metadata.api.model;

import at.ac.uibk.metadata.api.Column;
import at.ac.uibk.metadata.api.IdColumn;

@Table(name = "functiontype")
public class FunctionType implements Entity<Long> {

    @IdColumn(name = "id", clazz = Long.class)
    private Long id;

    @Column(name = "name", clazz = String.class)
    private String name;

    @Column(name = "type", clazz = String.class)
    private String type;

    @Column(name = "avgRTT", clazz = Double.class)
    private Double avgRTT;

    @Column(name = "avgCost", clazz = Double.class)
    private Double avgCost;

    @Column(name = "successRate", clazz = Double.class)
    private Double successRate;

    @Column(name = "invocations", clazz = int.class)
    private int invocations;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Double getAvgRTT() {
        return avgRTT;
    }

    public void setAvgRTT(Double avgRTT) {
        this.avgRTT = avgRTT;
    }

    public Double getAvgCost() {
        return avgCost;
    }

    public void setAvgCost(Double avgCost) {
        this.avgCost = avgCost;
    }

    public Double getSuccessRate() {
        return successRate;
    }

    public void setSuccessRate(Double successRate) {
        this.successRate = successRate;
    }

    public int getInvocations() {
        return invocations;
    }

    public void setInvocations(int invocations) {
        this.invocations = invocations;
    }
}
