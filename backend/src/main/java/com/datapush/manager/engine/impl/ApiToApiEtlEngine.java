package com.datapush.manager.engine.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONArray;
import com.datapush.manager.connector.impl.ApiConnector;
import com.datapush.manager.engine.EtlEngine;
import com.datapush.manager.entity.ConnectorInfo;
import com.datapush.manager.entity.FieldMapping;
import com.datapush.manager.entity.TaskConfig;
import com.datapush.manager.service.DictMappingService;
import com.datapush.manager.transformer.AdvancedTransformer;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import com.datapush.manager.utils.TransformFunctionUtil;

/**
 * API到API的ETL引擎实现
 * 从源API获取数据，转换后推送到目标API
 */
@Slf4j
@Component
public class ApiToApiEtlEngine implements EtlEngine {

    @Autowired
    private ApiConnector apiConnector;
    
    @Autowired
    private DictMappingService dictMappingService;
    
    @Autowired
    private AdvancedTransformer advancedTransformer;
    
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
        logBuilder.append("========== API->API ETL任务开始执行 ==========\n");
        logBuilder.append("任务名称: ").append(taskConfig.getTaskName()).append("\n");
        logBuilder.append("开始时间: ").append(LocalDateTime.now()).append("\n\n");

        try {
            // 1. 从源API抽取数据
            logBuilder.append("[步骤1] 从源API抽取数据...\n");
            List<Map<String, Object>> sourceData = extractFromApi(taskConfig, sourceConnector, logBuilder);
            logBuilder.append("  - 抽取记录数: ").append(sourceData.size()).append("\n\n");

            if (sourceData.isEmpty()) {
                logBuilder.append("源API无数据，任务结束\n");
                return EtlResult.success(0, 0, logBuilder.toString());
            }

            // 2. 数据转换
            logBuilder.append("[步骤2] 数据转换...\n");
            List<Map<String, Object>> transformedData = transformData(sourceData, fieldMappings, logBuilder);
            logBuilder.append("  - 转换完成，记录数: ").append(transformedData.size()).append("\n\n");

            // 3. 推送到目标API
            logBuilder.append("[步骤3] 推送数据到目标API...\n");
            int successCount = pushToApi(transformedData, taskConfig, targetConnector, fieldMappings, logBuilder);
            logBuilder.append("  - 成功推送记录数: ").append(successCount).append("\n\n");

            logBuilder.append("========== ETL任务执行成功 ==========\n");
            logBuilder.append("结束时间: ").append(LocalDateTime.now()).append("\n");

            return EtlResult.success(sourceData.size(), successCount, logBuilder.toString());

        } catch (Exception e) {
            log.error("API->API ETL任务执行失败", e);
            logBuilder.append("\n========== ETL任务执行失败 ==========\n");
            logBuilder.append("错误信息: ").append(e.getMessage()).append("\n");
            return EtlResult.failed(e.getMessage(), logBuilder.toString());
        }
    }

    /**
     * 从源API抽取数据
     */
    private List<Map<String, Object>> extractFromApi(TaskConfig taskConfig,
                                                     ConnectorInfo sourceConnector,
                                                     StringBuilder logBuilder) throws Exception {
        
        JSONObject sourceConfig = JSON.parseObject(taskConfig.getSourceConfig());
        
        String apiPath = sourceConfig.getString("apiPath");
        String apiMethod = sourceConfig.getString("apiMethod");
        String dataPath = sourceConfig.getString("dataPath"); // JSON数据路径
        
        logBuilder.append("  - 源API: ").append(apiPath).append("\n");
        logBuilder.append("  - 请求方法: ").append(apiMethod).append("\n");
        
        // 构建请求参数
        Map<String, String> params = new HashMap<>();
        if (sourceConfig.containsKey("apiParams")) {
            JSONArray apiParams = sourceConfig.getJSONArray("apiParams");
            if (apiParams != null) {
                for (int i = 0; i < apiParams.size(); i++) {
                    JSONObject param = apiParams.getJSONObject(i);
                    if (param.getBooleanValue("enabled")) {
                        params.put(param.getString("key"), param.getString("value"));
                    }
                }
            }
        }
        
        // 解析请求头
        Map<String, String> headers = new HashMap<>();
        if (sourceConfig.containsKey("headers")) {
            JSONArray headersArray = sourceConfig.getJSONArray("headers");
            if (headersArray != null) {
                for (int i = 0; i < headersArray.size(); i++) {
                    JSONObject header = headersArray.getJSONObject(i);
                    if (header.getBooleanValue("enabled")) {
                        headers.put(header.getString("key"), header.getString("value"));
                    }
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
        
        // 根据请求方法调用不同的API
        JSONObject response;
        
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
        
        // 根据HTTP方法调用：params拼接到URL，requestBody作为请求体
        String urlWithParams = buildUrlWithParams(apiPath, params);
        
        if ("GET".equalsIgnoreCase(apiMethod)) {
            response = apiConnector.get(sourceConnector, apiPath, params);
        } else if ("POST".equalsIgnoreCase(apiMethod)) {
            response = apiConnector.post(sourceConnector, urlWithParams, requestBody);
        } else if ("PUT".equalsIgnoreCase(apiMethod)) {
            response = apiConnector.put(sourceConnector, urlWithParams, requestBody);
        } else if ("PATCH".equalsIgnoreCase(apiMethod)) {
            response = apiConnector.patch(sourceConnector, urlWithParams, requestBody);
        } else if ("DELETE".equalsIgnoreCase(apiMethod)) {
            response = apiConnector.delete(sourceConnector, apiPath, params);
        } else {
            throw new RuntimeException("不支持的HTTP方法: " + apiMethod);
        }
        
        // 从响应中提取数据
        return extractDataFromResponse(response, dataPath);
    }

    /**
     * 数据转换
     */
    private List<Map<String, Object>> transformData(List<Map<String, Object>> sourceData,
                                                    List<FieldMapping> fieldMappings,
                                                    StringBuilder logBuilder) {
        
        List<Map<String, Object>> result = new ArrayList<>();
        GroovyShell shell = new GroovyShell();
        int filteredCount = 0;
        
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
        if (filteredCount > 0) {
            logBuilder.append("  - 过滤掉异常数据: ").append(filteredCount).append(" 条\n");
        }
        
        return result;
    }

    /**
     * 字段值转换
     */
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
                return mapping.getDefaultValue();
                
            case "CONSTANT":
                return mapping.getDefaultValue() != null ? mapping.getDefaultValue() : "";
                
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
     * 推送数据到目标API
     */
    private int pushToApi(List<Map<String, Object>> transformedData,
                         TaskConfig taskConfig,
                         ConnectorInfo targetConnector,
                         List<FieldMapping> fieldMappings,
                         StringBuilder logBuilder) throws Exception {
        
        JSONObject targetConfig = JSON.parseObject(taskConfig.getTargetConfig());
        
        String apiPath = targetConfig.getString("apiPath");
        String apiMethod = targetConfig.getString("apiMethod");
        String batchMode = targetConfig.getString("batchMode");
        
        logBuilder.append("  - 目标API: ").append(apiPath).append("\n");
        logBuilder.append("  - 请求方法: ").append(apiMethod).append("\n");
        logBuilder.append("  - 批量模式: ").append(batchMode).append("\n");
        
        int successCount = 0;
        
        if ("batch".equals(batchMode)) {
            // 批量推送
            int batchSize = targetConfig.getIntValue("batchSize");
            if (batchSize <= 0) {
                batchSize = 100; // 默认100条
            }
            
            String wrapperField = targetConfig.getString("wrapperField");
            
            for (int i = 0; i < transformedData.size(); i += batchSize) {
                int end = Math.min(i + batchSize, transformedData.size());
                List<Map<String, Object>> batch = transformedData.subList(i, end);
                
                // 构建批量请求体
                Object requestBody;
                if (StringUtils.hasText(wrapperField)) {
                    Map<String, Object> wrapper = new HashMap<>();
                    wrapper.put(wrapperField, batch);
                    requestBody = wrapper;
                } else {
                    requestBody = batch;
                }
                
                try {
                    // 根据方法调用对应的API
                    if ("POST".equalsIgnoreCase(apiMethod)) {
                        apiConnector.post(targetConnector, apiPath, requestBody);
                    } else if ("PUT".equalsIgnoreCase(apiMethod)) {
                        apiConnector.put(targetConnector, apiPath, requestBody);
                    } else if ("PATCH".equalsIgnoreCase(apiMethod)) {
                        apiConnector.patch(targetConnector, apiPath, requestBody);
                    } else {
                        throw new RuntimeException("不支持的HTTP方法: " + apiMethod);
                    }
                    
                    successCount += batch.size();
                    logBuilder.append("  - 批次推送成功: ").append(batch.size()).append(" 条\n");
                } catch (Exception e) {
                    log.error("批量推送失败", e);
                    logBuilder.append("  - 批次推送失败: ").append(e.getMessage()).append("\n");
                }
            }
        } else {
            // 单条推送
            for (Map<String, Object> row : transformedData) {
                try {
                    // 根据方法调用对应的API
                    if ("POST".equalsIgnoreCase(apiMethod)) {
                        apiConnector.post(targetConnector, apiPath, row);
                    } else if ("PUT".equalsIgnoreCase(apiMethod)) {
                        apiConnector.put(targetConnector, apiPath, row);
                    } else if ("PATCH".equalsIgnoreCase(apiMethod)) {
                        apiConnector.patch(targetConnector, apiPath, row);
                    } else {
                        throw new RuntimeException("不支持的HTTP方法: " + apiMethod);
                    }
                    
                    successCount++;
                } catch (Exception e) {
                    log.error("单条推送失败", e);
                    logBuilder.append("  - 单条推送失败: ").append(e.getMessage()).append("\n");
                }
            }
        }
        
        return successCount;
    }
    
    /**
     * 从 API 响应中提取数据
     */
    private List<Map<String, Object>> extractDataFromResponse(JSONObject response, String dataPath) {
        List<Map<String, Object>> result = new ArrayList<>();
        
        if (response == null) {
            return result;
        }
        
        Object data = response;
        
        // 如果指定了数据路径，按路径提取
        if (StringUtils.hasText(dataPath)) {
            String[] paths = dataPath.split("\\.");
            for (String path : paths) {
                if (data instanceof JSONObject) {
                    data = ((JSONObject) data).get(path);
                } else {
                    break;
                }
            }
        }
        
        // 转换为列表
        if (data instanceof com.alibaba.fastjson.JSONArray) {
            com.alibaba.fastjson.JSONArray array = (com.alibaba.fastjson.JSONArray) data;
            for (int i = 0; i < array.size(); i++) {
                Object item = array.get(i);
                if (item instanceof JSONObject) {
                    result.add(new HashMap<>(((JSONObject) item).getInnerMap()));
                }
            }
        } else if (data instanceof JSONObject) {
            result.add(new HashMap<>(((JSONObject) data).getInnerMap()));
        }
        
        return result;
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
