-- ============================================
-- V60: clusters 表瘦身（删 region/priority/health_status，新增 description）
-- ============================================
-- Task 6：Cluster 语义改造为「跨供应商故障独立性分组」并瘦身字段。
--   删除 region（就近路由交还给应用层）、priority（跨域转移排序交还给应用层）、
--   health_status（域级健康聚合随 DomainHealth 路由器在 Task 5 移除）；
--   新增 description（共因特征说明，spec cluster-failover 新增字段）。
-- 保留 code/name/provider_id + 审计。
--
-- 生产环境 ddl-auto=validate，schema 全程由 Flyway 管理，故 description 必须在此迁移补加
-- （开发环境 ddl-auto=update 虽可自动补列，但为保持 dev/prod schema 一致，统一由 Flyway 管理）。
--
-- 使用 IF EXISTS / IF NOT EXISTS：确认列存在性后再增删，列状态不符时不报错
-- （H2/PG 均支持 DROP COLUMN IF EXISTS 与 ADD COLUMN IF NOT EXISTS），兼容已执行过部分清理的环境。
-- 方言：遵循 V55/V57 项目约定，使用 H2（PostgreSQL 兼容模式）/ PostgreSQL 语法。
-- ============================================

ALTER TABLE clusters DROP COLUMN IF EXISTS region;
ALTER TABLE clusters DROP COLUMN IF EXISTS priority;
ALTER TABLE clusters DROP COLUMN IF EXISTS health_status;
ALTER TABLE clusters ADD COLUMN IF NOT EXISTS description VARCHAR(512);
