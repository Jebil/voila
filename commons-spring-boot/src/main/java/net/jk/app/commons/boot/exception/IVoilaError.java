package net.jk.app.commons.boot.exception;

/** Interface representing an error condition in an application */
public interface IVoilaError {

  /** Get the message key that maps to an internationalized version of the message */
  String getMessageKey();
}
