package at.ac.uibk.metadata.api.daos;

@FunctionalInterface
public interface ExceptionalRunner {

    void run() throws Exception;

}
