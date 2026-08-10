--
-- Copyright (c) 2025 codingas.com
-- Licensed under the Apache License, Version 2.0.
-- See the LICENSE file for details.
--
-- V45: 移除 UserApiKey 的 channel_ids 字段
-- 渠道权限现在通过 Team ↔ Channel 关系管理
-- API Key 继承用户所属团队的渠道权限，不再直接关联渠道

ALTER TABLE user_api_keys DROP COLUMN IF EXISTS channel_ids;
