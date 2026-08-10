--
-- Copyright (c) 2025 codingas.com
-- Licensed under the Apache License, Version 2.0.
-- See the LICENSE file for details.
--
-- V29__add_product_metadata.sql
-- 新增产品元数据表，建立三级元数据关系

-- ============================================
-- 1. 创建 product_metadata 表
-- ============================================
CREATE TABLE product_metadata (
    id BIGSERIAL PRIMARY KEY,
    provider_id VARCHAR(64) NOT NULL,
    product_name VARCHAR(128) NOT NULL,
    product_type VARCHAR(32) NOT NULL,
    description TEXT,
    endpoints JSON NOT NULL,
    is_default BOOLEAN DEFAULT false,
    state VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    source VARCHAR(32) NOT NULL DEFAULT 'BUILTIN',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_at TIMESTAMP WITH TIME ZONE,
    updated_by BIGINT
);

-- 唯一索引：(provider_id, product_name) 联合唯一
CREATE UNIQUE INDEX uk_product_metadata_provider_name
    ON product_metadata(provider_id, product_name);

-- 查询索引
CREATE INDEX idx_product_metadata_provider ON product_metadata(provider_id);
CREATE INDEX idx_product_metadata_type ON product_metadata(product_type);
CREATE INDEX idx_product_metadata_state ON product_metadata(state);

-- ============================================
-- 2. 调整 model_metadata 表
-- ============================================
-- 新增 product_id 字段
ALTER TABLE model_metadata ADD COLUMN product_id BIGINT;

-- 新增索引
CREATE INDEX idx_model_metadata_product ON model_metadata(product_id);

-- 添加外键约束（可选，根据实际需求决定）
-- ALTER TABLE model_metadata
--     ADD CONSTRAINT fk_model_metadata_product
--     FOREIGN KEY (product_id) REFERENCES product_metadata(id);

-- ============================================
-- 3. 数据迁移说明
-- ============================================
-- 现有 model_metadata 数据暂时保留 provider_id，
-- 待产品数据同步后，通过脚本或应用层迁移 product_id
