package com.healtouch.service;

import com.healtouch.model.Permission;
import com.healtouch.model.UserSession;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.DataSource;

public class DashboardService {
  public static class Today {
    public long incomeCents;
    public long newPatients;
    public long paidBills;
  }

  private final DataSource dataSource;

  public DashboardService(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  public Today today(UserSession actor) {
    Authorization.require(actor, Permission.PATIENT_VIEW);
    try (Connection c = dataSource.getConnection()) {
      Today t = new Today();
      t.incomeCents =
          query(
              c,
              "SELECT COALESCE(SUM(CASE WHEN status IN ('PAID','PARTIALLY_REFUNDED','REFUNDED')"
                  + " THEN paid_cents-refunded_cents ELSE 0 END),0) FROM bill WHERE"
                  + " date(paid_at,'localtime')=date('now','localtime')");
      t.newPatients =
          query(
              c,
              "SELECT COUNT(*) FROM patient WHERE"
                  + " date(created_at,'localtime')=date('now','localtime')");
      t.paidBills =
          query(
              c,
              "SELECT COUNT(*) FROM bill WHERE status IN ('PAID','PARTIALLY_REFUNDED','REFUNDED')"
                  + " AND date(paid_at,'localtime')=date('now','localtime')");
      return t;
    } catch (SQLException e) {
      throw new IllegalStateException("读取工作台数据失败", e);
    }
  }

  private long query(Connection c, String sql) throws SQLException {
    try (PreparedStatement ps = c.prepareStatement(sql);
        ResultSet rs = ps.executeQuery()) {
      rs.next();
      return rs.getLong(1);
    }
  }
}
