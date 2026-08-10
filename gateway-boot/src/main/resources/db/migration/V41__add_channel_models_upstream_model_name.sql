--
-- Copyright (c) 2025 codingas.com
-- Licensed under the Apache License, Version 2.0.
-- See the LICENSE file for details.
--
-- 渠道模型关联表增加上游模型名字段
ALTER TABLE channel_models ADD COLUMN upstream_model_name VARCHAR(256);