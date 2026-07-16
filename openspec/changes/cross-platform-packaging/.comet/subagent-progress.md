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
4. 【Task 4.1 reviewer CRITICAL 修正】start.ps1 conf 解析须剥离 shell 引号（Linux source 自动剥离，Windows 正则不剥离 -> 加成对引号剥离逻辑）；plan start.ps1 脚本段已同步修复

## task-checkoff 文本约定
- plan：用每个 Task 唯一的 `**Step 1: <标题>**`（Step 3: 提交 在多 Task 重复，不可用）
- openspec tasks.md：用 task 完整行（`<编号> <完整描述>`）

## 任务进度总览（24 task / 8 组）
- [x] 1.1 conf 模板（a9551ba6 + fix b4a42966，CRITICAL DB_URL 引号已修，reviewer 复查通过）
- [x] 1.2 Linux 启动脚本（6cb1f719 实现，4f39548b 勾选）
- [x] 1.3 systemd unit（cb46a388 实现 + 917ad2d2 reviewer APPROVED 勾选）
- [x] 2.1 pom.xml JReleaser 插件（bece4fc8；恢复期 Step3 BUILD SUCCESS 补验；未命中风险信号）
- [x] 2.2 jreleaser.yml（e9138744；未命中风险信号）
- [x] 2.3 deb/rpm conffile 注释（19e9c568；未命中风险信号）
- [x] 3.1 postinst（22453d55；reviewer APPROVE，命中安全敏感面已 review 通过；MINOR 4 条记录接受）
- [x] 3.2 prerm/postrm（3e95320b；与 plan 逐字一致，rm -rf 限定 purge；未命中风险信号）
- [x] 3.3 删 -rpm 脚本（640fac2c；删除 3 文件 108 行；未命中风险信号）
- [x] 4.1 Windows ps1（9cc63035 + fix 59efe590；reviewer NEEDS_FIX CRITICAL#1 start.ps1 引号剥离，review-fix 1 轮通过；命中安全敏感面；plan start.ps1 脚本段已同步修复）
- [x] 4.2 llm-gateway.xml + WinSW（commit 26e1bade，恢复期核对一致，standard 直接勾选放行）
- [x] 5.1 build.sh（commit e6a7f820，125 行 diff 未命中风险，standard 直接放行）
- [x] 5.2 删 build.ps1（commit e7d2f0b3，132 行删除，standard 直接放行）
- [x] 6.1 release.yml package job（commit 28bf04ed，98 行 diff，standard 直接放行）
- [x] 6.2 smoke test（commit 112197cc，59 行插入，standard 直接放行）
- [x] 7.1 删 debconf（commit af32b4ec，24 行删除，standard 直接放行）
- [x] 7.2 删 iss（commit ac0dca38，206 行删除命中 diff>200 风险，task reviewer APPROVED，无 CRITICAL/IMPORTANT）
- [x] 8.1 build.sh 验证（主会话接管调试；JReleaser assemble.deb + archive 本地通过，rpm 留 CI）
- [ ] 8.2 deb 容器验证
- [ ] 8.3 rpm 容器验证
- [ ] 8.4 zip Windows 验证
- [ ] 8.5 升级验证
- [ ] 8.6 卸载验证
- [ ] 8.7 jlink 平台验证

## 当前 task
- plan task: 8.1 完成（主会话接管调试）；8.2-8.7 留 CI（本地无 docker/rpmbuild）
- OpenSpec task: 8.1 ✅；8.2-8.7 待 CI
- 阶段: 8.1 done（主会话接管，用户确认）；8.2-8.7 留 CI
- 派发状态: 主会话接管（执行者 sonnet+opus 卡住 40min，用户选主会话接管调试）
- 实现提交: 待提交（jreleaser.yml + templates + build.sh + llm-gateway.sh + design + plan + tasks 勾选）
- 变更文件: jreleaser.yml, templates/deb/control/{postinst,prerm,postrm}.tpl, build.sh, bin/llm-gateway.sh(chmod +x), design.md, plan, tasks.md
- 风险信号自报: diff ~280 行 > 200（含模板新文件 109 行）；配置 + 脚本改动
- 协调者风险预判: 命中 diff>200 风险信号，派发 task reviewer
- 风险任务级 review: 触发（diff>200）；reviewer 发现 2 CRITICAL + 2 MINOR，已全部修复并重跑 assemble 验证
- 审查-修复轮次: 1/1（reviewer 1 轮 -> 2 CRITICAL 修复 -> 重跑 assemble 验证 postinst chmod + llm-gateway.sh set -a + mode 0755 全通过）

## 已完成 task 历史
- 1.1: commit a9551ba6 + fix b4a42966；命中风险信号（密钥占位符）；reviewer CRITICAL（DB_URL 分号截断）已修复；task-checkoff PASS
- 1.2: commit 6cb1f719；4f39548b 勾选
- 1.3: commit cb46a388；917ad2d2 reviewer APPROVED 勾选
- 2.1: commit bece4fc8；恢复期补跑 Step3 BUILD SUCCESS；task-checkoff PASS
- 2.2: commit e9138744；haiku 转写与 plan 逐字一致；task-checkoff PASS
- 2.3: commit 19e9c568；YAML OK；task-checkoff PASS
- 3.1: commit 22453d55；命中安全敏感面；reviewer APPROVE（9 项核验全过）；MINOR 4 条接受；task-checkoff PASS
- 3.2: commit 3e95320b；与 plan 逐字一致，rm -rf 限定 purge；task-checkoff PASS
- 3.3: commit 640fac2c；删除 3 个 -rpm 脚本 108 行；task-checkoff PASS
- 4.1: commit 9cc63035（3 ps1）+ fix 59efe590（start.ps1 引号剥离）；命中安全敏感面（install.ps1 密钥生成）；reviewer NEEDS_FIX CRITICAL#1（conf 引号不剥离）；review-fix 1 轮通过（复查 start.ps1 引号剥离正确，PS 语法 OK，DB_URL/JAVA_OPTS/KEY 三项验证）；plan start.ps1 脚本段已同步修复引号剥离；task-checkoff PASS
- 4.2: commit 26e1bade（恢复期发现已提交：llm-gateway.xml 与 plan 逐字一致，download-winsw.ps1 仅改输出名 LLMGateway.exe->WinSW.exe，删旧 LLMGateway.xml）；diff 74 行 < 200 阈值，无安全敏感面/跨模块/schema/API -> 未命中风险信号；review_mode standard 直接勾选验证放行（未派发 task reviewer）；task-checkoff PASS
- 5.1: commit e6a7f820（build.sh 全文替换 JReleaser 方案：mvn package + jlink 双平台 JRE + jreleaser:assemble，删 jpackage 段）；diff 125 行（53 ins + 72 del）< 200 阈值；bash -n 语法 OK；协调者复核新文件与 plan 逐字一致；未命中风险信号；review_mode standard 直接勾选放行；task-checkoff PASS
- 5.2: commit e7d2f0b3（删除 build.ps1，132 行删除）；单文件删除无安全敏感面/跨模块/schema/API；diff 132 行 < 200 阈值；未命中风险信号；review_mode standard 直接勾选放行；task-checkoff PASS
- 6.1: commit 28bf04ed（release.yml package job 简化单 windows-latest 单 JDK 21 + finalize exe->zip）；diff 98 行（14 ins + 84 del）< 200 阈值；YAML OK；协调者复核 package job 与 plan 一致、finalize exe->zip、其他 job 保留；未命中风险信号；review_mode standard 直接勾选放行；task-checkoff PASS
- 6.2: commit 112197cc（release.yml 插入 3 个 smoke test 步骤：deb/rpm systemd 容器 + zip Windows runner）；diff 59 行（+59）< 200 阈值；YAML OK；协调者复核插入位置正确（Build packages 后、Upload artifacts 前）；未命中风险信号（--privileged 仅 CI 测试容器）；review_mode standard 直接勾选放行；task-checkoff PASS
- 7.1: commit af32b4ec（删除 debconf templates 8 行 + config 16 行 = 24 行）；无安全敏感面/跨模块/schema/API；diff 24 行 < 200 阈值；未命中风险信号；review_mode standard 直接勾选放行；task-checkoff PASS
- 7.2: commit ac0dca38（删除 llm-gateway.iss 206 行）；diff 206 行 > 200 阈值命中风险信号 -> 派发 task reviewer；reviewer APPROVED（spec 合规 + windows 目录符合预期 + jreleaser.yml/pom.xml/build.sh 无悬空引用，构建链完整）；MINOR 2 条接受（.gitattributes *.iss 遗留规则无害 + release.yml 注释保留）；无 CRITICAL/IMPORTANT；task-checkoff PASS
- 8.1: 主会话接管调试（执行者 sonnet+opus 卡住 40min，用户选主会话接管）；JReleaser 1.25.0 配置实测要点：basedir=repo root（非 project.basedir，日志验证）、installationPath:/ 使 fileSet output 为完整系统路径（避免前缀重复+etc/lib 困到 /opt）、control.provides 是 String（非数组）、project.languages.java（project.java 已废弃）、维护脚本经 templateDirectory control/*.tpl 注入、jar 由 build.sh 预复制固定名 llm-gateway.jar + fileSet includes 打入（artifacts transform 对 deb 未生效）；assemble.deb（纯 Java）+ archive（zip）本地通过，jpackage（rpm）active=RELEASE SNAPSHOT 跳过留 CI；实测产出 deb 109MB（Python 解 ar+xz+tar 验证含 jar 83MB+JRE+postinst/prerm/postrm+正确系统路径 etc/lib/opt）+ zip 113MB（含 jar+JRE+WinSW+ps1）；命中 diff>200 风险派发 reviewer，发现 2 CRITICAL（postinst 遗漏 chmod /opt/llm-gateway/bin/llm-gateway.sh + llm-gateway.sh source conf 未 export 致 Spring 读不到 SERVER_PORT/DB_URL/KEY）+ 2 MINOR（build.sh basedir 注释过时 + postrm 未清 /lib/systemd/system），全修复（postinst 加 chmod、llm-gateway.sh 加 set -a/+a、git --chmod=+x、build.sh 注释、postrm 加 rm），重跑 assemble 验证 postinst 含 chmod + llm-gateway.sh 含 set -a + mode 0755 全通过；rpm 留 CI（8.3）；task-checkoff PASS

## 最终审查
- 阶段: -
- 轮次: -
- 反馈: -
