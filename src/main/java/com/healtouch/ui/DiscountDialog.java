package com.healtouch.ui;

import com.healtouch.app.AppServices;
import com.healtouch.model.*;
import com.healtouch.util.Money;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

final class DiscountDialog {
  enum Result { NONE, APPLIED, REQUESTED, REJECTED }
  private DiscountDialog() {}
  static Result show(AppServices services,UserSession session,BillSummary bill) {
    if(bill==null||bill.status!=BillStatus.PENDING_PAYMENT)throw new IllegalArgumentException("请选择待收费账单");
    boolean admin=session.can(Permission.DISCOUNT_APPLY);
    Dialog<Result> dialog=new Dialog<Result>();dialog.setTitle("折扣申请与授权");
    dialog.setHeaderText(DbWork.read(services.billing::pendingDiscount,session,bill.id));
    TextField amount=new TextField();Ui.moneyField(amount);amount.setPromptText("减免金额（元），折后必须大于0");
    TextField reason=new TextField();reason.setPromptText("折扣原因（必填）");
    dialog.getDialogPane().setContent(new VBox(10,new Label("账单："+bill.billCode+"；当前应收："+Money.format(bill.receivableCents)),amount,reason));
    ButtonType reject=new ButtonType("驳回折扣申请",ButtonBar.ButtonData.OTHER);
    dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK,ButtonType.CANCEL);
    if(admin)dialog.getDialogPane().getButtonTypes().add(reject);
    Result[] outcome={Result.NONE};
    dialog.setResultConverter(type->outcome[0]);
    Button ok=(Button)dialog.getDialogPane().lookupButton(ButtonType.OK);ok.setText(admin?"授权折扣":"提交申请");
    ok.addEventFilter(javafx.event.ActionEvent.ACTION,event->{try {
      long cents=Money.parse(amount.getText());
      if(admin) {DbWork.write(services.billing::applyDiscount,session,bill.id,cents,reason.getText());outcome[0]=Result.APPLIED;}
      else {DbWork.read(services.billing::requestDiscount,session,bill.id,cents,reason.getText());outcome[0]=Result.REQUESTED;}
    }catch(Exception error){event.consume();Ui.error(error);}});
    if(admin)dialog.getDialogPane().lookupButton(reject).addEventFilter(javafx.event.ActionEvent.ACTION,event->{try {
      DbWork.write(services.billing::rejectDiscount,session,bill.id);outcome[0]=Result.REJECTED;
    }catch(Exception error){event.consume();Ui.error(error);}});
    return Ui.show(dialog).orElse(Result.NONE);
  }
}
