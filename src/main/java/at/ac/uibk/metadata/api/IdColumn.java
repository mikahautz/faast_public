package at.ac.uibk.metadata.api;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface IdColumn {

    String name();

    Class clazz();

}
