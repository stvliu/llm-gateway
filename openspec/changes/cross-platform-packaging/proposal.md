## Why

前置 `one-click-bare-deploy` 建立的 jpackage 打包链存在两个结构性问题：

1. **跨平台构建受限**：jpackage 打 deb/rpm 依赖系统原生 `dpkg-deb`/`rpmbuild`，必须在目标 OS 上构建，导致 Windows 开发机无法本地产出/验证 deb/rpm，100% 依赖 CI matrix（ubuntu + windows 分头跑）。
2. **配置注入分散且硬编码**：JVM 参数硬编码在 `jpackage --java-options`、业务配置（端口/DB/密钥）散落在 env 文件 + debconf 交互，调优需重打包、配置外部化不统一。

用纯 Java 的 JReleaser assemble 跨平台打 deb/rpm（deb 自实现 assembler + rpm Redline，不依赖系统工具）、archive 出 Windows zip，实现单机一次构建出全平台包；并引入单一 `llmgateway.conf` 统一配置外部化，顺带清掉 jpackage 硬编码与 env/debconf 分层冗余。

## What Changes

- **deb/rpm 打包换 JReleaser assemble**：从 jpackage 迁移到 `org.jreleaser:jreleaser-maven-plugin` 1.25.0（deb 自实现 assembler + rpm Redline，纯 Java 跨平台），Windows 单机即可同时产出 deb + rpm。**BREAKING**（deb/rpm 内部结构变化：启动器、配置文件布局调整）
- **引入单 llmgateway.conf 配置文件**：`/etc/llm-gateway/llmgateway.conf` 集中管理端口、数据库访问信息、加密因子、JVM 参数、路径，由启动脚本 `source` 注入，替代 env 文件 + jpackage `--java-options` 硬编码。
- **自定义启动脚本**：`/opt/llm-gateway/bin/llm-gateway.sh`（source conf + exec java），替代 jpackage 原生启动器。
- **postinst 改生成 conf**：首次安装生成 conf（含密钥），升级保留（conffile/NOREPLACE 保护）；密钥生成改 `/dev/urandom | base64`，去 openssl 依赖。
- **systemd unit 调整**：`ExecStart` 指向 `llm-gateway.sh`，去掉 `EnvironmentFile`。
- **BREAKING：去掉 debconf 端口交互**：Linux 端口改由 conf 文件配置（默认 8080），deb/rpm 体验统一（rpm 本无 debconf）。
- **CI 矩阵简化**：release.yml 从 `[ubuntu-latest, windows-latest]` 双 job 简化为单 windows job、单 JDK 21 出 deb + rpm + zip 全套（JReleaser 纯 Java，砍 rpm 工具与 iscc 依赖）。
- **Windows exe 改 zip**：jpackage + Inno Setup 安装器改为 JReleaser archive 出 zip + install.ps1 注册 WinSW service。**BREAKING**（无安装向导，端口改 conf 配置）。
- **清理冗余**：删除 `llm-gateway.templates`（debconf）、`postinst-rpm`/`prerm-rpm`/`postrm-rpm`（JReleaser 原生支持 rpm 脚本分离）、`build.gradle`+`gradle/wrapper/`（无 gradle）、Inno Setup `.iss`（Windows 改 zip）。

## Capabilities

### New Capabilities

（无。本次是对现有 `bare-metal-deploy` 能力的演进重构，不引入新能力。）

### Modified Capabilities

- `bare-metal-deploy`: 打包工具链（jpackage → JReleaser assemble + archive）、配置注入机制（env 文件 + debconf → 单 llmgateway.conf + 启动脚本 source）、CI 矩阵（双 OS → 单 windows job 单 JDK）、Windows 分发（exe → zip）四项 requirements 变化。

## Impact

- **代码**：不动 `gateway-boot` 业务 Java 源码、不动 `application*.yml` 内容。
- **新增文件**：`deployments/package/jreleaser.yml`（或 pom 插件段）、`deployments/package/conf/llmgateway.conf`（模板）、`deployments/package/bin/llm-gateway.sh`、`deployments/package/windows/install.ps1`/`uninstall.ps1`/`start.ps1`、`deployments/package/windows/llm-gateway.xml`（WinSW 配置）。
- **修改文件**：`deployments/package/linux/postinst`/`prerm`/`postrm`、`llm-gateway.service`、`build.sh`、`.github/workflows/release.yml`、`pom.xml`（加 JReleaser 插件）。
- **删除文件**：`llm-gateway.templates`、`llm-gateway.config`、`postinst-rpm`/`prerm-rpm`/`postrm-rpm`、`build.gradle`、`gradle/wrapper/`、Inno Setup `.iss`、`build.ps1` 的 jpackage+iscc 段。
- **依赖**：新增 `org.jreleaser:jreleaser-maven-plugin` 1.25.0；去 openssl 系统依赖、去 gradle、去 Inno Setup。
- **CI**：windows-latest runner 需 JDK 21（JReleaser 纯 Java，无需 gradle/rpm 工具/iscc）。
- **风险（build 阶段实测）**：JReleaser 在 Windows 出 deb/rpm/zip 未实测、JReleaser fileSet per-file 权限（postinst chmod 兜底）、jlink 交叉生成 Linux JRE、83MB fat jar + 50MB JRE 打包体积、conf 升级保留逻辑平移。
