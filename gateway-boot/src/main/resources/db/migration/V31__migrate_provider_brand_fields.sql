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
-- 为现有 providers 补充品牌信息
-- 从 provider_metadata 表复制 icon_url、description 等字段

-- 更新 providers 表，根据名称匹配 provider_metadata
UPDATE providers p
SET
  provider_id = (SELECT pm.provider_id FROM provider_metadata pm WHERE LOWER(p.provider_name) = LOWER(pm.provider_name) LIMIT 1),
  icon_url = (SELECT pm.icon_url FROM provider_metadata pm WHERE LOWER(p.provider_name) = LOWER(pm.provider_name) LIMIT 1),
  description = (SELECT pm.description FROM provider_metadata pm WHERE LOWER(p.provider_name) = LOWER(pm.provider_name) LIMIT 1),
  tags = (SELECT pm.tags FROM provider_metadata pm WHERE LOWER(p.provider_name) = LOWER(pm.provider_name) LIMIT 1)
WHERE p.provider_id IS NULL
  AND EXISTS (SELECT 1 FROM provider_metadata pm WHERE LOWER(p.provider_name) = LOWER(pm.provider_name));

-- 更新 providers 表，根据 provider_id 匹配（如果 provider_name 就是 provider_id）
UPDATE providers p
SET
  icon_url = (SELECT pm.icon_url FROM provider_metadata pm WHERE LOWER(p.provider_name) = LOWER(pm.provider_id) LIMIT 1),
  description = (SELECT pm.description FROM provider_metadata pm WHERE LOWER(p.provider_name) = LOWER(pm.provider_id) LIMIT 1),
  tags = (SELECT pm.tags FROM provider_metadata pm WHERE LOWER(p.provider_name) = LOWER(pm.provider_id) LIMIT 1)
WHERE p.icon_url IS NULL
  AND EXISTS (SELECT 1 FROM provider_metadata pm WHERE LOWER(p.provider_name) = LOWER(pm.provider_id));

COMMENT ON COLUMN providers.provider_id IS '品牌标识（关联 ProviderMetadata.providerId）';
COMMENT ON COLUMN providers.icon_url IS '品牌图标 URL';
COMMENT ON COLUMN providers.description IS '描述信息';
COMMENT ON COLUMN providers.tags IS '标签（JSON 格式）';