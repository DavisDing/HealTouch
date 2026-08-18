package com.healtouch.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;

public final class Money {
    private Money() {}
    public static long parse(String value) {
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException("金额不能为空");
        try {
            BigDecimal result = new BigDecimal(value.trim()).movePointRight(2).setScale(0, RoundingMode.UNNECESSARY);
            return result.longValueExact();
        } catch (ArithmeticException | NumberFormatException e) {
            throw new IllegalArgumentException("金额格式无效，最多保留两位小数");
        }
    }
    public static String format(long cents) {
        return NumberFormat.getCurrencyInstance(Locale.CHINA).format(BigDecimal.valueOf(cents, 2));
    }
}
