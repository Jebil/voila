package net.jk.app.commons.domain.model.tenant;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import net.jk.app.commons.boot.domain.IEntity;
import net.jk.app.commons.domain.model.AbstractAuditableEntity;

/** Entity representing an user role. */
@AllArgsConstructor
@Data
@Entity
@EqualsAndHashCode(
    callSuper = false,
    of = {"roleId"})
@Table(name = "app_role")
@NoArgsConstructor
public class Role extends AbstractAuditableEntity implements IEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "app_role_id")
  private Long roleId;

  @NotNull
  @Column(name = "tenant_id")
  private int tenantId;

  @Column(name = "name")
  @NotNull
  private String name;

  @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
  @JoinTable(
      name = "app_role_to_perm",
      joinColumns = @JoinColumn(name = "app_role_id"),
      inverseJoinColumns = @JoinColumn(name = "app_perm_id"))
  private Set<Permission> permissions = new HashSet<>();

  @Override
  @JsonIgnore
  public String getPublicId() {
    return String.valueOf(roleId);
  }
}
