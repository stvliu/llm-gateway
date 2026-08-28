# 模型/厂商/供应关系目录完善设计（models.dev 同步）

> 日期：2026-08-28
> 状态：已确认（手工同步版方案 A）
> 功能目标：**模型、厂商、关系数据及时准确**

## 1. 背景与目标

llm-gateway 当前的厂商/模型目录为**静态人工维护**的种子 JSON（`gateway-boot/src/main/resources/catalog/*.json`，19 家厂商、75+ 模型），严重滞后于业界（仍停留在 gpt-4o / claude-3.5 时代），且缺少结构化能力/限额/定价/许可证等元数据，厂商-模型-定价的供应关系缺位。

[models.dev](https://models.dev)（Vercel 维护的公开模型目录，数据源 `https://models.dev/api.json`）提供权威、完整的模型目录。本设计引入 models.dev 数据作为**参考目录数据源**，通过**手工触发同步**将模型、厂商、供应关系数据落地，实现"及时准确"。

### 目标

1. **模型信息完善**：models 表补齐 description、release_date、last_updated、license、open_weights、benchmarks、weights、外部 ID 等字段；能力字段（reasoning / tool_call / structured_output / vision）结构化映射。
2. **厂商信息完善**：providers 表补齐 env（接入所需密钥变量）、api_compatibility（API 兼容性）、source、last_synced_at、default_endpoint（参考端点）。
3. **供应关系落地**：新增 `model_supplies` 表承载"厂商 × 模型 × 参考定价"三元关系（上游模型名、input/output/cache_read 参考价）。
4. **手工同步机制**：内置 models.dev 数据源适配器，管理员通过控制台/API 手动触发同步，增量 upsert、变更审计、人工编辑保护。

### 非目标

- ❌ 不做定时自动同步（用户已确认改为手工同步）
- ❌ 不把 models.dev 定价写入 plan_catalogs（保持套餐只读架构）
- ❌ 不把 base URL / 密钥写回 providers 表（架构约束：端点下沉 channel_endpoints、密钥下沉 channel_credentials；models.dev 的 api/env 仅作参考元信息）
- ❌ 同步不自动创建渠道/模型实例（全量目录 + 管理员按需开通）
- ❌ 不删除现有旧模型（避免破坏已有渠道、路由、token 限额）

## 2. 数据源分析（models.dev）

`https://models.dev/api.json`（约 4.3MB）为厂商维度视图，与 `catalog.json` / `models.json` 一致（已核验）：

| 层级 | 规模 | 结构 |
|------|------|------|
| 厂商主数据 | 204 个 | `id`、`name`、`env`（密钥变量数组）、`npm`（SDK 包名，可推断 API 兼容性）、`api`（base URL，可含 `${ENV}` 占位符）、`doc` |
| 模型主数据 | 361 个（models.json） | `id`（canonical，如 `openai/gpt-4o`）、`name`、`description`、`family`、`attachment`、`reasoning`、`reasoning_options`、`tool_call`、`structured_output`、`temperature`、`knowledge`、`release_date`、`last_updated`、`modalities`、`open_weights`、`limit`(context/output/input)、`cost`(input/output/cache_read)、`status`、`weights`、`benchmarks`、`license` |
| 供应关系 | 7372 条 | 模型嵌套在厂商 `models` 下，每条含厂商特定 `cost` 定价与 `limit` |

**映射可行性结论**（已核验）：
- canonical ID 末段即通用模型名（`deepseek/deepseek-v4-flash` → `deepseek-v4-flash`），可作为默认 modelName。
- 现有种子的旧模型名（如 `deepseek-chat` = V3 时代）在 models.dev 官方厂商下已不存在（官方已是 `deepseek-v4-*`）——同步不删除旧模型，仅新增新模型。
- 同一模型跨厂商定价不同（如 gpt-4o 在 OpenAI 官方与第三方定价不同）——供应定价必须按"厂商×模型"粒度。

## 3. 现状与差距

| 维度 | 现状 | 目标 | 差距 |
|------|------|------|------|
| 厂商数量/信息 | 19 家；code/name/logo/website/desc/apiDocUrl/priority | 204 家；+env、api_compatibility、source、last_synced_at、default_endpoint | 数量级 + 元信息 |
| 模型数量/信息 | 75+；name/displayName/family/context/limits/knowledge/capabilities(Map)/modalities | 361+；+description/release_date/last_updated/license/open_weights/benchmarks/weights/external_id | 数量级 + 元信息 |
| 供应关系 | 无（仅 plan_catalogs 套餐驱动） | model_supplies 表承载厂商×模型×参考价 | 全新 |
| 同步机制 | 静态种子 JSON，启动表空加载 + forceReload | 手工触发 + models.dev 数据源 + 增量 upsert + 审计 | 全新 |

## 4. 方案总览（方案 A：手工同步）

```
[管理员] ──控制台按钮/API──▶ ModelCatalogSyncController (web)
                                    │
                                    ▼
                    ModelCatalogSyncService (provider.catalog.sync)
                    ① 拉取  ② 转换  ③ 三阶段 upsert
                                    │
              ┌─────────────┬───────┴────────┬─────────────┐
              ▼             ▼                ▼             ▼
        providers 表   models 表      model_supplies 表   AuditEvent
        (+参考信息)    (+模型元信息)   (厂商×模型×参考价)    (审计闭环)
              ▲
              │
    ModelCatalogClient (provider.catalog.sync)
    └── RestClient 拉取 https://models.dev/api.json
```

## 5. 数据模型设计

### 5.1 providers 表新增列（V69 迁移 + Provider.java + ProviderDo.java）

| 列 | 类型 | 说明 |
|----|------|------|
| `env` | jsonb | 接入所需环境变量数组（models.dev `env`，如 `["OPENAI_API_KEY"]`），纯元信息，不含密钥值 |
| `api_compatibility` | varchar(32) | API 兼容性枚举：`OPENAI_COMPATIBLE` / `ANTHROPIC` / `GEMINI` / `NATIVE` / `UNKNOWN`，由 models.dev `npm` 字段推断（`@ai-sdk/openai-compatible` → OPENAI_COMPATIBLE 等） |
| `default_endpoint` | varchar(512) | models.dev 提供的默认 base URL（仅作开通渠道时预填 channel_endpoints 的建议值，不参与运行时） |
| `source` | varchar(32) | 数据来源：`MODELS_DEV` / `BUILTIN` / `MANUAL`，默认 `MANUAL` |
| `last_synced_at` | timestamp | 最近一次同步时间 |
| `locked_fields` | jsonb | 人工锁定字段名集合（§7），空表示无锁定 |

> `api_compatibility` 为独立枚举 `ApiCompatibility`（`OPENAI_COMPATIBLE` / `ANTHROPIC` / `GEMINI` / `NATIVE` / `UNKNOWN`），与协议层 `Protocol` 枚举（OPENAI/ANTHROPIC/GEMINI/NATIVE，表示端点协议类型）语义不同：前者描述厂商 API 的兼容面，后者是渠道端点实际使用的协议，二者不互相替代。

### 5.2 models 表新增列（V69 迁移 + Model.java + ModelDo.java）

| 列 | 类型 | 说明 |
|----|------|------|
| `description` | text | 模型描述 |
| `release_date` | date | 发布日期 |
| `last_updated` | date | 最后更新日期 |
| `license` | varchar(128) | 许可证（如 MIT） |
| `open_weights` | boolean | 是否开源权重 |
| `benchmarks` | jsonb | 基准测试数组 `[{name, score, metric, source}]` |
| `weights` | jsonb | 权重/模型卡片链接数组 `[{label, url}]` |
| `source` | varchar(32) | 数据来源（同上） |
| `external_id` | varchar(256) | models.dev canonical ID（如 `openai/gpt-4o`），同步幂等匹配键 |
| `locked_fields` | jsonb | 人工锁定字段名集合（§7），空表示无锁定 |

**能力字段映射**（不新增列，映射进现有 `capabilities` Map，键名对齐现有前端语义）：
- `attachment` → `capabilities["vision"]`
- `reasoning` → `capabilities["reasoning"]`
- `tool_call` → `capabilities["function_calling"]`
- `structured_output` → `capabilities["structured_output"]`
- `temperature`（false）→ 不入 Map
- `modalities`（input/output 合并去重）→ 现有 `modalities` 列

### 5.3 新增 model_supplies 表（V69 迁移 + ModelSupply.java + ModelSupplyDo.java）

承载"厂商 × 模型 × 参考定价"供应关系：

| 列 | 类型 | 说明 |
|----|------|------|
| `id` | bigint PK | |
| `provider_code` | varchar(64) | → providers.provider_id |
| `model_name` | varchar(128) | → models.model_name |
| `upstream_model_name` | varchar(256) | 厂商侧实际模型名（models.dev 模型 ID 或厂商 API 接受的名称），供开通渠道时预填 ModelInstance.upstreamModelName |
| `input_price` | numeric(18,8) | 参考输入价（每百万 token，USD） |
| `output_price` | numeric(18,8) | 参考输出价 |
| `cache_read_price` | numeric(18,8) | 参考缓存读取价 |
| `status` | varchar(16) | `ACTIVE` / `SUSPENDED`，默认 ACTIVE（仅表示供应目录可用，与渠道/实例无关） |
| `source` | varchar(32) | 数据来源，默认 `MODELS_DEV` |
| `last_synced_at` | timestamp | 最近同步时间 |
| 审计字段 | | created_by/created_at/updated_by/updated_at（BaseEntity 约定） |

唯一约束：`uk_provider_model (provider_code, model_name)`。

## 6. 同步子系统设计

### 6.1 ModelCatalogClient（provider 模块 `provider/catalog/sync/`）

- 职责：拉取并解析 models.dev 数据，对上层屏蔽数据源细节。
- 技术：Spring `RestClient`，超时 30s，连接失败抛 `CatalogSyncException`。
- 配置（`@ConfigurationProperties(prefix = "gateway.catalog.sync")`）：
  - `url`（默认 `https://models.dev/api.json`）
  - `connect-timeout` / `read-timeout`
  - `enabled`（默认 true，可关闭数据源）
- 解析：JSON 反序列化为中间 DTO（ProviderCatalogDto / ModelCatalogDto / SupplyDto），忽略未知字段。

### 6.2 ModelCatalogSyncService（provider 模块 `provider/catalog/sync/`）

`sync()` 主流程，返回 `CatalogSyncReport`（added/updated/skipped/failed 各维度计数 + 明细）：

1. **拉取**：`ModelCatalogClient.fetch()`；失败时抛异常并由上层记录失败审计（保留旧数据不动）。
2. **阶段一：厂商 upsert**（按 `code` 匹配）：
   - 新增：写入完整厂商信息，`source=MODELS_DEV`，`priority=100`。
   - 已存在：仅更新参考字段（name/logo/description/apiDocUrl/env/api_compatibility/default_endpoint），**跳过人工锁定字段**（见 §7）；`source` 不变（保留 BUILTIN/MANUAL 标记）。
3. **阶段二：模型 upsert**（按 `external_id` 匹配，次选 `model_name` 匹配）：
   - 新增：`modelName = canonical ID 末段`，映射能力字段（§5.2），`source=MODELS_DEV`，写入 external_id。
   - 已存在：更新元信息字段，跳过人工锁定字段；为已有模型补写 external_id（存量模型首次同步建立映射）。
   - **不删除** models.dev 未出现的现有模型（保留旧模型，避免破坏渠道/路由/限额）。
4. **阶段三：供应关系 upsert**（按 `provider_code + model_name` 匹配）：
   - 新增：写入供应记录（含参考定价），`status=ACTIVE`。
   - 已存在：更新参考定价与 upstream_model_name。
   - 同步后标记 `last_synced_at`。
5. **发布事件**：`CatalogSyncCompletedEvent`（同步报告），供 audit 域采集。

### 6.3 CatalogSyncController（gateway-web）

| 端点 | 能力 |
|------|------|
| `POST /api/v1/catalog/sync` | 触发手工同步，返回 `CatalogSyncReport` |
| `GET /api/v1/catalog/sync/status` | 查询最近同步状态（时间、结果、来源） |

实现：新增 `CatalogSyncController`（gateway-web），委托 `ModelCatalogSyncService.sync()`；现有 `CatalogSyncFacade.syncBuiltin()`（BUILTIN 种子重载）与 `POST /provision/sync/builtin` 保持兼容不动，两套入口并行——前者同步 models.dev 目录，后者重载内置种子。

### 6.4 审计

触发同步属于管理操作，纳入现有 audit 闭环：发布 `CatalogSyncCompletedEvent`，audit 域采集（谁、何时、同步结果 added/updated/skipped/failed）落库，可在控制台审计页查看。

## 7. 人工编辑保护

- `providers` / `models` 新增 `locked_fields` jsonb 列：记录管理员手动编辑过的字段名集合。
- 管理 API（ProviderController / ModelController 的 update）写入时自动把变更字段加入 `locked_fields`。
- 同步 upsert 时跳过 `locked_fields` 中的字段，避免覆盖人工修改。
- 同步新增的记录 `locked_fields` 为空（全量可覆盖）。

## 8. 前端设计（gateway-console）

1. **Catalog 页**：新增"模型目录同步"区域——显示最近同步时间/结果，"同步模型目录"按钮（调 `POST /api/v1/catalog/sync`，成功后刷新）。
2. **Models 页**：模型详情/列表展示新增字段（description、release_date、license、open_weights、benchmarks、supply 价格参考）。
3. **Channels 页**：
   - 厂商信息展示 env、api_compatibility；
   - 开通渠道向导（ChannelCreateWizard）预填 default_endpoint 建议；
   - 模型接入时预填 supply.upstream_model_name 与参考定价。
4. **Catalog 浏览**：模型维度可看"可用厂商列表 + 各厂商参考价"（model_supplies）。

## 9. 测试计划（TDD）

| 层 | 测试 |
|----|------|
| 客户端 | ModelCatalogClient 拉取/解析（mock HTTP）、超时/失败处理 |
| 映射 | models.dev JSON → 实体转换（canonical ID 末段、能力字段映射、定价解析） |
| 服务 | ModelCatalogSyncService：三阶段 upsert 幂等、人工锁定字段保护、不删除旧模型、报告统计 |
| 仓库 | ModelSupplyRepository 唯一约束、按 provider+model 查询 |
| Web | CatalogSyncController 集成测试（同步触发 + 状态查询） |
| 审计 | CatalogSyncCompletedEvent 采集落库 |

## 10. 风险与对策

| 风险 | 对策 |
|------|------|
| api.json 4.3MB 拉取失败/超时 | 超时+重试；失败保留旧数据，记录失败审计 |
| 模型名冲突（canonical ID 末段相同） | 按 external_id 精确匹配优先；冲突时跳过并计入报告 |
| 7372 条供应关系同步性能 | 分批批量 upsert；首次全量，后续按 last_updated 增量 |
| 存量 75 模型映射不完全 | 不删除旧模型；已有模型按 modelName/displayName 尝试匹配补 external_id，匹配不到仅保留 |
| 人工修改被覆盖 | locked_fields 保护（§7） |
| 数据源字段与现有语义差异 | 能力字段映射对齐现有 capabilities 键名（§5.2） |

## 11. 实施范围（模块落位）

| 模块 | 变更 |
|------|------|
| gateway-provider/provider | Provider/Model 实体扩展、ModelSupply 新实体、ModelCatalogClient、ModelCatalogSyncService、配置属性、CatalogSyncCompletedEvent |
| gateway-provider/provider-data | ProviderDo/ModelDo 扩展、ModelSupplyDo + Repository |
| gateway-boot | V69 迁移（providers/models 加列 + model_supplies 建表） |
| gateway-web | CatalogSyncController、Provider/Model DTO 扩展 |
| gateway-audit | CatalogSyncCompletedEvent 采集 |
| gateway-console | Catalog 同步区域、Models 字段展示、Channels 预填 |
| gateway-coverage | 新代码测试覆盖（域服务 ≥90%） |

## 12. 里程碑

1. **M1 数据模型**：V69 迁移 + 实体/DO/Repository 扩展 + 测试
2. **M2 同步内核**：ModelCatalogClient + ModelCatalogSyncService + 映射 + 测试
3. **M3 接口与审计**：CatalogSyncController + DTO + 审计事件 + 测试
4. **M4 前端**：控制台同步入口 + 字段展示 + 预填
5. **M5 验证**：手工同步全流程验证（首次全量 + 二次幂等 + 人工锁定保护）
