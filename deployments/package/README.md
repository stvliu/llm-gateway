# LLM-Gateway 系统安装包

非 Docker 一键部署：Linux deb/rpm + Windows exe，内置 jlink 精简 JRE，默认 `local` profile（H2 文件 + Caffeine，零外部依赖，无 Redis）。

## 构建依赖

- JDK 21（含 `jdeps`/`jlink`/`jpackage`）
- Maven 3.9+（或项目根 `./mvnw`）
- Linux 额外：`dpkg-deb`（默认有）、`rpm`（`sudo apt-get install -y rpm`，交叉打 rpm）
- Windows 额外：[Inno Setup 6](https://jrsoftware.org/isdl.php)（`choco install innosetup`）

## 构建命令

### Linux（deb + rpm）

```bash
./deployments/package/build.sh
# 产物: deployments/package/dist/llmgateway_*.deb, llmgateway-*.rpm
```

### Windows（exe）

```powershell
.\deployments\package\build.ps1
# 产物: deployments\package\dist\llmgateway-setup.exe
```

CI 自动构建见 `.github/workflows/release.yml` 的 `package` job（git tag `v*` 触发）。

## 安装

### Linux deb（Ubuntu/Debian）

```bash
sudo apt install ./llmgateway_*.deb
# 安装时交互询问端口（默认 8080），非交互: DEBIAN_FRONTEND=noninteractive
```

### Linux rpm（RHEL/Rocky/CentOS）

```bash
sudo dnf install ./llmgateway-*.rpm
```

### Windows exe

双击 `llmgateway-setup.exe`，按向导输入端口（默认 8080）。
静默安装：`llmgateway-setup.exe /VERYSILENT`

## 目录布局

### Linux

| 路径 | 用途 |
|------|------|
| `/opt/llmgateway/` | 安装目录（JRE + jar + 启动器） |
| `/var/lib/llmgateway/` | 数据目录（H2 文件，`DB_URL` 指向此） |
| `/var/log/llmgateway/` | 日志目录 |
| `/etc/llmgateway/env` | 环境变量配置（conffile，升级保留） |

### Windows

| 路径 | 用途 |
|------|------|
| `%ProgramFiles%\LLM-Gateway\` | 安装目录（app-image + WinSW） |
| `%ProgramData%\LLM-Gateway\data\` | 数据目录（H2 文件） |
| `%ProgramData%\LLM-Gateway\logs\` | 日志目录 |
| `%ProgramFiles%\LLM-Gateway\LLMGateway.xml` | WinSW 配置（含环境变量，升级保留） |

## 配置说明

环境变量经 `DB_URL`/`SERVER_PORT`/`GATEWAY_ENCRYPTION_KEY` 注入，无需改 `application*.yml`：

- `DB_URL`：H2 文件路径，默认指向数据目录
- `SERVER_PORT`：服务端口，安装时交互设置（默认 8080），**不校验占用**
- `GATEWAY_ENCRYPTION_KEY`：加密密钥，**首次安装自动生成，升级保留**

## 服务管理

### Linux（systemd）

```bash
systemctl status llmgateway
systemctl restart llmgateway
journalctl -u llmgateway -f
```

### Windows（WinSW / sc）

```powershell
Get-Service LLMGateway
Restart-Service LLMGateway
Get-WinSwLog  # 或查看 %ProgramData%\LLM-Gateway\logs
```

## 健康检查

```bash
curl http://localhost:8080/actuator/health
# {"status":"UP",...}
```

## 升级

直接安装新版包覆盖：
- Linux：`sudo apt install ./llmgateway_*.deb`（或 `dnf install`）
- Windows：重新运行 `llmgateway-setup.exe`

升级**保留**：数据目录、`GATEWAY_ENCRYPTION_KEY`、端口配置。Flyway 自动迁移 schema。

## 卸载

- Linux：`sudo apt remove llmgateway`（保留数据）或 `sudo apt purge llmgateway`（清数据）
- Windows：控制面板卸载或 `unins000.exe /VERYSILENT`（保留数据目录）

## 重要提示

- **加密密钥备份**：`GATEWAY_ENCRYPTION_KEY` 丢失则历史加密数据（如 API Key）无法解密。务必备份 `/etc/llmgateway/env`（Linux）或 `LLMGateway.xml`（Windows）。
- **Windows xml 权限**：`LLMGateway.xml` 含 `GATEWAY_ENCRYPTION_KEY`，位于 `C:\Program Files\LLM-Gateway\`，默认 ACL 允许 `Users` 组读取。生产环境建议用 `icacls` 收紧权限，仅管理员与 SYSTEM 可读：
  ```cmd
  icacls "C:\Program Files\LLM-Gateway\LLMGateway.xml" /inheritance:r /grant:r Administrators:F /grant:r SYSTEM:F
  ```
- **默认凭据**：`local` profile 自动创建 `admin/admin`，首次登录后请立即改密。
- **H2 Console**：`local` profile 开启 H2 Console（`/h2-console`，`web-allow-others=true`），生产环境请关闭或限制访问。
- **端口冲突**：安装时不校验端口占用，冲突时服务反复重启暴露（systemd `Restart=on-failure` / WinSW `onfailure restart`）。
