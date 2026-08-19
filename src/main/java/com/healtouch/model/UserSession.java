package com.healtouch.model;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public class UserSession {
  public final long userId;
  public final String userCode;
  public final String name;
  public final Set<RoleCode> roles;
  public final boolean mustChangePassword;

  public UserSession(
      long userId, String userCode, String name, Set<RoleCode> roles, boolean mustChangePassword) {
    this.userId = userId;
    this.userCode = userCode;
    this.name = name;
    this.roles =
        Collections.unmodifiableSet(
            roles.isEmpty() ? EnumSet.noneOf(RoleCode.class) : EnumSet.copyOf(roles));
    this.mustChangePassword = mustChangePassword;
  }

  public boolean hasRole(RoleCode role) {
    return roles.contains(RoleCode.ADMIN) || roles.contains(role);
  }

  public boolean isAdmin() {
    return roles.contains(RoleCode.ADMIN);
  }
}
