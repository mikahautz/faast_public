package at.ac.uibk.metadata.api.model.storage;

import at.ac.uibk.metadata.api.Column;
import at.ac.uibk.metadata.api.model.AdditionalServiceType;
import at.ac.uibk.metadata.api.model.Table;
import at.ac.uibk.metadata.api.model.enums.Provider;
import at.ac.uibk.metadata.api.model.enums.ProviderConverter;

@Table(name = "storagedeployment")
public class StorageDeployment extends AdditionalServiceType {

    @Column(name = "provider", clazz = Provider.class, converter = ProviderConverter.class)
    private Provider provider;

    @Column(name = "KMS_Arn", clazz = String.class)
    private String kmsArn;

    @Column(name = "location", clazz = String.class)
    private String location;

    public Provider getProvider() {
        return provider;
    }

    public void setProvider(Provider provider) {
        this.provider = provider;
    }

    public String getKmsArn() {
        return kmsArn;
    }

    public void setKmsArn(String kmsArn) {
        this.kmsArn = kmsArn;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }
}
