package com.healtouch.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;

import javax.sql.DataSource;
import java.io.File;

public final class Database implements AutoCloseable {
    private final HikariDataSource dataSource;

    public Database() { this(AppPaths.databaseFile()); }
    public Database(File file) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + file.getAbsolutePath());
        config.setDriverClassName("org.sqlite.JDBC");
        config.setMaximumPoolSize(1); // 单机 SQLite：用一个连接避免写锁争用
        config.setMinimumIdle(1);
        config.setConnectionTimeout(10000);
        config.setConnectionInitSql("PRAGMA foreign_keys=ON; PRAGMA busy_timeout=10000;");
        dataSource = new HikariDataSource(config);
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load().migrate();
    }
    public DataSource dataSource() { return dataSource; }
    @Override public void close() { dataSource.close(); }
}
