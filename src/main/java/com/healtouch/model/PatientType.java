package com.healtouch.model;

import java.time.LocalDate;

public enum PatientType {
  ADULT("成年人"),
  CHILD("未成年人");

  private final String label;

  PatientType(String label) {
    this.label = label;
  }

  public String getLabel() {
    return label;
  }

  /** Patients become adults on their eighteenth birthday. */
  public static PatientType fromBirthDate(LocalDate birthDate) {
    if (birthDate == null) return null;
    return birthDate.plusYears(18).isAfter(LocalDate.now()) ? CHILD : ADULT;
  }

  @Override
  public String toString() {
    return label;
  }
}
