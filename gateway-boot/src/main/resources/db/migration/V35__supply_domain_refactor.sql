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
-- ==========================================
-- V35: 供给域重构 — 表/列重命名
-- ==========================================

-- 1. products → channels
ALTER TABLE products RENAME TO channels;

-- 2. product_api_keys → channel_credentials
ALTER TABLE product_api_keys RENAME TO channel_credentials;

-- 3. product_models → channel_models
ALTER TABLE product_models RENAME TO channel_models;

-- 4. models → model_specs
ALTER TABLE models RENAME TO model_specs;

-- 5. 渠道表列重命名
ALTER TABLE channels RENAME COLUMN product_type TO billing_mode;

-- 6. 渠道凭证表列重命名
ALTER TABLE channel_credentials RENAME COLUMN product_id TO channel_id;
ALTER TABLE channel_credentials RENAME COLUMN api_key_encrypted TO encrypted_api_key;

-- 7. 渠道模型表列重命名
ALTER TABLE channel_models RENAME COLUMN product_id TO channel_id;
ALTER TABLE channel_models RENAME COLUMN model_id TO model_spec_id;

-- 8. 用户 API Key 关联表重命名 + 列重命名
-- 先删除外键约束（H2 将小写约束名转为大写存储）
ALTER TABLE user_api_key_products DROP CONSTRAINT IF EXISTS FK_UAKP_PRODUCT;
ALTER TABLE user_api_key_products DROP CONSTRAINT IF EXISTS FK_UAKP_USER_API_KEY;
ALTER TABLE user_api_key_products DROP CONSTRAINT IF EXISTS fk_uakp_product;
ALTER TABLE user_api_key_products DROP CONSTRAINT IF EXISTS fk_uakp_user_api_key;
ALTER TABLE user_api_key_products DROP CONSTRAINT IF EXISTS FK_UAKP_KEY;
ALTER TABLE user_api_key_products DROP CONSTRAINT IF EXISTS fk_uakp_key;
-- 重命名表和列
ALTER TABLE user_api_key_products RENAME TO user_api_key_channels;
ALTER TABLE user_api_key_channels RENAME COLUMN product_id TO channel_id;
-- 重建外键约束
ALTER TABLE user_api_key_channels ADD CONSTRAINT fk_uakc_channel FOREIGN KEY (channel_id) REFERENCES channels(id) ON DELETE CASCADE;
ALTER TABLE user_api_key_channels ADD CONSTRAINT fk_uakc_user_api_key FOREIGN KEY (user_api_key_id) REFERENCES user_api_keys(id) ON DELETE CASCADE;