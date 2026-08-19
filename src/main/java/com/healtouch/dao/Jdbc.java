package com.healtouch.dao;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Jdbc {
  private static final Logger LOGGER = LoggerFactory.getLogger(Jdbc.class);

  private Jdbc() {}

  public interface Transaction<T> {
    T run(Connection connection) throws Exception;
  }

  public static <T> T inTransaction(DataSource dataSource, Transaction<T> work) {
    try (Connection connection = dataSource.getConnection()) {
      boolean originalAutoCommit = connection.getAutoCommit();
      Throwable failure = null;
      try {
        connection.setAutoCommit(false);
        T result = work.run(connection);
        connection.commit();
        return result;
      } catch (Throwable exception) {
        failure = exception;
        rollback(connection, exception);
        throw rethrow(exception);
      } finally {
        restoreAutoCommit(connection, originalAutoCommit, failure);
      }
    } catch (SQLException exception) {
      throw new IllegalStateException("无法连接本地数据库", exception);
    }
  }

  private static void rollback(Connection connection, Throwable originalFailure) {
    try {
      connection.rollback();
      LOGGER.warn("数据库事务已回滚", originalFailure);
    } catch (SQLException rollbackFailure) {
      originalFailure.addSuppressed(rollbackFailure);
      LOGGER.error("数据库事务回滚失败", rollbackFailure);
    }
  }

  private static void restoreAutoCommit(
      Connection connection, boolean originalAutoCommit, Throwable failure) throws SQLException {
    try {
      connection.setAutoCommit(originalAutoCommit);
    } catch (SQLException restoreFailure) {
      if (failure != null) {
        failure.addSuppressed(restoreFailure);
        LOGGER.error("数据库连接自动提交状态恢复失败", restoreFailure);
        return;
      }
      throw restoreFailure;
    }
  }

  private static RuntimeException rethrow(Throwable exception) {
    if (exception instanceof RuntimeException) {
      return (RuntimeException) exception;
    }
    if (exception instanceof Error) {
      throw (Error) exception;
    }
    return new IllegalStateException("数据库操作失败", exception);
  }
}
