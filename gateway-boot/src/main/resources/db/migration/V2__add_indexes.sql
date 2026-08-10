--
-- Copyright (c) 2025 codingas.com
-- Licensed under the Apache License, Version 2.0.
-- See the LICENSE file for details.
--
-- V2__add_indexes.sql
-- 补充索引（V1 已包含主要索引）

-- ============================================
-- 补充索引（按需添加）
-- ============================================

-- 用户表补充索引
CREATE INDEX IF NOT EXISTS idx_users_deleted ON users(deleted_at);

-- 模型表补充索引
CREATE INDEX IF NOT EXISTS idx_models_deleted ON models(deleted_at);

-- 提供商表补充索引
CREATE INDEX IF NOT EXISTS idx_providers_deleted ON providers(deleted_at);
CREATE INDEX IF NOT EXISTS idx_providers_type ON providers(provider_type);

-- API Key 表补充索引
CREATE INDEX IF NOT EXISTS idx_gateway_api_keys_deleted ON gateway_api_keys(deleted_at);
CREATE INDEX IF NOT EXISTS idx_provider_api_keys_status ON provider_api_keys(status);

-- Token 限额表补充索引
CREATE INDEX IF NOT EXISTS idx_token_limits_deleted ON token_limits(deleted_at);
CREATE INDEX IF NOT EXISTS idx_token_limits_status ON token_limits(status);

-- 使用记录表补充索引
CREATE INDEX IF NOT EXISTS idx_usage_logs_status ON usage_logs(status_code);
CREATE INDEX IF NOT EXISTS idx_usage_logs_model ON usage_logs(model_id);

-- 预警规则表补充索引
CREATE INDEX IF NOT EXISTS idx_alert_rules_deleted ON alert_rules(deleted_at);

-- 预警通知表补充索引
CREATE INDEX IF NOT EXISTS idx_alert_notifications_deleted ON alert_notifications(deleted_at);
