--
-- Copyright (c) 2025 codingas.com
-- Licensed under the Apache License, Version 2.0.
-- See the LICENSE file for details.
--
-- 应用聚合根表：权限+行为双聚合，承载 Key 归属、渠道可见性、容灾画像
-- 预留配额/看板字段；application_channels 关联表由后续迁移创建
-- 说明：原计划版本号 V37，但仓库现有最大迁移为 V50（V37 已被 channel_endpoints 占用），
--       故顺延使用 V51 以避免 Flyway 版本冲突。
-- 方言：遵循 V17/V44 项目约定，使用 H2（PostgreSQL 兼容模式）/ PostgreSQL 语法：
--       BIGSERIAL 主键、NOW() 时间默认、CONSTRAINT ... UNIQUE 复合唯一约束。
CREATE TABLE applications (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(128) NOT NULL,
    description VARCHAR(512),
    state VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    resilience_profile_id BIGINT,
    quota_budget_id BIGINT,
    dashboard_id BIGINT,
    created_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_by BIGINT,
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_applications_code ON applications(code);

-- 应用-渠道授权关联表：决定应用可见的渠道集合
-- (application_id, channel_id) 组合唯一，防止重复授权
CREATE TABLE application_channels (
    id BIGSERIAL PRIMARY KEY,
    application_id BIGINT NOT NULL,
    channel_id BIGINT NOT NULL,
    created_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_by BIGINT,
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_app_channel UNIQUE (application_id, channel_id)
);
CREATE INDEX idx_app_channels_app ON application_channels(application_id);

-- Task 1.3: UserApiKey 权限锚点由团队改为应用，挂 application_id
ALTER TABLE user_api_keys ADD COLUMN application_id BIGINT;
CREATE INDEX idx_user_api_keys_app ON user_api_keys(application_id);
