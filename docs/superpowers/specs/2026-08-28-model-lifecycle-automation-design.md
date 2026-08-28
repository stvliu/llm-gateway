# 模型生命周期管理自动化 — 技术设计

> 日期：2026-08-28
> 状态：已确认（§1 架构/状态/配置、§2 数据面信号识别、§3 管理面列表探测）

## 1. 背景与目标

### 1.1 现状

- **新增自动化（已上线）**：`ModelCatalogSyncService` 从 models.dev 拉目录，按 `externalId`（次选 `modelName`）幂等 upsert，自动新增/更新模型；含定时任务（DAILY/WEEKLY/MONTHLY）、手工触发、`catalog_sync_logs` 日志、人工字段锁定保护（`lockedFields`）。
- **两层生命周期概念已存在**：
  - `Model`（规格层，全局注册表）：`deprecatedAt/scheduledRetiredAt/deprecationMessage` 字段 + `isAvailable()`（= `deprecatedAt == null`）。
  - `ModelInstance`（渠道实例层）：5 状态（`PENDING/ACTIVE/SUSPENDED/DEPRECATED/RETIRED`），`isRoutable()` = `ACTIVE || DEPRECATED`。
- **路由已隔离废弃**：`ModelMatcher`、`ModelDiscoveryService`、`ModelExperienceService`、`PlanCatalogServiceImpl` 均过滤 `Model::isAvailable()`。

### 1.2 核心缺口

1. **数据源无废弃信息**：实测 `https://models.dev/models.json` 不含 `deprecated`/`aliases`/`deprecation_date` 任何废弃字段。
2. **上游消失无处理**：同步策略明确"不删除 models.dev 未出现的现有模型"，即上游移除的模型无任何自动动作。
3. **`scheduledRetiredAt` 无消费方**：字段存在但没有任何代码读取。
4. **废弃只能人工标记**：模型编辑接口可人工设置废弃字段，但无自动化信号。

### 1.3 目标

上游**新增**（已自动）、**即将废弃**、**废弃**三个阶段的自动化，减少人工干预，保证模型目录及时准确，且不误伤仍在宽限期的模型。

## 2. 需求决策（已与用户确认）

| 决策点 | 结论 |
|--------|------|
| 信号源 | **双通道**：官方模型列表 API 探测（提前预警）+ 运行期 `model_not_found`（兜底确认） |
| 路由策略 | **两段式**：即将废弃 = 继续路由 + 控制台标注；已废弃 = 停止路由 |
| 自动化程度 | **完全自动**：连续 N 次（默认 3，可配置）确认后自动标记 + 停用 + 审计 |
| 状态落地 | **渠道级**（方案 A）：即将废弃落在 `ModelInstance.DEPRECATED`；已废弃落在 `Model.deprecatedAt` + 实例转 `RETIRED` |
| 配置 | 系统设置分层：总开关 + 两条通道子开关 + 确认次数 + 探测周期 |
| 计数存储 | **进程内内存**（`ConcurrentHashMap`），开发/本地/单实例**不依赖 Redis** |

## 3. 总体架构

```
                    ┌─────────────────────────────────────────────┐
   上游数据源        │             管理面（Gateway）                  │
 ┌──────────────┐   │                                             │
 │ models.dev   │──▶│ CatalogSyncTask ──▶ 目录同步（新增/更新，已有）│
 │ (新增/更新)   │   │                                             │
 └──────────────┘   │ CatalogProbeTask ──▶ 上游列表探测（新）        │
 ┌──────────────┐   │   用渠道凭证调 /v1/models                     │
 │ 上游模型 API  │──▶│   消失连续N次 → ModelInstance 转 DEPRECATED  │
 │ (OpenAI等)   │   │               （即将废弃，继续路由）            │
 └──────────────┘   │                                             │
                    │             数据面（代理转发）                  │
 ┌──────────────┐   │ ┌────────────────────────────────────────┐  │
 │ 上游响应 404  │──▶│ │ 请求 → 上游返回 model_not_found        │  │
 │ model_not_   │   │ │   连续N次确认 → Model.deprecatedAt 全局  │  │
 │ found        │   │ │   停用 + 相关实例转 RETIRED             │  │
 └──────────────┘   │ └────────────────────────────────────────┘  │
                    └─────────────────────────────────────────────┘
```

### 3.1 状态语义（方案 A）

| 阶段 | 信号 | 状态落点 | 路由行为 |
|------|------|---------|---------|
| 正常 | — | `ModelInstance.ACTIVE` / `Model.deprecatedAt=null` | 正常路由 |
| **即将废弃** | 上游列表消失（连续 N 次探测） | `ModelInstance` 转 `DEPRECATED` | **继续路由**（上游宽限期）+ 控制台标注 |
| **已废弃** | 运行期 `model_not_found`（连续 N 次确认） | `Model.deprecatedAt` 设置 + 相关实例转 `RETIRED` | **停止路由**（`isAvailable()` 过滤） |

**配套修复**：`findActiveByModelIdOrderByPriority` / `findActiveByChannelId` 查询从严格 `ACTIVE` 改为 `isRoutable()`（ACTIVE + 未到期 DEPRECATED）——与 `ModelInstance.State.DEPRECATED.isRoutable()=true` 语义对齐，否则"即将废弃继续路由"无法落地。

### 3.2 系统设置项（新增）

| 配置 key | 类型 | 默认 | 说明 |
|---------|------|------|------|
| `catalog.deprecation.enabled` | boolean | true | **废弃自动化总开关**——关闭后运行期不标记、列表不探测、不自动停用（已标记状态保留） |
| `catalog.deprecation.runtime.enabled` | boolean | true | 调用中自动检查模型废弃（运行期 `model_not_found` 确认通道） |
| `catalog.deprecation.confirm-count` | int | 3 | 运行期/探测确认次数（防误判） |
| `catalog.deprecation.probe.enabled` | boolean | true | 上游列表探测（提前预警通道） |
| `catalog.deprecation.probe.interval` | enum | WEEKLY | 探测周期（复用 `SyncInterval`） |

- 总开关关闭 → 所有自动废弃行为停止（使能及关闭）。
- 子开关独立控制两条信号通道。
- 与现有 `catalog.sync.enabled` / `catalog.sync.interval`（目录同步）完全独立。

## 4. 数据面信号识别（运行期确认）

### 4.1 错误信号增强

- `UpstreamException` 增加 `httpStatus`（`Integer`）字段——协议层（openai/anthropic/gemini upstream client）解析上游错误响应时填充。
- 新增 `ProviderErrorType.MODEL_NOT_FOUND`：HTTP 404 且错误类型匹配（OpenAI `model_not_found` / Anthropic `not_found_error`）时归类。
- `ErrorClassifier` 映射 `MODEL_NOT_FOUND → NONE`（请求级，不故障转移——与现状一致，直接抛给用户，**不改变现有路由行为**）。

### 4.2 废弃检测器 `RuntimeDeprecationDetector`（proxy 域）

```
数据面请求 → 上游返回 model_not_found → UpstreamException(MODEL_NOT_FOUND)
  → ChatDispatchServiceImpl 捕获处挂接检测器
    → 开关校验（catalog.deprecation.enabled + catalog.deprecation.runtime.enabled 均开）
    → 内存计数 map.merge(modelName, 1, sum)      （正常路径零开销，只在异常分支）
    → 达到 confirm-count(3)？
        ├─ 否 → 继续（错误照常返回用户）
        └─ 是 → 清计数 + 幂等确认：
                Model.deprecatedAt = now
                deprecationMessage = "上游确认模型已废弃（model_not_found）"
                该模型所有 ModelInstance 转 RETIRED
                审计日志
```

**设计要点**：
- 正常路径零开销（只在 `MODEL_NOT_FOUND` 异常分支计数）。
- **计数用进程内 `ConcurrentHashMap`**（key=modelName），开发/本地/单实例不依赖 Redis。
- 防御误判：只认协议层明确归类的 `MODEL_NOT_FOUND`（404 + 错误类型匹配），网关配置错误/端点 404 不触发。
- 跨域依赖：proxy（数据面）→ provider 域接口（标记废弃），符合现有依赖方向。
- 幂等：已标记的模型重复确认直接跳过。
- 多实例部署各实例独立计数：确认动作幂等，最坏情况某实例提前触发；触发前提是"上游真实拒绝该模型"，不会误伤正常模型；实例重启计数清零无害（需重新积累）。

## 5. 管理面列表探测（提前预警）

### 5.1 探测客户端 `UpstreamModelProbeClient`

- 按渠道 `protocol` 类型调上游模型列表 API（渠道凭证解密后 `Bearer` 认证）：
  - OpenAI 兼容：`GET /v1/models` → `data[].id`
  - Anthropic：`GET /v1/models` → `data[].id`
  - Gemini：`GET /v1/models` → `models[].name`
- 归一化为模型 ID 集合返回。

### 5.2 探测编排 `CatalogProbeService`

```
对每个参与探测的渠道（有凭证 + protocol 支持列表 API）:
  拉取上游模型 ID 集合
  对比该渠道所有 ModelInstance.upstreamModelName:
    存在        → 正常（若曾标记"即将废弃"则自动恢复 ACTIVE——回滚保护）
    不存在      → 内存计数 +1（与运行期一致，幂等）
        连续 N 次探测消失 → 该 ModelInstance 转 DEPRECATED（"即将废弃"，继续路由）
  报告落 catalog_sync_logs（result=PROBE，独立于目录同步日志）
```

### 5.3 定时任务 `CatalogProbeTask`

- 复用 `CatalogSyncTask` 的调度模式（每小时检查，按 `catalog.deprecation.probe.interval` 判断是否执行）。
- 开关：`catalog.deprecation.enabled` + `catalog.deprecation.probe.enabled`。
- 装配开关 `gateway.catalog.probe.auto-enabled`（测试环境关闭，避免启动即真实探测，与 `CatalogSyncTask` 一致）。

### 5.4 状态流转（渠道级）

```
ACTIVE ──探测连续N次消失──▶ DEPRECATED（即将废弃，继续路由，控制台标注）
DEPRECATED ──探测重新出现──▶ ACTIVE（自动恢复）
DEPRECATED ──运行期确认──▶ RETIRED（已废弃，停用）+ Model.deprecatedAt
```

## 6. 前端与审计

- **渠道详情抽屉**模型映射表格：`DEPRECATED` 状态显示黄色"即将废弃"标签（`ModelInstanceResponse.state` 已透出）。
- **模型列表**：无变化（规格级未标记，渠道级不影响全局）。
- **审计**：自动状态变更（DEPRECATED/RETIRED）写管理操作审计（复用 gateway-audit 域）。

## 7. 错误处理

| 场景 | 处理 |
|------|------|
| 探测拉取失败（网络/凭证失效） | 记录 PROBE 日志（FAILURE），跳过该渠道，不阻断其他渠道；凭证错误不触发废弃标记 |
| 运行期确认动作失败 | 记录错误日志 + 审计失败，不向请求方暴露（错误已按原样返回用户） |
| 探测与运行期同时触发 | 幂等保护，先到先得，重复确认跳过 |
| 人工编辑与自动标记冲突 | 人工标记优先（已有 lockedFields 机制；自动标记不覆盖人工状态） |

## 8. 测试策略（TDD）

- **provider 域**：
  - `RuntimeDeprecationDetectorTest`：计数/阈值/幂等/开关/清计数
  - `CatalogProbeServiceTest`：消失→DEPRECATED、重新出现→ACTIVE、连续 N 次、凭证缺失跳过
  - `ModelInstanceServiceImpl` 状态流转（DEPRECATED/RETIRED 边界，已有状态机测试扩展）
- **protocol 域**：`UpstreamException.httpStatus` 填充、`MODEL_NOT_FOUND` 归类（openai/anthropic）
- **settings 域**：新配置项默认值与读取
- **web 域**：探测触发端点（若有）、状态查询
- **集成**：开关关闭时数据面零干预（装配开关测试环境关闭）

## 9. 后续工作（独立主题）

**同家族模型复制**（已确认"仅复制规格"）：Model 规格字段复制（modelName 必改/displayName/族/限额/能力/模态/描述/基准），不复制 `externalId/source/lockedFields/生命周期字段`；新模型 `source=MANUAL`。待独立设计文档。
