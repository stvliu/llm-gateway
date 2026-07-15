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