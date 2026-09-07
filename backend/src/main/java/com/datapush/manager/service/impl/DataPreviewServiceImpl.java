package com.datapush.manager.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.datapush.manager.connector.Connector;
import com.datapush.manager.connector.ConnectorFactory;
import com.datapush.manager.connector.impl.ApiConnector;
import com.datapush.manager.dto.TableInfo;
import com.datapush.manager.entity.ConnectorInfo;
import com.datapush.manager.service.ConnectorInfoService;
import com.datapush.manager.service.DataPreviewService;
import com.datapush.manager.util.EncryptUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.sql.*;
import java.util.*;

/**
 * 数据预览服务实现
 *
 * @author Data Push Team
 * @since 2024-11-20
 */
@Slf4j
@Service
public class DataPreviewServiceImpl implements DataPreviewService {

    @Autowired
    private ConnectorInfoService connectorInfoService;

    @Autowired
    private EncryptUtil encryptUtil;

    @Override
    public List<TableInfo> getTablesWithComment(Long connectorId) {
        ConnectorInfo connectorInfo = connectorInfoService.getById(connectorId);
        if (connectorInfo == null) {
            throw new RuntimeException("连接器不存在");
        }

        // 仅支持数据库类型
        if (!"DATABASE".equals(connectorInfo.getConnectorType())) {
            throw new RuntimeException("仅支持数据库类型连接器");
        }

        List<TableInfo> tables = new ArrayList<>();
        String jdbcUrl = buildJdbcUrl(connectorInfo);
        String username = connectorInfo.getUsername();
        String password = encryptUtil.decrypt(connectorInfo.getPassword());
        
        log.info("开始加载表列表(带注释) - 连接器ID: {}, 数据库类型: {}, 数据库名: {}", 
                 connectorId, connectorInfo.getDbType(), connectorInfo.getDatabaseName());

        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password)) {
            DatabaseMetaData metaData = conn.getMetaData();
            String catalog = conn.getCatalog();
            String schema = getSchemaByDbType(connectorInfo.getDbType(), connectorInfo.getDatabaseName(), username);
            
            log.info("数据库元信息 - Catalog: {}, Schema: {}, 数据库产品: {}", 
                     catalog, schema, metaData.getDatabaseProductName());

            String tablesSchema = schema; // schema已在getSchemaByDbType按类型处理
            
            // MySQL 需要通过查询 information_schema 获取表注释
            Map<String, String> tableComments = new HashMap<>();
            if ("MYSQL".equalsIgnoreCase(connectorInfo.getDbType())) {
                String commentSql = "SELECT TABLE_NAME, TABLE_COMMENT FROM information_schema.TABLES WHERE TABLE_SCHEMA = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(commentSql)) {
                    pstmt.setString(1, connectorInfo.getDatabaseName());
                    try (ResultSet commentRs = pstmt.executeQuery()) {
                        while (commentRs.next()) {
                            String tableName = commentRs.getString("TABLE_NAME");
                            String comment = commentRs.getString("TABLE_COMMENT");
                            if (comment != null && !comment.trim().isEmpty()) {
                                tableComments.put(tableName, comment);
                            }
                        }
                    }
                }
                log.info("从 information_schema 获取到 {} 个表注释", tableComments.size());
            }
            
            try (ResultSet rs = metaData.getTables(catalog, tablesSchema, "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    String tableName = rs.getString("TABLE_NAME");
                    String tableSchema = rs.getString("TABLE_SCHEM"); // 获取表所属的schema
                    
                    // 过滤达梦数据库的系统表
                    if ("DM".equalsIgnoreCase(connectorInfo.getDbType())) {
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
                    
                    String tableComment = "";
                    
                    // 优先使用从 information_schema 查询的注释
                    if (tableComments.containsKey(tableName)) {
                        tableComment = tableComments.get(tableName);
                    } else {
                        // 尝试从 REMARKS 获取（适用于其他数据库）
                        String remarks = rs.getString("REMARKS");
                        if (remarks != null && !remarks.trim().isEmpty()) {
                            tableComment = remarks;
                        }
                    }
                    
                    // 调试日志：输出每个表的注释
                    log.info("表名: {}, Schema: {}, 注释: [{}]", tableName, tableSchema, tableComment);
                    
                    tables.add(new TableInfo(tableName, tableComment));
                }
            }
            
            log.info("加载表列表成功 - 连接器ID: {}, 表数量: {}", connectorId, tables.size());
            
        } catch (SQLException e) {
            log.error("获取表列表失败 - 连接器ID: {}, 数据库类型: {}, 错误: {}", 
                     connectorId, connectorInfo.getDbType(), e.getMessage(), e);
            
            String errorMsg = "获取表列表失败";
            String sqlExceptionMsg = e.getMessage();
            
            if (sqlExceptionMsg != null) {
                // 驱动类不存在
                if (sqlExceptionMsg.contains("No suitable driver") || sqlExceptionMsg.contains("ClassNotFoundException")) {
                    errorMsg = "数据库驱动未找到，请确认已添加" + connectorInfo.getDbType() + "驱动jar包";
                }
                // 连接失败
                else if (sqlExceptionMsg.contains("Connection refused") || sqlExceptionMsg.contains("拒绝连接")) {
                    errorMsg = "数据库连接被拒绝，请检查主机和端口是否正确";
                }
                // 认证失败
                else if (sqlExceptionMsg.contains("Access denied") || sqlExceptionMsg.contains("访问被拒绝")) {
                    errorMsg = "数据库认证失败，请检查用户名和密码";
                }
                // 数据库不存在
                else if (sqlExceptionMsg.contains("Unknown database") || sqlExceptionMsg.contains("数据库不存在")) {
                    errorMsg = "数据库 '" + connectorInfo.getDatabaseName() + "' 不存在";
                }
                else {
                    errorMsg = "获取表列表失败: " + sqlExceptionMsg;
                }
            }
            
            throw new RuntimeException(errorMsg);
        }

        return tables;
    }

    @Override
    public List<String> getTables(Long connectorId) {
        ConnectorInfo connectorInfo = connectorInfoService.getById(connectorId);
        if (connectorInfo == null) {
            throw new RuntimeException("连接器不存在");
        }

        // 仅支持数据库类型
        if (!"DATABASE".equals(connectorInfo.getConnectorType())) {
            throw new RuntimeException("仅支持数据库类型连接器");
        }

        List<String> tables = new ArrayList<>();
        String jdbcUrl = buildJdbcUrl(connectorInfo);
        String username = connectorInfo.getUsername();
        String password = encryptUtil.decrypt(connectorInfo.getPassword());
        
        log.info("开始加载表列表 - 连接器ID: {}, 数据库类型: {}, 数据库名: {}", 
                 connectorId, connectorInfo.getDbType(), connectorInfo.getDatabaseName());

        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password)) {
            DatabaseMetaData metaData = conn.getMetaData();
            String catalog = conn.getCatalog();
            String schema = getSchemaByDbType(connectorInfo.getDbType(), connectorInfo.getDatabaseName(), username);
            
            log.info("数据库元信息 - Catalog: {}, Schema: {}, 数据库产品: {}", 
                     catalog, schema, metaData.getDatabaseProductName());

            String tablesSchema = schema; // schema已在getSchemaByDbType按类型处理
            try (ResultSet rs = metaData.getTables(catalog, tablesSchema, "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    String tableName = rs.getString("TABLE_NAME");
                    String tableSchema = rs.getString("TABLE_SCHEM"); // 获取表所属的schema
                    
                    // 过滤达梦数据库的系统表
                    if ("DM".equalsIgnoreCase(connectorInfo.getDbType())) {
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
            }
            
            log.info("加载表列表成功 - 连接器ID: {}, 表数量: {}", connectorId, tables.size());
            
        } catch (SQLException e) {
            log.error("获取表列表失败 - 连接器ID: {}, 数据库类型: {}, 错误: {}", 
                     connectorId, connectorInfo.getDbType(), e.getMessage(), e);
            
            String errorMsg = "获取表列表失败";
            String sqlExceptionMsg = e.getMessage();
            
            if (sqlExceptionMsg != null) {
                // 驱动类不存在
                if (sqlExceptionMsg.contains("No suitable driver") || sqlExceptionMsg.contains("ClassNotFoundException")) {
                    errorMsg = "数据库驱动未找到，请确认已添加" + connectorInfo.getDbType() + "驱动jar包";
                }
                // 连接失败
                else if (sqlExceptionMsg.contains("Connection refused") || sqlExceptionMsg.contains("拒绝连接")) {
                    errorMsg = "数据库连接被拒绝，请检查主机和端口是否正确";
                }
                // 认证失败
                else if (sqlExceptionMsg.contains("Access denied") || sqlExceptionMsg.contains("访问被拒绝")) {
                    errorMsg = "数据库认证失败，请检查用户名和密码";
                }
                // 数据库不存在
                else if (sqlExceptionMsg.contains("Unknown database") || sqlExceptionMsg.contains("数据库不存在")) {
                    errorMsg = "数据库 '" + connectorInfo.getDatabaseName() + "' 不存在";
                }
                else {
                    errorMsg = "获取表列表失败: " + sqlExceptionMsg;
                }
            }
            
            throw new RuntimeException(errorMsg);
        }

        return tables;
    }

    @Override
    public Map<String, Object> previewData(Long connectorId, String sql, Integer limit) {
        ConnectorInfo connectorInfo = connectorInfoService.getById(connectorId);
        if (connectorInfo == null) {
            throw new RuntimeException("连接器不存在");
        }

        if (!"DATABASE".equals(connectorInfo.getConnectorType())) {
            throw new RuntimeException("仅支持数据库类型连接器");
        }
        
        // 处理增量占位符（预览时总是移除增量条件，显示全量数据）
        sql = processIncrementalPlaceholder(sql);

        // 添加LIMIT限制(如果SQL中没有)
        String previewSql = addLimitToSql(sql, limit, connectorInfo.getDbType());

        List<String> columns = new ArrayList<>();
        List<Map<String, Object>> data = new ArrayList<>();

        String jdbcUrl = buildJdbcUrl(connectorInfo);
        String username = connectorInfo.getUsername();
        String password = encryptUtil.decrypt(connectorInfo.getPassword());

        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(previewSql)) {

            // 获取列信息
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            for (int i = 1; i <= columnCount; i++) {
                columns.add(metaData.getColumnLabel(i));
            }

            // 获取数据
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    row.put(metaData.getColumnLabel(i), rs.getObject(i));
                }
                data.add(row);
            }

        } catch (SQLException e) {
            log.error("预览数据失败 - SQL: {}, 错误: {}", sql, e.getMessage(), e);

            String errorMsg = buildSqlErrorMessage(e);
            throw new RuntimeException(errorMsg);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("columns", columns);
        result.put("data", data);
        result.put("total", data.size());

        return result;
    }

    /**
     * 构建统一的 SQL 错误提示信息
     */
    private String buildSqlErrorMessage(SQLException e) {
        String errorMsg = "SQL执行失败";
        String sqlExceptionMsg = e.getMessage();

        // 输出原始错误信息用于调试
        log.error("===== MySQL 原始错误信息: [{}] =====", sqlExceptionMsg);

        if (sqlExceptionMsg == null) {
            sqlExceptionMsg = "未知错误";
        }

        // 表不存在
        if (sqlExceptionMsg.contains("doesn't exist") || sqlExceptionMsg.contains("不存在")) {
            if (sqlExceptionMsg.contains("Table")) {
                // 提取表名
                int startIdx = sqlExceptionMsg.indexOf("Table '") + 7;
                int endIdx = sqlExceptionMsg.indexOf("'", startIdx);
                if (startIdx > 0 && endIdx > startIdx) {
                    String tableName = sqlExceptionMsg.substring(startIdx, endIdx);
                    errorMsg = String.format("表 '%s' 不存在，请点击'加载表列表'按钮查看可用表", tableName);
                } else {
                    errorMsg = "表不存在，请检查表名是否正确";
                }
            } else {
                errorMsg = "表或字段不存在，请检查SQL语句";
            }
        }
        // 字段不存在
        else if (sqlExceptionMsg.contains("Unknown column") || sqlExceptionMsg.contains("未知列")) {
            errorMsg = "字段不存在: " + sqlExceptionMsg;
        }
        // 语法错误 - 进行更详细的分析
        else if (sqlExceptionMsg.contains("syntax error") || sqlExceptionMsg.contains("语法错误")
                || sqlExceptionMsg.contains("You have an error in your SQL syntax")) {
            
            String upperMsg = sqlExceptionMsg.toUpperCase();
            
            // 最高优先级：JOIN 相关错误（必须在前面，避免被其他判断拦截）
            // JOIN 后缺少表名（检查 LEFT/RIGHT/INNER/OUTER/CROSS 后直接跟了其他关键字或语句末尾）
            if ((sqlExceptionMsg.contains("use near 'JOIN'") || sqlExceptionMsg.contains("use near 'join'")) ||
                (upperMsg.contains("LEFT") && sqlExceptionMsg.contains("use near 'JOIN'")) ||
                (upperMsg.contains("RIGHT") && sqlExceptionMsg.contains("use near 'JOIN'")) ||
                (upperMsg.contains("INNER") && sqlExceptionMsg.contains("use near 'JOIN'")) ||
                // JOIN 在语句末尾：系统会添加 LIMIT 1，所以错误信息会是 "use near 'LIMIT 1'"
                // 关键判断：错误信息同时包含 "LIMIT" 和其中一种 JOIN 关键词
                (upperMsg.contains("LIMIT") && 
                 (upperMsg.contains("JOIN") || upperMsg.contains("LEFT") || upperMsg.contains("RIGHT") || 
                  upperMsg.contains("INNER") || upperMsg.contains("OUTER") || upperMsg.contains("CROSS")))) {
                errorMsg = "SQL语法错误: JOIN 关键字后必须指定要关联的表名。正确格式: LEFT JOIN 表名 ON 条件";
            }
            // JOIN 语法错误（JOIN 和 WHERE 之间缺少内容）
            else if (upperMsg.contains("JOIN") && 
                (sqlExceptionMsg.contains("use near 'WHERE'") || sqlExceptionMsg.contains("use near 'where'"))) {
                errorMsg = "SQL语法错误: JOIN 子句不完整，缺少表名或 ON 关联条件。正确格式: LEFT JOIN 表名 ON t1.字段 = t2.字段";
            }
            // JOIN 缺少 ON 条件
            else if (upperMsg.contains("JOIN") && 
                     (sqlExceptionMsg.contains("use near 'LEFT'") || sqlExceptionMsg.contains("use near 'RIGHT'") ||
                      sqlExceptionMsg.contains("use near 'INNER'"))) {
                errorMsg = "SQL语法错误: JOIN 后缺少 ON 关联条件。正确格式: JOIN 表名 ON t1.字段 = t2.字段";
            }
            // 缺少 SELECT 关键字（场景：* FROM tab 或 1 FROM tab 或 abc FROM tab）
            else if ((sqlExceptionMsg.contains("use near '*'") || 
                     (sqlExceptionMsg.contains("use near '") && !upperMsg.contains("SELECT"))) && 
                    upperMsg.contains("FROM")) {
                errorMsg = "SQL语法错误: SELECT 语句必须以 SELECT 关键字开头。正确格式: SELECT * FROM 表名";
            }
            // SELECT 后缺少字段（SELECT 后直接跟 FROM）
            else if (sqlExceptionMsg.contains("use near 'FROM'") || sqlExceptionMsg.contains("use near 'from'")) {
                errorMsg = "SQL语法错误: SELECT 后缺少字段列表。正确格式: SELECT 字段1, 字段2 FROM 表名 或 SELECT * FROM 表名";
            }
            // 缺少 FROM 子句（检查 SELECT 后直接跟WHERE/LIMIT/ORDER BY/GROUP BY）
            else if (sqlExceptionMsg.contains("right syntax to use near") && 
                (sqlExceptionMsg.contains("WHERE") || sqlExceptionMsg.contains("LIMIT") || 
                 sqlExceptionMsg.contains("ORDER") || sqlExceptionMsg.contains("GROUP"))) {
                // 提取出问题的位置
                // 注意：JOIN 相关错误已经在前面处理，这里不再检查 JOIN
                if ((upperMsg.contains("WHERE") || upperMsg.contains("LIMIT") || 
                     upperMsg.contains("ORDER") || upperMsg.contains("GROUP")) && 
                    !upperMsg.contains(" FROM ")) {
                    errorMsg = "SQL语法错误: 缺少 FROM 子句。正确格式: SELECT * FROM 表名 WHERE 条件";
                } else {
                    errorMsg = "SQL语法错误: 请检查 SELECT 和 WHERE/ORDER BY/GROUP BY 之间是否缺少 FROM 子句";
                }
            }
            // ORDER BY 后缺少字段
            else if (sqlExceptionMsg.contains("use near 'ORDER BY'") || 
                     (sqlExceptionMsg.toUpperCase().contains("ORDER BY") && 
                      (sqlExceptionMsg.contains("use near 'LIMIT'") || sqlExceptionMsg.contains("use near 'UNION'") ||
                       sqlExceptionMsg.contains("use near ''"))) ) {
                errorMsg = "SQL语法错误: ORDER BY 后必须指定排序字段。正确格式: ORDER BY 字段名 ASC/DESC";
            }
            // GROUP BY 后缺少字段
            else if (sqlExceptionMsg.contains("use near 'GROUP BY'") ||
                     (sqlExceptionMsg.toUpperCase().contains("GROUP BY") && 
                      (sqlExceptionMsg.contains("use near 'ORDER'") || sqlExceptionMsg.contains("use near 'HAVING'") ||
                       sqlExceptionMsg.contains("use near 'LIMIT'"))) ) {
                errorMsg = "SQL语法错误: GROUP BY 后必须指定分组字段";
            }
            // WHERE 后缺少条件（检查 WHERE 后直接跟其他关键字）
            else if ((sqlExceptionMsg.contains("use near 'ORDER BY'") || sqlExceptionMsg.contains("use near 'GROUP BY'") ||
                      sqlExceptionMsg.contains("use near 'LIMIT'") || sqlExceptionMsg.contains("use near 'UNION'")) && 
                     sqlExceptionMsg.toUpperCase().contains("WHERE")) {
                errorMsg = "SQL语法错误: WHERE 后缺少筛选条件。正确格式: WHERE 字段 = 值";
            }
            // 逗号使用错误
            else if (sqlExceptionMsg.contains("use near ','")) {
                errorMsg = "SQL语法错误: 逗号位置不正确，请检查字段列表中是否有多余或缺少的逗号";
            }
            // 字段别名使用错误（缺少 AS 或空格）
            else if (sqlExceptionMsg.contains("use near") && 
                     (sqlExceptionMsg.contains("AS") || sqlExceptionMsg.contains("as"))) {
                errorMsg = "SQL语法错误: 字段别名格式不正确。正确格式: 字段名 AS 别名 或 字段名 别名";
            }
            // 比较运算符使用错误
            else if (sqlExceptionMsg.contains("use near '='") || 
                     sqlExceptionMsg.contains("use near '>'") || 
                     sqlExceptionMsg.contains("use near '<'")) {
                errorMsg = "SQL语法错误: 比较运算符使用不当，请检查运算符两边是否都有有效字段或值";
            }
            // AND/OR 逻辑运算符使用错误
            else if ((sqlExceptionMsg.contains("use near 'AND'") || sqlExceptionMsg.contains("use near 'OR'")) &&
                     sqlExceptionMsg.toUpperCase().contains("WHERE")) {
                errorMsg = "SQL语法错误: AND/OR 前后必须有完整的条件表达式";
            }
            // IN 子句格式错误
            else if (sqlExceptionMsg.contains("use near 'IN'") || 
                     (sqlExceptionMsg.toUpperCase().contains(" IN ") && sqlExceptionMsg.contains("use near '('")) ) {
                errorMsg = "SQL语法错误: IN 子句格式不正确。正确格式: 字段名 IN (值1, 值2, 值3)";
            }
            // BETWEEN 子句格式错误
            else if (sqlExceptionMsg.toUpperCase().contains("BETWEEN") && 
                     !sqlExceptionMsg.toUpperCase().contains(" AND ")) {
                errorMsg = "SQL语法错误: BETWEEN 子句格式不正确。正确格式: 字段名 BETWEEN 值1 AND 值2";
            }
            // LIKE 模糊查询格式错误
            else if (sqlExceptionMsg.contains("use near 'LIKE'") || sqlExceptionMsg.contains("use near 'like'")) {
                errorMsg = "SQL语法错误: LIKE 后必须跟字符串模式。正确格式: 字段名 LIKE '%关键词%'";
            }
            // 子查询格式错误
            else if (sqlExceptionMsg.contains("subquery") || sqlExceptionMsg.contains("Subquery")) {
                errorMsg = "SQL语法错误: 子查询格式不正确，请检查括号是否完整以及子查询是否返回单一值（使用 = 时）";
            }
            // CASE WHEN 语句错误
            else if (sqlExceptionMsg.toUpperCase().contains("CASE") && 
                     (sqlExceptionMsg.contains("use near 'WHEN'") || sqlExceptionMsg.contains("use near 'END'"))) {
                errorMsg = "SQL语法错误: CASE WHEN 语句格式不正确。正确格式: CASE WHEN 条件 THEN 值 ELSE 默认值 END";
            }
            // DISTINCT 使用错误
            else if (sqlExceptionMsg.contains("use near 'DISTINCT'") || sqlExceptionMsg.contains("use near 'distinct'")) {
                errorMsg = "SQL语法错误: DISTINCT 必须紧跟在 SELECT 之后，且后面必须有字段列表";
            }
            // UNION 语句错误
            else if (sqlExceptionMsg.toUpperCase().contains("UNION")) {
                errorMsg = "SQL语法错误: UNION 前后的 SELECT 语句字段数量和类型必须一致";
            }
            // 引号不匹配
            else if (sqlExceptionMsg.contains("''") || sqlExceptionMsg.contains("unclosed") || 
                     sqlExceptionMsg.contains("unterminated")) {
                errorMsg = "SQL语法错误: 引号不匹配，请检查单引号或双引号是否成对出现";
            }
            // 括号不匹配
            else if (sqlExceptionMsg.contains("(") || sqlExceptionMsg.contains(")")) {
                errorMsg = "SQL语法错误: 括号不匹配，请检查左右括号数量是否相等";
            }
            // 保留关键字作为标识符
            else if (sqlExceptionMsg.contains("reserved keyword") || sqlExceptionMsg.contains("reserved word")) {
                errorMsg = "SQL语法错误: 使用了数据库保留关键字作为表名或字段名，请用反引号包裹（如：`order`、`table`）";
            }
            // 数据类型不匹配
            else if (sqlExceptionMsg.contains("Incorrect") && 
                     (sqlExceptionMsg.contains("integer") || sqlExceptionMsg.contains("string") || 
                      sqlExceptionMsg.contains("datetime"))) {
                errorMsg = "SQL语法错误: 数据类型不匹配，请检查字段类型和比较值是否一致（如：数字字段不要加引号）";
            }
            // 关键字拼写错误或位置错误
            else if (sqlExceptionMsg.contains("use near '")) {
                // 尝试提取错误位置附近的内容
                int nearIdx = sqlExceptionMsg.indexOf("use near '");
                if (nearIdx > 0) {
                    int startIdx = nearIdx + 10;
                    int endIdx = sqlExceptionMsg.indexOf("'", startIdx);
                    if (endIdx > startIdx && endIdx - startIdx < 50) {
                        String errorPart = sqlExceptionMsg.substring(startIdx, endIdx);
                        errorMsg = String.format("SQL语法错误: '%s' 附近有语法问题，请检查关键字拼写或语句结构", errorPart);
                    } else {
                        errorMsg = "SQL语法错误: 请检查SQL语句的语法是否正确";
                    }
                } else {
                    errorMsg = "SQL语法错误: 请检查SQL语句的语法是否正确";
                }
            }
            // 其他语法错误，只返回简化的提示
            else {
                errorMsg = "SQL语法错误: 请检查SQL语句格式是否正确（常见问题：缺少FROM子句、JOIN语法不完整、关键字拼写错误、符号不匹配、聚合函数使用不当）";
            }
        }
        // 表不存在
        else if (sqlExceptionMsg.contains("Table") && 
                 (sqlExceptionMsg.contains("doesn't exist") || sqlExceptionMsg.contains("不存在"))) {
            // 提取表名
            int tableIdx = sqlExceptionMsg.indexOf("Table '");
            if (tableIdx > 0) {
                int startIdx = tableIdx + 7;
                int endIdx = sqlExceptionMsg.indexOf("'", startIdx);
                if (endIdx > startIdx) {
                    String tableName = sqlExceptionMsg.substring(startIdx, endIdx);
                    errorMsg = String.format("表不存在: '%s'。请检查表名是否正确，或者该表是否已被删除", tableName);
                } else {
                    errorMsg = "表不存在，请检查表名是否正确";
                }
            } else {
                errorMsg = "表不存在，请检查表名是否正确";
            }
        }
        // 字段不存在
        else if ((sqlExceptionMsg.contains("Unknown column") || sqlExceptionMsg.contains("未知列")) &&
                 !sqlExceptionMsg.contains("is ambiguous")) {
            // 提取字段名
            int columnIdx = sqlExceptionMsg.indexOf("Unknown column '");
            if (columnIdx > 0) {
                int startIdx = columnIdx + 16;
                int endIdx = sqlExceptionMsg.indexOf("'", startIdx);
                if (endIdx > startIdx) {
                    String columnName = sqlExceptionMsg.substring(startIdx, endIdx);
                    errorMsg = String.format("字段不存在: '%s'。请检查字段名是否正确，或者该字段是否存在于指定的表中", columnName);
                } else {
                    errorMsg = "字段不存在，请检查字段名是否正确";
                }
            } else {
                errorMsg = "字段不存在，请检查字段名是否正确";
            }
        }
        // 模糊列(常见于JOIN查询)
        else if (sqlExceptionMsg.contains("Column") && sqlExceptionMsg.contains("is ambiguous")) {
            errorMsg = "字段名模糊不清，请在JOIN查询中使用表别名指明字段所属表（如: t1.id, t2.name）\n错误详情: " + sqlExceptionMsg;
        }
        // 访问权限
        else if (sqlExceptionMsg.contains("Access denied") || sqlExceptionMsg.contains("拒绝访问")) {
            errorMsg = "数据库访问权限不足: " + sqlExceptionMsg;
        }
        // 连接超时
        else if (sqlExceptionMsg.contains("timeout") || sqlExceptionMsg.contains("超时")) {
            errorMsg = "数据库连接超时，请检查网络或SQL执行时间";
        }
        // 其他错误
        else {
            errorMsg = "SQL执行失败: " + sqlExceptionMsg;
        }

        return errorMsg;
    }

    @Override
    public Map<String, Object> validateSql(Long connectorId, String sql) {
        Map<String, Object> result = new HashMap<>();
        result.put("valid", false);
        result.put("sql", sql);

        if (connectorId == null) {
            result.put("errorMessage", "连接器ID不能为空");
            return result;
        }
        if (sql == null || sql.trim().isEmpty()) {
            result.put("errorMessage", "SQL不能为空");
            return result;
        }

        ConnectorInfo connectorInfo = connectorInfoService.getById(connectorId);
        if (connectorInfo == null) {
            result.put("errorMessage", "连接器不存在，请检查配置");
            return result;
        }
        if (!"DATABASE".equals(connectorInfo.getConnectorType())) {
            result.put("errorMessage", "当前连接器类型不支持SQL校验");
            return result;
        }

        String dbType = connectorInfo.getDbType();
        String processedSql = processIncrementalPlaceholder(sql);
        String validateSql = addLimitToSql(processedSql, 1, dbType);
        result.put("sql", validateSql);

        String jdbcUrl = buildJdbcUrl(connectorInfo);
        String username = connectorInfo.getUsername();
        String password = encryptUtil.decrypt(connectorInfo.getPassword());

        Connection connection = null;
        Statement stmt = null;
        try {
            connection = DriverManager.getConnection(jdbcUrl, username, password);
            stmt = connection.createStatement();
            stmt.execute(validateSql);

            result.put("valid", true);
            result.put("errorMessage", "");
            return result;
        } catch (SQLException e) {
            log.error("SQL校验失败 - SQL: {}, 错误: {}", validateSql, e.getMessage(), e);
            String errorMsg = buildSqlErrorMessage(e);
            result.put("errorMessage", errorMsg);
            return result;
        } catch (Exception e) {
            log.error("SQL校验发生未知错误 - SQL: {}, 错误: {}", validateSql, e.getMessage(), e);
            result.put("errorMessage", "SQL校验失败: " + (e.getMessage() == null ? "未知错误" : e.getMessage()));
            return result;
        } finally {
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException ignore) {}
            }
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException ignore) {}
            }
        }
    }


    /**
     * 构建JDBC URL
     */
    private String buildJdbcUrl(ConnectorInfo connectorInfo) {
        String dbType = connectorInfo.getDbType();
        String host = connectorInfo.getHost();
        Integer port = connectorInfo.getPort();
        String databaseName = connectorInfo.getDatabaseName();

        switch (dbType.toUpperCase()) {
            case "MYSQL":
                return String.format("jdbc:mysql://%s:%d/%s?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai",
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
                return String.format("jdbc:dm://%s:%d/%s", host, port, databaseName);
            default:
                throw new RuntimeException("不支持的数据库类型: " + dbType);
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

    /**
     * 为SQL添加LIMIT限制
     * 支持单表查询和JOIN多表查询
     */
    private String addLimitToSql(String sql, Integer limit, String dbType) {
        // 移除末尾的分号和空格
        sql = sql.trim();
        
        // 处理SQL末尾：移除分号和注释
        // 情兵1: GROUP BY xxx;  -- 注释
        // 情兵2: GROUP BY xxx; 
        // 情兵3: GROUP BY xxx
        
        // 先移除单行注释（-- 开头的部分）
        String[] lines = sql.split("\n");
        StringBuilder cleanSql = new StringBuilder();
        for (String line : lines) {
            String trimmedLine = line.trim();
            // 如果是纯注释行，跳过
            if (trimmedLine.startsWith("--")) {
                continue;
            }
            // 如果行中包含注释，移除注释部分
            int commentIndex = line.indexOf("--");
            if (commentIndex > 0) {
                line = line.substring(0, commentIndex).trim();
            }
            cleanSql.append(line).append(" ");
        }
        
        sql = cleanSql.toString().trim();
        
        // 现在移除末尾分号
        if (sql.endsWith(";")) {
            sql = sql.substring(0, sql.length() - 1).trim();
        }
        
        String upperSql = sql.toUpperCase();
        
        // 更精确的LIMIT检测：只检查在最后一个关键子句之后是否有LIMIT
        // 不能简单的contains，因为可能在子查询或注释中有LIMIT
        boolean hasLimit = false;
        
        // 检查是否在主查询的最后部分有LIMIT/TOP/ROWNUM
        // 匹配模式：LIMIT\s+\d+ 或 TOP\s+\d+ 或 ROWNUM\s*<=\s*\d+
        if (upperSql.matches(".*\\bLIMIT\\s+\\d+\\s*$") ||
            upperSql.matches(".*\\bTOP\\s+\\d+\\b.*") ||
            upperSql.matches(".*\\bROWNUM\\s*<=\\s*\\d+.*")) {
            hasLimit = true;
        }
        
        // 如果已经有LIMIT，直接返回
        if (hasLimit) {
            log.info("检测到SQL已包含LIMIT限制，跳过添加");
            return sql;
        }

        // 根据数据库类型添加LIMIT
        String limitedSql;
        switch (dbType.toUpperCase()) {
            case "MYSQL":
            case "POSTGRESQL":
            case "KINGBASE":
                // MySQL/PostgreSQL/金仓: 直接在SQL末尾添加LIMIT，支持JOIN查询
                limitedSql = sql + " LIMIT " + limit;
                break;
            case "ORACLE":
                // Oracle: 使用子查询包裹，支持JOIN查询
                limitedSql = "SELECT * FROM (" + sql + ") WHERE ROWNUM <= " + limit;
                break;
            case "SQLSERVER":
                // SQL Server: 在SELECT后添加TOP，支持JOIN查询
                limitedSql = sql.replaceFirst("(?i)SELECT", "SELECT TOP " + limit);
                break;
            default:
                limitedSql = sql + " LIMIT " + limit;
                break;
        }
        
        log.info("为SQL添加LIMIT限制: {} 条", limit);
        log.debug("原始SQL: {}", sql);
        log.debug("添加LIMIT后SQL: {}", limitedSql);
        
        return limitedSql;
    }

    @Override
    public Map<String, Object> previewApiData(
            Long connectorId, 
            String apiPath, 
            String apiMethod, 
            Map<String, String> params,
            Map<String, String> headers,
            String bodyType,
            Object requestBody,
            String dataPath, 
            Integer limit) {
        
        ConnectorInfo connectorInfo = connectorInfoService.getById(connectorId);
        if (connectorInfo == null) {
            throw new RuntimeException("连接器不存在");
        }

        if (!"API".equals(connectorInfo.getConnectorType())) {
            throw new RuntimeException("仅支持API类型连接器");
        }

        try {
            // 合并临时Headers到连接器配置
            if (headers != null && !headers.isEmpty()) {
                JSONObject extraConfig = new JSONObject();
                if (StringUtils.hasText(connectorInfo.getExtraConfig())) {
                    try {
                        extraConfig = JSON.parseObject(connectorInfo.getExtraConfig());
                    } catch (Exception e) {
                        log.warn("解析extra_config失败，使用空配置", e);
                    }
                }
                
                // 合并headers
                JSONObject existingHeaders = extraConfig.getJSONObject("headers");
                if (existingHeaders == null) {
                    existingHeaders = new JSONObject();
                }
                existingHeaders.putAll(headers);
                extraConfig.put("headers", existingHeaders);
                
                // 更新连接器信息（不保存到数据库，只是临时使用）
                connectorInfo.setExtraConfig(extraConfig.toJSONString());
            }
            
            // 调用API获取数据
            ApiConnector apiConnector = new ApiConnector();
            JSONObject apiResponse;
            
            if (params == null) {
                params = new HashMap<>();
            }
            
            // 根据请求方法调用不同的API方法
            if ("GET".equalsIgnoreCase(apiMethod)) {
                apiResponse = apiConnector.get(connectorInfo, apiPath, params);
            } else if ("POST".equalsIgnoreCase(apiMethod)) {
                // POST请求：params拼接到URL，requestBody作为请求体
                String urlWithParams = buildUrlWithParams(apiPath, params);
                apiResponse = apiConnector.post(connectorInfo, urlWithParams, requestBody);
            } else if ("PUT".equalsIgnoreCase(apiMethod)) {
                String urlWithParams = buildUrlWithParams(apiPath, params);
                apiResponse = apiConnector.put(connectorInfo, urlWithParams, requestBody);
            } else if ("PATCH".equalsIgnoreCase(apiMethod)) {
                String urlWithParams = buildUrlWithParams(apiPath, params);
                apiResponse = apiConnector.patch(connectorInfo, urlWithParams, requestBody);
            } else if ("DELETE".equalsIgnoreCase(apiMethod)) {
                // DELETE请求,参数放在URL中
                apiResponse = apiConnector.delete(connectorInfo, apiPath, params);
            } else {
                throw new RuntimeException("不支持的请求方法: " + apiMethod);
            }

            // 解析数据路径
            Object data = apiResponse;
            if (StringUtils.hasText(dataPath)) {
                String[] paths = dataPath.split("\\.");
                for (String path : paths) {
                    if (data instanceof JSONObject) {
                        data = ((JSONObject) data).get(path);
                    }
                }
            }

            // 转换为列表
            List<Map<String, Object>> resultList = new ArrayList<>();
            if (data instanceof JSONArray) {
                JSONArray array = (JSONArray) data;
                int maxSize = Math.min(array.size(), limit);
                for (int i = 0; i < maxSize; i++) {
                    Object item = array.get(i);
                    if (item instanceof JSONObject) {
                        resultList.add(new HashMap<>(((JSONObject) item).getInnerMap()));
                    }
                }
            } else if (data instanceof JSONObject) {
                resultList.add(new HashMap<>(((JSONObject) data).getInnerMap()));
            }

            // 提取列名
            List<String> columns = new ArrayList<>();
            if (!resultList.isEmpty()) {
                columns.addAll(resultList.get(0).keySet());
            }

            Map<String, Object> result = new HashMap<>();
            result.put("columns", columns);
            result.put("data", resultList);
            result.put("total", resultList.size());

            return result;

        } catch (Exception e) {
            log.error("预览API数据失败: {}", e.getMessage(), e);
            throw new RuntimeException("API请求失败: " + e.getMessage());
        }
    }
    
    /**
     * 处理增量占位符（预览时移除增量条件，显示全量数据）
     * 支持两种形式：
     * 1. WHERE update_time > '{last_sync_time}' -- 移除整个WHERE子句
     * 2. ... AND update_time > '{last_sync_time}' -- 只移除AND部分
     */
    private String processIncrementalPlaceholder(String sql) {
        if (sql == null || !sql.contains("{last_sync_time}")) {
            return sql;
        }
        
        // 移除增量条件（AND形式）
        sql = sql.replaceAll("(?i)\\s+AND\\s+.*?\\{last_sync_time\\}[^)]*", "");
        
        // 移除增量条件（WHERE形式）
        sql = sql.replaceAll("(?i)\\s+WHERE\\s+.*?\\{last_sync_time\\}[^)]*", "");
        
        log.info("预览模式: 已移除增量条件，执行全量SQL");
        return sql;
    }
    
    /**
     * 构建带参数的URL（将params拼接到URL上）
     */
    private String buildUrlWithParams(String apiPath, Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return apiPath;
        }
        
        StringBuilder url = new StringBuilder(apiPath);
        boolean first = !apiPath.contains("?");
        
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (first) {
                url.append("?");
                first = false;
            } else {
                url.append("&");
            }
            url.append(entry.getKey()).append("=").append(entry.getValue());
        }
        
        return url.toString();
    }
}
