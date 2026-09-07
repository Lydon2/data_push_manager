package com.datapush.manager.utils;

import lombok.extern.slf4j.Slf4j;

/**
 * 字段转换函数工具类
 * 统一处理各种函数转换，包括数据脱敏
 */
@Slf4j
public class TransformFunctionUtil {

    /**
     * 应用函数转换
     */
    public static Object applyFunction(Object value, String func) {
        String expr = func.trim();
        try {
            // ========== 字符串函数 ==========
            if ("UPPER".equalsIgnoreCase(expr)) {
                return value == null ? null : String.valueOf(value).toUpperCase();
            }
            if ("LOWER".equalsIgnoreCase(expr)) {
                return value == null ? null : String.valueOf(value).toLowerCase();
            }
            if ("TRIM".equalsIgnoreCase(expr)) {
                return value == null ? null : String.valueOf(value).trim();
            }
            
            // ========== 数值函数 ==========
            if ("CEIL".equalsIgnoreCase(expr)) {
                return value == null ? null : Math.ceil(Double.parseDouble(String.valueOf(value)));
            }
            if ("FLOOR".equalsIgnoreCase(expr)) {
                return value == null ? null : Math.floor(Double.parseDouble(String.valueOf(value)));
            }
            if ("ABS".equalsIgnoreCase(expr)) {
                return value == null ? null : Math.abs(Double.parseDouble(String.valueOf(value)));
            }
            if ("TO_INT".equalsIgnoreCase(expr)) {
                return value == null ? null : Integer.parseInt(String.valueOf(value));
            }
            if ("TO_STRING".equalsIgnoreCase(expr)) {
                return value == null ? null : String.valueOf(value);
            }
            
            // TO_DECIMAL
            if (expr.startsWith("TO_DECIMAL")) {
                int l = expr.indexOf('(');
                int r = expr.indexOf(')', l + 1);
                int scale = 0;
                if (l > 0 && r > l) {
                    String s = expr.substring(l + 1, r).trim();
                    if (!s.isEmpty()) scale = Integer.parseInt(s);
                }
                java.math.BigDecimal bd = new java.math.BigDecimal(String.valueOf(value));
                return bd.setScale(scale, java.math.RoundingMode.HALF_UP);
            }
            
            // ROUND
            if (expr.startsWith("ROUND")) {
                int l = expr.indexOf('(');
                int r = expr.indexOf(')', l + 1);
                int scale = 0;
                if (l > 0 && r > l) {
                    String s = expr.substring(l + 1, r).trim();
                    if (!s.isEmpty()) scale = Integer.parseInt(s);
                }
                java.math.BigDecimal bd = new java.math.BigDecimal(String.valueOf(value));
                return bd.setScale(scale, java.math.RoundingMode.HALF_UP).toString();
            }
            
            // SUBSTR
            if (expr.startsWith("SUBSTR")) {
                int l = expr.indexOf('(');
                int r = expr.indexOf(')', l + 1);
                if (l > 0 && r > l) {
                    String[] parts = expr.substring(l + 1, r).split(",");
                    int start = Integer.parseInt(parts[0].trim());
                    int end = parts.length > 1 ? Integer.parseInt(parts[1].trim()) : (value == null ? 0 : String.valueOf(value).length());
                    String s = value == null ? "" : String.valueOf(value);
                    int sLen = s.length();
                    int from = Math.max(0, Math.min(start, sLen));
                    int to = Math.max(from, Math.min(end, sLen));
                    return s.substring(from, to);
                }
            }
            
            // REPLACE_REGEX
            if (expr.startsWith("REPLACE_REGEX")) {
                int l = expr.indexOf('(');
                int r = expr.indexOf(')', l + 1);
                if (l > 0 && r > l) {
                    String[] parts = expr.substring(l + 1, r).split(",");
                    String pattern = parts[0].trim().replaceAll("^'|'$", "");
                    String repl = parts.length > 1 ? parts[1].trim().replaceAll("^'|'$", "") : "";
                    String s = value == null ? "" : String.valueOf(value);
                    return s.replaceAll(pattern, repl);
                }
            }
            
            // REPLACE
            if (expr.startsWith("REPLACE")) {
                int l = expr.indexOf('(');
                int r = expr.indexOf(')', l + 1);
                if (l > 0 && r > l) {
                    String[] parts = expr.substring(l + 1, r).split(",");
                    String oldStr = parts[0].trim().replaceAll("^'|'$", "");
                    String newStr = parts.length > 1 ? parts[1].trim().replaceAll("^'|'$", "") : "";
                    String s = value == null ? "" : String.valueOf(value);
                    return s.replace(oldStr, newStr);
                }
            }
            
            // LPAD / RPAD
            if (expr.startsWith("LPAD") || expr.startsWith("RPAD")) {
                int l = expr.indexOf('(');
                int r = expr.indexOf(')', l + 1);
                if (l > 0 && r > l) {
                    String[] parts = expr.substring(l + 1, r).split(",");
                    String padChar = parts[0].trim().replaceAll("^'|'$", "");
                    int length = parts.length > 1 ? Integer.parseInt(parts[1].trim()) : 0;
                    String s = value == null ? "" : String.valueOf(value);
                    if (padChar.isEmpty()) padChar = "0";
                    char ch = padChar.charAt(0);
                    StringBuilder sb = new StringBuilder(s);
                    if (expr.startsWith("LPAD")) {
                        while (sb.length() < length) {
                            sb.insert(0, ch);
                        }
                    } else {
                        while (sb.length() < length) {
                            sb.append(ch);
                        }
                    }
                    return sb.toString();
                }
            }
            
            // CONCAT_PREFIX
            if (expr.startsWith("CONCAT_PREFIX")) {
                int l = expr.indexOf('(');
                int r = expr.indexOf(')', l + 1);
                String prefix = "";
                if (l > 0 && r > l) {
                    prefix = expr.substring(l + 1, r).trim().replaceAll("^'|'$", "");
                }
                return (prefix) + (value == null ? "" : String.valueOf(value));
            }
            
            // CONCAT_SUFFIX
            if (expr.startsWith("CONCAT_SUFFIX")) {
                int l = expr.indexOf('(');
                int r = expr.indexOf(')', l + 1);
                String suffix = "";
                if (l > 0 && r > l) {
                    suffix = expr.substring(l + 1, r).trim().replaceAll("^'|'$", "");
                }
                return (value == null ? "" : String.valueOf(value)) + (suffix);
            }
            
            // DATE_FORMAT
            if (expr.startsWith("DATE_FORMAT")) {
                int l = expr.indexOf('(');
                int r = expr.indexOf(')', l + 1);
                if (l > 0 && r > l) {
                    String pattern = expr.substring(l + 1, r).trim().replaceAll("^'|'$", "");
                    java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern(pattern);
                    if (value instanceof java.time.LocalDateTime) {
                        return ((java.time.LocalDateTime) value).format(formatter);
                    }
                    if (value instanceof java.util.Date) {
                        return new java.text.SimpleDateFormat(pattern).format((java.util.Date) value);
                    }
                    if (value != null) {
                        String s = String.valueOf(value);
                        try {
                            java.time.LocalDateTime dt = java.time.LocalDateTime.parse(s);
                            return dt.format(formatter);
                        } catch (Exception ignore) {}
                    }
                }
            }
            
            // ========== 高级转换函数 ==========
            
            // SPLIT - 字符串拆分
            if (expr.startsWith("SPLIT")) {
                int l = expr.indexOf('(');
                int r = expr.indexOf(')', l + 1);
                if (l > 0 && r > l) {
                    String[] parts = expr.substring(l + 1, r).split(",");
                    String delimiter = parts[0].trim().replaceAll("^'|'$", "");
                    int index = parts.length > 1 ? Integer.parseInt(parts[1].trim()) : 0;
                    String s = value == null ? "" : String.valueOf(value);
                    String[] arr = s.split(java.util.regex.Pattern.quote(delimiter));
                    return index >= 0 && index < arr.length ? arr[index] : "";
                }
            }
            
            // CONCAT - 多字段拼接
            if (expr.startsWith("CONCAT")) {
                int l = expr.indexOf('(');
                int r = expr.indexOf(')', l + 1);
                if (l > 0 && r > l) {
                    String text = expr.substring(l + 1, r).trim().replaceAll("^'|'$", "");
                    return (value == null ? "" : String.valueOf(value)) + text;
                }
            }
            
            // COALESCE - NULL处理
            if (expr.startsWith("COALESCE")) {
                int l = expr.indexOf('(');
                int r = expr.indexOf(')', l + 1);
                if (l > 0 && r > l) {
                    String defaultVal = expr.substring(l + 1, r).trim().replaceAll("^'|'$", "");
                    return value == null || "".equals(value) ? defaultVal : value;
                }
            }
            
            // LENGTH - 获取字符串长度
            if ("LENGTH".equalsIgnoreCase(expr)) {
                return value == null ? 0 : String.valueOf(value).length();
            }
            
            // REVERSE - 字符串反转
            if ("REVERSE".equalsIgnoreCase(expr)) {
                String s = value == null ? "" : String.valueOf(value);
                return new StringBuilder(s).reverse().toString();
            }
            
            // ADD - 数值加法
            if (expr.startsWith("ADD")) {
                int l = expr.indexOf('(');
                int r = expr.indexOf(')', l + 1);
                if (l > 0 && r > l) {
                    double num = Double.parseDouble(expr.substring(l + 1, r).trim());
                    double val = value == null ? 0 : Double.parseDouble(String.valueOf(value));
                    return val + num;
                }
            }
            
            // MULTIPLY - 数值乘法
            if (expr.startsWith("MULTIPLY")) {
                int l = expr.indexOf('(');
                int r = expr.indexOf(')', l + 1);
                if (l > 0 && r > l) {
                    double num = Double.parseDouble(expr.substring(l + 1, r).trim());
                    double val = value == null ? 0 : Double.parseDouble(String.valueOf(value));
                    return val * num;
                }
            }
            
            // URL_ENCODE - URL编码
            if ("URL_ENCODE".equalsIgnoreCase(expr)) {
                String s = value == null ? "" : String.valueOf(value);
                return java.net.URLEncoder.encode(s, "UTF-8");
            }
            
            // URL_DECODE - URL解码
            if ("URL_DECODE".equalsIgnoreCase(expr)) {
                String s = value == null ? "" : String.valueOf(value);
                return java.net.URLDecoder.decode(s, "UTF-8");
            }
            
            // BASE64_ENCODE - Base64编码
            if ("BASE64_ENCODE".equalsIgnoreCase(expr)) {
                String s = value == null ? "" : String.valueOf(value);
                return java.util.Base64.getEncoder().encodeToString(s.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
            
            // BASE64_DECODE - Base64解码
            if ("BASE64_DECODE".equalsIgnoreCase(expr)) {
                String s = value == null ? "" : String.valueOf(value);
                byte[] bytes = java.util.Base64.getDecoder().decode(s);
                return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
            }
            
            // MD5 - MD5哈希
            if ("MD5".equalsIgnoreCase(expr)) {
                String s = value == null ? "" : String.valueOf(value);
                java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
                byte[] bytes = md.digest(s.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                for (byte b : bytes) {
                    sb.append(String.format("%02x", b));
                }
                return sb.toString();
            }
            
            // ========== 数据脱敏函数 ==========
            
            // DESENSITIZE_PHONE - 手机号脱敏（保留前3后4位）
            if ("DESENSITIZE_PHONE".equalsIgnoreCase(expr)) {
                String s = value == null ? "" : String.valueOf(value);
                if (s.length() == 11) {
                    return s.substring(0, 3) + "****" + s.substring(7);
                }
                return s;
            }
            
            // DESENSITIZE_ID_CARD - 身份证号脱敏（保留前6后4位）
            if ("DESENSITIZE_ID_CARD".equalsIgnoreCase(expr)) {
                String s = value == null ? "" : String.valueOf(value);
                if (s.length() == 18) {
                    return s.substring(0, 6) + "********" + s.substring(14);
                } else if (s.length() == 15) {
                    return s.substring(0, 6) + "*****" + s.substring(11);
                }
                return s;
            }
            
            // DESENSITIZE_BANK_CARD - 银行卡号脱敏（保留前4后4位）
            if ("DESENSITIZE_BANK_CARD".equalsIgnoreCase(expr)) {
                String s = value == null ? "" : String.valueOf(value);
                if (s.length() >= 8) {
                    int starCount = s.length() - 8;
                    StringBuilder stars = new StringBuilder();
                    for (int i = 0; i < starCount; i++) {
                        stars.append("*");
                    }
                    return s.substring(0, 4) + stars.toString() + s.substring(s.length() - 4);
                }
                return s;
            }
            
            // DESENSITIZE_NAME - 姓名脱敏（保留姓氏）
            if ("DESENSITIZE_NAME".equalsIgnoreCase(expr)) {
                String s = value == null ? "" : String.valueOf(value);
                if (s.length() == 2) {
                    return s.substring(0, 1) + "*";
                } else if (s.length() == 3) {
                    return s.substring(0, 1) + "**";
                } else if (s.length() > 3) {
                    StringBuilder stars = new StringBuilder();
                    for (int i = 1; i < s.length(); i++) {
                        stars.append("*");
                    }
                    return s.substring(0, 1) + stars.toString();
                }
                return s;
            }
            
            // DESENSITIZE_EMAIL - 邮箱脱敏（保留前2位和@后的域名）
            if ("DESENSITIZE_EMAIL".equalsIgnoreCase(expr)) {
                String s = value == null ? "" : String.valueOf(value);
                int atIndex = s.indexOf('@');
                if (atIndex > 2) {
                    String prefix = s.substring(0, 2);
                    String domain = s.substring(atIndex);
                    int starCount = atIndex - 2;
                    StringBuilder stars = new StringBuilder();
                    for (int i = 0; i < starCount; i++) {
                        stars.append("*");
                    }
                    return prefix + stars.toString() + domain;
                }
                return s;
            }
            
            // DESENSITIZE_ADDRESS - 地址脱敏（保留省市，详细地址星号）
            if ("DESENSITIZE_ADDRESS".equalsIgnoreCase(expr)) {
                String s = value == null ? "" : String.valueOf(value);
                if (s.length() > 6) {
                    return s.substring(0, 6) + "****";
                }
                return s;
            }
            
        } catch (Exception ignore) {
            // 解析或执行失败，回退原值
            log.warn("函数转换失败: func={}, value={}, error={}", func, value, ignore.getMessage());
        }
        return value;
    }
}
