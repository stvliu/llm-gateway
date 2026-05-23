# 用户与 UserApiKey 关系重构设计

## 背景

当前 UserApiKey 实体同时绑定 `teamId` 和 `userId`，但前端仅在团队维度管理 Key，缺少用户维度的视角。具体问题：

1. Key 列表没有显示所属用户
2. 用户页面没有查看该用户 Key 的入口
3. 创建 Key 时未自动关联当前用户
4. 没有"我的密钥"功能

## 设计决策

- **Key 归属**：以 `userId` 为第一归属，`teamId` 为访问权限边界
- **Key 与团队**：多对一关系（多个 Key 可属于同一个团队，一个 Key 只属于一个团队）
- **方案选择**：方案 A — 以用户为主维度，团队为访问边界
- **Controller 归属**：新增用户维度端点放在 `UserController`
- **userId 来源**：前端传入 + 后端校验（普通用户只能为自己创建，管理员可为团队成员创建）
- **分组逻辑**：后端返回扁平列表 + 冗余 `teamName`，前端按 `teamId` 分组展示

## 后端 API 变更

### 新增接口

| 端点 | 方法 | 说明 | 权限 |
|------|------|------|------|
| `/api/v1/me/api-keys` | GET | 查询当前登录用户的所有 Key | 任何已登录用户 |
| `/api/v1/users/{userId}/api-keys` | GET | 查询指定用户的所有 Key | 仅 ADMIN |

### 现有接口调整

| 端点 | 变更 |
|------|------|
| `GET /api/v1/teams/{teamId}/api-keys` | 响应已包含 `userId` 字段 |
| `POST /api/v1/teams/{teamId}/api-keys` | 前端传入 `userId`，后端校验权限：普通用户只能设置自己，管理员可为团队成员设置 |

### Response DTO 变更

- `UserApiKeyResponse` 新增 `teamName` 冗余字段（便于前端分组展示）
- `UserApiKeyDetailResponse` 新增 `teamName` 冗余字段

### Gateway 新增方法

```java
// UserApiKeyGateway
List<UserApiKey> findByUserId(Long userId);
```

### Controller 变更

- `UserController`：新增 `GET /me/api-keys` 和 `GET /users/{userId}/api-keys` 端点
- `TeamController`：创建 Key 时校验 `userId` 权限

### 权限规则

- `GET /api/v1/me/api-keys` — 任何已登录用户
- `GET /api/v1/users/{userId}/api-keys` — 仅 ADMIN 角色
- `POST /api/v1/teams/{teamId}/api-keys` — 团队 ADMIN 角色
  - 普通用户：`userId` 必须等于当前用户 ID
  - 管理员：`userId` 必须是该团队成员

## 前端变更

### 新增"我的密钥"页面

- 路由：`/my-api-keys`
- 展示当前用户的所有 Key，按团队分组（前端按 `teamId` + `teamName` 分组）
- 支持查看 Key 详情、复制 Key、吊销 Key、创建 Key
- 创建时需选择归属团队（复用 `POST /api/v1/teams/{teamId}/api-keys` 端点）
- 侧边栏导航增加"我的密钥"入口

### 用户列表页变更

- 操作列增加"管理密钥"按钮
- 点击后打开弹窗，支持密钥的**创建/查看/复制/吊销**全生命周期操作
- 创建时需选择归属团队（复用 `POST /api/v1/teams/{teamId}/api-keys` 端点）
- 弹窗内按团队分组展示该用户的所有 Key

### 团队 Key 管理弹窗变更

- Key 列表增加"用户 ID"列（已完成）
- 创建表单的"用户 ID"输入调整：
  - 管理员模式 → 显示团队成员选择器（从团队成员列表中选择）
  - 普通用户模式 → 自动填充当前用户 ID，隐藏输入

## 数据流

### 创建 Key 流程

```
用户点击"创建密钥" → 选择团队 → 填写名称/产品/模型/额度
  → 前端传入 userId（管理员选择团队成员，普通用户自动填充自己）
  → 后端校验：普通用户 userId 必须等于当前用户 ID；管理员 userId 必须是该团队成员
  → 创建 Key
```

### 查询"我的密钥"流程

```
用户点击"我的密钥" → GET /api/v1/me/api-keys
  → 后端从 SecurityContext 获取当前用户 ID
  → 查询该用户的所有 Key（含 teamName 冗余字段）
  → 返回扁平列表
  → 前端按 teamId + teamName 分组展示
```

## 错误处理

| 场景 | 响应 |
|------|------|
| 非团队成员创建 Key | 403 Forbidden |
| 普通用户为他人创建 Key | 403 Forbidden |
| 管理员为非团队成员创建 Key | 403 Forbidden |
| 查询非自己的 Key（非 ADMIN） | 403 Forbidden |
| 用户无任何 Key | 200 + 空列表 |

## 已完成项

- [x] `ownerUserId` → `userId` 重命名（数据库、后端、前端）
- [x] `UserApiKeyResponse` / `UserApiKeyDetailResponse` 增加 `userId` 字段
- [x] 前端 Key 列表增加"用户 ID"列
- [x] 前端创建表单增加"用户 ID"输入

## 待实现项

- [ ] 后端：`UserApiKeyGateway.findByUserId()` 方法
- [ ] 后端：`UserApiKeyGatewayImpl.findByUserId()` 实现
- [ ] 后端：`UserApiKeyRepository.findByUserId()` 查询方法
- [ ] 后端：`UserApiKeyResponse` / `UserApiKeyDetailResponse` 增加 `teamName` 字段
- [ ] 后端：`UserApiKeyServiceImpl` 映射 `teamName`（需查询 TeamGateway）
- [ ] 后端：`UserController` 新增 `GET /me/api-keys` 端点
- [ ] 后端：`UserController` 新增 `GET /users/{userId}/api-keys` 端点
- [ ] 后端：创建 Key 时校验 userId 权限
- [ ] 前端：新增"我的密钥"页面（`/my-api-keys`）
- [ ] 前端：用户列表页增加"管理密钥"入口（弹窗支持创建/查看/复制/吊销）
- [ ] 前端：侧边栏导航增加"我的密钥"入口
- [ ] 前端：创建表单 userId 输入改为团队成员选择器（管理员模式）
- [ ] 测试：后端 API 测试
- [ ] 测试：前端页面测试
