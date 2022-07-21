package at.ac.uibk.metadata.api.model.fcs;

import at.ac.uibk.metadata.api.Column;
import at.ac.uibk.metadata.api.IdColumn;
import at.ac.uibk.metadata.api.model.Entity;
import at.ac.uibk.metadata.api.model.Table;

@Table(name = "fcteam")
public class FCTeam implements Entity<Integer> {

    @IdColumn(name = "id", clazz = Integer.class)
    private Integer id;

    @Column(name = "name", clazz = String.class)
    private String name;

    @Column(name = "fctype", clazz = Integer.class)
    private Integer fcTypeId;

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

    public Integer getFcTypeId() {
        return fcTypeId;
    }

    public void setFcTypeId(Integer fcTypeId) {
        this.fcTypeId = fcTypeId;
    }
}
