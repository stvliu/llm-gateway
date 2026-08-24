# LLM-Gateway 业务域清单 + 跨域依赖图

> **已过时**：本文为 2026-08 模块化**之前**基于单模块结构（`gateway-boot`，分层 adapter/application/domain/infrastructure/common）的领域耦合分析快照，用于支撑"按业务域拆模块"重构设计。项目已于 2026-08-25 完成 17 模块化（模块 = 根包），文中包路径仅作历史参考，请以各功能域模块的实际结构为准。

> 探索代理分析产出（2026-08-16），为"按业务域拆模块"重构设计提供基础数据。
> 主代码在 `gateway-boot/src/main/java/com/codingas/gateway/`（分层：adapter/application/domain/infrastructure/common）。

## 一、业务域 → 覆盖的包（四层横切视图）

| 业务域 | domain/ | application/ | infrastructure/ | adapter/ |
|---|---|---|---|---|
| **supply**（供应/渠道/模型/Provider） | supply(entity/enums/gateway/valueobject)、supply/catalog | channel、channelcredential、provider、model、catalog、supply | supply(gateway/upstream/catalog/repository)、upstream | api: Channel/ChannelCredential/Provider/Model/ModelInstance/PlanCatalog/ChannelProvision/ModelDiscovery/Protocol |
| **protocol**（协议契约） | protocol/contract、protocol/tuning、protocol/validation | protocol/conversion | protocol(OpenAI/AnthropicAdapter, StreamConverter) | protocol/openai、protocol/anthropic、api: OpenAI/Anthropic/Protocol |
| **iam**（身份/用户/APIKey/加密） | iam(entity/enums/exception/gateway/service/valueobject) | auth、user、userapikey | iam(gateway/encryption) | api: Auth/User/UserApiKey/Me；interceptor: ApiKeyAuth/TokenAuth；advice: Iam |
| **usage**（用量/TokenLimit） | usage(entity/event/enums) | quota(TokenLimitService) | usage(TokenLimitGatewayImpl、TokenLimitDo/RateLimitConfigDo) | api: TokenLimit |
| **quota**（限额端口，很薄） | quota(gateway/TokenLimitGateway) | quota(Service/Impl、TokenUsageEventListener) | 实现放在 usage 下(TokenLimitGatewayImpl) | api: TokenLimit |
| **threat**（威胁/IP/限流） | threat(entity/exception/gateway/service) | （无 application/threat） | threat(InMemoryTokenBucket、IpBlockGatewayImpl) | interceptor: RateLimit/IPBlockCheck；advice: Threat |
| **application**（接入方，包名易混淆） | application(entity/enums/gateway) | application(ApplicationService/Impl) | application(ApplicationGatewayImpl、ApplicationChannelGatewayImpl) | api: Application |
| **resilience**（韧性/failover/重试） | resilience(entity/gateway) | resilience(Service、event/FailoverEventListener) | resilience(RetryStrategy/CircuitBreaker/ResilientUpstreamClient/FailoverEventGatewayImpl) | api: ResilienceEvent |
| **audit**（审计/调用日志） | audit(entity/gateway) | audit(AuditEventListener) | audit(AuditGatewayImpl、CallLogGatewayImpl、repositories) | （经 advice/interceptor 消费） |
| **alert**（告警，脚手架级） | alert(entity) | （无） | alert(dataobject 仅 AlertRuleDo/AlertNotificationDo) | （无） |
| **dataprotection**（脱敏规则，孤立） | dataprotection(entity/exception/gateway) | （无） | dataprotection(GatewayImpl/Converter/Initializer) | （无） |
| **experience**（体验/playground，无 domain 层） | （无 domain/experience） | experience(ModelExperienceService+dto) | （无） | api: Experience |
| **proxy**（运行时核心，无 domain 层） | （无 domain/proxy） | proxy(ChatDispatchService/Impl、routing/*、invoker/*、failover、OutboundTuner) | （实现散布在 supply/resilience/upstream） | api: OpenAI/Anthropic/SseStreamHelper |
| **stats**（报表） | （无） | stats(StatsService+dto) | （复用 audit/usage 数据） | api: Stats |

## 二、跨域依赖图（import 证据）

### domain 层内部
- **supply → protocol**：`domain/supply/gateway/UpstreamClient.java:18-20` import `domain.protocol.contract.*`
- **supply → application**：`domain/supply/valueobject/RoutingContext.java:18` import `domain.application.enums.FailureStrategy`
- **usage → supply**：`domain/usage/entity/TokenLimit.java:22-23` import `domain.supply.entity.Model/Provider`
- **usage → iam**：`domain/usage/entity/TokenLimit.java:24` import `domain.iam.entity.User`
- **quota → usage**：`domain/quota/gateway/TokenLimitGateway.java:18` import `domain.usage.entity.TokenLimit`
- **resilience → supply**：`domain/resilience/entity/FailoverEvent.java:20-21` import `domain.supply.enums.FailoverDecision/ProviderErrorType`
- **alert → usage**：`domain/alert/entity/AlertRule.java:20` import `domain.usage.enums.PeriodType`
- iam / threat / audit / application / dataprotection：域内自洽，无对外 domain 依赖

### application 层跨域
- **proxy**（最重）→ protocol.contract、supply(RoutingContext/enums/gateways/exception/ResilientClientFactory)、iam.Identity、application(FailureStrategy/ApplicationChannel)、audit(CallLog/AuditGateway)、usage(TokenUsedEvent)、application/protocol(ProtocolConversionFacade) — `application/proxy/ChatDispatchServiceImpl.java:20-27`
- **experience → supply + protocol**：`application/experience/ModelExperienceService.java:21-37`
- **quota(TokenLimitServiceImpl) → supply + usage + iam + quota**：`application/quota/TokenLimitServiceImpl.java:24-32`
- **catalog → supply + supply/catalog**：`application/catalog/ChannelProvisionService.java:22-39`
- **provider / channel / channelcredential / model / supply → supply**：各自 Service 大量 import `domain.supply.*`
- **application(ApplicationServiceImpl) → iam**：`application/application/ApplicationServiceImpl.java:28`
- **userapikey → iam + application**：`application/userapikey/UserApiKeyServiceImpl.java:24-28`
- **user → auth**：`application/user/UserServiceImpl.java:19-21`
- **channelcredential → channel**：`application/channelcredential/ChannelCredentialServiceImpl.java:18`
- **proxy → application/protocol**：`application/proxy/invoker/ChannelFailoverInvoker.java:27`

### infrastructure 层跨域（反向/横向耦合）
- **usage 实现 quota 端口**：`infrastructure/usage/gateway/TokenLimitGatewayImpl.java:18-19`（实现被放 usage 包下）
- **supply → iam 加密**：`infrastructure/supply/gateway/ChannelCredentialGatewayImpl.java:18` import `domain.iam.service.ApiKeyEncryptionDomainService`
- **supply → application**（向上依赖，反 DDD）：`infrastructure/supply/gateway/StubChannelKeyProbe.java:18` import `application.supply.dto.KeyTestResult`
- **resilience → supply + protocol**：`infrastructure/resilience/*`
- **config / actuator / upstream → supply**：`ConfigVersionChecker`、`ProviderHealthTracker`、`{OpenAI,Anthropic}UpstreamClient`
- **alert → usage**：`infrastructure/alert/gateway/database/dataobject/AlertRuleDo.java:19`

### 依赖矩阵汇总
| 被依赖域 \ 依赖方 | proxy | experience | quota | usage | resilience | alert | catalog | channel/provider/model | infra(supply/config/actuator/upstream) |
|---|---|---|---|---|---|---|---|---|---|
| **supply** | ✔ | ✔ | ✔ | ✔ | ✔ | — | ✔ | ✔ | ✔ |
| **protocol** | ✔ | ✔ | — | — | ✔ | — | — | — | ✔ |
| **iam** | ✔ | — | ✔ | ✔ | — | — | — | — | ✔ |
| **application** | ✔ | — | — | — | — | — | — | — | — |
| **usage** | ✔ | — | ✔ | — | — | ✔ | — | — | ✔ |
| **audit** | ✔ | — | — | — | — | — | — | — | — |
| **quota** | — | — | ✔ | — | — | — | — | — | ✔(实现) |
| **threat/alert/dataprotection** | — | — | — | — | — | — | — | — | — |

## 三、公共底座 vs 上层
- **被依赖最多的公共底座**：**supply**（几乎每个域都引用其 Model/Channel/Provider/enums）、**protocol**（契约被 supply/proxy/resilience/experience/adapters 共享）、**iam**（Identity/User/加密服务被 proxy/usage/interceptors 依赖）
- **依赖最多的上层/编排者**：**proxy**（依赖 supply+protocol+iam+application+audit+usage 六域）、**experience**、**catalog**、**quota**
- **无出向依赖的叶子域**：threat、audit、dataprotection、alert（仅依赖 common）

## 四、横切/共享清单
1. **common/**：`BaseEntity`（审计字段 createdBy/createdAt/updatedBy/updatedAt）、DomainEntity、ApiResponse/PageRequest/PageResponse、GatewayException 族、JsonUtils
2. **协议契约**：`domain/protocol/contract/*` + `adapter/protocol/*` + `infrastructure/protocol/*`
3. **事件总线**：`common/event/*` + 实现 `infrastructure/event/LocalDomainEventPublisher` + `domain/usage/event/TokenUsedEvent`
4. **安全拦截器责任链**：`adapter/interceptor/SecurityInterceptorChain`（组合 iam 认证 + threat IP/限流等）
5. **infrastructure/config**：GatewayProperties/HttpClientConfig/SecurityConfig/WebConfig/ConfigVersionChecker
6. **init/种子数据**：`application/init/*` + `infrastructure/supply/catalog/loader/BuiltinDataLoader` + `infrastructure/dataprotection/SensitiveDataRuleInitializer`

## 五、拆模块最"痛"的耦合点
1. **proxy 是"上帝编排者"**：无 domain/proxy 包，application 层直接操作六域。拆 proxy 需为其发明 domain 抽象。
2. **quota ↔ usage ↔ supply ↔ iam 四分五裂**：TokenLimit 实体在 usage、端口在 quota、实现在 usage、管理服务在 quota。同一概念拆进 4 个包。
3. **supply 是全局依赖中枢**：几乎全被依赖；按 provider/channel/model 细分会让所有消费者 import 跟着改。
4. **supply infra → iam 加密服务**：凭据持久化与 iam 加密域强耦合。
5. **resilience ↔ supply 双向渗透**：failover 语义与供应概念相互纠缠。
6. **threat 与 usage/quota 限流语义重叠**：同一"限流"概念被切开。
7. **alert 是未完成脚手架**：只有 entity+DO。
8. **dataprotection 完全孤立、无消费者**：脱敏能力未接入 proxy 路径。
9. **experience 无 domain 层**：纯 application 薄层。
