-- ===================================================================
-- LLM-Gateway 初始数据库结构
-- 版本: V1
-- 描述: 创建核心表结构
-- ===================================================================

-- -------------------------------------------------------------------
-- 1. 团队表 (teams)
-- -------------------------------------------------------------------
CREATE TABLE teams (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    team_code VARCHAR(64) NOT NULL UNIQUE,
    team_name VARCHAR(128) NOT NULL,
    description TEXT,
    admin_id BIGINT,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_by BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_at TIMESTAMP,
    deleted_by BIGINT,
    deleted_at TIMESTAMP,
    version INT NOT NULL DEFAULT 0,

    CONSTRAINT chk_teams_status CHECK (status IN ('ACTIVE', 'SUSPENDED', 'DELETED'))
) COMMENT '团队表';

CREATE INDEX idx_teams_team_code ON teams(team_code);
CREATE INDEX idx_teams_status ON teams(status);

-- -------------------------------------------------------------------
-- 2. 用户表 (users)
-- -------------------------------------------------------------------
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_code VARCHAR(64) NOT NULL UNIQUE,
    username VARCHAR(64) NOT NULL,
    email VARCHAR(128) NOT NULL UNIQUE,
    password_hash VARCHAR(256),
    phone VARCHAR(32),
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    oauth_providers JSON,
    pii_salt VARCHAR(64),
    last_login_at TIMESTAMP,
    created_by BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_at TIMESTAMP,
    deleted_by BIGINT,
    deleted_at TIMESTAMP,
    version INT NOT NULL DEFAULT 0,

    CONSTRAINT chk_users_status CHECK (status IN ('ACTIVE', 'DISABLED', 'LOCKED', 'DELETED'))
) COMMENT '用户表';

CREATE INDEX idx_users_user_code ON users(user_code);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_status ON users(status);

-- -------------------------------------------------------------------
-- 3. 成员表 (members)
-- -------------------------------------------------------------------
CREATE TABLE members (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    team_id BIGINT NOT NULL,
    team_role VARCHAR(32) NOT NULL DEFAULT 'MEMBER',
    joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_at TIMESTAMP,
    deleted_by BIGINT,
    deleted_at TIMESTAMP,
    version INT NOT NULL DEFAULT 0,

    CONSTRAINT chk_members_team_role CHECK (team_role IN ('ADMIN', 'MEMBER'))
) COMMENT '成员表 (用户-团队关联)';

CREATE UNIQUE INDEX idx_members_user_team ON members(user_id, team_id);
CREATE INDEX idx_members_team_id ON members(team_id);

-- -------------------------------------------------------------------
-- 4. 角色表 (roles)
-- -------------------------------------------------------------------
CREATE TABLE roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_code VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(64) NOT NULL,
    role_type VARCHAR(32) NOT NULL DEFAULT 'CUSTOM',
    scope_type VARCHAR(32) NOT NULL DEFAULT 'SYSTEM',
    scope_id BIGINT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_at TIMESTAMP,
    deleted_by BIGINT,
    deleted_at TIMESTAMP,
    version INT NOT NULL DEFAULT 0,

    CONSTRAINT chk_roles_type CHECK (role_type IN ('SYSTEM', 'CUSTOM')),
    CONSTRAINT chk_roles_scope CHECK (scope_type IN ('SYSTEM', 'TEAM'))
) COMMENT '角色表';

CREATE INDEX idx_roles_role_code ON roles(role_code);
CREATE INDEX idx_roles_scope ON roles(scope_type, scope_id);

-- -------------------------------------------------------------------
-- 5. 权限表 (permissions)
-- -------------------------------------------------------------------
CREATE TABLE permissions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    permission_code VARCHAR(128) NOT NULL UNIQUE,
    name VARCHAR(64) NOT NULL,
    description VARCHAR(256),
    resource_type VARCHAR(64) NOT NULL,
    created_by BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_at TIMESTAMP,
    deleted_by BIGINT,
    deleted_at TIMESTAMP,
    version INT NOT NULL DEFAULT 0
) COMMENT '权限表';

CREATE INDEX idx_permissions_code ON permissions(permission_code);
CREATE INDEX idx_permissions_resource ON permissions(resource_type);

-- -------------------------------------------------------------------
-- 6. 角色-权限关联表 (role_permissions)
-- -------------------------------------------------------------------
CREATE TABLE role_permissions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    created_by BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    UNIQUE KEY uk_role_permission (role_id, permission_id)
) COMMENT '角色-权限关联表';

CREATE INDEX idx_role_perms_role ON role_permissions(role_id);

-- -------------------------------------------------------------------
-- 7. 用户-角色关联表 (user_roles)
-- -------------------------------------------------------------------
CREATE TABLE user_roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    team_id BIGINT,
    created_by BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    UNIQUE KEY uk_user_role_team (user_id, role_id, team_id)
) COMMENT '用户-角色关联表';

CREATE INDEX idx_user_roles_user ON user_roles(user_id);
CREATE INDEX idx_user_roles_role ON user_roles(role_id);

-- -------------------------------------------------------------------
-- 8. 模型供应商表 (model_providers)
-- -------------------------------------------------------------------
CREATE TABLE model_providers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    provider_code VARCHAR(64) NOT NULL UNIQUE,
    provider_name VARCHAR(128) NOT NULL,
    website_url VARCHAR(512),
    api_doc_url VARCHAR(512),
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_by BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_at TIMESTAMP,
    deleted_by BIGINT,
    deleted_at TIMESTAMP,
    version INT NOT NULL DEFAULT 0,

    CONSTRAINT chk_providers_status CHECK (status IN ('ACTIVE', 'DISABLED'))
) COMMENT '模型供应商表';

CREATE INDEX idx_providers_code ON model_providers(provider_code);

-- -------------------------------------------------------------------
-- 9. 模型表 (models)
-- -------------------------------------------------------------------
CREATE TABLE models (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    model_code VARCHAR(128) NOT NULL UNIQUE,
    model_name VARCHAR(128) NOT NULL,
    provider_id BIGINT NOT NULL,
    context_length INT,
    capabilities JSON,
    input_price DECIMAL(10, 6),
    output_price DECIMAL(10, 6),
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_by BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_at TIMESTAMP,
    deleted_by BIGINT,
    deleted_at TIMESTAMP,
    version INT NOT NULL DEFAULT 0
) COMMENT '模型表';

CREATE INDEX idx_models_code ON models(model_code);
CREATE INDEX idx_models_provider ON models(provider_id);
CREATE INDEX idx_models_status ON models(status);

-- -------------------------------------------------------------------
-- 10. 渠道分组表 (channel_groups)
-- -------------------------------------------------------------------
CREATE TABLE channel_groups (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_code VARCHAR(64) NOT NULL UNIQUE,
    group_name VARCHAR(128) NOT NULL,
    team_id BIGINT,
    description TEXT,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_by BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_at TIMESTAMP,
    deleted_by BIGINT,
    deleted_at TIMESTAMP,
    version INT NOT NULL DEFAULT 0,

    CONSTRAINT chk_groups_status CHECK (status IN ('ACTIVE', 'DISABLED'))
) COMMENT '渠道分组表';

CREATE INDEX idx_groups_code ON channel_groups(group_code);
CREATE INDEX idx_groups_team ON channel_groups(team_id);

-- -------------------------------------------------------------------
-- 11. 渠道表 (channels)
-- -------------------------------------------------------------------
CREATE TABLE channels (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    channel_code VARCHAR(64) NOT NULL UNIQUE,
    channel_name VARCHAR(128) NOT NULL,
    team_id BIGINT NOT NULL,
    provider_id BIGINT NOT NULL,
    group_id BIGINT,
    models JSON,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    priority INT NOT NULL DEFAULT 100,
    timeout_seconds INT NOT NULL DEFAULT 30,
    retry_count INT NOT NULL DEFAULT 3,
    rpm_limit INT,
    tpm_limit INT,
    base_url VARCHAR(512),
    created_by BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_at TIMESTAMP,
    deleted_by BIGINT,
    deleted_at TIMESTAMP,
    version INT NOT NULL DEFAULT 0,

    CONSTRAINT chk_channels_status CHECK (status IN ('ACTIVE', 'DISABLED', 'ERROR'))
) COMMENT '渠道表';

CREATE INDEX idx_channels_code ON channels(channel_code);
CREATE INDEX idx_channels_team ON channels(team_id);
CREATE INDEX idx_channels_provider ON channels(provider_id);
CREATE INDEX idx_channels_group ON channels(group_id);
CREATE INDEX idx_channels_status ON channels(status);
CREATE INDEX idx_channels_priority ON channels(team_id, priority);

-- -------------------------------------------------------------------
-- 12. 渠道密钥表 (channel_keys)
-- -------------------------------------------------------------------
CREATE TABLE channel_keys (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    channel_id BIGINT NOT NULL,
    key_alias VARCHAR(128),
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    rpm_limit INT,
    tpm_limit INT,
    expires_at TIMESTAMP,
    last_used_at TIMESTAMP,
    error_count INT NOT NULL DEFAULT 0,
    created_by BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_at TIMESTAMP,
    deleted_by BIGINT,
    deleted_at TIMESTAMP,
    version INT NOT NULL DEFAULT 0,

    CONSTRAINT chk_keys_status CHECK (status IN ('ACTIVE', 'DISABLED', 'EXPIRED', 'ERROR'))
) COMMENT '渠道密钥表 (单渠道多 Key)';

CREATE INDEX idx_keys_channel ON channel_keys(channel_id);
CREATE INDEX idx_keys_status ON channel_keys(status);

-- -------------------------------------------------------------------
-- 13. API 密钥表 (api_keys)
-- -------------------------------------------------------------------
CREATE TABLE api_keys (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    key_code VARCHAR(128) NOT NULL UNIQUE,
    key_hash VARCHAR(256) NOT NULL,
    user_id BIGINT NOT NULL,
    team_id BIGINT NOT NULL,
    key_status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    quota DECIMAL(20, 6) NOT NULL DEFAULT 0,
    used_quota DECIMAL(20, 6) NOT NULL DEFAULT 0,
    expires_at TIMESTAMP,
    last_used_at TIMESTAMP,
    model_whitelist JSON,
    ip_whitelist JSON,
    created_by BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_at TIMESTAMP,
    deleted_by BIGINT,
    deleted_at TIMESTAMP,
    version INT NOT NULL DEFAULT 0,

    CONSTRAINT chk_api_keys_status CHECK (key_status IN ('ACTIVE', 'SUSPENDED', 'EXPIRED', 'DELETED'))
) COMMENT 'API 密钥表';

CREATE INDEX idx_api_keys_code ON api_keys(key_code);
CREATE INDEX idx_api_keys_user ON api_keys(user_id);
CREATE INDEX idx_api_keys_team ON api_keys(team_id);
CREATE INDEX idx_api_keys_status ON api_keys(key_status);

-- -------------------------------------------------------------------
-- 14. 密钥加密存储表 (api_key_secrets)
-- -------------------------------------------------------------------
CREATE TABLE api_key_secrets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    channel_key_id BIGINT NOT NULL,
    secret_key VARCHAR(512) NOT NULL,
    key_version INT NOT NULL DEFAULT 1,
    kek_version INT NOT NULL DEFAULT 1,
    rotated_at TIMESTAMP,
    expires_at TIMESTAMP,
    created_by BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_at TIMESTAMP,
    deleted_by BIGINT,
    deleted_at TIMESTAMP,
    version INT NOT NULL DEFAULT 0
) COMMENT '密钥加密存储表';

CREATE INDEX idx_secrets_key ON api_key_secrets(channel_key_id);

-- -------------------------------------------------------------------
-- 15. Token 限额表 (token_limits)
-- -------------------------------------------------------------------
CREATE TABLE token_limits (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    limit_code VARCHAR(64) NOT NULL UNIQUE,
    team_id BIGINT NOT NULL,
    scope_type VARCHAR(32) NOT NULL,
    scope_id BIGINT,
    user_id BIGINT,
    channel_id BIGINT,
    parent_limit_id BIGINT,
    token_limit_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    max_tokens DECIMAL(20, 6),
    period_type VARCHAR(32) NOT NULL DEFAULT 'TOTAL',
    period_day_of_week INT,
    period_day_of_month INT,
    request_limit_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    max_requests INT,
    exceeded_action VARCHAR(32),
    switch_model_id BIGINT,
    switch_channel_id BIGINT,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_by BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_at TIMESTAMP,
    deleted_by BIGINT,
    deleted_at TIMESTAMP,
    version INT NOT NULL DEFAULT 0,

    CONSTRAINT chk_limits_scope CHECK (scope_type IN ('TEAM', 'USER', 'API_KEY', 'USER_CHANNEL')),
    CONSTRAINT chk_limits_period CHECK (period_type IN ('DAILY', 'WEEKLY', 'MONTHLY', 'TOTAL')),
    CONSTRAINT chk_limits_action CHECK (exceeded_action IS NULL OR exceeded_action IN ('REJECT', 'DOWNGRADE'))
) COMMENT 'Token 限额表';

CREATE INDEX idx_limits_code ON token_limits(limit_code);
CREATE INDEX idx_limits_team ON token_limits(team_id);
CREATE INDEX idx_limits_scope ON token_limits(scope_type, scope_id);

-- -------------------------------------------------------------------
-- 16. Token 使用记录表 (token_usages)
-- -------------------------------------------------------------------
CREATE TABLE token_usages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    token_limit_id BIGINT NOT NULL,
    api_key_id BIGINT,
    user_id BIGINT,
    channel_id BIGINT,
    used_tokens DECIMAL(20, 6) NOT NULL,
    used_requests INT NOT NULL DEFAULT 1,
    period_start TIMESTAMP NOT NULL,
    period_end TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) COMMENT 'Token 使用记录表';

CREATE INDEX idx_usages_limit ON token_usages(token_limit_id);
CREATE INDEX idx_usages_period ON token_usages(period_start, period_end);
CREATE INDEX idx_usages_api_key ON token_usages(api_key_id);

-- -------------------------------------------------------------------
-- 17. Token 限额告警表 (token_limit_alerts)
-- -------------------------------------------------------------------
CREATE TABLE token_limit_alerts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    token_limit_id BIGINT NOT NULL,
    alert_type VARCHAR(32) NOT NULL,
    threshold_percent DECIMAL(5, 2) NOT NULL,
    current_percent DECIMAL(5, 2) NOT NULL,
    triggered_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    acknowledged BOOLEAN NOT NULL DEFAULT FALSE,
    acknowledged_at TIMESTAMP,
    acknowledged_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) COMMENT 'Token 限额告警表';

CREATE INDEX idx_alerts_limit ON token_limit_alerts(token_limit_id);
CREATE INDEX idx_alerts_triggered ON token_limit_alerts(triggered_at);

-- -------------------------------------------------------------------
-- 18. 调用日志表 (request_logs)
-- -------------------------------------------------------------------
CREATE TABLE request_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    request_id VARCHAR(64) NOT NULL UNIQUE,
    trace_id VARCHAR(64),
    api_key_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    team_id BIGINT NOT NULL,
    channel_id BIGINT,
    model VARCHAR(128) NOT NULL,
    provider_code VARCHAR(64),
    input_tokens INT,
    output_tokens INT,
    total_tokens INT,
    duration_ms INT,
    status_code INT NOT NULL,
    error_type VARCHAR(64),
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) COMMENT '调用日志表';

CREATE INDEX idx_logs_request_id ON request_logs(request_id);
CREATE INDEX idx_logs_trace_id ON request_logs(trace_id);
CREATE INDEX idx_logs_api_key ON request_logs(api_key_id);
CREATE INDEX idx_logs_team ON request_logs(team_id);
CREATE INDEX idx_logs_created ON request_logs(created_at);
CREATE INDEX idx_logs_model ON request_logs(model);

-- -------------------------------------------------------------------
-- 19. 请求体日志表 (request_body_logs)
-- -------------------------------------------------------------------
CREATE TABLE request_body_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    request_log_id BIGINT NOT NULL UNIQUE,
    request_body JSON,
    response_body JSON,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) COMMENT '请求体日志表';

CREATE INDEX idx_body_logs_request ON request_body_logs(request_log_id);

-- -------------------------------------------------------------------
-- 20. 审计日志表 (audit_logs)
-- -------------------------------------------------------------------
CREATE TABLE audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    audit_code VARCHAR(64) NOT NULL UNIQUE,
    trace_id VARCHAR(64),
    user_id BIGINT NOT NULL,
    team_id BIGINT,
    action VARCHAR(128) NOT NULL,
    resource_type VARCHAR(64) NOT NULL,
    resource_id VARCHAR(64),
    details JSON,
    ip_address VARCHAR(64),
    user_agent VARCHAR(256),
    previous_hash VARCHAR(64),
    current_hash VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_audit_previous_hash CHECK (
        (previous_hash IS NULL AND id = 1) OR (previous_hash IS NOT NULL)
    )
) COMMENT '审计日志表 (WORM 存储)';

CREATE INDEX idx_audit_code ON audit_logs(audit_code);
CREATE INDEX idx_audit_user ON audit_logs(user_id);
CREATE INDEX idx_audit_team ON audit_logs(team_id);
CREATE INDEX idx_audit_action ON audit_logs(action);
CREATE INDEX idx_audit_created ON audit_logs(created_at);

-- -------------------------------------------------------------------
-- 21. 渠道健康日志表 (channel_health_logs)
-- -------------------------------------------------------------------
CREATE TABLE channel_health_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    channel_id BIGINT NOT NULL,
    check_type VARCHAR(32) NOT NULL,
    is_healthy BOOLEAN NOT NULL,
    latency_ms INT,
    error_message TEXT,
    checked_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) COMMENT '渠道健康日志表';

CREATE INDEX idx_health_channel ON channel_health_logs(channel_id);
CREATE INDEX idx_health_checked ON channel_health_logs(checked_at);

-- -------------------------------------------------------------------
-- 22. 路由策略表 (strategies)
-- -------------------------------------------------------------------
CREATE TABLE strategies (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    strategy_code VARCHAR(128) NOT NULL UNIQUE,
    strategy_name VARCHAR(128) NOT NULL,
    team_id BIGINT NOT NULL,
    strategy_type VARCHAR(32) NOT NULL,
    config JSON,
    priority INT NOT NULL DEFAULT 100,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_at TIMESTAMP,
    deleted_by BIGINT,
    deleted_at TIMESTAMP,
    version INT NOT NULL DEFAULT 0
) COMMENT '路由策略表';

CREATE INDEX idx_strategies_code ON strategies(strategy_code);
CREATE INDEX idx_strategies_team ON strategies(team_id);
CREATE INDEX idx_strategies_active ON strategies(is_active);

-- -------------------------------------------------------------------
-- 23. 策略节点表 (strategy_nodes)
-- -------------------------------------------------------------------
CREATE TABLE strategy_nodes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    strategy_id BIGINT NOT NULL,
    node_type VARCHAR(32) NOT NULL,
    node_config JSON,
    parent_node_id BIGINT,
    position_x INT,
    position_y INT,
    created_by BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_at TIMESTAMP
) COMMENT '策略节点表';

CREATE INDEX idx_nodes_strategy ON strategy_nodes(strategy_id);
CREATE INDEX idx_nodes_parent ON strategy_nodes(parent_node_id);

-- -------------------------------------------------------------------
-- 提交
-- -------------------------------------------------------------------
COMMIT;
