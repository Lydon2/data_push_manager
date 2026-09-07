package com.datapush.manager.connector.config;

import lombok.Data;

/**
 * API分页配置
 */
@Data
public class PaginationConfig {
    
    /**
     * 页码参数名
     */
    private String pageParamName = "page";
    
    /**
     * 每页数量参数名
     */
    private String pageSizeParamName = "pageSize";
    
    /**
     * 起始页(有的API从0开始,有的从1开始)
     */
    private Integer startPage = 1;
    
    /**
     * 每页数量
     */
    private Integer pageSize = 100;
    
    /**
     * 最大页数(防止无限循环)
     */
    private Integer maxPages = 100;
    
    /**
     * 总数路径(如: data.total)
     */
    private String totalPath;
}
