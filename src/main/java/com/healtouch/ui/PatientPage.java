package com.healtouch.ui;

import com.healtouch.app.AppServices;
import com.healtouch.model.Gender;
import com.healtouch.model.PageResult;
import com.healtouch.model.Patient;
import com.healtouch.model.PatientType;
import com.healtouch.model.Permission;
import com.healtouch.model.UserSession;
import com.healtouch.service.Authorization;
import com.healtouch.util.Money;
import java.time.LocalDate;
import java.util.function.Function;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
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
import javafx.scene.input.KeyCode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class PatientPage {
  private static final int PAGE_SIZE = 50;
  private static final String[] ID_TYPES = {"身份证", "户口本", "出生证明", "护照", "其他"};

  private final AppServices services;
  private final UserSession session;
  private final boolean openCreateOnLoad;
  private final TableView<Patient> table = new TableView<Patient>();
  private final TextField search = new TextField();
  private final ComboBox<String> typeFilter = new ComboBox<String>();
  private final Label pageInfo = new Label();
  private final Button previousPage = new Button("上一页");
  private final Button nextPage = new Button("下一页");
  private int currentPage = 1;

  public PatientPage(AppServices services, UserSession session) {
    this(services, session, false);
  }

  public PatientPage(AppServices services, UserSession session, boolean openCreateOnLoad) {
    this.services = services;
    this.session = session;
    this.openCreateOnLoad = openCreateOnLoad;
  }

  public Parent node() {
    search.setPromptText("姓名、证件号码或手机号");
    search.setPrefWidth(280);
    Button query = Ui.primaryButton("查询");
    query.setOnAction(event -> searchFromFirstPage());
    search.setOnKeyPressed(event -> {
      if (event.getCode() == KeyCode.ENTER) searchFromFirstPage();
    });

    typeFilter.setItems(FXCollections.observableArrayList("全部类型", "成人", "小儿"));
    typeFilter.setValue("全部类型");
    typeFilter.setOnAction(event -> searchFromFirstPage());

    final boolean canEdit = can(Permission.PATIENT_EDIT);
    Button create = Ui.primaryButton("＋ 创建新档案");
    create.setOnAction(event -> openForm(null));
    create.setVisible(canEdit);
    create.setManaged(create.isVisible());

    previousPage.setOnAction(event -> {
      if (currentPage > 1) {
        currentPage--;
        load();
      }
    });
    nextPage.setOnAction(event -> {
      currentPage++;
      load();
    });

    HBox pagination = new HBox(8, previousPage, pageInfo, nextPage);
    pagination.setAlignment(Pos.CENTER_LEFT);
    HBox toolbar = Ui.toolbar(new Label("检索患者："), search, typeFilter, query, Ui.spacer(), create);

    TableColumn<Patient, String> code = column("档案编号", patient -> patient.patientCode);
    code.setPrefWidth(110);
    TableColumn<Patient, String> name = column("姓名", patient -> patient.name);
    name.setPrefWidth(100);
    TableColumn<Patient, String> type = column("患者类型", patient -> patient.isChild() ? "小儿" : "成人");
    type.setPrefWidth(90);
    type.setCellFactory(col -> new TableCell<Patient, String>() {
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

    TableColumn<Patient, String> gender = column("性别", patient -> patient.gender == null ? "—" : patient.gender.getLabel());
    gender.setPrefWidth(60);
    TableColumn<Patient, String> phone = column("联系电话", patient -> patient.phone);
    phone.setPrefWidth(125);
    TableColumn<Patient, String> balance = column("预存可用余额", patient -> Money.format(patient.balanceCents));
    balance.setPrefWidth(130);
    balance.setStyle("-fx-font-weight: bold; -fx-text-fill: #087F6D;");
    TableColumn<Patient, String> guardian = column(
        "法定监护人 (小儿专填)",
        patient -> patient.isChild()
            ? patient.guardianName + " (" + patient.guardianRelationship + " - " + patient.guardianPhone + ")"
            : "—");
    guardian.setPrefWidth(220);

    TableColumn<Patient, String> actions = column("操作", patient -> "");
    actions.setPrefWidth(150);
    actions.setCellFactory(col -> new TableCell<Patient, String>() {
      private final Button view = new Button("查看详情");
      private final Button edit = new Button("编辑档案");
      private final HBox buttons = new HBox(6, view, edit);
      {
        view.setStyle("-fx-font-size: 11px; -fx-padding: 4 8; -fx-text-fill: #087F6D;");
        edit.setStyle("-fx-font-size: 11px; -fx-padding: 4 8;");
        edit.setDisable(!canEdit);
        view.setOnAction(event -> PatientPage.this.view(getTableView().getItems().get(getIndex())));
        edit.setOnAction(event -> PatientPage.this.openForm(getTableView().getItems().get(getIndex())));
      }
      @Override
      protected void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);
        setGraphic(empty ? null : buttons);
      }
    });

    table.getColumns().addAll(code, name, type, gender, phone, balance, guardian, actions);
    table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    VBox.setVgrow(table, Priority.ALWAYS);

    VBox page = Ui.page("患者档案管理", "规范建立健康档案 · 支持成人与小儿患者特殊监护人必填核验", toolbar, table, pagination);
    load();
    if (openCreateOnLoad && canEdit) Platform.runLater(() -> openForm(null));
    return page;
  }

  private TableColumn<Patient, String> column(String label, Function<Patient, String> getter) {
    TableColumn<Patient, String> column = new TableColumn<Patient, String>(label);
    column.setCellValueFactory(cell -> new SimpleStringProperty(getter.apply(cell.getValue())));
    return column;
  }

  private void searchFromFirstPage() {
    currentPage = 1;
    load();
  }

  private void load() {
    try {
      PatientType filter = null;
      if ("成人".equals(typeFilter.getValue())) filter = PatientType.ADULT;
      if ("小儿".equals(typeFilter.getValue())) filter = PatientType.CHILD;
      PageResult<Patient> result = services.patients.searchPage(session, search.getText(), filter, currentPage, PAGE_SIZE);
      currentPage = result.getPage();
      table.setItems(FXCollections.observableArrayList(result.getItems()));
      previousPage.setDisable(!result.hasPreviousPage());
      nextPage.setDisable(!result.hasNextPage());
      pageInfo.setText("第 " + result.getPage() + " / " + result.getTotalPages() + " 页，共 " + result.getTotalItems() + " 位患者");
    } catch (Exception exception) {
      Ui.error(exception);
    }
  }

  private void view(Patient patient) {
    Alert alert = new Alert(Alert.AlertType.INFORMATION);
    alert.setTitle("患者档案详情 - " + patient.name);
    alert.setHeaderText(patient.name + " (" + (patient.isChild() ? "小儿患者" : "成人患者") + ") · 档案号: " + patient.patientCode);

    StringBuilder text = new StringBuilder();
    text.append("性别: ").append(patient.gender == null ? "未填" : patient.gender.getLabel()).append("\n");
    text.append("出生日期: ").append(patient.birthDate == null ? "未填" : patient.birthDate).append("\n");
    text.append("联系电话: ").append(patient.phone).append("\n");
    text.append("证件号码: ").append(patient.idNumber == null ? "未登记" : patient.idNumber).append("\n");
    text.append("预存可用金: ").append(Money.format(patient.balanceCents)).append("\n\n");
    if (patient.isChild()) {
      text.append("【法定监护人信息】\n");
      text.append("监护人姓名: ").append(patient.guardianName).append("\n");
      text.append("亲属关系: ").append(patient.guardianRelationship).append("\n");
      text.append("监护人电话: ").append(patient.guardianPhone).append("\n\n");
    }
    text.append("既往调理/病史: ").append(emptyAsDash(patient.medicalHistory)).append("\n");
    text.append("药物过敏史: ").append(emptyAsDash(patient.allergies)).append("\n");
    text.append("家庭住址: ").append(emptyAsDash(patient.address));
    alert.setContentText(text.toString());
    alert.showAndWait();
  }

  private String emptyAsDash(String value) {
    return value == null || value.trim().isEmpty() ? "无" : value;
  }

  private void openForm(Patient existing) {
    Dialog<ButtonType> dialog = new Dialog<ButtonType>();
    dialog.setTitle(existing == null ? "创建患者档案" : "编辑患者档案");
    dialog.setHeaderText("带 * 的内容为必填项；患者类型会按出生日期自动判断。");
    dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

    GridPane form = new GridPane();
    form.setHgap(12);
    form.setVgap(10);
    ComboBox<Gender> gender = new ComboBox<Gender>(FXCollections.observableArrayList(Gender.values()));
    gender.setPromptText("请选择性别");
    ComboBox<String> idType = new ComboBox<String>(FXCollections.observableArrayList(ID_TYPES));
    idType.setPromptText("请选择证件类型");
    TextField name = new TextField();
    TextField idNumber = new TextField();
    DatePicker birth = new DatePicker();
    birth.setEditable(false);
    Label patientType = new Label("请选择出生日期");
    patientType.setStyle("-fx-font-weight: bold; -fx-text-fill: #087F6D;");
    TextField phone = new TextField();
    TextField address = new TextField();
    TextField guardian = new TextField();
    TextField relationship = new TextField();
    TextField guardianPhone = new TextField();
    TextArea allergies = new TextArea();
    allergies.setPrefRowCount(2);
    TextArea medicalHistory = new TextArea();
    medicalHistory.setPrefRowCount(2);
    TextArea remark = new TextArea();
    remark.setPrefRowCount(2);

    form.addRow(0, new Label("患者类型"), patientType);
    form.addRow(1, new Label("姓名*"), name);
    form.addRow(2, new Label("性别*"), gender);
    form.addRow(3, new Label("证件类型*"), idType);
    form.addRow(4, new Label("证件号码*"), idNumber);
    form.addRow(5, new Label("出生日期*"), birth);
    form.addRow(6, new Label("联系电话*"), phone);
    form.addRow(7, new Label("联系地址"), address);
    form.addRow(8, new Label("监护人姓名（未成年人必填）"), guardian);
    form.addRow(9, new Label("关系（未成年人必填）"), relationship);
    form.addRow(10, new Label("监护人电话（未成年人必填）"), guardianPhone);
    form.addRow(11, new Label("药物过敏史"), allergies);
    form.addRow(12, new Label("既往病史"), medicalHistory);
    form.addRow(13, new Label("备注"), remark);

    birth.valueProperty().addListener((observable, oldDate, newDate) -> updatePatientType(patientType, newDate));
    if (existing != null) populateForm(existing, name, gender, idType, idNumber, birth, phone, address,
        guardian, relationship, guardianPhone, allergies, medicalHistory, remark);
    updatePatientType(patientType, birth.getValue());

    dialog.getDialogPane().setContent(form);
    Button ok = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
    ok.addEventFilter(
        javafx.event.ActionEvent.ACTION,
        event -> {
          try {
            Patient patient = existing == null ? new Patient() : copyOf(existing);
            if (existing != null) patient.id = existing.id;
            patient.name = name.getText();
            patient.gender = gender.getValue();
            patient.idType = idType.getValue();
            patient.idNumber = idNumber.getText();
            patient.birthDate = birth.getValue();
            patient.phone = phone.getText();
            patient.address = address.getText();
            patient.guardianName = guardian.getText();
            patient.guardianRelationship = relationship.getText();
            patient.guardianPhone = guardianPhone.getText();
            patient.allergies = allergies.getText();
            patient.medicalHistory = medicalHistory.getText();
            patient.remark = remark.getText();
            if (existing == null) services.patients.create(session, patient);
            else services.patients.update(session, patient);
            load();
          } catch (Exception exception) {
            event.consume();
            Ui.error(exception);
          }
        });
    dialog.showAndWait();
  }

  private void updatePatientType(Label type, LocalDate birthDate) {
    PatientType patientType = PatientType.fromBirthDate(birthDate);
    type.setText(patientType == null ? "请选择出生日期" : patientType.getLabel() + "（满 18 周岁为成年人）");
  }

  private void populateForm(
      Patient patient,
      TextField name,
      ComboBox<Gender> gender,
      ComboBox<String> idType,
      TextField idNumber,
      DatePicker birth,
      TextField phone,
      TextField address,
      TextField guardian,
      TextField relationship,
      TextField guardianPhone,
      TextArea allergies,
      TextArea medicalHistory,
      TextArea remark) {
    name.setText(patient.name);
    gender.setValue(patient.gender);
    if (patient.idType != null && !idType.getItems().contains(patient.idType)) {
      idType.getItems().add(patient.idType);
    }
    idType.setValue(patient.idType);
    idNumber.setText(patient.idNumber);
    birth.setValue(patient.birthDate);
    phone.setText(patient.phone);
    address.setText(patient.address);
    guardian.setText(patient.guardianName);
    relationship.setText(patient.guardianRelationship);
    guardianPhone.setText(patient.guardianPhone);
    allergies.setText(patient.allergies);
    medicalHistory.setText(patient.medicalHistory);
    remark.setText(patient.remark);
  }

  private Patient copyOf(Patient source) {
    Patient copy = new Patient();
    copy.id = source.id;
    copy.patientCode = source.patientCode;
    copy.patientType = source.patientType;
    copy.name = source.name;
    copy.gender = source.gender;
    copy.idType = source.idType;
    copy.idNumber = source.idNumber;
    copy.birthDate = source.birthDate;
    copy.phone = source.phone;
    copy.address = source.address;
    copy.guardianName = source.guardianName;
    copy.guardianRelationship = source.guardianRelationship;
    copy.guardianPhone = source.guardianPhone;
    copy.allergies = source.allergies;
    copy.medicalHistory = source.medicalHistory;
    copy.remark = source.remark;
    copy.balanceCents = source.balanceCents;
    return copy;
  }

  private boolean can(Permission permission) {
    try {
      Authorization.require(session, permission);
      return true;
    } catch (SecurityException e) {
      return false;
    }
  }
}
