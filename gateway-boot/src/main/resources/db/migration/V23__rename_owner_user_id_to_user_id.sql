--
-- Copyright (c) 2025 codingas.com
-- Licensed under the Apache License, Version 2.0.
-- See the LICENSE file for details.
--
-- 将 user_api_keys 表的 owner_user_id 列重命名为 user_id
ALTER TABLE user_api_keys RENAME COLUMN owner_user_id TO user_id;
