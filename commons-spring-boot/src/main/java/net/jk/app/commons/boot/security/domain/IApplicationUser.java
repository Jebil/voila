package net.jk.app.commons.boot.security.domain;

import com.google.common.collect.ImmutableSetMultimap;
import java.security.Principal;
import java.util.Set;
import lombok.NonNull;
import net.jk.app.commons.boot.domain.ITenantEntity;
import net.jk.app.commons.boot.security.SecurityConstants;
import net.jk.app.commons.boot.security.permissions.IDataPermission;

/**
 * Simple interface to represent tenant/user Implementation needs to be concrete entity using
 * whatever DB technology a service is using m
 */
public interface IApplicationUser extends ITenantEntity, Principal {

  /** User email address */
  String getEmailAddress();

  /** One-way secure password hash for login process */
  String getPasswordHash();

  /** High-level user roles (admin vs regular) */
  Set<String> getRoles();

  /** Tenant the user belongs to */
  ITenant getTenant();

  /** Get the set of permissions this user is granted */
  Set<IPermission> getPermissions();

  /**
   * Returns false if user was disabled (e.g. left company, etc) but we still need to maintain
   * historical data in DB that references them
   *
   * <p>Should not be allowed to log in if disabled.
   */
  boolean isActive();

  boolean isUserIsTenantAdmin();

  /** Returns true if user has the given permission, false otherwise. */
  boolean isGrantedPermission(String permissionName);

  /** Helper method to help with security logic */
  default boolean isInRole(@NonNull String roleName) {
    return getRoles().contains(roleName);
  }

  default boolean isSystemAdmin() {
    return isInRole(SecurityConstants.ROLE_SYSTEM_ADMIN);
  }

  default boolean isTenantAdmin() {
    return isInRole(SecurityConstants.ADMIN);
  }

  default boolean isTenantUser() {
    return isInRole(SecurityConstants.USER);
  }

  default boolean isTenantPartner() {
    return isInRole(SecurityConstants.PARTNER);
  }

  default boolean isTenantAdminOf(int tenantId) {
    return tenantId == getTenantId() && isTenantAdmin();
  }

  default boolean isTenantAdminOf(String tenantPublicId) {
    return getTenant().getPublicId().equals(tenantPublicId) && isTenantAdmin();
  }

  default boolean isTenantUserOf(int tenantId) {
    return tenantId == getTenantId() && isTenantUser();
  }

  default boolean isTenantUserOf(String tenantPublicId) {
    return getTenant().getPublicId().equals(tenantPublicId) && isTenantUser();
  }

  default boolean isTenantPartnerOf(int tenantId) {
    return tenantId == getTenantId() && isTenantPartner();
  }

  default boolean isGrantedPermission(IPermission permission) {
    return getPermissions().contains(permission);
  }

  /** Helper method to quickly get tenant Id */
  @Override
  default int getTenantId() {
    return getTenant().getTenantId();
  }

  /** Helper method to quickly get tenant Id */
  default String getTenantPublicId() {
    return getTenant().getPublicId();
  }

  /** List of user data permissions, grouped by type of data permissions */
  default ImmutableSetMultimap<Class<? extends IDataPermission>, IDataPermission>
      getDataPermissions() {
    return ImmutableSetMultimap.of();
  }

  @Override
  default String getPublicId() {
    return getName();
  }
}
