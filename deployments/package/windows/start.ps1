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
  -jar "$PSScriptRoot\llmgateway.jar"