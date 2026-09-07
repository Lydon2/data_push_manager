package com.datapush.manager.dto;

import lombok.Data;

/**
 * 监控仪表盘统计数据
 */
@Data
public class MonitorDashboardDTO {
    
    /**
     * 任务总数
     */
    private Long totalTasks;

    /**
     * 启用任务数
     */
    private Long enabledTasks;

    /**
     * 禁用任务数
     */
    private Long disabledTasks;

    /**
     * 今日执行次数
     */
    private Long todayExecuteCount;

    /**
     * 今日成功次数
     */
    private Long todaySuccessCount;

    /**
     * 今日失败次数
     */
    private Long todayFailCount;

    /**
     * 今日同步数据量
     */
    private Long todayDataCount;

    /**
     * 运行中任务数
     */
    private Long runningTasks;

    /**
     * 未读告警数
     */
    private Long unreadAlerts;

    /**
     * 未读消息数
     */
    private Long unreadMessages;

    /**
     * 今日成功率（百分比）
     */
    private Double todaySuccessRate;

    /**
     * 平均执行时长（毫秒）
     */
    private Long avgDuration;
}
