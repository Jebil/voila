package net.jk.app.commons.boot.exception;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Standard exception if a non-updatedable field is attempted to be changed */
@ResponseStatus(value = HttpStatus.CONFLICT)
@Value
@EqualsAndHashCode(callSuper = true)
public class NonUpdateableFieldViolationException extends InternationalizedException {

  private static final long serialVersionUID = 6253387596047169170L;
  private String fieldName;

  public NonUpdateableFieldViolationException(String fieldName) {
    super(VoilaError.NON_UPDATEABLE_FIELD_MOD, fieldName);

    this.fieldName = fieldName;
  }
}
