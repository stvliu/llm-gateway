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
    id              BIGSERIAL PRIMARY KEY,
    product_id      BIGINT NOT NULL REFERENCES product_metadata(id) ON DELETE CASCADE,
    model_id        BIGINT NOT NULL REFERENCES model_metadata(id) ON DELETE CASCADE,
    source          VARCHAR(32) NOT NULL DEFAULT 'BUILTIN',
    source_synced_at TIMESTAMP WITH TIME ZONE,
    state           VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      BIGINT,
    updated_at      TIMESTAMP WITH TIME ZONE,
    updated_by      BIGINT,
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
-- 使用临时表绕过 H2 不支持 CTE+UPDATE 的限制
DROP TABLE IF EXISTS tmp_ranked_prices;
CREATE TABLE tmp_ranked_prices AS
SELECT m.product_id, m.input_price, m.output_price,
       m.reasoning_price, m.cache_read_price, m.cache_write_price,
       m.input_audio_price, m.output_audio_price,
       ROW_NUMBER() OVER (PARTITION BY m.product_id ORDER BY m.id) AS rn
FROM model_metadata m
WHERE m.product_id IS NOT NULL AND m.input_price IS NOT NULL;

UPDATE product_metadata pm
SET input_price = (SELECT r.input_price FROM tmp_ranked_prices r WHERE r.product_id = pm.id AND r.rn = 1),
    output_price = (SELECT r.output_price FROM tmp_ranked_prices r WHERE r.product_id = pm.id AND r.rn = 1),
    reasoning_price = (SELECT r.reasoning_price FROM tmp_ranked_prices r WHERE r.product_id = pm.id AND r.rn = 1),
    cache_read_price = (SELECT r.cache_read_price FROM tmp_ranked_prices r WHERE r.product_id = pm.id AND r.rn = 1),
    cache_write_price = (SELECT r.cache_write_price FROM tmp_ranked_prices r WHERE r.product_id = pm.id AND r.rn = 1),
    input_audio_price = (SELECT r.input_audio_price FROM tmp_ranked_prices r WHERE r.product_id = pm.id AND r.rn = 1),
    output_audio_price = (SELECT r.output_audio_price FROM tmp_ranked_prices r WHERE r.product_id = pm.id AND r.rn = 1)
WHERE EXISTS (SELECT 1 FROM tmp_ranked_prices r WHERE r.product_id = pm.id AND r.rn = 1);

DROP TABLE tmp_ranked_prices;

-- 4. 从 model_metadata.product_id 迁移关系到 product_model_metadata
-- 使用 MERGE INTO 兼容 H2 和 PostgreSQL
MERGE INTO product_model_metadata (product_id, model_id, created_at, updated_at)
KEY (product_id, model_id)
SELECT m.product_id, m.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM model_metadata m
WHERE m.product_id IS NOT NULL;

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

-- 7. products 新增定价列（与 ProductDo 7 个定价字段对齐）
ALTER TABLE products ADD COLUMN input_price DECIMAL(12,6);
ALTER TABLE products ADD COLUMN output_price DECIMAL(12,6);
ALTER TABLE products ADD COLUMN reasoning_price DECIMAL(12,6);
ALTER TABLE products ADD COLUMN cache_read_price DECIMAL(12,6);
ALTER TABLE products ADD COLUMN cache_write_price DECIMAL(12,6);
ALTER TABLE products ADD COLUMN input_audio_price DECIMAL(12,6);
ALTER TABLE products ADD COLUMN output_audio_price DECIMAL(12,6);

-- 8. 从 models 迁移定价到 products
-- models 表只有 input_price 和 output_price，其他定价列通过元数据同步填充
DROP TABLE IF EXISTS tmp_ranked_models;
CREATE TABLE tmp_ranked_models AS
SELECT m.provider_id, m.input_price, m.output_price,
       ROW_NUMBER() OVER (PARTITION BY m.provider_id ORDER BY m.id) AS rn
FROM models m
WHERE m.input_price IS NOT NULL;

UPDATE products p
SET input_price = (SELECT r.input_price FROM tmp_ranked_models r WHERE r.provider_id = p.provider_id AND r.rn = 1),
    output_price = (SELECT r.output_price FROM tmp_ranked_models r WHERE r.provider_id = p.provider_id AND r.rn = 1)
WHERE EXISTS (SELECT 1 FROM tmp_ranked_models r WHERE r.provider_id = p.provider_id AND r.rn = 1);

DROP TABLE tmp_ranked_models;

-- 9. 关联数据迁移由应用层 MetadataSyncService 处理
-- products.models JSON 数组的数据将在元数据同步时自动建立关联

-- 10. products 删除 models 列
ALTER TABLE products DROP COLUMN IF EXISTS models;

-- 11. models 删除定价列
ALTER TABLE models DROP COLUMN IF EXISTS input_price;
ALTER TABLE models DROP COLUMN IF EXISTS output_price;