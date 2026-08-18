package com.healtouch.util;

public final class Checks {
    private Checks() {}
    public static String required(String value, String label) {
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException(label + "不能为空");
        return value.trim();
    }
    public static void state(boolean condition, String message) { if (!condition) throw new IllegalStateException(message); }
}
