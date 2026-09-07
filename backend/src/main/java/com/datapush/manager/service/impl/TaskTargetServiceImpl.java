package com.datapush.manager.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.datapush.manager.entity.TaskTarget;
import com.datapush.manager.mapper.TaskTargetMapper;
import com.datapush.manager.service.TaskTargetService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 任务目标配置Service实现类
 *
 * @author Data Push Team
 * @since 2024-11-24
 */
@Slf4j
@Service
public class TaskTargetServiceImpl extends ServiceImpl<TaskTargetMapper, TaskTarget> implements TaskTargetService {

    @Override
    public List<TaskTarget> getEnabledTargetsByTaskId(Long taskId) {
        LambdaQueryWrapper<TaskTarget> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TaskTarget::getTaskId, taskId)
               .eq(TaskTarget::getStatus, 1)
               .orderByAsc(TaskTarget::getSortOrder, TaskTarget::getId);
        return this.list(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveTargets(Long taskId, List<TaskTarget> targets) {
        // 1. 删除该任务的旧目标配置
        deleteByTaskId(taskId);
        
        // 2. 批量插入新配置
        if (targets != null && !targets.isEmpty()) {
            for (int i = 0; i < targets.size(); i++) {
                TaskTarget target = targets.get(i);
                target.setTaskId(taskId);
                target.setSortOrder(i); // 按列表顺序设置
                if (target.getStatus() == null) {
                    target.setStatus(1); // 默认启用
                }
            }
            this.saveBatch(targets);
            log.info("保存任务{}的目标配置，共{}个目标", taskId, targets.size());
        }
    }

    @Override
    public void deleteByTaskId(Long taskId) {
        LambdaQueryWrapper<TaskTarget> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TaskTarget::getTaskId, taskId);
        this.remove(wrapper);
    }
}
