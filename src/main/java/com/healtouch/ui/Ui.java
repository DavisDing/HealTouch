package com.healtouch.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public final class Ui {
  private Ui() {}

  public static void error(Throwable e) {
    Alert a = new Alert(Alert.AlertType.ERROR);
    a.setTitle("操作未完成");
    a.setHeaderText(e.getMessage() == null ? "发生未预期错误" : e.getMessage());
    a.setContentText("请核对必填项目与业务规则后继续操作。");
    a.showAndWait();
  }

  public static void info(String text) {
    Alert a = new Alert(Alert.AlertType.INFORMATION, text, ButtonType.OK);
    a.setTitle("HealTouch 门诊提示");
    a.setHeaderText(null);
    a.showAndWait();
  }

  public static boolean confirm(String text) {
    Alert a = new Alert(Alert.AlertType.CONFIRMATION, text, ButtonType.OK, ButtonType.CANCEL);
    a.setTitle("请确认操作");
    a.setHeaderText(null);
    return a.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
  }

  public static VBox page(String title, Node... children) {
    return page(title, "标准门诊规范 · 严禁欠费挂账 · 预存资金双写审计", children);
  }

  public static VBox page(String title, String subtitle, Node... children) {
    Label eyebrow = new Label("HEALTOUCH · 推拿门诊管理系统");
    eyebrow.setStyle("-fx-text-fill: #087F6D; -fx-font-size: 11px; -fx-font-weight: bold; -fx-letter-spacing: 0.5px;");

    Label h = new Label(title);
    h.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #13334A;");

    Label sub = new Label(subtitle);
    sub.setStyle("-fx-font-size: 13px; -fx-text-fill: #668292;");

    VBox heading = new VBox(4, eyebrow, h, sub);
    VBox box = new VBox(16);
    box.setPadding(new Insets(24, 28, 28, 28));
    box.setStyle("-fx-background-color: #F4F7F9;");
    box.getChildren().add(heading);
    box.getChildren().addAll(children);
    return box;
  }

  public static HBox toolbar(Node... children) {
    HBox h = new HBox(12);
    h.setAlignment(Pos.CENTER_LEFT);
    h.setPadding(new Insets(12, 16, 12, 16));
    h.getStyleClass().add("toolbar-container");
    h.getChildren().addAll(children);
    return h;
  }

  public static Button navButton(String text) {
    Button b = new Button(text);
    b.setMaxWidth(Double.MAX_VALUE);
    b.setStyle(
        "-fx-alignment: CENTER_LEFT; -fx-padding: 10 14; -fx-background-color: transparent; -fx-border-color: transparent; -fx-font-weight: 600; -fx-text-fill: #415E70; -fx-font-size: 13px;");
    return b;
  }

  public static Button primaryButton(String text) {
    Button b = new Button(text);
    b.getStyleClass().add("primary-button");
    return b;
  }

  public static Button secondaryButton(String text) {
    Button b = new Button(text);
    b.getStyleClass().add("secondary-button");
    return b;
  }

  public static Button dangerButton(String text) {
    Button b = new Button(text);
    b.getStyleClass().add("danger-button");
    return b;
  }

  public static HBox brand() {
    Label cross = new Label("+");
    cross.setStyle("-fx-text-fill: white; -fx-font-size: 22px; -fx-font-weight: bold;");
    StackPane mark = new StackPane(cross);
    mark.setMinSize(36, 36);
    mark.setPrefSize(36, 36);
    mark.setMaxSize(36, 36);
    mark.setStyle(
        "-fx-background-color: linear-gradient(to bottom right, #087F6D, #149A84); -fx-background-radius: 9;");

    Label name = new Label("HealTouch");
    name.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #13334A;");

    Label ver = new Label("门诊专业版 V2.4");
    ver.setStyle("-fx-font-size: 11px; -fx-text-fill: #087F6D; -fx-font-weight: bold;");

    HBox brandLine = new HBox(6, name, ver);
    brandLine.setAlignment(Pos.BASELINE_LEFT);

    Label clinic = new Label("和康堂中医推拿门诊 · 就诊中");
    clinic.setStyle("-fx-font-size: 11px; -fx-text-fill: #7892A0;");

    VBox words = new VBox(2, brandLine, clinic);
    words.setAlignment(Pos.CENTER_LEFT);

    HBox brand = new HBox(10, mark, words);
    brand.setAlignment(Pos.CENTER_LEFT);
    return brand;
  }

  public static VBox section(Node... children) {
    VBox box = new VBox(12);
    box.getStyleClass().add("card-section");
    box.getChildren().addAll(children);
    return box;
  }

  public static Label childBadge() {
    Label label = new Label("小儿");
    label.setStyle("-fx-background-color: #FEF2E2; -fx-text-fill: #C26A08; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 3 8; -fx-background-radius: 4;");
    return label;
  }

  public static Label adultBadge() {
    Label label = new Label("成人");
    label.setStyle("-fx-background-color: #E8F4FD; -fx-text-fill: #1E40AF; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 3 8; -fx-background-radius: 4;");
    return label;
  }

  public static Label paidBadge() {
    Label label = new Label("已支付");
    label.setStyle("-fx-background-color: #D1EFE8; -fx-text-fill: #087F6D; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 3 8; -fx-background-radius: 4;");
    return label;
  }

  public static Label pendingBadge() {
    Label label = new Label("待收费");
    label.setStyle("-fx-background-color: #FFF3CD; -fx-text-fill: #B45309; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 3 8; -fx-background-radius: 4;");
    return label;
  }

  public static Label ruleNotice(String title, String detail) {
    Label t = new Label("● " + title + "：" + detail);
    t.setStyle("-fx-text-fill: #4B6A78; -fx-font-size: 12px;");
    return t;
  }

  public static Region spacer() {
    Region spacer = new Region();
    HBox.setHgrow(spacer, Priority.ALWAYS);
    return spacer;
  }

  public static void grow(Node node) {
    HBox.setHgrow(node, Priority.ALWAYS);
  }
}
