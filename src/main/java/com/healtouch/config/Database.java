package com.healtouch.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.*;
import java.nio.file.*;
import java.sql.*;
import java.lang.reflect.*;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;

/** A stable DataSource facade permits an exclusive, reversible full-database restore. */
public final class Database implements AutoCloseable {
  private static final java.util.Set<String> INSTANCE_PATHS=new java.util.HashSet<String>();
  private final File file;
  private volatile HikariDataSource pool;
  private volatile RuntimeException startupFailure;
  private RandomAccessFile instanceFile;
  private java.nio.channels.FileLock instanceLock;
  private String instanceIdentity;
  private final ReentrantReadWriteLock lock=new ReentrantReadWriteLock(true);
  private final DataSource source=new DataSource() {
    public Connection getConnection() throws SQLException {
      lock.readLock().lock();
      try {
        if(pool==null) throw new SQLException("数据库需要从备份恢复");
        Connection connection=pool.getConnection();
        return (Connection)Proxy.newProxyInstance(Connection.class.getClassLoader(),new Class<?>[]{Connection.class},new InvocationHandler() {
          private boolean closed;
          public Object invoke(Object proxy,Method method,Object[] args) throws Throwable {
            if(method.getName().equals("close")) {
              if(!closed) {closed=true;try {connection.close();} finally {lock.readLock().unlock();}}
              return null;
            }
            try{return method.invoke(connection,args);}catch(InvocationTargetException e){throw e.getCause();}
          }
        });
      } catch(SQLException | RuntimeException e) {lock.readLock().unlock();throw e;}
    }
    public Connection getConnection(String user,String password) throws SQLException {return getConnection();}
    public PrintWriter getLogWriter() throws SQLException {return pool.getLogWriter();}
    public void setLogWriter(PrintWriter writer) throws SQLException {pool.setLogWriter(writer);}
    public void setLoginTimeout(int seconds) throws SQLException {pool.setLoginTimeout(seconds);}
    public int getLoginTimeout() throws SQLException {return pool.getLoginTimeout();}
    public Logger getParentLogger() {return Logger.getLogger("com.healtouch.database");}
    public <T>T unwrap(Class<T> type) throws SQLException {if(type.isInstance(this)) return type.cast(this);throw new SQLException("不支持解包");}
    public boolean isWrapperFor(Class<?> type) {return type.isInstance(this);}
  };
  public Database() {this(AppPaths.databaseFile());}
  public Database(File file) {this(file,false);}
  public Database(File file,boolean permitRecovery) {
    try{this.file=file.getCanonicalFile();}catch(IOException e){throw new IllegalStateException("无法解析数据库路径",e);}
    synchronized(INSTANCE_PATHS) {
      String identity=this.file.getAbsolutePath();
      if(!INSTANCE_PATHS.add(identity))throw new IllegalStateException("数据库已在当前进程中打开");
      instanceIdentity=identity;
    }
    try {
      instanceFile=new RandomAccessFile(this.file+".lock","rw");
      instanceLock=instanceFile.getChannel().tryLock();
      if(instanceLock==null)throw new IOException("数据库已被另一实例占用");
    }catch(IOException | java.nio.channels.OverlappingFileLockException e) {
      releaseInstanceLock();throw new IllegalStateException("无法独占本地数据库，请关闭其他 HealTouch 实例后重试",e);
    }
    try {
      open();
      Flyway.configure().dataSource(source).locations("classpath:db/migration").mixed(true).load().migrate();
      com.healtouch.service.Authorization.configure(source);
    } catch(RuntimeException e) {
      if(pool!=null)pool.close();pool=null;
      if(!permitRecovery){releaseInstanceLock();throw e;}
      startupFailure=e;
      org.slf4j.LoggerFactory.getLogger(Database.class).error("数据库初始化失败，已进入受控恢复模式",e);
    }
  }
  private void open() {
    HikariConfig config=new HikariConfig();
    config.setJdbcUrl("jdbc:sqlite:"+file.getAbsolutePath());config.setDriverClassName("org.sqlite.JDBC");
    config.setMaximumPoolSize(1);config.setMinimumIdle(1);config.setConnectionTimeout(10_000);
    config.setConnectionInitSql("PRAGMA foreign_keys=ON");
    config.addDataSourceProperty("busy_timeout","10000");
    pool=new HikariDataSource(config);
  }
  public File file() {return file;}
  public boolean recoveryRequired(){return startupFailure!=null;}
  public DataSource dataSource() {return source;}
  public <T>T exclusive(Callable<T> work) throws Exception {
    lock.writeLock().lock();try{return work.call();}finally{lock.writeLock().unlock();}
  }
  /** Caller must validate the candidate and create an emergency backup before entering here. */
  public void replace(File candidate, Callable<Void> recordRestore) throws Exception {
    exclusive(()->{
      Path rollback=Files.createTempDirectory(file.toPath().getParent(),"restore-rollback-");
      if(pool!=null)pool.close();
      boolean saved=false,cleanup=false;
      String[] suffixes={"","-wal","-shm","-journal"};
      try {
        for(String suffix:suffixes) {
          Path original=Paths.get(file+suffix);
          if(Files.exists(original)) Files.copy(original,rollback.resolve("database.db"+suffix));
        }
        saved=true;
        for(String suffix:suffixes) Files.deleteIfExists(Paths.get(file+suffix));
        Files.copy(candidate.toPath(),file.toPath(),StandardCopyOption.REPLACE_EXISTING);
        open();recordRestore.call();startupFailure=null;cleanup=true;
      } catch(Exception failure) {
        if(pool!=null)pool.close();pool=null;
        if(saved) {
          try {
            for(String suffix:suffixes) {
              Path original=Paths.get(file+suffix);Files.deleteIfExists(original);
              Path prior=rollback.resolve("database.db"+suffix);
              if(Files.exists(prior)) Files.copy(prior,original);
            }
            if(!recoveryRequired())open();cleanup=true;
          } catch(Exception recovery) {
            failure.addSuppressed(recovery);
            startupFailure=new IllegalStateException("还原回滚失败，请保留紧急备份并联系技术支持。回滚副本："+rollback,failure);
            throw startupFailure;
          }
        } else if(!recoveryRequired())open();
        throw failure;
      } finally {
        if(cleanup) {
          try {for(String suffix:suffixes)Files.deleteIfExists(rollback.resolve("database.db"+suffix));Files.deleteIfExists(rollback);}
          catch(IOException e){org.slf4j.LoggerFactory.getLogger(Database.class).warn("无法清理还原临时副本",e);}
        }
      }
      return null;
    });
  }
  private void releaseInstanceLock() {
    try{if(instanceLock!=null && instanceLock.isValid())instanceLock.release();if(instanceFile!=null)instanceFile.close();}
    catch(IOException e){org.slf4j.LoggerFactory.getLogger(Database.class).warn("数据库实例锁释放失败",e);}
    finally{synchronized(INSTANCE_PATHS){if(instanceIdentity!=null)INSTANCE_PATHS.remove(instanceIdentity);instanceIdentity=null;}}
  }
  @Override public void close() {lock.writeLock().lock();try{if(pool!=null)pool.close();}finally{releaseInstanceLock();lock.writeLock().unlock();}}
}
