package com.healtouch.model;

public enum Permission {
  PATIENT_VIEW("查看患者档案"),
  PATIENT_EDIT("创建/编辑患者档案"),
  TREATMENT_CREATE("创建治疗登记"),
  BILL_CHARGE("账单收费"),
  DEPOSIT_RECHARGE("预存充值"),
  DEPOSIT_REFUND("预存余额退款"),
  BILL_REFUND("账单退款"),
  REPORT_VIEW("查看数据统计"),
  SYSTEM_MANAGE("系统管理"),
  DISCOUNT_APPLY("账单折扣");

  private final String label;

  Permission(String label) {
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
