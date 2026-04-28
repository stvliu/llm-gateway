-- V2__add_indexes.sql
-- Phase 2 CRUD 功能所需索引

-- ============================================
-- 用户表索引
-- ============================================
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_user_code ON users(user_code);
CREATE INDEX IF NOT EXISTS idx_users_status ON users(status);

-- ============================================
-- 角色表索引
-- ============================================
CREATE INDEX IF NOT EXISTS idx_roles_role_code ON roles(role_code);
CREATE INDEX IF NOT EXISTS idx_roles_is_active ON roles(is_active);

-- ============================================
-- 权限表索引
-- ============================================
CREATE INDEX IF NOT EXISTS idx_permissions_code ON permissions(permission_code);

-- ============================================
-- 用户角色中间表索引
-- ============================================
CREATE INDEX IF NOT EXISTS idx_user_roles_user ON user_roles(user_id);
CREATE INDEX IF NOT EXISTS idx_user_roles_role ON user_roles(role_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_user_roles_unique ON user_roles(user_id, role_id);

-- ============================================
-- 角色权限中间表索引
-- ============================================
CREATE INDEX IF NOT EXISTS idx_role_permissions_role ON role_permissions(role_id);
CREATE INDEX IF NOT EXISTS idx_role_permissions_permission ON role_permissions(permission_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_role_permissions_unique ON role_permissions(role_id, permission_id);

-- ============================================
-- API Key 表索引
-- ============================================
CREATE INDEX IF NOT EXISTS idx_gateway_api_keys_key_hash ON gateway_api_keys(key_hash);
CREATE INDEX IF NOT EXISTS idx_gateway_api_keys_user ON gateway_api_keys(user_id);
CREATE INDEX IF NOT EXISTS idx_gateway_api_keys_status ON gateway_api_keys(status);
CREATE INDEX IF NOT EXISTS idx_gateway_api_keys_expires ON gateway_api_keys(expires_at);

-- ============================================
-- 提供商表索引
-- ============================================
CREATE INDEX IF NOT EXISTS idx_providers_provider_code ON providers(provider_code);
CREATE INDEX IF NOT EXISTS idx_providers_status ON providers(status);

-- ============================================
-- 模型表索引
-- ============================================
CREATE INDEX IF NOT EXISTS idx_models_model_code ON models(model_code);
CREATE INDEX IF NOT EXISTS idx_models_provider ON models(provider_id);
CREATE INDEX IF NOT EXISTS idx_models_status ON models(status);
CREATE UNIQUE INDEX IF NOT EXISTS idx_models_provider_model ON models(provider_id, provider_model_id);

-- ============================================
-- 路由分组表索引
-- ============================================
CREATE INDEX IF NOT EXISTS idx_route_groups_group_code ON route_groups(group_code);

-- ============================================
-- 路由分组提供商中间表索引
-- ============================================
CREATE INDEX IF NOT EXISTS idx_route_group_providers_group ON route_group_providers(route_group_id);
CREATE INDEX IF NOT EXISTS idx_route_group_providers_provider ON route_group_providers(provider_id);

-- ============================================
-- Token 限额表索引
-- ============================================
CREATE INDEX IF NOT EXISTS idx_token_limits_limit_code ON token_limits(limit_code);
CREATE INDEX IF NOT EXISTS idx_token_limits_user ON token_limits(user_id);
CREATE INDEX IF NOT EXISTS idx_token_limits_provider ON token_limits(provider_id);
CREATE INDEX IF NOT EXISTS idx_token_limits_model ON token_limits(model_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_token_limits_unique ON token_limits(user_id, provider_id, model_id);
