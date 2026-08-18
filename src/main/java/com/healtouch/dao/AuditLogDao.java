package com.healtouch.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class AuditLogDao {
    public void record(Connection connection, Long operatorId, String action, String targetType, String targetId,
                       String beforeValue, String afterValue) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO audit_log(operator_id, action, target_type, target_id, before_value, after_value) VALUES (?, ?, ?, ?, ?, ?)")) {
            if (operatorId == null) ps.setNull(1, java.sql.Types.INTEGER); else ps.setLong(1, operatorId);
            ps.setString(2, action); ps.setString(3, targetType); ps.setString(4, targetId);
            ps.setString(5, beforeValue); ps.setString(6, afterValue); ps.executeUpdate();
        }
    }
}
