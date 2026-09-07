package com.datapush.manager.engine.loader;

import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.Map;

/**
 * Oracle批量加载器
 * 使用PreparedStatement批处理优化 + 自动提交优化
 * 
 * @author Data Push Team
 * @since 2024-12-05
 */
@Slf4j
public class OracleBatchLoader {
    
    /**
     * Oracle批量插入优化
     * 使用大批次 + 优化的提交策略
     * 
     * @param conn 数据库连接
     * @param tableName 表名
     * @param dataList 数据列表
     * @param fieldList 字段列表
     * @param batchSize 批次大小（建议10000）
     * @return 成功导入的记录数
     */
    public static int batchInsert(Connection conn, String tableName, 
                                  List<Map<String, Object>> dataList, 
                                  List<String> fieldList,
                                  int batchSize) throws Exception {
        
        if (dataList == null || dataList.isEmpty()) {
            return 0;
        }
        
        // 构建INSERT SQL
        String insertSql = buildInsertSql(tableName, fieldList);
        
        int totalInserted = 0;
        
        try {
            conn.setAutoCommit(false);
            
            try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
                // 设置Oracle批处理优化参数
                pstmt.setFetchSize(batchSize);
                
                int count = 0;
                for (Map<String, Object> row : dataList) {
                    // 设置参数
                    for (int i = 0; i < fieldList.size(); i++) {
                        Object value = row.get(fieldList.get(i));
                        pstmt.setObject(i + 1, value);
                    }
                    pstmt.addBatch();
                    count++;
                    
                    // 每batchSize条执行一次
                    if (count % batchSize == 0) {
                        pstmt.executeBatch();
                        conn.commit();
                        totalInserted += count;
                        count = 0;
                        pstmt.clearBatch();
                        
                        log.debug("Oracle批量插入：已提交 {} 条", totalInserted);
                    }
                }
                
                // 处理剩余数据
                if (count > 0) {
                    pstmt.executeBatch();
                    conn.commit();
                    totalInserted += count;
                }
            }
            
            conn.setAutoCommit(true);
            log.info("Oracle批量插入完成，总计 {} 条", totalInserted);
            
        } catch (Exception e) {
            conn.rollback();
            conn.setAutoCommit(true);
            throw e;
        }
        
        return totalInserted;
    }
    
    /**
     * 构建INSERT SQL
     */
    private static String buildInsertSql(String tableName, List<String> fieldList) {
        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO ").append(tableName).append(" (");
        sql.append(String.join(", ", fieldList));
        sql.append(") VALUES (");
        
        for (int i = 0; i < fieldList.size(); i++) {
            if (i > 0) sql.append(", ");
            sql.append("?");
        }
        sql.append(")");
        
        return sql.toString();
    }
    
    /**
     * 判断是否适合使用大批次插入
     * 建议：数据量 > 5000条时使用
     */
    public static boolean shouldUseBatchInsert(int dataSize) {
        return dataSize > 5000;
    }
}
