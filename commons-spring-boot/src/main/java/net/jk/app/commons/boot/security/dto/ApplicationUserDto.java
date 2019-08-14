package net.jk.app.commons.boot.security.dto;

import java.util.HashSet;
import java.util.Set;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.jk.app.commons.boot.validation.PhoneNumber;

/** DTO used for passing in user name / password */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationUserDto {
  @NotEmpty
  @Size(min = 8, max = 50)
  private String name;

  @NotEmpty
  @Size(min = 10, max = 50)
  private String password;

  @NotNull private Long roleId;

  @NotEmpty private String tenantPublicId;

  @Builder.Default private boolean active = true;

  @Size(min = 1, max = 10)
  @Builder.Default
  private Set<String> roles = new HashSet<>();

  private Long dataContextId;

  @Builder.Default private boolean tenantAdmin = false;

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
}
