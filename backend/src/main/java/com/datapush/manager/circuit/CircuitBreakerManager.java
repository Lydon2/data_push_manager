package com.datapush.manager.circuit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 断路器管理器
 * 统一管理所有断路器实例
 *
 * @author Data Push Team
 * @since 2024-11-24
 */
@Slf4j
@Component
public class CircuitBreakerManager {
    
    private final Map<String, CircuitBreaker> circuitBreakers = new ConcurrentHashMap<>();
    
    /**
     * 获取或创建断路器
     * @param name 断路器名称
     * @return 断路器实例
     */
    public CircuitBreaker getOrCreate(String name) {
        return circuitBreakers.computeIfAbsent(name, k -> {
            log.info("创建断路器: {}", name);
            return new CircuitBreaker(name);
        });
    }
    
    /**
     * 获取或创建断路器（自定义配置）
     */
    public CircuitBreaker getOrCreate(String name, int failureThreshold, long resetTimeoutMs, int halfOpenMaxAttempts) {
        return circuitBreakers.computeIfAbsent(name, k -> {
            log.info("创建断路器: {}, 失败阈值: {}, 重置超时: {}ms, 半开最大尝试: {}", 
                name, failureThreshold, resetTimeoutMs, halfOpenMaxAttempts);
            return new CircuitBreaker(name, failureThreshold, resetTimeoutMs, halfOpenMaxAttempts);
        });
    }
    
    /**
     * 获取断路器
     */
    public CircuitBreaker get(String name) {
        return circuitBreakers.get(name);
    }
    
    /**
     * 移除断路器
     */
    public void remove(String name) {
        CircuitBreaker removed = circuitBreakers.remove(name);
        if (removed != null) {
            log.info("移除断路器: {}", name);
        }
    }
    
    /**
     * 重置所有断路器
     */
    public void resetAll() {
        log.info("重置所有断路器");
        circuitBreakers.values().forEach(CircuitBreaker::reset);
    }
    
    /**
     * 获取所有断路器统计信息
     */
    public Map<String, CircuitBreaker.CircuitBreakerStats> getAllStats() {
        Map<String, CircuitBreaker.CircuitBreakerStats> stats = new ConcurrentHashMap<>();
        circuitBreakers.forEach((name, breaker) -> stats.put(name, breaker.getStats()));
        return stats;
    }
}
