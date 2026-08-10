--
-- Copyright (c) 2025 codingas.com
-- Licensed under the Apache License, Version 2.0.
-- See the LICENSE file for details.
--
-- 为 providers 表添加品牌信息字段
-- 这些字段从 ProviderMetadata 复制而来，支持完全复制模式

ALTER TABLE providers ADD COLUMN IF NOT EXISTS provider_id VARCHAR(64);
ALTER TABLE providers ADD COLUMN IF NOT EXISTS icon_url VARCHAR(512);
ALTER TABLE providers ADD COLUMN IF NOT EXISTS description VARCHAR(512);
ALTER TABLE providers ADD COLUMN IF NOT EXISTS tags TEXT;

-- 创建索引加速按 provider_id 查询
CREATE INDEX IF NOT EXISTS idx_providers_provider_id ON providers(provider_id);

COMMENT ON COLUMN providers.provider_id IS '品牌标识（关联 ProviderMetadata.providerId）';
COMMENT ON COLUMN providers.icon_url IS '品牌图标 URL';
COMMENT ON COLUMN providers.description IS '描述信息';
COMMENT ON COLUMN providers.tags IS '标签（JSON 格式）';
