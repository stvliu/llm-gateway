-- ============================================
-- V16: 添加产品相关表
-- ============================================

-- 1. 创建产品表
CREATE TABLE products (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    provider_id BIGINT NOT NULL REFERENCES providers(id),
    name VARCHAR(128) NOT NULL,
    product_type VARCHAR(32) NOT NULL DEFAULT 'pay_as_you_go',
    models JSONB DEFAULT '[]'::jsonb,
    endpoints JSONB DEFAULT '{}'::jsonb,
    quota_limit BIGINT,
    state VARCHAR(16) DEFAULT 'active',
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    CONSTRAINT uk_products_provider_name UNIQUE (provider_id, name)
);

COMMENT ON TABLE products IS '产品表';
COMMENT ON COLUMN products.provider_id IS '供应商 ID';
COMMENT ON COLUMN products.name IS '产品名称';
COMMENT ON COLUMN products.product_type IS '产品类型：pay_as_you_go, subscription_coding, subscription_token';
COMMENT ON COLUMN products.models IS '模型列表 JSONB';
COMMENT ON COLUMN products.endpoints IS '端点映射 JSONB';
COMMENT ON COLUMN products.quota_limit IS '额度限制（Token 数）';
COMMENT ON COLUMN products.state IS '状态：active, inactive, deleted';

-- 2. 创建产品 API Key 表
CREATE TABLE product_api_keys (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    name VARCHAR(128),
    api_key_encrypted TEXT NOT NULL,
    weight INT DEFAULT 1,
    priority INT DEFAULT 1,
    state VARCHAR(16) DEFAULT 'active',
    last_used_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

COMMENT ON TABLE product_api_keys IS '产品 API Key 表';
COMMENT ON COLUMN product_api_keys.product_id IS '产品 ID';
COMMENT ON COLUMN product_api_keys.api_key_encrypted IS '加密存储的 API Key';
COMMENT ON COLUMN product_api_keys.weight IS '负载均衡权重';
COMMENT ON COLUMN product_api_keys.priority IS '故障转移优先级（数值越小优先级越高）';

-- 3. 创建索引
CREATE INDEX idx_products_provider ON products(provider_id);
CREATE INDEX idx_products_type ON products(product_type);
CREATE INDEX idx_products_state ON products(state);
CREATE INDEX idx_product_api_keys_product ON product_api_keys(product_id);
CREATE INDEX idx_product_api_keys_state ON product_api_keys(state);

-- 4. 为 models JSONB 创建 GIN 索引（支持 @> 查询）
CREATE INDEX idx_products_models ON products USING GIN (models);

-- 5. Model 表新增 product_id 字段
ALTER TABLE models ADD COLUMN product_id BIGINT REFERENCES products(id);
CREATE INDEX idx_models_product ON models(product_id);

-- 6. 自动迁移：为每个 Provider 创建默认产品
INSERT INTO products (provider_id, name, product_type, state)
SELECT id, name || '-default', 'pay_as_you_go', 'active'
FROM providers
WHERE state = 'active';

-- 7. 自动迁移：关联 Model 到默认产品
UPDATE models m
SET product_id = p.id
FROM products p
WHERE m.provider_id = p.provider_id
  AND p.name LIKE '%-default'
  AND m.product_id IS NULL;

-- 8. 自动迁移：ProviderApiKey 迁移到 ProductApiKey
INSERT INTO product_api_keys (product_id, name, api_key_encrypted, weight, priority, state, last_used_at, created_at)
SELECT
    p.id,
    pak.key_name,
    pak.api_key_encrypted,
    COALESCE(pak.weight, 1),
    COALESCE(pak.priority, 1),
    LOWER(pak.state)::VARCHAR,
    pak.last_used_at,
    pak.created_at
FROM provider_api_keys pak
JOIN products p ON p.provider_id = pak.provider_id
WHERE p.name LIKE '%-default'
  AND pak.state = 'ACTIVE';

-- 9. 创建更新时间触发器
CREATE TRIGGER products_updated_at
    BEFORE UPDATE ON products
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at();

CREATE TRIGGER product_api_keys_updated_at
    BEFORE UPDATE ON product_api_keys
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at();
