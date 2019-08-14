package net.jk.app.commons.boot.service;

import java.util.Optional;
import net.jk.app.commons.boot.domain.IEntity;
import net.jk.app.commons.boot.security.domain.IApplicationUser;

/** Interface defining methods for services that support {@link IOn3Entity} */
public interface IEntityService<E extends IEntity> {

  /**
   * Should be a read-only transaction and idempotent
   *
   * @return the entity corresponding to the publicId; otheriwse {@link Optional#empty()}
   */
  Optional<E> getByPublicId(IApplicationUser user, String publicId);

  /**
   * Should be a read-only transaction and idempotent
   *
   * @return the entity corresponding to the publicId
   * @throws EntityNotFoundException if an entity with the specified publicId does not exist
   */
  E getExistingByPublicId(IApplicationUser user, String publicId);
}
