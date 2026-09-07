package com.datapush.manager.connector.config;

import lombok.Data;

/**
 * API批量推送配置
 */
@Data
public class BatchConfig {
    
    /**
     * 批次大小
     */
    private Integer batchSize = 100;
    
    /**
     * 数据包裹字段(如: data)
     * 示例: {"data": [{},{}]} 而非 [{},{}]
     */
    private String wrapperField;
    
    /**
     * 并发批次数(1=串行, >1=并发)
     */
    private Integer concurrency = 1;
    
    /**
     * 重试次数
     */
    private Integer retryTimes = 3;
    
    /**
     * 重试间隔(毫秒)
     */
    private Long retryInterval = 1000L;
}
