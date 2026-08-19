package com.healtouch.dao;

import com.healtouch.model.Gender;
import com.healtouch.model.Patient;
import com.healtouch.model.PatientType;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;

public class PatientDao {
  private static final int MAX_PAGE_SIZE = 500;

  private final DataSource dataSource;

  public PatientDao(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  public Patient find(long id) {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(baseSql() + " WHERE p.id=?")) {
      statement.setLong(1, id);
      try (ResultSet resultSet = statement.executeQuery()) {
        return resultSet.next() ? map(resultSet) : null;
      }
    } catch (SQLException exception) {
      throw new IllegalStateException("查询患者失败", exception);
    }
  }

  public Patient findByDocument(String type, String number) {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement =
            connection.prepareStatement(baseSql() + " WHERE p.id_type=? AND p.id_number=?")) {
      statement.setString(1, type);
      statement.setString(2, number);
      try (ResultSet resultSet = statement.executeQuery()) {
        return resultSet.next() ? map(resultSet) : null;
      }
    } catch (SQLException exception) {
      throw new IllegalStateException("查询患者失败", exception);
    }
  }

  /**
   * Retained for callers that only need the first page. New UI code should use the paged overload.
   */
  public List<Patient> search(String keyword) {
    return search(keyword, 1, 100);
  }

  public List<Patient> search(String keyword, int page, int pageSize) {
    validatePage(page, pageSize);
    List<Patient> patients = new ArrayList<Patient>();
    String term = searchTerm(keyword);
    int offset = (page - 1) * pageSize;
    String sql =
        baseSql()
            + " WHERE p.name LIKE ? OR p.id_number LIKE ? OR p.phone LIKE ?"
            + " ORDER BY p.created_at DESC, p.id DESC LIMIT ? OFFSET ?";

    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      bindSearchTerm(statement, term);
      statement.setInt(4, pageSize);
      statement.setInt(5, offset);
      try (ResultSet resultSet = statement.executeQuery()) {
        while (resultSet.next()) {
          patients.add(map(resultSet));
        }
      }
      return patients;
    } catch (SQLException exception) {
      throw new IllegalStateException("查询患者失败", exception);
    }
  }

  public long countSearch(String keyword) {
    String sql =
        "SELECT COUNT(*) FROM patient p"
            + " WHERE p.name LIKE ? OR p.id_number LIKE ? OR p.phone LIKE ?";
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      bindSearchTerm(statement, searchTerm(keyword));
      try (ResultSet resultSet = statement.executeQuery()) {
        if (!resultSet.next()) {
          throw new SQLException("无法统计患者数量");
        }
        return resultSet.getLong(1);
      }
    } catch (SQLException exception) {
      throw new IllegalStateException("查询患者失败", exception);
    }
  }

  private String baseSql() {
    return "SELECT p.*, COALESCE(d.balance_cents, 0) balance_cents"
        + " FROM patient p LEFT JOIN deposit_account d ON d.patient_id=p.id";
  }

  private void bindSearchTerm(PreparedStatement statement, String term) throws SQLException {
    statement.setString(1, term);
    statement.setString(2, term);
    statement.setString(3, term);
  }

  private String searchTerm(String keyword) {
    return "%" + (keyword == null ? "" : keyword.trim()) + "%";
  }

  private void validatePage(int page, int pageSize) {
    if (page < 1 || pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
      throw new IllegalArgumentException("分页参数不正确");
    }
    if ((long) (page - 1) * pageSize > Integer.MAX_VALUE) {
      throw new IllegalArgumentException("页码超出范围");
    }
  }

  public static Patient map(ResultSet resultSet) throws SQLException {
    Patient patient = new Patient();
    patient.id = resultSet.getLong("id");
    patient.patientCode = resultSet.getString("patient_code");
    patient.patientType = PatientType.valueOf(resultSet.getString("patient_type"));
    patient.name = resultSet.getString("name");
    patient.gender = Gender.valueOf(resultSet.getString("gender"));
    patient.idType = resultSet.getString("id_type");
    patient.idNumber = resultSet.getString("id_number");
    patient.birthDate = requiredDate(resultSet, patient.id);
    patient.phone = resultSet.getString("phone");
    patient.address = resultSet.getString("address");
    patient.guardianName = resultSet.getString("guardian_name");
    patient.guardianRelationship = resultSet.getString("guardian_relationship");
    patient.guardianPhone = resultSet.getString("guardian_phone");
    patient.allergies = resultSet.getString("allergies");
    patient.medicalHistory = resultSet.getString("medical_history");
    patient.remark = resultSet.getString("remark");
    patient.balanceCents = resultSet.getLong("balance_cents");
    return patient;
  }

  private static LocalDate requiredDate(ResultSet resultSet, Long patientId) throws SQLException {
    String value = resultSet.getString("birth_date");
    if (value == null) {
      throw new SQLException("患者 " + patientId + " 的出生日期为空，数据库记录已损坏");
    }
    try {
      return LocalDate.parse(value);
    } catch (DateTimeParseException exception) {
      throw new SQLException("患者 " + patientId + " 的出生日期格式无效: " + value, exception);
    }
  }
}
