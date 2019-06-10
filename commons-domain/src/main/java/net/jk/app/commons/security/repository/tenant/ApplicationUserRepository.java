package net.jk.app.commons.security.repository.tenant;

import net.jk.app.commons.security.domain.tenant.ApplicationUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationUserRepository extends JpaRepository<ApplicationUser, Long> {}
