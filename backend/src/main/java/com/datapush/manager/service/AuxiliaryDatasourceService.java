package com.datapush.manager.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datapush.manager.entity.AuxiliaryDatasource;

import java.util.List;

/**
 * 辅助数据源服务接口
 */
public interface AuxiliaryDatasourceService extends IService<AuxiliaryDatasource> {
    
    /**
     * 根据任务ID查询启用的辅助数据源列表
     */
    List<AuxiliaryDatasource> listByTaskId(Long taskId);
    
    /**
     * 批量保存辅助数据源配置
     */
    boolean saveBatch(Long taskId, List<AuxiliaryDatasource> auxiliaryDatasources);
    
    /**
     * 根据任务ID删除所有辅助数据源
     *
     * @param taskId 任务ID
     */
    void deleteByTaskId(Long taskId);
}
