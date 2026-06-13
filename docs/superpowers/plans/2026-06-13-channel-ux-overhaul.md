---
change: channel-ux-overhaul
design-doc: docs/superpowers/specs/2026-06-13-channel-ux-overhaul-design.md
base-ref: 49751feb47def31363e58da26fb7ab94751eb8a6
---

# 渠道控制台 UX 整修 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (推荐) 或 superpowers:executing-plans 按任务逐项实施本计划。所有步骤使用 `- [ ]` 复选框跟踪进度。

**Goal:** 将渠道管理页面从"可用"提升到"顺手"——把六类用户每天都遇到的小摩擦（创建路径割裂 / 保存反馈缺失 / 状态语义不可见 / 测试入口三套 / 危险操作强度不齐 / 错误被静默吞掉）一次性清理完，并通过新增三个健康字段把测试结果从一次性 toast 升级为 Channel 实体可观测属性。

**Architecture:** 后端先行——数据库迁移 → JPA 实体扩展三字段 → ChannelHealthService 聚合服务 → POST /api/channels/{id}/health-check 端点 → ChannelProvisionService 内联事务扩展。前端跟进——引入测试栈（Vitest + RTL + Playwright） → 错误反馈兜底 → 状态 SSOT 整合（CHANNEL_LIFECYCLE 单一来源） → useSavePulse 脉冲反馈 → useDangerConfirm 危险确认 → 测试入口归一 + HealthDot → Wizard 内联创建供应商。所有可独立验收的批次走单独 PR，可独立回滚。

**Tech Stack:**
- 后端：Java 21 + Spring Boot 3.5 + JPA + PostgreSQL 14+/H2 + Flyway
- 前端：React + TypeScript + Vite + Ant Design + React Query
- 测试：JUnit 5 + Mockito（后端） / Vitest + @testing-library/react + Playwright（前端，本期新增）

**关键决策溯源（Design Doc §x）:**
- §1 状态 SSOT：新建 `domain/channel/lifecycle.ts`，旧 `STATE_CONFIG` / `STATE_TRANSITION_LABELS` **完全删除无别名**
- §2 useSavePulse：纯 CSS keyframes + `prefers-reduced-motion` 适配
- §3 ChannelHealthService 聚合规则：HEALTHY = 全部 PASS 且每 Key 至少 1 模型；FAILED = 全部失败；DEGRADED = 部分通过；UNKNOWN = 无 Key
- §3 PRECHECK 不持久化（在 Service 层短路）
- §5 Wizard 状态扁平化：`selectedProviderCode` 与 `inlineProviderExpanded + inlineProvider` 互斥不变量
- §6 事务性 Provision：单一 `@Transactional` 包裹 `ensureProvider` + 后续创建链
- §7 last-write-wins：不加 `@Version`，最晚 UPDATE 胜出
- §8 useDangerConfirm：通过 `Modal.useModal()` 的 `contextHolder` 注入

**TDD 总规则：** 每个任务严格遵守"先写失败测试 → 跑一遍确认失败 → 写最小实现 → 跑一遍确认通过 → 提交"五步循环。提交粒度小、频率高。

**章节并行/串行建议：**
- 第 1 章 → 第 2 章 → 第 3 章（后端串行：1 提供字段，2 提供服务，3 提供 Provision 扩展）
- 第 4 章必须先于 5/6/7/8/9 章节（前端测试栈是后续单测的前提）
- 第 5/6/7/8/9 章节相互独立，**可并行**（不同 PR / 不同 worktree）
- 第 10 章依赖第 6 章（lifecycle SSOT）与第 3 章（事务 API）
- 第 11/12 章为收尾汇总，必须在 5–10 章全部完成后

**风险提示：**
- ⚠️ **删除 STATE_CONFIG 无别名**：第 6 章必须先用 codegraph 列出所有引用点并一次性替换，再删除导出，否则编译失败
- ⚠️ **PostgreSQL/H2 兼容**：Flyway 迁移文件必须使用通用 SQL（不要使用 `IF NOT EXISTS` 在 H2 老版本上的差异行为；本仓库已使用 Flyway 标准 V 编号，需确认 H2 测试 profile 也跑同一份脚本）
- ⚠️ **AbortController 取消语义**：关闭 Drawer 时调用 `ac.abort()`，但要避免取消已完成请求的回调依然触发 setState（用 `if (!signal.aborted) ...`）
- ⚠️ **PRECHECK 不持久化**：Controller / Service 层都要校验，不能让前端 source 字段直接控制持久化逻辑（防越权）
- ⚠️ **乐观更新回滚**：`onMutate` 必须返回 `{ prev }`，`onError` 才能正确还原；`onSettled` 不要主动 invalidate（否则脉冲被打断）
- ⚠️ **i18n key 一致性**：Tooltip / 危险确认 / 错误反馈三处 key 必须最后统一审校，避免出现 `state.activeDesc` 与 `channel.state.activeDesc` 共存

---

## 文件结构总览

**后端（gateway-boot）新建/修改：**
- 新建：`src/main/resources/db/migration/V50__add_channel_health_columns.sql`
- 新建：`domain/supply/enums/ChannelHealthStatus.java`、`ChannelHealthSource.java`
- 修改：`domain/supply/entity/Channel.java`（新增 3 字段）
- 修改：`domain/supply/gateway/ChannelGateway.java`、`infrastructure/gateway/supply/ChannelGatewayImpl.java`、`ChannelRepository`
- 新建：`application/supply/ChannelHealthService.java` + DTO（`ChannelHealthResult` / `KeyMatrixRow` / `KeyTestResult`）
- 修改：`adapter/api/ChannelController.java`（新增 `/health-check` 端点 + DTO 增 3 字段）
- 修改：`application/catalog/ChannelProvisionService.java`（`ensureProvider` + `inlineProvider` 入参）
- 修改：`adapter/api/dto/...ProvisionRequest.java`（新增 `InlineProvider` record）

**前端（gateway-console）新建/修改：**
- 新建：`src/domain/channel/lifecycle.ts`
- 新建：`src/components/common/useSavePulse.ts` + `SavePulse.css`
- 新建：`src/components/common/useDangerConfirm.tsx`
- 新建：`src/components/common/HealthDot.tsx`
- 新建：`src/pages/Channels/ProviderForm.tsx`（从 ProviderCreateModal 拆分）
- 修改：`src/components/common/ChannelStateTag.tsx`（删除 STATE_CONFIG，引用新 SSOT）
- 修改：`src/utils/stateTransitions.ts`（删除 STATE_TRANSITION_LABELS）
- 修改：`src/pages/Channels/index.tsx`（移除独立"+ 新增供应商"按钮）
- 修改：`src/pages/Channels/QuickOnboardMode.tsx`（Step 0.5 + 状态扁平化）
- 修改：`src/pages/Channels/{EndpointSection,CredentialSection,ModelMappingSection,QuotaSettingsSection}.tsx`（脉冲 + 错误反馈）
- 修改：`src/pages/Channels/{ChannelCard,ChannelDetailDrawer,ConnectivityTestPanel,InlineEditableList}.tsx`
- 修改：`src/services/query/useChannels.ts`（健康字段类型）
- 修改：`src/locales/{zh-CN,en-US}/channels.json`
- 配置：`vite.config.ts`、新建 `vitest.setup.ts`、`playwright.config.ts`

---

## 第 1 章：后端——健康状态字段与持久化

**Files:**
- Create: `gateway-boot/src/main/resources/db/migration/V50__add_channel_health_columns.sql`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/domain/supply/enums/ChannelHealthStatus.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/domain/supply/enums/ChannelHealthSource.java`
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/domain/supply/entity/Channel.java`
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/domain/supply/gateway/ChannelGateway.java` 与对应 `infrastructure/gateway/supply/` 实现
- Test: `gateway-boot/src/test/java/com/codingas/gateway/domain/supply/entity/ChannelHealthFieldsTest.java`

### 任务 1.1 数据库迁移

- [x] **Step 1：写迁移脚本**

`V50__add_channel_health_columns.sql`：

```sql
-- 渠道健康状态字段（last-write-wins，无版本锁）
ALTER TABLE channels ADD COLUMN last_health_check_at TIMESTAMP NULL;
ALTER TABLE channels ADD COLUMN last_health_status VARCHAR(16) NULL;
ALTER TABLE channels ADD COLUMN last_health_source VARCHAR(16) NULL;

CREATE INDEX idx_channels_last_health_status ON channels(last_health_status);
```

- [x] **Step 2：本地启动并验证迁移成功**

> 实施备注：未启动 `spring-boot:run` 长驻进程；改由任务 1.4 的 `@DataJpaTest` 集成测试在 H2 上由 Flyway 自动应用 V50 完成等价验证。

- [x] **Step 3：提交**

```bash
git add gateway-boot/src/main/resources/db/migration/V50__add_channel_health_columns.sql
git commit -m "feat(supply): 增加 channel 健康状态字段迁移 V50"
```

### 任务 1.2 / 1.3 健康枚举 + 实体扩展

- [x] **Step 1：先写失败测试 `ChannelHealthFieldsTest`**

```java
package com.codingas.gateway.domain.supply.entity;

import com.codingas.gateway.domain.supply.enums.ChannelHealthSource;
import com.codingas.gateway.domain.supply.enums.ChannelHealthStatus;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import static org.assertj.core.api.Assertions.assertThat;

class ChannelHealthFieldsTest {

    @Test
    void 应能读写三个健康字段() {
        Channel channel = new Channel();
        Instant now = Instant.now();
        channel.setLastHealthCheckAt(now);
        channel.setLastHealthStatus(ChannelHealthStatus.HEALTHY);
        channel.setLastHealthSource(ChannelHealthSource.DRAWER);

        assertThat(channel.getLastHealthCheckAt()).isEqualTo(now);
        assertThat(channel.getLastHealthStatus()).isEqualTo(ChannelHealthStatus.HEALTHY);
        assertThat(channel.getLastHealthSource()).isEqualTo(ChannelHealthSource.DRAWER);
    }

    @Test
    void 默认值为_null() {
        Channel channel = new Channel();
        assertThat(channel.getLastHealthCheckAt()).isNull();
        assertThat(channel.getLastHealthStatus()).isNull();
        assertThat(channel.getLastHealthSource()).isNull();
    }

    @Test
    void 健康状态枚举包含四个值() {
        assertThat(ChannelHealthStatus.values())
            .containsExactlyInAnyOrder(
                ChannelHealthStatus.HEALTHY,
                ChannelHealthStatus.DEGRADED,
                ChannelHealthStatus.FAILED,
                ChannelHealthStatus.UNKNOWN);
    }

    @Test
    void 健康来源枚举包含三个值() {
        assertThat(ChannelHealthSource.values())
            .containsExactlyInAnyOrder(
                ChannelHealthSource.CARD,
                ChannelHealthSource.DRAWER,
                ChannelHealthSource.PRECHECK);
    }
}
```

- [x] **Step 2：跑测试确认失败**

```bash
./mvnw test -pl gateway-boot -Dtest=ChannelHealthFieldsTest
# Expected: 编译错误（枚举与字段不存在）
```

- [x] **Step 3：实现枚举**

`ChannelHealthStatus.java`：

```java
package com.codingas.gateway.domain.supply.enums;

/**
 * 渠道健康状态聚合枚举。
 * <ul>
 *   <li>HEALTHY: 全部 Key 通过且各自至少返回 1 个可用模型</li>
 *   <li>DEGRADED: 部分通过、部分失败</li>
 *   <li>FAILED: 全部 Key 失败或无任何可用模型</li>
 *   <li>UNKNOWN: 无 Key 或未执行过测试</li>
 * </ul>
 */
public enum ChannelHealthStatus { HEALTHY, DEGRADED, FAILED, UNKNOWN }
```

`ChannelHealthSource.java`：

```java
package com.codingas.gateway.domain.supply.enums;

/**
 * 触发健康测试的来源。仅 CARD / DRAWER 会持久化到 channel 表。
 */
public enum ChannelHealthSource { CARD, DRAWER, PRECHECK }
```

- [x] **Step 4：扩展 Channel 实体**

在 `Channel.java` 中追加 3 个字段：

```java
/** 最近一次连通性测试完成时间 */
@Column(name = "last_health_check_at")
private Instant lastHealthCheckAt;

/** 最近一次健康聚合状态 */
@Column(name = "last_health_status", length = 16)
@Enumerated(EnumType.STRING)
private ChannelHealthStatus lastHealthStatus;

/** 最近一次测试触发来源 */
@Column(name = "last_health_source", length = 16)
@Enumerated(EnumType.STRING)
private ChannelHealthSource lastHealthSource;
```

> 实施备注：根据 CLAUDE.md "领域模型纯洁性"约束，`Channel` 是领域 POJO（继承 BaseEntity，无 JPA），JPA 注解应放在 `ChannelDo` 上。本任务仅在 `Channel` 加 3 个 POJO 字段；`@Column` 与 `toEntity/toDo` 透传统一在任务 1.4 处理。

- [x] **Step 5：跑测试确认通过**

```bash
./mvnw test -pl gateway-boot -Dtest=ChannelHealthFieldsTest
# Expected: 4 tests pass
```

- [x] **Step 6：提交**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/domain/supply/enums/ChannelHealthStatus.java \
        gateway-boot/src/main/java/com/codingas/gateway/domain/supply/enums/ChannelHealthSource.java \
        gateway-boot/src/main/java/com/codingas/gateway/domain/supply/entity/Channel.java \
        gateway-boot/src/test/java/com/codingas/gateway/domain/supply/entity/ChannelHealthFieldsTest.java
git commit -m "feat(supply): 增加 Channel 健康状态字段与枚举"
```

### 任务 1.4 Repository / Gateway 透传

- [x] **Step 1：检查 Gateway 接口与实现是否需要新增方法**

通常 `findById` / `save` 已能透传（JPA 反射映射）。如有自定义 RowMapper（如 MyBatis），需补字段映射。先用 codegraph 确认：

```
mcp__codegraph__codegraph_explore({ query: "ChannelGateway ChannelGatewayImpl ChannelRepository save findById" })
```

- [x] **Step 2：写一个集成测试 `ChannelHealthRepositoryIT`**

```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ChannelHealthRepositoryIT {

    @Autowired ChannelRepository repository;

    @Test
    void 应能持久化与读回三个健康字段() {
        Channel saved = createValidChannel(); // helper：填充必填字段
        saved.setLastHealthCheckAt(Instant.parse("2026-06-13T10:00:00Z"));
        saved.setLastHealthStatus(ChannelHealthStatus.DEGRADED);
        saved.setLastHealthSource(ChannelHealthSource.CARD);
        Channel persisted = repository.save(saved);

        Channel reloaded = repository.findById(persisted.getId()).orElseThrow();
        assertThat(reloaded.getLastHealthStatus()).isEqualTo(ChannelHealthStatus.DEGRADED);
        assertThat(reloaded.getLastHealthSource()).isEqualTo(ChannelHealthSource.CARD);
        assertThat(reloaded.getLastHealthCheckAt()).isEqualTo(Instant.parse("2026-06-13T10:00:00Z"));
    }
}
```

- [x] **Step 3：跑测试**

```bash
./mvnw test -pl gateway-boot -Dtest=ChannelHealthRepositoryIT
# Expected: 通过
```

- [x] **Step 4：提交**

```bash
git commit -am "test(supply): 健康字段 Repository 集成测试"
```

**验收标准（第 1 章整体）：**
- ✅ Flyway 自动跑通 V50
- ✅ Channel 实体可读写 3 字段
- ✅ 枚举值集合完整且不可扩展（switch 默认抛 IllegalArgumentException 即可）
- ✅ 至少 4 个单测 + 1 个 Repository IT 全绿

---

## 第 2 章：后端——连通性测试 API 与聚合

**Files:**
- Create: `application/supply/ChannelHealthService.java`
- Create: `application/supply/dto/ChannelHealthResult.java`、`KeyMatrixRow.java`、`KeyTestResult.java`
- Modify: `adapter/api/ChannelController.java`
- Create / Modify: `adapter/api/dto/ChannelHealthCheckRequest.java`、`ChannelDTO.java`（增 3 字段）
- Test: `application/supply/ChannelHealthServiceTest.java`、`adapter/api/ChannelHealthControllerIT.java`

### 任务 2.1 ChannelHealthService（聚合规则）

- [x] **Step 1：先写失败测试 `ChannelHealthServiceTest`（覆盖聚合 4 分支 + PRECHECK 不持久化 + 持久化失败兜底）**

```java
@ExtendWith(MockitoExtension.class)
class ChannelHealthServiceTest {

    @Mock ChannelGateway channelGateway;
    @Mock ChannelCredentialGateway credentialGateway;
    @Mock ConnectivityTester connectivityTester;
    Executor executor = Runnable::run; // 同步执行简化测试

    ChannelHealthService service;

    @BeforeEach
    void setUp() {
        service = new ChannelHealthService(channelGateway, credentialGateway, connectivityTester, executor);
    }

    @Test
    void 全部_Key_通过聚合为_HEALTHY() { /* mock 2 keys, 全 PASS, 各 1 模型 */ }

    @Test
    void 部分通过聚合为_DEGRADED() { /* mock 2 keys：1 PASS 1 FAIL */ }

    @Test
    void 全部失败聚合为_FAILED() { /* mock 2 keys 全 FAIL */ }

    @Test
    void 无_Key_聚合为_UNKNOWN() { /* credentialGateway 返回空列表 */ }

    @Test
    void PRECHECK_来源不写入持久化字段() {
        // ... 执行 service.check(channelId, PRECHECK)
        verify(channelGateway, never()).save(any());
    }

    @Test
    void 持久化失败时主流程仍返回结果() {
        when(channelGateway.save(any())).thenThrow(new RuntimeException("DB down"));
        ChannelHealthResult result = service.check(channelId, ChannelHealthSource.DRAWER);
        assertThat(result.aggregateStatus()).isEqualTo(ChannelHealthStatus.HEALTHY);
        // 不抛出异常
    }

    @Test
    void aggregate_静态方法五分支() {
        assertThat(ChannelHealthService.aggregate(List.of())).isEqualTo(ChannelHealthStatus.UNKNOWN);
        // ... 其余分支
    }
}
```

- [x] **Step 2：跑测试确认失败（编译错误）**

- [x] **Step 3：实现 `ChannelHealthService`**

参考 design doc §3 的代码骨架，关键要点：
- `@Transactional`，但 `persistHealth` 内部 try-catch 不让异常冒出
- `aggregate` 提取为 `static` 方法便于单测
- `source == PRECHECK` 时短路，**完全跳过 `persistHealth`**
- 用 `CompletableFuture.allOf(...).orTimeout(30s)` 做总超时；单 Key `orTimeout(5s)` 后 `exceptionally` 转 `KeyTestResult.timeout(...)`
- 通过构造器注入 `Executor healthCheckExecutor`，避免阻塞 Tomcat 主线程池

DTO（record 形式）：

```java
public record ChannelHealthResult(
    Long channelId,
    ChannelHealthStatus aggregateStatus,
    Instant startedAt,
    Instant finishedAt,
    List<KeyMatrixRow> matrix
) {}

public record KeyMatrixRow(
    Long credentialId,
    String keyMasked,
    AuthStatus auth,
    String authError,
    List<String> availableModels,
    Long latencyMs
) {}
```

- [x] **Step 4：跑测试确认全绿**

```bash
./mvnw test -pl gateway-boot -Dtest=ChannelHealthServiceTest
# Expected: 7 tests pass
```

- [x] **Step 5：提交**

```bash
git commit -am "feat(supply): 实现 ChannelHealthService 聚合规则与持久化"
```

### 任务 2.2 健康检查 API 端点

- [ ] **Step 1：写失败的端点集成测试 `ChannelHealthControllerIT`**

```java
@SpringBootTest
@AutoConfigureMockMvc
class ChannelHealthControllerIT {

    @Autowired MockMvc mockMvc;
    @MockBean ConnectivityTester connectivityTester; // mock 出站请求

    @Test
    void DRAWER_来源应返回矩阵并写入持久化字段() throws Exception {
        // 准备：插入 channel + 2 credentials；mock connectivityTester 返回 PASS
        mockMvc.perform(post("/api/channels/{id}/health-check", channelId)
                .contentType(APPLICATION_JSON)
                .content("""{ "source": "DRAWER" }"""))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.aggregateStatus").value("HEALTHY"))
            .andExpect(jsonPath("$.matrix.length()").value(2));

        // 断言持久化已发生
        Channel reloaded = channelRepository.findById(channelId).orElseThrow();
        assertThat(reloaded.getLastHealthStatus()).isEqualTo(ChannelHealthStatus.HEALTHY);
        assertThat(reloaded.getLastHealthSource()).isEqualTo(ChannelHealthSource.DRAWER);
    }

    @Test
    void PRECHECK_来源不写入持久化字段() throws Exception { /* ... */ }

    @Test
    void 零_Key_返回_UNKNOWN() throws Exception { /* ... */ }

    @Test
    void 缺少_source_字段返回_400() throws Exception { /* ... */ }

    @Test
    void 不存在的_channelId_返回_404() throws Exception { /* ... */ }

    @Test
    void 并发触发_last_write_wins() throws Exception {
        // 并发 2 次请求，断言 lastHealthCheckAt 等于较晚那次
    }
}
```

- [ ] **Step 2：跑测试确认失败（404 端点不存在）**

- [ ] **Step 3：实现 Controller 端点**

`ChannelController.java` 新增方法：

```java
/**
 * 触发渠道连通性测试，按聚合规则写入健康状态。
 *
 * @param id 渠道 ID
 * @param request 测试参数（含触发来源）
 * @return 测试矩阵 + 聚合状态
 */
@PostMapping("/{id}/health-check")
public ChannelHealthResult healthCheck(
        @PathVariable Long id,
        @RequestBody @Valid ChannelHealthCheckRequest request) {
    return channelHealthService.check(id, request.source());
}
```

`ChannelHealthCheckRequest.java`：

```java
public record ChannelHealthCheckRequest(
    @NotNull ChannelHealthSource source
) {}
```

- [ ] **Step 4：跑测试确认全绿**

- [ ] **Step 5：提交**

```bash
git commit -am "feat(supply): POST /api/channels/{id}/health-check 端点"
```

### 任务 2.3 列表 / 详情 DTO 增 3 字段（向后兼容）

- [ ] **Step 1：写失败测试**

`ChannelControllerListIT.java`：

```java
@Test
void GET_channels_响应包含三个健康字段() throws Exception {
    mockMvc.perform(get("/api/channels"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].lastHealthCheckAt").exists())
        .andExpect(jsonPath("$[0].lastHealthStatus").exists())
        .andExpect(jsonPath("$[0].lastHealthSource").exists());
}
```

- [ ] **Step 2：跑测试确认失败**

- [ ] **Step 3：在 `ChannelDTO`（或 `ChannelResponse`）追加 3 字段并在 Mapper 中映射**

```java
private Instant lastHealthCheckAt;
private ChannelHealthStatus lastHealthStatus;
private ChannelHealthSource lastHealthSource;
```

- [ ] **Step 4：跑测试确认通过**

- [ ] **Step 5：提交**

```bash
git commit -am "feat(supply): channel 列表/详情响应附加健康字段"
```

**验收标准（第 2 章）：**
- ✅ POST /health-check 端到端跑通 6 个 IT
- ✅ 聚合规则单测覆盖 4 分支 + 持久化跳过 / 失败兜底
- ✅ GET /api/channels 响应字段向后兼容（旧字段未删）

---

## 第 3 章：后端——事务性 Provision 扩展

**Files:**
- Modify: `application/catalog/ChannelProvisionService.java`
- Modify / Create: `adapter/api/dto/ProvisionRequest.java`（含内嵌 record `InlineProvider`）
- Test: `application/catalog/ChannelProvisionServiceInlineProviderTest.java`、`application/catalog/ChannelProvisionTransactionalIT.java`

### 任务 3.1 / 3.2 入参扩展 + ensureProvider 三路径

- [ ] **Step 1：先写失败的单元测试 `ChannelProvisionServiceInlineProviderTest`**

```java
@ExtendWith(MockitoExtension.class)
class ChannelProvisionServiceInlineProviderTest {

    @Test
    void providerCode不存在_inlineProvider非空_使用inline创建Provider() {
        // mock providerGateway.findByCode returns empty
        // 调用 provisionFromPlan(planCode, request with inlineProvider)
        // verify providerGateway.save with name = inline.name(), description = inline.description()
    }

    @Test
    void providerCode不存在_inlineProvider为空_使用旧默认级联创建() {
        // verify providerGateway.save with name = providerCode（默认行为）
    }

    @Test
    void providerCode已存在_inlineProvider被忽略() {
        // mock providerGateway.findByCode returns existing
        // verify providerGateway.save NEVER called
    }

    @Test
    void inlineProvider_code与planCode的providerCode不一致_抛出_INLINE_PROVIDER_CODE_MISMATCH() {
        assertThatThrownBy(() -> service.provisionFromPlan(planCode, requestWithMismatchedInline))
            .isInstanceOf(CatalogException.class)
            .hasMessageContaining("INLINE_PROVIDER_CODE_MISMATCH");
    }
}
```

- [ ] **Step 2：跑测试确认失败**

- [ ] **Step 3：扩展 `ProvisionRequest` 与 `ChannelProvisionService`**

`ProvisionRequest.java` 追加：

```java
private InlineProvider inlineProvider;

public record InlineProvider(
    String code,
    String name,
    String description,
    String websiteUrl,
    String apiDocUrl
) {}
```

`ChannelProvisionService.java` 重构 `ensureProvider`（参考 design doc §6）：

```java
private Provider ensureProvider(String providerCode, ProvisionRequest.InlineProvider inline) {
    return providerGateway.findByCode(providerCode).orElseGet(() -> {
        Provider p = new Provider();
        p.setCode(providerCode);
        p.setPriority(100);
        if (inline != null) {
            p.setName(Optional.ofNullable(inline.name()).orElse(providerCode));
            p.setDescription(inline.description());
            p.setWebsiteUrl(inline.websiteUrl());
            p.setApiDocUrl(inline.apiDocUrl());
        } else {
            p.setName(providerCode);
        }
        Provider saved = providerGateway.save(p);
        log.info("自动创建供应商: code={}, inline={}, id={}", providerCode, inline != null, saved.getId());
        return saved;
    });
}
```

`provisionFromPlan` 入口校验 code 一致性，详见 design doc。

- [ ] **Step 4：跑测试全绿，提交**

```bash
git commit -am "feat(catalog): provisionFromPlan 支持 inlineProvider 入参"
```

### 任务 3.3 / 3.4 事务回滚集成测试

- [ ] **Step 1：写失败的事务测试 `ChannelProvisionTransactionalIT`**

```java
@SpringBootTest
@Transactional(propagation = Propagation.NOT_SUPPORTED) // 禁止外层事务，让 service 内部事务真实生效
class ChannelProvisionTransactionalIT {

    @Autowired ChannelProvisionService service;
    @Autowired ProviderRepository providerRepository;
    @MockBean ChannelEndpointGateway channelEndpointGateway; // 故意 mock 出错

    @Test
    void 内联创建过程中端点保存失败_整体回滚不留孤儿_Provider() {
        when(channelEndpointGateway.save(any()))
            .thenThrow(new RuntimeException("simulated failure"));

        ProvisionRequest req = new ProvisionRequest(...);
        req.setInlineProvider(new InlineProvider("brand-new-provider", "Brand New", null, null, null));

        assertThatThrownBy(() -> service.provisionFromPlan(planCode, req))
            .isInstanceOf(RuntimeException.class);

        // 关键断言：Provider 未被持久化
        assertThat(providerRepository.findByCode("brand-new-provider")).isEmpty();
    }

    @Test
    void 级联Model失败也整体回滚() { /* 类似 */ }

    @Test
    void providerCode已存在时inlineProvider被忽略_正常路径() { /* ... */ }
}
```

- [ ] **Step 2：跑测试确认失败 → 调整 `provisionFromPlan` 的 `@Transactional` 到方法级（含 `ensureProvider`）→ 再跑测试通过**

- [ ] **Step 3：提交**

```bash
git commit -am "test(catalog): 验证 inlineProvider 事务性回滚"
```

**验收标准（第 3 章）：**
- ✅ `ensureProvider` 三路径单测全绿
- ✅ 事务回滚 IT 验证孤儿 Provider 不会出现
- ✅ providerCode 已存在时 inlineProvider 被忽略

---

## 第 4 章：前端——测试栈引入

**前置条件：** 第 5–10 章的所有单测和组件测试都依赖此章节。

**Files:**
- Modify: `gateway-console/package.json`
- Modify: `gateway-console/vite.config.ts`
- Create: `gateway-console/vitest.setup.ts`
- Create: `gateway-console/playwright.config.ts`
- Create: `gateway-console/src/components/common/__tests__/ChannelStateTag.smoke.test.tsx`
- Create: `gateway-console/e2e/smoke.spec.ts`

### 任务 4.1 安装依赖

- [x] **Step 1：在 `gateway-console` 目录执行**

```bash
cd gateway-console
pnpm add -D vitest @testing-library/react @testing-library/user-event @testing-library/jest-dom jsdom @vitest/ui @playwright/test
pnpm exec playwright install --with-deps chromium
```

- [x] **Step 2：提交 lock 文件变更**

```bash
git add package.json pnpm-lock.yaml
git commit -m "chore(console): 引入 vitest + RTL + playwright 测试栈"
```

### 任务 4.2 配置 Vitest

- [x] **Step 1：修改 `vite.config.ts`**，添加 test 段：

```ts
/// <reference types="vitest" />
export default defineConfig({
  // ... existing
  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: ['./vitest.setup.ts'],
    css: true,
    include: ['src/**/*.{test,spec}.{ts,tsx}'],
  },
});
```

- [x] **Step 2：新建 `vitest.setup.ts`**

```ts
import '@testing-library/jest-dom/vitest';
import { afterEach } from 'vitest';
import { cleanup } from '@testing-library/react';

afterEach(() => cleanup());
```

- [x] **Step 3：新建 `playwright.config.ts`**

```ts
import { defineConfig } from '@playwright/test';

export default defineConfig({
  testDir: './e2e',
  use: {
    baseURL: process.env.E2E_BASE_URL ?? 'http://localhost:5173',
    trace: 'on-first-retry',
  },
  webServer: {
    command: 'pnpm dev',
    url: 'http://localhost:5173',
    reuseExistingServer: !process.env.CI,
  },
});
```

- [x] **Step 4：在 `package.json` 增加 scripts**

```json
{
  "scripts": {
    "test": "vitest run",
    "test:watch": "vitest",
    "test:e2e": "playwright test"
  }
}
```

### 任务 4.3 / 4.4 / 4.5 Smoke 测试验证

- [x] **Step 1：写组件 smoke `ChannelStateTag.smoke.test.tsx`**

```tsx
import { render, screen } from '@testing-library/react';
import { ChannelStateTag } from '@/components/common/ChannelStateTag';
import { describe, it, expect } from 'vitest';

describe('ChannelStateTag smoke', () => {
  it('应渲染 ACTIVE 状态文案', () => {
    render(<ChannelStateTag state="ACTIVE" />);
    expect(screen.getByText(/active/i)).toBeInTheDocument();
  });
});
```

- [x] **Step 2：写 E2E smoke `e2e/smoke.spec.ts`**

```ts
import { test, expect } from '@playwright/test';

test('渠道页面应能加载', async ({ page }) => {
  await page.goto('/channels');
  await expect(page.getByRole('button', { name: /新增渠道/ })).toBeVisible();
});
```

- [x] **Step 3：跑两个 smoke 测试**

```bash
pnpm test
pnpm test:e2e
# Expected: 全绿（注意 E2E 需要后端在 baseURL 上可用）
```

- [x] **Step 4：提交**

```bash
git add gateway-console/{vite.config.ts,vitest.setup.ts,playwright.config.ts,package.json}
git add gateway-console/src/components/common/__tests__/ChannelStateTag.smoke.test.tsx
git add gateway-console/e2e/smoke.spec.ts
git commit -m "chore(console): 配置 vitest/playwright + 验证 smoke"
```

**验收标准（第 4 章）：**
- ✅ `pnpm test` 跑通 1 个 vitest smoke
- ✅ `pnpm test:e2e` 跑通 1 个 playwright smoke
- ✅ CI / 本地都可重复跑

---

## 第 5 章：前端——错误反馈兜底（最低风险批）

**Files:**
- Modify: `gateway-console/src/pages/Channels/EndpointSection.tsx`
- Modify: `gateway-console/src/pages/Channels/CredentialSection.tsx`
- Modify: `gateway-console/src/pages/Channels/ModelMappingSection.tsx`
- Modify: `gateway-console/src/pages/Channels/QuotaSettingsSection.tsx`
- Test: `gateway-console/src/pages/Channels/__tests__/error-feedback.test.tsx`

### 任务 5.1 审计

- [x] **Step 1：用 codegraph + grep 列出所有空 catch / 仅注释 catch**

```bash
cd gateway-console
grep -rn "catch" src/pages/Channels/ | grep -v "//.*catch" | head -50
# 重点位置：EndpointSection.tsx:62-63, 89-90 等（参见 design）
```

记录在 PR 描述中作为变更清单。

### 任务 5.2 / 5.3 写失败测试 + 改造各 Section

- [ ] **Step 1：写失败测试 `error-feedback.test.tsx`**

```tsx
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { EndpointSection } from '../EndpointSection';
import { vi } from 'vitest';
import { server, rest } from '@/test/msw';

describe('EndpointSection 错误反馈', () => {
  it('保存失败时应弹 message.error 而非静默吞错', async () => {
    server.use(rest.put('/api/endpoints/:id', (_, res, ctx) =>
      res(ctx.status(500), ctx.json({ message: 'simulated' }))));

    render(<EndpointSection channelId={1} />);
    await userEvent.type(screen.getByRole('textbox', { name: /url/ }), 'http://x');
    await userEvent.tab(); // 触发保存

    await waitFor(() => {
      expect(screen.getByText(/保存失败/)).toBeInTheDocument();
    });
  });
});
```

- [ ] **Step 2：跑测试确认失败**

- [ ] **Step 3：改造 `EndpointSection.tsx` 等 4 个 Section**

把每个 catch 块从：

```ts
} catch {
  // 校验失败
}
```

改为：

```ts
} catch (err) {
  const msg = extractErrorMessage(err); // 抽取通用工具
  message.error(t('common.saveFailed', { reason: msg }));
  // 注：如有乐观更新，在 onError 内也调用 queryClient.setQueryData(qk, ctx.prev)
}
```

每个文件单独提交：

```bash
git add gateway-console/src/pages/Channels/EndpointSection.tsx
git commit -m "fix(channels): EndpointSection 补齐错误反馈"
# ... 其余 3 个 Section 同样
```

- [ ] **Step 4：跑测试全绿**

### 任务 5.4 单元测试覆盖

- [ ] **Step 1：每个 Section 至少 1 个错误路径测试**

- [ ] **Step 2：`pnpm test` 全绿后提交**

**验收标准（第 5 章）：**
- ✅ 所有 mutation catch 块都有 `message.error` 或行内反馈
- ✅ 4 个 Section 各有至少 1 个失败路径测试
- ✅ 不变量：`pages/Channels/` 下不存在仅注释或空的 catch 块

---

## 第 6 章：前端——状态语义可视化与 SSOT 整合

**Files:**
- Create: `gateway-console/src/domain/channel/lifecycle.ts`
- Create: `gateway-console/src/domain/channel/__tests__/lifecycle.test.ts`
- Modify: `gateway-console/src/components/common/ChannelStateTag.tsx`
- Modify: `gateway-console/src/utils/stateTransitions.ts`
- Modify: `gateway-console/src/pages/Channels/ChannelCard.tsx`（DEPRECATED 副标题 + RETIRED 视觉）
- Modify: `gateway-console/src/locales/zh-CN/channels.json`、`en-US/channels.json`
- Test: `gateway-console/src/components/common/__tests__/ChannelStateTag.test.tsx`

### 任务 6.1 codegraph 列出引用点

- [ ] **Step 1：执行 codegraph_explore**

```
mcp__codegraph__codegraph_explore({ query: "STATE_CONFIG STATE_TRANSITION_LABELS ChannelStateTag stateTransitions" })
```

- [ ] **Step 2：把所有引用文件列在 PR 描述里，准备一次性替换**

### 任务 6.2 新建 lifecycle.ts + i18n key

- [ ] **Step 1：先写失败测试 `lifecycle.test.ts`**

```ts
import { describe, it, expect } from 'vitest';
import {
  CHANNEL_LIFECYCLE,
  isRoutable, isBilling, allowedTransitions, canTransitionTo,
  buildStateTooltip,
} from '../lifecycle';

describe('CHANNEL_LIFECYCLE', () => {
  it('五条状态的字段不变性', () => {
    expect(Object.keys(CHANNEL_LIFECYCLE)).toEqual(['PENDING','ACTIVE','SUSPENDED','DEPRECATED','RETIRED']);
    expect(CHANNEL_LIFECYCLE.ACTIVE.isRoutable).toBe(true);
    expect(CHANNEL_LIFECYCLE.ACTIVE.isBilling).toBe(true);
    expect(CHANNEL_LIFECYCLE.DEPRECATED.isRoutable).toBe(true); // 关键：DEPRECATED 仍参与流量
    expect(CHANNEL_LIFECYCLE.SUSPENDED.isRoutable).toBe(false);
    expect(CHANNEL_LIFECYCLE.RETIRED.nextStates).toEqual([]);
    expect(CHANNEL_LIFECYCLE.RETIRED.visualStyle).toBe('strikethrough');
    expect(CHANNEL_LIFECYCLE.PENDING.color).toBe('#d48806'); // 加深后保证 4.5:1
  });

  it('selector helpers', () => {
    expect(isRoutable('ACTIVE')).toBe(true);
    expect(isBilling('SUSPENDED')).toBe(false);
    expect(allowedTransitions('ACTIVE')).toEqual(['SUSPENDED', 'DEPRECATED']);
    expect(canTransitionTo('PENDING', 'ACTIVE')).toBe(true);
    expect(canTransitionTo('RETIRED', 'ACTIVE')).toBe(false);
  });

  it('buildStateTooltip 注入 i18n', () => {
    const t = (k: string) => k;
    expect(buildStateTooltip('ACTIVE', t)).toContain('channel.state.activeDesc');
    expect(buildStateTooltip('ACTIVE', t)).toContain('channel.state.tooltipRoutable');
  });
});
```

- [ ] **Step 2：跑测试确认失败**

- [ ] **Step 3：实现 `lifecycle.ts`**（直接照搬 design doc §1 代码）

- [ ] **Step 4：在 `locales/zh-CN/channels.json` 与 `en-US/channels.json` 增 key**

新增 key 列表：
- `channel.state.pending` / `pendingDesc`、`active` / `activeDesc`、`suspended` / `suspendedDesc`、`deprecated` / `deprecatedDesc`、`retired` / `retiredDesc`
- `channel.state.tooltipRoutable`、`tooltipBilling`、`tooltipNext`、`tooltipTerminal`
- `channel.state.deprecatedSubtitle`（DEPRECATED 卡片副标题"仍参与流量分配，但已标记为不推荐"）

- [ ] **Step 5：跑测试全绿**

### 任务 6.3 替换全部引用并删除旧导出

- [ ] **Step 1：批量替换**（按任务 6.1 列出的文件清单，一次性替换 → 编译确认）

```ts
// 旧
import { STATE_CONFIG } from '@/components/common/ChannelStateTag';
const color = STATE_CONFIG[state].color;
// 新
import { CHANNEL_LIFECYCLE } from '@/domain/channel/lifecycle';
const color = CHANNEL_LIFECYCLE[state].color;
```

- [ ] **Step 2：从 `ChannelStateTag.tsx` 删除 `STATE_CONFIG` 导出；从 `stateTransitions.ts` 删除 `STATE_TRANSITION_LABELS` 导出**

⚠️ **不保留向后兼容别名**——design doc §1 明确决策。如有遗漏引用，必须立即编译失败发现并修复。

- [ ] **Step 3：`pnpm build` 确认无 TS 编译错误**

- [ ] **Step 4：提交**

```bash
git commit -am "refactor(channels): 状态 SSOT 整合至 CHANNEL_LIFECYCLE"
```

### 任务 6.4 / 6.5 ChannelStateTag 增 Tooltip + PENDING 加深

- [ ] **Step 1：先写失败测试 `ChannelStateTag.test.tsx`**

```tsx
describe('ChannelStateTag', () => {
  it('hover ACTIVE 应显示流量/计费/可转换至说明', async () => {
    render(<ChannelStateTag state="ACTIVE" />);
    await userEvent.hover(screen.getByText(/active/i));
    await waitFor(() => {
      expect(screen.getByRole('tooltip')).toHaveTextContent(/流量.*是/);
      expect(screen.getByRole('tooltip')).toHaveTextContent(/计费.*是/);
      expect(screen.getByRole('tooltip')).toHaveTextContent(/SUSPENDED/);
    });
  });

  it('RETIRED 应显示终止状态文案', async () => { /* ... */ });
});
```

- [ ] **Step 2：跑测试确认失败**

- [ ] **Step 3：在 `ChannelStateTag.tsx` 包一层 `<Tooltip>`，content 调用 `buildStateTooltip(state, t)`**

```tsx
import { CHANNEL_LIFECYCLE, buildStateTooltip } from '@/domain/channel/lifecycle';

export function ChannelStateTag({ state }: { state: ChannelState }) {
  const meta = CHANNEL_LIFECYCLE[state];
  const { t } = useTranslation();
  return (
    <Tooltip title={<pre>{buildStateTooltip(state, t)}</pre>}>
      <Tag color={meta.tagColor}>{t(meta.label)}</Tag>
    </Tooltip>
  );
}
```

- [ ] **Step 4：跑测试全绿，提交**

### 任务 6.6 RETIRED 卡片视觉重设 + 6.7 DEPRECATED 副标题

- [ ] **Step 1：写测试**

```tsx
it('RETIRED 渠道卡片名应有 line-through 且无 opacity 整体降透', () => {
  render(<ChannelCard channel={{ ...mockChannel, state: 'RETIRED' }} />);
  const name = screen.getByText(mockChannel.name);
  expect(name).toHaveStyle({ textDecoration: 'line-through' });
  const card = name.closest('[data-testid="channel-card"]');
  expect(card).not.toHaveStyle({ opacity: '0.5' });
});

it('DEPRECATED 卡片应显示副标题"仍参与流量分配"', () => {
  render(<ChannelCard channel={{ ...mockChannel, state: 'DEPRECATED' }} />);
  expect(screen.getByText(/仍参与流量分配/)).toBeInTheDocument();
});
```

- [ ] **Step 2：跑测试失败**

- [ ] **Step 3：改造 `ChannelCard.tsx`**

- 移除 `opacity: 0.5`，改用 `meta.visualStyle === 'strikethrough'` 时给名称加 `text-decoration: line-through; color: #8c8c8c`
- DEPRECATED：在 channel name 下方加 `<small>{t('channel.state.deprecatedSubtitle')}</small>`

- [ ] **Step 4：跑测试全绿，提交**

### 任务 6.8 单元测试 + 组件测试汇总

- [ ] 全章测试 `pnpm test src/domain/channel src/components/common src/pages/Channels/__tests__/ChannelCard` 全绿后提交。

**验收标准（第 6 章）：**
- ✅ `STATE_CONFIG` 与 `STATE_TRANSITION_LABELS` 在仓库中已无任何引用
- ✅ ChannelStateTag 全部 5 状态都展示 Tooltip
- ✅ DEPRECATED / RETIRED 卡片视觉符合 spec
- ✅ PENDING 颜色为 `#d48806`
- ✅ lifecycle 单测 + ChannelStateTag 组件测试全绿

---

## 第 7 章：前端——保存反馈脉冲

**Files:**
- Create: `gateway-console/src/components/common/useSavePulse.ts`
- Create: `gateway-console/src/components/common/SavePulse.css`
- Modify: 4 个 Section（Endpoint / Credential / ModelMapping / QuotaSettings）
- Test: `gateway-console/src/components/common/__tests__/useSavePulse.test.tsx`

### 任务 7.1 / 7.2 useSavePulse + CSS

- [ ] **Step 1：先写失败测试 `useSavePulse.test.tsx`**

```tsx
import { renderHook, act } from '@testing-library/react';
import { useSavePulse } from '../useSavePulse';
import { vi } from 'vitest';

beforeEach(() => vi.useFakeTimers());
afterEach(() => vi.useRealTimers());

describe('useSavePulse', () => {
  it('triggerSuccess 后 className=save-pulse-success，3s 后自动归位 idle', () => {
    const { result } = renderHook(() => useSavePulse());
    act(() => result.current.triggerSuccess());
    expect(result.current.className).toBe('save-pulse-success');
    act(() => vi.advanceTimersByTime(3001));
    expect(result.current.state).toBe('idle');
    expect(result.current.className).toBe('');
  });

  it('triggerError 应保持 error 状态不自动清除', () => {
    const { result } = renderHook(() => useSavePulse());
    act(() => result.current.triggerError('boom'));
    act(() => vi.advanceTimersByTime(10000));
    expect(result.current.state).toBe('error');
    expect(result.current.errorMsg).toBe('boom');
  });

  it('triggerError 后再 triggerSuccess 应清空 errorMsg', () => { /* ... */ });

  it('卸载时 cleanup 定时器', () => {
    const { result, unmount } = renderHook(() => useSavePulse());
    act(() => result.current.triggerSuccess());
    unmount();
    act(() => vi.advanceTimersByTime(3001)); // 不应抛出
  });
});
```

- [ ] **Step 2：跑测试失败**

- [ ] **Step 3：实现 `useSavePulse.ts`**（照搬 design doc §2 代码骨架）

- [ ] **Step 4：实现 `SavePulse.css`**（照搬 design doc §2 keyframes，含 `prefers-reduced-motion` 适配）

- [ ] **Step 5：跑测试全绿，提交**

```bash
git commit -am "feat(common): useSavePulse hook + CSS keyframes"
```

### 任务 7.3 / 7.4 / 7.5 接入 Endpoint / Credential / ModelMapping

- [ ] **Step 1：写失败的组件测试 `EndpointSection.pulse.test.tsx`**

```tsx
it('保存成功后行尾应出现 ✓ 已保存', async () => { /* mock 200 → 断言 save-tip-ok */ });
it('保存失败应出现 ✗ 错误且行加红框', async () => { /* mock 500 → 断言 save-tip-err + className 含 save-pulse-error */ });
it('乐观更新失败应回滚到上一个值', async () => { /* mock 500 → 断言旧值仍在 */ });
```

- [ ] **Step 2：在每个 Section 接入 `useSavePulse`**（参考 design doc §2 调用示例）：
  - `onMutate` 备份 + 乐观更新
  - `onSuccess` 调 `pulse.triggerSuccess()`
  - `onError` 回滚 + `pulse.triggerError(extractMsg(err))`
  - JSX 在行内挂 `ref={pulse.ref} className={pulse.className}` + 后置反馈节点

- [ ] **Step 3：每个 Section 单独提交**

### 任务 7.6 QuotaSettingsSection 批量保存模式

- [ ] **Step 1：在编辑模式提交按钮上挂 useSavePulse，成功后对编辑容器触发脉冲（不需要乐观更新）**

- [ ] **Step 2：测试 + 提交**

### 任务 7.7 测试汇总

- [ ] 全章 `pnpm test` 全绿。

**验收标准（第 7 章）：**
- ✅ 4 个 Section 成功路径都有可见脉冲 + ✓ 已保存
- ✅ 4 个 Section 失败路径都有红框 + ✗ 错误 + 字段值回滚
- ✅ `prefers-reduced-motion` 适配可见（CSS 退化为软变色）

---

## 第 8 章：前端——危险操作确认升级

**Files:**
- Create: `gateway-console/src/components/common/useDangerConfirm.tsx`
- Modify: `gateway-console/src/pages/Channels/InlineEditableList.tsx`
- Modify: 暂停/删除调用点（CredentialSection、EndpointSection、ModelMappingSection、ChannelCard、ChannelDetailDrawer）
- Test: `gateway-console/src/components/common/__tests__/useDangerConfirm.test.tsx`

### 任务 8.1 useDangerConfirm hook

- [ ] **Step 1：先写失败测试**

```tsx
describe('useDangerConfirm', () => {
  it('confirm() 应弹出 Modal.confirm 含 danger okType', async () => {
    const { result } = renderHook(() => useDangerConfirm());
    const onOk = vi.fn();
    act(() => result.current.confirm({
      titleKey: 'credential.deleteTitle',
      descriptionKey: 'credential.deleteDescription',
      onOk,
    }));
    // contextHolder 必须 render，否则 modal 不出现
    expect(screen.getByText(/credential.deleteTitle/)).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: /删除/ }));
    expect(onOk).toHaveBeenCalled();
  });

  it('onOk 异步 reject 时不关闭 modal', async () => { /* ... */ });
});
```

- [ ] **Step 2：跑测试失败**

- [ ] **Step 3：实现 `useDangerConfirm.tsx`**（照搬 design doc §8 代码）

- [ ] **Step 4：跑测试全绿，提交**

### 任务 8.2 InlineEditableList 删除回调签名扩展

- [ ] **Step 1：把 `onDelete` 签名从 `(id) => void` 扩展为 `(id, ctx?) => void`，组件本身不再硬编码 Popconfirm；调用方注入 `useDangerConfirm`**

- [ ] **Step 2：写测试 + 提交**

### 任务 8.3 暂停操作 Popconfirm

- [ ] **Step 1：在所有"暂停"按钮（→ SUSPENDED）入口包 `<Popconfirm>`，文案：「暂停后该渠道不再分配流量，但保留配置」**

- [ ] **Step 2：测试 + 提交**

```bash
git commit -am "feat(channels): 暂停操作增加 Popconfirm 二次确认"
```

### 任务 8.4 / 8.5 / 8.6 / 8.7 删除类升级到 useDangerConfirm

每个删除场景写一个组件测试，断言 Modal.confirm 出现并含正确 description；然后改造调用点：

- [ ] **8.4 删除 API Key**：description = `t('credential.deleteDescription', { keyMasked })`，包含「删除后无法恢复，使用此 Key 的请求将立即失败」
- [ ] **8.5 删除端点**：description 包含「删除该端点后，路由到 baseUrl=… 的流量将立即失败」
- [ ] **8.6 删除模型映射**：description 包含「删除后，模型 ID `{modelId}` 不再被路由到此渠道」
- [ ] **8.7 删除整个渠道 / 转 RETIRED**：description 与上述风格对齐

每项任务都遵循 TDD：先失败测试 → 实现 → 全绿 → 提交。

### 任务 8.8 hook 单测汇总

- [ ] 全章 `pnpm test src/components/common/__tests__/useDangerConfirm` 全绿。

**验收标准（第 8 章）：**
- ✅ 暂停操作有 Popconfirm（轻量）
- ✅ 4 类删除操作均使用 Modal.confirm + okType=danger + description
- ✅ description 文案明确包含"删除后无法恢复"+具体业务影响

---

## 第 9 章：前端——测试入口归一与健康指示

**Files:**
- Modify: `gateway-console/src/pages/Channels/ChannelCard.tsx`（闪电图标行为 + HealthDot）
- Create: `gateway-console/src/components/common/HealthDot.tsx`
- Create: `gateway-console/src/components/common/__tests__/HealthDot.test.tsx`
- Modify: `gateway-console/src/pages/Channels/ChannelDetailDrawer.tsx`（矩阵 Table）
- Modify: `gateway-console/src/pages/Channels/ConnectivityTestPanel.tsx`（改名 + 文案 + source=PRECHECK）
- Modify: `gateway-console/src/pages/Channels/ProviderGroupHeader.tsx`（N/M 健康聚合）
- Modify: `gateway-console/src/services/query/useChannels.ts`（健康字段类型）
- Create: `gateway-console/e2e/health-check-matrix.spec.ts` (S5)

### 任务 9.1 卡片闪电图标行为改造

- [ ] **Step 1：写失败测试**

```tsx
it('点击闪电图标应打开详情抽屉并跳到 Credentials Tab，"测试全部"按钮短暂高亮', async () => {
  // mock useState/router；点击 → 断言 onOpenDrawer 被调用 with tab=credentials, highlightTestAll=true
});
```

- [ ] **Step 2：实现：闪电图标点击 → `openDrawer(channelId, { tab: 'credentials', highlightTestAll: true })`，800ms 后 highlight 清除**

- [ ] **Step 3：跑测试 + 提交**

### 任务 9.2 / 9.3 HealthDot 组件 + 嵌入卡片

- [ ] **Step 1：先写失败测试 `HealthDot.test.tsx`**

```tsx
describe('HealthDot', () => {
  it.each([
    ['HEALTHY', '#52c41a'],
    ['DEGRADED', '#faad14'],
    ['FAILED', '#ff4d4f'],
  ])('%s 状态应填充色 %s', (status, color) => {
    render(<HealthDot status={status as any} />);
    expect(screen.getByTestId('health-dot')).toHaveStyle({ backgroundColor: color });
  });

  it('UNKNOWN 应空心（背景透明 + border）', () => {
    render(<HealthDot status="UNKNOWN" />);
    const dot = screen.getByTestId('health-dot');
    expect(dot).toHaveStyle({ backgroundColor: 'transparent' });
  });

  it('null 应当作 UNKNOWN', () => { /* ... */ });

  it('hover 应显示 lastCheckAt + source', async () => {
    render(<HealthDot status="HEALTHY" lastCheckAt="2026-06-13T10:00:00Z" source="DRAWER" />);
    await userEvent.hover(screen.getByTestId('health-dot'));
    await waitFor(() => {
      expect(screen.getByText(/最后一次测试/)).toBeInTheDocument();
      expect(screen.getByText(/详情/)).toBeInTheDocument(); // source=DRAWER 翻译
    });
  });
});
```

- [ ] **Step 2：实现 `HealthDot.tsx`**（照搬 design doc §4 代码）

- [ ] **Step 3：在 `ChannelCard.tsx` 状态 Tag 右侧 6px 处嵌入**

```tsx
<Space size={6}>
  <ChannelStateTag state={channel.state} />
  <HealthDot
    status={channel.lastHealthStatus}
    lastCheckAt={channel.lastHealthCheckAt}
    source={channel.lastHealthSource}
  />
</Space>
```

- [ ] **Step 4：跑测试 + 提交**

### 任务 9.4 ProviderGroupHeader N/M 健康聚合

- [ ] **Step 1：写测试断言"3/5 健康"小字渲染**
- [ ] **Step 2：实现：在分组下统计 `lastHealthStatus === 'HEALTHY'` 的数量 / 总数**
- [ ] **Step 3：测试 + 提交**

### 任务 9.5 / 9.6 详情抽屉矩阵 Table + AbortController

- [ ] **Step 1：写失败测试**

```tsx
it('点击"测试全部"应渲染矩阵 Table，行=Key 列=认证/可用模型/延迟/时间戳', async () => {
  server.use(rest.post('/api/channels/1/health-check', (req, res, ctx) => res(ctx.json({
    aggregateStatus: 'DEGRADED',
    matrix: [
      { credentialId: 1, keyMasked: 'sk-***wxyz', auth: 'PASS', availableModels: ['gpt-4'], latencyMs: 230 },
      { credentialId: 2, keyMasked: 'sk-***abcd', auth: 'FAIL', authError: '401', latencyMs: null },
    ],
  }))));

  render(<ChannelDetailDrawer channelId={1} />);
  await userEvent.click(screen.getByRole('button', { name: /测试全部/ }));
  await waitFor(() => {
    expect(screen.getByText('sk-***wxyz')).toBeInTheDocument();
    expect(screen.getByText('230ms')).toBeInTheDocument();
    expect(screen.getByText('401')).toBeInTheDocument();
  });
});

it('关闭抽屉应中止进行中的请求', async () => {
  // 用 vi.fn() 装饰 AbortController.abort 验证调用
});
```

- [ ] **Step 2：实现矩阵 Table + AbortController：**

```tsx
const acRef = useRef<AbortController | null>(null);

const triggerTest = async () => {
  acRef.current = new AbortController();
  try {
    const res = await axios.post(`/api/channels/${id}/health-check`,
      { source: 'DRAWER' },
      { signal: acRef.current.signal, timeout: 35000 });
    if (acRef.current.signal.aborted) return; // 防越权 setState
    setMatrix(res.data.matrix);
    queryClient.invalidateQueries({ queryKey: ['channels'] }); // 刷新列表健康状态
  } catch (err) {
    if (axios.isCancel(err)) return;
    message.error(extractMsg(err));
  }
};

useEffect(() => () => acRef.current?.abort(), []); // 卸载/关闭时中止
```

- [ ] **Step 3：测试 + 提交**

### 任务 9.7 ConnectivityTestPanel 改名为"预检工具"

- [ ] **Step 1：写测试**

```tsx
it('预检工具 UI 文案应明确"创建渠道前测试 baseUrl + Key 的可用性"', () => { /* ... */ });
it('预检工具应使用 source=PRECHECK 调用 health-check API', async () => { /* ... */ });
```

- [ ] **Step 2：把组件 title / heading 改为"预检工具"，加副标题"用于在创建渠道前测试 baseUrl + Key 的可用性"**

- [ ] **Step 3：调用 `/api/channels/{id}/health-check` 时 `source: 'PRECHECK'`，注意此 API 形态需要已有 channelId——若预检工具尚无 channel，则改用 design doc 7 隐含的"独立测试调用"路径，仍透传 source=PRECHECK 让后端跳过持久化**

- [ ] **Step 4：测试 + 提交**

### 任务 9.8 useChannels 类型扩展

- [ ] **Step 1：在 `Channel` TS 类型 + `useChannels` 查询返回里增加 3 字段**
- [ ] **Step 2：编译通过 + 提交**

### 任务 9.9 单测 + Playwright e2e

- [ ] **Step 1：HealthDot 组件测试 ≥ 4 断言（4 状态色 + Popover）已在 9.2 完成**

- [ ] **Step 2：写 `e2e/health-check-matrix.spec.ts` (S5)**

```ts
test('卡片闪电图标 → 跳转抽屉 → 测试矩阵 → 关闭后卡片显示健康指示点', async ({ page }) => {
  await page.goto('/channels');
  await page.getByTestId('channel-card-test-icon').first().click();
  // 跳转抽屉 + 高亮"测试全部"
  await expect(page.getByRole('button', { name: /测试全部/ })).toBeVisible();
  await page.getByRole('button', { name: /测试全部/ }).click();
  // 矩阵 Table 出现
  await expect(page.getByRole('columnheader', { name: /认证/ })).toBeVisible();
  await page.getByRole('button', { name: /关闭/ }).click();
  // 卡片健康指示点出现
  await expect(page.getByTestId('health-dot').first()).toBeVisible();
});
```

- [ ] **Step 3：`pnpm test:e2e` 跑通 + 提交**

**验收标准（第 9 章）：**
- ✅ 渠道卡片闪电图标不再就地弹 toast
- ✅ 详情抽屉是唯一执行入口 + 矩阵 Table 展现
- ✅ 预检工具明确"创建渠道前"定位 + source=PRECHECK
- ✅ 列表卡片显示 HealthDot
- ✅ S5 e2e 跑通

---

## 第 10 章：前端——创建入口合并

**Files:**
- Create: `gateway-console/src/pages/Channels/ProviderForm.tsx`
- Modify: `gateway-console/src/pages/Channels/ProviderCreateModal.tsx`（包装 ProviderForm）
- Modify: `gateway-console/src/pages/Channels/QuickOnboardMode.tsx`（Step 0.5 + 状态扁平化）
- Modify: `gateway-console/src/pages/Channels/index.tsx`（移除独立"+ 新增供应商"按钮）
- Test: `gateway-console/src/pages/Channels/__tests__/QuickOnboardMode.test.tsx`
- Create: `gateway-console/e2e/onboard-inline-provider.spec.ts` (S1)

### 任务 10.1 ProviderForm 拆分

- [ ] **Step 1：先写测试 `ProviderForm.test.tsx`**

```tsx
it('受控组件：value/onChange 双向绑定', () => { /* ... */ });
it('校验：code 必填 + 与 expectedProviderCode 不一致时报错', () => { /* ... */ });
```

- [ ] **Step 2：把 `ProviderCreateModal` 的表单部分抽到 `ProviderForm.tsx`，作为受控组件（接受 `value`、`onChange`、`onSubmit`、`expectedProviderCode?`）**

- [ ] **Step 3：`ProviderCreateModal.tsx` 改为简单包装 `<Modal><ProviderForm /></Modal>` 以保持原 API（仍被批量导入复用）**

- [ ] **Step 4：测试 + 提交**

### 任务 10.2 QuickOnboardMode 状态扁平化

- [ ] **Step 1：把现有 step state 扩展为：**

```ts
interface QuickOnboardState {
  step: 0 | 1 | 2 | 3;
  selectedProviderCode: string | null;
  inlineProviderExpanded: boolean;
  inlineProvider: InlineProviderForm | null;
  endpoints: EndpointForm[];
  selectedModels: string[];
  apiKeysRaw: string;
}
```

- [ ] **Step 2：写不变量测试**

```tsx
describe('QuickOnboardMode 状态机', () => {
  it('选择已有 provider 时应清空 inline 字段', () => { /* ... */ });
  it('展开内联创建时应清空 selectedProviderCode', () => { /* ... */ });
  it('Step 0 校验：必须二选一', () => { /* ... */ });
  it('提交时 inlineProvider 字段仅当走内联路径时存在', () => { /* ... */ });
});
```

- [ ] **Step 3：实现切换逻辑：用户切换分支时清空对方**

- [ ] **Step 4：测试 + 提交**

### 任务 10.3 / 10.4 / 10.5 Step 0 增"+ 新建供应商"链接

- [ ] **Step 1：在 Step 0 的供应商下拉旁加 "+ 新建供应商" 链接，点击展开 Step 0.5（同 Drawer 内）渲染 `ProviderForm`，`expectedProviderCode={planCatalog.providerCode}`**

- [ ] **Step 2：Step 0 "下一步"校验：`selectedProviderCode != null` 或 (`inlineProviderExpanded && validate(inlineProvider) == ok`)**

- [ ] **Step 3：最终提交时 payload 含 `inlineProvider`（仅当走内联路径）**

```ts
const payload: ProvisionFromPlanRequest = {
  endpoints: state.endpoints,
  apiKeys: parseApiKeys(state.apiKeysRaw),
  inlineProvider: state.inlineProvider ?? undefined,
};
await axios.post(`/api/v1/provision/from-plan/${planCode}`, payload);
```

- [ ] **Step 4：测试 + 提交**

### 任务 10.6 主页面移除独立"+ 新增供应商"按钮

- [ ] **Step 1：写失败测试**

```tsx
it('/channels 主页面不应有独立的"+ 新增供应商"按钮', () => {
  render(<ChannelsPage />);
  expect(screen.queryByRole('button', { name: /新增供应商/ })).not.toBeInTheDocument();
});
```

- [ ] **Step 2：从 `index.tsx` 删除按钮 + 相关 state + handlers**

- [ ] **Step 3：批量导入路径仍保留 ProviderCreateModal 调用（其他批量导入入口）**

- [ ] **Step 4：测试 + 提交**

### 任务 10.7 e2e S1

- [ ] **Step 1：写 `e2e/onboard-inline-provider.spec.ts`**

```ts
test('S1：内联创建供应商 → 接续创建渠道', async ({ page }) => {
  await page.goto('/channels');
  // 主页面无独立"+ 新增供应商"按钮
  await expect(page.getByRole('button', { name: /^\+ 新增供应商/ })).toHaveCount(0);
  await page.getByRole('button', { name: /\+ 新增渠道/ }).click();
  await page.getByRole('link', { name: /\+ 新建供应商/ }).click();
  await page.getByLabel('供应商代码').fill('e2e-provider-' + Date.now());
  await page.getByLabel('供应商名称').fill('E2E 供应商');
  await page.getByRole('button', { name: /下一步/ }).click();
  // 进入 Step 1 端点配置
  await expect(page.getByLabel(/baseUrl/)).toBeVisible();
});
```

- [ ] **Step 2：跑测试 + 提交**

**验收标准（第 10 章）：**
- ✅ 主页面无独立"+ 新增供应商"按钮（仅"+ 新增渠道"+"批量导入"+"批量导出"）
- ✅ Step 0 同时支持选已有 / 内联创建，互斥不变量
- ✅ 提交时 payload 走 `provisionFromPlan` 含 inlineProvider 字段
- ✅ S1 e2e 跑通

---

## 第 11 章：国际化与文案统一

**Files:**
- Modify: `gateway-console/src/locales/zh-CN/*.json`、`en-US/*.json`

### 任务 11.1 整理新增 / 修改 i18n key

- [ ] **Step 1：列出本期所有 i18n key（按章节分组，附上下文）**

汇总至少包含：
- `channel.state.{state}` / `{state}Desc`（5 状态 × 2）
- `channel.state.tooltipRoutable` / `tooltipBilling` / `tooltipNext` / `tooltipTerminal`
- `channel.state.deprecatedSubtitle`
- `save.success` / `save.failed`、`common.saveFailed`
- `credential.deleteTitle` / `deleteDescription`
- `endpoint.deleteTitle` / `deleteDescription`
- `model.deleteTitle` / `deleteDescription`
- `channel.deleteTitle` / `deleteDescription`、`channel.suspendTitle` / `suspendDescription`
- `health.lastCheckAt` / `health.notTested` / `health.source.{CARD,DRAWER,PRECHECK}` / `health.summary`（N/M）
- `precheck.title` / `precheck.subtitle`
- `provider.formTitle` / `provider.codeMismatch`

### 任务 11.2 文案审校

- [ ] **Step 1：与产品对齐文案，重点：**
  - 危险确认 description 必须含"删除后无法恢复"+ 业务影响
  - 状态 Tooltip 简洁、可逐行扫描
  - 保存反馈短小
  - 错误反馈不暴露技术细节（堆栈/SQL）

### 任务 11.3 codegraph 验证孤立 key

- [ ] **Step 1：运行**

```
mcp__codegraph__codegraph_explore({ query: "i18n locales channel.state channel.health" })
```

- [ ] **Step 2：用 `grep -r "channel\\.state\\." gateway-console/src/` 比对 locales 文件，确认无未使用 key、无未翻译 key**

- [ ] **Step 3：提交 i18n 整理**

```bash
git commit -am "i18n(channels): 整理本期 channel-ux-overhaul 全量 key"
```

**验收标准（第 11 章）：**
- ✅ 中英文 locales 文件 key 完全对齐
- ✅ 无孤立 key（grep 验证）
- ✅ 文案与产品一致

---

## 第 12 章：联调与回归

**Files:**
- Test: `gateway-console/e2e/delete-key-confirm.spec.ts` (S6)
- Doc: `openspec/changes/channel-ux-overhaul/.comet/verify/report.md`（验证报告草稿）

### 任务 12.1 前后端联调 9 条端到端验收场景

- [ ] **Step 1：列出 9 条 scenario（来自 spec）**

| ID | Scenario | 自动化 |
|---|---|---|
| S1 | 内联创建供应商 → 接续渠道 | Playwright e2e（任务 10.7） |
| S2 | 状态 Tooltip / RETIRED line-through / DEPRECATED 副标题 | 视觉走查（12.2） |
| S3 | 即时保存脉冲 + 失败回滚 | 组件测试（任务 7） |
| S4 | 配额批量保存反馈 | 组件测试（任务 7.6） |
| S5 | 闪电图标 → 抽屉 → 矩阵 → 健康点 | Playwright e2e（任务 9.9） |
| S6 | 删除 Key 弹 Modal.confirm | Playwright e2e（任务 12.3） |
| S7 | 暂停弹 Popconfirm | 组件测试（任务 8） |
| S8 | mutation 失败必有 message.error | 组件测试（任务 5） |
| S9 | 预检工具不写入持久化字段 | 后端 IT（任务 2.5） + 前端组件测试（任务 9.7） |

- [ ] **Step 2：每条 scenario 在 PR 合并后跑通一次，记录到验证报告**

### 任务 12.2 视觉走查

- [ ] **Step 1：在 1280×800 / 1440×900 两种分辨率下走查：**
  - 状态色对比度 ≥ 4.5:1（用 axe DevTools / WAVE）
  - 脉冲动画时长 800ms 不刺眼
  - 矩阵 Table 列宽合理、不溢出
  - HealthDot 与状态 Tag 间距 6px
  - **决策**：ProviderGroupHeader 的"N/M 健康"是否冗余 → 视情况保留或砍掉

- [ ] **Step 2：截图存到 verify report**

### 任务 12.3 Playwright e2e/delete-key-confirm.spec.ts (S6)

- [ ] **Step 1：写**

```ts
test('S6：删除 API Key 弹出 Modal.confirm 含"删除后无法恢复"', async ({ page }) => {
  await page.goto('/channels');
  await page.getByTestId('channel-card').first().click(); // 打开抽屉
  await page.getByRole('tab', { name: /credentials/i }).click();
  await page.getByRole('button', { name: /删除/ }).first().click();
  // Modal.confirm 出现
  await expect(page.getByRole('dialog')).toContainText(/删除后无法恢复/);
  await expect(page.getByRole('dialog')).toContainText(/请求将立即失败/);
});
```

- [ ] **Step 2：跑通 + 提交**

### 任务 12.4 回归测试

- [ ] **Step 1：手工验证旧路径不受影响：**
  - 批量导入：仍可走原 ProviderCreateModal 链路（拆分后仍兼容）
  - 模板创建（TemplateLibrary）
  - 批量导出
  - 旧的 `provisionFromPlan` 调用方（不传 inlineProvider）仍正常

- [ ] **Step 2：跑全量后端测试 + 全量前端测试**

```bash
./mvnw test -pl gateway-boot
cd gateway-console && pnpm test && pnpm test:e2e
```

### 任务 12.5 验证报告草稿

- [ ] **Step 1：在 `openspec/changes/channel-ux-overhaul/.comet/verify/report.md` 列出每条 Requirement → 对应测试**

格式：
```
| Requirement | Scenario | 自动化测试 / 验证方式 | 结果 |
|---|---|---|---|
| 渠道创建入口单一闭合 | 选择已有供应商 | e2e/onboard-inline-provider.spec.ts | ✅ |
| 渠道创建入口单一闭合 | 内联创建供应商 | e2e/onboard-inline-provider.spec.ts | ✅ |
| ... | ... | ... | ... |
```

- [ ] **Step 2：草稿 review 后进入 verify 阶段**

**验收标准（第 12 章）：**
- ✅ 9 条 scenario 全部跑通
- ✅ 视觉走查通过
- ✅ S5 + S6 + S1 三个 e2e 在 CI 上跑通
- ✅ 旧路径回归无破坏
- ✅ 验证报告草稿已写入

---

## Self-Review

### Spec 覆盖检查

| Spec Requirement | 任务 |
|---|---|
| channel-console-ux: 渠道创建入口单一闭合 | 第 10 章 |
| channel-console-ux: 字段保存反馈可视化 | 第 7 章 |
| channel-console-ux: 状态语义可见 | 第 6 章 |
| channel-console-ux: 测试入口归一 | 第 9 章 |
| channel-console-ux: 危险操作确认强度对齐 | 第 8 章 |
| channel-console-ux: 错误反馈不变量 | 第 5 章 |
| channel-health-tracking: Channel 实体扩展健康字段 | 第 1 章 |
| channel-health-tracking: 测试结果聚合与持久化 | 第 2 章 |
| channel-health-tracking: 列表响应包含健康状态 | 任务 2.4 + 9.8 |
| channel-provision: 内联创建事务性 | 第 3 章 |
| channel-provision: 现有"从套餐创建"修改 | 第 3 章 |

✅ 全部覆盖。

### 类型 / 命名一致性

- `ChannelHealthStatus` / `ChannelHealthSource`：贯穿后端枚举 / DTO / 前端 TS 类型一致
- `useSavePulse` 返回 `{ ref, state, errorMsg, className, triggerSuccess, triggerError }`：第 7 章所有调用点字段名一致
- `useDangerConfirm` 返回 `{ confirm, contextHolder }`：第 8 章一致
- `CHANNEL_LIFECYCLE` / `LifecycleMeta` / `buildStateTooltip`：第 6 章一致
- `inlineProvider` 字段命名：前后端 / DTO / record 一致

### 风险再确认

- ⚠️ 第 6 章删除 STATE_CONFIG 后，需立刻全量编译验证（`pnpm build`）
- ⚠️ 第 2 章 PRECHECK 不持久化的判断要在 Service 层做，Controller 不可信任前端字段
- ⚠️ 第 3 章 `@Transactional` 必须落在 `provisionFromPlan` 方法上，不要落在 `ensureProvider`（私有方法 self-invocation 不会触发代理）
- ⚠️ 第 9 章 AbortController 取消后必须用 `if (signal.aborted) return` 防越权 setState

---

## 执行交付

**计划已保存到 `docs/superpowers/plans/2026-06-13-channel-ux-overhaul.md`。两种执行模式：**

**1. Subagent-Driven（推荐）** - 父代理逐任务派发新 subagent，任务间 review，迭代快，适合本期 50+ 子任务的体量

**2. Inline Execution** - 在当前会话直接执行，按章节批量推进 + 检查点 review

**推荐路径：** 先做后端三章（1/2/3）一个 subagent 跑完；再做第 4 章测试栈；之后 5–10 章可分多 subagent 并行（不同 worktree）；最后 11/12 章串行收尾。
