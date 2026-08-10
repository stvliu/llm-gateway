--
-- Copyright (c) 2025 codingas.com
-- Licensed under the Apache License, Version 2.0.
-- See the LICENSE file for details.
--
-- V10__rename_status_to_state.sql
-- 统一状态字段命名：将 status 重命名为 state

-- ============================================
-- users 表
-- ============================================

-- 删除状态为 DELETED 的用户（DELETED 状态已废弃）
DELETE FROM users WHERE status = 'DELETED';

-- 重命名 status 列为 state
ALTER TABLE users RENAME COLUMN status TO state;

-- 更新注释
COMMENT ON COLUMN users.state IS '用户状态: ACTIVE, DISABLED, LOCKED';