# Phase 3: 切换路由逻辑 — 实施计划

> 目标：将认证/路由从旧架构（GatewayApiKey → Provider → ProviderApiKey）切换到新架构（UserApiKey → Product → ProductApiKey），实现双写兼容。

## 当前架构（旧）

```
请求 → ApiKeyAuthInterceptor → AuthenticationDomainService → GatewayApiKey
     → ChannelRoutingService → RoutingDomainService → ApiKeySelectionService → ProviderApiKey
     → ProxyDomainService → ProviderCallGateway → 供应商 API
```

**关键调用链**：
1. `ApiKeyAuthInterceptor` 用 `keyHash` 查 `GatewayApiKey`，得到 `providerId`
2. `ChannelRoutingService` 用 `providerId` + `model` 查 `Provider`，选 `ProviderApiKey`
3. `RoutingContext` 携带 `providerId, model, providerApiKey, endpoint`
4. `ProxyDomainService` 用 `RoutingContext` 调用供应商

## 目标架构（新）

```
请求 → ApiKeyAuthInterceptor → AuthenticationDomainService → UserApiKey (新)
     → ChannelRoutingService → RoutingDomainService → ProductApiKey (新)
     → ProxyDomainService → ProviderCallGateway → 供应商 API
```

**关键变化**：
1. 认证：`GatewayApiKey` → `UserApiKey`（通过 `keyHash` 查找，得到 `productId`）
2. 鉴权：`UserApiKey.canAccessModel(model)` 校验模型权限
3. 路由：`ProductApiKey` 替代 `ProviderApiKey`（通过 `productId` 查找）
4. 端点：`Product.getEndpoint(protocol)` 替代 `Provider` 的固定端点
5. 审计：`UsageRecord` 增加 `userApiKeyId, teamId, productId` 字段

## 双写兼容策略

**核心思路**：新旧认证并行，优先新架构，降级到旧架构。

1. 请求到达时，先尝试用 `keyHash` 查 `UserApiKey`
2. 如果找到 → 走新路由（Product → ProductApiKey）
3. 如果未找到 → 降级走旧路由（GatewayApiKey → Provider → ProviderApiKey）
4. 所有请求的审计记录都写入新格式的 `UsageRecord`

## 任务分解

### Task 1: 扩展 RoutingContext 支持新架构

**文件**: `domain/proxy/entity/RoutingContext.java`

当前 RoutingContext 包含：
- providerId, providerName, model, protocol
- providerApiKey (String), providerApiKeyId (Long)
- endpoint (String)

需要新增：
- productId (Long) — 新架构路由依据
- productType (ProductType) — 产品类型
- userApiKeyId (Long) — 用户密钥 ID
- teamId (Long) — 团队 ID
- 保留旧字段用于降级路由

**修改方式**：在现有字段基础上添加新字段，旧字段标记 `@Deprecated` 但不删除。

### Task 2: 创建 ProductRoutingService（新路由服务）

**新文件**: `application/proxy/ProductRoutingService.java`

职责：
- 接收 `UserApiKey` + `model` + `protocol`
- 查 `Product`（通过 `UserApiKey.productId`）
- 校验模型权限（`Product.containsModel(model)` 且 `UserApiKey.canAccessModel(model)`）
- 选 `ProductApiKey`（复用现有 weight/priority 策略逻辑）
- 构建 `RoutingContext`（新字段填充）

**策略**：将 `ApiKeySelectionService` 的 weight/priority 选择逻辑抽象为通用方法，`ProductRoutingService` 和旧路由都可复用。

### Task 3: 修改 AuthenticationDomainService 支持双路认证

**文件**: `domain/security/service/AuthenticationDomainService.java`

当前逻辑：
```java
authenticate(keyHash) → 查 GatewayApiKey → 返回 UserAuthResult(providerId, ...)
```

修改为：
```java
authenticate(keyHash) → {
  1. 先查 UserApiKey (新)
  2. 找到 → 返回 UserAuthResult(productId, userApiKeyId, teamId, isNewArchitecture=true)
  3. 未找到 → 查 GatewayApiKey (旧)
  4. 找到 → 返回 UserAuthResult(providerId, isNewArchitecture=false)
  5. 未找到 → 认证失败
}
```

**UserAuthResult 扩展**：
- 新增 `productId`, `userApiKeyId`, `teamId`, `isNewArchitecture` 字段
- 保留旧字段 `providerId` 用于降级

### Task 4: 修改 ChannelRoutingService 支持双路路由

**文件**: `application/proxy/ChannelRoutingService.java`

当前逻辑：
```java
route(authResult, model, protocol) → RoutingDomainService.route(providerId, model, protocol)
```

修改为：
```java
route(authResult, model, protocol) → {
  if (authResult.isNewArchitecture()) {
    ProductRoutingService.route(userApiKeyId, productId, model, protocol)
  } else {
    RoutingDomainService.route(providerId, model, protocol)  // 旧路径
  }
}
```

### Task 5: 修改 ProxyServiceImpl 整合新路由

**文件**: `application/proxy/ProxyServiceImpl.java`

当前逻辑：
```java
proxy(request) → {
  1. authenticate(keyHash)
  2. route(authResult, model, protocol)
  3. call(routingContext, request)
  4. recordUsage(...)
}
```

修改：
- `recordUsage` 增加 `userApiKeyId, teamId, productId` 参数
- `RoutingContext` 新字段传递到 `ProviderCallGateway`（端点选择逻辑适配）

### Task 6: 扩展 UsageRecord 和审计记录

**文件**:
- `domain/audit/entity/UsageRecord.java` — 新增 `userApiKeyId`, `teamId`, `productId` 字段
- `infrastructure/audit/gateway/database/dataobject/UsageRecordDo.java` — 对应 JPA 实体
- `db/migration/V18__add_usage_record_product_fields.sql` — 数据库迁移

### Task 7: 修改 ProviderCallService 适配新端点选择

**文件**: `infrastructure/proxy/gateway/rpc/ProviderCallService.java`

当前逻辑：从 `RoutingContext` 获取 `endpoint`（已由路由层决定）

修改：无需大改，`RoutingContext.endpoint` 已由路由层填充。但需确认 `Product.getEndpoint(protocol)` 返回的 URL 格式与旧格式兼容。

### Task 8: 修改 ApiKeyAuthInterceptor 传递新认证信息

**文件**: `adapter/interceptor/ApiKeyAuthInterceptor.java`

当前逻辑：将 `UserAuthResult` 放入请求属性

修改：确保新的 `UserAuthResult` 字段（`productId`, `userApiKeyId`, `teamId`, `isNewArchitecture`）也传递到后续处理链。

### Task 9: 数据迁移脚本 — 为现有 GatewayApiKey 创建对应的 UserApiKey

**文件**: `db/migration/V19__migrate_gateway_api_keys_to_user_api_keys.sql`

逻辑：
1. 为每个 `GatewayApiKey` 创建对应的 `UserApiKey`
2. 关联到默认团队和对应产品
3. 保留旧数据不删除（双写兼容期）

### Task 10: 编译验证 + 集成测试

运行 `./mvnw compile -pl gateway-boot -DskipTests` 确保编译通过。

## 实施顺序

```
Task 1 (RoutingContext) → Task 3 (认证) → Task 2 (新路由服务) → Task 4 (路由分发)
→ Task 5 (ProxyService) → Task 8 (拦截器) → Task 6 (审计) → Task 7 (端点适配)
→ Task 9 (数据迁移) → Task 10 (验证)
```

## 风险与回滚

- **风险**：双路认证增加复杂度，需确保旧路径不受影响
- **回滚**：每个 Task 独立提交，可通过 `git revert` 回滚单个 Task
- **测试**：双写兼容期，新旧路径都需手动验证
