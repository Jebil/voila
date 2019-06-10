package net.jk.app.commons.boot.exception;

import lombok.Getter;
import net.jk.app.commons.boot.ApplicationContextProvider;
import org.springframework.context.i18n.LocaleContextHolder;

/** Standard internationalized exception that expects message name + parameters */
public abstract class InternationalizedException extends RuntimeException {

  private static final long serialVersionUID = 4630786080242440985L;
  @Getter private final IVoilaError error;
  @Getter private final Object[] messageParameters;

  public InternationalizedException(IVoilaError error, Object... messageParameters) {
    super(error.getMessageKey());
    this.error = error;
    this.messageParameters = messageParameters;
  }

  @Override
  public String getMessage() {
    return ApplicationContextProvider.getI18n()
        .getMessage(error.getMessageKey(), messageParameters, LocaleContextHolder.getLocale());
  }
}
