package com.healtouch.dao;

import com.healtouch.model.Gender;
import com.healtouch.model.Patient;
import com.healtouch.model.PatientType;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PatientDao {
    private final DataSource dataSource;
    public PatientDao(DataSource dataSource) { this.dataSource = dataSource; }

    public Patient find(long id) {
        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(baseSql() + " WHERE p.id=?")) {
            ps.setLong(1, id); try (ResultSet rs = ps.executeQuery()) { return rs.next() ? map(rs) : null; }
        } catch (SQLException e) { throw new IllegalStateException("查询患者失败", e); }
    }
    public Patient findByDocument(String type, String number) {
        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(baseSql() + " WHERE p.id_type=? AND p.id_number=?")) {
            ps.setString(1, type); ps.setString(2, number); try (ResultSet rs = ps.executeQuery()) { return rs.next() ? map(rs) : null; }
        } catch (SQLException e) { throw new IllegalStateException("查询患者失败", e); }
    }
    public List<Patient> search(String keyword) {
        List<Patient> list = new ArrayList<Patient>(); String term = "%" + (keyword == null ? "" : keyword.trim()) + "%";
        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(baseSql() + " WHERE p.name LIKE ? OR p.id_number LIKE ? OR p.phone LIKE ? ORDER BY p.created_at DESC LIMIT 100")) {
            ps.setString(1, term); ps.setString(2, term); ps.setString(3, term);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) list.add(map(rs)); }
            return list;
        } catch (SQLException e) { throw new IllegalStateException("查询患者失败", e); }
    }
    private String baseSql() { return "SELECT p.*, COALESCE(d.balance_cents, 0) balance_cents FROM patient p LEFT JOIN deposit_account d ON d.patient_id=p.id"; }
    public static Patient map(ResultSet rs) throws SQLException {
        Patient p = new Patient(); p.id = rs.getLong("id"); p.patientCode = rs.getString("patient_code");
        p.patientType = PatientType.valueOf(rs.getString("patient_type")); p.name = rs.getString("name");
        p.gender = Gender.valueOf(rs.getString("gender")); p.idType = rs.getString("id_type"); p.idNumber = rs.getString("id_number");
        p.birthDate = LocalDate.parse(rs.getString("birth_date")); p.phone = rs.getString("phone"); p.address = rs.getString("address");
        p.guardianName = rs.getString("guardian_name"); p.guardianRelationship = rs.getString("guardian_relationship"); p.guardianPhone = rs.getString("guardian_phone");
        p.allergies = rs.getString("allergies"); p.medicalHistory = rs.getString("medical_history"); p.remark = rs.getString("remark"); p.balanceCents = rs.getLong("balance_cents"); return p;
    }
}
