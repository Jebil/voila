package net.jk.app.commons.boot.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception when a user makes a request that is not valid following the semantics of the {@link
 * HttpStatus#BAD_REQUEST} status code
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidRequestException extends InternationalizedException {

  private static final long serialVersionUID = 5512130606977284810L;

  public InvalidRequestException(IVoilaError error, Object... messageParameters) {
    super(error, messageParameters);
  }
}
