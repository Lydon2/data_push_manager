package com.datapush.manager.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.datapush.manager.entity.FieldMapping;
import com.datapush.manager.mapper.FieldMappingMapper;
import com.datapush.manager.service.FieldMappingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 字段映射服务实现
 *
 * @author Data Push Team
 * @since 2024-11-20
 */
@Slf4j
@Service
public class FieldMappingServiceImpl extends ServiceImpl<FieldMappingMapper, FieldMapping>
        implements FieldMappingService {

    @Override
    public List<FieldMapping> listByTaskId(Long taskId) {
        LambdaQueryWrapper<FieldMapping> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FieldMapping::getTaskId, taskId);
        wrapper.orderByAsc(FieldMapping::getSortOrder);
        return list(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveBatch(Long taskId, List<FieldMapping> mappings) {
        // 删除旧的映射
        LambdaQueryWrapper<FieldMapping> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FieldMapping::getTaskId, taskId);
        remove(wrapper);
        
        // 保存新的映射
        if (mappings != null && !mappings.isEmpty()) {
            for (int i = 0; i < mappings.size(); i++) {
                mappings.get(i).setTaskId(taskId);
                mappings.get(i).setSortOrder(i);
            }
            return saveBatch(mappings);
        }
        return true;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveFieldMappings(Long taskId, List<FieldMapping> mappings) {
        // 1. 删除该任务的旧映射
        deleteByTaskId(taskId);
        
        // 2. 批量插入新映射
        if (mappings != null && !mappings.isEmpty()) {
            for (int i = 0; i < mappings.size(); i++) {
                FieldMapping mapping = mappings.get(i);
                mapping.setTaskId(taskId);
                mapping.setSortOrder(i);
            }
            this.saveBatch(mappings);
            log.info("保存任务{}的字段映射，共{}个字段", taskId, mappings.size());
        }
    }
    
    @Override
    public List<FieldMapping> listByTaskIdAndTargetId(Long taskId, Long targetId) {
        LambdaQueryWrapper<FieldMapping> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FieldMapping::getTaskId, taskId)
               .eq(FieldMapping::getTargetId, targetId)
               .orderByAsc(FieldMapping::getSortOrder);
        return this.list(wrapper);
    }
    
    @Override
    public void deleteByTaskId(Long taskId) {
        LambdaQueryWrapper<FieldMapping> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FieldMapping::getTaskId, taskId);
        this.remove(wrapper);
    }
}
