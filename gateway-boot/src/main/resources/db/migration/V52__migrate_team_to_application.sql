-- ============================================
-- V52: Team → Application 1:1 平移迁移 + migration-default 兜底
-- ============================================
-- 依赖 V51 建立的 applications / application_channels 表与 user_api_keys.application_id 列
--
-- 迁移语义（对应设计 D7 幂等 / D9 授权不丢失不放大）：
--   1. 创建 migration-default 兜底应用（归属不明 Key 的容器）
--   2. 每个 Team → 一个 Application（code = 'team-' || team.id，name/description 继承，
--      state 小写 'active' 映射为大写 'ACTIVE'）
--   3. TeamChannel → ApplicationChannel 1:1 平移（每个 team 的渠道授权集合不变）
--   4. 单 Team 用户的 Key 回填到对应 Team 的 Application（归属明确）
--   5a. 多 Team 用户原 Team 渠道集取并集 → 授权给 migration-default（不放大到全局，
--       仅限多 Team 用户实际涉及的 team 渠道）
--   5b. 剩余 application_id 为 NULL 的 Key（多 Team / 无 Team 用户）归 migration-default
--
-- 幂等（D7）：所有 INSERT 用 WHERE NOT EXISTS 去重；UPDATE 仅作用于 application_id IS NULL
-- 的行，重复执行不再变更。UPDATE 采用相关子查询赋值（H2/PostgreSQL 通用，非 MySQL JOIN 语法）。
-- ============================================

-- 1. 兜底应用 migration-default
INSERT INTO applications (code, name, description, state)
SELECT 'migration-default', '迁移兜底应用', '归属不明 Key 兜底应用', 'ACTIVE'
WHERE NOT EXISTS (
    SELECT 1 FROM applications a WHERE a.code = 'migration-default'
);

-- 2. Team → Application 1:1（code = 'team-' || id，name/description 继承，state 大写化）
INSERT INTO applications (code, name, description, state)
SELECT
    CONCAT('team-', t.id),
    t.name,
    t.description,
    CASE WHEN LOWER(COALESCE(t.state, 'active')) = 'active' THEN 'ACTIVE' ELSE 'INACTIVE' END
FROM teams t
WHERE NOT EXISTS (
    SELECT 1 FROM applications a WHERE a.code = CONCAT('team-', t.id)
);

-- 3. TeamChannel → ApplicationChannel 1:1 平移（授权集合保持不变）
INSERT INTO application_channels (application_id, channel_id)
SELECT a.id, tc.channel_id
FROM team_channels tc
JOIN applications a ON a.code = CONCAT('team-', tc.team_id)
WHERE NOT EXISTS (
    SELECT 1 FROM application_channels ac
    WHERE ac.application_id = a.id AND ac.channel_id = tc.channel_id
);

-- 4. 单 Team 用户 Key 回填到对应 Team 的 Application
--    多 Team 用户此处跳过（交由第 5 步归 migration-default），避免归属歧义
UPDATE user_api_keys k
SET application_id = (
    SELECT a.id
    FROM user_teams ut
    JOIN applications a ON a.code = CONCAT('team-', ut.team_id)
    WHERE ut.user_id = k.user_id
)
WHERE k.application_id IS NULL
  AND k.user_id IN (
      SELECT ut2.user_id FROM user_teams ut2 GROUP BY ut2.user_id HAVING COUNT(*) = 1
  );

-- 5a. 多 Team 用户原 Team 渠道集并集 → 授权给 migration-default
--     仅收集多 Team 用户实际所属 team 的渠道，不引入与多 Team 用户无关的全局渠道
--
--     【已知取舍披露：跨用户渠道放大】
--     由于所有多 Team 用户的 Key 共用同一个 migration-default 兜底应用，本并集会跨所有
--     多 Team 用户累加到该单一应用上。当多个多 Team 用户的团队渠道集互不相交时（例如
--     用户 X 团队渠道 {100,200} 与用户 Y 团队渠道 {300,400}），其渠道会在 migration-default
--     上取并集 {100,200,300,400}，导致 X 的 Key 经由 migration-default 获得 Y 的 300/400
--     渠道访问权，Y 亦获得 X 的 100/200 —— 即跨用户授权放大。
--     这是单兜底应用设计下的已知取舍：单一应用内"取并集必放大、取交集必丢失"，本迁移
--     选择并集以避免丢失授权（D7 不丢失优先于 D9 不放大）。migration-default 仅为迁移期
--     临时容器，运维须在迁移后按用户拆分应用以恢复按用户渠道隔离，避免长期放大。
--     披露测试：TeamToApplicationMigrationTest#
--       migrationDefault_accumulatesMultiTeamUsersChannels_isKnownWidening
INSERT INTO application_channels (application_id, channel_id)
SELECT DISTINCT a.id, tc.channel_id
FROM applications a
JOIN team_channels tc ON tc.team_id IN (
    SELECT ut.team_id
    FROM user_teams ut
    WHERE ut.user_id IN (
        SELECT ut3.user_id FROM user_teams ut3 GROUP BY ut3.user_id HAVING COUNT(*) > 1
    )
)
WHERE a.code = 'migration-default'
  AND NOT EXISTS (
      SELECT 1 FROM application_channels ac
      WHERE ac.application_id = a.id AND ac.channel_id = tc.channel_id
  );

-- 5b. 剩余 application_id 为 NULL 的 Key（多 Team / 无 Team 用户）归 migration-default
UPDATE user_api_keys k
SET application_id = (
    SELECT a.id FROM applications a WHERE a.code = 'migration-default'
)
WHERE k.application_id IS NULL
  AND EXISTS (SELECT 1 FROM applications a WHERE a.code = 'migration-default');
