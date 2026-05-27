-- 渠道模型关联表增加上游模型名字段
ALTER TABLE channel_models ADD COLUMN upstream_model_name VARCHAR(256);