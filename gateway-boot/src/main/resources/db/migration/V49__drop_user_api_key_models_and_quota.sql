--
-- Copyright (c) 2025 codingas.com
-- Licensed under the Apache License, Version 2.0.
-- See the LICENSE file for details.
--
-- 移除 UserApiKey 的 models 和 quota_limit 列
-- UserApiKey 不关联模型，也没有额度限制

ALTER TABLE user_api_keys DROP COLUMN IF EXISTS models;
ALTER TABLE user_api_keys DROP COLUMN IF EXISTS quota_limit;
