package com.datapush.manager.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 辅助数据源配置实体
 */
@Data
@TableName("dp_auxiliary_datasource")
public class AuxiliaryDatasource {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 任务ID
     */
    private Long taskId;
    
    /**
     * 连接器ID
     */
    private Long connectorId;
    
    /**
     * 连接器类型：DATABASE/API
     */
    private String connectorType;
    
    /**
     * 数据源别名（用于字段引用）
     */
    private String alias;
    
    /**
     * 关联类型：LEFT/INNER
     */
    private String joinType;
    
    /**
     * 关联条件配置（JSON格式）
     * 例如：{"mainField":"user_id","auxField":"id"}
     */
    private String joinCondition;
    
    /**
     * 数据源配置（JSON格式）
     * DATABASE: {"sql":"SELECT ..."}
     * API: {"apiPath":"/users","apiMethod":"GET","dataPath":"data"}
     */
    private String config;
    
    /**
     * 是否启用
     */
    private Integer enabled;
    
    /**
     * 排序序号
     */
    private Integer sortOrder;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
