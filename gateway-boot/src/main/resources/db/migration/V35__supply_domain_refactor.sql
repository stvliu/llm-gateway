-- 供给域重构：表重命名 + 字段重命名
-- 兼容 PostgreSQL 和 H2 数据库

-- ============================================================
-- 1. 重命名表
-- ============================================================

ALTER TABLE products RENAME TO channels;
ALTER TABLE product_api_keys RENAME TO channel_credentials;
ALTER TABLE product_model_associations RENAME TO channel_models;
ALTER TABLE models RENAME TO model_specs;

-- ============================================================
-- 2. 重命名 channels 表字段
-- ============================================================

ALTER TABLE channels RENAME COLUMN product_type TO billing_mode;
ALTER TABLE channels RENAME COLUMN product_state TO state;

-- ============================================================
-- 3. 重命名 channel_credentials 表字段
-- ============================================================

ALTER TABLE channel_credentials RENAME COLUMN product_id TO channel_id;
ALTER TABLE channel_credentials RENAME COLUMN api_key_state TO state;

-- ============================================================
-- 4. 重命名 channel_models 表字段
-- ============================================================

ALTER TABLE channel_models RENAME COLUMN product_id TO channel_id;
ALTER TABLE channel_models RENAME COLUMN model_id TO model_spec_id;
ALTER TABLE channel_models RENAME COLUMN association_state TO state;

-- ============================================================
-- 5. 重命名 model_specs 表字段
-- ============================================================

ALTER TABLE model_specs RENAME COLUMN model_state TO state;

-- ============================================================
-- 6. 更新枚举值
-- ============================================================

UPDATE channels SET billing_mode = 'SUBSCRIPTION_CODING' WHERE billing_mode IN ('PROMOTION', 'SUBSCRIPTION');
