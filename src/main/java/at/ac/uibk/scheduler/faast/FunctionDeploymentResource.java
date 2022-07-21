package at.ac.uibk.scheduler.faast;

import at.ac.uibk.metadata.api.model.FunctionType;
import at.ac.uibk.metadata.api.model.functions.FunctionDeployment;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class FunctionDeploymentResource {

    private static final AtomicLong objectCounter = new AtomicLong(0);

    private final long id;

    private final FunctionDeployment deployment;

    private final FunctionType type;

    private final SortedSet<PlannedExecution> plannedExecutions = new TreeSet<>();

    public FunctionDeploymentResource(final FunctionDeployment deployment, final FunctionType type) {
        this.id = FunctionDeploymentResource.objectCounter.getAndIncrement();
        this.deployment = deployment;
        this.type = type;
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

    public FunctionDeployment getDeployment() {
        return this.deployment;
    }

    public FunctionType getType() {
        return this.type;
    }

    public SortedSet<PlannedExecution> getPlannedExecutions() {
        return this.plannedExecutions;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FunctionDeploymentResource)) {
            return false;
        }
        final FunctionDeploymentResource that = (FunctionDeploymentResource) o;
        return this.getId() == that.getId();
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getId());
    }
}
