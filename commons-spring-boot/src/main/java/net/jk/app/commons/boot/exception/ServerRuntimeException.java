package net.jk.app.commons.boot.exception;

import java.text.MessageFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Catch all exception for any 500 server errors */
@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
public class ServerRuntimeException extends RuntimeException {

  private static final long serialVersionUID = -561937474542123559L;

  public ServerRuntimeException(String messageFormat, Object... args) {
    super(MessageFormat.format(messageFormat, args));
  }

  public ServerRuntimeException(String messageFormat, Throwable cause, Object... args) {
    super(MessageFormat.format(messageFormat, args), cause);
  }

  public ServerRuntimeException(String message) {
    super(message);
  }

  public ServerRuntimeException(String message, Throwable cause) {
    super(message, cause);
  }

  public ServerRuntimeException(Throwable cause) {
    super(cause);
  }
}
