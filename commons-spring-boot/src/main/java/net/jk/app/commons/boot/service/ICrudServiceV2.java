package net.jk.app.commons.boot.service;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.jk.app.commons.boot.security.domain.IApplicationUser;

/**
 * Common interface for any service that is responsible for basic CRUD operations on a resource
 *
 * <p>IApplicationUser is passed in explicitly in order to provide tenant / security context instead
 * of fetching it from the ThreadLocal SecurityContext and risking picking up wrong tenant / user
 *
 * @param <E> DB entity type
 * @param <C> Create object type
 * @param <U> Update object type
 * @param <ID> The identifier type of the objects
 */
public interface ICrudServiceV2<E, C, U, ID extends Serializable> {

  /**
   * Should be write transaction. Should throw EntityExistException if entity is already present
   *
   * @throws EntityExistsException if an object with the same unique data exists
   */
  E add(IApplicationUser user, C createDto);

  /**
   * Should be a read-only transaction and idempotent.
   *
   * <p>Retrieves all objects the requesting user is authorized to view.
   */
  List<E> getAll(IApplicationUser user);

  /**
   * Should be a read-only transaction and idempotent.
   *
   * <p>Retrieves the object corresponding to the particular id if it exists and the user is
   * authorized to view the object
   */
  Optional<E> getById(IApplicationUser user, ID id);

  /**
   * Should be a read-only transaction and idempotent.
   *
   * <p>Retrieves the object corresponding to the particular id and the user is authorized to view
   * the object
   *
   * @throws EntityNotFoundException if the object doesn't exist
   */
  E getExistingById(IApplicationUser user, ID id);

  /**
   * Should be write transaction.
   *
   * <p>Updates an object with the specified id with values from the specified update object
   *
   * @throws EntityNotFoundException if the object with the id doesn't exist
   * @throws EntityExistsException if the unique update data collides with another existing object
   */
  E update(IApplicationUser user, ID id, U updateDto);

  /**
   * Adds multiple new entities in one batch
   *
   * @throws EntityExistsException if any of the entities in the list exist in DB
   */
  Set<E> addAll(IApplicationUser user, Collection<C> createDtos);

  /** Deletes entity */
  void delete(IApplicationUser user, E entity);

  /** Deletes entity */
  default void delete(IApplicationUser user, ID id) {
    delete(user, getExistingById(user, id));
  }
}
