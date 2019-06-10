package net.jk.app.commons.domain.repository.tenant;

import net.jk.app.commons.domain.model.tenant.Contact;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContactRepository extends JpaRepository<Contact, Long> {}
