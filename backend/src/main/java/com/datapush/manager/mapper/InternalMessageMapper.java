package com.datapush.manager.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.datapush.manager.entity.InternalMessage;
import org.apache.ibatis.annotations.Mapper;

/**
 * 内部消息Mapper
 */
@Mapper
public interface InternalMessageMapper extends BaseMapper<InternalMessage> {
}
