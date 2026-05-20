-- V21: 为 user_api_keys 表添加 key_encrypted 列

ALTER TABLE user_api_keys ADD COLUMN key_encrypted TEXT;

COMMENT ON COLUMN user_api_keys.key_encrypted IS '加密存储的 API Key（AES-256-GCM），由基础设施层处理加解密';