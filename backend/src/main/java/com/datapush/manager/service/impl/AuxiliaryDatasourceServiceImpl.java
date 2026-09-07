package com.datapush.manager.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.datapush.manager.entity.AuxiliaryDatasource;
import com.datapush.manager.mapper.AuxiliaryDatasourceMapper;
import com.datapush.manager.service.AuxiliaryDatasourceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 辅助数据源服务实现
 */
@Service
public class AuxiliaryDatasourceServiceImpl extends ServiceImpl<AuxiliaryDatasourceMapper, AuxiliaryDatasource> 
        implements AuxiliaryDatasourceService {
    
    @Override
    public List<AuxiliaryDatasource> listByTaskId(Long taskId) {
        LambdaQueryWrapper<AuxiliaryDatasource> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AuxiliaryDatasource::getTaskId, taskId)
               .eq(AuxiliaryDatasource::getEnabled, 1)
               .orderByAsc(AuxiliaryDatasource::getSortOrder);
        return list(wrapper);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveBatch(Long taskId, List<AuxiliaryDatasource> auxiliaryDatasources) {
        // 先删除该任务的所有辅助数据源
        LambdaQueryWrapper<AuxiliaryDatasource> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AuxiliaryDatasource::getTaskId, taskId);
        remove(wrapper);
        
        // 批量保存新的配置
        if (auxiliaryDatasources != null && !auxiliaryDatasources.isEmpty()) {
            for (AuxiliaryDatasource aux : auxiliaryDatasources) {
                aux.setTaskId(taskId);
            }
            return saveBatch(auxiliaryDatasources);
        }
        
        return true;
    }
    
    @Override
    public void deleteByTaskId(Long taskId) {
        LambdaQueryWrapper<AuxiliaryDatasource> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AuxiliaryDatasource::getTaskId, taskId);
        this.remove(wrapper);
    }
}
