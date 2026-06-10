-- V48: user_api_keys 表 state 列替换为 deleted 逻辑删除标记
-- UserApiKey 只有"存在"和"已删除"两种状态，无需 state 枚举

-- 1. 添加 deleted 列
ALTER TABLE user_api_keys ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT FALSE;

-- 2. 迁移数据：REVOKED 状态的 Key 标记为已删除
UPDATE user_api_keys SET deleted = TRUE WHERE state = 'REVOKED';

-- 3. 删除 state 列及其索引
DROP INDEX IF EXISTS idx_user_api_keys_state;
ALTER TABLE user_api_keys DROP COLUMN state;
