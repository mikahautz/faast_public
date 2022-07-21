package at.ac.uibk.metadata.filestorage.services;

import at.ac.uibk.metadata.api.daos.DetailedProviderDao;
import at.ac.uibk.metadata.api.model.DetailedProvider;

import java.io.IOException;
import java.nio.file.Path;

public class JsonDetailedProviderDao extends JsonDao<DetailedProvider, Integer> implements DetailedProviderDao {

    public JsonDetailedProviderDao(final Path file) throws IOException {
        super(file);
    }

    @Override
    protected Class<DetailedProvider> getEntityClass() {
        return DetailedProvider.class;
    }
}
