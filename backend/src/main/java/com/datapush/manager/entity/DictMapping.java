package com.datapush.manager.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 字典映射关系实体
 * 
 * @author Data Push Team
 * @since 2024-11-21
 */
@Data
@TableName("dp_dict_mapping")
public class DictMapping {
    
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 映射名称
     */
    private String mappingName;
    
    /**
     * 映射编码（唯一）
     */
    private String mappingCode;
    
    /**
     * 映射类型：
     * SOURCE_TO_SOURCE - 源数据源 → 目标数据源
     * SOURCE_TO_CUSTOM - 源数据源 → 自定义目标
     * CUSTOM_TO_SOURCE - 自定义源 → 目标数据源
     * CUSTOM_TO_CUSTOM - 自定义源 → 自定义目标
     */
    private String mappingType;
    
    /**
     * 源字典数据源ID
     */
    private Long sourceDictId;
    
    /**
     * 目标字典数据源ID（自定义映射时为空）
     */
    private Long targetDictId;
    
    /**
     * 未匹配时的默认值
     */
    private String defaultValue;
    
    /**
     * 描述
     */
    private String description;
    
    /**
     * 源自定义项(JSON)，用于CUSTOM源场景
     */
    @TableField(value = "source_custom_items")
    private String sourceCustomItems;
    
    /**
     * 类型标签映射(JSON): {"source":{"typeValue":"label"}, "target":{"typeValue":"label"}}
     */
    @TableField(value = "type_labels")
    private String typeLabels;
    
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
