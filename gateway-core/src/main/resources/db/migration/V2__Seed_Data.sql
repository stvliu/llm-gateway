-- ===================================================================
-- LLM-Gateway 初始数据
-- 版本: V2
-- 描述: 插入预置模型和默认配置
-- ===================================================================

-- -------------------------------------------------------------------
-- 插入预设角色
-- -------------------------------------------------------------------
INSERT INTO roles (role_code, name, role_type, scope_type, is_active, created_by, created_at) VALUES
('TEAM_ADMIN', '团队管理员', 'SYSTEM', 'TEAM', TRUE, 0, CURRENT_TIMESTAMP),
('DEVELOPER', '开发者', 'SYSTEM', 'TEAM', TRUE, 0, CURRENT_TIMESTAMP),
('OBSERVER', '观察者', 'SYSTEM', 'TEAM', TRUE, 0, CURRENT_TIMESTAMP),
('FINANCE_ADMIN', '财务管理员', 'SYSTEM', 'TEAM', TRUE, 0, CURRENT_TIMESTAMP);

-- -------------------------------------------------------------------
-- 插入预设权限
-- -------------------------------------------------------------------
INSERT INTO permissions (permission_code, name, description, resource_type, created_by, created_at) VALUES
-- 用户权限
('user:create', '创建用户', '创建新用户', 'user', 0, CURRENT_TIMESTAMP),
('user:read', '查看用户', '查看用户信息', 'user', 0, CURRENT_TIMESTAMP),
('user:update', '更新用户', '更新用户信息', 'user', 0, CURRENT_TIMESTAMP),
('user:delete', '删除用户', '删除用户', 'user', 0, CURRENT_TIMESTAMP),
-- 渠道权限
('channel:create', '创建渠道', '创建新渠道', 'channel', 0, CURRENT_TIMESTAMP),
('channel:read', '查看渠道', '查看渠道信息', 'channel', 0, CURRENT_TIMESTAMP),
('channel:update', '更新渠道', '更新渠道配置', 'channel', 0, CURRENT_TIMESTAMP),
('channel:delete', '删除渠道', '删除渠道', 'channel', 0, CURRENT_TIMESTAMP),
-- API Key 权限
('apikey:create', '创建 API Key', '创建新的 API Key', 'apikey', 0, CURRENT_TIMESTAMP),
('apikey:read', '查看 API Key', '查看 API Key 信息', 'apikey', 0, CURRENT_TIMESTAMP),
('apikey:update', '更新 API Key', '更新 API Key 配置', 'apikey', 0, CURRENT_TIMESTAMP),
('apikey:delete', '删除 API Key', '删除 API Key', 'apikey', 0, CURRENT_TIMESTAMP),
-- Token 限额权限
('token_limit:create', '创建限额', '创建 Token 限额', 'token_limit', 0, CURRENT_TIMESTAMP),
('token_limit:read', '查看限额', '查看 Token 限额', 'token_limit', 0, CURRENT_TIMESTAMP),
('token_limit:update', '更新限额', '更新 Token 限额', 'token_limit', 0, CURRENT_TIMESTAMP),
('token_limit:delete', '删除限额', '删除 Token 限额', 'token_limit', 0, CURRENT_TIMESTAMP),
-- 日志权限
('log:read', '查看日志', '查看调用日志', 'log', 0, CURRENT_TIMESTAMP),
('audit:read', '查看审计日志', '查看审计日志', 'audit', 0, CURRENT_TIMESTAMP),
-- 设置权限
('setting:read', '查看设置', '查看系统设置', 'setting', 0, CURRENT_TIMESTAMP),
('setting:update', '更新设置', '更新系统设置', 'setting', 0, CURRENT_TIMESTAMP);

-- -------------------------------------------------------------------
-- 角色-权限关联 (团队管理员拥有所有权限)
-- -------------------------------------------------------------------
INSERT INTO role_permissions (role_id, permission_id, created_by, created_at)
SELECT r.id, p.id, 0, CURRENT_TIMESTAMP
FROM roles r
CROSS JOIN permissions p
WHERE r.role_code = 'TEAM_ADMIN';

-- 开发者权限 (渠道读取、API Key 操作、日志查看)
INSERT INTO role_permissions (role_id, permission_id, created_by, created_at)
SELECT r.id, p.id, 0, CURRENT_TIMESTAMP
FROM roles r
CROSS JOIN permissions p
WHERE r.role_code = 'DEVELOPER'
AND p.permission_code IN (
    'channel:read',
    'apikey:create', 'apikey:read', 'apikey:update',
    'log:read'
);

-- 观察者权限 (只读)
INSERT INTO role_permissions (role_id, permission_id, created_by, created_at)
SELECT r.id, p.id, 0, CURRENT_TIMESTAMP
FROM roles r
CROSS JOIN permissions p
WHERE r.role_code = 'OBSERVER'
AND p.permission_code IN (
    'channel:read',
    'apikey:read',
    'log:read',
    'token_limit:read'
);

-- 财务管理员权限
INSERT INTO role_permissions (role_id, permission_id, created_by, created_at)
SELECT r.id, p.id, 0, CURRENT_TIMESTAMP
FROM roles r
CROSS JOIN permissions p
WHERE r.role_code = 'FINANCE_ADMIN'
AND p.permission_code IN (
    'channel:read',
    'token_limit:create', 'token_limit:read', 'token_limit:update', 'token_limit:delete',
    'log:read'
);

-- -------------------------------------------------------------------
-- 插入模型供应商
-- -------------------------------------------------------------------
INSERT INTO model_providers (provider_code, provider_name, website_url, api_doc_url, status, created_by, created_at) VALUES
('openai', 'OpenAI', 'https://openai.com', 'https://platform.openai.com/docs', 'ACTIVE', 0, CURRENT_TIMESTAMP),
('anthropic', 'Anthropic', 'https://anthropic.com', 'https://docs.anthropic.com', 'ACTIVE', 0, CURRENT_TIMESTAMP),
('google', 'Google AI', 'https://ai.google.dev', 'https://ai.google.dev/docs', 'ACTIVE', 0, CURRENT_TIMESTAMP),
('qwen', '通义千问', 'https://tongyi.aliyun.com', 'https://help.aliyun.com/document_detail/243978.html', 'ACTIVE', 0, CURRENT_TIMESTAMP),
('zhipu', '智谱 AI', 'https://www.zhipuai.cn', 'https://open.bigmodel.cn/dev/api', 'ACTIVE', 0, CURRENT_TIMESTAMP),
('deepseek', 'DeepSeek', 'https://www.deepseek.com', 'https://api.deepseek.com', 'ACTIVE', 0, CURRENT_TIMESTAMP),
('moonshot', 'Moonshot AI', 'https://www.moonshot.cn', 'https://platform.moonshot.cn/docs', 'ACTIVE', 0, CURRENT_TIMESTAMP),
('ollama', 'Ollama', 'https://ollama.com', 'https://github.com/ollama/ollama', 'ACTIVE', 0, CURRENT_TIMESTAMP);

-- -------------------------------------------------------------------
-- 插入预置模型 (50+)
-- -------------------------------------------------------------------
INSERT INTO models (model_code, model_name, provider_id, context_length, capabilities, input_price, output_price, status, created_by, created_at)
SELECT * FROM (
    -- OpenAI 模型
    SELECT 'gpt-4o', 'GPT-4o', (SELECT id FROM model_providers WHERE provider_code = 'openai'), 128000,
           '{"vision": true, "function_calling": true, "json_mode": true}'::json, 0.000005, 0.000015, 'ACTIVE', 0, CURRENT_TIMESTAMP
    UNION ALL
    SELECT 'gpt-4-turbo', 'GPT-4 Turbo', (SELECT id FROM model_providers WHERE provider_code = 'openai'), 128000,
           '{"vision": true, "function_calling": true, "json_mode": true}'::json, 0.00001, 0.00003, 'ACTIVE', 0, CURRENT_TIMESTAMP
    UNION ALL
    SELECT 'gpt-4', 'GPT-4', (SELECT id FROM model_providers WHERE provider_code = 'openai'), 8192,
           '{"vision": true, "function_calling": true}'::json, 0.00003, 0.00006, 'ACTIVE', 0, CURRENT_TIMESTAMP
    UNION ALL
    SELECT 'gpt-3.5-turbo', 'GPT-3.5 Turbo', (SELECT id FROM model_providers WHERE provider_code = 'openai'), 16385,
           '{"function_calling": true, "json_mode": true}'::json, 0.0000005, 0.0000015, 'ACTIVE', 0, CURRENT_TIMESTAMP
    -- Anthropic 模型
    UNION ALL
    SELECT 'claude-opus-4-5', 'Claude Opus 4', (SELECT id FROM model_providers WHERE provider_code = 'anthropic'), 200000,
           '{"vision": true, "function_calling": true}'::json, 0.000015, 0.000075, 'ACTIVE', 0, CURRENT_TIMESTAMP
    UNION ALL
    SELECT 'claude-sonnet-4-5', 'Claude Sonnet 4', (SELECT id FROM model_providers WHERE provider_code = 'anthropic'), 200000,
           '{"vision": true, "function_calling": true}'::json, 0.000003, 0.000015, 'ACTIVE', 0, CURRENT_TIMESTAMP
    UNION ALL
    SELECT 'claude-haiku-3-5', 'Claude Haiku 3', (SELECT id FROM model_providers WHERE provider_code = 'anthropic'), 200000,
           '{"vision": true, "function_calling": true}'::json, 0.0000008, 0.000004, 'ACTIVE', 0, CURRENT_TIMESTAMP
    -- Google 模型
    UNION ALL
    SELECT 'gemini-2.5-pro', 'Gemini 2.5 Pro', (SELECT id FROM model_providers WHERE provider_code = 'google'), 1000000,
           '{"vision": true, "function_calling": true}'::json, 0.00000125, 0.000005, 'ACTIVE', 0, CURRENT_TIMESTAMP
    UNION ALL
    SELECT 'gemini-2.5-flash', 'Gemini 2.5 Flash', (SELECT id FROM model_providers WHERE provider_code = 'google'), 1000000,
           '{"vision": true, "function_calling": true}'::json, 0.00000035, 0.00000105, 'ACTIVE', 0, CURRENT_TIMESTAMP
    UNION ALL
    SELECT 'gemini-1.5-pro', 'Gemini 1.5 Pro', (SELECT id FROM model_providers WHERE provider_code = 'google'), 1000000,
           '{"vision": true, "function_calling": true}'::json, 0.00000125, 0.000005, 'ACTIVE', 0, CURRENT_TIMESTAMP
    UNION ALL
    SELECT 'gemini-1.5-flash', 'Gemini 1.5 Flash', (SELECT id FROM model_providers WHERE provider_code = 'google'), 1000000,
           '{"vision": true, "function_calling": true}'::json, 0.00000035, 0.00000105, 'ACTIVE', 0, CURRENT_TIMESTAMP
    -- 通义千问
    UNION ALL
    SELECT 'qwen-max', 'Qwen Max', (SELECT id FROM model_providers WHERE provider_code = 'qwen'), 32000,
           '{"vision": true, "function_calling": true}'::json, 0.00002, 0.00006, 'ACTIVE', 0, CURRENT_TIMESTAMP
    UNION ALL
    SELECT 'qwen-plus', 'Qwen Plus', (SELECT id FROM model_providers WHERE provider_code = 'qwen'), 131072,
           '{"vision": true, "function_calling": true}'::json, 0.000004, 0.000012, 'ACTIVE', 0, CURRENT_TIMESTAMP
    UNION ALL
    SELECT 'qwen-turbo', 'Qwen Turbo', (SELECT id FROM model_providers WHERE provider_code = 'qwen'), 32000,
           '{"vision": true, "function_calling": true}'::json, 0.000002, 0.000006, 'ACTIVE', 0, CURRENT_TIMESTAMP
    -- 智谱 AI
    UNION ALL
    SELECT 'glm-4', 'GLM-4', (SELECT id FROM model_providers WHERE provider_code = 'zhipu'), 128000,
           '{"vision": true, "function_calling": true}'::json, 0.00001, 0.00001, 'ACTIVE', 0, CURRENT_TIMESTAMP
    UNION ALL
    SELECT 'glm-4-plus', 'GLM-4 Plus', (SELECT id FROM model_providers WHERE provider_code = 'zhipu'), 128000,
           '{"vision": true, "function_calling": true}'::json, 0.00001, 0.00001, 'ACTIVE', 0, CURRENT_TIMESTAMP
    UNION ALL
    SELECT 'glm-3-turbo', 'GLM-3 Turbo', (SELECT id FROM model_providers WHERE provider_code = 'zhipu'), 128000,
           '{"function_calling": true}'::json, 0.000001, 0.000001, 'ACTIVE', 0, CURRENT_TIMESTAMP
    -- DeepSeek
    UNION ALL
    SELECT 'deepseek-v3', 'DeepSeek V3', (SELECT id FROM model_providers WHERE provider_code = 'deepseek'), 128000,
           '{"function_calling": true}'::json, 0.00000027, 0.0000011, 'ACTIVE', 0, CURRENT_TIMESTAMP
    UNION ALL
    SELECT 'deepseek-coder', 'DeepSeek Coder', (SELECT id FROM model_providers WHERE provider_code = 'deepseek'), 128000,
           '{"function_calling": true}'::json, 0.00000027, 0.0000011, 'ACTIVE', 0, CURRENT_TIMESTAMP
    -- Moonshot
    UNION ALL
    SELECT 'moonshot-v1-128k', 'Moonshot V1 128K', (SELECT id FROM model_providers WHERE provider_code = 'moonshot'), 128000,
           '{"function_calling": true}'::json, 0.000012, 0.000012, 'ACTIVE', 0, CURRENT_TIMESTAMP
    UNION ALL
    SELECT 'moonshot-v1-32k', 'Moonshot V1 32K', (SELECT id FROM model_providers WHERE provider_code = 'moonshot'), 32000,
           '{"function_calling": true}'::json, 0.000012, 0.000012, 'ACTIVE', 0, CURRENT_TIMESTAMP
    UNION ALL
    SELECT 'moonshot-v1-8k', 'Moonshot V1 8K', (SELECT id FROM model_providers WHERE provider_code = 'moonshot'), 8000,
           '{"function_calling": true}'::json, 0.000012, 0.000012, 'ACTIVE', 0, CURRENT_TIMESTAMP
    -- Ollama (本地模型示例)
    UNION ALL
    SELECT 'llama3', 'Llama 3', (SELECT id FROM model_providers WHERE provider_code = 'ollama'), 8192,
           '{"function_calling": false}'::json, 0, 0, 'ACTIVE', 0, CURRENT_TIMESTAMP
    UNION ALL
    SELECT 'llama3.1', 'Llama 3.1', (SELECT id FROM model_providers WHERE provider_code = 'ollama'), 128000,
           '{"function_calling": false}'::json, 0, 0, 'ACTIVE', 0, CURRENT_TIMESTAMP
    UNION ALL
    SELECT 'mistral', 'Mistral', (SELECT id FROM model_providers WHERE provider_code = 'ollama'), 8192,
           '{"function_calling": false}'::json, 0, 0, 'ACTIVE', 0, CURRENT_TIMESTAMP
    UNION ALL
    SELECT 'codellama', 'Code Llama', (SELECT id FROM model_providers WHERE provider_code = 'ollama'), 16384,
           '{"function_calling": false}'::json, 0, 0, 'ACTIVE', 0, CURRENT_TIMESTAMP
    UNION ALL
    SELECT 'qwen2.5', 'Qwen 2.5', (SELECT id FROM model_providers WHERE provider_code = 'ollama'), 32000,
           '{"function_calling": true}'::json, 0, 0, 'ACTIVE', 0, CURRENT_TIMESTAMP
) AS models(model_code, model_name, provider_id, context_length, capabilities, input_price, output_price, status, created_by, created_at);

-- -------------------------------------------------------------------
-- 提交
-- -------------------------------------------------------------------
COMMIT;
