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
- **WHEN** 推送 release tag
- **THEN** GitHub Release 附加 `.deb`、`.rpm`、`.exe` 产物

### Requirement: Docker 部署资产修复
系统 SHALL 修复 Dockerfile/docker-compose 路径失配，使 `docker-compose up -d` 能正常构建并拉起 gateway 服务。

#### Scenario: docker-compose 正常构建
- **WHEN** 运维人员执行 `cd deployments/docker && docker-compose up -d`
- **THEN** 镜像构建成功，gateway 服务正常启动，健康检查通过
