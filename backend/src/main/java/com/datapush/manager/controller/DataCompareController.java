package com.datapush.manager.controller;

import com.datapush.manager.common.Result;
import com.datapush.manager.service.DataCompareService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 数据对比控制器
 */
@Slf4j
@RestController
@RequestMapping("/v1/data-compare")
public class DataCompareController {
    
    @Autowired
    private DataCompareService dataCompareService;
    
    /**
     * 执行数据对比（通用接口）
     */
    @PostMapping("/compare")
    public Result<Map<String, Object>> compare(@RequestBody Map<String, Object> request) {
        log.info("执行数据对比: {}", request);
        
        try {
            Long sourceConnectorId = ((Number) request.get("sourceConnectorId")).longValue();
            String sourceTableOrSql = (String) request.get("sourceTableOrSql");
            Long targetConnectorId = ((Number) request.get("targetConnectorId")).longValue();
            String targetTableOrSql = (String) request.get("targetTableOrSql");
            List<String> compareFields = (List<String>) request.get("compareFields");
            List<String> keyFields = (List<String>) request.get("keyFields");
            
            Map<String, Object> result = dataCompareService.compareData(
                sourceConnectorId, sourceTableOrSql,
                targetConnectorId, targetTableOrSql,
                compareFields, keyFields
            );
            
            return Result.success(result);
        } catch (Exception e) {
            log.error("数据对比失败", e);
            return Result.error(e.getMessage());
        }
    }
    
    @PostMapping("/execute")
    public Result<Map<String, Object>> compareData(
            @RequestParam Long taskId,
            @RequestBody(required = false) List<String> compareFields) {
        
        log.info("执行数据对比，任务ID: {}", taskId);
        
        try {
            Map<String, Object> result = dataCompareService.compareData(taskId, compareFields);
            return Result.success(result);
        } catch (Exception e) {
            log.error("数据对比失败", e);
            return Result.error(e.getMessage());
        }
    }
    
    @GetMapping("/history/{taskId}")
    public Result<List<Map<String, Object>>> getCompareHistory(@PathVariable Long taskId) {
        log.info("查询对比历史，任务ID: {}", taskId);
        List<Map<String, Object>> history = dataCompareService.getCompareHistory(taskId);
        return Result.success(history);
    }
    
    @GetMapping("/detail/{compareId}")
    public Result<Map<String, Object>> getCompareDetail(@PathVariable Long compareId) {
        log.info("查询对比详情，对比ID: {}", compareId);
        Map<String, Object> detail = dataCompareService.getCompareDetail(compareId);
        return Result.success(detail);
    }
    
    @GetMapping("/export/{compareId}")
    public Result<List<Map<String, Object>>> exportDiffData(@PathVariable Long compareId) {
        log.info("导出差异数据，对比ID: {}", compareId);
        List<Map<String, Object>> diffData = dataCompareService.exportDiffData(compareId);
        return Result.success(diffData);
    }
    
    /**
     * 分页查询差异数据
     */
    @GetMapping("/differences/{compareId}")
    public Result<Map<String, Object>> getDifferences(
            @PathVariable Long compareId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) String diffType) {
        
        log.info("分页查询差异数据，compareId: {}, page: {}, pageSize: {}, diffType: {}", 
            compareId, page, pageSize, diffType);
        
        Map<String, Object> result = dataCompareService.getDifferencesPaged(compareId, page, pageSize, diffType);
        return Result.success(result);
    }
    
    /**
     * 导出CSV格式的差异数据
     */
    @GetMapping("/export-csv/{compareId}")
    public Result<String> exportDiffDataAsCSV(@PathVariable Long compareId) {
        log.info("导出CSV格式差异数据，对比ID: {}", compareId);
        String csv = dataCompareService.exportDiffDataAsCSV(compareId);
        return Result.success(csv);
    }
}
