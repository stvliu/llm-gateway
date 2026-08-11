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
-- V17: 添加团队相关表（H2/PostgreSQL 兼容）
-- ============================================

-- 1. 创建团队表
CREATE TABLE teams (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    description VARCHAR(256),
    state VARCHAR(16) DEFAULT 'active',
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- 2. 创建用户-团队关联表
CREATE TABLE user_teams (
    user_id BIGINT NOT NULL REFERENCES users(id),
    team_id BIGINT NOT NULL REFERENCES teams(id),
    role VARCHAR(32) DEFAULT 'member',
    created_at TIMESTAMP DEFAULT NOW(),
    PRIMARY KEY (user_id, team_id)
);

-- 3. 创建用户 API Key 表
CREATE TABLE user_api_keys (
    id BIGSERIAL PRIMARY KEY,
    team_id BIGINT NOT NULL REFERENCES teams(id),
    owner_user_id BIGINT REFERENCES users(id),
    product_id BIGINT NOT NULL REFERENCES products(id),
    key_hash VARCHAR(128) NOT NULL,
    key_prefix VARCHAR(16) NOT NULL,
    name VARCHAR(128),
    models TEXT,
    quota_limit BIGINT,
    state VARCHAR(16) DEFAULT 'active',
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- 4. 创建索引
CREATE INDEX idx_teams_state ON teams(state);
CREATE INDEX idx_user_teams_user ON user_teams(user_id);
CREATE INDEX idx_user_teams_team ON user_teams(team_id);
CREATE UNIQUE INDEX idx_user_api_keys_key_hash ON user_api_keys(key_hash);
CREATE INDEX idx_user_api_keys_team ON user_api_keys(team_id);
CREATE INDEX idx_user_api_keys_product ON user_api_keys(product_id);
CREATE INDEX idx_user_api_keys_state ON user_api_keys(state);

-- 5. 自动迁移：为每个用户创建默认团队
INSERT INTO teams (name, description, state)
SELECT u.username || '-team', 'Default team for ' || u.username, 'active'
FROM users u
WHERE u.state = 'active';

-- 6. 自动迁移：关联用户到默认团队
INSERT INTO user_teams (user_id, team_id, role)
SELECT u.id, t.id, 'owner'
FROM users u
JOIN teams t ON t.name = u.username || '-team'
WHERE u.state = 'active';
