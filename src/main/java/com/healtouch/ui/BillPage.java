package com.healtouch.ui;

import com.healtouch.app.AppServices;
import com.healtouch.model.BillStatus;
import com.healtouch.model.BillSummary;
import com.healtouch.model.UserSession;
import com.healtouch.service.DepositService;
import com.healtouch.util.Money;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class BillPage {
  private final AppServices services;
  private final UserSession session;
  private final TableView<BillSummary> table = new TableView<BillSummary>();
  private final TextField keyword = new TextField();
  private final ComboBox<BillStatus> status = new ComboBox<BillStatus>(FXCollections.observableArrayList(BillStatus.values()));

  public BillPage(AppServices services, UserSession session) {
    this.services = services;
    this.session = session;
  }

  public Parent node() {
    keyword.setPromptText("患者、证件号码或账单号");
    keyword.setPrefWidth(230);
    status.setPromptText("全部状态");
    Button search = new Button("查询");
    search.setOnAction(e -> load());
    keyword.setOnAction(e -> load());
    Button prepaidHistory = new Button("查看患者预存记录");
    prepaidHistory.setOnAction(e -> showPrepaidHistory());
    Button voidBill = new Button("作废待收费账单");
    voidBill.setOnAction(e -> voidBill());
    Button refund = new Button("申请退款");
    refund.setOnAction(e -> refund());
    table.getColumns().addAll(
        column("账单号", x -> x.billCode),
        column("患者", x -> x.patientName),
        column("日期", x -> String.valueOf(x.treatmentDate)),
        column("治疗师", x -> x.therapistName),
        column("状态", x -> statusLabel(x.status)),
        column("应收", x -> Money.format(x.receivableCents)),
        column("已收", x -> Money.format(x.paidCents)),
        column("已退款", x -> Money.format(x.refundedCents)));
    table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    VBox.setVgrow(table, Priority.ALWAYS);
    VBox page = Ui.page("账单管理", Ui.toolbar(keyword, status, search, prepaidHistory, voidBill, refund), table);
    load();
    return page;
  }

  private void voidBill() {
    BillSummary bill = table.getSelectionModel().getSelectedItem();
    try {
      if (bill == null) throw new IllegalArgumentException("请选择账单");
      if (Ui.confirm("确认作废账单 " + bill.billCode + "？")) {
        services.billing.voidPending(session, bill.id, "前台主动取消");
        load();
      }
    } catch (Exception exception) { Ui.error(exception); }
  }

  private void load() {
    try {
      services.billing.voidExpiredPending();
      table.setItems(FXCollections.observableArrayList(services.billing.list(keyword.getText(), status.getValue())));
    } catch (Exception e) { Ui.error(e); }
  }

  private void showPrepaidHistory() {
    BillSummary bill = table.getSelectionModel().getSelectedItem();
    if (bill == null) { Ui.error(new IllegalArgumentException("请选择账单后查看该患者的预存记录")); return; }
    try {
      long balance = services.deposits.balance(session, bill.patientId);
      TableView<DepositService.TransactionSummary> history = new TableView<DepositService.TransactionSummary>();
      history.getColumns().addAll(
          historyColumn("时间", row -> row.createdAt),
          historyColumn("类型", row -> row.type == null ? "—" : row.type.getLabel()),
          historyColumn("金额", row -> Money.format(row.amountCents)),
          historyColumn("余额", row -> Money.format(row.balanceAfterCents)),
          historyColumn("关联账单", row -> row.billCode == null ? "—" : row.billCode),
          historyColumn("备注", row -> row.remark == null ? "—" : row.remark));
      history.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
      history.setPrefHeight(340);
      history.setItems(FXCollections.observableArrayList(services.deposits.history(session, bill.patientId)));
      Dialog<Void> dialog = new Dialog<Void>();
      dialog.setTitle("患者预存记录");
      dialog.setHeaderText(bill.patientName + " · 当前预存余额：" + Money.format(balance));
      dialog.getDialogPane().setContent(history);
      dialog.getDialogPane().getButtonTypes().add(javafx.scene.control.ButtonType.CLOSE);
      dialog.showAndWait();
    } catch (Exception exception) { Ui.error(exception); }
  }

  private void refund() {
    BillSummary bill = table.getSelectionModel().getSelectedItem();
    if (bill == null) { Ui.error(new IllegalArgumentException("请选择账单")); return; }
    TextInputDialog dialog = new TextInputDialog();
    dialog.setTitle("申请账单退款");
    dialog.setHeaderText(bill.billCode + "，可退款：" + Money.format(bill.paidCents - bill.refundedCents));
    dialog.setContentText("退款金额：");
    dialog.showAndWait().ifPresent(value -> {
      try {
        long id = services.refunds.request(session, bill.id, Money.parse(value), "界面申请退款", null);
        Ui.info("退款申请已提交，编号：" + id);
        load();
      } catch (Exception exception) { Ui.error(exception); }
    });
  }

  private TableColumn<BillSummary, String> column(String title, java.util.function.Function<BillSummary, String> getter) {
    TableColumn<BillSummary, String> column = new TableColumn<BillSummary, String>(title);
    column.setCellValueFactory(x -> new SimpleStringProperty(getter.apply(x.getValue())));
    return column;
  }

  private TableColumn<DepositService.TransactionSummary, String> historyColumn(
      String title, java.util.function.Function<DepositService.TransactionSummary, String> getter) {
    TableColumn<DepositService.TransactionSummary, String> column = new TableColumn<DepositService.TransactionSummary, String>(title);
    column.setCellValueFactory(x -> new SimpleStringProperty(getter.apply(x.getValue())));
    return column;
  }

  private String statusLabel(BillStatus status) {
    switch (status) {
      case PENDING_PAYMENT: return "待收费";
      case PAID: return "已支付";
      case PARTIALLY_REFUNDED: return "部分退款";
      case REFUNDED: return "已退款";
      default: return "已作废";
    }
  }
}
