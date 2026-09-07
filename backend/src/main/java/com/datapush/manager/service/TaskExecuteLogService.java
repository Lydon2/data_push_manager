package com.datapush.manager.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datapush.manager.entity.TaskExecuteLog;

/**
 * 任务执行日志服务接口
 *
 * @author Data Push Team
 * @since 2024-11-20
 */
public interface TaskExecuteLogService extends IService<TaskExecuteLog> {

    /**
     * 创建执行日志
     *
     * @param taskId 任务ID
     * @param taskName 任务名称
     * @param triggerType 触发类型
     * @return 日志ID
     */
    Long createLog(Long taskId, String taskName, String triggerType);

    /**
     * 更新日志为成功
     *
     * @param logId 日志ID
     * @param totalCount 总记录数
     * @param successCount 成功记录数
     * @param executeLog 执行日志
     */
    void updateSuccess(Long logId, Integer totalCount, Integer successCount, String executeLog);

    /**
     * 更新日志为失败
     *
     * @param logId 日志ID
     * @param errorMessage 错误信息
     * @param executeLog 执行日志
     */
    void updateFailed(Long logId, String errorMessage, String executeLog);
}
