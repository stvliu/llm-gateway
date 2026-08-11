--
-- Copyright © 2025-2026 codingas.com
--
-- Licensed under the Apache License, Version 2.0 (the "License");
-- you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at
--
--     http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.
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
