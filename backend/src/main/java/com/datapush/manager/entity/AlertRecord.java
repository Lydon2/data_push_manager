package com.datapush.manager.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 告警记录实体
 */
@Data
@TableName("dp_alert_record")
public class AlertRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 告警规则ID
     */
    private Long ruleId;

    /**
     * 规则名称
     */
    private String ruleName;

    /**
     * 告警类型
     */
    private String alertType;

    /**
     * 告警级别
     */
    private String alertLevel;

    /**
     * 关联任务ID
     */
    private Long taskId;

    /**
     * 任务名称
     */
    private String taskName;

    /**
     * 执行日志ID
     */
    private Long executeLogId;

    /**
     * 告警标题
     */
    private String alertTitle;

    /**
     * 告警内容
     */
    private String alertContent;

    /**
     * 告警时间
     */
    private LocalDateTime alertTime;

    /**
     * 告警状态：PENDING-待处理 NOTIFIED-已通知 HANDLED-已处理 IGNORED-已忽略
     */
    private String alertStatus;

    /**
     * 已发送渠道
     */
    private String sendChannels;

    /**
     * 发送结果（JSON格式）
     */
    private String sendResult;

    /**
     * 处理人
     */
    private String handleUser;

    /**
     * 处理时间
     */
    private LocalDateTime handleTime;

    /**
     * 处理备注
     */
    private String handleRemark;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
