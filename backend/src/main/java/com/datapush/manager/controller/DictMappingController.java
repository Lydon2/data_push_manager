package com.datapush.manager.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.datapush.manager.common.Result;
import com.datapush.manager.entity.DictItemMapping;
import com.datapush.manager.entity.DictMapping;
import com.datapush.manager.service.DictMappingService;
import com.datapush.manager.service.DictSourceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 字典映射关系控制器
 * 
 * @author Data Push Team
 * @since 2024-11-21
 */
@Slf4j
@RestController
@RequestMapping("/v1/dict-mapping")
public class DictMappingController {
    
    @Autowired
    private DictMappingService dictMappingService;
    
    @Autowired
    private DictSourceService dictSourceService;
    
    /**
     * 分页查询字典映射
     */
    @GetMapping("/page")
    public Result<IPage<DictMapping>> page(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String mappingName) {
        IPage<DictMapping> page = dictMappingService.pageList(current, size, mappingName);
        return Result.success(page);
    }
    
    /**
     * 获取所有字典映射（下拉选择）
     */
    @GetMapping("/list")
    public Result<List<DictMapping>> list() {
        List<DictMapping> list = dictMappingService.listAll();
        return Result.success(list);
    }
    
    /**
     * 根据ID获取字典映射
     */
    @GetMapping("/{id}")
    public Result<DictMapping> getById(@PathVariable Long id) {
        DictMapping dictMapping = dictMappingService.getById(id);
        return Result.success(dictMapping);
    }
    
    /**
     * 新增字典映射
     */
    @PostMapping
    public Result<DictMapping> save(@RequestBody DictMapping dictMapping) {
        dictMappingService.save(dictMapping);
        return Result.success(dictMapping);
    }
    
    /**
     * 更新字典映射
     */
    @PutMapping
    public Result<Void> update(@RequestBody DictMapping dictMapping) {
        dictMappingService.updateById(dictMapping);
        return Result.success(null);
    }
    
    /**
     * 删除字典映射
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        dictMappingService.removeById(id);
        return Result.success(null);
    }
    
    /**
     * 获取字典映射项（按类型过滤）
     */
    @GetMapping("/{id}/items")
    public Result<List<DictItemMapping>> getMappingItems(
            @PathVariable Long id,
            @RequestParam(required = false) String sourceTypeValue,
            @RequestParam(required = false) String targetTypeValue) {
        List<DictItemMapping> items = dictMappingService.getMappingItems(id, sourceTypeValue, targetTypeValue);
        return Result.success(items);
    }
    
    /**
     * 批量保存字典映射项（按类型覆盖）
     */
    @PostMapping("/{id}/items")
    public Result<Void> saveMappingItems(
            @PathVariable Long id,
            @RequestParam(required = false) String sourceTypeValue,
            @RequestParam(required = false) String targetTypeValue,
            @RequestParam(required = false) String sourceTypeLabel,
            @RequestParam(required = false) String targetTypeLabel,
            @RequestBody List<DictItemMapping> items) {
        dictMappingService.saveMappingItems(id, sourceTypeValue, targetTypeValue, items);
        
        // 如果是自定义类型，更新typeLabels字段
        if ((sourceTypeLabel != null && !sourceTypeLabel.isEmpty()) || (targetTypeLabel != null && !targetTypeLabel.isEmpty())) {
            DictMapping dictMapping = dictMappingService.getById(id);
            if (dictMapping != null) {
                // 解析现有的typeLabels
                Map<String, Object> typeLabelsMap = new HashMap<>();
                String existingLabels = dictMapping.getTypeLabels();
                if (existingLabels != null && !existingLabels.isEmpty()) {
                    try {
                        ObjectMapper mapper = new ObjectMapper();
                        typeLabelsMap = mapper.readValue(existingLabels, Map.class);
                    } catch (Exception e) {
                        log.error("解析typeLabels失败", e);
                    }
                }
                
                // 确保存在source和target映射
                if (!typeLabelsMap.containsKey("source")) {
                    typeLabelsMap.put("source", new HashMap<String, String>());
                }
                if (!typeLabelsMap.containsKey("target")) {
                    typeLabelsMap.put("target", new HashMap<String, String>());
                }
                
                @SuppressWarnings("unchecked")
                Map<String, String> sourceMap = (Map<String, String>) typeLabelsMap.get("source");
                @SuppressWarnings("unchecked")
                Map<String, String> targetMap = (Map<String, String>) typeLabelsMap.get("target");
                
                // 更新类型标签
                if (sourceTypeValue != null && sourceTypeLabel != null && !sourceTypeLabel.isEmpty()) {
                    sourceMap.put(sourceTypeValue, sourceTypeLabel);
                }
                if (targetTypeValue != null && targetTypeLabel != null && !targetTypeLabel.isEmpty()) {
                    targetMap.put(targetTypeValue, targetTypeLabel);
                }
                
                // 序列化并更新
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    String updatedLabels = mapper.writeValueAsString(typeLabelsMap);
                    dictMapping.setTypeLabels(updatedLabels);
                    dictMappingService.updateById(dictMapping);
                    log.info("更新typeLabels成功: {}", updatedLabels);
                } catch (Exception e) {
                    log.error("序列化typeLabels失败", e);
                }
            }
        }
        
        return Result.success(null);
    }
    
    /**
     * 智能匹配字典项
     */
    @PostMapping("/{id}/auto-match")
    public Result<List<DictItemMapping>> autoMatch(
            @PathVariable Long id,
            @RequestParam(required = false) String sourceTypeValue,
            @RequestParam(required = false) String targetTypeValue) {
        List<DictItemMapping> items = dictMappingService.autoMatch(id, sourceTypeValue, targetTypeValue);
        return Result.success(items);
    }
    
    /**
     * 执行字典转换
     */
    @GetMapping("/{id}/transform")
    public Result<String> transform(
            @PathVariable Long id,
            @RequestParam String sourceValue) {
        String targetValue = dictMappingService.transform(id, sourceValue);
        return Result.success(targetValue);
    }
    
    /**
     * 获取字典映射中已配置的源字典类型列表（便捷接口）
     * 返回该字典映射中已配置映射项的源类型列表
     * 支持自定义源场景：当source_dict_id为空时，从type_labels中解析类型
     */
    @GetMapping("/{id}/types")
    public Result<List<Map<String, Object>>> getConfiguredSourceTypes(@PathVariable Long id) {
        // 获取字典映射信息
        DictMapping dictMapping = dictMappingService.getById(id);
        if (dictMapping == null) {
            return Result.error("字典映射不存在");
        }
        
        String mappingType = dictMapping.getMappingType();
        
        // 如果是自定义源场景(CUSTOM_TO_SOURCE 或 CUSTOM_TO_CUSTOM)，从type_labels解析类型
        if (dictMapping.getSourceDictId() == null && 
            ("CUSTOM_TO_SOURCE".equals(mappingType) || "CUSTOM_TO_CUSTOM".equals(mappingType))) {
            
            String typeLabelsJson = dictMapping.getTypeLabels();
            if (typeLabelsJson == null || typeLabelsJson.trim().isEmpty()) {
                // 如果type_labels为空，则从映射项中提取类型
                return extractTypesFromMappingItems(id);
            }
            
            try {
                // 解析type_labels JSON
                ObjectMapper mapper = new ObjectMapper();
                Map<String, Object> typeLabelsMap = mapper.readValue(typeLabelsJson, Map.class);
                Map<String, Object> sourceTypes = (Map<String, Object>) typeLabelsMap.get("source");
                
                if (sourceTypes != null && !sourceTypes.isEmpty()) {
                    List<Map<String, Object>> typeList = new ArrayList<>();
                    for (Map.Entry<String, Object> entry : sourceTypes.entrySet()) {
                        Map<String, Object> typeItem = new HashMap<>();
                        typeItem.put("value", entry.getKey());
                        typeItem.put("label", entry.getValue());
                        typeList.add(typeItem);
                    }
                    return Result.success(typeList);
                } else {
                    // 如果source为空，回退从映射项提取
                    return extractTypesFromMappingItems(id);
                }
            } catch (Exception e) {
                log.error("解析type_labels失败: {}", e.getMessage());
                // 解析失败，回退从映射项提取类型
                return extractTypesFromMappingItems(id);
            }
        }
        
        // 原有逻辑：源字典数据源场景
        if (dictMapping.getSourceDictId() == null) {
            return Result.error("该字典映射未配置源字典");
        }
        
        // 获取所有映射项
        List<DictItemMapping> items = dictMappingService.getMappingItems(id);
        
        // 提取已配置的源类型值（去重）
        Set<String> configuredTypeValues = new HashSet<>();
        for (DictItemMapping item : items) {
            String typeValue = item.getSourceTypeValue();
            if (typeValue != null && !typeValue.isEmpty()) {
                configuredTypeValues.add(typeValue);
            }
        }
        
        if (configuredTypeValues.isEmpty()) {
            return Result.success(new ArrayList<>());
        }
        
        // 获取源字典的所有类型
        List<Map<String, Object>> allTypes = dictSourceService.getDistinctTypes(dictMapping.getSourceDictId());
        
        // 过滤出已配置的类型
        List<Map<String, Object>> configuredTypes = new ArrayList<>();
        for (Map<String, Object> type : allTypes) {
            Object value = type.get("value");
            if (value != null && configuredTypeValues.contains(String.valueOf(value))) {
                configuredTypes.add(type);
            }
        }
        
        return Result.success(configuredTypes);
    }
    
    /**
     * 从映射项中提取已配置的类型（用于自定义源场景回退方案）
     */
    private Result<List<Map<String, Object>>> extractTypesFromMappingItems(Long mappingId) {
        // 获取所有映射项
        List<DictItemMapping> items = dictMappingService.getMappingItems(mappingId);
        
        // 提取已配置的源类型值（去重）
        Map<String, String> typeMap = new HashMap<>();
        for (DictItemMapping item : items) {
            String typeValue = item.getSourceTypeValue();
            if (typeValue != null && !typeValue.isEmpty()) {
                // 使用typeValue作为label（自定义场景下类型编码和名称相同）
                typeMap.put(typeValue, typeValue);
            }
        }
        
        List<Map<String, Object>> typeList = new ArrayList<>();
        for (Map.Entry<String, String> entry : typeMap.entrySet()) {
            Map<String, Object> typeItem = new HashMap<>();
            typeItem.put("value", entry.getKey());
            typeItem.put("label", entry.getValue());
            typeList.add(typeItem);
        }
        
        return Result.success(typeList);
    }
}
