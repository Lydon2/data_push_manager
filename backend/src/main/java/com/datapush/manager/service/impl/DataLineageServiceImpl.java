package com.datapush.manager.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.datapush.manager.entity.DataLineage;
import com.datapush.manager.mapper.DataLineageMapper;
import com.datapush.manager.service.DataLineageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 数据血缘服务实现
 * 支持缓存优化
 */
@Slf4j
@Service
public class DataLineageServiceImpl implements DataLineageService {
    
    @Autowired
    private DataLineageMapper dataLineageMapper;
    
    // 血缘图谱缓存（键：connectorId:tableName:fieldName）
    private final Map<String, CachedLineageGraph> graphCache = new ConcurrentHashMap<>();
    // 缓存过期时间（5分钟）
    private static final long CACHE_EXPIRE_MS = 5 * 60 * 1000;
    
    /**
     * 缓存的血缘图谱
     */
    private static class CachedLineageGraph {
        Map<String, Object> graph;
        long timestamp;
        
        CachedLineageGraph(Map<String, Object> graph) {
            this.graph = graph;
            this.timestamp = System.currentTimeMillis();
        }
        
        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_EXPIRE_MS;
        }
    }
    
    @Override
    public void recordLineage(DataLineage lineage) {
        dataLineageMapper.insert(lineage);
        log.info("记录数据血缘: {} -> {}", lineage.getSourceField(), lineage.getTargetField());
        // 清除相关缓存
        clearRelatedCache(lineage);
    }
    
    @Override
    public void batchRecordLineage(List<DataLineage> lineageList) {
        if (lineageList == null || lineageList.isEmpty()) {
            return;
        }
        
        for (DataLineage lineage : lineageList) {
            dataLineageMapper.insert(lineage);
        }
        
        log.info("批量记录数据血缘，共 {} 条", lineageList.size());
        // 清除相关缓存
        lineageList.forEach(this::clearRelatedCache);
    }
    
    @Override
    public List<DataLineage> getUpstreamLineage(Long connectorId, String tableName, String fieldName) {
        log.info("查询上游血缘: connectorId={}, tableName={}, fieldName={}", connectorId, tableName, fieldName);
        List<DataLineage> result = dataLineageMapper.selectUpstreamByField(connectorId, tableName, fieldName);
        log.info("上游血缘查询结果: {} 条", result.size());
        return result;
    }
    
    @Override
    public List<DataLineage> getDownstreamLineage(Long connectorId, String tableName, String fieldName) {
        log.info("查询下游血缘: connectorId={}, tableName={}, fieldName={}", connectorId, tableName, fieldName);
        List<DataLineage> result = dataLineageMapper.selectDownstreamByField(connectorId, tableName, fieldName);
        log.info("下游血缘查询结果: {} 条", result.size());
        return result;
    }
    
    @Override
    public List<DataLineage> getTaskLineage(Long taskId) {
        return dataLineageMapper.selectByTaskId(taskId);
    }
    
    @Override
    public List<Map<String, Object>> getLineageChain(Long connectorId, String tableName, String fieldName) {
        return dataLineageMapper.selectLineageChain(connectorId, tableName, fieldName);
    }
    
    @Override
    public Map<String, Object> buildLineageGraph(Long connectorId, String tableName, String fieldName) {
        // 尝试从缓存获取
        String cacheKey = buildNodeId(connectorId, tableName, fieldName);
        CachedLineageGraph cached = graphCache.get(cacheKey);
        
        if (cached != null && !cached.isExpired()) {
            log.info("使用缓存的血缘图谱: {}", cacheKey);
            return cached.graph;
        }
        
        // 缓存过期或不存在，重新构建
        Map<String, Object> graph = buildLineageGraphInternal(connectorId, tableName, fieldName);
        
        // 存入缓存
        graphCache.put(cacheKey, new CachedLineageGraph(graph));
        
        // 定期清理过期缓存
        cleanExpiredCache();
        
        return graph;
    }
    
    /**
     * 内部方法：构建血缘图谱（无缓存）
     */
    private Map<String, Object> buildLineageGraphInternal(Long connectorId, String tableName, String fieldName) {
        Map<String, Object> graph = new LinkedHashMap<>();
        
        // 节点列表
        List<Map<String, Object>> nodes = new ArrayList<>();
        // 边列表
        List<Map<String, Object>> edges = new ArrayList<>();
        
        // 使用Set去重
        Set<String> nodeIds = new HashSet<>();
        
        // 查询上游血缘（递归查询所有层级）
        List<DataLineage> upstreamList = new ArrayList<>();
        queryUpstreamRecursive(connectorId, tableName, fieldName, upstreamList, new HashSet<>());
        
        // 查询下游血缘（递归查询所有层级）
        List<DataLineage> downstreamList = new ArrayList<>();
        queryDownstreamRecursive(connectorId, tableName, fieldName, downstreamList, new HashSet<>());
        
        // 添加当前节点
        String currentNodeId = buildNodeId(connectorId, tableName, fieldName);
        if (nodeIds.add(currentNodeId)) {
            Map<String, Object> currentNode = new LinkedHashMap<>();
            currentNode.put("id", currentNodeId);
            currentNode.put("label", fieldName);
            currentNode.put("table", tableName);
            currentNode.put("connectorId", connectorId);
            currentNode.put("type", "current");
            nodes.add(currentNode);
        }
        
        // 处理上游血缘
        for (DataLineage lineage : upstreamList) {
            // 源节点
            String sourceNodeId = buildNodeId(lineage.getSourceConnectorId(), lineage.getSourceTable(), lineage.getSourceField());
            if (nodeIds.add(sourceNodeId)) {
                Map<String, Object> sourceNode = new LinkedHashMap<>();
                sourceNode.put("id", sourceNodeId);
                sourceNode.put("label", lineage.getSourceField());
                sourceNode.put("table", lineage.getSourceTable());
                sourceNode.put("connectorId", lineage.getSourceConnectorId());
                sourceNode.put("type", "source");
                sourceNode.put("fieldType", lineage.getSourceFieldType());
                nodes.add(sourceNode);
            }
            
            // 目标节点
            String targetNodeId = buildNodeId(lineage.getTargetConnectorId(), lineage.getTargetTable(), lineage.getTargetField());
            if (nodeIds.add(targetNodeId)) {
                Map<String, Object> targetNode = new LinkedHashMap<>();
                targetNode.put("id", targetNodeId);
                targetNode.put("label", lineage.getTargetField());
                targetNode.put("table", lineage.getTargetTable());
                targetNode.put("connectorId", lineage.getTargetConnectorId());
                targetNode.put("type", "intermediate");
                targetNode.put("fieldType", lineage.getTargetFieldType());
                nodes.add(targetNode);
            }
            
            // 边
            Map<String, Object> edge = new LinkedHashMap<>();
            edge.put("source", sourceNodeId);
            edge.put("target", targetNodeId);
            edge.put("transformType", lineage.getTransformType());
            edge.put("transformRule", lineage.getTransformRule());
            edge.put("taskId", lineage.getTaskId());
            edges.add(edge);
        }
        
        // 处理下游血缘
        for (DataLineage lineage : downstreamList) {
            // 源节点
            String sourceNodeId = buildNodeId(lineage.getSourceConnectorId(), lineage.getSourceTable(), lineage.getSourceField());
            if (nodeIds.add(sourceNodeId)) {
                Map<String, Object> sourceNode = new LinkedHashMap<>();
                sourceNode.put("id", sourceNodeId);
                sourceNode.put("label", lineage.getSourceField());
                sourceNode.put("table", lineage.getSourceTable());
                sourceNode.put("connectorId", lineage.getSourceConnectorId());
                sourceNode.put("type", "intermediate");
                sourceNode.put("fieldType", lineage.getSourceFieldType());
                nodes.add(sourceNode);
            }
            
            // 目标节点
            String targetNodeId = buildNodeId(lineage.getTargetConnectorId(), lineage.getTargetTable(), lineage.getTargetField());
            if (nodeIds.add(targetNodeId)) {
                Map<String, Object> targetNode = new LinkedHashMap<>();
                targetNode.put("id", targetNodeId);
                targetNode.put("label", lineage.getTargetField());
                targetNode.put("table", lineage.getTargetTable());
                targetNode.put("connectorId", lineage.getTargetConnectorId());
                targetNode.put("type", "target");
                targetNode.put("fieldType", lineage.getTargetFieldType());
                nodes.add(targetNode);
            }
            
            // 边
            Map<String, Object> edge = new LinkedHashMap<>();
            edge.put("source", sourceNodeId);
            edge.put("target", targetNodeId);
            edge.put("transformType", lineage.getTransformType());
            edge.put("transformRule", lineage.getTransformRule());
            edge.put("taskId", lineage.getTaskId());
            edges.add(edge);
        }
        
        graph.put("nodes", nodes);
        graph.put("edges", edges);
        graph.put("currentNode", currentNodeId);
        
        log.info("构建血缘图谱完成，节点数: {}, 边数: {}", nodes.size(), edges.size());
        
        return graph;
    }
    
    @Override
    public void deleteByTaskId(Long taskId) {
        LambdaQueryWrapper<DataLineage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DataLineage::getTaskId, taskId);
        dataLineageMapper.delete(wrapper);
        log.info("删除任务血缘记录，taskId: {}", taskId);
        // 清空缓存（任务删除后血缘关系可能变化）
        graphCache.clear();
    }
    
    /**
     * 清除相关缓存
     */
    private void clearRelatedCache(DataLineage lineage) {
        // 清除源字段和目标字段的缓存
        String sourceKey = buildNodeId(lineage.getSourceConnectorId(), lineage.getSourceTable(), lineage.getSourceField());
        String targetKey = buildNodeId(lineage.getTargetConnectorId(), lineage.getTargetTable(), lineage.getTargetField());
        graphCache.remove(sourceKey);
        graphCache.remove(targetKey);
    }
    
    /**
     * 清理过期缓存
     */
    private void cleanExpiredCache() {
        if (graphCache.size() > 100) { // 缓存数量超过100时执行清理
            graphCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
            log.debug("清理过期血缘缓存，剩余: {}", graphCache.size());
        }
    }
    
    /**
     * 递归查询上游血缘
     */
    private void queryUpstreamRecursive(Long connectorId, String tableName, String fieldName, 
                                       List<DataLineage> result, Set<String> visited) {
        String key = buildNodeId(connectorId, tableName, fieldName);
        if (visited.contains(key)) {
            return; // 避免循环依赖
        }
        visited.add(key);
        
        List<DataLineage> upstream = dataLineageMapper.selectUpstreamByField(connectorId, tableName, fieldName);
        for (DataLineage lineage : upstream) {
            result.add(lineage);
            // 递归查询源字段的上游
            queryUpstreamRecursive(
                lineage.getSourceConnectorId(),
                lineage.getSourceTable(),
                lineage.getSourceField(),
                result,
                visited
            );
        }
    }
    
    /**
     * 递归查询下游血缘
     */
    private void queryDownstreamRecursive(Long connectorId, String tableName, String fieldName,
                                         List<DataLineage> result, Set<String> visited) {
        String key = buildNodeId(connectorId, tableName, fieldName);
        if (visited.contains(key)) {
            return; // 避免循环依赖
        }
        visited.add(key);
        
        List<DataLineage> downstream = dataLineageMapper.selectDownstreamByField(connectorId, tableName, fieldName);
        for (DataLineage lineage : downstream) {
            result.add(lineage);
            // 递归查询目标字段的下游
            queryDownstreamRecursive(
                lineage.getTargetConnectorId(),
                lineage.getTargetTable(),
                lineage.getTargetField(),
                result,
                visited
            );
        }
    }
    
    /**
     * 构建节点ID
     */
    private String buildNodeId(Long connectorId, String tableName, String fieldName) {
        return connectorId + ":" + tableName + ":" + fieldName;
    }
}
