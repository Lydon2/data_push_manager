package com.datapush.manager.transformer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 数据清洗函数定义
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CleanseFunction {
    
    /**
     * 函数唯一标识
     */
    private String functionCode;
    
    /**
     * 函数显示名称
     */
    private String functionName;
    
    /**
     * 函数分类：TEXT(文本处理), NUMBER(数值处理), DATE(日期处理), NULL(空值处理), CUSTOM(自定义)
     */
    private String category;
    
    /**
     * 函数描述
     */
    private String description;
    
    /**
     * 参数定义列表
     */
    private List<ParamDefinition> params;
    
    /**
     * 函数实现类型：BUILTIN(内置), GROOVY(脚本)
     */
    private String implementType;
    
    /**
     * 参数定义
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParamDefinition {
        /**
         * 参数名称
         */
        private String paramName;
        
        /**
         * 参数显示标签
         */
        private String paramLabel;
        
        /**
         * 参数类型：STRING, NUMBER, BOOLEAN, ENUM
         */
        private String paramType;
        
        /**
         * 是否必填
         */
        private Boolean required;
        
        /**
         * 默认值
         */
        private String defaultValue;
        
        /**
         * 枚举选项（当paramType=ENUM时）
         */
        private List<Map<String, String>> enumOptions;
        
        /**
         * 提示信息
         */
        private String placeholder;
    }
}
