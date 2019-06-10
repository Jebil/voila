package net.jk.app.commons.boot.exception;

import com.google.common.collect.ImmutableMultimap;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Provides an option to wrap multiple exceptions in a single exception. Useful during bulk uploads
 * where there are multiple validation errors.
 */
@Value
@EqualsAndHashCode(callSuper = false)
@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class InvalidRequestMultiException extends RuntimeException {

  private static final long serialVersionUID = 1L;
  private ImmutableMultimap<IVoilaError, Object[]> exceptions;
}
