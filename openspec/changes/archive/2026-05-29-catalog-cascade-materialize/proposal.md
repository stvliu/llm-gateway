## 为什么

平台管理员接入新供应商（如 DeepSeek）时，需要在 Catalog 页面先物化 Provider，再逐个物化每个 Plan。对于拥有 5-10 个定价方案的供应商，操作重复性高，与 LLM-Gateway "开箱即用" 的企业级定位不符。用户期望一个操作完成供应商的完整接入。

## 变更内容

- **Provider 级联物化**：`materializeProvider()` 级联创建该 Provider 下所有 Plans（含 Endpoints + Models）
- **批量物化 API**：新增 `POST /api/v1/catalog/materialize/provider/{code}/with-plans` 端点（支持选择性 Plans）
- **前端级联确认弹窗**：物化前展示准备创建的 Plans 清单，允许用户取消勾选不需要的 Plans
- **保持后向兼容**：逐条物化 API 保留不变，级联作为增强而非替代

## 能力

### 新能力
- `catalog-cascade-materialize`: Provider 物化时级联关联 Plans（含 Endpoints/Models），支持选择性 Plans 列表

### 修改的能力
- （无——不修改已有 API 行为，仅增加级联选项）

## 影响

- **后端**：`CatalogMaterializeService.materializeProvider()` 增加级联逻辑；
`CatalogController` 新增批量物化端点；
新增 `MaterializePlanRequest` DTO 支持 Plans 筛选
- **前端**：`ProviderCatalogView.tsx` 物化按钮增加确认弹窗；
新增级联物化 API 调用；
i18n 新增级联物化文案
- **数据**：无 DB schema 变更，无 migration