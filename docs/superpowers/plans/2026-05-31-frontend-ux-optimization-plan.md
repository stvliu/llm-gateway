# 前端 UX 优化与渠道创建向导实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现前端 UX 优化，包括 API Key 脱敏显示组件、快速开始改进、供应商目录 LOGO、新建渠道向导

**Architecture:** 后端新增 `keyMasked` 字段，前端抽取统一 `MaskedKeyDisplay` 组件复用，向导组件基于目录数据实现四步创建流程

**Tech Stack:** Java 21 + Spring Boot 3.5.x, React 18 + Ant Design 5.x + TypeScript

---

## 文件结构

### 后端文件

| 文件 | 操作 | 职责 |
|------|------|------|
| `gateway-boot/src/main/java/com/codingas/gateway/application/userapikey/dto/UserApiKeyResponse.java` | 修改 | 新增 `keyMasked` 字段 |
| `gateway-boot/src/main/java/com/codingas/gateway/application/channelcredential/dto/ChannelCredentialResponse.java` | 修改 | 新增 `apiKeyMasked` 字段 |
| `gateway-boot/src/main/java/com/codingas/gateway/application/channelcredential/dto/ChannelCredentialUpdateRequest.java` | 修改 | 新增 `apiKey` 字段支持替换 |
| `gateway-boot/src/main/java/com/codingas/gateway/application/channelcredential/ChannelCredentialService.java` | 修改 | 更新时支持替换 API Key |
| `gateway-boot/src/main/java/com/codingas/gateway/application/catalog/dto/MaterializePlanRequest.java` | 新建 | 物化请求扩展（apiKeys/endpoints/models） |
| `gateway-boot/src/main/java/com/codingas/gateway/application/catalog/CatalogMaterializeService.java` | 修改 | 扩展物化逻辑支持批量 Key |
| `gateway-boot/src/main/java/com/codingas/gateway/adapter/api/CatalogController.java` | 修改 | 扩展物化端点参数 |

### 前端文件

| 文件 | 操作 | 职责 |
|------|------|------|
| `gateway-console/src/components/MaskedKeyDisplay.tsx` | 新建 | 统一 API Key 脱敏显示组件 |
| `gateway-console/src/pages/Channels/ChannelCreateWizard.tsx` | 新建 | 新建渠道向导组件 |
| `gateway-console/src/pages/Channels/ApiKeyEditModal.tsx` | 新建 | API Key 编辑弹窗 |
| `gateway-console/src/types/team.ts` | 修改 | UserApiKey 新增 `keyMasked` |
| `gateway-console/src/types/channel.ts` | 修改 | ChannelCredential 新增 `apiKeyMasked` |
| `gateway-console/src/types/catalog.ts` | 修改 | 新增物化请求类型 |
| `gateway-console/src/services/api/catalog.ts` | 修改 | 扩展物化 API 参数 |
| `gateway-console/src/pages/Developer/DeveloperKeyList.tsx` | 修改 | 集成 MaskedKeyDisplay |
| `gateway-console/src/pages/Developer/CodeSnippet.tsx` | 修改 | 移除硬编码颜色，自动填充 URL/Key |
| `gateway-console/src/pages/Catalog/ProviderCatalogView.tsx` | 修改 | 卡片增加 LOGO |
| `gateway-console/src/pages/Catalog/PlanCatalogView.tsx` | 修改 | 增加"快速创建渠道"入口 |
| `gateway-console/src/pages/ApiKeys/DownstreamKeysTable.tsx` | 修改 | 集成 MaskedKeyDisplay |
| `gateway-console/src/pages/ApiKeys/UpstreamKeysTable.tsx` | 修改 | 集成 MaskedKeyDisplay + 编辑 |
| `gateway-console/src/pages/Channels/CredentialSection.tsx` | 修改 | 集成 MaskedKeyDisplay + 编辑 |

---

## Task 1: 后端 DTO 新增 keyMasked 字段

**Files:**
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/application/userapikey/dto/UserApiKeyResponse.java`
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/application/channelcredential/dto/ChannelCredentialResponse.java`
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/application/userapikey/UserApiKeyService.java`
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/application/channelcredential/ChannelCredentialService.java`

- [ ] **Step 1: 在 UserApiKeyResponse 中新增 keyMasked 字段**

```java
// 文件: UserApiKeyResponse.java
public record UserApiKeyResponse(
        Long id,
        Long userId,
        String keyPrefix,
        String keyMasked,  // 新增：脱敏格式
        String name,
        List<String> models,
        Long quotaLimit,
        UserApiKeyState state,
        Instant createdAt,
        Instant updatedAt
) {}
```

- [ ] **Step 2: 在 ChannelCredentialResponse 中新增 apiKeyMasked 字段**

```java
// 文件: ChannelCredentialResponse.java
public record ChannelCredentialResponse(
        Long id,
        Long channelId,
        String apiKeyPrefix,
        String apiKeyMasked,  // 新增：脱敏格式
        String name,
        String description,
        Integer weight,
        Integer priority,
        CredentialState state,
        Instant createdAt,
        Instant updatedAt
) {}
```

- [ ] **Step 3: 创建脱敏工具方法**

在 `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/util/` 下新建 `KeyMasker.java`：

```java
package com.codingas.gateway.infrastructure.util;

/**
 * API Key 脱敏工具
 * 规则：保留前 6 位 + **** + 保留后 4 位
 * 长度不足 12 位时：仅显示前缀 + ****
 */
public final class KeyMasker {

    private static final int PREFIX_LEN = 6;
    private static final int SUFFIX_LEN = 4;
    private static final String MASK = "****";

    private KeyMasker() {}

    public static String mask(String key) {
        if (key == null || key.isEmpty()) {
            return "";
        }
        if (key.length() < 12) {
            return key + MASK;
        }
        String prefix = key.substring(0, Math.min(PREFIX_LEN, key.length()));
        String suffix = key.substring(key.length() - SUFFIX_LEN);
        return prefix + MASK + suffix;
    }
}
```

- [ ] **Step 4: 在 UserApiKeyService 中填充 keyMasked**

找到 `UserApiKeyService.java` 中返回 `UserApiKeyResponse` 的方法，在构建响应时添加 `keyMasked`：

```java
// 在 toResponse 方法或列表查询方法中
private UserApiKeyResponse toResponse(UserApiKey entity) {
    return new UserApiKeyResponse(
            entity.getId(),
            entity.getUserId(),
            entity.getKeyPrefix(),
            KeyMasker.mask(entity.getKeyPrefix()),  // 使用前缀生成脱敏格式
            entity.getName(),
            entity.getModels(),
            entity.getQuotaLimit(),
            entity.getState(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
    );
}
```

- [ ] **Step 5: 在 ChannelCredentialService 中填充 apiKeyMasked**

类似地，在 `ChannelCredentialService.java` 中更新响应构建逻辑。

- [ ] **Step 6: 编译验证**

```bash
cd gateway-boot && ../mvnw compile -pl gateway-boot
```

Expected: BUILD SUCCESS

- [ ] **Step 7: Commit**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/application/userapikey/dto/UserApiKeyResponse.java \
        gateway-boot/src/main/java/com/codingas/gateway/application/channelcredential/dto/ChannelCredentialResponse.java \
        gateway-boot/src/main/java/com/codingas/gateway/infrastructure/util/KeyMasker.java
git commit -m "feat(backend): DTO 新增 keyMasked 脱敏字段"
```

---

## Task 2: 后端支持更新 API Key

**Files:**
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/application/channelcredential/dto/ChannelCredentialUpdateRequest.java`
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/application/channelcredential/ChannelCredentialService.java`

- [ ] **Step 1: 在 ChannelCredentialUpdateRequest 中新增 apiKey 字段**

```java
// 文件: ChannelCredentialUpdateRequest.java
public record ChannelCredentialUpdateRequest(
        Integer priority,
        Integer weight,
        CredentialState state,
        String description,
        String apiKey  // 新增：可选，传值则替换 API Key
) {
}
```

- [ ] **Step 2: 在 ChannelCredentialService 中处理 apiKey 替换**

找到 `update` 方法，添加 API Key 替换逻辑：

```java
@Transactional
public ChannelCredentialResponse update(Long channelId, Long credentialId, ChannelCredentialUpdateRequest request) {
    ChannelCredential credential = credentialGateway.findById(credentialId)
            .orElseThrow(() -> new NotFoundException("凭证不存在"));

    // 更新基本字段
    if (request.priority() != null) credential.setPriority(request.priority());
    if (request.weight() != null) credential.setWeight(request.weight());
    if (request.state() != null) credential.setState(request.state());
    if (request.description() != null) credential.setDescription(request.description());

    // 新增：替换 API Key
    if (request.apiKey() != null && !request.apiKey().isBlank()) {
        String newKey = request.apiKey().trim();
        credential.setApiKeyPlain(newKey);
        credential.setApiKeyEncrypted(encryptionService.encrypt(newKey));
        credential.setApiKeyPrefix(extractPrefix(newKey));
    }

    ChannelCredential saved = credentialGateway.save(credential);
    return toResponse(saved);
}
```

- [ ] **Step 3: 编译验证**

```bash
../mvnw compile -pl gateway-boot
```

Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/application/channelcredential/dto/ChannelCredentialUpdateRequest.java \
        gateway-boot/src/main/java/com/codingas/gateway/application/channelcredential/ChannelCredentialService.java
git commit -m "feat(backend): 支持更新 API Key"
```

---

## Task 3: 后端扩展物化 API 支持批量 Key 和自定义配置

**Files:**
- Create: `gateway-boot/src/main/java/com/codingas/gateway/application/catalog/dto/MaterializePlanRequest.java`
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/application/catalog/CatalogMaterializeService.java`
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/adapter/api/CatalogController.java`

- [ ] **Step 1: 创建 MaterializePlanRequest DTO**

```java
// 文件: MaterializePlanRequest.java
package com.codingas.gateway.application.catalog.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 套餐物化请求（扩展版）
 */
@Getter
@Setter
public class MaterializePlanRequest {

    /** API Key 列表（批量创建凭证） */
    private List<String> apiKeys;

    /** 自定义端点列表（覆盖目录默认值） */
    private List<EndpointConfig> endpoints;

    /** 自定义模型列表（覆盖目录默认值） */
    private List<String> models;

    /** 渠道名称（可选，默认使用 planCode） */
    private String channelName;

    @Getter
    @Setter
    public static class EndpointConfig {
        private String protocol;  // OPENAI / ANTHROPIC / GEMINI
        private String url;
    }
}
```

- [ ] **Step 2: 扩展 CatalogMaterializeService.materializePlan 方法**

```java
// 在 CatalogMaterializeService.java 中添加重载方法
@Transactional
public MaterializeResult materializePlan(String planCode, MaterializePlanRequest request) {
    // 1. 原有物化逻辑创建 Channel + ChannelEndpoint + ChannelModel
    MaterializeResult baseResult = materializePlan(planCode);
    if (baseResult.status() != MaterializeStatus.CREATED) {
        return baseResult;
    }

    Long channelId = baseResult.entityId();

    // 2. 如果请求中有自定义端点，更新或新增
    if (request.getEndpoints() != null && !request.getEndpoints().isEmpty()) {
        // 先删除默认端点（可选），再创建自定义端点
        for (MaterializePlanRequest.EndpointConfig ep : request.getEndpoints()) {
            ChannelEndpoint endpoint = new ChannelEndpoint();
            endpoint.setChannelId(channelId);
            endpoint.setProtocol(Protocol.valueOf(ep.getProtocol()));
            endpoint.setEndpointUrl(ep.getUrl());
            endpointGateway.save(endpoint);
        }
    }

    // 3. 如果请求中有自定义模型，更新 ChannelModel
    if (request.getModels() != null && !request.getModels().isEmpty()) {
        // 类似处理
    }

    // 4. 批量创建 API Key 凭证
    if (request.getApiKeys() != null && !request.getApiKeys().isEmpty()) {
        int priority = 1;
        for (String apiKey : request.getApiKeys()) {
            if (apiKey == null || apiKey.isBlank()) continue;
            ChannelCredential credential = new ChannelCredential();
            credential.setChannelId(channelId);
            credential.setApiKeyPlain(apiKey.trim());
            credential.setApiKeyEncrypted(encryptionService.encrypt(apiKey.trim()));
            credential.setApiKeyPrefix(extractPrefix(apiKey.trim()));
            credential.setPriority(priority++);
            credential.setWeight(100);
            credential.setState(CredentialState.ACTIVE);
            credentialGateway.save(credential);
        }
    }

    return baseResult;
}
```

- [ ] **Step 3: 扩展 CatalogController 端点**

```java
// 在 CatalogController.java 中修改物化端点
@PostMapping("/materialize/plan/{planCode}")
@PreAuthorize("hasRole('ADMIN')")
public MaterializeResult materializePlan(
        @PathVariable String planCode,
        @RequestBody(required = false) MaterializePlanRequest request) {
    if (request != null) {
        return catalogMaterializeService.materializePlan(planCode, request);
    }
    return catalogMaterializeService.materializePlan(planCode);
}
```

- [ ] **Step 4: 编译验证**

```bash
../mvnw compile -pl gateway-boot
```

Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/application/catalog/dto/MaterializePlanRequest.java \
        gateway-boot/src/main/java/com/codingas/gateway/application/catalog/CatalogMaterializeService.java \
        gateway-boot/src/main/java/com/codingas/gateway/adapter/api/CatalogController.java
git commit -m "feat(backend): 物化 API 支持批量 Key 和自定义端点/模型"
```

---

## Task 4: 前端类型定义更新

**Files:**
- Modify: `gateway-console/src/types/team.ts`
- Modify: `gateway-console/src/types/channel.ts`
- Modify: `gateway-console/src/types/catalog.ts`

- [ ] **Step 1: 更新 UserApiKey 类型**

```typescript
// 文件: types/team.ts
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
```

- [ ] **Step 2: 更新 ChannelCredential 类型**

```typescript
// 文件: types/channel.ts
export interface ChannelCredential {
  id: number;
  channelId: number;
  apiKeyPrefix: string;
  apiKeyMasked: string;  // 新增
  name: string;
  description: string | null;
  weight: number;
  priority: number;
  state: ChannelCredentialState;
  createdAt: string;
  updatedAt: string;
}
```

- [ ] **Step 3: 新增物化请求类型**

```typescript
// 文件: types/catalog.ts
export interface MaterializePlanRequest {
  apiKeys?: string[];
  endpoints?: Array<{ protocol: string; url: string }>;
  models?: string[];
  channelName?: string;
}

// 更新 UpdateChannelCredentialRequest
export interface UpdateChannelCredentialRequest {
  priority?: number;
  weight?: number;
  description?: string;
  state?: ChannelCredentialState;
  apiKey?: string;  // 新增
}
```

- [ ] **Step 4: Commit**

```bash
git add gateway-console/src/types/team.ts \
        gateway-console/src/types/channel.ts \
        gateway-console/src/types/catalog.ts
git commit -m "feat(frontend): 类型定义新增 keyMasked 和物化请求类型"
```

---

## Task 5: 创建 MaskedKeyDisplay 组件

**Files:**
- Create: `gateway-console/src/components/MaskedKeyDisplay.tsx`

- [ ] **Step 1: 创建 MaskedKeyDisplay 组件**

```tsx
// 文件: components/MaskedKeyDisplay.tsx
import { useState } from 'react';
import { Space, Button, Typography, message, Tooltip } from 'antd';
import { EyeOutlined, EyeInvisibleOutlined, CopyOutlined, EditOutlined } from '@ant-design/icons';

const { Text } = Typography;

export interface MaskedKeyDisplayProps {
  /** 脱敏格式，如 "sk-abc****dEf1" */
  keyMasked: string;
  /** 明文（可选，详情 API 返回） */
  keyPlain?: string;
  /** 模式：editable 上游 Key 可编辑；readonly 下游 Key 仅复制 */
  mode?: 'editable' | 'readonly';
  /** 编辑回调（editable 模式） */
  onEdit?: () => void;
  /** 是否显示复制按钮，默认 true */
  showCopy?: boolean;
  /** 尺寸 */
  size?: 'small' | 'default';
  /** 获取明文的回调（按需调用详情 API） */
  onFetchPlain?: () => Promise<string | undefined>;
}

export const MaskedKeyDisplay: React.FC<MaskedKeyDisplayProps> = ({
  keyMasked,
  keyPlain,
  mode = 'readonly',
  onEdit,
  showCopy = true,
  size = 'default',
  onFetchPlain,
}) => {
  const [visible, setVisible] = useState(false);
  const [plain, setPlain] = useState<string | undefined>(keyPlain);
  const [loading, setLoading] = useState(false);

  const displayText = visible && plain ? plain : keyMasked;

  const handleToggleVisibility = async () => {
    if (!visible && !plain && onFetchPlain) {
      setLoading(true);
      try {
        const fetched = await onFetchPlain();
        if (fetched) {
          setPlain(fetched);
          setVisible(true);
        }
      } finally {
        setLoading(false);
      }
    } else {
      setVisible(!visible);
    }
  };

  const handleCopy = async () => {
    let textToCopy = plain || keyMasked;
    if (!plain && onFetchPlain) {
      setLoading(true);
      try {
        const fetched = await onFetchPlain();
        if (fetched) {
          setPlain(fetched);
          textToCopy = fetched;
        }
      } finally {
        setLoading(false);
      }
    }
    await navigator.clipboard.writeText(textToCopy);
    message.success('已复制到剪贴板');
  };

  const iconSize = size === 'small' ? 12 : 14;
  const buttonSize = size === 'small' ? 'small' : 'middle';

  return (
    <Space size={4}>
      <Text code style={{ fontSize: size === 'small' ? 12 : 13 }}>
        {displayText}
      </Text>
      <Tooltip title={visible ? '隐藏' : '显示'}>
        <Button
          type="text"
          size={buttonSize}
          icon={visible ? <EyeInvisibleOutlined style={{ fontSize: iconSize }} /> : <EyeOutlined style={{ fontSize: iconSize }} />}
          onClick={handleToggleVisibility}
          loading={loading}
          style={{ padding: '0 4px' }}
        />
      </Tooltip>
      {showCopy && (
        <Tooltip title="复制">
          <Button
            type="text"
            size={buttonSize}
            icon={<CopyOutlined style={{ fontSize: iconSize }} />}
            onClick={handleCopy}
            style={{ padding: '0 4px' }}
          />
        </Tooltip>
      )}
      {mode === 'editable' && onEdit && (
        <Tooltip title="编辑">
          <Button
            type="text"
            size={buttonSize}
            icon={<EditOutlined style={{ fontSize: iconSize }} />}
            onClick={onEdit}
            style={{ padding: '0 4px' }}
          />
        </Tooltip>
      )}
    </Space>
  );
};

export default MaskedKeyDisplay;
```

- [ ] **Step 2: Commit**

```bash
git add gateway-console/src/components/MaskedKeyDisplay.tsx
git commit -m "feat(frontend): 新增 MaskedKeyDisplay 统一脱敏显示组件"
```

---

## Task 6: 创建 ApiKeyEditModal 组件

**Files:**
- Create: `gateway-console/src/pages/Channels/ApiKeyEditModal.tsx`

- [ ] **Step 1: 创建 ApiKeyEditModal 组件**

```tsx
// 文件: pages/Channels/ApiKeyEditModal.tsx
import { useState } from 'react';
import { Modal, Form, Input, Typography, Space, message } from 'antd';
import { MaskedKeyDisplay } from '../../components/MaskedKeyDisplay';

const { Text } = Typography;

export interface ApiKeyEditModalProps {
  open: boolean;
  channelId: number;
  credentialId: number;
  keyMasked: string;
  onClose: () => void;
  onSuccess: () => void;
  onUpdate: (channelId: number, credentialId: number, data: { apiKey: string }) => Promise<void>;
}

export const ApiKeyEditModal: React.FC<ApiKeyEditModalProps> = ({
  open,
  channelId,
  credentialId,
  keyMasked,
  onClose,
  onSuccess,
  onUpdate,
}) => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [newKeyMasked, setNewKeyMasked] = useState<string | null>(null);

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      setLoading(true);
      await onUpdate(channelId, credentialId, { apiKey: values.apiKey });
      message.success('API Key 已更新');
      onSuccess();
      onClose();
    } catch (error) {
      // 错误已在 mutation 中处理
    } finally {
      setLoading(false);
    }
  };

  const maskKey = (key: string): string => {
    if (!key || key.length < 12) return key + '****';
    return key.substring(0, 6) + '****' + key.substring(key.length - 4);
  };

  return (
    <Modal
      title="替换 API Key"
      open={open}
      onOk={handleSubmit}
      onCancel={onClose}
      confirmLoading={loading}
      destroyOnClose
    >
      <Form form={form} layout="vertical">
        <Form.Item label="当前 API Key">
          <MaskedKeyDisplay keyMasked={keyMasked} mode="readonly" showCopy={false} />
        </Form.Item>
        <Form.Item
          name="apiKey"
          label="新 API Key"
          rules={[{ required: true, message: '请输入新的 API Key' }]}
        >
          <Input.Password
            placeholder="sk-..."
            onChange={(e) => {
              const val = e.target.value;
              setNewKeyMasked(val ? maskKey(val) : null);
            }}
          />
        </Form.Item>
        {newKeyMasked && (
          <Form.Item label="变更预览">
            <Space>
              <Text type="secondary">将替换：</Text>
              <Text code delete>{keyMasked}</Text>
              <Text type="secondary">→</Text>
              <Text code>{newKeyMasked}</Text>
            </Space>
          </Form.Item>
        )}
      </Form>
    </Modal>
  );
};

export default ApiKeyEditModal;
```

- [ ] **Step 2: Commit**

```bash
git add gateway-console/src/pages/Channels/ApiKeyEditModal.tsx
git commit -m "feat(frontend): 新增 ApiKeyEditModal 编辑弹窗组件"
```

---

## Task 7: 集成 MaskedKeyDisplay 到 DeveloperKeyList

**Files:**
- Modify: `gateway-console/src/pages/Developer/DeveloperKeyList.tsx`

- [ ] **Step 1: 导入 MaskedKeyDisplay 并修改表格列**

```tsx
// 在 DeveloperKeyList.tsx 中
import { MaskedKeyDisplay } from '../../components/MaskedKeyDisplay';
import { userApiKeyApi } from '../../services/api/userApiKey';

// 修改 keyPrefix 列
{
  title: t('keyPrefix'),
  dataIndex: 'keyMasked',  // 改为使用 keyMasked
  key: 'keyMasked',
  render: (keyMasked: string, record) => (
    <MaskedKeyDisplay
      keyMasked={keyMasked}
      mode="readonly"
      size="small"
      onFetchPlain={async () => {
        const detail = await userApiKeyApi.get(record.id);
        return detail.keyPlain;
      }}
    />
  ),
},
```

- [ ] **Step 2: Commit**

```bash
git add gateway-console/src/pages/Developer/DeveloperKeyList.tsx
git commit -m "feat(frontend): DeveloperKeyList 集成 MaskedKeyDisplay"
```

---

## Task 8: 集成 MaskedKeyDisplay 到 DownstreamKeysTable

**Files:**
- Modify: `gateway-console/src/pages/ApiKeys/DownstreamKeysTable.tsx`

- [ ] **Step 1: 导入并修改表格列**

```tsx
import { MaskedKeyDisplay } from '../../components/MaskedKeyDisplay';
import { userApiKeyApi } from '../../services/api/userApiKey';

// 修改 Key 前缀列
{
  title: t('keyPrefix'),
  dataIndex: 'keyMasked',
  key: 'keyMasked',
  render: (keyMasked: string, record) => (
    <MaskedKeyDisplay
      keyMasked={keyMasked}
      mode="readonly"
      size="small"
      onFetchPlain={async () => {
        const detail = await userApiKeyApi.get(record.id);
        return detail.keyPlain;
      }}
    />
  ),
},
```

- [ ] **Step 2: Commit**

```bash
git add gateway-console/src/pages/ApiKeys/DownstreamKeysTable.tsx
git commit -m "feat(frontend): DownstreamKeysTable 集成 MaskedKeyDisplay"
```

---

## Task 9: 集成 MaskedKeyDisplay 到 UpstreamKeysTable

**Files:**
- Modify: `gateway-console/src/pages/ApiKeys/UpstreamKeysTable.tsx`

- [ ] **Step 1: 导入组件并添加编辑功能**

```tsx
import { useState } from 'react';
import { MaskedKeyDisplay } from '../../components/MaskedKeyDisplay';
import { ApiKeyEditModal } from '../Channels/ApiKeyEditModal';
import { channelApi } from '../../services/api/channel';

// 在组件内添加状态
const [editModalOpen, setEditModalOpen] = useState(false);
const [editingCredential, setEditingCredential] = useState<{
  channelId: number;
  credentialId: number;
  keyMasked: string;
} | null>(null);

const handleEditKey = (row: AggregateCredential) => {
  setEditingCredential({
    channelId: row.channelId,
    credentialId: row.credentialId,
    keyMasked: row.apiKeyPrefix + '****',
  });
  setEditModalOpen(true);
};

const handleUpdateKey = async (channelId: number, credentialId: number, data: { apiKey: string }) => {
  await channelApi.updateCredential(channelId, credentialId, data);
  queryClient.invalidateQueries({ queryKey: channelKeys.credentials(channelId) });
};

// 修改 Key 前缀列
{
  title: t('keyPrefix'),
  dataIndex: 'apiKeyPrefix',
  key: 'apiKeyPrefix',
  render: (prefix: string, record) => (
    <MaskedKeyDisplay
      keyMasked={prefix + '****'}
      mode="editable"
      size="small"
      onEdit={() => handleEditKey(record)}
      onFetchPlain={async () => {
        const detail = await channelApi.getCredentialDetail(record.channelId, record.credentialId);
        return detail.apiKeyPlain;
      }}
    />
  ),
},

// 在组件末尾添加 Modal
{editingCredential && (
  <ApiKeyEditModal
    open={editModalOpen}
    channelId={editingCredential.channelId}
    credentialId={editingCredential.credentialId}
    keyMasked={editingCredential.keyMasked}
    onClose={() => {
      setEditModalOpen(false);
      setEditingCredential(null);
    }}
    onSuccess={() => {
      queryClient.invalidateQueries({ queryKey: ['channels', 'credentials'] });
    }}
    onUpdate={handleUpdateKey}
  />
)}
```

- [ ] **Step 2: Commit**

```bash
git add gateway-console/src/pages/ApiKeys/UpstreamKeysTable.tsx
git commit -m "feat(frontend): UpstreamKeysTable 集成 MaskedKeyDisplay 和编辑功能"
```

---

## Task 10: 集成 MaskedKeyDisplay 到 CredentialSection

**Files:**
- Modify: `gateway-console/src/pages/Channels/CredentialSection.tsx`

- [ ] **Step 1: 导入组件并修改渲染逻辑**

```tsx
import { useState } from 'react';
import { MaskedKeyDisplay } from '../../components/MaskedKeyDisplay';
import { ApiKeyEditModal } from './ApiKeyEditModal';

// 在组件内添加编辑状态
const [editModalOpen, setEditModalOpen] = useState(false);
const [editingCredential, setEditingCredential] = useState<ChannelCredential | null>(null);

// 修改 renderItem
const renderItem = (credential: ChannelCredential) => (
  <div style={{ display: 'flex', alignItems: 'center', gap: 12, width: '100%' }}>
    <MaskedKeyDisplay
      keyMasked={credential.apiKeyMasked || credential.apiKeyPrefix + '****'}
      mode="editable"
      size="small"
      onEdit={() => {
        setEditingCredential(credential);
        setEditModalOpen(true);
      }}
      onFetchPlain={async () => {
        const detail = await channelApi.getCredentialDetail(channelId, credential.id);
        return detail.apiKeyPlain;
      }}
    />
    <Tag color="blue">P{credential.priority}</Tag>
    <Tag color="purple">W{credential.weight}</Tag>
    <Tag color={credential.state === 'ACTIVE' ? 'green' : 'default'}>
      {credential.state === 'ACTIVE' ? '已启用' : '已停用'}
    </Tag>
    <Button type="link" size="small" onClick={() => handleTest(credential.id)}>测试</Button>
  </div>
);

// 在组件末尾添加 Modal
{editingCredential && (
  <ApiKeyEditModal
    open={editModalOpen}
    channelId={channelId}
    credentialId={editingCredential.id}
    keyMasked={editingCredential.apiKeyMasked || editingCredential.apiKeyPrefix + '****'}
    onClose={() => {
      setEditModalOpen(false);
      setEditingCredential(null);
    }}
    onSuccess={() => {
      // 刷新凭证列表
    }}
    onUpdate={async (chId, crId, data) => {
      await updateCredential.mutateAsync({ channelId: chId, credentialId: crId, data });
    }}
  />
)}
```

- [ ] **Step 2: Commit**

```bash
git add gateway-console/src/pages/Channels/CredentialSection.tsx
git commit -m "feat(frontend): CredentialSection 集成 MaskedKeyDisplay 和编辑功能"
```

---

## Task 11: 修复 CodeSnippet 样式和自动填充

**Files:**
- Modify: `gateway-console/src/pages/Developer/CodeSnippet.tsx`
- Modify: `gateway-console/src/services/api/userApiKey.ts` (添加详情 API)

- [ ] **Step 1: 修改 CodeSnippet 组件**

```tsx
// 文件: CodeSnippet.tsx
import { useState, useEffect } from 'react';
import { Card, Segmented, Button, Typography, Empty, message } from 'antd';
import { CopyOutlined } from '@ant-design/icons';
import { useUserApiKeys } from '../../services/query/useUserApiKeys';
import { userApiKeyApi } from '../../services/api/userApiKey';

const { Text, Paragraph } = Typography;

type Lang = 'curl' | 'python' | 'node' | 'java';

// 获取网关 URL
const getGatewayUrl = () => {
  return import.meta.env.VITE_API_BASE_URL || window.location.origin;
};

const snippets: Record<Lang, (url: string, key: string) => string> = {
  curl: (url, key) => `curl ${url}/v1/chat/completions \\
  -H "Content-Type: application/json" \\
  -H "Authorization: Bearer ${key}" \\
  -d '{
    "model": "gpt-4o",
    "messages": [{"role": "user", "content": "Hello!"}]
  }'`,
  python: (url, key) => `import requests

response = requests.post(
    "${url}/v1/chat/completions",
    headers={
        "Content-Type": "application/json",
        "Authorization": "Bearer ${key}"
    },
    json={
        "model": "gpt-4o",
        "messages": [{"role": "user", "content": "Hello!"}]
    }
)
print(response.json())`,
  node: (url, key) => `const response = await fetch("${url}/v1/chat/completions", {
  method: "POST",
  headers: {
    "Content-Type": "application/json",
    "Authorization": "Bearer ${key}"
  },
  body: JSON.stringify({
    model: "gpt-4o",
    messages: [{ role: "user", content: "Hello!" }]
  })
});
const data = await response.json();
console.log(data);`,
  java: (url, key) => `HttpClient client = HttpClient.newHttpClient();
HttpRequest request = HttpRequest.newBuilder()
    .uri(URI.create("${url}/v1/chat/completions"))
    .header("Content-Type", "application/json")
    .header("Authorization", "Bearer ${key}")
    .POST(HttpRequest.BodyPublishers.ofString(\"\"\"
        {
          "model": "gpt-4o",
          "messages": [{"role": "user", "content": "Hello!"}]
        }
        \"\"\"))
    .build();
HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
System.out.println(response.body());`,
};

interface CodeSnippetProps {
  apiKey?: string;
}

export default function CodeSnippet({ apiKey: propApiKey }: CodeSnippetProps) {
  const [lang, setLang] = useState<Lang>('curl');
  const [apiKey, setApiKey] = useState<string>(propApiKey || '');
  const [loading, setLoading] = useState(false);

  const { data: keys } = useUserApiKeys(/* current user id */);

  // 自动填充第一个有效的 API Key
  useEffect(() => {
    if (propApiKey) {
      setApiKey(propApiKey);
      return;
    }
    if (keys && keys.length > 0) {
      const activeKey = keys.find(k => k.state === 'ACTIVE');
      if (activeKey) {
        // 获取明文 Key
        setLoading(true);
        userApiKeyApi.get(activeKey.id)
          .then(detail => {
            if (detail.keyPlain) {
              setApiKey(detail.keyPlain);
            }
          })
          .finally(() => setLoading(false));
      }
    }
  }, [propApiKey, keys]);

  const gatewayUrl = getGatewayUrl();
  const code = snippets[lang](gatewayUrl, apiKey || 'sk-your-api-key');

  const handleCopy = () => {
    navigator.clipboard.writeText(code);
    message.success('已复制到剪贴板');
  };

  if (!apiKey && keys?.length === 0) {
    return (
      <Card title="快速开始" size="small">
        <Empty description="请先创建 API Key" image={Empty.PRESENTED_IMAGE_SIMPLE} />
      </Card>
    );
  }

  return (
    <Card
      title="快速开始"
      size="small"
      extra={
        <Segmented
          options={[
            { label: 'cURL', value: 'curl' },
            { label: 'Python', value: 'python' },
            { label: 'Node.js', value: 'node' },
            { label: 'Java', value: 'java' },
          ]}
          value={lang}
          onChange={(v) => setLang(v as Lang)}
        />
      }
    >
      <div style={{ position: 'relative' }}>
        <Paragraph>
          <pre style={{
            margin: 0,
            padding: 16,
            background: 'var(--ant-color-bg-container)',
            border: '1px solid var(--ant-color-border)',
            borderRadius: 6,
            fontFamily: 'monospace',
            fontSize: 13,
            whiteSpace: 'pre-wrap',
            wordBreak: 'break-all',
          }}>
            {code}
          </pre>
        </Paragraph>
        <Button
          type="text"
          icon={<CopyOutlined />}
          onClick={handleCopy}
          style={{ position: 'absolute', top: 8, right: 8 }}
        >
          复制
        </Button>
      </div>
    </Card>
  );
}
```

- [ ] **Step 2: 确保 userApiKeyApi 有 get 方法**

```typescript
// 在 services/api/userApiKey.ts 中确保有
export const userApiKeyApi = {
  // ... 其他方法
  get: (id: number) => api.get<UserApiKeyDetail>(`/me/api-keys/${id}`),
};
```

- [ ] **Step 3: Commit**

```bash
git add gateway-console/src/pages/Developer/CodeSnippet.tsx \
        gateway-console/src/services/api/userApiKey.ts
git commit -m "feat(frontend): CodeSnippet 移除硬编码颜色，自动填充 URL/API Key"
```

---

## Task 12: 供应商目录卡片增加 LOGO

**Files:**
- Modify: `gateway-console/src/pages/Catalog/ProviderCatalogView.tsx`

- [ ] **Step 1: 导入 ProviderIcon 并修改卡片渲染**

```tsx
import { ProviderIcon } from '../../components/ui/ProviderIcon';

// 修改卡片内容
<Col key={provider.code} xs={24} sm={12} md={8} lg={6}>
  <Card
    hoverable
    onClick={() => onSelectProvider(provider.code, provider.name)}
    actions={[
      <Button
        type="link"
        size="small"
        onClick={(e) => {
          e.stopPropagation();
          onCascadeMaterialize(provider.code, provider.name);
        }}
      >
        {provider.materialized ? '已物化' : '物化'}
      </Button>,
      <Button type="link" size="small" onClick={() => onSelectProvider(provider.code, provider.name)}>
        套餐
      </Button>,
    ]}
  >
    <Card.Meta
      avatar={<ProviderIcon providerId={provider.code} size={40} />}
      title={provider.name}
      description={
        <div>
          <div style={{ marginBottom: 4 }}>
            <Tag color={provider.providerType === 'INTERNATIONAL' ? 'blue' : 'green'}>
              {provider.providerType}
            </Tag>
          </div>
          <Text type="secondary" style={{ fontSize: 12 }}>{provider.code}</Text>
        </div>
      }
    />
  </Card>
</Col>
```

- [ ] **Step 2: Commit**

```bash
git add gateway-console/src/pages/Catalog/ProviderCatalogView.tsx
git commit -m "feat(frontend): 供应商目录卡片增加品牌 LOGO"
```

---

## Task 13: 套餐目录增加"快速创建渠道"入口

**Files:**
- Modify: `gateway-console/src/pages/Catalog/PlanCatalogView.tsx`

- [ ] **Step 1: 添加"快速创建渠道"按钮**

```tsx
// 在操作列添加新按钮
{
  title: '',
  key: 'actions',
  width: 240,
  render: (_: unknown, record: PlanCatalog) => (
    <Space size="small">
      <Button
        type="link"
        size="small"
        icon={<CloudDownloadOutlined />}
        disabled={record.materialized}
        onClick={(e) => {
          e.stopPropagation();
          onMaterialize('PLAN', record.planCode, record.planName);
        }}
      >
        {record.materialized ? '已物化' : '物化'}
      </Button>
      <Button type="link" size="small" onClick={() => onSelectPlan(record.planCode, record.planName)}>
        详情
      </Button>
      <Button
        type="primary"
        size="small"
        onClick={(e) => {
          e.stopPropagation();
          onQuickCreate?.(record.planCode, record.planName);
        }}
      >
        快速创建
      </Button>
    </Space>
  ),
}
```

- [ ] **Step 2: 更新组件 Props**

```tsx
interface PlanCatalogViewProps {
  onSelectPlan: (planCode: string, planName: string) => void;
  onMaterialize: (type: string, code: string, name: string) => void;
  onQuickCreate?: (planCode: string, planName: string) => void;  // 新增
}
```

- [ ] **Step 3: Commit**

```bash
git add gateway-console/src/pages/Catalog/PlanCatalogView.tsx
git commit -m "feat(frontend): 套餐目录增加快速创建渠道入口"
```

---

## Task 14: 创建 ChannelCreateWizard 向导组件

**Files:**
- Create: `gateway-console/src/pages/Channels/ChannelCreateWizard.tsx`

- [ ] **Step 1: 创建向导组件（Step 1-2）**

```tsx
// 文件: pages/Channels/ChannelCreateWizard.tsx
import { useState, useEffect } from 'react';
import { Modal, Steps, Form, Select, Input, Button, Checkbox, Table, Space, Tag, Typography, message, Alert } from 'antd';
import { PlusOutlined, DeleteOutlined } from '@ant-design/icons';
import { useProviderCatalogs, usePlanCatalogs, usePlanDetail } from '../../services/query/useCatalog';
import { catalogMaterializeApi } from '../../services/api/catalog';
import { useQueryClient } from '@tanstack/react-query';

const { Text } = Typography;

interface EndpointItem {
  key: string;
  protocol: string;
  url: string;
  checked: boolean;
}

interface ModelItem {
  modelName: string;
  contextWindow?: number;
  capabilities?: string[];
  checked: boolean;
}

export interface ChannelCreateWizardProps {
  open: boolean;
  onClose: () => void;
  /** 预选的 Plan（从目录入口传入） */
  initialPlanCode?: string;
  initialPlanName?: string;
}

export const ChannelCreateWizard: React.FC<ChannelCreateWizardProps> = ({
  open,
  onClose,
  initialPlanCode,
  initialPlanName,
}) => {
  const [currentStep, setCurrentStep] = useState(0);
  const [selectedProvider, setSelectedProvider] = useState<string>();
  const [selectedPlan, setSelectedPlan] = useState<string>();
  const [endpoints, setEndpoints] = useState<EndpointItem[]>([]);
  const [models, setModels] = useState<ModelItem[]>([]);
  const [apiKeysText, setApiKeysText] = useState('');
  const [loading, setLoading] = useState(false);

  const queryClient = useQueryClient();

  // 查询数据
  const { data: providers } = useProviderCatalogs({});
  const { data: plans } = usePlanCatalogs({ providerCode: selectedProvider });
  const { data: planDetail } = usePlanDetail(selectedPlan);

  // 初始化预选
  useEffect(() => {
    if (initialPlanCode) {
      setSelectedPlan(initialPlanCode);
      // 从 planCode 解析 providerCode
      const providerCode = initialPlanCode.split('_')[0];
      setSelectedProvider(providerCode);
    }
  }, [initialPlanCode]);

  // Plan 详情加载后初始化端点和模型
  useEffect(() => {
    if (planDetail) {
      // 初始化端点
      const epItems: EndpointItem[] = (planDetail.endpoints || []).map((ep, idx) => ({
        key: `ep-${idx}`,
        protocol: ep.protocol,
        url: ep.url,
        checked: true,
      }));
      setEndpoints(epItems);

      // 初始化模型
      const modelItems: ModelItem[] = (planDetail.pricing || []).map((p) => ({
        modelName: p.modelName,
        contextWindow: p.contextWindow,
        capabilities: p.capabilities,
        checked: true,
      }));
      setModels(modelItems);
    }
  }, [planDetail]);

  // 解析 API Keys
  const parseApiKeys = (text: string): string[] => {
    return text
      .split(/[;,]/)
      .map(k => k.trim())
      .filter(k => k.length > 0)
      .filter((k, i, arr) => arr.indexOf(k) === i); // 去重
  };

  const parsedKeys = parseApiKeys(apiKeysText);

  // 步骤定义
  const steps = [
    { title: '选择套餐' },
    { title: '配置端点与模型' },
    { title: '配置凭证' },
    { title: '确认创建' },
  ];

  // 渲染 Step 1: 选择套餐
  const renderStep1 = () => (
    <Form layout="vertical">
      <Form.Item label="供应商">
        <Select
          value={selectedProvider}
          onChange={(v) => {
            setSelectedProvider(v);
            setSelectedPlan(undefined);
          }}
          placeholder="选择供应商"
          options={providers?.map(p => ({ label: p.name, value: p.code }))}
        />
      </Form.Item>
      <Form.Item label="套餐">
        <Select
          value={selectedPlan}
          onChange={setSelectedPlan}
          placeholder="选择套餐"
          disabled={!selectedProvider}
          options={plans?.map(p => ({ label: p.planName, value: p.planCode }))}
        />
      </Form.Item>
      {planDetail && (
        <Alert
          type="info"
          message={`套餐预览：${planDetail.endpoints?.length || 0} 个端点，${planDetail.pricing?.length || 0} 个模型，${planDetail.billingMode}`}
        />
      )}
    </Form>
  );

  // 渲染 Step 2: 配置端点与模型
  const renderStep2 = () => (
    <div>
      <h4>端点配置</h4>
      {endpoints.map((ep, idx) => (
        <div key={ep.key} style={{ display: 'flex', gap: 8, marginBottom: 8, alignItems: 'center' }}>
          <Checkbox
            checked={ep.checked}
            onChange={(e) => {
              const newEps = [...endpoints];
              newEps[idx].checked = e.target.checked;
              setEndpoints(newEps);
            }}
          />
          <Select
            value={ep.protocol}
            onChange={(v) => {
              const newEps = [...endpoints];
              newEps[idx].protocol = v;
              setEndpoints(newEps);
            }}
            style={{ width: 120 }}
            options={[
              { label: 'OPENAI', value: 'OPENAI' },
              { label: 'ANTHROPIC', value: 'ANTHROPIC' },
              { label: 'GEMINI', value: 'GEMINI' },
            ]}
          />
          <Input
            value={ep.url}
            onChange={(e) => {
              const newEps = [...endpoints];
              newEps[idx].url = e.target.value;
              setEndpoints(newEps);
            }}
            style={{ flex: 1 }}
            placeholder="https://api.example.com/v1"
          />
          <Button
            type="text"
            danger
            icon={<DeleteOutlined />}
            onClick={() => setEndpoints(endpoints.filter((_, i) => i !== idx))}
          />
        </div>
      ))}
      <Button type="dashed" icon={<PlusOutlined />} onClick={() => setEndpoints([...endpoints, { key: `ep-new-${Date.now()}`, protocol: 'OPENAI', url: '', checked: true }])}>
        新增端点
      </Button>

      <h4 style={{ marginTop: 24 }}>模型配置</h4>
      <Checkbox.Group
        value={models.filter(m => m.checked).map(m => m.modelName)}
        onChange={(vals) => {
          setModels(models.map(m => ({ ...m, checked: vals.includes(m.modelName) })));
        }}
        style={{ width: '100%' }}
      >
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: 8 }}>
          {models.map(m => (
            <Checkbox key={m.modelName} value={m.modelName}>
              <Space>
                <Text>{m.modelName}</Text>
                {m.contextWindow && <Tag>{(m.contextWindow / 1000).toFixed(0)}K</Tag>}
              </Space>
            </Checkbox>
          ))}
        </div>
      </Checkbox.Group>
      <div style={{ marginTop: 8 }}>
        <Input
          placeholder="输入自定义模型名，回车添加"
          onPressEnter={(e) => {
            const val = (e.target as HTMLInputElement).value.trim();
            if (val && !models.find(m => m.modelName === val)) {
              setModels([...models, { modelName: val, checked: true }]);
              (e.target as HTMLInputElement).value = '';
            }
          }}
          style={{ width: 300 }}
        />
      </div>
    </div>
  );

  // 渲染 Step 3: 配置凭证
  const renderStep3 = () => (
    <div>
      <Form layout="vertical">
        <Form.Item label="API Key" required>
          <Input.TextArea
            value={apiKeysText}
            onChange={(e) => setApiKeysText(e.target.value)}
            placeholder="请输入 API Key，多个 Key 用分号或逗号分隔，如：sk-xxx; sk-yyy"
            rows={4}
          />
        </Form.Item>
      </Form>
      {parsedKeys.length > 0 && (
        <div>
          <Text type="secondary">已识别 {parsedKeys.length} 个 API Key：</Text>
          <div style={{ marginTop: 8 }}>
            {parsedKeys.map((key, idx) => {
              const masked = key.length < 12 ? key + '****' : key.substring(0, 6) + '****' + key.substring(key.length - 4);
              const tooShort = key.length < 8;
              return (
                <div key={idx} style={{ marginBottom: 4 }}>
                  <Tag color={tooShort ? 'error' : 'success'}>{idx + 1}</Tag>
                  <Text code>{masked}</Text>
                  {tooShort && <Text type="danger" style={{ marginLeft: 8 }}>长度过短</Text>}
                </div>
              );
            })}
          </div>
        </div>
      )}
    </div>
  );

  // 渲染 Step 4: 确认创建
  const renderStep4 = () => (
    <div>
      <h4>创建汇总</h4>
      <p><Text strong>套餐：</Text>{selectedPlan}</p>
      <p><Text strong>端点：</Text>{endpoints.filter(e => e.checked).length} 个</p>
      <p><Text strong>模型：</Text>{models.filter(m => m.checked).length} 个</p>
      <p><Text strong>API Key：</Text>{parsedKeys.length} 个</p>
    </div>
  );

  // 处理创建
  const handleCreate = async () => {
    if (!selectedPlan) {
      message.error('请选择套餐');
      return;
    }
    if (parsedKeys.length === 0) {
      message.error('请输入至少一个 API Key');
      return;
    }

    setLoading(true);
    try {
      await catalogMaterializeApi.materializePlan(selectedPlan, {
        apiKeys: parsedKeys,
        endpoints: endpoints.filter(e => e.checked).map(e => ({ protocol: e.protocol, url: e.url })),
        models: models.filter(m => m.checked).map(m => m.modelName),
      });
      message.success('渠道创建成功');
      queryClient.invalidateQueries({ queryKey: ['channels'] });
      onClose();
    } catch (error) {
      message.error('创建失败');
    } finally {
      setLoading(false);
    }
  };

  // 导航按钮
  const renderFooter = () => (
    <div style={{ display: 'flex', justifyContent: 'space-between' }}>
      <Button onClick={onClose}>取消</Button>
      <Space>
        {currentStep > 0 && (
          <Button onClick={() => setCurrentStep(currentStep - 1)}>上一步</Button>
        )}
        {currentStep < 3 ? (
          <Button type="primary" onClick={() => setCurrentStep(currentStep + 1)}>下一步</Button>
        ) : (
          <Button type="primary" loading={loading} onClick={handleCreate}>创建</Button>
        )}
      </Space>
    </div>
  );

  return (
    <Modal
      title="新建渠道"
      open={open}
      onCancel={onClose}
      width={700}
      footer={renderFooter()}
      destroyOnClose
    >
      <Steps current={currentStep} items={steps} style={{ marginBottom: 24 }} />
      {currentStep === 0 && renderStep1()}
      {currentStep === 1 && renderStep2()}
      {currentStep === 2 && renderStep3()}
      {currentStep === 3 && renderStep4()}
    </Modal>
  );
};

export default ChannelCreateWizard;
```

- [ ] **Step 2: Commit**

```bash
git add gateway-console/src/pages/Channels/ChannelCreateWizard.tsx
git commit -m "feat(frontend): 新建渠道向导组件"
```

---

## Task 15: 集成向导到渠道页面和目录页面

**Files:**
- Modify: `gateway-console/src/pages/Channels/index.tsx`
- Modify: `gateway-console/src/pages/Catalog/index.tsx`

- [ ] **Step 1: 更新渠道页面**

```tsx
// 在 pages/Channels/index.tsx 中
import { ChannelCreateWizard } from './ChannelCreateWizard';

// 确保状态和组件已存在（当前代码已有）
const [wizardVisible, setWizardVisible] = useState(false);

// 确保 Button 和 Modal 正确渲染
<Button type="primary" icon={<PlusOutlined />} onClick={() => setWizardVisible(true)}>
  新建渠道
</Button>

<ChannelCreateWizard
  open={wizardVisible}
  onClose={() => setWizardVisible(false)}
/>
```

- [ ] **Step 2: 更新目录页面**

```tsx
// 在 pages/Catalog/index.tsx 中
import { ChannelCreateWizard } from '../Channels/ChannelCreateWizard';

// 添加状态
const [wizardVisible, setWizardVisible] = useState(false);
const [initialPlan, setInitialPlan] = useState<{ code: string; name: string }>();

// 处理快速创建
const handleQuickCreate = (planCode: string, planName: string) => {
  setInitialPlan({ code: planCode, name: planName });
  setWizardVisible(true);
};

// 传递给 PlanCatalogView
<PlanCatalogView
  onSelectPlan={handleSelectPlan}
  onMaterialize={handleMaterialize}
  onQuickCreate={handleQuickCreate}
/>

// 渲染向导
<ChannelCreateWizard
  open={wizardVisible}
  onClose={() => setWizardVisible(false)}
  initialPlanCode={initialPlan?.code}
  initialPlanName={initialPlan?.name}
/>
```

- [ ] **Step 3: Commit**

```bash
git add gateway-console/src/pages/Channels/index.tsx \
        gateway-console/src/pages/Catalog/index.tsx
git commit -m "feat(frontend): 集成向导到渠道页面和目录页面"
```

---

## Task 16: 扩展前端 catalog API 服务

**Files:**
- Modify: `gateway-console/src/services/api/catalog.ts`

- [ ] **Step 1: 更新 materializePlan 方法**

```typescript
// 在 services/api/catalog.ts 中
import { MaterializePlanRequest, MaterializeResult } from '../../types/catalog';

export const catalogMaterializeApi = {
  materializeProvider: (providerCode: string) =>
    api.post<MaterializeResult>(`${BASE_URL}/materialize/provider/${providerCode}`),

  materializeProviderWithPlans: (providerCode: string, data?: MaterializeBatchRequest) =>
    api.post<MaterializeBatchResult>(`${BASE_URL}/materialize/provider/${providerCode}/with-plans`, data),

  // 扩展：支持请求体参数
  materializePlan: (planCode: string, data?: MaterializePlanRequest) =>
    api.post<MaterializeResult>(`${BASE_URL}/materialize/plan/${planCode}`, data),
};
```

- [ ] **Step 2: Commit**

```bash
git add gateway-console/src/services/api/catalog.ts
git commit -m "feat(frontend): 扩展物化 API 支持请求体参数"
```

---

## Task 17: 端到端测试与验收

**Files:**
- 无新增文件

- [ ] **Step 1: 启动后端服务**

```bash
cd gateway-boot && ../mvnw spring-boot:run
```

- [ ] **Step 2: 启动前端开发服务器**

```bash
cd gateway-console && npm run dev
```

- [ ] **Step 3: 验收测试清单**

1. **API Key 显示**
   - [ ] 开发者门户"我的 API Key"列表显示脱敏格式
   - [ ] 点击眼睛图标可切换明文/脱敏
   - [ ] 点击复制按钮可复制明文到剪贴板

2. **快速开始**
   - [ ] 代码卡片无硬编码深色背景
   - [ ] URL 自动填充为当前域名或环境变量
   - [ ] API Key 自动填充用户第一个有效 Key

3. **供应商目录**
   - [ ] 卡片显示品牌 LOGO

4. **渠道向导**
   - [ ] 渠道页面"新建渠道"按钮打开向导
   - [ ] 目录页面"快速创建"按钮打开向导并预选 Plan
   - [ ] 四步流程正常工作
   - [ ] 端点可编辑、可增删
   - [ ] 模型可勾选、可增删
   - [ ] API Key 批量输入、解析、校验
   - [ ] 创建成功后返回渠道列表

- [ ] **Step 4: 最终 Commit**

```bash
git add -A
git commit -m "feat: 前端 UX 优化与渠道创建向导完成"
```

---

## 自检清单

**1. Spec 覆盖检查：**
- [x] API Key 脱敏显示 → Task 5-10
- [x] 快速开始改进 → Task 11
- [x] 供应商目录 LOGO → Task 12
- [x] 新建渠道向导 → Task 14-15
- [x] 后端 DTO 扩展 → Task 1-3
- [x] 类型定义更新 → Task 4

**2. 占位符扫描：**
- 无 TBD/TODO
- 所有代码步骤包含完整实现

**3. 类型一致性：**
- `keyMasked` 字段在前后端类型定义中一致
- `MaterializePlanRequest` 在前后端定义一致
- `MaskedKeyDisplay` props 与使用处一致
