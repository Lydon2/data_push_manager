package com.datapush.manager.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.datapush.manager.common.Result;
import com.datapush.manager.dto.MonitorDashboardDTO;
import com.datapush.manager.entity.AlertRecord;
import com.datapush.manager.entity.TaskExecuteLog;
import com.datapush.manager.service.AlertService;
import com.datapush.manager.service.MonitorService;
import com.datapush.manager.service.TaskExecuteLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

/**
 * 监控控制器
 */
@Slf4j
@RestController
@RequestMapping("/v1/monitor")
@CrossOrigin
public class MonitorController {

    @Autowired
    private MonitorService monitorService;

    @Autowired
    private AlertService alertService;

    @Autowired
    private TaskExecuteLogService taskExecuteLogService;

    /**
     * 获取监控仪表盘数据
     */
    @GetMapping("/dashboard")
    public Result<MonitorDashboardDTO> getDashboard() {
        try {
            MonitorDashboardDTO dashboard = monitorService.getDashboardData();
            return Result.success(dashboard);
        } catch (Exception e) {
            log.error("获取监控仪表盘数据失败", e);
            return Result.error("获取监控仪表盘数据失败: " + e.getMessage());
        }
    }

    /**
     * 获取最近执行任务列表
     */
    @GetMapping("/recent-tasks")
    public Result<List<TaskExecuteLog>> getRecentTasks(@RequestParam(defaultValue = "10") Integer limit) {
        try {
            LambdaQueryWrapper<TaskExecuteLog> wrapper = new LambdaQueryWrapper<>();
            wrapper.orderByDesc(TaskExecuteLog::getStartTime)
                   .last("LIMIT " + limit);
            List<TaskExecuteLog> logs = taskExecuteLogService.list(wrapper);
            return Result.success(logs);
        } catch (Exception e) {
            log.error("获取最近执行任务失败", e);
            return Result.error("获取最近执行任务失败: " + e.getMessage());
        }
    }

    /**
     * 获取任务执行趋势（最近7天）
     */
    @GetMapping("/execute-trend")
    public Result<Map<String, Object>> getExecuteTrend() {
        try {
            List<String> dates = new ArrayList<>();
            List<Long> successCounts = new ArrayList<>();
            List<Long> failCounts = new ArrayList<>();
            
            LocalDate today = LocalDate.now();
            for (int i = 6; i >= 0; i--) {
                LocalDate date = today.minusDays(i);
                dates.add(date.toString());
                
                LocalDateTime dayStart = LocalDateTime.of(date, LocalTime.MIN);
                LocalDateTime dayEnd = LocalDateTime.of(date, LocalTime.MAX);
                
                // 成功次数
                LambdaQueryWrapper<TaskExecuteLog> successWrapper = new LambdaQueryWrapper<>();
                successWrapper.between(TaskExecuteLog::getStartTime, dayStart, dayEnd)
                             .eq(TaskExecuteLog::getExecuteStatus, "SUCCESS");
                Long successCount = Long.valueOf(taskExecuteLogService.count(successWrapper));
                successCounts.add(successCount);
                
                // 失败次数
                LambdaQueryWrapper<TaskExecuteLog> failWrapper = new LambdaQueryWrapper<>();
                failWrapper.between(TaskExecuteLog::getStartTime, dayStart, dayEnd)
                          .eq(TaskExecuteLog::getExecuteStatus, "FAILED");
                Long failCount = Long.valueOf(taskExecuteLogService.count(failWrapper));
                failCounts.add(failCount);
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("dates", dates);
            result.put("successCounts", successCounts);
            result.put("failCounts", failCounts);
            
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取任务执行趋势失败", e);
            return Result.error("获取任务执行趋势失败: " + e.getMessage());
        }
    }

    /**
     * 获取告警记录列表
     */
    @GetMapping("/alerts")
    public Result<IPage<AlertRecord>> getAlerts(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String alertLevel,
            @RequestParam(required = false) String alertStatus) {
        try {
            Page<AlertRecord> page = new Page<>(current, size);
            LambdaQueryWrapper<AlertRecord> wrapper = new LambdaQueryWrapper<>();
            
            if (alertLevel != null && !alertLevel.isEmpty()) {
                wrapper.eq(AlertRecord::getAlertLevel, alertLevel);
            }
            if (alertStatus != null && !alertStatus.isEmpty()) {
                wrapper.eq(AlertRecord::getAlertStatus, alertStatus);
            }
            
            wrapper.orderByDesc(AlertRecord::getAlertTime);
            
            IPage<AlertRecord> result = alertService.page(page, wrapper);
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取告警记录失败", e);
            return Result.error("获取告警记录失败: " + e.getMessage());
        }
    }

    /**
     * 处理告警
     */
    @PostMapping("/alerts/{id}/handle")
    public Result<Void> handleAlert(
            @PathVariable Long id,
            @RequestParam String handleUser,
            @RequestParam(required = false) String handleRemark) {
        try {
            alertService.handleAlert(id, handleUser, handleRemark);
            return Result.success();
        } catch (Exception e) {
            log.error("处理告警失败", e);
            return Result.error("处理告警失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取性能指标
     * @param hours 过去 N 小时，默认 24 小时
     */
    @GetMapping("/performance-metrics")
    public Result<Map<String, Object>> getPerformanceMetrics(
            @RequestParam(defaultValue = "24") Integer hours) {
        try {
            Map<String, Object> metrics = monitorService.getPerformanceMetrics(hours);
            return Result.success(metrics);
        } catch (Exception e) {
            log.error("获取性能指标失败", e);
            return Result.error("获取性能指标失败: " + e.getMessage());
        }
    }
}
