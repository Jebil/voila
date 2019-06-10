package net.jk.app.commons.domain.model.tenant;

import com.fasterxml.jackson.annotation.JsonIgnore;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.jk.app.commons.boot.domain.IEntity;
import net.jk.app.commons.boot.security.domain.IPermission;
import net.jk.app.commons.domain.enums.PermissionType;

/** Entity representing a user permission. */
@AllArgsConstructor
@Data
@Entity
@EqualsAndHashCode(
    callSuper = false,
    of = {"permissionId"})
@Table(name = "permission")
@NoArgsConstructor
public class Permission implements IEntity, IPermission {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "permission_id")
  private Long permissionId;

  @NotNull
  @Column(name = "name")
  @Enumerated(EnumType.STRING)
  private PermissionType name;

  @Override
  public String getPermissionName() {
    return name.getName();
  }

  @Column(name = "description")
  @NotNull
  @Setter(AccessLevel.NONE)
  private String description;

  @Override
  @JsonIgnore
  public String getPublicId() {
    return String.valueOf(permissionId);
  }
}
