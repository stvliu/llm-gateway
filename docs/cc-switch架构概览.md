# CC Switch 架构概览

> **项目**: CC Switch v3.14.1 — 桌面端管理工具，统一管理 Claude Code、Codex、Gemini CLI、OpenCode、OpenClaw 五个 CLI 工具的配置、MCP、Skills、供应商和用量。

## 技术栈

| 层 | 技术 |
|---|---|
| 桌面壳层 | **Tauri 2** (Rust + WebView) |
| 前端 | **React 18** + TypeScript + Vite |
| UI 组件 | shadcn/ui (Radix + Tailwind CSS) + Framer Motion |
| 状态管理 | @tanstack/react-query (服务端状态) + React Context (主题/更新) |
| 国际化 | i18next + react-i18next |
| 后端语言 | **Rust** (edition 2021, rquickjs 嵌入式 JS 引擎) |
| 数据库 | **SQLite** (rusqlite, bundled, 支持备份/hooks) |
| HTTP 代理 | **Axum 0.7** + Hyper 1.0 (自建代理服务器) |
| 序列化 | serde_json (preserve_order), toml_edit, serde_yaml, json5 |
| 其他 | reqwest (HTTP 客户端), regex, zip, tokio, arboard (剪贴板) |

## 整体架构（4 层）

```
┌──────────────────────────────────────────────────────┐
│                  前端 (React SPA)                      │
│  src/components/    src/hooks/      src/lib/api/       │
│  (页面/组件)         (业务 Hook)      (Tauri命令绑定)    │
├──────────────────────────────────────────────────────┤
│              Tauri IPC (invoke / listen)               │
├──────────┬───────────────────────────────────────────┤
│ Rust 命令层 │            Rust 服务层                     │
│ commands/  │  services/ (provider/mcp/skill/proxy...)  │
├──────────┴───────────────────────────────────────────┤
│                  Rust 基础设施                          │
│  proxy/ (本地代理服务器)   database/ (SQLite)  config/   │
│  session_manager/           tray/ (系统托盘)            │
│  settings/                  deeplink/                  │
└──────────────────────────────────────────────────────┘
```

## 前端架构

### 入口

`src/main.tsx` → `App.tsx`

### 视图系统

无客户端路由库，使用 React 状态驱动的视图切换：

```typescript
type View =
  | "providers"      // 供应商列表（首页）
  | "settings"       // 设置页
  | "mcp"            // MCP 统一管理
  | "skills"         // Skills 发现/管理
  | "prompts"        // Prompt 管理
  | "sessions"       // 会话管理
  | "agents"         // Agents 配置
  | "universal"      // 通用供应商面板
  | "workspace"      // 工作区文件
  | "openclawEnv/Tools/Agents"  // OpenClaw 子页面
  | "hermesMemory"   // Hermes 记忆
```

### App 切换器

顶栏 `AppSwitcher` 切换当前管理的目标应用：`claude` / `codex` / `gemini` / `opencode` / `hermes` / `openclaw`。全局覆盖模式下选择 `universal` 视图。

### 组件目录结构

| 目录 | 职责 |
|---|---|
| `components/providers/` | 供应商 CRUD (ProviderList, ProviderCard, AddProviderDialog) |
| `components/mcp/` | MCP 服务器管理 (UnifiedMcpPanel, McpFormModal, McpWizardModal) |
| `components/skills/` | Skills 发现/安装 (SkillsPage, UnifiedSkillsPanel, RepoManager) |
| `components/settings/` | 设置 (代理、代理配置、备份、WebDAV、外观等) |
| `components/sessions/` | 会话浏览/管理 (SessionManagerPage) |
| `components/proxy/` | 代理开关、故障转移开关 |
| `components/openclaw/` | OpenClaw 环境变量、工具、代理默认值 |
| `components/hermes/` | Hermes 记忆面板 |
| `components/workspace/` | 工作区文件 (DailyMemoryPanel, WorkspaceFilesPanel) |

### 数据流

React Query (`@tanstack/react-query`) 封装 `lib/api/` 中的 Tauri `invoke` 调用，hooks (`useProviderActions`, `useSkills`, `useSettings` 等) 封装业务逻辑。

## 后端 (Rust) 架构

### 入口 (`main.rs` → `lib.rs`)

`cc_switch_lib::run()` 组装 Tauri Builder:
1. 初始化 SQLite 数据库 (自动迁移)
2. 注册全局 Tauri 命令 (commands/mod.rs)
3. 设置系统托盘 (tray.rs)
4. 设置深链接 (deeplink)
5. 启动代理服务器 (proxy/server.rs)
6. 设置 WebDAV 自动同步
7. 启动 Gemini shadow store

### 模块划分

| 模块 | 职责 |
|---|---|
| **commands/** (30+ 文件) | Tauri 命令处理，对前端暴露 invoke 接口 |
| **services/** | 业务逻辑服务层 (provider, mcp, skill, proxy, webdav 等) |
| **proxy/** (~33 文件, 5000+ 行) | 核心代理引擎 |
| **database/** | SQLite 持久化 (schema, dao, backup, migration) |
| **config/** | CLI 工具专属配置读写 |
| **session_manager/** | 会话扫描/解析/删除 |

### 核心代理引擎 (proxy/)

```
proxy/server.rs              ← Axum HTTP 服务器
proxy/handlers.rs            ← 路由处理器
proxy/forwarder.rs           ← 请求转发到上游 (100KB+, 最复杂)
proxy/provider_router.rs     ← 按 provider 路由选择
proxy/providers/             ← 各供应商特定处理 (Anthropic, OpenAI, Google, AWS 等)
proxy/response_processor.rs  ← 响应流处理/修整
proxy/circuit_breaker.rs     ← 熔断器
proxy/failover_switch.rs     ← 故障转移
proxy/thinking_rectifier/optimizer ← 思考预算修正
proxy/sse.rs                 ← SSE 流处理
proxy/session.rs             ← 代理会话追踪
proxy/usage/                 ← 用量统计集成
```

### 数据持久化

SQLite 数据库 (位置: `~/.cc-switch/cc-switch.db`)

**核心表**:

| 表 | 用途 |
|---|---|
| `providers` | 供应商配置 (复合主键: id + app_type) |
| `provider_endpoints` | 端点候选项 |
| `mcp_servers` | MCP 服务器 (支持多应用启用标记) |
| `prompts` | Prompt 模板 |
| `skills` | 已安装 skills |
| `skill_repos` | Skills 仓库源 |
| `settings` | KV 设置 |

数据库变更 Hook 自动触发 WebDAV 同步。

### 配置映射关系

CC Switch 统一管理 5 个 CLI 工具的配置格式:

| 工具 | 关键配置文件 | Rust 模块 |
|---|---|---|
| **Claude Code** | `~/.claude/settings.json`, `~/.claude.json` | `claude_desktop_config.rs` |
| **Codex** | `~/.codex/config.json`, `~/.codex/auth.json` | `codex_config.rs` |
| **Gemini CLI** | `~/.gemini/config.toml` | `gemini_config.rs` |
| **OpenCode** | `~/.opencode/opencode.json` | `opencode_config.rs` |
| **OpenClaw** | `~/.openclaw/config.toml` | `openclaw_config.rs` |
| **Hermes** | `~/.hermes` | `hermes_config.rs` |

### 系统托盘 (tray.rs)

- 托盘菜单显示所有供应商，支持即时切换
- 后台运行时保持代理运行
- macOS 特定: 隐藏 dock 图标模式

### 错误处理

自定义 `AppError` 枚举，包含 `Config`、`Database`、`Json`、`Toml`、`HttpStatus`、`Localized` 等变体，通过 `thiserror` 派生。全局 panic hook (`panic_hook.rs`) 将崩溃写入 `~/.cc-switch/crash.log`。

## 核心业务流程

### 供应商切换流程

1. 用户在前端选择供应商 → `invoke("switch_provider", { id, appId })`
2. 后端 `ProviderService.switch_provider()` 将供应商设置写入对应 CLI 的配置文件中
3. 更新数据库当前供应商状态
4. 向前端发送 `provider-switched` 事件 → 更新 UI

### 代理请求流程

1. CLI 工具配置指向 `localhost:PORT`（CC Switch 代理端口）
2. Axum 服务器接收请求 → `handlers::proxy_handler()`
3. `ProviderRouter` 选择当前激活的供应商
4. `forwarder` 转发请求到上游 API（支持 HTTP/HTTPS/SOCKS5）
5. `response_processor` 处理响应（SSE 流、思考预算修正等）
6. 用量统计记录

### MCP 同步流程

1. 从各工具的 MCP 配置文件中扫描/导入 MCP 服务器
2. 统一存储在 SQLite `mcp_servers` 表 + 多应用启用标记
3. 任何更改 → 双向同步回各工具的 MCP 配置文件