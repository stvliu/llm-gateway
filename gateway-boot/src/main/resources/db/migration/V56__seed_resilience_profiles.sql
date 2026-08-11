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
-- V56: 预设容灾档位初始化数据（resilience_profiles seed）
-- ============================================
-- Task 4.4：写入四个预设容灾档位，供控制台模板化与 ResilienceResolver 全局回退使用。
-- 档位语义见 design.md D5 与 docs/容灾管理范式.md 第四节：
--   - default    : STANDARD 通用默认，L1 全开 + L2 浅降级（深度 2），平衡可用性与质量。
--                  ResilienceResolver 解析链 Application → Global 回退依赖 code='default' 始终存在，
--                  缺失会抛 RESILIENCE_DEFAULT_PROFILE_MISSING（见 ResilienceResolver）。
--   - strict     : STRICT 严格，L1 全开 + L2 关闭（深度 0），宁可报错不可换模型
--                  （对应 Claude Code/CodeX）。超时 60s。
--   - aggressive : AGGRESSIVE 激进，L1 全开 + L2 深降级（深度 3）+ 短超时（15s），
--                  可用性优先，质量次之（对应客服/HelpDesk）。
--   - batch      : STANDARD 的 QUEUED 变体（批量），L1 全开 + L2 浅降级（深度 1）+ 长超时（120s），
--                  对应 IPD 文档生成。BATCH 不单列 ResilienceMode 档位（保持三档简洁），
--                  QUEUED/COST_OPTIMIZED 等高级转移特性由后续字段扩展或运行时按 code 推导。
--
-- 字段值推导依据：
--   - ResilienceMode 枚举 Javadoc（STANDARD 深度 2 / STRICT L2 关闭 / AGGRESSIVE 深度 3 + 短超时）
--   - docs/容灾管理范式.md 第四节「档位 → ResilienceProfile 字段推导表」
--   - docs/容灾方案设计.md 第六节五场景映射（aggressive ↔ 客服 15s；batch ↔ IPD 120s 浅 1）
--
-- 说明：
--   1. 版本号：plan 原写 V42，但 V42-V55 已被占用（当前最高 V55，V54 建表/V55 Cluster），
--      故用 V56 避免 Flyway 版本冲突。
--   2. strict 的 pinned_model：范式建议锁定模型，但 V54 仅有 pinned_model_id（单数，依赖部署具体
--      model id），seed 无法预知，故 enable_pinned_model=FALSE、pinned_model_id=NULL，由管理员按需配置。
--   3. batch 的 QUEUED 特性：V54 表无 transfer_mode/cost_strategy 字段，QUEUED/COST_OPTIMIZED 无法
--      落表，仅靠 code='batch' + name + 本注释标识，运行时按 code 推导或后续字段扩展。
--   4. 审计字段：created_by/updated_by=0 表示系统初始化标识（seed 无真实用户上下文）。
--   5. 幂等：沿用 V52 项目约定，INSERT ... SELECT ... WHERE NOT EXISTS（H2/PostgreSQL 通用）。
--      Flyway 保证迁移只执行一次；WHERE NOT EXISTS 防御开发期 H2 重置重跑与误重复。
-- ============================================

-- 1. default 画像（全局兜底，ResilienceResolver 回退依赖）
INSERT INTO resilience_profiles (
    code, name, mode,
    enable_l2_model_degradation, degradation_max_depth,
    enable_session_affinity, session_affinity_ttl_minutes,
    enable_pinned_model, pinned_model_id,
    timeout,
    created_by, created_at, updated_by, updated_at
)
SELECT
    'default', '默认画像', 'STANDARD',
    TRUE, 2,
    FALSE, 30,
    FALSE, NULL,
    0,
    0, NOW(), 0, NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM resilience_profiles WHERE code = 'default'
);

-- 2. strict 画像（严格，L2 关闭，宁可报错不可换模型）
INSERT INTO resilience_profiles (
    code, name, mode,
    enable_l2_model_degradation, degradation_max_depth,
    enable_session_affinity, session_affinity_ttl_minutes,
    enable_pinned_model, pinned_model_id,
    timeout,
    created_by, created_at, updated_by, updated_at
)
SELECT
    'strict', '严格画像', 'STRICT',
    FALSE, 0,
    FALSE, 30,
    FALSE, NULL,
    60,
    0, NOW(), 0, NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM resilience_profiles WHERE code = 'strict'
);

-- 3. aggressive 画像（激进，L2 深降级 + 短超时）
INSERT INTO resilience_profiles (
    code, name, mode,
    enable_l2_model_degradation, degradation_max_depth,
    enable_session_affinity, session_affinity_ttl_minutes,
    enable_pinned_model, pinned_model_id,
    timeout,
    created_by, created_at, updated_by, updated_at
)
SELECT
    'aggressive', '激进画像', 'AGGRESSIVE',
    TRUE, 3,
    FALSE, 30,
    FALSE, NULL,
    15,
    0, NOW(), 0, NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM resilience_profiles WHERE code = 'aggressive'
);

-- 4. batch 画像（STANDARD 的 QUEUED 变体，批量长超时）
INSERT INTO resilience_profiles (
    code, name, mode,
    enable_l2_model_degradation, degradation_max_depth,
    enable_session_affinity, session_affinity_ttl_minutes,
    enable_pinned_model, pinned_model_id,
    timeout,
    created_by, created_at, updated_by, updated_at
)
SELECT
    'batch', '批量画像', 'STANDARD',
    TRUE, 1,
    FALSE, 30,
    FALSE, NULL,
    120,
    0, NOW(), 0, NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM resilience_profiles WHERE code = 'batch'
);
