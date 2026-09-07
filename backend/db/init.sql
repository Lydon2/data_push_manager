/*
 Navicat Premium Data Transfer

 Source Server         : localhost
 Source Server Type    : MySQL
 Source Server Version : 50719 (5.7.19)
 Source Host           : localhost:3306
 Source Schema         : data_push_manager

 Target Server Type    : MySQL
 Target Server Version : 50719 (5.7.19)
 File Encoding         : 65001

 Date: 07/09/2026 11:04:57
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for dp_alert_record
-- ----------------------------
DROP TABLE IF EXISTS `dp_alert_record`;
CREATE TABLE `dp_alert_record`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `rule_id` bigint(20) NOT NULL COMMENT '告警规则ID',
  `rule_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '规则名称',
  `alert_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '告警类型',
  `alert_level` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '告警级别',
  `task_id` bigint(20) NULL DEFAULT NULL COMMENT '关联任务ID',
  `task_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '任务名称',
  `execute_log_id` bigint(20) NULL DEFAULT NULL COMMENT '执行日志ID',
  `alert_title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '告警标题',
  `alert_content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '告警内容',
  `alert_time` datetime NOT NULL COMMENT '告警时间',
  `alert_status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'PENDING' COMMENT '告警状态：PENDING-待处理 NOTIFIED-已通知 HANDLED-已处理 IGNORED-已忽略',
  `send_channels` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '已发送渠道',
  `send_result` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '发送结果（JSON格式）',
  `handle_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '处理人',
  `handle_time` datetime NULL DEFAULT NULL COMMENT '处理时间',
  `handle_remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '处理备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_rule_id`(`rule_id`) USING BTREE,
  INDEX `idx_task_id`(`task_id`) USING BTREE,
  INDEX `idx_alert_time`(`alert_time`) USING BTREE,
  INDEX `idx_alert_status`(`alert_status`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '告警记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for dp_alert_rule
-- ----------------------------
DROP TABLE IF EXISTS `dp_alert_rule`;
CREATE TABLE `dp_alert_rule`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `rule_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '规则名称',
  `rule_code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '规则编码（唯一）',
  `rule_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '规则类型：TASK_FAIL-任务失败 TASK_DELAY-任务延迟 SYSTEM_ERROR-系统错误',
  `task_id` bigint(20) NULL DEFAULT NULL COMMENT '关联任务ID（为空表示全局规则）',
  `condition_config` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '条件配置（JSON格式）：如连续失败次数、延迟时间等',
  `alert_level` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'WARNING' COMMENT '告警级别：INFO-提示 WARNING-警告 ERROR-错误 CRITICAL-严重',
  `alert_channels` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '告警渠道（逗号分隔）：INTERNAL-内部消息 WEBHOOK-回调 LOG-日志',
  `channel_config` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '渠道配置（JSON格式）：Webhook地址等',
  `enabled` tinyint(4) NOT NULL DEFAULT 1 COMMENT '是否启用：0-禁用 1-启用',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '规则描述',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(4) NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_rule_code`(`rule_code`) USING BTREE,
  INDEX `idx_rule_type`(`rule_type`) USING BTREE,
  INDEX `idx_task_id`(`task_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '告警规则表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for dp_auxiliary_datasource
-- ----------------------------
DROP TABLE IF EXISTS `dp_auxiliary_datasource`;
CREATE TABLE `dp_auxiliary_datasource`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `task_id` bigint(20) NOT NULL COMMENT '任务ID',
  `connector_id` bigint(20) NOT NULL COMMENT '连接器ID',
  `connector_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '连接器类型：DATABASE/API',
  `alias` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '数据源别名（用于字段引用，如aux1, aux2）',
  `join_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'LEFT' COMMENT '关联类型：LEFT/INNER',
  `join_condition` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '关联条件配置（JSON格式）',
  `config` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '数据源配置（JSON格式，包含sql/apiPath等）',
  `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
  `sort_order` int(11) NOT NULL DEFAULT 0 COMMENT '排序序号',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_task_id`(`task_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '辅助数据源配置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for dp_connector_info
-- ----------------------------
DROP TABLE IF EXISTS `dp_connector_info`;
CREATE TABLE `dp_connector_info`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `connector_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '连接器名称',
  `connector_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '连接器类型：DATABASE/API/FILE',
  `db_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '数据库类型：MYSQL/ORACLE/POSTGRESQL/SQLSERVER/KINGBASE',
  `host` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '主机地址',
  `port` int(11) NULL DEFAULT NULL COMMENT '端口',
  `database_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '数据库名',
  `username` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '用户名',
  `password` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '密码（加密存储）',
  `url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'API地址或文件路径',
  `auth_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '认证类型：NONE/BASIC/BEARER/API_KEY/OAUTH2',
  `auth_config` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '认证配置（JSON格式，加密存储）',
  `extra_config` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '额外配置（JSON格式）',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '描述',
  `status` tinyint(4) NOT NULL DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(4) NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_connector_name`(`connector_name`) USING BTREE,
  INDEX `idx_connector_type`(`connector_type`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 20 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '连接器信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for dp_data_compare
-- ----------------------------
DROP TABLE IF EXISTS `dp_data_compare`;
CREATE TABLE `dp_data_compare`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `task_id` bigint(20) NOT NULL COMMENT '任务ID',
  `compare_time` datetime NOT NULL COMMENT '对比时间',
  `source_count` int(11) NULL DEFAULT 0 COMMENT '源端记录数',
  `target_count` int(11) NULL DEFAULT 0 COMMENT '目标端记录数',
  `match_count` int(11) NULL DEFAULT 0 COMMENT '匹配记录数',
  `diff_count` int(11) NULL DEFAULT 0 COMMENT '差异记录数',
  `source_only_count` int(11) NULL DEFAULT 0 COMMENT '仅源端存在记录数',
  `target_only_count` int(11) NULL DEFAULT 0 COMMENT '仅目标端存在记录数',
  `compare_fields` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '对比字段列表（JSON数组）',
  `diff_detail` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '差异详情（JSON）',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT 'SUCCESS' COMMENT '对比状态：SUCCESS/FAILED',
  `error_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '错误信息',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_task_id`(`task_id`) USING BTREE,
  INDEX `idx_compare_time`(`compare_time`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '数据对比记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for dp_data_lineage
-- ----------------------------
DROP TABLE IF EXISTS `dp_data_lineage`;
CREATE TABLE `dp_data_lineage`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `task_id` bigint(20) NOT NULL COMMENT '任务ID',
  `source_connector_id` bigint(20) NOT NULL COMMENT '源连接器ID',
  `source_table` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '源表名/API路径',
  `source_field` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '源字段名',
  `source_field_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '源字段类型',
  `target_connector_id` bigint(20) NOT NULL COMMENT '目标连接器ID',
  `target_table` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '目标表名/API路径',
  `target_field` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '目标字段名',
  `target_field_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '目标字段类型',
  `transform_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '转换类型：DIRECT/FUNCTION/DICT/SCRIPT/CONDITION',
  `transform_rule` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '转换规则描述',
  `dependent_fields` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '依赖字段（JSON数组）',
  `field_mapping_id` bigint(20) NULL DEFAULT NULL COMMENT '字段映射ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_task_id`(`task_id`) USING BTREE,
  INDEX `idx_source`(`source_connector_id`, `source_table`, `source_field`) USING BTREE,
  INDEX `idx_target`(`target_connector_id`, `target_table`, `target_field`) USING BTREE,
  INDEX `idx_field_mapping`(`field_mapping_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 754 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '数据血缘关系表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for dp_dict_item_mapping
-- ----------------------------
DROP TABLE IF EXISTS `dp_dict_item_mapping`;
CREATE TABLE `dp_dict_item_mapping`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `mapping_id` bigint(20) NOT NULL COMMENT '关联映射关系ID',
  `source_type_value` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '源字典类型值',
  `target_type_value` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '目标字典类型值',
  `source_key` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '源字典键值',
  `source_label` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '源字典标签（用于显示）',
  `target_key` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '目标字典键值',
  `target_label` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '目标字典标签（用于显示）',
  `sort_order` int(11) NOT NULL DEFAULT 0 COMMENT '排序',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_mapping_id`(`mapping_id`) USING BTREE,
  INDEX `idx_mapping_type`(`mapping_id`, `source_type_value`, `target_type_value`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 197 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '字典项映射表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for dp_dict_mapping
-- ----------------------------
DROP TABLE IF EXISTS `dp_dict_mapping`;
CREATE TABLE `dp_dict_mapping`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `mapping_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '映射名称，如：系统A民族->系统B民族',
  `mapping_code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '映射编码（唯一）',
  `mapping_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'SOURCE_TO_SOURCE' COMMENT '映射类型：SOURCE_TO_SOURCE-数据源映射 SOURCE_TO_CUSTOM-自定义映射',
  `source_dict_id` bigint(20) NULL DEFAULT NULL COMMENT '源字典数据源ID（自定义源场景可为空）',
  `target_dict_id` bigint(20) NULL DEFAULT NULL COMMENT '目标字典数据源ID（自定义映射时为空）',
  `default_value` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '未匹配时的默认值',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '描述',
  `status` tinyint(4) NOT NULL DEFAULT 1 COMMENT '状态：1-启用 0-禁用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `source_custom_items` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `type_labels` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_mapping_code`(`mapping_code`) USING BTREE,
  INDEX `idx_source_dict`(`source_dict_id`) USING BTREE,
  INDEX `idx_target_dict`(`target_dict_id`) USING BTREE,
  INDEX `idx_mapping_name`(`mapping_name`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '字典映射关系表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for dp_dict_source
-- ----------------------------
DROP TABLE IF EXISTS `dp_dict_source`;
CREATE TABLE `dp_dict_source`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `source_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '数据源名称，如：系统A-民族字典',
  `source_code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '数据源编码（唯一）',
  `connector_id` bigint(20) NOT NULL COMMENT '关联连接器ID',
  `table_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '字典表名',
  `key_field` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '字典键字段名（如：dict_code）',
  `value_field` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '字典值字段名（如：dict_name）',
  `type_field` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '字典类型字段名（如：dict_type），可选',
  `type_label_field` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '字典类型名称字段名（如：type_name），可选',
  `type_value` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '字典类型值（如：NATION），用于筛选',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '描述',
  `status` tinyint(4) NOT NULL DEFAULT 1 COMMENT '状态：1-启用 0-禁用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_source_code`(`source_code`) USING BTREE,
  INDEX `idx_connector_id`(`connector_id`) USING BTREE,
  INDEX `idx_source_name`(`source_name`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '字典数据源表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for dp_field_mapping
-- ----------------------------
DROP TABLE IF EXISTS `dp_field_mapping`;
CREATE TABLE `dp_field_mapping`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `task_id` bigint(20) NOT NULL COMMENT '任务ID',
  `target_id` bigint(20) NULL DEFAULT NULL COMMENT '目标表ID（关联dp_task_target.id，多目标模式使用）',
  `source_field` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '源字段名',
  `target_field` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '目标字段名',
  `source_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '源字段类型',
  `target_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '目标字段类型',
  `transform_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '转换类型：DIRECT-直接映射  DICT-字典映射 CONSTANT-固定值  SCRIPT--脚本',
  `transform_script` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '转换脚本（Groovy）',
  `dict_mapping_id` bigint(20) NULL DEFAULT NULL COMMENT '字典映射ID',
  `dict_source_type_value` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '字典映射源类型值（用于区分同一字典映射下的不同类型）',
  `dict_target_type_value` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '字典映射目标类型值（用于区分同一字典映射下的不同类型）',
  `dict_output_mode` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT 'TARGET_KEY' COMMENT '字典输出模式：TARGET_KEY-目标编码 SOURCE_LABEL-源名称 TARGET_LABEL-目标名称',
  `transform_function` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '转换函数名',
  `null_strategy` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT 'KEEP' COMMENT '空值处理策略：KEEP-保持空值 DEFAULT-使用默认值 SKIP-跳过该字段 REMOVE-移除该行',
  `default_value` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '默认值',
  `sort_order` int(11) NOT NULL DEFAULT 0 COMMENT '排序序号',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `cleanse_functions` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '清洗函数链配置（JSON格式）格式: [{\"functionCode\": \"TRIM\", \"params\": {}}, {\"functionCode\": \"UPPER\", \"params\": {}}]',
  `processor_chain` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '处理器链配置(JSON格式,支持动态顺序)',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_task_id`(`task_id`) USING BTREE,
  INDEX `idx_dict_mapping`(`dict_mapping_id`) USING BTREE,
  INDEX `idx_target_id`(`target_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2636 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '字段映射表 - 包含数据转换、清洗、校验、脱敏等配置' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for dp_internal_message
-- ----------------------------
DROP TABLE IF EXISTS `dp_internal_message`;
CREATE TABLE `dp_internal_message`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `message_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '消息类型：ALERT-告警 NOTICE-通知 SYSTEM-系统消息',
  `message_level` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'INFO' COMMENT '消息级别：INFO-提示 WARNING-警告 ERROR-错误',
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '消息标题',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '消息内容',
  `related_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '关联类型：TASK-任务 CONNECTOR-连接器',
  `related_id` bigint(20) NULL DEFAULT NULL COMMENT '关联ID',
  `is_read` tinyint(4) NOT NULL DEFAULT 0 COMMENT '是否已读：0-未读 1-已读',
  `read_time` datetime NULL DEFAULT NULL COMMENT '已读时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_message_type`(`message_type`) USING BTREE,
  INDEX `idx_is_read`(`is_read`) USING BTREE,
  INDEX `idx_create_time`(`create_time`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 43 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '内部消息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for dp_task_config
-- ----------------------------
DROP TABLE IF EXISTS `dp_task_config`;
CREATE TABLE `dp_task_config`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `task_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '任务名称',
  `task_code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '任务编码（唯一）',
  `source_connector_id` bigint(20) NOT NULL COMMENT '源连接器ID',
  `target_connector_id` bigint(20) NULL DEFAULT NULL COMMENT '目标连接器ID（单目标模式使用，多目标模式为NULL）',
  `multi_target` tinyint(1) NULL DEFAULT 0 COMMENT '是否多目标模式：0-单目标（兼容旧版）1-多目标',
  `source_config` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '源端配置（JSON格式）：SQL/API配置等',
  `target_config` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '目标端配置（JSON格式，单目标模式使用，多目标模式为NULL）',
  `post_load_config` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '推送后处理配置(JSON格式)：状态回写、数据验证等',
  `sync_mode` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'FULL' COMMENT '同步模式：FULL-全量 INCREMENTAL-增量',
  `incremental_field` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '增量字段名',
  `schedule_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'MANUAL' COMMENT '调度类型：MANUAL-手动 CRON-定时',
  `cron_expression` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'Cron表达式',
  `batch_size` int(11) NULL DEFAULT 1000 COMMENT '批量处理大小',
  `enable_stream` tinyint(4) NULL DEFAULT 0 COMMENT '是否启用流式处理：0-否 1-是',
  `memory_limit_mb` int(11) NULL DEFAULT 512 COMMENT '内存限制(MB)',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '任务描述',
  `status` tinyint(4) NOT NULL DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
  `last_sync_time` datetime NULL DEFAULT NULL COMMENT '最后同步时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(4) NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_task_code`(`task_code`) USING BTREE,
  INDEX `idx_task_name`(`task_name`) USING BTREE,
  INDEX `idx_source_connector`(`source_connector_id`) USING BTREE,
  INDEX `idx_target_connector`(`target_connector_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 36 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '任务配置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for dp_task_execute_log
-- ----------------------------
DROP TABLE IF EXISTS `dp_task_execute_log`;
CREATE TABLE `dp_task_execute_log`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `task_id` bigint(20) NOT NULL COMMENT '任务ID',
  `task_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '任务名称',
  `execute_status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '执行状态：RUNNING-执行中 SUCCESS-成功 FAILED-失败',
  `trigger_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '触发类型：MANUAL-手动 CRON-定时 API-接口',
  `start_time` datetime NOT NULL COMMENT '开始时间',
  `end_time` datetime NULL DEFAULT NULL COMMENT '结束时间',
  `duration` bigint(20) NULL DEFAULT NULL COMMENT '执行时长（毫秒）',
  `total_count` int(11) NULL DEFAULT 0 COMMENT '总记录数',
  `success_count` int(11) NULL DEFAULT 0 COMMENT '成功记录数',
  `failed_count` int(11) NULL DEFAULT 0 COMMENT '失败记录数',
  `error_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '错误信息',
  `execute_log` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '执行日志',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_task_id`(`task_id`) USING BTREE,
  INDEX `idx_execute_status`(`execute_status`) USING BTREE,
  INDEX `idx_start_time`(`start_time`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '任务执行日志表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for dp_task_target
-- ----------------------------
DROP TABLE IF EXISTS `dp_task_target`;
CREATE TABLE `dp_task_target`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `task_id` bigint(20) NOT NULL COMMENT '任务ID',
  `target_connector_id` bigint(20) NOT NULL COMMENT '目标连接器ID',
  `target_config` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '目标端配置（JSON格式）\r\n示例: {\"tableName\":\"user_info\",\"writeMode\":\"INSERT\",\"batchSize\":1000,\"maxRetries\":3,\"idempotentKey\":\"user_id\"}',
  `target_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '目标名称（用于区分）',
  `sort_order` int(11) NULL DEFAULT 0 COMMENT '执行顺序',
  `status` tinyint(1) NULL DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_task_id`(`task_id`) USING BTREE,
  INDEX `idx_status`(`status`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 36 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '任务目标配置表（支持单任务推送到多个目标表）' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for transform_template
-- ----------------------------
DROP TABLE IF EXISTS `transform_template`;
CREATE TABLE `transform_template`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `template_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '模板名称',
  `template_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '模板编码',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '模板描述',
  `mappings_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '转换规则JSON',
  `tags` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '适用场景标签',
  `use_count` int(11) NULL DEFAULT 0 COMMENT '使用次数',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_template_code`(`template_code`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '数据转换模板表' ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;


INSERT INTO `transform_template` (`id`, `template_name`, `template_code`, `description`, `mappings_json`, `tags`, `use_count`, `create_time`, `update_time`, `create_by`) VALUES (1, '用户数据脱敏模板', 'user_desensitize', '适用于用户数据导出时的脱敏处理', '[{\"sourceField\":\"phone\",\"transformType\":\"FUNCTION\",\"transformFunction\":\"DESENSITIZE_PHONE\",\"targetField\":\"phone\"},{\"sourceField\":\"id_card\",\"transformType\":\"FUNCTION\",\"transformFunction\":\"DESENSITIZE_ID_CARD\",\"targetField\":\"id_card\"},{\"sourceField\":\"name\",\"transformType\":\"FUNCTION\",\"transformFunction\":\"DESENSITIZE_NAME\",\"targetField\":\"name\"}]', '用户,脱敏,隐私', 0, '2025-11-21 13:24:09', '2025-11-21 13:24:09', 'system');
INSERT INTO `transform_template` (`id`, `template_name`, `template_code`, `description`, `mappings_json`, `tags`, `use_count`, `create_time`, `update_time`, `create_by`) VALUES (2, '日期格式化模板', 'date_format', '将日期时间字段统一格式化为 yyyy-MM-dd', '[{\"sourceField\":\"create_time\",\"transformType\":\"FUNCTION\",\"transformFunction\":\"DATE_FORMAT(yyyy-MM-dd)\",\"targetField\":\"create_date\"},{\"sourceField\":\"update_time\",\"transformType\":\"FUNCTION\",\"transformFunction\":\"DATE_FORMAT(yyyy-MM-dd)\",\"targetField\":\"update_date\"}]', '日期,格式化', 0, '2025-11-21 13:24:09', '2025-11-21 13:24:09', 'system');
INSERT INTO `transform_template` (`id`, `template_name`, `template_code`, `description`, `mappings_json`, `tags`, `use_count`, `create_time`, `update_time`, `create_by`) VALUES (3, '文本规范化模板', 'text_normalize', '文本字段转小写并去除空格', '[{\"sourceField\":\"email\",\"transformType\":\"FUNCTION\",\"transformFunction\":\"LOWER\",\"targetField\":\"email\"},{\"sourceField\":\"username\",\"transformType\":\"FUNCTION\",\"transformFunction\":\"TRIM\",\"targetField\":\"username\"}]', '文本,规范化,清洗', 1, '2025-11-21 13:24:09', '2025-11-21 13:24:09', 'system');
