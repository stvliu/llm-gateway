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
