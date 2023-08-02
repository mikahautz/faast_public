package at.ac.uibk.metadata.api.model;

import at.ac.uibk.metadata.api.Column;
import at.ac.uibk.metadata.api.IdColumn;
import at.ac.uibk.metadata.api.model.enums.DataTransferType;
import at.ac.uibk.metadata.api.model.enums.DataTransferTypeConverter;
import com.fasterxml.jackson.annotation.JsonAlias;

import java.io.Serializable;

public class DataTransfer implements Serializable, Entity<Long> {

    private static final long serialVersionUID = -3097154798986909607L;

    @IdColumn(name = "id", clazz = Integer.class)
    private Long id;

    @Column(name = "type", clazz = DataTransferType.class, converter = DataTransferTypeConverter.class)
    private DataTransferType type;

    @Column(name = "functionRegionID", clazz = Long.class)
    @JsonAlias({"functionRegionID", "functionRegionId"})
    private Long functionRegionID;

    @Column(name = "storageRegionID", clazz = Long.class)
    @JsonAlias({"storageRegionID", "storageRegionId"})
    private Long storageRegionID;

    @Column(name = "bandwidth", clazz = Double.class)
    private Double bandwidth;

    @Column(name = "latency", clazz = int.class)
    private int latency;

    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public DataTransferType getType() {
        return type;
    }

    public void setType(DataTransferType type) {
        this.type = type;
    }

    public Long getFunctionRegionID() {
        return functionRegionID;
    }

    public void setFunctionRegionID(Long functionRegionID) {
        this.functionRegionID = functionRegionID;
    }

    public Long getStorageRegionID() {
        return storageRegionID;
    }

    public void setStorageRegionID(Long storageRegionID) {
        this.storageRegionID = storageRegionID;
    }

    public Double getBandwidth() {
        return bandwidth;
    }

    public void setBandwidth(Double bandwidth) {
        this.bandwidth = bandwidth;
    }

    public int getLatency() {
        return latency;
    }

    public void setLatency(int latency) {
        this.latency = latency;
    }
}
