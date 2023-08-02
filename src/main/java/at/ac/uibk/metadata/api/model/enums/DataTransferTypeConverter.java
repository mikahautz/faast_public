package at.ac.uibk.metadata.api.model.enums;

public class DataTransferTypeConverter implements DbConverter {
    @Override
    public Object from(String dbValue) {
        return dbValue != null ? DataTransferType.valueOf(dbValue) : null;
    }

    @Override
    public String to(Object object) {
        return object != null ? object.toString() : null;
    }
}
