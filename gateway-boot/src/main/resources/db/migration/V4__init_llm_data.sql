-- V4__init_llm_data.sql
-- 初始化大模型 Provider、API Key 和 Model 数据

-- ============================================
-- 插入 Provider（火山引擎）
-- ============================================
INSERT INTO providers (id, provider_name, provider_type, base_url,
    website_url, api_doc_url, priority, status, created_at, version)
VALUES (1, '火山引擎', 'VOLCENGINE', 'https://ark.cn-beijing.volces.com/api/v3',
    'https://www.volcengine.com', 'https://www.volcengine.com/docs/82379/1298454', 100, 'ACTIVE',
    CURRENT_TIMESTAMP, 0);

-- ============================================
-- 插入 Provider API Key
-- ============================================
INSERT INTO provider_api_keys (provider_id, key_name, api_key, encrypted_api_key,
    priority, status, created_at, version)
VALUES (1, '火山引擎主密钥', '1fb8bdcf-3383-426d-9f3d-4c2979895c58', NULL,
    100, 'ACTIVE', CURRENT_TIMESTAMP, 0);

-- ============================================
-- 插入模型数据
-- ============================================

-- 豆包 Pro 32K
INSERT INTO models (provider_id, provider_model_id, display_name,
    context_window, input_price, output_price, capabilities, status, created_at, version)
VALUES (1, 'doubao-pro-32k', '豆包 Pro 32K',
    32768, 0.0008, 0.002,
    JSON '{"chat": true, "streaming": true, "function_calling": true}',
    'ACTIVE', CURRENT_TIMESTAMP, 0);

-- 豆包 Pro 128K
INSERT INTO models (provider_id, provider_model_id, display_name,
    context_window, input_price, output_price, capabilities, status, created_at, version)
VALUES (1, 'doubao-pro-128k', '豆包 Pro 128K',
    131072, 0.005, 0.009,
    JSON '{"chat": true, "streaming": true, "function_calling": true}',
    'ACTIVE', CURRENT_TIMESTAMP, 0);

-- 豆包 Lite 32K
INSERT INTO models (provider_id, provider_model_id, display_name,
    context_window, input_price, output_price, capabilities, status, created_at, version)
VALUES (1, 'doubao-lite-32k', '豆包 Lite 32K',
    32768, 0.0003, 0.0006,
    JSON '{"chat": true, "streaming": true, "function_calling": true}',
    'ACTIVE', CURRENT_TIMESTAMP, 0);

-- 豆包 Seed 2.0 Pro
INSERT INTO models (provider_id, provider_model_id, display_name,
    context_window, input_price, output_price, capabilities, status, created_at, version)
VALUES (1, 'doubao-seed-2-0-pro-260215', '豆包 Seed 2.0 Pro',
    128000, 0.001, 0.002,
    JSON '{"chat": true, "streaming": true, "function_calling": true}',
    'ACTIVE', CURRENT_TIMESTAMP, 0);

-- 豆包 Seed 2.0 Code Preview
INSERT INTO models (provider_id, provider_model_id, display_name,
    context_window, input_price, output_price, capabilities, status, created_at, version)
VALUES (1, 'doubao-seed-2-0-code-preview-260215', '豆包 Seed 2.0 Code Preview',
    128000, 0.001, 0.002,
    JSON '{"chat": true, "streaming": true, "function_calling": true}',
    'ACTIVE', CURRENT_TIMESTAMP, 0);

-- 豆包 Seed 2.0 Mini（用户测试模型）
INSERT INTO models (provider_id, provider_model_id, display_name,
    context_window, input_price, output_price, capabilities, status, created_at, version)
VALUES (1, 'doubao-seed-2-0-mini-260215', '豆包 Seed 2.0 Mini',
    128000, 0.0005, 0.001,
    JSON '{"chat": true, "streaming": true, "function_calling": true}',
    'ACTIVE', CURRENT_TIMESTAMP, 0);
