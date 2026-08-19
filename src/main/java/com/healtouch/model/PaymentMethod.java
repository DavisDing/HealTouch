package com.healtouch.model;

public enum PaymentMethod {
  DEPOSIT("预存扣款"),
  CASH("现金"),
  WECHAT("微信"),
  ALIPAY("支付宝"),
  BANK_CARD("银行卡"),
  OTHER("其他");
  private final String label;

  PaymentMethod(String label) {
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
