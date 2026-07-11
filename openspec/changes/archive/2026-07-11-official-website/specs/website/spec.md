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

#### Scenario: 双语文案渲染

- **WHEN** 切换语言
- **THEN** 首页所有区块文案从 `src/data/i18n/` 加载对应语言版本

### Requirement: 部署流水线

系统 SHALL 通过 GitHub Actions 构建并部署到 Cloudflare Pages，含预览环境（PR 触发）与生产环境（master 触发），构建过程包含链接检查。

#### Scenario: 预览部署

- **WHEN** 向 Pull Request 推送变更
- **THEN** GitHub Actions 构建站点并部署到 Cloudflare Pages 预览环境

#### Scenario: 链接检查通过

- **WHEN** 构建站点
- **THEN** linkcheck 通过且无断链
