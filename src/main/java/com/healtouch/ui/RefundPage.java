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
  private final AppServices services;
  private final UserSession session;
  private final TableView<RefundSummary> table = new TableView<RefundSummary>();

  public RefundPage(AppServices services, UserSession session) {
    this.services = services;
    this.session = session;
  }

  public Parent node() {
    Button approve = Ui.primaryButton("✓ 审批通过");
    approve.setOnAction(e -> decide(true));

    Button reject = Ui.dangerButton("✕ 驳回申请");
    reject.setOnAction(e -> decide(false));

    Button execute = Ui.secondaryButton("⚡ 执行冲正退款");
    execute.setOnAction(
        e -> {
          try {
            RefundSummary r = selected();
            DbWork.write(services.refunds::execute, session, r.id);
            load();
            Ui.info("退款冲正已成功执行，相关预存资金流水已自动冲销入账！");
          } catch (Exception x) {
            Ui.error(x);
          }
        });

    Button refresh = new Button("刷新列表");
    refresh.setOnAction(e -> load());

    Button revoke=new Button("撤销申请");
    revoke.setOnAction(e->{try {DbWork.write(services.refunds::revoke,session,selected().id,"操作者撤销退款申请");load();}catch(Exception error){Ui.error(error);}});
    Button details=new Button("查看申请与审批意见");
    details.setOnAction(e->{try{RefundSummary r=selected();Ui.info("申请理由："+r.reason+"\n申请备注："+empty(r.applicantRemark)+"\n审批意见："+empty(r.approvalRemark)+"\n撤销原因："+empty(r.revokeReason));}catch(Exception error){Ui.error(error);}});
    HBox tb = Ui.toolbar(refresh, details, Ui.spacer(), approve, reject, revoke, execute);

    TableColumn<RefundSummary, String> codeCol = col("退款申请单号", x -> x.refundCode);
    codeCol.setPrefWidth(150);
    codeCol.setStyle("-fx-font-weight: bold;");

    TableColumn<RefundSummary, String> billCol = col("原就诊账单", x -> x.billCode);
    billCol.setPrefWidth(140);

    TableColumn<RefundSummary, String> patientCol = col("就诊患者", x -> x.patientName);
    patientCol.setPrefWidth(110);

    TableColumn<RefundSummary, String> amountCol = col("申请退款金额", x -> Money.format(x.amountCents));
    amountCol.setPrefWidth(130);
    amountCol.setStyle("-fx-font-weight: bold; -fx-text-fill: #DC2626;");

    TableColumn<RefundSummary, String> statusCol = col("审批状态", x -> status(x.status));
    statusCol.setPrefWidth(110);
    statusCol.setCellFactory(c -> new TableCell<RefundSummary, String>() {
      @Override
      protected void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
          setText(null);
          setGraphic(null);
        } else {
          setText(item);
          if ("待审批".equals(item)) {
            setStyle("-fx-text-fill: #D97706; -fx-font-weight: bold;");
          } else if ("已审批".equals(item) || "已执行".equals(item)) {
            setStyle("-fx-text-fill: #087F6D; -fx-font-weight: bold;");
          } else if ("已驳回".equals(item)) {
            setStyle("-fx-text-fill: #DC2626; -fx-font-weight: bold;");
          }
        }
      }
    });

    TableColumn<RefundSummary, String> applicantCol = col("申请人 / 经手人", x -> x.applicantName);
    applicantCol.setPrefWidth(130);

    TableColumn<RefundSummary, String> approverCol = col("最终审批人", x -> x.approverName == null ? "待审批" : x.approverName);
    approverCol.setPrefWidth(130);

    table.getColumns().addAll(
        codeCol,
        billCol,
        patientCol,
        amountCol,
        statusCol,
        applicantCol,
        approverCol);

    table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    VBox.setVgrow(table, Priority.ALWAYS);

    VBox page = Ui.page(
        "退款审批与冲正管理",
        "具备退款权限的角色可申请 · 申请人不得自审 · 审批后按原支付方式退款 · 全流程审计留痕",
        tb,
        table);
    load();
    return page;
  }

  private void decide(boolean approve) {
    try {
      RefundSummary r = selected();
      TextInputDialog comment=new TextInputDialog(approve?"审核通过":"");
      comment.setTitle(approve?"审批通过":"驳回申请");comment.setHeaderText("申请理由："+r.reason+"\n申请备注："+empty(r.applicantRemark));comment.setContentText("审批意见：");
      java.util.Optional<String> opinion=Ui.show(comment);if(!opinion.isPresent())return;
      DbWork.write(services.refunds::approve, session, r.id, approve, opinion.get());
      load();
      Ui.info(approve ? "退款申请已批准！请在核验后点击【执行冲正退款】完成资金原路冲正。" : "已成功驳回该退款申请。");
    } catch (Exception e) {
      Ui.error(e);
    }
  }

  private RefundSummary selected() {
    RefundSummary r = table.getSelectionModel().getSelectedItem();
    if (r == null) throw new IllegalArgumentException("请在列表中选择需要操作的退款单据");
    return r;
  }
  private String empty(String value){return value==null||value.trim().isEmpty()?"无":value;}

  private void load() {
    try {
      table.setItems(FXCollections.observableArrayList(DbWork.read(services.refunds::list, session)));
    } catch (Exception e) {
      Ui.error(e);
    }
  }

  private TableColumn<RefundSummary, String> col(
      String title, java.util.function.Function<RefundSummary, String> getter) {
    TableColumn<RefundSummary, String> c = new TableColumn<RefundSummary, String>(title);
    c.setCellValueFactory(x -> new SimpleStringProperty(getter.apply(x.getValue())));
    return c;
  }

  private String status(String s) {
    if ("PENDING_APPROVAL".equals(s)) return "待审批";
    if ("APPROVED".equals(s)) return "已审批";
    if ("EXECUTED".equals(s)) return "已执行";
    if ("REVOKED".equals(s)) return "已撤销";
    if ("REJECTED".equals(s)) return "已驳回";
    return s;
  }
}
