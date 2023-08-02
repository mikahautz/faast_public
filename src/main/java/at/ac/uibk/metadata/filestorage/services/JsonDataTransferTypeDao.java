package at.ac.uibk.metadata.filestorage.services;

import at.ac.uibk.metadata.api.daos.DataTransferDao;
import at.ac.uibk.metadata.api.model.DataTransfer;

import java.io.IOException;
import java.nio.file.Path;

public class JsonDataTransferTypeDao extends JsonDao<DataTransfer, Long> implements DataTransferDao {

    public JsonDataTransferTypeDao(final Path file) throws IOException {
        super(file);
    }

    @Override
    protected Class<DataTransfer> getEntityClass() {
        return DataTransfer.class;
    }
}
