package com.datapush.manager.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.datapush.manager.common.Result;
import com.datapush.manager.entity.TaskExecuteLog;
import com.datapush.manager.service.TaskExecuteLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 任务执行日志控制器
 *
 * @author Data Push Team
 * @since 2024-11-20
 */
@Slf4j
@RestController
@RequestMapping("/v1/log")
public class TaskExecuteLogController {

    @Autowired
    private TaskExecuteLogService taskExecuteLogService;

    /**
     * 分页查询执行日志
     */
    @GetMapping("/page")
    public Result<IPage<TaskExecuteLog>> page(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Long taskId,
            @RequestParam(required = false) String taskName,
            @RequestParam(required = false) String executeStatus,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        
        Page<TaskExecuteLog> page = new Page<>(current, size);
        LambdaQueryWrapper<TaskExecuteLog> wrapper = new LambdaQueryWrapper<>();
        
        if (taskId != null) {
            wrapper.eq(TaskExecuteLog::getTaskId, taskId);
        }
        if (StringUtils.hasText(taskName)) {
            wrapper.like(TaskExecuteLog::getTaskName, taskName);
        }
        if (StringUtils.hasText(executeStatus)) {
            wrapper.eq(TaskExecuteLog::getExecuteStatus, executeStatus);
        }
        if (startTime != null) {
            wrapper.ge(TaskExecuteLog::getStartTime, startTime);
        }
        if (endTime != null) {
            wrapper.le(TaskExecuteLog::getStartTime, endTime);
        }
        
        wrapper.orderByDesc(TaskExecuteLog::getStartTime);
        IPage<TaskExecuteLog> result = taskExecuteLogService.page(page, wrapper);
        
        return Result.success(result);
    }

    /**
     * 根据ID查询日志详情
     */
    @GetMapping("/{id}")
    public Result<TaskExecuteLog> getById(@PathVariable Long id) {
        TaskExecuteLog log = taskExecuteLogService.getById(id);
        return Result.success(log);
    }

    /**
     * 删除日志
     */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        boolean success = taskExecuteLogService.removeById(id);
        return success ? Result.success("删除成功") : Result.error("删除失败");
    }
}
