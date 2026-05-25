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
ALTER TABLE channels RENAME COLUMN type TO billing_mode;
ALTER TABLE channels RENAME COLUMN enabled TO state;
ALTER TABLE channels RENAME COLUMN api_key TO credential_preview;

-- 6. 渠道凭证表列重命名
ALTER TABLE channel_credentials RENAME COLUMN product_id TO channel_id;
ALTER TABLE channel_credentials RENAME COLUMN api_key TO encrypted_api_key;
ALTER TABLE channel_credentials RENAME COLUMN is_default TO is_default;
ALTER TABLE channel_credentials RENAME COLUMN enabled TO state;

-- 7. 渠道模型表列重命名
ALTER TABLE channel_models RENAME COLUMN product_id TO channel_id;
ALTER TABLE channel_models RENAME COLUMN model_id TO model_spec_id;

-- 8. 用户 API Key 关联列重命名
ALTER TABLE user_api_keys RENAME COLUMN product_ids TO channel_ids;
