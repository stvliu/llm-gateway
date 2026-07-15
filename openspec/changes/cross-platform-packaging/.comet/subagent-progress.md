# Subagent-Driven Development 进度检查点

> Comet 持久协调状态。每次派发/回报/审查/勾选后立即更新。
> 事实源：plan checkbox + OpenSpec tasks.md checkbox + 本文件。本文件不替代 plan/OpenSpec checkbox。

## 全局配置
- change: cross-platform-packaging
- plan: docs/superpowers/plans/2026-07-14-cross-platform-packaging.md
- workflow: full
- build_mode: subagent-driven-development
- tdd_mode: direct
- review_mode: standard
- isolation: branch (feature/20260714/cross-platform-packaging)
- base-ref: e0895232f4f747e0731f013a0a8941a4bca1f1a4

## 预检修正（用户已确认）
1. Task 1.1 conf JAVA_OPTS 加引号
2. Task 8.1 git add 改精确（deployments/package/）
3. 【审查新增】conf 所有含特殊字符的值（分号/空格）都需加引号 -- DB_URL 分号截断是同类 bug

## task-checkoff 文本约定
- plan：用每个 Task 唯一的 `**Step 1: <标题>**`（Step 3: 提交 在多 Task 重复，不可用）
- openspec tasks.md：用 task 完整行（`<编号> <完整描述>`）

## 任务进度总览（24 task / 8 组）
- [x] 1.1 conf 模板（a9551ba6 + fix b4a42966，CRITICAL DB_URL 引号已修，reviewer 复查通过）
- [x] 1.2 Linux 启动脚本（6cb1f719 实现，4f39548b 勾选）
- [x] 1.3 systemd unit（cb46a388 实现 + 917ad2d2 reviewer APPROVED 勾选）
- [x] 2.1 pom.xml JReleaser 插件（bece4fc8；恢复期 Step3 BUILD SUCCESS 补验；未命中风险信号）
- [x] 2.2 jreleaser.yml（e9138744；haiku 转写与 plan 逐字一致；未命中风险信号）
- [x] 2.3 deb/rpm conffile 注释（19e9c568；4 行注释与 plan 一致，YAML OK；未命中风险信号）
- [x] 3.1 postinst（22453d55；reviewer APPROVE，与 plan 418-479 逐字一致，符合 D2/D4/D7/D8；命中安全敏感面已 review 通过，9 项核验全过；MINOR 建议 4 条记录接受不采纳）
- [ ] 3.2 prerm/postrm  ← 当前
- [ ] 3.3 删 -rpm 脚本
- [ ] 4.1 Windows ps1
- [ ] 4.2 llm-gateway.xml + WinSW
- [ ] 5.1 build.sh
- [ ] 5.2 删 build.ps1
- [ ] 6.1 release.yml package job
- [ ] 6.2 smoke test
- [ ] 7.1 删 debconf
- [ ] 7.2 删 iss
- [ ] 8.1 build.sh 验证
- [ ] 8.2 deb 容器验证
- [ ] 8.3 rpm 容器验证
- [ ] 8.4 zip Windows 验证
- [ ] 8.5 升级验证
- [ ] 8.6 卸载验证
- [ ] 8.7 jlink 平台验证

## 当前 task
- plan task: Task 3.2 改 prerm/postrm（共用版，适配 conf 与新布局）
- OpenSpec task: 3.2 改 prerm/postrm：适配 conf 与 systemd unit 新布局（保留数据目录 `/var/lib/llm-gateway`）
- 阶段: implementing（待派发 implementer）
- 派发状态: 待派发
- 实现提交: -
- 变更文件: -
- 风险信号自报: 待回报
- 协调者风险预判: postrm purge 模式 `rm -rf /var/lib/llm-gateway /var/log/llm-gateway /etc/llm-gateway` 是破坏性操作但限定 purge 模式 + 明确路径（plan 要求），预判不命中风险信号清单（非认证/加解密/密钥）；以 implementer 自报 + 协调者复核为准
- 风险任务级 review: 未触发
- 审查-修复轮次: 0/1

## 已完成 task 历史
- 1.1: commit a9551ba6 + fix b4a42966；命中风险信号（密钥占位符）；reviewer 发现 CRITICAL（DB_URL 分号截断）已修复；复查通过；task-checkoff PASS
- 1.2: commit 6cb1f719；4f39548b 勾选 plan/tasks（修复 Step3 误勾选）
- 1.3: commit cb46a388（ExecStart 指向 llm-gateway.sh，去 EnvironmentFile）；917ad2d2 reviewer APPROVED 勾选
- 2.1: commit bece4fc8；恢复期补跑 Step3 BUILD SUCCESS；未命中风险信号；task-checkoff PASS
- 2.2: commit e9138744；haiku 转写与 plan 逐字一致；未命中风险信号；task-checkoff PASS
- 2.3: commit 19e9c568；4 行注释与 plan 一致，YAML OK；未命中风险信号；task-checkoff PASS
- 3.1: commit 22453d55（postinst +19/-52）；命中安全敏感面（密钥生成）；reviewer APPROVE（9 项核验全过，符合 D2/D4/D7/D8，与 plan 逐字一致）；MINOR 4 条（末尾换行/pipefail+非空校验/chmod 静默/conf 缺失分支）记录接受不采纳（实现忠于 plan）；task-checkoff PASS

## 最终审查
- 阶段: -
- 轮次: -
- 反馈: -
