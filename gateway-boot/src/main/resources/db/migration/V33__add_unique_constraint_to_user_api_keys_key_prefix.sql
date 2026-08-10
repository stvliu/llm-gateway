--
-- Copyright (c) 2025 codingas.com
-- Licensed under the Apache License, Version 2.0.
-- See the LICENSE file for details.
--
-- V33: 为 user_api_keys.key_prefix 添加唯一约束
-- 确保 findByKeyPrefix 认证查询的 prefix 唯一性

CREATE UNIQUE INDEX idx_user_api_keys_key_prefix ON user_api_keys(key_prefix);
