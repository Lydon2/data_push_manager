package com.datapush.manager.common;

/**
 * 错误码枚举
 * 规范：
 * - 1000-1999: 参数校验相关
 * - 2000-2999: 业务逻辑相关
 * - 3000-3999: 数据库相关
 * - 4000-4999: 外部API相关
 * - 5000-5999: 系统内部错误
 *
 * @author Data Push Team
 * @since 2024-11-24
 */
public enum ErrorCode {

    // ========== 通用错误 ==========
    SUCCESS(200, "操作成功"),
    SYSTEM_ERROR(500, "系统异常，请联系管理员"),
    
    // ========== 参数校验错误 1000-1999 ==========
    PARAM_VALIDATION_ERROR(1000, "参数校验失败"),
    PARAM_MISSING(1001, "缺少必填参数"),
    PARAM_FORMAT_ERROR(1002, "参数格式错误"),
    PARAM_VALUE_INVALID(1003, "参数值无效"),
    
    // ========== 业务逻辑错误 2000-2999 ==========
    BUSINESS_ERROR(2000, "业务处理失败"),
    RESOURCE_NOT_FOUND(2001, "资源不存在"),
    RESOURCE_ALREADY_EXISTS(2002, "资源已存在"),
    OPERATION_NOT_ALLOWED(2003, "操作不允许"),
    STATUS_INVALID(2004, "状态不合法"),
    
    // ========== 连接器相关 2100-2199 ==========
    CONNECTOR_NOT_FOUND(2100, "连接器不存在"),
    CONNECTOR_TEST_FAILED(2101, "连接器测试失败"),
    CONNECTOR_TYPE_NOT_SUPPORTED(2102, "连接器类型不支持"),
    
    // ========== 任务相关 2200-2299 ==========
    TASK_NOT_FOUND(2200, "任务不存在"),
    TASK_ALREADY_RUNNING(2201, "任务正在运行中"),
    TASK_CONFIG_INVALID(2202, "任务配置无效"),
    TASK_EXECUTE_FAILED(2203, "任务执行失败"),
    
    // ========== 字段映射相关 2300-2399 ==========
    FIELD_MAPPING_NOT_FOUND(2300, "字段映射不存在"),
    FIELD_MAPPING_DUPLICATE(2301, "字段映射重复"),
    FIELD_TYPE_MISMATCH(2302, "字段类型不匹配"),
    
    // ========== 数据库错误 3000-3999 ==========
    DB_ERROR(3000, "数据库操作失败"),
    DB_CONNECTION_FAILED(3001, "数据库连接失败"),
    DB_QUERY_TIMEOUT(3002, "数据库查询超时"),
    DB_DUPLICATE_KEY(3003, "数据重复"),
    DB_CONSTRAINT_VIOLATION(3004, "数据约束违反"),
    
    // ========== 外部API错误 4000-4999 ==========
    API_ERROR(4000, "API调用失败"),
    API_TIMEOUT(4001, "API调用超时"),
    API_AUTH_FAILED(4002, "API认证失败"),
    API_RESPONSE_INVALID(4003, "API响应格式错误"),
    API_NETWORK_ERROR(4004, "网络连接失败"),
    
    // ========== 系统内部错误 5000-5999 ==========
    INTERNAL_ERROR(5000, "系统内部错误"),
    CONFIG_ERROR(5001, "配置错误"),
    FILE_IO_ERROR(5002, "文件读写错误"),
    JSON_PARSE_ERROR(5003, "JSON解析失败"),
    THREAD_INTERRUPTED(5004, "线程中断"),
    ;

    private final Integer code;
    private final String message;

    ErrorCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public Integer getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
