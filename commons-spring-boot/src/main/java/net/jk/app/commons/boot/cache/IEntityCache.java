package net.jk.app.commons.boot.cache;

import java.util.Optional;
import net.jk.app.commons.boot.domain.IEntity;
import net.jk.app.commons.boot.security.domain.IApplicationUser;

/**
 * Common interface for all entity caches
 *
 * @param <E> entity type
 * @param <C> cache type (not always same as entity type, in case we optimize memory usage)
 */
public interface IEntityCache<E extends IEntity, C> {

  /** Access by public ID */
  Optional<C> getByPublicId(IApplicationUser user, String publicId);

  /**
   * Access by public ID, throw 404 if not found
   *
   * @throws net.on3.commons.boot.exception.EntityNotFoundException
   */
  C getExistingByPublicId(IApplicationUser user, String publicId);
}
