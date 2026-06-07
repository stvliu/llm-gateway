# 供应域合并 — 验证报告

**Change**: supply-domain-merge
**验证日期**: 2026-06-07
**验证模式**: full
**验证结果**: PASS

## 检查项

| # | 检查项 | 结果 |
|---|--------|------|
| 1 | tasks.md 全部任务已完成 | PASS |
| 2 | 实现符合 design.md 高层设计 | PASS |
| 3 | 实现符合 Design Doc | PASS |
| 4 | 能力规格场景全部通过 | PASS |
| 5 | proposal.md 目标已满足 | PASS |
| 6 | delta spec 与 design doc 无矛盾 | PASS |
| 7 | 编译通过 | PASS |
| 8 | 全部测试通过 (363/363) | PASS |

## 修复记录

| 问题 | 严重程度 | 修复方式 |
|------|---------|---------|
| ChannelServiceImpl.setState 未在接口声明 | CRITICAL | ChannelService 接口补充 setState 声明 |
| UserServiceImpl.GatewayRequestException 未 import | CRITICAL | 补充 import |
| ModelInstanceRepository priority 排序方向为降序 | CRITICAL | 改为升序（最小值优先） |
| ModelInstance 能力覆盖/上下文窗口覆盖逻辑未实现 | WARNING | 添加 resolveCapabilities/resolveContextWindow |
| PlanCatalogService.getPricing 方法缺失 | WARNING | 补充接口声明与实现 |
| RoutingResolverTest 缺少 InstanceSelector mock | CRITICAL | 添加 @Mock 与 mock 行为设置 |

## 确认

- 所有 CRITICAL 问题已修复
- 286 个文件变更，12359 行新增，13624 行删除
- 实体数从 14 减少到 8
- 定价数据集中在 PlanCatalog.pricing
- Provider/Model 合并完成，删除双轨数据冗余
- ChannelModel 重命名为 ModelInstance，支持实例级能力覆盖
- 物化概念替换为供给（ChannelProvisionService）