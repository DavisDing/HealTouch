package com.healtouch.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

public final class Codes {
  private static final AtomicLong SEQUENCE = new AtomicLong(0);
  private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

  private Codes() {}

  public static String next(String prefix) {
    return prefix
        + LocalDateTime.now().format(FORMAT)
        + String.format("%04d", SEQUENCE.incrementAndGet() % 10000);
  }

  public static String sequential(String prefix, long id) {
    return prefix + String.format("%06d", id);
  }
}
