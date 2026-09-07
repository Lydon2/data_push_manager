package com.datapush.manager.engine.loader;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

/**
 * MySQL LOAD DATA INFILE 高性能加载器
 * 使用文件批量导入，性能比INSERT快10-100倍
 * 
 * @author Data Push Team
 * @since 2024-12-05
 */
@Slf4j
public class MySQLLoadDataLoader {
    
    /**
     * 使用LOAD DATA INFILE批量导入数据
     * 
     * @param conn 数据库连接
     * @param tableName 表名
     * @param dataList 数据列表
     * @param fieldList 字段列表
     * @return 成功导入的记录数
     */
    public static int loadData(Connection conn, String tableName, 
                               List<Map<String, Object>> dataList, 
                               List<String> fieldList) throws Exception {
        
        if (dataList == null || dataList.isEmpty()) {
            return 0;
        }
        
        // 1. 生成临时CSV文件
        File tempFile = createTempCsvFile(dataList, fieldList);
        
        try {
            // 2. 执行LOAD DATA INFILE
            String sql = buildLoadDataSql(tableName, tempFile.getAbsolutePath(), fieldList);
            
            log.info("执行LOAD DATA INFILE: file={}, records={}", tempFile.getName(), dataList.size());
            
            try (Statement stmt = conn.createStatement()) {
                // 设置local_infile=1（允许客户端加载本地文件）
                stmt.execute("SET GLOBAL local_infile = 1");
                
                // 执行LOAD DATA
                stmt.execute(sql);
                
                log.info("LOAD DATA完成，导入 {} 条记录", dataList.size());
                return dataList.size();
            }
            
        } finally {
            // 3. 删除临时文件
            if (tempFile.exists()) {
                tempFile.delete();
            }
        }
    }
    
    /**
     * 创建临时CSV文件
     */
    private static File createTempCsvFile(List<Map<String, Object>> dataList, 
                                         List<String> fieldList) throws IOException {
        
        File tempFile = File.createTempFile("etl_load_", ".csv");
        
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(tempFile), StandardCharsets.UTF_8))) {
            
            for (Map<String, Object> row : dataList) {
                StringBuilder line = new StringBuilder();
                
                for (int i = 0; i < fieldList.size(); i++) {
                    if (i > 0) {
                        line.append("\t"); // 使用TAB分隔
                    }
                    
                    Object value = row.get(fieldList.get(i));
                    if (value != null) {
                        // 转义特殊字符
                        String strValue = value.toString()
                                .replace("\\", "\\\\")  // 反斜杠
                                .replace("\t", "\\t")   // TAB
                                .replace("\n", "\\n")   // 换行
                                .replace("\r", "\\r");  // 回车
                        line.append(strValue);
                    } else {
                        line.append("\\N"); // NULL值
                    }
                }
                
                line.append("\n");
                writer.write(line.toString());
            }
        }
        
        log.debug("临时CSV文件创建成功: {}, size={} bytes", tempFile.getName(), tempFile.length());
        return tempFile;
    }
    
    /**
     * 构建LOAD DATA SQL语句
     */
    private static String buildLoadDataSql(String tableName, String filePath, 
                                          List<String> fieldList) {
        
        // 将Windows路径转换为Unix风格（MySQL要求）
        String unixPath = filePath.replace("\\", "/");
        
        StringBuilder sql = new StringBuilder();
        sql.append("LOAD DATA LOCAL INFILE '").append(unixPath).append("' ");
        sql.append("INTO TABLE ").append(tableName).append(" ");
        sql.append("CHARACTER SET utf8mb4 ");
        sql.append("FIELDS TERMINATED BY '\\t' ");
        sql.append("LINES TERMINATED BY '\\n' ");
        sql.append("(").append(String.join(", ", fieldList)).append(")");
        
        return sql.toString();
    }
    
    /**
     * 判断是否适合使用LOAD DATA
     * 建议：数据量 > 10000条且表无索引时使用
     */
    public static boolean shouldUseLoadData(int dataSize, boolean hasIndex) {
        return dataSize > 10000 && !hasIndex;
    }
}
