package com.datapush.manager.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datapush.manager.entity.TaskTarget;

import java.util.List;

/**
 * 任务目标配置Service接口
 *
 * @author Data Push Team
 * @since 2024-11-24
 */
public interface TaskTargetService extends IService<TaskTarget> {

    /**
     * 根据任务ID获取所有启用的目标（按sortOrder排序）
     *
     * @param taskId 任务ID
     * @return 目标列表
     */
    List<TaskTarget> getEnabledTargetsByTaskId(Long taskId);

    /**
     * 批量保存任务目标配置
     * 会删除旧的配置并创建新的
     *
     * @param taskId  任务ID
     * @param targets 目标列表
     */
    void saveTargets(Long taskId, List<TaskTarget> targets);

    /**
     * 根据任务ID删除所有目标配置
     *
     * @param taskId 任务ID
     */
    void deleteByTaskId(Long taskId);
}
