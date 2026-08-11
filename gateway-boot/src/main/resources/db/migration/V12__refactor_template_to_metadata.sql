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
-- V12__refactor_template_to_metadata.sql
-- 供应商模板重构为元数据体系：provider_metadata + model_metadata

-- ============================================
-- 1. 创建 provider_metadata 表
-- ============================================
CREATE TABLE provider_metadata (
    id BIGSERIAL PRIMARY KEY,
    provider_id VARCHAR(64) NOT NULL,
    provider_name VARCHAR(128) NOT NULL,
    provider_type VARCHAR(32) NOT NULL,
    provider_config JSON NOT NULL,
    metadata_type VARCHAR(32) NOT NULL DEFAULT 'OFFICIAL',
    icon_url VARCHAR(512),
    description TEXT,
    tags JSON,
    market_state VARCHAR(32) NOT NULL DEFAULT 'PRIVATE',
    publish_at TIMESTAMP WITH TIME ZONE,
    download_count INT NOT NULL DEFAULT 0,
    author_id BIGINT,
    author_name VARCHAR(64),
    state VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    deleted_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_at TIMESTAMP WITH TIME ZONE,
    updated_by BIGINT
);

-- 唯一索引：provider_id 全局唯一
CREATE UNIQUE INDEX uk_provider_metadata_provider_id ON provider_metadata(provider_id);

-- 查询索引
CREATE INDEX idx_provider_metadata_type ON provider_metadata(provider_type, state);
CREATE INDEX idx_provider_metadata_market ON provider_metadata(market_state);
CREATE INDEX idx_provider_metadata_author ON provider_metadata(author_id);

-- ============================================
-- 2. 创建 model_metadata 表
-- ============================================
CREATE TABLE model_metadata (
    id BIGSERIAL PRIMARY KEY,
    provider_id VARCHAR(64) NOT NULL,
    provider_model_id VARCHAR(128) NOT NULL,
    display_name VARCHAR(128) NOT NULL,
    model_family VARCHAR(64),
    context_window INTEGER,
    max_input_tokens INTEGER,
    max_output_tokens INTEGER,
    input_price DECIMAL(12,6),
    output_price DECIMAL(12,6),
    reasoning_price DECIMAL(12,6),
    cache_read_price DECIMAL(12,6),
    cache_write_price DECIMAL(12,6),
    input_audio_price DECIMAL(12,6),
    output_audio_price DECIMAL(12,6),
    knowledge_cutoff VARCHAR(32),
    release_date DATE,
    open_weights BOOLEAN,
    modalities JSON,
    capabilities JSON,
    source VARCHAR(32) NOT NULL DEFAULT 'BUILTIN',
    source_synced_at TIMESTAMP WITH TIME ZONE,
    state VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_at TIMESTAMP WITH TIME ZONE,
    updated_by BIGINT
);

-- 唯一索引：(provider_id, provider_model_id) 联合唯一
CREATE UNIQUE INDEX uk_model_metadata_provider_model ON model_metadata(provider_id, provider_model_id);

-- 查询索引
CREATE INDEX idx_model_metadata_provider ON model_metadata(provider_id);
CREATE INDEX idx_model_metadata_source ON model_metadata(source);
CREATE INDEX idx_model_metadata_state ON model_metadata(state);

-- ============================================
-- 3. 数据迁移说明
-- ============================================
-- provider_templates 中的数据不再通过 SQL 迁移到新表。
-- 内置元数据由 BuiltinMetadataSyncRunner 在应用启动时从 JSON 文件自动同步。
-- 用户自创建的模板数据在重构中不再保留（原模板功能已移除）。

-- ============================================
-- 4. 清理：删除不再使用的 provider_templates 表及相关对象
-- ============================================
DROP TABLE IF EXISTS provider_templates;
