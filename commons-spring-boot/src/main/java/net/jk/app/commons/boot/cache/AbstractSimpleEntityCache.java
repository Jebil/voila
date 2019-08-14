package net.jk.app.commons.boot.cache;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.collect.ImmutableList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import lombok.NonNull;
import net.jk.app.commons.boot.domain.IEntity;
import net.jk.app.commons.boot.exception.EntityNotFoundException;
import net.jk.app.commons.boot.security.IApplicationUserService;
import net.jk.app.commons.boot.security.domain.IApplicationUser;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Very simple implementation of an entity cache with a scheduled refresh
 *
 * <p>It should only be used for *SMALL* master data entities ( a few hundred, thousands entities at
 * most)
 */
public abstract class AbstractSimpleEntityCache<E extends IEntity, C>
    implements IEntityCache<E, C> {

  private static final int INITIAL_CAPACITY = 1000;

  private AtomicReference<ConcurrentMap<String, C>> cache =
      new AtomicReference<>(new ConcurrentHashMap<>(INITIAL_CAPACITY));

  @Autowired private IApplicationUserService userService;

  @Override
  public Optional<C> getByPublicId(IApplicationUser user, String publicId) {
    if (publicId == null) {
      return Optional.empty();
    } else {
      return Optional.ofNullable(cache.get().get(publicId));
    }
  }

  @Override
  public C getExistingByPublicId(IApplicationUser user, @NonNull String publicId) {
    C entity = cache.get().computeIfAbsent(publicId, (key) -> null);
    if (entity == null) {
      // not found, do a secondary lookup in DB in case it was added in the meantime
      E dbEntity = queryByPublicId(user, publicId);
      if (dbEntity != null) {
        entity = convert(user, dbEntity);
        cache.get().put(dbEntity.getPublicId(), entity);
      } else {
        // should not happen, queryByPublicId should throw 404 error, but just in case....
        throw new EntityNotFoundException(getEntityType(), publicId);
      }
    }

    return entity;
  }

  // gets all values, for internal usage
  protected List<C> getAll(IApplicationUser user) {
    return ImmutableList.copyOf(cache.get().values());
  }

  protected List<C> getAll(
      IApplicationUser user, Predicate<C> filter, Optional<Comparator<C>> comparator) {
    return comparator
        .map(c -> cache.get().values().stream().filter(filter).sorted(c).collect(toImmutableList()))
        .orElse(cache.get().values().stream().filter(filter).collect(toImmutableList()));
  }

  protected ConcurrentMap<String, C> getCache() {
    return cache.get();
  }

  /**
   * Alias for 'getExistingByPublicId' that just verifies an entity exists, used for input
   * validation (but with a more logical method name for that task)
   *
   * @param publicId Check skipped if null
   * @throws EntityNotFoundException if not found
   */
  public void checkExists(IApplicationUser user, @Nullable String publicId) {
    if (StringUtils.isNotEmpty(publicId)) {
      getExistingByPublicId(user, publicId);
    }
  }

  /** Converts entity to type being stored in cache */
  protected C convert(IApplicationUser user, E entity) {
    // default implementation assumes they are the same
    return (C) entity;
  }

  /** Return entity type explicitly (due to generics erasure) */
  protected abstract Class<E> getEntityType();

  /**
   * Override to get a single one, in case we are doing a secondary lookup from DB
   *
   * @throws net.on3.commons.boot.exception.EntityNotFoundException
   */
  protected abstract E queryByPublicId(IApplicationUser user, String publicId);

  /** Queries all from DB */
  protected abstract List<E> queryAll(IApplicationUser user);

  /** Refreshes once a minute (very simple) */
  @Scheduled(fixedDelay = 60 * 1000, initialDelay = 1000)
  public synchronized void refreshAll() {
    IApplicationUser user = userService.getSystemTenantBackgroundUser();
    List<E> all = queryAll(user);

    ConcurrentMap<String, C> map = new ConcurrentHashMap<>(INITIAL_CAPACITY);
    all.stream().forEach(e -> map.put(e.getPublicId(), convert(user, e)));

    // do atomic replace with new values
    cache.set(map);

    // extension for downstream use
    setExtraCaches(map);
  }

  /** Helper method in case a single cache needs to support access by different fields */
  protected void setExtraCaches(ConcurrentMap<String, C> map) {}

  @PostConstruct
  public void init() {
    // load up at startup
    refreshAll();
  }
}
