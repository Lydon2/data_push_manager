package com.datapush.manager.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.datapush.manager.common.Result;
import com.datapush.manager.entity.InternalMessage;
import com.datapush.manager.mapper.InternalMessageMapper;
import com.datapush.manager.service.AlertService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 消息控制器
 */
@Slf4j
@RestController
@RequestMapping("/v1/message")
@CrossOrigin
public class MessageController {

    @Autowired
    private AlertService alertService;

    @Autowired
    private InternalMessageMapper internalMessageMapper;

    /**
     * 获取消息列表
     */
    @GetMapping("/list")
    public Result<IPage<InternalMessage>> getMessages(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String messageType,
            @RequestParam(required = false) Integer isRead) {
        try {
            Page<InternalMessage> page = new Page<>(current, size);
            LambdaQueryWrapper<InternalMessage> wrapper = new LambdaQueryWrapper<>();
            
            if (messageType != null && !messageType.isEmpty()) {
                wrapper.eq(InternalMessage::getMessageType, messageType);
            }
            if (isRead != null) {
                wrapper.eq(InternalMessage::getIsRead, isRead);
            }
            
            wrapper.orderByDesc(InternalMessage::getCreateTime);
            
            IPage<InternalMessage> result = internalMessageMapper.selectPage(page, wrapper);
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取消息列表失败", e);
            return Result.error("获取消息列表失败: " + e.getMessage());
        }
    }

    /**
     * 标记消息为已读
     */
    @PostMapping("/{id}/read")
    public Result<Void> markAsRead(@PathVariable Long id) {
        try {
            alertService.markMessageAsRead(id);
            return Result.success();
        } catch (Exception e) {
            log.error("标记消息为已读失败", e);
            return Result.error("标记消息为已读失败: " + e.getMessage());
        }
    }

    /**
     * 批量标记消息为已读
     */
    @PostMapping("/batch-read")
    public Result<Void> batchMarkAsRead(@RequestBody List<Long> ids) {
        try {
            for (Long id : ids) {
                alertService.markMessageAsRead(id);
            }
            return Result.success();
        } catch (Exception e) {
            log.error("批量标记消息为已读失败", e);
            return Result.error("批量标记消息为已读失败: " + e.getMessage());
        }
    }

    /**
     * 删除消息
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteMessage(@PathVariable Long id) {
        try {
            internalMessageMapper.deleteById(id);
            return Result.success();
        } catch (Exception e) {
            log.error("删除消息失败", e);
            return Result.error("删除消息失败: " + e.getMessage());
        }
    }
}
