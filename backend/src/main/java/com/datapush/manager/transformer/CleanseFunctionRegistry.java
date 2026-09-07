package com.datapush.manager.transformer;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 数据清洗函数注册中心
 * 开发者可以在这里注册自定义的清洗函数
 */
@Component
public class CleanseFunctionRegistry {
    
    /**
     * 函数注册表
     */
    private final Map<String, CleanseFunction> functionRegistry = new LinkedHashMap<>();
    
    /**
     * SimpleDateFormat缓存池（性能优化）
     * Key: 日期格式字符串
     * Value: SimpleDateFormat实例
     * 注意：SimpleDateFormat不是线程安全的，但在ETL场景下每个任务是单线程执行
     */
    private final Map<String, java.text.SimpleDateFormat> dateFormatCache = new ConcurrentHashMap<>();
    
    /**
     * Pattern编译缓存（性能优化）
     * Key: 正则表达式
     * Value: Pattern实例
     */
    private final Map<String, java.util.regex.Pattern> patternCache = new ConcurrentHashMap<>();
    
    public CleanseFunctionRegistry() {
        registerBuiltInFunctions();
    }
    
    /**
     * 注册内置函数
     */
    private void registerBuiltInFunctions() {
        // ========== 文本处理函数 ==========
        
        // 去除首尾空格
        register(new CleanseFunction(
            "TRIM",
            "去除首尾空格",
            "TEXT",
            "去除字符串首尾的空白字符",
            Collections.emptyList(),
            "BUILTIN"
        ));
        
        // 去除所有空格
        register(new CleanseFunction(
            "TRIM_ALL",
            "去除所有空格",
            "TEXT",
            "去除字符串中的所有空格",
            Collections.emptyList(),
            "BUILTIN"
        ));
        
        // 转大写
        register(new CleanseFunction(
            "UPPER",
            "转大写",
            "TEXT",
            "将字符串转换为大写",
            Collections.emptyList(),
            "BUILTIN"
        ));
        
        // 转小写
        register(new CleanseFunction(
            "LOWER",
            "转小写",
            "TEXT",
            "将字符串转换为小写",
            Collections.emptyList(),
            "BUILTIN"
        ));
        
        // 字符串替换
        register(new CleanseFunction(
            "REPLACE",
            "字符串替换",
            "TEXT",
            "将指定字符串替换为另一个字符串",
            Arrays.asList(
                new CleanseFunction.ParamDefinition("oldStr", "原字符串", "STRING", true, null, null, "要被替换的字符串"),
                new CleanseFunction.ParamDefinition("newStr", "新字符串", "STRING", true, "", null, "替换后的字符串")
            ),
            "BUILTIN"
        ));
        
        // 正则替换
        register(new CleanseFunction(
            "REPLACE_REGEX",
            "正则替换",
            "TEXT",
            "使用正则表达式替换字符串",
            Arrays.asList(
                new CleanseFunction.ParamDefinition("pattern", "正则表达式", "STRING", true, null, null, "正则表达式模式"),
                new CleanseFunction.ParamDefinition("replacement", "替换内容", "STRING", true, "", null, "替换后的内容")
            ),
            "BUILTIN"
        ));
        
        // 字符串截取
        register(new CleanseFunction(
            "SUBSTRING",
            "字符串截取",
            "TEXT",
            "截取字符串的一部分",
            Arrays.asList(
                new CleanseFunction.ParamDefinition("start", "起始位置", "NUMBER", true, "0", null, "起始索引(从0开始)"),
                new CleanseFunction.ParamDefinition("length", "长度", "NUMBER", false, null, null, "截取长度(不填则到末尾)")
            ),
            "BUILTIN"
        ));
        
        // 左填充
        register(new CleanseFunction(
            "LPAD",
            "左填充",
            "TEXT",
            "在字符串左侧填充字符到指定长度",
            Arrays.asList(
                new CleanseFunction.ParamDefinition("length", "目标长度", "NUMBER", true, null, null, "填充后的总长度"),
                new CleanseFunction.ParamDefinition("padChar", "填充字符", "STRING", false, "0", null, "用于填充的字符")
            ),
            "BUILTIN"
        ));
        
        // 右填充
        register(new CleanseFunction(
            "RPAD",
            "右填充",
            "TEXT",
            "在字符串右侧填充字符到指定长度",
            Arrays.asList(
                new CleanseFunction.ParamDefinition("length", "目标长度", "NUMBER", true, null, null, "填充后的总长度"),
                new CleanseFunction.ParamDefinition("padChar", "填充字符", "STRING", false, "0", null, "用于填充的字符")
            ),
            "BUILTIN"
        ));
        
        // 字符串拆分
        register(new CleanseFunction(
            "SPLIT",
            "字符串拆分",
            "TEXT",
            "按分隔符拆分字符串并取指定索引的值",
            Arrays.asList(
                new CleanseFunction.ParamDefinition("delimiter", "分隔符", "STRING", true, ",", null, "分隔符，如: ,"),
                new CleanseFunction.ParamDefinition("index", "索引", "NUMBER", true, "0", null, "取第几个，从0开始")
            ),
            "BUILTIN"
        ));
        
        // 文本拼接
        register(new CleanseFunction(
            "CONCAT",
            "文本拼接",
            "TEXT",
            "拼接文本到字符串末尾",
            Arrays.asList(
                new CleanseFunction.ParamDefinition("text", "拼接文本", "STRING", true, "", null, "要拼接的文本")
            ),
            "BUILTIN"
        ));
        
        // 前缀拼接
        register(new CleanseFunction(
            "CONCAT_PREFIX",
            "前缀拼接",
            "TEXT",
            "在字符串前面添加前缀",
            Arrays.asList(
                new CleanseFunction.ParamDefinition("prefix", "前缀", "STRING", true, "", null, "要添加的前缀")
            ),
            "BUILTIN"
        ));
        
        // 后缀拼接
        register(new CleanseFunction(
            "CONCAT_SUFFIX",
            "后缀拼接",
            "TEXT",
            "在字符串后面添加后缀",
            Arrays.asList(
                new CleanseFunction.ParamDefinition("suffix", "后缀", "STRING", true, "", null, "要添加的后缀")
            ),
            "BUILTIN"
        ));
        
        // 获取长度
        register(new CleanseFunction(
            "LENGTH",
            "获取长度",
            "TEXT",
            "获取字符串长度",
            Collections.emptyList(),
            "BUILTIN"
        ));
        
        // 字符反转
        register(new CleanseFunction(
            "REVERSE",
            "字符反转",
            "TEXT",
            "反转字符串",
            Collections.emptyList(),
            "BUILTIN"
        ));
        
        // 转字符串
        register(new CleanseFunction(
            "TO_STRING",
            "转字符串",
            "TEXT",
            "将值转换为字符串",
            Collections.emptyList(),
            "BUILTIN"
        ));
        
        // ========== 数值处理函数 ==========
        
        // 四舍五入
        register(new CleanseFunction(
            "ROUND",
            "四舍五入",
            "NUMBER",
            "对数值进行四舍五入",
            Arrays.asList(
                new CleanseFunction.ParamDefinition("scale", "小数位数", "NUMBER", false, "0", null, "保留的小数位数")
            ),
            "BUILTIN"
        ));
        
        // 向上取整
        register(new CleanseFunction(
            "CEIL",
            "向上取整",
            "NUMBER",
            "向上取整到最接近的整数",
            Collections.emptyList(),
            "BUILTIN"
        ));
        
        // 向下取整
        register(new CleanseFunction(
            "FLOOR",
            "向下取整",
            "NUMBER",
            "向下取整到最接近的整数",
            Collections.emptyList(),
            "BUILTIN"
        ));
        
        // 绝对值
        register(new CleanseFunction(
            "ABS",
            "绝对值",
            "NUMBER",
            "取数值的绝对值",
            Collections.emptyList(),
            "BUILTIN"
        ));
        
        // 转整数
        register(new CleanseFunction(
            "TO_INT",
            "转整数",
            "NUMBER",
            "将值转换为整数",
            Collections.emptyList(),
            "BUILTIN"
        ));
        
        // 转小数
        register(new CleanseFunction(
            "TO_DECIMAL",
            "转小数",
            "NUMBER",
            "将值转换为小数并保留指定位数",
            Arrays.asList(
                new CleanseFunction.ParamDefinition("scale", "小数位数", "NUMBER", false, "2", null, "保留的小数位数")
            ),
            "BUILTIN"
        ));
        
        // 加法运算
        register(new CleanseFunction(
            "ADD",
            "加法运算",
            "NUMBER",
            "对数值进行加法运算",
            Arrays.asList(
                new CleanseFunction.ParamDefinition("number", "加数", "NUMBER", true, "0", null, "要加的数值")
            ),
            "BUILTIN"
        ));
        
        // 乘法运算
        register(new CleanseFunction(
            "MULTIPLY",
            "乘法运算",
            "NUMBER",
            "对数值进行乘法运算",
            Arrays.asList(
                new CleanseFunction.ParamDefinition("number", "乘数", "NUMBER", true, "1", null, "要乘的数值")
            ),
            "BUILTIN"
        ));
        
        // ========== 编码转换函数 ==========
        
        // URL编码
        register(new CleanseFunction(
            "URL_ENCODE",
            "URL编码",
            "ENCODING",
            "对字符串进行URL编码",
            Collections.emptyList(),
            "BUILTIN"
        ));
        
        // URL解码
        register(new CleanseFunction(
            "URL_DECODE",
            "URL解码",
            "ENCODING",
            "对字符串进行URL解码",
            Collections.emptyList(),
            "BUILTIN"
        ));
        
        // Base64编码
        register(new CleanseFunction(
            "BASE64_ENCODE",
            "Base64编码",
            "ENCODING",
            "对字符串进行Base64编码",
            Collections.emptyList(),
            "BUILTIN"
        ));
        
        // Base64解码
        register(new CleanseFunction(
            "BASE64_DECODE",
            "Base64解码",
            "ENCODING",
            "对字符串进行Base64解码",
            Collections.emptyList(),
            "BUILTIN"
        ));
        
        // MD5哈希
        register(new CleanseFunction(
            "MD5",
            "MD5哈希",
            "ENCODING",
            "计算字符串的MD5哈希值",
            Collections.emptyList(),
            "BUILTIN"
        ));
        
        // ========== 日期处理函数 ==========
        
        // 日期格式化
        register(new CleanseFunction(
            "DATE_FORMAT",
            "日期格式化",
            "DATE",
            "将日期格式化为指定格式",
            Arrays.asList(
                new CleanseFunction.ParamDefinition("format", "日期格式", "STRING", true, "yyyy-MM-dd HH:mm:ss", null, "如: yyyy-MM-dd HH:mm:ss")
            ),
            "BUILTIN"
        ));
    }
    
    /**
     * 注册自定义函数
     */
    public void register(CleanseFunction function) {
        functionRegistry.put(function.getFunctionCode(), function);
    }
    
    /**
     * 获取所有函数列表
     */
    public List<CleanseFunction> getAllFunctions() {
        return new ArrayList<>(functionRegistry.values());
    }
    
    /**
     * 按分类获取函数
     */
    public Map<String, List<CleanseFunction>> getFunctionsByCategory() {
        return functionRegistry.values().stream()
            .collect(Collectors.groupingBy(
                CleanseFunction::getCategory,
                LinkedHashMap::new,
                Collectors.toList()
            ));
    }
    
    /**
     * 获取根据函数代码获取函数定义
     */
    public CleanseFunction getFunction(String functionCode) {
        return functionRegistry.get(functionCode);
    }
    
    /**
     * 从缓存池获取SimpleDateFormat（性能优化）
     */
    private java.text.SimpleDateFormat getDateFormat(String pattern) {
        return dateFormatCache.computeIfAbsent(pattern, p -> {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(p);
            sdf.setLenient(false); // 严格模式
            return sdf;
        });
    }
    
    /**
     * 执行清洗函数
     */
    public Object executeFunction(String functionCode, Object value, Map<String, Object> params) {
        if (value == null) {
            return null;
        }
        
        switch (functionCode) {
            // 文本处理
            case "TRIM":
                return String.valueOf(value).trim();
                
            case "TRIM_ALL":
                return String.valueOf(value).replaceAll("\\s+", "");
                
            case "UPPER":
                return String.valueOf(value).toUpperCase();
                
            case "LOWER":
                return String.valueOf(value).toLowerCase();
                
            case "REPLACE":
                String oldStr = (String) params.get("oldStr");
                String newStr = (String) params.getOrDefault("newStr", "");
                return String.valueOf(value).replace(oldStr, newStr);
                
            case "REPLACE_REGEX":
                String pattern = (String) params.get("pattern");
                String replacement = (String) params.getOrDefault("replacement", "");
                // 使用缓存避免重复编译正则表达式（性能优化）
                java.util.regex.Pattern regex = patternCache.computeIfAbsent(pattern, java.util.regex.Pattern::compile);
                return regex.matcher(String.valueOf(value)).replaceAll(replacement);
                
            case "SUBSTRING":
                String str = String.valueOf(value);
                int start = Integer.parseInt(String.valueOf(params.get("start")));
                Object lengthObj = params.get("length");
                int end = lengthObj != null ? start + Integer.parseInt(String.valueOf(lengthObj)) : str.length();
                start = Math.max(0, Math.min(start, str.length()));
                end = Math.max(start, Math.min(end, str.length()));
                return str.substring(start, end);
                
            case "LPAD":
                int lpadLength = Integer.parseInt(String.valueOf(params.get("length")));
                String lpadChar = (String) params.getOrDefault("padChar", "0");
                StringBuilder lpadResult = new StringBuilder(String.valueOf(value));
                while (lpadResult.length() < lpadLength) {
                    lpadResult.insert(0, lpadChar.charAt(0));
                }
                return lpadResult.toString();
                
            case "RPAD":
                int rpadLength = Integer.parseInt(String.valueOf(params.get("length")));
                String rpadChar = (String) params.getOrDefault("padChar", "0");
                StringBuilder rpadResult = new StringBuilder(String.valueOf(value));
                while (rpadResult.length() < rpadLength) {
                    rpadResult.append(rpadChar.charAt(0));
                }
                return rpadResult.toString();
                
            case "SPLIT":
                String delimiter = (String) params.get("delimiter");
                int index = Integer.parseInt(String.valueOf(params.get("index")));
                String[] parts = String.valueOf(value).split(delimiter, -1);
                if (index >= 0 && index < parts.length) {
                    return parts[index];
                }
                return "";
                
            case "CONCAT":
                String text = (String) params.get("text");
                return String.valueOf(value) + text;
                
            case "CONCAT_PREFIX":
                String prefix = (String) params.get("prefix");
                return prefix + String.valueOf(value);
                
            case "CONCAT_SUFFIX":
                String suffix = (String) params.get("suffix");
                return String.valueOf(value) + suffix;
                
            case "LENGTH":
                return String.valueOf(value).length();
                
            case "REVERSE":
                return new StringBuilder(String.valueOf(value)).reverse().toString();
                
            case "TO_STRING":
                return String.valueOf(value);
                
            // 数值处理
            case "ROUND":
                int scale = Integer.parseInt(String.valueOf(params.getOrDefault("scale", "0")));
                java.math.BigDecimal bd = new java.math.BigDecimal(String.valueOf(value));
                return bd.setScale(scale, java.math.RoundingMode.HALF_UP).toString();
                
            case "CEIL":
                return Math.ceil(Double.parseDouble(String.valueOf(value)));
                
            case "FLOOR":
                return Math.floor(Double.parseDouble(String.valueOf(value)));
                
            case "ABS":
                return Math.abs(Double.parseDouble(String.valueOf(value)));
                
            case "TO_INT":
                return Integer.parseInt(String.valueOf(value).split("\\.")[0]);
                
            case "TO_DECIMAL":
                int decimalScale = Integer.parseInt(String.valueOf(params.getOrDefault("scale", "2")));
                java.math.BigDecimal decimalBd = new java.math.BigDecimal(String.valueOf(value));
                return decimalBd.setScale(decimalScale, java.math.RoundingMode.HALF_UP).toString();
                
            case "ADD":
                double addNumber = Double.parseDouble(String.valueOf(params.get("number")));
                double addValue = Double.parseDouble(String.valueOf(value));
                return addValue + addNumber;
                
            case "MULTIPLY":
                double multiplyNumber = Double.parseDouble(String.valueOf(params.get("number")));
                double multiplyValue = Double.parseDouble(String.valueOf(value));
                return multiplyValue * multiplyNumber;
                
            // 编码转换
            case "URL_ENCODE":
                try {
                    return java.net.URLEncoder.encode(String.valueOf(value), "UTF-8");
                } catch (Exception e) {
                    return value;
                }
                
            case "URL_DECODE":
                try {
                    return java.net.URLDecoder.decode(String.valueOf(value), "UTF-8");
                } catch (Exception e) {
                    return value;
                }
                
            case "BASE64_ENCODE":
                try {
                    return java.util.Base64.getEncoder().encodeToString(String.valueOf(value).getBytes("UTF-8"));
                } catch (Exception e) {
                    return value;
                }
                
            case "BASE64_DECODE":
                try {
                    byte[] decodedBytes = java.util.Base64.getDecoder().decode(String.valueOf(value));
                    return new String(decodedBytes, "UTF-8");
                } catch (Exception e) {
                    return value;
                }
                
            case "MD5":
                try {
                    java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
                    byte[] messageDigest = md.digest(String.valueOf(value).getBytes("UTF-8"));
                    StringBuilder hexString = new StringBuilder();
                    for (byte b : messageDigest) {
                        String hex = Integer.toHexString(0xff & b);
                        if (hex.length() == 1) hexString.append('0');
                        hexString.append(hex);
                    }
                    return hexString.toString();
                } catch (Exception e) {
                    return value;
                }
                
            // 日期处理
            case "DATE_FORMAT":
                Object sourceFormatObj = params.get("sourceFormat");
                String targetFormat = (String) params.get("targetFormat");
                
                if (sourceFormatObj == null || targetFormat == null) {
                    return value;
                }
                
                // 处理sourceFormat：可能是字符串或数组
                String sourceFormat;
                if (sourceFormatObj instanceof java.util.List) {
                    // 前端传来的是数组，转成逗号分隔的字符串
                    sourceFormat = String.join(",", (java.util.List<String>) sourceFormatObj);
                } else {
                    sourceFormat = String.valueOf(sourceFormatObj);
                }
                
                try {
                    // 使用缓存的SimpleDateFormat（性能优化）
                    java.text.SimpleDateFormat targetSdf = getDateFormat(targetFormat);
                    
                    // 如果是Date类型，直接格式化
                    if (value instanceof java.util.Date) {
                        synchronized (targetSdf) { // SimpleDateFormat不线程安全
                            return targetSdf.format((java.util.Date) value);
                        }
                    }
                    
                    // 如果是字符串，支持多个源格式依次尝试解析
                    String dateStr = String.valueOf(value).trim();
                    if (!dateStr.isEmpty()) {
                        // 将sourceFormat按逗号或端线分割为多个格式
                        String[] sourceFormats = sourceFormat.split("[,|\\|]");
                        
                        // 依次尝试每个源格式
                        for (String format : sourceFormats) {
                            format = format.trim();
                            if (format.isEmpty()) continue;
                            
                            try {
                                // 使用缓存的SimpleDateFormat（性能优化）
                                java.text.SimpleDateFormat sourceSdf = getDateFormat(format);
                                synchronized (sourceSdf) { // 同步保证线程安全
                                    java.util.Date date = sourceSdf.parse(dateStr);
                                    synchronized (targetSdf) {
                                        return targetSdf.format(date);
                                    }
                                }
                            } catch (Exception ignored) {
                                // 当前格式解析失败，尝试下一个
                            }
                        }
                    }
                    
                    // 所有格式都尝试失败，返回原值
                    return value;
                } catch (Exception e) {
                    // 日期格式化失败，返回原值
                    return value;
                }
                
            default:
                return value;
        }
    }
}
