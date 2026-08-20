package com.healtouch.service;

import com.healtouch.dao.AuditLogDao;
import com.healtouch.dao.Jdbc;
import com.healtouch.model.*;
import com.healtouch.util.Checks;
import com.healtouch.util.Codes;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;
import javax.sql.DataSource;

public class BillingService {
  private final DataSource dataSource;
  private final AuditLogDao audit = new AuditLogDao();

  public BillingService(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  public long createPendingBill(
      UserSession actor,
      long patientId,
      LocalDate treatmentDate,
      long therapistId,
      String note,
      List<TreatmentLine> requestedLines) {
    Authorization.require(actor, Permission.TREATMENT_CREATE);
    if (treatmentDate == null || requestedLines == null || requestedLines.isEmpty())
      throw new IllegalArgumentException("治疗日期和至少一个治疗项目不能为空");
    return Jdbc.inTransaction(
        dataSource,
        c -> {
          requirePatient(c, patientId);
          String therapistName = requireActiveTherapist(c, therapistId);
          List<TreatmentLine> lines = canonicalLines(c, requestedLines);
          long gross = sumLines(lines);
          long id;
          try (PreparedStatement ps =
              c.prepareStatement(
                  "INSERT INTO"
                      + " bill(bill_code,patient_id,treatment_date,therapist_id,therapist_name_snapshot,note,status,gross_cents,receivable_cents,created_by)"
                      + " VALUES(?,?,?,?,?,?,?,?,?,?)",
                  Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, Codes.next("B"));
            ps.setLong(2, patientId);
            ps.setString(3, treatmentDate.toString());
            ps.setLong(4, therapistId);
            ps.setString(5, therapistName);
            ps.setString(6, note);
            ps.setString(7, BillStatus.PENDING_PAYMENT.name());
            ps.setLong(8, gross);
            ps.setLong(9, gross);
            ps.setLong(10, actor.userId);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
              if (!keys.next()) throw new SQLException("未生成账单");
              id = keys.getLong(1);
            }
          }
          try (PreparedStatement ps =
              c.prepareStatement("UPDATE bill SET bill_code=? WHERE id=?")) {
            ps.setString(1, Codes.sequential("B", id));
            ps.setLong(2, id);
            ps.executeUpdate();
          }
          insertItems(c, id, lines);
          audit.record(
              c,
              actor.userId,
              "BILL_DRAFT_CREATED",
              "BILL",
              String.valueOf(id),
              null,
              "gross=" + gross);
          return id;
        });
  }

  public void applyDiscount(UserSession actor, long billId, long discountCents, String reason) {
    Authorization.require(actor, Permission.DISCOUNT_APPLY);
    Checks.required(reason, "折扣原因");
    Jdbc.inTransaction(
        dataSource,
        c -> {
          BillAmounts bill = loadAmounts(c, billId);
          checkPending(bill);
          if (discountCents < 0 || discountCents > bill.gross)
            throw new IllegalArgumentException("折扣金额无效");
          try (PreparedStatement ps =
              c.prepareStatement(
                  "UPDATE bill SET discount_cents=?, discount_reason=?, discount_authorizer_id=?,"
                      + " discount_authorized_at=CURRENT_TIMESTAMP, receivable_cents=gross_cents-?"
                      + " WHERE id=?")) {
            ps.setLong(1, discountCents);
            ps.setString(2, reason);
            ps.setLong(3, actor.userId);
            ps.setLong(4, discountCents);
            ps.setLong(5, billId);
            ps.executeUpdate();
          }
          audit.record(
              c,
              actor.userId,
              "BILL_DISCOUNT_APPLIED",
              "BILL",
              String.valueOf(billId),
              String.valueOf(bill.discount),
              String.valueOf(discountCents));
          return null;
        });
  }

  public void checkout(UserSession actor, long billId, List<PaymentLine> payments) {
    Authorization.require(actor, Permission.BILL_CHARGE);
    if (payments == null || payments.isEmpty()) throw new IllegalArgumentException("至少输入一种支付方式");
    Jdbc.inTransaction(
        dataSource,
        c -> {
          BillAmounts bill = loadAmounts(c, billId);
          checkPending(bill);
          long total = sumPayments(payments);
          if (total != bill.receivable)
            throw new IllegalArgumentException(
                "实收总额必须等于应收总额（应收：" + com.healtouch.util.Money.format(bill.receivable) + "）");
          long deposit = amountFor(payments, PaymentMethod.DEPOSIT);
          if (deposit > 0) deductDeposit(c, bill.patientId, deposit, billId, actor.userId);
          insertPayments(c, billId, payments);
          try (PreparedStatement ps =
              c.prepareStatement(
                  "UPDATE bill SET status='PAID',paid_cents=?,paid_at=CURRENT_TIMESTAMP WHERE id=?"
                      + " AND status='PENDING_PAYMENT'")) {
            ps.setLong(1, total);
            ps.setLong(2, billId);
            if (ps.executeUpdate() != 1) throw new IllegalStateException("账单状态已变更，请刷新后重试");
          }
          try (PreparedStatement ps =
              c.prepareStatement(
                  "INSERT INTO"
                      + " treatment_record(bill_id,patient_id,therapist_id,therapist_name_snapshot,treatment_date,note,status)"
                      + " VALUES(?,?,?,?,?,?,'COMPLETED')")) {
            ps.setLong(1, billId);
            ps.setLong(2, bill.patientId);
            ps.setLong(3, bill.therapistId);
            ps.setString(4, bill.therapistName);
            ps.setString(5, bill.treatmentDate);
            ps.setString(6, bill.note);
            ps.executeUpdate();
          }
          audit.record(
              c, actor.userId, "BILL_PAID", "BILL", String.valueOf(billId), null, "paid=" + total);
          return null;
        });
  }

  public void voidPending(UserSession actor, long billId, String reason) {
    Authorization.require(actor, Permission.TREATMENT_CREATE);
    Checks.required(reason, "作废原因");
    Jdbc.inTransaction(
        dataSource,
        c -> {
          try (PreparedStatement ps =
              c.prepareStatement(
                  "UPDATE bill SET status='VOIDED',voided_at=CURRENT_TIMESTAMP,void_reason=? WHERE"
                      + " id=? AND status='PENDING_PAYMENT'")) {
            ps.setString(1, reason);
            ps.setLong(2, billId);
            if (ps.executeUpdate() != 1) throw new IllegalStateException("只有待收费账单可作废");
          }
          audit.record(
              c, actor.userId, "BILL_VOIDED", "BILL", String.valueOf(billId), null, reason);
          return null;
        });
  }

  public int voidExpiredPending() {
    return Jdbc.inTransaction(
        dataSource,
        c -> {
          try (PreparedStatement ps =
              c.prepareStatement(
                  "UPDATE bill SET"
                      + " status='VOIDED',voided_at=CURRENT_TIMESTAMP,void_reason='收费会话超过30分钟未完成'"
                      + " WHERE status='PENDING_PAYMENT' AND created_at < datetime('now','-30"
                      + " minutes')")) {
            return ps.executeUpdate();
          }
        });
  }

  public List<BillSummary> list(String keyword, BillStatus status) {
    List<BillSummary> list = new ArrayList<BillSummary>();
    String sql =
        "SELECT b.*,p.name patient_name FROM bill b JOIN patient p ON p.id=b.patient_id WHERE (?=''"
            + " OR p.name LIKE ? OR p.id_number LIKE ? OR b.bill_code LIKE ?)"
            + (status == null ? "" : " AND b.status=?")
            + " ORDER BY b.created_at DESC LIMIT 200";
    try (Connection c = dataSource.getConnection();
        PreparedStatement ps = c.prepareStatement(sql)) {
      String word = keyword == null ? "" : keyword.trim();
      ps.setString(1, word);
      ps.setString(2, "%" + word + "%");
      ps.setString(3, "%" + word + "%");
      ps.setString(4, "%" + word + "%");
      if (status != null) ps.setString(5, status.name());
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          BillSummary b = new BillSummary();
          b.id = rs.getLong("id");
          b.billCode = rs.getString("bill_code");
          b.patientId = rs.getLong("patient_id");
          b.patientName = rs.getString("patient_name");
          b.treatmentDate = LocalDate.parse(rs.getString("treatment_date"));
          b.therapistName = rs.getString("therapist_name_snapshot");
          b.status = BillStatus.valueOf(rs.getString("status"));
          b.receivableCents = rs.getLong("receivable_cents");
          b.paidCents = rs.getLong("paid_cents");
          b.refundedCents = rs.getLong("refunded_cents");
          list.add(b);
        }
      }
      return list;
    } catch (SQLException e) {
      throw new IllegalStateException("查询账单失败", e);
    }
  }

  private List<TreatmentLine> canonicalLines(Connection c, List<TreatmentLine> requested)
      throws SQLException {
    List<TreatmentLine> lines = new ArrayList<TreatmentLine>();
    try (PreparedStatement ps =
        c.prepareStatement(
            "SELECT id,name,price_cents FROM treatment_project WHERE id=? AND active=1")) {
      for (TreatmentLine line : requested) {
        ps.setLong(1, line.projectId);
        try (ResultSet rs = ps.executeQuery()) {
          if (!rs.next()) throw new IllegalArgumentException("治疗项目不存在或已停用");
          lines.add(
              new TreatmentLine(
                  line.projectId, rs.getString("name"), rs.getLong("price_cents"), line.quantity));
        }
      }
    }
    return lines;
  }

  private void insertItems(Connection c, long billId, List<TreatmentLine> lines)
      throws SQLException {
    try (PreparedStatement ps =
        c.prepareStatement(
            "INSERT INTO"
                + " bill_item(bill_id,project_id,project_name_snapshot,unit_price_cents,quantity,subtotal_cents)"
                + " VALUES(?,?,?,?,?,?)")) {
      for (TreatmentLine x : lines) {
        ps.setLong(1, billId);
        ps.setLong(2, x.projectId);
        ps.setString(3, x.projectName);
        ps.setLong(4, x.unitPriceCents);
        ps.setInt(5, x.quantity);
        ps.setLong(6, x.subtotalCents());
        ps.addBatch();
      }
      ps.executeBatch();
    }
  }

  private void insertPayments(Connection c, long billId, List<PaymentLine> payments)
      throws SQLException {
    try (PreparedStatement ps =
        c.prepareStatement("INSERT INTO payment(bill_id,method,amount_cents) VALUES(?,?,?)")) {
      for (PaymentLine x : payments) {
        ps.setLong(1, billId);
        ps.setString(2, x.method.name());
        ps.setLong(3, x.amountCents);
        ps.addBatch();
      }
      ps.executeBatch();
    }
  }

  private void deductDeposit(Connection c, long patientId, long amount, long billId, long actorId)
      throws SQLException {
    try (PreparedStatement ps =
        c.prepareStatement(
            "UPDATE deposit_account SET balance_cents=balance_cents-?,updated_at=CURRENT_TIMESTAMP"
                + " WHERE patient_id=? AND balance_cents>=?")) {
      ps.setLong(1, amount);
      ps.setLong(2, patientId);
      ps.setLong(3, amount);
      if (ps.executeUpdate() != 1) throw new IllegalArgumentException("预存余额不足");
    }
    long balance = balance(c, patientId);
    try (PreparedStatement ps =
        c.prepareStatement(
            "INSERT INTO"
                + " deposit_transaction(transaction_code,patient_id,transaction_type,amount_cents,balance_after_cents,payment_method,bill_id,operator_id)"
                + " VALUES(?,?,'CONSUMPTION',?,?,?,?,?)")) {
      ps.setString(1, Codes.next("DT"));
      ps.setLong(2, patientId);
      ps.setLong(3, -amount);
      ps.setLong(4, balance);
      ps.setString(5, PaymentMethod.DEPOSIT.name());
      ps.setLong(6, billId);
      ps.setLong(7, actorId);
      ps.executeUpdate();
    }
  }

  private long balance(Connection c, long patientId) throws SQLException {
    try (PreparedStatement ps =
        c.prepareStatement("SELECT balance_cents FROM deposit_account WHERE patient_id=?")) {
      ps.setLong(1, patientId);
      try (ResultSet rs = ps.executeQuery()) {
        if (!rs.next()) throw new IllegalArgumentException("患者预存账户不存在");
        return rs.getLong(1);
      }
    }
  }

  private void requirePatient(Connection c, long id) throws SQLException {
    try (PreparedStatement ps = c.prepareStatement("SELECT id FROM patient WHERE id=?")) {
      ps.setLong(1, id);
      try (ResultSet rs = ps.executeQuery()) {
        if (!rs.next()) throw new IllegalArgumentException("患者不存在");
      }
    }
  }

  private String requireActiveTherapist(Connection c, long id) throws SQLException {
    String sql =
        "SELECT u.name FROM app_user u JOIN user_role ur ON ur.user_id=u.id JOIN role r ON"
            + " r.id=ur.role_id WHERE u.id=? AND u.active=1 AND r.code='THERAPIST'";
    try (PreparedStatement ps = c.prepareStatement(sql)) {
      ps.setLong(1, id);
      try (ResultSet rs = ps.executeQuery()) {
        if (!rs.next()) throw new IllegalArgumentException("治疗师不存在、未在职或未配置治疗师角色");
        return rs.getString(1);
      }
    }
  }

  private BillAmounts loadAmounts(Connection c, long billId) throws SQLException {
    try (PreparedStatement ps = c.prepareStatement("SELECT * FROM bill WHERE id=?")) {
      ps.setLong(1, billId);
      try (ResultSet rs = ps.executeQuery()) {
        if (!rs.next()) throw new IllegalArgumentException("账单不存在");
        BillAmounts b = new BillAmounts();
        b.id = billId;
        b.patientId = rs.getLong("patient_id");
        b.therapistId = rs.getLong("therapist_id");
        b.therapistName = rs.getString("therapist_name_snapshot");
        b.treatmentDate = rs.getString("treatment_date");
        b.note = rs.getString("note");
        b.status = BillStatus.valueOf(rs.getString("status"));
        b.gross = rs.getLong("gross_cents");
        b.discount = rs.getLong("discount_cents");
        b.receivable = rs.getLong("receivable_cents");
        return b;
      }
    }
  }

  private void checkPending(BillAmounts bill) {
    if (bill.status != BillStatus.PENDING_PAYMENT) throw new IllegalStateException("仅待收费账单可进行此操作");
  }

  private long sumLines(List<TreatmentLine> items) {
    long total = 0;
    for (TreatmentLine i : items) total = Math.addExact(total, i.subtotalCents());
    return total;
  }

  private long sumPayments(List<PaymentLine> items) {
    long total = 0;
    for (PaymentLine i : items) total = Math.addExact(total, i.amountCents);
    return total;
  }

  private long amountFor(List<PaymentLine> items, PaymentMethod method) {
    long total = 0;
    for (PaymentLine i : items) if (i.method == method) total += i.amountCents;
    return total;
  }

  private static class BillAmounts {
    long id, patientId, therapistId, gross, discount, receivable;
    String therapistName, treatmentDate, note;
    BillStatus status;
  }
}
