# Product Layer Implementation Plan (Phase 1)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 引入 Product 概念，在 Provider 和 Model 之间增加产品层，支持产品类型区分、多协议端点和产品级密钥管理。

**Architecture:** 采用增量重构策略，新增 Product/ProductApiKey 实体，保留现有 Provider/ProviderApiKey 代码，通过数据迁移实现平滑过渡。新实体遵循现有 COLA Light 架构模式。

**Tech Stack:** Java 21, Spring Boot 3.5.x, JPA, PostgreSQL, Flyway

---

## File Structure

```
domain/product/
├── entity/
│   ├── Product.java              # 产品实体
│   └── ProductApiKey.java        # 产品密钥实体
├── enums/
│   ├── ProductType.java          # 产品类型枚举
│   └── ProductState.java         # 产品状态枚举
├── gateway/
│   ├── ProductGateway.java       # 产品 Gateway 接口
│   └── ProductApiKeyGateway.java # 产品密钥 Gateway 接口
├── service/
│   └── ProductDomainService.java # 产品管理服务
└── exception/
    └── ProductNotFoundException.java

infrastructure/product/
└── gateway/
    ├── database/
    │   ├── dataobject/
    │   │   ├── ProductDo.java
    │   │   └── ProductApiKeyDo.java
    │   └── repository/
    │       ├── ProductRepository.java
    │       └── ProductApiKeyRepository.java
    ├── ProductGatewayImpl.java
    └── ProductApiKeyGatewayImpl.java

application/product/
├── ProductService.java           # 管理服务接口
├── ProductServiceImpl.java       # 管理服务实现
└── dto/
    ├── ProductRequest.java
    ├── ProductResponse.java
    ├── ProductApiKeyRequest.java
    └── ProductApiKeyResponse.java

adapter/api/
└── ProductController.java        # REST 控制器

db/migration/
└── V16__add_product_tables.sql   # 数据库迁移
```

---

## Task 1: Product 枚举定义

**Files:**
- Create: `gateway-boot/src/main/java/com/codingas/gateway/domain/product/enums/ProductType.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/domain/product/enums/ProductState.java`

- [ ] **Step 1: 创建 ProductType 枚举**

```java
package com.codingas.gateway.domain.product.enums;

/**
 * 产品类型枚举
 *
 * <p>定义供应商提供的产品计费类型。</p>
 */
public enum ProductType {

    /** 按量计费产品 */
    PAY_AS_YOU_GO("pay_as_you_go", "按量计费"),

    /** 订阅制 Coding Plan */
    SUBSCRIPTION_CODING("subscription_coding", "Coding Plan"),

    /** 订阅制 Token Plan */
    SUBSCRIPTION_TOKEN("subscription_token", "Token Plan");

    private final String code;
    private final String displayName;

    ProductType(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static ProductType fromCode(String code) {
        for (ProductType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown product type: " + code);
    }
}
```

- [ ] **Step 2: 创建 ProductState 枚举**

```java
package com.codingas.gateway.domain.product.enums;

/**
 * 产品状态枚举
 */
public enum ProductState {

    /** 活跃状态，可正常使用 */
    ACTIVE("active"),

    /** 已停用，暂停服务 */
    INACTIVE("inactive"),

    /** 已删除 */
    DELETED("deleted");

    private final String code;

    ProductState(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public boolean isAvailable() {
        return this == ACTIVE;
    }

    public static ProductState fromCode(String code) {
        for (ProductState state : values()) {
            if (state.code.equals(code)) {
                return state;
            }
        }
        throw new IllegalArgumentException("Unknown product state: " + code);
    }
}
```

- [ ] **Step 3: 提交枚举定义**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/domain/product/enums/
git commit -m "feat(product): add ProductType and ProductState enums"
```

---

## Task 2: Product 实体定义

**Files:**
- Create: `gateway-boot/src/main/java/com/codingas/gateway/domain/product/entity/Product.java`

- [ ] **Step 1: 创建 Product 实体**

```java
package com.codingas.gateway.domain.product.entity;

import com.codingas.gateway.common.entity.BaseEntity;
import com.codingas.gateway.common.entity.DomainEntity;
import com.codingas.gateway.domain.product.enums.ProductState;
import com.codingas.gateway.domain.product.enums.ProductType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

/**
 * 产品实体
 *
 * <p>表示供应商提供的计费产品，包含一组模型和访问端点。</p>
 * <p>一个供应商可以有多个产品（如按量计费、Coding Plan、Token Plan）。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@DomainEntity
@Slf4j
public class Product extends BaseEntity {

    /** 关联的供应商 ID */
    private Long providerId;

    /** 供应商名称（冗余，便于显示） */
    private String providerName;

    /** 产品名称 */
    private String name;

    /** 产品类型 */
    private ProductType productType;

    /** 产品包含的模型列表 */
    private List<String> models;

    /** 多协议端点映射，key 为协议名，value 为 Base URL */
    private Map<String, String> endpoints;

    /** 额度限制（Token 数），订阅产品专用 */
    private Long quotaLimit;

    /** 产品状态 */
    private ProductState state = ProductState.ACTIVE;

    /**
     * 检查产品是否可用
     */
    public boolean isAvailable() {
        return ProductState.ACTIVE.equals(state);
    }

    /**
     * 检查产品是否包含指定模型
     */
    public boolean containsModel(String modelName) {
        return models != null && models.contains(modelName);
    }

    /**
     * 获取指定协议的端点
     */
    public String getEndpoint(String protocol) {
        if (endpoints == null) {
            return null;
        }
        return endpoints.get(protocol);
    }

    /**
     * 获取默认端点（优先 openai，其次任意一个）
     */
    public String getDefaultEndpoint() {
        if (endpoints == null || endpoints.isEmpty()) {
            return null;
        }
        if (endpoints.containsKey("openai")) {
            return endpoints.get("openai");
        }
        return endpoints.values().iterator().next();
    }
}
```

- [ ] **Step 2: 提交 Product 实体**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/domain/product/entity/Product.java
git commit -m "feat(product): add Product entity"
```

---

## Task 3: ProductApiKey 实体定义

**Files:**
- Create: `gateway-boot/src/main/java/com/codingas/gateway/domain/product/entity/ProductApiKey.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/domain/product/enums/ProductApiKeyState.java`

- [ ] **Step 1: 创建 ProductApiKeyState 枚举**

```java
package com.codingas.gateway.domain.product.enums;

/**
 * 产品 API Key 状态枚举
 */
public enum ProductApiKeyState {

    /** 活跃状态 */
    ACTIVE("active"),

    /** 已停用 */
    INACTIVE("inactive"),

    /** 已删除 */
    DELETED("deleted");

    private final String code;

    ProductApiKeyState(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public boolean isAvailable() {
        return this == ACTIVE;
    }

    public static ProductApiKeyState fromCode(String code) {
        for (ProductApiKeyState state : values()) {
            if (state.code.equals(code)) {
                return state;
            }
        }
        throw new IllegalArgumentException("Unknown product api key state: " + code);
    }
}
```

- [ ] **Step 2: 创建 ProductApiKey 实体**

```java
package com.codingas.gateway.domain.product.entity;

import com.codingas.gateway.common.entity.BaseEntity;
import com.codingas.gateway.common.entity.DomainEntity;
import com.codingas.gateway.domain.product.enums.ProductApiKeyState;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;

/**
 * 产品 API Key 实体
 *
 * <p>供应商侧认证密钥，用于调用供应商 API。</p>
 * <p>一个产品可配置多个密钥，支持密钥轮换、负载均衡和故障转移。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@DomainEntity
@Slf4j
public class ProductApiKey extends BaseEntity {

    /** 关联的产品 ID */
    private Long productId;

    /** 密钥名称 */
    private String name;

    /** 加密存储的 API Key */
    private String apiKeyEncrypted;

    /** 负载均衡权重 */
    private Integer weight = 1;

    /** 故障转移优先级（数值越小优先级越高） */
    private Integer priority = 1;

    /** 密钥状态 */
    private ProductApiKeyState state = ProductApiKeyState.ACTIVE;

    /** 最后使用时间 */
    private Instant lastUsedAt;

    /**
     * 检查密钥是否可用
     */
    public boolean isAvailable() {
        return state.isAvailable();
    }
}
```

- [ ] **Step 3: 提交 ProductApiKey 实体**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/domain/product/
git commit -m "feat(product): add ProductApiKey entity and state enum"
```

---

## Task 4: Product 异常定义

**Files:**
- Create: `gateway-boot/src/main/java/com/codingas/gateway/domain/product/exception/ProductNotFoundException.java`

- [ ] **Step 1: 创建 ProductNotFoundException**

```java
package com.codingas.gateway.domain.product.exception;

import com.codingas.gateway.common.exception.GatewayException;

/**
 * 产品未找到异常
 */
public class ProductNotFoundException extends GatewayException {

    public ProductNotFoundException(Long productId) {
        super("Product not found: id=" + productId);
    }

    public ProductNotFoundException(String message) {
        super(message);
    }
}
```

- [ ] **Step 2: 提交异常定义**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/domain/product/exception/
git commit -m "feat(product): add ProductNotFoundException"
```

---

## Task 5: ProductGateway 接口定义

**Files:**
- Create: `gateway-boot/src/main/java/com/codingas/gateway/domain/product/gateway/ProductGateway.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/domain/product/gateway/ProductApiKeyGateway.java`

- [ ] **Step 1: 创建 ProductGateway 接口**

```java
package com.codingas.gateway.domain.product.gateway;

import com.codingas.gateway.domain.product.entity.Product;
import com.codingas.gateway.domain.product.enums.ProductType;

import java.util.List;
import java.util.Optional;

/**
 * 产品 Gateway 接口
 */
public interface ProductGateway {

    /**
     * 保存产品
     */
    Product save(Product product);

    /**
     * 根据 ID 查找产品
     */
    Optional<Product> findById(Long id);

    /**
     * 根据供应商 ID 查找所有产品
     */
    List<Product> findByProviderId(Long providerId);

    /**
     * 根据供应商 ID 和产品类型查找产品
     */
    List<Product> findByProviderIdAndType(Long providerId, ProductType type);

    /**
     * 查找包含指定模型的产品
     */
    List<Product> findByModel(String modelName);

    /**
     * 查找所有活跃产品
     */
    List<Product> findAllActive();

    /**
     * 删除产品
     */
    void deleteById(Long id);

    /**
     * 检查产品名称是否已存在
     */
    boolean existsByProviderIdAndName(Long providerId, String name);
}
```

- [ ] **Step 2: 创建 ProductApiKeyGateway 接口**

```java
package com.codingas.gateway.domain.product.gateway;

import com.codingas.gateway.domain.product.entity.ProductApiKey;

import java.util.List;
import java.util.Optional;

/**
 * 产品 API Key Gateway 接口
 */
public interface ProductApiKeyGateway {

    /**
     * 保存密钥
     */
    ProductApiKey save(ProductApiKey apiKey);

    /**
     * 根据 ID 查找密钥
     */
    Optional<ProductApiKey> findById(Long id);

    /**
     * 根据产品 ID 查找所有活跃密钥
     */
    List<ProductApiKey> findActiveByProductId(Long productId);

    /**
     * 根据产品 ID 查找默认密钥（优先级最高的活跃密钥）
     */
    Optional<ProductApiKey> findDefaultByProductId(Long productId);

    /**
     * 更新最后使用时间
     */
    void updateLastUsedAt(Long id);

    /**
     * 删除密钥
     */
    void deleteById(Long id);

    /**
     * 统计产品的活跃密钥数量
     */
    long countActiveByProductId(Long productId);
}
```

- [ ] **Step 3: 提交 Gateway 接口**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/domain/product/gateway/
git commit -m "feat(product): add ProductGateway and ProductApiKeyGateway interfaces"
```

---

## Task 6: 数据库迁移脚本

**Files:**
- Create: `gateway-boot/src/main/resources/db/migration/V16__add_product_tables.sql`

- [ ] **Step 1: 创建数据库迁移脚本**

```sql
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
```

- [ ] **Step 2: 提交迁移脚本**

```bash
git add gateway-boot/src/main/resources/db/migration/V16__add_product_tables.sql
git commit -m "feat(product): add database migration for product tables"
```

---

## Task 7: Product 数据对象和 Repository

**Files:**
- Create: `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/product/gateway/database/dataobject/ProductDo.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/product/gateway/database/dataobject/ProductApiKeyDo.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/product/gateway/database/repository/ProductRepository.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/product/gateway/database/repository/ProductApiKeyRepository.java`

- [ ] **Step 1: 创建 ProductDo**

```java
package com.codingas.gateway.infrastructure.product.gateway.database.dataobject;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 产品数据对象
 */
@Data
@Entity
@Table(name = "products", uniqueConstraints = {
    @UniqueConstraint(name = "uk_products_provider_name", columnNames = {"provider_id", "name"})
})
public class ProductDo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "provider_id", nullable = false)
    private Long providerId;

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Column(name = "product_type", nullable = false, length = 32)
    private String productType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "models", columnDefinition = "jsonb")
    private List<String> models;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "endpoints", columnDefinition = "jsonb")
    private Map<String, String> endpoints;

    @Column(name = "quota_limit")
    private Long quotaLimit;

    @Column(name = "state", length = 16)
    private String state;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
```

- [ ] **Step 2: 创建 ProductApiKeyDo**

```java
package com.codingas.gateway.infrastructure.product.gateway.database.dataobject;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 产品 API Key 数据对象
 */
@Data
@Entity
@Table(name = "product_api_keys")
public class ProductApiKeyDo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "name", length = 128)
    private String name;

    @Column(name = "api_key_encrypted", columnDefinition = "TEXT", nullable = false)
    private String apiKeyEncrypted;

    @Column(name = "weight")
    private Integer weight = 1;

    @Column(name = "priority")
    private Integer priority = 1;

    @Column(name = "state", length = 16)
    private String state;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
```

- [ ] **Step 3: 创建 ProductRepository**

```java
package com.codingas.gateway.infrastructure.product.gateway.database.repository;

import com.codingas.gateway.infrastructure.product.gateway.database.dataobject.ProductDo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 产品 Repository
 */
@Repository
public interface ProductRepository extends JpaRepository<ProductDo, Long> {

    List<ProductDo> findByProviderId(Long providerId);

    List<ProductDo> findByProviderIdAndProductType(Long providerId, String productType);

    @Query("SELECT p FROM ProductDo p WHERE p.state = 'active'")
    List<ProductDo> findAllActive();

    @Query("SELECT p FROM ProductDo p WHERE :modelName MEMBER OF p.models AND p.state = 'active'")
    List<ProductDo> findByModel(@Param("modelName") String modelName);

    boolean existsByProviderIdAndName(Long providerId, String name);
}
```

- [ ] **Step 4: 创建 ProductApiKeyRepository**

```java
package com.codingas.gateway.infrastructure.product.gateway.database.repository;

import com.codingas.gateway.infrastructure.product.gateway.database.dataobject.ProductApiKeyDo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 产品 API Key Repository
 */
@Repository
public interface ProductApiKeyRepository extends JpaRepository<ProductApiKeyDo, Long> {

    @Query("SELECT k FROM ProductApiKeyDo k WHERE k.productId = :productId AND k.state = 'active' ORDER BY k.priority ASC")
    List<ProductApiKeyDo> findActiveByProductId(@Param("productId") Long productId);

    @Query("SELECT k FROM ProductApiKeyDo k WHERE k.productId = :productId AND k.state = 'active' ORDER BY k.priority ASC LIMIT 1")
    Optional<ProductApiKeyDo> findDefaultByProductId(@Param("productId") Long productId);

    @Modifying
    @Query("UPDATE ProductApiKeyDo k SET k.lastUsedAt = :lastUsedAt WHERE k.id = :id")
    void updateLastUsedAt(@Param("id") Long id, @Param("lastUsedAt") LocalDateTime lastUsedAt);

    long countByProductIdAndState(Long productId, String state);
}
```

- [ ] **Step 5: 提交数据层代码**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/infrastructure/product/
git commit -m "feat(product): add ProductDo, ProductApiKeyDo and repositories"
```

---

## Task 8: ProductGateway 实现

**Files:**
- Create: `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/product/gateway/ProductGatewayImpl.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/product/gateway/ProductApiKeyGatewayImpl.java`

- [ ] **Step 1: 创建 ProductGatewayImpl**

```java
package com.codingas.gateway.infrastructure.product.gateway;

import com.codingas.gateway.domain.product.entity.Product;
import com.codingas.gateway.domain.product.enums.ProductState;
import com.codingas.gateway.domain.product.enums.ProductType;
import com.codingas.gateway.domain.product.gateway.ProductGateway;
import com.codingas.gateway.infrastructure.product.gateway.database.dataobject.ProductDo;
import com.codingas.gateway.infrastructure.product.gateway.database.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 产品 Gateway 实现
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ProductGatewayImpl implements ProductGateway {

    private final ProductRepository productRepository;

    @Override
    public Product save(Product product) {
        ProductDo dataObject = toDataObject(product);
        if (product.getId() == null) {
            dataObject.setCreatedAt(LocalDateTime.now());
        }
        dataObject.setUpdatedAt(LocalDateTime.now());
        ProductDo saved = productRepository.save(dataObject);
        return toEntity(saved);
    }

    @Override
    public Optional<Product> findById(Long id) {
        return productRepository.findById(id).map(this::toEntity);
    }

    @Override
    public List<Product> findByProviderId(Long providerId) {
        return productRepository.findByProviderId(providerId).stream()
            .map(this::toEntity)
            .toList();
    }

    @Override
    public List<Product> findByProviderIdAndType(Long providerId, ProductType type) {
        return productRepository.findByProviderIdAndProductType(providerId, type.getCode()).stream()
            .map(this::toEntity)
            .toList();
    }

    @Override
    public List<Product> findByModel(String modelName) {
        return productRepository.findByModel(modelName).stream()
            .map(this::toEntity)
            .toList();
    }

    @Override
    public List<Product> findAllActive() {
        return productRepository.findAllActive().stream()
            .map(this::toEntity)
            .toList();
    }

    @Override
    public void deleteById(Long id) {
        productRepository.deleteById(id);
    }

    @Override
    public boolean existsByProviderIdAndName(Long providerId, String name) {
        return productRepository.existsByProviderIdAndName(providerId, name);
    }

    private Product toEntity(ProductDo dataObject) {
        Product entity = new Product();
        entity.setId(dataObject.getId());
        entity.setProviderId(dataObject.getProviderId());
        entity.setName(dataObject.getName());
        entity.setProductType(ProductType.fromCode(dataObject.getProductType()));
        entity.setModels(dataObject.getModels());
        entity.setEndpoints(dataObject.getEndpoints());
        entity.setQuotaLimit(dataObject.getQuotaLimit());
        entity.setState(ProductState.fromCode(dataObject.getState()));
        return entity;
    }

    private ProductDo toDataObject(Product entity) {
        ProductDo dataObject = new ProductDo();
        dataObject.setId(entity.getId());
        dataObject.setProviderId(entity.getProviderId());
        dataObject.setName(entity.getName());
        dataObject.setProductType(entity.getProductType().getCode());
        dataObject.setModels(entity.getModels());
        dataObject.setEndpoints(entity.getEndpoints());
        dataObject.setQuotaLimit(entity.getQuotaLimit());
        dataObject.setState(entity.getState().getCode());
        return dataObject;
    }
}
```

- [ ] **Step 2: 创建 ProductApiKeyGatewayImpl**

```java
package com.codingas.gateway.infrastructure.product.gateway;

import com.codingas.gateway.domain.product.entity.ProductApiKey;
import com.codingas.gateway.domain.product.enums.ProductApiKeyState;
import com.codingas.gateway.domain.product.gateway.ProductApiKeyGateway;
import com.codingas.gateway.infrastructure.product.gateway.database.dataobject.ProductApiKeyDo;
import com.codingas.gateway.infrastructure.product.gateway.database.repository.ProductApiKeyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

/**
 * 产品 API Key Gateway 实现
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ProductApiKeyGatewayImpl implements ProductApiKeyGateway {

    private final ProductApiKeyRepository productApiKeyRepository;

    @Override
    public ProductApiKey save(ProductApiKey apiKey) {
        ProductApiKeyDo dataObject = toDataObject(apiKey);
        if (apiKey.getId() == null) {
            dataObject.setCreatedAt(LocalDateTime.now());
        }
        dataObject.setUpdatedAt(LocalDateTime.now());
        ProductApiKeyDo saved = productApiKeyRepository.save(dataObject);
        return toEntity(saved);
    }

    @Override
    public Optional<ProductApiKey> findById(Long id) {
        return productApiKeyRepository.findById(id).map(this::toEntity);
    }

    @Override
    public List<ProductApiKey> findActiveByProductId(Long productId) {
        return productApiKeyRepository.findActiveByProductId(productId).stream()
            .map(this::toEntity)
            .toList();
    }

    @Override
    public Optional<ProductApiKey> findDefaultByProductId(Long productId) {
        return productApiKeyRepository.findDefaultByProductId(productId)
            .map(this::toEntity);
    }

    @Override
    public void updateLastUsedAt(Long id) {
        productApiKeyRepository.updateLastUsedAt(id, LocalDateTime.now());
    }

    @Override
    public void deleteById(Long id) {
        productApiKeyRepository.deleteById(id);
    }

    @Override
    public long countActiveByProductId(Long productId) {
        return productApiKeyRepository.countByProductIdAndState(productId, "active");
    }

    private ProductApiKey toEntity(ProductApiKeyDo dataObject) {
        ProductApiKey entity = new ProductApiKey();
        entity.setId(dataObject.getId());
        entity.setProductId(dataObject.getProductId());
        entity.setName(dataObject.getName());
        entity.setApiKeyEncrypted(dataObject.getApiKeyEncrypted());
        entity.setWeight(dataObject.getWeight());
        entity.setPriority(dataObject.getPriority());
        entity.setState(ProductApiKeyState.fromCode(dataObject.getState()));
        if (dataObject.getLastUsedAt() != null) {
            entity.setLastUsedAt(dataObject.getLastUsedAt().atZone(ZoneOffset.UTC).toInstant());
        }
        return entity;
    }

    private ProductApiKeyDo toDataObject(ProductApiKey entity) {
        ProductApiKeyDo dataObject = new ProductApiKeyDo();
        dataObject.setId(entity.getId());
        dataObject.setProductId(entity.getProductId());
        dataObject.setName(entity.getName());
        dataObject.setApiKeyEncrypted(entity.getApiKeyEncrypted());
        dataObject.setWeight(entity.getWeight());
        dataObject.setPriority(entity.getPriority());
        dataObject.setState(entity.getState().getCode());
        if (entity.getLastUsedAt() != null) {
            dataObject.setLastUsedAt(LocalDateTime.ofInstant(entity.getLastUsedAt(), ZoneOffset.UTC));
        }
        return dataObject;
    }
}
```

- [ ] **Step 3: 提交 Gateway 实现**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/infrastructure/product/gateway/
git commit -m "feat(product): implement ProductGateway and ProductApiKeyGateway"
```

---

## Task 9: Product 管理服务

**Files:**
- Create: `gateway-boot/src/main/java/com/codingas/gateway/application/product/dto/ProductRequest.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/application/product/dto/ProductResponse.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/application/product/ProductService.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/application/product/ProductServiceImpl.java`

- [ ] **Step 1: 创建 ProductRequest DTO**

```java
package com.codingas.gateway.application.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 产品创建/更新请求
 */
@Data
public class ProductRequest {

    @NotNull(message = "供应商 ID 不能为空")
    private Long providerId;

    @NotBlank(message = "产品名称不能为空")
    private String name;

    @NotBlank(message = "产品类型不能为空")
    private String productType;

    private List<String> models;

    private Map<String, String> endpoints;

    private Long quotaLimit;
}
```

- [ ] **Step 2: 创建 ProductResponse DTO**

```java
package com.codingas.gateway.application.product.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 产品响应
 */
@Data
public class ProductResponse {

    private Long id;

    private Long providerId;

    private String providerName;

    private String name;

    private String productType;

    private List<String> models;

    private Map<String, String> endpoints;

    private Long quotaLimit;

    private String state;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
```

- [ ] **Step 3: 创建 ProductService 接口**

```java
package com.codingas.gateway.application.product;

import com.codingas.gateway.application.product.dto.ProductRequest;
import com.codingas.gateway.application.product.dto.ProductResponse;
import com.codingas.gateway.domain.product.enums.ProductType;

import java.util.List;

/**
 * 产品管理服务接口
 */
public interface ProductService {

    /**
     * 创建产品
     */
    ProductResponse create(ProductRequest request);

    /**
     * 更新产品
     */
    ProductResponse update(Long id, ProductRequest request);

    /**
     * 获取产品详情
     */
    ProductResponse getById(Long id);

    /**
     * 获取供应商的所有产品
     */
    List<ProductResponse> getByProviderId(Long providerId);

    /**
     * 获取供应商指定类型的产品
     */
    List<ProductResponse> getByProviderIdAndType(Long providerId, ProductType type);

    /**
     * 删除产品
     */
    void delete(Long id);
}
```

- [ ] **Step 4: 创建 ProductServiceImpl**

```java
package com.codingas.gateway.application.product;

import com.codingas.gateway.application.product.dto.ProductRequest;
import com.codingas.gateway.application.product.dto.ProductResponse;
import com.codingas.gateway.domain.product.entity.Product;
import com.codingas.gateway.domain.product.enums.ProductType;
import com.codingas.gateway.domain.product.exception.ProductNotFoundException;
import com.codingas.gateway.domain.product.gateway.ProductGateway;
import com.codingas.gateway.domain.model.gateway.ProviderGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 产品管理服务实现
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {

    private final ProductGateway productGateway;
    private final ProviderGateway providerGateway;

    @Override
    @Transactional
    public ProductResponse create(ProductRequest request) {
        // 检查名称是否重复
        if (productGateway.existsByProviderIdAndName(request.getProviderId(), request.getName())) {
            throw new IllegalArgumentException("产品名称已存在: " + request.getName());
        }

        Product product = new Product();
        product.setProviderId(request.getProviderId());
        product.setName(request.getName());
        product.setProductType(ProductType.fromCode(request.getProductType()));
        product.setModels(request.getModels());
        product.setEndpoints(request.getEndpoints());
        product.setQuotaLimit(request.getQuotaLimit());

        // 获取供应商名称
        providerGateway.findById(request.getProviderId())
            .ifPresent(p -> product.setProviderName(p.getName()));

        Product saved = productGateway.save(product);
        log.info("Created product: id={}, name={}", saved.getId(), saved.getName());

        return toResponse(saved);
    }

    @Override
    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = productGateway.findById(id)
            .orElseThrow(() -> new ProductNotFoundException(id));

        // 如果名称变更，检查是否重复
        if (!product.getName().equals(request.getName())) {
            if (productGateway.existsByProviderIdAndName(request.getProviderId(), request.getName())) {
                throw new IllegalArgumentException("产品名称已存在: " + request.getName());
            }
        }

        product.setProviderId(request.getProviderId());
        product.setName(request.getName());
        product.setProductType(ProductType.fromCode(request.getProductType()));
        product.setModels(request.getModels());
        product.setEndpoints(request.getEndpoints());
        product.setQuotaLimit(request.getQuotaLimit());

        Product saved = productGateway.save(product);
        log.info("Updated product: id={}", saved.getId());

        return toResponse(saved);
    }

    @Override
    public ProductResponse getById(Long id) {
        Product product = productGateway.findById(id)
            .orElseThrow(() -> new ProductNotFoundException(id));
        return toResponse(product);
    }

    @Override
    public List<ProductResponse> getByProviderId(Long providerId) {
        return productGateway.findByProviderId(providerId).stream()
            .map(this::toResponse)
            .toList();
    }

    @Override
    public List<ProductResponse> getByProviderIdAndType(Long providerId, ProductType type) {
        return productGateway.findByProviderIdAndType(providerId, type).stream()
            .map(this::toResponse)
            .toList();
    }

    @Override
    @Transactional
    public void delete(Long id) {
        productGateway.deleteById(id);
        log.info("Deleted product: id={}", id);
    }

    private ProductResponse toResponse(Product product) {
        ProductResponse response = new ProductResponse();
        response.setId(product.getId());
        response.setProviderId(product.getProviderId());
        response.setProviderName(product.getProviderName());
        response.setName(product.getName());
        response.setProductType(product.getProductType().getCode());
        response.setModels(product.getModels());
        response.setEndpoints(product.getEndpoints());
        response.setQuotaLimit(product.getQuotaLimit());
        response.setState(product.getState().getCode());
        return response;
    }
}
```

- [ ] **Step 5: 提交管理服务**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/application/product/
git commit -m "feat(product): add ProductService and DTOs"
```

---

## Task 10: Product REST 控制器

**Files:**
- Create: `gateway-boot/src/main/java/com/codingas/gateway/adapter/api/ProductController.java`

- [ ] **Step 1: 创建 ProductController**

```java
package com.codingas.gateway.adapter.api;

import com.codingas.gateway.application.product.ProductService;
import com.codingas.gateway.application.product.dto.ProductRequest;
import com.codingas.gateway.application.product.dto.ProductResponse;
import com.codingas.gateway.domain.product.enums.ProductType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 产品管理 REST 控制器
 */
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    /**
     * 创建产品
     */
    @PostMapping
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest request) {
        ProductResponse response = productService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 更新产品
     */
    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request) {
        ProductResponse response = productService.update(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 获取产品详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getById(@PathVariable Long id) {
        ProductResponse response = productService.getById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * 获取供应商的所有产品
     */
    @GetMapping
    public ResponseEntity<List<ProductResponse>> getByProviderId(
            @RequestParam Long providerId,
            @RequestParam(required = false) String productType) {
        
        List<ProductResponse> responses;
        if (productType != null) {
            responses = productService.getByProviderIdAndType(
                providerId, ProductType.fromCode(productType));
        } else {
            responses = productService.getByProviderId(providerId);
        }
        return ResponseEntity.ok(responses);
    }

    /**
     * 删除产品
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
```

- [ ] **Step 2: 提交控制器**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/adapter/api/ProductController.java
git commit -m "feat(product): add ProductController REST API"
```

---

## Task 11: 单元测试 - ProductGateway

**Files:**
- Create: `gateway-boot/src/test/java/com/codingas/gateway/infrastructure/product/gateway/ProductGatewayImplTest.java`

- [ ] **Step 1: 创建测试类**

```java
package com.codingas.gateway.infrastructure.product.gateway;

import com.codingas.gateway.domain.product.entity.Product;
import com.codingas.gateway.domain.product.enums.ProductType;
import com.codingas.gateway.domain.product.enums.ProductState;
import com.codingas.gateway.infrastructure.product.gateway.database.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ProductGateway 单元测试
 */
@DataJpaTest
@Import(ProductGatewayImpl.class)
class ProductGatewayImplTest {

    @Autowired
    private ProductGatewayImpl productGateway;

    @Autowired
    private ProductRepository productRepository;

    private Product testProduct;

    @BeforeEach
    void setUp() {
        testProduct = new Product();
        testProduct.setProviderId(1L);
        testProduct.setName("test-product");
        testProduct.setProductType(ProductType.PAY_AS_YOU_GO);
        testProduct.setModels(List.of("model-1", "model-2"));
        testProduct.setEndpoints(Map.of("openai", "https://api.example.com/v1"));
        testProduct.setState(ProductState.ACTIVE);
    }

    @Test
    @DisplayName("should save and find product by id")
    void save_andFindById() {
        Product saved = productGateway.save(testProduct);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("test-product");
        assertThat(saved.getProductType()).isEqualTo(ProductType.PAY_AS_YOU_GO);

        Optional<Product> found = productGateway.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("test-product");
    }

    @Test
    @DisplayName("should find products by provider id")
    void findByProviderId() {
        productGateway.save(testProduct);

        List<Product> products = productGateway.findByProviderId(1L);

        assertThat(products).hasSize(1);
        assertThat(products.get(0).getName()).isEqualTo("test-product");
    }

    @Test
    @DisplayName("should find products by provider id and type")
    void findByProviderIdAndType() {
        productGateway.save(testProduct);

        List<Product> products = productGateway.findByProviderIdAndType(1L, ProductType.PAY_AS_YOU_GO);

        assertThat(products).hasSize(1);
    }

    @Test
    @DisplayName("should check if product name exists")
    void existsByProviderIdAndName() {
        productGateway.save(testProduct);

        boolean exists = productGateway.existsByProviderIdAndName(1L, "test-product");
        boolean notExists = productGateway.existsByProviderIdAndName(1L, "other-product");

        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    @DisplayName("should delete product by id")
    void deleteById() {
        Product saved = productGateway.save(testProduct);

        productGateway.deleteById(saved.getId());

        Optional<Product> found = productGateway.findById(saved.getId());
        assertThat(found).isEmpty();
    }
}
```

- [ ] **Step 2: 运行测试验证**

```bash
cd gateway-boot && ../mvnw test -Dtest=ProductGatewayImplTest
```

Expected: All tests pass

- [ ] **Step 3: 提交测试**

```bash
git add gateway-boot/src/test/java/com/codingas/gateway/infrastructure/product/
git commit -m "test(product): add ProductGatewayImplTest"
```

---

## Task 12: 集成测试 - 产品 API

**Files:**
- Create: `gateway-boot/src/test/java/com/codingas/gateway/adapter/api/ProductControllerIT.java`

- [ ] **Step 1: 创建集成测试**

```java
package com.codingas.gateway.adapter.api;

import com.codingas.gateway.application.product.dto.ProductRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ProductController 集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc
class ProductControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("should create and get product")
    void createAndGetProduct() throws Exception {
        ProductRequest request = new ProductRequest();
        request.setProviderId(1L);
        request.setName("integration-test-product");
        request.setProductType("pay_as_you_go");
        request.setModels(List.of("model-a", "model-b"));
        request.setEndpoints(Map.of("openai", "https://api.test.com/v1"));

        // Create
        String response = mockMvc.perform(post("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.name").value("integration-test-product"))
            .andReturn().getResponse().getContentAsString();

        // Extract ID from response
        Long id = objectMapper.readTree(response).get("id").asLong();

        // Get by ID
        mockMvc.perform(get("/api/v1/products/{id}", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("integration-test-product"));
    }

    @Test
    @DisplayName("should list products by provider")
    void listProductsByProvider() throws Exception {
        mockMvc.perform(get("/api/v1/products")
                .param("providerId", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("should reject invalid product request")
    void rejectInvalidRequest() throws Exception {
        ProductRequest request = new ProductRequest();
        // Missing required fields

        mockMvc.perform(post("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }
}
```

- [ ] **Step 2: 运行集成测试**

```bash
cd gateway-boot && ../mvnw test -Dtest=ProductControllerIT
```

Expected: All tests pass

- [ ] **Step 3: 提交集成测试**

```bash
git add gateway-boot/src/test/java/com/codingas/gateway/adapter/api/ProductControllerIT.java
git commit -m "test(product): add ProductController integration test"
```

---

## Task 13: 验证与文档更新

**Files:**
- Update: `doc/信息架构.md`

- [ ] **Step 1: 运行所有测试**

```bash
cd gateway-boot && ../mvnw test
```

Expected: All tests pass

- [ ] **Step 2: 运行应用验证**

```bash
cd gateway-boot && ../mvnw spring-boot:run
```

Verify:
- Application starts successfully
- Database migration runs
- Products table created
- API endpoint accessible at `/api/v1/products`

- [ ] **Step 3: 最终提交**

```bash
git add -A
git commit -m "feat(product): complete Phase 1 - Product layer implementation

- Add Product and ProductApiKey entities
- Add ProductGateway and ProductApiKeyGateway
- Add ProductService and ProductController
- Add database migration V16
- Migrate existing ProviderApiKey to ProductApiKey
- Add unit and integration tests"
```

---

## Verification Checklist

- [ ] 所有单元测试通过
- [ ] 集成测试通过
- [ ] 应用启动成功
- [ ] 数据库迁移执行成功
- [ ] Product API 端点可访问
- [ ] 现有功能不受影响（Model、Provider API 正常）

---

**Plan Version:** 1.0
**Created:** 2026-05-18
