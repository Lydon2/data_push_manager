package com.datapush.manager.config;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

/**
 * TraceId过滤器
 * 为每个请求生成唯一的traceId，用于日志追踪
 *
 * @author Data Push Team
 * @since 2024-11-24
 */
@Slf4j
@Component
@Order(1)
public class TraceIdFilter implements Filter {

    private static final String TRACE_ID = "traceId";
    private static final String TRACE_ID_HEADER = "X-Trace-Id";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        try {
            // 尝试从请求头获取traceId，如果没有则生成新的
            String traceId = httpRequest.getHeader(TRACE_ID_HEADER);
            if (traceId == null || traceId.trim().isEmpty()) {
                traceId = generateTraceId();
            }
            
            // 将traceId存入MDC，这样日志就能自动打印
            MDC.put(TRACE_ID, traceId);
            
            // 将traceId添加到响应头，方便前端追踪
            httpResponse.setHeader(TRACE_ID_HEADER, traceId);
            
            // 继续处理请求
            chain.doFilter(request, response);
            
        } finally {
            // 请求结束后清理MDC，避免内存泄漏
            MDC.remove(TRACE_ID);
        }
    }

    /**
     * 生成traceId
     * 格式: yyyyMMddHHmmssSSS + 8位随机数
     */
    private String generateTraceId() {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String random = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return timestamp + random;
    }
}
