package com.healtouch.service;

import com.healtouch.dao.AuditLogDao;
import com.healtouch.dao.Jdbc;
import com.healtouch.dao.PatientDao;
import com.healtouch.model.PageResult;
import com.healtouch.model.Patient;
import com.healtouch.model.Permission;
import com.healtouch.model.UserSession;
import com.healtouch.util.Checks;
import com.healtouch.util.Codes;
import java.sql.*;
import java.util.List;
import javax.sql.DataSource;

public class PatientService {
  private final DataSource dataSource;
  private final PatientDao patientDao;
  private final AuditLogDao audit = new AuditLogDao();

  public PatientService(DataSource dataSource) {
    this.dataSource = dataSource;
    this.patientDao = new PatientDao(dataSource);
  }

  public Patient get(UserSession actor, long id) {
    Authorization.require(actor, Permission.PATIENT_VIEW);
    Patient p = patientDao.find(id);
    if (p == null) throw new IllegalArgumentException("患者不存在");
    return p;
  }

  public List<Patient> search(UserSession actor, String keyword) {
    Authorization.require(actor, Permission.PATIENT_VIEW);
    return patientDao.search(keyword);
  }

  public PageResult<Patient> searchPage(UserSession actor, String keyword, int page, int pageSize) {
    Authorization.require(actor, Permission.PATIENT_VIEW);
    if (page < 1 || pageSize < 1) throw new IllegalArgumentException("分页参数不正确");
    long total = patientDao.countSearch(keyword);
    int totalPages = (int) Math.max(1L, (total + pageSize - 1L) / pageSize);
    int actualPage = Math.min(page, totalPages);
    return new PageResult<Patient>(
        patientDao.search(keyword, actualPage, pageSize), total, actualPage, pageSize);
  }

  public long create(UserSession actor, Patient p) {
    Authorization.require(actor, Permission.PATIENT_EDIT);
    validate(p);
    return Jdbc.inTransaction(
        dataSource,
        c -> {
          try (PreparedStatement duplicate =
              c.prepareStatement("SELECT id FROM patient WHERE id_type=? AND id_number=?")) {
            duplicate.setString(1, p.idType.trim());
            duplicate.setString(2, p.idNumber.trim());
            try (ResultSet rs = duplicate.executeQuery()) {
              if (rs.next()) throw new IllegalArgumentException("该证件已有档案，患者ID：" + rs.getLong(1));
            }
          }
          long id;
          try (PreparedStatement ps =
              c.prepareStatement(
                  "INSERT INTO"
                      + " patient(patient_code,patient_type,name,gender,id_type,id_number,birth_date,phone,address,guardian_name,guardian_relationship,guardian_phone,allergies,medical_history,remark,created_by)"
                      + " VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                  Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, Codes.next("P"));
            ps.setString(2, p.patientType.name());
            ps.setString(3, p.name.trim());
            ps.setString(4, p.gender.name());
            ps.setString(5, p.idType.trim());
            ps.setString(6, p.idNumber.trim());
            ps.setString(7, p.birthDate.toString());
            ps.setString(8, p.phone.trim());
            ps.setString(9, p.address);
            ps.setString(10, p.guardianName);
            ps.setString(11, p.guardianRelationship);
            ps.setString(12, p.guardianPhone);
            ps.setString(13, p.allergies);
            ps.setString(14, p.medicalHistory);
            ps.setString(15, p.remark);
            ps.setLong(16, actor.userId);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
              if (!rs.next()) throw new SQLException("未生成患者ID");
              id = rs.getLong(1);
            }
          }
          try (PreparedStatement ps =
              c.prepareStatement("UPDATE patient SET patient_code=? WHERE id=?")) {
            ps.setString(1, Codes.sequential("P", id));
            ps.setLong(2, id);
            ps.executeUpdate();
          }
          try (PreparedStatement ps =
              c.prepareStatement(
                  "INSERT INTO deposit_account(patient_id,balance_cents) VALUES(?,0)")) {
            ps.setLong(1, id);
            ps.executeUpdate();
          }
          audit.record(
              c, actor.userId, "PATIENT_CREATED", "PATIENT", String.valueOf(id), null, p.name);
          return id;
        });
  }

  public void update(UserSession actor, Patient p) {
    Authorization.require(actor, Permission.PATIENT_EDIT);
    if (p == null || p.id == null) throw new IllegalArgumentException("患者ID不能为空");
    validate(p);
    Jdbc.inTransaction(
        dataSource,
        c -> {
          Patient previous = load(c, p.id);
          if (previous == null) throw new IllegalArgumentException("患者不存在");
          try (PreparedStatement duplicate =
              c.prepareStatement(
                  "SELECT id FROM patient WHERE id_type=? AND id_number=? AND id<>?")) {
            duplicate.setString(1, p.idType.trim());
            duplicate.setString(2, p.idNumber.trim());
            duplicate.setLong(3, p.id);
            try (ResultSet rs = duplicate.executeQuery()) {
              if (rs.next()) throw new IllegalArgumentException("该证件号码已被其他患者使用");
            }
          }
          try (PreparedStatement ps =
              c.prepareStatement(
                  "UPDATE patient SET"
                      + " patient_type=?,name=?,gender=?,id_type=?,id_number=?,birth_date=?,phone=?,address=?,guardian_name=?,guardian_relationship=?,guardian_phone=?,allergies=?,medical_history=?,remark=?,updated_at=CURRENT_TIMESTAMP"
                      + " WHERE id=?")) {
            ps.setString(1, p.patientType.name());
            ps.setString(2, p.name.trim());
            ps.setString(3, p.gender.name());
            ps.setString(4, p.idType.trim());
            ps.setString(5, p.idNumber.trim());
            ps.setString(6, p.birthDate.toString());
            ps.setString(7, p.phone.trim());
            ps.setString(8, p.address);
            ps.setString(9, p.guardianName);
            ps.setString(10, p.guardianRelationship);
            ps.setString(11, p.guardianPhone);
            ps.setString(12, p.allergies);
            ps.setString(13, p.medicalHistory);
            ps.setString(14, p.remark);
            ps.setLong(15, p.id);
            ps.executeUpdate();
          }
          audit.record(
              c,
              actor.userId,
              "PATIENT_SENSITIVE_UPDATED",
              "PATIENT",
              String.valueOf(p.id),
              previous.idType + ":" + previous.idNumber + "|" + previous.name,
              p.idType + ":" + p.idNumber + "|" + p.name);
          return null;
        });
  }

  private void validate(Patient p) {
    if (p == null) throw new IllegalArgumentException("患者信息不能为空");
    Checks.required(p.name, "姓名");
    Checks.required(p.idType, "证件类型");
    Checks.required(p.idNumber, "证件号码");
    Checks.required(p.phone, "联系电话");
    if (p.patientType == null || p.gender == null || p.birthDate == null)
      throw new IllegalArgumentException("患者类型、性别和出生日期不能为空");
    if (p.isChild()) {
      Checks.required(p.guardianName, "监护人姓名");
      Checks.required(p.guardianRelationship, "监护人关系");
      Checks.required(p.guardianPhone, "监护人电话");
    }
  }

  private Patient load(Connection c, long id) throws SQLException {
    try (PreparedStatement ps =
        c.prepareStatement("SELECT p.*,0 balance_cents FROM patient p WHERE p.id=?")) {
      ps.setLong(1, id);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next() ? PatientDao.map(rs) : null;
      }
    }
  }
}
