package com.healtouch.ui;

import com.healtouch.app.AppServices;
import com.healtouch.model.UserSession;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

public class LoginView {
    private final VBox root = new VBox(14);
    public LoginView(AppServices services, Consumer<UserSession> onLogin) {
        Label title=new Label("HealTouch");title.setStyle("-fx-font-size: 28px; -fx-font-weight: bold;");
        Label subtitle=new Label("推拿门诊管理系统");
        TextField account=new TextField();account.setPromptText("登录账号");account.setText("admin");
        PasswordField password=new PasswordField();password.setPromptText("登录密码");
        Label hint=new Label("首次启动账号：admin，初始密码：Admin@123（登录后必须修改）");hint.setWrapText(true);hint.setStyle("-fx-text-fill: #666;");
        Button login=new Button("登录");login.setDefaultButton(true);login.setMaxWidth(Double.MAX_VALUE);
        login.setOnAction(e->{try{UserSession session=services.auth.login(account.getText(),password.getText());if(session.mustChangePassword)changePassword(services,session,()->onLogin.accept(session));else onLogin.accept(session);}catch(Exception ex){Ui.error(ex);}});
        root.setAlignment(Pos.CENTER);root.setPadding(new Insets(40));root.getChildren().addAll(title,subtitle,new Label("账号"),account,new Label("密码"),password,login,hint);
    }
    public Parent node(){return root;}
    private void changePassword(AppServices services,UserSession session,Runnable done){Dialog<ButtonType> d=new Dialog<ButtonType>();d.setTitle("修改初始密码");d.setHeaderText("为保护数据安全，请先修改密码");PasswordField old=new PasswordField();PasswordField newer=new PasswordField();PasswordField confirm=new PasswordField();GridPane g=new GridPane();g.setHgap(8);g.setVgap(10);g.addRow(0,new Label("当前密码"),old);g.addRow(1,new Label("新密码"),newer);g.addRow(2,new Label("确认密码"),confirm);d.getDialogPane().setContent(g);d.getDialogPane().getButtonTypes().addAll(ButtonType.OK,ButtonType.CANCEL);d.setResultConverter(x->{if(x==ButtonType.OK){try{if(!newer.getText().equals(confirm.getText()))throw new IllegalArgumentException("两次输入的新密码不一致");services.auth.changePassword(session,old.getText(),newer.getText());done.run();}catch(Exception e){Ui.error(e);return null;}}return x;});d.showAndWait();}
}
