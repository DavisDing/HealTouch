package com.healtouch.service;

import com.healtouch.dao.AuditLogDao;
import com.healtouch.dao.Jdbc;
import com.healtouch.model.RoleCode;
import com.healtouch.model.UserSession;
import com.healtouch.util.Checks;
import com.healtouch.util.Codes;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.EnumSet;
import javax.sql.DataSource;
import org.mindrot.jbcrypt.BCrypt;

public class AuthService {
  private final DataSource dataSource;
  private final AuditLogDao audit = new AuditLogDao();

  public AuthService(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  /** Returns true only while the local database has no user accounts. */
  public boolean requiresInitialAdministratorSetup() {
    try (Connection connection = dataSource.getConnection()) {
      return !hasUsers(connection);
    } catch (SQLException exception) {
      throw new IllegalStateException("无法检查初始管理员状态", exception);
    }
  }

  /**
   * Creates the only account allowed on a new installation. The password is chosen locally and is
   * never embedded in the application or displayed in the normal login screen.
   */
  public void initializeAdministrator(String password) {
    validatePassword(password);
    Jdbc.inTransaction(
        dataSource,
        connection -> {
          if (hasUsers(connection)) {
            throw new IllegalStateException("初始管理员已创建，请使用已有账号登录");
          }
          long userId = createAdministrator(connection, password);
          audit.record(
              connection,
              userId,
              "INITIAL_ADMIN_CREATED",
              "USER",
              String.valueOf(userId),
              null,
              "admin");
          return null;
        });
  }

  public UserSession login(String loginName, String password) {
    LoginResult result =
        Jdbc.inTransaction(
            dataSource,
            connection -> {
              String name = Checks.required(loginName, "登录账号");
              Checks.required(password, "密码");
              try (PreparedStatement statement =
                  connection.prepareStatement("SELECT * FROM app_user WHERE login_name=?")) {
                statement.setString(1, name);
                try (ResultSet resultSet = statement.executeQuery()) {
                  if (!resultSet.next()) {
                    return LoginResult.failure("账号或密码错误");
                  }
                  long userId = resultSet.getLong("id");
                  if (!resultSet.getBoolean("active")) {
                    return LoginResult.failure("账号或密码错误");
                  }
                  String lockedUntil = resultSet.getString("locked_until");
                  if (lockedUntil != null
                      && LocalDateTime.parse(lockedUntil).isAfter(LocalDateTime.now())) {
                    return LoginResult.failure("账号已锁定，请 15 分钟后重试");
                  }
                  if (!BCrypt.checkpw(password, resultSet.getString("password_hash"))) {
                    int failures = resultSet.getInt("failed_login_count") + 1;
                    recordFailedLogin(connection, userId, failures);
                    return LoginResult.failure(
                        failures >= 5 ? "连续 5 次登录失败，账号已锁定 15 分钟" : "账号或密码错误");
                  }
                  resetLoginFailures(connection, userId);
                  EnumSet<RoleCode> roles = loadRoles(connection, userId);
                  audit.record(
                      connection,
                      userId,
                      "LOGIN_SUCCESS",
                      "USER",
                      String.valueOf(userId),
                      null,
                      null);
                  return LoginResult.success(
                      new UserSession(
                          userId,
                          resultSet.getString("user_code"),
                          resultSet.getString("name"),
                          roles,
                          resultSet.getBoolean("must_change_password")));
                }
              }
            });
    if (result.failureMessage != null) {
      throw new SecurityException(result.failureMessage);
    }
    return result.session;
  }

  public void changePassword(UserSession session, String oldPassword, String newPassword) {
    if (session == null) {
      throw new SecurityException("请先登录");
    }
    validatePassword(newPassword);
    Jdbc.inTransaction(
        dataSource,
        connection -> {
          try (PreparedStatement statement =
              connection.prepareStatement("SELECT password_hash FROM app_user WHERE id=?")) {
            statement.setLong(1, session.userId);
            try (ResultSet resultSet = statement.executeQuery()) {
              if (!resultSet.next() || !BCrypt.checkpw(oldPassword, resultSet.getString(1))) {
                throw new SecurityException("原密码错误");
              }
            }
          }
          try (PreparedStatement statement =
              connection.prepareStatement(
                  "UPDATE app_user SET password_hash=?, must_change_password=0 WHERE id=?")) {
            statement.setString(1, BCrypt.hashpw(newPassword, BCrypt.gensalt(12)));
            statement.setLong(2, session.userId);
            statement.executeUpdate();
          }
          audit.record(
              connection,
              session.userId,
              "PASSWORD_CHANGED",
              "USER",
              String.valueOf(session.userId),
              null,
              null);
          return null;
        });
  }

  public static void validatePassword(String password) {
    Checks.required(password, "密码");
    if (password.length() < 8
        || !password.matches(".*[A-Za-z].*")
        || !password.matches(".*[0-9].*")
        || !password.matches(".*[^A-Za-z0-9].*")) {
      throw new IllegalArgumentException("密码至少 8 位，且包含字母、数字和特殊字符");
    }
  }

  private long createAdministrator(Connection connection, String password) throws SQLException {
    long userId;
    try (PreparedStatement statement =
        connection.prepareStatement(
            "INSERT INTO app_user(user_code,name,login_name,password_hash,must_change_password)"
                + " VALUES(?,?,?,?,0)",
            Statement.RETURN_GENERATED_KEYS)) {
      statement.setString(1, Codes.next("U"));
      statement.setString(2, "系统管理员");
      statement.setString(3, "admin");
      statement.setString(4, BCrypt.hashpw(password, BCrypt.gensalt(12)));
      statement.executeUpdate();
      try (ResultSet keys = statement.getGeneratedKeys()) {
        if (!keys.next()) {
          throw new SQLException("未生成用户主键");
        }
        userId = keys.getLong(1);
      }
    }
    try (PreparedStatement statement =
        connection.prepareStatement(
            "INSERT INTO user_role(user_id, role_id) SELECT ?, id FROM role WHERE code='ADMIN'")) {
      statement.setLong(1, userId);
      if (statement.executeUpdate() != 1) {
        throw new SQLException("未找到管理员角色");
      }
    }
    return userId;
  }

  private boolean hasUsers(Connection connection) throws SQLException {
    try (Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery("SELECT EXISTS(SELECT 1 FROM app_user)")) {
      if (!resultSet.next()) {
        throw new SQLException("无法检查用户数量");
      }
      return resultSet.getBoolean(1);
    }
  }

  private void recordFailedLogin(Connection connection, long userId, int failures)
      throws SQLException {
    String lockedUntil = failures >= 5 ? LocalDateTime.now().plusMinutes(15).toString() : null;
    try (PreparedStatement statement =
        connection.prepareStatement(
            "UPDATE app_user SET failed_login_count=?, locked_until=? WHERE id=?")) {
      statement.setInt(1, failures);
      statement.setString(2, lockedUntil);
      statement.setLong(3, userId);
      statement.executeUpdate();
    }
    audit.record(connection, userId, "LOGIN_FAILED", "USER", String.valueOf(userId), null, null);
  }

  private void resetLoginFailures(Connection connection, long userId) throws SQLException {
    try (PreparedStatement statement =
        connection.prepareStatement(
            "UPDATE app_user SET failed_login_count=0, locked_until=NULL,"
                + " last_login_at=CURRENT_TIMESTAMP WHERE id=?")) {
      statement.setLong(1, userId);
      statement.executeUpdate();
    }
  }

  private static final class LoginResult {
    private final UserSession session;
    private final String failureMessage;

    private LoginResult(UserSession session, String failureMessage) {
      this.session = session;
      this.failureMessage = failureMessage;
    }

    private static LoginResult success(UserSession session) {
      return new LoginResult(session, null);
    }

    private static LoginResult failure(String message) {
      return new LoginResult(null, message);
    }
  }

  private EnumSet<RoleCode> loadRoles(Connection connection, long userId) throws SQLException {
    EnumSet<RoleCode> roles = EnumSet.noneOf(RoleCode.class);
    try (PreparedStatement statement =
        connection.prepareStatement(
            "SELECT r.code FROM role r JOIN user_role ur ON ur.role_id=r.id WHERE ur.user_id=?")) {
      statement.setLong(1, userId);
      try (ResultSet resultSet = statement.executeQuery()) {
        while (resultSet.next()) {
          roles.add(RoleCode.valueOf(resultSet.getString(1)));
        }
      }
    }
    return roles;
  }
}
