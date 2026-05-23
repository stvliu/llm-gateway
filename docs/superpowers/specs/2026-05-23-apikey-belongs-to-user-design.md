# 密钥归属重构：密钥归于用户

## 背景

当前 `UserApiKey` 同时持有 `teamId` 和 `userId`，密钥"脚踏两只船"，导致：
1. 用户列表无法直接管理密钥——必须先选团队
2. 概念混淆——密钥的归属（谁创建的）和权限边界（能调什么）耦合在一起
3. 与现实世界模型不符——OpenAI/Anthropic 的 Key 属于用户/账号，权限由独立策略控制

## 核心模型变更

**重构前：**
```
UserApiKey { teamId, userId, productIds }
密钥属于团队，团队决定权限
```

**重构后：**
```
UserApiKey { userId, productIds }   ← 去掉 teamId
密钥属于用户，权限由 productIds 定义
```

**领域关系：**
```
用户 ──属于──→ 团队      （组织关系，管人、管预算）
密钥 ──属于──→ 用户      （身份归属，创建、使用）
密钥 ──关联──→ 产品      （权限边界，能调什么模型）
用量 ──归集──→ 团队      （通过 userId → UserTeam → teamId 推导）
```

团队不再直接出现在 Key 的数据上，而是通过用户关系间接影响密钥（预算约束、可见性范围）。

## 后端变更

### 1. 领域实体

**UserApiKey** — 移除 `teamId` 字段：
```java
public class UserApiKey {
    private Long id;
    // private Long teamId;  ← 删除
    private Long userId;
    private List<Long> productIds;
    private String keyHash;
    private String keyPrefix;
    private String keyPlain;
    private String name;
    private List<String> models;
    private Long quotaLimit;
    private UserApiKeyState state;
    private Instant createdAt;
    private Instant updatedAt;
}
```

### 2. 数据库迁移

新增迁移脚本 `V32__drop_team_id_from_user_api_keys.sql`：
```sql
ALTER TABLE user_api_keys DROP COLUMN team_id;
```

### 3. Gateway 接口变更

**UserApiKeyGateway** — 移除 teamId 相关方法：
```java
public interface UserApiKeyGateway {
    Optional<UserApiKey> findById(Long id);
    // List<UserApiKey> findByTeamId(Long teamId);  ← 删除
    List<UserApiKey> findByUserId(Long userId);
    Optional<UserApiKey> findByKeyPrefix(String keyPrefix);
    UserApiKey save(UserApiKey userApiKey);
    void deleteById(Long id);
    // long countByTeamId(Long teamId);  ← 删除
    List<Long> findIdsByProductId(Long productId);
}
```

### 4. 认证链路变更

**AuthenticationDomainService** — 认证结果不再携带 teamId：
```java
// 重构前
public record UserAuthResult(Long userId, String role, Long userApiKeyId, Long teamId)

// 重构后
public record UserAuthResult(Long userId, String role, Long userApiKeyId)
```

认证后 teamId 的获取方式改为：`userId → UserTeam → teamId`，由需要团队上下文的服务自行查询。

### 5. 路由上下文变更

**RoutingContext** — 移除 teamId 字段，路由不再依赖团队：
```java
public class RoutingContext {
    // private Long teamId;  ← 删除
    // 其他字段不变
}
```

**ProductRoutingService.resolve()** — 不再设置 teamId：
```java
return RoutingContext.builder()
    .providerId(product.getProviderId())
    .providerName(provider.getName())
    .productId(product.getId())
    .productType(product.getProductType())
    .userApiKeyId(userApiKey.getId())
    // .teamId(userApiKey.getTeamId())  ← 删除
    .model(model)
    .protocol(resolved.protocolName)
    .providerApiKey(plainApiKey)
    .providerApiKeyId(apiKey.getId())
    .endpoint(resolved.endpointUrl)
    .build();
```

### 6. 应用服务变更

**UserApiKeyService / UserApiKeyServiceImpl**：
- `create()` — 不再设置 teamId，CreateRequest 去掉 teamId
- `listByTeamId()` — 删除此方法
- `getDetailByIdAndTeamId()` — 删除此方法（不再需要团队归属校验）
- `findByUserId()` — 保留，这是主查询维度

**UserApiKeyCreateRequest**：
```java
// 重构前
public record UserApiKeyCreateRequest(Long teamId, Long userId, List<Long> productIds, ...)

// 重构后
public record UserApiKeyCreateRequest(Long userId, List<Long> productIds, String name, ...)
```

**UserApiKeyResponse / UserApiKeyDetailResponse** — 去掉 teamId 字段。

### 7. Controller 变更

**UserApiKeyController**：
- `GET /api/v1/user-api-keys/team/{teamId}` — 删除（不再有团队维度查询）
- 其他 CRUD 不变

**TeamController**：
- `GET /api/v1/teams/{teamId}/api-keys` — 改为查询该团队成员的所有 Key（通过 userId → UserTeam 推导）
- `GET /api/v1/teams/{teamId}/api-keys/{id}` — 删除团队归属校验
- `POST /api/v1/teams/{teamId}/api-keys` — 保留，但语义变为"在团队上下文中为成员创建 Key"，创建的 Key 不绑定 teamId

**UserController**：
- `GET /api/v1/users/{userId}/api-keys` — 保留，这是用户维度主查询

**MeController**：
- `GET /api/v1/me/api-keys` — 保留，当前用户的 Key 列表

### 8. 用量统计变更

**TokenUsedEvent / AuditEvent / UsageLogDo** — teamId 不再从 Key 直接获取，改为从 userId 推导：

- 用量事件发布时，通过 userId 查询 UserTeam 获取 teamId
- 用户属于多个团队时，取主团队（`UserTeam` 中 `role = owner` 的团队，或第一个团队）归集用量

### 9. DataInitializer

种子数据中 UserApiKey 不再设置 teamId。

## 前端变更

### 1. 类型定义

**team.ts** — UserApiKey 去掉 teamId：
```typescript
export interface UserApiKey {
  id: number;
  // teamId: number;  ← 删除
  userId: number;
  productIds: number[];
  keyPrefix: string;
  name: string;
  models: string[];
  quotaLimit: number | null;
  state: 'ACTIVE' | 'DISABLED';
  createdAt: string;
  updatedAt: string;
}
```

**CreateUserApiKeyRequest** — 去掉 teamId：
```typescript
export interface CreateUserApiKeyRequest {
  // teamId: number;  ← 删除
  userId: number;
  productIds: number[];
  name: string;
  models?: string[];
  quotaLimit?: number | null;
}
```

### 2. API 服务

**team.ts** — API Key 相关方法调整：
- `listApiKeys(teamId)` — 改为查询团队成员的 Key（后端实现变更，前端接口不变）
- `createApiKey(teamId, data)` — URL 保持不变（团队上下文创建），但 request body 不再含 teamId
- `listUserApiKeys(userId)` — 保留，这是用户维度主查询

**user.ts** — 新增：
- `listApiKeys(userId)` — `GET /users/{userId}/api-keys`

### 3. 用户列表页 — 新增密钥管理入口

**Users/index.tsx** — 操作列增加"密钥管理"按钮：
- 点击打开 `UserApiKeyModal`，传入 userId
- 弹窗内展示该用户的 Key 列表（名称、前缀、关联产品、状态、操作）
- 支持创建/编辑/删除 Key
- 创建时不需要选团队，直接选产品即可

### 4. 新增 UserApiKeyModal 组件

**Users/UserApiKeyModal.tsx**：
- Props: `{ userId: number, open: boolean, onClose: () => void }`
- 列表数据: `GET /api/v1/users/{userId}/api-keys`
- 创建: `POST /api/v1/user-api-keys`（body 不含 teamId）
- 编辑: `PUT /api/v1/user-api-keys/{id}`
- 删除: `DELETE /api/v1/user-api-keys/{id}`
- 创建表单字段：名称、关联产品、可用模型、额度限制

### 5. Teams 页面密钥管理调整

**Teams/UserApiKeyManageModal.tsx**：
- 创建表单去掉 teamId（已从 request 中移除）
- Key 列表不再显示 teamId 列

### 6. i18n 补充

**users.json** — 补充密钥管理相关文案（已有部分，需补充弹窗相关）。

## 测试变更

### 后端测试
- `UserApiKeyServiceImplTest` — 去掉 teamId 相关断言
- `TeamControllerUserApiKeyTest` — 调整团队维度 Key 查询的预期
- `AuthenticationDomainServiceTest` — UserAuthResult 不再含 teamId
- `UserAuthResultTest` — 同上
- 新增：`UserController` 用户维度 Key 查询测试

### 前端测试
- 用户列表页密钥管理弹窗测试
- UserApiKeyModal 组件测试

## 迁移风险

1. **数据库迁移不可逆** — `DROP COLUMN team_id` 需确保所有代码不再引用
2. **现有数据** — 如果生产环境已有含 teamId 的 Key 数据，迁移前需确认数据兼容策略
3. **用量统计** — teamId 获取路径变长（Key → userId → UserTeam → teamId），需确保查询性能

## 实施顺序

1. 后端领域模型变更（实体、Gateway、Service）
2. 数据库迁移脚本
3. 认证链路调整
4. 路由上下文调整
5. Controller 调整
6. 后端测试修复
7. 前端类型定义调整
8. 前端 API 服务调整
9. 用户列表页新增密钥管理入口
10. Teams 页面调整
11. 前端测试
