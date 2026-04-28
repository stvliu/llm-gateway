-- V1__init_schema.sql
-- LLM Gateway 初始化数据库结构

-- ============================================
-- 1. 身份与访问控制域
-- ============================================

-- 用户表
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_code VARCHAR(64) NOT NULL UNIQUE COMMENT '用户编码',
    username VARCHAR(64) NOT NULL COMMENT '用户名',
    email VARCHAR(128) COMMENT '邮箱',
    password_hash VARCHAR(256) COMMENT '密码哈希',
    phone VARCHAR(32) COMMENT '手机号',
    avatar_url VARCHAR(512) COMMENT '头像URL',
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE/DISABLED/LOCKED/DELETED',
    email_verified BOOLEAN DEFAULT FALSE COMMENT '邮箱已验证',
    oauth_providers JSON COMMENT 'OAuth提供者列表',
    pii_salt VARCHAR(64) COMMENT 'PII脱敏盐值',
    last_login_at TIMESTAMP COMMENT '最后登录时间',
    created_by BIGINT COMMENT '创建人',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT COMMENT '更新人',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP COMMENT '删除时间',
    INDEX idx_users_email (email),
    INDEX idx_users_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 角色表
CREATE TABLE roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_code VARCHAR(64) NOT NULL UNIQUE COMMENT '角色编码',
    name VARCHAR(64) NOT NULL COMMENT '角色名称',
    description TEXT COMMENT '角色描述',
    role_type VARCHAR(32) NOT NULL DEFAULT 'CUSTOM' COMMENT '类型: SYSTEM/CUSTOM',
    is_active BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否启用',
    created_by BIGINT COMMENT '创建人',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT COMMENT '更新人',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP COMMENT '删除时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

-- 权限表
CREATE TABLE permissions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    permission_code VARCHAR(128) NOT NULL UNIQUE COMMENT '权限编码',
    name VARCHAR(64) NOT NULL COMMENT '权限名称',
    description TEXT COMMENT '权限描述',
    category VARCHAR(32) COMMENT '权限分类: user/provider/model/token/log/setting',
    created_by BIGINT COMMENT '创建人',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT COMMENT '更新人',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权限表';

-- 用户角色关联表
CREATE TABLE user_roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    created_by BIGINT COMMENT '创建人',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT COMMENT '更新人',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_role (user_id, role_id),
    INDEX idx_user_roles_user (user_id),
    INDEX idx_user_roles_role (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关联表';

-- ============================================
-- 2. 提供商与模型域
-- ============================================

-- 提供商表
CREATE TABLE providers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    provider_code VARCHAR(64) NOT NULL UNIQUE COMMENT '提供商编码',
    provider_name VARCHAR(128) NOT NULL COMMENT '提供商名称',
    provider_type VARCHAR(32) NOT NULL COMMENT '类型: OPENAI/ANTHROPIC/GEMINI/ZHIPU/QWEN/VOLCENGINE/WENXIN/OTHER',
    base_url VARCHAR(256) COMMENT 'API端点',
    website_url VARCHAR(512) COMMENT '官网URL',
    api_doc_url VARCHAR(512) COMMENT 'API文档URL',
    priority INT DEFAULT 100 COMMENT '优先级',
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE/SUSPENDED/DELETED',
    created_by BIGINT COMMENT '创建人',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT COMMENT '更新人',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP COMMENT '删除时间',
    INDEX idx_providers_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='提供商表';

-- 模型表
CREATE TABLE models (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    model_code VARCHAR(128) NOT NULL UNIQUE COMMENT '模型编码',
    provider_id BIGINT NOT NULL COMMENT '所属提供商ID',
    provider_model_id VARCHAR(128) NOT NULL COMMENT 'Provider侧模型ID',
    display_name VARCHAR(256) COMMENT '显示名称',
    context_window INT COMMENT '上下文窗口(Token数)',
    input_price DECIMAL(10,6) COMMENT '输入价格(每1M tokens)',
    output_price DECIMAL(10,6) COMMENT '输出价格(每1M tokens)',
    capabilities JSON COMMENT '能力标志',
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE/DEPRECATED/DELETED',
    created_by BIGINT COMMENT '创建人',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT COMMENT '更新人',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP COMMENT '删除时间',
    UNIQUE KEY uk_model_provider (provider_id, provider_model_id),
    INDEX idx_models_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='模型表';

-- Provider API Key表
CREATE TABLE provider_api_keys (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    key_code VARCHAR(64) NOT NULL UNIQUE COMMENT 'Key编码',
    provider_id BIGINT NOT NULL COMMENT '所属提供商ID',
    key_name VARCHAR(64) COMMENT 'Key名称',
    api_key VARCHAR(512) NOT NULL COMMENT 'API Key(加密存储)',
    priority INT DEFAULT 100 COMMENT '优先级',
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE/DISABLED/EXHAUSTED/EXPIRED',
    last_used_at TIMESTAMP COMMENT '最后使用时间',
    created_by BIGINT COMMENT '创建人',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT COMMENT '更新人',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP COMMENT '删除时间',
    INDEX idx_provider_api_keys_provider (provider_id),
    INDEX idx_provider_api_keys_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Provider API Key表';

-- ============================================
-- 3. 令牌与限额域
-- ============================================

-- 网关API Key表
CREATE TABLE gateway_api_keys (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    key_code VARCHAR(128) NOT NULL UNIQUE COMMENT 'Key编码',
    key_hash VARCHAR(256) NOT NULL COMMENT 'Key哈希',
    user_id BIGINT NOT NULL COMMENT '所属用户ID',
    name VARCHAR(64) COMMENT '密钥名称',
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE/DISABLED/EXPIRED/DELETED',
    expires_at TIMESTAMP COMMENT '过期时间',
    last_used_at TIMESTAMP COMMENT '最后使用时间',
    ip_whitelist JSON COMMENT 'IP白名单',
    created_by BIGINT COMMENT '创建人',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT COMMENT '更新人',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP COMMENT '删除时间',
    INDEX idx_gateway_api_keys_user (user_id),
    INDEX idx_gateway_api_keys_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='网关API Key表';

-- Token限额表
CREATE TABLE token_limits (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    limit_code VARCHAR(64) NOT NULL UNIQUE COMMENT '限额编码',
    user_id BIGINT NOT NULL COMMENT '所属用户ID',
    provider_id BIGINT COMMENT '关联Provider',
    model_id BIGINT COMMENT '关联Model',
    limit_type VARCHAR(32) NOT NULL DEFAULT 'USER_CUSTOM' COMMENT '类型: SYSTEM_DEFAULT/USER_CUSTOM',
    max_tokens DECIMAL(20,6) COMMENT 'Token限额总量',
    used_tokens DECIMAL(20,6) DEFAULT 0 COMMENT '已用Token量',
    period_type VARCHAR(32) NOT NULL DEFAULT 'MONTHLY' COMMENT '周期: DAILY/WEEKLY/MONTHLY/TOTAL',
    period_day_of_week INT COMMENT '周内日期(1-7)',
    period_day_of_month INT COMMENT '月内日期(1-31)',
    exceeded_action VARCHAR(32) NOT NULL DEFAULT 'REJECT' COMMENT '超限动作: REJECT/DOWNGRADE',
    switch_model_id BIGINT COMMENT '降级切换Model',
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE/SUSPENDED/DELETED',
    created_by BIGINT COMMENT '创建人',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT COMMENT '更新人',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP COMMENT '删除时间',
    UNIQUE KEY uk_token_limit (user_id, provider_id, model_id),
    INDEX idx_token_limits_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Token限额表';

-- ============================================
-- 4. 计量与分析域
-- ============================================

-- 使用记录表
CREATE TABLE usage_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    log_code VARCHAR(64) NOT NULL UNIQUE COMMENT '记录编码',
    gateway_api_key_id BIGINT NOT NULL COMMENT '使用的API Key',
    user_id BIGINT NOT NULL COMMENT '所属用户',
    provider_id BIGINT NOT NULL COMMENT '调用的Provider',
    model_id BIGINT NOT NULL COMMENT '使用的Model',
    request_id VARCHAR(64) COMMENT '请求追踪ID',
    input_tokens INT COMMENT '输入Token数',
    output_tokens INT COMMENT '输出Token数',
    total_tokens INT COMMENT '总Token数',
    latency_ms INT COMMENT '响应延迟(毫秒)',
    status_code VARCHAR(32) COMMENT '响应状态码',
    error_message TEXT COMMENT '错误信息',
    failover BOOLEAN DEFAULT FALSE COMMENT '是否发生failover',
    api_format VARCHAR(32) NOT NULL COMMENT 'API格式: OPENAI/ANTHROPIC',
    created_by BIGINT COMMENT '创建人',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT COMMENT '更新人',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_usage_user_created (user_id, created_at),
    INDEX idx_usage_provider_created (provider_id, created_at),
    INDEX idx_usage_key_created (gateway_api_key_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='使用记录表';

-- 预警规则表
CREATE TABLE alert_rules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rule_code VARCHAR(64) NOT NULL UNIQUE COMMENT '规则编码',
    name VARCHAR(128) NOT NULL COMMENT '规则名称',
    alert_type VARCHAR(32) NOT NULL COMMENT '预警类型: USAGE/HEALTH/QUOTA',
    target_type VARCHAR(32) NOT NULL COMMENT '目标类型: USER/PROVIDER/API_KEY',
    target_id BIGINT COMMENT '目标ID',
    condition_type VARCHAR(32) COMMENT '条件类型: THRESHOLD/RATIO/TREND',
    threshold_value DECIMAL(20,6) COMMENT '阈值',
    period_type VARCHAR(32) COMMENT '周期: DAILY/WEEKLY/MONTHLY/TOTAL',
    notification_channels JSON COMMENT '通知渠道',
    is_active BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否启用',
    created_by BIGINT COMMENT '创建人',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT COMMENT '更新人',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP COMMENT '删除时间',
    INDEX idx_alert_rules_type (alert_type),
    INDEX idx_alert_rules_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='预警规则表';

-- 预警通知表
CREATE TABLE alert_notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    notification_code VARCHAR(64) NOT NULL UNIQUE COMMENT '通知编码',
    alert_rule_id BIGINT NOT NULL COMMENT '关联预警规则',
    target_user_id BIGINT NOT NULL COMMENT '通知目标用户',
    channel VARCHAR(32) NOT NULL COMMENT '通知渠道: SYSTEM/EMAIL/IM/SMS',
    title VARCHAR(256) COMMENT '通知标题',
    content TEXT COMMENT '通知内容',
    alert_data JSON COMMENT '预警数据',
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING/SENT/FAILED',
    sent_at TIMESTAMP COMMENT '发送时间',
    created_by BIGINT COMMENT '创建人',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT COMMENT '更新人',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP COMMENT '删除时间',
    INDEX idx_alert_notifications_rule (alert_rule_id),
    INDEX idx_alert_notifications_user (target_user_id),
    INDEX idx_alert_notifications_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='预警通知表';

-- ============================================
-- 5. 审计日志表
-- ============================================

CREATE TABLE audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    audit_code VARCHAR(64) NOT NULL UNIQUE COMMENT '审计编码',
    user_id BIGINT COMMENT '操作用户',
    username VARCHAR(64) COMMENT '操作人用户名',
    operation_type VARCHAR(32) NOT NULL COMMENT '操作类型: CREATE/UPDATE/DELETE',
    target_type VARCHAR(64) NOT NULL COMMENT '操作对象类型',
    target_id BIGINT COMMENT '操作对象ID',
    target_code VARCHAR(128) COMMENT '操作对象编码',
    detail JSON COMMENT '操作详情',
    ip_address VARCHAR(64) COMMENT 'IP地址',
    created_by BIGINT COMMENT '创建人',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT COMMENT '更新人',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_audit_logs_user (user_id),
    INDEX idx_audit_logs_target (target_type, target_id),
    INDEX idx_audit_logs_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审计日志表';