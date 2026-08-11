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
-- V54: 容灾画像表（resilience_profiles）
-- ============================================
-- P2 段 Task 4.1：引入应用级容灾画像聚合根。
-- 画像承载四层容灾栈（L0 Key 级 / L1 Channel 级 / L2 模型级 / L3 抛错）的开关与参数，
-- 预设档位（default/strict/aggressive/batch）由后续 Task 4.4 初始化数据写入。
-- 解析链 Application → Global（design.md D5），Application.resilience_profile_id 已在 V51 预留。
--
-- 说明：原计划版本号 V40，但 V40-V53 已被占用（当前最高 V53，Team→Application 迁移），
--       故顺延使用 V54 以避免 Flyway 版本冲突。Task 4.2（Cluster）将用 V55。
-- 方言：遵循 V51 项目约定，使用 H2（PostgreSQL 兼容模式）/ PostgreSQL 语法：
--       BIGSERIAL 主键、NOW() 时间默认、CONSTRAINT ... UNIQUE 唯一约束。
-- ============================================

CREATE TABLE resilience_profiles (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(128) NOT NULL,
    mode VARCHAR(16) NOT NULL DEFAULT 'STANDARD',
    enable_l2_model_degradation BOOLEAN NOT NULL DEFAULT TRUE,
    degradation_max_depth INT NOT NULL DEFAULT 2,
    enable_session_affinity BOOLEAN NOT NULL DEFAULT FALSE,
    session_affinity_ttl_minutes INT NOT NULL DEFAULT 30,
    enable_pinned_model BOOLEAN NOT NULL DEFAULT FALSE,
    pinned_model_id BIGINT,
    timeout INT NOT NULL DEFAULT 0,
    created_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_by BIGINT,
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_resilience_profiles_code ON resilience_profiles(code);
