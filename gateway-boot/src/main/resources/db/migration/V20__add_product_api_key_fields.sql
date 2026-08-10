--
-- Copyright (c) 2025 codingas.com
-- Licensed under the Apache License, Version 2.0.
-- See the LICENSE file for details.
--
-- ============================================
-- V20: 产品 API Key 表新增 base_url 和 description 字段
-- ============================================

ALTER TABLE product_api_keys ADD COLUMN IF NOT EXISTS base_url VARCHAR(512);
ALTER TABLE product_api_keys ADD COLUMN IF NOT EXISTS description VARCHAR(512);

COMMENT ON COLUMN product_api_keys.base_url IS '请求基础 URL';
COMMENT ON COLUMN product_api_keys.description IS '描述';