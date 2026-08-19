package com.healtouch.ui;

import com.healtouch.app.AppServices;
import com.healtouch.model.TreatmentProject;
import com.healtouch.model.UserSession;
import com.healtouch.service.CatalogService;
import com.healtouch.util.Money;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class CatalogPage {
  private final AppServices services;
  private final UserSession session;
  private final TableView<TreatmentProject> table = new TableView<TreatmentProject>();

  public CatalogPage(AppServices services, UserSession session) {
    this.services = services;
    this.session = session;
  }

  public Parent node() {
    Button category = new Button("新增分类");
    category.setOnAction(e -> addCategory());
    Button project = new Button("新增项目");
    project.setOnAction(e -> addProject());
    Button toggle = new Button("启用/停用选中项目");
    toggle.setOnAction(
        e -> {
          try {
            TreatmentProject p = table.getSelectionModel().getSelectedItem();
            if (p == null) throw new IllegalArgumentException("请选择项目");
            services.catalog.setProjectActive(session, p.id, !p.active);
            load();
          } catch (Exception x) {
            Ui.error(x);
          }
        });
    table
        .getColumns()
        .addAll(
            col("项目名称", p -> p.name),
            col("分类", p -> p.categoryName),
            col("标准价格", p -> Money.format(p.priceCents)),
            col("状态", p -> p.active ? "启用" : "停用"));
    table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    VBox.setVgrow(table, Priority.ALWAYS);
    VBox page = Ui.page("治疗项目管理", new HBox(10, category, project, toggle), table);
    load();
    return page;
  }

  private TableColumn<TreatmentProject, String> col(
      String label, java.util.function.Function<TreatmentProject, String> getter) {
    TableColumn<TreatmentProject, String> c = new TableColumn<TreatmentProject, String>(label);
    c.setCellValueFactory(x -> new SimpleStringProperty(getter.apply(x.getValue())));
    return c;
  }

  private void load() {
    try {
      table.setItems(FXCollections.observableArrayList(services.catalog.projects(false)));
    } catch (Exception e) {
      Ui.error(e);
    }
  }

  private void addCategory() {
    Dialog<ButtonType> d = new Dialog<ButtonType>();
    d.setTitle("新增项目分类");
    TextField name = new TextField();
    TextField code = new TextField();
    d.getDialogPane().setContent(new VBox(8, new Label("分类名称"), name, new Label("分类编码（唯一）"), code));
    d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
    d.setResultConverter(
        b -> {
          if (b == ButtonType.OK) {
            try {
              services.catalog.addCategory(session, name.getText(), code.getText(), null, 100);
              Ui.info("分类已添加");
            } catch (Exception e) {
              Ui.error(e);
              return null;
            }
          }
          return b;
        });
    d.showAndWait();
  }

  private void addProject() {
    Dialog<ButtonType> d = new Dialog<ButtonType>();
    d.setTitle("新增治疗项目");
    TextField name = new TextField();
    TextField code = new TextField();
    ComboBox<CatalogService.Category> category = new ComboBox<CatalogService.Category>();
    category.setItems(FXCollections.observableArrayList(services.catalog.categories(true)));
    TextField price = new TextField();
    CheckBox active = new CheckBox("启用");
    active.setSelected(true);
    d.getDialogPane()
        .setContent(
            new VBox(
                8,
                new Label("项目名称"),
                name,
                new Label("项目编码（可选）"),
                code,
                new Label("所属分类"),
                category,
                new Label("标准价格"),
                price,
                active));
    d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
    d.setResultConverter(
        b -> {
          if (b == ButtonType.OK) {
            try {
              TreatmentProject p = new TreatmentProject();
              p.name = name.getText();
              p.code = code.getText();
              p.categoryId = category.getValue() == null ? 0 : category.getValue().id;
              p.priceCents = Money.parse(price.getText());
              p.active = active.isSelected();
              services.catalog.addProject(session, p);
              load();
              Ui.info("项目已添加");
            } catch (Exception e) {
              Ui.error(e);
              return null;
            }
          }
          return b;
        });
    d.showAndWait();
  }
}
