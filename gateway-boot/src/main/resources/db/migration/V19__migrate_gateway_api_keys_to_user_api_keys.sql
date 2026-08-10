--
-- Copyright (c) 2025 codingas.com
-- Licensed under the Apache License, Version 2.0.
-- See the LICENSE file for details.
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