package com.datapush.manager.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 连接器信息实体
 * 对应表：dp_connector_info
 *
 * @author Data Push Team
 * @since 2024-11-20
 */
@Data
@TableName("dp_connector_info")
public class ConnectorInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 连接器名称
     */
    private String connectorName;

    /**
     * 连接器类型：DATABASE/API/FILE
     */
    private String connectorType;

    /**
     * 数据库类型：MYSQL/ORACLE/POSTGRESQL/SQLSERVER/KINGBASE
     */
    private String dbType;

    /**
     * 主机地址
     */
    private String host;

    /**
     * 端口
     */
    private Integer port;

    /**
     * 数据库名
     */
    private String databaseName;

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码（加密存储）
     */
    private String password;

    /**
     * API地址或文件路径
     */
    private String url;

    /**
     * 认证类型：NONE/BASIC/BEARER/API_KEY/OAUTH2
     */
    private String authType;

    /**
     * 认证配置（JSON格式，加密存储）
     */
    private String authConfig;

    /**
     * 额外配置（JSON格式）
     */
    private String extraConfig;

    /**
     * 描述
     */
    private String description;

    /**
     * 状态：0-禁用 1-启用
     */
    private Integer status;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 逻辑删除：0-未删除 1-已删除
     */
    @TableLogic
    private Integer deleted;
}
