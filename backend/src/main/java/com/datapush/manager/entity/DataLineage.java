package com.datapush.manager.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 数据血缘关系表
 * 记录字段级别的数据流转关系
 */
@Data
@TableName("dp_data_lineage")
public class DataLineage implements Serializable {

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
     * 源连接器ID
     */
    private Long sourceConnectorId;

    /**
     * 源表名/API路径
     */
    private String sourceTable;

    /**
     * 源字段名
     */
    private String sourceField;

    /**
     * 源字段类型
     */
    private String sourceFieldType;

    /**
     * 目标连接器ID
     */
    private Long targetConnectorId;

    /**
     * 目标表名/API路径
     */
    private String targetTable;

    /**
     * 目标字段名
     */
    private String targetField;

    /**
     * 目标字段类型
     */
    private String targetFieldType;

    /**
     * 转换类型：DIRECT/FUNCTION/DICT/SCRIPT/CONDITION
     */
    private String transformType;

    /**
     * 转换规则描述
     */
    private String transformRule;

    /**
     * 依赖字段（JSON数组）：多字段联合转换时记录依赖关系
     */
    private String dependentFields;

    /**
     * 字段映射ID
     */
    private Long fieldMappingId;

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
