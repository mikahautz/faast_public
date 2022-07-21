package at.ac.uibk.metadata.filestorage.services;


import at.ac.uibk.metadata.api.daos.Dao;
import at.ac.uibk.metadata.api.model.Entity;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class JsonDao<E extends Entity<ID>, ID> implements Dao<E, ID> {

    private final Map<ID, E> entities;

    protected JsonDao(final Path file) throws IOException {
        this.entities = new LinkedHashMap<>();
        try (final InputStream is = Files.newInputStream(file, StandardOpenOption.READ)) {
            final MappingIterator<E> iter = new ObjectMapper().readerFor(this.getEntityClass()).readValues(is);

            while (iter.hasNext()) {
                final E next = iter.next();
                this.entities.put(next.getId(), next);
            }
        }

    }

    @Override
    public E getById(final ID id) {
        return this.getEntities().get(id);
    }

    @Override
    public List<E> getAll() {
        return new ArrayList<>(this.getEntities().values());
    }

    protected Map<ID, E> getEntities() {
        return this.entities;
    }

    protected abstract Class<E> getEntityClass();
}
