## Task 3: 侧边栏配置

**Files:**
- Create: `site/astro.sidebar.ts`
- Modify: `site/astro.config.ts`（Task 1 已 import sidebar，无需再改，仅确认）

**目标：** 8 大能力域分组 + 企业版 badge 标注，全部条目渲染（不做条件隐藏）。每个 sidebar 条目的 `slug` 必须对应 Task 2 中已创建的 `.mdx` 文件，确保 linkcheck 通过。

- [ ] **Step 1: 创建 `site/astro.sidebar.ts`**

```ts
import type { SidebarItem } from 'astro';

// 企业版 badge：Starlight 原生能力，variant: 'tip' 显示为绿色。
const enterpriseBadge = { text: '企业版', variant: 'tip' as const };

// 8 大能力域分组 + 快速开始 + 参考。
// 全部条目均渲染（不做条件隐藏）；企业版专属项附 badge。
export const sidebar: SidebarItem[] = [
  {
    label: '快速开始',
    collapsed: true,
    items: [
      { label: '需求规格', slug: 'guide/spec' },
      { label: '架构章程', slug: 'guide/constitution' },
    ],
  },
  {
    label: 'API 网关',
    collapsed: true,
    items: [
      { label: 'API 网关', slug: 'api-gateway' },
      { label: 'API 参考', slug: 'api/api-spec' },
      { label: 'OpenAI 兼容端点', slug: 'api-gateway', badges: [enterpriseBadge] },
    ],
  },
];
```

> ⚠️ 上面是结构示意。实际完整配置见 Step 2（避免重复条目）。请使用 Step 2 的完整版本**替换**本文件全部内容。

- [ ] **Step 2: 写入完整的 `site/astro.sidebar.ts`**

用以下完整内容**覆盖** Step 1 创建的文件。每个 slug 严格对应 Task 2 产出的 `.mdx`：

```ts
import type { SidebarItem } from 'astro';

// 企业版 badge：Starlight 原生，variant 'tip' = 绿色。
const enterpriseBadge = { text: '企业版', variant: 'tip' as const };

export const sidebar: SidebarItem[] = [
  {
    label: '快速开始',
    collapsed: true,
    items: [
      { label: '需求规格', slug: 'guide/spec' },
      { label: '架构章程', slug: 'guide/constitution' },
    ],
  },
  {
    label: 'API 网关',
    collapsed: true,
    items: [
      { label: 'API 网关概览', slug: 'api-gateway' },
      { label: 'API 参考', slug: 'api/api-spec' },
    ],
  },
  {
    label: 'Provider 管理',
    collapsed: true,
    items: [
      { label: 'Provider 管理概览', slug: 'provider-management' },
      { label: '代理配置', slug: 'provider-management', badge: enterpriseBadge },
    ],
  },
  {
    label: '路由',
    collapsed: true,
    items: [
      { label: '路由设计', slug: 'features/routing' },
    ],
  },
  {
    label: '用户与认证',
    collapsed: true,
    items: [
      { label: '用户与认证概览', slug: 'auth' },
    ],
  },
  {
    label: '密钥管理',
    collapsed: true,
    items: [
      { label: '密钥管理概览', slug: 'apikey-management' },
    ],
  },
  {
    label: 'Token 计量与配额',
    collapsed: true,
    items: [
      { label: 'Token 计量与配额概览', slug: 'token-quota' },
    ],
  },
  {
    label: '安全与风控',
    collapsed: true,
    items: [
      { label: '安全与风控概览', slug: 'security' },
      { label: '容灾方案', slug: 'features/resilience' },
    ],
  },
  {
    label: '可观测性',
    collapsed: true,
    items: [
      { label: '可观测性概览', slug: 'observability' },
    ],
  },
  {
    label: '高级能力',
    collapsed: true,
    items: [
      { label: '语义缓存', slug: 'features/semantic-cache', badge: enterpriseBadge },
      { label: 'MCP 协议', slug: 'features/mcp-protocol', badge: enterpriseBadge },
    ],
  },
  {
    label: '模型广场',
    collapsed: true,
    items: [
      { label: '模型广场', slug: 'features/model-plaza' },
    ],
  },
  {
    label: '参考',
    collapsed: true,
    items: [
      { label: '功能特性总览', slug: 'features/index' },
      { label: '技术架构', slug: 'architecture/technical' },
      { label: '应用架构', slug: 'architecture/application' },
      { label: '数据架构', slug: 'architecture/data' },
      { label: '信息架构', slug: 'architecture/information' },
    ],
  },
];
```

> 说明：`badge` 字段名以实际安装的 Starlight 版本 API 为准。Starlight 0.28+ sidebar 项支持 `badge: { text, variant }`。若版本字段名为 `badges: [badge]`（数组形式），改为数组形式。

- [ ] **Step 3: 验证侧边栏渲染**

Run:
```bash
cd site && pnpm dev
```
Expected: 访问 `http://localhost:4321/`，左侧侧边栏显示 12 个分组（8 能力域 + 快速开始 + 高级能力 + 模型广场 + 参考），「语义缓存」「MCP 协议」条目旁显示绿色「企业版」badge。点击任一条目可跳转到对应文档页（无 404）。Ctrl+C 停止。

- [ ] **Step 4: Commit**

```bash
git add site/astro.sidebar.ts
git commit -m "feat(site): 配置 8 能力域侧边栏+企业版 badge 标注"
```

---

