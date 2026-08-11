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
-- V66: 删除 channels.cluster_id 列（共因跳过机制退场）
-- ============================================
-- 容灾重构：Cluster 故障域退场后，channels.cluster_id 物理外键不再有意义。
-- channels.cluster_id 由 V55 添加（BIGINT，可空，无 FK 约束，遵循项目物理 ID 约定），
-- 配套索引 idx_channels_cluster_id（V55 创建）。
--
-- H2 2.3 不支持 DROP COLUMN 时级联删除索引（PostgreSQL 支持），故先显式 DROP INDEX
-- 再 DROP COLUMN，两端兼容。
--
-- 方言：H2（PostgreSQL 兼容模式）/ PostgreSQL 均支持
--       DROP INDEX IF EXISTS 与 DROP COLUMN IF EXISTS。
-- ============================================

-- 先删除引用 cluster_id 的索引（V55 创建 idx_channels_cluster_id）
DROP INDEX IF EXISTS idx_channels_cluster_id;

-- 再删除 channels.cluster_id 列
ALTER TABLE channels DROP COLUMN IF EXISTS cluster_id;
