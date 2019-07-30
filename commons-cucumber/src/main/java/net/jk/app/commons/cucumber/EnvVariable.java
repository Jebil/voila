package net.jk.app.commons.cucumber;

import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;

/**
 * A simple class that represents an ENV variable name that we should look up and the default value
 * it should return if not found
 */
@AllArgsConstructor
public class EnvVariable {
  private final String environmentVariableName;
  private final String defaultValue;

  public String getValue() {
    String env = System.getenv(environmentVariableName);
    return StringUtils.isEmpty(env) ? defaultValue : env;
  }
}
