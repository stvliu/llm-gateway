-- 将 gemini 供应商元数据合并到 google（按公司组织，Gemini 是 Google 的产品）

-- 1. 更新 provider_metadata：gemini → google
UPDATE provider_metadata
SET provider_id = 'google',
    provider_name = 'Google',
    description = 'Google Gemini API，支持 Gemini 2.5 Pro、Gemini 2.5 Flash 等最新模型，超长上下文'
WHERE provider_id = 'gemini';

-- 2. 更新 product_metadata：gemini → google
UPDATE product_metadata
SET provider_id = 'google'
WHERE provider_id = 'gemini';

-- 3. 更新 model_metadata：gemini → google
UPDATE model_metadata
SET provider_id = 'google'
WHERE provider_id = 'gemini';

-- 4. 更新 providers 表中已创建的供应商实例
UPDATE providers
SET provider_id = 'google',
    provider_name = 'Google'
WHERE provider_id = 'gemini';
