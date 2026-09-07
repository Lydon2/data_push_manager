package com.datapush.manager.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.datapush.manager.common.BusinessException;
import com.datapush.manager.common.Result;
import com.datapush.manager.dto.TableInfo;
import com.datapush.manager.entity.AuxiliaryDatasource;
import com.datapush.manager.entity.FieldMapping;
import com.datapush.manager.entity.TaskConfig;
import com.datapush.manager.entity.TaskTarget;
import com.datapush.manager.service.FieldMappingService;
import com.datapush.manager.service.TaskConfigService;
import com.datapush.manager.service.DataPreviewService;
import com.datapush.manager.service.TaskTargetService;
import com.datapush.manager.service.AuxiliaryDatasourceService;
import com.datapush.manager.validator.ConfigValidator;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 任务配置控制器
 * 支持配置校验
 *
 * @author Data Push Team
 * @since 2024-11-20
 */
@Slf4j
@RestController
@RequestMapping("/v1/task")
public class TaskConfigController {

    @Autowired
    private TaskConfigService taskConfigService;

    @Autowired
    private FieldMappingService fieldMappingService;

    @Autowired
    private DataPreviewService dataPreviewService;
    
    @Autowired
    private ConfigValidator configValidator;
    
    @Autowired
    private TaskTargetService taskTargetService;
    
    @Autowired
    private AuxiliaryDatasourceService auxiliaryDatasourceService;

    /**
     * 分页查询任务列表
     */
    @GetMapping("/page")
    public Result<IPage<TaskConfig>> page(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String taskName,
            @RequestParam(required = false) String syncMode,
            @RequestParam(required = false) String scheduleType) {
        
        Page<TaskConfig> page = new Page<>(current, size);
        LambdaQueryWrapper<TaskConfig> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(taskName)) {
            wrapper.like(TaskConfig::getTaskName, taskName);
        }
        if (StringUtils.hasText(syncMode)) {
            wrapper.eq(TaskConfig::getSyncMode, syncMode);
        }
        if (StringUtils.hasText(scheduleType)) {
            wrapper.eq(TaskConfig::getScheduleType, scheduleType);
        }
        
        wrapper.orderByDesc(TaskConfig::getCreateTime);
        IPage<TaskConfig> result = taskConfigService.page(page, wrapper);
        
        return Result.success(result);
    }

    /**
     * 根据ID查询任务详情
     */
    @GetMapping("/{id}")
    public Result<TaskConfig> getById(@PathVariable Long id) {
        TaskConfig taskConfig = taskConfigService.getById(id);
        return Result.success(taskConfig);
    }
    
    /**
     * 根据ID查询任务详情（支持多目标，包含targets和fieldMappings）
     */
    @GetMapping("/{id}/detail")
    public Result<Map<String, Object>> getDetailById(@PathVariable Long id) {
        TaskConfig taskConfig = taskConfigService.getById(id);
        if (taskConfig == null) {
            return Result.error("任务不存在");
        }
        
        // 获取目标配置
        List<TaskTarget> targets = taskTargetService.getEnabledTargetsByTaskId(id);
        
        // 获取字段映射
        List<FieldMapping> fieldMappings = fieldMappingService.listByTaskId(id);
        
        // 组装返回数据
        Map<String, Object> result = new HashMap<>();
        result.put("task", taskConfig);
        result.put("targets", targets);
        result.put("fieldMappings", fieldMappings);
        
        return Result.success(result);
    }

    /**
     * 新增任务
     */
    @PostMapping
    public Result<?> save(@Valid @RequestBody TaskConfig taskConfig) {
        // 校验源端配置
        ConfigValidator.ValidationResult sourceResult = 
            configValidator.validateSourceConfig(taskConfig.getSourceConfig());
        if (!sourceResult.isValid()) {
            throw new BusinessException("源端配置校验失败: " + sourceResult.getErrorMessage());
        }
        
        // 校验目标端配置（单目标模式才校验任务级targetConfig）
        if (taskConfig.getMultiTarget() == null || taskConfig.getMultiTarget() != 1) {
            ConfigValidator.ValidationResult targetResult = 
                configValidator.validateTargetConfig(taskConfig.getTargetConfig());
            if (!targetResult.isValid()) {
                throw new BusinessException("目标端配置校验失败: " + targetResult.getErrorMessage());
            }
        }
        
        // 校验Cron表达式（如果是定时调度）
        if ("CRON".equals(taskConfig.getScheduleType()) && 
            StringUtils.hasText(taskConfig.getCronExpression())) {
            ConfigValidator.ValidationResult cronResult = 
                configValidator.validateCronExpression(taskConfig.getCronExpression());
            if (!cronResult.isValid()) {
                throw new BusinessException("Cron表达式校验失败: " + cronResult.getErrorMessage());
            }
        }
        
        boolean success = taskConfigService.save(taskConfig);
        
        if (success) {
            // 如果任务状态为启用且是定时任务，自动启动调度
            if (taskConfig.getStatus() != null && taskConfig.getStatus() == 1 
                && "CRON".equals(taskConfig.getScheduleType())) {
                try {
                    taskConfigService.startSchedule(taskConfig.getId());
                    log.info("任务新增成功，定时调度已启动: taskId={}", taskConfig.getId());
                } catch (Exception e) {
                    log.warn("任务新增成功，但调度启动失败: taskId={}", taskConfig.getId(), e);
                }
            }
        }
        
        return success ? Result.success("新增成功", taskConfig.getId()) : Result.error("新增失败");
    }

    /**
     * 更新任务
     */
    @PutMapping
    public Result<?> update(@Valid @RequestBody TaskConfig taskConfig) {
        // 校验源端配置
        ConfigValidator.ValidationResult sourceResult = 
            configValidator.validateSourceConfig(taskConfig.getSourceConfig());
        if (!sourceResult.isValid()) {
            throw new BusinessException("源端配置校验失败: " + sourceResult.getErrorMessage());
        }
        
        // 校验目标端配置（单目标模式才校验任务级targetConfig）
        if (taskConfig.getMultiTarget() == null || taskConfig.getMultiTarget() != 1) {
            ConfigValidator.ValidationResult targetResult = 
                configValidator.validateTargetConfig(taskConfig.getTargetConfig());
            if (!targetResult.isValid()) {
                throw new BusinessException("目标端配置校验失败: " + targetResult.getErrorMessage());
            }
        }
        
        // 校验Cron表达式（如果是定时调度）
        if ("CRON".equals(taskConfig.getScheduleType()) && 
            StringUtils.hasText(taskConfig.getCronExpression())) {
            ConfigValidator.ValidationResult cronResult = 
                configValidator.validateCronExpression(taskConfig.getCronExpression());
            if (!cronResult.isValid()) {
                throw new BusinessException("Cron表达式校验失败: " + cronResult.getErrorMessage());
            }
        }
        
        // 获取旧状态
        TaskConfig oldTask = taskConfigService.getById(taskConfig.getId());
        Integer oldStatus = oldTask != null ? oldTask.getStatus() : 0;
        
        boolean success = taskConfigService.updateById(taskConfig);
        
        if (success) {
            // 如果是从禁用变为启用，且是定时任务，则启动调度
            if (oldStatus == 0 && taskConfig.getStatus() == 1 && "CRON".equals(taskConfig.getScheduleType())) {
                try {
                    taskConfigService.startSchedule(taskConfig.getId());
                    log.info("任务更新成功，定时调度已启动: taskId={}", taskConfig.getId());
                } catch (Exception e) {
                    log.warn("任务更新成功，但调度启动失败: taskId={}", taskConfig.getId(), e);
                }
            }
            // 如果是从启用变为禁用，且是定时任务，则停止调度
            else if (oldStatus == 1 && taskConfig.getStatus() == 0 && "CRON".equals(taskConfig.getScheduleType())) {
                try {
                    taskConfigService.stopSchedule(taskConfig.getId());
                    log.info("任务更新成功，定时调度已停止: taskId={}", taskConfig.getId());
                } catch (Exception e) {
                    log.warn("任务更新成功，但调度停止失败: taskId={}", taskConfig.getId(), e);
                }
            }
        }
        
        return success ? Result.success("更新成功") : Result.error("更新失败");
    }

    /**
     * 删除任务
     */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        // 先停止调度
        taskConfigService.stopSchedule(id);
        boolean success = taskConfigService.removeById(id);
        return success ? Result.success("删除成功") : Result.error("删除失败");
    }

    /**
     * 查询任务的字段映射
     */
    @GetMapping("/{taskId}/mappings")
    public Result<List<FieldMapping>> getMappings(@PathVariable Long taskId) {
        List<FieldMapping> mappings = fieldMappingService.listByTaskId(taskId);
        return Result.success(mappings);
    }

    /**
     * 保存任务的字段映射
     */
    @PostMapping("/{taskId}/mappings")
    public Result<?> saveMappings(@PathVariable Long taskId, @RequestBody List<FieldMapping> mappings) {
        boolean success = fieldMappingService.saveBatch(taskId, mappings);
        return success ? Result.success("保存成功") : Result.error("保存失败");
    }

    /**
     * 手动执行任务
     */
    @PostMapping("/{id}/execute")
    public Result<Long> execute(@PathVariable Long id) {
        Long logId = taskConfigService.executeTask(id, "MANUAL");
        return Result.success("任务已提交执行", logId);
    }

    /**
     * 启动任务调度
     */
    @PostMapping("/{id}/start")
    public Result<?> startSchedule(@PathVariable Long id) {
        boolean success = taskConfigService.startSchedule(id);
        return success ? Result.success("调度启动成功") : Result.error("调度启动失败");
    }

    /**
     * 停止任务调度
     */
    @PostMapping("/{id}/stop")
    public Result<?> stopSchedule(@PathVariable Long id) {
        boolean success = taskConfigService.stopSchedule(id);
        return success ? Result.success("调度已停止") : Result.error("调度停止失败");
    }

    /**
     * 获取连接器的表列表（包含注释）
     */
    @GetMapping("/connector/{connectorId}/tables-with-comment")
    public Result<List<TableInfo>> getTablesWithComment(@PathVariable Long connectorId) {
        List<TableInfo> tables = dataPreviewService.getTablesWithComment(connectorId);
        return Result.success(tables);
    }

    /**
     * 获取连接器的表列表
     */
    @GetMapping("/connector/{connectorId}/tables")
    public Result<List<String>> getTables(@PathVariable Long connectorId) {
        List<String> tables = dataPreviewService.getTables(connectorId);
        return Result.success(tables);
    }

    /**
     * 校验 SQL 语法（主/辅数据源 SQL 编辑器统一调用）
     */
    @PostMapping("/validate-sql")
    public Result<Map<String, Object>> validateSql(@RequestBody Map<String, Object> request) {
        Object connectorIdObj = request.get("connectorId");
        Object sqlObj = request.get("sql");
        if (connectorIdObj == null || sqlObj == null) {
            return Result.error("connectorId 或 sql 不能为空");
        }

        Long connectorId;
        try {
            connectorId = Long.valueOf(connectorIdObj.toString());
        } catch (NumberFormatException e) {
            return Result.error("connectorId 非法");
        }
        String sql = sqlObj.toString();

        Map<String, Object> result = dataPreviewService.validateSql(connectorId, sql);
        return Result.success(result);
    }

    /**
     * 预览SQL查询结果
     */
    @PostMapping("/preview")
    public Result<Map<String, Object>> previewData(@RequestBody Map<String, Object> request) {
        Long connectorId = Long.valueOf(request.get("connectorId").toString());
        String sql = request.get("sql").toString();
        Integer limit = request.containsKey("limit") ? 
            Integer.valueOf(request.get("limit").toString()) : 10;
        
        Map<String, Object> result = dataPreviewService.previewData(connectorId, sql, limit);
        return Result.success(result);
    }

    /**
     * 预览API数据
     */
    @PostMapping("/preview-api")
    public Result<Map<String, Object>> previewApiData(@RequestBody Map<String, Object> request) {
        Long connectorId = Long.valueOf(request.get("connectorId").toString());
        String apiPath = request.get("apiPath").toString();
        String apiMethod = request.get("apiMethod").toString();
        String dataPath = request.containsKey("dataPath") ? request.get("dataPath").toString() : "";
        Integer limit = request.containsKey("limit") ? 
            Integer.valueOf(request.get("limit").toString()) : 10;
        
        // 获取参数
        Map<String, String> params = new HashMap<>();
        if (request.containsKey("params") && request.get("params") != null) {
            try {
                params = (Map<String, String>) request.get("params");
            } catch (Exception e) {
                log.warn("解析params参数失败", e);
            }
        }
        
        // 获取请求头
        Map<String, String> headers = new HashMap<>();
        if (request.containsKey("headers") && request.get("headers") != null) {
            try {
                headers = (Map<String, String>) request.get("headers");
            } catch (Exception e) {
                log.warn("解析headers参数失败", e);
            }
        }
        
        // 获取请求体
        String bodyType = request.containsKey("bodyType") ? request.get("bodyType").toString() : "none";
        Object requestBody = request.get("requestBody");
        
        Map<String, Object> result = dataPreviewService.previewApiData(
            connectorId, apiPath, apiMethod, params, headers, bodyType, requestBody, dataPath, limit);
        return Result.success(result);
    }

    /**
     * Webhook触发任务执行（事件驱动）
     */
    @PostMapping("/trigger/{taskCode}")
    public Result<Long> triggerByCode(@PathVariable String taskCode,
                                      @RequestHeader(value = "X-Webhook-Token", required = false) String token) {
        // 若设置了环境变量 WEBHOOK_TOKEN，则需要校验令牌
        String expected = System.getenv("WEBHOOK_TOKEN");
        if (expected != null && !expected.isEmpty()) {
            if (token == null || !expected.equals(token)) {
                return Result.error(403, "Webhook鉴权失败");
            }
        }

        TaskConfig taskConfig = taskConfigService.getOne(new LambdaQueryWrapper<TaskConfig>()
                .eq(TaskConfig::getTaskCode, taskCode).last("limit 1"));
        if (taskConfig == null) {
            return Result.error(404, "任务不存在: " + taskCode);
        }
        Long logId = taskConfigService.executeTask(taskConfig.getId(), "WEBHOOK");
        return Result.success("任务已触发执行", logId);
    }
    
    /**
     * 保存任务配置（支持多目标）
     */
    @PostMapping("/save-with-targets")
    public Result<?> saveTaskWithTargets(@RequestBody TaskSaveRequest request) {
        try {
            TaskConfig task = request.getTask();
            List<TaskTarget> targets = request.getTargets();
            List<FieldMapping> fieldMappings = request.getFieldMappings();
            
            // 1. 保存任务基础信息
            boolean taskSaved;
            if (task.getId() == null) {
                taskSaved = taskConfigService.save(task);
            } else {
                taskSaved = taskConfigService.updateById(task);
            }
            
            if (!taskSaved) {
                return Result.error("保存任务失败");
            }
            
            // 2. 保存目标配置
            if (task.getMultiTarget() != null && task.getMultiTarget() == 1) {
                // 多目标模式：保存targets
                if (targets != null && !targets.isEmpty()) {
                    taskTargetService.saveTargets(task.getId(), targets);
                    
                    // 更新fieldMappings中的targetId
                    List<TaskTarget> savedTargets = taskTargetService.getEnabledTargetsByTaskId(task.getId());
                    for (int i = 0; i < savedTargets.size() && i < targets.size(); i++) {
                        Long targetId = savedTargets.get(i).getId();
                        final int index = i;
                        // 更新该目标的字段映射
                        if (fieldMappings != null) {
                            fieldMappings.stream()
                                .filter(m -> m.getTargetId() != null && m.getTargetId().equals((long)index))
                                .forEach(m -> m.setTargetId(targetId));
                        }
                    }
                }
            } else {
                // 单目标模式：创建默认target（兼容性）
                TaskTarget defaultTarget = new TaskTarget();
                defaultTarget.setTaskId(task.getId());
                defaultTarget.setTargetConnectorId(task.getTargetConnectorId());
                defaultTarget.setTargetConfig(task.getTargetConfig());
                defaultTarget.setTargetName("默认目标");
                defaultTarget.setSortOrder(0);
                defaultTarget.setStatus(1);
                
                taskTargetService.saveTargets(task.getId(), java.util.Collections.singletonList(defaultTarget));
                
                // 获取保存后的targetId
                List<TaskTarget> savedTargets = taskTargetService.getEnabledTargetsByTaskId(task.getId());
                if (!savedTargets.isEmpty()) {
                    Long targetId = savedTargets.get(0).getId();
                    // 单目标模式：所有映射都关联到这个目标
                    if (fieldMappings != null) {
                        fieldMappings.forEach(m -> m.setTargetId(targetId));
                    }
                }
            }
            
            // 3. 保存字段映射
            if (fieldMappings != null && !fieldMappings.isEmpty()) {
                fieldMappingService.saveFieldMappings(task.getId(), fieldMappings);
            }
            
            // 4. 如果任务状态为启用且是定时任务，自动启动调度
            if (task.getStatus() != null && task.getStatus() == 1 
                && "CRON".equals(task.getScheduleType())) {
                try {
                    taskConfigService.startSchedule(task.getId());
                    log.info("任务保存成功，定时调度已启动: taskId={}", task.getId());
                } catch (Exception e) {
                    log.warn("任务保存成功，但调度启动失败: taskId={}", task.getId(), e);
                    // 不影响保存结果，只是记录警告
                }
            }
            
            return Result.success("保存成功", task.getId());
            
        } catch (Exception e) {
            log.error("保存任务失败", e);
            return Result.error("保存失败：" + e.getMessage());
        }
    }
    
    /**
     * 删除任务（级联删除目标和映射）
     */
    @DeleteMapping("/delete-cascade/{id}")
    public Result<?> deleteCascade(@PathVariable Long id) {
        try {
            // 1. 先停止调度
            taskConfigService.stopSchedule(id);
            
            // 2. 删除任务（逻辑删除）
            taskConfigService.removeById(id);
            
            // 3. 删除目标配置
            taskTargetService.deleteByTaskId(id);
            
            // 4. 删除字段映射
            fieldMappingService.deleteByTaskId(id);
            
            // 5. 删除辅助数据源
            auxiliaryDatasourceService.deleteByTaskId(id);
            
            return Result.success("删除成功");
            
        } catch (Exception e) {
            log.error("删除任务失败", e);
            return Result.error("删除失败：" + e.getMessage());
        }
    }
    
    /**
     * 复制任务
     */
    @PostMapping("/{id}/copy")
    public Result<?> copyTask(@PathVariable Long id, @RequestBody Map<String, String> request) {
        try {
            String newTaskName = request.get("taskName");
            if (!StringUtils.hasText(newTaskName)) {
                return Result.error("任务名称不能为空");
            }
            
            // 1. 获取原任务配置
            TaskConfig originalTask = taskConfigService.getById(id);
            if (originalTask == null) {
                return Result.error("原任务不存在");
            }
            
            // 2. 创建新任务（复制属性）
            TaskConfig newTask = new TaskConfig();
            newTask.setTaskName(newTaskName);
            newTask.setTaskCode(originalTask.getTaskCode() + "_COPY_" + System.currentTimeMillis());
            newTask.setSourceConnectorId(originalTask.getSourceConnectorId());
            newTask.setSourceConfig(originalTask.getSourceConfig());
            newTask.setTargetConnectorId(originalTask.getTargetConnectorId());
            newTask.setTargetConfig(originalTask.getTargetConfig());
            newTask.setSyncMode(originalTask.getSyncMode());
            newTask.setIncrementalField(originalTask.getIncrementalField());
            newTask.setScheduleType(originalTask.getScheduleType());
            newTask.setCronExpression(originalTask.getCronExpression());
            newTask.setMultiTarget(originalTask.getMultiTarget());
            newTask.setPostLoadConfig(originalTask.getPostLoadConfig());
            newTask.setDescription(originalTask.getDescription());
            newTask.setStatus(originalTask.getStatus()); // 复制原任务的状态
            
            // 保存新任务
            boolean taskSaved = taskConfigService.save(newTask);
            if (!taskSaved) {
                return Result.error("复制任务失败");
            }
            
            Long newTaskId = newTask.getId();
            
            // 3. 复制目标配置
            List<TaskTarget> originalTargets = taskTargetService.getEnabledTargetsByTaskId(id);
            if (originalTargets != null && !originalTargets.isEmpty()) {
                for (TaskTarget originalTarget : originalTargets) {
                    TaskTarget newTarget = new TaskTarget();
                    newTarget.setTaskId(newTaskId);
                    newTarget.setTargetConnectorId(originalTarget.getTargetConnectorId());
                    newTarget.setTargetName(originalTarget.getTargetName());
                    newTarget.setTargetConfig(originalTarget.getTargetConfig());
                    newTarget.setSortOrder(originalTarget.getSortOrder());
                    newTarget.setStatus(originalTarget.getStatus());
                    taskTargetService.save(newTarget);
                }
            }
            
            // 4. 复制字段映射
            List<FieldMapping> originalMappings = fieldMappingService.listByTaskId(id);
            if (originalMappings != null && !originalMappings.isEmpty()) {
                // 获取新保存的targets，建立索引映射
                List<TaskTarget> newTargets = taskTargetService.getEnabledTargetsByTaskId(newTaskId);
                Map<Long, Long> targetIdMapping = new HashMap<>();
                for (int i = 0; i < originalTargets.size() && i < newTargets.size(); i++) {
                    targetIdMapping.put(originalTargets.get(i).getId(), newTargets.get(i).getId());
                }
                
                for (FieldMapping originalMapping : originalMappings) {
                    FieldMapping newMapping = new FieldMapping();
                    newMapping.setTaskId(newTaskId);
                    // 映射到新的targetId
                    Long originalTargetId = originalMapping.getTargetId();
                    newMapping.setTargetId(targetIdMapping.getOrDefault(originalTargetId, originalTargetId));
                    newMapping.setSourceField(originalMapping.getSourceField());
                    newMapping.setTargetField(originalMapping.getTargetField());
                    newMapping.setSourceType(originalMapping.getSourceType());
                    newMapping.setTargetType(originalMapping.getTargetType());
                    newMapping.setTransformType(originalMapping.getTransformType());
                    newMapping.setDefaultValue(originalMapping.getDefaultValue());
                    newMapping.setTransformScript(originalMapping.getTransformScript());
                    newMapping.setTransformFunction(originalMapping.getTransformFunction());
                    newMapping.setDictMappingId(originalMapping.getDictMappingId());
                    newMapping.setDictSourceTypeValue(originalMapping.getDictSourceTypeValue());
                    newMapping.setDictTargetTypeValue(originalMapping.getDictTargetTypeValue());
                    newMapping.setDictOutputMode(originalMapping.getDictOutputMode());
                    newMapping.setNullStrategy(originalMapping.getNullStrategy());
                    newMapping.setCleanseFunctions(originalMapping.getCleanseFunctions());
                    newMapping.setProcessorChain(originalMapping.getProcessorChain());
                    newMapping.setSortOrder(originalMapping.getSortOrder());
                    fieldMappingService.save(newMapping);
                }
            }
            
            // 5. 复制辅助数据源（如果有）
            List<AuxiliaryDatasource> originalAuxDatasources = auxiliaryDatasourceService.listByTaskId(id);
            if (originalAuxDatasources != null && !originalAuxDatasources.isEmpty()) {
                for (AuxiliaryDatasource originalAux : originalAuxDatasources) {
                    AuxiliaryDatasource newAux = new AuxiliaryDatasource();
                    newAux.setTaskId(newTaskId);
                    newAux.setConnectorId(originalAux.getConnectorId());
                    newAux.setConnectorType(originalAux.getConnectorType());
                    newAux.setAlias(originalAux.getAlias());
                    newAux.setJoinType(originalAux.getJoinType());
                    newAux.setJoinCondition(originalAux.getJoinCondition());
                    newAux.setConfig(originalAux.getConfig());
                    newAux.setEnabled(originalAux.getEnabled());
                    newAux.setSortOrder(originalAux.getSortOrder());
                    auxiliaryDatasourceService.save(newAux);
                }
            }
            
            return Result.success("复制成功", newTaskId);
            
        } catch (Exception e) {
            log.error("复制任务失败: taskId={}", id, e);
            return Result.error("复制失败：" + e.getMessage());
        }
    }
    
    /**
     * 任务保存请求DTO
     */
    @Data
    public static class TaskSaveRequest {
        private TaskConfig task;
        private List<TaskTarget> targets;
        private List<FieldMapping> fieldMappings;
    }
}
