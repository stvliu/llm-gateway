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
-- Phase 3: 数据迁移 — GatewayApiKey → UserApiKey
-- 为现有 GatewayApiKey 创建对应的 UserApiKey，实现双写兼容

-- gateway_api_keys 表没有 provider_id 列，无法直接关联到供应商的默认产品
-- 因此使用第一个默认产品作为迁移目标

INSERT INTO user_api_keys (team_id, owner_user_id, product_id, key_hash, key_prefix, name, state, created_at, updated_at)
SELECT
    t.id,
    gak.user_id,
    (SELECT MIN(p.id) FROM products p WHERE p.name LIKE '%-default'),
    gak.key_hash,
    'sk-',
    COALESCE(gak.name, 'migrated-key-' || gak.id),
    CAST(LOWER(gak.state) AS VARCHAR),
    gak.created_at,
    gak.updated_at
FROM gateway_api_keys gak
JOIN teams t ON t.name = (SELECT u.username FROM users u WHERE u.id = gak.user_id) || '-team'
WHERE gak.state IN ('ACTIVE', 'active')
  AND NOT EXISTS (
    SELECT 1 FROM user_api_keys uak WHERE uak.key_hash = gak.key_hash
  );

-- 注意：此迁移脚本仅迁移活跃状态的 GatewayApiKey
-- 旧数据不删除，保留用于降级兼容