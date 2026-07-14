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
系统 SHALL 通过 `llmgateway.conf` 的 `SERVER_PORT`（Linux deb/rpm 与 Windows zip，默认 8080）配置服务端口，不修改 `application*.yml` 配置文件。Linux deb/rpm 不再使用 debconf 交互，Windows 不再使用 Inno Setup 安装向导交互。

#### Scenario: Linux 端口由 conf 配置
- **WHEN** 运维人员安装 deb/rpm
- **THEN** 服务端口由 `/etc/llm-gateway/llmgateway.conf` 的 `SERVER_PORT` 决定，默认 8080，改后 `systemctl restart` 生效

#### Scenario: 非交互安装使用默认端口
- **WHEN** 运维人员以非交互方式安装 deb/rpm 或解压 Windows zip
- **THEN** 服务以默认端口 8080 启动

#### Scenario: 端口冲突运行时暴露
- **WHEN** conf 配置的端口已被占用（安装不校验占用）
- **THEN** 服务启动失败，systemd `Restart=on-failure` / WinSW `onfailure restart` 反复重启，运维可从服务状态与日志发现冲突

#### Scenario: Windows zip 端口由 conf 配置
- **WHEN** 运维人员解压 Windows zip 并运行 `install.ps1`
- **THEN** Windows Service 端口由 `conf\llmgateway.conf` 的 `SERVER_PORT` 决定，默认 8080，改后重启 service 生效

### Requirement: CI 多平台打包
CI SHALL 在 release tag 触发时，于单个 windows-latest runner（单 JDK 21）产出 `.deb`、`.rpm`、`.zip` 全部平台包并挂到 GitHub Release（deb/rpm 由 JReleaser assemble 跨平台产出，zip 由 JReleaser archive 产出，均纯 Java 无需系统 dpkg-deb/rpmbuild/iscc）。

#### Scenario: release 产出多平台包
- **WHEN** 推送 release tag
- **THEN** 单 windows job 产出 `.deb`、`.rpm`、`.zip` 并附加到 GitHub Release
