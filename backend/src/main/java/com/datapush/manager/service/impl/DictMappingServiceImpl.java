package com.datapush.manager.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.datapush.manager.entity.DictItemMapping;
import com.datapush.manager.entity.DictMapping;
import com.datapush.manager.mapper.DictItemMappingMapper;
import com.datapush.manager.mapper.DictMappingMapper;
import com.datapush.manager.service.DictMappingService;
import com.datapush.manager.service.DictSourceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 字典映射关系服务实现
 * 
 * @author Data Push Team
 * @since 2024-11-21
 */
@Slf4j
@Service
public class DictMappingServiceImpl extends ServiceImpl<DictMappingMapper, DictMapping> implements DictMappingService {
    
    @Autowired
    private DictItemMappingMapper dictItemMappingMapper;
    
    @Autowired
    private DictSourceService dictSourceService;
    
    /**
     * 字典映射缓存（性能优化）
     * Key: mappingId_sourceTypeValue_outputMode
     * Value: Map<sourceKey, outputValue>
     */
    private final Map<String, Map<String, String>> dictMappingCache = new ConcurrentHashMap<>();
    
    @Override
    public IPage<DictMapping> pageList(Integer current, Integer size, String mappingName) {
        Page<DictMapping> page = new Page<>(current, size);
        LambdaQueryWrapper<DictMapping> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(mappingName)) {
            wrapper.like(DictMapping::getMappingName, mappingName);
        }
        
        wrapper.orderByDesc(DictMapping::getCreateTime);
        return page(page, wrapper);
    }
    
    @Override
    public List<DictMapping> listAll() {
        LambdaQueryWrapper<DictMapping> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DictMapping::getStatus, 1);
        wrapper.orderByAsc(DictMapping::getMappingName);
        return list(wrapper);
    }
    
    @Override
    public List<DictItemMapping> getMappingItems(Long mappingId, String sourceTypeValue, String targetTypeValue) {
        LambdaQueryWrapper<DictItemMapping> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DictItemMapping::getMappingId, mappingId);
        
        // 按类型过滤
        if (StringUtils.hasText(sourceTypeValue)) {
            wrapper.eq(DictItemMapping::getSourceTypeValue, sourceTypeValue);
        }
        if (StringUtils.hasText(targetTypeValue)) {
            wrapper.eq(DictItemMapping::getTargetTypeValue, targetTypeValue);
        }
        
        wrapper.orderByAsc(DictItemMapping::getSortOrder);
        return dictItemMappingMapper.selectList(wrapper);
    }
    
    @Override
    public List<DictItemMapping> getMappingItems(Long mappingId) {
        return getMappingItems(mappingId, null, null);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveMappingItems(Long mappingId, String sourceTypeValue, String targetTypeValue, List<DictItemMapping> items) {
        // 删除原有映射项（按类型过滤）
        LambdaQueryWrapper<DictItemMapping> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DictItemMapping::getMappingId, mappingId);
        
        if (StringUtils.hasText(sourceTypeValue)) {
            wrapper.eq(DictItemMapping::getSourceTypeValue, sourceTypeValue);
        }
        if (StringUtils.hasText(targetTypeValue)) {
            wrapper.eq(DictItemMapping::getTargetTypeValue, targetTypeValue);
        }
        
        dictItemMappingMapper.delete(wrapper);
        
        // 批量插入新映射项
        if (items != null && !items.isEmpty()) {
            for (int i = 0; i < items.size(); i++) {
                DictItemMapping item = items.get(i);
                item.setMappingId(mappingId);
                item.setSourceTypeValue(sourceTypeValue);
                item.setTargetTypeValue(targetTypeValue);
                item.setSortOrder(i);
                dictItemMappingMapper.insert(item);
            }
        }
        
        log.info("保存字典映射项成功，映射ID: {}, 源类型: {}, 目标类型: {}, 项数: {}", 
                 mappingId, sourceTypeValue, targetTypeValue, items.size());
    }
    
    @Override
    public List<DictItemMapping>    autoMatch(Long mappingId, String sourceTypeValue, String targetTypeValue) {
        DictMapping mapping = getById(mappingId);
        if (mapping == null) {
            throw new RuntimeException("字典映射不存在");
        }
        
        // 仅支持 SOURCE_TO_SOURCE 的智能匹配
        if (!"SOURCE_TO_SOURCE".equals(mapping.getMappingType())) {
            throw new RuntimeException("仅支持源→目标数据源的智能匹配，请手动配置或使用前端智能填充");
        }
        
        // 验证目标数据源
        if (mapping.getTargetDictId() == null) {
            throw new RuntimeException("数据源映射必须配置目标数据源");
        }
        
        // 加载源字典数据（按类型过滤）
        Map<String, String> sourceDict = dictSourceService.loadDictDataByType(
            mapping.getSourceDictId(), sourceTypeValue);
        if (sourceDict == null || sourceDict.isEmpty()) {
            log.warn("源字典数据为空，映射ID: {}, 类型: {}", mappingId, sourceTypeValue);
            return new ArrayList<>();
        }
        
        // 加载目标字典数据（按类型过滤）
        Map<String, String> targetDict = dictSourceService.loadDictDataByType(
            mapping.getTargetDictId(), targetTypeValue);
        if (targetDict == null || targetDict.isEmpty()) {
            log.warn("目标字典数据为空，映射ID: {}, 类型: {}", mappingId, targetTypeValue);
            return new ArrayList<>();
        }
        
        // 智能匹配（以目标字典项为基准）
        List<DictItemMapping> matchedItems = new ArrayList<>();
        
        // 注意：sourceDict 和 targetDict 的结构是 Map<名称, 编码>
        // 为提升匹配成功率，构建源字典的规范化名称映射（去空格+小写）
        Map<String, String> normalizedSource = new HashMap<>();
        for (Map.Entry<String, String> e : sourceDict.entrySet()) {
            String lbl = e.getKey();
            String code = e.getValue();
            if (lbl != null && code != null) {
                String norm = lbl.trim().toLowerCase();
                if (!norm.isEmpty()) {
                    normalizedSource.put(norm, code);
                }
            }
        }
        
        // 遍历目标字典项：每个目标项都生成一行（未匹配时源端留空），确保数量与目标字典项一致
        for (Map.Entry<String, String> targetEntry : targetDict.entrySet()) {
            String targetLabel = targetEntry.getKey();    // 目标名称
            String targetKey = targetEntry.getValue();    // 目标编码
            
            if (targetKey == null || targetLabel == null) {
                continue;
            }
            
            // 在源字典中查找同名项（规范化对比）
            String normTarget = targetLabel.trim().toLowerCase();
            String sourceKey = normalizedSource.get(normTarget);
            String sourceLabel = (sourceKey != null) ? targetLabel : null;
            
            // 生成映射项（即使未匹配也加一行，源端留空以便人工补齐）
            DictItemMapping item = new DictItemMapping();
            item.setMappingId(mappingId);
            item.setSourceTypeValue(sourceTypeValue);
            item.setTargetTypeValue(targetTypeValue);
            item.setSourceKey(sourceKey != null ? sourceKey : "");
            item.setSourceLabel(sourceLabel != null ? sourceLabel : "");
            item.setTargetKey(targetKey);
            item.setTargetLabel(targetLabel);
            matchedItems.add(item);
        }
        
        log.info("智能匹配完成(以目标为基准)，映射ID: {}, 源项数: {}, 目标项数: {}, 匹配项数: {}",
                 mappingId, sourceDict.size(), targetDict.size(), matchedItems.size());
        return matchedItems;
    }
    
    @Override
    public String transform(Long mappingId, String sourceValue) {
        if (sourceValue == null) {
            return null;
        }
        
        // 加载映射缓存
        Map<String, String> mappingCache = loadMappingCache(mappingId);
        
        // 执行转换
        String targetValue = mappingCache.get(sourceValue);
        
        // 如果未找到映射，返回默认值
        if (targetValue == null) {
            DictMapping mapping = getById(mappingId);
            if (mapping != null && StringUtils.hasText(mapping.getDefaultValue())) {
                return mapping.getDefaultValue();
            }
            // 如果没有默认值，返回原值
            return sourceValue;
        }
        
        return targetValue;
    }
    
    @Override
    public String transform(Long mappingId, String sourceTypeValue, String sourceValue) {
        if (sourceValue == null) {
            return null;
        }
        
        // 加载指定类型的映射缓存
        Map<String, String> mappingCache = loadMappingCache(mappingId, sourceTypeValue);
        
        // 执行转换
        String targetValue = mappingCache.get(sourceValue);
        
        // 如果未找到映射，返回默认值
        if (targetValue == null) {
            DictMapping mapping = getById(mappingId);
            if (mapping != null && StringUtils.hasText(mapping.getDefaultValue())) {
                return mapping.getDefaultValue();
            }
            // 如果没有默认值，返回原值
            return sourceValue;
        }
        
        return targetValue;
    }
    
    @Override
    public Map<String, String> loadMappingCache(Long mappingId) {
        return loadMappingCache(mappingId, null, "TARGET_KEY");
    }
    
    @Override
    public Map<String, String> loadMappingCache(Long mappingId, String sourceTypeValue) {
        return loadMappingCache(mappingId, sourceTypeValue, "TARGET_KEY");
    }
    
    @Override
    public Map<String, String> loadMappingCache(Long mappingId, String sourceTypeValue, String outputMode) {
        // 默认输出模式
        final String finalOutputMode = (outputMode == null || outputMode.isEmpty()) ? "TARGET_KEY" : outputMode;
        
        // 使用缓存避免重复查询数据库（性能优化）
        String cacheKey = mappingId + "_" + (sourceTypeValue != null ? sourceTypeValue : "null") + "_" + finalOutputMode;
        
        return dictMappingCache.computeIfAbsent(cacheKey, key -> {
            // 加载指定类型的映射项
            List<DictItemMapping> items = getMappingItems(mappingId, sourceTypeValue, null);
            
            return items.stream()
                    .collect(Collectors.toMap(
                            DictItemMapping::getSourceKey,
                            item -> getOutputValue(item, finalOutputMode),
                            (v1, v2) -> v1  // 如果有重复键，保留第一个
                    ));
        });
    }
    
    /**
     * 根据输出模式获取输出值
     */
    private String getOutputValue(DictItemMapping item, String outputMode) {
        if (outputMode == null || outputMode.isEmpty()) {
            outputMode = "TARGET_KEY";  // 默认模式
        }
        
        switch (outputMode) {
            case "SOURCE_LABEL":
                return item.getSourceLabel();  // 源名称
            case "TARGET_LABEL":
                return item.getTargetLabel();  // 目标名称
            case "TARGET_KEY":
            default:
                return item.getTargetKey();    // 目标编码（默认）
        }
    }
    
    @Override
    public String transformMultiple(Long mappingId, String sourceTypeValue, String sourceValue, String outputMode, String separator) {
        if (sourceValue == null || sourceValue.trim().isEmpty()) {
            return null;
        }
        
        // 默认分隔符
        if (separator == null || separator.isEmpty()) {
            separator = ",";
        }
        
        // 默认输出模式
        if (outputMode == null || outputMode.isEmpty()) {
            outputMode = "TARGET_KEY";
        }
        
        // 加载映射缓存
        Map<String, String> mappingCache = loadMappingCache(mappingId, sourceTypeValue, outputMode);
        
        // 获取默认值
        DictMapping mapping = getById(mappingId);
        String defaultValue = (mapping != null && StringUtils.hasText(mapping.getDefaultValue())) 
                              ? mapping.getDefaultValue() : null;
        
        // 判断格式并解析
        List<String> sourceValues = parseMultipleValues(sourceValue);
        
        // 转换每个值
        List<String> targetValues = new ArrayList<>();
        for (String singleValue : sourceValues) {
            if (singleValue == null || singleValue.trim().isEmpty()) {
                continue;
            }
            
            String trimmedValue = singleValue.trim();
            String targetValue = mappingCache.get(trimmedValue);
            
            // 如果未找到映射，使用默认值或原值
            if (targetValue == null) {
                targetValue = defaultValue != null ? defaultValue : trimmedValue;
            }
            
            targetValues.add(targetValue);
        }
        
        // 根据原始格式返回结果
        return formatMultipleValues(sourceValue, targetValues, separator);
    }
    
    /**
     * 解析多选值（支持多种格式）
     * @param sourceValue 源值
     * @return 解析后的值列表
     */
    private List<String> parseMultipleValues(String sourceValue) {
        List<String> result = new ArrayList<>();
        
        if (sourceValue == null || sourceValue.trim().isEmpty()) {
            return result;
        }
        
        String trimmed = sourceValue.trim();
        
        // 格式1: JSON数组 "[1,2,3]" 或 "[\"a\",\"b\",\"c\"]"
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            String content = trimmed.substring(1, trimmed.length() - 1);
            if (content.isEmpty()) {
                return result;
            }
            
            // 分隔并清理
            String[] parts = content.split(",");
            for (String part : parts) {
                String cleaned = part.trim();
                // 移除引号
                if ((cleaned.startsWith("\"") && cleaned.endsWith("\"")) 
                    || (cleaned.startsWith("'") && cleaned.endsWith("'"))) {
                    cleaned = cleaned.substring(1, cleaned.length() - 1);
                }
                if (!cleaned.isEmpty()) {
                    result.add(cleaned);
                }
            }
            return result;
        }
        
        // 格式2: 逗号分隔 "1,2,3"
        if (trimmed.contains(",")) {
            String[] parts = trimmed.split(",");
            for (String part : parts) {
                String cleaned = part.trim();
                if (!cleaned.isEmpty()) {
                    result.add(cleaned);
                }
            }
            return result;
        }
        
        // 格式3: 分号分隔 "1;2;3"
        if (trimmed.contains(";")) {
            String[] parts = trimmed.split(";");
            for (String part : parts) {
                String cleaned = part.trim();
                if (!cleaned.isEmpty()) {
                    result.add(cleaned);
                }
            }
            return result;
        }
        
        // 格式4: 单个值
        result.add(trimmed);
        return result;
    }
    
    /**
     * 格式化多选值输出（统一输出为逗号分隔）
     * @param originalValue 原始值
     * @param targetValues 转换后的值列表
     * @param separator 分隔符
     * @return 格式化后的结果
     */
    private String formatMultipleValues(String originalValue, List<String> targetValues, String separator) {
        if (targetValues == null || targetValues.isEmpty()) {
            return null;
        }
        
        // 统一使用逗号分隔输出
        return String.join(",", targetValues);
    }
}
