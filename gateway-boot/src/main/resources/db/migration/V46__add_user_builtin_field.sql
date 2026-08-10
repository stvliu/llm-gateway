--
-- Copyright (c) 2025 codingas.com
-- Licensed under the Apache License, Version 2.0.
-- See the LICENSE file for details.
--
-- 添加内建用户标记字段，用于保护系统关键账户不被删除/降级/禁用
ALTER TABLE users ADD COLUMN builtin BOOLEAN NOT NULL DEFAULT FALSE;

-- 将 admin 用户标记为内建用户
UPDATE users SET builtin = TRUE WHERE username = 'admin';
