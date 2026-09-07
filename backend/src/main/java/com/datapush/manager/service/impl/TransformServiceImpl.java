package com.datapush.manager.service.impl;

import com.datapush.manager.dto.TransformPreviewRequest;
import com.datapush.manager.entity.FieldMapping;
import com.datapush.manager.service.DictMappingService;
import com.datapush.manager.service.TransformService;
import com.datapush.manager.transformer.AdvancedTransformer;
import com.datapush.manager.transformer.DataTransformer;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

/**
 * 数据转换服务实现
 */
@Slf4j
@Service
public class TransformServiceImpl implements TransformService {
    
    @Autowired
    private DictMappingService dictMappingService;
    
    @Autowired
    private DataTransformer dataTransformer;
    
    @Autowired
    private AdvancedTransformer advancedTransformer;
    
    @Override
    public String previewTransform(TransformPreviewRequest request) {
        try {
            String testValue = request.getTestValue();
            
            switch (request.getTransformType()) {
                case "DIRECT":
                    return testValue;
                    
                case "FUNCTION":
                    return dataTransformer.applyFunction(testValue, request.getTransformFunction());
                    
                case "DICT":
                    return applyDictMapping(testValue, request.getDictMappingId(), request.getDictSourceTypeValue(), request.getDefaultValue());
                    
                case "SCRIPT":
                    return applyScript(testValue, request.getTransformScript());
                    
                case "CONSTANT":
                    return request.getConstantValue();
                    
                default:
                    throw new RuntimeException("不支持的转换类型: " + request.getTransformType());
            }
        } catch (Exception e) {
            log.error("转换预览失败", e);
            throw new RuntimeException("转换预览失败: " + e.getMessage());
        }
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> previewBatchTransform(Map<String, Object> request) {
        try {
            List<Map<String, Object>> sourceData = (List<Map<String, Object>>) request.get("sourceData");
            List<Map<String, Object>> mappings = (List<Map<String, Object>>) request.get("mappings");
            
            if (sourceData == null || sourceData.isEmpty()) {
                throw new RuntimeException("源数据为空");
            }
            
            if (mappings == null || mappings.isEmpty()) {
                throw new RuntimeException("字段映射为空");
            }
            
            // 转换每一条数据
            List<Map<String, Object>> transformedData = new ArrayList<>();
            
            for (Map<String, Object> sourceRow : sourceData) {
                Map<String, Object> targetRow = new LinkedHashMap<>();
                
                // 应用所有字段映射
                for (Map<String, Object> mapping : mappings) {
                    String sourceField = (String) mapping.get("sourceField");
                    String targetField = (String) mapping.get("targetField");
                    String transformType = (String) mapping.get("transformType");
                    
                    if (targetField == null || targetField.trim().isEmpty()) {
                        continue; // 跳过没有目标字段的映射
                    }
                    
                    Object sourceValue = sourceField != null ? sourceRow.get(sourceField) : null;
                    Object targetValue = null;
                    
                    try {
                        targetValue = transformValue(sourceValue, transformType, mapping, sourceRow);
                    } catch (Exception e) {
                        log.error("字段 {} 转换失败: {}", targetField, e.getMessage());
                        targetValue = "[ERROR: " + e.getMessage() + "]";
                    }
                    
                    targetRow.put(targetField, targetValue);
                }
                
                transformedData.add(targetRow);
            }
            
            // 返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("transformedData", transformedData);
            result.put("totalCount", transformedData.size());
            
            return result;
            
        } catch (Exception e) {
            log.error("批量转换预览失败", e);
            throw new RuntimeException("批量转换预览失败: " + e.getMessage());
        }
    }
    
    /**
     * 转换单个字段值
     */
    private Object transformValue(Object sourceValue, String transformType, Map<String, Object> mapping, Map<String, Object> sourceRow) {
        String valueStr = sourceValue != null ? sourceValue.toString() : null;
        
        switch (transformType) {
            case "DIRECT":
                return sourceValue;
                
            case "FUNCTION":
                String transformFunction = (String) mapping.get("transformFunction");
                if (transformFunction != null && !transformFunction.isEmpty()) {
                    return dataTransformer.applyFunction(valueStr, transformFunction);
                }
                return sourceValue;
                
            case "DICT":
                Object dictMappingIdObj = mapping.get("dictMappingId");
                Long dictMappingId = null;
                if (dictMappingIdObj instanceof Number) {
                    dictMappingId = ((Number) dictMappingIdObj).longValue();
                }
                String dictSourceTypeValue = (String) mapping.get("dictSourceTypeValue");
                String defaultValue = (String) mapping.get("defaultValue");
                if (dictMappingId != null) {
                    return applyDictMapping(valueStr, dictMappingId, dictSourceTypeValue, defaultValue);
                }
                return sourceValue;
                
            case "SCRIPT":
                String transformScript = (String) mapping.get("transformScript");
                if (transformScript != null && !transformScript.isEmpty()) {
                    return applyScriptWithRow(valueStr, transformScript, sourceRow);
                }
                return sourceValue;
                
            case "CONSTANT":
                String constantValue = (String) mapping.get("constantValue");
                return constantValue != null ? constantValue : "";
                
            default:
                return sourceValue;
        }
    }
    
    /**
     * 执行 Groovy 脚本（支持整行数据）
     */
    private Object applyScriptWithRow(String value, String script, Map<String, Object> row) {
        try {
            log.debug("执行脚本转换 - value: {}, row字段: {}, script: {}", value, row != null ? row.keySet() : null, script);
            
            Binding binding = new Binding();
            binding.setVariable("value", value);
            binding.setVariable("row", row);
            
            GroovyShell shell = new GroovyShell(binding);
            Object result = shell.evaluate(script);
            
            log.debug("脚本执行结果: {}", result);
            return result != null ? result.toString() : null;
        } catch (Exception e) {
            log.error("脚本执行失败 - script: {}, value: {}, row: {}, error: {}", script, value, row, e.getMessage());
            throw new RuntimeException("脚本执行失败: " + e.getMessage());
        }
    }
    
    @Override
    public Map<String, Object> validateExpression(String expression) {
        Map<String, Object> result = new HashMap<>();
        
        if (expression == null || expression.trim().isEmpty()) {
            result.put("valid", false);
            result.put("message", "表达式不能为空");
            return result;
        }
        
        try {
            // 使用测试值验证表达式
            String testValue = "TEST_VALUE_123";
            String transformedValue = dataTransformer.applyFunction(testValue, expression);
            
            result.put("valid", true);
            result.put("message", "表达式有效");
            result.put("example", "输入: " + testValue + " → 输出: " + transformedValue);
        } catch (Exception e) {
            result.put("valid", false);
            result.put("message", "表达式错误: " + e.getMessage());
        }
        
        return result;
    }
    
    @Override
    public List<FieldMapping> importMappingsFromExcel(MultipartFile file) {
        List<FieldMapping> mappings = new ArrayList<>();
        
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            
            // 跳过表头，从第2行开始读取
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                
                FieldMapping mapping = new FieldMapping();
                
                // 源字段
                Cell sourceFieldCell = row.getCell(0);
                if (sourceFieldCell != null) {
                    mapping.setSourceField(getCellValueAsString(sourceFieldCell));
                }
                
                // 转换类型
                Cell transformTypeCell = row.getCell(1);
                if (transformTypeCell != null) {
                    mapping.setTransformType(getCellValueAsString(transformTypeCell));
                }
                
                // 转换规则
                Cell transformRuleCell = row.getCell(2);
                if (transformRuleCell != null) {
                    String rule = getCellValueAsString(transformRuleCell);
                    parseTransformRule(mapping, rule);
                }
                
                // 目标字段
                Cell targetFieldCell = row.getCell(3);
                if (targetFieldCell != null) {
                    mapping.setTargetField(getCellValueAsString(targetFieldCell));
                }
                
                // 说明
                Cell descCell = row.getCell(4);
                if (descCell != null) {
                    // 可以存储在备注字段中
                }
                
                // 验证必填字段
                if (mapping.getSourceField() != null && !mapping.getSourceField().isEmpty() &&
                    mapping.getTargetField() != null && !mapping.getTargetField().isEmpty()) {
                    mappings.add(mapping);
                }
            }
            
            log.info("从Excel导入了 {} 条字段映射", mappings.size());
            
        } catch (IOException e) {
            log.error("导入Excel失败", e);
            throw new RuntimeException("导入Excel失败: " + e.getMessage());
        }
        
        return mappings;
    }
    
    @Override
    public byte[] exportMappingsToExcel(List<FieldMapping> mappings) {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            
            Sheet sheet = workbook.createSheet("字段映射");
            
            // 创建表头样式
            CellStyle headerStyle = createHeaderStyle(workbook);
            
            // 创建表头
            Row headerRow = sheet.createRow(0);
            String[] headers = {"源字段", "转换类型", "转换规则", "目标字段", "说明"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            
            // 填充数据
            for (int i = 0; i < mappings.size(); i++) {
                FieldMapping mapping = mappings.get(i);
                Row row = sheet.createRow(i + 1);
                
                row.createCell(0).setCellValue(mapping.getSourceField());
                row.createCell(1).setCellValue(mapping.getTransformType());
                row.createCell(2).setCellValue(formatTransformRule(mapping));
                row.createCell(3).setCellValue(mapping.getTargetField());
                row.createCell(4).setCellValue(getTransformTypeDescription(mapping.getTransformType()));
            }
            
            // 自动调整列宽
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                sheet.setColumnWidth(i, sheet.getColumnWidth(i) + 1000); // 额外空间
            }
            
            workbook.write(out);
            return out.toByteArray();
            
        } catch (IOException e) {
            log.error("导出Excel失败", e);
            throw new RuntimeException("导出Excel失败: " + e.getMessage());
        }
    }
    
    @Override
    public byte[] downloadExcelTemplate() {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            
            Sheet sheet = workbook.createSheet("字段映射模板");
            
            // 创建表头样式
            CellStyle headerStyle = createHeaderStyle(workbook);
            
            // 创建表头
            Row headerRow = sheet.createRow(0);
            String[] headers = {"源字段", "转换类型", "转换规则", "目标字段", "说明"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            
            // 添加示例数据
            String[][] examples = {
                {"user_name", "DIRECT", "", "userName", "直接映射，不做转换"},
                {"phone", "FUNCTION", "DESENSITIZE_PHONE", "phone", "手机号脱敏"},
                {"status", "DICT", "字典ID:1", "status_name", "字典映射（需先在系统中创建字典）"},
                {"create_time", "FUNCTION", "DATE_FORMAT(yyyy-MM-dd)", "create_date", "日期格式化"},
                {"", "CONSTANT", "1001", "tenant_id", "固定值"},
                {"email", "FUNCTION", "LOWER", "email", "转小写"}
            };
            
            for (int i = 0; i < examples.length; i++) {
                Row row = sheet.createRow(i + 1);
                for (int j = 0; j < examples[i].length; j++) {
                    row.createCell(j).setCellValue(examples[i][j]);
                }
            }
            
            // 自动调整列宽
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                sheet.setColumnWidth(i, sheet.getColumnWidth(i) + 1000);
            }
            
            workbook.write(out);
            return out.toByteArray();
            
        } catch (IOException e) {
            log.error("生成Excel模板失败", e);
            throw new RuntimeException("生成Excel模板失败: " + e.getMessage());
        }
    }
    
    /**
     * 应用字典映射
     */
    private String applyDictMapping(String value, Long dictMappingId, String dictSourceTypeValue, String defaultValue) {
        if (dictMappingId == null) {
            return defaultValue;
        }
        
        // 使用新的字典映射服务，指定类型
        if (dictSourceTypeValue != null && !dictSourceTypeValue.isEmpty()) {
            return dictMappingService.transform(dictMappingId, dictSourceTypeValue, value);
        } else {
            // 如果没有指定类型，使用旧的方法（兼容）
            return dictMappingService.transform(dictMappingId, value);
        }
    }
    
    /**
     * 应用脚本转换
     */
    private String applyScript(String value, String script) {
        try {
            Binding binding = new Binding();
            binding.setVariable("value", value);
            
            GroovyShell shell = new GroovyShell(binding);
            Object result = shell.evaluate(script);
            
            return result != null ? result.toString() : null;
        } catch (Exception e) {
            log.error("执行Groovy脚本失败", e);
            throw new RuntimeException("脚本执行失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取单元格值（字符串形式）
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null) return null;
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                return String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }
    
    /**
     * 解析转换规则
     */
    private void parseTransformRule(FieldMapping mapping, String rule) {
        if (rule == null || rule.trim().isEmpty()) {
            return;
        }
        
        String transformType = mapping.getTransformType();
        
        if ("CONSTANT".equals(transformType)) {
            mapping.setConstantValue(rule);
        } else if ("DICT".equals(transformType)) {
            // 格式: 字典ID:123
            if (rule.startsWith("字典ID:")) {
                String dictIdStr = rule.substring(4);
                try {
                    mapping.setDictMappingId(Long.parseLong(dictIdStr));
                } catch (NumberFormatException e) {
                    log.warn("解析字典ID失败: {}", rule);
                }
            }
        }
        // FUNCTION和SCRIPT类型已废弃，所有函数处理应该在cleanseFunctions中配置
    }
    
    /**
     * 格式化转换规则
     */
    private String formatTransformRule(FieldMapping mapping) {
        String transformType = mapping.getTransformType();
        
        switch (transformType) {
            case "DIRECT":
                return "";
            case "CONSTANT":
                return mapping.getConstantValue() != null ? mapping.getConstantValue() : "";
            case "DICT":
                return mapping.getDictMappingId() != null ? "字典ID:" + mapping.getDictMappingId() : "";
            default:
                return "";
        }
        // FUNCTION和SCRIPT类型已废弃
    }
    
    /**
     * 获取转换类型说明
     */
    private String getTransformTypeDescription(String transformType) {
        switch (transformType) {
            case "DIRECT":
                return "直接映射";
            case "CONSTANT":
                return "固定值";
            case "DICT":
                return "字典映射";
            default:
                return "";
        }
        // FUNCTION和SCRIPT类型已废弃，所有函数处理应该在cleanseFunctions中配置
    }
    
    /**
     * 创建表头样式
     */
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        
        return style;
    }
}
