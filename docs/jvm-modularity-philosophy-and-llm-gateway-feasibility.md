# JVM 企业应用框架的模块化设计哲学与 llm-gateway 的模块化改造可行性评估

> 本文以 Jmix 框架（`E:\workspace\jmix-3.0.1`）为参照样本，剖析成熟 JVM 企业级框架的模块化设计哲学，并以此为镜，评估 llm-gateway（`E:\workspace\llm-gateway`）当前的模块化现状与后续改造的可行性。
>
> 技术背景：Jmix 3.0 基于 Spring Boot 4.0 / Vaadin 25 的全栈企业应用框架；llm-gateway 是 Java 21 + Spring Boot 3.5 + PostgreSQL + Redis 的 AI 模型 API 聚合分发网关（APIPark 竞品），采用 COLA Light 5.0 架构。

---

## 一、引言：为什么要谈"模块化"

模块化是大型企业级应用无法回避的架构命题。当代码规模从几百行增长到几十万行，从"能跑"到"要长期演进"，模块化决定了系统的可维护性、可扩展性与团队协作效率。但"模块化"本身是个模糊词——按什么切？切到什么粒度？用什么技术机制承载边界？不同的框架给出了截然不同的答案。

Jmix 与 llm-gateway 恰好代表了两种典型思路：

- **Jmix 是"能力/技术域"粒度**——几十个 addon 各成独立 jar，天然可插拔复用；
- **llm-gateway 是"架构层"粒度**——单模块内用 package 划分 adapter/application/domain/infrastructure，靠约定隔离。

本文先深入 Jmix 的模块化设计，提炼其哲学，再评估 llm-gateway 的现状与改造路径。

---

## 二、Jmix 框架的模块化设计哲学

### 2.1 顶层模块划分：约 120 个 Gradle 子项目，按功能组聚合

`E:\workspace\jmix-3.0.1\settings.gradle` 通过 `includeBuild`/`includeProject` 声明了约 120 个 Gradle project。每个顶层目录（addon 组）内部通常包含 `<功能>`、`<功能>-starter`，UI 模块另有 `-flowui`、`-flowui-kit` 等子模块。

模块大致分为六类：

| 分类 | 顶层目录 | 说明 |
|------|---------|------|
| **构建/工具**（独立 includeBuild） | `jmix-gradle-plugin`、`jmix-build`、`jmix-templates`、`jmix-translations`、`jmix-bom` | Gradle 插件、框架构建逻辑、Studio 模板、翻译、版本 BOM |
| **核心框架** | `jmix-core` | 基石模块（core / core-starter） |
| **数据模块** | `jmix-data` | data（抽象）/ eclipselink（JPA 实现）/ data-autoconfigure |
| **UI 模块** | `jmix-flowui` | flowui / flowui-kit / flowui-data / flowui-restds … |
| **安全/REST/认证** | `jmix-security`、`jmix-rest`、`jmix-restds`、`jmix-authserver`、`jmix-oidc`、`jmix-saml`、`jmix-ldap` | 权限、REST、远程数据、认证方式 |
| **业务扩展（addon）** | `jmix-audit`、`jmix-email`、`jmix-reports`、`jmix-quartz`、`jmix-search`、`jmix-dynattr`、`jmix-charts`、`jmix-fullcalendar` 等 | 各业务功能模块，含 `-flowui`/`-flowui-kit`/`-starter` |

### 2.2 模块划分原则：四条轴

从 `README.md`（"rich set of functional modules... plug in advanced system functionality with just a few lines of code"）和 `AGENTS.md` 以及各模块依赖声明中，可提炼出 Jmix 划分模块的**四条原则**：

1. **按领域职责切分**：每个 addon 是独立业务领域（audit=审计、email=邮件、reports=报表、quartz=调度、search=搜索），各自独立坐标（`io.jmix.audit:jmix-audit`）。

2. **"核心 vs 扩展"两级分层**：`jmix-core` 是唯一不被其他模块反向依赖的基石；`jmix-data` 是第二层；`jmix-flowui`/`jmix-rest` 是第三层；security 及业务 addon 在最上。

3. **"接口与实现"分离**：数据层分为 `data`（抽象）/`eclipselink`（JPA 实现），文件存储分 `localfs`/`awsfs` 两实现，实现可替换。

4. **"功能与装配"分离**：每个功能模块配套 `-starter`，把 Spring 自动配置从业务逻辑剥离。

5. **"逻辑与界面"分离**：凡有 UI 的 addon 拆出 `-flowui`（依赖 flowui）与 `-flowui-kit`（仅依赖 Vaadin 组件，不依赖 core），使纯 REST 应用不引入 UI 依赖。

> 这是 Jmix 模块化哲学的核心：**模块边界不是"架构层"而是"业务能力 + 技术域"**，每个 addon 是一个完整的纵向切片。

### 2.3 核心 vs 扩展（addon）机制：BOM + Gradle 插件 + Spring 自动配置

"核心"与"扩展"的区分由三层机制共同实现：

**① 版本统一：`jmix-bom/bom.gradle`（Java Platform / BOM）**

```groovy
javaPlatform { allowDependencies() }
dependencies {
    api platform("org.springframework.boot:spring-boot-dependencies:4.0.7")
    ...
    constraints {
        api "io.jmix.audit:jmix-audit:$freeVersion"
        ...
    }
}
```

所有官方模块与社区 addon 都以 `constraints` 声明统一版本；**免费/商业（premium）addon 的分界就在这里**——`premiumVersion += '.trial'`，`jmix-bpm` 等商业模块源码不在此仓库，只在 BOM 中声明。

**② 构建期接入：`jmix-gradle-plugin`（`io.jmix` 插件）**

`JmixPlugin.groovy` 自动为应用引入 BOM，并用"是否应用了 `io.jmix.build`"来区分**应用项目 vs 框架自身模块**（二者互斥）。插件还通过 `EnhancingAction` 对实体做 EclipseLink 字节码增强——这是 Jmix 免写 DAO、通过 `DataManager`/`DataStore` 操作实体的底层支撑。

**③ 运行期接入：Spring Boot 自动配置（starter 机制）**

每个功能模块配套 `-starter`，用 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 注册自动配置类：

```
io.jmix.autoconfigure.core.CoreAutoConfiguration
io.jmix.autoconfigure.eclipselink.EclipselinkAutoConfiguration
io.jmix.autoconfigure.flowui.FlowuiAutoConfiguration
```

应用只需在 `build.gradle` 依赖对应 `-starter`，**无需写任何装配代码**，即插即用。全仓库 30+ 个 `-starter` 模块都走这一机制。

**④ Studio 集成**：`jmix-templates` 提供向导模板，UI 组件通过 `META-INF/services/io.jmix.flowui.kit.meta.component.preview.StudioPreviewComponentLoader`（Java ServiceLoader）向 Studio 预览注册。

### 2.4 模块间依赖关系：单向分层，`jmix-core` 是唯一基石

通过各模块 `<模块名>.gradle` 的 `api project(':...')` 归纳出单向分层：

```
jmix-core  (基石，不依赖任何 jmix 项目模块)
   ▲
jmix-data  (api project(':core'))
   ▲
jmix-eclipselink  (api project(':data'))
   ▲
jmix-flowui (api core, flowui-kit, flowui-themes)
   ▲
安全 / REST / 业务 addon
```

关键证据：`jmix-core/core/core.gradle` **不含任何 `project(':')` 依赖**，全部是外部依赖（Spring Boot、Spring Security、Spring Data、Jakarta Persistence、Guava、dom4j 等）。所有核心模块均直接 `api project(':core')`。

依赖使用方式上，跨模块公开 API 用 `api`，可选/运行时特性用 `compileOnly`（如 rest 对 security/oidc/authserver），测试专用用 `testImplementation`。

### 2.5 技术实现机制：双轨制

| 层面 | 机制 | 证据 |
|------|------|------|
| 构建期 | Gradle 多模块 + 平台 BOM | `settings.gradle`、`jmix-bom/bom.gradle` |
| 构建期 | `io.jmix` 插件实体字节码增强 + 引 BOM | `JmixPlugin.groovy`、`EnhancingAction.groovy` |
| 运行期 | Spring Boot `AutoConfiguration.imports` | 各 `-starter` |
| 运行期 | Spring Bean 注入 + 原型 Bean 工厂（SPI 式） | `DataStore`/`DataStoreFactory` |
| 集成 | Java `META-INF/services` ServiceLoader | `StudioPreviewComponentLoader` |
| 集成 | 旧式 `spring.factories`（少数） | `flowui-test-assist`、`quartz-starter` |

**数据层的 SPI 化是"接口与实现"模块化的典型范例**：

`jmix-core/.../DataStore.java` 接口声明："Implementations of this interface must be **prototype beans**... do not access data stores directly from your application code." 接口在 core，工厂在 core：

```java
// DataStoreFactory.java —— 按 store 名从 Spring 容器取原型 Bean
public DataStore get(String name) {
    String beanName = descriptorsRegistry.getStoreDescriptor(name).getBeanName();
    return dataStores.computeIfAbsent(name, key -> {
        DataStore dataStore = (DataStore) applicationContext.getBean(beanName);
        dataStore.setName(name);
        applicationContext.getBeanProvider(DataStoreCustomizer.class).stream()
                .forEach(customizer -> customizer.customize(dataStore));
        return dataStore;
    });
}
```

JPA 实现在 `jmix-data/eclipselink/.../JpaDataStore.java`，REST 远程存储（`jmix-restds`）、内存存储（`TestInMemoryDataStore`）都以不同 `DataStore` Bean 接入，`DataManager` 统一调度——**任何后端都可作为 DataStore 插件化接入**。

### 2.6 UI 模块化：与核心/数据/安全正交解耦

`flowui-kit` 只含 Vaadin 组件与 SPI，**不依赖 core**；`flowui` 依赖 core；`flowui-data` 依赖 flowui+data+eclipselink；`flowui-restds` 依赖 flowui+restds。业务 addon 普遍拆 `-flowui`/`-flowui-kit`，使"无 UI 应用"只依赖逻辑模块不引入 UI。UI 与数据、安全彼此正交。

---

## 三、llm-gateway 的模块化现状

### 3.1 模块清单与依赖

根 `pom.xml` 声明 4 个 Maven 模块：

```
gateway-project (父 POM, pom 打包)
├── gateway-capability-api   (jar, 无 Spring, 纯 SPI 契约 + Canonical IR)
│     └── (被 gateway-boot 依赖)
├── gateway-boot             (jar, 依赖 capability-api + 全部 Spring 生态)
├── gateway-cli              (jar, 独立, spring-shell, 不依赖 boot)
└── gateway-simulator        (jar, 独立, spring-boot-starter-web, 不依赖 boot)
```

`gateway-console`（前端 React）不在 Maven reactor 内，是独立 Web 工程，走 HTTP 调用。

文件规模：gateway-capability-api 8 个 main 文件、gateway-boot 399 个 main 文件 + 121 个 test、gateway-cli 仅 1 个 main 文件（骨架）、gateway-simulator 10 个 main 文件。

**模块间耦合现状**：除 `gateway-boot → gateway-capability-api` 这一条内聚依赖外，gateway-cli / gateway-simulator 与核心完全解耦（仅 HTTP 消费 API）。

### 3.2 gateway-boot 内部：COLA Light 分层

`gateway-boot` 内部按 COLA Light 5.0 用 package 分层：

```
adapter / application / common / domain / infrastructure
```

- **adapter**：接收请求返回响应（api Controller、interceptor、protocol 校验/调谐器）
- **application**：用例编排、跨域协调（19 个子包：auth/channel/provider/proxy/routing/invoker/protocol/conversion 等）
- **domain**：业务逻辑与领域模型（11 个领域：protocol/supply/proxy/model/iam/quota/usage/audit/alert/dataprotection/threat/resilience）
- **infrastructure**：技术实现（Gateway 实现、config、resilience 的 Retry/CircuitBreaker 等）
- **common**：跨领域共享（dto/entity/event/exception/util）

依赖方向：`adapter → application → domain → infrastructure`。CLAUDE.md 明文约束：分层依赖、Gateway 模式（接口在 `domain/xxx/gateway/`，实现在 `infrastructure/xxx/gateway/`）、依赖倒置、领域模型纯洁性。

### 3.3 能力插件化改造进展：只完成第 1 阶段

llm-gateway 最近的核心工作是**"能力插件化"改造**（设计文档 `docs/superpowers/specs/2026-08-11-capability-plugin-design.md`）。痛点：旧 `ProtocolConverter` 是硬编码的 OpenAI↔Anthropic 两两转换器（N×N 组合爆炸）；`Model.capabilities` 字段仅存储未参与决策；新增协议 = 改核心转换器。

**设计决策**（spec §2）：D2 插件形态 = Spring Boot Starter 构建期模块化（放弃外部 JAR 热插拔）；D3 `gateway-capability-api` 为稳定 SPI（纯接口）；D4 按协议/能力类型划分，不按厂商/模型；D6 插件零厂商数据（厂商数据全进 DB）；D7 本轮仅覆盖 chat 类；D8 每协议一个 Adapter 做"原生↔规范"，消除 N×N。

**已落地的第 1 阶段（Canonical IR + ProtocolAdapter SPI，git `4405d284`~`35ca4088`）**：

| 组件 | 位置 | 状态 |
|------|------|------|
| Canonical IR 7 模型（CanonicalChatRequest/Message/Tool/ToolCall/Response/ContentBlock/Usage） | `gateway-capability-api/.../protocol/` | ✅ |
| ProtocolAdapter SPI | `gateway-capability-api/.../protocol/ProtocolAdapter.java` | ✅ |
| OpenAIProtocolAdapter / AnthropicProtocolAdapter / ProtocolStreamConverter | `gateway-boot/.../infrastructure/protocol/` | ✅ |
| ProtocolConversionFacade | `gateway-boot/.../application/protocol/conversion/` | ✅（偏离计划，见下） |
| 旧 ProtocolConverter | 已删除 | ✅ |

`ProtocolAdapter` SPI（纯接口，无 Spring）：

```java
public interface ProtocolAdapter<T> {
    String protocol();                                          // "openai" / "anthropic"
    CanonicalChatRequest normalizeRequest(T nativeRequest);     // 入站: 原生→规范
    T denormalizeRequest(CanonicalChatRequest canonical);       // 出站: 规范→原生
    CanonicalChatResponse normalizeResponse(Object nativeResponse);
    Object denormalizeResponse(CanonicalChatResponse canonical);
}
```

任意两协议互转 = normalize + denormalize 两跳，**从根上消除 N×N 转换器**。

**两处偏离计划**：① `ProtocolConversionFacade` 实际落在 `application/protocol/conversion/`（非计划中的 domain 层，git `4033d228`），且直接依赖 infrastructure 的 Adapter 具体类，跨层落位；② 流式转换按 YAGNI 保持 JSON 字符串方向，未做 canonical 化。

**未完成（仅设计无代码）**：真正的"插件化"——`Capability` 模型 + `CapabilityRegistry`（能力注册表）、能力感知路由（打通 `Model.capabilities` 参与决策）、`gateway-protocol-openai/anthropic` 独立 Starter 模块拆分（当前两 Adapter 仍在 gateway-boot 内）、健康机制、Gemini 示例插件。

> **结论**：能力插件化目前只完成了"Canonical IR + ProtocolAdapter SPI"这一第 1 阶段（协议转换层的重构与去 N×N）；真正意义上的"插件化"尚在设计阶段。设计文档中的 `gateway-core`、`gateway-protocol-openai/anthropic/gemini` 模块**在物理上还不存在**。

### 3.4 模块化约束与问题

**现有良好实践**：
- 依赖倒置 + Gateway 模式（domain 只依赖接口）
- `gateway-capability-api` 纯净（无 Spring、只依赖 jackson），是真正可被第三方能力模块复用的稳定契约
- gateway-cli / gateway-simulator 是干净的 API 消费者
- 配置外部化、测试覆盖率门槛、全实体可审计

**潜在问题**：
1. **gateway-boot 仍然过重**：399 个 main 文件、11 领域 + 19 个 application 子包全挤在单 jar，所有技术栈耦合在同一 Spring 上下文。
2. **Facade 跨层落位**：`ProtocolConversionFacade`（application）直接依赖 `infrastructure` 的 Adapter 具体类，未走 domain Gateway 接口，违反分层依赖约束——暴露"Adapter 放哪一层"未定的遗留问题。
3. **可复用能力边界未形成**：目前只有 `gateway-capability-api` 是稳定契约，OpenAI/Anthropic Adapter 仍嵌在 gateway-boot，Capability 注册表缺失，`Model.capabilities` 仍"仅存储、未参与决策"。复用边界仅"协议转换"一处打通。
4. **gateway-cli 名不副实**：仅 1 个 main 文件、0 测试，与核心无任何契约共享，潜在 DTO 重复漂移风险。
5. **双粒度并存未统一**：当前按"架构层 + 部署形态"分模块（boot/cli/simulator/capability-api），能力插件化设计希望按"业务能力/协议"分（openai/anthropic/gemini），两种粒度并存是架构演进的主要张力来源。

---

## 四、模块化改造可行性评估

### 4.1 两种模块化哲学的对比

| 维度 | Jmix（能力/技术域 addon 式） | llm-gateway（COLA 架构层式） |
|------|------|------|
| 划分粒度 | 按业务能力/技术域，每个 addon 是完整纵向切片 | 按架构层，单模块内 package 隔离 |
| 模块实体 | 每个 addon 独立 jar，天然可插拔复用 | `gateway-boot` 单 jar 承载所有层次 |
| 复用边界 | addon 即为复用单元，跨项目可装 | 目前仅 `gateway-capability-api` 可复用 |
| 组织方式 | 靠 addon 依赖关系（api/compileOnly） | 靠层间依赖倒置 + Gateway 模式 |
| 版本管理 | BOM 统一 + 免费/商业分层 | 父 POM dependencyManagement |

二者的本质差异是**模块边界的定义维度不同**：COLA 用"架构层"定义边界，Jmix 用"业务能力/技术域"定义边界。llm-gateway 的能力插件化改造，本质上正是在 COLA 分层之上**引入第二种"能力模块"粒度**。

### 4.2 可行性总体判断

**结论：llm-gateway 的模块化改造方向正确、技术可行，且已经完成了最关键的"契约地基"（Canonical IR + ProtocolAdapter SPI），但距离真正的插件化还有清晰且可控的距离。**

理由：

1. **方向已被验证**：Jmix 证明"BOM 统一版本 + Gradle/Maven 多模块 + Spring Boot Starter 自动配置 + 纯接口 SPI"这套组合是成熟可靠的模块化范式。llm-gateway 的技术栈（Spring Boot 3.5）天然支持 Spring Boot Starter 机制，与 Jmix 的 addon 模型同构。

2. **契约地基已就位**：`gateway-capability-api` 无 Spring 依赖、纯接口 + Canonical IR，设计上对标 Jmix 的"接口在 core、实现在 addon"。这是整个改造中最难、最不可逆的部分，**已经完成**。

3. **剩余工作边界清晰**：CapabilityRegistry、能力感知路由、独立 Starter 模块拆分、健康机制——每一项都是增量、可独立交付的，且已有设计文档（spec §3、§9）作为蓝图。

### 4.3 需要解决的三个前置问题（可行性"扣分项"）

尽管总体可行，以下三个问题若不解决，会阻碍"插件化"真正落地：

**① Adapter 的层归属未定（架构遗留）**
当前 `ProtocolConversionFacade` 跨层落位、Adapter 既实现 capability-api SPI 又被放在 infrastructure 却未被 domain 抽象。若不先明确"Adapter 属于哪个层/谁的边界"，拆分独立模块时会反复调整，成本高。

**② 双粒度并存的统一问题**
"架构层模块（boot/cli/simulator）"与"能力模块（openai/anthropic）"两种粒度如何共存，需要明确的模块化策略（谁是主切面、如何组装）。这是架构决策，不是编码问题。

**③ "插件化"程度与需求的匹配度**
llm-gateway 是**单实例部署的私有网关**，不是多团队共建的开源框架。是否需要完整的 addon 生态（Studio、BOM 商业分层、ServiceLoader 热插拔）值得权衡——对内部项目，"Starter 构建期模块化 + 能力注册表"可能已经足够，过度模块化反而增加复杂度。

### 4.4 分阶段演进路径（可行性落地）

基于 Jmix 的模块化范式，llm-gateway 的改造可分四步，每步可独立交付、可验证：

| 阶段 | 内容 | 参考 Jmix 机制 | 可验证产物 |
|------|------|---------------|-----------|
| **P1（已完成）** | Canonical IR + ProtocolAdapter SPI | core 定义接口 | `gateway-capability-api` |
| **P2** | 拆 `gateway-protocol-openai/anthropic` 独立 Starter 模块 + 各自 AutoConfiguration.imports | `-starter` + 自动配置 | 两个可独立安装的协议能力模块 |
| **P3** | `CapabilityRegistry` + 能力感知路由，打通 `Model.capabilities` 参与决策 | `DataStoreFactory`/`StoreDescriptorsRegistry` 按名装配 | 新增协议无需改核心 |
| **P4** | 健康机制（HealthIndicator/HealthGroup）、Gemini 示例插件验证可扩展性 | addon 生态 | 第三方能力即插即用 |

### 4.5 风险与建议

**风险**：
- **过度模块化的反噬**：私有单实例网关若完全照搬 Jmix 的 addon 生态（BOM 商业分层、Studio、热插拔），会引入与需求不匹配的复杂度。
- **跨层落位的债务**：Facade 当前违反分层约束，若不及时治理会固化错误边界。
- **契约冻结过早**：Canonical IR 作为稳定 SPI，若在 embedding/rerank 能力扩展时发现模型缺字段，改契约会牵动所有 Adapter。

**建议**：
1. **先定边界再拆模块**：在拆分独立能力模块前，先明确"Adapter 归属层"与"双粒度共存策略"，避免返工。
2. **以"新增一个协议的成本"为验收标准**：改造成功的标志应是——新增一种协议（如 Gemini）只需新增一个 Adapter 模块 + DB 配置，不改任何核心代码。
3. **渐进式、不毕其功于一役**：按 P2→P4 逐步推进，每阶段独立验证，避免大爆炸式重构。
4. **借鉴 Jmix 的关键机制**：BOM 统一版本、`-starter` 自动配置、能力按名装配注册表——这三样是模块化范式的精髓，值得重点吸收。

---

## 五、结论

Jmix 用"BOM + Gradle 插件 + Spring Boot Starter 自动配置 + 纯接口 SPI"这套组合，实现了成熟且优雅的能力级模块化，其核心哲学是**"以业务能力/技术域为模块边界，接口与实现、功能与装配、逻辑与界面彻底分离"**。

llm-gateway 采用 COLA 架构层式模块化，单模块内分层隔离清晰，且已完成能力插件化最关键的"契约地基"（Canonical IR + ProtocolAdapter SPI），方向正确、技术可行。其可行性取决于三个前置问题的治理（Adapter 层归属、双粒度统一、插件化程度与需求匹配），以及是否严格遵循"先定边界、再拆模块、分阶段渐进"的改造纪律。

**一句话总结**：llm-gateway 的模块化改造不是"能不能做"的问题，而是"如何做得恰到好处"的问题——借鉴 Jmix 的模块化范式，但以自身"私有单实例网关"的定位为尺，避免过度设计。

---

## 附：关键文件索引

**Jmix（`E:\workspace\jmix-3.0.1`）**
- `settings.gradle`（模块清单）
- `jmix-bom/bom.gradle`（版本 BOM）
- `jmix-gradle-plugin/src/main/groovy/io/jmix/gradle/JmixPlugin.groovy`（实体增强 + 引 BOM）
- `jmix-core/core/core.gradle`（基石模块依赖）
- `jmix-core/core/src/main/java/io/jmix/core/DataStore.java`、`impl/DataStoreFactory.java`（数据 SPI）
- 各 `-starter` 的 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

**llm-gateway（`E:\workspace\llm-gateway`）**
- `pom.xml`（模块声明）
- `CLAUDE.md`、`doc/constitution.md`、`doc/spec.md`（架构意图）
- `docs/superpowers/specs/2026-08-11-capability-plugin-design.md`（能力插件化设计）
- `docs/superpowers/plans/2026-08-12-capability-plugin-phase1.md`（第 1 阶段计划）
- `gateway-capability-api/.../protocol/*.java`（Canonical IR + ProtocolAdapter SPI）
- `gateway-boot/.../infrastructure/protocol/*.java`（OpenAI/Anthropic Adapter、StreamConverter）
- `gateway-boot/.../application/protocol/conversion/ProtocolConversionFacade.java`
- `gateway-boot/.../application/proxy/invoker/ChannelFailoverInvoker.java`
