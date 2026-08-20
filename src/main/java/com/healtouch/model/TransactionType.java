package com.healtouch.model;

public enum TransactionType {
  RECHARGE("充值"),
  CONSUMPTION("预存扣款"),
  REFUND("余额退款");

  private final String label;

  TransactionType(String label) {
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
