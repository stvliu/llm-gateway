-- V1__init_schema.sql
-- 初始数据库结构（H2 PostgreSQL 兼容模式）

-- ============================================
-- 模型提供商表
-- ============================================
CREATE TABLE model_providers (
    id BIGSERIAL PRIMARY KEY,
    provider_code VARCHAR(64) NOT NULL UNIQUE,
    provider_name VARCHAR(128) NOT NULL,
    provider_type VARCHAR(32) NOT NULL,
    api_endpoint VARCHAR(512),
    api_key_encrypted VARCHAR(512),
    is_active BOOLEAN DEFAULT TRUE,
    priority INT DEFAULT 100,
    config_json TEXT,
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_providers_code ON model_providers(provider_code);
CREATE INDEX idx_providers_active ON model_providers(is_active);

-- ============================================
-- API Key 表
-- ============================================
CREATE TABLE api_keys (
    id BIGSERIAL PRIMARY KEY,
    key_code VARCHAR(128) NOT NULL UNIQUE,
    key_prefix VARCHAR(32) NOT NULL,
    hashed_key VARCHAR(256) NOT NULL,
    user_id BIGINT NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    last_used_at TIMESTAMP,
    expires_at TIMESTAMP,
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_keys_code ON api_keys(key_code);
CREATE INDEX idx_keys_user ON api_keys(user_id);

-- ============================================
-- 路由策略表
-- ============================================
CREATE TABLE routing_strategies (
    id BIGSERIAL PRIMARY KEY,
    strategy_code VARCHAR(64) NOT NULL UNIQUE,
    strategy_name VARCHAR(128) NOT NULL,
    strategy_type VARCHAR(32) NOT NULL,
    priority INT DEFAULT 100,
    conditions_json TEXT,
    actions_json TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- 用户表
-- ============================================
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(64) NOT NULL UNIQUE,
    password_hash VARCHAR(256) NOT NULL,
    email VARCHAR(128),
    is_active BOOLEAN DEFAULT TRUE,
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_username ON users(username);

-- ============================================
-- Token 使用统计表
-- ============================================
CREATE TABLE token_usage (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    provider_code VARCHAR(64) NOT NULL,
    model_name VARCHAR(128),
    input_tokens INT DEFAULT 0,
    output_tokens INT DEFAULT 0,
    total_tokens INT DEFAULT 0,
    period_key VARCHAR(32) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_usage_user ON token_usage(user_id);
CREATE INDEX idx_usage_period ON token_usage(period_key);