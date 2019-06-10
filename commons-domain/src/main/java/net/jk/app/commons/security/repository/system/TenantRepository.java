package net.jk.app.commons.security.repository.system;

import java.util.Optional;
import net.jk.app.commons.boot.repository.IEntityRepository;
import net.jk.app.commons.security.domain.system.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** Spring Data tenant repository */
public interface TenantRepository
    extends JpaRepository<Tenant, Long>, IEntityRepository<Tenant, Long> {

  /** Finds used by the int tenant ID used for striping */
  Optional<Tenant> findByTenantId(int tenantId);

  Optional<Tenant> findByPublicId(String publicId);

  @Query(value = "SELECT MAX(t.tenant_id) + 1 FROM tenant t", nativeQuery = true)
  int findNextTenantId();
}
