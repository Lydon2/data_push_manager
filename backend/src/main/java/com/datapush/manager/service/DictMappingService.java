package com.datapush.manager.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.datapush.manager.entity.DictMapping;
import com.datapush.manager.entity.DictItemMapping;

import java.util.List;
import java.util.Map;

/**
 * 字典映射关系服务接口
 * 
 * @author Data Push Team
 * @since 2024-11-21
 */
public interface DictMappingService extends IService<DictMapping> {
    
    /**
     * 分页查询字典映射
     */
    IPage<DictMapping> pageList(Integer current, Integer size, String mappingName);
    
    /**
     * 获取所有字典映射（用于下拉选择）
     */
    List<DictMapping> listAll();
    
    /**
     * 获取字典映射项（按类型过滤）
     */
    List<DictItemMapping> getMappingItems(Long mappingId, String sourceTypeValue, String targetTypeValue);
    
    /**
     * 获取字典映射项（所有类型）
     */
    List<DictItemMapping> getMappingItems(Long mappingId);
    
    /**
     * 批量保存字典映射项（按类型覆盖）
     */
    void saveMappingItems(Long mappingId, String sourceTypeValue, String targetTypeValue, List<DictItemMapping> items);
    
    /**
     * 智能匹配字典项
     */
    List<DictItemMapping> autoMatch(Long mappingId, String sourceTypeValue, String targetTypeValue);
    
    /**
     * 执行字典转换
     */
    String transform(Long mappingId, String sourceValue);
    
    /**
     * 执行字典转换（指定类型）
     * @param mappingId 字典映射ID
     * @param sourceTypeValue 源类型值（用于区分同一字典映射下的不同类型）
     * @param sourceValue 源值
     * @return 目标值
     */
    String transform(Long mappingId, String sourceTypeValue, String sourceValue);
    
    /**
     * 加载映射缓存
     */
    Map<String, String> loadMappingCache(Long mappingId);
    
    /**
     * 加载映射缓存（指定类型）
     * @param mappingId 字典映射ID
     * @param sourceTypeValue 源类型值
     * @return Map<sourceKey, targetKey>
     */
    Map<String, String> loadMappingCache(Long mappingId, String sourceTypeValue);
    
    /**
     * 加载映射缓存（指定输出模式）
     * @param mappingId 字典映射ID
     * @param sourceTypeValue 源类型值
     * @param outputMode 输出模式：TARGET_KEY/SOURCE_LABEL/TARGET_LABEL
     * @return Map<sourceKey, outputValue>
     */
    Map<String, String> loadMappingCache(Long mappingId, String sourceTypeValue, String outputMode);
    
    /**
     * 执行字典转换（支持多选值）
     * @param mappingId 字典映射ID
     * @param sourceTypeValue 源类型值
     * @param sourceValue 源值（支持单值、逗号分隔、JSON数组）
     * @param outputMode 输出模式
     * @param separator 分隔符（默认逗号）
     * @return 目标值
     */
    String transformMultiple(Long mappingId, String sourceTypeValue, String sourceValue, String outputMode, String separator);
}
