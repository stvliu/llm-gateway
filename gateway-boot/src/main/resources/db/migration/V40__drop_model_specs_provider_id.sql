-- V40: Drop provider_id from model_specs, make it a global registry
-- ModelSpec 不再绑定特定供应商，成为全局模型注册表

-- 1. 删除外键约束（存在则删）
ALTER TABLE model_specs DROP CONSTRAINT IF EXISTS fk_models_provider;

-- 2. 合并相同 provider_model_id 的重复记录（保留 id 最小的那行）
UPDATE channel_models cm
SET model_spec_id = (
    SELECT MIN(ms2.id) FROM model_specs ms2
    WHERE ms2.provider_model_id = (
        SELECT ms3.provider_model_id FROM model_specs ms3 WHERE ms3.id = cm.model_spec_id
    )
)
WHERE (
    SELECT COUNT(*) FROM model_specs ms2
    WHERE ms2.provider_model_id = (
        SELECT ms3.provider_model_id FROM model_specs ms3 WHERE ms3.id = cm.model_spec_id
    )
) > 1;

-- 3. 删除重复的 model_specs 记录
DELETE FROM model_specs ms
WHERE EXISTS (
    SELECT 1 FROM model_specs ms2
    WHERE ms2.provider_model_id = ms.provider_model_id
      AND ms2.id < ms.id
);

-- 4. 先改为 nullable
ALTER TABLE model_specs ALTER COLUMN provider_id DROP NOT NULL;

-- 5. 设置所有 provider_id 为 NULL
UPDATE model_specs SET provider_id = NULL;

-- 6. 删除旧的唯一约束（包含 provider_id）
ALTER TABLE model_specs DROP CONSTRAINT IF EXISTS uk_models_provider_model;

-- 7. 删除 provider_id 列（级联删除外键约束）
ALTER TABLE model_specs DROP COLUMN provider_id;

-- 8. 删除 provider_id 上的索引
DROP INDEX IF EXISTS idx_models_provider;

-- 9. 添加唯一约束：provider_model_id 全局唯一
ALTER TABLE model_specs ADD CONSTRAINT uq_model_specs_provider_model_id UNIQUE (provider_model_id);