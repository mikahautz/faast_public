package at.ac.uibk.util;

import at.ac.uibk.core.functions.AtomicFunction;
import at.ac.uibk.scheduler.api.node.AtomicFunctionNode;

public class LogEntry {
    private final AtomicFunctionNode functionNode;

    private double RTT;

    private final double downloadTime;

    private final double uploadTime;

    public LogEntry(AtomicFunctionNode functionNode, double RTT, double downloadTime, double uploadTime) {
        this.functionNode = functionNode;
        this.RTT = RTT;
        this.downloadTime = downloadTime;
        this.uploadTime = uploadTime;
    }

    public AtomicFunctionNode getFunctionNode() {
        return functionNode;
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

    public void addToRTT(int time) {
        this.RTT += time;
    }
}
