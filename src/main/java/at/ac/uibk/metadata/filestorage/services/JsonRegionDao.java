package at.ac.uibk.metadata.filestorage.services;

import at.ac.uibk.metadata.api.daos.RegionDao;
import at.ac.uibk.metadata.api.model.Region;

import java.io.IOException;
import java.nio.file.Path;

public class JsonRegionDao extends JsonDao<Region, Integer> implements RegionDao {

    public JsonRegionDao(final Path file) throws IOException {
        super(file);
    }

    @Override
    protected Class<Region> getEntityClass() {
        return Region.class;
    }
}
