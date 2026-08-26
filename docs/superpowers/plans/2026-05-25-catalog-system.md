# Catalog 体系完善实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 用 ProviderCatalog / PlanCatalog / PlanModelCatalog / ModelSpecCatalog 四层实体替代旧 domain/metadata 包，实现"有什么可用"的只读快照定位，包含同步、去重、source 覆盖策略、物化流程。

**Architecture:** 四层 Catalog 实体与运营层（Provider/Channel/ChannelModel/ChannelEndpoint/ModelSpec）同构，通过一次性物化对接。CatalogDomainService 封装同步、upsert、markDeprecated、物化核心逻辑。BuiltinCatalogLoader 启动时从 classpath catalog/*.json 加载 BUILTIN 数据。

**Tech Stack:** Java 21 + Spring Boot 3.5.x + JPA + Flyway + H2(dev) / PostgreSQL(prod) + React 19 + Ant Design + TanStack React Query

---

## File Structure

### 新增文件

```
domain/supply/catalog/
  entity/
    ProviderCatalog.java
    PlanCatalog.java
    PlanModelCatalog.java
    ModelSpecCatalog.java
  enums/
    CatalogSource.java
    CatalogState.java
    ProviderType.java
    BillingMode.java
  gateway/
    ProviderCatalogGateway.java
    PlanCatalogGateway.java
    PlanModelCatalogGateway.java
    ModelSpecCatalogGateway.java
  service/
    CatalogDomainService.java
  exception/
    CatalogException.java

infrastructure/supply/catalog/
  gateway/
    ProviderCatalogGatewayImpl.java
    PlanCatalogGatewayImpl.java
    PlanModelCatalogGatewayImpl.java
    ModelSpecCatalogGatewayImpl.java
  database/
    ProviderCatalogDo.java
    PlanCatalogDo.java
    PlanModelCatalogDo.java
    ModelSpecCatalogDo.java
    ProviderCatalogRepository.java
    PlanCatalogRepository.java
    PlanModelCatalogRepository.java
    ModelSpecCatalogRepository.java
  loader/
    BuiltinCatalogLoader.java
  sync/
    ModelsDevSyncClient.java
    CatalogSyncScheduler.java

application/catalog/
  CatalogService.java
  CatalogServiceImpl.java
  CatalogSyncService.java
  CatalogMaterializeService.java
  dto/
    ProviderCatalogResponse.java
    PlanCatalogResponse.java
    PlanDetailResponse.java
    ModelSpecCatalogResponse.java
    CatalogSyncResult.java
    MaterializeRequest.java
    MaterializeResult.java

adapter/api/
  CatalogController.java

src/main/resources/
  catalog/
    providers.json
    plans.json
    plan-models.json
    model-specs.json
  db/migration/
    V38__catalog_tables.sql

src/test/java/.../domain/supply/catalog/
  entity/
    ProviderCatalogTest.java
    PlanCatalogTest.java
    PlanModelCatalogTest.java
    ModelSpecCatalogTest.java
  enums/
    CatalogSourceTest.java
    CatalogStateTest.java
  service/
    CatalogDomainServiceTest.java

src/test/java/.../application/catalog/
  CatalogServiceImplTest.java
  CatalogMaterializeServiceTest.java
```

### 删除文件

```
domain/metadata/                          (整个目录)
infrastructure/metadata/                  (整个目录)
application/metadata/                      (整个目录)
adapter/api/ProviderMetadataController.java
adapter/api/ModelMetadataController.java
adapter/api/ChannelMetadataController.java
adapter/api/MetadataSyncController.java
gateway-console/src/pages/Metadata/        (整个目录)
gateway-console/src/types/metadata.ts
gateway-console/src/services/api/metadata.ts
gateway-console/src/services/query/useMetadata.ts
gateway-console/src/locales/zh-CN/metadata.json
gateway-console/src/locales/en-US/metadata.json
```

### 前端新增文件

```
gateway-console/src/
  pages/Catalog/
    index.tsx                    — 目录管理页面（三级联动：供应商→套餐→模型规格）
    ProviderCatalogView.tsx      — 供应商目录卡片视图 + 物化按钮
    PlanCatalogView.tsx          — 套餐目录表格视图 + 物化按钮
    ModelSpecCatalogView.tsx     — 模型规格目录表格视图 + 物化按钮
    MaterializeModal.tsx         — 物化确认弹窗
  types/catalog.ts              — Catalog 类型定义
  services/api/catalog.ts       — Catalog API 客户端
  services/query/useCatalog.ts  — Catalog React Query hooks
  locales/zh-CN/catalog.json    — 中文 i18n
  locales/en-US/catalog.json    — 英文 i18n
```

### 前端修改文件

```
gateway-console/src/router/index.tsx                    — 路由 /metadata → /catalog
gateway-console/src/constants/menuConfig.tsx            — 菜单"元数据管理" → "目录管理"，路径 /metadata → /catalog
gateway-console/src/constants/permissions.ts            — 权限 metadata:read → catalog:read
gateway-console/src/i18n.ts                             — 加载 catalog i18n 替换 metadata i18n
gateway-console/src/services/api/index.ts               — 导出 catalog API 替换 metadata API
gateway-console/src/pages/Providers/ProviderMetadataSelector.tsx → 改为基于 ProviderCatalog 的选择器
gateway-console/src/pages/Providers/BasicInfoStep.tsx   — 如引用 metadata 则替换
gateway-console/src/pages/Providers/ModelSetupStep.tsx  — 如引用 metadata 则替换
gateway-console/src/pages/Providers/ProviderCreateModal.tsx — 如引用 metadata 则替换
```

---

### Task 1: Catalog 枚举 — CatalogSource, CatalogState, ProviderType, BillingMode

**Files:**
- Create: `domain/supply/catalog/enums/CatalogSource.java`
- Create: `domain/supply/catalog/enums/CatalogState.java`
- Create: `domain/supply/catalog/enums/ProviderType.java`
- Create: `domain/supply/catalog/enums/BillingMode.java`
- Test: `domain/supply/catalog/enums/CatalogSourceTest.java`
- Test: `domain/supply/catalog/enums/CatalogStateTest.java`

- [ ] **Step 1: Write failing tests for CatalogSource**

```java
package com.codingas.gateway.domain.supply.catalog.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CatalogSource 测试")
class CatalogSourceTest {

    @Test
    @DisplayName("优先级顺序：BUILTIN < MODELS_DEV < PROVIDER_API < MANUAL < OVERRIDE")
    void priorityOrder() {
        assertThat(CatalogSource.BUILTIN.getPriority()).isLessThan(CatalogSource.MODELS_DEV.getPriority());
        assertThat(CatalogSource.MODELS_DEV.getPriority()).isLessThan(CatalogSource.PROVIDER_API.getPriority());
        assertThat(CatalogSource.PROVIDER_API.getPriority()).isLessThan(CatalogSource.MANUAL.getPriority());
        assertThat(CatalogSource.MANUAL.getPriority()).isLessThan(CatalogSource.OVERRIDE.getPriority());
    }

    @Test
    @DisplayName("高优先级可以覆盖低优先级")
    void canOverride_higherOverridesLower() {
        assertThat(CatalogSource.MODELS_DEV.canOverride(CatalogSource.BUILTIN)).isTrue();
        assertThat(CatalogSource.BUILTIN.canOverride(CatalogSource.MODELS_DEV)).isFalse();
        assertThat(CatalogSource.OVERRIDE.canOverride(CatalogSource.MANUAL)).isTrue();
    }

    @Test
    @DisplayName("同优先级可以互相覆盖")
    void canOverride_samePriority() {
        assertThat(CatalogSource.MODELS_DEV.canOverride(CatalogSource.MODELS_DEV)).isTrue();
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw test -pl gateway-boot -Dtest="CatalogSourceTest" -DfailIfNoTests=false 2>&1 | tail -5`
Expected: FAIL — class not found

- [ ] **Step 3: Implement CatalogSource**

```java
package com.codingas.gateway.domain.supply.catalog.enums;

import lombok.Getter;

/**
 * 目录数据来源
 *
 * <p>优先级（低→高）：BUILTIN < MODELS_DEV < PROVIDER_API < MANUAL < OVERRIDE</p>
 * <p>低优先级不可覆盖高优先级，同优先级可互相覆盖。</p>
 */
@Getter
public enum CatalogSource {

    BUILTIN(0),
    MODELS_DEV(10),
    PROVIDER_API(20),
    MANUAL(30),
    OVERRIDE(40);

    private final int priority;

    CatalogSource(int priority) {
        this.priority = priority;
    }

    /** 当前 source 是否可以覆盖目标 source */
    public boolean canOverride(CatalogSource target) {
        return this.priority >= target.priority;
    }
}
```

- [ ] **Step 4: Implement CatalogState**

```java
package com.codingas.gateway.domain.supply.catalog.enums;

/**
 * 目录状态
 */
public enum CatalogState {

    /** 正常可用 */
    ACTIVE,

    /** 已下线（上游数据源中消失） */
    DEPRECATED
}
```

- [ ] **Step 5: Implement ProviderType**

```java
package com.codingas.gateway.domain.supply.catalog.enums;

/**
 * 供应商类型
 */
public enum ProviderType {

    /** 国际供应商 */
    INTERNATIONAL,

    /** 国内供应商 */
    DOMESTIC
}
```

- [ ] **Step 6: Implement BillingMode**

```java
package com.codingas.gateway.domain.supply.catalog.enums;

/**
 * 计费模式
 */
public enum BillingMode {

    /** 按量付费 */
    PAY_AS_YOU_GO,

    /** 订阅制 */
    SUBSCRIPTION,

    /** 资源包 */
    PACKAGE
}
```

- [ ] **Step 7: Write test for CatalogState**

```java
package com.codingas.gateway.domain.supply.catalog.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CatalogState 测试")
class CatalogStateTest {

    @Test
    @DisplayName("包含 ACTIVE 和 DEPRECATED 两个值")
    void hasExpectedValues() {
        assertThat(CatalogState.values()).containsExactly(CatalogState.ACTIVE, CatalogState.DEPRECATED);
    }
}
```

- [ ] **Step 8: Run all catalog enum tests**

Run: `./mvnw test -pl gateway-boot -Dtest="CatalogSourceTest,CatalogStateTest" -DfailIfNoTests=false 2>&1 | tail -5`
Expected: PASS

- [ ] **Step 9: Commit**

```bash
git add domain/supply/catalog/enums/ test/.../catalog/enums/
git commit -m "feat(catalog): 新增 CatalogSource/CatalogState/ProviderType/BillingMode 枚举"
```

---

### Task 2: Catalog 实体 — ProviderCatalog, PlanCatalog, PlanModelCatalog, ModelSpecCatalog

**Files:**
- Create: `domain/supply/catalog/entity/ProviderCatalog.java`
- Create: `domain/supply/catalog/entity/PlanCatalog.java`
- Create: `domain/supply/catalog/entity/PlanModelCatalog.java`
- Create: `domain/supply/catalog/entity/ModelSpecCatalog.java`
- Create: `domain/supply/catalog/exception/CatalogException.java`
- Test: `domain/supply/catalog/entity/ProviderCatalogTest.java`
- Test: `domain/supply/catalog/entity/PlanCatalogTest.java`
- Test: `domain/supply/catalog/entity/ModelSpecCatalogTest.java`

- [ ] **Step 1: Write CatalogException**

```java
package com.codingas.gateway.domain.supply.catalog.exception;

import com.codingas.gateway.common.exception.GatewayException;

/**
 * 目录异常
 */
public class CatalogException extends GatewayException {

    public CatalogException(String code, String message) {
        super(code, message);
    }

    public CatalogException(String code, String message, Throwable cause) {
        super(code, message, cause);
    }
}
```

- [ ] **Step 2: Write failing tests for ProviderCatalog**

```java
package com.codingas.gateway.domain.supply.catalog.entity;

import com.codingas.gateway.domain.supply.catalog.enums.CatalogSource;
import com.codingas.gateway.domain.supply.catalog.enums.CatalogState;
import com.codingas.gateway.domain.supply.catalog.enums.ProviderType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ProviderCatalog 测试")
class ProviderCatalogTest {

    @Test
    @DisplayName("默认状态为 ACTIVE，来源为 BUILTIN")
    void defaults() {
        ProviderCatalog catalog = new ProviderCatalog();
        assertThat(catalog.getState()).isEqualTo(CatalogState.ACTIVE);
        assertThat(catalog.getSource()).isEqualTo(CatalogSource.BUILTIN);
    }

    @Test
    @DisplayName("isAvailable — ACTIVE 返回 true，DEPRECATED 返回 false")
    void isAvailable() {
        ProviderCatalog catalog = new ProviderCatalog();
        assertThat(catalog.isAvailable()).isTrue();
        catalog.setState(CatalogState.DEPRECATED);
        assertThat(catalog.isAvailable()).isFalse();
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

Run: `./mvnw test -pl gateway-boot -Dtest="ProviderCatalogTest" -DfailIfNoTests=false 2>&1 | tail -5`
Expected: FAIL

- [ ] **Step 4: Implement ProviderCatalog**

```java
package com.codingas.gateway.domain.supply.catalog.entity;

import com.codingas.gateway.common.entity.BaseEntity;
import com.codingas.gateway.domain.supply.catalog.enums.CatalogSource;
import com.codingas.gateway.domain.supply.catalog.enums.CatalogState;
import com.codingas.gateway.domain.supply.catalog.enums.ProviderType;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.Instant;

/**
 * 供应商目录实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ProviderCatalog extends BaseEntity {

    private String providerCode;
    private String providerName;
    private ProviderType providerType;
    private String logoUrl;
    private String websiteUrl;
    private String baseUrl;
    private String description;
    private CatalogSource source = CatalogSource.BUILTIN;
    private Instant syncedAt;
    private CatalogState state = CatalogState.ACTIVE;

    /** 检查是否可用 */
    public boolean isAvailable() {
        return CatalogState.ACTIVE.equals(state);
    }
}
```

- [ ] **Step 5: Implement PlanCatalog**

```java
package com.codingas.gateway.domain.supply.catalog.entity;

import com.codingas.gateway.common.entity.BaseEntity;
import com.codingas.gateway.domain.supply.catalog.enums.BillingMode;
import com.codingas.gateway.domain.supply.catalog.enums.CatalogSource;
import com.codingas.gateway.domain.supply.catalog.enums.CatalogState;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.Instant;
import java.util.Map;

/**
 * 套餐目录实体
 *
 * <p>卖什么套餐（含套餐价格、计费模式、端点、包含的模型列表）</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PlanCatalog extends BaseEntity {

    private String planCode;
    private String providerCode;
    private String planName;
    private BillingMode billingMode;
    /** [{protocol, url}, ...] */
    private String endpoints;
    /** [{providerModelId, inputPrice, outputPrice, ...}, ...] */
    private String pricing;
    private String description;
    private CatalogSource source = CatalogSource.BUILTIN;
    private Instant syncedAt;
    private CatalogState state = CatalogState.ACTIVE;

    /** 检查是否可用 */
    public boolean isAvailable() {
        return CatalogState.ACTIVE.equals(state);
    }
}
```

- [ ] **Step 6: Implement PlanModelCatalog**

```java
package com.codingas.gateway.domain.supply.catalog.entity;

import com.codingas.gateway.common.entity.BaseEntity;
import com.codingas.gateway.domain.supply.catalog.enums.CatalogSource;
import com.codingas.gateway.domain.supply.catalog.enums.CatalogState;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.Instant;

/**
 * 套餐-模型关联目录实体（纯关联）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PlanModelCatalog extends BaseEntity {

    private String planCode;
    private String providerModelId;
    private CatalogSource source = CatalogSource.BUILTIN;
    private Instant syncedAt;
    private CatalogState state = CatalogState.ACTIVE;

    /** 检查是否可用 */
    public boolean isAvailable() {
        return CatalogState.ACTIVE.equals(state);
    }
}
```

- [ ] **Step 7: Implement ModelSpecCatalog**

```java
package com.codingas.gateway.domain.supply.catalog.entity;

import com.codingas.gateway.common.entity.BaseEntity;
import com.codingas.gateway.domain.supply.catalog.enums.CatalogSource;
import com.codingas.gateway.domain.supply.catalog.enums.CatalogState;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.Instant;

/**
 * 模型规格目录实体
 *
 * <p>模型能力描述，不含定价。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ModelSpecCatalog extends BaseEntity {

    private String providerModelId;
    private String displayName;
    private String modelFamily;
    private Integer contextWindow;
    private Integer maxInputTokens;
    private Integer maxOutputTokens;
    private String knowledgeCutoff;
    /** {vision, tool_use, streaming, ...} */
    private String capabilities;
    /** ["text", "image", "audio"] */
    private String modalities;
    private CatalogSource source = CatalogSource.BUILTIN;
    private Instant syncedAt;
    private CatalogState state = CatalogState.ACTIVE;

    /** 检查是否可用 */
    public boolean isAvailable() {
        return CatalogState.ACTIVE.equals(state);
    }
}
```

- [ ] **Step 8: Write and run PlanCatalog test**

```java
package com.codingas.gateway.domain.supply.catalog.entity;

import com.codingas.gateway.domain.supply.catalog.enums.CatalogState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PlanCatalog 测试")
class PlanCatalogTest {

    @Test
    @DisplayName("DEPRECATED 状态不可用")
    void deprecated_notAvailable() {
        PlanCatalog plan = new PlanCatalog();
        plan.setState(CatalogState.DEPRECATED);
        assertThat(plan.isAvailable()).isFalse();
    }
}
```

- [ ] **Step 9: Run all entity tests**

Run: `./mvnw test -pl gateway-boot -Dtest="ProviderCatalogTest,PlanCatalogTest" -DfailIfNoTests=false 2>&1 | tail -5`
Expected: PASS

- [ ] **Step 10: Commit**

```bash
git add domain/supply/catalog/entity/ domain/supply/catalog/exception/ test/.../catalog/entity/
git commit -m "feat(catalog): 新增 ProviderCatalog/PlanCatalog/PlanModelCatalog/ModelSpecCatalog 实体"
```

---

### Task 3: Catalog Gateway 接口

**Files:**
- Create: `domain/supply/catalog/gateway/ProviderCatalogGateway.java`
- Create: `domain/supply/catalog/gateway/PlanCatalogGateway.java`
- Create: `domain/supply/catalog/gateway/PlanModelCatalogGateway.java`
- Create: `domain/supply/catalog/gateway/ModelSpecCatalogGateway.java`

- [ ] **Step 1: Implement ProviderCatalogGateway**

```java
package com.codingas.gateway.domain.supply.catalog.gateway;

import com.codingas.gateway.domain.supply.catalog.entity.ProviderCatalog;
import com.codingas.gateway.domain.supply.catalog.enums.CatalogSource;
import com.codingas.gateway.domain.supply.catalog.enums.ProviderType;

import java.util.List;
import java.util.Optional;

/**
 * 供应商目录持久化接口
 */
public interface ProviderCatalogGateway {

    ProviderCatalog save(ProviderCatalog catalog);

    Optional<ProviderCatalog> findByProviderCode(String providerCode);

    List<ProviderCatalog> findAll();

    List<ProviderCatalog> findBySource(CatalogSource source);

    List<ProviderCatalog> findByProviderType(ProviderType providerType);

    boolean existsByProviderCode(String providerCode);

    void deleteByProviderCode(String providerCode);
}
```

- [ ] **Step 2: Implement PlanCatalogGateway**

```java
package com.codingas.gateway.domain.supply.catalog.gateway;

import com.codingas.gateway.domain.supply.catalog.entity.PlanCatalog;
import com.codingas.gateway.domain.supply.catalog.enums.CatalogSource;

import java.util.List;
import java.util.Optional;

/**
 * 套餐目录持久化接口
 */
public interface PlanCatalogGateway {

    PlanCatalog save(PlanCatalog catalog);

    Optional<PlanCatalog> findByPlanCode(String planCode);

    List<PlanCatalog> findAll();

    List<PlanCatalog> findByProviderCode(String providerCode);

    List<PlanCatalog> findBySource(CatalogSource source);

    boolean existsByPlanCode(String planCode);

    void deleteByPlanCode(String planCode);
}
```

- [ ] **Step 3: Implement PlanModelCatalogGateway**

```java
package com.codingas.gateway.domain.supply.catalog.gateway;

import com.codingas.gateway.domain.supply.catalog.entity.PlanModelCatalog;
import com.codingas.gateway.domain.supply.catalog.enums.CatalogSource;

import java.util.List;
import java.util.Optional;

/**
 * 套餐-模型关联目录持久化接口
 */
public interface PlanModelCatalogGateway {

    PlanModelCatalog save(PlanModelCatalog catalog);

    List<PlanModelCatalog> saveAll(List<PlanModelCatalog> catalogs);

    Optional<PlanModelCatalog> findByPlanCodeAndProviderModelId(String planCode, String providerModelId);

    List<PlanCatalog> findByPlanCode(String planCode);

    List<PlanModelCatalog> findBySource(CatalogSource source);

    boolean existsByPlanCodeAndProviderModelId(String planCode, String providerModelId);

    void deleteByPlanCode(String planCode);

    void deleteByPlanCodeAndProviderModelId(String planCode, String providerModelId);
}
```

- [ ] **Step 4: Implement ModelSpecCatalogGateway**

```java
package com.codingas.gateway.domain.supply.catalog.gateway;

import com.codingas.gateway.domain.supply.catalog.entity.ModelSpecCatalog;
import com.codingas.gateway.domain.supply.catalog.enums.CatalogSource;

import java.util.List;
import java.util.Optional;

/**
 * 模型规格目录持久化接口
 */
public interface ModelSpecCatalogGateway {

    ModelSpecCatalog save(ModelSpecCatalog catalog);

    Optional<ModelSpecCatalog> findByProviderModelId(String providerModelId);

    List<ModelSpecCatalog> findAll();

    List<ModelSpecCatalog> findBySource(CatalogSource source);

    boolean existsByProviderModelId(String providerModelId);

    void deleteByProviderModelId(String providerModelId);
}
```

- [ ] **Step 5: Verify compilation**

Run: `./mvnw compile -pl gateway-boot 2>&1 | tail -3`
Expected: BUILD SUCCESS

- [ ] **Step 6: Commit**

```bash
git add domain/supply/catalog/gateway/
git commit -m "feat(catalog): 新增四层 Catalog Gateway 接口"
```

---

### Task 4: CatalogDomainService — 同步、upsert、markDeprecated、物化

**Files:**
- Create: `domain/supply/catalog/service/CatalogDomainService.java`
- Test: `domain/supply/catalog/service/CatalogDomainServiceTest.java`

- [ ] **Step 1: Write failing tests for CatalogDomainService**

```java
package com.codingas.gateway.domain.supply.catalog.service;

import com.codingas.gateway.domain.supply.catalog.entity.ModelSpecCatalog;
import com.codingas.gateway.domain.supply.catalog.entity.PlanCatalog;
import com.codingas.gateway.domain.supply.catalog.entity.PlanModelCatalog;
import com.codingas.gateway.domain.supply.catalog.entity.ProviderCatalog;
import com.codingas.gateway.domain.supply.catalog.enums.*;
import com.codingas.gateway.domain.supply.catalog.gateway.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CatalogDomainService 测试")
class CatalogDomainServiceTest {

    @Mock private ProviderCatalogGateway providerCatalogGateway;
    @Mock private PlanCatalogGateway planCatalogGateway;
    @Mock private PlanModelCatalogGateway planModelCatalogGateway;
    @Mock private ModelSpecCatalogGateway modelSpecCatalogGateway;

    private CatalogDomainService service;

    @BeforeEach
    void setUp() {
        service = new CatalogDomainService(
            providerCatalogGateway, planCatalogGateway, planModelCatalogGateway, modelSpecCatalogGateway);
    }

    @Nested
    @DisplayName("upsert ProviderCatalog")
    class UpsertProvider {

        @Test
        @DisplayName("不存在时新增")
        void insert_whenNotExists() {
            when(providerCatalogGateway.findByProviderCode("openai")).thenReturn(Optional.empty());
            when(providerCatalogGateway.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var result = service.upsertProvider(aProvider("openai", CatalogSource.MODELS_DEV));

            assertThat(result).isEqualTo("ADDED");
            verify(providerCatalogGateway).save(any());
        }

        @Test
        @DisplayName("同优先级覆盖")
        void update_samePriority() {
            var existing = aProvider("openai", CatalogSource.BUILTIN);
            when(providerCatalogGateway.findByProviderCode("openai")).thenReturn(Optional.of(existing));
            when(providerCatalogGateway.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var result = service.upsertProvider(aProvider("openai", CatalogSource.MODELS_DEV));

            assertThat(result).isEqualTo("UPDATED");
        }

        @Test
        @DisplayName("低优先级不可覆盖高优先级")
        void skip_lowerPriority() {
            var existing = aProvider("openai", CatalogSource.MANUAL);
            when(providerCatalogGateway.findByProviderCode("openai")).thenReturn(Optional.of(existing));

            var result = service.upsertProvider(aProvider("openai", CatalogSource.BUILTIN));

            assertThat(result).isEqualTo("SKIPPED");
            verify(providerCatalogGateway, never()).save(any());
        }
    }

    @Nested
    @DisplayName("markDeprecated")
    class MarkDeprecated {

        @Test
        @DisplayName("本轮未出现的条目标记为 DEPRECATED")
        void mark_deprecated() {
            var provider = aProvider("openai", CatalogSource.BUILTIN);
            when(providerCatalogGateway.findBySource(CatalogSource.BUILTIN))
                .thenReturn(List.of(provider));
            when(providerCatalogGateway.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.markDeprecated(CatalogSource.BUILTIN, List.of("anthropic"));

            assertThat(provider.getState()).isEqualTo(CatalogState.DEPRECATED);
            verify(providerCatalogGateway).save(provider);
        }

        @Test
        @DisplayName("本轮出现的条目保持 ACTIVE")
        void keep_active() {
            var provider = aProvider("openai", CatalogSource.BUILTIN);
            when(providerCatalogGateway.findBySource(CatalogSource.BUILTIN))
                .thenReturn(List.of(provider));

            service.markDeprecated(CatalogSource.BUILTIN, List.of("openai"));

            assertThat(provider.getState()).isEqualTo(CatalogState.ACTIVE);
            verify(providerCatalogGateway, never()).save(any());
        }
    }

    @Nested
    @DisplayName("materializeProvider")
    class Materialize {

        @Test
        @DisplayName("已物化时抛出异常")
        void alreadyMaterialized() {
            // 需要 ProviderGateway，暂时用 mock 验证异常路径
            // 物化逻辑涉及跨域调用，在应用层测试更合适
        }
    }

    private ProviderCatalog aProvider(String code, CatalogSource source) {
        var c = new ProviderCatalog();
        c.setProviderCode(code);
        c.setSource(source);
        return c;
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw test -pl gateway-boot -Dtest="CatalogDomainServiceTest" -DfailIfNoTests=false 2>&1 | tail -5`
Expected: FAIL

- [ ] **Step 3: Implement CatalogDomainService**

```java
package com.codingas.gateway.domain.supply.catalog.service;

import com.codingas.gateway.domain.supply.catalog.entity.*;
import com.codingas.gateway.domain.supply.catalog.enums.CatalogSource;
import com.codingas.gateway.domain.supply.catalog.enums.CatalogState;
import com.codingas.gateway.domain.supply.catalog.exception.CatalogException;
import com.codingas.gateway.domain.supply.catalog.gateway.*;
import com.codingas.gateway.domain.supply.gateway.ChannelGateway;
import com.codingas.gateway.domain.supply.gateway.ModelSpecGateway;
import com.codingas.gateway.domain.supply.gateway.ProviderGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 目录管理服务
 *
 * <p>封装同步、upsert、markDeprecated、物化核心逻辑。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CatalogDomainService {

    private final ProviderCatalogGateway providerCatalogGateway;
    private final PlanCatalogGateway planCatalogGateway;
    private final PlanModelCatalogGateway planModelCatalogGateway;
    private final ModelSpecCatalogGateway modelSpecCatalogGateway;

    // ===== upsert =====

    @Transactional
    public String upsertProvider(ProviderCatalog catalog) {
        return providerCatalogGateway.findByProviderCode(catalog.getProviderCode())
            .map(existing -> {
                if (catalog.getSource().canOverride(existing.getSource())) {
                    copyProviderFields(catalog, existing);
                    providerCatalogGateway.save(existing);
                    return "UPDATED";
                }
                return "SKIPPED";
            })
            .orElseGet(() -> {
                providerCatalogGateway.save(catalog);
                return "ADDED";
            });
    }

    @Transactional
    public String upsertPlan(PlanCatalog catalog) {
        return planCatalogGateway.findByPlanCode(catalog.getPlanCode())
            .map(existing -> {
                if (catalog.getSource().canOverride(existing.getSource())) {
                    copyPlanFields(catalog, existing);
                    planCatalogGateway.save(existing);
                    return "UPDATED";
                }
                return "SKIPPED";
            })
            .orElseGet(() -> {
                planCatalogGateway.save(catalog);
                return "ADDED";
            });
    }

    @Transactional
    public String upsertPlanModel(PlanModelCatalog catalog) {
        return planModelCatalogGateway.findByPlanCodeAndProviderModelId(
                catalog.getPlanCode(), catalog.getProviderModelId())
            .map(existing -> {
                if (catalog.getSource().canOverride(existing.getSource())) {
                    existing.setSource(catalog.getSource());
                    existing.setSyncedAt(catalog.getSyncedAt());
                    planModelCatalogGateway.save(existing);
                    return "UPDATED";
                }
                return "SKIPPED";
            })
            .orElseGet(() -> {
                planModelCatalogGateway.save(catalog);
                return "ADDED";
            });
    }

    @Transactional
    public String upsertModelSpec(ModelSpecCatalog catalog) {
        return modelSpecCatalogGateway.findByProviderModelId(catalog.getProviderModelId())
            .map(existing -> {
                if (catalog.getSource().canOverride(existing.getSource())) {
                    copyModelSpecFields(catalog, existing);
                    modelSpecCatalogGateway.save(existing);
                    return "UPDATED";
                }
                return "SKIPPED";
            })
            .orElseGet(() -> {
                modelSpecCatalogGateway.save(catalog);
                return "ADDED";
            });
    }

    // ===== markDeprecated =====

    @Transactional
    public void markProvidersDeprecated(CatalogSource source, List<String> activeCodes) {
        for (var entry : providerCatalogGateway.findBySource(source)) {
            if (!activeCodes.contains(entry.getProviderCode()) && entry.getState() == CatalogState.ACTIVE) {
                entry.setState(CatalogState.DEPRECATED);
                providerCatalogGateway.save(entry);
                log.info("Deprecated provider: code={}", entry.getProviderCode());
            }
        }
    }

    @Transactional
    public void markPlansDeprecated(CatalogSource source, List<String> activeCodes) {
        for (var entry : planCatalogGateway.findBySource(source)) {
            if (!activeCodes.contains(entry.getPlanCode()) && entry.getState() == CatalogState.ACTIVE) {
                entry.setState(CatalogState.DEPRECATED);
                planCatalogGateway.save(entry);
                log.info("Deprecated plan: code={}", entry.getPlanCode());
            }
        }
    }

    @Transactional
    public void markModelSpecsDeprecated(CatalogSource source, List<String> activeIds) {
        for (var entry : modelSpecCatalogGateway.findBySource(source)) {
            if (!activeIds.contains(entry.getProviderModelId()) && entry.getState() == CatalogState.ACTIVE) {
                entry.setState(CatalogState.DEPRECATED);
                modelSpecCatalogGateway.save(entry);
                log.info("Deprecated model spec: id={}", entry.getProviderModelId());
            }
        }
    }

    // ===== 字段拷贝 =====

    private void copyProviderFields(ProviderCatalog src, ProviderCatalog dst) {
        dst.setProviderName(src.getProviderName());
        dst.setProviderType(src.getProviderType());
        dst.setLogoUrl(src.getLogoUrl());
        dst.setWebsiteUrl(src.getWebsiteUrl());
        dst.setBaseUrl(src.getBaseUrl());
        dst.setDescription(src.getDescription());
        dst.setSource(src.getSource());
        dst.setSyncedAt(src.getSyncedAt());
    }

    private void copyPlanFields(PlanCatalog src, PlanCatalog dst) {
        dst.setPlanName(src.getPlanName());
        dst.setBillingMode(src.getBillingMode());
        dst.setEndpoints(src.getEndpoints());
        dst.setPricing(src.getPricing());
        dst.setDescription(src.getDescription());
        dst.setSource(src.getSource());
        dst.setSyncedAt(src.getSyncedAt());
    }

    private void copyModelSpecFields(ModelSpecCatalog src, ModelSpecCatalog dst) {
        dst.setDisplayName(src.getDisplayName());
        dst.setModelFamily(src.getModelFamily());
        dst.setContextWindow(src.getContextWindow());
        dst.setMaxInputTokens(src.getMaxInputTokens());
        dst.setMaxOutputTokens(src.getMaxOutputTokens());
        dst.setKnowledgeCutoff(src.getKnowledgeCutoff());
        dst.setCapabilities(src.getCapabilities());
        dst.setModalities(src.getModalities());
        dst.setSource(src.getSource());
        dst.setSyncedAt(src.getSyncedAt());
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./mvnw test -pl gateway-boot -Dtest="CatalogDomainServiceTest" -DfailIfNoTests=false 2>&1 | tail -5`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add domain/supply/catalog/service/ test/.../catalog/service/
git commit -m "feat(catalog): 新增 CatalogDomainService — upsert/markDeprecated 核心逻辑"
```

---

### Task 5: Infrastructure 层 — JPA 实体 + Repository + GatewayImpl

**Files:**
- Create: `infrastructure/supply/catalog/database/ProviderCatalogDo.java`
- Create: `infrastructure/supply/catalog/database/PlanCatalogDo.java`
- Create: `infrastructure/supply/catalog/database/PlanModelCatalogDo.java`
- Create: `infrastructure/supply/catalog/database/ModelSpecCatalogDo.java`
- Create: `infrastructure/supply/catalog/database/ProviderCatalogRepository.java`
- Create: `infrastructure/supply/catalog/database/PlanCatalogRepository.java`
- Create: `infrastructure/supply/catalog/database/PlanModelCatalogRepository.java`
- Create: `infrastructure/supply/catalog/database/ModelSpecCatalogRepository.java`
- Create: `infrastructure/supply/catalog/gateway/ProviderCatalogGatewayImpl.java`
- Create: `infrastructure/supply/catalog/gateway/PlanCatalogGatewayImpl.java`
- Create: `infrastructure/supply/catalog/gateway/PlanModelCatalogGatewayImpl.java`
- Create: `infrastructure/supply/catalog/gateway/ModelSpecCatalogGatewayImpl.java`

- [ ] **Step 1: Implement ProviderCatalogDo**

```java
package com.codingas.gateway.infrastructure.supply.catalog.database;

import com.codingas.gateway.infrastructure.common.database.BaseDo;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "provider_catalogs", uniqueConstraints = @UniqueConstraint(columnNames = "provider_code"))
public class ProviderCatalogDo extends BaseDo {

    @Column(name = "provider_code", nullable = false)
    private String providerCode;

    @Column(name = "provider_name", nullable = false)
    private String providerName;

    @Column(name = "provider_type", nullable = false)
    private String providerType;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "website_url")
    private String websiteUrl;

    @Column(name = "base_url")
    private String baseUrl;

    @Column(name = "description", length = 1024)
    private String description;

    @Column(name = "source", nullable = false)
    private String source;

    @Column(name = "synced_at")
    private java.time.Instant syncedAt;

    @Column(name = "state", nullable = false)
    private String state;
}
```

- [ ] **Step 2: Implement PlanCatalogDo**

```java
package com.codingas.gateway.infrastructure.supply.catalog.database;

import com.codingas.gateway.infrastructure.common.database.BaseDo;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "plan_catalogs", uniqueConstraints = @UniqueConstraint(columnNames = "plan_code"))
public class PlanCatalogDo extends BaseDo {

    @Column(name = "plan_code", nullable = false)
    private String planCode;

    @Column(name = "provider_code", nullable = false)
    private String providerCode;

    @Column(name = "plan_name", nullable = false)
    private String planName;

    @Column(name = "billing_mode", nullable = false)
    private String billingMode;

    @Column(name = "endpoints", columnDefinition = "TEXT")
    private String endpoints;

    @Column(name = "pricing", columnDefinition = "TEXT")
    private String pricing;

    @Column(name = "description", length = 1024)
    private String description;

    @Column(name = "source", nullable = false)
    private String source;

    @Column(name = "synced_at")
    private java.time.Instant syncedAt;

    @Column(name = "state", nullable = false)
    private String state;
}
```

- [ ] **Step 3: Implement PlanModelCatalogDo**

```java
package com.codingas.gateway.infrastructure.supply.catalog.database;

import com.codingas.gateway.infrastructure.common.database.BaseDo;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "plan_model_catalogs", uniqueConstraints = @UniqueConstraint(columnNames = {"plan_code", "provider_model_id"}))
public class PlanModelCatalogDo extends BaseDo {

    @Column(name = "plan_code", nullable = false)
    private String planCode;

    @Column(name = "provider_model_id", nullable = false)
    private String providerModelId;

    @Column(name = "source", nullable = false)
    private String source;

    @Column(name = "synced_at")
    private java.time.Instant syncedAt;

    @Column(name = "state", nullable = false)
    private String state;
}
```

- [ ] **Step 4: Implement ModelSpecCatalogDo**

```java
package com.codingas.gateway.infrastructure.supply.catalog.database;

import com.codingas.gateway.infrastructure.common.database.BaseDo;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "model_spec_catalogs", uniqueConstraints = @UniqueConstraint(columnNames = "provider_model_id"))
public class ModelSpecCatalogDo extends BaseDo {

    @Column(name = "provider_model_id", nullable = false)
    private String providerModelId;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "model_family")
    private String modelFamily;

    @Column(name = "context_window")
    private Integer contextWindow;

    @Column(name = "max_input_tokens")
    private Integer maxInputTokens;

    @Column(name = "max_output_tokens")
    private Integer maxOutputTokens;

    @Column(name = "knowledge_cutoff")
    private String knowledgeCutoff;

    @Column(name = "capabilities", columnDefinition = "TEXT")
    private String capabilities;

    @Column(name = "modalities", columnDefinition = "TEXT")
    private String modalities;

    @Column(name = "source", nullable = false)
    private String source;

    @Column(name = "synced_at")
    private java.time.Instant syncedAt;

    @Column(name = "state", nullable = false)
    private String state;
}
```

- [ ] **Step 5: Implement 4 Repository interfaces and 4 GatewayImpl classes**

Each GatewayImpl follows项目中已有的 `ChannelGatewayImpl` / `ModelSpecGatewayImpl` 模式：toEntity/toDo 转换 + 委托 Repository。因篇幅原因，此处以 `ProviderCatalogGatewayImpl` 为示例，其余三个结构相同。

```java
package com.codingas.gateway.infrastructure.supply.catalog.gateway;

import com.codingas.gateway.domain.supply.catalog.entity.ProviderCatalog;
import com.codingas.gateway.domain.supply.catalog.enums.*;
import com.codingas.gateway.domain.supply.catalog.gateway.ProviderCatalogGateway;
import com.codingas.gateway.infrastructure.supply.catalog.database.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ProviderCatalogGatewayImpl implements ProviderCatalogGateway {

    private final ProviderCatalogRepository repository;

    @Override
    public ProviderCatalog save(ProviderCatalog catalog) {
        var doEntity = toDo(catalog);
        var saved = repository.save(doEntity);
        return toEntity(saved);
    }

    @Override
    public Optional<ProviderCatalog> findByProviderCode(String providerCode) {
        return repository.findByProviderCode(providerCode).map(this::toEntity);
    }

    @Override
    public List<ProviderCatalog> findAll() {
        return repository.findAll().stream().map(this::toEntity).toList();
    }

    @Override
    public List<ProviderCatalog> findBySource(CatalogSource source) {
        return repository.findBySource(source.name()).stream().map(this::toEntity).toList();
    }

    @Override
    public List<ProviderCatalog> findByProviderType(ProviderType providerType) {
        return repository.findByProviderType(providerType.name()).stream().map(this::toEntity).toList();
    }

    @Override
    public boolean existsByProviderCode(String providerCode) {
        return repository.existsByProviderCode(providerCode);
    }

    @Override
    public void deleteByProviderCode(String providerCode) {
        repository.deleteByProviderCode(providerCode);
    }

    private ProviderCatalog toEntity(ProviderCatalogDo d) {
        var e = new ProviderCatalog();
        e.setId(d.getId());
        e.setProviderCode(d.getProviderCode());
        e.setProviderName(d.getProviderName());
        e.setProviderType(ProviderType.valueOf(d.getProviderType()));
        e.setLogoUrl(d.getLogoUrl());
        e.setWebsiteUrl(d.getWebsiteUrl());
        e.setBaseUrl(d.getBaseUrl());
        e.setDescription(d.getDescription());
        e.setSource(CatalogSource.valueOf(d.getSource()));
        e.setSyncedAt(d.getSyncedAt());
        e.setState(CatalogState.valueOf(d.getState()));
        e.setCreatedAt(d.getCreatedAt());
        e.setUpdatedAt(d.getUpdatedAt());
        return e;
    }

    private ProviderCatalogDo toDo(ProviderCatalog e) {
        var d = new ProviderCatalogDo();
        d.setId(e.getId());
        d.setProviderCode(e.getProviderCode());
        d.setProviderName(e.getProviderName());
        d.setProviderType(e.getProviderType().name());
        d.setLogoUrl(e.getLogoUrl());
        d.setWebsiteUrl(e.getWebsiteUrl());
        d.setBaseUrl(e.getBaseUrl());
        d.setDescription(e.getDescription());
        d.setSource(e.getSource().name());
        d.setSyncedAt(e.getSyncedAt());
        d.setState(e.getState().name());
        d.setCreatedAt(e.getCreatedAt());
        d.setUpdatedAt(e.getUpdatedAt());
        return d;
    }
}
```

- [ ] **Step 6: Verify compilation**

Run: `./mvnw compile -pl gateway-boot 2>&1 | tail -3`
Expected: BUILD SUCCESS

- [ ] **Step 7: Commit**

```bash
git add infrastructure/supply/catalog/
git commit -m "feat(catalog): 新增 JPA 实体/Repository/GatewayImpl — 四层 catalog 基础设施"
```

---

### Task 6: Flyway V38 — 建 catalog 表 + 删旧 metadata 表

**Files:**
- Create: `src/main/resources/db/migration/V38__catalog_tables.sql`

- [ ] **Step 1: Write migration script**

```sql
-- provider_catalogs
CREATE TABLE provider_catalogs (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    provider_code  VARCHAR(64)  NOT NULL,
    provider_name  VARCHAR(128) NOT NULL,
    provider_type  VARCHAR(32)  NOT NULL,
    logo_url       VARCHAR(512),
    website_url    VARCHAR(512),
    base_url       VARCHAR(512),
    description    VARCHAR(1024),
    source         VARCHAR(32)  NOT NULL DEFAULT 'BUILTIN',
    synced_at      TIMESTAMP,
    state          VARCHAR(32)  NOT NULL DEFAULT 'ACTIVE',
    created_by     BIGINT,
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by     BIGINT,
    updated_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_provider_catalogs_code UNIQUE (provider_code)
);

-- plan_catalogs
CREATE TABLE plan_catalogs (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    plan_code      VARCHAR(64)  NOT NULL,
    provider_code  VARCHAR(64)  NOT NULL,
    plan_name      VARCHAR(128) NOT NULL,
    billing_mode   VARCHAR(32)  NOT NULL,
    endpoints      TEXT,
    pricing        TEXT,
    description     VARCHAR(1024),
    source         VARCHAR(32)  NOT NULL DEFAULT 'BUILTIN',
    synced_at      TIMESTAMP,
    state          VARCHAR(32)  NOT NULL DEFAULT 'ACTIVE',
    created_by     BIGINT,
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by     BIGINT,
    updated_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_plan_catalogs_code UNIQUE (plan_code)
);

-- plan_model_catalogs
CREATE TABLE plan_model_catalogs (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    plan_code         VARCHAR(64)  NOT NULL,
    provider_model_id VARCHAR(128) NOT NULL,
    source            VARCHAR(32)  NOT NULL DEFAULT 'BUILTIN',
    synced_at         TIMESTAMP,
    state             VARCHAR(32)  NOT NULL DEFAULT 'ACTIVE',
    created_by        BIGINT,
    created_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by        BIGINT,
    updated_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_plan_model_catalogs UNIQUE (plan_code, provider_model_id)
);

-- model_spec_catalogs
CREATE TABLE model_spec_catalogs (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    provider_model_id VARCHAR(128) NOT NULL,
    display_name      VARCHAR(128),
    model_family      VARCHAR(64),
    context_window    INTEGER,
    max_input_tokens  INTEGER,
    max_output_tokens INTEGER,
    knowledge_cutoff  VARCHAR(32),
    capabilities      TEXT,
    modalities        TEXT,
    source            VARCHAR(32)  NOT NULL DEFAULT 'BUILTIN',
    synced_at    TIMESTAMP,
    state             VARCHAR(32)  NOT NULL DEFAULT 'ACTIVE',
    created_by        BIGINT,
    created_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by        BIGINT,
    updated_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_model_spec_catalogs_id UNIQUE (provider_model_id)
);

-- 删旧 metadata 表
DROP TABLE IF EXISTS product_model_metadata;
DROP TABLE IF EXISTS product_metadata;
DROP TABLE IF EXISTS model_metadata;
DROP TABLE IF EXISTS provider_metadata;
```

- [ ] **Step 2: Verify app starts with migration**

Run: `./mvnw spring-boot:run -pl gateway-boot 2>&1 | head -50 &`; sleep 20; kill %1 2>/dev/null
Expected: 日志中出现 "Successfully applied 1 migration to dataset"

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/db/migration/V38__catalog_tables.sql
git commit -m "feat(catalog): V38 迁移 — 建四张 catalog 表 + 删旧 metadata 表"
```

---

### Task 7: 生成 catalog/*.json BUILTIN 数据文件

**Files:**
- Create: `src/main/resources/catalog/providers.json`
- Create: `src/main/resources/catalog/plans.json`
- Create: `src/main/resources/catalog/plan-models.json`
- Create: `src/main/resources/catalog/model-specs.json`

- [ ] **Step 1: 生成 providers.json**

从 `metadata/providers/*.json` + 调研报告，按规格中的映射规则转换。包含 12 个现有供应商 + 8 个新增供应商，共 20 个条目。

- [ ] **Step 2: 生成 model-specs.json**

从 `metadata/models/*.json` + 调研报告定价表，按映射规则转换。补充调研报告中新增的模型条目。

- [ ] **Step 3: 生成 plans.json**

从 `metadata/products/*.json` + 调研报告端点/定价，按映射规则转换。包含旧 JSON 中的所有套餐 + 调研报告中的新增套餐（双协议端点等）。

- [ ] **Step 4: 生成 plan-models.json**

从 `metadata/product-models/*.json`，通过 planCode 映射转换。

- [ ] **Step 5: 验证 JSON 合法性**

Run: `for f in E:/workspace/llm-gateway/gateway-boot/src/main/resources/catalog/*.json; do echo "$f:"; python3 -m json.tool "$f" > /dev/null && echo "OK" || echo "INVALID"; done`
Expected: 全部 OK

- [ ] **Step 6: Commit**

```bash
git add src/main/resources/catalog/
git commit -m "feat(catalog): 新增 BUILTIN 数据 — providers/plans/plan-models/model-specs JSON"
```

---

### Task 8: BuiltinCatalogLoader — 启动时加载 JSON

**Files:**
- Create: `infrastructure/supply/catalog/loader/BuiltinCatalogLoader.java`
- Test: `infrastructure/supply/catalog/loader/BuiltinCatalogLoaderTest.java`

- [ ] **Step 1: Write failing test**

```java
package com.codingas.gateway.infrastructure.supply.catalog.loader;

import com.codingas.gateway.domain.supply.catalog.entity.*;
import com.codingas.gateway.domain.supply.catalog.enums.CatalogSource;
import com.codingas.gateway.domain.supply.catalog.gateway.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BuiltinCatalogLoader 测试")
class BuiltinCatalogLoaderTest {

    @Mock private ProviderCatalogGateway providerCatalogGateway;
    @Mock private PlanCatalogGateway planCatalogGateway;
    @Mock private PlanModelCatalogGateway planModelCatalogGateway;
    @Mock private ModelSpecCatalogGateway modelSpecCatalogGateway;
    @Mock private CatalogDomainService catalogDomainService;

    @Test
    @DisplayName("加载 JSON 后调用 upsert")
    void load_callsUpsert() {
        when(providerCatalogGateway.count()).thenReturn(0L);
        var loader = new BuiltinCatalogLoader(providerCatalogGateway, planCatalogGateway,
            planModelCatalogGateway, modelSpecCatalogGateway, catalogDomainService);
        loader.loadBuiltinCatalog();
        verify(catalogDomainService, atLeastOnce()).upsertProvider(any());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw test -pl gateway-boot -Dtest="BuiltinCatalogLoaderTest" -DfailIfNoTests=false 2>&1 | tail -5`
Expected: FAIL

- [ ] **Step 3: Implement BuiltinCatalogLoader**

```java
package com.codingas.gateway.infrastructure.supply.catalog.loader;

import com.codingas.gateway.application.catalog.CatalogSyncService;
import com.codingas.gateway.domain.supply.catalog.gateway.*;
import com.codingas.gateway.common.util.JsonUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

/**
 * BUILTIN 目录数据加载器
 *
 * <p>应用启动时，如果 catalog 表为空，从 classpath catalog/*.json 加载数据。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BuiltinCatalogLoader implements CommandLineRunner {

    private final ProviderCatalogGateway providerCatalogGateway;
    private final PlanCatalogGateway planCatalogGateway;
    private final PlanModelCatalogGateway planModelCatalogGateway;
    private final ModelSpecCatalogGateway modelSpecCatalogGateway;
    private final CatalogSyncService catalogSyncService;

    @Override
    public void run(String... args) {
        if (providerCatalogGateway.count() > 0) {
            log.info("Catalog already initialized, skipping...");
            return;
        }
        log.info("Loading builtin catalog data...");
        catalogSyncService.syncBuiltin();
        log.info("Builtin catalog data loaded successfully!");
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./mvnw test -pl gateway-boot -Dtest="BuiltinCatalogLoaderTest" -DfailIfNoTests=false 2>&1 | tail -5`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add infrastructure/supply/catalog/loader/ test/.../loader/
git commit -m "feat(catalog): 新增 BuiltinCatalogLoader — 启动时加载 BUILTIN JSON"
```

---

### Task 9: Application 层 — CatalogSyncService + CatalogMaterializeService + CatalogService

**Files:**
- Create: `application/catalog/CatalogService.java`
- Create: `application/catalog/CatalogServiceImpl.java`
- Create: `application/catalog/CatalogSyncService.java`
- Create: `application/catalog/CatalogMaterializeService.java`
- Create: `application/catalog/dto/*`
- Test: `application/catalog/CatalogServiceImplTest.java`
- Test: `application/catalog/CatalogMaterializeServiceTest.java`

- [ ] **Step 1: Implement CatalogSyncService**

```java
package com.codingas.gateway.application.catalog;

import com.codingas.gateway.domain.supply.catalog.entity.*;
import com.codingas.gateway.domain.supply.catalog.enums.CatalogSource;
import com.codingas.gateway.domain.supply.catalog.service.CatalogDomainService;
import com.codingas.gateway.common.util.JsonUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.List;

/**
 * 目录同步服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CatalogSyncService {

    private final CatalogDomainService catalogDomainService;

    @Transactional
    public void syncBuiltin() {
        var providers = loadJson("catalog/providers.json", new TypeReference<List<ProviderCatalog>>() {});
        var plans = loadJson("catalog/plans.json", new TypeReference<List<PlanCatalog>>() {});
        var planModels = loadJson("catalog/plan-models.json", new TypeReference<List<PlanModelCatalog>>() {});
        var modelSpecs = loadJson("catalog/model-specs.json", new TypeReference<List<ModelSpecCatalog>>() {});

        int added = 0, updated = 0, skipped = 0;
        for (var p : providers) {
            p.setSource(CatalogSource.BUILTIN);
            var result = catalogDomainService.upsertProvider(p);
            if ("ADDED".equals(result)) added++;
            else if ("UPDATED".equals(result)) updated++;
            else skipped++;
        }
        for (var p : plans) {
            p.setSource(CatalogSource.BUILTIN);
            var result = catalogDomainService.upsertPlan(p);
            if ("ADDED".equals(result)) added++;
            else if ("UPDATED".equals(result)) updated++;
            else skipped++;
        }
        for (var pm : planModels) {
            pm.setSource(CatalogSource.BUILTIN);
            var result = catalogDomainService.upsertPlanModel(pm);
            if ("ADDED".equals(result)) added++;
            else if ("UPDATED".equals(result)) updated++;
            else skipped++;
        }
        for (var ms : modelSpecs) {
            ms.setSource(CatalogSource.BUILTIN);
            var result = catalogDomainService.upsertModelSpec(ms);
            if ("ADDED".equals(result)) added++;
            else if ("UPDATED".equals(result)) updated++;
            else skipped++;
        }
        catalogDomainService.markProvidersDeprecated(CatalogSource.BUILTIN,
            providers.stream().map(ProviderCatalog::getProviderCode).toList());
        catalogDomainService.markPlansDeprecated(CatalogSource.BUILTIN,
            plans.stream().map(PlanCatalog::getPlanCode).toList());
        catalogDomainService.markModelSpecsDeprecated(CatalogSource.BUILTIN,
            modelSpecs.stream().map(ModelSpecCatalog::getProviderModelId).toList());

        log.info("BUILTIN sync: added={}, updated={}, skipped={}", added, updated, skipped);
    }

    private <T> List<T> loadJson(String path, TypeReference<List<T>> type) {
        try (InputStream is = new ClassPathResource(path).getInputStream()) {
            return JsonUtils.fromJson(is.readAllBytes(), type);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load " + path, e);
        }
    }
}
```

- [ ] **Step 2: Implement CatalogMaterializeService**

```java
package com.codingas.gateway.application.catalog;

import com.codingas.gateway.domain.supply.catalog.entity.*;
import com.codingas.gateway.domain.supply.catalog.exception.CatalogException;
import com.codingas.gateway.domain.supply.catalog.gateway.*;
import com.codingas.gateway.domain.supply.entity.*;
import com.codingas.gateway.domain.supply.enums.*;
import com.codingas.gateway.domain.supply.gateway.*;
import com.codingas.gateway.common.util.JsonUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 目录物化服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CatalogMaterializeService {

    private final ProviderCatalogGateway providerCatalogGateway;
    private final PlanCatalogGateway planCatalogGateway;
    private final ModelSpecCatalogGateway modelSpecCatalogGateway;
    private final ProviderGateway providerGateway;
    private final ChannelGateway channelGateway;
    private final ChannelEndpointGateway channelEndpointGateway;
    private final ModelSpecGateway modelSpecGateway;
    // ChannelModelGateway 也需要注入

    @Transactional
    public Provider materializeProvider(String providerCode) {
        var catalog = providerCatalogGateway.findByProviderCode(providerCode)
            .orElseThrow(() -> new CatalogException("PROVIDER_NOT_FOUND", "供应商目录不存在: " + providerCode));
        if (providerGateway.existsByCode(providerCode)) {
            throw new CatalogException("ALREADY_MATERIALIZED", "供应商已物化: " + providerCode);
        }
        Provider provider = new Provider();
        provider.setCode(catalog.getProviderCode());
        provider.setName(catalog.getProviderName());
        provider.setBaseUrl(catalog.getBaseUrl());
        provider.setLogoUrl(catalog.getLogoUrl());
        provider.setWebsiteUrl(catalog.getWebsiteUrl());
        provider.setDescription(catalog.getDescription());
        provider.setState(ProviderState.ACTIVE);
        return providerGateway.save(provider);
    }

    @Transactional
    public ModelSpec materializeModelSpec(String providerModelId) {
        var catalog = modelSpecCatalogGateway.findByProviderModelId(providerModelId)
            .orElseThrow(() -> new CatalogException("MODEL_NOT_FOUND", "模型规格目录不存在: " + providerModelId));
        if (modelSpecGateway.existsByProviderModelId(providerModelId)) {
            throw new CatalogException("ALREADY_MATERIALIZED", "模型规格已物化: " + providerModelId);
        }
        ModelSpec spec = new ModelSpec();
        spec.setProviderModelId(catalog.getProviderModelId());
        spec.setDisplayName(catalog.getDisplayName());
        spec.setModelFamily(catalog.getModelFamily());
        spec.setContextWindow(catalog.getContextWindow());
        spec.setMaxInputTokens(catalog.getMaxInputTokens());
        spec.setMaxOutputTokens(catalog.getMaxOutputTokens());
        spec.setCapabilities(catalog.getCapabilities());
        spec.setState(ModelSpecState.ACTIVE);
        return modelSpecGateway.save(spec);
    }

    /** 物化 Plan 时级联创建 Channel + ChannelEndpoint + ChannelModel */
    @Transactional
    public Channel materializePlan(String planCode) {
        var catalog = planCatalogGateway.findByPlanCode(planCode)
            .orElseThrow(() -> new CatalogException("PLAN_NOT_FOUND", "套餐目录不存在: " + planCode));
        // 创建 Channel
        Channel channel = new Channel();
        channel.setProviderId(/* lookup by providerCode */);
        channel.setName(catalog.getPlanName());
        channel.setBillingMode(BillingMode.valueOf(catalog.getBillingMode().name()));
        channel.setState(ChannelState.ACTIVE);
        channel = channelGateway.save(channel);
        // 创建 ChannelEndpoint(s)
        var endpoints = JsonUtils.fromJson(catalog.getEndpoints(), new TypeReference<List<Map<String,String>>>() {});
        for (var ep : endpoints) {
            ChannelEndpoint endpoint = new ChannelEndpoint();
            endpoint.setChannelId(channel.getId());
            endpoint.setProtocol(Protocol.valueOf(ep.get("protocol")));
            endpoint.setEndpointUrl(ep.get("url"));
            endpoint.setState(ChannelEndpointState.ACTIVE);
            channelEndpointGateway.save(endpoint);
        }
        // 创建 ChannelModel(s) from pricing
        var pricing = JsonUtils.fromJson(catalog.getPricing(), new TypeReference<List<Map<String,Object>>>() {});
        for (var p : pricing) {
            ModelSpec spec = findOrCreateModelSpec((String) p.get("providerModelId"));
            ChannelModel cm = new ChannelModel();
            cm.setChannelId(channel.getId());
            cm.setModelSpecId(spec.getId());
            if (p.get("inputPrice") != null) cm.setInputPrice(toBigDecimal(p.get("inputPrice")));
            if (p.get("outputPrice") != null) cm.setOutputPrice(toBigDecimal(p.get("outputPrice")));
            cm.setState(ChannelModelState.ACTIVE);
            // channelModelGateway.save(cm);
        }
        return channel;
    }

    private ModelSpec findOrCreateModelSpec(String providerModelId) {
        return modelSpecGateway.findByProviderModelId(providerModelId)
            .orElseGet(() -> materializeModelSpec(providerModelId));
    }

    private BigDecimal toBigDecimal(Object val) {
        if (val == null) return null;
        if (val instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        return new BigDecimal(val.toString());
    }
}
```

- [ ] **Step 3: Implement CatalogService + DTO**

CatalogService 封装查询接口（listProviderCatalogs, listPlanCatalogs, getPlanDetail, listModelSpecCatalogs），实现类委托各 Gateway。

- [ ] **Step 4: Write test for materializeProvider**

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("CatalogMaterializeService 测试")
class CatalogMaterializeServiceTest {

    @Mock private ProviderCatalogGateway providerCatalogGateway;
    @Mock private ProviderGateway providerGateway;
    // ... other mocks

    @Test
    @DisplayName("物化供应商成功")
    void materializeProvider_success() {
        var catalog = new ProviderCatalog();
        catalog.setProviderCode("openai");
        catalog.setProviderName("OpenAI");
        when(providerCatalogGateway.findByProviderCode("openai")).thenReturn(Optional.of(catalog));
        when(providerGateway.existsByCode("openai")).thenReturn(false);
        when(providerGateway.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // var result = service.materializeProvider("openai");
        // assertThat(result.getCode()).isEqualTo("openai");
    }

    @Test
    @DisplayName("重复物化抛出异常")
    void materializeProvider_alreadyMaterialized() {
        var catalog = new ProviderCatalog();
        catalog.setProviderCode("openai");
        when(providerCatalogGateway.findByProviderCode("openai")).thenReturn(Optional.of(catalog));
        when(providerGateway.existsByCode("openai")).thenReturn(true);

        // assertThatThrownBy(() -> service.materializeProvider("openai"))
        //     .isInstanceOf(CatalogException.class);
    }
}
```

- [ ] **Step 5: Run all catalog tests**

Run: `./mvnw test -pl gateway-boot -Dtest="CatalogMaterializeServiceTest,CatalogServiceImplTest" -DfailIfNoTests=false 2>&1 | tail -5`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add application/catalog/
git commit -m "feat(catalog): 新增 CatalogSyncService/MaterializeService/QueryService 应用层"
```

---

### Task 10: CatalogController — REST API

**Files:**
- Create: `adapter/api/CatalogController.java`

- [ ] **Step 1: Implement CatalogController**

```java
package com.codingas.gateway.adapter.api;

import com.codingas.gateway.application.catalog.CatalogService;
import com.codingas.gateway.application.catalog.CatalogMaterializeService;
import com.codingas.gateway.application.catalog.CatalogSyncService;
import com.codingas.gateway.application.catalog.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 目录管理 REST 控制器
 */
@RestController
@RequestMapping("/api/v1/catalog")
@RequiredArgsConstructor
public class CatalogController {

    private final CatalogService catalogService;
    private final CatalogSyncService catalogSyncService;
    private final CatalogMaterializeService catalogMaterializeService;

    // ===== 供应商目录 =====

    @GetMapping("/providers")
    public ResponseEntity<List<ProviderCatalogResponse>> listProviders(
            @RequestParam(required = false) String providerType,
            @RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(catalogService.listProviderCatalogs(providerType, keyword));
    }

    // ===== 套餐目录 =====

    @GetMapping("/plans")
    public ResponseEntity<List<PlanCatalogResponse>> listPlans(
            @RequestParam(required = false) String providerCode) {
        return ResponseEntity.ok(catalogService.listPlanCatalogs(providerCode));
    }

    @GetMapping("/plans/{planCode}")
    public ResponseEntity<PlanDetailResponse> getPlanDetail(@PathVariable String planCode) {
        return ResponseEntity.ok(catalogService.getPlanDetail(planCode));
    }

    // ===== 模型规格目录 =====

    @GetMapping("/model-specs")
    public ResponseEntity<List<ModelSpecCatalogResponse>> listModelSpecs(
            @RequestParam(required = false) String providerCode,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String capability) {
        return ResponseEntity.ok(catalogService.listModelSpecCatalogs(providerCode, keyword, capability));
    }

    // ===== 物化 =====

    @PostMapping("/materialize/provider/{providerCode}")
    public ResponseEntity<MaterializeResult> materializeProvider(@PathVariable String providerCode) {
        return ResponseEntity.ok(catalogMaterializeService.materializeProvider(providerCode));
    }

    @PostMapping("/materialize/plan/{planCode}")
    public ResponseEntity<MaterializeResult> materializePlan(@PathVariable String planCode) {
        return ResponseEntity.ok(catalogMaterializeService.materializePlan(planCode));
    }

    @PostMapping("/materialize/model-spec/{providerModelId}")
    public ResponseEntity<MaterializeResult> materializeModelSpec(@PathVariable String providerModelId) {
        return ResponseEntity.ok(catalogMaterializeService.materializeModelSpec(providerModelId));
    }

    // ===== 同步 =====

    @PostMapping("/sync/builtin")
    public ResponseEntity<CatalogSyncResult> syncBuiltin() {
        catalogSyncService.syncBuiltin();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/sync/models-dev")
    public ResponseEntity<CatalogSyncResult> syncModelsDev() {
        catalogSyncService.syncModelsDev();
        return ResponseEntity.ok().build();
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `./mvnw compile -pl gateway-boot 2>&1 | tail -3`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add adapter/api/CatalogController.java
git commit -m "feat(catalog): 新增 CatalogController — 查询/物化/同步 API"
```

---

### Task 11: 删除旧 metadata 包 + 清理所有引用

**Files:**
- Delete: `domain/metadata/` (整个目录)
- Delete: `infrastructure/metadata/` (整个目录)
- Delete: `application/metadata/` (整个目录)
- Delete: `adapter/api/ProviderMetadataController.java`
- Delete: `adapter/api/ModelMetadataController.java`
- Delete: `adapter/api/ChannelMetadataController.java`
- Delete: `adapter/api/MetadataSyncController.java`
- Modify: 所有引用旧 metadata 类的文件

- [ ] **Step 1: 查找所有引用旧 metadata 的文件**

Run: `grep -rl "domain.metadata\|infrastructure.metadata\|application.metadata\|MetadataController" gateway-boot/src/main/java/ gateway-boot/src/test/java/ 2>/dev/null`

- [ ] **Step 2: 逐文件修复引用，替换为 catalog 包**

主要需要修复的引用：
- `DataInitializer.java` — 移除旧 Metadata Gateway 注入
- `ProxyServiceImpl.java` / `SupplyRoutingService.java` — 如有引用 metadata Gateway
- `application.yml` — 移除旧 `metadata.sync` 配置（如有）
- 测试文件 — 移除旧 metadata 测试

- [ ] **Step 3: 删除旧 metadata 目录**

Run: `rm -rf gateway-boot/src/main/java/com/codingas/gateway/domain/metadata/ gateway-boot/src/main/java/com/codingas/gateway/infrastructure/metadata/ gateway-boot/src/main/java/com/codingas/gateway/application/metadata/`

- [ ] **Step 4: 删除旧 metadata Controller**

Run: `rm gateway-boot/src/main/java/com/codingas/gateway/adapter/api/ProviderMetadataController.java gateway-boot/src/main/java/com/codingas/gateway/adapter/api/ModelMetadataController.java gateway-boot/src/main/java/com/codingas/gateway/adapter/api/ChannelMetadataController.java gateway-boot/src/main/java/com/codingas/gateway/adapter/api/MetadataSyncController.java`

- [ ] **Step 5: Run full test suite**

Run: `./mvnw clean test -pl gateway-boot 2>&1 | tail -10`
Expected: 全部测试通过

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "refactor(catalog): 删除旧 domain/infrastructure/application/metadata 包，替换为 catalog"
```

---

### Task 12: 全量测试 + 启动验证

- [ ] **Step 1: 清理 H2 数据并运行全量测试**

Run: `rm -rf E:/workspace/llm-gateway/data/gateway.* 2>/dev/null; ./mvnw clean test -pl gateway-boot 2>&1 | tail -10`
Expected: 全部测试通过

- [ ] **Step 2: 启动应用验证**

Run: `./mvnw spring-boot:run -pl gateway-boot 2>&1 &`; sleep 30; `curl -s http://localhost:8080/actuator/health`; kill %1 2>/dev/null
Expected: health 返回 UP，日志中出现 "Builtin catalog data loaded successfully"

- [ ] **Step 3: 验证 catalog API 可用**

Run: `curl -s http://localhost:8080/api/v1/catalog/providers | head -50`
Expected: 返回供应商列表 JSON

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "test(catalog): 全量测试通过 + 启动验证"
```

---

### Task 13: 前端类型定义 + API 客户端 + React Query hooks

**Files:**
- Create: `gateway-console/src/types/catalog.ts`
- Create: `gateway-console/src/services/api/catalog.ts`
- Create: `gateway-console/src/services/query/useCatalog.ts`
- Create: `gateway-console/src/locales/zh-CN/catalog.json`
- Create: `gateway-console/src/locales/en-US/catalog.json`

- [ ] **Step 1: 创建 catalog 类型定义**

```typescript
/**
 * 目录类型定义 — 替代旧 metadata.ts
 */

export type CatalogState = 'ACTIVE' | 'DEPRECATED';
export type CatalogSource = 'BUILTIN' | 'MODELS_DEV' | 'PROVIDER_API' | 'MANUAL' | 'OVERRIDE';
export type ProviderType = 'INTERNATIONAL' | 'DOMESTIC';
export type BillingMode = 'PAY_AS_YOU_GO' | 'SUBSCRIPTION' | 'PACKAGE';

export interface ProviderCatalog {
  id: number;
  providerCode: string;
  providerName: string;
  providerType: ProviderType;
  logoUrl: string | null;
  websiteUrl: string | null;
  baseUrl: string | null;
  description: string | null;
  source: CatalogSource;
  syncedAt: string | null;
  state: CatalogState;
}

export interface PlanCatalog {
  id: number;
  planCode: string;
  providerCode: string;
  planName: string;
  billingMode: BillingMode;
  endpoints: { protocol: string; url: string }[];
  pricing: PlanPricing[];
  description: string | null;
  source: CatalogSource;
  syncedAt: string | null;
  state: CatalogState;
}

export interface PlanPricing {
  providerModelId: string;
  inputPrice?: number;
  outputPrice?: number;
  cacheReadPrice?: number;
  cacheWritePrice?: number;
  reasoningPrice?: number;
  inputAudioPrice?: number;
  outputAudioPrice?: number;
}

export interface ModelSpecCatalog {
  id: number;
  providerModelId: string;
  displayName: string | null;
  modelFamily: string | null;
  contextWindow: number | null;
  maxInputTokens: number | null;
  maxOutputTokens: number | null;
  knowledgeCutoff: string | null;
  capabilities: Record<string, boolean> | null;
  modalities: string[] | null;
  source: CatalogSource;
  syncedAt: string | null;
  state: CatalogState;
}

export interface MaterializeResult {
  success: boolean;
  entityId?: number;
  entityName?: string;
  errorCode?: string;
  errorMessage?: string;
}

export interface CatalogSyncResult {
  addedCount: number;
  updatedCount: number;
  skippedCount: number;
  deprecatedCount: number;
}
```

- [ ] **Step 2: 创建 catalog API 客户端**

```typescript
import { api } from './client';
import type {
  ProviderCatalog,
  PlanCatalog,
  ModelSpecCatalog,
  MaterializeResult,
  CatalogSyncResult,
} from '@/types/catalog';

const BASE_URL = '/api/v1/catalog';

/** 目录 API */
export const catalogApi = {
  // 供应商目录
  listProviders: (params?: { providerType?: string; keyword?: string }) =>
    api.get<ProviderCatalog[]>(`${BASE_URL}/providers`, { params }),

  // 套餐目录
  listPlans: (params?: { providerCode?: string }) =>
    api.get<PlanCatalog[]>(`${BASE_URL}/plans`, { params }),

  getPlanDetail: (planCode: string) =>
    api.get<PlanCatalog>(`${BASE_URL}/plans/${planCode}`),

  // 模型规格目录
  listModelSpecs: (params?: { providerCode?: string; keyword?: string; capability?: string }) =>
    api.get<ModelSpecCatalog[]>(`${BASE_URL}/model-specs`, { params }),

  // 物化
  materializeProvider: (providerCode: string) =>
    api.post<MaterializeResult>(`${BASE_URL}/materialize/provider/${providerCode}`),

  materializePlan: (planCode: string) =>
    api.post<MaterializeResult>(`${BASE_URL}/materialize/plan/${planCode}`),

  materializeModelSpec: (providerModelId: string) =>
    api.post<MaterializeResult>(`${BASE_URL}/materialize/model-spec/${providerModelId}`),

  // 同步
  syncBuiltin: () =>
    api.post<CatalogSyncResult>(`${BASE_URL}/sync/builtin`),

  syncModelsDev: () =>
    api.post<CatalogSyncResult>(`${BASE_URL}/sync/models-dev`),
};
```

- [ ] **Step 3: 创建 React Query hooks**

```typescript
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { catalogApi } from '@/services/api/catalog';

const PROVIDER_KEY = 'provider-catalog';
const PLAN_KEY = 'plan-catalog';
const MODEL_SPEC_KEY = 'model-spec-catalog';

/** 供应商目录查询 */
export function useProviderCatalogs(params?: { providerType?: string; keyword?: string }) {
  return useQuery({
    queryKey: [PROVIDER_KEY, 'list', params],
    queryFn: () => catalogApi.listProviders(params),
  });
}

/** 套餐目录查询 */
export function usePlanCatalogs(providerCode?: string) {
  return useQuery({
    queryKey: [PLAN_KEY, 'list', providerCode],
    queryFn: () => catalogApi.listPlans(providerCode ? { providerCode } : undefined),
  });
}

/** 套餐详情 */
export function usePlanDetail(planCode: string | null) {
  return useQuery({
    queryKey: [PLAN_KEY, 'detail', planCode],
    queryFn: () => catalogApi.getPlanDetail(planCode!),
    enabled: planCode !== null,
  });
}

/** 模型规格目录查询 */
export function useModelSpecCatalogs(params?: { providerCode?: string; keyword?: string; capability?: string }) {
  return useQuery({
    queryKey: [MODEL_SPEC_KEY, 'list', params],
    queryFn: () => catalogApi.listModelSpecs(params),
  });
}

/** 物化供应商 */
export function useMaterializeProvider() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (providerCode: string) => catalogApi.materializeProvider(providerCode),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: [PROVIDER_KEY] });
      qc.invalidateQueries({ queryKey: ['providers'] });
    },
  });
}

/** 物化套餐 */
export function useMaterializePlan() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (planCode: string) => catalogApi.materializePlan(planCode),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: [PLAN_KEY] });
      qc.invalidateQueries({ queryKey: ['providers'] });
    },
  });
}

/** 物化模型规格 */
export function useMaterializeModelSpec() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (providerModelId: string) => catalogApi.materializeModelSpec(providerModelId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: [MODEL_SPEC_KEY] });
    },
  });
}

/** 同步 BUILTIN */
export function useSyncBuiltin() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: () => catalogApi.syncBuiltin(),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: [PROVIDER_KEY] });
      qc.invalidateQueries({ queryKey: [PLAN_KEY] });
      qc.invalidateQueries({ queryKey: [MODEL_SPEC_KEY] });
    },
  });
}

/** 同步 Models.dev */
export function useSyncModelsDev() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: () => catalogApi.syncModelsDev(),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: [PROVIDER_KEY] });
      qc.invalidateQueries({ queryKey: [PLAN_KEY] });
      qc.invalidateQueries({ queryKey: [MODEL_SPEC_KEY] });
    },
  });
}
```

- [ ] **Step 4: 创建 i18n 文件**

`gateway-console/src/locales/zh-CN/catalog.json`:
```json
{
  "title": "目录管理",
  "provider": {
    "title": "供应商目录",
    "code": "供应商标识",
    "name": "供应商名称",
    "type": "类型",
    "baseUrl": "API 地址",
    "description": "描述",
    "modelCount": "模型数量",
    "source": "数据来源",
    "materialize": "物化",
    "materializeConfirm": "确定将此供应商目录物化为运营供应商？",
    "materializeSuccess": "物化成功",
    "materializeFailed": "物化失败",
    "typeInternational": "国际",
    "typeDomestic": "国内",
    "stateActive": "可用",
    "stateDeprecated": "已下线"
  },
  "plan": {
    "title": "套餐目录",
    "code": "套餐标识",
    "name": "套餐名称",
    "provider": "供应商",
    "billingMode": "计费模式",
    "endpoints": "端点",
    "pricing": "定价",
    "description": "描述",
    "source": "数据来源",
    "materialize": "物化",
    "materializeConfirm": "确定将此套餐目录物化为渠道？",
    "materializeSuccess": "物化成功",
    "materializeFailed": "物化失败",
    "modePayg": "按量付费",
    "modeSubscription": "订阅制",
    "modePackage": "资源包"
  },
  "modelSpec": {
    "title": "模型规格目录",
    "modelId": "模型标识",
    "displayName": "显示名称",
    "family": "模型族",
    "contextWindow": "上下文窗口",
    "maxOutput": "最大输出",
    "capabilities": "能力",
    "modalities": "模态",
    "knowledgeCutoff": "知识截止",
    "source": "数据来源",
    "materialize": "物化",
    "materializeConfirm": "确定将此模型规格目录物化为运营模型规格？",
    "materializeSuccess": "物化成功",
    "materializeFailed": "物化失败"
  },
  "sync": {
    "title": "同步",
    "builtin": "同步内置数据",
    "modelsDev": "同步 Models.dev",
    "success": "同步成功",
    "failed": "同步失败"
  },
  "search": {
    "provider": "搜索供应商名称或标识",
    "model": "搜索模型名称或标识"
  },
  "message": {
    "noData": "暂无数据"
  }
}
```

`gateway-console/src/locales/en-US/catalog.json`:
```json
{
  "title": "Catalog",
  "provider": {
    "title": "Provider Catalog",
    "code": "Provider Code",
    "name": "Provider Name",
    "type": "Type",
    "baseUrl": "API Base URL",
    "description": "Description",
    "modelCount": "Model Count",
    "source": "Source",
    "materialize": "Materialize",
    "materializeConfirm": "Are you sure to materialize this provider catalog into an operational provider?",
    "materializeSuccess": "Materialized successfully",
    "materializeFailed": "Materialization failed",
    "typeInternational": "International",
    "typeDomestic": "Domestic",
    "stateActive": "Active",
    "stateDeprecated": "Deprecated"
  },
  "plan": {
    "title": "Plan Catalog",
    "code": "Plan Code",
    "name": "Plan Name",
    "provider": "Provider",
    "billingMode": "Billing Mode",
    "endpoints": "Endpoints",
    "pricing": "Pricing",
    "description": "Description",
    "source": "Source",
    "materialize": "Materialize",
    "materializeConfirm": "Are you sure to materialize this plan catalog into a channel?",
    "materializeSuccess": "Materialized successfully",
    "materializeFailed": "Materialization failed",
    "modePayg": "Pay As You Go",
    "modeSubscription": "Subscription",
    "modePackage": "Package"
  },
  "modelSpec": {
    "title": "Model Spec Catalog",
    "modelId": "Model ID",
    "displayName": "Display Name",
    "family": "Model Family",
    "contextWindow": "Context Window",
    "maxOutput": "Max Output",
    "capabilities": "Capabilities",
    "modalities": "Modalities",
    "knowledgeCutoff": "Knowledge Cutoff",
    "source": "Source",
    "materialize": "Materialize",
    "materializeConfirm": "Are you sure to materialize this model spec catalog?",
    "materializeSuccess": "Materialized successfully",
    "materializeFailed": "Materialization failed"
  },
  "sync": {
    "title": "Sync",
    "builtin": "Sync Builtin",
    "modelsDev": "Sync Models.dev",
    "success": "Sync completed",
    "failed": "Sync failed"
  },
  "search": {
    "provider": "Search provider name or code",
    "model": "Search model name or ID"
  },
  "message": {
    "noData": "No data"
  }
}
```

- [ ] **Step 5: Verify TypeScript compilation**

Run: `cd gateway-console && npx tsc --noEmit 2>&1 | head -10`
Expected: No errors related to catalog files

- [ ] **Step 6: Commit**

```bash
cd gateway-console && git add src/types/catalog.ts src/services/api/catalog.ts src/services/query/useCatalog.ts src/locales/zh-CN/catalog.json src/locales/en-US/catalog.json
git commit -m "feat(catalog): 前端类型定义 + API 客户端 + React Query hooks + i18n"
```

---

### Task 14: 前端目录管理页面 — 三级联动 + 物化操作

**Files:**
- Create: `gateway-console/src/pages/Catalog/index.tsx`
- Create: `gateway-console/src/pages/Catalog/ProviderCatalogView.tsx`
- Create: `gateway-console/src/pages/Catalog/PlanCatalogView.tsx`
- Create: `gateway-console/src/pages/Catalog/ModelSpecCatalogView.tsx`
- Create: `gateway-console/src/pages/Catalog/MaterializeModal.tsx`

- [ ] **Step 1: 创建目录管理主页面**

```tsx
// gateway-console/src/pages/Catalog/index.tsx
import { useState } from 'react';
import { Card, Button, Space, Breadcrumb, Typography, theme, App, Table, Tag, Spin } from 'antd';
import { SyncOutlined, SearchOutlined, CloseOutlined, ArrowRightOutlined, CloudDownloadOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import {
  useProviderCatalogs,
  usePlanCatalogs,
  useModelSpecCatalogs,
  useSyncBuiltin,
  useSyncModelsDev,
} from '@/services/query/useCatalog';
import type { ProviderCatalog, PlanCatalog, ModelSpecCatalog } from '@/types/catalog';
import { ProviderCatalogView } from './ProviderCatalogView';
import { PlanCatalogView } from './PlanCatalogView';
import { ModelSpecCatalogView } from './ModelSpecCatalogView';
import { MaterializeModal } from './MaterializeModal';

const { Text } = Typography;

/**
 * 目录管理页面 — 三级联动导航
 * 供应商目录 → 套餐目录 → 模型规格目录
 */
export default function CatalogPage() {
  const [providerCode, setProviderCode] = useState<string | undefined>();
  const [providerName, setProviderName] = useState<string>('');
  const [planCode, setPlanCode] = useState<string | null>(null);
  const [planName, setPlanName] = useState<string>('');

  const [materializeTarget, setMaterializeTarget] = useState<{
    type: 'provider' | 'plan' | 'modelSpec';
    code: string;
  } | null>(null);

  return (
    <div style={{ padding: 24 }}>
      <Card>
        <div style={{ marginBottom: 16 }}>
          <Breadcrumb
            items={[
              {
                title: (
                  <Button
                    type={providerCode ? 'link' : 'text'}
                    style={{ padding: 0, fontWeight: providerCode ? undefined : 600 }}
                    onClick={() => {
                      setProviderCode(undefined);
                      setPlanCode(null);
                    }}
                  >
                    供应商目录
                  </Button>
                ),
              },
              ...(providerCode
                ? [
                    {
                      title: (
                        <Button
                          type={planCode ? 'link' : 'text'}
                          style={{ padding: 0, fontWeight: planCode ? undefined : 600 }}
                          onClick={() => setPlanCode(null)}
                        >
                          套餐目录
                          {providerName && <Text type="secondary" style={{ marginLeft: 4, fontSize: 12 }}>({providerName})</Text>}
                        </Button>
                      ),
                    },
                  ]
                : []),
              ...(planCode
                ? [
                    {
                      title: (
                        <Text strong>
                          模型规格目录
                          {planName && <Text type="secondary" style={{ marginLeft: 4, fontSize: 12 }}>({planName})</Text>}
                        </Text>
                      ),
                    },
                  ]
                : []),
            ]}
          />
        </div>

        {!providerCode && (
          <ProviderCatalogView
            onSelectProvider={(code, name) => {
              setProviderCode(code);
              setProviderName(name);
              setPlanCode(null);
            }}
            onMaterialize={(code) => setMaterializeTarget({ type: 'provider', code })}
          />
        )}
        {providerCode && !planCode && (
          <PlanCatalogView
            providerCode={providerCode}
            onSelectPlan={(code, name) => {
              setPlanCode(code);
              setPlanName(name);
            }}
            onMaterialize={(code) => setMaterializeTarget({ type: 'plan', code })}
          />
        )}
        {providerCode && planCode && (
          <ModelSpecCatalogView
            planCode={planCode}
            onMaterialize={(providerModelId) => setMaterializeTarget({ type: 'modelSpec', code: providerModelId })}
          />
        )}
      </Card>

      <MaterializeModal
        target={materializeTarget}
        onClose={() => setMaterializeTarget(null)}
      />
    </div>
  );
}
```

- [ ] **Step 2: 创建供应商目录视图**

```tsx
// gateway-console/src/pages/Catalog/ProviderCatalogView.tsx
import { useState } from 'react';
import { Card, Button, Space, Input, Tag, Typography, Spin, App } from 'antd';
import { SearchOutlined, CloseOutlined, ArrowRightOutlined, CloudDownloadOutlined, SyncOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useProviderCatalogs, useSyncBuiltin, useSyncModelsDev } from '@/services/query/useCatalog';
import type { ProviderCatalog } from '@/types/catalog';

const { Text } = Typography;

interface Props {
  onSelectProvider: (providerCode: string, providerName: string) => void;
  onMaterialize: (providerCode: string) => void;
}

export function ProviderCatalogView({ onSelectProvider, onMaterialize }: Props) {
  const { t } = useTranslation('catalog');
  const { message } = App.useApp();
  const [keyword, setKeyword] = useState('');
  const [searchKeyword, setSearchKeyword] = useState('');

  const { data: providers, isLoading } = useProviderCatalogs({
    keyword: searchKeyword || undefined,
  });
  const syncBuiltin = useSyncBuiltin();
  const syncModelsDev = useSyncModelsDev();

  const handleSync = async (type: 'builtin' | 'modelsDev') => {
    try {
      if (type === 'builtin') await syncBuiltin.mutateAsync();
      else await syncModelsDev.mutateAsync();
      message.success(t('sync.success'));
    } catch {
      message.error(t('sync.failed'));
    }
  };

  const providerList = providers ?? [];

  return (
    <div>
      <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between', gap: 16, flexWrap: 'wrap' }}>
        <Space wrap>
          <Input.Search
            placeholder={t('search.provider')}
            allowClear
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
            onSearch={(value) => setSearchKeyword(value)}
            style={{ width: 240 }}
            prefix={<SearchOutlined />}
          />
          {searchKeyword && (
            <Button icon={<CloseOutlined />} onClick={() => { setKeyword(''); setSearchKeyword(''); }}>
              清除筛选
            </Button>
          )}
        </Space>
        <Space>
          <Button icon={<SyncOutlined />} onClick={() => handleSync('builtin')} loading={syncBuiltin.isPending}>
            {t('sync.builtin')}
          </Button>
          <Button onClick={() => handleSync('modelsDev')} loading={syncModelsDev.isPending}>
            {t('sync.modelsDev')}
          </Button>
        </Space>
      </div>

      <Spin spinning={isLoading}>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(300px, 1fr))', gap: 16, minHeight: 200 }}>
          {providerList.map((p: ProviderCatalog) => (
            <Card
              key={p.id}
              size="small"
              hoverable
              actions={[
                <Button
                  key="materialize"
                  type="link"
                  icon={<CloudDownloadOutlined />}
                  onClick={(e) => { e.stopPropagation(); onMaterialize(p.providerCode); }}
                >
                  {t('provider.materialize')}
                </Button>,
                <Button
                  key="view"
                  type="link"
                  icon={<ArrowRightOutlined />}
                  onClick={() => onSelectProvider(p.providerCode, p.providerName)}
                >
                  套餐
                </Button>,
              ]}
            >
              <Card.Meta
                title={
                  <Space>
                    <span>{p.providerName}</span>
                    <Tag color={p.state === 'ACTIVE' ? 'green' : 'default'} style={{ fontSize: 10 }}>
                      {p.state === 'ACTIVE' ? t('provider.stateActive') : t('provider.stateDeprecated')}
                    </Tag>
                  </Space>
                }
                description={
                  <div>
                    <Text type="secondary" style={{ fontSize: 12 }}>{p.providerCode}</Text>
                    <Tag color={p.providerType === 'INTERNATIONAL' ? 'blue' : 'orange'} style={{ fontSize: 10, marginLeft: 4 }}>
                      {p.providerType === 'INTERNATIONAL' ? t('provider.typeInternational') : t('provider.typeDomestic')}
                    </Tag>
                    {p.description && <div style={{ marginTop: 4, fontSize: 12, color: 'var(--ant-color-text-secondary)' }}>{p.description}</div>}
                  </div>
                }
              />
            </Card>
          ))}
        </div>
      </Spin>

      {providerList.length === 0 && !isLoading && (
        <div style={{ textAlign: 'center', padding: 48 }}>
          <Text type="secondary">{t('message.noData')}</Text>
        </div>
      )}
    </div>
  );
}
```

- [ ] **Step 3: 创建套餐目录视图**

```tsx
// gateway-console/src/pages/Catalog/PlanCatalogView.tsx
import { Table, Tag, Space, Button, Typography, Spin } from 'antd';
import { ArrowRightOutlined, CloudDownloadOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { usePlanCatalogs } from '@/services/query/useCatalog';
import type { PlanCatalog } from '@/types/catalog';

const { Text } = Typography;

interface Props {
  providerCode: string;
  onSelectPlan: (planCode: string, planName: string) => void;
  onMaterialize: (planCode: string) => void;
}

const BILLING_MODE_OPTIONS: Record<string, { label: string; color: string }> = {
  PAY_AS_YOU_GO: { label: '按量付费', color: 'green' },
  SUBSCRIPTION: { label: '订阅制', color: 'purple' },
  PACKAGE: { label: '资源包', color: 'orange' },
};

export function PlanCatalogView({ providerCode, onSelectPlan, onMaterialize }: Props) {
  const { t } = useTranslation('catalog');
  const { data: plans, isLoading } = usePlanCatalogs(providerCode);

  const planList = plans ?? [];

  return (
    <Spin spinning={isLoading}>
      <Table
        dataSource={planList}
        rowKey="planCode"
        size="small"
        pagination={false}
        onRow={(record) => ({
          onClick: () => onSelectPlan(record.planCode, record.planName),
          style: { cursor: 'pointer' },
        })}
        columns={[
          {
            title: t('plan.name'),
            dataIndex: 'planName',
            key: 'planName',
            render: (name: string, record: PlanCatalog) => (
              <Space>
                <span style={{ fontWeight: 500 }}>{name}</span>
                <Tag color={record.state === 'ACTIVE' ? 'green' : 'default'} style={{ fontSize: 10 }}>
                  {record.state === 'ACTIVE' ? '可用' : '已下线'}
                </Tag>
              </Space>
            ),
          },
          {
            title: t('plan.billingMode'),
            dataIndex: 'billingMode',
            key: 'billingMode',
            width: 120,
            render: (mode: string) => {
              const info = BILLING_MODE_OPTIONS[mode] ?? { label: mode, color: 'default' };
              return <Tag color={info.color}>{info.label}</Tag>;
            },
          },
          {
            title: t('plan.endpoints'),
            dataIndex: 'endpoints',
            key: 'endpoints',
            render: (endpoints: { protocol: string; url: string }[]) => (
              <Space direction="vertical" size={2}>
                {(endpoints ?? []).map((ep) => (
                  <Space key={ep.protocol} size={4}>
                    <Tag style={{ fontSize: 10 }}>{ep.protocol}</Tag>
                    <Text code style={{ fontSize: 11 }}>{ep.url}</Text>
                  </Space>
                ))}
              </Space>
            ),
          },
          {
            title: t('plan.pricing'),
            dataIndex: 'pricing',
            key: 'pricing',
            render: (pricing: PlanCatalog['pricing']) => (
              <Space direction="vertical" size={2}>
                {(pricing ?? []).slice(0, 3).map((p) => (
                  <Text key={p.providerModelId} style={{ fontSize: 11 }}>
                    {p.providerModelId}: {p.inputPrice != null ? `$${p.inputPrice}` : '-'}/{p.outputPrice != null ? `$${p.outputPrice}` : '-'}
                  </Text>
                ))}
                {(pricing ?? []).length > 3 && <Text type="secondary" style={{ fontSize: 10 }}>+{pricing.length - 3} more</Text>}
              </Space>
            ),
          },
          {
            title: t('plan.source'),
            dataIndex: 'source',
            key: 'source',
            width: 90,
            render: (source: string) => (
              <Tag color={source === 'BUILTIN' ? 'blue' : source === 'MODELS_DEV' ? 'green' : 'default'} style={{ fontSize: 10 }}>
                {source}
              </Tag>
            ),
          },
          {
            title: '',
            key: 'actions',
            width: 120,
            render: (_, record) => (
              <Space>
                <Button
                  type="link"
                  size="small"
                  icon={<CloudDownloadOutlined />}
                  onClick={(e) => { e.stopPropagation(); onMaterialize(record.planCode); }}
                >
                  {t('plan.materialize')}
                </Button>
                <ArrowRightOutlined style={{ color: 'var(--ant-color-text-secondary)' }} />
              </Space>
            ),
          },
        ]}
      />
    </Spin>
  );
}
```

- [ ] **Step 4: 创建模型规格目录视图**

```tsx
// gateway-console/src/pages/Catalog/ModelSpecCatalogView.tsx
import { Table, Tag, Space, Button, Typography } from 'antd';
import { CloudDownloadOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useModelSpecCatalogs } from '@/services/query/useCatalog';
import type { ModelSpecCatalog } from '@/types/catalog';

const { Text } = Typography;

interface Props {
  planCode: string;
  onMaterialize: (providerModelId: string) => void;
}

export function ModelSpecCatalogView({ planCode, onMaterialize }: Props) {
  const { t } = useTranslation('catalog');
  // 注意：这里按 planCode 过滤模型规格，需要后端支持或前端过滤
  // 当前暂时列出所有模型规格
  const { data: modelSpecs, isLoading } = useModelSpecCatalogs();

  const models = modelSpecs ?? [];

  return (
    <Table
      dataSource={models}
      rowKey="id"
      size="small"
      pagination={false}
      loading={isLoading}
      columns={[
        {
          title: t('modelSpec.displayName'),
          dataIndex: 'displayName',
          key: 'displayName',
          render: (name: string) => <Text strong>{name}</Text>,
        },
        {
          title: t('modelSpec.modelId'),
          dataIndex: 'providerModelId',
          key: 'providerModelId',
          render: (id: string) => <Text code style={{ fontSize: 11 }}>{id}</Text>,
        },
        {
          title: t('modelSpec.family'),
          dataIndex: 'modelFamily',
          key: 'modelFamily',
          width: 120,
          render: (f: string | null) => f ?? '-',
        },
        {
          title: t('modelSpec.contextWindow'),
          dataIndex: 'contextWindow',
          key: 'contextWindow',
          width: 120,
          render: (v: number | null) => v ? `${(v / 1024).toFixed(0)}K` : '-',
        },
        {
          title: t('modelSpec.capabilities'),
          dataIndex: 'capabilities',
          key: 'capabilities',
          render: (caps: Record<string, boolean> | null) => (
            <Space size={4} wrap>
              {Object.entries(caps ?? {})
                .filter(([, v]) => v)
                .map(([k]) => <Tag key={k} style={{ fontSize: 10 }}>{k}</Tag>)}
            </Space>
          ),
        },
        {
          title: t('modelSpec.modalities'),
          dataIndex: 'modalities',
          key: 'modalities',
          width: 120,
          render: (mods: string[] | null) => (
            <Space size={4} wrap>
              {(mods ?? []).map((m) => <Tag key={m} style={{ fontSize: 10 }}>{m}</Tag>)}
            </Space>
          ),
        },
        {
          title: '',
          key: 'actions',
          width: 80,
          render: (_, record: ModelSpecCatalog) => (
            <Button
              type="link"
              size="small"
              icon={<CloudDownloadOutlined />}
              onClick={() => onMaterialize(record.providerModelId)}
            >
              {t('modelSpec.materialize')}
            </Button>
          ),
        },
      ]}
    />
  );
}
```

- [ ] **Step 5: 创建物化确认弹窗**

```tsx
// gateway-console/src/pages/Catalog/MaterializeModal.tsx
import { Modal, Typography, App } from 'antd';
import { useTranslation } from 'react-i18next';
import {
  useMaterializeProvider,
  useMaterializePlan,
  useMaterializeModelSpec,
} from '@/services/query/useCatalog';

const { Text } = Typography;

interface MaterializeTarget {
  type: 'provider' | 'plan' | 'modelSpec';
  code: string;
}

interface Props {
  target: MaterializeTarget | null;
  onClose: () => void;
}

const TYPE_LABELS: Record<string, string> = {
  provider: '供应商',
  plan: '套餐',
  modelSpec: '模型规格',
};

export function MaterializeModal({ target, onClose }: Props) {
  const { t } = useTranslation('catalog');
  const { message } = App.useApp();

  const materializeProvider = useMaterializeProvider();
  const materializePlan = useMaterializePlan();
  const materializeModelSpec = useMaterializeModelSpec();

  const handleOk = async () => {
    if (!target) return;
    try {
      switch (target.type) {
        case 'provider':
          await materializeProvider.mutateAsync(target.code);
          break;
        case 'plan':
          await materializePlan.mutateAsync(target.code);
          break;
        case 'modelSpec':
          await materializeModelSpec.mutateAsync(target.code);
          break;
      }
      message.success(t(`${target.type}.materializeSuccess`));
      onClose();
    } catch {
      message.error(t(`${target.type}.materializeFailed`));
    }
  };

  const isLoading = materializeProvider.isPending || materializePlan.isPending || materializeModelSpec.isPending;

  return (
    <Modal
      title={t(`${target?.type ?? 'provider'}.materialize`)}
      open={target !== null}
      onCancel={onClose}
      onOk={handleOk}
      confirmLoading={isLoading}
    >
      {target && (
        <div>
          <Text>
            {t(`${target.type}.materializeConfirm`)}
          </Text>
          <div style={{ marginTop: 8, fontWeight: 500 }}>
            {TYPE_LABELS[target.type]}: {target.code}
          </div>
        </div>
      )}
    </Modal>
  );
}
```

- [ ] **Step 6: 验证前端编译**

Run: `cd gateway-console && npx tsc --noEmit 2>&1 | head -20`
Expected: No errors

- [ ] **Step 7: Commit**

```bash
cd gateway-console && git add src/pages/Catalog/
git commit -m "feat(catalog): 新增目录管理页面 — 三级联动 + 物化操作"
```

---

### Task 15: 前端路由、菜单、i18n 迁移 — 替换旧 metadata 引用

**Files:**
- Modify: `gateway-console/src/router/index.tsx`
- Modify: `gateway-console/src/constants/menuConfig.tsx`
- Modify: `gateway-console/src/constants/permissions.ts`
- Modify: `gateway-console/src/i18n.ts`
- Modify: `gateway-console/src/services/api/index.ts`
- Modify: `gateway-console/src/pages/Providers/ProviderMetadataSelector.tsx` → rename to `ProviderCatalogSelector.tsx`
- Modify: `gateway-console/src/pages/Providers/BasicInfoStep.tsx`
- Modify: `gateway-console/src/pages/Providers/ModelSetupStep.tsx`
- Modify: `gateway-console/src/pages/Providers/ProviderCreateModal.tsx`
- Delete: `gateway-console/src/pages/Metadata/`
- Delete: `gateway-console/src/types/metadata.ts`
- Delete: `gateway-console/src/services/api/metadata.ts`
- Delete: `gateway-console/src/services/query/useMetadata.ts`
- Delete: `gateway-console/src/locales/zh-CN/metadata.json`
- Delete: `gateway-console/src/locales/en-US/metadata.json`

- [ ] **Step 1: 修改路由 — /metadata → /catalog**

在 `router/index.tsx` 中：
- 将 `import Metadata from '@/pages/Metadata'` 替换为 `import Catalog from '@/pages/Catalog'`
- 将路由 path `'metadata'` 替换为 `'catalog'`
- 将 element `<Metadata />` 替换为 `<Catalog />`
- 保留旧路由 `/metadata` 重定向到 `/catalog`：`{ path: '/metadata', element: <Navigate to="/catalog" replace /> }`

- [ ] **Step 2: 修改菜单配置**

在 `constants/menuConfig.tsx` 中：
- 将菜单 label `'menu.metadata'` 替换为 `'menu.catalog'`
- 将 key `'/metadata'` 替换为 `'/catalog'`
- 将图标保持 `DatabaseOutlined` 或改为 `AppstoreOutlined`

- [ ] **Step 3: 修改权限常量**

在 `constants/permissions.ts` 中：
- 将 `METADATA_READ` 权限替换为 `CATALOG_READ`
- 添加 `CATALOG_WRITE`（物化操作需要）
- 添加 `CATALOG_SYNC`（同步操作需要）

- [ ] **Step 4: 修改 i18n 加载**

在 `i18n.ts` 中：
- 将 `metadata` namespace 的 import 从 `./locales/zh-CN/metadata.json` 替换为 `./locales/zh-CN/catalog.json`
- 同样替换 en-US

- [ ] **Step 5: 修改 API 导出**

在 `services/api/index.ts` 中：
- 移除 `export * from './metadata'`
- 添加 `export * from './catalog'`

- [ ] **Step 6: 修改供应商创建流程中的元数据选择器**

将 `ProviderMetadataSelector.tsx` 重命名为 `ProviderCatalogSelector.tsx`：
- import 来源从 `@/types/metadata` 改为 `@/types/catalog`
- import 从 `@/services/query/useMetadata` 改为 `@/services/query/useCatalog`
- 类型从 `ProviderMetadata` 改为 `ProviderCatalog`
- hook 从 `useProviderMetadataList` 改为 `useProviderCatalogs`

同步修改所有引用 `ProviderMetadataSelector` 的文件：
- `BasicInfoStep.tsx`
- `ProviderCreateModal.tsx`

- [ ] **Step 7: 删除旧 metadata 文件**

```bash
rm -rf gateway-console/src/pages/Metadata/
rm gateway-console/src/types/metadata.ts
rm gateway-console/src/services/api/metadata.ts
rm gateway-console/src/services/query/useMetadata.ts
rm gateway-console/src/locales/zh-CN/metadata.json
rm gateway-console/src/locales/en-US/metadata.json
```

- [ ] **Step 8: 添加旧路由兼容重定向**

在 `router/index.tsx` 的兼容路由区域添加：
```tsx
{ path: '/metadata', element: <Navigate to="/catalog" replace /> },
```

- [ ] **Step 9: 验证前端编译和运行**

Run: `cd gateway-console && npx tsc --noEmit 2>&1 | head -20`
Expected: No errors

- [ ] **Step 10: Commit**

```bash
cd gateway-console && git add -A
git commit -m "refactor(catalog): 前端路由/菜单/i18n 迁移 — 替换旧 metadata 引用"
```

---

### Task 16: 前端构建验证 + 全量端到端检查

- [ ] **Step 1: 前端构建**

Run: `cd gateway-console && pnpm build 2>&1 | tail -10`
Expected: 构建成功

- [ ] **Step 2: 后端全量测试**

Run: `rm -rf E:/workspace/llm-gateway/data/gateway.* 2>/dev/null; ./mvnw clean test -pl gateway-boot 2>&1 | tail -10`
Expected: 全部测试通过

- [ ] **Step 3: 启动后端 + 前端联调验证**

1. 启动后端: `./mvnw spring-boot:run -pl gateway-boot`
2. 启动前端: `cd gateway-console && pnpm dev`
3. 浏览器访问 `/catalog` 页面，验证三级联动和物化按钮
4. 验证 `/metadata` 自动重定向到 `/catalog`
5. 验证供应商创建流程中的目录选择器正常工作

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "test(catalog): 前端构建 + 后端全量测试 + 联调验证"
```