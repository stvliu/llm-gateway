# bug 单与技术债处理 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 修复 DoD 补测暴露的生产缺陷（ProviderServiceImpl.create 缺 code）+ 处理技术债（限流器 Clock 注入、分页断言弱、覆盖率统计脚本）+ provider 剩余补测 + minor 清理。

**Architecture:** `ProviderServiceImpl.create` 补 `provider.setCode(request.getCode())`（`ProviderCreateRequest` 已有 code 字段，@NotBlank 品牌标识，明确遗漏——toResponse 用 code 填 providerId）；`InMemoryTokenBucketRateLimiter` 注入 `Clock`（构造器默认 `Clock.systemUTC()`，可注入固定 Clock 消除秒边界 flake）；`TokenLimitServiceImplTest` 分页断言补具体条目；provider 剩余真实逻辑类（BuiltinDataLoader/ChannelHealthService/ChannelCredentialServiceImpl/ConnectivityTesterImpl）补测；覆盖率 DoD 口径沉淀为可审计脚本。**用户 2026-08-24 授权修改实现**（bug 修复，区别于补测分支的"行为不变"约束）。

**Tech Stack:** Java 21、JUnit 5 + Mockito + AssertJ、JaCoCo

## Global Constraints

- 全量 `./mvnw clean install` 每任务末尾必须绿（含测试）
- 每任务独立提交，commit message 中文
- **bug 修复（授权改实现）**：只改本计划明确的目标（ProviderServiceImpl.create code、限流器 Clock），不得引入其他行为变更
- **code 修复**：`ProviderServiceImpl.create` 加 `provider.setCode(request.getCode());`（ProviderCreateRequest.code 是 @NotBlank 品牌标识，全局唯一）；`ProviderServiceImplTest` 补断言（create 后 code 传播、toResponse providerId=code）
- **限流器 Clock**：`InMemoryTokenBucketRateLimiter` 加 `Clock` 字段（`@RequiredArgsConstructor` 或构造器注入，默认 `Clock.systemUTC()` 保持现有行为）；`System.currentTimeMillis()` 两处（41/74 行）改 `clock.millis()`；测试改造：秒边界测试（tryAcquire_insufficientTokens_rejects 等）用 `Clock.fixed(...)` 确定性消除 flake
- **分页断言**：`TokenLimitServiceImplTest.query_pagination_skipsAndLimits` 补 `getItems().get(0).getId()` 断言（page=2 应返回第 2 条）
- **minor 清理**：`UserServiceImplTest` 删未使用 `java.time.Instant` import；`PlanCatalogServiceImplTest.keywordPriority` 拆 3 个独立用例；`UpstreamClientRegistryImplTest` 的 `connectivityResult_successFactory/failureFactory` 调整（测 ConnectivityTestResult 工厂与 registry 职责错位——移出或改名）
- **provider 剩余补测**（B4）：`BuiltinDataLoader`（56 行 miss）、`ChannelHealthService`、`ChannelCredentialServiceImpl`、`ConnectivityTesterImpl`（mock 依赖 + 分支覆盖）；目标 provider 覆盖率 ≥93%
- **统计脚本**（B5）：`gateway-coverage/coverage-summary.py`（或 shell）——固化 DoD 口径（包 `com.codingas.gateway.<域>` 含 -data、排除 dataobject/dto/enums/entity/config/autoconfigure + `*Do/*Request/*Response`、行覆盖率），读取 `gateway-coverage/target/site/jacoco-aggregate/jacoco.csv` 输出各域覆盖率表 + DoD 判定
- alert 域不补（纯枚举口径，已在 DoD 补测计划明确）

---

## Task 1: 基线验证

**Files:**
- 无

**Interfaces:**
- Consumes: 无
- Produces: 基线全绿

- [ ] **Step 1: 全量构建 + 测试**

```bash
cd /e/workspace/llm-gateway
./mvnw clean install
```

Expected: BUILD SUCCESS，全部测试绿（master `ceae3499`；1296 测试）。

---

## Task 2: bug 修复——ProviderServiceImpl.create 缺 code

**Files:**
- Modify: `gateway-provider/provider/src/main/java/com/codingas/gateway/provider/service/ProviderServiceImpl.java`（create 方法）
- Modify: `gateway-provider/provider/src/test/java/com/codingas/gateway/provider/service/ProviderServiceImplTest.java`（补断言）

**Interfaces:**
- Consumes: `ProviderCreateRequest.code`（已有字段）
- Produces: create 后 Provider.code = request.code；toResponse providerId = code

- [ ] **Step 1: 修复 create**

`ProviderServiceImpl.create` 在 `provider.setName(...)` 后加：

```java
provider.setCode(request.getCode());
```

（`ProviderCreateRequest.code` 是 @NotBlank 品牌标识——create 明确遗漏设置。）

- [ ] **Step 2: 补测试断言**

`ProviderServiceImplTest` 的 create 用例补断言：
- `gateway.save` 捕获的 Provider 的 `code` 等于 request.code（ArgumentCaptor）
- 返回 response 的 `providerId` 等于 code（toResponse:252 `setProviderId(provider.getCode())`）

- [ ] **Step 3: 运行 + 全量构建**

```bash
./mvnw test -pl gateway-provider/provider
./mvnw clean install
```

Expected: 全部绿（create 测试含 code 断言）。

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "fix: ProviderServiceImpl.create 设置 code（品牌标识，补测暴露的遗漏）+ 断言补强
Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

## Task 3: 限流器 Clock 注入 + 分页断言 + minor 清理

**Files:**
- Modify: `gateway-security/security/src/main/java/com/codingas/gateway/security/threat/InMemoryTokenBucketRateLimiter.java`（Clock 注入）
- Modify: `gateway-security/security/src/test/java/com/codingas/gateway/security/threat/InMemoryTokenBucketRateLimiterTest.java`（固定 Clock）
- Modify: `gateway-usage/usage/src/test/java/com/codingas/gateway/usage/tokenlimit/TokenLimitServiceImplTest.java`（分页断言）
- Modify: `gateway-iam/iam/src/test/java/com/codingas/gateway/iam/service/UserServiceImplTest.java`（删 import）
- Modify: `gateway-provider/provider/src/test/java/com/codingas/gateway/provider/service/PlanCatalogServiceImplTest.java`（keywordPriority 拆分）
- Modify: `gateway-protocol/protocol/src/test/java/com/codingas/gateway/protocol/transport/UpstreamClientRegistryImplTest.java`（connectivityResult 测试调整）

**Interfaces:**
- Consumes: 无
- Produces: 限流器可注入 Clock；分页断言强化；minor 清理

- [ ] **Step 1: 限流器 Clock 注入**

`InMemoryTokenBucketRateLimiter`：
- 加 `private final Clock clock;` 字段
- 构造器：`public InMemoryTokenBucketRateLimiter(RateLimitProperties properties, Clock clock)`（或 @RequiredArgsConstructor + 默认——以实际类结构为准，保留现有无参/属性构造器兼容调用方）
- 两处 `System.currentTimeMillis() / 1000` 改 `clock.millis() / 1000`

**注意**：RateLimitDomainService/其他调用方如何 new InMemoryTokenBucketRateLimiter——若构造器变更需同步调用方（保持 Spring 装配正常）。

- [ ] **Step 2: 测试改造（固定 Clock）**

`InMemoryTokenBucketRateLimiterTest`：秒边界相关测试（tryAcquire_insufficientTokens_rejects、tryAcquire_exactConsumption_thenRejects、getStatus_afterConsumption_returnsRemaining）用 `Clock.fixed(Instant.ofEpochSecond(N), ZoneOffset.UTC)` 确定性——消除跨秒 flake。

- [ ] **Step 3: 分页断言 + minor 清理**

- `TokenLimitServiceImplTest.query_pagination_skipsAndLimits` 补 `getItems().get(0).getId()` 断言
- `UserServiceImplTest` 删 `java.time.Instant` import
- `PlanCatalogServiceImplTest.keywordPriority` 拆 3 用例（keyword 优先 / capability 次之 / 空则 findAll）
- `UpstreamClientRegistryImplTest` 的 `connectivityResult_successFactory/failureFactory`：若测的是 ConnectivityTestResult 工厂（与 registry 职责错位），移至 ConnectivityTestResult 自己的测试或改名明确

- [ ] **Step 4: 运行 + 全量构建 + Commit**

```bash
./mvnw clean install
git add -A
git commit -m "refactor: 限流器注入 Clock 消除秒边界 flake + 分页断言强化 + minor 清理
Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

## Task 4: provider 剩余补测

**Files:**
- Create/扩展（gateway-provider/provider/src/test/.../provider/）：
  - `impl/BuiltinDataLoaderTest.java`（新建，56 行 miss——mock Gateway 验证种子装载逻辑）
  - `service/ChannelHealthServiceTest.java`（新建）
  - `service/ChannelCredentialServiceImplTest.java`（扩展，补未覆盖分支）
  - `upstream/ConnectivityTesterImplTest.java`（新建，mock UpstreamClientRegistry）

**Interfaces:**
- Consumes: 各实现 + 依赖 Gateway
- Produces: provider 覆盖率 ≥93%（拉离 90% 警戒线）

- [ ] **Step 1-3: 逐类补测 + 运行 + 覆盖率验证**

mock 依赖 + 分支覆盖（BuiltinDataLoader 的初始化/幂等/异常；ChannelHealthService 的健康检查聚合；ChannelCredentialServiceImpl 的加解密分支；ConnectivityTesterImpl 的测试委托）。统计 provider 覆盖率（目标 ≥93%）。

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "test: provider 剩余逻辑类补测（BuiltinDataLoader/ChannelHealthService/ChannelCredential/ConnectivityTester，拉离 DoD 警戒线）
Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

## Task 5: 覆盖率统计脚本 + 全量回归

**Files:**
- Create: `gateway-coverage/coverage-summary.py`（统计脚本）

**Interfaces:**
- Consumes: `gateway-coverage/target/site/jacoco-aggregate/jacoco.csv`
- Produces: 可审计的覆盖率统计脚本（固化 DoD 口径）

- [ ] **Step 1: 写统计脚本**

`gateway-coverage/coverage-summary.py`：读取 jacoco.csv，按 DoD 口径（包 `com.codingas.gateway.<域>` 含 -data、排除 dataobject/dto/enums/entity/config/autoconfigure + `*Do/*Request/*Response`）统计各域行覆盖率，输出表格 + DoD 判定（≥90% 达标）。

```python
#!/usr/bin/env python3
"""DoD 覆盖率统计（行覆盖率，含 -data 模块，排除模型/枚举/配置）"""
import csv, sys
from collections import defaultdict
CSV_PATH = sys.argv[1] if len(sys.argv) > 1 else 'gateway-coverage/target/site/jacoco-aggregate/jacoco.csv'
EXCLUDE_PKG = ('dataobject', '/dto', 'enums', 'entity', 'config', 'autoconfigure')
EXCLUDE_CLS = ('Do', 'Request', 'Response')
def is_core(pkg, cls):
    if any(x in pkg.lower() for x in EXCLUDE_PKG): return False
    if cls.endswith(EXCLUDE_CLS): return False
    return True
doms = defaultdict(lambda: [0, 0])
with open(CSV_PATH) as f:
    for r in csv.DictReader(f):
        if not is_core(r['PACKAGE'], r['CLASS']): continue
        dom = r['PACKAGE'].split('.')[3]
        doms[dom][0] += int(r['LINE_COVERED']); doms[dom][1] += int(r['LINE_MISSED'])
print(f"{'域':<12}{'覆盖/总':<12}{'覆盖率':<10}{'DoD(≥90%)'}")
for dom in sorted(doms):
    c, m = doms[dom]; t = c + m
    pct = c / t * 100 if t else 0
    print(f"{dom:<12}{c}/{t:<10}{pct:.2f}%{'':<6}{'✓' if pct >= 90 else '✗'}")
```

- [ ] **Step 2: 全量回归 + 脚本验证**

```bash
./mvnw clean install
python gateway-coverage/coverage-summary.py
```

Expected: BUILD SUCCESS；脚本输出各域覆盖率表（对照 DoD 判定）。

- [ ] **Step 3: Commit**

```bash
git add -A
git commit -m "chore: 覆盖率 DoD 统计脚本沉淀（固化口径可审计，技术债清理）
Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

## Self-Review 记录

**Spec 覆盖对照**：
- bug 单（ProviderServiceImpl.create code）→ Task 2
- 限流器 Clock 注入 → Task 3
- 分页断言 → Task 3
- minor 清理 → Task 3
- provider 剩余补测 → Task 4
- 统计脚本 → Task 5

**Placeholder 扫描**：统计脚本完整代码；各任务目标类/修改点明确；无 TBD。

**Type/命名一致性**：
- `ProviderCreateRequest.code`（已有）→ `Provider.setCode(request.getCode())` ✓
- `InMemoryTokenBucketRateLimiter` Clock 字段 + `clock.millis()` ✓
- 测试类名与目标类对应 ✓

**风险**：
- 限流器构造器变更影响调用方（RateLimitDomainService）→ Task 3 Step 1 同步调用方
- Clock 注入行为不变（默认 systemUTC）→ 验证
- bug 修复改实现 → 用户已授权（区别于补测分支的行为不变）
