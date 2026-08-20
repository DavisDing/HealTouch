package com.healtouch.ui;

import com.healtouch.app.AppServices;
import com.healtouch.model.Gender;
import com.healtouch.model.PageResult;
import com.healtouch.model.Patient;
import com.healtouch.model.PatientType;
import com.healtouch.model.UserSession;
import com.healtouch.util.Money;
import java.time.LocalDate;
import java.util.function.Function;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
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
  private static final String ID_CARD = "身份证";
  private static final String MEDICAL_CARD = "病历卡";

  private final AppServices services;
  private final UserSession session;
  private final boolean openCreateOnLoad;
  private final TableView<Patient> table = new TableView<Patient>();
  private final TextField search = new TextField();
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
    search.setPrefWidth(240);
    Button query = new Button("查询");
    query.setOnAction(event -> searchFromFirstPage());
    search.setOnAction(event -> searchFromFirstPage());
    Button create = Ui.primaryButton("＋ 创建档案");
    create.setOnAction(event -> openForm(null));

    previousPage.setOnAction(event -> { if (currentPage > 1) { currentPage--; load(); } });
    nextPage.setOnAction(event -> { currentPage++; load(); });

    table.getColumns().addAll(
        column("患者ID", patient -> patient.patientCode),
        column("姓名", patient -> patient.name),
        column("患者类型", patient -> patient.patientType == null ? "—" : patient.patientType.getLabel()),
        column("性别", patient -> patient.gender == null ? "—" : patient.gender.getLabel()),
        column("电话", patient -> patient.phone),
        column("预存余额", patient -> Money.format(patient.balanceCents)));
    table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    table.setOnMouseClicked(event -> {
      if (event.getClickCount() == 2 && table.getSelectionModel().getSelectedItem() != null)
        openForm(table.getSelectionModel().getSelectedItem());
    });

    HBox pagination = new HBox(8, previousPage, pageInfo, nextPage);
    pagination.setPadding(new Insets(0, 0, 0, 8));
    VBox.setVgrow(table, Priority.ALWAYS);
    VBox page = Ui.page("患者档案", Ui.toolbar(search, query, create, pagination), table);
    load();
    if (openCreateOnLoad) Platform.runLater(() -> openForm(null));
    return page;
  }

  private TableColumn<Patient, String> column(String label, Function<Patient, String> getter) {
    TableColumn<Patient, String> column = new TableColumn<Patient, String>(label);
    column.setCellValueFactory(cell -> new SimpleStringProperty(getter.apply(cell.getValue())));
    return column;
  }

  private void searchFromFirstPage() { currentPage = 1; load(); }

  private void load() {
    try {
      PageResult<Patient> result = services.patients.searchPage(session, search.getText(), currentPage, PAGE_SIZE);
      currentPage = result.getPage();
      table.setItems(FXCollections.observableArrayList(result.getItems()));
      previousPage.setDisable(!result.hasPreviousPage());
      nextPage.setDisable(!result.hasNextPage());
      pageInfo.setText("第 " + result.getPage() + " / " + result.getTotalPages() + " 页，共 " + result.getTotalItems() + " 位患者");
    } catch (Exception exception) { Ui.error(exception); }
  }

  private void openForm(Patient existing) {
    Dialog<ButtonType> dialog = new Dialog<ButtonType>();
    dialog.setTitle(existing == null ? "创建患者档案" : "编辑患者档案");
    dialog.setHeaderText("带 * 的内容为必填项；患者类型会按出生日期自动判断。");
    dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

    GridPane form = new GridPane();
    form.setHgap(12);
    form.setVgap(10);
    Label type = new Label("请选择出生日期");
    type.setStyle("-fx-font-weight: bold; -fx-text-fill: #087f6d;");
    ComboBox<Gender> gender = new ComboBox<Gender>(FXCollections.observableArrayList(Gender.values()));
    gender.setPromptText("请选择性别");
    ComboBox<String> idType = new ComboBox<String>(FXCollections.observableArrayList(ID_CARD, MEDICAL_CARD));
    idType.setPromptText("请选择证件类型");
    TextField name = new TextField();
    TextField idNumber = new TextField();
    DatePicker birth = new DatePicker();
    birth.setEditable(false);
    TextField phone = new TextField();
    TextField address = new TextField();
    TextField guardian = new TextField();
    TextField relationship = new TextField();
    TextField guardianPhone = new TextField();
    TextArea note = new TextArea();
    note.setPrefRowCount(2);

    form.addRow(0, new Label("患者类型"), type);
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
    form.addRow(11, new Label("备注"), note);

    birth.valueProperty().addListener((observable, oldDate, newDate) -> updatePatientType(type, newDate));
    if (existing != null) {
      name.setText(existing.name);
      gender.setValue(existing.gender);
      idType.setValue(existing.idType);
      idNumber.setText(existing.idNumber);
      birth.setValue(existing.birthDate);
      phone.setText(existing.phone);
      address.setText(existing.address);
      guardian.setText(existing.guardianName);
      relationship.setText(existing.guardianRelationship);
      guardianPhone.setText(existing.guardianPhone);
      note.setText(existing.remark);
    }
    updatePatientType(type, birth.getValue());

    dialog.getDialogPane().setContent(form);
    Button ok = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
    ok.addEventFilter(
        javafx.event.ActionEvent.ACTION,
        event -> {
          try {
            Patient patient = existing == null ? new Patient() : existing;
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
            patient.remark = note.getText();
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
}
