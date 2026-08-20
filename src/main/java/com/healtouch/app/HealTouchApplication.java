package com.healtouch.app;

import com.healtouch.model.UserSession;
import com.healtouch.ui.LoginView;
import com.healtouch.ui.MainView;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class HealTouchApplication extends Application {
  private AppServices services;
  private Stage stage;

  @Override
  public void start(Stage primaryStage) {
    this.stage = primaryStage;
    this.services = new AppServices();
    stage.setTitle("HealTouch 推拿门诊管理系统");
    showLogin();
    stage.show();
  }

  public void showLogin() {
    Scene scene = new Scene(new LoginView(services, this::onLogin).node(), 520, 430);
    applyTheme(scene);
    stage.setScene(scene);
  }

  private void onLogin(UserSession session) {
    Scene scene = new Scene(new MainView(services, session, this::showLogin).node(), 1240, 800);
    applyTheme(scene);
    stage.setScene(scene);
  }

  private void applyTheme(Scene scene) {
    scene.getStylesheets().add(
        HealTouchApplication.class.getResource("/styles/healtouch.css").toExternalForm());
  }

  @Override
  public void stop() {
    if (services != null) services.close();
    Platform.exit();
  }

  public static void main(String[] args) {
    launch(args);
  }
}
