package at.ac.uibk.metadata.api.model;

import at.ac.uibk.metadata.api.Column;
import at.ac.uibk.metadata.api.IdColumn;

@Table(name = "language")
public class Language {

    @IdColumn(name = "id", clazz = Long.class)
    private Long id;

    @Column(name = "name", clazz = String.class)
    private String name;

    @Column(name = "idAWS", clazz = String.class)
    private String idAWS;

    @Column(name = "idAzure", clazz = String.class)
    private String idAzure;

    @Column(name = "idGoogle", clazz = String.class)
    private String idGoogle;

    @Column(name = "idAlibaba", clazz = String.class)
    private String idAlibaba;

    public Long getId() {
        return this.id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getIdAWS() {
        return this.idAWS;
    }

    public void setIdAWS(String idAWS) {
        this.idAWS = idAWS;
    }

    public String getIdAzure() {
        return idAzure;
    }

    public void setIdAzure(String idAzure) {
        this.idAzure = idAzure;
    }

    public String getIdGoogle() {
        return idGoogle;
    }

    public void setIdGoogle(String idGoogle) {
        this.idGoogle = idGoogle;
    }

    public String getIdAlibaba() {
        return idAlibaba;
    }

    public void setIdAlibaba(String idAlibaba) {
        this.idAlibaba = idAlibaba;
    }
}
