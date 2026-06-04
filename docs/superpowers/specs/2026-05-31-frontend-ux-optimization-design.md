# 前端 UX 优化与渠道创建向导设计

## 元信息

| 属性 | 值 |
|------|------|
| 规范名称 | Frontend UX Optimization and Channel Creation Wizard |
| 版本 | 1.0.0 |
| 状态 | 草案 |
| 创建日期 | 2026-05-31 |
| 关联需求 | API Key 显示优化、快速开始改进、供应商目录 LOGO、渠道创建向导 |

---

## 概述

本设计涵盖以下前端 UX 优化需求：

1. **API Key 显示优化**：所有 API Key 展示场景统一使用脱敏格式，支持明文切换和复制
2. **快速开始改进**：代码卡片使用主题色，URL/API Key 自动填充真实值
3. **供应商目录 LOGO**：卡片视图显示品牌 LOGO
4. **新建渠道向导**：基于目录数据的一键创建，支持批量 API Key、端点/模型编辑

---

## 一、API Key 显示优化

### 1.1 统一脱敏组件 `MaskedKeyDisplay`

**位置**：`gateway-console/src/components/MaskedKeyDisplay.tsx`

**Props 定义**：
```typescript
interface MaskedKeyDisplayProps {
  keyMasked: string;           // 脱敏格式，如 "sk-abc****dEf1"
  keyPlain?: string;           // 明文（可选，详情 API 返回）
  mode: 'editable' | 'readonly';  // editable: 上游 Key 可编辑；readonly: 下游 Key 仅复制
  onEdit?: () => void;         // 编辑回调（editable 模式）
  showCopy?: boolean;          // 是否显示复制按钮，默认 true
  size?: 'small' | 'default';  // 尺寸
}
```

**功能**：
- 默认显示脱敏格式（如 `sk-abc****dEf1`）
- 点击眼睛图标：按需调用详情 API 获取 `keyPlain`，切换明文/脱敏显示（无需二次确认，系统为内部使用）
- 复制按钮：按需获取明文后复制到剪贴板，复制成功后 `message.success('已复制到剪贴板')`
- `editable` 模式显示编辑按钮，点击触发 `onEdit` 回调
- 明文不预加载，仅在用户主动操作时获取

**交互流程**：
```
[脱敏显示] → 点击眼睛 → [明文显示] → 点击复制 → [复制到剪贴板]
                    ↓
              再次点击眼睛 → [脱敏显示]
```

### 1.2 后端 DTO 扩展

**UserApiKeyResponse** 新增字段：
```java
String keyMasked();  // 脱敏格式
```

**ChannelCredentialResponse** 新增字段：
```java
String apiKeyMasked();  // 脱敏格式
```

### 1.3 脱敏规则

统一策略：
- 保留前 6 位 + `****` + 保留后 4 位
- 示例：`sk-abc123xyz789` → `sk-abc****z789`
- 长度不足 12 位时：仅显示前缀 + `****`
- 示例：`sk-abc` → `sk-abc****`
- 7-11 位时：保留前缀（全部已知字符）+ `****`

### 1.4 上游 Key 编辑功能

**交互方式**：弹窗替换整个 Key

**流程**：
1. 点击编辑按钮 → 弹出 Modal
2. 输入新的 API Key（Password 输入框）
3. 确认时展示变更摘要："将替换 API Key：`sk-abc****dEf1` → `sk-new****xyz1`"
4. 确认 → 调用 `PUT /api/v1/channels/{channelId}/credentials/{id}` 替换
5. 成功后刷新列表

**后端 API 扩展**：
```
PUT /api/v1/channels/{channelId}/credentials/{id}
Request Body: { "apiKey": "sk-new-key-xxx" }  // 可选，传值则替换
```

### 1.5 应用场景

| 组件 | 模式 | 功能 |
|------|------|------|
| `DeveloperKeyList` | readonly | 脱敏显示 + 明文切换 + 复制 |
| `DownstreamKeysTable` | readonly | 脱敏显示 + 明文切换 + 复制 |
| `UpstreamKeysTable` | editable | 脱敏显示 + 明文切换 + 复制 + 编辑 |
| `CredentialSection` | editable（上游） | 脱敏显示 + 明文切换 + 复制 + 编辑 |
| `ExpertCredentialTab` | editable | 脱敏显示 + 明文切换 + 复制 + 编辑 |

---

## 二、快速开始改进

### 2.1 代码卡片样式

**当前问题**：`CodeSnippet.tsx` 使用硬编码背景色 `#1e293b`

**解决方案**：
- 移除内联 `style={{ backgroundColor: '#1e293b' }}`
- 使用 Ant Design Card 组件默认样式
- 代码区域使用 `Typography.Text.code` 展示

**符合宪章**：§3.2 前端主题色规范

### 2.2 URL 自动填充

**当前**：`import.meta.env.VITE_API_BASE_URL || 'https://api.your-gateway.com'`

**改进**：
- 优先使用环境变量 `VITE_API_BASE_URL`
- 后备使用当前域名 `window.location.origin`
- 移除硬编码占位符

### 2.3 API Key 自动填充

**当前**：硬编码占位符 `sk-your-api-key`

**改进**：
- 从 `useUserApiKeys()` 获取当前用户的 API Key 列表
- 取第一个 `ACTIVE` 状态的 Key
- 调用详情 API 获取 `keyPlain`（明文）
- 直接填充到示例代码（系统为内部使用，无需脱敏确认）
- 若用户无 Key，显示占位符 + 提示"请先创建 API Key"

---

## 三、供应商目录 LOGO

### 3.1 当前状态

`ProviderCatalogView.tsx` 卡片仅显示：名称 + 类型标签 + code + 状态标签

### 3.2 改进方案

在卡片中集成 `ProviderIcon` 组件：

**卡片布局**：
```
┌─────────────────────────────┐
│  [LOGO]  OpenAI             │
│          INTERNATIONAL      │
│          openai             │
│          [已物化] [BUILTIN] │
│          [级联物化] [套餐]   │
└─────────────────────────────┘
```

**实现**：
- 使用 `ProviderIcon` 组件（已存在于 `components/ui/ProviderIcon.tsx`）
- 支持 12 个供应商品牌图标
- 找不到图标时显示首字母降级
- LOGO 尺寸参照供应商目录页面卡片样式

---

## 四、新建渠道向导

### 4.1 入口

1. **渠道页面** → "新建渠道"按钮 → 打开向导
2. **目录页面** → Plan 卡片 → "快速创建渠道"按钮 → 打开向导（预选该 Plan）

### 4.2 向导流程

**统一组件**：`ChannelCreateWizard.tsx`

| Step | 标题 | 内容 |
|------|------|------|
| **1** | 选择套餐 | 选择 Provider → 选择 Plan（目录入口则预选） |
| **2** | 配置端点与模型 | 端点配置 + 模型配置 |
| **3** | 配置凭证 | API Key 输入（支持批量） |
| **4** | 确认创建 | 汇总信息，一键完成 |

**导航按钮**：每个 Step 底部显示"上一步"、"下一步"按钮，Step 1 无上一步，Step 4 为"创建"。

### 4.3 Step 1 - 选择套餐

**内容**：
- Provider 下拉选择（从 `ProviderCatalog` 列表）
- Plan 下拉选择（根据 Provider 过滤 `PlanCatalog`）
- 预览区域：显示选中 Plan 的端点数量、模型数量、计费模式

**入口适配**：
- 渠道页面入口：Step 1 从 Provider 列表开始
- 目录页面入口：Step 1 预选 Provider 和 Plan，直接显示预览

**步骤间状态保持**：
- 返回上一步时保留已填写的数据
- 更换 Plan 时重置端点和模型为新的 Plan 默认值（不同 Plan 的端点/模型不同）
- 更换 Plan 时保留 API Key 输入（与 Plan 无关）

### 4.4 Step 2 - 配置端点与模型

#### 端点配置

**数据来源**：从选中 Plan 的 `endpoints` 数组自动填充

**UI 设计**：
- 可编辑表格（`EditableTable` 或自定义列表）
- 每行显示：
  - 复选框（默认选中）
  - 协议下拉（OPENAI / ANTHROPIC / GEMINI）
  - BaseUrl 输入框（可编辑）
  - 删除按钮
- 底部"新增端点"按钮

**交互**：
- 默认全选
- 可编辑 BaseUrl
- 可新增自定义端点
- 可删除端点

#### 模型配置

**数据来源**：从选中 Plan 的 `pricing` 数组 + `plan-models.json` 获取模型列表

**UI 设计**：
- 复选框列表（`Checkbox.Group`）
- 每项显示：模型名 + 上下文窗口 + 能力标签
- 底部"新增模型"按钮（输入自定义模型名）

**交互**：
- 默认全选
- 可取消勾选
- 可新增自定义模型
- 可删除模型

### 4.5 Step 3 - 配置凭证

**UI 设计**：
- 多行文本输入框（`Input.TextArea`）
- Placeholder：`请输入 API Key，多个 Key 用分号或逗号分隔，如：sk-xxx; sk-yyy`
- 实时解析提示：`已识别 3 个 API Key`

**解析规则**：
- 分隔符：分号 `;` 或逗号 `,`
- 自动 trim 空白
- 自动去重
- 过滤空值

**校验反馈**：
- 解析后展示逐条预览列表
- 每条 Key 显示脱敏格式
- 进行长度校验（过短的 Key 标红警告，如少于 8 位）
- 提示格式异常项

**后端处理**：
- 接收 `apiKeys: string[]` 数组
- 批量创建 `ChannelCredential`
- 每个 Key 对应一条记录
- 优先级/权重自动分配（递增或均分）

### 4.6 Step 4 - 确认创建

**汇总信息**：
- Provider / Plan
- 端点数量 + 列表预览
- 模型数量 + 列表预览
- API Key 数量

**操作**：
- "创建"按钮 → 调用后端 API
- 成功后返回渠道列表页面

### 4.7 后端 API

**方案 A：扩展现有物化 API**

```
POST /api/v1/catalog/plans/{planCode}/materialize
Request Body: {
  "apiKeys": ["sk-xxx", "sk-yyy"],
  "endpoints": [
    {"protocol": "OPENAI", "url": "https://api.openai.com/v1"}
  ],
  "models": ["gpt-4o", "gpt-4o-mini"],
  "channelName": "My OpenAI Channel"  // 可选
}
```

**方案 B：新增专用 API**

```
POST /api/v1/channels/create-from-catalog
Request Body: {
  "planCode": "openai_standard",
  "apiKeys": ["sk-xxx", "sk-yyy"],
  "endpoints": [...],
  "models": [...],
  "channelName": "My OpenAI Channel"
}
```

**选定方案 A**：复用现有物化逻辑，扩展参数支持。

---

## 五、类型定义更新

### 5.1 前端类型

```typescript
// types/team.ts
export interface UserApiKey {
  id: number;
  userId: number;
  keyPrefix: string;
  keyMasked: string;  // 新增
  name: string;
  models: string[];
  quotaLimit: number | null;
  state: 'ACTIVE' | 'INACTIVE';
  createdAt: string;
  updatedAt: string;
}

// types/channel.ts
export interface ChannelCredential {
  id: number;
  channelId: number;
  apiKeyPrefix: string;
  apiKeyMasked: string;  // 新增
  name: string;
  description: string | null;
  weight: number;
  priority: number;
  state: 'ACTIVE' | 'INACTIVE';
  createdAt: string;
  updatedAt: string;
}
```

### 5.2 后端 DTO

```java
// UserApiKeyResponse.java
public record UserApiKeyResponse(
    Long id,
    Long userId,
    String keyPrefix,
    String keyMasked,  // 新增
    String name,
    List<String> models,
    Long quotaLimit,
    UserApiKeyState state,
    Instant createdAt,
    Instant updatedAt
) {}

// ChannelCredentialResponse.java
public record ChannelCredentialResponse(
    Long id,
    Long channelId,
    String apiKeyPrefix,
    String apiKeyMasked,  // 新增
    String name,
    String description,
    Integer weight,
    Integer priority,
    CredentialState state,
    Instant createdAt,
    Instant updatedAt
) {}
```

---

## 六、实现优先级

| 优先级 | 任务 | 依赖 |
|--------|------|------|
| P0 | 后端 DTO 新增 `keyMasked` 字段 | 无 |
| P0 | `MaskedKeyDisplay` 组件开发 | 后端 DTO |
| P0 | 各页面集成 `MaskedKeyDisplay` | 组件开发 |
| P1 | 快速开始代码卡片样式修复 | 无 |
| P1 | 快速开始 URL/API Key 自动填充 | 后端 DTO |
| P1 | 供应商目录 LOGO 显示 | 无 |
| P2 | 新建渠道向导组件 | 后端 API 扩展 |
| P2 | 后端物化 API 扩展（支持批量 Key、自定义端点/模型） | 无 |

---

## 七、风险与缓解

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| API Key 明文传输安全 | 中 | 仅在 HTTPS 下传输，前端不缓存明文 |
| 批量 Key 创建失败部分成功 | 中 | 后端事务回滚或返回部分成功详情 |
| 目录数据与实际不一致 | 低 | 物化时校验端点可达性 |

---

## 八、验收标准

1. **API Key 显示**：所有场景统一使用 `MaskedKeyDisplay`，交互一致
2. **快速开始**：代码卡片无硬编码颜色，URL/API Key 自动填充真实值
3. **供应商目录**：卡片显示品牌 LOGO
4. **渠道向导**：两个入口正常工作，四步完成创建，支持批量 Key、端点/模型编辑，创建成功后返回渠道列表
5. **宪章合规**：无硬编码颜色值，所有颜色使用 Ant Design token
