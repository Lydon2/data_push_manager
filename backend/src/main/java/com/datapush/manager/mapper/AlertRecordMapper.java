package com.datapush.manager.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.datapush.manager.entity.AlertRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 告警记录Mapper
 */
@Mapper
public interface AlertRecordMapper extends BaseMapper<AlertRecord> {
}
