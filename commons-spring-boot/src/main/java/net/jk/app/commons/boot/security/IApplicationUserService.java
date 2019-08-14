package net.jk.app.commons.boot.security;

import java.util.Optional;
import lombok.NonNull;
import net.jk.app.commons.boot.security.domain.IApplicationUser;
import net.jk.app.commons.boot.security.dto.ApplicationUserDto;
import net.jk.app.commons.boot.security.dto.ApplicationUserUpdateDto;
import net.jk.app.commons.boot.service.ICrudServiceV2;
import net.jk.app.commons.boot.service.IEntityService;

/**
 * Common DB-independent interface for application user service
 *
 * <p>Each application should implement their own, depending on underlying DB technology
 */
public interface IApplicationUserService<E extends IApplicationUser>
    extends ICrudServiceV2<E, ApplicationUserDto, ApplicationUserUpdateDto, String>,
        IEntityService<E> {

  Optional<E> getByName(String name);

  E getExistingByName(String name);

  /** For internal background scheduled tasks, specific to a particular tenant */
  @NonNull
  E getTenantBackgroundUser(IApplicationUser user, int tenantId);

  /** For internal background scheduled tasks, specific to a particular tenant */
  @NonNull
  E getTenantBackgroundUser(IApplicationUser user, String tenantPublicId);

  /** For internal tasks only that require a system tenant user to get going */
  E getSystemTenantBackgroundUser();
}
