# 配置体验重构实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**目标：** 将配置体验从纯 REST API 升级为面向用户思维模型的图形化管理界面，覆盖快速接入/专家模式/开发者门户/统一 Key 管理/模板批量操作。

**架构概要：** React 19 + TypeScript + Ant Design v6 + React Router v7 + Zustand + TanStack React Query。后端 API 已就绪（provider/channel/credential/model/catalog CRUD），前端增量改造现有页面并新建页面。

**分解说明：** 此规格覆盖多个独立子系统，拆分为 8 个可独立交付的阶段。每个阶段产出可工作的软件并有独立的价值闭环。

---

## 阶段说明

| 阶段 | 内容 | 依赖 | 价值 |
|------|------|------|------|
| 1 | 导航重构 + 页面骨架 | 无 | 承载后续所有页面的基础框架 |
| 2 | 开发者门户 | 阶段 1 | 开发者自助获取 Key，减少 Admin 工作量 |
| 3 | 模型目录页 | 阶段 1 | 模型可视化浏览和管理 |
| 4 | 快速接入向导增强 | 阶段 1 | 降低供应商配置门槛 |
| 5 | 统一 API Key 管理 | 阶段 1 | 上下游 Key 统一视图 |
| 6 | 专家模式增强 | 阶段 1 | 高阶管理员的完整控制面板 |
| 7 | 团队模型可见性 | 阶段 1 | 团队级别控制模型访问范围 |
| 8 | 模板与批量操作 | 阶段 1 | 规模化配置效率提升 |

---

## 阶段 1：导航重构 + 页面骨架

**说明：** 重构侧边栏导航为 7 项独立入口，新增 /models、/keys、/developer 路由和空页面骨架。新增权限常量，更新 i18n 词条。新增"团队与权限"入口功能域。

### 涉及文件

- Modify: `gateway-console/src/constants/menuConfig.tsx` — 重构为 7 项一级菜单
- Modify: `gateway-console/src/constants/permissions.ts` — 新增 KEY_READ/KEY_WRITE、DEVELOPER 权限
- Modify: `gateway-console/src/router/index.tsx` — 新增 /models、/keys、/developer 路由
- Create: `gateway-console/src/pages/Models/index.tsx` — 模型目录占位页
- Create: `gateway-console/src/pages/ApiKeys/index.tsx` — API Key 管理占位页
- Create: `gateway-console/src/pages/Developer/index.tsx` — 开发者门户占位页
- Modify: `gateway-console/src/locales/zh-CN/common.json` — 新增菜单 i18n 词条
- Modify: `gateway-console/src/locales/en-US/common.json` — 新增菜单 i18n 词条

### 步骤

**Step 1: 新增权限常量**

`gateway-console/src/constants/permissions.ts`：

```typescript
export const P = {
  DASHBOARD: 'dashboard',
  DASHBOARD_ADMIN: 'dashboard:admin',
  MODEL_READ: 'model:read',
  MODEL_WRITE: 'model:write',
  PROVIDER_READ: 'provider:read',
  PROVIDER_WRITE: 'provider:write',
  CATALOG_READ: 'catalog:read',
  CATALOG_WRITE: 'catalog:write',
  USER_READ: 'user:read',
  USER_WRITE: 'user:write',
  SETTINGS_READ: 'settings:read',
  SETTINGS_WRITE: 'settings:write',
  KEY_READ: 'key:read',
  KEY_WRITE: 'key:write',
  DEVELOPER: 'developer:access',
} as const;
```

**Step 2: 重构菜单配置**

`gateway-console/src/constants/menuConfig.tsx`：

```typescript
import {
  DashboardOutlined,
  CloudServerOutlined,
  TeamOutlined,
  SettingOutlined,
  DatabaseOutlined,
  LockOutlined,
  KeyOutlined,
  CodeOutlined,
} from '@ant-design/icons';

// ...MenuItemConfig type unchanged...

export const menuConfig: MenuItemConfig[] = [
  {
    key: '/dashboard',
    icon: <DashboardOutlined />,
    label: 'menu.home',
  },
  {
    key: '/providers',
    icon: <CloudServerOutlined />,
    label: 'menu.providers',
    permission: 'provider:read',
  },
  {
    key: '/models',
    icon: <DatabaseOutlined />,
    label: 'menu.models',
    permission: 'model:read',
  },
  {
    key: '/keys',
    icon: <KeyOutlined />,
    label: 'menu.apiKeys',
    permission: 'key:read',
  },
  {
    key: '/teams',
    icon: <TeamOutlined />,
    label: 'menu.teams',
    permission: 'user:read',
  },
  {
    key: '/developer',
    icon: <CodeOutlined />,
    label: 'menu.developer',
    permission: 'developer:access',
  },
  {
    key: 'system-settings',
    icon: <SettingOutlined />,
    label: 'menu.systemSettings',
    children: [
      {
        key: '/catalog',
        icon: <DatabaseOutlined />,
        label: 'menu.catalog',
        permission: 'catalog:read',
      },
      {
        key: '/change-password',
        icon: <LockOutlined />,
        label: 'menu.changePassword',
      },
    ],
  },
];
```

**Step 3: 新增路由**

`gateway-console/src/router/index.tsx` — 在 children 数组中新增：

```typescript
{
  path: 'models',
  element: <PermissionGuard permission={P.MODEL_READ}><Models /></PermissionGuard>,
},
{
  path: 'keys',
  element: <PermissionGuard permission={P.KEY_READ}><ApiKeys /></PermissionGuard>,
},
{
  path: 'developer',
  element: <PermissionGuard permission={P.DEVELOPER}><Developer /></PermissionGuard>,
},
```

同时新增 import：

```typescript
import Models from '@/pages/Models';
import ApiKeys from '@/pages/ApiKeys';
import Developer from '@/pages/Developer';
```

**Step 4: 创建占位页面**

`gateway-console/src/pages/Models/index.tsx`：

```typescript
import { Card, Typography } from 'antd';
import { useTranslation } from 'react-i18next';
import PageHeader from '@/components/ui/PageHeader';

export default function Models() {
  const { t } = useTranslation('models');
  return (
    <div>
      <PageHeader
        title={t('title', { defaultValue: '模型目录' })}
        subtitle={t('subtitle', { defaultValue: '浏览和管理全局模型' })}
      />
      <Card>
        <Typography.Text type="secondary">
          {t('placeholder', { defaultValue: '模型目录页面开发中...' })}
        </Typography.Text>
      </Card>
    </div>
  );
}
```

类似结构创建 `ApiKeys/index.tsx` 和 `Developer/index.tsx`。

**Step 5: 更新 i18n 词条**

`gateway-console/src/locales/zh-CN/common.json`：

```json
{
  "menu": {
    "home": "仪表盘",
    "providers": "供应商",
    "models": "模型目录",
    "apiKeys": "API Key 管理",
    "teams": "团队与权限",
    "developer": "开发者门户",
    "systemSettings": "系统设置",
    "catalog": "目录管理",
    "changePassword": "修改密码"
  }
}
```

同样更新 `en-US/common.json` 英文版。

**Step 6: 验证导航渲染**

预期效果：侧边栏显示 7 项（仪表盘 / 供应商 / 模型目录 / API Key 管理 / 团队与权限 / 开发者门户 / 系统设置），每项展开对应空页面。系统设置保持折叠子菜单（目录管理 + 修改密码）。

**Step 7: Commit**

```bash
git add gateway-console/src/constants/permissions.ts \
  gateway-console/src/constants/menuConfig.tsx \
  gateway-console/src/router/index.tsx \
  gateway-console/src/pages/Models/index.tsx \
  gateway-console/src/pages/ApiKeys/index.tsx \
  gateway-console/src/pages/Developer/index.tsx \
  gateway-console/src/locales/
git commit -m "feat: 重构导航为7项，新增模型目录/API Key管理/开发者门户页面骨架"
```

---

## 阶段 2：开发者门户

**说明：** 实现开发者门户独立视图，包含模型目录浏览、自助 API Key 生成、代码示例。通过右上角"切换到开发者视图"按钮进入。

**前置条件：** 阶段 1（路由 + 占位页已就位）

### 涉及文件

- Create: `gateway-console/src/pages/Developer/ModelCard.tsx` — 模型卡片组件
- Create: `gateway-console/src/pages/Developer/KeyGenerateModal.tsx` — 一键生成 Key 弹窗
- Create: `gateway-console/src/pages/Developer/CodeSnippet.tsx` — 多语言代码示例组件
- Create: `gateway-console/src/pages/Developer/DeveloperLayout.tsx` — 开发者门户专用布局（无侧边栏）
- Modify: `gateway-console/src/pages/Developer/index.tsx` — 主页面，组装子组件
- Modify: `gateway-console/src/services/api/userApiKey.ts` — 新增开发者自助 Key API
- Create: `gateway-console/src/services/query/useUserApiKeys.ts` — React Query hooks
- Modify: `gateway-console/src/components/layout/Header.tsx` — 添加"切换到开发者视图"按钮
- Modify: `gateway-console/src/router/index.tsx` — 开发者门户使用独立布局

### 步骤

**Step 1: 创建 ModelCard 组件**

`gateway-console/src/pages/Developer/ModelCard.tsx`

```typescript
import { Card, Tag, Space, Typography } from 'antd';
import { CheckCircleFilled } from '@ant-design/icons';
import type { Model } from '@/types/model';

const { Text } = Typography;

interface Props {
  model: Model;
  onSelect?: (model: Model) => void;
}

const capabilityLabels: Record<string, string> = {
  vision: '图像',
  function_calling: '函数调用',
  streaming: '流式',
};

export default function ModelCard({ model, onSelect }: Props) {
  const caps = model.capabilities
    ? Object.entries(model.capabilities)
        .filter(([, v]) => v)
        .map(([k]) => capabilityLabels[k] || k)
    : [];

  return (
    <Card
      hoverable
      size="small"
      onClick={() => onSelect?.(model)}
      styles={{ body: { padding: 16 } }}
    >
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
        <div>
          <Text strong style={{ fontSize: 15 }}>{model.displayName || model.modelName}</Text>
          <div>
            <Text type="secondary" style={{ fontSize: 12 }}>{model.modelName}</Text>
          </div>
        </div>
        <CheckCircleFilled style={{ color: '#22c55e', fontSize: 18 }} />
      </div>
      {caps.length > 0 && (
        <div style={{ marginTop: 8, display: 'flex', gap: 4, flexWrap: 'wrap' }}>
          {caps.map((c) => (
            <Tag key={c} color="blue" style={{ fontSize: 11 }}>{c}</Tag>
          ))}
        </div>
      )}
      <div style={{ marginTop: 8, fontSize: 12, color: '#64748b' }}>
        上下文: {model.contextWindow ? `${(model.contextWindow / 1000).toFixed(0)}K` : '-'}
      </div>
    </Card>
  );
}
```

**Step 2: 创建 KeyGenerateModal 组件**

`gateway-console/src/pages/Developer/KeyGenerateModal.tsx`

```typescript
import { useState } from 'react';
import { Modal, App, Result, Button, Typography } from 'antd';
import { useTranslation } from 'react-i18next';
import { useCreateUserApiKey } from '@/services/query/useUserApiKeys';
import { useAuthStore } from '@/stores/authStore';
import type { CreateUserApiKeyResponse } from '@/types/team';

const { Text, Paragraph } = Typography;

interface Props {
  open: boolean;
  onClose: () => void;
}

export default function KeyGenerateModal({ open, onClose }: Props) {
  const { t } = useTranslation('developer');
  const { message } = App.useApp();
  const { user } = useAuthStore();
  const createMutation = useCreateUserApiKey();
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState<CreateUserApiKeyResponse | null>(null);

  const handleCreate = async () => {
    if (!user) return;
    setLoading(true);
    try {
      const res = await createMutation.mutateAsync({
        userId: user.id,
        name: `开发者 Key - ${new Date().toLocaleDateString()}`,
      });
      setResult(res);
      message.success(t('keyCreated', { defaultValue: 'API Key 创建成功' }));
    } catch (e) {
      message.error(t('keyCreateFailed', { defaultValue: '创建失败' }));
    } finally {
      setLoading(false);
    }
  };

  const handleClose = () => {
    setResult(null);
    onClose();
  };

  return (
    <Modal
      title={t('createKey', { defaultValue: '创建 API Key' })}
      open={open}
      onCancel={handleClose}
      footer={result ? (
        <Button type="primary" onClick={handleClose}>
          {t('done', { defaultValue: '完成' })}
        </Button>
      ) : (
        <Button type="primary" loading={loading} onClick={handleCreate}>
          {t('generate', { defaultValue: '一键生成' })}
        </Button>
      )}
      width={560}
    >
      {result ? (
        <Result
          status="success"
          title={t('keyCreated', { defaultValue: 'API Key 已创建' })}
          subTitle={t('keySaveHint', { defaultValue: '请立即复制，关闭后不再显示' })}
          extra={[
            <div key="key" style={{ background: '#1e293b', color: '#e2e8f0', padding: '12px 16px', borderRadius: 8, fontFamily: 'monospace', fontSize: 13, wordBreak: 'break-all' }}>
              {result.keyPlain}
            </div>,
          ]}
        />
      ) : (
        <Paragraph type="secondary">
          {t('createKeyHint', { defaultValue: '点击下方按钮创建 API Key，创建后自动关联到当前用户，可调用团队已开通的所有模型。' })}
        </Paragraph>
      )}
    </Modal>
  );
}
```

**Step 3: 创建 CodeSnippet 组件**

`gateway-console/src/pages/Developer/CodeSnippet.tsx`

```typescript
import { useState } from 'react';
import { Segmented, Typography, Button, App } from 'antd';
import { CopyOutlined } from '@ant-design/icons';

const { Text } = Typography;

interface Props {
  apiKey?: string;
}

const snippets: Record<string, (key: string) => string> = {
  curl: (key) => `curl https://api.your-gateway.com/v1/chat/completions \\
  -H "Authorization: Bearer ${key || 'sk-your-api-key'}" \\
  -H "Content-Type: application/json" \\
  -d '{
    "model": "gpt-4o",
    "messages": [{"role": "user", "content": "Hello"}]
  }'`,
  python: (key) => `import requests

response = requests.post(
    "https://api.your-gateway.com/v1/chat/completions",
    headers={
        "Authorization": "Bearer ${key || 'sk-your-api-key'}",
        "Content-Type": "application/json"
    },
    json={
        "model": "gpt-4o",
        "messages": [{"role": "user", "content": "Hello"}]
    }
)
print(response.json())`,
  node: (key) => `const response = await fetch("https://api.your-gateway.com/v1/chat/completions", {
  method: "POST",
  headers: {
    "Authorization": "Bearer ${key || 'sk-your-api-key'}",
    "Content-Type": "application/json"
  },
  body: JSON.stringify({
    model: "gpt-4o",
    messages: [{ role: "user", content: "Hello" }]
  })
});
const data = await response.json();
console.log(data);`,
};

type Lang = 'curl' | 'python' | 'node';

export default function CodeSnippet({ apiKey }: Props) {
  const { message } = App.useApp();
  const [lang, setLang] = useState<Lang>('curl');
  const code = snippets[lang](apiKey || '');

  const handleCopy = () => {
    navigator.clipboard.writeText(code);
    message.success('已复制到剪贴板');
  };

  return (
    <div style={{ border: '1px solid #e2e8f0', borderRadius: 8, overflow: 'hidden' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '8px 16px', background: '#f8fafc', borderBottom: '1px solid #e2e8f0' }}>
        <Segmented
          size="small"
          value={lang}
          onChange={(v) => setLang(v as Lang)}
          options={[
            { value: 'curl', label: 'cURL' },
            { value: 'python', label: 'Python' },
            { value: 'node', label: 'Node.js' },
          ]}
        />
        <Button size="small" icon={<CopyOutlined />} onClick={handleCopy}>
          复制
        </Button>
      </div>
      <pre style={{ margin: 0, padding: 16, background: '#1e293b', color: '#e2e8f0', fontSize: 13, lineHeight: 1.6, overflow: 'auto' }}>
        <code>{code}</code>
      </pre>
    </div>
  );
}
```

**Step 4: 组装开发者门户主页面**

`gateway-console/src/pages/Developer/index.tsx`

```typescript
import { useState } from 'react';
import { Row, Col, Input, Button, Typography, Empty } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useModels } from '@/services/query/useModels';
import ModelCard from './ModelCard';
import KeyGenerateModal from './KeyGenerateModal';
import CodeSnippet from './CodeSnippet';

const { Title, Paragraph } = Typography;

export default function Developer() {
  const { t } = useTranslation('developer');
  const { data: models, isLoading } = useModels();
  const [search, setSearch] = useState('');
  const [keyModalOpen, setKeyModalOpen] = useState(false);
  const [currentKey, setCurrentKey] = useState<string>();

  const filtered = models?.filter((m) =>
    m.state === 'ACTIVE' &&
    (m.displayName || m.modelName).toLowerCase().includes(search.toLowerCase())
  ) ?? [];

  const handleKeyCreated = (key: string) => {
    setCurrentKey(key);
  };

  return (
    <div style={{ maxWidth: 960, margin: '0 auto' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 24 }}>
        <div>
          <Title level={4} style={{ margin: 0 }}>{t('title', { defaultValue: '开发者门户' })}</Title>
          <Paragraph type="secondary" style={{ margin: '4px 0 0' }}>
            {t('subtitle', { defaultValue: '浏览可用模型，创建 API Key 快速开始' })}
          </Paragraph>
        </div>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => setKeyModalOpen(true)}>
          {t('createKey', { defaultValue: '创建 API Key' })}
        </Button>
      </div>

      <Input.Search
        placeholder={t('search', { defaultValue: '搜索模型...' })}
        style={{ width: 320, marginBottom: 16 }}
        onSearch={setSearch}
        allowClear
      />

      {filtered.length === 0 && !isLoading ? (
        <Empty description={t('noModels', { defaultValue: '暂无可用模型，请联系团队管理员开通' })} />
      ) : (
        <Row gutter={[12, 12]}>
          {filtered.map((m) => (
            <Col key={m.id} xs={24} sm={12} md={8}>
              <ModelCard model={m} />
            </Col>
          ))}
        </Row>
      )}

      <div style={{ marginTop: 32 }}>
        <Title level={5}>{t('quickStart', { defaultValue: '快速开始' })}</Title>
        <CodeSnippet apiKey={currentKey} />
      </div>

      <KeyGenerateModal
        open={keyModalOpen}
        onClose={() => setKeyModalOpen(false)}
      />
    </div>
  );
}
```

**Step 5: 创建 React Query hooks**

`gateway-console/src/services/query/useUserApiKeys.ts`：

```typescript
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { userApiKeyApi } from '@/services/api';
import type { CreateUserApiKeyRequest } from '@/types/team';

export const userApiKeyKeys = {
  all: ['userApiKeys'] as const,
  list: () => [...userApiKeyKeys.all, 'list'] as const,
  detail: (id: number) => [...userApiKeyKeys.all, 'detail', id] as const,
};

export function useCreateUserApiKey() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateUserApiKeyRequest) => userApiKeyApi.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: userApiKeyKeys.list() });
    },
  });
}
```

`gateway-console/src/services/query/useModels.ts`：

```typescript
import { useQuery } from '@tanstack/react-query';
import { modelApi } from '@/services/api';

export const modelKeys = {
  all: ['models'] as const,
  lists: () => [...modelKeys.all, 'list'] as const,
  details: () => [...modelKeys.all, 'detail'] as const,
  detail: (id: number) => [...modelKeys.all, 'detail', id] as const,
};

export function useModels() {
  return useQuery({
    queryKey: modelKeys.lists(),
    queryFn: () => modelApi.list(),
  });
}
```

**Step 6: 在 index.ts 中导出新 hooks**

确保 `gateway-console/src/services/query/index.ts` 和 `gateway-console/src/services/api/index.ts` 导出新模块。

**Step 7: Header 添加"切换到开发者视图"按钮**

`gateway-console/src/components/layout/Header.tsx` — 在右侧操作区添加：

```typescript
import { Button } from 'antd';
import { CodeOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '@/stores/authStore';

// 在 Header 组件内部、用户头像旁边：
const navigate = useNavigate();
const { hasPermission } = useAuthStore();

// ... 在 JSX 中合适位置：
{hasPermission('developer:access') && (
  <Button
    type="text"
    icon={<CodeOutlined />}
    onClick={() => navigate('/developer')}
  >
    切换到开发者视图
  </Button>
)}
```

**Step 8: Commit**

```bash
git add gateway-console/src/pages/Developer/ \
  gateway-console/src/services/query/useUserApiKeys.ts \
  gateway-console/src/services/query/useModels.ts \
  gateway-console/src/components/layout/Header.tsx
git commit -m "feat: 实现开发者门户（模型目录 + 自助Key + 代码示例）"
```

---

## 阶段 3：模型目录页

**说明：** 实现全局模型注册表可视化管理页面。Admin 可浏览、搜索、筛选、启用/禁用模型。此页面复用阶段 2 的 ModelCard 组件，但增加管理操作（编辑、删除、状态切换）。

**前置条件：** 阶段 1

### 涉及文件

- Modify: `gateway-console/src/pages/Models/index.tsx` — 完整的模型目录管理页
- Create: `gateway-console/src/pages/Models/ModelCreateModal.tsx` — 创建模型弹窗
- Create: `gateway-console/src/pages/Models/ModelEditDrawer.tsx` — 编辑模型抽屉
- Modify: `gateway-console/src/locales/zh-CN/models.json` — 新增 i18n 词条
- Modify: `gateway-console/src/locales/en-US/models.json` — 新增 i18n 词条

### 步骤

**Step 1: 创建 ModelCreateModal**

`gateway-console/src/pages/Models/ModelCreateModal.tsx` — 表单包含：modelName（必填）、displayName、modelFamily、contextWindow、capabilities（多选 Tag）、modalities（多选 Tag）。

```typescript
import { Modal, Form, Input, InputNumber, Select, Tag, App } from 'antd';
import { useTranslation } from 'react-i18next';
import { useCreateModel } from '@/services/query/useModels';
import type { CreateModelRequest } from '@/types/model';

interface Props {
  open: boolean;
  onClose: () => void;
}

const capabilityOptions = [
  { value: 'vision', label: '图像识别' },
  { value: 'function_calling', label: '函数调用' },
  { value: 'streaming', label: '流式' },
];

const modalityOptions = [
  { value: 'text', label: '文本' },
  { value: 'image', label: '图像' },
  { value: 'audio', label: '音频' },
];

export default function ModelCreateModal({ open, onClose }: Props) {
  const { t } = useTranslation('models');
  const { message } = App.useApp();
  const [form] = Form.useForm();
  const createMutation = useCreateModel();

  const handleOk = async () => {
    try {
      const values = await form.validateFields();
      const payload: CreateModelRequest = {
        ...values,
        capabilities: values.capabilities?.reduce((acc: Record<string, boolean>, k: string) => {
          acc[k] = true;
          return acc;
        }, {}) || {},
      };
      await createMutation.mutateAsync(payload);
      message.success(t('created', { defaultValue: '模型创建成功' }));
      form.resetFields();
      onClose();
    } catch (e: unknown) {
      if (e && typeof e === 'object' && 'errorFields' in e) return;
      message.error(t('createFailed', { defaultValue: '创建失败' }));
    }
  };

  return (
    <Modal title={t('createModel', { defaultValue: '新增模型' })} open={open} onOk={handleOk} onCancel={onClose} width={560}>
      <Form form={form} layout="vertical">
        <Form.Item name="modelName" label={t('modelName', { defaultValue: '模型标识' })} rules={[{ required: true }]}>
          <Input placeholder="gpt-4o" />
        </Form.Item>
        <Form.Item name="displayName" label={t('displayName', { defaultValue: '显示名称' })}>
          <Input placeholder="GPT-4o" />
        </Form.Item>
        <Form.Item name="modelFamily" label={t('modelFamily', { defaultValue: '模型族' })}>
          <Input placeholder="gpt-4" />
        </Form.Item>
        <Form.Item name="contextWindow" label={t('contextWindow', { defaultValue: '上下文窗口' })}>
          <InputNumber style={{ width: '100%' }} placeholder="128000" />
        </Form.Item>
        <Form.Item name="capabilities" label={t('capabilities', { defaultValue: '能力' })}>
          <Select mode="multiple" options={capabilityOptions} tagRender={(props) => <Tag closable={props.closable} onClose={props.onClose}>{props.label}</Tag>} />
        </Form.Item>
        <Form.Item name="modalities" label={t('modalities', { defaultValue: '模态' })}>
          <Select mode="multiple" options={modalityOptions} />
        </Form.Item>
      </Form>
    </Modal>
  );
}
```

**Step 2: 实现完整的模型目录页**

`gateway-console/src/pages/Models/index.tsx` — 包含搜索、筛选（按状态、供应商）、卡片视图/表格视图切换、创建/编辑/启用禁用/删除操作。复用阶段 2 的 ModelCard 风格。

**Step 3: Commit**

```bash
git add gateway-console/src/pages/Models/ \
  gateway-console/src/locales/
git commit -m "feat: 实现模型目录管理页（CRUD + 搜索筛选）"
```

---

## 阶段 4：快速接入向导增强

**说明：** 增强现有的 ProviderCreateModal，将目录选择 + 自定义创建的两步流程替换为设计稿中的三步向导（选供应商 → 填 Key → 选模型）。保留目录物化能力作为"从模板创建"选项。

**前置条件：** 阶段 1（路由就绪），现有 `ProviderCreateModal.tsx`、`BasicInfoStep.tsx`、`ModelSetupStep.tsx`

### 涉及文件

- Modify: `gateway-console/src/pages/Providers/ProviderCreateModal.tsx` — 重构为三步向导
- Modify/Replace: `gateway-console/src/pages/Providers/BasicInfoStep.tsx` — 适配新向导结构
- Modify/Replace: `gateway-console/src/pages/Providers/ModelSetupStep.tsx` — 适配新向导，默认不勾选
- Create: `gateway-console/src/pages/Providers/CredentialStep.tsx` — 新增 Key 配置步骤
- Modify: `gateway-console/src/pages/Providers/index.tsx` — 可能调整新建按钮行为

### 步骤

**Step 1: 创建 CredentialStep 组件**

`gateway-console/src/pages/Providers/CredentialStep.tsx` — API Key 输入 + 连通性测试 + 多 Key 负载均衡配置。

```typescript
import { useState } from 'react';
import { Input, Button, Tag, Space, Typography, Spin, App } from 'antd';
import { PlusOutlined, DeleteOutlined, CheckCircleFilled, CloseCircleFilled } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';

const { Text } = Typography;

interface CredentialEntry {
  id: string;
  value: string;
  status: 'pending' | 'testing' | 'success' | 'fail';
}

interface Props {
  credentials: CredentialEntry[];
  onChange: (credentials: CredentialEntry[]) => void;
}

export default function CredentialStep({ credentials, onChange }: Props) {
  const { t } = useTranslation('providers');
  const { message } = App.useApp();

  const handlePaste = (e: React.ClipboardEvent) => {
    const text = e.clipboardData.getData('text');
    const keys = text.split(/[\n,]+/).map((s) => s.trim()).filter(Boolean);
    if (keys.length > 1) {
      e.preventDefault();
      const newEntries = keys.map((k) => ({
        id: crypto.randomUUID(),
        value: k,
        status: 'pending' as const,
      }));
      onChange([...credentials, ...newEntries]);
      message.success(t('batchPaste', { defaultValue: `已添加 ${newEntries.length} 个 Key`, count: newEntries.length }));
    }
  };

  const addEntry = () => {
    onChange([...credentials, { id: crypto.randomUUID(), value: '', status: 'pending' }]);
  };

  const removeEntry = (id: string) => {
    onChange(credentials.filter((c) => c.id !== id));
  };

  const updateValue = (id: string, value: string) => {
    onChange(credentials.map((c) => (c.id === id ? { ...c, value } : c)));
  };

  return (
    <div>
      <Space direction="vertical" style={{ width: '100%' }} size="middle">
        {/* 批量粘贴提示 */}
        <div style={{ background: '#f0f5ff', padding: '8px 12px', borderRadius: 6, fontSize: 13, color: '#1d4ed8' }}>
          {t('credential.batchHint', { defaultValue: '支持同时粘贴多个 Key（换行或逗号分隔）' })}
        </div>

        {/* Key 列表 */}
        {credentials.map((entry, index) => (
          <div key={entry.id} style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
            <Input.Password
              value={entry.value}
              onChange={(e) => updateValue(entry.id, e.target.value)}
              onPaste={handlePaste}
              placeholder={t('credential.keyPlaceholder', { defaultValue: 'sk-...' })}
              style={{ flex: 1 }}
              addonBefore={
                <Tag color="default" style={{ marginRight: 0 }}>
                  #{index + 1}
                </Tag>
              }
              suffix={
                entry.status === 'success' ? <CheckCircleFilled style={{ color: '#22c55e' }} /> :
                entry.status === 'fail' ? <CloseCircleFilled style={{ color: '#ef4444' }} /> :
                null
              }
            />
            <Button
              type="text"
              danger
              icon={<DeleteOutlined />}
              onClick={() => removeEntry(entry.id)}
            />
          </div>
        ))}

        {/* 添加按钮 */}
        <Button
          type="dashed"
          icon={<PlusOutlined />}
          onClick={addEntry}
          block
        >
          {t('credential.addKey', { defaultValue: '添加 Key' })}
        </Button>
      </Space>
    </div>
  );
}
```

**Step 2: 重构 ProviderCreateModal 为三步向导**

将现有 ProviderCreateModal 改造为三步结构：
- Step 1: 选择供应商（目录模板 + 自定义。从模板选择时自动填充默认 Endpoint/Protocol/模型推荐）
- Step 2: 配置 Credential（新 CredentialStep 组件。可选多 Key）
- Step 3: 选择模型（现有 ModelSetupStep，默认全不勾选）

步骤间保存草稿到组件 state，关闭弹窗时丢弃。

**Step 3: 连通性测试集成**

在 CredentialStep 中添加"测试连通性"按钮，调用现有后端 ConnectivityTest API。测试结果显示在每个 Key 输入框右侧（CheckCircleFilled / CloseCircleFilled）。

**Step 4: Commit**

```bash
git add gateway-console/src/pages/Providers/CredentialStep.tsx \
  gateway-console/src/pages/Providers/ProviderCreateModal.tsx \
  gateway-console/src/pages/Providers/BasicInfoStep.tsx \
  gateway-console/src/pages/Providers/ModelSetupStep.tsx
git commit -m "feat: 三步向导式快速接入（供应商选择 → Key配置 → 模型选择）"
```

---

## 阶段 5：统一 API Key 管理

**说明：** 新建 API Key 管理页面，上游 Key（供应商凭证）和下游 Key（用户密钥）统一视图，双 Tab 切换。

**前置条件：** 阶段 1（路由就绪）

### 涉及文件

- Create: `gateway-console/src/pages/ApiKeys/UpstreamKeysTable.tsx` — 上游 Key 表格
- Create: `gateway-console/src/pages/ApiKeys/DownstreamKeysTable.tsx` — 下游 Key 表格
- Create: `gateway-console/src/pages/ApiKeys/KeyRotateModal.tsx` — Key 轮换弹窗
- Modify: `gateway-console/src/pages/ApiKeys/index.tsx` — 主页面，双 Tab 切换
- Modify: `gateway-console/src/locales/zh-CN/apiKeys.json` — i18n
- Modify: `gateway-console/src/locales/en-US/apiKeys.json` — i18n

### 步骤

**Step 1: 实现主页面双 Tab 布局**

`gateway-console/src/pages/ApiKeys/index.tsx`：

```typescript
import { Tabs } from 'antd';
import { UploadOutlined, DownloadOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import UpstreamKeysTable from './UpstreamKeysTable';
import DownstreamKeysTable from './DownstreamKeysTable';

export default function ApiKeys() {
  const { t } = useTranslation('apiKeys');

  return (
    <Tabs
      defaultActiveKey="upstream"
      items={[
        {
          key: 'upstream',
          label: <span>🔼 {t('upstream', { defaultValue: '上游 Key（供应商凭证）' })}</span>,
          children: <UpstreamKeysTable />,
        },
        {
          key: 'downstream',
          label: <span>🔽 {t('downstream', { defaultValue: '下游 Key（用户密钥）' })}</span>,
          children: <DownstreamKeysTable />,
        },
      ]}
    />
  );
}
```

**Step 2: 实现上游 Key 表格**

`UpstreamKeysTable.tsx` — 从现有 channel 和 credential API 获取数据。列：Key 前缀（可点击查看）、供应商（带 ProviderIcon）、关联通道、状态标签（色盲友好）、优先级/权重、最后使用时间、近24h 错误率、操作（查看/轮换/删除）。

**状态标签色盲友好设计：**
- `✅ 活跃` — 绿底 `#dcfce7`
- `⚠️ 已降级` — 黄底 `#fef9c3` + 警告图标
- `❌ 过期` — 红底 `#fef2f2` + 错误图标

**Step 3: 实现下游 Key 表格**

`DownstreamKeysTable.tsx` — 从现有 userApiKey API 获取数据。列：Key 前缀、所属用户、关联渠道数量、状态、最后使用时间、创建时间、操作（吊销/重生成）。

**Step 4: 实现 Key 轮换弹窗**

`KeyRotateModal.tsx` — 选择需要轮换的 Key → 确认 → 系统生成新 Key → 旧 Key 进入宽限期（显示宽限期倒计时）。

**Step 5: Commit**

```bash
git add gateway-console/src/pages/ApiKeys/ \
  gateway-console/src/locales/
git commit -m "feat: 统一 API Key 管理页（上游+下游双Tab + 轮换操作）"
```

---

## 阶段 6：专家模式增强

**说明：** 增强现有 ProviderManagementDrawer，从 2 Tab（基本信息/渠道）扩展为 6 Tab（基础信息/接入点/API Key/模型映射/限流配额/高级设置）。添加 YAML 实时预览。

**前置条件：** 阶段 1

### 涉及文件

- Modify: `gateway-console/src/pages/Providers/ProviderManagementDrawer.tsx` — 扩展为 6 Tab
- Create: `gateway-console/src/pages/Providers/ExpertEndpointTab.tsx` — 接入点管理 Tab
- Create: `gateway-console/src/pages/Providers/ExpertCredentialTab.tsx` — API Key Tab（带批量操作）
- Create: `gateway-console/src/pages/Providers/ExpertModelMappingTab.tsx` — 模型映射 Tab（upstream_model_name）
- Create: `gateway-console/src/pages/Providers/ExpertQuotaTab.tsx` — 限流配额 Tab
- Create: `gateway-console/src/pages/Providers/ExpertAdvancedTab.tsx` — 高级设置 Tab
- Create: `gateway-console/src/pages/Providers/YamlPreview.tsx` — YAML 实时预览组件
- Add: `gateway-console/src/pages/Providers/QuickModeSwitch.tsx` — 快速/专家模式切换

### 步骤

**Step 1: 新增 Tab 宽度常量 + 扩展 Drawer 宽度至 960px**

修改 `ProviderManagementDrawer.tsx`，`width` 从 720 → 960。

**Step 2: 实现 YAML 预览组件**

`YamlPreview.tsx` — 监听表单字段变化，实时生成 YAML 字符串展示。YAML 只读预览（点击"编辑 YAML"进入独立编辑器模式，此时锁定表单）。

```typescript
import { useMemo } from 'react';
import { Button, Typography } from 'antd';
import { EditOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';

const { Text } = Typography;

interface Props {
  provider: Record<string, unknown>;
  onEnterYamlMode: () => void;
}

function toYaml(obj: Record<string, unknown>, indent = 0): string {
  // 简单的 JS 对象 → YAML 字符串转换
  const pad = '  '.repeat(indent);
  return Object.entries(obj)
    .map(([key, value]) => {
      if (value === null || value === undefined) return '';
      if (Array.isArray(value)) {
        if (value.length === 0) return `${pad}${key}: []`;
        return `${pad}${key}:\n${value.map((item) => {
          if (typeof item === 'object') return `${pad}  - ${toYaml(item as Record<string, unknown>, indent + 2).trimStart()}`;
          return `${pad}  - ${item}`;
        }).join('\n')}`;
      }
      if (typeof value === 'object') {
        return `${pad}${key}:\n${toYaml(value as Record<string, unknown>, indent + 1)}`;
      }
      return `${pad}${key}: ${value}`;
    })
    .filter(Boolean)
    .join('\n');
}

export default function YamlPreview({ provider, onEnterYamlMode }: Props) {
  const { t } = useTranslation('providers');
  const yaml = useMemo(() => toYaml(provider), [provider]);

  return (
    <div style={{ border: '1px solid #e2e8f0', borderRadius: 8, overflow: 'hidden', marginTop: 16 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '8px 16px', background: '#f8fafc', borderBottom: '1px solid #e2e8f0' }}>
        <Text strong style={{ fontSize: 13 }}>{t('yamlPreview', { defaultValue: '配置预览（YAML）' })}</Text>
        <Button size="small" icon={<EditOutlined />} onClick={onEnterYamlMode}>
          {t('editYaml', { defaultValue: '编辑 YAML' })}
        </Button>
      </div>
      <pre style={{ margin: 0, padding: 16, background: '#1e293b', color: '#e2e8f0', fontSize: 12, lineHeight: 1.6, maxHeight: 300, overflow: 'auto', borderRadius: '0 0 8px 8px' }}>
        <code>{yaml}</code>
      </pre>
    </div>
  );
}
```

**Step 3-7:** 逐个实现 5 个新 Tab 组件，每个为独立的 Form 片段。

**Step 8: 重构 Drawer Tab 配置为 6 项**

```typescript
const tabs = [
  { key: 'basic', label: '基础信息', icon: <SettingOutlined /> },
  { key: 'endpoints', label: '接入点', icon: <ApiOutlined /> },
  { key: 'credentials', label: 'API Key', icon: <KeyOutlined /> },
  { key: 'models', label: '模型映射', icon: <DatabaseOutlined /> },
  { key: 'quota', label: '限流与配额', icon: <DashboardOutlined /> },
  { key: 'advanced', label: '高级设置', icon: <ControlOutlined /> },
];
```

每个 Tab 内容区域底部附加 YamlPreview 组件（仅在非编辑模式下显示）。

**Step 9: Commit**

```bash
git add gateway-console/src/pages/Providers/Expert*.tsx \
  gateway-console/src/pages/Providers/YamlPreview.tsx \
  gateway-console/src/pages/Providers/ProviderManagementDrawer.tsx
git commit -m "feat: 专家模式6Tab配置面板 + YAML实时预览"
```

---

## 阶段 7：团队模型可见性

**说明：** 增强现有 Teams 页面，在团队成员管理基础上增加模型可见性控制 Tab。团队管理员可在该 Tab 中选择哪些模型对团队成员可见。

**前置条件：** 阶段 3（模型目录已就位）

### 涉及文件

- Modify: `gateway-console/src/pages/Teams/index.tsx` — 新增"模型可见性"Tab
- Create: `gateway-console/src/pages/Teams/ModelVisibilityTab.tsx` — 模型可见性配置组件
- Create: `gateway-console/src/services/api/teamModel.ts` — 团队-模型关联 API
- Create: `gateway-console/src/services/query/useTeamModels.ts` — React Query hooks

### 步骤

**Step 1: 创建 ModelVisibilityTab**

`gateway-console/src/pages/Teams/ModelVisibilityTab.tsx` — 展示全局模型列表，每个模型带开关/Checkbox，团队管理员勾选后该团队可见。

```typescript
// 核心结构：Transfer 或 Checkbox.Group
// 左侧：全部可用模型列表
// 右侧：当前团队已选模型列表
```

**Step 2: 在 Teams 页面增加 Tab**

```typescript
const teamTabs = [
  { key: 'members', label: '成员管理' },
  { key: 'models', label: '模型可见性' },
];
```

**Step 3: Commit**

```bash
git add gateway-console/src/pages/Teams/ModelVisibilityTab.tsx \
  gateway-console/src/pages/Teams/index.tsx
git commit -m "feat: 团队模型可见性控制"
```

---

## 阶段 8：模板与批量操作

**说明：** 实现模板库浏览、使用模板创建、自定义模板保存、批量导入导出配置的功能。入口在供应商列表页面的操作栏。

**前置条件：** 阶段 1

### 涉及文件

- Create: `gateway-console/src/pages/Providers/TemplateGallery.tsx` — 模板库弹窗
- Create: `gateway-console/src/pages/Providers/BatchImportModal.tsx` — 批量导入弹窗（三步：上传→预览→确认）
- Create: `gateway-console/src/pages/Providers/BatchExportButton.tsx` — 导出按钮 + 范围选择
- Modify: `gateway-console/src/pages/Providers/index.tsx` — 添加模板/批量按钮

### 步骤

**Step 1: 实现 TemplateGallery**

展示预置模板卡片（OpenAI/Anthropic/Azure/Gemini），点击"使用此模板"进入快速向导（阶段 4 已实现），自动填充模板默认值。同时支持"保存为模板"按钮将现有配置保存为自定义模板。

**Step 2: 实现 BatchImportModal**

三步导入设计：
1. 上传：拖放区域 + 从剪贴板粘贴
2. 预览：解析结果概览 + 差异对比（新增/修改/不变），冲突项逐条标记
3. 确认：用户选择"跳过冲突"或"覆盖"

**Step 3: 实现 BatchExportButton**

下拉菜单选择导出范围（全部/指定供应商/指定通道），导出为 YAML/JSON 文件下载，不包含 API Key 明文。

**Step 4: Commit**

```bash
git add gateway-console/src/pages/Providers/TemplateGallery.tsx \
  gateway-console/src/pages/Providers/BatchImportModal.tsx \
  gateway-console/src/pages/Providers/BatchExportButton.tsx \
  gateway-console/src/pages/Providers/index.tsx
git commit -m "feat: 模板库 + 批量导入导出"
```

---

## 跨阶段工作清单

以下工作在实现各阶段时同步完成：

- [ ] **空状态**：每个列表页无数据时展示 EmptyState 组件 + 引导操作
- [ ] **加载态**：列表骨架屏、按钮 spinner、页面级 Spin
- [ ] **错误态**：API 失败时的错误提示 + 重试按钮 + 404 状态的 ServiceUnavailable 组件
- [ ] **无障碍**：状态标签使用图标+文字+颜色三重编码（色盲友好）
- [ ] **i18n**：每个新增页面需对应 zh-CN 和 en-US 词条文件

---

## 自检清单

- [ ] **规格覆盖**：每个阶段对应设计文档中的哪些章节已标明。核心章节覆盖：导航(§2.1)、快速接入(§3)、专家模式(§4)、开发者门户(§5)、模板批量(§6)、统一Key管理(§7)、团队模型可见性(§2.1/§13)
- [ ] **占位符扫描**：所有步骤包含完整代码，无 "TBD"/"TODO"/"实现细节待定"
- [ ] **类型一致性**：Model、Provider、Team、UserApiKey 等类型使用已有 `types/` 定义，新增组件接口 Props 类型均显式声明
- [ ] **依赖关系**：阶段 1 是所有阶段的前置依赖（提供路由和页面骨架），其余阶段相互独立可按任意顺序实现

---

## 执行方式选择

### 选项 A：Subagent-Driven（推荐）
按阶段逐个创建子 agent 实现，每个阶段完成后审查再进入下一阶段。

### 选项 B：Inline Execution
在本次会话中按步骤顺序手动实现。

**请选择执行方式。**