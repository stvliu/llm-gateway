--
-- Copyright (c) 2025 codingas.com
-- Licensed under the Apache License, Version 2.0.
-- See the LICENSE file for details.
--
-- ============================================
-- V58: failover_events 补 clusterId 过滤索引
-- ============================================
-- P2 段 Task 4.11c code quality 修复：V57 建表时未为 from_cluster_id/to_cluster_id 建索引，
-- findRecent 的 clusterId 过滤（fromClusterId 或 toClusterId 匹配）走全表扫描。
-- Invoker 经 ChannelGateway.findById 反查填充 clusterId 后，clusterId 过滤已生效，
-- 需补复合索引（clusterId + occurredAt）支撑倒序查询性能。
--
-- 两个独立索引：from_cluster_id + occurred_at、to_cluster_id + occurred_at，
-- 覆盖 clusterId 过滤的 OR 两分支（Repository @Query: fromClusterId = :clusterId OR toClusterId = :clusterId）。
--
-- 方言：遵循 V55/V57 项目约定，使用 H2（PostgreSQL 兼容模式）/ PostgreSQL 语法。
-- ============================================

CREATE INDEX idx_failover_events_from_cluster_id ON failover_events(from_cluster_id, occurred_at);
CREATE INDEX idx_failover_events_to_cluster_id ON failover_events(to_cluster_id, occurred_at);
