package com.datapush.manager.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 任务执行日志实体
 * 对应表：dp_task_execute_log
 *
 * @author Data Push Team
 * @since 2024-11-20
 */
@Data
@TableName("dp_task_execute_log")
public class TaskExecuteLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 任务ID
     */
    private Long taskId;

    /**
     * 任务名称
     */
    private String taskName;

    /**
     * 执行状态：RUNNING-执行中 SUCCESS-成功 FAILED-失败
     */
    private String executeStatus;

    /**
     * 触发类型：MANUAL-手动 CRON-定时 API-接口
     */
    private String triggerType;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    private LocalDateTime endTime;

    /**
     * 执行时长（毫秒）
     */
    private Long duration;

    /**
     * 总记录数
     */
    private Integer totalCount;

    /**
     * 成功记录数
     */
    private Integer successCount;

    /**
     * 失败记录数
     */
    private Integer failedCount;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 执行日志
     */
    private String executeLog;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
