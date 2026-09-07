package com.datapush.manager.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.datapush.manager.common.BusinessException;
import com.datapush.manager.connector.Connector;
import com.datapush.manager.connector.ConnectorFactory;
import com.datapush.manager.entity.ConnectorInfo;
import com.datapush.manager.mapper.ConnectorInfoMapper;
import com.datapush.manager.service.ConnectorInfoService;
import com.datapush.manager.util.EncryptUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 连接器信息服务实现
 *
 * @author Data Push Team
 * @since 2024-11-20
 */
@Slf4j
@Service
public class ConnectorInfoServiceImpl extends ServiceImpl<ConnectorInfoMapper, ConnectorInfo> 
        implements ConnectorInfoService {

    @Autowired
    private ConnectorFactory connectorFactory;

    @Autowired
    private EncryptUtil encryptUtil;

    @Override
    public boolean testConnection(Long id) {
        ConnectorInfo connectorInfo = getConnectorDetail(id);
        if (connectorInfo == null) {
            throw new BusinessException("连接器不存在");
        }
        Connector.ConnectionTestResult result = testConnectionWithResult(connectorInfo);
        if (!result.isSuccess()) {
            throw new BusinessException(result.getMessage());
        }
        return true;
    }

    @Override
    public boolean testConnection(ConnectorInfo connectorInfo) {
        Connector.ConnectionTestResult result = testConnectionWithResult(connectorInfo);
        if (!result.isSuccess()) {
            throw new BusinessException(result.getMessage());
        }
        return true;
    }

    /**
     * 测试连接并返回详细结果
     */
    private Connector.ConnectionTestResult testConnectionWithResult(ConnectorInfo connectorInfo) {
        try {
            Connector connector = connectorFactory.getConnector(connectorInfo.getConnectorType());
            return connector.testConnection(connectorInfo);
        } catch (Exception e) {
            log.error("测试连接失败", e);
            return Connector.ConnectionTestResult.failed("测试连接失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveConnector(ConnectorInfo connectorInfo) {
        // 加密敏感信息
        encryptSensitiveInfo(connectorInfo);
        return save(connectorInfo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateConnector(ConnectorInfo connectorInfo) {
        // 查询数据库中的旧数据
        ConnectorInfo oldInfo = getById(connectorInfo.getId());
        if (oldInfo == null) {
            throw new BusinessException("连接器不存在");
        }
        
        // 处理密码：比对密码是否修改
        if (StringUtils.hasText(connectorInfo.getPassword())) {
            // 解密旧密码
            String decryptedOldPassword = encryptUtil.decrypt(oldInfo.getPassword());
            // 如果密码与旧密码相同，说明未修改，使用旧的加密值
            if (decryptedOldPassword.equals(connectorInfo.getPassword())) {
                connectorInfo.setPassword(oldInfo.getPassword());
            } else {
                // 密码已修改，加密新密码
                connectorInfo.setPassword(encryptUtil.encrypt(connectorInfo.getPassword()));
            }
        } else {
            // 密码为空，保持旧密码
            connectorInfo.setPassword(oldInfo.getPassword());
        }
        
        // 处理认证配置
        if (StringUtils.hasText(connectorInfo.getAuthConfig())) {
            connectorInfo.setAuthConfig(encryptUtil.encrypt(connectorInfo.getAuthConfig()));
        }
        
        return updateById(connectorInfo);
    }

    @Override
    public ConnectorInfo getConnectorDetail(Long id) {
        ConnectorInfo connectorInfo = getById(id);
        if (connectorInfo != null) {
            // 解密敏感信息
            decryptSensitiveInfo(connectorInfo);
        }
        return connectorInfo;
    }

    /**
     * 加密敏感信息
     */
    private void encryptSensitiveInfo(ConnectorInfo connectorInfo) {
        if (StringUtils.hasText(connectorInfo.getPassword())) {
            connectorInfo.setPassword(encryptUtil.encrypt(connectorInfo.getPassword()));
        }
        if (StringUtils.hasText(connectorInfo.getAuthConfig())) {
            connectorInfo.setAuthConfig(encryptUtil.encrypt(connectorInfo.getAuthConfig()));
        }
    }

    /**
     * 解密敏感信息
     */
    private void decryptSensitiveInfo(ConnectorInfo connectorInfo) {
        if (StringUtils.hasText(connectorInfo.getPassword())) {
            try {
                connectorInfo.setPassword(encryptUtil.decrypt(connectorInfo.getPassword()));
            } catch (Exception e) {
                log.error("密码解密失败", e);
            }
        }
        if (StringUtils.hasText(connectorInfo.getAuthConfig())) {
            try {
                connectorInfo.setAuthConfig(encryptUtil.decrypt(connectorInfo.getAuthConfig()));
            } catch (Exception e) {
                log.error("认证配置解密失败", e);
            }
        }
    }
}
