-- ============================================
-- V62: 删 resilience_profiles 表（含 V56 seed 数据）
-- ============================================
-- Task 8（simplify-resilience-architecture）：ResilienceProfile 实体退场，
-- resilience_profiles 表与 V56 写入的四个预设档位 seed 数据（default/strict/aggressive/batch）
-- 一并删除。DROP TABLE 自动级联删除表内全部数据，无需单独 DELETE seed。
--
-- 顺序约束：依赖 V61 先删除 applications.resilience_profile_id（软外键），
--           再删本表，避免应用行残留对已删画像的引用。
--
-- 方言：H2（PostgreSQL 兼容模式）/ PostgreSQL 通用：DROP TABLE IF EXISTS 双方言均支持。
-- ============================================

DROP TABLE IF EXISTS resilience_profiles;
