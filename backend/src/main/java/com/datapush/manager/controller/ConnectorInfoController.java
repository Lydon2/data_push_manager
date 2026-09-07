package com.datapush.manager.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.datapush.manager.common.BusinessException;
import com.datapush.manager.common.Result;
import com.datapush.manager.entity.ConnectorInfo;
import com.datapush.manager.service.ConnectorInfoService;
import com.datapush.manager.validator.ConfigValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 连接器信息控制器
 * 支持配置校验
 *
 * @author Data Push Team
 * @since 2024-11-20
 */
@Slf4j
@RestController
@RequestMapping("/v1/connector")
public class ConnectorInfoController {

    @Autowired
    private ConnectorInfoService connectorInfoService;
    
    @Autowired
    private ConfigValidator configValidator;

    /**
     * 分页查询连接器列表
     */
    @GetMapping("/page")
    public Result<IPage<ConnectorInfo>> page(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String connectorName,
            @RequestParam(required = false) String connectorType) {
        
        Page<ConnectorInfo> page = new Page<>(current, size);
        LambdaQueryWrapper<ConnectorInfo> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(connectorName)) {
            wrapper.like(ConnectorInfo::getConnectorName, connectorName);
        }
        if (StringUtils.hasText(connectorType)) {
            wrapper.eq(ConnectorInfo::getConnectorType, connectorType);
        }
        
        wrapper.orderByDesc(ConnectorInfo::getCreateTime);
        IPage<ConnectorInfo> result = connectorInfoService.page(page, wrapper);
        
        return Result.success(result);
    }

    /**
     * 查询所有连接器（用于下拉选择）
     */
    @GetMapping("/list")
    public Result<List<ConnectorInfo>> list(@RequestParam(required = false) String connectorType) {
        LambdaQueryWrapper<ConnectorInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ConnectorInfo::getStatus, 1);
        
        if (StringUtils.hasText(connectorType)) {
            wrapper.eq(ConnectorInfo::getConnectorType, connectorType);
        }
        
        wrapper.orderByDesc(ConnectorInfo::getCreateTime);
        List<ConnectorInfo> list = connectorInfoService.list(wrapper);
        
        return Result.success(list);
    }

    /**
     * 根据ID查询连接器详情（前端编辑使用，返回解密后的密码）
     */
    @GetMapping("/{id}")
    public Result<ConnectorInfo> getById(@PathVariable Long id) {
        ConnectorInfo connectorInfo = connectorInfoService.getConnectorDetail(id);
        return Result.success(connectorInfo);
    }

    /**
     * 新增连接器
     */
    @PostMapping
    public Result<?> save(@Valid @RequestBody ConnectorInfo connectorInfo) {
        // 校验额外配置
        if (StringUtils.hasText(connectorInfo.getExtraConfig())) {
            ConfigValidator.ValidationResult result = 
                configValidator.validateConnectorExtraConfig(
                    connectorInfo.getExtraConfig(), 
                    connectorInfo.getConnectorType());
            if (!result.isValid()) {
                throw new BusinessException("额外配置校验失败: " + result.getErrorMessage());
            }
        }
        
        boolean success = connectorInfoService.saveConnector(connectorInfo);
        return success ? Result.success("新增成功") : Result.error("新增失败");
    }

    /**
     * 更新连接器
     */
    @PutMapping
    public Result<?> update(@Valid @RequestBody ConnectorInfo connectorInfo) {
        // 校验额外配置
        if (StringUtils.hasText(connectorInfo.getExtraConfig())) {
            ConfigValidator.ValidationResult result = 
                configValidator.validateConnectorExtraConfig(
                    connectorInfo.getExtraConfig(), 
                    connectorInfo.getConnectorType());
            if (!result.isValid()) {
                throw new BusinessException("额外配置校验失败: " + result.getErrorMessage());
            }
        }
        
        boolean success = connectorInfoService.updateConnector(connectorInfo);
        return success ? Result.success("更新成功") : Result.error("更新失败");
    }

    /**
     * 删除连接器
     */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        boolean success = connectorInfoService.removeById(id);
        return success ? Result.success("删除成功") : Result.error("删除失败");
    }

    /**
     * 测试连接
     */
    @PostMapping("/test/{id}")
    public Result<?> testConnection(@PathVariable Long id) {
        boolean success = connectorInfoService.testConnection(id);
        return success ? Result.success("连接测试成功") : Result.error("连接测试失败");
    }

    /**
     * 测试连接（不保存）
     */
    @PostMapping("/test")
    public Result<?> testConnectionWithoutSave(@RequestBody ConnectorInfo connectorInfo) {
        boolean success = connectorInfoService.testConnection(connectorInfo);
        return success ? Result.success("连接测试成功") : Result.error("连接测试失败");
    }
}
