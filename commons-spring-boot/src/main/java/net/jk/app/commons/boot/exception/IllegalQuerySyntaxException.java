package net.jk.app.commons.boot.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Catch all exception for any 400 invalid input errors related to invalid query syntax */
@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class IllegalQuerySyntaxException extends InternationalizedException {

  private static final long serialVersionUID = -6614000091726747362L;

  public IllegalQuerySyntaxException() {
    super(VoilaError.INVALID_QUERY_SYNTAX);
  }
}
