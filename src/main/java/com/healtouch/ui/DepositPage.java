package com.healtouch.ui;

import com.healtouch.app.AppServices;
import com.healtouch.model.Patient;
import com.healtouch.model.PaymentMethod;
import com.healtouch.model.UserSession;
import com.healtouch.service.DepositService;
import com.healtouch.util.Money;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class DepositPage {
  private final AppServices services;
  private final UserSession session;
  private final Label balance = new Label("请选择患者查看余额与预存记录");

  public DepositPage(AppServices services, UserSession session) {
    this.services = services;
    this.session = session;
  }

  public Parent node() {
    ComboBox<Patient> patient = new ComboBox<Patient>();
    patient.setPrefWidth(310);
    patient.setPromptText("选择患者");
    TableView<DepositService.TransactionSummary> history = historyTable();
    try { patient.setItems(FXCollections.observableArrayList(services.patients.search(session, ""))); }
    catch (Exception exception) { Ui.error(exception); }
    patient.setOnAction(event -> refresh(patient.getValue(), history));

    TextField amount = new TextField();
    amount.setPromptText("金额，例如 100.00");
    ComboBox<PaymentMethod> method = new ComboBox<PaymentMethod>(
        FXCollections.observableArrayList(
            PaymentMethod.CASH, PaymentMethod.WECHAT, PaymentMethod.ALIPAY,
            PaymentMethod.BANK_CARD, PaymentMethod.OTHER));
    method.setValue(PaymentMethod.CASH);
    Button recharge = Ui.primaryButton("确认充值");
    recharge.setOnAction(event -> {
      try {
        if (patient.getValue() == null) throw new IllegalArgumentException("请选择患者");
        services.deposits.recharge(session, patient.getValue().id, Money.parse(amount.getText()), method.getValue(), "界面充值");
        refresh(patient.getValue(), history);
        amount.clear();
        Ui.info("预存充值成功");
      } catch (Exception exception) { Ui.error(exception); }
    });
    Button refund = new Button("余额退款");
    refund.setOnAction(event -> {
      try {
        if (patient.getValue() == null) throw new IllegalArgumentException("请选择患者");
        if (!Ui.confirm("确认从该患者预存余额中退款？")) return;
        services.deposits.refundBalance(session, patient.getValue().id, Money.parse(amount.getText()), method.getValue(), "界面余额退款");
        refresh(patient.getValue(), history);
        amount.clear();
        Ui.info("余额退款已完成");
      } catch (Exception exception) { Ui.error(exception); }
    });

    Label recordTitle = new Label("预存流水");
    recordTitle.setStyle("-fx-font-size: 17px; -fx-font-weight: bold;");
    VBox.setVgrow(history, Priority.ALWAYS);
    return Ui.page(
        "预存管理",
        Ui.section(new HBox(12, new Label("患者"), patient, balance),
            new HBox(12, new Label("金额"), amount, new Label("支付/退款方式"), method, recharge, refund)),
        Ui.section(recordTitle, new Label("充值、预存扣款和余额退款均会记录在此。"), history));
  }

  private TableView<DepositService.TransactionSummary> historyTable() {
    TableView<DepositService.TransactionSummary> table = new TableView<DepositService.TransactionSummary>();
    table.getColumns().addAll(
        column("时间", row -> row.createdAt),
        column("类型", row -> row.type == null ? "—" : row.type.getLabel()),
        column("金额", row -> Money.format(row.amountCents)),
        column("余额", row -> Money.format(row.balanceAfterCents)),
        column("关联账单", row -> row.billCode == null ? "—" : row.billCode),
        column("备注", row -> row.remark == null ? "—" : row.remark));
    table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    table.setPrefHeight(300);
    return table;
  }

  private TableColumn<DepositService.TransactionSummary, String> column(
      String title, java.util.function.Function<DepositService.TransactionSummary, String> getter) {
    TableColumn<DepositService.TransactionSummary, String> column = new TableColumn<DepositService.TransactionSummary, String>(title);
    column.setCellValueFactory(cell -> new SimpleStringProperty(getter.apply(cell.getValue())));
    return column;
  }

  private void refresh(Patient patient, TableView<DepositService.TransactionSummary> history) {
    try {
      balance.setText("当前余额：" + Money.format(services.deposits.balance(session, patient.id)));
      history.setItems(FXCollections.observableArrayList(services.deposits.history(session, patient.id)));
    } catch (Exception exception) { Ui.error(exception); }
  }
}
