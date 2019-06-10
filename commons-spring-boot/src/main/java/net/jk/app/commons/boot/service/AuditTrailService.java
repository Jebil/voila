package net.jk.app.commons.boot.service;

import java.time.OffsetDateTime;
import net.jk.app.commons.boot.domain.IAuditTrail;
import net.jk.app.commons.boot.security.domain.IApplicationUser;
import org.springframework.stereotype.Component;

/** Common service for processing auditable entities */
@Component
public class AuditTrailService {

  /** Standard logic to add audit trail information to an auditable entity */
  public void process(IApplicationUser user, IAuditTrail entity) {
    // create vs modify logic
    if (entity.getCreatedBy() == null) {
      entity.setCreatedBy(user.getPublicId());
      entity.setCreatedOn(OffsetDateTime.now());
    } else {
      entity.setModifiedBy(user.getPublicId());
      entity.setModifiedOn(OffsetDateTime.now());
    }
  }
}
