# Resilience Profile Delta Spec

> ResilienceProfile 实体降级：删除 L2/PinnedModel/会话亲和后只剩 timeout，直接挂 Application，不独立成实体。

## REMOVED Requirements

### Requirement: ResilienceProfile 容灾画像实体
**Reason**: 删除 L2/PinnedModel/会话亲和后，ResilienceProfile 只剩 `timeout` 一个字段，不配独立实体。`timeout` 直接挂 `Application` 字段，ResilienceProfile 实体与 `resilience_profiles` 表退场。
**Migration**: `ResilienceProfile` 实体、`ResilienceProfileGateway`/Impl、`resilience_profiles` 表、`ResilienceProfileController`、`ResilienceProfileApplier` 整删。`Application.resilienceProfileId` 关联移除，新增 `Application.timeout` 字段。

### Requirement: 解析链 Application → Global
**Reason**: ResilienceProfile 实体退场，解析链失去载体。timeout 直接从 Application 读取。
**Migration**: `ResilienceResolver` 整删。timeout 由 `Application.timeout` 直接提供，无解析链。

### Requirement: 容灾模式档位推导
**Reason**: ResilienceProfile 退场，档位（default/strict/aggressive/batch）推导失去载体。L2 已删（strict 档关 L2 的语义无意义），PinnedModel/会话亲和删，档位无可推导字段。
**Migration**: `ResilienceProfileApplier` 整删，seed 数据（`V56__seed_resilience_profiles.sql`）移除。

### Requirement: 会话亲和
**Reason**: LLM 调用多为无状态（每次带完整上下文），会话亲和收益依赖上游 prompt caching 等机制且不确定，复杂度不低。延后至确认有缓存命中收益再做。
**Migration**: `SessionAffinityStore`（Redis/InMemory 双实现）、`SessionAffinityConfig`、画像 `enableSessionAffinity`/`sessionAffinityTtlMinutes` 字段整删。

### Requirement: 画像门禁 L2 降级
**Reason**: L2 模型降级删除，画像 L2 门禁失去对象。
**Migration**: `enableL2ModelDegradation`/`degradationMaxDepth` 字段删除。

### Requirement: 画像解析 fail-open
**Reason**: ResilienceProfile 退场，画像解析 fail-open 机制失去载体。timeout 直接从 Application 读取（Application 必然存在，无需 fail-open）。
**Migration**: `InstanceSelector.resolveProfileSafely`/`ChatDispatchServiceImpl.resolveProfileSafely` 整删。
