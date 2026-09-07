package com.datapush.manager.common;

import com.datapush.manager.util.EncryptUtil;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

import java.sql.SQLException;

/**
 * 全局异常处理器
 * 支持日志脱敏
 *
 * @author Data Push Team
 * @since 2024-11-20
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusinessException(BusinessException e) {
        String traceId = MDC.get("traceId");
        // 脱敏日志
        String safeMessage = EncryptUtil.desensitizeLog(e.getMessage());
        log.error("[业务异常] traceId={}, code={}, message={}", traceId, e.getCode(), safeMessage);
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理参数校验异常
     */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public Result<?> handleValidationException(Exception e) {
        String traceId = MDC.get("traceId");
        String message = "参数校验失败";
        
        if (e instanceof MethodArgumentNotValidException) {
            MethodArgumentNotValidException ex = (MethodArgumentNotValidException) e;
            if (ex.getBindingResult().hasErrors()) {
                message = ex.getBindingResult().getAllErrors().get(0).getDefaultMessage();
            }
        } else if (e instanceof BindException) {
            BindException ex = (BindException) e;
            if (ex.getBindingResult().hasErrors()) {
                message = ex.getBindingResult().getAllErrors().get(0).getDefaultMessage();
            }
        }
        
        // 脱敏日志
        String safeMessage = EncryptUtil.desensitizeLog(message);
        log.error("[参数校验异常] traceId={}, message={}", traceId, safeMessage);
        return Result.error(ErrorCode.PARAM_VALIDATION_ERROR, message);
    }

    /**
     * 处理数据库异常
     */
    @ExceptionHandler({SQLException.class, DataAccessException.class})
    public Result<?> handleDatabaseException(Exception e) {
        String traceId = MDC.get("traceId");
        String errorMsg = e.getMessage();
        // 脱敏SQL错误信息
        String safeErrorMsg = errorMsg != null ? EncryptUtil.desensitizeLog(errorMsg) : "";
        log.error("[数据库异常] traceId={}, error={}", traceId, safeErrorMsg, e);
        
        // 判断具体的数据库错误类型
        if (e instanceof DuplicateKeyException) {
            return Result.error(ErrorCode.DB_DUPLICATE_KEY, "数据重复，请检查唯一键约束");
        }
        
        if (errorMsg != null) {
            if (errorMsg.contains("timeout") || errorMsg.contains("超时")) {
                return Result.error(ErrorCode.DB_QUERY_TIMEOUT);
            } else if (errorMsg.contains("connect") || errorMsg.contains("连接")) {
                return Result.error(ErrorCode.DB_CONNECTION_FAILED);
            } else if (errorMsg.contains("constraint") || errorMsg.contains("约束")) {
                return Result.error(ErrorCode.DB_CONSTRAINT_VIOLATION);
            }
        }
        
        return Result.error(ErrorCode.DB_ERROR, "数据库操作失败: " + (errorMsg != null ? errorMsg : "未知错误"));
    }

    /**
     * 处理API调用异常
     */
    @ExceptionHandler({RestClientException.class, ResourceAccessException.class})
    public Result<?> handleApiException(Exception e) {
        String traceId = MDC.get("traceId");
        String errorMsg = e.getMessage();
        // 脱敏API错误信息
        String safeErrorMsg = errorMsg != null ? EncryptUtil.desensitizeLog(errorMsg) : "";
        log.error("[API调用异常] traceId={}, error={}", traceId, safeErrorMsg, e);
        
        if (e instanceof ResourceAccessException) {
            if (errorMsg != null && (errorMsg.contains("timeout") || errorMsg.contains("超时"))) {
                return Result.error(ErrorCode.API_TIMEOUT, "API调用超时");
            }
            return Result.error(ErrorCode.API_NETWORK_ERROR, "网络连接失败");
        }
        
        return Result.error(ErrorCode.API_ERROR, "API调用失败: " + errorMsg);
    }

    /**
     * 处理系统异常
     */
    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        String traceId = MDC.get("traceId");
        String errorMsg = e.getMessage();
        // 脱敏系统错误信息
        String safeErrorMsg = errorMsg != null ? EncryptUtil.desensitizeLog(errorMsg) : "";
        log.error("[系统异常] traceId={}, error={}", traceId, safeErrorMsg, e);
        
        // 如果是RuntimeException且有有意义的错误信息，直接返回
        if (e instanceof RuntimeException && errorMsg != null && !errorMsg.isEmpty()) {
            return Result.error(ErrorCode.INTERNAL_ERROR, errorMsg);
        }
        
        return Result.error(ErrorCode.SYSTEM_ERROR);
    }
}
