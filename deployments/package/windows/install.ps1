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

# 2. WinSW exe 改名为 llmgateway.exe（与 llmgateway.xml 同名配对，WinSW 要求 exe/xml 同名）
Copy-Item "$BinDir\WinSW.exe" "$BinDir\llmgateway.exe" -Force

# 3. 注册并启动服务
& "$BinDir\llmgateway.exe" install
Start-Service llmgateway
Write-Host "LLM-Gateway 服务已注册并启动。"
Write-Host "  配置文件: $ConfFile（改后 Restart-Service llmgateway 生效）"