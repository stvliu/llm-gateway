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
-- 移除 usage_logs 表对 gateway_api_keys 的外键依赖
-- 改用 user_api_key_id 字段（新架构）

-- 1. 删除旧的外键约束
ALTER TABLE usage_logs DROP CONSTRAINT IF EXISTS fk_usage_logs_api_key;

-- 2. 删除旧索引
DROP INDEX IF EXISTS idx_usage_key_created;

-- 3. 确保 user_api_key_id 列存在且非空
-- 注意：如果已有数据，需要先迁移数据
-- UPDATE usage_logs SET user_api_key_id = gateway_api_key_id WHERE user_api_key_id IS NULL;

-- 4. 创建新索引
CREATE INDEX IF NOT EXISTS idx_usage_key_created ON usage_logs(user_api_key_id, created_at);

-- 注意：此迁移不删除 gateway_api_key_id 列，保留用于降级兼容
-- 后续迁移脚本可删除该列
