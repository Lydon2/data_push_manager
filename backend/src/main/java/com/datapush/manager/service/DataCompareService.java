package com.datapush.manager.service;

import java.util.List;
import java.util.Map;

/**
 * 数据对比服务接口
 */
public interface DataCompareService {
    
    /**
     * 执行数据对比（通用接口）
     * 
     * @param sourceConnectorId 源连接器ID
     * @param sourceTableOrSql 源表名或SQL
     * @param targetConnectorId 目标连接器ID
     * @param targetTableOrSql 目标表名或SQL
     * @param compareFields 对比字段列表
     * @param keyFields 主键字段列表
     * @return 对比结果
     */
    Map<String, Object> compareData(
        Long sourceConnectorId, String sourceTableOrSql,
        Long targetConnectorId, String targetTableOrSql,
        List<String> compareFields, List<String> keyFields
    );
    
    /**
     * 执行数据对比
     * 
     * @param taskId 任务ID
     * @param compareFields 对比字段列表
     * @return 对比结果
     */
    Map<String, Object> compareData(Long taskId, List<String> compareFields);
    
    /**
     * 获取对比历史记录
     */
    List<Map<String, Object>> getCompareHistory(Long taskId);
    
    /**
     * 获取对比详情
     */
    Map<String, Object> getCompareDetail(Long compareId);
    
    /**
     * 导出差异数据
     */
    List<Map<String, Object>> exportDiffData(Long compareId);
    
    /**
     * 分页查询差异数据
     * 
     * @param compareId 对比ID
     * @param page 页码
     * @param pageSize 每页条数
     * @param diffType 差异类型（DIFFERENT/SOURCE_ONLY/TARGET_ONLY）
     * @return 分页结果
     */
    Map<String, Object> getDifferencesPaged(Long compareId, Integer page, Integer pageSize, String diffType);
    
    /**
     * 导出CSV格式的差异数据
     * 
     * @param compareId 对比ID
     * @return CSV字符串
     */
    String exportDiffDataAsCSV(Long compareId);
}
