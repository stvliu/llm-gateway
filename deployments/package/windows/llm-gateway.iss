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
AppId={{8F3B2A1C-4D5E-4A6B-9C7D-1A2B3C4D5E6F}
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

// 初始化端口输入页
procedure InitializeWizard;
var
  PrevPort: string;
begin
  PortPage := CreateInputQueryPage(wpSelectDir,
    '服务端口配置', '请输入 LLM-Gateway 服务监听端口',
    '默认端口 8080。安装时不校验端口占用；若端口冲突，服务将自动反复重启暴露问题。');
  PortPage.Add('服务端口:', False);
  PortPage.Values[0] := '8080';

  // 升级时从已有 xml 读取端口（默认安装路径探测）
  PrevPort := ReadXmlValue(ExpandConstant('{pf}\{#AppName}\{#WinSwXmlName}'), 'SERVER_PORT');
  if PrevPort <> '' then
    PortPage.Values[0] := PrevPort;
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

// 生成 32 字节 base64 加密密钥（PowerShell 加密安全 RNG，等价 openssl rand -base64 32）
function GenerateEncryptionKey: string;
var
  ResultCode: Integer;
  TempFile: string;
begin
  Result := '';
  TempFile := ExpandConstant('{tmp}\gateway_key.txt');
  // 用 PowerShell 加密安全 RNG 生成 32 字节 base64 密钥
  if Exec(ExpandConstant('{cmd}'), '/c powershell -NoProfile -Command "[Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(32)) > "' + TempFile + '"',
         '', SW_HIDE, ewWaitUntilTerminated, ResultCode) then
  begin
    LoadStringFromFile(TempFile, Result);
    Result := Trim(Result);
    // 立即清理临时文件（含密钥，避免残留）
    DeleteFile(TempFile);
  end;
  // PowerShell 失败：中止安装，不用固定占位串（避免全新安装使用公开密钥）
  if Result = '' then
  begin
    RaiseException('加密密钥生成失败，请检查 PowerShell 环境后重试');
  end;
end;

// 从 WinSW xml 读取环境变量值（匹配 name="KeyName" value="..." 格式）
function ReadXmlValue(const FileName, KeyName: string): string;
var
  Content: string;
  StartPos, EndPos: Integer;
  SearchStr: string;
begin
  Result := '';
  if FileExists(FileName) and LoadStringFromFile(FileName, Content) then
  begin
    // 匹配 WinSW xml 格式: name="KeyName" value="..."
    SearchStr := 'name="' + KeyName + '" value="';
    StartPos := Pos(SearchStr, Content);
    if StartPos > 0 then
    begin
      StartPos := StartPos + Length(SearchStr);
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
  PortValue, KeyValue, OldPort: string;
begin
  if CurStep = ssPostInstall then
  begin
    XmlPath := ExpandConstant('{app}\{#WinSwXmlName}');

    // 端口：用户输入（新装默认 8080，升级时为已有端口）
    PortValue := PortPage.Values[0];

    // 密钥：升级时读已有，新装则生成
    KeyValue := ReadXmlValue(XmlPath, 'GATEWAY_ENCRYPTION_KEY');
    if KeyValue = '' then
    begin
      KeyValue := GenerateEncryptionKey;
      Log('生成新的 GATEWAY_ENCRYPTION_KEY（请备份！）');
    end
    else
      Log('保留已有 GATEWAY_ENCRYPTION_KEY');

    // 重写 xml（确保端口与密钥就位）
    if LoadStringFromFile(XmlPath, Content) then
    begin
      // 端口替换：升级时按已有端口精确匹配，新装按默认 8080
      OldPort := ReadXmlValue(XmlPath, 'SERVER_PORT');
      if OldPort <> '' then
        StringChangeEx(Content, 'name="SERVER_PORT" value="' + OldPort + '"',
                       'name="SERVER_PORT" value="' + PortValue + '"', True)
      else
        StringChangeEx(Content, 'name="SERVER_PORT" value="8080"',
                       'name="SERVER_PORT" value="' + PortValue + '"', True);
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
