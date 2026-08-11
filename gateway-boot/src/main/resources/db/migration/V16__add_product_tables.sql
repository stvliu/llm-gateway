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
-- V16: 添加产品相关表（H2/PostgreSQL 兼容）
-- ============================================

-- 1. 创建产品表
CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    provider_id BIGINT NOT NULL REFERENCES providers(id),
    name VARCHAR(128) NOT NULL,
    product_type VARCHAR(32) NOT NULL DEFAULT 'pay_as_you_go',
    models TEXT DEFAULT '[]',
    endpoints TEXT DEFAULT '{}',
    quota_limit BIGINT,
    state VARCHAR(16) DEFAULT 'active',
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    CONSTRAINT uk_products_provider_name UNIQUE (provider_id, name)
);

-- 2. 创建产品 API Key 表
CREATE TABLE product_api_keys (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    name VARCHAR(128),
    api_key_encrypted TEXT NOT NULL,
    weight INT DEFAULT 1,
    priority INT DEFAULT 1,
    state VARCHAR(16) DEFAULT 'active',
    last_used_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- 3. 创建索引
CREATE INDEX idx_products_provider ON products(provider_id);
CREATE INDEX idx_products_type ON products(product_type);
CREATE INDEX idx_products_state ON products(state);
CREATE INDEX idx_product_api_keys_product ON product_api_keys(product_id);
CREATE INDEX idx_product_api_keys_state ON product_api_keys(state);

-- 4. Model 表新增 product_id 字段
ALTER TABLE models ADD COLUMN product_id BIGINT REFERENCES products(id);
CREATE INDEX idx_models_product ON models(product_id);

-- 5. 自动迁移：为每个 Provider 创建默认产品
INSERT INTO products (provider_id, name, product_type, state)
SELECT id, provider_name || '-default', 'pay_as_you_go', 'active'
FROM providers
WHERE state = 'active';

-- 6. 自动迁移：关联 Model 到默认产品
UPDATE models m
SET product_id = (
    SELECT p.id FROM products p
    WHERE p.provider_id = m.provider_id
      AND p.name LIKE '%-default'
    LIMIT 1
)
WHERE m.product_id IS NULL
  AND EXISTS (
    SELECT 1 FROM products p
    WHERE p.provider_id = m.provider_id
      AND p.name LIKE '%-default'
  );

-- 7. 自动迁移：ProviderApiKey 迁移到 ProductApiKey
INSERT INTO product_api_keys (product_id, name, api_key_encrypted, weight, priority, state, last_used_at, created_at)
SELECT
    p.id,
    pak.key_name,
    pak.encrypted_api_key,
    COALESCE(pak.weight, 1),
    COALESCE(pak.priority, 1),
    CAST(LOWER(pak.state) AS VARCHAR),
    pak.last_used_at,
    pak.created_at
FROM provider_api_keys pak
JOIN products p ON p.provider_id = pak.provider_id
WHERE p.name LIKE '%-default'
  AND pak.state = 'ACTIVE';
