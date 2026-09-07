package com.datapush.manager.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.datapush.manager.dto.TableColumnInfo;
import com.datapush.manager.entity.ConnectorInfo;
import com.datapush.manager.entity.DictSource;
import com.datapush.manager.mapper.DictSourceMapper;
import com.datapush.manager.service.ConnectorInfoService;
import com.datapush.manager.service.DictSourceService;
import com.datapush.manager.util.EncryptUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.sql.*;
import java.util.*;

/**
 * 字典数据源服务实现
 * 
 * @author Data Push Team
 * @since 2024-11-21
 */
@Slf4j
@Service
public class DictSourceServiceImpl extends ServiceImpl<DictSourceMapper, DictSource> implements DictSourceService {
    
    @Autowired
    private ConnectorInfoService connectorInfoService;
    
    @Autowired
    private EncryptUtil encryptUtil;
    
    @Override
    public IPage<DictSource> pageList(Integer current, Integer size, String sourceName) {
        Page<DictSource> page = new Page<>(current, size);
        LambdaQueryWrapper<DictSource> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(sourceName)) {
            wrapper.like(DictSource::getSourceName, sourceName);
        }
        
        wrapper.orderByDesc(DictSource::getCreateTime);
        return page(page, wrapper);
    }
    
    @Override
    public List<DictSource> listAll() {
        LambdaQueryWrapper<DictSource> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DictSource::getStatus, 1);
        wrapper.orderByAsc(DictSource::getSourceName);
        return list(wrapper);
    }
    
    @Override
    public Map<String, Object> testConnection(Long id) {
        DictSource dictSource = getById(id);
        if (dictSource == null) {
            throw new RuntimeException("字典数据源不存在");
        }
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 获取连接器信息
            ConnectorInfo connector = connectorInfoService.getById(dictSource.getConnectorId());
            if (connector == null) {
                throw new RuntimeException("连接器不存在");
            }
            
            // 构建JDBC URL
            String jdbcUrl = buildJdbcUrl(connector);
            String username = connector.getUsername();
            String password = encryptUtil.decrypt(connector.getPassword());
            
            // 测试查询
            String testSql = buildTestSql(dictSource);
            
            try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password);
                 PreparedStatement stmt = conn.prepareStatement(testSql)) {
                
                stmt.setMaxRows(1);
                ResultSet rs = stmt.executeQuery();
                
                result.put("success", true);
                result.put("message", "连接成功！字典表可访问");
                
            }
        } catch (Exception e) {
            log.error("测试字典数据源连接失败", e);
            result.put("success", false);
            result.put("message", "连接失败: " + e.getMessage());
        }
        
        return result;
    }
    
    @Override
    public Map<String, Object> previewData(Long id, Integer limit) {
        DictSource dictSource = getById(id);
        if (dictSource == null) {
            throw new RuntimeException("字典数据源不存在");
        }
        
        if (limit == null || limit <= 0) {
            limit = 10;
        }
        
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> data = new ArrayList<>();
        
        try {
            // 获取连接器信息
            ConnectorInfo connector = connectorInfoService.getById(dictSource.getConnectorId());
            if (connector == null) {
                throw new RuntimeException("连接器不存在");
            }
            
            // 构建JDBC URL
            String jdbcUrl = buildJdbcUrl(connector);
            String username = connector.getUsername();
            String password = encryptUtil.decrypt(connector.getPassword());
            
            // 构建查询SQL
            String sql = buildQuerySql(dictSource, limit);
            
            try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password);
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                ResultSet rs = stmt.executeQuery();
                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();
                
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        row.put(metaData.getColumnName(i), rs.getObject(i));
                    }
                    data.add(row);
                }
            }
            
            result.put("success", true);
            result.put("data", data);
            result.put("total", data.size());
            
        } catch (Exception e) {
            log.error("预览字典数据失败", e);
            throw new RuntimeException("预览数据失败: " + e.getMessage());
        }
        
        return result;
    }
    
    @Override
    public Map<String, String> loadDictData(Long sourceId) {
        DictSource dictSource = getById(sourceId);
        if (dictSource == null) {
            throw new RuntimeException("字典数据源不存在");
        }
        
        Map<String, String> dictMap = new HashMap<>();
        
        try {
            // 获取连接器信息
            ConnectorInfo connector = connectorInfoService.getById(dictSource.getConnectorId());
            if (connector == null) {
                throw new RuntimeException("连接器不存在");
            }
            
            // 构建JDBC URL
            String jdbcUrl = buildJdbcUrl(connector);
            String username = connector.getUsername();
            String password = encryptUtil.decrypt(connector.getPassword());
            
            // 构建查询SQL（查询全部数据）
            String sql = buildQuerySql(dictSource, null);
            
            try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password);
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                ResultSet rs = stmt.executeQuery();
                
                while (rs.next()) {
                    String key = rs.getString(dictSource.getKeyField());
                    String value = rs.getString(dictSource.getValueField());
                    if (key != null) {
                        dictMap.put(key, value);  // 保持原样，Map<keyField, valueField>
                    }
                }
            }
            
            log.info("加载字典数据成功，数据源ID: {}, 数据条数: {}", sourceId, dictMap.size());
            
        } catch (Exception e) {
            log.error("加载字典数据失败", e);
            throw new RuntimeException("加载字典数据失败: " + e.getMessage());
        }
        
        return dictMap;
    }
    
    @Override
    public Map<String, String> loadDictDataByType(Long sourceId, String typeValue) {
        DictSource dictSource = getById(sourceId);
        if (dictSource == null) {
            throw new RuntimeException("字典数据源不存在");
        }
        
        // 如果没有配置类型字段，调用原有方法
        if (!StringUtils.hasText(dictSource.getTypeField()) || !StringUtils.hasText(typeValue)) {
            return loadDictData(sourceId);
        }
        
        Map<String, String> dictMap = new HashMap<>();
        
        try {
            // 获取连接器信息
            ConnectorInfo connector = connectorInfoService.getById(dictSource.getConnectorId());
            if (connector == null) {
                throw new RuntimeException("连接器不存在");
            }
            
            // 构建JDBC URL
            String jdbcUrl = buildJdbcUrl(connector);
            String username = connector.getUsername();
            String password = encryptUtil.decrypt(connector.getPassword());
            
            // 构建SQL，按类型过滤
            StringBuilder sql = new StringBuilder();
            sql.append("SELECT ").append(dictSource.getKeyField());
            sql.append(", ").append(dictSource.getValueField());
            sql.append(" FROM ").append(dictSource.getTableName());
            sql.append(" WHERE ").append(dictSource.getTypeField()).append(" = '").append(typeValue).append("'");
            
            try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password);
                 PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
                
                ResultSet rs = stmt.executeQuery();
                
                while (rs.next()) {
                    String key = rs.getString(dictSource.getKeyField());
                    String value = rs.getString(dictSource.getValueField());
                    if (key != null) {
                        dictMap.put(key, value);  // 保持原样，Map<keyField, valueField>
                    }
                }
            }
            
            log.info("加载指定类型字典数据成功，数据源ID: {}, 类型: {}, 数据条数: {}", sourceId, typeValue, dictMap.size());
            
        } catch (Exception e) {
            log.error("加载指定类型字典数据失败", e);
            throw new RuntimeException("加载指定类型字典数据失败: " + e.getMessage());
        }
        
        return dictMap;
    }
    
    @Override
    public List<String> getTablesByConnector(Long connectorId) {
        List<String> tables = new ArrayList<>();
        
        try {
            // 获取连接器信息
            ConnectorInfo connector = connectorInfoService.getById(connectorId);
            if (connector == null) {
                throw new RuntimeException("连接器不存在");
            }
            
            // 构建JDBC URL
            String jdbcUrl = buildJdbcUrl(connector);
            String username = connector.getUsername();
            String password = encryptUtil.decrypt(connector.getPassword());
            
            try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password)) {
                DatabaseMetaData metaData = conn.getMetaData();
                
                // 获取所有表
                String catalog = conn.getCatalog();
                String schema = getSchemaByDbType(connector.getDbType(), connector.getDatabaseName(),username);
                
                ResultSet rs = metaData.getTables(catalog, schema, "%", new String[]{"TABLE"});
                
                while (rs.next()) {
                    String tableName = rs.getString("TABLE_NAME");
                    String tableSchema = rs.getString("TABLE_SCHEM"); // 获取表所属的schema
                    
                    // 过滤达梦数据库的系统表
                    if ("DM".equalsIgnoreCase(connector.getDbType())) {
                        // 过滤以#开头的临时表和系统表
                        if (tableName.startsWith("#") || tableName.startsWith("SYS")) {
                            continue;
                        }
                        // 过滤系统schema下的表
                        if (tableSchema != null && (
                            tableSchema.startsWith("SYS") || 
                            "CTISYS".equals(tableSchema) ||
                            "SYSSSO".equals(tableSchema)
                        )) {
                            continue;
                        }
                    }
                    
                    tables.add(tableName);
                }
                
                rs.close();
            }
            
            log.info("获取表列表成功，连接器ID: {}, 表数: {}", connectorId, tables.size());
            
        } catch (Exception e) {
            log.error("获取表列表失败", e);
            throw new RuntimeException("获取表列表失败: " + e.getMessage());
        }
        
        return tables;
    }
    
    @Override
    public List<String> getTableColumns(Long connectorId, String tableName) {
        List<String> columns = new ArrayList<>();
        
        try {
            // 获取连接器信息
            ConnectorInfo connector = connectorInfoService.getById(connectorId);
            if (connector == null) {
                throw new RuntimeException("连接器不存在");
            }
            
            // 构建JDBC URL
            String jdbcUrl = buildJdbcUrl(connector);
            String username = connector.getUsername();
            String password = encryptUtil.decrypt(connector.getPassword());
            
            try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password)) {
                DatabaseMetaData metaData = conn.getMetaData();
                
                // 获取表字段
                String catalog = conn.getCatalog();

                // 其他数据库正常处理
                String schema = getSchemaByDbType(connector.getDbType(), connector.getDatabaseName(),username);
                ResultSet rs = metaData.getColumns(catalog, schema, tableName, "%");

                while (rs.next()) {
                    String columnName = rs.getString("COLUMN_NAME");
                    columns.add(columnName);
                }

                rs.close();

            }
            
            log.info("获取表字段成功，表名: {}, 字段数: {}", tableName, columns.size());
            
        } catch (Exception e) {
            log.error("获取表字段失败，表名: {}", tableName, e);
            throw new RuntimeException("获取表字段失败: " + e.getMessage());
        }
        
        return columns;
    }
    
    @Override
    public List<TableColumnInfo> getTableColumnsWithType(Long connectorId, String tableName) {
        List<TableColumnInfo> columns = new ArrayList<>();
        
        try {
            // 获取连接器信息
            ConnectorInfo connector = connectorInfoService.getById(connectorId);
            if (connector == null) {
                throw new RuntimeException("连接器不存在");
            }
            
            // 构建JDBC URL
            String jdbcUrl = buildJdbcUrl(connector);
            String username = connector.getUsername();
            String password = encryptUtil.decrypt(connector.getPassword());
            
            try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password)) {
                DatabaseMetaData metaData = conn.getMetaData();
                
                // 获取表字段
                String catalog = conn.getCatalog();
                

                // 其他数据库正常处理
                String schema = getSchemaByDbType(connector.getDbType(), connector.getDatabaseName(),username);
                ResultSet rs = metaData.getColumns(catalog, schema, tableName, "%");

                while (rs.next()) {
                    TableColumnInfo columnInfo = new TableColumnInfo();
                    columnInfo.setColumnName(rs.getString("COLUMN_NAME"));
                    columnInfo.setColumnType(rs.getString("TYPE_NAME"));
                    columnInfo.setColumnSize(rs.getInt("COLUMN_SIZE"));
                    columnInfo.setNullable(rs.getInt("NULLABLE") == DatabaseMetaData.columnNullable);
                    columnInfo.setRemarks(rs.getString("REMARKS"));

                    columns.add(columnInfo);
                }

                rs.close();

            }
            
            log.info("获取表字段及类型成功，表名: {}, 字段数: {}", tableName, columns.size());
            
        } catch (Exception e) {
            log.error("获取表字段及类型失败，表名: {}", tableName, e);
            throw new RuntimeException("获取表字段及类型失败: " + e.getMessage());
        }
        
        return columns;
    }
    
    /**
     * 根据数据库类型获取Schema
     */
    private String getSchemaByDbType(String dbType, String databaseName) {
        switch (dbType) {
            case "MYSQL":
            case "KINGBASE":
                return databaseName;
            case "ORACLE":
                return databaseName.toUpperCase();
            case "POSTGRESQL":
                return "public";
            case "SQLSERVER":
                return "dbo";
            default:
                return null;
        }
    }

    /**
     * 根据数据库类型获取Schema
     */
    private String getSchemaByDbType(String dbType, String databaseName, String username) {
        switch (dbType.toUpperCase()) {
            case "MYSQL":
                // MySQL不使用schema参数，传null
                return null;
            case "KINGBASE":
                // 金仓数据库默认使用public作为schema
                return "public";
            case "DM":
                // 达梦数据库：使用数据库名作为schema（大写）
                // 达梦数据库中，数据库名即模式名
                return databaseName != null ? databaseName.toUpperCase() : null;
            case "ORACLE":
                // Oracle使用用户名作为schema（大写）
                return username.toUpperCase();
            case "POSTGRESQL":
                // PostgreSQL默认schema为public
                return "public";
            case "SQLSERVER":
                // SQL Server默认schema为dbo
                return "dbo";
            default:
                return null;
        }
    }
    
    @Override
    public List<Map<String, Object>> getDistinctTypes(Long sourceId) {
        DictSource dictSource = getById(sourceId);
        if (dictSource == null) {
            throw new RuntimeException("字典数据源不存在");
        }
        
        // 如果没有配置类型字段，返回空列表
        if (!StringUtils.hasText(dictSource.getTypeField())) {
            return new ArrayList<>();
        }
        
        List<Map<String, Object>> types = new ArrayList<>();
        
        try {
            // 获取连接器信息
            ConnectorInfo connector = connectorInfoService.getById(dictSource.getConnectorId());
            if (connector == null) {
                throw new RuntimeException("连接器不存在");
            }
            
            // 构建JDBC URL
            String jdbcUrl = buildJdbcUrl(connector);
            String username = connector.getUsername();
            String password = encryptUtil.decrypt(connector.getPassword());
            
            // 构建查询SQL - 使用DISTINCT去重
            StringBuilder sql = new StringBuilder();
            sql.append("SELECT DISTINCT ").append(dictSource.getTypeField());
            
            // 如果有类型名称字段，也查询出来
            if (StringUtils.hasText(dictSource.getTypeLabelField())) {
                sql.append(", ").append(dictSource.getTypeLabelField());
            }
            
            sql.append(" FROM ").append(dictSource.getTableName());
            
            // 如果有类型值筛选，也加上
            if (StringUtils.hasText(dictSource.getTypeValue())) {
                sql.append(" WHERE ").append(dictSource.getTypeField())
                   .append(" = '").append(dictSource.getTypeValue()).append("'");
            }
            
            sql.append(" ORDER BY ").append(dictSource.getTypeField());
            
            try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password);
                 PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
                
                ResultSet rs = stmt.executeQuery();
                
                while (rs.next()) {
                    Map<String, Object> typeInfo = new HashMap<>();
                    Object typeValue = rs.getObject(dictSource.getTypeField());
                    
                    if (typeValue != null) {
                        typeInfo.put("value", typeValue);
                        
                        // 如果有类型名称字段，读取名称
                        if (StringUtils.hasText(dictSource.getTypeLabelField())) {
                            Object typeLabel = rs.getObject(dictSource.getTypeLabelField());
                            typeInfo.put("label", typeLabel != null ? typeLabel : typeValue);
                        } else {
                            typeInfo.put("label", typeValue);
                        }
                        
                        types.add(typeInfo);
                    }
                }
            }
            
            log.info("获取字典类型列表成功，数据源ID: {}, 类型数: {}", sourceId, types.size());
            
        } catch (Exception e) {
            log.error("获取字典类型列表失败", e);
            throw new RuntimeException("获取字典类型列表失败: " + e.getMessage());
        }
        
        return types;
    }
    
    @Override
    public List<Map<String, Object>> getDictItems(Long sourceId, String typeValue) {
        DictSource dictSource = getById(sourceId);
        if (dictSource == null) {
            throw new RuntimeException("字典数据源不存在");
        }
        
        List<Map<String, Object>> items = new ArrayList<>();
        
        try {
            // 获取连接器信息
            ConnectorInfo connector = connectorInfoService.getById(dictSource.getConnectorId());
            if (connector == null) {
                throw new RuntimeException("连接器不存在");
            }
            
            // 构建JDBC URL
            String jdbcUrl = buildJdbcUrl(connector);
            String username = connector.getUsername();
            String password = encryptUtil.decrypt(connector.getPassword());
            
            // 构建查询SQL
            StringBuilder sql = new StringBuilder();
            sql.append("SELECT ").append(dictSource.getKeyField());
            sql.append(", ").append(dictSource.getValueField());
            sql.append(" FROM ").append(dictSource.getTableName());
            
            // 添加WHERE条件
            List<String> conditions = new ArrayList<>();
            
            // 如果有typeValue参数，按类型过滤
            if (StringUtils.hasText(typeValue) && StringUtils.hasText(dictSource.getTypeField())) {
                conditions.add(dictSource.getTypeField() + " = '" + typeValue + "'");
            }
            // 如果数据源配置了typeValue，也加上
            else if (StringUtils.hasText(dictSource.getTypeValue()) && StringUtils.hasText(dictSource.getTypeField())) {
                conditions.add(dictSource.getTypeField() + " = '" + dictSource.getTypeValue() + "'");
            }
            
            if (!conditions.isEmpty()) {
                sql.append(" WHERE ").append(String.join(" AND ", conditions));
            }
            
            sql.append(" ORDER BY ").append(dictSource.getKeyField());
            
            try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password);
                 PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
                
                ResultSet rs = stmt.executeQuery();
                
                while (rs.next()) {
                    Map<String, Object> item = new HashMap<>();
                    Object key = rs.getObject(dictSource.getKeyField());
                    Object value = rs.getObject(dictSource.getValueField());
                    
                    if (key != null) {
                        // 注意：这里对调 key 和 value，因为前端期望 key=编码, value=名称
                        // 但数据源配置中 keyField=名称字段, valueField=编码字段
                        item.put("key", value != null ? value : "");   // value(编码) -> key
                        item.put("value", key);                          // key(名称) -> value
                        items.add(item);
                    }
                }
            }
            
            log.info("获取字典项成功，数据源ID: {}, 类型: {}, 项数: {}", sourceId, typeValue, items.size());
            
        } catch (Exception e) {
            log.error("获取字典项失败", e);
            throw new RuntimeException("获取字典项失败: " + e.getMessage());
        }
        
        return items;
    }
    
    /**
     * 构建JDBC URL
     */
    private String buildJdbcUrl(ConnectorInfo connector) {
        String dbType = connector.getDbType();
        String host = connector.getHost();
        Integer port = connector.getPort();
        String databaseName = connector.getDatabaseName();
        
        switch (dbType) {
            case "MYSQL":
                return String.format("jdbc:mysql://%s:%d/%s?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8", 
                    host, port, databaseName);
            case "ORACLE":
                return String.format("jdbc:oracle:thin:@%s:%d:%s", host, port, databaseName);
            case "POSTGRESQL":
                return String.format("jdbc:postgresql://%s:%d/%s", host, port, databaseName);
            case "SQLSERVER":
                return String.format("jdbc:sqlserver://%s:%d;databaseName=%s", host, port, databaseName);
            case "KINGBASE":
                return String.format("jdbc:kingbase8://%s:%d/%s", host, port, databaseName);
            case "DM":
                return String.format("jdbc:dm://%s:%d", host, port);
            default:
                throw new RuntimeException("不支持的数据库类型: " + dbType);
        }
    }
    
    /**
     * 构建测试SQL
     */
    private String buildTestSql(DictSource dictSource) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ").append(dictSource.getKeyField());
        sql.append(", ").append(dictSource.getValueField());
        sql.append(" FROM ").append(dictSource.getTableName());
        
        if (StringUtils.hasText(dictSource.getTypeField()) && StringUtils.hasText(dictSource.getTypeValue())) {
            sql.append(" WHERE ").append(dictSource.getTypeField()).append(" = '").append(dictSource.getTypeValue()).append("'");
        }
        
        sql.append(" LIMIT 1");
        return sql.toString();
    }
    
    /**
     * 构建查询SQL
     */
    private String buildQuerySql(DictSource dictSource, Integer limit) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ").append(dictSource.getKeyField());
        sql.append(", ").append(dictSource.getValueField());
        
        // 如果有类型字段，也查询出来
        if (StringUtils.hasText(dictSource.getTypeField())) {
            sql.append(", ").append(dictSource.getTypeField());
        }
        
        // 如果有类型名称字段，也查询出来
        if (StringUtils.hasText(dictSource.getTypeLabelField())) {
            sql.append(", ").append(dictSource.getTypeLabelField());
        }
        
        sql.append(" FROM ").append(dictSource.getTableName());
        
        if (StringUtils.hasText(dictSource.getTypeField()) && StringUtils.hasText(dictSource.getTypeValue())) {
            sql.append(" WHERE ").append(dictSource.getTypeField()).append(" = '").append(dictSource.getTypeValue()).append("'");
        }
        
        if (limit != null && limit > 0) {
            sql.append(" LIMIT ").append(limit);
        }
        
        return sql.toString();
    }
}
