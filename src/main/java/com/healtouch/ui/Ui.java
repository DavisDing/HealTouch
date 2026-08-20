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
    a.setContentText("请检查必填内容后继续填写。");
    a.showAndWait();
  }

  public static void info(String text) {
    Alert a = new Alert(Alert.AlertType.INFORMATION, text, ButtonType.OK);
    a.setTitle("HealTouch");
    a.setHeaderText(null);
    a.showAndWait();
  }

  public static boolean confirm(String text) {
    Alert a = new Alert(Alert.AlertType.CONFIRMATION, text, ButtonType.OK, ButtonType.CANCEL);
    a.setTitle("请确认");
    return a.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
  }

  public static VBox page(String title, Node... children) {
    Label eyebrow = new Label("HEALTOUCH · 门诊工作台");
    eyebrow.setStyle("-fx-text-fill: #16806e; -fx-font-size: 11px; -fx-font-weight: bold;");
    Label h = new Label(title);
    h.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: #16334a;");
    VBox heading = new VBox(4, eyebrow, h);
    VBox box = new VBox(16);
    box.setPadding(new Insets(28, 32, 32, 32));
    box.setStyle("-fx-background-color: #f5f8fb;");
    box.getChildren().add(heading);
    box.getChildren().addAll(children);
    return box;
  }

  public static HBox toolbar(Node... children) {
    HBox h = new HBox(10);
    h.setAlignment(Pos.CENTER_LEFT);
    h.setPadding(new Insets(12));
    h.setStyle(
        "-fx-background-color: white; -fx-background-radius: 12; -fx-border-color: #e0e9ee; -fx-border-radius: 12;");
    h.getChildren().addAll(children);
    return h;
  }

  public static Button navButton(String text) {
    Button b = new Button(text);
    b.setMaxWidth(Double.MAX_VALUE);
    b.setStyle(
        "-fx-alignment: CENTER_LEFT; -fx-padding: 10 14; -fx-background-color: transparent; -fx-border-color: transparent; -fx-font-weight: bold; -fx-text-fill: #38556a;");
    return b;
  }

  public static Button primaryButton(String text) {
    Button b = new Button(text);
    b.getStyleClass().add("primary-button");
    return b;
  }

  public static HBox brand() {
    Label cross = new Label("+");
    cross.setStyle("-fx-text-fill: white; -fx-font-size: 25px; -fx-font-weight: bold;");
    StackPane mark = new StackPane(cross);
    mark.setMinSize(40, 40);
    mark.setPrefSize(40, 40);
    mark.setMaxSize(40, 40);
    mark.setStyle(
        "-fx-background-color: linear-gradient(to bottom right, #0a8873, #34b59d); -fx-background-radius: 13;");
    Label name = new Label("HealTouch");
    name.setStyle("-fx-font-size: 19px; -fx-font-weight: bold; -fx-text-fill: #17364b;");
    Label sub = new Label("推拿门诊管理");
    sub.setStyle("-fx-font-size: 10px; -fx-text-fill: #66808f;");
    VBox words = new VBox(1, name, sub);
    words.setAlignment(Pos.CENTER_LEFT);
    HBox brand = new HBox(10, mark, words);
    brand.setAlignment(Pos.CENTER_LEFT);
    return brand;
  }

  public static VBox section(Node... children) {
    VBox box = new VBox(12);
    box.setPadding(new Insets(18));
    box.setStyle(
        "-fx-background-color: white; -fx-background-radius: 12; -fx-border-color: #e0e9ee; -fx-border-radius: 12;");
    box.getChildren().addAll(children);
    return box;
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
