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
