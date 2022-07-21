package at.ac.uibk.metadata.api.model.fcs;

import at.ac.uibk.metadata.api.Column;
import at.ac.uibk.metadata.api.IdColumn;
import at.ac.uibk.metadata.api.model.Entity;
import at.ac.uibk.metadata.api.model.Table;

@Table(name = "fctype")
public class FCType implements Entity<Integer> {

    @IdColumn(name = "id", clazz = Integer.class)
    private Integer id;

    @Column(name = "name", clazz = String.class)
    private String name;

    @Column(name = "afcl", clazz = byte[].class)
    private byte[] afcl;

    @Override
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public byte[] getAfcl() {
        return afcl;
    }

    public void setAfcl(byte[] afcl) {
        this.afcl = afcl;
    }
}
