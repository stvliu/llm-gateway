---
comet_change: key-application-binding
role: technical-design
canonical_spec: openspec
archived-with: 2026-07-09-key-application-binding
status: final
---

# Design Doc: key-application-binding

## Context

`application` spec 与 `application-access-control` spec 已规定 Application 为权限锚点、`UserApiKey.applicationId` 为权限锚点字段、`applicationId` 为 null 时 `PermissionRouter` 返回空集。持久化层（`UserApiKeyDo` + `UserApiKeyGatewayImpl` 的 `toEntity:99`/`toDataObject:124`）已完整读写 `applicationId`。断层在应用层：`UserApiKeyServiceImpl.create()` 不调 `setApplicationId`、5 个 DTO 不暴露该字段、`UserApiKeyGateway` 无 `findByApplicationId`。结果管理 API 创建的 Key `applicationId` 永远为 null，路由时返回空渠道集，Key 不可用。前端停留在已归档的「团队继承」模型。重置密码前端有按钮后端无端点（404）。

本设计补齐 spec 已规定但实现缺失的管理面能力，不引入新架构。

## Goals / Non-Goals

**Goals**: 管理 API 完整暴露/操作 `applicationId`（创建必填、补绑/转移、响应字段、按应用查询）；修复重置密码 404；前端对齐 Application 锚点模型；删除 Application 防悬空。

**Non-Goals**: 不实现 rotate 端点；不做 Key 启用/禁用/有效期；不做用户搜索/筛选/分页后端化、Application 分页/状态切换、批量操作、独立详情页；不自动迁移存量 null Key；不改路由层。

## 技术方案

### 后端 DTO（record 扩展字段）

```
UserApiKeyCreateRequest(userId @NotNull, applicationId @NotNull, name @NotBlank)
UserApiKeyUpdateRequest(applicationId, name)              // 均可选
UserApiKeyResponse(id, userId, applicationId, keyPrefix, keyPlain, name, createdAt, updatedAt)
UserApiKeyDetailResponse(同 Response)
UserApiKeyCreateResponse(id, keyPrefix, apiKeyPlain)     // 保持不变
```

### UserApiKeyServiceImpl

- 新增依赖 `ApplicationGateway`（构造器注入）
- `create()`：`applicationGateway.findById(request.applicationId()).orElseThrow(ResourceNotFound)` → `apiKey.setApplicationId(request.applicationId())`
- `update()`：若 `request.applicationId() != null`，校验存在后 `apiKey.setApplicationId(...)`
- `toResponse/toDetailResponse`：加 `applicationId` 映射

### UserApiKeyGateway + Impl

- 接口加 `List<UserApiKey> findByApplicationId(Long applicationId)`
- Impl：`repository.findByApplicationId(applicationId).stream().map(this::toEntity).toList()`
- Repository 加 `findByApplicationId` 派生查询
- `toEntity/toDataObject` 已映射 applicationId，无需改

### ApplicationController + ApplicationServiceImpl

- `ApplicationController` 注入 `UserApiKeyService`，加 `GET /api/v1/applications/{id}/api-keys` → `userApiKeyService.findByApplicationId(id)`
- `ApplicationServiceImpl.delete` 注入 `UserApiKeyGateway`（或 Service），删除前 `findByApplicationId(id)`，非空抛 `ConflictException`（4xx）

### UserController + UserService（重置密码）

- `POST /api/v1/users/{id}/reset-password` → `userService.resetPassword(id)` 返回 `{newPassword}`
- `resetPassword(userId)`：
  - 查用户，不存在抛 404
  - `builtin=true` 抛 4xx
  - 生成 16 位随机密码（字符集 `ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789`，排除 O/0/I/1/l）
  - 密码哈希（复用现有 `PasswordEncoder`）更新
  - 返回明文 `{newPassword}`，不持久化明文

### 前端

- `types/userApiKey.ts`：`UserApiKey`/`CreateUserApiKeyRequest`/`UpdateUserApiKeyRequest` 加 `applicationId`，修正注释
- `services/api/userApiKey.ts`：移除 `rotate`，加 `listByApplication(applicationId)` → `GET /applications/{id}/api-keys`
- `DownstreamKeysTable`：创建表单加 Application Select（必填，showSearch）；列表加「所属应用」列；顶部加按应用筛选 Select；支持 URL `?applicationId=` 初始化筛选
- `UserApiKeyModal`：删除「团队继承」Alert；创建表单加 Application Select；编辑表单加 Application Select（补绑）
- `Applications/index.tsx`：行操作加「查看 Key」图标 → `navigate('/keys?applicationId=' + id)`
- `services/api/user.ts`：`resetPassword` 保留不变

## 测试策略

| 层 | 测试 | 覆盖 |
|---|---|---|
| 单测 | `UserApiKeyServiceImplTest` | create 必填落库、Application 存在性校验、update 补绑、响应含 applicationId |
| 单测 | `ApplicationServiceImplTest` | delete 有 Key 引用抛 Conflict、无引用正常删 |
| 单测 | `UserServiceImplTest` | resetPassword 生成 16 位、内建用户拒绝、不存在用户 404 |
| 集成 | `ApplicationController` IT | GET /applications/{id}/api-keys、DELETE 冲突 |
| 集成 | `UserController` IT | reset-password 成功 + 内建拒绝 |
| 路由回归 | `ChatDispatchService` IT | 带 applicationId 的 Key 路由返回非空渠道集（回归根因） |

## 边界条件

- **并发补绑同一 Key**：当前无版本字段，最后写入胜出。非本次加锁范围，记录为已知行为
- **删除 Application 与补绑竞争**：删除前置校验 + 事务隔离。极端并发下 Key 可能指向已删应用，`PermissionRouter` 查 `ApplicationChannel` 为空 → Key 不可路由（不 panic，符合现有空集语义）
- **内建用户重置密码**：`resetPassword` 检查 `builtin`，拒绝
- **applicationId 引用已删除应用**：删除前置校验保证不发生；并发场景见上

## 风险与取舍

- [CreateRequest 加必填字段 BREAKING] → 前端唯一调用方，同步改造；无 CLI/外部调用方
- [DELETE 加前置校验 BREAKING] → 管理员需先转移/删除 Key；前端冲突提示
- [存量 null Key 仍不可路由] → 补绑入口上线后管理员手动处理
- [重置密码返回明文] → 一次性返回不持久化，HTTPS 传输，与 UserApiKeyCreate 模式一致

## 迁移计划

1. 后端先行：DTO → Service → Gateway → Controller + 单测/集成测试
2. 前端跟进：类型 → 表单 → 列表 → 删 Alert → 移除 rotate → 查看入口
3. 存量 null Key：管理员通过补绑入口手动处理
4. 回滚：整体 revert change 分支，无 schema 变更，回滚无副作用

## Open Questions

均已在 brainstorming 解决：
- 重置密码字符集：16 位排除易混（O/0/I/1/l）
- 查看 Key 入口：跳转 `/keys?applicationId=<id>` 触发筛选
- Application 存在性校验层：Service 层注入 ApplicationGateway
