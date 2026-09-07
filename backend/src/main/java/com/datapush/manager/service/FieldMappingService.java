package com.datapush.manager.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datapush.manager.entity.FieldMapping;

import java.util.List;

/**
 * 字段映射服务接口
 *
 * @author Data Push Team
 * @since 2024-11-20
 */
public interface FieldMappingService extends IService<FieldMapping> {

    /**
     * 根据任务ID查询字段映射列表
     *
     * @param taskId 任务ID
     * @return 字段映射列表
     */
    List<FieldMapping> listByTaskId(Long taskId);

    /**
     * 批量保存字段映射
     *
     * @param taskId 任务ID
     * @param mappings 字段映射列表
     * @return 是否成功
     */
    boolean saveBatch(Long taskId, List<FieldMapping> mappings);
    
    /**
     * 批量保存字段映射（支持多目标）
     * 会删除旧的配置并创建新的
     *
     * @param taskId 任务ID
     * @param mappings 字段映射列表
     */
    void saveFieldMappings(Long taskId, List<FieldMapping> mappings);
    
    /**
     * 根据任务ID和目标ID查询
     *
     * @param taskId 任务ID
     * @param targetId 目标ID
     * @return 字段映射列表
     */
    List<FieldMapping> listByTaskIdAndTargetId(Long taskId, Long targetId);
    
    /**
     * 根据任务ID删除所有映射
     *
     * @param taskId 任务ID
     */
    void deleteByTaskId(Long taskId);
}
