package net.jk.app.commons.boot.security.permissions;

/**
 * Generic marker interface that any data permission-related object needs to implement Concrete
 * implementation will be domain-specific, depending on the app
 */
public interface IDataPermission {

  /**
   * Defines if user has access to ALL data within the data permission context (e.g. super user) or
   * not (the more typical scenario)
   *
   * <p>This will translate in extra parameters for DB queries (or less)
   */
  default boolean isFullAcess() {
    return false;
  }
}
