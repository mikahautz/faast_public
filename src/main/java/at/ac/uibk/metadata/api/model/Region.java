package at.ac.uibk.metadata.api.model;

import at.ac.uibk.metadata.api.Column;
import at.ac.uibk.metadata.api.IdColumn;
import at.ac.uibk.metadata.api.model.enums.Provider;
import at.ac.uibk.metadata.api.model.enums.ProviderConverter;
import com.fasterxml.jackson.annotation.JsonAlias;

import java.io.Serializable;

@Table(name = "region")
public class Region implements Serializable, Entity<Integer> {

    private static final long serialVersionUID = -6976637763705624872L;

    @IdColumn(name = "id", clazz = Integer.class)
    private Integer id;

    @Column(name = "region", clazz = String.class)
    private String region;

    @Column(name = "provider", clazz = Provider.class, converter = ProviderConverter.class)
    private Provider provider;

    @Column(name = "availability", clazz = Double.class)
    private Double availability;

    @Column(name = "providerID", clazz = Long.class)
    @JsonAlias({"providerID", "detailedProviderId"})
    private Long detailedProviderId;

    @Column(name = "location", clazz = String.class)
    private String location;

    @Column(name = "networkOverheadms", clazz = Double.class)
    private Double networkOverheadms;

    @Column(name = "overheadLoadms", clazz = Double.class)
    private Double overheadLoadms;

    @Column(name = "overheadBurstms", clazz = Double.class)
    private Double overheadBurstms;

    @Column(name = "invocationDelayLoadms", clazz = Double.class)
    private Double invocationDelayLoadms;

    @Column(name = "invocationDelayBurstms", clazz = Double.class)
    private Double invocationDelayBurstms;

    @Column(name = "concurrencyOverheadms", clazz = Double.class)
    private Double concurrencyOverheadms;

    @Column(name = "faasSystemOverheadms", clazz = Double.class)
    private Double faasSystemOverheadms;

    @Override
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public Provider getProvider() {
        return provider;
    }

    public void setProvider(Provider provider) {
        this.provider = provider;
    }

    public Double getAvailability() {
        return availability;
    }

    public void setAvailability(Double availability) {
        this.availability = availability;
    }

    public Long getDetailedProviderId() {
        return detailedProviderId;
    }

    public void setDetailedProviderId(Long detailedProviderId) {
        this.detailedProviderId = detailedProviderId;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Double getNetworkOverheadms() {
        return networkOverheadms;
    }

    public void setNetworkOverheadms(Double networkOverheadms) {
        this.networkOverheadms = networkOverheadms;
    }

    public Double getOverheadLoadms() {
        return overheadLoadms;
    }

    public void setOverheadLoadms(Double overheadLoadms) {
        this.overheadLoadms = overheadLoadms;
    }

    public Double getOverheadBurstms() {
        return overheadBurstms;
    }

    public void setOverheadBurstms(Double overheadBurstms) {
        this.overheadBurstms = overheadBurstms;
    }

    public Double getInvocationDelayLoadms() {
        return invocationDelayLoadms;
    }

    public void setInvocationDelayLoadms(Double invocationDelayLoadms) {
        this.invocationDelayLoadms = invocationDelayLoadms;
    }

    public Double getInvocationDelayBurstms() {
        return invocationDelayBurstms;
    }

    public void setInvocationDelayBurstms(Double invocationDelayBurstms) {
        this.invocationDelayBurstms = invocationDelayBurstms;
    }

    public Double getConcurrencyOverheadms() {
        return concurrencyOverheadms;
    }

    public void setConcurrencyOverheadms(Double concurrencyOverheadms) {
        this.concurrencyOverheadms = concurrencyOverheadms;
    }

    public Double getFaasSystemOverheadms() {
        return faasSystemOverheadms;
    }

    public void setFaasSystemOverheadms(Double faasSystemOverheadms) {
        this.faasSystemOverheadms = faasSystemOverheadms;
    }
}
