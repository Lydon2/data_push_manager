package com.datapush.manager.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.datapush.manager.common.Result;
import com.datapush.manager.dto.TableColumnInfo;
import com.datapush.manager.entity.DictSource;
import com.datapush.manager.service.DictSourceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 字典数据源控制器
 * 
 * @author Data Push Team
 * @since 2024-11-21
 */
@Slf4j
@RestController
@RequestMapping("/v1/dict-source")
public class DictSourceController {
    
    @Autowired
    private DictSourceService dictSourceService;
    
    /**
     * 分页查询字典数据源
     */
    @GetMapping("/page")
    public Result<IPage<DictSource>> page(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String sourceName) {
        IPage<DictSource> page = dictSourceService.pageList(current, size, sourceName);
        return Result.success(page);
    }
    
    /**
     * 获取所有字典数据源（下拉选择）
     */
    @GetMapping("/list")
    public Result<List<DictSource>> list() {
        List<DictSource> list = dictSourceService.listAll();
        return Result.success(list);
    }
    
    /**
     * 根据ID获取字典数据源
     */
    @GetMapping("/{id}")
    public Result<DictSource> getById(@PathVariable Long id) {
        DictSource dictSource = dictSourceService.getById(id);
        return Result.success(dictSource);
    }
    
    /**
     * 新增字典数据源
     */
    @PostMapping
    public Result<DictSource> save(@RequestBody DictSource dictSource) {
        dictSourceService.save(dictSource);
        return Result.success(dictSource);
    }
    
    /**
     * 更新字典数据源
     */
    @PutMapping
    public Result<Void> update(@RequestBody DictSource dictSource) {
        dictSourceService.updateById(dictSource);
        return Result.success(null);
    }
    
    /**
     * 删除字典数据源
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        dictSourceService.removeById(id);
        return Result.success(null);
    }
    
    /**
     * 测试连接
     */
    @PostMapping("/{id}/test")
    public Result<Map<String, Object>> testConnection(@PathVariable Long id) {
        Map<String, Object> result = dictSourceService.testConnection(id);
        return Result.success(result);
    }
    
    /**
     * 预览数据
     */
    @GetMapping("/{id}/preview")
    public Result<Map<String, Object>> previewData(
            @PathVariable Long id,
            @RequestParam(defaultValue = "10") Integer limit) {
        Map<String, Object> result = dictSourceService.previewData(id, limit);
        return Result.success(result);
    }
    
    /**
     * 获取连接器的所有表
     */
    @GetMapping("/connector/{connectorId}/tables")
    public Result<List<String>> getTablesByConnector(@PathVariable Long connectorId) {
        List<String> tables = dictSourceService.getTablesByConnector(connectorId);
        return Result.success(tables);
    }
    
    /**
     * 获取表的所有字段
     */
    @GetMapping("/connector/{connectorId}/table/{tableName}/columns")
    public Result<List<String>> getTableColumns(
            @PathVariable Long connectorId,
            @PathVariable String tableName) {
        List<String> columns = dictSourceService.getTableColumns(connectorId, tableName);
        return Result.success(columns);
    }
    
    /**
     * 获取表的所有字段（包含类型信息）
     */
    @GetMapping("/connector/{connectorId}/table/{tableName}/columns-with-type")
    public Result<List<TableColumnInfo>> getTableColumnsWithType(
            @PathVariable Long connectorId,
            @PathVariable String tableName) {
        List<TableColumnInfo> columns = dictSourceService.getTableColumnsWithType(connectorId, tableName);
        return Result.success(columns);
    }
    
    /**
     * 获取字典数据源的所有类型值
     */
    @GetMapping("/{id}/types")
    public Result<List<Map<String, Object>>> getDistinctTypes(@PathVariable Long id) {
        List<Map<String, Object>> types = dictSourceService.getDistinctTypes(id);
        return Result.success(types);
    }
    
    /**
     * 获取指定类型的字典项
     */
    @GetMapping("/{id}/items")
    public Result<List<Map<String, Object>>> getDictItems(
            @PathVariable Long id,
            @RequestParam(required = false) String typeValue) {
        List<Map<String, Object>> items = dictSourceService.getDictItems(id, typeValue);
        return Result.success(items);
    }
}
