package com.datapush.manager.util;

import cn.hutool.crypto.symmetric.AES;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 加密工具类
 * 支持密码加密/解密、日志脱敏
 *
 * @author Data Push Team
 * @since 2024-11-20
 */
@Slf4j
@Component
public class EncryptUtil {

    // AES密钥必须是16、24或32字节，使用16字节简单密钥
    private static final String DEFAULT_KEY = "DataPush2024Key!";
    
    // 敏感字段正则模式
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("(password|passwd|pwd|secret|token|apiKey|authorization)\\s*[:=]\\s*['\"]?([^'\"\\s,}]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern PHONE_PATTERN = Pattern.compile("(1[3-9]\\d{9})");
    private static final Pattern ID_CARD_PATTERN = Pattern.compile("(\\d{17}[\\dXx])");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("([\\w.-]+)@([\\w.-]+\\.[a-z]{2,})");

    @Value("${encrypt.aes.key:DataPush2024Key!}")
    private String aesKey;

    @PostConstruct
    public void init() {
        int keyLength = aesKey.getBytes(StandardCharsets.UTF_8).length;
        log.info("AES密钥长度: {} 字节", keyLength);
        if (keyLength != 16 && keyLength != 24 && keyLength != 32) {
            log.error("AES密钥长度不正确，必须是16、24或32字节！");
        }
    }

    /**
     * AES加密
     *
     * @param content 待加密内容
     * @return 加密后的Base64字符串
     */
    public String encrypt(String content) {
        if (content == null || content.isEmpty()) {
            return content;
        }
        try {
            AES aes = new AES(aesKey.getBytes(StandardCharsets.UTF_8));
            byte[] encrypted = aes.encrypt(content.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("加密失败", e);
        }
    }

    /**
     * AES解密
     *
     * @param encryptedContent 加密的Base64字符串
     * @return 解密后的内容
     */
    public String decrypt(String encryptedContent) {
        if (encryptedContent == null || encryptedContent.isEmpty()) {
            return encryptedContent;
        }
        try {
            AES aes = new AES(aesKey.getBytes(StandardCharsets.UTF_8));
            byte[] decryptedBytes = aes.decrypt(Base64.getDecoder().decode(encryptedContent));
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("解密失败", e);
        }
    }
    
    /**
     * 日志脱敏 - 自动识别并脱敏密码、Token等敏感信息
     * @param message 原始日志信息
     * @return 脱敏后的日志信息
     */
    public static String desensitizeLog(String message) {
        if (message == null || message.isEmpty()) {
            return message;
        }
        
        String result = message;
        
        // 1. 脱敏密码、密钥等敏感字段
        Matcher passwordMatcher = PASSWORD_PATTERN.matcher(result);
        while (passwordMatcher.find()) {
            String fieldName = passwordMatcher.group(1);
            String value = passwordMatcher.group(2);
            String masked = maskString(value);
            result = result.replace(fieldName + ":" + value, fieldName + ":" + masked)
                          .replace(fieldName + "=" + value, fieldName + "=" + masked)
                          .replace(fieldName + ": " + value, fieldName + ": " + masked)
                          .replace(fieldName + "= " + value, fieldName + "= " + masked);
        }
        
        return result;
    }
    
    /**
     * 脱敏手机号
     * @param phone 手机号
     * @return 脱敏后的手机号，如：138****5678
     */
    public static String desensitizePhone(String phone) {
        if (phone == null || phone.length() != 11) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }
    
    /**
     * 脱敏身份证号
     * @param idCard 身份证号
     * @return 脱敏后的身份证号，如：320***********1234
     */
    public static String desensitizeIdCard(String idCard) {
        if (idCard == null || idCard.length() < 8) {
            return idCard;
        }
        return idCard.substring(0, 3) + "***********" + idCard.substring(idCard.length() - 4);
    }
    
    /**
     * 脱敏邮箱
     * @param email 邮箱
     * @return 脱敏后的邮箱，如：abc***@example.com
     */
    public static String desensitizeEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        String[] parts = email.split("@");
        String username = parts[0];
        if (username.length() <= 3) {
            return username.charAt(0) + "***@" + parts[1];
        }
        return username.substring(0, 3) + "***@" + parts[1];
    }
    
    /**
     * 脱敏银行卡号
     * @param bankCard 银行卡号
     * @return 脱敏后的银行卡号，如：6222 **** **** 1234
     */
    public static String desensitizeBankCard(String bankCard) {
        if (bankCard == null || bankCard.length() < 8) {
            return bankCard;
        }
        return bankCard.substring(0, 4) + " **** **** " + bankCard.substring(bankCard.length() - 4);
    }
    
    /**
     * 通用字符串掩码
     * @param str 原始字符串
     * @return 掩码后的字符串
     */
    private static String maskString(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        if (str.length() <= 4) {
            return "****";
        }
        // 显示前2位和后2位，中间用*代替
        int showLen = 2;
        return str.substring(0, showLen) + "****" + str.substring(str.length() - showLen);
    }
    
    /**
     * 脱敏JSON中的敏感字段
     * @param json JSON字符串
     * @return 脱敏后的JSON字符串
     */
    public static String desensitizeJson(String json) {
        if (json == null || json.isEmpty()) {
            return json;
        }
        
        String result = json;
        
        // 脱敏密码类字段
        result = desensitizeLog(result);
        
        // 脱敏手机号
        Matcher phoneMatcher = PHONE_PATTERN.matcher(result);
        while (phoneMatcher.find()) {
            String phone = phoneMatcher.group(1);
            result = result.replace(phone, desensitizePhone(phone));
        }
        
        // 脱敏身份证
        Matcher idCardMatcher = ID_CARD_PATTERN.matcher(result);
        while (idCardMatcher.find()) {
            String idCard = idCardMatcher.group(1);
            result = result.replace(idCard, desensitizeIdCard(idCard));
        }
        
        // 脱敏邮箱
        Matcher emailMatcher = EMAIL_PATTERN.matcher(result);
        while (emailMatcher.find()) {
            String email = emailMatcher.group(0);
            result = result.replace(email, desensitizeEmail(email));
        }
        
        return result;
    }
}
