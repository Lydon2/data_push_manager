package com.datapush.manager.service;

import com.datapush.manager.entity.DataLineage;

import java.util.List;
import java.util.Map;

/**
 * 数据血缘服务接口
 */
public interface DataLineageService {
    
    /**
     * 记录字段血缘关系
     */
    void recordLineage(DataLineage lineage);
    
    /**
     * 批量记录血缘关系
     */
    void batchRecordLineage(List<DataLineage> lineageList);
    
    /**
     * 查询字段的上游血缘
     */
    List<DataLineage> getUpstreamLineage(Long connectorId, String tableName, String fieldName);
    
    /**
     * 查询字段的下游血缘
     */
    List<DataLineage> getDownstreamLineage(Long connectorId, String tableName, String fieldName);
    
    /**
     * 查询任务的血缘关系
     */
    List<DataLineage> getTaskLineage(Long taskId);
    
    /**
     * 查询完整的血缘链路
     */
    List<Map<String, Object>> getLineageChain(Long connectorId, String tableName, String fieldName);
    
    /**
     * 构建血缘图谱数据（用于前端可视化）
     */
    Map<String, Object> buildLineageGraph(Long connectorId, String tableName, String fieldName);
    
    /**
     * 删除任务的血缘记录
     */
    void deleteByTaskId(Long taskId);
}
