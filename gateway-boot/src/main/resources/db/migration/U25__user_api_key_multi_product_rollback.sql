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
