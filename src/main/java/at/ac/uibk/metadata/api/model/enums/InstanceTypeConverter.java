package at.ac.uibk.metadata.api.model.enums;

import com.amazonaws.services.ec2.model.InstanceType;

public class InstanceTypeConverter implements DbConverter {

    @Override
    public Object from(String dbValue) {
        return dbValue != null ? InstanceType.valueOf(dbValue) : null;
    }

    @Override
    public String to(Object object) {
        return object != null ? object.toString() : null;
    }
}
