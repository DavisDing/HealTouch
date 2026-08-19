package com.healtouch.ui;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public final class Ui {
  private Ui() {}

  public static void error(Throwable e) {
    Alert a = new Alert(Alert.AlertType.ERROR);
    a.setTitle("操作未完成");
    a.setHeaderText(e.getMessage() == null ? "发生未预期错误" : e.getMessage());
    a.setContentText(null);
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
    Label h = new Label(title);
    h.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");
    VBox box = new VBox(14);
    box.setPadding(new Insets(24));
    box.getChildren().add(h);
    box.getChildren().addAll(children);
    return box;
  }

  public static HBox toolbar(Node... children) {
    HBox h = new HBox(8);
    h.setPadding(new Insets(0, 0, 6, 0));
    h.getChildren().addAll(children);
    return h;
  }

  public static Button navButton(String text) {
    Button b = new Button(text);
    b.setMaxWidth(Double.MAX_VALUE);
    b.setStyle(
        "-fx-alignment: CENTER_LEFT; -fx-padding: 10 14; -fx-background-color: transparent;");
    return b;
  }

  public static void grow(Node node) {
    HBox.setHgrow(node, Priority.ALWAYS);
  }
}
