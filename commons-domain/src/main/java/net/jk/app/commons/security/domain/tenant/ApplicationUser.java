package net.jk.app.commons.security.domain.tenant;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.Version;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import net.jk.app.commons.boot.domain.IEntity;
import net.jk.app.commons.boot.domain.ITenantEntity;
import net.jk.app.commons.boot.security.domain.IApplicationUser;
import net.jk.app.commons.boot.security.domain.IPermission;
import net.jk.app.commons.domain.enums.PermissionType;
import net.jk.app.commons.domain.model.AbstractAuditableEntity;
import net.jk.app.commons.domain.model.tenant.Contact;
import net.jk.app.commons.domain.model.tenant.Permission;
import net.jk.app.commons.domain.model.tenant.Role;
import net.jk.app.commons.security.domain.system.Tenant;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

/** Tenant user entity */
@Entity
@Table(name = "app_user")
@Data
@EqualsAndHashCode(callSuper = false)
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
// keep minimal set of fields, as this entity gets serialized to logs from request filter
@ToString(of = {"tenantId", "name"})
public class ApplicationUser extends AbstractAuditableEntity
    implements IApplicationUser, ITenantEntity {

  /** Stores the list of all system permissions. */
  public static final Set<Permission> ALL_PERMS =
      Arrays.stream(PermissionType.values())
          .map(pt -> new Permission((long) pt.ordinal(), pt, ""))
          .collect(Collectors.toSet());

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "app_user_id")
  private Long applicationuUserId;

  @Column(name = "tenant_id")
  private int tenantId;

  @ManyToOne
  @JoinColumn(name = "app_role_id")
  private Role role;

  // set manually in svc layer to implement IApplicationUser
  @Transient private Tenant tenant;

  @NotEmpty
  @Size(min = 3, max = 100)
  @Column(name = "name")
  private String name;

  @NotEmpty
  @Column(name = "password_hash")
  private String passwordHash;

  @Size(min = 1)
  @Column(name = "roles", columnDefinition = "jsonb")
  @Type(type = "jsonb")
  private Set<String> roles;

  @Column(name = "active")
  private boolean active;

  @Version
  @Column(name = "version")
  private Integer version;

  @OneToOne
  @JoinColumn(name = "contact_id")
  private Contact contact;

  @Column(name = "tenant_admin")
  private boolean userIsTenantAdmin;

  @JsonIgnore
  public boolean belongsToTenant(int tenantId) {
    return tenantId == getTenantId();
  }

  /** Tenant public ID */
  @Override
  @JsonGetter("tenantPublicId")
  public @Nullable String getTenantPublicId() {
    if (this.getTenant() != null) {
      return this.getTenant().getPublicId();
    } else {
      return null;
    }
  }

  @Override
  public Set<IPermission> getPermissions() {
    Set<IPermission> permissions = Collections.unmodifiableSet(role.getPermissions());
    if (this.userIsTenantAdmin) {
      return Collections.unmodifiableSet(ALL_PERMS);
    }
    return permissions;
  }

  @Override
  public boolean isGrantedPermission(IPermission permission) {
    // TODO: this is obviously non-performant; unfortunately, limitations of the existing domain
    // model and dependencies prevents simplifying the approach.
    return getPermissions()
        .stream()
        .anyMatch(p -> p.getPermissionName().equals(permission.getPermissionName()));
  }

  @Override
  public boolean isGrantedPermission(String permissionName) {
    return getPermissions().stream().anyMatch(p -> p.getPermissionName().equals(permissionName));
  }

  @Override
  public int compareTo(IEntity o) {
    return Comparator.comparing(ApplicationUser::getTenantId)
        .thenComparing(ApplicationUser::getPublicId)
        .compare(this, (ApplicationUser) o);
  }

  @Override
  public String getEmailAddress() {
    return contact.getEmail();
  }
}
