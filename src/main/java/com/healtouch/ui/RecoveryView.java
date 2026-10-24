package com.healtouch.ui;

import com.healtouch.app.AppServices;
import com.healtouch.service.BackupService;
import java.io.File;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

/** Available when the current DB cannot initialize; credentials are verified against a validated backup. */
public final class RecoveryView {
  private final VBox root;
  public RecoveryView(AppServices services,Runnable done) {
    Label message=new Label("本地数据库初始化失败。请保留数据目录并联系管理员，或从已验证的完整备份恢复。\n恢复需要备份口令，以及该备份中的启用管理员账号和密码。");message.setWrapText(true);
    TextField path=new TextField();path.setEditable(false);
    Button choose=new Button("选择完整备份");choose.setOnAction(e->{
      FileChooser chooser=new FileChooser();chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("HealTouch 完整备份","*.healtouch-backup"));
      File file=chooser.showOpenDialog(DbWork.owner);if(file!=null)path.setText(file.getAbsolutePath());
    });
    TextField account=new TextField();account.setPromptText("备份中的管理员账号");
    PasswordField adminPassword=new PasswordField();adminPassword.setPromptText("备份中的管理员登录密码");
    PasswordField backupPassword=new PasswordField();backupPassword.setPromptText("备份包口令");
    Button recover=Ui.dangerButton("校验备份与管理员身份");recover.setOnAction(e->{
      try {
        if(path.getText().isEmpty())throw new IllegalArgumentException("请先选择完整备份");
        BackupService.Recovery preparation=DbWork.read(services.backups::prepareRecovery,new File(path.getText()),backupPassword.getText().toCharArray(),account.getText(),adminPassword.getText());
        backupPassword.clear();adminPassword.clear();
        try {
          if(!Ui.confirm(preparation.summary()+"\n确认先保存当前损坏数据库的加密紧急副本？"))return;
          File emergency=DbWork.read(services.backups::recoveryEmergency,preparation);
          if(!Ui.confirm("损坏原件加密副本已保存："+emergency+"\n此副本需使用所选备份的口令解密，仅供技术恢复。\n确认使用所选完整备份替换当前数据？"))return;
          DbWork.write(services.backups::recover,preparation);done.run();Ui.info("数据库已恢复，请重新登录。");
        }finally{preparation.close();}
      }catch(Exception error){Ui.error(error);}
    });
    root=Ui.page("数据库恢复",message,path,choose,account,adminPassword,backupPassword,recover,
        new Label("详细错误保存在数据目录 logs。没有可用完整备份时，请保存原件并联系技术支持。"));
  }
  public Parent node(){return root;}
}
