---
comet_change: provider-frontend-ux-redesign
role: technical-design
canonical_spec: openspec
---

# 供应商前端交互优化 — 技术设计文档

## 1. 核心架构决策

**渠道为中心，供应商=分组维度。** RoutingContext 锚点是 channelId，不是 providerId。Provider 的作用：1) 组织归属 2) 品牌标识 3) Catalog 物化入口。

### 导航架构

侧边栏 4 菜单项：

| 菜单 | 路由 | 职责 |
|------|------|------|
| 渠道管理 | `/channels` | 日常操作入口，渠道列表+详情抽屉 |
| 供应商目录 | `/providers` | Catalog 浏览+物化入口（轻量级） |
| 团队管理 | `/teams` | 成员+权限 |
| 开发者门户 | `/developers` | 自助开通+下游 Key+代码示例 |

模型管理不设独立页面，内嵌在渠道详情的模型映射区。上游 Key 内嵌渠道详情，下游 Key 放开发者门户。

## 2. 组件架构

```
ChannelManagementPage                    # 新页面 /channels
├── ChannelToolbar                       # 搜索+筛选+新建按钮
├── ChannelGroupedList                   # 按供应商分组
│   ├── ProviderGroupHeader              # Logo+名称+统计+折叠
│   └── ChannelCard                      # 一行一卡
└── ChannelDetailDrawer                  # 右侧抽屉
    ├── ChannelDetailHeader              # 供应商内联+操作按钮
    └── ChannelDetailGrid                # 四宫格
        ├── EndpointSection              # → InlineEditableList
        ├── CredentialSection            # → InlineEditableList
        ├── ModelMappingSection          # → InlineEditableList
        └── QuotaSettingsSection         # → 键值对编辑模式

InlineEditableList                        # 通用行内编辑列表
├── props: items, renderItem, renderEditForm, onAdd, onDelete
├── 行内展开编辑
├── 末尾插入空白行
└── Popconfirm 删除确认

ChannelCreateWizard                      # 创建向导
├── QuickOnboardMode                     # 快速接入(3步)
└── ExpertConfigMode                     # 专家配置(5步)
```

## 3. 渠道管理页详设

### 渠道卡片布局（一行一卡，横向铺满）

```
┌─────────────────────────────────────────────────────────────────────┐
│ ● 渠道名称  [状态标签]  🌐 N  🔑 N  🤖 N  计费·P·W   ⚡ XXms  详情→ │
└─────────────────────────────────────────────────────────────────────┘
```

- 左→右：状态点 + 渠道名 | 状态标签 | 资源统计 | 计费信息 | 响应时间色码+详情入口
- 停用渠道：整体降低透明度(0.6)
- 配置中渠道：黄色边框 + 缺失项高亮（"⚠ 缺 Key"）

### 响应时间色码

| 响应时间 | 颜色 |
|---------|------|
| ≤ 500ms | 绿 `#52c41a` |
| ≤ 2s | 黄 `#faad14` |
| > 2s | 红 `#cf1322` |
| 未测试 | 灰 `#8c8c8c` |

### 供应商分组

- 同供应商渠道归组，组头：Logo+名称+聚合统计
- 分组可折叠/展开，状态通过 URL query param 持久化：`?expanded=openai,anthropic`

## 4. 渠道详情抽屉详设

### 头部

- 供应商 Logo + 渠道名 + 状态标签
- 供应商名称（可点击跳转供应商目录）
- 计费模式 · 优先级 · 权重 · 响应时间
- 供应商元信息：官网链接 · API 文档链接
- 操作按钮：测试 / 编辑（渠道属性）/ 停用

### 四宫格

| 区域 | 展示 | 操作 |
|------|------|------|
| 端点 | 协议标签+URL+状态 | +添加 / 行内编辑 / 删除 |
| API Key | 前缀+P/W+最后使用+状态 | +添加 / 测试 / 行内编辑 / 删除 |
| 模型映射 | 模型名→上游名+定价 | 从上游获取 / +添加 / 行内编辑 / 删除 / 查看全部 |
| 配额与设置 | 键值对 | 编辑（整体切换编辑模式） |

### 行内编辑交互

统一的 `InlineEditableList` 组件：

- **编辑**：点击"编辑"→ 当前行原地展开为编辑表单，其他行不变
- **新增**：点击"+ 添加"→ 列表末尾插入空白编辑行，自动聚焦首个输入框
- **删除**：点击"删除"→ Popconfirm 二次确认
- **保存/取消**：编辑行包含保存和取消按钮

### "编辑渠道"按钮 vs 行内编辑边界

- 头部"编辑渠道"按钮：编辑渠道自身属性（名称、计费模式、优先级、权重）
- 四宫格行内编辑：编辑渠道子资源（端点、Key、模型映射、配额设置）
- 两类编辑互不干扰

## 5. 创建流程详设

### 快速接入（3步）

| 步骤 | 内容 | 自动化 |
|------|------|--------|
| 1. 选择供应商模板 | 从模板库选择 | — |
| 2. 粘贴 API Key | 输入 Key | 自动填充端点/协议/模型 |
| 3. 确认模型 | 勾选启用模型 | 自动推荐常用模型 |

从模板创建时自动物化：创建 Provider + Channel + Endpoint + ChannelModel。用户只需提供 Key。

### 专家配置（5步）

| 步骤 | 内容 | 可跳过 |
|------|------|--------|
| 1. 供应商+渠道信息 | 供应商/名/计费/优先级/权重 | 否 |
| 2. 配置端点 | 协议+URL | 是 |
| 3. 添加 Key | Key+优先级+权重 | 是 |
| 4. 选择模型+映射 | 模型列表+上游名映射 | 是 |
| 5. 配额与设置 | RPM/TPM/超时/重试/Header | 是 |

每步可跳过，后续在详情抽屉补充。向导状态通过 URL step param 管理：`/channels/create?mode=expert&step=2`。

## 6. 技术风险与缓解

| 风险 | 缓解措施 |
|------|---------|
| 行内编辑在小空间内拥挤 | 端点/Key 字段少(3-5个)，空间足够；模型映射可横向滚动 |
| 供应商分组折叠状态丢失 | URL query param 持久化 |
| 连通性测试阻塞 UI | 异步测试，spinner 显示，结果轮询更新 |
| 创建向导状态管理 | URL step param，支持浏览器回退 |
| 一次性替换导航影响现有用户 | 供应商目录仍可访问，旧路由 `/providers` 保留为目录页 |

## 7. API 依赖

现有 API 已覆盖大部分需求：

- `GET /api/channels` — flat 列表，前端按 providerId 分组
- `POST /api/channels` — 创建渠道
- `PUT /api/channels/{id}` — 更新渠道
- `GET /api/channels/{id}/endpoints` — 端点列表
- `POST /api/channels/{id}/credentials` — 添加 Key
- `GET /api/channels/{id}/models` — 模型映射列表

需确认：渠道连通性测试端点是否已存在，如不存在需后端新增。

## 8. 测试策略

| 层级 | 范围 |
|------|------|
| 组件单元测试 | InlineEditableList、ChannelCard、ChannelDetailDrawer |
| 集成测试 | 创建流程端到端（快速接入+专家配置） |
| 手动验证 | 渠道管理页交互体验、响应时间色码、配置中状态 |
