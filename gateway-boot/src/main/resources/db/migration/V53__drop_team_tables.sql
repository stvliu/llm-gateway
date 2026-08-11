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
-- ============================================
-- V53: 移除 Team 体系表（数据已由 V52 迁移至 Application/ApplicationChannel）
-- ============================================
-- 依赖 V52 已完成 Team → Application 1:1 平移迁移：
--   - 每个 Team 生成对应 Application（code = 'team-' || id）
--   - TeamChannel → ApplicationChannel 1:1 平移
--   - UserApiKey.application_id 已回填
-- 本迁移丢弃 Team 体系表，完成 Team 体系下线。
--
-- 方言：H2（PostgreSQL 兼容模式）/ PostgreSQL 通用。
--   DROP TABLE IF EXISTS 在两种方言下均可用。
-- 顺序：先 team_channels（逻辑依赖 teams，虽无显式 FK），再 user_teams，最后 teams。
-- ============================================

DROP TABLE IF EXISTS team_channels;
DROP TABLE IF EXISTS user_teams;
DROP TABLE IF EXISTS teams;
