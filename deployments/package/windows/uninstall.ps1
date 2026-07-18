# 卸载 LLM-Gateway Windows 服务（WinSW），保留数据目录
$ErrorActionPreference = 'Stop'
$BinDir = $PSScriptRoot
& "$BinDir\llmgateway.exe" uninstall
Write-Host "LLM-Gateway 服务已卸载。数据目录保留。"