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
-- V69: models 表新增模型目录同步字段 + catalog_sync_logs 表
-- ============================================
-- 模型目录同步（models.dev）：models 表新增 10 个同步元数据列
-- （description/release_date/last_updated/license/open_weights/benchmarks/weights/
--  source/external_id/locked_fields），并新建 catalog_sync_logs 表记录每次同步结果。
--
-- external_id 为同步幂等匹配键（如 openai/gpt-4o），建索引加速按外部 ID 查询。
-- source 默认 'MANUAL'（人工创建），同步来源为 MODELS_DEV / BUILTIN。
-- benchmarks/weights/locked_fields 为 JSON 列（jsonb，与 capabilities/modalities 一致）。
--
-- 方言：H2（PostgreSQL 兼容模式）/ PostgreSQL 均支持
--       ADD COLUMN IF NOT EXISTS / CREATE TABLE IF NOT EXISTS / CREATE INDEX IF NOT EXISTS。
--       id 风格沿用 V1 models 表的 BIGSERIAL PRIMARY KEY。
-- ============================================

-- models 表新增模型目录同步字段
ALTER TABLE models ADD COLUMN IF NOT EXISTS description TEXT;
ALTER TABLE models ADD COLUMN IF NOT EXISTS release_date DATE;
ALTER TABLE models ADD COLUMN IF NOT EXISTS last_updated DATE;
ALTER TABLE models ADD COLUMN IF NOT EXISTS license VARCHAR(128);
ALTER TABLE models ADD COLUMN IF NOT EXISTS open_weights BOOLEAN;
ALTER TABLE models ADD COLUMN IF NOT EXISTS benchmarks JSONB;
ALTER TABLE models ADD COLUMN IF NOT EXISTS weights JSONB;
ALTER TABLE models ADD COLUMN IF NOT EXISTS source VARCHAR(32) DEFAULT 'MANUAL';
ALTER TABLE models ADD COLUMN IF NOT EXISTS external_id VARCHAR(256);
ALTER TABLE models ADD COLUMN IF NOT EXISTS locked_fields JSONB;
CREATE INDEX IF NOT EXISTS idx_models_external_id ON models(external_id);

-- 模型目录同步日志表
CREATE TABLE IF NOT EXISTS catalog_sync_logs (
    id BIGSERIAL PRIMARY KEY,
    triggered_by VARCHAR(64),
    result VARCHAR(16) NOT NULL,
    added_count INTEGER DEFAULT 0,
    updated_count INTEGER DEFAULT 0,
    skipped_count INTEGER DEFAULT 0,
    failed_count INTEGER DEFAULT 0,
    message TEXT,
    synced_at TIMESTAMP,
    created_by BIGINT,
    created_at TIMESTAMP,
    updated_by BIGINT,
    updated_at TIMESTAMP
);
