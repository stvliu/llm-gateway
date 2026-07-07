## 1. 后端 DTO 与 Service（UserApiKey 关联打通）

- [x] 1.1 `UserApiKeyCreateRequest` 新增 `@NotNull Long applicationId`
- [x] 1.2 `UserApiKeyUpdateRequest` 新增可选 `Long applicationId`
- [x] 1.3 `UserApiKeyResponse`/`UserApiKeyDetailResponse`/`UserApiKeyCreateResponse` 新增 `applicationId` 字段
- [x] 1.4 `UserApiKeyServiceImpl.create()` 落库 `setApplicationId` 并校验 Application 存在
- [x] 1.5 `UserApiKeyServiceImpl.update()` 支持 `applicationId` 变更（补绑/转移）并校验 Application 存在
- [x] 1.6 `UserApiKeyServiceImpl.toResponse/toDetailResponse` 映射 `applicationId`
- [x] 1.7 单测 `UserApiKeyServiceImplTest`：创建必填落库、补绑转移、响应含 applicationId、引用不存在 Application 被拒

## 2. 后端 Gateway（按应用查询）

- [x] 2.1 `UserApiKeyGateway` 接口新增 `findByApplicationId(Long)`
- [x] 2.2 `UserApiKeyGatewayImpl` 实现 `findByApplicationId`，确认 `toEntity/toDataObject` 读写 `applicationId`
- [x] 2.3 单测覆盖 `findByApplicationId`

## 3. 后端 Controller 与端点

- [ ] 3.1 `ApplicationController` 新增 `GET /api/v1/applications/{id}/api-keys`
- [ ] 3.2 `ApplicationServiceImpl.delete()` 新增 UserApiKey 引用前置校验，有则抛 Conflict
- [ ] 3.3 `UserController` 新增 `POST /api/v1/users/{id}/reset-password` + `UserService.resetPassword()`（随机密码 + 一次性返回明文 + 禁止内建用户）
- [ ] 3.4 集成测试：带 `applicationId` 的 Key 调用 `ChatDispatchService` 路由返回非空渠道集（回归核心问题）
- [ ] 3.5 集成测试：删除有 Key 引用的 Application 返回 4xx Conflict
- [ ] 3.6 集成测试：重置密码端点成功 + 内建用户拒绝

## 4. 前端类型与 API 层

- [ ] 4.1 `types/userApiKey.ts`：`UserApiKey`/`CreateUserApiKeyRequest`/`UpdateUserApiKeyRequest` 加 `applicationId`，修正注释与字段一致
- [ ] 4.2 `services/api/userApiKey.ts`：移除 `rotate` 死代码封装，新增 `listByApplication(applicationId)`
- [ ] 4.3 `services/api/user.ts`：确认 `resetPassword` 封装（后端已补端点）

## 5. 前端 UserApiKey 管理页

- [ ] 5.1 `DownstreamKeysTable` 创建表单加 Application Select（必填），列表加「所属应用」列，加按应用筛选 Select
- [ ] 5.2 `UserApiKeyModal` 删除「团队继承」Alert，创建表单加 Application Select，支持编辑补绑 applicationId
- [ ] 5.3 组件测试：创建表单 Application 必填校验、列表 Application 列渲染、补绑交互

## 6. 前端 Application 管理页

- [ ] 6.1 `Applications/index.tsx` 行操作加「查看 Key」入口（跳转 `/keys?applicationId=<id>` 触发筛选）
- [ ] 6.2 删除 Application 冲突提示（有 Key 引用时显示后端 Conflict 信息）

## 7. 验证与收尾

- [ ] 7.1 后端全量测试通过（`./mvnw test`）
- [ ] 7.2 前端构建与测试通过（`npm run build` + `npm test`）
- [ ] 7.3 端到端手验：创建 Key 必选 App → 用该 Key 调网关 → 路由成功
