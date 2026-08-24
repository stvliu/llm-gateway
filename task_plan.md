# Task Plan: llm-gateway 代码质量评估与提升计划

## Goal
对 llm-gateway 项目进行多维度（代码可读性、架构设计、测试与工程化）系统评估，产出客观的质量评估报告，并制定一份可执行、有优先级、可验证的完整代码质量提升计划。

## Current Phase
Phase 5（交付）

## Phases

### Phase 1: 数据收集（并行探索）
- [x] 代码规模与结构：模块/类/代码行数/大方法/圈复杂度（452 文件/34,887 行；12 个大方法；Top CC 13）
- [x] 测试与覆盖率：jacoco 聚合结果、测试数量、测试金字塔结构（行 89.63%/分支 75.9%；alert 0%、web 38.2%）
- [x] 质量工具与工程化：checkstyle/spotbugs/owasp 配置与执行、CI 配置、技术债标记（三大工具未接线；gate 仅 3 个）
- [x] 依赖健康：依赖版本管理、数量、已知漏洞（版本漂移；OWASP 抑制未接入）
- **Status:** complete

### Phase 2: 分析评估
- [x] 代码可读性与可维护性评估（命名/注释/方法长度/复杂度/重复代码）
- [x] 架构与设计评估（耦合/单一职责/分层/技术债/依赖管理）
- [x] 测试与工程化评估（覆盖率/测试金字塔/CI/CD/构建时间）
- **Status:** complete

### Phase 3: 综合评估报告
- [x] 对照实用评估清单逐项打分（1-5 分）
- [x] 列出主要问题与风险排序
- [x] 输出评估报告（写入 findings.md / 交付文档）
- **Status:** complete

### Phase 4: 制定完整改进计划
- [x] 按优先级（P0-P4）制定改进措施
- [x] 每项措施含：目标、验收标准、预估工作量
- [x] 形成路线图（docs/code-quality-plan.md）
- **Status:** complete

### Phase 5: 交付
- [x] 汇总评估报告 + 改进计划交付用户
- [ ] 与用户确认后续执行方式
- **Status:** in_progress

### Phase 6: 包结构专项分析（用户追加，2026-08-25）
- [x] 全工程包结构扫描（106 包 / 36 单文件包 / 0 循环依赖 / 0 反向依赖）
- [x] 耦合度/内聚度/抽象稳定性度量（代理回报，DAG 主链清晰）
- [x] **修复 BaseDo 包名**：infrastructure.common → common.data（git mv + 22 引用 + 编译通过）
- [x] 评估 gateway-common → gateway-core（结论：不推荐）
- [ ] 待确认：provider.impl 命名、DTO 位置统一、CredentialEncryptorAdapter 归属等后续修复项
- **Status:** in_progress

## Key Questions
1. 项目各模块的实际测试覆盖率是多少？是否满足 CLAUDE.md 承诺的 ≥90%？
2. 质量工具（checkstyle/spotbugs/owasp）是否在构建中强制 gate？
3. 代码重复率、圈复杂度、大方法是否有数据支撑？
4. CI 流水线覆盖哪些检查？绿率与构建时间？
5. 依赖管理是否有统一版本管理（BOM）？有无高危漏洞？

## Decisions Made
| Decision | Rationale |
|----------|-----------|
| 使用并行子代理收集各维度数据 | 评估任务量大，并行可显著提速且互不依赖 |
| 用文件持久化所有发现 | 评估维度多、数据量大，防上下文丢失 |

## Errors Encountered
| Error | Attempt | Resolution |
|-------|---------|------------|
|       |         |            |

## Notes
- 更新阶段状态：pending → in_progress → complete
- 每完成一批数据收集立即写入 findings.md
- 所有评估必须基于证据（实际数据），不能主观臆断
