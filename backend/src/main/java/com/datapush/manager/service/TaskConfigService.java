package com.datapush.manager.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datapush.manager.entity.TaskConfig;

/**
 * 任务配置服务接口
 *
 * @author Data Push Team
 * @since 2024-11-20
 */
public interface TaskConfigService extends IService<TaskConfig> {

    /**
     * 执行任务
     *
     * @param taskId 任务ID
     * @param triggerType 触发类型：MANUAL-手动 CRON-定时 API-接口
     * @return 执行日志ID
     */
    Long executeTask(Long taskId, String triggerType);

    /**
     * 启动任务调度
     *
     * @param taskId 任务ID
     * @return 是否成功
     */
    boolean startSchedule(Long taskId);

    /**
     * 停止任务调度
     *
     * @param taskId 任务ID
     * @return 是否成功
     */
    boolean stopSchedule(Long taskId);
}
