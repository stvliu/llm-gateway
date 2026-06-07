# 快速开始页面改进 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将快速开始页面改造为渐进式 3 区块布局，支持双协议代码示例和轻量试玩功能。

**Architecture:** 单栏纵向布局，页面级状态管理（currentKey / selectedModel / protocol），3 个区块通过 props 联动。API Key 快捷区替代原有表格，代码示例扩展为 OpenAI + Anthropic 双协议，新增轻量试玩面板。

**Tech Stack:** React 18 + Ant Design 5 + react-i18next + TanStack Query + fetch API（SSE 流式）

---

## 文件结构

| 操作 | 文件 | 职责 |
|------|------|------|
| 新建 | `Quickstart/ApiKeySelector.tsx` | API Key 一行式选择器（脱敏显示 + 切换/新建/复制） |
| 新建 | `Quickstart/PlaygroundPanel.tsx` | 轻量试玩面板（发送请求 + 流式/JSON 响应预览） |
| 改造 | `Quickstart/index.tsx` | 页面级状态 + 3 区块布局 |
| 改造 | `Quickstart/ModelCard.tsx` | 增加 selected / onSelect |
| 改造 | `Quickstart/CodeSnippet.tsx` | 双协议 + 模型选择器 + 双向联动 |
| 保留 | `Quickstart/KeyGenerateModal.tsx` | 无需改动 |
| 删除 | `Quickstart/DeveloperKeyList.tsx` | 被 ApiKeySelector 替代 |
| 更新 | `locales/zh-CN/quickstart.json` | 新增翻译 key |
| 更新 | `locales/en-US/quickstart.json` | 新增翻译 key |

---

### Task 1: 新增翻译 key

**Files:**
- Modify: `gateway-console/src/locales/zh-CN/quickstart.json`
- Modify: `gateway-console/src/locales/en-US/quickstart.json`

- [ ] **Step 1: 更新中文翻译文件**

将 `zh-CN/quickstart.json` 替换为：

```json
{
  "title": "快速开始",
  "subtitle": "浏览可用模型，创建 API Key，一键验证连通性",
  "createKey": "创建 API Key",
  "search": "搜索模型...",
  "noModels": "暂无可用模型，请联系团队管理员开通",
  "apiKey.current": "API Key",
  "apiKey.copy": "复制",
  "apiKey.switch": "切换",
  "apiKey.create": "+ 新建",
  "apiKey.placeholder": "创建 API Key 以开始调用模型",
  "apiKey.createHint": "Key 创建后自动关联到您的账户",
  "protocol.openai": "OpenAI",
  "protocol.anthropic": "Anthropic",
  "model.selected": "已选中",
  "model.select": "选择模型",
  "playground.title": "试玩",
  "playground.send": "发送",
  "playground.stop": "停止",
  "playground.streaming": "流式响应中...",
  "playground.stream": "流式",
  "playground.placeholder": "输入测试消息...",
  "playground.noKey": "请先创建 API Key",
  "playground.usingAbove": "使用上方选中的 Key 和模型",
  "playground.done": "请求完成",
  "playground.failed": "请求失败",
  "playground.inputTokens": "输入",
  "playground.outputTokens": "输出",
  "playground.duration": "耗时",
  "playground.tokens": "tokens",
  "keyNamePrefix": "开发者 Key - {{date}}",
  "keyCreated": "API Key 创建成功",
  "keyCreatedTitle": "API Key 已创建",
  "keyCreateFailed": "创建失败",
  "keyCopied": "已复制",
  "keyCopyFailed": "复制失败",
  "keySaveHint": "请立即复制，关闭后不再显示",
  "createKeyHint": "点击下方按钮创建 API Key，创建后自动关联到当前用户，可调用团队已开通的所有模型。",
  "done": "完成",
  "generate": "一键生成",
  "keyRevoked": "Key 已吊销",
  "keyRevokeFailed": "吊销失败",
  "keyPrefix": "Key",
  "keyName": "名称",
  "keyStatus": "状态",
  "keyCreatedAt": "创建时间",
  "keyActions": "操作",
  "confirmRevoke": "确定吊销此 Key？",
  "noKeys": "暂无 API Key",
  "status.active": "活跃",
  "status.inactive": "未激活",
  "status.degraded": "已降级",
  "status.expired": "过期",
  "capability.vision": "图像",
  "capability.functionCalling": "函数调用",
  "capability.streaming": "流式",
  "model.contextWindow": "上下文:",
  "copy": "复制",
  "copySuccess": "已复制到剪贴板",
  "copyFailed": "复制失败，请手动选择复制",
  "noActiveKey": "暂无活跃的 API Key，请先激活或创建 Key",
  "noKey": "暂无 API Key，请先创建"
}
```

- [ ] **Step 2: 更新英文翻译文件**

将 `en-US/quickstart.json` 替换为：

```json
{
  "title": "Quick Start",
  "subtitle": "Browse models, create API keys, and verify connectivity in one click",
  "createKey": "Create API Key",
  "search": "Search models...",
  "noModels": "No models available, please contact your team admin",
  "apiKey.current": "API Key",
  "apiKey.copy": "Copy",
  "apiKey.switch": "Switch",
  "apiKey.create": "+ New",
  "apiKey.placeholder": "Create an API Key to start calling models",
  "apiKey.createHint": "Key will be automatically linked to your account",
  "protocol.openai": "OpenAI",
  "protocol.anthropic": "Anthropic",
  "model.selected": "Selected",
  "model.select": "Select model",
  "playground.title": "Playground",
  "playground.send": "Send",
  "playground.stop": "Stop",
  "playground.streaming": "Streaming...",
  "playground.stream": "Stream",
  "playground.placeholder": "Enter a test message...",
  "playground.noKey": "Please create an API Key first",
  "playground.usingAbove": "Using the Key and model selected above",
  "playground.done": "Request completed",
  "playground.failed": "Request failed",
  "playground.inputTokens": "Input",
  "playground.outputTokens": "Output",
  "playground.duration": "Duration",
  "playground.tokens": "tokens",
  "keyNamePrefix": "Developer Key - {{date}}",
  "keyCreated": "API Key created successfully",
  "keyCreatedTitle": "API Key Created",
  "keyCreateFailed": "Creation failed",
  "keyCopied": "Copied",
  "keyCopyFailed": "Copy failed",
  "keySaveHint": "Copy it now, it won't be shown again after closing",
  "createKeyHint": "Click the button below to create an API Key. It will be automatically linked to your account and can access all models enabled for your team.",
  "done": "Done",
  "generate": "Generate",
  "keyRevoked": "Key revoked",
  "keyRevokeFailed": "Revoke failed",
  "keyPrefix": "Key",
  "keyName": "Name",
  "keyStatus": "Status",
  "keyCreatedAt": "Created At",
  "keyActions": "Actions",
  "confirmRevoke": "Are you sure you want to revoke this key?",
  "noKeys": "No API Keys",
  "status.active": "Active",
  "status.inactive": "Inactive",
  "status.degraded": "Degraded",
  "status.expired": "Expired",
  "capability.vision": "Vision",
  "capability.functionCalling": "Function Calling",
  "capability.streaming": "Streaming",
  "model.contextWindow": "Context:",
  "copy": "Copy",
  "copySuccess": "Copied to clipboard",
  "copyFailed": "Copy failed, please select and copy manually",
  "noActiveKey": "No active API Key, please activate or create one first",
  "noKey": "No API Key, please create one first"
}
```

- [ ] **Step 3: 提交**

```bash
git add gateway-console/src/locales/zh-CN/quickstart.json gateway-console/src/locales/en-US/quickstart.json
git commit -m "feat(console): 新增快速开始页面翻译 key"
```

---

### Task 2: 新建 ApiKeySelector 组件

**Files:**
- Create: `gateway-console/src/pages/Quickstart/ApiKeySelector.tsx`

- [ ] **Step 1: 创建 ApiKeySelector 组件**

```tsx
import { useState, useEffect } from 'react';
import { Button, Dropdown, App, Typography, theme, Space } from 'antd';
import { CopyOutlined, PlusOutlined, SwapOutlined, KeyOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useAuthStore } from '@/stores/authStore';
import { useUserApiKeys } from '@/services/query/useUserApiKeys';
import { userApiKeyApi } from '@/services/api/userApiKey';
import { maskApiKey } from '@/utils/maskApiKey';

const { Text } = Typography;

interface Props {
  currentKey: string | undefined;
  currentKeyId: number | undefined;
  onKeyChange: (keyPlain: string, keyId: number) => void;
  onCreateClick: () => void;
}

export default function ApiKeySelector({ currentKey, currentKeyId, onKeyChange, onCreateClick }: Props) {
  const { t } = useTranslation('quickstart');
  const { token } = theme.useToken();
  const { message } = App.useApp();
  const currentUser = useAuthStore((s) => s.user);
  const userId = currentUser?.id ?? 0;
  const { data: keys } = useUserApiKeys(userId);
  const [loadingKeyId, setLoadingKeyId] = useState<number | null>(null);

  // 自动选择第一个 Key
  useEffect(() => {
    if (currentKey || !keys || keys.length === 0) return;
    const activeKey = keys.find((k) => k.state === 'ACTIVE');
    if (activeKey) {
      loadKeyDetail(activeKey.id);
    }
  }, [keys, currentKey]);

  const loadKeyDetail = async (keyId: number) => {
    setLoadingKeyId(keyId);
    try {
      const detail = await userApiKeyApi.getDetail(keyId);
      onKeyChange(detail.keyPlain, detail.id);
    } catch {
      // 获取失败忽略
    } finally {
      setLoadingKeyId(null);
    }
  };

  const handleCopy = async () => {
    if (!currentKey) return;
    try {
      await navigator.clipboard.writeText(currentKey);
      message.success(t('keyCopied'));
    } catch {
      message.error(t('keyCopyFailed'));
    }
  };

  const dropdownItems = keys?.map((k) => ({
    key: String(k.id),
    label: (
      <Space>
        <Text code style={{ fontSize: 12 }}>{maskApiKey(k.keyPlain)}</Text>
        <Text type="secondary" style={{ fontSize: 11 }}>{k.name}</Text>
      </Space>
    ),
    onClick: () => loadKeyDetail(k.id),
  })) ?? [];

  const hasKey = !!currentKey;

  return (
    <div style={{
      display: 'flex',
      alignItems: 'center',
      gap: 12,
      padding: '12px 16px',
      background: token.colorBgContainer,
      border: `1px solid ${hasKey ? token.colorBorder : token.colorBorderSecondary}`,
      borderRadius: token.borderRadiusLG,
      borderStyle: hasKey ? 'solid' : 'dashed',
    }}>
      <div style={{
        width: 32, height: 32,
        background: hasKey ? `${token.colorPrimary}10` : token.colorFillQuaternary,
        borderRadius: 6,
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        color: hasKey ? token.colorPrimary : token.colorTextQuaternary,
        fontSize: 16,
      }}>
        <KeyOutlined />
      </div>
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ fontSize: 12, color: token.colorTextSecondary, marginBottom: 2 }}>
          {t('apiKey.current')}
        </div>
        {hasKey ? (
          <Text code style={{ fontSize: 14 }}>{maskApiKey(currentKey!)}</Text>
        ) : (
          <Text type="secondary" style={{ fontSize: 13 }}>{t('apiKey.placeholder')}</Text>
        )}
      </div>
      <div style={{ display: 'flex', gap: 6, alignItems: 'center' }}>
        {hasKey && (
          <>
            <Button size="small" icon={<CopyOutlined />} onClick={handleCopy} loading={loadingKeyId !== null}>
              {t('apiKey.copy')}
            </Button>
            <Dropdown menu={{ items: dropdownItems }} trigger={['click']}>
              <Button size="small" icon={<SwapOutlined />}>
                {t('apiKey.switch')}
              </Button>
            </Dropdown>
          </>
        )}
        <Button size="small" type="primary" icon={<PlusOutlined />} onClick={onCreateClick}>
          {t('apiKey.create')}
        </Button>
      </div>
    </div>
  );
}
```

- [ ] **Step 2: 提交**

```bash
git add gateway-console/src/pages/Quickstart/ApiKeySelector.tsx
git commit -m "feat(console): 新增 ApiKeySelector 一行式 API Key 选择器"
```

---

### Task 3: 改造 ModelCard 组件

**Files:**
- Modify: `gateway-console/src/pages/Quickstart/ModelCard.tsx`

- [ ] **Step 1: 重写 ModelCard，增加 selected 和 onSelect**

将 `ModelCard.tsx` 替换为：

```tsx
import { Card, Tag, Typography, theme } from 'antd';
import { CheckCircleFilled } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import type { Model } from '@/types/model';

const { Text } = Typography;

interface Props {
  model: Model;
  selected?: boolean;
  onSelect?: (model: Model) => void;
}

export default function ModelCard({ model, selected, onSelect }: Props) {
  const { t } = useTranslation('quickstart');
  const { token } = theme.useToken();

  const capabilityKeys: Record<string, string> = {
    vision: 'capability.vision',
    function_calling: 'capability.functionCalling',
    streaming: 'capability.streaming',
  };

  const caps = model.capabilities
    ? Object.entries(model.capabilities)
        .filter(([, v]) => v)
        .map(([k]) => t(capabilityKeys[k] || k))
    : [];

  return (
    <Card
      hoverable
      size="small"
      onClick={() => onSelect?.(model)}
      styles={{
        body: { padding: 16 },
      }}
      style={selected ? {
        borderColor: token.colorPrimary,
        background: `${token.colorPrimary}08`,
        borderWidth: 2,
      } : undefined}
    >
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
        <div>
          <Text strong style={{ fontSize: 15 }}>{model.displayName || model.modelName}</Text>
          <div>
            <Text type="secondary" style={{ fontSize: 12 }}>{model.modelName}</Text>
          </div>
        </div>
        {selected ? (
          <CheckCircleFilled style={{ color: token.colorPrimary, fontSize: 18 }} />
        ) : (
          <CheckCircleFilled style={{ color: token.colorTextQuaternary, fontSize: 18 }} />
        )}
      </div>
      {caps.length > 0 && (
        <div style={{ marginTop: 8, display: 'flex', gap: 4, flexWrap: 'wrap' }}>
          {caps.map((c) => (
            <Tag key={c} color={selected ? 'blue' : 'default'} style={{ fontSize: 11 }}>{c}</Tag>
          ))}
        </div>
      )}
      <div style={{ marginTop: 8, fontSize: 12, color: token.colorTextSecondary }}>
        {t('model.contextWindow')} {model.contextWindow ? `${(model.contextWindow / 1000).toFixed(0)}K` : '-'}
      </div>
    </Card>
  );
}
```

- [ ] **Step 2: 提交**

```bash
git add gateway-console/src/pages/Quickstart/ModelCard.tsx
git commit -m "feat(console): ModelCard 增加 selected/onSelect，使用主题 token"
```

---

### Task 4: 改造 CodeSnippet 组件

**Files:**
- Modify: `gateway-console/src/pages/Quickstart/CodeSnippet.tsx`

- [ ] **Step 1: 重写 CodeSnippet，支持双协议 + 模型选择器 + 双向联动**

将 `CodeSnippet.tsx` 替换为：

```tsx
import { Segmented, Button, App, theme, Typography, Select, Spin, Empty } from 'antd';
import { CopyOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';

export type Protocol = 'openai' | 'anthropic';
type Lang = 'curl' | 'python' | 'node' | 'java';

interface Props {
  apiKey?: string;
  model: string;
  models: string[];
  protocol: Protocol;
  onModelChange: (model: string) => void;
  onProtocolChange: (protocol: Protocol) => void;
}

/** OpenAI 协议代码模板 */
const openaiSnippets: Record<Lang, (url: string, key: string, model: string) => string> = {
  curl: (url, key, model) => `curl ${url}/v1/chat/completions \\
  -H "Authorization: Bearer ${key}" \\
  -H "Content-Type: application/json" \\
  -d '{
    "model": "${model}",
    "messages": [{"role": "user", "content": "Hello"}]
  }'`,
  python: (url, key, model) => `import requests

response = requests.post(
    "${url}/v1/chat/completions",
    headers={
        "Authorization": "Bearer ${key}",
        "Content-Type": "application/json"
    },
    json={
        "model": "${model}",
        "messages": [{"role": "user", "content": "Hello"}]
    }
)
print(response.json())`,
  node: (url, key, model) => `const response = await fetch("${url}/v1/chat/completions", {
  method: "POST",
  headers: {
    "Authorization": "Bearer ${key}",
    "Content-Type": "application/json"
  },
  body: JSON.stringify({
    model: "${model}",
    messages: [{ role: "user", content: "Hello" }]
  })
});
const data = await response.json();
console.log(data);`,
  java: (url, key, model) => `HttpClient client = HttpClient.newHttpClient();
String body = """
{
  "model": "${model}",
  "messages": [{"role": "user", "content": "Hello"}]
}
""";

HttpRequest request = HttpRequest.newBuilder()
    .uri(URI.create("${url}/v1/chat/completions"))
    .header("Authorization", "Bearer ${key}")
    .header("Content-Type", "application/json")
    .POST(HttpRequest.BodyPublishers.ofString(body))
    .build();

HttpResponse<String> response =
    client.send(request, HttpResponse.BodyHandlers.ofString());
System.out.println(response.body());`,
};

/** Anthropic 协议代码模板 */
const anthropicSnippets: Record<Lang, (url: string, key: string, model: string) => string> = {
  curl: (url, key, model) => `curl ${url}/anthropic/v1/messages \\
  -H "x-api-key: ${key}" \\
  -H "anthropic-version: 2023-06-01" \\
  -H "Content-Type: application/json" \\
  -d '{
    "model": "${model}",
    "max_tokens": 1024,
    "messages": [{"role": "user", "content": "Hello"}]
  }'`,
  python: (url, key, model) => `import requests

response = requests.post(
    "${url}/anthropic/v1/messages",
    headers={
        "x-api-key": "${key}",
        "anthropic-version": "2023-06-01",
        "Content-Type": "application/json"
    },
    json={
        "model": "${model}",
        "max_tokens": 1024,
        "messages": [{"role": "user", "content": "Hello"}]
    }
)
print(response.json())`,
  node: (url, key, model) => `const response = await fetch("${url}/anthropic/v1/messages", {
  method: "POST",
  headers: {
    "x-api-key": "${key}",
    "anthropic-version": "2023-06-01",
    "Content-Type": "application/json"
  },
  body: JSON.stringify({
    model: "${model}",
    max_tokens: 1024,
    messages: [{ role: "user", content: "Hello" }]
  })
});
const data = await response.json();
console.log(data);`,
  java: (url, key, model) => `HttpClient client = HttpClient.newHttpClient();
String body = """
{
  "model": "${model}",
  "max_tokens": 1024,
  "messages": [{"role": "user", "content": "Hello"}]
}
""";

HttpRequest request = HttpRequest.newBuilder()
    .uri(URI.create("${url}/anthropic/v1/messages"))
    .header("x-api-key", "${key}")
    .header("anthropic-version", "2023-06-01")
    .header("Content-Type", "application/json")
    .POST(HttpRequest.BodyPublishers.ofString(body))
    .build();

HttpResponse<String> response =
    client.send(request, HttpResponse.BodyHandlers.ofString());
System.out.println(response.body());`,
};

const allSnippets: Record<Protocol, Record<Lang, (url: string, key: string, model: string) => string>> = {
  openai: openaiSnippets,
  anthropic: anthropicSnippets,
};

export default function CodeSnippet({ apiKey, model, models, protocol, onModelChange, onProtocolChange }: Props) {
  const { t } = useTranslation('quickstart');
  const { token } = theme.useToken();
  const { message } = App.useApp();
  const [lang, setLang] = useState<Lang>('curl');

  const gatewayUrl = import.meta.env.VITE_API_BASE_URL || window.location.origin;
  const displayApiKey = apiKey || 'sk-your-api-key';
  const code = allSnippets[protocol][lang](gatewayUrl, displayApiKey, model);

  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(code);
      message.success(t('copySuccess'));
    } catch {
      const textArea = document.createElement('textarea');
      textArea.value = code;
      textArea.style.position = 'fixed';
      textArea.style.left = '-9999px';
      document.body.appendChild(textArea);
      textArea.select();
      try {
        document.execCommand('copy');
        message.success(t('copySuccess'));
      } catch {
        message.error(t('copyFailed'));
      }
      document.body.removeChild(textArea);
    }
  };

  return (
    <div style={{ border: `1px solid ${token.colorBorder}`, borderRadius: token.borderRadiusLG, overflow: 'hidden' }}>
      <div style={{
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        padding: '8px 16px',
        background: token.colorBgLayout,
        borderBottom: `1px solid ${token.colorBorder}`,
        flexWrap: 'wrap',
        gap: 8,
      }}>
        <div style={{ display: 'flex', gap: 8, alignItems: 'center', flexWrap: 'wrap' }}>
          <Segmented
            size="small"
            value={protocol}
            onChange={(v) => onProtocolChange(v as Protocol)}
            options={[
              { value: 'openai', label: t('protocol.openai') },
              { value: 'anthropic', label: t('protocol.anthropic') },
            ]}
          />
          <Segmented
            size="small"
            value={lang}
            onChange={(v) => setLang(v as Lang)}
            options={[
              { value: 'curl', label: 'cURL' },
              { value: 'python', label: 'Python' },
              { value: 'node', label: 'Node.js' },
              { value: 'java', label: 'Java' },
            ]}
          />
        </div>
        <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
          <Select
            size="small"
            value={model}
            onChange={onModelChange}
            style={{ minWidth: 160 }}
            options={models.map((m) => ({ value: m, label: m }))}
            placeholder={t('model.select')}
          />
          <Button size="small" icon={<CopyOutlined />} onClick={handleCopy}>
            {t('copy')}
          </Button>
        </div>
      </div>
      <pre style={{
        margin: 0,
        padding: 16,
        background: token.colorBgElevated,
        color: token.colorText,
        fontSize: 13,
        lineHeight: 1.6,
        overflow: 'auto',
        fontFamily: 'Consolas, Monaco, "Courier New", monospace',
      }}>
        <code>{code}</code>
      </pre>
    </div>
  );
}
```

注意：需要补充 `import { useState } from 'react';` 在文件顶部。

- [ ] **Step 2: 提交**

```bash
git add gateway-console/src/pages/Quickstart/CodeSnippet.tsx
git commit -m "feat(console): CodeSnippet 支持双协议 + 模型选择器 + 主题 token"
```

---

### Task 5: 新建 PlaygroundPanel 组件

**Files:**
- Create: `gateway-console/src/pages/Quickstart/PlaygroundPanel.tsx`

- [ ] **Step 1: 创建 PlaygroundPanel 组件**

```tsx
import { useState, useRef, useCallback } from 'react';
import { Button, Input, Switch, Typography, App, Alert, theme, Space } from 'antd';
import { PlayCircleOutlined, StopOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import type { Protocol } from './CodeSnippet';

const { Text } = Typography;

interface Props {
  apiKey?: string;
  model: string;
  protocol: Protocol;
}

type RequestState = 'idle' | 'loading' | 'streaming' | 'done' | 'error';

export default function PlaygroundPanel({ apiKey, model, protocol }: Props) {
  const { t } = useTranslation('quickstart');
  const { token } = theme.useToken();
  const { message: msg } = App.useApp();

  const [input, setInput] = useState('你好，请用一句话介绍你自己');
  const [streamEnabled, setStreamEnabled] = useState(true);
  const [state, setState] = useState<RequestState>('idle');
  const [response, setResponse] = useState('');
  const [error, setError] = useState('');
  const [inputTokens, setInputTokens] = useState<number | null>(null);
  const [outputTokens, setOutputTokens] = useState<number | null>(null);
  const [duration, setDuration] = useState<number | null>(null);

  const abortRef = useRef<AbortController | null>(null);

  const gatewayUrl = import.meta.env.VITE_API_BASE_URL || window.location.origin;

  const buildRequest = () => {
    if (protocol === 'openai') {
      return {
        url: `${gatewayUrl}/v1/chat/completions`,
        headers: {
          'Authorization': `Bearer ${apiKey}`,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          model,
          messages: [{ role: 'user', content: input }],
          stream: streamEnabled,
        }),
      };
    }
    return {
      url: `${gatewayUrl}/anthropic/v1/messages`,
      headers: {
        'x-api-key': apiKey || '',
        'anthropic-version': '2023-06-01',
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        model,
        max_tokens: 1024,
        messages: [{ role: 'user', content: input }],
        stream: streamEnabled,
      }),
    };
  };

  const handleSend = useCallback(async () => {
    if (!apiKey || !model) return;

    const abort = new AbortController();
    abortRef.current = abort;
    setState('loading');
    setResponse('');
    setError('');
    setInputTokens(null);
    setOutputTokens(null);
    setDuration(null);

    const startTime = Date.now();
    const { url, headers, body } = buildRequest();

    try {
      const res = await fetch(url, {
        method: 'POST',
        headers,
        body,
        signal: abort.signal,
      });

      if (!res.ok) {
        const errText = await res.text();
        throw new Error(`${res.status} ${res.statusText}: ${errText}`);
      }

      if (streamEnabled && res.body) {
        setState('streaming');
        const reader = res.body.getReader();
        const decoder = new TextDecoder();
        let accumulated = '';

        while (true) {
          const { done, value } = await reader.read();
          if (done) break;

          const chunk = decoder.decode(value, { stream: true });
          const lines = chunk.split('\n');

          for (const line of lines) {
            if (line.startsWith('data: ')) {
              const data = line.slice(6).trim();
              if (data === '[DONE]') continue;

              try {
                const parsed = JSON.parse(data);
                // OpenAI SSE
                if (protocol === 'openai') {
                  const delta = parsed.choices?.[0]?.delta?.content;
                  if (delta) {
                    accumulated += delta;
                  }
                  if (parsed.usage) {
                    setInputTokens(parsed.usage.prompt_tokens);
                    setOutputTokens(parsed.usage.completion_tokens);
                  }
                }
                // Anthropic SSE
                if (protocol === 'anthropic') {
                  if (parsed.type === 'content_block_delta' && parsed.delta?.text) {
                    accumulated += parsed.delta.text;
                  }
                  if (parsed.type === 'message_start' && parsed.message?.usage) {
                    setInputTokens(parsed.message.usage.input_tokens);
                  }
                  if (parsed.type === 'message_delta' && parsed.usage) {
                    setOutputTokens(parsed.usage.output_tokens);
                  }
                }
              } catch {
                // 非 JSON 行，跳过
              }
            }
          }
          setResponse(accumulated);
        }
      } else {
        const data = await res.json();
        if (protocol === 'openai') {
          setResponse(data.choices?.[0]?.message?.content || JSON.stringify(data, null, 2));
          setInputTokens(data.usage?.prompt_tokens ?? null);
          setOutputTokens(data.usage?.completion_tokens ?? null);
        } else {
          setResponse(data.content?.[0]?.text || JSON.stringify(data, null, 2));
          setInputTokens(data.usage?.input_tokens ?? null);
          setOutputTokens(data.usage?.output_tokens ?? null);
        }
      }

      setDuration((Date.now() - startTime) / 1000);
      setState('done');
    } catch (err: unknown) {
      if (err instanceof DOMException && err.name === 'AbortError') {
        setState('idle');
        return;
      }
      setError(err instanceof Error ? err.message : String(err));
      setState('error');
    }
  }, [apiKey, model, protocol, input, streamEnabled, gatewayUrl]);

  const handleStop = () => {
    abortRef.current?.abort();
    abortRef.current = null;
  };

  const isRequesting = state === 'loading' || state === 'streaming';

  return (
    <div style={{
      background: token.colorBgContainer,
      border: `1px solid ${token.colorBorder}`,
      borderRadius: token.borderRadiusLG,
      padding: 16,
    }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
        <Text strong style={{ fontSize: 14 }}>{t('playground.title')}</Text>
        <Space size={4} style={{ fontSize: 11, color: token.colorTextSecondary }}>
          <span>{model}</span>
          <span>·</span>
          <span>{protocol === 'openai' ? t('protocol.openai') : t('protocol.anthropic')}</span>
        </Space>
      </div>

      <Input.TextArea
        value={input}
        onChange={(e) => setInput(e.target.value)}
        placeholder={t('playground.placeholder')}
        autoSize={{ minRows: 2, maxRows: 4 }}
        style={{ marginBottom: 12 }}
      />

      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
        <Space>
          {isRequesting ? (
            <Button
              danger
              icon={<StopOutlined />}
              onClick={handleStop}
            >
              {t('playground.stop')}
            </Button>
          ) : (
            <Button
              type="primary"
              icon={<PlayCircleOutlined />}
              onClick={handleSend}
              disabled={!apiKey}
            >
              {t('playground.send')}
            </Button>
          )}
          <Switch
            size="small"
            checked={streamEnabled}
            onChange={setStreamEnabled}
          />
          <Text type="secondary" style={{ fontSize: 11 }}>{t('playground.stream')}</Text>
        </Space>
        {!apiKey && (
          <Text type="secondary" style={{ fontSize: 11 }}>{t('playground.noKey')}</Text>
        )}
      </div>

      {state === 'streaming' && (
        <div style={{ fontSize: 11, color: token.colorSuccess, marginBottom: 8 }}>
          {t('playground.streaming')}
        </div>
      )}

      {state === 'error' && (
        <Alert
          type="error"
          message={t('playground.failed')}
          description={error}
          showIcon
          style={{ marginBottom: 12 }}
        />
      )}

      {response && (
        <pre style={{
          margin: 0,
          padding: 12,
          background: token.colorBgElevated,
          borderRadius: token.borderRadius,
          fontSize: 13,
          lineHeight: 1.6,
          overflow: 'auto',
          maxHeight: 240,
          fontFamily: 'Consolas, Monaco, "Courier New", monospace',
          whiteSpace: 'pre-wrap',
          wordBreak: 'break-word',
        }}>
          {response}
        </pre>
      )}

      {(state === 'done' || state === 'streaming') && (inputTokens !== null || outputTokens !== null || duration !== null) && (
        <div style={{
          display: 'flex',
          justifyContent: 'space-between',
          marginTop: 8,
          fontSize: 11,
          color: token.colorTextSecondary,
        }}>
          <Space size={12}>
            {inputTokens !== null && (
              <span>{t('playground.inputTokens')}: {inputTokens} {t('playground.tokens')}</span>
            )}
            {outputTokens !== null && (
              <span>{t('playground.outputTokens')}: {outputTokens} {t('playground.tokens')}</span>
            )}
          </Space>
          {duration !== null && <span>{t('playground.duration')}: {duration.toFixed(1)}s</span>}
        </div>
      )}
    </div>
  );
}
```

- [ ] **Step 2: 提交**

```bash
git add gateway-console/src/pages/Quickstart/PlaygroundPanel.tsx
git commit -m "feat(console): 新增 PlaygroundPanel 轻量试玩面板，支持双协议流式/非流式"
```

---

### Task 6: 改造 Quickstart 页面主文件

**Files:**
- Modify: `gateway-console/src/pages/Quickstart/index.tsx`
- Delete: `gateway-console/src/pages/Quickstart/DeveloperKeyList.tsx`

- [ ] **Step 1: 重写 index.tsx，整合 3 区块 + 页面级状态**

将 `Quickstart/index.tsx` 替换为：

```tsx
import { useState, useEffect } from 'react';
import { Row, Col, Input, Typography, Empty, Skeleton } from 'antd';
import { useTranslation } from 'react-i18next';
import { useModels } from '@/services/query/useModels';
import ModelCard from './ModelCard';
import ApiKeySelector from './ApiKeySelector';
import CodeSnippet from './CodeSnippet';
import type { Protocol } from './CodeSnippet';
import KeyGenerateModal from './KeyGenerateModal';
import PlaygroundPanel from './PlaygroundPanel';

const { Title, Paragraph } = Typography;

export default function Quickstart() {
  const { t } = useTranslation('quickstart');
  const { data: models, isLoading } = useModels();
  const [search, setSearch] = useState('');
  const [keyModalOpen, setKeyModalOpen] = useState(false);

  // 页面级联动状态
  const [currentKey, setCurrentKey] = useState<string>();
  const [currentKeyId, setCurrentKeyId] = useState<number>();
  const [selectedModel, setSelectedModel] = useState<string>('');
  const [protocol, setProtocol] = useState<Protocol>('openai');

  const filtered = models?.filter((m) =>
    m.state === 'ACTIVE' &&
    (m.displayName || m.modelName).toLowerCase().includes(search.toLowerCase())
  ) ?? [];

  // 模型列表（给 CodeSnippet 的 Select 用）
  const modelNames = filtered.map((m) => m.modelName);

  // 自动选择第一个模型
  useEffect(() => {
    if (!selectedModel && modelNames.length > 0) {
      setSelectedModel(modelNames[0]);
    }
  }, [modelNames, selectedModel]);

  // 如果当前选中的模型不在列表中，自动切换
  useEffect(() => {
    if (selectedModel && modelNames.length > 0 && !modelNames.includes(selectedModel)) {
      setSelectedModel(modelNames[0]);
    }
  }, [modelNames, selectedModel]);

  const handleKeyChange = (keyPlain: string, keyId: number) => {
    setCurrentKey(keyPlain);
    setCurrentKeyId(keyId);
  };

  const handleKeyCreated = (key: string) => {
    setCurrentKey(key);
    setKeyModalOpen(false);
  };

  const handleModelSelect = (modelName: string) => {
    setSelectedModel(modelName);
  };

  return (
    <div>
      <div style={{ marginBottom: 24 }}>
        <Title level={4} style={{ margin: 0 }}>{t('title')}</Title>
        <Paragraph type="secondary" style={{ margin: '4px 0 0' }}>
          {t('subtitle')}
        </Paragraph>
      </div>

      {/* 区块 1: API Key 快捷区 */}
      <div style={{ marginBottom: 24 }}>
        <ApiKeySelector
          currentKey={currentKey}
          currentKeyId={currentKeyId}
          onKeyChange={handleKeyChange}
          onCreateClick={() => setKeyModalOpen(true)}
        />
      </div>

      {/* 区块 2: 模型选择 + 代码示例 */}
      <div style={{ marginBottom: 24 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
          <Text strong>{t('search').replace('...', '')}</Text>
          <Input.Search
            placeholder={t('search')}
            style={{ width: 280 }}
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            allowClear
          />
        </div>

        {isLoading ? (
          <Row gutter={[12, 12]}>
            {Array.from({ length: 6 }).map((_, i) => (
              <Col key={i} xs={24} sm={12} md={8}>
                <Skeleton active paragraph={{ rows: 2 }} />
              </Col>
            ))}
          </Row>
        ) : filtered.length === 0 ? (
          <Empty description={t('noModels')} />
        ) : (
          <Row gutter={[12, 12]} style={{ marginBottom: 16 }}>
            {filtered.map((m) => (
              <Col key={m.id} xs={24} sm={12} md={8}>
                <ModelCard
                  model={m}
                  selected={m.modelName === selectedModel}
                  onSelect={(model) => handleModelSelect(model.modelName)}
                />
              </Col>
            ))}
          </Row>
        )}

        <CodeSnippet
          apiKey={currentKey}
          model={selectedModel}
          models={modelNames}
          protocol={protocol}
          onModelChange={setSelectedModel}
          onProtocolChange={setProtocol}
        />
      </div>

      {/* 区块 3: 轻量试玩 */}
      <PlaygroundPanel
        apiKey={currentKey}
        model={selectedModel}
        protocol={protocol}
      />

      <KeyGenerateModal
        open={keyModalOpen}
        onClose={() => setKeyModalOpen(false)}
        onKeyCreated={handleKeyCreated}
      />
    </div>
  );
}
```

- [ ] **Step 2: 删除 DeveloperKeyList.tsx**

```bash
rm gateway-console/src/pages/Quickstart/DeveloperKeyList.tsx
```

- [ ] **Step 3: 提交**

```bash
git add gateway-console/src/pages/Quickstart/index.tsx
git rm gateway-console/src/pages/Quickstart/DeveloperKeyList.tsx
git commit -m "feat(console): 快速开始页面改造为渐进式 3 区块布局，删除 DeveloperKeyList"
```

---

### Task 7: 构建验证

**Files:** 无文件变更

- [ ] **Step 1: 运行前端构建**

```bash
cd gateway-console && npm run build
```

Expected: 构建成功，无 TypeScript 错误

- [ ] **Step 2: 修复构建错误（如有）**

根据错误信息修复类型、导入等问题。

- [ ] **Step 3: 提交修复（如有）**

```bash
git add -A && git commit -m "fix(console): 修复快速开始页面构建错误"
```

---

### Task 8: KeyGenerateModal 翻译命名空间适配

**Files:**
- Modify: `gateway-console/src/pages/Quickstart/KeyGenerateModal.tsx`

KeyGenerateModal 当前使用 `useTranslation('developer')` 命名空间，需改为 `quickstart`。

- [ ] **Step 1: 修改 KeyGenerateModal 的翻译命名空间**

在 `KeyGenerateModal.tsx` 中，将：

```tsx
const { t } = useTranslation('developer');
```

改为：

```tsx
const { t } = useTranslation('quickstart');
```

- [ ] **Step 2: 提交**

```bash
git add gateway-console/src/pages/Quickstart/KeyGenerateModal.tsx
git commit -m "fix(console): KeyGenerateModal 翻译命名空间从 developer 改为 quickstart"
```

---

## 自审结果

1. **Spec 覆盖度**：所有设计规格中的需求均有对应 Task：
   - 区块 1 ApiKeySelector → Task 2
   - 区块 2 ModelCard selected → Task 3
   - 区块 2 CodeSnippet 双协议 → Task 4
   - 区块 3 PlaygroundPanel → Task 5
   - 页面整合 → Task 6
   - 翻译 → Task 1
   - KeyGenerateModal 命名空间 → Task 8
   - 构建验证 → Task 7

2. **占位符扫描**：无 TBD/TODO，所有步骤包含完整代码。

3. **类型一致性**：`Protocol` 类型在 CodeSnippet.tsx 中 export，PlaygroundPanel.tsx 中 import，一致。`Model` 类型从 `@/types/model` 导入，与现有代码一致。`UserApiKey` 的 `keyPlain` 字段与 `team.ts` 类型定义一致。

4. **遗漏修复**：CodeSnippet.tsx 中的 `useState` import 需确保包含在文件顶部。
