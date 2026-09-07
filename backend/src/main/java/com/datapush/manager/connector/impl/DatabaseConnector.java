package com.datapush.manager.connector.impl;

import com.datapush.manager.connector.Connector;
import com.datapush.manager.entity.ConnectorInfo;
import com.datapush.manager.util.EncryptUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * 数据库连接器实现
 *
 * @author Data Push Team
 * @since 2024-11-20
 */
@Slf4j
@Component
public class DatabaseConnector implements Connector {
    
    @Autowired
    private EncryptUtil encryptUtil;

    @Override
    public ConnectionTestResult testConnection(ConnectorInfo connectorInfo) {
        String jdbcUrl = buildJdbcUrl(connectorInfo);
        // 注：connectorInfo由ConnectorInfoService传入，已经解密，直接使用
        
        try (Connection conn = DriverManager.getConnection(
                jdbcUrl,
                connectorInfo.getUsername(),
                connectorInfo.getPassword())) {
            
            // 达梦数据库需要额外验证模式是否存在
            if ("DM".equalsIgnoreCase(connectorInfo.getDbType())) {
                String databaseName = connectorInfo.getDatabaseName();
                if (databaseName != null && !databaseName.trim().isEmpty()) {
                    String schemaName = databaseName.toUpperCase();
                    // 尝试设置当前模式，如果模式不存在会抛出异常
                    try (java.sql.Statement stmt = conn.createStatement()) {
                        stmt.execute("SET SCHEMA " + schemaName);
                    } catch (SQLException e) {
                        // 如果设置模式失败，说明模式不存在或无权限
                        log.error("达梦数据库设置模式失败: {}", e.getMessage());
                        return ConnectionTestResult.failed("模式/数据库 '" + databaseName + "' 不存在或无访问权限");
                    }
                }
            }
            
            log.info("数据库连接测试成功: {}", connectorInfo.getConnectorName());
            return ConnectionTestResult.success("连接成功！数据库类型: " + connectorInfo.getDbType());
            
        } catch (SQLException e) {
            log.error("数据库连接测试失败: {}, 错误: {}", connectorInfo.getConnectorName(), e.getMessage());
            String errorMsg = "连接失败：" + e.getMessage();
            if (e.getMessage().contains("Access denied")) {
                errorMsg = "用户名或密码错误";
            } else if (e.getMessage().contains("Unknown database")) {
                errorMsg = "数据库不存在";
            } else if (e.getMessage().contains("Communications link failure")) {
                errorMsg = "无法连接到数据库服务器，请检查主机地址和端口";
            }
            return ConnectionTestResult.failed(errorMsg);
        }
    }

    @Override
    public String getConnectorType() {
        return "DATABASE";
    }

    /**
     * 构建JDBC URL
     */
    private String buildJdbcUrl(ConnectorInfo connectorInfo) {
        String dbType = connectorInfo.getDbType();
        String host = connectorInfo.getHost();
        Integer port = connectorInfo.getPort();
        String databaseName = connectorInfo.getDatabaseName();

        switch (dbType.toUpperCase()) {
            case "MYSQL":
                return String.format("jdbc:mysql://%s:%d/%s?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&connectTimeout=10000&socketTimeout=30000&rewriteBatchedStatements=true",
                        host, port, databaseName);
            case "ORACLE":
                // Oracle使用属性设置批处理优化，在getConnection中处理
                return String.format("jdbc:oracle:thin:@%s:%d:%s", host, port, databaseName);
            case "POSTGRESQL":
                // PostgreSQL批处理优化：reWriteBatchedInserts=true
                return String.format("jdbc:postgresql://%s:%d/%s?tcpKeepAlive=true&loginTimeout=10&reWriteBatchedInserts=true",
                        host, port, databaseName);
            case "SQLSERVER":
                // SQL Server批处理优化
                return String.format("jdbc:sqlserver://%s:%d;databaseName=%s;loginTimeout=10;sendStringParametersAsUnicode=false;useBulkCopyForBatchInsert=true",
                        host, port, databaseName);
            case "KINGBASE":
                // 人大金仓（基于PostgreSQL），使用相同的批处理优化
                return String.format("jdbc:kingbase8://%s:%d/%s?loginTimeout=10&tcpKeepAlive=true&reWriteBatchedInserts=true", host, port, databaseName);
            case "DM":
                // 达梦数据库批处理优化
                return String.format("jdbc:dm://%s:%d/%s?loginTimeout=10&useUnicode=true&characterEncoding=utf8&batchAllowedInsert=true", host, port, databaseName);
            default:
                throw new IllegalArgumentException("不支持的数据库类型: " + dbType);
        }
    }

    /**
     * 获取数据库连接
     */
    public Connection getConnection(ConnectorInfo connectorInfo) throws SQLException {
        String jdbcUrl = buildJdbcUrl(connectorInfo);
        // 解密密码
        String password = encryptUtil.decrypt(connectorInfo.getPassword());
        
        DriverManager.setLoginTimeout(10);
        if ("ORACLE".equalsIgnoreCase(connectorInfo.getDbType())) {
            java.util.Properties props = new java.util.Properties();
            props.put("user", connectorInfo.getUsername());
            props.put("password", password); // 使用解密后的密码
            props.put("oracle.net.CONNECT_TIMEOUT", "10000");
            props.put("oracle.jdbc.ReadTimeout", "30000");
            // Oracle批处理优化
            props.put("defaultBatchValue", "1000"); // 默认批量大小
            props.put("oracle.jdbc.defaultExecuteBatch", "1000");
            return DriverManager.getConnection(jdbcUrl, props);
        }
        return DriverManager.getConnection(jdbcUrl, connectorInfo.getUsername(), password); // 使用解密后的密码
    }
}
