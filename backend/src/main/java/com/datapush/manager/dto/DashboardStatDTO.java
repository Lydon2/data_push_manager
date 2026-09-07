package com.datapush.manager.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 仪表盘统计数据DTO
 */
@Data
public class DashboardStatDTO {

    /**
     * 任务状态统计
     */
    private TaskStatusStat taskStatusStat;

    /**
     * 执行成功率统计
     */
    private ExecutionSuccessRateStat executionStat;

    /**
     * 最近执行记录
     */
    private List<RecentExecutionLog> recentExecutions;

    /**
     * 耗时TOP10任务
     */
    private List<TaskDurationStat> top10DurationTasks;

    /**
     * 最近7天执行趋势
     */
    private List<DailyExecutionTrend> dailyTrend;

    @Data
    public static class TaskStatusStat {
        private Integer totalTasks;
        private Integer enabledTasks;
        private Integer disabledTasks;
        private Integer cronTasks;
        private Integer manualTasks;
    }

    @Data
    public static class ExecutionSuccessRateStat {
        private Long totalExecutions;
        private Long successExecutions;
        private Long failedExecutions;
        private Long runningExecutions;
        private Double successRate;
    }

    @Data
    public static class RecentExecutionLog {
        private Long id;
        private String taskName;
        private String executeStatus;
        private String triggerType;
        private String startTime;
        private Long duration;
        private Integer totalCount;
        private Integer successCount;
    }

    @Data
    public static class TaskDurationStat {
        private String taskName;
        private Long avgDuration;
        private Long maxDuration;
        private Integer executionCount;
    }

    @Data
    public static class DailyExecutionTrend {
        private String date;
        private Integer totalCount;
        private Integer successCount;
        private Integer failedCount;
    }
}
