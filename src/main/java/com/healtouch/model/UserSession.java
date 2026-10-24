package com.healtouch.model;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

public class UserSession {
  public final long userId;
  public final String userCode;
  public final String name;
  public final Set<RoleCode> roles;
  public final boolean mustChangePassword;
  public final long credentialVersion;
  private final LongSupplier clock;
  private volatile long lastActivity;
  private volatile boolean valid = true;
  private volatile Set<Permission> permissions = Collections.emptySet();
  public static final long IDLE_TIMEOUT_NANOS = TimeUnit.MINUTES.toNanos(30);

  public UserSession(
      long userId, String userCode, String name, Set<RoleCode> roles, boolean mustChangePassword) {
    this(userId, userCode, name, roles, mustChangePassword, 0, System::nanoTime);
  }

  public UserSession(long userId, String userCode, String name, Set<RoleCode> roles,
      boolean mustChangePassword, long credentialVersion, LongSupplier clock) {
    this.userId = userId;
    this.userCode = userCode;
    this.name = name;
    this.roles =
        Collections.unmodifiableSet(
            roles.isEmpty() ? EnumSet.noneOf(RoleCode.class) : EnumSet.copyOf(roles));
    this.mustChangePassword = mustChangePassword;
    this.credentialVersion = credentialVersion;
    this.clock = clock;
    this.lastActivity = clock.getAsLong();
  }

  public boolean isValid() {
    if (clock.getAsLong() - lastActivity >= IDLE_TIMEOUT_NANOS) valid = false;
    return valid;
  }
  public void touch() { if (isValid()) lastActivity = clock.getAsLong(); }
  public void invalidate() { valid = false; permissions = Collections.emptySet(); }
  public boolean can(Permission permission) { return isValid() && permissions.contains(permission); }
  public void setPermissions(Set<Permission> values) {
    permissions = Collections.unmodifiableSet(values.isEmpty()
        ? EnumSet.noneOf(Permission.class) : EnumSet.copyOf(values));
  }

  public boolean hasRole(RoleCode role) {
    return roles.contains(RoleCode.ADMIN) || roles.contains(role);
  }

  public boolean isAdmin() {
    return roles.contains(RoleCode.ADMIN);
  }
}
