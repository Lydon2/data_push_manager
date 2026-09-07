-- ============================================
-- 多目标模式字段兼容修复
-- 版本: V1.3.1
-- 日期: 2024-11-24
-- 说明: 修复多目标模式保存草稿时的字段约束问题
-- ============================================

-- 修改 target_connector_id 为可空
ALTER TABLE `dp_task_config` 
MODIFY COLUMN `target_connector_id` bigint(20) DEFAULT NULL COMMENT '目标连接器ID（单目标模式使用，多目标模式为NULL）';

-- 修改 target_config 为可空
ALTER TABLE `dp_task_config` 
MODIFY COLUMN `target_config` text DEFAULT NULL COMMENT '目标端配置（JSON格式，单目标模式使用，多目标模式为NULL）';
