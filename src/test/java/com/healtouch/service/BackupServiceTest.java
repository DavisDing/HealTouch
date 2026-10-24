package com.healtouch.service;

import static org.junit.Assert.*;
import com.healtouch.config.Database;
import com.healtouch.model.*;
import java.io.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.time.*;
import org.junit.*;
import org.junit.rules.TemporaryFolder;

public class BackupServiceTest {
  @Rule public TemporaryFolder folder=new TemporaryFolder();
  private Database db;
  private BackupService backups;
  private AuthService auth;
  private UserSession admin;
  private File dbFile;
  private static char[] pass(){return "BackupSecret@123456".toCharArray();}
  @Before public void setup() throws Exception {
    dbFile=new File(folder.getRoot(),"healtouch.db");db=new Database(dbFile);
    auth=new AuthService(db.dataSource());auth.initializeAdministrator("AdminTest@123");admin=auth.login("admin","AdminTest@123");
    backups=new BackupService(db,Clock.fixed(Instant.parse("2026-10-24T12:00:00Z"),ZoneId.of("Asia/Shanghai")));backups.configure(admin,new File(folder.getRoot(),"backups"),"00:00",30,"TestStore",pass());
  }
  @After public void close(){if(db!=null)db.close();Authorization.configure(null);}
  private long users() throws Exception {try(Connection c=db.dataSource().getConnection();Statement st=c.createStatement();ResultSet rs=st.executeQuery("SELECT COUNT(*) FROM app_user")){rs.next();return rs.getLong(1);}}

  @Test public void encryptedBackupRoundTripRestoresSnapshotAndInvalidatesSessions() throws Exception {
    File file=backups.manual(admin,pass());assertTrue(file.getName().endsWith(".healtouch-backup"));
    byte[] bytes=Files.readAllBytes(file.toPath());assertFalse(new String(bytes,"ISO-8859-1").contains("SQLite format 3"));
    new UserService(db.dataSource()).create(admin,"later","later","","UserTest@123",EnumSet.of(RoleCode.RECEPTION));assertEquals(2,users());
    try(BackupService.Inspection inspection=backups.inspect(admin,file,pass())) {
      File emergency=backups.emergency(admin,pass());assertTrue(emergency.isFile());
      backups.restore(admin,inspection,emergency);
    }
    assertEquals(1,users());assertFalse(admin.isValid());
    assertTrue(auth.login("admin","AdminTest@123").isAdmin());
  }
  @Test public void badPasswordAndModifiedCiphertextFailWithoutChangingDatabase() throws Exception {
    File original=backups.manual(admin,pass());
    assertThrows(IllegalStateException.class,()->backups.inspect(admin,original,"WrongSecret@123456".toCharArray()));
    Path corrupted=folder.getRoot().toPath().resolve("corrupted.healtouch-backup");byte[] bytes=Files.readAllBytes(original.toPath());bytes[bytes.length-1]^=1;Files.write(corrupted,bytes);
    assertThrows(IllegalStateException.class,()->backups.inspect(admin,corrupted.toFile(),pass()));assertEquals(1,users());
  }
  @Test public void restoreRequiresValidEmergencyBackupAndDrillUsesDisposableDatabase() throws Exception {
    File file=backups.manual(admin,pass());
    try(BackupService.Inspection inspection=backups.inspect(admin,file,pass())) {
      assertThrows(IllegalStateException.class,()->backups.restore(admin,inspection,null));assertEquals(1,users());
    }
    backups.drill(admin,file,pass());assertEquals(1,users());assertTrue(Authorization.allowed(admin,Permission.SYSTEM_MANAGE));
    assertTrue(backups.summary(admin).contains("最近恢复演练"));
  }
  @Test public void automaticBackupNeedsNoPlaintextPasswordAndRunsOncePerDay() {
    backups.tick();backups.tick();
    File[] files=new File(folder.getRoot(),"backups").listFiles((dir,name)->name.contains("_auto_")&&name.endsWith(".healtouch-backup"));
    assertNotNull(files);assertEquals(1,files.length);
  }
  @Test public void retentionDeletesOnlyOldAutomaticPackagesEvenWhenStoreNameContainsAuto() throws Exception {
    backups.configure(admin,new File(folder.getRoot(),"backups"),"00:00",30,"Store_auto_branch",pass());
    File manual=backups.manual(admin,pass());
    long old=Instant.parse("2026-09-01T00:00:00Z").toEpochMilli();assertTrue(manual.setLastModified(old));
    File automatic=new File(manual.getParentFile(),"HealTouch_Store_auto_branch_20260901_000000_auto_"+UUID.randomUUID()+".healtouch-backup");
    Files.write(automatic.toPath(),new byte[]{1});assertTrue(automatic.setLastModified(old));
    backups.tick();assertTrue(manual.isFile());assertFalse(automatic.exists());
  }
  @Test public void retentionConfigurationRejectsLessThanThirtyDays() {
    assertThrows(IllegalArgumentException.class,()->backups.configure(admin,new File(folder.getRoot(),"backups"),"23:00",29,"TestStore",pass()));
  }
  @Test public void corruptStartupOffersAuthenticatedRecoveryAndPreservesEncryptedOriginals() throws Exception {
    File backup=backups.manual(admin,pass());db.close();Files.write(dbFile.toPath(),"damaged SQLite database".getBytes("UTF-8"));
    db=new Database(dbFile,true);assertTrue(db.recoveryRequired());backups=new BackupService(db);
    assertThrows(IllegalStateException.class,()->backups.prepareRecovery(backup,pass(),"admin","Wrong@123"));
    try(BackupService.Recovery recovery=backups.prepareRecovery(backup,pass(),"admin","AdminTest@123")) {
      File emergency=backups.recoveryEmergency(recovery);assertTrue(emergency.getName().endsWith(".healtouch-emergency"));
      backups.recover(recovery);assertFalse(db.recoveryRequired());assertEquals(1,users());
    }
    assertTrue(new AuthService(db.dataSource()).login("admin","AdminTest@123").isAdmin());
  }
  @Test public void recoveryAuthenticationLockPersistsAcrossServiceRestart() throws Exception {
    File backup=backups.manual(admin,pass());db.close();Files.write(dbFile.toPath(),"damaged".getBytes("UTF-8"));
    db=new Database(dbFile,true);backups=new BackupService(db);
    for(int i=0;i<5;i++)assertThrows(IllegalStateException.class,()->backups.prepareRecovery(backup,pass(),"admin","Wrong@123"));
    backups=new BackupService(db);
    IllegalStateException locked=assertThrows(IllegalStateException.class,()->backups.prepareRecovery(backup,pass(),"admin","AdminTest@123"));
    assertTrue(locked.getMessage().contains("锁定15分钟"));
  }
}
