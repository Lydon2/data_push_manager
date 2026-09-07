package com.datapush.manager.connector.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.datapush.manager.circuit.CircuitBreaker;
import com.datapush.manager.circuit.CircuitBreakerManager;
import com.datapush.manager.connector.Connector;
import com.datapush.manager.connector.config.BatchConfig;
import com.datapush.manager.connector.config.PaginationConfig;
import com.datapush.manager.connector.result.BatchPushResult;
import com.datapush.manager.entity.ConnectorInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * API连接器实现
 * 支持HTTP REST API调用、断路器熔断、健康检查
 */
@Slf4j
@Component
public class ApiConnector implements Connector {

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 健康检查 - 检查API是否可用
     */
    public boolean healthCheck(ConnectorInfo connectorInfo) {
        try {
            String host = connectorInfo.getHost();
            Integer port = connectorInfo.getPort();
            
            if (!StringUtils.hasText(host) || port == null) {
                return false;
            }
            
            // 短超时快速检查（2秒）
            try (java.net.Socket socket = new java.net.Socket()) {
                socket.connect(new java.net.InetSocketAddress(host, port), 2000);
                return true;
            }
        } catch (Exception e) {
            log.warn("API健康检查失败: {}:{}", connectorInfo.getHost(), connectorInfo.getPort(), e);
            return false;
        }
    }


    @Override
    public ConnectionTestResult testConnection(ConnectorInfo connectorInfo) {
        try {
            String host = connectorInfo.getHost();
            Integer port = connectorInfo.getPort();
            
            if (!StringUtils.hasText(host)) {
                return ConnectionTestResult.failed("主机地址不能为空");
            }
            if (port == null) {
                return ConnectionTestResult.failed("端口不能为空");
            }
            
            log.info("测试 API 连通性: {}:{}", host, port);
            
            // TCP 端口连通性测试（超时 5 秒）
            try (java.net.Socket socket = new java.net.Socket()) {
                socket.connect(new java.net.InetSocketAddress(host, port), 5000);
                String baseUrl = buildBaseUrl(connectorInfo);
                String detail = String.format("连接成功! 基础URL: %s", baseUrl);
                log.info(detail);
                return ConnectionTestResult.success(detail);
            } catch (java.net.SocketTimeoutException e) {
                log.error("连接超时: {}:{}", host, port);
                return ConnectionTestResult.failed(String.format("连接超时，无法访问 %s:%d", host, port));
            } catch (java.net.ConnectException e) {
                log.error("连接被拒绝: {}:{}", host, port);
                return ConnectionTestResult.failed(String.format("连接被拒绝，端口 %d 未开放或主机不可达", port));
            } catch (java.io.IOException e) {
                log.error("网络IO错误: {}:{}", host, port, e);
                return ConnectionTestResult.failed(String.format("网络错误: %s", e.getMessage()));
            }
            
        } catch (Exception e) {
            log.error("API连接测试失败", e);
            return ConnectionTestResult.failed("连接测试失败: " + e.getMessage());
        }
    }

    @Override
    public String getConnectorType() {
        return "API";
    }

    /**
     * 发送GET请求并获取JSON数据（带断路器保护）
     * 支持返回JSON对象或JSON数组，统一包装为JSONObject
     */
    public JSONObject get(ConnectorInfo connectorInfo, String path, Map<String, String> params) {
        try {
            // 合并公共参数
            Map<String, String> allParams = new HashMap<>();
            allParams.putAll(buildCommonParams(connectorInfo));
            if (params != null) {
                allParams.putAll(params);
            }
            
            String baseUrl = buildBaseUrl(connectorInfo);
            String url = buildUrl(baseUrl, path, allParams);
            HttpHeaders headers = buildHeaders(connectorInfo);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                String body = response.getBody();
                return parseJsonResponse(body);
            } else {
                throw new RuntimeException("API返回状态码: " + response.getStatusCodeValue());
            }
        } catch (Exception e) {
            log.error("API GET请求失败: {}", path, e);
            throw new RuntimeException("API请求失败: " + e.getMessage(), e);
        }
    }

    /**
     * 发送POST请求（带断路器保护）
     * 支持返回JSON对象或JSON数组，统一包装为JSONObject
     * 支持JSON和form-data两种格式
     */
    public JSONObject post(ConnectorInfo connectorInfo, String path, Object body) {
        try {
            // 添加公共参数到URL
            Map<String, String> commonParams = buildCommonParams(connectorInfo);
            String baseUrl = buildBaseUrl(connectorInfo);
            String url = buildUrl(baseUrl, path, commonParams);
            
            HttpHeaders headers = buildHeaders(connectorInfo);
            HttpEntity<?> entity;
            
            // 根据body类型决定Content-Type
            if (body == null) {
                // 无body
                if (headers.getContentType() == null) {
                    headers.setContentType(MediaType.APPLICATION_JSON);
                }
                entity = new HttpEntity<>("", headers);
            } else if (body instanceof Map) {
                // Map类型：检查是否所有value都是基本类型（String/Number/Boolean）
                Map<?, ?> map = (Map<?, ?>) body;
                boolean isSimpleMap = true;
                for (Object value : map.values()) {
                    if (value != null && 
                        !(value instanceof String) && 
                        !(value instanceof Number) && 
                        !(value instanceof Boolean)) {
                        isSimpleMap = false;
                        break;
                    }
                }
                
                if (isSimpleMap) {
                    // 简单Map（form-data）
                    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
                    @SuppressWarnings("unchecked")
                    Map<String, String> formData = new HashMap<>();
                    for (Map.Entry<?, ?> entry : map.entrySet()) {
                        formData.put(String.valueOf(entry.getKey()), 
                                    entry.getValue() == null ? "" : String.valueOf(entry.getValue()));
                    }
                    entity = new HttpEntity<>(formData, headers);
                } else {
                    // 复杂Map（JSON）
                    if (headers.getContentType() == null) {
                        headers.setContentType(MediaType.APPLICATION_JSON);
                    }
                    String jsonBody = JSON.toJSONString(body);
                    entity = new HttpEntity<>(jsonBody, headers);
                }
            } else {
                // 其他类型（JSON）
                if (headers.getContentType() == null) {
                    headers.setContentType(MediaType.APPLICATION_JSON);
                }
                String jsonBody = JSON.toJSONString(body);
                entity = new HttpEntity<>(jsonBody, headers);
            }

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                String respBody = response.getBody();
                return parseJsonResponse(respBody);
            } else {
                throw new RuntimeException("API返回状态码: " + response.getStatusCodeValue());
            }
        } catch (Exception e) {
            log.error("API POST请求失败: {}", path, e);
            throw new RuntimeException("API请求失败: " + e.getMessage(), e);
        }
    }

    /**
     * 发送PUT请求（带断路器保护）
     * 支持返回JSON对象或JSON数组，统一包装为JSONObject
     * 支持JSON和form-data两种格式
     */
    public JSONObject put(ConnectorInfo connectorInfo, String path, Object body) {
        try {
            Map<String, String> commonParams = buildCommonParams(connectorInfo);
            String baseUrl = buildBaseUrl(connectorInfo);
            String url = buildUrl(baseUrl, path, commonParams);
            
            HttpHeaders headers = buildHeaders(connectorInfo);
            HttpEntity<?> entity;
            
            // 根据body类型决定Content-Type
            if (body == null) {
                if (headers.getContentType() == null) {
                    headers.setContentType(MediaType.APPLICATION_JSON);
                }
                entity = new HttpEntity<>("", headers);
            } else if (body instanceof Map) {
                Map<?, ?> map = (Map<?, ?>) body;
                boolean isSimpleMap = true;
                for (Object value : map.values()) {
                    if (value != null && 
                        !(value instanceof String) && 
                        !(value instanceof Number) && 
                        !(value instanceof Boolean)) {
                        isSimpleMap = false;
                        break;
                    }
                }
                
                if (isSimpleMap) {
                    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
                    @SuppressWarnings("unchecked")
                    Map<String, String> formData = new HashMap<>();
                    for (Map.Entry<?, ?> entry : map.entrySet()) {
                        formData.put(String.valueOf(entry.getKey()), 
                                    entry.getValue() == null ? "" : String.valueOf(entry.getValue()));
                    }
                    entity = new HttpEntity<>(formData, headers);
                } else {
                    if (headers.getContentType() == null) {
                        headers.setContentType(MediaType.APPLICATION_JSON);
                    }
                    String jsonBody = JSON.toJSONString(body);
                    entity = new HttpEntity<>(jsonBody, headers);
                }
            } else {
                if (headers.getContentType() == null) {
                    headers.setContentType(MediaType.APPLICATION_JSON);
                }
                String jsonBody = JSON.toJSONString(body);
                entity = new HttpEntity<>(jsonBody, headers);
            }

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.PUT, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                String respBody = response.getBody();
                return parseJsonResponse(respBody);
            } else {
                throw new RuntimeException("API返回状态码: " + response.getStatusCodeValue());
            }
        } catch (Exception e) {
            log.error("API PUT请求失败: {}", path, e);
            throw new RuntimeException("API请求失败: " + e.getMessage(), e);
        }
    }

    /**
     * 发送PATCH请求（带断路器保护）
     * 支持返回JSON对象或JSON数组，统一包装为JSONObject
     * 支持JSON和form-data两种格式
     */
    public JSONObject patch(ConnectorInfo connectorInfo, String path, Object body) {
        try {
            Map<String, String> commonParams = buildCommonParams(connectorInfo);
            String baseUrl = buildBaseUrl(connectorInfo);
            String url = buildUrl(baseUrl, path, commonParams);
            
            HttpHeaders headers = buildHeaders(connectorInfo);
            HttpEntity<?> entity;
            
            // 根据body类型决定Content-Type
            if (body == null) {
                if (headers.getContentType() == null) {
                    headers.setContentType(MediaType.APPLICATION_JSON);
                }
                entity = new HttpEntity<>("", headers);
            } else if (body instanceof Map) {
                Map<?, ?> map = (Map<?, ?>) body;
                boolean isSimpleMap = true;
                for (Object value : map.values()) {
                    if (value != null && 
                        !(value instanceof String) && 
                        !(value instanceof Number) && 
                        !(value instanceof Boolean)) {
                        isSimpleMap = false;
                        break;
                    }
                }
                
                if (isSimpleMap) {
                    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
                    @SuppressWarnings("unchecked")
                    Map<String, String> formData = new HashMap<>();
                    for (Map.Entry<?, ?> entry : map.entrySet()) {
                        formData.put(String.valueOf(entry.getKey()), 
                                    entry.getValue() == null ? "" : String.valueOf(entry.getValue()));
                    }
                    entity = new HttpEntity<>(formData, headers);
                } else {
                    if (headers.getContentType() == null) {
                        headers.setContentType(MediaType.APPLICATION_JSON);
                    }
                    String jsonBody = JSON.toJSONString(body);
                    entity = new HttpEntity<>(jsonBody, headers);
                }
            } else {
                if (headers.getContentType() == null) {
                    headers.setContentType(MediaType.APPLICATION_JSON);
                }
                String jsonBody = JSON.toJSONString(body);
                entity = new HttpEntity<>(jsonBody, headers);
            }

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.PATCH, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                String respBody = response.getBody();
                return parseJsonResponse(respBody);
            } else {
                throw new RuntimeException("API返回状态码: " + response.getStatusCodeValue());
            }
        } catch (Exception e) {
            log.error("API PATCH请求失败: {}", path, e);
            throw new RuntimeException("API请求失败: " + e.getMessage(), e);
        }
    }

    /**
     * 发送DELETE请求（带断路器保护）
     * 支持返回JSON对象或JSON数组，统一包装为JSONObject
     */
    public JSONObject delete(ConnectorInfo connectorInfo, String path, Map<String, String> params) {
        try {
            // 合并公共参数
            Map<String, String> allParams = new HashMap<>();
            allParams.putAll(buildCommonParams(connectorInfo));
            if (params != null) {
                allParams.putAll(params);
            }
            
            String baseUrl = buildBaseUrl(connectorInfo);
            String url = buildUrl(baseUrl, path, allParams);
            HttpHeaders headers = buildHeaders(connectorInfo);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.DELETE, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                String body = response.getBody();
                return parseJsonResponse(body);
            } else {
                throw new RuntimeException("API返回状态码: " + response.getStatusCodeValue());
            }
        } catch (Exception e) {
            log.error("API DELETE请求失败: {}", path, e);
            throw new RuntimeException("API请求失败: " + e.getMessage(), e);
        }
    }

    /**
     * 带分页的GET请求(自动处理分页)
     * @param connectorInfo 连接器信息
     * @param path API路径
     * @param params 额外参数
     * @param paginationConfig 分页配置
     * @return 所有数据列表
     */
    public List<Map<String, Object>> fetchWithPagination(
            ConnectorInfo connectorInfo,
            String path,
            Map<String, String> params,
            PaginationConfig paginationConfig) {
        
        List<Map<String, Object>> allData = new ArrayList<>();
        
        try {
            int currentPage = paginationConfig.getStartPage();
            int pageSize = paginationConfig.getPageSize();
            int maxPages = paginationConfig.getMaxPages();
            int pageCount = 0;
            
            while (pageCount < maxPages) {
                // 构建分页参数
                Map<String, String> pageParams = new HashMap<>();
                if (params != null) {
                    pageParams.putAll(params);
                }
                pageParams.put(paginationConfig.getPageParamName(), String.valueOf(currentPage));
                pageParams.put(paginationConfig.getPageSizeParamName(), String.valueOf(pageSize));
                
                // 发送请求
                JSONObject response = get(connectorInfo, path, pageParams);
                
                // 提取数据
                List<Map<String, Object>> pageData = extractDataList(response, paginationConfig.getTotalPath());
                if (pageData == null || pageData.isEmpty()) {
                    break; // 没有数据了,退出循环
                }
                
                allData.addAll(pageData);
                
                // 检查是否还有更多数据
                if (pageData.size() < pageSize) {
                    break; // 返回数据少于pageSize,说明已经是最后一页
                }
                
                currentPage++;
                pageCount++;
                
                log.info("已获取第{}页数据,本页{}条,累计{}条", currentPage - 1, pageData.size(), allData.size());
            }
            
            log.info("分页获取完成,共{}页,总计{}条数据", pageCount, allData.size());
            return allData;
            
        } catch (Exception e) {
            log.error("分页获取数据失败", e);
            throw new RuntimeException("分页获取数据失败: " + e.getMessage(), e);
        }
    }

    /**
     * 批量POST请求(支持分批)
     * @param connectorInfo 连接器信息
     * @param path API路径
     * @param dataList 数据列表
     * @param batchConfig 批量配置
     * @return 批量推送结果
     */
    public BatchPushResult batchPost(
            ConnectorInfo connectorInfo,
            String path,
            List<Map<String, Object>> dataList,
            BatchConfig batchConfig) {
        
        BatchPushResult result = new BatchPushResult();
        result.setTotal(dataList.size());
        result.setSuccess(0);
        result.setFailed(0);
        
        if (dataList == null || dataList.isEmpty()) {
            return result;
        }
        
        int batchSize = batchConfig.getBatchSize();
        int retryTimes = batchConfig.getRetryTimes();
        long retryInterval = batchConfig.getRetryInterval();
        
        // 分批处理
        for (int i = 0; i < dataList.size(); i += batchSize) {
            int end = Math.min(i + batchSize, dataList.size());
            List<Map<String, Object>> batch = dataList.subList(i, end);
            
            // 构建请求体
            Object requestBody;
            if (StringUtils.hasText(batchConfig.getWrapperField())) {
                // 使用包裹字段
                Map<String, Object> wrapper = new HashMap<>();
                wrapper.put(batchConfig.getWrapperField(), batch);
                requestBody = wrapper;
            } else {
                requestBody = batch;
            }
            
            // 重试机制
            boolean success = false;
            Exception lastException = null;
            
            for (int retry = 0; retry <= retryTimes; retry++) {
                try {
                    post(connectorInfo, path, requestBody);
                    success = true;
                    result.setSuccess(result.getSuccess() + batch.size());
                    log.info("批次推送成功: [{}-{}], {}条数据", i, end - 1, batch.size());
                    break;
                    
                } catch (Exception e) {
                    lastException = e;
                    if (retry < retryTimes) {
                        log.warn("批次推送失败,将在{}ms后重试({}/{}): {}", 
                            retryInterval, retry + 1, retryTimes, e.getMessage());
                        try {
                            Thread.sleep(retryInterval);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                        }
                    }
                }
            }
            
            if (!success) {
                // 记录失败
                result.setFailed(result.getFailed() + batch.size());
                for (int j = 0; j < batch.size(); j++) {
                    BatchPushResult.ErrorRecord error = new BatchPushResult.ErrorRecord();
                    error.setIndex(i + j);
                    error.setData(batch.get(j));
                    error.setErrorMessage(lastException != null ? lastException.getMessage() : "未知错误");
                    result.getErrors().add(error);
                }
                log.error("批次推送失败: [{}-{}], 已重试{}次", i, end - 1, retryTimes);
            }
        }
        
        return result;
    }

    /**
     * 从JSON响应中提取数据列表
     */
    private List<Map<String, Object>> extractDataList(JSONObject response, String dataPath) {
        if (response == null) {
            return new ArrayList<>();
        }
        
        Object data = response;
        
        // 如果指定了数据路径,按路径提取
        if (StringUtils.hasText(dataPath)) {
            String[] paths = dataPath.split("\\.");
            for (String path : paths) {
                if (data instanceof JSONObject) {
                    data = ((JSONObject) data).get(path);
                } else {
                    break;
                }
            }
        }
        
        // 转换为列表
        List<Map<String, Object>> resultList = new ArrayList<>();
        if (data instanceof JSONArray) {
            JSONArray array = (JSONArray) data;
            for (int i = 0; i < array.size(); i++) {
                JSONObject item = array.getJSONObject(i);
                if (item != null) {
                    resultList.add(new HashMap<>(item.getInnerMap()));
                }
            }
        } else if (data instanceof JSONObject) {
            resultList.add(new HashMap<>(((JSONObject) data).getInnerMap()));
        }
        
        return resultList;
    }

    /**
     * 构建请求头
     */
    private HttpHeaders buildHeaders(ConnectorInfo connectorInfo) {
        HttpHeaders headers = new HttpHeaders();

        String authType = connectorInfo.getAuthType();
        if (authType == null || "NONE".equals(authType)) {
            // 继续处理自定义Headers
        } else {
            // 解析认证配置
            Map<String, String> authConfig = new HashMap<>();
            if (StringUtils.hasText(connectorInfo.getAuthConfig())) {
                try {
                    authConfig = JSON.parseObject(connectorInfo.getAuthConfig(), Map.class);
                } catch (Exception e) {
                    log.warn("解析认证配置失败", e);
                }
            }

            switch (authType) {
                case "BASIC":
                    String username = authConfig.getOrDefault("username", "");
                    String password = authConfig.getOrDefault("password", "");
                    String auth = username + ":" + password;
                    String encodedAuth = java.util.Base64.getEncoder().encodeToString(auth.getBytes());
                    headers.set("Authorization", "Basic " + encodedAuth);
                    break;

                case "BEARER":
                    String token = authConfig.getOrDefault("token", "");
                    headers.set("Authorization", "Bearer " + token);
                    break;

                case "API_KEY":
                    String apiKey = authConfig.getOrDefault("apiKey", "");
                    String headerName = authConfig.getOrDefault("headerName", "X-API-Key");
                    headers.set(headerName, apiKey);
                    break;

                default:
                    break;
            }
        }

        // 添加额外的自定义请求头
        if (StringUtils.hasText(connectorInfo.getExtraConfig())) {
            try {
                JSONObject extraConfig = JSON.parseObject(connectorInfo.getExtraConfig());
                JSONObject customHeaders = extraConfig.getJSONObject("headers");
                if (customHeaders != null) {
                    for (String key : customHeaders.keySet()) {
                        headers.set(key, customHeaders.getString(key));
                    }
                }
            } catch (Exception e) {
                log.warn("解析额外配置失败", e);
            }
        }

        return headers;
    }

    /**
     * 构建公共参数(从LextraConfig中获取)
     */
    private Map<String, String> buildCommonParams(ConnectorInfo connectorInfo) {
        Map<String, String> params = new HashMap<>();
        
        if (StringUtils.hasText(connectorInfo.getExtraConfig())) {
            try {
                JSONObject extraConfig = JSON.parseObject(connectorInfo.getExtraConfig());
                JSONObject customParams = extraConfig.getJSONObject("params");
                if (customParams != null) {
                    for (String key : customParams.keySet()) {
                        params.put(key, customParams.getString(key));
                    }
                }
            } catch (Exception e) {
                log.warn("解析公共参数失败", e);
            }
        }
        
        return params;
    }

    /**
     * 构建基础URL（从host、port、url组合）
     * 示例: host=127.0.0.1, port=8080, url=/api -> http://127.0.0.1:8080/api
     */
    private String buildBaseUrl(ConnectorInfo connectorInfo) {
        String host = connectorInfo.getHost();
        Integer port = connectorInfo.getPort();
        String baseUri = connectorInfo.getUrl(); // 实际是baseUri路径，如 /api
        
        // 根据端口判断协议
        String protocol = (port != null && port == 443) ? "https" : "http";
        
        // 构建基础URL
        StringBuilder baseUrl = new StringBuilder();
        baseUrl.append(protocol).append("://").append(host).append(":").append(port);
        
        // 添加baseUri（如果有）
        if (StringUtils.hasText(baseUri)) {
            if (!baseUri.startsWith("/")) {
                baseUrl.append("/");
            }
            baseUrl.append(baseUri);
        }
        
        return baseUrl.toString();
    }
    
    /**
     * 构建完整URL（基础URL + 路径 + 参数）
     */
    private String buildUrl(String baseUrl, String path, Map<String, String> params) {
        StringBuilder url = new StringBuilder(baseUrl);
        
        if (StringUtils.hasText(path)) {
            if (!baseUrl.endsWith("/") && !path.startsWith("/")) {
                url.append("/");
            }
            url.append(path);
        }

        if (params != null && !params.isEmpty()) {
            url.append("?");
            params.forEach((k, v) -> url.append(k).append("=").append(v).append("&"));
            url.deleteCharAt(url.length() - 1);
        }

        return url.toString();
    }
    
    /**
     * 解析JSON响应，支持对象和数组两种格式
     * - 如果是JSON对象：直接返回
     * - 如果是JSON数组：包装在 {"data": [...]} 中返回
     * - 如果为空或null：返回空对象 {}
     */
    private JSONObject parseJsonResponse(String body) {
        if (!StringUtils.hasText(body)) {
            return new JSONObject();
        }
        
        try {
            // 尝试解析为JSONObject
            return JSON.parseObject(body);
        } catch (Exception e) {
            // 如果解析失败，尝试解析为JSONArray
            try {
                JSONArray array = JSON.parseArray(body);
                JSONObject wrapper = new JSONObject();
                wrapper.put("data", array);
                log.debug("API返回数组格式，已包装为对象: data字段包含{}条记录", array.size());
                return wrapper;
            } catch (Exception ex) {
                log.error("JSON解析失败，返回原始字符串: {}", body.substring(0, Math.min(100, body.length())));
                // 无法解析，包装为字符串
                JSONObject wrapper = new JSONObject();
                wrapper.put("rawResponse", body);
                return wrapper;
            }
        }
    }
}
