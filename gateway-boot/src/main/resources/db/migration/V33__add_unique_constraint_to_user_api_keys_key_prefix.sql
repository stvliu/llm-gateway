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
-- V33: 为 user_api_keys.key_prefix 添加唯一约束
-- 确保 findByKeyPrefix 认证查询的 prefix 唯一性

CREATE UNIQUE INDEX idx_user_api_keys_key_prefix ON user_api_keys(key_prefix);
