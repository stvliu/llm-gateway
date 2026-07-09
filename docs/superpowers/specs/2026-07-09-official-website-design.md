---
comet_change: official-website
role: technical-design
canonical_spec: openspec
---

# LLM-Gateway 官方网站 Design Doc

## 1. 上下文

LLM-Gateway 是企业级大模型网关（开源 Apache-2.0 核心 + 企业版增值），当前对外仅 GitHub README。本设计建设官网 + 文档站一体化站点，对标 thingsboard.io，承载开源传播与企业版转化双目标。

本 Design Doc 是对 open 阶段 `design.md` 高层框架的**深度技术细化**。open 产物（proposal/design/tasks/specs）是上游事实源，本文档不重写需求，仅细化实现方案、技术风险、测试策略与边界条件。

## 2. 目标 / 非目标

**目标**
- P0 交付四产物：脚手架、侧边栏配置、版本对比页、首页文案
- 文档站 P0 中文 + 营销页中英双语
- 11 篇核心公开文档迁移
- Cloudflare Pages 部署流水线

**非目标**
- 托管云产品线页、用例页、案例页、替代方案着陆页、博客改写
- 文档站英文版（P0 不配 en locale，作为后续独立任务）
- gateway-boot/console/cli 源码改动
- 首页控制台真实截图（P0 占位，依赖 console 运行）

## 3. 架构决策

### 3.1 侧边栏：单一文档站 + 企业版 badge（已确认）

一套 `astro.sidebar.ts`，8 大能力域条目全部渲染，企业版专属项附 Starlight 原生 badge：

```ts
{ label: '语义缓存', slug: 'features/semantic-cache',
  badge: { text: '企业版', variant: 'tip' } }
```

**不做条件渲染、不做两套文档站。** 理由：企业版是标准版超集，文档结构一致；开源用户可见完整能力地图；Starlight badge 是原生能力，零定制成本。

> 修正 open 阶段 design.md 的"isEnterprise 控制渲染"。

### 3.2 i18n：文档站 P0 中文，营销页双语（已确认）

- **文档站**：Starlight 仅配 root locale（zh-CN），不配 en，不建英文骨架，不引入 lunaria。避免未翻译 fallback 噪音。
- **营销页**（首页 / 版本对比页 / 顶部导航）：中英双语，通过 `src/data/i18n/*.ts` 字典 + Astro 动态路由（`src/pages/` 与 `src/pages/en/`）实现，组件按 locale 取值。
- **后续**：文档站 en locale + lunaria 作为独立后续任务。

> 修正 open 阶段 proposal/design 的"文档站中英双语同步"。

### 3.3 其余实现决策

- **Astro 6 + @astrojs/starlight**：营销页 + 文档站同构，零 JS 默认输出。
- **内容数据化**：文案入 `src/data/`，组件只渲染（学 thingsboard.io）。
- **文档复制迁移**：11 篇 `docs/*.md` 复制到 `src/content/docs/`，原 `docs/` 保留。
- **site/ 依赖隔离**：独立 `package.json` + pnpm，不共享 console 的 React/Vite 依赖。
- **部署**：GitHub Actions -> Cloudflare Pages（PR 预览 + master 生产）。

## 4. 交付物详细设计

### 4.1 脚手架与目录结构

```
site/
├── astro.config.ts          # Starlight 集成，root locale，sitemap，editLink
├── astro.sidebar.ts         # 8 能力域分组 + 企业版 badge
├── astro.redirects.ts       # 旧链接重定向
├── package.json             # astro、starlight、sass、sharp（pnpm）
├── .nvmrc                   # Node 版本锁定
├── src/
│   ├── content/docs/        # Starlight 文档源（MDX，英文 slug）
│   │   ├── guide/           # 快速开始、部署、配置、constitution
│   │   ├── api/             # api-spec
│   │   ├── architecture/    # 技术/应用/数据/信息架构
│   │   └── features/        # 8 能力域文档（routing/resilience/...）
│   ├── pages/               # 营销页（中文 root）
│   │   ├── index.astro
│   │   ├── standard-vs-enterprise/index.astro
│   │   ├── contact-us.astro
│   │   └── en/              # 英文营销页镜像
│   │       ├── index.astro
│   │       └── standard-vs-enterprise/index.astro
│   ├── data/
│   │   ├── editionDiff.ts          # 版本对比表结构化数据
│   │   ├── homeFeatures.ts         # 8 能力域功能卡
│   │   ├── homeProducts.ts         # 三产品卡
│   │   ├── homeEcosystem.ts        # 生态组件
│   │   ├── navigation.ts           # 顶部导航
│   │   └── i18n/                   # 中英文案字典
│   │       ├── home.ts             # { zh: {...}, en: {...} }
│   │       └── editionDiff.ts
│   ├── components/           # Hero/ProductCard/FeatureCard/EcosystemCard/Carousel
│   ├── components/starlight/ # 主题覆盖：Header/Footer/SiteTitle
│   ├── layouts/              # BaseLayout（营销页）
│   └── styles/               # SCSS
├── public/                  # favicon、占位图
└── scripts/                 # linkcheck/slugcheck lint
```

`astro.config.ts` 关键配置：
- `starlight.locales`: `{ root: { label: '简体中文', lang: 'zh-CN' } }`（P0 仅 root）
- `trailingSlash: 'always'`
- `integrations`: `[starlight({...}), sitemap()]`
- `editLink.baseUrl`: 指向 GitHub `site/src/content/docs/`

### 4.2 侧边栏配置（`astro.sidebar.ts`）

8 大能力域分组，每组 `collapsed: true`，企业版项附 badge：

```ts
export const sidebar = [
  { label: '快速开始', collapsed: true, items: [
    { label: '概述', slug: 'guide/getting-started' },
    { label: '安装', slug: 'guide/installation' },
    { label: '部署', slug: 'guide/deployment' },
  ]},
  { label: 'API 网关', collapsed: true, items: [
    { label: 'OpenAI 兼容端点', slug: 'features/api-gateway/openai' },
    { label: 'Anthropic 兼容端点', slug: 'features/api-gateway/anthropic' },
    { label: '协议互转', slug: 'features/api-gateway/protocol-conversion' },
  ]},
  // ... Provider 管理 / 路由 / 用户与认证 / 密钥管理 / Token 计量 / 安全与风控 / 可观测性
  { label: '语义缓存', slug: 'features/semantic-cache',
    badge: { text: '企业版', variant: 'tip' } },
  { label: 'MCP 协议', slug: 'features/mcp-protocol',
    badge: { text: '企业版', variant: 'tip' } },
  { label: '参考', collapsed: true, items: [
    { label: '架构章程', slug: 'guide/constitution' },
    { label: '技术架构', slug: 'architecture/technical' },
  ]},
];
```

### 4.3 版本对比页

`src/data/editionDiff.ts` 结构化数据：

```ts
export interface DiffItem { name: string; standard: boolean; enterprise: boolean; }
export interface DiffCategory { category: string; items: DiffItem[]; }
export const editionDiff: DiffCategory[] = [
  { category: 'API 网关', items: [
    { name: 'OpenAI/Anthropic 兼容端点', standard: true, enterprise: true },
    { name: 'SSE 流式 / 协议互转', standard: true, enterprise: true },
  ]},
  { category: 'Provider 管理', items: [
    { name: '多 Key 轮换 / 负载均衡 / 熔断', standard: true, enterprise: true },
    { name: 'HTTP/S、Socket5 代理配置', standard: false, enterprise: true },
  ]},
  // ... 路由 / 认证 / 密钥 / Token / 安全 / 可观测性 / 高级能力 / 部署 / 支持
];
```

页面 `src/pages/standard-vs-enterprise/index.astro` 渲染：引言 -> 标准版介绍 -> 企业版介绍 -> 对比表（`rowspan` 按类别分组）-> 迁移路径段（`llm-gateway.edition: standard -> enterprise` 配置切换，数据兼容）-> 如何开始。英文镜像 `src/pages/en/standard-vs-enterprise/index.astro`，文案取 `src/data/i18n/editionDiff.ts`。

### 4.4 首页

`src/data/i18n/home.ts`：

```ts
export const home = {
  zh: {
    hero: { title: '企业级大模型网关 · 更合规、更安全、更智能、更易用',
            subtitle: 'OpenAI / Anthropic 双标准 API，统一接入 50+ 主流大模型...' },
    products: [ { name: '标准版', desc: 'Apache-2.0 开源，单机即可起步', href: '/docs/' }, ... ],
    features: [ { title: 'API 网关', desc: '...', href: '/docs/features/' }, ... ],
  },
  en: { hero: {...}, products: [...], features: [...] }
};
```

`src/pages/index.astro` 区块顺序（对标 thingsboard.io）：Hero -> 信任背书 logo 墙 -> 四差异化 -> 能力叙事（双协议/路由降级/安全合规）-> 三产品卡 -> 生态组件 -> 控制台轮播（P0 占位图）-> 8 能力域功能网格 -> 底部 CTA。组件按 locale 从 `home` 字典取值。

## 5. 文档迁移映射表

| 现有 `docs/` 文件 | 目标 slug | frontmatter title |
|---|---|---|
| `spec.md` | `guide/spec` | 需求规格 |
| `api-spec.md` | `api/` | API 参考 |
| `技术架构.md` | `architecture/technical` | 技术架构 |
| `应用架构.md` | `architecture/application` | 应用架构 |
| `数据架构.md` | `architecture/data` | 数据架构 |
| `信息架构.md` | `architecture/information` | 信息架构 |
| `constitution.md` | `guide/constitution` | 架构章程 |
| `routing-design.md` | `features/routing` | 路由设计 |
| `容灾方案设计.md` | `features/resilience` | 容灾方案 |
| `AI-Gateway功能特性.md` | `features/` | 功能特性 |
| `model-experience-design.md` | `features/model-plaza` | 模型广场 |

每篇加 Starlight frontmatter：`---\ntitle: <标题>\ndescription: <描述>\n---`。原 `docs/` 保留不删。内部调研文档（apipark/voapi/cc-switch/FEASIBILITY 等）不迁移。

## 6. SEO

- canonical 与 sitemap 通过 `PUBLIC_SITE_URL` 环境变量配置，P0 占位 `https://llm-gateway.dev`，域名确定后一处修改。
- `trailingSlash: 'always'` 统一 URL 形态。
- 文档 frontmatter 的 `description` 用于 og:description。
- `@astrojs/sitemap` 自动生成 sitemap.xml。

## 7. 测试策略

| 验证项 | 方法 | 通过标准 |
|--------|------|---------|
| 构建 | `pnpm build` | 无错误，产物生成 |
| 链接 | `lint:linkcheck` 脚本 | 无断链 |
| slug | `lint:slugcheck` 脚本 | 文档 slug 为合规英文 kebab-case |
| 中英切换 | 手动 | 营销页 `/` 与 `/en/` 均可访问，文案切换 |
| 企业版 badge | 手动 | 侧边栏企业版项附 badge，全部渲染 |
| 对比表覆盖度 | 手动 | 覆盖 README 全部 ✅/🔒 项 |
| 部署 | GitHub Actions | CF Pages 预览部署成功 |

lunaria P0 不引入（文档站无 en）。

## 8. 风险与缓解

- **[营销页双语自建 i18n]** -> 文档站 root-only，营销页用 `src/data/i18n/` + `src/pages/en/` 镜像。代价：locale 切换不自享 Starlight 自动路由。缓解：营销页数量少（首页+对比页），手动镜像成本可控。
- **[文档 slug 改名]** -> 建第 5 节映射表，迁移时严格对照。原 `docs/` 保留，`astro.redirects.ts` 可加旧路径重定向（如需）。
- **[截图素材]** -> 首页轮播 P0 占位图，build 末段用 gateway-console 真实截图替换。
- **[域名未定]** -> `PUBLIC_SITE_URL` 占位，环境变量驱动。
- **[双语工作量]** -> 营销页双语全量（首页+对比页+导航），文档站 P0 中文，翻译工作收敛。

## 9. Spec Patch 记录

回写 open 阶段产物（Design Doc 创建后同步执行）：

- `specs/website/spec.md` Requirement「文档站侧边栏组织」：场景「标准版上下文隐藏企业版条目」-> 调整为「企业版专属条目附 badge 标注，全部上下文均渲染」。
- `specs/website/spec.md` Requirement「中英双语国际化」：限定为「营销页中英双语；文档站 P0 仅中文，en locale 作为后续独立任务」。
- `tasks.md`：删除 2.2「建立英文目录骨架」；任务 8「英文文档翻译」-> 调整为「文档站 en locale 作为后续独立任务，P0 营销页英文文案已在 5.3/6.1 覆盖」。
