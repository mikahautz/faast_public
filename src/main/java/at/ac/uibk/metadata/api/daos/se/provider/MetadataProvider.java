package at.ac.uibk.metadata.api.daos.se.provider;

import at.ac.uibk.metadata.api.daos.*;
import at.ac.uibk.metadata.api.daos.servicetypes.functions.FunctionDeploymentDao;

import java.util.ServiceLoader;

public abstract class MetadataProvider implements AutoCloseable {

    private static class InstanceHolder {

        private static final ServiceLoader<MetadataProvider> INSTANCE = ServiceLoader.load(MetadataProvider.class);

        private InstanceHolder() {
        }
    }

    public static MetadataProvider get() {
        return InstanceHolder.INSTANCE.findFirst().orElseThrow(() -> new IllegalStateException("No implementation of MetadataProvider found"));
    }

    public abstract FunctionDeploymentDao functionDeploymentDao();

    public abstract RegionDao regionDao();

    public abstract FunctionTypeDao functionTypeDao();

    public abstract FunctionImplementationDao functionImplementationDao();

    public abstract DetailedProviderDao detailedProviderDao();

    public abstract DataTransferDao dataTransferDao();
}
