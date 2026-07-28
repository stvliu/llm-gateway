#!/usr/bin/env bash
# =============================================================================
# LLM-Gateway 跨平台安装包构建脚本（Gradle ospackage 方案）
# 产出: deb（JDeb 纯 Java）+ rpm（redline-rpm 纯 Java，支持完整 maintainer 脚本）+ zip（Windows）
# 不内置 JRE：requires java-17/21/25，arch NOARCH/all，目标机器需预装 Java
# 用法: ./deployments/package/build.sh [--skip-mvn] [--skip-deb] [--skip-rpm] [--skip-zip]
#   --skip-mvn  跳过 mvn package（复用已有 fat jar，仅重新替换占位符）
#   --skip-deb  跳过 deb 构建（-PskipDeb=true -> buildDeb.onlyIf 跳过）
#   --skip-rpm  跳过 rpm 构建（-PskipRpm=true -> buildRpm.onlyIf 跳过）
#   --skip-zip  跳过 zip 构建（-PskipZip=true -> zipWin.onlyIf 跳过）
# docker image 不在本脚本范围（独立 CI job build-docker，用 deployments/docker/Dockerfile）
#
# 流程: mvn package + process-resources（替换占位符）-> 预复制 jar -> gradlew buildDeb buildRpm zipWin
# 依赖: JDK 21、Maven（mvnw）、Gradle 9.5.1（wrapper 自动下载，nebula v12.x 要求 Gradle 9.x）
# 纯 Java 打包: 本地无需 dpkg-deb / rpmbuild，Windows 即可验证 deb + rpm + zip 三产物
# =============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

# 参数解析（pkg.skip.* 风格跳过能力）
SKIP_MVN=0; SKIP_DEB=0; SKIP_RPM=0; SKIP_ZIP=0
for arg in "$@"; do
  case "$arg" in
    --skip-mvn) SKIP_MVN=1 ;;
    --skip-deb) SKIP_DEB=1 ;;
    --skip-rpm) SKIP_RPM=1 ;;
    --skip-zip) SKIP_ZIP=1 ;;
    *) ;;
  esac
done
# 0/1 -> true/false（传给 Gradle -Pskip*，build.gradle onlyIf 判断）
to_bool() { [[ "$1" = "1" ]] && echo true || echo false; }

# 版本号（从 Maven 读取，如 1.0.0-SNAPSHOT）
APP_VERSION="$(cd "$REPO_ROOT" && ./mvnw help:evaluate -Dexpression=project.version -q -DforceStdout 2>/dev/null)"
JAR_NAME="gateway-boot-${APP_VERSION}.jar"
FAT_JAR="$REPO_ROOT/gateway-boot/target/${JAR_NAME}"
DIST_DIR="$SCRIPT_DIR/dist"
# mvn process-resources -Ppkg 输出（占位符已替换的 scripts/bin/conf）
PKG_STAGING="$REPO_ROOT/gateway-boot/target/packaging"

# 颜色输出：step 醒目步骤标题（编号），log 步骤内子信息，err 错误退出
STEP=0
TOTAL=5
step() { STEP=$((STEP+1)); echo -e "\n\033[1;36m=== [$STEP/$TOTAL] $* ===\033[0m"; }
log()  { echo -e "\033[32m[build]\033[0m $*"; }
ok()   { echo -e "\033[32m✓\033[0m $*"; }
err()  { echo -e "\033[31m[error]\033[0m $*" >&2; exit 1; }

# 1. 构建 fat jar + process-resources 替换占位符
if [[ "$SKIP_MVN" = "0" ]]; then
  step "构建 fat jar + 替换打包占位符（mvn package + process-resources -Ppkg）"
  (cd "$REPO_ROOT" && ./mvnw clean package -pl gateway-boot -am -DskipTests)
  (cd "$REPO_ROOT" && ./mvnw process-resources -pl gateway-boot -Ppkg)
else
  step "跳过 Maven package（--skip-mvn），仅重新替换占位符"
  (cd "$REPO_ROOT" && ./mvnw process-resources -pl gateway-boot -Ppkg)
fi
[[ -f "$FAT_JAR" ]] || err "fat jar 不存在: $FAT_JAR"
ok "fat jar: $FAT_JAR"
[[ -d "$PKG_STAGING" ]] || err "打包占位符输出不存在: $PKG_STAGING（process-resources 失败）"
ok "占位符已替换: $PKG_STAGING"

# 2. 预复制 fat jar 为固定名 llmgateway.jar（build.gradle 的 mainJar 引用此固定名）
step "预复制 jar"
STAGED_JAR="$REPO_ROOT/gateway-boot/target/llmgateway.jar"
cp "$FAT_JAR" "$STAGED_JAR"
ok "预复制 jar: $STAGED_JAR"

# 3. 确保 WinSW.exe 就绪（仅 zip 构建时需要）
if [[ "$SKIP_ZIP" = "0" ]]; then
  step "确保 WinSW.exe 就绪"
  WINSW_EXE="$SCRIPT_DIR/windows/WinSW.exe"
  WINSW_VERSION="2.12.0"
  if [[ ! -f "$WINSW_EXE" ]]; then
    log "下载 WinSW v${WINSW_VERSION}（zipWin 依赖）..."
    curl -fsSL "https://github.com/winsw/winsw/releases/download/v${WINSW_VERSION}/WinSW-x64.exe" -o "$WINSW_EXE" \
      || err "下载 WinSW 失败（检查网络或手动放置 $WINSW_EXE）"
    [[ -f "$WINSW_EXE" ]] || err "WinSW.exe 下载后仍不存在: $WINSW_EXE"
  else
    log "WinSW.exe 已存在，跳过下载"
  fi
else
  step "跳过 WinSW 就绪（--skip-zip）"
fi

# 4. Gradle ospackage 打包（-Pskip* 控制 onlyIf 跳过对应 task）
#    gradlew 总是列 buildDeb buildRpm zipWin，onlyIf false 的 task 自动跳过（SKIPPED）
step "Gradle ospackage 打包（deb/rpm/zip，按 --skip-* 跳过）"
SKIP_DEB_B=$(to_bool "$SKIP_DEB"); SKIP_RPM_B=$(to_bool "$SKIP_RPM"); SKIP_ZIP_B=$(to_bool "$SKIP_ZIP")
log "skipDeb=$SKIP_DEB_B skipRpm=$SKIP_RPM_B skipZip=$SKIP_ZIP_B"
# MSYS2_ARG_CONV_EXCL='*' 禁用 Git Bash 路径转换（-PpkgInstallFolder=/opt/... 的 / 开头值会被 MSYS2 转为 D:\Program Files\Git\opt\...）；Linux 下无害
(cd "$SCRIPT_DIR" && MSYS2_ARG_CONV_EXCL='*' ./gradlew buildDeb buildRpm zipWin --no-daemon \
  -PprojectBuildDir="../../gateway-boot/target/packaging" \
  -PprojectVersion="${APP_VERSION}" \
  -PmainJar="../../gateway-boot/target/llmgateway.jar" \
  -PpkgName=llmgateway \
  -PpkgUser=llmgateway \
  -PpkgInstallFolder=/opt/llmgateway \
  -PpkgLogFolder=/var/log/llmgateway \
  -PskipDeb="${SKIP_DEB_B}" \
  -PskipRpm="${SKIP_RPM_B}" \
  -PskipZip="${SKIP_ZIP_B}")

# 5. 复制产物到 dist + 汇总（跳过的 task 无产物，find 只复制存在的 deb/rpm/zip）
step "复制产物到 dist + 汇总"
find "$SCRIPT_DIR/build" \( -name "*.deb" -o -name "*.rpm" -o -name "*.zip" \) -exec cp {} "$DIST_DIR" \;

echo ""
ok "构建完成。产物目录: $DIST_DIR"
ls -lh "$DIST_DIR"
