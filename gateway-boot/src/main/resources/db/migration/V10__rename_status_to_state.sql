-- V10__rename_status_to_state.sql
-- 统一状态字段命名：将 status 重命名为 state，并更新用户状态枚举值

-- ============================================
-- users 表
-- ============================================

-- 先更新用户状态值：ACTIVE -> ENABLED（与前端保持一致）
UPDATE users SET status = 'ENABLED' WHERE status = 'ACTIVE';

-- 删除状态为 DELETED 的用户（DELETED 状态已废弃）
DELETE FROM users WHERE status = 'DELETED';

-- 重命名 status 列为 state
ALTER TABLE users RENAME COLUMN status TO state;

-- 更新注释
COMMENT ON COLUMN users.state IS '用户状态: ACTIVE, DISABLED, LOCKED';