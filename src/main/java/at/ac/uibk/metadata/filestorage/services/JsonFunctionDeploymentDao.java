package at.ac.uibk.metadata.filestorage.services;

import at.ac.uibk.metadata.api.daos.servicetypes.functions.FunctionDeploymentDao;
import at.ac.uibk.metadata.api.model.functions.FunctionDeployment;

import java.io.IOException;
import java.nio.file.Path;

public class JsonFunctionDeploymentDao extends JsonDao<FunctionDeployment, Long>
        implements FunctionDeploymentDao {

    public JsonFunctionDeploymentDao(final Path file) throws IOException {
        super(file);
    }

    @Override
    protected Class<FunctionDeployment> getEntityClass() {
        return FunctionDeployment.class;
    }

}


