-- V2__seed_providers.sql
-- 初始化提供商种子数据

-- OpenAI Provider
INSERT INTO providers (provider_code, provider_name, provider_type, base_url, website_url, api_doc_url, status, priority, created_by, created_at, updated_by, updated_at)
VALUES (
    'openai',
    'OpenAI',
    'OPENAI',
    'https://api.openai.com',
    'https://openai.com',
    'https://platform.openai.com/docs/api-reference',
    'ACTIVE',
    100,
    0,
    CURRENT_TIMESTAMP,
    NULL,
    CURRENT_TIMESTAMP
);

-- Anthropic Provider
INSERT INTO providers (provider_code, provider_name, provider_type, base_url, website_url, api_doc_url, status, priority, created_by, created_at, updated_by, updated_at)
VALUES (
    'anthropic',
    'Anthropic',
    'ANTHROPIC',
    'https://api.anthropic.com',
    'https://anthropic.com',
    'https://docs.anthropic.com/en/api/reference',
    'ACTIVE',
    90,
    0,
    CURRENT_TIMESTAMP,
    NULL,
    CURRENT_TIMESTAMP
);

-- Azure OpenAI Provider
INSERT INTO providers (provider_code, provider_name, provider_type, base_url, website_url, api_doc_url, status, priority, created_by, created_at, updated_by, updated_at)
VALUES (
    'azure-openai',
    'Azure OpenAI',
    'AZURE_OPENAI',
    'https://{your-resource-name}.openai.azure.com',
    'https://azure.microsoft.com/services/cognitive-services/openai/',
    'https://learn.microsoft.com/azure/ai-services/openai/reference',
    'ACTIVE',
    80,
    0,
    CURRENT_TIMESTAMP,
    NULL,
    CURRENT_TIMESTAMP
);