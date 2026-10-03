# 应用渠道配置缓存实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 数据面应用渠道配置读取缓存化（Caffeine 本地缓存）：新增 `ApplicationChannelConfigProvider`（缓存装饰 Repository），`InstanceSelector` 改消费 Provider，配置写操作显式失效——10k QPS 下配置读取 DB 查询降为 0。

**Architecture:** iam 域新增 `ApplicationChannelConfigProvider`（@Service）：两个 Caffeine 缓存（应用→渠道集合、应用→渠道优先级映射），`get(applicationId, loader)` 模式 + `evict(applicationId)` 显式失效。失效钩子挂在 iam 的 `ApplicationServiceImpl`（`delete`/`updateChannels` 写操作后 evict——单一失效点，实测写操作全部在此）。`InstanceSelector` 改注入 Provider（替代直查 Repository）。TTL 60s 兜底防遗漏。

**Tech Stack:** Java 21 + Spring Boot 3.5 + Caffeine（版本由 Spring Boot BOM 管理）+ JUnit 5 + Mockito + AssertJ

## Global Constraints

- 行为零变化：`findChannelIdsByApplicationId`/`findPriorityMapByApplicationId` 的返回值与现状一致；null applicationId → 空集/空映射（不缓存 null）
- 缓存语义：显式失效为主（配置写操作即时生效）、TTL 60s 兜底、maximumSize 500、recordStats 可观测
- 失效钩子：仅在 `ApplicationServiceImpl`（iam）的 `delete(Long)`、`updateChannels(Long, List)` 内 evict(applicationId)——已实测 application_channel 写操作全部在此
- `InstanceSelector` 注入 `ApplicationChannelConfigProvider`（替代 `ApplicationChannelRepository` 直查），行为等价（候选实例集合不变）
- Caffeine 依赖加入 `gateway-iam/iam` pom（`com.github.ben-manes.caffeine:caffeine`，不指定版本——Spring Boot BOM 管理；若 iam 未继承 BOM，实现者以项目现有依赖管理方式为准并报告）
- 模块坐标：`gateway-iam/iam`、`gateway-proxy/proxy`；测试命令 `mvn -q -pl <module> -am test`（`-Dtest=` 筛选时追加 `-Dsurefire.failIfNoSpecifiedTests=false`）
- 代码注释、Javadoc、commit message 一律中文
- Windows Git Bash：mvn 报 `NoDefaultCurrentDirectoryInExePath` 时用 `env -u NoDefaultCurrentDirectoryInExePath mvn ...` 重试

---

### Task 1: ApplicationChannelConfigProvider（缓存提供者）

**Files:**
- Modify: `gateway-iam/iam/pom.xml`（新增 Caffeine 依赖）
- Create: `gateway-iam/iam/src/main/java/com/codingas/gateway/iam/application/ApplicationChannelConfigProvider.java`
- Test: `gateway-iam/iam/src/test/java/com/codingas/gateway/iam/application/ApplicationChannelConfigProviderTest.java`

**Interfaces:**
- Consumes: `ApplicationChannelRepository.findChannelIdsByApplicationId(Long)`、`ApplicationChannelRepository.findByApplicationId(Long)`、`ApplicationChannel.getPriority()`
- Produces: `Set<Long> findChannelIdsByApplicationId(Long)`、`Map<Long, Integer> findPriorityMapByApplicationId(Long)`、`void evict(Long)`——Task 2 消费

- [ ] **Step 1: 写失败测试**

```java
package com.codingas.gateway.iam.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ApplicationChannelConfigProvider（应用渠道配置缓存）测试")
class ApplicationChannelConfigProviderTest {

    @Mock
    private ApplicationChannelRepository repository;

    private ApplicationChannelConfigProvider provider;

    @BeforeEach
    void setUp() {
        provider = new ApplicationChannelConfigProvider(repository);
    }

    private ApplicationChannel channel(long id, Integer priority) {
        ApplicationChannel ch = new ApplicationChannel();
        ch.setChannelId(id);
        ch.setPriority(priority);
        return ch;
    }

    @Test
    @DisplayName("findChannelIdsByApplicationId：首次查仓储，命中后不再查询")
    void channelIds_firstLoad_thenCached() {
        when(repository.findChannelIdsByApplicationId(100L)).thenReturn(Set.of(1L, 2L));

        assertThat(provider.findChannelIdsByApplicationId(100L)).containsExactlyInAnyOrder(1L, 2L);
        assertThat(provider.findChannelIdsByApplicationId(100L)).containsExactlyInAnyOrder(1L, 2L);
        verify(repository, times(1)).findChannelIdsByApplicationId(100L);
    }

    @Test
    @DisplayName("findChannelIdsByApplicationId：applicationId 为 null 返回空集，不查仓储不缓存")
    void channelIds_nullApplicationId_returnsEmpty() {
        assertThat(provider.findChannelIdsByApplicationId(null)).isEmpty();
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("findPriorityMapByApplicationId：按优先级构建映射并缓存（null 优先级剔除）")
    void priorityMap_buildsAndCaches() {
        when(repository.findByApplicationId(100L)).thenReturn(List.of(channel(1L, 10), channel(2L, null)));

        assertThat(provider.findPriorityMapByApplicationId(100L))
                .containsExactlyEntriesOf(Map.of(1L, 10));
        assertThat(provider.findPriorityMapByApplicationId(100L))
                .containsExactlyEntriesOf(Map.of(1L, 10));
        verify(repository, times(1)).findByApplicationId(100L);
    }

    @Test
    @DisplayName("findPriorityMapByApplicationId：applicationId 为 null 返回空映射")
    void priorityMap_nullApplicationId_returnsEmpty() {
        assertThat(provider.findPriorityMapByApplicationId(null)).isEmpty();
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("evict：失效后重新查询仓储（配置变更即时生效）")
    void evict_reloadsFromRepository() {
        when(repository.findChannelIdsByApplicationId(100L)).thenReturn(Set.of(1L));
        assertThat(provider.findChannelIdsByApplicationId(100L)).containsExactly(1L);

        provider.evict(100L);
        when(repository.findChannelIdsByApplicationId(100L)).thenReturn(Set.of(1L, 2L));
        assertThat(provider.findChannelIdsByApplicationId(100L)).containsExactlyInAnyOrder(1L, 2L);
        verify(repository, times(2)).findChannelIdsByApplicationId(100L);
    }
}
```

注：`ApplicationChannel` 的 setter（setChannelId/setPriority）以实体实际字段为准（实现者确认）；若字段仅 Lombok @Data 则有 setter。

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn -q -pl gateway-iam/iam -am test -Dtest=ApplicationChannelConfigProviderTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: 编译失败 `cannot find symbol: class ApplicationChannelConfigProvider`

- [ ] **Step 3: 最小实现**

`gateway-iam/iam/pom.xml` 新增依赖：

```xml
        <dependency>
            <groupId>com.github.ben-manes.caffeine</groupId>
            <artifactId>caffeine</artifactId>
        </dependency>
```

`ApplicationChannelConfigProvider.java`：

```java
package com.codingas.gateway.iam.application;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 应用渠道配置提供者（本地缓存装饰）
 *
 * <p>数据面路由前配置读取（应用→渠道集合、应用→渠道优先级映射）为每请求热点查询，
 * 以 Caffeine 本地缓存承载：显式失效为主（配置写操作即时 evict）、TTL 60 秒兜底防遗漏。
 * applicationId 为 null 不缓存（直接返回空集/空映射，语义与现状一致）。</p>
 */
@Service
public class ApplicationChannelConfigProvider {

    /** 应用 → 可见渠道集合缓存 */
    private final Cache<Long, Set<Long>> channelIdsCache;
    /** 应用 → 渠道优先级映射缓存 */
    private final Cache<Long, Map<Long, Integer>> priorityMapCache;

    private final ApplicationChannelRepository repository;

    public ApplicationChannelConfigProvider(ApplicationChannelRepository repository) {
        this.repository = repository;
        this.channelIdsCache = newCache();
        this.priorityMapCache = newCache();
    }

    /**
     * 应用可见渠道集合（缓存）
     *
     * @param applicationId 应用 ID（null → 空集，不缓存）
     * @return 应用可见的渠道 ID 集合
     */
    public Set<Long> findChannelIdsByApplicationId(Long applicationId) {
        if (applicationId == null) {
            return Set.of();
        }
        return channelIdsCache.get(applicationId,
                id -> repository.findChannelIdsByApplicationId(id));
    }

    /**
     * 应用渠道优先级映射（缓存；null 优先级剔除，PriorityRouter 回退默认值）
     *
     * @param applicationId 应用 ID（null → 空映射，不缓存）
     * @return 渠道 ID → 优先级 映射
     */
    public Map<Long, Integer> findPriorityMapByApplicationId(Long applicationId) {
        if (applicationId == null) {
            return Map.of();
        }
        return priorityMapCache.get(applicationId, id -> {
            Map<Long, Integer> map = new LinkedHashMap<>();
            for (ApplicationChannel channel : repository.findByApplicationId(id)) {
                if (channel.getPriority() != null) {
                    map.put(channel.getChannelId(), channel.getPriority());
                }
            }
            return map;
        });
    }

    /**
     * 显式失效（配置写操作后调用——变更即时生效）
     *
     * @param applicationId 应用 ID
     */
    public void evict(Long applicationId) {
        channelIdsCache.invalidate(applicationId);
        priorityMapCache.invalidate(applicationId);
    }

    private static <K, V> Cache<K, V> newCache() {
        return Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(60, TimeUnit.SECONDS)
                .recordStats()
                .build();
    }
}
```

注：Caffeine 包名以实际版本为准（`com.github.benmanes.caffeine`——若 BOM 管理的版本为 3.x 用 `com.github.benmanes.caffeine.cache.Caffeine`；实现者以实际可解析为准并报告）。

- [ ] **Step 4: 运行测试确认通过**

Run: `mvn -q -pl gateway-iam/iam -am test -Dtest=ApplicationChannelConfigProviderTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: PASS（5 个测试全绿）

- [ ] **Step 5: 提交**

```bash
git add gateway-iam/iam/pom.xml gateway-iam/iam/src/main/java/com/codingas/gateway/iam/application/ApplicationChannelConfigProvider.java gateway-iam/iam/src/test/java/com/codingas/gateway/iam/application/ApplicationChannelConfigProviderTest.java
git commit -m "feat(iam): 应用渠道配置缓存提供者（Caffeine 本地缓存 + 显式失效）"
```

---

### Task 2: InstanceSelector 消费 Provider + 失效钩子

**Files:**
- Modify: `gateway-proxy/proxy/src/main/java/com/codingas/gateway/proxy/routing/InstanceSelector.java`
- Modify: `gateway-iam/iam/src/main/java/com/codingas/gateway/iam/application/ApplicationServiceImpl.java`
- Modify: `gateway-proxy/proxy/src/test/java/com/codingas/gateway/proxy/routing/InstanceSelectorTest.java`
- Modify: `gateway-iam/iam/src/test/java/com/codingas/gateway/iam/application/ApplicationServiceImplTest.java`

**Interfaces:**
- Consumes: Task 1 的 `ApplicationChannelConfigProvider`（findChannelIdsByApplicationId/findPriorityMapByApplicationId/evict）
- Produces: `InstanceSelector` 构造器 `ApplicationChannelRepository` → `ApplicationChannelConfigProvider`（行为等价）；`ApplicationServiceImpl.delete`/`updateChannels` 写操作后调用 `evict(applicationId)`

- [ ] **Step 1: 改写失败测试**

`InstanceSelectorTest`：`@Mock ApplicationChannelRepository` → `@Mock ApplicationChannelConfigProvider`，`getConfiguredChannelIds`/`buildChannelPriorityMap` 相关 stub 改为 provider 方法（断言不变：候选集合/空配置异常等）。
`ApplicationServiceImplTest`：现有 `verify(applicationChannelRepository).saveAll(...)`/`deleteByApplicationId` 用例追加 `verify(applicationChannelConfigProvider).evict(id)`（先读现有测试确认构造器注入形态）。

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn -q -pl gateway-proxy/proxy,gateway-iam/iam -am test -Dtest=InstanceSelectorTest,ApplicationServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: 编译失败（构造器参数不匹配 / mock 类型不匹配）

- [ ] **Step 3: 修改实现**

`InstanceSelector` 改造（构造器 + 两处消费）：

```java
    /** 应用-渠道配置提供者（缓存装饰——配置读取热点本地缓存） */
    private final ApplicationChannelConfigProvider applicationChannelConfigProvider;
```

`getConfiguredChannelIds`：
```java
    private Set<Long> getConfiguredChannelIds(Long applicationId) {
        return applicationChannelConfigProvider.findChannelIdsByApplicationId(applicationId);
    }
```

`buildChannelPriorityMap`：
```java
    private Map<Long, Integer> buildChannelPriorityMap(Long applicationId) {
        return applicationChannelConfigProvider.findPriorityMapByApplicationId(applicationId);
    }
```

（删除 `ApplicationChannelRepository`/`ApplicationChannel` 的 import 与字段；`buildChannelPriorityMap` 的 null 分支已由 Provider 内部处理，方法体简化。）

`ApplicationServiceImpl` 失效钩子（注入 Provider，写操作后 evict）：

```java
    // delete(Long id) 内 deleteByApplicationId(id) 之后：
    applicationChannelConfigProvider.evict(id);

    // updateChannels(Long id, ...) 内 saveAll(channels) 之后：
    applicationChannelConfigProvider.evict(id);
```

- [ ] **Step 4: 运行测试确认通过**

Run: `mvn -q -pl gateway-proxy/proxy,gateway-iam/iam -am test`
Expected: PASS（两模块全绿，无回归）

- [ ] **Step 5: 提交**

```bash
git add gateway-proxy/proxy/src gateway-iam/iam/src
git commit -m "feat(proxy,iam): 配置读取经缓存提供者 + 写操作显式失效"
```

---

### Task 3: 手工失效管理端点

**Files:**
- Modify: `gateway-iam/iam/src/main/java/com/codingas/gateway/iam/application/ApplicationChannelConfigProvider.java`（新增 `clearAll()`）
- Modify: `gateway-iam/iam/src/test/java/com/codingas/gateway/iam/application/ApplicationChannelConfigProviderTest.java`
- Create: `gateway-web/src/main/java/com/codingas/gateway/web/api/CacheAdminController.java`
- Create: `gateway-web/src/test/java/com/codingas/gateway/web/api/CacheAdminControllerTest.java`

**Interfaces:**
- Consumes: Task 1 的 `ApplicationChannelConfigProvider.evict(Long)`（新增 `clearAll()`）
- Produces: `POST /api/v1/admin/cache/evict?applicationId=...`（applicationId 可空 = 全清）；端点属管理路径 `/api/v1/` 未匹配规则 → 控制面授权默认**仅 ADMIN**（`AuthorizationService` 未匹配规则默认拒绝分支，无需新增规则）

- [ ] **Step 1: 写失败测试**

`ApplicationChannelConfigProviderTest` 追加：

```java
    @Test
    @DisplayName("clearAll：清空全部缓存后重新查询仓储")
    void clearAll_reloadsAllFromRepository() {
        when(repository.findChannelIdsByApplicationId(100L)).thenReturn(Set.of(1L));
        when(repository.findChannelIdsByApplicationId(200L)).thenReturn(Set.of(2L));
        provider.findChannelIdsByApplicationId(100L);
        provider.findChannelIdsByApplicationId(200L);

        provider.clearAll();
        provider.findChannelIdsByApplicationId(100L);
        provider.findChannelIdsByApplicationId(200L);

        verify(repository, times(2)).findChannelIdsByApplicationId(100L);
        verify(repository, times(2)).findChannelIdsByApplicationId(200L);
    }
```

`CacheAdminControllerTest`（MockMvc standalone 或直接调用——实现者按现有 controller 测试形态）：

```java
    @Test
    @DisplayName("evict 带 applicationId：仅失效指定应用")
    void evict_withApplicationId() throws Exception {
        mockMvc.perform(post("/api/v1/admin/cache/evict").param("applicationId", "100"))
                .andExpect(status().isOk());
        verify(provider).evict(100L);
        verify(provider, never()).clearAll();
    }

    @Test
    @DisplayName("evict 无 applicationId：清空全部缓存")
    void evict_withoutApplicationId_clearsAll() throws Exception {
        mockMvc.perform(post("/api/v1/admin/cache/evict"))
                .andExpect(status().isOk());
        verify(provider, never()).evict(any());
        verify(provider).clearAll();
    }
```

注：授权（仅 ADMIN）由拦截器链保证（`/api/v1/admin/cache/evict` 未匹配 CONTROL_RULES → 默认拒绝仅 ADMIN），controller 单测不重复验证授权——实现者确认该路径确实未匹配规则（若无规则命中需在测试注明授权由链保证）。

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn -q -pl gateway-iam/iam,gateway-web -am test -Dtest=ApplicationChannelConfigProviderTest,CacheAdminControllerTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: 编译失败（clearAll / CacheAdminController 不存在）

- [ ] **Step 3: 最小实现**

`ApplicationChannelConfigProvider` 新增：

```java
    /**
     * 清空全部缓存（运维手工失效：全量重新加载）
     */
    public void clearAll() {
        channelIdsCache.invalidateAll();
        priorityMapCache.invalidateAll();
    }
```

`CacheAdminController.java`：

```java
package com.codingas.gateway.web.api;

import com.codingas.gateway.iam.application.ApplicationChannelConfigProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 缓存管理控制器
 *
 * <p>运维手工失效入口：按应用失效（applicationId）或清空全部缓存（缺省）。
 * 管理路径授权由拦截器链保证（未匹配授权规则 → 仅 ADMIN）。</p>
 */
@RestController
@RequestMapping("/api/v1/admin/cache")
@RequiredArgsConstructor
public class CacheAdminController {

    private final ApplicationChannelConfigProvider applicationChannelConfigProvider;

    /**
     * 缓存失效（手工运维）
     *
     * @param applicationId 应用 ID（可空——为空时清空全部缓存）
     */
    @PostMapping("/evict")
    public void evict(@RequestParam(value = "applicationId", required = false) Long applicationId) {
        if (applicationId != null) {
            applicationChannelConfigProvider.evict(applicationId);
        } else {
            applicationChannelConfigProvider.clearAll();
        }
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `mvn -q -pl gateway-iam/iam,gateway-web -am test -Dtest=ApplicationChannelConfigProviderTest,CacheAdminControllerTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: PASS（Provider 6 用例 + Controller 2 用例全绿）

- [ ] **Step 5: 提交**

```bash
git add gateway-iam/iam/src gateway-web/src/main/java/com/codingas/gateway/web/api/CacheAdminController.java gateway-web/src/test/java/com/codingas/gateway/web/api/CacheAdminControllerTest.java
git commit -m "feat(web,iam): 缓存手工失效管理端点（按应用 evict / 全清 clearAll，仅 ADMIN）"
```

---

### Task 3b: 前端 console 缓存清理功能

**Files:**
- Modify: `gateway-console/src/services/api/settings.ts`（新增 `evictCache`）
- Modify: `gateway-console/src/services/query/useSettings.ts`（新增 `useEvictCache`）
- Modify: `gateway-console/src/pages/Settings/index.tsx`（新增"缓存清理"分组）
- Modify: `gateway-console/src/locales/...`（i18n 文案，按现有语言文件结构）
- Test: `gateway-console/src/services/api/__tests__/settings.test.ts`（或现有对应测试文件）
- Test: `gateway-console/src/pages/Settings/__tests__/...`（如存在设置页测试）

**Interfaces:**
- Consumes: Task 3 的后端端点 `POST /api/v1/admin/cache/evict?applicationId=...`
- Produces: 前端"缓存清理"入口——全部清理按钮 + 按应用 ID 清理（复用 `useCleanupAuditLogs` 的 useMutation 模式）

- [ ] **Step 1: 写失败测试**（前端 vitest + testing-library，按现有 `__tests__` 形态）

```ts
// services/api/__tests__/settings.test.ts（或对应文件）追加：
describe('evictCache', () => {
  it('调用 POST /api/v1/admin/cache/evict（无 applicationId = 全清）', async () => {
    // mock client 请求，断言 method POST + 路径 + 无 applicationId 参数
  });
  it('带 applicationId 时作为查询参数传递', async () => {
    // 断言 query param applicationId=100
  });
});
```

- [ ] **Step 2: 运行测试确认失败**

Run: `cd gateway-console && npx vitest run`（或项目现有测试命令，实现者以 package.json scripts 为准）
Expected: FAIL（evictCache 不存在）

- [ ] **Step 3: 最小实现**

`services/api/settings.ts` 追加（仿 `cleanupAuditLogs` 形态）：

```ts
  /**
   * 应用渠道配置缓存失效（运维手工清理）
   *
   * @param applicationId 应用 ID（可空——为空时清空全部缓存）
   */
  evictCache: (applicationId?: number) =>
    client.post('/api/v1/admin/cache/evict', undefined, {
      params: applicationId != null ? { applicationId } : undefined,
    }),
```

`services/query/useSettings.ts` 追加（仿 `useCleanupAuditLogs`）：

```ts
export function useEvictCache() {
  const { message } = App.useApp();
  return useMutation({
    mutationFn: (applicationId?: number) => settingsApi.evictCache(applicationId),
    onSuccess: () => {
      message.success('缓存已清理');
    },
  });
}
```

`pages/Settings/index.tsx`：新增"缓存清理"分组（全部清理按钮 + 按应用清理：`InputNumber` 输入应用 ID + 清理按钮），复用 `useDangerConfirm` 或 `Popconfirm` 确认（全清为危险操作）。实现者按现有页面分组形态与 i18n 文案模式落地（中文为主）。

- [ ] **Step 4: 运行测试确认通过**

Run: `cd gateway-console && npx vitest run`
Expected: PASS（新增用例 + 现有用例全绿）

- [ ] **Step 5: 提交**

```bash
git add gateway-console/src
git commit -m "feat(console): 系统设置增加缓存清理功能（全部清理 + 按应用清理）"
```

---

### Task 4: 全量回归与文档对齐

**Files:**
- Modify: `docs/api-spec.md`（缓存说明 + 管理端点）

- [ ] **Step 1: 全量回归测试**

Run: `mvn -q test`（Windows 若报 `NoDefaultCurrentDirectoryInExePath`，用 `env -u NoDefaultCurrentDirectoryInExePath mvn -q test`）
Expected: 全模块 PASS

- [ ] **Step 2: 更新 api-spec.md**（可选，若授权小节已描述配置读取则补一句缓存说明）

在「数据面授权（认证即授权）」小节补充：

```markdown
应用渠道配置读取经本地缓存（Caffeine，`ApplicationChannelConfigProvider`）承载：
TTL 60s 兜底 + 配置写操作显式失效（`ApplicationServiceImpl` evict）+ 运维手工失效
（`POST /api/v1/admin/cache/evict`，按应用或全清，仅 ADMIN），配置变更即时生效。
```

- [ ] **Step 3: 提交**

```bash
git add docs/api-spec.md
git commit -m "docs: 应用渠道配置缓存说明（Caffeine 本地缓存 + 显式失效 + 管理端点）"
```

---

## Self-Review 记录

**1. Spec 覆盖**（对照设计文档 2026-10-02-application-channel-cache-design.md + 手工失效决策）：
- `ApplicationChannelConfigProvider`（双缓存 + evict + null 语义）✔ Task 1
- Caffeine 依赖（iam pom）✔ Task 1
- `InstanceSelector` 改消费 Provider（行为等价）✔ Task 2
- 失效钩子（`ApplicationServiceImpl.delete`/`updateChannels`——实测 application_channel 写操作唯一位置）✔ Task 2
- **手工失效管理端点**（`POST /api/v1/admin/cache/evict`，按应用/全清，仅 ADMIN——未匹配授权规则默认拒绝）✔ Task 3（用户决策 B 追加）
- **前端 console 缓存清理**（Settings 页"缓存清理"分组，全部/按应用，复用 cleanupAuditLogs 模式）✔ Task 3b（用户追加）
- TTL 兜底 + recordStats + maximumSize 500 ✔ Task 1
- 全量回归 + 文档 ✔ Task 4

**2. 占位符扫描**：无 TBD/TODO；Task 1 测试的 `ApplicationChannel` setter 与 Caffeine 包名标注"实现者确认"（以实际代码/版本为准），核心断言与实现完整给出。

**3. 类型一致性**：
- `ApplicationChannelConfigProvider.findChannelIdsByApplicationId(Long)→Set<Long>` / `findPriorityMapByApplicationId(Long)→Map<Long,Integer>` / `evict(Long)` / `clearAll()` → Task 2/3 消费一致 ✔
- `InstanceSelector` 构造器依赖替换后测试 mock 类型一致 ✔
- Caffeine `Cache<K,V>` 泛型用法一致 ✔
- `CacheAdminController` 端点路径与授权默认拒绝分支匹配（`/api/v1/` 未匹配规则 → 仅 ADMIN）✔

**已知边界**：
- 控制面不缓存（`checkControl` 代码化常量，无 DB 查询）
- 分布式缓存/监控接入不在范围（单实例部署 + recordStats 预留）
- 事务回滚后缓存被清为无害（仅多一次回查，无正确性影响）
- 管理端点鉴权由拦截器链保证（controller 单测不重复验证授权）
