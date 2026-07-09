---
change: official-website
design-doc: docs/superpowers/specs/2026-07-09-official-website-design.md
base-ref: 9513ee84dabbccd7e9fdbc4ce48d888c4256e43a
---

# 官方网站建设 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: 使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 按任务逐个实施。步骤使用复选框（`- [ ]`）语法跟踪。

**Goal:** 在 `site/` 目录建设 Astro 6 + Starlight 官网（文档站 + 营销页一体化），交付脚手架、11 篇文档迁移、8 能力域侧边栏（企业版 badge）、版本对比页、首页、Cloudflare Pages 部署流水线。

**Architecture:** 单一文档站 + 企业版 badge 标注（不做条件渲染、不做两套站）。文档站 P0 仅中文（Starlight root locale，不配 en）；营销页（首页/对比页/导航）中英双语，通过 `src/data/i18n/*.ts` 字典 + `src/pages/en/` 镜像路由实现。lunaria P0 不引入。site/ 独立 package.json + pnpm，与 gateway-boot/console/cli 平级，不共享 React/Vite 依赖。

**Tech Stack:** Astro 6、@astrojs/starlight、@astrojs/sitemap、sass、sharp、pnpm、Node 20+、GitHub Actions、Cloudflare Pages。

---

## 文件结构

`site/` 为新建独立前端项目，与现有 Maven 多模块平级。下列文件为本计划将创建/修改的全部产物。

```
site/                                    # 新建独立 package（pnpm）
├── astro.config.ts                      # Starlight 集成、root locale、sitemap、editLink、trailingSlash
├── astro.sidebar.ts                     # 8 能力域分组 + 企业版 badge
├── astro.redirects.ts                   # 旧 docs/ 链接重定向（预留，P0 可空）
├── package.json                         # astro/starlight/sass/sharp/sitemap 依赖 + lint 脚本
├── .nvmrc                               # Node 版本锁定（20）
├── tsconfig.json                        # Astro 严格 TS
├── src/
│   ├── content/docs/                    # Starlight 文档源（.mdx，英文 slug）
│   │   ├── guide/
│   │   │   ├── spec.mdx                 # 源 docs/spec.md
│   │   │   └── constitution.mdx         # 源 docs/constitution.md
│   │   ├── api/
│   │   │   └── api-spec.mdx             # 源 docs/api-spec.md
│   │   ├── architecture/
│   │   │   ├── technical.mdx            # 源 docs/技术架构.md
│   │   │   ├── application.mdx          # 源 docs/应用架构.md
│   │   │   ├── data.mdx                 # 源 docs/数据架构.md
│   │   │   └── information.mdx          # 源 docs/信息架构.md
│   │   ├── features/
│   │   │   ├── index.mdx                # 源 docs/AI-Gateway功能特性.md（功能特性总览）
│   │   │   ├── routing.mdx              # 源 docs/routing-design.md
│   │   │   ├── resilience.mdx           # 源 docs/容灾方案设计.md
│   │   │   ├── model-plaza.mdx          # 源 docs/model-experience-design.md
│   │   │   ├── semantic-cache.mdx       # 企业版占位（内容提炼自 README）
│   │   │   └── mcp-protocol.mdx         # 企业版占位（内容提炼自 README）
│   │   ├── api-gateway.mdx              # 能力域概览（README API 网关章节）
│   │   ├── provider-management.mdx      # 能力域概览（README Provider 章节）
│   │   ├── auth.mdx                     # 能力域概览（README 用户与认证章节）
│   │   ├── apikey-management.mdx        # 能力域概览（README 密钥管理章节）
│   │   ├── token-quota.mdx              # 能力域概览（README Token 章节）
│   │   ├── security.mdx                 # 能力域概览（README 安全与风控章节）
│   │   └── observability.mdx            # 能力域概览（README 可观测性章节）
│   ├── pages/                           # 营销页（中文 root）
│   │   ├── index.astro                  # 首页
│   │   ├── standard-vs-enterprise/index.astro
│   │   ├── contact-us.astro             # 联系页（P0 占位）
│   │   └── en/                          # 英文营销页镜像
│   │       ├── index.astro
│   │       └── standard-vs-enterprise/index.astro
│   ├── data/
│   │   ├── editionDiff.ts               # 版本对比表结构化数据
│   │   ├── homeFeatures.ts              # 8 能力域功能卡
│   │   ├── homeProducts.ts             # 三产品卡
│   │   ├── homeEcosystem.ts            # 生态组件
│   │   ├── navigation.ts               # 顶部导航
│   │   └── i18n/
│   │       ├── home.ts                  # { zh: {...}, en: {...} }
│   │       └── editionDiff.ts
│   ├── components/                      # Hero/ProductCard/FeatureCard/EcosystemCard/Carousel
│   ├── components/starlight/           # 主题覆盖：Header/Footer/SiteTitle
│   ├── layouts/
│   │   └── BaseLayout.astro            # 营销页基础布局
│   └── styles/
│       └── global.scss
├── public/                             # favicon、占位图
├── scripts/
│   ├── linkcheck.mjs                   # 断链检查
│   └── slugcheck.mjs                   # slug 合规检查
└── .github/workflows/
    └── deploy-site.yml                 # 根仓库工作流（放根目录 .github/）
```

**设计决策（来自 Design Doc，已锁定，勿重新设计）：**
- 单一文档站 + 企业版 badge：一套 `astro.sidebar.ts`，企业版专属项附 `{ text: '企业版', variant: 'tip' }`，全部渲染。
- i18n：文档站 P0 仅 root locale（zh-CN）；营销页双语。
- 文档 slug：中文文件名改英文 slug（见 Design Doc §5 映射表），原 `docs/` 保留不删。
- 部署：GitHub Actions -> Cloudflare Pages。

**验证策略（Design Doc §7）：** 本项目为静态文档站，无可单元测试的业务逻辑。验证手段为 `pnpm build`（构建无错）、`lint:linkcheck`（无断链）、`lint:slugcheck`（slug 合规）、手动验收（中英切换、badge、对比表覆盖度、CF Pages 预览）。每个任务以验证步骤 + commit 收尾，遵循频繁提交。

---

## Task 1: 项目脚手架与 i18n 架构

**Files:**
- Create: `site/package.json`
- Create: `site/.nvmrc`
- Create: `site/tsconfig.json`
- Create: `site/astro.config.ts`
- Create: `site/astro.redirects.ts`
- Create: `site/src/env.d.ts`
- Create: `site/src/styles/global.scss`
- Create: `site/public/favicon.svg`
- Create: `site/.gitignore`

- [x] **Step 1: 创建 `site/.nvmrc` 锁定 Node 版本**

文件内容：

```
20
```

- [x] **Step 2: 创建 `site/.gitignore`**

```
node_modules/
dist/
.astro/
.output/
*.log
.DS_Store
.env
.env.production
```

- [x] **Step 3: 创建 `site/package.json`**

```json
{
  "name": "llm-gateway-site",
  "version": "0.1.0",
  "private": true,
  "type": "module",
  "engines": { "node": ">=20.0.0" },
  "scripts": {
    "dev": "astro dev",
    "build": "astro build",
    "preview": "astro preview",
    "astro": "astro",
    "lint:linkcheck": "node scripts/linkcheck.mjs",
    "lint:slugcheck": "node scripts/slugcheck.mjs"
  },
  "dependencies": {
    "@astrojs/sitemap": "^3.2.1",
    "@astrojs/starlight": "^0.30.0",
    "astro": "^6.0.0",
    "sass": "^1.80.0",
    "sharp": "^0.33.5"
  }
}
```

> 注：执行 `pnpm install` 时若 starlight 对 astro 6 有 peer 警告，以实际可安装的兼容版本为准（starlight 0.30+ 支持 Astro 5/6）。若 Astro 6 尚未稳定，降级为 `astro: "^5.0.0"` + 对应 starlight，配置不变。

- [x] **Step 4: 创建 `site/tsconfig.json`**

```json
{
  "extends": "astro/tsconfigs/strict",
  "include": [".astro/types.d.ts", "**/*"],
  "exclude": ["dist"]
}
```

- [x] **Step 5: 创建 `site/src/env.d.ts`**

```ts
/// <reference path="../.astro/types.d.ts" />
```

- [x] **Step 6: 创建 `site/astro.redirects.ts`（P0 预留，导出空对象）**

```ts
// 旧 docs/ 链接 -> 新 slug 的重定向映射。
// P0 预留：原 docs/ 保留不删，如需重定向在此追加。
// 键为旧路径（相对站点根），值为新 slug。
export const redirects: Record<string, string> = {};

export default redirects;
```

- [x] **Step 7: 创建 `site/src/styles/global.scss`（营销页全局样式占位）**

```scss
// 营销页全局样式。Starlight 文档站样式由 Starlight 自带，此处仅影响营销页。
:root {
  --brand-primary: #6653e3;
  --brand-bg: #ffffff;
  --brand-text: #1a1a2e;
  --enterprise-badge: #16a34a;
}

html {
  scroll-behavior: smooth;
}

body {
  margin: 0;
  font-family: system-ui, -apple-system, "Segoe UI", "PingFang SC",
    "Hiragino Sans GB", "Microsoft YaHei", sans-serif;
  color: var(--brand-text);
  background: var(--brand-bg);
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
}
```

- [x] **Step 8: 创建 `site/public/favicon.svg`**

```xml
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 32 32">
  <rect width="32" height="32" rx="6" fill="#6653e3"/>
  <text x="16" y="22" font-size="18" font-family="sans-serif" font-weight="bold" fill="#fff" text-anchor="middle">G</text>
</svg>
```

- [x] **Step 9: 创建 `site/astro.config.ts`**

```ts
import { defineConfig } from 'astro/config';
import starlight from '@astrojs/starlight';
import sitemap from '@astrojs/sitemap';
import { sidebar } from './astro.sidebar.ts';

// 站点根 URL，P0 占位，域名确定后一处修改。
const SITE_URL = process.env.PUBLIC_SITE_URL ?? 'https://llm-gateway.dev';

// https://astro.build/config
export default defineConfig({
  site: SITE_URL,
  trailingSlash: 'always',
  integrations: [
    sitemap(),
    starlight({
      title: 'LLM-Gateway',
      // P0 仅 root locale（简体中文），不配 en，避免未翻译 fallback 噪音。
      locales: {
        root: { label: '简体中文', lang: 'zh-CN' },
      },
      sidebar,
      social: {
        github: 'https://github.com/codingas/llm-gateway',
      },
      editLink: {
        baseUrl: 'https://github.com/codingas/llm-gateway/edit/master/site/src/content/docs',
      },
      customCss: ['./src/styles/global.scss'],
      // 主题覆盖在 Task 4 启用，此处先留空对象。
      components: {},
    }),
  ],
});
```

- [x] **Step 10: 安装依赖**

Run:
```bash
cd site && pnpm install
```
Expected: 依赖安装成功，生成 `node_modules/` 与 `pnpm-lock.yaml`。若 astro 6 不可用，按 Step 3 注释降级 astro 主版本后重装。

- [x] **Step 11: 验证脚手架可启动**

Run:
```bash
cd site && pnpm dev
```
Expected: 开发服务在 `http://localhost:4321/` 启动，无报错（此时无文档内容，Starlight 会渲染空文档站首页）。Ctrl+C 停止。

- [x] **Step 12: Commit**

```bash
git add site/
git commit -m "feat(site): 初始化 Astro+Starlight 脚手架与 i18n 架构（root locale）"
```

---

## Task 2: 文档迁移

**Files:**
- Create: `site/src/content/docs/guide/spec.mdx`（源 `docs/spec.md`）
- Create: `site/src/content/docs/guide/constitution.mdx`（源 `docs/constitution.md`）
- Create: `site/src/content/docs/api/api-spec.mdx`（源 `docs/api-spec.md`）
- Create: `site/src/content/docs/architecture/technical.mdx`（源 `docs/技术架构.md`）
- Create: `site/src/content/docs/architecture/application.mdx`（源 `docs/应用架构.md`）
- Create: `site/src/content/docs/architecture/data.mdx`（源 `docs/数据架构.md`）
- Create: `site/src/content/docs/architecture/information.mdx`（源 `docs/信息架构.md`）
- Create: `site/src/content/docs/features/index.mdx`（源 `docs/AI-Gateway功能特性.md`）
- Create: `site/src/content/docs/features/routing.mdx`（源 `docs/routing-design.md`）
- Create: `site/src/content/docs/features/resilience.mdx`（源 `docs/容灾方案设计.md`）
- Create: `site/src/content/docs/features/model-plaza.mdx`（源 `docs/model-experience-design.md`）
- Create: `site/src/content/docs/features/semantic-cache.mdx`（企业版占位，提炼自 README）
- Create: `site/src/content/docs/features/mcp-protocol.mdx`（企业版占位，提炼自 README）
- Create: 7 篇能力域概览页（`api-gateway`/`provider-management`/`auth`/`apikey-management`/`token-quota`/`security`/`observability`，内容来自 README 对应章节）

**迁移映射表（Design Doc §5，逐篇严格对照）：**

| 现有 `docs/` 文件 | 目标路径 | frontmatter title |
|---|---|---|
| `spec.md` | `guide/spec.mdx` | 需求规格 |
| `constitution.md` | `guide/constitution.mdx` | 架构章程 |
| `api-spec.md` | `api/api-spec.mdx` | API 参考 |
| `技术架构.md` | `architecture/technical.mdx` | 技术架构 |
| `应用架构.md` | `architecture/application.mdx` | 应用架构 |
| `数据架构.md` | `architecture/data.mdx` | 数据架构 |
| `信息架构.md` | `architecture/information.mdx` | 信息架构 |
| `AI-Gateway功能特性.md` | `features/index.mdx` | 功能特性 |
| `routing-design.md` | `features/routing.mdx` | 路由设计 |
| `容灾方案设计.md` | `features/resilience.mdx` | 容灾方案 |
| `model-experience-design.md` | `features/model-plaza.mdx` | 模型广场 |

- [ ] **Step 1: 创建 `site/src/content/docs/guide/spec.mdx`**

将 `docs/spec.md` 全文内容复制到 `site/src/content/docs/guide/spec.mdx`，并在文件**最顶部**插入 Starlight frontmatter：

```
---
title: 需求规格
description: LLM-Gateway 完整需求规格说明书
---
```

其后粘贴 `docs/spec.md` 正文。原 `docs/spec.md` 保留不删。

- [ ] **Step 2: 对其余 10 篇迁移文档重复 Step 1 的流程**

每篇操作一致：复制源文件正文 -> 新建目标 `.mdx` -> 顶部插入 frontmatter（title/description 见映射表）-> 原 `docs/` 文件保留。具体：

| 目标文件 | 源文件 | frontmatter |
|---|---|---|
| `guide/constitution.mdx` | `docs/constitution.md` | `title: 架构章程` / `description: 架构设计铁律` |
| `api/api-spec.mdx` | `docs/api-spec.md` | `title: API 参考` / `description: OpenAI 与 Anthropic 双 API 标准` |
| `architecture/technical.mdx` | `docs/技术架构.md` | `title: 技术架构` / `description: 分层架构与技术选型` |
| `architecture/application.mdx` | `docs/应用架构.md` | `title: 应用架构` / `description: 应用层用例编排` |
| `architecture/data.mdx` | `docs/数据架构.md` | `title: 数据架构` / `description: 数据库设计与实体关系` |
| `architecture/information.mdx` | `docs/信息架构.md` | `title: 信息架构` / `description: 信息组织与导航结构` |
| `features/index.mdx` | `docs/AI-Gateway功能特性.md` | `title: 功能特性` / `description: 全部能力域功能总览` |
| `features/routing.mdx` | `docs/routing-design.md` | `title: 路由设计` / `description: 智能路由与降级策略` |
| `features/resilience.mdx` | `docs/容灾方案设计.md` | `title: 容灾方案` / `description: 熔断重试与故障转移` |
| `features/model-plaza.mdx` | `docs/model-experience-design.md` | `title: 模型广场` / `description: 模型展示与体验设计` |

- [ ] **Step 3: 创建企业版占位文档 `features/semantic-cache.mdx`**

内容提炼自 README §语义缓存，frontmatter + 正文如下：

```
---
title: 语义缓存
description: 相似请求命中缓存，降低成本 30%+
badge: 企业版
---

语义缓存是企业版专属能力，基于向量相似度匹配相似请求并返回缓存结果。

## 核心能力

- **相似请求返回缓存**：语义级别匹配，降低成本 30%+
- **缓存 TTL 配置**：可设置缓存过期时间
- **缓存命中率统计**：监控缓存效果
- **向量相似度搜索**：基于 pgvector 实现

> 此功能为企业版专属，标准版默认关闭（`semantic-cache: false`）。
```

- [ ] **Step 4: 创建企业版占位文档 `features/mcp-protocol.mdx`**

内容提炼自 README §MCP 协议：

```
---
title: MCP 协议
description: Model Context Protocol 支持（Resources/Prompts/Tools）
badge: 企业版
---

MCP（Model Context Protocol）是企业版专属能力，为大模型提供标准化上下文与工具接入。

## 核心能力

- **Resources**：提供上下文数据
- **Prompts**：提供预定义的提示模板
- **Tools**：提供可调用的工具函数

> 此功能为企业版专属，标准版默认关闭（`mcp-protocol: false`）。
```

- [ ] **Step 5: 创建 7 篇能力域概览页**

为满足 spec「8 能力域全部条目可见」，为缺少独立迁移文档的 7 个能力域各创建一篇概览页，内容来自 README 功能矩阵对应章节（README 第 41-118 行已含每域 ✅/🔒 清单）。路由域用已迁移的 `features/routing.mdx`，无需新建。

每篇概览页使用统一 frontmatter 模板，正文为该域功能清单（Markdown 表格，标准版 ✅ / 企业版 🔒 列）。

示例——创建 `site/src/content/docs/api-gateway.mdx`：

```
---
title: API 网关
description: OpenAI 与 Anthropic 双标准 API 兼容端点
---

LLM-Gateway 同时支持 OpenAI 和 Anthropic 两种 API 标准，统一接入。

## 功能清单

| 功能 | 标准版 | 企业版 |
|---|---|---|
| OpenAI 兼容端点（`/v1/chat/completions`、`/v1/completions`） | ✅ | ✅ |
| Anthropic 兼容端点（`/v1/messages`） | ✅ | ✅ |
| SSE 流式转发（首 token ≤100ms） | ✅ | ✅ |
| 协议转换（OpenAI ↔ Anthropic 互转） | ✅ | ✅ |
| 图像生成端点（`/v1/images/generations`） | ✅ | ✅ |
| 语音合成端点（`/v1/audio/speech`） | ✅ | ✅ |
| 语音识别端点（`/v1/audio/transcriptions`） | ✅ | ✅ |
| 内容审核端点（`/v1/moderations`） | ✅ | ✅ |

详见 [API 参考](/api/api-spec/)。
```

按相同模板创建其余 6 篇，正文数据取自 README 对应章节：

| 目标文件 | title | README 章节（行号） |
|---|---|---|
| `provider-management.mdx` | Provider 管理 | 第 53-62 行 |
| `auth.mdx` | 用户与认证 | 第 71-75 行 |
| `apikey-management.mdx` | 密钥管理 | 第 77-82 行 |
| `token-quota.mdx` | Token 计量与配额 | 第 84-88 行 |
| `security.mdx` | 安全与风控 | 第 90-99 行 |
| `observability.mdx` | 可观测性 | 第 101-107 行 |

每篇正文为「## 功能清单」表格，列名「功能 / 标准版 / 企业版」，行数据逐条来自 README 该章节的 ✅/🔒 项（✅ 对应两列均 ✅，🔒 [企业版] 对应标准版 ❌ / 企业版 ✅）。

- [ ] **Step 6: 复核内部调研文档未进入公开站点**

检查 `site/src/content/docs/` 下不存在以下内部调研文档：`apipark`、`voapi`、`cc-switch`、`FEASIBILITY`、`竞品分析`、`simulator-gateway-verification`、`connectivity-test-design`、`前端重构规划`、`页面设计规范`、`需求实现规划`、`Speckit`、`git-workflow`、`migration/`、`refactor/`、`db/`。

Run:
```bash
cd site && ls src/content/docs/ && echo "---" && find src/content/docs -type f | wc -l
```
Expected: 文件数 = 11 迁移 + 2 企业版占位 + 7 概览 = 20 个 `.mdx`，且输出中无上述内部调研文档名。

- [ ] **Step 7: 验证 Starlight 能解析迁移文档**

Run:
```bash
cd site && pnpm dev
```
Expected: 开发服务启动，访问 `http://localhost:4321/`，Starlight 文档站可渲染（侧边栏此时可能为空，因 Task 3 才配置；但文档文件本身不报构建错误）。Ctrl+C 停止。

- [ ] **Step 8: Commit**

```bash
git add site/src/content/docs/
git commit -m "feat(site): 迁移 11 篇核心文档+2 篇企业版占位+7 篇能力域概览"
```

---

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

## Task 4: Starlight 主题覆盖

**Files:**
- Create: `site/src/components/starlight/Header.astro`
- Create: `site/src/components/starlight/Footer.astro`
- Create: `site/src/components/starlight/SiteTitle.astro`
- Create: `site/src/data/navigation.ts`
- Modify: `site/astro.config.ts`（启用 components 覆盖）

**目标：** 覆盖 Starlight 的 Header（顶部导航：产品/文档/定价/版本对比）、Footer、SiteTitle，使其承载营销导航，文档站与营销页视觉统一。

- [ ] **Step 1: 创建 `site/src/data/navigation.ts`**

```ts
// 顶部导航项（中英双语，营销页与文档站 Header 共用）。
export interface NavItem {
  label: string;
  href: string;
  enLabel: string;
}

export const navigation: NavItem[] = [
  { label: '产品', href: '/#products', enLabel: 'Product' },
  { label: '文档', href: '/docs/', enLabel: 'Docs' },
  { label: '版本对比', href: '/standard-vs-enterprise/', enLabel: 'Editions' },
  { label: '联系我们', href: '/contact-us/', enLabel: 'Contact' },
];
```

- [ ] **Step 2: 创建 `site/src/components/starlight/SiteTitle.astro`**

```astro
---
// 覆盖 Starlight 默认 SiteTitle，链接回首页。
---
<a href="/" class="site-title">
  <span class="logo">LLM-Gateway</span>
</a>

<style>
  .site-title {
    text-decoration: none;
    font-weight: 700;
    font-size: 1.25rem;
    color: var(--sl-color-white);
  }
</style>
```

- [ ] **Step 3: 创建 `site/src/components/starlight/Header.astro`**

```astro
---
// 覆盖 Starlight Header，追加营销导航。
import { navigation } from '../../data/navigation.ts';
// 当前路径判断：/en/ 前缀用英文 label。
const pathname = Astro.url.pathname;
const isEn = pathname.startsWith('/en/');
---
<header class="header">
  <div class="header-inner">
    <a href="/" class="brand">LLM-Gateway</a>
    <nav class="nav">
      {navigation.map((item) => (
        <a href={item.href} class="nav-item">
          {isEn ? item.enLabel : item.label}
        </a>
      ))}
    </nav>
  </div>
</header>

<style>
  .header {
    position: sticky;
    top: 0;
    z-index: 100;
    background: var(--brand-bg, #fff);
    border-bottom: 1px solid #e5e7eb;
  }
  .header-inner {
    max-width: 1200px;
    margin: 0 auto;
    padding: 0 24px;
    height: 56px;
    display: flex;
    align-items: center;
    justify-content: space-between;
  }
  .brand {
    font-weight: 700;
    font-size: 1.25rem;
    text-decoration: none;
    color: var(--brand-text, #1a1a2e);
  }
  .nav {
    display: flex;
    gap: 24px;
  }
  .nav-item {
    text-decoration: none;
    color: var(--brand-text, #1a1a2e);
    font-size: 0.95rem;
  }
  .nav-item:hover {
    color: var(--brand-primary, #6653e3);
  }
</style>
```

- [ ] **Step 4: 创建 `site/src/components/starlight/Footer.astro`**

```astro
---
// 覆盖 Starlight Footer，含版权与 GitHub 链接。
const year = new Date().getFullYear();
---
<footer class="footer">
  <div class="footer-inner">
    <div class="copyright">© {year} LLM-Gateway · Apache-2.0 开源</div>
    <div class="links">
      <a href="https://github.com/codingas/llm-gateway">GitHub</a>
      <a href="/docs/">文档</a>
      <a href="/standard-vs-enterprise/">版本对比</a>
    </div>
  </div>
</footer>

<style>
  .footer {
    border-top: 1px solid #e5e7eb;
    padding: 24px 0;
    margin-top: 48px;
  }
  .footer-inner {
    max-width: 1200px;
    margin: 0 auto;
    padding: 0 24px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    flex-wrap: wrap;
    gap: 12px;
  }
  .copyright {
    color: #6b7280;
    font-size: 0.875rem;
  }
  .links {
    display: flex;
    gap: 20px;
  }
  .links a {
    color: #6b7280;
    text-decoration: none;
    font-size: 0.875rem;
  }
  .links a:hover {
    color: var(--brand-primary, #6653e3);
  }
</style>
```

- [ ] **Step 5: 修改 `site/astro.config.ts` 启用组件覆盖**

将 Task 1 Step 9 中 `starlight({...})` 内的 `components: {}` 替换为：

```ts
      components: {
        Header: './src/components/starlight/Header.astro',
        Footer: './src/components/starlight/Footer.astro',
        SiteTitle: './src/components/starlight/SiteTitle.astro',
      },
```

- [ ] **Step 6: 验证主题覆盖生效**

Run:
```bash
cd site && pnpm dev
```
Expected: 访问 `http://localhost:4321/docs/`，顶部显示自定义 Header（品牌名 + 产品/文档/版本对比/联系我们导航），底部显示自定义 Footer（版权 + GitHub/文档/版本对比链接），SiteTitle 显示「LLM-Gateway」。Ctrl+C 停止。

- [ ] **Step 7: Commit**

```bash
git add site/src/components/starlight/ site/src/data/navigation.ts site/astro.config.ts
git commit -m "feat(site): 覆盖 Starlight Header/Footer/SiteTitle 主题组件"
```

---

## Task 5: 版本对比页

**Files:**
- Create: `site/src/data/editionDiff.ts`
- Create: `site/src/data/i18n/editionDiff.ts`
- Create: `site/src/pages/standard-vs-enterprise/index.astro`
- Create: `site/src/pages/en/standard-vs-enterprise/index.astro`
- Create: `site/src/layouts/BaseLayout.astro`

**目标：** `/standard-vs-enterprise/`（中文）+ `/en/standard-vs-enterprise/`（英文）双页，对比表覆盖 README 全部 ✅/🔒 功能项（API 网关/Provider 管理/路由/用户认证/密钥管理/Token 配额/安全风控/可观测性/高级能力/部署/支持），含迁移路径段。

- [ ] **Step 1: 创建 `site/src/data/editionDiff.ts`（版本对比表结构化数据）**

数据覆盖 README 第 41-118 行全部 ✅/🔒 项，以及部署/支持类别。

```ts
// 版本对比表结构化数据。standard/enterprise 为 true 表示该版本具备该能力。
export interface DiffItem {
  name: string;
  standard: boolean;
  enterprise: boolean;
}
export interface DiffCategory {
  category: string;
  items: DiffItem[];
}

export const editionDiff: DiffCategory[] = [
  {
    category: 'API 网关',
    items: [
      { name: 'OpenAI 兼容端点（/v1/chat/completions、/v1/completions）', standard: true, enterprise: true },
      { name: 'Anthropic 兼容端点（/v1/messages）', standard: true, enterprise: true },
      { name: 'SSE 流式转发（首 token ≤100ms）', standard: true, enterprise: true },
      { name: '协议转换（OpenAI ↔ Anthropic 互转）', standard: true, enterprise: true },
      { name: '图像生成端点（/v1/images/generations）', standard: true, enterprise: true },
      { name: '语音合成端点（/v1/audio/speech）', standard: true, enterprise: true },
      { name: '语音识别端点（/v1/audio/transcriptions）', standard: true, enterprise: true },
      { name: '内容审核端点（/v1/moderations）', standard: true, enterprise: true },
    ],
  },
  {
    category: 'Provider 管理',
    items: [
      { name: 'Provider CRUD', standard: true, enterprise: true },
      { name: '多 Key 管理（自动轮换、调度、故障切换）', standard: true, enterprise: true },
      { name: '默认 Key 设置', standard: true, enterprise: true },
      { name: 'Key 统计展示', standard: true, enterprise: true },
      { name: '负载均衡（优先级 + 权重）', standard: true, enterprise: true },
      { name: '渠道分组', standard: true, enterprise: true },
      { name: '渠道级故障转移', standard: true, enterprise: true },
      { name: '熔断超时（防雪崩）', standard: true, enterprise: true },
      { name: '代理配置（HTTP/S、Socket5）', standard: false, enterprise: true },
    ],
  },
  {
    category: '路由',
    items: [
      { name: '模型级智能降级', standard: true, enterprise: true },
      { name: '场景路由（CODE/CREATIVE/SUMMARY 等）', standard: true, enterprise: true },
      { name: '模型别名映射', standard: true, enterprise: true },
      { name: '可视化策略编排', standard: false, enterprise: true },
      { name: '自定义脚本扩展', standard: false, enterprise: true },
    ],
  },
  {
    category: '用户与认证',
    items: [
      { name: '用户 CRUD', standard: true, enterprise: true },
      { name: '用户名密码登录/登出', standard: true, enterprise: true },
      { name: 'OAuth 登录（GitHub、Gitee、QQ、企业微信）', standard: true, enterprise: true },
      { name: '企业 OAuth（飞书、钉钉、GitHub Enterprise）', standard: false, enterprise: true },
    ],
  },
  {
    category: '密钥管理',
    items: [
      { name: 'API Key CRUD', standard: true, enterprise: true },
      { name: '额度限制', standard: true, enterprise: true },
      { name: '模型白名单', standard: true, enterprise: true },
      { name: 'IP 限制', standard: true, enterprise: true },
      { name: '过期时间', standard: true, enterprise: true },
    ],
  },
  {
    category: 'Token 计量与配额',
    items: [
      { name: 'Token 计量（输入/输出分别统计）', standard: true, enterprise: true },
      { name: 'Token 限额（用户级/API Key 级）', standard: true, enterprise: true },
      { name: '用户×渠道限额', standard: false, enterprise: true },
      { name: '请求次数配额', standard: false, enterprise: true },
    ],
  },
  {
    category: '安全与风控',
    items: [
      { name: '认证中间件（Token 验证）', standard: true, enterprise: true },
      { name: 'IP 白/黑名单', standard: true, enterprise: true },
      { name: 'UA 过滤', standard: true, enterprise: true },
      { name: 'PII 脱敏（手机号、身份证、邮箱、银行卡等）', standard: true, enterprise: true },
      { name: '数据掩码策略', standard: true, enterprise: true },
      { name: '审计日志', standard: true, enterprise: true },
      { name: '密钥加密存储（AES-256）', standard: true, enterprise: true },
      { name: '国密算法（SM2/SM3/SM4）', standard: false, enterprise: true },
      { name: '完整审计链（WORM）', standard: false, enterprise: true },
    ],
  },
  {
    category: '可观测性',
    items: [
      { name: 'Trace ID（全链路追踪）', standard: true, enterprise: true },
      { name: '结构化日志（JSON 格式）', standard: true, enterprise: true },
      { name: '实时指标（延迟/QPS/Token/费用）', standard: true, enterprise: true },
      { name: 'Prometheus 导出', standard: true, enterprise: true },
      { name: 'Grafana 仪表盘', standard: false, enterprise: true },
      { name: 'Jaeger 追踪', standard: false, enterprise: true },
    ],
  },
  {
    category: '高级能力',
    items: [
      { name: '语义缓存（相似请求命中，降本 30%+）', standard: false, enterprise: true },
      { name: 'MCP 协议（Resources/Prompts/Tools）', standard: false, enterprise: true },
    ],
  },
  {
    category: '部署',
    items: [
      { name: '单机部署', standard: true, enterprise: true },
      { name: 'Kubernetes 分布式部署', standard: false, enterprise: true },
      { name: '高可用集群', standard: false, enterprise: true },
    ],
  },
  {
    category: '支持',
    items: [
      { name: '社区支持', standard: true, enterprise: true },
      { name: '商业 SLA 保障', standard: false, enterprise: true },
      { name: '专属技术支持', standard: false, enterprise: true },
    ],
  },
];
```

- [ ] **Step 2: 创建 `site/src/data/i18n/editionDiff.ts`（对比页中英文案）**

```ts
// 版本对比页中英文案字典。
export const editionDiffI18n = {
  zh: {
    title: '标准版 vs 企业版',
    intro: 'LLM-Gateway 提供 Apache-2.0 开源标准版与企业版。企业版是标准版的超集，新增高级安全、可观测性与运维能力。',
    standard: '标准版',
    enterprise: '企业版',
    feature: '功能',
    standardDesc: '开源免费，单机即可起步，覆盖全部核心网关能力。',
    enterpriseDesc: '标准版全部能力 + 高级安全、分布式部署、商业支持。',
    migrationTitle: '迁移路径',
    migrationBody: '标准版起步后可平滑升级企业版：将配置 `llm-gateway.edition` 从 `standard` 切换为 `enterprise`，数据完全兼容，无需迁移。企业版专属功能（语义缓存、MCP、国密、WORM 等）通过配置开关启用。',
    getStarted: '如何开始',
    getStartedBody: '从标准版开源仓库开始，业务增长后联系团队获取企业版授权与部署支持。',
    contactCta: '联系我们',
  },
  en: {
    title: 'Standard vs Enterprise',
    intro: 'LLM-Gateway ships as an Apache-2.0 open-source Standard edition and an Enterprise edition. Enterprise is a superset adding advanced security, observability and ops.',
    standard: 'Standard',
    enterprise: 'Enterprise',
    feature: 'Feature',
    standardDesc: 'Free and open-source, single-node to start, covering all core gateway capabilities.',
    enterpriseDesc: 'Everything in Standard plus advanced security, distributed deployment and commercial support.',
    migrationTitle: 'Migration Path',
    migrationBody: 'Start on Standard and upgrade smoothly: flip `llm-gateway.edition` from `standard` to `enterprise`. Data is fully compatible, no migration needed. Enterprise-only features (semantic cache, MCP, SM crypto, WORM) are toggled via config.',
    getStarted: 'Getting Started',
    getStartedBody: 'Begin with the open-source Standard repo; as you grow, contact us for Enterprise licensing and deployment support.',
    contactCta: 'Contact Us',
  },
} as const;
```

- [ ] **Step 3: 创建 `site/src/layouts/BaseLayout.astro`（营销页基础布局）**

营销页（首页/对比页/联系页）共用此布局，复用自定义 Header/Footer。

```astro
---
import Header from '../components/starlight/Header.astro';
import Footer from '../components/starlight/Footer.astro';
import '../styles/global.scss';
interface Props {
  title: string;
  description?: string;
}
const { title, description } = Astro.props;
---
<!doctype html>
<html lang={Astro.url.pathname.startsWith('/en/') ? 'en' : 'zh-CN'}>
  <head>
    <meta charset="utf-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1" />
    <title>{title}</title>
    {description && <meta name="description" content={description} />}
    <link rel="icon" type="image/svg+xml" href="/favicon.svg" />
  </head>
  <body>
    <Header />
    <main class="container">
      <slot />
    </main>
    <Footer />
  </body>
</html>
```

- [ ] **Step 4: 创建 `site/src/pages/standard-vs-enterprise/index.astro`（中文对比页）**

```astro
---
import BaseLayout from '../../layouts/BaseLayout.astro';
import { editionDiff } from '../../data/editionDiff.ts';
import { editionDiffI18n } from '../../data/i18n/editionDiff.ts';

const t = editionDiffI18n.zh;
---
<BaseLayout title={`${t.title} · LLM-Gateway`} description={t.intro}>
  <section class="edition-hero">
    <h1>{t.title}</h1>
    <p class="intro">{t.intro}</p>
  </section>

  <section class="edition-cards">
    <div class="card standard">
      <h2>{t.standard}</h2>
      <p>{t.standardDesc}</p>
    </div>
    <div class="card enterprise">
      <h2>{t.enterprise}</h2>
      <p>{t.enterpriseDesc}</p>
    </div>
  </section>

  <section class="edition-table">
    <table>
      <thead>
        <tr>
          <th>{t.feature}</th>
          <th>{t.standard}</th>
          <th>{t.enterprise}</th>
        </tr>
      </thead>
      <tbody>
        {editionDiff.map((cat) => (
          <>
            <tr class="category-row">
              <th colspan="3">{cat.category}</th>
            </tr>
            {cat.items.map((item) => (
              <tr>
                <td>{item.name}</td>
                <td>{item.standard ? '✅' : '—'}</td>
                <td>{item.enterprise ? '✅' : '—'}</td>
              </tr>
            ))}
          </>
        ))}
      </tbody>
    </table>
  </section>

  <section class="edition-migration">
    <h2>{t.migrationTitle}</h2>
    <p>{t.migrationBody}</p>
  </section>

  <section class="edition-start">
    <h2>{t.getStarted}</h2>
    <p>{t.getStartedBody}</p>
    <a href="/contact-us/" class="cta">{t.contactCta}</a>
  </section>
</BaseLayout>

<style>
  .edition-hero { padding: 48px 0 24px; }
  .edition-hero h1 { font-size: 2rem; }
  .edition-cards { display: grid; grid-template-columns: 1fr 1fr; gap: 24px; margin: 24px 0; }
  .card { padding: 24px; border: 1px solid #e5e7eb; border-radius: 12px; }
  .card.enterprise { border-color: var(--enterprise-badge, #16a34a); }
  .edition-table { overflow-x: auto; margin: 32px 0; }
  table { width: 100%; border-collapse: collapse; }
  th, td { padding: 10px 12px; text-align: left; border-bottom: 1px solid #e5e7eb; }
  .category-row th { background: #f9fafb; font-size: 0.95rem; }
  .cta { display: inline-block; margin-top: 12px; padding: 10px 20px; background: var(--brand-primary, #6653e3); color: #fff; border-radius: 8px; text-decoration: none; }
</style>
```

- [ ] **Step 5: 创建 `site/src/pages/en/standard-vs-enterprise/index.astro`（英文镜像）**

```astro
---
import BaseLayout from '../../../layouts/BaseLayout.astro';
import { editionDiff } from '../../../data/editionDiff.ts';
import { editionDiffI18n } from '../../../data/i18n/editionDiff.ts';

const t = editionDiffI18n.en;
---
<BaseLayout title={`${t.title} · LLM-Gateway`} description={t.intro}>
  <section class="edition-hero">
    <h1>{t.title}</h1>
    <p class="intro">{t.intro}</p>
  </section>

  <section class="edition-cards">
    <div class="card standard">
      <h2>{t.standard}</h2>
      <p>{t.standardDesc}</p>
    </div>
    <div class="card enterprise">
      <h2>{t.enterprise}</h2>
      <p>{t.enterpriseDesc}</p>
    </div>
  </section>

  <section class="edition-table">
    <table>
      <thead>
        <tr>
          <th>{t.feature}</th>
          <th>{t.standard}</th>
          <th>{t.enterprise}</th>
        </tr>
      </thead>
      <tbody>
        {editionDiff.map((cat) => (
          <>
            <tr class="category-row">
              <th colspan="3">{cat.category}</th>
            </tr>
            {cat.items.map((item) => (
              <tr>
                <td>{item.name}</td>
                <td>{item.standard ? '✅' : '—'}</td>
                <td>{item.enterprise ? '✅' : '—'}</td>
              </tr>
            ))}
          </>
        ))}
      </tbody>
    </table>
  </section>

  <section class="edition-migration">
    <h2>{t.migrationTitle}</h2>
    <p>{t.migrationBody}</p>
  </section>

  <section class="edition-start">
    <h2>{t.getStarted}</h2>
    <p>{t.getStartedBody}</p>
    <a href="/en/contact-us/" class="cta">{t.contactCta}</a>
  </section>
</BaseLayout>

<style>
  .edition-hero { padding: 48px 0 24px; }
  .edition-hero h1 { font-size: 2rem; }
  .edition-cards { display: grid; grid-template-columns: 1fr 1fr; gap: 24px; margin: 24px 0; }
  .card { padding: 24px; border: 1px solid #e5e7eb; border-radius: 12px; }
  .card.enterprise { border-color: var(--enterprise-badge, #16a34a); }
  .edition-table { overflow-x: auto; margin: 32px 0; }
  table { width: 100%; border-collapse: collapse; }
  th, td { padding: 10px 12px; text-align: left; border-bottom: 1px solid #e5e7eb; }
  .category-row th { background: #f9fafb; font-size: 0.95rem; }
  .cta { display: inline-block; margin-top: 12px; padding: 10px 20px; background: var(--brand-primary, #6653e3); color: #fff; border-radius: 8px; text-decoration: none; }
</style>
```

- [ ] **Step 6: 创建联系页占位 `site/src/pages/contact-us.astro`**

```astro
---
import BaseLayout from '../layouts/BaseLayout.astro';
---
<BaseLayout title="联系我们 · LLM-Gateway" description="联系 LLM-Gateway 团队获取企业版授权与部署支持">
  <section style="padding: 64px 0;">
    <h1>联系我们</h1>
    <p>如需企业版授权、部署支持或商务合作，请通过以下方式联系我们：</p>
    <ul>
      <li>GitHub Issues：<a href="https://github.com/codingas/llm-gateway/issues">提交 Issue</a></li>
      <li>邮件：business@llm-gateway.dev（占位，待确定）</li>
    </ul>
  </section>
</BaseLayout>
```

- [ ] **Step 7: 验证对比页中英双页**

Run:
```bash
cd site && pnpm dev
```
Expected:
- `http://localhost:4321/standard-vs-enterprise/` 显示中文对比页，表格覆盖 11 个类别，企业版专属项标准版列显示「—」、企业版列显示「✅」，含迁移路径段。
- `http://localhost:4321/en/standard-vs-enterprise/` 显示英文对比页，文案为英文，表格数据一致。

Ctrl+C 停止。

- [ ] **Step 8: Commit**

```bash
git add site/src/data/editionDiff.ts site/src/data/i18n/editionDiff.ts site/src/pages/standard-vs-enterprise/ site/src/pages/en/standard-vs-enterprise/ site/src/pages/contact-us.astro site/src/layouts/BaseLayout.astro
git commit -m "feat(site): 版本对比页中英双语（覆盖 README 全部功能项+迁移路径）"
```

---

## Task 6: 首页

**Files:**
- Create: `site/src/data/i18n/home.ts`
- Create: `site/src/data/homeFeatures.ts`
- Create: `site/src/data/homeProducts.ts`
- Create: `site/src/data/homeEcosystem.ts`
- Create: `site/src/components/Hero.astro`
- Create: `site/src/components/ProductCard.astro`
- Create: `site/src/components/FeatureCard.astro`
- Create: `site/src/components/EcosystemCard.astro`
- Create: `site/src/components/Carousel.astro`
- Create: `site/src/pages/index.astro`
- Create: `site/src/pages/en/index.astro`

**目标：** 对标 thingsboard.io 的首页区块顺序：Hero -> 信任背书 -> 四差异化 -> 能力叙事 -> 三产品卡 -> 生态组件 -> 控制台轮播（P0 占位图）-> 8 能力域功能网格 -> 底部 CTA。中英双语。

- [ ] **Step 1: 创建 `site/src/data/i18n/home.ts`（首页中英文案字典）**

```ts
// 首页全部文案，中英双语，组件按 locale 取值。
export const home = {
  zh: {
    hero: {
      title: '企业级大模型网关 · 更合规、更安全、更智能、更易用',
      subtitle: 'OpenAI / Anthropic 双标准 API，统一接入 50+ 主流大模型，智能路由、语义缓存、全链路可观测。',
      cta: '快速开始',
      secondaryCta: '版本对比',
    },
    trust: { title: '已被多家团队信赖' },
    differentiators: [
      { title: '更易用', desc: '开箱即用（预置模板）+ 零学习成本（OpenAI 兼容）+ 运维友好（智能诊断）' },
      { title: '更合规', desc: '国密算法（SM2/SM3/SM4）+ 完整审计链（WORM）+ 等保 2.0 三级合规' },
      { title: '更安全', desc: 'Prompt 注入防护 + PII 智能脱敏 + 内容安全审核 + 零信任架构' },
      { title: '更智能', desc: '智能降级（业务连续性）+ 语义缓存（降本 30%+）+ 场景路由' },
    ],
    narrative: {
      title: '能力叙事',
      dualProtocol: '双协议：同时支持 OpenAI 与 Anthropic API 标准，协议互转零成本。',
      routing: '智能路由与降级：额度不足或模型不可用时自动切换，保障业务连续性。',
      security: '安全合规：认证、限流、脱敏、审计四层检查，密钥加密存储。',
    },
    products: {
      title: '三产品形态',
      items: [
        { name: '标准版', desc: 'Apache-2.0 开源，单机即可起步', href: '/docs/' },
        { name: '托管云', desc: '免运维的云托管服务（即将推出）', href: '/contact-us/' },
        { name: '企业版', desc: '分布式部署 + 高级安全 + 商业支持', href: '/standard-vs-enterprise/' },
      ],
    },
    ecosystem: { title: '生态组件' },
    console: { title: '控制台预览', placeholder: '控制台截图（P0 占位，待补充真实截图）' },
    features: { title: '8 大能力域' },
    cta: { title: '开始使用 LLM-Gateway', button: '查看文档' },
  },
  en: {
    hero: {
      title: 'Enterprise LLM Gateway · Compliant, Secure, Intelligent, Easy',
      subtitle: 'OpenAI / Anthropic dual-standard APIs, unified access to 50+ models, smart routing, semantic cache, full observability.',
      cta: 'Get Started',
      secondaryCta: 'Compare Editions',
    },
    trust: { title: 'Trusted by teams' },
    differentiators: [
      { title: 'Easier', desc: 'Out-of-the-box templates + zero learning curve (OpenAI-compatible) + ops-friendly diagnostics' },
      { title: 'More Compliant', desc: 'SM2/SM3/SM4 crypto + WORM audit chain + MLPS 2.0 Level 3' },
      { title: 'More Secure', desc: 'Prompt injection defense + PII masking + content moderation + zero-trust' },
      { title: 'Smarter', desc: 'Smart failover + semantic cache (30%+ savings) + scenario routing' },
    ],
    narrative: {
      title: 'Capabilities',
      dualProtocol: 'Dual protocol: OpenAI and Anthropic API standards, zero-cost protocol conversion.',
      routing: 'Smart routing & failover: auto-switch on quota exhaustion or model unavailability.',
      security: 'Security & compliance: auth, rate-limit, masking, audit — four-layer checks, encrypted keys.',
    },
    products: {
      title: 'Three Editions',
      items: [
        { name: 'Standard', desc: 'Apache-2.0 open-source, single-node to start', href: '/en/docs/' },
        { name: 'Managed Cloud', desc: 'Managed cloud service (coming soon)', href: '/en/contact-us/' },
        { name: 'Enterprise', desc: 'Distributed deployment + advanced security + support', href: '/en/standard-vs-enterprise/' },
      ],
    },
    ecosystem: { title: 'Ecosystem' },
    console: { title: 'Console Preview', placeholder: 'Console screenshots (placeholder, real shots pending)' },
    features: { title: '8 Capability Domains' },
    cta: { title: 'Start with LLM-Gateway', button: 'View Docs' },
  },
} as const;
```

- [ ] **Step 2: 创建 `site/src/data/homeFeatures.ts`（8 能力域功能卡数据）**

```ts
// 首页 8 能力域功能网格卡片数据（含跳转文档 slug）。
export interface FeatureCardData {
  title: string;
  desc: string;
  href: string;
  enterprise?: boolean;
}

export const homeFeatures: FeatureCardData[] = [
  { title: 'API 网关', desc: 'OpenAI / Anthropic 双标准端点，SSE 流式，协议互转', href: '/docs/api-gateway/' },
  { title: 'Provider 管理', desc: '多 Key 轮换、负载均衡、熔断故障转移', href: '/docs/provider-management/' },
  { title: '路由', desc: '模型级智能降级、场景路由、别名映射', href: '/docs/features/routing/' },
  { title: '用户与认证', desc: '用户 CRUD、OAuth 登录、企业 OAuth', href: '/docs/auth/' },
  { title: '密钥管理', desc: 'API Key CRUD、额度限制、模型白名单、IP 限制', href: '/docs/apikey-management/' },
  { title: 'Token 计量与配额', desc: '输入/输出分别统计、二级预算控制', href: '/docs/token-quota/' },
  { title: '安全与风控', desc: 'PII 脱敏、内容审核、密钥加密、国密', href: '/docs/security/' },
  { title: '可观测性', desc: 'Trace ID 全链路、结构化日志、Prometheus', href: '/docs/observability/' },
];
```

- [ ] **Step 3: 创建 `site/src/data/homeProducts.ts` 与 `homeEcosystem.ts`**

`site/src/data/homeProducts.ts`：

```ts
// 三产品卡数据（中英 label 由 i18n/home.ts 提供，此处仅结构）。
export interface ProductCardData {
  key: 'standard' | 'cloud' | 'enterprise';
}

export const homeProducts: ProductCardData[] = [
  { key: 'standard' },
  { key: 'cloud' },
  { key: 'enterprise' },
];
```

`site/src/data/homeEcosystem.ts`：

```ts
// 生态组件数据。
export interface EcosystemItem {
  name: string;
  desc: string;
}

export const homeEcosystem: EcosystemItem[] = [
  { name: 'PostgreSQL', desc: '主数据库 + pgvector 向量存储' },
  { name: 'Redis', desc: '分布式缓存与限流' },
  { name: 'OpenTelemetry', desc: '全链路追踪标准' },
  { name: 'Prometheus', desc: '指标采集与导出' },
  { name: 'Spring Boot 3.5', desc: '后端框架（Java 21）' },
  { name: 'Cloudflare Pages', desc: '官网与文档部署' },
];
```

- [ ] **Step 4: 创建 `site/src/components/Hero.astro`**

```astro
---
interface Props {
  title: string;
  subtitle: string;
  cta: string;
  secondaryCta: string;
}
const { title, subtitle, cta, secondaryCta } = Astro.props;
---
<section class="hero">
  <h1>{title}</h1>
  <p class="subtitle">{subtitle}</p>
  <div class="cta-group">
    <a href="/docs/" class="btn primary">{cta}</a>
    <a href="/standard-vs-enterprise/" class="btn secondary">{secondaryCta}</a>
  </div>
</section>

<style>
  .hero { text-align: center; padding: 80px 0 48px; }
  .hero h1 { font-size: 2.5rem; line-height: 1.3; max-width: 900px; margin: 0 auto 16px; }
  .subtitle { font-size: 1.15rem; color: #6b7280; max-width: 700px; margin: 0 auto 32px; }
  .cta-group { display: flex; gap: 16px; justify-content: center; }
  .btn { padding: 12px 28px; border-radius: 8px; text-decoration: none; font-weight: 600; }
  .btn.primary { background: var(--brand-primary, #6653e3); color: #fff; }
  .btn.secondary { border: 1px solid var(--brand-primary, #6653e3); color: var(--brand-primary, #6653e3); }
</style>
```

- [ ] **Step 5: 创建 `site/src/components/ProductCard.astro`、`FeatureCard.astro`、`EcosystemCard.astro`、`Carousel.astro`**

`ProductCard.astro`：

```astro
---
interface Props { name: string; desc: string; href: string; }
const { name, desc, href } = Astro.props;
---
<a href={href} class="product-card">
  <h3>{name}</h3>
  <p>{desc}</p>
</a>

<style>
  .product-card { display: block; padding: 24px; border: 1px solid #e5e7eb; border-radius: 12px; text-decoration: none; color: inherit; transition: box-shadow .2s; }
  .product-card:hover { box-shadow: 0 4px 12px rgba(0,0,0,.08); }
  .product-card h3 { margin: 0 0 8px; font-size: 1.2rem; }
  .product-card p { margin: 0; color: #6b7280; font-size: 0.95rem; }
</style>
```

`FeatureCard.astro`：

```astro
---
interface Props { title: string; desc: string; href: string; }
const { title, desc, href } = Astro.props;
---
<a href={href} class="feature-card">
  <h4>{title}</h4>
  <p>{desc}</p>
</a>

<style>
  .feature-card { display: block; padding: 20px; border: 1px solid #e5e7eb; border-radius: 8px; text-decoration: none; color: inherit; }
  .feature-card:hover { border-color: var(--brand-primary, #6653e3); }
  .feature-card h4 { margin: 0 0 6px; font-size: 1rem; }
  .feature-card p { margin: 0; color: #6b7280; font-size: 0.875rem; }
</style>
```

`EcosystemCard.astro`：

```astro
---
interface Props { name: string; desc: string; }
const { name, desc } = Astro.props;
---
<div class="ecosystem-card">
  <strong>{name}</strong>
  <span>{desc}</span>
</div>

<style>
  .ecosystem-card { padding: 16px; border: 1px solid #e5e7eb; border-radius: 8px; display: flex; flex-direction: column; gap: 4px; }
  .ecosystem-card strong { font-size: 0.95rem; }
  .ecosystem-card span { color: #6b7280; font-size: 0.8rem; }
</style>
```

`Carousel.astro`（P0 占位）：

```astro
---
interface Props { title: string; placeholder: string; }
const { title, placeholder } = Astro.props;
---
<section class="carousel">
  <h2>{title}</h2>
  <div class="placeholder">{placeholder}</div>
</section>

<style>
  .carousel { padding: 48px 0; text-align: center; }
  .placeholder { margin: 24px auto; max-width: 800px; height: 300px; display: flex; align-items: center; justify-content: center; background: #f3f4f6; border-radius: 12px; color: #9ca3af; }
</style>
```

- [ ] **Step 6: 创建 `site/src/pages/index.astro`（中文首页）**

```astro
---
import BaseLayout from '../layouts/BaseLayout.astro';
import Hero from '../components/Hero.astro';
import ProductCard from '../components/ProductCard.astro';
import FeatureCard from '../components/FeatureCard.astro';
import EcosystemCard from '../components/EcosystemCard.astro';
import Carousel from '../components/Carousel.astro';
import { home } from '../data/i18n/home.ts';
import { homeFeatures } from '../data/homeFeatures.ts';
import { homeProducts } from '../data/homeProducts.ts';
import { homeEcosystem } from '../data/homeEcosystem.ts';

const t = home.zh;
const products = homeProducts.map((p) => ({
  ...home.zh.products.items.find((i, idx) => idx === homeProducts.indexOf(p)),
  ...p,
}));
---
<BaseLayout title={`${t.hero.title} · LLM-Gateway`} description={t.hero.subtitle}>
  <Hero title={t.hero.title} subtitle={t.hero.subtitle} cta={t.hero.cta} secondaryCta={t.hero.secondaryCta} />

  <section class="trust"><h3>{t.trust.title}</h3></section>

  <section class="differentiators">
    {t.differentiators.map((d) => (
      <div class="diff-item"><h4>{d.title}</h4><p>{d.desc}</p></div>
    ))}
  </section>

  <section class="narrative">
    <h2>{t.narrative.title}</h2>
    <p>{t.narrative.dualProtocol}</p>
    <p>{t.narrative.routing}</p>
    <p>{t.narrative.security}</p>
  </section>

  <section class="products">
    <h2>{t.products.title}</h2>
    <div class="grid-3">
      {t.products.items.map((p) => <ProductCard name={p.name} desc={p.desc} href={p.href} />)}
    </div>
  </section>

  <section class="ecosystem">
    <h2>{t.ecosystem.title}</h2>
    <div class="grid-3">
      {homeEcosystem.map((e) => <EcosystemCard name={e.name} desc={e.desc} />)}
    </div>
  </section>

  <Carousel title={t.console.title} placeholder={t.console.placeholder} />

  <section class="features">
    <h2>{t.features.title}</h2>
    <div class="grid-4">
      {homeFeatures.map((f) => <FeatureCard title={f.title} desc={f.desc} href={f.href} />)}
    </div>
  </section>

  <section class="bottom-cta">
    <h2>{t.cta.title}</h2>
    <a href="/docs/" class="btn primary">{t.cta.button}</a>
  </section>
</BaseLayout>

<style>
  .trust { text-align: center; padding: 24px 0; color: #9ca3af; }
  .differentiators { display: grid; grid-template-columns: repeat(4, 1fr); gap: 20px; padding: 32px 0; }
  .diff-item h4 { margin: 0 0 8px; color: var(--brand-primary, #6653e3); }
  .diff-item p { margin: 0; font-size: 0.875rem; color: #6b7280; }
  .narrative, .products, .ecosystem, .features { padding: 32px 0; }
  .narrative h2, .products h2, .ecosystem h2, .features h2 { font-size: 1.5rem; margin-bottom: 24px; }
  .grid-3 { display: grid; grid-template-columns: repeat(3, 1fr); gap: 20px; }
  .grid-4 { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
  .bottom-cta { text-align: center; padding: 64px 0; }
  .bottom-cta .btn { display: inline-block; padding: 12px 28px; border-radius: 8px; background: var(--brand-primary, #6653e3); color: #fff; text-decoration: none; font-weight: 600; }
  @media (max-width: 768px) { .differentiators, .grid-3, .grid-4 { grid-template-columns: 1fr; } }
</style>
```

- [ ] **Step 7: 创建 `site/src/pages/en/index.astro`（英文首页镜像）**

结构与中文首页一致，`const t = home.en;`，导航 href 已在 `home.en` 字典中带 `/en/` 前缀。完整内容：

```astro
---
import BaseLayout from '../../layouts/BaseLayout.astro';
import Hero from '../../components/Hero.astro';
import ProductCard from '../../components/ProductCard.astro';
import FeatureCard from '../../components/FeatureCard.astro';
import EcosystemCard from '../../components/EcosystemCard.astro';
import Carousel from '../../components/Carousel.astro';
import { home } from '../../data/i18n/home.ts';
import { homeFeatures } from '../../data/homeFeatures.ts';
import { homeEcosystem } from '../../data/homeEcosystem.ts';

const t = home.en;
---
<BaseLayout title={`${t.hero.title} · LLM-Gateway`} description={t.hero.subtitle}>
  <Hero title={t.hero.title} subtitle={t.hero.subtitle} cta={t.hero.cta} secondaryCta={t.hero.secondaryCta} />

  <section class="trust"><h3>{t.trust.title}</h3></section>

  <section class="differentiators">
    {t.differentiators.map((d) => (
      <div class="diff-item"><h4>{d.title}</h4><p>{d.desc}</p></div>
    ))}
  </section>

  <section class="narrative">
    <h2>{t.narrative.title}</h2>
    <p>{t.narrative.dualProtocol}</p>
    <p>{t.narrative.routing}</p>
    <p>{t.narrative.security}</p>
  </section>

  <section class="products">
    <h2>{t.products.title}</h2>
    <div class="grid-3">
      {t.products.items.map((p) => <ProductCard name={p.name} desc={p.desc} href={p.href} />)}
    </div>
  </section>

  <section class="ecosystem">
    <h2>{t.ecosystem.title}</h2>
    <div class="grid-3">
      {homeEcosystem.map((e) => <EcosystemCard name={e.name} desc={e.desc} />)}
    </div>
  </section>

  <Carousel title={t.console.title} placeholder={t.console.placeholder} />

  <section class="features">
    <h2>{t.features.title}</h2>
    <div class="grid-4">
      {homeFeatures.map((f) => <FeatureCard title={f.title} desc={f.desc} href={f.href} />)}
    </div>
  </section>

  <section class="bottom-cta">
    <h2>{t.cta.title}</h2>
    <a href="/en/docs/" class="btn primary">{t.cta.button}</a>
  </section>
</BaseLayout>

<style>
  .trust { text-align: center; padding: 24px 0; color: #9ca3af; }
  .differentiators { display: grid; grid-template-columns: repeat(4, 1fr); gap: 20px; padding: 32px 0; }
  .diff-item h4 { margin: 0 0 8px; color: var(--brand-primary, #6653e3); }
  .diff-item p { margin: 0; font-size: 0.875rem; color: #6b7280; }
  .narrative, .products, .ecosystem, .features { padding: 32px 0; }
  .narrative h2, .products h2, .ecosystem h2, .features h2 { font-size: 1.5rem; margin-bottom: 24px; }
  .grid-3 { display: grid; grid-template-columns: repeat(3, 1fr); gap: 20px; }
  .grid-4 { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
  .bottom-cta { text-align: center; padding: 64px 0; }
  .bottom-cta .btn { display: inline-block; padding: 12px 28px; border-radius: 8px; background: var(--brand-primary, #6653e3); color: #fff; text-decoration: none; font-weight: 600; }
  @media (max-width: 768px) { .differentiators, .grid-3, .grid-4 { grid-template-columns: 1fr; } }
</style>
```

- [ ] **Step 8: 验证首页中英双语渲染**

Run:
```bash
cd site && pnpm dev
```
Expected:
- `http://localhost:4321/` 显示中文首页，依次呈现 Hero、信任背书、四差异化、能力叙事、三产品卡、生态组件、控制台占位、8 能力域功能网格、底部 CTA。
- `http://localhost:4321/en/` 显示英文首页，文案为英文，区块结构一致。
- 功能网格卡片链接指向 `/docs/*`，可跳转。

Ctrl+C 停止。

- [ ] **Step 9: Commit**

```bash
git add site/src/data/ site/src/components/ site/src/pages/index.astro site/src/pages/en/index.astro
git commit -m "feat(site): 首页中英双语（8 区块结构+数据化文案）"
```

---

## Task 7: 工具链与部署

**Files:**
- Create: `site/scripts/linkcheck.mjs`
- Create: `site/scripts/slugcheck.mjs`
- Create: `.github/workflows/deploy-site.yml`（根仓库工作流目录）
- Modify: `site/package.json`（lint 脚本已在 Task 1 定义，此处无需改）

**目标：** linkcheck（无断链）+ slugcheck（英文 kebab-case）lint 脚本；GitHub Actions -> Cloudflare Pages 部署流水线（PR 预览 + master 生产）。lunaria P0 不引入。

- [ ] **Step 1: 创建 `site/scripts/linkcheck.mjs`**

构建后扫描 `dist/` 产物中的 `<a href>`，校验内部链接是否指向存在的文件。断链则退出码 1。

```js
// 链接检查：扫描 dist/ 下 HTML 的内部链接，校验目标文件存在。
import { readdir, readFile, stat } from 'node:fs/promises';
import { join, dirname, resolve, extname } from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = dirname(fileURLToPath(import.meta.url));
const distDir = resolve(__dirname, '../dist');

const broken = [];

async function walk(dir) {
  const entries = await readdir(dir, { withFileTypes: true });
  const files = [];
  for (const e of entries) {
    const full = join(dir, e.name);
    if (e.isDirectory()) files.push(...await walk(full));
    else if (e.name.endsWith('.html')) files.push(full);
  }
  return files;
}

async function check() {
  if (!await stat(distDir).catch(() => null)) {
    console.error('linkcheck: dist/ 不存在，请先运行 pnpm build');
    process.exit(1);
  }
  const htmlFiles = await walk(distDir);
  const hrefRe = /href="([^"]+)"/g;
  for (const file of htmlFiles) {
    const html = await readFile(file, 'utf8');
    let m;
    while ((m = hrefRe.exec(html)) !== null) {
      let href = m[1];
      // 跳过外部链接、锚点、mailto、tel。
      if (/^(https?:|mailto:|tel:|#)/.test(href)) continue;
      // 去掉查询参数与锚点。
      href = href.split('#')[0].split('?')[0];
      if (!href) continue;
      // trailingSlash always：以 / 结尾则解析为 /index.html。
      const target = href.endsWith('/')
        ? join(distDir, href, 'index.html')
        : join(distDir, href);
      const exists = await stat(target).catch(() => null)
        || await stat(join(dirname(target), 'index.html')).catch(() => null);
      if (!exists) {
        broken.push(`${file.replace(distDir, '')} -> ${href}`);
      }
    }
  }
  if (broken.length) {
    console.error(`linkcheck: 发现 ${broken.length} 个断链：`);
    broken.forEach((b) => console.error('  ' + b));
    process.exit(1);
  }
  console.log('linkcheck: 无断链 ✅');
}

check();
```

- [ ] **Step 2: 创建 `site/scripts/slugcheck.mjs`**

校验 `src/content/docs/` 下所有 `.mdx` 文件名与目录名为英文 kebab-case（`^[a-z0-9-]+$`），不允许中文或大写。

```js
// slug 检查：src/content/docs/ 下文件名与目录名必须为英文 kebab-case。
import { readdir, stat } from 'node:fs/promises';
import { join, dirname, extname } from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = dirname(fileURLToPath(import.meta.url));
const docsDir = resolve(__dirname, '../src/content/docs');
const valid = /^[a-z0-9-]+$/;
const invalid = [];

async function walk(dir) {
  const entries = await readdir(dir, { withFileTypes: true });
  for (const e of entries) {
    const name = e.name;
    const stem = extname(name) ? name.replace(/\.(mdx|md)$/, '') : name;
    if (!valid.test(stem)) {
      invalid.push(join(dir.replace(docsDir, ''), name));
    }
    if (e.isDirectory()) await walk(join(dir, name));
  }
}

async function check() {
  await walk(docsDir);
  if (invalid.length) {
    console.error(`slugcheck: 发现 ${invalid.length} 个非法 slug（需英文 kebab-case）：`);
    invalid.forEach((s) => console.error('  ' + s));
    process.exit(1);
  }
  console.log('slugcheck: 所有 slug 合规 ✅');
}

check();
```

- [ ] **Step 3: 验证 lint 脚本可运行**

Run:
```bash
cd site && pnpm build && pnpm lint:slugcheck && pnpm lint:linkcheck
```
Expected:
- `pnpm build` 生成 `dist/`，无错误。
- `slugcheck` 输出「所有 slug 合规 ✅」。
- `linkcheck` 输出「无断链 ✅」。若有断链，修复对应 sidebar 条目或文档 href 后重跑。

- [ ] **Step 4: 创建 `.github/workflows/deploy-site.yml`（根仓库工作流）**

```yaml
name: Deploy Site

on:
  push:
    branches: [master]
    paths:
      - 'site/**'
      - '.github/workflows/deploy-site.yml'
  pull_request:
    paths:
      - 'site/**'
      - '.github/workflows/deploy-site.yml'

jobs:
  build-deploy:
    runs-on: ubuntu-latest
    defaults:
      run:
        working-directory: site
    steps:
      - uses: actions/checkout@v4

      - name: Setup Node
        uses: actions/setup-node@v4
        with:
          node-version-file: 'site/.nvmrc'

      - name: Setup pnpm
        uses: pnpm/action-setup@v4
        with:
          version: 9

      - name: Install dependencies
        run: pnpm install --frozen-lockfile

      - name: Build
        run: pnpm build
        env:
          PUBLIC_SITE_URL: https://llm-gateway.dev

      - name: Slug check
        run: pnpm lint:slugcheck

      - name: Link check
        run: pnpm lint:linkcheck

      - name: Deploy to Cloudflare Pages
        uses: cloudflare/wrangler-action@v3
        with:
          apiToken: ${{ secrets.CLOUDFLARE_API_TOKEN }}
          accountId: ${{ secrets.CLOUDFLARE_ACCOUNT_ID }}
          command: pages deploy dist --project-name=llm-gateway
```

> 注：PR 触发时 Cloudflare Pages 会自动生成预览部署（preview），master 触发为生产部署（production）。`CLOUDFLARE_API_TOKEN` 与 `CLOUDFLARE_ACCOUNT_ID` 需在 GitHub 仓库 Secrets 配置。wrangler-action 会以 `site` 为工作目录（defaults.run 已设），`dist` 路径相对于 `site/`。

- [ ] **Step 5: Commit**

```bash
git add site/scripts/ .github/workflows/deploy-site.yml
git commit -m "feat(site): linkcheck/slugcheck lint 脚本+GitHub Actions→Cloudflare Pages 部署"
```

---

## Task 8: 文档站英文版（deferred，P0 不做）

**状态：** DEFERRED — 本任务在 P0 不执行，仅作为后续独立任务的占位记录。

**Files（未来）:**
- Modify: `site/astro.config.ts`（`locales` 增加 `en`）
- Create: `site/lunaria.config.ts`
- Create: 英文文档骨架 `src/content/docs/en/`（或 Starlight en locale 目录）

**说明：**
- Design Doc §3.2 已确认：文档站 P0 仅 root locale（中文），不配 en，不引入 lunaria，避免未翻译 fallback 噪音。
- 营销页英文文案已在 Task 5（对比页 `/en/` 镜像）与 Task 6（首页 `/en/` 镜像）覆盖。
- 文档站 en locale + lunaria 翻译状态追踪作为后续独立任务，不在本 P0 计划内执行。
- tasks.md 原 8.1 已由 Design Doc §9 Spec Patch 调整为「文档站 en locale 作为后续独立任务，P0 营销页英文文案已在 5.3/6.1 覆盖」。

**无步骤、无 commit。** 本任务仅记录 deferred 状态。

---

## Task 9: 验收

**Files:** 无新建文件，仅执行验证。

**目标：** 按 Design Doc §7 测试策略全量验收，确认所有 P0 交付物达标。

- [ ] **Step 1: 构建验收**

Run:
```bash
cd site && pnpm build
```
Expected: 构建成功，无错误，`dist/` 产物生成，含 `sitemap.xml`。

- [ ] **Step 2: 链接验收**

Run:
```bash
cd site && pnpm lint:linkcheck
```
Expected: 输出「linkcheck: 无断链 ✅」，退出码 0。

- [ ] **Step 3: slug 验收**

Run:
```bash
cd site && pnpm lint:slugcheck
```
Expected: 输出「slugcheck: 所有 slug 合规 ✅」，退出码 0。

- [ ] **Step 4: 本地开发服务验收**

Run:
```bash
cd site && pnpm dev
```
Expected: 服务启动，逐项手动检查：
- 首页 `http://localhost:4321/` 中文，区块完整（Hero/信任/四差异化/能力叙事/三产品/生态/控制台占位/8 功能网格/CTA）。
- 英文首页 `http://localhost:4321/en/` 文案为英文，区块结构一致。
- 对比页 `http://localhost:4321/standard-vs-enterprise/` 中文，表格覆盖 11 类别。
- 英文对比页 `http://localhost:4321/en/standard-vs-enterprise/` 文案为英文。
- 文档站 `http://localhost:4321/docs/` 侧边栏 12 分组，8 能力域齐全，「语义缓存」「MCP 协议」附绿色「企业版」badge。
- 中英切换：首页与对比页 `/` <-> `/en/` 路径切换，文案随之切换。

Ctrl+C 停止。

- [ ] **Step 5: 企业版 badge 验收**

在 Step 4 的开发服务中，访问 `http://localhost:4321/docs/`，确认：
- 侧边栏「高级能力」分组下「语义缓存」「MCP 协议」条目旁显示绿色「企业版」badge。
- 8 能力域分组全部可见（API 网关 / Provider 管理 / 路由 / 用户与认证 / 密钥管理 / Token 计量与配额 / 安全与风控 / 可观测性）。
- 标准版条目无 badge。

- [ ] **Step 6: 对比表覆盖度验收**

打开 `http://localhost:4321/standard-vs-enterprise/`，对照 README 第 41-118 行功能矩阵，确认对比表覆盖全部类别与 ✅/🔒 项：
- API 网关（8 项）、Provider 管理（8 项，含代理配置企业版）、路由（5 项，含可视化编排/脚本扩展企业版）、用户与认证（4 项，含企业 OAuth 企业版）、密钥管理（5 项）、Token 计量与配额（4 项，含用户×渠道/请求次数企业版）、安全与风控（9 项，含国密/WORM 企业版）、可观测性（6 项，含 Grafana/Jaeger 企业版）、高级能力（语义缓存/MCP 企业版）、部署、支持。

- [ ] **Step 7: Cloudflare Pages 预览部署验收**

创建一个 PR（包含 `site/**` 变更），确认 GitHub Actions 触发 `Deploy Site` 工作流：
- `pnpm build` 成功。
- `lint:slugcheck` 通过。
- `lint:linkcheck` 通过。
- Cloudflare Pages 预览部署成功（在 PR 检查中可见预览 URL）。

> 若 `CLOUDFLARE_API_TOKEN` 等 Secrets 未配置，此步可标记为阻塞项，记录待运维配置后复验。

- [ ] **Step 8: 更新 tasks.md 勾选**

打开 `openspec/changes/official-website/tasks.md`，将 Task 1-7、Task 9 的复选框勾选为 `[x]`（Task 8 保持 `[ ]` 并标注 deferred）。注意 tasks.md 原文有几处与 Design Doc 锁定决策不一致（1.2 locales、3.2 isEnterprise、7.1 lunaria），按 Design Doc §9 Spec Patch 的修正理解执行：1.2 按 root locale 配置、3.2 按 badge 全渲染、7.1 lunaria 不引入。

- [ ] **Step 9: Commit 验收结果**

```bash
git add openspec/changes/official-website/tasks.md
git commit -m "chore(official-website): tasks.md 勾选完成（Task 8 deferred）"
```

---

## Self-Review（spec 覆盖度自检）

逐项核对 `openspec/changes/official-website/specs/website/spec.md` 的 7 个 Requirement：

| Requirement | 覆盖任务 | 说明 |
|---|---|---|
| 站点项目结构 | Task 1 | `site/` 独立 package.json + pnpm，`pnpm dev`/`pnpm build` 验证（Step 11/Step 1） |
| 中英双语国际化 | Task 5、Task 6、Task 8(deferred) | 营销页双语（`src/data/i18n/` + `src/pages/en/`）；文档站 P0 仅 root locale；文档 en deferred |
| 文档站侧边栏组织 | Task 3 | `astro.sidebar.ts` 8 能力域分组 + 企业版 badge，全部渲染 |
| 公开文档迁移 | Task 2 | 11 篇迁移 + 内部调研文档复核（Step 6 确认不公开） |
| 版本对比页 | Task 5 | `editionDiff.ts` 覆盖 README 全部 ✅/🔒 项 + 迁移路径段 + 中英双页 |
| 首页内容架构 | Task 6 | 8 区块结构（Hero/信任/四差异化/能力叙事/三产品/生态/控制台/功能网格/CTA）+ 数据化文案 |
| 部署流水线 | Task 7 | GitHub Actions -> Cloudflare Pages（PR 预览 + master 生产）+ linkcheck |

**占位符扫描：** 已检查，无 TBD/TODO/"implement later" 等占位。所有代码步骤含完整代码块。

**类型一致性：** `editionDiff`（`DiffCategory[]`）在 Task 5 Step 1 定义，Step 4/5 消费一致；`home`（i18n 字典）在 Task 6 Step 1 定义，Step 6/7 消费一致；`homeFeatures` 在 Step 2 定义、Step 6/7 消费一致；`SidebarItem`/`enterpriseBadge` 在 Task 3 定义并被 `astro.config.ts` import 一致。

**已知偏离与说明：**
- tasks.md 原文 1.2/3.2/7.1 与 Design Doc 锁定决策不一致，本计划以 Design Doc §9 Spec Patch 修正为准（root locale / badge 全渲染 / lunaria 不引入），Task 9 Step 8 已记录。
- Starlight badge 字段名以实际安装版本 API 为准（`badge` 单值或 `badges` 数组），Task 3 Step 2 已注明。
- Astro 6 若未稳定，Task 1 Step 3 注明降级 Astro 5 + 对应 starlight，配置不变。
- 首页 `homeProducts` 数据在 Step 6 的 map 逻辑较简（products 数据已在 i18n 字典中），可简化为直接遍历 `t.products.items`。如 TypeScript 报未使用导入，移除 `homeProducts`/`homeProducts` 导入即可。
