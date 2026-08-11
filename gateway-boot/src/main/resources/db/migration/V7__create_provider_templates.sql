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
-- V7__create_provider_templates.sql
-- Provider 模板表 - 用于存储预配置的 Provider 配置模板

CREATE TABLE provider_templates (
    id BIGSERIAL PRIMARY KEY,
    template_code VARCHAR(64) NOT NULL,
    template_name VARCHAR(128) NOT NULL,
    template_type VARCHAR(32) NOT NULL,
    provider_type VARCHAR(32) NOT NULL,
    provider_config JSON NOT NULL,
    models_config JSON NOT NULL,
    author_id BIGINT,
    author_name VARCHAR(64),
    market_status VARCHAR(32) NOT NULL DEFAULT 'PRIVATE',
    publish_at TIMESTAMP WITH TIME ZONE,
    download_count INT NOT NULL DEFAULT 0,
    tags JSON,
    description TEXT,
    icon_url VARCHAR(512),
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    deleted_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_at TIMESTAMP WITH TIME ZONE,
    updated_by BIGINT
);

-- 唯一索引
CREATE UNIQUE INDEX uk_provider_templates_code ON provider_templates(template_code);

-- 查询索引
CREATE INDEX idx_provider_templates_type ON provider_templates(template_type, status);
CREATE INDEX idx_provider_templates_provider ON provider_templates(provider_type);
CREATE INDEX idx_provider_templates_market ON provider_templates(market_status);
CREATE INDEX idx_provider_templates_author ON provider_templates(author_id);
