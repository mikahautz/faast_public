package at.ac.uibk.metadata.api.model.vm;

import at.ac.uibk.metadata.api.Column;
import at.ac.uibk.metadata.api.model.AdditionalServiceType;
import at.ac.uibk.metadata.api.model.Table;
import at.ac.uibk.metadata.api.model.enums.*;
import com.amazonaws.services.ec2.model.InstanceType;

@Table(name = "vm_deployment")
public class VmDeployment extends AdditionalServiceType {

    @Column(name = "description", clazz = String.class)
    private String description;

    @Column(name = "provider", clazz = String.class, converter = ProviderConverter.class)
    private Provider provider;

    @Column(name = "ami_id", clazz = String.class)
    private String amiId;

    @Column(name = "instanceType", clazz = InstanceType.class, converter = InstanceTypeConverter.class)
    private InstanceType instanceType;

    @Column(name = "blockDeviceName", clazz = String.class)
    private String blockDeviceName;

    @Column(name = "blockDeviceType", clazz = BlockDeviceType.class, converter = BlockDeviceTypeConverter.class)
    private BlockDeviceType blockDeviceType;

    @Column(name = "blockDeviceDeleteOnTermination", clazz = boolean.class)
    private boolean blockDeviceDeleteOnTermination;

    @Column(name = "blockDeviceVolumeSize", clazz = Integer.class)
    private Integer blockDeviceVolumeSize;

    @Column(name = "blockDeviceVolumeType", clazz = BlockDeviceVolumeType.class, converter = BlockDeviceVolumeTypeConverter.class)
    private BlockDeviceVolumeType blockDeviceVolumeType;

    @Column(name = "hostname", clazz = String.class)
    private String hostname;

    @Column(name = "domain", clazz = String.class)
    private String domain;

    @Column(name = "datacenter", clazz = String.class)
    private String datacenter;

    @Column(name = "memory", clazz = Integer.class)
    private Integer memory;

    @Column(name = "runningStatus", clazz = VmStatus.class, converter = VmStatusConverter.class)
    private VmStatus runningStatus;

    @Column(name = "ip", clazz = String.class)
    private String ip;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Provider getProvider() {
        return provider;
    }

    public void setProvider(Provider provider) {
        this.provider = provider;
    }

    public String getAmiId() {
        return amiId;
    }

    public void setAmiId(String amiId) {
        this.amiId = amiId;
    }

    public InstanceType getInstanceType() {
        return instanceType;
    }

    public void setInstanceType(InstanceType instanceType) {
        this.instanceType = instanceType;
    }

    public String getBlockDeviceName() {
        return blockDeviceName;
    }

    public void setBlockDeviceName(String blockDeviceName) {
        this.blockDeviceName = blockDeviceName;
    }

    public BlockDeviceType getBlockDeviceType() {
        return blockDeviceType;
    }

    public void setBlockDeviceType(BlockDeviceType blockDeviceType) {
        this.blockDeviceType = blockDeviceType;
    }

    public boolean isBlockDeviceDeleteOnTermination() {
        return blockDeviceDeleteOnTermination;
    }

    public void setBlockDeviceDeleteOnTermination(boolean blockDeviceDeleteOnTermination) {
        this.blockDeviceDeleteOnTermination = blockDeviceDeleteOnTermination;
    }

    public Integer getBlockDeviceVolumeSize() {
        return blockDeviceVolumeSize;
    }

    public void setBlockDeviceVolumeSize(Integer blockDeviceVolumeSize) {
        this.blockDeviceVolumeSize = blockDeviceVolumeSize;
    }

    public BlockDeviceVolumeType getBlockDeviceVolumeType() {
        return blockDeviceVolumeType;
    }

    public void setBlockDeviceVolumeType(BlockDeviceVolumeType blockDeviceVolumeType) {
        this.blockDeviceVolumeType = blockDeviceVolumeType;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getDatacenter() {
        return datacenter;
    }

    public void setDatacenter(String datacenter) {
        this.datacenter = datacenter;
    }

    public Integer getMemory() {
        return memory;
    }

    public void setMemory(Integer memory) {
        this.memory = memory;
    }

    public VmStatus getRunningStatus() {
        return runningStatus;
    }

    public void setRunningStatus(VmStatus runningStatus) {
        this.runningStatus = runningStatus;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }
}
