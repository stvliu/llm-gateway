# Subagent Progress — key-application-binding

> 恢复检查点。主会话协调，不直接写代码。

## 配置
- build_mode: subagent-driven-development
- tdd_mode: tdd
- review_mode: thorough
- isolation: branch (feature/20260706/key-application-binding)
- plan: docs/superpowers/plans/2026-07-06-key-application-binding.md

## 审查批次计划（thorough，每 ≤3 task 或风险边界合并审查）
- 批次1: Task 1-3（后端 DTO+Gateway+Service，高风险）✅ 通过
- 批次2: Task 4-6（delete 校验+resetPassword+endpoint，高风险）⏳ 审查-修复轮次 1/2
- 批次3: Task 7-8（集成测试+路由回归+后端验证）
- 批次4: Task 9-12（前端类型+3 页面）
- 批次5: Task 13（验证收尾）
- 最终完整审查

## 当前阶段
- 最终完整审查 派发中（thorough final review，覆盖 Task 1-13 + MINOR 待办评估）
- Task 13 DONE_WITH_CONCERNS：7.2 build 成功 + 测试逻辑正确（3 个超时为环境偶发，重跑通过），7.2 已勾选；7.3 端到端手验待人工（verify 阶段）
- MINOR 待办（最终审查评估）：testTimeout 调整、extractErrorMessage 统一修复、locales 补 key+清理 rotate 死 key、KeyGenerateModal 空状态引导、ApplicationControllerIT 补 IT 契约测试
- 批次4 通过（02526d25），批次3 通过（15f45219，702 tests）
  - CRITICAL-1: ApplicationControllerIT test-compile 失败。根因：Task 6 给 ApplicationController 加 UserApiKeyService 依赖，@RequiredArgsConstructor 生成双参构造，但 ApplicationControllerIT 第 48 行仍用单参 `new ApplicationController(applicationService)` → 编译失败
  - MINOR-1: import 顺序问题（Task 4-6 改动文件，待修复 agent 扫描定位）
  - MINOR-2: 末尾换行缺失（Task 4-6 改动文件，待修复 agent 扫描定位）

## Task 进度
- [x] Task 1: 后端 DTO 扩展（commit 2c0c04b4）
- [x] Task 2: Gateway findByApplicationId（commit 65126b8f）
- [x] Task 3: UserApiKeyServiceImpl 校验+映射+单测（commit 2e699d06 + f75d3b11 修复，批次1审查通过 08939ddf）
- [x] Task 4: ApplicationServiceImpl.delete 前置校验+单测（commit b2c0050b，批次2审查通过）
- [x] Task 5: UserController/UserService resetPassword+单测（commit f3398930，批次2审查通过）
- [x] Task 6: ApplicationController GET /applications/{id}/api-keys（commit 4e79019b，批次2审查通过）
- [x] Task 7: 集成测试+路由回归（commit 0838e48，25 tests，批次3审查通过）
- [x] Task 8: 后端全量测试+修复残留（702 tests PASS，无残留无提交，批次3审查通过）
- [x] Task 9: 前端类型/API 层（commit 3a64371，types/api 无错误，批次4审查通过）
- [x] Task 10: DownstreamKeysTable 改造（commit afaad6766，123 tests PASS，批次4审查通过）
- [x] Task 11: UserApiKeyModal 删 Alert+Application Select+补绑（commit 9493b1c1，6 组件测试，批次4审查通过）
- [x] Task 12: Applications 页查看 Key 入口（commit 28923e94，3 组件测试，批次4审查通过）
- [ ] Task 13: 验证收尾（7.2 build+test 通过，7.3 端到端手验待人工 verify）

## 审查记录
### 批次1（Task 1-3）— 通过
- 审查通过，commit 08939ddf 勾选 Task 1-3

### 批次2（Task 4-6）— 通过
- 第1轮审查：发现 CRITICAL-1 + MINOR-1 + MINOR-2
- 修复轮次 1：DONE（commit 5bfd2bf3，4 files，45 tests PASS）
  - CRITICAL-1: ApplicationControllerIT 单参→双参构造 + @Mock UserApiKeyService
  - MINOR-1: UserController/UserServiceImpl import 整理（cn.dev33 置首等）
  - MINOR-2: UserService/UserServiceImpl 补末尾换行
- 第2轮审查：PASS（无新 CRITICAL/IMPORTANT）
- MINOR 待办（纳入 Task 7）：Task 6 端点 IT 契约测试缺口、UserServiceImpl 静态字段位置
- MINOR 接受：Conflict 400 vs 409（设计妥协，超本批次）、ResetPasswordResponse 行尾（autocrlf 自动统一）

### 批次3（Task 7-8）— 通过
- Task 7（commit 0838e48）：5 个新测试，25 tests PASS
  - UserControllerTest +2（resetPassword 成功+内建拒绝）
  - ApplicationControllerTest 新建 +2（listApiKeys + delete 冲突）
  - RoutingResolverTest +1（applicationId 透传回归）
- Task 8：702 tests PASS，无残留无提交
- 第1轮审查：PASS（无 CRITICAL/IMPORTANT）
- 顾虑评估：测试风格非 MockMvc PASS（GlobalExceptionHandlerTest 400 + IamExceptionHandlerTest 403 覆盖 HTTP 映射）、RoutingResolver WARN PASS（既有模式）
- MINOR 待办（纳入最终审查）：ApplicationControllerIT 补 GET /api-keys + DELETE 冲突 IT 契约测试、ApplicationControllerTest 类注释 Task 编号引用

### 批次4（Task 9-12）- 通过
- Task 9（3a64371）：types/api 层加 applicationId，移除 rotate，加 listByApplication
- Task 10（afaad6766）：DownstreamKeysTable Application Select/列/筛选 + KeyGenerateModal 适配 + 2 组件测试
- Task 11（9493b1c1）：UserApiKeyModal 删 Alert+补绑 + resetPassword 明文展示 + 6 组件测试
- Task 12（28923e94）：Applications 查看 Key + 删除冲突提示 + 3 组件测试
- 前端 tsc + build 全绿，8 组件测试 PASS
- 第1轮审查：PASS（无 CRITICAL/IMPORTANT）
- 顾虑评估：extractErrorMessage 规避 PASS、KeyGenerateModal 适配 PASS、locales 兜底 PASS
- MINOR 待办（纳入最终审查/后续）：extractErrorMessage 统一修复、locales 补 key+清理 rotate 死 key、KeyGenerateModal 空状态引导、UserApiKeyModal 直接调 API 层（既有行为）
