-- 添加清洗函数链字段到字段映射表

ALTER TABLE dp_field_mapping 
ADD COLUMN cleanse_functions TEXT COMMENT '清洗函数链配置（JSON格式）格式: [{"functionCode": "TRIM", "params": {}}, {"functionCode": "UPPER", "params": {}}]';

-- 更新表注释
ALTER TABLE dp_field_mapping COMMENT = '字段映射表 - 包含数据转换、清洗、校验、脱敏等配置';
