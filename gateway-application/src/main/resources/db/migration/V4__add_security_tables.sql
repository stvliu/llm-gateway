-- V4__add_security_tables.sql
-- 安全零信任功能：添加限流配置、审计日志、数据脱敏、IP封锁表

-- ===================================================================
-- 1. rate_limit_configs - 限流配置表
-- ===================================================================
CREATE TABLE rate_limit_configs (
    id BIGSERIAL PRIMARY KEY,
    config_code VARCHAR(64) NOT NULL,
    name VARCHAR(64) NOT NULL,
    requests_per_minute INT NOT NULL DEFAULT 1000,
    requests_per_hour INT,
    requests_per_day INT,
    bucket_size INT NOT NULL DEFAULT 100,
    refill_rate INT NOT NULL DEFAULT 10,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_by BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_at TIMESTAMP,
    deleted_by BIGINT,
    deleted_at TIMESTAMP,
    version INT DEFAULT 0,
    CONSTRAINT uk_rate_limit_config_code UNIQUE (config_code)
);

CREATE INDEX idx_rate_limit_config_enabled ON rate_limit_configs(enabled);

-- ===================================================================
-- 2. audit_logs - 审计日志表
-- ===================================================================
CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    action VARCHAR(64) NOT NULL,
    resource VARCHAR(128),
    request_method VARCHAR(16),
    request_path VARCHAR(256),
    request_body TEXT,
    response_status INT,
    response_time INT,
    trace_id VARCHAR(64),
    ip_address VARCHAR(64),
    user_agent VARCHAR(256),
    error_message VARCHAR(512),
    created_by BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_at TIMESTAMP,
    deleted_by BIGINT,
    deleted_at TIMESTAMP,
    version INT DEFAULT 0
);

CREATE INDEX idx_audit_log_user ON audit_logs(user_id);
CREATE INDEX idx_audit_log_created ON audit_logs(created_at);
CREATE INDEX idx_audit_log_trace ON audit_logs(trace_id);
CREATE INDEX idx_audit_log_action ON audit_logs(action);

-- ===================================================================
-- 3. sensitive_data_rules - 数据脱敏规则表
-- ===================================================================
CREATE TABLE sensitive_data_rules (
    id BIGSERIAL PRIMARY KEY,
    rule_code VARCHAR(64) NOT NULL,
    data_type VARCHAR(32) NOT NULL,
    pattern VARCHAR(256) NOT NULL,
    mask_format VARCHAR(64) NOT NULL,
    description VARCHAR(128),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    priority INT NOT NULL DEFAULT 0,
    created_by BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_at TIMESTAMP,
    deleted_by BIGINT,
    deleted_at TIMESTAMP,
    version INT DEFAULT 0,
    CONSTRAINT uk_sensitive_rule_code UNIQUE (rule_code)
);

CREATE INDEX idx_sensitive_rule_enabled ON sensitive_data_rules(enabled);
CREATE INDEX idx_sensitive_rule_type ON sensitive_data_rules(data_type);

-- ===================================================================
-- 4. ip_blocklist - IP封锁列表表
-- ===================================================================
CREATE TABLE ip_blocklist (
    id BIGSERIAL PRIMARY KEY,
    ip_address VARCHAR(64),
    ip_range_start VARCHAR(64),
    ip_range_end VARCHAR(64),
    block_type VARCHAR(32) NOT NULL,
    reason VARCHAR(256) NOT NULL,
    blocked_by BIGINT NOT NULL,
    blocked_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP,
    unblocked_by BIGINT,
    unblocked_at TIMESTAMP,
    created_by BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_at TIMESTAMP,
    deleted_by BIGINT,
    deleted_at TIMESTAMP,
    version INT DEFAULT 0
);

CREATE INDEX idx_ip_blocklist_address ON ip_blocklist(ip_address);
CREATE INDEX idx_ip_blocklist_blocked_at ON ip_blocklist(blocked_at);
CREATE INDEX idx_ip_blocklist_expires ON ip_blocklist(expires_at);

-- ===================================================================
-- 5. 添加外键约束
-- ===================================================================
ALTER TABLE audit_logs ADD CONSTRAINT fk_audit_log_user FOREIGN KEY (user_id) REFERENCES users(id);
ALTER TABLE ip_blocklist ADD CONSTRAINT fk_ip_blocklist_blocked_by FOREIGN KEY (blocked_by) REFERENCES users(id);
ALTER TABLE ip_blocklist ADD CONSTRAINT fk_ip_blocklist_unblocked_by FOREIGN KEY (unblocked_by) REFERENCES users(id);