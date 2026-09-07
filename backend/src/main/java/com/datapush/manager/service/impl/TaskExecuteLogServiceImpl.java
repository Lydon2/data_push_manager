package com.datapush.manager.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.datapush.manager.entity.TaskExecuteLog;
import com.datapush.manager.mapper.TaskExecuteLogMapper;
import com.datapush.manager.service.TaskExecuteLogService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 任务执行日志服务实现
 *
 * @author Data Push Team
 * @since 2024-11-20
 */
@Service
public class TaskExecuteLogServiceImpl extends ServiceImpl<TaskExecuteLogMapper, TaskExecuteLog>
        implements TaskExecuteLogService {

    @Override
    public Long createLog(Long taskId, String taskName, String triggerType) {
        TaskExecuteLog log = new TaskExecuteLog();
        log.setTaskId(taskId);
        log.setTaskName(taskName);
        log.setTriggerType(triggerType);
        log.setExecuteStatus("RUNNING");
        log.setStartTime(LocalDateTime.now());
        log.setTotalCount(0);
        log.setSuccessCount(0);
        log.setFailedCount(0);
        
        save(log);
        return log.getId();
    }

    @Override
    public void updateSuccess(Long logId, Integer totalCount, Integer successCount, String executeLog) {
        TaskExecuteLog log = getById(logId);
        if (log != null) {
            log.setExecuteStatus("SUCCESS");
            log.setEndTime(LocalDateTime.now());
            log.setDuration(calculateDuration(log.getStartTime(), log.getEndTime()));
            log.setTotalCount(totalCount);
            log.setSuccessCount(successCount);
            log.setFailedCount(totalCount - successCount);
            log.setExecuteLog(executeLog);
            updateById(log);
        }
    }

    @Override
    public void updateFailed(Long logId, String errorMessage, String executeLog) {
        TaskExecuteLog log = getById(logId);
        if (log != null) {
            log.setExecuteStatus("FAILED");
            log.setEndTime(LocalDateTime.now());
            log.setDuration(calculateDuration(log.getStartTime(), log.getEndTime()));
            log.setErrorMessage(errorMessage);
            log.setExecuteLog(executeLog);
            updateById(log);
        }
    }

    /**
     * 计算执行时长（毫秒）
     */
    private Long calculateDuration(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null) {
            return 0L;
        }
        return java.time.Duration.between(startTime, endTime).toMillis();
    }
}
