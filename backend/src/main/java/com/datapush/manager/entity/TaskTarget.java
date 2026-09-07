package com.datapush.manager.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 任务目标配置实体
 * 对应表：dp_task_target
 * 支持单任务推送到多个目标表
 *
 * @author Data Push Team
 * @since 2024-11-24
 */
@Data
@TableName("dp_task_target")
public class TaskTarget implements Serializable {

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
     * 目标连接器ID
     */
    private Long targetConnectorId;

    /**
     * 目标端配置（JSON格式）
     * 示例: {"tableName":"user_info","writeMode":"INSERT","batchSize":1000,"maxRetries":3,"idempotentKey":"user_id"}
     */
    private String targetConfig;

    /**
     * 目标名称（用于区分多个目标）
     */
    private String targetName;

    /**
     * 执行顺序
     */
    private Integer sortOrder;

    /**
     * 状态：0-禁用 1-启用
     */
    private Integer status;

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
}
