package com.datapush.manager.transformer;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 高级数据转换处理器
 * 支持条件转换、聚合转换、数据清洗等高级功能
 */
@Slf4j
@Component
public class AdvancedTransformer {
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    @Autowired
    private CleanseFunctionRegistry functionRegistry;
    
    /**
     * 函数链缓存（性能优化）
     * Key: cleanseFunctions JSON字符串
     * Value: 解析后的函数链配置列表
     */
    private final Map<String, List<FunctionConfig>> functionChainCache = new ConcurrentHashMap<>();
    
    /**
     * 函数配置类（内部使用）
     */
    private static class FunctionConfig {
        String functionCode;
        Map<String, Object> params;
        
        FunctionConfig(String functionCode, Map<String, Object> params) {
            this.functionCode = functionCode;
            this.params = params;
        }
    }
    
    /**
     * 空值处理
     */
    public Object handleNullValue(Object value, String nullStrategy, String defaultValue) {
        if (value != null && !String.valueOf(value).trim().isEmpty()) {
            return value;
        }
        
        if (nullStrategy == null) {
            nullStrategy = "KEEP";
        }
        
        // 处理内置占位符默认值
        if (defaultValue != null) {
            String trimmed = defaultValue.trim();
            if ("{CURRENT_DATE}".equalsIgnoreCase(trimmed)) {
                // 当前日期: yyyy-MM-dd
                defaultValue = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            } else if ("{CURRENT_DATETIME}".equalsIgnoreCase(trimmed)) {
                // 当前时间: yyyy-MM-dd HH:mm:ss
                defaultValue = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            }
        }
        
        switch (nullStrategy.toUpperCase()) {
            case "KEEP":
                return value;
            case "DEFAULT":
                return defaultValue != null ? defaultValue : "";
            case "SKIP":
                return null; // 标记为跳过
            case "REMOVE":
                throw new RuntimeException("REMOVE_ROW"); // 抛出异常标记移除整行
            default:
                return value;
        }
    }
    
    /**
     * 执行清洗函数链
     * 按顺序执行多个清洗函数，前一个函数的输出是后一个函数的输入
     * 
     * @param value 原始值
     * @param cleanseFunctionsJson 清洗函数链配置 JSON
     * @return 清洗后的值
     */
    public Object applyCleanseFunctionChain(Object value, String cleanseFunctionsJson) {
        if (cleanseFunctionsJson == null || cleanseFunctionsJson.trim().isEmpty()) {
            return value;
        }
        
        try {
            // 使用缓存避免重复JSON解析（性能优化）
            List<FunctionConfig> functionConfigs = functionChainCache.computeIfAbsent(cleanseFunctionsJson, json -> {
                List<FunctionConfig> configs = new ArrayList<>();
                JSONArray functionArray = JSON.parseArray(json);
                
                for (int i = 0; i < functionArray.size(); i++) {
                    JSONObject funcConfig = functionArray.getJSONObject(i);
                    String functionCode = funcConfig.getString("functionCode");
                    JSONObject params = funcConfig.getJSONObject("params");
                    
                    // 转换为 Map
                    Map<String, Object> paramMap = new HashMap<>();
                    if (params != null) {
                        for (String key : params.keySet()) {
                            paramMap.put(key, params.get(key));
                        }
                    }
                    
                    configs.add(new FunctionConfig(functionCode, paramMap));
                }
                
                return configs;
            });
            
            Object currentValue = value;
            
            // 按顺序执行每个函数
            for (FunctionConfig config : functionConfigs) {
                currentValue = functionRegistry.executeFunction(config.functionCode, currentValue, config.params);
            }
            
            return currentValue;
            
        } catch (Exception e) {
            log.error("执行清洗函数链失败: {}", e.getMessage(), e);
            return value;
        }
    }
}
