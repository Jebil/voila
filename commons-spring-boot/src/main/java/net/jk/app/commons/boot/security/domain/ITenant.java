package net.jk.app.commons.boot.security.domain;

import net.jk.app.commons.boot.domain.IEntity;

/**
 * Simple interface to represent a tenant Implementation needs to be concrete entity using whatever
 * DB technology a service is using
 */
public interface ITenant extends IEntity {

  public static final String SYSTEM_TENANT = "SYSTEM";
  public static final int SYSTEM_TENANT_ID = 0;

  /** Used in all data striping, has to be unique */
  int getTenantId();

  /** Public ID of a tenant, e.g. "J&J" */
  String getPublicId();
}
