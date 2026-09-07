package com.datapush.manager.controller;

import com.datapush.manager.common.Result;
import com.datapush.manager.entity.AuxiliaryDatasource;
import com.datapush.manager.service.AuxiliaryDatasourceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 辅助数据源控制器
 */
@Slf4j
@RestController
@RequestMapping("/v1/auxiliary-datasource")
@CrossOrigin
public class AuxiliaryDatasourceController {

    @Autowired
    private AuxiliaryDatasourceService auxiliaryDatasourceService;

    /**
     * 根据任务ID查询辅助数据源列表
     */
    @GetMapping("/list/{taskId}")
    public Result<List<AuxiliaryDatasource>> listByTaskId(@PathVariable Long taskId) {
        try {
            List<AuxiliaryDatasource> list = auxiliaryDatasourceService.listByTaskId(taskId);
            return Result.success(list);
        } catch (Exception e) {
            log.error("查询辅助数据源列表失败", e);
            return Result.error("查询辅助数据源列表失败: " + e.getMessage());
        }
    }

    /**
     * 批量保存辅助数据源配置
     */
    @PostMapping("/batch/{taskId}")
    public Result<Void> saveBatch(@PathVariable Long taskId, @RequestBody List<AuxiliaryDatasource> auxiliaryDatasources) {
        try {
            auxiliaryDatasourceService.saveBatch(taskId, auxiliaryDatasources);
            return Result.success();
        } catch (Exception e) {
            log.error("保存辅助数据源配置失败", e);
            return Result.error("保存辅助数据源配置失败: " + e.getMessage());
        }
    }

    /**
     * 删除辅助数据源
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        try {
            auxiliaryDatasourceService.removeById(id);
            return Result.success();
        } catch (Exception e) {
            log.error("删除辅助数据源失败", e);
            return Result.error("删除辅助数据源失败: " + e.getMessage());
        }
    }
}
