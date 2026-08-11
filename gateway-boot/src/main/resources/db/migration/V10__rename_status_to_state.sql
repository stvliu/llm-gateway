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