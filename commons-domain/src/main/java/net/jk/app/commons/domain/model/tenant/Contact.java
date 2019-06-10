package net.jk.app.commons.domain.model.tenant;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Version;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import net.jk.app.commons.boot.domain.ITenantEntity;
import net.jk.app.commons.boot.validation.PhoneNumber;
import net.jk.app.commons.domain.model.AbstractAuditableEntity;

/**
 * Entity representing contacts in the system.
 *
 * <p>Validations are added at the getter method level to alleviate
 * https://hibernate.atlassian.net/browse/HV-535
 *
 * @author jebil.kuruvila
 */
@Entity
@Table(name = "contact")
@Data
@EqualsAndHashCode(
    callSuper = false,
    of = {"tenantId", "firstName", "lastName", "email"})
@NoArgsConstructor
@AllArgsConstructor
public class Contact extends AbstractAuditableEntity implements ITenantEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "contact_id")
  private Long contactId;

  @Column(name = "tenant_id")
  private int tenantId;

  @Column(name = "first_name")
  private String firstName;

  @Column(name = "last_name")
  private String lastName;

  @Column(name = "middle_name")
  private String middleName;

  @Column(name = "email")
  private String email;

  @Column(name = "phone_number")
  private String phoneNumber;

  @Column(name = "fax")
  private String fax;

  @Version
  @Column(name = "version")
  private Integer version;

  @Override
  public String getPublicId() {
    return String.valueOf(contactId);
  }

  public int getTenantId() {
    return tenantId;
  }

  @NotEmpty
  public String getFirstName() {
    return firstName;
  }

  @NotEmpty
  public String getLastName() {
    return lastName;
  }

  @Email
  @NotNull
  public String getEmail() {
    return email;
  }

  @PhoneNumber
  public String getPhoneNumber() {
    return phoneNumber;
  }

  @PhoneNumber
  public String getFax() {
    return fax;
  }

  /**
   * @param firstName
   * @param lastName
   * @param email
   *     <p>For the search API
   */
  public Contact(String firstName, String lastName, String email) {
    this.firstName = firstName;
    this.lastName = lastName;
    this.email = email;
  }
}
