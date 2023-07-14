package at.ac.uibk.scheduler.api;

import at.ac.uibk.scheduler.faast.PlannedExecution;
import at.ac.uibk.scheduler.storeless.RegionResource;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicLong;

public class AbstractResource {
    public static final AtomicLong objectCounter = new AtomicLong(0);

    private final long id;

    private final SortedSet<PlannedExecution> plannedExecutions = new TreeSet<>();

    public AbstractResource() {
        this.id = RegionResource.objectCounter.getAndIncrement();
    }

    public boolean hasPlannedExecutionInTimeFrame(final double start, final double end) {
        return this.getPlannedExecutions()
                .stream().anyMatch(plannedExecution -> {
                    boolean crossesStart =
                            start <= plannedExecution.getStartTime() && plannedExecution.getStartTime() <= end;

                    boolean crossesEnd =
                            start <= plannedExecution.getEndTime() && plannedExecution.getEndTime() <= end;

                    boolean isInBetween =
                            plannedExecution.getStartTime() <= start && end <= plannedExecution.getEndTime();

                    return crossesEnd || crossesStart || isInBetween;
                });
    }

    public List<Pair<Double, Double>> getEligibleRunTimesAfter(final double after) {
        if (this.getPlannedExecutions().isEmpty()) {
            return List.of(Pair.of(after, Double.MAX_VALUE));
        }

        final List<Pair<Double, Double>> result = new ArrayList<>(this.getPlannedExecutions().size() + 2);
        double endTimeOfPreviousFrame = 0D;

        for (final PlannedExecution plannedExecution : this.getPlannedExecutions()) {

            if (endTimeOfPreviousFrame != 0D) {

                if (endTimeOfPreviousFrame >= after) {
                    result.add(Pair.of(endTimeOfPreviousFrame, plannedExecution.getStartTime()));
                } else if (endTimeOfPreviousFrame < after && plannedExecution.getStartTime() > after) {
                    result.add(Pair.of(after, plannedExecution.getStartTime()));
                }
            }

            endTimeOfPreviousFrame = plannedExecution.getEndTime();
        }

        result.add(Pair.of(endTimeOfPreviousFrame, Double.MAX_VALUE));

        return result;
    }

    public long getId() {
        return this.id;
    }

    public SortedSet<PlannedExecution> getPlannedExecutions() {
        return this.plannedExecutions;
    }
}
