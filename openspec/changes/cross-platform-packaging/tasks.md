## 1. conf 与启动脚本

- [x] 1.1 新增 conf 模板 `deployments/package/conf/llmgateway.conf`（SERVER_PORT/DB_URL/GATEWAY_ENCRYPTION_KEY 占位符/JAVA_OPTS/路径）
- [x] 1.2 新增 Linux 启动脚本 `deployments/package/bin/llm-gateway.sh`（source conf + exec java $JAVA_OPTS -jar）
- [x] 1.3 改 `deployments/package/linux/llm-gateway.service`（ExecStart 指向 llm-gateway.sh，去掉 EnvironmentFile）

## 2. JReleaser 打包配置

- [x] 2.1 `pom.xml` 加 `org.jreleaser:jreleaser-maven-plugin` 1.25.0 插件段（绑定 package 阶段，只 assemble 不 release）
- [x] 2.2 新增 `deployments/package/jreleaser.yml`：SINGLE_JAR distribution + fileSets（fat jar/JRE/conf/启动脚本/systemd unit）+ deb/rpm packager + archive assembler
- [ ] 2.3 配 deb/rpm packager（requires/conffile/scripts 共用 postinst/prerm/postrm）+ archive 出 Windows zip（fileSets: Windows JRE + WinSW + ps1 + conf + jar）

## 3. maintainer 脚本迁移

- [ ] 3.1 改 postinst：生成 conf（密钥 `head -c 32 /dev/urandom | base64` + sed `|` 分隔符）+ `chmod 640` conf + `chmod -R 0755 runtime/bin` 兜底 JRE 权限
- [ ] 3.2 改 prerm/postrm：适配 conf 与 systemd unit 新布局（保留数据目录 `/var/lib/llm-gateway`）
- [ ] 3.3 删除 `postinst-rpm`/`prerm-rpm`/`postrm-rpm`，JReleaser deb/rpm 共用 maintainer 脚本

## 4. Windows zip 脚本

- [ ] 4.1 新增 `deployments/package/windows/start.ps1`（解析 conf 注入环境变量 + java -jar）+ `install.ps1`（WinSW install + Start-Service）+ `uninstall.ps1`（WinSW uninstall）
- [ ] 4.2 新增 `deployments/package/windows/llm-gateway.xml`（WinSW service 配置）+ 准备 WinSW.exe（v2.x，作为 archive fileSet）

## 5. 构建脚本改造

- [ ] 5.1 改 `deployments/package/build.sh`：mvn package + jlink（Linux JRE 交叉生成/下载，见 design §4.3）+ `mvn jreleaser:assemble` 出 deb/rpm/zip；删 jpackage deb/rpm 段 + `-rpm` 临时 resource-dir hack
- [ ] 5.2 删除 `deployments/package/build.ps1`（Windows 改 zip，不再用 jpackage+iscc）

## 6. CI 矩阵简化

- [ ] 6.1 改 `.github/workflows/release.yml`：package job 从 matrix `[ubuntu-latest, windows-latest]` 简化为单 windows-latest、单 JDK 21（删 Java 17 setup + JAVA17_HOME + `apt-get install rpm` + `choco install innosetup`）
- [ ] 6.2 smoke test 调整：deb 用 systemd-ubuntu 容器、rpm 用 systemd-rockylinux 容器、zip 用 windows runner（Expand-Archive + install.ps1 + Get-Service + uninstall.ps1）

## 7. 清理冗余

- [ ] 7.1 删除 `deployments/package/linux/llm-gateway.templates` + `llm-gateway.config`（debconf）
- [ ] 7.2 删除 Inno Setup `.iss` 脚本（Windows 改 zip）

## 8. 验证

- [ ] 8.1 Windows 开发机跑 build.sh，验证一次产出 deb + rpm（JReleaser assemble）+ zip（archive）
- [ ] 8.2 deb 在 systemd-ubuntu 容器装，`/actuator/health` 200，`runtime/bin/java` 0755（postinst chmod），改 conf SERVER_PORT 重启生效
- [ ] 8.3 rpm 在 systemd-rockylinux 容器装，`/actuator/health` 200，改 conf 重启生效
- [ ] 8.4 zip 在 Windows 解压 + install.ps1，Get-Service llm-gateway Running，`/actuator/health` 200
- [ ] 8.5 升级验证：deb/rpm 升级后 conf 保留（端口/密钥不变），`/var/lib/llm-gateway` 数据不丢
- [ ] 8.6 卸载验证：apt/dnf remove 或 zip uninstall.ps1 后 `/var/lib/llm-gateway` 数据目录保留
- [ ] 8.7 jlink 平台验证（design §4.3）：确认交叉生成 Linux JRE 或下载 Linux JRE 方案可行
