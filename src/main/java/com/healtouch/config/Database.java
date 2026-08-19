package com.healtouch.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.File;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;

public final class Database implements AutoCloseable {
  private final HikariDataSource dataSource;

  public Database() {
    this(AppPaths.databaseFile());
  }

  public Database(File file) {
    HikariConfig config = new HikariConfig();
    config.setJdbcUrl("jdbc:sqlite:" + file.getAbsolutePath());
    config.setDriverClassName("org.sqlite.JDBC");
    // SQLite serializes writes. One pooled connection avoids competing writers in this
    // single-process app.
    config.setMaximumPoolSize(1);
    config.setMinimumIdle(1);
    // Fail a blocked UI request after the same 10-second window configured in SQLite's busy
    // timeout.
    config.setConnectionTimeout(10_000);
    // Each pooled connection enforces referential integrity and waits briefly for a transient file
    // lock.
    config.setConnectionInitSql("PRAGMA foreign_keys=ON; PRAGMA busy_timeout=10000;");
    dataSource = new HikariDataSource(config);
    // V1 enables SQLite foreign keys with PRAGMA before its DDL. Flyway must therefore permit this
    // intentionally mixed migration; keeping V1 unchanged also preserves its checksum for existing
    // installations.
    Flyway.configure()
        .dataSource(dataSource)
        .locations("classpath:db/migration")
        .mixed(true)
        .load()
        .migrate();
  }

  public DataSource dataSource() {
    return dataSource;
  }

  @Override
  public void close() {
    dataSource.close();
  }
}
