package net.jk.app.commons.boot.security;

import com.google.common.collect.ImmutableSet;
import net.jk.app.commons.boot.VoilaCommonsSpringBootConfiguration;
import org.springframework.util.StringUtils;

/** Common security constants */
public class SecurityConstants {

  /** Admin across entire system, an employe * */
  public static final String SYSTEM_ADMIN = "SYSTEM_ADMIN";

  public static final String ROLE_SYSTEM_ADMIN = "SYSTEM_ADMIN";

  /** Admin within a SINGLE tenant */
  public static final String ADMIN = "SYSTEM";

  public static final String ROLE_ADMIN = "ROLE_ADMIN";

  /** User within single tenant */
  public static final String USER = "USER";

  public static final String ROLE_USER = "ROLE_USER";

  /**
   * 3rd party partner within single tenant with limited data visibility e.g. a carrier within the
   * contxt of a shipper
   */
  public static final String PARTNER = "PARTNER";

  public static final String ROLE_PARTNER = "ROLE_PARTNER";

  /** For easy validation */
  public static final ImmutableSet<String> ALL_ROLES = ImmutableSet.of(ADMIN, USER);

  public static final long EXPIRATION_TIME = 864_000_000; // 10 days
  public static final String TOKEN_PREFIX = "Bearer ";
  public static final String HEADER_AUTHORIZATION = "Authorization";

  // obfuscate meaning of this variable, it will actually be used for signing JWT tokens
  static final String ROLE_DUMMY;

  static {
    // obfuscate as much as possible, in case someone gets access to the compiled binary
    // it won't be impossible, but at least let's not make it easy
    ROLE_DUMMY =
        StringUtils.delete(VoilaCommonsSpringBootConfiguration.TEST_STRING, " ").toLowerCase();
  }
}
