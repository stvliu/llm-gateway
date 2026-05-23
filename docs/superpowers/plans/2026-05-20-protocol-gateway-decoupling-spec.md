# 端点协议与供应商解耦重构 — 规格文档

## 问题

当前架构中"供应商"和"协议"职责严重耦合：

1. **ProviderType 枚举**同时承担品牌标识和协议暗示，但无法表达"同一供应商不同产品支持不同协议"
2. **Provider.baseUrl** 把端点信息放在供应商上，但端点应属于产品
3. **LLMAdapter** 按供应商分（OpenAI/Anthropic/DeepSeek），DeepSeek 几乎复用 OpenAI 全部代码
4. **Product.endpoints** 的 key 是自由文本 `Map<String, String>`，无类型约束
5. **EndpointProtocol 枚举**与 ProtocolGateway 实现类是 1:1 映射，枚举是冗余的

## 目标

**供应商和协议完全正交，端点只跟产品关联，协议类型由 ProtocolGateway 实现类自声明。**

新增协议 = 新增一个 ProtocolGateway 实现类，零改动其他代码。

## 最终职责划分

| 实体/接口 | 职责 |
|-----------|------|
| **Provider** | 品牌名称(name)、连接参数(timeout/retry/priority) |
| **ProviderGateway** | 纯数据访问 CRUD |
| **Product** | 端点(protocolName→URL)、模型列表、计费 |
| **ProtocolGateway** | 协议标识、认证、请求构建、响应解析、Key 格式验证 |
| **ProductApiKey** | 加密存储的 API Key |

## 关键设计决策

### D1: 协议类型不使用枚举，由 ProtocolGateway 自声明

ProtocolGateway 实现类声明 `getProtocolName()` 返回唯一字符串标识。
Product.endpoints 保持 `Map<String, String>`，key 是 ProtocolGateway.getProtocolName()。
前端从后端 API 动态获取可选协议列表，不硬编码。

理由：枚举和 ProtocolGateway 实现类是 1:1 映射，枚举是冗余的。
新增 Gemini 协议只需加一个 GeminiProtocolGateway 实现类，零改动其他代码。

### D2: Provider.baseUrl 移除，端点信息全部在 Product 上

端点属于产品，不属于供应商。同一供应商的不同产品用不同的端点。
Provider 只保留品牌信息和连接参数。

### D3: ProviderType 枚举移除，品牌标识用 Provider.name

ProviderType 不再承担协议暗示职责，品牌标识用 Provider.name（自由文本）。
Provider.type 字段从 ProviderType 枚举改为 String。

### D4: 认证是协议的职责，不是供应商的职责

OpenAI 协议用 `Authorization: Bearer`，Anthropic 协议用 `x-api-key`。
认证方式跟着协议走，不跟着供应商走。
同一供应商如果同时提供两种协议端点，认证方式就不同。

### D5: 所有 API Key 都加密存储，无需按供应商区分

## 变更范围

### 后端 Java

**新建：**
- `domain/proxy/gateway/ProtocolGateway.java` — 协议网关接口
- `domain/proxy/gateway/ProtocolGatewayRegistry.java` — 协议网关注册表接口
- `infrastructure/proxy/gateway/protocol/OpenAIProtocolGateway.java` — OpenAI 协议实现
- `infrastructure/proxy/gateway/protocol/AnthropicProtocolGateway.java` — Anthropic 协议实现
- `infrastructure/proxy/gateway/protocol/ProtocolGatewayRegistryImpl.java` — 注册表实现
- `adapter/api/ProtocolController.java` — 协议列表 API
- `db/migration/V17__protocol_gateway_and_provider_simplify.sql` — 数据库迁移

**修改：**
- `domain/product/entity/Product.java` — getDefaultEndpoint 逻辑优化
- `domain/model/entity/Provider.java` — 移除 baseUrl，type 改为 String
- `domain/proxy/entity/RoutingContext.java` — providerType 改为 String
- `domain/proxy/gateway/LLMGateway.java` — getProviderType 返回 String
- `domain/proxy/gateway/LLMGatewayRegistry.java` — getGateway 参数改为 String
- `domain/model/entity/ProviderCapabilities.java` — providerType 改为 String
- `application/product/dto/ProductRequest.java` — endpoints key 语义约束
- `application/product/dto/ProductResponse.java` — 同上
- `application/proxy/ProductRoutingService.java` — 使用 ProtocolGateway 路由
- `application/proxy/ChannelRoutingService.java` — 移除 provider.getBaseUrl()
- `application/proxy/ProxyServiceImpl.java` — 使用 ProtocolGateway
- `application/provider/ProviderServiceImpl.java` — providerType 改为 String，移除 baseUrl
- `application/provider/dto/*` — providerType 改为 String，移除 baseUrl
- `application/experience/ModelExperienceService.java` — providerType 改为 String
- `application/experience/dto/ExperienceChatRequest.java` — providerType 改为 String
- `application/metadata/ProviderMetadataService.java` — providerType 改为 String
- `adapter/api/ProviderController.java` — getProviderTypes 改为返回供应商名称列表
- `infrastructure/proxy/gateway/rpc/LLMAdapter.java` — getProviderType 返回 String
- `infrastructure/proxy/gateway/rpc/OpenAIAdapter.java` — getProviderType 返回 "openai"
- `infrastructure/proxy/gateway/rpc/AnthropicAdapter.java` — getProviderType 返回 "anthropic"
- `infrastructure/proxy/gateway/rpc/AdapterBuilderFactory.java` — 基于 ProtocolGateway 查找
- `infrastructure/product/gateway/ProductGatewayImpl.java` — 适配
- `infrastructure/model/gateway/ProviderGatewayImpl.java` — 移除 baseUrl，type 改为 String
- `infrastructure/model/gateway/database/dataobject/ProviderDo.java` — 移除 baseUrl，type 改为 String
- `infrastructure/init/DataInitializer.java` — 移除 setBaseUrl，端点写入产品

**删除：**
- `domain/model/enums/ProviderType.java`

### 前端

- `gateway-console/src/types/product.ts` — 端点 key 从后端动态获取
- `gateway-console/src/pages/Providers/ProductFormModal.tsx` — 端点 key 改为动态 Select
- Provider 表单移除 baseUrl 字段
- ProviderType 下拉改为自由文本或后端建议列表

## 验收标准

1. 编译通过: `./mvnw clean compile -pl gateway-boot`
2. 测试通过: `./mvnw test -pl gateway-boot`
3. 前端编译: `cd gateway-console && pnpm build`
4. 创建供应商时无 baseUrl 字段
5. 创建产品时端点 key 为动态下拉（从后端获取协议列表）
6. 旧架构路由降级兼容
7. 新架构路由按协议名称精确匹配端点
8. 新增协议只需加一个 ProtocolGateway 实现类
