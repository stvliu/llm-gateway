# 统一分层连通性测试策略实现方案

## Context

当前 Provider 连通性测试存在两个问题：
1. **OpenAI 兼容供应商**只调用 `GET /v1/models`，仅验证认证，不验证模型可用性；而 **Anthropic** 发送最小 chat 请求同时验证了认证和模型可用性——两套标准不一致
2. **火山引擎等不支持 /v1/models 的供应商**，`checkConnection()` 直接返回 `isAvailable()`（只检查 apiKey 非空），完全没有网络验证

需要统一为分层测试策略：Level 1 认证 + Level 2 模型可用性，让所有供应商都有一致的验证深度。同时丰富测试结果（延迟、错误分类），为后续定时巡检打基础。

---

## 分层测试策略设计

### 执行策略

| 供应商类型 | Level 1（认证） | Level 2（模型可用性） |
|-----------|----------------|---------------------|
| **OpenAI 兼容** | `GET /v1/models` → 认证 + 模型列表 | `POST /v1/chat/completions` (max_tokens=1) → 模型可用 |
| **Anthropic** | `POST /v1/messages` (max_tokens=1) → 认证 + 模型可用（二合一） | 跳过（Level 1 已覆盖） |
| **火山引擎** | `POST /v1/chat/completions` (max_tokens=1) → 认证 + 模型可用（二合一） | 跳过（Level 1 已覆盖） |

- Level 1 失败则不执行 Level 2
- Level 2 的测试模型优先取 Level 1 返回的第一个模型，否则用默认模型

### 默认测试模型映射

```java
OPENAI    → gpt-4o-mini
DEEPSEEK  → deepseek-chat
MOONSHOT  → moonshot-v1-8k
ZHIPU     → glm-4-flash
BAICHUAN  → Baichuan4
MINIMAX   → MiniMax-Text-01
VOLCENGINE → doubao-1-5-pro-32k
ANTHROPIC → claude-haiku-3-5-20250514
QWEN      → qwen-turbo
GEMINI    → gemini-2.0-flash
// 其他     → 由 Level 1 返回的第一个模型
```

---

## 后端变更

### 1. 新增 DTO

**文件**: `gateway-boot/src/main/java/com/codingas/gateway/application/provider/dto/`

#### ConnectivityTestResult.java（替换 TestApiKeyResultDTO）

```java
public record ConnectivityTestResult(
    boolean success,                    // 整体是否成功
    String message,                     // 摘要消息
    List<String> models,               // 发现的模型列表
    LevelResult level1,                // Level 1 测试结果
    LevelResult level2,                // Level 2 测试结果（可为 null）
    long totalLatencyMs                // 总耗时（毫秒）
) {
    public record LevelResult(
        boolean success,               // 是否成功
        String message,                // 结果消息
        Long latencyMs,               // 响应延迟（毫秒）
        String errorType,             // 错误分类（ProviderErrorType 名称），成功时为 null
        List<String> models           // Level 1 特有：发现的模型列表
    ) {}
}
```

#### ConnectivityTestRequest.java（替换 TestApiKeyRequestDTO）

```java
public record ConnectivityTestRequest(
    @NotNull ProviderType providerType,
    String baseUrl,
    @NotBlank String apiKey,
    String model                       // 可选：指定 Level 2 测试模型
) {}
```

### 2. 新增 Gateway 接口 + 实现（方案 C）

**接口**: `gateway-boot/src/main/java/com/codingas/gateway/domain/model/gateway/ConnectivityTester.java`

```java
public interface ConnectivityTester {
    /** 执行分层连通性测试 */
    ConnectivityTestResult test(ConnectivityTestRequest request);
}
```

**实现**: `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/model/gateway/ConnectivityTesterImpl.java`

核心逻辑：
1. 根据 ProviderType 选择 Level 1 测试策略
2. 执行 Level 1，记录延迟和错误分类
3. 如果 Level 1 成功且需要 Level 2，执行 Level 2
4. 组装 ConnectivityTestResult 返回

内部方法：
- `testLevel1OpenAICompatible()` — GET /v1/models
- `testLevel1Anthropic()` — POST /v1/messages 最小请求
- `testLevel1ChatCompletion()` — POST /v1/chat/completions 最小请求（火山引擎等）
- `testLevel2ChatCompletion()` — POST /v1/chat/completions 验证模型可用
- `getDefaultTestModel()` — 各 ProviderType 的默认测试模型映射
- `resolveBaseUrl()` — 从 ProviderType 解析默认 baseUrl
- `classifyError()` — HTTP 状态码/异常 → ProviderErrorType 映射

### 3. 重构 ProviderServiceImpl

- 删除 `testApiKey`、`testOpenAICompatibleKey`、`testAnthropicKey`、`getDefaultBaseUrl` 方法
- 注入 `ConnectivityTester`（Domain Gateway 接口），`testConnectivity` 委托调用
- `ProviderService` 接口方法名改为 `testConnectivity`

### 4. 新增 Controller 端点

- 新增 `POST /api/v1/providers/connectivity-test`
- 删除旧端点 `POST /api/v1/providers/test-api-key`
- 参数 `ConnectivityTestRequest`，返回 `ConnectivityTestResult`

### 5. 重构 LLMAdapter.checkConnection()

- `OpenAIAdapter.checkConnection()` 改为 `POST /v1/chat/completions` 最小请求
- `VolcengineAdapter.checkConnection()` 不再返回 `isAvailable()`，改为发最小 chat 请求
- `AnthropicAdapter.checkConnection()` 保持不变
- LLMAdapter 可注入 `ConnectivityTesterImpl` 复用测试逻辑

### 6. 删除旧 DTO

- 删除 `TestApiKeyRequestDTO.java`
- 删除 `TestApiKeyResultDTO.java`

---

## 前端变更

### 1. 更新 API 类型

**文件**: `gateway-console/src/services/api/provider.ts`

- 删除旧 `TestApiKeyRequest` / `TestApiKeyResult` 类型
- 新增连通性测试类型
- API 路径改为 `/providers/connectivity-test`

```typescript
/** 连通性测试层级结果 */
interface LevelResult {
  success: boolean;
  message?: string;
  latencyMs?: number;
  errorType?: string;
  models?: string[];        // Level 1 特有
}

/** 连通性测试结果 */
interface ConnectivityTestResult {
  success: boolean;
  message?: string;
  models?: string[];        // 所有发现的模型
  level1?: LevelResult;
  level2?: LevelResult;
  totalLatencyMs?: number;
}

/** 连通性测试请求 */
interface ConnectivityTestRequest {
  providerType: string;
  baseUrl?: string;
  apiKey: string;
  model?: string;           // 可选：指定测试模型
}
```

### 2. 更新 ApiKeySetupStep.tsx

**文件**: `gateway-console/src/pages/Providers/ApiKeySetupStep.tsx`

当前展示：仅图标 + "有效/无效" 文字

增强展示：

```
┌─────────────────────────────────────────────────────┐
│ ★ 生产环境 Key  sk-abc1...  ✅ 有效                 │
│                              ├─ 认证: ✅ 230ms      │
│                              ├─ 模型可用: ✅ 450ms   │
│                              └─ 发现 56 个模型       │
└─────────────────────────────────────────────────────┘
```

变更点：
- `testMessages` 状态从 `Record<number, string>` 改为 `Record<number, ConnectivityTestResult>`
- 渲染分层结果：展示 Level 1/Level 2 各自状态、延迟、模型数量
- 测试失败时展示错误分类（如"认证失败"、"速率限制"）

### 3. 更新 ProviderApiKeysTab.tsx（编辑详情页）

**文件**: `gateway-console/src/pages/Providers/ProviderApiKeysTab.tsx`

当前：无连通性测试功能

新增：
- 每个 Key 卡片右侧增加"测试连通性"按钮
- 复用 ApiKeySetupStep 的测试逻辑和展示组件
- 提取公共组件 `ConnectivityTestButton` + `ConnectivityTestResult`

### 4. 提取公共组件

**新文件**: `gateway-console/src/pages/Providers/ConnectivityTestResult.tsx`

```
Props:
  - result: ConnectivityTestResult
  - compact?: boolean     // 紧凑模式（行内展示）vs 详细模式

渲染内容：
  - 整体状态图标 + 文字
  - Level 1: 认证 ✅/❌ + 延迟
  - Level 2: 模型可用 ✅/❌/跳过 + 延迟
  - 模型数量（有 Level 1 时）
  - 错误分类（失败时）
```

---

## 实现步骤

### Step 1: 后端 DTO 层
- 新增 `ConnectivityTestRequest.java`
- 新增 `ConnectivityTestResult.java`（含 `LevelResult` 内部 record）

### Step 2: 后端 Gateway 接口 + 实现
- 新增 `domain/model/gateway/ConnectivityTester.java` 接口
- 新增 `infrastructure/model/gateway/ConnectivityTesterImpl.java` 实现
- 实现 Level 1 / Level 2 分层测试逻辑
- 实现默认测试模型映射和 baseUrl 映射

### Step 3: 后端重构 ProviderServiceImpl + Controller
- 删除旧的 `testApiKey`、`testOpenAICompatibleKey`、`testAnthropicKey`、`getDefaultBaseUrl` 方法
- 注入 `ConnectivityTester`，委托调用
- 新增 `POST /api/v1/providers/connectivity-test` 端点
- 删除旧 `POST /api/v1/providers/test-api-key` 端点
- 删除 `TestApiKeyRequestDTO.java`、`TestApiKeyResultDTO.java`

### Step 4: 后端重构 LLMAdapter
- `OpenAIAdapter.checkConnection()` 改为最小 chat 请求
- `VolcengineAdapter.checkConnection()` 同上

### Step 5: 前端 API 层
- 更新 `provider.ts` 中的类型定义和 API 路径

### Step 6: 前端公共组件
- 新增 `ConnectivityTestResult.tsx` 展示组件
- 新增 `useConnectivityTest` 自定义 hook

### Step 7: 前端页面更新
- 更新 `ApiKeySetupStep.tsx`
- 更新 `ProviderApiKeysTab.tsx`（新增测试按钮）

---

## 关键文件清单

### 新增文件
- `domain/model/gateway/ConnectivityTester.java` — Gateway 接口
- `infrastructure/model/gateway/ConnectivityTesterImpl.java` — Gateway 实现（OkHttp）
- `application/provider/dto/ConnectivityTestRequest.java`
- `application/provider/dto/ConnectivityTestResult.java`
- `gateway-console/src/pages/Providers/ConnectivityTestResult.tsx`
- `gateway-console/src/hooks/useConnectivityTest.ts`

### 修改文件
- `application/provider/ProviderService.java` — 方法名 testApiKey → testConnectivity
- `application/provider/ProviderServiceImpl.java` — 删除旧测试方法，委托 ConnectivityTester
- `adapter/api/ProviderController.java` — 新端点 connectivity-test，删除旧端点
- `infrastructure/proxy/gateway/rpc/OpenAIAdapter.java` — checkConnection 改为 chat 请求
- `infrastructure/proxy/gateway/rpc/VolcengineAdapter.java` — checkConnection 改为 chat 请求
- `gateway-console/src/services/api/provider.ts` — 更新类型和 API 路径
- `gateway-console/src/pages/Providers/ApiKeySetupStep.tsx` — 使用新结果类型
- `gateway-console/src/pages/Providers/ProviderApiKeysTab.tsx` — 新增测试功能

### 删除文件
- `application/provider/dto/TestApiKeyRequestDTO.java`
- `application/provider/dto/TestApiKeyResultDTO.java`

---

## 验证方式

1. **单元测试**：ConnectivityTesterImpl 各 ProviderType 的分层测试逻辑
2. **手动测试**：通过前端创建向导测试 API Key 连通性，验证分层结果展示
3. **手动测试**：在 Provider 详情页 API Keys 标签页测试连通性
4. **API 测试**：`curl -X POST /api/v1/providers/connectivity-test` 验证返回格式
5. **编译验证**：`./mvnw clean compile -pl gateway-boot` 确保无编译错误
