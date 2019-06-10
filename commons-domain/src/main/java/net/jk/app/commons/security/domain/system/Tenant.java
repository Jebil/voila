package net.jk.app.commons.security.domain.system;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Version;
import javax.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.jk.app.commons.boot.domain.IAuditTrail;
import net.jk.app.commons.boot.domain.IEntity;
import net.jk.app.commons.boot.security.domain.ITenant;
import net.jk.app.commons.domain.model.AbstractAuditableEntity;

@Entity
@Table(name = "tenant")
@Data
@EqualsAndHashCode(of = "publicId", callSuper = false)
public class Tenant extends AbstractAuditableEntity implements ITenant, IEntity, IAuditTrail {

  /** Int tenant ID used in all data striping for min memory/disk usage */
  @Id
  @Column(name = "tenant_id")
  private int tenantId;

  /** The public ID exposed via REST API should be unique per entity type */
  @Column(name = "public_id")
  private String publicId;

  @Column(name = "name")
  @Size(min = 3, max = 100)
  private String name;

  @Column(name = "logo_url")
  private String logoUrl;

  // tells us if this instance was system-created or part of user-created data
  // used for BDD data cleanups between scenarios
  @Column(name = "system")
  private boolean system;

  // support for OPTIMISTIC locking
  @Version
  @Column(name = "version")
  private Integer version;
}
