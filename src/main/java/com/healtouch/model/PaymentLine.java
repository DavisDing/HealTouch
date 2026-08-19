package com.healtouch.model;

public class PaymentLine {
  public final PaymentMethod method;
  public final long amountCents;

  public PaymentLine(PaymentMethod method, long amountCents) {
    if (method == null || amountCents <= 0) throw new IllegalArgumentException("支付金额必须大于 0");
    this.method = method;
    this.amountCents = amountCents;
  }
}
