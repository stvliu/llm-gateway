-- V9__add_enabled_deleted_fields.sql
-- 添加 models 和 providers 表的 enabled 和 deleted 字段

-- ============================================
-- providers 表
-- ============================================

-- 添加 timeout 列
ALTER TABLE providers ADD COLUMN IF NOT EXISTS timeout INT DEFAULT 30000;

-- 添加 max_retries 列
ALTER TABLE providers ADD COLUMN IF NOT EXISTS max_retries INT DEFAULT 3;

-- 添加 enabled 列（默认 true）
ALTER TABLE providers ADD COLUMN IF NOT EXISTS enabled BOOLEAN NOT NULL DEFAULT TRUE;

-- 添加 deleted 列（默认 false）
ALTER TABLE providers ADD COLUMN IF NOT EXISTS deleted BOOLEAN NOT NULL DEFAULT FALSE;

-- 添加索引以支持按 enabled/deleted 筛选
CREATE INDEX IF NOT EXISTS idx_providers_enabled ON providers(enabled);
CREATE INDEX IF NOT EXISTS idx_providers_deleted ON providers(deleted);

-- ============================================
-- models 表
-- ============================================

-- 添加 enabled 列（默认 true）
ALTER TABLE models ADD COLUMN IF NOT EXISTS enabled BOOLEAN NOT NULL DEFAULT TRUE;

-- 添加 deleted 列（默认 false）
ALTER TABLE models ADD COLUMN IF NOT EXISTS deleted BOOLEAN NOT NULL DEFAULT FALSE;

-- 添加索引以支持按 enabled/deleted 筛选
CREATE INDEX IF NOT EXISTS idx_models_enabled ON models(enabled);
CREATE INDEX IF NOT EXISTS idx_models_deleted ON models(deleted);
