package com.datapush.manager.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 加密工具类单元测试
 */
class EncryptUtilTest {

    @Test
    void testDesensitizeLog() {
        String log = "连接失败: password=mySecret123, token=abc123xyz";
        String result = EncryptUtil.desensitizeLog(log);
        
        assertFalse(result.contains("mySecret123"));
        assertFalse(result.contains("abc123xyz"));
        assertTrue(result.contains("password"));
        assertTrue(result.contains("token"));
    }

    @Test
    void testDesensitizePhone() {
        String phone = "13812345678";
        String result = EncryptUtil.desensitizePhone(phone);
        
        assertEquals("138****5678", result);
    }

    @Test
    void testDesensitizePhone_Invalid() {
        String phone = "12345";
        String result = EncryptUtil.desensitizePhone(phone);
        
        assertEquals("12345", result); // 不处理无效手机号
    }

    @Test
    void testDesensitizeIdCard() {
        String idCard = "320123199001011234";
        String result = EncryptUtil.desensitizeIdCard(idCard);
        
        assertEquals("320***********1234", result);
    }

    @Test
    void testDesensitizeEmail() {
        String email = "abcdef@example.com";
        String result = EncryptUtil.desensitizeEmail(email);
        
        assertEquals("abc***@example.com", result);
    }

    @Test
    void testDesensitizeEmail_Short() {
        String email = "ab@example.com";
        String result = EncryptUtil.desensitizeEmail(email);
        
        assertEquals("a***@example.com", result);
    }

    @Test
    void testDesensitizeBankCard() {
        String bankCard = "6222123456781234";
        String result = EncryptUtil.desensitizeBankCard(bankCard);
        
        assertEquals("6222 **** **** 1234", result);
    }

    @Test
    void testDesensitizeJson() {
        String json = "{\"password\":\"abc123\",\"phone\":\"13812345678\",\"email\":\"test@example.com\"}";
        String result = EncryptUtil.desensitizeJson(json);
        
        assertFalse(result.contains("abc123"));
        assertTrue(result.contains("138****5678"));
        assertTrue(result.contains("***@example.com"));
    }

    @Test
    void testDesensitizeLog_MultiplePasswords() {
        String log = "password=pass1, token=token1, apiKey=key1";
        String result = EncryptUtil.desensitizeLog(log);
        
        assertFalse(result.contains("pass1"));
        assertFalse(result.contains("token1"));
        assertFalse(result.contains("key1"));
    }

    @Test
    void testDesensitizeLog_NoSensitiveData() {
        String log = "Normal log message without sensitive data";
        String result = EncryptUtil.desensitizeLog(log);
        
        assertEquals(log, result); // 不应修改
    }
}
