package com.datapush.manager.dto;

import lombok.Data;

/**
 * 表字段信息DTO
 * 
 * @author Data Push Team
 * @since 2024-11-22
 */
@Data
public class TableColumnInfo {
    
    /**
     * 字段名
     */
    private String columnName;
    
    /**
     * 字段类型
     */
    private String columnType;
    
    /**
     * 字段大小
     */
    private Integer columnSize;
    
    /**
     * 是否可为空
     */
    private Boolean nullable;
    
    /**
     * 备注
     */
    private String remarks;
}
