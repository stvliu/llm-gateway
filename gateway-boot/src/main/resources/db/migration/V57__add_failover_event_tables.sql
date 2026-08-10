--
-- Copyright (c) 2025 codingas.com
-- Licensed under the Apache License, Version 2.0.
-- See the LICENSE file for details.
--
-- ============================================
-- V57: 转移事件表（failover_events）
-- ============================================
-- P2 段 Task 4.11c：引入 FailoverEvent 转移事件 domain（design.md D12）。
-- 记录每次候选转移（from 渠道/端点 → to 渠道/端点、errorType、decision L1/L2、exhausted、
-- traceId 串联同请求多次转移、occurredAt）。
-- 用途：容灾可观测性（读侧重），容灾总览页 10s 轮询渲染转移事件流 + 耗尽告警。
-- 不复用 CallLog（调用结果语义与转移动作语义不同维度，混表职责模糊）。
--
-- 冗余 from_cluster_id/to_cluster_id：便于 findRecent 的 clusterId 过滤直接匹配，避免 join channels 表。
-- Invoker 经 ChannelGateway.findById 反查 channelId→clusterId 填充，渠道不存在或未关联 cluster 时为 null；
-- clusterId 过滤已生效。索引见 V58。
--
-- 方言：遵循 V55 项目约定，使用 H2（PostgreSQL 兼容模式）/ PostgreSQL 语法：
--       BIGSERIAL 主键、NOW() 时间默认。
-- ============================================

CREATE TABLE failover_events (
    id BIGSERIAL PRIMARY KEY,
    trace_id VARCHAR(64),
    application_id BIGINT,
    from_channel_id BIGINT,
    from_endpoint_id BIGINT,
    to_channel_id BIGINT,
    to_endpoint_id BIGINT,
    from_cluster_id BIGINT,
    to_cluster_id BIGINT,
    error_type VARCHAR(32) NOT NULL,
    decision VARCHAR(8) NOT NULL,
    exhausted BOOLEAN NOT NULL DEFAULT FALSE,
    occurred_at TIMESTAMP NOT NULL,
    created_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_by BIGINT,
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_failover_events_occurred_at ON failover_events(occurred_at);
CREATE INDEX idx_failover_events_application_id ON failover_events(application_id);
CREATE INDEX idx_failover_events_trace_id ON failover_events(trace_id);
