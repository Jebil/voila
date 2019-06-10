package net.jk.app.commons.boot.exception;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

import org.springframework.web.bind.annotation.ResponseStatus;

/** Exception wrapper to hold exceptions that should be reported as 500. */
@ResponseStatus(value = INTERNAL_SERVER_ERROR)
public class VoilaRuntimeException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public VoilaRuntimeException(Exception e) {
    super(e);
  }
}
