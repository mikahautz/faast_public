package at.ac.uibk.metadata.api.model;

import at.ac.uibk.metadata.api.Column;

import java.io.Serializable;

public class AdditionalServiceType implements Entity<Long>, Serializable {

    @Column(name = "id", clazz = Long.class)
    private Long id;

    @Column(name = "name", clazz = String.class)
    private String name;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
