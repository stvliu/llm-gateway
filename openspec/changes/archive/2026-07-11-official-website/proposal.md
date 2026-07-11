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
