--
-- Copyright (c) 2025 codingas.com
-- Licensed under the Apache License, Version 2.0.
-- See the LICENSE file for details.
--
-- 回滚 V25: 将 user_api_key_products 关联数据写回 user_api_keys.product_id
-- 注意：如果一个 Key 关联了多个产品，回滚时只保留第一个关联的产品

-- 恢复 product_id 列
ALTER TABLE user_api_keys ADD COLUMN product_id BIGINT;

-- 从关联表回填（取每个 key 的第一个产品关联）
UPDATE user_api_keys u
SET product_id = (
    SELECT MIN(uakp.product_id)
    FROM user_api_key_products uakp
    WHERE uakp.user_api_key_id = u.id
);

-- 删除关联表
DROP TABLE user_api_key_products;
