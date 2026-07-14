# Comet Design Handoff

- Change: cross-platform-packaging
- Phase: design
- Mode: compact
- Context hash: 031f636810960f0279c1a2e8259ac90cad6bc2c46dd49347afbc08be1516b5bf

Generated-by: comet-handoff.sh

OpenSpec remains the canonical capability spec. This handoff is a deterministic, source-traceable context pack, not an agent-authored summary.

## openspec/changes/cross-platform-packaging/proposal.md

- Source: openspec/changes/cross-platform-packaging/proposal.md
- Lines: 1-40
- SHA256: c171e2abfdcfe17fd80c686bbbdc60e105b3a3c7c577a3eb970eff424210dbfd

```md
## Why

前置 `one-click-bare-deploy` 建立的 jpackage 打包链存在两个结构性问题：

1. **跨平台构建受限**：jpackage 打 deb/rpm 依赖系统原生 `dpkg-deb`/`rpmbuild`，必须在目标 OS 上构建，导致 Windows 开发机无法本地产出/验证 deb/rpm，100% 依赖 CI matrix（ubuntu + windows 分头跑）。
2. **配置注入分散且硬编码**：JVM 参数硬编码在 `jpackage --java-options`、业务配置（端口/DB/密钥）散落在 env 文件 + debconf 交互，调优需重打包、配置外部化不统一。

参照 thingsboard 用纯 Java 的 nebula-ospackage 插件跨平台打 deb/rpm（不依赖系统工具），实现单机一次构建出全平台包；并引入单一 `llmgateway.conf` 统一配置外部化，顺带清掉 jpackage 硬编码与 env/debconf 分层冗余。

## What Changes

- **deb/rpm 打包换 nebula-ospackage**：从 jpackage 迁移到 Gradle `nebula.ospackage` 插件（纯 Java 实现 deb/rpm 格式写入），Windows 单机即可同时产出 deb + rpm。**BREAKING**（deb/rpm 内部结构变化：启动器、配置文件布局调整）
- **引入单 llmgateway.conf 配置文件**：`/etc/llm-gateway/llmgateway.conf` 集中管理端口、数据库访问信息、加密因子、JVM 参数、路径，由启动脚本 `source` 注入，替代 env 文件 + jpackage `--java-options` 硬编码。
- **自定义启动脚本**：`/opt/llm-gateway/bin/llm-gateway.sh`（source conf + exec java），替代 jpackage 原生启动器。
- **postinst 改生成 conf**：首次安装生成 conf（含密钥），升级保留（conffile/NOREPLACE 保护）；密钥生成改 `/dev/urandom | base64`，去 openssl 依赖。
- **systemd unit 调整**：`ExecStart` 指向 `llm-gateway.sh`，去掉 `EnvironmentFile`。
- **BREAKING：去掉 debconf 端口交互**：Linux 端口改由 conf 文件配置（默认 8080），deb/rpm 体验统一（rpm 本无 debconf）。
- **CI 矩阵简化**：release.yml 从 `[ubuntu-latest, windows-latest]` 双 job 简化为单 windows job 出 deb + rpm + exe 全套。
- **Windows exe 保留**：jpackage + Inno Setup 不变（无跨平台痛点）。
- **清理冗余**：删除 `llm-gateway.templates`（debconf）、`postinst-rpm`/`prerm-rpm`/`postrm-rpm`（nebula 原生支持 rpm 脚本分离，不再需要 -rpm 后缀 hack）。

## Capabilities

### New Capabilities

（无。本次是对现有 `bare-metal-deploy` 能力的演进重构，不引入新能力。）

### Modified Capabilities

- `bare-metal-deploy`: 打包工具链（jpackage → nebula-ospackage）、配置注入机制（env 文件 + debconf → 单 llmgateway.conf + 启动脚本 source）、CI 矩阵（双 OS → 单 windows job）三项 requirements 变化。

## Impact

- **代码**：不动 `gateway-boot` 业务 Java 源码、不动 `application*.yml` 内容。
- **新增文件**：`deployments/package/build.gradle`、`deployments/package/gradle/wrapper/`、`deployments/package/conf/llmgateway.conf`（模板）、`deployments/package/bin/llm-gateway.sh`。
- **修改文件**：`deployments/package/linux/postinst`/`prerm`/`postrm`、`llm-gateway.service`、`build.sh`、`build.ps1`、`.github/workflows/release.yml`。
- **删除文件**：`llm-gateway.templates`、`postinst-rpm`/`prerm-rpm`/`postrm-rpm`。
- **依赖**：新增 `nebula.ospackage` 8.6.3（Gradle 插件）、gradle-wrapper；去 openssl 系统依赖。
- **CI**：windows-latest runner 需 Gradle（wrapper 自带）+ jpackage（JDK 21 自带）+ Inno Setup（已配）。
- **风险（design 阶段验证）**：nebula 在 Windows runner 打 deb/rpm 未实测、jlink JRE 目录经 nebula `from()` 后 `bin/java` 0755 权限保留、83MB fat jar + 50MB JRE 打 deb 性能、conf 升级保留逻辑平移。

```

## openspec/changes/cross-platform-packaging/design.md

- Source: openspec/changes/cross-platform-packaging/design.md
- Lines: 1-93
- SHA256: 3175ad79a791c42437ac367ea0df6c3a403fc28cd1880340c163cd0cda5708c2

[TRUNCATED]

```md
## Context

前置 `one-click-bare-deploy` 建立了基于 jpackage 的 deb/rpm/exe 打包链：jlink 内置精简 JRE、systemd/WinSW 服务注册、env 文件注入业务参数（DB_URL/SERVER_PORT/GATEWAY_ENCRYPTION_KEY）、debconf 交互端口、jpackage `--java-options` 硬编码 JVM 参数。

两个结构问题：
1. jpackage 打 deb/rpm 依赖系统 `dpkg-deb`/`rpmbuild`，无法跨平台构建，Windows 开发机不能本地产出/验证 deb/rpm，100% 依赖 CI matrix。
2. 配置散落三处：env 文件（业务参数）、debconf（端口）、jpackage `--java-options`（JVM 硬编码），调优需重打包。

参照 thingsboard：用纯 Java 的 nebula-ospackage 跨平台打 deb/rpm，单 conf 文件 source 注入所有配置。

## Goals / Non-Goals

**Goals:**
- Windows 单机一次构建同时产出 deb + rpm + exe（nebula 出 deb/rpm，jpackage+Inno Setup 出 exe）
- 单 `/etc/llm-gateway/llmgateway.conf` 统一管理端口/DB/加密因子/JVM/路径，启动脚本 source 注入
- JVM 参数运行时可调（改 conf 重启生效，不重打包）
- 去掉 debconf，deb/rpm 配置体验统一
- CI 简化为单 windows job

**Non-Goals:**
- 不动 Windows exe 工具链（保留 jpackage + Inno Setup）
- 不动 Docker 部署
- 不补 macOS
- 不分发 gateway-cli / gateway-simulator
- 不做 H2 -> PostgreSQL 迁移引导
- 不动业务 Java 源码与 application*.yml 内容

## Decisions

**D1: deb/rpm 打包换 nebula-ospackage（纯 Java 跨平台）**
- 选择：Gradle `nebula.ospackage` 8.6.3，纯 Java 实现 deb/rpm 格式写入，不依赖系统 dpkg-deb/rpmbuild。
- 备选：jpackage（现状，无法跨平台）、JReleaser（更现代但需重写打包配置）、fpm（Ruby，Windows 支持差）。
- 理由：thingsboard 验证过的方案，纯 Java 跨平台，能自由控制 deb 内部文件布局（塞 jlink JRE、启动脚本、conf）。

**D2: 单 llmgateway.conf 配置文件（source 注入）**
- 选择：`/etc/llm-gateway/llmgateway.conf` 是 shell 脚本，启动脚本 `source` 后 export 为环境变量 + JAVA_OPTS，Spring `${ENV:默认}` 占位符绑定。
- 备选：env 文件 + jpackage --java-options 分层（现状，JVM 硬编码）、纯 yml 编辑（thingsboard 模式，不如 env 云原生）。
- 理由：单文件管所有，shell 表达力（注释/累加/默认值），运维心智最小；source 后仍是环境变量，保留 Spring relaxed binding。

**D3: 自定义启动脚本 llm-gateway.sh**
- 选择：`/opt/llm-gateway/bin/llm-gateway.sh`（source conf + exec java $JAVA_OPTS -jar），systemd ExecStart 指向它。
- 备选：jpackage 原生启动器（不 source conf，JVM 硬编码）。
- 理由：conf 机制前提是启动脚本 source；jpackage 启动器做不到。顺带实现 JVM 参数运行时注入。

**D4: 去掉 debconf 端口交互**
- 选择：端口由 conf 的 SERVER_PORT 配置（默认 8080），deb/rpm 不交互。
- 备选：保留 debconf（hack nebula 注入 templates，维护成本高）。
- 理由：nebula 不原生支持 debconf；rpm 本无 debconf；去掉统一 deb/rpm 体验。Windows Inno Setup 向导端口交互保留（Windows 不动）。

**D5: Gradle 独立 wrapper**
- 选择：`deployments/package/gradle/wrapper/`，build.sh 调 `./gradlew buildDeb buildRpm`，与 Maven 解耦。
- 备选：gradle-maven-plugin 桥接（仿 thingsboard，桥接较重）。
- 理由：独立 wrapper 简单清晰，Maven 只管 fat jar，Gradle 只管打 deb/rpm。

**D6: CI 单 windows job 出全套**
- 选择：release.yml 单 windows-latest job，nebula 出 deb/rpm + jpackage+iscc 出 exe。
- 备选：双 job（ubuntu deb/rpm + windows exe，未兑现单机出全平台）。
- 理由：兑现"本地单机出全平台"核心价值；windows runner 也能跑 nebula（纯 Java）。

**D7: 密钥 /dev/urandom 兜底**
- 选择：postinst 用 `head -c 32 /dev/urandom | base64` 生成密钥，去 openssl 依赖。
- 备选：deb requires openssl（增加系统依赖）。
- 理由：内置 JRE 已不依赖系统 Java，openssl 也应去依赖；与 Windows PowerShell RandomNumberGenerator 对齐。

**D8: jlink JRE 塞进 nebula deb**
- 选择：build.gradle 里 `from(jreDir) into "runtime"`，jlink 产出的 JRE 目录整体打入 deb 的 `/opt/llm-gateway/runtime/`。
- 验证点：nebula 是否保留 `bin/java` 的 0755 可执行位（design 阶段实测）。

## Risks / Trade-offs

- [nebula 在 Windows runner 打 deb/rpm 未实测] -> design 阶段在 Windows 跑一次 buildDeb/buildRpm 验证；thingsboard 已验证可行，风险可控。
- [jlink JRE 目录经 nebula from() 后 bin/java 0755 权限丢失] -> design 阶段检查打包后权限；必要时显式 fileMode 或保留源权限。
- [83MB fat jar + 50MB JRE 打 deb 性能/体积] -> 实测打包时间与 deb 体积；nebula 纯 Java 写 ar 归档，预计可接受。
- [conf 升级保留逻辑] -> conf 标记为 configurationFile（nebula）/ conffile（deb），NOREPLACE；postinst 首次生成、升级不覆盖。
- [debconf 去掉是 BREAKING] -> 现有 debconf 用户需改 conf 配端口；文档说明迁移。
- [Windows 配置机制与 Linux conf 不一致] -> 本次非目标不动 Windows；后续 change 可统一。

## Migration Plan

1. 新增 build.gradle + gradle wrapper + conf 模板 + 启动脚本

```

Full source: openspec/changes/cross-platform-packaging/design.md

## openspec/changes/cross-platform-packaging/tasks.md

- Source: openspec/changes/cross-platform-packaging/tasks.md
- Lines: 1-44
- SHA256: 8c1779b75a4074812d13d19511b583259c2d7be35bbf80090a4a5324fd001e92

```md
## 1. conf 与启动脚本

- [ ] 1.1 新增 conf 模板 `deployments/package/conf/llmgateway.conf`（SERVER_PORT/DB_URL/GATEWAY_ENCRYPTION_KEY 占位符/JAVA_OPTS/路径）
- [ ] 1.2 新增启动脚本 `deployments/package/bin/llm-gateway.sh`（source conf + exec java $JAVA_OPTS -jar）
- [ ] 1.3 改 `deployments/package/linux/llm-gateway.service`（ExecStart 指向 llm-gateway.sh，去掉 EnvironmentFile）

## 2. Gradle/nebula 打包链

- [ ] 2.1 新增 `deployments/package/gradle/wrapper/`（gradle-wrapper.properties + wrapper jar + 脚本）
- [ ] 2.2 新增 `deployments/package/build.gradle`（nebula.ospackage 8.6.3，buildDeb/buildRpm 任务）
- [ ] 2.3 build.gradle 配置 from fat jar + jlink JRE（`from(jreDir) into "runtime"`）+ conf + 启动脚本 + systemd unit
- [ ] 2.4 验证 jlink JRE 经 nebula `from()` 后 `bin/java` 0755 权限保留
- [ ] 2.5 build.gradle 标记 conf 为 configurationFile（升级 NOREPLACE）

## 3. maintainer 脚本迁移

- [ ] 3.1 改 postinst：生成 `/etc/llm-gateway/llmgateway.conf`（首次生成密钥 `head -c 32 /dev/urandom | base64`，升级保留），替代生成 env
- [ ] 3.2 改 prerm/postrm：适配 conf 与 systemd unit 新布局
- [ ] 3.3 合并 -rpm 后缀脚本：nebula 原生支持 buildRpm 的 preInstall/postInstall/prerm/postrm 分离，去除 -rpm hack

## 4. 构建脚本改造

- [ ] 4.1 改 `deployments/package/build.sh`：保留 mvn package + jlink，jpackage deb/rpm 换成 `./gradlew buildDeb buildRpm`
- [ ] 4.2 确认 `deployments/package/build.ps1`（Windows exe，jpackage + Inno Setup）不受影响

## 5. CI 矩阵简化

- [ ] 5.1 改 `.github/workflows/release.yml`：package job 从 matrix `[ubuntu-latest, windows-latest]` 简化为单 windows-latest
- [ ] 5.2 单 windows job 串行执行 gradle 出 deb/rpm + jpackage+iscc 出 exe
- [ ] 5.3 smoke test 调整：deb 用 systemd-ubuntu 容器、rpm 用 systemd-rockylinux 容器、exe 用 windows runner

## 6. 清理冗余

- [ ] 6.1 删除 `deployments/package/linux/llm-gateway.templates`（debconf）
- [ ] 6.2 删除 `deployments/package/linux/postinst-rpm`、`prerm-rpm`、`postrm-rpm`

## 7. 验证

- [ ] 7.1 Windows 开发机跑 build.sh，验证一次产出 deb + rpm（gradle）+ exe（build.ps1）
- [ ] 7.2 deb 在 systemd-ubuntu 容器装，`/actuator/health` 200，改 conf SERVER_PORT 重启生效
- [ ] 7.3 rpm 在 systemd-rockylinux 容器装，`/actuator/health` 200，改 conf 重启生效
- [ ] 7.4 exe 在 Windows 装，WinSW service Running，`/actuator/health` 200
- [ ] 7.5 升级验证：deb/rpm 升级后 conf 保留（端口/密钥不变），`/var/lib/llm-gateway` 数据不丢
- [ ] 7.6 卸载验证：apt/dnf remove 后 `/var/lib/llm-gateway` 数据目录保留

```

## openspec/changes/cross-platform-packaging/specs/bare-metal-deploy/spec.md

- Source: openspec/changes/cross-platform-packaging/specs/bare-metal-deploy/spec.md
- Lines: 1-48
- SHA256: e148aa12438cb0fb77f5c76b62282eaa910c70398c742dd8823b88a14a2b9dfb

```md
## ADDED Requirements

### Requirement: llmgateway.conf 统一配置外部化
系统 SHALL 在 Linux deb/rpm 部署中通过 `/etc/llm-gateway/llmgateway.conf` 单一 shell 配置文件集中管理服务端口、数据库访问信息、加密因子、JVM 参数与运行路径，由启动脚本 `source` 注入为环境变量与 `JAVA_OPTS`，应用通过现有 `${ENV:默认}` 占位符绑定，升级时该文件保留（conffile/NOREPLACE）。

#### Scenario: conf 注入业务参数
- **WHEN** 安装 deb/rpm 并启动服务
- **THEN** `llmgateway.conf` 中的 SERVER_PORT、DB_URL、GATEWAY_ENCRYPTION_KEY 经 `source` 注入为环境变量，Spring 占位符正确绑定，服务以 conf 配置启动

#### Scenario: JVM 参数运行时可调
- **WHEN** 运维人员修改 `llmgateway.conf` 的 JAVA_OPTS（如 -Xmx）后执行 `systemctl restart llm-gateway`
- **THEN** 服务以新 JVM 参数启动，无需重新打包

#### Scenario: 升级保留 conf
- **WHEN** 对已安装实例执行 `apt upgrade` / `dnf upgrade` 到新版本
- **THEN** `/etc/llm-gateway/llmgateway.conf` 不被覆盖，端口/加密因子/DB 配置保持不变

#### Scenario: 首次安装生成加密因子
- **WHEN** 首次安装 deb/rpm
- **THEN** postinst 在 conf 中生成 32 字节 base64 加密因子（`/dev/urandom`），不依赖 openssl

## MODIFIED Requirements

### Requirement: 安装时端口交互设置
系统 SHALL 通过 `llmgateway.conf` 的 `SERVER_PORT`（Linux deb/rpm，默认 8080）或 Inno Setup 安装向导（Windows）配置服务端口，不修改 `application*.yml` 配置文件。Linux deb/rpm 不再使用 debconf 交互。

#### Scenario: Linux 端口由 conf 配置
- **WHEN** 运维人员安装 deb/rpm
- **THEN** 服务端口由 `/etc/llm-gateway/llmgateway.conf` 的 `SERVER_PORT` 决定，默认 8080，改后 `systemctl restart` 生效

#### Scenario: 非交互安装使用默认端口
- **WHEN** 运维人员以非交互方式安装 deb/rpm
- **THEN** 服务以默认端口 8080 启动

#### Scenario: 端口冲突运行时暴露
- **WHEN** conf 配置的端口已被占用（安装不校验占用）
- **THEN** 服务启动失败，systemd `Restart=on-failure` / WinSW `onfailure restart` 反复重启，运维可从服务状态与日志发现冲突

#### Scenario: Windows 安装向导设置端口
- **WHEN** 运维人员在 Inno Setup 安装向导中输入端口
- **THEN** Windows Service 以该端口启动

### Requirement: CI 多平台打包
CI SHALL 在 release tag 触发时，于单个 windows-latest runner 产出 `.deb`、`.rpm`、`.exe` 全部平台安装包并挂到 GitHub Release（deb/rpm 由 nebula-ospackage 跨平台产出，exe 由 jpackage + Inno Setup 产出）。

#### Scenario: release 产出多平台包
- **WHEN** 推送 release tag
- **THEN** 单 windows job 产出 `.deb`、`.rpm`、`.exe` 并附加到 GitHub Release

```
