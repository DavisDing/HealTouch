package com.healtouch.ui;

import com.healtouch.app.AppServices;
import com.healtouch.model.*;
import javafx.collections.FXCollections;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/** Reuses the paged service so every patient remains selectable as the archive grows. */
public final class PatientPicker extends VBox {
  private final AppServices services;
  private final UserSession session;
  private final ComboBox<Patient> choice = new ComboBox<Patient>();
  private final TextField keyword = new TextField();
  private final Label status = new Label();
  private final Button previous = new Button("上一页"), next = new Button("下一页");
  private int page = 1;
  public PatientPicker(AppServices services, UserSession session) {
    super(6); this.services=services; this.session=session;
    keyword.setPromptText("姓名、证件号或手机号");
    keyword.setPrefWidth(200);
    Button search=new Button("搜索");
    search.setOnAction(e->{page=1;load();});keyword.setOnAction(e->{page=1;load();});
    previous.setOnAction(e->{page--;load();});next.setOnAction(e->{page++;load();});
    choice.setPrefWidth(280);choice.setPromptText("请选择搜索结果中的患者");
    getChildren().addAll(new HBox(6,keyword,search),choice,new HBox(6,previous,status,next));
    load();
  }
  public ComboBox<Patient> choice() { return choice; }
  public Patient getValue() { return choice.getValue(); }
  private void load() {
    try {
      Patient selected=choice.getValue();
      PageResult<Patient> result=DbWork.read(services.patients::searchPage,session,keyword.getText(),page,50);
      page=result.getPage();choice.setItems(FXCollections.observableArrayList(result.getItems()));
      choice.setValue(null);
      if(selected!=null) for(Patient p:result.getItems()) if(p.id.equals(selected.id)) {choice.setValue(p);break;}
      previous.setDisable(!result.hasPreviousPage());next.setDisable(!result.hasNextPage());
      status.setText("第"+page+"/"+result.getTotalPages()+"页 · 共"+result.getTotalItems()+"人");
      choice.setDisable(result.getItems().isEmpty());
      if(result.getItems().isEmpty()) status.setText("未找到患者，请更换关键词或先建档");
    } catch(Exception e) {
      choice.getItems().clear();choice.setValue(null);choice.setDisable(true);
      status.setText("加载失败，可点击搜索重试");previous.setDisable(true);next.setDisable(true);Ui.error(e);
    }
  }
}
