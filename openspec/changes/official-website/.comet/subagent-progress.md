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
- [ ] Task 4: Starlight 主题覆盖（映射 4.1/4.2）
- [ ] Task 5: 版本对比页（映射 5.1/5.2/5.3）
- [ ] Task 6: 首页（映射 6.1/6.2/6.3）
- [ ] Task 7: 工具链与部署（映射 7.1/7.2/7.3）
- [ ] Task 8: 文档站英文版（deferred，P0 不做，映射 8.1）
- [ ] Task 9: 验收（映射 9.1/9.2）

## 当前 Task

- Task: Task 4 - Starlight 主题覆盖
- 映射 OpenSpec task: 4.1 / 4.2
- 阶段: implementing
- 交接文件: openspec/changes/official-website/.comet/handoff/task-4-implementer.md
- 实现提交: 待定
- 风险信号自报: 待定
- 风险任务级 review: 未触发
- 审查-修复轮次: 0（standard 最多 1 轮）
- Comet 约束: 覆盖 Header/Footer/SiteTitle 组件（顶部导航：产品/文档/定价/版本对比/联系我们）；创建 navigation.ts；启用 astro.config.ts components 覆盖；注意 starlight 0.40 components API

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
