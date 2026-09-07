package com.datapush.manager.controller;

import com.datapush.manager.common.Result;
import com.datapush.manager.connector.pool.ConnectionPoolManager;
import com.datapush.manager.entity.ConnectorInfo;
import com.datapush.manager.service.ConnectorInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 性能监控控制器 - 支持全部数据库类型
 * 
 * @author Data Push Team
 * @since 2024-12-05
 */
@Slf4j
@RestController
@RequestMapping("/v1/performance")
public class PerformanceController {
    
    @Autowired
    private ConnectionPoolManager connectionPoolManager;
    
    @Autowired
    private ConnectorInfoService connectorInfoService;
    
    /**
     * 获取连接池统计信息（支持所有数据库类型）
     */
    @GetMapping("/pool-stats/{connectorId}")
    public Result<Map<String, Object>> getPoolStats(@PathVariable Long connectorId) {
        try {
            ConnectorInfo connector = connectorInfoService.getById(connectorId);
            if (connector == null) {
                return Result.error("连接器不存在");
            }
            
            String stats = connectionPoolManager.getPoolStats(connectorId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("connectorId", connectorId);
            result.put("connectorName", connector.getConnectorName());
            result.put("dbType", connector.getDbType());
            result.put("stats", stats);
            
            // 添加数据库特定的优化信息
            result.put("optimizations", getDatabaseOptimizations(connector.getDbType()));
            
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取连接池统计失败: connectorId={}", connectorId, e);
            return Result.error("获取连接池统计失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取所有连接池统计信息
     */
    @GetMapping("/pool-stats")
    public Result<List<Map<String, Object>>> getAllPoolStats() {
        try {
            List<ConnectorInfo> connectors = connectorInfoService.list();
            List<Map<String, Object>> statsList = new ArrayList<>();
            
            for (ConnectorInfo connector : connectors) {
                try {
                    String stats = connectionPoolManager.getPoolStats(connector.getId());
                    if (!"连接池不存在".equals(stats)) {
                        Map<String, Object> item = new HashMap<>();
                        item.put("connectorId", connector.getId());
                        item.put("connectorName", connector.getConnectorName());
                        item.put("dbType", connector.getDbType());
                        item.put("stats", stats);
                        statsList.add(item);
                    }
                } catch (Exception e) {
                    log.warn("获取连接池统计失败: connectorId={}", connector.getId(), e);
                }
            }
            
            return Result.success(statsList);
        } catch (Exception e) {
            log.error("获取所有连接池统计失败", e);
            return Result.error("获取所有连接池统计失败: " + e.getMessage());
        }
    }
    
    /**
     * 清除指定连接池
     */
    @DeleteMapping("/pool/{connectorId}")
    public Result<String> evictPool(@PathVariable Long connectorId) {
        try {
            ConnectorInfo connector = connectorInfoService.getById(connectorId);
            if (connector == null) {
                return Result.error("连接器不存在");
            }
            
            connectionPoolManager.evictPool(connectorId);
            log.info("连接池已清除: connectorId={}, dbType={}", connectorId, connector.getDbType());
            return Result.success("连接池已清除");
        } catch (Exception e) {
            log.error("清除连接池失败: connectorId={}", connectorId, e);
            return Result.error("清除连接池失败: " + e.getMessage());
        }
    }
    
    /**
     * 清除所有连接池
     */
    @DeleteMapping("/pool")
    public Result<String> evictAllPools() {
        try {
            connectionPoolManager.shutdown();
            log.info("所有连接池已清除");
            return Result.success("所有连接池已清除");
        } catch (Exception e) {
            log.error("清除所有连接池失败", e);
            return Result.error("清除所有连接池失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取性能优化配置（各数据库类型）
     */
    @GetMapping("/optimization-config")
    public Result<Map<String, Object>> getOptimizationConfig() {
        Map<String, Object> config = new HashMap<>();
        
        // 各数据库JDBC优化参数
        Map<String, String> jdbcParams = new HashMap<>();
        jdbcParams.put("MYSQL", "rewriteBatchedStatements=true");
        jdbcParams.put("POSTGRESQL", "reWriteBatchedInserts=true");
        jdbcParams.put("KINGBASE", "reWriteBatchedInserts=true");
        jdbcParams.put("ORACLE", "defaultBatchValue=1000");
        jdbcParams.put("SQLSERVER", "useBulkCopyForBatchInsert=true");
        jdbcParams.put("DM", "batchAllowedInsert=true");
        config.put("jdbcOptimizations", jdbcParams);
        
        // 高性能加载器阈值
        Map<String, Integer> loaderThresholds = new HashMap<>();
        loaderThresholds.put("MYSQL", 10000);  // LOAD DATA INFILE
        loaderThresholds.put("POSTGRESQL", 5000);  // COPY FROM STDIN
        loaderThresholds.put("KINGBASE", 5000);  // COPY FROM STDIN
        loaderThresholds.put("ORACLE", 5000);  // 优化批量插入
        loaderThresholds.put("DM", 5000);  // 优化批量插入
        config.put("loaderThresholds", loaderThresholds);
        
        // 连接池配置
        Map<String, Object> poolConfig = new HashMap<>();
        poolConfig.put("maxPoolSize", 10);
        poolConfig.put("minIdle", 2);
        poolConfig.put("connectionTimeout", 10000);
        poolConfig.put("idleTimeout", 300000);
        poolConfig.put("maxLifetime", 600000);
        config.put("connectionPool", poolConfig);
        
        // 并行写入配置
        Map<String, Object> parallelConfig = new HashMap<>();
        parallelConfig.put("defaultThreads", 4);
        parallelConfig.put("enableThreshold", "batchSize * 2");
        config.put("parallelWrite", parallelConfig);
        
        return Result.success(config);
    }
    
    /**
     * 获取数据库特定的优化信息
     */
    private Map<String, Object> getDatabaseOptimizations(String dbType) {
        Map<String, Object> optimizations = new HashMap<>();
        
        switch (dbType.toUpperCase()) {
            case "MYSQL":
                optimizations.put("jdbcParam", "rewriteBatchedStatements=true");
                optimizations.put("loader", "LOAD DATA INFILE");
                optimizations.put("loaderThreshold", 10000);
                optimizations.put("performance", "50-150倍提升");
                break;
            case "POSTGRESQL":
                optimizations.put("jdbcParam", "reWriteBatchedInserts=true");
                optimizations.put("loader", "COPY FROM STDIN");
                optimizations.put("loaderThreshold", 5000);
                optimizations.put("performance", "30-100倍提升");
                break;
            case "KINGBASE":
                optimizations.put("jdbcParam", "reWriteBatchedInserts=true");
                optimizations.put("loader", "COPY FROM STDIN（基于PostgreSQL）");
                optimizations.put("loaderThreshold", 5000);
                optimizations.put("performance", "30-100倍提升");
                break;
            case "ORACLE":
                optimizations.put("jdbcParam", "defaultBatchValue=1000");
                optimizations.put("loader", "优化批量插入");
                optimizations.put("loaderThreshold", 5000);
                optimizations.put("performance", "30-60倍提升");
                break;
            case "DM":
                optimizations.put("jdbcParam", "batchAllowedInsert=true");
                optimizations.put("loader", "优化批量插入");
                optimizations.put("loaderThreshold", 5000);
                optimizations.put("performance", "30-80倍提升");
                break;
            case "SQLSERVER":
                optimizations.put("jdbcParam", "useBulkCopyForBatchInsert=true");
                optimizations.put("loader", "JDBC优化");
                optimizations.put("loaderThreshold", "自动启用");
                optimizations.put("performance", "20-40倍提升");
                break;
            default:
                optimizations.put("jdbcParam", "标准批量插入");
                optimizations.put("loader", "无专用加载器");
                break;
        }
        
        return optimizations;
    }
}
