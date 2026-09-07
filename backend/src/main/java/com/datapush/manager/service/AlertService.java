package com.datapush.manager.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.datapush.manager.entity.*;
import com.datapush.manager.mapper.AlertRecordMapper;
import com.datapush.manager.mapper.AlertRuleMapper;
import com.datapush.manager.mapper.InternalMessageMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 告警服务
 */
@Slf4j
@Service
public class AlertService extends ServiceImpl<AlertRecordMapper, AlertRecord> {

    @Autowired
    private AlertRuleMapper alertRuleMapper;

    @Autowired
    private AlertRecordMapper alertRecordMapper;

    @Autowired
    private InternalMessageMapper internalMessageMapper;

    @Autowired
    private TaskExecuteLogService taskExecuteLogService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 检查并触发任务失败告警
     */
    public void checkTaskFailAlert(Long taskId, String taskName, Long executeLogId, String errorMessage) {
        try {
            // 查询该任务的告警规则
            LambdaQueryWrapper<AlertRule> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(AlertRule::getRuleType, "TASK_FAIL")
                   .eq(AlertRule::getEnabled, 1)
                   .and(w -> w.isNull(AlertRule::getTaskId).or().eq(AlertRule::getTaskId, taskId));
            
            List<AlertRule> rules = alertRuleMapper.selectList(wrapper);
            
            for (AlertRule rule : rules) {
                boolean shouldAlert = checkAlertCondition(rule, taskId, executeLogId);
                if (shouldAlert) {
                    triggerAlert(rule, taskId, taskName, executeLogId, errorMessage);
                }
            }
        } catch (Exception e) {
            log.error("检查任务失败告警异常", e);
        }
    }

    /**
     * 检查告警条件是否满足
     */
    private boolean checkAlertCondition(AlertRule rule, Long taskId, Long executeLogId) {
        try {
            String conditionConfig = rule.getConditionConfig();
            JsonNode config = objectMapper.readTree(conditionConfig);
            
            // 检查连续失败次数
            if (config.has("continuousFailCount")) {
                int requiredCount = config.get("continuousFailCount").asInt();
                int actualCount = getRecentContinuousFailCount(taskId, requiredCount);
                return actualCount >= requiredCount;
            }
            
            // 单次失败即告警
            if (config.has("failCount")) {
                return true;
            }
            
            return false;
        } catch (Exception e) {
            log.error("检查告警条件异常", e);
            return false;
        }
    }

    /**
     * 获取最近连续失败次数
     */
    private int getRecentContinuousFailCount(Long taskId, int limit) {
        LambdaQueryWrapper<TaskExecuteLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TaskExecuteLog::getTaskId, taskId)
               .orderByDesc(TaskExecuteLog::getStartTime)
               .last("LIMIT " + limit);
        
        List<TaskExecuteLog> logs = taskExecuteLogService.list(wrapper);
        
        int count = 0;
        for (TaskExecuteLog log : logs) {
            if ("FAILED".equals(log.getExecuteStatus())) {
                count++;
            } else {
                break;
            }
        }
        
        return count;
    }

    /**
     * 触发告警
     */
    private void triggerAlert(AlertRule rule, Long taskId, String taskName, Long executeLogId, String errorMessage) {
        try {
            // 创建告警记录
            AlertRecord record = new AlertRecord();
            record.setRuleId(rule.getId());
            record.setRuleName(rule.getRuleName());
            record.setAlertType(rule.getRuleType());
            record.setAlertLevel(rule.getAlertLevel());
            record.setTaskId(taskId);
            record.setTaskName(taskName);
            record.setExecuteLogId(executeLogId);
            record.setAlertTime(LocalDateTime.now());
            record.setAlertStatus("PENDING");
            
            // 构建告警标题和内容
            String title = buildAlertTitle(rule, taskName);
            String content = buildAlertContent(rule, taskName, errorMessage);
            record.setAlertTitle(title);
            record.setAlertContent(content);
            
            alertRecordMapper.insert(record);
            
            // 发送告警到各渠道
            sendAlert(rule, record, title, content);
            
            // 更新告警记录状态
            record.setAlertStatus("NOTIFIED");
            alertRecordMapper.updateById(record);
            
        } catch (Exception e) {
            log.error("触发告警异常", e);
        }
    }

    /**
     * 构建告警标题
     */
    private String buildAlertTitle(AlertRule rule, String taskName) {
        return String.format("【%s】%s - %s", 
            rule.getAlertLevel(), 
            rule.getRuleName(), 
            taskName);
    }

    /**
     * 构建告警内容
     */
    private String buildAlertContent(AlertRule rule, String taskName, String errorMessage) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        StringBuilder sb = new StringBuilder();
        sb.append("告警规则：").append(rule.getRuleName()).append("\n");
        sb.append("任务名称：").append(taskName).append("\n");
        sb.append("告警时间：").append(LocalDateTime.now().format(formatter)).append("\n");
        sb.append("告警级别：").append(rule.getAlertLevel()).append("\n");
        if (errorMessage != null && !errorMessage.isEmpty()) {
            sb.append("错误信息：").append(errorMessage).append("\n");
        }
        return sb.toString();
    }

    /**
     * 发送告警到各渠道
     */
    private void sendAlert(AlertRule rule, AlertRecord record, String title, String content) {
        String[] channels = rule.getAlertChannels().split(",");
        Map<String, Object> sendResults = new HashMap<>();
        List<String> sentChannels = new ArrayList<>();
        
        for (String channel : channels) {
            channel = channel.trim();
            try {
                boolean success = false;
                
                switch (channel) {
                    case "INTERNAL":
                        success = sendInternalMessage(record, title, content);
                        break;
                    case "WEBHOOK":
                        success = sendWebhook(rule, title, content);
                        break;
                    case "LOG":
                        success = writeAlertLog(title, content);
                        break;
                    default:
                        log.warn("未知的告警渠道: {}", channel);
                }
                
                sendResults.put(channel, success);
                if (success) {
                    sentChannels.add(channel);
                }
            } catch (Exception e) {
                log.error("发送告警到渠道{}失败", channel, e);
                sendResults.put(channel, false);
            }
        }
        
        // 更新发送结果
        try {
            record.setSendChannels(String.join(",", sentChannels));
            record.setSendResult(objectMapper.writeValueAsString(sendResults));
        } catch (Exception e) {
            log.error("保存告警发送结果异常", e);
        }
    }

    /**
     * 发送内部消息
     */
    private boolean sendInternalMessage(AlertRecord record, String title, String content) {
        try {
            InternalMessage message = new InternalMessage();
            message.setMessageType("ALERT");
            message.setMessageLevel(record.getAlertLevel());
            message.setTitle(title);
            message.setContent(content);
            message.setRelatedType("TASK");
            message.setRelatedId(record.getTaskId());
            message.setIsRead(0);
            
            internalMessageMapper.insert(message);
            log.info("内部消息发送成功：{}", title);
            return true;
        } catch (Exception e) {
            log.error("发送内部消息失败", e);
            return false;
        }
    }

    /**
     * 发送Webhook回调
     */
    private boolean sendWebhook(AlertRule rule, String title, String content) {
        try {
            if (rule.getChannelConfig() == null || rule.getChannelConfig().isEmpty()) {
                log.warn("Webhook配置为空");
                return false;
            }
            
            JsonNode config = objectMapper.readTree(rule.getChannelConfig());
            if (!config.has("webhookUrl")) {
                log.warn("Webhook URL未配置");
                return false;
            }
            
            String webhookUrl = config.get("webhookUrl").asText();
            
            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("title", title);
            requestBody.put("content", content);
            requestBody.put("level", rule.getAlertLevel());
            requestBody.put("timestamp", System.currentTimeMillis());
            
            // 发送POST请求
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<String> response = restTemplate.postForEntity(webhookUrl, request, String.class);
            
            boolean success = response.getStatusCode().is2xxSuccessful();
            if (success) {
                log.info("Webhook发送成功：{} -> {}", title, webhookUrl);
            } else {
                log.warn("Webhook发送失败，状态码：{}", response.getStatusCodeValue());
            }
            return success;
            
        } catch (Exception e) {
            log.error("发送Webhook失败", e);
            return false;
        }
    }

    /**
     * 写入告警日志
     */
    private boolean writeAlertLog(String title, String content) {
        try {
            log.warn("【告警】{}\n{}", title, content);
            return true;
        } catch (Exception e) {
            log.error("写入告警日志失败", e);
            return false;
        }
    }

    /**
     * 获取未读告警数量
     */
    public Long getUnreadAlertCount() {
        LambdaQueryWrapper<AlertRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AlertRecord::getAlertStatus, "PENDING")
               .or()
               .eq(AlertRecord::getAlertStatus, "NOTIFIED");
        return (long) alertRecordMapper.selectCount(wrapper);
    }

    /**
     * 获取未读消息数量
     */
    public Long getUnreadMessageCount() {
        LambdaQueryWrapper<InternalMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InternalMessage::getIsRead, 0);
        return (long) internalMessageMapper.selectCount(wrapper);
    }

    /**
     * 标记告警为已处理
     */
    public void handleAlert(Long alertId, String handleUser, String handleRemark) {
        AlertRecord record = alertRecordMapper.selectById(alertId);
        if (record != null) {
            record.setAlertStatus("HANDLED");
            record.setHandleUser(handleUser);
            record.setHandleTime(LocalDateTime.now());
            record.setHandleRemark(handleRemark);
            alertRecordMapper.updateById(record);
        }
    }

    /**
     * 标记消息为已读
     */
    public void markMessageAsRead(Long messageId) {
        InternalMessage message = internalMessageMapper.selectById(messageId);
        if (message != null && message.getIsRead() == 0) {
            message.setIsRead(1);
            message.setReadTime(LocalDateTime.now());
            internalMessageMapper.updateById(message);
        }
    }
}
