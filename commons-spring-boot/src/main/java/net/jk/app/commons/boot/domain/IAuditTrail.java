package net.jk.app.commons.boot.domain;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import javax.annotation.Nullable;

/**
 * Common interface for entities that need to maintain basic audit trail information, regardless of
 * underlying database technology
 *
 * <p>OffsetDateTime is commonly used in Java 8 Date API for storing timestamp information, as it is
 * not time-zone specific yet contains offset to UTC (unlike LocalDateTime)
 */
public interface IAuditTrail {

  public static ZoneId AUDIT_ZONE_ID = ZoneId.of("UTC");

  void setCreatedBy(String createdBy);

  String getCreatedBy();

  void setCreatedOn(OffsetDateTime createdOn);

  OffsetDateTime getCreatedOn();

  void setModifiedBy(String modifiedBy);

  @Nullable
  String getModifiedBy();

  void setModifiedOn(OffsetDateTime modifiedOn);

  @Nullable
  OffsetDateTime getModifiedOn();
}
