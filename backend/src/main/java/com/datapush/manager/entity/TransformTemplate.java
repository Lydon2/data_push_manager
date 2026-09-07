package com.datapush.manager.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 数据转换模板
 */
@Data
@TableName("transform_template")
public class TransformTemplate {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 模板名称
     */
    private String templateName;
    
    /**
     * 模板编码
     */
    private String templateCode;
    
    /**
     * 模板描述
     */
    private String description;
    
    /**
     * 转换规则JSON (存储mappings数组)
     */
    private String mappingsJson;
    
    /**
     * 适用场景标签 (逗号分隔)
     */
    private String tags;
    
    /**
     * 使用次数
     */
    private Integer useCount;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
    
    /**
     * 创建人
     */
    private String createBy;
}
