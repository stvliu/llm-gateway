# ADR-0001: 模块化后分层架构与命名规范

- 状态：已接受（2026-08-25）
- 决策者：架构组 / Liu Ye
- 关联：CLAUDE.md「包名：域模块化」、P1-P4.5 模块化重构、`docs/constitution.md`

## 背景

项目已完成 17 模块化（域模块 + `-data` 绑定模块 + `-starter` 三明治结构）。历史演进：早期为单模块 + 分层包（`adapter/application/domain/infrastructure` 四层），P1-P4.5 分批拆分为多模块并做模块化命名对齐，但部分包名层名残留（如 gateway-boot 的 `infrastructure.*`、`application.*`）。

需要明确：**模块化后，单模块分层架构的哪些部分保留、哪些废弃**，以及命名规范的最终形态。

## 决策

### 1. 分层依赖规则——保留，载体迁移到模块边界 + ArchUnit

依赖方向控制（上层依赖下层接口、领域不依赖基础设施、禁止反向依赖）是软件工程铁律，必须保留。模块化后其承担者已从"package 分层命名约定"（软约束）迁移为：

- **Maven 模块依赖声明**（编译期硬约束）
- **ArchUnit 7 条铁律**（`LayerDependencyTest`，随单测强制执行：COMMON_NOT_DEPEND_ON_BUSINESS、NO_DEPENDS_ON_BOOT_OR_WEB、PROTOCOL_PLUGIN_ONLY_SPI 等）

### 2. 分层包名（adapter/application/domain/infrastructure 前缀）——废弃

模块化后包名层名成为冗余表达，且破坏模块内聚。统一为 **域模块化：模块 = 根包**：

| 模块 | 根包 | 子包示例 |
|------|------|---------|
| gateway-provider | `com.codingas.gateway.provider` | channel / service / catalog / model / vendor / health |
| gateway-iam | `com.codingas.gateway.iam` | dto / service / auth / apikey / encryption |
| gateway-protocol | `com.codingas.gateway.protocol` | contract / transport / tuning / validation |
| gateway-web | `com.codingas.gateway.web.*` | api / interceptor / advice（Controller/Interceptor/Advice 承载层） |
| gateway-boot | `com.codingas.gateway.boot.*` | config / init / event（收拢后） |
| 绑定模块 | `com.codingas.gateway.<域>data` | dataobject / gateway / repository |
| starter | `com.codingas.gateway.autoconfigure.<域>` | - |

### 3. 类名后缀——保留

`XxxService/XxxServiceImpl/XxxController/XxxGatewayImpl/XxxConfig/XxxDo` 等后缀是通用工程惯例，与分层无关，全部保留。Gateway 模式实现类（`XxxGatewayImpl`）放绑定模块 `-data` 的 `gateway` 包。

### 4. 健康监控类归属原则（本次实践确立）

**健康监控整体归域**（含 Actuator 端点适配）：`ProviderHealthProbe/Tracker/State/Properties/RegistryHealthIndicator` 全部归属 provider `health` 子包。

- 依据：① 功能内聚（同一能力不跨模块拆分）；② `HealthIndicator` 的聚合规则（任一 DOWN → 整体 DOWN）是域业务规则，Indicator 只是暴露薄壳；③ 可观测性内建原则（域对自己的健康负责）；④ 域模块已依赖 actuator（State/Tracker 使用 `Status` 类型），无额外技术成本；⑤ `ProviderConfiguration` 的 `@ComponentScan` 覆盖域根包，自动装配
- 例外：**集成测试依赖启动类的留 boot**（如 `ProviderHealthTrackerIntegrationTest` 为 `@SpringBootTest(GatewayApplication.class)`，归 boot `integration` 测试包），避免域模块反向依赖 boot
- boot 模块不再承担 ProviderHealth 任何组件（`boot.actuator` 包已删除）

## 后果

**正面**：包名与模块一一对应，消除层名歧义；依赖方向由模块边界硬约束，比包名约定更可靠；领域内聚性提升。

**负面**：boot/web 收拢改动影响面（约 40+ 文件）；`application`/`infrastructure` 目录名在部分文档（CLAUDE.md 项目结构）仍为单模块描述，需同步更新。

## 已执行（2026-08-25）

- `BaseDo`：`infrastructure.common` → `common.data`（gateway-common 模块内）
- `ProviderHealth` 全系列：`infrastructure.actuator` → provider `health`（Probe/Tracker/State/Properties/RegistryHealthIndicator，含 Indicator 端点适配）
- `CredentialEncryptorAdapter`：boot `infrastructure.encryption` → provider `service`
- gateway-boot 收拢：`infrastructure.config/event`、`application.init` → `boot.config/init/event`
- `GatewayApplication` 扫描配置：移除 `infrastructure`/`application` 扫描项
- gateway-web 根包改名：`adapter.*` → `web.*`（api/interceptor/advice 子包保留），同步更新 `WebAutoConfiguration` 自动配置注册文件（`AutoConfiguration.imports`）、`WebConfiguration` 的 `@ComponentScan`、ArchUnit `LayerDependencyTest` 5 处引用
- 测试包路径清理：boot 的 `application/listener`（AuditEventListenerTest/TokenUsageEventListenerTest 增强版归位对应域模块测试包）、`application/catalog`、`adapter/api` 遗留集成测试归位 boot `integration` 包

## 未决

- boot 测试中少量"包名跟随被测域模块"的测试（如 `iam/application`、`iamdata/gateway`、`proxy/invoker` 等约 10 个）为历史遗留，因不影响主代码与构建，暂不处理
- `docs/` 下部分文档（CLAUDE.md 项目结构、constitution.md）仍为单模块分层描述，待同步更新
