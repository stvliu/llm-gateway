# LLM-Gateway 系统安装包

非 Docker 部署：Linux deb/rpm + Windows zip。**不内置 JRE，目标机器需预装 Java 17/21/25**。默认 `local` profile（H2 文件 + Caffeine，零外部依赖，无 Redis）。

打包照抄 thingsboard 设计：Gradle ospackage（`com.netflix.nebula:gradle-ospackage-plugin:12.3.0`）+ Maven filters 占位符替换。deb 基于 JDeb、rpm 基于 redline-rpm，**纯 Java 实现，本地无需 dpkg-deb / rpmbuild**，rpm 完整支持 maintainer 脚本（密钥生成 + systemd 注册）。

## 构建依赖

- JDK 21（构建用）
- Maven 3.9+（或 `./mvnw`）
- Git Bash（Windows）或 sh（Linux）
- Gradle 9.5.1（wrapper 自动下载，nebula v12.x 要求 Gradle 9.x）

## 目标机器依赖

- **Java 17/21/25**（JRE 或 JDK，系统 PATH 可执行 `java`）--包不内置 JRE
- systemd（Linux 服务管理）

## 构建命令

```bash
./deployments/package/build.sh
# 产物: deployments/package/dist/
#   llmgateway.deb            Linux deb（Ubuntu/Debian）
#   llmgateway.rpm            Linux rpm（RHEL/Rocky/CentOS）
#   llmgateway-win-*.zip      Windows zip
```

跳过 Maven package（复用已有 fat jar）：`./deployments/package/build.sh --skip-mvn`

CI 自动构建见 `.github/workflows/release.yml` 的 `package` job（git tag `v*` 触发，含 deb/rpm/zip systemd smoke test）。

## 安装

### Linux deb（Ubuntu/Debian）

```bash
sudo apt install ./llmgateway_*.deb   # 自动拉取 openjdk-21-jre-headless 依赖
```

### Linux rpm（RHEL/Rocky/CentOS）

```bash
sudo dnf install ./llmgateway-*.rpm   # 自动拉取 java-21-headless 依赖
```

### Windows zip

解压 `llmgateway-win-*.zip`，以管理员身份运行 `bin\install.ps1`（需预装 Java，PATH 可执行 `java`）。

## 目录布局

### Linux

| 路径 | 用途 |
|------|------|
| `/opt/llmgateway/` | 安装目录 |
| `/opt/llmgateway/bin/llmgateway.jar` | 应用 fat jar |
| `/opt/llmgateway/bin/llmgateway.sh` | 启动脚本（`exec java`，系统 Java） |
| `/opt/llmgateway/conf/llmgateway.conf` | 配置文件（conffile，升级保留） |
| `/etc/llmgateway/conf` | 软链接 -> `/opt/llmgateway/conf`（ospackage `link`） |
| `/var/lib/llmgateway/` | 数据目录（H2 文件，`DB_URL` 指向此） |
| `/var/log/llmgateway/` | 日志目录 |
| `/usr/lib/systemd/system/llmgateway.service` | systemd unit（rpm） |
| `/lib/systemd/system/llmgateway.service` | systemd unit（deb） |

### Windows

| 路径 | 用途 |
|------|------|
| `<解压目录>\bin\llmgateway.jar` | 应用 fat jar |
| `<解压目录>\bin\WinSW.exe` | 服务包装器 |
| `<解压目录>\bin\install.ps1` / `uninstall.ps1` | 安装/卸载脚本 |
| `<解压目录>\conf\llmgateway.conf` | 配置文件 |

## 配置说明

环境变量经 `llmgateway.conf` 注入（`llmgateway.sh` source 该文件），无需改 `application*.yml`：

- `DB_URL`：H2 文件路径，默认 `/var/lib/llmgateway/gateway`
- `SERVER_PORT`：服务端口，默认 8080
- `GATEWAY_ENCRYPTION_KEY`：加密密钥，**首次安装由 postinst 自动生成，升级保留**（conf 含 `__GENERATE_KEY__` 占位符，安装时替换为随机密钥）
- `JAVA_OPTS`：JVM 参数，默认 `-Xmx512m -Dmanagement.health.redis.enabled=false`

改配置后 `systemctl restart llmgateway`（Linux）或 `Restart-Service llmgateway`（Windows）生效，无需重打包。

## 服务管理

### Linux（systemd）

```bash
systemctl status llmgateway
systemctl restart llmgateway
journalctl -u llmgateway -f
```

### Windows（WinSW / sc）

```powershell
Get-Service llmgateway
Restart-Service llmgateway
```

## 健康检查

```bash
curl http://localhost:8080/actuator/health
# {"status":"UP",...}
```

## 升级

直接安装新版包覆盖（conffile / `%config(noreplace)` 保留配置与密钥，Flyway 自动迁移 schema）：
- Linux：`sudo apt install ./llmgateway_*.deb`（或 `dnf install ./llmgateway-*.rpm`）
- Windows：重新解压新版 zip，运行 `bin\install.ps1`

## 卸载

- Linux：`sudo apt remove llmgateway`（保留数据）或 `sudo apt purge llmgateway`（清数据）；rpm 对应 `dnf remove llmgateway`
- Windows：运行 `bin\uninstall.ps1`（停止并注销服务，保留数据目录）

## 重要提示

- **需预装 Java**：包不内置 JRE，目标机器须装 Java 17/21/25（PATH 可执行 `java`）
- **加密密钥备份**：`GATEWAY_ENCRYPTION_KEY` 丢失则历史加密数据（如 API Key）无法解密。备份 `/opt/llmgateway/conf/llmgateway.conf`
- **默认凭据**：`local` profile 自动创建 `admin/admin`，首次登录后改密
- **H2 Console**：`local` profile 开启 `/h2-console`，生产请关闭或限制访问
- **端口冲突**：安装时不校验端口占用，冲突时服务反复重启（systemd `Restart=on-failure`）

## 打包实现（照抄 thingsboard）

- `build.gradle`：ospackage DSL（`ospackage{}` 通用 + `buildRpm` + `buildDeb` + `zipWin`），requires java-17/21/25、arch NOARCH/all、`link` /etc 软链接、`configurationFile`、`filter(ReplaceTokens)` 平台差异
- `scripts/control/{deb,rpm}/`：maintainer 脚本（preinst/postinst/prerm/postrm），用 `${pkg.name}` `${pkg.user}` `${pkg.installFolder}` `${pkg.logFolder}` 占位符
- `scripts/control/template.service`：systemd unit 占位符模板
- `filters/unix.properties`：占位符替换值（`pkg.logFolder=${pkg.unixLogFolder}`）
- `gateway-boot/pom.xml` pkg profile：maven-resources-plugin 复制 scripts/bin/conf 到 `target/packaging`，filtering 替换占位符
- `build.sh`：`mvn package + process-resources -Ppkg` -> `./gradlew buildDeb buildRpm zipWin`
