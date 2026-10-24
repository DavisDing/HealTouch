package com.healtouch.ui;

import com.healtouch.app.AppServices;
import com.healtouch.model.UserSession;
import com.healtouch.service.DashboardService;
import com.healtouch.util.Money;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class DashboardPage {
  private final AppServices services;
  private final UserSession session;
  private final MainView main;
  private final Label income = new Label("¥ 0.00");
  private final Label patients = new Label("0 人");
  private final Label bills = new Label("0 笔");

  public DashboardPage(AppServices services, UserSession session, MainView main) {
    this.services = services;
    this.session = session;
    this.main = main;
  }

  public Parent node() {
    GridPane cards = new GridPane();
    cards.setHgap(16);
    cards.add(metricCard("今日服务净收入", income, "已扣除退款 · 实收账单数", "#087F6D", "#E8F6F3"), 0, 0);
    cards.add(metricCard("今日就诊及建档", patients, "门诊成人与小儿患者档案", "#D97706", "#FEF3C7"), 1, 0);
    cards.add(metricCard("今日已收费账单", bills, "全额收齐 · 严禁欠费挂账", "#2563EB", "#DBEAFE"), 2, 0);
    for (int i = 0; i < 3; i++) GridPane.setHgrow(cards.getChildren().get(i), Priority.ALWAYS);

    // Quick Actions Banner
    Label shortcutTitle = new Label("门诊核心快捷操作");
    shortcutTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #17384D;");
    Label shortcutSub = new Label("规范流程：先建档查询患者，再登记推拿理疗项目，支持预存金划扣与多种支付方式即时结清。");
    shortcutSub.setStyle("-fx-font-size: 12px; -fx-text-fill: #668292;");

    Button create = Ui.primaryButton("＋ 新建患者档案");
    create.setOnAction(e -> main.openCreatePatient());
    Button treatment = Ui.secondaryButton("⚡ 发起治疗登记");
    treatment.setOnAction(e -> main.openTreatment());
    Button bill = new Button("账单查询与收银");
    bill.setOnAction(e -> main.openBills());
    Button deposit = new Button("预存充值管理");
    deposit.setOnAction(e -> main.openDeposits());

    create.setDisable(!session.can(com.healtouch.model.Permission.PATIENT_EDIT));
    treatment.setDisable(!session.can(com.healtouch.model.Permission.TREATMENT_CREATE));
    bill.setDisable(!session.can(com.healtouch.model.Permission.BILL_CHARGE) && !session.can(com.healtouch.model.Permission.BILL_REFUND));
    deposit.setDisable(!session.can(com.healtouch.model.Permission.DEPOSIT_RECHARGE) && !session.can(com.healtouch.model.Permission.DEPOSIT_REFUND));
    HBox btnRow = new HBox(12, create, treatment, bill, deposit);
    btnRow.setAlignment(Pos.CENTER_LEFT);
    VBox shortcuts = Ui.section(shortcutTitle, shortcutSub, btnRow);

    // Compliance & Rule Warning Section
    Label ruleTitle = new Label("门诊合规与风控提示");
    ruleTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #17384D;");

    Label r1 = Ui.ruleNotice("严禁欠费挂账", "收费必须全额结清，未完成结算单据超期 30 分钟将自动作废。");
    Label r2 = Ui.ruleNotice("小儿建档监护人必填", "小儿患者建档必须录入监护人姓名、与患儿关系和有效联系电话。");
    Label r3 = Ui.ruleNotice("预存账户流水审计", "每笔预存充值、划扣与退款均记入流水且余额不可为负。");

    VBox rules = Ui.section(ruleTitle, r1, r2, r3);

    Label backupWarning=new Label();backupWarning.setWrapText(true);
    backupWarning.setStyle("-fx-text-fill: #B45309; -fx-font-weight: bold;");
    Runnable updateBackupWarning=()->{
      String warning=services.backups.problem();
      if(!services.maintenance.problem().isEmpty()) warning=warning+"\n"+services.maintenance.problem();
      backupWarning.setText(warning);backupWarning.setVisible(!warning.isEmpty());backupWarning.setManaged(!warning.isEmpty());
    };
    updateBackupWarning.run();
    javafx.animation.Timeline warnings=new javafx.animation.Timeline(new javafx.animation.KeyFrame(javafx.util.Duration.seconds(10),e->updateBackupWarning.run()));
    warnings.setCycleCount(javafx.animation.Animation.INDEFINITE);
    backupWarning.sceneProperty().addListener((o,oldScene,newScene)->{if(newScene==null)warnings.stop();else warnings.play();});
    VBox page = Ui.page("首页工作台", "欢迎回来，" + session.name + " · 门诊接诊进行中", backupWarning, cards, shortcuts, rules);
    refresh();
    return page;
  }

  private VBox metricCard(String title, Label value, String helper, String accentColor, String badgeBg) {
    Label t = new Label(title);
    t.setStyle("-fx-text-fill: #5A748A; -fx-font-weight: bold; -fx-font-size: 13px;");

    value.setStyle("-fx-font-size: 32px; -fx-font-weight: 800; -fx-text-fill: #0C3449;");

    Label h = new Label(helper);
    h.setStyle("-fx-text-fill: #7892A0; -fx-font-size: 11px;");

    VBox box = new VBox(6, t, value, h);
    box.setPadding(new Insets(18, 20, 18, 20));
    box.setPrefWidth(260);
    box.setMaxWidth(Double.MAX_VALUE);
    box.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 12; -fx-border-color: #E1EBF0; -fx-border-radius: 12;");
    return box;
  }

  private void refresh() {
    try {
      DashboardService.Today t = DbWork.read(services.dashboard::today, session);
      income.setText(Money.format(t.incomeCents));
      patients.setText(String.valueOf(t.newPatients) + " 人次");
      bills.setText(String.valueOf(t.paidBills) + " 笔已结");
    } catch (Exception e) {
      Ui.error(e);
    }
  }
}
