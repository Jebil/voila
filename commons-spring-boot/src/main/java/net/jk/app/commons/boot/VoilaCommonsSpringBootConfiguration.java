package net.jk.app.commons.boot;

import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;

@Configuration
@ComponentScan
@EnableScheduling
@EnableTransactionManagement
@EnableAspectJAutoProxy
public class VoilaCommonsSpringBootConfiguration {
  private static final char[] HEX_CODE = "0123456789ABCDEF".toCharArray();

  private static String printHexBinary(byte[] data) {
    StringBuilder r = new StringBuilder(data.length * 2);
    for (byte b : data) {
      r.append(HEX_CODE[(b >> 4) & 0xF]);
      r.append(HEX_CODE[(b & 0xF)]);
    }
    return r.toString();
  }

  // primitively obfuscate the signing key used for JWT signing
  // TODO: fetch this from a key vault in AWS in the future
  public static final String TEST_STRING =
      printHexBinary(
          ThreadLocals.STRINGBUILDER
              .get()
              .append(321)
              .append(323.322f)
              .append(true)
              .append("ts^A&n,mn,zxcvzxcc@#t")
              .append(false)
              .append(73.d)
              .append("avcfnJD&*")
              .append(" z a ")
              .append(4235d)
              .toString()
              .replace("3", "7 ")
              .replace("4", " 8 ")
              .toUpperCase()
              .getBytes(StandardCharsets.UTF_8));

  @Bean
  public BCryptPasswordEncoder bCryptPasswordEncoder(
      @Value("${voila.security.bcryptStrength}") int strength) {
    // strongest round allowed
    return new BCryptPasswordEncoder(strength);
  }

  //  @Bean
  //  public AuthenticationManager authenticationManager() {
  //    // return dummy auth manager that does nothing, Spring Security needs it
  //    return new AuthenticationManager() {
  //      @Override
  //      public Authentication authenticate(Authentication authentication)
  //          throws AuthenticationException {
  //        return authentication;
  //      }
  //    };
  //  }

  /**
   * Enables the TimedAspect advice that will allow methods annotated with {@link
   * io.micrometer.core.annotation.Timed} to have timer metrics recorded about them. The methods
   * must be public to be able to be timed.
   */
  @Bean
  public TimedAspect timedAspect(@Autowired MeterRegistry registry) {
    return new TimedAspect(registry);
  }

  /** To enable method level validations eg: to validate path params */
  @Bean
  public MethodValidationPostProcessor methodValidationPostProcessor() {
    return new MethodValidationPostProcessor();
  }
}
