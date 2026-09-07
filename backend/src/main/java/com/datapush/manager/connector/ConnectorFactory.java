package com.datapush.manager.connector;

import com.datapush.manager.connector.impl.ApiConnector;
import com.datapush.manager.connector.impl.DatabaseConnector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 连接器工厂
 * 根据类型获取对应的连接器实现
 *
 * @author Data Push Team
 * @since 2024-11-20
 */
@Component
public class ConnectorFactory {

    private final Map<String, Connector> connectorMap = new HashMap<>();

    @Autowired
    public ConnectorFactory(DatabaseConnector databaseConnector, ApiConnector apiConnector) {
        // 注册连接器
        connectorMap.put("DATABASE", databaseConnector);
        connectorMap.put("API", apiConnector);
        // 后续可扩展：文件连接器等
    }

    /**
     * 根据类型获取连接器
     *
     * @param connectorType 连接器类型
     * @return 连接器实例
     */
    public Connector getConnector(String connectorType) {
        Connector connector = connectorMap.get(connectorType.toUpperCase());
        if (connector == null) {
            throw new IllegalArgumentException("不支持的连接器类型: " + connectorType);
        }
        return connector;
    }
}
