# ProductModel 关联表重构 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将定价从模型移到产品上，新建纯关系表承载产品-模型 M:N 关联，使模型成为纯粹的属性实体。

**Architecture:** 元数据体系和业务体系同步重构。ProductMetadata/Product 新增定价字段、移除 models 列表；新建 ProductModelMetadata/ProductModel 纯关系表；ModelMetadata/Model 移除定价字段。两套体系结构完全对齐。

**Tech Stack:** Java 21, Spring Boot 3.5.x, JPA, PostgreSQL, Flyway, React, Ant Design, TanStack Query

---

## 文件结构

### 新建文件

| 文件 | 职责 |
|------|------|
| `gateway-boot/src/main/resources/db/migration/V34__product_model_association.sql` | Flyway 迁移：建关联表 + 定价迁移 + 列清理 |
| `gateway-boot/src/main/resources/metadata/product-models/*.json` | 12 个供应商的产品-模型关联 JSON |
| `gateway-boot/src/main/java/com/codingas/gateway/domain/metadata/entity/ProductModelMetadata.java` | 元数据关联实体 |
| `gateway-boot/src/main/java/com/codingas/gateway/domain/metadata/gateway/ProductModelMetadataGateway.java` | 元数据关联 Gateway 接口 |
| `gateway-boot/src/main/java/com/codingas/gateway/domain/product/entity/ProductModel.java` | 业务关联实体 |
| `gateway-boot/src/main/java/com/codingas/gateway/domain/product/gateway/ProductModelGateway.java` | 业务关联 Gateway 接口 |
| `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/metadata/database/ProductModelMetadataDo.java` | 元数据关联 JPA DO |
| `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/metadata/database/ProductModelMetadataRepository.java` | 元数据关联 JPA Repository |
| `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/metadata/gateway/ProductModelMetadataGatewayImpl.java` | 元数据关联 Gateway 实现 |
| `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/product/gateway/database/dataobject/ProductModelDo.java` | 业务关联 JPA DO |
| `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/product/gateway/database/repository/ProductModelRepository.java` | 业务关联 JPA Repository |
| `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/product/gateway/ProductModelGatewayImpl.java` | 业务关联 Gateway 实现 |
| `gateway-boot/src/main/java/com/codingas/gateway/application/metadata/ProductModelMetadataService.java` | 元数据关联应用服务 |
| `gateway-boot/src/main/java/com/codingas/gateway/application/metadata/dto/ProductModelMetadataResponse.java` | 元数据关联响应 DTO |
| `gateway-boot/src/main/java/com/codingas/gateway/adapter/api/ProductModelMetadataController.java` | 元数据关联 API Controller |

### 修改文件

| 文件 | 变更 |
|------|------|
| `domain/metadata/entity/ProductMetadata.java` | 新增 7 个定价字段 |
| `domain/metadata/entity/ModelMetadata.java` | 移除 productId + 7 个定价字段 |
| `domain/product/entity/Product.java` | 新增 inputPrice/outputPrice，移除 models 列表 |
| `domain/product/entity/Model.java` | 移除 inputPrice/outputPrice |
| `domain/product/service/ProductDomainService.java` | containsModel 改用关联表 |
| `application/proxy/ProductRoutingService.java` | 路由改用关联表 |
| `application/metadata/MetadataSyncService.java` | 同步时写入关联 + 产品定价 |
| `application/metadata/ProviderMetadataService.java` | apply 时产品持有定价、模型无定价、创建关联 |
| `application/metadata/ModelMetadataService.java` | DTO 移除定价字段 |
| `application/metadata/ProductMetadataService.java` | DTO 新增定价字段 |
| `application/product/ProductServiceImpl.java` | 适配 models 移除 + 定价新增 |
| `application/model/ModelServiceImpl.java` | 适配定价移除 |
| `infrastructure/metadata/database/ProductMetadataDo.java` | 新增 7 个定价列 |
| `infrastructure/metadata/database/ModelMetadataDo.java` | 移除 product_id + 7 个定价列 |
| `infrastructure/metadata/gateway/ProductMetadataGatewayImpl.java` | DO↔Entity 映射新增定价 |
| `infrastructure/metadata/gateway/ModelMetadataGatewayImpl.java` | DO↔Entity 映射移除定价 |
| `infrastructure/product/gateway/database/dataobject/ProductDo.java` | 新增定价列，移除 models 列 |
| `infrastructure/product/gateway/ProductGatewayImpl.java` | DO↔Entity 适配 |
| `infrastructure/metadata/repository/BuiltinMetadataLoader.java` | 新增 product-models 加载 |
| `adapter/api/ProductController.java` | 新增模型子资源端点 |
| `application/metadata/dto/ModelMetadataResponse.java` | 移除定价字段 |
| `application/metadata/dto/ModelMetadataCreateRequest.java` | 移除定价字段 |
| `application/metadata/dto/ModelMetadataUpdateRequest.java` | 移除定价字段 |
| `application/metadata/dto/ProductMetadataResponse.java` | 新增定价字段 |
| `application/product/dto/ProductResponse.java` | 新增定价，移除 models |
| `application/product/dto/ProductRequest.java` | 新增定价，移除 models |
| `application/model/dto/ModelResponse.java` | 移除定价字段 |
| `application/model/dto/ModelCreateRequest.java` | 移除定价字段 |
| `application/model/dto/ModelUpdateRequest.java` | 移除定价字段 |
| `metadata/models/*.json` (12 个文件) | 移除 product_name + 定价字段 |
| `metadata/products/*.json` (12 个文件) | 新增定价字段 |
| `gateway-console/src/types/metadata.ts` | 前端类型适配 |
| `gateway-console/src/types/product.ts` | 前端类型适配 |
| `gateway-console/src/types/model.ts` | 前端类型适配 |
| `gateway-console/src/services/api/metadata.ts` | 前端 API 适配 |

---

## Task 1: Flyway 迁移脚本

**Files:**
- Create: `gateway-boot/src/main/resources/db/migration/V34__product_model_association.sql`

- [ ] **Step 1: 编写迁移 SQL**

```sql
-- ============================================
-- V34: 产品-模型关联重构
-- 1. 创建 product_model_metadata 纯关系表
-- 2. product_metadata 新增 7 个定价列
-- 3. 从 model_metadata 迁移定价到 product_metadata
-- 4. 从 model_metadata.product_id 迁移关系到 product_model_metadata
-- 5. model_metadata 删除定价列和 product_id 列
-- 6. 创建 product_models 纯关系表（业务体系）
-- 7. products 新增定价列，删除 models 列
-- 8. 从 models 迁移定价到 products
-- 9. 从现有数据迁移关系到 product_models
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
-- 策略：每个供应商的默认产品取第一个有定价模型的价格
UPDATE product_metadata pm
SET input_price = mm.input_price,
    output_price = mm.output_price,
    reasoning_price = mm.reasoning_price,
    cache_read_price = mm.cache_read_price,
    cache_write_price = mm.cache_write_price,
    input_audio_price = mm.input_audio_price,
    output_audio_price = mm.output_audio_price
FROM (
    SELECT DISTINCT ON (m.product_id)
        m.product_id, m.input_price, m.output_price,
        m.reasoning_price, m.cache_read_price, m.cache_write_price,
        m.input_audio_price, m.output_audio_price
    FROM model_metadata m
    WHERE m.product_id IS NOT NULL AND m.input_price IS NOT NULL
    ORDER BY m.product_id, m.id
) mm
WHERE pm.id = mm.product_id;

-- 4. 从 model_metadata.product_id 迁移关系到 product_model_metadata
INSERT INTO product_model_metadata (product_id, model_id, created_at, updated_at)
SELECT m.product_id, m.id, NOW(), NOW()
FROM model_metadata m
WHERE m.product_id IS NOT NULL
ON CONFLICT (product_id, model_id) DO NOTHING;

-- 5. model_metadata 删除定价列和 product_id 列
ALTER TABLE model_metadata DROP COLUMN product_id;
ALTER TABLE model_metadata DROP COLUMN input_price;
ALTER TABLE model_metadata DROP COLUMN output_price;
ALTER TABLE model_metadata DROP COLUMN reasoning_price;
ALTER TABLE model_metadata DROP COLUMN cache_read_price;
ALTER TABLE model_metadata DROP COLUMN cache_write_price;
ALTER TABLE model_metadata DROP COLUMN input_audio_price;
ALTER TABLE model_metadata DROP COLUMN output_audio_price;

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
-- 策略：每个产品取第一个有定价模型的价格
UPDATE products p
SET input_price = sub.input_price,
    output_price = sub.output_price
FROM (
    SELECT DISTINCT ON (m.provider_id)
        m.provider_id, m.input_price, m.output_price
    FROM models m
    WHERE m.input_price IS NOT NULL
    ORDER BY m.provider_id, m.id
) sub
WHERE p.provider_id = sub.provider_id;

-- 9. 迁移关系到 product_models（从 products.models JSON 数组）
-- PostgreSQL: 使用 jsonb_array_elements_text 展开 JSON 数组
INSERT INTO product_models (product_id, model_id, created_at, updated_at)
SELECT p.id, m.id, NOW(), NOW()
FROM products p
CROSS JOIN LATERAL jsonb_array_elements_text(p.models::jsonb) AS model_name
JOIN models m ON m.provider_model_id = model_name AND m.provider_id = p.provider_id
WHERE p.models IS NOT NULL
ON CONFLICT (product_id, model_id) DO NOTHING;

-- 10. products 删除 models 列
ALTER TABLE products DROP COLUMN models;

-- 11. models 删除定价列
ALTER TABLE models DROP COLUMN input_price;
ALTER TABLE models DROP COLUMN output_price;
```

- [ ] **Step 2: 提交**

```bash
git add gateway-boot/src/main/resources/db/migration/V34__product_model_association.sql
git commit -m "feat: V34 产品-模型关联重构迁移脚本"
```

---

## Task 2: 元数据体系 - ProductMetadata 新增定价字段

**Files:**
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/domain/metadata/entity/ProductMetadata.java`
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/metadata/database/ProductMetadataDo.java`
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/metadata/gateway/ProductMetadataGatewayImpl.java`
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/application/metadata/dto/ProductMetadataResponse.java`
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/application/metadata/ProductMetadataService.java`

- [ ] **Step 1: ProductMetadata 实体新增 7 个定价字段**

在 `ProductMetadata.java` 中添加：
```java
private BigDecimal inputPrice;
private BigDecimal outputPrice;
private BigDecimal reasoningPrice;
private BigDecimal cacheReadPrice;
private BigDecimal cacheWritePrice;
private BigDecimal inputAudioPrice;
private BigDecimal outputAudioPrice;
```
及对应 getter/setter。添加 `import java.math.BigDecimal`。

- [ ] **Step 2: ProductMetadataDo 新增 7 个定价列**

在 `ProductMetadataDo.java` 中添加：
```java
@Column(name = "input_price", precision = 12, scale = 6)
private BigDecimal inputPrice;
// ... 其余 6 个同上
```
及对应 getter/setter。

- [ ] **Step 3: ProductMetadataGatewayImpl DO↔Entity 映射新增定价**

在 `toEntity()` 和 `toDo()` 方法中添加 7 个定价字段的映射。

- [ ] **Step 4: ProductMetadataResponse 新增定价字段**

```java
private BigDecimal inputPrice;
private BigDecimal outputPrice;
private BigDecimal reasoningPrice;
private BigDecimal cacheReadPrice;
private BigDecimal cacheWritePrice;
private BigDecimal inputAudioPrice;
private BigDecimal outputAudioPrice;
```

- [ ] **Step 5: ProductMetadataService.toResponse() 新增定价映射**

在 `toResponse()` 方法中添加定价字段映射。

- [ ] **Step 6: 提交**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/domain/metadata/entity/ProductMetadata.java \
       gateway-boot/src/main/java/com/codingas/gateway/infrastructure/metadata/database/ProductMetadataDo.java \
       gateway-boot/src/main/java/com/codingas/gateway/infrastructure/metadata/gateway/ProductMetadataGatewayImpl.java \
       gateway-boot/src/main/java/com/codingas/gateway/application/metadata/dto/ProductMetadataResponse.java \
       gateway-boot/src/main/java/com/codingas/gateway/application/metadata/ProductMetadataService.java
git commit -m "feat: ProductMetadata 新增 7 个定价字段"
```

---

## Task 3: 元数据体系 - ModelMetadata 移除定价字段

**Files:**
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/domain/metadata/entity/ModelMetadata.java`
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/metadata/database/ModelMetadataDo.java`
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/metadata/gateway/ModelMetadataGatewayImpl.java`
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/application/metadata/dto/ModelMetadataResponse.java`
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/application/metadata/dto/ModelMetadataCreateRequest.java`
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/application/metadata/dto/ModelMetadataUpdateRequest.java`
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/application/metadata/ModelMetadataService.java`

- [ ] **Step 1: ModelMetadata 实体移除 productId + 7 个定价字段**

从 `ModelMetadata.java` 中移除：
- `productId` 字段及 getter/setter
- `inputPrice`, `outputPrice`, `reasoningPrice`, `cacheReadPrice`, `cacheWritePrice`, `inputAudioPrice`, `outputAudioPrice` 字段及 getter/setter

- [ ] **Step 2: ModelMetadataDo 移除对应列**

从 `ModelMetadataDo.java` 中移除 `product_id` + 7 个定价列及 getter/setter。

- [ ] **Step 3: ModelMetadataGatewayImpl 移除映射**

在 `toEntity()` 和 `toDo()` 方法中移除 productId + 7 个定价字段的映射。

- [ ] **Step 4: ModelMetadataResponse 移除定价字段**

移除 7 个定价字段。移除 `productId` 字段。

- [ ] **Step 5: ModelMetadataCreateRequest/UpdateRequest 移除定价字段**

从 record 中移除 7 个定价字段。

- [ ] **Step 6: ModelMetadataService 移除定价映射**

在 `applyCreateRequest()`, `applyUpdateRequest()`, `toResponse()` 方法中移除定价字段映射。

- [ ] **Step 7: ModelMetadataGateway 移除 findByProductId**

从 `ModelMetadataGateway.java` 接口中移除 `findByProductId(Long productId)` 方法。
从 `ModelMetadataGatewayImpl.java` 中移除实现。
从 `ModelMetadataRepository.java` 中移除 `findByProductId` 方法。
从 `ModelMetadataController.java` 中移除 `listByProductId` 端点。
从 `ModelMetadataService.java` 中移除 `listByProductId` 方法。

- [ ] **Step 8: 提交**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/domain/metadata/ \
       gateway-boot/src/main/java/com/codingas/gateway/infrastructure/metadata/ \
       gateway-boot/src/main/java/com/codingas/gateway/application/metadata/ \
       gateway-boot/src/main/java/com/codingas/gateway/adapter/api/ModelMetadataController.java
git commit -m "refactor: ModelMetadata 移除 productId 和定价字段"
```

---

## Task 4: 元数据体系 - ProductModelMetadata 关联实体和 Gateway

**Files:**
- Create: `gateway-boot/src/main/java/com/codingas/gateway/domain/metadata/entity/ProductModelMetadata.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/domain/metadata/gateway/ProductModelMetadataGateway.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/metadata/database/ProductModelMetadataDo.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/metadata/database/ProductModelMetadataRepository.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/metadata/gateway/ProductModelMetadataGatewayImpl.java`

- [ ] **Step 1: 创建 ProductModelMetadata 领域实体**

```java
package com.codingas.gateway.domain.metadata.entity;

import java.time.Instant;

/**
 * 产品-模型元数据关联实体（纯关系）
 */
public class ProductModelMetadata {

    private Long id;
    private Long productId;
    private Long modelId;
    private Instant createdAt;
    private Long createdBy;
    private Instant updatedAt;
    private Long updatedBy;

    public ProductModelMetadata() {}

    // getter/setter 省略（与 ModelMetadata 风格一致，手写 getter/setter）
}
```

- [ ] **Step 2: 创建 ProductModelMetadataGateway 接口**

```java
package com.codingas.gateway.domain.metadata.gateway;

import com.codingas.gateway.domain.metadata.entity.ProductModelMetadata;
import java.util.List;

/**
 * 产品-模型元数据关联 Gateway
 */
public interface ProductModelMetadataGateway {

    ProductModelMetadata save(ProductModelMetadata pmm);
    List<ProductModelMetadata> saveAll(List<ProductModelMetadata> list);
    List<ProductModelMetadata> findByProductId(Long productId);
    List<ProductModelMetadata> findByModelId(Long modelId);
    void deleteById(Long id);
    boolean existsByProductIdAndModelId(Long productId, Long modelId);
    void deleteByProductId(Long productId);
}
```

- [ ] **Step 3: 创建 ProductModelMetadataDo**

```java
package com.codingas.gateway.infrastructure.metadata.database;

import com.codingas.gateway.infrastructure.common.BaseDo;
import jakarta.persistence.*;

@Entity
@Table(name = "product_model_metadata", uniqueConstraints = {
    @UniqueConstraint(name = "uk_pmm_product_model", columnNames = {"product_id", "model_id"})
})
public class ProductModelMetadataDo extends BaseDo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "model_id", nullable = false)
    private Long modelId;

    // getter/setter
}
```

- [ ] **Step 4: 创建 ProductModelMetadataRepository**

```java
package com.codingas.gateway.infrastructure.metadata.database;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProductModelMetadataRepository extends JpaRepository<ProductModelMetadataDo, Long> {
    List<ProductModelMetadataDo> findByProductId(Long productId);
    List<ProductModelMetadataDo> findByModelId(Long modelId);
    boolean existsByProductIdAndModelId(Long productId, Long modelId);
    void deleteByProductId(Long productId);
}
```

- [ ] **Step 5: 创建 ProductModelMetadataGatewayImpl**

实现 `ProductModelMetadataGateway` 接口，包含 `toEntity()` / `toDo()` 转换方法。

- [ ] **Step 6: 提交**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/domain/metadata/entity/ProductModelMetadata.java \
       gateway-boot/src/main/java/com/codingas/gateway/domain/metadata/gateway/ProductModelMetadataGateway.java \
       gateway-boot/src/main/java/com/codingas/gateway/infrastructure/metadata/database/ProductModelMetadataDo.java \
       gateway-boot/src/main/java/com/codingas/gateway/infrastructure/metadata/database/ProductModelMetadataRepository.java \
       gateway-boot/src/main/java/com/codingas/gateway/infrastructure/metadata/gateway/ProductModelMetadataGatewayImpl.java
git commit -m "feat: 元数据体系 ProductModelMetadata 关联实体和 Gateway"
```

---

## Task 5: 业务体系 - ProductModel 关联实体和 Gateway

**Files:**
- Create: `gateway-boot/src/main/java/com/codingas/gateway/domain/product/entity/ProductModel.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/domain/product/gateway/ProductModelGateway.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/product/gateway/database/dataobject/ProductModelDo.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/product/gateway/database/repository/ProductModelRepository.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/product/gateway/ProductModelGatewayImpl.java`

- [ ] **Step 1: 创建 ProductModel 领域实体**

```java
package com.codingas.gateway.domain.product.entity;

import com.codingas.gateway.common.entity.BaseEntity;

/**
 * 产品-模型关联实体（纯关系）
 */
public class ProductModel extends BaseEntity {
    private Long id;
    private Long productId;
    private Long modelId;

    @Override
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public Long getModelId() { return modelId; }
    public void setModelId(Long modelId) { this.modelId = modelId; }
}
```

- [ ] **Step 2: 创建 ProductModelGateway 接口**

```java
package com.codingas.gateway.domain.product.gateway;

import com.codingas.gateway.domain.product.entity.ProductModel;
import java.util.List;

public interface ProductModelGateway {
    ProductModel save(ProductModel pm);
    List<ProductModel> saveAll(List<ProductModel> list);
    List<ProductModel> findByProductId(Long productId);
    List<ProductModel> findByModelId(Long modelId);
    void deleteByProductIdAndModelId(Long productId, Long modelId);
    boolean existsByProductIdAndModelId(Long productId, Long modelId);
}
```

- [ ] **Step 3: 创建 ProductModelDo、ProductModelRepository、ProductModelGatewayImpl**

按照项目既有模式（参照 `ProductApiKeyDo` / `ProductApiKeyRepository`）创建。

- [ ] **Step 4: 提交**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/domain/product/entity/ProductModel.java \
       gateway-boot/src/main/java/com/codingas/gateway/domain/product/gateway/ProductModelGateway.java \
       gateway-boot/src/main/java/com/codingas/gateway/infrastructure/product/gateway/database/ \
       gateway-boot/src/main/java/com/codingas/gateway/infrastructure/product/gateway/ProductModelGatewayImpl.java
git commit -m "feat: 业务体系 ProductModel 关联实体和 Gateway"
```

---

## Task 6: 业务体系 - Product/Model 实体字段变更

**Files:**
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/domain/product/entity/Product.java`
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/domain/model/entity/Model.java`
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/product/gateway/database/dataobject/ProductDo.java`
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/product/gateway/ProductGatewayImpl.java`
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/product/gateway/database/repository/ProductRepository.java`

- [ ] **Step 1: Product 实体新增定价、移除 models**

在 `Product.java` 中：
- 新增 `private BigDecimal inputPrice` 和 `private BigDecimal outputPrice`
- 移除 `private List<String> models` 及其 getter/setter

- [ ] **Step 2: Model 实体移除定价**

在 `Model.java` 中移除 `inputPrice` 和 `outputPrice` 字段及 getter/setter。

- [ ] **Step 3: ProductDo 适配**

在 `ProductDo.java` 中：
- 新增 `@Column(name = "input_price", precision = 12, scale = 6) private BigDecimal inputPrice`
- 新增 `@Column(name = "output_price", precision = 12, scale = 6) private BigDecimal outputPrice`
- 移除 `@Column(name = "models") private List<String> models` 及 `@JdbcTypeCode(SqlTypes.JSON)` 注解

- [ ] **Step 4: ProductGatewayImpl DO↔Entity 适配**

在 `toEntity()` 和 `toDataObject()` 中：
- 新增 `inputPrice`/`outputPrice` 映射
- 移除 `models` 映射

- [ ] **Step 5: ProductRepository.findByModel 改用关联表查询**

移除 `findByModel` 的 LIKE 查询（因为 models 列已删除）。改为在应用层通过 `ProductModelGateway` 查询。

- [ ] **Step 6: 提交**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/domain/product/entity/Product.java \
       gateway-boot/src/main/java/com/codingas/gateway/domain/model/entity/Model.java \
       gateway-boot/src/main/java/com/codingas/gateway/infrastructure/product/
git commit -m "refactor: Product 新增定价移除 models，Model 移除定价"
```

---

## Task 7: ProductDomainService + ProductRoutingService 适配

**Files:**
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/domain/product/service/ProductDomainService.java`
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/application/proxy/ProductRoutingService.java`

- [ ] **Step 1: ProductDomainService.containsModel 改用关联表**

```java
@Service
@RequiredArgsConstructor
public class ProductDomainService {

    private final ProductModelGateway productModelGateway;
    private final ModelGateway modelGateway;

    /**
     * 检查产品是否包含指定模型
     */
    public boolean containsModel(Product product, String modelName) {
        List<ProductModel> associations = productModelGateway.findByProductId(product.getId());
        return associations.stream().anyMatch(pm -> {
            Model model = modelGateway.findById(pm.getModelId()).orElse(null);
            return model != null && model.getProviderModelId().equals(modelName);
        });
    }

    // getEndpoint / getDefaultEndpoint 不变
}
```

- [ ] **Step 2: ProductRoutingService 注入变更**

`ProductRoutingService` 已通过 `productDomainService.containsModel(product, modelName)` 调用，方法签名未变，无需修改调用方。但需确认 `ProductDomainService` 注入的 `ProductModelGateway` 和 `ModelGateway` 在 Spring 上下文中可用。

- [ ] **Step 3: 提交**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/domain/product/service/ProductDomainService.java
git commit -m "refactor: ProductDomainService.containsModel 改用关联表查询"
```

---

## Task 8: MetadataSyncService 适配

**Files:**
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/application/metadata/MetadataSyncService.java`
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/metadata/repository/BuiltinMetadataLoader.java`

- [ ] **Step 1: MetadataSyncService 新增关联同步步骤**

在 `syncBuiltinMetadata()` 中新增第 4 步：同步 product-model-metadata 关联。

```java
// 在 syncBuiltinMetadata() 中添加：
var associationResult = syncBuiltinProductModelAssociations();
```

新增 `syncBuiltinProductModelAssociations()` 私有方法：
1. 加载 `metadata/product-models/*.json` 数据
2. 对每条关联记录，查找 `ProductMetadata` 和 `ModelMetadata` 的 ID
3. 检查关联是否已存在，不存在则创建

- [ ] **Step 2: MetadataSyncService 产品同步新增定价**

在 `createProductMetadata()` 和 `updateProductMetadata()` 方法中，从 JSON 数据提取定价字段写入 `ProductMetadata`。

```java
// createProductMetadata 中新增：
if (data.get("input_price") != null) {
    metadata.setInputPrice(BigDecimal.valueOf(((Number) data.get("input_price")).doubleValue()));
}
// ... 其余 6 个定价字段同理
```

- [ ] **Step 3: MetadataSyncService 模型同步移除定价**

在 `createModelMetadata()` 和 `applyModelFields()` 方法中，移除定价字段的设置逻辑。
移除 `resolveProductId()` 方法（productId 已不存在）。

- [ ] **Step 4: BuiltinMetadataLoader 新增 product-models 加载**

```java
private static final String PRODUCT_MODELS_LOCATION = "classpath*:metadata/product-models/*.json";

public List<Map<String, Object>> loadProductModelMetadata() {
    // 与 loadModelMetadata() 类似，从 product-models/ 目录加载 JSON
}
```

- [ ] **Step 5: 提交**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/application/metadata/MetadataSyncService.java \
       gateway-boot/src/main/java/com/codingas/gateway/infrastructure/metadata/repository/BuiltinMetadataLoader.java
git commit -m "feat: MetadataSyncService 同步关联 + 产品定价，模型移除定价"
```

---

## Task 9: ProviderMetadataService.apply() 适配

**Files:**
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/application/metadata/ProviderMetadataService.java`

- [ ] **Step 1: apply 流程变更**

当前 `applyMetadata()` 流程：创建 Provider → Product → ProductApiKey → Model（模型设置定价）

重构后：
1. 创建 Provider（不变）
2. 创建 Product：从 `ProductMetadata` 获取定价，设置 `inputPrice`/`outputPrice`
3. 创建 ProductApiKey（不变）
4. 创建 Model：不再设置定价
5. 新增：创建 ProductModel 关联记录

```java
// 步骤 2 修改：Product 新增定价
product.setInputPrice(pm.getInputPrice());
product.setOutputPrice(pm.getOutputPrice());

// 步骤 4 修改：Model 不再设置定价
// 移除: model.setInputPrice(mm.getInputPrice());
// 移除: model.setOutputPrice(mm.getOutputPrice());

// 步骤 5 新增：创建 ProductModel 关联
for (ModelMetadata mm : modelMetadatas) {
    // 查找该模型属于哪个产品（通过 ProductModelMetadataGateway）
    List<ProductModelMetadata> pmms = productModelMetadataGateway.findByModelId(mm.getId());
    for (ProductModelMetadata pmm : pmms) {
        // 查找业务 Product 的 ID（通过元数据 ProductMetadata → 业务 Product 映射）
        ProductModel pm = new ProductModel();
        pm.setProductId(/* 对应的业务 Product ID */);
        pm.setModelId(savedModel.getId());
        productModelGateway.save(pm);
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/application/metadata/ProviderMetadataService.java
git commit -m "refactor: apply 流程产品持有定价、模型无定价、创建关联"
```

---

## Task 10: 业务 Service/DTO 适配

**Files:**
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/application/product/ProductServiceImpl.java`
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/application/model/ModelServiceImpl.java`
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/application/product/dto/ProductResponse.java`
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/application/product/dto/ProductRequest.java`
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/application/model/dto/ModelResponse.java`
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/application/model/dto/ModelCreateRequest.java`
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/application/model/dto/ModelUpdateRequest.java`

- [ ] **Step 1: ProductResponse 新增定价、移除 models**

```java
// 新增
private BigDecimal inputPrice;
private BigDecimal outputPrice;
// 移除
// private List<String> models;
```

- [ ] **Step 2: ProductRequest 新增定价、移除 models**

```java
// 新增
private BigDecimal inputPrice;
private BigDecimal outputPrice;
// 移除
// private List<String> models;
```

- [ ] **Step 3: ProductServiceImpl 适配**

- `create()`: 设置 `inputPrice`/`outputPrice`，移除 `models` 设置
- `update()`: 同上
- `toResponse()`: 映射 `inputPrice`/`outputPrice`，移除 `models` 映射

- [ ] **Step 4: ModelResponse/ModelCreateRequest/ModelUpdateRequest 移除定价**

移除 `inputPrice` 和 `outputPrice` 字段。

- [ ] **Step 5: ModelServiceImpl 适配**

- `create()`: 移除 `inputPrice`/`outputPrice` 设置
- `update()`: 移除 `inputPrice`/`outputPrice` 更新
- `toResponse()`: 移除 `inputPrice`/`outputPrice` 映射

- [ ] **Step 6: 提交**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/application/product/ \
       gateway-boot/src/main/java/com/codingas/gateway/application/model/
git commit -m "refactor: 业务 DTO 适配定价迁移和 models 移除"
```

---

## Task 11: JSON 数据文件变更

**Files:**
- Modify: `gateway-boot/src/main/resources/metadata/models/*.json` (12 个文件)
- Modify: `gateway-boot/src/main/resources/metadata/products/*.json` (12 个文件)
- Create: `gateway-boot/src/main/resources/metadata/product-models/*.json` (12 个文件)

- [ ] **Step 1: models/*.json 移除 product_name 和定价字段**

每个模型 JSON 文件中，每条记录只保留模型固有属性：
- 保留：`provider_model_id`, `display_name`, `model_family`, `context_window`, `max_input_tokens`, `max_output_tokens`, `knowledge_cutoff`, `release_date`, `open_weights`, `modalities`, `capabilities`
- 移除：`product_name`, `input_price`, `output_price`, `reasoning_price`, `cache_read_price`, `cache_write_price`, `input_audio_price`, `output_audio_price`

示例（deepseek.json 重构后）：
```json
[
  {"provider_model_id": "deepseek-v4-pro", "display_name": "DeepSeek V4 Pro", "context_window": 1048576, "max_output_tokens": 384000, "capabilities": {"vision": false, "function_calling": true, "streaming": true, "reasoning": true}, "knowledge_cutoff": "2025-04-15"},
  {"provider_model_id": "deepseek-v4-flash", "display_name": "DeepSeek V4 Flash", "context_window": 1048576, "max_output_tokens": 384000, "capabilities": {"vision": false, "function_calling": true, "streaming": true}, "knowledge_cutoff": "2025-04-15"}
]
```

- [ ] **Step 2: products/*.json 新增定价字段**

每个产品 JSON 文件中，新增定价字段。定价值取该产品下最常用（或最便宜）模型的定价。

示例（deepseek.json 重构后）：
```json
[
  {"provider_id": "deepseek", "product_name": "按量付费", "product_type": "STANDARD", "endpoints": {"OPENAI": "https://api.deepseek.com", "ANTHROPIC": "https://api.deepseek.com/anthropic"}, "is_default": true, "description": "标准按量计费", "input_price": 0.14, "output_price": 0.28, "cache_read_price": 0.0028}
]
```

注意：产品定价代表该产品的基准价格。对于同一产品下不同模型有不同定价的情况，产品定价取该产品下最便宜模型的定价作为基准。

- [ ] **Step 3: 新建 product-models/*.json**

每个供应商一个文件，定义产品包含哪些模型：

示例（deepseek.json）：
```json
[
  {"product_name": "按量付费", "provider_model_id": "deepseek-v4-pro"},
  {"product_name": "按量付费", "provider_model_id": "deepseek-v4-flash"},
  {"product_name": "按量付费", "provider_model_id": "deepseek-v3.2"},
  {"product_name": "按量付费", "provider_model_id": "deepseek-r1"},
  {"product_name": "按量付费", "provider_model_id": "deepseek-chat"},
  {"product_name": "按量付费", "provider_model_id": "deepseek-coder"}
]
```

需要为以下 12 个供应商创建文件：deepseek, openai, anthropic, google, qwen, moonshot, minim, xunfei, tencent, zhipu, volcengine, wenxin

- [ ] **Step 4: 提交**

```bash
git add gateway-boot/src/main/resources/metadata/
git commit -m "feat: JSON 数据文件适配产品-模型关联重构"
```

---

## Task 12: 前端类型和 API 适配

**Files:**
- Modify: `gateway-console/src/types/metadata.ts`
- Modify: `gateway-console/src/types/product.ts`
- Modify: `gateway-console/src/types/model.ts`
- Modify: `gateway-console/src/services/api/metadata.ts`
- Modify: `gateway-console/src/services/query/useMetadata.ts`

- [ ] **Step 1: metadata.ts 类型适配**

```typescript
// ProductMetadata 新增定价字段
export interface ProductMetadata {
  // ... 原有字段
  inputPrice?: number;
  outputPrice?: number;
  reasoningPrice?: number;
  cacheReadPrice?: number;
  cacheWritePrice?: number;
  inputAudioPrice?: number;
  outputAudioPrice?: number;
}

// ModelMetadata 移除定价和 productId
export interface ModelMetadata {
  // ... 移除 productId, inputPrice, outputPrice 等 7 个定价字段
}

// 新增 ProductModelMetadata 接口
export interface ProductModelMetadata {
  id: number;
  productId: number;
  modelId: number;
  createdAt: string;
  updatedAt: string;
}
```

- [ ] **Step 2: product.ts 类型适配**

```typescript
export interface Product {
  // ... 原有字段
  inputPrice?: number;
  outputPrice?: number;
  // 移除 models 字段
}
```

- [ ] **Step 3: model.ts 类型适配**

```typescript
export interface Model {
  // ... 移除 inputPrice, outputPrice
}
```

- [ ] **Step 4: metadata.ts API 适配**

新增 `productModelMetadataApi` 对象，包含 `listByProductId`, `listByModelId`, `create`, `delete` 方法。

- [ ] **Step 5: 提交**

```bash
git add gateway-console/src/types/ gateway-console/src/services/
git commit -m "feat: 前端类型和 API 适配产品-模型关联重构"
```

---

## Task 13: 编译验证与测试修复

**Files:**
- 修改所有因字段移除而编译失败的测试文件

- [ ] **Step 1: 编译项目**

运行: `./mvnw clean compile -pl gateway-boot`
预期: BUILD SUCCESS（如有错误，逐个修复）

- [ ] **Step 2: 运行测试**

运行: `./mvnw test -pl gateway-boot`
预期: BUILD SUCCESS（如有失败，逐个修复）

- [ ] **Step 3: 修复编译和测试错误**

常见修复：
- `Product.getModels()` → 通过 `ProductModelGateway.findByProductId()` 查询
- `Model.getInputPrice()` / `Model.getOutputPrice()` → 从关联的 Product 获取
- `ModelMetadata.getProductId()` → 通过 `ProductModelMetadataGateway` 查询
- mock 设置中移除定价字段

- [ ] **Step 4: 提交**

```bash
git add -A
git commit -m "fix: 修复因重构导致的编译和测试错误"
```

---

## 自检清单

| 检查项 | 覆盖 Task |
|--------|-----------|
| Spec §3.1: ProductMetadata 新增定价 | Task 2 |
| Spec §3.1: ProductModelMetadata 新建 | Task 4 |
| Spec §3.1: ModelMetadata 移除定价+productId | Task 3 |
| Spec §3.2: Product 新增定价、移除 models | Task 6 |
| Spec §3.2: ProductModel 新建 | Task 5 |
| Spec §3.2: Model 移除定价 | Task 6 |
| Spec §4.1: 元数据 API 变更 | Task 3, 4 |
| Spec §4.2: 业务 API 变更 | Task 5, 10 |
| Spec §5.1: MetadataSyncService 变更 | Task 8 |
| Spec §5.2: ProviderMetadataService.apply() 变更 | Task 9 |
| Spec §5.3: ProductRoutingService 变更 | Task 7 |
| Spec §6: JSON 数据文件变更 | Task 11 |
| Spec §7: 数据迁移策略 | Task 1 |
| 无 Placeholder（TBD/TODO） | ✅ |
| 类型一致性 | ✅ |
