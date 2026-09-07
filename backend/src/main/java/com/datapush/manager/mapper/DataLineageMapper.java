package com.datapush.manager.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.datapush.manager.entity.DataLineage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 数据血缘 Mapper
 */
@Mapper
public interface DataLineageMapper extends BaseMapper<DataLineage> {
    
    /**
     * 查询字段的上游血缘
     */
    @Select("SELECT * FROM dp_data_lineage WHERE target_connector_id = #{connectorId} AND target_table = #{tableName} AND target_field = #{fieldName} ORDER BY create_time DESC")
    List<DataLineage> selectUpstreamByField(@Param("connectorId") Long connectorId, 
                                            @Param("tableName") String tableName,
                                            @Param("fieldName") String fieldName);
    
    /**
     * 查询字段的下游血缘
     */
    @Select("SELECT * FROM dp_data_lineage WHERE source_connector_id = #{connectorId} AND source_table = #{tableName} AND source_field = #{fieldName} ORDER BY create_time DESC")
    List<DataLineage> selectDownstreamByField(@Param("connectorId") Long connectorId,
                                              @Param("tableName") String tableName,
                                              @Param("fieldName") String fieldName);
    
    /**
     * 查询任务的所有血缘关系
     */
    @Select("SELECT * FROM dp_data_lineage WHERE task_id = #{taskId} ORDER BY source_field, target_field")
    List<DataLineage> selectByTaskId(@Param("taskId") Long taskId);
    
    /**
     * 查询血缘链路（递归查询）
     * 返回字段流转路径
     */
    @Select("SELECT dl.*, tc.task_name FROM dp_data_lineage dl " +
            "LEFT JOIN dp_task_config tc ON dl.task_id = tc.id " +
            "WHERE dl.source_connector_id = #{connectorId} AND dl.source_table = #{tableName} AND dl.source_field = #{fieldName}")
    List<Map<String, Object>> selectLineageChain(@Param("connectorId") Long connectorId,
                                                 @Param("tableName") String tableName,
                                                 @Param("fieldName") String fieldName);
}
