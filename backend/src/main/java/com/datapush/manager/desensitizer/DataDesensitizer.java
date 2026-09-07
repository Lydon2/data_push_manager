package com.datapush.manager.desensitizer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * 数据脱敏处理器
 * 支持手机号、身份证、银行卡、邮箱等常见类型脱敏
 */
@Slf4j
@Component
public class DataDesensitizer {
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 脱敏类型枚举
     */
    public enum DesensitizeType {
        PHONE,          // 手机号
        ID_CARD,        // 身份证号
        BANK_CARD,      // 银行卡号
        EMAIL,          // 邮箱
        NAME,           // 姓名
        ADDRESS,        // 地址
        CUSTOM          // 自定义
    }
    
    /**
     * 脱敏策略枚举
     */
    public enum DesensitizeStrategy {
        FULL,           // 完全脱敏（全部替换为*）
        PARTIAL,        // 部分脱敏（保留部分字符）
        MASK            // 掩码替换（使用指定字符替换）
    }
    
    /**
     * 执行脱敏
     * 
     * @param value 原始值
     * @param desensitizeConfig 脱敏配置JSON
     * @return 脱敏后的值
     */
    public Object desensitize(Object value, String desensitizeConfig) {
        if (value == null || desensitizeConfig == null || desensitizeConfig.trim().isEmpty()) {
            return value;
        }
        
        String strValue = String.valueOf(value);
        
        try {
            JsonNode config = objectMapper.readTree(desensitizeConfig);
            
            String typeStr = config.has("type") ? config.get("type").asText() : "CUSTOM";
            String strategyStr = config.has("strategy") ? config.get("strategy").asText() : "PARTIAL";
            
            DesensitizeType type = DesensitizeType.valueOf(typeStr.toUpperCase());
            DesensitizeStrategy strategy = DesensitizeStrategy.valueOf(strategyStr.toUpperCase());
            
            switch (type) {
                case PHONE:
                    return desensitizePhone(strValue, strategy);
                case ID_CARD:
                    return desensitizeIdCard(strValue, strategy);
                case BANK_CARD:
                    return desensitizeBankCard(strValue, strategy);
                case EMAIL:
                    return desensitizeEmail(strValue, strategy);
                case NAME:
                    return desensitizeName(strValue, strategy);
                case ADDRESS:
                    return desensitizeAddress(strValue, strategy);
                case CUSTOM:
                    String pattern = config.has("pattern") ? config.get("pattern").asText() : "";
                    char maskChar = config.has("maskChar") ? config.get("maskChar").asText().charAt(0) : '*';
                    int keepStart = config.has("keepStart") ? config.get("keepStart").asInt() : 0;
                    int keepEnd = config.has("keepEnd") ? config.get("keepEnd").asInt() : 0;
                    return desensitizeCustom(strValue, strategy, pattern, maskChar, keepStart, keepEnd);
                default:
                    return value;
            }
            
        } catch (Exception e) {
            log.error("数据脱敏失败: {}", e.getMessage());
            return value;
        }
    }
    
    /**
     * 手机号脱敏
     * 11位手机号，根据策略脱敏
     */
    private String desensitizePhone(String phone, DesensitizeStrategy strategy) {
        if (phone == null || phone.length() != 11) {
            return phone;
        }
        
        switch (strategy) {
            case FULL:
                return "***********";
            case PARTIAL:
                // 保留前3位和后4位：138****1234
                return phone.substring(0, 3) + "****" + phone.substring(7);
            case MASK:
                // 中间4位替换为*：138****1234
                return phone.substring(0, 3) + "****" + phone.substring(7);
            default:
                return phone;
        }
    }
    
    /**
     * 身份证号脱敏
     * 18位身份证号，根据策略脱敏
     */
    private String desensitizeIdCard(String idCard, DesensitizeStrategy strategy) {
        if (idCard == null || (idCard.length() != 15 && idCard.length() != 18)) {
            return idCard;
        }
        
        switch (strategy) {
            case FULL:
                return idCard.replaceAll(".", "*");
            case PARTIAL:
                // 保留前6位和后4位：110101********1234
                int len = idCard.length();
                return idCard.substring(0, 6) + "********" + idCard.substring(len - 4);
            case MASK:
                // 中间8位替换为*
                int len2 = idCard.length();
                return idCard.substring(0, 6) + "********" + idCard.substring(len2 - 4);
            default:
                return idCard;
        }
    }
    
    /**
     * 银行卡号脱敏
     * 16-19位银行卡号，根据策略脱敏
     */
    private String desensitizeBankCard(String bankCard, DesensitizeStrategy strategy) {
        if (bankCard == null || bankCard.length() < 16 || bankCard.length() > 19) {
            return bankCard;
        }
        
        switch (strategy) {
            case FULL:
                return bankCard.replaceAll(".", "*");
            case PARTIAL:
                // 保留前6位和后4位：622202******1234
                int len = bankCard.length();
                StringBuilder masked = new StringBuilder(bankCard.substring(0, 6));
                for (int i = 6; i < len - 4; i++) {
                    masked.append("*");
                }
                masked.append(bankCard.substring(len - 4));
                return masked.toString();
            case MASK:
                int len2 = bankCard.length();
                StringBuilder masked2 = new StringBuilder(bankCard.substring(0, 6));
                for (int i = 6; i < len2 - 4; i++) {
                    masked2.append("*");
                }
                masked2.append(bankCard.substring(len2 - 4));
                return masked2.toString();
            default:
                return bankCard;
        }
    }
    
    /**
     * 邮箱脱敏
     * 保留邮箱前缀第一个字符和@后的域名
     */
    private String desensitizeEmail(String email, DesensitizeStrategy strategy) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        
        String[] parts = email.split("@");
        if (parts.length != 2) {
            return email;
        }
        
        String prefix = parts[0];
        String domain = parts[1];
        
        switch (strategy) {
            case FULL:
                return "***@" + domain;
            case PARTIAL:
                // 保留首字符：z****@example.com
                if (prefix.length() == 1) {
                    return prefix + "@" + domain;
                }
                return prefix.charAt(0) + "****@" + domain;
            case MASK:
                if (prefix.length() == 1) {
                    return prefix + "@" + domain;
                }
                StringBuilder masked = new StringBuilder();
                masked.append(prefix.charAt(0));
                for (int i = 1; i < prefix.length(); i++) {
                    masked.append("*");
                }
                masked.append("@").append(domain);
                return masked.toString();
            default:
                return email;
        }
    }
    
    /**
     * 姓名脱敏
     */
    private String desensitizeName(String name, DesensitizeStrategy strategy) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        
        switch (strategy) {
            case FULL:
                return name.replaceAll(".", "*");
            case PARTIAL:
                // 保留姓氏：张**
                if (name.length() == 1) {
                    return name;
                } else if (name.length() == 2) {
                    return name.charAt(0) + "*";
                } else {
                    return name.charAt(0) + "**";
                }
            case MASK:
                if (name.length() == 1) {
                    return name;
                }
                StringBuilder masked = new StringBuilder();
                masked.append(name.charAt(0));
                for (int i = 1; i < name.length(); i++) {
                    masked.append("*");
                }
                return masked.toString();
            default:
                return name;
        }
    }
    
    /**
     * 地址脱敏
     */
    private String desensitizeAddress(String address, DesensitizeStrategy strategy) {
        if (address == null || address.isEmpty()) {
            return address;
        }
        
        switch (strategy) {
            case FULL:
                return "***";
            case PARTIAL:
                // 保留省市：北京市海淀区***
                if (address.length() <= 6) {
                    return address.substring(0, Math.min(2, address.length())) + "***";
                }
                return address.substring(0, 6) + "***";
            case MASK:
                if (address.length() <= 6) {
                    return address.substring(0, Math.min(2, address.length())) + "***";
                }
                return address.substring(0, 6) + "***";
            default:
                return address;
        }
    }
    
    /**
     * 自定义脱敏
     * 
     * @param value 原始值
     * @param strategy 脱敏策略
     * @param pattern 正则表达式（用于验证格式）
     * @param maskChar 掩码字符
     * @param keepStart 保留开始位数
     * @param keepEnd 保留结束位数
     * @return 脱敏后的值
     */
    private String desensitizeCustom(
            String value, 
            DesensitizeStrategy strategy,
            String pattern,
            char maskChar,
            int keepStart,
            int keepEnd) {
        
        if (value == null || value.isEmpty()) {
            return value;
        }
        
        // 如果指定了pattern，先验证格式
        if (pattern != null && !pattern.isEmpty()) {
            if (!Pattern.matches(pattern, value)) {
                log.warn("自定义脱敏: 值不匹配指定格式，跳过脱敏");
                return value;
            }
        }
        
        int len = value.length();
        
        switch (strategy) {
            case FULL:
                return value.replaceAll(".", String.valueOf(maskChar));
                
            case PARTIAL:
            case MASK:
                // 保留前keepStart位和后keepEnd位
                if (keepStart + keepEnd >= len) {
                    return value; // 保留位数超过总长度，不脱敏
                }
                
                StringBuilder result = new StringBuilder();
                
                // 前缀
                if (keepStart > 0) {
                    result.append(value, 0, keepStart);
                }
                
                // 中间掩码
                int maskLength = len - keepStart - keepEnd;
                for (int i = 0; i < maskLength; i++) {
                    result.append(maskChar);
                }
                
                // 后缀
                if (keepEnd > 0) {
                    result.append(value.substring(len - keepEnd));
                }
                
                return result.toString();
                
            default:
                return value;
        }
    }
}
