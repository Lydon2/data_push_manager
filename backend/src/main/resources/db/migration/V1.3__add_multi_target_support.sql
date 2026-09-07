-- ============================================
-- 多目标支持 - 数据库变更脚本
-- 版本: V1.3
-- 日期: 2024-11-24
-- 说明: 支持单任务推送到多个目标表
-- ============================================

-- 1. 新增任务目标配置表
CREATE TABLE IF NOT EXISTS `dp_task_target` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `task_id` bigint(20) NOT NULL COMMENT '任务ID',
  `target_connector_id` bigint(20) NOT NULL COMMENT '目标连接器ID',
  `target_config` text COMMENT '目标端配置（JSON格式）
示例: {"tableName":"user_info","writeMode":"INSERT","batchSize":1000,"maxRetries":3,"idempotentKey":"user_id"}',
  `target_name` varchar(100) DEFAULT NULL COMMENT '目标名称（用于区分）',
  `sort_order` int(11) DEFAULT '0' COMMENT '执行顺序',
  `status` tinyint(1) DEFAULT '1' COMMENT '状态：0-禁用 1-启用',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_task_id` (`task_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务目标配置表（支持多目标）';

-- 2. 修改任务配置表：添加多目标模式标识
ALTER TABLE `dp_task_config` 
ADD COLUMN `multi_target` tinyint(1) DEFAULT '0' COMMENT '是否多目标模式：0-单目标（兼容旧版）1-多目标' AFTER `target_connector_id`;

-- 3. 修改字段映射表：添加目标表ID关联
ALTER TABLE `dp_field_mapping` 
ADD COLUMN `target_id` bigint(20) DEFAULT NULL COMMENT '目标表ID（关联dp_task_target.id，多目标模式使用）' AFTER `task_id`,
ADD KEY `idx_target_id` (`target_id`);

-- 4. 添加外键约束（可选，根据需要启用）
-- ALTER TABLE `dp_task_target` ADD CONSTRAINT `fk_task_target_task` FOREIGN KEY (`task_id`) REFERENCES `dp_task_config` (`id`) ON DELETE CASCADE;
-- ALTER TABLE `dp_field_mapping` ADD CONSTRAINT `fk_field_mapping_target` FOREIGN KEY (`target_id`) REFERENCES `dp_task_target` (`id`) ON DELETE CASCADE;

-- 5. 数据兼容性处理：为现有任务创建默认目标配置
-- 注意：只为非多目标模式的现有任务创建默认目标
INSERT INTO `dp_task_target` (`task_id`, `target_connector_id`, `target_config`, `target_name`, `sort_order`, `status`)
SELECT 
    `id` AS task_id,
    `target_connector_id`,
    `target_config`,
    '默认目标' AS target_name,
    0 AS sort_order,
    `status`
FROM `dp_task_config`
WHERE `multi_target` = 0 
  AND `target_connector_id` IS NOT NULL
  AND `deleted` = 0
  AND NOT EXISTS (
    SELECT 1 FROM `dp_task_target` WHERE `task_id` = `dp_task_config`.`id`
  );

-- 6. 更新现有字段映射：关联到默认目标
UPDATE `dp_field_mapping` fm
INNER JOIN `dp_task_target` tt ON fm.`task_id` = tt.`task_id` AND tt.`target_name` = '默认目标'
SET fm.`target_id` = tt.`id`
WHERE fm.`target_id` IS NULL;

-- 7. 添加注释
ALTER TABLE `dp_task_target` COMMENT = '任务目标配置表（支持单任务推送到多个目标表）';
