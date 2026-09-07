package com.datapush.manager.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.datapush.manager.dto.TableColumnInfo;
import com.datapush.manager.entity.DictSource;

import java.util.List;
import java.util.Map;

/**
 * 字典数据源服务接口
 * 
 * @author Data Push Team
 * @since 2024-11-21
 */
public interface DictSourceService extends IService<DictSource> {
    
    /**
     * 分页查询字典数据源
     */
    IPage<DictSource> pageList(Integer current, Integer size, String sourceName);
    
    /**
     * 获取所有字典数据源（用于下拉选择）
     */
    List<DictSource> listAll();
    
    /**
     * 测试字典数据源连接
     */
    Map<String, Object> testConnection(Long id);
    
    /**
     * 预览字典数据
     */
    Map<String, Object> previewData(Long id, Integer limit);
    
    /**
     * 加载字典数据（用于转换）
     */
    Map<String, String> loadDictData(Long sourceId);
    
    /**
     * 加载指定类型的字典数据（用于智能匹配）
     */
    Map<String, String> loadDictDataByType(Long sourceId, String typeValue);
    
    /**
     * 获取连接器的所有表
     */
    List<String> getTablesByConnector(Long connectorId);
    
    /**
     * 获取表的所有字段
     */
    List<String> getTableColumns(Long connectorId, String tableName);
    
    /**
     * 获取表的所有字段（包含类型信息）
     */
    List<TableColumnInfo> getTableColumnsWithType(Long connectorId, String tableName);
    
    /**
     * 获取字典数据源的所有类型值（去重）
     */
    List<Map<String, Object>> getDistinctTypes(Long sourceId);
    
    /**
     * 获取指定类型的字典项
     */
    List<Map<String, Object>> getDictItems(Long sourceId, String typeValue);
}
