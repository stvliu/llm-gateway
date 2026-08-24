# 覆盖率补测至 DoD（核心 ≥90%）实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 按域补齐核心逻辑类单元测试，将各域核心模块行覆盖率提升至 DoD（核心 ≥90%），以 `gateway-coverage/target/site/jacoco-aggregate/jacoco.csv` 为验证依据。

**Architecture:** 逐域补测（mock Gateway/Repository/依赖 + 状态与行为断言），聚焦**有逻辑的类**（门面服务、调用器、适配器、策略、限流器、事件监听器、工具类）；纯 POJO 模型（@DomainEntity/record/枚举）不补测（其 getter/setter 无测试价值，属 jacoco excludes 之外的口径噪音，见风险）。每任务构建绿 + 覆盖率提升验证。

**Tech Stack:** Java 21、JUnit 5 + Mockito + AssertJ、JaCoCo（gateway-coverage 聚合）

## Global Constraints

- 全量 `./mvnw clean install` 每任务末尾必须绿（含测试）
- 每任务独立提交，commit message 中文
- 行为不变：只加测试，不改业务逻辑（若测试暴露实现 bug，记录并暂停该处——不顺手改实现）
- **DoD 验证**：`gateway-coverage/target/site/jacoco-aggregate/jacoco.csv` 按域（包 `com.codingas.gateway.<域>`，排除 dataobject/dto/enums/entity/config/autoconfigure + `*Do/*Request/*Response`）行覆盖率
- **目标类清单**（2026-08-24 聚合报告缺口，按域分批）：

  | 任务 | 域（当前覆盖率） | 目标类 |
  |---|---|---|
  | T2 | provider（50.1%） | PlanCatalogServiceImpl、ChannelProvisionService、ProviderServiceImpl、ChannelServiceImpl、ModelInstanceServiceImpl（5 个门面服务，mock 各 Gateway） |
  | T3 | proxy（60.8%） | ModelExperienceService、KeyFailoverInvoker、ChatDispatchServiceImpl、ErrorClassifier |
  | T4 | protocol（80.3%）+ iam（82.1%） | OpenAI/Anthropic/GeminiProtocolAdapter、OpenAI/AnthropicProtocolValidator、OpenAI/AnthropicUpstreamClient（SSE 回调）；UserServiceImpl、Aes256EncryptionService、UserApiKeyServiceImpl |
  | T5 | usage（67.4%）+ security（44.6%）+ resilience（83.2%） | TokenLimitServiceImpl、TokenUsageEventListener；InMemoryTokenBucketRateLimiter、RateLimitDomainService；ResilientUpstreamClient、RetryExecutor、各 RetryStrategy、EndpointMetrics |
  | T6 | audit（38.5%）+ common（43.1%）+ alert/stats（0%） | AuditContext、AuditEventListener、CallLogGatewayImpl；JsonUtils；StatsService（stats 7 行简单可补）；alert 为纯枚举/模型（0% 属口径噪音，不补，见风险） |

- **测试模式**：
  - 门面服务：mock 各 `*Gateway` → 验证编排逻辑（状态流转/异常/边界）；转换类断言
  - 调用器/适配器：mock 依赖（UpstreamClient/协议契约）→ 验证调用链/错误分类/流式回调
  - 策略/限流器：直接实例化 + 边界输入断言
  - 事件监听器：mock 依赖 + 发布事件断言
  - 工具类（JsonUtils）：真实输入断言序列化/反序列化
- **不补测**：纯 POJO 模型（@DomainEntity/record/enum/Builder）——无逻辑；若某域覆盖率因纯模型拉低无法达 90%，如实记录并说明口径
- alert/stats 的 0% 主因是域根包嵌套枚举（`AlertRule.NotificationChannel` 等）未被 excludes 覆盖——**不补测枚举**（无价值），若 stats 仅 StatsService 可快速补至可测
- 若测试暴露实现 bug（如行为与预期不符）：**不顺手改实现**，记录该处并跳过，纳入遗留

---

## Task 1: 基线验证

**Files:**
- 无

**Interfaces:**
- Consumes: 无
- Produces: 基线覆盖率数据（已采集，确认构建绿）

- [ ] **Step 1: 全量构建 + 测试**

```bash
cd /e/workspace/llm-gateway
./mvnw clean install
```

Expected: BUILD SUCCESS，全部测试绿（master `3c46be26`；surefire 349 + failsafe 48）。

- [ ] **Step 2: 确认聚合报告可读**

`gateway-coverage/target/site/jacoco-aggregate/jacoco.csv` 存在（基线数据已在本计划头部）。

---

## Task 2: provider 门面服务补测

**Files:**
- Create（gateway-provider/provider/src/test/.../provider/service/）：
  - `PlanCatalogServiceImplTest.java`、`ChannelProvisionServiceTest.java`、`ProviderServiceImplTest.java`、`ChannelServiceImplTest.java`、`ModelInstanceServiceImplTest.java`
- 若有既有同名测试（P3 随迁的 Mockito 单测存在，如 ChannelServiceImplTest 已有）——**扩展/补全**而非新建（核对现状）

**Interfaces:**
- Consumes: 各 ServiceImpl + 其依赖 Gateway（ChannelGateway/ModelGateway/ProviderGateway/PlanCatalogGateway 等）
- Produces: 5 个门面服务的核心逻辑覆盖

- [ ] **Step 1: 核对既有测试**

`ls gateway-provider/provider/src/test/java/com/codingas/gateway/provider/service/`——P3 随迁的测试（ChannelServiceImplTest、ModelServiceTest 等）已覆盖部分；补测目标是缺口类（PlanCatalogServiceImpl/ChannelProvisionService/ProviderServiceImpl/ChannelServiceImpl 未覆盖分支/ModelInstanceServiceImpl）。

- [ ] **Step 2: 补测缺口类**

对每个目标 ServiceImpl：mock 其依赖 Gateway（按构造器/字段），覆盖全部 public 方法的**分支**（成功/失败/空/异常/边界）。测试模式参考既有测试风格（@ExtendWith(MockitoExtension) + @Mock + @InjectMocks + AssertJ）。

- [ ] **Step 3: 运行 + 覆盖率验证**

```bash
./mvnw test -pl gateway-provider/provider
./mvnw clean install
python -c "（按 Global Constraints 口径统计 provider 域行覆盖率）"
```

Expected: provider 域核心覆盖率较 50.1% 显著提升（目标 ≥70% 本轮；若门面服务覆盖后仍受纯模型拉低，如实记录）。

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "test: provider 门面服务补测（PlanCatalog/ChannelProvision/Provider/Channel/ModelInstance，覆盖率补测）
Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

## Task 3: proxy 补测

**Files:**
- Create/扩展（gateway-proxy/proxy/src/test/.../proxy/）：
  - `experience/ModelExperienceServiceTest.java`、`invoker/KeyFailoverInvokerTest.java`（若已有则扩展未覆盖分支）、`chat/ErrorClassifierTest.java`（扩展）、`chat/ChatDispatchServiceImplTest.java`（已有则扩展）

**Interfaces:**
- Consumes: 各依赖（UpstreamClientRegistry、ResilientClientFactory、Gateways、ProtocolConversionFacade）
- Produces: proxy 核心覆盖提升

- [ ] **Step 1: 核对既有测试 + 补缺口**

`grep -l "class.*Test" gateway-proxy/proxy/src/test/java/` 核对已有；对 ModelExperienceService（127 行 miss）新建全量测试；KeyFailoverInvoker（47 miss）补故障转移分支；ChatDispatchServiceImpl 补分发分支。

- [ ] **Step 2: 运行 + 覆盖率验证**

```bash
./mvnw clean install
```

Expected: proxy 域覆盖率较 60.8% 提升（目标 ≥75%）。

- [ ] **Step 3: Commit**

```bash
git add -A
git commit -m "test: proxy 补测（ModelExperienceService/KeyFailoverInvoker/ChatDispatch，覆盖率补测）
Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

## Task 4: protocol + iam 补测

**Files:**
- Create/扩展：
  - `gateway-protocol/protocol-openai/src/test/.../OpenAIProtocolAdapterTest.java`（扩展 adapter 分支）、`OpenAIProtocolValidatorTest.java`（新建）
  - `gateway-protocol/protocol-anthropic/...`（AnthropicProtocolAdapter/Validator/Tuner）
  - `gateway-protocol/protocol-gemini/.../GeminiProtocolAdapterTest.java`（扩展）
  - `gateway-iam/iam/src/test/.../service/UserServiceImplTest.java`、`encryption/Aes256EncryptionServiceTest.java`（新建）

**Interfaces:**
- Consumes: adapter/validator（protocol 契约）+ UserServiceImpl/Aes256（iam 依赖）
- Produces: protocol/iam 覆盖提升至 ≥90%

- [ ] **Step 1: protocol 补测**

协议 adapter：canonical↔协议转换双向断言；validator：合法/非法请求断言；tuner：默认值/模型名替换断言；UpstreamClient 流式回调（onChunk/onError）断言。

- [ ] **Step 2: iam 补测**

UserServiceImpl（30 miss）：mock UserGateway/UserApiKeyGateway/加密服务，覆盖创建/更新/查询分支；Aes256EncryptionService（22 miss）：加密/解密/错误密钥断言。

- [ ] **Step 3: 运行 + 覆盖率验证 + Commit**

```bash
./mvnw clean install
```

Expected: protocol ≥90%、iam ≥90%。

```bash
git commit -m "test: protocol + iam 补测（adapter/validator/UserService/Aes256，覆盖率补测）
Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

## Task 5: usage + security + resilience 补测

**Files:**
- Create/扩展：
  - `gateway-usage/usage/src/test/.../tokenlimit/TokenLimitServiceImplTest.java`（扩展）、`event/TokenUsageEventListenerTest.java`
  - `gateway-security/security/src/test/.../threat/InMemoryTokenBucketRateLimiterTest.java`、`RateLimitDomainServiceTest.java`
  - `gateway-resilience/resilience/src/test/.../upstream/ResilientUpstreamClientTest.java`（扩展）、`retry/RetryExecutorTest.java`（扩展）、各 `*RetryStrategyTest`、`metrics/EndpointMetricsTest.java`

**Interfaces:**
- Consumes: 各域依赖
- Produces: usage/security/resilience 覆盖提升至 ≥90%

- [ ] **Step 1-2: 逐域补测**

usage：TokenLimit 增删改查/超限行为/事件发布；security：限流器桶状态/窗口/拒绝；resilience：包装器熔断/重试/指标、各策略分支。

- [ ] **Step 3: 运行 + 覆盖率验证 + Commit**

```bash
./mvnw clean install
git commit -m "test: usage + security + resilience 补测（限流/重试/熔断/策略，覆盖率补测）
Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

## Task 6: audit + common + alert/stats + 最终验证

**Files:**
- Create/扩展：
  - `gateway-audit/audit/src/test/.../AuditContextTest.java`、`event/AuditEventListenerTest.java`（扩展）；`gateway-audit/audit-data/src/test/.../CallLogGatewayImplTest.java`（新建）
  - `gateway-common/src/test/.../util/JsonUtilsTest.java`
  - `gateway-stats/stats/src/test/.../StatsServiceTest.java`（新建，mock 4 Gateway 断言统计）
  - alert：不补测（纯枚举/模型，见 Global Constraints）

**Interfaces:**
- Consumes: 各依赖
- Produces: 全域覆盖率最终验证

- [ ] **Step 1-2: 补测 + 运行**

- [ ] **Step 3: 最终覆盖率验证（DoD 对照）**

```bash
./mvnw clean install
```

从聚合 CSV 统计各域核心覆盖率，对照 DoD ≥90%。**如实记录**：达标域清单 + 未达标域与原因（多为纯 POJO 模型拉低口径，属预期噪音）。

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "test: audit/common/stats 补测 + 覆盖率 DoD 验证（质量基建）
Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

## Self-Review 记录

**Spec 覆盖对照**：
- provider 门面 → T2
- proxy → T3
- protocol/iam → T4
- usage/security/resilience → T5
- audit/common/alert/stats → T6
- DoD 验证 → T6 Step 3

**Placeholder 扫描**：目标类清单明确；测试模式明确（参考 Q3 与既有测试）；无 TBD。每任务的具体测试内容由 implementer 按目标类与模式编写（数量大，不逐一贴码——以既有测试风格为准）。

**Type/命名一致性**：测试类名 = 目标类 + Test ✓；与 P3 随迁测试/CountPortTest 风格一致 ✓

**风险**：
- 纯 POJO 模型（@DomainEntity/枚举）拉低覆盖率 → 如实记录口径（DoD 按逻辑类评估）
- 测试暴露实现 bug → 不顺手改实现，记录遗留（行为不变约束）
- 门面服务 mock 面大（多 Gateway）→ 测试编写量大，按任务分批
- alert 0% 为枚举口径 → 不补测枚举，如实说明
