package com.healtouch.dao;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public final class Jdbc {
    private Jdbc() {}
    public interface Transaction<T> { T run(Connection connection) throws Exception; }
    public static <T> T inTransaction(DataSource dataSource, Transaction<T> work) {
        try (Connection connection = dataSource.getConnection()) {
            boolean originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                T result = work.run(connection);
                connection.commit();
                return result;
            } catch (Exception ex) {
                connection.rollback();
                if (ex instanceof RuntimeException) throw (RuntimeException) ex;
                throw new IllegalStateException("数据库操作失败", ex);
            } finally {
                connection.setAutoCommit(originalAutoCommit);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("无法连接本地数据库", ex);
        }
    }
}
