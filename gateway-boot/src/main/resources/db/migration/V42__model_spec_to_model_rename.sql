--
-- Copyright (c) 2025 codingas.com
-- Licensed under the Apache License, Version 2.0.
-- See the LICENSE file for details.
--
-- ==========================================
-- V42: ModelSpec → Model 重命名（表/列重命名）
-- ==========================================

-- 1. model_specs → models
ALTER TABLE model_specs RENAME TO models;

-- 2. provider_model_id → model_name
ALTER TABLE models RENAME COLUMN provider_model_id TO model_name;

-- 3. 删除 models 表中不再使用的路由字段
ALTER TABLE models DROP COLUMN IF EXISTS priority;
ALTER TABLE models DROP COLUMN IF EXISTS weight;

-- 4. 重命名唯一约束
ALTER TABLE models RENAME CONSTRAINT uq_model_specs_provider_model_id TO uq_models_model_name;

-- 5. 重命名索引
DROP INDEX IF EXISTS idx_models_provider_model_id;
-- 索引名根据实际需要添加
CREATE INDEX IF NOT EXISTS idx_models_model_name ON models(model_name);

-- 6. channel_models.model_spec_id → model_id
-- 先删除外键约束（如果存在）
ALTER TABLE channel_models DROP CONSTRAINT IF EXISTS fk_channel_models_model;
ALTER TABLE channel_models DROP CONSTRAINT IF EXISTS fk_channel_models_model_spec;
-- 重命名列
ALTER TABLE channel_models RENAME COLUMN model_spec_id TO model_id;
-- 添加外键约束
ALTER TABLE channel_models ADD CONSTRAINT fk_channel_models_model FOREIGN KEY (model_id) REFERENCES models(id) ON DELETE CASCADE;

-- 7. model_spec_catalogs → model_catalogs
ALTER TABLE model_spec_catalogs RENAME TO model_catalogs;

-- 8. model_catalogs.provider_model_id → model_name
ALTER TABLE model_catalogs RENAME COLUMN provider_model_id TO model_name;

-- 9. 重命名唯一约束
ALTER TABLE model_catalogs RENAME CONSTRAINT uk_model_spec_catalogs_provider_model_id TO uk_model_catalogs_model_name;

-- 10. 重命名索引
DROP INDEX IF EXISTS idx_model_spec_catalogs_family;
DROP INDEX IF EXISTS idx_model_spec_catalogs_source;
CREATE INDEX IF NOT EXISTS idx_model_catalogs_family ON model_catalogs(model_family);
CREATE INDEX IF NOT EXISTS idx_model_catalogs_source ON model_catalogs(source);

-- 11. plan_model_catalogs.provider_model_id → model_name
ALTER TABLE plan_model_catalogs RENAME COLUMN provider_model_id TO model_name;

-- 12. 重命名唯一约束
ALTER TABLE plan_model_catalogs RENAME CONSTRAINT uk_plan_model_catalogs TO uk_plan_model_catalogs_unique;

-- 13. 重命名索引
DROP INDEX IF EXISTS idx_plan_model_catalogs_model;
CREATE INDEX IF NOT EXISTS idx_plan_model_catalogs_model ON plan_model_catalogs(model_name);