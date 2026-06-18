# Brainstorm Summary

- Change: simulator-verification-enhancement
- Date: 2026-06-17

## 确认的技术方案

### SimulatorMode 扩展
- 从 3 种枚举扩展到 9 种：NORMAL / AUTH_ERROR / RATE_LIMITED / QUOTA_EXCEEDED / INVALID_REQUEST / UPSTREAM_ERROR / SERVICE_DOWN / TIMEOUT / INTERMITTENT
- TIMEOUT 模式：延迟后返回 408，延迟可配置（测试中 5s，Gateway 超时 2s）

### BehaviorSequence（行为序列）
- HTTP 状态码序列（如 `[200, 401, 500, 200]`）
- 支持一次性（消费完恢复全局模式）和循环（loop=true）两种模式
- 响应优先级：行为序列 > API Key 覆盖 > 全局模式
- 管理 API：POST/GET/DELETE `/simulator/behavior`

### 延迟配置
- 独立于模式的正交配置，可与任何模式组合
- 管理 API：POST/DELETE/GET `/simulator/delay`

### 流控制
- 可配置：chunk 数、chunk 间隔、中断、非法数据、重复 DONE、空 SSE、不完整流
- 管理 API：POST `/simulator/stream`

### API Key 覆盖
- 前缀匹配规则（如 keyPrefix: "sk-test-key1"）
- 管理 API：POST/DELETE/GET `/simulator/apikey-override`

### 集成测试架构
- gateway-boot test scope 引入 gateway-simulator Maven 依赖
- 集成测试中缩短熔断器/超时配置以加速测试

## 关键取舍与风险

| 取舍 | 决策 |
|------|------|
| 行为序列 vs 定时切换 | 行为序列（精确可控） |
| HTTP 状态码 vs 枚举名 | HTTP 状态码（直观） |
| 延迟独立 vs 内嵌 | 独立（灵活组合） |
| 前缀匹配 vs 精确匹配 | 前缀匹配（测试友好） |

**风险**：两个 Spring Boot 应用 Bean 冲突 → 不同包扫描路径 + exclude。

## 测试策略

- Simulator 单元测试 ~10、Controller 测试 ~8、端到端测试 ~7
- Gateway 集成测试 ~12 场景
- 总计 ~37 测试

## Spec Patch

无。
