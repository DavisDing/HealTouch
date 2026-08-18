package com.healtouch.ui;

import com.healtouch.app.AppServices;
import com.healtouch.model.Permission;
import com.healtouch.model.UserSession;
import com.healtouch.service.Authorization;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class MainView {
    private final BorderPane root = new BorderPane();
    private final AppServices services; private final UserSession session;
    public MainView(AppServices services, UserSession session, Runnable logout) {
        this.services=services;this.session=session;
        Label brand=new Label("HealTouch");brand.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
        Label who=new Label(session.name+"  ["+session.roles+"]");Button exit=new Button("退出登录");exit.setOnAction(e->logout.run());Region spacer=new Region();HBox.setHgrow(spacer,Priority.ALWAYS);HBox header=new HBox(14,brand,spacer,who,exit);header.setPadding(new Insets(12,18,12,18));header.setStyle("-fx-background-color: #f3f6f8;");root.setTop(header);
        VBox nav=new VBox(2);nav.setPadding(new Insets(12));nav.setPrefWidth(180);nav.setStyle("-fx-background-color: #fbfbfb;");
        add(nav,"首页",()->show(new DashboardPage(services,session,this).node()));
        if(can(Permission.PATIENT_VIEW)) add(nav,"患者管理",()->show(new PatientPage(services,session).node()));
        if(can(Permission.TREATMENT_CREATE)) add(nav,"治疗登记",()->show(new TreatmentPage(services,session).node()));
        if(can(Permission.BILL_CHARGE)||can(Permission.BILL_REFUND)) add(nav,"账单管理",()->show(new BillPage(services,session).node()));
        if(can(Permission.BILL_REFUND)) add(nav,"退款审批",()->show(new RefundPage(services,session).node()));
        if(can(Permission.DEPOSIT_RECHARGE)||can(Permission.DEPOSIT_REFUND)) add(nav,"预存管理",()->show(new DepositPage(services,session).node()));
        if(can(Permission.REPORT_VIEW)) add(nav,"数据统计",()->show(new ReportPage(services,session).node()));
        if(can(Permission.SYSTEM_MANAGE)) { Separator sep=new Separator();nav.getChildren().add(sep);Label label=new Label("系统管理");label.setStyle("-fx-font-weight: bold; -fx-padding: 10 8 4 8;");nav.getChildren().add(label);add(nav,"用户管理",()->show(new UserPage(services,session).node()));add(nav,"权限管理",()->show(new PermissionPage(services,session).node()));add(nav,"治疗项目管理",()->show(new CatalogPage(services,session).node())); }
        root.setLeft(nav);show(new DashboardPage(services,session,this).node());
    }
    public Parent node(){return root;}
    public void show(Parent page){root.setCenter(page);}
    public void openPatients(){show(new PatientPage(services,session).node());}
    public void openTreatment(){show(new TreatmentPage(services,session).node());}
    public void openBills(){show(new BillPage(services,session).node());}
    public void openDeposits(){show(new DepositPage(services,session).node());}
    private boolean can(Permission p){try{Authorization.require(session,p);return true;}catch(SecurityException e){return false;}}
    private void add(VBox box,String text,Runnable action){Button b=Ui.navButton(text);b.setOnAction(e->action.run());box.getChildren().add(b);}
}
