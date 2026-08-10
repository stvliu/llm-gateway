--
-- Copyright (c) 2025 codingas.com
-- Licensed under the Apache License, Version 2.0.
-- See the LICENSE file for details.
--
-- ==========================================
-- V38: Catalog 表替代 Metadata 表
-- 1. 创建 provider_catalogs 表
-- 2. 创建 plan_catalogs 表
-- 3. 创建 plan_model_catalogs 表
-- 4. 创建 model_spec_catalogs 表
-- 5. 删除旧 metadata 表（按依赖顺序）
-- ==========================================

-- === 1. 供应商目录 ===
CREATE TABLE IF NOT EXISTS provider_catalogs (
    id BIGSERIAL PRIMARY KEY,
    provider_code VARCHAR(64) NOT NULL,
    provider_name VARCHAR(128) NOT NULL,
    provider_type VARCHAR(32) NOT NULL,
    logo_url VARCHAR(512),
    website_url VARCHAR(512),
    base_url VARCHAR(512),
    description TEXT,
    source VARCHAR(32) NOT NULL DEFAULT 'BUILTIN',
    synced_at TIMESTAMP WITH TIME ZONE,
    state VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    created_by BIGINT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_by BIGINT,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_provider_catalogs_provider_code UNIQUE (provider_code)
);

CREATE INDEX idx_provider_catalogs_type ON provider_catalogs(provider_type, state);
CREATE INDEX idx_provider_catalogs_source ON provider_catalogs(source);

-- === 2. 套餐目录 ===
CREATE TABLE IF NOT EXISTS plan_catalogs (
    id BIGSERIAL PRIMARY KEY,
    plan_code VARCHAR(64) NOT NULL,
    provider_code VARCHAR(64) NOT NULL,
    plan_name VARCHAR(128) NOT NULL,
    billing_mode VARCHAR(32) NOT NULL,
    endpoints TEXT,
    pricing TEXT,
    description TEXT,
    source VARCHAR(32) NOT NULL DEFAULT 'BUILTIN',
    synced_at TIMESTAMP WITH TIME ZONE,
    state VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    created_by BIGINT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_by BIGINT,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_plan_catalogs_plan_code UNIQUE (plan_code)
);

CREATE INDEX idx_plan_catalogs_provider ON plan_catalogs(provider_code);
CREATE INDEX idx_plan_catalogs_billing ON plan_catalogs(billing_mode);
CREATE INDEX idx_plan_catalogs_source ON plan_catalogs(source);

-- === 3. 套餐-模型关联目录 ===
CREATE TABLE IF NOT EXISTS plan_model_catalogs (
    id BIGSERIAL PRIMARY KEY,
    plan_code VARCHAR(64) NOT NULL,
    provider_model_id VARCHAR(128) NOT NULL,
    source VARCHAR(32) NOT NULL DEFAULT 'BUILTIN',
    synced_at TIMESTAMP WITH TIME ZONE,
    state VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    created_by BIGINT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_by BIGINT,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_plan_model_catalogs UNIQUE (plan_code, provider_model_id)
);

CREATE INDEX idx_plan_model_catalogs_plan ON plan_model_catalogs(plan_code);
CREATE INDEX idx_plan_model_catalogs_model ON plan_model_catalogs(provider_model_id);

-- === 4. 模型规格目录 ===
CREATE TABLE IF NOT EXISTS model_spec_catalogs (
    id BIGSERIAL PRIMARY KEY,
    provider_model_id VARCHAR(128) NOT NULL,
    display_name VARCHAR(128),
    model_family VARCHAR(64),
    context_window INTEGER,
    max_input_tokens INTEGER,
    max_output_tokens INTEGER,
    knowledge_cutoff VARCHAR(32),
    capabilities TEXT,
    modalities TEXT,
    source VARCHAR(32) NOT NULL DEFAULT 'BUILTIN',
    synced_at TIMESTAMP WITH TIME ZONE,
    state VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    created_by BIGINT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_by BIGINT,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_model_spec_catalogs_provider_model_id UNIQUE (provider_model_id)
);

CREATE INDEX idx_model_spec_catalogs_family ON model_spec_catalogs(model_family);
CREATE INDEX idx_model_spec_catalogs_source ON model_spec_catalogs(source);

-- === 5. 删除旧 Metadata 表（按依赖顺序：子表先删） ===
DROP TABLE IF EXISTS product_model_metadata;
DROP TABLE IF EXISTS product_metadata;
DROP TABLE IF EXISTS model_metadata;
DROP TABLE IF EXISTS provider_metadata;
