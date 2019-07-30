package net.jk.app;

import java.util.Optional;
import net.jk.app.commons.domain.model.tenant.Contact;
import net.jk.app.commons.security.domain.system.Tenant;
import net.jk.app.commons.security.domain.tenant.ApplicationUser;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@ComponentScan
@EnableTransactionManagement
@EntityScan(basePackageClasses = {Tenant.class, ApplicationUser.class, Contact.class})
@EnableJpaAuditing
public class CommonDomainConfiguration {

  @Bean
  public AuditorAware<String> auditorProvider() {
    return new AuditorAware<String>() {
      @Override
      public Optional<String> getCurrentAuditor() {
        return Optional.of("system");
      }
    };
  }
}
