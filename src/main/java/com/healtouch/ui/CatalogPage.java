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
    Button category = Ui.secondaryButton("📁 分类管理/新增");
    category.setOnAction(e -> addCategory());

    Button project = Ui.primaryButton("＋ 新增治疗项目");
    project.setOnAction(e -> addProject());

    Button toggle = new Button("切换启用 / 停用状态");
    toggle.setOnAction(
        e -> {
          try {
            TreatmentProject p = table.getSelectionModel().getSelectedItem();
            if (p == null) throw new IllegalArgumentException("请选择需要更改状态的项目");
            services.catalog.setProjectActive(session, p.id, !p.active);
            load();
          } catch (Exception x) {
            Ui.error(x);
          }
        });

    Button refresh = new Button("刷新");
    refresh.setOnAction(e -> load());

    HBox tb = Ui.toolbar(refresh, Ui.spacer(), category, project, toggle);

    TableColumn<TreatmentProject, String> nameCol = col("推拿理疗项目名称", p -> p.name);
    nameCol.setPrefWidth(220);
    nameCol.setStyle("-fx-font-weight: bold;");

    TableColumn<TreatmentProject, String> codeCol = col("项目编码", p -> p.code == null || p.code.isEmpty() ? "—" : p.code);
    codeCol.setPrefWidth(120);

    TableColumn<TreatmentProject, String> catCol = col("所属分类", p -> p.categoryName);
    catCol.setPrefWidth(150);

    TableColumn<TreatmentProject, String> priceCol = col("标准收费基准价", p -> Money.format(p.priceCents));
    priceCol.setPrefWidth(140);
    priceCol.setStyle("-fx-font-weight: bold; -fx-text-fill: #087F6D;");

    TableColumn<TreatmentProject, String> statusCol = col("当前状态", p -> p.active ? "启用中" : "已停用");
    statusCol.setPrefWidth(100);
    statusCol.setCellFactory(c -> new TableCell<TreatmentProject, String>() {
      @Override
      protected void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
          setText(null);
          setGraphic(null);
        } else {
          setText(item);
          if ("启用中".equals(item)) {
            setStyle("-fx-text-fill: #15803D; -fx-font-weight: bold;");
          } else {
            setStyle("-fx-text-fill: #94A3B8;");
          }
        }
      }
    });

    table.getColumns().addAll(nameCol, codeCol, catCol, priceCol, statusCol);
    table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    VBox.setVgrow(table, Priority.ALWAYS);

    VBox page = Ui.page(
        "治疗项目与分类管理",
        "配置门诊推拿项目名称、所属专科分类、基准单价及启用状态",
        tb,
        table);
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
    d.setTitle("新增治疗项目分类");
    d.setHeaderText("录入新的专科或理疗分类类别");
    TextField name = new TextField();
    TextField code = new TextField();
    d.getDialogPane().setContent(new VBox(10, new Label("分类名称 *:"), name, new Label("分类编码 (唯一识别码) *:"), code));
    d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
    d.setResultConverter(
        b -> {
          if (b == ButtonType.OK) {
            try {
              services.catalog.addCategory(session, name.getText().trim(), code.getText().trim(), null, 100);
              Ui.info("分类创建成功！");
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
    d.setTitle("新增推拿治疗项目");
    d.setHeaderText("录入项目明细、选择分类并设定标准价格");
    TextField name = new TextField();
    TextField code = new TextField();
    ComboBox<CatalogService.Category> category = new ComboBox<CatalogService.Category>();
    category.setItems(FXCollections.observableArrayList(services.catalog.categories(true)));
    TextField price = new TextField();
    price.setPromptText("例如：120.00");
    CheckBox active = new CheckBox("立即启用此项目");
    active.setSelected(true);
    d.getDialogPane()
        .setContent(
            new VBox(
                10,
                new Label("项目名称 *:"),
                name,
                new Label("项目编码 (可选):"),
                code,
                new Label("所属分类 *:"),
                category,
                new Label("标准单价 (元) *:"),
                price,
                active));
    d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
    d.setResultConverter(
        b -> {
          if (b == ButtonType.OK) {
            try {
              TreatmentProject p = new TreatmentProject();
              p.name = name.getText().trim();
              p.code = code.getText().trim();
              p.categoryId = category.getValue() == null ? 0 : category.getValue().id;
              p.priceCents = Money.parse(price.getText());
              p.active = active.isSelected();
              services.catalog.addProject(session, p);
              load();
              Ui.info("治疗项目新增成功！");
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
