package net.jk.app.commons.domain.model;

import java.time.OffsetDateTime;
import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import lombok.Data;
import net.jk.app.commons.boot.domain.IAuditTrail;

/**
 * An abstract class that implements the {@link IAuditTrail} interface for entities that are
 * persisted to a relational database
 */
@MappedSuperclass
@Data
public class AbstractAuditableEntity implements IAuditTrail {
  public static final String FIELD_CREATED_BY = "createdBy";

  @Column(name = "created_by")
  private String createdBy;

  @Column(name = "created_on")
  private OffsetDateTime createdOn;

  @Column(name = "modified_by")
  private String modifiedBy;

  @Column(name = "modified_on")
  private OffsetDateTime modifiedOn;

  @NotEmpty
  public String getCreatedBy() {
    return createdBy;
  }

  @NotNull
  public OffsetDateTime getCreatedOn() {
    return createdOn;
  }
}
