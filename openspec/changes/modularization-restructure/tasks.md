## 1. Phase 0：基础设施与基线

- [x] 0.1 引入 `archunit-junit5`，落地 ArchUnit 架构测试基座（`noClasses` + freeze 冻结 5 条分层铁律），对当前代码通过（34 处历史违规冻结为基线）
- [x] 0.2 冻结 baseline：`gateway-boot` 全量 `mvn test` 通过（717 测试全绿），记录为迁移前后回归对照基线
- [x] 0.3 建立跨层违规点清单（`docs/modularity-violation-inventory.md`：domain→infrastructure 34 处 + Facade→Adapter）作为后续各阶段验收参照

## 2. Phase 1：底座拆分

- [x] 1.1 新建 `gateway-common` 模块，迁移 exception/BaseEntity/util/event（16 个文件 + 上浮 ProviderErrorType/FailoverDecision 两个通用 enum 到 common.enums），编译 + 测试通过（715 全绿）
- [x] 1.2 common 纯横切校验：enum 上浮后 common 不再依赖任何业务层，`COMMON_NOT_DEPEND_ON_BUSINESS` 违规解冻
- [x] 1.3 新建 `gateway-provider`，迁移 `domain/supply` 全部（38 main + 6 test）。前置治理：KeyTestResult/AuthStatus 下沉到 supply、FailureStrategy 上浮 common、移除 Javadoc @link 误判、ArchUnit 改精确包前缀
- [x] 1.4 `RoutingContext` 已是 Java `record`（字段 final、自动 getter、无 setter），天然不可变值对象，无需额外收敛
- [ ] 1.5 （可选）拆 `provider-api` 子模块，上层只依赖 api
- [x] 1.6 迁移 provider 的 application（ChannelHealthService 等 3 文件）+ infrastructure（JPA/Repository/UpstreamClient 等 33 文件 + ErrorClassification 族 4 文件）到 gateway-provider。前置：BaseDo 迁 gateway-common（加 spring-data-jpa）、CredentialEncryptor 接口解耦 iam 加密、provider 加 JPA/okhttp/jackson 依赖。单测迁 provider（63 全绿），boot surefire 652 + failsafe 37 全绿，provider 独立于 boot 编译
- [x] 1.7 新建 `gateway-iam`，迁移 User/Application/APIKey/Auth/加密。迁移三层 62 生产文件 + 10 测试（domain.iam + domain.application + application.auth/user/userapikey/application + infrastructure.iam/application）。PasswordEncoder 从 boot SecurityConfig 迁入 iam（Sa-Token SHA-256），adapter/拦截器留 boot。依赖 JPA + sa-token
- [x] 1.8 确认 iam 依赖面：只依赖 gateway-common + gateway-provider 的 CredentialEncryptor（+ BaseDo），无 boot 特有包引用，独立编译。iam 106 测试、boot 546 surefire + failsafe 全绿，全仓 8 模块 install 通过
- [x] 1.9 重命名 `gateway-capability-api` → `gateway-protocol`：改 artifactId/父 pom/boot 依赖；并入 Canonical IR + `ProtocolAdapter` SPI + 契约 DTO + tuning/validation（**保持包名仅改模块归属**，包名统一留待后续）
- [x] 1.10 改造 `ProtocolConversionFacade` 按 SPI 装配：注入 `List<ProtocolAdapter<?>>` 按 `protocol()` 动态装配，删除对 OpenAI/Anthropic Adapter 具体类 import（仅保留 stream 委托）。新增协议仅需注册 Adapter Bean 即可。546 surefire + failsafe 全绿
- [x] 1.11 迁移 tuning/validation 契约到 `gateway-protocol`（随 domain.protocol 一并并入）
- [x] 1.12 底座四模块（common/provider/iam/protocol）各自独立编译 + ArchUnit 通过。治理 iam 违规：EncryptionService 接口从 infrastructure 上浮到 domain.iam.gateway（依赖倒置），四底座 domain 层 0 依赖 infrastructure。全仓 8 模块 clean install + ArchUnit 4 规则全绿，boot 546 surefire + failsafe 全绿

## 3. Phase 2：中基拆分

- [x] 2.1 新建 `gateway-security`，迁移 threat（14）+ dataprotection（8）+ 3 测试（23 文件）。前置治理：RateLimitProperties 值对象上浮 threat 域（RateLimitDomainService 不再依赖 boot GatewayProperties），boot ThreatRateLimitConfig 映射注入。security 只依赖 common，14 测试全绿
- [x] 2.2 拦截链澄清：拦截链为 adapter 编排（iam/threat + sa-token），security 不反向依赖 usage（TokenLimit 配额与令牌桶限流为两套独立机制，不进拦截链）
- [x] 2.3 新建 `gateway-usage`，迁移 usage+quota 17 生产文件 + 3 测试（usage↔quota 共享 TokenLimit 边界故合并）。依赖 common/provider/iam，独立编译，23 测试全绿
- [x] 2.4 明确 usage 明细表归属：当前 stats 直连 provider/iam 仓库计数、不读 usage（todayRequests/tokenUsage 为 mock，TODO 留待审计统计），无只读接口需求
- [x] 2.5 新建 `gateway-resilience`，迁移 failover/retry/circuit-breaker（25 生产文件 + 7 单测，2 个 @SpringBootTest 留 boot）。依赖 common/protocol/provider + micrometer，56 测试全绿，独立编译
- [x] 2.6 新建 `gateway-audit`，迁移调用日志/审计事件（14 生产文件 + 2 测试）。依赖 common/provider/iam，5 测试全绿，独立编译
- [x] 2.7 新建 `gateway-stats`，迁移聚合统计/仪表盘（2 文件，依赖 provider/iam 直连仓库计数）
- [x] 2.8 新建 `gateway-alert`，迁移告警（4 文件，依赖 common/usage/iam）。另修复 2 个误置于 infrastructure.alert 的测试（IpBlock/SensitiveDataRule）迁回 gateway-security 正确包
- [x] 2.9 评估合并弱内聚域：按设计维持 6 个中基模块（stats/alert 独立，未合并），experience 暂留 boot（薄应用门面，与 proxy 强相关，仅 4 文件）
- [x] 2.10 中基模块（usage/security/resilience/audit/stats/alert）各自独立编译 + ArchUnit 通过。所有已拆 8 模块 domain 层 0 依赖 infrastructure（无冻结掩盖），全仓 13 模块 clean install 通过，boot 433 surefire + failsafe 全绿

## 4. Phase 3：上层拆分

- [x] 3.1 新建 `gateway-proxy`，迁移 ChatDispatchService/routing（RoutingResolver 等）/invoker（Channel/KeyFailoverInvoker）18 文件 + ProtocolConversionFacade + ProtocolStreamConverter + 15 测试。依赖 8 模块（common/iam/audit/protocol/provider/usage/resilience），102 测试全绿
- [x] 3.2 确认 proxy 经 `ProtocolConversionFacade` 编排：仅通过 ProtocolAdapter SPI 动态装配，无任何具体 OpenAI/Anthropic Adapter 代码引用（仅 Javadoc 举例）
- [x] 3.3 核心代理调度迁出 `gateway-boot`，boot 保留 adapter/管理端应用服务（catalog/channel/model 等）/config + Phase 4 协议 Adapter；ArchUnit 通过，全仓 14 模块 clean install + boot 331 surefire + failsafe 全绿

## 5. Phase 4：协议能力插件化

- [x] 4.1 新建 `gateway-protocol-openai`，迁移 `OpenAIProtocolAdapter`（去 @Component），`OpenAIProtocolAutoConfiguration`（@Bean 注册 + `@ConditionalOnProperty(gateway.protocol.openai.enabled)`）
- [x] 4.2 新建 `gateway-protocol-anthropic`，迁移 `AnthropicProtocolAdapter` + AutoConfiguration（ProtocolStreamConverter 已随 proxy 迁）
- [x] 4.3 `gateway-boot` 依赖 `protocol-openai/anthropic` 经 AutoConfiguration 启用；删除原 `infrastructure/protocol` 实现（Adapter 测试随迁插件）；全仓 16 模块 install + boot 322 surefire 全绿
- [x] 4.4 落 `gateway-protocol-gemini` 示例插件验证可扩展性——**验收金标准达成**：GeminiChatRequest 契约 + GeminiProtocolAdapter<GeminiChatRequest> + AutoConfiguration；ProtocolConversionFacade.convertRequest 通用化（源=request.getProtocol()，按协议名动态路由），新增 gemini 不改核心，集成测试验证 gemini→openai 转换
- [x] 4.5 健康机制：HealthIndicator/HealthGroup 已有（ProviderRegistryHealthIndicator + readiness group）；新增 `ProviderHealthProbe`（@Scheduled 周期探活：遍历启用通道→端点→凭证→UpstreamClient.testConnectivity→更新供应商级健康状态，探活间隔/初始延迟可配置）+ 3 单测

## 6. 收尾与验证

- [ ] 6.1 全仓 `mvn clean install` 通过，所有模块独立可构建
- [ ] 6.2 ArchUnit 架构测试全绿，依赖方向不回退
- [ ] 6.3 集成测试全绿（ProtocolConversionIntegrationTest、ChatDispatch 相关），迁移前后行为等价
- [ ] 6.4 JaCoCo 覆盖率不因迁移跌破门槛
- [ ] 6.5 移除 `gateway-capability-api` 旧模块残留，`openspec validate` 通过
