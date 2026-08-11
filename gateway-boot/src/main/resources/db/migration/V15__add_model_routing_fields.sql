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
-- 为 models 表添加路由字段
-- 支持一个模型多供应商路由

-- 添加优先级字段（用于 FAILOVER 策略，值越小越优先）
ALTER TABLE models ADD COLUMN priority INT NOT NULL DEFAULT 100;

-- 添加权重字段（用于 WEIGHTED 策略）
ALTER TABLE models ADD COLUMN weight INT NOT NULL DEFAULT 100;

-- 添加索引优化路由查询（按 provider_model_id 查多渠道）
CREATE INDEX idx_models_provider_model_id ON models(provider_model_id);
CREATE INDEX idx_models_provider_model_id_state ON models(provider_model_id, state);
