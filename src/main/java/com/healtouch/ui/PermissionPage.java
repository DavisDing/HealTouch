package com.healtouch.ui;

import com.healtouch.app.AppServices;
import com.healtouch.model.Permission;
import com.healtouch.model.RoleCode;
import com.healtouch.model.UserSession;
import javafx.collections.FXCollections;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.EnumSet;

public class PermissionPage {
    private final AppServices services; private final UserSession session;
    public PermissionPage(AppServices services,UserSession session){this.services=services;this.session=session;}
    public Parent node(){ComboBox<RoleCode> role=new ComboBox<RoleCode>(FXCollections.observableArrayList(RoleCode.values()));role.setValue(RoleCode.RECEPTION);ListView<Permission> permissions=new ListView<Permission>(FXCollections.observableArrayList(Permission.values()));permissions.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);Runnable load=()->{permissions.getSelectionModel().clearSelection();for(Permission p:services.permissions.permissionsFor(role.getValue())) permissions.getSelectionModel().select(p);};role.setOnAction(e->load.run());Button save=new Button("保存权限（立即生效）");save.setOnAction(e->{try{if(role.getValue()==RoleCode.ADMIN)throw new IllegalArgumentException("系统管理员为内置角色，始终拥有全部权限");EnumSet<Permission> selected=EnumSet.noneOf(Permission.class);selected.addAll(permissions.getSelectionModel().getSelectedItems());services.permissions.update(session,role.getValue(),selected);Ui.info("角色权限已保存，角色变更即时生效。");}catch(Exception ex){Ui.error(ex);}});load.run();return Ui.page("权限管理",new HBox(10,new Label("角色"),role),new Label("勾选该角色可使用的操作权限："),permissions,save);}
}
