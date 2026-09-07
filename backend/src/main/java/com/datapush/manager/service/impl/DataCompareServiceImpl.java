package com.datapush.manager.service.impl;

import com.alibaba.fastjson.JSON;
import com.datapush.manager.connector.impl.DatabaseConnector;
import com.datapush.manager.entity.ConnectorInfo;
import com.datapush.manager.entity.FieldMapping;
import com.datapush.manager.entity.TaskConfig;
import com.datapush.manager.service.ConnectorInfoService;
import com.datapush.manager.service.DataCompareService;
import com.datapush.manager.service.FieldMappingService;
import com.datapush.manager.service.TaskConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 数据对比服务实现
 */
@Slf4j
@Service
public class DataCompareServiceImpl implements DataCompareService {
    
    @Autowired
    private TaskConfigService taskConfigService;
    
    @Autowired
    private ConnectorInfoService connectorInfoService;
    
    @Autowired
    private FieldMappingService fieldMappingService;
    
    @Autowired
    private DatabaseConnector databaseConnector;
    
    @Override
    public Map<String, Object> compareData(
            Long sourceConnectorId, String sourceTableOrSql,
            Long targetConnectorId, String targetTableOrSql,
            List<String> compareFields, List<String> keyFields) {
        
        log.info("开始数据对比，源: {}, 目标: {}", sourceTableOrSql, targetTableOrSql);
        
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("compareTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        
        try {
            // 获取连接器信息
            ConnectorInfo sourceConnector = connectorInfoService.getById(sourceConnectorId);
            ConnectorInfo targetConnector = connectorInfoService.getById(targetConnectorId);
            
            if (sourceConnector == null || targetConnector == null) {
                throw new RuntimeException("连接器不存在");
            }
            
            // 读取源端数据
            Map<String, Map<String, Object>> sourceDataMap = fetchData(sourceConnector, sourceTableOrSql, keyFields);
            int sourceCount = sourceDataMap.size();
            
            // 读取目标端数据
            Map<String, Map<String, Object>> targetDataMap = fetchData(targetConnector, targetTableOrSql, keyFields);
            int targetCount = targetDataMap.size();
            
            // 执行对比
            Map<String, Object> compareResult = performCompare(sourceDataMap, targetDataMap, compareFields);
            
            // 组装结果
            result.put("status", "SUCCESS");
            result.put("sourceCount", sourceCount);
            result.put("targetCount", targetCount);
            result.put("matchCount", compareResult.get("matchCount"));
            result.put("diffCount", compareResult.get("diffCount"));
            result.put("sourceOnlyCount", compareResult.get("sourceOnlyCount"));
            result.put("targetOnlyCount", compareResult.get("targetOnlyCount"));
            result.put("compareFields", compareFields);
            result.put("differences", compareResult.get("diffDetail"));  // 差异记录（兼容旧字段名）
            result.put("matchDetail", compareResult.get("matchDetail"));  // 匹配记录明细
            result.put("sourceOnlyDetail", compareResult.get("sourceOnlyDetail"));  // 仅源端存在明细
            result.put("targetOnlyDetail", compareResult.get("targetOnlyDetail"));  // 仅目标端存在明细
            
            log.info("数据对比完成，匹配: {}, 差异: {}", compareResult.get("matchCount"), compareResult.get("diffCount"));
            
        } catch (Exception e) {
            log.error("数据对比失败", e);
            result.put("status", "FAILED");
            result.put("errorMessage", e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 通用数据读取方法
     */
    private Map<String, Map<String, Object>> fetchData(ConnectorInfo connector, String tableOrSql, List<String> keyFields) throws Exception {
        Map<String, Map<String, Object>> dataMap = new LinkedHashMap<>();
        
        Connection conn = databaseConnector.getConnection(connector);
        
        try {
            // 判断是表名还是SQL
            String sql = tableOrSql.trim().toUpperCase().startsWith("SELECT") 
                ? tableOrSql 
                : "SELECT * FROM " + tableOrSql;
            
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                StringBuilder keyBuilder = new StringBuilder();
                
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnLabel(i);
                    Object value = rs.getObject(i);
                    row.put(columnName, value);
                }
                
                // 构建唯一键
                if (keyFields != null && !keyFields.isEmpty()) {
                    for (String keyField : keyFields) {
                        Object value = row.get(keyField);
                        keyBuilder.append(value != null ? value.toString() : "null").append("|");
                    }
                } else {
                    // 如果没有指定主键，使用前3个字段
                    for (int i = 1; i <= Math.min(3, columnCount); i++) {
                        Object value = rs.getObject(i);
                        keyBuilder.append(value != null ? value.toString() : "null").append("|");
                    }
                }
                
                String key = keyBuilder.toString();
                dataMap.put(key, row);
            }
            
            rs.close();
            stmt.close();
            
        } finally {
            conn.close();
        }
        
        return dataMap;
    }
    
    @Override
    public Map<String, Object> compareData(Long taskId, List<String> compareFields) {
        log.info("开始数据对比，任务ID: {}", taskId);
        
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("taskId", taskId);
        result.put("compareTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        
        try {
            // 1. 获取任务配置
            TaskConfig taskConfig = taskConfigService.getById(taskId);
            if (taskConfig == null) {
                result.put("status", "FAILED");
                result.put("errorMessage", "任务不存在");
                return result;
            }
            
            // 2. 获取连接器信息
            ConnectorInfo sourceConnector = connectorInfoService.getById(taskConfig.getSourceConnectorId());
            ConnectorInfo targetConnector = connectorInfoService.getById(taskConfig.getTargetConnectorId());
            
            // 3. 获取字段映射
            List<FieldMapping> fieldMappings = fieldMappingService.listByTaskId(taskId);
            
            // 4. 确定对比字段
            if (compareFields == null || compareFields.isEmpty()) {
                compareFields = fieldMappings.stream()
                    .map(FieldMapping::getTargetField)
                    .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
            }
            
            // 5. 读取源端数据
            Map<String, Map<String, Object>> sourceDataMap = fetchSourceData(taskConfig, sourceConnector, fieldMappings);
            int sourceCount = sourceDataMap.size();
            
            // 6. 读取目标端数据
            Map<String, Map<String, Object>> targetDataMap = fetchTargetData(taskConfig, targetConnector, compareFields);
            int targetCount = targetDataMap.size();
            
            // 7. 执行对比
            Map<String, Object> compareResult = performCompare(sourceDataMap, targetDataMap, compareFields);
            
            // 8. 组装结果
            result.put("status", "SUCCESS");
            result.put("sourceCount", sourceCount);
            result.put("targetCount", targetCount);
            result.put("matchCount", compareResult.get("matchCount"));
            result.put("diffCount", compareResult.get("diffCount"));
            result.put("sourceOnlyCount", compareResult.get("sourceOnlyCount"));
            result.put("targetOnlyCount", compareResult.get("targetOnlyCount"));
            result.put("compareFields", compareFields);
            result.put("diffDetail", compareResult.get("diffDetail"));  // 差异记录列表
            result.put("differences", compareResult.get("diffDetail"));  // 兼容旧字段名
            result.put("matchDetail", compareResult.get("matchDetail"));  // 匹配记录明细
            result.put("sourceOnlyDetail", compareResult.get("sourceOnlyDetail"));  // 仅源端存在明细
            result.put("targetOnlyDetail", compareResult.get("targetOnlyDetail"));  // 仅目标端存在明细
            result.put("accuracy", calculateAccuracy(sourceCount, targetCount, (int) compareResult.get("matchCount")));
            
            log.info("数据对比完成，匹配: {}, 差异: {}", compareResult.get("matchCount"), compareResult.get("diffCount"));
            
        } catch (Exception e) {
            log.error("数据对比失败", e);
            result.put("status", "FAILED");
            result.put("errorMessage", e.getMessage());
        }
        
        return result;
    }
    
    @Override
    public List<Map<String, Object>> getCompareHistory(Long taskId) {
        // TODO: 从dp_data_compare表查询历史记录
        // 这里简化处理，实际应该从数据库查询
        List<Map<String, Object>> history = new ArrayList<>();
        return history;
    }
    
    @Override
    public Map<String, Object> getCompareDetail(Long compareId) {
        // TODO: 查询对比详情
        Map<String, Object> detail = new LinkedHashMap<>();
        return detail;
    }
    
    @Override
    public List<Map<String, Object>> exportDiffData(Long compareId) {
        // TODO: 导出差异数据
        List<Map<String, Object>> diffData = new ArrayList<>();
        return diffData;
    }
    
    /**
     * 读取源端数据
     */
    private Map<String, Map<String, Object>> fetchSourceData(TaskConfig taskConfig, 
                                                             ConnectorInfo sourceConnector,
                                                             List<FieldMapping> fieldMappings) throws Exception {
        Map<String, Map<String, Object>> dataMap = new LinkedHashMap<>();
        
        Connection conn = databaseConnector.getConnection(sourceConnector);
        
        try {
            com.alibaba.fastjson.JSONObject sourceConfig = JSON.parseObject(taskConfig.getSourceConfig());
            String sql = sourceConfig.getString("sql");
            
            if (!StringUtils.hasText(sql)) {
                sql = "SELECT * FROM " + sourceConfig.getString("tableName");
            }
            
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                StringBuilder keyBuilder = new StringBuilder();
                
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnLabel(i);
                    Object value = rs.getObject(i);
                    row.put(columnName, value);
                    
                    // 使用前几个字段作为唯一键
                    if (i <= 3) {
                        keyBuilder.append(value).append("|");
                    }
                }
                
                String key = keyBuilder.toString();
                dataMap.put(key, row);
            }
            
            rs.close();
            stmt.close();
            
        } finally {
            conn.close();
        }
        
        return dataMap;
    }
    
    /**
     * 读取目标端数据
     */
    private Map<String, Map<String, Object>> fetchTargetData(TaskConfig taskConfig,
                                                             ConnectorInfo targetConnector,
                                                             List<String> compareFields) throws Exception {
        Map<String, Map<String, Object>> dataMap = new LinkedHashMap<>();
        
        Connection conn = databaseConnector.getConnection(targetConnector);
        
        try {
            com.alibaba.fastjson.JSONObject targetConfig = JSON.parseObject(taskConfig.getTargetConfig());
            String tableName = targetConfig.getString("tableName");
            
            String sql = "SELECT * FROM " + tableName;
            
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                StringBuilder keyBuilder = new StringBuilder();
                
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnLabel(i);
                    Object value = rs.getObject(i);
                    row.put(columnName, value);
                    
                    // 使用前几个字段作为唯一键
                    if (i <= 3) {
                        keyBuilder.append(value).append("|");
                    }
                }
                
                String key = keyBuilder.toString();
                dataMap.put(key, row);
            }
            
            rs.close();
            stmt.close();
            
        } finally {
            conn.close();
        }
        
        return dataMap;
    }
    
    /**
     * 执行对比
     */
    private Map<String, Object> performCompare(Map<String, Map<String, Object>> sourceDataMap,
                                               Map<String, Map<String, Object>> targetDataMap,
                                               List<String> compareFields) {
        Map<String, Object> result = new LinkedHashMap<>();
        
        int matchCount = 0;
        int diffCount = 0;
        int sourceOnlyCount = 0;
        int targetOnlyCount = 0;
        
        List<Map<String, Object>> diffList = new ArrayList<>();
        List<Map<String, Object>> matchList = new ArrayList<>();  // 新增：匹配记录列表
        List<Map<String, Object>> sourceOnlyList = new ArrayList<>();  // 新增：仅源端存在列表
        List<Map<String, Object>> targetOnlyList = new ArrayList<>();  // 新增：仅目标端存在列表
        
        // 遍历源端数据
        for (Map.Entry<String, Map<String, Object>> entry : sourceDataMap.entrySet()) {
            String key = entry.getKey();
            Map<String, Object> sourceRow = entry.getValue();
            
            if (targetDataMap.containsKey(key)) {
                // 源端和目标端都存在
                Map<String, Object> targetRow = targetDataMap.get(key);
                
                // 对比字段值
                boolean isMatch = true;
                Map<String, Object> diffFields = new LinkedHashMap<>();
                
                for (String field : compareFields) {
                    Object sourceValue = sourceRow.get(field);
                    Object targetValue = targetRow.get(field);
                    
                    if (!Objects.equals(sourceValue, targetValue)) {
                        isMatch = false;
                        Map<String, Object> fieldDiff = new LinkedHashMap<>();
                        fieldDiff.put("sourceValue", sourceValue);
                        fieldDiff.put("targetValue", targetValue);
                        diffFields.put(field, fieldDiff);
                    }
                }
                
                if (isMatch) {
                    matchCount++;
                    // 添加到匹配列表
                    Map<String, Object> matchRecord = new LinkedHashMap<>();
                    matchRecord.put("key", key);
                    matchRecord.put("type", "MATCH");
                    matchRecord.put("sourceRow", sourceRow);
                    matchRecord.put("targetRow", targetRow);
                    matchList.add(matchRecord);
                } else {
                    diffCount++;
                    Map<String, Object> diffRecord = new LinkedHashMap<>();
                    diffRecord.put("key", key);
                    diffRecord.put("type", "DIFF");
                    diffRecord.put("diffFields", diffFields);
                    diffRecord.put("sourceRow", sourceRow); // 添加完整的源端行数据
                    diffRecord.put("targetRow", targetRow); // 添加完整的目标端行数据
                    diffList.add(diffRecord);
                }
                
            } else {
                // 仅源端存在
                sourceOnlyCount++;
                Map<String, Object> sourceOnlyRecord = new LinkedHashMap<>();
                sourceOnlyRecord.put("key", key);
                sourceOnlyRecord.put("type", "SOURCE_ONLY");
                sourceOnlyRecord.put("sourceRow", sourceRow);
                sourceOnlyRecord.put("targetRow", null);
                sourceOnlyList.add(sourceOnlyRecord);
                // 同时添加到差异列表（保持兼容性）
                diffList.add(sourceOnlyRecord);
            }
        }
        
        // 查找仅目标端存在的记录
        for (Map.Entry<String, Map<String, Object>> entry : targetDataMap.entrySet()) {
            String key = entry.getKey();
            if (!sourceDataMap.containsKey(key)) {
                targetOnlyCount++;
                Map<String, Object> targetOnlyRecord = new LinkedHashMap<>();
                targetOnlyRecord.put("key", key);
                targetOnlyRecord.put("type", "TARGET_ONLY");
                targetOnlyRecord.put("sourceRow", null);
                targetOnlyRecord.put("targetRow", entry.getValue());
                targetOnlyList.add(targetOnlyRecord);
                // 同时添加到差异列表（保持兼容性）
                diffList.add(targetOnlyRecord);
            }
        }
        
        result.put("matchCount", matchCount);
        result.put("diffCount", diffCount);
        result.put("sourceOnlyCount", sourceOnlyCount);
        result.put("targetOnlyCount", targetOnlyCount);
        result.put("diffDetail", diffList);  // 所有差异记录（包括SOURCE_ONLY和TARGET_ONLY）
        result.put("matchDetail", matchList);  // 新增：所有匹配记录
        result.put("sourceOnlyDetail", sourceOnlyList);  // 新增：仅源端存在记录
        result.put("targetOnlyDetail", targetOnlyList);  // 新增：仅目标端存在记录
        
        return result;
    }
    
    /**
     * 计算准确率
     */
    private double calculateAccuracy(int sourceCount, int targetCount, int matchCount) {
        if (sourceCount == 0 && targetCount == 0) {
            return 100.0;
        }
        
        int total = Math.max(sourceCount, targetCount);
        return (matchCount * 100.0) / total;
    }
    
    @Override
    public Map<String, Object> getDifferencesPaged(Long compareId, Integer page, Integer pageSize, String diffType) {
        // 简单实现：先获取全部差异，然后分页
        List<Map<String, Object>> allDiffs = exportDiffData(compareId);
        
        // 根据类型筛选
        List<Map<String, Object>> filteredDiffs = allDiffs;
        if (diffType != null && !diffType.isEmpty()) {
            filteredDiffs = new ArrayList<>();
            for (Map<String, Object> diff : allDiffs) {
                String type = (String) diff.get("type");
                if (diffType.equals(type)) {
                    filteredDiffs.add(diff);
                }
            }
        }
        
        // 分页
        int total = filteredDiffs.size();
        int start = (page - 1) * pageSize;
        int end = Math.min(start + pageSize, total);
        
        List<Map<String, Object>> pagedData = filteredDiffs.subList(start, end);
        
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("total", total);
        result.put("totalPages", (int) Math.ceil((double) total / pageSize));
        result.put("data", pagedData);
        
        return result;
    }
    
    @Override
    public String exportDiffDataAsCSV(Long compareId) {
        List<Map<String, Object>> diffData = exportDiffData(compareId);
        
        if (diffData.isEmpty()) {
            return "";
        }
        
        StringBuilder csv = new StringBuilder();
        
        // 获取列名（从第一条数据中提取）
        Map<String, Object> firstRow = diffData.get(0);
        @SuppressWarnings("unchecked")
        Map<String, Object> firstData = (Map<String, Object>) firstRow.get("data");
        
        // CSV头：类型 + 数据列
        csv.append("\"Type\",");
        List<String> columnNames = new ArrayList<>(firstData.keySet());
        for (int i = 0; i < columnNames.size(); i++) {
            csv.append("\"").append(escapeCSV(columnNames.get(i))).append("\"");
            if (i < columnNames.size() - 1) csv.append(",");
        }
        csv.append("\n");
        
        // CSV数据行
        for (Map<String, Object> diff : diffData) {
            String type = (String) diff.get("type");
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) diff.get("data");
            
            csv.append("\"").append(escapeCSV(type)).append("\",");
            for (int i = 0; i < columnNames.size(); i++) {
                Object value = data.get(columnNames.get(i));
                String valueStr = value != null ? value.toString() : "";
                csv.append("\"").append(escapeCSV(valueStr)).append("\"");
                if (i < columnNames.size() - 1) csv.append(",");
            }
            csv.append("\n");
        }
        
        return csv.toString();
    }
    
    /**
     * 转CSV时的字符转义
     */
    private String escapeCSV(String value) {
        if (value == null) return "";
        // 如果包含双引号、逗号或换行符，需要转义
        return value.replace("\"", "\"\""); // 双引号转义
    }
}
