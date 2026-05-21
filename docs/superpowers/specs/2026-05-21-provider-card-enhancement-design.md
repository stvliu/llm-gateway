---
name: provider-product-enhancement
description: 供应商卡片及产品标签页完善设计文档
metadata:
  type: design
---

# 供应商卡片及产品标签页完善设计

**日期**: 2026-05-21
**状态**: 待审批

## 背景

当前供应商管理界面存在以下问题：

1. **卡片展示信息简单**：仅展示名称、状态、网址、Key 统计，缺少产品和模型维度信息
2. **产品标签页交互体验欠佳**：Collapse 嵌套层次深、API Key 新增表单样式简陋
3. **缺少密钥测试功能**：无法验证 API Key 是否有效
4. **信息展示不完整**：缺少调用状态、模型关联等信息

## 目标

1. **卡片展示增强**：展示产品数量、模型数量预览
2. **产品标签页重构**：卡片式布局、列表式 API Key 展示、弹窗表单新增
3. **密钥测试功能**：后端新增测试接口，前端展示测试结果
4. **信息增强**：展示调用状态、模型关联

---

## Part 1: 供应商卡片展示增强

### 布局设计

保持现有垂直布局，移除原有的 Key 统计和模型展示，新增产品展示区域：

```
┌─────────────────────────────────────┐
│ [状态] 供应商名称                     │
├─────────────────────────────────────┤
│ 🌐 官网地址                          │
│ 📄 API 文档                          │
├─────────────────────────────────────┤
│ 📦 产品 (3)                          │
│ [产品A] [产品B] [产品C] +0           │
└─────────────────────────────────────┘
```

**数据模型说明**：
- **API Key 属于产品**，不属于供应商，因此供应商卡片不展示 Key 统计
- **模型属于产品**，不属于供应商，因此供应商卡片不展示模型信息
- Key 和模型信息在产品卡片中展示

### 交互设计

1. **产品标签点击**：打开供应商详情抽屉，自动切换到产品标签页
2. **超出数量展示**：超过限制数量时显示 `+N` 标签

### 数据获取

采用 **前端并行查询** 方案：
- 卡片组件内使用 `useProducts(providerId)` 查询产品列表
- 利用 React Query 的缓存和并行能力

---

## Part 2: 产品标签页重构

### 布局设计

采用 **卡片式布局**，每个产品一张独立卡片：

```
┌─────────────────────────────────────────────────────────┐
│ 📦 产品名称                              [编辑] [删除]   │
│ 状态: 活跃 | 端点: 2 | Keys: 5 | 模型: 12               │
├─────────────────────────────────────────────────────────┤
│ 端点配置                                                 │
│ ┌─────────────────────────────────────────────────────┐ │
│ │ OpenAI Chat    https://api.openai.com/v1            │ │
│ │ Anthropic Msg  https://api.anthropic.com/v1         │ │
│ └─────────────────────────────────────────────────────┘ │
├─────────────────────────────────────────────────────────┤
│ API Keys                              [+ 新增 Key]      │
│ ┌─────────────────────────────────────────────────────┐ │
│ │ 🔑 prod-key-001  sk-abc...  优先级:10 权重:100 [✓]  │ │
│ │    最近调用: 2小时前  成功率: 99.2%    [测试] [编辑] │ │
│ ├─────────────────────────────────────────────────────┤ │
│ │ 🔑 prod-key-002  sk-def...  优先级:5  权重:50  [✓]  │ │
│ │    最近调用: 1天前    成功率: 98.5%    [测试] [编辑] │ │
│ └─────────────────────────────────────────────────────┘ │
├─────────────────────────────────────────────────────────┤
│ 关联模型 (12)                                            │
│ [gpt-4o] [gpt-4-turbo] [gpt-3.5-turbo] [claude-3-opus] │
│ [claude-3-sonnet] +7                                     │
└─────────────────────────────────────────────────────────┘
```

### API Key 展示（列表式）

每行展示：
- **名称**：Key 名称（可自定义）
- **前缀**：`sk-abc...`（前 6 位 + ...）
- **优先级**：数字标签
- **权重**：数字标签
- **状态**：活跃/禁用（带颜色标识）
- **最近调用**：相对时间（如"2小时前"）
- **成功率**：百分比（需后端支持）
- **操作**：[测试] [编辑] [删除]

### API Key 新增（弹窗表单）

点击"新增 Key"按钮打开 Modal，表单字段：
- **API Key**：密码输入框，必填
- **名称**：可选，便于识别
- **优先级**：InputNumber，0-100
- **权重**：InputNumber，≥0
- **描述**：TextArea，可选

**统一交互规范**：所有新增/创建操作统一使用弹窗式窗口（Modal），保持体验一致性。包括：
- 新增供应商 → `ProviderCreateModal`
- 新增产品 → `ProductFormModal`
- 新增 API Key → `ProductApiKeyCreateModal`
- 编辑供应商/产品/API Key → 同样使用 Modal

创建成功后 Modal 展示：
- 成功提示
- 明文密钥（可复制）
- 警告提示："请立即保存此密钥，关闭后将无法再次查看"

---

## Part 3: 密钥测试功能

### 后端 API 设计

**端点**: `POST /api/v1/products/{productId}/keys/{keyId}/test`

**请求体**: 无（或可选传入测试参数）

**响应体**:
```json
{
  "success": true,
  "latency": 245,
  "modelName": "gpt-4o",
  "responsePreview": "Hello! How can I assist you today?",
  "testedAt": "2026-05-21T10:30:00Z",
  "error": null
}
```

**失败响应**:
```json
{
  "success": false,
  "latency": null,
  "modelName": null,
  "responsePreview": null,
  "testedAt": "2026-05-21T10:30:00Z",
  "error": {
    "code": "AUTHENTICATION_FAILED",
    "message": "Invalid API key"
  }
}
```

### 前端交互

1. 点击"测试"按钮，显示 loading 状态
2. 测试完成后，在行内展示结果：
   - 成功：绿色勾 + 延迟 + 模型名称
   - 失败：红色叉 + 错误信息
3. 结果展示 3 秒后自动消失，或点击关闭

---

## Part 4: 信息增强

### 产品卡片统计信息

| 字段 | 说明 | 数据来源 |
|------|------|----------|
| 端点数量 | 配置的 BaseURL 数量 | `product.endpoints` 长度 |
| Keys 数量 | API Key 总数/活跃数 | `useProductApiKeys` |
| 模型数量 | 关联的模型数 | 新增 `useProductModels` |
| 最近调用 | 最后一次请求时间 | 需后端新增字段 |
| 成功率 | 近期请求成功率 | 需后端新增字段 |

### 模型关联展示

在产品卡片底部展示关联模型：
- 最多展示 5 个模型标签
- 超出显示 `+N`
- 点击标签可跳转到模型详情（后续扩展）

---

## 详细设计

### 1. ProviderCard 组件改造

**文件**: `gateway-console/src/pages/Providers/ProviderCard.tsx`

**改动点**：
- 新增 `onViewProducts` 回调
- 内部使用 `useProducts` 查询产品数据
- 新增产品展示区域（移除原有的 Key 统计）

### 2. ProviderCardView 组件改造

**文件**: `gateway-console/src/pages/Providers/ProviderCardView.tsx`

**改动点**：
- 新增 `onViewProducts` 回调
- 传递回调给 `ProviderCard`

### 3. Providers 主页面改造

**文件**: `gateway-console/src/pages/Providers/index.tsx`

**改动点**：
- 新增 `defaultTab` 状态，控制抽屉默认标签页
- 传递回调给 `ProviderCardView`

### 4. ProviderManagementDrawer 改造

**文件**: `gateway-console/src/pages/Providers/ProviderManagementDrawer.tsx`

**改动点**：
- 新增 `defaultTab` prop，支持打开时默认选中指定标签页

### 5. ProviderProductsTab 重构

**文件**: `gateway-console/src/pages/Providers/ProviderProductsTab.tsx`

**改动点**：
- 从 Collapse 改为 Card 布局
- API Key 从内嵌改为列表式
- 新增统计信息展示
- 新增模型关联展示

### 6. 新增 ProductApiKeyCreateModal

**文件**: `gateway-console/src/pages/Providers/ProductApiKeyCreateModal.tsx`（新建）

**功能**：
- 弹窗表单新增 API Key
- 创建成功后展示明文密钥

### 7. 新增 ProductApiKeyTestButton

**文件**: `gateway-console/src/pages/Providers/ProductApiKeyTestButton.tsx`（新建）

**功能**：
- 调用测试 API
- 展示测试结果

### 8. 后端新增测试接口

**文件**: `gateway-boot/src/main/java/com/codingas/gateway/adapter/api/ProductApiKeyController.java`

**新增端点**: `POST /api/v1/products/{productId}/keys/{keyId}/test`

### 9. 国际化

**文件**:
- `gateway-console/src/locales/zh-CN/providers.json`
- `gateway-console/src/locales/en-US/providers.json`

**新增翻译键**：

| 键名 | 中文 | 英文 |
|------|------|------|
| `card.products` | 产品 | Products |
| `card.noProducts` | 暂无产品 | No products |
| `product.endpoints` | 端点 | Endpoints |
| `product.keys` | Keys | Keys |
| `product.associatedModels` | 关联模型 | Associated Models |
| `product.lastCall` | 最近调用 | Last Call |
| `product.successRate` | 成功率 | Success Rate |
| `product.testKey` | 测试 | Test |
| `product.testSuccess` | 测试成功 | Test Passed |
| `product.testFailed` | 测试失败 | Test Failed |
| `product.latency` | 延迟 | Latency |
| `product.keyCreated` | 密钥创建成功 | API Key Created |
| `product.keyCreatedHint` | 请立即保存此密钥，关闭后将无法再次查看 | Please save this key immediately. You won't be able to see it again after closing. |

---

## 实现计划

### Phase 1: 卡片展示增强（前端）

1. 修改 `ProviderCard` 组件，新增产品展示，移除 Key 统计
2. 修改 `ProviderCardView` 组件，传递回调
3. 修改 `Providers` 主页面，控制抽屉默认标签页
4. 修改 `ProviderManagementDrawer`，支持 `defaultTab`
5. 添加国际化翻译

### Phase 2: 产品标签页重构（前端）

1. 重构 `ProviderProductsTab`，改为卡片式布局
2. 重构 API Key 展示，改为列表式
3. 新增 `ProductApiKeyCreateModal` 弹窗组件
4. 新增统计信息展示
5. 新增模型关联展示

### Phase 3: 密钥测试功能（后端 + 前端）

1. 后端新增测试接口
2. 前端新增 `ProductApiKeyTestButton` 组件
3. 集成测试结果展示

### Phase 4: 验证测试

1. TypeScript 类型检查
2. ESLint 检查
3. 手动测试完整流程

---

## 风险与考量

1. **性能考量**：每个卡片都会发起产品查询，建议利用 React Query 缓存机制
2. **调用状态数据**：最近调用时间、成功率需后端新增字段或接口
3. **模型关联**：模型属于产品，需确认模型与产品的关联关系和查询接口

## 后续迭代

- [ ] 健康状态展示（需后端新增 API）
- [ ] 模型详情页
- [ ] 批量操作 API Key
- [ ] 后端聚合接口优化性能
