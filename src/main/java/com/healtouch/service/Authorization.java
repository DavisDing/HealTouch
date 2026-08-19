package com.healtouch.service;

import com.healtouch.model.Permission;
import com.healtouch.model.RoleCode;
import com.healtouch.model.UserSession;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.sql.DataSource;

/** 每个业务入口都会调用本类。已配置数据库时从角色权限表实时判断，停用/角色变更即时生效。 */
public final class Authorization {
  private static volatile DataSource dataSource;

  private Authorization() {}

  public static void configure(DataSource source) {
    dataSource = source;
  }

  public static boolean allowed(UserSession session, Permission permission) {
    if (session == null) return false;
    if (session.isAdmin()) return true; // 内置管理员不可配置，始终拥有全部权限
    DataSource source = dataSource;
    if (source != null) {
      String sql =
          "SELECT 1 FROM app_user u JOIN user_role ur ON ur.user_id=u.id JOIN role_permission rp ON"
              + " rp.role_id=ur.role_id JOIN permission p ON p.id=rp.permission_id WHERE u.id=? AND"
              + " u.active=1 AND p.code=? LIMIT 1";
      try (Connection c = source.getConnection();
          PreparedStatement ps = c.prepareStatement(sql)) {
        ps.setLong(1, session.userId);
        ps.setString(2, permission.name());
        try (ResultSet rs = ps.executeQuery()) {
          return rs.next();
        }
      } catch (Exception e) {
        throw new IllegalStateException("权限校验失败", e);
      }
    }
    // 无数据库的纯单元测试回退到系统默认矩阵。
    switch (permission) {
      case PATIENT_VIEW:
        return session.roles.contains(RoleCode.RECEPTION)
            || session.roles.contains(RoleCode.THERAPIST)
            || session.roles.contains(RoleCode.FINANCE);
      case PATIENT_EDIT:
        return session.roles.contains(RoleCode.RECEPTION);
      case TREATMENT_CREATE:
        return session.roles.contains(RoleCode.RECEPTION)
            || session.roles.contains(RoleCode.THERAPIST);
      case BILL_CHARGE:
      case DEPOSIT_RECHARGE:
        return session.roles.contains(RoleCode.RECEPTION);
      case DEPOSIT_REFUND:
      case BILL_REFUND:
      case REPORT_VIEW:
        return session.roles.contains(RoleCode.FINANCE);
      default:
        return false;
    }
  }

  public static void require(UserSession session, Permission permission) {
    if (!allowed(session, permission))
      throw new SecurityException(session == null ? "请先登录" : "当前账号无“" + permission.name() + "”权限");
  }
}
