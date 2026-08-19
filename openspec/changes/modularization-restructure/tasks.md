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

- [ ] 2.1 新建 `gateway-security`，迁移 threat（IP/威胁检测）+ dataprotection（脱敏）
- [ ] 2.2 拦截链澄清：跨 auth+threat+quota 的编排放应用层，security 不反向依赖 usage
- [ ] 2.3 新建 `gateway-usage`，迁移 TokenLimit/配额/用量事件/限流执行（写路径）
- [ ] 2.4 明确 usage 明细表归属，暴露稳定只读查询接口给 stats
- [ ] 2.5 新建 `gateway-resilience`，迁移 failover/retry/circuit-breaker
- [ ] 2.6 新建 `gateway-audit`，迁移调用日志/审计事件
- [ ] 2.7 新建 `gateway-stats`，迁移聚合统计/仪表盘（读路径，经 usage 只读接口，不写用量）
- [ ] 2.8 新建 `gateway-alert`，迁移告警
- [ ] 2.9 评估合并弱内聚域（alert/experience/stats），控制业务模块数 8~10
- [ ] 2.10 中基模块各自独立编译 + ArchUnit 通过

## 4. Phase 3：上层拆分

- [ ] 3.1 新建 `gateway-proxy`，迁移 ChatDispatchService/routing（RoutingResolver/ModelMatcher/ChannelSelector/CredentialResolver/EndpointResolver）/invoker（ChannelFailoverInvoker/KeyFailoverInvoker）/domain/proxy
- [ ] 3.2 确认 proxy 经 `ProtocolConversionFacade` 编排，不依赖任何具体能力插件
- [ ] 3.3 `gateway-boot` 仅剩组装代码，业务逻辑全部迁出 + ArchUnit 通过

## 5. Phase 4：协议能力插件化

- [ ] 4.1 新建 `gateway-protocol-openai`，迁移 `OpenAIProtocolAdapter`，实现 `ProtocolAdapter` SPI + `OpenAIProtocolAutoConfiguration`（`@Bean` 注册 + `@ConditionalOnProperty`）
- [ ] 4.2 新建 `gateway-protocol-anthropic`，迁移 `AnthropicProtocolAdapter` + `ProtocolStreamConverter`
- [ ] 4.3 `gateway-boot` 依赖 `protocol-openai/anthropic`，通过 AutoConfiguration 启用；删除原 `infrastructure/protocol` 实现；`mvn test` 全绿
- [ ] 4.4 落 `gateway-protocol-gemini` 示例插件验证可扩展性（含 Adapter + AutoConfiguration + DB 配置）——**验收金标准：新增协议仅新增插件模块 + DB 配置，不改核心**
- [ ] 4.5 健康机制：HealthIndicator/HealthGroup + `@Scheduled` 周期探活

## 6. 收尾与验证

- [ ] 6.1 全仓 `mvn clean install` 通过，所有模块独立可构建
- [ ] 6.2 ArchUnit 架构测试全绿，依赖方向不回退
- [ ] 6.3 集成测试全绿（ProtocolConversionIntegrationTest、ChatDispatch 相关），迁移前后行为等价
- [ ] 6.4 JaCoCo 覆盖率不因迁移跌破门槛
- [ ] 6.5 移除 `gateway-capability-api` 旧模块残留，`openspec validate` 通过
