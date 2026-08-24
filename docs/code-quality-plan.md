# LLM-Gateway 代码质量提升计划

> 生成日期：2026-08-24
> 依据：全项目多维度质量评估（详见下方"评估摘要"与 `findings.md`）
> 原则：先让质量防线真正生效（P0）→ 再补测试短板（P1）→ 做热点重构（P2）→ 工程化治理（P3）→ 技术演进（P4）

---

## 评估摘要

| 维度 | 现状 | 评分 |
|------|------|------|
| 代码可读性 | 方法平均 7.5 行、>100 行方法为 0、中文注释规范；仅 12 个大方法 | 4/5 |
| 架构与设计 | 17 模块 + 三明治结构，ArchUnit 7 条铁律生效，源码 TODO 仅 4 条 | 4/5 |
| 测试与工程化 | 行覆盖 89.63%，断言质量优秀；但 gateway-alert 0% / gateway-web 38.2%，三大质量工具"未接线" | 3.5/5 |
| 文档完备度 | constitution/spec/api-spec + 20+ 规划文档 + 19 份验证报告 | 5/5 |

**核心问题**：代码本体质量良好，但**质量保障工程化系统性"假 gate"**——checkstyle/spotbugs/owasp 三个工具均处于"配置存在但未接线"状态（CI 中全部失效且被 `continue-on-error: true` 吞掉），覆盖率 gate 已损坏（无 jacoco rules + test.yml YAML 语法错误）。真正生效的 gate 仅 3 个：license 头校验、ArchUnit、Trivy。

---

## P0 — 修复质量防线（预估 2-3 天）

> 目标：让每个质量检查真正成为 gate，杜绝"红着也合并"

| # | 动作 | 现状问题 | 验收标准 |
|---|------|---------|---------|
| P0-1 | 修复覆盖率 gate | 全部 pom 无 jacoco `check` goal 与 rules 阈值；test.yml 第 158-160 行把 `continue-on-error` 写进 `run:` 块导致 YAML 语法错误 | test.yml 的 coverage-check job 转绿；jacoco:check 配置 line/branch 阈值（建议起步 line ≥80%、branch ≥70%，逐步收紧） |
| P0-2 | 接线 Checkstyle | `checkstyle.xml` 是孤儿配置（任何 pom 未引用），CI 落到默认 sun_checks，gateway-boot 单模块 525 违规被吞 | 根 pom 配置 maven-checkstyle-plugin 并引用自研 checkstyle.xml；先以 `failOnError=false` 跑出基线、清零违规后转硬 gate |
| P0-3 | 接线 SpotBugs | 任何 pom 未配置 spotbugs 插件，CI `mvn spotbugs:check` 因前缀无法解析必然失败；`spotbugs-exclude.xml` 未传入 | pom 配置 spotbugs-maven-plugin（含 `-Dspotbugs.excludeFilterFile`）；CI 命令改为全限定或加 pluginGroups；同 P0-2 两步走 |
| P0-4 | 接线 OWASP 抑制规则 | 抑制文件名 `owasp-suppressions.xml` ≠ 插件默认名，且 security.yml 未传 `-Dodc.suppressionFiles`，6 条抑制全部失效；Jackson 规则一刀切抑制所有 CVE-2024-* 过宽 | security.yml 显式传 `-Dodc.suppressionFiles=owasp-suppressions.xml`；收窄 Jackson 规则（按具体 CVE 白名单） |
| P0-5 | 修复 CI 缓存顺序 | build.yml 中缓存 step 排在 `mvn` 之后，缓存从未生效 | cache 步骤移到执行 mvn 之前 |
| P0-6 | 依赖版本对齐 | spring-boot-maven-plugin 3.5.0 vs Boot 3.5.13；postgresql 属性 42.7.4 vs BOM 42.7.10 两处不一致 | 统一到 Boot BOM 版本，消除版本漂移 |

## P1 — 补测试短板（预估 1-2 周）

> 目标：兑现 CLAUDE.md 承诺的核心服务层 ≥90% 行覆盖

| # | 动作 | 现状 | 验收标准 |
|---|------|------|---------|
| P1-1 | gateway-web 补测 | 38.2% 行 / 25.4% 分支，最大短板 | ≥80% 行覆盖；Anthropic/OpenAI/Auth/Provider/UserApiKey Controller + TokenAuth/RateLimit/Gateway 三个 Interceptor 全覆盖 |
| P1-2 | gateway-alert 补测 | 0%，src/test 为空目录 | ≥90% 行（AlertNotification/AlertRule/AlertConfiguration 3 类） |
| P1-3 | gateway-simulator 补测 | 68.7% 行 / 56.3% 分支 | ≥90% 行覆盖 |
| P1-4 | 全项目分支覆盖提升 | 75.9%（多模块 50-80% 区间） | ≥80%，重点补边界条件与异常分支 |
| P1-5 | -data 模块集成测试 | 全项目无 @DataJpaTest/Testcontainers，数据层零 Spring 测试 | 每 -data 模块补 @DataJpaTest 或 Testcontainers 冒烟测试 |

## P2 — 代码热点重构（预估 2-4 周）

> 目标：消除 SRP 高风险点（大方法 + 高复杂度 + 大类叠加）

| # | 动作 | 现状 | 验收标准 |
|---|------|------|---------|
| P2-1 | ChannelFailoverInvoker 拆分（492 行） | invokeStream 89 行 + invoke 54 行 + buildStreamCallback 49 行 | 提取策略类/回调构建器，类 <300 行，方法 ≤50 行 |
| P2-2 | ChannelProvisionService 拆分（410 行） | provisionFromPlan 90 行 + provisionBatch 53 行 | 提取套餐解析/批次执行逻辑，类 <300 行 |
| P2-3 | 高复杂度方法重构 | getLabel(13)/parseMode(10)/chatStream(11)/preHandle(10) 等 8-9 个方法 CC>10 | switch→枚举映射/策略，CC ≤8 |
| P2-4 | 清理死管理依赖 | 根 DM 11 条无人引用（redisson/springdoc/otel×3/testcontainers×6） | 删除或补实际使用（如 springdoc 已注释"需要添加依赖"） |
| P2-5 | 过时依赖升级 | jgit 6.8.0（现行 7.x）、jetbrains:annotations 13.0（现行 25.x）、archunit 1.3.0 | 升级并纳入根 DM 集中管理 |

## P3 — 工程化治理（持续，1-2 月）

> 目标：把质量度量常态化、可视化

| # | 动作 | 目标 |
|---|------|------|
| P3-1 | 引入重复代码检测（PMD-CPD 或 jscpd） | 建立重复率基线 <5%，纳入 CI 报表 |
| P3-2 | 构建时间度量与优化 | 记录 CI 各阶段时长，全量验证 ≤10 分钟 |
| P3-3 | Codecov 阈值配置 | 新增 codecov.yml，line ≥80% 门禁（当前 fail_ci_if_error: false 无约束） |
| P3-4 | 前端规范补全 | 补 .editorconfig/.prettierrc，ESLint 纳入前端 CI |
| P3-5 | 源码 TODO 清零 | 4 条 TODO 转正式任务（ChannelCredentialServiceImpl/ConnectivityTesterImpl/ModelExperienceService/StatsService） |

## P4 — 技术演进（季度级）

> 目标：保证长期生存能力

| # | 动作 | 目标 |
|---|------|------|
| P4-1 | Spring Boot 3.5→4.x 升级路径规划 | 3.5 免费支持 2026 年中结束，提前制定迁移方案与排期 |
| P4-2 | 质量报表可视化 | jacoco/复杂度/重复率报表进 CI 产物，或引入 SonarQube |
| P4-3 | 监控告警与 MTTR 度量 | 生产监控 + 告警 + 事故复盘制度化 |

---

## 执行建议

1. **P0 全部完成后**，立即重跑 CI 确认各检查项真实转绿（`verification-before-completion` 原则：以证据为准）
2. **P1 与 P0-2/P0-3 联动**：checkstyle/spotbugs 转硬 gate 前，先让 gateway-web/alert/simulator 补测降低存量违规
3. 每项完成后更新本文件勾选状态，形成可审计的改进轨迹
4. 重构（P2）必须依赖 P1 的测试兜底后再动，避免"重构无网"

## 工作量汇总

| 优先级 | 预估 | 性质 |
|--------|------|------|
| P0 | 2-3 天 | 修复性，立即执行 |
| P1 | 1-2 周 | 补测，与 P0 联动 |
| P2 | 2-4 周 | 重构，需测试兜底 |
| P3 | 1-2 月（持续） | 工程化治理 |
| P4 | 季度级 | 技术演进规划 |
