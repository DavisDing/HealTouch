package com.healtouch.ui;

import com.healtouch.app.AppServices;
import com.healtouch.model.UserSession;
import com.healtouch.service.BackupService;
import java.io.File;
import java.util.Optional;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

public final class BackupPage {
  private final AppServices services;
  private final UserSession session;
  private final Label state=new Label();
  public BackupPage(AppServices services,UserSession session){this.services=services;this.session=session;}
  public Parent node() {
    state.setWrapText(true);
    Button configure=new Button("配置每日备份");configure.setOnAction(e->configure());
    Button backup=Ui.primaryButton("立即加密备份");backup.setOnAction(e->{try {
      Optional<char[]> password=password("输入当前备份口令");if(!password.isPresent())return;
      File result=DbWork.read(services.backups::manual,session,password.get());Ui.info("备份已完成并校验："+result);refresh();
    }catch(Exception error){Ui.error(error);refresh();}});
    Button restore=Ui.dangerButton("校验并还原完整备份");restore.setOnAction(e->restore());
    Button delete=Ui.dangerButton("删除备份");delete.setOnAction(e->{try {
      File file=choose();if(file==null)return;
      if(!Ui.confirm("确认永久删除备份 "+file.getName()+"？请确认另有可用备份。"))return;
      DbWork.write(services.backups::delete,session,file);Ui.info("备份已删除并记录审计日志。");
    }catch(Exception error){Ui.error(error);}});
    Button drill=new Button("在临时测试库执行恢复演练");drill.setOnAction(e->{try {
      File file=choose();if(file==null)return;Optional<char[]> password=password("输入所选备份包口令");if(!password.isPresent())return;
      DbWork.write(services.backups::drill,session,file,password.get());refresh();Ui.info("恢复演练通过，校验和耗时已记录。生产数据库未替换。");
    }catch(Exception error){Ui.error(error);}});
    refresh();
    return Ui.page("数据备份与还原","完整备份包含全部业务、权限及审计数据；还原将替换当前全部数据并退出所有会话。",
        state,new HBox(10,configure,backup,restore,delete),drill,
        new Label("自动备份在应用运行时按每日设定时间执行；关闭期间的备份将在下一次启动到点后补做。"),
        new Label("请将备份目录设置到独立存储，并妥善保管口令。首次配置后自动备份只使用公钥，无需保存明文口令。"));
  }
  private void refresh(){try{state.setText(DbWork.read(services.backups::summary,session)+"\n"+services.backups.problem());}catch(Exception e){Ui.error(e);}}
  private void configure() {
    java.util.Map<String,String> values;
    try{values=DbWork.read(services.backups::configuration,session);}catch(Exception error){Ui.error(error);return;}
    Dialog<ButtonType> dialog=new Dialog<ButtonType>();dialog.setTitle("每日备份设置");
    TextField directory=new TextField(values.get("directory"));
    TextField time=new TextField(values.get("time")),retention=new TextField(values.get("retention")),store=new TextField(values.get("store"));
    PasswordField password=new PasswordField();password.setPromptText("首次配置需至少12位；已有密钥保持不变");
    dialog.getDialogPane().setContent(new VBox(8,new Label("备份目录"),directory,new Label("每日时间 HH:mm"),time,new Label("自动备份保留天数（至少30）"),retention,new Label("门店标识"),store,new Label("备份口令"),password));
    dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK,ButtonType.CANCEL);
    dialog.getDialogPane().lookupButton(ButtonType.OK).addEventFilter(javafx.event.ActionEvent.ACTION,event->{try {
      DbWork.write(services.backups::configure,session,new File(directory.getText()),time.getText(),Integer.parseInt(retention.getText()),store.getText(),password.getText().toCharArray());refresh();
    }catch(Exception error){event.consume();Ui.error(error);}});Ui.show(dialog);password.clear();
  }
  private void restore() {
    services.maintenance.setPaused(true);
    try {
      File file=choose();if(file==null)return;Optional<char[]> password=password("输入所选备份包口令");if(!password.isPresent())return;
      BackupService.Inspection inspection=DbWork.read(services.backups::inspect,session,file,password.get());
      try {
        if(!Ui.confirm(inspection.summary()+"\n先创建当前数据的紧急备份，再确认还原。"))return;
        Optional<char[]> current=password("输入当前门店的备份口令，用于校验紧急备份密钥");if(!current.isPresent())return;
        File emergency=DbWork.read(services.backups::emergency,session,current.get());
        if(!Ui.confirm("紧急备份已生成："+emergency+"\n确认用已校验备份替换全部当前数据？此操作会退出所有会话。"))return;
        DbWork.write(services.backups::restore,session,inspection,emergency);
        Ui.sessionExpired.run();Ui.info("完整还原成功，请使用备份中的账号重新登录。");
      } finally {inspection.close();}
    }catch(Exception error){Ui.error(error);}finally{services.maintenance.setPaused(false);}
  }
  private File choose(){FileChooser chooser=new FileChooser();chooser.setTitle("选择 HealTouch 完整备份");chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("HealTouch 备份","*.healtouch-backup"));return chooser.showOpenDialog(DbWork.owner);}
  private Optional<char[]> password(String title) {
    Dialog<ButtonType> dialog=new Dialog<ButtonType>();dialog.setTitle(title);
    PasswordField field=new PasswordField();dialog.getDialogPane().setContent(field);
    dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK,ButtonType.CANCEL);
    Optional<ButtonType> result=Ui.show(dialog);char[] value=field.getText().toCharArray();field.clear();
    if(result.isPresent()&&result.get()==ButtonType.OK)return Optional.of(value);
    java.util.Arrays.fill(value,'\0');return Optional.empty();
  }
}
