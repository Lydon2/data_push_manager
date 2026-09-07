package com.datapush.manager.engine.loader;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.util.List;
import java.util.Map;

/**
 * PostgreSQL COPY 高性能加载器
 * 使用COPY FROM STDIN批量导入，性能比INSERT快10-50倍
 * 
 * @author Data Push Team
 * @since 2024-12-05
 */
@Slf4j
public class PostgreSQLCopyLoader {
    
    /**
     * 使用COPY FROM STDIN批量导入数据
     * 使用反射调用PostgreSQL的CopyManager，避免编译时依赖
     * 
     * @param conn 数据库连接
     * @param tableName 表名
     * @param dataList 数据列表
     * @param fieldList 字段列表
     * @return 成功导入的记录数
     */
    public static long copyData(Connection conn, String tableName, 
                                List<Map<String, Object>> dataList, 
                                List<String> fieldList) throws Exception {
        
        if (dataList == null || dataList.isEmpty()) {
            return 0;
        }
        
        try {
            // 使用反射获取PostgreSQL的CopyManager
            Class<?> copyManagerClass = Class.forName("org.postgresql.copy.CopyManager");
            Class<?> baseConnectionClass = Class.forName("org.postgresql.core.BaseConnection");
            
            // 创建CopyManager实例
            Object copyManager = copyManagerClass.getConstructor(baseConnectionClass).newInstance(conn);
            
            // 构建COPY SQL
            String copySql = buildCopySql(tableName, fieldList);
            log.info("执行PostgreSQL COPY: table={}, records={}", tableName, dataList.size());
            
            // 调用copyIn方法
            Method copyInMethod = copyManagerClass.getMethod("copyIn", String.class, Reader.class);
            try (StringReader reader = new StringReader(buildCopyData(dataList, fieldList))) {
                long rowsInserted = (Long) copyInMethod.invoke(copyManager, copySql, reader);
                log.info("COPY完成，导入 {} 条记录", rowsInserted);
                return rowsInserted;
            }
        } catch (ClassNotFoundException e) {
            log.warn("PostgreSQL驱动不支持COPY功能，降级为普通批量插入");
            throw new UnsupportedOperationException("PostgreSQL COPY not supported", e);
        }
    }
    
    /**
     * 构建COPY SQL语句
     */
    private static String buildCopySql(String tableName, List<String> fieldList) {
        StringBuilder sql = new StringBuilder();
        sql.append("COPY ").append(tableName).append(" (");
        sql.append(String.join(", ", fieldList));
        sql.append(") FROM STDIN WITH (FORMAT CSV, DELIMITER '\t', NULL '\\N')");
        return sql.toString();
    }
    
    /**
     * 构建COPY数据内容（CSV格式）
     */
    private static String buildCopyData(List<Map<String, Object>> dataList, 
                                       List<String> fieldList) {
        StringBuilder data = new StringBuilder();
        
        for (Map<String, Object> row : dataList) {
            for (int i = 0; i < fieldList.size(); i++) {
                if (i > 0) {
                    data.append("\t");
                }
                
                Object value = row.get(fieldList.get(i));
                if (value != null) {
                    // 转义特殊字符
                    String strValue = value.toString()
                            .replace("\\", "\\\\")
                            .replace("\t", "\\t")
                            .replace("\n", "\\n")
                            .replace("\r", "\\r");
                    data.append(strValue);
                } else {
                    data.append("\\N"); // NULL值
                }
            }
            data.append("\n");
        }
        
        return data.toString();
    }
    
    /**
     * 判断是否适合使用COPY
     * 建议：数据量 > 5000条时使用
     */
    public static boolean shouldUseCopy(int dataSize) {
        return dataSize > 5000;
    }
}
