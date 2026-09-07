package com.datapush.manager.circuit;

import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 断路器
 * 实现熔断降级策略，防止故障扩散
 *
 * @author Data Push Team
 * @since 2024-11-24
 */
@Slf4j
public class CircuitBreaker {
    
    /**
     * 断路器状态
     */
    public enum State {
        CLOSED,      // 关闭状态，正常请求
        OPEN,        // 开启状态，拒绝请求
        HALF_OPEN    // 半开状态，尝试恢复
    }
    
    private final String name;
    private volatile State state = State.CLOSED;
    
    // 失败计数器
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicInteger successCount = new AtomicInteger(0);
    
    // 配置参数
    private final int failureThreshold;      // 失败阈值
    private final long resetTimeoutMs;       // 重置超时时间（毫秒）
    private final int halfOpenMaxAttempts;   // 半开状态最大尝试次数
    
    // 时间记录
    private final AtomicLong lastFailureTime = new AtomicLong(0);
    private final AtomicLong openTime = new AtomicLong(0);
    
    /**
     * 构造函数
     * @param name 断路器名称
     * @param failureThreshold 失败阈值（连续失败多少次后开启）
     * @param resetTimeoutMs 重置超时时间（毫秒）
     * @param halfOpenMaxAttempts 半开状态最大尝试次数
     */
    public CircuitBreaker(String name, int failureThreshold, long resetTimeoutMs, int halfOpenMaxAttempts) {
        this.name = name;
        this.failureThreshold = failureThreshold;
        this.resetTimeoutMs = resetTimeoutMs;
        this.halfOpenMaxAttempts = halfOpenMaxAttempts;
    }
    
    /**
     * 默认构造函数
     * 失败阈值5次，重置超时60秒，半开状态最大尝试3次
     */
    public CircuitBreaker(String name) {
        this(name, 5, 60000, 3);
    }
    
    /**
     * 判断是否允许请求通过
     */
    public boolean allowRequest() {
        State currentState = state;
        
        if (currentState == State.CLOSED) {
            return true;
        }
        
        if (currentState == State.OPEN) {
            // 检查是否超过重置超时时间
            long now = System.currentTimeMillis();
            long openTimestamp = openTime.get();
            
            if (now - openTimestamp >= resetTimeoutMs) {
                // 切换到半开状态
                log.info("断路器 [{}] 从 OPEN 切换到 HALF_OPEN", name);
                state = State.HALF_OPEN;
                successCount.set(0);
                return true;
            }
            
            log.debug("断路器 [{}] 处于 OPEN 状态，拒绝请求", name);
            return false;
        }
        
        // HALF_OPEN 状态：允许有限次数的请求
        return successCount.get() < halfOpenMaxAttempts;
    }
    
    /**
     * 记录成功
     */
    public void recordSuccess() {
        State currentState = state;
        
        if (currentState == State.HALF_OPEN) {
            int currentSuccess = successCount.incrementAndGet();
            log.debug("断路器 [{}] HALF_OPEN 状态成功计数: {}/{}", name, currentSuccess, halfOpenMaxAttempts);
            
            if (currentSuccess >= halfOpenMaxAttempts) {
                // 半开状态成功次数达到阈值，切换到关闭状态
                log.info("断路器 [{}] 从 HALF_OPEN 切换到 CLOSED", name);
                state = State.CLOSED;
                failureCount.set(0);
                successCount.set(0);
            }
        } else if (currentState == State.CLOSED) {
            // 关闭状态下成功，重置失败计数
            if (failureCount.get() > 0) {
                failureCount.set(0);
                log.debug("断路器 [{}] 成功，重置失败计数", name);
            }
        }
    }
    
    /**
     * 记录失败
     */
    public void recordFailure() {
        lastFailureTime.set(System.currentTimeMillis());
        
        State currentState = state;
        
        if (currentState == State.HALF_OPEN) {
            // 半开状态失败，直接切换回开启状态
            log.warn("断路器 [{}] 从 HALF_OPEN 切换回 OPEN", name);
            state = State.OPEN;
            openTime.set(System.currentTimeMillis());
            successCount.set(0);
        } else if (currentState == State.CLOSED) {
            int currentFailures = failureCount.incrementAndGet();
            log.warn("断路器 [{}] 失败计数: {}/{}", name, currentFailures, failureThreshold);
            
            if (currentFailures >= failureThreshold) {
                // 失败次数达到阈值，切换到开启状态
                log.error("断路器 [{}] 从 CLOSED 切换到 OPEN，失败次数: {}", name, currentFailures);
                state = State.OPEN;
                openTime.set(System.currentTimeMillis());
            }
        }
    }
    
    /**
     * 手动重置断路器
     */
    public void reset() {
        log.info("断路器 [{}] 手动重置", name);
        state = State.CLOSED;
        failureCount.set(0);
        successCount.set(0);
        lastFailureTime.set(0);
        openTime.set(0);
    }
    
    /**
     * 获取当前状态
     */
    public State getState() {
        return state;
    }
    
    /**
     * 获取失败次数
     */
    public int getFailureCount() {
        return failureCount.get();
    }
    
    /**
     * 获取成功次数（半开状态）
     */
    public int getSuccessCount() {
        return successCount.get();
    }
    
    /**
     * 获取统计信息
     */
    public CircuitBreakerStats getStats() {
        return new CircuitBreakerStats(
            name,
            state.name(),
            failureCount.get(),
            successCount.get(),
            lastFailureTime.get(),
            openTime.get()
        );
    }
    
    /**
     * 断路器统计信息
     */
    public static class CircuitBreakerStats {
        private String name;
        private String state;
        private int failureCount;
        private int successCount;
        private long lastFailureTime;
        private long openTime;
        
        public CircuitBreakerStats(String name, String state, int failureCount, int successCount, 
                                   long lastFailureTime, long openTime) {
            this.name = name;
            this.state = state;
            this.failureCount = failureCount;
            this.successCount = successCount;
            this.lastFailureTime = lastFailureTime;
            this.openTime = openTime;
        }
        
        // Getters
        public String getName() { return name; }
        public String getState() { return state; }
        public int getFailureCount() { return failureCount; }
        public int getSuccessCount() { return successCount; }
        public long getLastFailureTime() { return lastFailureTime; }
        public long getOpenTime() { return openTime; }
    }
}
