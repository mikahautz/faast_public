package at.ac.uibk.metadata.api.model.enums;

public interface DbConverter {

    Object from(String dbValue);

    String to(Object object);

}
