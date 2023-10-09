package at.ac.uibk.util;

import at.ac.uibk.core.functions.objects.DataIns;
import at.ac.uibk.metadata.api.model.DataTransfer;
import at.ac.uibk.metadata.api.model.Region;
import at.ac.uibk.scheduler.api.MetadataCache;
import at.ac.uibk.scheduler.api.node.AtomicFunctionNode;

import java.util.Map;

public class LogEntry {
    private final AtomicFunctionNode functionNode;

    private final Region region;

    private double RTT;

    private final double downloadTime;

    private final double uploadTime;

    private final Map<DataIns, DataTransfer> scheduledDataUpload;

    public LogEntry(AtomicFunctionNode functionNode, Region region, double RTT, double downloadTime, double uploadTime,
                    Map<DataIns, DataTransfer> scheduledDataUpload) {
        this.functionNode = functionNode;
        this.region = region;
        this.RTT = RTT;
        this.downloadTime = downloadTime;
        this.uploadTime = uploadTime;
        this.scheduledDataUpload = scheduledDataUpload;
    }

    public AtomicFunctionNode getFunctionNode() {
        return functionNode;
    }

    public Region getRegion() {
        return region;
    }

    public double getRTT() {
        return RTT;
    }

    public double getDownloadTime() {
        return downloadTime;
    }

    public double getUploadTime() {
        return uploadTime;
    }

    public Map<DataIns, DataTransfer> getScheduledDataUpload() {
        return scheduledDataUpload;
    }

    public void addToRTT(int time) {
        this.RTT += time;
    }

    public void print() {
        StringBuilder storageInfo = new StringBuilder();

        for (Map.Entry<DataIns, DataTransfer> entry : scheduledDataUpload.entrySet()) {
            Region storageRegion = MetadataCache.get().getRegionsById().get(entry.getValue().getStorageRegionID().intValue());
            storageInfo.append(entry.getKey().getName()).append(": ").append(storageRegion.getProvider())
                    .append(" ").append(storageRegion.getRegion()).append(", ");
        }

        if (storageInfo.length() > 0) {
            storageInfo.delete(storageInfo.length() - 2, storageInfo.length());
        }

        System.out.printf("%-25s | %9.2fms | %15.2fms | %11s | %11s | %-10s | %-18s | %-15s%n",
                this.functionNode.getAtomicFunction().getName(), this.RTT, this.RTT - this.downloadTime - this.uploadTime,
                this.downloadTime == 0 ? "-" : String.format("%.2fms", this.downloadTime),
                this.uploadTime == 0 ? "-" : String.format("%.2fms", this.uploadTime),
                this.region.getProvider(), this.region.getRegion(), storageInfo);
    }
}
