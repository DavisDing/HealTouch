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
    stage.setScene(new Scene(new LoginView(services, this::onLogin).node(), 460, 330));
  }

  private void onLogin(UserSession session) {
    stage.setScene(new Scene(new MainView(services, session, this::showLogin).node(), 1180, 760));
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
