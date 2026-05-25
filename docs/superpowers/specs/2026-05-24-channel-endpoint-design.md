# ChannelEndpoint 设计规格

> 日期: 2026-05-24
> 状态: Draft
> 分支: refactor/template-to-metadata

## 1. 背景与动机

国内主流云厂商（火山引擎、腾讯、阿里、星火）的 Coding Plan / Token Plan 套餐，都同时提供两个 BaseUrl：

- 一个兼容 OpenAI 协议（`/v1/chat/completions`）
- 一个兼容 Anthropic 协议（`/v1/messages`）

当前 Channel 实体设计为 `一个 Channel = 一个协议端点`（endpointUrl + protocol 扁平字段），导致同一个套餐被拆成两个 Channel，造成凭证冗余、模型关联冗余、计费/额度分裂、管理复杂度倍增。

Channel 应该是"逻辑接入点"——一个渠道就是一个套餐/一个计费单位/一组模型，而协议端点是渠道的子属性，不是渠道本身。

## 2. 方案决策

| 方案 | 描述 | 优点 | 缺点 |
|------|------|------|------|
| A. 保持现状 | 多协议建多个 Channel | 改动最小 | 凭证/模型/额度冗余，管理 O(渠道×协议) |
| B. Map<Protocol, String> endpoints | Channel 持有 JSON endpoints 字段 | 无冗余 | JPA 查询不便，设计规格已否决 |
| **C. ChannelEndpoint 实体** | 独立实体，Channel 拥有多个 ChannelEndpoint | 无冗余、JPA 友好、额度共享 | 多一层实体/一张表 |

**选择方案 C**。理由：

1. ChannelEndpoint 是最小粒度的协议接入点——只回答"用什么协议、调哪个 URL"
2. Channel 回答"这是一个什么套餐、有什么模型、用什么凭证、多少额度"
3. 路由时先选 Channel（模型匹配+凭证选择），再从 endpoints 中选匹配入站协议的 ChannelEndpoint
4. 如果 Channel 没有匹配的端点，再降级做跨协议转换

## 3. 实体设计

### 3.1 实体关系

```
Provider (1) → (N) Channel (1) → (N) ChannelEndpoint
                       (1) → (N) ChannelCredential
                       (1) → (N) ChannelModel → ModelSpec
```

### 3.2 Channel 实体（精简）

```java
public class Channel extends BaseEntity {
    private Long providerId;
    private String name;
    private BillingMode billingMode;
    private Long quotaLimit;
    private Integer priority;
    private Integer weight;
    private Integer timeout;
    private Integer maxRetries;
    private ChannelState state;
}
```

删除 `endpointUrl` 和 `protocol`——下沉到 ChannelEndpoint。

### 3.3 ChannelEndpoint 实体（新增）

```java
public class ChannelEndpoint extends BaseEntity {
    private Long channelId;
    private Protocol protocol;
    private String endpointUrl;
    private ChannelEndpointState state;
}
```

极简——只做一件事：声明一个协议端点。

### 3.4 ChannelEndpointState 枚举（新增）

```java
public enum ChannelEndpointState {
    ACTIVE, DISABLED
}
```

## 4. 数据库设计

### 4.1 channel_endpoints 表

```sql
CREATE TABLE channel_endpoints (
    id BIGSERIAL PRIMARY KEY,
    channel_id BIGINT NOT NULL REFERENCES channels(id) ON DELETE CASCADE,
    protocol VARCHAR(32) NOT NULL,
    endpoint_url VARCHAR(512) NOT NULL,
    state VARCHAR(16) DEFAULT 'ACTIVE',
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_by BIGINT,
    updated_at TIMESTAMP DEFAULT NOW(),
    CONSTRAINT uk_channel_endpoint UNIQUE (channel_id, protocol)
);

CREATE INDEX idx_channel_endpoints_channel ON channel_endpoints(channel_id);
CREATE INDEX idx_channel_endpoints_protocol ON channel_endpoints(protocol);
```

`UNIQUE (channel_id, protocol)` — 一个渠道下同一协议只有一个端点。

### 4.2 channels 表变更

```sql
-- 删除 channels 上的 endpoint_url 和 protocol 列
ALTER TABLE channels DROP COLUMN endpoint_url;
ALTER TABLE channels DROP COLUMN protocol;
```

### 4.3 数据迁移

将 channels 表现有的 endpoint_url 和 protocol 迁移到 channel_endpoints 表：

```sql
INSERT INTO channel_endpoints (channel_id, protocol, endpoint_url, state, created_at, updated_at)
SELECT id, protocol, endpoint_url, 'ACTIVE', created_at, updated_at
FROM channels
WHERE endpoint_url IS NOT NULL;
```

## 5. Gateway 接口设计

### 5.1 ChannelEndpointGateway（新增）

```java
public interface ChannelEndpointGateway {
    ChannelEndpoint save(ChannelEndpoint endpoint);
    Optional<ChannelEndpoint> findById(Long id);
    List<ChannelEndpoint> findByChannelId(Long channelId);
    List<ChannelEndpoint> findActiveByChannelId(Long channelId);
    Optional<ChannelEndpoint> findByChannelIdAndProtocol(Long channelId, Protocol protocol);
    List<ChannelEndpoint> findAll();
    void delete(ChannelEndpoint endpoint);
}
```

### 5.2 ChannelGateway 变更

删除 protocol 相关查询方法（`findByProviderIdAndProtocol` 等），因为 protocol 不再是 Channel 的属性。

### 5.3 ChannelDomainService 变更

新增 `resolveEndpoint` 方法：

```java
/**
 * 根据入站协议解析渠道端点
 * 优先匹配同名协议端点，无匹配则降级选第一个可用端点
 */
ChannelEndpoint resolveEndpoint(Channel channel, Protocol inboundProtocol);
```

## 6. RoutingContext 演进

```java
// 之前
record RoutingContext(
    Long channelId, String endpoint, Protocol protocol,
    String providerApiKey, RoutingStrategy strategy, Integer timeout
)

// 之后
record RoutingContext(
    Long channelId,
    Long channelEndpointId,        // 新增：具体端点 ID
    String endpointUrl,
    Protocol upstreamProtocol,      // 重命名：明确是上游协议
    String providerApiKey,
    Integer timeout,
    boolean needsProtocolAdaptation // 新增：是否需要跨协议转换
)
```

`needsProtocolAdaptation` 由路由层在 `resolveEndpoint` 时决定——选到的 ChannelEndpoint.protocol 与入站协议不同则为 true。

## 7. 路由逻辑演进

```
请求进入（入站协议 = inboundProtocol）
  → SupplyRoutingService.resolve(identity, model, inboundProtocol)
    → 1. UserApiKey → channelIds
    → 2. matchChannel(channelIds, model) → Channel
    → 3. selectChannelCredential(channel) → ChannelCredential
    → 4. resolveEndpoint(channel, inboundProtocol) → ChannelEndpoint 或 null
         优先：找到 protocol == inboundProtocol 的 ChannelEndpoint → 直接调用
         降级：没有匹配的端点 → 选第一个可用的 ChannelEndpoint → 需跨协议转换
    → 5. 构建 RoutingContext
```

路由场景矩阵：

| 场景 | Channel.endpoints | 入站协议 | 路由结果 | 需要转换 |
|------|-------------------|----------|----------|----------|
| Anthropic 客户端 → 火山引擎 | {OPENAI, ANTHROPIC} | ANTHROPIC | 选 ANTHROPIC 端点 | 否 |
| OpenAI 客户端 → 火山引擎 | {OPENAI, ANTHROPIC} | OPENAI | 选 OPENAI 端点 | 否 |
| Anthropic 客户端 → 只有 OpenAI 的渠道 | {OPENAI} | ANTHROPIC | 降级选 OPENAI | 是 |
| OpenAI 客户端 → Claude API | {ANTHROPIC} | OPENAI | 降级选 ANTHROPIC | 是 |

## 8. 影响范围

| 层 | 改动 |
|----|------|
| domain/supply/entity | Channel 删除 endpointUrl/protocol；新增 ChannelEndpoint 实体 |
| domain/supply/enums | 新增 ChannelEndpointState |
| domain/supply/gateway | 新增 ChannelEndpointGateway；ChannelGateway 删除 protocol 相关查询 |
| domain/supply/valueobject | RoutingContext 增加 channelEndpointId + needsProtocolAdaptation |
| domain/supply/service | ChannelDomainService 增加 resolveEndpoint 逻辑 |
| infrastructure/supply | 新增 ChannelEndpointDo/Repository/GatewayImpl；ChannelDo 删除 endpointUrl/protocol |
| application/proxy | SupplyRoutingService.resolve 增加 resolveEndpoint 步骤 |
| application/init | DataInitializer 适配 ChannelEndpoint 创建 |
| adapter/api | ChannelController 增加端点管理端点 |
| DB migration | V35 重写：创建 channel_endpoints 表 + 数据迁移 + channels 列变更 |
| test | 更新所有受影响的测试 |

## 9. 与设计规格的偏差说明

设计规格中写的是：

> endpoints: Map<Protocol, String> → endpointUrl + protocol 扁平字段。一个渠道对应一个端点一个协议，多协议需求通过建多个 Channel 解决。

这个决策在国内云厂商 Coding Plan 普遍提供双协议端点的事实下需要修正。方案 C 不是回到 Map<Protocol, String>（那是 JSON 字段，查询不便），而是用独立实体 ChannelEndpoint 来表达——既保持了关系型数据库的查询能力，又解决了"一个渠道多协议端点"的业务需求。

## 10. 成功标准

1. 一个 Coding Plan 套餐只需创建一个 Channel + 两个 ChannelEndpoint，无凭证/模型冗余
2. 入站协议匹配时直接调用对应端点，无跨协议转换开销
3. 入站协议不匹配时自动降级到跨协议转换，不报错
4. 已有的单协议渠道（如纯 OpenAI 的渠道）迁移后功能不受影响
5. 全量测试通过
