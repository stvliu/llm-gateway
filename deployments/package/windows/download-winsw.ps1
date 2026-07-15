# =============================================================================
# 下载 WinSW x64 exe 并命名为 LLMGateway.exe
# WinSW 用于将 LLM-Gateway Java 应用包装为 Windows 服务
#
# 用法:
#   .\download-winsw.ps1                              # 默认 v2.12.0，输出到脚本所在目录
#   .\download-winsw.ps1 -Version 2.12.0 -OutDir D:\out
#
# 依赖: 联网（Invoke-WebRequest 从 GitHub releases 下载）
# =============================================================================
[CmdletBinding()]
param(
  [string]$Version = "2.12.0",
  [string]$OutDir = $PSScriptRoot
)
$ErrorActionPreference = 'Stop'

# 确保输出目录存在
if (-not (Test-Path $OutDir)) { New-Item -ItemType Directory -Force -Path $OutDir | Out-Null }

$outFile = Join-Path $OutDir "WinSW.exe"
$url = "https://github.com/winsw/winsw/releases/download/v$Version/WinSW-x64.exe"
Write-Host "下载 WinSW v$Version -> $outFile"
Invoke-WebRequest -Uri $url -OutFile $outFile -UseBasicParsing
if (-not (Test-Path $outFile)) { throw "WinSW 下载失败" }
Write-Host "完成: $outFile"
