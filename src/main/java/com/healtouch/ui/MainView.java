package com.healtouch.ui;

import com.healtouch.app.AppServices;
import com.healtouch.model.Permission;
import com.healtouch.model.UserSession;
import com.healtouch.service.Authorization;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class MainView {
  private final BorderPane root = new BorderPane();
  private final AppServices services;
  private final UserSession session;

  public MainView(AppServices services, UserSession session, Runnable logout) {
    this.services = services;
    this.session = session;
    HBox brand = Ui.brand();
    Label who = new Label(session.name + " · " + session.roles);
    who.setStyle("-fx-text-fill: #526c7c; -fx-font-weight: bold;");
    Button exit = new Button("退出登录");
    exit.setOnAction(e -> logout.run());
    Region spacer = new Region();
    HBox.setHgrow(spacer, Priority.ALWAYS);
    HBox header = new HBox(18, brand, spacer, who, exit);
    header.setPadding(new Insets(14, 24, 14, 24));
    header.setStyle("-fx-background-color: white; -fx-border-color: transparent transparent #dfe8ed transparent;");
    root.setTop(header);

    VBox nav = new VBox(4);
    nav.setPadding(new Insets(18, 12, 18, 12));
    nav.setPrefWidth(208);
    nav.setStyle("-fx-background-color: #ffffff; -fx-border-color: transparent #e1eaee transparent transparent;");
    Label navigation = new Label("业务导航");
    navigation.setStyle("-fx-text-fill: #71909a; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 4 10 8 10;");
    nav.getChildren().add(navigation);
    add(nav, "首页工作台", () -> show(new DashboardPage(services, session, this).node()));
    if (can(Permission.PATIENT_VIEW))
      add(nav, "患者档案", () -> show(new PatientPage(services, session).node()));
    if (can(Permission.TREATMENT_CREATE))
      add(nav, "治疗登记", () -> show(new TreatmentPage(services, session).node()));
    if (can(Permission.BILL_CHARGE) || can(Permission.BILL_REFUND))
      add(nav, "账单管理", () -> show(new BillPage(services, session).node()));
    if (can(Permission.BILL_REFUND))
      add(nav, "退款审批", () -> show(new RefundPage(services, session).node()));
    if (can(Permission.DEPOSIT_RECHARGE) || can(Permission.DEPOSIT_REFUND))
      add(nav, "预存管理", () -> show(new DepositPage(services, session).node()));
    if (can(Permission.REPORT_VIEW))
      add(nav, "数据统计", () -> show(new ReportPage(services, session).node()));
    if (can(Permission.SYSTEM_MANAGE)) {
      Separator sep = new Separator();
      sep.setStyle("-fx-padding: 8 4 8 4;");
      nav.getChildren().add(sep);
      Label label = new Label("系统管理");
      label.setStyle("-fx-text-fill: #71909a; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 4 10 8 10;");
      nav.getChildren().add(label);
      add(nav, "用户管理", () -> show(new UserPage(services, session).node()));
      add(nav, "权限管理", () -> show(new PermissionPage(services, session).node()));
      add(nav, "治疗项目", () -> show(new CatalogPage(services, session).node()));
    }
    root.setLeft(nav);
    show(new DashboardPage(services, session, this).node());
  }

  public Parent node() { return root; }

  public void show(Parent page) { root.setCenter(page); }

  public void openPatients() { show(new PatientPage(services, session).node()); }

  public void openCreatePatient() { show(new PatientPage(services, session, true).node()); }

  public void openTreatment() { show(new TreatmentPage(services, session).node()); }

  public void openBills() { show(new BillPage(services, session).node()); }

  public void openDeposits() { show(new DepositPage(services, session).node()); }

  private boolean can(Permission p) {
    try { Authorization.require(session, p); return true; }
    catch (SecurityException e) { return false; }
  }

  private void add(VBox box, String text, Runnable action) {
    Button b = Ui.navButton(text);
    b.setOnAction(e -> action.run());
    box.getChildren().add(b);
  }
}
