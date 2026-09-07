package com.datapush.manager.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datapush.manager.entity.ConnectorInfo;

/**
 * 连接器信息服务接口
 *
 * @author Data Push Team
 * @since 2024-11-20
 */
public interface ConnectorInfoService extends IService<ConnectorInfo> {

    /**
     * 测试连接器连接
     *
     * @param id 连接器ID
     * @return 连接是否成功
     */
    boolean testConnection(Long id);

    /**
     * 测试连接器连接（不保存）
     *
     * @param connectorInfo 连接器信息
     * @return 连接是否成功
     */
    boolean testConnection(ConnectorInfo connectorInfo);

    /**
     * 保存连接器（加密敏感信息）
     *
     * @param connectorInfo 连接器信息
     * @return 是否成功
     */
    boolean saveConnector(ConnectorInfo connectorInfo);

    /**
     * 更新连接器（加密敏感信息）
     *
     * @param connectorInfo 连接器信息
     * @return 是否成功
     */
    boolean updateConnector(ConnectorInfo connectorInfo);

    /**
     * 获取连接器详情（解密敏感信息）
     *
     * @param id 连接器ID
     * @return 连接器信息
     */
    ConnectorInfo getConnectorDetail(Long id);
}
