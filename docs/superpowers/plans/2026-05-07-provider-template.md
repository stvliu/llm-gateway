# Provider 模板功能实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现预置大模型厂商模板功能，用户只需设置 API Key 即可快速创建 Provider 配置

**Architecture:** 采用 COLA Light 5.0 分层架构，Gateway 模式隔离数据访问，JGit 同步远程模板仓库，JSONB 存储灵活配置

**Tech Stack:** Java 21 + Spring Boot 3.5.x + JPA + PostgreSQL + JGit + Jackson

---

## Task 1: 数据库迁移 - 创建 provider_templates 表

**Files:**
- Create: `gateway-boot/src/main/resources/db/migration/V7__create_provider_templates.sql`

- [ ] **Step 1: 编写迁移 SQL**

```sql
-- V7__create_provider_templates.sql
-- Provider 模板表

CREATE TABLE provider_templates (
    id BIGSERIAL PRIMARY KEY,
    template_code VARCHAR(64) NOT NULL,
    template_name VARCHAR(128) NOT NULL,
    template_type VARCHAR(32) NOT NULL,
    provider_type VARCHAR(32) NOT NULL,
    provider_config JSON NOT NULL,
    models_config JSON NOT NULL,
    author_id BIGINT,
    author_name VARCHAR(64),
    market_status VARCHAR(32) NOT NULL DEFAULT 'PRIVATE',
    publish_at TIMESTAMP WITH TIME ZONE,
    download_count INT NOT NULL DEFAULT 0,
    tags JSON,
    description TEXT,
    icon_url VARCHAR(512),
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    deleted_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_at TIMESTAMP WITH TIME ZONE,
    updated_by BIGINT
);

-- 唯一索引（排除已删除）
CREATE UNIQUE INDEX uq_provider_templates_code ON provider_templates(template_code) WHERE deleted_at IS NULL;

-- 查询索引
CREATE INDEX ix_provider_templates_type ON provider_templates(template_type, status) WHERE deleted_at IS NULL;
CREATE INDEX ix_provider_templates_provider ON provider_templates(provider_type) WHERE deleted_at IS NULL;
CREATE INDEX ix_provider_templates_market ON provider_templates(market_status) WHERE deleted_at IS NULL;
CREATE INDEX ix_provider_templates_author ON provider_templates(author_id) WHERE deleted_at IS NULL;
```

- [ ] **Step 2: 验证迁移脚本**

```bash
./mvnw flyway:validate -pl gateway-boot
```

---

## Task 2: 创建枚举类

**Files:**
- Create: `gateway-boot/src/main/java/com/codingas/gateway/domain/template/entity/ProviderTemplate.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/domain/template/entity/TemplateType.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/domain/template/entity/MarketStatus.java`

- [ ] **Step 1: 编写 TemplateType 枚举**

```java
package com.codingas.gateway.domain.template.entity;

/**
 * 模板类型枚举
 */
public enum TemplateType {
    /** 官方预置模板 */
    OFFICIAL,
    /** 用户自定义模板 */
    USER
}
```

- [ ] **Step 2: 编写 MarketStatus 枚举**

```java
package com.codingas.gateway.domain.template.entity;

/**
 * 模板市场状态枚举
 */
public enum MarketStatus {
    /** 私有，仅创建者可见 */
    PRIVATE,
    /** 待审核 */
    PENDING,
    /** 已发布到公共市场 */
    PUBLISHED,
    /** 审核拒绝 */
    REJECTED
}
```

- [ ] **Step 3: 编写 ProviderTemplate 实体类**

```java
package com.codingas.gateway.domain.template.entity;

import com.codingas.gateway.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Provider 模板实体
 *
 * <p>预置大模型厂商配置模板，用户可一键创建 Provider。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Slf4j
public class ProviderTemplate extends BaseEntity {

    /** 模板唯一标识 */
    private String templateCode;

    /** 模板显示名称 */
    private String templateName;

    /** 模板类型 */
    private TemplateType templateType;

    /** Provider 类型 */
    private String providerType;

    /** Provider 配置（JSON） */
    private Map<String, Object> providerConfig;

    /** 模型列表配置（JSON） */
    private List<Map<String, Object>> modelsConfig;

    /** 创建者 ID（官方模板为 null） */
    private Long authorId;

    /** 创建者名称 */
    private String authorName;

    /** 市场状态 */
    private MarketStatus marketStatus = MarketStatus.PRIVATE;

    /** 发布时间 */
    private Instant publishAt;

    /** 使用次数 */
    private Integer downloadCount = 0;

    /** 标签列表 */
    private List<String> tags;

    /** 描述 */
    private String description;

    /** 图标 URL */
    private String iconUrl;

    /** 状态 */
    private TemplateStatus status = TemplateStatus.ACTIVE;

    /** 软删除时间 */
    private Instant deletedAt;

    /**
     * 模板状态枚举
     */
    public enum TemplateStatus {
        /** 启用 */
        ACTIVE,
        /** 禁用 */
        DISABLED
    }

    /**
     * 检查模板是否可用
     */
    public boolean isAvailable() {
        return TemplateStatus.ACTIVE.equals(status) && deletedAt == null;
    }

    /**
     * 增加使用次数
     */
    public void incrementDownloadCount() {
        this.downloadCount = (this.downloadCount == null ? 0 : this.downloadCount) + 1;
    }
}
```

- [ ] **Step 4: 编译验证**

```bash
./mvnw compile -pl gateway-boot -DskipTests
```

---

## Task 3: 创建 Gateway 接口

**Files:**
- Create: `gateway-boot/src/main/java/com/codingas/gateway/domain/template/gateway/ProviderTemplateGateway.java`

- [ ] **Step 1: 编写 Gateway 接口**

```java
package com.codingas.gateway.domain.template.gateway;

import com.codingas.gateway.domain.template.entity.ProviderTemplate;
import com.codingas.gateway.domain.template.entity.MarketStatus;
import com.codingas.gateway.domain.template.entity.TemplateType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * Provider 模板网关接口
 *
 * <p>定义模板数据访问操作。</p>
 */
public interface ProviderTemplateGateway {

    /**
     * 保存模板
     */
    ProviderTemplate save(ProviderTemplate template);

    /**
     * 根据 ID 查询模板
     */
    Optional<ProviderTemplate> findById(Long id);

    /**
     * 根据模板编码查询
     */
    Optional<ProviderTemplate> findByTemplateCode(String templateCode);

    /**
     * 分页查询模板
     *
     * @param templateType 模板类型（可选）
     * @param providerType Provider 类型（可选）
     * @param keyword 关键词（可选）
     * @param marketStatus 市场状态（可选）
     * @param pageable 分页参数
     * @return 分页结果
     */
    Page<ProviderTemplate> findByConditions(
        TemplateType templateType,
        String providerType,
        String keyword,
        MarketStatus marketStatus,
        Pageable pageable
    );

    /**
     * 查询所有官方模板
     */
    List<ProviderTemplate> findOfficialTemplates();

    /**
     * 查询公共市场模板
     */
    Page<ProviderTemplate> findMarketTemplates(Pageable pageable);

    /**
     * 根据作者查询模板
     */
    List<ProviderTemplate> findByAuthorId(Long authorId);

    /**
     * 删除模板（软删除）
     */
    void deleteById(Long id);

    /**
     * 检查模板编码是否存在
     */
    boolean existsByTemplateCode(String templateCode);

    /**
     * 更新市场状态
     */
    void updateMarketStatus(Long id, MarketStatus marketStatus);

    /**
     * 增加使用次数
     */
    void incrementDownloadCount(Long id);
}
```

- [ ] **Step 2: 编译验证**

```bash
./mvnw compile -pl gateway-boot -DskipTests
```

---

## Task 4: 创建 DO 和 Repository

**Files:**
- Create: `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/template/database/ProviderTemplateDo.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/template/database/ProviderTemplateRepository.java`

- [ ] **Step 1: 编写 ProviderTemplateDo**

```java
package com.codingas.gateway.infrastructure.template.database;

import com.codingas.gateway.infrastructure.common.BaseDo;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Provider 模板 DO
 *
 * <p>JPA 实体，对应数据库 provider_templates 表。</p>
 */
@Entity
@Table(name = "provider_templates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProviderTemplateDo extends BaseDo {

    @Column(name = "template_code", nullable = false, unique = true, length = 64)
    private String templateCode;

    @Column(name = "template_name", nullable = false, length = 128)
    private String templateName;

    @Enumerated(EnumType.STRING)
    @Column(name = "template_type", nullable = false, length = 32)
    private TemplateType templateType;

    @Column(name = "provider_type", nullable = false, length = 32)
    private String providerType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "provider_config", nullable = false, columnDefinition = "json")
    private Map<String, Object> providerConfig;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "models_config", nullable = false, columnDefinition = "json")
    private List<Map<String, Object>> modelsConfig;

    @Column(name = "author_id")
    private Long authorId;

    @Column(name = "author_name", length = 64)
    private String authorName;

    @Enumerated(EnumType.STRING)
    @Column(name = "market_status", nullable = false, length = 32)
    private MarketStatus marketStatus = MarketStatus.PRIVATE;

    @Column(name = "publish_at")
    private Instant publishAt;

    @Column(name = "download_count", nullable = false)
    private Integer downloadCount = 0;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tags", columnDefinition = "json")
    private List<String> tags;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "icon_url", length = 512)
    private String iconUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private TemplateStatus status = TemplateStatus.ACTIVE;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    /**
     * 模板类型枚举
     */
    public enum TemplateType {
        OFFICIAL, USER
    }

    /**
     * 市场状态枚举
     */
    public enum MarketStatus {
        PRIVATE, PENDING, PUBLISHED, REJECTED
    }

    /**
     * 模板状态枚举
     */
    public enum TemplateStatus {
        ACTIVE, DISABLED
    }
}
```

- [ ] **Step 2: 编写 ProviderTemplateRepository**

```java
package com.codingas.gateway.infrastructure.template.database;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProviderTemplateRepository extends JpaRepository<ProviderTemplateDo, Long> {

    /**
     * 根据模板编码查询（排除已删除）
     */
    @Query("SELECT t FROM ProviderTemplateDo t WHERE t.templateCode = :code AND t.deletedAt IS NULL")
    Optional<ProviderTemplateDo> findByTemplateCode(@Param("code") String code);

    /**
     * 查询所有官方模板（排除已删除）
     */
    @Query("SELECT t FROM ProviderTemplateDo t WHERE t.templateType = 'OFFICIAL' AND t.deletedAt IS NULL AND t.status = 'ACTIVE'")
    List<ProviderTemplateDo> findOfficialTemplates();

    /**
     * 分页查询公共市场模板
     */
    @Query("SELECT t FROM ProviderTemplateDo t WHERE t.marketStatus = 'PUBLISHED' AND t.deletedAt IS NULL AND t.status = 'ACTIVE'")
    Page<ProviderTemplateDo> findMarketTemplates(Pageable pageable);

    /**
     * 根据作者查询
     */
    @Query("SELECT t FROM ProviderTemplateDo t WHERE t.authorId = :authorId AND t.deletedAt IS NULL")
    List<ProviderTemplateDo> findByAuthorId(@Param("authorId") Long authorId);

    /**
     * 检查模板编码是否存在
     */
    @Query("SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END FROM ProviderTemplateDo t WHERE t.templateCode = :code AND t.deletedAt IS NULL")
    boolean existsByTemplateCode(@Param("code") String code);

    /**
     * 软删除
     */
    @Modifying
    @Query("UPDATE ProviderTemplateDo t SET t.deletedAt = :deletedAt WHERE t.id = :id")
    void softDelete(@Param("id") Long id, @Param("deletedAt") Instant deletedAt);

    /**
     * 更新市场状态
     */
    @Modifying
    @Query("UPDATE ProviderTemplateDo t SET t.marketStatus = :status WHERE t.id = :id")
    void updateMarketStatus(@Param("id") Long id, @Param("status") ProviderTemplateDo.MarketStatus status);

    /**
     * 增加使用次数
     */
    @Modifying
    @Query("UPDATE ProviderTemplateDo t SET t.downloadCount = t.downloadCount + 1 WHERE t.id = :id")
    void incrementDownloadCount(@Param("id") Long id);

    /**
     * 动态条件查询
     */
    @Query("SELECT t FROM ProviderTemplateDo t WHERE t.deletedAt IS NULL " +
           "AND (:templateType IS NULL OR t.templateType = :templateType) " +
           "AND (:providerType IS NULL OR t.providerType = :providerType) " +
           "AND (:keyword IS NULL OR LOWER(t.templateName) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:marketStatus IS NULL OR t.marketStatus = :marketStatus)")
    Page<ProviderTemplateDo> findByConditions(
        @Param("templateType") ProviderTemplateDo.TemplateType templateType,
        @Param("providerType") String providerType,
        @Param("keyword") String keyword,
        @Param("marketStatus") ProviderTemplateDo.MarketStatus marketStatus,
        Pageable pageable
    );
}
```

- [ ] **Step 3: 编译验证**

```bash
./mvnw compile -pl gateway-boot -DskipTests
```

---

## Task 5: 实现 Gateway

**Files:**
- Create: `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/template/gateway/ProviderTemplateGatewayImpl.java`

- [ ] **Step 1: 编写 Gateway 实现**

```java
package com.codingas.gateway.infrastructure.template.gateway;

import com.codingas.gateway.domain.template.entity.MarketStatus;
import com.codingas.gateway.domain.template.entity.ProviderTemplate;
import com.codingas.gateway.domain.template.entity.TemplateType;
import com.codingas.gateway.domain.template.gateway.ProviderTemplateGateway;
import com.codingas.gateway.infrastructure.template.database.ProviderTemplateDo;
import com.codingas.gateway.infrastructure.template.database.ProviderTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Provider 模板网关实现
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProviderTemplateGatewayImpl implements ProviderTemplateGateway {

    private final ProviderTemplateRepository repository;

    @Override
    public ProviderTemplate save(ProviderTemplate template) {
        ProviderTemplateDo doEntity = toDo(template);
        ProviderTemplateDo saved = repository.save(doEntity);
        return toEntity(saved);
    }

    @Override
    public Optional<ProviderTemplate> findById(Long id) {
        return repository.findById(id).map(this::toEntity);
    }

    @Override
    public Optional<ProviderTemplate> findByTemplateCode(String templateCode) {
        return repository.findByTemplateCode(templateCode).map(this::toEntity);
    }

    @Override
    public Page<ProviderTemplate> findByConditions(
            TemplateType templateType,
            String providerType,
            String keyword,
            MarketStatus marketStatus,
            Pageable pageable) {
        ProviderTemplateDo.TemplateType doTemplateType = templateType != null
            ? ProviderTemplateDo.TemplateType.valueOf(templateType.name())
            : null;
        ProviderTemplateDo.MarketStatus doMarketStatus = marketStatus != null
            ? ProviderTemplateDo.MarketStatus.valueOf(marketStatus.name())
            : null;
        return repository.findByConditions(doTemplateType, providerType, keyword, doMarketStatus, pageable)
            .map(this::toEntity);
    }

    @Override
    public List<ProviderTemplate> findOfficialTemplates() {
        return repository.findOfficialTemplates().stream()
            .map(this::toEntity)
            .collect(Collectors.toList());
    }

    @Override
    public Page<ProviderTemplate> findMarketTemplates(Pageable pageable) {
        return repository.findMarketTemplates(pageable).map(this::toEntity);
    }

    @Override
    public List<ProviderTemplate> findByAuthorId(Long authorId) {
        return repository.findByAuthorId(authorId).stream()
            .map(this::toEntity)
            .collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        repository.softDelete(id, Instant.now());
    }

    @Override
    public boolean existsByTemplateCode(String templateCode) {
        return repository.existsByTemplateCode(templateCode);
    }

    @Override
    public void updateMarketStatus(Long id, MarketStatus marketStatus) {
        repository.updateMarketStatus(id, ProviderTemplateDo.MarketStatus.valueOf(marketStatus.name()));
    }

    @Override
    public void incrementDownloadCount(Long id) {
        repository.incrementDownloadCount(id);
    }

    /**
     * DO 转 Entity
     */
    private ProviderTemplate toEntity(ProviderTemplateDo doEntity) {
        if (doEntity == null) {
            return null;
        }
        ProviderTemplate entity = new ProviderTemplate();
        entity.setId(doEntity.getId());
        entity.setTemplateCode(doEntity.getTemplateCode());
        entity.setTemplateName(doEntity.getTemplateName());
        entity.setProviderType(doEntity.getProviderType());
        entity.setProviderConfig(doEntity.getProviderConfig());
        entity.setModelsConfig(doEntity.getModelsConfig());
        entity.setAuthorId(doEntity.getAuthorId());
        entity.setAuthorName(doEntity.getAuthorName());
        entity.setPublishAt(doEntity.getPublishAt());
        entity.setDownloadCount(doEntity.getDownloadCount());
        entity.setTags(doEntity.getTags());
        entity.setDescription(doEntity.getDescription());
        entity.setIconUrl(doEntity.getIconUrl());
        entity.setDeletedAt(doEntity.getDeletedAt());
        entity.setCreatedAt(doEntity.getCreatedAt());
        entity.setCreatedBy(doEntity.getCreatedBy());
        entity.setUpdatedAt(doEntity.getUpdatedAt());
        entity.setUpdatedBy(doEntity.getUpdatedBy());

        // 枚举转换
        if (doEntity.getTemplateType() != null) {
            entity.setTemplateType(TemplateType.valueOf(doEntity.getTemplateType().name()));
        }
        if (doEntity.getMarketStatus() != null) {
            entity.setMarketStatus(MarketStatus.valueOf(doEntity.getMarketStatus().name()));
        }
        if (doEntity.getStatus() != null) {
            entity.setStatus(ProviderTemplate.TemplateStatus.valueOf(doEntity.getStatus().name()));
        }
        return entity;
    }

    /**
     * Entity 转 DO
     */
    private ProviderTemplateDo toDo(ProviderTemplate entity) {
        if (entity == null) {
            return null;
        }
        ProviderTemplateDo doEntity = new ProviderTemplateDo();
        if (entity.getId() != null) {
            doEntity.setId(entity.getId());
        }
        doEntity.setTemplateCode(entity.getTemplateCode());
        doEntity.setTemplateName(entity.getTemplateName());
        doEntity.setProviderType(entity.getProviderType());
        doEntity.setProviderConfig(entity.getProviderConfig());
        doEntity.setModelsConfig(entity.getModelsConfig());
        doEntity.setAuthorId(entity.getAuthorId());
        doEntity.setAuthorName(entity.getAuthorName());
        doEntity.setPublishAt(entity.getPublishAt());
        doEntity.setDownloadCount(entity.getDownloadCount());
        doEntity.setTags(entity.getTags());
        doEntity.setDescription(entity.getDescription());
        doEntity.setIconUrl(entity.getIconUrl());
        doEntity.setDeletedAt(entity.getDeletedAt());

        // 枚举转换
        if (entity.getTemplateType() != null) {
            doEntity.setTemplateType(ProviderTemplateDo.TemplateType.valueOf(entity.getTemplateType().name()));
        }
        if (entity.getMarketStatus() != null) {
            doEntity.setMarketStatus(ProviderTemplateDo.MarketStatus.valueOf(entity.getMarketStatus().name()));
        }
        if (entity.getStatus() != null) {
            doEntity.setStatus(ProviderTemplateDo.TemplateStatus.valueOf(entity.getStatus().name()));
        }
        return doEntity;
    }
}
```

- [ ] **Step 2: 编译验证**

```bash
./mvnw compile -pl gateway-boot -DskipTests
```

---

## Task 6: 单元测试 - Gateway

**Files:**
- Create: `gateway-boot/src/test/java/com/codingas/gateway/infrastructure/template/gateway/ProviderTemplateGatewayImplTest.java`

- [ ] **Step 1: 编写测试类**

```java
package com.codingas.gateway.infrastructure.template.gateway;

import com.codingas.gateway.domain.template.entity.MarketStatus;
import com.codingas.gateway.domain.template.entity.ProviderTemplate;
import com.codingas.gateway.domain.template.entity.TemplateType;
import com.codingas.gateway.infrastructure.template.database.ProviderTemplateDo;
import com.codingas.gateway.infrastructure.template.database.ProviderTemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ProviderTemplateGateway 单元测试
 */
@ExtendWith(MockitoExtension.class)
class ProviderTemplateGatewayImplTest {

    @Mock
    private ProviderTemplateRepository repository;

    private ProviderTemplateGatewayImpl gateway;

    @BeforeEach
    void setUp() {
        gateway = new ProviderTemplateGatewayImpl(repository);
    }

    @Test
    @DisplayName("保存模板成功")
    void save_success() {
        // Arrange
        ProviderTemplate template = createTestTemplate();
        ProviderTemplateDo savedDo = createTestDo();
        savedDo.setId(1L);

        when(repository.save(any())).thenReturn(savedDo);

        // Act
        ProviderTemplate result = gateway.save(template);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTemplateCode()).isEqualTo("openai");
        verify(repository).save(any());
    }

    @Test
    @DisplayName("根据 ID 查询模板")
    void findById_success() {
        // Arrange
        ProviderTemplateDo doEntity = createTestDo();
        doEntity.setId(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(doEntity));

        // Act
        Optional<ProviderTemplate> result = gateway.findById(1L);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getTemplateCode()).isEqualTo("openai");
    }

    @Test
    @DisplayName("根据模板编码查询")
    void findByTemplateCode_success() {
        // Arrange
        ProviderTemplateDo doEntity = createTestDo();
        when(repository.findByTemplateCode("openai")).thenReturn(Optional.of(doEntity));

        // Act
        Optional<ProviderTemplate> result = gateway.findByTemplateCode("openai");

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getTemplateCode()).isEqualTo("openai");
    }

    @Test
    @DisplayName("查询所有官方模板")
    void findOfficialTemplates_success() {
        // Arrange
        ProviderTemplateDo do1 = createTestDo();
        do1.setId(1L);
        do1.setTemplateType(ProviderTemplateDo.TemplateType.OFFICIAL);

        ProviderTemplateDo do2 = createTestDo();
        do2.setId(2L);
        do2.setTemplateCode("anthropic");
        do2.setTemplateType(ProviderTemplateDo.TemplateType.OFFICIAL);

        when(repository.findOfficialTemplates()).thenReturn(List.of(do1, do2));

        // Act
        List<ProviderTemplate> result = gateway.findOfficialTemplates();

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getTemplateType()).isEqualTo(TemplateType.OFFICIAL);
    }

    @Test
    @DisplayName("检查模板编码存在")
    void existsByTemplateCode_true() {
        // Arrange
        when(repository.existsByTemplateCode("openai")).thenReturn(true);

        // Act
        boolean result = gateway.existsByTemplateCode("openai");

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("增加使用次数")
    void incrementDownloadCount_success() {
        // Act
        gateway.incrementDownloadCount(1L);

        // Assert
        verify(repository).incrementDownloadCount(1L);
    }

    private ProviderTemplate createTestTemplate() {
        ProviderTemplate template = new ProviderTemplate();
        template.setTemplateCode("openai");
        template.setTemplateName("OpenAI");
        template.setTemplateType(TemplateType.OFFICIAL);
        template.setProviderType("OPENAI");
        template.setProviderConfig(java.util.Map.of("base_url", "https://api.openai.com"));
        template.setModelsConfig(List.of(java.util.Map.of("provider_model_id", "gpt-4o")));
        return template;
    }

    private ProviderTemplateDo createTestDo() {
        ProviderTemplateDo doEntity = new ProviderTemplateDo();
        doEntity.setTemplateCode("openai");
        doEntity.setTemplateName("OpenAI");
        doEntity.setTemplateType(ProviderTemplateDo.TemplateType.OFFICIAL);
        doEntity.setProviderType("OPENAI");
        doEntity.setProviderConfig(java.util.Map.of("base_url", "https://api.openai.com"));
        doEntity.setModelsConfig(List.of(java.util.Map.of("provider_model_id", "gpt-4o")));
        doEntity.setMarketStatus(ProviderTemplateDo.MarketStatus.PRIVATE);
        doEntity.setStatus(ProviderTemplateDo.TemplateStatus.ACTIVE);
        doEntity.setDownloadCount(0);
        return doEntity;
    }
}
```

- [ ] **Step 2: 运行测试**

```bash
./mvnw test -pl gateway-boot -Dtest=ProviderTemplateGatewayImplTest
```

---

## Task 7: 创建 Application Service

**Files:**
- Create: `gateway-boot/src/main/java/com/codingas/gateway/application/template/ProviderTemplateService.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/application/template/dto/TemplateCreateRequest.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/application/template/dto/TemplateUpdateRequest.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/application/template/dto/TemplateResponse.java`

- [ ] **Step 1: 编写 DTO 类**

```java
package com.codingas.gateway.application.template.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 创建模板请求
 */
@Data
public class TemplateCreateRequest {

    @NotBlank(message = "模板编码不能为空")
    private String templateCode;

    @NotBlank(message = "模板名称不能为空")
    private String templateName;

    @NotBlank(message = "Provider 类型不能为空")
    private String providerType;

    @NotNull(message = "Provider 配置不能为空")
    private Map<String, Object> providerConfig;

    @NotNull(message = "模型配置不能为空")
    private List<Map<String, Object>> modelsConfig;

    private String description;
    private String iconUrl;
    private List<String> tags;
}
```

```java
package com.codingas.gateway.application.template.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 更新模板请求
 */
@Data
public class TemplateUpdateRequest {

    private String templateName;
    private Map<String, Object> providerConfig;
    private List<Map<String, Object>> modelsConfig;
    private String description;
    private String iconUrl;
    private List<String> tags;
    private String status;
}
```

```java
package com.codingas.gateway.application.template.dto;

import com.codingas.gateway.domain.template.entity.MarketStatus;
import com.codingas.gateway.domain.template.entity.TemplateType;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 模板响应
 */
@Data
@Builder
public class TemplateResponse {

    private Long id;
    private String templateCode;
    private String templateName;
    private TemplateType templateType;
    private String providerType;
    private Map<String, Object> providerConfig;
    private List<Map<String, Object>> modelsConfig;
    private Long authorId;
    private String authorName;
    private MarketStatus marketStatus;
    private Instant publishAt;
    private Integer downloadCount;
    private List<String> tags;
    private String description;
    private String iconUrl;
    private String status;
    private Instant createdAt;
    private Instant updatedAt;

    /**
     * 模型数量
     */
    private Integer modelCount;
}
```

- [ ] **Step 2: 编写 Service**

```java
package com.codingas.gateway.application.template;

import com.codingas.gateway.application.template.dto.TemplateCreateRequest;
import com.codingas.gateway.application.template.dto.TemplateResponse;
import com.codingas.gateway.application.template.dto.TemplateUpdateRequest;
import com.codingas.gateway.domain.template.entity.MarketStatus;
import com.codingas.gateway.domain.template.entity.ProviderTemplate;
import com.codingas.gateway.domain.template.entity.TemplateType;
import com.codingas.gateway.domain.template.gateway.ProviderTemplateGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Provider 模板应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProviderTemplateService {

    private final ProviderTemplateGateway gateway;

    /**
     * 创建自定义模板
     */
    @Transactional
    public TemplateResponse createTemplate(TemplateCreateRequest request, Long userId, String username) {
        // 检查编码唯一性
        if (gateway.existsByTemplateCode(request.getTemplateCode())) {
            throw new IllegalArgumentException("模板编码已存在: " + request.getTemplateCode());
        }

        ProviderTemplate template = new ProviderTemplate();
        template.setTemplateCode(request.getTemplateCode());
        template.setTemplateName(request.getTemplateName());
        template.setTemplateType(TemplateType.USER);
        template.setProviderType(request.getProviderType());
        template.setProviderConfig(request.getProviderConfig());
        template.setModelsConfig(request.getModelsConfig());
        template.setDescription(request.getDescription());
        template.setIconUrl(request.getIconUrl());
        template.setTags(request.getTags());
        template.setAuthorId(userId);
        template.setAuthorName(username);
        template.setMarketStatus(MarketStatus.PRIVATE);
        template.setDownloadCount(0);

        ProviderTemplate saved = gateway.save(template);
        return toResponse(saved);
    }

    /**
     * 更新模板
     */
    @Transactional
    public TemplateResponse updateTemplate(Long id, TemplateUpdateRequest request) {
        ProviderTemplate template = gateway.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("模板不存在: " + id));

        // 官方模板不允许修改
        if (TemplateType.OFFICIAL.equals(template.getTemplateType())) {
            throw new IllegalStateException("官方模板不允许修改");
        }

        if (request.getTemplateName() != null) {
            template.setTemplateName(request.getTemplateName());
        }
        if (request.getProviderConfig() != null) {
            template.setProviderConfig(request.getProviderConfig());
        }
        if (request.getModelsConfig() != null) {
            template.setModelsConfig(request.getModelsConfig());
        }
        if (request.getDescription() != null) {
            template.setDescription(request.getDescription());
        }
        if (request.getIconUrl() != null) {
            template.setIconUrl(request.getIconUrl());
        }
        if (request.getTags() != null) {
            template.setTags(request.getTags());
        }

        ProviderTemplate saved = gateway.save(template);
        return toResponse(saved);
    }

    /**
     * 删除模板
     */
    @Transactional
    public void deleteTemplate(Long id) {
        ProviderTemplate template = gateway.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("模板不存在: " + id));

        if (TemplateType.OFFICIAL.equals(template.getTemplateType())) {
            throw new IllegalStateException("官方模板不允许删除");
        }

        gateway.deleteById(id);
    }

    /**
     * 查询模板详情
     */
    public TemplateResponse getTemplate(Long id) {
        ProviderTemplate template = gateway.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("模板不存在: " + id));
        return toResponse(template);
    }

    /**
     * 分页查询模板
     */
    public Page<TemplateResponse> listTemplates(
            TemplateType type,
            String providerType,
            String keyword,
            MarketStatus marketStatus,
            int page,
            int limit) {

        Pageable pageable = PageRequest.of(page - 1, limit, Sort.by("createdAt").descending());
        Page<ProviderTemplate> result = gateway.findByConditions(type, providerType, keyword, marketStatus, pageable);
        return result.map(this::toResponse);
    }

    /**
     * 发布模板到公共市场
     */
    @Transactional
    public void publishTemplate(Long id) {
        ProviderTemplate template = gateway.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("模板不存在: " + id));

        if (TemplateType.OFFICIAL.equals(template.getTemplateType())) {
            throw new IllegalStateException("官方模板无需发布");
        }

        // TODO: 添加安全检测逻辑
        template.setMarketStatus(MarketStatus.PUBLISHED);
        template.setPublishAt(Instant.now());
        gateway.save(template);
    }

    /**
     * 转换为响应对象
     */
    private TemplateResponse toResponse(ProviderTemplate template) {
        int modelCount = template.getModelsConfig() != null ? template.getModelsConfig().size() : 0;
        return TemplateResponse.builder()
            .id(template.getId())
            .templateCode(template.getTemplateCode())
            .templateName(template.getTemplateName())
            .templateType(template.getTemplateType())
            .providerType(template.getProviderType())
            .providerConfig(template.getProviderConfig())
            .modelsConfig(template.getModelsConfig())
            .authorId(template.getAuthorId())
            .authorName(template.getAuthorName())
            .marketStatus(template.getMarketStatus())
            .publishAt(template.getPublishAt())
            .downloadCount(template.getDownloadCount())
            .tags(template.getTags())
            .description(template.getDescription())
            .iconUrl(template.getIconUrl())
            .status(template.getStatus() != null ? template.getStatus().name() : null)
            .createdAt(template.getCreatedAt())
            .updatedAt(template.getUpdatedAt())
            .modelCount(modelCount)
            .build();
    }
}
```

- [ ] **Step 3: 编译验证**

```bash
./mvnw compile -pl gateway-boot -DskipTests
```

---

## Task 8: 单元测试 - Service

**Files:**
- Create: `gateway-boot/src/test/java/com/codingas/gateway/application/template/ProviderTemplateServiceTest.java`

- [ ] **Step 1: 编写测试类**

```java
package com.codingas.gateway.application.template;

import com.codingas.gateway.application.template.dto.TemplateCreateRequest;
import com.codingas.gateway.application.template.dto.TemplateResponse;
import com.codingas.gateway.application.template.dto.TemplateUpdateRequest;
import com.codingas.gateway.domain.template.entity.MarketStatus;
import com.codingas.gateway.domain.template.entity.ProviderTemplate;
import com.codingas.gateway.domain.template.entity.TemplateType;
import com.codingas.gateway.domain.template.gateway.ProviderTemplateGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ProviderTemplateService 单元测试
 */
@ExtendWith(MockitoExtension.class)
class ProviderTemplateServiceTest {

    @Mock
    private ProviderTemplateGateway gateway;

    private ProviderTemplateService service;

    @BeforeEach
    void setUp() {
        service = new ProviderTemplateService(gateway);
    }

    @Test
    @DisplayName("创建自定义模板成功")
    void createTemplate_success() {
        // Arrange
        TemplateCreateRequest request = createTestRequest();
        when(gateway.existsByTemplateCode("test-provider")).thenReturn(false);
        when(gateway.save(any())).thenAnswer(inv -> {
            ProviderTemplate t = inv.getArgument(0);
            t.setId(1L);
            return t;
        });

        // Act
        TemplateResponse response = service.createTemplate(request, 1L, "testuser");

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getTemplateCode()).isEqualTo("test-provider");
        assertThat(response.getTemplateType()).isEqualTo(TemplateType.USER);
        verify(gateway).save(any());
    }

    @Test
    @DisplayName("创建模板时编码重复抛出异常")
    void createTemplate_duplicateCode_throws() {
        // Arrange
        TemplateCreateRequest request = createTestRequest();
        when(gateway.existsByTemplateCode("test-provider")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> service.createTemplate(request, 1L, "testuser"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("模板编码已存在");
    }

    @Test
    @DisplayName("更新模板成功")
    void updateTemplate_success() {
        // Arrange
        ProviderTemplate existing = createTestTemplate();
        existing.setId(1L);
        existing.setTemplateType(TemplateType.USER);

        TemplateUpdateRequest request = new TemplateUpdateRequest();
        request.setTemplateName("Updated Name");

        when(gateway.findById(1L)).thenReturn(Optional.of(existing));
        when(gateway.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Act
        TemplateResponse response = service.updateTemplate(1L, request);

        // Assert
        assertThat(response.getTemplateName()).isEqualTo("Updated Name");
    }

    @Test
    @DisplayName("更新官方模板抛出异常")
    void updateTemplate_officialTemplate_throws() {
        // Arrange
        ProviderTemplate official = createTestTemplate();
        official.setId(1L);
        official.setTemplateType(TemplateType.OFFICIAL);

        when(gateway.findById(1L)).thenReturn(Optional.of(official));

        // Act & Assert
        assertThatThrownBy(() -> service.updateTemplate(1L, new TemplateUpdateRequest()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("官方模板不允许修改");
    }

    @Test
    @DisplayName("删除模板成功")
    void deleteTemplate_success() {
        // Arrange
        ProviderTemplate template = createTestTemplate();
        template.setId(1L);
        template.setTemplateType(TemplateType.USER);

        when(gateway.findById(1L)).thenReturn(Optional.of(template));
        doNothing().when(gateway).deleteById(1L);

        // Act
        service.deleteTemplate(1L);

        // Assert
        verify(gateway).deleteById(1L);
    }

    @Test
    @DisplayName("发布模板到公共市场")
    void publishTemplate_success() {
        // Arrange
        ProviderTemplate template = createTestTemplate();
        template.setId(1L);
        template.setTemplateType(TemplateType.USER);
        template.setMarketStatus(MarketStatus.PRIVATE);

        when(gateway.findById(1L)).thenReturn(Optional.of(template));
        when(gateway.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Act
        service.publishTemplate(1L);

        // Assert
        verify(gateway).save(any());
    }

    private TemplateCreateRequest createTestRequest() {
        TemplateCreateRequest request = new TemplateCreateRequest();
        request.setTemplateCode("test-provider");
        request.setTemplateName("Test Provider");
        request.setProviderType("OTHER");
        request.setProviderConfig(Map.of("base_url", "https://api.test.com"));
        request.setModelsConfig(List.of(Map.of("provider_model_id", "model-1")));
        return request;
    }

    private ProviderTemplate createTestTemplate() {
        ProviderTemplate template = new ProviderTemplate();
        template.setTemplateCode("test-provider");
        template.setTemplateName("Test Provider");
        template.setProviderType("OTHER");
        template.setProviderConfig(Map.of("base_url", "https://api.test.com"));
        template.setModelsConfig(List.of(Map.of("provider_model_id", "model-1")));
        return template;
    }
}
```

- [ ] **Step 2: 运行测试**

```bash
./mvnw test -pl gateway-boot -Dtest=ProviderTemplateServiceTest
```

---

## Task 9: 创建 Controller

**Files:**
- Create: `gateway-boot/src/main/java/com/codingas/gateway/adapter/admin/controller/ProviderTemplateController.java`

- [ ] **Step 1: 编写 Controller**

```java
package com.codingas.gateway.adapter.admin.controller;

import com.codingas.gateway.application.template.ProviderTemplateService;
import com.codingas.gateway.application.template.dto.TemplateCreateRequest;
import com.codingas.gateway.application.template.dto.TemplateResponse;
import com.codingas.gateway.application.template.dto.TemplateUpdateRequest;
import com.codingas.gateway.domain.template.entity.MarketStatus;
import com.codingas.gateway.domain.template.entity.TemplateType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Provider 模板管理接口
 */
@RestController
@RequestMapping("/api/v1/templates")
@RequiredArgsConstructor
public class ProviderTemplateController {

    private final ProviderTemplateService service;

    /**
     * 分页查询模板列表
     */
    @GetMapping
    public ResponseEntity<Page<TemplateResponse>> listTemplates(
            @RequestParam(required = false) TemplateType type,
            @RequestParam(required = false) String providerType,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) MarketStatus marketStatus,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit) {

        limit = Math.min(limit, 100);
        Page<TemplateResponse> result = service.listTemplates(type, providerType, keyword, marketStatus, page, limit);
        return ResponseEntity.ok(result);
    }

    /**
     * 获取模板详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<TemplateResponse> getTemplate(@PathVariable Long id) {
        TemplateResponse response = service.getTemplate(id);
        return ResponseEntity.ok(response);
    }

    /**
     * 创建自定义模板
     */
    @PostMapping
    public ResponseEntity<TemplateResponse> createTemplate(
            @Valid @RequestBody TemplateCreateRequest request,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId,
            @RequestHeader(value = "X-Username", defaultValue = "admin") String username) {

        TemplateResponse response = service.createTemplate(request, userId, username);
        return ResponseEntity.ok(response);
    }

    /**
     * 更新模板
     */
    @PutMapping("/{id}")
    public ResponseEntity<TemplateResponse> updateTemplate(
            @PathVariable Long id,
            @Valid @RequestBody TemplateUpdateRequest request) {

        TemplateResponse response = service.updateTemplate(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 删除模板
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTemplate(@PathVariable Long id) {
        service.deleteTemplate(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 发布到公共市场
     */
    @PostMapping("/{id}/publish")
    public ResponseEntity<Void> publishTemplate(@PathVariable Long id) {
        service.publishTemplate(id);
        return ResponseEntity.ok().build();
    }
}
```

- [ ] **Step 2: 编译验证**

```bash
./mvnw compile -pl gateway-boot -DskipTests
```

---

## Task 10: 添加 JGit 依赖

**Files:**
- Modify: `gateway-boot/pom.xml`

- [ ] **Step 1: 添加 JGit 依赖**

在 `pom.xml` 的 `<dependencies>` 中添加：

```xml
<!-- JGit for Git repository operations -->
<dependency>
    <groupId>org.eclipse.jgit</groupId>
    <artifactId>org.eclipse.jgit</artifactId>
    <version>6.8.0.202311291450-r</version>
</dependency>
```

- [ ] **Step 2: 重新加载依赖**

```bash
./mvnw dependency:resolve -pl gateway-boot
```

---

## Task 11: 创建模板同步配置

**Files:**
- Create: `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/template/config/TemplateSyncConfig.java`

- [ ] **Step 1: 编写配置类**

```java
package com.codingas.gateway.infrastructure.template.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 模板同步配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "template.git")
public class TemplateSyncConfig {

    /**
     * Git 仓库地址
     */
    private String url = "https://github.com/codingas/llm-gateway-templates.git";

    /**
     * 分支名称
     */
    private String branch = "main";

    /**
     * 本地存储路径
     */
    private String localPath = System.getProperty("user.home") + "/.llm-gateway/templates";

    /**
     * 启动时是否同步
     */
    private boolean syncOnStartup = true;

    /**
     * 定时同步间隔（秒），0 表示禁用
     */
    private int syncInterval = 3600;
}
```

- [ ] **Step 2: 编译验证**

```bash
./mvnw compile -pl gateway-boot -DskipTests
```

---

## Task 12: 创建 Git 同步服务

**Files:**
- Create: `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/template/repository/GitTemplateRepository.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/application/template/OfficialTemplateSyncService.java`

- [ ] **Step 1: 编写 GitTemplateRepository**

```java
package com.codingas.gateway.infrastructure.template.repository;

import com.codingas.gateway.infrastructure.template.config.TemplateSyncConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Git 模板仓库操作
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GitTemplateRepository {

    private final TemplateSyncConfig config;
    private final ObjectMapper objectMapper;

    /**
     * 克隆或拉取远程仓库
     */
    public void syncRepository() throws GitAPIException, IOException {
        File repoDir = new File(config.getLocalPath());

        if (repoDir.exists()) {
            // 拉取更新
            log.info("Pulling template repository from {}", config.getUrl());
            try (Git git = Git.open(repoDir)) {
                git.pull().call();
            }
        } else {
            // 克隆仓库
            log.info("Cloning template repository from {}", config.getUrl());
            repoDir.getParentFile().mkdirs();
            Git.cloneRepository()
                .setURI(config.getUrl())
                .setDirectory(repoDir)
                .setBranch(config.getBranch())
                .call()
                .close();
        }
    }

    /**
     * 读取所有模板文件
     */
    public List<Map<String, Object>> loadTemplates() throws IOException {
        List<Map<String, Object>> templates = new ArrayList<>();
        File templatesDir = new File(config.getLocalPath(), "templates");

        if (!templatesDir.exists() || !templatesDir.isDirectory()) {
            log.warn("Templates directory not found: {}", templatesDir.getAbsolutePath());
            return templates;
        }

        File[] files = templatesDir.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null) {
            return templates;
        }

        for (File file : files) {
            try {
                String content = Files.readString(file.toPath());
                @SuppressWarnings("unchecked")
                Map<String, Object> template = objectMapper.readValue(content, Map.class);
                templates.add(template);
                log.debug("Loaded template: {}", template.get("template_code"));
            } catch (Exception e) {
                log.error("Failed to load template file: {}", file.getName(), e);
            }
        }

        return templates;
    }
}
```

- [ ] **Step 2: 编写 OfficialTemplateSyncService**

```java
package com.codingas.gateway.application.template;

import com.codingas.gateway.domain.template.entity.MarketStatus;
import com.codingas.gateway.domain.template.entity.ProviderTemplate;
import com.codingas.gateway.domain.template.entity.TemplateType;
import com.codingas.gateway.domain.template.gateway.ProviderTemplateGateway;
import com.codingas.gateway.infrastructure.template.repository.GitTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 官方模板同步服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OfficialTemplateSyncService {

    private final GitTemplateRepository gitRepository;
    private final ProviderTemplateGateway gateway;

    /**
     * 同步官方模板
     */
    @Transactional
    public SyncResult syncTemplates() {
        try {
            // 1. 同步 Git 仓库
            gitRepository.syncRepository();

            // 2. 加载模板文件
            List<Map<String, Object>> templates = gitRepository.loadTemplates();

            int addedCount = 0;
            int updatedCount = 0;

            // 3. 同步到数据库
            for (Map<String, Object> templateData : templates) {
                String templateCode = (String) templateData.get("template_code");
                if (templateCode == null) {
                    continue;
                }

                ProviderTemplate template = gateway.findByTemplateCode(templateCode)
                    .orElseGet(() -> {
                        ProviderTemplate t = new ProviderTemplate();
                        t.setTemplateCode(templateCode);
                        t.setTemplateType(TemplateType.OFFICIAL);
                        t.setMarketStatus(MarketStatus.PUBLISHED);
                        t.setDownloadCount(0);
                        return t;
                    });

                // 更新模板数据
                updateTemplateFromMap(template, templateData);

                gateway.save(template);

                if (template.getId() == null) {
                    addedCount++;
                } else {
                    updatedCount++;
                }
            }

            log.info("Template sync completed: {} added, {} updated", addedCount, updatedCount);
            return new SyncResult(templates.size(), addedCount, updatedCount, Instant.now());

        } catch (Exception e) {
            log.error("Failed to sync templates", e);
            throw new RuntimeException("模板同步失败: " + e.getMessage(), e);
        }
    }

    /**
     * 从 Map 更新模板属性
     */
    @SuppressWarnings("unchecked")
    private void updateTemplateFromMap(ProviderTemplate template, Map<String, Object> data) {
        template.setTemplateName((String) data.get("template_name"));
        template.setProviderType((String) data.get("provider_type"));
        template.setProviderConfig((Map<String, Object>) data.get("provider_config"));
        template.setModelsConfig((List<Map<String, Object>>) data.get("models_config"));
        template.setDescription((String) data.get("description"));
        template.setIconUrl((String) data.get("icon_url"));
        template.setTags((List<String>) data.get("tags"));
    }

    /**
     * 同步结果
     */
    public record SyncResult(int syncedCount, int addedCount, int updatedCount, Instant syncedAt) {}
}
```

- [ ] **Step 3: 编译验证**

```bash
./mvnw compile -pl gateway-boot -DskipTests
```

---

## Task 13: 添加同步端点和启动同步

**Files:**
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/adapter/admin/controller/ProviderTemplateController.java`

- [ ] **Step 1: 添加同步端点**

在 Controller 中添加：

```java
/**
 * 手动同步官方模板
 */
@PostMapping("/sync")
public ResponseEntity<OfficialTemplateSyncService.SyncResult> syncTemplates() {
    OfficialTemplateSyncService.SyncResult result = syncService.syncTemplates();
    return ResponseEntity.ok(result);
}
```

需要在 Controller 中注入 `OfficialTemplateSyncService`：

```java
private final OfficialTemplateSyncService syncService;
```

- [ ] **Step 2: 添加启动时同步**

创建 `TemplateSyncRunner.java`:

```java
package com.codingas.gateway.infrastructure.template.config;

import com.codingas.gateway.application.template.OfficialTemplateSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 启动时同步模板
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "template.git", name = "sync-on-startup", havingValue = "true", matchIfMissing = true)
public class TemplateSyncRunner implements ApplicationRunner {

    private final OfficialTemplateSyncService syncService;

    @Override
    public void run(ApplicationArguments args) {
        try {
            log.info("Starting official template sync on startup...");
            syncService.syncTemplates();
        } catch (Exception e) {
            log.warn("Failed to sync templates on startup, will use built-in templates", e);
        }
    }
}
```

- [ ] **Step 3: 编译验证**

```bash
./mvnw compile -pl gateway-boot -DskipTests
```

---

## Task 14: 创建内置模板

**Files:**
- Create: `gateway-boot/src/main/resources/templates/openai.json`
- Create: `gateway-boot/src/main/resources/templates/anthropic.json`
- Create: `gateway-boot/src/main/resources/templates/deepseek.json`

- [ ] **Step 1: 创建 openai.json**

```json
{
  "template_code": "openai",
  "template_name": "OpenAI",
  "provider_type": "OPENAI",
  "provider_config": {
    "provider_name": "OpenAI",
    "base_url": "https://api.openai.com",
    "website_url": "https://openai.com",
    "api_doc_url": "https://platform.openai.com/docs"
  },
  "models_config": [
    {
      "provider_model_id": "gpt-4o",
      "display_name": "GPT-4o",
      "context_window": 128000,
      "input_price": 2.5,
      "output_price": 10.0,
      "capabilities": {
        "vision": true,
        "function_calling": true,
        "streaming": true
      }
    },
    {
      "provider_model_id": "gpt-4-turbo",
      "display_name": "GPT-4 Turbo",
      "context_window": 128000,
      "input_price": 10.0,
      "output_price": 30.0,
      "capabilities": {
        "vision": true,
        "function_calling": true,
        "streaming": true
      }
    },
    {
      "provider_model_id": "gpt-3.5-turbo",
      "display_name": "GPT-3.5 Turbo",
      "context_window": 16385,
      "input_price": 0.5,
      "output_price": 1.5,
      "capabilities": {
        "vision": false,
        "function_calling": true,
        "streaming": true
      }
    }
  ],
  "description": "OpenAI 官方 API，支持 GPT-4o、GPT-4 Turbo、GPT-3.5 等模型",
  "icon_url": "https://cdn.example.com/icons/openai.png",
  "tags": ["国际", "多模态", "主流"]
}
```

- [ ] **Step 2: 创建 anthropic.json**

```json
{
  "template_code": "anthropic",
  "template_name": "Anthropic Claude",
  "provider_type": "ANTHROPIC",
  "provider_config": {
    "provider_name": "Anthropic",
    "base_url": "https://api.anthropic.com",
    "website_url": "https://anthropic.com",
    "api_doc_url": "https://docs.anthropic.com"
  },
  "models_config": [
    {
      "provider_model_id": "claude-sonnet-4-20250514",
      "display_name": "Claude Sonnet 4",
      "context_window": 200000,
      "input_price": 3.0,
      "output_price": 15.0,
      "capabilities": {
        "vision": true,
        "function_calling": true,
        "streaming": true
      }
    },
    {
      "provider_model_id": "claude-opus-4-20250514",
      "display_name": "Claude Opus 4",
      "context_window": 200000,
      "input_price": 15.0,
      "output_price": 75.0,
      "capabilities": {
        "vision": true,
        "function_calling": true,
        "streaming": true
      }
    },
    {
      "provider_model_id": "claude-3-5-haiku-20241022",
      "display_name": "Claude 3.5 Haiku",
      "context_window": 200000,
      "input_price": 0.8,
      "output_price": 4.0,
      "capabilities": {
        "vision": true,
        "function_calling": true,
        "streaming": true
      }
    }
  ],
  "description": "Anthropic Claude API，支持 Claude Sonnet 4、Claude Opus 4、Claude 3.5 Haiku 等模型",
  "icon_url": "https://cdn.example.com/icons/anthropic.png",
  "tags": ["国际", "多模态", "主流"]
}
```

- [ ] **Step 3: 创建 deepseek.json**

```json
{
  "template_code": "deepseek",
  "template_name": "DeepSeek",
  "provider_type": "DEEPSEEK",
  "provider_config": {
    "provider_name": "DeepSeek",
    "base_url": "https://api.deepseek.com",
    "website_url": "https://deepseek.com",
    "api_doc_url": "https://platform.deepseek.com/docs"
  },
  "models_config": [
    {
      "provider_model_id": "deepseek-chat",
      "display_name": "DeepSeek Chat",
      "context_window": 64000,
      "input_price": 0.14,
      "output_price": 0.28,
      "capabilities": {
        "vision": false,
        "function_calling": true,
        "streaming": true
      }
    },
    {
      "provider_model_id": "deepseek-coder",
      "display_name": "DeepSeek Coder",
      "context_window": 64000,
      "input_price": 0.14,
      "output_price": 0.28,
      "capabilities": {
        "vision": false,
        "function_calling": true,
        "streaming": true
      }
    }
  ],
  "description": "DeepSeek API，高性价比国产大模型，支持对话和代码生成",
  "icon_url": "https://cdn.example.com/icons/deepseek.png",
  "tags": ["国内", "高性价比", "代码"]
}
```

- [ ] **Step 4: 验证文件创建**

```bash
ls -la gateway-boot/src/main/resources/templates/
```

---

## Task 15: 集成测试

**Files:**
- Create: `gateway-boot/src/test/java/com/codingas/gateway/application/template/ProviderTemplateServiceIT.java`

- [ ] **Step 1: 编写集成测试**

```java
package com.codingas.gateway.application.template;

import com.codingas.gateway.application.template.dto.TemplateCreateRequest;
import com.codingas.gateway.application.template.dto.TemplateResponse;
import com.codingas.gateway.domain.template.entity.MarketStatus;
import com.codingas.gateway.domain.template.entity.TemplateType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ProviderTemplateService 集成测试
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProviderTemplateServiceIT {

    @Autowired
    private ProviderTemplateService service;

    @Test
    void createAndRetrieveTemplate() {
        // Arrange
        TemplateCreateRequest request = new TemplateCreateRequest();
        request.setTemplateCode("test-integration");
        request.setTemplateName("Test Integration");
        request.setProviderType("OTHER");
        request.setProviderConfig(Map.of("base_url", "https://api.test.com"));
        request.setModelsConfig(List.of(Map.of("provider_model_id", "model-1")));

        // Act
        TemplateResponse created = service.createTemplate(request, 1L, "testuser");
        TemplateResponse retrieved = service.getTemplate(created.getId());

        // Assert
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getTemplateCode()).isEqualTo("test-integration");
        assertThat(retrieved.getTemplateType()).isEqualTo(TemplateType.USER);
        assertThat(retrieved.getMarketStatus()).isEqualTo(MarketStatus.PRIVATE);
    }

    @Test
    void listTemplates() {
        // Arrange
        TemplateCreateRequest request = new TemplateCreateRequest();
        request.setTemplateCode("list-test");
        request.setTemplateName("List Test");
        request.setProviderType("OTHER");
        request.setProviderConfig(Map.of("base_url", "https://api.test.com"));
        request.setModelsConfig(List.of(Map.of("provider_model_id", "model-1")));

        service.createTemplate(request, 1L, "testuser");

        // Act
        Page<TemplateResponse> result = service.listTemplates(TemplateType.USER, null, null, null, 1, 10);

        // Assert
        assertThat(result.getContent()).isNotEmpty();
        assertThat(result.getContent().stream().anyMatch(t -> "list-test".equals(t.getTemplateCode()))).isTrue();
    }
}
```

- [ ] **Step 2: 运行集成测试**

```bash
./mvnw test -pl gateway-boot -Dtest=ProviderTemplateServiceIT
```

---

## Task 16: 运行完整测试并提交

- [ ] **Step 1: 运行所有测试**

```bash
./mvnw test -pl gateway-boot
```

- [ ] **Step 2: 提交代码**

```bash
git add gateway-boot/src/main/resources/db/migration/V7__create_provider_templates.sql
git add gateway-boot/src/main/java/com/codingas/gateway/domain/template/
git add gateway-boot/src/main/java/com/codingas/gateway/infrastructure/template/
git add gateway-boot/src/main/java/com/codingas/gateway/application/template/
git add gateway-boot/src/main/java/com/codingas/gateway/adapter/admin/controller/ProviderTemplateController.java
git add gateway-boot/src/main/resources/templates/
git add gateway-boot/src/test/java/com/codingas/gateway/application/template/
git add gateway-boot/src/test/java/com/codingas/gateway/infrastructure/template/
git add gateway-boot/pom.xml

git commit -m "feat(template): 添加 Provider 模板功能基础实现

- 新增 provider_templates 数据库表
- 创建 ProviderTemplate 实体、Gateway 接口及实现
- 实现模板 CRUD API（创建、查询、更新、删除）
- 添加 JGit 依赖支持 Git 同步
- 创建官方模板同步服务
- 添加内置模板（OpenAI、Anthropic、DeepSeek）
- 完成单元测试和集成测试"
```

---

## 后续任务（Phase 4-8）

剩余任务将在下一阶段实现：

- **Task 17-20**: 应用模板创建 Provider（读取现有 Provider/Channel/Model 结构）
- **Task 21-24**: 导入导出功能
- **Task 25-28**: 公共市场 + 发布审核
- **Task 29-32**: 前端页面
- **Task 33-35**: 测试 + 文档

这些任务需要先了解现有的 Provider、Channel、Model、ApiKey 相关代码。
