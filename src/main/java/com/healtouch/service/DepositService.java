package com.healtouch.service;

import com.healtouch.dao.AuditLogDao;
import com.healtouch.dao.Jdbc;
import com.healtouch.model.*;
import com.healtouch.util.Codes;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;

public class DepositService {
  private final DataSource dataSource;
  private final AuditLogDao audit = new AuditLogDao();

  public DepositService(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  public long balance(UserSession actor, long patientId) {
    Authorization.require(actor, Permission.PATIENT_VIEW);
    try (Connection c = dataSource.getConnection()) {
      return balance(c, patientId);
    } catch (SQLException e) {
      throw new IllegalStateException("查询预存余额失败", e);
    }
  }

  public List<TransactionSummary> history(UserSession actor, long patientId) {
    Authorization.require(actor, Permission.PATIENT_VIEW);
    List<TransactionSummary> rows = new ArrayList<TransactionSummary>();
    String sql =
        "SELECT dt.*, b.bill_code FROM deposit_transaction dt "
            + "LEFT JOIN bill b ON b.id=dt.bill_id WHERE dt.patient_id=? "
            + "ORDER BY dt.created_at DESC, dt.id DESC LIMIT 200";
    try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
      ps.setLong(1, patientId);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          TransactionSummary row = new TransactionSummary();
          row.transactionCode = rs.getString("transaction_code");
          row.type = TransactionType.valueOf(rs.getString("transaction_type"));
          row.amountCents = rs.getLong("amount_cents");
          row.balanceAfterCents = rs.getLong("balance_after_cents");
          String method = rs.getString("payment_method");
          row.paymentMethod = method == null ? null : PaymentMethod.valueOf(method);
          row.billCode = rs.getString("bill_code");
          row.remark = rs.getString("remark");
          row.createdAt = rs.getString("created_at");
          rows.add(row);
        }
      }
      return rows;
    } catch (SQLException e) {
      throw new IllegalStateException("查询预存记录失败", e);
    }
  }

  public void recharge(
      UserSession actor, long patientId, long amountCents, PaymentMethod method, String remark) {
    Authorization.require(actor, Permission.DEPOSIT_RECHARGE);
    if (amountCents <= 0 || method == null || method == PaymentMethod.DEPOSIT)
      throw new IllegalArgumentException("充值金额或支付方式无效");
    Jdbc.inTransaction(
        dataSource,
        c -> {
          ensureAccount(c, patientId);
          try (PreparedStatement ps =
              c.prepareStatement(
                  "UPDATE deposit_account SET"
                      + " balance_cents=balance_cents+?,updated_at=CURRENT_TIMESTAMP WHERE"
                      + " patient_id=?")) {
            ps.setLong(1, amountCents);
            ps.setLong(2, patientId);
            ps.executeUpdate();
          }
          long after = balance(c, patientId);
          transaction(
              c,
              patientId,
              TransactionType.RECHARGE,
              amountCents,
              after,
              method,
              null,
              remark,
              actor.userId);
          audit.record(
              c,
              actor.userId,
              "DEPOSIT_RECHARGED",
              "PATIENT",
              String.valueOf(patientId),
              null,
              String.valueOf(amountCents));
          return null;
        });
  }

  public void refundBalance(
      UserSession actor, long patientId, long amountCents, PaymentMethod method, String remark) {
    Authorization.require(actor, Permission.DEPOSIT_REFUND);
    if (amountCents <= 0 || method == null) throw new IllegalArgumentException("退款金额或方式无效");
    Jdbc.inTransaction(
        dataSource,
        c -> {
          ensureAccount(c, patientId);
          try (PreparedStatement ps =
              c.prepareStatement(
                  "UPDATE deposit_account SET"
                      + " balance_cents=balance_cents-?,updated_at=CURRENT_TIMESTAMP WHERE"
                      + " patient_id=? AND balance_cents>=?")) {
            ps.setLong(1, amountCents);
            ps.setLong(2, patientId);
            ps.setLong(3, amountCents);
            if (ps.executeUpdate() != 1) throw new IllegalArgumentException("预存余额不足，无法退款");
          }
          long after = balance(c, patientId);
          transaction(
              c,
              patientId,
              TransactionType.REFUND,
              -amountCents,
              after,
              method,
              null,
              remark,
              actor.userId);
          audit.record(
              c,
              actor.userId,
              "DEPOSIT_BALANCE_REFUNDED",
              "PATIENT",
              String.valueOf(patientId),
              null,
              String.valueOf(amountCents));
          return null;
        });
  }

  public static class TransactionSummary {
    public String transactionCode;
    public TransactionType type;
    public long amountCents;
    public long balanceAfterCents;
    public PaymentMethod paymentMethod;
    public String billCode;
    public String remark;
    public String createdAt;
  }

  private void ensureAccount(Connection c, long patientId) throws SQLException {
    try (PreparedStatement p =
        c.prepareStatement(
            "INSERT OR IGNORE INTO deposit_account(patient_id,balance_cents) SELECT id,0 FROM"
                + " patient WHERE id=?")) {
      p.setLong(1, patientId);
      if (p.executeUpdate() == 0) {
        try (PreparedStatement exists =
            c.prepareStatement("SELECT 1 FROM deposit_account WHERE patient_id=?")) {
          exists.setLong(1, patientId);
          try (ResultSet rs = exists.executeQuery()) {
            if (!rs.next()) throw new IllegalArgumentException("患者不存在");
          }
        }
      }
    }
  }

  private long balance(Connection c, long patientId) throws SQLException {
    try (PreparedStatement ps =
        c.prepareStatement("SELECT balance_cents FROM deposit_account WHERE patient_id=?")) {
      ps.setLong(1, patientId);
      try (ResultSet rs = ps.executeQuery()) {
        if (!rs.next()) throw new IllegalArgumentException("患者不存在");
        return rs.getLong(1);
      }
    }
  }

  private void transaction(
      Connection c,
      long patientId,
      TransactionType type,
      long amount,
      long after,
      PaymentMethod method,
      Long bill,
      String remark,
      long operator)
      throws SQLException {
    try (PreparedStatement ps =
        c.prepareStatement(
            "INSERT INTO"
                + " deposit_transaction(transaction_code,patient_id,transaction_type,amount_cents,balance_after_cents,payment_method,bill_id,remark,operator_id)"
                + " VALUES(?,?,?,?,?,?,?,?,?)")) {
      ps.setString(1, Codes.next("DT"));
      ps.setLong(2, patientId);
      ps.setString(3, type.name());
      ps.setLong(4, amount);
      ps.setLong(5, after);
      ps.setString(6, method.name());
      if (bill == null) ps.setNull(7, Types.INTEGER);
      else ps.setLong(7, bill);
      ps.setString(8, remark);
      ps.setLong(9, operator);
      ps.executeUpdate();
    }
  }
}
