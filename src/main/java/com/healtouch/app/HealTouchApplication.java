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
  private UserSession currentSession;
  private javafx.animation.Timeline sessionCheck;
  private boolean checking;

  @Override
  public void start(Stage primaryStage) {
    this.stage = primaryStage;
    com.healtouch.ui.DbWork.owner=stage;
    com.healtouch.ui.Ui.sessionExpired=this::showLogin;
    com.healtouch.ui.Ui.activity=()->{if(currentSession!=null)currentSession.touch();};
    stage.setTitle("HealTouch 推拿门诊管理系统");
    Scene startup=new Scene(com.healtouch.ui.Ui.page("正在启动",new javafx.scene.control.Label("正在初始化本地数据库…")),520,430);
    applyTheme(startup);stage.setScene(startup);stage.show();
    try {com.healtouch.config.Logging.initialize();}
    catch(Exception e) {com.healtouch.ui.Ui.error(new IllegalStateException("日志目录不可写，无法启动",e));stage.close();return;}
    try {this.services = com.healtouch.ui.DbWork.call(AppServices::new);}
    catch(Exception e) {com.healtouch.ui.Ui.error(e);stage.close();return;}
    stage.setTitle("HealTouch 推拿门诊管理系统");
    showLogin();
    stage.show();
  }

  public void showLogin() {
    if(sessionCheck!=null) sessionCheck.stop();
    if(currentSession!=null) currentSession.invalidate();
    currentSession=null;
    com.healtouch.ui.Ui.openRecovery=null;
    com.healtouch.ui.Ui.closeDialogs();
    Scene scene = services.database.recoveryRequired()
        ? new Scene(new com.healtouch.ui.RecoveryView(services,this::showLogin).node(),850,720)
        : new Scene(new LoginView(services, this::onLogin).node(), 520, 430);
    applyTheme(scene);
    stage.setScene(scene);
  }

  private void onLogin(UserSession session) {
    currentSession=session;
    Scene scene = new Scene(new MainView(services, session, this::showLogin).node(), 1240, 800);
    scene.addEventFilter(javafx.scene.input.InputEvent.ANY,event->session.touch());
    applyTheme(scene);
    stage.setScene(scene);
    sessionCheck=new javafx.animation.Timeline(new javafx.animation.KeyFrame(javafx.util.Duration.seconds(5),event->{
      if(checking || currentSession!=session) return;
      checking=true;
      com.healtouch.app.AppTasks.DATABASE.execute(()->{
        Throwable failure=null;
        try {com.healtouch.service.Authorization.refresh(session);}catch(Throwable e){failure=e;}
        final Throwable error=failure;
        Platform.runLater(()->{
          checking=false;
          if(currentSession!=session) return;
          if(error!=null) {
            org.slf4j.LoggerFactory.getLogger(HealTouchApplication.class).error("会话检查失败",error);
            showLogin();
          }
        });
      });
    }));
    sessionCheck.setCycleCount(javafx.animation.Animation.INDEFINITE);sessionCheck.play();
  }

  private void applyTheme(Scene scene) {
    scene.getStylesheets().add(
        HealTouchApplication.class.getResource("/styles/healtouch.css").toExternalForm());
  }

  @Override
  public void stop() {
    if(sessionCheck!=null) sessionCheck.stop();
    if(services!=null)services.maintenance.close();
    AppTasks.DATABASE.shutdown();
    Thread cleanup=new Thread(()->{
      try {
        if(!AppTasks.DATABASE.awaitTermination(60,java.util.concurrent.TimeUnit.SECONDS)) {
          AppTasks.DATABASE.shutdownNow();
          AppTasks.DATABASE.awaitTermination(30,java.util.concurrent.TimeUnit.SECONDS);
        }
      }catch(InterruptedException e){Thread.currentThread().interrupt();}
      finally{if(services!=null)services.close();}
    },"healtouch-shutdown");
    cleanup.setDaemon(false);cleanup.start();
  }

  public static void main(String[] args) {
    launch(args);
  }
}
