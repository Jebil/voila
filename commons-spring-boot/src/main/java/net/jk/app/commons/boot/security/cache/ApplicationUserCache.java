package net.jk.app.commons.boot.security.cache;

import java.util.List;
import net.jk.app.commons.boot.cache.AbstractSimpleEntityCache;
import net.jk.app.commons.boot.security.IApplicationUserService;
import net.jk.app.commons.boot.security.domain.IApplicationUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * In-memory application user cache for fast access during authentication/authorization without
 * hitting DB
 */
@Component
public class ApplicationUserCache
    extends AbstractSimpleEntityCache<IApplicationUser, IApplicationUser> {

  @Autowired private IApplicationUserService<? extends IApplicationUser> userService;

  @Override
  protected Class<IApplicationUser> getEntityType() {
    return IApplicationUser.class;
  }

  @Override
  protected IApplicationUser queryByPublicId(IApplicationUser user, String publicId) {
    return userService.getExistingByName(publicId);
  }

  @Override
  protected List<IApplicationUser> queryAll(IApplicationUser user) {
    return (List<IApplicationUser>) userService.getAll(user);
  }
}
