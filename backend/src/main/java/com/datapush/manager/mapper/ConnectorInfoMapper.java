package com.datapush.manager.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.datapush.manager.entity.ConnectorInfo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 连接器信息Mapper
 *
 * @author Data Push Team
 * @since 2024-11-20
 */
@Mapper
public interface ConnectorInfoMapper extends BaseMapper<ConnectorInfo> {

}
