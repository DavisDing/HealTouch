package com.healtouch.model;

public enum RoleCode {
  ADMIN("系统管理员"),
  RECEPTION("前台"),
  THERAPIST("治疗师"),
  FINANCE("财务/主管");

  private final String label;

  RoleCode(String label) {
    this.label = label;
  }

  public String getLabel() {
    return label;
  }

  @Override
  public String toString() {
    return label;
  }
}
