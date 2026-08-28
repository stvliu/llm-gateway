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
-- ============================================
-- V70: 新建 system_settings 系统设置表
-- ============================================
-- 系统设置（gateway-settings 域）：键值对存储全局可配置项，
-- setting_key 为业务唯一键（如 audit.retention.days），
-- value_type 声明值类型（STRING/NUMBER/BOOLEAN 等），
-- is_editable 标记是否允许运行时修改。
--
-- 方言：H2（PostgreSQL 兼容模式）/ PostgreSQL 均支持
--       CREATE TABLE IF NOT EXISTS；id 风格沿用 V1 models 表的 BIGSERIAL PRIMARY KEY。
-- ============================================

CREATE TABLE IF NOT EXISTS system_settings (
    id BIGSERIAL PRIMARY KEY,
    setting_key VARCHAR(128) NOT NULL UNIQUE,
    setting_value TEXT,
    group_name VARCHAR(64),
    description VARCHAR(256),
    value_type VARCHAR(32) DEFAULT 'STRING',
    is_editable BOOLEAN DEFAULT TRUE,
    created_by BIGINT,
    created_at TIMESTAMP,
    updated_by BIGINT,
    updated_at TIMESTAMP
);
