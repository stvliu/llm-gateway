-- V10__update_user_status_enum.sql
-- 将用户状态从 ACTIVE 更新为 ENABLED，移除 DELETED 状态

-- 更新现有用户状态：ACTIVE -> ENABLED
UPDATE users SET status = 'ENABLED' WHERE status = 'ACTIVE';

-- 删除状态为 DELETED 的用户（物理删除，因为 DELETED 状态已废弃）
DELETE FROM users WHERE status = 'DELETED';
