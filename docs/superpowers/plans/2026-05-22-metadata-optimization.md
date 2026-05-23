# 元数据体系优化实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 建立三级元数据关系 ProviderMetadata → ProductMetadata → ModelMetadata，支持多协议端点配置

**Architecture:** 新增 product_metadata 表作为中间层，model_metadata 改为关联 product_id，端点配置以 JSON 格式存储在产品层级

**Tech Stack:** Java 21, Spring Boot 3.5.x, JPA, PostgreSQL, Flyway

---

## 文件结构

### 新增文件

| 文件路径 | 职责 |
|---------|------|
| `gateway-boot/src/main/resources/db/migration/V29__add_product_metadata.sql` | 数据库迁移脚本 |
| `gateway-boot/src/main/java/com/codingas/gateway/domain/metadata/entity/ProductMetadata.java` | 产品元数据实体 |
| `gateway-boot/src/main/java/com/codingas/gateway/domain/metadata/enums/ProductType.java` | 产品类型枚举 |
| `gateway-boot/src/main/java/com/codingas/gateway/domain/metadata/enums/Protocol.java` | 协议类型枚举 |
| `gateway-boot/src/main/java/com/codingas/gateway/domain/metadata/gateway/ProductMetadataGateway.java` | 产品元数据网关接口 |
| `gateway-boot/src/main/java/com/codingas/gateway/domain/metadata/service/ProductMetadataDomainService.java` | 产品元数据领域服务 |
| `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/metadata/database/ProductMetadataRepository.java` | JPA Repository |
| `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/metadata/database/ProductMetadataDo.java` | 数据库对象 |
| `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/metadata/gateway/ProductMetadataGatewayImpl.java` | 网关实现 |
| `gateway-boot/src/main/java/com/codingas/gateway/application/metadata/ProductMetadataService.java` | 应用服务 |
| `gateway-boot/src/main/java/com/codingas/gateway/application/metadata/dto/ProductMetadataResponse.java` | 响应DTO |
| `gateway-boot/src/main/resources/metadata/products/*.json` | 产品元数据JSON文件（13个） |

### 修改文件

| 文件路径 | 修改内容 |
|---------|---------|
| `gateway-boot/src/main/java/com/codingas/gateway/domain/metadata/entity/ModelMetadata.java` | 新增 productId 字段 |
| `gateway-boot/src/main/java/com/codingas/gateway/domain/metadata/gateway/ModelMetadataGateway.java` | 新增按产品查询方法 |
| `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/metadata/repository/BuiltinMetadataLoader.java` | 新增加载产品方法 |
| `gateway-boot/src/main/java/com/codingas/gateway/application/metadata/MetadataSyncService.java` | 新增产品同步逻辑 |
| `gateway-boot/src/main/resources/metadata/models/*.json` | 调整为关联 product_name |

---

## Task 1: 数据库迁移 - 新增 product_metadata 表

**Files:**
- Create: `gateway-boot/src/main/resources/db/migration/V29__add_product_metadata.sql`

- [ ] **Step 1: 创建迁移脚本**

```sql
-- V29__add_product_metadata.sql
-- 新增产品元数据表，建立三级元数据关系

-- ============================================
-- 1. 创建 product_metadata 表
-- ============================================
CREATE TABLE product_metadata (
    id BIGSERIAL PRIMARY KEY,
    provider_id VARCHAR(64) NOT NULL,
    product_name VARCHAR(128) NOT NULL,
    product_type VARCHAR(32) NOT NULL,
    description TEXT,
    endpoints JSON NOT NULL,
    is_default BOOLEAN DEFAULT false,
    state VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    source VARCHAR(32) NOT NULL DEFAULT 'BUILTIN',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_at TIMESTAMP WITH TIME ZONE,
    updated_by BIGINT
);

-- 唯一索引：(provider_id, product_name) 联合唯一
CREATE UNIQUE INDEX uk_product_metadata_provider_name
    ON product_metadata(provider_id, product_name);

-- 查询索引
CREATE INDEX idx_product_metadata_provider ON product_metadata(provider_id);
CREATE INDEX idx_product_metadata_type ON product_metadata(product_type);
CREATE INDEX idx_product_metadata_state ON product_metadata(state);

-- ============================================
-- 2. 调整 model_metadata 表
-- ============================================
-- 新增 product_id 字段
ALTER TABLE model_metadata ADD COLUMN product_id BIGINT;

-- 新增索引
CREATE INDEX idx_model_metadata_product ON model_metadata(product_id);

-- 添加外键约束（可选，根据实际需求决定）
-- ALTER TABLE model_metadata
--     ADD CONSTRAINT fk_model_metadata_product
--     FOREIGN KEY (product_id) REFERENCES product_metadata(id);

-- ============================================
-- 3. 数据迁移说明
-- ============================================
-- 现有 model_metadata 数据暂时保留 provider_id，
-- 待产品数据同步后，通过脚本或应用层迁移 product_id
```

- [ ] **Step 2: 验证迁移脚本语法**

运行: `cd gateway-boot && ../mvnw flyway:info`
预期: 显示当前迁移状态，确认 V29 待执行

- [ ] **Step 3: 提交**

```bash
git add gateway-boot/src/main/resources/db/migration/V29__add_product_metadata.sql
git commit -m "feat(db): 新增 product_metadata 表和 model_metadata.product_id 字段

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## Task 2: 领域层 - 新增枚举类型

**Files:**
- Create: `gateway-boot/src/main/java/com/codingas/gateway/domain/metadata/enums/ProductType.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/domain/metadata/enums/Protocol.java`

- [ ] **Step 1: 创建 ProductType 枚举**

```java
package com.codingas.gateway.domain.metadata.enums;

/**
 * 产品类型枚举
 * <p>
 * 定义供应商提供的产品/套餐类型。
 * </p>
 */
public enum ProductType {

    /** 标准按量付费 */
    STANDARD("standard", "按量付费"),

    /** 批量异步（通常50%折扣） */
    BATCH("batch", "批量处理"),

    /** 缓存折扣 */
    CACHE("cache", "缓存折扣"),

    /** 订阅制（Coding Plan、Token Plan） */
    SUBSCRIPTION("subscription", "订阅制"),

    /** 限时优惠 */
    PROMOTION("promotion", "限时优惠"),

    /** 免费额度 */
    FREE_TIER("free_tier", "免费额度");

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
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown product type: " + code);
    }
}
```

- [ ] **Step 2: 创建 Protocol 枚举**

```java
package com.codingas.gateway.domain.metadata.enums;

/**
 * 协议类型枚举
 * <p>
 * 定义 API 端点支持的协议类型。
 * </p>
 */
public enum Protocol {

    /** OpenAI 原生/兼容协议 */
    OPENAI("openai", "OpenAI"),

    /** Anthropic Messages API */
    ANTHROPIC("anthropic", "Anthropic"),

    /** Google Gemini API */
    GEMINI("gemini", "Gemini"),

    /** 原生私有协议 */
    NATIVE("native", "Native");

    private final String code;
    private final String displayName;

    Protocol(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static Protocol fromCode(String code) {
        for (Protocol protocol : values()) {
            if (protocol.code.equalsIgnoreCase(code)) {
                return protocol;
            }
        }
        throw new IllegalArgumentException("Unknown protocol: " + code);
    }
}
```

- [ ] **Step 3: 提交**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/domain/metadata/enums/
git commit -m "feat(domain): 新增 ProductType 和 Protocol 枚举

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## Task 3: 领域层 - 新增 ProductMetadata 实体

**Files:**
- Create: `gateway-boot/src/main/java/com/codingas/gateway/domain/metadata/entity/ProductMetadata.java`

- [ ] **Step 1: 创建 ProductMetadata 实体**

```java
package com.codingas.gateway.domain.metadata.entity;

import com.codingas.gateway.domain.metadata.enums.MetadataState;
import com.codingas.gateway.domain.metadata.enums.ProductType;

import java.time.Instant;
import java.util.Map;

/**
 * 产品元数据实体
 * <p>
 * 表示供应商提供的产品/套餐，包含多协议端点配置。
 * 一个供应商可以有多个产品（如按量计费、Coding Plan）。
 * </p>
 */
public class ProductMetadata {

    private Long id;
    private String providerId;
    private String productName;
    private ProductType productType;
    private String description;
    private Map<String, String> endpoints;  // protocol -> base_url
    private Boolean isDefault;
    private MetadataState state;
    private MetadataSource source;
    private Instant createdAt;
    private Long createdBy;
    private Instant updatedAt;
    private Long updatedBy;

    public ProductMetadata() {}

    public ProductMetadata(String providerId, String productName, ProductType productType) {
        this.providerId = providerId;
        this.productName = productName;
        this.productType = productType;
        this.isDefault = false;
        this.state = MetadataState.ACTIVE;
        this.source = MetadataSource.BUILTIN;
    }

    // ==================== Getter / Setter ====================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getProviderId() { return providerId; }
    public void setProviderId(String providerId) { this.providerId = providerId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public ProductType getProductType() { return productType; }
    public void setProductType(ProductType productType) { this.productType = productType; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Map<String, String> getEndpoints() { return endpoints; }
    public void setEndpoints(Map<String, String> endpoints) { this.endpoints = endpoints; }

    public Boolean getIsDefault() { return isDefault; }
    public void setIsDefault(Boolean isDefault) { this.isDefault = isDefault; }

    public MetadataState getState() { return state; }
    public void setState(MetadataState state) { this.state = state; }

    public MetadataSource getSource() { return source; }
    public void setSource(MetadataSource source) { this.source = source; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public Long getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(Long updatedBy) { this.updatedBy = updatedBy; }
}
```

- [ ] **Step 2: 提交**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/domain/metadata/entity/ProductMetadata.java
git commit -m "feat(domain): 新增 ProductMetadata 实体

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## Task 4: 领域层 - 新增 ProductMetadataGateway 接口

**Files:**
- Create: `gateway-boot/src/main/java/com/codingas/gateway/domain/metadata/gateway/ProductMetadataGateway.java`

- [ ] **Step 1: 创建 Gateway 接口**

```java
package com.codingas.gateway.domain.metadata.gateway;

import com.codingas.gateway.domain.metadata.entity.ProductMetadata;

import java.util.List;
import java.util.Optional;

/**
 * 产品元数据网关接口
 */
public interface ProductMetadataGateway {

    /**
     * 保存产品元数据
     */
    ProductMetadata save(ProductMetadata metadata);

    /**
     * 根据 ID 查询
     */
    Optional<ProductMetadata> findById(Long id);

    /**
     * 查询某供应商的所有产品
     */
    List<ProductMetadata> findByProviderId(String providerId);

    /**
     * 精确查找：(provider_id, product_name)
     */
    Optional<ProductMetadata> findByProviderIdAndProductName(String providerId, String productName);

    /**
     * 查询某供应商的默认产品
     */
    Optional<ProductMetadata> findDefaultByProviderId(String providerId);

    /**
     * 批量保存
     */
    List<ProductMetadata> saveAll(List<ProductMetadata> metadataList);

    /**
     * 删除
     */
    void deleteById(Long id);

    /**
     * 检查产品是否存在
     */
    boolean existsByProviderIdAndProductName(String providerId, String productName);
}
```

- [ ] **Step 2: 提交**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/domain/metadata/gateway/ProductMetadataGateway.java
git commit -m "feat(domain): 新增 ProductMetadataGateway 接口

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## Task 5: 领域层 - 新增 ProductMetadataDomainService

**Files:**
- Create: `gateway-boot/src/main/java/com/codingas/gateway/domain/metadata/service/ProductMetadataDomainService.java`

- [ ] **Step 1: 创建领域服务**

```java
package com.codingas.gateway.domain.metadata.service;

import com.codingas.gateway.domain.metadata.entity.ProductMetadata;
import com.codingas.gateway.domain.metadata.enums.MetadataSource;
import com.codingas.gateway.domain.metadata.enums.ProductType;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

/**
 * 产品元数据领域服务
 * <p>
 * 处理产品元数据的业务逻辑。
 * </p>
 */
@Service
public class ProductMetadataDomainService {

    /**
     * 从内置数据创建产品元数据
     */
    public ProductMetadata createFromBuiltinData(
            String providerId,
            String productName,
            ProductType productType,
            Map<String, String> endpoints,
            String description,
            Boolean isDefault) {

        ProductMetadata metadata = new ProductMetadata(providerId, productName, productType);
        metadata.setEndpoints(endpoints);
        metadata.setDescription(description);
        metadata.setIsDefault(isDefault != null ? isDefault : false);
        metadata.setSource(MetadataSource.BUILTIN);
        metadata.setCreatedAt(Instant.now());
        metadata.setUpdatedAt(Instant.now());
        return metadata;
    }

    /**
     * 应用更新数据
     */
    public void applyUpdateData(ProductMetadata existing, Map<String, String> endpoints,
                                 String description, Boolean isDefault) {
        if (endpoints != null) {
            existing.setEndpoints(endpoints);
        }
        if (description != null) {
            existing.setDescription(description);
        }
        if (isDefault != null) {
            existing.setIsDefault(isDefault);
        }
        existing.setUpdatedAt(Instant.now());
    }

    /**
     * 检查是否可被同步覆盖
     */
    public boolean canBeOverriddenBySync(ProductMetadata metadata) {
        return metadata.getSource() == MetadataSource.BUILTIN;
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/domain/metadata/service/ProductMetadataDomainService.java
git commit -m "feat(domain): 新增 ProductMetadataDomainService

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## Task 6: 基础设施层 - 新增 ProductMetadataDo 和 Repository

**Files:**
- Create: `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/metadata/database/ProductMetadataDo.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/metadata/database/ProductMetadataRepository.java`

- [ ] **Step 1: 创建 ProductMetadataDo**

```java
package com.codingas.gateway.infrastructure.metadata.database;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

/**
 * 产品元数据数据库对象
 */
@Data
@Entity
@Table(name = "product_metadata", uniqueConstraints = {
    @UniqueConstraint(name = "uk_product_metadata_provider_name", columnNames = {"provider_id", "product_name"})
})
public class ProductMetadataDo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "provider_id", nullable = false, length = 64)
    private String providerId;

    @Column(name = "product_name", nullable = false, length = 128)
    private String productName;

    @Column(name = "product_type", nullable = false, length = 32)
    private String productType;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "endpoints", nullable = false, columnDefinition = "json")
    private Map<String, String> endpoints;

    @Column(name = "is_default")
    private Boolean isDefault;

    @Column(name = "state", length = 32)
    private String state;

    @Column(name = "source", length = 32)
    private String source;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "updated_by")
    private Long updatedBy;
}
```

- [ ] **Step 2: 创建 ProductMetadataRepository**

```java
package com.codingas.gateway.infrastructure.metadata.database;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 产品元数据 JPA Repository
 */
@Repository
public interface ProductMetadataRepository extends JpaRepository<ProductMetadataDo, Long> {

    /**
     * 查询某供应商的所有产品
     */
    List<ProductMetadataDo> findByProviderId(String providerId);

    /**
     * 精确查找
     */
    Optional<ProductMetadataDo> findByProviderIdAndProductName(String providerId, String productName);

    /**
     * 查询默认产品
     */
    Optional<ProductMetadataDo> findByProviderIdAndIsDefaultTrue(String providerId);

    /**
     * 检查是否存在
     */
    boolean existsByProviderIdAndProductName(String providerId, String productName);
}
```

- [ ] **Step 3: 提交**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/infrastructure/metadata/database/ProductMetadataDo.java
git add gateway-boot/src/main/java/com/codingas/gateway/infrastructure/metadata/database/ProductMetadataRepository.java
git commit -m "feat(infra): 新增 ProductMetadataDo 和 ProductMetadataRepository

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## Task 7: 基础设施层 - 新增 ProductMetadataGatewayImpl

**Files:**
- Create: `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/metadata/gateway/ProductMetadataGatewayImpl.java`

- [ ] **Step 1: 创建 Gateway 实现**

```java
package com.codingas.gateway.infrastructure.metadata.gateway;

import com.codingas.gateway.domain.metadata.entity.ProductMetadata;
import com.codingas.gateway.domain.metadata.enums.MetadataSource;
import com.codingas.gateway.domain.metadata.enums.MetadataState;
import com.codingas.gateway.domain.metadata.enums.ProductType;
import com.codingas.gateway.domain.metadata.gateway.ProductMetadataGateway;
import com.codingas.gateway.infrastructure.metadata.database.ProductMetadataDo;
import com.codingas.gateway.infrastructure.metadata.database.ProductMetadataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 产品元数据网关实现
 */
@Component
@RequiredArgsConstructor
public class ProductMetadataGatewayImpl implements ProductMetadataGateway {

    private final ProductMetadataRepository repository;

    @Override
    public ProductMetadata save(ProductMetadata metadata) {
        ProductMetadataDo dataObject = toDataObject(metadata);
        ProductMetadataDo saved = repository.save(dataObject);
        return toEntity(saved);
    }

    @Override
    public Optional<ProductMetadata> findById(Long id) {
        return repository.findById(id).map(this::toEntity);
    }

    @Override
    public List<ProductMetadata> findByProviderId(String providerId) {
        return repository.findByProviderId(providerId).stream()
            .map(this::toEntity)
            .collect(Collectors.toList());
    }

    @Override
    public Optional<ProductMetadata> findByProviderIdAndProductName(String providerId, String productName) {
        return repository.findByProviderIdAndProductName(providerId, productName)
            .map(this::toEntity);
    }

    @Override
    public Optional<ProductMetadata> findDefaultByProviderId(String providerId) {
        return repository.findByProviderIdAndIsDefaultTrue(providerId)
            .map(this::toEntity);
    }

    @Override
    public List<ProductMetadata> saveAll(List<ProductMetadata> metadataList) {
        List<ProductMetadataDo> dataObjects = metadataList.stream()
            .map(this::toDataObject)
            .collect(Collectors.toList());
        return repository.saveAll(dataObjects).stream()
            .map(this::toEntity)
            .collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    @Override
    public boolean existsByProviderIdAndProductName(String providerId, String productName) {
        return repository.existsByProviderIdAndProductName(providerId, productName);
    }

    // ==================== 转换方法 ====================

    private ProductMetadataDo toDataObject(ProductMetadata entity) {
        ProductMetadataDo dataObject = new ProductMetadataDo();
        dataObject.setId(entity.getId());
        dataObject.setProviderId(entity.getProviderId());
        dataObject.setProductName(entity.getProductName());
        dataObject.setProductType(entity.getProductType() != null ? entity.getProductType().name() : null);
        dataObject.setDescription(entity.getDescription());
        dataObject.setEndpoints(entity.getEndpoints());
        dataObject.setIsDefault(entity.getIsDefault());
        dataObject.setState(entity.getState() != null ? entity.getState().name() : MetadataState.ACTIVE.name());
        dataObject.setSource(entity.getSource() != null ? entity.getSource().name() : MetadataSource.BUILTIN.name());
        dataObject.setCreatedAt(entity.getCreatedAt());
        dataObject.setCreatedBy(entity.getCreatedBy());
        dataObject.setUpdatedAt(entity.getUpdatedAt());
        dataObject.setUpdatedBy(entity.getUpdatedBy());
        return dataObject;
    }

    private ProductMetadata toEntity(ProductMetadataDo dataObject) {
        ProductMetadata entity = new ProductMetadata();
        entity.setId(dataObject.getId());
        entity.setProviderId(dataObject.getProviderId());
        entity.setProductName(dataObject.getProductName());
        entity.setProductType(dataObject.getProductType() != null ? ProductType.valueOf(dataObject.getProductType()) : null);
        entity.setDescription(dataObject.getDescription());
        entity.setEndpoints(dataObject.getEndpoints());
        entity.setIsDefault(dataObject.getIsDefault());
        entity.setState(dataObject.getState() != null ? MetadataState.valueOf(dataObject.getState()) : null);
        entity.setSource(dataObject.getSource() != null ? MetadataSource.valueOf(dataObject.getSource()) : null);
        entity.setCreatedAt(dataObject.getCreatedAt());
        entity.setCreatedBy(dataObject.getCreatedBy());
        entity.setUpdatedAt(dataObject.getUpdatedAt());
        entity.setUpdatedBy(dataObject.getUpdatedBy());
        return entity;
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/infrastructure/metadata/gateway/ProductMetadataGatewayImpl.java
git commit -m "feat(infra): 新增 ProductMetadataGatewayImpl

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## Task 8: 基础设施层 - 扩展 BuiltinMetadataLoader

**Files:**
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/metadata/repository/BuiltinMetadataLoader.java`

- [ ] **Step 1: 添加产品元数据加载方法**

在 `BuiltinMetadataLoader` 类中添加：

```java
private static final String PRODUCTS_LOCATION = "classpath*:metadata/products/*.json";

/**
 * 加载所有内置产品元数据
 */
@SuppressWarnings("unchecked")
public List<Map<String, Object>> loadProductMetadata() {
    List<Map<String, Object>> allProducts = new ArrayList<>();
    try {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(resourceLoader);
        Resource[] resources = resolver.getResources(PRODUCTS_LOCATION);
        log.info("Found {} builtin product metadata files", resources.length);

        for (Resource resource : resources) {
            try {
                // 产品元数据 JSON 是数组格式
                List<Map<String, Object>> products = JsonUtils.fromJson(
                    resource.getInputStream(),
                    new TypeReference<List<Map<String, Object>>>() {}
                );
                // 从文件名推断 provider_id
                String filename = resource.getFilename();
                String providerId = filename != null ? filename.replace(".json", "") : null;
                if (providerId != null) {
                    for (Map<String, Object> product : products) {
                        // 如果 JSON 中没有 provider_id，从文件名推断
                        if (!product.containsKey("provider_id")) {
                            product.put("provider_id", providerId);
                        }
                    }
                }
                allProducts.addAll(products);
            } catch (Exception e) {
                log.error("Failed to load product metadata from: {}", resource.getFilename(), e);
            }
        }
    } catch (IOException e) {
        log.warn("Failed to resolve product metadata resources from classpath", e);
    }
    return allProducts;
}
```

- [ ] **Step 2: 提交**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/infrastructure/metadata/repository/BuiltinMetadataLoader.java
git commit -m "feat(infra): BuiltinMetadataLoader 新增产品元数据加载方法

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## Task 9: 应用层 - 新增 ProductMetadataService 和 DTO

**Files:**
- Create: `gateway-boot/src/main/java/com/codingas/gateway/application/metadata/dto/ProductMetadataResponse.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/application/metadata/ProductMetadataService.java`

- [ ] **Step 1: 创建 ProductMetadataResponse DTO**

```java
package com.codingas.gateway.application.metadata.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * 产品元数据响应 DTO
 */
@Data
@Builder
public class ProductMetadataResponse {

    private Long id;
    private String providerId;
    private String productName;
    private String productType;
    private String description;
    private Map<String, String> endpoints;
    private Boolean isDefault;
    private String state;
}
```

- [ ] **Step 2: 创建 ProductMetadataService**

```java
package com.codingas.gateway.application.metadata;

import com.codingas.gateway.application.metadata.dto.ProductMetadataResponse;
import com.codingas.gateway.domain.metadata.entity.ProductMetadata;
import com.codingas.gateway.domain.metadata.gateway.ProductMetadataGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 产品元数据应用服务
 */
@Service
@RequiredArgsConstructor
public class ProductMetadataService {

    private final ProductMetadataGateway productMetadataGateway;

    /**
     * 查询供应商的产品列表
     */
    public List<ProductMetadataResponse> findByProviderId(String providerId) {
        return productMetadataGateway.findByProviderId(providerId).stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    /**
     * 查询产品详情
     */
    public ProductMetadataResponse findById(Long id) {
        return productMetadataGateway.findById(id)
            .map(this::toResponse)
            .orElse(null);
    }

    /**
     * 查询供应商的默认产品
     */
    public ProductMetadataResponse findDefaultByProviderId(String providerId) {
        return productMetadataGateway.findDefaultByProviderId(providerId)
            .map(this::toResponse)
            .orElse(null);
    }

    private ProductMetadataResponse toResponse(ProductMetadata entity) {
        return ProductMetadataResponse.builder()
            .id(entity.getId())
            .providerId(entity.getProviderId())
            .productName(entity.getProductName())
            .productType(entity.getProductType() != null ? entity.getProductType().name() : null)
            .description(entity.getDescription())
            .endpoints(entity.getEndpoints())
            .isDefault(entity.getIsDefault())
            .state(entity.getState() != null ? entity.getState().name() : null)
            .build();
    }
}
```

- [ ] **Step 3: 提交**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/application/metadata/dto/ProductMetadataResponse.java
git add gateway-boot/src/main/java/com/codingas/gateway/application/metadata/ProductMetadataService.java
git commit -m "feat(app): 新增 ProductMetadataService 和 ProductMetadataResponse

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## Task 10: 应用层 - 扩展 MetadataSyncService

**Files:**
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/application/metadata/MetadataSyncService.java`

- [ ] **Step 1: 添加产品同步依赖和方法**

在类中添加依赖注入：

```java
private final ProductMetadataGateway productMetadataGateway;
private final ProductMetadataDomainService productMetadataDomainService;
```

添加产品同步方法：

```java
/**
 * 同步内置产品元数据
 */
private SyncCounts syncBuiltinProducts() {
    List<Map<String, Object>> products = builtinMetadataLoader.loadProductMetadata();
    int added = 0, updated = 0;

    for (Map<String, Object> data : products) {
        String providerId = (String) data.get("provider_id");
        String productName = (String) data.get("product_name");
        try {
            ProductMetadata existing = productMetadataGateway
                .findByProviderIdAndProductName(providerId, productName).orElse(null);

            if (existing == null) {
                productMetadataGateway.save(createProductMetadata(data));
                added++;
            } else {
                updateProductMetadata(existing, data);
                productMetadataGateway.save(existing);
                updated++;
            }
        } catch (Exception e) {
            log.error("Failed to sync builtin product metadata: {}/{}", providerId, productName, e);
        }
    }

    return new SyncCounts(products.size(), added, updated);
}

@SuppressWarnings("unchecked")
private ProductMetadata createProductMetadata(Map<String, Object> data) {
    String providerId = (String) data.get("provider_id");
    String productName = (String) data.get("product_name");
    String productTypeStr = (String) data.get("product_type");
    ProductType productType = productTypeStr != null ? ProductType.fromCode(productTypeStr) : ProductType.STANDARD;

    ProductMetadata metadata = new ProductMetadata(providerId, productName, productType);
    metadata.setEndpoints((Map<String, String>) data.get("endpoints"));
    metadata.setDescription((String) data.get("description"));
    metadata.setIsDefault((Boolean) data.getOrDefault("is_default", false));
    metadata.setCreatedAt(Instant.now());
    metadata.setUpdatedAt(Instant.now());
    return metadata;
}

@SuppressWarnings("unchecked")
private void updateProductMetadata(ProductMetadata existing, Map<String, Object> data) {
    if (data.get("endpoints") != null) {
        existing.setEndpoints((Map<String, String>) data.get("endpoints"));
    }
    existing.setDescription((String) data.getOrDefault("description", existing.getDescription()));
    existing.setIsDefault((Boolean) data.getOrDefault("is_default", existing.getIsDefault()));
    existing.setUpdatedAt(Instant.now());
}
```

修改 `syncBuiltinMetadata` 方法：

```java
@Transactional
public MetadataSyncResult syncBuiltinMetadata() {
    log.info("Starting builtin metadata sync...");

    var providerResult = syncBuiltinProviders();
    var productResult = syncBuiltinProducts();  // 新增
    var modelResult = syncBuiltinModels();

    log.info("Builtin metadata sync completed: {} providers, {} products, {} models",
        providerResult.total, productResult.total, modelResult.total);

    return MetadataSyncResult.builder()
        .syncedCount(providerResult.total + productResult.total + modelResult.total)
        .addedCount(providerResult.added + productResult.added + modelResult.added)
        .updatedCount(providerResult.updated + productResult.updated + modelResult.updated)
        .syncedAt(Instant.now())
        .build();
}
```

添加 import：

```java
import com.codingas.gateway.domain.metadata.entity.ProductMetadata;
import com.codingas.gateway.domain.metadata.enums.ProductType;
import com.codingas.gateway.domain.metadata.gateway.ProductMetadataGateway;
import com.codingas.gateway.domain.metadata.service.ProductMetadataDomainService;
```

- [ ] **Step 2: 提交**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/application/metadata/MetadataSyncService.java
git commit -m "feat(app): MetadataSyncService 新增产品元数据同步

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## Task 11: JSON 文件 - 创建产品元数据文件

**Files:**
- Create: `gateway-boot/src/main/resources/metadata/products/deepseek.json`
- Create: `gateway-boot/src/main/resources/metadata/products/zhipu.json`
- Create: `gateway-boot/src/main/resources/metadata/products/volcengine.json`
- Create: `gateway-boot/src/main/resources/metadata/products/qwen.json`
- Create: `gateway-boot/src/main/resources/metadata/products/minimax.json`
- Create: `gateway-boot/src/main/resources/metadata/products/moonshot.json`
- Create: `gateway-boot/src/main/resources/metadata/products/openai.json`
- Create: `gateway-boot/src/main/resources/metadata/products/anthropic.json`
- Create: `gateway-boot/src/main/resources/metadata/products/gemini.json`
- Create: `gateway-boot/src/main/resources/metadata/products/wenxin.json`
- Create: `gateway-boot/src/main/resources/metadata/products/xunfei.json`
- Create: `gateway-boot/src/main/resources/metadata/products/tencent.json`
- Create: `gateway-boot/src/main/resources/metadata/products/baichuan.json`

- [ ] **Step 1: 创建 products 目录**

```bash
mkdir -p gateway-boot/src/main/resources/metadata/products
```

- [ ] **Step 2: 创建 deepseek.json**

```json
[
  {
    "provider_id": "deepseek",
    "product_name": "按量付费",
    "product_type": "STANDARD",
    "endpoints": {
      "OPENAI": "https://api.deepseek.com",
      "ANTHROPIC": "https://api.deepseek.com/anthropic"
    },
    "is_default": true,
    "description": "标准按量计费，支持双协议"
  }
]
```

- [ ] **Step 3: 创建 zhipu.json**

```json
[
  {
    "provider_id": "zhipu",
    "product_name": "按量付费",
    "product_type": "STANDARD",
    "endpoints": {
      "OPENAI": "https://open.bigmodel.cn/api/paas/v4/"
    },
    "is_default": true,
    "description": "标准按量计费"
  },
  {
    "provider_id": "zhipu",
    "product_name": "Coding Plan Lite",
    "product_type": "SUBSCRIPTION",
    "endpoints": {
      "OPENAI": "https://open.bigmodel.cn/api/coding/paas/v4"
    },
    "description": "月订阅制，¥49/月"
  },
  {
    "provider_id": "zhipu",
    "product_name": "Coding Plan Pro",
    "product_type": "SUBSCRIPTION",
    "endpoints": {
      "OPENAI": "https://open.bigmodel.cn/api/coding/paas/v4"
    },
    "description": "月订阅制，¥149/月"
  },
  {
    "provider_id": "zhipu",
    "product_name": "Coding Plan Max",
    "product_type": "SUBSCRIPTION",
    "endpoints": {
      "OPENAI": "https://open.bigmodel.cn/api/coding/paas/v4"
    },
    "description": "月订阅制，¥469/月"
  }
]
```

- [ ] **Step 4: 创建 volcengine.json**

```json
[
  {
    "provider_id": "volcengine",
    "product_name": "在线推理",
    "product_type": "STANDARD",
    "endpoints": {
      "OPENAI": "https://ark.cn-beijing.volces.com/api/v3"
    },
    "is_default": true,
    "description": "标准在线推理"
  },
  {
    "provider_id": "volcengine",
    "product_name": "Coding Plan",
    "product_type": "SUBSCRIPTION",
    "endpoints": {
      "OPENAI": "https://ark.cn-beijing.volces.com/api/coding/v3",
      "ANTHROPIC": "https://ark.cn-beijing.volces.com/api/coding"
    },
    "description": "AI编码专用，支持双协议"
  }
]
```

- [ ] **Step 5: 创建 qwen.json**

```json
[
  {
    "provider_id": "qwen",
    "product_name": "按量付费-北京",
    "product_type": "STANDARD",
    "endpoints": {
      "OPENAI": "https://dashscope.aliyuncs.com/compatible-mode/v1"
    },
    "is_default": true,
    "description": "北京区域"
  },
  {
    "provider_id": "qwen",
    "product_name": "按量付费-弗吉尼亚",
    "product_type": "STANDARD",
    "endpoints": {
      "OPENAI": "https://dashscope-us.aliyuncs.com/compatible-mode/v1"
    },
    "description": "美国弗吉尼亚区域"
  },
  {
    "provider_id": "qwen",
    "product_name": "按量付费-新加坡",
    "product_type": "STANDARD",
    "endpoints": {
      "OPENAI": "https://dashscope-intl.aliyuncs.com/compatible-mode/v1"
    },
    "description": "新加坡区域"
  }
]
```

- [ ] **Step 6: 创建 minimax.json**

```json
[
  {
    "provider_id": "minimax",
    "product_name": "按量付费",
    "product_type": "STANDARD",
    "endpoints": {
      "OPENAI": "https://api.minimaxi.com/v1",
      "ANTHROPIC": "https://api.minimaxi.com/anthropic"
    },
    "is_default": true,
    "description": "标准按量计费，支持双协议"
  }
]
```

- [ ] **Step 7: 创建 moonshot.json**

```json
[
  {
    "provider_id": "moonshot",
    "product_name": "按量付费",
    "product_type": "STANDARD",
    "endpoints": {
      "OPENAI": "https://api.moonshot.cn/v1"
    },
    "is_default": true,
    "description": "标准按量计费"
  }
]
```

- [ ] **Step 8: 创建 openai.json**

```json
[
  {
    "provider_id": "openai",
    "product_name": "按量付费",
    "product_type": "STANDARD",
    "endpoints": {
      "OPENAI": "https://api.openai.com/v1"
    },
    "is_default": true,
    "description": "标准按量计费"
  }
]
```

- [ ] **Step 9: 创建 anthropic.json**

```json
[
  {
    "provider_id": "anthropic",
    "product_name": "按量付费",
    "product_type": "STANDARD",
    "endpoints": {
      "ANTHROPIC": "https://api.anthropic.com/v1"
    },
    "is_default": true,
    "description": "标准按量计费"
  }
]
```

- [ ] **Step 10: 创建 gemini.json**

```json
[
  {
    "provider_id": "gemini",
    "product_name": "按量付费",
    "product_type": "STANDARD",
    "endpoints": {
      "GEMINI": "https://generativelanguage.googleapis.com/v1beta"
    },
    "is_default": true,
    "description": "标准按量计费"
  }
]
```

- [ ] **Step 11: 创建其他厂商文件**

wenxin.json:
```json
[
  {
    "provider_id": "wenxin",
    "product_name": "按量付费",
    "product_type": "STANDARD",
    "endpoints": {
      "OPENAI": "https://qianfan.baidubce.com/v2"
    },
    "is_default": true,
    "description": "标准按量计费"
  }
]
```

xunfei.json:
```json
[
  {
    "provider_id": "xunfei",
    "product_name": "按量付费",
    "product_type": "STANDARD",
    "endpoints": {
      "OPENAI": "https://spark-api.xf-yun.com/v1"
    },
    "is_default": true,
    "description": "标准按量计费"
  }
]
```

tencent.json:
```json
[
  {
    "provider_id": "tencent",
    "product_name": "按量付费",
    "product_type": "STANDARD",
    "endpoints": {
      "NATIVE": "https://hunyuan.tencentcloudapi.com"
    },
    "is_default": true,
    "description": "标准按量计费"
  }
]
```

baichuan.json:
```json
[
  {
    "provider_id": "baichuan",
    "product_name": "按量付费",
    "product_type": "STANDARD",
    "endpoints": {
      "OPENAI": "https://api.baichuan-ai.com/v1"
    },
    "is_default": true,
    "description": "标准按量计费"
  }
]
```

- [ ] **Step 12: 提交**

```bash
git add gateway-boot/src/main/resources/metadata/products/
git commit -m "feat(data): 新增 13 家厂商的产品元数据 JSON 文件

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## Task 12: 验证 - 启动应用测试同步

**Files:**
- 无新增文件

- [ ] **Step 1: 编译项目**

运行: `cd gateway-boot && ../mvnw clean compile`
预期: 编译成功，无错误

- [ ] **Step 2: 启动应用**

运行: `cd gateway-boot && ../mvnw spring-boot:run`
预期: 应用启动成功，日志显示产品元数据同步

- [ ] **Step 3: 验证数据库**

连接数据库，执行:
```sql
SELECT * FROM product_metadata LIMIT 10;
```
预期: 显示已同步的产品数据

- [ ] **Step 4: 最终提交**

```bash
git add -A
git commit -m "feat: 元数据体系优化完成 - 新增 product_metadata 三级关系

- 新增 product_metadata 表
- 新增 ProductMetadata 实体、Gateway、Service
- 扩展 MetadataSyncService 支持产品同步
- 新增 13 家厂商的产品元数据 JSON 文件

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## 自检清单

### 1. Spec 覆盖检查

| 需求 | 任务 |
|------|------|
| 新增 product_metadata 表 | Task 1 |
| 新增 ProductMetadata 实体 | Task 3 |
| 新增 ProductType/Protocol 枚举 | Task 2 |
| 新增 ProductMetadataGateway | Task 4 |
| 新增 ProductMetadataDomainService | Task 5 |
| 新增 ProductMetadataDo/Repository | Task 6 |
| 新增 ProductMetadataGatewayImpl | Task 7 |
| 扩展 BuiltinMetadataLoader | Task 8 |
| 新增 ProductMetadataService/DTO | Task 9 |
| 扩展 MetadataSyncService | Task 10 |
| 新增 products/*.json 文件 | Task 11 |
| 验证同步机制 | Task 12 |

### 2. Placeholder 检查

- 无 TBD、TODO 等占位符
- 所有代码步骤都包含完整代码

### 3. 类型一致性检查

- ProductType 枚举值与 JSON 中 product_type 一致
- Protocol 枚举值与 endpoints Map key 一致
- 所有实体字段与数据库表字段对应

---

**计划完成，保存至 `docs/superpowers/plans/2026-05-22-metadata-optimization.md`**
