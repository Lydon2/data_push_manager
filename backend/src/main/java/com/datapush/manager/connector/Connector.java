package com.datapush.manager.connector;

import com.datapush.manager.entity.ConnectorInfo;

/**
 * 连接器接口
 * 所有连接器实现此接口
 *
 * @author Data Push Team
 * @since 2024-11-20
 */
public interface Connector {

    /**
     * 测试连接
     *
     * @param connectorInfo 连接器信息
     * @return 连接结果
     */
    ConnectionTestResult testConnection(ConnectorInfo connectorInfo);

    /**
     * 获取连接器类型
     *
     * @return 连接器类型
     */
    String getConnectorType();

    /**
     * 连接测试结果
     */
    class ConnectionTestResult {
        private boolean success;
        private String message;

        public ConnectionTestResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public static ConnectionTestResult success(String message) {
            return new ConnectionTestResult(true, message);
        }

        public static ConnectionTestResult failed(String message) {
            return new ConnectionTestResult(false, message);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }
    }
}
