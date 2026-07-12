## 1. jpackage 打包基础验证

- [x] 1.1 Spike：验证 jpackage + Spring Boot fat jar 启动（`--main-jar gateway-boot-<ver>.jar --main-class org.springframework.boot.loader.launch.JarLauncher`），确认应用正常启动
- [x] 1.2 用 jdeps 分析 fat jar 依赖，确定 jlink 精简 JRE 模块清单
- [x] 1.3 创建 `deployments/package/` 目录结构（`jpackage/`、`linux/`、`windows/`、构建脚本）
- [x] 1.4 编写 `build.sh` / `build.ps1` 构建入口（mvn package -> jlink 生成精简 JRE -> jpackage 打包）

## 2. Linux 安装包（deb + rpm）

- [x] 2.1 编写 systemd unit 模板（`Environment=DB_URL/SERVER_PORT/GATEWAY_ENCRYPTION_KEY`，`Restart=on-failure`）
- [x] 2.2 编写 debconf 模板（端口交互，默认 8080，非交互回退默认）
- [x] 2.3 编写 `postinst`（建 `/var/lib/llm-gateway` 与日志目录、生成 `GATEWAY_ENCRYPTION_KEY`、读 debconf 端口、注册 systemd、`enable --now`）
- [x] 2.4 编写 `prerm`（stop/disable）与 `postrm`（清理安装文件、保留数据目录）
- [x] 2.5 配置 jpackage `--type deb`（`--resource-dir` 挂 postinst/prerm/postrm/debconf/systemd unit）
- [x] 2.6 配置 jpackage `--type rpm`（等价 maintainer 脚本，适配 dnf）
- [x] 2.7 本地验证 deb：干净 Ubuntu 安装 -> 健康检查 UP -> 数据落 `/var/lib/llm-gateway/`
- [x] 2.8 本地验证 rpm：RHEL 系安装 -> 健康检查 UP

## 3. Windows 安装包（exe）

- [x] 3.1 编写 WinSW 配置（`LLMGateway.xml` + `winsw.exe`，注册 Windows Service，`<env>` 写 `DB_URL/SERVER_PORT/GATEWAY_ENCRYPTION_KEY`，`<arguments>` 指向 jpackage 启动器 exe）
- [x] 3.2 编写 Inno Setup 安装向导 UI（端口输入框，默认 8080）
- [x] 3.3 编写安装时密钥生成 Pascal Script（生成 `GATEWAY_ENCRYPTION_KEY`，已存在则保留）
- [x] 3.4 配置服务环境变量写入 WinSW xml（`DB_URL` 指向 `%ProgramData%\LLM-Gateway\data\`、`SERVER_PORT`、`GATEWAY_ENCRYPTION_KEY`）
- [x] 3.5 配置 jpackage `--type app-image`（生成精简 JRE + jar + 启动器 exe）+ Inno Setup 编译 exe（安装 Inno Setup）
- [x] 3.6 本地验证 exe：干净 Windows 安装 -> Service 启动 -> 健康检查 UP -> 数据落 `%ProgramData%`

## 4. CI 集成

- [x] 4.1 在 `release.yml` 加 `package` job，matrix `[ubuntu-latest, windows-latest]`
- [x] 4.2 ubuntu job：构建 deb + rpm（安装 `rpm` 工具）
- [x] 4.3 windows job：构建 exe（jpackage app-image + WinSW + Inno Setup 编译，安装 Inno Setup）
- [x] 4.4 产物上传到 GitHub Release
- [x] 4.5 验证 release tag 触发，deb/rpm/exe 产物齐全

## 5. Docker 资产修复

- [x] 5.1 修复 `Dockerfile`：构建路径改为单模块 `gateway-boot`，修正 COPY 与 jar 名
- [x] 5.2 修复 `docker-compose.yml`：`context` 改根目录、移除源码挂载、补 `gateway-console` 服务
- [x] 5.3 验证 `docker-compose up -d` 正常构建并拉起 gateway

## 6. 文档

- [ ] 6.1 新增 `deployments/package/README.md`（构建步骤、安装命令、配置说明）
- [ ] 6.2 更新 `README.md` 部署章节：修正 DB 类型/jar 名/安装包用法，补充 admin/admin 首次改密提示与 H2 Console 风险提示
