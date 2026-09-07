package com.datapush.manager.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 字段映射实体
 * 对应表：dp_field_mapping
 *
 * @author Data Push Team
 * @since 2024-11-20
 */
@Data
@TableName("dp_field_mapping")
public class FieldMapping implements Serializable {

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
     * 目标表ID（关联dp_task_target.id，多目标模式使用）
     */
    private Long targetId;

    /**
     * 源字段名
     */
    private String sourceField;

    /**
     * 目标字段名
     */
    private String targetField;

    /**
     * 源字段类型
     */
    private String sourceType;

    /**
     * 目标字段类型
     */
    private String targetType;

    /**
     * 转换类型：DIRECT-直接映射 DICT-字典映射 CONSTANT-固定值 SCRIPT-脚本
     */
    private String transformType;

    /**
     * 转换脚本（Groovy）
     */
    private String transformScript;

    /**
     * 字典映射ID
     */
    private Long dictMappingId;
    
    /**
     * 字典映射源类型值（用于区分同一字典映射下的不同类型）
     */
    private String dictSourceTypeValue;
    
    /**
     * 字典映射目标类型值（用于区分同一字典映射下的不同类型）
     */
    private String dictTargetTypeValue;
    
    /**
     * 字典输出模式：TARGET_KEY-目标编码 SOURCE_LABEL-源名称 TARGET_LABEL-目标名称
     */
    private String dictOutputMode;

    /**
     * 字典编码(新增,用于向导页面)
     */
    @TableField(exist = false)
    private String dictCode;

    /**
     * 转换函数名
     */
    private String transformFunction;

    /**
     * 默认值
     */
    private String defaultValue;

    /**
     * 固定值(新增,用于向导页面)
     */
    @TableField(exist = false)
    private String constantValue;
    
    /**
     * 空值处理策略：KEEP-保持空值 DEFAULT-使用默认值 SKIP-跳过该字段 REMOVE-移除该行
     */
    private String nullStrategy;
    
    /**
     * 清洗函数链配置（JSON格式）
     * 格式: [{"functionCode": "TRIM", "params": {}}, {"functionCode": "UPPER", "params": {}}]
     * 多个函数按顺序执行，前一个函数的输出是后一个函数的输入
     */
    private String cleanseFunctions;
    
    /**
     * 处理器链配置（JSON格式）
     * 格式: [{"type": "NULL_HANDLE", "order": 1, "config": {"strategy": "KEEP"}}, 
     *        {"type": "CLEANSE", "order": 2, "config": {}}, 
     *        {"type": "TRANSFORM", "order": 3, "config": {}}]
     * 支持类型: NULL_HANDLE(空值处理), CLEANSE(数据清洗), TRANSFORM(数据转换)
     * 按order字段排序执行，支持用户自定义顺序
     */
    private String processorChain;

    /**
     * 排序序号
     */
    private Integer sortOrder;

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
