--
-- Copyright (c) 2025 codingas.com
-- Licensed under the Apache License, Version 2.0.
-- See the LICENSE file for details.
--
-- Phase 3: 扩展 usage_logs 表，新增新架构字段
-- 用于记录 UserApiKey、Team、Product 关联信息

ALTER TABLE usage_logs ADD COLUMN IF NOT EXISTS user_api_key_id BIGINT;
ALTER TABLE usage_logs ADD COLUMN IF NOT EXISTS team_id BIGINT;
ALTER TABLE usage_logs ADD COLUMN IF NOT EXISTS product_id BIGINT;

-- 创建索引以支持按新架构维度查询
CREATE INDEX IF NOT EXISTS idx_usage_logs_user_api_key_id ON usage_logs(user_api_key_id);
CREATE INDEX IF NOT EXISTS idx_usage_logs_team_id ON usage_logs(team_id);
CREATE INDEX IF NOT EXISTS idx_usage_logs_product_id ON usage_logs(product_id);

-- 外键约束（可选，根据数据一致性要求决定）
-- ALTER TABLE usage_logs ADD CONSTRAINT fk_usage_logs_user_api_key
--     FOREIGN KEY (user_api_key_id) REFERENCES user_api_keys(id);
-- ALTER TABLE usage_logs ADD CONSTRAINT fk_usage_logs_team
--     FOREIGN KEY (team_id) REFERENCES teams(id);
-- ALTER TABLE usage_logs ADD CONSTRAINT fk_usage_logs_product
--     FOREIGN KEY (product_id) REFERENCES products(id);
