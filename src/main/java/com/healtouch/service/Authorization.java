package com.healtouch.service;

import com.healtouch.model.*;
import java.sql.*;
import java.util.*;
import javax.sql.DataSource;

/** Database authorization is authoritative; the permission cache is only for UI rendering. */
public final class Authorization {
  private static volatile DataSource dataSource;
  private static final Set<UserSession> sessions = Collections.synchronizedSet(
      Collections.newSetFromMap(new WeakHashMap<UserSession, Boolean>()));
  private Authorization() {}
  public static void configure(DataSource source) { dataSource = source; }
  public static class SessionExpiredException extends SecurityException {
    public SessionExpiredException() { super("会话已失效，请重新登录"); }
  }
  public static void invalidateAll() {
    synchronized (sessions) { for (UserSession session : sessions) session.invalidate(); }
  }
  public static void validateSession(UserSession session) { refresh(session); }
  public static void refresh(UserSession session) {
    if (session == null || !session.isValid()) throw new SessionExpiredException();
    DataSource source = dataSource;
    if (source == null) throw new IllegalStateException("权限数据源尚未初始化");
    try (Connection c = source.getConnection();
        PreparedStatement user = c.prepareStatement("SELECT active,session_version FROM app_user WHERE id=?")) {
      user.setLong(1, session.userId);
      try (ResultSet rs = user.executeQuery()) {
        if (!rs.next() || !rs.getBoolean(1) || rs.getLong(2) != session.credentialVersion) {
          session.invalidate(); throw new SessionExpiredException();
        }
      }
      EnumSet<RoleCode> roles = EnumSet.noneOf(RoleCode.class);
      try (PreparedStatement ps = c.prepareStatement(
          "SELECT r.code FROM role r JOIN user_role ur ON r.id=ur.role_id WHERE ur.user_id=?")) {
        ps.setLong(1, session.userId);
        try (ResultSet rs = ps.executeQuery()) { while (rs.next()) roles.add(RoleCode.valueOf(rs.getString(1))); }
      }
      if (!roles.equals(session.roles)) { session.invalidate(); throw new SessionExpiredException(); }
      EnumSet<Permission> permissions = EnumSet.noneOf(Permission.class);
      if (roles.contains(RoleCode.ADMIN)) permissions = EnumSet.allOf(Permission.class);
      else {
        try (PreparedStatement ps = c.prepareStatement(
            "SELECT DISTINCT p.code FROM user_role ur JOIN role_permission rp ON rp.role_id=ur.role_id "
                + "JOIN permission p ON p.id=rp.permission_id WHERE ur.user_id=?")) {
          ps.setLong(1, session.userId);
          try (ResultSet rs = ps.executeQuery()) { while (rs.next()) permissions.add(Permission.valueOf(rs.getString(1))); }
        }
        permissions.remove(Permission.SYSTEM_MANAGE);
        permissions.remove(Permission.DISCOUNT_APPLY);
      }
      session.setPermissions(permissions);
      sessions.add(session);
    } catch (SQLException e) { throw new IllegalStateException("权限校验失败", e); }
  }
  public static boolean allowed(UserSession session, Permission permission) {
    if (session == null) return false;
    refresh(session); session.touch(); return session.can(permission);
  }
  public static void require(UserSession session, Permission permission) {
    if (!allowed(session, permission)) throw new SecurityException("当前账号无“" + permission.getLabel() + "”权限");
  }
  public static void requireAny(UserSession session, Permission... permissions) {
    refresh(session); session.touch();
    for (Permission permission : permissions) if (session.can(permission)) return;
    throw new SecurityException("当前账号无此操作权限");
  }
}
