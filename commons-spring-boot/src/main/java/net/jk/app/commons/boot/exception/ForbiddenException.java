package net.jk.app.commons.boot.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception to be thrown when an authenticated user attempts an action they aren't authorized to
 * perform
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class ForbiddenException extends InternationalizedException {

  private static final long serialVersionUID = -8585224019217982529L;

  public ForbiddenException() {
    super(VoilaError.USER_NOT_AUTHORIZED);
  }

  public ForbiddenException(IVoilaError error, Object... messageParameters) {
    super(error, messageParameters);
  }
}
