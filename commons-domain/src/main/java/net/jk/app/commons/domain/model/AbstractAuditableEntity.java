package net.jk.app.commons.domain.model;

import java.time.OffsetDateTime;
import javax.persistence.Column;
import javax.persistence.EntityListeners;
import javax.persistence.MappedSuperclass;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import lombok.Data;
import net.jk.app.commons.boot.domain.IAuditTrail;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * An abstract class that implements the {@link IAuditTrail} interface for entities that are
 * persisted to a relational database
 */
@MappedSuperclass
@Data
@EntityListeners(AuditingEntityListener.class)
public class AbstractAuditableEntity implements IAuditTrail {
  public static final String FIELD_CREATED_BY = "createdBy";

  @Column(name = "created_by")
  @CreatedBy
  private String createdBy;

  @Column(name = "created_on")
  @CreatedDate
  private OffsetDateTime createdOn;

  @Column(name = "modified_by")
  @LastModifiedBy
  private String modifiedBy;

  @Column(name = "modified_on")
  @LastModifiedDate
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
