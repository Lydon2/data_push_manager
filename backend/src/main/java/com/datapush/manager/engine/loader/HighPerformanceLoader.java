package com.datapush.manager.engine.loader;

import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.util.List;
import java.util.Map;

/**
 * 高性能批量加载器管理类
 * 根据数据库类型和数据量自动选择最优加载策略
 * 
 * @author Data Push Team
 * @since 2024-12-05
 */
@Slf4j
public class HighPerformanceLoader {
    
    /**
     * 智能批量加载数据
     * 根据数据库类型和数据量自动选择最优策略
     * 
     * @param conn 数据库连接
     * @param dbType 数据库类型
     * @param tableName 表名
     * @param dataList 数据列表
     * @param fieldList 字段列表
     * @return 成功导入的记录数
     */
    public static int load(Connection conn, String dbType, String tableName,
                          List<Map<String, Object>> dataList,
                          List<String> fieldList) throws Exception {
        
        if (dataList == null || dataList.isEmpty()) {
            return 0;
        }
        
        int dataSize = dataList.size();
        
        try {
            switch (dbType.toUpperCase()) {
                case "MYSQL":
                    // MySQL: 大数据量使用LOAD DATA INFILE
                    if (MySQLLoadDataLoader.shouldUseLoadData(dataSize, false)) {
                        log.info("使用MySQL LOAD DATA INFILE加载 {} 条数据", dataSize);
                        return MySQLLoadDataLoader.loadData(conn, tableName, dataList, fieldList);
                    }
                    break;
                    
                case "POSTGRESQL":
                    // PostgreSQL: 大数据量使用COPY
                    if (PostgreSQLCopyLoader.shouldUseCopy(dataSize)) {
                        try {
                            log.info("使用PostgreSQL COPY加载 {} 条数据", dataSize);
                            return (int) PostgreSQLCopyLoader.copyData(conn, tableName, dataList, fieldList);
                        } catch (UnsupportedOperationException e) {
                            log.warn("COPY不支持，降级为批量插入");
                            // 降级为普通批量插入
                        }
                    }
                    break;
                    
                case "KINGBASE":
                    // 人大金仓（基于PostgreSQL）: 大数据量使用COPY
                    if (PostgreSQLCopyLoader.shouldUseCopy(dataSize)) {
                        try {
                            log.info("使用人大金仓 COPY加载 {} 条数据", dataSize);
                            return (int) PostgreSQLCopyLoader.copyData(conn, tableName, dataList, fieldList);
                        } catch (UnsupportedOperationException e) {
                            log.warn("人大金仓 COPY不支持，降级为批量插入");
                            // 降级为普通批量插入
                        }
                    }
                    break;
                    
                case "ORACLE":
                    // Oracle: 大数据量使用优化的批量插入
                    if (OracleBatchLoader.shouldUseBatchInsert(dataSize)) {
                        log.info("使用Oracle优化批量插入加载 {} 条数据", dataSize);
                        return OracleBatchLoader.batchInsert(conn, tableName, dataList, fieldList, 10000);
                    }
                    break;
                    
                case "DM":
                    // 达梦数据库: 大数据量使用优化的批量插入
                    if (DamengBatchLoader.shouldUseBatchInsert(dataSize)) {
                        log.info("使用达梦数据库优化批量插入加载 {} 条数据", dataSize);
                        return DamengBatchLoader.batchInsert(conn, tableName, dataList, fieldList, 10000);
                    }
                    break;
                    
                case "SQLSERVER":
                    // SQL Server: 使用标准批量插入（已通过JDBC参数优化）
                    log.debug("SQL Server 使用标准批量插入（JDBC优化）");
                    break;
                    
                default:
                    log.debug("数据库类型 {} 使用标准批量插入", dbType);
            }
            
        } catch (Exception e) {
            log.warn("高性能加载失败，降级为标准批量插入: {}", e.getMessage());
        }
        
        // 默认：返回null表示使用标准批量插入
        return -1;
    }
    
    /**
     * 判断是否应该使用高性能加载
     */
    public static boolean shouldUseHighPerformanceLoader(String dbType, int dataSize) {
        switch (dbType.toUpperCase()) {
            case "MYSQL":
                return dataSize > 10000;
            case "POSTGRESQL":
            case "KINGBASE":
            case "ORACLE":
            case "DM":
                return dataSize > 5000;
            default:
                return false;
        }
    }
}
