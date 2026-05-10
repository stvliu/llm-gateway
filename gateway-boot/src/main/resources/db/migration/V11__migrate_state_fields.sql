-- V11__migrate_provider_model_state.sql
-- 将 providers 和 models 表的 enabled 字段迁移为 state 字段

-- ============================================
-- providers 表
-- ============================================

-- 添加 state 列
ALTER TABLE providers ADD COLUMN IF NOT EXISTS state VARCHAR(32);

-- 迁移数据：enabled=true → ACTIVE, enabled=false → SUSPENDED
UPDATE providers SET state = CASE WHEN enabled THEN 'ACTIVE' ELSE 'SUSPENDED' END WHERE state IS NULL;

-- 设置 NOT NULL 约束
ALTER TABLE providers ALTER COLUMN state SET NOT NULL;

-- 设置默认值
ALTER TABLE providers ALTER COLUMN state SET DEFAULT 'ACTIVE';

-- 删除 enabled 列
ALTER TABLE providers DROP COLUMN IF EXISTS enabled;

-- 删除旧索引
DROP INDEX IF EXISTS idx_providers_enabled;

-- 创建新索引
CREATE INDEX IF NOT EXISTS idx_providers_state ON providers(state);

-- ============================================
-- models 表
-- ============================================

-- 添加 state 列
ALTER TABLE models ADD COLUMN IF NOT EXISTS state VARCHAR(32);

-- 迁移数据：enabled=true → ACTIVE, enabled=false → DISABLED
UPDATE models SET state = CASE WHEN enabled THEN 'ACTIVE' ELSE 'DISABLED' END WHERE state IS NULL;

-- 设置 NOT NULL 约束
ALTER TABLE models ALTER COLUMN state SET NOT NULL;

-- 设置默认值
ALTER TABLE models ALTER COLUMN state SET DEFAULT 'ACTIVE';

-- 删除 enabled 列
ALTER TABLE models DROP COLUMN IF EXISTS enabled;

-- 删除旧索引
DROP INDEX IF EXISTS idx_models_enabled;

-- 创建新索引
CREATE INDEX IF NOT EXISTS idx_models_state ON models(state);

-- ============================================
-- provider_api_keys 表
-- ============================================

-- 重命名 status 列为 state
ALTER TABLE provider_api_keys RENAME COLUMN status TO state;

-- 删除旧索引
DROP INDEX IF EXISTS idx_status;

-- 创建新索引
CREATE INDEX IF NOT EXISTS idx_provider_api_keys_state ON provider_api_keys(state);

-- ============================================
-- gateway_api_keys 表
-- ============================================

-- 重命名 status 列为 state
ALTER TABLE gateway_api_keys RENAME COLUMN status TO state;

-- ============================================
-- provider_templates 表
-- ============================================

-- 重命名 status 列为 state
ALTER TABLE provider_templates RENAME COLUMN status TO state;
