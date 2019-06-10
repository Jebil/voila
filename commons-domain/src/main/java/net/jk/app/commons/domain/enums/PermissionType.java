package net.jk.app.commons.domain.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Getter;

/** Enumerates the application permissions. TODO: revamp the naming convention. */
@Getter
public enum PermissionType {
  ADMIN_USERS_VIEW("admin.users.view"),
  ADMIN_USERS_CREATE("admin.users.add"),
  ADMIN_USERS_DELETE("admin.users.delete"),
  ADMIN_USERS_ACTIVATE("admin.users.activate"),
  ADMIN_ROLES_VIEW("admin.roles.view"),
  ADMIN_ROLES_CREATE("admin.roles.add"),
  ADMIN_ROLES_DELETE("admin.roles.delete");

  PermissionType(String name) {
    this.name = name;
  }

  @Getter @JsonValue private final String name;

  /** Stores the mapping of all permissions. */
  private static final Map<String, PermissionType> ENUM_MAP;

  public static final Comparator<PermissionType> PRIORITY_COMPARATOR =
      Comparator.comparing(PermissionType::getName);

  static {
    Map<String, PermissionType> map = new ConcurrentHashMap<String, PermissionType>();
    for (PermissionType instance : PermissionType.values()) {
      map.put(instance.getName(), instance);
    }
    ENUM_MAP = Collections.unmodifiableMap(map);
  }

  /**
   * Converts the permission name to the PermissionType enum.
   *
   * @param name the permission name to be converted
   * @return the corresponding permission type enum
   * @throws IllegalArgumentException if the provided permission name is not a valid permission.
   */
  public static PermissionType getByPermissionName(String name) throws IllegalArgumentException {
    PermissionType perm = ENUM_MAP.get(name);
    if (perm == null) {
      throw new IllegalArgumentException("Unsupported permission");
    }
    return perm;
  }
}
