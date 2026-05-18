-- ============================================
-- V17: 添加团队相关表
-- ============================================

-- 1. 创建团队表
CREATE TABLE teams (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    name VARCHAR(128) NOT NULL,
    description VARCHAR(256),
    state VARCHAR(16) DEFAULT 'active',
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

COMMENT ON TABLE teams IS '团队表';
COMMENT ON COLUMN teams.name IS '团队名称';
COMMENT ON COLUMN teams.state IS '状态：active, inactive, deleted';

-- 2. 创建用户-团队关联表
CREATE TABLE user_teams (
    user_id BIGINT NOT NULL REFERENCES users(id),
    team_id BIGINT NOT NULL REFERENCES teams(id),
    role VARCHAR(32) DEFAULT 'member',
    created_at TIMESTAMP DEFAULT NOW(),
    PRIMARY KEY (user_id, team_id)
);

COMMENT ON TABLE user_teams IS '用户-团队关联表';
COMMENT ON COLUMN user_teams.role IS '角色：owner, admin, member';

-- 3. 创建用户 API Key 表
CREATE TABLE user_api_keys (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    team_id BIGINT NOT NULL REFERENCES teams(id),
    owner_user_id BIGINT REFERENCES users(id),
    product_id BIGINT NOT NULL REFERENCES products(id),
    key_hash VARCHAR(128) NOT NULL,
    key_prefix VARCHAR(16) NOT NULL,
    name VARCHAR(128),
    models JSONB,
    quota_limit BIGINT,
    state VARCHAR(16) DEFAULT 'active',
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

COMMENT ON TABLE user_api_keys IS '用户侧 API Key 表';
COMMENT ON COLUMN user_api_keys.team_id IS '所属团队 ID';
COMMENT ON COLUMN user_api_keys.owner_user_id IS '创建者用户 ID';
COMMENT ON COLUMN user_api_keys.product_id IS '绑定的产品 ID';
COMMENT ON COLUMN user_api_keys.key_hash IS 'Key 哈希值，用于认证';
COMMENT ON COLUMN user_api_keys.key_prefix IS 'Key 前缀，用于识别';
COMMENT ON COLUMN user_api_keys.models IS '可访问的模型列表（子集），为空表示全部';
COMMENT ON COLUMN user_api_keys.quota_limit IS 'Key 级别的额度限制';

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
SELECT u.name || '-team', 'Default team for ' || u.name, 'active'
FROM users u
WHERE u.state = 'active';

-- 6. 自动迁移：关联用户到默认团队
INSERT INTO user_teams (user_id, team_id, role)
SELECT u.id, t.id, 'owner'
FROM users u
JOIN teams t ON t.name = u.name || '-team'
WHERE u.state = 'active';

-- 7. 创建更新时间触发器
CREATE TRIGGER teams_updated_at
    BEFORE UPDATE ON teams
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at();

CREATE TRIGGER user_api_keys_updated_at
    BEFORE UPDATE ON user_api_keys
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at();
