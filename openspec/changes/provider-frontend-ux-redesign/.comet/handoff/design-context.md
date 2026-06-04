# Comet Design Handoff

- Change: provider-frontend-ux-redesign
- Phase: design
- Mode: compact
- Context hash: ff9b6148ec409e388ea24159f76a871675eeda1d67026d977c5a396443c7ea3f

Generated-by: comet-handoff.sh

OpenSpec remains the canonical capability spec. This handoff is a deterministic, source-traceable context pack, not an agent-authored summary.

## openspec/changes/provider-frontend-ux-redesign/proposal.md

- Source: openspec/changes/provider-frontend-ux-redesign/proposal.md
- Lines: 1-32
- SHA256: 1130d5efc84429de7e1388f9bb4ab45023f31a5aeeb0767d74c0bf317d9b7ceb

```md
## Why

当前供应商页面采用 6 Tab Drawer 模式，渠道资源（端点、Key、模型、配额）被分散在不同 Tab 中。从路由调度视角，渠道（Channel）才是核心实体（RoutingContext 以 channelId 为锚点），供应商只是渠道的分组标签。UI 导航应与数据模型对齐：渠道提升为一级菜单，供应商降级为分组维度。

## What Changes

- **BREAKING**: 侧边栏导航重构——4 菜单项：渠道管理、供应商目录、团队管理、开发者门户。供应商降级为目录，移除独立"API Key"和"模型目录"页面
- 新增渠道管理页面：按供应商分组的渠道卡片列表（一行一卡），显示资源统计+响应时间色码
- 新增渠道详情抽屉：供应商信息内联头部 + 四宫格（端点/Key/模型映射/配额设置）
- 供应商详情页精简：6 Tab → 3 Tab（基本信息/渠道管理/连通性测试），渠道资源统一归入渠道详情
- 创建流程升级：双模式——快速接入(3步) + 专家配置(5步)
- 交互规则统一：编辑=行内展开，新增=插入空白行，删除=Popconfirm
- 上游 Key 管理内嵌到渠道详情，下游 Key 管理移入开发者门户
- 模型管理内嵌在渠道详情的模型映射区中，不设独立页面

## Capabilities

### New Capabilities
- `channel-management-page`: 渠道管理一级页面——卡片列表+详情抽屉+双模式创建
- `channel-detail-drawer`: 渠道详情抽屉——四宫格布局+行内编辑交互
- `inline-edit-pattern`: 行内编辑交互模式——编辑=行内展开，新增=插入空白行

### Modified Capabilities
- `provider-navigation`: 供应商从一级菜单降级为目录，导航结构变更

## Impact

- **前端页面**: Providers 页面重构、新增 Channels 页面、ApiKeys 页面移除、Sidebar 菜单变更、Router 路由变更
- **前端组件**: 新增 ChannelCardView、ChannelDetailDrawer、ChannelCreateWizard 等组件；复用现有 ChannelFormModal、CredentialFormModal、ConnectivityTestPanel
- **后端 API**: 无新增端点，现有 Channel API 已满足需求；可能需要新增渠道连通性测试端点（如 `/channels/{id}/test`）
- **权限模型**: 新增 `channel:read`、`channel:write` 权限标识
- **国际化**: 新增渠道管理相关 i18n 键
```

## openspec/changes/provider-frontend-ux-redesign/design.md

- Source: openspec/changes/provider-frontend-ux-redesign/design.md
- Lines: 1-90
- SHA256: 33f418777c988d3380bc7f56bd28681df56f290ac0a669c2043f305c753842a5

[TRUNCATED]

```md
## Architecture Decision: 渠道为中心的信息架构

核心认知：**供应商 = 渠道的分组**。RoutingContext 的锚点是 channelId，不是 providerId。Provider 的作用：1) 组织归属 2) 品牌标识 3) Catalog 物化入口。

### 数据模型对齐导航层级

```
Provider（分组维度）
  └── Channel（一等公民，导航锚点）
       ├── ChannelEndpoint（协议+URL）
       ├── ChannelCredential（API Key+优先级+权重）
       ├── ChannelModel（模型映射+定价+上游名）
       └── 配额设置（RPM/TPM/超时/重试）
```

### 导航架构变更

| 变更前 | 变更后 |
|--------|--------|
| 供应商（一级菜单） | 渠道管理（一级菜单） |
| 模型（一级菜单） | 移除（模型管理内嵌在渠道详情） |
| API Key（一级菜单） | 移除（上游Key内嵌渠道详情，下游Key放开发者门户） |
| 团队（一级菜单） | 团队管理（一级菜单） |
| 开发者门户（一级菜单） | 开发者门户（一级菜单）+下游Key |
| Catalog（系统设置子项） | 供应商目录（一级菜单） |

### 渠道管理页设计

按供应商分组的渠道卡片列表，一行一卡。卡片横向布局：状态点+渠道名 | 状态标签 | 端点/Key/模型数 | 计费+优先级+权重 | 响应时间色码 | 详情入口。

供应商分组：组头显示 Logo+名称+聚合统计，可折叠/展开。

### 渠道详情抽屉设计

头部：供应商Logo + 渠道名 + 状态 + 供应商元信息（官网/文档） + 操作按钮（测试/编辑/停用）

四宫格：
- 端点：协议标签+URL+状态+行内编辑
- API Key：前缀+优先级/权重+最后使用时间+测试+行内编辑
- 模型映射：模型名→上游名+定价+从上游获取+行内编辑
- 配额与设置：键值对展示+编辑模式切换

### 交互规则

统一规则：编辑=行内展开，新增=插入空白行，删除=Popconfirm，测试=响应时间色码。

"编辑渠道"按钮（头部）vs 行内编辑（四宫格）边界：头部按钮编辑渠道自身属性（名称/计费/优先级/权重），四宫格编辑子资源（端点/Key/模型/配额）。

### 创建流程

快速接入（3步）：选模板 → 粘贴Key → 确认模型。从模板创建时自动物化 Provider+Channel+Endpoint+ChannelModel。

专家配置（5步）：选供应商+渠道信息 → 配端点 → 加Key → 选模型 → 配额设置。每步可跳过，后续在详情补充。

### 响应时间色码

≤500ms=绿，≤2s=黄，>2s=红，未测试=灰。

### 配置中状态

缺Key/缺端点/缺模型时显示黄色"配置中"状态并高亮缺失项。

## Approach Selection

### 方案选择：渠道详情抽屉模式

评估了三种方案：
1. 渠道卡片展开模式（改动最小但多渠道时页面过长）
2. 渠道详情抽屉模式（渠道概念清晰、一屏可见、选中）
3. 渠道独立页面模式（改动最大、供应商关联感减弱）

选择方案 B：渠道详情抽屉。权衡：改动量适中、概念清晰、一屏可见、可复用现有组件。

## Data Flow

```
渠道列表页
  → 渠道卡片（点击"详情 →")
    → 渠道详情抽屉（四宫格）
      → 行内编辑（端点/Key/模型/配额）
```

Full source: openspec/changes/provider-frontend-ux-redesign/design.md

## openspec/changes/provider-frontend-ux-redesign/tasks.md

- Source: openspec/changes/provider-frontend-ux-redesign/tasks.md
- Lines: 1-45
- SHA256: 675cfa635b51e76cddcd7185617d9ddfb89a9b4eb9ceded133d9c017dff7a7ca

```md
## Tasks

### 阶段 1：导航结构重构

- [ ] 1.1 更新侧边栏菜单：渠道管理 / 供应商目录 / 团队管理 / 开发者门户（4项）
- [ ] 1.2 更新路由配置：新增 `/channels` 路由，调整 `/providers` 路由
- [ ] 1.3 移除独立 API Key 和模型页面的菜单入口
- [ ] 1.4 供应商目录页面：从现有 Providers 页面精简为 Catalog 浏览+物化入口

### 阶段 2：渠道管理页面

- [ ] 2.1 创建渠道管理页面骨架：工具栏（搜索+供应商筛选+状态筛选+新建按钮）
- [ ] 2.2 实现按供应商分组的渠道卡片列表（一行一卡，横向布局）
- [ ] 2.3 实现供应商分组头（Logo+名称+聚合统计+折叠/展开）
- [ ] 2.4 实现渠道卡片内容：状态点+渠道名+状态标签+资源统计+计费信息+响应时间
- [ ] 2.5 实现配置中状态高亮（缺Key/缺端点/缺模型）
- [ ] 2.6 实现渠道连通性测试：点击测试→响应时间色码更新

### 阶段 3：渠道详情抽屉

- [ ] 3.1 创建渠道详情抽屉组件
- [ ] 3.2 实现头部区域：供应商Logo+渠道名+状态+元信息+操作按钮
- [ ] 3.3 实现端点区：协议标签+URL+状态+行内编辑+添加
- [ ] 3.4 实现 API Key 区：前缀+优先级/权重+最后使用时间+测试+行内编辑+添加
- [ ] 3.5 实现模型映射区：模型名→上游名+定价+从上游获取+行内编辑+添加+查看全部
- [ ] 3.6 实现配额与设置区：键值对展示+编辑模式切换
- [ ] 3.7 实现行内编辑交互模式（编辑=行内展开，新增=插入空白行，删除=Popconfirm）

### 阶段 4：创建流程

- [ ] 4.1 实现快速接入向导（3步：选模板→粘Key→确认模型）
- [ ] 4.2 实现专家配置向导（5步：渠道信息→端点→Key→模型→配额）
- [ ] 4.3 对接 Catalog 物化 API（选模板后自动创建 Provider+Channel+Endpoint+ChannelModel）

### 阶段 5：供应商详情页精简

- [ ] 5.1 供应商详情页 Tab 精简：6 Tab → 3 Tab（基本信息/渠道管理/连通性测试）
- [ ] 5.2 "渠道管理"Tab 复用渠道卡片列表组件
- [ ] 5.3 移除独立的接入点/Key/模型映射/限流配额/高级设置 Tab

### 阶段 6：收尾

- [ ] 6.1 开发者门户：增加下游 Key 管理功能
- [ ] 6.2 更新 i18n 国际化键
- [ ] 6.3 清理废弃代码（旧 Provider 详情页组件、API Key 独立页面组件、模型独立页面组件）
```

