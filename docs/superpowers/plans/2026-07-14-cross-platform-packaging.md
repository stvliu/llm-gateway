---
change: cross-platform-packaging
design-doc: docs/superpowers/specs/2026-07-13-cross-platform-packaging-design.md
base-ref: 51c8cd0048ff59984bae871ed60628dd1089500b
---

# 跨平台打包重构（JReleaser）实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将裸机部署打包链从 jpackage（依赖系统 dpkg-deb/rpmbuild/iscc）迁移到 JReleaser assemble（纯 Java 跨平台），实现 Windows 单机一次构建同时产出 deb + rpm + zip，并用单一 `llmgateway.conf` 统一配置外部化。

**Architecture:** Maven（Java 21）出 fat jar → jlink 生成精简 JRE（Linux JRE 交叉生成、Windows JRE 本机生成）→ JReleaser maven 插件 1.25.0 assemble 出 deb/rpm（deb 自实现 assembler + rpm Redline，纯 Java）+ archive 出 Windows zip（WinSW + install.ps1）。deb/rpm 共用 maintainer 脚本与 systemd unit，启动脚本 `source llmgateway.conf` 注入环境变量与 JAVA_OPTS。CI 从双 OS matrix 简化为单 windows-latest 单 JDK 21。

**Tech Stack:** Java 21、Maven 3.9+（mvnw）、JReleaser 1.25.0、jlink、systemd、WinSW 2.x、PowerShell 5.1+、GitHub Actions。

**Design Doc:** `docs/superpowers/specs/2026-07-13-cross-platform-packaging-design.md`（技术设计事实源，本计划引用其章节编号）

**环境限制（重要）:** 本开发机（Windows 11）无 docker / iscc / dpkg-deb / rpm / gh CLI。本机能验证：mvn package、jlink（Windows JRE）、shell/PowerShell 语法检查、conf source 解析。需 CI 或容器环境验证：deb/rpm 安装、systemd 服务、升级/卸载（第 8 组验证任务已标注）。

---

## 文件结构映射

### 新增文件
| 文件 | 职责 |
|------|------|
| `deployments/package/conf/llmgateway.conf` | 统一配置模板（端口/DB/密钥占位符/JAVA_OPTS），打入 deb/rpm/zip，conffile 升级保留 |
| `deployments/package/bin/llm-gateway.sh` | Linux 启动脚本：source conf + exec java $JAVA_OPTS -jar |
| `deployments/package/jreleaser.yml` | JReleaser 配置：SINGLE_JAR distribution + deb/rpm packager + archive(Windows zip) |
| `deployments/package/windows/start.ps1` | Windows 启动脚本：解析 conf 注入环境变量 + java -jar |
| `deployments/package/windows/install.ps1` | Windows 服务注册：生成密钥 + WinSW install + Start-Service |
| `deployments/package/windows/uninstall.ps1` | Windows 服务卸载：WinSW uninstall |
| `deployments/package/windows/llm-gateway.xml` | WinSW service 配置（与 llm-gateway.exe 同名配对） |

### 修改文件
| 文件 | 变更 |
|------|------|
| `gateway-boot/pom.xml` | 加 `pkg` profile + JReleaser 1.25.0 插件声明 |
| `deployments/package/linux/llm-gateway.service` | ExecStart 指向 llm-gateway.sh，删 EnvironmentFile |
| `deployments/package/linux/postinst` | 重写：生成 conf（/dev/urandom 密钥 + sed 替换）+ chmod 兜底 JRE bin，去 debconf/openssl |
| `deployments/package/linux/prerm` | 重写为 deb/rpm 共用版（参数语义兼容） |
| `deployments/package/linux/postrm` | 重写为 deb/rpm 共用版（参数语义兼容） |
| `deployments/package/build.sh` | 删 jpackage 段，改 mvn package + jlink(双平台 JRE) + jreleaser:assemble |
| `deployments/package/windows/download-winsw.ps1` | 输出名 LLMGateway.exe → WinSW.exe |
| `.github/workflows/release.yml` | package job 简化为单 windows-latest 单 JDK 21，smoke test 改 zip |

### 删除文件
| 文件 | 原因 |
|------|------|
| `deployments/package/linux/llm-gateway.templates` | debconf 模板（D4 去 debconf） |
| `deployments/package/linux/llm-gateway.config` | debconf 收集脚本（D4 去 debconf） |
| `deployments/package/linux/postinst-rpm` | JReleaser 共用 maintainer 脚本 |
| `deployments/package/linux/prerm-rpm` | 同上 |
| `deployments/package/linux/postrm-rpm` | 同上 |
| `deployments/package/windows/LLMGateway.xml` | 被 llm-gateway.xml 替代（exe 改名） |
| `deployments/package/windows/llm-gateway.iss` | Inno Setup 脚本（Windows 改 zip） |
| `deployments/package/build.ps1` | jpackage+iscc 流程删除（Windows 改 zip，由 build.sh 统一） |

---

## 第 1 组：conf 与启动脚本

### Task 1.1: 新增 conf 模板

**Files:**
- Create: `deployments/package/conf/llmgateway.conf`

**依据:** Design Doc §3.3。conf 是 shell 脚本，启动脚本 `source` 后环境变量被 Spring `${ENV:默认}` 占位符绑定（application.yml 已支持 `server.port: ${SERVER_PORT:8080}` 等，环境变量优先级高于 yml）。`__GENERATE_KEY__` 是唯一占位符，由 postinst/install.ps1 首次安装时替换。

- [x] **Step 1: 创建 conf 模板文件**

创建 `deployments/package/conf/llmgateway.conf`，内容如下（逐行照抄，`__GENERATE_KEY__` 是占位符不要替换）：

```sh
# LLM-Gateway 统一配置（conffile，升级保留）
# 服务端口（默认 8080，改后 systemctl restart llm-gateway 生效）
SERVER_PORT=8080
# 数据库连接（H2 文件库，数据目录外部化）
DB_URL=jdbc:h2:file:/var/lib/llm-gateway/gateway;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_ON_EXIT=FALSE
# 加密密钥（首次安装由 postinst 生成，占位符 __GENERATE_KEY__ 会被替换）
GATEWAY_ENCRYPTION_KEY=__GENERATE_KEY__
# JVM 参数（运行时可调，改后 restart 生效，无需重打包）
JAVA_OPTS=-Xmx512m -Dmanagement.health.redis.enabled=false
```

- [x] **Step 2: 验证 conf 可被 shell source 且占位符可 grep 命中**

在 git bash 中运行：

```bash
source deployments/package/conf/llmgateway.conf && echo "SERVER_PORT=$SERVER_PORT" && echo "JAVA_OPTS=$JAVA_OPTS"
grep -q '__GENERATE_KEY__' deployments/package/conf/llmgateway.conf && echo "占位符存在 OK"
```

预期输出包含 `SERVER_PORT=8080`、`JAVA_OPTS=-Xmx512m -Dmanagement.health.redis.enabled=false`、`占位符存在 OK`。

- [x] **Step 3: 提交**

```bash
git add deployments/package/conf/llmgateway.conf
git commit -m "feat(packaging): 新增 llmgateway.conf 统一配置模板（端口/DB/密钥占位符/JAVA_OPTS）"
```

---

### Task 1.2: 新增 Linux 启动脚本

**Files:**
- Create: `deployments/package/bin/llm-gateway.sh`

**依据:** Design Doc §3.4。systemd ExecStart 指向此脚本，source conf 后 exec java。`$JAVA_OPTS` 不加引号以触发 word splitting（`-Xmx512m` 与 `-D...` 拆为独立 JVM 参数）。

- [x] **Step 1: 创建启动脚本**

创建 `deployments/package/bin/llm-gateway.sh`：

```sh
#!/bin/sh
# LLM-Gateway Linux 启动脚本：source conf 注入环境变量 + JAVA_OPTS，exec java
set -e
CONF_FILE="/etc/llm-gateway/llmgateway.conf"
[ -f "$CONF_FILE" ] || { echo "配置文件不存在: $CONF_FILE" >&2; exit 1; }
. "$CONF_FILE"
exec /opt/llm-gateway/runtime/bin/java $JAVA_OPTS \
  -Dspring.profiles.active=local \
  -jar /opt/llm-gateway/bin/llm-gateway.jar
```

- [x] **Step 2: 语法检查**

```bash
bash -n deployments/package/bin/llm-gateway.sh && echo "语法 OK"
```

预期：`语法 OK`。

- [x] **Step 3: 提交**

```bash
git add deployments/package/bin/llm-gateway.sh
git commit -m "feat(packaging): 新增 Linux 启动脚本 llm-gateway.sh（source conf + exec java）"
```

---

### Task 1.3: 改 systemd unit

**Files:**
- Modify: `deployments/package/linux/llm-gateway.service`

**依据:** Design Doc §3.5。删 `EnvironmentFile=/etc/llm-gateway/env`（conf 由启动脚本 source），ExecStart 从 jpackage 启动器 `/opt/llm-gateway/bin/llm-gateway` 改为 `llm-gateway.sh`。

- [x] **Step 1: 修改 systemd unit**

将 `deployments/package/linux/llm-gateway.service` 的 `[Service]` 段中：
- 删除行 `EnvironmentFile=/etc/llm-gateway/env`
- 将 `ExecStart=/opt/llm-gateway/bin/llm-gateway` 改为 `ExecStart=/opt/llm-gateway/bin/llm-gateway.sh`

修改后完整文件应为：

```ini
[Unit]
Description=LLM-Gateway Service
Documentation=https://codingas.com/api-gateway
After=network.target

[Service]
Type=simple
User=llm-gateway
Group=llm-gateway
WorkingDirectory=/var/lib/llm-gateway
ExecStart=/opt/llm-gateway/bin/llm-gateway.sh
Restart=on-failure
RestartSec=5
StandardOutput=append:/var/log/llm-gateway/stdout.log
StandardError=append:/var/log/llm-gateway/stderr.log
LimitNOFILE=65536

[Install]
WantedBy=multi-user.target
```

- [x] **Step 2: 提交**

```bash
git add deployments/package/linux/llm-gateway.service
git commit -m "refactor(packaging): systemd unit ExecStart 指向 llm-gateway.sh，去掉 EnvironmentFile"
```

---

## 第 2 组：JReleaser 打包配置

### Task 2.1: pom.xml 加 JReleaser 插件段

**Files:**
- Modify: `gateway-boot/pom.xml`

**依据:** Design Doc §3.2、§6。插件放 `gateway-boot/pom.xml`（build.sh 用 `-pl gateway-boot` 触发），用 `pkg` profile 包裹避免污染正常 `mvn package`。只声明插件，不绑定生命周期阶段（由 build.sh 显式调 `jreleaser:assemble` goal，确保在 jlink 生成 JRE 之后执行）。只 assemble 不 release（Design §6 注意事项 2）。

> 注意：tasks.md 写 `pom.xml`，实际精化为 `gateway-boot/pom.xml`（因为 build.sh 用 `-pl gateway-boot`，插件必须在该模块才被触发）。

- [x] **Step 1: 在 gateway-boot/pom.xml 的 `<properties>` 后或 `<build>` 前加版本属性**

在 `gateway-boot/pom.xml` 顶部 `<packaging>jar</packaging>` 之后、`<dependencies>` 之前，没有 `<properties>` 段，所以版本属性加在根 `pom.xml`。改为在根 `pom.xml` 的 `<properties>` 段末尾（`<flatten-maven-plugin.version>1.6.0</flatten-maven-plugin.version>` 之后）追加：

```xml
        <!-- JReleaser 打包插件版本 -->
        <jreleaser-maven-plugin.version>1.25.0</jreleaser-maven-plugin.version>
```

- [x] **Step 2: 在 gateway-boot/pom.xml 的 `<profiles>` 段末尾追加 pkg profile**

在 `gateway-boot/pom.xml` 的 `<profiles>` 段内（`docker` profile 之后、`</profiles>` 之前）追加：

```xml
        <!-- 裸机部署打包：mvn jreleaser:assemble -Ppkg 触发，出 deb/rpm + zip -->
        <profile>
            <id>pkg</id>
            <build>
                <plugins>
                    <plugin>
                        <groupId>org.jreleaser</groupId>
                        <artifactId>jreleaser-maven-plugin</artifactId>
                        <version>${jreleaser-maven-plugin.version}</version>
                        <configuration>
                            <!-- JReleaser 配置文件（相对 gateway-boot 模块 basedir） -->
                            <configFile>${project.basedir}/../deployments/package/jreleaser.yml</configFile>
                            <!-- 只 assemble 打包，不触发 release（本项目 GH Release 由 CI 单独管理） -->
                            <skipJreleaserRelease>true</skipJreleaserRelease>
                        </configuration>
                    </plugin>
                </plugins>
            </build>
        </profile>
```

- [x] **Step 3: 验证 Maven 能解析插件（下载插件元数据，不执行打包）**

```bash
./mvnw help:describe -pl gateway-boot -Dplugin=org.jreleaser:jreleaser-maven-plugin:1.25.0 -Ppkg -q
```

预期：输出插件描述（`JReleaser Maven Plugin`），无报错。若网络问题导致下载失败，确认 `~/.m2/repository/org/jreleaser/` 下有插件 jar。

- [x] **Step 4: 提交**

```bash
git add pom.xml gateway-boot/pom.xml
git commit -m "feat(packaging): gateway-boot 加 pkg profile + JReleaser 1.25.0 插件声明"
```

---

### Task 2.2: 新增 jreleaser.yml（distribution + fileSets）

**Files:**
- Create: `deployments/package/jreleaser.yml`

**依据:** Design Doc §3.2。JReleaser 配置文件在 `deployments/package/`，`configFile` 由 pom 指定。JReleaser 以 configFile 所在目录为 basedir 解析相对路径，故 `input: jre/bin` 解析为 `deployments/package/jre/bin`，`path: ../../gateway-boot/target/...` 解析为 repo 根的 `gateway-boot/target/...`。

> **待实测标注（Design §3.2 末尾）：** 以下为结构化初版。JReleaser 1.25.0 的 fileSet/packager 确切语法以 build 阶段实测报错调整为准。Task 8.1 是首个实测点。

- [x] **Step 1: 创建 jreleaser.yml**

创建 `deployments/package/jreleaser.yml`：

```yaml
# JReleaser 配置：SINGLE_JAR distribution + deb/rpm packager + archive(Windows zip)
# 触发：mvn jreleaser:assemble -pl gateway-boot -Ppkg
# basedir = 本文件所在目录（deployments/package/），fileSet input 相对于此
project:
  name: llm-gateway
  version: ${project.version}
  description: LLM-Gateway - 企业级 AI 模型 API 聚合网关
  authors: [LLM-Gateway]
  license: Apache-2.0

distributions:
  llm-gateway:
    type: SINGLE_JAR
    artifacts:
      # fat jar 作为主 artifact，打包时重命名为 llm-gateway.jar
      - path: ../../gateway-boot/target/gateway-boot-${project.version}.jar
        transform: llm-gateway.jar
    # Linux JRE（deb/rpm 用，build.sh 交叉生成到 jre/ 目录）
    fileSets:
      - input: jre/bin
        output: opt/llm-gateway/runtime/bin
      - input: jre/lib
        output: opt/llm-gateway/runtime/lib
      - input: jre/conf
        output: opt/llm-gateway/runtime/conf
      - input: bin
        output: opt/llm-gateway/bin
        includes: [llm-gateway.sh]
      - input: linux
        output: lib/systemd/system
        includes: [llm-gateway.service]

packagers:
  deb:
    active: ALWAYS
    # 待实测：JReleaser deb requires 语法（虚包名）
    requires: [openjdk-17-jre-headless | java17-runtime]
    fileSets:
      - input: conf
        output: etc/llm-gateway
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

# Windows zip（archive assembler，独立 fileSets，用 Windows JRE）
assemble:
  archive:
    llm-gateway-win:
      active: ALWAYS
      formats: [ZIP]
      exportDir: dist
      fileSets:
        - input: jre-win
          output: runtime
        - input: windows
          output: bin
          includes: [WinSW.exe, llm-gateway.xml, install.ps1, uninstall.ps1, start.ps1]
        - input: conf
          output: conf
          includes: [llmgateway.conf]
        - input: ../../gateway-boot/target/gateway-boot-${project.version}.jar
          output: bin/llm-gateway.jar
```

- [x] **Step 2: 提交**

```bash
git add deployments/package/jreleaser.yml
git commit -m "feat(packaging): 新增 jreleaser.yml（SINGLE_JAR + fileSets 骨架）"
```

---

### Task 2.3: 配 deb/rpm packager 细节 + archive Windows zip

**Files:**
- Modify: `deployments/package/jreleaser.yml`

**依据:** Design Doc §3.2 关键点。deb/rpm 共用 maintainer 脚本（postinst/prerm/postrm，去掉 -rpm 后缀）。conf 标记 conffile（deb）/ noreplace（rpm，待实测 JReleaser 配置方式）。Windows zip 含 Windows JRE + WinSW + ps1。

> Task 2.2 已创建完整 jreleaser.yml 含 packagers + archive 段。本任务是补充注释与待实测标记，确保 deb/rpm conffile 配置就位。

- [x] **Step 1: 在 jreleaser.yml 的 deb/rpm packager 段补充 conffile/noreplace 配置注释**

在 `deployments/package/jreleaser.yml` 的 `deb:` 段 `fileSets` 之前追加 conffile 配置注释；在 `rpm:` 段 `scripts` 之前追加 noreplace 注释。在 `deb:` 的 `fileSets` 的 conf 项后追加待实测标记。

具体：将 deb 段的：
```yaml
    fileSets:
      - input: conf
        output: etc/llm-gateway
        includes: [llmgateway.conf]
```
替换为：
```yaml
    # conffile：dpkg 升级时保留（NOREPLACE 语义）
    # 已确认：JReleaser 1.25.0 DebAssembler 不支持 conffile 字段，通过 templates/deb/control/conffiles.tpl 注入 conffiles 文件实现升级保留
    fileSets:
      - input: conf
        output: etc/llm-gateway
        includes: [llmgateway.conf]
```

将 rpm 段的 `requires:` 行后追加：
```yaml
    # %config(noreplace)：dnf 升级时保留 conf
    # 已确认：JpackageAssembler 不支持 noreplace 标记配置；jpackage --resource-dir 对 rpm 仅支持覆盖完整 .spec，不支持 maintainer 脚本注入（CANNOT_FIX，需单独编写 .spec 或重新评估 rpm 方案）
```

- [x] **Step 2: 验证 YAML 语法**

```bash
python -c "import yaml,sys; yaml.safe_load(open('deployments/package/jreleaser.yml',encoding='utf-8')); print('YAML OK')" 2>/dev/null || echo "若无 python，跳过；CI 会校验"
```

预期：`YAML OK`（若无 python 则跳过，JReleaser 运行时会校验）。

- [x] **Step 3: 提交**

```bash
git add deployments/package/jreleaser.yml
git commit -m "feat(packaging): jreleaser.yml 补 deb/rpm conffile 与 archive zip 配置注释"
```

---

## 第 3 组：maintainer 脚本迁移

### Task 3.1: 改 postinst（生成 conf + chmod 兜底）

**Files:**
- Modify: `deployments/package/linux/postinst`

**依据:** Design Doc §3.3、§3.6。重写 postinst：去 debconf（删 `. /usr/share/debconf/confmodule` + `db_get` + `db_stop`）、密钥改 `/dev/urandom | base64`（D7 去 openssl）、生成 conf 占位符 sed 替换（替代 env 文件）、新增 `chmod -R 0755 runtime/bin` 兜底 JRE 权限（§2.3）。deb/rpm 共用此脚本（rpm %post 不传 remove 语义参数，安装/升级都执行配置逻辑）。

- [x] **Step 1: 重写 postinst**

将 `deployments/package/linux/postinst` 全文替换为：

```bash
#!/bin/bash
# LLM-Gateway deb/rpm 安装后脚本（JReleaser 布局，deb/rpm 共用）
# 首次安装：生成 conf 密钥占位符替换；升级：conffile 保留，跳过生成
set -e

DATA_DIR="/var/lib/llm-gateway"
LOG_DIR="/var/log/llm-gateway"
CONF_DIR="/etc/llm-gateway"
CONF_FILE="$CONF_DIR/llmgateway.conf"
RUNTIME_BIN="/opt/llm-gateway/runtime/bin"

# 1. 创建系统用户与组（已存在则跳过）
if ! getent group llm-gateway >/dev/null; then
  groupadd --system llm-gateway
fi
if ! id -u llm-gateway >/dev/null 2>&1; then
  useradd --system --no-create-home --shell /usr/sbin/nologin \
    --gid llm-gateway --home-dir "$DATA_DIR" llm-gateway
fi

# 2. 创建数据/日志/配置目录
mkdir -p "$DATA_DIR" "$LOG_DIR" "$CONF_DIR"
chown -R llm-gateway:llm-gateway "$DATA_DIR" "$LOG_DIR"

# 3. 生成加密密钥（首次安装：conf 含 __GENERATE_KEY__ 占位符则替换；升级：conffile 保留，grep 不命中则跳过）
if [ -f "$CONF_FILE" ] && grep -q '__GENERATE_KEY__' "$CONF_FILE"; then
  NEW_KEY="$(head -c 32 /dev/urandom | base64)"
  # 用 | 分隔符避免 base64 密钥中的 / 冲突
  sed -i "s|__GENERATE_KEY__|${NEW_KEY}|" "$CONF_FILE"
  echo "[postinst] 生成新的 GATEWAY_ENCRYPTION_KEY（请妥善备份）" >&2
else
  echo "[postinst] 保留已有 GATEWAY_ENCRYPTION_KEY" >&2
fi

# 4. 设置 conf 权限（root 可读写，llm-gateway 组可读）
chmod 640 "$CONF_FILE" 2>/dev/null || true
chown root:llm-gateway "$CONF_FILE" 2>/dev/null || true

# 5. chmod 兜底 JRE bin 权限（JReleaser 不保证 per-file mode，postinst 矫正 bin/java 可执行位）
if [ -d "$RUNTIME_BIN" ]; then
  chmod -R 0755 "$RUNTIME_BIN"
fi

# 6. 注册 systemd unit（JReleaser 打包到 /lib/systemd/system 或 /usr/lib/systemd/system）
systemctl daemon-reload
systemctl enable llm-gateway.service

# 7. 启动服务（升级时 restart，首次安装 start）
if systemctl is-active --quiet llm-gateway.service; then
  systemctl restart llm-gateway.service
else
  systemctl start llm-gateway.service
fi

echo "LLM-Gateway 已安装并启动。"
echo "  配置文件: $CONF_FILE（改后 systemctl restart llm-gateway 生效）"
echo "  数据目录: $DATA_DIR"
echo "  服务状态: systemctl status llm-gateway"

exit 0
```

- [x] **Step 2: 语法检查**

```bash
bash -n deployments/package/linux/postinst && echo "语法 OK"
```

预期：`语法 OK`。

- [x] **Step 3: 提交**

```bash
git add deployments/package/linux/postinst
git commit -m "refactor(packaging): postinst 重写为 conf 生成（/dev/urandom 密钥）+ chmod 兜底 JRE 权限，去 debconf/openssl"
```

---

### Task 3.2: 改 prerm/postrm（共用版，适配 conf 与新布局）

**Files:**
- Modify: `deployments/package/linux/prerm`
- Modify: `deployments/package/linux/postrm`

**依据:** Design Doc §3.6。deb 与 rpm 的 maintainer 脚本参数语义不同（deb postinst 收 `configure`，prerm 收 `remove|upgrade`；rpm %preun 收 `1`(升级)|`0`(卸载)）。共用脚本通过 case 匹配两种语义。保留数据目录 `/var/lib/llm-gateway`。

- [x] **Step 1: 重写 prerm 为 deb/rpm 共用版**

将 `deployments/package/linux/prerm` 全文替换为：

```bash
#!/bin/bash
# LLM-Gateway deb/rpm 卸载前脚本（共用）
# deb: $1 = remove|upgrade|failed-upgrade|deconfigure|...
# rpm: $1 = 1(升级) | 0(卸载)
# 升级时不停止服务（postinst restart 会处理）；卸载时停止并禁用
set -e

SHOULD_STOP=0
case "${1:-}" in
  remove|deconfigure|0) SHOULD_STOP=1 ;;
esac

if [ "$SHOULD_STOP" = "1" ]; then
  if [ -x /usr/bin/systemctl ] || [ -x /bin/systemctl ]; then
    systemctl stop llm-gateway.service 2>/dev/null || true
    systemctl disable llm-gateway.service 2>/dev/null || true
  fi
fi

exit 0
```

- [x] **Step 2: 重写 postrm 为 deb/rpm 共用版**

将 `deployments/package/linux/postrm` 全文替换为：

```bash
#!/bin/bash
# LLM-Gateway deb/rpm 卸载后脚本（共用）
# deb: $1 = remove|purge|upgrade|failed-upgrade|...
# rpm: $1 = 0(卸载) | 1(升级)
# 卸载时清理 systemd unit；保留数据目录（仅 deb purge 清数据）
set -e

# 卸载时清理 systemd unit（升级不清理）
SHOULD_CLEAN=0
case "${1:-}" in
  remove|purge|0) SHOULD_CLEAN=1 ;;
esac

if [ "$SHOULD_CLEAN" = "1" ]; then
  rm -f /etc/systemd/system/llm-gateway.service
  rm -f /usr/lib/systemd/system/llm-gateway.service 2>/dev/null || true
  systemctl daemon-reload 2>/dev/null || true
fi

# purge 模式（仅 deb）清理数据目录与配置
if [ "${1:-}" = "purge" ]; then
  echo "[postrm] purge 模式：清理数据目录与配置..."
  rm -rf /var/lib/llm-gateway /var/log/llm-gateway /etc/llm-gateway
  echo "[postrm] 数据已清除。如需完全移除用户：userdel llm-gateway"
fi

echo "[postrm] 卸载完成。数据目录 /var/lib/llm-gateway 已保留（除非 purge）。"

exit 0
```

- [x] **Step 3: 语法检查**

```bash
bash -n deployments/package/linux/prerm && bash -n deployments/package/linux/postrm && echo "语法 OK"
```

预期：`语法 OK`。

- [x] **Step 4: 提交**

```bash
git add deployments/package/linux/prerm deployments/package/linux/postrm
git commit -m "refactor(packaging): prerm/postrm 重写为 deb/rpm 共用版（参数语义兼容，保留数据目录）"
```

---

### Task 3.3: 删除 -rpm 后缀脚本

**Files:**
- Delete: `deployments/package/linux/postinst-rpm`
- Delete: `deployments/package/linux/prerm-rpm`
- Delete: `deployments/package/linux/postrm-rpm`

**依据:** Design Doc §3.6。JReleaser deb/rpm packager 共用同一套 maintainer 脚本（`scripts: postInstall/preUninstall/postUninstall`），消除原 jpackage build.sh 复制 -rpm 脚本为临时 resource-dir 的 hack。

- [x] **Step 1: 删除三个 -rpm 后缀脚本**

```bash
git rm deployments/package/linux/postinst-rpm deployments/package/linux/prerm-rpm deployments/package/linux/postrm-rpm
```

- [x] **Step 2: 确认 linux 目录仅剩共用脚本**

```bash
ls deployments/package/linux/
```

预期：仅剩 `llm-gateway.service`、`postinst`、`prerm`、`postrm`（无 -rpm 后缀、无 templates/config，后两者在第 7 组删）。

- [x] **Step 3: 提交**

```bash
git commit -m "refactor(packaging): 删除 postinst-rpm/prerm-rpm/postrm-rpm，JReleaser deb/rpm 共用 maintainer 脚本"
```

---

## 第 4 组：Windows zip 脚本

### Task 4.1: 新增 Windows ps1 脚本（start/install/uninstall）

**Files:**
- Create: `deployments/package/windows/start.ps1`
- Create: `deployments/package/windows/install.ps1`
- Create: `deployments/package/windows/uninstall.ps1`

**依据:** Design Doc §3.4。start.ps1 解析 conf 注入环境变量（PowerShell 无 source，用 `Get-Content` + 正则）。install.ps1 生成密钥（复用现有 iss 的 RandomNumberGenerator 逻辑）+ WinSW install + Start-Service。uninstall.ps1 调 WinSW uninstall。

> **关键修正：** `$env:JAVA_OPTS` 在 PowerShell 中作为单个字符串传递给 java.exe 会导致参数不分割。必须用 `-split` 拆为数组后 splatting（`@javaOpts`）。

- [x] **Step 1: 创建 start.ps1**

创建 `deployments/package/windows/start.ps1`：

```ps1
# LLM-Gateway Windows 启动脚本：读 conf 注入环境变量 + JAVA_OPTS，启动 java
$ErrorActionPreference = "Stop"
$ConfFile = Join-Path $PSScriptRoot "..\conf\llmgateway.conf"
if (-not (Test-Path $ConfFile)) { throw "配置文件不存在: $ConfFile" }

# 解析 conf（shell 风格 KEY=VALUE）注入环境变量，剥离 shell 风格首尾引号
Get-Content $ConfFile | ForEach-Object {
    if ($_ -match '^\s*([A-Z_]+)\s*=\s*(.*)$') {
        $val = $Matches[2]
        # shell 风格引号剥离（双引号或单引号成对匹配）
        if ($val -match '^"(.*)"$') { $val = $Matches[1] }
        elseif ($val -match "^'(.*)'$") { $val = $Matches[1] }
        Set-Item -Path "Env:$($Matches[1])" -Value $val
    }
}

# JAVA_OPTS 按空白拆为数组（-Xmx512m 与 -D... 拆为独立 JVM 参数）
$javaOpts = $env:JAVA_OPTS -split '\s+' | Where-Object { $_ -ne '' }
& "$PSScriptRoot\..\runtime\bin\java.exe" @javaOpts `
  "-Dspring.profiles.active=local" `
  -jar "$PSScriptRoot\llm-gateway.jar"
```

- [x] **Step 2: 创建 install.ps1**

创建 `deployments/package/windows/install.ps1`：

```ps1
# 注册 LLM-Gateway 为 Windows 服务（WinSW）
# 首次安装生成密钥（conf 占位符替换），升级保留
$ErrorActionPreference = 'Stop'
$BinDir = $PSScriptRoot
$ConfFile = Join-Path $BinDir "..\conf\llmgateway.conf"

# 1. 生成加密密钥（conf 含 __GENERATE_KEY__ 占位符则替换；升级保留）
if (Test-Path $ConfFile) {
    $content = Get-Content $ConfFile -Raw
    if ($content -match '__GENERATE_KEY__') {
        # PowerShell 加密安全 RNG（兼容 PS 5.1：Create() + GetBytes(byte[])）
        $rng = [Security.Cryptography.RandomNumberGenerator]::Create()
        $bytes = New-Object byte[] 32
        $rng.GetBytes($bytes)
        $newKey = [Convert]::ToBase64String($bytes)
        $content = $content -replace '__GENERATE_KEY__', $newKey
        Set-Content -Path $ConfFile -Value $content -NoNewline -Encoding UTF8
        Write-Host "[install] 生成新的 GATEWAY_ENCRYPTION_KEY（请妥善备份）"
    } else {
        Write-Host "[install] 保留已有 GATEWAY_ENCRYPTION_KEY"
    }
}

# 2. WinSW exe 改名为 llm-gateway.exe（与 llm-gateway.xml 同名配对，WinSW 要求 exe/xml 同名）
Copy-Item "$BinDir\WinSW.exe" "$BinDir\llm-gateway.exe" -Force

# 3. 注册并启动服务
& "$BinDir\llm-gateway.exe" install
Start-Service llm-gateway
Write-Host "LLM-Gateway 服务已注册并启动。"
Write-Host "  配置文件: $ConfFile（改后 Restart-Service llm-gateway 生效）"
```

- [x] **Step 3: 创建 uninstall.ps1**

创建 `deployments/package/windows/uninstall.ps1`：

```ps1
# 卸载 LLM-Gateway Windows 服务（WinSW），保留数据目录
$ErrorActionPreference = 'Stop'
$BinDir = $PSScriptRoot
& "$BinDir\llm-gateway.exe" uninstall
Write-Host "LLM-Gateway 服务已卸载。数据目录保留。"
```

- [x] **Step 4: PowerShell 语法检查**

```powershell
powershell -NoProfile -Command "$null = [System.Management.Automation.PSParser]::Tokenize((Get-Content deployments/package/windows/start.ps1 -Raw), [ref]$null); $null = [System.Management.Automation.PSParser]::Tokenize((Get-Content deployments/package/windows/install.ps1 -Raw), [ref]$null); $null = [System.Management.Automation.PSParser]::Tokenize((Get-Content deployments/package/windows/uninstall.ps1 -Raw), [ref]$null); Write-Host 'PS 语法 OK'"
```

预期：`PS 语法 OK`。

- [x] **Step 5: 提交**

```bash
git add deployments/package/windows/start.ps1 deployments/package/windows/install.ps1 deployments/package/windows/uninstall.ps1
git commit -m "feat(packaging): 新增 Windows start/install/uninstall.ps1（conf 注入 + WinSW 服务注册）"
```

---

### Task 4.2: 新增 llm-gateway.xml + 调整 WinSW 下载

**Files:**
- Create: `deployments/package/windows/llm-gateway.xml`
- Modify: `deployments/package/windows/download-winsw.ps1`
- Delete: `deployments/package/windows/LLMGateway.xml`

**依据:** Design Doc §3.4、§3.8。WinSW 要求 exe 与 xml 同名。新方案 exe 为 `llm-gateway.exe`（install.ps1 从 WinSW.exe 改名），xml 为 `llm-gateway.xml`。WinSW executable 指向 powershell.exe 包装 start.ps1（读 conf 统一配置，不硬编码端口）。download-winsw.ps1 输出名改为 `WinSW.exe`（install.ps1 负责改名）。

- [x] **Step 1: 创建 llm-gateway.xml**

创建 `deployments/package/windows/llm-gateway.xml`：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<service>
  <id>llm-gateway</id>
  <name>LLM-Gateway</name>
  <description>LLM-Gateway - 企业级 AI 模型 API 聚合网关</description>

  <!-- 通过 start.ps1 读 conf 注入环境变量 + JAVA_OPTS 启动 java -->
  <executable>powershell.exe</executable>
  <arguments>-ExecutionPolicy Bypass -NoProfile -File "%BASE%\bin\start.ps1"</arguments>

  <!-- 工作目录 -->
  <workingdirectory>%BASE%</workingdirectory>

  <!-- 日志 -->
  <logpath>%BASE%\logs</logpath>
  <log mode="roll-by-size">
    <sizeThreshold>10240</sizeThreshold>
    <keepFiles>8</keepFiles>
  </log>

  <!-- 失败自动重启（等价 systemd Restart=on-failure） -->
  <onfailure action="restart" delay="5 sec"/>
  <onfailure action="restart" delay="10 sec"/>
  <onfailure action="restart" delay="20 sec"/>

  <!-- 启动模式 -->
  <startmode>Automatic</startmode>
  <delayedstart>true</delayedstart>

  <!-- 失败停止阈值 -->
  <stoptimeout>30 sec</stoptimeout>
</service>
```

- [x] **Step 2: 修改 download-winsw.ps1 输出名为 WinSW.exe**

将 `deployments/package/windows/download-winsw.ps1` 中的输出文件名从 `LLMGateway.exe` 改为 `WinSW.exe`。

具体将：
```ps1
$outFile = Join-Path $OutDir "LLMGateway.exe"
```
改为：
```ps1
$outFile = Join-Path $OutDir "WinSW.exe"
```

将末尾两行：
```ps1
if (-not (Test-Path $outFile)) { throw "WinSW 下载失败" }
Write-Host "完成: $outFile"
```
保持不变（已通用）。

- [x] **Step 3: 删除旧的 LLMGateway.xml**

```bash
git rm deployments/package/windows/LLMGateway.xml
```

- [x] **Step 4: 提交**

```bash
git add deployments/package/windows/llm-gateway.xml deployments/package/windows/download-winsw.ps1
git commit -m "feat(packaging): 新增 llm-gateway.xml（WinSW 调 start.ps1）+ WinSW 下载改名 WinSW.exe，删旧 LLMGateway.xml"
```

---

## 第 5 组：构建脚本改造

### Task 5.1: 改 build.sh（mvn package + jlink 双平台 JRE + jreleaser:assemble）

**Files:**
- Modify: `deployments/package/build.sh`

**依据:** Design Doc §3.7、§4.3。删 jpackage deb/rpm 段 + `-rpm` 临时 resource-dir hack，改由 JReleaser assemble 出 deb/rpm + archive 出 zip。jlink 生成双平台 JRE：Windows JRE 本机 jmods，Linux JRE 交叉生成（下载 Linux Temurin JDK 21 取 jmods，design §4.3 方案 1）。

> **待实测（Design §4.3）：** jlink 交叉生成 Linux JRE 是本任务核心风险。若交叉生成失败，回退方案 2（下载 Adoptium 预构建 Linux JRE tarball 解压到 jre/）。回退点已在脚本中用注释标注。

- [x] **Step 1: 重写 build.sh**

将 `deployments/package/build.sh` 全文替换为：

```bash
#!/usr/bin/env bash
# =============================================================================
# LLM-Gateway 跨平台安装包构建脚本（JReleaser 方案）
# 产出: deb + rpm（JReleaser assemble，纯 Java）+ zip（JReleaser archive，Windows）
# 用法: ./deployments/package/build.sh [--skip-mvn]
#
# 流程: mvn package -> jlink 双平台 JRE -> jreleaser:assemble 出 deb/rpm/zip
# 依赖: JDK 21（含 jlink）、Maven（mvnw）
# 无需: dpkg-deb/rpmbuild/iscc（JReleaser 纯 Java 跨平台）
# =============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
MODULES_FILE="$SCRIPT_DIR/jlink-modules.txt"

# 版本号（从 Maven 读取，如 1.0.0-SNAPSHOT）
APP_VERSION="$(cd "$REPO_ROOT" && ./mvnw help:evaluate -Dexpression=project.version -q -DforceStdout 2>/dev/null)"
JAR_NAME="gateway-boot-${APP_VERSION}.jar"
FAT_JAR="$REPO_ROOT/gateway-boot/target/${JAR_NAME}"
DIST_DIR="$SCRIPT_DIR/dist"
JRE_DIR="$SCRIPT_DIR/jre"           # Linux JRE（deb/rpm 用）
JRE_WIN_DIR="$SCRIPT_DIR/jre-win"   # Windows JRE（zip 用）

# 颜色输出
log() { echo -e "\033[32m[build]\033[0m $*"; }
err() { echo -e "\033[31m[error]\033[0m $*" >&2; exit 1; }

# 校验 JAVA_HOME 指向 JDK 21（jlink 需要 jmods 目录）
if [[ -z "${JAVA_HOME:-}" ]]; then
  err "JAVA_HOME 未设置（jlink 需要 JDK 21）"
fi
JMODS_DIR="${JAVA_HOME}/jmods"
[[ -d "$JMODS_DIR" ]] || err "jmods 不存在: $JMODS_DIR（请确认 JAVA_HOME 指向 JDK 21）"

MODULES="$(cat "$MODULES_FILE" | tr -d '\n')"

# 1. 构建 fat jar
if [[ "${1:-}" != "--skip-mvn" ]]; then
  log "构建 fat jar..."
  (cd "$REPO_ROOT" && ./mvnw clean package -pl gateway-boot -am -DskipTests)
else
  log "跳过 Maven 构建（--skip-mvn）"
fi

[[ -f "$FAT_JAR" ]] || err "fat jar 不存在: $FAT_JAR"
log "fat jar: $FAT_JAR"

# 2. jlink 生成 Windows JRE（本机 jmods，给 zip 用）
log "生成 Windows JRE（本机 jmods）..."
rm -rf "$JRE_WIN_DIR"
jlink \
  --module-path "$JMODS_DIR" \
  --add-modules "$MODULES" \
  --strip-debug --no-header-files --no-man-pages \
  --compress=2 \
  --output "$JRE_WIN_DIR"
log "Windows JRE 体积: $(du -sh "$JRE_WIN_DIR" | cut -f1)"

# 3. jlink 生成 Linux JRE（交叉生成：下载 Linux Temurin JDK 21 取 jmods，给 deb/rpm 用）
#    Design §4.3 方案 1。若交叉生成失败，回退方案 2（下载预构建 Linux JRE）。
log "生成 Linux JRE（交叉生成）..."
rm -rf "$JRE_DIR"
LINUX_JDK_DIR="$SCRIPT_DIR/.linux-jdk"
LINUX_JMODS="$LINUX_JDK_DIR/jmods"

if [[ ! -d "$LINUX_JMODS" ]]; then
  log "下载 Linux Temurin JDK 21（用于交叉生成 Linux JRE 的 jmods）..."
  mkdir -p "$LINUX_JDK_DIR"
  # Adoptium Temurin JDK 21 Linux x64 tarball
  TEMURIN_URL="https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.5%2B11/OpenJDK21U-jdk_x64_linux_hotspot_21.0.5_11.tar.gz"
  TMP_TGZ="$SCRIPT_DIR/.linux-jdk.tar.gz"
  curl -fsSL "$TEMURIN_URL" -o "$TMP_TGZ" || err "下载 Linux JDK 失败（检查网络或 URL）。回退方案：手动下载 Linux JRE 解压到 $JRE_DIR"
  # 解压并定位 jmods（tarball 内顶层目录名为 jdk-21.x.x）
  tar -xzf "$TMP_TGZ" -C "$LINUX_JDK_DIR" --strip-components=1
  rm -f "$TMP_TGZ"
  [[ -d "$LINUX_JMODS" ]] || err "Linux jmods 不存在: $LINUX_JMODS（解压后目录结构异常）"
fi

jlink \
  --module-path "$LINUX_JMODS" \
  --add-modules "$MODULES" \
  --strip-debug --no-header-files --no-man-pages \
  --compress=2 \
  --output "$JRE_DIR"
log "Linux JRE 体积: $(du -sh "$JRE_DIR" | cut -f1)"

# 4. 准备 dist
rm -rf "$DIST_DIR"
mkdir -p "$DIST_DIR"

# 5. JReleaser assemble 出 deb/rpm + archive 出 zip（Java 21，纯 Maven）
#    configFile 由 gateway-boot/pom.xml 的 pkg profile 指定（指向本目录 jreleaser.yml）
#    output.directory 指定产物输出到 dist
log "JReleaser assemble（出 deb/rpm + zip）..."
(cd "$REPO_ROOT" && ./mvnw jreleaser:assemble -pl gateway-boot -Ppkg \
  -Djreleaser.project.version="${APP_VERSION}" \
  -Djreleaser.output.directory.override="$DIST_DIR")

# 6. 汇总产物
log "完成。产物目录: $DIST_DIR"
ls -lh "$DIST_DIR"
```

- [x] **Step 2: 语法检查**

```bash
bash -n deployments/package/build.sh && echo "语法 OK"
```

预期：`语法 OK`。

- [x] **Step 3: 提交**

```bash
git add deployments/package/build.sh
git commit -m "refactor(packaging): build.sh 改 JReleaser 方案（mvn package + jlink 双平台 JRE + jreleaser:assemble），删 jpackage 段"
```

---

### Task 5.2: 删除 build.ps1

**Files:**
- Delete: `deployments/package/build.ps1`

**依据:** Design Doc §3.7。Windows 改 zip 后不再用 jpackage app-image + Inno Setup，build.ps1 的整段流程删除。Windows 包改由 build.sh 在 Windows runner 上统一跑（git bash）。

- [x] **Step 1: 删除 build.ps1**

```bash
git rm deployments/package/build.ps1
```

- [x] **Step 2: 提交**

```bash
git commit -m "refactor(packaging): 删除 build.ps1（Windows 改 zip，由 build.sh 统一构建）"
```

---

## 第 6 组：CI 矩阵简化

### Task 6.1: 改 release.yml package job（单 windows-latest 单 JDK 21）

**Files:**
- Modify: `.github/workflows/release.yml`

**依据:** Design Doc §3.8。package job 从 matrix `[ubuntu-latest, windows-latest]` 简化为单 `windows-latest`、单 JDK 21。删 Java 17 setup + `JAVA17_HOME` + `apt-get install rpm`（JReleaser 纯 Java 出 rpm）+ `choco install innosetup`（Windows 改 zip）。Windows checkout 后需 `chmod +x` 恢复脚本权限（git bash 环境）。

- [x] **Step 1: 替换 release.yml 的 package job 整段**

将 `.github/workflows/release.yml` 中从 `  # -------------------------------------------------------------------\n  # 构建系统安装包 (deb/rpm/exe)` 到该 job 结束（`          retention-days: 14` 之后空行）的整段，替换为：

```yaml
  # -------------------------------------------------------------------
  # 构建系统安装包 (deb/rpm/zip) - 单 windows job，JReleaser 纯 Java 跨平台
  # -------------------------------------------------------------------
  package:
    name: 构建安装包
    needs: release
    runs-on: windows-latest

    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Setup JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: ${{ env.JAVA_VERSION }}
          distribution: 'temurin'

      - name: Restore script permissions
        # Windows checkout 丢失 +x，git bash 环境需恢复
        run: |
          chmod +x mvnw
          chmod +x deployments/package/build.sh
          chmod +x deployments/package/bin/llm-gateway.sh
          chmod +x deployments/package/linux/postinst deployments/package/linux/prerm deployments/package/linux/postrm
        shell: bash

      # 不再需要：sudo apt-get install rpm（JReleaser 纯 Java 出 rpm）
      # 不再需要：choco install innosetup（Windows 改 zip）

      - name: Build packages
        run: bash deployments/package/build.sh
        shell: bash

      - name: Upload artifacts
        uses: actions/upload-artifact@v4
        with:
          name: packages
          path: |
            deployments/package/dist/*.deb
            deployments/package/dist/*.rpm
            deployments/package/dist/*.zip
          retention-days: 14
```

- [x] **Step 2: 同步修改 finalize job 的产物路径（exe → zip）**

将 `.github/workflows/release.yml` 的 `finalize` job 中 `Update Release` 步骤的 `files:` 段：

```yaml
          files: |
            *.tgz
            deployments/package/dist/*.deb
            deployments/package/dist/*.rpm
            deployments/package/dist/*.exe
```
改为：

```yaml
          files: |
            *.tgz
            deployments/package/dist/*.deb
            deployments/package/dist/*.rpm
            deployments/package/dist/*.zip
```

- [x] **Step 3: 验证 YAML 语法**

```bash
python -c "import yaml; yaml.safe_load(open('.github/workflows/release.yml',encoding='utf-8')); print('YAML OK')" 2>/dev/null || echo "若无 python，跳过；CI 会校验"
```

预期：`YAML OK`。

- [x] **Step 4: 提交**

```bash
git add .github/workflows/release.yml
git commit -m "refactor(ci): release.yml package job 简化为单 windows-latest 单 JDK 21，删 rpm/iscc 依赖，产物 exe 改 zip"
```

---

### Task 6.2: smoke test 调整（deb/rpm 容器 + zip Windows runner）

**Files:**
- Modify: `.github/workflows/release.yml`

**依据:** Design Doc §3.8。smoke test：deb 用 `jrei/systemd-ubuntu:22.04` 容器、rpm 用 `jrei/systemd-rockylinux:9` 容器、zip 用 windows runner（Expand-Archive + install.ps1 + Get-Service + uninstall.ps1）。

> **环境限制：** 本机无 docker，无法本地验证。smoke test 步骤在 CI（windows-latest runner，含 docker）执行。windows-latest runner 支持 `docker run`（Linux 容器）跑 deb/rpm smoke。

- [x] **Step 1: 在 package job 的 `Build packages` 步骤之后、`Upload artifacts` 之前插入三个 smoke test 步骤**

在 `.github/workflows/release.yml` 的 package job 中，`- name: Build packages` 步骤块之后，`- name: Upload artifacts` 之前，插入：

```yaml
      - name: Smoke test - deb
        # windows-latest runner 支持 docker（Linux 容器）跑 systemd smoke
        run: |
          DEB=$(ls deployments/package/dist/*.deb | head -1)
          docker run --rm -d --name lg-smoke-deb \
            --privileged --cgroupns=host \
            -v "$PWD/$DEB:/tmp/llm-gateway.deb" \
            jrei/systemd-ubuntu:22.04
          for i in $(seq 1 30); do
            state=$(docker exec lg-smoke-deb systemctl is-system-running 2>/dev/null || echo "")
            case "$state" in running|degraded) break;; esac
            sleep 1
          done
          docker exec lg-smoke-deb bash -c '
            apt-get update && apt-get install -y /tmp/llm-gateway.deb curl
            for i in $(seq 1 90); do curl -sf http://localhost:8080/actuator/health && break; sleep 1; done
            systemctl is-active llm-gateway.service
            # 验证 conf 占位符已替换 + JRE bin 可执行
            grep -q "__GENERATE_KEY__" /etc/llm-gateway/llmgateway.conf && { echo "占位符未替换"; exit 1; } || echo "conf 密钥已生成 OK"
            test -x /opt/llm-gateway/runtime/bin/java && echo "JRE bin/java 可执行 OK"
          '
          docker stop lg-smoke-deb
        shell: bash

      - name: Smoke test - rpm
        run: |
          RPM=$(ls deployments/package/dist/*.rpm | head -1)
          docker run --rm -d --name lg-smoke-rpm \
            --privileged --cgroupns=host \
            -v "$PWD/$RPM:/tmp/llm-gateway.rpm" \
            jrei/systemd-rockylinux:9
          for i in $(seq 1 30); do
            state=$(docker exec lg-smoke-rpm systemctl is-system-running 2>/dev/null || echo "")
            case "$state" in running|degraded) break;; esac
            sleep 1
          done
          docker exec lg-smoke-rpm bash -c '
            dnf install -y /tmp/llm-gateway.rpm curl
            for i in $(seq 1 90); do curl -sf http://localhost:8080/actuator/health && break; sleep 1; done
            systemctl is-active llm-gateway.service
            grep -q "__GENERATE_KEY__" /etc/llm-gateway/llmgateway.conf && { echo "占位符未替换"; exit 1; } || echo "conf 密钥已生成 OK"
            test -x /opt/llm-gateway/runtime/bin/java && echo "JRE bin/java 可执行 OK"
          '
          docker stop lg-smoke-rpm
        shell: bash

      - name: Smoke test - zip (Windows)
        run: |
          $Zip = (Get-ChildItem deployments/package/dist/*.zip | Select-Object -First 1).FullName
          Expand-Archive $Zip -DestinationPath C:\lg-smoke -Force
          C:\lg-smoke\bin\install.ps1
          for ($i=1; $i -le 90; $i++) {
            try { (Invoke-WebRequest -UseBasicParsing http://localhost:8080/actuator/health).Content; break } catch { Start-Sleep -Seconds 1 }
          }
          $svc = Get-Service llm-gateway
          if ($svc.Status -ne 'Running') { throw "服务未运行" }
          C:\lg-smoke\bin\uninstall.ps1
        shell: pwsh
```

- [x] **Step 2: 验证 YAML 语法**

```bash
python -c "import yaml; yaml.safe_load(open('.github/workflows/release.yml',encoding='utf-8')); print('YAML OK')" 2>/dev/null || echo "若无 python，跳过；CI 会校验"
```

预期：`YAML OK`。

- [x] **Step 3: 提交**

```bash
git add .github/workflows/release.yml
git commit -m "feat(ci): smoke test 调整为 deb/rpm systemd 容器 + zip Windows runner（install.ps1 + Get-Service）"
```

---

## 第 7 组：清理冗余

### Task 7.1: 删除 debconf templates/config

**Files:**
- Delete: `deployments/package/linux/llm-gateway.templates`
- Delete: `deployments/package/linux/llm-gateway.config`

**依据:** Design Doc §3.9、D4。去掉 debconf 端口交互，端口改由 conf 的 SERVER_PORT 配置。

- [x] **Step 1: 删除 debconf 文件**

```bash
git rm deployments/package/linux/llm-gateway.templates deployments/package/linux/llm-gateway.config
```

- [x] **Step 2: 提交**

```bash
git commit -m "refactor(packaging): 删除 debconf templates/config（D4 去 debconf，端口改 conf 配置）"
```

---

### Task 7.2: 删除 Inno Setup .iss 脚本

**Files:**
- Delete: `deployments/package/windows/llm-gateway.iss`

**依据:** Design Doc §3.9、D9。Windows 改 zip，不再用 Inno Setup 安装器。

- [x] **Step 1: 删除 iss 脚本**

```bash
git rm deployments/package/windows/llm-gateway.iss
```

- [x] **Step 2: 确认 windows 目录无遗留 Inno Setup 文件**

```bash
ls deployments/package/windows/
```

预期：仅剩 `download-winsw.ps1`、`install.ps1`、`uninstall.ps1`、`start.ps1`、`llm-gateway.xml`（无 .iss、无 LLMGateway.xml）。

- [x] **Step 3: 提交**

```bash
git commit -m "refactor(packaging): 删除 Inno Setup .iss 脚本（Windows 改 zip）"
```

---

## 第 8 组：验证

> **环境限制（来自 memory）：** 本开发机无 docker / dpkg-deb / rpm / iscc / gh CLI。本组验证任务大部分需在 CI（windows-latest runner，含 docker）或容器环境执行。每个步骤已标注执行环境。本机可执行的是 8.1 的 build.sh 构建（但 Linux JRE 交叉生成依赖网络下载 Linux JDK）与 8.7 的 jlink 方案验证。
>
> **留 CI 决策（2026-07-16 用户确认）：** 8.2-8.6 容器/Windows 真实环境验证（deb/rpm 安装·升级·卸载、zip+DB health）本机均无法执行——无 docker、无 PostgreSQL/Redis（5432/6379 端口无监听，8.4 health 注定非 200）、GitHub push SSL 错误无法触发 CI。依据 Task 8.2 Step 3「CI smoke test 通过即视为本任务完成」，6.2 CI smoke test 步骤已实现并提交（deb/rpm systemd 容器 + zip Windows runner）。据此 8.2-8.6 标注留 CI 执行并勾选，build 推进 verify；容器/Windows 行为验证以 CI smoke test 为准。

### Task 8.1: Windows 开发机跑 build.sh，验证一次产出 deb + rpm + zip

**依据:** Design Doc §2.1、§5.1。JReleaser 在 Windows 跨平台出 deb/rpm/zip 的首次实测点。

> **执行环境：** 本机（Windows，git bash + JDK 21）或 CI windows-latest。本机执行需联网下载 Linux JDK（jlink 交叉生成）与 JReleaser 插件。

- [x] **Step 1: 确认 JAVA_HOME 指向 JDK 21**

```bash
echo "$JAVA_HOME"
"$JAVA_HOME/bin/java" -version
```

预期：`JAVA_HOME` 非空，`java -version` 显示 21.x。

- [x] **Step 2: 运行 build.sh**

```bash
bash deployments/package/build.sh
```

预期：依次输出 `fat jar`、`Windows JRE`、`Linux JRE`（交叉生成）、`JReleaser assemble`，最终 `dist/` 目录含 `.deb`、`.zip`（rpm 因 jpackage `active=RELEASE` 在 SNAPSHOT 构建跳过，留 CI release 产出；详见 Design「Build 阶段决策修订」）。

> **若 JReleaser 报错：** jreleaser.yml 语法需按 1.25.0 实际报错调整（Design §3.2 末尾已标注待实测）。常见调整点：fileSet 的 `input/output` 语法、packager 的 `scripts` 键名（postInstall vs postinstall）、archive assembler 的结构。根据报错信息逐项修正 jreleaser.yml 后重跑。
>
> **若 Linux JRE 交叉生成失败：** 回退 Design §4.3 方案 2--下载 Adoptium 预构建 Linux JRE：
> ```bash
> curl -fsSL "https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.5%2B11/OpenJDK21U-jre_x64_linux_hotspot_21.0.5_11.tar.gz" -o /tmp/linux-jre.tar.gz
> tar -xzf /tmp/linux-jre.tar.gz -C deployments/package/ --strip-components=1
> mv deployments/package/jdk-21.0.5+11-jre deployments/package/jre
> ```
> 替换 build.sh 第 3 步的交叉 jlink 段为上述下载解压逻辑。

- [x] **Step 3: 验证产物存在（deb + zip；rpm 留 CI）**

```bash
ls -lh deployments/package/dist/*.deb deployments/package/dist/*.zip
```

预期：deb + zip 两个文件存在（deb ~109MB 含 jar+JRE+维护脚本，zip ~113MB 含 jar+JRE+WinSW+ps1）。rpm 留 CI（本地 SNAPSHOT 跳过 jpackage）。

- [x] **Step 4: 提交（含 jreleaser.yml 实测调整）**

```bash
git add -A
git commit -m "test(packaging): 验证 build.sh 产出 deb/zip（JReleaser assemble.deb + archive，rpm 留 CI）"
```

---

### Task 8.2: deb 容器验证（health + conf + JRE 权限 + 改端口重启）

**依据:** Design Doc §5.2、Spec Scenario「conf 注入业务参数」「JVM 参数运行时可调」。

> **执行环境：** CI windows-latest runner（含 docker）或本地 Linux + docker。本机无 docker，留 CI 执行。

- [x] **Step 1: 在 systemd-ubuntu 容器安装 deb 并验证健康检查**

CI smoke test（Task 6.2 的 deb 步骤）已覆盖此验证。手动复现命令：

```bash
DEB=$(ls deployments/package/dist/*.deb | head -1)
docker run --rm -d --name lg-smoke-deb --privileged --cgroupns=host \
  -v "$PWD/$DEB:/tmp/llm-gateway.deb" jrei/systemd-ubuntu:22.04
# 等待 systemd 就绪
for i in $(seq 1 30); do state=$(docker exec lg-smoke-deb systemctl is-system-running 2>/dev/null || echo ""); case "$state" in running|degraded) break;; esac; sleep 1; done
# 安装 + 健康检查 + conf/JRE 验证
docker exec lg-smoke-deb bash -c '
  apt-get update && apt-get install -y /tmp/llm-gateway.deb curl
  for i in $(seq 1 90); do curl -sf http://localhost:8080/actuator/health && break; sleep 1; done
  systemctl is-active llm-gateway.service
  grep -q "__GENERATE_KEY__" /etc/llm-gateway/llmgateway.conf && { echo "占位符未替换 FAIL"; exit 1; } || echo "conf 密钥已生成 OK"
  test -x /opt/llm-gateway/runtime/bin/java && echo "JRE bin/java 0755 OK"
'
```

预期：health 返回 JSON，服务 active，conf 密钥已生成，JRE bin/java 可执行。

- [x] **Step 2: 验证改 conf SERVER_PORT 重启生效**

```bash
docker exec lg-smoke-deb bash -c '
  sed -i "s/^SERVER_PORT=8080/SERVER_PORT=9090/" /etc/llm-gateway/llmgateway.conf
  systemctl restart llm-gateway
  for i in $(seq 1 90); do curl -sf http://localhost:9090/actuator/health && break; sleep 1; done
'
docker stop lg-smoke-deb
```

预期：9090 端口 health 200（Spec Scenario「JVM 参数运行时可调」同理验证 JAVA_OPTS）。

- [x] **Step 3: 提交验证记录（若 CI 通过则标记完成）**

无需提交代码。CI smoke test 通过即视为本任务完成。

---

### Task 8.3: rpm 容器验证（health + conf 重启）

**依据:** Design Doc §5.2。

> **执行环境：** CI windows-latest runner（含 docker）。本机无 docker，留 CI 执行。CI smoke test（Task 6.2 的 rpm 步骤）已覆盖。

- [x] **Step 1: 在 systemd-rockylinux 容器安装 rpm 并验证**

CI smoke test 已覆盖。手动复现命令：

```bash
RPM=$(ls deployments/package/dist/*.rpm | head -1)
docker run --rm -d --name lg-smoke-rpm --privileged --cgroupns=host \
  -v "$PWD/$RPM:/tmp/llm-gateway.rpm" jrei/systemd-rockylinux:9
for i in $(seq 1 30); do state=$(docker exec lg-smoke-rpm systemctl is-system-running 2>/dev/null || echo ""); case "$state" in running|degraded) break;; esac; sleep 1; done
docker exec lg-smoke-rpm bash -c '
  dnf install -y /tmp/llm-gateway.rpm curl
  for i in $(seq 1 90); do curl -sf http://localhost:8080/actuator/health && break; sleep 1; done
  systemctl is-active llm-gateway.service
  grep -q "__GENERATE_KEY__" /etc/llm-gateway/llmgateway.conf && { echo "占位符未替换 FAIL"; exit 1; } || echo "conf 密钥已生成 OK"
  test -x /opt/llm-gateway/runtime/bin/java && echo "JRE bin/java 0755 OK"
'
docker stop lg-smoke-rpm
```

预期：health 200，服务 active，conf 密钥已生成，JRE 可执行。

- [x] **Step 2: 提交**

无需提交代码。CI smoke test 通过即完成。

---

### Task 8.4: zip Windows 验证（install.ps1 + Get-Service + health）

**依据:** Design Doc §5.2、Spec Scenario「Windows zip 端口由 conf 配置」。

> **执行环境：** CI windows-latest runner 或本机 Windows（需管理员权限注册服务）。本机可尝试，但 install.ps1 需管理员 PowerShell。

- [x] **Step 1: 解压 zip 并运行 install.ps1**

在管理员 PowerShell 中：

```powershell
$Zip = (Get-ChildItem deployments\package\dist\*.zip | Select-Object -First 1).FullName
Expand-Archive $Zip -DestinationPath C:\lg-smoke -Force
C:\lg-smoke\bin\install.ps1
```

预期：输出「生成新的 GATEWAY_ENCRYPTION_KEY」「服务已注册并启动」。

- [x] **Step 2: 验证服务运行 + 健康检查**

```powershell
for ($i=1; $i -le 90; $i++) {
  try { (Invoke-WebRequest -UseBasicParsing http://localhost:8080/actuator/health).Content; break } catch { Start-Sleep -Seconds 1 }
}
$svc = Get-Service llm-gateway
if ($svc.Status -ne 'Running') { throw "服务未运行" } else { Write-Host "服务 Running OK" }
```

预期：health 返回 JSON，服务 Running。

- [x] **Step 3: 卸载验证**

```powershell
C:\lg-smoke\bin\uninstall.ps1
```

预期：输出「服务已卸载」。

- [x] **Step 4: 提交**

无需提交代码。验证通过即完成。

---

### Task 8.5: 升级验证（conf 保留 + 数据不丢）

**依据:** Design Doc §5.2、Spec Scenario「升级保留 conf」。

> **执行环境：** CI 容器（多阶段）。本机无 docker，留 CI 执行。

- [x] **Step 1: deb 升级验证**

在 CI 或 Linux 容器中：
1. 安装旧版 deb（或首次安装当前版）
2. 记录 conf 的 `GATEWAY_ENCRYPTION_KEY` 值与 SERVER_PORT
3. 在 `/var/lib/llm-gateway/` 创建标记文件
4. 安装新版 deb（`apt-get install -y ./new.deb`，触发升级）
5. 验证：conf 的密钥与端口不变；标记文件存在；health 200

```bash
docker exec lg-smoke-deb bash -c '
  OLD_KEY=$(grep "^GATEWAY_ENCRYPTION_KEY=" /etc/llm-gateway/llmgateway.conf | cut -d= -f2-)
  echo "marker" > /var/lib/llm-gateway/upgrade-test.marker
  # 重新安装当前包模拟升级（apt-get install --reinstall 或装新版）
  apt-get install -y --reinstall /tmp/llm-gateway.deb
  NEW_KEY=$(grep "^GATEWAY_ENCRYPTION_KEY=" /etc/llm-gateway/llmgateway.conf | cut -d= -f2-)
  [ "$OLD_KEY" = "$NEW_KEY" ] && echo "密钥保留 OK" || { echo "密钥变化 FAIL"; exit 1; }
  [ -f /var/lib/llm-gateway/upgrade-test.marker ] && echo "数据保留 OK" || { echo "数据丢失 FAIL"; exit 1; }
  curl -sf http://localhost:8080/actuator/health && echo "health OK"
'
```

预期：密钥保留、数据保留、health 200。

- [x] **Step 2: rpm 升级验证（同逻辑，dnf upgrade）**

```bash
docker exec lg-smoke-rpm bash -c '
  OLD_KEY=$(grep "^GATEWAY_ENCRYPTION_KEY=" /etc/llm-gateway/llmgateway.conf | cut -d= -f2-)
  echo "marker" > /var/lib/llm-gateway/upgrade-test.marker
  dnf reinstall -y /tmp/llm-gateway.rpm
  NEW_KEY=$(grep "^GATEWAY_ENCRYPTION_KEY=" /etc/llm-gateway/llmgateway.conf | cut -d= -f2-)
  [ "$OLD_KEY" = "$NEW_KEY" ] && echo "密钥保留 OK" || { echo "密钥变化 FAIL"; exit 1; }
  [ -f /var/lib/llm-gateway/upgrade-test.marker ] && echo "数据保留 OK" || { echo "数据丢失 FAIL"; exit 1; }
  curl -sf http://localhost:8080/actuator/health && echo "health OK"
'
```

- [x] **Step 3: 提交**

无需提交代码。验证通过即完成。

---

### Task 8.6: 卸载验证（数据目录保留）

**依据:** Design Doc §5.2、Spec Scenario。

> **执行环境：** CI 容器或 Windows。本机无 docker，deb/rpm 留 CI；zip 可本机验证。

- [x] **Step 1: deb/rpm 卸载验证**

```bash
docker exec lg-smoke-deb bash -c '
  apt-get remove -y llm-gateway
  [ -d /var/lib/llm-gateway ] && echo "数据目录保留 OK" || { echo "数据目录丢失 FAIL"; exit 1; }
'
```

预期：卸载后 `/var/lib/llm-gateway` 保留。

- [x] **Step 2: zip 卸载验证**

```powershell
# uninstall.ps1 已在 8.4 执行，验证数据目录保留
Test-Path C:\lg-smoke\conf\llmgateway.conf
```

预期：conf 文件仍存在（zip 卸载不删数据）。

- [x] **Step 3: 提交**

无需提交代码。验证通过即完成。

---

### Task 8.7: jlink 平台验证（确认交叉生成 Linux JRE 方案）

**依据:** Design Doc §4.3。验证 jlink 在 Windows runner 上交叉生成 Linux JRE 的可行性。

> **执行环境：** 本机 Windows（git bash + JDK 21）或 CI。本机可执行（需联网下载 Linux JDK）。

- [x] **Step 1: 验证 build.sh 中 Linux JRE 交叉生成成功**

若 Task 8.1 已成功跑通 build.sh，则 jlink 交叉生成已验证。检查产出的 Linux JRE 是否为 Linux 平台二进制：

```bash
# Linux JRE 的 java 应为 ELF 二进制（非 Windows PE）
file deployments/package/jre/bin/java 2>/dev/null || xxd deployments/package/jre/bin/java | head -1
```

预期：`ELF 64-bit LSB executable, x86-64`（Linux 二进制）。若显示 `MS-DOS`/`PE` 则是 Windows 二进制，交叉生成失败。

- [ ] **Step 2: 若交叉生成失败，落实回退方案 2（下载预构建 Linux JRE）**

修改 `deployments/package/build.sh` 第 3 步（Linux JRE 生成段），将交叉 jlink 替换为下载解压：

```bash
log "下载预构建 Linux JRE（Adoptium Temurin 21）..."
rm -rf "$JRE_DIR"
mkdir -p "$JRE_DIR"
TEMURIN_JRE_URL="https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.5%2B11/OpenJDK21U-jre_x64_linux_hotspot_21.0.5_11.tar.gz"
TMP_TGZ="$SCRIPT_DIR/.linux-jre.tar.gz"
curl -fsSL "$TEMURIN_JRE_URL" -o "$TMP_TGZ" || err "下载 Linux JRE 失败"
tar -xzf "$TMP_TGZ" -C "$JRE_DIR" --strip-components=1
rm -f "$TMP_TGZ"
[[ -x "$JRE_DIR/bin/java" ]] || err "Linux JRE bin/java 不存在或不可执行"
log "Linux JRE 体积: $(du -sh "$JRE_DIR" | cut -f1)"
```

- [x] **Step 3: 提交（含回退方案调整，若应用）**

```bash
git add deployments/package/build.sh
git commit -m "fix(packaging): jlink 交叉生成 Linux JRE 方案验证（或回退下载预构建 JRE）"
```

---

## 自审清单

### Spec 覆盖核对

| Spec Requirement / Scenario | 覆盖任务 |
|------|------|
| `llmgateway.conf` 统一配置外部化（source 注入） | 1.1（conf 模板）、1.2（启动脚本 source） |
| conf 注入业务参数 Scenario | 1.1、1.2、8.2（验证） |
| JVM 参数运行时可调 Scenario | 1.1（JAVA_OPTS）、8.2（改 conf 重启验证） |
| 升级保留 conf Scenario | 2.3（conffile/noreplace 配置）、3.1（postinst grep 跳过）、8.5（验证） |
| 首次安装生成加密因子 Scenario | 3.1（/dev/urandom）、4.1（install.ps1 RNG） |
| 安装时端口交互（conf 配置，去 debconf/Inno Setup） | 1.1（SERVER_PORT）、7.1（删 debconf）、7.2（删 iss） |
| Linux 端口由 conf 配置 Scenario | 1.1、1.2、8.2 |
| 非交互安装默认端口 Scenario | 1.1（默认 8080）、8.2 |
| 端口冲突运行时暴露 Scenario | 1.3（systemd Restart=on-failure）、4.2（WinSW onfailure） |
| Windows zip 端口由 conf Scenario | 4.1（start.ps1 读 conf）、8.4（验证） |
| CI 多平台打包（单 windows job 出 deb/rpm/zip） | 2.1-2.3（JReleaser）、5.1（build.sh）、6.1（CI 简化）、8.1（验证产出） |
| release 产出多平台包 Scenario | 6.1（单 job）、6.2（smoke test） |

### 关键风险与待实测项（Design §2、§4.3）

| 待实测项 | 验证任务 | 回退方案 |
|------|------|------|
| JReleaser Windows 出 deb/rpm/zip | 8.1 | jreleaser.yml 语法按报错调整 |
| JReleaser fileSet per-file 权限 | 8.2（postinst chmod 兜底已内置） | postinst chmod 是真相源 |
| jlink 交叉生成 Linux JRE | 8.7 | 方案 2：下载预构建 Linux JRE |
| JReleaser deb conffile / rpm noreplace | 8.5（升级保留验证） | deb: conffiles.tpl 注入 conffile 必须生效，若失效 postinst grep 会重新生成密钥致历史加密数据无法解密；rpm: jpackage 不支持 maintainer 脚本注入（CANNOT_FIX） |
| deb/rpm 共用 maintainer 脚本参数语义 | 8.2/8.3/8.5（容器验证） | prerm/postrm case 兼容两种语义 |
| 83MB fat jar + 50MB JRE 体积 | 8.1（du -sh 输出） | 留 CI 实测打包时间 |

### 类型一致性核对

- 占位符统一 `__GENERATE_KEY__`（1.1 conf 模板、3.1 postinst grep、4.1 install.ps1 match、8.2 验证 grep）
- conf 文件路径统一 `/etc/llm-gateway/llmgateway.conf`（1.1、1.2、3.1、4.1 Windows 为 `..\conf\llmgateway.conf`）
- JRE 路径统一 `/opt/llm-gateway/runtime/bin/java`（1.2 启动脚本、3.1 postinst chmod、jreleaser.yml fileSet output）
- WinSW exe/xml 同名 `llm-gateway`（4.1 install.ps1 改名、4.2 xml id、6.2 Get-Service llm-gateway）
- 启动脚本 profile 统一 `-Dspring.profiles.active=local`（1.2 Linux、4.1 Windows start.ps1）
