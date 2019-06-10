package net.jk.app.commons.boot.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception representing a user who fails to authenticate for either bad credentials or a user that
 * doesn't exist
 */
@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class AuthenticationException extends InternationalizedException {

  private static final long serialVersionUID = 7169299080474808705L;

  public AuthenticationException() {
    super(VoilaError.INVALID_USER_OR_CREDS);
  }

  public AuthenticationException(IVoilaError error, Object... messageParameters) {
    super(error, messageParameters);
  }
}
