package com.healtouch.model;

import java.time.LocalDate;

public class BillSummary {
  public long id;
  public String billCode;
  public long patientId;
  public String patientName;
  public LocalDate treatmentDate;
  public String therapistName;
  public BillStatus status;
  public long receivableCents;
  public long paidCents;
  public long refundedCents;
}
