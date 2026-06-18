-- 应用聚合根表：权限+行为双聚合，承载 Key 归属、渠道可见性、容灾画像
-- 预留配额/看板字段；application_channels 关联表由后续迁移创建
-- 说明：原计划版本号 V37，但仓库现有最大迁移为 V50（V37 已被 channel_endpoints 占用），
--       故顺延使用 V51 以避免 Flyway 版本冲突。
CREATE TABLE applications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(128) NOT NULL,
    description VARCHAR(512),
    state VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    resilience_profile_id BIGINT,
    quota_budget_id BIGINT,
    dashboard_id BIGINT,
    created_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_applications_code ON applications(code);
