--
-- Copyright (c) 2025 codingas.com
-- Licensed under the Apache License, Version 2.0.
-- See the LICENSE file for details.
--
-- ============================================
-- V61: applications 表加 timeout 列、删 resilience_profile_id 列
-- ============================================
-- Task 8（simplify-resilience-architecture）：ResilienceProfile 实体退场，
-- timeout 直接下沉到 Application 字段（承接原 ResilienceProfile.timeout 语义）。
--
-- 变更：
--   1. applications 新增 timeout INT NOT NULL DEFAULT 0
--      （0 表示用渠道默认，与原 ResilienceProfile.timeout=0 语义一致；既有行回填 0）
--   2. applications 删除 resilience_profile_id 列
--      （ResilienceProfile 实体退场，不再关联画像；原列为软外键 BIGINT，无物理 FK 约束）
--
-- 顺序约束：本迁移先于 V62（DROP TABLE resilience_profiles），
--           先删 applications.resilience_profile_id 再删画像表，避免软外键引用悬空。
--
-- 方言：H2（PostgreSQL 兼容模式）/ PostgreSQL 通用：
--       ADD COLUMN IF NOT EXISTS / DROP COLUMN IF EXISTS 双方言均支持。
-- ============================================

ALTER TABLE applications ADD COLUMN IF NOT EXISTS timeout INT NOT NULL DEFAULT 0;
ALTER TABLE applications DROP COLUMN IF EXISTS resilience_profile_id;
