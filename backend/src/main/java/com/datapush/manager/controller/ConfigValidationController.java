package com.datapush.manager.controller;

import com.datapush.manager.common.Result;
import com.datapush.manager.validator.ConfigValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 配置校验控制器
 * 提供前端实时配置校验接口
 *
 * @author Data Push Team
 * @since 2024-11-24
 */
@Slf4j
@RestController
@RequestMapping("/v1/validate")
public class ConfigValidationController {

    @Autowired
    private ConfigValidator configValidator;

    /**
     * 校验连接器额外配置
     */
    @PostMapping("/connector-extra-config")
    public Result<Map<String, Object>> validateConnectorExtraConfig(@RequestBody Map<String, String> request) {
        String extraConfig = request.get("extraConfig");
        String connectorType = request.get("connectorType");
        
        ConfigValidator.ValidationResult result = 
            configValidator.validateConnectorExtraConfig(extraConfig, connectorType);
        
        Map<String, Object> response = new HashMap<>();
        response.put("valid", result.isValid());
        response.put("errorMessage", result.getErrorMessage());
        
        return Result.success(response);
    }

    /**
     * 校验任务源配置
     */
    @PostMapping("/source-config")
    public Result<Map<String, Object>> validateSourceConfig(@RequestBody Map<String, String> request) {
        String sourceConfig = request.get("sourceConfig");
        
        ConfigValidator.ValidationResult result = 
            configValidator.validateSourceConfig(sourceConfig);
        
        Map<String, Object> response = new HashMap<>();
        response.put("valid", result.isValid());
        response.put("errorMessage", result.getErrorMessage());
        
        return Result.success(response);
    }

    /**
     * 校验任务目标配置
     */
    @PostMapping("/target-config")
    public Result<Map<String, Object>> validateTargetConfig(@RequestBody Map<String, String> request) {
        String targetConfig = request.get("targetConfig");
        
        ConfigValidator.ValidationResult result = 
            configValidator.validateTargetConfig(targetConfig);
        
        Map<String, Object> response = new HashMap<>();
        response.put("valid", result.isValid());
        response.put("errorMessage", result.getErrorMessage());
        
        return Result.success(response);
    }

    /**
     * 校验Cron表达式
     */
    @PostMapping("/cron-expression")
    public Result<Map<String, Object>> validateCronExpression(@RequestBody Map<String, String> request) {
        String cronExpression = request.get("cronExpression");
        
        ConfigValidator.ValidationResult result = 
            configValidator.validateCronExpression(cronExpression);
        
        Map<String, Object> response = new HashMap<>();
        response.put("valid", result.isValid());
        response.put("errorMessage", result.getErrorMessage());
        
        return Result.success(response);
    }

    /**
     * 校验邮箱格式
     */
    @PostMapping("/email")
    public Result<Map<String, Object>> validateEmail(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        
        ConfigValidator.ValidationResult result = 
            configValidator.validateEmail(email);
        
        Map<String, Object> response = new HashMap<>();
        response.put("valid", result.isValid());
        response.put("errorMessage", result.getErrorMessage());
        
        return Result.success(response);
    }
}
