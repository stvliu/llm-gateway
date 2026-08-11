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
-- V67: 删除 failover_events 的 Cluster 冗余列
-- ============================================
-- 容灾重构：Cluster 故障域 + 共因跳过逻辑退场，failover_events 表的
-- from_cluster_id、to_cluster_id（V57 建表时冗余，便于 findRecent 的 clusterId 过滤）、
-- common_cause_skip（V64 新增的共因跳过标记）三列均不再需要。
--
-- H2 2.3 不支持 DROP COLUMN 时级联删除索引（PostgreSQL 支持），故先显式 DROP INDEX
-- 再 DROP COLUMN，两端兼容。idx_failover_events_from_cluster_id、
-- idx_failover_events_to_cluster_id 由 V58 创建（复合索引，含 occurred_at）。
-- common_cause_skip 无索引，可直接 DROP COLUMN。
--
-- 方言：H2（PostgreSQL 兼容模式）/ PostgreSQL 均支持
--       DROP INDEX IF EXISTS 与 DROP COLUMN IF EXISTS。
-- ============================================

-- 先删除引用 cluster 列的复合索引（V58 创建）
DROP INDEX IF EXISTS idx_failover_events_from_cluster_id;
DROP INDEX IF EXISTS idx_failover_events_to_cluster_id;

-- 再删除 cluster 冗余列 + 共因跳过标记列
ALTER TABLE failover_events DROP COLUMN IF EXISTS from_cluster_id;
ALTER TABLE failover_events DROP COLUMN IF EXISTS to_cluster_id;
ALTER TABLE failover_events DROP COLUMN IF EXISTS common_cause_skip;
