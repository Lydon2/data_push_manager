package com.datapush.manager.connector.result;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 批量推送结果
 */
@Data
public class BatchPushResult {
    
    /**
     * 总数
     */
    private Integer total;
    
    /**
     * 成功数
     */
    private Integer success;
    
    /**
     * 失败数
     */
    private Integer failed;
    
    /**
     * 失败记录详情
     */
    private List<ErrorRecord> errors = new ArrayList<>();
    
    /**
     * 错误记录
     */
    @Data
    public static class ErrorRecord {
        /**
         * 记录索引
         */
        private Integer index;
        
        /**
         * 记录数据
         */
        private Object data;
        
        /**
         * 错误信息
         */
        private String errorMessage;
    }
}
