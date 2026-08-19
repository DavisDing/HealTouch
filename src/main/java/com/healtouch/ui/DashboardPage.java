package com.healtouch.ui;

import com.healtouch.app.AppServices;
import com.healtouch.model.UserSession;
import com.healtouch.service.DashboardService;
import com.healtouch.util.Money;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
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
    cards.setHgap(14);
    cards.add(card("今日服务净收入", income), 0, 0);
    cards.add(card("今日新增患者", patients), 1, 0);
    cards.add(card("今日已收费账单", bills), 2, 0);
    Button create = new Button("创建档案");
    create.setOnAction(e -> main.openPatients());
    Button treatment = new Button("治疗登记");
    treatment.setOnAction(e -> main.openTreatment());
    Button bill = new Button("账单管理");
    bill.setOnAction(e -> main.openBills());
    Button deposit = new Button("预存管理");
    deposit.setOnAction(e -> main.openDeposits());
    VBox page =
        Ui.page(
            "首页工作台", cards, new Label("业务快捷入口"), new HBox(10, create, treatment, bill, deposit));
    refresh();
    return page;
  }

  private VBox card(String title, Label value) {
    Label t = new Label(title);
    t.setStyle("-fx-text-fill: #666;");
    value.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
    VBox box = new VBox(8, t, value);
    box.setPrefWidth(220);
    box.setStyle("-fx-padding: 18; -fx-background-color: #f2f7ff; -fx-background-radius: 8;");
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
