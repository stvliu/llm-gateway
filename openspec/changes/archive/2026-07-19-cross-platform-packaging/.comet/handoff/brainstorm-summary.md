# Brainstorm Summary

- Change: cross-platform-packaging
- Date: 2026-07-13

## 现状探索发现（已确认事实）

### 现有 jpackage 打包链（前置 one-click-bare-deploy 已建立）
- `build.sh`：mvn package -> jlink（19 模块/约 50MB JRE）-> jpackage `--type deb/rpm`
  - jpackage 在 Windows **不支持 deb/rpm**（仅 msi/exe）-> 核心痛点
  - rpm 分支用 `-rpm` 后缀脚本 + 临时 resource-dir
  - `--java-options` 硬编码 JVM 参数
- `postinst`：debconf 读端口 + `openssl rand` 生成密钥 + `cat > env <<EOF` 整文件生成
- `service`：`EnvironmentFile=/etc/llm-gateway/env` + `ExecStart=/opt/llm-gateway/bin/llm-gateway`（jpackage 启动器）
- `release.yml`：package job matrix `[ubuntu-latest, windows-latest]`，ubuntu 出 deb/rpm + 容器 smoke test，windows 出 exe

### 关键技术参数
- fat jar 约 83MB，jlink JRE 约 50MB（19 模块固化在 jlink-modules.txt）
- Main-Class: `org.springframework.boot.loader.launch.JarLauncher`

### Windows 环境限制
- 无 docker/iscc/dpkg-deb/rpm/gh，有 JDK 21 + JDK 17（D:\Java\jdk-17）+ Python 3.14
- 本机能验证：Gradle 产出 deb/rpm 文件结构、ar 归档、权限位、conffile
- 本机不能验证：装包跑 systemd、iscc 出 exe

## 实测验证结论（design 阶段 spike，2026-07-13）

### OQ1：nebula 在 Windows 打 deb/rpm 可用性 ✅ 验证通过
- **可用组合**：gradle 7.6.4 + Java 17 + nebula.ospackage 8.6.3（thingsboard 同款）
- Windows 上成功产出 deb（Debian binary package 2.0）+ rpm（RPM v3.0）

### 关键约束：nebula 8.6.3 不兼容 gradle 8.x（实测确认）
- nebula 8.6.3 / 9.1.1（最新）均用 gradle 7.x deprecated API（DefaultCopySpec 构造函数），gradle 8.0 移除
- gradle 8.5 + nebula 8.6.3/9.1.1 -> `Could not find matching constructor for DefaultCopySpec` 失败
- **gradle 7.x 不支持 Java 21**（gradle 7.6 最高 Java 19）
- **结论**：gradle 必须用 **Java 17** 跑（与 Maven/jlink/jpackage 的 Java 21 解耦）

### OQ2：jlink JRE bin/java 权限保留 ✅ 验证通过
- **默认权限丢失确认**：nebula from() 不指定 fileMode 时，deb 内 bin/java = **0o644**（不可执行）
- **正确缓解**：thingsboard 式 `fileMode 0755`（在 from 闭包内）-> bin/java = **0o755** ✅
- **无效方式**：`eachFile { details.mode = 0755 }` 不生效
- **正确模式**：JRE 按子目录分 from：bin/ fileMode 0755，lib/conf fileMode 0644

### OQ5：conf 升级保留（configurationFile）✅ 验证通过
- `configurationFile '/etc/llm-gateway/llmgateway.conf'` -> deb conffiles 含该路径 ✅

### OQ3：conf 密钥机制（用户确认）
- 占位符 sed 替换：conf 模板含 `GATEWAY_ENCRYPTION_KEY=__GENERATE_KEY__`，postinst 首次安装 sed 替换（用 `|` 分隔符避免 base64 的 `/` 冲突），升级靠 conffile 保留

## 确认的技术方案（用户已确认 2026-07-13）

### 整体架构
Maven（Java 21）-> fat jar -> jlink（Java 21）-> JRE；gradle 7.6.4 + nebula 8.6.3（**Java 17** 跑 gradle）打 deb/rpm；jpackage+Inno Setup（Java 21）打 exe。Gradle 与 Maven 解耦（D5）。

### build.gradle（新增 deployments/package/build.gradle）
gradle 7.6.4 wrapper + nebula 8.6.3，thingsboard 模式：
- `from(jreDir/bin) { fileMode 0755; into "/opt/llm-gateway/runtime/bin" }`
- `from(jreDir/lib) { fileMode 0644; into ".../runtime/lib" }`
- `from(fatJar) { fileMode 0644; into ".../bin" }`
- `from(conf模板) { fileType CONFIG|NOREPLACE; into "/etc/llm-gateway" }` + `configurationFile`
- `from(启动脚本) { fileMode 0755; into ".../bin" }`
- `from(systemd unit) { fileMode 0644; into "/lib/systemd/system" }`
- `preInstall/postInstall/preUninstall/postUninstall file(...)`（合并 -rpm hack）

### conf 机制
- 模板 `deployments/package/conf/llmgateway.conf`：SERVER_PORT/DB_URL/GATEWAY_ENCRYPTION_KEY=__GENERATE_KEY__/JAVA_OPTS/MANAGEMENT_HEALTH_REDIS_ENABLED
- 打入 deb + `configurationFile`（conffile 升级保留）
- postinst 首次：`grep __GENERATE_KEY__` 命中 -> `head -c 32 /dev/urandom | base64` -> `sed -i "s|__GENERATE_KEY__|${NEW_KEY}|"`
- 升级：conffile 保留，grep 不命中 -> 跳过

### 启动脚本（新增 deployments/package/bin/llm-gateway.sh，fileMode 0755）
`source /etc/llm-gateway/llmgateway.conf` + `exec /opt/llm-gateway/runtime/bin/java $JAVA_OPTS -jar /opt/llm-gateway/bin/llm-gateway.jar`

### systemd unit（改 llm-gateway.service）
ExecStart 指向 llm-gateway.sh，去掉 EnvironmentFile

### maintainer 脚本
postinst（生成 conf + 占位符 sed 替换密钥，D7 去 openssl）+ prerm/postrm 适配 + 合并 -rpm 后缀（nebula 原生 preInstall/postInstall/preUninstall/postUninstall）

### build.sh 改造
保留 mvn package + jlink（Java 21），jpackage deb/rpm 换成 `JAVA_HOME=$JAVA17_HOME ./gradlew buildDeb buildRpm`。build.ps1 不变。

### CI 改造（release.yml）
package job matrix `[ubuntu, windows]` -> 单 windows-latest；双 JDK（setup-java 17 设 JAVA17_HOME + setup-java 21）；单 windows job 串行 gradle 出 deb/rpm + jpackage+iscc 出 exe；smoke test 不变（deb systemd-ubuntu 容器、rpm systemd-rockylinux 容器、exe windows runner）

### 清理冗余
删 `llm-gateway.templates`（debconf）+ `postinst-rpm`/`prerm-rpm`/`postrm-rpm`

## 关键取舍与风险

### design 偏差（相对原 design.md，已确认）
1. **D1**：nebula 8.6.3 正确，但必须配 **gradle 7.6.4**（非 8.x，因 nebula 用 gradle 7.x deprecated API）
2. **D6**：单 windows job 正确，但需**双 JDK**（Java 17 跑 gradle + Java 21 跑 jlink/jpackage），原 design.md 未提及 Java 17
3. **gradle 与 Maven/JDK 解耦更彻底**：build.sh/build.ps1 用 Java 21，gradle 用 Java 17

### 风险
- [nebula 8.6.3 锁定 gradle 7.6.4] -> 后续若升级 gradle 8.x 需换打包方案（JReleaser）；本次接受锁定
- [CI 双 JDK 复杂度] -> setup-java 17 + 21，JAVA17_HOME 环境变量传递给 gradlew
- [deb 体积 83MB+50MB] -> 留 CI 实测打包时间与体积
- [conf 占位符 sed 替换] -> 用 `|` 分隔符避免 base64 的 `/` 冲突；升级靠 conffile 保留

## 测试策略

### 本机验证层（已实测 ✅）
- gradle 7.6.4 + Java 17 + nebula 8.6.3 产出 deb/rpm 文件
- Python 解析 deb ar 归档：bin/java 权限 0o755（fileMode 0755 生效）、lib/modules 0o644
- conffiles 含 /etc/llm-gateway/llmgateway.conf（configurationFile 生效）

### CI 验证层（留 CI）
- deb 在 systemd-ubuntu 容器装，/actuator/health 200，改 conf SERVER_PORT 重启生效
- rpm 在 systemd-rockylinux 容器装，/actuator/health 200
- exe 在 windows runner 装，WinSW service Running
- 升级验证：deb/rpm 升级后 conf 保留（端口/密钥不变），/var/lib/llm-gateway 数据不丢
- 卸载验证：apt/dnf remove 后 /var/lib/llm-gateway 数据目录保留

## Spec Patch

无。spec.md 现有验收场景（conf 注入业务参数、JVM 参数运行时可调、升级保留 conf、首次安装生成加密因子、Linux 端口由 conf 配置、非交互安装默认端口、端口冲突运行时暴露、Windows 安装向导设置端口、CI 多平台打包）完整覆盖本次变更。双 JDK/gradle 7.x 属实现细节（HOW），spec 是 WHAT，无需补充。
