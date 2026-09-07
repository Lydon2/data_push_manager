package com.datapush.manager.circuit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 断路器单元测试
 */
class CircuitBreakerTest {

    private CircuitBreaker circuitBreaker;

    @BeforeEach
    void setUp() {
        // 创建断路器：失败阈值3次，重置超时1秒，半开最大尝试2次
        circuitBreaker = new CircuitBreaker("test-breaker", 3, 1000, 2);
    }

    @Test
    void testInitialStateClosed() {
        assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.getState());
        assertTrue(circuitBreaker.allowRequest());
    }

    @Test
    void testTransitionToOpen() {
        // 记录3次失败
        circuitBreaker.recordFailure();
        circuitBreaker.recordFailure();
        circuitBreaker.recordFailure();
        
        // 应该切换到OPEN状态
        assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.getState());
        assertEquals(3, circuitBreaker.getFailureCount());
        assertFalse(circuitBreaker.allowRequest());
    }

    @Test
    void testTransitionToHalfOpen() throws InterruptedException {
        // 切换到OPEN状态
        circuitBreaker.recordFailure();
        circuitBreaker.recordFailure();
        circuitBreaker.recordFailure();
        
        assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.getState());
        
        // 等待重置超时时间（1秒）
        Thread.sleep(1100);
        
        // 再次请求应该切换到HALF_OPEN
        assertTrue(circuitBreaker.allowRequest());
        assertEquals(CircuitBreaker.State.HALF_OPEN, circuitBreaker.getState());
    }

    @Test
    void testHalfOpenToClosedOnSuccess() throws InterruptedException {
        // 切换到OPEN状态
        circuitBreaker.recordFailure();
        circuitBreaker.recordFailure();
        circuitBreaker.recordFailure();
        
        // 等待切换到HALF_OPEN
        Thread.sleep(1100);
        circuitBreaker.allowRequest();
        
        // HALF_OPEN状态下记录2次成功（达到halfOpenMaxAttempts）
        circuitBreaker.recordSuccess();
        circuitBreaker.recordSuccess();
        
        // 应该切换回CLOSED
        assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.getState());
        assertEquals(0, circuitBreaker.getFailureCount());
    }

    @Test
    void testHalfOpenToOpenOnFailure() throws InterruptedException {
        // 切换到OPEN状态
        circuitBreaker.recordFailure();
        circuitBreaker.recordFailure();
        circuitBreaker.recordFailure();
        
        // 等待切换到HALF_OPEN
        Thread.sleep(1100);
        circuitBreaker.allowRequest();
        
        // HALF_OPEN状态下记录失败
        circuitBreaker.recordFailure();
        
        // 应该切换回OPEN
        assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.getState());
    }

    @Test
    void testSuccessResetsFailureCount() {
        // 记录2次失败
        circuitBreaker.recordFailure();
        circuitBreaker.recordFailure();
        assertEquals(2, circuitBreaker.getFailureCount());
        
        // 记录成功
        circuitBreaker.recordSuccess();
        
        // 失败计数应该重置
        assertEquals(0, circuitBreaker.getFailureCount());
        assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.getState());
    }

    @Test
    void testManualReset() {
        // 切换到OPEN状态
        circuitBreaker.recordFailure();
        circuitBreaker.recordFailure();
        circuitBreaker.recordFailure();
        
        assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.getState());
        
        // 手动重置
        circuitBreaker.reset();
        
        // 应该回到CLOSED状态
        assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.getState());
        assertEquals(0, circuitBreaker.getFailureCount());
        assertTrue(circuitBreaker.allowRequest());
    }

    @Test
    void testGetStats() {
        circuitBreaker.recordFailure();
        circuitBreaker.recordFailure();
        
        CircuitBreaker.CircuitBreakerStats stats = circuitBreaker.getStats();
        
        assertEquals("test-breaker", stats.getName());
        assertEquals("CLOSED", stats.getState());
        assertEquals(2, stats.getFailureCount());
        assertEquals(0, stats.getSuccessCount());
        assertTrue(stats.getLastFailureTime() > 0);
    }
}
