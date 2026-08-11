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
-- V24: 迁移 providers.base_url 到 products.endpoints，然后移除 base_url 列
-- Provider 的端点信息已迁移到 Product.endpoints，baseUrl 不再需要

-- 1. 数据迁移：将 providers.base_url 写入对应默认产品的 endpoints
-- 使用 provider_type 作为协议名称的 key
UPDATE products p
SET endpoints = (
    SELECT CASE
        WHEN prv.provider_type = 'ANTHROPIC' THEN
            '{"anthropic":"' || prv.base_url || '"}'
        ELSE
            '{"openai":"' || prv.base_url || '"}'
    END
    FROM providers prv
    WHERE prv.id = p.provider_id
      AND prv.base_url IS NOT NULL
      AND prv.base_url != ''
)
WHERE EXISTS (
    SELECT 1 FROM providers prv
    WHERE prv.id = p.provider_id
      AND prv.base_url IS NOT NULL
      AND prv.base_url != ''
)
AND p.name LIKE '%-default';

-- 2. 移除 providers.base_url 列
ALTER TABLE providers DROP COLUMN IF EXISTS base_url;
