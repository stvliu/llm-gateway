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
-- 移除 Provider.type 字段（协议信息已由 ProtocolGateway 体系管理）
DROP INDEX IF EXISTS idx_providers_type;
ALTER TABLE providers DROP COLUMN IF EXISTS provider_type;

-- 移除 ProviderMetadata.providerType 字段（先删除索引再删除列）
DROP INDEX IF EXISTS idx_provider_metadata_type;
ALTER TABLE provider_metadata DROP COLUMN IF EXISTS provider_type;