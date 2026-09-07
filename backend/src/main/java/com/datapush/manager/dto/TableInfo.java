package com.datapush.manager.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 表信息DTO
 * 
 * @author Data Push Team
 * @since 2024-12-02
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TableInfo {
    
    /**
     * 表名
     */
    private String tableName;
    
    /**
     * 表注释
     */
    private String tableComment;
}
