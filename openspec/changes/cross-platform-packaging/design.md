## Context

前置 `one-click-bare-deploy` 建立了基于 jpackage 的 deb/rpm/exe 打包链：jlink 内置精简 JRE、systemd/WinSW 服务注册、env 文件注入业务参数（DB_URL/SERVER_PORT/GATEWAY_ENCRYPTION_KEY）、debconf 交互端口、jpackage `--java-options` 硬编码 JVM 参数。

两个结构问题：
1. jpackage 打 deb/rpm 依赖系统 `dpkg-deb`/`rpmbuild`，无法跨平台构建，Windows 开发机不能本地产出/验证 deb/rpm，100% 依赖 CI matrix。
2. 配置散落三处：env 文件（业务参数）、debconf（端口）、jpackage `--java-options`（JVM 硬编码），调优需重打包。

用纯 Java 的 JReleaser assemble 跨平台打 deb/rpm（deb 自实现 assembler + rpm Redline）、archive 出 zip，单 conf 文件 source 注入所有配置。

## Goals / Non-Goals

**Goals:**
- Windows 单机一次构建同时产出 deb + rpm + zip（JReleaser assemble 出 deb/rpm，archive 出 zip）
- 单 `/etc/llmgateway/llmgateway.conf` 统一管理端口/DB/加密因子/JVM/路径，启动脚本 source 注入
- JVM 参数运行时可调（改 conf 重启生效，不重打包）
- 去掉 debconf，deb/rpm 配置体验统一
- CI 简化为单 windows job

**Non-Goals:**
- 不动 Docker 部署
- 不补 macOS
- 不分发 gateway-cli / gateway-simulator
- 不做 H2 -> PostgreSQL 迁移引导
- 不动业务 Java 源码与 application*.yml 内容

## Decisions

**D1: deb/rpm 打包换 JReleaser assemble（纯 Java 跨平台）**
- 选择：`org.jreleaser:jreleaser-maven-plugin` 1.25.0，deb 用 JReleaser 自实现 assembler（作者 2023 年因 jdeb 的 Ant 依赖与 control 二次解析问题重写，不再套 jdeb）、rpm 用 Redline RPM，纯 Java 跨平台，不依赖系统 dpkg-deb/rpmbuild。
- 备选：nebula-ospackage（纯 Java 跨平台，但 8.6.3/9.1.1 均用 gradle 7.x deprecated API，gradle 8.0 移除该 API，且 gradle 7.x 不支持 Java 21，死结无解）、jpackage（现状，无法跨平台）、redlinerpm-maven-plugin（Central 上 airsquared 坐标 404、同名插件最高 0.0.7 半成品，不可用）、fpm（Ruby，Windows 支持差）。
- 理由：JReleaser 1.25.0 活跃维护（2026-06 仍更新），纯 Java 跨平台，Maven 原生插件与项目 mvnw 统一、消除 gradle/Java17 双工具链双 JDK；fileSets + packagers.scripts 能控制 deb/rpm 内部布局（塞 jlink JRE、启动脚本、conf、systemd unit）。

**D2: 单 llmgateway.conf 配置文件（source 注入）**
- 选择：`/etc/llmgateway/llmgateway.conf` 是 shell 脚本，启动脚本 `source` 后 export 为环境变量 + JAVA_OPTS，Spring `${ENV:默认}` 占位符绑定。
- 备选：env 文件 + jpackage --java-options 分层（现状，JVM 硬编码）、纯 yml 编辑（thingsboard 模式，不如 env 云原生）。
- 理由：单文件管所有，shell 表达力（注释/累加/默认值），运维心智最小；source 后仍是环境变量，保留 Spring relaxed binding。

**D3: 自定义启动脚本 llmgateway.sh**
- 选择：`/opt/llmgateway/bin/llmgateway.sh`（source conf + exec java $JAVA_OPTS -jar），systemd ExecStart 指向它。
- 备选：jpackage 原生启动器（不 source conf，JVM 硬编码）。
- 理由：conf 机制前提是启动脚本 source；jpackage 启动器做不到。顺带实现 JVM 参数运行时注入。

**D4: 去掉 debconf 端口交互**
- 选择：端口由 conf 的 SERVER_PORT 配置（默认 8080），deb/rpm 不交互。
- 备选：保留 debconf（hack nebula 注入 templates，维护成本高）。
- 理由：nebula 不原生支持 debconf；rpm 本无 debconf；去掉统一 deb/rpm 体验。Windows Inno Setup 向导端口交互保留（Windows 不动）。

**D5: 纯 Maven 构建（删除 Gradle）**
- 选择：JReleaser maven 插件绑定 `package` 阶段，`mvn package` 一条命令出 fat jar + jlink JRE + deb/rpm/zip，全 Java 21，无 Gradle。
- 备选：Gradle 独立 wrapper（nebula 方案，锁定 gradle 7.x + Java 17，双 JDK 双构建工具）。
- 理由：消除 gradle 7.x 死结与双 JDK；与项目 Maven 主线统一；JReleaser 插件配置在 pom 或 jreleaser.yml，build.sh 只调 `mvn package`。

**D6: CI 单 windows job 出 deb/rpm/zip**
- 选择：release.yml 单 windows-latest job，单 JDK 21，`mvn package` 触发 JReleaser assemble 出 deb/rpm + archive 出 zip，全套纯 Java 跨平台。
- 备选：双 job（ubuntu deb/rpm + windows exe，未兑现单机出全平台）；保留 exe（jpackage+iscc，仍需 choco install innosetup，且 exe 非跨平台痛点却保留 iscc 依赖）。
- 理由：兑现"本地单机出全平台"核心价值；砍掉 iscc 依赖与双 JDK；Windows 改 zip 后 CI 全纯 Maven。

**D7: 密钥 /dev/urandom 兜底**
- 选择：postinst 用 `head -c 32 /dev/urandom | base64` 生成密钥，去 openssl 依赖。
- 备选：deb requires openssl（增加系统依赖）。
- 理由：内置 JRE 已不依赖系统 Java，openssl 也应去依赖；与 Windows PowerShell RandomNumberGenerator 对齐。

**D8: jlink JRE 塞进 JReleaser deb/rpm，权限靠 postinst chmod 兜底**
- 选择：JReleaser fileSets 把 jlink JRE 目录整体打入 deb/rpm 的 `/opt/llmgateway/runtime/`；`bin/java` 可执行位不依赖打包工具的 per-file mode，由 postinst `chmod -R 0755 /opt/llmgateway/runtime/bin` 兜底保证。
- 验证点：JReleaser fileSet 是否支持 per-file mode（build 阶段实测；不支持则 postinst chmod 是真相源，符合 Linux 包惯例）。

**D9: Windows 分发改 zip（JReleaser archive + install.ps1 注册 WinSW service）**
- 选择：JReleaser `archive` assembler 出 Windows zip，内含 jlink Windows JRE + fat jar + WinSW.exe + llmgateway.xml + conf + install.ps1/uninstall.ps1/start.ps1；`install.ps1` 注册 WinSW service（端口由 conf 的 SERVER_PORT 配置，与 Linux 统一）。
- 备选：保留 exe（jpackage app-image + Inno Setup 安装向导 + WinSW，体验完整但需 iscc、非纯 Maven）；纯前台运行无 service（体验降级，无开机自启）。
- 理由：用户决策改 zip 以全纯 Maven、砍 iscc；保留 WinSW service 维持开机自启体验，注册方式从 Inno Setup 自动注册改 install.ps1 手动注册（BREAKING：Windows 无安装向导，端口改 conf 配置）。

## Risks / Trade-offs

- [JReleaser 在 Windows runner 打 deb/rpm 未实测] -> build 阶段在 Windows 跑 `mvn package` 验证 assemble 出 deb/rpm/zip；JReleaser deb 自实现 assembler + rpm Redline 均纯 Java，预计可行。
- [JReleaser fileSet per-file 权限支持未确认] -> 不依赖 per-file mode，postinst `chmod -R 0755 runtime/bin` 兜底 bin/java 可执行位（符合 Linux 包惯例，postinst 本就要改 conf 权限）；build 阶段实测确认。
- [Redline RPM 底层 2021 停更] -> JReleaser 1.25.0 活跃封装层兜底维护；接受底层停更风险，因 JReleaser 团队持续维护封装。
- [jlink JRE 平台问题：Windows runner jlink 出 Windows JRE，塞不进 Linux deb/rpm] -> 与打包工具无关；build 阶段验证 jlink 交叉生成 Linux JRE（`--module-path` 指向 Linux jmods）或回退下载 Adoptium Linux JRE。Windows zip 用 Windows JRE 无此问题。
- [83MB fat jar + 50MB JRE 打包体积] -> 实测打包时间与包体积；JReleaser 纯 Java 写归档，预计可接受。
- [conf 升级保留逻辑] -> deb conffile / rpm %config(noreplace)（JReleaser packager 配置）；postinst 首次生成密钥、升级不覆盖。
- [debconf 去掉是 BREAKING] -> 现有 debconf 用户需改 conf 配端口；文档说明迁移。
- [Windows 改 zip 是 BREAKING] -> 无安装向导，端口改 conf 配置；WinSW service 注册改 install.ps1 手动；卸载改 uninstall.ps1；文档说明迁移。

## Migration Plan

1. 新增 JReleaser 配置（pom 插件段或 jreleaser.yml）+ conf 模板 + Linux 启动脚本 + Windows install/uninstall/start.ps1
2. 改 postinst 生成 conf（env 逻辑迁移到 conf）+ chmod 兜底 JRE bin 权限
3. 改 systemd unit ExecStart 指向 llmgateway.sh
4. 改 build.sh：mvn package + jlink + 触发 JReleaser assemble（出 deb/rpm）+ archive（出 zip）
5. release.yml 单 windows job、单 JDK 21、砍 choco iscc、exe smoke 改 zip smoke
6. 删除 build.gradle + gradle wrapper + debconf templates + -rpm 后缀脚本 + Inno Setup .iss + build.ps1 的 jpackage 段
7. CI smoke test 回归（deb/rpm/zip 安装、升级保留、卸载保留）
8. 回滚：保留 jpackage/nebula 分支引用，若 JReleaser 验证失败可回退

## Open Questions

- JReleaser 在 Windows runner 打 deb/rpm/zip 的实际可用性（build 阶段实测）
- JReleaser fileSet 是否支持 per-file mode（build 阶段实测；不支持则 postinst chmod 兜底）
- jlink 交叉生成 Linux JRE 的可行性（build 阶段实测；§4.3 平台问题）
- conf 模板里 GATEWAY_ENCRYPTION_KEY 占位符的 postinst 替换机制（沿用 design 阶段 sed 方案）

## Build 阶段决策修订（2026-07-16，Task 8.1 实测）

> 以下修订基于 Task 8.1 在 Windows 开发机的实测结果，由用户确认（AskUserQuestion 选择"1.25.0 assemble.deb + jpackage rpm"）。

**D1 修订：rpm 从 Redline 改 JpackageAssembler**
- 原决策 D1 设想"rpm 用 Redline RPM 纯 Java 跨平台"。实测发现 JReleaser 1.25.0 **已移除 distribution 级 deb/rpm 打包器**（packagers.deb/rpm 不存在），deb 改用 `assemble.deb`（DebAssembler，纯 Java，本地 Windows 可验证，无需 dpkg-deb）；rpm 无纯 Java assembler，唯一路径是 `assemble.jpackage`（JpackageAssembler，底层调用 JDK jpackage，需系统 rpmbuild）。
- 选择：deb = `assemble.deb`（纯 Java，本地出包）；rpm = `assemble.jpackage`（`linux.types: [rpm]`，`runtimeImages` 复用 build.sh 交叉 jlink 的 Linux JRE，需 rpmbuild）。
- 影响：rpm 不再"纯 Java 跨平台"，本地 Windows 无 rpmbuild 不能出 rpm；jpackage `active: RELEASE` 使本地 SNAPSHOT 构建跳过 rpm，仅出 deb+zip，rpm 留 CI release 产出。

**D6 修订：本地出 deb+zip，rpm 留 CI**
- 原决策 D6"Windows 单机出 deb+rpm+zip"。修订为：Windows 单机出 deb + zip（纯 Java，8.1 实测通过）；rpm 需 rpmbuild，留 CI（release.yml 的 linux job 或 windows job 装 rpm-build）。
- 8.1 实测结果（2026-07-16）：`build.sh --skip-mvn` 在 Windows 产出 `dist/llmgateway-1.0.0-1_amd64.deb`（109MB，含 jar+JRE+维护脚本）+ `dist/llmgateway-win-1.0.0-SNAPSHOT.zip`（113MB，含 jar+JRE+WinSW+ps1）；jpackage（rpm）因 SNAPSHOT+active=RELEASE 跳过。

**JReleaser 配置实测要点（已记入 jreleaser.yml 注释）**
- basedir 实测为 repo root（非 `${project.basedir}`=gateway-boot），日志 `basedir set to E:\workspace\llm-gateway`。fileSet input 路径相对 repo root（`deployments/package/...`、`gateway-boot/target/...`）。
- `installationPath: /` 使 fileSet output 为完整系统路径（`opt/llmgateway/runtime`、`etc/llmgateway`、`lib/systemd/system`）；若设 `/opt/llmgateway` 会与前缀重复且把 etc/lib 困到 /opt 下。
- 维护脚本通过 `templateDirectory` 的 `control/{postinst,prerm,postrm}.tpl` 注入（assemble.deb 无 yaml 配置维护脚本的字段）。
- jar 由 build.sh 预复制为 `gateway-boot/target/llmgateway.jar`（固定名），fileSet `includes: [llmgateway.jar]` 打入 `opt/llmgateway/bin/`（匹配 llmgateway.sh 的 `-jar` 路径）；artifacts transform 对 deb assembler 未生效，改用 fileSet。
- `control.provides` 是 String（非数组）；`project.java` 已废弃，改 `project.languages.java`。

**systemd unit 路径对齐 FHS 现代（2026-07-16 探讨决策）**
- fileSet output 从 `lib/systemd/system` 改 `usr/lib/systemd/system`（FHS 现代路径；usrmerge 后 `/lib` 是 `/usr/lib` symlink，现代发行版 Debian 12+/Fedora/Ubuntu 22.04+ 推荐 `/usr/lib/systemd/system/`；jpackage 默认亦用此路径）。
- postinst 注释同步（`systemctl enable` 不依赖路径，systemd 自动发现 unit）；postrm 多路径清理保留（`/etc/systemd` + `/usr/lib/systemd` + `/lib/systemd`，兼容非 usrmerge 旧系统）。
- 应用根保持 `/opt/llmgateway/`（自带 bundled JRE，自包含产品语义符合 `/opt`；不迁 `/usr/share`）。

**Open Questions 解决状态**
- JReleaser Windows 打 deb/zip：✅ 实测可用（deb 109MB + zip 113MB）。rpm 需 rpmbuild，留 CI。
- fileSet per-file 权限：未依赖，postinst `chmod -R 0755 runtime/bin` 兜底（D8 不变）。
- jlink 交叉生成 Linux JRE：✅ 可行（.linux-jdk/jmods 交叉 jlink，62MB）。
- conf 占位符 postinst 替换：✅ 沿用 sed 方案（postinst.tpl 已注入 deb）。
