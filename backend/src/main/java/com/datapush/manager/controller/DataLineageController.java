package com.datapush.manager.controller;

import com.datapush.manager.common.Result;
import com.datapush.manager.entity.DataLineage;
import com.datapush.manager.service.DataLineageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 数据血缘控制器
 */
@Slf4j
@RestController
@RequestMapping("/v1/data-lineage")
public class DataLineageController {
    
    @Autowired
    private DataLineageService dataLineageService;
    
    @GetMapping("/upstream")
    public Result<List<DataLineage>> getUpstreamLineage(
            @RequestParam Long connectorId,
            @RequestParam String tableName,
            @RequestParam String fieldName) {
        
        log.info("查询上游血缘: connectorId={}, tableName={}, fieldName={}", 
                connectorId, tableName, fieldName);
        
        List<DataLineage> lineageList = dataLineageService.getUpstreamLineage(connectorId, tableName, fieldName);
        return Result.success(lineageList);
    }
    
    @GetMapping("/downstream")
    public Result<List<DataLineage>> getDownstreamLineage(
            @RequestParam Long connectorId,
            @RequestParam String tableName,
            @RequestParam String fieldName) {
        
        log.info("查询下游血缘: connectorId={}, tableName={}, fieldName={}", 
                connectorId, tableName, fieldName);
        
        List<DataLineage> lineageList = dataLineageService.getDownstreamLineage(connectorId, tableName, fieldName);
        return Result.success(lineageList);
    }
    
    @GetMapping("/task/{taskId}")
    public Result<List<DataLineage>> getTaskLineage(@PathVariable Long taskId) {
        log.info("查询任务血缘: taskId={}", taskId);
        List<DataLineage> lineageList = dataLineageService.getTaskLineage(taskId);
        return Result.success(lineageList);
    }
    
    @GetMapping("/chain")
    public Result<List<Map<String, Object>>> getLineageChain(
            @RequestParam Long connectorId,
            @RequestParam String tableName,
            @RequestParam String fieldName) {
        
        log.info("查询血缘链路: connectorId={}, tableName={}, fieldName={}", 
                connectorId, tableName, fieldName);
        
        List<Map<String, Object>> chain = dataLineageService.getLineageChain(connectorId, tableName, fieldName);
        return Result.success(chain);
    }
    
    @PostMapping("/graph")
    public Result<Map<String, Object>> buildLineageGraph(@RequestBody Map<String, Object> request) {
        Long connectorId = ((Number) request.get("connectorId")).longValue();
        String tableName = (String) request.get("tableName");
        String fieldName = (String) request.get("fieldName");
        
        log.info("构建血缘图谱: connectorId={}, tableName={}, fieldName={}", 
                connectorId, tableName, fieldName);
        
        Map<String, Object> graph = dataLineageService.buildLineageGraph(connectorId, tableName, fieldName);
        return Result.success(graph);
    }
    
    @PostMapping("/record")
    public Result<Void> recordLineage(@RequestBody DataLineage lineage) {
        log.info("记录血缘关系: {} -> {}", lineage.getSourceField(), lineage.getTargetField());
        dataLineageService.recordLineage(lineage);
        return Result.success();
    }
    
    @PostMapping("/batch")
    public Result<Void> batchRecordLineage(@RequestBody List<DataLineage> lineageList) {
        log.info("批量记录血缘关系，共 {} 条", lineageList.size());
        dataLineageService.batchRecordLineage(lineageList);
        return Result.success();
    }
    
    @DeleteMapping("/task/{taskId}")
    public Result<Void> deleteByTaskId(@PathVariable Long taskId) {
        log.info("删除任务血缘记录: taskId={}", taskId);
        dataLineageService.deleteByTaskId(taskId);
        return Result.success();
    }
    
    /**
     * 查询所有血缘记录（调试用）
     */
    @GetMapping("/all")
    public Result<List<DataLineage>> getAllLineage() {
        log.info("查询所有血缘记录");
        // 直接查询所有任务的血缘记录
        List<DataLineage> allLineage = dataLineageService.getTaskLineage(null);
        log.info("总计 {} 条血缘记录", allLineage.size());
        return Result.success(allLineage);
    }
}
