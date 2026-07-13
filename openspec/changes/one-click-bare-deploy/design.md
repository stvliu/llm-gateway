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

- **全新安装**：无迁移，安装即用。
- **升级**：数据目录与密钥文件保留，service 自动重启，Flyway 自动迁移。
- **回滚**：降级包版本；若 Flyway 不兼容向下迁移，需手动恢复升级前数据目录备份。

## Open Questions

以下问题在 design 阶段 brainstorming 中已解决，详见 Design Doc（`docs/superpowers/specs/2026-07-11-one-click-bare-deploy-design.md`）的 Open Questions 章节：

- jpackage `--main-class` 需显式 `JarLauncher`（已确认，D8）。
- Windows 放弃 msi/WiX，改 Inno Setup exe + WinSW（因 jpackage msi 不支持 ServiceInstall，D9）。
- debconf 端口不校验占用，运行时冲突由 systemd 暴露（D4）。
- Windows Service 环境变量写入 WinSW xml（D6）。
