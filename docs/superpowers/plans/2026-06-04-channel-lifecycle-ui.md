# 渠道生命周期管理页面 — 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 合并 Providers 和 Channels 页面为统一的渠道管理页面，以 Channel 为核心聚合单元管理供应商生命周期。

**Architecture:** 移除 Providers 页面（25个组件），扩展 Channels 页面为唯一入口。供应商作为分组头部，渠道卡片展示统计+用量，详情抽屉新增概览Tab。新增3个组件，迁移4个组件，保留11个现有组件。

**Tech Stack:** React 18 + TypeScript + Ant Design 5 + TanStack Query + Zustand

---
change: channel-lifecycle-ui
design-doc: docs/superpowers/specs/2026-06-04-channel-lifecycle-ui-design.md
base-ref: cc37e83
---

## 文件变更清单

### 新增文件
- `src/pages/Channels/ChannelOverviewTab.tsx` — 渠道概览Tab
- `src/pages/Channels/ChannelTableView.tsx` — 列表视图
- `src/pages/Channels/ProviderEditModal.tsx` — 供应商编辑弹窗

### 迁移文件（从 Providers → Channels）
- `src/pages/Providers/TemplateLibrary.tsx` → `src/pages/Channels/TemplateLibrary.tsx`
- `src/pages/Providers/BatchImportModal.tsx` → `src/pages/Channels/BatchImportModal.tsx`
- `src/pages/Providers/BatchExportButton.tsx` → `src/pages/Channels/BatchExportButton.tsx`
- `src/pages/Providers/ConnectivityTestPanel.tsx` → `src/pages/Channels/ConnectivityTestPanel.tsx`

### 修改文件
- `src/pages/Channels/index.tsx` — 增加筛选栏、视图切换、批量操作、搜索增强
- `src/pages/Channels/ChannelCard.tsx` — 增加统计+用量展示
- `src/pages/Channels/ChannelDetailDrawer.tsx` — 新增概览Tab、快捷操作条
- `src/pages/Channels/ProviderGroupHeader.tsx` — 增加hover操作、更多菜单
- `src/pages/Channels/ChannelGroupedList.tsx` — 适配增强后的分组头部

### 删除文件
- `src/pages/Providers/` 整个目录（25个组件）
- 路由配置中 `/providers` 路由
- 侧边栏导航中"供应商"入口

---

## Task 1: 增强渠道卡片 ChannelCard

**Files:**
- Modify: `gateway-console/src/pages/Channels/ChannelCard.tsx`

- [ ] **Step 1: 增强 ChannelCard 组件，添加统计区域和用量展示**

在现有 ChannelCard 基础上增加：
- 端点/Key/模型三列数字统计区域（Stat 组件布局）
- 今日 Token 用量和成本展示行
- Key=0 时的警告标签 "⚠ 配置中"
- 停用渠道的视觉降级（opacity: 0.5、灰色数字、"最后活跃时间"）

```tsx
// ChannelCard.tsx 增加的统计区域
<Row gutter={8} style={{ marginTop: 8, marginBottom: 8 }}>
  <Col span={8}>
    <div style={{ textAlign: 'center', padding: '6px', background: '#f9f9f9', borderRadius: 4 }}>
      <div style={{ fontSize: 16, fontWeight: 700, color: isActive ? '#1677ff' : '#999' }}>{endpointCount}</div>
      <div style={{ fontSize: 10, color: '#999' }}>端点</div>
    </div>
  </Col>
  <Col span={8}>
    <div style={{ textAlign: 'center', padding: '6px', background: '#f9f9f9', borderRadius: 4 }}>
      <div style={{ fontSize: 16, fontWeight: 700, color: isActive ? (credentialCount === 0 ? '#fa8c16' : '#722ed1') : '#999' }}>{credentialCount}</div>
      <div style={{ fontSize: 10, color: '#999' }}>Key</div>
    </div>
  </Col>
  <Col span={8}>
    <div style={{ textAlign: 'center', padding: '6px', background: '#f9f9f9', borderRadius: 4 }}>
      <div style={{ fontSize: 16, fontWeight: 700, color: isActive ? '#13c2c2' : '#999' }}>{modelCount}</div>
      <div style={{ fontSize: 10, color: '#999' }}>模型</div>
    </div>
  </Col>
</Row>
```

- [ ] **Step 2: 验证卡片渲染**

运行: `cd gateway-console && npx tsc --noEmit`
Expected: 无类型错误

- [ ] **Step 3: 提交**

```bash
git add gateway-console/src/pages/Channels/ChannelCard.tsx
git commit -m "feat(channels): 增强渠道卡片展示端点/Key/模型统计和用量"
```

---

## Task 2: 增强分组头部 ProviderGroupHeader

**Files:**
- Modify: `gateway-console/src/pages/Channels/ProviderGroupHeader.tsx`

- [ ] **Step 1: 为分组头部添加操作按钮和更多菜单**

增强 ProviderGroupHeader 组件：
- 右侧常驻"⋯"更多菜单图标（Dropdown）
- 悬停时展开操作按钮：编辑供应商 / 停用 / 连通性测试
- 更多菜单项：编辑供应商 / 连通性测试 / 导出配置 / 停用供应商
- 状态为已停用时，停用按钮改为"启用供应商"

```tsx
// ProviderGroupHeader.tsx 新增的更多菜单
const menuItems = [
  { key: 'edit', label: '编辑供应商', icon: <EditOutlined /> },
  { key: 'test', label: '连通性测试', icon: <ApiOutlined /> },
  { key: 'export', label: '导出配置', icon: <ExportOutlined /> },
  { type: 'divider' },
  {
    key: 'toggle',
    label: provider.enabled ? '停用供应商' : '启用供应商',
    icon: provider.enabled ? <PauseCircleOutlined /> : <PlayCircleOutlined />,
    danger: provider.enabled,
  },
];
```

- [ ] **Step 2: 验证类型和渲染**

运行: `cd gateway-console && npx tsc --noEmit`
Expected: 无类型错误

- [ ] **Step 3: 提交**

```bash
git add gateway-console/src/pages/Channels/ProviderGroupHeader.tsx
git commit -m "feat(channels): 分组头部增加操作菜单和供应商管理功能"
```

---

## Task 3: 新增供应商编辑弹窗 ProviderEditModal

**Files:**
- Create: `gateway-console/src/pages/Channels/ProviderEditModal.tsx`

- [ ] **Step 1: 创建 ProviderEditModal 组件**

轻量弹窗，仅修改供应商品牌信息：

```tsx
interface ProviderEditModalProps {
  open: boolean;
  provider: Provider;
  onClose: () => void;
}

const ProviderEditModal: React.FC<ProviderEditModalProps> = ({ open, provider, onClose }) => {
  const [form] = Form.useForm();
  const updateProvider = useUpdateProvider();

  useEffect(() => {
    if (open && provider) {
      form.setFieldsValue({
        name: provider.name,
        description: provider.description,
        websiteUrl: provider.websiteUrl,
        apiDocUrl: provider.apiDocUrl,
      });
    }
  }, [open, provider, form]);

  const handleSave = async () => {
    const values = await form.validateFields();
    await updateProvider.mutateAsync({ id: provider.id, ...values });
    onClose();
  };

  return (
    <Modal title="编辑供应商" open={open} onCancel={onClose} onOk={handleSave} width={480}>
      <Form form={form} layout="vertical">
        <Form.Item name="name" label="供应商名称" rules={[{ required: true }]}>
          <Input />
        </Form.Item>
        <Form.Item name="description" label="描述">
          <Input.TextArea rows={3} />
        </Form.Item>
        <Form.Item name="websiteUrl" label="官网地址">
          <Input />
        </Form.Item>
        <Form.Item name="apiDocUrl" label="API 文档地址">
          <Input />
        </Form.Item>
      </Form>
    </Modal>
  );
};
```

- [ ] **Step 2: 在 ProviderGroupHeader 中集成 ProviderEditModal**

在 ProviderGroupHeader 中添加 `editModalOpen` state，点击"编辑供应商"菜单项时打开弹窗。

- [ ] **Step 3: 验证**

运行: `cd gateway-console && npx tsc --noEmit`
Expected: 无类型错误

- [ ] **Step 4: 提交**

```bash
git add gateway-console/src/pages/Channels/ProviderEditModal.tsx gateway-console/src/pages/Channels/ProviderGroupHeader.tsx
git commit -m "feat(channels): 新增供应商编辑弹窗，集成到分组头部"
```

---

## Task 4: 新增渠道概览Tab ChannelOverviewTab

**Files:**
- Create: `gateway-console/src/pages/Channels/ChannelOverviewTab.tsx`

- [ ] **Step 1: 创建 ChannelOverviewTab 组件**

包含4个区域：连通状态卡、Token/成本统计卡、资源摘要4卡片、活动时间线

```tsx
interface ChannelOverviewTabProps {
  channel: ChannelDetail;
  credentials: ChannelCredential[];
  channelModels: ChannelModel[];
  onTabChange: (tab: string) => void;
}

const ChannelOverviewTab: React.FC<ChannelOverviewTabProps> = ({
  channel, credentials, channelModels, onTabChange
}) => {
  return (
    <div>
      {/* 连通状态 + Token/成本 统计卡 (3列) */}
      <Row gutter={12} style={{ marginBottom: 20 }}>
        <Col span={8}>
          <ConnectivityCard channel={channel} />
        </Col>
        <Col span={8}>
          <TokenUsageCard />
        </Col>
        <Col span={8}>
          <CostCard />
        </Col>
      </Row>

      {/* 资源摘要 4卡片 (2x2) */}
      <Row gutter={12} style={{ marginBottom: 20 }}>
        <Col span={12}>
          <EndpointSummaryCard channel={channel} onViewDetail={() => onTabChange('endpoints')} />
        </Col>
        <Col span={12}>
          <CredentialSummaryCard credentials={credentials} onViewDetail={() => onTabChange('credentials')} />
        </Col>
        <Col span={12}>
          <ModelSummaryCard channelModels={channelModels} onViewDetail={() => onTabChange('models')} />
        </Col>
        <Col span={12}>
          <QuotaSummaryCard channel={channel} onViewDetail={() => onTabChange('quota')} />
        </Col>
      </Row>

      {/* 最近活动 */}
      <ActivityTimeline />
    </div>
  );
};
```

每个摘要卡片点击"查看详情→"时调用 `onTabChange` 切换到对应 Tab。Token/成本暂显示"--"占位符。活动时间线暂显示"暂无活动记录"。

- [ ] **Step 2: 验证**

运行: `cd gateway-console && npx tsc --noEmit`
Expected: 无类型错误

- [ ] **Step 3: 提交**

```bash
git add gateway-console/src/pages/Channels/ChannelOverviewTab.tsx
git commit -m "feat(channels): 新增渠道概览Tab，展示连通状态、资源摘要、活动时间线"
```

---

## Task 5: 改造渠道详情抽屉 ChannelDetailDrawer

**Files:**
- Modify: `gateway-console/src/pages/Channels/ChannelDetailDrawer.tsx`

- [ ] **Step 1: 集成概览Tab和快捷操作条**

改造 ChannelDetailDrawer：
- 默认 activeTab 从第一个Tab改为 `'overview'`
- 新增"概览"Tab项，使用 ChannelOverviewTab 组件
- 头部增加快捷操作条：连通性测试(蓝色主按钮) / 停用渠道(灰色) / 删除(红色，Popconfirm)
- 头部显示所属供应商Logo+名称，点击可打开 ProviderEditModal
- 状态标签移至头部右侧

```tsx
// ChannelDetailDrawer 改造后的 Tab 结构
const tabItems = [
  { key: 'overview', label: '📊 概览', children: <ChannelOverviewTab ... /> },
  { key: 'endpoints', label: `🌐 端点 (${endpointCount})`, children: <EndpointSection ... /> },
  { key: 'credentials', label: `🔑 API Key (${credentialCount})`, children: <CredentialSection ... /> },
  { key: 'models', label: `🤖 模型映射 (${modelCount})`, children: <ModelMappingSection ... /> },
  { key: 'quota', label: '⚙️ 配额与设置', children: <QuotaSettingsSection ... /> },
];
```

- [ ] **Step 2: 验证**

运行: `cd gateway-console && npx tsc --noEmit`
Expected: 无类型错误

- [ ] **Step 3: 提交**

```bash
git add gateway-console/src/pages/Channels/ChannelDetailDrawer.tsx gateway-console/src/pages/Channels/ChannelOverviewTab.tsx
git commit -m "feat(channels): 渠道详情抽屉增加概览Tab和快捷操作条"
```

---

## Task 6: 新增列表视图 ChannelTableView

**Files:**
- Create: `gateway-console/src/pages/Channels/ChannelTableView.tsx`

- [ ] **Step 1: 创建 ChannelTableView 组件**

紧凑表格模式，列为：供应商标签 / 渠道名 / 计费模式 / 优先级 / 端点数 / Key数 / 模型数 / 状态 / 操作

```tsx
interface ChannelTableViewProps {
  channels: Channel[];
  providers: Provider[];
  onChannelClick: (channelId: number) => void;
}

const ChannelTableView: React.FC<ChannelTableViewProps> = ({ channels, providers, onChannelClick }) => {
  const providerMap = useMemo(() => {
    return new Map(providers.map(p => [p.id, p]));
  }, [providers]);

  const columns = [
    {
      title: '供应商',
      dataIndex: 'providerId',
      width: 80,
      render: (id: number) => {
        const p = providerMap.get(id);
        return <Tag color="blue">{p?.name || '-'}</Tag>;
      },
    },
    { title: '渠道名称', dataIndex: 'name', render: (name: string, record: Channel) => (
      <a onClick={() => onChannelClick(record.id)}>{name}</a>
    )},
    { title: '计费模式', dataIndex: 'billingMode', width: 80 },
    { title: '优先级', dataIndex: 'priority', width: 60 },
    { title: '端点', key: 'endpoints', width: 60, render: (_: any, r: Channel) => r.endpoints?.length || 0 },
    { title: 'Key', key: 'credentials', width: 60 },
    { title: '模型', key: 'models', width: 60 },
    {
      title: '状态', key: 'status', width: 60,
      render: (_: any, r: Channel) => (
        <Tag color={r.enabled ? 'green' : 'orange'}>{r.enabled ? '运行中' : '已停用'}</Tag>
      ),
    },
  ];

  return <Table rowKey="id" columns={columns} dataSource={channels} size="small" pagination={false} />;
};
```

- [ ] **Step 2: 验证**

运行: `cd gateway-console && npx tsc --noEmit`
Expected: 无类型错误

- [ ] **Step 3: 提交**

```bash
git add gateway-console/src/pages/Channels/ChannelTableView.tsx
git commit -m "feat(channels): 新增渠道列表视图（紧凑表格模式）"
```

---

## Task 7: 迁移 Providers 组件到 Channels 目录

**Files:**
- Move: `src/pages/Providers/TemplateLibrary.tsx` → `src/pages/Channels/TemplateLibrary.tsx`
- Move: `src/pages/Providers/BatchImportModal.tsx` → `src/pages/Channels/BatchImportModal.tsx`
- Move: `src/pages/Providers/BatchExportButton.tsx` → `src/pages/Channels/BatchExportButton.tsx`
- Move: `src/pages/Providers/ConnectivityTestPanel.tsx` → `src/pages/Channels/ConnectivityTestPanel.tsx`

- [ ] **Step 1: 复制4个组件到 Channels 目录**

```bash
cp gateway-console/src/pages/Providers/TemplateLibrary.tsx gateway-console/src/pages/Channels/TemplateLibrary.tsx
cp gateway-console/src/pages/Providers/BatchImportModal.tsx gateway-console/src/pages/Channels/BatchImportModal.tsx
cp gateway-console/src/pages/Providers/BatchExportButton.tsx gateway-console/src/pages/Channels/BatchExportButton.tsx
cp gateway-console/src/pages/Providers/ConnectivityTestPanel.tsx gateway-console/src/pages/Channels/ConnectivityTestPanel.tsx
```

- [ ] **Step 2: 修复迁移组件中的导入路径**

检查每个迁移文件中的 import 路径，将 `../Providers/` 或 `./` 引用调整为 `./`（同一目录下）。修复对 types、api、query 的相对路径引用。

- [ ] **Step 3: 验证编译**

运行: `cd gateway-console && npx tsc --noEmit`
Expected: 无类型错误

- [ ] **Step 4: 提交**

```bash
git add gateway-console/src/pages/Channels/TemplateLibrary.tsx gateway-console/src/pages/Channels/BatchImportModal.tsx gateway-console/src/pages/Channels/BatchExportButton.tsx gateway-console/src/pages/Channels/ConnectivityTestPanel.tsx
git commit -m "refactor(channels): 迁移模板库、批量导入导出、连通性测试组件到Channels目录"
```

---

## Task 8: 重构渠道管理页面主入口

**Files:**
- Modify: `gateway-console/src/pages/Channels/index.tsx`

- [ ] **Step 1: 增强主入口页面**

在 `Channels/index.tsx` 中增加：
- 顶部操作栏：新增供应商按钮、批量导入按钮、导出按钮
- 筛选栏：供应商下拉筛选、状态下拉筛选、全局搜索输入框
- 视图切换：分组/列表切换按钮，持久化到 localStorage
- 搜索增强：匹配渠道名称和端点URL
- 底部新增供应商虚线卡片（在分组视图中显示）
- 集成 ChannelTableView（列表视图时使用）
- 集成迁移来的 BatchImportModal、BatchExportButton

```tsx
// 视图切换持久化
const [viewMode, setViewMode] = useState<ViewMode>(() => {
  return (localStorage.getItem('channel-view-mode') as ViewMode) || 'grouped';
});

const handleViewChange = (mode: ViewMode) => {
  setViewMode(mode);
  localStorage.setItem('channel-view-mode', mode);
};

// 搜索增强
const filteredChannels = useMemo(() => {
  if (!searchText) return channels;
  const q = searchText.toLowerCase();
  return channels.filter(ch => {
    if (ch.name.toLowerCase().includes(q)) return true;
    if (ch.endpoints?.some(ep => ep.endpointUrl.toLowerCase().includes(q))) return true;
    return false;
  });
}, [channels, searchText]);
```

- [ ] **Step 2: 验证**

运行: `cd gateway-console && npx tsc --noEmit`
Expected: 无类型错误

- [ ] **Step 3: 提交**

```bash
git add gateway-console/src/pages/Channels/index.tsx
git commit -m "feat(channels): 渠道管理页面增加筛选栏、视图切换、批量操作、搜索增强"
```

---

## Task 9: 更新路由和侧边栏导航

**Files:**
- Modify: `gateway-console/src/routes/` 下路由配置
- Modify: `gateway-console/src/layouts/` 下侧边栏导航配置

- [ ] **Step 1: 移除 /providers 路由，添加重定向**

在路由配置中：
- 移除 `/providers` 路由定义
- 添加从 `/providers` 到 `/channels` 的重定向

```tsx
{ path: '/providers', redirect: '/channels' }
```

- [ ] **Step 2: 合并侧边栏导航入口**

将侧边栏中"供应商"和"渠道"合并为单一"渠道管理"入口，URL 为 `/channels`。

- [ ] **Step 3: 验证**

运行: `cd gateway-console && npx tsc --noEmit`
Expected: 无类型错误

- [ ] **Step 4: 提交**

```bash
git add gateway-console/src/routes/ gateway-console/src/layouts/
git commit -m "refactor(nav): 合并供应商和渠道为渠道管理入口，添加路由重定向"
```

---

## Task 10: 删除 Providers 目录并清理引用

**Files:**
- Delete: `gateway-console/src/pages/Providers/` 整个目录

- [ ] **Step 1: 搜索所有对 Providers 目录的引用**

```bash
cd gateway-console && grep -r "pages/Providers" src/ --include="*.ts" --include="*.tsx" -l
```

- [ ] **Step 2: 移除所有引用**

逐个文件清理对 `pages/Providers/` 下组件的 import 语句。已迁移到 Channels 的组件更新 import 路径。

- [ ] **Step 3: 删除 Providers 目录**

```bash
rm -rf gateway-console/src/pages/Providers/
```

- [ ] **Step 4: 验证编译**

运行: `cd gateway-console && npx tsc --noEmit`
Expected: 无类型错误

- [ ] **Step 5: 提交**

```bash
git add -A gateway-console/src/pages/Providers/ gateway-console/src/
git commit -m "refactor: 移除Providers页面，所有功能已合并到渠道管理页面"
```

---

## Task 11: 端到端验证与构建

**Files:**
- None (验证任务)

- [ ] **Step 1: 运行 TypeScript 编译检查**

```bash
cd gateway-console && npx tsc --noEmit
```

Expected: 无错误

- [ ] **Step 2: 运行 ESLint 检查**

```bash
cd gateway-console && npx eslint src/pages/Channels/ --ext .ts,.tsx
```

Expected: 无错误（或仅有与现有代码一致的 warning）

- [ ] **Step 3: 运行前端构建**

```bash
cd gateway-console && npm run build
```

Expected: 构建成功

- [ ] **Step 4: 更新 tasks.md**

将所有已完成的任务在 `openspec/changes/channel-lifecycle-ui/tasks.md` 中勾选。

- [ ] **Step 5: 提交**

```bash
git add openspec/changes/channel-lifecycle-ui/tasks.md
git commit -m "chore: 更新渠道生命周期管理任务清单"
```
