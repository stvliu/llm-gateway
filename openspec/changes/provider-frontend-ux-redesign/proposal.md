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
