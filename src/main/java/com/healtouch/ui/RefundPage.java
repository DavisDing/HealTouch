package com.healtouch.ui;

import com.healtouch.app.AppServices;
import com.healtouch.model.RefundSummary;
import com.healtouch.model.UserSession;
import com.healtouch.util.Money;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class RefundPage {
    private final AppServices services; private final UserSession session; private final TableView<RefundSummary> table=new TableView<RefundSummary>();
    public RefundPage(AppServices services,UserSession session){this.services=services;this.session=session;}
    public Parent node(){Button approve=new Button("审批通过");approve.setOnAction(e->decide(true));Button reject=new Button("驳回");reject.setOnAction(e->decide(false));Button execute=new Button("执行退款");execute.setOnAction(e->{try{RefundSummary r=selected();services.refunds.execute(session,r.id);load();Ui.info("退款已执行，相关预存流水已生成。");}catch(Exception x){Ui.error(x);}});table.getColumns().addAll(col("退款单",x->x.refundCode),col("账单",x->x.billCode),col("患者",x->x.patientName),col("金额",x->Money.format(x.amountCents)),col("状态",x->status(x.status)),col("申请人",x->x.applicantName),col("审批人",x->x.approverName==null?"":x.approverName));table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);VBox.setVgrow(table,Priority.ALWAYS);VBox page=Ui.page("退款审批",new HBox(8,approve,reject,execute),table);load();return page;}
    private void decide(boolean approve){try{RefundSummary r=selected();services.refunds.approve(session,r.id,approve,approve?"审批通过":"审批驳回");load();Ui.info(approve?"已审批通过":"已驳回退款申请");}catch(Exception e){Ui.error(e);}}
    private RefundSummary selected(){RefundSummary r=table.getSelectionModel().getSelectedItem();if(r==null)throw new IllegalArgumentException("请选择退款单");return r;}
    private void load(){try{table.setItems(FXCollections.observableArrayList(services.refunds.list()));}catch(Exception e){Ui.error(e);}}
    private TableColumn<RefundSummary,String> col(String title,java.util.function.Function<RefundSummary,String> getter){TableColumn<RefundSummary,String> c=new TableColumn<RefundSummary,String>(title);c.setCellValueFactory(x->new SimpleStringProperty(getter.apply(x.getValue())));return c;}
    private String status(String s){if("PENDING_APPROVAL".equals(s))return "待审批";if("APPROVED".equals(s))return "已审批";if("EXECUTED".equals(s))return "已执行";if("REJECTED".equals(s))return "已驳回";return s;}
}
