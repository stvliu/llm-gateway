--
-- Copyright (c) 2025 codingas.com
-- Licensed under the Apache License, Version 2.0.
-- See the LICENSE file for details.
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
