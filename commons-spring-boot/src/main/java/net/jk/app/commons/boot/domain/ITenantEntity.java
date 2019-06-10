package net.jk.app.commons.boot.domain;

/** Common interface for all tenant entities to implement, */
public interface ITenantEntity extends IEntity {

  /** Used for data striping */
  int getTenantId();

  void setTenantId(int tenantId);
}
