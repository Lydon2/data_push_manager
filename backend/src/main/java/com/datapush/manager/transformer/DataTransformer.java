package com.datapush.manager.transformer;

import com.datapush.manager.utils.TransformFunctionUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 数据转换器
 * 用于字段映射和转换处理
 * 支持跨数据库类型映射和统一的空值处理
 */
@Slf4j
@Component
public class DataTransformer {
    
    // 跨数据库类型映射表（MySQL -> PostgreSQL/Oracle/SQLServer/KingBase）
    private static final Map<String, Map<String, String>> TYPE_MAPPING = new HashMap<>();
    
    static {
        // MySQL -> PostgreSQL
        Map<String, String> mysqlToPg = new HashMap<>();
        mysqlToPg.put("INT", "INTEGER");
        mysqlToPg.put("TINYINT", "SMALLINT");
        mysqlToPg.put("BIGINT", "BIGINT");
        mysqlToPg.put("VARCHAR", "VARCHAR");
        mysqlToPg.put("TEXT", "TEXT");
        mysqlToPg.put("DATETIME", "TIMESTAMP");
        mysqlToPg.put("DATE", "DATE");
        mysqlToPg.put("DECIMAL", "DECIMAL");
        mysqlToPg.put("DOUBLE", "DOUBLE PRECISION");
        mysqlToPg.put("FLOAT", "REAL");
        TYPE_MAPPING.put("MYSQL_TO_POSTGRESQL", mysqlToPg);
        
        // MySQL -> Oracle
        Map<String, String> mysqlToOracle = new HashMap<>();
        mysqlToOracle.put("INT", "NUMBER(10)");
        mysqlToOracle.put("BIGINT", "NUMBER(19)");
        mysqlToOracle.put("VARCHAR", "VARCHAR2");
        mysqlToOracle.put("TEXT", "CLOB");
        mysqlToOracle.put("DATETIME", "TIMESTAMP");
        mysqlToOracle.put("DATE", "DATE");
        mysqlToOracle.put("DECIMAL", "NUMBER");
        mysqlToOracle.put("DOUBLE", "BINARY_DOUBLE");
        TYPE_MAPPING.put("MYSQL_TO_ORACLE", mysqlToOracle);
        
        // MySQL -> SQLServer
        Map<String, String> mysqlToSqlServer = new HashMap<>();
        mysqlToSqlServer.put("INT", "INT");
        mysqlToSqlServer.put("BIGINT", "BIGINT");
        mysqlToSqlServer.put("VARCHAR", "NVARCHAR");
        mysqlToSqlServer.put("TEXT", "NVARCHAR(MAX)");
        mysqlToSqlServer.put("DATETIME", "DATETIME2");
        mysqlToSqlServer.put("DATE", "DATE");
        mysqlToSqlServer.put("DECIMAL", "DECIMAL");
        mysqlToSqlServer.put("DOUBLE", "FLOAT");
        TYPE_MAPPING.put("MYSQL_TO_SQLSERVER", mysqlToSqlServer);
        
        // MySQL -> KingBase (金仓基于 PostgreSQL)
        TYPE_MAPPING.put("MYSQL_TO_KINGBASE", mysqlToPg);
        
        // MySQL -> DM (达梦数据库)
        Map<String, String> mysqlToDm = new HashMap<>();
        mysqlToDm.put("INT", "INTEGER");
        mysqlToDm.put("TINYINT", "TINYINT");
        mysqlToDm.put("BIGINT", "BIGINT");
        mysqlToDm.put("VARCHAR", "VARCHAR");
        mysqlToDm.put("TEXT", "TEXT");
        mysqlToDm.put("DATETIME", "TIMESTAMP");
        mysqlToDm.put("DATE", "DATE");
        mysqlToDm.put("DECIMAL", "DECIMAL");
        mysqlToDm.put("DOUBLE", "DOUBLE");
        mysqlToDm.put("FLOAT", "FLOAT");
        TYPE_MAPPING.put("MYSQL_TO_DM", mysqlToDm);
    }
    
    /**
     * 应用函数转换
     * @param value 源值
     * @param function 函数表达式
     * @return 转换后的值
     */
    public String applyFunction(Object value, String function) {
        if (value == null) {
            return null;
        }
        
        if (function == null || function.trim().isEmpty()) {
            return String.valueOf(value);
        }
        
        try {
            Object result = TransformFunctionUtil.applyFunction(value, function);
            return result != null ? String.valueOf(result) : null;
        } catch (Exception e) {
            log.error("函数转换失败: function={}, value={}, error={}", function, value, e.getMessage());
            throw new RuntimeException("函数转换失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 跨数据库类型映射
     * @param sourceDbType 源数据库类型
     * @param targetDbType 目标数据库类型
     * @param sourceFieldType 源字段类型
     * @return 目标字段类型
     */
    public String mapDataType(String sourceDbType, String targetDbType, String sourceFieldType) {
        if (sourceDbType == null || targetDbType == null || sourceFieldType == null) {
            return sourceFieldType;
        }
        
        // 同类型数据库，不需要转换
        if (sourceDbType.equalsIgnoreCase(targetDbType)) {
            return sourceFieldType;
        }
        
        String mappingKey = sourceDbType.toUpperCase() + "_TO_" + targetDbType.toUpperCase();
        Map<String, String> mapping = TYPE_MAPPING.get(mappingKey);
        
        if (mapping == null) {
            log.warn("未找到类型映射: {} -> {}, 使用原始类型", sourceDbType, targetDbType);
            return sourceFieldType;
        }
        
        String upperType = sourceFieldType.toUpperCase();
        // 提取基础类型（去除长度等信息）
        String baseType = upperType.split("\\(")[0].trim();
        
        String targetType = mapping.get(baseType);
        if (targetType != null) {
            log.debug("类型映射: {} ({}) -> {} ({})", sourceFieldType, sourceDbType, targetType, targetDbType);
            return targetType;
        }
        
        log.warn("未找到类型 '{}' 的映射，使用原始类型", sourceFieldType);
        return sourceFieldType;
    }
    
    /**
     * 类型转换和默认值处理
     * @param value 原始值
     * @param targetType 目标类型
     * @param defaultValue 默认值
     * @return 转换后的值
     */
    public Object convertType(Object value, String targetType, Object defaultValue) {
        // 空值处理
        if (value == null || (value instanceof String && ((String) value).trim().isEmpty())) {
            return defaultValue;
        }
        
        if (targetType == null || "AUTO".equalsIgnoreCase(targetType)) {
            return value;
        }
        
        try {
            switch (targetType.toUpperCase()) {
                case "STRING":
                case "VARCHAR":
                case "TEXT":
                    return String.valueOf(value);
                    
                case "INTEGER":
                case "INT":
                    if (value instanceof Number) {
                        return ((Number) value).intValue();
                    }
                    return Integer.parseInt(String.valueOf(value).trim());
                    
                case "LONG":
                case "BIGINT":
                    if (value instanceof Number) {
                        return ((Number) value).longValue();
                    }
                    return Long.parseLong(String.valueOf(value).trim());
                    
                case "DOUBLE":
                case "FLOAT":
                    if (value instanceof Number) {
                        return ((Number) value).doubleValue();
                    }
                    return Double.parseDouble(String.valueOf(value).trim());
                    
                case "DECIMAL":
                case "NUMERIC":
                    if (value instanceof BigDecimal) {
                        return value;
                    }
                    return new BigDecimal(String.valueOf(value).trim());
                    
                case "BOOLEAN":
                    if (value instanceof Boolean) {
                        return value;
                    }
                    String strValue = String.valueOf(value).trim().toLowerCase();
                    return "true".equals(strValue) || "1".equals(strValue) || "yes".equals(strValue);
                    
                case "DATE":
                    if (value instanceof LocalDate) {
                        return value;
                    }
                    if (value instanceof java.util.Date) {
                        return ((java.util.Date) value).toInstant()
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDate();
                    }
                    return LocalDate.parse(String.valueOf(value).trim());
                    
                case "DATETIME":
                case "TIMESTAMP":
                    if (value instanceof LocalDateTime) {
                        return value;
                    }
                    if (value instanceof Timestamp) {
                        return ((Timestamp) value).toLocalDateTime();
                    }
                    if (value instanceof java.util.Date) {
                        return ((java.util.Date) value).toInstant()
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDateTime();
                    }
                    // 尝试常见格式
                    String dateStr = String.valueOf(value).trim();
                    if (dateStr.length() == 19) {
                        return LocalDateTime.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                    }
                    return LocalDateTime.parse(dateStr);
                    
                default:
                    log.warn("未知目标类型: {}, 返回原值", targetType);
                    return value;
            }
        } catch (Exception e) {
            log.error("类型转换失败: value={}, targetType={}, error={}", value, targetType, e.getMessage());
            // 转换失败，返回默认值
            return defaultValue != null ? defaultValue : value;
        }
    }
    
    /**
     * 统一的空值处理策略
     * @param value 值
     * @param strategy 策略: KEEP(保留), DEFAULT(默认值), SKIP(跳过字段), REMOVE(移除整行), NULL(设为null)
     * @param defaultValue 默认值
     * @param targetType 目标类型（用于默认值的类型转换）
     * @return 处理后的值
     */
    public Object handleNull(Object value, String strategy, Object defaultValue, String targetType) {
        // 非空值，直接返回
        if (value != null && !(value instanceof String && ((String) value).trim().isEmpty())) {
            return value;
        }
        
        if (strategy == null || strategy.trim().isEmpty()) {
            strategy = "KEEP";
        }
        
        switch (strategy.toUpperCase()) {
            case "KEEP":
                // 保留空值
                return value;
                
            case "DEFAULT":
                // 使用默认值，并按目标类型转换
                if (defaultValue == null) {
                    return getTypeDefaultValue(targetType);
                }
                return targetType != null ? convertType(defaultValue, targetType, defaultValue) : defaultValue;
                
            case "SKIP":
                // 跳过字段（返回null作为标记）
                return null;
                
            case "REMOVE":
                // 移除整行（抛出异常作为标记）
                throw new RuntimeException("REMOVE_ROW");
                
            case "NULL":
                // 强制设置为null
                return null;
                
            default:
                log.warn("未知的空值处理策略: {}, 使用KEEP", strategy);
                return value;
        }
    }
    
    /**
     * 获取类型的默认值
     */
    private Object getTypeDefaultValue(String type) {
        if (type == null) {
            return "";
        }
        
        switch (type.toUpperCase()) {
            case "INTEGER":
            case "INT":
            case "LONG":
            case "BIGINT":
                return 0;
            case "DOUBLE":
            case "FLOAT":
            case "DECIMAL":
            case "NUMERIC":
                return 0.0;
            case "BOOLEAN":
                return false;
            case "DATE":
                return LocalDate.now();
            case "DATETIME":
            case "TIMESTAMP":
                return LocalDateTime.now();
            default:
                return "";
        }
    }
}
