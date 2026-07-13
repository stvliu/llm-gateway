# Brainstorm Summary

- Change: one-click-bare-deploy
- Date: 2026-07-11

## 确认的技术方案

### 打包流程
`mvn package` -> fat jar (`gateway-boot-1.0.0-SNAPSHOT.jar`, Main-Class=`org.springframework.boot.loader.launch.JarLauncher`) -> `jdeps` 分析 -> `jlink` 精简 JRE -> `jpackage` 打包

### Linux (deb + rpm)
- `jpackage --type deb` / `--type rpm`，ubuntu-latest 装 `rpm` 工具交叉打 rpm
- `--main-jar gateway-boot-1.0.0-SNAPSHOT.jar --main-class org.springframework.boot.loader.launch.JarLauncher`（显式）
- systemd unit: `Environment=DB_URL/SERVER_PORT/GATEWAY_ENCRYPTION_KEY`, `Restart=on-failure`, `WorkingDirectory`, `User`
- debconf 端口交互: 默认 8080，非交互回退默认，**不校验占用**（运行时冲突由 systemd 暴露）
- `postinst`: 建 `/var/lib/llm-gateway` 与 `/var/log/llm-gateway`、生成 `GATEWAY_ENCRYPTION_KEY`（已存在则保留）、读 debconf 端口、注册 systemd、`enable --now`
- `prerm`: stop/disable；`postrm`: 清理安装文件、保留数据目录
- env 文件 `/etc/llm-gateway/env`（DB_URL/SERVER_PORT/GATEWAY_ENCRYPTION_KEY），标记 conffile 升级保留
- 数据目录 `/var/lib/llm-gateway/`，`DB_URL=jdbc:h2:file:/var/lib/llm-gateway/gateway;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_ON_EXIT=FALSE`

### Windows (exe installer)
- `jpackage --type app-image` 生成应用目录（精简 JRE + jar + 启动器 exe）
- WinSW (`winsw.exe` + `LLMGateway.xml`) 把启动器 exe 注册成 Windows Service
  - xml 配 `<env name="DB_URL/SERVER_PORT/GATEWAY_ENCRYPTION_KEY">` + `<arguments>` 指向启动器 exe
- Inno Setup 打 exe installer: 安装时复制 app-image + WinSW 到 Program Files、生成 `GATEWAY_ENCRYPTION_KEY`（已存在保留）、运行 WinSW install、建数据目录 `%ProgramData%\LLM-Gateway\data\`
- 端口: Inno Setup 向导输入框（默认 8080），写入 WinSW xml 的 `SERVER_PORT`
- 放弃 msi/WiX: jpackage `--type msi` 不支持 ServiceInstall，WiX 手写 msi 工作量大；Inno Setup + WinSW 简单可控

### CI
- `release.yml` 加 `package` job，matrix `[ubuntu-latest, windows-latest]`
- ubuntu job: 装 `rpm` 工具 -> jpackage `--type deb` + `--type rpm` -> 上传 deb/rpm
- windows job: 装 Inno Setup -> jpackage app-image -> 配 WinSW -> Inno Setup 编译 exe -> 上传 exe
- 产物挂 GitHub Release（已有 release job 创建 Release）

## 关键取舍与风险

- **Windows 改 exe（放弃 msi）**: jpackage msi 不支持 ServiceInstall。代价: 产出 exe 非 msi，需 Spec Patch
- **jpackage + fat jar**: 显式 `--main-class JarLauncher`，build 阶段 spike 验证（最大技术风险，备选: layered jar）
- **debconf 端口不校验占用**: 简单，运行时冲突由 systemd Restart 失败暴露
- **H2 文件锁**: `DB_CLOSE_ON_EXIT=FALSE` 已配 + systemd `Restart=on-failure` 兜底
- **GATEWAY_ENCRYPTION_KEY 丢失**: 历史加密数据无法解密，env 文件 conffile 保留 + 文档提示备份
- **Flyway 降级不兼容**: 降级风险，文档提示升级前备份数据目录
- **admin/admin 默认凭据 + H2 Console 远程访问**: 接受现状，文档强提示

## 测试策略

### CI smoke test（在 package job 内）
- ubuntu: 打 deb 后 `docker run ubuntu apt install ./deb`，curl `/actuator/health` 验 UP；打 rpm 后 `docker run rockylinux dnf install`，验证 UP
- windows: 打 exe 后 windows runner 静默安装 (`/VERYSILENT`)，验证 service 启动 + health UP

### 本地手工验证
- 干净 Ubuntu/RHEL VM: 安装/升级/卸载全流程
- 干净 Windows VM: 安装/升级/卸载全流程

### 升级测试
- 装旧版 -> 装新版: 数据目录保留、`GATEWAY_ENCRYPTION_KEY` 不变、加密数据可解密（创建 API Key -> 升级 -> 验证可解密）

## Spec Patch

`specs/bare-metal-deploy/spec.md` 需回写:
- Requirement "Windows msi 安装包一键部署" -> "Windows exe 安装包一键部署": 场景中 msi 改 exe，服务注册改为 WinSW
- Requirement "CI 多平台打包" 场景: `.msi` 改 `.exe`
- 补充边界场景: 升级保留密钥、端口冲突运行时暴露

## 待确认

- 测试策略是否充分（CI smoke test 在 package job 内 vs 单独 job）
- 是否还需补充其他边界场景
