package at.ac.uibk.metadata.filestorage.services;

import at.ac.uibk.metadata.api.daos.FunctionImplementationDao;
import at.ac.uibk.metadata.api.model.FunctionImplementation;

import java.io.IOException;
import java.nio.file.Path;

public class JsonFunctionImplementationDao extends JsonDao<FunctionImplementation, Long>
        implements FunctionImplementationDao {

    public JsonFunctionImplementationDao(final Path file) throws IOException {
        super(file);
    }

    @Override
    protected Class<FunctionImplementation> getEntityClass() {
        return FunctionImplementation.class;
    }

}
