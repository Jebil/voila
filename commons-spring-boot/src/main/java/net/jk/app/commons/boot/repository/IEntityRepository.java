package net.jk.app.commons.boot.repository;

/** Minimalistic interface for data repostories (DAOs) */
public interface IEntityRepository<E, ID> {

  Iterable<E> findAll();

  <S extends E> S save(S entity);

  /*
   * Batch save support
   */
  <S extends E> Iterable<S> saveAll(Iterable<S> entities);

  /** Deletes specified entity */
  void delete(E entity);
}
