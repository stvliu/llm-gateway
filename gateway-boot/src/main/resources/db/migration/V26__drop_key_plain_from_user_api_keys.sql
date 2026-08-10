--
-- Copyright (c) 2025 codingas.com
-- Licensed under the Apache License, Version 2.0.
-- See the LICENSE file for details.
--
-- V26: 移除 user_api_keys 表的 key_plain 列
-- 密钥已通过 key_hash + key_encrypted 安全存储，key_plain 不再需要

ALTER TABLE user_api_keys DROP COLUMN IF EXISTS key_plain;
