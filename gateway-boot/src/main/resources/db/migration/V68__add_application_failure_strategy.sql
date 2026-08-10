--
-- Copyright (c) 2025 codingas.com
-- Licensed under the Apache License, Version 2.0.
-- See the LICENSE file for details.
--
-- ============================================
-- V68: applications 新增 failure_strategy 列 + 数据迁移
-- ============================================
-- 容灾重构：引入应用级失败处理策略 Application.failureStrategy
-- （@Enumerated(STRING)，枚举名存储：FAIL_FAST/FAIL_RETRY/FAIL_OVER，默认 FAIL_RETRY）。
-- ApplicationDo 映射列名 failure_strategy（VARCHAR(16), NOT NULL）。
--
-- 列加完后做数据迁移：现有应用设为 FAIL_OVER，保持原 L0+L1 容灾行为不变
-- （原 Cluster 故障域机制默认跨域转移，等价于 FAIL_OVER 语义）。
--
-- 方言：H2（PostgreSQL 兼容模式）/ PostgreSQL 均支持
--       ADD COLUMN IF NOT EXISTS ... NOT NULL DEFAULT ...。
-- ============================================

-- applications 新增 failure_strategy 列（默认 FAIL_RETRY，保证 NOT NULL 约束）
ALTER TABLE applications ADD COLUMN IF NOT EXISTS failure_strategy VARCHAR(16) NOT NULL DEFAULT 'FAIL_RETRY';

-- 数据迁移：现有应用设为 FAIL_OVER（保持原 L0+L1 行为不变）
UPDATE applications SET failure_strategy = 'FAIL_OVER';
