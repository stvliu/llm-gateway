--
-- Copyright (c) 2025 codingas.com
-- Licensed under the Apache License, Version 2.0.
-- See the LICENSE file for details.
--
-- 删除 gateway_api_keys 表的 deleted_at 列（软删除改为硬删除）
ALTER TABLE gateway_api_keys DROP COLUMN IF EXISTS deleted_at;
