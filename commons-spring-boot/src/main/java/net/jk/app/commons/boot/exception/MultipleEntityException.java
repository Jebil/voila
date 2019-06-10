package net.jk.app.commons.boot.exception;

import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Standard exception for 400 if we find multiple entities with the requested identifier and have no
 * further way to narrow it down to the one we want
 */
@ResponseStatus(value = HttpStatus.BAD_REQUEST)
@Value
@EqualsAndHashCode(callSuper = true)
public class MultipleEntityException extends InternationalizedException {

  private static final long serialVersionUID = -4441480232238591990L;
  private final Class<?> type;
  private final String publicId;
  private final List<?> multipleEntities;

  public MultipleEntityException(Class<?> type, String publicId, List<?> multipleEntities) {
    super(VoilaError.ENTITY_MULTIPLE_EXIST, type.getSimpleName(), publicId);
    this.type = type;
    this.publicId = publicId;
    this.multipleEntities = multipleEntities;
  }
}
