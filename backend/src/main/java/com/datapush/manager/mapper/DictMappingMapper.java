package com.datapush.manager.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.datapush.manager.entity.DictMapping;
import org.apache.ibatis.annotations.Mapper;

/**
 * 字典映射关系 Mapper
 * 
 * @author Data Push Team
 * @since 2024-11-21
 */
@Mapper
public interface DictMappingMapper extends BaseMapper<DictMapping> {
}
