package at.ac.uibk.scheduler.faast;

import at.ac.uibk.metadata.api.model.FunctionType;
import at.ac.uibk.metadata.api.model.functions.FunctionDeployment;
import at.ac.uibk.scheduler.api.AbstractResource;

import java.util.Objects;

public class FunctionDeploymentResource extends AbstractResource {

    private final FunctionDeployment deployment;

    private final FunctionType type;

    public FunctionDeploymentResource(final FunctionDeployment deployment, final FunctionType type) {
        super();
        this.deployment = deployment;
        this.type = type;
    }

    public FunctionDeployment getDeployment() {
        return this.deployment;
    }

    public FunctionType getType() {
        return this.type;
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
