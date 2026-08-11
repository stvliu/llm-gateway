--
-- Copyright © 2025-2026 codingas.com
--
-- Licensed under the Apache License, Version 2.0 (the "License");
-- you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at
--
--     http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.
--
-- 为 gateway_api_keys 表添加 key_encrypted 字段，用于存储加密后的完整 API Key
ALTER TABLE gateway_api_keys ADD COLUMN key_encrypted TEXT;

-- 添加注释
COMMENT ON COLUMN gateway_api_keys.key_encrypted IS '加密后的完整 API Key（AES-256-GCM）';
