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
-- V48: user_api_keys 表 state 列替换为 deleted 逻辑删除标记
-- UserApiKey 只有"存在"和"已删除"两种状态，无需 state 枚举

-- 1. 添加 deleted 列
ALTER TABLE user_api_keys ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT FALSE;

-- 2. 迁移数据：REVOKED 状态的 Key 标记为已删除
UPDATE user_api_keys SET deleted = TRUE WHERE state = 'REVOKED';

-- 3. 删除 state 列及其索引
DROP INDEX IF EXISTS idx_user_api_keys_state;
ALTER TABLE user_api_keys DROP COLUMN state;
