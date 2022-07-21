package at.ac.uibk.metadata.api.daos;

import java.util.List;

public interface Dao<I, ID> {

    I getById(ID id) throws Exception;

    List<I> getAll() throws Exception;

}
