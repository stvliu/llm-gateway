-- V1__init_schema.sql
-- 初始化数据库 schema (MySQL 兼容)

-- ===================================================================
-- 1. users - 用户表
-- ===================================================================
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    user_code VARCHAR(64) NOT NULL,
    username VARCHAR(64) NOT NULL UNIQUE,
    email VARCHAR(256) NOT NULL UNIQUE,
    password_hash VARCHAR(256),
    phone VARCHAR(32),
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    role VARCHAR(32) NOT NULL DEFAULT 'USER',
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    oauth_providers VARCHAR(1024),
    pii_salt VARCHAR(64),
    last_login_at TIMESTAMP,
    created_by BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_by BIGINT,
    deleted_at TIMESTAMP,
    version INT DEFAULT 0,
    CONSTRAINT uk_user_code UNIQUE (user_code)
);

CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_status ON users(status);

-- ===================================================================
-- 2. providers - 提供商表
-- ===================================================================
CREATE TABLE providers (
    id BIGSERIAL PRIMARY KEY,
    provider_code VARCHAR(64) NOT NULL UNIQUE,
    provider_name VARCHAR(128) NOT NULL,
    provider_type VARCHAR(32) NOT NULL,
    base_url VARCHAR(256),
    website_url VARCHAR(512),
    api_doc_url VARCHAR(512),
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    priority INTEGER DEFAULT 100,
    created_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_by BIGINT,
    deleted_at TIMESTAMP,
    version INT DEFAULT 0
);

CREATE INDEX idx_providers_status ON providers(status);
CREATE INDEX idx_providers_provider_type ON providers(provider_type);

-- ===================================================================
-- 3. provider_api_keys - Provider 调用凭证表
-- ===================================================================
CREATE TABLE provider_api_keys (
    id BIGSERIAL PRIMARY KEY,
    key_code VARCHAR(64) NOT NULL UNIQUE,
    provider_id BIGINT NOT NULL,
    key_name VARCHAR(64),
    api_key VARCHAR(512) NOT NULL,
    encrypted_api_key VARCHAR(512),
    priority INTEGER DEFAULT 100,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    last_used_at TIMESTAMP,
    expires_at TIMESTAMP,
    created_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_by BIGINT,
    deleted_at TIMESTAMP,
    version INT DEFAULT 0,
    CONSTRAINT fk_provider_api_keys_provider FOREIGN KEY (provider_id) REFERENCES providers(id)
);

CREATE INDEX idx_provider_api_keys_provider_id ON provider_api_keys(provider_id);
CREATE INDEX idx_provider_api_keys_status ON provider_api_keys(status);

-- ===================================================================
-- 4. route_groups - 路由分组表
-- ===================================================================
CREATE TABLE route_groups (
    id BIGSERIAL PRIMARY KEY,
    group_code VARCHAR(64) NOT NULL UNIQUE,
    group_name VARCHAR(128) NOT NULL,
    strategy VARCHAR(32) NOT NULL DEFAULT 'PRIORITY',
    failover_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    max_retry INTEGER DEFAULT 2,
    health_check_interval INTEGER DEFAULT 30,
    description VARCHAR(1024),
    created_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_by BIGINT,
    deleted_at TIMESTAMP,
    version INT DEFAULT 0
);

CREATE INDEX idx_route_groups_group_code ON route_groups(group_code);

-- ===================================================================
-- 5. route_group_providers - 路由分组与Provider关联表
-- ===================================================================
CREATE TABLE route_group_providers (
    id BIGSERIAL PRIMARY KEY,
    route_group_id BIGINT NOT NULL,
    provider_id BIGINT NOT NULL,
    weight INTEGER DEFAULT 100,
    priority INTEGER DEFAULT 100,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    health_status VARCHAR(32) NOT NULL DEFAULT 'UNKNOWN',
    consecutive_failures INTEGER DEFAULT 0,
    last_health_check_at TIMESTAMP,
    created_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_by BIGINT,
    deleted_at TIMESTAMP,
    version INT DEFAULT 0,
    CONSTRAINT fk_route_group_providers_route_group FOREIGN KEY (route_group_id) REFERENCES route_groups(id),
    CONSTRAINT fk_route_group_providers_provider FOREIGN KEY (provider_id) REFERENCES providers(id)
);

CREATE INDEX idx_route_group_providers_route_group_id ON route_group_providers(route_group_id);
CREATE INDEX idx_route_group_providers_provider_id ON route_group_providers(provider_id);

-- ===================================================================
-- 6. models - 模型表
-- ===================================================================
CREATE TABLE models (
    id BIGSERIAL PRIMARY KEY,
    model_code VARCHAR(128) NOT NULL UNIQUE,
    display_name VARCHAR(256) NOT NULL,
    provider_id BIGINT NOT NULL,
    provider_model_id VARCHAR(128) NOT NULL,
    context_window INTEGER,
    input_price DECIMAL(10, 6),
    output_price DECIMAL(10, 6),
    capabilities JSONB,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_by BIGINT,
    deleted_at TIMESTAMP,
    version INT DEFAULT 0,
    CONSTRAINT fk_models_provider FOREIGN KEY (provider_id) REFERENCES providers(id)
);

CREATE INDEX idx_models_provider_id ON models(provider_id);
CREATE INDEX idx_models_status ON models(status);
CREATE INDEX idx_models_provider_model_id ON models(provider_id, provider_model_id);

-- ===================================================================
-- 7. gateway_api_keys - 网关访问凭证表
-- ===================================================================
CREATE TABLE gateway_api_keys (
    id BIGSERIAL PRIMARY KEY,
    key_code VARCHAR(128) NOT NULL UNIQUE,
    key_hash VARCHAR(256) NOT NULL,
    user_id BIGINT NOT NULL,
    provider_id BIGINT NOT NULL,
    route_group_id BIGINT,
    name VARCHAR(64),
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    expires_at TIMESTAMP,
    last_used_at TIMESTAMP,
    model_whitelist VARCHAR(1024),
    ip_whitelist VARCHAR(1024),
    created_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_by BIGINT,
    deleted_at TIMESTAMP,
    version INT DEFAULT 0,
    CONSTRAINT fk_gateway_api_keys_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_gateway_api_keys_provider FOREIGN KEY (provider_id) REFERENCES providers(id),
    CONSTRAINT fk_gateway_api_keys_route_group FOREIGN KEY (route_group_id) REFERENCES route_groups(id)
);

CREATE INDEX idx_gateway_api_keys_key_hash ON gateway_api_keys(key_hash);
CREATE INDEX idx_gateway_api_keys_user_provider ON gateway_api_keys(user_id, provider_id);

-- ===================================================================
-- 8. token_limits - Token 限额表
-- ===================================================================
CREATE TABLE token_limits (
    id BIGSERIAL PRIMARY KEY,
    limit_code VARCHAR(64) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    provider_id BIGINT,
    model_id BIGINT,
    token_limit_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    max_tokens DECIMAL(20, 6),
    used_tokens DECIMAL(20, 6) DEFAULT 0,
    period_type VARCHAR(32) NOT NULL DEFAULT 'TOTAL',
    period_day_of_week INTEGER,
    period_day_of_month INTEGER,
    request_limit_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    max_requests INTEGER,
    used_requests INTEGER DEFAULT 0,
    exceeded_action VARCHAR(32),
    switch_model_id BIGINT,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_by BIGINT,
    deleted_at TIMESTAMP,
    version INT DEFAULT 0,
    CONSTRAINT fk_token_limits_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_token_limits_provider FOREIGN KEY (provider_id) REFERENCES providers(id),
    CONSTRAINT fk_token_limits_model FOREIGN KEY (model_id) REFERENCES models(id)
);

CREATE INDEX idx_token_limits_user_id ON token_limits(user_id);
CREATE INDEX idx_token_limits_provider_id ON token_limits(provider_id);