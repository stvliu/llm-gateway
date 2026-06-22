---
comet_change: resilience-architecture
role: technical-design
canonical_spec: openspec
---

# Resilience Architecture — 容灾架构与权限重构设计

> Comet change `resilience-architecture` 的 Superpowers Design Doc。详细 OpenSpec 产物见 `openspec/changes/resilience-architecture/design.md`，本文件为其镜像与补充。

## Context

网关作为「应用聚合分发层」，下游调用方是应用（Claude Code/OpenClaw/ERP）而非人。两类核心缺口：

1. **权限模型错位**：授权对象是「人/团队」（`User → Team → TeamChannel`），无法干净表达「不同应用访问不同渠道」——不同应用要不同渠道只能靠为每个应用建 Team 迁就。
2. **容灾缺口**：仅 Key 级转移 + 模型级降级两层，缺 Channel 级运行时失败转移；模型级降级对共因故障无效却被当主容灾手段；熔断 key 路由侧（channelId）与调用侧（endpointId）不一致。

## Goals / Non-Goals

**Goals:**
- P-r：Application 升级为权限+行为聚合根，移除 Team，权限锚点从 userId 改为 applicationId。
- P0：修正 RouterChain 顺序，统一熔断 key 为 endpointId。
- P1：ChannelFailoverInvoker + 候选列表路由 + 错误分流表，补齐 L1 透明容灾。
- P2：ResilienceProfile 纯数据库管理 + 显式 Cluster 实体 + 域级健康聚合。

**Non-Goals:**
- Application 配额/看板/轮换不实做（预留字段，留 quota/audit 域）。
- 不重写上游客户端与重试/熔断算法。
- 不保留应用成员管理；不独立配模型可见性（D8）。

## Decisions

- **D1**: 权限锚点从「人/团队」转为「应用」。权限链 `UserApiKey → Application → ApplicationChannel → Channel`。
- **D2**: 执行顺序 P-r → P0 → P1 → P2（四段，原 P2/P3 合并）。
- **D3**: 四层容灾栈 + 错误分流。INVALID_REQUEST 不转移，共因故障走 L1 换渠道。
- **D4**: Application 为权限+行为双聚合根（Key 归属 + 渠道可见性 + 画像），不独立配模型可见性，不保留成员。
- **D5**: 画像纯数据库管理，解析链 Application → Global，档位推导。
- **D6**: 前端 Teams → Applications 平移，移除成员管理与模型可见性 Modal。
- **D7**: 数据迁移 1:1 平移 + migration-default 兜底应用（按原 Team 渠道集授权）。
- **D8**: 模型从属渠道，砍模型可见性。废弃现有团队模型可见性机制。
- **D9**: ADMIN 退管理面，数据面 PermissionRouter 无跳过。
- **D10**: 合并 P2/P3，直接建 Cluster 实体，不经软字段。
- **D11**: 熔断 key 统一为 endpointId 采用「运行时派生」方案，不动 DB/实体。RouterChain 在 ModelInstance（channel 粒度）过滤时未绑定 endpoint，故 `RoutingRequest` 透传入站 `protocol`，`HealthRouter` 用 `channelId + protocol` 经 `EndpointResolver` 派生 `endpointId` 后查 `ChannelEndpointCircuitBreakerManager`，与 `KeyFailoverInvoker`（用 `RoutingContext.channelEndpointId()`）共享同一 manager bean。不向 `ModelInstance`/`model_instances` 表加 endpointId 字段（channel 粒度与 channel×protocol 端点粒度 1:1 不自洽）。
- **D12**: 转移事件流（容灾可观测性，读侧重）。新建独立 `FailoverEvent` domain 记录每次候选转移（from 渠道/端点 → to 渠道/端点、errorType、decision L1/L2、exhausted、traceId 串联同请求多次转移、occurredAt）。`ChannelFailoverInvoker` 在 catch 块 decision != NONE 换候选时经既有 `DomainEventPublisher` 发布 `FailoverEvent`（DomainEvent），由 `@TransactionalEventListener`(AFTER_COMMIT) 异步调 `FailoverEventGateway.save` 持久化——发布与持久化解耦，不阻塞 10k QPS 调用链。可靠性边界：发布后持久化前进程崩溃则事件丢失（可观测性数据可接受，非计费/审计关键路径）。`ResilienceEventController` 提供轮询查询端点（`GET /resilience/events` 分页 + since/applicationId/clusterId 过滤、`GET /resilience/events/exhausted` 耗尽告警），前端总览页 10s 轮询渲染。不复用 CallLog（调用结果语义与转移动作语义不同维度，混表职责模糊、查询复杂）。

## Risks / Trade-offs

- R1 数据迁移风险（高）：1:1 平移 + 兜底 + 授权集合比对校验。
- R2 权限锚点切换兼容性（高）：migration-default 软兜底。
- R3 单 change 体量大（中）：四段分阶段交付。
- R4 RouterChain 顺序 BREAKING（中）：预期修正 + 测试。
- R5 移除 Team 清理面（中）：逐项清理 + grep 确认。
- R6 砍模型可见性能力回退（中）：收敛到供给侧。
- R7 ADMIN 数据面特权移除适应（低）：全渠道调试应用替代。

## Components

- **Application** 聚合根 + ApplicationChannel（应用-渠道授权）
- **ResilienceProfile** 画像 + ResilienceResolver（解析链 Application→Global）
- **ChannelFailoverInvoker** L1 转移回路 + 错误分流表
- **Cluster** 实体 + ClusterAffinityRouter + 域级健康聚合
- **SessionAffinityStore** Redis/InMemory 双实现
- **FailoverEvent** 转移事件 domain + FailoverEventGateway + FailoverEventListener（@TransactionalEventListener 异步持久化）+ ResilienceEventController（轮询查询）
- RouterChain 改造（顺序修正 + 候选列表产出）

## Data Flow

```
请求 → ApiKeyAuthInterceptor(解析 application_id)
  → PermissionRouter(ApplicationChannel 过滤,无 ADMIN 跳过)
  → HealthRouter(endpointId 熔断) → PriorityRouter
  → 产出「应用可见+健康+按 cluster/priority 排序」候选列表
  → ChannelFailoverInvoker(候选内逐个试,实时查熔断)
     ├─ L0 KeyFailoverInvoker(同渠道换 Key)
     ├─ L1 换渠道(共因故障) ──每次换候选经 DomainEventPublisher 发布 FailoverEvent──┐
     └─ L2 degrade(reason)(模型降级,受画像门禁)                                │
  → (调用链外) FailoverEventListener @TransactionalEventListener(AFTER_COMMIT) ←─┘
     └─ FailoverEventGateway.save → failover_events 表
  → 容灾总览页 GET /resilience/events 10s 轮询 → 渲染转移事件流 + 耗尽告警
```

## Testing

- 路由器顺序、熔断 key 统一、L1 转移、错误分流、流式边界、跨 Cluster 不越权。
- ApplicationChannel 过滤、权限锚点切换、无 ADMIN 跳过、数据迁移正确性。
- 解析链、档位推导、会话亲和、画像继承、Cluster 健康聚合。
- 两对照场景（Claude Code 禁降级 / 客服全开）端到端。

## Implementation Divergence

实现过程中相对 D 系列决策的偏差记录（以实现为准，delta spec 与 `docs/容灾方案设计.md` 已据实描述）：

- **D12 转移事件流监听机制**：D12 原设想 `@TransactionalEventListener(AFTER_COMMIT)` 异步持久化。实现中改为 `@EventListener` 同步持久化。原因：调用链 `ChatDispatchServiceImpl.dispatch` 无 `@Transactional`，整个请求处理不开启事务，`@TransactionalEventListener(AFTER_COMMIT)` 在无事务上下文时（`fallbackExecution` 默认 false）会静默丢弃事件，导致转移事件全部丢失。改为 `@EventListener` 后无事务上下文下事件仍被处理。项目未配置 `@EnableAsync`，故 `@EventListener` 在同一线程内同步调用监听器完成持久化（发布即持久化），参照既有 `AuditEventListener` 范式。可靠性边界不变（发布后持久化前进程崩溃则事件丢失，可观测性数据可接受）。
- **D12 clusterId 过滤填充**：D12 未明确 clusterId 过滤的数据来源。实现中因 `RoutingContext` 无 clusterId 字段（扩展波及 14 处构造点超范围），采用 `ChannelFailoverInvoker` 注入 `ChannelGateway` 反查 `channelId → clusterId` 填充冗余 `fromClusterId/toClusterId` 字段，使 `GET /resilience/events?clusterId=` 过滤真正生效。
- **D12 traceId**：D12 设想 traceId 串联同请求多次转移。当前调用链未透传 OpenTelemetry traceId，事件 traceId 暂填 null，后续 OpenTelemetry 接入后填充。
