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
import javafx.scene.layout.VBox;

public class LoginView {
  private final VBox root = new VBox(14);

  public LoginView(AppServices services, Consumer<UserSession> onLogin) {
    Label title = new Label("HealTouch");
    title.setStyle("-fx-font-size: 28px; -fx-font-weight: bold;");
    Label subtitle = new Label("推拿门诊管理系统");

    root.setAlignment(Pos.CENTER);
    root.setPadding(new Insets(40));
    root.getChildren().addAll(title, subtitle);
    if (services.auth.requiresInitialAdministratorSetup()) {
      createInitialAdministratorForm(services, onLogin);
    } else {
      createLoginForm(services, onLogin);
    }
  }

  public Parent node() {
    return root;
  }

  private void createInitialAdministratorForm(AppServices services, Consumer<UserSession> onLogin) {
    Label heading = new Label("首次启动：创建系统管理员");
    Label instruction = new Label("请为管理员账号 admin 设置仅由您保存的密码。");
    instruction.setWrapText(true);
    instruction.setStyle("-fx-text-fill: #666;");
    PasswordField password = new PasswordField();
    password.setPromptText("设置管理员密码");
    PasswordField confirmation = new PasswordField();
    confirmation.setPromptText("再次输入管理员密码");
    Button create = new Button("创建管理员并登录");
    create.setDefaultButton(true);
    create.setMaxWidth(Double.MAX_VALUE);
    create.setOnAction(
        event -> {
          try {
            if (!password.getText().equals(confirmation.getText())) {
              throw new IllegalArgumentException("两次输入的密码不一致");
            }
            services.auth.initializeAdministrator(password.getText());
            onLogin.accept(services.auth.login("admin", password.getText()));
          } catch (Exception exception) {
            Ui.error(exception);
          }
        });
    root.getChildren()
        .addAll(
            heading,
            instruction,
            new Label("管理员密码"),
            password,
            new Label("确认密码"),
            confirmation,
            create);
  }

  private void createLoginForm(AppServices services, Consumer<UserSession> onLogin) {
    TextField account = new TextField();
    account.setPromptText("登录账号");
    PasswordField password = new PasswordField();
    password.setPromptText("登录密码");
    Button login = new Button("登录");
    login.setDefaultButton(true);
    login.setMaxWidth(Double.MAX_VALUE);
    login.setOnAction(
        event -> {
          try {
            UserSession session = services.auth.login(account.getText(), password.getText());
            if (session.mustChangePassword) {
              changePassword(services, session, () -> onLogin.accept(session));
            } else {
              onLogin.accept(session);
            }
          } catch (Exception exception) {
            Ui.error(exception);
          }
        });
    root.getChildren().addAll(new Label("账号"), account, new Label("密码"), password, login);
  }

  private void changePassword(AppServices services, UserSession session, Runnable done) {
    Dialog<ButtonType> dialog = new Dialog<ButtonType>();
    dialog.setTitle("修改初始密码");
    dialog.setHeaderText("为保护数据安全，请先修改密码");
    PasswordField oldPassword = new PasswordField();
    PasswordField newPassword = new PasswordField();
    PasswordField confirmation = new PasswordField();
    GridPane form = new GridPane();
    form.setHgap(8);
    form.setVgap(10);
    form.addRow(0, new Label("当前密码"), oldPassword);
    form.addRow(1, new Label("新密码"), newPassword);
    form.addRow(2, new Label("确认密码"), confirmation);
    dialog.getDialogPane().setContent(form);
    dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
    dialog.setResultConverter(
        button -> {
          if (button == ButtonType.OK) {
            try {
              if (!newPassword.getText().equals(confirmation.getText())) {
                throw new IllegalArgumentException("两次输入的新密码不一致");
              }
              services.auth.changePassword(session, oldPassword.getText(), newPassword.getText());
              done.run();
            } catch (Exception exception) {
              Ui.error(exception);
              return null;
            }
          }
          return button;
        });
    dialog.showAndWait();
  }
}
