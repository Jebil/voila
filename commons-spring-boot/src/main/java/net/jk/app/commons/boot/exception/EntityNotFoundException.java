package net.jk.app.commons.boot.exception;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import lombok.EqualsAndHashCode;
import lombok.Value;
import net.jk.app.commons.boot.utils.ExceptionUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Standard exception for 404 NOT FOUND errors */
@ResponseStatus(value = HttpStatus.NOT_FOUND)
@Value
@EqualsAndHashCode(callSuper = true)
public class EntityNotFoundException extends InternationalizedException {

  private static final long serialVersionUID = 8707599161621833823L;
  private final String typeName;
  private final String fieldName;
  private final Map<String, Object> finderValues;

  public EntityNotFoundException(Class<?> type, String publicId) {
    this(type, ImmutableMap.of("publicId", publicId));
  }

  public EntityNotFoundException(Class<?> type, Map<String, Object> finderValues) {
    this(type.getSimpleName(), null, finderValues);
  }

  public EntityNotFoundException(
      Class<?> type, String fieldName, Map<String, Object> finderValues) {
    this(type.getSimpleName(), fieldName, finderValues);
  }

  public EntityNotFoundException(
      String typeName, String fieldName, Map<String, Object> finderValues) {
    super(
        VoilaError.ENTITY_DOES_NOT_EXIST, typeName, ExceptionUtils.getFinderSummary(finderValues));
    this.typeName = typeName;
    this.fieldName = fieldName;
    this.finderValues = finderValues;
  }
}
