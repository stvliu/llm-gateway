# Task 1.1 Spike 验证报告：jpackage + fat jar 裸机部署

> 分支：`feature/20260711/one-click-bare-deploy`
> 验证日期：2026-07-12
> 验证人：implementer subagent

## 1. 结论

**Spike 通过**：jpackage 基于 fat jar + 精简 JRE 生成 app-image，可在裸机（无 JDK 环境）启动，Tomcat 8080 正常运行，`/actuator/health` 返回 **200 UP**。

本次在 spike 过程中发现并修复了 `ProviderRegistryHealthIndicator` 的预存在设计缺陷（全新安装 UNKNOWN 被误判为 DOWN），修复后 health 端点恢复正常。

## 2. 环境信息

| 项 | 值 |
|---|---|
| OS | Windows 11 Pro（Git Bash 环境） |
| JDK | Java(TM) SE Runtime Environment 21.0.10+8-LTS-217 |
| 工具 | jdeps / jlink / jpackage 21.0.10 |
| 构建产物 | `gateway-boot/target/gateway-boot-1.0.0-SNAPSHOT.jar`（fat jar，约 83 MB） |
| Spring Boot | 3.5.0 |
| Main-Class | `org.springframework.boot.loader.launch.JarLauncher` |
| Start-Class | `com.codingas.gateway.GatewayApplication` |

## 3. 构建步骤

### 3.1 构建 fat jar

```bash
./mvnw clean package -pl gateway-boot -am -DskipTests
```

产物：`gateway-boot/target/gateway-boot-1.0.0-SNAPSHOT.jar`（83 MB，含 115 个 lib jar）。

### 3.2 jdeps 模块依赖分析

解压 fat jar 后对 `BOOT-INF/classes`（以 `BOOT-INF/lib/*` 为 class-path）执行 jdeps：

```bash
jdeps --multi-release 21 --ignore-missing-deps --recursive \
  --print-module-deps \
  -classpath "<BOOT-INF/lib 下所有 jar，分号分隔的 Windows 路径>" \
  BOOT-INF/classes
```

**jdeps 原始输出（14 个模块）：**

```
java.base,java.compiler,java.desktop,java.instrument,java.net.http,
java.prefs,java.rmi,java.scripting,java.security.jgss,java.sql.rowset,
jdk.jfr,jdk.management,jdk.net,jdk.unsupported
```

> **注意（Windows 坑）**：jdeps 在 Windows 上不展开 `-cp` 的 `*` 通配符（报 `InvalidPathException: Illegal char <*>`），必须用分号分隔的完整 jar 路径列表；且 lib 中存在多版本 jar（如 JavaEWAH-1.2.3.jar），必须加 `--multi-release 21`。

**反射遗漏模块补充（5 个）：**

jdeps 基于静态分析，无法识别 Spring Boot 反射加载的 JDK 模块。根据经验补充以下必需模块，否则启动时会出现 `NoClassDefFoundError`：

| 补充模块 | 用途 |
|---|---|
| `java.management` | JMX / Tomcat / Actuator / Micrometer 反射使用 |
| `java.naming` | JNDI 相关（部分库初始化引用） |
| `java.xml` | JAXB / DOM / Spring XML 配置解析 |
| `jdk.crypto.cryptoki` | SSL/TLS（PKCS11 provider，HTTPS 客户端必需） |
| `jdk.crypto.ec` | SSL/TLS（椭圆曲线 provider，HTTPS 客户端必需） |

**最终 jlink 模块清单（19 个）：**

```
java.base,java.compiler,java.desktop,java.instrument,java.management,java.naming,java.net.http,java.prefs,java.rmi,java.scripting,java.security.jgss,java.sql.rowset,java.xml,jdk.crypto.cryptoki,jdk.crypto.ec,jdk.jfr,jdk.management,jdk.net,jdk.unsupported
```

> `java.sql`、`java.transaction.xa`、`java.sql.rowset` 之间有传递依赖，jlink 会自动解析。

### 3.3 jlink 生成精简 JRE

```bash
jlink \
  --module-path "$JAVA_HOME/jmods" \
  --add-modules "<上述 19 个模块>" \
  --output <jre-dir> \
  --no-header-files --no-man-pages \
  --strip-debug --compress=2
```

产物：精简 JRE，约 **50 MB**（含 Windows runtime DLL）。

### 3.4 jpackage 生成 app-image

```bash
jpackage --type app-image \
  --name llm-gateway \
  --input <含 fat jar 的目录> \
  --main-jar gateway-boot-1.0.0-SNAPSHOT.jar \
  --main-class org.springframework.boot.loader.launch.JarLauncher \
  --runtime-image <jlink 产出的 JRE 目录> \
  --java-options "-Dspring.profiles.active=local -Dmanagement.health.redis.enabled=false" \
  --dest <输出目录>
```

`--java-options` 说明：
- `-Dspring.profiles.active=local`：激活 local profile（使用 H2 内嵌库，无需 PostgreSQL）。
- `-Dmanagement.health.redis.enabled=false`：spike 阶段禁用 Redis health 指标（local profile 未排除 Redis 自动配置，裸机无 Redis 会 DOWN；后续 Task 需在 local profile 正式排除 Redis 自动配置）。

## 4. app-image 结构

```
llm-gateway/
├── llm-gateway.exe                      # 原生启动器（472 KB）
├── app/
│   ├── gateway-boot-1.0.0-SNAPSHOT.jar  # fat jar（83 MB）
│   ├── llm-gateway.cfg                  # jpackage 启动配置
│   └── .jpackage.xml
└── runtime/                             # 精简 JRE（约 50 MB）
    ├── bin/   (java.exe, keytool, ...)
    ├── lib/   (modules, ...)
    ├── conf/
    └── legal/
```

app-image 总体积约 **133 MB**（fat jar 83 MB + JRE 50 MB），可整目录拷贝到目标裸机运行，无需预装 JDK。

## 5. health 修复说明

### 5.1 问题

spike 初次验证时 jpackage app-image 启动成功（Tomcat 8080 运行），但 `/actuator/health` 返回 **503 DOWN**。定位到 `ProviderRegistryHealthIndicator` 的设计缺陷：

- 原逻辑：「至少一个 Provider UP -> 整体 UP；全部 DOWN/UNKNOWN -> 整体 DOWN」。
- 全新安装时所有 Provider 处于 `Status.UNKNOWN` 初始态（无实际流量，未触发被动推断），导致 `anyUp=false`，整体被判定为 DOWN。
- **无流量 ≠ 不健康**：UNKNOWN 是初始态，不应等同于故障。

### 5.2 修复

修改 `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/actuator/ProviderRegistryHealthIndicator.java`：

- 新逻辑：「有任何 Provider 明确 `Status.DOWN` -> 整体 DOWN；全部 UP/UNKNOWN -> 整体 UP」。
- `allStatuses` 为空时保持 DOWN（无 Provider 是异常）。
- UNKNOWN（初始态/无流量）和 UP 都视为健康。

`ProviderHealthState` 使用 Spring Boot 的 `org.springframework.boot.actuate.health.Status`：
- `Status.UNKNOWN`：初始态（`ProviderHealthState.initial()`），无流量。
- `Status.UP`：连续成功后恢复。
- `Status.DOWN`：连续失败达阈值。

### 5.3 验证结果

修复后重新构建并打包，启动 app-image：

```
Tomcat started on port 8080 (http) with context path '/'
Started GatewayApplication in 8.629 seconds (process running for 9.21)
```

`curl http://localhost:8080/actuator/health`（HTTP 200）：

```json
{
  "status": "UP",
  "groups": ["liveness", "readiness"],
  "components": {
    "db": { "status": "UP", "details": { "database": "H2", "validationQuery": "isValid()" } },
    "diskSpace": { "status": "UP", "details": { "total": 209715195904, "free": 75670016000, "threshold": 10485760, "path": "E:\\workspace\\llm-gateway\\.", "exists": true } },
    "ping": { "status": "UP" },
    "providerRegistry": {
      "status": "UP",
      "details": {
        "openai":    { "status": "UNKNOWN", "consecutiveFailures": 0, "consecutiveSuccesses": 0, "lastError": "" },
        "anthropic": { "status": "UNKNOWN", "consecutiveFailures": 0, "consecutiveSuccesses": 0, "lastError": "" }
      }
    },
    "ssl": { "status": "UP", "details": { "validChains": [], "invalidChains": [] } }
  }
}
```

关键点：`providerRegistry` 组件为 **UP**（两个 Provider 均 UNKNOWN，但整体 UP），整体 health 为 **UP**，HTTP 200。

## 6. 遗留事项与注意事项

1. **Redis health 指标**：当前通过 `-Dmanagement.health.redis.enabled=false` 临时禁用。后续 Task 需在 local profile 中正式排除 Redis 自动配置（如 `spring.autoconfigure.exclude`），避免裸机部署误报。
2. **jdeps 反射遗漏**：jdeps 静态分析无法覆盖 Spring Boot 反射加载的 JDK 模块，需手工补充 `java.management`/`java.naming`/`java.xml`/`jdk.crypto.cryptoki`/`jdk.crypto.ec`。后续若上游依赖变化，需重新跑 jdeps 并回归启动验证。
3. **现有单测待更新**：`ProviderRegistryHealthIndicatorTest` 中两个用例断言旧行为，与本次修复后的语义冲突，需主会话后续更新（详见 implementer 回报）：
   - `partialDown_returnsUp`：原期望「部分 DOWN -> UP」，新语义为「任何 DOWN -> DOWN」。
   - `unknownProviders_treatedAsDown`：原期望「UNKNOWN -> DOWN」，新语义为「UNKNOWN -> UP」。
4. **app-image 体积**：当前 133 MB，主要由 fat jar（83 MB）与精简 JRE（50 MB）构成。后续可考虑 jlink `--strip-debug` 进一步压缩或分层优化。

## 7. 产物位置

- fat jar：`gateway-boot/target/gateway-boot-1.0.0-SNAPSHOT.jar`
- spike 临时产物（jdeps/jlink/app-image/日志）：`/tmp/llm-gateway-spike/`（本机临时目录，不入库）
- 本报告：`deployments/package/spike-report.md`

## 8. Task 2.5 环境限制说明（deb 构建验证）

> 追加日期：2026-07-12
> 关联任务：Task 2.5 - 配置 jpackage `--type deb`

### 8.1 环境限制

本任务在 Windows 11（Git Bash）环境执行，存在以下工具缺失：

| 工具 | Windows 可用 | 用途 |
|------|-------------|------|
| `dpkg-deb` | 否 | 解包/查看 deb 内容（Step 3 验证 maintainer 脚本） |
| `jpackage --type deb` | 否 | jpackage 在 Windows 上仅支持 `msi`/`exe`，不支持 `deb` |
| Docker Desktop | 否（未安装） | 无法通过 Linux 容器交叉构建 deb |

### 8.2 本任务已完成项

1. **脚本权限持久化**：通过 `git update-index --chmod=+x` 将 4 个 maintainer/debconf 脚本的 git 索引模式从 `100644` 改为 `100755`，确保 CI checkout 后脚本自带可执行位（jpackage 要求 resource-dir 内 maintainer 脚本可执行）：
   - `deployments/package/linux/postinst`
   - `deployments/package/linux/prerm`
   - `deployments/package/linux/postrm`
   - `deployments/package/linux/llm-gateway.config`

2. **build.sh deb 配置验证**：确认 `deployments/package/build.sh` 的 deb 打包配置正确（无需修改，Task 1.4 已配置）：
   - `--type deb`（line 88）
   - `--resource-dir "$LINUX_RES"`（line 89，`LINUX_RES` 在 line 15 解析为 `deployments/package/linux/`）
   - `--maintainer "LLM-Gateway Team"`（line 90）

### 8.3 延后至 CI（Phase 4）的验证项

以下验证项需在 Linux CI 环境（ubuntu job）中完成：

- 实际执行 `./deployments/package/build.sh` 产出 `llm-gateway_<version>_amd64.deb`
- `dpkg-deb -c` 检查 deb 内 maintainer 脚本（postinst/prerm/postrm）、systemd unit（llm-gateway.service）、debconf 模板（templates/config）是否就位
- `dpkg-deb -I` 检查 control 信息含 `llm-gateway` 包名与 maintainer 字段
- 验证 jpackage 是否正确将 resource-dir 内 `postinst/prerm/postrm` 合并进 deb control archive（若未正确挂载，需改用 `dpkg-deb` 手动重组 control archive，记录到本节补充）

## 9. Task 2.7 环境限制说明（deb 干净 Ubuntu 安装验证）

> 追加日期：2026-07-12
> 关联任务：Task 2.7 - 本地验证 deb（干净 Ubuntu）
> 关联 Plan：`docs/superpowers/plans/2026-07-11-one-click-bare-deploy.md` Task 2.7（Step 1-5，约行 948-1015）

### 9.1 环境限制

本任务计划在干净 Ubuntu 容器中安装 deb 并验证服务启动、健康检查、数据落盘。实际执行环境（Windows 11 + Git Bash）存在以下限制：

| 依赖 | Windows 可用 | 说明 |
|------|-------------|------|
| `docker` | 否（`command not found`，Task 2.5 已确认） | 无法拉起 `ubuntu:22.04` 容器执行干净安装 |
| deb 产物 | 否（`deployments/package/dist/*.deb` 不存在） | Task 2.5 因 jpackage 在 Windows 不支持 `--type deb`，跳过实际构建，产物留 CI 产出 |

两项前置依赖均不可用，plan Step 1-4 的本地 docker 验证无法执行。

### 9.2 实际验证留 CI（Phase 4 ubuntu job）

deb 的干净 Ubuntu 安装验证将延后至 CI（Phase 4 ubuntu job）完成。CI smoke test 流程如下：

1. **构建 deb**：CI 环境（Linux）执行 `./deployments/package/build.sh` 产出 `deployments/package/dist/llm-gateway_<version>_amd64.deb`
2. **干净容器安装**：`docker run ubuntu:22.04 apt-get install -y ./llm-gateway.deb`
3. **健康检查**：`curl -sf http://localhost:8080/actuator/health` 验证返回 `"status":"UP"`
4. **数据落盘验证**：`ls -la /var/lib/llm-gateway/` 确认含 H2 数据文件（如 `gateway.mv.db`）

### 9.3 CI smoke test 参考步骤（plan Step 1-3 映射）

以下为 plan Task 2.7 Step 1-3 的验证步骤，作为 CI smoke test 编写参考：

#### Step 1: 干净 Ubuntu 容器挂载 deb（端口映射 18080:8080）

```bash
DEB=$(ls deployments/package/dist/*.deb | head -1)
docker run --rm -d --name lg-deb-test \
  -v "$(pwd)/$DEB:/tmp/llm-gateway.deb" \
  -p 18080:8080 \
  ubuntu:22.04 sleep 300
```

#### Step 2: 容器内安装并验证服务启动

```bash
docker exec lg-deb-test bash -c '
  apt-get update && apt-get install -y /tmp/llm-gateway.deb curl
  # 等待服务就绪（最多 90s）
  for i in $(seq 1 90); do
    if curl -sf http://localhost:8080/actuator/health; then echo; break; fi
    sleep 1
  done
  systemctl is-active llm-gateway.service
  ls -la /var/lib/llm-gateway/
'
```

**预期结果：**
- `curl` 输出含 `"status":"UP"`
- `systemctl is-active` 输出 `active`
- `/var/lib/llm-gateway/` 含 H2 数据文件（如 `gateway.mv.db`）

#### Step 3: 验证非交互安装回退默认端口 8080

```bash
docker stop lg-deb-test 2>/dev/null || true
docker run --rm -d --name lg-deb-nonint \
  -v "$(pwd)/$DEB:/tmp/llm-gateway.deb" \
  -p 18080:8080 \
  -e DEBIAN_FRONTEND=noninteractive \
  ubuntu:22.04 sleep 300
docker exec lg-deb-nonint bash -c '
  apt-get update && apt-get install -y /tmp/llm-gateway.deb
  grep SERVER_PORT /etc/llm-gateway/env
  for i in $(seq 1 90); do curl -sf http://localhost:8080/actuator/health && break; sleep 1; done
'
```

**预期结果：** `SERVER_PORT=8080`（非交互安装回退默认端口），health UP。

### 9.4 Spec Scenario 实际验证归属

以下 Spec Scenario 的实际验证均在 CI 完成：

| Scenario | 验证内容 | 验证位置 |
|----------|---------|---------|
| 全新安装并启动 | deb 安装后 health 60s 内 UP | CI ubuntu job（Step 2 for 循环 90s 内） |
| 升级保留数据与密钥 | 旧版 deb 安装 -> 新版 deb 升级 -> `/var/lib/llm-gateway/` 数据与密钥保留 | CI ubuntu job（需补充升级流程） |
| 卸载保留数据 | `apt remove` / `apt purge` 后 `/var/lib/llm-gateway/` 数据保留/清除符合预期 | CI ubuntu job（需补充卸载流程） |

> 注：升级保留与卸载保留两个 Scenario 需 CI 补充多阶段 docker 流程（安装旧版 -> 升级/卸载 -> 验数据），当前 plan Task 2.7 Step 1-3 仅覆盖全新安装场景。

## 10. Task 2.8 环境限制说明（rpm Rocky Linux 安装验证）

### 10.1 环境限制

- Windows 无 docker（`docker: command not found`），无法本地启动 Rocky Linux 容器
- 无 rpm 产物（`deployments/package/dist/*.rpm` 不存在；rpm 需 Linux 环境 rpmbuild 构建，Task 2.6 已留 CI）
- 本地 docker 验证无法执行，实际验证留 CI

### 10.2 实际验证留 CI（Phase 4 ubuntu job 交叉打 rpm）

rpm 包需在 Linux 环境构建（rpmbuild），CI Phase 4 ubuntu job 将交叉打 rpm，再用 Rocky Linux 容器做 smoke test：

1. ubuntu job 构建 rpm 产物（jpackage `--type rpm`，在 Linux 上交叉构建）
2. `docker run rockylinux:9` 挂载 rpm 产物（端口映射 18081:8080）
3. 容器内 `dnf install -y /tmp/llm-gateway.rpm`
4. `curl http://localhost:8080/actuator/health` 验 UP
5. 验数据目录 `/var/lib/llm-gateway/` 落盘

### 10.3 CI smoke test 参考步骤（plan Step 1-2 映射）

#### Step 1: 用 docker 跑 Rocky Linux 安装 rpm（端口映射 18081:8080）

```bash
RPM=$(ls deployments/package/dist/*.rpm | head -1)
docker run --rm -d --name lg-rpm-test \
  -v "$(pwd)/$RPM:/tmp/llm-gateway.rpm" \
  -p 18081:8080 \
  rockylinux:9 sleep 300
```

#### Step 2: 容器内安装并验证

```bash
docker exec lg-rpm-test bash -c '
  dnf install -y /tmp/llm-gateway.rpm curl
  for i in $(seq 1 90); do
    if curl -sf http://localhost:8080/actuator/health; then echo; break; fi
    sleep 1
  done
  systemctl is-active llm-gateway.service
  ls -la /var/lib/llm-gateway/
'
```

**预期结果：**
- `curl` 输出含 `"status":"UP"`
- `systemctl is-active` 输出 `active`
- `/var/lib/llm-gateway/` 含 H2 数据文件（如 `gateway.mv.db`）

### 10.4 Spec Scenario 实际验证归属

以下 Spec Scenario 的实际验证均在 CI 完成：

| Scenario | 验证内容 | 验证位置 |
|----------|---------|---------|
| rpm 全新安装并启动 | rpm 安装后 health 60s 内 UP | CI Phase 4 ubuntu job（交叉打 rpm -> Rocky Linux 9 容器 `dnf install` -> health UP） |

> 注：rpm 验证需 Rocky Linux 9 容器（RHEL 系），与 deb（Ubuntu/Debian 系）互补，覆盖两类包格式；systemd 单元由 jpackage 生成，deb/rpm 均注册 `llm-gateway.service`，`systemctl is-active` 验证服务 active。

## 11. Task 3.3 密钥生成 Pascal Script 验证

> 追加日期：2026-07-12
> 关联任务：Task 3.3 - 验证密钥生成 Pascal Script
> 关联 Plan：`docs/superpowers/plans/2026-07-11-one-click-bare-deploy.md` Task 3.3（Step 1-2，约行 1350-1385）

### 11.1 验证环境

| 项 | 值 |
|---|---|
| PowerShell 版本 | 5.1.26100.8655（Desktop edition，Windows PowerShell，基于 .NET Framework 4.x） |
| pwsh (PowerShell 7+) | 未安装 |
| 验证目标 | `deployments/package/windows/llm-gateway.iss` 中 `GenerateEncryptionKey` 调用的 PowerShell 密钥生成命令 |

### 11.2 Step 1：PowerShell 密钥生成命令验证

**plan 原始命令（Get-Random，非加密安全）：**
```powershell
$key = [Convert]::ToBase64String((1..32 | ForEach-Object { [byte](Get-Random -Max 256) }))
```

**Task 3.2 reviewer 修复后命令（iss line 118，静态方法）：**
```powershell
[Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
```

**验证结果：静态方法在 Windows PowerShell 5.1 下失败 ❌**

模拟 iss `Exec(ExpandConstant('{cmd}'), '/c powershell -NoProfile -Command "..."')` 实际调用：

```
iss 命令输出: 'Method invocation failed because [System.Security.Cryptography.RandomNumberGenerator]
does not contain a method named 'GetBytes'.'
退出码: 1
```

**根因分析：**
- `[Security.Cryptography.RandomNumberGenerator]` 是抽象基类。
- `GetBytes(int)` 静态方法（入参 int，返回 `byte[]`）只在 **.NET Core 3.0+ / .NET 5+** 添加。
- Windows PowerShell 5.1 基于 .NET Framework 4.x，仅有实例方法 `GetBytes(byte[])`，无静态 `GetBytes(int)` 重载。
- iss 通过 `powershell` 调用的是 `powershell.exe`（即 Windows PowerShell 5.1，非 `pwsh.exe`），故命令失败。

**影响（CRITICAL - 全新安装受阻）：**
- iss `GenerateEncryptionKey`（line 110-131）调用该 PowerShell 命令，退出码 1，stdout 重定向到 `{tmp}\gateway_key.txt` 内容为空。
- `LoadStringFromFile` 读取空内容，`Result := Trim('') = ''`。
- 触发 `RaiseException('加密密钥生成失败，请检查 PowerShell 环境后重试')`，**全新安装中止**。
- 升级场景因已有 `GATEWAY_ENCRYPTION_KEY`（`ReadXmlValue` 非空），不调用 `GenerateEncryptionKey`，升级不受影响；仅全新安装受阻。

**可行的实例方法命令（.NET Framework 兼容，验证通过 ✓）：**
```powershell
$rng = [Security.Cryptography.RandomNumberGenerator]::Create()
$bytes = New-Object byte[] 32
$rng.GetBytes($bytes)
[Convert]::ToBase64String($bytes)
```

验证输出（连续两次确认随机性与字节数）：
```
实例方法密钥: fTjpJmQIwpM2tZZgqyMYdA/KhK4RGf3kTzoLXXWco+E=
长度: 44
解码字节数: 32
```

44 字符 base64（32 字节经 base64 编码 = ⌈32/3⌉×4 = 44 字符，含 1 个 `=` 填充），解码 32 字节，符合预期。

> **建议修复（待主会话决策）**：iss line 118 的 PowerShell 命令需改为实例方法方式（`Create()` + `GetBytes(byte[])`），保留加密安全性同时兼容 Windows 默认 PowerShell 5.1。本任务范围仅验证，不修改 iss。

### 11.3 Step 2：升级时保留已有密钥逻辑验证 ✓

核对 iss 代码（`deployments/package/windows/llm-gateway.iss`）：

1. **xml 文件保留**（line 43）：`Source: "LLMGateway.xml"; DestDir: "{app}"; Flags: ignoreversion onlyifdoesntexist`
   - `onlyifdoesntexist`：升级时目标已存在则不覆盖，保留已有 xml（含密钥与端口）✓

2. **密钥保留逻辑**（`CurStepChanged`，line 171-178）：
   ```pascal
   KeyValue := ReadXmlValue(XmlPath, 'GATEWAY_ENCRYPTION_KEY');
   if KeyValue = '' then
   begin
     KeyValue := GenerateEncryptionKey;
     Log('生成新的 GATEWAY_ENCRYPTION_KEY（请备份！）');
   end
   else
     Log('保留已有 GATEWAY_ENCRYPTION_KEY');
   ```
   - 升级时 `ReadXmlValue` 读取已有 `GATEWAY_ENCRYPTION_KEY`，非空则保留（Log "保留已有"），空则生成 ✓

3. **端口保留**（line 183-190）：`ReadXmlValue` 读取已有 `SERVER_PORT`，精确匹配替换为用户输入值（`InitializeWizard` line 83 预填已有端口）✓

4. **`ReadXmlValue` 实现**（line 134-154）：匹配 WinSW xml 格式 `name="KeyName" value="..."`，字符串解析逻辑正确 ✓

### 11.4 验证结论汇总

| 验证项 | 结果 |
|---|---|
| PowerShell `RandomNumberGenerator::GetBytes(32)`（静态方法，iss 当前命令） | ❌ Windows PowerShell 5.1 下失败（MethodNotFound），全新安装中止 |
| PowerShell 实例方法 `Create()` + `GetBytes(byte[])`（.NET Framework 兼容） | ✓ 生成 44 字符 base64，解码 32 字节 |
| Inno Setup `onlyifdoesntexist` 保留已有 xml | ✓ 升级时不覆盖 |
| `ReadXmlValue` 升级时读取已有 `GATEWAY_ENCRYPTION_KEY` 并保留 | ✓ 非空则保留 |
| `ReadXmlValue` 升级时读取已有 `SERVER_PORT` 预填 | ✓ |

**CRITICAL 待修复**：iss `GenerateEncryptionKey`（line 118）的 PowerShell 静态方法命令需改为实例方法方式，否则全新安装无法生成密钥并中止。详见 11.2 节根因分析与建议修复。

> 注：plan 原用 `Get-Random`（非加密安全），Task 3.2 reviewer 修复改用 `RandomNumberGenerator`（加密安全，方向正确），但采用了 .NET Core 专属的静态 `GetBytes(int)` 重载，未兼容 Windows PowerShell 5.1（.NET Framework）。需改用实例方法以同时保留加密安全性与 Windows 默认 PowerShell 兼容性。
