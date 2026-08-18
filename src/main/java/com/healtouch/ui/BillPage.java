package com.healtouch.ui;

import com.healtouch.app.AppServices;
import com.healtouch.model.*;
import com.healtouch.util.Money;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class BillPage {
    private final AppServices services; private final UserSession session;private final TableView<BillSummary> table=new TableView<BillSummary>();private final TextField keyword=new TextField();private final ComboBox<BillStatus> status=new ComboBox<BillStatus>(FXCollections.observableArrayList(BillStatus.values()));
    public BillPage(AppServices services,UserSession session){this.services=services;this.session=session;}
    public Parent node(){keyword.setPromptText("患者、证件号码或账单号");status.setPromptText("全部状态");Button search=new Button("查询");search.setOnAction(e->load());Button voidBill=new Button("作废待收费账单");voidBill.setOnAction(e->{BillSummary b=table.getSelectionModel().getSelectedItem();try{if(b==null)throw new IllegalArgumentException("请选择账单");if(Ui.confirm("确认作废账单 "+b.billCode+"？")){services.billing.voidPending(session,b.id,"前台主动取消");load();}}catch(Exception ex){Ui.error(ex);}});Button refund=new Button("申请退款");refund.setOnAction(e->refund());table.getColumns().addAll(column("账单号",x->x.billCode),column("患者",x->x.patientName),column("日期",x->String.valueOf(x.treatmentDate)),column("治疗师",x->x.therapistName),column("状态",x->statusLabel(x.status)),column("应收",x->Money.format(x.receivableCents)),column("已退款",x->Money.format(x.refundedCents)));table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);VBox.setVgrow(table,Priority.ALWAYS);VBox page=Ui.page("账单管理",Ui.toolbar(keyword,status,search,voidBill,refund),table);load();return page;}
    private void load(){try{services.billing.voidExpiredPending();table.setItems(FXCollections.observableArrayList(services.billing.list(keyword.getText(),status.getValue())));}catch(Exception e){Ui.error(e);}}
    private void refund(){BillSummary b=table.getSelectionModel().getSelectedItem();if(b==null){Ui.error(new IllegalArgumentException("请选择账单"));return;}TextInputDialog d=new TextInputDialog();d.setTitle("申请账单退款");d.setHeaderText(b.billCode+"，可退款："+Money.format(b.paidCents-b.refundedCents));d.setContentText("退款金额：");d.showAndWait().ifPresent(v->{try{long id=services.refunds.request(session,b.id,Money.parse(v),"界面申请退款",null);Ui.info("退款申请已提交，编号："+id);load();}catch(Exception e){Ui.error(e);}});}
    private TableColumn<BillSummary,String> column(String title,java.util.function.Function<BillSummary,String> getter){TableColumn<BillSummary,String> c=new TableColumn<BillSummary,String>(title);c.setCellValueFactory(x->new SimpleStringProperty(getter.apply(x.getValue())));return c;}
    private String statusLabel(BillStatus s){switch(s){case PENDING_PAYMENT:return "待收费";case PAID:return "已支付";case PARTIALLY_REFUNDED:return "部分退款";case REFUNDED:return "已退款";default:return "已作废";}}
}
