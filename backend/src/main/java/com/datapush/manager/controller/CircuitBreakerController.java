package com.datapush.manager.controller;

import com.datapush.manager.circuit.CircuitBreaker;
import com.datapush.manager.circuit.CircuitBreakerManager;
import com.datapush.manager.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 断路器监控控制器
 * 提供断路器状态查询和管理接口
 *
 * @author Data Push Team
 * @since 2024-11-24
 */
@Slf4j
@RestController
@RequestMapping("/v1/circuit-breaker")
public class CircuitBreakerController {

    @Autowired
    private CircuitBreakerManager circuitBreakerManager;

    /**
     * 获取所有断路器统计信息
     */
    @GetMapping("/stats")
    public Result<Map<String, CircuitBreaker.CircuitBreakerStats>> getAllStats() {
        Map<String, CircuitBreaker.CircuitBreakerStats> stats = circuitBreakerManager.getAllStats();
        return Result.success(stats);
    }

    /**
     * 获取指定断路器统计信息
     */
    @GetMapping("/stats/{name}")
    public Result<CircuitBreaker.CircuitBreakerStats> getStats(@PathVariable String name) {
        CircuitBreaker breaker = circuitBreakerManager.get(name);
        if (breaker == null) {
            return Result.error("断路器不存在: " + name);
        }
        return Result.success(breaker.getStats());
    }

    /**
     * 重置指定断路器
     */
    @PostMapping("/reset/{name}")
    public Result<?> reset(@PathVariable String name) {
        CircuitBreaker breaker = circuitBreakerManager.get(name);
        if (breaker == null) {
            return Result.error("断路器不存在: " + name);
        }
        breaker.reset();
        log.info("断路器 {} 已重置", name);
        return Result.success("重置成功");
    }

    /**
     * 重置所有断路器
     */
    @PostMapping("/reset-all")
    public Result<?> resetAll() {
        circuitBreakerManager.resetAll();
        log.info("所有断路器已重置");
        return Result.success("重置成功");
    }

    /**
     * 移除指定断路器
     */
    @DeleteMapping("/{name}")
    public Result<?> remove(@PathVariable String name) {
        circuitBreakerManager.remove(name);
        log.info("断路器 {} 已移除", name);
        return Result.success("移除成功");
    }
}
