package com.healtouch.model;

public enum Gender {
  MALE("男"),
  FEMALE("女"),
  UNKNOWN("未知");

  private final String label;

  Gender(String label) {
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
