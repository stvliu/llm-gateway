-- 移除 Provider.type 字段（协议信息已由 ProtocolGateway 体系管理）
DROP INDEX IF EXISTS idx_providers_type;
ALTER TABLE providers DROP COLUMN IF EXISTS provider_type;

-- 移除 ProviderMetadata.providerType 字段（先删除索引再删除列）
DROP INDEX IF EXISTS idx_provider_metadata_type;
ALTER TABLE provider_metadata DROP COLUMN IF EXISTS provider_type;