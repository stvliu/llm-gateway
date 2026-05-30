## 背景

当前 `materializeProvider()` 只创建 Provider 实体，关联的 Plans 需用户在 PlanCatalogView 中逐条手动物化。这导致一个供应商需要 N+1 次操作。`materializePlan()` 内部已实现 Model 的级联物化（`findOrCreateModel`），但 Provider → Plans 的级联缺失。

约束条件：
- 逐条物化 API 必须保持向后兼容
- 级联物化必须是事务一致的（全有或全无）
- Plans 数据量 = 单供应商通常 1-10 条，级联性能无瓶颈
- 已有前端 ProviderCatalogView 的「物化」按钮交互路径

## 目标 / 非目标

**目标：**
- 提供一键级联物化 Provider + 所有（或选中）Plans 的能力
- 前端提供可视化确认弹窗，显示将物化的 Plans 清单
- 后端事务性保证：任一 Plan 失败则整体回滚
- 保持现有逐条物化 API 不变

**非目标：**
- 不修改 DB schema（无需 migration）
- 不修改 Plan 逐条物化逻辑（复用现有 `materializePlan()`）
- 不引入异步/事件驱动（级联是同步操作，数据量小）
- 不涉及级联物化后的自动配置（如 API Key 填充）

## 设计决策

### 决策 1：新端点 vs 参数扩展

**方案 A（选中的方案）：新端点 `POST /with-plans`**
- URL：`POST /api/v1/catalog/materialize/provider/{code}/with-plans`
- 请求体：`{ planCodes?: string[] }` — 可选，缺省表示全部
- 响应：新 DTO `MaterializeBatchResult`
- **理由**：与现有逐条物化端点语义分离，避免 `?cascade=true` 参数污染已有接口行为

**方案 B（弃选）：`?cascade=true` 参数**
- `POST /api/v1/catalog/materialize/provider/{code}?cascade=true`
- **弃选理由**：现有端点是幂等的（已物化则抛 `ALREADY_MATERIALIZED`），而级联是"尽可能多地物化"，语义不兼容

### 决策 2：Provider 级联实现策略

**方案 A（选中的方案）：在 Service 层新增独立方法，复用 `materializePlan()`**

```java
@Transactional
public MaterializeBatchResult materializeProviderWithPlans(String providerCode, List<String> planCodes) {
    // 1. 调用 materializeProvider 创建 Provider
    // 2. 查询关联 Plans（按 planCodes 过滤或全部）
    // 3. 遍历调用 materializePlan（仅创建 Channel 部分的事务传播）
    // 4. 收集每条结果，返回批量 DTO
}
```

**理由**：复用已测试的 `materializePlan()` 逻辑，避免重复的 Channel/Endpoint/Model 创建代码。

**事务传播处理**：`materializePlan()` 标记为 `@Transactional(propagation = Propagation.REQUIRES_NEW)` → 改为 `REQUIRED`（默认），让外层事务统一控制回滚。

**方案 B（弃选）：直接修改 `materializeProvider()`**
- **弃选理由**：违反开闭原则。已有调用者（快路径 API）不期望级联行为

### 决策 3：前端弹窗数据来源

**方案：前端直接调用现有 `GET /api/v1/catalog/plans?providerCode=xxx` 获取 Plans 列表**

- 不需要新增 API：现有 `listPlanCatalogs` 已返回包含 `materialized` 标志的响应
- 弹窗只展示未物化的 Plans（已物化的标灰或隐藏）
- 用户确认后调用后端批量端点

### 决策 4：批处理结果结构

```java
// 整体封装
MaterializeBatchResult {
    String providerCode;       // 操作关联供应商
    int totalCount;            // 本次处理总条目
    int successCount;          // 成功数
    int skippedCount;          // 跳过数（已物化）
    int failedCount;           // 失败数
    List<PlanResult> results;  // 每条 Plan 的独立结果
}

// 每条 Plan 结果
PlanResult {
    String type = "PLAN";
    String planCode;
    Long entityId;             // Channel ID（成功时）
    String status;             // CREATED / SKIPPED / FAILED
    String errorMessage;       // 失败时原因
}
```

## 风险 / 权衡

| 风险 | 缓解措施 |
|------|---------|
| [事务超时] 单个供应商 10+ Plans 级联可能超过默认事务超时 | `@Transactional(timeout = 30)` 显式设置合理超时 |
| [数据不一致] `materializePlan()` 内部 `findOrCreateModel` 的级联物化可能导致 Model 被重复创建 | 已验证：`findOrCreateModel` 已做幂等检查（`findByModelName`），不会重复 |
| [用户误操作] 用户无意点击级联物化，创建过多 Channel | 前端弹窗明确展示 Plans 清单和数量，需确认后才执行 |
| [SKIPPED 语义混淆] 用户可能认为 SKIPPED 是错误 | 前端对 SKIPPED 展示不同颜色 Tag，附 tips 文案 |

## 实现步骤

1. 新增 `MaterializeBatchResult.java` 和 `PlanResult.java` DTO
2. 新增 `MaterializeBatchRequest.java` 请求 DTO（含可选 `planCodes`）
3. `CatalogMaterializeService` 新增 `materializeProviderWithPlans(providerCode, planCodes)` 方法
4. 必要时调整 `materializePlan()` 的事务传播行为
5. `CatalogController` 新增 `POST /with-plans` 端点
6. `ProviderCatalogView.tsx` 物化按钮改为弹窗 → 确认 → 调用批量 API
7. `useCatalog.ts` 新增级联物化 mutation hook
8. `catalog.ts` 类型定义新增 `MaterializeBatchRequest` / `MaterializeBatchResponse`
9. i18n 新增弹窗文案

## 开放问题

- 弹窗是否需要分页？考虑单供应商 Plans > 20 的情况（目前不存在，但设计时考虑 UI 溢出）

## 架构交互图

```
用户 (@ProviderCatalogView)
  │  点击「物化」
  ▼
确认弹窗
  │  GET /api/v1/catalog/plans?providerCode=xxx  ← 获取 Plans 列表
  │  用户勾选/取消 Plans
  │  点击确认
  ▼
POST /api/v1/catalog/materialize/provider/{code}/with-plans
  │  body: { planCodes: ["plan_a", "plan_b"] }
  ▼
CatalogController.materializeProviderWithPlans()
  ▼
CatalogMaterializeService.materializeProviderWithPlans()
  ├─ ① materializeProvider()           → Provider 实体
  ├─ ② 遍历 planCodes
  │    └─ materializePlan(planCode)     → Channel + Endpoint + Model
  └─ ③ 返回 MaterializeBatchResult
  ▼
前端更新 UI，刷新物料化状态
```