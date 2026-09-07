package com.datapush.manager.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.datapush.manager.entity.TaskTarget;
import org.apache.ibatis.annotations.Mapper;

/**
 * 任务目标配置Mapper接口
 *
 * @author Data Push Team
 * @since 2024-11-24
 */
@Mapper
public interface TaskTargetMapper extends BaseMapper<TaskTarget> {
}
