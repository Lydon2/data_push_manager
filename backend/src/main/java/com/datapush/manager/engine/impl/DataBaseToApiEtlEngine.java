package com.datapush.manager.engine.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.datapush.manager.connector.config.BatchConfig;
import com.datapush.manager.connector.impl.ApiConnector;
import com.datapush.manager.connector.impl.DatabaseConnector;
import com.datapush.manager.connector.result.BatchPushResult;
import com.datapush.manager.engine.EtlEngine;
import com.datapush.manager.engine.PostLoadProcessor;
import com.datapush.manager.entity.ConnectorInfo;
import com.datapush.manager.entity.FieldMapping;
import com.datapush.manager.entity.TaskConfig;
import com.datapush.manager.service.DictMappingService;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Database To API ETL引擎
 * 从数据库读取并推送到REST API
 */
@Slf4j
@Component
public class DataBaseToApiEtlEngine implements EtlEngine {

    @Autowired
    private DatabaseConnector databaseConnector;

    @Autowired
    private ApiConnector apiConnector;

    @Autowired
    private DictMappingService dictMappingService;
    
    @Autowired
    private PostLoadProcessor postLoadProcessor;
    
    @Autowired
    private com.datapush.manager.transformer.AdvancedTransformer advancedTransformer;
    
    /**
     * Groovy脚本缓存（性能优化）
     */
    private final Map<String, groovy.lang.Script> scriptCache = new java.util.concurrent.ConcurrentHashMap<>();
    private final GroovyShell groovyShell = new GroovyShell();

    @Override
    public EtlResult execute(TaskConfig taskConfig,
                            ConnectorInfo sourceConnector,
                            ConnectorInfo targetConnector,
                            List<FieldMapping> fieldMappings,
                            Long logId) {

        StringBuilder logBuilder = new StringBuilder();
        logBuilder.append("========== Database To API ETL任务开始 ==========\n");
        logBuilder.append("任务名称: ").append(taskConfig.getTaskName()).append("\n");
        logBuilder.append("开始时间: ").append(LocalDateTime.now()).append("\n\n");

        Connection sourceConn = null;

        try {
            // 1. 从数据库抽取数据
            logBuilder.append("[步骤1] 从数据库抽取数据...\n");
            sourceConn = databaseConnector.getConnection(sourceConnector);
            List<Map<String, Object>> sourceData = extractDataFromDatabase(sourceConn, taskConfig, logBuilder);
            logBuilder.append("  - 抽取记录数: ").append(sourceData.size()).append("\n\n");

            if (sourceData.isEmpty()) {
                logBuilder.append("源端无数据，任务结束\n");
                return EtlResult.success(0, 0, logBuilder.toString());
            }

            // 2. 数据转换
            logBuilder.append("[步骤2] 数据转换...\n");
            List<Map<String, Object>> transformedData = transformData(sourceData, fieldMappings, logBuilder);
            logBuilder.append("  - 转换完成，记录数: ").append(transformedData.size()).append("\n\n");

            // 3. 推送到API
            logBuilder.append("[步骤3] 推送数据到API...\n");
            int successCount = pushDataToApi(targetConnector, transformedData, taskConfig, logBuilder);
            logBuilder.append("  - 成功推送记录数: ").append(successCount).append("\n\n");
            
            // 3.5 推送后处理：状态回写
            postLoadProcessor.process(sourceConn, sourceData, transformedData, taskConfig, successCount, logBuilder);

            logBuilder.append("========== ETL任务执行成功 ==========\n");
            logBuilder.append("结束时间: ").append(LocalDateTime.now()).append("\n");

            return EtlResult.success(sourceData.size(), successCount, logBuilder.toString());

        } catch (Exception e) {
            log.error("Database To API ETL任务执行失败", e);
            logBuilder.append("\n========== ETL任务执行失败 ==========\n");
            logBuilder.append("错误信息: ").append(e.getMessage()).append("\n");
            return EtlResult.failed(e.getMessage(), logBuilder.toString());

        } finally {
            if (sourceConn != null) {
                try {
                    sourceConn.close();
                } catch (Exception e) {
                    log.warn("关闭数据库连接失败", e);
                }
            }
        }
    }

    /**
     * 从数据库抽取数据
     */
    private List<Map<String, Object>> extractDataFromDatabase(Connection conn, TaskConfig taskConfig,
                                                              StringBuilder logBuilder) throws Exception {

        JSONObject sourceConfig = JSON.parseObject(taskConfig.getSourceConfig());
        String sql = sourceConfig.getString("sql");

        // 处理增量同步
        if ("INCREMENTAL".equals(taskConfig.getSyncMode())) {
            String incrementalField = taskConfig.getIncrementalField();
            LocalDateTime lastSyncTime = taskConfig.getLastSyncTime();
            
            // 判断SQL是否已包含增量占位符
            if (sql.contains("{last_sync_time}")) {
                // 方式二：用户自定义增量 SQL，直接替换占位符
                if (lastSyncTime != null) {
                    // 非首次执行：替换增量占位符
                    String formattedTime = lastSyncTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                    sql = sql.replace("{last_sync_time}", "'" + formattedTime + "'");
                    logBuilder.append("  - 增量模式（手动控制），增量字段: ").append(incrementalField)
                             .append(", 最后同步时间: ").append(formattedTime).append("\n");
                } else {
                    // 首次执行：移除增量条件，执行全量抽取
                    sql = sql.replaceAll("(?i)\\s+AND\\s+.*?\\{last_sync_time\\}.*?", "");
                    sql = sql.replaceAll("(?i)\\s+WHERE\\s+.*?\\{last_sync_time\\}.*?", "");
                    logBuilder.append("  - 增量模式: 首次同步，执行全量抽取\n");
                }
            } else {
                // 方式一：自动追加增量条件
                if (lastSyncTime != null) {
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
                    
                    logBuilder.append("  - 增量模式（自动追加），增量字段: ").append(incrementalField)
                             .append(", 最后同步时间: ").append(formattedTime).append("\n");
                } else {
                    logBuilder.append("  - 增量模式: 首次同步，执行全量抽取\n");
                }
            }
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
     * 数据转换（支持处理器链）
     */
    private List<Map<String, Object>> transformData(List<Map<String, Object>> sourceData,
                                                    List<FieldMapping> fieldMappings,
                                                    StringBuilder logBuilder) {
        List<Map<String, Object>> result = new ArrayList<>();
        GroovyShell shell = new GroovyShell();

        for (Map<String, Object> sourceRow : sourceData) {
            Map<String, Object> targetRow = new HashMap<>();
            boolean skipRow = false;

            for (FieldMapping mapping : fieldMappings) {
                Object value = sourceRow.get(mapping.getSourceField());
                
                // 新版：支持动态处理器链（用户可调整顺序）
                if (mapping.getProcessorChain() != null && !mapping.getProcessorChain().trim().isEmpty()) {
                    value = executeProcessorChain(value, mapping, shell, sourceRow);
                    if (value == null && "SKIP".equals(getSkipFlag(mapping))) {
                        skipRow = true;
                        break;
                    }
                } else {
                    // 兼容旧版：固定顺序执行
                    value = executeLegacyProcessing(value, mapping, shell, sourceRow);
                    if (value == null && "SKIP".equals(mapping.getNullStrategy())) {
                        skipRow = true;
                        break;
                    }
                }
                
                targetRow.put(mapping.getTargetField(), value);
            }
            
            if (!skipRow) {
                result.add(targetRow);
            }
        }

        logBuilder.append("  - 字段映射规则数: ").append(fieldMappings.size()).append("\n");
        return result;
    }

    private Object transformValue(Object value, FieldMapping mapping, GroovyShell shell, Map<String, Object> sourceRow) {
        if (value == null) {
            return mapping.getDefaultValue();
        }

        String transformType = mapping.getTransformType();

        switch (transformType) {
            case "DIRECT":
                return value;

            case "SCRIPT":
                if (StringUtils.hasText(mapping.getTransformScript())) {
                    // 使用缓存避免重复Groovy脚本编译（性能优化）
                    String scriptText = mapping.getTransformScript();
                    groovy.lang.Script script = scriptCache.computeIfAbsent(scriptText, s -> groovyShell.parse(s));
                    
                    Binding binding = new Binding();
                    binding.setVariable("value", value);
                    binding.setVariable("row", sourceRow);
                    script.setBinding(binding);
                    
                    return script.run();
                }
                return value;

            case "DICT":
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
                        return dictMap.getOrDefault(key, mapping.getDefaultValue());
                    } catch (Exception e) {
                        log.warn("查询字典失败, dictId={}", mapping.getDictMappingId(), e);
                        return mapping.getDefaultValue();
                    }
                }
                return mapping.getDefaultValue();

            case "CONSTANT":
                return mapping.getDefaultValue();

            default:
                return value;
        }
    }

    /**
     * 推送数据到API
     */
    private int pushDataToApi(ConnectorInfo targetConnector, List<Map<String, Object>> data,
                             TaskConfig taskConfig, StringBuilder logBuilder) throws Exception {

        JSONObject targetConfig = JSON.parseObject(taskConfig.getTargetConfig());
        String apiPath = targetConfig.getString("apiPath");
        String apiMethod = targetConfig.getString("apiMethod");
        String batchMode = targetConfig.getOrDefault("batchMode", "single").toString();

        logBuilder.append("  - 目标API: ").append(targetConnector.getUrl()).append(apiPath).append("\n");
        logBuilder.append("  - 请求方法: ").append(apiMethod).append("\n");
        logBuilder.append("  - 推送模式: ").append(batchMode).append("\n");

        int successCount = 0;

        if ("batch".equalsIgnoreCase(batchMode)) {
            // 批量推送
            JSONObject batchConfigJson = targetConfig.getJSONObject("batchConfig");
            JSONObject retryConfigJson = targetConfig.getJSONObject("retryConfig");
            
            BatchConfig batchConfig = buildBatchConfig(batchConfigJson, retryConfigJson);
            
            logBuilder.append("  - 批次大小: ").append(batchConfig.getBatchSize()).append("\n");
            logBuilder.append("  - 重试次数: ").append(batchConfig.getRetryTimes()).append("\n");
            
            BatchPushResult result;
            if ("POST".equalsIgnoreCase(apiMethod)) {
                result = apiConnector.batchPost(targetConnector, apiPath, data, batchConfig);
            } else {
                // PUT/PATCH也使用batchPost逻辑
                result = apiConnector.batchPost(targetConnector, apiPath, data, batchConfig);
            }
            
            logBuilder.append("  - 批量推送结果: 成功=").append(result.getSuccess())
                      .append(", 失败=").append(result.getFailed()).append("\n");
            
            if (!result.getErrors().isEmpty()) {
                logBuilder.append("  - 失败记录详情:\n");
                for (BatchPushResult.ErrorRecord error : result.getErrors()) {
                    logBuilder.append("    [")
                             .append(error.getIndex())
                             .append("] ")
                             .append(error.getErrorMessage())
                             .append("\n");
                }
            }
            
            successCount = result.getSuccess();
            
        } else {
            // 逐条推送
            JSONObject retryConfigJson = targetConfig.getJSONObject("retryConfig");
            int retryTimes = retryConfigJson != null ? retryConfigJson.getIntValue("retryTimes") : 3;
            long retryInterval = retryConfigJson != null ? retryConfigJson.getLongValue("retryInterval") : 1000L;
            
            logBuilder.append("  - 重试配置: ").append(retryTimes).append("次, 间隔").append(retryInterval).append("ms\n");
            
            for (Map<String, Object> row : data) {
                boolean success = false;
                Exception lastException = null;
                
                for (int retry = 0; retry <= retryTimes; retry++) {
                    try {
                        JSONObject response;
                        if ("POST".equalsIgnoreCase(apiMethod)) {
                            response = apiConnector.post(targetConnector, apiPath, row);
                        } else if ("PUT".equalsIgnoreCase(apiMethod)) {
                            response = apiConnector.put(targetConnector, apiPath, row);
                        } else if ("PATCH".equalsIgnoreCase(apiMethod)) {
                            response = apiConnector.patch(targetConnector, apiPath, row);
                        } else {
                            response = apiConnector.post(targetConnector, apiPath, row);
                        }
                        
                        successCount++;
                        success = true;
                        break;
                        
                    } catch (Exception e) {
                        lastException = e;
                        if (retry < retryTimes) {
                            try {
                                Thread.sleep(retryInterval);
                            } catch (InterruptedException ie) {
                                Thread.currentThread().interrupt();
                            }
                        }
                    }
                }
                
                if (!success && lastException != null) {
                    logBuilder.append("  - 推送失败(已重试")
                             .append(retryTimes)
                             .append("次): ")
                             .append(lastException.getMessage())
                             .append("\n");
                }
            }
        }

        return successCount;
    }
    
    /**
     * 构建BatchConfig
     */
    private BatchConfig buildBatchConfig(JSONObject batchConfigJson, JSONObject retryConfigJson) {
        BatchConfig config = new BatchConfig();
        
        if (batchConfigJson != null) {
            config.setBatchSize(batchConfigJson.getIntValue("batchSize"));
            config.setWrapperField(batchConfigJson.getString("wrapperField"));
        }
        
        if (retryConfigJson != null) {
            config.setRetryTimes(retryConfigJson.getIntValue("retryTimes"));
            config.setRetryInterval(retryConfigJson.getLongValue("retryInterval"));
        }
        
        return config;
    }
    
    /**
     * 执行处理器链（支持用户自定义顺序）
     */
    private Object executeProcessorChain(Object value, FieldMapping mapping, GroovyShell shell, Map<String, Object> sourceRow) {
        try {
            // 解析处理器链
            List<JSONObject> processorList = JSON.parseArray(mapping.getProcessorChain(), JSONObject.class);
            List<Map<String, Object>> processors = new ArrayList<>();
            for (JSONObject obj : processorList) {
                processors.add(obj.getInnerMap());
            }
            
            // 按order排序
            processors.sort((p1, p2) -> {
                Integer order1 = (Integer) p1.getOrDefault("order", 0);
                Integer order2 = (Integer) p2.getOrDefault("order", 0);
                return order1.compareTo(order2);
            });
            
            // 依次执行各个处理器
            for (Map<String, Object> processor : processors) {
                String type = (String) processor.get("type");
                Map<String, Object> config = (Map<String, Object>) processor.getOrDefault("config", new HashMap<>());
                
                switch (type) {
                    case "NULL_HANDLE":
                        // 空值处理
                        if (value == null || String.valueOf(value).trim().isEmpty()) {
                            String strategy = (String) config.getOrDefault("strategy", "KEEP");
                            String defaultValue = (String) config.get("defaultValue");
                            value = advancedTransformer.handleNullValue(value, strategy, defaultValue);
                            if (value == null && "SKIP".equalsIgnoreCase(strategy)) {
                                // SKIP策略：跳过后续所有处理器，直接返回null
                                return null;
                            }
                        }
                        break;
                        
                    case "CLEANSE":
                        // 数据清洗：只在value不为null时执行
                        if (value != null && mapping.getCleanseFunctions() != null && !mapping.getCleanseFunctions().trim().isEmpty()) {
                            value = advancedTransformer.applyCleanseFunctionChain(value, mapping.getCleanseFunctions());
                        }
                        break;
                        
                    case "TRANSFORM":
                        // 数据转换：只在value不为null时执行
                        if (value != null) {
                            value = transformValue(value, mapping, shell, sourceRow);
                        }
                        break;
                }
            }
            
            return value;
            
        } catch (Exception e) {
            log.error("执行处理器链失败: {}", e.getMessage(), e);
            return value;
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
     * 获取SKIP标记（从处理器链中解析）
     */
    private String getSkipFlag(FieldMapping mapping) {
        try {
            List<JSONObject> processorList = JSON.parseArray(mapping.getProcessorChain(), JSONObject.class);
            for (JSONObject processorObj : processorList) {
                Map<String, Object> processor = processorObj.getInnerMap();
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
}
