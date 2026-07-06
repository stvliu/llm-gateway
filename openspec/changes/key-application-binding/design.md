## Context

`application` spec 与 `application-access-control` spec 已定义「Application 为权限锚点」「UserApiKey.applicationId 作为权限锚点」「applicationId 为 null 时 PermissionRouter 返回空集」——spec 层模型正确。问题在实现层断层：管理 API 从未暴露 `applicationId`，`UserApiKeyServiceImpl.create()` 不落库该字段，5 个 DTO 均不含它。结果是通过管理 API 创建的 Key `applicationId` 永远为 null，路由时返回空渠道集，Key 能创建但不可用。

前端则停留在已归档的 `inherited-permission-model`（团队继承）模型，`UserApiKeyModal` 的 Alert 与后端现行模型冲突。管理员重置密码前端调用 `POST /users/{id}/reset-password` 但后端无此端点。

本次变更不引入新架构，而是补齐 spec 已规定但实现缺失的管理面 API，并消除前后端模型分歧。

## Goals / Non-Goals

**Goals:**
- 管理 API 完整暴露与操作 `applicationId`：创建必填、更新补绑/转移、响应字段、按应用查询
- 修复管理员重置密码 404
- 前端对齐 Application 锚点模型，删除「团队继承」Alert
- 删除 Application 时防止 `applicationId` 悬空

**Non-Goals:**
- 不实现 UserApiKey rotate 端点（仅清理前端死代码封装）
- 不做 Key 启用/禁用状态、有效期
- 不做用户搜索/筛选/分页后端化、Application 分页/状态切换、批量操作、独立详情页
- 不自动迁移存量 null Key（仅提供补绑入口）
- 不改路由层（PermissionRouter 行为已符合 spec）

## Decisions

### D1: applicationId 创建时 @NotNull 必填
- **选择**：`UserApiKeyCreateRequest.applicationId` 加 `@NotNull`，create 落库 `setApplicationId`
- **理由**：路由层 `PermissionRouter` 对 null 返回空集（spec 规定），null 的 Key 不可路由。必填保证新建 Key 即刻可用
- **否决**：「可选 + 默认应用」引入默认应用概念且无路由放行语义；「可选 + null 放行全部」违反零信任与 spec

### D2: 补绑/转移复用 PUT /user-api-keys/{id}
- **选择**：`UserApiKeyUpdateRequest` 加可选 `applicationId` 字段，update 时若传入则变更
- **理由**：复用现有 update 端点，最小改动；补绑与改名同属 Key 编辑
- **否决**：专用 `/transfer` 端点过度设计

### D3: 按应用查询放 ApplicationController
- **选择**：`GET /api/v1/applications/{id}/api-keys` 作为 Application 子资源
- **理由**：子资源语义清晰（应用的 Key 集合），与现有 `/applications/{id}/channels` 一致
- **否决**：`UserApiKeyController` 加 `applicationId` 查询参数不够 RESTful

### D4: 重置密码后端随机生成 + 一次性返回明文
- **选择**：`POST /users/{id}/reset-password` 生成随机密码，更新哈希，一次性返回明文
- **理由**：与 `UserApiKeyCreate` 明文一次性返回模式一致；管理员不应知晓用户密码
- **否决**：管理员设定密码（安全差）；保留旧密码（不构成"重置"）

### D5: 删除 Application 前置校验有 Key 引用则拒绝
- **选择**：`ApplicationServiceImpl.delete` 删除前查 `applicationId = id` 的非删除 Key，有则抛 Conflict
- **理由**：避免 `applicationId` 悬空导致 Key 不可路由；强制管理员显式处理 Key
- **否决**：级联置 null（引入悬空坏数据）；级联删除 Key（危险，丢失凭证）

### D6: 存量 null Key 不自动迁移
- **选择**：提供补绑入口（D2），存量由管理员手动补绑
- **理由**：自动绑默认应用有赋错权限风险；开发期存量少
- **否决**：自动迁移到默认应用

### D7: 前端删除「团队继承」Alert，创建表单加 Application 必选
- **选择**：`UserApiKeyModal` 删 Alert、加 Application Select；`DownstreamKeysTable` 创建表单同步；列表加「所属应用」列
- **理由**：对齐后端 Application 锚点模型，消除代码自相矛盾

### D8: 移除前端 rotate 死代码封装
- **选择**：`services/api/userApiKey.ts` 移除 `rotate` 方法
- **理由**：后端无该端点，封装误导后续调用

## Risks / Trade-offs

- [CreateRequest 加必填字段是破坏性变更] → 前端是唯一调用方，同步改造；无 CLI/外部调用方依赖旧契约
- [DELETE 加前置校验是破坏性变更] → 管理员需先转移/删除 Key；前端删除按钮加冲突提示
- [存量 null Key 仍不可路由] → 补绑入口上线后由管理员手动处理，release notes 说明
- [重置密码返回明文] → 一次性返回不持久化，HTTPS 传输，与 UserApiKeyCreate 一致

## Migration Plan

1. 后端先行：DTO 加字段 → Service 落库/补绑 → Gateway 加 `findByApplicationId` → Controller 加端点 → 单测 + 集成测试（含「带 applicationId 的 Key 路由返回非空渠道集」回归）
2. 前端跟进：类型加字段 → 表单加 Application 选择 → 列表加列 → 删 Alert → 移除 rotate → Applications 加「查看 Key」入口
3. 存量 null Key：管理员通过补绑入口手动处理
4. 回滚：整体 revert change 分支，无 schema 变更，回滚无副作用

## Open Questions

- 重置密码随机密码长度/字符集：倾向 16 位字母数字混合，实现时定
- Application「查看 Key」入口形态：倾向跳转 `/keys?applicationId=<id>` 触发筛选，build 阶段定
