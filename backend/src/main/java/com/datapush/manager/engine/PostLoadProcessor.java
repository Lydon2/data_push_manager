package com.datapush.manager.engine;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.datapush.manager.entity.TaskConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 推送后处理器
 * 负责推送完成后的状态回写等操作
 */
@Slf4j
@Component
public class PostLoadProcessor {
    
    @Autowired
    private RestTemplate restTemplate;
    
    /**
     * 执行推送后处理
     */
    public void process(Connection sourceConn,
                       List<Map<String, Object>> sourceData,
                       List<Map<String, Object>> transformedData,
                       TaskConfig taskConfig,
                       int successCount,
                       StringBuilder logBuilder) {
        try {
            // 解析推送后处理配置
            String postLoadConfigStr = taskConfig.getPostLoadConfig();
            if (postLoadConfigStr == null || postLoadConfigStr.trim().isEmpty()) {
                return; // 未配置，跳过
            }
            
            JSONObject postLoadConfig = JSON.parseObject(postLoadConfigStr);
            JSONObject statusUpdateConfig = postLoadConfig.getJSONObject("statusUpdate");
            
            if (statusUpdateConfig == null || !statusUpdateConfig.getBooleanValue("enabled")) {
                return; // 未启用，跳过
            }
            
            logBuilder.append("[推送后处理] 执行状态回写...\n");
            
            String updateType = statusUpdateConfig.getString("updateType");
            
            if ("SQL".equals(updateType)) {
                // SQL方式回写
                executeStatusUpdateBySQL(sourceConn, sourceData, statusUpdateConfig, successCount, logBuilder);
            } else if ("API".equals(updateType)) {
                // API方式回写
                executeStatusUpdateByAPI(sourceData, statusUpdateConfig, successCount, logBuilder);
            }
            
        } catch (Exception e) {
            log.error("推送后处理失败", e);
            logBuilder.append("  - [警告] 状态回写失败: ").append(e.getMessage()).append("\n\n");
        }
    }
    
    /**
     * 通过SQL回写状态
     */
    private void executeStatusUpdateBySQL(Connection sourceConn,
                                         List<Map<String, Object>> sourceData,
                                         JSONObject config,
                                         int successCount,
                                         StringBuilder logBuilder) throws SQLException {
        
        String updateSql = config.getString("updateSql");
        String keyField = config.getString("keyField");
        int batchSize = config.getInteger("batchSize") != null ? config.getInteger("batchSize") : 100;
        
        if (updateSql == null || updateSql.trim().isEmpty()) {
            logBuilder.append("  - [警告] 未配置SQL语句，跳过状态回写\n\n");
            return;
        }
        
        // 提取成功的记录的主键值
        List<Object> keyValues = new ArrayList<>();
        for (Map<String, Object> row : sourceData) {
            Object keyValue = row.get(keyField);
            if (keyValue != null) {
                keyValues.add(keyValue);
            }
        }
        
        if (keyValues.isEmpty()) {
            logBuilder.append("  - [警告] 未找到主键字段[").append(keyField).append("]，跳过状态回写\n\n");
            return;
        }
        
        // 分批执行UPDATE
        int totalUpdated = 0;
        for (int i = 0; i < keyValues.size(); i += batchSize) {
            int end = Math.min(i + batchSize, keyValues.size());
            List<Object> batchKeys = keyValues.subList(i, end);
            
            // 构建IN条件
            StringBuilder inClause = new StringBuilder();
            for (int j = 0; j < batchKeys.size(); j++) {
                Object key = batchKeys.get(j);
                if (key instanceof String) {
                    inClause.append("'").append(key).append("'");
                } else {
                    inClause.append(key);
                }
                if (j < batchKeys.size() - 1) {
                    inClause.append(",");
                }
            }
            
            // 替换占位符
            String executeSql = updateSql.replace("{ids}", inClause.toString());
            
            try (Statement stmt = sourceConn.createStatement()) {
                int updated = stmt.executeUpdate(executeSql);
                totalUpdated += updated;
                log.debug("批次{}回写状态，更新行数: {}", (i / batchSize + 1), updated);
            }
        }
        
        logBuilder.append("  - 状态回写成功，更新记录数: ").append(totalUpdated).append("\n");
        logBuilder.append("  - 执行SQL示例: ").append(updateSql.replace("{ids}", "...")).append("\n\n");
    }
    
    /**
     * 通过API回写状态
     */
    private void executeStatusUpdateByAPI(List<Map<String, Object>> sourceData,
                                         JSONObject config,
                                         int successCount,
                                         StringBuilder logBuilder) {
        
        String apiUrl = config.getString("apiUrl");
        String method = config.getString("method");
        String keyField = config.getString("keyField");
        String bodyType = config.getString("bodyType");
        String jsonBody = config.getString("jsonBody");
        
        if (apiUrl == null || apiUrl.trim().isEmpty()) {
            logBuilder.append("  - [警告] 未配置API地址，跳过状态回写\n\n");
            return;
        }
        
        // 提取成功的记录的主键值
        List<Object> keyValues = new ArrayList<>();
        for (Map<String, Object> row : sourceData) {
            Object keyValue = row.get(keyField);
            if (keyValue != null) {
                keyValues.add(keyValue);
            }
        }
        
        try {
            // 构建请求头
            HttpHeaders headers = new HttpHeaders();
            
            // 应用自定义Headers
            JSONArray customHeaders = config.getJSONArray("headers");
            if (customHeaders != null) {
                for (int i = 0; i < customHeaders.size(); i++) {
                    JSONObject header = customHeaders.getJSONObject(i);
                    if (header.getBooleanValue("enabled")) {
                        String key = header.getString("key");
                        String value = header.getString("value");
                        if (key != null && !key.trim().isEmpty()) {
                            headers.set(key, value);
                        }
                    }
                }
            }
            
            // 根据bodyType设置Content-Type（如果自定义headers中没有设置）
            if ("raw".equals(bodyType) && !headers.containsKey("Content-Type")) {
                headers.set("Content-Type", "application/json");
            } else if ("form-data".equals(bodyType) && !headers.containsKey("Content-Type")) {
                headers.set("Content-Type", "application/x-www-form-urlencoded");
            }
            
            // 构建URL参数
            StringBuilder urlBuilder = new StringBuilder(apiUrl);
            JSONArray params = config.getJSONArray("params");
            if (params != null && params.size() > 0) {
                boolean hasParam = apiUrl.contains("?");
                for (int i = 0; i < params.size(); i++) {
                    JSONObject param = params.getJSONObject(i);
                    if (param.getBooleanValue("enabled")) {
                        String key = param.getString("key");
                        String value = param.getString("value");
                        if (key != null && !key.trim().isEmpty()) {
                            urlBuilder.append(hasParam ? "&" : "?");
                            urlBuilder.append(key).append("=").append(value != null ? value : "");
                            hasParam = true;
                        }
                    }
                }
            }
            String finalUrl = urlBuilder.toString();
            
            // 构建请求体
            String requestBodyStr;
            if ("none".equals(bodyType)) {
                // 无请求体
                requestBodyStr = null;
            } else if ("form-data".equals(bodyType)) {
                // form-data格式
                JSONArray formData = config.getJSONArray("formData");
                Map<String, String> formMap = new HashMap<>();
                if (formData != null) {
                    for (int i = 0; i < formData.size(); i++) {
                        JSONObject field = formData.getJSONObject(i);
                        if (field.getBooleanValue("enabled")) {
                            String key = field.getString("key");
                            String value = field.getString("value");
                            if (key != null && !key.trim().isEmpty()) {
                                formMap.put(key, value != null ? value : "");
                            }
                        }
                    }
                }
                // 添加ids字段
                formMap.put("ids", JSON.toJSONString(keyValues));
                formMap.put("timestamp", String.valueOf(System.currentTimeMillis()));
                
                // 转换为form-urlencoded格式
                StringBuilder formBody = new StringBuilder();
                for (Map.Entry<String, String> entry : formMap.entrySet()) {
                    if (formBody.length() > 0) formBody.append("&");
                    formBody.append(entry.getKey()).append("=").append(entry.getValue());
                }
                requestBodyStr = formBody.toString();
            } else if ("raw".equals(bodyType) && jsonBody != null && !jsonBody.trim().isEmpty()) {
                // 自定义JSON，直接使用（不再替换占位符）
                requestBodyStr = jsonBody;
            } else {
                // 默认JSON模板
                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("ids", keyValues);
                requestBody.put("status", "COMPLETED");
                requestBody.put("timestamp", System.currentTimeMillis());
                requestBodyStr = JSON.toJSONString(requestBody);
            }
            
            // 发送HTTP请求
            HttpEntity<String> entity = new HttpEntity<>(requestBodyStr, headers);
            HttpMethod httpMethod = HttpMethod.valueOf(method != null ? method : "POST");
            
            log.info("发送状态回写API请求: {} {}", httpMethod, finalUrl);
            
            ResponseEntity<String> response = restTemplate.exchange(finalUrl, httpMethod, entity, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                logBuilder.append("  - 通过API回写状态成功: ").append(finalUrl).append("\n");
                logBuilder.append("  - 响应状态码: ").append(response.getStatusCodeValue()).append("\n");
                logBuilder.append("  - 待更新记录数: ").append(keyValues.size()).append("\n\n");
            } else {
                logBuilder.append("  - [警告] API回写响应非2xx: ").append(response.getStatusCodeValue()).append("\n\n");
            }
            
        } catch (Exception e) {
            log.error("API状态回写失败", e);
            logBuilder.append("  - [错误] API回写失败: ").append(e.getMessage()).append("\n\n");
        }
    }
}
