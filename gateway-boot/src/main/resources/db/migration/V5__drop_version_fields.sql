--
-- Copyright © 2025-2026 codingas.com
--
-- Licensed under the Apache License, Version 2.0 (the "License");
-- you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at
--
--     http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.
--
-- V5__drop_version_fields.sql
-- 移除 version 字段以简化架构（去除乐观锁机制）

-- ============================================
-- 删除 version 索引
-- ============================================
DROP INDEX IF EXISTS idx_providers_version;
DROP INDEX IF EXISTS idx_models_version;
DROP INDEX IF EXISTS idx_provider_api_keys_version;

-- ============================================
-- 删除 version 列
-- ============================================
ALTER TABLE users DROP COLUMN IF EXISTS version;
ALTER TABLE gateway_api_keys DROP COLUMN IF EXISTS version;
ALTER TABLE providers DROP COLUMN IF EXISTS version;
ALTER TABLE provider_api_keys DROP COLUMN IF EXISTS version;
ALTER TABLE models DROP COLUMN IF EXISTS version;
ALTER TABLE route_groups DROP COLUMN IF EXISTS version;
ALTER TABLE route_group_providers DROP COLUMN IF EXISTS version;
ALTER TABLE token_limits DROP COLUMN IF EXISTS version;
ALTER TABLE rate_limit_configs DROP COLUMN IF EXISTS version;
ALTER TABLE audit_logs DROP COLUMN IF EXISTS version;
ALTER TABLE usage_logs DROP COLUMN IF EXISTS version;
ALTER TABLE alert_rules DROP COLUMN IF EXISTS version;
ALTER TABLE alert_notifications DROP COLUMN IF EXISTS version;
ALTER TABLE ip_blocklist DROP COLUMN IF EXISTS version;
ALTER TABLE sensitive_data_rules DROP COLUMN IF EXISTS version;
