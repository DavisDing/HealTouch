package com.healtouch.ui;

import com.healtouch.app.AppServices;
import com.healtouch.model.BillStatus;
import com.healtouch.model.BillSummary;
import com.healtouch.model.Permission;
import com.healtouch.model.UserSession;
import com.healtouch.service.DepositService;
import com.healtouch.service.Authorization;
import com.healtouch.util.Money;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
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
    keyword.setPromptText("搜索患者姓名、证件号码或账单编号...");
    keyword.setPrefWidth(260);
    keyword.setOnAction(e -> load());

    status.setPromptText("全部状态");
    status.setOnAction(e -> load());

    Button search = Ui.primaryButton("查询");
    search.setOnAction(e -> load());

    Button refresh = new Button("刷新重载");
    refresh.setOnAction(e -> load());

    Button prepaidHistory = new Button("预存变动记录");
    prepaidHistory.setOnAction(e -> showPrepaidHistory());

    Button voidBill = Ui.dangerButton("作废超期/待收费单");
    voidBill.setDisable(!can(Permission.TREATMENT_CREATE));
    voidBill.setOnAction(e -> voidBill());

    Button refund = new Button("申请冲正退款");
    refund.setDisable(!can(Permission.BILL_REFUND));
    refund.setOnAction(e -> refund());

    Button charge=Ui.primaryButton("收取选中账单");
    charge.setDisable(!can(Permission.BILL_CHARGE));
    charge.setOnAction(e->{try {
      BillSummary bill=table.getSelectionModel().getSelectedItem();
      if(bill==null || bill.status!=BillStatus.PENDING_PAYMENT) throw new IllegalArgumentException("请选择待收费账单");
      com.healtouch.model.Patient patient=DbWork.read(services.patients::get,session,bill.patientId);
      new TreatmentPage(services,session).showCharge(bill.id,DbWork.read(services.billing::receivable,session,bill.id),patient);load();
    } catch(Exception error) {Ui.error(error);}});
    Button discount=new Button(can(Permission.DISCOUNT_APPLY)?"折扣授权/审批":"申请折扣");
    discount.setDisable(!can(Permission.BILL_CHARGE));
    discount.setOnAction(e->discount());
    HBox tb = Ui.toolbar(keyword, status, search, refresh);
    HBox actions = Ui.toolbar(prepaidHistory, charge, discount, Ui.spacer(), voidBill, refund);

    TableColumn<BillSummary, String> billCodeCol = column("账单单据号", x -> x.billCode);
    billCodeCol.setPrefWidth(140);
    billCodeCol.setStyle("-fx-font-weight: bold;");

    TableColumn<BillSummary, String> patientCol = column("患者姓名", x -> x.patientName);
    patientCol.setPrefWidth(110);

    TableColumn<BillSummary, String> dateCol = column("就诊日期", x -> String.valueOf(x.treatmentDate));
    dateCol.setPrefWidth(110);

    TableColumn<BillSummary, String> therapistCol = column("责任主治医师", x -> x.therapistName);
    therapistCol.setPrefWidth(130);

    TableColumn<BillSummary, String> statusCol = column("单据结算状态", x -> statusLabel(x.status));
    statusCol.setPrefWidth(110);
    statusCol.setCellFactory(c -> new TableCell<BillSummary, String>() {
      @Override
      protected void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
          setGraphic(null);
          setText(null);
        } else if ("已支付".equals(item)) {
          setGraphic(Ui.paidBadge());
          setText(null);
        } else if ("待收费".equals(item)) {
          setGraphic(Ui.pendingBadge());
          setText(null);
        } else {
          setText(item);
          setStyle("-fx-font-size: 11px; -fx-text-fill: #668292;");
        }
      }
    });

    TableColumn<BillSummary, String> receivableCol = column("应收总额", x -> Money.format(x.receivableCents));
    receivableCol.setPrefWidth(110);

    TableColumn<BillSummary, String> paidCol = column("实收金额 (全额)", x -> Money.format(x.paidCents));
    paidCol.setPrefWidth(130);
    paidCol.setStyle("-fx-font-weight: bold; -fx-text-fill: #087F6D;");

    TableColumn<BillSummary, String> refundedCol = column("累计已退款", x -> Money.format(x.refundedCents));
    refundedCol.setPrefWidth(110);
    refundedCol.setStyle("-fx-text-fill: #DC2626;");

    table.getColumns().addAll(
        billCodeCol,
        patientCol,
        dateCol,
        therapistCol,
        statusCol,
        receivableCol,
        paidCol,
        refundedCol);

    table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    VBox.setVgrow(table, Priority.ALWAYS);

    VBox page = Ui.page(
        "账单与收银管理",
        "全额收齐原则 · 待收费单据 30 分钟未结算自动作废 · 严禁欠费挂账",
        tb,
        actions,
        table);
    load();
    return page;
  }

  private void voidBill() {
    BillSummary bill = table.getSelectionModel().getSelectedItem();
    try {
      if (bill == null) throw new IllegalArgumentException("请选择需要作废的账单");
      if (Ui.confirm("确认作废待收费账单 " + bill.billCode + " 吗？作废后不可恢复。")) {
        DbWork.write(services.billing::voidPending, session, bill.id, "前台操作员主动取消");
        load();
      }
    } catch (Exception exception) { Ui.error(exception); }
  }

  private void load() {
    try {
      table.setItems(FXCollections.observableArrayList(DbWork.read(services.billing::list, session, keyword.getText(), status.getValue())));
    } catch (Exception e) { Ui.error(e); }
  }

  private void showPrepaidHistory() {
    BillSummary bill = table.getSelectionModel().getSelectedItem();
    if (bill == null) { Ui.error(new IllegalArgumentException("请在列表中选择账单后查看该患者的预存记录")); return; }
    try {
      long balance = DbWork.read(services.deposits::balance, session, bill.patientId);
      TableView<DepositService.TransactionSummary> history = new TableView<DepositService.TransactionSummary>();
      history.getColumns().addAll(
          historyColumn("记账流水时间", row -> row.createdAt),
          historyColumn("业务类型", row -> row.type == null ? "—" : row.type.getLabel()),
          historyColumn("变动金额", row -> Money.format(row.amountCents)),
          historyColumn("划扣后余额", row -> Money.format(row.balanceAfterCents)),
          historyColumn("关联账单单号", row -> row.billCode == null ? "—" : row.billCode),
          historyColumn("审计备注", row -> row.remark == null ? "—" : row.remark));
      history.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
      history.setPrefHeight(340);
      history.setItems(FXCollections.observableArrayList(DbWork.read(services.deposits::history, session, bill.patientId)));
      Dialog<Void> dialog = new Dialog<Void>();
      dialog.setTitle("患者预存资金双写审计流水");
      dialog.setHeaderText("患者：" + bill.patientName + "  ·  当前可用预存结存：" + Money.format(balance));
      dialog.getDialogPane().setContent(history);
      dialog.getDialogPane().getButtonTypes().add(javafx.scene.control.ButtonType.CLOSE);
      Ui.show(dialog);
    } catch (Exception exception) { Ui.error(exception); }
  }

  private void discount() {
    try {DiscountDialog.show(services,session,table.getSelectionModel().getSelectedItem());load();}
    catch(Exception error){Ui.error(error);}
  }

  private void refund() {
    BillSummary bill = table.getSelectionModel().getSelectedItem();
    if (bill == null) { Ui.error(new IllegalArgumentException("请选择需要申请退款的账单")); return; }
    Dialog<ButtonType> dialog = new Dialog<ButtonType>();
    dialog.setTitle("发起账单退款申请");
    dialog.setHeaderText("账单号：" + bill.billCode + "，当前最大可退款额：" + Money.format(bill.paidCents - bill.refundedCents));
    TextField amount=new TextField();Ui.moneyField(amount);amount.setPromptText("退款金额（元）");
    TextField reason=new TextField();reason.setPromptText("退款原因（必填）");
    TextArea remark=new TextArea();remark.setPromptText("申请人备注（可选）");remark.setPrefRowCount(2);
    dialog.getDialogPane().setContent(new VBox(10,amount,reason,remark));
    dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK,ButtonType.CANCEL);
    dialog.getDialogPane().lookupButton(ButtonType.OK).addEventFilter(javafx.event.ActionEvent.ACTION,event->{
      try {
        long id = DbWork.read(services.refunds::request, session, bill.id, Money.parse(amount.getText()), reason.getText(), remark.getText());
        Ui.info("退款申请已成功提交至系统管理员审批，审批流水号：" + id);
        load();
      } catch (Exception exception) { event.consume();Ui.error(exception); }
    });
    Ui.show(dialog);
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

  private boolean can(Permission permission) { return session.can(permission); }
}
