-- ============================================
-- V34: 产品-模型关联重构
-- 1. 创建 product_model_metadata 纯关系表
-- 2. product_metadata 新增 7 个定价列
-- 3. 从 model_metadata 迁移定价到 product_metadata
-- 4. 从 model_metadata.product_id 迁移关系到 product_model_metadata
-- 5. model_metadata 删除定价列和 product_id 列
-- 6. 创建 product_models 纯关系表（业务体系）
-- 7. products 新增定价列
-- 8. 从 models 迁移定价到 products
-- 9. products 删除 models 列（关联数据由应用层 MetadataSyncService 处理）
-- 10. models 删除定价列
-- ============================================

-- === 元数据体系 ===

-- 1. 创建 product_model_metadata 纯关系表
CREATE TABLE product_model_metadata (
    id          BIGSERIAL PRIMARY KEY,
    product_id  BIGINT NOT NULL REFERENCES product_metadata(id) ON DELETE CASCADE,
    model_id    BIGINT NOT NULL REFERENCES model_metadata(id) ON DELETE CASCADE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by  BIGINT,
    updated_at  TIMESTAMP WITH TIME ZONE,
    updated_by  BIGINT,
    CONSTRAINT uk_pmm_product_model UNIQUE (product_id, model_id)
);

CREATE INDEX idx_pmm_product_id ON product_model_metadata(product_id);
CREATE INDEX idx_pmm_model_id ON product_model_metadata(model_id);

-- 2. product_metadata 新增 7 个定价列
ALTER TABLE product_metadata ADD COLUMN input_price DECIMAL(12,6);
ALTER TABLE product_metadata ADD COLUMN output_price DECIMAL(12,6);
ALTER TABLE product_metadata ADD COLUMN reasoning_price DECIMAL(12,6);
ALTER TABLE product_metadata ADD COLUMN cache_read_price DECIMAL(12,6);
ALTER TABLE product_metadata ADD COLUMN cache_write_price DECIMAL(12,6);
ALTER TABLE product_metadata ADD COLUMN input_audio_price DECIMAL(12,6);
ALTER TABLE product_metadata ADD COLUMN output_audio_price DECIMAL(12,6);

-- 3. 从 model_metadata 迁移定价到 product_metadata
-- 策略：每个 product_id 取第一个有定价的模型的价格
-- 注意：使用标准 SQL 兼容 H2 和 PostgreSQL
UPDATE product_metadata pm
SET input_price = mm.input_price,
    output_price = mm.output_price,
    reasoning_price = mm.reasoning_price,
    cache_read_price = mm.cache_read_price,
    cache_write_price = mm.cache_write_price,
    input_audio_price = mm.input_audio_price,
    output_audio_price = mm.output_audio_price
FROM (
    SELECT m.product_id, m.input_price, m.output_price,
           m.reasoning_price, m.cache_read_price, m.cache_write_price,
           m.input_audio_price, m.output_audio_price,
           ROW_NUMBER() OVER (PARTITION BY m.product_id ORDER BY m.id) AS rn
    FROM model_metadata m
    WHERE m.product_id IS NOT NULL AND m.input_price IS NOT NULL
) mm
WHERE pm.id = mm.product_id AND mm.rn = 1;

-- 4. 从 model_metadata.product_id 迁移关系到 product_model_metadata
INSERT INTO product_model_metadata (product_id, model_id, created_at, updated_at)
SELECT m.product_id, m.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM model_metadata m
WHERE m.product_id IS NOT NULL
ON CONFLICT (product_id, model_id) DO NOTHING;

-- 5. model_metadata 删除定价列和 product_id 列
ALTER TABLE model_metadata DROP COLUMN IF EXISTS product_id;
ALTER TABLE model_metadata DROP COLUMN IF EXISTS input_price;
ALTER TABLE model_metadata DROP COLUMN IF EXISTS output_price;
ALTER TABLE model_metadata DROP COLUMN IF EXISTS reasoning_price;
ALTER TABLE model_metadata DROP COLUMN IF EXISTS cache_read_price;
ALTER TABLE model_metadata DROP COLUMN IF EXISTS cache_write_price;
ALTER TABLE model_metadata DROP COLUMN IF EXISTS input_audio_price;
ALTER TABLE model_metadata DROP COLUMN IF EXISTS output_audio_price;

-- === 业务体系 ===

-- 6. 创建 product_models 纯关系表
CREATE TABLE product_models (
    id          BIGSERIAL PRIMARY KEY,
    product_id  BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    model_id    BIGINT NOT NULL REFERENCES models(id) ON DELETE CASCADE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by  BIGINT,
    updated_at  TIMESTAMP WITH TIME ZONE,
    updated_by  BIGINT,
    CONSTRAINT uk_pm_product_model UNIQUE (product_id, model_id)
);

CREATE INDEX idx_pm_product_id ON product_models(product_id);
CREATE INDEX idx_pm_model_id ON product_models(model_id);

-- 7. products 新增定价列
ALTER TABLE products ADD COLUMN input_price DECIMAL(12,6);
ALTER TABLE products ADD COLUMN output_price DECIMAL(12,6);

-- 8. 从 models 迁移定价到 products
-- 策略：每个 provider_id 对应的产品取第一个有定价模型的价格
UPDATE products p
SET input_price = sub.input_price,
    output_price = sub.output_price
FROM (
    SELECT m.provider_id, m.input_price, m.output_price,
           ROW_NUMBER() OVER (PARTITION BY m.provider_id ORDER BY m.id) AS rn
    FROM models m
    WHERE m.input_price IS NOT NULL
) sub
WHERE p.provider_id = sub.provider_id AND sub.rn = 1;

-- 9. 关联数据迁移由应用层 MetadataSyncService 处理
-- products.models JSON 数组的数据将在元数据同步时自动建立关联

-- 10. products 删除 models 列
ALTER TABLE products DROP COLUMN IF EXISTS models;

-- 11. models 删除定价列
ALTER TABLE models DROP COLUMN IF EXISTS input_price;
ALTER TABLE models DROP COLUMN IF EXISTS output_price;