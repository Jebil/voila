package net.jk.app.commons.boot.exception;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import lombok.EqualsAndHashCode;
import lombok.Value;
import net.jk.app.commons.boot.utils.ExceptionUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Standard exception for 409 conflict errors (if there is an unexpected conflict in existing data)
 */
@ResponseStatus(value = HttpStatus.CONFLICT)
@Value
@EqualsAndHashCode(callSuper = true)
public class EntityExistsException extends InternationalizedException {

  private static final long serialVersionUID = 1287666399236658623L;
  private final Class<?> type;
  private final Map<String, Object> finderValues;

  public EntityExistsException(Class<?> type, String publicId) {
    this(type, ImmutableMap.of("publicId", publicId));
  }

  public EntityExistsException(Class<?> type, Map<String, Object> finderValues) {
    super(
        VoilaError.ENTITY_ALREADY_EXISTS,
        type.getSimpleName(),
        ExceptionUtils.getFinderSummary(finderValues));
    this.type = type;
    this.finderValues = finderValues;
  }
}
