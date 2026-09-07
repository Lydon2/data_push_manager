package com.datapush.manager.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 告警规则实体
 */
@Data
@TableName("dp_alert_rule")
public class AlertRule implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 规则名称
     */
    private String ruleName;

    /**
     * 规则编码（唯一）
     */
    private String ruleCode;

    /**
     * 规则类型：TASK_FAIL-任务失败 TASK_DELAY-任务延迟 SYSTEM_ERROR-系统错误
     */
    private String ruleType;

    /**
     * 关联任务ID（为空表示全局规则）
     */
    private Long taskId;

    /**
     * 条件配置（JSON格式）
     */
    private String conditionConfig;

    /**
     * 告警级别：INFO-提示 WARNING-警告 ERROR-错误 CRITICAL-严重
     */
    private String alertLevel;

    /**
     * 告警渠道（逗号分隔）：INTERNAL-内部消息 WEBHOOK-回调 LOG-日志
     */
    private String alertChannels;

    /**
     * 渠道配置（JSON格式）
     */
    private String channelConfig;

    /**
     * 是否启用：0-禁用 1-启用
     */
    private Integer enabled;

    /**
     * 规则描述
     */
    private String description;

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
