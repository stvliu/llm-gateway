# 无用代码清理清单（2026-08-26 全量分析）

> 分析方式：7 个并行审计代理（后端 4 组 + 依赖/资源 + 前端代码 + 前端 i18n/依赖/资源）逐类全仓库引用验证 + 主会话抽查复核。
> 保守原则：宁可少删不可误删；框架反射/SPI/规划性声明一律保留；i18n 用"完整 key 字面量"判据。

## ✅ 执行结果（2026-08-27 完成）

用户确认范围：A 组全部删除；B1 inert 骨架保留待实现；质量配置保留 + 删 specs/；D 组一并修复。

**已执行**（共删除 88 个文件 + 修改 80 个文件）：
- 后端：6 死类/功能簇 + 约 30 处死方法/死字段（含 Repository 三层连锁、构造器、写-only 字段）+ 连带死 JPA 接口/实现 + 同步删除/改写 8 个测试文件
- 前端：`ApiResponse`/`Status` 类型 + `chatService.chat` + 4 个孤儿 svg + 21 个 i18n 未用 key（zh/en 对称）
- 依赖/资源：`okhttp-sse`、`jgit`（boot pom）、`metadata/**`（49 json）、孤儿 `db/V1__init_schema.sql`、2 段无绑定配置块
- 内容：`specs/`（23 文件）、`docs/数据架构.md` 的 `channel_operation_logs` 过时描述
- D 组修复：前后端 `manifest.json` favicon 路径（各 8 处）、11 个反向缺失 i18n key 补充、3 处重复 import、`ModelInstanceFacade` 冗余 setChannelId
- 保留：规划性依赖（redisson/springdoc/OTel/testcontainers）、redis、quality 配置、B1 inert 骨架

**验证**：
- 后端 `mvnw clean test-compile` ✅ 通过（初版编译失败后补齐 6 处测试遗漏）
- 后端 `mvnw test` ✅ BUILD SUCCESS（全模块 0 失败）
- 前端 `pnpm build`（tsc + vite）✅ 通过
- 前端 `pnpm test`：142/145 通过；3 个失败经 stash 基线验证 = 2 个基线既有（UserApiKeyModal 补绑测试）+ 1 个偶发超时（Applications 跳转测试，单独跑通过），均与本次清理无关

**遗留建议**：`RateLimitProperties.qpsThreshold` 在删除 `shouldFailClose` 后无消费者（配置残留，未列入清单）；`ProviderResponse.description`/`ChannelCredentialResponse.description` 前端读取但后端不填充（契约占位，需产品决策补映射或删除）。

## A. 已确认死代码（高置信，建议删除）

### A1. 后端死类/功能簇
| 项 | 位置 | 证据 |
|---|---|---|
| UsageLogDo | `gateway-audit/audit-data/.../usagelog/UsageLogDo.java` | 全仓库仅自身文件；表已建但无 Java 代码读写 |
| DataProtectionException | `gateway-security/security/.../dataprotection/DataProtectionException.java` | 仅自身文件，无 throw/catch/继承 |
| ChannelOperationLog 功能簇（6 文件） | `provider/channel/ChannelOperationLogRepository.java`、`ChannelOperationLog.java`、`providerdata/channel/JpaChannelOperationLogRepository.java`、`ChannelOperationLogJpaEntity.java`、`ChannelOperationLogJpaRepository.java` + 1 测试 | 整条链路从未接线：接口无任何注入方，仅 bean 自身 + 单测 |
| AlertRule / AlertNotification | `gateway-alert/alert/.../AlertRule.java`、`AlertNotification.java` | 与 DO 互为孤岛，无 repository/service/controller |
| AlertRuleDo / AlertNotificationDo | `gateway-alert/alert-data/.../AlertRuleDo.java`、`AlertNotificationDo.java` | 同上，无任何消费者 |

### A2. 后端死方法/死字段（均无生产调用，仅测试引用）
| 项 | 位置 |
|---|---|
| ModelResponse.from(List) | `gateway-web/.../dto/ModelResponse.java:80` |
| ProviderResponse.from(List) | `gateway-web/.../dto/ProviderResponse.java:68` |
| TokenLimitCreateRequest.limitCode（含 @NotBlank） | `gateway-web/.../dto/TokenLimitCreateRequest.java:37` |
| TokenLimitResponse.limitCode | `gateway-web/.../dto/TokenLimitResponse.java:39` |
| ModelNestedRequest.inputPrice/outputPrice | `gateway-web/.../dto/ModelNestedRequest.java:39-40` |
| ModelInstanceUpdateRequest.channelId（含 facade setChannelId 冗余写） | `gateway-web/.../dto/ModelInstanceUpdateRequest.java:26` |
| UserRepository.delete / findByEmail / existsByUsername（三层连锁） | `gateway-iam/iam/.../user/UserRepository.java:72/51/88` + `iam-data/JpaUserRepository` + `UserJpaRepository` |
| UserApiKeyJpaRepository.findByKeyHash / existsByKeyPrefix | `gateway-iam/iam-data/.../UserApiKeyJpaRepository.java:30/43` |
| ApplicationChannelRepository.existsByApplicationIdAndChannelId（三层连锁） | `gateway-iam/iam/.../ApplicationChannelRepository.java:61` |
| ApplicationState.isRoutable() | `gateway-iam/iam/.../ApplicationState.java:39`（勿误删 provider 域 ChannelState.isRoutable） |
| UserQuery.roleCode（写-only 字段） | `gateway-iam/iam/.../user/UserQuery.java:38` |
| Application 8 参构造器 | `gateway-iam/iam/.../Application.java:96`（生产走无参+setter） |
| ApplicationChannel 3 参构造器 | `gateway-iam/iam/.../ApplicationChannel.java:83` |
| RateLimitManager.shouldFailClose / getStatus | `gateway-security/security/.../threat/RateLimitManager.java:60/67` |
| IpBlocklistManager.blockIp×2 / unblockIp | `gateway-security/security/.../threat/IpBlocklistManager.java:48/56/66` |
| TokenBucketRateLimiter.reset（接口+实现） | `gateway-security/security/.../TokenBucketRateLimiter.java:46` / `InMemoryTokenBucketRateLimiter.java:103` |
| TokenBucketStatus.usagePercent | `gateway-security/security/.../TokenBucketStatus.java:22` |
| IpBlocklist.isExpired | `gateway-security/security/.../IpBlocklist.java:47` |
| SensitiveDataRule.isEnabled | `gateway-security/security/.../SensitiveDataRule.java:45` |
| TokenLimitRepository.findByUserId/count/delete/deductUsage | `gateway-usage/usage/.../TokenLimitRepository.java:52/66/73/83` |
| TokenLimit.isExceeded | `gateway-usage/usage/.../TokenLimit.java:83` |
| AuditLogRepository.save/findByUserId | `gateway-audit/audit/.../AuditLogRepository.java:33/41` |
| CallLogRepository.findByTraceId/findByUserId | `gateway-audit/audit/.../CallLogRepository.java:34/39` |
| StreamConfig.invalidChunk | `gateway-simulator/.../service/StreamConfig.java:29` |
| ChannelCredentialManager.getById（接口+实现） | `gateway-provider/provider/.../ChannelCredentialManager.java:42` / `Impl:59` |
| ProviderHealthTracker.getStatus / hasHealthyProvider | `gateway-provider/provider/.../ProviderHealthTracker.java:58/125` |
| ProviderHealthState.isStale（随 getStatus 联动） | `gateway-provider/provider/.../ProviderHealthState.java:70` |
| RoutingResolver.resolve（单值版） | `gateway-proxy/proxy/.../RoutingResolver.java:66` |

### A3. 前端死代码
| 项 | 位置 | 证据 |
|---|---|---|
| ApiResponse&lt;T&gt; | `gateway-console/src/types/api.ts:34` | 仅定义 + 3 处注释提及 |
| Status 类型 | `gateway-console/src/types/api.ts:47` | 无任何 import |
| chatService.chat（非流式） | `gateway-console/src/services/chatService.ts:131` | 零调用，仅 streamChat 被使用 |
| welcome_crm.svg | `gateway-console/src/assets/images/backgrounds/` | 全仓库零引用 |
| codingas-favicon-black.svg / white.svg / logo-black.svg | `gateway-console/src/assets/images/` | 全仓库零引用 |

### A4. 依赖与资源
| 项 | 位置 | 证据 |
|---|---|---|
| okhttp-sse 依赖 | `gateway-boot/pom.xml:153` | 全仓库零 import okhttp3.sse |
| org.eclipse.jgit 依赖 | `gateway-boot/pom.xml:286-289` | 全仓库零 import |
| metadata/** 目录（49 个 json） | `gateway-boot/src/main/resources/metadata/` | 无代码加载；BuiltinDataLoader 用 catalog/*.json；pom license exclude 印证历史遗留 |
| db/V1__init_schema.sql（孤儿） | `gateway-boot/src/main/resources/db/` | Flyway locations 仅 db/migration，此文件不加载 |
| application-standalone.yml `gateway.config.*` 块 | `:24-29` | 无 @ConfigurationProperties/@Value 绑定 |
| application-integration-test.yml `gateway.circuit-breaker.*/http-client.*/degradation.*` 块 | `:31-46` | 无绑定（仅测试资源） |

### A5. 前端 i18n 未用 key（23 个，高置信：全源码零字面量）
- **apiKeys**: `revoke, rotate, updateSuccess, updatedAt`
- **catalog**: `model.modelName, model.capabilities, model.contextWindow, provider.code, provider.name`
- **channels**: `inlineList.cancel, inlineList.confirm`
- **common**: `state.suspended`
- **quickstart**: `apiKey.createHint, keyActions, keyCreatedAt, keyRevokeFailed, keyRevoked, keyStatus, noActiveKey, noKey, playground.done, playground.usingAbove, status.degraded, status.expired`

## B. 需人工决策项

### B1. 功能未接线（inert）——删 or 保留待实现
- **dataprotection 脱敏规则**：`SensitiveDataRuleInitializer` 启动写入 8 条规则，但无任何运行时消费方（规则"只写不读"）；`findByRuleCode/findByEnabledTrue/findByDataType/existsByRuleCode` 仅测试调用
- **AuditEvent 无发布方**：`AuditEventListener` 为 @Component 但无代码发布 AuditEvent → 永不触发
- **TokenUsageEventListener 仅打日志**：`TokenUsedEvent` 正常发布但监听器不执行扣减
- **gateway-alert 整域**：仅配置 + 4 模型类，无业务装配（见 A1）
- **gateway-cli 空壳**：仅 Application 类，无任何 @ShellComponent 命令

### B2. 前端次要项
- **测试专用 helper**：`stateTransitions.getAvailableTransitions`、`lifecycle.isRoutable/isBilling/canTransitionTo`（仅被防漂移测试引用；删需同步改测试）
- **过度导出**（去 export 关键字即可）：`CodeSnippet.Lang`、`chatStore.ModelOption`、`useConfirm.ConfirmType/ConfirmOptions`、`useSavePulse.UseSavePulseResult`、`useDangerConfirm.DangerConfirmOptions`、`theme.lightTheme/darkTheme`、`client.API_BASE_URL/notifyForbidden`、`stats.StatsResponse` 等
- **类型重复定义**：`ChannelHealthStatus/ChannelHealthSource`、`ChannelState` 各两份（建议合并，非死代码）

### B3. 质量配置孤儿（涉及 CI 决策）
- `checkstyle.xml` / `spotbugs-exclude.xml` / `owasp-suppressions.xml`：无 pom 引用（CI 命令失效被 continue-on-error 吞）。**删除 or 接线** 属质量基建决策，默认建议保留待接线

### B4. 历史内容目录
- `specs/`（001-003 历史规格）：已被 openspec 取代，无文档引用
- `docs/favicons/`：需确认是否被引用

## C. 明确保留项（不清理）
- 根 pom dependencyManagement 全部规划性声明：redisson、springdoc、micrometer-tracing-bridge-otel、OTel exporter×2、testcontainers 系列、flyway-database-h2、dependency-check-maven、junit-jupiter、elasticsearch、toxiproxi、h2、postgresql
- redis 相关（spring-boot-starter-data-redis、redisson 规划）：生产环境要用
- gateway-boot 的 opentelemetry-api：可观测性规划
- 前端 prettier、@vitest/ui：无使用证据但删除收益低（若用户要求可移除）
- `.specify/`（Speckit 工具配置）、`openspec/changes/archive/`（规范归档）
- gateway-web 全部 DTO 中"被 Controller/Facade 方法签名引用"的类

## D. 附带发现（建议修复而非删除）
1. **manifest.json favicon 路径 bug**：`gateway-console/public/manifest.json` 图标路径指向 `/favicons/` 子目录（不存在），实际文件在 public 根目录 → PWA 图标 404。修复：去掉 favicons/ 前缀（同 `gateway-boot/static/manifest.json` 相同问题）
2. **反向缺失 i18n key（6 组）**：源码 t() 但 locale 无 key，全走 defaultValue 兜底（如 catalog `billingMode.*` 动态 key 恒回退原始字符串）→ 建议补充 key
3. **flyway-database-h2 缺口**：仅根 DM 规划，gateway-boot 默认 H2+Flyway 路径可能需提升为真实依赖（需实测确认）
4. **重复 import**：ProtocolController/AnthropicController 的 java.util.Map、ApplicationChannelItem、BuiltinVendorLoader 等

## 预计影响面
- 后端：约 30 处方法/字段删除 + 6 死类 + 1 功能簇（5 文件），涉及 gateway-web/iam/security/usage/audit/simulator/provider/proxy/alert 模块
- 前端：2 类型 + 1 方法 + 4 svg + 23 i18n key
- 依赖：2 个（okhttp-sse、jgit）
- 资源：metadata/** + 1 sql + 配置块
- 均需同步删除仅测试引用的测试代码；删后全量 `mvnw clean install` + 前端 build/test 验证
