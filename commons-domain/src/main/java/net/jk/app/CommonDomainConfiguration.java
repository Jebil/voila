package net.jk.app;

import net.jk.app.commons.domain.model.tenant.Contact;
import net.jk.app.commons.security.domain.system.Tenant;
import net.jk.app.commons.security.domain.tenant.ApplicationUser;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@ComponentScan
@EnableTransactionManagement
@EntityScan(basePackageClasses = {Tenant.class, ApplicationUser.class, Contact.class})
public class CommonDomainConfiguration {}
