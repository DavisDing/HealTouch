package com.healtouch.ui;

import com.healtouch.app.AppServices;
import com.healtouch.model.Patient;
import com.healtouch.model.PaymentMethod;
import com.healtouch.model.UserSession;
import com.healtouch.util.Money;
import javafx.collections.FXCollections;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

public class DepositPage {
  private final AppServices services;
  private final UserSession session;
  private final Label balance = new Label("请选择患者");

  public DepositPage(AppServices services, UserSession session) {
    this.services = services;
    this.session = session;
  }

  public Parent node() {
    ComboBox<Patient> patient = new ComboBox<Patient>();
    patient.setPrefWidth(300);
    try {
      patient.setItems(FXCollections.observableArrayList(services.patients.search(session, "")));
    } catch (Exception e) {
      Ui.error(e);
    }
    patient.setOnAction(e -> refresh(patient.getValue()));
    TextField amount = new TextField();
    amount.setPromptText("金额，例如 100.00");
    ComboBox<PaymentMethod> method =
        new ComboBox<PaymentMethod>(
            FXCollections.observableArrayList(
                PaymentMethod.CASH,
                PaymentMethod.WECHAT,
                PaymentMethod.ALIPAY,
                PaymentMethod.BANK_CARD,
                PaymentMethod.OTHER));
    method.setValue(PaymentMethod.CASH);
    Button recharge = new Button("确认充值");
    recharge.setOnAction(
        e -> {
          try {
            if (patient.getValue() == null) throw new IllegalArgumentException("请选择患者");
            services.deposits.recharge(
                session,
                patient.getValue().id,
                Money.parse(amount.getText()),
                method.getValue(),
                "界面充值");
            refresh(patient.getValue());
            amount.clear();
            Ui.info("预存充值成功");
          } catch (Exception ex) {
            Ui.error(ex);
          }
        });
    Button refund = new Button("余额退款");
    refund.setOnAction(
        e -> {
          try {
            if (patient.getValue() == null) throw new IllegalArgumentException("请选择患者");
            if (!Ui.confirm("确认从该患者预存余额中退款？")) return;
            services.deposits.refundBalance(
                session,
                patient.getValue().id,
                Money.parse(amount.getText()),
                method.getValue(),
                "界面余额退款");
            refresh(patient.getValue());
            amount.clear();
            Ui.info("余额退款已完成");
          } catch (Exception ex) {
            Ui.error(ex);
          }
        });
    return Ui.page(
        "预存管理",
        new HBox(10, new Label("患者"), patient, balance),
        new HBox(10, new Label("金额"), amount, new Label("支付/退款方式"), method, recharge, refund));
  }

  private void refresh(Patient patient) {
    try {
      balance.setText("当前余额：" + Money.format(services.deposits.balance(session, patient.id)));
    } catch (Exception e) {
      Ui.error(e);
    }
  }
}
