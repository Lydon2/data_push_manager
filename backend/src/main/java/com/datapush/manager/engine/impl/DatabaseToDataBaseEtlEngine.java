package com.datapush.manager.engine.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.datapush.manager.connector.impl.DatabaseConnector;
import com.datapush.manager.connector.impl.ApiConnector;
import com.datapush.manager.connector.pool.ConnectionPoolManager;
import com.datapush.manager.engine.EtlEngine;
import com.datapush.manager.engine.PostLoadProcessor;
import com.datapush.manager.entity.ConnectorInfo;
import com.datapush.manager.entity.FieldMapping;
import com.datapush.manager.entity.TaskConfig;
import com.datapush.manager.entity.AuxiliaryDatasource;
import com.datapush.manager.service.DictMappingService;
import com.datapush.manager.service.AuxiliaryDatasourceService;
import com.datapush.manager.service.ConnectorInfoService;
import com.datapush.manager.service.DataLineageService;
import com.datapush.manager.entity.DataLineage;
import com.datapush.manager.transformer.AdvancedTransformer;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.sql.*;
import com.datapush.manager.utils.TransformFunctionUtil;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * 数据库到数据库的ETL引擎实现
 *
 * @author Data Push Team
 * @since 2024-11-20
 */
@Slf4j
@Component
public class DatabaseToDataBaseEtlEngine implements EtlEngine {

    @Autowired
    private DatabaseConnector databaseConnector;
    
    @Autowired
    private ConnectionPoolManager connectionPoolManager;
    
    @Autowired
    private ApiConnector apiConnector;
    
    @Autowired
    private DictMappingService dictMappingService;
    
    @Autowired
    private AuxiliaryDatasourceService auxiliaryDatasourceService;
    
    @Autowired
    private ConnectorInfoService connectorInfoService;
    
    @Autowired
    private AdvancedTransformer advancedTransformer;
    
    /**
     * ProcessorChain缓存（性能优化）
     * Key: processorChain JSON字符串
     * Value: 解析后的处理器列表
     */
    private final Map<String, List<Map<String, Object>>> processorChainCache = new java.util.concurrent.ConcurrentHashMap<>();
    
    /**
     * Groovy脚本缓存（性能优化）
     * Key: 脚本内容
     * Value: 编译后的Script实例
     */
    private final Map<String, groovy.lang.Script> scriptCache = new java.util.concurrent.ConcurrentHashMap<>();
    private final GroovyShell groovyShell = new GroovyShell();
    
    /**
     * Pattern编译缓存（性能优化）
     * Key: 正则表达式
     * Value: Pattern实例
     */
    private final Map<String, java.util.regex.Pattern> patternCache = new java.util.concurrent.ConcurrentHashMap<>();
    
    @Autowired
    private DataLineageService dataLineageService;
    
    @Autowired
    private PostLoadProcessor postLoadProcessor;
    
    @Autowired
    private com.datapush.manager.service.TaskTargetService taskTargetService;

    @Override
    public EtlResult execute(TaskConfig taskConfig, 
                            ConnectorInfo sourceConnector, 
                            ConnectorInfo targetConnector,
                            List<FieldMapping> fieldMappings,
                            Long logId) {
        
        // 判断是否多目标模式
        if (taskConfig.getMultiTarget() != null && taskConfig.getMultiTarget() == 1) {
            // 多目标模式
            return executeMultiTarget(taskConfig, sourceConnector, fieldMappings, logId);
        } else {
            // 单目标模式（原有逻辑，兼容旧版）
            return executeSingleTarget(taskConfig, sourceConnector, targetConnector, fieldMappings, logId);
        }
    }
    
    /**
     * 单目标模式执行（原有逻辑）
     */
    private EtlResult executeSingleTarget(TaskConfig taskConfig, 
                                         ConnectorInfo sourceConnector, 
                                         ConnectorInfo targetConnector,
                                         List<FieldMapping> fieldMappings,
                                         Long logId) {
        
        StringBuilder logBuilder = new StringBuilder();
        logBuilder.append("========== ETL任务开始执行 ==========\n");
        logBuilder.append("任务名称: ").append(taskConfig.getTaskName()).append("\n");
        logBuilder.append("开始时间: ").append(LocalDateTime.now()).append("\n\n");

        Connection sourceConn = null;
        Connection targetConn = null;
        
        try {
            // 1. 建立源端连接(使用连接池)
            logBuilder.append("[步骤1] 建立源端数据库连接(使用HikariCP连接池)...\n");
            sourceConn = connectionPoolManager.getConnection(sourceConnector);
            logBuilder.append("  - 源端连接成功: ").append(sourceConnector.getConnectorName()).append("\n\n");

            // 2. 判断是否开启流式模式
            com.alibaba.fastjson.JSONObject sourceCfg = com.alibaba.fastjson.JSON.parseObject(taskConfig.getSourceConfig());
            boolean streamMode = sourceCfg.getBooleanValue("streamMode");
            int fetchBatchSize = sourceCfg.getInteger("fetchBatchSize") != null ? sourceCfg.getInteger("fetchBatchSize") : 500;

            // 2. 从源端抽取数据
            logBuilder.append("[步骤2] 从源端抽取数据...\n");
            
            // 检查是否有辅助数据源（如果有，不能使用流式模式）
            List<AuxiliaryDatasource> auxiliaryDatasources = auxiliaryDatasourceService.listByTaskId(taskConfig.getId());
            if (!auxiliaryDatasources.isEmpty()) {
                logBuilder.append("  - 检测到辅助数据源，使用内存模式处理\n");
                streamMode = false;
            }
            
            if (streamMode && auxiliaryDatasources.isEmpty()) {
                // 流式处理模式：需要同时使用源和目标连接
                logBuilder.append("[步顴1.2] 建立目标端连接（使用HikariCP连接池）...\n");
                targetConn = connectionPoolManager.getConnection(targetConnector);
                logBuilder.append("  - 目标端连接成功: ").append(targetConnector.getConnectorName()).append("\n\n");
                
                logBuilder.append("  - 使用流式处理模式，批次大小: ").append(fetchBatchSize).append("\n");
                int totalCount = executeStreamMode(sourceConn, targetConn, taskConfig, fieldMappings, fetchBatchSize, logBuilder);
                
                logBuilder.append("\n========== ETL任务执行成功 ==========\n");
                logBuilder.append("结束时间: ").append(LocalDateTime.now()).append("\n");
                return EtlResult.success(totalCount, totalCount, logBuilder.toString());
            } else {
                // 内存模式：先抽取后关闭源连接，转换后再建立目标连接
                logBuilder.append("  - 使用内存模式处理\n");
                List<Map<String, Object>> sourceData = extractData(sourceConn, taskConfig, logBuilder);
                logBuilder.append("  - 抽取记录数: ").append(sourceData.size()).append("\n\n");

                if (sourceData.isEmpty()) {
                    logBuilder.append("源端无数据，任务结束\n");
                    return EtlResult.success(0, 0, logBuilder.toString());
                }
                
                // 2.5 关联辅助数据源（多表关联）
                if (!auxiliaryDatasources.isEmpty()) {
                    logBuilder.append("[步骤2.5] 关联辅助数据源...\n");
                    sourceData = joinAuxiliaryData(sourceData, auxiliaryDatasources, logBuilder);
                    logBuilder.append("  - 关联完成，记录数: ").append(sourceData.size()).append("\n\n");
                }
                
                // 关闭源端连接，释放资源
                logBuilder.append("  - 关闭源端连接，释放资源\n\n");
                closeConnection(sourceConn);
                sourceConn = null;

                // 3. 数据转换（无数据库连接，避免超时）
                logBuilder.append("[步骤3] 数据转换...\n");
                List<Map<String, Object>> transformedData = transformData(sourceData, fieldMappings, logBuilder);
                logBuilder.append("  - 转换完成，记录数: ").append(transformedData.size()).append("\n\n");
                
                // 释放源数据内存
                sourceData.clear();
                sourceData = null;

                // 4. 建立目标端连接（使用连接池）
                logBuilder.append("[步顴4] 建立目标端连接（使用HikariCP连接池）...\n");
                targetConn = connectionPoolManager.getConnection(targetConnector);
                logBuilder.append("  - 目标端连接成功: ").append(targetConnector.getConnectorName()).append("\n\n");
                
                // 5. 加载到目标端
                logBuilder.append("[步骤5] 加载数据到目标端...\n");
                int successCount = loadData(targetConn, targetConnector, transformedData, taskConfig, fieldMappings, logBuilder);
                logBuilder.append("  - 成功加载记录数: ").append(successCount).append("\n\n");
                
                // 5.5 推送后处理：状态回写（如果需要，重新建立源连接）
                String postLoadConfigStr = taskConfig.getPostLoadConfig();
                if (postLoadConfigStr != null && !postLoadConfigStr.trim().isEmpty()) {
                    JSONObject postLoadConfig = JSON.parseObject(postLoadConfigStr);
                    JSONObject statusUpdateConfig = postLoadConfig.getJSONObject("statusUpdate");
                    
                    if (statusUpdateConfig != null && statusUpdateConfig.getBooleanValue("enabled") 
                        && "SQL".equals(statusUpdateConfig.getString("updateType"))) {
                        // 需要执行SQL回写，重新建立源连接
                        logBuilder.append("[步骤5.5] 重新建立源连接用于状态回写...\n");
                        sourceConn = databaseConnector.getConnection(sourceConnector);
                        logBuilder.append("  - 源端连接成功\n\n");
                    }
                }
                
                // 需要传递sourceData给postLoadProcessor，但已释放，使用transformedData代替
                // 注意：transformedData中的字段名是目标字段名，需要映射回源字段
                List<Map<String, Object>> sourceDataForCallback = new ArrayList<>();
                for (Map<String, Object> targetRow : transformedData) {
                    Map<String, Object> sourceRow = new HashMap<>();
                    for (FieldMapping mapping : fieldMappings) {
                        sourceRow.put(mapping.getSourceField(), targetRow.get(mapping.getTargetField()));
                    }
                    sourceDataForCallback.add(sourceRow);
                }
                
                postLoadProcessor.process(sourceConn, sourceDataForCallback, transformedData, taskConfig, successCount, logBuilder);
                
                // 6. 记录数据血缘
                recordDataLineage(taskConfig, sourceConnector, targetConnector, fieldMappings, logBuilder);

                logBuilder.append("========== ETL任务执行成功 ==========\n");
                logBuilder.append("结束时间: ").append(LocalDateTime.now()).append("\n");

                return EtlResult.success(transformedData.size(), successCount, logBuilder.toString());
            }

        } catch (Exception e) {
            log.error("ETL任务执行失败", e);
            logBuilder.append("\n========== ETL任务执行失败 ==========\n");
            logBuilder.append("错误信息: ").append(e.getMessage()).append("\n");
            return EtlResult.failed(e.getMessage(), logBuilder.toString());
            
        } finally {
            closeConnection(sourceConn);
            closeConnection(targetConn);
        }
    }

    /**
     * 流式处理模式（性能优化版本）
     * 边抽取边转换边加载，避免大数据量占用内存
     */
    private int executeStreamMode(Connection sourceConn, Connection targetConn, 
                                  TaskConfig taskConfig, List<FieldMapping> fieldMappings,
                                  int batchSize, StringBuilder logBuilder) throws SQLException {
        
        JSONObject sourceConfig = JSON.parseObject(taskConfig.getSourceConfig());
        String sql = sourceConfig.getString("sql");
        
        // 处理增量同步
        if ("INCREMENTAL".equals(taskConfig.getSyncMode())) {
            sql = buildIncrementalSql(sql, taskConfig, logBuilder);
        }
        
        logBuilder.append("  - 执行SQL: ").append(sql).append("\n");
        
        JSONObject targetConfig = JSON.parseObject(taskConfig.getTargetConfig());
        String targetTable = targetConfig.getString("tableName");
        String writeMode = targetConfig.getString("writeMode"); // INSERT/UPDATE/UPSERT
        String primaryKey = targetConfig.getString("primaryKey"); // 主键字段
        
        // 获取目标字段列表
        List<String> targetFields = new ArrayList<>();
        for (FieldMapping mapping : fieldMappings) {
            targetFields.add(mapping.getTargetField());
        }
        
        // 根据写入模式构建SSQL
        String insertSql;
        if ("UPSERT".equals(writeMode)) {
            // UPSERT模式：使用ON DUPLICATE KEY UPDATE（MySQL）或MERGE（Oracle）
            insertSql = buildUpsertSql(targetTable, targetFields, primaryKey);
        } else if ("UPDATE".equals(writeMode)) {
            // UPDATE模式：只更新
            insertSql = buildUpdateSql(targetTable, targetFields, primaryKey);
        } else {
            // INSERT模式：默认
            insertSql = buildInsertSql(targetTable, targetFields);
        }
        
        int totalCount = 0;
        int successCount = 0;
        int batchCount = 0;
        
        Statement queryStmt = null;
        ResultSet rs = null;
        GroovyShell shell = new GroovyShell();
        
        try {
            // 创建查询Statement（流式读取）
            queryStmt = sourceConn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            
            // 根据数据库类型设置fetchSize（启用流式读取）
            String jdbcUrl = sourceConn.getMetaData().getURL();
            if (jdbcUrl.contains("mysql")) {
                // MySQL特殊处理：使用Integer.MIN_VALUE启用流式模式
                queryStmt.setFetchSize(Integer.MIN_VALUE);
                log.info("MySQL流式模式已启用，fetchSize: Integer.MIN_VALUE");
            } else if (jdbcUrl.contains("postgresql") || jdbcUrl.contains("kingbase") 
                    || jdbcUrl.contains("oracle") || jdbcUrl.contains(":dm:")) {
                // PostgreSQL/金仓/Oracle/达梦：使用标准fetchSize
                queryStmt.setFetchSize(batchSize);
                log.info("流式读取已启用，fetchSize: {}", batchSize);
            } else {
                // 其他数据库：尝试使用标准fetchSize
                queryStmt.setFetchSize(batchSize);
                log.warn("未识别的数据库类型，尝试使用标准fetchSize: {}", batchSize);
            }
            
            // 执行查询
            rs = queryStmt.executeQuery(sql);
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            
            List<Map<String, Object>> batchData = new ArrayList<>(batchSize);
            PreparedStatement insertStmt = targetConn.prepareStatement(insertSql);
            
            // 性能优化：每1批数据commit一次（减少事务大小，避免锁等待）
            int commitBatchCount = 1;  // 每1批commit一次（Kingbase事务中止问题优化）
            int currentBatchInTransaction = 0;  // 当前事务中的批次数
            
            try {
                targetConn.setAutoCommit(false);
                
                while (rs.next()) {
                    // 读取一行数据
                    Map<String, Object> sourceRow = new HashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        String columnName = metaData.getColumnLabel(i);
                        Object value = rs.getObject(i);
                        sourceRow.put(columnName, value);
                    }
                    
                    totalCount++;
                    
                    // 数据转换
                    Map<String, Object> targetRow = new HashMap<>();
                    boolean skipRow = false;
                    
                    for (FieldMapping mapping : fieldMappings) {
                        Object value = sourceRow.get(mapping.getSourceField());
                        
                        // 调试日志：输出每个字段的转换信息
                        if ("cjr_id".equals(mapping.getTargetField()) || "cjr_id".equals(mapping.getSourceField())) {
                            log.info("[cjr_id字段调试] 源字段: {}, 目标字段: {}, 原始值: {}, processorChain: {}, transformType: {}, constantValue: {}, defaultValue: {}",
                                mapping.getSourceField(), mapping.getTargetField(), value, 
                                mapping.getProcessorChain(), mapping.getTransformType(),
                                mapping.getConstantValue(), mapping.getDefaultValue());
                        }
                        
                        // 执行转换逻辑
                        if (mapping.getProcessorChain() != null && !mapping.getProcessorChain().trim().isEmpty()) {
                            value = executeProcessorChain(value, mapping, shell, sourceRow);
                            if (value == null && "SKIP".equals(getSkipFlag(mapping))) {
                                skipRow = true;
                                break;
                            }
                        } else {
                            value = executeLegacyProcessing(value, mapping, shell, sourceRow);
                            if (value == null && "SKIP".equals(mapping.getNullStrategy())) {
                                skipRow = true;
                                break;
                            }
                        }
                        
                        targetRow.put(mapping.getTargetField(), value);
                        
                        // 调试日志：输出转换后的值
                        if ("cjr_id".equals(mapping.getTargetField()) || "cjr_id".equals(mapping.getSourceField())) {
                            log.info("[cjr_id字段调试] 转换后的值: {}", value);
                        }
                    }
                    
                    if (!skipRow) {
                        batchData.add(targetRow);
                    }
                    
                    // 达到批次大小，立即执行批量写入
                    if (batchData.size() >= batchSize) {
                        batchCount++;  // 移到try外面，无论成功失败都统计批次数
                        long batchStartTime = System.currentTimeMillis(); // 记录批次开始时间
                        try {
                            // 批量设置参数
                            long paramStartTime = System.currentTimeMillis();
                            for (Map<String, Object> row : batchData) {
                                for (int j = 0; j < targetFields.size(); j++) {
                                    insertStmt.setObject(j + 1, row.get(targetFields.get(j)));
                                }
                                
                                if ("UPSERT".equals(writeMode)) {
                                    for (int j = 0; j < targetFields.size(); j++) {
                                        insertStmt.setObject(targetFields.size() + j + 1, row.get(targetFields.get(j)));
                                    }
                                } else if ("UPDATE".equals(writeMode)) {
                                    insertStmt.setObject(targetFields.size() + 1, row.get(targetFields.get(0)));
                                }
                                
                                insertStmt.addBatch();
                            }
                            long paramEndTime = System.currentTimeMillis();
                            
                            long executeBatchStartTime = System.currentTimeMillis();
                            insertStmt.executeBatch();
                            long executeBatchEndTime = System.currentTimeMillis();
                            
                            insertStmt.clearBatch();
                            successCount += batchData.size();
                            currentBatchInTransaction++;
                            
                            // 每1批commit一次，减少事务开销（Kingbase优化）
                            long commitStartTime = System.currentTimeMillis();
                            if (currentBatchInTransaction >= commitBatchCount) {
                                targetConn.commit();
                                currentBatchInTransaction = 0;
                                long commitEndTime = System.currentTimeMillis();
                                long totalBatchTime = commitEndTime - batchStartTime;
                                
                                log.info("流式模式[批量写入成功]：已提交第 {} 批，本批 {} 条，累计 {} 条", batchCount, batchData.size(), successCount);
                                log.info("[性能分析] 批次耗时: {}ms (参数设置:{}ms, executeBatch:{}ms, commit:{}ms)", 
                                    totalBatchTime, 
                                    paramEndTime - paramStartTime,
                                    executeBatchEndTime - executeBatchStartTime,
                                    commitEndTime - commitStartTime);
                            } else {
                                // 未达到commit条件，也记录批次信息
                                log.debug("流式模式[批量写入成功]：第 {} 批已执行，本批 {} 条，累计 {} 条（待提交）", batchCount, batchData.size(), successCount);
                            }
                            
                        } catch (SQLException e) {
                            // 批量失败，回滚当前事务
                            targetConn.rollback();
                            currentBatchInTransaction = 0;
                            insertStmt.clearBatch();
                            
                            // 详细记录批量失败的原因
                            log.error("==================== 批量写入失败诊断 ====================");
                            log.error("批次编号: {}", batchCount + 1);
                            log.error("数据量: {}", batchData.size());
                            log.error("写入模式: {}", writeMode);
                            log.error("目标表: {}", targetTable);
                            log.error("SQL语句: {}", insertSql);
                            log.error("错误类型: {}", e.getClass().getName());
                            log.error("SQLState: {}", e.getSQLState());
                            log.error("错误代码: {}", e.getErrorCode());
                            log.error("错误信息: {}", e.getMessage());
                            log.error("完整堆栈:", e);
                            log.error("=========================================================");
                            
                            log.warn("[性能警告] 批次写入失败，进入逐条重试模式（极慢）：批次={}, 数据量={}", batchCount + 1, batchData.size());
                            log.warn("[性能分析] 如果频繁出现此警告，说明存在大量主键冲突或JDBC参数未生效");
                            
                            int skipCount = 0; // 统计跳过数
                            for (int i = 0; i < batchData.size(); i++) {
                                Map<String, Object> row = batchData.get(i);
                                try {
                                    for (int j = 0; j < targetFields.size(); j++) {
                                        insertStmt.setObject(j + 1, row.get(targetFields.get(j)));
                                    }
                                    
                                    if ("UPSERT".equals(writeMode)) {
                                        for (int j = 0; j < targetFields.size(); j++) {
                                            insertStmt.setObject(targetFields.size() + j + 1, row.get(targetFields.get(j)));
                                        }
                                    } else if ("UPDATE".equals(writeMode)) {
                                        insertStmt.setObject(targetFields.size() + 1, row.get(targetFields.get(0)));
                                    }
                                    
                                    insertStmt.execute();
                                    targetConn.commit(); // 每条成功后立即commit
                                    successCount++;
                                    currentBatchInTransaction++;
                                } catch (SQLException singleEx) {
                                    try {
                                        targetConn.rollback(); // 失败后立即rollback，清除事务中止状态
                                    } catch (SQLException rollbackEx) {
                                        log.error("事务回滚失败", rollbackEx);
                                    }
                                    
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
                                        skipCount++;
                                        log.debug("跳过重复数据，索引: {}", totalCount - batchData.size() + i);
                                    } else {
                                        log.error("数据插入失败，索引: {}, 错误: {}", 
                                            totalCount - batchData.size() + i, errorMsg);
                                        throw singleEx;
                                    }
                                }
                            }
                            
                            if (skipCount > 0) {
                                log.warn("批次 {} 跳过 {} 条重复数据", batchCount + 1, skipCount);
                            }
                            
                            // 逼条重试后提交事务（注：由于每条都已经单独提交，这里不需要再提交）
                            // if (currentBatchInTransaction > 0) {
                            //     targetConn.commit();
                            //     currentBatchInTransaction = 0;
                            // }
                        }
                        
                        logBuilder.append("  - 已处理: ").append(totalCount)
                                 .append(" 条，已加载: ").append(successCount).append(" 条\n");
                        
                        batchData.clear();
                    }
                }
                
                // 处理最后不足一批的数据
                if (!batchData.isEmpty()) {
                    batchCount++;  // 移到try外面，无论成功失败都统计批次数
                    try {
                        for (Map<String, Object> row : batchData) {
                            for (int j = 0; j < targetFields.size(); j++) {
                                insertStmt.setObject(j + 1, row.get(targetFields.get(j)));
                            }
                            
                            if ("UPSERT".equals(writeMode)) {
                                for (int j = 0; j < targetFields.size(); j++) {
                                    insertStmt.setObject(targetFields.size() + j + 1, row.get(targetFields.get(j)));
                                }
                            } else if ("UPDATE".equals(writeMode)) {
                                insertStmt.setObject(targetFields.size() + 1, row.get(targetFields.get(0)));
                            }
                            
                            insertStmt.addBatch();
                        }
                        
                        insertStmt.executeBatch();
                        insertStmt.clearBatch();
                        successCount += batchData.size();
                        currentBatchInTransaction++;
                        
                    } catch (SQLException e) {
                        targetConn.rollback();
                        currentBatchInTransaction = 0;
                        insertStmt.clearBatch();
                        
                        // 详细记录批量失败的原因
                        log.error("==================== 最后批次写入失败诊断 ====================");
                        log.error("批次编号: {}", batchCount + 1);
                        log.error("数据量: {}", batchData.size());
                        log.error("写入模式: {}", writeMode);
                        log.error("目标表: {}", targetTable);
                        log.error("SQL语句: {}", insertSql);
                        log.error("错误类型: {}", e.getClass().getName());
                        log.error("SQLState: {}", e.getSQLState());
                        log.error("错误代码: {}", e.getErrorCode());
                        log.error("错误信息: {}", e.getMessage());
                        log.error("完整堆栈:", e);
                        log.error("=========================================================");
                        
                        int skipCount = 0; // 统计跳过数
                        for (int i = 0; i < batchData.size(); i++) {
                            Map<String, Object> row = batchData.get(i);
                            try {
                                for (int j = 0; j < targetFields.size(); j++) {
                                    insertStmt.setObject(j + 1, row.get(targetFields.get(j)));
                                }
                                
                                if ("UPSERT".equals(writeMode)) {
                                    for (int j = 0; j < targetFields.size(); j++) {
                                        insertStmt.setObject(targetFields.size() + j + 1, row.get(targetFields.get(j)));
                                    }
                                } else if ("UPDATE".equals(writeMode)) {
                                    insertStmt.setObject(targetFields.size() + 1, row.get(targetFields.get(0)));
                                }
                                
                                insertStmt.execute();
                                targetConn.commit(); // 每条成功后立即commit，避免事务中止影响后续数据
                                successCount++;
                                currentBatchInTransaction++;
                            } catch (SQLException singleEx) {
                                try {
                                    targetConn.rollback(); // 失败后立即rollback，清除PostgreSQL/Kingbase的事务中止状态
                                } catch (SQLException rollbackEx) {
                                    log.error("事务回滚失败", rollbackEx);
                                }
                                
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
                                    skipCount++;
                                    log.debug("跳过重复数据，索引: {}", totalCount - batchData.size() + i);
                                } else {
                                    log.error("数据插入失败，索引: {}, 错误: {}", 
                                        totalCount - batchData.size() + i, errorMsg);
                                    throw singleEx;
                                }
                            }
                        }
                        
                        if (skipCount > 0) {
                            log.warn("最后批次跳过 {} 条重复数据", skipCount);
                        }
                        
                        // 逼条重试后提交事务（注：由于每条都已经单独提交，这里不需要再提交）
                        // if (currentBatchInTransaction > 0) {
                        //     targetConn.commit();
                        //     currentBatchInTransaction = 0;
                        // }
                    }
                    
                    logBuilder.append("  - 已处理: ").append(totalCount)
                             .append(" 条，已加载: ").append(successCount).append(" 条\n");
                }
                
                // 最后提交剩余的事务
                if (currentBatchInTransaction > 0) {
                    targetConn.commit();
                    log.info("流式模式：最终提交，总计 {} 批，{} 条数据", batchCount, successCount);
                }
                
                targetConn.setAutoCommit(true);
                
            } finally {
                if (insertStmt != null) try { insertStmt.close(); } catch (SQLException e) {}
            }
            
            logBuilder.append("\n[步骤完成] 流式处理完成\n");
            logBuilder.append("  - 总抽取记录数: ").append(totalCount).append("\n");
            logBuilder.append("  - 成功加载记录数: ").append(successCount).append("\n");
            logBuilder.append("  - 批次数: ").append(batchCount).append("\n");
            
        } catch (SQLException e) {
            throw e;
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) {}
            if (queryStmt != null) try { queryStmt.close(); } catch (SQLException e) {}
        }
        
        return successCount;
    }
    
    /**
     * 批量写入数据（每次创建新的PreparedStatement，避免连接空闲）
     * 支持INSERT/UPDATE/UPSERT模式
     */
    private int batchInsertWithNewStatement(Connection conn, String insertSql, 
                                           List<Map<String, Object>> dataList, 
                                           List<String> targetFields) throws SQLException {
        // 直接调用通用方法，默认INSERT模式
        return batchWriteWithNewStatement(conn, insertSql, dataList, targetFields, "INSERT");
    }
    
    /**
     * 批量写入数据通用方法
     * 支持INSERT/UPDATE/UPSERT模式
     * 包含智能重试和主键冲突跳过逻辑
     */
    private int batchWriteWithNewStatement(Connection conn, String sql, 
                                          List<Map<String, Object>> dataList, 
                                          List<String> targetFields,
                                          String writeMode) throws SQLException {
        if (dataList == null || dataList.isEmpty()) {
            return 0;
        }
        
        int totalInserted = 0;
        PreparedStatement pstmt = null;
        
        try {
            conn.setAutoCommit(false);
            pstmt = conn.prepareStatement(sql);
            
            for (int i = 0; i < dataList.size(); i++) {
                Map<String, Object> row = dataList.get(i);
                
                // 设置参数
                for (int j = 0; j < targetFields.size(); j++) {
                    pstmt.setObject(j + 1, row.get(targetFields.get(j)));
                }
                
                // UPSERT模式：ON DUPLICATE KEY UPDATE需要重复设置字段值
                if ("UPSERT".equals(writeMode)) {
                    for (int j = 0; j < targetFields.size(); j++) {
                        pstmt.setObject(targetFields.size() + j + 1, row.get(targetFields.get(j)));
                    }
                }
                // UPDATE模式：需要额外设置WHERE条件的主键值
                else if ("UPDATE".equals(writeMode)) {
                    pstmt.setObject(targetFields.size() + 1, row.get(targetFields.get(0)));
                }
                
                pstmt.addBatch();
            }
            
            // 尝试批量执行
            try {
                int[] results = pstmt.executeBatch();
                conn.commit();
                totalInserted = dataList.size();
            } catch (SQLException e) {
                // 批量失败，回滚并逐条插入
                conn.rollback();
                pstmt.clearBatch();
                log.warn("批次插入失败，尝试逐条插入: {}", e.getMessage());
                
                // 逐条插入
                for (int i = 0; i < dataList.size(); i++) {
                    Map<String, Object> row = dataList.get(i);
                    try {
                        // 设置参数
                        for (int j = 0; j < targetFields.size(); j++) {
                            pstmt.setObject(j + 1, row.get(targetFields.get(j)));
                        }
                        
                        // UPSERT/UPDATE模式的额外参数
                        if ("UPSERT".equals(writeMode)) {
                            for (int j = 0; j < targetFields.size(); j++) {
                                pstmt.setObject(targetFields.size() + j + 1, row.get(targetFields.get(j)));
                            }
                        } else if ("UPDATE".equals(writeMode)) {
                            pstmt.setObject(targetFields.size() + 1, row.get(targetFields.get(0)));
                        }
                        
                        pstmt.execute();
                        conn.commit();
                        totalInserted++;
                    } catch (SQLException singleEx) {
                        conn.rollback();
                        
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
                            log.warn("跳过重复数据（主键冲突），索引: {}, 错误: {}", i, errorMsg);
                        } else {
                            // 非主键冲突错误，记录详细信息并抛出
                            log.error("数据插入失败（非主键冲突），索引: {}, 数据: {}, SQLState: {}, 错误: {}", 
                                i, row, sqlState, errorMsg);
                            throw singleEx;
                        }
                    }
                }
            }
            
            conn.setAutoCommit(true);
            return totalInserted;
            
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                    conn.setAutoCommit(true);
                } catch (SQLException ex) {
                    log.error("回滚失败", ex);
                }
            }
            throw e;
        } finally {
            if (pstmt != null) try { pstmt.close(); } catch (SQLException e) {}
        }
    }
    
    /**
     * 从源端抽取数据（内存模式）
     */
    private List<Map<String, Object>> extractData(Connection conn, TaskConfig taskConfig, 
                                                  StringBuilder logBuilder) throws SQLException {
        
        JSONObject sourceConfig = JSON.parseObject(taskConfig.getSourceConfig());
        String sql = sourceConfig.getString("sql");
        
        // 处理增量同步
        if ("INCREMENTAL".equals(taskConfig.getSyncMode())) {
            sql = buildIncrementalSql(sql, taskConfig, logBuilder);
        }
        
        logBuilder.append("  - 执行SQL: ").append(sql).append("\n");
        
        List<Map<String, Object>> result = new ArrayList<>();
        
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnLabel(i);
                    Object value = rs.getObject(i);
                    row.put(columnName, value);
                }
                result.add(row);
            }
        }
        
        return result;
    }

    /**
     * 构建增量SQL
     * 支持三种增量模式：
     * 1. 时间戳增量：基于时间字段（如 update_time > {last_sync_time}）
     * 2. 主键增量：基于自增主键（如 id > {last_max_id}）
     * 3. 水位增量：基于版本号/序列号等（如 version > {last_version}）
     */
    private String buildIncrementalSql(String originalSql, TaskConfig taskConfig, StringBuilder logBuilder) {
        String incrementalField = taskConfig.getIncrementalField();
        if (incrementalField == null || incrementalField.trim().isEmpty()) {
            logBuilder.append("  - [警告] 增量模式未配置增量字段，将执行全量抽取\n");
            return originalSql;
        }
        
        // 判断SQL是否已包含增量占位符
        if (originalSql.contains("{last_sync_time}") || 
            originalSql.contains("{last_max_id}") || 
            originalSql.contains("{last_watermark}")) {
            // 用户自定义增量SQL，直接替换占位符
            return replaceIncrementalPlaceholders(originalSql, taskConfig, logBuilder);
        }
        
        // 自动构建增量条件
        return autoAppendIncrementalCondition(originalSql, taskConfig, logBuilder);
    }
    
    /**
     * 替换增量占位符
     */
    private String replaceIncrementalPlaceholders(String sql, TaskConfig taskConfig, StringBuilder logBuilder) {
        LocalDateTime lastSyncTime = taskConfig.getLastSyncTime();
        
        if (lastSyncTime != null) {
            String formattedTime = lastSyncTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            sql = sql.replace("{last_sync_time}", "'" + formattedTime + "'");
            
            logBuilder.append("  - 增量模式: 时间戳，字段: ").append(taskConfig.getIncrementalField())
                     .append(", 最后同步时间: ").append(formattedTime).append("\n");
        } else {
            // 首次同步，移除增量条件（全量同步）
            sql = sql.replaceAll("(?i)\\s+AND\\s+.*?\\{last_sync_time\\}.*?", "");
            sql = sql.replaceAll("(?i)\\s+WHERE\\s+.*?\\{last_sync_time\\}.*?", "");
            logBuilder.append("  - 增量模式: 首次同步，执行全量抽取\n");
        }
        
        // 替换主键/水位占位符
        sql = sql.replace("{last_max_id}", lastSyncTime != null ? "0" : "0");
        sql = sql.replace("{last_watermark}", lastSyncTime != null ? "0" : "0");
        
        return sql;
    }
    
    /**
     * 自动追加增量条件
     */
    private String autoAppendIncrementalCondition(String sql, TaskConfig taskConfig, StringBuilder logBuilder) {
        String incrementalField = taskConfig.getIncrementalField();
        LocalDateTime lastSyncTime = taskConfig.getLastSyncTime();
        
        if (lastSyncTime == null) {
            logBuilder.append("  - 增量模式: 首次同步，执行全量抽取\n");
            return sql;
        }
        
        String formattedTime = lastSyncTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String incrementalCondition = String.format("%s > '%s'", incrementalField, formattedTime);
        
        // 判断SQL是否已包含WHERE子句
        if (sql.toUpperCase().contains(" WHERE ")) {
            sql = sql + " AND " + incrementalCondition;
        } else {
            // 找到FROM子句之后的位置插入WHERE
            int fromIndex = sql.toUpperCase().lastIndexOf(" FROM ");
            if (fromIndex > 0) {
                // 查找是否有ORDER BY/GROUP BY/LIMIT等子句
                int orderByIndex = sql.toUpperCase().indexOf(" ORDER BY ", fromIndex);
                int groupByIndex = sql.toUpperCase().indexOf(" GROUP BY ", fromIndex);
                int limitIndex = sql.toUpperCase().indexOf(" LIMIT ", fromIndex);
                
                int insertPos = sql.length();
                if (orderByIndex > 0) insertPos = Math.min(insertPos, orderByIndex);
                if (groupByIndex > 0) insertPos = Math.min(insertPos, groupByIndex);
                if (limitIndex > 0) insertPos = Math.min(insertPos, limitIndex);
                
                sql = sql.substring(0, insertPos) + " WHERE " + incrementalCondition + sql.substring(insertPos);
            } else {
                sql = sql + " WHERE " + incrementalCondition;
            }
        }
        
        logBuilder.append("  - 增量模式: 自动追加条件，字段: ").append(incrementalField)
                 .append(", 最后同步时间: ").append(formattedTime).append("\n");
        
        return sql;
    }

    /**
     * 数据转换
     * 处理顺序：空值处理 → 数据清洗 → 数据转换 → 数据验证 → 质量检查 → 数据脱敏
     */
    private List<Map<String, Object>> transformData(List<Map<String, Object>> sourceData,
                                                    List<FieldMapping> fieldMappings,
                                                    StringBuilder logBuilder) {
        
        List<Map<String, Object>> result = new ArrayList<>();
        GroovyShell shell = new GroovyShell();
        int filteredCount = 0;
        int validationErrorCount = 0;
        
        for (Map<String, Object> sourceRow : sourceData) {
            try {
                Map<String, Object> targetRow = new HashMap<>();
                boolean skipRow = false;
                
                for (FieldMapping mapping : fieldMappings) {
                    Object value = sourceRow.get(mapping.getSourceField());
                    
                    // 新版：支持动态处理器链（用户可调整顺序）
                    if (mapping.getProcessorChain() != null && !mapping.getProcessorChain().trim().isEmpty()) {
                        value = executeProcessorChain(value, mapping, shell, sourceRow);
                        if (value == null && "SKIP".equals(getSkipFlag(mapping))) {
                            skipRow = true; // SKIP策略：标记跳过整行数据
                            break; // 直接退出字段遍历
                        }
                    } else {
                        // 兼容旧版：固定顺序执行
                        value = executeLegacyProcessing(value, mapping, shell, sourceRow);
                        if (value == null && "SKIP".equals(mapping.getNullStrategy())) {
                            skipRow = true; // SKIP策略：标记跳过整行数据
                            break; // 直接退出字段遍历
                        }
                    }
                    
                    targetRow.put(mapping.getTargetField(), value);
                }
                
                if (!skipRow) {
                    result.add(targetRow);
                } else {
                    filteredCount++;
                }
                
            } catch (RuntimeException e) {
                // REMOVE_ROW异常，跳过该行
                if ("REMOVE_ROW".equals(e.getMessage())) {
                    filteredCount++;
                } else {
                    throw e;
                }
            }
        }
        
        logBuilder.append("  - 字段映射规则数: ").append(fieldMappings.size()).append("\n");
        if (validationErrorCount > 0) {
            logBuilder.append("  - 校验失败数据: ").append(validationErrorCount).append(" 条\n");
        }
        if (filteredCount > 0) {
            logBuilder.append("  - 过滤掉异常数据: ").append(filteredCount).append(" 条\n");
        }
        
        return result;
    }

    /**
     * 字段值转换
     */
    private Object transformValue(Object value, FieldMapping mapping, GroovyShell shell, Map<String, Object> sourceRow) {
        String transformType = mapping.getTransformType();
        // 固定值：不依赖源字段，优先返回配置的常量
        if ("CONSTANT".equals(transformType)) {
            String constVal = mapping.getConstantValue();
            return constVal != null ? constVal : mapping.getDefaultValue();
        }
        if (value == null && !"SCRIPT".equals(transformType)) {
            return mapping.getDefaultValue();
        }
        
        switch (transformType) {

            case "DIRECT":
                return value;
                
            case "SCRIPT":
                if (StringUtils.hasText(mapping.getTransformScript())) {
                    try {
                        // 使用缓存避免重复Groovy脚本编译（性能优化）
                        String scriptText = mapping.getTransformScript();
                        groovy.lang.Script script = scriptCache.computeIfAbsent(scriptText, s -> groovyShell.parse(s));
                        
                        // 设置Binding变量
                        Binding binding = new Binding();
                        binding.setVariable("value", value);
                        binding.setVariable("row", sourceRow);
                        script.setBinding(binding);
                        
                        // 执行脚本
                        return script.run();
                    } catch (Exception e) {
                        // Groovy脚本执行异常，返回默认值（避免打印大量ERROR日志）
                        log.debug("Groovy脚本执行异常: value={}, error={}", value, e.getMessage());
                        return !StringUtils.isEmpty(mapping.getDefaultValue())? mapping.getDefaultValue() : value;
                    }
                }
                return value;
                
            case "DICT":
                // 优先使用dictMappingId查询字典表，其次使用transformScript中的内置JSON
                if (mapping.getDictMappingId() != null) {
                    try {
                        // 使用字典输出模式
                        String outputMode = mapping.getDictOutputMode();
                        if (outputMode == null || outputMode.isEmpty()) {
                            outputMode = "TARGET_KEY";  // 默认输出目标编码
                        }
                        
                        Map<String, String> dictMap = dictMappingService.loadMappingCache(
                            mapping.getDictMappingId(),
                            mapping.getDictSourceTypeValue(),
                            outputMode
                        );
                        
                        String key = String.valueOf(value);
                        if (dictMap.containsKey(key)) {
                            return dictMap.get(key);
                        } else {
                            return mapping.getDefaultValue();
                        }
                    } catch (Exception e) {
                        log.warn("查询字典失败, dictId={}", mapping.getDictMappingId(), e);
                        return mapping.getDefaultValue();
                    }
                }
                // 备用: 使用transformScript中的JSON作为内置字典
                if (StringUtils.hasText(mapping.getTransformScript())) {
                    try {
                        Map<String, Object> dict = JSON.parseObject(mapping.getTransformScript());
                        String key = String.valueOf(value);
                        if (dict.containsKey(key)) {
                            return dict.get(key);
                        } else {
                            return mapping.getDefaultValue();
                        }
                    } catch (Exception ignore) {
                        return mapping.getDefaultValue();
                    }
                }
                return mapping.getDefaultValue();
                
            case "CONSTANT":
                // 固定值：不读取源字段，直接返回配置的值
                return mapping.getConstantValue() != null ? mapping.getConstantValue() : mapping.getDefaultValue();
                
            default:
                return value;
        }
    }
    
    /**
     * 执行处理器链（支持用户自定义顺序）
     */
    private Object executeProcessorChain(Object value, FieldMapping mapping, GroovyShell shell, Map<String, Object> sourceRow) {
        Object currentValue = value; // 使用局部变量跟踪最新值
        try {
            // 使用缓存避免重复JSON解析（性能优化）
            String processorChainJson = mapping.getProcessorChain();
            List<Map<String, Object>> processors = processorChainCache.computeIfAbsent(processorChainJson, json -> {
                List<JSONObject> processorList = JSON.parseArray(json, JSONObject.class);
                List<Map<String, Object>> procs = new ArrayList<>();
                for (JSONObject obj : processorList) {
                    procs.add(obj.getInnerMap());
                }
                
                // 按order排序
                procs.sort((p1, p2) -> {
                    Integer order1 = (Integer) p1.getOrDefault("order", 0);
                    Integer order2 = (Integer) p2.getOrDefault("order", 0);
                    return order1.compareTo(order2);
                });
                
                return procs;
            });
            
            // 依次执行各个处理器
            for (Map<String, Object> processor : processors) {
                String type = (String) processor.get("type");
                Map<String, Object> config = (Map<String, Object>) processor.getOrDefault("config", new HashMap<>());
                
                try {
                    switch (type) {
                        case "NULL_HANDLE":
                            // 空值处理
                            if (currentValue == null || String.valueOf(currentValue).trim().isEmpty()) {
                                String strategy = (String) config.getOrDefault("strategy", "KEEP");
                                String defaultValue = (String) config.get("defaultValue");
                                currentValue = advancedTransformer.handleNullValue(currentValue, strategy, defaultValue);
                                if (currentValue == null && "SKIP".equalsIgnoreCase(strategy)) {
                                    // SKIP策略：跳过后续所有处理器，直接返回null
                                    return null;
                                }
                            }
                            break;
                            
                        case "CLEANSE":
                            // 数据清洗：只在value不为null时执行
                            if (currentValue != null && mapping.getCleanseFunctions() != null && !mapping.getCleanseFunctions().trim().isEmpty()) {
                                currentValue = advancedTransformer.applyCleanseFunctionChain(currentValue, mapping.getCleanseFunctions());
                            }
                            break;
                            
                        case "TRANSFORM":
                            // 数据转换：只在value不为null时执行（除非是CONSTANT类型）
                            String transformType = (String) config.getOrDefault("transformType", "DIRECT");
                            
                            // 固定值类型：直接返回常量，不依赖源字段
                            if ("CONSTANT".equals(transformType)) {
                                Object constantValue = config.get("constantValue");
                                Object defaultValue = config.get("defaultValue");
                                
                                // 优先使用constantValue，其次defaultValue，如果都为null则保持currentValue不变
                                if (constantValue != null) {
                                    currentValue = constantValue;
                                    log.debug("[TRANSFORM-CONSTANT] 字段: {}, 使用constantValue: {}", mapping.getTargetField(), constantValue);
                                } else if (defaultValue != null) {
                                    currentValue = defaultValue;
                                    log.debug("[TRANSFORM-CONSTANT] 字段: {}, 使用defaultValue: {}", mapping.getTargetField(), defaultValue);
                                } else {
                                    // 都为null时保持currentValue，不要设置为空字符串
                                    log.warn("[TRANSFORM-CONSTANT] 字段: {} 的constantValue和defaultValue都为null，保持原值: {}", 
                                        mapping.getTargetField(), currentValue);
                                }
                            } else if (currentValue != null) {
                                // 其他转换类型：仅当value不为null时执行
                                FieldMapping tempMapping = new FieldMapping();
                                tempMapping.setTransformType(transformType);
                                Object dictMappingIdObj = config.get("dictMappingId");
                                Long dictMappingId = null;
                                if (dictMappingIdObj instanceof Number) {
                                    dictMappingId = ((Number) dictMappingIdObj).longValue();
                                } else if (dictMappingIdObj instanceof String) {
                                    String s = ((String) dictMappingIdObj).trim();
                                    if (!s.isEmpty()) {
                                        try { dictMappingId = Long.valueOf(s); } catch (Exception ignore) {}
                                    }
                                }
                                tempMapping.setDictMappingId(dictMappingId);
                                tempMapping.setDictSourceTypeValue((String) config.get("dictSourceTypeValue"));
                                tempMapping.setDictOutputMode((String) config.get("dictOutputMode"));
                                tempMapping.setTransformScript((String) config.get("transformScript"));
                                tempMapping.setDefaultValue((String) config.get("defaultValue"));
                                tempMapping.setConstantValue((String) config.get("constantValue"));
                                currentValue = transformValue(currentValue, tempMapping, shell, sourceRow);
                            }
                            break;
                    }
                } catch (Exception processorException) {
                    // 单个处理器执行失败，记录日志但继续使用当前值
                    log.debug("处理器 {} 执行异常: {}", type, processorException.getMessage());
                }
            }
            
            return currentValue;
            
        } catch (Exception e) {
            log.error("执行处理器链失败: {}", e.getMessage(), e);
            // 返回处理过程中的最新值，而不是初始值
            return currentValue;
        }
    }
    
    /**
     * 兼容旧版：固定顺序执行（空值 -> 清洗 -> 转换）
     */
    private Object executeLegacyProcessing(Object value, FieldMapping mapping, GroovyShell shell, Map<String, Object> sourceRow) {
        // 步骤1: 空值处理
        if (value == null || String.valueOf(value).trim().isEmpty()) {
            value = advancedTransformer.handleNullValue(value, mapping.getNullStrategy(), mapping.getDefaultValue());
            if (value == null && "SKIP".equalsIgnoreCase(mapping.getNullStrategy())) {
                return null; // SKIP策略：直接返回
            }
        }
        
        // 步骤2: 数据清洗：只在value不为null时执行
        if (value != null && mapping.getCleanseFunctions() != null && !mapping.getCleanseFunctions().trim().isEmpty()) {
            value = advancedTransformer.applyCleanseFunctionChain(value, mapping.getCleanseFunctions());
        }
        
        // 步骤3: 数据转换：只在value不为null时执行
        if (value != null) {
            value = transformValue(value, mapping, shell, sourceRow);
        }
        
        return value;
    }
    
    /**
     * 数据去重
     */
    private List<Map<String, Object>> removeDuplicates(
            List<Map<String, Object>> dataList,
            List<String> uniqueFields) {
        
        if (uniqueFields == null || uniqueFields.isEmpty()) {
            return dataList;
        }
        
        Set<String> seen = new HashSet<>();
        List<Map<String, Object>> result = new ArrayList<>();
        
        for (Map<String, Object> row : dataList) {
            String key = uniqueFields.stream()
                .map(field -> String.valueOf(row.get(field)))
                .collect(java.util.stream.Collectors.joining("_"));
            
            if (seen.add(key)) {
                result.add(row);
            }
        }
        
        log.info("去重前: {} 条，去重后: {} 条", dataList.size(), result.size());
        return result;
    }
    
    /**
     * 获取SKIP标记（从处理器链中解析）
     */
    private String getSkipFlag(FieldMapping mapping) {
        try {
            // 使用缓存避免重复JSON解析（性能优化）
            String processorChainJson = mapping.getProcessorChain();
            List<Map<String, Object>> processors = processorChainCache.computeIfAbsent(processorChainJson, json -> {
                List<JSONObject> processorList = JSON.parseArray(json, JSONObject.class);
                List<Map<String, Object>> procs = new ArrayList<>();
                for (JSONObject obj : processorList) {
                    procs.add(obj.getInnerMap());
                }
                return procs;
            });
            
            for (Map<String, Object> processor : processors) {
                if ("NULL_HANDLE".equals(processor.get("type"))) {
                    Map<String, Object> config = (Map<String, Object>) processor.getOrDefault("config", new HashMap<>());
                    return (String) config.getOrDefault("strategy", "KEEP");
                }
            }
        } catch (Exception e) {
            // 忽略解析错误
        }
        return "KEEP";
    }
    
    /**
     * 应用函数转换
     */
    private Object applyFunction(Object value, String func) {
        String expr = func.trim();
        try {
            if ("UPPER".equalsIgnoreCase(expr)) {
                return value == null ? null : String.valueOf(value).toUpperCase();
            }
            if ("LOWER".equalsIgnoreCase(expr)) {
                return value == null ? null : String.valueOf(value).toLowerCase();
            }
            if ("TRIM".equalsIgnoreCase(expr)) {
                return value == null ? null : String.valueOf(value).trim();
            }
            if ("CEIL".equalsIgnoreCase(expr)) {
                return value == null ? null : Math.ceil(Double.parseDouble(String.valueOf(value)));
            }
            if ("FLOOR".equalsIgnoreCase(expr)) {
                return value == null ? null : Math.floor(Double.parseDouble(String.valueOf(value)));
            }
            if ("ABS".equalsIgnoreCase(expr)) {
                return value == null ? null : Math.abs(Double.parseDouble(String.valueOf(value)));
            }
            if ("TO_INT".equalsIgnoreCase(expr)) {
                return value == null ? null : Integer.parseInt(String.valueOf(value));
            }
            if (expr.startsWith("TO_DECIMAL")) {
                int l = expr.indexOf('(');
                int r = expr.indexOf(')', l + 1);
                int scale = 0;
                if (l > 0 && r > l) {
                    String s = expr.substring(l + 1, r).trim();
                    if (!s.isEmpty()) scale = Integer.parseInt(s);
                }
                java.math.BigDecimal bd = new java.math.BigDecimal(String.valueOf(value));
                return bd.setScale(scale, java.math.RoundingMode.HALF_UP);
            }
            if (expr.startsWith("ROUND")) {
                int l = expr.indexOf('(');
                int r = expr.indexOf(')', l + 1);
                int scale = 0;
                if (l > 0 && r > l) {
                    String s = expr.substring(l + 1, r).trim();
                    if (!s.isEmpty()) scale = Integer.parseInt(s);
                }
                java.math.BigDecimal bd = new java.math.BigDecimal(String.valueOf(value));
                return bd.setScale(scale, java.math.RoundingMode.HALF_UP).toString();
            }
            if (expr.startsWith("SUBSTR")) {
                int l = expr.indexOf('(');
                int r = expr.indexOf(')', l + 1);
                if (l > 0 && r > l) {
                    String[] parts = expr.substring(l + 1, r).split(",");
                    int start = Integer.parseInt(parts[0].trim());
                    int end = parts.length > 1 ? Integer.parseInt(parts[1].trim()) : (value == null ? 0 : String.valueOf(value).length());
                    String s = value == null ? "" : String.valueOf(value);
                    int sLen = s.length();
                    int from = Math.max(0, Math.min(start, sLen));
                    int to = Math.max(from, Math.min(end, sLen));
                    return s.substring(from, to);
                }
            }
            if (expr.startsWith("REPLACE_REGEX")) {
                int l = expr.indexOf('(');
                int r = expr.indexOf(')', l + 1);
                if (l > 0 && r > l) {
                    String[] parts = expr.substring(l + 1, r).split(",");
                    String pattern = parts[0].trim().replaceAll("^'|'$", "");
                    String repl = parts.length > 1 ? parts[1].trim().replaceAll("^'|'$", "") : "";
                    String s = value == null ? "" : String.valueOf(value);
                    // 使用缓存避免重复编译正则表达式（性能优化）
                    java.util.regex.Pattern regex = patternCache.computeIfAbsent(pattern, java.util.regex.Pattern::compile);
                    return regex.matcher(s).replaceAll(repl);
                }
            }
            if (expr.startsWith("REPLACE")) {
                int l = expr.indexOf('(');
                int r = expr.indexOf(')', l + 1);
                if (l > 0 && r > l) {
                    String[] parts = expr.substring(l + 1, r).split(",");
                    String oldStr = parts[0].trim().replaceAll("^'|'$", "");
                    String newStr = parts.length > 1 ? parts[1].trim().replaceAll("^'|'$", "") : "";
                    String s = value == null ? "" : String.valueOf(value);
                    return s.replace(oldStr, newStr);
                }
            }
            if (expr.startsWith("LPAD") || expr.startsWith("RPAD")) {
                int l = expr.indexOf('(');
                int r = expr.indexOf(')', l + 1);
                if (l > 0 && r > l) {
                    String[] parts = expr.substring(l + 1, r).split(",");
                    String padChar = parts[0].trim().replaceAll("^'|'$", "");
                    int length = parts.length > 1 ? Integer.parseInt(parts[1].trim()) : 0;
                    String s = value == null ? "" : String.valueOf(value);
                    if (padChar.isEmpty()) padChar = "0";
                    char ch = padChar.charAt(0);
                    StringBuilder sb = new StringBuilder(s);
                    if (expr.startsWith("LPAD")) {
                        while (sb.length() < length) {
                            sb.insert(0, ch);
                        }
                    } else {
                        while (sb.length() < length) {
                            sb.append(ch);
                        }
                    }
                    return sb.toString();
                }
            }
            if (expr.startsWith("CONCAT_PREFIX")) {
                int l = expr.indexOf('(');
                int r = expr.indexOf(')', l + 1);
                String prefix = "";
                if (l > 0 && r > l) {
                    prefix = expr.substring(l + 1, r).trim().replaceAll("^'|'$", "");
                }
                return (prefix) + (value == null ? "" : String.valueOf(value));
            }
            if (expr.startsWith("CONCAT_SUFFIX")) {
                int l = expr.indexOf('(');
                int r = expr.indexOf(')', l + 1);
                String suffix = "";
                if (l > 0 && r > l) {
                    suffix = expr.substring(l + 1, r).trim().replaceAll("^'|'$", "");
                }
                return (value == null ? "" : String.valueOf(value)) + (suffix);
            }
            if (expr.startsWith("DATE_FORMAT")) {
                int l = expr.indexOf('(');
                int r = expr.indexOf(')', l + 1);
                if (l > 0 && r > l) {
                    String pattern = expr.substring(l + 1, r).trim().replaceAll("^'|'$", "");
                    java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern(pattern);
                    if (value instanceof java.time.LocalDateTime) {
                        return ((java.time.LocalDateTime) value).format(formatter);
                    }
                    if (value instanceof java.util.Date) {
                        return new java.text.SimpleDateFormat(pattern).format((java.util.Date) value);
                    }
                    if (value != null) {
                        String s = String.valueOf(value);
                        try {
                            java.time.LocalDateTime dt = java.time.LocalDateTime.parse(s);
                            return dt.format(formatter);
                        } catch (Exception ignore) {}
                    }
                }
            }
            // 新增函数: SPLIT - 字符串拆分
            if (expr.startsWith("SPLIT")) {
                int l = expr.indexOf('(');
                int r = expr.indexOf(')', l + 1);
                if (l > 0 && r > l) {
                    String[] parts = expr.substring(l + 1, r).split(",");
                    String delimiter = parts[0].trim().replaceAll("^'|'$", "");
                    int index = parts.length > 1 ? Integer.parseInt(parts[1].trim()) : 0;
                    String s = value == null ? "" : String.valueOf(value);
                    String[] arr = s.split(java.util.regex.Pattern.quote(delimiter));
                    return index >= 0 && index < arr.length ? arr[index] : "";
                }
            }
            // CONCAT - 多字段拼接
            if (expr.startsWith("CONCAT")) {
                int l = expr.indexOf('(');
                int r = expr.indexOf(')', l + 1);
                if (l > 0 && r > l) {
                    String text = expr.substring(l + 1, r).trim().replaceAll("^'|'$", "");
                    return (value == null ? "" : String.valueOf(value)) + text;
                }
            }
            // COALESCE - NULL处理
            if (expr.startsWith("COALESCE")) {
                int l = expr.indexOf('(');
                int r = expr.indexOf(')', l + 1);
                if (l > 0 && r > l) {
                    String defaultVal = expr.substring(l + 1, r).trim().replaceAll("^'|'$", "");
                    return value == null || "".equals(value) ? defaultVal : value;
                }
            }
            // LENGTH - 获取字符串长度
            if ("LENGTH".equalsIgnoreCase(expr)) {
                return value == null ? 0 : String.valueOf(value).length();
            }
            // REVERSE - 字符串反转
            if ("REVERSE".equalsIgnoreCase(expr)) {
                String s = value == null ? "" : String.valueOf(value);
                return new StringBuilder(s).reverse().toString();
            }
            // ADD - 数值加法
            if (expr.startsWith("ADD")) {
                int l = expr.indexOf('(');
                int r = expr.indexOf(')', l + 1);
                if (l > 0 && r > l) {
                    double num = Double.parseDouble(expr.substring(l + 1, r).trim());
                    double val = value == null ? 0 : Double.parseDouble(String.valueOf(value));
                    return val + num;
                }
            }
            // MULTIPLY - 数值乘法
            if (expr.startsWith("MULTIPLY")) {
                int l = expr.indexOf('(');
                int r = expr.indexOf(')', l + 1);
                if (l > 0 && r > l) {
                    double num = Double.parseDouble(expr.substring(l + 1, r).trim());
                    double val = value == null ? 0 : Double.parseDouble(String.valueOf(value));
                    return val * num;
                }
            }
            // URL_ENCODE - URL编码
            if ("URL_ENCODE".equalsIgnoreCase(expr)) {
                String s = value == null ? "" : String.valueOf(value);
                return java.net.URLEncoder.encode(s, "UTF-8");
            }
            // URL_DECODE - URL解码
            if ("URL_DECODE".equalsIgnoreCase(expr)) {
                String s = value == null ? "" : String.valueOf(value);
                return java.net.URLDecoder.decode(s, "UTF-8");
            }
            // BASE64_ENCODE - Base64编码
            if ("BASE64_ENCODE".equalsIgnoreCase(expr)) {
                String s = value == null ? "" : String.valueOf(value);
                return java.util.Base64.getEncoder().encodeToString(s.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
            // BASE64_DECODE - Base64解码
            if ("BASE64_DECODE".equalsIgnoreCase(expr)) {
                String s = value == null ? "" : String.valueOf(value);
                byte[] bytes = java.util.Base64.getDecoder().decode(s);
                return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
            }
            // MD5 - MD5哈希
            if ("MD5".equalsIgnoreCase(expr)) {
                String s = value == null ? "" : String.valueOf(value);
                java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
                byte[] bytes = md.digest(s.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                for (byte b : bytes) {
                    sb.append(String.format("%02x", b));
                }
                return sb.toString();
            }
            
            // ========== 数据脱敏函数 ==========
            
            // DESENSITIZE_PHONE - 手机号脱敏（保留前3后4位）
            if ("DESENSITIZE_PHONE".equalsIgnoreCase(expr)) {
                String s = value == null ? "" : String.valueOf(value);
                if (s.length() == 11) {
                    return s.substring(0, 3) + "****" + s.substring(7);
                }
                return s;
            }
            
            // DESENSITIZE_ID_CARD - 身份证号脱敏（保留前6后4位）
            if ("DESENSITIZE_ID_CARD".equalsIgnoreCase(expr)) {
                String s = value == null ? "" : String.valueOf(value);
                if (s.length() == 18) {
                    return s.substring(0, 6) + "********" + s.substring(14);
                } else if (s.length() == 15) {
                    return s.substring(0, 6) + "*****" + s.substring(11);
                }
                return s;
            }
            
            // DESENSITIZE_BANK_CARD - 银行卡号脱敏（保留前4后4位）
            if ("DESENSITIZE_BANK_CARD".equalsIgnoreCase(expr)) {
                String s = value == null ? "" : String.valueOf(value);
                if (s.length() >= 8) {
                    int starCount = s.length() - 8;
                    StringBuilder stars = new StringBuilder();
                    for (int i = 0; i < starCount; i++) {
                        stars.append("*");
                    }
                    return s.substring(0, 4) + stars.toString() + s.substring(s.length() - 4);
                }
                return s;
            }
            
            // DESENSITIZE_NAME - 姓名脱敏（保留姓氏）
            if ("DESENSITIZE_NAME".equalsIgnoreCase(expr)) {
                String s = value == null ? "" : String.valueOf(value);
                if (s.length() == 2) {
                    return s.substring(0, 1) + "*";
                } else if (s.length() == 3) {
                    return s.substring(0, 1) + "**";
                } else if (s.length() > 3) {
                    StringBuilder stars = new StringBuilder();
                    for (int i = 1; i < s.length(); i++) {
                        stars.append("*");
                    }
                    return s.substring(0, 1) + stars.toString();
                }
                return s;
            }
            
            // DESENSITIZE_EMAIL - 邮箱脱敏（保留前2位和@后的域名）
            if ("DESENSITIZE_EMAIL".equalsIgnoreCase(expr)) {
                String s = value == null ? "" : String.valueOf(value);
                int atIndex = s.indexOf('@');
                if (atIndex > 2) {
                    String prefix = s.substring(0, 2);
                    String domain = s.substring(atIndex);
                    int starCount = atIndex - 2;
                    StringBuilder stars = new StringBuilder();
                    for (int i = 0; i < starCount; i++) {
                        stars.append("*");
                    }
                    return prefix + stars.toString() + domain;
                }
                return s;
            }
            
            // DESENSITIZE_ADDRESS - 地址脱敏（保留省市，详细地址星号）
            if ("DESENSITIZE_ADDRESS".equalsIgnoreCase(expr)) {
                String s = value == null ? "" : String.valueOf(value);
                if (s.length() > 6) {
                    return s.substring(0, 6) + "****";
                }
                return s;
            }
        } catch (Exception ignore) {
            // 解析或执行失败，回退原值
        }
        return value;
    }

    /**
     * 加载数据到目标端
     */
    private int loadData(Connection conn, ConnectorInfo targetConnector, List<Map<String, Object>> data, 
                        TaskConfig taskConfig, List<FieldMapping> fieldMappings,
                        StringBuilder logBuilder) throws Exception {
        
        JSONObject sourceConfig = JSON.parseObject(taskConfig.getSourceConfig());
        JSONObject targetConfig = JSON.parseObject(taskConfig.getTargetConfig());
        String targetTable = targetConfig.getString("tableName");
        String writeMode = targetConfig.getString("writeMode"); // INSERT/UPDATE/REPLACE
        
        // 批次大小：优先从sourceConfig读取fetchBatchSize（执行策略配置）
        int batchSize = 1000; // 默认值
        if (sourceConfig != null && sourceConfig.getInteger("fetchBatchSize") != null) {
            batchSize = sourceConfig.getInteger("fetchBatchSize");
        } else if (targetConfig.getInteger("batchSize") != null) {
            // 兼容老数据：如果sourceConfig没有，尝试从targetConfig读取
            batchSize = targetConfig.getInteger("batchSize");
        }
        
        // 并行线程数：从配置读取，默认4（大数据量时自动启用）
        int parallelThreads = sourceConfig != null && sourceConfig.getInteger("parallelThreads") != null 
            ? sourceConfig.getInteger("parallelThreads") : 4;
        
        int maxRetries = targetConfig.getInteger("maxRetries") != null ? targetConfig.getInteger("maxRetries") : 3;
        String idempotentKey = targetConfig.getString("idempotentKey");

        logBuilder.append("  - 目标表: ").append(targetTable).append("\n");
        logBuilder.append("  - 写入模式: ").append(writeMode).append("\n");
        logBuilder.append("  - 批次大小: ").append(batchSize).append("\n");
        logBuilder.append("  - 最大重试次数: ").append(maxRetries).append("\n");

        // 目标字段列表
        List<String> targetFields = new ArrayList<>();
        for (FieldMapping mapping : fieldMappings) {
            targetFields.add(mapping.getTargetField());
        }

        // 简单幂等：根据配置的键进行去重
        List<Map<String, Object>> dataToWrite = data;
        if (idempotentKey != null && !idempotentKey.trim().isEmpty()) {
            String[] keys = idempotentKey.split(",");
            Set<String> seen = new HashSet<>();
            List<Map<String, Object>> dedup = new ArrayList<>(data.size());
            for (Map<String, Object> row : data) {
                StringBuilder keyBuilder = new StringBuilder();
                for (String k : keys) {
                    keyBuilder.append(String.valueOf(row.get(k.trim()))).append("|");
                }
                String compositeKey = keyBuilder.toString();
                if (!seen.contains(compositeKey)) {
                    seen.add(compositeKey);
                    dedup.add(row);
                }
            }
            logBuilder.append("  - 幂等去重: 从 ").append(data.size()).append(" 到 ").append(dedup.size()).append(" 条\n");
            dataToWrite = dedup;
        }

        // 根据写入模式执行批量写入
        int successCount = 0;
        
        // 获取主键字段
        String primaryKey = targetConfig.getString("primaryKey");
        
        // 根据写入模式执行不同逻辑
        if ("INSERT".equals(writeMode) || writeMode == null) {
            // INSERT模式：优先使用高性能加载器
            int totalSize = dataToWrite.size();
            String dbType = targetConnector != null ? targetConnector.getDbType() : null;
            
            try {
                // 尝试使用高性能加载器（针对MySQL、PostgreSQL、Oracle等）
                if (dbType != null && com.datapush.manager.engine.loader.HighPerformanceLoader.shouldUseHighPerformanceLoader(dbType, totalSize)) {
                    logBuilder.append("  - 启用高性能加载器：数据库=").append(dbType)
                              .append(", 记录数=").append(totalSize).append("\n");
                    successCount = com.datapush.manager.engine.loader.HighPerformanceLoader.load(
                        conn, dbType, targetTable, dataToWrite, targetFields
                    );
                    logBuilder.append("  - 高性能加载完成：成功=").append(successCount).append("条\n");
                } else if (totalSize >= batchSize * 2 && parallelThreads > 1) {
                    // 并行写入模式：构建INSERT SQL
                    logBuilder.append("  - 启用并行写入：总记录=").append(totalSize)
                              .append(", 线程数=").append(parallelThreads).append("\n");
                    String insertSql = buildInsertSql(targetTable, targetFields);
                    successCount = parallelBatchWrite(targetConnector, insertSql, dataToWrite, targetFields, "INSERT", batchSize, parallelThreads);
                } else {
                    // 单线程写入
                    successCount = com.datapush.manager.util.BatchProcessorUtil.batchInsert(conn, targetTable, dataToWrite, batchSize);
                }
            } catch (Exception ex) {
                // 高性能加载或并行写入失败，降级为单线程重试
                if ((dbType != null && com.datapush.manager.engine.loader.HighPerformanceLoader.shouldUseHighPerformanceLoader(dbType, totalSize)) 
                    || (totalSize >= batchSize * 2 && parallelThreads > 1)) {
                    logBuilder.append("  - 警告：高性能加载/并行写入失败，降级为单线程重试\n");
                    logBuilder.append("  - 失败原因: ").append(ex.getMessage()).append("\n");
                    successCount = com.datapush.manager.util.BatchProcessorUtil.batchInsert(conn, targetTable, dataToWrite, batchSize);
                } else {
                    logBuilder.append("  - 批量写入失败: ").append(ex.getMessage()).append("\n");
                    throw ex;
                }
            }
        } else {
            // UPDATE/UPSERT模式：构建SQL并手动执行
            String sql;
            if ("UPSERT".equals(writeMode)) {
                sql = buildUpsertSql(targetTable, targetFields, primaryKey);
            } else {
                sql = buildUpdateSql(targetTable, targetFields, primaryKey);
            }
            
            int totalSize = dataToWrite.size();
            
            try {
                if (totalSize >= batchSize * 2 && parallelThreads > 1) {
                    // 并行写入模式
                    logBuilder.append("  - 启用并行写入(").append(writeMode).append("):总记录=").append(totalSize)
                              .append(", 线程数=").append(parallelThreads).append("\n");
                    successCount = parallelBatchWrite(targetConnector, sql, dataToWrite, targetFields, writeMode, batchSize, parallelThreads);
                } else {
                    // 单线程写入
                    successCount = batchWriteData(conn, sql, dataToWrite, targetFields, writeMode, batchSize);
                }
            } catch (Exception ex) {
                if (totalSize >= batchSize * 2 && parallelThreads > 1) {
                    // 并行失败，降级为单线程重试
                    logBuilder.append("  - 警告：并行写入失败，降级为单线程重试\n");
                    successCount = batchWriteData(conn, sql, dataToWrite, targetFields, writeMode, batchSize);
                } else {
                    logBuilder.append("  - 批量写入失败: ").append(ex.getMessage()).append("\n");
                    throw ex;
                }
            }
        }

        return successCount;
    }

    /**
     * 构建INSERT SQL
     */
    private String buildInsertSql(String tableName, List<String> fields) {
        StringBuilder sql = new StringBuilder("INSERT INTO ");
        sql.append(tableName).append(" (");
        sql.append(String.join(", ", fields));
        sql.append(") VALUES (");
        sql.append(String.join(", ", Collections.nCopies(fields.size(), "?")));
        sql.append(")");
        return sql.toString();
    }
    
    /**
     * 构建UPDATE SQL
     * 格式：UPDATE table SET field1=?, field2=? WHERE primaryKey=?
     */
    private String buildUpdateSql(String tableName, List<String> fields, String primaryKey) {
        StringBuilder sql = new StringBuilder("UPDATE ");
        sql.append(tableName).append(" SET ");
        
        // SET子句：所有字段
        for (int i = 0; i < fields.size(); i++) {
            if (i > 0) sql.append(", ");
            sql.append(fields.get(i)).append(" = ?");
        }
        
        // WHERE子句：使用主键
        if (primaryKey != null && !primaryKey.trim().isEmpty()) {
            sql.append(" WHERE ").append(primaryKey).append(" = ?");
        } else {
            // 如果没有指定主键，默认使用第一个字段
            sql.append(" WHERE ").append(fields.get(0)).append(" = ?");
        }
        
        return sql.toString();
    }
    
    /**
     * 构建UPSERT SQL (INSERT ... ON DUPLICATE KEY UPDATE)
     * MySQL语法：INSERT INTO table (f1,f2) VALUES (?,?) ON DUPLICATE KEY UPDATE f1=?, f2=?
     */
    private String buildUpsertSql(String tableName, List<String> fields, String primaryKey) {
        StringBuilder sql = new StringBuilder("INSERT INTO ");
        sql.append(tableName).append(" (");
        sql.append(String.join(", ", fields));
        sql.append(") VALUES (");
        sql.append(String.join(", ", Collections.nCopies(fields.size(), "?")));
        sql.append(")")
           .append(" ON DUPLICATE KEY UPDATE ");
        
        // ON DUPLICATE KEY UPDATE子句
        for (int i = 0; i < fields.size(); i++) {
            if (i > 0) sql.append(", ");
            sql.append(fields.get(i)).append(" = ?");
        }
        
        return sql.toString();
    }
    
    /**
     * 批量写入数据（支持UPDATE/UPSERT模式）
     * 分批执行，每批提交
     */
    private int batchWriteData(Connection conn, String sql, 
                              List<Map<String, Object>> dataList, 
                              List<String> targetFields,
                              String writeMode,
                              int batchSize) throws SQLException {
        if (dataList == null || dataList.isEmpty()) {
            return 0;
        }
        
        int totalSuccess = 0;
        int totalSize = dataList.size();
        
        // 分批处理
        for (int start = 0; start < totalSize; start += batchSize) {
            int end = Math.min(start + batchSize, totalSize);
            List<Map<String, Object>> batch = dataList.subList(start, end);
            
            PreparedStatement pstmt = null;
            try {
                conn.setAutoCommit(false);
                pstmt = conn.prepareStatement(sql);
                
                for (Map<String, Object> row : batch) {
                    // 设置参数
                    for (int j = 0; j < targetFields.size(); j++) {
                        pstmt.setObject(j + 1, row.get(targetFields.get(j)));
                    }
                    
                    // UPSERT模式：ON DUPLICATE KEY UPDATE需要重复设置字段值
                    if ("UPSERT".equals(writeMode)) {
                        // 再次设置所有字段用于UPDATE部分
                        for (int j = 0; j < targetFields.size(); j++) {
                            pstmt.setObject(targetFields.size() + j + 1, row.get(targetFields.get(j)));
                        }
                    }
                    // UPDATE模式：需要额外设置WHERE条件的主键值
                    else if ("UPDATE".equals(writeMode)) {
                        // WHERE条件的主键值在最后
                        pstmt.setObject(targetFields.size() + 1, row.get(targetFields.get(0))); // 假设第一个字段是主键
                    }
                    
                    pstmt.addBatch();
                }
                
                pstmt.executeBatch();
                conn.commit();
                conn.setAutoCommit(true);
                
                totalSuccess += batch.size();
                
            } catch (SQLException e) {
                if (conn != null) {
                    try {
                        conn.rollback();
                        conn.setAutoCommit(true);
                    } catch (SQLException ex) {
                        log.error("回滚失败", ex);
                    }
                }
                throw e;
            } finally {
                if (pstmt != null) try { pstmt.close(); } catch (SQLException e) {}
            }
        }
        
        return totalSuccess;
    }
    
    /**
     * 并行写入数据（性能优化）
     * 将数据分片后多线程并行写入，大幅提升吨吐量
     * 
     * @param connectorInfo 连接器信息（用于创建多个连接）
     * @param sql SQL语句
     * @param dataList 数据列表
     * @param targetFields 目标字段
     * @param writeMode 写入模式
     * @param batchSize 批次大小
     * @param parallelThreads 并行线程数（默认4）
     */
    private int parallelBatchWrite(ConnectorInfo connectorInfo, String sql,
                                  List<Map<String, Object>> dataList,
                                  List<String> targetFields,
                                  String writeMode,
                                  int batchSize,
                                  int parallelThreads) throws Exception {
        if (dataList == null || dataList.isEmpty()) {
            return 0;
        }
        
        int totalSize = dataList.size();
        
        // 小数据量不必并行，直接单线程处理
        if (totalSize < batchSize * 2 || parallelThreads <= 1) {
            Connection conn = databaseConnector.getConnection(connectorInfo);
            try {
                return batchWriteData(conn, sql, dataList, targetFields, writeMode, batchSize);
            } finally {
                closeConnection(conn);
            }
        }
        
        // 创建线程池
        ExecutorService executor = Executors.newFixedThreadPool(parallelThreads);
        List<Future<Integer>> futures = new ArrayList<>();
        
        // 计算每个线程处理的数据量
        int chunkSize = (int) Math.ceil((double) totalSize / parallelThreads);
        
        log.info("并行写入模式：总记录数={}, 线程数={}, 每线程处理={} 条", 
            totalSize, parallelThreads, chunkSize);
        
        try {
            // 分片并提交到线程池
            for (int i = 0; i < parallelThreads; i++) {
                int start = i * chunkSize;
                int end = Math.min(start + chunkSize, totalSize);
                
                if (start >= totalSize) {
                    break;  // 数据已分配完
                }
                
                List<Map<String, Object>> chunk = dataList.subList(start, end);
                final int threadIndex = i;
                
                // 提交任务
                Future<Integer> future = executor.submit(() -> {
                    Connection conn = null;
                    try {
                        // 每个线程从连接池获取连接
                        conn = connectionPoolManager.getConnection(connectorInfo);
                        int count = batchWriteData(conn, sql, chunk, targetFields, writeMode, batchSize);
                        log.info("线程-{} 完成，写入 {} 条", threadIndex, count);
                        return count;
                    } catch (Exception e) {
                        log.error("线程-{} 写入失败", threadIndex, e);
                        throw new RuntimeException("并行写入失败: " + e.getMessage(), e);
                    } finally {
                        closeConnection(conn);
                    }
                });
                
                futures.add(future);
            }
            
            // 等待所有线程完成并汇总结果
            int totalSuccess = 0;
            for (Future<Integer> future : futures) {
                totalSuccess += future.get();  // 会阻塞直到线程完成
            }
            
            log.info("并行写入完成，总计成功 {} 条", totalSuccess);
            return totalSuccess;
            
        } finally {
            // 关闭线程池
            executor.shutdown();
            try {
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * 关联辅助数据源（多表关联）
     */
    private List<Map<String, Object>> joinAuxiliaryData(List<Map<String, Object>> mainData,
                                                        List<AuxiliaryDatasource> auxiliaryDatasources,
                                                        StringBuilder logBuilder) {
        try {
            for (AuxiliaryDatasource auxDs : auxiliaryDatasources) {
                logBuilder.append("  - 关联辅助数据源: ").append(auxDs.getAlias()).append("\n");
                
                // 获取辅助数据源的连接器
                ConnectorInfo auxConnector = connectorInfoService.getById(auxDs.getConnectorId());
                
                // 解析关联条件
                JSONObject joinCondition = JSON.parseObject(auxDs.getJoinCondition());
                String mainField = joinCondition.getString("mainField");
                String auxField = joinCondition.getString("auxField");
                
                // 抽取辅助数据源数据
                Map<Object, Map<String, Object>> auxDataMap = new HashMap<>();
                
                if ("DATABASE".equals(auxDs.getConnectorType())) {
                    // 数据库类型的辅助数据源
                    Connection auxConn = null;
                    try {
                        auxConn = databaseConnector.getConnection(auxConnector);
                        JSONObject auxConfig = JSON.parseObject(auxDs.getConfig());
                        String sql = auxConfig.getString("sql");
                        
                        try (Statement stmt = auxConn.createStatement();
                             ResultSet rs = stmt.executeQuery(sql)) {
                            ResultSetMetaData metaData = rs.getMetaData();
                            int columnCount = metaData.getColumnCount();
                            
                            while (rs.next()) {
                                Map<String, Object> row = new HashMap<>();
                                Object keyValue = null;
                                for (int i = 1; i <= columnCount; i++) {
                                    String columnName = metaData.getColumnLabel(i);
                                    Object value = rs.getObject(i);
                                    row.put(columnName, value);
                                    if (columnName.equals(auxField)) {
                                        keyValue = value;
                                    }
                                }
                                if (keyValue != null) {
                                    auxDataMap.put(keyValue, row);
                                }
                            }
                        }
                    } finally {
                        closeConnection(auxConn);
                    }
                } else if ("API".equals(auxDs.getConnectorType())) {
                    // API类型的辅助数据源
                    JSONObject auxConfig = JSON.parseObject(auxDs.getConfig());
                    String apiPath = auxConfig.getString("apiPath");
                    String apiMethod = auxConfig.getString("apiMethod");
                    String dataPath = auxConfig.getString("dataPath");
                    
                    Map<String, String> params = new HashMap<>();
                    JSONObject queryParams = auxConfig.getJSONObject("params");
                    if (queryParams != null) {
                        queryParams.forEach((k, v) -> params.put(k, String.valueOf(v)));
                    }
                    
                    JSONObject apiResponse;
                    if ("POST".equalsIgnoreCase(apiMethod)) {
                        apiResponse = apiConnector.post(auxConnector, apiPath, params);
                    } else {
                        apiResponse = apiConnector.get(auxConnector, apiPath, params);
                    }
                    
                    // 解析API响应
                    List<Map<String, Object>> auxDataList = extractDataFromJson(apiResponse, dataPath);
                    for (Map<String, Object> row : auxDataList) {
                        Object keyValue = row.get(auxField);
                        if (keyValue != null) {
                            auxDataMap.put(keyValue, row);
                        }
                    }
                }
                
                logBuilder.append("    辅助数据源记录数: ").append(auxDataMap.size()).append("\n");
                
                // 关联数据
                for (Map<String, Object> mainRow : mainData) {
                    Object mainKeyValue = mainRow.get(mainField);
                    Map<String, Object> auxRow = auxDataMap.get(mainKeyValue);
                    
                    if (auxRow != null) {
                        // LEFT JOIN：将辅助数据源的字段加入主数据，加前缀
                        for (Map.Entry<String, Object> entry : auxRow.entrySet()) {
                            mainRow.put(auxDs.getAlias() + "." + entry.getKey(), entry.getValue());
                        }
                    } else if ("INNER".equals(auxDs.getJoinType())) {
                        // INNER JOIN: 如果没有匹配，则标记为删除
                        mainRow.put("_TO_REMOVE", true);
                    }
                }
                
                // 如果是INNER JOIN，移除没有匹配的记录
                if ("INNER".equals(auxDs.getJoinType())) {
                    mainData.removeIf(row -> row.containsKey("_TO_REMOVE"));
                }
            }
        } catch (Exception e) {
            log.error("关联辅助数据源失败", e);
            logBuilder.append("  - 警告: 关联辅助数据源失败: ").append(e.getMessage()).append("\n");
        }
        
        return mainData;
    }
    
    /**
     * 从 JSON响应中提取数据
     */
    private List<Map<String, Object>> extractDataFromJson(JSONObject apiResponse, String dataPath) {
        List<Map<String, Object>> result = new ArrayList<>();
        try {
            Object data = apiResponse;
            if (StringUtils.hasText(dataPath)) {
                String[] paths = dataPath.split("\\.");
                for (String path : paths) {
                    if (data instanceof JSONObject) {
                        data = ((JSONObject) data).get(path);
                    }
                }
            }
            if (data instanceof com.alibaba.fastjson.JSONArray) {
                com.alibaba.fastjson.JSONArray array = (com.alibaba.fastjson.JSONArray) data;
                for (int i = 0; i < array.size(); i++) {
                    Object item = array.get(i);
                    if (item instanceof JSONObject) {
                        Map<String, Object> row = new HashMap<>(((JSONObject) item).getInnerMap());
                        result.add(row);
                    }
                }
            } else if (data instanceof JSONObject) {
                Map<String, Object> row = new HashMap<>(((JSONObject) data).getInnerMap());
                result.add(row);
            }
        } catch (Exception e) {
            log.warn("解析JSON数据失败", e);
        }
        return result;
    }

    /**
     * 关闭连接
     */
    /**
     * 流式执行（分批抽取→转换→写入）
     */
    private int executeStreaming(Connection sourceConn, Connection targetConn,
                                 TaskConfig taskConfig, List<FieldMapping> fieldMappings,
                                 int fetchBatchSize, List<String> uniqueFields,
                                 StringBuilder logBuilder) throws SQLException {
        com.alibaba.fastjson.JSONObject sourceConfig = com.alibaba.fastjson.JSON.parseObject(taskConfig.getSourceConfig());
        String sql = sourceConfig.getString("sql");
        
        // 处理增量同步（复用增强逻辑）
        if ("INCREMENTAL".equals(taskConfig.getSyncMode())) {
            sql = buildIncrementalSql(sql, taskConfig, logBuilder);
        }
        
        logBuilder.append("  - 抽取SQL(流式): ").append(sql).append("\n");

        // 预加载辅助数据源（用于JOIN）
        List<AuxiliaryDatasource> auxiliaryDatasources = auxiliaryDatasourceService.listByTaskId(taskConfig.getId());

        final int[] totalSuccess = {0};
        final int[] totalFetched = {0};

        com.datapush.manager.util.BatchProcessorUtil.batchFetch(sourceConn, sql, fetchBatchSize, batch -> {
            totalFetched[0] += batch.size();
            // 关联辅助数据源（对当前批次）
            if (!auxiliaryDatasources.isEmpty()) {
                List<Map<String, Object>> joined = joinAuxiliaryData(batch, auxiliaryDatasources, logBuilder);
                batch = joined;
            }
            // 转换（复用现有方法）
            List<Map<String, Object>> transformed = transformData(batch, fieldMappings, logBuilder);
            // 去重（如果配置了unique字段）
            if (!uniqueFields.isEmpty()) {
                int before = transformed.size();
                transformed = removeDuplicates(transformed, uniqueFields);
                logBuilder.append("  - 批次去重: ").append(before).append("→").append(transformed.size()).append(" 条\n");
            }
            try {
                int ok = loadData(targetConn, null, transformed, taskConfig, fieldMappings, logBuilder); // targetConnector为null（流式模式不用并行）
                totalSuccess[0] += ok;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            // 清理批次内存
            com.datapush.manager.util.BatchProcessorUtil.clearData(transformed);
        });

        logBuilder.append("  - 流式抽取总记录: ").append(totalFetched[0]).append("，成功写入: ").append(totalSuccess[0]).append("\n\n");
        return totalSuccess[0];
    }

    private void closeConnection(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                log.error("关闭数据库连接失败", e);
            }
        }
    }
    
    /**
     * 记录数据血缘
     */
    private void recordDataLineage(TaskConfig taskConfig,
                                   ConnectorInfo sourceConnector,
                                   ConnectorInfo targetConnector,
                                   List<FieldMapping> fieldMappings,
                                   StringBuilder logBuilder) {
        try {
            JSONObject sourceConfig = JSON.parseObject(taskConfig.getSourceConfig());
            JSONObject targetConfig = JSON.parseObject(taskConfig.getTargetConfig());
            
            // 从配置中获取表名，如果不存在则从 SQL 中提取
            String sourceTable = sourceConfig.getString("tableName");
            if (sourceTable == null || sourceTable.trim().isEmpty()) {
                // 尝试从 SQL 中提取表名
                String sql = sourceConfig.getString("sql");
                if (sql != null && !sql.trim().isEmpty()) {
                    sourceTable = extractTableNameFromSql(sql);
                }
            }
            
            String targetTable = targetConfig.getString("tableName");
            if (targetTable == null || targetTable.trim().isEmpty()) {
                targetTable = "unknown";
            }
            
            List<DataLineage> lineageList = new ArrayList<>();
            
            for (FieldMapping mapping : fieldMappings) {
                DataLineage lineage = new DataLineage();
                lineage.setTaskId(taskConfig.getId());
                lineage.setSourceConnectorId(sourceConnector.getId());
                lineage.setSourceTable(sourceTable != null ? sourceTable : "unknown");
                lineage.setSourceField(mapping.getSourceField());
                lineage.setTargetConnectorId(targetConnector.getId());
                lineage.setTargetTable(targetTable);
                lineage.setTargetField(mapping.getTargetField());
                lineage.setTransformType(mapping.getTransformType());
                
                // 构建转换规则描述
                String transformRule = buildTransformRule(mapping);
                lineage.setTransformRule(transformRule);
                lineage.setFieldMappingId(mapping.getId());
                
                lineageList.add(lineage);
            }
            
            // 删除旧的血缘记录
            dataLineageService.deleteByTaskId(taskConfig.getId());
            
            // 批量记录新的血缘
            dataLineageService.batchRecordLineage(lineageList);
            
            logBuilder.append("[步骤5] 记录数据血缘...\n");
            logBuilder.append("  - 记录血缘关系: ").append(lineageList.size()).append(" 条\n");
            logBuilder.append("  - 源表: ").append(sourceTable).append(", 目标表: ").append(targetTable).append("\n");
            
            // 输出详细血缘信息（仅输出前3条作为示例）
            for (int i = 0; i < Math.min(3, lineageList.size()); i++) {
                DataLineage lineage = lineageList.get(i);
                logBuilder.append("  - 示例").append(i + 1).append(": ")
                    .append(sourceTable).append(".").append(lineage.getSourceField())
                    .append(" -> ")
                    .append(targetTable).append(".").append(lineage.getTargetField())
                    .append(" [").append(lineage.getTransformType()).append("]\n");
            }
            if (lineageList.size() > 3) {
                logBuilder.append("  - ... 还有 ").append(lineageList.size() - 3).append(" 条\n");
            }
            logBuilder.append("\n");
            
        } catch (Exception e) {
            log.warn("记录数据血缘失败", e);
        }
    }
    
    /**
     * 从 SQL 中提取表名
     * 支持简单的 SELECT * FROM table 和 SELECT ... FROM table 格式
     */
    private String extractTableNameFromSql(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return "unknown";
        }
        
        try {
            // 转换为大写并移除多余空格
            String upperSql = sql.toUpperCase().replaceAll("\\s+", " ").trim();
            
            // 查找 FROM 关键字
            int fromIndex = upperSql.indexOf(" FROM ");
            if (fromIndex == -1) {
                return "unknown";
            }
            
            // 提取 FROM 后面的内容
            String afterFrom = upperSql.substring(fromIndex + 6).trim();
            
            // 提取第一个词（表名）
            String[] words = afterFrom.split("\\s+");
            if (words.length > 0) {
                String tableName = words[0];
                // 移除可能的逗号或分号
                tableName = tableName.replaceAll("[,;]", "");
                // 移除可能的引号
                tableName = tableName.replaceAll("[`'\"\\[\\]]", "");
                // 返回原始大小写的表名（从原始 SQL 中提取）
                int originalFromIndex = sql.toUpperCase().indexOf(" FROM ");
                String originalAfterFrom = sql.substring(originalFromIndex + 6).trim();
                String[] originalWords = originalAfterFrom.split("\\s+");
                if (originalWords.length > 0) {
                    String originalTableName = originalWords[0];
                    originalTableName = originalTableName.replaceAll("[,;]", "");
                    originalTableName = originalTableName.replaceAll("[`'\"\\[\\]]", "");
                    return originalTableName;
                }
                return tableName;
            }
            
            return "unknown";
        } catch (Exception e) {
            log.warn("从 SQL 中提取表名失败: {}", sql, e);
            return "unknown";
        }
    }
    
    /**
     * 构建转换规则描述
     */
    private String buildTransformRule(FieldMapping mapping) {
        StringBuilder rule = new StringBuilder();
        
        switch (mapping.getTransformType()) {
            case "DIRECT":
                rule.append("直接映射");
                break;
            case "FUNCTION":
                rule.append("函数转换: ").append(mapping.getTransformFunction());
                break;
            case "DICT":
                rule.append("字典映射");
                break;
            case "SCRIPT":
                rule.append("脚本转换");
                break;
            case "CONDITION":
                rule.append("条件转换");
                break;
            default:
                rule.append("未知转换类型");
        }
        
        // 添加数据清洗信息
        if (mapping.getNullStrategy() != null) {
            rule.append("; 空值处理: ").append(mapping.getNullStrategy());
        }
        
        return rule.toString();
    }
    
    /**
     * 多目标模式执行
     */
    private EtlResult executeMultiTarget(TaskConfig taskConfig, 
                                        ConnectorInfo sourceConnector,
                                        List<FieldMapping> allFieldMappings,
                                        Long logId) {
        StringBuilder logBuilder = new StringBuilder();
        logBuilder.append("========== 多目标ETL任务开始执行 ==========\n");
        logBuilder.append("任务名称: ").append(taskConfig.getTaskName()).append("\n");
        logBuilder.append("开始时间: ").append(LocalDateTime.now()).append("\n\n");
        
        Connection sourceConn = null;
        int totalCount = 0;
        int totalSuccess = 0;
        
        try {
            // 1. 建立源端连接
            logBuilder.append("[步骤1] 建立源端数据库连接...\n");
            sourceConn = databaseConnector.getConnection(sourceConnector);
            logBuilder.append("  - 源端连接成功: ").append(sourceConnector.getConnectorName()).append("\n\n");
            
            // 2. 从源端抽取数据（一次抽取）
            logBuilder.append("[步骤2] 从源端抽取数据...\n");
            List<Map<String, Object>> sourceData = extractData(sourceConn, taskConfig, logBuilder);
            totalCount = sourceData.size();
            logBuilder.append("  - 抽取记录数: ").append(totalCount).append("\n\n");
            
            if (sourceData.isEmpty()) {
                logBuilder.append("源端无数据，任务结束\n");
                return EtlResult.success(0, 0, logBuilder.toString());
            }
            
            // 2.5 关联辅助数据源（多表关联）
            List<AuxiliaryDatasource> auxiliaryDatasources = auxiliaryDatasourceService.listByTaskId(taskConfig.getId());
            if (!auxiliaryDatasources.isEmpty()) {
                logBuilder.append("[步骤2.5] 关联辅助数据源...\n");
                sourceData = joinAuxiliaryData(sourceData, auxiliaryDatasources, logBuilder);
                logBuilder.append("  - 关联完成，记录数: ").append(sourceData.size()).append("\n\n");
            }
            
            // 3. 获取所有目标配置
            List<com.datapush.manager.entity.TaskTarget> targets = taskTargetService.getEnabledTargetsByTaskId(taskConfig.getId());
            logBuilder.append("[步骤3] 找到").append(targets.size()).append("个目标表\n\n");
            
            if (targets.isEmpty()) {
                logBuilder.append("未配置目标表，任务结束\n");
                return EtlResult.success(totalCount, 0, logBuilder.toString());
            }
            
            // 4. 逐个目标处理
            for (int i = 0; i < targets.size(); i++) {
                com.datapush.manager.entity.TaskTarget target = targets.get(i);
                logBuilder.append("------ 目标").append(i + 1).append(": ").append(target.getTargetName()).append(" ------\n");
                
                try {
                    // 4.1 获取该目标的连接器
                    ConnectorInfo targetConnector = connectorInfoService.getById(target.getTargetConnectorId());
                    if (targetConnector == null) {
                        logBuilder.append("  - ❗ 跳过: 目标连接器不存在\n\n");
                        continue;
                    }
                    
                    logBuilder.append("  - 目标连接器: ").append(targetConnector.getConnectorName()).append("\n");
                    
                    // 4.2 获取该目标的字段映射
                    List<FieldMapping> targetMappings = allFieldMappings.stream()
                        .filter(m -> target.getId().equals(m.getTargetId()))
                        .collect(java.util.stream.Collectors.toList());
                    
                    if (targetMappings.isEmpty()) {
                        logBuilder.append("  - ❗ 跳过: 未配置字段映射\n\n");
                        continue;
                    }
                    
                    logBuilder.append("  - 字段映射数: ").append(targetMappings.size()).append("\n");
                    
                    // 4.3 数据转换（每个目标独立转换）
                    List<Map<String, Object>> transformedData = transformData(
                        new ArrayList<>(sourceData), targetMappings, logBuilder);
                    
                    logBuilder.append("  - 转换后记录数: ").append(transformedData.size()).append("\n");
                    
                    // 4.4 加载到目标表
                    Connection targetConn = null;
                    try {
                        targetConn = databaseConnector.getConnection(targetConnector);
                        
                        int successCount = loadDataToTarget(
                            targetConn, transformedData, target, targetMappings, logBuilder);
                        
                        totalSuccess += successCount;
                        logBuilder.append("  - ✅ 成功写入: ").append(successCount).append("条\n");
                        
                        // 4.5 推送后处理：状态回写（每个目标都进行处理）
                        postLoadProcessor.process(sourceConn, sourceData, transformedData, taskConfig, successCount, logBuilder);
                        
                        // 4.6 记录数据血缘
                        recordDataLineage(taskConfig, sourceConnector, targetConnector, targetMappings, logBuilder);
                        
                    } finally {
                        closeConnection(targetConn);
                    }
                    
                    logBuilder.append("\n");
                    
                } catch (Exception e) {
                    logBuilder.append("  - ❌ 目标处理失败: ").append(e.getMessage()).append("\n\n");
                    log.error("目标{}处理失败", target.getTargetName(), e);
                    // 继续处理下一个目标
                }
            }
            
            logBuilder.append("========== 多目标ETL任务执行完成 ==========\n");
            logBuilder.append("总抽取: ").append(totalCount).append("条\n");
            logBuilder.append("总写入: ").append(totalSuccess).append("条\n");
            logBuilder.append("结束时间: ").append(LocalDateTime.now()).append("\n");
            
            return EtlResult.success(totalCount, totalSuccess, logBuilder.toString());
            
        } catch (Exception e) {
            log.error("多目标ETL任务执行失败", e);
            logBuilder.append("\n========== 任务执行失败 ==========\n");
            logBuilder.append("错误: ").append(e.getMessage()).append("\n");
            return EtlResult.failed(e.getMessage(), logBuilder.toString());
            
        } finally {
            closeConnection(sourceConn);
        }
    }
    
    /**
     * 加载数据到指定目标表
     */
    private int loadDataToTarget(Connection conn, 
                                List<Map<String, Object>> data,
                                com.datapush.manager.entity.TaskTarget target,
                                List<FieldMapping> fieldMappings,
                                StringBuilder logBuilder) throws SQLException {
        
        JSONObject targetConfig = JSON.parseObject(target.getTargetConfig());
        String targetTable = targetConfig.getString("tableName");
        String writeMode = targetConfig.getString("writeMode"); // INSERT/UPDATE/REPLACE
        int batchSize = targetConfig.getInteger("batchSize") != null ? targetConfig.getInteger("batchSize") : 1000;
        int maxRetries = targetConfig.getInteger("maxRetries") != null ? targetConfig.getInteger("maxRetries") : 3;
        String idempotentKey = targetConfig.getString("idempotentKey");
        
        logBuilder.append("  - 目标表: ").append(targetTable).append("\n");
        logBuilder.append("  - 写入模式: ").append(writeMode).append("\n");
        logBuilder.append("  - 批次大小: ").append(batchSize).append("\n");
        
        // 幂等去重处理
        List<Map<String, Object>> dataToWrite = data;
        if (idempotentKey != null && !idempotentKey.trim().isEmpty()) {
            logBuilder.append("  - 幂等处理: 根据字段").append(idempotentKey).append("去重\n");
            
            // 去重逻辑：根据idempotentKey字段值去重
            Map<Object, Map<String, Object>> deduplicateMap = new java.util.LinkedHashMap<>();
            for (Map<String, Object> row : data) {
                Object keyValue = row.get(idempotentKey);
                if (keyValue != null) {
                    deduplicateMap.put(keyValue, row);
                }
            }
            
            List<Map<String, Object>> dedup = new ArrayList<>(deduplicateMap.values());
            logBuilder.append("  - 去重后记录数: ").append(dedup.size()).append("\n");
            dataToWrite = dedup;
        }
        
        // 使用批量插入（分批执行），并带重试
        int successCount = 0;
        int attempt = 0;
        long delay = 500L;
        while (true) {
            try {
                successCount = com.datapush.manager.util.BatchProcessorUtil.batchInsert(conn, targetTable, dataToWrite, batchSize);
                break;
            } catch (SQLException ex) {
                attempt++;
                if (attempt > maxRetries) {
                    logBuilder.append("  - 批量写入失败，已超过重试次数: ").append(ex.getMessage()).append("\n");
                    throw ex;
                }
                log.warn("批量写入失败，准备重试({}/{})", attempt, maxRetries, ex);
                logBuilder.append("  - 写入失败，重试 ").append(attempt).append("/").append(maxRetries).append("，等待 ").append(delay).append(" ms\n");
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
                delay = Math.min(delay * 2, 5000L);
            }
        }
        
        return successCount;
    }
}
