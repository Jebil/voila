package net.jk.app.commons.boot.service;

import static org.springframework.util.StringUtils.concatenateStringArrays;

import com.google.common.collect.ImmutableSet;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.validation.Validator;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.jk.app.commons.boot.domain.IAuditTrail;
import net.jk.app.commons.boot.domain.ITenantEntity;
import net.jk.app.commons.boot.domain.NonUpdateable;
import net.jk.app.commons.boot.exception.EntityExistsException;
import net.jk.app.commons.boot.exception.EntityNotFoundException;
import net.jk.app.commons.boot.exception.NonUpdateableFieldViolationException;
import net.jk.app.commons.boot.exception.ServerRuntimeException;
import net.jk.app.commons.boot.repository.IEntityRepository;
import net.jk.app.commons.boot.security.domain.IApplicationUser;
import net.jk.app.commons.boot.utils.Assertions;
import org.reflections.ReflectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * An abstract implementation of the {@link ICrudService} for services that represent database
 * entities.
 */
@Slf4j
public abstract class AbstractEntityCrudServiceV2<E, C, U, ID extends Serializable>
    implements ICrudServiceV2<E, C, U, ID> {

  @Autowired protected AuditTrailService auditTrailService;
  @Autowired protected Validator entityValidator;

  @Getter(AccessLevel.PROTECTED)
  private final Class<E> entityType;

  /** Required for data access operations */
  protected abstract IEntityRepository<E, ?> getRepository();

  protected final ImmutableSet<Field> nonUpdateableFields;
  protected final String[] nonUpdateableFieldNames;

  public AbstractEntityCrudServiceV2() {
    // get generic types for entity & relevant DTOs (workaround generics erasure)
    entityType =
        (Class<E>)
            ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];

    // keep track of non-updateable fields
    nonUpdateableFields =
        ImmutableSet.copyOf(
            ReflectionUtils.getAllFields(
                getEntityType(), f -> f.getAnnotation(NonUpdateable.class) != null));
    nonUpdateableFieldNames =
        nonUpdateableFields
            .stream()
            .map(Field::getName)
            .collect(Collectors.toSet())
            .toArray(new String[nonUpdateableFields.size()]);

    // allow access to private fields
    nonUpdateableFields.forEach(f -> f.setAccessible(true));
  }

  @Override
  public E add(IApplicationUser user, C createDto) {

    checkForExistingConflictingEntity(user, createDto);

    E entity = prepareEntityForAdd(user, createDto);
    return doAdd(user, entity);
  }

  /** Prepares the entity so that it can be added */
  public E prepareEntityForAdd(IApplicationUser user, C createDto) {
    E entity = createDtoToEntity(user, createDto);
    // update system specific data
    populateTenantData(user, entity);
    populateAuditData(user, entity);
    // allow customizations
    preAdd(user, entity);
    preAddOrUpdate(user, entity);

    // now that entity should be fully populated, ensure the user is authorized
    Assertions.isAuthorized(isAuthorized(user, entity, OperationType.ADD));

    return entity;
  }

  @Override
  public Set<E> addAll(IApplicationUser user, Collection<C> createDtos) {
    return createDtos.stream().map(c -> add(user, c)).collect(Collectors.toSet());
  }

  @Override
  public List<E> getAll(IApplicationUser user) {
    return StreamSupport.stream(getRepository().findAll().spliterator(), false)
        .filter(e -> isAuthorized(user, e, OperationType.READ))
        .sorted()
        .collect(Collectors.toList());
  }

  @Override
  public Optional<E> getById(IApplicationUser user, ID id) {
    return getEntityById(user, id).filter(e -> isAuthorized(user, e, OperationType.READ));
  }

  @Override
  public E getExistingById(IApplicationUser user, ID id) {
    return getById(user, id)
        .orElseThrow(() -> new EntityNotFoundException(getEntityType(), id.toString()));
  }

  @Override
  public E update(IApplicationUser user, ID id, U updateDto) {
    E entity = prepareEntityForUpdate(user, id, updateDto);

    return update(user, entity);
  }

  @Override
  public void delete(IApplicationUser user, E entity) {
    Assertions.isAuthorized(isAuthorized(user, entity, OperationType.DELETE));
    getRepository().delete(entity);
  }

  protected E doAdd(IApplicationUser user, E entity) {
    // user is authorized, validate the entity before persistence
    Assertions.isValid(entityValidator.validate(entity));

    entity = getRepository().save(entity);

    postAdd(user, entity);
    postAddOrUpdate(user, entity);
    return entity;
  }

  // prepares entity for update operation without actually updating it (yet)
  protected E prepareEntityForUpdate(IApplicationUser user, ID id, U updateDto) {
    E entity = getExistingEntityById(user, id);

    updateDtoToEntity(user, updateDto, entity);
    return entity;
  }

  protected void checkForExistingConflictingEntity(IApplicationUser user, C createDto) {
    Optional<E> existingEntity = getExistingConflictingEntity(user, createDto);
    if (existingEntity.isPresent()) {
      throw new EntityExistsException(getEntityType(), getDtoFinderValues(createDto));
    }
  }

  /**
   * Allow to update DB entity directly, in cases where other services need to manipulate it
   * directly
   */
  public E update(IApplicationUser user, E entity) {
    Assertions.isAuthorized(isAuthorized(user, entity, OperationType.UPDATE));

    // system updates
    populateAuditData(user, entity);

    preUpdate(user, entity);
    preAddOrUpdate(user, entity);

    Assertions.isValid(entityValidator.validate(entity));

    entity = getRepository().save(entity);

    postUpdate(user, entity);
    postAddOrUpdate(user, entity);
    return entity;
  }

  /** Map the create dto to the entity object */
  protected E createDtoToEntity(IApplicationUser user, C createDto) {

    E entity = constructInstance(getEntityType());
    BeanUtils.copyProperties(createDto, entity, ignoreOnCopyProperties());

    return entity;
  }

  /**
   * The fields returned by this method will be ignored while converting dto-> entity during
   * add/update.
   *
   * <p><i>It is observed that {@link BeanUtils#copyProperties(Object, Object)} copy collections
   * from source to target even if the underlying types are different. This may not be desirable in
   * some of the JPA use-cases</i>
   *
   * @return
   */
  protected String[] ignoreOnCopyProperties() {
    return null;
  }

  /** Map the update dto to the entity object */
  protected E updateDtoToEntity(IApplicationUser user, U updateDto, E entity) {
    // exclude non-updateable fields by default
    nonUpdateableFields.forEach(f -> verifyNonUpdateableField(updateDto, entity, f));

    BeanUtils.copyProperties(
        updateDto,
        entity,
        concatenateStringArrays(ignoreOnCopyProperties(), nonUpdateableFieldNames));
    return entity;
  }

  protected void populateTenantData(IApplicationUser user, E entity) {
    if (ITenantEntity.class.isAssignableFrom(entity.getClass())) {
      ITenantEntity tenantEntity = (ITenantEntity) entity;
      tenantEntity.setTenantId(user.getTenantId());
    }
  }

  protected void populateAuditData(IApplicationUser user, Object entity) {
    if (IAuditTrail.class.isAssignableFrom(entity.getClass())) {
      auditTrailService.process(user, (IAuditTrail) entity);
    }
  }

  protected abstract Optional<E> getEntityById(IApplicationUser user, ID id);

  protected E getExistingEntityById(IApplicationUser user, ID id) {
    return getEntityById(user, id)
        .orElseThrow(() -> new EntityNotFoundException(getEntityType(), id.toString()));
  }

  /** Basic entity-level permissions, depending on request user */
  protected abstract boolean isAuthorized(
      IApplicationUser user, E entity, OperationType operationType);

  /** Try to find an existing entity using the values in the create dto */
  protected abstract Optional<E> getExistingConflictingEntity(IApplicationUser user, C createDto);

  /** Given a create dto, retrieve the field -> value pairs that represent a unique entity */
  protected abstract Map<String, Object> getDtoFinderValues(C createDto);

  /** pre-save hook */
  protected void preAdd(IApplicationUser user, E entity) {}

  /** pre-update hook */
  protected void preUpdate(IApplicationUser user, E entity) {}

  /** pre-update hook */
  protected void preAddOrUpdate(IApplicationUser user, E entity) {}

  /** Post add method to customize any logic */
  protected void postAdd(IApplicationUser user, E entity) {}

  /** Post update method to customize any logic */
  protected void postUpdate(IApplicationUser user, E entity) {}

  /** Post add or update method to customize any logic */
  protected void postAddOrUpdate(IApplicationUser user, E entity) {}

  @SuppressFBWarnings("EXS")
  private <T> T constructInstance(Class<T> tClass) {
    try {
      return tClass.newInstance();
    } catch (InstantiationException | IllegalAccessException e) {
      // should never happens, means default constructor is missing
      throw new ServerRuntimeException(
          "Unable to instantiate object of type " + tClass.getSimpleName(), e);
    }
  }

  // basic check to throw 409 Conflict error on common data types
  private void verifyNonUpdateableField(U input, E entity, Field field) {
    try {
      Field inputSource = input.getClass().getDeclaredField(field.getName());
      inputSource.setAccessible(true);

      // only compare if types are identical
      if (field.getType().equals(inputSource.getType())) {
        Object inputValue = inputSource.get(input);
        Object existingValue = field.get(entity);

        if (!Objects.equals(inputValue, existingValue)) {
          throw new NonUpdateableFieldViolationException(field.getName());
        }
      }

    } catch (NoSuchFieldException | IllegalAccessException e) {
      // safely ignore then
      log.debug(
          "Failed to compare field {} on class {}",
          field.getName(),
          entity.getClass().getSimpleName());
    }
  }
}
