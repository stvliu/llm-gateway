# Brainstorm Summary

- Change: key-application-binding
- Date: 2026-07-06

## 确认的技术方案

**后端**：
- 5 个 DTO record 加 `applicationId`：`CreateRequest`(@NotNull)、`UpdateRequest`(可选)、`Response`/`DetailResponse`/`CreateResponse`(响应字段)
- `UserApiKeyServiceImpl` 注入 `ApplicationGateway`，create/update 校验 `applicationId` 引用存在并 `setApplicationId`；`toResponse/toDetailResponse` 映射 `applicationId`
- `UserApiKeyGateway` 加 `findByApplicationId(Long)`；Impl 已映射 applicationId（toEntity:99/toDataObject:124），仅加查询实现
- `ApplicationController` 加 `GET /api/v1/applications/{id}/api-keys`；`ApplicationServiceImpl.delete` 注入 UserApiKeyGateway，有引用抛 Conflict
- `UserController` + `UserService.resetPassword()`：16 位字母数字（排除 O/0/I/1/l），更新哈希，一次性返回明文，禁止内建用户

**前端**：
- `types/userApiKey.ts` 加字段 + 修正注释
- `services/api/userApiKey.ts` 移除 rotate 死代码 + 加 `listByApplication`
- `DownstreamKeysTable` 创建表单加 Application Select、列表加列、按应用筛选、支持 URL `?applicationId=` 触发筛选
- `UserApiKeyModal` 删「团队继承」Alert + 加 Application Select + 补绑
- `Applications/index.tsx` 加「查看 Key」跳转 `/keys?applicationId=<id>`
- `services/api/user.ts` resetPassword 保留

**已解决决策**：重置密码 16 位排除易混；查看 Key 入口跳转+筛选；Application 存在性校验放 Service 层注入 ApplicationGateway。

## 关键取舍与风险

- CreateRequest 必填是 BREAKING → 前端唯一调用方，同步改
- DELETE 前置校验是 BREAKING → UI 加冲突提示
- 存量 null Key 不自动迁移 → 管理员手动补绑
- 重置密码明文一次性返回 → HTTPS 传输，不持久化
- 并发补绑同一 Key：无版本字段，最后写入胜出（非本次加锁范围）
- 删除与补绑竞争：删除前置校验 + 事务；极端并发下 Key 可能指向已删应用，降级为不可路由（不 panic）

## 测试策略

- **单测**：`UserApiKeyServiceImplTest`（create 必填落库 + Application 存在性、update 补绑 + 校验、响应含 applicationId）、`ApplicationServiceImplTest`（delete 有 Key 冲突）、`UserServiceImplTest`（resetPassword 16 位 + 内建拒绝）
- **集成**：`GET /applications/{id}/api-keys`、`POST /users/{id}/reset-password`、DELETE 冲突
- **路由回归**（核心）：带 `applicationId` 的 Key 调 `ChatDispatchService` 返回非空渠道集——回归本次根因

## Spec Patch

无。现有 delta spec 验收场景充分（含「引用不存在 Application 被拒」「内建用户拒绝」「空应用返回空列表」）。
