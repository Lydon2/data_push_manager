package com.datapush.manager.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.datapush.manager.entity.TaskExecuteLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 任务执行日志Mapper
 *
 * @author Data Push Team
 * @since 2024-11-20
 */
@Mapper
public interface TaskExecuteLogMapper extends BaseMapper<TaskExecuteLog> {

}
