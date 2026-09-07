package com.datapush.manager.service;

import com.datapush.manager.dto.TransformPreviewRequest;
import com.datapush.manager.entity.FieldMapping;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 数据转换服务
 */
public interface TransformService {
    
    /**
     * 转换预览
     * @param request 预览请求
     * @return 转换后的值
     */
    String previewTransform(TransformPreviewRequest request);
    
    /**
     * 批量转换预览
     * @param request 包含 sourceData 和 mappings 的 Map
     * @return 包含 transformedData 的 Map
     */
    Map<String, Object> previewBatchTransform(Map<String, Object> request);
    
    /**
     * 验证函数表达式
     * @param expression 表达式
     * @return 验证结果
     */
    Map<String, Object> validateExpression(String expression);
    
    /**
     * 从Excel导入字段映射
     * @param file Excel文件
     * @return 字段映射列表
     */
    List<FieldMapping> importMappingsFromExcel(MultipartFile file);
    
    /**
     * 导出字段映射到Excel模板
     * @param mappings 字段映射列表
     * @return Excel文件字节数组
     */
    byte[] exportMappingsToExcel(List<FieldMapping> mappings);
    
    /**
     * 下载Excel模板
     * @return Excel模板文件字节数组
     */
    byte[] downloadExcelTemplate();
}
