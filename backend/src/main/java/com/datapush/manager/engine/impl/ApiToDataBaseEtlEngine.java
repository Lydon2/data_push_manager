package com.datapush.manager.engine.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.datapush.manager.connector.config.PaginationConfig;
import com.datapush.manager.connector.impl.ApiConnector;
import com.datapush.manager.connector.impl.DatabaseConnector;
import com.datapush.manager.engine.EtlEngine;
import com.datapush.manager.entity.ConnectorInfo;
import com.datapush.manager.entity.FieldMapping;
import com.datapush.manager.entity.TaskConfig;
import com.datapush.manager.service.DictMappingService;
import com.datapush.manager.utils.TransformFunctionUtil;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.util.*;

/**
 * API To Database ETL引擎
 * 从REST API抽取JSON数据并写入数据库
 */
@Slf4j
@Component
public class ApiToDataBaseEtlEngine implements EtlEngine {

    @Autowired
    private ApiConnector apiConnector;

    @Autowired
    private DatabaseConnector databaseConnector;

    @Autowired
    private DictMappingService dictMappingService;
    
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
        logBuilder.append("========== API To Database ETL任务开始 ==========\n");
        logBuilder.append("任务名称: ").append(taskConfig.getTaskName()).append("\n");
        logBuilder.append("开始时间: ").append(LocalDateTime.now()).append("\n\n");

        Connection targetConn = null;

        try {
            // 1. 从API抽取数据
            logBuilder.append("[步骤1] 从API抽取数据...\n");
            JSONObject sourceConfig = JSON.parseObject(taskConfig.getSourceConfig());
            
            String apiPath = sourceConfig.getString("apiPath");
            String apiMethod = sourceConfig.getString("apiMethod");
            String dataPath = sourceConfig.getString("dataPath");
            
            // 解析请求参数
            Map<String, String> params = new HashMap<>();
            JSONObject queryParams = sourceConfig.getJSONObject("params");
            if (queryParams != null) {
                queryParams.forEach((k, v) -> params.put(k, String.valueOf(v)));
            }
            
            // 解析请求头
            Map<String, String> headers = new HashMap<>();
            JSONArray headersArray = sourceConfig.getJSONArray("headers");
            if (headersArray != null) {
                for (int i = 0; i < headersArray.size(); i++) {
                    JSONObject header = headersArray.getJSONObject(i);
                    if (header.getBooleanValue("enabled")) {
                        headers.put(header.getString("key"), header.getString("value"));
                    }
                }
            }
            
            // 合并headers到连接器配置
            if (!headers.isEmpty()) {
                JSONObject extraConfig = new JSONObject();
                if (StringUtils.hasText(sourceConnector.getExtraConfig())) {
                    try {
                        extraConfig = JSON.parseObject(sourceConnector.getExtraConfig());
                    } catch (Exception e) {
                        log.warn("解析extra_config失败，使用空配置", e);
                    }
                }
                
                JSONObject existingHeaders = extraConfig.getJSONObject("headers");
                if (existingHeaders == null) {
                    existingHeaders = new JSONObject();
                }
                existingHeaders.putAll(headers);
                extraConfig.put("headers", existingHeaders);
                
                sourceConnector.setExtraConfig(extraConfig.toJSONString());
                logBuilder.append("  - 使用自定义Headers: ").append(headers.size()).append(" 个\n");
            }

            List<Map<String, Object>> sourceData;
            
            // 检查是否启用分页
            JSONObject paginationConfig = sourceConfig.getJSONObject("pagination");
            if (paginationConfig != null && paginationConfig.getBoolean("enabled") == Boolean.TRUE) {
                // 使用分页获取
                logBuilder.append("  - 检测到分页配置，使用自动分页获取\n");
                PaginationConfig pagingConfig = buildPaginationConfig(paginationConfig);
                sourceData = apiConnector.fetchWithPagination(sourceConnector, apiPath, params, pagingConfig);
                logBuilder.append("  - 自动分页获取完成\n");
            } else {
                // 单次请求
                JSONObject apiResponse;
                
                // 解析Body参数
                String bodyType = sourceConfig.getString("bodyType");
                Object requestBody = null;
                
                if ("json".equals(bodyType) && sourceConfig.containsKey("jsonBody")) {
                    String jsonBody = sourceConfig.getString("jsonBody");
                    if (StringUtils.hasText(jsonBody)) {
                        requestBody = JSON.parse(jsonBody);
                    }
                } else if ("form-data".equals(bodyType) && sourceConfig.containsKey("formData")) {
                    JSONArray formDataArray = sourceConfig.getJSONArray("formData");
                    if (formDataArray != null && formDataArray.size() > 0) {
                        Map<String, String> formDataMap = new HashMap<>();
                        for (int i = 0; i < formDataArray.size(); i++) {
                            JSONObject formItem = formDataArray.getJSONObject(i);
                            if (formItem.getBooleanValue("enabled")) {
                                formDataMap.put(formItem.getString("key"), formItem.getString("value"));
                            }
                        }
                        requestBody = formDataMap;
                    }
                }
                
                // 根据方法调用API：params拼接到URL，requestBody作为请求体
                String urlWithParams = buildUrlWithParams(apiPath, params);
                
                if ("POST".equalsIgnoreCase(apiMethod)) {
                    apiResponse = apiConnector.post(sourceConnector, urlWithParams, requestBody);
                } else if ("PUT".equalsIgnoreCase(apiMethod)) {
                    apiResponse = apiConnector.put(sourceConnector, urlWithParams, requestBody);
                } else if ("PATCH".equalsIgnoreCase(apiMethod)) {
                    apiResponse = apiConnector.patch(sourceConnector, urlWithParams, requestBody);
                } else if ("DELETE".equalsIgnoreCase(apiMethod)) {
                    // DELETE不支持body，只用params
                    apiResponse = apiConnector.delete(sourceConnector, apiPath, params);
                } else {
                    // GET或其他
                    apiResponse = apiConnector.get(sourceConnector, apiPath, params);
                }
                logBuilder.append("  - API请求成功: ").append(sourceConnector.getHost()).append(":").append(sourceConnector.getPort()).append(apiPath).append("\n");
                sourceData = extractDataFromJson(apiResponse, dataPath, logBuilder);
            }
            
            logBuilder.append("  - 抽取记录数: ").append(sourceData.size()).append("\n\n");

            if (sourceData.isEmpty()) {
                logBuilder.append("API返回无数据，任务结束\n");
                return EtlResult.success(0, 0, logBuilder.toString());
            }

            // 2. 数据转换
            logBuilder.append("[步骤2] 数据转换...\n");
            List<Map<String, Object>> transformedData = transformData(sourceData, fieldMappings, logBuilder);
            logBuilder.append("  - 转换完成，记录数: ").append(transformedData.size()).append("\n\n");

            // 3. 加载到数据库
            logBuilder.append("[步骤3] 加载数据到数据库...\n");
            targetConn = databaseConnector.getConnection(targetConnector);
            int successCount = loadDataToDatabase(targetConn, transformedData, taskConfig, fieldMappings, logBuilder);
            logBuilder.append("  - 成功加载记录数: ").append(successCount).append("\n\n");

            logBuilder.append("========== ETL任务执行成功 ==========\n");
            logBuilder.append("结束时间: ").append(LocalDateTime.now()).append("\n");

            return EtlResult.success(sourceData.size(), successCount, logBuilder.toString());

        } catch (Exception e) {
            log.error("API To Database ETL任务执行失败", e);
            logBuilder.append("\n========== ETL任务执行失败 ==========\n");
            logBuilder.append("错误信息: ").append(e.getMessage()).append("\n");
            return EtlResult.failed(e.getMessage(), logBuilder.toString());

        } finally {
            if (targetConn != null) {
                try {
                    targetConn.close();
                } catch (Exception e) {
                    log.warn("关闭数据库连接失败", e);
                }
            }
        }
    }

    /**
     * 从JSON响应中提取数据
     */
    private List<Map<String, Object>> extractDataFromJson(JSONObject apiResponse, String dataPath, StringBuilder logBuilder) {
        List<Map<String, Object>> result = new ArrayList<>();

        try {
            Object data = apiResponse;
            
            // 按路径解析，如: data.list
            if (StringUtils.hasText(dataPath)) {
                String[] paths = dataPath.split("\\.");
                for (String path : paths) {
                    if (data instanceof JSONObject) {
                        data = ((JSONObject) data).get(path);
                    } else {
                        throw new RuntimeException("数据路径解析失败: " + path);
                    }
                }
            }

            // 转换为List
            if (data instanceof JSONArray) {
                JSONArray array = (JSONArray) data;
                for (int i = 0; i < array.size(); i++) {
                    Object item = array.get(i);
                    if (item instanceof JSONObject) {
                        Map<String, Object> row = new HashMap<>(((JSONObject) item).getInnerMap());
                        result.add(row);
                    }
                }
            } else if (data instanceof JSONObject) {
                // 单个对象也当作一条记录
                Map<String, Object> row = new HashMap<>(((JSONObject) data).getInnerMap());
                result.add(row);
            } else {
                throw new RuntimeException("不支持的数据格式，期望JSONArray或JSONObject");
            }

        } catch (Exception e) {
            logBuilder.append("  - 解析JSON数据失败: ").append(e.getMessage()).append("\n");
            throw new RuntimeException("解析JSON数据失败", e);
        }

        return result;
    }

    /**
     * 构建PaginationConfig
     */
    private PaginationConfig buildPaginationConfig(JSONObject paginationJson) {
        PaginationConfig config = new PaginationConfig();
        config.setPageParamName(paginationJson.getString("pageParam"));
        config.setPageSizeParamName(paginationJson.getString("pageSizeParam"));
        config.setStartPage(paginationJson.getInteger("startPage"));
        config.setPageSize(paginationJson.getInteger("pageSize"));
        config.setTotalPath(paginationJson.getString("totalPath"));
        config.setMaxPages(100); // 默认最多100页
        return config;
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

            case "FUNCTION":
                if (StringUtils.hasText(mapping.getTransformFunction())) {
                    return TransformFunctionUtil.applyFunction(value, mapping.getTransformFunction());
                }
                return value;

            case "CONSTANT":
                return mapping.getDefaultValue();

            default:
                return value;
        }
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

    /**
     * 加载数据到数据库
     */
    private int loadDataToDatabase(Connection conn, List<Map<String, Object>> data,
                                   TaskConfig taskConfig, List<FieldMapping> fieldMappings,
                                   StringBuilder logBuilder) throws Exception {

        JSONObject targetConfig = JSON.parseObject(taskConfig.getTargetConfig());
        String targetTable = targetConfig.getString("tableName");
        String writeMode = targetConfig.getOrDefault("writeMode", "INSERT").toString();

        logBuilder.append("  - 目标表: ").append(targetTable).append("\n");
        logBuilder.append("  - 写入模式: ").append(writeMode).append("\n");

        List<String> targetFields = new ArrayList<>();
        for (FieldMapping mapping : fieldMappings) {
            targetFields.add(mapping.getTargetField());
        }

        StringBuilder sql = new StringBuilder("INSERT INTO ").append(targetTable).append(" (");
        sql.append(String.join(", ", targetFields));
        sql.append(") VALUES (");
        sql.append(String.join(", ", Collections.nCopies(targetFields.size(), "?")));
        sql.append(")");

        logBuilder.append("  - INSERT SQL: ").append(sql).append("\n");

        int successCount = 0;
        try (PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            for (Map<String, Object> row : data) {
                int index = 1;
                for (String field : targetFields) {
                    pstmt.setObject(index++, row.get(field));
                }
                pstmt.executeUpdate();
                successCount++;
            }
        }

        return successCount;
    }
    
    /**
     * 构建带参数的URL（将params拼接到URL上）
     */
    private String buildUrlWithParams(String apiPath, Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return apiPath;
        }
        
        StringBuilder url = new StringBuilder(apiPath);
        boolean first = !apiPath.contains("?");
        
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (first) {
                url.append("?");
                first = false;
            } else {
                url.append("&");
            }
            url.append(entry.getKey()).append("=").append(entry.getValue());
        }
        
        return url.toString();
    }
}
