--
-- Copyright (c) 2025 codingas.com
-- Licensed under the Apache License, Version 2.0.
-- See the LICENSE file for details.
--
-- 密钥归于用户，移除 team_id 列
ALTER TABLE user_api_keys DROP COLUMN team_id;
