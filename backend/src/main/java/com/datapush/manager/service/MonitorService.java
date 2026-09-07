package com.datapush.manager.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.datapush.manager.dto.MonitorDashboardDTO;
import com.datapush.manager.entity.TaskConfig;
import com.datapush.manager.entity.TaskExecuteLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 监控服务
 * 支持吐吞量、错误率、延迟等核心指标
 */
@Slf4j
@Service
public class MonitorService {

    @Autowired
    private TaskConfigService taskConfigService;

    @Autowired
    private TaskExecuteLogService taskExecuteLogService;

    @Autowired
    private AlertService alertService;

    /**
     * 获取监控仪表盘数据
     */
    public MonitorDashboardDTO getDashboardData() {
        MonitorDashboardDTO dashboard = new MonitorDashboardDTO();
        
        // 任务统计
        LambdaQueryWrapper<TaskConfig> taskWrapper = new LambdaQueryWrapper<>();
        dashboard.setTotalTasks((long) taskConfigService.count(taskWrapper));
        
        taskWrapper.eq(TaskConfig::getStatus, 1);
        dashboard.setEnabledTasks((long) taskConfigService.count(taskWrapper));
        
        dashboard.setDisabledTasks(dashboard.getTotalTasks() - dashboard.getEnabledTasks());
        
        // 今日执行统计
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime todayEnd = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);
        
        LambdaQueryWrapper<TaskExecuteLog> todayWrapper = new LambdaQueryWrapper<>();
        todayWrapper.between(TaskExecuteLog::getStartTime, todayStart, todayEnd);
        dashboard.setTodayExecuteCount((long) taskExecuteLogService.count(todayWrapper));
        
        // 今日成功次数
        LambdaQueryWrapper<TaskExecuteLog> successWrapper = new LambdaQueryWrapper<>();
        successWrapper.between(TaskExecuteLog::getStartTime, todayStart, todayEnd)
                     .eq(TaskExecuteLog::getExecuteStatus, "SUCCESS");
        dashboard.setTodaySuccessCount((long) taskExecuteLogService.count(successWrapper));
        
        // 今日失败次数
        LambdaQueryWrapper<TaskExecuteLog> failWrapper = new LambdaQueryWrapper<>();
        failWrapper.between(TaskExecuteLog::getStartTime, todayStart, todayEnd)
                  .eq(TaskExecuteLog::getExecuteStatus, "FAILED");
        dashboard.setTodayFailCount((long) taskExecuteLogService.count(failWrapper));
        
        // 今日数据量
        LambdaQueryWrapper<TaskExecuteLog> dataWrapper = new LambdaQueryWrapper<>();
        dataWrapper.between(TaskExecuteLog::getStartTime, todayStart, todayEnd)
                  .eq(TaskExecuteLog::getExecuteStatus, "SUCCESS");
        List<TaskExecuteLog> todayLogs = taskExecuteLogService.list(dataWrapper);
        long totalData = todayLogs.stream()
                                  .mapToLong(log -> log.getSuccessCount() != null ? log.getSuccessCount() : 0)
                                  .sum();
        dashboard.setTodayDataCount(totalData);
        
        // 运行中任务数
        LambdaQueryWrapper<TaskExecuteLog> runningWrapper = new LambdaQueryWrapper<>();
        runningWrapper.eq(TaskExecuteLog::getExecuteStatus, "RUNNING");
        dashboard.setRunningTasks((long) taskExecuteLogService.count(runningWrapper));
        
        // 告警和消息数
        dashboard.setUnreadAlerts(alertService.getUnreadAlertCount());
        dashboard.setUnreadMessages(alertService.getUnreadMessageCount());
        
        // 今日成功率
        if (dashboard.getTodayExecuteCount() > 0) {
            double successRate = (double) dashboard.getTodaySuccessCount() / dashboard.getTodayExecuteCount() * 100;
            dashboard.setTodaySuccessRate(Math.round(successRate * 100.0) / 100.0);
        } else {
            dashboard.setTodaySuccessRate(0.0);
        }
        
        // 平均执行时长
        if (!todayLogs.isEmpty()) {
            long avgDuration = (long) todayLogs.stream()
                                               .filter(log -> log.getDuration() != null)
                                               .mapToLong(TaskExecuteLog::getDuration)
                                               .average()
                                               .orElse(0);
            dashboard.setAvgDuration(avgDuration);
        } else {
            dashboard.setAvgDuration(0L);
        }
        
        return dashboard;
    }
    
    /**
     * 获取实时性能指标
     * @param hours 过去 N 小时
     * @return 性能指标数据
     */
    public Map<String, Object> getPerformanceMetrics(int hours) {
        Map<String, Object> metrics = new LinkedHashMap<>();
        
        LocalDateTime startTime = LocalDateTime.now().minusHours(hours);
        LocalDateTime endTime = LocalDateTime.now();
        
        // 查询过去 N 小时的执行日志
        LambdaQueryWrapper<TaskExecuteLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.between(TaskExecuteLog::getStartTime, startTime, endTime)
               .orderByDesc(TaskExecuteLog::getStartTime);
        List<TaskExecuteLog> logs = taskExecuteLogService.list(wrapper);
        
        // 1. 吐吞量指标 (Throughput)
        Map<String, Object> throughput = calculateThroughput(logs, hours);
        metrics.put("throughput", throughput);
        
        // 2. 错误率指标 (Error Rate)
        Map<String, Object> errorRate = calculateErrorRate(logs);
        metrics.put("errorRate", errorRate);
        
        // 3. 延迟指标 (Latency)
        Map<String, Object> latency = calculateLatency(logs);
        metrics.put("latency", latency);
        
        // 4. 数据质量指标
        Map<String, Object> dataQuality = calculateDataQuality(logs);
        metrics.put("dataQuality", dataQuality);
        
        // 5. 趋势数据（按小时分组）
        List<Map<String, Object>> trends = calculateTrends(logs, hours);
        metrics.put("trends", trends);
        
        return metrics;
    }
    
    /**
     * 计算吐吞量指标
     */
    private Map<String, Object> calculateThroughput(List<TaskExecuteLog> logs, int hours) {
        Map<String, Object> throughput = new LinkedHashMap<>();
        
        // 总执行次数
        long totalExecutions = logs.size();
        throughput.put("totalExecutions", totalExecutions);
        
        // 成功执行次数
        long successExecutions = logs.stream()
            .filter(log -> "SUCCESS".equals(log.getExecuteStatus()))
            .count();
        throughput.put("successExecutions", successExecutions);
        
        // 总数据量
        long totalRecords = logs.stream()
            .filter(log -> "SUCCESS".equals(log.getExecuteStatus()))
            .mapToLong(log -> log.getSuccessCount() != null ? log.getSuccessCount() : 0)
            .sum();
        throughput.put("totalRecords", totalRecords);
        
        // 每小时执行次数 (TPS - Tasks Per Hour)
        double tph = hours > 0 ? (double) totalExecutions / hours : totalExecutions;
        throughput.put("tasksPerHour", Math.round(tph * 100.0) / 100.0);
        
        // 每小时数据量 (RPS - Records Per Hour)
        double rph = hours > 0 ? (double) totalRecords / hours : totalRecords;
        throughput.put("recordsPerHour", Math.round(rph * 100.0) / 100.0);
        
        // 平均每次执行处理记录数
        double avgRecordsPerExecution = successExecutions > 0 ? (double) totalRecords / successExecutions : 0;
        throughput.put("avgRecordsPerExecution", Math.round(avgRecordsPerExecution * 100.0) / 100.0);
        
        return throughput;
    }
    
    /**
     * 计算错误率指标
     */
    private Map<String, Object> calculateErrorRate(List<TaskExecuteLog> logs) {
        Map<String, Object> errorRate = new LinkedHashMap<>();
        
        long totalCount = logs.size();
        long failedCount = logs.stream()
            .filter(log -> "FAILED".equals(log.getExecuteStatus()))
            .count();
        long successCount = logs.stream()
            .filter(log -> "SUCCESS".equals(log.getExecuteStatus()))
            .count();
        
        errorRate.put("totalCount", totalCount);
        errorRate.put("failedCount", failedCount);
        errorRate.put("successCount", successCount);
        
        // 错误率（%）
        double errorRatePercent = totalCount > 0 ? (double) failedCount / totalCount * 100 : 0;
        errorRate.put("errorRatePercent", Math.round(errorRatePercent * 100.0) / 100.0);
        
        // 成功率（%）
        double successRatePercent = totalCount > 0 ? (double) successCount / totalCount * 100 : 0;
        errorRate.put("successRatePercent", Math.round(successRatePercent * 100.0) / 100.0);
        
        // 按任务统计错误
        Map<Long, Long> errorsByTask = logs.stream()
            .filter(log -> "FAILED".equals(log.getExecuteStatus()))
            .collect(Collectors.groupingBy(TaskExecuteLog::getTaskId, Collectors.counting()));
        errorRate.put("topFailedTasks", errorsByTask);
        
        return errorRate;
    }
    
    /**
     * 计算延迟指标
     */
    private Map<String, Object> calculateLatency(List<TaskExecuteLog> logs) {
        Map<String, Object> latency = new LinkedHashMap<>();
        
        List<Long> durations = logs.stream()
            .filter(log -> log.getDuration() != null && log.getDuration() > 0)
            .map(TaskExecuteLog::getDuration)
            .sorted()
            .collect(Collectors.toList());
        
        if (durations.isEmpty()) {
            latency.put("avgDuration", 0L);
            latency.put("minDuration", 0L);
            latency.put("maxDuration", 0L);
            latency.put("p50Duration", 0L);
            latency.put("p90Duration", 0L);
            latency.put("p95Duration", 0L);
            latency.put("p99Duration", 0L);
            return latency;
        }
        
        // 平均延迟
        long avgDuration = (long) durations.stream().mapToLong(Long::longValue).average().orElse(0);
        latency.put("avgDuration", avgDuration);
        
        // 最小/最大延迟
        latency.put("minDuration", durations.get(0));
        latency.put("maxDuration", durations.get(durations.size() - 1));
        
        // 百分位数
        latency.put("p50Duration", getPercentile(durations, 50));
        latency.put("p90Duration", getPercentile(durations, 90));
        latency.put("p95Duration", getPercentile(durations, 95));
        latency.put("p99Duration", getPercentile(durations, 99));
        
        return latency;
    }
    
    /**
     * 计算百分位数
     */
    private long getPercentile(List<Long> sortedList, int percentile) {
        if (sortedList.isEmpty()) return 0;
        int index = (int) Math.ceil(sortedList.size() * percentile / 100.0) - 1;
        index = Math.max(0, Math.min(index, sortedList.size() - 1));
        return sortedList.get(index);
    }
    
    /**
     * 计算数据质量指标
     */
    private Map<String, Object> calculateDataQuality(List<TaskExecuteLog> logs) {
        Map<String, Object> quality = new LinkedHashMap<>();
        
        List<TaskExecuteLog> successLogs = logs.stream()
            .filter(log -> "SUCCESS".equals(log.getExecuteStatus()))
            .collect(Collectors.toList());
        
        if (successLogs.isEmpty()) {
            quality.put("totalProcessed", 0L);
            quality.put("totalSuccess", 0L);
            quality.put("dataAccuracy", 100.0);
            return quality;
        }
        
        // 总处理数
        long totalProcessed = successLogs.stream()
            .mapToLong(log -> (log.getTotalCount() != null ? log.getTotalCount() : 0))
            .sum();
        
        // 总成功数
        long totalSuccess = successLogs.stream()
            .mapToLong(log -> (log.getSuccessCount() != null ? log.getSuccessCount() : 0))
            .sum();
        
        quality.put("totalProcessed", totalProcessed);
        quality.put("totalSuccess", totalSuccess);
        
        // 数据准确率（成功数/处理数）
        double accuracy = totalProcessed > 0 ? (double) totalSuccess / totalProcessed * 100 : 100;
        quality.put("dataAccuracy", Math.round(accuracy * 100.0) / 100.0);
        
        return quality;
    }
    
    /**
     * 计算趋势数据（按小时分组）
     */
    private List<Map<String, Object>> calculateTrends(List<TaskExecuteLog> logs, int hours) {
        List<Map<String, Object>> trends = new ArrayList<>();
        
        // 按小时分组
        Map<Integer, List<TaskExecuteLog>> groupedByHour = logs.stream()
            .collect(Collectors.groupingBy(log -> {
                if (log.getStartTime() != null) {
                    return log.getStartTime().getHour();
                }
                return 0;
            }));
        
        for (int i = 0; i < hours; i++) {
            LocalDateTime hourStart = LocalDateTime.now().minusHours(hours - i).withMinute(0).withSecond(0);
            int hour = hourStart.getHour();
            
            List<TaskExecuteLog> hourLogs = groupedByHour.getOrDefault(hour, Collections.emptyList());
            
            Map<String, Object> hourData = new LinkedHashMap<>();
            hourData.put("hour", hourStart.toString());
            hourData.put("executions", hourLogs.size());
            
            long successCount = hourLogs.stream()
                .filter(log -> "SUCCESS".equals(log.getExecuteStatus()))
                .count();
            hourData.put("successCount", successCount);
            
            long failedCount = hourLogs.stream()
                .filter(log -> "FAILED".equals(log.getExecuteStatus()))
                .count();
            hourData.put("failedCount", failedCount);
            
            long records = hourLogs.stream()
                .filter(log -> "SUCCESS".equals(log.getExecuteStatus()))
                .mapToLong(log -> log.getSuccessCount() != null ? log.getSuccessCount() : 0)
                .sum();
            hourData.put("records", records);
            
            trends.add(hourData);
        }
        
        return trends;
    }
}
