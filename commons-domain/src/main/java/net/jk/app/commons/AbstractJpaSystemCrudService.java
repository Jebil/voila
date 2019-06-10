package net.jk.app.commons;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.persistence.EntityExistsException;
import net.jk.app.commons.boot.CommonConstants;
import net.jk.app.commons.boot.security.domain.IApplicationUser;
import net.jk.app.commons.boot.service.AbstractEntityCrudServiceV2;
import org.hibernate.dialect.lock.OptimisticEntityLockException;
import org.springframework.transaction.annotation.Transactional;

/**
 * Abstract JPA entity crud service to serve as base with common functionality for the main SYSTEM
 * database
 *
 * <p>Adds System JPA specific transactional boundaries to all standard methods from {@link
 * AbstractEntityCrudServiceV2}
 */
public abstract class AbstractJpaSystemCrudService<E, C, U, ID extends Serializable>
    extends AbstractEntityCrudServiceV2<E, C, U, ID> {

  @Transactional(value = CommonConstants.SYSTEM_TX_MANAGER, readOnly = false)
  @Override
  public E add(IApplicationUser user, C createDto) {
    return super.add(user, createDto);
  }

  @Transactional(value = CommonConstants.SYSTEM_TX_MANAGER, readOnly = false)
  @Override
  public Set<E> addAll(IApplicationUser user, Collection<C> createDtos) {
    return super.addAll(user, createDtos);
  }

  @Transactional(value = CommonConstants.SYSTEM_TX_MANAGER, readOnly = true)
  @Override
  public List<E> getAll(IApplicationUser user) {
    return super.getAll(user);
  }

  @Transactional(value = CommonConstants.SYSTEM_TX_MANAGER, readOnly = true)
  @Override
  public Optional<E> getById(IApplicationUser user, ID id) {
    return super.getById(user, id);
  }

  @Transactional(value = CommonConstants.SYSTEM_TX_MANAGER, readOnly = true)
  @Override
  public E getExistingById(IApplicationUser user, ID id) {
    return super.getExistingById(user, id);
  }

  @Transactional(value = CommonConstants.SYSTEM_TX_MANAGER, readOnly = false)
  @Override
  public E update(IApplicationUser user, ID id, U updateDto) {
    return super.update(user, id, updateDto);
  }

  @Transactional(value = CommonConstants.SYSTEM_TX_MANAGER, readOnly = false)
  @Override
  public void delete(IApplicationUser user, E entity) {
    super.delete(user, entity);
  }

  protected void addIfMissing(IApplicationUser user, C createDto) {
    try {
      add(user, createDto);
    } catch (EntityExistsException | OptimisticEntityLockException e) {
      // ignore
    }
  }
}
