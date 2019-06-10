package net.jk.app.commons.boot.exception;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.CONFLICT)
@Value
@EqualsAndHashCode(callSuper = true)
public class InvalidValueForAttributeException extends InternationalizedException {

  private String fieldName;
  private static final long serialVersionUID = 23137158740385571L;

  public InvalidValueForAttributeException(Object... messageParameters) {
    super(VoilaError.INVALID_VALUE_FOR_ATTRIBUTE, messageParameters);
    this.fieldName = null;
  }
}
