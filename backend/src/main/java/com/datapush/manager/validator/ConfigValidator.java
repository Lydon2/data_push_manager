package com.datapush.manager.validator;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;

/**
 * 配置校验器
 * 统一校验 extraConfig、字典映射配置等JSON格式配置
 *
 * @author Data Push Team
 * @since 2024-11-24
 */
@Slf4j
@Component
public class ConfigValidator {

    /**
     * 校验连接器额外配置
     * @param extraConfig 额外配置JSON
     * @param connectorType 连接器类型
     * @return 校验结果
     */
    public ValidationResult validateConnectorExtraConfig(String extraConfig, String connectorType) {
        if (extraConfig == null || extraConfig.trim().isEmpty()) {
            return ValidationResult.success();
        }
        
        try {
            JSONObject config = JSON.parseObject(extraConfig);
            
            if ("API".equalsIgnoreCase(connectorType)) {
                return validateApiExtraConfig(config);
            } else if ("DATABASE".equalsIgnoreCase(connectorType)) {
                return validateDatabaseExtraConfig(config);
            }
            
            return ValidationResult.success();
            
        } catch (Exception e) {
            return ValidationResult.error("配置格式错误: " + e.getMessage());
        }
    }
    
    /**
     * 校验API连接器额外配置
     */
    private ValidationResult validateApiExtraConfig(JSONObject config) {
        List<String> errors = new ArrayList<>();
        
        // 校验headers格式
        if (config.containsKey("headers")) {
            Object headers = config.get("headers");
            if (!(headers instanceof JSONObject)) {
                errors.add("headers必须是JSON对象格式");
            }
        }
        
        // 校验params格式
        if (config.containsKey("params")) {
            Object params = config.get("params");
            if (!(params instanceof JSONObject)) {
                errors.add("params必须是JSON对象格式");
            }
        }
        
        // 校验timeout
        if (config.containsKey("timeout")) {
            Object timeout = config.get("timeout");
            try {
                int timeoutValue = Integer.parseInt(String.valueOf(timeout));
                if (timeoutValue <= 0 || timeoutValue > 300000) {
                    errors.add("timeout必须在1-300000毫秒之间");
                }
            } catch (NumberFormatException e) {
                errors.add("timeout必须是数字");
            }
        }
        
        return errors.isEmpty() ? ValidationResult.success() : ValidationResult.error(String.join("; ", errors));
    }
    
    /**
     * 校验数据库连接器额外配置
     */
    private ValidationResult validateDatabaseExtraConfig(JSONObject config) {
        List<String> errors = new ArrayList<>();
        
        // 校验连接池配置
        if (config.containsKey("maxPoolSize")) {
            Object maxPoolSize = config.get("maxPoolSize");
            try {
                int size = Integer.parseInt(String.valueOf(maxPoolSize));
                if (size <= 0 || size > 100) {
                    errors.add("maxPoolSize必须在1-100之间");
                }
            } catch (NumberFormatException e) {
                errors.add("maxPoolSize必须是数字");
            }
        }
        
        return errors.isEmpty() ? ValidationResult.success() : ValidationResult.error(String.join("; ", errors));
    }
    
    /**
     * 校验任务源配置
     */
    public ValidationResult validateSourceConfig(String sourceConfig) {
        if (sourceConfig == null || sourceConfig.trim().isEmpty()) {
            return ValidationResult.error("源端配置不能为空");
        }
        
        try {
            JSONObject config = JSON.parseObject(sourceConfig);
            List<String> errors = new ArrayList<>();
            
            // 必填字段校验
            if (!config.containsKey("sql") || config.getString("sql").trim().isEmpty()) {
                errors.add("SQL语句不能为空");
            }
            
            // 校验流式模式配置
            if (config.getBooleanValue("streamMode")) {
                if (config.containsKey("fetchBatchSize")) {
                    Integer batchSize = config.getInteger("fetchBatchSize");
                    if (batchSize != null && (batchSize <= 0 || batchSize > 10000)) {
                        errors.add("fetchBatchSize必须在1-10000之间");
                    }
                }
            }
            
            return errors.isEmpty() ? ValidationResult.success() : ValidationResult.error(String.join("; ", errors));
            
        } catch (Exception e) {
            return ValidationResult.error("源端配置格式错误: " + e.getMessage());
        }
    }
    
    /**
     * 校验任务目标配置
     */
    public ValidationResult validateTargetConfig(String targetConfig) {
        if (targetConfig == null || targetConfig.trim().isEmpty()) {
            return ValidationResult.error("目标端配置不能为空");
        }
        
        try {
            JSONObject config = JSON.parseObject(targetConfig);
            List<String> errors = new ArrayList<>();
            
            // 必填字段校验
            if (!config.containsKey("tableName") || config.getString("tableName").trim().isEmpty()) {
                errors.add("目标表名不能为空");
            }
            
            // 校验写入模式
            if (config.containsKey("writeMode")) {
                String writeMode = config.getString("writeMode");
                if (!Arrays.asList("INSERT", "UPDATE", "REPLACE", "UPSERT").contains(writeMode)) {
                    errors.add("writeMode必须是INSERT、UPDATE、REPLACE或UPSERT之一");
                }
            }
            
            // 校验批次大小
            if (config.containsKey("batchSize")) {
                Integer batchSize = config.getInteger("batchSize");
                if (batchSize != null && (batchSize <= 0 || batchSize > 10000)) {
                    errors.add("batchSize必须在1-10000之间");
                }
            }
            
            // 校验最大重试次数
            if (config.containsKey("maxRetries")) {
                Integer maxRetries = config.getInteger("maxRetries");
                if (maxRetries != null && (maxRetries < 0 || maxRetries > 10)) {
                    errors.add("maxRetries必须在0-10之间");
                }
            }
            
            return errors.isEmpty() ? ValidationResult.success() : ValidationResult.error(String.join("; ", errors));
            
        } catch (Exception e) {
            return ValidationResult.error("目标端配置格式错误: " + e.getMessage());
        }
    }
    
    /**
     * 校验字典映射配置
     */
    public ValidationResult validateDictMappingConfig(String mappingConfig) {
        if (mappingConfig == null || mappingConfig.trim().isEmpty()) {
            return ValidationResult.success(); // 字典映射配置可选
        }
        
        try {
            JSONObject config = JSON.parseObject(mappingConfig);
            
            // 校验必须包含sourceField和targetField
            if (!config.containsKey("sourceField") || !config.containsKey("targetField")) {
                return ValidationResult.error("字典映射配置必须包含sourceField和targetField");
            }
            
            return ValidationResult.success();
            
        } catch (Exception e) {
            return ValidationResult.error("字典映射配置格式错误: " + e.getMessage());
        }
    }
    
    /**
     * 校验Cron表达式
     */
    public ValidationResult validateCronExpression(String cronExpression) {
        if (cronExpression == null || cronExpression.trim().isEmpty()) {
            return ValidationResult.error("Cron表达式不能为空");
        }
        
        // 简单的Cron表达式格式校验（6或7个字段）
        String[] parts = cronExpression.trim().split("\\s+");
        if (parts.length < 6 || parts.length > 7) {
            return ValidationResult.error("Cron表达式格式错误，应包含6或7个字段");
        }
        
        // 校验每个字段的基本格式
        try {
            // 这里可以使用Quartz的CronExpression来详细校验
            // 为简化，这里只做基本格式检查
            return ValidationResult.success();
        } catch (Exception e) {
            return ValidationResult.error("Cron表达式格式错误: " + e.getMessage());
        }
    }
    
    /**
     * 校验邮箱格式
     */
    public ValidationResult validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return ValidationResult.error("邮箱不能为空");
        }
        
        String emailRegex = "^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$";
        if (!Pattern.matches(emailRegex, email)) {
            return ValidationResult.error("邮箱格式不正确");
        }
        
        return ValidationResult.success();
    }
    
    /**
     * 校验结果类
     */
    public static class ValidationResult {
        private boolean valid;
        private String errorMessage;
        
        private ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }
        
        public static ValidationResult success() {
            return new ValidationResult(true, null);
        }
        
        public static ValidationResult error(String errorMessage) {
            return new ValidationResult(false, errorMessage);
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }
    }
}
