package com.datapush.manager.dto;

import lombok.Data;

/**
 * 转换预览请求DTO
 */
@Data
public class TransformPreviewRequest {
    
    /**
     * 转换类型: DIRECT, FUNCTION, DICT, SCRIPT, CONSTANT
     */
    private String transformType;
    
    /**
     * 测试值
     */
    private String testValue;
    
    /**
     * 函数转换表达式
     */
    private String transformFunction;
    
    /**
     * 脚本转换代码
     */
    private String transformScript;
    
    /**
     * 字典映射ID
     */
    private Long dictMappingId;
    
    /**
     * 字典源类型值
     */
    private String dictSourceTypeValue;
    
    /**
     * 字典默认值
     */
    private String defaultValue;
    
    /**
     * 固定值
     */
    private String constantValue;
}
