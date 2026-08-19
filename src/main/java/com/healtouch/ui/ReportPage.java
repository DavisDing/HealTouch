package com.healtouch.ui;

import com.healtouch.app.AppServices;
import com.healtouch.model.UserSession;
import com.healtouch.service.DashboardService;
import com.healtouch.util.Money;
import javafx.scene.Parent;
import javafx.scene.control.Label;

/** 统计页面明确显示口径：收入按支付/退款执行日期归集，预存充值不计服务收入。 */
public class ReportPage {
  private final AppServices services;
  private final UserSession session;

  public ReportPage(AppServices services, UserSession session) {
    this.services = services;
    this.session = session;
  }

  public Parent node() {
    DashboardService.Today today = services.dashboard.today(session);
    Label net = new Label("今日服务净收入：" + Money.format(today.incomeCents));
    net.setStyle("-fx-font-size: 26px; -fx-font-weight: bold;");
    Label rule = new Label("统计口径：按支付成功日统计服务收入；退款按执行日冲减净收入。预存充值仅作为资金流入，不计入服务收入。");
    rule.setWrapText(true);
    return Ui.page(
        "数据统计",
        net,
        new Label("今日已收费账单：" + today.paidBills + " 笔"),
        new Label("今日新增患者：" + today.newPatients + " 人"),
        rule);
  }
}
