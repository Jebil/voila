package net.jk.app.eurekaserver.security;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@Configuration
@EnableWebSecurity
@Order(1)
public class SecurityConfig extends WebSecurityConfigurerAdapter {

  @Override
  @SuppressFBWarnings(
      value = "SECSPRCSRFPD",
      justification =
          "Passing JWT tokens through Authorization header; only vulnerable to XSS attacks")
  protected void configure(HttpSecurity http) throws Exception {
    http.csrf().disable();
  }
}
