package com.datapush.manager.validator;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 配置校验器单元测试
 */
class ConfigValidatorTest {

    private ConfigValidator validator;

    @BeforeEach
    void setUp() {
        validator = new ConfigValidator();
    }

    @Test
    void testValidateApiExtraConfig_Valid() {
        JSONObject config = new JSONObject();
        config.put("headers", new JSONObject());
        config.put("params", new JSONObject());
        config.put("timeout", 5000);
        
        ConfigValidator.ValidationResult result = 
            validator.validateConnectorExtraConfig(config.toJSONString(), "API");
        
        assertTrue(result.isValid());
        assertNull(result.getErrorMessage());
    }

    @Test
    void testValidateApiExtraConfig_InvalidTimeout() {
        JSONObject config = new JSONObject();
        config.put("timeout", 500000); // 超过最大值
        
        ConfigValidator.ValidationResult result = 
            validator.validateConnectorExtraConfig(config.toJSONString(), "API");
        
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("timeout"));
    }

    @Test
    void testValidateApiExtraConfig_InvalidHeadersType() {
        JSONObject config = new JSONObject();
        config.put("headers", "invalid_string"); // 应该是JSON对象
        
        ConfigValidator.ValidationResult result = 
            validator.validateConnectorExtraConfig(config.toJSONString(), "API");
        
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("headers"));
    }

    @Test
    void testValidateSourceConfig_Valid() {
        JSONObject config = new JSONObject();
        config.put("sql", "SELECT * FROM users");
        config.put("streamMode", true);
        config.put("fetchBatchSize", 1000);
        
        ConfigValidator.ValidationResult result = 
            validator.validateSourceConfig(config.toJSONString());
        
        assertTrue(result.isValid());
    }

    @Test
    void testValidateSourceConfig_MissingSql() {
        JSONObject config = new JSONObject();
        config.put("streamMode", true);
        
        ConfigValidator.ValidationResult result = 
            validator.validateSourceConfig(config.toJSONString());
        
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("SQL"));
    }

    @Test
    void testValidateSourceConfig_InvalidBatchSize() {
        JSONObject config = new JSONObject();
        config.put("sql", "SELECT * FROM users");
        config.put("streamMode", true);
        config.put("fetchBatchSize", 20000); // 超过最大值
        
        ConfigValidator.ValidationResult result = 
            validator.validateSourceConfig(config.toJSONString());
        
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("fetchBatchSize"));
    }

    @Test
    void testValidateTargetConfig_Valid() {
        JSONObject config = new JSONObject();
        config.put("tableName", "target_table");
        config.put("writeMode", "INSERT");
        config.put("batchSize", 500);
        config.put("maxRetries", 3);
        
        ConfigValidator.ValidationResult result = 
            validator.validateTargetConfig(config.toJSONString());
        
        assertTrue(result.isValid());
    }

    @Test
    void testValidateTargetConfig_MissingTableName() {
        JSONObject config = new JSONObject();
        config.put("writeMode", "INSERT");
        
        ConfigValidator.ValidationResult result = 
            validator.validateTargetConfig(config.toJSONString());
        
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("表名"));
    }

    @Test
    void testValidateTargetConfig_InvalidWriteMode() {
        JSONObject config = new JSONObject();
        config.put("tableName", "target_table");
        config.put("writeMode", "INVALID_MODE");
        
        ConfigValidator.ValidationResult result = 
            validator.validateTargetConfig(config.toJSONString());
        
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("writeMode"));
    }

    @Test
    void testValidateTargetConfig_InvalidBatchSize() {
        JSONObject config = new JSONObject();
        config.put("tableName", "target_table");
        config.put("batchSize", 50000); // 超过最大值
        
        ConfigValidator.ValidationResult result = 
            validator.validateTargetConfig(config.toJSONString());
        
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("batchSize"));
    }

    @Test
    void testValidateCronExpression_Valid() {
        ConfigValidator.ValidationResult result = 
            validator.validateCronExpression("0 0 12 * * ?");
        
        assertTrue(result.isValid());
    }

    @Test
    void testValidateCronExpression_Invalid() {
        ConfigValidator.ValidationResult result = 
            validator.validateCronExpression("invalid cron");
        
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("Cron"));
    }

    @Test
    void testValidateEmail_Valid() {
        ConfigValidator.ValidationResult result = 
            validator.validateEmail("test@example.com");
        
        assertTrue(result.isValid());
    }

    @Test
    void testValidateEmail_Invalid() {
        ConfigValidator.ValidationResult result = 
            validator.validateEmail("invalid-email");
        
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("邮箱"));
    }

    @Test
    void testValidateDictMappingConfig_Valid() {
        JSONObject config = new JSONObject();
        config.put("sourceField", "source_col");
        config.put("targetField", "target_col");
        
        ConfigValidator.ValidationResult result = 
            validator.validateDictMappingConfig(config.toJSONString());
        
        assertTrue(result.isValid());
    }

    @Test
    void testValidateDictMappingConfig_MissingFields() {
        JSONObject config = new JSONObject();
        config.put("sourceField", "source_col");
        
        ConfigValidator.ValidationResult result = 
            validator.validateDictMappingConfig(config.toJSONString());
        
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("sourceField"));
    }
}
