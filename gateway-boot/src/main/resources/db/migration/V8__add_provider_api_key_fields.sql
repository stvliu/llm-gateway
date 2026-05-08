-- V8__add_provider_api_key_fields.sql
-- 为 provider_api_keys 表添加缺失的字段
-- 为 providers 表添加缺失的字段

-- provider_api_keys 表添加字段
ALTER TABLE provider_api_keys ADD COLUMN IF NOT EXISTS disabled_reason VARCHAR(32);
ALTER TABLE provider_api_keys ADD COLUMN IF NOT EXISTS weight INT DEFAULT 100;
ALTER TABLE provider_api_keys ADD COLUMN IF NOT EXISTS is_default BOOLEAN DEFAULT FALSE;
ALTER TABLE provider_api_keys ADD COLUMN IF NOT EXISTS rpm_limit INT;
ALTER TABLE provider_api_keys ADD COLUMN IF NOT EXISTS tpm_limit BIGINT;

-- providers 表添加字段
ALTER TABLE providers ADD COLUMN IF NOT EXISTS max_retries INT DEFAULT 3;
ALTER TABLE providers ADD COLUMN IF NOT EXISTS timeout INT DEFAULT 30000;