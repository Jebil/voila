package net.jk.app.commons.boot.domain;

/**
 * Common interface for all entities to implement, regardless whether master data or tenant-specific
 * data
 *
 * <p>Comparable implementation is required so that all entities are sorted in a predictable order
 * in order to allow integration tests to pass reliably in all environments
 */
public interface IEntity extends Comparable<IEntity> {

  /** Public ID for REST access */
  String getPublicId();

  /**
   * Called before initial save to generate public ID Should be immutable once saved Not necessary
   * to override if the public ID is a well defined single field
   */
  default void generatePublicId() {}

  /** Any additional checks prior to save */
  default void preUpdate() {}

  /**
   * Sort consistent by public ID for predictable sort in JSON documents (required for integration
   * tests to pass reliably in all environments)
   */
  default int compareTo(IEntity o) {
    if (this.getPublicId() == null) {
      return -1;
    } else {
      return this.getPublicId().compareTo(o.getPublicId());
    }
  }
}
