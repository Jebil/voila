package net.jk.app.commons.boot.domain;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates a field is non-updateable and should not be changed once an entity is created
 *
 * <p>Used in dto2Entity()
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.FIELD)
public @interface NonUpdateable {}
