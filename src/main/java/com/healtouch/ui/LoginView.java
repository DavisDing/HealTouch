package com.healtouch.ui;

import com.healtouch.app.AppServices;
import com.healtouch.model.UserSession;
import java.util.function.Consumer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class LoginView {
  private final VBox root = new VBox(18);

  public LoginView(AppServices services, Consumer<UserSession> onLogin) {
    HBox brand = Ui.brand();
    Label subtitle = new Label("安心、清晰地管理每一次诊疗服务");
    subtitle.setStyle("-fx-text-fill: #67818d;");
    VBox intro = new VBox(8, brand, subtitle);
    intro.setAlignment(Pos.CENTER_LEFT);
    VBox card = Ui.section();
    card.setMaxWidth(400);
    root.setAlignment(Pos.CENTER);
    root.setPadding(new Insets(42));
    root.getChildren().addAll(intro, card);
    if (services.auth.requiresInitialAdministratorSetup()) {
      createInitialAdministratorForm(services, onLogin, card);
    } else {
      createLoginForm(services, onLogin, card);
    }
  }

  public Parent node() { return root; }

  private void createInitialAdministratorForm(
      AppServices services, Consumer<UserSession> onLogin, VBox card) {
    Label heading = new Label("首次启动：创建系统管理员");
    heading.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
    Label instruction = new Label("请为管理员账号 admin 设置仅由您保存的密码。");
    instruction.setWrapText(true);
    instruction.setStyle("-fx-text-fill: #667f8c;");
    PasswordField password = new PasswordField();
    password.setPromptText("设置管理员密码");
    PasswordField confirmation = new PasswordField();
    confirmation.setPromptText("再次输入管理员密码");
    Button create = Ui.primaryButton("创建管理员并登录");
    create.setDefaultButton(true);
    create.setMaxWidth(Double.MAX_VALUE);
    create.setOnAction(
        event -> {
          try {
            if (!password.getText().equals(confirmation.getText()))
              throw new IllegalArgumentException("两次输入的密码不一致");
            services.auth.initializeAdministrator(password.getText());
            onLogin.accept(services.auth.login("admin", password.getText()));
          } catch (Exception exception) {
            Ui.error(exception);
          }
        });
    card.getChildren().addAll(heading, instruction, new Label("管理员密码"), password, new Label("确认密码"), confirmation, create);
  }

  private void createLoginForm(AppServices services, Consumer<UserSession> onLogin, VBox card) {
    Label heading = new Label("欢迎回来");
    heading.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
    Label description = new Label("使用您的工作账号登录系统");
    description.setStyle("-fx-text-fill: #667f8c;");
    TextField account = new TextField();
    account.setPromptText("登录账号");
    PasswordField password = new PasswordField();
    password.setPromptText("登录密码");
    Button login = Ui.primaryButton("登录系统");
    login.setDefaultButton(true);
    login.setMaxWidth(Double.MAX_VALUE);
    login.setOnAction(
        event -> {
          try { onLogin.accept(services.auth.login(account.getText(), password.getText())); }
          catch (Exception exception) { Ui.error(exception); }
        });
    Button changePassword = new Button("修改密码");
    changePassword.setOnAction(event -> openChangePassword(services));
    card.getChildren().addAll(heading, description, new Label("账号"), account, new Label("密码"), password, login, changePassword);
  }

  private void openChangePassword(AppServices services) {
    Dialog<ButtonType> dialog = new Dialog<ButtonType>();
    dialog.setTitle("修改密码");
    TextField account = new TextField();
    PasswordField oldPassword = new PasswordField();
    PasswordField newPassword = new PasswordField();
    GridPane form = new GridPane();
    form.setVgap(8);
    form.setHgap(10);
    form.addRow(0, new Label("账号"), account);
    form.addRow(1, new Label("原密码"), oldPassword);
    form.addRow(2, new Label("新密码"), newPassword);
    dialog.getDialogPane().setContent(form);
    dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
    dialog.showAndWait().ifPresent(
        button -> {
          if (button == ButtonType.OK) {
            try { services.auth.changePassword(account.getText(), oldPassword.getText(), newPassword.getText()); Ui.info("密码已修改，请使用新密码登录。"); }
            catch (Exception exception) { Ui.error(exception); }
          }
        });
  }
}
