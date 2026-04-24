-- V3__add_missing_columns.sql
-- 修复 V1 schema 与实体定义不匹配的问题
-- 添加缺失的列

-- ===================================================================
-- 1. 为 users 表添加缺失的列
-- ===================================================================
ALTER TABLE users ADD COLUMN user_code VARCHAR(64) UNIQUE;
ALTER TABLE users ADD COLUMN phone VARCHAR(32);
ALTER TABLE users ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE users ADD COLUMN oauth_providers JSON;
ALTER TABLE users ADD COLUMN pii_salt VARCHAR(64);
ALTER TABLE users ADD COLUMN last_login_at TIMESTAMP;
-- 软删除列
ALTER TABLE users ADD COLUMN deleted_by BIGINT;
ALTER TABLE users ADD COLUMN deleted_at TIMESTAMP;
-- 乐观锁版本列
ALTER TABLE users ADD COLUMN version INTEGER NOT NULL DEFAULT 0;

-- ===================================================================
-- 2. 为 providers 表添加软删除列和版本列
-- ===================================================================
ALTER TABLE providers ADD COLUMN deleted_by BIGINT;
ALTER TABLE providers ADD COLUMN deleted_at TIMESTAMP;
ALTER TABLE providers ADD COLUMN version INTEGER NOT NULL DEFAULT 0;

-- ===================================================================
-- 3. 为 provider_api_keys 表添加软删除列和版本列
-- ===================================================================
ALTER TABLE provider_api_keys ADD COLUMN deleted_by BIGINT;
ALTER TABLE provider_api_keys ADD COLUMN deleted_at TIMESTAMP;
ALTER TABLE provider_api_keys ADD COLUMN version INTEGER NOT NULL DEFAULT 0;

-- ===================================================================
-- 4. 为 route_groups 表添加软删除列和版本列
-- ===================================================================
ALTER TABLE route_groups ADD COLUMN deleted_by BIGINT;
ALTER TABLE route_groups ADD COLUMN deleted_at TIMESTAMP;
ALTER TABLE route_groups ADD COLUMN version INTEGER NOT NULL DEFAULT 0;

-- ===================================================================
-- 5. 为 route_group_providers 表添加软删除列和版本列
-- ===================================================================
ALTER TABLE route_group_providers ADD COLUMN deleted_by BIGINT;
ALTER TABLE route_group_providers ADD COLUMN deleted_at TIMESTAMP;
ALTER TABLE route_group_providers ADD COLUMN version INTEGER NOT NULL DEFAULT 0;

-- ===================================================================
-- 6. 为 models 表添加软删除列和版本列
-- ===================================================================
ALTER TABLE models ADD COLUMN deleted_by BIGINT;
ALTER TABLE models ADD COLUMN deleted_at TIMESTAMP;
ALTER TABLE models ADD COLUMN version INTEGER NOT NULL DEFAULT 0;

-- ===================================================================
-- 7. 为 gateway_api_keys 表添加缺失的 route_group_id 列和软删除列
-- ===================================================================
ALTER TABLE gateway_api_keys ADD COLUMN route_group_id BIGINT;
ALTER TABLE gateway_api_keys ADD COLUMN deleted_by BIGINT;
ALTER TABLE gateway_api_keys ADD COLUMN deleted_at TIMESTAMP;
ALTER TABLE gateway_api_keys ADD COLUMN version INTEGER NOT NULL DEFAULT 0;

-- ===================================================================
-- 8. 为 token_limits 表添加软删除列和版本列
-- ===================================================================
ALTER TABLE token_limits ADD COLUMN deleted_by BIGINT;
ALTER TABLE token_limits ADD COLUMN deleted_at TIMESTAMP;
ALTER TABLE token_limits ADD COLUMN version INTEGER NOT NULL DEFAULT 0;
