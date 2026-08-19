# 跨层违规点清单（模块化重构待治理项）

> 来源：ArchUnit `LayerDependencyTest` freeze 基线（Phase 0.1 建立）
> 作用：作为后续各阶段（Phase 1-4）验收参照——违规减少即基线更新，新增违规则测试失败。
> 冻结基线存储：`gateway-boot/target/archunit`

## 一、domain 层依赖 infrastructure（违反依赖倒置）

ArchUnit 规则 `DOMAIN_NOT_DEPEND_ON_INFRASTRUCTURE` 检测到 **34 处**违规，主要聚集于：

| 领域类 | 违规依赖 | 说明 |
|--------|---------|------|
| `domain.iam.service.ApiKeyEncryptionDomainService` | `infrastructure.iam.gateway.encryption.EncryptionService` | 构造函数参数 + 字段直接依赖基础设施实现 |
| `domain.threat.service.RateLimitDomainService` | `infrastructure.config.GatewayProperties` | 直接读取基础设施配置类（限流参数） |

**治理方向**：domain 只应依赖 `domain/xxx/gateway/` 接口；`EncryptionService`、`GatewayProperties` 等基础设施实现/配置应通过 gateway 接口或 application 层注入，移出 domain 直接依赖。

## 二、application 层依赖 infrastructure 具体类（Facade 跨层）

| 应用类 | 违规依赖 | 说明 |
|--------|---------|------|
| `application.protocol.conversion.ProtocolConversionFacade` | `infrastructure.protocol.OpenAIProtocolAdapter` / `AnthropicProtocolAdapter`（构造注入） | 已核实：application 直接依赖 infrastructure 具体 Adapter 类，绕过 domain gateway 接口 |

**治理方向**：对应 Task 1.10——Facade 改为按 SPI 装配（注入 `List<ProtocolAdapter<?>>` 收集 Bean），删除对具体 Adapter 类的 import。

## 三、其他潜在违规（需在迁移时核查）

- `domain.supply` 相关类与 `application.supply.dto` 的交叉引用（如 `ChannelKeyProbe` 返回 `application.supply.dto.KeyTestResult`）。
- domain 各领域枚举/实体与 infrastructure 配置类的偶发直接引用。

> 注：以上为 Phase 0.3 初始清单。完整违规明细以 ArchUnit store 基线为准，后续每阶段解冻修复后更新本清单。
