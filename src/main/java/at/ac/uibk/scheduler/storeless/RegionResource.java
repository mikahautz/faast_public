package at.ac.uibk.scheduler.storeless;

import at.ac.uibk.metadata.api.model.Region;
import at.ac.uibk.scheduler.api.AbstractResource;

import java.util.Objects;

public class RegionResource extends AbstractResource {

    private final Region region;

    public RegionResource(Region region) {
        super();
        this.region = region;
    }

    public double getLatestEndTime() {
        if (getPlannedExecutions().isEmpty()) {
            return 0;
        }
        return getPlannedExecutions().last().getEndTime();
    }

    public Region getRegion() {
        return region;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RegionResource that = (RegionResource) o;
        return Objects.equals(region, that.region) && Objects.equals(getPlannedExecutions(), that.getPlannedExecutions());
    }

    @Override
    public int hashCode() {
        return Objects.hash(region, getPlannedExecutions());
    }
}
