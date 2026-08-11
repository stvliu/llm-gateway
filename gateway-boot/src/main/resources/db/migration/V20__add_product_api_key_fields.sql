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
-- ============================================
-- V20: 产品 API Key 表新增 base_url 和 description 字段
-- ============================================

ALTER TABLE product_api_keys ADD COLUMN IF NOT EXISTS base_url VARCHAR(512);
ALTER TABLE product_api_keys ADD COLUMN IF NOT EXISTS description VARCHAR(512);

COMMENT ON COLUMN product_api_keys.base_url IS '请求基础 URL';
COMMENT ON COLUMN product_api_keys.description IS '描述';