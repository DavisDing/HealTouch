package com.healtouch.model;

public class TreatmentLine {
    public final long projectId;
    public final String projectName;
    public final long unitPriceCents;
    public final int quantity;
    public TreatmentLine(long projectId, String projectName, long unitPriceCents, int quantity) {
        if (projectId <= 0 || unitPriceCents < 0 || quantity <= 0) throw new IllegalArgumentException("治疗项目参数无效");
        this.projectId = projectId; this.projectName = projectName; this.unitPriceCents = unitPriceCents; this.quantity = quantity;
    }
    public long subtotalCents() { return unitPriceCents * quantity; }
}
