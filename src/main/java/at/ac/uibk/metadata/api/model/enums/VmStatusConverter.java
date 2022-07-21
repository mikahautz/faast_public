package at.ac.uibk.metadata.api.model.enums;

public class VmStatusConverter implements DbConverter {

    @Override
    public Object from(String dbValue) {
        return dbValue != null ? VmStatus.valueOf(dbValue) : null;
    }

    @Override
    public String to(Object object) {
        return object != null ? object.toString() : null;
    }
}
