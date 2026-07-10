## Task 2: 文档迁移

**Files:**
- Create: `site/src/content/docs/guide/spec.mdx`（源 `docs/spec.md`）
- Create: `site/src/content/docs/guide/constitution.mdx`（源 `docs/constitution.md`）
- Create: `site/src/content/docs/api/api-spec.mdx`（源 `docs/api-spec.md`）
- Create: `site/src/content/docs/architecture/technical.mdx`（源 `docs/技术架构.md`）
- Create: `site/src/content/docs/architecture/application.mdx`（源 `docs/应用架构.md`）
- Create: `site/src/content/docs/architecture/data.mdx`（源 `docs/数据架构.md`）
- Create: `site/src/content/docs/architecture/information.mdx`（源 `docs/信息架构.md`）
- Create: `site/src/content/docs/features/index.mdx`（源 `docs/AI-Gateway功能特性.md`）
- Create: `site/src/content/docs/features/routing.mdx`（源 `docs/routing-design.md`）
- Create: `site/src/content/docs/features/resilience.mdx`（源 `docs/容灾方案设计.md`）
- Create: `site/src/content/docs/features/model-plaza.mdx`（源 `docs/model-experience-design.md`）
- Create: `site/src/content/docs/features/semantic-cache.mdx`（企业版占位，提炼自 README）
- Create: `site/src/content/docs/features/mcp-protocol.mdx`（企业版占位，提炼自 README）
- Create: 7 篇能力域概览页（`api-gateway`/`provider-management`/`auth`/`apikey-management`/`token-quota`/`security`/`observability`，内容来自 README 对应章节）

**迁移映射表（Design Doc §5，逐篇严格对照）：**

| 现有 `docs/` 文件 | 目标路径 | frontmatter title |
|---|---|---|
| `spec.md` | `guide/spec.mdx` | 需求规格 |
| `constitution.md` | `guide/constitution.mdx` | 架构章程 |
| `api-spec.md` | `api/api-spec.mdx` | API 参考 |
| `技术架构.md` | `architecture/technical.mdx` | 技术架构 |
| `应用架构.md` | `architecture/application.mdx` | 应用架构 |
| `数据架构.md` | `architecture/data.mdx` | 数据架构 |
| `信息架构.md` | `architecture/information.mdx` | 信息架构 |
| `AI-Gateway功能特性.md` | `features/index.mdx` | 功能特性 |
| `routing-design.md` | `features/routing.mdx` | 路由设计 |
| `容灾方案设计.md` | `features/resilience.mdx` | 容灾方案 |
| `model-experience-design.md` | `features/model-plaza.mdx` | 模型广场 |

- [ ] **Step 1: 创建 `site/src/content/docs/guide/spec.mdx`**

将 `docs/spec.md` 全文内容复制到 `site/src/content/docs/guide/spec.mdx`，并在文件**最顶部**插入 Starlight frontmatter：

```
---
title: 需求规格
description: LLM-Gateway 完整需求规格说明书
---
```

其后粘贴 `docs/spec.md` 正文。原 `docs/spec.md` 保留不删。

- [ ] **Step 2: 对其余 10 篇迁移文档重复 Step 1 的流程**

每篇操作一致：复制源文件正文 -> 新建目标 `.mdx` -> 顶部插入 frontmatter（title/description 见映射表）-> 原 `docs/` 文件保留。具体：

| 目标文件 | 源文件 | frontmatter |
|---|---|---|
| `guide/constitution.mdx` | `docs/constitution.md` | `title: 架构章程` / `description: 架构设计铁律` |
| `api/api-spec.mdx` | `docs/api-spec.md` | `title: API 参考` / `description: OpenAI 与 Anthropic 双 API 标准` |
| `architecture/technical.mdx` | `docs/技术架构.md` | `title: 技术架构` / `description: 分层架构与技术选型` |
| `architecture/application.mdx` | `docs/应用架构.md` | `title: 应用架构` / `description: 应用层用例编排` |
| `architecture/data.mdx` | `docs/数据架构.md` | `title: 数据架构` / `description: 数据库设计与实体关系` |
| `architecture/information.mdx` | `docs/信息架构.md` | `title: 信息架构` / `description: 信息组织与导航结构` |
| `features/index.mdx` | `docs/AI-Gateway功能特性.md` | `title: 功能特性` / `description: 全部能力域功能总览` |
| `features/routing.mdx` | `docs/routing-design.md` | `title: 路由设计` / `description: 智能路由与降级策略` |
| `features/resilience.mdx` | `docs/容灾方案设计.md` | `title: 容灾方案` / `description: 熔断重试与故障转移` |
| `features/model-plaza.mdx` | `docs/model-experience-design.md` | `title: 模型广场` / `description: 模型展示与体验设计` |

- [ ] **Step 3: 创建企业版占位文档 `features/semantic-cache.mdx`**

内容提炼自 README §语义缓存，frontmatter + 正文如下：

```
---
title: 语义缓存
description: 相似请求命中缓存，降低成本 30%+
badge: 企业版
---

语义缓存是企业版专属能力，基于向量相似度匹配相似请求并返回缓存结果。

## 核心能力

- **相似请求返回缓存**：语义级别匹配，降低成本 30%+
- **缓存 TTL 配置**：可设置缓存过期时间
- **缓存命中率统计**：监控缓存效果
- **向量相似度搜索**：基于 pgvector 实现

> 此功能为企业版专属，标准版默认关闭（`semantic-cache: false`）。
```

- [ ] **Step 4: 创建企业版占位文档 `features/mcp-protocol.mdx`**

内容提炼自 README §MCP 协议：

```
---
title: MCP 协议
description: Model Context Protocol 支持（Resources/Prompts/Tools）
badge: 企业版
---

MCP（Model Context Protocol）是企业版专属能力，为大模型提供标准化上下文与工具接入。

## 核心能力

- **Resources**：提供上下文数据
- **Prompts**：提供预定义的提示模板
- **Tools**：提供可调用的工具函数

> 此功能为企业版专属，标准版默认关闭（`mcp-protocol: false`）。
```

- [ ] **Step 5: 创建 7 篇能力域概览页**

为满足 spec「8 能力域全部条目可见」，为缺少独立迁移文档的 7 个能力域各创建一篇概览页，内容来自 README 功能矩阵对应章节（README 第 41-118 行已含每域 ✅/🔒 清单）。路由域用已迁移的 `features/routing.mdx`，无需新建。

每篇概览页使用统一 frontmatter 模板，正文为该域功能清单（Markdown 表格，标准版 ✅ / 企业版 🔒 列）。

示例——创建 `site/src/content/docs/api-gateway.mdx`：

```
---
title: API 网关
description: OpenAI 与 Anthropic 双标准 API 兼容端点
---

LLM-Gateway 同时支持 OpenAI 和 Anthropic 两种 API 标准，统一接入。

## 功能清单

| 功能 | 标准版 | 企业版 |
|---|---|---|
| OpenAI 兼容端点（`/v1/chat/completions`、`/v1/completions`） | ✅ | ✅ |
| Anthropic 兼容端点（`/v1/messages`） | ✅ | ✅ |
| SSE 流式转发（首 token ≤100ms） | ✅ | ✅ |
| 协议转换（OpenAI ↔ Anthropic 互转） | ✅ | ✅ |
| 图像生成端点（`/v1/images/generations`） | ✅ | ✅ |
| 语音合成端点（`/v1/audio/speech`） | ✅ | ✅ |
| 语音识别端点（`/v1/audio/transcriptions`） | ✅ | ✅ |
| 内容审核端点（`/v1/moderations`） | ✅ | ✅ |

详见 [API 参考](/api/api-spec/)。
```

按相同模板创建其余 6 篇，正文数据取自 README 对应章节：

| 目标文件 | title | README 章节（行号） |
|---|---|---|
| `provider-management.mdx` | Provider 管理 | 第 53-62 行 |
| `auth.mdx` | 用户与认证 | 第 71-75 行 |
| `apikey-management.mdx` | 密钥管理 | 第 77-82 行 |
| `token-quota.mdx` | Token 计量与配额 | 第 84-88 行 |
| `security.mdx` | 安全与风控 | 第 90-99 行 |
| `observability.mdx` | 可观测性 | 第 101-107 行 |

每篇正文为「## 功能清单」表格，列名「功能 / 标准版 / 企业版」，行数据逐条来自 README 该章节的 ✅/🔒 项（✅ 对应两列均 ✅，🔒 [企业版] 对应标准版 ❌ / 企业版 ✅）。

- [ ] **Step 6: 复核内部调研文档未进入公开站点**

检查 `site/src/content/docs/` 下不存在以下内部调研文档：`apipark`、`voapi`、`cc-switch`、`FEASIBILITY`、`竞品分析`、`simulator-gateway-verification`、`connectivity-test-design`、`前端重构规划`、`页面设计规范`、`需求实现规划`、`Speckit`、`git-workflow`、`migration/`、`refactor/`、`db/`。

Run:
```bash
cd site && ls src/content/docs/ && echo "---" && find src/content/docs -type f | wc -l
```
Expected: 文件数 = 11 迁移 + 2 企业版占位 + 7 概览 = 20 个 `.mdx`，且输出中无上述内部调研文档名。

- [ ] **Step 7: 验证 Starlight 能解析迁移文档**

Run:
```bash
cd site && pnpm dev
```
Expected: 开发服务启动，访问 `http://localhost:4321/`，Starlight 文档站可渲染（侧边栏此时可能为空，因 Task 3 才配置；但文档文件本身不报构建错误）。Ctrl+C 停止。

- [ ] **Step 8: Commit**

```bash
git add site/src/content/docs/
git commit -m "feat(site): 迁移 11 篇核心文档+2 篇企业版占位+7 篇能力域概览"
```

---

