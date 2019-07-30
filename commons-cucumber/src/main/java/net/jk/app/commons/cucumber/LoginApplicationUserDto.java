package net.jk.app.commons.cucumber;

import com.google.common.collect.Lists;
import java.util.List;
import lombok.Data;

/** DTO used for passing in user name / password */
@Data
public class LoginApplicationUserDto {
  private String name;
  private String password;
  private List<String> roles = Lists.newArrayList();
}
