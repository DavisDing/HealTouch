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

  public Parent node() {
    return root;
  }

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
            if (!password.getText().equals(confirmation.getText())) {
              throw new IllegalArgumentException("两次输入的密码不一致");
            }
            services.auth.initializeAdministrator(password.getText());
            onLogin.accept(services.auth.login("admin", password.getText()));
          } catch (Exception exception) {
            Ui.error(exception);
          }
        });
    card
        .getChildren()
        .addAll(
            heading,
            instruction,
            new Label("管理员密码"),
            password,
            new Label("确认密码"),
            confirmation,
            create);
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
          try {
            UserSession session = services.auth.login(account.getText(), password.getText());
            if (session.mustChangePassword) {
              openLoggedInPasswordChange(services, session, () -> onLogin.accept(session));
            } else {
              onLogin.accept(session);
            }
          } catch (Exception exception) {
            Ui.error(exception);
          }
        });
    Button changePassword = new Button("修改密码");
    changePassword.setOnAction(event -> openPasswordChangeFromLogin(services));
    card
        .getChildren()
        .addAll(
            heading,
            description,
            new Label("账号"),
            account,
            new Label("密码"),
            password,
            login,
            changePassword);
  }

  /** Allows a signed-out user to verify their current credentials before changing their password. */
  private void openPasswordChangeFromLogin(AppServices services) {
    Dialog<ButtonType> dialog = new Dialog<ButtonType>();
    dialog.setTitle("修改密码");
    dialog.setHeaderText("请先验证账号和当前密码。");
    TextField account = new TextField();
    PasswordField oldPassword = new PasswordField();
    PasswordField newPassword = new PasswordField();
    PasswordField confirmation = new PasswordField();
    GridPane form = passwordForm(account, oldPassword, newPassword, confirmation, true);
    dialog.getDialogPane().setContent(form);
    dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
    Button ok = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
    ok.addEventFilter(
        javafx.event.ActionEvent.ACTION,
        event -> {
          try {
            requireMatchingNewPasswords(newPassword, confirmation);
            UserSession session = services.auth.login(account.getText(), oldPassword.getText());
            services.auth.changePassword(session, oldPassword.getText(), newPassword.getText());
            Ui.info("密码已修改，请使用新密码登录。");
          } catch (Exception exception) {
            event.consume();
            Ui.error(exception);
          }
        });
    dialog.showAndWait();
  }

  private void openLoggedInPasswordChange(AppServices services, UserSession session, Runnable done) {
    Dialog<ButtonType> dialog = new Dialog<ButtonType>();
    dialog.setTitle("修改初始密码");
    dialog.setHeaderText("为保护数据安全，请先修改密码后继续使用系统。");
    PasswordField oldPassword = new PasswordField();
    PasswordField newPassword = new PasswordField();
    PasswordField confirmation = new PasswordField();
    GridPane form = passwordForm(null, oldPassword, newPassword, confirmation, false);
    dialog.getDialogPane().setContent(form);
    dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
    Button ok = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
    ok.addEventFilter(
        javafx.event.ActionEvent.ACTION,
        event -> {
          try {
            requireMatchingNewPasswords(newPassword, confirmation);
            services.auth.changePassword(session, oldPassword.getText(), newPassword.getText());
            done.run();
          } catch (Exception exception) {
            event.consume();
            Ui.error(exception);
          }
        });
    dialog.showAndWait();
  }

  private GridPane passwordForm(
      TextField account,
      PasswordField oldPassword,
      PasswordField newPassword,
      PasswordField confirmation,
      boolean includeAccount) {
    GridPane form = new GridPane();
    form.setVgap(8);
    form.setHgap(10);
    int row = 0;
    if (includeAccount) {
      form.addRow(row++, new Label("账号"), account);
    }
    form.addRow(row++, new Label("当前密码"), oldPassword);
    form.addRow(row++, new Label("新密码"), newPassword);
    form.addRow(row, new Label("确认新密码"), confirmation);
    return form;
  }

  private void requireMatchingNewPasswords(PasswordField newPassword, PasswordField confirmation) {
    if (!newPassword.getText().equals(confirmation.getText())) {
      throw new IllegalArgumentException("两次输入的新密码不一致");
    }
  }
}
