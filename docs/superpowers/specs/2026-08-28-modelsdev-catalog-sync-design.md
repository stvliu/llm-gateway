# 模型目录及时准确设计（models.dev 同步）

> 日期：2026-08-28
> 状态：已确认（精简版：仅模型维度）
> 功能目标：**模型数据及时准确**

## 1. 背景与目标

llm-gateway 当前的模型目录为**静态人工维护**的种子 JSON（`gateway-boot/src/main/resources/catalog/model-specs.json`，75+ 模型），严重滞后（仍停留在 gpt-4o / claude-3.5 时代），且缺少结构化能力/限额/许可证/发布时间等元数据。

[models.dev](https://models.dev) 的 `models.json`（361 个唯一模型主数据）提供权威、完整的模型信息。本设计引入 models.dev 作为**模型参考数据源**，通过**手工触发同步**落地模型数据，实现"及时准确"。

### 本期范围（仅模型）

1. **模型信息完善**：models 表补齐 description、release_date、last_updated、license、open_weights、benchmarks、weights、external_id（canonical ID）等字段。
2. **能力字段结构化映射**：models.dev 的 attachment / reasoning / tool_call / structured_output 映射进现有 `capabilities` Map。
3. **手工同步机制**：内置 models.dev 模型数据源适配器，管理员通过 API/控制台手动触发同步，增量 upsert、变更审计、人工编辑保护。

### 非目标（后续阶段）

- ❌ 厂商信息完善（env / api_compatibility / default_endpoint）——后续阶段
- ❌ 厂商-模型供应关系表 `model_supplies`（含参考定价）——后续阶段
- ❌ 定时自动同步（已确认手工同步）
- ❌ 不把 models.dev 定价写入系统（模型无单一价格，定价属供应维度，留待供应关系阶段）
- ❌ 不删除现有旧模型（避免破坏已有渠道、路由、token 限额）

## 2. 数据源分析（models.dev models.json）

`https://models.dev/models.json`（约 292KB）为**模型主数据**视图：361 个唯一模型（canonical ID 去重），与 `catalog.json` 的 `models` 完全一致（已核验）。

| 字段 | 覆盖率 | 说明 |
|------|--------|------|
| `id` | 100% | canonical ID（如 `openai/gpt-4o`、`deepseek/deepseek-v4-flash`） |
| `name` / `description` | 100% | 显示名 / 描述 |
| `family` | 97% | 模型族（如 `deepseek-flash`、`llama`） |
| `attachment` / `reasoning` / `tool_call` | 100% | 多模态 / 推理 / 工具调用能力 |
| `structured_output` | 38% | 结构化输出 |
| `temperature` | 98% | 是否支持温度参数 |
| `knowledge` | 59% | 知识截止日期 |
| `release_date` / `last_updated` | 100% | 发布日期 / 最后更新 |
| `modalities` | 100% | 输入/输出模态（text/image/audio） |
| `open_weights` | 100% | 是否开源权重 |
| `limit` | 100% | `context` / `output` / `input` 限额 |
| `weights` / `benchmarks` | 37% | 权重链接 / 基准测试分数 |
| `license` | 9% | 许可证（如 MIT） |

**映射结论**（已核验）：
- canonical ID 末段即通用模型名（`deepseek/deepseek-v4-flash` → `deepseek-v4-flash`），可作为默认 modelName。
- 现有种子的旧模型名（如 `deepseek-chat` = V3 时代）在 models.dev 已不存在——同步不删除旧模型，仅新增新模型，避免破坏现有渠道/路由/限额。

## 3. 现状与差距（模型维度）

| 维度 | 现状 | 目标 | 差距 |
|------|------|------|------|
| 数量 | 75+（静态种子） | 361+（全量目录） | 数量级 |
| 基础信息 | modelName/displayName/family/context/limits/knowledge | +description/release_date/last_updated/license | 元信息缺失 |
| 能力 | capabilities(Map)：vision/function_calling/streaming | +reasoning/structured_output 等 | 能力不全 |
| 技术属性 | 无 | open_weights/benchmarks/weights/external_id | 缺失 |
| 同步机制 | 静态 JSON，启动表空加载 + forceReload | 手工触发 + models.dev 数据源 + 增量 upsert + 审计 | 全新 |

## 4. 方案总览

```
[管理员] ──API/控制台──▶ ModelCatalogSyncController (web)
                              │
                              ▼
                ModelCatalogSyncService (provider.catalog.sync)
                ① 拉取  ② 转换  ③ 模型 upsert
                              │
                    ┌─────────┴─────────┐
                    ▼                   ▼
              models 表              AuditEvent
              (+模型元信息)            (审计闭环)
                    ▲
                    │
    ModelCatalogClient (provider.catalog.sync)
    └── RestClient 拉取 https://models.dev/models.json
```

## 5. 数据模型设计（models 表扩展）

V69 迁移 + Model.java + ModelDo.java 新增列：

| 列 | 类型 | 说明 |
|----|------|------|
| `description` | text | 模型描述 |
| `release_date` | date | 发布日期 |
| `last_updated` | date | 最后更新日期 |
| `license` | varchar(128) | 许可证（如 MIT） |
| `open_weights` | boolean | 是否开源权重 |
| `benchmarks` | jsonb | 基准测试数组 `[{name, score, metric, source}]` |
| `weights` | jsonb | 权重/模型卡片链接数组 `[{label, url}]` |
| `source` | varchar(32) | 数据来源：`MODELS_DEV` / `BUILTIN` / `MANUAL`，默认 `MANUAL` |
| `external_id` | varchar(256) | models.dev canonical ID（如 `openai/gpt-4o`），同步幂等匹配键 |
| `locked_fields` | jsonb | 人工锁定字段名集合（§7），空表示无锁定 |

**能力字段映射**（不新增列，映射进现有 `capabilities` Map，键名对齐现有前端语义）：
- `attachment` → `capabilities["vision"]`
- `reasoning` → `capabilities["reasoning"]`
- `tool_call` → `capabilities["function_calling"]`
- `structured_output` → `capabilities["structured_output"]`
- `modalities`（input/output 合并去重）→ 现有 `modalities` 列

**限额映射**（现有列已具备）：
- `limit.context` → `contextWindow`
- `limit.input` → `maxInputTokens`（缺失时回退 contextWindow）
- `limit.output` → `maxOutputTokens`

**知识截止**：`knowledge` → `knowledgeCutoff`（现有列）。

## 6. 同步子系统设计

### 6.1 ModelCatalogClient（provider 模块 `provider/catalog/sync/`）

- 职责：拉取并解析 models.dev 模型数据，对上层屏蔽数据源细节。
- 技术：Spring `RestClient`，超时 30s，连接失败抛 `CatalogSyncException`。
- 配置（`@ConfigurationProperties(prefix = "gateway.catalog.sync")`）：
  - `url`（默认 `https://models.dev/models.json`）
  - `connect-timeout` / `read-timeout`
  - `enabled`（默认 true，可关闭数据源）
- 解析：JSON 反序列化为 `ModelCatalogDto`（忽略未知字段）。

### 6.2 ModelCatalogSyncService（provider 模块 `provider/catalog/sync/`）

`sync()` 主流程，返回 `CatalogSyncReport`（added/updated/skipped/failed 计数 + 明细）：

1. **拉取**：`ModelCatalogClient.fetch()`；失败时抛异常并由上层记录失败审计（旧数据不动）。
2. **模型 upsert**（按 `external_id` 匹配，次选 `model_name` 匹配）：
   - 新增：`modelName = canonical ID 末段`，映射能力/限额/元信息字段（§5），`source=MODELS_DEV`，写入 external_id。
   - 已存在：更新元信息字段，**跳过人工锁定字段**（§7）；为存量模型补写 external_id（首次同步建立映射）。
   - **不删除** models.dev 未出现的现有模型（保留旧模型，避免破坏渠道/路由/限额）。
3. **记录同步日志**：每次同步写入 `catalog_sync_logs` 表（结果、各计数、消息、触发时间），供 `GET status` 与前端展示最近同步状态。

### 6.3 CatalogSyncController（gateway-web）

| 端点 | 能力 |
|------|------|
| `POST /api/v1/catalog/sync` | 触发手工同步，返回 `CatalogSyncReport` |
| `GET /api/v1/catalog/sync/status` | 查询最近同步状态（时间、结果、来源） |

实现：新增 `CatalogSyncController`，委托 `ModelCatalogSyncService.sync()`；现有 `CatalogSyncFacade.syncBuiltin()`（BUILTIN 种子重载）与 `POST /provision/sync/builtin` 保持兼容不动。

### 6.4 审计

同步属于管理操作，`POST /api/v1/catalog/sync` 在现有 `AuditLogInterceptor` 的审计范围（`/api/v1/**` 的 POST/PUT/PATCH/DELETE）内，自动落审计日志（谁、何时、成败）。同步结果明细由 `catalog_sync_logs` 表承载，二者互补。

## 7. 人工编辑保护

- models 表新增 `locked_fields` jsonb：记录管理员手动编辑过的字段名集合。
- 管理 API（ModelController 的 update）写入时自动把变更字段加入 `locked_fields`。
- 同步 upsert 时跳过 `locked_fields` 中的字段，避免覆盖人工修改。
- 同步新增的记录 `locked_fields` 为空（全量可覆盖）。

## 8. 前端设计（gateway-console）

1. **Models 页**：模型详情/列表展示新增字段（description、release_date、license、open_weights、benchmarks）。
2. **Catalog 页**：新增"模型目录同步"区域——显示最近同步时间/结果，"同步模型目录"按钮（调 `POST /api/v1/catalog/sync`，成功后刷新）。

## 9. 测试计划（TDD）

| 层 | 测试 |
|----|------|
| 客户端 | ModelCatalogClient 拉取/解析（mock HTTP）、超时/失败处理 |
| 映射 | models.json → Model 转换（canonical ID 末段、能力/限额/元信息映射） |
| 服务 | ModelCatalogSyncService：upsert 幂等、人工锁定字段保护、不删除旧模型、报告统计 |
| Web | CatalogSyncController 集成测试（同步触发 + 状态查询） |
| 日志 | CatalogSyncLog 实体/Repository 往返与 findLatest |

## 10. 风险与对策

| 风险 | 对策 |
|------|------|
| models.json 拉取失败/超时 | 超时+重试；失败保留旧数据，记录失败审计 |
| 模型名冲突（canonical ID 末段相同） | 按 external_id 精确匹配优先；冲突时跳过并计入报告 |
| 存量 75 模型映射不完全 | 不删除旧模型；按 modelName/displayName 尝试匹配补 external_id，匹配不到仅保留 |
| 人工修改被覆盖 | locked_fields 保护（§7） |
| 数据源字段与现有语义差异 | 能力/限额字段映射对齐现有列与 capabilities 键名（§5） |

## 11. 实施范围（模块落位）

| 模块 | 变更 |
|------|------|
| gateway-provider/provider | Model 实体扩展、ModelCatalogClient、ModelCatalogSyncService、CatalogSyncLog、配置属性 |
| gateway-provider/provider-data | ModelDo 扩展、CatalogSyncLogDo + Repository |
| gateway-boot | V69 迁移（models 表加列 + catalog_sync_logs 建表） |
| gateway-web | CatalogSyncController、Model DTO 扩展 |
| gateway-console | Models 字段展示 + Catalog 同步区域 |
| gateway-coverage | 新代码测试覆盖（域服务 ≥90%） |

## 12. 里程碑

1. **M1 数据模型**：V69 迁移 + Model/ModelDo 扩展 + 测试
2. **M2 同步内核**：ModelCatalogClient + ModelCatalogSyncService + 映射 + 测试
3. **M3 接口与审计**：CatalogSyncController + DTO + 审计事件 + 测试
4. **M4 前端**：控制台同步入口 + Models 字段展示
5. **M5 验证**：手工同步全流程验证（首次全量 + 二次幂等 + 人工锁定保护）

## 13. 后续阶段（不在本期）

厂商信息完善（providers 表 env / api_compatibility / default_endpoint）与厂商-模型供应关系表 `model_supplies`（含参考定价）留待后续独立设计与实施。
