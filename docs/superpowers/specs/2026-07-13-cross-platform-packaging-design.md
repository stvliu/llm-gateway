---
comet_change: cross-platform-packaging
role: technical-design
canonical_spec: openspec
---

# 跨平台打包重构技术设计

> 本文档是对 OpenSpec `cross-platform-packaging` change 的 `design.md`（D1-D9 决策）的深度技术细化。OpenSpec delta spec（`specs/bare-metal-deploy/spec.md`）是能力规格的事实源，本文档不重复 requirements，只给出实现设计、技术风险、测试策略与边界条件。

## 1. 背景与目标

前置 `one-click-bare-deploy` 建立了基于 jpackage 的 deb/rpm/exe 打包链，存在两个结构性问题：

1. **跨平台构建受限**：jpackage 打 deb/rpm 依赖系统 `dpkg-deb`/`rpmbuild`，且 jpackage 在 Windows 上仅支持 `msi`/`exe`（不支持 `deb`/`rpm`），导致 Windows 开发机无法本地产出/验证 deb/rpm，100% 依赖 CI ubuntu runner。
2. **配置注入分散**：业务配置散落在 env 文件（DB_URL/SERVER_PORT/GATEWAY_ENCRYPTION_KEY）+ debconf（端口交互）+ jpackage `--java-options`（JVM 硬编码），调优需重打包。

本设计用纯 Java 的 `JReleaser` assemble 跨平台打 deb/rpm（deb 自实现 assembler + rpm Redline）、archive 出 Windows zip，引入单一 `llmgateway.conf` 统一配置外部化。**不引入新能力**，是对现有 `bare-metal-deploy` 能力的演进重构（含 BREAKING：deb/rpm 内部结构变化、去掉 debconf、Windows exe 改 zip）。

> **方案变更说明**：design 阶段初版选 nebula-ospackage + gradle 7.6.4 + Java 17，实测后发现 nebula 8.6.3/9.1.1 均锁定 gradle 7.x deprecated API（gradle 8.0 移除，且 gradle 7.x 不支持 Java 21），死结无解。build 阶段前改用 JReleaser 1.25.0（活跃、纯 Java、Maven 原生），消除双 JDK/双构建工具。详见 §4.1。

## 2. 验证结论与待实测项

design 阶段已实测 nebula 方案的可行性（结论归档），本 change 已切换为 JReleaser 方案，下列结论需在 build 阶段以 JReleaser 重新实测确认。

### 2.1 JReleaser 在 Windows 跨平台打 deb/rpm/zip ⏳ 待 build 实测

- **预期组合**：JReleaser maven 插件 1.25.0 + Java 21，`mvn package` 触发 assemble 出 deb/rpm + archive 出 zip
- **底层**：deb 用 JReleaser 自实现 assembler（作者 2023 年因 jdeb 的 Ant 依赖 + control 二次解析问题重写，不再套 jdeb）；rpm 用 Redline RPM（纯 Java）
- **验证方式**：Windows 开发机/CI 跑 `mvn package`，确认产出 deb + rpm + zip 三件套
- **design 阶段已确认（Maven Central）**：JReleaser 1.25.0（2026-06 活跃）；jdeb 1.14（2025-06 活跃，参考）；redline 1.2.10（2021 停更，由 JReleaser 封装层兜底）

### 2.2 关键约束：JReleaser 消除 gradle/Java17 双工具链 ✅

切换 JReleaser 后，design 阶段发现的 nebula 致命约束不再存在：

- 原 nebula 8.6.3/9.1.1 均使用 gradle 7.x 的 deprecated 内部 API（`DefaultCopySpec` 构造函数），gradle 8.0 移除该 API；gradle 7.x 不支持 Java 21（最高 Java 19）。死结无解。
- JReleaser 是 Maven 插件，JDK 11+ 即可运行（项目用 Java 21），无 gradle 依赖，无双 JDK。
- **结论**：全 Java 21 + 纯 Maven，彻底消除 gradle 7.x 死结与双 JDK 复杂度。

### 2.3 jlink JRE bin/java 权限：postinst chmod 兜底 ⏳ 待 build 实测

design 阶段实测 nebula 时发现：JRE `bin/java` 必须打包时 0755，否则不可执行；`eachFile` 无效，必须 `fileMode 0755`（thingsboard 式）。

切换 JReleaser 后，**不依赖打包工具的 per-file mode**，改由 postinst 兜底：

```sh
# postinst 里（本就要改 conf 权限，顺手 chmod JRE bin）
chmod -R 0755 /opt/llmgateway/runtime/bin
chmod 0640 /etc/llmgateway/llmgateway.conf
```

- **理由**：Linux 包惯例允许 postinst 修权限；postinst chmod 是权限真相源，比依赖打包工具权限位更稳健（rpm 某些打包工具会重置权限位）
- **代价**：deb/rpm 内文件权限位显示默认（0644），实际运行时靠 postinst 矫正
- **验证点**：JReleaser fileSet 是否支持 per-file mode（build 实测；支持则可省 postinst chmod，不支持则 postinst 兜底）

### 2.4 conf 升级保留 ⏳ 待 build 实测

- JReleaser deb packager 配置 conffile（`/etc/llmgateway/llmgateway.conf`），rpm 侧 `%config(noreplace)`
- dpkg/dnf 升级时保留 conf（NOREPLACE 语义），postinst 首次生成、升级不覆盖
- **验证点**：JReleaser packager 是否正确标记 conffile/noreplace（build 实测）

## 3. 详细设计

### 3.1 整体架构

```
Maven (Java 21) ──> fat jar (83MB, gateway-boot-<ver>.jar)
                       │
jlink (Java 21) ──> 精简 JRE (50MB, 19 模块, jlink-modules.txt)
                       │  ├── Linux JRE（deb/rpm 用，需交叉生成或下载，见 4.3）
                       │  └── Windows JRE（zip 用，本机 jlink 即可）
                       ▼
┌──────────────────────────────────────────────┐
│ JReleaser maven 插件 1.25.0（Java 21）        │
│ (deployments/package/jreleaser.yml 或 pom)   │
│   assemble:                                   │
│     deb packager（自实现 assembler）          │
│       from fat jar  -> /opt/llmgateway/bin   │
│       from JRE      -> /opt/llmgateway/runtime│
│       from conf     -> /etc/llmgateway (conffile)│
│       from 启动脚本 -> /opt/llmgateway/bin   │
│       from unit     -> /lib/systemd/system    │
│     rpm packager（Redline，同布局）           │
│     archive assembler                          │
│       -> Windows zip（JRE + jar + WinSW + ps1）│
└──────────────────────────────────────────────┘
```

**JDK 统一**：build.sh 与 JReleaser 均用 `JAVA_HOME`（Java 21），无双 JDK。jlink 产 Linux JRE 需交叉生成（见 4.3）。

### 3.2 JReleaser 配置（新增 `deployments/package/jreleaser.yml` 或 pom 插件段）

JReleaser 1.25.0，用 `SINGLE_JAR` distribution 类型（专给单 jar + fileSets 场景），配 deb/rpm packager + archive assembler：

```yaml
project:
  name: llmgateway
  version: ${project.version}
  description: LLM-Gateway
  authors: [LLM-Gateway]
  license: Apache-2.0

distributions:
  llmgateway:
    type: SINGLE_JAR
    artifacts:
      - path: ../../gateway-boot/target/gateway-boot-${project.version}.jar
        transform: llmgateway.jar
    # jlink JRE 作为 fileSet 塞入（Linux JRE，见 4.3 交叉生成）
    fileSets:
      - input: jre/bin
        output: opt/llmgateway/runtime/bin
      - input: jre/lib
        output: opt/llmgateway/runtime/lib
      - input: jre/conf
        output: opt/llmgateway/runtime/conf
      - input: bin
        output: opt/llmgateway/bin
      - input: linux
        output: lib/systemd/system
        includes: [llmgateway.service]

packagers:
  deb:
    active: ALWAYS
    requires: [openjdk-17-jre-headless | java17-runtime]
    fileSets:
      - input: conf
        output: etc/llmgateway
        includes: [llmgateway.conf]
    scripts:
      postInstall: linux/postinst
      preUninstall: linux/prerm
      postUninstall: linux/postrm
  rpm:
    active: ALWAYS
    requires: [(java-17-headless or java-21-headless)]
    scripts:
      postInstall: linux/postinst
      preUninstall: linux/prerm
      postUninstall: linux/postrm

# Windows zip（archive assembler）
archive:
  active: ALWAYS
  formats: [ZIP]
  fileSets:
    - input: win-jre          # Windows JRE（本机 jlink）
      output: runtime
    - input: windows          # WinSW.exe + llmgateway.xml + install.ps1 + uninstall.ps1 + start.ps1
      output: bin
    - input: conf
      output: conf
    - input: ../../gateway-boot/target/gateway-boot-${project.version}.jar
      output: bin/llmgateway.jar
```

**关键点**：
- `SINGLE_JAR` distribution：fat jar 作为主 artifact，JRE/conf/脚本/systemd 作为 fileSets 塞入
- deb/rpm 共用同一套 maintainer 脚本（postinst/prerm/postrm），**去除 -rpm 后缀 hack**
- archive assembler 独立出 Windows zip，含 Windows JRE + WinSW + ps1 脚本
- per-file 权限不在此配置，靠 postinst chmod 兜底（§2.3）
- conf 标记 conffile（deb）/ noreplace（rpm，待 build 实测 JReleaser 配置方式）
- 具体 JReleaser 配置语法以 build 阶段实测调整为准，本节给出结构意图

### 3.3 conf 机制（OQ3：占位符 sed 替换）-- 不变

**conf 模板**（`deployments/package/conf/llmgateway.conf`，打入 deb/rpm/zip）：

```sh
# LLM-Gateway 统一配置（conffile，升级保留）
# 服务端口（默认 8080，改后 systemctl restart 生效）
SERVER_PORT=8080
# 数据库连接（H2 文件库，数据目录外部化）
DB_URL=jdbc:h2:file:/var/lib/llmgateway/gateway;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_ON_EXIT=FALSE
# 加密密钥（首次安装由 postinst 生成，占位符 __GENERATE_KEY__ 会被替换）
GATEWAY_ENCRYPTION_KEY=__GENERATE_KEY__
# JVM 参数（运行时可调，改后 restart 生效，无需重打包）
JAVA_OPTS=-Xmx512m -Dmanagement.health.redis.enabled=false
```

**postinst 密钥生成逻辑**（D7：去 openssl，用 `/dev/urandom`）：

```sh
CONF_FILE="/etc/llmgateway/llmgateway.conf"
if grep -q '__GENERATE_KEY__' "$CONF_FILE"; then
    NEW_KEY="$(head -c 32 /dev/urandom | base64)"
    # 用 | 分隔符避免 base64 密钥中的 / 冲突
    sed -i "s|__GENERATE_KEY__|${NEW_KEY}|" "$CONF_FILE"
    echo "[postinst] 生成新的 GATEWAY_ENCRYPTION_KEY（请妥善备份）" >&2
else
    echo "[postinst] 保留已有 GATEWAY_ENCRYPTION_KEY" >&2
fi
chmod 640 "$CONF_FILE"
chown root:llmgateway "$CONF_FILE"
```

**升级保留语义**：
- 首次安装：conf 模板含 `__GENERATE_KEY__`，postinst grep 命中 -> 生成密钥 sed 替换。
- 升级：dpkg/dnf 因 conffile/NOREPLACE 保留已有 conf（占位符已是真实密钥），postinst grep 不命中 -> 跳过。密钥与端口保持不变。

### 3.4 启动脚本

**Linux**（`deployments/package/bin/llmgateway.sh`，不变）：

```sh
#!/bin/sh
# LLM-Gateway 启动脚本：source conf 注入环境变量 + JAVA_OPTS，exec java
set -e
CONF_FILE="/etc/llmgateway/llmgateway.conf"
[ -f "$CONF_FILE" ] || { echo "配置文件不存在: $CONF_FILE" >&2; exit 1; }
. "$CONF_FILE"
exec /opt/llmgateway/runtime/bin/java $JAVA_OPTS \
  -Dspring.profiles.active=local \
  -jar /opt/llmgateway/bin/llmgateway.jar
```

**Windows**（新增 `deployments/package/windows/start.ps1`）：

```ps1
# LLM-Gateway Windows 启动脚本：读 conf 注入环境变量 + JAVA_OPTS
$ErrorActionPreference = "Stop"
$ConfFile = "$PSScriptRoot\..\conf\llmgateway.conf"
if (-not (Test-Path $ConfFile)) { throw "配置文件不存在: $ConfFile" }
Get-Content $ConfFile | ForEach-Object {
    if ($_ -match '^\s*([A-Z_]+)\s*=\s*(.*)$') {
        Set-Item -Path "Env:$($Matches[1])" -Value $Matches[2]
    }
}
& "$PSScriptRoot\..\runtime\bin\java.exe" $env:JAVA_OPTS `
  "-Dspring.profiles.active=local" `
  -jar "$PSScriptRoot\llmgateway.jar"
```

**install.ps1**（注册 WinSW service）：

```ps1
# 注册 LLM-Gateway 为 Windows 服务（WinSW）
$ErrorActionPreference = "Stop"
$BinDir = $PSScriptRoot
Copy-Item "$BinDir\WinSW.exe" "$BinDir\llmgateway.exe" -Force
& "$BinDir\llmgateway.exe" install
Start-Service llmgateway
Write-Host "LLM-Gateway 服务已注册并启动。端口见 conf\llmgateway.conf 的 SERVER_PORT"
```

**uninstall.ps1**：

```ps1
$ErrorActionPreference = "Stop"
& "$PSScriptRoot\llmgateway.exe" uninstall
Write-Host "LLM-Gateway 服务已卸载。数据目录保留。"
```

**关键点**：
- Linux 启动脚本 source conf 不变；Windows start.ps1 解析 conf 注入环境变量（PowerShell 无 source，用 `Get-Content` + 正则）
- Windows service 用 WinSW（exe 改名为 `llmgateway.exe` + xml 配置），`install.ps1` 注册
- 端口由 conf 的 `SERVER_PORT` 配置，与 Linux 统一（不再有 Inno Setup 安装向导交互）
- `-Dspring.profiles.active=local` 激活 local profile（H2 内嵌库，裸机部署）

### 3.5 systemd unit（不变，`deployments/package/linux/llmgateway.service`）

```ini
[Unit]
Description=LLM-Gateway Service
Documentation=https://codingas.com/api-gateway
After=network.target

[Service]
Type=simple
User=llmgateway
Group=llmgateway
WorkingDirectory=/var/lib/llmgateway
# 去掉 EnvironmentFile，改由启动脚本 source conf
ExecStart=/opt/llmgateway/bin/llmgateway.sh
Restart=on-failure
RestartSec=5
StandardOutput=append:/var/log/llmgateway/stdout.log
StandardError=append:/var/log/llmgateway/stderr.log
LimitNOFILE=65536

[Install]
WantedBy=multi-user.target
```

**变化**（相对原 jpackage 方案）：`EnvironmentFile=/etc/llmgateway/env` 删除；`ExecStart` 从 jpackage 启动器改为 `llmgateway.sh`。

### 3.6 maintainer 脚本迁移

**postinst**（改 `deployments/package/linux/postinst`）：
1. 创建系统用户/组 llmgateway（已存在则跳过）
2. 创建数据/日志/配置目录（`/var/lib/llmgateway`、`/var/log/llmgateway`、`/etc/llmgateway`）
3. **生成 conf 密钥**：grep `__GENERATE_KEY__` 占位符 -> `head -c 32 /dev/urandom | base64` 生成 -> sed 替换（`|` 分隔符）
4. 设置 conf 权限 640 root:llmgateway
5. **chmod 兜底 JRE bin 权限**：`chmod -R 0755 /opt/llmgateway/runtime/bin`（§2.3，不依赖打包工具 per-file mode）
6. 注册 systemd unit（`systemctl daemon-reload && systemctl enable`）
7. 启动服务（升级时 restart）

**变化**（相对原 jpackage 方案）：
- 去掉 `. /usr/share/debconf/confmodule` + `db_get`（D4 去 debconf）
- 密钥生成从 `openssl rand -base64 32` 改为 `head -c 32 /dev/urandom | base64`（D7 去 openssl）
- env 文件生成逻辑替换为 conf 占位符 sed 替换
- **新增** chmod 兜底 JRE bin 权限（JReleaser 不保证 per-file mode）

**prerm/postrm**（改 `deployments/package/linux/prerm`、`postrm`）：
- prerm：停止服务 `systemctl stop llmgateway.service`
- postrm：清理 systemd unit（`systemctl disable` + 删除 unit 文件 + daemon-reload）；**保留数据目录** `/var/lib/llmgateway`（卸载不丢数据）

**合并 -rpm 后缀脚本**：
- 删除 `postinst-rpm`/`prerm-rpm`/`postrm-rpm`
- JReleaser `deb`/`rpm` packager 共用同一套 `postinst`/`prerm`/`postrm`（`scripts: postInstall/preUninstall/postUninstall`）
- 消除原 jpackage build.sh 复制 -rpm 脚本为临时 resource-dir 的 hack

### 3.7 build.sh 改造（`deployments/package/build.sh`）

保留 mvn package + jlink（Java 21），删除 jpackage deb/rpm 段，改由 JReleaser assemble 出 deb/rpm + archive 出 zip：

```sh
# 1. mvn package（Java 21）
(cd "$REPO_ROOT" && ./mvnw clean package -pl gateway-boot -am -DskipTests)

# 2. jlink 生成精简 JRE
#    - Linux JRE（deb/rpm 用）：交叉生成或下载（见 4.3）
#    - Windows JRE（zip 用）：本机 jlink
jlink --module-path "$JMODS_DIR" --add-modules "$(cat "$MODULES_FILE" | tr -d '\n')" \
  --strip-debug --no-header-files --no-man-pages --compress=2 --output "$JRE_DIR"

# 3. JReleaser assemble 出 deb/rpm + archive 出 zip（Java 21，无 gradle）
(cd "$REPO_ROOT" && ./mvnw jreleaser:assemble -pl gateway-boot \
  -Djreleaser.project.version="${APP_VERSION}" -Djreleaser.output.directory="$DIST_DIR")
```

**变化**：
- 删除 jpackage deb/rpm 段 + `-rpm` 临时 resource-dir hack
- 删除 gradlew 调用（原 nebula 方案）+ `JAVA17_HOME`
- 新增 `mvnw jreleaser:assemble`，全 Java 21
- `JAVA_OPTS` 硬编码移入 conf 模板（不变）

**build.ps1**：原 jpackage app-image + Inno Setup 流程**删除**（Windows 改 zip，由 JReleaser archive 出）。Windows 包改由 build.sh 在 Windows runner 上统一跑（git bash）。

### 3.8 CI 改造（`.github/workflows/release.yml`，D6）

package job 从 matrix `[ubuntu-latest, windows-latest]` 简化为**单 windows-latest**，单 JDK 21：

```yaml
package:
  name: 构建安装包
  needs: release
  runs-on: windows-latest  # 单 job，去掉 matrix
  steps:
    - uses: actions/checkout@v4

    - name: Setup JDK 21
      uses: actions/setup-java@v4
      with:
        java-version: '21'
        distribution: 'temurin'

    - name: Restore script permissions
      run: chmod +x mvnw deployments/package/build.sh deployments/package/linux/postinst ...

    # 不再需要：sudo apt-get install rpm（JReleaser 纯 Java 出 rpm）
    # 不再需要：choco install innosetup（Windows 改 zip）

    - name: Build packages
      run: bash deployments/package/build.sh
      # build.sh 内 mvn package + jlink + jreleaser:assemble 出 deb/rpm/zip

    - name: Smoke test - deb
      run: |
        DEB=$(ls deployments/package/dist/*.deb | head -1)
        docker run --rm -d --name lg-smoke-deb --privileged --cgroupns=host \
          -v "$PWD/$DEB:/tmp/llmgateway.deb" jrei/systemd-ubuntu:22.04
        # ... systemd 就绪等待 + apt-get install + health 检查
    - name: Smoke test - rpm
      run: |
        # ... jrei/systemd-rockylinux:9 + dnf install + health
    - name: Smoke test - zip (Windows)
      run: |
        $Zip = (Get-ChildItem deployments/package/dist/*.zip | Select-Object -First 1).FullName
        Expand-Archive $Zip -DestinationPath C:\lg-smoke
        C:\lg-smoke\bin\install.ps1
        for ($i=1; $i -le 90; $i++) {
          try { (Invoke-WebRequest -UseBasicParsing http://localhost:8080/actuator/health).Content; break } catch { Start-Sleep -Seconds 1 }
        }
        $svc = Get-Service llmgateway
        if ($svc.Status -ne 'Running') { throw "服务未运行" }
        C:\lg-smoke\bin\uninstall.ps1
```

**关键点**：
- 单 JDK 21（砍 Java 17 + `JAVA17_HOME`）
- 砍 `sudo apt-get install rpm`（JReleaser 纯 Java 出 rpm）
- 砍 `choco install innosetup`（Windows 改 zip）
- smoke test：deb/rpm 用 systemd 容器（不变）；exe smoke 改 zip smoke（解压 + `install.ps1` + health + `Get-Service` + `uninstall.ps1`）
- 单 windows job 串行：JReleaser 出 deb/rpm/zip

### 3.9 清理冗余

| 删除文件 | 原因 |
|---------|------|
| `deployments/package/build.gradle` | JReleaser 替代 nebula（原 plan 产物，未实现则直接不创建） |
| `deployments/package/gradle/wrapper/` | 同上，无 gradle |
| `deployments/package/linux/llmgateway.templates` | debconf 模板，D4 去掉 debconf |
| `deployments/package/linux/llmgateway.config` | debconf 收集脚本，D4 去掉 debconf |
| `deployments/package/linux/postinst-rpm`/`prerm-rpm`/`postrm-rpm` | JReleaser 原生支持 rpm maintainer 脚本，合并 |
| `deployments/package/windows/*.iss`（Inno Setup 脚本） | Windows 改 zip，不再用 Inno Setup |
| `deployments/package/build.ps1` 的 jpackage+iscc 段 | Windows 改 zip |

## 4. 关键取舍与风险

### 4.1 design 偏差（相对原 design.md，已确认）

1. **D1 打包工具**：nebula-ospackage -> **JReleaser assemble**。原 design 选 nebula（参照 thingsboard），但 design 阶段实测发现 nebula 8.6.3 锁定 gradle 7.x + Java 17（死结无解）。JReleaser 1.25.0 活跃、纯 Java、Maven 原生，消除死结。
2. **D5 构建工具**：Gradle 独立 wrapper -> **删除，纯 Maven**。
3. **D6 CI**：单 windows job 出 deb/rpm + exe -> 出 **deb/rpm + zip**。双 JDK -> 单 JDK 21。
4. **D8 JRE 权限**：nebula fileMode 0755 -> **postinst chmod 兜底**。
5. **D9 Windows**（新增）：exe（jpackage+iscc+WinSW）-> **zip（JReleaser archive + install.ps1 注册 WinSW）**。BREAKING：无安装向导，端口改 conf。
6. **redlinerpm-maven-plugin 不可用**：build 前查证 Central 上 `com.airsquared:redlinerpm-maven-plugin` 404、同名插件最高 0.0.7 半成品，故 rpm 走 JReleaser（封装 Redline）而非独立 redlinerpm 插件。

### 4.2 风险与缓解

| 风险 | 缓解 |
|------|------|
| JReleaser 在 Windows 出 deb/rpm/zip 未实测 | build 阶段 Windows 跑 `mvn package` 验证；deb 自实现 assembler + rpm Redline 均纯 Java |
| JReleaser fileSet per-file 权限未确认 | postinst `chmod -R 0755 runtime/bin` 兜底（§2.3），符合 Linux 包惯例 |
| Redline RPM 2021 停更 | JReleaser 1.25.0 活跃封装层兜底维护 |
| jlink 生成 Windows JRE 不能塞进 deb/rpm | 见 4.3，jlink 交叉生成 Linux JRE 或下载 |
| Windows zip 无安装向导（BREAKING） | install.ps1 注册 WinSW service 维持开机自启；端口改 conf；文档说明迁移 |
| deb/rpm 体积 83MB fat jar + 50MB JRE | 留 CI 实测打包时间与体积；JReleaser 纯 Java 写归档，预计可接受 |

### 4.3 待 build 阶段确认的 JRE 平台问题 ⚠️

**风险**（与打包工具无关）：jlink 在 Windows runner 上生成的 JRE 是 Windows 版（`bin/java.exe` + `*.dll`），不能塞进 Linux deb/rpm（需 `bin/java` + `*.so`）。Windows zip 用 Windows JRE 无此问题。

**可能方案**（build 阶段验证）：
1. jlink 交叉生成：`--module-path <linux-jmods>` 指向下载的 Linux JDK jmods，生成 Linux JRE。需 CI 下载 Linux JDK。
2. 下载预构建 Linux JRE：从 Adoptium 下载 Linux JRE tarball，解压后塞进 deb/rpm。
3. jlink 不塞进 deb/rpm，改为 deb `requires openjdk-17-jre`（依赖系统 JRE）。

**当前设计倾向**：方案 1（jlink 交叉生成，保持 JRE 精简可控），build 阶段实测验证。若交叉生成复杂，回退方案 2（下载 Linux JRE）。

## 5. 测试策略

### 5.1 本机验证层（build 阶段实测）

| 验证项 | 方法 | 预期 |
|--------|------|------|
| JReleaser Windows 跨平台 | `mvn package` 跑 assemble + archive | 出 deb + rpm + zip |
| bin/java 权限 | 安装 deb/rpm 后检查 `runtime/bin/java` 0755（postinst chmod） | 可执行 |
| conf 升级保留 | 升级后检查 conf 不变 | conffile/noreplace 生效 |
| Windows zip service | 解压 + install.ps1 + Get-Service | WinSW service Running |

### 5.2 CI 验证层（留 CI）

| Scenario | 验证内容 | 验证位置 |
|----------|---------|---------|
| deb 全新安装 | systemd-ubuntu 容器 apt-get install + /actuator/health 200 | CI smoke - deb |
| rpm 全新安装 | systemd-rockylinux 容器 dnf install + /actuator/health 200 | CI smoke - rpm |
| zip 全新安装 | windows runner 解压 + install.ps1 + Get-Service + health 200 | CI smoke - zip |
| conf 改端口生效 | 改 conf SERVER_PORT + restart + health 新端口 | CI（deb/rpm 容器） |
| JVM 参数运行时可调 | 改 conf JAVA_OPTS + restart + 新参数生效 | CI（deb/rpm 容器） |
| 升级保留 conf/数据 | 旧版 -> 新版升级 -> conf 不变 + /var/lib 数据不丢 | CI（多阶段容器） |
| 卸载数据保留 | apt/dnf remove -> /var/lib 数据目录保留 | CI（容器） |
| CI 产出多平台包 | release tag -> 单 windows job 产出 deb/rpm/zip 挂 Release | CI release workflow |

## 6. 实现注意事项

1. **JReleaser 配置位置**：`deployments/package/jreleaser.yml` 或 pom 插件段。`mvn jreleaser:assemble` 触发。
2. **不绑 release**：只用 `jreleaser:assemble`（打包），不绑 `jreleaser:release`（本项目 GH Release 已有，避免本地误发 release）。
3. **JAVA17_HOME 不再需要**：全 Java 21，build.sh 与 JReleaser 统一用 `JAVA_HOME`。
4. **maintainer 脚本权限**：`postinst`/`prerm`/`postrm` 需可执行位。CI checkout 后 `chmod +x` 恢复（Windows checkout 丢失 +x）。
5. **conf 占位符**：`__GENERATE_KEY__` 是唯一占位符，postinst 用 `grep -q` + `sed -i "s|...|..."`。不要用 `/` 作为 sed 分隔符（base64 密钥含 `/`）。
6. **JRE 平台问题**（4.3）：build 阶段确认 jlink 交叉生成 Linux JRE 或下载 Linux JRE。
7. **不动 application*.yml**：conf 的环境变量通过 Spring `${ENV:默认}` 占位符绑定，application*.yml 已支持（前置 one-click-bare-deploy 配的 env 注入）。
8. **BREAKING 说明**：去掉 debconf + Windows exe 改 zip，升级文档说明迁移。
9. **Windows zip 结构**：`runtime/`（Windows JRE）+ `bin/`（jar + WinSW.exe + llmgateway.xml + install.ps1 + uninstall.ps1 + start.ps1）+ `conf/`。`install.ps1` 注册 service，`uninstall.ps1` 卸载。
10. **JReleaser 配置语法**：§3.2 给出结构意图，具体 fileSet/packager 语法以 build 阶段实测 JReleaser 1.25.0 文档调整为准（design 阶段未实测 JReleaser 配置）。
