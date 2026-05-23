-- 为现有 providers 补充品牌信息
-- 从 provider_metadata 表复制 icon_url、description 等字段

-- 更新 providers 表，根据名称匹配 provider_metadata
UPDATE providers p
SET
  provider_id = pm.provider_id,
  icon_url = pm.icon_url,
  description = pm.description,
  tags = pm.tags
FROM provider_metadata pm
WHERE LOWER(p.provider_name) = LOWER(pm.provider_name)
  AND p.provider_id IS NULL;

-- 更新 providers 表，根据 provider_id 匹配（如果 provider_name 就是 provider_id）
UPDATE providers p
SET
  icon_url = pm.icon_url,
  description = pm.description,
  tags = pm.tags
FROM provider_metadata pm
WHERE LOWER(p.provider_name) = LOWER(pm.provider_id)
  AND p.icon_url IS NULL;

COMMENT ON COLUMN providers.provider_id IS '品牌标识（关联 ProviderMetadata.providerId）';
COMMENT ON COLUMN providers.icon_url IS '品牌图标 URL';
COMMENT ON COLUMN providers.description IS '描述信息';
COMMENT ON COLUMN providers.tags IS '标签（JSON 格式）';