# Brainstorm Summary

- Change: official-website
- Date: 2026-07-09

## 确认的技术方案

### 1. 侧边栏机制（已确认）

单一文档站 + 企业版 badge 标注。一套 `astro.sidebar.ts`，所有 8 大能力域条目都渲染，企业版专属项附 Starlight 原生 badge（`{ text: '企业版', variant: 'tip' }`）。**不做条件渲染，不做两套文档站。**

理由：企业版是标准版超集，文档结构一致；开源用户可见完整能力地图；维护成本最低；Starlight badge 是原生能力。

> 修正 open 阶段 design.md 的"isEnterprise 控制渲染"。

### 2. i18n 策略（已确认）

- **文档站**：P0 仅中文，Starlight 仅配 root locale（zh-CN），**不配 en locale**，不建英文目录骨架，不引入 lunaria fallback。避免未翻译 fallback 噪音。
- **营销页**（首页 / 版本对比页 / 顶部导航）：中英双语，通过 `src/data/i18n/` 字典 + Astro 动态路由（`/en/` 前缀）实现，组件按 locale 取值。
- **后续**：文档站引入 en locale + lunaria 作为独立后续任务，不阻塞 P0。

> 修正 open 阶段 proposal/design 的"文档站中英双语同步"为"文档站 P0 中文、营销页双语"。

### 3. 实现细节（设计方案，待用户确认）

- **脚手架**：`site/` Astro 6 + Starlight，独立 `package.json` + pnpm；`astro.config.ts` 配 root locale、trailingSlash always、sitemap、editLink；目录 `src/{content/docs,pages,data,components,layouts,styles}` + `src/components/starlight/`（主题覆盖）。
- **文档迁移**：11 篇 `docs/*.md` 复制到 `src/content/docs/`，**文件名改英文 slug**（如 `技术架构.md` -> `architecture/technical.md`，避免 URL 编码、利于 SEO），每篇加 Starlight frontmatter（title/description）。内部调研文档不迁移。P0 仅中文。
- **版本对比页**：`/standard-vs-enterprise/` + `/en/standard-vs-enterprise/`；`src/data/editionDiff.ts` 结构化数据（`categories[] -> items[] -> { name, standard, enterprise }`）；`src/data/i18n/editionDiff.ts` 中英文案。
- **首页**：`index.astro` 区块对标 thingsboard.io；`src/data/i18n/home.ts` 按 `{ zh, en }` 结构组织 hero/products/features/ecosystem；组件 Hero/ProductCard/FeatureCard/EcosystemCard/Carousel；控制台轮播 P0 占位图。
- **SEO**：canonical 用环境变量 `PUBLIC_SITE_URL` 配置（P0 占位 `https://llm-gateway.dev`），域名确定后一处改。
- **测试策略**：`pnpm build` 通过 + linkcheck（无断链）+ slugcheck（英文 slug 规范）+ 手动验收（中英切换、badge 标注、对比表覆盖度）。lunaria P0 不引入。

## 关键取舍与风险

- **营销页双语不依赖 Starlight locales**：文档站 root-only，营销页用独立 i18n 字典 + 动态路由。代价：营销页 locale 切换需自建（不享 Starlight 自动路由）。收益：文档站无 fallback 噪音，P0 简单。
- **文档 slug 改名**：中文文件名 -> 英文 slug，需建立映射表，原 docs/ 保留不删。
- **截图素材**：首页控制台轮播依赖 gateway-console 运行，P0 占位图，build 末段补齐。

## 测试策略

- 构建验证：`pnpm build` 无错误
- linkcheck：所有内部链接无断链（脚本 `lint:linkcheck`）
- slugcheck：文档 slug 为合规英文 kebab-case
- 手动验收：营销页中英切换、企业版 badge 标注、对比表覆盖 README 全部 ✅/🔒 项、CF Pages 预览部署

## Spec Patch

- `specs/website/spec.md` Requirement「文档站侧边栏组织」：场景「标准版上下文隐藏企业版条目」-> 调整为「企业版专属条目附 badge 标注，全部上下文均渲染」。
- `specs/website/spec.md` Requirement「中英双语国际化」：限定为「营销页（首页/对比页/导航）中英双语；文档站 P0 仅中文，en locale 作为后续任务」。
- `tasks.md`：2.2「建立英文目录骨架」-> 删除；8「英文文档翻译」-> 调整为「营销页英文文案已在 5.3/6.1 覆盖，文档站 en 作为后续独立任务」。
