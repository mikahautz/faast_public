package at.ac.uibk.metadata.api.model.enums;

public class ProviderConverter implements DbConverter {

    @Override
    public Object from(String dbValue) {
        return dbValue != null ? Provider.valueOf(dbValue) : null;
    }

    @Override
    public String to(Object object) {
        return object != null ? object.toString() : null;
    }
}
