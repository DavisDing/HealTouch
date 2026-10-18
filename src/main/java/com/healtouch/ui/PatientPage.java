package com.healtouch.ui;

import com.healtouch.app.AppServices;
import com.healtouch.model.Patient;
import com.healtouch.model.Permission;
import com.healtouch.model.UserSession;
import com.healtouch.service.Authorization;
import com.healtouch.util.Money;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.List;

public class PatientPage {
  private final AppServices services;
  private final UserSession session;
  private final boolean autoCreate;
  private final TableView<Patient> table = new TableView<Patient>();
  private final TextField searchField = new TextField();
  private final ComboBox<String> typeFilter = new ComboBox<String>();

  public PatientPage(AppServices services, UserSession session) {
    this(services, session, false);
  }

  public PatientPage(AppServices services, UserSession session, boolean autoCreate) {
    this.services = services;
    this.session = session;
    this.autoCreate = autoCreate;
  }

  public Parent node() {
    searchField.setPromptText("输入患者姓名 / 手机号 / 身份证号搜索...");
    searchField.setPrefWidth(300);
    searchField.textProperty().addListener((obs, o, n) -> search());

    typeFilter.setItems(FXCollections.observableArrayList("全部类型", "成人", "小儿"));
    typeFilter.setValue("全部类型");
    typeFilter.setOnAction(e -> search());

    Button create = Ui.primaryButton("＋ 创建新档案");
    create.setOnAction(e -> edit(new Patient()));
    create.setVisible(can(Permission.PATIENT_CREATE));

    Button refreshBtn = new Button("刷新");
    refreshBtn.setOnAction(e -> search());

    HBox tb = Ui.toolbar(new Label("检索患者："), searchField, typeFilter, refreshBtn, Ui.spacer(), create);

    TableColumn<Patient, String> codeCol = new TableColumn<Patient, String>("档案编号");
    codeCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().patientCode));
    codeCol.setPrefWidth(110);

    TableColumn<Patient, String> nameCol = new TableColumn<Patient, String>("姓名");
    nameCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().name));
    nameCol.setPrefWidth(100);

    TableColumn<Patient, String> typeCol = new TableColumn<Patient, String>("患者类型");
    typeCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().isChild ? "小儿" : "成人"));
    typeCol.setPrefWidth(90);
    typeCol.setCellFactory(col -> new TableCell<Patient, String>() {
      @Override
      protected void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
          setGraphic(null);
          setText(null);
        } else if ("小儿".equals(item)) {
          setGraphic(Ui.childBadge());
          setText(null);
        } else {
          setGraphic(Ui.adultBadge());
          setText(null);
        }
      }
    });

    TableColumn<Patient, String> genderCol = new TableColumn<Patient, String>("性别");
    genderCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().gender == null ? "-" : c.getValue().gender));
    genderCol.setPrefWidth(60);

    TableColumn<Patient, String> phoneCol = new TableColumn<Patient, String>("联系电话");
    phoneCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().phone));
    phoneCol.setPrefWidth(125);

    TableColumn<Patient, String> balanceCol = new TableColumn<Patient, String>("预存可用余额");
    balanceCol.setCellValueFactory(c -> new SimpleStringProperty(Money.format(c.getValue().depositBalanceCents)));
    balanceCol.setPrefWidth(130);
    balanceCol.setStyle("-fx-font-weight: bold; -fx-text-fill: #087F6D;");

    TableColumn<Patient, String> guardianCol = new TableColumn<Patient, String>("法定监护人 (小儿专填)");
    guardianCol.setCellValueFactory(c -> new SimpleStringProperty(
        c.getValue().isChild ? (c.getValue().guardianName + " (" + (c.getValue().guardianRelation == null ? "" : c.getValue().guardianRelation) + " - " + c.getValue().guardianPhone + ")") : "-"));
    guardianCol.setPrefWidth(220);

    TableColumn<Patient, String> actionCol = new TableColumn<Patient, String>("操作");
    actionCol.setPrefWidth(150);
    actionCol.setCellFactory(c -> new TableCell<Patient, String>() {
      private final Button editBtn = new Button("编辑档案");
      private final Button viewBtn = new Button("查看详情");
      private final HBox box = new HBox(6, viewBtn, editBtn);
      {
        editBtn.setStyle("-fx-font-size: 11px; -fx-padding: 4 8;");
        viewBtn.setStyle("-fx-font-size: 11px; -fx-padding: 4 8; -fx-text-fill: #087F6D;");
        editBtn.setOnAction(e -> edit(getTableView().getItems().get(getIndex())));
        viewBtn.setOnAction(e -> view(getTableView().getItems().get(getIndex())));
      }
      @Override
      protected void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);
        setGraphic(empty ? null : box);
      }
    });

    table.getColumns().addAll(codeCol, nameCol, typeCol, genderCol, phoneCol, balanceCol, guardianCol, actionCol);
    VBox.setVgrow(table, Priority.ALWAYS);

    search();
    if (autoCreate && can(Permission.PATIENT_CREATE)) edit(new Patient());
    return Ui.page("患者档案管理", "规范建立健康档案 · 支持成人与小儿患者特殊监护人必填核验", tb, table);
  }

  private void search() {
    try {
      List<Patient> list = services.patients.search(session, searchField.getText(), 0, 100);
      String filter = typeFilter.getValue();
      if ("成人".equals(filter)) {
        list.removeIf(p -> p.isChild);
      } else if ("小儿".equals(filter)) {
        list.removeIf(p -> !p.isChild);
      }
      table.setItems(FXCollections.observableArrayList(list));
    } catch (Exception e) {
      Ui.error(e);
    }
  }

  private void view(Patient p) {
    Alert a = new Alert(Alert.AlertType.INFORMATION);
    a.setTitle("患者档案详情 - " + p.name);
    a.setHeaderText(p.name + " (" + (p.isChild ? "小儿患者" : "成人患者") + ") · 档案号: " + p.patientCode);

    StringBuilder sb = new StringBuilder();
    sb.append("性别: ").append(p.gender == null ? "未填" : p.gender).append("\n");
    sb.append("出生日期: ").append(p.birthDate == null ? "未填" : p.birthDate.toString()).append("\n");
    sb.append("联系电话: ").append(p.phone).append("\n");
    sb.append("证件号码: ").append(p.idCard == null || p.idCard.isEmpty() ? "未登记" : p.idCard).append("\n");
    sb.append("预存可用金: ").append(Money.format(p.depositBalanceCents)).append("\n\n");

    if (p.isChild) {
      sb.append("【法定监护人信息】\n");
      sb.append("监护人姓名: ").append(p.guardianName).append("\n");
      sb.append("亲属关系: ").append(p.guardianRelation).append("\n");
      sb.append("监护人电话: ").append(p.guardianPhone).append("\n\n");
    }

    sb.append("既往调理/病史: ").append(p.medicalHistory == null || p.medicalHistory.isEmpty() ? "无" : p.medicalHistory).append("\n");
    sb.append("过敏反应史: ").append(p.allergies == null || p.allergies.isEmpty() ? "无" : p.allergies).append("\n");
    sb.append("家庭住址: ").append(p.address == null || p.address.isEmpty() ? "未填" : p.address);

    a.setContentText(sb.toString());
    a.showAndWait();
  }

  private void edit(Patient p) {
    Dialog<ButtonType> dialog = new Dialog<ButtonType>();
    dialog.setTitle(p.id == null ? "新建患者档案" : "编辑档案 - " + p.name);
    dialog.setHeaderText("填写门诊患者详细信息（带 * 为必填项）");

    TextField name = new TextField(p.name == null ? "" : p.name);
    ComboBox<String> type = new ComboBox<String>(FXCollections.observableArrayList("成人", "小儿"));
    type.setValue(p.isChild ? "小儿" : "成人");
    ComboBox<String> gender = new ComboBox<String>(FXCollections.observableArrayList("男", "女"));
    gender.setValue(p.gender == null ? "男" : p.gender);
    DatePicker birthDate = new DatePicker(p.birthDate == null ? LocalDate.now().minusYears(25) : p.birthDate);
    TextField phone = new TextField(p.phone == null ? "" : p.phone);
    TextField idCard = new TextField(p.idCard == null ? "" : p.idCard);

    // Guardian fields
    TextField gName = new TextField(p.guardianName == null ? "" : p.guardianName);
    TextField gRel = new TextField(p.guardianRelation == null ? "" : p.guardianRelation);
    TextField gPhone = new TextField(p.guardianPhone == null ? "" : p.guardianPhone);

    TextArea medicalHistory = new TextArea(p.medicalHistory == null ? "" : p.medicalHistory);
    medicalHistory.setPrefRowCount(2);
    TextArea allergies = new TextArea(p.allergies == null ? "" : p.allergies);
    allergies.setPrefRowCount(2);
    TextField address = new TextField(p.address == null ? "" : p.address);

    Label gNameLbl = new Label("监护人姓名 *:");
    Label gRelLbl = new Label("监护人关系 *:");
    Label gPhoneLbl = new Label("监护人电话 *:");

    VBox guardianBox = new VBox(8);
    guardianBox.setPadding(new Insets(10));
    guardianBox.setStyle("-fx-background-color: #FEF9F3; -fx-border-color: #FDE68A; -fx-border-radius: 8; -fx-background-radius: 8;");
    Label guardianNotice = new Label("小儿患者法定监护人信息（必填）");
    guardianNotice.setStyle("-fx-text-fill: #B45309; -fx-font-weight: bold; -fx-font-size: 11px;");
    GridPane gg = new GridPane();
    gg.setHgap(8);
    gg.setVgap(8);
    gg.add(gNameLbl, 0, 0); gg.add(gName, 1, 0);
    gg.add(gRelLbl, 2, 0); gg.add(gRel, 3, 0);
    gg.add(gPhoneLbl, 0, 1); gg.add(gPhone, 1, 1);
    guardianBox.getChildren().addAll(guardianNotice, gg);

    Runnable toggleChild = () -> {
      boolean child = "小儿".equals(type.getValue());
      guardianBox.setVisible(child);
      guardianBox.setManaged(child);
    };
    type.setOnAction(e -> toggleChild.run());
    toggleChild.run();

    GridPane grid = new GridPane();
    grid.setHgap(10);
    grid.setVgap(10);
    grid.setPadding(new Insets(16));
    grid.add(new Label("患者姓名 *:"), 0, 0); grid.add(name, 1, 0);
    grid.add(new Label("患者类型 *:"), 2, 0); grid.add(type, 3, 0);
    grid.add(new Label("性别:"), 0, 1); grid.add(gender, 1, 1);
    grid.add(new Label("出生日期:"), 2, 1); grid.add(birthDate, 3, 1);
    grid.add(new Label("联系电话 *:"), 0, 2); grid.add(phone, 1, 2);
    grid.add(new Label("身份证件号:"), 2, 2); grid.add(idCard, 3, 2);

    grid.add(guardianBox, 0, 3, 4, 1);

    grid.add(new Label("既往病史:"), 0, 4); grid.add(medicalHistory, 1, 4, 3, 1);
    grid.add(new Label("药物过敏史:"), 0, 5); grid.add(allergies, 1, 5, 3, 1);
    grid.add(new Label("家庭住址:"), 0, 6); grid.add(address, 1, 6, 3, 1);

    dialog.getDialogPane().setContent(grid);
    dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
    dialog.showAndWait().ifPresent(btn -> {
      if (btn != ButtonType.OK) return;
      try {
        p.name = name.getText().trim();
        p.isChild = "小儿".equals(type.getValue());
        p.gender = gender.getValue();
        p.birthDate = birthDate.getValue();
        p.phone = phone.getText().trim();
        p.idCard = idCard.getText().trim();
        p.guardianName = gName.getText().trim();
        p.guardianRelation = gRel.getText().trim();
        p.guardianPhone = gPhone.getText().trim();
        p.medicalHistory = medicalHistory.getText().trim();
        p.allergies = allergies.getText().trim();
        p.address = address.getText().trim();
        if (p.id == null) services.patients.create(session, p);
        else services.patients.update(session, p);
        search();
      } catch (Exception e) {
        Ui.error(e);
      }
    });
  }

  private boolean can(Permission p) {
    try { Authorization.require(session, p); return true; }
    catch (SecurityException e) { return false; }
  }
}
