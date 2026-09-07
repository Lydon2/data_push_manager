package com.datapush.manager.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.datapush.manager.common.Result;
import com.datapush.manager.entity.FieldMapping;
import com.datapush.manager.service.DictMappingService;
import com.datapush.manager.transformer.AdvancedTransformer;
import com.datapush.manager.transformer.CleanseFunction;
import com.datapush.manager.transformer.CleanseFunctionRegistry;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 数据转换函数API
 */
@Slf4j
@RestController
@RequestMapping("/v1/transform")
public class TransformController {
    
    @Autowired
    private CleanseFunctionRegistry functionRegistry;
    
    @Autowired
    private AdvancedTransformer advancedTransformer;
    
    @Autowired
    private DictMappingService dictMappingService;
    
    /**
     * 获取所有清洗函数列表
     */
    @GetMapping("/cleanse/functions")
    public Result<List<CleanseFunction>> getAllCleanseFunctions() {
        return Result.success(functionRegistry.getAllFunctions());
    }
    
    /**
     * 按分类获取清洗函数
     */
    @GetMapping("/cleanse/functions/by-category")
    public Result<Map<String, List<CleanseFunction>>> getCleanseFunctionsByCategory() {
        return Result.success(functionRegistry.getFunctionsByCategory());
    }
    
    /**
     * 获取指定函数详情
     */
    @GetMapping("/cleanse/functions/{functionCode}")
    public Result<CleanseFunction> getCleanseFunctionDetail(@PathVariable String functionCode) {
        return Result.success(functionRegistry.getFunction(functionCode));
    }
    
    /**
     * 预览函数执行结果
     */
    @PostMapping("/cleanse/preview")
    public Result<Map<String, Object>> previewCleanseFunction(@RequestBody Map<String, Object> request) {
        String functionCode = (String) request.get("functionCode");
        Object value = request.get("value");
        @SuppressWarnings("unchecked")
        Map<String, Object> params = (Map<String, Object>) request.getOrDefault("params", new java.util.HashMap<>());
        
        try {
            Object result = functionRegistry.executeFunction(functionCode, value, params);
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("success", true);
            response.put("originalValue", value != null ? value : "NULL");
            response.put("result", result != null ? result : "NULL");
            return Result.success(response);
        } catch (Exception e) {
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return Result.success(response);
        }
    }
    
    /**
     * 预览脚本执行结果
     */
    @PostMapping("/script/preview")
    public Result<Map<String, Object>> previewScript(@RequestBody Map<String, Object> request) {
        String script = (String) request.get("script");
        Object value = request.get("value");
        @SuppressWarnings("unchecked")
        Map<String, Object> row = (Map<String, Object>) request.getOrDefault("row", new HashMap<>());
        
        if (script == null || script.trim().isEmpty()) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", "脚本代码不能为空");
            return Result.success(response);
        }
        
        try {
            log.debug("脚本预览 - value: {}, row: {}, script: {}", value, row, script);
            
            // 创建 Groovy 绑定
            Binding binding = new Binding();
            binding.setVariable("value", value);
            binding.setVariable("row", row);
            binding.setVariable("ctx", new HashMap<>()); // 空的上下文
            
            // 执行脚本
            GroovyShell shell = new GroovyShell(binding);
            Object result = shell.evaluate(script);
            
            log.debug("脚本执行结果: {}", result);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("result", result != null ? result : "NULL");
            return Result.success(response);
            
        } catch (Exception e) {
            log.error("脚本执行失败: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return Result.success(response);
        }
    }
    
    /**
     * 转换预览
     */
    @PostMapping("/preview")
    public Result<Map<String, Object>> previewTransform(@RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> sourceData = (List<Map<String, Object>>) request.get("sourceData");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> mappingsData = (List<Map<String, Object>>) request.get("mappings");
            
            if (sourceData == null || sourceData.isEmpty()) {
                return Result.error("源数据不能为空");
            }
            
            if (mappingsData == null || mappingsData.isEmpty()) {
                return Result.error("字段映射不能为空");
            }
            
            // 转换数据
            List<Map<String, Object>> transformedData = new ArrayList<>();
            int skippedCount = 0;
            
            for (Map<String, Object> sourceRow : sourceData) {
                // 使用 LinkedHashMap 保持字段顺序
                Map<String, Object> targetRow = new LinkedHashMap<>();
                boolean skipRow = false;
                
                for (Map<String, Object> mappingData : mappingsData) {
                    String sourceField = (String) mappingData.get("sourceField");
                    String targetField = (String) mappingData.get("targetField");
                    
                    Object value = sourceRow.get(sourceField);
                    Object transformedValue = value;
                    
                    // 检查是否有处理器链
                    String processorChainJson = (String) mappingData.get("processorChain");
                    
                    if (processorChainJson != null && !processorChainJson.trim().isEmpty()) {
                        // 有处理器链，执行处理器链
                        transformedValue = executeProcessorChain(value, processorChainJson, mappingData, sourceRow);
                        // 检查SKIP策略
                        if (transformedValue == null && isSkipStrategy(processorChainJson)) {
                            skipRow = true;
                            break; // 跳过整行数据
                        }
                    } else {
                        // 旧版模式：直接根据转换类型处理
                        transformedValue = executeLegacyTransform(value, mappingData, sourceRow);
                        // 检查旧版SKIP策略
                        String nullStrategy = (String) mappingData.get("nullStrategy");
                        if (transformedValue == null && "SKIP".equalsIgnoreCase(nullStrategy)) {
                            skipRow = true;
                            break; // 跳过整行数据
                        }
                    }
                    
                    targetRow.put(targetField, transformedValue);
                }
                
                if (!skipRow) {
                    transformedData.add(targetRow);
                } else {
                    skippedCount++;
                }
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("transformedData", transformedData);
            
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("转换预览失败: " + e.getMessage());
        }
    }
    
    /**
     * 检查处理器链中是否配置了SKIP策略
     */
    private boolean isSkipStrategy(String processorChainJson) {
        try {
            JSONArray processorArray = JSON.parseArray(processorChainJson);
            for (int i = 0; i < processorArray.size(); i++) {
                JSONObject processor = processorArray.getJSONObject(i);
                if ("NULL_HANDLE".equals(processor.getString("type"))) {
                    JSONObject config = processor.getJSONObject("config");
                    if (config != null && "SKIP".equalsIgnoreCase(config.getString("strategy"))) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            // 忽略解析错误
        }
        return false;
    }
    
    /**
     * 执行处理器链
     */
    private Object executeProcessorChain(Object value, String processorChainJson, 
                                          Map<String, Object> mappingData, Map<String, Object> sourceRow) {
        try {
            JSONArray processorArray = JSON.parseArray(processorChainJson);
            
            // 按order排序
            List<JSONObject> processors = new ArrayList<>();
            for (int i = 0; i < processorArray.size(); i++) {
                processors.add(processorArray.getJSONObject(i));
            }
            processors.sort((p1, p2) -> {
                Integer order1 = p1.getInteger("order");
                Integer order2 = p2.getInteger("order");
                if (order1 == null) order1 = 0;
                if (order2 == null) order2 = 0;
                return order1.compareTo(order2);
            });
            
            Object currentValue = value;
            
            // 按顺序执行每个处理器
            for (JSONObject processor : processors) {
                String type = processor.getString("type");
                JSONObject config = processor.getJSONObject("config");
                
                if ("NULL_HANDLE".equals(type)) {
                    // 空值处理
                    String strategy = config != null ? config.getString("strategy") : "KEEP";
                    String defaultValue = config != null ? config.getString("defaultValue") : null;
                    
                    // 调试日志：帮助排查空值处理是否触发
                    log.debug("预览-空值处理: value={}, isEmpty={}, strategy={}, defaultValue={}", 
                             currentValue, 
                             currentValue == null || String.valueOf(currentValue).trim().isEmpty(),
                             strategy, 
                             defaultValue);
                    
                    currentValue = advancedTransformer.handleNullValue(currentValue, strategy, defaultValue);
                    
                    log.debug("预览-空值处理后: currentValue={}", currentValue);
                    
                    // SKIP策略：跳过后续所有处理器
                    if (currentValue == null && "SKIP".equalsIgnoreCase(strategy)) {
                        return null;
                    }
                    
                } else if ("CLEANSE".equals(type)) {
                    // 数据清洗：只在currentValue不为null时执行
                    if (currentValue != null) {
                        String cleanseFunctions = (String) mappingData.get("cleanseFunctions");
                        // 优先使用处理器config中的清洗函数配置（兼容新旧存储）
                        if ((cleanseFunctions == null || cleanseFunctions.trim().isEmpty()) && config != null) {
                            String cfgStr = config.getString("cleanseFunctions");
                            if (cfgStr != null && !cfgStr.trim().isEmpty()) {
                                cleanseFunctions = cfgStr;
                            } else {
                                com.alibaba.fastjson.JSONArray funcArr = config.getJSONArray("functions");
                                if (funcArr != null && !funcArr.isEmpty()) {
                                    cleanseFunctions = funcArr.toJSONString();
                                }
                            }
                        }
                        if (cleanseFunctions != null && !cleanseFunctions.trim().isEmpty()) {
                            currentValue = advancedTransformer.applyCleanseFunctionChain(currentValue, cleanseFunctions);
                        }
                    }
                    
                } else if ("TRANSFORM".equals(type)) {
                    // 数据转换：只在currentValue不为null时执行（除非是CONSTANT类型）
                    // 从 processor.config 中获取转换配置
                    String transformType = config != null && config.getString("transformType") != null
                        ? config.getString("transformType")
                        : (String) mappingData.getOrDefault("transformType", "DIRECT");
                    
                    // CONSTANT类型不依赖源值，直接返回常量
                    if ("CONSTANT".equals(transformType)) {
                        currentValue = config != null ? config.get("constantValue") : mappingData.get("constantValue");
                    } else if (currentValue != null) {
                        // 其他转换类型：仅当currentValue不为null时执行
                        // 构造一个临时mappingData，包含 processor.config 中的所有转换配置
                        Map<String, Object> tempMappingData = new HashMap<>(mappingData);
                        tempMappingData.put("transformType", transformType);
                        
                        // 从 config 中读取其他转换参数（优先使用 config 中的配置）
                        if (config != null) {
                            if (config.getString("constantValue") != null) {
                                tempMappingData.put("constantValue", config.getString("constantValue"));
                            }
                            if (config.getString("defaultValue") != null) {
                                tempMappingData.put("defaultValue", config.getString("defaultValue"));
                            }
                            if (config.get("dictMappingId") != null) {
                                tempMappingData.put("dictMappingId", config.get("dictMappingId"));
                            }
                            if (config.getString("dictSourceTypeValue") != null) {
                                tempMappingData.put("dictSourceTypeValue", config.getString("dictSourceTypeValue"));
                            }
                            if (config.getString("dictOutputMode") != null) {
                                tempMappingData.put("dictOutputMode", config.getString("dictOutputMode"));
                            }
                            if (config.getString("transformScript") != null) {
                                tempMappingData.put("transformScript", config.getString("transformScript"));
                            }
                        }
                        
                        currentValue = executeLegacyTransform(currentValue, tempMappingData, sourceRow);
                    }
                }
            }
            
            return currentValue;
            
        } catch (Exception e) {
            log.error("执行处理器链失败: {}", e.getMessage(), e);
            return value;
        }
    }
    
    /**
     * 执行旧版转换（兼容模式）
     */
    private Object executeLegacyTransform(Object value, Map<String, Object> mappingData, Map<String, Object> sourceRow) {
        String transformType = (String) mappingData.getOrDefault("transformType", "DIRECT");
        
        switch (transformType) {
            case "DIRECT":
                return value;
                
            case "SCRIPT":
                String script = (String) mappingData.get("transformScript");
                if (script != null && !script.trim().isEmpty()) {
                    try {
                        log.debug("预览执行脚本 - value: {}, row字段: {}, script: {}", value, sourceRow != null ? sourceRow.keySet() : null, script);
                        
                        Binding binding = new Binding();
                        binding.setVariable("value", value);
                        binding.setVariable("row", sourceRow);
                        
                        GroovyShell scriptShell = new GroovyShell(binding);
                        Object result = scriptShell.evaluate(script);
                        
                        log.debug("脚本执行结果: {}", result);
                        return result;
                    } catch (Exception e) {
                        log.error("脚本执行失败 - script: {}, value: {}, row: {}, error: {}", script, value, sourceRow, e.getMessage(), e);
                        return "[ERROR: " + e.getMessage() + "]";
                    }
                }
                return value;
                
            case "CONSTANT":
                return mappingData.get("constantValue");
                
            case "DICT":
                Long dictMappingId = mappingData.get("dictMappingId") != null 
                    ? Long.valueOf(mappingData.get("dictMappingId").toString()) 
                    : null;
                
                if (dictMappingId != null && value != null) {
                    String sourceTypeValue = (String) mappingData.get("dictSourceTypeValue");
                    String outputMode = (String) mappingData.getOrDefault("dictOutputMode", "TARGET_KEY");
                    
                    try {
                        Map<String, String> dictCache = dictMappingService.loadMappingCache(
                            dictMappingId, 
                            sourceTypeValue, 
                            outputMode
                        );
                        
                        String sourceKey = String.valueOf(value);
                        String defaultVal = mappingData.get("defaultValue") != null 
                            ? mappingData.get("defaultValue").toString() 
                            : String.valueOf(value);
                        
                        return dictCache.getOrDefault(sourceKey, defaultVal);
                    } catch (Exception e) {
                        log.error("字典映射失败: {}", e.getMessage());
                        return value;
                    }
                }
                return value;
                
            default:
                return value;
        }
    }
}


