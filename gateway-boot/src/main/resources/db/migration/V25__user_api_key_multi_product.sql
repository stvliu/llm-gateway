--
-- Copyright (c) 2025 codingas.com
-- Licensed under the Apache License, Version 2.0.
-- See the LICENSE file for details.
--
-- ============================================
-- V25: UserApiKey 多产品关联（1 Key → N Products）
-- ============================================

-- 1. 新建关联表
CREATE TABLE user_api_key_products (
    user_api_key_id BIGINT NOT NULL,
    product_id      BIGINT NOT NULL,
    PRIMARY KEY (user_api_key_id, product_id),
    CONSTRAINT fk_uakp_key      FOREIGN KEY (user_api_key_id) REFERENCES user_api_keys(id) ON DELETE CASCADE,
    CONSTRAINT fk_uakp_product  FOREIGN KEY (product_id)       REFERENCES products(id)      ON DELETE CASCADE
);

-- 2. 索引
CREATE INDEX idx_uakp_product ON user_api_key_products(product_id);

-- 3. 数据迁移：将现有 product_id 写入关联表
INSERT INTO user_api_key_products (user_api_key_id, product_id)
SELECT id, product_id FROM user_api_keys WHERE product_id IS NOT NULL;

-- 4. 移除原列
ALTER TABLE user_api_keys DROP COLUMN product_id;
