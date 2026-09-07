package com.datapush.manager.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 字典数据源实体
 * 
 * @author Data Push Team
 * @since 2024-11-21
 */
@Data
@TableName("dp_dict_source")
public class DictSource {
    
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 数据源名称
     */
    private String sourceName;
    
    /**
     * 数据源编码（唯一）
     */
    private String sourceCode;
    
    /**
     * 关联连接器ID
     */
    private Long connectorId;
    
    /**
     * 字典表名
     */
    private String tableName;
    
    /**
     * 字典键字段名
     */
    private String keyField;
    
    /**
     * 字典值字段名
     */
    private String valueField;
    
    /**
     * 字典类型字段名（可选）
     */
    private String typeField;
    
    /**
     * 字典类型名称字段名（可选）
     */
    private String typeLabelField;
    
    /**
     * 字典类型值（用于筛选）
     */
    private String typeValue;
    
    /**
     * 描述
     */
    private String description;
    
    /**
     * 状态：1-启用 0-禁用
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
