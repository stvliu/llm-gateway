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

- [ ] Task 1: 项目脚手架与 i18n 架构（映射 tasks.md 1.1/1.2/1.3）
- [ ] Task 2: 文档迁移（映射 2.1/2.2）
- [ ] Task 3: 侧边栏配置（映射 3.1/3.2/3.3）
- [ ] Task 4: Starlight 主题覆盖（映射 4.1/4.2）
- [ ] Task 5: 版本对比页（映射 5.1/5.2/5.3）
- [ ] Task 6: 首页（映射 6.1/6.2/6.3）
- [ ] Task 7: 工具链与部署（映射 7.1/7.2/7.3）
- [ ] Task 8: 文档站英文版（deferred，P0 不做，映射 8.1）
- [ ] Task 9: 验收（映射 9.1/9.2）

## 当前 Task

- Task: Task 1 - 项目脚手架与 i18n 架构
- 映射 OpenSpec task: 1.1 / 1.2 / 1.3
- 阶段: task-review（命中风险信号 DONE_WITH_CONCERNS，已派发每任务 reviewer）
- 交接文件: openspec/changes/official-website/.comet/handoff/task-1-implementer.md
- 实现提交: c586e1e7
- 风险信号自报: DONE_WITH_CONCERNS（命中风险清单"implementer 返回 DONE_WITH_CONCERNS"；starlight 0.30->0.40 升级影响后续 Task API）
- 风险任务级 review: 已触发（reviewer agent 派发中）
- 审查-修复轮次: 0（即将进入第 1 轮，standard 最多 1 轮）
- Comet 约束: 占位 `site/astro.sidebar.ts`（`export const sidebar = []`）已创建确认
- 关键偏离: starlight 0.30->0.40（peer 兼容 astro 6）、social 改数组、新增 pnpm-workspace.yaml（allowBuilds）

## tasks.md 勾选映射文本（用于定向勾选验证）

- 1.1: 在 `site/` 初始化 Astro 6 + @astrojs/starlight 项目（独立 `package.json`，pnpm，`.nvmrc` 锁定 Node 版本）
- 1.2: 配置 `astro.config.ts`：Starlight `locales`（zh root + en）、`trailingSlash: 'always'`、sitemap、editLink 指向 GitHub（实际以 Design Doc §9 为准：仅 root locale，不配 en）
- 1.3: 搭建目录结构（`src/content/docs`、`src/pages`、`src/data`、`src/components`、`src/layouts`、`src/styles`）
