package com.healtouch.ui;

import com.healtouch.app.AppServices;
import com.healtouch.model.RoleCode;
import com.healtouch.model.UserSession;
import com.healtouch.service.UserService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.EnumSet;

public class UserPage {
    private final AppServices services; private final UserSession session;private final TableView<UserService.Staff> table=new TableView<UserService.Staff>();
    public UserPage(AppServices services,UserSession session){this.services=services;this.session=session;}
    public Parent node(){Button add=new Button("新增用户");add.setOnAction(e->add());Button toggle=new Button("在职/停用");toggle.setOnAction(e->{try{UserService.Staff s=table.getSelectionModel().getSelectedItem();if(s==null)throw new IllegalArgumentException("请选择用户");services.users.setActive(session,s.id,!s.active);load();}catch(Exception ex){Ui.error(ex);}});table.getColumns().addAll(col("姓名",x->x.name),col("账号",x->x.loginName),col("角色",x->x.roles.toString()),col("状态",x->x.active?"在职":"停用"));table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);VBox.setVgrow(table,Priority.ALWAYS);VBox page=Ui.page("用户管理",new HBox(10,add,toggle),table);load();return page;}
    private TableColumn<UserService.Staff,String> col(String label,java.util.function.Function<UserService.Staff,String> getter){TableColumn<UserService.Staff,String> c=new TableColumn<UserService.Staff,String>(label);c.setCellValueFactory(x->new SimpleStringProperty(getter.apply(x.getValue())));return c;}
    private void load(){try{table.setItems(FXCollections.observableArrayList(services.users.listAll()));}catch(Exception e){Ui.error(e);}}
    private void add(){Dialog<ButtonType> d=new Dialog<ButtonType>();d.setTitle("新增用户");TextField name=new TextField();TextField account=new TextField();TextField phone=new TextField();PasswordField password=new PasswordField();password.setText("User@123");ListView<RoleCode> roles=new ListView<RoleCode>(FXCollections.observableArrayList(RoleCode.values()));roles.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);roles.setPrefHeight(120);d.getDialogPane().setContent(new VBox(8,new Label("姓名"),name,new Label("登录账号"),account,new Label("手机号"),phone,new Label("初始密码"),password,new Label("角色（可多选）"),roles));d.getDialogPane().getButtonTypes().addAll(ButtonType.OK,ButtonType.CANCEL);d.setResultConverter(b->{if(b==ButtonType.OK){try{EnumSet<RoleCode> set=EnumSet.noneOf(RoleCode.class);set.addAll(roles.getSelectionModel().getSelectedItems());services.users.create(session,name.getText(),account.getText(),phone.getText(),password.getText(),set);load();Ui.info("用户已创建，首次登录需修改密码。");}catch(Exception e){Ui.error(e);return null;}}return b;});d.showAndWait();}
}
