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
-- V45: 移除 UserApiKey 的 channel_ids 字段
-- 渠道权限现在通过 Team ↔ Channel 关系管理
-- API Key 继承用户所属团队的渠道权限，不再直接关联渠道

ALTER TABLE user_api_keys DROP COLUMN IF EXISTS channel_ids;
