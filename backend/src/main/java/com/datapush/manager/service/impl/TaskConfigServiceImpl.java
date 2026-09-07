package com.datapush.manager.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.datapush.manager.common.BusinessException;
import com.datapush.manager.engine.EtlEngine;
import com.datapush.manager.engine.impl.ApiToApiEtlEngine;
import com.datapush.manager.engine.impl.ApiToDataBaseEtlEngine;
import com.datapush.manager.engine.impl.DataBaseToApiEtlEngine;
import com.datapush.manager.engine.impl.DatabaseToDataBaseEtlEngine;
import com.datapush.manager.entity.ConnectorInfo;
import com.datapush.manager.entity.FieldMapping;
import com.datapush.manager.entity.TaskConfig;
import com.datapush.manager.mapper.TaskConfigMapper;
import com.datapush.manager.service.ConnectorInfoService;
import com.datapush.manager.service.FieldMappingService;
import com.datapush.manager.service.TaskConfigService;
import com.datapush.manager.service.TaskExecuteLogService;
import com.datapush.manager.service.AlertService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * 任务配置服务实现
 *
 * @author Data Push Team
 * @since 2024-11-20
 */
@Slf4j
@Service
public class TaskConfigServiceImpl extends ServiceImpl<TaskConfigMapper, TaskConfig>
        implements TaskConfigService {

    @Autowired
    private AlertService alertService;

    @Autowired
    private ConnectorInfoService connectorInfoService;

    @Autowired
    private FieldMappingService fieldMappingService;

    @Autowired
    private TaskExecuteLogService taskExecuteLogService;

    @Autowired
    private DatabaseToDataBaseEtlEngine databaseToDataBaseEtlEngine;

    @Autowired
    private ApiToDataBaseEtlEngine apiToDataBaseEtlEngine;

    @Autowired
    private DataBaseToApiEtlEngine dataBaseToApiEtlEngine;

    @Autowired
    private ApiToApiEtlEngine apiToApiEtlEngine;

    @Autowired
    private TaskScheduler taskScheduler;

    /**
     * 存储定时任务的ScheduledFuture
     */
    private final Map<Long, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    @Override
    public Long executeTask(Long taskId, String triggerType) {
        // 1. 查询任务配置
        TaskConfig taskConfig = getById(taskId);
        if (taskConfig == null) {
            throw new BusinessException("任务不存在");
        }

        if (taskConfig.getStatus() == 0) {
            throw new BusinessException("任务已禁用");
        }

        // 2. 创建执行日志
        Long logId = taskExecuteLogService.createLog(taskId, taskConfig.getTaskName(), triggerType);

        // 3. 异步执行任务
        executeTaskAsync(taskConfig, logId);

        return logId;
    }

    /**
     * 异步执行任务
     */
    private void executeTaskAsync(TaskConfig taskConfig, Long logId) {
        new Thread(() -> {
            try {
                // 查询源连接器信息
                ConnectorInfo sourceConnector = connectorInfoService.getById(taskConfig.getSourceConnectorId());
                
                if (sourceConnector == null) {
                    throw new BusinessException("源连接器不存在");
                }
                
                // 查询目标连接器信息（仅单目标模式需要）
                ConnectorInfo targetConnector = null;
                if (taskConfig.getMultiTarget() == null || taskConfig.getMultiTarget() == 0) {
                    // 单目标模式：查询目标连接器
                    targetConnector = connectorInfoService.getById(taskConfig.getTargetConnectorId());
                    if (targetConnector == null) {
                        throw new BusinessException("目标连接器不存在");
                    }
                }
                // 多目标模式：targetConnector为null,由ETL引擎内部查询各个目标的连接器

                // 查询字段映射
                List<FieldMapping> fieldMappings = fieldMappingService.listByTaskId(taskConfig.getId());

                if (fieldMappings == null || fieldMappings.isEmpty()) {
                    throw new BusinessException("字段映射不存在");
                }

                // 根据连接器类型选择ETL引擎
                EtlEngine etlEngine;
                if (taskConfig.getMultiTarget() != null && taskConfig.getMultiTarget() == 1) {
                    // 多目标模式：只支持DATABASE->DATABASE
                    etlEngine = databaseToDataBaseEtlEngine;
                } else {
                    // 单目标模式：根据连接器类型选择
                    etlEngine = selectEtlEngine(sourceConnector.getConnectorType(), 
                                               targetConnector.getConnectorType());
                }

                // 执行ETL
                EtlEngine.EtlResult result = etlEngine.execute(
                        taskConfig, sourceConnector, targetConnector, fieldMappings, logId);

                // 更新日志
                if (result.isSuccess()) {
                    taskExecuteLogService.updateSuccess(logId, result.getTotalCount(), 
                            result.getSuccessCount(), result.getExecuteLog());
                    
                    // 更新任务的最后同步时间
                    taskConfig.setLastSyncTime(LocalDateTime.now());
                    updateById(taskConfig);
                } else {
                    taskExecuteLogService.updateFailed(logId, result.getMessage(), result.getExecuteLog());
                    // 触发任务失败告警
                    alertService.checkTaskFailAlert(taskConfig.getId(), taskConfig.getTaskName(), 
                                                    logId, result.getMessage());
                }

            } catch (Exception e) {
                log.error("任务执行失败", e);
                taskExecuteLogService.updateFailed(logId, e.getMessage(), "任务执行异常: " + e.getMessage());
                // 触发任务失败告警
                alertService.checkTaskFailAlert(taskConfig.getId(), taskConfig.getTaskName(), 
                                                logId, e.getMessage());
            }
        }).start();
    }

    /**
     * 根据连接器类型选择ETL引擎
     */
    private EtlEngine selectEtlEngine(String sourceType, String targetType) {
        if ("DATABASE".equals(sourceType) && "DATABASE".equals(targetType)) {
            return databaseToDataBaseEtlEngine;
        } else if ("API".equals(sourceType) && "DATABASE".equals(targetType)) {
            return apiToDataBaseEtlEngine;
        } else if ("DATABASE".equals(sourceType) && "API".equals(targetType)) {
            return dataBaseToApiEtlEngine;
        } else if ("API".equals(sourceType) && "API".equals(targetType)) {
            return apiToApiEtlEngine;
        } else {
            throw new BusinessException("不支持的连接器类型组合: " + sourceType + " -> " + targetType);
        }
    }

    @Override
    public boolean startSchedule(Long taskId) {
        TaskConfig taskConfig = getById(taskId);
        if (taskConfig == null) {
            throw new BusinessException("任务不存在");
        }

        if (!"CRON".equals(taskConfig.getScheduleType())) {
            throw new BusinessException("非定时任务无法启动调度");
        }

        if (scheduledTasks.containsKey(taskId)) {
            throw new BusinessException("任务调度已启动");
        }

        try {
            ScheduledFuture<?> future = taskScheduler.schedule(
                    () -> executeTask(taskId, "CRON"),
                    new CronTrigger(taskConfig.getCronExpression())
            );
            
            scheduledTasks.put(taskId, future);
            log.info("任务调度启动成功: {}, Cron: {}", taskConfig.getTaskName(), taskConfig.getCronExpression());
            return true;
            
        } catch (Exception e) {
            log.error("任务调度启动失败", e);
            throw new BusinessException("任务调度启动失败: " + e.getMessage());
        }
    }

    @Override
    public boolean stopSchedule(Long taskId) {
        ScheduledFuture<?> future = scheduledTasks.get(taskId);
        if (future != null) {
            future.cancel(false);
            scheduledTasks.remove(taskId);
            log.info("任务调度已停止: taskId={}", taskId);
            return true;
        }
        return false;
    }
}
