package com.healtouch.ui;

import com.healtouch.app.AppServices;
import com.healtouch.model.*;
import com.healtouch.service.UserService;
import com.healtouch.util.Money;
import java.time.LocalDate;
import java.util.ArrayList;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class TreatmentPage {
  private final AppServices services;
  private final UserSession session;
  private final ObservableList<TreatmentLine> lines = FXCollections.observableArrayList();
  private final TableView<TreatmentLine> table = new TableView<TreatmentLine>();
  private final Label total = new Label("¥0.00");

  public TreatmentPage(AppServices services, UserSession session) {
    this.services = services;
    this.session = session;
  }

  public Parent node() {
    ComboBox<Patient> patient = new ComboBox<Patient>();
    patient.setPrefWidth(260);
    ComboBox<UserService.Staff> therapist = new ComboBox<UserService.Staff>();
    therapist.setPrefWidth(180);
    ComboBox<TreatmentProject> project = new ComboBox<TreatmentProject>();
    project.setPrefWidth(260);
    Spinner<Integer> quantity = new Spinner<Integer>(1, 99, 1);
    DatePicker date = new DatePicker(LocalDate.now());
    TextArea note = new TextArea();
    note.setPromptText("治疗过程备注（可选）");
    note.setPrefRowCount(2);
    try {
      patient.setItems(FXCollections.observableArrayList(services.patients.search(session, "")));
      therapist.setItems(FXCollections.observableArrayList(services.users.activeTherapists()));
      project.setItems(FXCollections.observableArrayList(services.catalog.activeProjects()));
    } catch (Exception e) {
      Ui.error(e);
    }
    Button add = new Button("添加项目");
    add.setOnAction(
        e -> {
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
    remove.setOnAction(
        e -> {
          lines.remove(table.getSelectionModel().getSelectedItem());
          updateTotal();
        });
    TableColumn<TreatmentLine, String> pcol = col("项目", x -> x.projectName);
    TableColumn<TreatmentLine, String> price = col("单价", x -> Money.format(x.unitPriceCents));
    TableColumn<TreatmentLine, String> qty = col("次数", x -> String.valueOf(x.quantity));
    TableColumn<TreatmentLine, String> subtotal = col("小计", x -> Money.format(x.subtotalCents()));
    table.getColumns().addAll(pcol, price, qty, subtotal);
    table.setItems(lines);
    table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    VBox.setVgrow(table, Priority.ALWAYS);
    Button save = new Button("保存并收费");
    save.setOnAction(
        e -> {
          try {
            if (patient.getValue() == null || therapist.getValue() == null)
              throw new IllegalArgumentException("请选择患者和治疗师");
            long id =
                services.billing.createPendingBill(
                    session,
                    patient.getValue().id,
                    date.getValue(),
                    therapist.getValue().id,
                    note.getText(),
                    new ArrayList<TreatmentLine>(lines));
            showCharge(id, sum(), patient.getValue());
            lines.clear();
            updateTotal();
            Ui.info("账单草稿已创建，请完成收费。");
          } catch (Exception ex) {
            Ui.error(ex);
          }
        });
    GridPane form = new GridPane();
    form.setHgap(10);
    form.setVgap(8);
    form.addRow(0, new Label("患者"), patient, new Label("治疗日期"), date);
    form.addRow(1, new Label("治疗师"), therapist, new Label("治疗项目"), project, quantity, add);
    VBox page =
        Ui.page(
            "治疗登记",
            form,
            Ui.toolbar(remove, new Label("应收总额："), total),
            table,
            new Label("治疗过程备注"),
            note,
            save);
    return page;
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
    dialog.setTitle("账单收费");
    long balance = services.deposits.balance(session, patient.id);
    dialog.setHeaderText(
        "应收：" + Money.format(receivable) + "  ·  当前预存：" + Money.format(balance));
    Label tip = new Label("默认使用预存扣款；预存不足时可移除或调整支付行后补充其他支付方式。");
    tip.setWrapText(true);
    tip.setStyle("-fx-text-fill: #56737e;");
    ObservableList<PaymentLine> payments = FXCollections.observableArrayList();
    payments.add(new PaymentLine(PaymentMethod.DEPOSIT, receivable));
    TableView<PaymentLine> paymentTable = new TableView<PaymentLine>();
    TableColumn<PaymentLine, String> method = colPay("支付方式", p -> p.method.getLabel());
    TableColumn<PaymentLine, String> amount = colPay("金额", p -> Money.format(p.amountCents));
    paymentTable.getColumns().addAll(method, amount);
    paymentTable.setItems(payments);
    paymentTable.setPrefHeight(160);
    ComboBox<PaymentMethod> paymentMethod =
        new ComboBox<PaymentMethod>(FXCollections.observableArrayList(PaymentMethod.values()));
    paymentMethod.setValue(PaymentMethod.DEPOSIT);
    TextField value = new TextField(String.format("%.2f", receivable / 100.0));
    value.setPromptText("金额");
    Button add = new Button("添加支付行");
    add.setOnAction(
        e -> {
          try {
            payments.add(new PaymentLine(paymentMethod.getValue(), Money.parse(value.getText())));
            value.clear();
          } catch (Exception exception) { Ui.error(exception); }
        });
    Button replace = new Button("替换选中行");
    replace.setOnAction(
        e -> {
          try {
            int selected = paymentTable.getSelectionModel().getSelectedIndex();
            if (selected < 0) throw new IllegalArgumentException("请选择要替换的支付行");
            payments.set(selected, new PaymentLine(paymentMethod.getValue(), Money.parse(value.getText())));
          } catch (Exception exception) { Ui.error(exception); }
        });
    Button delete = new Button("移除选中行");
    delete.setOnAction(e -> payments.remove(paymentTable.getSelectionModel().getSelectedItem()));
    dialog.getDialogPane().setContent(new VBox(10, tip, new HBox(8, paymentMethod, value, add, replace, delete), paymentTable));
    dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
    Button ok = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
    ok.addEventFilter(
        javafx.event.ActionEvent.ACTION,
        event -> {
          try {
            services.billing.checkout(session, billId, new ArrayList<PaymentLine>(payments));
            Ui.info("收费成功，预存扣款已同步入账，治疗记录已完成。");
          } catch (Exception exception) {
            event.consume();
            Ui.error(exception);
          }
        });
    dialog.showAndWait();
  }

  private TableColumn<PaymentLine, String> colPay(
      String title, java.util.function.Function<PaymentLine, String> value) {
    TableColumn<PaymentLine, String> c = new TableColumn<PaymentLine, String>(title);
    c.setCellValueFactory(x -> new SimpleStringProperty(value.apply(x.getValue())));
    return c;
  }
}
