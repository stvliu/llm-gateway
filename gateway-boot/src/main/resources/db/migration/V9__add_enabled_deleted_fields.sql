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
