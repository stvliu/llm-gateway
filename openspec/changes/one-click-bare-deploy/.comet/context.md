# Comet Design Handoff

- Change: one-click-bare-deploy
- Phase: design
- Mode: compact
- Context hash: 45655c3deb8062463d4c972f79f7c273e962927188c5dd672b0c8607cdf7b93d

Generated-by: comet-handoff.sh

OpenSpec remains the canonical capability spec. This handoff is a deterministic, source-traceable context pack, not an agent-authored summary.

## openspec/changes/one-click-bare-deploy/proposal.md

- Source: openspec/changes/one-click-bare-deploy/proposal.md
- Lines: 1-41
- SHA256: d294199fa32e962ef6cb5c8f8ec43cc11fca9541764764f1b7c5ad709dccc9a6

```md
## Why

llm-gateway 当前部署资产存在两处阻碍快速落地的问题：

1. `deployments/docker` 下 Dockerfile/docker-compose 仍引用旧多模块路径（`gateway/gateway-app`、`gateway-web`、`gateway-core`、`gateway-adapter`），与 COLA Light 重构后的单模块 `gateway-boot` 严重失配，`docker-compose up` 根本无法构建。
2. 仅有 Docker 一种部署形态，无法覆盖禁 Docker 的政企内网、嫌 Docker 重的个人开发者等场景，缺少"一条命令装好即用"的非 Docker 路径。

本次补齐基于 jpackage 的系统安装包（deb/rpm/exe）形态，让 `apt install` / `dnf install` / Windows exe 一条命令完成安装、服务注册、启动；同时修复 Docker 失配。

## What Changes

- **新增 jpackage 打包流水线**：基于 `gateway-boot` fat jar 产出 Linux `.deb` + `.rpm` 和 Windows `.exe` 系统安装包，内置精简 JRE（jlink），默认 `local` profile（H2 文件持久化 + Caffeine + local 事件，零外部依赖）。
- **新增服务注册与生命周期管理**：
  - Linux：systemd unit + `postinst`/`prerm`/`postrm` 脚本（注册/停用/清理，数据目录保留）
  - Windows：WinSW 注册 Windows Service（Inno Setup 打 exe installer）
- **新增安装时配置（不改配置文件，全靠环境变量）**：
  - 端口交互设置：debconf（Linux）/ Inno Setup 安装向导（Windows）收集 -> `SERVER_PORT`
  - 加密密钥自动生成：安装时生成 `GATEWAY_ENCRYPTION_KEY`（`openssl rand -base64 32`）写入服务环境变量，**升级时保持不变**
  - 数据目录外部化：`DB_URL` 指向 `/var/lib/llm-gateway/`（Linux）/ `%ProgramData%\LLM-Gateway\data\`（Windows），与安装目录分离，升级不丢数据
- **新增 CI 打包 job**：`release.yml` 加 `package` job，matrix `[ubuntu-latest, windows-latest]`，产物挂 GitHub Release。
- **修复 Dockerfile**：构建路径改为单模块 `gateway-boot`。
- **修复 docker-compose.yml**：`context` 改根目录、移除源码挂载、补 `gateway-console` 服务。

## Capabilities

### New Capabilities

- `bare-metal-deploy`: 非 Docker 的系统安装包部署能力，覆盖 jpackage 打包（deb/rpm/exe）、systemd/Windows Service 注册、安装时端口交互、数据目录外部化、加密密钥自动生成与持久化、升级数据保留。

### Modified Capabilities

（无。本次不改变现有任何 spec 级别需求，仅新增部署形态与修复失配资产。）

## Impact

- **代码**：不涉及 `gateway-boot` 业务 Java 源码；不修改 `application*.yml` 配置内容。
- **新增文件**：`deployments/package/`（jpackage 配置、systemd unit、WinSW 配置、Inno Setup 脚本、构建脚本）、`release.yml` 新增 `package` job。
- **修改文件**：`deployments/docker/Dockerfile`、`deployments/docker/docker-compose.yml`。
- **CI 依赖**：windows-latest runner 需 Inno Setup（`choco install innosetup`）；JDK 21 自带 jpackage。
- **运行时行为**：安装包用 `local` profile 启动，沿用现有 `BuiltinUserLoader` + `SampleDataLoader` 自动创建 `admin/admin` 账号；用户首次登录后自行改密。H2 Console 默认开启（`web-allow-others: true`，接受现状）。
- **安全提示**：`admin/admin` 默认凭据与 H2 Console 远程访问需在文档中明确提示风险。

```

## openspec/changes/one-click-bare-deploy/design.md

- Source: openspec/changes/one-click-bare-deploy/design.md
- Lines: 1-92
- SHA256: dd8027687e021488240289cf7f363c8c9de7c79d72b56b9184a6bb7f2af87924

[TRUNCATED]

```md
## Context

- llm-gateway 已有 Docker 部署资产但失配：Dockerfile/docker-compose 仍引用旧多模块路径（`gateway/gateway-app` 等），实际已是 COLA Light 单模块 `gateway-boot`，`docker-compose up` 无法构建。
- 现有可复用基础：`spring-boot-maven-plugin` 已配 fat jar；`application.yml` 默认 H2 文件模式（`jdbc:h2:file:./data/gateway;MODE=PostgreSQL`）+ Flyway；`application-local.yml` 中 `demo-data-enabled: true`（admin/admin 自动创建）、`${GATEWAY_ENCRYPTION_KEY:}` 注入加密密钥、端口 8080。
- 现有 CI：`build.yml` 出 jar，`release.yml` 建 Docker 镜像 + GitHub Release（已用 matrix），`openspec/specs/` 无部署相关 capability。
- 决策约束：两平台 jpackage 安装包，默认 `local` profile，H2+Caffeine 零外部依赖，无 Redis，不改业务 Java 源码与 `application*.yml`。

## Goals / Non-Goals

**Goals:**
- `apt install` / `dnf install` / Windows exe 安装一条命令完成安装 + 服务注册 + 启动 + 健康就绪
- 零外部依赖（H2 文件 + Caffeine），装完即用
- 数据持久化与升级安全（数据目录与安装目录分离、加密密钥升级不变）
- 修复 Docker 资产失配

**Non-Goals:**
- macOS(.dmg)、K8s/Helm 修复、外部 PG/Redis 接入
- 改业务 Java 源码、改 `application*.yml` 内容
- 安装时交互设置管理员密码（沿用 admin/admin）
- native-image（GraalVM）

## Decisions

### D1: 打包工具选 jpackage（而非 native-image / 纯 jar+脚本）
- 备选：native-image（启动快但 Spring Boot 3 反射/资源配置成本高、构建慢）、纯 jar+脚本（需用户预装 JDK、无原生服务注册）。
- 选 jpackage：JDK 21 自带，产出原生 deb/rpm/exe，可 jlink 精简 JRE 自带，服务注册标准化。
- 代价：镜像体积 ~80MB（含精简 JRE）。

### D2: profile 用默认 local（不激活 standalone）
- `local` 已满足需求：H2 文件持久化、默认 cache 非 Redis、demo-data-enabled 创建 admin。
- 启动不带 `--spring.profiles.active`，走默认 `local`，不改配置文件。

### D3: 数据目录外部化用 DB_URL 环境变量
- `application.yml` 已支持 `${DB_URL:...}` 占位。
- systemd unit / Windows Service 设 `DB_URL` 指向 `/var/lib/llm-gateway/`（Linux）或 `%ProgramData%\LLM-Gateway\data\`（Windows）。
- 升级包不碰数据目录。

### D4: 端口交互设置走 debconf / Inno Setup 向导 -> SERVER_PORT
- Spring Boot 标准 `SERVER_PORT` 覆盖 `server.port`，无需改配置。
- Linux: debconf 模板收集端口；Windows: Inno Setup 安装向导 UI 输入框。
- **不校验端口占用**：安装时不探测冲突，运行时冲突由 systemd `Restart=on-failure` / WinSW `onfailure restart` 失败暴露。
- 非交互安装（CI 测试，`DEBIAN_FRONTEND=noninteractive` / `/VERYSILENT`）回退默认 8080。

### D5: 加密密钥安装时自动生成，升级不变
- `GATEWAY_ENCRYPTION_KEY` 经 `application-local.yml` 的 `${GATEWAY_ENCRYPTION_KEY:}` 注入 `gateway.security.encryption-key`，由 `Aes256EncryptionService` 读取。
- postinst（Linux）/ Inno Setup Pascal Script（Windows）安装时生成 32 字节 base64 密钥（Linux `openssl rand -base64 32`，Windows PowerShell 等价），写入服务环境变量文件 / WinSW xml。
- 升级时检测已存在则保留（Linux env 文件 conffile / Windows WinSW xml 检查不覆盖）。

### D6: 服务注册 Linux systemd / Windows WinSW
- Linux: `postinst` 注册 systemd unit（含 `EnvironmentFile=/etc/llm-gateway/env`，env 文件含 `DB_URL/SERVER_PORT/GATEWAY_ENCRYPTION_KEY`，`Restart=on-failure`）+ `enable --now`；`prerm` stop/disable；`postrm` 清理但保留数据。
- Windows: WinSW（`winsw.exe` + `LLMGateway.xml`）把 jpackage 启动器 exe 注册成 Windows Service，xml 配 `<env name="DB_URL/SERVER_PORT/GATEWAY_ENCRYPTION_KEY">` + `<arguments>`；Inno Setup 安装时 `winsw install`，卸载时 `winsw uninstall`。

### D9: Windows 放弃 msi/WiX，改 Inno Setup exe + WinSW（brainstorming 新增）
- 原设计用 jpackage `--type msi` + WiX `ServiceInstall`，但 jpackage `--type msi` 不支持 ServiceInstall（只装文件不注册服务），WiX 手写 msi 工作量大。
- 改用：jpackage `--type app-image` + WinSW 注册服务 + Inno Setup 打 exe installer。
- 代价：产出 exe 非 msi（已回写 delta spec）；Inno Setup exe 支持静默安装（`/VERYSILENT`）与升级覆盖安装。

### D7: CI 在 release.yml 加 package job，matrix ubuntu/windows
- ubuntu-latest 产出 deb + rpm（jpackage `--type deb` 与 `--type rpm`，rpm 需 `rpm` 工具）。
- windows-latest 产出 exe（jpackage app-image + WinSW + Inno Setup，需 Inno Setup）。
- 产物挂到现有 release job 创建的 GitHub Release。

### D8: jpackage + Spring Boot fat jar 启动
- Spring Boot 3.5 repackage 后 fat jar 的 Main-Class 为 `org.springframework.boot.loader.launch.JarLauncher`。
- jpackage 用 `--main-jar gateway-boot-<ver>.jar` + `--main-class org.springframework.boot.loader.launch.JarLauncher`。
- 具体在 design 阶段 Design Doc 做 spike 验证（见 Open Questions）。

## Risks / Trade-offs

- [jpackage + fat jar main-class 启动失败] -> design 阶段先 spike 验证 JarLauncher，必要时改用 layered jar 或 `jpackage --main-jar` 配合 `JAVA_OPTIONS`。
- [Inno Setup 未在 windows-latest 预装] -> job 内 `choco install innosetup` 兜底。
- [rpm 在 ubuntu-latest 交叉打包] -> ubuntu 装 `rpm` 包即可，jpackage `--type rpm` 不需 fedora runner。
- [H2 文件锁残留致重启失败] -> `DB_CLOSE_ON_EXIT=FALSE` 已配置；systemd `Restart=on-failure` 兜底。
- [admin/admin 默认凭据风险] -> 文档强提示首次改密（接受现状）。
- [H2 Console 远程访问] -> 接受现状，文档提示风险。
- [升级时 GATEWAY_ENCRYPTION_KEY 丢失] -> 环境变量文件标记为 conffile（deb）/ WinSW xml 检查不覆盖（Windows）。
- [Flyway 升级脚本向下不兼容] -> 降级回滚风险，标注于文档，建议升级前备份数据目录。

## Migration Plan


```

Full source: openspec/changes/one-click-bare-deploy/design.md

## openspec/changes/one-click-bare-deploy/tasks.md

- Source: openspec/changes/one-click-bare-deploy/tasks.md
- Lines: 1-45
- SHA256: c08087c5e7ccb40e8c33e515b727a1936969211885fe47ee698af4c5095d935c

```md
## 1. jpackage 打包基础验证

- [ ] 1.1 Spike：验证 jpackage + Spring Boot fat jar 启动（`--main-jar gateway-boot-<ver>.jar --main-class org.springframework.boot.loader.launch.JarLauncher`），确认应用正常启动
- [ ] 1.2 用 jdeps 分析 fat jar 依赖，确定 jlink 精简 JRE 模块清单
- [ ] 1.3 创建 `deployments/package/` 目录结构（`jpackage/`、`linux/`、`windows/`、构建脚本）
- [ ] 1.4 编写 `build.sh` / `build.ps1` 构建入口（mvn package -> jlink 生成精简 JRE -> jpackage 打包）

## 2. Linux 安装包（deb + rpm）

- [ ] 2.1 编写 systemd unit 模板（`Environment=DB_URL/SERVER_PORT/GATEWAY_ENCRYPTION_KEY`，`Restart=on-failure`）
- [ ] 2.2 编写 debconf 模板（端口交互，默认 8080，非交互回退默认）
- [ ] 2.3 编写 `postinst`（建 `/var/lib/llm-gateway` 与日志目录、生成 `GATEWAY_ENCRYPTION_KEY`、读 debconf 端口、注册 systemd、`enable --now`）
- [ ] 2.4 编写 `prerm`（stop/disable）与 `postrm`（清理安装文件、保留数据目录）
- [ ] 2.5 配置 jpackage `--type deb`（`--resource-dir` 挂 postinst/prerm/postrm/debconf/systemd unit）
- [ ] 2.6 配置 jpackage `--type rpm`（等价 maintainer 脚本，适配 dnf）
- [ ] 2.7 本地验证 deb：干净 Ubuntu 安装 -> 健康检查 UP -> 数据落 `/var/lib/llm-gateway/`
- [ ] 2.8 本地验证 rpm：RHEL 系安装 -> 健康检查 UP

## 3. Windows 安装包（exe）

- [ ] 3.1 编写 WinSW 配置（`LLMGateway.xml` + `winsw.exe`，注册 Windows Service，`<env>` 写 `DB_URL/SERVER_PORT/GATEWAY_ENCRYPTION_KEY`，`<arguments>` 指向 jpackage 启动器 exe）
- [ ] 3.2 编写 Inno Setup 安装向导 UI（端口输入框，默认 8080）
- [ ] 3.3 编写安装时密钥生成 Pascal Script（生成 `GATEWAY_ENCRYPTION_KEY`，已存在则保留）
- [ ] 3.4 配置服务环境变量写入 WinSW xml（`DB_URL` 指向 `%ProgramData%\LLM-Gateway\data\`、`SERVER_PORT`、`GATEWAY_ENCRYPTION_KEY`）
- [ ] 3.5 配置 jpackage `--type app-image`（生成精简 JRE + jar + 启动器 exe）+ Inno Setup 编译 exe（安装 Inno Setup）
- [ ] 3.6 本地验证 exe：干净 Windows 安装 -> Service 启动 -> 健康检查 UP -> 数据落 `%ProgramData%`

## 4. CI 集成

- [ ] 4.1 在 `release.yml` 加 `package` job，matrix `[ubuntu-latest, windows-latest]`
- [ ] 4.2 ubuntu job：构建 deb + rpm（安装 `rpm` 工具）
- [ ] 4.3 windows job：构建 exe（jpackage app-image + WinSW + Inno Setup 编译，安装 Inno Setup）
- [ ] 4.4 产物上传到 GitHub Release
- [ ] 4.5 验证 release tag 触发，deb/rpm/exe 产物齐全

## 5. Docker 资产修复

- [ ] 5.1 修复 `Dockerfile`：构建路径改为单模块 `gateway-boot`，修正 COPY 与 jar 名
- [ ] 5.2 修复 `docker-compose.yml`：`context` 改根目录、移除源码挂载、补 `gateway-console` 服务
- [ ] 5.3 验证 `docker-compose up -d` 正常构建并拉起 gateway

## 6. 文档

- [ ] 6.1 新增 `deployments/package/README.md`（构建步骤、安装命令、配置说明）
- [ ] 6.2 更新 `README.md` 部署章节：修正 DB 类型/jar 名/安装包用法，补充 admin/admin 首次改密提示与 H2 Console 风险提示

```

## openspec/changes/one-click-bare-deploy/specs/bare-metal-deploy/spec.md

- Source: openspec/changes/one-click-bare-deploy/specs/bare-metal-deploy/spec.md
- Lines: 1-89
- SHA256: c82dac5a53bf3304fa3b3993a0b32e2946293e0e4f58c1d48b98a56ce4bc0fe7

[TRUNCATED]

```md
## ADDED Requirements

### Requirement: Linux deb 安装包一键部署
系统 SHALL 提供 `.deb` 安装包，使运维人员在干净 Ubuntu 上执行 `apt install` 后自动完成服务注册、启动并达到健康就绪，无需预装 PostgreSQL/Redis。

#### Scenario: 全新安装并启动
- **WHEN** 运维人员在干净 Ubuntu 执行 `sudo apt install ./llm-gateway_<ver>.deb`
- **THEN** 安装包自动注册 systemd 服务、生成 `GATEWAY_ENCRYPTION_KEY`、启动服务，`/actuator/health` 在 60 秒内返回 UP

#### Scenario: 升级保留数据与密钥
- **WHEN** 运维人员对已安装实例执行 `apt upgrade` 到新版本
- **THEN** `/var/lib/llm-gateway/` 数据目录不被覆盖，`GATEWAY_ENCRYPTION_KEY` 保持不变，服务重启后历史加密数据可正常解密

#### Scenario: 卸载保留数据
- **WHEN** 运维人员执行 `apt remove`
- **THEN** 服务停止并注销，`/var/lib/llm-gateway/` 数据目录保留

### Requirement: Linux rpm 安装包一键部署
系统 SHALL 提供 `.rpm` 安装包覆盖 RHEL 系，行为与 deb 等价（服务注册、密钥生成、数据持久化）。

#### Scenario: 全新安装并启动
- **WHEN** 运维人员在 RHEL/CentOS 系执行 `sudo dnf install ./llm-gateway-<ver>.rpm`
- **THEN** 自动注册 systemd 服务、生成密钥、启动，`/actuator/health` 返回 UP

### Requirement: Windows exe 安装包一键部署
系统 SHALL 提供 `.exe` 安装包（Inno Setup + WinSW），使运维人员在干净 Windows 上安装后自动注册并启动 Windows Service，无需预装 PostgreSQL/Redis。

#### Scenario: 全新安装并启动
- **WHEN** 运维人员在 Windows 运行 exe 安装包完成安装向导
- **THEN** WinSW 注册并启动 Windows Service，`/actuator/health` 返回 UP，H2 数据文件落在 `%ProgramData%\LLM-Gateway\data\`

#### Scenario: 升级保留数据与密钥
- **WHEN** 运维人员运行新版本 exe 覆盖安装
- **THEN** 数据目录与 `GATEWAY_ENCRYPTION_KEY` 保留，服务重启后历史加密数据可正常解密

#### Scenario: 静默安装使用默认端口
- **WHEN** 运维人员以 `/VERYSILENT` 参数静默安装
- **THEN** Windows Service 以默认端口 8080 启动

### Requirement: 安装时端口交互设置
系统 SHALL 在安装时通过交互收集服务端口并写入 `SERVER_PORT` 环境变量，不修改 `application*.yml` 配置文件。

#### Scenario: Linux 交互设置端口
- **WHEN** 运维人员在可交互终端执行 `apt install`
- **THEN** debconf 提示输入端口，输入后服务以该端口启动

#### Scenario: 非交互安装使用默认端口
- **WHEN** 运维人员以 `DEBIAN_FRONTEND=noninteractive` 安装
- **THEN** 服务以默认端口 8080 启动

#### Scenario: 端口冲突运行时暴露
- **WHEN** 安装时设置的端口已被占用（安装不校验占用）
- **THEN** 服务启动失败，systemd `Restart=on-failure` / WinSW `onfailure restart` 反复重启，运维可从服务状态与日志发现冲突

#### Scenario: Windows 安装向导设置端口
- **WHEN** 运维人员在 Inno Setup 安装向导中输入端口
- **THEN** Windows Service 以该端口启动

### Requirement: 加密密钥自动生成与持久化
系统 SHALL 在首次安装时自动生成 `GATEWAY_ENCRYPTION_KEY` 并写入服务环境变量，升级时保持不变。

#### Scenario: 全新安装生成密钥
- **WHEN** 安装包首次安装
- **THEN** 自动生成 32 字节 base64 密钥写入服务环境变量，应用启动后加密功能可用

#### Scenario: 升级保留密钥
- **WHEN** 升级安装
- **THEN** 已存在的 `GATEWAY_ENCRYPTION_KEY` 不被覆盖

### Requirement: 数据目录外部化
系统 SHALL 通过 `DB_URL` 环境变量将 H2 数据文件指向与安装目录分离的标准数据目录，确保升级不丢数据。

#### Scenario: 数据落标准目录
- **WHEN** 安装并启动服务
- **THEN** H2 数据文件落在 `/var/lib/llm-gateway/`（Linux）或 `%ProgramData%\LLM-Gateway\data\`（Windows），而非安装目录

### Requirement: CI 多平台打包
CI SHALL 在 release tag 触发时，于 ubuntu-latest 和 windows-latest 各产出对应平台安装包并挂到 GitHub Release。

#### Scenario: release 产出多平台包

```

Full source: openspec/changes/one-click-bare-deploy/specs/bare-metal-deploy/spec.md
