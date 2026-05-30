## 1. DTO 定义

- [x] 1.1 创建 `MaterializeBatchResult.java` — 批量物化结果封装（totalCount/successCount/skippedCount/failedCount/results）
- [x] 1.2 创建 `PlanResult.java` — 每条 Plan 的独立物化结果（planCode/entityId/status/errorMessage）
- [x] 1.3 创建 `MaterializeBatchRequest.java` — 请求 DTO（含可选 planCodes 列表）

## 2. 后端 Service 实现

- [x] 2.1 新增 `materializeProviderWithPlans(providerCode, planCodes)` 方法 — 级联物化主逻辑
- [x] 2.2 调整 `materializePlan()` 事务传播为 `REQUIRED`（默认），确保与外层事务一致
- [x] 2.3 处理已物化 Plan 的 SKIPPED 语义（捕获 `ALREADY_MATERIALIZED` 异常并跳过）
- [x] 2.4 处理 Provider 已物化的幂等情况（复用现有检查，SKIPPED 而非抛异常）

## 3. 后端 Controller 实现

- [x] 3.1 `CatalogController` 新增 `POST /materialize/provider/{code}/with-plans` 端点
- [x] 3.2 验证 `@SaCheckRole("ADMIN")` 权限控制

## 4. 前端类型定义

- [x] 4.1 `catalog.ts` 新增 `MaterializeBatchRequest`、`MaterializeBatchResponse`、`PlanMaterializeResult` TypeScript 类型

## 5. 前端 API 客户端

- [x] 5.1 `services/api/catalog.ts` 新增 `materializeProviderWithPlans(providerCode, planCodes?)` 函数

## 6. 前端 React Query Hooks

- [x] 6.1 `services/query/useCatalog.ts` 新增 `useMaterializeProviderWithPlans` mutation hook
- [x] 6.2 mutation 成功后自动 invalidate 相关查询（providers/plans）

## 7. 前端组件改造

- [x] 7.1 `ProviderCatalogView.tsx` 「物化」按钮改为弹窗触发
- [x] 7.2 新增级联确认弹窗组件 `CascadeMaterializeDialog.tsx`（展示 Plans 清单 + 确认/取消）
- [x] 7.3 弹窗中已物化 Plans 标灰显示，不可勾选
- [x] 7.4 物化完成后刷新列表状态 & 关闭弹窗

## 8. 前端 i18n

- [x] 8.1 `zh-CN/catalog.json` 新增级联弹窗文案（标题/描述/按钮/统计信息）
- [x] 8.2 `en-US/catalog.json` 新增对应英文文案

## 9. 测试适配

- [x] 9.1 `CatalogMaterializeServiceTest` 新增级联物化测试用例（全级联 + 部分级联 + 全部已物化）
- [x] 9.2 `CatalogControllerTest` 新增级联端点测试（Controller 层无独立测试，逻辑在 Service 层覆盖）
- [x] 9.3 验证现有逐条物化测试不受影响（436 tests 全部通过）