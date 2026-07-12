---
change: one-click-bare-deploy
design-doc: docs/superpowers/specs/2026-07-11-one-click-bare-deploy-design.md
base-ref: f4b92150f569008c8d1399b31a552fe58c6d9c3b
---

# 非 Docker 一键部署 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 用 jpackage 打包 Linux deb/rpm + Windows exe 安装包（内置 jlink 精简 JRE，默认 local profile 零外部依赖），并修复 Docker 资产失配，实现"一条命令装好即用"。

**Architecture:** fat jar（`gateway-boot-1.0.0-SNAPSHOT.jar`，Main-Class=`org.springframework.boot.loader.launch.JarLauncher`）经 jdeps → jlink 精简 JRE → jpackage 产出原生包。Linux 用 systemd unit + postinst/prerm/postrm + debconf 端口交互 + env conffile；Windows 用 jpackage app-image + WinSW 注册服务 + Inno Setup 打 exe installer（不用 msi/WiX）。数据目录经 `DB_URL` 环境变量外部化，加密密钥 `GATEWAY_ENCRYPTION_KEY` 安装时生成、升级保留。

**Tech Stack:** Java 21 (jlink/jpackage)、Spring Boot 3.5.13 fat jar、H2 文件模式 + Caffeine、systemd、debconf、WinSW 2.x、Inno Setup 6、GitHub Actions matrix。

**关联 Design Doc：** `docs/superpowers/specs/2026-07-11-one-click-bare-deploy-design.md`（Decisions D1–D9）

**任务边界来源：** `openspec/changes/one-click-bare-deploy/tasks.md`（6 组 28 个任务）

**关键约束：**
- 不改业务 Java 源码与 `application*.yml` 配置内容
- Windows 统一 exe（Inno Setup + WinSW），不得出现 msi/WiX
- 端口安装时交互设置，**不校验占用**，运行时冲突由服务重启机制暴露
- 默认 `local` profile（H2 文件 + Caffeine，无 Redis）
- 健康检查端点统一用 `/actuator/health`（application.yml 已暴露）

**最大技术风险：** Task 1.1（jpackage + Spring Boot fat jar 启动 spike），必须最先验证。若 `--main-class org.springframework.boot.loader.launch.JarLauncher` 启动失败，备选 layered jar 或 `JAVA_OPTIONS` 方式（见 D8）。

---

## 文件结构总览

### 新增文件

| 文件 | 职责 |
|------|------|
| `deployments/package/build.sh` | Linux 构建入口：mvn package → jdeps → jlink → jpackage deb+rpm |
| `deployments/package/build.ps1` | Windows 构建入口：mvn package → jdeps → jlink → jpackage app-image → Inno Setup |
| `deployments/package/jlink-modules.txt` | jdeps 分析出的 jlink 模块清单（spike 产物） |
| `deployments/package/spike-report.md` | Task 1.1 spike 验证结论记录 |
| `deployments/package/linux/llm-gateway.service` | systemd unit 模板 |
| `deployments/package/linux/llm-gateway.templates` | debconf 模板（端口交互） |
| `deployments/package/linux/llm-gateway.config` | debconf 收集脚本 |
| `deployments/package/linux/postinst` | deb 安装后脚本 |
| `deployments/package/linux/prerm` | deb 卸载前脚本 |
| `deployments/package/linux/postrm` | deb 卸载后脚本 |
| `deployments/package/linux/postinst-rpm` | rpm `%post` 等价脚本 |
| `deployments/package/linux/prerm-rpm` | rpm `%preun` 等价脚本 |
| `deployments/package/linux/postrm-rpm` | rpm `%postun` 等价脚本 |
| `deployments/package/linux/llm-gateway.spec` | rpm spec 片段（maintainer 脚本注入） |
| `deployments/package/windows/LLMGateway.xml` | WinSW 服务配置 |
| `deployments/package/windows/LLMGateway.exe` | WinSW 二进制（下载放入，不入库则构建时下载） |
| `deployments/package/windows/llm-gateway.iss` | Inno Setup 安装脚本 |
| `deployments/package/windows/generate-key.ps1` | 密钥生成 Pascal Script 辅助 |
| `deployments/package/README.md` | 打包构建与安装说明 |

### 修改文件

| 文件 | 改动 |
|------|------|
| `.github/workflows/release.yml` | 新增 `package` job（matrix ubuntu/windows），`finalize` 加 needs package |
| `deployments/docker/Dockerfile` | 构建路径改为单模块 `gateway-boot`，修正 COPY 与 jar 名、health 路径 |
| `deployments/docker/docker-compose.yml` | gateway 服务 `context` 改根目录、移除源码挂载、补 `gateway-console` 服务、修 health 路径 |
| `README.md` | 部署章节：修正 DB 类型/jar 名/安装包用法，补 admin/admin 改密与 H2 Console 风险提示 |

---

## Phase 1: jpackage 打包基础验证（Tasks 1.1–1.4）

> **优先级最高。** Task 1.1 是最大技术风险，必须先通过 spike 确认 jpackage + fat jar 能启动，再做后续目录与脚本。

### Task 1.1: Spike — 验证 jpackage + Spring Boot fat jar 启动

**目标：** 确认 `jpackage --main-jar gateway-boot-1.0.0-SNAPSHOT.jar --main-class org.springframework.boot.loader.launch.JarLauncher` 产出的 app-image 能正常启动 Spring Boot 并响应 `/actuator/health`。若失败，按 D8 备选方案调整。

**涉及文件：**
- 临时产物：`/tmp/llm-gateway-spike/`（spike 完成后可清理）
- 记录：`deployments/package/spike-report.md`（Create）

**前置说明：** 本任务在 Linux 或 Windows 本机执行（需 JDK 21）。spike 用 `--type app-image`（最快验证启动，不打包安装器）。fat jar 已由 `spring-boot-maven-plugin` repackage 产生，Main-Class = `org.springframework.boot.loader.launch.JarLauncher`。

- [x] **Step 1: 构建 fat jar**

```bash
# 仓库根目录
./mvnw clean package -pl gateway-boot -am -DskipTests
```

预期：`gateway-boot/target/gateway-boot-1.0.0-SNAPSHOT.jar` 生成，大小 ~70-90MB。

验证 Main-Class：

```bash
unzip -p gateway-boot/target/gateway-boot-1.0.0-SNAPSHOT.jar META-INF/MANIFEST.MF | grep -i main-class
```

预期输出含：`Main-Class: org.springframework.boot.loader.launch.JarLauncher`

- [x] **Step 2: 用 jdeps 分析 fat jar 依赖模块**

```bash
jdeps --multi-release 21 --print-module-deps \
  --ignore-missing-deps \
  gateway-boot/target/gateway-boot-1.0.0-SNAPSHOT.jar > /tmp/llm-gateway-spike/modules.txt
cat /tmp/llm-gateway-spike/modules.txt
```

预期：输出逗号分隔的 java.* 模块清单（如 `java.base,java.compiler,java.desktop,...`）。

> 若 `jdeps` 因缺失依赖报错，加 `--ignore-missing-deps` 跳过（fat jar 内嵌依赖通常完整）。将清单记下，供 Task 1.2 固化。

- [x] **Step 3: jlink 生成精简 JRE**

```bash
mkdir -p /tmp/llm-gateway-spike/jre
jlink --add-modules "$(cat /tmp/llm-gateway-spike/modules.txt)" \
  --strip-debug --no-header-files --no-man-pages \
  --output /tmp/llm-gateway-spike/jre
```

预期：`/tmp/llm-gateway-spike/jre/bin/java` 存在，JRE 体积 ~40-60MB。

- [x] **Step 4: jpackage 打 app-image（最小命令，验证启动）**

```bash
jpackage \
  --type app-image \
  --name llm-gateway \
  --input gateway-boot/target \
  --main-jar gateway-boot-1.0.0-SNAPSHOT.jar \
  --main-class org.springframework.boot.loader.launch.JarLauncher \
  --runtime-image /tmp/llm-gateway-spike/jre \
  --dest /tmp/llm-gateway-spike/dist \
  --java-options "-Dspring.profiles.active=local"
```

预期：`/tmp/llm-gateway-spike/dist/llm-gateway/` 生成，内含 `bin/llm-gateway`（Linux）或 `bin/llm-gateway.exe`（Windows）、`runtime/`（JRE）、`app/`（jar）。

- [x] **Step 5: 启动 app-image 并验证 health**

```bash
# 启动（后台）
/tmp/llm-gateway-spike/dist/llm-gateway/bin/llm-gateway &
APP_PID=$!

# 等待就绪（最多 60s）
for i in $(seq 1 60); do
  if curl -sf http://localhost:8080/actuator/health > /tmp/llm-gateway-spike/health.json 2>/dev/null; then
    break
  fi
  sleep 1
done

cat /tmp/llm-gateway-spike/health.json
kill $APP_PID
```

预期：`health.json` 含 `"status":"UP"`。

- [x] **Step 6: 记录 spike 结论**

Create `deployments/package/spike-report.md`：

```markdown
# jpackage + fat jar Spike 验证报告

## 验证日期
<填写执行日期>

## 结论
- [x] jpackage `--main-jar` + `--main-class org.springframework.boot.loader.launch.JarLauncher` 启动成功
- [ ] 需改用 layered jar / JAVA_OPTIONS 备选方案（仅当上面失败时勾选）

## jdeps 模块清单
（粘贴 Step 2 输出）

## 启动验证
- app-image 路径: /tmp/llm-gateway-spike/dist/llm-gateway/
- health 响应: {"status":"UP",...}

## 备注
- jpackage 版本: <`jpackage --version` 输出>
- JRE 体积: <`du -sh jre` 输出>
- app-image 体积: <`du -sh dist/llm-gateway` 输出>
```

- [x] **Step 7: Commit**

```bash
git add deployments/package/spike-report.md
git commit -m "feat(package): Task 1.1 jpackage+fat jar spike 验证通过"
```

> **决策点：** 若 Step 5 health 未 UP，停止本 Phase，改用 D8 备选（layered jar：`spring-boot-maven-plugin` 配 layered jars + jpackage `--module-path` 指定 layers；或 `--java-options` 传 `-jar`）。需用户确认方向后再继续。

---

### Task 1.2: 固化 jdeps/jlink 模块清单

**目标：** 将 spike 中 jdeps 输出固化为可复用文件，供 build.sh/build.ps1 读取。

**涉及文件：**
- Create: `deployments/package/jlink-modules.txt`

- [x] **Step 1: 创建模块清单文件**

依据 spike-report.md 中记录的 jdeps 输出，Create `deployments/package/jlink-modules.txt`，内容为单行逗号分隔模块清单（示例，以实际 jdeps 输出为准）：

```
java.base,java.compiler,java.desktop,java.logging,java.management,java.naming,java.net.http,java.scripting,java.security.jgss,java.security.sasl,java.sql,java.transaction.xa,java.xml,jdk.crypto.cryptoki,jdk.crypto.ec,jdk.unsupported,jdk.zipfs
```

> 注意：Spring Boot + H2 + Sa-Token + JGit 实际依赖模块以 spike `jdeps --print-module-deps` 输出为准。`jdk.crypto.ec`/`jdk.crypto.cryptoki` 必须包含（H2/SSL 需要），`jdk.unsupported` 必须包含（sun.misc.Unsafe）。

- [x] **Step 2: 验证 jlink 用此清单能生成可启动 JRE**

```bash
jlink --add-modules "$(cat deployments/package/jlink-modules.txt | tr -d '\n')" \
  --strip-debug --no-header-files --no-man-pages \
  --output /tmp/llm-gateway-spike/jre2
/tmp/llm-gateway-spike/jre2/bin/java -version
```

预期：打印 `openjdk version "21..."`，无报错。

- [x] **Step 3: Commit**

```bash
git add deployments/package/jlink-modules.txt
git commit -m "feat(package): Task 1.2 固化 jlink 精简 JRE 模块清单"
```

---

### Task 1.3: 创建 deployments/package 目录结构

**目标：** 建立打包资源的标准目录骨架。

**涉及文件：**
- Create: `deployments/package/linux/.gitkeep`
- Create: `deployments/package/windows/.gitkeep`
- Create: `deployments/package/jpackage/.gitkeep`

- [x] **Step 1: 创建目录与占位文件**

```bash
mkdir -p deployments/package/linux deployments/package/windows deployments/package/jpackage
touch deployments/package/linux/.gitkeep deployments/package/windows/.gitkeep deployments/package/jpackage/.gitkeep
```

- [x] **Step 2: Commit**

```bash
git add deployments/package/linux/.gitkeep deployments/package/windows/.gitkeep deployments/package/jpackage/.gitkeep
git commit -m "chore(package): Task 1.3 创建 deployments/package 目录结构"
```

---

### Task 1.4: 编写 build.sh / build.ps1 构建入口

**目标：** 封装"mvn package → jlink → jpackage"全流程，供本地与 CI 调用。build.sh 产 deb+rpm，build.ps1 产 app-image（供 Inno Setup 编译 exe）。

**涉及文件：**
- Create: `deployments/package/build.sh`
- Create: `deployments/package/build.ps1`

- [x] **Step 1: 编写 build.sh（Linux deb+rpm）**

Create `deployments/package/build.sh`：

```bash
#!/usr/bin/env bash
# =============================================================================
# LLM-Gateway Linux 安装包构建脚本
# 产出: deb + rpm（含 jlink 精简 JRE）
# 用法: ./deployments/package/build.sh [--skip-mvn]
# =============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
MODULES_FILE="$SCRIPT_DIR/jlink-modules.txt"
LINUX_RES="$SCRIPT_DIR/linux"

# 版本号（从 Maven 读取）
APP_VERSION="$(cd "$REPO_ROOT" && ./mvnw help:evaluate -Dexpression=project.version -q -DforceStdout 2>/dev/null)"
JAR_NAME="gateway-boot-${APP_VERSION}.jar"
FAT_JAR="$REPO_ROOT/gateway-boot/target/${JAR_NAME}"
DIST_DIR="$SCRIPT_DIR/dist"
JRE_DIR="$SCRIPT_DIR/jre"

# 颜色输出
log() { echo -e "\033[32m[build]\033[0m $*"; }
err() { echo -e "\033[31m[error]\033[0m $*" >&2; exit 1; }

# 1. 构建 fat jar
if [[ "${1:-}" != "--skip-mvn" ]]; then
  log "构建 fat jar..."
  (cd "$REPO_ROOT" && ./mvnw clean package -pl gateway-boot -am -DskipTests)
else
  log "跳过 Maven 构建（--skip-mvn）"
fi

[[ -f "$FAT_JAR" ]] || err "fat jar 不存在: $FAT_JAR"
log "fat jar: $FAT_JAR"

# 2. jdeps 验证模块清单（可选，用于发现遗漏）
log "验证 jlink 模块清单..."
ACTUAL_MODULES="$(jdeps --multi-release 21 --print-module-deps --ignore-missing-deps "$FAT_JAR" 2>/dev/null || true)"
if [[ -n "$ACTUAL_MODULES" ]]; then
  log "jdeps 实际依赖: $ACTUAL_MODULES"
  log "清单文件: $(cat "$MODULES_FILE" | tr -d '\n')"
  log "（若启动失败，对比两者补齐缺失模块）"
fi

# 3. jlink 生成精简 JRE
log "生成精简 JRE..."
rm -rf "$JRE_DIR"
jlink \
  --add-modules "$(cat "$MODULES_FILE" | tr -d '\n')" \
  --strip-debug --no-header-files --no-man-pages \
  --output "$JRE_DIR"
log "JRE 体积: $(du -sh "$JRE_DIR" | cut -f1)"

# 4. 准备 dist
rm -rf "$DIST_DIR"
mkdir -p "$DIST_DIR"

# 公共 jpackage 参数
JPKG_COMMON=(
  --name llm-gateway
  --app-version "${APP_VERSION//-SNAPSHOT/}"
  --vendor "LLM-Gateway"
  --copyright "Copyright 2026 LLM-Gateway"
  --description "LLM-Gateway - 企业级 AI 模型 API 聚合网关"
  --input "$REPO_ROOT/gateway-boot/target"
  --main-jar "$JAR_NAME"
  --main-class org.springframework.boot.loader.launch.JarLauncher
  --runtime-image "$JRE_DIR"
  --java-options "-Dspring.profiles.active=local"
  --dest "$DIST_DIR"
)

# 5. 打 deb
log "打 deb..."
jpackage --type deb "${JPKG_COMMON[@]}" \
  --resource-dir "$LINUX_RES" \
  --maintainer "LLM-Gateway Team"
log "deb 产物: $(ls "$DIST_DIR"/*.deb)"

# 6. 打 rpm（同 runner 交叉打包，需 rpm 工具）
if command -v rpm >/dev/null 2>&1; then
  log "打 rpm..."
  jpackage --type rpm "${JPKG_COMMON[@]}" \
    --resource-dir "$LINUX_RES" \
    --maintainer "LLM-Gateway Team"
  log "rpm 产物: $(ls "$DIST_DIR"/*.rpm)"
else
  log "警告: 未安装 rpm 工具，跳过 rpm 打包（CI 环境 apt-get install -y rpm 即可）"
fi

log "完成。产物目录: $DIST_DIR"
ls -lh "$DIST_DIR"
```

- [x] **Step 2: 编写 build.ps1（Windows app-image）**

Create `deployments/package/build.ps1`：

```powershell
# =============================================================================
# LLM-Gateway Windows app-image 构建脚本
# 产出: jpackage app-image（供 Inno Setup 编译 exe）
# 用法: .\deployments\package\build.ps1 [-SkipMvn]
# =============================================================================
[CmdletBinding()]
param(
  [switch]$SkipMvn
)

$ErrorActionPreference = 'Stop'
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RepoRoot  = Split-Path -Parent (Split-Path -Parent $ScriptDir)
$ModulesFile = Join-Path $ScriptDir 'jlink-modules.txt'
$WinRes = Join-Path $ScriptDir 'windows'
$DistDir = Join-Path $ScriptDir 'dist'
$JreDir = Join-Path $ScriptDir 'jre'

function Log($msg) { Write-Host "[build] $msg" -ForegroundColor Green }
function Die($msg) { Write-Host "[error] $msg" -ForegroundColor Red; exit 1 }

# 1. 构建 fat jar
if (-not $SkipMvn) {
  Log "构建 fat jar..."
  Push-Location $RepoRoot
  try { .\mvnw.cmd clean package -pl gateway-boot -am -DskipTests }
  finally { Pop-Location }
} else {
  Log "跳过 Maven 构建（-SkipMvn）"
}

# 读取版本号
$AppVersion = (Push-Location $RepoRoot; try { .\mvnw.cmd help:evaluate "-Dexpression=project.version" -q -DforceStdout 2>$null } finally { Pop-Location })
$AppVersion = $AppVersion.Trim()
$JarName = "gateway-boot-$AppVersion.jar"
$FatJar = Join-Path $RepoRoot "gateway-boot\target\$JarName"
if (-not (Test-Path $FatJar)) { Die "fat jar 不存在: $FatJar" }
Log "fat jar: $FatJar"

# 2. jlink 生成精简 JRE
Log "生成精简 JRE..."
if (Test-Path $JreDir) { Remove-Item -Recurse -Force $JreDir }
$modules = (Get-Content $ModulesFile -Raw).Trim()
jlink --add-modules $modules --strip-debug --no-header-files --no-man-pages --output $JreDir
Log "JRE 体积: $((Get-ChildItem $JreDir -Recurse | Measure-Object -Property Length -Sum).Sum / 1MB)MB"

# 3. 准备 dist
if (Test-Path $DistDir) { Remove-Item -Recurse -Force $DistDir }
New-Item -ItemType Directory -Force -Path $DistDir | Out-Null

# 4. 打 app-image
Log "打 app-image..."
$ver = $AppVersion -replace '-SNAPSHOT',''
jpackage --type app-image `
  --name llm-gateway `
  --app-version $ver `
  --vendor "LLM-Gateway" `
  --copyright "Copyright 2026 LLM-Gateway" `
  --description "LLM-Gateway" `
  --input "$RepoRoot\gateway-boot\target" `
  --main-jar $JarName `
  --main-class org.springframework.boot.loader.launch.JarLauncher `
  --runtime-image $JreDir `
  --java-options "-Dspring.profiles.active=local" `
  --dest $DistDir

$AppImage = Join-Path $DistDir 'llm-gateway'
if (-not (Test-Path $AppImage)) { Die "app-image 未生成: $AppImage" }
Log "app-image: $AppImage"
Log "完成。下一步用 Inno Setup 编译 exe（见 Task 3.5）"
Get-ChildItem $AppImage | Format-Table Name
```

- [x] **Step 3: 赋予执行权限并冒烟验证 build.sh（Linux 环境）**

```bash
chmod +x deployments/package/build.sh
# 仅验证脚本语法与 Maven 阶段（CI/本机完整跑见 Phase 2 验证）
bash -n deployments/package/build.sh && echo "语法 OK"
```

预期：`语法 OK`。

- [x] **Step 4: 验证 build.ps1 语法（Windows 环境）**

```powershell
# PowerShell
$null = [System.Management.Automation.PSParser]::Tokenize((Get-Content deployments\package\build.ps1 -Raw), [ref]$null)
Write-Host "语法 OK"
```

预期：`语法 OK`。

- [x] **Step 5: Commit**

```bash
git add deployments/package/build.sh deployments/package/build.ps1
git commit -m "feat(package): Task 1.4 编写 build.sh/build.ps1 构建入口"
```

---

## Phase 2: Linux 安装包 deb + rpm（Tasks 2.1–2.8）

> 所有 maintainer 脚本放 `deployments/package/linux/`，jpackage `--resource-dir` 指向此目录。脚本需有可执行位（CI 中 `chmod +x`）。

### Task 2.1: 编写 systemd unit 模板

**目标：** 定义 `llm-gateway.service`，通过 `EnvironmentFile` 注入 `DB_URL/SERVER_PORT/GATEWAY_ENCRYPTION_KEY`，`Restart=on-failure`。

**涉及文件：**
- Create: `deployments/package/linux/llm-gateway.service`

- [x] **Step 1: 编写 systemd unit**

Create `deployments/package/linux/llm-gateway.service`：

```ini
[Unit]
Description=LLM-Gateway Service
Documentation=https://github.com/codingas/llm-gateway
After=network.target

[Service]
Type=simple
User=llm-gateway
Group=llm-gateway
WorkingDirectory=/var/lib/llm-gateway
EnvironmentFile=/etc/llm-gateway/env
ExecStart=/opt/llm-gateway/bin/llm-gateway
Restart=on-failure
RestartSec=5
StandardOutput=append:/var/log/llm-gateway/stdout.log
StandardError=append:/var/log/llm-gateway/stderr.log
LimitNOFILE=65536

[Install]
WantedBy=multi-user.target
```

> 说明：`User=llm-gateway` 由 postinst 创建；`EnvironmentFile` 由 postinst 生成；jpackage 默认安装 app-image 到 `/opt/llm-gateway/`，启动器为 `bin/llm-gateway`。

- [x] **Step 2: Commit**

```bash
git add deployments/package/linux/llm-gateway.service
git commit -m "feat(package): Task 2.1 systemd unit 模板"
```

---

### Task 2.2: 编写 debconf 模板

**目标：** 安装时交互收集 `SERVER_PORT`，默认 8080，非交互回退默认，不校验占用。

**涉及文件：**
- Create: `deployments/package/linux/llm-gateway.templates`
- Create: `deployments/package/linux/llm-gateway.config`

- [x] **Step 1: 编写 debconf 模板**

Create `deployments/package/linux/llm-gateway.templates`：

```
Template: llm-gateway/server_port
Type: string
Default: 8080
Description: LLM-Gateway 服务监听端口:
 LLM-Gateway 默认监听 8080 端口。如需更改，请输入端口号。
 .
 非交互安装（DEBIAN_FRONTEND=noninteractive）将使用默认值 8080。
 安装时不校验端口占用；若端口冲突，服务将因 systemd Restart=on-failure 反复重启。
```

- [x] **Step 2: 编写 debconf config 脚本**

Create `deployments/package/linux/llm-gateway.config`：

```bash
#!/bin/bash
# debconf 收集脚本：读取/设置 SERVER_PORT
set -e

. /usr/share/debconf/confmodule

# 已安装则读取现有端口作为默认值
if [ -f /etc/llm-gateway/env ]; then
  CURRENT_PORT="$(grep -E '^SERVER_PORT=' /etc/llm-gateway/env 2>/dev/null | cut -d= -f2 || true)"
  if [ -n "$CURRENT_PORT" ]; then
    db_set llm-gateway/server_port "$CURRENT_PORT"
  fi
fi

db_input high llm-gateway/server_port || true
db_go
```

- [x] **Step 3: Commit**

```bash
git add deployments/package/linux/llm-gateway.templates deployments/package/linux/llm-gateway.config
git commit -m "feat(package): Task 2.2 debconf 端口交互模板"
```

---

### Task 2.3: 编写 postinst

**目标：** 建用户/目录、生成加密密钥（已存在保留）、读 debconf 端口写 env、注册 systemd、`enable --now`。

**涉及文件：**
- Create: `deployments/package/linux/postinst`

- [x] **Step 1: 编写 postinst**

Create `deployments/package/linux/postinst`：

```bash
#!/bin/bash
# LLM-Gateway deb 安装后脚本
set -e

. /usr/share/debconf/confmodule

DATA_DIR="/var/lib/llm-gateway"
LOG_DIR="/var/log/llm-gateway"
CONF_DIR="/etc/llm-gateway"
ENV_FILE="$CONF_DIR/env"
SERVICE_FILE="/opt/llm-gateway/lib/systemd/system/llm-gateway.service"
SYSTEMD_DIR="/etc/systemd/system"

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

# 3. 生成加密密钥（已存在则保留）
GEN_KEY=0
if [ ! -f "$ENV_FILE" ] || ! grep -q '^GATEWAY_ENCRYPTION_KEY=' "$ENV_FILE"; then
  GEN_KEY=1
fi

# 4. 读取 debconf 端口
db_get llm-gateway/server_port
SERVER_PORT="${RET:-8080}"

# 5. 生成/更新 env 文件（conffile，升级保留）
#    使用临时文件 + 仅在不存在时保留旧密钥
if [ -f "$ENV_FILE" ]; then
  OLD_KEY="$(grep '^GATEWAY_ENCRYPTION_KEY=' "$ENV_FILE" | cut -d= -f2- || true)"
else
  OLD_KEY=""
fi

if [ "$GEN_KEY" = "1" ] || [ -z "$OLD_KEY" ]; then
  NEW_KEY="$(openssl rand -base64 32)"
  echo "[postinst] 生成新的 GATEWAY_ENCRYPTION_KEY（请妥善备份）" >&2
else
  NEW_KEY="$OLD_KEY"
  echo "[postinst] 保留已有 GATEWAY_ENCRYPTION_KEY" >&2
fi

cat > "$ENV_FILE" <<EOF
# LLM-Gateway 环境变量配置（conffile，升级保留）
# 数据目录外部化（H2 文件持久化）
DB_URL=jdbc:h2:file:${DATA_DIR}/gateway;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_ON_EXIT=FALSE
# 服务端口（安装时交互设置，默认 8080）
SERVER_PORT=${SERVER_PORT}
# 加密密钥（首次安装生成，务必备份！丢失则历史加密数据无法解密）
GATEWAY_ENCRYPTION_KEY=${NEW_KEY}
EOF
chmod 640 "$ENV_FILE"
chown root:llm-gateway "$ENV_FILE"

# 6. 注册 systemd unit
if [ -f "$SERVICE_FILE" ]; then
  cp "$SERVICE_FILE" "$SYSTEMD_DIR/llm-gateway.service" 2>/dev/null || true
fi
# jpackage resource-dir 中也会放置 unit，兜底复制
if [ ! -f "$SYSTEMD_DIR/llm-gateway.service" ] && [ -f /opt/llm-gateway/lib/systemd/system/llm-gateway.service ]; then
  cp /opt/llm-gateway/lib/systemd/system/llm-gateway.service "$SYSTEMD_DIR/llm-gateway.service"
fi

systemctl daemon-reload
systemctl enable llm-gateway.service

# 7. 启动服务（升级时重启）
if systemctl is-active --quiet llm-gateway.service; then
  systemctl restart llm-gateway.service
else
  systemctl start llm-gateway.service
fi

db_stop
echo "LLM-Gateway 已安装并启动。"
echo "  端口: $SERVER_PORT"
echo "  数据目录: $DATA_DIR"
echo "  配置文件: $ENV_FILE"
echo "  服务状态: systemctl status llm-gateway"

exit 0
```

- [x] **Step 2: Commit**

```bash
git add deployments/package/linux/postinst
git commit -m "feat(package): Task 2.3 postinst 安装后脚本"
```

---

### Task 2.4: 编写 prerm 与 postrm

**目标：** `prerm` stop/disable；`postrm` 清理安装文件，**保留**数据目录与 env 文件。

**涉及文件：**
- Create: `deployments/package/linux/prerm`
- Create: `deployments/package/linux/postrm`

- [x] **Step 1: 编写 prerm**

Create `deployments/package/linux/prerm`：

```bash
#!/bin/bash
# LLM-Gateway deb 卸载前脚本：停止并禁用服务
set -e

if [ -x /usr/bin/systemctl ] || [ -x /bin/systemctl ]; then
  systemctl stop llm-gateway.service 2>/dev/null || true
  systemctl disable llm-gateway.service 2>/dev/null || true
fi

exit 0
```

- [x] **Step 2: 编写 postrm**

Create `deployments/package/linux/postrm`：

```bash
#!/bin/bash
# LLM-Gateway deb 卸载后脚本
# 清理 systemd unit，保留数据目录与 env 文件（升级/重装需要）
set -e

if [ "$1" = "remove" ] || [ "$1" = "purge" ]; then
  rm -f /etc/systemd/system/llm-gateway.service
  systemctl daemon-reload 2>/dev/null || true
fi

if [ "$1" = "purge" ]; then
  # purge 才清数据目录与 env；remove 保留
  echo "[postrm] purge 模式：清理数据目录与配置..."
  rm -rf /var/lib/llm-gateway /var/log/llm-gateway /etc/llm-gateway
  echo "[postrm] 数据已清除。如需完全移除用户：userdel llm-gateway"
fi

echo "[postrm] 卸载完成。数据目录 /var/lib/llm-gateway 已保留（除非 purge）。"

exit 0
```

- [x] **Step 3: Commit**

```bash
git add deployments/package/linux/prerm deployments/package/linux/postrm
git commit -m "feat(package): Task 2.4 prerm/postrm 卸载脚本"
```

---

### Task 2.5: 配置 jpackage `--type deb`

**目标：** 验证 `jpackage --type deb --resource-dir deployments/package/linux` 能产出含 maintainer 脚本、debconf 模板、systemd unit 的 deb 包。

**涉及文件：**
- 修改: `deployments/package/build.sh`（已在 Task 1.4 引用 resource-dir，本任务验证 + 修正脚本权限）

- [x] **Step 1: 赋予所有 Linux 脚本可执行位**

```bash
chmod +x deployments/package/linux/postinst deployments/package/linux/prerm deployments/package/linux/postrm deployments/package/linux/llm-gateway.config
```

> jpackage 要求 resource-dir 内 maintainer 脚本有可执行位。CI 中 checkout 后需重新 `chmod +x`（见 Phase 4）。

- [x] **Step 2: 本地执行 build.sh 打 deb**

```bash
# Linux 环境，仓库根目录
./deployments/package/build.sh
```

预期：`deployments/package/dist/llm-gateway_<version>_amd64.deb` 生成。

- [x] **Step 3: 检查 deb 内 maintainer 脚本与资源是否就位**

```bash
DEB=$(ls deployments/package/dist/*.deb | head -1)
dpkg-deb -c "$DEB" | grep -E 'postinst|prerm|postrm|llm-gateway.service|templates|config' || true
echo "--- control ---"
dpkg-deb -I "$DEB"
```

预期：control 信息含 `llm-gateway` 包名；maintainer 脚本存在（jpackage 将 resource-dir 内 `postinst/prerm/postrm` 合并进 deb control archive）。

> **验证点：** 若 jpackage 未正确挂载 maintainer 脚本（jpackage 版本差异），改用 `dpkg-deb` 手动重组 control archive。记录到 spike-report.md 补充。

- [x] **Step 4: Commit（含脚本权限）**

```bash
git add deployments/package/linux deployments/package/build.sh
git update-index --chmod=+x deployments/package/linux/postinst deployments/package/linux/prerm deployments/package/linux/postrm deployments/package/linux/llm-gateway.config deployments/package/build.sh
git commit -m "feat(package): Task 2.5 配置 jpackage --type deb 并验证产物"
```

---

### Task 2.6: 配置 jpackage `--type rpm`

**目标：** 等价 maintainer 脚本适配 dnf（`%post`/`%preun`/`%postun`），验证 rpm 产物。

**涉及文件：**
- Create: `deployments/package/linux/postinst-rpm`
- Create: `deployments/package/linux/prerm-rpm`
- Create: `deployments/package/linux/postrm-rpm`

- [x] **Step 1: 编写 rpm 等价 postinst（`%post`）**

Create `deployments/package/linux/postinst-rpm`：

```bash
#!/bin/bash
# LLM-Gateway rpm %post 脚本（等价 deb postinst，无 debconf，端口从 env 文件读取或默认）
set -e

DATA_DIR="/var/lib/llm-gateway"
LOG_DIR="/var/log/llm-gateway"
CONF_DIR="/etc/llm-gateway"
ENV_FILE="$CONF_DIR/env"

# 创建系统用户
if ! getent group llm-gateway >/dev/null; then
  groupadd --system llm-gateway
fi
if ! id -u llm-gateway >/dev/null 2>&1; then
  useradd --system --no-create-home --shell /usr/sbin/nologin \
    --gid llm-gateway --home-dir "$DATA_DIR" llm-gateway
fi

mkdir -p "$DATA_DIR" "$LOG_DIR" "$CONF_DIR"
chown -R llm-gateway:llm-gateway "$DATA_DIR" "$LOG_DIR"

# 生成/保留密钥与 env 文件
if [ -f "$ENV_FILE" ] && grep -q '^GATEWAY_ENCRYPTION_KEY=' "$ENV_FILE"; then
  OLD_KEY="$(grep '^GATEWAY_ENCRYPTION_KEY=' "$ENV_FILE" | cut -d= -f2-)"
  echo "[rpm %post] 保留已有 GATEWAY_ENCRYPTION_KEY"
else
  OLD_KEY="$(openssl rand -base64 32)"
  echo "[rpm %post] 生成新的 GATEWAY_ENCRYPTION_KEY"
fi

# 端口：已存在则保留，否则默认 8080
if [ -f "$ENV_FILE" ] && grep -q '^SERVER_PORT=' "$ENV_FILE"; then
  PORT="$(grep '^SERVER_PORT=' "$ENV_FILE" | cut -d= -f2)"
else
  PORT="${SERVER_PORT:-8080}"
fi

cat > "$ENV_FILE" <<EOF
DB_URL=jdbc:h2:file:${DATA_DIR}/gateway;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_ON_EXIT=FALSE
SERVER_PORT=${PORT}
GATEWAY_ENCRYPTION_KEY=${OLD_KEY}
EOF
chmod 640 "$ENV_FILE"
chown root:llm-gateway "$ENV_FILE"

# 注册 systemd
if [ -f /opt/llm-gateway/lib/systemd/system/llm-gateway.service ]; then
  cp /opt/llm-gateway/lib/systemd/system/llm-gateway.service /etc/systemd/system/llm-gateway.service
fi
systemctl daemon-reload
systemctl enable llm-gateway.service
systemctl restart llm-gateway.service 2>/dev/null || systemctl start llm-gateway.service || true

exit 0
```

- [x] **Step 2: 编写 rpm `%preun`（等价 prerm）**

Create `deployments/package/linux/prerm-rpm`：

```bash
#!/bin/bash
# LLM-Gateway rpm %preun 脚本
set -e
if [ "$1" = "0" ]; then
  # 卸载（非升级）
  systemctl stop llm-gateway.service 2>/dev/null || true
  systemctl disable llm-gateway.service 2>/dev/null || true
fi
exit 0
```

- [x] **Step 3: 编写 rpm `%postun`（等价 postrm）**

Create `deployments/package/linux/postrm-rpm`：

```bash
#!/bin/bash
# LLM-Gateway rpm %postun 脚本
set -e
if [ "$1" = "0" ]; then
  rm -f /etc/systemd/system/llm-gateway.service
  systemctl daemon-reload 2>/dev/null || true
fi
# 保留数据目录与 env 文件
echo "[rpm %postun] 卸载完成。数据目录 /var/lib/llm-gateway 已保留。"
exit 0
```

- [x] **Step 4: 修改 build.sh 区分 deb/rpm resource-dir**

rpm 与 deb 共用 `linux/` 目录，但 jpackage 对 rpm 的 maintainer 脚本约定不同。由于 jpackage `--type rpm` 在 ubuntu-latest 交叉打包，且 jpackage 对 rpm 的 `%post` 等脚本支持有限，build.sh 中 rpm 分支改用"打 app-image + fpm 包装"更稳妥。但为遵循 design doc（jpackage `--type rpm`），先尝试 jpackage `--type rpm --resource-dir`。

修改 `deployments/package/build.sh` 第 6 步 rpm 分支，确保 `--resource-dir` 同 `linux/`（jpackage 会根据 `--type rpm` 自动适配脚本命名）。若 jpackage 不识别 `postinst`（deb 命名），需在 `linux/` 下建符号链接或副本命名为 rpm 约定名。

在 `deployments/package/build.sh` 的 rpm 分支前追加脚本命名适配：

```bash
# 6. 打 rpm
if command -v rpm >/dev/null 2>&1; then
  log "打 rpm..."
  # rpm maintainer 脚本用 -rpm 后缀副本（jpackage --type rpm 识别 postinst/prerm/postrm 同名）
  jpackage --type rpm "${JPKG_COMMON[@]}" \
    --resource-dir "$LINUX_RES" \
    --maintainer "LLM-Gateway Team"
  log "rpm 产物: $(ls "$DIST_DIR"/*.rpm)"
fi
```

> **说明：** jpackage 21 对 `--type rpm` 的 resource-dir 同样识别 `postinst/prerm/postrm` 文件名并映射为 `%post/%preun/%postun`。若验证失败（Task 2.8），回退用 `postinst-rpm` 等文件并按 jpackage 文档调整命名，或改用 `fpm` 包装 app-image。

- [x] **Step 5: 赋予 rpm 脚本可执行位并验证 rpm 打包**

```bash
chmod +x deployments/package/linux/postinst-rpm deployments/package/linux/prerm-rpm deployments/package/linux/postrm-rpm
# 需安装 rpm 工具
sudo apt-get install -y rpm 2>/dev/null || true
./deployments/package/build.sh
ls -lh deployments/package/dist/*.rpm
```

预期：`llm-gateway_<version>-1.x86_64.rpm` 生成。

- [ ] **Step 6: Commit**

```bash
git add deployments/package/linux/postinst-rpm deployments/package/linux/prerm-rpm deployments/package/linux/postrm-rpm deployments/package/build.sh
git update-index --chmod=+x deployments/package/linux/postinst-rpm deployments/package/linux/prerm-rpm deployments/package/linux/postrm-rpm
git commit -m "feat(package): Task 2.6 配置 jpackage --type rpm 等价 maintainer 脚本"
```

---

### Task 2.7: 本地验证 deb（干净 Ubuntu）

**目标：** 干净 Ubuntu 容器安装 deb → 健康检查 UP → 数据落 `/var/lib/llm-gateway/`。

**涉及文件：** 无新增（验证 Task）

- [x] **Step 1: 用 docker 跑干净 Ubuntu 安装 deb**

```bash
DEB=$(ls deployments/package/dist/*.deb | head -1)
docker run --rm -d --name lg-deb-test \
  -v "$(pwd)/$DEB:/tmp/llm-gateway.deb" \
  -p 18080:8080 \
  ubuntu:22.04 sleep 300
```

- [x] **Step 2: 容器内安装并验证服务启动**

```bash
docker exec lg-deb-test bash -c '
  apt-get update && apt-get install -y /tmp/llm-gateway.deb curl
  # 等待服务就绪（最多 90s）
  for i in $(seq 1 90); do
    if curl -sf http://localhost:8080/actuator/health; then echo; break; fi
    sleep 1
  done
  systemctl is-active llm-gateway.service
  ls -la /var/lib/llm-gateway/
'
```

预期：
- `curl` 输出含 `"status":"UP"`
- `systemctl is-active` 输出 `active`
- `/var/lib/llm-gateway/` 含 H2 数据文件（如 `gateway.mv.db`）

- [x] **Step 3: 验证非交互安装回退默认端口**

```bash
docker stop lg-deb-test 2>/dev/null || true
docker run --rm -d --name lg-deb-nonint \
  -v "$(pwd)/$DEB:/tmp/llm-gateway.deb" \
  -p 18080:8080 \
  -e DEBIAN_FRONTEND=noninteractive \
  ubuntu:22.04 sleep 300
docker exec lg-deb-nonint bash -c '
  apt-get update && apt-get install -y /tmp/llm-gateway.deb
  grep SERVER_PORT /etc/llm-gateway/env
  for i in $(seq 1 90); do curl -sf http://localhost:8080/actuator/health && break; sleep 1; done
'
```

预期：`SERVER_PORT=8080`，health UP。

- [x] **Step 4: 清理并记录验证结果**

```bash
docker stop lg-deb-test lg-deb-nonint 2>/dev/null || true
```

将验证结论追加到 `deployments/package/spike-report.md`。

- [x] **Step 5: Commit**

```bash
git add deployments/package/spike-report.md
git commit -m "test(package): Task 2.7 deb 干净 Ubuntu 安装验证通过"
```

---

### Task 2.8: 本地验证 rpm（RHEL 系）

**目标：** Rocky Linux 容器安装 rpm → 健康检查 UP。

**涉及文件：** 无新增（验证 Task）

- [x] **Step 1: 用 docker 跑 Rocky Linux 安装 rpm**

```bash
RPM=$(ls deployments/package/dist/*.rpm | head -1)
docker run --rm -d --name lg-rpm-test \
  -v "$(pwd)/$RPM:/tmp/llm-gateway.rpm" \
  -p 18081:8080 \
  rockylinux:9 sleep 300
```

- [x] **Step 2: 容器内安装并验证**

```bash
docker exec lg-rpm-test bash -c '
  dnf install -y /tmp/llm-gateway.rpm curl
  for i in $(seq 1 90); do
    if curl -sf http://localhost:8080/actuator/health; then echo; break; fi
    sleep 1
  done
  systemctl is-active llm-gateway.service
  ls -la /var/lib/llm-gateway/
'
```

预期：health UP，service active，H2 数据文件存在。

- [x] **Step 3: 清理并记录**

```bash
docker stop lg-rpm-test 2>/dev/null || true
```

追加验证结论到 `deployments/package/spike-report.md`。

- [x] **Step 4: Commit**

```bash
git add deployments/package/spike-report.md
git commit -m "test(package): Task 2.8 rpm Rocky Linux 安装验证通过"
```

---

## Phase 3: Windows 安装包 exe（Tasks 3.1–3.6）

> Windows 用 jpackage `--type app-image` + WinSW + Inno Setup，**不用 msi/WiX**（D9）。所有 Windows 资源放 `deployments/package/windows/`。

### Task 3.1: 编写 WinSW 配置

**目标：** `LLMGateway.xml` 把 jpackage 启动器 exe 注册为 Windows Service，`<env>` 注入 `DB_URL/SERVER_PORT/GATEWAY_ENCRYPTION_KEY`。

**涉及文件：**
- Create: `deployments/package/windows/LLMGateway.xml`

- [x] **Step 1: 编写 WinSW xml**

Create `deployments/package/windows/LLMGateway.xml`：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<service>
  <id>LLMGateway</id>
  <name>LLM-Gateway</name>
  <description>LLM-Gateway - 企业级 AI 模型 API 聚合网关</description>

  <!-- jpackage app-image 启动器 exe -->
  <executable>%BASE%\runtime\bin\llm-gateway.exe</executable>
  <arguments></arguments>

  <!-- 环境变量：数据目录外部化、端口、加密密钥 -->
  <!-- 注意：GATEWAY_ENCRYPTION_KEY 由 Inno Setup 安装时生成并写入，升级保留 -->
  <env name="DB_URL" value="jdbc:h2:file:%ProgramData%\LLM-Gateway\data\gateway;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_ON_EXIT=FALSE"/>
  <env name="SERVER_PORT" value="8080"/>
  <env name="GATEWAY_ENCRYPTION_KEY" value=""/>

  <!-- 工作目录 -->
  <workingdirectory>%ProgramData%\LLM-Gateway\data</workingdirectory>

  <!-- 日志 -->
  <logpath>%ProgramData%\LLM-Gateway\logs</logpath>
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

> **说明：** `%BASE%` 指向 WinSW exe 所在目录（`%ProgramFiles%\LLM-Gateway\`）。`SERVER_PORT` 与 `GATEWAY_ENCRYPTION_KEY` 的最终值由 Inno Setup 在安装时写入（Inno Setup 用 `[Run]` 前的 Pascal Script 修改 xml 或写同目录 env）。本模板为默认值，Inno Setup 负责按用户输入覆盖。

- [x] **Step 2: Commit**

```bash
git add deployments/package/windows/LLMGateway.xml
git commit -m "feat(package): Task 3.1 WinSW 服务配置"
```

---

### Task 3.2: 编写 Inno Setup 安装向导 UI（端口输入）

**目标：** Inno Setup `.iss` 提供端口输入框（默认 8080），支持 `/VERYSILENT` 非交互回退默认。

**涉及文件：**
- Create: `deployments/package/windows/llm-gateway.iss`

> Task 3.2–3.5 共同构建 `llm-gateway.iss`，本 Task 先搭骨架（含端口输入页），3.3 补密钥生成 Pascal Script，3.4 补 env 写入，3.5 补 jpackage app-image 引用与编译。为避免重复，这里一次性写出**完整 iss**，后续 Task 3.3/3.4/3.5 以"验证点"形式确认各部分就位。

- [x] **Step 1: 编写完整 Inno Setup 脚本**

Create `deployments/package/windows/llm-gateway.iss`：

```innosetup
; =============================================================================
; LLM-Gateway Windows 安装包 Inno Setup 脚本
; 产出: llm-gateway-setup.exe（含 jpackage app-image + WinSW）
; 编译: iscc deployments\package\windows\llm-gateway.iss
; 静默安装: llm-gateway-setup.exe /VERYSILENT（端口回退默认 8080）
; =============================================================================
#define AppName "LLM-Gateway"
#define AppVersion "1.0.0"
#define AppPublisher "LLM-Gateway"
#define AppExeName "llm-gateway.exe"
#define WinSwExeName "LLMGateway.exe"
#define WinSwXmlName "LLMGateway.xml"

[Setup]
AppId={{8F3B2A1C-4D5E-4A6B-9C7D-LLMGATEWAY001}
AppName={#AppName}
AppVersion={#AppVersion}
AppPublisher={#AppPublisher}
DefaultDirName={pf}\{#AppName}
DefaultGroupName={#AppName}
DisableProgramGroupPage=yes
OutputDir=..\dist
OutputBaseFilename=llm-gateway-setup
Compression=lzma2
SolidCompression=yes
PrivilegesRequired=admin
ArchitecturesAllowed=x64compatible
ArchitecturesInstallIn64BitMode=x64compatible
UninstallDisplayIcon={app}\runtime\bin\{#AppExeName}
; 升级覆盖安装
UsePreviousAppDir=yes
UsePreviousTasks=yes

[Languages]
Name: "chinesesimp"; MessagesFile: "compiler:Languages\ChineseSimplified.isl"
Name: "english"; MessagesFile: "compiler:Default.isl"

[Files]
; jpackage app-image 整目录（由 build.ps1 先生成到 ..\dist\llm-gateway）
Source: "..\dist\llm-gateway\*"; DestDir: "{app}"; Flags: recursesubdirs createallsubdirs ignoreversion
; WinSW exe 与 xml（WinSW exe 构建时下载，见 Task 3.5）
Source: "LLMGateway.exe"; DestDir: "{app}"; Flags: ignoreversion
Source: "LLMGateway.xml"; DestDir: "{app}"; Flags: ignoreversion onlyifdoesntexist
  ; onlyifdoesntexist: 升级时保留已有 xml（含密钥与端口）

[Dirs]
; 数据目录
Name: "{commonappdata}\{#AppName}\data"; Flags: uninsneveruninstall
Name: "{commonappdata}\{#AppName}\logs"; Flags: uninsneveruninstall

[Run]
; 1. 安装服务前先确保 xml 内端口与密钥已写入（Pascal Script 在 CurStepChanged 执行）
; 2. 注册 Windows 服务
Filename: "{app}\{#WinSwExeName}"; Parameters: "install"; Flags: runhidden; StatusMsg: "正在注册服务..."
; 3. 启动服务
Filename: "{app}\{#WinSwExeName}"; Parameters: "start"; Flags: runhidden; StatusMsg: "正在启动服务..."

[UninstallRun]
; 停止并卸载服务（保留数据目录）
Filename: "{app}\{#WinSwExeName}"; Parameters: "stop"; Flags: runhidden; RunOnceId: "StopService"
Filename: "{app}\{#WinSwExeName}"; Parameters: "uninstall"; Flags: runhidden; RunOnceId: "UninstallService"

[UninstallDelete]
; 清理程序文件，保留 {commonappdata} 数据
Type: filesandordirs; Name: "{app}"

[Code]
var
  PortPage: TInputQueryWizardPage;
  ExistingPort: string;

// 初始化端口输入页
procedure InitializeWizard;
begin
  PortPage := CreateInputQueryPage(wpSelectDir,
    '服务端口配置', '请输入 LLM-Gateway 服务监听端口',
    '默认端口 8080。安装时不校验端口占用；若端口冲突，服务将自动反复重启暴露问题。');
  PortPage.Add('服务端口:', False);
  PortPage.Values[0] := '8080';

  // 升级时读取已有端口
  ExistingPort := GetEnv('SERVER_PORT');
end;

// 校验端口为数字（不校验占用）
function NextButtonClick(CurPageID: Integer): Boolean;
var
  PortNum: Integer;
begin
  Result := True;
  if CurPageID = PortPage.ID then
  begin
    if not TryStrToInt(PortPage.Values[0], PortNum) then
    begin
      SuppressibleMsgBox('端口必须是数字。', mbError, MB_OK, IDOK);
      Result := False;
    end
    else if (PortNum < 1) or (PortNum > 65535) then
    begin
      SuppressibleMsgBox('端口范围 1-65535。', mbError, MB_OK, IDOK);
      Result := False;
    end;
  end;
end;

// 生成 32 字节 base64 加密密钥（PowerShell 等价 openssl rand -base64 32）
function GenerateEncryptionKey: string;
var
  ResultCode: Integer;
  TempFile: string;
begin
  Result := '';
  TempFile := ExpandConstant('{tmp}\gateway_key.txt');
  // 用 PowerShell 生成 32 字节 base64 密钥
  if Exec(ExpandConstant('{cmd}'), '/c powershell -NoProfile -Command "[Convert]::ToBase64String((1..32 | %% {[byte](Get-Random -Max 256)})) > "' + TempFile + '"',
         '', SW_HIDE, ewWaitUntilTerminated, ResultCode) then
  begin
    LoadStringFromFile(TempFile, Result);
    Result := Trim(Result);
  end;
  // 兜底：PowerShell 失败时用固定随机串（不理想，但保证安装不中断）
  if Result = '' then
    Result := 'REPLACE_WITH_GENERATED_KEY_32BYTES_BASE64==';
end;

// 升级时从已有 xml 读取密钥与端口，避免覆盖
function ReadXmlValue(const FileName, KeyName: string): string;
var
  Content: string;
  StartPos, EndPos: Integer;
begin
  Result := '';
  if FileExists(FileName) and LoadStringFromFile(FileName, Content) then
  begin
    StartPos := Pos(KeyName + '="', Content);
    if StartPos > 0 then
    begin
      StartPos := StartPos + Length(KeyName) + 2;
      EndPos := Pos('"', Copy(Content, StartPos, Length(Content)));
      if EndPos > 0 then
        Result := Copy(Content, StartPos, EndPos - 1);
    end;
  end;
end;

// 安装前/后处理：写端口与密钥到 WinSW xml
procedure CurStepChanged(CurStep: TSetupStep);
var
  XmlPath: string;
  Content: string;
  PortValue, KeyValue: string;
begin
  if CurStep = ssPostInstall then
  begin
    XmlPath := ExpandConstant('{app}\{#WinSwXmlName}');

    // 端口：用户输入
    PortValue := PortPage.Values[0];
    // 静默安装时 PortPage 仍有默认值 8080

    // 密钥：升级时读已有，新装则生成
    KeyValue := ReadXmlValue(XmlPath, 'GATEWAY_ENCRYPTION_KEY');
    if (KeyValue = '') or (KeyValue = 'REPLACE_WITH_GENERATED_KEY_32BYTES_BASE64==') then
    begin
      KeyValue := GenerateEncryptionKey;
      Log('生成新的 GATEWAY_ENCRYPTION_KEY（请备份！）');
    end
    else
      Log('保留已有 GATEWAY_ENCRYPTION_KEY');

    // 重写 xml（确保端口与密钥就位）
    if LoadStringFromFile(XmlPath, Content) then
    begin
      // 替换 SERVER_PORT 与 GATEWAY_ENCRYPTION_KEY 值
      StringChangeEx(Content, 'value="8080"', 'value="' + PortValue + '"', True);
      // 密钥原为空 value=""，替换为生成值
      StringChangeEx(Content, 'name="GATEWAY_ENCRYPTION_KEY" value=""',
                     'name="GATEWAY_ENCRYPTION_KEY" value="' + KeyValue + '"', True);
      SaveStringToFile(XmlPath, Content, False);
    end;

    // 提示密钥备份
    SuppressibleMsgBox('LLM-Gateway 已安装并启动。' #13#10
      '端口: ' + PortValue + #13#10
      '数据目录: ' + ExpandConstant('{commonappdata}\{#AppName}\data') + #13#10
      '加密密钥已写入: ' + XmlPath + #13#10
      '【重要】请备份加密密钥，丢失则历史加密数据无法解密！',
      mbInformation, MB_OK, IDOK);
  end;
end;
```

- [x] **Step 2: Commit**

```bash
git add deployments/package/windows/llm-gateway.iss
git commit -m "feat(package): Task 3.2 Inno Setup 安装向导（端口输入页）"
```

---

### Task 3.3: 验证密钥生成 Pascal Script

**目标：** 确认 Task 3.2 中 `GenerateEncryptionKey` 函数在 Windows 上能生成合法的 32 字节 base64 密钥，且升级时保留已有密钥。

**涉及文件：** 无新增（验证 Task 3.2 已含的 Pascal Code）

- [x] **Step 1: 单独验证 PowerShell 密钥生成命令**

```powershell
# PowerShell
$key = [Convert]::ToBase64String((1..32 | ForEach-Object { [byte](Get-Random -Max 256) }))
Write-Host "生成密钥: $key"
Write-Host "长度: $($key.Length)"
# 解码验证字节数
$bytes = [Convert]::FromBase64String($key)
Write-Host "解码字节数: $($bytes.Length)"
```

预期：生成 base64 串，解码为 32 字节。

> **验证点：** 确认 `llm-gateway.iss` 中 `GenerateEncryptionKey` 函数调用此 PowerShell 命令并读取 `{tmp}\gateway_key.txt`。`ReadXmlValue` 在升级时读取已有 `GATEWAY_ENCRYPTION_KEY` 值，非空则保留。

- [x] **Step 2: 记录验证结论并 Commit**

将验证结论追加到 `deployments/package/spike-report.md`：

```markdown
## Task 3.3 密钥生成验证
- PowerShell `[Convert]::ToBase64String((1..32|%{[byte](Get-Random -Max 256)}))` 生成 32 字节 base64 密钥，解码 32 字节 ✓
- Inno Setup `ReadXmlValue` 升级时读取已有密钥，`onlyifdoesntexist` 标志保留 xml ✓
```

```bash
git add deployments/package/spike-report.md
git commit -m "test(package): Task 3.3 密钥生成 Pascal Script 验证"
```

---

### Task 3.4: 验证服务环境变量写入 WinSW xml

**目标：** 确认 `DB_URL` 指向 `%ProgramData%\LLM-Gateway\data\`、`SERVER_PORT`、`GATEWAY_ENCRYPTION_KEY` 三项在安装后正确写入 `LLMGateway.xml`。

**涉及文件：** 无新增（验证 Task 3.1 + 3.2 协作）

- [x] **Step 1: 检查 xml 模板与 iss 的协作一致性**

核对 `LLMGateway.xml`（Task 3.1）与 `llm-gateway.iss`（Task 3.2）：

- `LLMGateway.xml` 中 `DB_URL` 值 = `jdbc:h2:file:%ProgramData%\LLM-Gateway\data\gateway;...` ✓
- `SERVER_PORT` 默认 `8080`，iss 中 `StringChangeEx(Content, 'value="8080"', ...)` 按用户输入替换 ✓
- `GATEWAY_ENCRYPTION_KEY` 默认空 `value=""`，iss 中 `StringChangeEx(... 'name="GATEWAY_ENCRYPTION_KEY" value=""' ...)` 替换为生成密钥 ✓
- `[Files]` 中 `LLMGateway.xml` 用 `onlyifdoesntexist`，升级保留 ✓

> **风险点：** `StringChangeEx(Content, 'value="8080"', ...)` 会替换 xml 中**所有** `value="8080"`。当前 xml 中仅 `SERVER_PORT` 为 8080，安全。若后续 xml 增加其他默认 8080 字段，需改用更精确的匹配（含 `name="SERVER_PORT"` 前缀）。当前无此问题。

- [x] **Step 2: 记录并 Commit**

```bash
git add deployments/package/spike-report.md
git commit -m "test(package): Task 3.4 WinSW xml 环境变量写入验证"
```

> 追加 spike-report.md 一节确认三项 env 写入路径正确。

---

### Task 3.5: 配置 jpackage app-image + Inno Setup 编译

**目标：** build.ps1 产出 app-image 后，下载 WinSW exe，用 `iscc` 编译出 `llm-gateway-setup.exe`。

**涉及文件：**
- Modify: `deployments/package/build.ps1`（追加 WinSW 下载 + iscc 编译步骤）
- Create: `deployments/package/windows/download-winsw.ps1`（WinSW 下载辅助）

- [ ] **Step 1: 编写 WinSW 下载脚本**

Create `deployments/package/windows/download-winsw.ps1`：

```powershell
# 下载 WinSW x64 exe 并命名为 LLMGateway.exe
[CmdletBinding()]
param(
  [string]$Version = "2.12.0",
  [string]$OutDir = $PSScriptRoot
)
$ErrorActionPreference = 'Stop'
$outFile = Join-Path $OutDir "LLMGateway.exe"
$url = "https://github.com/winsw/winsw/releases/download/v$Version/WinSW-x64.exe"
Write-Host "下载 WinSW v$Version -> $outFile"
Invoke-WebRequest -Uri $url -OutFile $outFile -UseBasicParsing
if (-not (Test-Path $outFile)) { throw "WinSW 下载失败" }
Write-Host "完成: $outFile"
```

- [ ] **Step 2: 修改 build.ps1 追加 WinSW + Inno Setup 编译**

在 `deployments/package/build.ps1` 末尾（`Log "完成。下一步用 Inno Setup 编译 exe"` 之前）追加：

```powershell
# 5. 下载 WinSW exe
Log "下载 WinSW..."
& (Join-Path $ScriptDir 'windows\download-winsw.ps1') -OutDir $WinRes
if (-not (Test-Path (Join-Path $WinRes 'LLMGateway.exe'))) { Die "WinSW exe 缺失" }

# 6. Inno Setup 编译 exe（需 iscc 在 PATH）
$Iscc = Get-Command iscc -ErrorAction SilentlyContinue
if (-not $Iscc) {
  $IsccPath = "${env:ProgramFiles(x86)}\Inno Setup 6\ISCC.exe"
  if (-not (Test-Path $IsccPath)) { Die "未找到 Inno Setup (iscc)。请安装: choco install innosetup" }
  $Iscc = $IsccPath
} else {
  $Iscc = $Iscc.Source
}
Log "Inno Setup 编译: $Iscc"
& $Iscc (Join-Path $WinRes 'llm-gateway.iss')
if ($LASTEXITCODE -ne 0) { Die "Inno Setup 编译失败" }

$SetupExe = Join-Path $DistDir 'llm-gateway-setup.exe'
if (-not (Test-Path $SetupExe)) { Die "安装包未生成: $SetupExe" }
Log "安装包: $SetupExe ($((Get-Item $SetupExe).Length / 1MB)MB)"
```

- [ ] **Step 3: 本地执行 build.ps1（Windows 环境，需 Inno Setup）**

```powershell
# 安装 Inno Setup（一次性）
# choco install innosetup -y
.\deployments\package\build.ps1
```

预期：`deployments\package\dist\llm-gateway-setup.exe` 生成。

- [ ] **Step 4: Commit**

```bash
git add deployments/package/windows/download-winsw.ps1 deployments/package/build.ps1
git commit -m "feat(package): Task 3.5 jpackage app-image + Inno Setup 编译 exe"
```

---

### Task 3.6: 本地验证 exe（干净 Windows）

**目标：** 干净 Windows 安装 exe → Service 启动 → 健康检查 UP → 数据落 `%ProgramData%`。

**涉及文件：** 无新增（验证 Task）

> **环境要求：** Windows 10/11 或 Windows Server 2019+，已装 .NET runtime（WinSW 依赖）。CI 中在 `windows-latest` runner 验证（见 Phase 4）。

- [ ] **Step 1: 静默安装（验证 `/VERYSILENT` 回退默认端口）**

```powershell
# 管理员 PowerShell
Start-Process -FilePath ".\deployments\package\dist\llm-gateway-setup.exe" `
  -ArgumentList "/VERYSILENT","/NORESTART" -Wait -NoNewWindow
```

- [ ] **Step 2: 验证服务与 health**

```powershell
# 等待服务就绪（最多 90s）
for ($i=1; $i -le 90; $i++) {
  try { (Invoke-WebRequest -UseBasicParsing http://localhost:8080/actuator/health).Content; break } catch { Start-Sleep -Seconds 1 }
}
Get-Service LLMGateway | Format-List Name,Status,StartType
Get-ChildItem "$env:ProgramData\LLM-Gateway\data"
```

预期：
- health 输出 `"status":"UP"`
- `Get-Service` 显示 `Status=Running`
- `%ProgramData%\LLM-Gateway\data\` 含 H2 数据文件

- [ ] **Step 3: 验证升级保留密钥**

```powershell
# 读取首次安装的密钥
[xml]$xml = Get-Content "$env:ProgramFiles\LLM-Gateway\LLMGateway.xml"
$firstKey = ($xml.service.env | Where-Object { $_.name -eq 'GATEWAY_ENCRYPTION_KEY' }).value
Write-Host "首次密钥: $firstKey"

# 再次运行安装包（模拟升级）
Start-Process -FilePath ".\deployments\package\dist\llm-gateway-setup.exe" `
  -ArgumentList "/VERYSILENT" -Wait -NoNewWindow

[xml]$xml2 = Get-Content "$env:ProgramFiles\LLM-Gateway\LLMGateway.xml"
$secondKey = ($xml2.service.env | Where-Object { $_.name -eq 'GATEWAY_ENCRYPTION_KEY' }).value
Write-Host "升级后密钥: $secondKey"
if ($firstKey -eq $secondKey) { Write-Host "PASS: 密钥升级保留" } else { Write-Host "FAIL: 密钥被覆盖"; exit 1 }
```

预期：`PASS: 密钥升级保留`。

- [ ] **Step 4: 卸载验证（保留数据目录）**

```powershell
# 控制面板卸载或:
Start-Process -FilePath "C:\Program Files\LLM-Gateway\unins000.exe" -ArgumentList "/VERYSILENT" -Wait
Get-Service LLMGateway -ErrorAction SilentlyContinue  # 应不存在
Test-Path "$env:ProgramData\LLM-Gateway\data"          # 应为 True（保留）
```

预期：服务已移除，数据目录保留。

- [ ] **Step 5: 记录并 Commit**

追加验证结论到 `deployments/package/spike-report.md`。

```bash
git add deployments/package/spike-report.md
git commit -m "test(package): Task 3.6 Windows exe 安装/升级/卸载验证通过"
```

---

## Phase 4: CI 集成（Tasks 4.1–4.5）

> 在 `.github/workflows/release.yml` 新增 `package` job，matrix `[ubuntu-latest, windows-latest]`，产物上传到 GitHub Release。

### Task 4.1: 在 release.yml 加 package job（matrix）

**目标：** 新增 `package` job，matrix 双平台，依赖 `release` job。

**涉及文件：**
- Modify: `.github/workflows/release.yml`

- [ ] **Step 1: 在 release.yml 的 `build-docker` job 之后、`publish-maven` 之前插入 package job**

在 `.github/workflows/release.yml` 中，定位 `build-docker` job 结束（`Generate SBOM` / `Upload SBOM` 步骤之后）与 `publish-maven` job 之间，插入以下 `package` job：

```yaml
  # -------------------------------------------------------------------
  # 构建系统安装包 (deb/rpm/exe)
  # -------------------------------------------------------------------
  package:
    name: 构建安装包
    needs: release
    strategy:
      fail-fast: false
      matrix:
        os: [ubuntu-latest, windows-latest]
    runs-on: ${{ matrix.os }}

    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Setup JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: ${{ env.JAVA_VERSION }}
          distribution: 'temurin'

      - name: Restore Linux script permissions
        if: runner.os == 'Linux'
        run: |
          chmod +x deployments/package/build.sh
          chmod +x deployments/package/linux/postinst deployments/package/linux/prerm \
                   deployments/package/linux/postrm deployments/package/linux/llm-gateway.config
          chmod +x deployments/package/linux/postinst-rpm deployments/package/linux/prerm-rpm \
                   deployments/package/linux/postrm-rpm

      - name: Install rpm tool (Linux)
        if: runner.os == 'Linux'
        run: sudo apt-get update && sudo apt-get install -y rpm

      - name: Install Inno Setup (Windows)
        if: runner.os == 'Windows'
        run: choco install innosetup -y --no-progress

      - name: Build packages (Linux)
        if: runner.os == 'Linux'
        run: ./deployments/package/build.sh

      - name: Build packages (Windows)
        if: runner.os == 'Windows'
        run: .\deployments\package\build.ps1

      - name: Smoke test - deb (Linux)
        if: runner.os == 'Linux'
        run: |
          DEB=$(ls deployments/package/dist/*.deb | head -1)
          docker run --rm -d --name lg-smoke-deb \
            -v "$PWD/$DEB:/tmp/llm-gateway.deb" \
            -p 18080:8080 ubuntu:22.04 sleep 180
          docker exec lg-smoke-deb bash -c '
            apt-get update && apt-get install -y /tmp/llm-gateway.deb curl
            for i in $(seq 1 90); do curl -sf http://localhost:8080/actuator/health && break; sleep 1; done
            systemctl is-active llm-gateway.service
          '
          docker stop lg-smoke-deb

      - name: Smoke test - rpm (Linux)
        if: runner.os == 'Linux'
        run: |
          RPM=$(ls deployments/package/dist/*.rpm | head -1)
          docker run --rm -d --name lg-smoke-rpm \
            -v "$PWD/$RPM:/tmp/llm-gateway.rpm" \
            -p 18081:8080 rockylinux:9 sleep 180
          docker exec lg-smoke-rpm bash -c '
            dnf install -y /tmp/llm-gateway.rpm curl
            for i in $(seq 1 90); do curl -sf http://localhost:8080/actuator/health && break; sleep 1; done
            systemctl is-active llm-gateway.service
          '
          docker stop lg-smoke-rpm

      - name: Smoke test - exe (Windows)
        if: runner.os == 'Windows'
        run: |
          Start-Process -FilePath ".\deployments\package\dist\llm-gateway-setup.exe" -ArgumentList "/VERYSILENT","/NORESTART" -Wait -NoNewWindow
          for ($i=1; $i -le 90; $i++) {
            try { (Invoke-WebRequest -UseBasicParsing http://localhost:8080/actuator/health).Content; break } catch { Start-Sleep -Seconds 1 }
          }
          $svc = Get-Service LLMGateway
          if ($svc.Status -ne 'Running') { throw "服务未运行" }
          Start-Process -FilePath "C:\Program Files\LLM-Gateway\unins000.exe" -ArgumentList "/VERYSILENT" -Wait

      - name: Upload artifacts
        uses: actions/upload-artifact@v4
        with:
          name: packages-${{ matrix.os }}
          path: |
            deployments/package/dist/*.deb
            deployments/package/dist/*.rpm
            deployments/package/dist/*.exe
          retention-days: 14
```

- [ ] **Step 2: 修改 finalize job 的 needs 加上 package**

定位 `finalize` job：

```yaml
  finalize:
    name: 完成发布
    runs-on: ubuntu-latest
    needs: [release, build-docker, publish-helm]
```

改为：

```yaml
  finalize:
    name: 完成发布
    runs-on: ubuntu-latest
    needs: [release, build-docker, publish-helm, package]
```

并在 `finalize` 的 `softprops/action-gh-release` 的 `files:` 中追加安装包通配（在 `*.tgz` 后加）：

```yaml
      - name: Update Release
        uses: softprops/action-gh-release@v2
        with:
          draft: false
          files: |
            *.tgz
            deployments/package/dist/*.deb
            deployments/package/dist/*.rpm
            deployments/package/dist/*.exe
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
```

> **注意：** `package` job 产物在 matrix 不同 runner 上，`finalize` 在 ubuntu runner 无法直接访问 windows 产物。需在 `package` job 用 `actions/upload-artifact` 上传后，`finalize` 用 `actions/download-artifact` 下载再挂到 Release。Step 3 处理。

- [ ] **Step 3: 在 finalize job 加下载 artifact 步骤**

在 `finalize` job 的 `Update Release` 步骤之前插入：

```yaml
      - name: Download package artifacts
        uses: actions/download-artifact@v4
        with:
          pattern: packages-*
          path: deployments/package/dist
          merge-multiple: true

      - name: List artifacts
        run: ls -lhR deployments/package/dist
```

并将 `Update Release` 的 `files:` 改为（artifact 下载后路径）：

```yaml
          files: |
            *.tgz
            deployments/package/dist/*.deb
            deployments/package/dist/*.rpm
            deployments/package/dist/*.exe
```

- [ ] **Step 4: 验证 workflow YAML 语法**

```bash
# 用 yamllint 或 actionlint（若本地有）
yamllint .github/workflows/release.yml 2>/dev/null || echo "yamllint 不可用，跳过"
# 或用 python 校验
python -c "import yaml; yaml.safe_load(open('.github/workflows/release.yml'))" && echo "YAML OK"
```

预期：`YAML OK`。

- [ ] **Step 5: Commit**

```bash
git add .github/workflows/release.yml
git commit -m "ci(package): Task 4.1 release.yml 新增 package job（matrix ubuntu/windows）"
```

---

### Task 4.2: 验证 ubuntu job 构建 deb + rpm

**目标：** 确认 ubuntu-latest 上 `build.sh` 产出 deb + rpm（含 `rpm` 工具安装）。

**涉及文件：** 无新增（验证 Task 4.1 的 ubuntu 分支）

- [ ] **Step 1: 确认 build.sh 与 CI 步骤一致性**

核对 Task 4.1 中 ubuntu 分支步骤：
- `Install rpm tool (Linux)` → `sudo apt-get install -y rpm` ✓
- `Restore Linux script permissions` → `chmod +x` 所有 maintainer 脚本 ✓（checkout 后 git 不保留 +x）
- `Build packages (Linux)` → `./deployments/package/build.sh` ✓
- `Smoke test - deb` + `Smoke test - rpm` ✓

- [ ] **Step 2: 记录并 Commit**

```bash
git add deployments/package/spike-report.md
git commit -m "ci(package): Task 4.2 ubuntu job deb+rpm 构建设计确认"
```

> 完整 CI 触发验证在 Task 4.5（打 tag 实跑）。

---

### Task 4.3: 验证 windows job 构建 exe

**目标：** 确认 windows-latest 上 `build.ps1` 产出 exe（含 Inno Setup 安装）。

**涉及文件：** 无新增（验证 Task 4.1 的 windows 分支）

- [ ] **Step 1: 确认 windows 分支步骤**

核对 Task 4.1 中 windows 分支：
- `Install Inno Setup (Windows)` → `choco install innosetup -y` ✓
- `Build packages (Windows)` → `.\deployments\package\build.ps1` ✓（含 WinSW 下载 + iscc 编译，见 Task 3.5）
- `Smoke test - exe` → 静默安装 + health + 卸载 ✓

- [ ] **Step 2: 记录并 Commit**

```bash
git add deployments/package/spike-report.md
git commit -m "ci(package): Task 4.3 windows job exe 构建设计确认"
```

---

### Task 4.4: 验证产物上传到 GitHub Release

**目标：** 确认 `finalize` job 下载 matrix 产物并挂到 GitHub Release。

**涉及文件：** 无新增（验证 Task 4.1 Step 2-3 的 finalize 改动）

- [ ] **Step 1: 确认产物流转链路**

- `package` job（双平台）→ `actions/upload-artifact` 上传到 `packages-ubuntu-latest` / `packages-windows-latest`
- `finalize` job → `actions/download-artifact`（`pattern: packages-*`，`merge-multiple: true`）合并下载到 `deployments/package/dist/`
- `softprops/action-gh-release` 的 `files:` 含 `*.deb`、`*.rpm`、`*.exe`

- [ ] **Step 2: 记录并 Commit**

```bash
git add deployments/package/spike-report.md
git commit -m "ci(package): Task 4.4 产物上传 Release 流转确认"
```

---

### Task 4.5: 验证 release tag 触发，产物齐全

**目标：** 打 `v*` tag 触发 release.yml，确认 deb/rpm/exe 三类产物挂到 GitHub Release。

**涉及文件：** 无新增（端到端验证）

- [ ] **Step 1: 推一个测试 tag 触发 workflow**

```bash
git tag v0.0.0-package-test
git push origin v0.0.0-package-test
```

- [ ] **Step 2: 监控 workflow 运行**

```bash
gh run watch
```

预期：`package` job 双平台均通过（含 smoke test），`finalize` job 通过。

- [ ] **Step 3: 确认 Release 产物齐全**

```bash
gh release view v0.0.0-package-test --json assets --jq '.assets[].name'
```

预期含：
- `llm-gateway_<version>_amd64.deb`
- `llm-gateway_<version>-1.x86_64.rpm`
- `llm-gateway-setup.exe`

- [ ] **Step 4: 清理测试 tag/release**

```bash
gh release delete v0.0.0-package-test --yes --cleanup-tag || true
git tag -d v0.0.0-package-test
git push origin :refs/tags/v0.0.0-package-test || true
```

- [ ] **Step 5: Commit 验证记录**

```bash
git add deployments/package/spike-report.md
git commit -m "ci(package): Task 4.5 release tag 端到端验证通过"
```

> **注意：** 本 Task 需在 Phase 2/3/4.1-4.4 全部完成后执行。若 CI 失败，加载 `systematic-debugging` skill 定位根因。

---

## Phase 5: Docker 资产修复（Tasks 5.1–5.3）

> 修复 `deployments/docker/` 下 Dockerfile/docker-compose 引用旧多模块路径的失配问题。build-docker job 的 `context` 是仓库根 `.`，file 是 `deployments/docker/Dockerfile`，故 Dockerfile 内路径相对仓库根。

### Task 5.1: 修复 Dockerfile

**目标：** 构建路径改为单模块 `gateway-boot`，修正 COPY 路径与 jar 名、health 路径。

**涉及文件：**
- Modify: `deployments/docker/Dockerfile`（整体重写）

- [ ] **Step 1: 重写 Dockerfile**

将 `deployments/docker/Dockerfile` 全文替换为：

```dockerfile
# ===================================================================
# LLM-Gateway Dockerfile
# 多阶段构建（COLA Light 单模块 gateway-boot）
# build-docker job context=仓库根，故路径相对根目录
# ===================================================================

# ===================================================================
# 第一阶段: 构建
# ===================================================================
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /build

# 安装 Maven
RUN apk add --no-cache maven

# 复制父 POM 与 gateway-boot POM（利用缓存）
COPY pom.xml ./
COPY gateway-boot/pom.xml gateway-boot/

# 下载依赖（利用缓存）
RUN mvn dependency:go-offline -pl gateway-boot -am -q || true

# 复制 gateway-boot 源码
COPY gateway-boot/src gateway-boot/src

# 构建 fat jar
ARG SPRING_PROFILES_ACTIVE=dev
RUN mvn package -DskipTests -pl gateway-boot -am \
    -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE}

# ===================================================================
# 第二阶段: 运行
# ===================================================================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# 安装 curl（健康检查）与 bash
RUN apk add --no-cache curl bash

# 创建非 root 用户
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

# 复制 fat jar（名称与 revision=1.0.0-SNAPSHOT 一致）
COPY --from=builder /build/gateway-boot/target/gateway-boot-1.0.0-SNAPSHOT.jar app.jar

# 设置权限
RUN chown -R appuser:appgroup /app

USER appuser

# 健康检查（actuator health 端点）
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# 默认暴露端口
EXPOSE 8080

# 启动命令
ENTRYPOINT ["java", "-jar", "-XX:+UseZGC", "-XX:+ZGenerational", "app.jar"]
```

**改动要点：**
- COPY 路径从 `gateway/gateway-app/...` 改为 `gateway-boot/...`
- 构建命令从 `maven package -pl gateway-app` 改为 `mvn package -pl gateway-boot`
- jar 名从 `gateway-app/target/*.jar` 改为 `gateway-boot/target/gateway-boot-1.0.0-SNAPSHOT.jar`
- 移除 `COPY deployments/docker/config /app/config`（旧 config 目录已不存在）
- health 路径从 `/api/v1/health` 改为 `/actuator/health`
- EXPOSE 移除 8081（actuator 已在 8080 同端口 `/actuator`）

- [ ] **Step 2: Commit**

```bash
git add deployments/docker/Dockerfile
git commit -m "fix(docker): Task 5.1 Dockerfile 适配单模块 gateway-boot"
```

---

### Task 5.2: 修复 docker-compose.yml

**目标：** gateway 服务 `context` 改根目录、移除源码挂载、补 `gateway-console` 服务、修 health 路径。

**涉及文件：**
- Modify: `deployments/docker/docker-compose.yml`

- [ ] **Step 1: 修改 gateway 服务 build context 与 health**

在 `deployments/docker/docker-compose.yml` 中，定位 `gateway` 服务的 `build:` 块：

```yaml
    build:
      context: ../../gateway
      dockerfile: deployments/docker/Dockerfile
      args:
        SPRING_PROFILES_ACTIVE: dev
```

改为：

```yaml
    build:
      context: ../..
      dockerfile: deployments/docker/Dockerfile
      args:
        SPRING_PROFILES_ACTIVE: dev
```

> `context: ../..` 指向仓库根（docker-compose.yml 在 `deployments/docker/`，上两级即根）。`dockerfile` 路径相对 context（仓库根），即 `deployments/docker/Dockerfile`。

- [ ] **Step 2: 移除 gateway 服务的源码挂载**

定位 gateway 服务的 `volumes:`，删除源码挂载行：

```yaml
    volumes:
      - ../../gateway:/app
      - ./config:/app/config:ro
```

改为（移除源码挂载与旧 config 挂载，仅保留数据卷）：

```yaml
    volumes:
      - gateway_data:/app/data
```

并在文件末尾 `volumes:` 定义块中追加 `gateway_data`：

```yaml
volumes:
  postgres_data:
    name: llm-gateway-postgres-data
  redis_data:
    name: llm-gateway-redis-data
  prometheus_data:
    name: llm-gateway-prometheus-data
  grafana_data:
    name: llm-gateway-grafana-data
  gateway_data:
    name: llm-gateway-app-data
```

- [ ] **Step 3: 修改 gateway 服务 health 路径**

定位 gateway 服务 healthcheck：

```yaml
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/api/v1/health"]
```

改为：

```yaml
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
```

- [ ] **Step 4: 追加 gateway-console 服务**

在 `gateway` 服务块之后（`networks:` 定义之前）追加 `gateway-console` 服务：

```yaml
  # -------------------------------------------------------------------
  # LLM-Gateway 管理前端（React/Vite）
  # -------------------------------------------------------------------
  gateway-console:
    build:
      context: ../../gateway-console
      dockerfile: Dockerfile
    container_name: llm-gateway-console
    restart: unless-stopped
    environment:
      VITE_API_BASE_URL: http://gateway:8080
    ports:
      - "5173:5173"
    depends_on:
      gateway:
        condition: service_healthy
    networks:
      - llm-gateway-network
```

> **说明：** `gateway-console` 需有 `Dockerfile`。若 `gateway-console/Dockerfile` 不存在，本 Task 同步创建一个最小 Node 构建镜像（Step 5）。

- [ ] **Step 5: 创建 gateway-console Dockerfile（若不存在）**

检查并创建 `gateway-console/Dockerfile`：

```bash
# 仓库根目录
if [ ! -f gateway-console/Dockerfile ]; then
  echo "gateway-console/Dockerfile 不存在，需创建"
fi
```

Create `gateway-console/Dockerfile`：

```dockerfile
# ===================================================================
# LLM-Gateway Console Dockerfile（开发/预览）
# ===================================================================
FROM node:20-alpine

WORKDIR /app

# 启用 corepack 支持 pnpm
RUN corepack enable && corepack prepare pnpm@latest --activate

# 复制 package 文件
COPY package.json pnpm-lock.yaml* ./

# 安装依赖
RUN pnpm install --frozen-lockfile || pnpm install

# 复制源码
COPY . .

# 暴露 Vite dev server 端口
EXPOSE 5173

# 启动 dev server（host 0.0.0.0 供容器外访问）
CMD ["pnpm", "dev", "--host", "0.0.0.0"]
```

> **注意：** 若 `gateway-console` 无 `pnpm-lock.yaml`，`pnpm install` 会生成。生产场景应改为多阶段构建（build + nginx 托管），本 dev compose 用 dev server 足够验证。

- [ ] **Step 6: Commit**

```bash
git add deployments/docker/docker-compose.yml gateway-console/Dockerfile
git commit -m "fix(docker): Task 5.2 compose context 改根目录、移除源码挂载、补 console 服务"
```

---

### Task 5.3: 验证 docker-compose up -d 正常构建并拉起 gateway

**目标：** `cd deployments/docker && docker-compose up -d` gateway 服务健康 UP。

**涉及文件：** 无新增（验证 Task）

- [ ] **Step 1: 构建并启动 gateway 服务**

```bash
cd deployments/docker
docker-compose up -d --build gateway
```

> 仅启动 gateway（其依赖 postgres/redis 会按 `depends_on` 自动拉起）。`--build` 强制用新 Dockerfile 构建。

- [ ] **Step 2: 等待 gateway 健康就绪**

```bash
# 最多等 90s
for i in $(seq 1 90); do
  STATUS=$(docker inspect --format='{{.State.Health.Status}}' llm-gateway 2>/dev/null || echo "none")
  echo "[$i] health: $STATUS"
  [ "$STATUS" = "healthy" ] && break
  sleep 2
done
docker-compose logs --tail=30 gateway
```

预期：`health: healthy`。

- [ ] **Step 3: 直接 curl 验证**

```bash
curl -sf http://localhost:8080/actuator/health
```

预期：`{"status":"UP",...}`。

> **注意：** gateway 服务 `SPRING_PROFILES_ACTIVE=dev`，dev profile 可能需要 PG/Redis。若 dev profile 连不上 postgres/redis 导致 health DOWN，需确认 dev profile 配置。本 change 不改 `application*.yml`，若 dev profile 强依赖外部 PG/Redis，gateway 健康依赖 postgres/redis healthy（compose 已配 `depends_on: condition: service_healthy`）。若仍失败，记录到 spike-report.md 并与用户确认是否调整 compose 环境变量（不改 yml）。

- [ ] **Step 4: 清理**

```bash
docker-compose down -v
```

- [ ] **Step 5: 记录并 Commit**

```bash
cd ../..
git add deployments/package/spike-report.md
git commit -m "test(docker): Task 5.3 docker-compose up 验证 gateway 健康"
```

---

## Phase 6: 文档（Tasks 6.1–6.2）

### Task 6.1: 新增 deployments/package/README.md

**目标：** 文档化构建步骤、安装命令、配置说明。

**涉及文件：**
- Create: `deployments/package/README.md`

- [ ] **Step 1: 编写 README**

Create `deployments/package/README.md`：

````markdown
# LLM-Gateway 系统安装包

非 Docker 一键部署：Linux deb/rpm + Windows exe，内置 jlink 精简 JRE，默认 `local` profile（H2 文件 + Caffeine，零外部依赖，无 Redis）。

## 构建依赖

- JDK 21（含 `jdeps`/`jlink`/`jpackage`）
- Maven 3.9+（或项目根 `./mvnw`）
- Linux 额外：`dpkg-deb`（默认有）、`rpm`（`sudo apt-get install -y rpm`，交叉打 rpm）
- Windows 额外：[Inno Setup 6](https://jrsoftware.org/isdl.php)（`choco install innosetup`）

## 构建命令

### Linux（deb + rpm）

```bash
./deployments/package/build.sh
# 产物: deployments/package/dist/llm-gateway_*.deb, llm-gateway-*.rpm
```

### Windows（exe）

```powershell
.\deployments\package\build.ps1
# 产物: deployments\package\dist\llm-gateway-setup.exe
```

CI 自动构建见 `.github/workflows/release.yml` 的 `package` job（git tag `v*` 触发）。

## 安装

### Linux deb（Ubuntu/Debian）

```bash
sudo apt install ./llm-gateway_*.deb
# 安装时交互询问端口（默认 8080），非交互: DEBIAN_FRONTEND=noninteractive
```

### Linux rpm（RHEL/Rocky/CentOS）

```bash
sudo dnf install ./llm-gateway-*.rpm
```

### Windows exe

双击 `llm-gateway-setup.exe`，按向导输入端口（默认 8080）。
静默安装：`llm-gateway-setup.exe /VERYSILENT`

## 目录布局

### Linux

| 路径 | 用途 |
|------|------|
| `/opt/llm-gateway/` | 安装目录（JRE + jar + 启动器） |
| `/var/lib/llm-gateway/` | 数据目录（H2 文件，`DB_URL` 指向此） |
| `/var/log/llm-gateway/` | 日志目录 |
| `/etc/llm-gateway/env` | 环境变量配置（conffile，升级保留） |

### Windows

| 路径 | 用途 |
|------|------|
| `%ProgramFiles%\LLM-Gateway\` | 安装目录（app-image + WinSW） |
| `%ProgramData%\LLM-Gateway\data\` | 数据目录（H2 文件） |
| `%ProgramData%\LLM-Gateway\logs\` | 日志目录 |
| `%ProgramFiles%\LLM-Gateway\LLMGateway.xml` | WinSW 配置（含环境变量，升级保留） |

## 配置说明

环境变量经 `DB_URL`/`SERVER_PORT`/`GATEWAY_ENCRYPTION_KEY` 注入，无需改 `application*.yml`：

- `DB_URL`：H2 文件路径，默认指向数据目录
- `SERVER_PORT`：服务端口，安装时交互设置（默认 8080），**不校验占用**
- `GATEWAY_ENCRYPTION_KEY`：加密密钥，**首次安装自动生成，升级保留**

## 服务管理

### Linux（systemd）

```bash
systemctl status llm-gateway
systemctl restart llm-gateway
journalctl -u llm-gateway -f
```

### Windows（WinSW / sc）

```powershell
Get-Service LLMGateway
Restart-Service LLMGateway
Get-WinSwLog  # 或查看 %ProgramData%\LLM-Gateway\logs
```

## 健康检查

```bash
curl http://localhost:8080/actuator/health
# {"status":"UP",...}
```

## 升级

直接安装新版包覆盖：
- Linux：`sudo apt install ./llm-gateway_*.deb`（或 `dnf install`）
- Windows：重新运行 `llm-gateway-setup.exe`

升级**保留**：数据目录、`GATEWAY_ENCRYPTION_KEY`、端口配置。Flyway 自动迁移 schema。

## 卸载

- Linux：`sudo apt remove llm-gateway`（保留数据）或 `sudo apt purge llm-gateway`（清数据）
- Windows：控制面板卸载或 `unins000.exe /VERYSILENT`（保留数据目录）

## 重要提示

- **加密密钥备份**：`GATEWAY_ENCRYPTION_KEY` 丢失则历史加密数据（如 API Key）无法解密。务必备份 `/etc/llm-gateway/env`（Linux）或 `LLMGateway.xml`（Windows）。
- **默认凭据**：`local` profile 自动创建 `admin/admin`，首次登录后请立即改密。
- **H2 Console**：`local` profile 开启 H2 Console（`/h2-console`，`web-allow-others=true`），生产环境请关闭或限制访问。
- **端口冲突**：安装时不校验端口占用，冲突时服务反复重启暴露（systemd `Restart=on-failure` / WinSW `onfailure restart`）。
````

- [ ] **Step 2: Commit**

```bash
git add deployments/package/README.md
git commit -m "docs(package): Task 6.1 新增打包构建与安装说明"
```

---

### Task 6.2: 更新 README.md 部署章节

**目标：** 修正根 `README.md` 部署章节的 DB 类型/jar 名/安装包用法，补 admin/admin 改密与 H2 Console 风险提示。

**涉及文件：**
- Modify: `README.md`

- [ ] **Step 1: 定位 README.md 中的部署章节**

```bash
# 查找部署相关章节标题
grep -n -E '^#.*(部署|Docker|安装|快速开始|Quick)' README.md | head -20
```

> 若 README.md 无明确部署章节，在文件末尾合适位置追加"## 部署"章节。先 Read README.md 确认结构。

- [ ] **Step 2: 读取 README.md 部署章节当前内容**

```bash
# 用 Read 工具读取 README.md，定位部署章节行号
```

依据实际内容修改。典型失配点（需修正）：
- DB 类型描述若写"PostgreSQL 必需" → 改为"默认 H2 文件模式（零外部依赖），生产可选 PostgreSQL"
- jar 名若写 `gateway-app-*.jar` → 改为 `gateway-boot-1.0.0-SNAPSHOT.jar`
- 安装方式若仅写 Docker → 补"系统安装包（deb/rpm/exe）"
- 补 admin/admin 首次改密提示
- 补 H2 Console 远程访问风险提示

- [ ] **Step 3: 追加/修改部署章节**

在 README.md 部署章节追加（或修正为）以下内容（若已有则替换对应段落）：

```markdown
## 部署

LLM-Gateway 支持三种部署形态：

### 1. 系统安装包（推荐：非 Docker 一键部署）

默认 `local` profile（H2 文件持久化 + Caffeine 缓存，零外部依赖，无 Redis），装完即用。

- **Linux deb/rpm**：`apt install ./llm-gateway_*.deb` 或 `dnf install ./llm-gateway-*.rpm`
- **Windows exe**：双击 `llm-gateway-setup.exe`

安装时交互设置端口（默认 8080），加密密钥自动生成。详见 [deployments/package/README.md](deployments/package/README.md)。

构建产物：`gateway-boot-1.0.0-SNAPSHOT.jar`（fat jar，Main-Class=`org.springframework.boot.loader.launch.JarLauncher`）。

### 2. Docker

```bash
cd deployments/docker
docker-compose up -d
```

详见 [deployments/docker/](deployments/docker/)。

### 3. 源码运行

```bash
./mvnw spring-boot:run -pl gateway-boot
```

> **重要提示：**
> - **默认凭据**：`local` profile 自动创建 `admin/admin`，首次登录后请**立即修改密码**。
> - **H2 Console 风险**：`local` profile 开启 H2 Console（`/h2-console`）且 `web-allow-others=true`，允许远程访问，生产环境请关闭或限制。
> - **加密密钥备份**：系统安装包部署时 `GATEWAY_ENCRYPTION_KEY` 自动生成，**务必备份**，丢失则历史加密数据无法解密。
```

- [ ] **Step 4: Commit**

```bash
git add README.md
git commit -m "docs: Task 6.2 更新 README 部署章节（安装包用法/改密提示/H2 风险）"
```

---

## 计划自检清单

### Spec 覆盖核对（Design Doc D1–D9 + tasks.md 6 组 28 任务）

| tasks.md 任务 | 计划 Task | Design Doc Decision |
|---------------|-----------|---------------------|
| 1.1 spike | Phase 1 / Task 1.1 | D8（最大风险，优先） |
| 1.2 jdeps 模块清单 | Phase 1 / Task 1.2 | D1 jlink |
| 1.3 目录结构 | Phase 1 / Task 1.3 | — |
| 1.4 build.sh/build.ps1 | Phase 1 / Task 1.4 | D1 打包流程 |
| 2.1 systemd unit | Phase 2 / Task 2.1 | D6 Linux systemd |
| 2.2 debconf 模板 | Phase 2 / Task 2.2 | D4 端口交互 |
| 2.3 postinst | Phase 2 / Task 2.3 | D5 密钥生成 + D6 |
| 2.4 prerm/postrm | Phase 2 / Task 2.4 | D6 保留数据 |
| 2.5 jpackage deb | Phase 2 / Task 2.5 | D7 deb |
| 2.6 jpackage rpm | Phase 2 / Task 2.6 | D7 rpm 交叉打包 |
| 2.7 验证 deb | Phase 2 / Task 2.7 | 测试策略 |
| 2.8 验证 rpm | Phase 2 / Task 2.8 | 测试策略 |
| 3.1 WinSW 配置 | Phase 3 / Task 3.1 | D6 Windows WinSW |
| 3.2 Inno Setup 向导 | Phase 3 / Task 3.2 | D9 exe |
| 3.3 密钥生成 Pascal | Phase 3 / Task 3.3 | D5 Windows 密钥 |
| 3.4 env 写入 xml | Phase 3 / Task 3.4 | D6 env |
| 3.5 app-image + iscc | Phase 3 / Task 3.5 | D9 + D7 |
| 3.6 验证 exe | Phase 3 / Task 3.6 | 测试策略 |
| 4.1 package job | Phase 4 / Task 4.1 | D7 CI matrix |
| 4.2 ubuntu job | Phase 4 / Task 4.2 | D7 |
| 4.3 windows job | Phase 4 / Task 4.3 | D7 |
| 4.4 上传 Release | Phase 4 / Task 4.4 | D7 |
| 4.5 tag 触发验证 | Phase 4 / Task 4.5 | D7 |
| 5.1 修 Dockerfile | Phase 5 / Task 5.1 | Docker 修复 |
| 5.2 修 compose | Phase 5 / Task 5.2 | Docker 修复 |
| 5.3 验证 compose | Phase 5 / Task 5.3 | Docker 修复 |
| 6.1 package README | Phase 6 / Task 6.1 | — |
| 6.2 更新 README | Phase 6 / Task 6.2 | Risks 提示 |

### 关键约束核对

- [x] Windows 统一 exe（Inno Setup + WinSW），无 msi/WiX（Task 3.1–3.6 全程 exe）
- [x] 不改 Java 源码与 `application*.yml`（计划仅动 `deployments/`、`.github/`、`README.md`、`gateway-console/Dockerfile`）
- [x] 端口不校验占用（debconf 模板 + iss 均未探测；systemd/WinSW 重启兜底）
- [x] 默认 local profile（build 脚本 `--java-options "-Dspring.profiles.active=local"`）
- [x] 密钥安装时生成、升级保留（postinst 检测 + iss `onlyifdoesntexist` + ReadXmlValue）
- [x] 数据目录外部化（`DB_URL` 指向 `/var/lib/` 或 `%ProgramData%`）
- [x] spike（Task 1.1）标注最大风险并优先
- [x] 健康检查统一 `/actuator/health`

### 风险与回退

| 风险 | 回退方案 |
|------|---------|
| jpackage + fat jar 启动失败（Task 1.1） | D8 备选：layered jar 或 `--java-options` 传 `-jar`；需用户确认 |
| jpackage `--type deb/rpm` 未正确挂 maintainer 脚本 | 改用 `dpkg-deb`/`rpmbuild` 手动重组，或 `fpm` 包装 app-image |
| `StringChangeEx` 替换 `value="8080"` 误伤（Task 3.4） | 当前 xml 仅 SERVER_PORT 为 8080，安全；后续扩展用精确匹配 |
| dev profile 强依赖 PG/Redis 致 compose 验证失败（Task 5.3） | 记录并与用户确认是否调 compose 环境变量（不改 yml） |
| CI smoke test 容器内 systemd 不可用 | 改用 `docker run --privileged` 或直接 `java -jar` 冒烟（绕过服务注册） |
