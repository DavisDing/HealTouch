package com.healtouch.model;

public class TreatmentProject {
    public long id;
    public String name;
    public String code;
    public long categoryId;
    public String categoryName;
    public long priceCents;
    public Integer durationMinutes;
    public String description;
    public boolean active;
    @Override public String toString() { return name + " - " + com.healtouch.util.Money.format(priceCents); }
}
