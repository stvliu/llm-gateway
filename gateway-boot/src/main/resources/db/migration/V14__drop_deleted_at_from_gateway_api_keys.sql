-- 删除 gateway_api_keys 表的 deleted_at 列（软删除改为硬删除）
ALTER TABLE gateway_api_keys DROP COLUMN IF EXISTS deleted_at;
