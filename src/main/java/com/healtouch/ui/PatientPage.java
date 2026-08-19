package com.healtouch.ui;

import com.healtouch.app.AppServices;
import com.healtouch.model.Gender;
import com.healtouch.model.PageResult;
import com.healtouch.model.Patient;
import com.healtouch.model.PatientType;
import com.healtouch.model.UserSession;
import com.healtouch.util.Checks;
import com.healtouch.util.Money;
import java.util.function.Function;
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
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class PatientPage {
  private static final int PAGE_SIZE = 50;

  private final AppServices services;
  private final UserSession session;
  private final TableView<Patient> table = new TableView<Patient>();
  private final TextField search = new TextField();
  private final Label pageInfo = new Label();
  private final Button previousPage = new Button("上一页");
  private final Button nextPage = new Button("下一页");

  private int currentPage = 1;

  public PatientPage(AppServices services, UserSession session) {
    this.services = services;
    this.session = session;
  }

  public Parent node() {
    search.setPromptText("姓名、证件号码或手机号");
    Button query = new Button("查询");
    query.setOnAction(event -> searchFromFirstPage());
    search.setOnAction(event -> searchFromFirstPage());

    Button create = new Button("创建档案");
    create.setOnAction(event -> openForm(null));

    previousPage.setOnAction(
        event -> {
          if (currentPage > 1) {
            currentPage--;
            load();
          }
        });
    nextPage.setOnAction(
        event -> {
          currentPage++;
          load();
        });

    TableColumn<Patient, String> code = column("患者ID", patient -> patient.patientCode);
    TableColumn<Patient, String> name = column("姓名", patient -> patient.name);
    TableColumn<Patient, String> type =
        column("类型", patient -> patient.patientType == PatientType.CHILD ? "小儿" : "成人");
    TableColumn<Patient, String> phone = column("电话", patient -> patient.phone);
    TableColumn<Patient, String> balance =
        column("预存余额", patient -> Money.format(patient.balanceCents));
    table.getColumns().addAll(code, name, type, phone, balance);
    table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    table.setOnMouseClicked(
        event -> {
          if (event.getClickCount() == 2 && table.getSelectionModel().getSelectedItem() != null) {
            openForm(table.getSelectionModel().getSelectedItem());
          }
        });

    HBox pagination = new HBox(8, previousPage, pageInfo, nextPage);
    pagination.setPadding(new Insets(0, 0, 0, 8));
    VBox.setVgrow(table, Priority.ALWAYS);
    VBox page = Ui.page("患者管理", Ui.toolbar(search, query, create, pagination), table);
    load();
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
      PageResult<Patient> result =
          services.patients.searchPage(session, search.getText(), currentPage, PAGE_SIZE);
      currentPage = result.getPage();
      table.setItems(FXCollections.observableArrayList(result.getItems()));
      previousPage.setDisable(!result.hasPreviousPage());
      nextPage.setDisable(!result.hasNextPage());
      pageInfo.setText(
          "第 "
              + result.getPage()
              + " / "
              + result.getTotalPages()
              + " 页，共 "
              + result.getTotalItems()
              + " 位患者");
    } catch (Exception exception) {
      Ui.error(exception);
    }
  }

  private void openForm(Patient existing) {
    Dialog<ButtonType> dialog = new Dialog<ButtonType>();
    dialog.setTitle(existing == null ? "创建患者档案" : "编辑患者档案");
    dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

    GridPane form = new GridPane();
    form.setHgap(10);
    form.setVgap(8);
    ComboBox<PatientType> type =
        new ComboBox<PatientType>(FXCollections.observableArrayList(PatientType.values()));
    ComboBox<Gender> gender =
        new ComboBox<Gender>(FXCollections.observableArrayList(Gender.values()));
    TextField name = new TextField();
    TextField idType = new TextField();
    TextField idNumber = new TextField();
    DatePicker birth = new DatePicker();
    TextField phone = new TextField();
    TextField address = new TextField();
    TextField guardian = new TextField();
    TextField relationship = new TextField();
    TextField guardianPhone = new TextField();
    TextArea note = new TextArea();
    note.setPrefRowCount(2);

    form.addRow(0, new Label("患者类型*"), type);
    form.addRow(1, new Label("姓名*"), name);
    form.addRow(2, new Label("性别*"), gender);
    form.addRow(3, new Label("证件类型*"), idType);
    form.addRow(4, new Label("证件号码*"), idNumber);
    form.addRow(5, new Label("出生日期*"), birth);
    form.addRow(6, new Label("联系电话*"), phone);
    form.addRow(7, new Label("联系地址"), address);
    form.addRow(8, new Label("监护人姓名（小儿必填）"), guardian);
    form.addRow(9, new Label("关系（小儿必填）"), relationship);
    form.addRow(10, new Label("监护人电话（小儿必填）"), guardianPhone);
    form.addRow(11, new Label("备注"), note);

    if (existing != null) {
      type.setValue(existing.patientType);
      name.setText(existing.name);
      gender.setValue(existing.gender);
      idType.setText(existing.idType);
      idNumber.setText(existing.idNumber);
      birth.setValue(existing.birthDate);
      phone.setText(existing.phone);
      address.setText(existing.address);
      guardian.setText(existing.guardianName);
      relationship.setText(existing.guardianRelationship);
      guardianPhone.setText(existing.guardianPhone);
      note.setText(existing.remark);
    }

    dialog.getDialogPane().setContent(form);
    dialog.setResultConverter(
        button -> {
          if (button == ButtonType.OK) {
            try {
              Patient patient = existing == null ? new Patient() : existing;
              patient.patientType = type.getValue();
              patient.name = Checks.required(name.getText(), "姓名");
              patient.gender = gender.getValue();
              patient.idType = idType.getText();
              patient.idNumber = idNumber.getText();
              patient.birthDate = birth.getValue();
              patient.phone = phone.getText();
              patient.address = address.getText();
              patient.guardianName = guardian.getText();
              patient.guardianRelationship = relationship.getText();
              patient.guardianPhone = guardianPhone.getText();
              patient.remark = note.getText();
              if (existing == null) {
                services.patients.create(session, patient);
              } else {
                services.patients.update(session, patient);
              }
              load();
            } catch (Exception exception) {
              Ui.error(exception);
              return null;
            }
          }
          return button;
        });
    dialog.showAndWait();
  }
}
