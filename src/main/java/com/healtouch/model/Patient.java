package com.healtouch.model;

import java.time.LocalDate;

public class Patient {
  public Long id;
  public String patientCode;
  public PatientType patientType;
  public String name;
  public Gender gender;
  public String idType;
  public String idNumber;
  public LocalDate birthDate;
  public String phone;
  public String address;
  public String guardianName;
  public String guardianRelationship;
  public String guardianPhone;
  public String allergies;
  public String medicalHistory;
  public String remark;
  public long balanceCents;

  public boolean isChild() {
    return patientType == PatientType.CHILD;
  }

  @Override
  public String toString() {
    return patientCode + " - " + name + "（" + phone + "）";
  }
}
