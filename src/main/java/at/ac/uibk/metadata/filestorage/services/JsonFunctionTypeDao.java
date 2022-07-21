package at.ac.uibk.metadata.filestorage.services;

import at.ac.uibk.metadata.api.daos.FunctionTypeDao;
import at.ac.uibk.metadata.api.model.FunctionType;

import java.io.IOException;
import java.nio.file.Path;

public class JsonFunctionTypeDao extends JsonDao<FunctionType, Long> implements FunctionTypeDao {

    public JsonFunctionTypeDao(final Path file) throws IOException {
        super(file);
    }

    @Override
    protected Class<FunctionType> getEntityClass() {
        return FunctionType.class;
    }
}
