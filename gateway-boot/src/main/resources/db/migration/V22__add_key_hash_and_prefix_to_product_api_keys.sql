-- V22: 为 product_api_keys 表添加 api_key_hash 和 api_key_prefix 列

ALTER TABLE product_api_keys ADD COLUMN api_key_hash VARCHAR(128);
ALTER TABLE product_api_keys ADD COLUMN api_key_prefix VARCHAR(16);

COMMENT ON COLUMN product_api_keys.api_key_hash IS 'API Key 的 SHA-256 哈希值，用于认证查找';
COMMENT ON COLUMN product_api_keys.api_key_prefix IS 'API Key 前缀（前8位），用于识别';

-- 从已有的 api_key_encrypted 反填 api_key_hash（需要应用层处理，此处仅添加列）
