# Subagent 派发进度检查点

- Change: official-website
- build_mode: subagent-driven-development
- tdd_mode: direct
- review_mode: standard
- language: zh-CN
- 分支: feature/20260709/official-website
- base-ref: 9513ee84dabbccd7e9fdbc4ce48d888c4256e43a

## 预检计划审查结论

- 无 plan 与全局约束的硬矛盾
- 已知偏离（plan Self-Review 记录，以 Design Doc §9 Spec Patch 为准）：tasks.md 1.2/3.2/7.1 原文与 Design Doc 不一致 -- root locale（不配 en）/ badge 全渲染 / lunaria P0 不引入
- Task 1 sidebar 依赖缺陷：用户确认方案 A -- Task 1 建占位 `astro.sidebar.ts`（`export const sidebar = []`），Task 3 implementer 用完整 8 能力域配置替换

## Task 列表（plan 9 个 Task，Task 8 deferred）

- [x] Task 1: 项目脚手架与 i18n 架构（映射 tasks.md 1.1/1.2/1.3）✓ APPROVE c586e1e7
- [x] Task 2: 文档迁移（映射 2.1/2.2）✓ APPROVE a3d02058
- [x] Task 3: 侧边栏配置（映射 3.1/3.2/3.3）✓ APPROVE 5e0fb614+bafe22a1
- [x] Task 4: Starlight 主题覆盖（映射 4.1/4.2）✓ APPROVE 14d61d71
- [x] Task 5: 版本对比页（映射 5.1/5.2/5.3）✓ APPROVE b500566c+11cee6b5
- [x] Task 6: 首页（映射 6.1/6.2/6.3）✓ APPROVE 2f39d1bc
- [x] Task 7: 工具链与部署（映射 7.1/7.2/7.3）✓ APPROVE 1c44930a+0ba56cfb+2d8c533a
- [x] Task 8: 文档站英文版（deferred，P0 不做，映射 8.1）✓ DEFERRED（无实现，P0 决策推迟）
- [ ] Task 9: 验收（映射 9.1/9.2）

## 当前 Task

- Task: Task 7 - 工具链与部署
- 映射 OpenSpec task: 7.1 / 7.2 / 7.3
- 阶段: done
- 交接文件: openspec/changes/official-website/.comet/handoff/task-7-implementer.md
- 实现提交: 1c44930a（11 文件 176+/16-）+ fix 0ba56cfb（linkcheck fallback [I-1]）+ fix 2d8c533a（.mdx 交叉引用 .md 断链）
- 风险信号自报: 命中（implementer 回报 DONE_WITH_CONCERNS）-> 触发风险任务级 review
- 风险任务级 review: APPROVE（1 IMPORTANT [I-1] + 4 MINOR，均不阻塞）
- 审查-修复轮次: 1（[I-1] 修复 0ba56cfb 后暴露 8 个旧 fallback 漏报的真实 .mdx 断链，补修 2d8c533a）
- 验证证据: pnpm build 27 页、slugcheck ✅、linkcheck ✅ 0 断链（协调者自行重跑确认）
- reviewer 发现: [I-1] IMPORTANT linkcheck fallback 漏报（已修）；[M-1] 相对路径不支持（接受）；[M-2] slugcheck 目录不存在崩溃（接受）；[M-3] slugcheck 非 md/mdx 误报（接受）；[M-4] .gitignore .github/ 忽略（项目级待办）
- implementer 顾虑: (1) .gitignore:65 .github/ 忽略（项目级待办）；(2) /en/ 营销页文档链接 fallback 中文 /features/；(3) 文档入口指向 /features/
- task-checkoff: plan Step 5 PASS, tasks.md 7.1/7.2/7.3 PASS
- Comet 约束: lunaria P0 不引入（7.1 回写）；sitemap 已配；linkcheck/slugcheck；部署流水线；/docs/ 路由 + .mdx 交叉引用断链均已修复

## site 级待办（跨 Task，Task 7/9 处理）

- **[/docs/ 路由 404]**：✅ Task 7 已修复（navigation.ts/BaseLayout/homeFeatures 等的 /docs/ 前缀改为根级 /features/ 或 /xxx/；.mdx 交叉引用 .md 改页面路径，见 1c44930a+2d8c533a）
- **[英文页功能卡中文]**：homeFeatures 共享非 i18n，英文页功能卡中文。spec 偏离 Design Doc §4.4（features 应在 i18n）。Task 9 验收或后续 i18n 化
- **[死代码 products 映射]**：index.astro:14-17 未使用。Task 9 清理

## 已完成 Task 记录

### Task 7: 工具链与部署 - DONE
- 实现提交: 1c44930a（linkcheck/slugcheck/deploy-site.yml + /docs/ 断链修复 8 文件）
- fix: 0ba56cfb（[I-1] linkcheck fallback）+ 2d8c533a（.mdx 交叉引用 .md 断链 8 处）
- review: APPROVE（1 IMPORTANT [I-1] 已修+暴露并修复 8 漏报断链，4 MINOR 接受）
- 关键偏离: 文档入口 /docs/ -> /features/（root locale 无 /docs/）；workingDirectory 修正 wrangler-action
- task-checkoff: plan Step 5 PASS, tasks.md 7.1/7.2/7.3 PASS

### Task 6: 首页 - DONE
- 实现提交: 2f39d1bc（11 文件，4 数据+5 组件+2 页面，346 行）
- review: APPROVE（命中风险信号 diff 超 200 行；2 IMPORTANT 根因 spec/plan 层面非 implementer 返工：英文功能卡中文、/docs/ href 404；1 MINOR 死代码。作为 site 级待办）
- 关键偏离: homeFeatures 共享非 i18n（spec 偏离 Design Doc §4.4）
- task-checkoff: plan Step 9 PASS, tasks.md 6.1/6.2/6.3 PASS

### Task 5: 版本对比页 - DONE
- 实现提交: b500566c + fixup 11cee6b5
- review: NEEDS_FIX -> 修复后 APPROVE（spec PASS，1 IMPORTANT 修复：/en/contact-us/ 404 补建英文占位页；BaseLayout 内联偏离合理、editionDiff 覆盖准确）
- 关键偏离: BaseLayout 内联营销页 Header/Footer（starlight 组件依赖 Astro.locals.starlightRoute 运行时，营销页无该运行时，改内联+复用 navigation.ts）
- editionDiff 覆盖: 11 类别 58 功能项（README 52 矩阵 + 部署3 + 支持3），✅/🔒 一一对应
- task-checkoff: plan Step 8 PASS, tasks.md 5.1/5.2/5.3 PASS

### Task 4: Starlight 主题覆盖 - DONE
- 实现提交: 14d61d71
- review: APPROVE（命中风险信号 DONE_WITH_CONCERNS[404 顾虑经实测澄清为路径误判] + diff 超 200 行[239]，spec PASS，0.40 适配正确，.gitignore 修复必要）
- 关键偏离: starlight 0.40 适配（Header 不用独立容器、props->Astro.locals+virtual:starlight、SiteTitle 动态取值、CSS --sl-color-*、保留默认子组件）、.gitignore 修复（data/ 误伤 site/src/data/）
- 待 Task 9 跟进: /docs/ 导航链接在 root locale 下可能 404（应改根级路径）
- task-checkoff: plan Step 7 PASS, tasks.md 4.1/4.2 PASS

### Task 3: 侧边栏配置 - DONE
- 实现提交: 5e0fb614 + fixup bafe22a1
- review: NEEDS_FIX -> 修复后 APPROVE（spec PASS，2 IMPORTANT 修复：badge tip->success 绿色、类型导入移除；2 MINOR 留待 Task 9：slug 重复、代理配置 badge 语义）
- 关键偏离: features/index->features slug 修复（Astro glob loader）、badge variant success（用户决策，plan tip=绿色事实错误）
- 待 Task 9 跟进: provider-management slug 重复、代理配置 badge 产品语义确认
- task-checkoff: plan Step 4 PASS, tasks.md 3.1/3.2/3.3 PASS

### Task 2: 文档迁移 - DONE
- 实现提交: a3d02058（20 篇 .mdx + content.config.ts + 2 处 MDX 修复）
- review: APPROVE（命中风险信号 DONE_WITH_CONCERNS + diff 超 200 行，派发 reviewer，无 CRITICAL/IMPORTANT，1 MINOR badge 占位）
- 关键偏离: content.config.ts（Task 1 遗漏，Starlight 0.40 Content Layer API 必需）、MDX 兼容性修复（`<60`->`&lt;60`、裸花括号->反引号）
- 待 Task 3 跟进: sidebar.badge 覆盖企业版标识、迁移文档内部链接断链修正
- task-checkoff: plan Step 8 PASS, tasks.md 2.1/2.2 PASS

### Task 1: 项目脚手架与 i18n 架构 - DONE
- 实现提交: c586e1e7（site/ Astro 6.4.8 + Starlight 0.40 脚手架，root locale）
- 进度提交: 17a9d174
- review: APPROVE（命中风险信号 DONE_WITH_CONCERNS，派发 reviewer，无 CRITICAL/IMPORTANT，2 MINOR 可接受）
- 关键偏离: starlight 0.30->0.40（peer 兼容 astro 6.4.8）、social 改数组、新增 pnpm-workspace.yaml（allowBuilds）
- task-checkoff: plan Step 12 PASS, tasks.md 1.1/1.2/1.3 PASS

## 已完成 Task 记录

### Task 5: 版本对比页 - DONE
- 实现提交: b500566c + fixup 11cee6b5
- review: NEEDS_FIX -> 修复后 APPROVE（spec PASS，1 IMPORTANT 修复：/en/contact-us/ 404 补建英文占位页；BaseLayout 内联偏离合理、editionDiff 覆盖准确）
- 关键偏离: BaseLayout 内联营销页 Header/Footer（starlight 组件依赖 Astro.locals.starlightRoute 运行时，营销页无该运行时，改内联+复用 navigation.ts）
- editionDiff 覆盖: 11 类别 58 功能项（README 52 矩阵 + 部署3 + 支持3），✅/🔒 一一对应
- task-checkoff: plan Step 8 PASS, tasks.md 5.1/5.2/5.3 PASS

### Task 4: Starlight 主题覆盖 - DONE
- 实现提交: 14d61d71
- review: APPROVE（命中风险信号 DONE_WITH_CONCERNS[404 顾虑经实测澄清为路径误判] + diff 超 200 行[239]，spec PASS，0.40 适配正确，.gitignore 修复必要）
- 关键偏离: starlight 0.40 适配（Header 不用独立容器、props->Astro.locals+virtual:starlight、SiteTitle 动态取值、CSS --sl-color-*、保留默认子组件）、.gitignore 修复（data/ 误伤 site/src/data/）
- 待 Task 9 跟进: /docs/ 导航链接在 root locale 下可能 404（应改根级路径）
- task-checkoff: plan Step 7 PASS, tasks.md 4.1/4.2 PASS

### Task 3: 侧边栏配置 - DONE
- 实现提交: 5e0fb614 + fixup bafe22a1
- review: NEEDS_FIX -> 修复后 APPROVE（spec PASS，2 IMPORTANT 修复：badge tip->success 绿色、类型导入移除；2 MINOR 留待 Task 9：slug 重复、代理配置 badge 语义）
- 关键偏离: features/index->features slug 修复（Astro glob loader）、badge variant success（用户决策，plan tip=绿色事实错误）
- 待 Task 9 跟进: provider-management slug 重复、代理配置 badge 产品语义确认
- task-checkoff: plan Step 4 PASS, tasks.md 3.1/3.2/3.3 PASS

### Task 2: 文档迁移 - DONE
- 实现提交: a3d02058（20 篇 .mdx + content.config.ts + 2 处 MDX 修复）
- review: APPROVE（命中风险信号 DONE_WITH_CONCERNS + diff 超 200 行，派发 reviewer，无 CRITICAL/IMPORTANT，1 MINOR badge 占位）
- 关键偏离: content.config.ts（Task 1 遗漏，Starlight 0.40 Content Layer API 必需）、MDX 兼容性修复（`<60`->`&lt;60`、裸花括号->反引号）
- 待 Task 3 跟进: sidebar.badge 覆盖企业版标识、迁移文档内部链接断链修正
- task-checkoff: plan Step 8 PASS, tasks.md 2.1/2.2 PASS

### Task 1: 项目脚手架与 i18n 架构 - DONE
- 实现提交: c586e1e7（site/ Astro 6.4.8 + Starlight 0.40 脚手架，root locale）
- 进度提交: 17a9d174
- review: APPROVE（命中风险信号 DONE_WITH_CONCERNS，派发 reviewer，无 CRITICAL/IMPORTANT，2 MINOR 可接受）
- 关键偏离: starlight 0.30->0.40（peer 兼容 astro 6.4.8）、social 改数组、新增 pnpm-workspace.yaml（allowBuilds）
- task-checkoff: plan Step 12 PASS, tasks.md 1.1/1.2/1.3 PASS

## 已完成 Task 记录

### Task 4: Starlight 主题覆盖 - DONE
- 实现提交: 14d61d71
- review: APPROVE（命中风险信号 DONE_WITH_CONCERNS[404 顾虑经实测澄清为路径误判] + diff 超 200 行[239]，spec PASS，0.40 适配正确，.gitignore 修复必要）
- 关键偏离: starlight 0.40 适配（Header 不用独立容器、props->Astro.locals+virtual:starlight、SiteTitle 动态取值、CSS --sl-color-*、保留默认子组件）、.gitignore 修复（data/ 误伤 site/src/data/）
- 待 Task 9 跟进: /docs/ 导航链接在 root locale 下可能 404（应改根级路径）
- task-checkoff: plan Step 7 PASS, tasks.md 4.1/4.2 PASS

### Task 3: 侧边栏配置 - DONE
- 实现提交: 5e0fb614 + fixup bafe22a1
- review: NEEDS_FIX -> 修复后 APPROVE（spec PASS，2 IMPORTANT 修复：badge tip->success 绿色、类型导入移除；2 MINOR 留待 Task 9：slug 重复、代理配置 badge 语义）
- 关键偏离: features/index->features slug 修复（Astro glob loader）、badge variant success（用户决策，plan tip=绿色事实错误）
- 待 Task 9 跟进: provider-management slug 重复、代理配置 badge 产品语义确认
- task-checkoff: plan Step 4 PASS, tasks.md 3.1/3.2/3.3 PASS

### Task 2: 文档迁移 - DONE
- 实现提交: a3d02058（20 篇 .mdx + content.config.ts + 2 处 MDX 修复）
- review: APPROVE（命中风险信号 DONE_WITH_CONCERNS + diff 超 200 行，派发 reviewer，无 CRITICAL/IMPORTANT，1 MINOR badge 占位）
- 关键偏离: content.config.ts（Task 1 遗漏，Starlight 0.40 Content Layer API 必需）、MDX 兼容性修复（`<60`->`&lt;60`、裸花括号->反引号）
- 待 Task 3 跟进: sidebar.badge 覆盖企业版标识、迁移文档内部链接断链修正
- task-checkoff: plan Step 8 PASS, tasks.md 2.1/2.2 PASS

### Task 1: 项目脚手架与 i18n 架构 - DONE
- 实现提交: c586e1e7（site/ Astro 6.4.8 + Starlight 0.40 脚手架，root locale）
- 进度提交: 17a9d174
- review: APPROVE（命中风险信号 DONE_WITH_CONCERNS，派发 reviewer，无 CRITICAL/IMPORTANT，2 MINOR 可接受）
- 关键偏离: starlight 0.30->0.40（peer 兼容 astro 6.4.8）、social 改数组、新增 pnpm-workspace.yaml（allowBuilds）
- task-checkoff: plan Step 12 PASS, tasks.md 1.1/1.2/1.3 PASS

## 已完成 Task 记录

### Task 3: 侧边栏配置 - DONE
- 实现提交: 5e0fb614 + fixup bafe22a1
- review: NEEDS_FIX -> 修复后 APPROVE（spec PASS，2 IMPORTANT 修复：badge tip->success 绿色、类型导入移除；2 MINOR 留待 Task 9：slug 重复、代理配置 badge 语义）
- 关键偏离: features/index->features slug 修复（Astro glob loader）、badge variant success（用户决策，plan tip=绿色事实错误）
- 待 Task 9 跟进: provider-management slug 重复、代理配置 badge 产品语义确认
- task-checkoff: plan Step 4 PASS, tasks.md 3.1/3.2/3.3 PASS

### Task 2: 文档迁移 - DONE
- 实现提交: a3d02058（20 篇 .mdx + content.config.ts + 2 处 MDX 修复）
- review: APPROVE（命中风险信号 DONE_WITH_CONCERNS + diff 超 200 行，派发 reviewer，无 CRITICAL/IMPORTANT，1 MINOR badge 占位）
- 关键偏离: content.config.ts（Task 1 遗漏，Starlight 0.40 Content Layer API 必需）、MDX 兼容性修复（`<60`->`&lt;60`、裸花括号->反引号）
- 待 Task 3 跟进: sidebar.badge 覆盖企业版标识、迁移文档内部链接断链修正
- task-checkoff: plan Step 8 PASS, tasks.md 2.1/2.2 PASS

### Task 1: 项目脚手架与 i18n 架构 - DONE
- 实现提交: c586e1e7（site/ Astro 6.4.8 + Starlight 0.40 脚手架，root locale）
- 进度提交: 17a9d174
- review: APPROVE（命中风险信号 DONE_WITH_CONCERNS，派发 reviewer，无 CRITICAL/IMPORTANT，2 MINOR 可接受）
- 关键偏离: starlight 0.30->0.40（peer 兼容 astro 6.4.8）、social 改数组、新增 pnpm-workspace.yaml（allowBuilds）
- task-checkoff: plan Step 12 PASS, tasks.md 1.1/1.2/1.3 PASS

## 已完成 Task 记录

### Task 2: 文档迁移 - DONE
- 实现提交: a3d02058（20 篇 .mdx + content.config.ts + 2 处 MDX 修复）
- review: APPROVE（命中风险信号 DONE_WITH_CONCERNS + diff 超 200 行，派发 reviewer，无 CRITICAL/IMPORTANT，1 MINOR badge 占位）
- 关键偏离: content.config.ts（Task 1 遗漏，Starlight 0.40 Content Layer API 必需）、MDX 兼容性修复（`<60`->`&lt;60`、裸花括号->反引号）
- 待 Task 3 跟进: sidebar.badge 覆盖企业版标识、迁移文档内部链接断链修正
- task-checkoff: plan Step 8 PASS, tasks.md 2.1/2.2 PASS

### Task 1: 项目脚手架与 i18n 架构 - DONE
- 实现提交: c586e1e7（site/ Astro 6.4.8 + Starlight 0.40 脚手架，root locale）
- 进度提交: 17a9d174
- review: APPROVE（命中风险信号 DONE_WITH_CONCERNS，派发 reviewer，无 CRITICAL/IMPORTANT，2 MINOR 可接受）
- 关键偏离: starlight 0.30->0.40（peer 兼容 astro 6.4.8）、social 改数组、新增 pnpm-workspace.yaml（allowBuilds）
- task-checkoff: plan Step 12 PASS, tasks.md 1.1/1.2/1.3 PASS

## 已完成 Task 记录

### Task 1: 项目脚手架与 i18n 架构 - DONE
- 实现提交: c586e1e7（site/ Astro 6.4.8 + Starlight 0.40 脚手架，root locale）
- 进度提交: 17a9d174
- review: APPROVE（命中风险信号 DONE_WITH_CONCERNS，派发 reviewer，无 CRITICAL/IMPORTANT，2 MINOR 可接受）
- 关键偏离: starlight 0.30->0.40（peer 兼容 astro 6.4.8）、social 改数组、新增 pnpm-workspace.yaml（allowBuilds）
- task-checkoff: plan Step 12 PASS, tasks.md 1.1/1.2/1.3 PASS

## tasks.md 勾选映射文本（用于定向勾选验证）

- 1.1: 在 `site/` 初始化 Astro 6 + @astrojs/starlight 项目（独立 `package.json`，pnpm，`.nvmrc` 锁定 Node 版本）
- 1.2: 配置 `astro.config.ts`：Starlight `locales`（zh root + en）、`trailingSlash: 'always'`、sitemap、editLink 指向 GitHub（实际以 Design Doc §9 为准：仅 root locale，不配 en）
- 1.3: 搭建目录结构（`src/content/docs`、`src/pages`、`src/data`、`src/components`、`src/layouts`、`src/styles`）
