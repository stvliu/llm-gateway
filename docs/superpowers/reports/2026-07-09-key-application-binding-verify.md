# 验证报告：key-application-binding

**日期**: 2026-07-09
**变更**: key-application-binding（UserApiKey 绑定 Application，修复路由根因）
**验证模式**: full（27 tasks / 3 capabilities / 48 files）
**验证阶段**: verify

## Summary

| 维度 | 状态 |
|------|------|
| Completeness | 27/27 tasks ✓，6 Requirement 全部实现 ✓ |
| Correctness | 14 Scenario 全部有测试覆盖 ✓ |
| Coherence | D1-D8 设计决策遵循 ✓，代码模式一致 ✓ |

## Fresh Evidence（本轮 verify 实测）

### 后端（gateway-boot）
- `./mvnw -pl gateway-boot test`：**EXIT_CODE=0**，702 tests PASS（0 failures, 0 errors）
- 含路由回归：`RoutingResolverTest.resolveCandidates_withApplicationId_forwardsAndReturnsNonEmpty`（applicationId 透传 + 非空候选集，回归根因）

### 前端（gateway-console）
- `npm run build`：✓ built in 37.61s（tsc -b + vite build，17421 modules）
- `npx vitest run`：3 failed | 129 passed（132）-- 3 个偶发超时（见 WARNING-1）
- 重跑 2 失败文件：9/9 通过 ✓（确认环境偶发，非源码缺陷）

## Completeness 验证

### Task 完成
- tasks.md：27/27 `[x]`（7.3 端到端手验标注"待 verify 阶段人工完成"，用户确认推进）
- openspec status：isComplete=true，所有 artifacts done
- openspec validate：Change is valid

### Spec 覆盖（6 Requirement / 3 capability）
1. **application - 删除前置校验**：`ApplicationServiceImpl.delete` + `UserApiKeyGateway.findByApplicationId` ✓
2. **application - 按应用查询**：`ApplicationController GET /applications/{id}/api-keys` ✓
3. **user-apikey-management - 创建必填**：`UserApiKeyCreateRequest @NotNull applicationId` + Service create setApplicationId ✓
4. **user-apikey-management - 更新补绑**：`UserApiKeyUpdateRequest` 可选 applicationId + Service update ✓
5. **user-apikey-management - 响应暴露**：`UserApiKeyResponse`/`DetailResponse` 含 applicationId ✓
6. **user-password-management - 重置密码**：`UserController POST /users/{id}/reset-password` + `UserService.resetPassword` ✓

## Correctness 验证

### Scenario 覆盖（14 Scenario 全部有测试）
- application 4 Scenario：`delete_hasApiKeys_throwsConflict`（ApplicationServiceImplTest + ApplicationControllerTest）+ `delete_noApiKeys_deletesCascade` + `listApiKeys_success` + `findByApplicationId_empty` ✓
- user-apikey-management 7 Scenario：`create_success_setsApplicationId` + `create_applicationNotFound_throws` + `update_rebindApplicationId` + `update_applicationNotFound_throws` + ResponseMappingTests ✓
- user-password-management 3 Scenario：`resetPassword_success_returns16CharPlain` + `resetPassword_builtin_throws` + `resetPassword_notFound_throws` + UserControllerTest 契约 ✓
- 路由回归 1：`resolveCandidates_withApplicationId_forwardsAndReturnsNonEmpty` ✓

## Coherence 验证

### Design 决策遵循（D1-D8）
- D1 applicationId @NotNull 必填 ✓
- D2 补绑复用 PUT /user-api-keys/{id} ✓
- D3 按应用查询放 ApplicationController ✓
- D4 重置密码随机生成 + 一次性明文 ✓
- D5 删除前置校验（设计妥协：GatewayRequestException 400 而非 ConflictException 409，已接受）✓
- D6 存量 null Key 不自动迁移 ✓
- D7 前端删团队继承 Alert + Application Select ✓
- D8 移除 rotate 死代码 ✓

### 代码模式一致
- 后端分层架构、Gateway 模式、public 方法中文 Javadoc ✓
- 前端类型安全、组件改造、i18n defaultValue 兜底 ✓

## Issues

### CRITICAL（必须修复）
无。

### WARNING（接受偏差）
- **WARNING-1**: 前端 vitest 3 测试偶发超时（默认 testTimeout=5000ms）
  - 文件：`Applications/index.test.tsx`（查看 Key 跳转）、`UserApiKeyModal.test.tsx`（补绑交互 + 补绑转移）
  - 原因：Windows + jsdom + antd 重交互，测试耗时 6070-6279ms 略超 5000ms 阈值
  - 证据：重跑 2 文件 9/9 通过，确认非源码缺陷
  - 处理：用户确认接受偏差，testTimeout 调整作为 MINOR 待办
  - 影响范围：仅 CI 稳定性，不影响功能正确性

### MINOR（接受待办，后续迭代）
1. `vite.config.ts` testTimeout 调整（建议 15000ms）
2. `utils/errorMessage.ts` extractErrorMessage 不识别 ApiResponse error.message 嵌套（Task 12 规避）
3. locales 清理（补 applicationId 相关 key + 删 rotate 死 key）
4. `ApplicationControllerIT` 补 GET /api-keys + DELETE 冲突 IT 契约测试
5. `ApplicationControllerTest` 类注释 Task 编号引用与 tasks.md 不一致
6. `KeyGenerateModal` applications 为空时无引导文案
7. `UserApiKeyModal` 直接调 API 层而非 hook（既有行为）
8. `ApplicationServiceImpl` 400 vs 409（设计妥协，已接受）

### 7.3 端到端手验（待 verify 阶段人工完成）
6 项手验待用户人工执行：
1. 创建 Application
2. 创建 UserApiKey（必选应用，明文显示一次）
3. Applications 页点击「查看 Key」跳转 `/keys?applicationId=<id>`
4. 用该 Key 调 `POST /v1/chat/completions` 验证路由成功（根因修复）
5. 删除有 Key 的 Application 返回 400 + APPLICATION_HAS_API_KEYS
6. 重置非内建用户密码返回 16 位明文；内建用户 403

## Final Assessment

**验证通过**。无 CRITICAL/IMPORTANT。3 个 WARNING（偶发超时，用户确认接受偏差）。8 项 MINOR（接受待办）。

后端 702 tests + 前端 129 tests PASS（3 偶发超时接受），build 全绿，spec 6 Requirement / 14 Scenario 全部覆盖，D1-D8 设计决策遵循。

**可进入归档阶段**（7.3 端到端手验待人工 verify）。
