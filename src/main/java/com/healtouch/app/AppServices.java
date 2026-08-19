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

  public AppServices() {
    database = new Database();
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
  }

  @Override
  public void close() {
    database.close();
  }
}
