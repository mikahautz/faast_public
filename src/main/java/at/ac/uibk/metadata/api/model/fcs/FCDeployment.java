package at.ac.uibk.metadata.api.model.fcs;

import at.ac.uibk.metadata.api.Column;
import at.ac.uibk.metadata.api.IdColumn;
import at.ac.uibk.metadata.api.model.Entity;
import at.ac.uibk.metadata.api.model.Table;

@Table(name = "fcdeployment")
public class FCDeployment implements Entity<Integer> {

    @IdColumn(name = "id", clazz = Integer.class)
    private Integer id;

    @Column(name = "description", clazz = String.class)
    private String description;

    @Column(name = "FCteam", clazz = Integer.class)
    private Integer fcTeamId;

    @Column(name = "fd", clazz = Integer.class)
    private Integer functionDeploymentId;

    @Override
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getFcTeamId() {
        return fcTeamId;
    }

    public void setFcTeamId(Integer fcTeamId) {
        this.fcTeamId = fcTeamId;
    }

    public Integer getFunctionDeploymentId() {
        return functionDeploymentId;
    }

    public void setFunctionDeploymentId(Integer functionDeploymentId) {
        this.functionDeploymentId = functionDeploymentId;
    }
}
