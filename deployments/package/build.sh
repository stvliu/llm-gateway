#!/usr/bin/env bash
# =============================================================================
# LLM-Gateway Linux 安装包构建脚本
# 产出: deb + rpm（含 jlink 精简 JRE）
# 用法: ./deployments/package/build.sh [--skip-mvn]
#
# 流程: mvn package -> jlink 精简 JRE -> jpackage 打 deb/rpm
# 依赖: JDK 21（含 jlink/jpackage）、Maven（mvnw）、可选 rpm 工具（打 rpm 时）
# =============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
MODULES_FILE="$SCRIPT_DIR/jlink-modules.txt"
LINUX_RES="$SCRIPT_DIR/linux"

# 版本号（从 Maven 读取，如 1.0.0-SNAPSHOT）
APP_VERSION="$(cd "$REPO_ROOT" && ./mvnw help:evaluate -Dexpression=project.version -q -DforceStdout 2>/dev/null)"
JAR_NAME="gateway-boot-${APP_VERSION}.jar"
FAT_JAR="$REPO_ROOT/gateway-boot/target/${JAR_NAME}"
DIST_DIR="$SCRIPT_DIR/dist"
JRE_DIR="$SCRIPT_DIR/jre"
# 干净目录：仅放 fat jar，避免 target/ 中 .original、classes/ 等多余文件被打包进安装包
STAGING_DIR="$SCRIPT_DIR/staging"

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

# 2. jdeps 验证模块清单（可选，用于发现遗漏；jdeps 静态分析无法覆盖反射加载的模块）
log "验证 jlink 模块清单..."
ACTUAL_MODULES="$(jdeps --multi-release 21 --print-module-deps --ignore-missing-deps "$FAT_JAR" 2>/dev/null || true)"
if [[ -n "$ACTUAL_MODULES" ]]; then
  log "jdeps 实际依赖: $ACTUAL_MODULES"
  log "清单文件: $(cat "$MODULES_FILE" | tr -d '\n')"
  log "（若启动失败，对比两者补齐缺失模块；spike 已补充 5 个反射模块）"
fi

# 3. jlink 生成精简 JRE（模块清单已固化在 jlink-modules.txt，19 个模块）
log "生成精简 JRE..."
rm -rf "$JRE_DIR"
jlink \
  --add-modules "$(cat "$MODULES_FILE" | tr -d '\n')" \
  --strip-debug --no-header-files --no-man-pages \
  --output "$JRE_DIR"
log "JRE 体积: $(du -sh "$JRE_DIR" | cut -f1)"

# 4. 准备 dist 与 staging（staging 仅含 fat jar，避免 target/ 多余文件入包）
rm -rf "$DIST_DIR"
mkdir -p "$DIST_DIR"
rm -rf "$STAGING_DIR"
mkdir -p "$STAGING_DIR"
cp "$FAT_JAR" "$STAGING_DIR/"

# 公共 jpackage 参数
# --java-options 写入 app cfg，使产出的安装包默认禁用 redis health（裸机无 Redis 时不误报 DOWN）
JPKG_COMMON=(
  --name llm-gateway
  --app-version "${APP_VERSION//-SNAPSHOT/}"
  --vendor "LLM-Gateway"
  --copyright "Copyright 2026 LLM-Gateway"
  --description "LLM-Gateway - 企业级 AI 模型 API 聚合网关"
  --input "$STAGING_DIR"
  --main-jar "$JAR_NAME"
  --main-class org.springframework.boot.loader.launch.JarLauncher
  --runtime-image "$JRE_DIR"
  --java-options "-Dspring.profiles.active=local -Dmanagement.health.redis.enabled=false"
  --dest "$DIST_DIR"
)

# 5. 打 deb
log "打 deb..."
jpackage --type deb "${JPKG_COMMON[@]}" \
  --resource-dir "$LINUX_RES" \
  --maintainer "LLM-Gateway Team"
log "deb 产物: $(ls "$DIST_DIR"/*.deb)"

# 6. 打 rpm（需 rpm 工具；CI 环境 apt-get install -y rpm 即可）
if command -v rpm >/dev/null 2>&1; then
  log "打 rpm..."
  jpackage --type rpm "${JPKG_COMMON[@]}" \
    --resource-dir "$LINUX_RES" \
    --maintainer "LLM-Gateway Team"
  log "rpm 产物: $(ls "$DIST_DIR"/*.rpm)"
else
  log "警告: 未安装 rpm 工具，跳过 rpm 打包（CI 环境 apt-get install -y rpm 即可）"
fi

# 7. 清理 staging
rm -rf "$STAGING_DIR"

log "完成。产物目录: $DIST_DIR"
ls -lh "$DIST_DIR"
