# 快速开始页面改进设计

## 背景

当前快速开始页面存在以下问题：
- 模型卡片点击无响应（`onSelect` 未使用）
- 代码示例仅支持 OpenAI 协议，缺少 Anthropic 协议示例
- 缺少在线试玩能力，开发者无法在页面上验证连通性
- API Key 管理与代码示例之间无联动
- 新用户缺少引导

## 目标用户

全栈开发者，既用 OpenAI SDK 也用 Anthropic SDK，需要两种协议的代码示例和快速验证能力。

## 设计方案：渐进式布局

单栏纵向布局，按信息优先级自然排列，3 个独立但联动的区块。不强制步骤顺序，兼顾新手和老手。

### 核心原则

- **禁止自定义颜色**：所有颜色必须使用 Ant Design 主题 token（`token.colorPrimary`、`token.colorSuccess`、`token.colorError` 等），确保暗色主题兼容
- **区块联动**：Key、模型、协议的选择变化自动传播到下游区块
- **双协议支持**：OpenAI 和 Anthropic 两种协议的代码示例和试玩

---

## 区块 1：API Key 快捷区

一行式设计，始终显示当前选中的 Key（脱敏），无状态区分。

### 布局

```
[🔑图标] [当前 API Key: sk-••••••••3f8a]  [复制] [切换▾] [+新建]
```

### 交互

- **复制**：调用 API 获取 keyPlain，复制到剪贴板
- **切换 ▾**：Dropdown 展示所有 Key 列表（脱敏 + 状态标签），点击切换当前 Key
- **+ 新建**：打开 KeyGenerateModal，创建成功后自动设为当前 Key
- **无 Key 时**：显示占位文本"创建 API Key 以开始调用模型"，仅显示创建按钮
- **联动**：当前 Key 变化时，区块 2 代码示例和区块 3 试玩自动更新

### 组件

- 新建 `ApiKeySelector` 组件，替代原 `DeveloperKeyList` 表格
- 复用现有 `KeyGenerateModal`

---

## 区块 2：模型选择 + 代码示例

### 模型卡片网格

- 3 列响应式网格（xs:24, sm:12, md:8）
- 每张卡片显示：displayName、modelName、上下文窗口、能力标签
- 点击选中：蓝色边框高亮 + ✓ 标记（使用 `token.colorPrimary`）
- 搜索框：按 displayName 或 modelName 过滤
- 仅显示 `state === 'ACTIVE'` 的模型

### 代码示例区

工具栏包含：
- **协议切换**：OpenAI / Anthropic（Segmented 组件）
- **语言切换**：cURL / Python / Node.js / Java（Segmented 组件）
- **模型选择器**：Select 下拉，列出所有可用模型
- **复制按钮**

代码内容区：
- 深色背景（`token.colorBgElevated` 或固定深色主题），等宽字体
- Key 使用明文（非脱敏），模型名使用当前选中模型

### 双向联动

- 点击模型卡片 → 更新代码区模型下拉
- 代码区模型下拉切换 → 卡片网格同步高亮
- 协议切换 → 代码模板、端点路径、认证头全部切换

### Anthropic 协议特殊处理

- 端点：`/anthropic/v1/messages`（而非 `/v1/chat/completions`）
- 认证头：`x-api-key: {key}`（而非 `Authorization: Bearer {key}`）
- 额外头：`anthropic-version: 2023-06-01`
- 请求体：需 `max_tokens` 字段，`messages` 格式与 OpenAI 一致

### 代码模板

每种协议 × 每种语言 = 8 个模板。模板函数签名：

```ts
type Protocol = 'openai' | 'anthropic';
type Lang = 'curl' | 'python' | 'node' | 'java';

const snippets: Record<Protocol, Record<Lang, (url: string, key: string, model: string) => string>>;
```

### 组件

- 改造 `ModelCard`：增加 `selected` 和 `onSelect` props
- 改造 `CodeSnippet`：增加 `protocol` 和 `model` props，扩展模板为双协议
- 删除原 `DeveloperKeyList`（被区块 1 的 `ApiKeySelector` 替代）

---

## 区块 3：轻量试玩

### 布局

```
[试玩]                                    [模型: gpt-4o · 协议: OpenAI]
[输入框：输入测试消息...]
[▶ 发送]  [✓ 流式]                        [使用上方选中的 Key 和模型]

--- 请求后 ---

[响应预览区：流式逐字输出 / JSON 格式化]
[输入: 12 tokens · 输出: 18 tokens]       [耗时: 0.8s]
```

### 交互

- **联动**：自动使用区块 1 的 Key + 区块 2 的模型和协议
- **流式开关**：默认开启
  - 开启：SSE 逐字输出，带光标动画
  - 关闭：等完整响应后 JSON 格式化展示
- **发送按钮**：请求中变为"停止"按钮（使用 `token.colorError`），可中断请求（AbortController）
- **Token 统计**：请求完成后显示输入/输出 Token 数和耗时
- **错误处理**：认证失败、模型不可用、网络错误等，使用 `Result` 组件或 Alert 展示（使用 `token.colorError`）
- **无 Key 时**：发送按钮 disabled，Tooltip 提示"请先创建 API Key"
- **协议适配**：
  - OpenAI：POST `/v1/chat/completions`，`Authorization: Bearer {key}`
  - Anthropic：POST `/anthropic/v1/messages`，`x-api-key: {key}`，`anthropic-version: 2023-06-01`

### 组件

- 新建 `PlaygroundPanel` 组件
- 内部管理请求状态（idle / loading / streaming / done / error）
- 使用 `fetch` + `ReadableStream` 处理 SSE 流式响应
- 使用 `AbortController` 支持中断

---

## 状态管理

页面级状态提升到 `Quickstart/index.tsx`：

```ts
const [currentKey, setCurrentKey] = useState<string>();      // 当前 Key 明文
const [currentKeyId, setCurrentKeyId] = useState<number>();   // 当前 Key ID
const [selectedModel, setSelectedModel] = useState<string>(); // 当前模型 modelName
const [protocol, setProtocol] = useState<Protocol>('openai'); // 当前协议
```

各区块通过 props 接收和回调更新这些状态。

---

## 翻译

扩展 `quickstart.json`，新增以下 key：

- `apiKey.current` / `apiKey.copy` / `apiKey.switch` / `apiKey.create` / `apiKey.placeholder`
- `protocol.openai` / `protocol.anthropic`
- `playground.title` / `playground.send` / `playground.stop` / `playground.streaming` / `playground.stream` / `playground.placeholder` / `playground.noKey` / `playground.done` / `playground.failed` / `playground.inputTokens` / `playground.outputTokens` / `playground.duration`
- `model.selected` / `model.select`

---

## 文件变更清单

| 操作 | 文件 | 说明 |
|------|------|------|
| 新建 | `Quickstart/ApiKeySelector.tsx` | API Key 快捷选择器 |
| 新建 | `Quickstart/PlaygroundPanel.tsx` | 轻量试玩面板 |
| 改造 | `Quickstart/index.tsx` | 页面状态管理 + 3 区块布局 |
| 改造 | `Quickstart/ModelCard.tsx` | 增加 selected/onSelect |
| 改造 | `Quickstart/CodeSnippet.tsx` | 双协议 + 模型选择器 + 双向联动 |
| 保留 | `Quickstart/KeyGenerateModal.tsx` | 无需改动 |
| 删除 | `Quickstart/DeveloperKeyList.tsx` | 被 ApiKeySelector 替代 |
| 更新 | `locales/zh-CN/quickstart.json` | 新增翻译 key |
| 更新 | `locales/en-US/quickstart.json` | 新增翻译 key |
