package com.datapush.manager.util;

import lombok.extern.slf4j.Slf4j;

import java.sql.*;
import java.util.*;
import java.util.function.Consumer;

/**
 * 批量数据处理工具类
 * 支持分批次读取大数据、流式处理、内存优化
 */
@Slf4j
public class BatchProcessorUtil {
    
    /**
     * 默认批次大小
     */
    private static final int DEFAULT_BATCH_SIZE = 1000;
    
    /**
     * 批量读取数据库数据
     * 
     * @param conn 数据库连接
     * @param sql SQL查询语句
     * @param batchSize 批次大小
     * @param processor 数据处理器
     * @return 总处理记录数
     */
    public static int batchFetch(Connection conn, String sql, int batchSize, Consumer<List<Map<String, Object>>> processor) throws SQLException {
        if (batchSize <= 0) {
            batchSize = DEFAULT_BATCH_SIZE;
        }
        
        int totalCount = 0;
        int batchCount = 0;
        
        // 设置 FETCH_SIZE 进行流式读取
        Statement stmt = conn.createStatement(
            ResultSet.TYPE_FORWARD_ONLY,
            ResultSet.CONCUR_READ_ONLY
        );
        
        // MySQL流式读取配置
        stmt.setFetchSize(Integer.MIN_VALUE);
        
        try (ResultSet rs = stmt.executeQuery(sql)) {
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            
            List<Map<String, Object>> batch = new ArrayList<>(batchSize);
            
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnLabel(i);
                    Object value = rs.getObject(i);
                    row.put(columnName, value);
                }
                
                batch.add(row);
                totalCount++;
                
                // 达到批次大小，处理并清空
                if (batch.size() >= batchSize) {
                    processor.accept(batch);
                    batchCount++;
                    log.debug("批次 {} 处理完成，记录数: {}", batchCount, batch.size());
                    batch.clear();
                    
                    // 建议GC回收
                    if (batchCount % 10 == 0) {
                        System.gc();
                    }
                }
            }
            
            // 处理最后不足一批的数据
            if (!batch.isEmpty()) {
                processor.accept(batch);
                batchCount++;
                log.debug("最后批次处理完成，记录数: {}", batch.size());
            }
            
            log.info("批量读取完成，总记录数: {}，批次数: {}", totalCount, batchCount);
            
        } finally {
            if (stmt != null) {
                stmt.close();
            }
        }
        
        return totalCount;
    }
    
    /**
     * 批量写入数据
     * 
     * @param conn 数据库连接
     * @param tableName 表名
     * @param dataList 数据列表
     * @param batchSize 批次大小
     * @return 插入成功的记录数
     */
    public static int batchInsert(Connection conn, String tableName, List<Map<String, Object>> dataList, int batchSize) throws SQLException {
        if (dataList == null || dataList.isEmpty()) {
            return 0;
        }
        
        if (batchSize <= 0) {
            batchSize = DEFAULT_BATCH_SIZE;
        }
        
        int totalInserted = 0;
        int totalSkipped = 0; // 记录跳过的主键冲突数
        int totalFailed = 0;  // 记录实际失败数
        
        // 获取字段列表
        Map<String, Object> firstRow = dataList.get(0);
        List<String> fields = new ArrayList<>(firstRow.keySet());
        
        // 构建SQL
        StringBuilder sqlBuilder = new StringBuilder("INSERT INTO ").append(tableName).append(" (");
        for (int i = 0; i < fields.size(); i++) {
            if (i > 0) sqlBuilder.append(", ");
            sqlBuilder.append(fields.get(i));
        }
        sqlBuilder.append(") VALUES (");
        for (int i = 0; i < fields.size(); i++) {
            if (i > 0) sqlBuilder.append(", ");
            sqlBuilder.append("?");
        }
        sqlBuilder.append(")");
        
        String sql = sqlBuilder.toString();
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            conn.setAutoCommit(false);
            
            int currentBatchSize = 0;
            int batchCounter = 0; // 批次计数器，用于每10批commit
            
            for (int i = 0; i < dataList.size(); i++) {
                Map<String, Object> row = dataList.get(i);
                
                // 设置参数
                for (int j = 0; j < fields.size(); j++) {
                    pstmt.setObject(j + 1, row.get(fields.get(j)));
                }
                
                pstmt.addBatch();
                currentBatchSize++;
                
                // 达到批次大小，执行批量插入
                if (currentBatchSize >= batchSize) {
                    try {
                        int[] results = pstmt.executeBatch();
                        totalInserted += results.length;
                        pstmt.clearBatch();
                        currentBatchSize = 0;
                        batchCounter++;
                        
                        // 每10批commit一次（阶段1优化）
                        if (batchCounter % 10 == 0) {
                            conn.commit();
                            log.debug("批次提交: 已完成10批, 总计插入: {} 条", totalInserted);
                        }
                    } catch (SQLException e) {
                        // 批量失败，回滚当前批次
                        conn.rollback();
                        pstmt.clearBatch();
                        currentBatchSize = 0;
                        log.warn("批次插入失败，尝试逐条插入本批次数据", e);
                        
                        // 逐条插入当前批次
                        int batchStartIndex = i - currentBatchSize + 1;
                        int batchEndIndex = i + 1;
                        
                        for (int k = batchStartIndex; k < batchEndIndex; k++) {
                            Map<String, Object> singleRow = dataList.get(k);
                            try {
                                for (int j = 0; j < fields.size(); j++) {
                                    pstmt.setObject(j + 1, singleRow.get(fields.get(j)));
                                }
                                pstmt.execute();
                                totalInserted++;
                            } catch (SQLException singleEx) {
                                // 判断是否是主键冲突错误
                                String sqlState = singleEx.getSQLState();
                                String errorMsg = singleEx.getMessage();
                                boolean isDuplicateKey = "23000".equals(sqlState) 
                                    || "23505".equals(sqlState) 
                                    || "00001".equals(sqlState)
                                    || (errorMsg != null && (errorMsg.contains("Duplicate entry") 
                                        || errorMsg.contains("重复键违反唯一约束")
                                        || errorMsg.contains("重复键违反主键约束")
                                        || errorMsg.contains("unique constraint")
                                        || errorMsg.contains("PRIMARY KEY")));
                                
                                if (isDuplicateKey) {
                                    totalSkipped++;
                                    log.warn("跳过重复数据（主键冲突），索引: {}", k);
                                } else {
                                    totalFailed++;
                                    log.error("数据插入失败（非主键冲突），索引: {}, 数据: {}, 错误: {}", 
                                        k, singleRow, errorMsg);
                                    throw singleEx;
                                }
                            }
                        }
                        
                        // 逐条插入后提交
                        conn.commit();
                    }
                }
            }
            
            // 执行最后不足一批的数据
            if (currentBatchSize > 0) {
                try {
                    int[] results = pstmt.executeBatch();
                    totalInserted += results.length;
                    log.debug("最后批次插入完成，本批: {} 条", results.length);
                } catch (SQLException e) {
                    // 批量失败，回滚并逐条插入
                    conn.rollback();
                    pstmt.clearBatch();
                    log.warn("最后批次插入失败，尝试逐条插入", e);
                    
                    int batchStartIndex = dataList.size() - currentBatchSize;
                    
                    for (int k = batchStartIndex; k < dataList.size(); k++) {
                        Map<String, Object> singleRow = dataList.get(k);
                        try {
                            for (int j = 0; j < fields.size(); j++) {
                                pstmt.setObject(j + 1, singleRow.get(fields.get(j)));
                            }
                            pstmt.execute();
                            totalInserted++;
                        } catch (SQLException singleEx) {
                            String sqlState = singleEx.getSQLState();
                            String errorMsg = singleEx.getMessage();
                            boolean isDuplicateKey = "23000".equals(sqlState) 
                                || "23505".equals(sqlState) 
                                || "00001".equals(sqlState)
                                || (errorMsg != null && (errorMsg.contains("Duplicate entry") 
                                    || errorMsg.contains("重复键违反唯一约束")
                                    || errorMsg.contains("重复键违反主键约束")
                                    || errorMsg.contains("unique constraint")
                                    || errorMsg.contains("PRIMARY KEY")));
                            
                            if (isDuplicateKey) {
                                totalSkipped++;
                                log.warn("跳过重复数据（主键冲突），索引: {}", k);
                            } else {
                                totalFailed++;
                                log.error("数据插入失败（非主键冲突），索引: {}, 数据: {}, 错误: {}", 
                                    k, singleRow, errorMsg);
                                throw singleEx;
                            }
                        }
                    }
                }
            }
            
            // 最终提交
            conn.commit();
            conn.setAutoCommit(true);
            
            log.info("批量插入完成 - 总记录: {}, 成功: {}, 跳过(主键冲突): {}, 失败: {}", 
                dataList.size(), totalInserted, totalSkipped, totalFailed);
            
        } catch (SQLException e) {
            try {
                conn.rollback();
                conn.setAutoCommit(true);
            } catch (SQLException rollbackEx) {
                log.error("回滚失败", rollbackEx);
            }
            throw e;
        }
        
        return totalInserted;
    }
    
    /**
     * 流式处理大数据
     * 
     * @param conn 数据库连接
     * @param sql SQL查询语句
     * @param processor 行处理器（每行单独处理）
     * @return 总处理记录数
     */
    public static int streamProcess(Connection conn, String sql, Consumer<Map<String, Object>> processor) throws SQLException {
        int totalCount = 0;
        
        Statement stmt = conn.createStatement(
            ResultSet.TYPE_FORWARD_ONLY,
            ResultSet.CONCUR_READ_ONLY
        );
        stmt.setFetchSize(Integer.MIN_VALUE); // MySQL流式读取
        
        try (ResultSet rs = stmt.executeQuery(sql)) {
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnLabel(i);
                    Object value = rs.getObject(i);
                    row.put(columnName, value);
                }
                
                processor.accept(row);
                totalCount++;
                
                // 定期建议GC
                if (totalCount % 10000 == 0) {
                    log.debug("已处理 {} 条记录", totalCount);
                    System.gc();
                }
            }
            
            log.info("流式处理完成，总记录数: {}", totalCount);
            
        } finally {
            if (stmt != null) {
                stmt.close();
            }
        }
        
        return totalCount;
    }
    
    /**
     * 内存优化：清理数据列表
     */
    public static void clearData(List<?> dataList) {
        if (dataList != null) {
            dataList.clear();
        }
        System.gc();
    }
    
    /**
     * 获取当前JVM内存使用情况
     */
    public static String getMemoryInfo() {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        long maxMemory = runtime.maxMemory();
        
        return String.format("内存使用: %.2f MB / %.2f MB (最大: %.2f MB)",
            usedMemory / 1024.0 / 1024.0,
            totalMemory / 1024.0 / 1024.0,
            maxMemory / 1024.0 / 1024.0
        );
    }
}
