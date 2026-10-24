package com.healtouch.app;

import java.util.concurrent.*;

/** One worker serializes UI database jobs and maintenance, keeping the FX thread responsive. */
public final class AppTasks {
  private AppTasks() {}
  public static final ScheduledExecutorService DATABASE = Executors.newSingleThreadScheduledExecutor(r -> {
    Thread thread = new Thread(r, "healtouch-database"); thread.setDaemon(true); return thread;
  });
}
