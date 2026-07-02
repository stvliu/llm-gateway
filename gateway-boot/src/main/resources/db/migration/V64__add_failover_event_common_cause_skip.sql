-- ============================================
-- V64: failover_events 表新增 common_cause_skip 列
-- ============================================
-- Task 6：FailoverEvent/FailoverOccurredEvent/DO 新增 commonCauseSkip 标记字段
-- （boolean，默认 false）。本任务仅加列，Task 9 填充判定逻辑
-- （从 RoutingContext 直取共因跳过判定）。
--
-- 版本号 V64 按 plan 指定使用（现有最新 V59/V60/V61-V63 间隔），Flyway 允许跳号。
-- 方言：遵循 V55/V57 项目约定，使用 H2（PostgreSQL 兼容模式）/ PostgreSQL 语法，
--       ADD COLUMN ... NOT NULL DEFAULT ... 两端均支持。
-- ============================================

ALTER TABLE failover_events ADD COLUMN common_cause_skip BOOLEAN NOT NULL DEFAULT FALSE;
