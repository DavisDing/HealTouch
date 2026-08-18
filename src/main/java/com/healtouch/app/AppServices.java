package com.healtouch.app;

import com.healtouch.config.Database;
import com.healtouch.service.*;

/** 应用级服务容器：整个桌面进程共享一个 SQLite 连接池。 */
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
        auth = new AuthService(database.dataSource()); auth.ensureInitialAdministrator();
        users = new UserService(database.dataSource()); patients = new PatientService(database.dataSource());
        catalog = new CatalogService(database.dataSource()); billing = new BillingService(database.dataSource());
        deposits = new DepositService(database.dataSource()); refunds = new RefundService(database.dataSource());
        dashboard = new DashboardService(database.dataSource()); permissions = new PermissionService(database.dataSource());
    }
    @Override public void close() { database.close(); }
}
