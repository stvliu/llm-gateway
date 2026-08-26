# 遗留模块迁移计划

**日期:** 2026-04-27
**目标:** 将 gateway-common, gateway-core, gateway-infrastructure, gateway-router, gateway-adapter, gateway-api, gateway-application 中的遗留文件迁移到 gateway-boot 单模块

---

## 一、现状分析

### 1.1 各模块文件统计

| 模块 | 主文件数 | 测试文件数 | 说明 |
|------|----------|------------|------|
| gateway-common | 3 | 3 | DTO、异常基类 |
| gateway-core | ~50 | 6 | 实体、Gateway接口、服务、仓储、基础设施 |
| gateway-infrastructure | 9 | 0 | JpaGateway 实现 |
| gateway-router | 6 | 2 | 路由调度、协议转换 |
| gateway-adapter | ~16 | 6 | LLM 适配器 (OpenAI/Anthropic) |
| gateway-analytics | 0 | 0 | 空模块 |
| gateway-api | ~30 | 6 | Controller、DTO、Service、Advice、Config |
| gateway-application | ~15 | 3 | 事件监听器、SaToken 配置 |

### 1.2 已迁移到 gateway-boot 的文件

根据 CLAUDE.md 中的架构，已经存在于 gateway-boot 的文件不应重复迁移。

---

## 二、迁移原则

1. **按领域内聚迁移**: Entity + Domain Service + Gateway 接口 → `domain/xxx/`
2. **Gateway 实现迁移到**: `infrastructure/gateway/xxx/`
3. **Adapter 迁移到**: `infrastructure/adapter/`
4. **Controller 迁移到**: `adapter/xxx/controller/`
5. **DTO 迁移到**: `adapter/xxx/dto/`
6. **Application Service 迁移到**: `application/xxx/`
7. **配置类迁移到**: `infrastructure/config/`

---

## 三、迁移清单

### 3.1 gateway-common → gateway-boot/common/

| 源文件 | 目标路径 | 优先级 | 状态 |
|--------|----------|--------|------|
| common/dto/LLMRequest.java | common/dto/LLMRequest.java | P1 | 待迁移 |
| common/dto/LLMResponse.java | common/dto/LLMResponse.java | P1 | 待迁移 |
| common/exception/GatewayException.java | common/exception/GatewayException.java | P1 | 待迁移 |

**注意:** gateway-boot 中已有 `ApiResponse`, `GatewayRequestException`, `ProviderException`, `SecurityException`，需检查是否重复。

### 3.2 gateway-core → gateway-boot/domain/

| 源文件 | 目标路径 | 优先级 | 状态 |
|--------|----------|--------|------|
| core/domain/security/entity/* | domain/security/entity/ | P1 | 待迁移 |
| core/domain/router/entity/* | domain/router/entity/ | P1 | 待迁移 |
| core/domain/analytics/entity/TokenLimit.java | domain/security/entity/TokenLimit.java | P1 | 待迁移 |
| core/domain/enums/* | common/enums/ | P1 | 待迁移 |
| core/domain/event/* | common/event/ | P2 | 待迁移 |
| core/domain/gateway/* | domain/xxx/gateway/ | P1 | 待迁移 |
| core/infrastructure/encryption/* | infrastructure/security/encryption/ | P2 | 待迁移 |
| core/service/* | domain/xxx/service/ | P2 | 待迁移 |
| core/exception/* | common/exception/ | P2 | 待迁移 |

### 3.3 gateway-infrastructure → gateway-boot/infrastructure/gateway/

| 源文件 | 目标路径 | 优先级 | 状态 |
|--------|----------|--------|------|
| JpaApiKeyGateway | infrastructure/gateway/security/JpaApiKeyGateway | P1 | 已存在 |
| JpaModelGateway | infrastructure/gateway/router/JpaModelGateway | P1 | 已存在 |
| JpaProviderGateway | infrastructure/gateway/router/JpaProviderGateway | P1 | 已存在 |
| JpaRouteGroupGateway | infrastructure/gateway/router/JpaRouteGroupGateway | P1 | 已存在 |
| JpaAuditGateway | infrastructure/gateway/security/JpaAuditGateway | P1 | 已存在 |
| JpaIpBlockGateway | infrastructure/gateway/security/JpaIpBlockGateway | P1 | 已存在 |
| JpaTokenLimitGateway | infrastructure/gateway/security/JpaTokenLimitGateway | P1 | 已存在 |
| JpaProviderApiKeyGateway | infrastructure/gateway/security/JpaProviderApiKeyGateway | P1 | 待迁移 |
| PageResult | infrastructure/util/PageResult | P1 | 待迁移 |

### 3.4 gateway-router → gateway-boot/infrastructure/

| 源文件 | 目标路径 | 优先级 | 状态 |
|--------|----------|--------|------|
| LLMDispatcher | domain/router/service/LLMDispatcher | P1 | 已存在 |
| ModelRouter | domain/router/gateway/ModelRouter | P1 | 已存在 |
| ProtocolTranslator | infrastructure/util/ProtocolTranslator | P2 | 待迁移 |
| ErrorResponseAdapter | infrastructure/adapter/ErrorResponseAdapter | P2 | 待迁移 |
| DefaultModelRouter | domain/router/service/ | P2 | 待迁移 |

### 3.5 gateway-adapter → gateway-boot/infrastructure/adapter/

| 源文件 | 目标路径 | 优先级 | 状态 |
|--------|----------|--------|------|
| LLMProviderAdapter | infrastructure/adapter/LLMProviderAdapter | P1 | 已存在 |
| StreamCallback | infrastructure/adapter/StreamCallback | P1 | 已存在 |
| StreamCallbackImpl | infrastructure/adapter/StreamCallbackImpl | P1 | 已存在 |
| openai/OpenAIAdapter | infrastructure/adapter/openai/OpenAIAdapter | P1 | 待迁移 |
| anthropic/AnthropicAdapter | infrastructure/adapter/anthropic/AnthropicAdapter | P1 | 待迁移 |
| common/CredentialsLoader | infrastructure/util/CredentialsLoader | P2 | 待迁移 |
| common/ProviderCapabilities | common/ProviderCapabilities | P2 | 待迁移 |
| spi/AdapterLoader | infrastructure/spi/AdapterLoader | P2 | 待迁移 |
| spi/AdapterRegistry | infrastructure/spi/AdapterRegistry | P2 | 待迁移 |

### 3.6 gateway-api → gateway-boot/adapter/

| 源文件 | 目标路径 | 优先级 | 状态 |
|--------|----------|--------|------|
| controller/* | adapter/xxx/controller/ | P1 | 待迁移 |
| dto/* | adapter/xxx/dto/ | P1 | 待迁移 |
| advice/* | infrastructure/advice/ | P2 | 待迁移 |
| config/* | infrastructure/config/ | P2 | 待迁移 |
| security/SecurityExceptionHandler | infrastructure/security/SecurityExceptionHandler | P2 | 待迁移 |
| service/* | application/xxx/ | P1 | 待迁移 |

### 3.7 gateway-application → gateway-boot/infrastructure/

| 源文件 | 目标路径 | 优先级 | 状态 |
|--------|----------|--------|------|
| listener/* | application/listener/ | P1 | 已存在 |
| config/satoken/* | infrastructure/security/ | P2 | 待迁移 |

---

## 四、迁移优先级

### P0 (必须迁移 - 阻塞构建)
- gateway-infrastructure 中未在 gateway-boot 存在的 Jpa*Gateway 实现
- gateway-adapter 中的 OpenAIAdapter, AnthropicAdapter

### P1 (高优先级 - 功能完整)
- gateway-core 中的 Domain Entity 和 Gateway 接口
- gateway-api 中的 Controller 和 DTO
- gateway-adapter 中的核心适配器组件

### P2 (中优先级 - 完善功能)
- gateway-common 的 DTO/异常
- gateway-router 的辅助类
- gateway-application 的 SaToken 配置

### P3 (低优先级 - 清理)
- 重复文件的清理
- 未使用的文件删除

---

## 五、冲突检测

### 5.1 已存在于 gateway-boot 的文件

```
gateway-boot 中已存在:
- common/dto/ApiResponse.java
- common/exception/GatewayRequestException.java
- common/exception/ProviderException.java
- common/exception/SecurityException.java
- domain/router/entity/Model.java
- domain/router/entity/Provider.java
- domain/router/gateway/ModelGateway.java
- domain/router/gateway/ProviderGateway.java
- domain/router/service/ModelService.java
- domain/router/service/ProviderService.java
- domain/security/entity/AuditLog.java
- domain/security/entity/GatewayApiKey.java
- domain/security/entity/IpBlocklist.java
- domain/security/entity/RateLimitConfig.java
- domain/security/entity/SensitiveDataRule.java
- domain/security/entity/User.java
- domain/security/gateway/ApiKeyGateway.java
- domain/security/gateway/AuditGateway.java
- domain/security/gateway/IpBlockGateway.java
- domain/security/gateway/TokenLimitGateway.java
- infrastructure/gateway/router/JpaModelGateway.java
- infrastructure/gateway/router/JpaProviderGateway.java
- infrastructure/gateway/security/JpaApiKeyGateway.java
- infrastructure/gateway/security/JpaAuditGateway.java
- infrastructure/gateway/security/JpaIpBlockGateway.java
```

### 5.2 需要决策的文件

| 源文件 | 冲突情况 | 建议 |
|--------|----------|------|
| gateway-common/exception/GatewayException.java | gateway-boot 已有 GatewayException | 保留 gateway-boot 版本 |
| gateway-common/dto/LLMRequest.java | gateway-boot 无此文件 | 迁移 |
| gateway-common/dto/LLMResponse.java | gateway-boot 无此文件 | 迁移 |
| gateway-core/domain/enums/* | gateway-boot 已有 ProviderErrorType, ProviderType | 合并 |
| gateway-core/domain/event/* | gateway-boot 已有 AuditEvent, DomainEvent, TokenUsedEvent | 合并 |
| gateway-core/infrastructure/encryption/* | gateway-boot 已有加密服务 | 迁移 gateway-core 版本 |

---

## 六、实施步骤

### 阶段 1: 核心模型迁移
1. 迁移 gateway-core/domain/entity 到 gateway-boot/domain/
2. 迁移 gateway-core/domain/gateway 到 gateway-boot/domain/
3. 迁移 gateway-core/domain/enums 到 gateway-boot/common/enums/
4. 迁移 gateway-core/domain/event 到 gateway-boot/common/event/

### 阶段 2: 基础设施层迁移
5. 迁移 gateway-infrastructure 的 JpaGateway 实现
6. 迁移 gateway-core/infrastructure/encryption 到 gateway-boot/infrastructure/
7. 迁移 gateway-adapter 的适配器到 gateway-boot/infrastructure/adapter/

### 阶段 3: 适配层迁移
8. 迁移 gateway-api/controller 到 gateway-boot/adapter/
9. 迁移 gateway-api/dto 到 gateway-boot/adapter/xxx/dto/
10. 迁移 gateway-api/service 到 gateway-boot/application/

### 阶段 4: 配置和安全迁移
11. 迁移 gateway-application/config/satoken 到 gateway-boot/infrastructure/
12. 迁移 gateway-api/config 到 gateway-boot/infrastructure/config/

### 阶段 5: 清理
13. 删除重复文件
14. 更新各模块 pom.xml 移除已迁移的依赖
15. 验证构建

---

## 七、后续行动

- [ ] 确认迁移清单和优先级
- [ ] 开始阶段 1 迁移
- [ ] 逐步验证构建
- [ ] 清理旧模块

---

## 八、风险评估

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| 文件冲突 | 高 | 逐一检查，手动合并 |
| 构建失败 | 中 | 分阶段迁移，每阶段验证 |
| 功能 regression | 中 | 保留备份，运行测试 |
| 循环依赖 | 低 | 按依赖顺序迁移 |
