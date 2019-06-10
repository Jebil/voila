package net.jk.app.commons.boot.utils;

import com.google.common.collect.ImmutableMap;
import java.nio.file.AccessDeniedException;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import net.jk.app.commons.boot.exception.EntityNotFoundException;
import net.jk.app.commons.boot.exception.ForbiddenException;
import net.jk.app.commons.boot.exception.IVoilaError;
import net.jk.app.commons.boot.exception.InputValidationException;
import net.jk.app.commons.boot.exception.InvalidRequestException;
import net.jk.app.commons.boot.exception.VoilaError;
import org.apache.commons.lang3.StringUtils;

/** Utility class to throw the appropriate exception for various types of assertions */
public final class Assertions {

  private Assertions() {}

  /** Throws an {@link AccessDeniedException} if {@code expression} is false */
  public static void isAuthorized(boolean expression) {
    isAuthorized(expression, VoilaError.USER_NOT_AUTHORIZED);
  }

  /**
   * Throws an {@link AccessDeniedException} with the specified message if {@code expression} is
   * false
   */
  public static void isAuthorized(boolean expression, IVoilaError error) {
    if (!expression) {
      throw new ForbiddenException(error);
    }
  }

  /**
   * Throws a {@link ConstraintViolationException} if there are any constraint violations provided
   */
  public static <T> void isValid(Set<ConstraintViolation<T>> errors) {
    if (!errors.isEmpty()) {
      throw new ConstraintViolationException(errors);
    }
  }

  /** Throws an {@link InvalidRequestException} if {@code expression} is false */
  public static void isValidRequest(boolean expression, IVoilaError error, Object... errorArgs) {
    if (!expression) {
      throw new InvalidRequestException(error, errorArgs);
    }
  }

  /** Throws an {@link EntityNotFoundException} if {@code expression} is false */
  public static <T> void isFound(
      boolean expression, Class<T> entityType, String idField, Object identifier) {
    if (!expression) {
      throw new EntityNotFoundException(entityType, ImmutableMap.of(idField, identifier));
    }
  }

  /**
   * Verifies a string value is not empty Useful when we need dynamic field validation that is
   * conditional, so basic Hibernate validator entities cannot be used
   */
  public static void isNotEmpty(String fieldName, String value) {
    if (StringUtils.isEmpty(value)) {
      throw new InputValidationException(fieldName, VoilaError.FIELD_MUST_NOT_BE_EMPTY);
    }
  }
}
