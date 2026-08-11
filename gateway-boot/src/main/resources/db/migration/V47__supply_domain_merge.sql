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
-- V47: 供应域合并 — Catalog 表合并到运营表
-- ==========================================
--
-- 背景：供应域合并后，Catalog 表作为配置数据源，
-- 数据需迁移到运营表（providers、models），然后删除 Catalog 表。
--
-- Phase 1: provider_catalogs → providers
-- Phase 2: model_catalogs → models
-- Phase 3: plan_catalogs/plan_model_catalogs 精简
-- Phase 4: 删除 catalog 相关表
-- ==========================================


-- === Phase 1: Provider 合并 ===

-- 1.1 从 provider_catalogs 迁移不存在的 Provider
-- providers 表使用 provider_id 列存储 code，provider_name 存储 name
INSERT INTO providers (provider_id, provider_name, icon_url, website_url, description, state)
SELECT provider_code, provider_name, logo_url, website_url, description, state
FROM provider_catalogs pc
WHERE NOT EXISTS (
    SELECT 1 FROM providers p WHERE p.provider_id = pc.provider_code
)
AND pc.provider_code IS NOT NULL;

-- 1.2 更新已存在的 Provider 的品牌信息（如果 Catalog 有更完整的数据）
UPDATE providers p
SET
    icon_url = COALESCE(pc.logo_url, p.icon_url),
    website_url = COALESCE(pc.website_url, p.website_url),
    description = COALESCE(pc.description, p.description)
FROM provider_catalogs pc
WHERE p.provider_id = pc.provider_code
AND (p.icon_url IS NULL OR p.website_url IS NULL OR p.description IS NULL);


-- === Phase 2: Model 合并 ===

-- 2.1 确保 models 表有 models 表缺少的列
ALTER TABLE models ADD COLUMN IF NOT EXISTS knowledge_cutoff VARCHAR(32);
ALTER TABLE models ADD COLUMN IF NOT EXISTS model_family VARCHAR(64);
ALTER TABLE models ADD COLUMN IF NOT EXISTS max_input_tokens INTEGER;
ALTER TABLE models ADD COLUMN IF NOT EXISTS max_output_tokens INTEGER;
ALTER TABLE models ADD COLUMN IF NOT EXISTS modalities TEXT;

-- 2.2 从 model_catalogs 迁移不存在的 Model
-- model_catalogs 表的 model_name 对应 models.model_name
INSERT INTO models (model_name, display_name, model_family, context_window,
                    max_input_tokens, max_output_tokens, knowledge_cutoff,
                    capabilities, modalities, state)
SELECT mc.model_name, mc.display_name, mc.model_family, mc.context_window,
       mc.max_input_tokens, mc.max_output_tokens, mc.knowledge_cutoff,
       mc.capabilities, mc.modalities, mc.state
FROM model_catalogs mc
WHERE NOT EXISTS (
    SELECT 1 FROM models m WHERE m.model_name = mc.model_name
)
AND mc.model_name IS NOT NULL;

-- 2.3 更新已存在 Model 的规格信息（如果 Catalog 有更完整的数据）
UPDATE models m
SET
    display_name = COALESCE(mc.display_name, m.display_name),
    model_family = COALESCE(mc.model_family, m.model_family),
    context_window = COALESCE(mc.context_window, m.context_window),
    max_input_tokens = COALESCE(mc.max_input_tokens, m.max_input_tokens),
    max_output_tokens = COALESCE(mc.max_output_tokens, m.max_output_tokens),
    knowledge_cutoff = COALESCE(mc.knowledge_cutoff, m.knowledge_cutoff)
FROM model_catalogs mc
WHERE m.model_name = mc.model_name
AND (
    m.display_name IS NULL OR
    m.model_family IS NULL OR
    m.context_window IS NULL OR
    m.knowledge_cutoff IS NULL
);


-- === Phase 3: ChannelModels → ModelInstances ===

-- 3.1 重命名表（H2 兼容）
-- V35 已将 product_models 改名为 channel_models，此处进一步改为 model_instances
ALTER TABLE channel_models RENAME TO model_instances;

-- 删除旧的定价字段
ALTER TABLE model_instances DROP COLUMN IF EXISTS input_price;
ALTER TABLE model_instances DROP COLUMN IF EXISTS output_price;
ALTER TABLE model_instances DROP COLUMN IF EXISTS reasoning_price;
ALTER TABLE model_instances DROP COLUMN IF EXISTS cache_read_price;
ALTER TABLE model_instances DROP COLUMN IF EXISTS cache_write_price;
ALTER TABLE model_instances DROP COLUMN IF EXISTS input_audio_price;
ALTER TABLE model_instances DROP COLUMN IF EXISTS output_audio_price;

-- 新增实例级覆盖字段（使用 JSON 而非 JSONB 以兼容 H2）
ALTER TABLE model_instances ADD COLUMN IF NOT EXISTS capabilities_override JSON;
ALTER TABLE model_instances ADD COLUMN IF NOT EXISTS context_window_override INTEGER;

-- 新增 priority/weight（从 Channel 下沉）
ALTER TABLE model_instances ADD COLUMN IF NOT EXISTS priority INTEGER DEFAULT 100;
ALTER TABLE model_instances ADD COLUMN IF NOT EXISTS weight INTEGER DEFAULT 100;


-- === Phase 4: Catalog 表精简 ===

-- 4.1 删除 plan_catalogs 的 source/synced_at 字段
ALTER TABLE plan_catalogs DROP COLUMN IF EXISTS source;
ALTER TABLE plan_catalogs DROP COLUMN IF EXISTS synced_at;

-- 4.2 删除 plan_model_catalogs 的 source/synced_at 字段
ALTER TABLE plan_model_catalogs DROP COLUMN IF EXISTS source;
ALTER TABLE plan_model_catalogs DROP COLUMN IF EXISTS synced_at;


-- === Phase 5: 删除 Catalog 表 ===

-- 按依赖顺序删除（先删除子表）
DROP TABLE IF EXISTS plan_model_catalogs;
DROP TABLE IF EXISTS plan_catalogs;
DROP TABLE IF EXISTS model_catalogs;
DROP TABLE IF EXISTS provider_catalogs;


-- === Phase 6: 清理索引 ===

-- 删除 Catalog 表的索引（如果表已删除，索引也会自动删除）
-- 但显式删除以防遗留
DROP INDEX IF EXISTS idx_provider_catalogs_type;
DROP INDEX IF EXISTS idx_provider_catalogs_source;
DROP INDEX IF EXISTS idx_plan_catalogs_provider;
DROP INDEX IF EXISTS idx_plan_catalogs_billing;
DROP INDEX IF EXISTS idx_plan_catalogs_source;
DROP INDEX IF EXISTS idx_plan_model_catalogs_plan;
DROP INDEX IF EXISTS idx_plan_model_catalogs_model;
DROP INDEX IF EXISTS idx_model_catalogs_family;
DROP INDEX IF EXISTS idx_model_catalogs_source;


-- === Phase 7: 添加运营表的缺失索引 ===

CREATE INDEX IF NOT EXISTS idx_providers_provider_id ON providers(provider_id);
CREATE INDEX IF NOT EXISTS idx_models_model_name ON models(model_name);
CREATE INDEX IF NOT EXISTS idx_model_instances_channel ON model_instances(channel_id);
CREATE INDEX IF NOT EXISTS idx_model_instances_model ON model_instances(model_id);