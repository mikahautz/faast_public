package at.ac.uibk.metadata.filestorage.services.se.provider;

import at.ac.uibk.metadata.api.daos.DetailedProviderDao;
import at.ac.uibk.metadata.api.daos.FunctionImplementationDao;
import at.ac.uibk.metadata.api.daos.FunctionTypeDao;
import at.ac.uibk.metadata.api.daos.RegionDao;
import at.ac.uibk.metadata.api.daos.se.provider.MetadataProvider;
import at.ac.uibk.metadata.api.daos.servicetypes.functions.FunctionDeploymentDao;
import at.ac.uibk.metadata.filestorage.services.*;

import java.nio.file.Path;
import java.nio.file.Paths;

public class FileStorageMetaDataProvider extends MetadataProvider {

    private DetailedProviderDao detailedProviderDao;

    private FunctionDeploymentDao functionDeploymentDao;

    private FunctionImplementationDao functionImplementationDao;

    private FunctionTypeDao functionTypeDao;

    private RegionDao regionDao;

    @Override
    public synchronized DetailedProviderDao detailedProviderDao() {
        if (this.detailedProviderDao == null) {
            try {
                this.detailedProviderDao = new JsonDetailedProviderDao(Path.of("metadata/provider.json"));
            } catch (final Exception e) {
                throw new IllegalStateException("an error occured while loading detailed providers");
            }
        }
        return this.detailedProviderDao;
    }

    @Override
    public RegionDao regionDao() {
        if (this.regionDao == null) {
            try {
                this.regionDao = new JsonRegionDao(Path.of("metadata/region.json"));
            } catch (final Exception e) {
                throw new IllegalStateException("an error occured while loading regions");
            }
        }
        return this.regionDao;
    }

    @Override
    public FunctionTypeDao functionTypeDao() {
        if (this.functionTypeDao == null) {
            try {
                this.functionTypeDao = new JsonFunctionTypeDao(Paths.get("metadata/functiontype.json"));
            } catch (final Exception e) {
                throw new IllegalStateException("an error occured while loading function types");
            }
        }
        return this.functionTypeDao;
    }

    @Override
    public FunctionImplementationDao functionImplementationDao() {
        if (this.functionImplementationDao == null) {
            try {
                this.functionImplementationDao = new JsonFunctionImplementationDao(Path.of("metadata/functionimplementation.json"));
            } catch (final Exception e) {
                throw new IllegalStateException("an error occured while loading function implementations");
            }
        }
        return this.functionImplementationDao;
    }

    @Override
    public FunctionDeploymentDao functionDeploymentDao() {
        if (this.functionDeploymentDao == null) {
            try {
                this.functionDeploymentDao = new JsonFunctionDeploymentDao(Path.of("metadata/functiondeployment.json"));
            } catch (final Exception e) {
                throw new IllegalStateException("an error occured while loading function deployments");
            }
        }
        return this.functionDeploymentDao;
    }

    @Override
    public void close() {
        //noop
    }
}
