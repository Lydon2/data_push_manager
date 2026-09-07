package com.datapush.manager.connector.pool;

import com.datapush.manager.entity.ConnectorInfo;
import com.datapush.manager.util.EncryptUtil;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 连接池管理器（性能优化）
 * 使用HikariCP管理数据库连接，避免频繁创建/销毁连接
 * 
 * @author Data Push Team
 * @since 2024-12-05
 */
@Slf4j
@Component
public class ConnectionPoolManager {
    
    @Autowired
    private EncryptUtil encryptUtil;
    
    /**
     * 连接池缓存：Key=connectorId，Value=HikariDataSource
     */
    private final Map<Long, HikariDataSource> poolCache = new ConcurrentHashMap<>();
    
    /**
     * 获取连接（从连接池）
     */
    public Connection getConnection(ConnectorInfo connectorInfo) throws SQLException {
        Long connectorId = connectorInfo.getId();
        
        // 如果连接池不存在，创建新的
        HikariDataSource dataSource = poolCache.computeIfAbsent(connectorId, id -> {
            log.info("创建连接池：connectorId={}, name={}", id, connectorInfo.getConnectorName());
            return createDataSource(connectorInfo);
        });
        
        return dataSource.getConnection();
    }
    
    /**
     * 创建HikariCP数据源
     */
    private HikariDataSource createDataSource(ConnectorInfo connectorInfo) {
        HikariConfig config = new HikariConfig();
        
        // JDBC配置
        String jdbcUrl = buildJdbcUrl(connectorInfo);
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(connectorInfo.getUsername());
        config.setPassword(encryptUtil.decrypt(connectorInfo.getPassword()));
        
        // 连接池配置（性能优化）
        config.setMaximumPoolSize(10);          // 最大连接数：10个（并行写入4线程+预留）
        config.setMinimumIdle(2);               // 最小空闲连接：2个
        config.setConnectionTimeout(10000);     // 连接超时：10秒
        config.setIdleTimeout(300000);          // 空闲超时：5分钟
        config.setMaxLifetime(600000);          // 连接最大存活时间：10分钟
        config.setConnectionTestQuery("SELECT 1"); // 连接测试SQL
        
        // 连接池名称
        config.setPoolName("ETL-Pool-" + connectorInfo.getId());
        
        // Oracle特殊配置
        if ("ORACLE".equalsIgnoreCase(connectorInfo.getDbType())) {
            config.addDataSourceProperty("oracle.net.CONNECT_TIMEOUT", "10000");
            config.addDataSourceProperty("oracle.jdbc.ReadTimeout", "30000");
            config.addDataSourceProperty("defaultBatchValue", "1000");
            config.addDataSourceProperty("oracle.jdbc.defaultExecuteBatch", "1000");
        }
        
        return new HikariDataSource(config);
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
                return String.format("jdbc:oracle:thin:@%s:%d:%s", host, port, databaseName);
            case "POSTGRESQL":
                return String.format("jdbc:postgresql://%s:%d/%s?tcpKeepAlive=true&loginTimeout=10&reWriteBatchedInserts=true",
                        host, port, databaseName);
            case "SQLSERVER":
                return String.format("jdbc:sqlserver://%s:%d;databaseName=%s;loginTimeout=10;sendStringParametersAsUnicode=false;useBulkCopyForBatchInsert=true",
                        host, port, databaseName);
            case "KINGBASE":
                return String.format("jdbc:kingbase8://%s:%d/%s?loginTimeout=10&tcpKeepAlive=true&reWriteBatchedInserts=true", host, port, databaseName);
            case "DM":
                return String.format("jdbc:dm://%s:%d/%s?loginTimeout=10&useUnicode=true&characterEncoding=utf8&batchAllowedInsert=true", host, port, databaseName);
            default:
                throw new IllegalArgumentException("不支持的数据库类型: " + dbType);
        }
    }
    
    /**
     * 清除指定连接器的连接池
     */
    public void evictPool(Long connectorId) {
        HikariDataSource dataSource = poolCache.remove(connectorId);
        if (dataSource != null) {
            log.info("关闭连接池：connectorId={}", connectorId);
            dataSource.close();
        }
    }
    
    /**
     * 清除所有连接池（应用关闭时）
     */
    @PreDestroy
    public void shutdown() {
        log.info("关闭所有连接池，总数：{}", poolCache.size());
        poolCache.values().forEach(HikariDataSource::close);
        poolCache.clear();
    }
    
    /**
     * 获取连接池统计信息
     */
    public String getPoolStats(Long connectorId) {
        HikariDataSource dataSource = poolCache.get(connectorId);
        if (dataSource == null) {
            return "连接池不存在";
        }
        
        return String.format("连接池[%s]: 活跃=%d, 空闲=%d, 总数=%d, 等待线程=%d",
                dataSource.getPoolName(),
                dataSource.getHikariPoolMXBean().getActiveConnections(),
                dataSource.getHikariPoolMXBean().getIdleConnections(),
                dataSource.getHikariPoolMXBean().getTotalConnections(),
                dataSource.getHikariPoolMXBean().getThreadsAwaitingConnection());
    }
}
