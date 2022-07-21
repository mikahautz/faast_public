package at.ac.uibk.scheduler.faast;

import java.util.Comparator;
import java.util.Objects;

public class PlannedExecution implements Comparable<PlannedExecution> {

    private double startTime;

    private double endTime;

    public PlannedExecution(double startTime, double endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlannedExecution)) return false;
        PlannedExecution that = (PlannedExecution) o;
        return Double.compare(that.startTime, startTime) == 0 && Double.compare(that.endTime, endTime) == 0;
    }

    public double getStartTime() {
        return startTime;
    }

    public double getEndTime() {
        return endTime;
    }

    @Override
    public int hashCode() {
        return Objects.hash(startTime, endTime);
    }

    @Override
    public int compareTo(PlannedExecution plannedExecution) {
        return Comparator.comparing(PlannedExecution::getStartTime).thenComparing(PlannedExecution::getEndTime).compare(this, plannedExecution);
    }
}
