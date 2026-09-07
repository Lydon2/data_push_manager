package com.datapush.manager.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.datapush.manager.common.Result;
import com.datapush.manager.dto.DashboardStatDTO;
import com.datapush.manager.entity.TaskConfig;
import com.datapush.manager.entity.TaskExecuteLog;
import com.datapush.manager.mapper.TaskConfigMapper;
import com.datapush.manager.mapper.TaskExecuteLogMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 仪表盘统计接口
 */
@RestController
@RequestMapping("/v1/dashboard")
public class DashboardController {

    @Resource
    private TaskConfigMapper taskConfigMapper;

    @Resource
    private TaskExecuteLogMapper taskExecuteLogMapper;

    /**
     * 获取仪表盘统计数据
     */
    @GetMapping("/stats")
    public Result<DashboardStatDTO> getStats() {
        DashboardStatDTO dto = new DashboardStatDTO();

        // 1. 任务状态统计
        dto.setTaskStatusStat(getTaskStatusStat());

        // 2. 执行成功率统计
        dto.setExecutionStat(getExecutionSuccessRateStat());

        // 3. 最近执行记录（最近10条）
        dto.setRecentExecutions(getRecentExecutions());

        // 4. 耗时TOP10任务
        dto.setTop10DurationTasks(getTop10DurationTasks());

        // 5. 最近7天执行趋势
        dto.setDailyTrend(getDailyTrend());

        return Result.success(dto);
    }

    /**
     * 任务状态统计
     */
    private DashboardStatDTO.TaskStatusStat getTaskStatusStat() {
        DashboardStatDTO.TaskStatusStat stat = new DashboardStatDTO.TaskStatusStat();

        List<TaskConfig> allTasks = taskConfigMapper.selectList(null);
        stat.setTotalTasks(allTasks.size());
        stat.setEnabledTasks((int) allTasks.stream().filter(t -> t.getStatus() == 1).count());
        stat.setDisabledTasks((int) allTasks.stream().filter(t -> t.getStatus() == 0).count());
        stat.setCronTasks((int) allTasks.stream().filter(t -> "CRON".equals(t.getScheduleType())).count());
        stat.setManualTasks((int) allTasks.stream().filter(t -> "MANUAL".equals(t.getScheduleType())).count());

        return stat;
    }

    /**
     * 执行成功率统计
     */
    private DashboardStatDTO.ExecutionSuccessRateStat getExecutionSuccessRateStat() {
        DashboardStatDTO.ExecutionSuccessRateStat stat = new DashboardStatDTO.ExecutionSuccessRateStat();

        List<TaskExecuteLog> logs = taskExecuteLogMapper.selectList(null);
        stat.setTotalExecutions((long) logs.size());
        stat.setSuccessExecutions(logs.stream().filter(l -> "SUCCESS".equals(l.getExecuteStatus())).count());
        stat.setFailedExecutions(logs.stream().filter(l -> "FAILED".equals(l.getExecuteStatus())).count());
        stat.setRunningExecutions(logs.stream().filter(l -> "RUNNING".equals(l.getExecuteStatus())).count());

        if (stat.getTotalExecutions() > 0) {
            double rate = stat.getSuccessExecutions() * 100.0 / stat.getTotalExecutions();
            stat.setSuccessRate(BigDecimal.valueOf(rate).setScale(2, RoundingMode.HALF_UP).doubleValue());
        } else {
            stat.setSuccessRate(0.0);
        }

        return stat;
    }

    /**
     * 最近执行记录
     */
    private List<DashboardStatDTO.RecentExecutionLog> getRecentExecutions() {
        List<TaskExecuteLog> logs = taskExecuteLogMapper.selectList(
                new LambdaQueryWrapper<TaskExecuteLog>()
                        .orderByDesc(TaskExecuteLog::getStartTime)
                        .last("limit 10")
        );

        return logs.stream().map(log -> {
            DashboardStatDTO.RecentExecutionLog dto = new DashboardStatDTO.RecentExecutionLog();
            dto.setId(log.getId());
            dto.setTaskName(log.getTaskName());
            dto.setExecuteStatus(log.getExecuteStatus());
            dto.setTriggerType(log.getTriggerType());
            dto.setStartTime(log.getStartTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            dto.setDuration(log.getDuration());
            dto.setTotalCount(log.getTotalCount());
            dto.setSuccessCount(log.getSuccessCount());
            return dto;
        }).collect(Collectors.toList());
    }

    /**
     * 耗时TOP10任务
     */
    private List<DashboardStatDTO.TaskDurationStat> getTop10DurationTasks() {
        List<TaskExecuteLog> logs = taskExecuteLogMapper.selectList(
                new LambdaQueryWrapper<TaskExecuteLog>()
                        .eq(TaskExecuteLog::getExecuteStatus, "SUCCESS")
        );

        Map<String, List<TaskExecuteLog>> groupedLogs = logs.stream()
                .collect(Collectors.groupingBy(TaskExecuteLog::getTaskName));

        List<DashboardStatDTO.TaskDurationStat> stats = new ArrayList<>();
        for (Map.Entry<String, List<TaskExecuteLog>> entry : groupedLogs.entrySet()) {
            DashboardStatDTO.TaskDurationStat stat = new DashboardStatDTO.TaskDurationStat();
            stat.setTaskName(entry.getKey());
            stat.setExecutionCount(entry.getValue().size());

            List<Long> durations = entry.getValue().stream()
                    .map(TaskExecuteLog::getDuration)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            if (!durations.isEmpty()) {
                stat.setAvgDuration((long) durations.stream().mapToLong(Long::longValue).average().orElse(0));
                stat.setMaxDuration(durations.stream().mapToLong(Long::longValue).max().orElse(0));
            } else {
                stat.setAvgDuration(0L);
                stat.setMaxDuration(0L);
            }

            stats.add(stat);
        }

        return stats.stream()
                .sorted(Comparator.comparing(DashboardStatDTO.TaskDurationStat::getAvgDuration).reversed())
                .limit(10)
                .collect(Collectors.toList());
    }

    /**
     * 最近7天执行趋势
     */
    private List<DashboardStatDTO.DailyExecutionTrend> getDailyTrend() {
        LocalDate today = LocalDate.now();
        LocalDate sevenDaysAgo = today.minusDays(6);

        List<TaskExecuteLog> logs = taskExecuteLogMapper.selectList(
                new LambdaQueryWrapper<TaskExecuteLog>()
                        .ge(TaskExecuteLog::getStartTime, sevenDaysAgo.atStartOfDay())
        );

        Map<String, DashboardStatDTO.DailyExecutionTrend> trendMap = new HashMap<>();
        for (int i = 0; i < 7; i++) {
            LocalDate date = sevenDaysAgo.plusDays(i);
            String dateStr = date.format(DateTimeFormatter.ofPattern("MM-dd"));
            DashboardStatDTO.DailyExecutionTrend trend = new DashboardStatDTO.DailyExecutionTrend();
            trend.setDate(dateStr);
            trend.setTotalCount(0);
            trend.setSuccessCount(0);
            trend.setFailedCount(0);
            trendMap.put(dateStr, trend);
        }

        for (TaskExecuteLog log : logs) {
            String dateStr = log.getStartTime().toLocalDate().format(DateTimeFormatter.ofPattern("MM-dd"));
            DashboardStatDTO.DailyExecutionTrend trend = trendMap.get(dateStr);
            if (trend != null) {
                trend.setTotalCount(trend.getTotalCount() + 1);
                if ("SUCCESS".equals(log.getExecuteStatus())) {
                    trend.setSuccessCount(trend.getSuccessCount() + 1);
                } else if ("FAILED".equals(log.getExecuteStatus())) {
                    trend.setFailedCount(trend.getFailedCount() + 1);
                }
            }
        }

        return trendMap.values().stream()
                .sorted(Comparator.comparing(DashboardStatDTO.DailyExecutionTrend::getDate))
                .collect(Collectors.toList());
    }
}
