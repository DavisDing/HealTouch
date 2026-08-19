package com.healtouch.service;

import com.healtouch.dao.AuditLogDao;
import com.healtouch.dao.Jdbc;
import com.healtouch.model.Permission;
import com.healtouch.model.RoleCode;
import com.healtouch.model.UserSession;
import com.healtouch.util.Checks;
import com.healtouch.util.Codes;
import java.sql.*;
import java.util.*;
import javax.sql.DataSource;
import org.mindrot.jbcrypt.BCrypt;

public class UserService {
  public static class Staff {
    public long id;
    public String name;
    public String loginName;
    public String phone;
    public boolean active;
    public Set<RoleCode> roles;

    @Override
    public String toString() {
      return name;
    }
  }

  private final DataSource dataSource;
  private final AuditLogDao audit = new AuditLogDao();

  public UserService(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  public long create(
      UserSession actor,
      String name,
      String loginName,
      String phone,
      String password,
      Set<RoleCode> roles) {
    Authorization.require(actor, Permission.SYSTEM_MANAGE);
    AuthService.validatePassword(password);
    Checks.required(name, "姓名");
    Checks.required(loginName, "登录账号");
    if (roles == null || roles.isEmpty()) throw new IllegalArgumentException("至少分配一个角色");
    return Jdbc.inTransaction(
        dataSource,
        c -> {
          long id;
          try (PreparedStatement ps =
              c.prepareStatement(
                  "INSERT INTO"
                      + " app_user(user_code,name,login_name,password_hash,phone,must_change_password)"
                      + " VALUES(?,?,?,?,?,1)",
                  Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, Codes.next("U"));
            ps.setString(2, name.trim());
            ps.setString(3, loginName.trim());
            ps.setString(4, BCrypt.hashpw(password, BCrypt.gensalt(12)));
            ps.setString(5, phone);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
              if (!rs.next()) throw new SQLException("未生成用户");
              id = rs.getLong(1);
            }
          }
          assignRoles(c, id, roles);
          audit.record(
              c, actor.userId, "USER_CREATED", "USER", String.valueOf(id), null, loginName);
          return id;
        });
  }

  public List<Staff> activeTherapists() {
    return list("WHERE u.active=1 AND r.code='THERAPIST'");
  }

  public List<Staff> listAll() {
    return list("");
  }

  public void setActive(UserSession actor, long userId, boolean active) {
    Authorization.require(actor, Permission.SYSTEM_MANAGE);
    if (actor.userId == userId && !active) throw new IllegalArgumentException("不能停用当前登录账号");
    Jdbc.inTransaction(
        dataSource,
        c -> {
          try (PreparedStatement ps =
              c.prepareStatement("UPDATE app_user SET active=? WHERE id=?")) {
            ps.setBoolean(1, active);
            ps.setLong(2, userId);
            if (ps.executeUpdate() != 1) throw new IllegalArgumentException("用户不存在");
          }
          audit.record(
              c,
              actor.userId,
              "USER_STATUS_CHANGED",
              "USER",
              String.valueOf(userId),
              null,
              String.valueOf(active));
          return null;
        });
  }

  private List<Staff> list(String where) {
    Map<Long, Staff> result = new LinkedHashMap<Long, Staff>();
    String sql =
        "SELECT u.id,u.name,u.login_name,u.phone,u.active,r.code FROM app_user u LEFT JOIN"
            + " user_role ur ON ur.user_id=u.id LEFT JOIN role r ON r.id=ur.role_id "
            + where
            + " ORDER BY u.id";
    try (Connection c = dataSource.getConnection();
        PreparedStatement ps = c.prepareStatement(sql);
        ResultSet rs = ps.executeQuery()) {
      while (rs.next()) {
        long id = rs.getLong(1);
        Staff s = result.get(id);
        if (s == null) {
          s = new Staff();
          s.id = id;
          s.name = rs.getString(2);
          s.loginName = rs.getString(3);
          s.phone = rs.getString(4);
          s.active = rs.getBoolean(5);
          s.roles = EnumSet.noneOf(RoleCode.class);
          result.put(id, s);
        }
        String role = rs.getString(6);
        if (role != null) s.roles.add(RoleCode.valueOf(role));
      }
      return new ArrayList<Staff>(result.values());
    } catch (SQLException e) {
      throw new IllegalStateException("查询用户失败", e);
    }
  }

  private void assignRoles(Connection c, long userId, Set<RoleCode> roles) throws SQLException {
    try (PreparedStatement ps =
        c.prepareStatement(
            "INSERT INTO user_role(user_id,role_id) SELECT ?,id FROM role WHERE code=?")) {
      for (RoleCode role : roles) {
        ps.setLong(1, userId);
        ps.setString(2, role.name());
        ps.addBatch();
      }
      ps.executeBatch();
    }
  }
}
