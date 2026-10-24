package com.healtouch.app;

import com.healtouch.config.Database;
import com.healtouch.service.AuthService;
import com.healtouch.service.Authorization;
import com.healtouch.service.BillingService;
import com.healtouch.service.CatalogService;
import com.healtouch.service.DashboardService;
import com.healtouch.service.DepositService;
import com.healtouch.service.PatientService;
import com.healtouch.service.PermissionService;
import com.healtouch.service.RefundService;
import com.healtouch.service.UserService;
import com.healtouch.service.BackupService;
import com.healtouch.service.MaintenanceService;

/** Application-level service container. The desktop process shares one SQLite connection pool. */
public class AppServices implements AutoCloseable {
  public final Database database;
  public final AuthService auth;
  public final UserService users;
  public final PatientService patients;
  public final CatalogService catalog;
  public final BillingService billing;
  public final DepositService deposits;
  public final RefundService refunds;
  public final DashboardService dashboard;
  public final PermissionService permissions;
  public final BackupService backups;
  public final MaintenanceService maintenance;

  public AppServices() {
    database = new Database(com.healtouch.config.AppPaths.databaseFile(),true);
    Authorization.configure(database.dataSource());
    auth = new AuthService(database.dataSource());
    users = new UserService(database.dataSource());
    patients = new PatientService(database.dataSource());
    catalog = new CatalogService(database.dataSource());
    billing = new BillingService(database.dataSource());
    deposits = new DepositService(database.dataSource());
    refunds = new RefundService(database.dataSource());
    dashboard = new DashboardService(database.dataSource());
    permissions = new PermissionService(database.dataSource());
    backups = new BackupService(database);
    maintenance = new MaintenanceService(database,billing,backups);
  }

  @Override
  public void close() {
    maintenance.close();
    database.close();
  }
}
