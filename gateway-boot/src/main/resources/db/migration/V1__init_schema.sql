--
-- Copyright (c) 2025 codingas.com
-- Licensed under the Apache License, Version 2.0.
-- See the LICENSE file for details.
--
-- V1__init_schema.sql
-- LLM Gateway 初始数据库结构（H2 PostgreSQL 兼容模式）

-- ============================================
-- 用户表
-- ============================================
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(64) NOT NULL,
    email VARCHAR(128),
    password_hash VARCHAR(256),
    phone VARCHAR(32),
    avatar_url VARCHAR(512),
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    role VARCHAR(32) DEFAULT 'USER',
    email_verified BOOLEAN DEFAULT FALSE,
    oauth_providers JSON,
    pii_salt VARCHAR(64),
    last_login_at TIMESTAMP,
    deleted_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_at TIMESTAMP,
    updated_by BIGINT
);

CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_status ON users(status);

-- ============================================
-- 网关 API Key 表
-- ============================================
CREATE TABLE gateway_api_keys (
    id BIGSERIAL PRIMARY KEY,
    key_hash VARCHAR(256) NOT NULL,
    user_id BIGINT NOT NULL,
    name VARCHAR(64),
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    expires_at TIMESTAMP,
    last_used_at TIMESTAMP,
    ip_whitelist JSON,
    deleted_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_at TIMESTAMP,
    updated_by BIGINT,
    CONSTRAINT fk_gateway_api_keys_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_gateway_api_keys_key_hash ON gateway_api_keys(key_hash);
CREATE INDEX idx_gateway_api_keys_user ON gateway_api_keys(user_id);
CREATE INDEX idx_gateway_api_keys_status ON gateway_api_keys(status);
CREATE INDEX idx_gateway_api_keys_expires ON gateway_api_keys(expires_at);

-- ============================================
-- 提供商表
-- ============================================
CREATE TABLE providers (
    id BIGSERIAL PRIMARY KEY,
    provider_name VARCHAR(128) NOT NULL,
    provider_type VARCHAR(32) NOT NULL,
    base_url VARCHAR(256),
    website_url VARCHAR(512),
    api_doc_url VARCHAR(512),
    priority INT DEFAULT 100,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    deleted_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_at TIMESTAMP,
    updated_by BIGINT
);

CREATE INDEX idx_providers_status ON providers(status);

-- ============================================
-- Provider API Key 表
-- ============================================
CREATE TABLE provider_api_keys (
    id BIGSERIAL PRIMARY KEY,
    provider_id BIGINT NOT NULL,
    key_name VARCHAR(64),
    api_key VARCHAR(512) NOT NULL,
    encrypted_api_key VARCHAR(512),
    priority INT DEFAULT 100,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    last_used_at TIMESTAMP,
    expires_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_at TIMESTAMP,
    updated_by BIGINT,
    CONSTRAINT fk_provider_api_keys_provider FOREIGN KEY (provider_id) REFERENCES providers(id)
);

CREATE INDEX idx_provider_api_keys_provider_id ON provider_api_keys(provider_id);

-- ============================================
-- 模型表
-- ============================================
CREATE TABLE models (
    id BIGSERIAL PRIMARY KEY,
    provider_id BIGINT NOT NULL,
    provider_model_id VARCHAR(128) NOT NULL,
    display_name VARCHAR(256),
    context_window INT,
    input_price DECIMAL(10, 6),
    output_price DECIMAL(10, 6),
    capabilities JSON,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    deleted_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_at TIMESTAMP,
    updated_by BIGINT,
    CONSTRAINT fk_models_provider FOREIGN KEY (provider_id) REFERENCES providers(id),
    CONSTRAINT uk_models_provider_model UNIQUE (provider_id, provider_model_id)
);

CREATE INDEX idx_models_provider ON models(provider_id);
CREATE INDEX idx_models_status ON models(status);

-- ============================================
-- 路由分组表
-- ============================================
CREATE TABLE route_groups (
    id BIGSERIAL PRIMARY KEY,
    group_name VARCHAR(128) NOT NULL,
    strategy VARCHAR(32) NOT NULL,
    enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_at TIMESTAMP,
    updated_by BIGINT
);

CREATE INDEX idx_route_groups_name ON route_groups(group_name);

-- ============================================
-- 路由分组与提供商关联表
-- ============================================
CREATE TABLE route_group_providers (
    id BIGSERIAL PRIMARY KEY,
    route_group_id BIGINT NOT NULL,
    provider_id BIGINT NOT NULL,
    weight INT,
    priority INT,
    enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_at TIMESTAMP,
    updated_by BIGINT,
    CONSTRAINT fk_route_group_providers_group FOREIGN KEY (route_group_id) REFERENCES route_groups(id),
    CONSTRAINT fk_route_group_providers_provider FOREIGN KEY (provider_id) REFERENCES providers(id)
);

CREATE INDEX idx_route_group_providers_group ON route_group_providers(route_group_id);
CREATE INDEX idx_route_group_providers_provider ON route_group_providers(provider_id);

-- ============================================
-- Token 限额表
-- ============================================
CREATE TABLE token_limits (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    provider_id BIGINT,
    model_id BIGINT,
    limit_type VARCHAR(32) NOT NULL DEFAULT 'USER_CUSTOM',
    max_tokens DECIMAL(20, 6),
    used_tokens DECIMAL(20, 6) DEFAULT 0,
    period_type VARCHAR(32) NOT NULL DEFAULT 'MONTHLY',
    period_day_of_week INT,
    period_day_of_month INT,
    exceeded_action VARCHAR(32) NOT NULL DEFAULT 'REJECT',
    switch_model_id BIGINT,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    deleted_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_at TIMESTAMP,
    updated_by BIGINT,
    CONSTRAINT uk_token_limits_user_provider_model UNIQUE (user_id, provider_id, model_id)
);

CREATE INDEX idx_token_limits_user ON token_limits(user_id);
CREATE INDEX idx_token_limits_provider ON token_limits(provider_id);
CREATE INDEX idx_token_limits_model ON token_limits(model_id);

-- ============================================
-- 限流配置表
-- ============================================
CREATE TABLE rate_limit_configs (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(128),
    requests_per_minute INT,
    bucket_size INT,
    refill_rate INT,
    enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_at TIMESTAMP,
    updated_by BIGINT
);

CREATE INDEX idx_rate_limit_configs_name ON rate_limit_configs(name);

-- ============================================
-- 审计日志表
-- ============================================
CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    action VARCHAR(64) NOT NULL,
    resource VARCHAR(256),
    result VARCHAR(64),
    ip_address VARCHAR(64),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_at TIMESTAMP,
    updated_by BIGINT
);

CREATE INDEX idx_audit_logs_user ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_action ON audit_logs(action);
CREATE INDEX idx_audit_logs_created ON audit_logs(created_at);

-- ============================================
-- 使用记录表
-- ============================================
CREATE TABLE usage_logs (
    id BIGSERIAL PRIMARY KEY,
    gateway_api_key_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    provider_id BIGINT NOT NULL,
    model_id BIGINT NOT NULL,
    request_id VARCHAR(64),
    input_tokens INT,
    output_tokens INT,
    total_tokens INT,
    latency_ms INT,
    status_code VARCHAR(32),
    error_message TEXT,
    failover BOOLEAN DEFAULT FALSE,
    api_format VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_at TIMESTAMP,
    updated_by BIGINT,
    CONSTRAINT fk_usage_logs_api_key FOREIGN KEY (gateway_api_key_id) REFERENCES gateway_api_keys(id),
    CONSTRAINT fk_usage_logs_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_usage_logs_provider FOREIGN KEY (provider_id) REFERENCES providers(id),
    CONSTRAINT fk_usage_logs_model FOREIGN KEY (model_id) REFERENCES models(id)
);

CREATE INDEX idx_usage_user_created ON usage_logs(user_id, created_at);
CREATE INDEX idx_usage_provider_created ON usage_logs(provider_id, created_at);
CREATE INDEX idx_usage_key_created ON usage_logs(gateway_api_key_id, created_at);

-- ============================================
-- 预警规则表
-- ============================================
CREATE TABLE alert_rules (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    alert_type VARCHAR(32) NOT NULL,
    target_type VARCHAR(32) NOT NULL,
    target_id BIGINT,
    condition_type VARCHAR(32),
    threshold_value DECIMAL(20, 6),
    period_type VARCHAR(32),
    notification_channels JSON,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    deleted_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_at TIMESTAMP,
    updated_by BIGINT
);

CREATE INDEX idx_alert_rules_type ON alert_rules(alert_type);
CREATE INDEX idx_alert_rules_active ON alert_rules(is_active);

-- ============================================
-- 预警通知表
-- ============================================
CREATE TABLE alert_notifications (
    id BIGSERIAL PRIMARY KEY,
    alert_rule_id BIGINT NOT NULL,
    target_user_id BIGINT NOT NULL,
    channel VARCHAR(32) NOT NULL,
    title VARCHAR(256),
    content TEXT,
    alert_data JSON,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    sent_at TIMESTAMP,
    deleted_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_at TIMESTAMP,
    updated_by BIGINT,
    CONSTRAINT fk_alert_notifications_rule FOREIGN KEY (alert_rule_id) REFERENCES alert_rules(id),
    CONSTRAINT fk_alert_notifications_user FOREIGN KEY (target_user_id) REFERENCES users(id)
);

CREATE INDEX idx_alert_notifications_rule ON alert_notifications(alert_rule_id);
CREATE INDEX idx_alert_notifications_user ON alert_notifications(target_user_id);
CREATE INDEX idx_alert_notifications_status ON alert_notifications(status);

-- ============================================
-- IP 黑名单表
-- ============================================
CREATE TABLE ip_blocklist (
    id BIGSERIAL PRIMARY KEY,
    ip_address VARCHAR(64) NOT NULL,
    block_reason VARCHAR(256),
    blocked_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP,
    blocked_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_at TIMESTAMP,
    updated_by BIGINT
);

CREATE INDEX idx_ip_blocklist_ip ON ip_blocklist(ip_address);
CREATE INDEX idx_ip_blocklist_expires ON ip_blocklist(expires_at);

-- ============================================
-- 敏感数据规则表
-- ============================================
CREATE TABLE sensitive_data_rules (
    id BIGSERIAL PRIMARY KEY,
    rule_name VARCHAR(128) NOT NULL,
    data_type VARCHAR(64) NOT NULL,
    regex_pattern VARCHAR(512) NOT NULL,
    mask_format VARCHAR(128),
    enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_at TIMESTAMP,
    updated_by BIGINT
);

CREATE INDEX idx_sensitive_data_rules_type ON sensitive_data_rules(data_type);
CREATE INDEX idx_sensitive_data_rules_enabled ON sensitive_data_rules(enabled);
