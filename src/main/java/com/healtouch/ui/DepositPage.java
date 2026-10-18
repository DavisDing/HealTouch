package com.healtouch.ui;

import com.healtouch.app.AppServices;
import com.healtouch.model.Patient;
import com.healtouch.model.PaymentMethod;
import com.healtouch.model.Permission;
import com.healtouch.model.UserSession;
import com.healtouch.service.Authorization;
import com.healtouch.service.DepositService;
import com.healtouch.util.Money;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class DepositPage {
  private final AppServices services;
  private final UserSession session;
  private final Label balance = new Label("请选择患者查看预存资金");

  public DepositPage(AppServices services, UserSession session) {
    this.services = services;
    this.session = session;
  }

  public Parent node() {
    ComboBox<Patient> patient = new ComboBox<Patient>();
    patient.setPrefWidth(320);
    patient.setPromptText("选择需要充值或查看的患者...");

    TableView<DepositService.TransactionSummary> history = historyTable();
    try { patient.setItems(FXCollections.observableArrayList(services.patients.search(session, ""))); }
    catch (Exception exception) { Ui.error(exception); }
    patient.setOnAction(event -> refresh(patient.getValue(), history));

    TextField amount = new TextField();
    amount.setPromptText("输入充值/退款金额 (元)");
    amount.setPrefWidth(180);

    ComboBox<PaymentMethod> method = new ComboBox<PaymentMethod>(
        FXCollections.observableArrayList(
            PaymentMethod.CASH, PaymentMethod.WECHAT, PaymentMethod.ALIPAY,
            PaymentMethod.BANK_CARD, PaymentMethod.OTHER));
    method.setValue(PaymentMethod.WECHAT);

    Button recharge = Ui.primaryButton("＋ 立即充值入账");
    recharge.setDisable(!can(Permission.DEPOSIT_RECHARGE));
    recharge.setOnAction(event -> {
      try {
        if (patient.getValue() == null) throw new IllegalArgumentException("请选择患者");
        services.deposits.recharge(session, patient.getValue().id, Money.parse(amount.getText()), method.getValue(), "门诊柜台充值");
        refresh(patient.getValue(), history);
        amount.clear();
        Ui.info("预存金充值成功，资金变动流水已实时双写生效！");
      } catch (Exception exception) { Ui.error(exception); }
    });

    Button refund = Ui.dangerButton("申请余额退款");
    refund.setDisable(!can(Permission.DEPOSIT_REFUND));
    refund.setOnAction(event -> {
      try {
        if (patient.getValue() == null) throw new IllegalArgumentException("请选择患者");
        if (!Ui.confirm("确认从该患者的可用预存余额中扣除并退还资金？")) return;
        services.deposits.refundBalance(session, patient.getValue().id, Money.parse(amount.getText()), method.getValue(), "门诊预存余额退款");
        refresh(patient.getValue(), history);
        amount.clear();
        Ui.info("预存余额退款已顺利完成并记录冲正审计流水！");
      } catch (Exception exception) { Ui.error(exception); }
    });

    balance.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #087F6D;");
    HBox patientSelectRow = new HBox(12, new Label("办理患者 *:"), patient, balance);
    patientSelectRow.setAlignment(Pos.CENTER_LEFT);

    HBox actionRow = new HBox(12, new Label("变动金额 *:"), amount, new Label("渠道方式 *:"), method, recharge, refund);
    actionRow.setAlignment(Pos.CENTER_LEFT);

    VBox operationSection = Ui.section(patientSelectRow, actionRow);

    Label recordTitle = new Label("患者预存资金双写审计流水明细");
    recordTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #17384D;");
    Label recordSub = new Label("全生命周期可追溯：门诊充值、消费划扣结算及退款冲正均不可篡改，且保证预存余额不可为负。");
    recordSub.setStyle("-fx-font-size: 12px; -fx-text-fill: #668292;");

    VBox.setVgrow(history, Priority.ALWAYS);
    VBox tableSection = Ui.section(recordTitle, recordSub, history);
    VBox.setVgrow(tableSection, Priority.ALWAYS);

    return Ui.page(
        "预存管理",
        "门诊预存金全生命周期审计 · 实时双写流水 · 充值与消费可追溯",
        operationSection,
        tableSection);
  }

  private TableView<DepositService.TransactionSummary> historyTable() {
    TableView<DepositService.TransactionSummary> table = new TableView<DepositService.TransactionSummary>();

    TableColumn<DepositService.TransactionSummary, String> timeCol = column("流水时间戳", row -> row.createdAt);
    timeCol.setPrefWidth(160);

    TableColumn<DepositService.TransactionSummary, String> typeCol = column("业务类型", row -> row.type == null ? "—" : row.type.getLabel());
    typeCol.setPrefWidth(120);

    TableColumn<DepositService.TransactionSummary, String> amountCol = column("变动金额", row -> Money.format(row.amountCents));
    amountCol.setPrefWidth(130);
    amountCol.setStyle("-fx-font-weight: bold; -fx-text-fill: #087F6D;");

    TableColumn<DepositService.TransactionSummary, String> balanceCol = column("变动后结存余额", row -> Money.format(row.balanceAfterCents));
    balanceCol.setPrefWidth(140);
    balanceCol.setStyle("-fx-font-weight: bold; -fx-text-fill: #13334A;");

    TableColumn<DepositService.TransactionSummary, String> billCol = column("关联就诊账单", row -> row.billCode == null ? "—" : row.billCode);
    billCol.setPrefWidth(150);

    TableColumn<DepositService.TransactionSummary, String> remarkCol = column("审计说明备注", row -> row.remark == null ? "—" : row.remark);
    remarkCol.setPrefWidth(220);

    table.getColumns().addAll(timeCol, typeCol, amountCol, balanceCol, billCol, remarkCol);
    table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
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
      if (patient != null) {
        balance.setText("当前可用预存金余额：" + Money.format(services.deposits.balance(session, patient.id)));
        history.setItems(FXCollections.observableArrayList(services.deposits.history(session, patient.id)));
      }
    } catch (Exception exception) { Ui.error(exception); }
  }

  private boolean can(Permission permission) {
    try {
      Authorization.require(session, permission);
      return true;
    } catch (SecurityException e) {
      return false;
    }
  }
}
