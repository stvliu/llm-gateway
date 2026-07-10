## 1. 项目脚手架与 i18n 架构

- [x] 1.1 在 `site/` 初始化 Astro 6 + @astrojs/starlight 项目（独立 `package.json`，pnpm，`.nvmrc` 锁定 Node 版本）
- [x] 1.2 配置 `astro.config.ts`：Starlight `locales`（zh root + en）、`trailingSlash: 'always'`、sitemap、editLink 指向 GitHub
- [x] 1.3 搭建目录结构（`src/content/docs`、`src/pages`、`src/data`、`src/components`、`src/layouts`、`src/styles`）

## 2. 文档迁移

- [x] 2.1 迁移 11 篇核心公开文档到 `src/content/docs/`，文件名改英文 slug 并加 Starlight frontmatter（见 Design Doc §5 映射表）
- [x] 2.2 复核内部调研文档（apipark/voapi/cc-switch/FEASIBILITY/竞品分析）未进入公开站点

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
