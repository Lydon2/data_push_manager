package com.datapush.manager.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.datapush.manager.entity.AlertRule;
import org.apache.ibatis.annotations.Mapper;

/**
 * 告警规则Mapper
 */
@Mapper
public interface AlertRuleMapper extends BaseMapper<AlertRule> {
}
