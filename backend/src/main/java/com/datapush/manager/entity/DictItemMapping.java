package com.datapush.manager.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 字典项映射实体
 * 
 * @author Data Push Team
 * @since 2024-11-21
 */
@Data
@TableName("dp_dict_item_mapping")
public class DictItemMapping {
    
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 关联映射关系ID
     */
    private Long mappingId;
    
    /**
     * 源字典类型值
     */
    private String sourceTypeValue;
    
    /**
     * 目标字典类型值
     */
    private String targetTypeValue;
    
    /**
     * 源字典键值
     */
    private String sourceKey;
    
    /**
     * 源字典标签（用于显示）
     */
    private String sourceLabel;
    
    /**
     * 目标字典键值
     */
    private String targetKey;
    
    /**
     * 目标字典标签（用于显示）
     */
    private String targetLabel;
    
    /**
     * 排序
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
