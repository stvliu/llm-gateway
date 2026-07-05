-- ============================================
-- V65: 删除 Cluster 故障域表（Cluster 聚合根退场）
-- ============================================
-- 容灾重构：Cluster 故障域 + 共因跳过逻辑退场，引入应用级失败处理策略
-- （Application.failureStrategy）。Cluster 聚合根已在前序 Task 从 Java 代码删除，
-- 本迁移同步 DB schema。
--
-- clusters 表由 V55 创建（V60 瘦身保留 code/name/provider_id/description + 审计）。
-- 表上索引（idx_clusters_code、idx_clusters_provider_id）随 DROP TABLE 自动删除。
-- channels.cluster_id 为物理 ID（无 FK 约束），删表无依赖阻塞。
--
-- 方言：H2（PostgreSQL 兼容模式）/ PostgreSQL 均支持 DROP TABLE IF EXISTS。
-- ============================================

DROP TABLE IF EXISTS clusters;
