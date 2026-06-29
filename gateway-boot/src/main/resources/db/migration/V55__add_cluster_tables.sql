-- ============================================
-- V55: Cluster 故障域表（clusters）+ Channel.cluster_id 物理外键
-- ============================================
-- P2 段 Task 4.2：引入 Cluster 故障域聚合根（design.md D10）。
-- Cluster = Channel 的故障域分组，同组 Channel 共享共因特征（同供应商/同账号/同区域/同专线）。
-- 容灾转移规则：故障域内优先 → 整域故障才跨域（容灾方案设计.md 第三节）。
-- Channel.cluster_id 为物理 ID（无 FK 约束，遵循项目"外键关联使用物理 ID"约定），可空。
--
-- 说明：plan 原写 V41，但 V41-V54 已被占用（Task 4.1 用 V54），故顺延使用 V55。
-- 方言：遵循 V51/V54 项目约定，使用 H2（PostgreSQL 兼容模式）/ PostgreSQL 语法：
--       BIGSERIAL 主键、NOW() 时间默认、CONSTRAINT ... UNIQUE 唯一约束。
-- ============================================

CREATE TABLE clusters (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(128) NOT NULL,
    provider_id BIGINT,
    region VARCHAR(32),
    priority INT NOT NULL DEFAULT 100,
    health_status VARCHAR(16) NOT NULL DEFAULT 'HEALTHY',
    created_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_by BIGINT,
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_clusters_code ON clusters(code);
CREATE INDEX idx_clusters_provider_id ON clusters(provider_id);

-- Channel 增 cluster_id 物理外键（可空，无 FK 约束，遵循项目物理 ID 约定）
ALTER TABLE channels ADD COLUMN cluster_id BIGINT;
CREATE INDEX idx_channels_cluster_id ON channels(cluster_id);
