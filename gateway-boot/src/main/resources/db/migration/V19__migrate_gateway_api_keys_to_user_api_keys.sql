-- Phase 3: 数据迁移 — GatewayApiKey → UserApiKey
-- 为现有 GatewayApiKey 创建对应的 UserApiKey，实现双写兼容

-- 1. 为每个 GatewayApiKey 的用户查找其默认团队
-- （V17 已为每个用户创建了默认团队，名称为 {username}-team）

-- 2. 为每个 GatewayApiKey 创建对应的 UserApiKey
-- 绑定到用户的默认团队和对应供应商的默认产品
INSERT INTO user_api_keys (team_id, owner_user_id, product_id, key_hash, key_prefix, name, state, created_at, updated_at)
SELECT
    t.id,                                          -- 用户默认团队 ID
    gak.user_id,                                   -- 原始用户 ID
    p.id,                                          -- 供应商默认产品 ID
    gak.key_hash,                                  -- 原始 key_hash（认证兼容）
    'sk-',                                         -- 前缀
    COALESCE(gak.name, 'migrated-key-' || gak.id), -- 名称
    LOWER(gak.state)::VARCHAR,                      -- 状态转换
    gak.created_at,
    gak.updated_at
FROM gateway_api_keys gak
JOIN teams t ON t.name = (SELECT u.name FROM users u WHERE u.id = gak.user_id) || '-team'
JOIN products p ON p.provider_id = (SELECT m.provider_id FROM models m WHERE m.id = gak.model_id LIMIT 1)
                AND p.name LIKE '%-default'
WHERE gak.state IN ('ACTIVE', 'active')
  AND NOT EXISTS (
    SELECT 1 FROM user_api_keys uak
    WHERE uak.key_hash = gak.key_hash
  );

-- 3. 备用迁移：如果 GatewayApiKey 没有 model_id，使用用户关联的第一个产品
-- 对于没有 model_id 的 GatewayApiKey，绑定到用户所属供应商的第一个默认产品
INSERT INTO user_api_keys (team_id, owner_user_id, product_id, key_hash, key_prefix, name, state, created_at, updated_at)
SELECT
    t.id,
    gak.user_id,
    (SELECT MIN(p.id) FROM products p WHERE p.name LIKE '%-default'),
    gak.key_hash,
    'sk-',
    COALESCE(gak.name, 'migrated-key-' || gak.id),
    LOWER(gak.state)::VARCHAR,
    gak.created_at,
    gak.updated_at
FROM gateway_api_keys gak
JOIN teams t ON t.name = (SELECT u.name FROM users u WHERE u.id = gak.user_id) || '-team'
WHERE gak.state IN ('ACTIVE', 'active')
  AND NOT EXISTS (
    SELECT 1 FROM user_api_keys uak WHERE uak.key_hash = gak.key_hash
  )
  AND NOT EXISTS (
    SELECT 1 FROM user_api_keys uak2
    WHERE uak2.key_hash = gak.key_hash
  );

-- 注意：此迁移脚本仅迁移活跃状态的 GatewayApiKey
-- 旧数据不删除，保留用于降级兼容