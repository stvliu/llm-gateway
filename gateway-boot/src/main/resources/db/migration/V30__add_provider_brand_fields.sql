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
-- 为 providers 表添加品牌信息字段
-- 这些字段从 ProviderMetadata 复制而来，支持完全复制模式

ALTER TABLE providers ADD COLUMN IF NOT EXISTS provider_id VARCHAR(64);
ALTER TABLE providers ADD COLUMN IF NOT EXISTS icon_url VARCHAR(512);
ALTER TABLE providers ADD COLUMN IF NOT EXISTS description VARCHAR(512);
ALTER TABLE providers ADD COLUMN IF NOT EXISTS tags TEXT;

-- 创建索引加速按 provider_id 查询
CREATE INDEX IF NOT EXISTS idx_providers_provider_id ON providers(provider_id);

COMMENT ON COLUMN providers.provider_id IS '品牌标识（关联 ProviderMetadata.providerId）';
COMMENT ON COLUMN providers.icon_url IS '品牌图标 URL';
COMMENT ON COLUMN providers.description IS '描述信息';
COMMENT ON COLUMN providers.tags IS '标签（JSON 格式）';
