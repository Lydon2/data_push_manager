package com.datapush.manager.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 任务配置实体
 * 对应表：dp_task_config
 *
 * @author Data Push Team
 * @since 2024-11-20
 */
@Data
@TableName("dp_task_config")
public class TaskConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 任务名称
     */
    private String taskName;

    /**
     * 任务编码（唯一）
     */
    private String taskCode;

    /**
     * 源连接器ID
     */
    private Long sourceConnectorId;

    /**
     * 目标连接器ID
     */
    private Long targetConnectorId;

    /**
     * 是否多目标模式：0-单目标（兼容旧版）1-多目标
     */
    private Integer multiTarget;

    /**
     * 源端配置（JSON格式）
     */
    private String sourceConfig;

    /**
     * 目标端配置（JSON格式）
     */
    private String targetConfig;

    /**
     * 同步模式：FULL-全量 INCREMENTAL-增量
     */
    private String syncMode;

    /**
     * 增量字段名
     */
    private String incrementalField;

    /**
     * 调度类型：MANUAL-手动 CRON-定时
     */
    private String scheduleType;

    /**
     * Cron表达式
     */
    private String cronExpression;

    /**
     * 任务描述
     */
    private String description;

    /**
     * 状态：0-禁用 1-启用
     */
    private Integer status;

    /**
     * 最后同步时间
     */
    private LocalDateTime lastSyncTime;
    
    /**
     * 推送后处理配置（JSON格式）
     */
    private String postLoadConfig;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 逻辑删除：0-未删除 1-已删除
     */
    @TableLogic
    private Integer deleted;
}
