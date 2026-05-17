-- 为 gateway_api_keys 表添加 key_encrypted 字段，用于存储加密后的完整 API Key
ALTER TABLE gateway_api_keys ADD COLUMN key_encrypted TEXT;

-- 添加注释
COMMENT ON COLUMN gateway_api_keys.key_encrypted IS '加密后的完整 API Key（AES-256-GCM）';
