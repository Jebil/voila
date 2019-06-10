package net.jk.app.commons.boot.exception;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Simple exception for manually created input validation errors (for those dynamic cases where
 * Hibernate Validators cannot be used)
 *
 * <p>Creating input validation errors manually via Hibernate Validators
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
@Value
@EqualsAndHashCode(callSuper = true)
public class InputValidationException extends InternationalizedException {

  private static final long serialVersionUID = 4656455974770796440L;
  private String propertyPath;

  public InputValidationException(
      String propertyPath, IVoilaError error, Object... messageParameters) {
    super(error, messageParameters);

    this.propertyPath = propertyPath;
  }
}
