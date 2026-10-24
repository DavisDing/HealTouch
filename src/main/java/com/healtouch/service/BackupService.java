package com.healtouch.service;

import com.healtouch.config.*;
import com.healtouch.dao.*;
import com.healtouch.model.*;
import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.security.spec.*;
import java.sql.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.zip.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import javax.sql.DataSource;

/** Versioned, authenticated encrypted backups. Automatic jobs only need the public key. */
public final class BackupService {
  private static final int FORMAT=1, ITERATIONS=210000;
  private static final String MAGIC="HEALTOUCH-BACKUP";
  private static final SecureRandom RANDOM=new SecureRandom();
  private final Database database;
  private final Clock clock;
  private final File settingsFile;
  private final Properties settings=new Properties();
  private final AuditLogDao audit=new AuditLogDao();
  private volatile String problem="备份尚未配置，请管理员设置备份口令和每日时间";
  public BackupService(Database database) {this(database,Clock.systemDefaultZone());}
  public BackupService(Database database,Clock clock) {
    this.clock=clock;
    this.database=database;settingsFile=new File(database.file().getParentFile(),"backup-settings.properties");
    if(settingsFile.isFile()) {
      try(InputStream in=new FileInputStream(settingsFile)) {
        settings.load(in);
        problem=settings.getProperty("lastFailure",settings.getProperty("lastSuccess")==null?"尚未成功生成备份，请管理员立即备份":"");
      }
      catch(Exception e) {problem="读取备份设置失败："+e.getMessage();}
    }
  }
  public String problem() {
    String warning=problem;
    String drill=settings.getProperty("lastDrill");
    boolean overdue=drill==null;
    if(drill!=null)try{overdue=!LocalDate.parse(drill).plusMonths(3).isAfter(LocalDate.now(clock));}
    catch(java.time.format.DateTimeParseException invalid){overdue=true;}
    if(configured() && overdue)
      warning+=(warning.isEmpty()?"":"\n")+"请管理员执行本季度备份恢复演练";
    return warning;
  }
  public String summary(UserSession actor) {
    Authorization.require(actor,Permission.SYSTEM_MANAGE);
    return configured() ? "目录："+settings.getProperty("directory")+"；每日："+settings.getProperty("time")
        +"；保留自动备份："+settings.getProperty("retention")+"天；最近成功："+settings.getProperty("lastSuccess","尚无")
        +"；最近恢复演练："+settings.getProperty("lastDrill","尚无") : problem;
  }
  public Map<String,String> configuration(UserSession actor) {
    Authorization.require(actor,Permission.SYSTEM_MANAGE);
    Map<String,String> values=new HashMap<String,String>();
    values.put("directory",settings.getProperty("directory",new File(database.file().getParentFile(),"backups").getAbsolutePath()));
    values.put("time",settings.getProperty("time","23:00"));values.put("retention",settings.getProperty("retention","30"));values.put("store",settings.getProperty("store","Store001"));
    return values;
  }
  private boolean configured() {return settings.getProperty("publicKey")!=null;}
  public void configure(UserSession actor,File directory,String time,int retention,String store,char[] password) {
    Authorization.require(actor,Permission.SYSTEM_MANAGE);
    try {
      if(retention<30) throw new IllegalArgumentException("自动备份至少保留30天");
      LocalTime.parse(time);
      if(store==null || !store.matches("[A-Za-z0-9_-]{1,40}")) throw new IllegalArgumentException("门店标识需为1-40位字母、数字、下划线或短横线");
      requireDirectory(directory);
      if(!configured()) {
        if(password==null || password.length<12) throw new IllegalArgumentException("首次配置请设置至少12位备份口令，并妥善保存");
        KeyPairGenerator generator=KeyPairGenerator.getInstance("RSA");generator.initialize(2048);
        KeyPair pair=generator.generateKeyPair();byte[] salt=random(16),iv=random(12);
        settings.setProperty("publicKey",encode(pair.getPublic().getEncoded()));
        settings.setProperty("privateKey",encode(crypt(Cipher.ENCRYPT_MODE,derive(password,salt),iv,pair.getPrivate().getEncoded())));
        settings.setProperty("salt",encode(salt));settings.setProperty("keyIv",encode(iv));
      }
      settings.setProperty("directory",directory.getCanonicalPath());settings.setProperty("time",time);
      settings.setProperty("retention",String.valueOf(retention));settings.setProperty("store",store);
      saveSettings();record(actor.userId,"BACKUP_CONFIGURED",null,"directory="+directory+";time="+time+";retention="+retention);
      problem=settings.getProperty("lastSuccess")==null?"备份已配置，尚未成功生成备份":"";
    } catch(RuntimeException e) {throw e;}
    catch(Exception e) {throw new IllegalStateException("保存备份设置失败",e);}
    finally {if(password!=null) Arrays.fill(password,'\0');}
  }
  public File manual(UserSession actor,char[] password) {
    Authorization.require(actor,Permission.SYSTEM_MANAGE);
    try {if(!configured())throw new IllegalStateException("请先配置备份");unlock(settings,password);return create("manual",actor.userId);}
    catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException("手动备份失败",e);}
    finally {Arrays.fill(password,'\0');}
  }
  /** Runs on the application's serial DB worker; never extends any user's idle timeout. */
  void tick() {
    if(!configured()) return;
    try {
      LocalDate today=LocalDate.now(clock);
      if(LocalTime.now(clock).isBefore(LocalTime.parse(settings.getProperty("time")))) return;
      if(today.toString().equals(settings.getProperty("lastAutoDate"))) return;
      create("auto",null);
      settings.setProperty("lastAutoDate",today.toString());saveSettings();prune();
    } catch(Exception e) {
      problem="自动备份失败："+e.getMessage();settings.setProperty("lastFailure",problem);
      try{saveSettings();}catch(IOException saveFailure){e.addSuppressed(saveFailure);}
      org.slf4j.LoggerFactory.getLogger(BackupService.class).error("自动备份失败",e);
    }
  }
  private File create(String kind,Long actor) throws Exception {
    if(!configured()) throw new IllegalStateException("请先配置备份");
    File directory=new File(settings.getProperty("directory"));requireDirectory(directory);
    String id=UUID.randomUUID().toString();
    File output=new File(directory,"HealTouch_"+settings.getProperty("store")+"_"+LocalDateTime.now(clock).format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))+"_"+kind+"_"+id+".healtouch-backup");
    Path snapshot=Files.createTempFile(database.file().toPath().getParent(),"backup-snapshot-",".db");
    Path partial=Files.createTempFile(directory.toPath(),"backup-",".partial");
    Files.delete(snapshot);
    try {
      database.exclusive(()->{
        try(Connection c=database.dataSource().getConnection();PreparedStatement ps=c.prepareStatement("VACUUM INTO ?")) {
          ps.setString(1,snapshot.toString());ps.execute();
        }
        return null;
      });
      Properties manifest=new Properties();manifest.setProperty("format",String.valueOf(FORMAT));
      manifest.setProperty("id",id);manifest.setProperty("created",Instant.now(clock).toString());
      manifest.setProperty("kind",kind);manifest.setProperty("databaseSha256",hash(snapshot));
      manifest.setProperty("databaseBytes",String.valueOf(Files.size(snapshot)));manifest.putAll(inventory(snapshot.toFile()));
      Properties envelope=new Properties();
      for(String key:Arrays.asList("publicKey","privateKey","salt","keyIv")) envelope.setProperty(key,settings.getProperty(key));
      byte[] key=random(16),iv=random(12);
      PublicKey publicKey=KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(decode(settings.getProperty("publicKey"))));
      Cipher rsa=Cipher.getInstance("RSA/ECB/OAEPWithSHA-1AndMGF1Padding");rsa.init(Cipher.ENCRYPT_MODE,publicKey);
      envelope.setProperty("wrappedKey",encode(rsa.doFinal(key)));envelope.setProperty("dataIv",encode(iv));
      SecretKey recordKey=new SecretKeySpec(key,"AES");
      byte[] header=propertiesBytes(envelope);
      try(DataOutputStream out=new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(partial)))) {
        out.writeUTF(MAGIC);out.writeInt(FORMAT);out.writeInt(header.length);out.write(header);
        try(ZipOutputStream zip=new ZipOutputStream(new AuthenticatedStreams.Encrypt(out,recordKey,iv,header))) {
          zip.putNextEntry(new ZipEntry("manifest.properties"));zip.write(propertiesBytes(manifest));zip.closeEntry();
          zip.putNextEntry(new ZipEntry("database.db"));Files.copy(snapshot,zip);zip.closeEntry();
        }
      } finally {Arrays.fill(key,(byte)0);}
      String checksum=hash(partial);
      move(partial,output.toPath());
      Path checksumFile=Paths.get(output+".sha256");Files.write(checksumFile,checksum.getBytes("US-ASCII"));
      if(!checksum.equals(hash(output.toPath()))) throw new IOException("备份写入校验失败");
      record(actor,"BACKUP_CREATED",id,"kind="+kind+";file="+output.getName()+";sha256="+checksum);
      settings.setProperty("lastSuccess",Instant.now(clock).toString());settings.remove("lastFailure");saveSettings();problem="";
      return output;
    } catch(Exception e) {
      problem="备份失败："+e.getMessage();settings.setProperty("lastFailure",problem);
      try{saveSettings();}catch(IOException failure){e.addSuppressed(failure);}throw e;
    }
    finally {Files.deleteIfExists(snapshot);Files.deleteIfExists(partial);}
  }
  public static final class Inspection implements AutoCloseable {
    private final File original;
    private final Path candidate;
    private final String packageHash;
    private final Properties envelope;
    private final Properties manifest;
    private Inspection(File original,Path candidate,String packageHash,Properties manifest,Properties envelope) {
      this.original=original;this.candidate=candidate;this.packageHash=packageHash;this.manifest=manifest;this.envelope=envelope;
    }
    public String summary() {return "备份编号："+manifest.getProperty("id")+"\n生成时间："+manifest.getProperty("created")+"\n完整替换全部门诊数据。\n"+manifest.getProperty("tables");}
    @Override public void close() throws IOException {Files.deleteIfExists(candidate);}
  }
  public Inspection inspect(UserSession actor,File backup,char[] password) {
    Authorization.require(actor,Permission.SYSTEM_MANAGE);
    try{return inspect(backup,password);}
    catch(Exception e){throw new IllegalStateException("备份校验失败：口令、完整性或版本不匹配",e);}
    finally{Arrays.fill(password,'\0');}
  }
  private Inspection inspect(File backup,char[] password) throws Exception {
    if(!backup.getName().endsWith(".healtouch-backup")) throw new IOException("仅支持系统备份包");
    Path candidate=Files.createTempFile(database.file().toPath().getParent(),"restore-validated-",".db");
    boolean success=false;
    try {
      String packageHash=hash(backup.toPath());
      Properties manifest=new Properties();
      Properties envelope=new Properties();
      try(DataInputStream in=new DataInputStream(new BufferedInputStream(new FileInputStream(backup)))) {
        if(!MAGIC.equals(in.readUTF()) || in.readInt()!=FORMAT) throw new IOException("不支持的备份版本");
        int length=in.readInt();if(length<1||length>32768) throw new IOException("备份头损坏");
        byte[] header=new byte[length];in.readFully(header);envelope.load(new ByteArrayInputStream(header));
        PrivateKey privateKey=unlock(envelope,password);
        Cipher rsa=Cipher.getInstance("RSA/ECB/OAEPWithSHA-1AndMGF1Padding");rsa.init(Cipher.DECRYPT_MODE,privateKey);
        byte[] key=rsa.doFinal(decode(envelope.getProperty("wrappedKey")));
        SecretKey recordKey=new SecretKeySpec(key,"AES");Arrays.fill(key,(byte)0);
        // Drain to the authenticated final record even after ZIP's last entry.
        try(AuthenticatedStreams.Decrypt decrypted=new AuthenticatedStreams.Decrypt(in,recordKey,decode(envelope.getProperty("dataIv")),header);ZipInputStream zip=new ZipInputStream(decrypted)) {
          ZipEntry entry=zip.getNextEntry();if(entry==null||!"manifest.properties".equals(entry.getName())) throw new IOException("缺少备份清单");
          ByteArrayOutputStream bytes=new ByteArrayOutputStream();copyBounded(zip,bytes,1024*1024);manifest.load(new ByteArrayInputStream(bytes.toByteArray()));
          entry=zip.getNextEntry();if(entry==null||!"database.db".equals(entry.getName())) throw new IOException("缺少数据库");
          long expected=Long.parseLong(manifest.getProperty("databaseBytes"));
          if(expected<1 || expected>database.file().getParentFile().getUsableSpace()/2) throw new IOException("备份大小无效或空间不足");
          try(OutputStream out=Files.newOutputStream(candidate)){copyBounded(zip,out,expected);}
          if(Files.size(candidate)!=expected || zip.getNextEntry()!=null) throw new IOException("备份内容大小不匹配");
          byte[] drain=new byte[8192];while(decrypted.read(drain)!=-1) { /* verify GCM tag */ }
        }
      }
      if(!"1".equals(manifest.getProperty("format")) || !hash(candidate).equals(manifest.getProperty("databaseSha256"))) throw new IOException("数据库校验值不匹配");
      Properties actual=inventory(candidate.toFile());
      for(String key:actual.stringPropertyNames()) if(!actual.getProperty(key).equals(manifest.getProperty(key))) throw new IOException("备份清单不匹配："+key);
      // Validate checksums and versions against the migrations shipped with this binary, without migrating.
      org.flywaydb.core.Flyway.configure().dataSource("jdbc:sqlite:"+candidate.toAbsolutePath(),null,null)
          .locations("classpath:db/migration").mixed(true).load().validate();
      try(Connection c=DriverManager.getConnection("jdbc:sqlite:"+candidate.toAbsolutePath());Statement st=c.createStatement();ResultSet rs=st.executeQuery("SELECT MAX(version) FROM flyway_schema_history WHERE success=1")) {
        if(!rs.next()||!"3".equals(rs.getString(1))) throw new IOException("备份需与当前数据库版本3一致");
      }
      success=true;return new Inspection(backup,candidate,packageHash,manifest,envelope);
    } finally{if(!success) Files.deleteIfExists(candidate);}
  }
  /** Emergency backup is created before the UI's final confirmation and remains if cancelled. */
  public File emergency(UserSession actor,char[] password) {
    Authorization.require(actor,Permission.SYSTEM_MANAGE);
    try {if(!configured())throw new IllegalStateException("请先配置备份");unlock(settings,password);return create("emergency",actor.userId);}
    catch(Exception e){throw new IllegalStateException("还原前紧急备份失败，已停止还原",e);}
    finally{Arrays.fill(password,'\0');}
  }
  public void restore(UserSession actor,Inspection inspection,File emergency) {
    Authorization.require(actor,Permission.SYSTEM_MANAGE);
    try {
      if(!inspection.packageHash.equals(hash(inspection.original.toPath()))) throw new IOException("备份在预览后发生变化，请重新校验");
      if(!hash(inspection.candidate).equals(inspection.manifest.getProperty("databaseSha256"))) throw new IOException("临时数据库校验失败，请重新校验备份");
      if(emergency==null||!emergency.isFile()||!new String(Files.readAllBytes(Paths.get(emergency+".sha256")),"US-ASCII").equals(hash(emergency.toPath()))) throw new IOException("紧急备份不存在或损坏");
      database.replace(inspection.candidate.toFile(),()->{
        // Operator IDs in the old and restored snapshots may differ: retain source identity as text.
        record(null,"DATABASE_RESTORED",inspection.manifest.getProperty("id"),"sourceOperator="+actor.userId+";name="+actor.name+";emergency="+emergency.getName());return null;
      });
      Authorization.invalidateAll();
    } catch(Exception e){throw new IllegalStateException("还原失败，已尝试恢复还原前数据库",e);}
  }
  public void drill(UserSession actor,File backup,char[] password) {
    Authorization.require(actor,Permission.SYSTEM_MANAGE);long started=System.nanoTime();
    try(Inspection result=inspect(backup,password)) {
      // Exercise the same close/replace/reopen/rollback-capable path in a disposable test database.
      Path folder=Files.createTempDirectory(database.file().toPath().getParent(),"restore-drill-");
      try(Database test=new Database(folder.resolve("test.db").toFile())) {
        test.replace(result.candidate.toFile(),()->{inventory(folder.resolve("test.db").toFile());return null;});
      } finally {
        Authorization.configure(database.dataSource());
        try(java.util.stream.Stream<Path> paths=Files.walk(folder)) {paths.sorted(Comparator.reverseOrder()).forEach(path->{try{Files.delete(path);}catch(IOException e){throw new UncheckedIOException(e);}});}
      }
      settings.setProperty("lastDrill",LocalDate.now(clock).toString());saveSettings();
      record(actor.userId,"BACKUP_RESTORE_DRILL",result.manifest.getProperty("id"),"elapsedMs="+(System.nanoTime()-started)/1000000+";integrity=OK;testDatabase=OK");
    }catch(Exception e){throw new IllegalStateException("恢复演练失败",e);}finally{Arrays.fill(password,'\0');}
  }

  public static final class Recovery implements AutoCloseable {
    private final Inspection inspection;
    private final String operator;
    private File emergency;
    private String emergencyHash;
    private Recovery(Inspection inspection,String operator){this.inspection=inspection;this.operator=operator;}
    public String summary(){return inspection.summary();}
    @Override public void close() throws IOException {inspection.close();}
  }

  /** Recovery requires both the backup passphrase and an active admin's credentials in that backup. */
  public Recovery prepareRecovery(File backup,char[] password,String login,String adminPassword) {
    if(!database.recoveryRequired())throw new SecurityException("数据库可用，请登录后使用管理员还原入口");
    Inspection inspection=null;
    try {
      String locked=settings.getProperty("recoveryLockedUntil");
      if(locked!=null && LocalDateTime.parse(locked).isAfter(LocalDateTime.now(clock)))throw new RecoveryLocked();
      if(locked!=null){settings.remove("recoveryLockedUntil");settings.setProperty("recoveryFailures","0");}
      inspection=inspect(backup,password);
      try(Connection c=DriverManager.getConnection("jdbc:sqlite:"+inspection.candidate.toAbsolutePath());
          PreparedStatement ps=c.prepareStatement("SELECT u.id,u.name,u.password_hash,u.locked_until FROM app_user u WHERE u.login_name=? AND u.active=1 AND EXISTS(SELECT 1 FROM user_role ur JOIN role r ON r.id=ur.role_id WHERE ur.user_id=u.id AND r.code='ADMIN')")) {
        ps.setString(1,login);
        try(ResultSet rs=ps.executeQuery()) {
          if(!rs.next() || !org.mindrot.jbcrypt.BCrypt.checkpw(adminPassword,rs.getString(3))
              || (rs.getString(4)!=null && LocalDateTime.parse(rs.getString(4)).isAfter(LocalDateTime.now(clock))))
            throw new SecurityException("备份中的管理员账号或密码无效");
          settings.remove("recoveryLockedUntil");settings.setProperty("recoveryFailures","0");saveSettings();
          return new Recovery(inspection,rs.getLong(1)+":"+rs.getString(2));
        }
      }
    }catch(Exception e){
      if(!(e instanceof RecoveryLocked)) {
        int failures=0;
        try{failures=Integer.parseInt(settings.getProperty("recoveryFailures","0"));}catch(NumberFormatException invalid){/* reset invalid local counter */}
        failures=Math.min(5,Math.max(0,failures)+1);
        settings.setProperty("recoveryFailures",String.valueOf(failures));
        if(failures>=5)settings.setProperty("recoveryLockedUntil",LocalDateTime.now(clock).plusMinutes(15).toString());
        try{saveSettings();}catch(IOException saveFailure){e.addSuppressed(saveFailure);}
      }
      if(inspection!=null)try{inspection.close();}catch(IOException cleanup){e.addSuppressed(cleanup);}
      throw new IllegalStateException("受控恢复校验失败"+(e instanceof RecoveryLocked?"，连续失败已锁定15分钟":""),e);
    }
    finally{Arrays.fill(password,'\0');}
  }
  private static final class RecoveryLocked extends SecurityException {
    RecoveryLocked(){super("受控恢复连续失败已锁定15分钟");}
  }

  /** Corrupt originals are preserved byte-for-byte, including journals, in an encrypted evidence ZIP. */
  public File recoveryEmergency(Recovery recovery) {
    if(!database.recoveryRequired())throw new SecurityException("当前不处于恢复模式");
    try {
      Properties header=new Properties();header.putAll(recovery.inspection.envelope);
      byte[] key=random(16),iv=random(12);
      PublicKey publicKey=KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(decode(header.getProperty("publicKey"))));
      Cipher rsa=Cipher.getInstance("RSA/ECB/OAEPWithSHA-1AndMGF1Padding");rsa.init(Cipher.ENCRYPT_MODE,publicKey);
      header.setProperty("wrappedKey",encode(rsa.doFinal(key)));header.setProperty("dataIv",encode(iv));
      SecretKey recordKey=new SecretKeySpec(key,"AES");Arrays.fill(key,(byte)0);
      File folder=new File(database.file().getParentFile(),"backups");requireDirectory(folder);
      File file=new File(folder,"HealTouch_corrupt_emergency_"+UUID.randomUUID()+".healtouch-emergency");
      Path partial=Files.createTempFile(folder.toPath(),"corrupt-emergency-",".partial");
      try {
        byte[] headerBytes=propertiesBytes(header);
        try(DataOutputStream out=new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(partial)))) {
          out.writeUTF("HEALTOUCH-CORRUPT-EVIDENCE");out.writeInt(FORMAT);out.writeInt(headerBytes.length);out.write(headerBytes);
          try(ZipOutputStream zip=new ZipOutputStream(new AuthenticatedStreams.Encrypt(out,recordKey,iv,headerBytes))) {
            zip.putNextEntry(new ZipEntry("WARNING.txt"));zip.write("Unverified corrupt database originals. For technical recovery only; not a restorable system backup.".getBytes("UTF-8"));zip.closeEntry();
            for(String suffix:new String[]{"","-wal","-shm","-journal"}) {
              Path original=Paths.get(database.file()+suffix);
              if(Files.exists(original)){zip.putNextEntry(new ZipEntry("healtouch.db"+suffix));Files.copy(original,zip);zip.closeEntry();}
            }
          }
        }
        String checksum=hash(partial);move(partial,file.toPath());
        if(!checksum.equals(hash(file.toPath())))throw new IOException("损坏原件紧急副本校验失败");
        recovery.emergency=file;recovery.emergencyHash=checksum;return file;
      }finally{Files.deleteIfExists(partial);}
    }catch(Exception e){throw new IllegalStateException("损坏数据库紧急副本生成失败，已停止恢复",e);}
  }

  public void recover(Recovery recovery) {
    if(!database.recoveryRequired())throw new SecurityException("当前不处于恢复模式");
    try {
      if(recovery.emergency==null || !recovery.emergencyHash.equals(hash(recovery.emergency.toPath())))throw new IOException("缺少有效紧急副本");
      if(!hash(recovery.inspection.candidate).equals(recovery.inspection.manifest.getProperty("databaseSha256")))throw new IOException("临时还原数据库校验失败");
      database.replace(recovery.inspection.candidate.toFile(),()->{
        record(null,"CORRUPT_DATABASE_RECOVERED",recovery.inspection.manifest.getProperty("id"),"backupAdmin="+recovery.operator+";emergency="+recovery.emergency.getName());return null;
      });
      Authorization.configure(database.dataSource());Authorization.invalidateAll();
    }catch(Exception e){throw new IllegalStateException("恢复失败，损坏原件紧急副本已保留",e);}
  }
  private void prune() throws Exception {
    File directory=new File(settings.getProperty("directory"));File[] files=directory.listFiles((dir,name)->name.matches("HealTouch_[A-Za-z0-9_-]{1,40}_[0-9]{8}_[0-9]{6}_auto_[0-9a-f-]{36}\\.healtouch-backup"));
    if(files==null) throw new IOException("无法读取备份目录");
    long cutoff=clock.millis()-Integer.parseInt(settings.getProperty("retention"))*86400000L;
    for(File file:files) if(file.lastModified()<cutoff) {
      record(null,"AUTO_BACKUP_RETENTION_DELETE_REQUESTED",file.getName(),"retention="+settings.getProperty("retention"));
      Files.delete(file.toPath());Files.deleteIfExists(Paths.get(file+".sha256"));
      record(null,"AUTO_BACKUP_RETENTION_DELETED",file.getName(),"retention="+settings.getProperty("retention"));
    }
  }

  public void delete(UserSession actor,File file) {
    Authorization.require(actor,Permission.SYSTEM_MANAGE);
    try {
      if(!file.getCanonicalFile().getParentFile().equals(new File(settings.getProperty("directory")).getCanonicalFile()) || !file.getName().endsWith(".healtouch-backup"))
        throw new IllegalArgumentException("只能删除当前备份目录中的系统备份包");
      record(actor.userId,"BACKUP_DELETE_REQUESTED",file.getName(),"operatorConfirmed=true");
      Files.delete(file.toPath());Files.deleteIfExists(Paths.get(file+".sha256"));
      record(actor.userId,"BACKUP_DELETED",file.getName(),"operatorConfirmed=true");
    } catch(Exception e){throw new IllegalStateException("删除备份失败",e);}
  }
  private Properties inventory(File file) throws Exception {
    Properties result=new Properties();
    try(Connection c=DriverManager.getConnection("jdbc:sqlite:"+file.getAbsolutePath());Statement st=c.createStatement()) {
      try(ResultSet rs=st.executeQuery("PRAGMA integrity_check")){if(!rs.next()||!"ok".equals(rs.getString(1))) throw new IOException("SQLite完整性检查失败");}
      try(ResultSet rs=st.executeQuery("PRAGMA foreign_key_check")){if(rs.next()) throw new IOException("数据库外键完整性检查失败");}
      List<String> tables=new ArrayList<String>();
      try(ResultSet rs=st.executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' ORDER BY name")){while(rs.next())tables.add(rs.getString(1));}
      for(String table:tables) try(ResultSet rs=st.executeQuery("SELECT COUNT(*) FROM \""+table.replace("\"","\"\"")+"\"")){rs.next();result.setProperty("count."+table,String.valueOf(rs.getLong(1)));}
      result.setProperty("tables",String.join(", ",tables));
    }return result;
  }
  private void record(Long actor,String action,String target,String details) {
    Jdbc.inTransaction(database.dataSource(),c->{audit.record(c,actor,action,"BACKUP",target,null,details);return null;});
  }
  private void requireDirectory(File directory) throws IOException {
    if(!directory.isDirectory()&&!directory.mkdirs()) throw new IOException("无法创建备份目录");
    if(directory.getUsableSpace()<Math.max(32*1024*1024L,database.file().length()*3)) throw new IOException("备份目录空间不足");
    Path probe=Files.createTempFile(directory.toPath(),"write-check-",".tmp");Files.delete(probe);
  }
  private void saveSettings() throws IOException {
    Path temp=Files.createTempFile(settingsFile.toPath().getParent(),"backup-settings-",".tmp");
    try {try(OutputStream out=Files.newOutputStream(temp)){settings.store(out,"No plaintext passphrase or private key");}move(temp,settingsFile.toPath());}
    finally{Files.deleteIfExists(temp);}
  }
  private static void move(Path from,Path to) throws IOException {
    try{Files.move(from,to,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
    catch(AtomicMoveNotSupportedException e){Files.move(from,to,StandardCopyOption.REPLACE_EXISTING);}
  }
  private static PrivateKey unlock(Properties p,char[] password) throws Exception {
    byte[] plain=crypt(Cipher.DECRYPT_MODE,derive(password,decode(p.getProperty("salt"))),decode(p.getProperty("keyIv")),decode(p.getProperty("privateKey")));
    try{return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(plain));}finally{Arrays.fill(plain,(byte)0);}
  }
  private static SecretKey derive(char[] password,byte[] salt) throws Exception {
    PBEKeySpec spec=new PBEKeySpec(password,salt,ITERATIONS,128);
    try{return new SecretKeySpec(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded(),"AES");}finally{spec.clearPassword();}
  }
  private static Cipher aes(int mode,SecretKey key,byte[] iv) throws Exception {Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(mode,key,new GCMParameterSpec(128,iv));return cipher;}
  private static byte[] crypt(int mode,SecretKey key,byte[] iv,byte[] data) throws Exception {return aes(mode,key,iv).doFinal(data);}
  private static byte[] random(int length){byte[] bytes=new byte[length];RANDOM.nextBytes(bytes);return bytes;}
  private static String encode(byte[] bytes){return Base64.getEncoder().encodeToString(bytes);}
  private static byte[] decode(String value){return Base64.getDecoder().decode(value);}
  private static byte[] propertiesBytes(Properties p) throws IOException {ByteArrayOutputStream out=new ByteArrayOutputStream();p.store(out,null);return out.toByteArray();}
  private static String hash(Path path) throws Exception {
    MessageDigest digest=MessageDigest.getInstance("SHA-256");
    try(InputStream in=Files.newInputStream(path)){byte[] bytes=new byte[65536];int n;while((n=in.read(bytes))!=-1)digest.update(bytes,0,n);}
    StringBuilder out=new StringBuilder();for(byte b:digest.digest())out.append(String.format("%02x",b&255));return out.toString();
  }
  private static void copyBounded(InputStream in,OutputStream out,long maximum) throws IOException {
    byte[] bytes=new byte[65536];long total=0;int n;
    while((n=in.read(bytes))!=-1){total+=n;if(total>maximum)throw new IOException("备份内容超出清单限制");out.write(bytes,0,n);}
  }
}
