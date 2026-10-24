package com.healtouch.service;

import com.healtouch.app.AppTasks;
import java.util.concurrent.*;

/** System-owned expiration is independent of reads and records a SYSTEM audit entry per bill. */
public final class MaintenanceService implements AutoCloseable {
  private final ScheduledFuture<?> job;
  private volatile String problem="";
  private volatile boolean paused;
  public MaintenanceService(com.healtouch.config.Database database,BillingService billing,BackupService backups) {
    job=AppTasks.DATABASE.scheduleWithFixedDelay(()->{
      if(database.recoveryRequired() || paused)return;
      try {billing.voidExpiredPending();problem="";}
      catch(Exception e) {problem="待收费账单超时处理失败，请联系管理员";org.slf4j.LoggerFactory.getLogger(MaintenanceService.class).error(problem,e);}
      backups.tick();
    },5,30,TimeUnit.SECONDS);
  }
  public String problem(){return problem;}
  public void setPaused(boolean value){paused=value;}
  public void close(){job.cancel(false);}
}
