package com.healtouch.ui;

import com.healtouch.app.AppServices;
import com.healtouch.model.UserSession;
import com.healtouch.service.DashboardService;
import com.healtouch.util.Money;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class DashboardPage {
  private final AppServices services;
  private final UserSession session;
  private final MainView main;
  private final Label income = new Label();
  private final Label patients = new Label();
  private final Label bills = new Label();

  public DashboardPage(AppServices services, UserSession session, MainView main) {
    this.services = services;
    this.session = session;
    this.main = main;
  }

  public Parent node() {
    GridPane cards = new GridPane();
    cards.setHgap(16);
    cards.add(card("今日服务净收入", income, "已完成收费的服务收入"), 0, 0);
    cards.add(card("今日新增患者", patients, "今天新建的患者档案"), 1, 0);
    cards.add(card("今日已收费账单", bills, "已完成收费的治疗单"), 2, 0);
    for (int i = 0; i < 3; i++) GridPane.setHgrow(cards.getChildren().get(i), Priority.ALWAYS);

    Button create = Ui.primaryButton("＋ 创建患者档案");
    create.setOnAction(e -> main.openCreatePatient());
    Button treatment = new Button("治疗登记");
    treatment.setOnAction(e -> main.openTreatment());
    Button bill = new Button("账单管理");
    bill.setOnAction(e -> main.openBills());
    Button deposit = new Button("预存管理");
    deposit.setOnAction(e -> main.openDeposits());
    Label hint = new Label("从这里开始今天的门诊工作：先建档，再登记治疗并完成收费。");
    hint.setStyle("-fx-text-fill: #65808e;");
    VBox shortcuts = Ui.section(new Label("常用业务"), hint, new HBox(10, create, treatment, bill, deposit));
    shortcuts.getChildren().get(0).setStyle("-fx-font-size: 17px; -fx-font-weight: bold;");

    VBox page = Ui.page("首页工作台", cards, shortcuts);
    refresh();
    return page;
  }

  private VBox card(String title, Label value, String helper) {
    Label t = new Label(title);
    t.setStyle("-fx-text-fill: #597382; -fx-font-weight: bold;");
    value.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #153d51;");
    Label h = new Label(helper);
    h.setStyle("-fx-text-fill: #7c929d; -fx-font-size: 11px;");
    VBox box = new VBox(8, t, value, h);
    box.setPadding(new Insets(18));
    box.setPrefWidth(250);
    box.setMaxWidth(Double.MAX_VALUE);
    box.setStyle("-fx-background-color: linear-gradient(to bottom right, #ffffff, #eaf7f4); -fx-background-radius: 13; -fx-border-color: #d9ebe7; -fx-border-radius: 13;");
    return box;
  }

  private void refresh() {
    try {
      DashboardService.Today t = services.dashboard.today(session);
      income.setText(Money.format(t.incomeCents));
      patients.setText(String.valueOf(t.newPatients) + " 人");
      bills.setText(String.valueOf(t.paidBills) + " 笔");
    } catch (Exception e) {
      Ui.error(e);
    }
  }
}
