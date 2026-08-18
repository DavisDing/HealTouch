package com.healtouch.service;

import com.healtouch.dao.AuditLogDao;
import com.healtouch.dao.Jdbc;
import com.healtouch.model.RoleCode;
import com.healtouch.model.UserSession;
import com.healtouch.util.Checks;
import com.healtouch.util.Codes;
import org.mindrot.jbcrypt.BCrypt;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.EnumSet;

public class AuthService {
    private final DataSource dataSource;
    private final AuditLogDao audit = new AuditLogDao();
    public AuthService(DataSource dataSource) { this.dataSource = dataSource; }

    public void ensureInitialAdministrator() {
        Jdbc.inTransaction(dataSource, c -> {
            try (Statement statement = c.createStatement(); ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM app_user")) {
                if (rs.next() && rs.getInt(1) > 0) return null;
            }
            long id;
            try (PreparedStatement ps = c.prepareStatement("INSERT INTO app_user(user_code,name,login_name,password_hash,must_change_password) VALUES(?,?,?,?,1)", Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, Codes.next("U")); ps.setString(2, "系统管理员"); ps.setString(3, "admin"); ps.setString(4, BCrypt.hashpw("Admin@123", BCrypt.gensalt(12))); ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) { if (!keys.next()) throw new SQLException("未生成用户主键"); id = keys.getLong(1); }
            }
            try (PreparedStatement ps = c.prepareStatement("INSERT INTO user_role(user_id, role_id) SELECT ?, id FROM role WHERE code='ADMIN'")) { ps.setLong(1, id); ps.executeUpdate(); }
            audit.record(c, id, "INITIAL_ADMIN_CREATED", "USER", String.valueOf(id), null, "admin");
            return null;
        });
    }

    public UserSession login(String loginName, String password) {
        return Jdbc.inTransaction(dataSource, c -> {
            String name = Checks.required(loginName, "登录账号"); Checks.required(password, "密码");
            try (PreparedStatement ps = c.prepareStatement("SELECT * FROM app_user WHERE login_name=?")) {
                ps.setString(1, name); try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) throw new SecurityException("账号或密码错误");
                    long id = rs.getLong("id");
                    if (!rs.getBoolean("active")) throw new SecurityException("账号已停用");
                    String locked = rs.getString("locked_until");
                    if (locked != null && LocalDateTime.parse(locked).isAfter(LocalDateTime.now())) throw new SecurityException("账号已锁定，请 15 分钟后重试");
                    if (!BCrypt.checkpw(password, rs.getString("password_hash"))) {
                        int failures = rs.getInt("failed_login_count") + 1;
                        String lock = failures >= 5 ? LocalDateTime.now().plusMinutes(15).toString() : null;
                        try (PreparedStatement update = c.prepareStatement("UPDATE app_user SET failed_login_count=?, locked_until=? WHERE id=?")) {
                            update.setInt(1, failures); update.setString(2, lock); update.setLong(3, id); update.executeUpdate();
                        }
                        audit.record(c, id, "LOGIN_FAILED", "USER", String.valueOf(id), null, null);
                        throw new SecurityException(failures >= 5 ? "连续 5 次登录失败，账号已锁定 15 分钟" : "账号或密码错误");
                    }
                    try (PreparedStatement update = c.prepareStatement("UPDATE app_user SET failed_login_count=0, locked_until=NULL, last_login_at=CURRENT_TIMESTAMP WHERE id=?")) { update.setLong(1, id); update.executeUpdate(); }
                    EnumSet<RoleCode> roles = loadRoles(c, id); audit.record(c, id, "LOGIN_SUCCESS", "USER", String.valueOf(id), null, null);
                    return new UserSession(id, rs.getString("user_code"), rs.getString("name"), roles, rs.getBoolean("must_change_password"));
                }
            }
        });
    }

    public void changePassword(UserSession session, String oldPassword, String newPassword) {
        if (session == null) throw new SecurityException("请先登录"); validatePassword(newPassword);
        Jdbc.inTransaction(dataSource, c -> {
            try (PreparedStatement ps = c.prepareStatement("SELECT password_hash FROM app_user WHERE id=?")) {
                ps.setLong(1, session.userId); try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next() || !BCrypt.checkpw(oldPassword, rs.getString(1))) throw new SecurityException("原密码错误");
                }
            }
            try (PreparedStatement ps = c.prepareStatement("UPDATE app_user SET password_hash=?, must_change_password=0 WHERE id=?")) { ps.setString(1, BCrypt.hashpw(newPassword, BCrypt.gensalt(12))); ps.setLong(2, session.userId); ps.executeUpdate(); }
            audit.record(c, session.userId, "PASSWORD_CHANGED", "USER", String.valueOf(session.userId), null, null); return null;
        });
    }

    public static void validatePassword(String password) {
        Checks.required(password, "密码");
        if (password.length() < 8 || !password.matches(".*[A-Za-z].*") || !password.matches(".*[0-9].*") || !password.matches(".*[^A-Za-z0-9].*"))
            throw new IllegalArgumentException("密码至少 8 位，且包含字母、数字和特殊字符");
    }
    private EnumSet<RoleCode> loadRoles(Connection c, long userId) throws SQLException {
        EnumSet<RoleCode> roles = EnumSet.noneOf(RoleCode.class);
        try (PreparedStatement ps = c.prepareStatement("SELECT r.code FROM role r JOIN user_role ur ON ur.role_id=r.id WHERE ur.user_id=?")) { ps.setLong(1, userId); try (ResultSet rs = ps.executeQuery()) { while (rs.next()) roles.add(RoleCode.valueOf(rs.getString(1))); } }
        return roles;
    }
}
