package net.jk.app.commons.boot.exception;

import com.google.common.collect.ImmutableSortedSet;
import java.nio.file.AccessDeniedException;
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.validation.ConstraintViolationException;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** Common handling logic for standard exceptions */
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

  @Qualifier("messageSource")
  @Autowired
  private MessageSource i18n;

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorSummary> handleIllegalArgumentException(IllegalArgumentException e) {
    log.debug("{}", e.getMessage(), e);
    return new ResponseEntity<>(new ErrorSummary(e.getLocalizedMessage()), HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ErrorSummary> handleConstraintViolationException(
      ConstraintViolationException e) {
    log.debug("{}", e.getMessage(), e);
    Set<Error> errors =
        e.getConstraintViolations()
            .stream()
            .map(v -> new Error(v.getMessage(), String.valueOf(v.getPropertyPath())))
            .collect(Collectors.toSet());

    return new ResponseEntity<>(new ErrorSummary(errors), HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(InputValidationException.class)
  public ResponseEntity<ErrorSummary> handleInputValidationException(InputValidationException e) {
    String message = e.getMessage();
    log.debug("{}", message, e);

    return new ResponseEntity<>(
        new ErrorSummary(message, e.getPropertyPath()), HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(InvalidRequestMultiException.class)
  public ResponseEntity<ErrorSummary> handleInvalidRequestMultiException(
      InvalidRequestMultiException ex) {
    Set<Error> errors =
        ex.getExceptions()
            .entries()
            .stream()
            .map(
                e ->
                    new Error(
                        i18n.getMessage(
                            e.getKey().getMessageKey(),
                            e.getValue(),
                            LocaleContextHolder.getLocale())))
            .collect(Collectors.toSet());
    return new ResponseEntity<>(new ErrorSummary(errors), HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ErrorSummary> handleMethodArgumentTypeMismatchException(
      MethodArgumentTypeMismatchException e) {
    log.debug("{}", e.getMessage(), e);

    return new ResponseEntity<>(
        new ErrorSummary(e.getLocalizedMessage(), e.getName()), HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorSummary> handleMethodArgumentNotValidException(
      MethodArgumentNotValidException e) {
    log.debug("{}", e.getMessage(), e);
    // field errord
    Set<Error> errors =
        e.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(fe -> new Error(fe.getDefaultMessage(), fe.getField()))
            .collect(Collectors.toSet());

    // object level errors
    errors.addAll(
        e.getBindingResult()
            .getGlobalErrors()
            .stream()
            .map(v -> new Error(v.getDefaultMessage(), v.getObjectName()))
            .collect(Collectors.toSet()));

    return new ResponseEntity<>(new ErrorSummary(errors), HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ErrorSummary> handleAccessDeniedException(AccessDeniedException e) {
    log.warn("{}", e.getMessage(), e);
    return new ResponseEntity<>(new ErrorSummary(e.getLocalizedMessage()), HttpStatus.FORBIDDEN);
  }

  @ExceptionHandler(SecurityException.class)
  public ResponseEntity<ErrorSummary> handleSecurityException(SecurityException e) {
    log.warn("{}", e.getMessage(), e);
    return new ResponseEntity<>(new ErrorSummary(e.getLocalizedMessage()), HttpStatus.UNAUTHORIZED);
  }

  @ExceptionHandler(BadCredentialsException.class)
  public ResponseEntity<ErrorSummary> handleBadCredentialsException(BadCredentialsException e) {
    log.warn("{}", e.getMessage(), e);
    return new ResponseEntity<>(new ErrorSummary(e.getLocalizedMessage()), HttpStatus.UNAUTHORIZED);
  }

  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<ErrorSummary> handleAuthenticationException(AuthenticationException e) {
    String message = e.getMessage();
    log.warn("{}", message, e);
    return new ResponseEntity<>(new ErrorSummary(message), HttpStatus.UNAUTHORIZED);
  }

  @ExceptionHandler(ForbiddenException.class)
  public ResponseEntity<ErrorSummary> handleForbiddenException(ForbiddenException e) {
    String message = e.getMessage();
    log.warn("{}", message, e);
    return new ResponseEntity<>(new ErrorSummary(message), HttpStatus.FORBIDDEN);
  }

  @ExceptionHandler(EntityNotFoundException.class)
  public ResponseEntity<ErrorSummary> handleEntityNotFoundException(EntityNotFoundException e) {
    String message = e.getMessage();
    log.info("{}", message, e);
    if (e.getFinderValues().size() == 1 && e.getFinderValues().containsKey("publicId")) {
      // backwards compatibility
      return new ResponseEntity<>(new ErrorSummary(message, "publicId"), HttpStatus.NOT_FOUND);
    } else {
      return new ResponseEntity<>(
          new ErrorSummary(message, e.getFieldName()), HttpStatus.NOT_FOUND);
    }
  }

  @ExceptionHandler(EntityExistsException.class)
  public ResponseEntity<ErrorSummary> handleEntityExistsException(EntityExistsException e) {
    String message = e.getMessage();
    log.debug("{}", message, e);
    return new ResponseEntity<>(new ErrorSummary(message), HttpStatus.CONFLICT);
  }

  @ExceptionHandler(MultipleEntityException.class)
  public ResponseEntity<ErrorSummary> handleMultipleEntityException(MultipleEntityException e) {
    String message = e.getMessage();
    log.debug("{}", message, e);
    return new ResponseEntity<>(new ErrorSummary(message), HttpStatus.BAD_REQUEST);
  }

  // thrown when input JSON is malformed
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorSummary> handleMessageNotReadbleException(
      HttpMessageNotReadableException e) {
    log.debug("{}", e.getMessage(), e);
    return new ResponseEntity<>(new ErrorSummary(e.getMessage()), HttpStatus.BAD_REQUEST);
  }

  // thrown when @Valid validation fails on REST API DTOs
  @ExceptionHandler(BindException.class)
  public ResponseEntity<ErrorSummary> handleBindException(BindException e) {
    log.debug("{}", e.getMessage(), e);
    // TODO: pass in field in error information
    return new ResponseEntity<>(new ErrorSummary(e.getMessage()), HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(NonUpdateableFieldViolationException.class)
  public ResponseEntity<ErrorSummary> handleNonUpdateableFieldViolationException(
      NonUpdateableFieldViolationException e) {
    String message = e.getMessage();
    log.debug("{}", message, e);
    return new ResponseEntity<>(new ErrorSummary(message, e.getFieldName()), HttpStatus.CONFLICT);
  }

  @ExceptionHandler(InvalidValueForAttributeException.class)
  public ResponseEntity<ErrorSummary> handleInvalidValueForAttributeException(
      InvalidValueForAttributeException e) {
    String message = e.getMessage();
    log.debug("{}", message, e);
    return new ResponseEntity<>(new ErrorSummary(message, e.getFieldName()), HttpStatus.CONFLICT);
  }

  @ExceptionHandler(MissingServletRequestParameterException.class)
  public ResponseEntity<ErrorSummary> handle(MissingServletRequestParameterException e) {
    String message =
        i18n.getMessage(
            VoilaError.QUERY_PARAM_REQUIRED.getMessageKey(),
            new Object[] {e.getParameterName()},
            LocaleContextHolder.getLocale());
    log.debug("{}", message, e);
    return new ResponseEntity<>(new ErrorSummary(message), HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(InvalidRequestException.class)
  public ResponseEntity<ErrorSummary> handleInvalidRequestException(InvalidRequestException e) {
    String message = e.getMessage();
    log.debug("{}", message, e);
    return new ResponseEntity<>(new ErrorSummary(message), HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(Throwable.class)
  public ResponseEntity<ErrorSummary> handleUncaughtExceptions(Throwable e) {
    // respect HTTP status if defined
    ResponseStatus status = e.getClass().getAnnotation(ResponseStatus.class);
    if (status != null) {
      if (status.value() == HttpStatus.INTERNAL_SERVER_ERROR) {
        log.error("Response exception: {}", e.getMessage(), e);
      } else {
        log.info("Response exception: {}", e.getMessage(), e);
      }
      return new ResponseEntity<>(new ErrorSummary(e.getMessage()), status.value());
    } else {
      log.error("UNEXPECTED SERVER ERROR", e);
      Throwable t = e.getCause();
      if (t == null) {
        return new ResponseEntity<>(
            new ErrorSummary(i18n.getMessage("serverError", null, LocaleContextHolder.getLocale())),
            HttpStatus.INTERNAL_SERVER_ERROR);

      } else if (t instanceof ConstraintViolationException) {
        return handleConstraintViolationException((ConstraintViolationException) t);
      } else {
        // go one level deeper, just in case
        t = t.getCause();
        if (t instanceof ConstraintViolationException) {
          return handleConstraintViolationException((ConstraintViolationException) t);
        } else {
          return new ResponseEntity<>(
              new ErrorSummary(
                  i18n.getMessage("serverError", null, LocaleContextHolder.getLocale())),
              HttpStatus.INTERNAL_SERVER_ERROR);
        }
      }
    }
  }

  @Value
  @AllArgsConstructor
  private static class ErrorSummary {

    // make sure it is always sorted for testability
    private ImmutableSortedSet<Error> errors;

    public ErrorSummary(String error) {
      this.errors = ImmutableSortedSet.of(new Error(error));
    }

    public ErrorSummary(String error, String fieldName) {
      this.errors = ImmutableSortedSet.of(new Error(error, fieldName));
    }

    public ErrorSummary(Set<Error> errors) {
      if (errors instanceof ImmutableSortedSet) {
        this.errors = (ImmutableSortedSet<Error>) errors;
      } else {
        this.errors = ImmutableSortedSet.copyOf(errors);
      }
    }
  }

  @Value
  private static class Error implements Comparable<Error> {
    private @NonNull String error;
    private @NonNull String fieldName;

    Error(@NonNull String error) {
      this.error = error;
      fieldName = "";
    }

    Error(@NonNull String error, @Nullable String fieldName) {
      this.error = error;
      this.fieldName = (fieldName != null) ? fieldName : "";
    }

    @Override
    public int compareTo(Error o) {
      return Comparator.comparing(Error::getFieldName)
          .thenComparing(Error::getError)
          .compare(this, o);
    }
  }
}
