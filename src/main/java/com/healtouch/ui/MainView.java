package com.healtouch.ui;

import com.healtouch.app.AppServices;
import com.healtouch.model.Permission;
import com.healtouch.model.UserSession;
import com.healtouch.service.Authorization;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.HashMap;
import java.util.Map;

public class MainView {
  private final BorderPane root = new BorderPane();
  private final AppServices services;
  private final UserSession session;
  private final Map<String, Button> navButtons = new HashMap<String, Button>();
  private String currentActiveNav = "";

  public MainView(AppServices services, UserSession session, Runnable logout) {
    this.services = services;
    this.session = session;

    // Top Bar
    HBox brand = Ui.brand();

    HBox userBox = new HBox(8);
    userBox.setAlignment(Pos.CENTER_LEFT);
    userBox.setPadding(new Insets(4, 12, 4, 8));
    userBox.setStyle("-fx-background-color: #F0F5F8; -fx-background-radius: 16;");

    Label avatar = new Label(session.name.isEmpty() ? "医" : session.name.substring(0, 1));
    avatar.setStyle("-fx-background-color: #087F6D; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 3 7; -fx-background-radius: 10;");

    Label who = new Label(session.name + " (" + session.roles + ")");
    who.setStyle("-fx-text-fill: #244255; -fx-font-weight: bold; -fx-font-size: 12px;");
    userBox.getChildren().addAll(avatar, who);

    Button exit = new Button("退出登录");
    exit.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #D3E0E8; -fx-border-radius: 6; -fx-font-size: 12px; -fx-text-fill: #668292;");
    exit.setOnAction(e -> logout.run());

    Region spacer = new Region();
    HBox.setHgrow(spacer, Priority.ALWAYS);

    HBox header = new HBox(16, brand, spacer, userBox, exit);
    header.setAlignment(Pos.CENTER_LEFT);
    header.setPadding(new Insets(12, 24, 12, 24));
    header.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: transparent transparent #E5EDF0 transparent; -fx-border-width: 1;");
    root.setTop(header);

    // Left Navigation
    VBox nav = new VBox(4);
    nav.setPadding(new Insets(16, 10, 16, 10));
    nav.setPrefWidth(216);
    nav.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: transparent #E5EDF0 transparent transparent; -fx-border-width: 1;");

    Label navigation = new Label("核心门诊业务");
    navigation.setStyle("-fx-text-fill: #8DA4B0; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 6 12 8 12; -fx-letter-spacing: 0.5px;");
    nav.getChildren().add(navigation);

    add(nav, "dashboard", "首页工作台", () -> show("dashboard", new DashboardPage(services, session, this).node()));
    if (can(Permission.PATIENT_VIEW))
      add(nav, "patients", "患者档案", () -> show("patients", new PatientPage(services, session).node()));
    if (can(Permission.TREATMENT_CREATE))
      add(nav, "treatment", "治疗登记", () -> show("treatment", new TreatmentPage(services, session).node()));
    if (can(Permission.BILL_CHARGE) || can(Permission.BILL_REFUND))
      add(nav, "bills", "账单与收银", () -> show("bills", new BillPage(services, session).node()));
    if (can(Permission.BILL_REFUND))
      add(nav, "refunds", "退款审批", () -> show("refunds", new RefundPage(services, session).node()));
    if (can(Permission.DEPOSIT_RECHARGE) || can(Permission.DEPOSIT_REFUND))
      add(nav, "deposits", "预存管理", () -> show("deposits", new DepositPage(services, session).node()));
    if (can(Permission.REPORT_VIEW))
      add(nav, "reports", "数据统计", () -> show("reports", new ReportPage(services, session).node()));

    if (can(Permission.SYSTEM_MANAGE)) {
      Ui.openRecovery=()->show("backups",new BackupPage(services,session).node());
      Separator sep = new Separator();
      sep.setStyle("-fx-padding: 10 4 6 4;");
      nav.getChildren().add(sep);

      Label label = new Label("门诊运维设置");
      label.setStyle("-fx-text-fill: #8DA4B0; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 4 12 8 12; -fx-letter-spacing: 0.5px;");
      nav.getChildren().add(label);

      add(nav, "users", "用户管理", () -> show("users", new UserPage(services, session).node()));
      add(nav, "permissions", "权限管理", () -> show("permissions", new PermissionPage(services, session).node()));
      add(nav, "backups", "数据备份与还原", () -> show("backups", new BackupPage(services, session).node()));
      add(nav, "catalog", "治疗项目", () -> show("catalog", new CatalogPage(services, session).node()));
    }

    Region navSpacer = new Region();
    VBox.setVgrow(navSpacer, Priority.ALWAYS);
    nav.getChildren().add(navSpacer);

    // Bottom status box
    VBox dbStatus = new VBox(2);
    dbStatus.setPadding(new Insets(10, 12, 10, 12));
    dbStatus.setStyle("-fx-background-color: #F7FAFC; -fx-border-color: #E3EBEF; -fx-border-radius: 8; -fx-background-radius: 8;");
    Label dbStatusTitle = new Label("● 本地 SQLite 在线");
    dbStatusTitle.setStyle("-fx-text-fill: #10B981; -fx-font-size: 11px; -fx-font-weight: bold;");
    Label dbStatusSub = new Label("就诊记录双写事务保障");
    dbStatusSub.setStyle("-fx-text-fill: #8CA1AE; -fx-font-size: 10px;");
    dbStatus.getChildren().addAll(dbStatusTitle, dbStatusSub);
    nav.getChildren().add(dbStatus);

    root.setLeft(nav);
    show("dashboard", new DashboardPage(services, session, this).node());
  }

  public Parent node() { return root; }

  public void show(Parent page) { root.setCenter(page); }

  public void show(String navKey, Parent page) {
    this.currentActiveNav = navKey;
    for (Map.Entry<String, Button> entry : navButtons.entrySet()) {
      boolean active = entry.getKey().equals(navKey);
      Button b = entry.getValue();
      if (active) {
        b.setStyle("-fx-alignment: CENTER_LEFT; -fx-padding: 10 14; -fx-background-color: #E8F6F3; -fx-border-color: transparent; -fx-font-weight: bold; -fx-text-fill: #087F6D; -fx-font-size: 13px; -fx-background-radius: 8;");
      } else {
        b.setStyle("-fx-alignment: CENTER_LEFT; -fx-padding: 10 14; -fx-background-color: transparent; -fx-border-color: transparent; -fx-font-weight: 500; -fx-text-fill: #415E70; -fx-font-size: 13px;");
      }
    }
    root.setCenter(page);
  }

  public void openPatients() { show("patients", new PatientPage(services, session).node()); }

  public void openCreatePatient() { show("patients", new PatientPage(services, session, true).node()); }

  public void openTreatment() { show("treatment", new TreatmentPage(services, session).node()); }

  public void openBills() { show("bills", new BillPage(services, session).node()); }

  public void openDeposits() { show("deposits", new DepositPage(services, session).node()); }

  private boolean can(Permission p) { return session.can(p); }

  private void add(VBox box, final String key, String text, final Runnable action) {
    Button b = Ui.navButton(text);
    b.setOnAction(e -> action.run());
    navButtons.put(key, b);
    box.getChildren().add(b);
  }
}
