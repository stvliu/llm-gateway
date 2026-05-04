-- V3__add_version_fields.sql
-- 添加 version 字段用于乐观锁和变更检测

-- ============================================
-- 用户表
-- ============================================
ALTER TABLE users ADD COLUMN version BIGINT DEFAULT 0 NOT NULL;

-- ============================================
-- 网关 API Key 表
-- ============================================
ALTER TABLE gateway_api_keys ADD COLUMN version BIGINT DEFAULT 0 NOT NULL;

-- ============================================
-- 提供商表
-- ============================================
ALTER TABLE providers ADD COLUMN version BIGINT DEFAULT 0 NOT NULL;

-- ============================================
-- Provider API Key 表
-- ============================================
ALTER TABLE provider_api_keys ADD COLUMN version BIGINT DEFAULT 0 NOT NULL;

-- ============================================
-- 模型表
-- ============================================
ALTER TABLE models ADD COLUMN version BIGINT DEFAULT 0 NOT NULL;

-- ============================================
-- 路由分组表
-- ============================================
ALTER TABLE route_groups ADD COLUMN version BIGINT DEFAULT 0 NOT NULL;

-- ============================================
-- 路由分组与提供商关联表
-- ============================================
ALTER TABLE route_group_providers ADD COLUMN version BIGINT DEFAULT 0 NOT NULL;

-- ============================================
-- Token 限额表
-- ============================================
ALTER TABLE token_limits ADD COLUMN version BIGINT DEFAULT 0 NOT NULL;

-- ============================================
-- 限流配置表
-- ============================================
ALTER TABLE rate_limit_configs ADD COLUMN version BIGINT DEFAULT 0 NOT NULL;

-- ============================================
-- 审计日志表
-- ============================================
ALTER TABLE audit_logs ADD COLUMN version BIGINT DEFAULT 0 NOT NULL;

-- ============================================
-- 使用记录表
-- ============================================
ALTER TABLE usage_logs ADD COLUMN version BIGINT DEFAULT 0 NOT NULL;

-- ============================================
-- 预警规则表
-- ============================================
ALTER TABLE alert_rules ADD COLUMN version BIGINT DEFAULT 0 NOT NULL;

-- ============================================
-- 预警通知表
-- ============================================
ALTER TABLE alert_notifications ADD COLUMN version BIGINT DEFAULT 0 NOT NULL;

-- ============================================
-- IP 黑名单表
-- ============================================
ALTER TABLE ip_blocklist ADD COLUMN version BIGINT DEFAULT 0 NOT NULL;

-- ============================================
-- 敏感数据规则表
-- ============================================
ALTER TABLE sensitive_data_rules ADD COLUMN version BIGINT DEFAULT 0 NOT NULL;

-- ============================================
-- 索引：加速 MAX(version) 查询
-- 仅对需要频繁查询版本的核心配置表创建索引
-- ============================================
CREATE INDEX IF NOT EXISTS idx_providers_version ON providers(version);
CREATE INDEX IF NOT EXISTS idx_models_version ON models(version);
CREATE INDEX IF NOT EXISTS idx_provider_api_keys_version ON provider_api_keys(version);
