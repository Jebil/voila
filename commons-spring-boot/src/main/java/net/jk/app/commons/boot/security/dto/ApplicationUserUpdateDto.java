package net.jk.app.commons.boot.security.dto;

import java.util.HashSet;
import java.util.Set;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.jk.app.commons.boot.validation.PhoneNumber;

/** DTO used for updating an application user. For now, force all fields to be set */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationUserUpdateDto {

  @Size(min = 1, max = 10)
  private Set<String> roles = new HashSet<>();

  @NotNull private Long roleId;

  boolean active = true;

  @Size(min = 1, max = 50)
  @NotBlank
  private String firstName;

  @Size(min = 1, max = 50)
  @NotBlank
  private String lastName;

  @Size(min = 1, max = 50)
  private String middleName;

  @Size(min = 1, max = 50)
  @Email
  @NotBlank
  private String email;

  @Size(max = 20)
  @PhoneNumber
  private String phoneNumber;

  @Size(max = 20)
  @PhoneNumber
  private String fax;

  @NotNull private Long dataContextId;
  private Boolean tenantAdmin;
}
