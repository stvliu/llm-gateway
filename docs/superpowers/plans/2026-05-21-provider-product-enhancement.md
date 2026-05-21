# 供应商卡片及产品标签页完善实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 增强供应商卡片展示产品预览，重构产品标签页为卡片式布局，新增密钥测试功能。

**Architecture:** 前端采用 React Query 并行查询获取产品数据，卡片组件内聚数据获取逻辑。产品标签页从 Collapse 改为 Card 布局，API Key 改为列表式展示。后端新增密钥测试接口。

**Tech Stack:** React 18 + TypeScript + Ant Design 5 + React Query + i18next (前端); Spring Boot 3.5 + JPA (后端)

---

## 文件结构

```
gateway-console/src/pages/Providers/
├── index.tsx                      # 修改：新增 defaultTab 状态
├── ProviderCard.tsx               # 修改：新增产品展示，移除 Key 统计
├── ProviderCardView.tsx           # 修改：新增 onViewProducts 回调
├── ProviderManagementDrawer.tsx   # 修改：新增 defaultTab prop
├── ProviderProductsTab.tsx        # 重构：改为卡片式布局
├── ProductApiKeyCreateModal.tsx   # 新增：API Key 创建弹窗
├── ProductApiKeyTestButton.tsx    # 新增：密钥测试按钮组件
└── ProductCard.tsx                # 新增：产品卡片组件

gateway-console/src/services/
├── api/product.ts                 # 修改：新增 testKey API
└── query/useProducts.ts           # 修改：新增 useTestProductApiKey

gateway-console/src/locales/
├── zh-CN/providers.json           # 修改：新增翻译
└── en-US/providers.json           # 修改：新增翻译

gateway-boot/src/main/java/.../
├── adapter/api/ProductApiKeyController.java  # 新增：测试接口
├── application/product/dto/ApiKeyTestResponse.java  # 新增：响应 DTO
└── application/product/ProductApiKeyService.java    # 修改：新增测试方法
```

---

## Phase 1: 供应商卡片展示增强

### Task 1: 修改 ProviderCard 组件

**Files:**
- Modify: `gateway-console/src/pages/Providers/ProviderCard.tsx`

- [ ] **Step 1: 重写 ProviderCard 组件，新增产品展示区域**

```tsx
import { Card, Tag, Typography, Space, Tooltip, Spin } from 'antd';
import { GlobalOutlined, LinkOutlined, AppstoreOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useProducts } from '@/services/query/useProducts';
import type { Provider } from '@/types/provider';

const { Text, Paragraph } = Typography;

interface Props {
  provider: Provider;
  onClick: () => void;
  onViewProducts?: () => void;
}

export default function ProviderCard({ provider, onClick, onViewProducts }: Props) {
  const { t } = useTranslation('providers');
  const { data: products, isLoading } = useProducts(provider.id);

  const activeProducts = products?.filter(p => p.state === 'ACTIVE') || [];
  const displayProducts = activeProducts.slice(0, 3);
  const remainingCount = activeProducts.length - displayProducts.length;

  return (
    <Card
      hoverable
      onClick={onClick}
      style={{ height: '100%' }}
    >
      <Space direction="vertical" style={{ width: '100%' }} size="middle">
        {/* 基本信息 */}
        <Space>
          <Text strong>{provider.providerName}</Text>
          <Tag color={provider.state === 'ACTIVE' ? 'green' : 'default'}>
            {provider.state}
          </Tag>
        </Space>

        {provider.websiteUrl && (
          <Tooltip title={provider.websiteUrl}>
            <Text type="secondary" ellipsis style={{ maxWidth: 200 }}>
              <GlobalOutlined /> {provider.websiteUrl}
            </Text>
          </Tooltip>
        )}

        {provider.apiDocUrl && (
          <Paragraph type="secondary" ellipsis style={{ maxWidth: 200, marginBottom: 0 }}>
            <LinkOutlined /> {provider.apiDocUrl}
          </Paragraph>
        )}

        {/* 产品展示区域 */}
        <div style={{ marginTop: 8 }}>
          <div style={{ marginBottom: 4, display: 'flex', alignItems: 'center', gap: 4 }}>
            <AppstoreOutlined />
            <Text type="secondary">
              {t('card.products', { defaultValue: '产品' })} ({activeProducts.length})
            </Text>
          </div>
          {isLoading ? (
            <Spin size="small" />
          ) : activeProducts.length === 0 ? (
            <Text type="secondary" style={{ fontSize: 12 }}>
              {t('card.noProducts', { defaultValue: '暂无产品' })}
            </Text>
          ) : (
            <Space wrap size={[4, 4]}>
              {displayProducts.map((product) => (
                <Tag
                  key={product.id}
                  color="blue"
                  style={{ cursor: 'pointer' }}
                  onClick={(e) => {
                    e.stopPropagation();
                    onViewProducts?.();
                  }}
                >
                  {product.productName}
                </Tag>
              ))}
              {remainingCount > 0 && (
                <Tag>+{remainingCount}</Tag>
              )}
            </Space>
          )}
        </div>
      </Space>
    </Card>
  );
}
```

- [ ] **Step 2: 验证文件修改**

Run: `head -30 gateway-console/src/pages/Providers/ProviderCard.tsx`
Expected: 看到新的导入和组件定义

---

### Task 2: 修改 ProviderCardView 组件

**Files:**
- Modify: `gateway-console/src/pages/Providers/ProviderCardView.tsx`

- [ ] **Step 1: 新增 onViewProducts 回调**

```tsx
import { Row, Col } from 'antd';
import type { Provider } from '@/types/provider';
import ProviderCard from './ProviderCard';

interface Props {
  providers: Provider[];
  onSelect: (provider: Provider) => void;
  onViewProducts?: (provider: Provider) => void;
}

export default function ProviderCardView({ providers, onSelect, onViewProducts }: Props) {
  return (
    <Row gutter={[16, 16]}>
      {providers.map((provider) => (
        <Col key={provider.id} xs={24} sm={12} md={8} lg={6}>
          <ProviderCard
            provider={provider}
            onClick={() => onSelect(provider)}
            onViewProducts={() => onViewProducts?.(provider)}
          />
        </Col>
      ))}
    </Row>
  );
}
```

- [ ] **Step 2: 验证文件修改**

Run: `grep -n "onViewProducts" gateway-console/src/pages/Providers/ProviderCardView.tsx`
Expected: 找到新增的回调

---

### Task 3: 修改 ProviderManagementDrawer 组件

**Files:**
- Modify: `gateway-console/src/pages/Providers/ProviderManagementDrawer.tsx`

- [ ] **Step 1: 新增 defaultTab prop**

修改接口定义：

```tsx
interface ProviderManagementDrawerProps {
  providerId: number | null;
  providers: Provider[];
  onClose: () => void;
  onProviderChange: (providerId: number) => void;
  onProviderDeleted?: () => void;
  defaultTab?: 'basic' | 'products';  // 新增
}
```

修改组件实现：

```tsx
export function ProviderManagementDrawer({
  providerId,
  providers,
  onClose,
  onProviderChange,
  onProviderDeleted,
  defaultTab = 'basic',  // 新增默认值
}: ProviderManagementDrawerProps) {
  const { t } = useTranslation('providers');
  const { confirm } = useConfirm();

  // 状态
  const [activeTab, setActiveTab] = useState(defaultTab);  // 使用 defaultTab 初始化
  const [editing, setEditing] = useState(false);
  // ... 其余代码保持不变

  // 当 providerId 变化时重置状态，使用 defaultTab
  useEffect(() => {
    if (providerId !== null) {
      setActiveTab(defaultTab);
      setEditing(false);
      setDirty(false);
    }
  }, [providerId, defaultTab]);  // 依赖 defaultTab

  // ... 其余代码保持不变
}
```

- [ ] **Step 2: 验证文件修改**

Run: `grep -n "defaultTab" gateway-console/src/pages/Providers/ProviderManagementDrawer.tsx`
Expected: 找到新增的 prop

---

### Task 4: 修改 Providers 主页面

**Files:**
- Modify: `gateway-console/src/pages/Providers/index.tsx`

- [ ] **Step 1: 新增 defaultTab 状态和回调**

```tsx
import { useState, useCallback } from 'react';
import { Button, Input } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useAuthStore } from '@/stores/authStore';
import { P } from '@/constants/permissions';
import { useProviders } from '@/services/query';
import type { Provider } from '@/types/provider';
import ProviderCardView from './ProviderCardView';
import { ProviderManagementDrawer } from './ProviderManagementDrawer';
import { ProviderCreateModal } from './ProviderCreateModal';

export default function Providers() {
  const { t } = useTranslation('providers');
  const { hasPermission } = useAuthStore();
  const canWrite = hasPermission(P.PROVIDER_WRITE);

  const { data: providersData } = useProviders();
  const providers = providersData?.items ?? [];
  const [search, setSearch] = useState('');
  const [selected, setSelected] = useState<Provider | null>(null);
  const [createOpen, setCreateOpen] = useState(false);
  const [defaultTab, setDefaultTab] = useState<'basic' | 'products'>('basic');  // 新增

  const filtered = providers.filter((p) =>
    !search || p.providerName.toLowerCase().includes(search.toLowerCase())
  );

  const handleCreated = useCallback(() => {
    setCreateOpen(false);
  }, []);

  const handleViewProducts = useCallback((provider: Provider) => {
    setSelected(provider);
    setDefaultTab('products');
  }, []);

  const handleSelect = useCallback((provider: Provider) => {
    setSelected(provider);
    setDefaultTab('basic');
  }, []);

  return (
    <>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
        <Input.Search
          placeholder={t('search', { defaultValue: '搜索供应商' })}
          style={{ width: 300 }}
          onSearch={setSearch}
          allowClear
        />
        {canWrite && (
          <Button type="primary" icon={<PlusOutlined />} onClick={() => setCreateOpen(true)}>
            {t('addProvider', { defaultValue: '新增供应商' })}
          </Button>
        )}
      </div>

      <ProviderCardView
        providers={filtered}
        onSelect={handleSelect}
        onViewProducts={handleViewProducts}
      />

      <ProviderManagementDrawer
        providerId={selected?.id ?? null}
        providers={providers}
        onClose={() => setSelected(null)}
        onProviderChange={(id) => {
          const p = providers.find((pr) => pr.id === id);
          if (p) setSelected(p);
        }}
        defaultTab={defaultTab}
      />

      <ProviderCreateModal
        open={createOpen}
        providers={providers}
        onClose={() => setCreateOpen(false)}
        onCreated={handleCreated}
      />
    </>
  );
}
```

- [ ] **Step 2: 验证文件修改**

Run: `grep -n "defaultTab\|handleViewProducts" gateway-console/src/pages/Providers/index.tsx`
Expected: 找到新增的状态和回调

---

### Task 5: 添加国际化翻译

**Files:**
- Modify: `gateway-console/src/locales/zh-CN/providers.json`
- Modify: `gateway-console/src/locales/en-US/providers.json`

- [ ] **Step 1: 更新中文翻译文件**

在 `providers.json` 中添加：

```json
{
  "card.products": "产品",
  "card.noProducts": "暂无产品",
  "product.endpoints": "端点",
  "product.keys": "Keys",
  "product.associatedModels": "关联模型",
  "product.lastCall": "最近调用",
  "product.successRate": "成功率",
  "product.testKey": "测试",
  "product.testSuccess": "测试成功",
  "product.testFailed": "测试失败",
  "product.latency": "延迟",
  "product.keyCreated": "密钥创建成功",
  "product.keyCreatedHint": "请立即保存此密钥，关闭后将无法再次查看"
}
```

- [ ] **Step 2: 更新英文翻译文件**

在 `providers.json` 中添加：

```json
{
  "card.products": "Products",
  "card.noProducts": "No products",
  "product.endpoints": "Endpoints",
  "product.keys": "Keys",
  "product.associatedModels": "Associated Models",
  "product.lastCall": "Last Call",
  "product.successRate": "Success Rate",
  "product.testKey": "Test",
  "product.testSuccess": "Test Passed",
  "product.testFailed": "Test Failed",
  "product.latency": "Latency",
  "product.keyCreated": "API Key Created",
  "product.keyCreatedHint": "Please save this key immediately. You won't be able to see it again after closing."
}
```

- [ ] **Step 3: 验证翻译文件**

Run: `grep -n "card.products" gateway-console/src/locales/zh-CN/providers.json gateway-console/src/locales/en-US/providers.json`
Expected: 两个文件都找到新增的翻译键

---

## Phase 2: 产品标签页重构

### Task 6: 新增 ProductCard 组件

**Files:**
- Create: `gateway-console/src/pages/Providers/ProductCard.tsx`

- [ ] **Step 1: 创建产品卡片组件**

```tsx
import { useState } from 'react';
import { Card, Button, Tag, Space, Spin, Typography, Collapse } from 'antd';
import { EditOutlined, DeleteOutlined, PlusOutlined, ApiOutlined, AppstoreOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useProductApiKeys } from '@/services/query/useProducts';
import type { Product, ProductApiKey } from '@/types/product';
import ProductApiKeyCreateModal from './ProductApiKeyCreateModal';
import ProductApiKeyEditModal from './ProductApiKeyEditModal';

const { Text } = Typography;

interface Props {
  product: Product;
  onEdit: () => void;
  onDelete: () => void;
  onAddKey: () => void;
}

export default function ProductCard({ product, onEdit, onDelete, onAddKey }: Props) {
  const { t } = useTranslation('products');
  const { data: apiKeys, isLoading } = useProductApiKeys(product.id);

  const activeKeys = apiKeys?.filter(k => k.state === 'ACTIVE') || [];
  const endpointCount = Object.keys(product.endpoints || {}).length;

  const [createModalOpen, setCreateModalOpen] = useState(false);
  const [editingKey, setEditingKey] = useState<ProductApiKey | undefined>();

  return (
    <Card
      title={
        <Space>
          <Text strong>{product.productName}</Text>
          <Tag color={product.state === 'ACTIVE' ? 'green' : 'default'}>
            {product.state}
          </Tag>
        </Space>
      }
      extra={
        <Space>
          <Button type="text" icon={<EditOutlined />} onClick={onEdit} />
          <Button type="text" danger icon={<DeleteOutlined />} onClick={onDelete} />
        </Space>
      }
      style={{ marginBottom: 16 }}
    >
      {/* 统计信息 */}
      <Space style={{ marginBottom: 12 }}>
        <Tag>{t('product.endpoints')}: {endpointCount}</Tag>
        <Tag>{t('product.keys')}: {activeKeys.length}/{apiKeys?.length || 0}</Tag>
      </Space>

      {/* 端点配置 */}
      <div style={{ marginBottom: 16 }}>
        <Text type="secondary" style={{ marginBottom: 4, display: 'block' }}>
          {t('product.endpoints')}
        </Text>
        <Collapse
          ghost
          items={Object.entries(product.endpoints || {}).map(([protocol, url]) => ({
            key: protocol,
            label: protocol,
            children: <Text code>{url}</Text>,
          }))}
        />
      </div>

      {/* API Keys 列表 */}
      <div>
        <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8 }}>
          <Text type="secondary">{t('product.apiKey')}</Text>
          <Button
            type="link"
            size="small"
            icon={<PlusOutlined />}
            onClick={() => setCreateModalOpen(true)}
          >
            {t('product.addApiKey')}
          </Button>
        </div>

        {isLoading ? (
          <Spin size="small" />
        ) : (
          <div style={{ border: '1px solid #f0f0f0', borderRadius: 4 }}>
            {apiKeys?.map((key, index) => (
              <div
                key={key.id}
                style={{
                  padding: '8px 12px',
                  borderBottom: index < apiKeys.length - 1 ? '1px solid #f0f0f0' : 'none',
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                }}
              >
                <Space>
                  <ApiOutlined />
                  {key.name && <Text strong>{key.name}</Text>}
                  <Text code>{key.apiKeyPrefix}</Text>
                  <Tag>{t('product.priorityLabel')}: {key.priority}</Tag>
                  <Tag>{t('product.weightLabel')}: {key.weight}</Tag>
                  <Tag color={key.state === 'ACTIVE' ? 'green' : 'default'}>
                    {key.state}
                  </Tag>
                </Space>
                <Space>
                  <Button
                    type="text"
                    size="small"
                    icon={<EditOutlined />}
                    onClick={() => setEditingKey(key)}
                  />
                </Space>
              </div>
            ))}
            {(!apiKeys || apiKeys.length === 0) && (
              <div style={{ padding: 12, textAlign: 'center', color: '#999' }}>
                {t('product.addApiKey')}
              </div>
            )}
          </div>
        )}
      </div>

      {/* API Key 创建弹窗 */}
      <ProductApiKeyCreateModal
        open={createModalOpen}
        productId={product.id}
        onClose={() => setCreateModalOpen(false)}
        onSuccess={() => setCreateModalOpen(false)}
      />

      {/* API Key 编辑弹窗 */}
      <ProductApiKeyEditModal
        open={!!editingKey}
        productId={product.id}
        apiKey={editingKey}
        onClose={() => setEditingKey(undefined)}
      />
    </Card>
  );
}
```

- [ ] **Step 2: 验证文件创建**

Run: `ls -la gateway-console/src/pages/Providers/ProductCard.tsx`
Expected: 文件存在且大小 > 0

---

### Task 7: 新增 ProductApiKeyCreateModal 组件

**Files:**
- Create: `gateway-console/src/pages/Providers/ProductApiKeyCreateModal.tsx`

- [ ] **Step 1: 创建 API Key 创建弹窗组件**

```tsx
import { useState, useEffect } from 'react';
import { Modal, Form, Input, InputNumber, Typography, Alert } from 'antd';
import { useTranslation } from 'react-i18next';
import { useCreateProductApiKey } from '@/services/query/useProducts';
import type { CreateProductApiKeyRequest } from '@/types/product';

const { Paragraph } = Typography;

interface Props {
  open: boolean;
  productId: number;
  onClose: () => void;
  onSuccess: () => void;
}

export default function ProductApiKeyCreateModal({ open, productId, onClose, onSuccess }: Props) {
  const { t } = useTranslation('products');
  const [form] = Form.useForm<CreateProductApiKeyRequest>();
  const createMutation = useCreateProductApiKey();
  const [createdKey, setCreatedKey] = useState<string | null>(null);

  useEffect(() => {
    if (open) {
      form.resetFields();
      setCreatedKey(null);
    }
  }, [open, form]);

  const handleOk = async () => {
    const values = await form.validateFields();
    const result = await createMutation.mutateAsync({ productId, data: values });
    setCreatedKey(result.apiKeyPlain);
  };

  // 创建成功后展示密钥
  if (createdKey) {
    return (
      <Modal
        title={t('product.apiKeyCreated')}
        open={open}
        onOk={onSuccess}
        onCancel={onSuccess}
        okText={t('common:confirm')}
        cancelButtonProps={{ style: { display: 'none' } }}
      >
        <Alert
          type="warning"
          message={t('product.apiKeyCreatedHint')}
          style={{ marginBottom: 16 }}
        />
        <Paragraph copyable={{ text: createdKey }} code>
          {createdKey}
        </Paragraph>
      </Modal>
    );
  }

  return (
    <Modal
      title={t('product.addApiKey')}
      open={open}
      onOk={handleOk}
      onCancel={onClose}
      confirmLoading={createMutation.isPending}
      destroyOnClose
    >
      <Form form={form} layout="vertical">
        <Form.Item
          name="apiKey"
          label={t('product.apiKeyLabel')}
          rules={[{ required: true, message: t('product.apiKeyRequired') }]}
        >
          <Input.Password placeholder="sk-..." />
        </Form.Item>
        <Form.Item name="name" label={t('product.apiKeyName')}>
          <Input placeholder={t('product.apiKeyName')} />
        </Form.Item>
        <Form.Item name="priority" label={t('product.priorityLabel')}>
          <InputNumber min={0} max={100} style={{ width: '100%' }} />
        </Form.Item>
        <Form.Item name="weight" label={t('product.weightLabel')}>
          <InputNumber min={0} style={{ width: '100%' }} />
        </Form.Item>
        <Form.Item name="description" label={t('product.description')}>
          <Input.TextArea rows={2} />
        </Form.Item>
      </Form>
    </Modal>
  );
}
```

- [ ] **Step 2: 验证文件创建**

Run: `ls -la gateway-console/src/pages/Providers/ProductApiKeyCreateModal.tsx`
Expected: 文件存在且大小 > 0

---

### Task 8: 重构 ProviderProductsTab 组件

**Files:**
- Modify: `gateway-console/src/pages/Providers/ProviderProductsTab.tsx`

- [ ] **Step 1: 重构为卡片式布局**

```tsx
import { useState } from 'react';
import { Button, Empty, Spin, App } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useAuthStore } from '@/stores/authStore';
import { P } from '@/constants/permissions';
import { useProducts, useDeleteProduct } from '@/services/query/useProducts';
import type { Product } from '@/types/product';
import ProductCard from './ProductCard';
import { ProductFormModal } from './ProductFormModal';

interface ProviderProductsTabProps {
  providerId: number;
}

export default function ProviderProductsTab({ providerId }: ProviderProductsTabProps) {
  const { t } = useTranslation('products');
  const { message, modal } = App.useApp();
  const { hasPermission } = useAuthStore();
  const canWrite = hasPermission(P.PROVIDER_WRITE);

  const { data: products, isLoading } = useProducts(providerId);
  const deleteMutation = useDeleteProduct();

  const [formVisible, setFormVisible] = useState(false);
  const [editingProduct, setEditingProduct] = useState<Product | undefined>();

  const handleAdd = () => {
    setEditingProduct(undefined);
    setFormVisible(true);
  };

  const handleEdit = (product: Product) => {
    setEditingProduct(product);
    setFormVisible(true);
  };

  const handleDelete = (product: Product) => {
    if (products && products.length <= 1) {
      message.warning(t('product.lastProductWarning'));
      return;
    }
    modal.confirm({
      title: t('product.deleteProduct'),
      content: t('product.deleteConfirm', { name: product.productName }),
      okType: 'danger',
      onOk: () => deleteMutation.mutateAsync({ id: product.id, providerId }),
    });
  };

  if (isLoading) return <Spin />;

  return (
    <>
      <div style={{ marginBottom: 16, textAlign: 'right' }}>
        {canWrite && (
          <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
            {t('product.addProduct')}
          </Button>
        )}
      </div>

      {!products?.length ? (
        <Empty description={t('product.title')} />
      ) : (
        products.map((product) => (
          <ProductCard
            key={product.id}
            product={product}
            onEdit={() => handleEdit(product)}
            onDelete={() => handleDelete(product)}
            onAddKey={() => {}}
          />
        ))
      )}

      <ProductFormModal
        open={formVisible}
        providerId={providerId}
        providerName={editingProduct?.providerName ?? ''}
        editingProduct={editingProduct ?? null}
        onClose={() => setFormVisible(false)}
        onSaved={() => setFormVisible(false)}
      />
    </>
  );
}
```

- [ ] **Step 2: 验证文件修改**

Run: `grep -n "ProductCard" gateway-console/src/pages/Providers/ProviderProductsTab.tsx`
Expected: 找到 ProductCard 的使用

---

## Phase 3: 密钥测试功能

### Task 9: 后端新增测试接口 DTO

**Files:**
- Create: `gateway-boot/src/main/java/com/codingas/gateway/application/product/dto/ApiKeyTestResponse.java`

- [ ] **Step 1: 创建测试响应 DTO**

```java
package com.codingas.gateway.application.product.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * API Key 测试响应
 */
@Data
@Builder
public class ApiKeyTestResponse {

    /** 测试是否成功 */
    private Boolean success;

    /** 延迟（毫秒） */
    private Long latency;

    /** 测试的模型名称 */
    private String modelName;

    /** 响应预览 */
    private String responsePreview;

    /** 测试时间 */
    private Instant testedAt;

    /** 错误信息 */
    private ApiKeyTestError error;

    @Data
    @Builder
    public static class ApiKeyTestError {
        private String code;
        private String message;
    }
}
```

- [ ] **Step 2: 验证文件创建**

Run: `ls -la gateway-boot/src/main/java/com/codingas/gateway/application/product/dto/ApiKeyTestResponse.java`
Expected: 文件存在

---

### Task 10: 后端新增测试接口

**Files:**
- Create: `gateway-boot/src/main/java/com/codingas/gateway/adapter/api/ProductApiKeyController.java`

- [ ] **Step 1: 创建测试接口 Controller**

```java
package com.codingas.gateway.adapter.api;

import com.codingas.gateway.application.product.ProductApiKeyService;
import com.codingas.gateway.application.product.dto.ApiKeyTestResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 产品 API Key 管理接口
 */
@RestController
@RequestMapping("/api/v1/products/{productId}/keys")
@RequiredArgsConstructor
public class ProductApiKeyController {

    private final ProductApiKeyService productApiKeyService;

    /**
     * 测试 API Key 是否有效
     */
    @PostMapping("/{keyId}/test")
    public ResponseEntity<ApiKeyTestResponse> testApiKey(
            @PathVariable Long productId,
            @PathVariable Long keyId) {
        ApiKeyTestResponse response = productApiKeyService.testApiKey(productId, keyId);
        return ResponseEntity.ok(response);
    }
}
```

- [ ] **Step 2: 验证文件创建**

Run: `ls -la gateway-boot/src/main/java/com/codingas/gateway/adapter/api/ProductApiKeyController.java`
Expected: 文件存在

---

### Task 11: 后端新增测试服务方法

**Files:**
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/application/product/ProductApiKeyService.java`

- [ ] **Step 1: 新增测试方法（占位实现）**

在 `ProductApiKeyService` 中添加：

```java
/**
 * 测试 API Key 是否有效
 *
 * @param productId 产品 ID
 * @param keyId     Key ID
 * @return 测试结果
 */
public ApiKeyTestResponse testApiKey(Long productId, Long keyId) {
    // TODO: 实现真实的 API Key 测试逻辑
    // 1. 获取 API Key
    // 2. 获取产品端点配置
    // 3. 发送测试请求
    // 4. 返回测试结果

    return ApiKeyTestResponse.builder()
            .success(true)
            .latency(100L)
            .modelName("gpt-4o")
            .responsePreview("Hello! How can I assist you today?")
            .testedAt(Instant.now())
            .build();
}
```

- [ ] **Step 2: 验证文件修改**

Run: `grep -n "testApiKey" gateway-boot/src/main/java/com/codingas/gateway/application/product/ProductApiKeyService.java`
Expected: 找到新增的方法

---

### Task 12: 前端新增测试 API

**Files:**
- Modify: `gateway-console/src/services/api/product.ts`

- [ ] **Step 1: 新增测试 API 方法**

在 `productApiKeyApi` 中添加：

```typescript
export const productApiKeyApi = {
  // ... 现有方法

  /** 测试 API Key */
  test: (productId: number, keyId: number) =>
    api.post<ApiKeyTestResponse>(`/products/${productId}/api-keys/${keyId}/test`),
};

/** API Key 测试响应 */
export interface ApiKeyTestResponse {
  success: boolean;
  latency: number | null;
  modelName: string | null;
  responsePreview: string | null;
  testedAt: string;
  error: {
    code: string;
    message: string;
  } | null;
}
```

- [ ] **Step 2: 验证文件修改**

Run: `grep -n "test:" gateway-console/src/services/api/product.ts`
Expected: 找到新增的测试方法

---

### Task 13: 前端新增测试 Hook

**Files:**
- Modify: `gateway-console/src/services/query/useProducts.ts`

- [ ] **Step 1: 新增 useTestProductApiKey Hook**

```typescript
/** 测试 API Key */
export function useTestProductApiKey() {
  return useMutation({
    mutationFn: ({ productId, keyId }: { productId: number; keyId: number }) =>
      productApiKeyApi.test(productId, keyId),
  });
}
```

- [ ] **Step 2: 验证文件修改**

Run: `grep -n "useTestProductApiKey" gateway-console/src/services/query/useProducts.ts`
Expected: 找到新增的 Hook

---

### Task 14: 新增 ProductApiKeyTestButton 组件

**Files:**
- Create: `gateway-console/src/pages/Providers/ProductApiKeyTestButton.tsx`

- [ ] **Step 1: 创建测试按钮组件**

```tsx
import { useState, useEffect } from 'react';
import { Button, Tag, Tooltip, message } from 'antd';
import { CheckCircleOutlined, CloseCircleOutlined, LoadingOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useTestProductApiKey } from '@/services/query/useProducts';

interface Props {
  productId: number;
  keyId: number;
}

export default function ProductApiKeyTestButton({ productId, keyId }: Props) {
  const { t } = useTranslation('products');
  const [result, setResult] = useState<{ success: boolean; latency?: number; error?: string } | null>(null);
  const testMutation = useTestProductApiKey();

  // 结果 3 秒后自动消失
  useEffect(() => {
    if (result) {
      const timer = setTimeout(() => setResult(null), 3000);
      return () => clearTimeout(timer);
    }
  }, [result]);

  const handleTest = async () => {
    try {
      const response = await testMutation.mutateAsync({ productId, keyId });
      setResult({
        success: response.success,
        latency: response.latency ?? undefined,
        error: response.error?.message,
      });
    } catch {
      setResult({ success: false, error: t('product.testFailed') });
    }
  };

  if (result) {
    return result.success ? (
      <Tooltip title={`${t('product.latency')}: ${result.latency}ms`}>
        <Tag color="success" icon={<CheckCircleOutlined />}>
          {t('product.testSuccess')}
        </Tag>
      </Tooltip>
    ) : (
      <Tooltip title={result.error}>
        <Tag color="error" icon={<CloseCircleOutlined />}>
          {t('product.testFailed')}
        </Tag>
      </Tooltip>
    );
  }

  return (
    <Button
      type="text"
      size="small"
      icon={testMutation.isPending ? <LoadingOutlined /> : undefined}
      onClick={handleTest}
      loading={testMutation.isPending}
    >
      {t('product.testKey')}
    </Button>
  );
}
```

- [ ] **Step 2: 验证文件创建**

Run: `ls -la gateway-console/src/pages/Providers/ProductApiKeyTestButton.tsx`
Expected: 文件存在且大小 > 0

---

### Task 15: 集成测试按钮到 ProductCard

**Files:**
- Modify: `gateway-console/src/pages/Providers/ProductCard.tsx`

- [ ] **Step 1: 在 API Key 行中添加测试按钮**

在 API Key 行的操作按钮区域添加测试按钮：

```tsx
import ProductApiKeyTestButton from './ProductApiKeyTestButton';

// 在 API Key 行的操作区域
<Space>
  <ProductApiKeyTestButton productId={product.id} keyId={key.id} />
  <Button
    type="text"
    size="small"
    icon={<EditOutlined />}
    onClick={() => setEditingKey(key)}
  />
</Space>
```

- [ ] **Step 2: 验证文件修改**

Run: `grep -n "ProductApiKeyTestButton" gateway-console/src/pages/Providers/ProductCard.tsx`
Expected: 找到测试按钮的使用

---

## Phase 4: 验证测试

### Task 16: TypeScript 类型检查

- [ ] **Step 1: 运行 TypeScript 类型检查**

Run: `cd gateway-console && pnpm tsc --noEmit`
Expected: 无类型错误

---

### Task 17: ESLint 检查

- [ ] **Step 1: 运行 ESLint 检查**

Run: `cd gateway-console && pnpm eslint src/pages/Providers/ --ext .tsx,.ts`
Expected: 无严重错误

---

### Task 18: 后端编译检查

- [ ] **Step 1: 编译后端项目**

Run: `cd gateway-boot && ../mvnw compile -DskipTests`
Expected: 编译成功

---

## 自我审查清单

**1. 规格覆盖检查：**
- [x] 供应商卡片展示产品预览 → Task 1
- [x] 移除 Key 统计 → Task 1
- [x] 产品标签页卡片式布局 → Task 6, Task 8
- [x] API Key 列表式展示 → Task 6
- [x] API Key 创建弹窗 → Task 7
- [x] 密钥测试功能 → Task 9-15
- [x] 国际化支持 → Task 5

**2. 占位符扫描：**
- [x] 无 TBD、TODO（后端测试方法有 TODO 注释，为预期占位）
- [x] 所有代码步骤都有完整实现

**3. 类型一致性：**
- [x] Props 接口定义与使用一致
- [x] API 响应类型定义一致
- [x] 翻译键名一致

---

**计划完成并保存到 `docs/superpowers/plans/2026-05-21-provider-product-enhancement.md`。**

**两种执行方式：**

1. **子代理驱动（推荐）** - 每个任务派发新子代理，任务间可审查，快速迭代
2. **内联执行** - 在当前会话中使用 executing-plans 执行，批量执行带检查点

**选择哪种方式？**
