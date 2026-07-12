# =============================================================================
# LLM-Gateway Windows app-image 构建脚本
# 产出: jpackage app-image（供 Inno Setup 编译 exe，见 Task 3.5）
# 用法: .\deployments\package\build.ps1 [-SkipMvn]
#
# 流程: mvn package -> jlink 精简 JRE -> jpackage 打 app-image
# 依赖: JDK 21（含 jlink/jpackage）、Maven（mvnw.cmd）
#
# 注意: Windows jpackage app-image 的原生启动器（llm-gateway.exe）位于
#       app-image 根目录，而非 bin/ 子目录（与 Linux 不同）。
#       Inno Setup 打包时以此路径为准。
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
# 干净目录：仅放 fat jar，避免 target/ 中 .original、classes/ 等多余文件被打包进 app-image
$StagingDir = Join-Path $ScriptDir 'staging'

function Log($msg) { Write-Host "[build] $msg" -ForegroundColor Green }
function Die($msg) { Write-Host "[error] $msg" -ForegroundColor Red; exit 1 }

# 1. 构建 fat jar
if (-not $SkipMvn) {
  Log "构建 fat jar..."
  Push-Location $RepoRoot
  try { .\mvnw.cmd clean package -pl gateway-boot -am -DskipTests }
  finally { Pop-Location }
  if ($LASTEXITCODE -ne 0) { Die "Maven 构建失败 (exit $LASTEXITCODE)" }
} else {
  Log "跳过 Maven 构建（-SkipMvn）"
}

# 读取版本号（从 Maven 读取，如 1.0.0-SNAPSHOT）
Push-Location $RepoRoot
try {
  $AppVersion = (& .\mvnw.cmd help:evaluate "-Dexpression=project.version" -q -DforceStdout 2>$null)
} finally { Pop-Location }
$AppVersion = $AppVersion.Trim()
$JarName = "gateway-boot-$AppVersion.jar"
$FatJar = Join-Path $RepoRoot "gateway-boot\target\$JarName"
if (-not (Test-Path $FatJar)) { Die "fat jar 不存在: $FatJar" }
Log "fat jar: $FatJar"

# 2. jlink 生成精简 JRE（模块清单已固化在 jlink-modules.txt，19 个模块）
Log "生成精简 JRE..."
if (Test-Path $JreDir) { Remove-Item -Recurse -Force $JreDir }
$modules = (Get-Content $ModulesFile -Raw).Trim()
jlink --add-modules $modules --strip-debug --no-header-files --no-man-pages --output $JreDir
if ($LASTEXITCODE -ne 0) { Die "jlink 失败 (exit $LASTEXITCODE)" }
$JreSizeMB = [math]::Round((Get-ChildItem $JreDir -Recurse | Measure-Object -Property Length -Sum).Sum / 1MB, 1)
Log "JRE 体积: ${JreSizeMB}MB"

# 3. 准备 dist 与 staging（staging 仅含 fat jar，避免 target/ 多余文件入包）
if (Test-Path $DistDir) { Remove-Item -Recurse -Force $DistDir }
New-Item -ItemType Directory -Force -Path $DistDir | Out-Null
if (Test-Path $StagingDir) { Remove-Item -Recurse -Force $StagingDir }
New-Item -ItemType Directory -Force -Path $StagingDir | Out-Null
# 仅复制 fat jar 到 staging
Copy-Item $FatJar $StagingDir

# 4. 打 app-image
# --java-options 写入 app cfg，使产出的 app-image 默认禁用 redis health（裸机无 Redis 时不误报 DOWN）
Log "打 app-image..."
$ver = $AppVersion -replace '-SNAPSHOT',''
jpackage --type app-image `
  --name llm-gateway `
  --app-version $ver `
  --vendor "LLM-Gateway" `
  --copyright "Copyright 2026 LLM-Gateway" `
  --description "LLM-Gateway - 企业级 AI 模型 API 聚合网关" `
  --input $StagingDir `
  --main-jar $JarName `
  --main-class org.springframework.boot.loader.launch.JarLauncher `
  --runtime-image $JreDir `
  --java-options "-Dspring.profiles.active=local -Dmanagement.health.redis.enabled=false" `
  --dest $DistDir
if ($LASTEXITCODE -ne 0) { Die "jpackage 失败 (exit $LASTEXITCODE)" }

# 5. 清理 staging
Remove-Item -Recurse -Force $StagingDir

$AppImage = Join-Path $DistDir 'llm-gateway'
if (-not (Test-Path $AppImage)) { Die "app-image 未生成: $AppImage" }
# Windows: 启动器在 app-image 根目录（llm-gateway.exe），非 bin/
$Launcher = Join-Path $AppImage 'llm-gateway.exe'
if (Test-Path $Launcher) { Log "启动器: $Launcher" } else { Log "警告: 未找到根目录启动器 llm-gateway.exe" }
Log "完成。下一步用 Inno Setup 编译 exe（见 Task 3.5）"
Get-ChildItem $AppImage | Format-Table Name
