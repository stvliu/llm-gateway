# Jmix 框架模块化设计研究报告

> 研究对象：`E:\workspace\jmix-3.0.1`（Jmix 3.0，基于 Spring Boot 4.0 / Vaadin 25 的全栈企业应用框架）
> 核心结论先行：Jmix 采用 **Gradle 多模块 + Spring Boot 自动配置（starter/autoconfigure 模式）双轨** 实现模块化；以 `jmix-core` 为唯一基石，`core → data → flowui/rest → security → 业务扩展模块` 单向分层；"核心/扩展（addon）" 的区分通过 **BOM 统一版本 + Gradle 插件实体增强 + 各 addon 独立坐标与 starter 自动装配** 实现。

---

## 1. 顶层模块划分

### 1.1 模块清单（来自 `settings.gradle`）

根目录 `settings.gradle`（共约 120 个 Gradle project）通过 `includeBuild`/`includeProject` 声明模块。核心结构：

```groovy
rootProject.name = 'jmix'
includeBuild 'jmix-gradle-plugin'
includeBuild 'jmix-build'
includeBuild 'jmix-templates'
includeBuild 'jmix-translations'
includeProject('bom', 'jmix-bom')
includeProject('core', 'jmix-core/core')
includeProject('core-starter', 'jmix-core/core-starter')
includeProject('framework-test-support', 'jmix-core/framework-test-support')
includeProject('data', 'jmix-data/data')
includeProject('data-autoconfigure', 'jmix-data/data-autoconfigure')
includeProject('eclipselink', 'jmix-data/eclipselink')
includeProject('eclipselink-starter', 'jmix-data/eclipselink-starter')
includeProject('flowui', 'jmix-flowui/flowui')
includeProject('flowui-starter', 'jmix-flowui/flowui-starter')
...
```

关键机制：`includeProject(String name, String path, boolean changeBuildFileName = true)` 会把每个模块的 build 文件重命名为 `<模块名>.gradle`（例如 `jmix-data/data/data.gradle`）。这是识别各模块依赖声明的入口。

### 1.2 模块分类表

每个顶层目录通常包含多个 Gradle 子项目（`<功能名>`、`<功能名>-starter`，UI 模块另有 `-flowui`、`-flowui-kit`）。分类如下：

| 分类 | 顶层目录（addon 组） | 内部子模块 | 说明 |
|------|---------------------|-----------|------|
| **构建/工具**（独立 includeBuild） | `jmix-gradle-plugin` | — | `io.jmix` Gradle 插件，实体增强（Entity Enhancing）+ BOM 引入 |
| | `jmix-build` | — | `io.jmix.build` 内部插件，封装框架自身构建逻辑 |
| | `jmix-templates` | — | Studio 新建项目向导模板 |
| | `jmix-translations` | — | 框架翻译资源 |
| | `jmix-bom` | `bom` | Java Platform（BOM），统一版本约束 |
| **核心框架** | `jmix-core` | `core`, `core-starter`, `framework-test-support` | 基石模块 |
| **数据模块** | `jmix-data` | `data`, `data-autoconfigure`, `eclipselink`, `eclipselink-starter` | 数据访问 + JPA 实现（EclipseLink） |
| **UI 模块** | `jmix-flowui` | `flowui`, `flowui-kit`, `flowui-themes`, `flowui-data`, `flowui-restds`, `flowui-devserver`, `flowui-test-assist` 及各自 starter | Vaadin Flow UI |
| **安全模块** | `jmix-security` | `security`, `security-data`, `security-flowui`, `security-resource-server` 及 starter | 权限/认证 |
| **REST 模块** | `jmix-rest` | `rest`, `rest-starter`, `sample-rest` | REST API |
| **远程数据源** | `jmix-restds` | `restds`, `restds-starter`, `sample-common`, `sample-rest-service` | REST 数据存储 |
| **认证扩展** | `jmix-authserver`, `jmix-oidc`, `jmix-saml`, `jmix-ldap` | 各含 `-starter` | 认证方式 |
| **会话/集群** | `jmix-sessions`, `jmix-multitenancy` | 各含 `-starter`/`-flowui` | 会话管理、多租户 |
| **文件存储** | `jmix-localfs`, `jmix-awsfs` | 各含 `-starter` | 文件系统实现 |
| **业务扩展（addon）** | `jmix-audit`, `jmix-email`, `jmix-dynattr`, `jmix-datatools`, `jmix-reports`, `jmix-quartz`, `jmix-search`, `jmix-dataimport`, `jmix-bulkeditor`, `jmix-appsettings`, `jmix-pessimisticlock`, `jmix-jmxconsole`, `jmix-messagetemplates`, `jmix-aitools`, `jmix-superset` 等 | 各含主模块 + `-flowui` + `-kit` + 相应 starter | 业务功能模块 |
| **图表/UI 组件扩展** | `jmix-charts`, `jmix-fullcalendar`, `jmix-gridexport`, `jmix-pivottable` | 各含 `-flowui`/`-flowui-kit`/`-starter` | UI 组件 |
| **独立模块** | `jmix-masquerade` | — | 单模块（无子目录） |

> 注：`jmix-bpm`、`jmix-kanban`、`jmix-dynmodel` 等**商业（premium）模块**不在本仓库源码中，只在 BOM 中声明（见 §3）。

---

## 2. 模块划分原则

### 2.1 文档依据

- `AGENTS.md`（被 `CLAUDE.md` 通过 `@AGENTS.md` 引用）明确说明：
  > "Multi-module Gradle workspace for Jmix development. Standard Gradle layouts (`src/main`, `src/test`) per module. **BOM for shared dependencies and Jmix modules in `jmix-bom`**."
- `README.md`：Jmix 定位为 "high-level full-stack framework"，"**rich set of functional modules**。You can plug in advanced system functionality ... with just a few lines of code."——模块即插即用是设计目标。
- `README.md` 对四个构建组件的分工描述：
  > "`jmix-gradle-plugin` - a Gradle plugin for building Jmix applications. `jmix-build` - an internal Gradle plugin which encapsulates the framework build logic. It's not used when building applications. `jmix-templates` - templates used by Studio new project wizard. `jmix-translations` - framework translations."

### 2.2 划分逻辑

1. **按领域职责切分**：每个 addon 是一个独立业务领域（audit=审计、email=邮件、reports=报表、quartz=调度、search=搜索等），各自打包独立坐标（`io.jmix.audit:jmix-audit`、`io.jmix.quartz:jmix-quartz` 等）。
2. **"核心 vs 扩展"两级分层**：`jmix-core` 是唯一不被其他模块反向依赖的基石；`jmix-data` 是第二层核心；`jmix-flowui`/`jmix-rest` 是第三层（UI/API）；`security` 及所有业务 addon 在最上层。
3. **"接口与实现"分离**：数据层分为 `data`（抽象 + 通用）/`eclipselink`（具体 JPA 实现）/`data-autoconfigure`（Liquibase 等装配），实现可替换（文件存储也分 `localfs`/`awsfs` 两实现）。
4. **"功能与装配"分离**：每个功能模块配套 `-starter`，把 Spring 自动配置从业务逻辑中剥离（如 `core-starter`、`eclipselink-starter`、`flowui-starter`）。
5. **"逻辑与界面"分离**：凡有 UI 的 addon 都会拆出 `-flowui`（依赖 `jmix-flowui`）与 `-flowui-kit`（仅依赖 Vaadin 组件，不依赖 core），使无界面应用（如纯 REST）不引入 UI 依赖。

---

## 3. 核心 vs 扩展机制（addon）

### 3.1 版本统一：`jmix-bom/bom.gradle`

`jmix-bom/bom.gradle` 是一个 `java-platform`（Gradle Java Platform / BOM）：

```groovy
group = 'io.jmix.bom'
javaPlatform { allowDependencies() }
dependencies {
    api platform("org.springframework.boot:spring-boot-dependencies:4.0.7")
    api platform('org.springframework.ai:spring-ai-bom:2.0.0')
    ...
    constraints {
        // community add-ons
        api "gr.netmechanics.jmix:azurefs:3.0.0"
        ...
        api "io.jmix.audit:jmix-audit:$freeVersion"
        api "io.jmix.audit:jmix-audit-starter:$freeVersion"
        ...
    }
}
```

- 所有官方模块与社区 addon 都在 BOM 中以 `constraints` 声明统一版本（`$freeVersion`/`$premiumVersion`）。
- **premium（商业）addon 与免费模块的区分就在此**：`premiumVersion += '.trial'`（构建试用版时），`io.jmix.bpm/businesscalendar/groupgrid/kanban/dynmodel` 等使用 `$premiumVersion`，源码不在此仓库。

### 3.2 addon 接入核心：`jmix-gradle-plugin`（`io.jmix` 插件）

`jmix-gradle-plugin/src/main/groovy/io/jmix/gradle/JmixPlugin.groovy`：

- 插件 id 为 `io.jmix`（见 `jmix-gradle-plugin/build.gradle` 的 `gradlePlugin.plugins` 块）。
- **自动引入 BOM**：
  ```groovy
  if (isJmixApp(project) && project.jmix.useBom) {
      String bomVersion = project.jmix.bomVersion ?: getBomVersion()
      def platform = project.dependencies.platform("io.jmix.bom:jmix-bom:$bomVersion")
      project.dependencies.add('implementation', platform)
      ...
  }
  ```
- **核心/扩展判定**：`private boolean isJmixApp(Project project) { !project.plugins.hasPlugin('io.jmix.build') }`——即**应用项目**用 `io.jmix` 插件引入 BOM；**框架自身模块**用 `io.jmix.build`（被 `build.gradle` 的 `configure(subprojects ...) { apply(plugin: 'io.jmix.build') }` 应用），从而两者互斥。
- 插件从自身 MANIFEST 读取 `Jmix-BOM-Version` 属性，自动关联配套 BOM 版本。
- **实体增强（bytecode enhancing）**：插件通过 `enhanceJmixMain`/`enhanceJmixTest` 任务调用 `EnhancingAction`，用 EclipseLink `org.eclipse.persistence.jpa:5.0.0-1-jmix` 在编译后对实体做字节码增强（生成 `persistence.xml`/`orm.xml` 描述符），这是 Jmix 数据访问的核心机制。
- `JmixExtension.groovy` 暴露 `useBom`/`bomVersion`/`entitiesEnhancing.enabled` 等配置。

### 3.3 运行期接入：Spring Boot 自动配置（starter 机制）

每个功能模块配套 `-starter` 模块，用 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 注册自动配置类（见 §5），这是 addon 在运行期"被发现并接入核心"的标准途径。示例：
- `jmix-core/core-starter/.../AutoConfiguration.imports`：
  ```
  io.jmix.autoconfigure.core.CoreAutoConfiguration
  io.jmix.autoconfigure.core.cluster.ClusterApplicationEventChannelAutoConfiguration
  io.jmix.autoconfigure.core.cluster.LocalApplicationEventChannelAutoConfiguration
  ```
- `jmix-data/eclipselink-starter/...`：
  ```
  io.jmix.autoconfigure.eclipselink.EclipselinkAutoConfiguration
  io.jmix.autoconfigure.data.JmixLiquibaseAutoConfiguration
  io.jmix.autoconfigure.eclipselink.JmixEclipseLinkChannelAutoConfiguration
  ```
- `jmix-flowui/flowui-starter/...`：
  ```
  io.jmix.autoconfigure.flowui.FlowuiAutoConfiguration
  ```

### 3.4 Jmix Studio 的 addon 机制

- Studio（IntelliJ 插件，不在本仓库源码内，仓库提供支撑资源）：
  - `jmix-templates` 提供新项目/模块向导模板；
  - `jmix-gradle-plugin` 的 `JmixExtension` 支持 `zipProject` 任务（`ZipProject.groovy`）、依赖管理，使 Studio 生成的项目直接可构建；
  - Studio 通过向导把 addon 坐标写进 `build.gradle`，并利用各 `-starter` 的自动配置实现即插即用。
- UI 组件与 Studio 集成的 SPI：`META-INF/services/io.jmix.flowui.kit.meta.component.preview.StudioPreviewComponentLoader`（见 `jmix-fullcalendar/fullcalendar-flowui-kit`、`jmix-pivottable/pivottable-flowui-kit`、`jmix-superset/superset-flowui-kit`），这是 addon 的 UI 组件向 Studio 预览功能注册的 Java SPI 机制。
- `README.md`：Studio 辅助 "creating and configuring a project, defining its data model, generating database migration scripts, developing UI views"。

---

## 4. 模块间依赖关系

### 4.1 依赖方向（分层）

通过各模块 `<模块名>.gradle` 的 `api project(':...')` 声明归纳出单向分层：

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

具体证据（`grep "project(':"` 各模块 gradle 文件）：

| 模块 | 文件 | 项目内依赖 |
|------|------|-----------|
| core | `jmix-core/core/core.gradle` | **无** `project(':')`（仅依赖第三方：spring-boot、spring-security、spring-data-commons、jakarta.persistence 等） |
| data | `jmix-data/data/data.gradle` | `api project(':core')` |
| eclipselink | `jmix-data/eclipselink/eclipselink.gradle` | `api project(':data')` |
| data-autoconfigure | `jmix-data/data-autoconfigure/data-autoconfigure.gradle` | `api project(':data')` |
| flowui | `jmix-flowui/flowui/flowui.gradle` | `api project(':core')`, `:flowui-kit`, `:flowui-themes` |
| flowui-data | `jmix-flowui/flowui-data/flowui-data.gradle` | `api project(':flowui')`, `:data`, `:eclipselink` |
| flowui-restds | `jmix-flowui/flowui-restds/flowui-restds.gradle` | `api project(':flowui')`, `:restds` |
| security | `jmix-security/security/security.gradle` | `api project(':core')` |
| security-data | `jmix-security/security-data/security-data.gradle` | `api project(':security')`, `:data`, `:eclipselink` |
| rest | `jmix-rest/rest/rest.gradle` | `api project(':core')`, `:security-resource-server`; `compileOnly :security/:oidc/:authserver` |
| restds | `jmix-restds/restds/restds.gradle` | `api project(':core')`; `compileOnly :security/:oidc` |

### 4.2 `jmix-core` 是唯一基石（已确认）

- `jmix-core/core/core.gradle` 不含任何 `project(':')` 依赖，全部是外部依赖（Spring Boot、Spring Security、Spring Data Commons、Jakarta Persistence、Guava、dom4j 等）。
- 所有核心模块（data/eclipselink/flowui/security/rest/restds）均直接 `api project(':core')`。
- 跨模块的核心类型证据：`io.jmix.core.DataStore`（在 `jmix-core`，被数据实现引用）、`io.jmix.core.impl.DataStoreFactory`、`io.jmix.core.Stores`。

### 4.3 依赖使用方式

- `api` 依赖：跨模块公开 API（如 data 依赖 core 用 `api`，eclipselink 依赖 data 用 `api`）。
- `compileOnly`：可选/运行时特性（如 rest `compileOnly :security/:oidc/:authserver`，restds `compileOnly :security/:oidc`）。
- `testImplementation`：仅测试用（如 data 的 `testImplementation project(':eclipselink')`，flowui 的 `testImplementation project(':flowui-test-assist')`）。
- 装配示例 `jmix-rest/sample-rest/sample-rest.gradle`：一个真实应用把所有 starter 组合起来：
  ```groovy
  implementation project(':rest-starter')
  implementation project(':core-starter')
  implementation project(':eclipselink-starter')
  implementation project(':security-starter')
  implementation project(':security-data-starter')
  implementation project(':authserver-starter')
  implementation project(':localfs-starter')
  ...
  ```

---

## 5. 技术实现机制

### 5.1 双轨机制总览

| 层面 | 机制 | 证据 |
|------|------|------|
| 构建期 | Gradle 多模块 + 平台 BOM | `settings.gradle`、`jmix-bom/bom.gradle` |
| 构建期 | `io.jmix` 插件做实体字节码增强 + 引 BOM | `JmixPlugin.groovy`、`EnhancingAction.groovy` |
| 运行期 | Spring Boot `AutoConfiguration.imports` 自动配置 | 各 `-starter/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` |
| 运行期 | Spring Bean 注入 + 原型 Bean 工厂（SPI 式） | `DataStore`/`DataStoreFactory`/`StoreDescriptorsRegistry` |
| 集成 | Java `META-INF/services` ServiceLoader | `StudioPreviewComponentLoader` |
| 集成 | 旧式 `spring.factories`（少数） | `jmix-flowui/flowui-test-assist`、`jmix-quartz/quartz-starter` |

### 5.2 自动配置（核心机制）

全仓库 **30+ 个 `-starter` 模块**都在 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 中声明自动配置类（Spring Boot 3.x 新格式）。这是 addon 在运行期接入核心的**标准发现机制**——应用只需在 `build.gradle` 依赖对应 `-starter`，无需写任何装配代码。

### 5.3 数据层 SPI（接口在 core，实现在 addon）

这是"接口与实现"模块化的典型证据，展示了如何通过 Spring Bean 机制把数据实现插件化：

- **接口在 core**：`jmix-core/core/src/main/java/io/jmix/core/DataStore.java`
  > "Implementations of this interface must be **prototype beans**. They are used by `DataManager`, do not access data stores directly from your application code."
  接口含 `getName/setName/load/loadList/getCount/save/loadValues` 等 CRUD 方法。
- **工厂在 core**：`jmix-core/core/src/main/java/io/jmix/core/impl/DataStoreFactory.java`（`@Component("core_DataStoreFactory")`）
  ```java
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
  说明：core 定义 `DataStore` 接口与 `DataStoreFactory`，通过 `StoreDescriptorsRegistry`（`jmix-core/core/src/main/java/io/jmix/core/impl/StoreDescriptorsRegistry.java`）按名字查 Bean，用 `applicationContext.getBean(beanName)` 从 Spring 容器取**原型 Bean 实现**——即数据存储实现（如 JPA）由各 addon 通过 Spring 注册，core 只负责按名实例化。
- **实现插件化**：JPA 实现在 `jmix-data/eclipselink/src/main/java/io/jmix/eclipselink/impl/JpaDataStore.java`（实现 `DataStore`），并通过 `JpaDataStoreCustomizer` 定制。不同的存储（RDBMS/JPA、REST、内存等）通过向 Spring 容器注册不同 `DataStore` Bean 接入，`DataManager` 统一调度。

### 5.4 其他 SPI / 服务加载

- `META-INF/services/io.jmix.flowui.kit.meta.component.preview.StudioPreviewComponentLoader`：UI 组件 addon 用标准 Java ServiceLoader 向 Studio 预览组件注册（`jmix-fullcalendar`、`jmix-pivottable`、`jmix-superset` 的 `-flowui-kit`）。
- `jmix-flowui/flowui-devserver/src/main/resources/META-INF/services/org.slf4j.spi.SLF4JServiceProvider`：日志服务提供者。
- 少数 `spring.factories`（旧机制，`:flowui-test-assist`、`:quartz-starter`）。

### 5.5 命名约定佐证模块化

`AGENTS.md` 强调 Bean 显式命名 `@Component("module_BeanName")`、View 路由 `kebab-case` 加模块前缀（如 `sec/resource-role-models`），说明跨模块按命名空间隔离、避免冲突。

---

## 6. 数据/实体模块化特点

### 6.1 数据模块拆分（jmix-data 目录）

```
jmix-data/
├── data/                 # 抽象 + 通用数据访问（io.jmix.data）
├── data-autoconfigure/   # Liquibase 等自动装配（JmixLiquibaseAutoConfiguration）
├── eclipselink/          # JPA 具体实现（io.jmix.eclipselink）
└── eclipselink-starter/  # Spring 自动配置入口
```

- `data` 依赖 `core`，`eclipselink` 依赖 `data`，`data-autoconfigure` 依赖 `data`。
- 实体元模型（`io.jmix.core.metamodel`）与 `DataStore` 接口放在 `jmix-core`，数据实现放 `jmix-data/eclipselink`，实现可替换（`TestInMemoryDataStore` 存在即证）。

### 6.2 不同数据存储的抽象方式

- **统一接口**：`io.jmix.core.DataStore` 抽象 CRUD，`DataStoreFactory` + `StoreDescriptorsRegistry` 按 `store name` 从 Spring 容器取原型 Bean 实现。
- **多数据源**：`jmix-data/eclipselink/src/test/groovy/data_stores/MultiDbDataManagerTest.groovy` 演示多库场景；`Stores` 类（`jmix-core/core/src/main/java/io/jmix/core/Stores.java`）管理 store 名。
- **远程数据源**：`jmix-restds/restds/restds.gradle`（`api project(':core')`）把远程 REST 服务也实现为一种 `DataStore`，与 JPA store 并列，体现"任何后端都可作为 DataStore 接入"的设计。
- **Liquibase 迁移**：`JmixLiquibaseAutoConfiguration`/`JmixLiquibaseCreator`（`jmix-data/data-autoconfigure`）统一管理数据库迁移脚本。

### 6.3 实体字节码增强

`JmixPlugin` 用 EclipseLink enhancing 对实体做字节码增强（生成 `persistence.xml`/`orm.xml`），这是 Jmix 免写 DAO、通过 `DataManager`/`DataStore` 操作实体的底层支撑。

---

## 7. UI 模块化（jmix-flowui）

### 7.1 flowui 子模块拆分

```
jmix-flowui/
├── flowui/               # 核心 UI 逻辑（依赖 core）
├── flowui-kit/           # Vaadin 组件/元数据（不依赖 core）
├── flowui-themes/        # 主题
├── flowui-data/          # UI 与数据绑定（依赖 flowui + data + eclipselink）
├── flowui-restds/        # UI + 远程数据（依赖 flowui + restds）
├── flowui-devserver/     # 开发服务器
├── flowui-test-assist/   # 测试辅助
└── 对应 -starter 模块     # 自动配置
```

### 7.2 与核心解耦的证据

- `flowui/flowui.gradle`：`api project(':core')`（只依赖 core，不依赖 data/security），UI 通过 core 的接口访问数据。
- `flowui-kit`：**只含 Vaadin 组件与 `StudioPreviewComponentLoader` SPI，不依赖 core**——即纯前端组件层，可独立于框架数据/安全模型存在，addon 的 UI 组件也按此模式拆分（如 `fullcalendar-flowui-kit`）。
- **功能 addon 的 UI 与逻辑分离**：业务 addon 普遍拆出 `-flowui`（依赖 flowui）与 `-flowui-kit`（仅组件），使"无 UI 应用"（如纯 REST 服务）可只依赖逻辑模块不引入 UI。
- 与安全解耦：`security-flowui` 单独存在，`security` 本身只依赖 `core`；UI 界面（security-flowui）与安全逻辑（security）分离。
- 每个 flowui 相关模块都有独立 `-starter`，通过 `AutoConfiguration.imports` 装配（如 `io.jmix.autoconfigure.flowui.FlowuiAutoConfiguration`），运行期无需显式配置即被 Spring Boot 发现。

---

## 8. 关键文件路径索引

| 用途 | 路径 |
|------|------|
| 模块清单/目录映射 | `E:\workspace\jmix-3.0.1\settings.gradle` |
| 根构建逻辑 | `E:\workspace\jmix-3.0.1\build.gradle` |
| 版本/BOM | `E:\workspace\jmix-3.0.1\jmix-bom\bom.gradle` |
| 应用 Gradle 插件 | `E:\workspace\jmix-3.0.1\jmix-gradle-plugin\src\main\groovy\io\jmix\gradle\JmixPlugin.groovy` |
| 插件配置扩展 | `...\jmix-gradle-plugin\src\main\groovy\io\jmix\gradle\JmixExtension.groovy` |
| 实体增强动作 | `...\jmix-gradle-plugin\src\main\groovy\io\jmix\gradle\EnhancingAction.groovy` |
| 内部构建插件 | `E:\workspace\jmix-3.0.1\jmix-build\src\main\groovy\io\jmix\build\JmixBuildPlugin.groovy` |
| core 依赖声明 | `E:\workspace\jmix-3.0.1\jmix-core\core\core.gradle` |
| data 依赖声明 | `E:\workspace\jmix-3.0.1\jmix-data\data\data.gradle` |
| eclipselink 依赖 | `E:\workspace\jmix-3.0.1\jmix-data\eclipselink\eclipselink.gradle` |
| flowui 依赖 | `E:\workspace\jmix-3.0.1\jmix-flowui\flowui\flowui.gradle` |
| security 依赖 | `E:\workspace\jmix-3.0.1\jmix-security\security\security.gradle` |
| rest 依赖 | `E:\workspace\jmix-3.0.1\jmix-rest\rest\rest.gradle` |
| restds 依赖 | `E:\workspace\jmix-3.0.1\jmix-restds\restds\restds.gradle` |
| 应用装配示例 | `E:\workspace\jmix-3.0.1\jmix-rest\sample-rest\sample-rest.gradle` |
| DataStore 接口 | `E:\workspace\jmix-3.0.1\jmix-core\core\src\main\java\io\jmix\core\DataStore.java` |
| DataStore 工厂 | `E:\workspace\jmix-3.0.1\jmix-core\core\src\main\java\io\jmix\core\impl\DataStoreFactory.java` |
| Store 描述符注册 | `E:\workspace\jmix-3.0.1\jmix-core\core\src\main\java\io\jmix\core\impl\StoreDescriptorsRegistry.java` |
| JPA DataStore 实现 | `E:\workspace\jmix-3.0.1\jmix-data\eclipselink\src\main\java\io\jmix\eclipselink\impl\JpaDataStore.java` |
| 自动配置注册 | 各 `-starter\src\main\resources\META-INF\spring\org.springframework.boot.autoconfigure.AutoConfiguration.imports`（如 core-starter、eclipselink-starter、flowui-starter） |
| UI 组件 Studio SPI | `E:\workspace\jmix-3.0.1\jmix-fullcalendar\fullcalendar-flowui-kit\src\main\resources\META-INF\services\io.jmix.flowui.kit.meta.component.preview.StudioPreviewComponentLoader` |
| 编码规范/架构说明 | `E:\workspace\jmix-3.0.1\AGENTS.md`（被 `CLAUDE.md` 引用） |

---

## 9. 结论摘要

1. **模块清单**：约 120 个 Gradle 项目，按功能组（顶层目录）聚合，每组内含 `<功能>`、`-starter`（及 UI 的 `-flowui`/`-flowui-kit`）。
2. **划分原则**：领域职责 + 接口/实现分离 + 功能/装配分离 + 逻辑/界面分离；`core → data → flowui/rest → security → 业务 addon` 单向分层。
3. **核心/扩展机制**：BOM 统一版本（含免费/商业分层）；`io.jmix` Gradle 插件引 BOM 并做实体增强；运行期通过各 `-starter` 的 Spring Boot 自动配置发现接入；Studio 借助 `jmix-templates` + `jmix-gradle-plugin` + `StudioPreviewComponentLoader` SPI 管理 addon。
4. **依赖关系**：`jmix-core` 是唯一基石（无任何 jmix 项目内依赖），所有模块经 `api project(':core')` 依赖它；数据、UI、安全、REST 相互正交。
5. **技术实现**：Gradle 多模块 + Spring Boot AutoConfiguration.imports + Spring Bean/原型 Bean 工厂（`DataStoreFactory`）+ 少量 Java ServiceLoader（`META-INF/services`）/旧式 `spring.factories`。
6. **数据模块化**：`DataStore` 接口在 core，实现以原型 Bean 注册，`DataStoreFactory`/`StoreDescriptorsRegistry` 按 store 名装配；JPA、REST、内存存储均可作为不同 DataStore 接入。
7. **UI 模块化**：`flowui-kit`（纯组件，不依赖 core）与 `flowui`（依赖 core）分离，addon UI 拆 `-flowui`/`-flowui-kit`，UI 与数据/安全正交解耦。
