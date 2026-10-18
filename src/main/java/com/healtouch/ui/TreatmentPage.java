package com.healtouch.ui;

import com.healtouch.app.AppServices;
import com.healtouch.model.*;
import com.healtouch.service.UserService;
import com.healtouch.service.Authorization;
import com.healtouch.util.Money;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.LocalDate;
import java.util.ArrayList;

public class TreatmentPage {
  private final AppServices services;
  private final UserSession session;
  private final ObservableList<TreatmentLine> lines = FXCollections.observableArrayList();
  private final TableView<TreatmentLine> table = new TableView<TreatmentLine>();
  private final Label total = new Label("¥ 0.00");
  private final Label patientNotice = new Label("请选择就诊患者");

  public TreatmentPage(AppServices services, UserSession session) {
    this.services = services;
    this.session = session;
  }

  public Parent node() {
    ComboBox<Patient> patient = new ComboBox<Patient>();
    patient.setPrefWidth(280);
    patient.setPromptText("选择就诊患者");

    ComboBox<UserService.Staff> therapist = new ComboBox<UserService.Staff>();
    therapist.setPrefWidth(200);
    therapist.setPromptText("选择主治治疗师");

    DatePicker date = new DatePicker(LocalDate.now());
    date.setPrefWidth(160);

    ComboBox<TreatmentProject> project = new ComboBox<TreatmentProject>();
    project.setPrefWidth(280);
    project.setPromptText("选择推拿理疗项目");

    Spinner<Integer> quantity = new Spinner<Integer>(1, 99, 1);
    quantity.setPrefWidth(80);

    patientNotice.setStyle("-fx-text-fill: #5E7F8E; -fx-font-size: 12px;");

    patient.valueProperty().addListener((obs, o, p) -> {
      if (p != null) {
        String typeStr = p.isChild() ? "小儿患者" : "成人患者";
        String extra = p.isChild() ? (" · 监护人：" + p.guardianName + " (" + p.guardianPhone + ")") : "";
        patientNotice.setText("就诊患者：" + p.name + " (" + typeStr + ")" + extra + " · 当前预存可用金：" + Money.format(p.balanceCents));
        patientNotice.setStyle("-fx-text-fill: #087F6D; -fx-font-weight: bold; -fx-font-size: 12px;");
      } else {
        patientNotice.setText("请选择就诊患者");
        patientNotice.setStyle("-fx-text-fill: #5E7F8E; -fx-font-size: 12px;");
      }
    });

    try {
      patient.setItems(FXCollections.observableArrayList(services.patients.search(session, "")));
      therapist.setItems(FXCollections.observableArrayList(services.users.activeTherapists()));
      project.setItems(FXCollections.observableArrayList(services.catalog.activeProjects()));
    } catch (Exception e) {
      Ui.error(e);
    }

    Button add = Ui.secondaryButton("＋ 加入治疗项目");
    add.setOnAction(e -> {
      try {
        TreatmentProject p = project.getValue();
        if (p == null) throw new IllegalArgumentException("请选择治疗项目");
        lines.add(new TreatmentLine(p.id, p.name, p.priceCents, quantity.getValue()));
        updateTotal();
      } catch (Exception ex) {
        Ui.error(ex);
      }
    });

    Button remove = new Button("移除选中项");
    remove.setOnAction(e -> {
      TreatmentLine sel = table.getSelectionModel().getSelectedItem();
      if (sel != null) {
        lines.remove(sel);
        updateTotal();
      }
    });

    TableColumn<TreatmentLine, String> pcol = col("推拿理疗项目", x -> x.projectName);
    pcol.setPrefWidth(240);
    TableColumn<TreatmentLine, String> price = col("基准单价", x -> Money.format(x.unitPriceCents));
    price.setPrefWidth(120);
    TableColumn<TreatmentLine, String> qty = col("频次/数量", x -> String.valueOf(x.quantity));
    qty.setPrefWidth(90);
    TableColumn<TreatmentLine, String> subtotal = col("小计金额", x -> Money.format(x.subtotalCents()));
    subtotal.setPrefWidth(140);
    subtotal.setStyle("-fx-font-weight: bold; -fx-text-fill: #087F6D;");

    table.getColumns().addAll(pcol, price, qty, subtotal);
    table.setItems(lines);
    table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    table.setPrefHeight(220);

    TextArea note = new TextArea();
    note.setPromptText("录入患者本次推拿调理重点及施术医嘱（可选）...");
    note.setPrefRowCount(2);

    total.setStyle("-fx-font-size: 20px; -fx-font-weight: 800; -fx-text-fill: #087F6D;");
    HBox totalBar = new HBox(12, remove, Ui.spacer(), new Label("应收总额合计："), total);
    totalBar.setAlignment(Pos.CENTER_RIGHT);
    totalBar.setStyle("-fx-padding: 8 16; -fx-background-color: #F0F8F6; -fx-background-radius: 8;");

    boolean canCharge = can(Permission.BILL_CHARGE);
    Button save = Ui.primaryButton(canCharge ? "生成账单并即时收费 (严禁欠费)" : "生成待收费账单");
    save.setStyle("-fx-font-size: 14px; -fx-padding: 10 24;");
    save.setOnAction(e -> {
      try {
        if (patient.getValue() == null || therapist.getValue() == null)
          throw new IllegalArgumentException("请选择就诊患者和主治治疗师");
        if (lines.isEmpty())
          throw new IllegalArgumentException("请至少添加一项治疗项目");

        long id = services.billing.createPendingBill(
            session,
            patient.getValue().id,
            date.getValue(),
            therapist.getValue().id,
            note.getText(),
            new ArrayList<TreatmentLine>(lines));

        long receivable = services.billing.receivable(session, id);
        if (canCharge) {
          showCharge(id, receivable, patient.getValue());
        } else {
          Ui.info("待收费账单已生成，请交由前台完成全额收款。");
        }
        lines.clear();
        updateTotal();
        note.clear();
      } catch (Exception ex) {
        Ui.error(ex);
      }
    });

    GridPane form = new GridPane();
    form.setHgap(12);
    form.setVgap(10);
    form.addRow(0, new Label("就诊患者 *:"), patient, new Label("就诊日期 *:"), date);
    form.addRow(1, new Label("主治治疗师 *:"), therapist, new Label("推拿项目 *:"), project, quantity, add);

    VBox formBox = Ui.section(new Label("登记信息与治疗师"), patientNotice, form);
    ((Label) formBox.getChildren().get(0)).setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #17384D;");

    VBox listBox = Ui.section(new Label("治疗项目明细"), table, totalBar);
    ((Label) listBox.getChildren().get(0)).setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #17384D;");

    HBox actionRow = new HBox(12, Ui.spacer(), save);
    actionRow.setAlignment(Pos.CENTER_RIGHT);

    return Ui.page("治疗登记与开单", "标准业务流程：选择患者 → 组合多项推拿理疗项目 → 选定主治治疗师 → 保存即生成待收费账单并立即结算",
        formBox, listBox, new Label("调理过程医嘱记录"), note, actionRow);
  }

  private TableColumn<TreatmentLine, String> col(
      String title, java.util.function.Function<TreatmentLine, String> value) {
    TableColumn<TreatmentLine, String> c = new TableColumn<TreatmentLine, String>(title);
    c.setCellValueFactory(x -> new SimpleStringProperty(value.apply(x.getValue())));
    return c;
  }

  private long sum() {
    long t = 0;
    for (TreatmentLine line : lines) t += line.subtotalCents();
    return t;
  }

  private void updateTotal() {
    total.setText(Money.format(sum()));
  }

  private void showCharge(long billId, long receivable, Patient patient) {
    Dialog<ButtonType> dialog = new Dialog<ButtonType>();
    dialog.setTitle("门诊收银结算");
    long balance = services.deposits.balance(session, patient.id);
    dialog.setHeaderText("应收总额：" + Money.format(receivable) + "  ·  患者可用预存余额：" + Money.format(balance));

    Label tip = new Label("【门诊收费规则】严禁欠费挂账，实收总额必须完全等于应收总额。\n系统已预填患者预存扣款，预存不足时请添加微信/支付宝或现金支付。");
    tip.setWrapText(true);
    tip.setStyle("-fx-text-fill: #668292; -fx-font-size: 12px;");

    ObservableList<PaymentLine> payments = FXCollections.observableArrayList();
    if (balance >= receivable) {
      payments.add(new PaymentLine(PaymentMethod.DEPOSIT, receivable));
    } else if (balance > 0) {
      payments.add(new PaymentLine(PaymentMethod.DEPOSIT, balance));
      payments.add(new PaymentLine(PaymentMethod.WECHAT, receivable - balance));
    } else {
      payments.add(new PaymentLine(PaymentMethod.WECHAT, receivable));
    }

    TableView<PaymentLine> paymentTable = new TableView<PaymentLine>();
    TableColumn<PaymentLine, String> method = colPay("支付渠道方式", p -> p.method.getLabel());
    method.setPrefWidth(160);
    TableColumn<PaymentLine, String> amount = colPay("实收金额", p -> Money.format(p.amountCents));
    amount.setPrefWidth(160);
    amount.setStyle("-fx-font-weight: bold; -fx-text-fill: #087F6D;");
    paymentTable.getColumns().addAll(method, amount);
    paymentTable.setItems(payments);
    paymentTable.setPrefHeight(150);

    ComboBox<PaymentMethod> paymentMethod =
        new ComboBox<PaymentMethod>(FXCollections.observableArrayList(PaymentMethod.values()));
    paymentMethod.setValue(PaymentMethod.WECHAT);

    TextField value = new TextField();
    value.setPromptText("输入金额 (元)");
    value.setPrefWidth(110);

    Button addPay = new Button("＋ 添加支付行");
    addPay.setOnAction(e -> {
      try {
        payments.add(new PaymentLine(paymentMethod.getValue(), Money.parse(value.getText())));
        value.clear();
      } catch (Exception exception) { Ui.error(exception); }
    });

    Button deletePay = new Button("移除选中行");
    deletePay.setOnAction(e -> {
      PaymentLine sel = paymentTable.getSelectionModel().getSelectedItem();
      if (sel != null) payments.remove(sel);
    });

    HBox payToolbar = new HBox(8, paymentMethod, value, addPay, deletePay);
    payToolbar.setAlignment(Pos.CENTER_LEFT);

    Label validationStatus = new Label();
    Runnable checkStatus = () -> {
      long paidSum = 0;
      for (PaymentLine p : payments) paidSum += p.amountCents;
      if (paidSum == receivable) {
        validationStatus.setText("✓ 支付总和 " + Money.format(paidSum) + " ＝ 应收金额，校验通过。");
        validationStatus.setStyle("-fx-text-fill: #16A34A; -fx-font-weight: bold; -fx-font-size: 12px;");
      } else {
        long diff = receivable - paidSum;
        validationStatus.setText("⚠ 差额 " + Money.format(diff) + " 未分配，必须全额收齐，禁止欠费。");
        validationStatus.setStyle("-fx-text-fill: #DC2626; -fx-font-weight: bold; -fx-font-size: 12px;");
      }
    };
    payments.addListener((javafx.collections.ListChangeListener<PaymentLine>) c -> checkStatus.run());
    checkStatus.run();

    VBox content = new VBox(12, tip, payToolbar, paymentTable, validationStatus);
    content.setPrefWidth(480);

    dialog.getDialogPane().setContent(content);
    dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
    Button ok = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
    ok.setText("确认收款入账");

    ok.addEventFilter(
        javafx.event.ActionEvent.ACTION,
        event -> {
          try {
            services.billing.checkout(session, billId, new ArrayList<PaymentLine>(payments));
            Ui.info("收款成功！账单已结清入账，预存资金扣减完成。");
          } catch (Exception exception) {
            event.consume();
            Ui.error(exception);
          }
        });
    dialog.showAndWait();
  }

  private boolean can(Permission permission) {
    try {
      Authorization.require(session, permission);
      return true;
    } catch (SecurityException e) {
      return false;
    }
  }

  private TableColumn<PaymentLine, String> colPay(
      String title, java.util.function.Function<PaymentLine, String> value) {
    TableColumn<PaymentLine, String> c = new TableColumn<PaymentLine, String>(title);
    c.setCellValueFactory(x -> new SimpleStringProperty(value.apply(x.getValue())));
    return c;
  }
}
