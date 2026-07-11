# Comet Design Handoff

- Change: official-website
- Phase: design
- Mode: compact
- Context hash: 3cc7bcd1880311e2dbf38dfe6bc8b32e92f6f57ffaf63530951ea0a00e6537aa

Generated-by: comet-handoff.sh

OpenSpec remains the canonical capability spec. This handoff is a deterministic, source-traceable context pack, not an agent-authored summary.

## openspec/changes/official-website/proposal.md

- Source: openspec/changes/official-website/proposal.md
- Lines: 1-32
- SHA256: c192361186820076dfee83ce01f9eedb7ec7b8a57db2ae4f545c1f9f4ac3ba89

```md
## Why

LLM-Gateway 目前仅有 GitHub README 作为对外窗口，缺乏独立的官方网站与文档站，导致三个问题：开源传播完全依赖 README（SEO 缺失、首次印象弱）、企业版差异化能力（国密合规、语义缓存、MCP 协议）缺乏独立呈现与版本对比、文档散落在 `docs/` 目录无导航与全文检索。建设官网 + 文档站一体化站点是开源获客与企业版转化的前提。

## What Changes

- 新增 `site/` 目录：基于 Astro 6 + @astrojs/starlight 的官网与文档站一体化站点，与 `gateway-boot/console/cli` 平级，技术栈独立（不共享 gateway-console 的 React/Vite 依赖）
- 中英双语同步：zh 为 root locale，en 为第二 locale，lunaria 追踪翻译状态
- 迁移 `docs/` 11 篇核心公开文档到 `src/content/docs/`（spec、api-spec、技术架构、应用架构、数据架构、信息架构、constitution、routing-design、容灾方案设计、AI-Gateway功能特性、model-experience-design）
- 新增版本对比页 `/standard-vs-enterprise/`：标准版 vs 企业版功能对比表（基于 README 的 ✅/🔒 矩阵）+ 迁移路径，中英双语
- 新增首页：区块结构对标 thingsboard.io（Hero / 三产品卡 / 生态组件 / 控制台轮播 / 功能网格），中英双语文案数据化到 `src/data/i18n/`
- 新增侧边栏配置 `astro.sidebar.ts`：8 大能力域分组 + `isEnterprise` 标记（企业版条目在标准版上下文不渲染）
- 新增 GitHub Actions 部署流水线到 Cloudflare Pages
- 不修改 `gateway-boot/console/cli` 任何源码

## Capabilities

### New Capabilities

- `website`: 官方网站与文档站一体化能力——营销页（首页、版本对比页）+ 文档站（Starlight 侧边栏、内容迁移、全文检索）+ 中英双语 i18n + 部署流水线

### Modified Capabilities

无。官网是全新独立产物，不修改现有 gateway 业务 spec（application/channel/provider/user 等）的任何 requirement。

## Impact

- **新增代码**：`site/` 目录（Astro 项目，独立 `package.json`，隔离的 pnpm 依赖）
- **文档迁移**：`docs/` 11 篇公开文档复制到 `site/src/content/docs/`（原 `docs/` 保留作为开发文档源，不删除）
- **CI/CD**：新增 `.github/workflows/` 站点构建与部署任务（Cloudflare Pages 预览 + 生产）
- **依赖**：新增 astro、@astrojs/starlight、lunaria、sass、sharp 等前端依赖（隔离在 `site/` 内，不影响 Java/React 构建）
- **不受影响**：gateway-boot Java 源码、gateway-console React 源码、现有 REST API 与数据库 schema

```

## openspec/changes/official-website/design.md

- Source: openspec/changes/official-website/design.md
- Lines: 1-66
- SHA256: 82be53a4615a0250266a806e80ce47c24a801cfec4be0740ede1b0fba0c34238

```md
## Context

LLM-Gateway 是企业级大模型网关，采用开源（Apache-2.0）核心 + 企业版增值双模式。当前对外窗口仅 GitHub README，缺乏独立官网与文档站。本设计建设官网 + 文档站一体化站点，对标 thingsboard.io（Astro + Starlight 技术栈，已验证的"营销页 + 文档站同构"模式）。站点置于仓库 `site/` 目录，与 `gateway-boot/console/cli` 平级，技术栈与依赖完全独立。

详细技术设计（组件 API、样式系统、SEO 策略等）留待 `/comet-design` 阶段的 Design Doc 细化，本文档仅记录高层架构决策。

## Goals / Non-Goals

**Goals:**
- P0 交付四产物：脚手架、侧边栏配置、版本对比页、首页文案
- 中英双语架构就绪（zh root + en，lunaria 追踪）
- 11 篇核心公开文档迁移到文档站
- Cloudflare Pages 部署流水线

**Non-Goals:**
- 托管云产品线独立页、用例页、客户案例页、替代方案着陆页、博客改写
- 英文文档全量翻译（架构预留，翻译作为 build 并行任务）
- gateway-boot/console/cli 任何源码改动
- 首页控制台真实截图（P0 可占位，依赖 console 运行）

## Decisions

**1. Astro 6 + @astrojs/starlight（而非 Docusaurus / VitePress）**
营销页与文档站同构于一个 Astro 项目，Starlight 提供成熟文档主题。thingsboard.io 已用此组合验证。备选：Docusaurus（React 生态重，营销页定制弱）、VitePress（文档强但营销页能力弱）。Astro 零 JS 默认输出，SEO 与性能最优。

**2. 中英双语：zh root locale + en，lunaria 追踪**
Starlight `locales` 原生支持多语言路由（中文 root + `/en/` 前缀）。lunaria 生成翻译进度看板，未翻译项标红可见。营销页文案抽到 `src/data/i18n/` 字典，组件按 locale 取值，便于维护与 A/B 测试。

**3. 内容数据化（src/data/）**
学习 thingsboard.io 模式：首页功能卡、产品卡、轮播项、版本对比表数据全部放入 `src/data/*.ts`，组件只管渲染。文案与展示分离，降低维护成本。

**4. 文档迁移：复制而非移动，严格甄别**
11 篇公开文档从 `docs/` 复制到 `site/src/content/docs/`，原 `docs/` 保留作为开发文档源。内部调研文档（apipark/voapi/cc-switch/FEASIBILITY/竞品分析）不迁移，避免泄露未定方案。

**5. isEnterprise 侧边栏标记**
学习 thingsboard.io 的 `isPE` 模式：`astro.sidebar.ts` 用 `isEnterprise` 参数控制企业版专属条目渲染。标准版上下文不渲染企业版条目（避免"功能缺失"错觉），企业版上下文渲染全部。企业版条目附 Starlight `badge` 标注。

**6. 站点位置 site/，依赖隔离**
`site/` 独立 `package.json` + pnpm，不共享 gateway-console 的 React/Vite/Antd 依赖。避免前端构建相互影响。`site/` 失败不影响 Java/React 主构建。

**7. 部署：GitHub Actions -> Cloudflare Pages**
PR 触发预览部署，master 触发生产部署。构建含 linkcheck/slugcheck lint 防止文档腐烂。

## Risks / Trade-offs

- **[双语工作量 +50%]** -> 营销页（首页/对比页/导航）双语全量；文档站先中文 + 英文目录骨架，翻译作为 build 并行任务，lunaria 标红可见
- **[文档迁移甄别风险]** -> 严格按 proposal 迁移映射表，内部调研文档不进官网，build 前人工复核
- **[技术栈独立]** -> site/ 用 Astro，与 console(React+Vite) 独立，互不影响；代价是两套前端工具链
- **[截图素材依赖 console]** -> P0 首页轮播先用占位图，真实截图作为 build 末段任务补齐
- **[域名未定]** -> P0 用占位 origin（如 `https://llm-gateway.dev`），SEO canonical 与 sitemap 通过环境变量配置，域名确定后一处修改

## Migration Plan

1. 初始化 `site/` Astro + Starlight 项目，配置 locales
2. 迁移 11 篇公开文档（复制）+ 建英文目录骨架
3. 编写 `astro.sidebar.ts`（8 能力域 + isEnterprise）
4. 实现版本对比页 + 首页各区块
5. 配置 lunaria、sitemap、linkcheck、部署流水线

**回滚策略**：`site/` 为独立目录，删除即完全回滚，不影响 gateway-boot/console/cli 任何构建与运行。

## Open Questions

- 最终域名（影响 SEO canonical 与 sitemap）
- 英文文档翻译的优先级排序与时机
- 托管云产品页落地时间（P1，P0 仅占位卡）

```

## openspec/changes/official-website/tasks.md

- Source: openspec/changes/official-website/tasks.md
- Lines: 1-48
- SHA256: 4588e74a28aca726bce0c4deae89b918cd923beee7c0ca6850d3c3226d00d377

```md
## 1. 项目脚手架与 i18n 架构

- [ ] 1.1 在 `site/` 初始化 Astro 6 + @astrojs/starlight 项目（独立 `package.json`，pnpm，`.nvmrc` 锁定 Node 版本）
- [ ] 1.2 配置 `astro.config.ts`：Starlight `locales`（zh root + en）、`trailingSlash: 'always'`、sitemap、editLink 指向 GitHub
- [ ] 1.3 搭建目录结构（`src/content/docs`、`src/pages`、`src/data`、`src/components`、`src/layouts`、`src/styles`）

## 2. 文档迁移

- [ ] 2.1 迁移 11 篇核心公开文档到 `src/content/docs/`，文件名改英文 slug 并加 Starlight frontmatter（见 Design Doc §5 映射表）
- [ ] 2.2 复核内部调研文档（apipark/voapi/cc-switch/FEASIBILITY/竞品分析）未进入公开站点

## 3. 侧边栏配置

- [ ] 3.1 编写 `astro.sidebar.ts`：8 大能力域分组 + 折叠 + 中英 label
- [ ] 3.2 实现 `isEnterprise` 标记（企业版条目附 badge，标准版上下文不渲染）
- [ ] 3.3 配置 Recipes（场景配方）子组与参考（Reference）分组

## 4. Starlight 主题覆盖

- [ ] 4.1 覆盖 Header 组件（顶部导航：产品 / 文档 / 定价 / 版本对比）
- [ ] 4.2 覆盖 Footer 与 SiteTitle 组件

## 5. 版本对比页

- [ ] 5.1 创建 `src/data/editionDiff.ts`（功能对比表数据，覆盖 README 全部 ✅/🔒 项）
- [ ] 5.2 实现 `/standard-vs-enterprise/` 页面（对比表 + 迁移路径段）
- [ ] 5.3 实现英文版 `/en/standard-vs-enterprise/`

## 6. 首页

- [ ] 6.1 创建 `src/data/i18n/home.ts` 文案（Hero / 四差异化 / 三产品卡 / 生态组件 / 功能网格，中英双语）
- [ ] 6.2 实现首页 `index.astro` 各区块组件（Hero / ProductCard / FeatureCard / Carousel 等）
- [ ] 6.3 验证首页区块完整呈现与双语渲染

## 7. 工具链与部署

- [ ] 7.1 配置 lunaria 翻译状态追踪（`lunaria.config.ts`）
- [ ] 7.2 配置 sitemap 与 linkcheck/slugcheck lint 脚本
- [ ] 7.3 实现 GitHub Actions -> Cloudflare Pages 部署流水线（PR 预览 + master 生产）

## 8. 文档站英文版（后续独立任务，P0 不做）

- [ ] 8.1 文档站引入 en locale + lunaria（P0 营销页英文文案已在 5.3/6.1 覆盖，文档 en 作为后续任务）

## 9. 验收

- [ ] 9.1 本地 `pnpm dev` 与 `pnpm build` 验证通过
- [ ] 9.2 全量验收：中英切换、侧边栏 isEnterprise、对比表覆盖度、linkcheck 无断链、CF Pages 预览部署成功

```

## openspec/changes/official-website/specs/website/spec.md

- Source: openspec/changes/official-website/specs/website/spec.md
- Lines: 1-99
- SHA256: 6a671a5093d99f18fce03b2a874f9e0e0027b88955459e49dcfac4cbf8bb968d

[TRUNCATED]

```md
## ADDED Requirements

### Requirement: 站点项目结构

系统 SHALL 在 `site/` 目录提供 Astro 6 + @astrojs/starlight 项目，与 `gateway-boot/console/cli` 平级，拥有独立的 `package.json` 与隔离的 pnpm 依赖，不共享 gateway-console 的 React/Vite 依赖。

#### Scenario: 本地开发服务启动

- **WHEN** 在 `site/` 目录执行 `pnpm dev`
- **THEN** 本地开发服务启动，首页、文档站、版本对比页均可访问

#### Scenario: 静态构建产物生成

- **WHEN** 执行 `pnpm build`
- **THEN** 生成静态站点产物且无构建错误

### Requirement: 中英双语国际化

系统 SHALL 对营销页（首页、版本对比页、顶部导航）提供中英双语，通过 `src/data/i18n/` 字典与 `src/pages/en/` 动态路由实现。文档站 P0 仅中文（Starlight root locale，不配 en），文档站英文版作为后续独立任务。

#### Scenario: 营销页语言切换

- **WHEN** 访问者从中文营销页切换到英文
- **THEN** 营销页内容切换为英文版本，路径前缀变为 `/en/`，文案从 `src/data/i18n/` 加载

#### Scenario: 文档站 P0 仅中文

- **WHEN** 访问文档站
- **THEN** 文档站仅提供中文内容（root locale），不配 en locale，无未翻译 fallback 噪音

### Requirement: 文档站侧边栏组织

系统 SHALL 通过 `astro.sidebar.ts` 提供按 8 大能力域（API 网关 / Provider 管理 / 路由 / 用户与认证 / 密钥管理 / Token 计量与配额 / 安全与风控 / 可观测性）分组的折叠式侧边栏，企业版专属条目附 Starlight 原生 badge（"企业版"）标注，全部上下文均渲染（不做条件隐藏）。

#### Scenario: 企业版专属条目附 badge 标注

- **WHEN** 侧边栏渲染
- **THEN** 企业版专属条目（语义缓存、MCP 协议、国密、WORM 审计链等）均渲染并附"企业版"badge，标准版条目无 badge

#### Scenario: 全部能力域可见

- **WHEN** 访问者浏览文档站侧边栏
- **THEN** 8 大能力域全部条目（含企业版专属项）均可见，开源用户可见完整能力地图

### Requirement: 公开文档迁移

系统 SHALL 将 `docs/` 下 11 篇核心公开文档（spec、api-spec、技术架构、应用架构、数据架构、信息架构、constitution、routing-design、容灾方案设计、AI-Gateway功能特性、model-experience-design）迁移到 `src/content/docs/`，并建立中英目录结构。

#### Scenario: 核心文档可检索

- **WHEN** 访问者在文档站搜索"路由"或"协议转换"
- **THEN** 迁移后的 routing-design 等相关文档出现在搜索结果

#### Scenario: 内部调研文档不公开

- **WHEN** 构建站点
- **THEN** apipark、voapi、cc-switch、FEASIBILITY 等内部调研文档不出现在公开站点

### Requirement: 版本对比页

系统 SHALL 提供 `/standard-vs-enterprise/` 页面，含标准版 vs 企业版功能对比表（覆盖 README 全部 ✅/🔒 功能项）与迁移路径说明，中英双语。

#### Scenario: 对比表覆盖全部功能类别

- **WHEN** 访问者打开版本对比页
- **THEN** 表格覆盖 API 网关、Provider 管理、路由、用户认证、密钥管理、Token 配额、安全风控、可观测性、高级能力、部署、支持全部类别

#### Scenario: 迁移路径说明

- **WHEN** 访问者查看对比页迁移段
- **THEN** 看到"标准版起步 -> 平滑升级企业版"的配置切换说明（`llm-gateway.edition` 切换，数据兼容）

### Requirement: 首页内容架构

系统 SHALL 提供对标 thingsboard.io 的首页区块结构（Hero / 信任背书 / 价值主张 / 能力叙事 / 三产品卡 / 生态组件 / 控制台轮播 / 功能网格 / 底部 CTA），文案数据化到 `src/data/i18n/`。

#### Scenario: 首页区块完整呈现

- **WHEN** 访问者打开首页
- **THEN** 依次呈现 Hero、四差异化价值、三产品卡（标准版/托管云/企业版）、生态组件、控制台轮播、8 能力域功能网格

```

Full source: openspec/changes/official-website/specs/website/spec.md
