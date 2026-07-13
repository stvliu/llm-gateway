---
comet_change: one-click-bare-deploy
role: technical-design
canonical_spec: openspec
status: final
---

# 非 Docker 一键部署 - 技术设计文档

## Context

llm-gateway 当前部署资产存在两处阻碍快速落地的问题：

1. **Docker 资产失配**：`deployments/docker` 下 Dockerfile/docker-compose 仍引用旧多模块路径（`gateway/gateway-app`、`gateway-web`、`gateway-core`、`gateway-adapter`），与 COLA Light 重构后的单模块 `gateway-boot` 严重失配，`docker-compose up` 根本无法构建。
2. **缺少非 Docker 路径**：仅有 Docker 一种部署形态，无法覆盖禁 Docker 的政企内网、嫌 Docker 重的个人开发者等场景，缺少"一条命令装好即用"的系统安装包。

现有可复用基础：

- `spring-boot-maven-plugin` 已配 fat jar repackage（产物 `gateway-boot-1.0.0-SNAPSHOT.jar`，Main-Class = `org.springframework.boot.loader.launch.JarLauncher`）。
- `application.yml` 默认 H2 文件模式（`jdbc:h2:file:./data/gateway;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_ON_EXIT=FALSE`）+ Flyway，且支持 `${DB_URL:...}` 环境变量占位。
- `application-local.yml`：`demo-data-enabled: true`（admin/admin 自动创建）、`encryption-key: ${GATEWAY_ENCRYPTION_KEY:}` 注入加密密钥、端口 8080、H2 Console `enabled: true` + `web-allow-others: true`。
- `BuiltinUserLoader`（始终启用，加载 `data/builtin/users.json` 的 admin/admin）+ `SampleDataLoader`（受 `demo-data-enabled` 控制，local 下 true）。
- `Aes256EncryptionService` 通过 `@Value("${gateway.security.encryption-key:#{null}}")` 读取密钥。
- 现有 CI：`build.yml` 出 jar，`release.yml` 建 Docker 镜像 + GitHub Release（已用 matrix）。

决策约束：两平台 jpackage 安装包，默认 `local` profile，H2+Caffeine 零外部依赖，无 Redis，不改业务 Java 源码与 `application*.yml` 配置内容。

## Goals / Non-Goals

**Goals:**

- `apt install` / `dnf install` / Windows exe 安装一条命令完成安装 + 服务注册 + 启动 + 健康就绪。
- 零外部依赖（H2 文件 + Caffeine），装完即用。
- 数据持久化与升级安全（数据目录与安装目录分离、加密密钥升级不变）。
- 修复 Docker 资产失配，`docker-compose up -d` 能正常构建拉起。

**Non-Goals:**

- macOS(.dmg)、K8s/Helm、外部 PG/Redis 接入。
- 改业务 Java 源码、改 `application*.yml` 配置内容。
- 安装时交互设置管理员密码（沿用 admin/admin）。
- native-image（GraalVM）。

## Decisions

### D1: 打包工具选 jpackage（而非 native-image / 纯 jar+脚本）

- 备选：native-image（启动快但 Spring Boot 3 反射/资源配置成本高、构建慢）、纯 jar+脚本（需用户预装 JDK、无原生服务注册）。
- 选 jpackage：JDK 21 自带，产出原生 deb/rpm/exe，可 jlink 精简 JRE 自带，服务注册标准化。
- 代价：镜像体积 ~80MB（含精简 JRE）。

### D2: profile 用默认 local（不激活 standalone）

- `local` 已满足需求：H2 文件持久化、默认 cache 非 Redis、`demo-data-enabled` 创建 admin。
- 启动不带 `--spring.profiles.active`，走默认 `local`，不改配置文件。

### D3: 数据目录外部化用 DB_URL 环境变量

- `application.yml` 已支持 `${DB_URL:...}` 占位。
- systemd unit / Windows Service 设 `DB_URL` 指向 `/var/lib/llm-gateway/gateway`（Linux）或 `%ProgramData%\LLM-Gateway\data\gateway`（Windows）。
- 完整 URL：`jdbc:h2:file:/var/lib/llm-gateway/gateway;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_ON_EXIT=FALSE`。
- 升级包不碰数据目录。

### D4: 端口交互设置走 debconf / Inno Setup 向导 -> SERVER_PORT

- Spring Boot 标准 `SERVER_PORT` 覆盖 `server.port`，无需改配置。
- Linux：debconf 模板收集端口，默认 8080。
- Windows：Inno Setup 安装向导 UI 输入框，默认 8080。
- **不校验端口占用**：安装时不探测端口冲突，简化安装逻辑；运行时冲突由 systemd `Restart=on-failure` 失败暴露（服务反复重启，运维可见）。
- 非交互安装（CI 测试，`DEBIAN_FRONTEND=noninteractive` / Inno Setup `/VERYSILENT`）回退默认 8080。

### D5: 加密密钥安装时自动生成，升级不变

- `GATEWAY_ENCRYPTION_KEY` 经 `application-local.yml` 的 `${GATEWAY_ENCRYPTION_KEY:}` 注入 `gateway.security.encryption-key`，由 `Aes256EncryptionService` 读取。
- 安装时生成 32 字节 base64 密钥（Linux `openssl rand -base64 32`，Windows PowerShell 等价命令），写入服务环境变量文件。
- 升级时检测已存在则保留：Linux env 文件标记为 deb conffile，Windows Inno Setup 安装前检查已存在则不覆盖。
- 风险：密钥丢失则历史加密数据无法解密，文档强提示备份。

### D6: 服务注册 Linux systemd / Windows WinSW

- **Linux**：`postinst` 注册 systemd unit（含 `EnvironmentFile=/etc/llm-gateway/env`，env 文件含 `DB_URL/SERVER_PORT/GATEWAY_ENCRYPTION_KEY`，`Restart=on-failure`，`WorkingDirectory`，`User`）+ `enable --now`；`prerm` stop/disable；`postrm` 清理安装文件但保留数据目录。
- **Windows**：WinSW（`winsw.exe` + `LLMGateway.xml`）把 jpackage 启动器 exe 注册成 Windows Service。xml 配 `<env name="DB_URL/SERVER_PORT/GATEWAY_ENCRYPTION_KEY">` + `<arguments>` 指向启动器 exe。Inno Setup 安装时复制 app-image + WinSW 到 Program Files、运行 `winsw install`、建数据目录；卸载时 `winsw uninstall`。

### D7: CI 在 release.yml 加 package job，matrix ubuntu/windows

- ubuntu-latest 产出 deb + rpm（jpackage `--type deb` 与 `--type rpm`，rpm 需 `rpm` 工具，ubuntu 装 `rpm` 包即可交叉打包，不需 fedora runner）。
- windows-latest 产出 exe（jpackage `--type app-image` + WinSW 配置 + Inno Setup 编译，需 Inno Setup）。
- 产物挂到现有 release job 创建的 GitHub Release。

### D8: jpackage + Spring Boot fat jar 启动

- Spring Boot 3.5 repackage 后 fat jar 的 Main-Class 为 `org.springframework.boot.loader.launch.JarLauncher`。
- jpackage 用 `--main-jar gateway-boot-1.0.0-SNAPSHOT.jar` + 显式 `--main-class org.springframework.boot.loader.launch.JarLauncher`。
- build 阶段先做 spike 验证（最大技术风险），备选：layered jar 或 `jpackage --main-jar` 配合 `JAVA_OPTIONS`。

### D9: Windows 放弃 msi/WiX，改 Inno Setup exe + WinSW（brainstorming 新增）

- 原设计（open 阶段 design.md D6/D7）用 jpackage `--type msi` + WiX `ServiceInstall`。brainstorming 发现 **jpackage `--type msi` 不支持 ServiceInstall**（jpackage 生成的 msi 只装文件，不注册服务），WiX 手写 msi 工作量大。
- 改用：jpackage `--type app-image` 生成应用目录（精简 JRE + jar + 启动器 exe）+ WinSW 注册服务 + Inno Setup 打 exe installer。
- 代价：产出 exe 非 msi（需 Spec Patch：delta spec 的 msi Requirement 改 exe）；Inno Setup exe 同样支持静默安装（`/VERYSILENT`）与升级覆盖安装。

### D10: 修复 ProviderRegistryHealthIndicator（build 阶段 spike 发现，用户确认补充）

- **问题**：Task 1.1 spike 发现 `/actuator/health` 全新安装时 503 DOWN。根因是 `ProviderRegistryHealthIndicator` 逻辑"全部 DOWN/UNKNOWN -> DOWN"，全新安装时 provider 状态 UNKNOWN（无流量初始态）导致整体 DOWN。这是预存在的设计缺陷：无流量≠不健康。
- **修复**：调整 `ProviderRegistryHealthIndicator` 逻辑为"只有 provider 明确 DOWN 才整体 DOWN；UNKNOWN（初始态/无流量）和 UP 都视为健康（整体 UP）"。改动仅 1 个文件（actuator 基础设施，非业务逻辑）。
- **Redis**：local profile 未排除 Redis 自动配置（`spring.data.redis.enabled: false` 是无效自定义属性），redis health 指标存在。spike 验证用 `-Dmanagement.health.redis.enabled=false` 禁用；安装包阶段将 `MANAGEMENT_HEALTH_REDIS_ENABLED=false` 放入服务环境变量（Linux env 文件 `/etc/llm-gateway/env` + Windows WinSW xml `<env>`）。
- **用户决策**：build 阶段 Step 4 中等变更，用户选择"修复 health indicator"（而非改 Spec 验收标准或拆分新 change）。
- **影响**：delta spec 的 health UP 场景依赖此修复；Task 2.3 postinst / Task 3.2 Inno Setup 需加 `MANAGEMENT_HEALTH_REDIS_ENABLED=false` 环境变量。

## 打包流程

```
mvn package -pl gateway-boot -DskipTests
  -> fat jar: gateway-boot/target/gateway-boot-1.0.0-SNAPSHOT.jar
jdeps --list-deps gateway-boot-1.0.0-SNAPSHOT.jar
  -> 依赖模块清单（java.base, java.logging, java.sql, java.naming, ...）
jlink --add-modules <deps> --output deployments/package/jre
  -> 精简 JRE
jpackage --main-jar gateway-boot-1.0.0-SNAPSHOT.jar \
         --main-class org.springframework.boot.loader.launch.JarLauncher \
         --runtime-image deployments/package/jre \
         --type deb|rpm|app-image \
         --resource-dir deployments/package/<platform> \
         ...
  -> deb / rpm / app-image
```

- `build.sh`（Linux）/ `build.ps1`（Windows）封装上述流程。
- Windows 额外步骤：jpackage app-image 后，配 WinSW xml，Inno Setup 编译 exe（`iscc`）。

## Linux 安装包设计（deb + rpm）

**目录布局：**

- 安装目录：`/opt/llm-gateway/`（jpackage app-image：精简 JRE + jar + 启动器）
- 数据目录：`/var/lib/llm-gateway/`（H2 数据文件，`DB_URL` 指向此）
- 日志目录：`/var/log/llm-gateway/`
- 配置目录：`/etc/llm-gateway/env`（环境变量文件，conffile）

**systemd unit（`llm-gateway.service`）：**

```ini
[Unit]
Description=LLM-Gateway Service
After=network.target

[Service]
Type=simple
User=llm-gateway
WorkingDirectory=/var/lib/llm-gateway
EnvironmentFile=/etc/llm-gateway/env
ExecStart=/opt/llm-gateway/bin/llm-gateway
Restart=on-failure
RestartSec=5

[Install]
WantedBy=multi-user.target
```

**`/etc/llm-gateway/env`（conffile，升级保留）：**

```env
DB_URL=jdbc:h2:file:/var/lib/llm-gateway/gateway;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_ON_EXIT=FALSE
SERVER_PORT=8080
GATEWAY_ENCRYPTION_KEY=<首次安装生成，升级保留>
```

**maintainer 脚本：**

- `postinst`：建 `llm-gateway` 用户、建 `/var/lib/llm-gateway` 与 `/var/log/llm-gateway`、生成 `GATEWAY_ENCRYPTION_KEY`（已存在则保留）、读 debconf 端口写 env、注册 systemd、`enable --now`。
- `prerm`：`systemctl stop` + `disable`。
- `postrm`：清理安装文件，**保留** `/var/lib/llm-gateway` 与 `/etc/llm-gateway/env`。

**debconf 模板：** 端口交互，默认 8080，非交互回退默认，不校验占用。

**deb vs rpm：** maintainer 脚本等价，rpm 适配 dnf（`%post`/`%preun`/`%postun`）。jpackage `--type deb` 与 `--type rpm` 通过不同 `--resource-dir` 挂对应脚本。

## Windows 安装包设计（exe）

**目录布局：**

- 安装目录：`%ProgramFiles%\LLM-Gateway\`（jpackage app-image + WinSW exe + xml）
- 数据目录：`%ProgramData%\LLM-Gateway\data\`（H2 数据文件，`DB_URL` 指向此）
- 配置：WinSW xml 内嵌环境变量（或同目录 env 文件）

**WinSW（`LLMGateway.exe` + `LLMGateway.xml`）：**

```xml
<service>
  <id>LLMGateway</id>
  <name>LLM-Gateway</name>
  <executable>%ProgramFiles%\LLM-Gateway\runtime\bin\llm-gateway.exe</executable>
  <arguments></arguments>
  <env name="DB_URL" value="jdbc:h2:file:%ProgramData%\LLM-Gateway\data\gateway;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_ON_EXIT=FALSE"/>
  <env name="SERVER_PORT" value="8080"/>
  <env name="GATEWAY_ENCRYPTION_KEY" value="<首次安装生成，升级保留>"/>
  <onfailure action="restart" delay="5 sec"/>
</service>
```

**Inno Setup 脚本（`.iss`）：**

- 安装：复制 app-image + WinSW 到 Program Files、生成 `GATEWAY_ENCRYPTION_KEY`（已存在保留）、向导收集端口写入 xml、`winsw install`、建 `%ProgramData%\LLM-Gateway\data\`。
- 升级：覆盖安装 app-image，保留 xml 中的密钥与端口（检查已存在不覆盖），`winsw stop` -> 覆盖 -> `winsw start`。
- 卸载：`winsw uninstall`，保留数据目录（提示用户）。
- 静默安装：`/VERYSILENT`（CI 测试用），端口回退默认 8080。

## CI 设计

`release.yml` 新增 `package` job，matrix `[ubuntu-latest, windows-latest]`：

| 平台 | 步骤 | 产物 |
|------|------|------|
| ubuntu-latest | 装 `rpm` 工具 -> `build.sh` -> jpackage `--type deb` + `--type rpm` | `.deb`、`.rpm` |
| windows-latest | 装 Inno Setup -> `build.ps1` -> jpackage app-image -> 配 WinSW -> `iscc` 编译 | `.exe` |

产物上传到现有 release job 创建的 GitHub Release。

**CI smoke test（在 package job 内）：**

- ubuntu：打 deb 后 `docker run ubuntu apt install ./deb`，curl `/actuator/health` 验 UP；打 rpm 后 `docker run rockylinux dnf install`，验 UP。
- windows：打 exe 后 windows runner 静默安装（`/VERYSILENT`），验 service 启动 + health UP。

## Docker 资产修复

- **Dockerfile**：构建路径改为单模块 `gateway-boot`，修正 COPY 路径与 jar 名（`gateway-boot-1.0.0-SNAPSHOT.jar`）。
- **docker-compose.yml**：`context` 改根目录、移除源码挂载、补 `gateway-console` 服务。
- 验证：`cd deployments/docker && docker-compose up -d` 正常构建并拉起 gateway。

## 测试策略

### CI smoke test（package job 内）

- ubuntu：deb 装干净 Ubuntu（`docker run`）-> health UP -> 数据落 `/var/lib/llm-gateway/`；rpm 装干净 Rocky Linux -> health UP。
- windows：exe 静默装 windows runner -> service 启动 -> health UP -> 数据落 `%ProgramData%`。

### 本地手工验证

- 干净 Ubuntu/RHEL VM：安装/升级/卸载全流程。
- 干净 Windows VM：安装/升级/卸载全流程。

### 升级测试

- 装旧版 -> 装新版：数据目录保留、`GATEWAY_ENCRYPTION_KEY` 不变、加密数据可解密（创建 API Key -> 升级 -> 验证可解密）。

### 边界场景

- 升级保留密钥：env 文件 conffile（deb）/ xml 检查不覆盖（Windows）。
- 端口冲突运行时暴露：systemd `Restart=on-failure` 反复重启，WinSW `onfailure restart`。
- 非交互安装回退默认端口 8080。
- 卸载保留数据目录。

## Risks / Trade-offs

| 风险 | 缓解 |
|------|------|
| jpackage + fat jar main-class 启动失败 | build 阶段先 spike 验证 JarLauncher，必要时改 layered jar 或 `JAVA_OPTIONS` |
| `GATEWAY_ENCRYPTION_KEY` 丢失 | env 文件 conffile / xml 不覆盖 + 文档提示备份 |
| H2 文件锁残留致重启失败 | `DB_CLOSE_ON_EXIT=FALSE` 已配 + systemd `Restart=on-failure` / WinSW `onfailure restart` 兜底 |
| Flyway 升级脚本向下不兼容 | 降级回滚风险，文档提示升级前备份数据目录 |
| admin/admin 默认凭据 | 文档强提示首次改密（接受现状） |
| H2 Console 远程访问（`web-allow-others: true`） | 接受现状，文档提示风险 |
| rpm 在 ubuntu-latest 交叉打包 | ubuntu 装 `rpm` 包即可，jpackage `--type rpm` 不需 fedora runner |
| Inno Setup 未在 windows-latest 预装 | job 内 `choco install innosetup` 兜底 |

## Migration Plan

- **全新安装**：无迁移，安装即用。
- **升级**：数据目录与密钥文件保留，service 自动重启，Flyway 自动迁移。
- **回滚**：降级包版本；若 Flyway 不兼容向下迁移，需手动恢复升级前数据目录备份。

## Open Questions（已解决）

1. **jpackage 是否需显式 `--main-class`**：是，fat jar Main-Class 为 `JarLauncher`，显式指定（D8）。
2. **rpm 是否需 fedora runner**：否，ubuntu-latest 装 `rpm` 工具即可交叉打包（D7）。
3. **debconf 是否校验端口占用**：否，运行时冲突由 systemd 暴露（D4）。
4. **Windows 用 msi 还是 exe**：exe（Inno Setup + WinSW），因 jpackage msi 不支持 ServiceInstall（D9）。
5. **测试策略是否充分**：CI smoke test 在 package job 内（docker 验 deb/rpm + windows runner 验 exe）+ 本地 VM 全流程 + 升级测试。
