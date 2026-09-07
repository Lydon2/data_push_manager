package com.datapush.manager.service;

import com.datapush.manager.dto.TableInfo;
import java.util.List;
import java.util.Map;

/**
 * 数据预览服务接口
 *
 * @author Data Push Team
 * @since 2024-11-20
 */
public interface DataPreviewService {

    /**
     * 获取连接器的表列表（包含表名和注释）
     *
     * @param connectorId 连接器ID
     * @return 表信息列表
     */
    List<TableInfo> getTablesWithComment(Long connectorId);

    /**
     * 获取连接器的表列表（仅表名）
     *
     * @param connectorId 连接器ID
     * @return 表名列表
     * @deprecated 使用 getTablesWithComment 替代
     */
    @Deprecated
    List<String> getTables(Long connectorId);

    /**
     * 预览SQL查询结果
     *
     * @param connectorId 连接器ID
     * @param sql SQL语句
     * @param limit 限制条数
     * @return 包含columns和data的Map
     */
    Map<String, Object> previewData(Long connectorId, String sql, Integer limit);

    /**
     * 校验 SQL 语法和基础可执行性（不返回实际数据）
     *
     * @param connectorId 连接器ID
     * @param sql SQL语句
     * @return 校验结果，包含是否有效及错误信息
     */
    Map<String, Object> validateSql(Long connectorId, String sql);

    /**
     * 预览API数据
     *
     * @param connectorId 连接器ID
     * @param apiPath API路径
     * @param apiMethod 请求方法
     * @param params 请求参数
     * @param dataPath 数据路径
     * @param limit 限制条数
     * @return 包含columns和data的Map
     */
    Map<String, Object> previewApiData(
        Long connectorId, 
        String apiPath, 
        String apiMethod, 
        Map<String, String> params,
        Map<String, String> headers,
        String bodyType,
        Object requestBody,
        String dataPath, 
        Integer limit
    );
}
