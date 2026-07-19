#!/usr/bin/env bash
# =============================================================================
# LLM-Gateway 跨平台安装包构建脚本（JReleaser 方案）
# 产出: deb（DebAssembler 纯 Java）+ zip（ArchiveAssembler）+ rpm（JpackageAssembler，需 rpmbuild）
# 用法: ./deployments/package/build.sh [--skip-mvn]
#
# 流程: mvn package -> jlink 双平台 JRE -> 预复制 jar -> jreleaser:assemble 出 deb/zip/rpm
# 依赖: JDK 21（含 jlink）、Maven（mvnw）
# 本地（Windows, SNAPSHOT）: 出 deb + zip（jpackage active=RELEASE 跳过 rpm，本地无 rpmbuild）
# CI（release）: 出 deb + zip + rpm（CI runner 含 rpmbuild）
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

# 颜色输出：step 醒目步骤标题（编号），log 步骤内子信息，err 错误退出
STEP=0
TOTAL=7
step() { STEP=$((STEP+1)); echo -e "\n\033[1;36m=== [$STEP/$TOTAL] $* ===\033[0m"; }
log()  { echo -e "\033[32m[build]\033[0m $*"; }
ok()   { echo -e "\033[32m✓\033[0m $*"; }
err()  { echo -e "\033[31m[error]\033[0m $*" >&2; exit 1; }

# 校验 JAVA_HOME 指向 JDK 21（jlink 需要 jmods 目录）
if [[ -z "${JAVA_HOME:-}" ]]; then
  err "JAVA_HOME 未设置（jlink 需要 JDK 21）。示例：export JAVA_HOME=/d/Java/jdk-21.0.10（Git Bash）或 D:\\Java\\jdk-21.0.10（cmd）"
fi
JMODS_DIR="${JAVA_HOME}/jmods"
[[ -d "$JMODS_DIR" ]] || err "jmods 不存在: $JMODS_DIR（请确认 JAVA_HOME 指向 JDK 21）"

MODULES="$(tr -d '\r\n' < "$MODULES_FILE")"

# 1. 构建 fat jar
if [[ "${1:-}" != "--skip-mvn" ]]; then
  step "构建 fat jar（mvn package）"
  (cd "$REPO_ROOT" && ./mvnw clean package -pl gateway-boot -am -DskipTests)
else
  step "跳过 Maven 构建（--skip-mvn）"
fi
[[ -f "$FAT_JAR" ]] || err "fat jar 不存在: $FAT_JAR"
ok "fat jar: $FAT_JAR"

# 2. jlink 生成 Windows JRE（本机 jmods，给 zip 用）
step "生成 Windows JRE（jlink 本机 jmods）"
rm -rf "$JRE_WIN_DIR"
jlink \
  --module-path "$JMODS_DIR" \
  --add-modules "$MODULES" \
  --strip-debug --no-header-files --no-man-pages \
  --compress=2 \
  --output "$JRE_WIN_DIR"
ok "Windows JRE 体积: $(du -sh "$JRE_WIN_DIR" | cut -f1)"

# 3. jlink 生成 Linux JRE（交叉生成：下载 Linux Temurin JDK 21 取 jmods，给 deb/rpm 用）
#    Design §4.3 方案 1。若交叉生成失败，回退方案 2（下载预构建 Linux JRE）。
step "生成 Linux JRE（jlink 交叉生成）"
rm -rf "$JRE_DIR"
LINUX_JDK_DIR="$SCRIPT_DIR/.linux-jdk"
LINUX_JMODS="$LINUX_JDK_DIR/jmods"

if [[ ! -d "$LINUX_JMODS" ]]; then
  log "下载 Linux Temurin JDK 21（用于交叉生成 Linux JRE 的 jmods）..."
  mkdir -p "$LINUX_JDK_DIR"
  # Adoptium Temurin JDK 21 Linux x64 tarball（清华镜像，规避 GitHub SSL 问题）
  TEMURIN_URL="https://mirrors.tuna.tsinghua.edu.cn/Adoptium/21/jdk/x64/linux/OpenJDK21U-jdk_x64_linux_hotspot_21.0.11_10.tar.gz"
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
ok "Linux JRE 体积: $(du -sh "$JRE_DIR" | cut -f1)"

# 4. 准备 dist + 预复制 jar
step "准备 dist 目录 + 预复制 jar"
rm -rf "$DIST_DIR"
mkdir -p "$DIST_DIR"
# 预复制 fat jar 为固定名 llmgateway.jar（jreleaser.yml 的 artifacts/mainJar 引用此固定名）
STAGED_JAR="$REPO_ROOT/gateway-boot/target/llmgateway.jar"
cp "$FAT_JAR" "$STAGED_JAR"
ok "预复制 jar: $STAGED_JAR"

# 5. 确保 WinSW.exe 就绪（archive assembler 包含它，CI 全新 checkout 后需下载）
#    download-winsw.ps1 的下载逻辑在此用 curl 跨平台复现（Windows Git Bash / Linux CI 均可用）
step "确保 WinSW.exe 就绪"
WINSW_EXE="$SCRIPT_DIR/windows/WinSW.exe"
WINSW_VERSION="2.12.0"
if [[ ! -f "$WINSW_EXE" ]]; then
  log "下载 WinSW v${WINSW_VERSION}（archive assembler 依赖）..."
  curl -fsSL "https://github.com/winsw/winsw/releases/download/v${WINSW_VERSION}/WinSW-x64.exe" -o "$WINSW_EXE" \
    || err "下载 WinSW 失败（检查网络或手动放置 $WINSW_EXE）"
  [[ -f "$WINSW_EXE" ]] || err "WinSW.exe 下载后仍不存在: $WINSW_EXE"
else
  log "WinSW.exe 已存在，跳过下载"
fi

# 6. JReleaser assemble 出 deb + zip + rpm（Java 21，纯 Maven）
#    configFile 由 gateway-boot/pom.xml 的 pkg profile 指定（指向本目录 jreleaser.yml）
#    basedir = repo root（jreleaser-maven-plugin 1.25.0 实测，日志 "basedir set to <repo root>"）
#    deb: DebAssembler 纯 Java（本地可验证）；zip: ArchiveAssembler；rpm: JpackageAssembler（active=RELEASE，本地 SNAPSHOT 跳过，留 CI release）
step "JReleaser assemble（出 deb + zip [+ rpm if release]）"
JRELEASER_OUT="$REPO_ROOT/gateway-boot/target/jreleaser/assemble"
(cd "$REPO_ROOT" && JRELEASER_PROJECT_VERSION="${APP_VERSION}" \
  ./mvnw jreleaser:assemble -pl gateway-boot -Ppkg \
  -Djreleaser.project.version="${APP_VERSION}")

# 7. 复制产物到 dist + 汇总（rpm 仅 release 时产出，本地 SNAPSHOT 无 rpm 正常）
step "复制产物到 dist + 汇总"
find "$JRELEASER_OUT" -name "*.deb" -exec cp {} "$DIST_DIR" \;
find "$JRELEASER_OUT" -name "*.zip" -exec cp {} "$DIST_DIR" \;
find "$JRELEASER_OUT" -name "*.rpm" -exec cp {} "$DIST_DIR" \; 2>/dev/null || true

echo ""
ok "构建完成。产物目录: $DIST_DIR"
ls -lh "$DIST_DIR"
