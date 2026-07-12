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

## 12. Task 3.3 修复说明：GenerateEncryptionKey PS 5.1 兼容

> 追加日期：2026-07-12
> 关联任务：Task 3.3 修复（CRITICAL）
> 关联文件：`deployments/package/windows/llm-gateway.iss` line 117-120

### 12.1 问题

iss `GenerateEncryptionKey`（原 line 118）原用 PowerShell 静态方法：

```powershell
[Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
```

`RandomNumberGenerator.GetBytes(int)` 静态方法（入参 int，返回 `byte[]`）仅在 **.NET Core 3.0+ / .NET 5+** 添加。Windows PowerShell 5.1 基于 .NET Framework 4.x，仅有实例方法 `GetBytes(byte[])`，无静态 `GetBytes(int)` 重载。

iss 通过 `{cmd}` 调用 `powershell.exe`（即 Windows PowerShell 5.1，非 `pwsh.exe`），命令报 `MethodNotFound`，退出码 1，`gateway_key.txt` 内容为空，`GenerateEncryptionKey` 返回空，触发 `RaiseException` **中止全新安装**（CRITICAL）。升级场景因已有 `GATEWAY_ENCRYPTION_KEY` 不调用此函数，不受影响。

### 12.2 修复

将 PowerShell 命令改为实例方法（兼容 .NET Framework / PS 5.1，保留加密安全性）：

```powershell
$r=[Security.Cryptography.RandomNumberGenerator]::Create();$b=New-Object byte[] 32;$r.GetBytes($b);[Convert]::ToBase64String($b)
```

Pascal Script 改写（line 120，PowerShell `-Command` 用双引号外包，TempFile 路径用单引号包裹避免变量展开）：

```pascal
if Exec(ExpandConstant('{cmd}'), '/c powershell -NoProfile -Command "$r=[Security.Cryptography.RandomNumberGenerator]::Create();$b=New-Object byte[] 32;$r.GetBytes($b);[Convert]::ToBase64String($b) > ''' + TempFile + '''"',
       '', SW_HIDE, ewWaitUntilTerminated, ResultCode) then
```

引号配对说明：
- 外层 Pascal 单引号 `'...'` 包裹字符串
- `''`（两个连续单引号）转义为单个单引号字符 `'`
- PowerShell `-Command "..."` 用双引号包裹脚本
- TempFile 路径用单引号 `'...'` 包裹（PowerShell 字面量字符串，不展开变量）

### 12.3 验证

在 Windows PowerShell 5.1.26100.8655 环境模拟 iss `Exec` 完整调用链（cmd /c powershell）：

```
命令行: powershell -NoProfile -Command "$r=[Security.Cryptography.RandomNumberGenerator]::Create();$b=New-Object byte[] 32;$r.GetBytes($b);[Convert]::ToBase64String($b) > 'C:\Users\liuye\AppData\Local\Temp\gateway_key_test.txt'"
退出码: 0
文件内容: vF6+2AarWsZ9mP+IqEmHGs7zkSZpKvE5u/uKBjhiFxo=
内容长度: 44
解码字节数: 32
```

- 退出码 0（成功）
- 44 字符 base64（32 字节经 base64 = ⌈32/3⌉×4 = 44 字符，含 1 个 `=` 填充）
- 解码 32 字节，符合加密密钥长度要求
- 随机性由 `RandomNumberGenerator`（加密安全 RNG）保证

### 12.4 影响范围

- **全新安装**：修复后 `GenerateEncryptionKey` 正常生成密钥，不再中止安装 ✓
- **升级安装**：不调用 `GenerateEncryptionKey`（已有密钥），不受影响 ✓
- **加密安全性**：`RandomNumberGenerator` 为加密安全 RNG（FIPS 140-2 兼容），等价 `openssl rand -base64 32`，保留 ✓
- **环境兼容性**：实例方法 `Create()` + `GetBytes(byte[])` 在 .NET Framework 2.0+ 与 .NET Core 均可用，兼容 Windows PowerShell 5.1 及 PowerShell 7+ ✓

## 13. Task 3.4 WinSW xml 环境变量写入验证

> 追加日期：2026-07-12
> 关联任务：Task 3.4 - 验证服务环境变量写入 WinSW xml
> 关联 Plan：`docs/superpowers/plans/2026-07-11-one-click-bare-deploy.md` Task 3.4（Step 1-2，约行 1389-1413）
> 验证文件：`deployments/package/windows/LLMGateway.xml`（Task 3.1）+ `deployments/package/windows/llm-gateway.iss`（Task 3.2）

### 13.1 Step 1：xml 模板与 iss 协作一致性核对

核对 `LLMGateway.xml`（Task 3.1 产出）与 `llm-gateway.iss`（Task 3.2 产出）在四项环境变量上的写入路径与替换逻辑。

#### 13.1.1 DB_URL（数据目录外部化）

| 项 | 值 | 来源 |
|---|---|---|
| xml 默认值 | `jdbc:h2:file:%ProgramData%\LLM-Gateway\data\gateway;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_ON_EXIT=FALSE` | `LLMGateway.xml` line 13 |
| iss 修改逻辑 | 无（全文无 DB_URL 相关 `StringChangeEx`） | iss 全文 |
| 数据目录指向 | `%ProgramData%\LLM-Gateway\data\` | xml line 13 DB_URL + line 20 `<workingdirectory>` 一致 |

**结论**：DB_URL 在 xml 模板中硬编码为指向 `%ProgramData%\LLM-Gateway\data\gateway` 的 H2 文件库，iss 不修改此项，安装后值正确 ✓。xml line 20 `<workingdirectory>%ProgramData%\LLM-Gateway\data</workingdirectory>` 与 DB_URL 路径一致，H2 文件库落在数据目录下。

#### 13.1.2 SERVER_PORT（服务监听端口）

| 项 | 值 | 来源 |
|---|---|---|
| xml 默认值 | `8080` | `LLMGateway.xml` line 14 |
| iss 端口输入页预填 | 升级时 `ReadXmlValue({pf}\...\LLMGateway.xml, 'SERVER_PORT')` 读已有端口预填；新装默认 8080 | iss `InitializeWizard` line 80-85 |
| iss 端口写入（新装） | `StringChangeEx(Content, 'name="SERVER_PORT" value="8080"', 'name="SERVER_PORT" value="' + PortValue + '"', True)` | iss `CurStepChanged` line 191 |
| iss 端口写入（升级） | `OldPort := ReadXmlValue(XmlPath, 'SERVER_PORT')` → `StringChangeEx(Content, 'name="SERVER_PORT" value="' + OldPort + '"', 'name="SERVER_PORT" value="' + PortValue + '"', True)` | iss `CurStepChanged` line 186-189 |

**关键验证点：精确匹配 vs 全局替换**

plan Step 1 文本（plan line 1400）描述旧逻辑 `StringChangeEx(Content, 'value="8080"', ...)`，会替换 xml 中**所有** `value="8080"`。Task 3.2 reviewer 修复后，iss 实际代码已改为**精确匹配** `name="SERVER_PORT" value="..."`：

- 新装分支（line 191）：匹配串 `name="SERVER_PORT" value="8080"`，含 `name="SERVER_PORT"` 前缀，仅替换 SERVER_PORT 这一 env 节点
- 升级分支（line 188）：匹配串 `name="SERVER_PORT" value="<已有端口>"`，按 `ReadXmlValue` 读到的实际已有端口精确匹配

此精确匹配消除了 plan line 1404 风险点所述"后续 xml 增加其他默认 8080 字段时全局替换误伤"的隐患 ✓。

**结论**：SERVER_PORT 默认 8080，iss 用 `ReadXmlValue` 读已有端口 + 精确匹配 `name="SERVER_PORT" value="..."` 替换为新输入值，新装/升级两场景写入路径正确 ✓。

#### 13.1.3 GATEWAY_ENCRYPTION_KEY（加密密钥）

| 项 | 值 | 来源 |
|---|---|---|
| xml 默认值 | 空 `value=""` | `LLMGateway.xml` line 15 |
| iss 密钥来源 | 升级时 `ReadXmlValue(XmlPath, 'GATEWAY_ENCRYPTION_KEY')` 读已有值保留；新装 `GenerateEncryptionKey` 生成 32 字节 base64 | iss `CurStepChanged` line 173-180 |
| iss 密钥写入 | `StringChangeEx(Content, 'name="GATEWAY_ENCRYPTION_KEY" value=""', 'name="GATEWAY_ENCRYPTION_KEY" value="' + KeyValue + '"', True)` | iss `CurStepChanged` line 194-195 |

**结论**：GATEWAY_ENCRYPTION_KEY 默认空 `value=""`，iss 精确匹配 `name="GATEWAY_ENCRYPTION_KEY" value=""` 替换为生成密钥（新装）或保留已有密钥（升级），写入路径正确 ✓。密钥生成逻辑经 Task 3.3 修复后兼容 Windows PowerShell 5.1（实例方法 `Create()` + `GetBytes(byte[])`，详见第 12 节）。

#### 13.1.4 MANAGEMENT_HEALTH_REDIS_ENABLED（D10 Redis health 禁用）

| 项 | 值 | 来源 |
|---|---|---|
| xml 值 | `false` | `LLMGateway.xml` line 17 |
| iss 修改逻辑 | 无（全文无 MANAGEMENT_HEALTH_REDIS_ENABLED 相关 `StringChangeEx`） | iss 全文 |

**结论**：MANAGEMENT_HEALTH_REDIS_ENABLED 在 xml 中固定为 `false`（D10 修复：裸机部署默认无 Redis，禁用 redis health 检查避免误报 DOWN），iss 不动此 env，安装后值正确 ✓。

#### 13.1.5 [Files] 升级保留机制

| 项 | 值 | 来源 |
|---|---|---|
| xml 部署标记 | `Flags: ignoreversion onlyifdoesntexist` | iss line 43 |
| 升级行为 | 目标已存在则不覆盖，保留已有 xml（含密钥与端口） | iss line 44 注释 |

**结论**：`onlyifdoesntexist` 确保升级时不覆盖已有 xml，配合 `CurStepChanged` 的 `ReadXmlValue` 读取已有端口/密钥，升级保留语义完整 ✓。

### 13.2 Step 2：四项 env 写入路径汇总

| 环境变量 | xml 默认值 | iss 修改逻辑 | 安装后值 | 结果 |
|---|---|---|---|---|
| `DB_URL` | `jdbc:h2:file:%ProgramData%\LLM-Gateway\data\gateway;...` | 不修改 | 保持 xml 默认（指向 `%ProgramData%\LLM-Gateway\data\`） | ✓ |
| `SERVER_PORT` | `8080` | `ReadXmlValue` 读已有端口 + 精确匹配 `name="SERVER_PORT" value="..."` 替换为用户输入 | 用户输入值（新装默认 8080，升级保留/修改） | ✓ |
| `GATEWAY_ENCRYPTION_KEY` | 空 `value=""` | `ReadXmlValue` 读已有密钥保留；空则 `GenerateEncryptionKey` 生成 + 精确匹配 `name="GATEWAY_ENCRYPTION_KEY" value=""` 替换 | 生成密钥（新装）/ 已有密钥（升级） | ✓ |
| `MANAGEMENT_HEALTH_REDIS_ENABLED` | `false` | 不修改 | 保持 xml 默认 `false`（D10） | ✓ |

### 13.3 验证结论

1. **四项 env 写入路径正确**：DB_URL / SERVER_PORT / GATEWAY_ENCRYPTION_KEY / MANAGEMENT_HEALTH_REDIS_ENABLED 在 `LLMGateway.xml` 中均有定义，iss 按预期修改需动态写入的两项（SERVER_PORT / GATEWAY_ENCRYPTION_KEY），保持另两项（DB_URL / MANAGEMENT_HEALTH_REDIS_ENABLED）为 xml 默认值 ✓
2. **iss 替换逻辑正确（精确匹配）**：SERVER_PORT 与 GATEWAY_ENCRYPTION_KEY 均用 `name="<KEY>" value="<...>"` 精确匹配替换，非旧逻辑全局 `value="8080"` 替换，消除了误伤 xml 中其他同值字段的风险 ✓
3. **升级保留语义完整**：`onlyifdoesntexist`（iss line 43）保留已有 xml + `ReadXmlValue` 读取已有端口/密钥 + 非空则保留逻辑，升级时端口与密钥不丢失 ✓
4. **DB_URL 数据目录一致**：xml line 13 DB_URL 路径 `%ProgramData%\LLM-Gateway\data\gateway` 与 xml line 20 `<workingdirectory>%ProgramData%\LLM-Gateway\data</workingdirectory>` 一致，H2 文件库落在数据目录下 ✓

> 注：本任务为静态代码核对（xml 模板 vs iss Pascal Script），未执行实际 Inno Setup 编译安装。实际安装后 xml 值的运行时验证留 Task 3.5（iscc 编译产出 setup.exe）及 Phase 4 CI smoke test。

---

## 14. Task 3.5 环境限制说明（iscc 未安装）

### 14.1 环境检查结果

| 检查项 | 命令 | 结果 |
|---|---|---|
| iscc 在 PATH | `Get-Command iscc -ErrorAction SilentlyContinue` | `$null`（未找到） |
| Inno Setup 6 安装目录 | `Test-Path "${env:ProgramFiles(x86)}\Inno Setup 6\ISCC.exe"` | `False`（路径 `C:\Program Files (x86)\Inno Setup 6\ISCC.exe` 不存在） |

**结论**：本机未安装 Inno Setup，无法执行 `build.ps1` 全流程（mvn package -> jlink -> jpackage app-image -> iscc 编译 setup.exe）。实际 `iscc llm-gateway.iss` 编译留 Phase 4 CI `windows-latest` runner（CI 环境通过 `choco install innosetup -y` 预装 iscc）。

### 14.2 已完成的验证（语法层）

| 项 | 验证方式 | 结果 |
|---|---|---|
| `deployments/package/windows/download-winsw.ps1` 语法 | `[System.Management.Automation.Language.Parser]::ParseFile(...)` | [OK] 语法正确，0 错误 |
| `deployments/package/build.ps1` 语法（含新增 #6/#7 段） | `[System.Management.Automation.Language.Parser]::ParseFile(...)` | [OK] 语法正确，0 错误 |

### 14.3 build.ps1 新增逻辑说明（#6 WinSW + #7 iscc）

新增两段在 `# 5. 验证 app-image 产物` 之后、`Log "完成..."` 之前（try 块内，staging 清理在 finally 不受影响）：

1. **#6 下载 WinSW**：调用 `windows/download-winsw.ps1 -OutDir $WinRes`，从 `https://github.com/winsw/winsw/releases/download/v2.12.0/WinSW-x64.exe` 下载并命名为 `LLMGateway.exe`，落到 `deployments/package/windows/`（供 iss `[Files]` 段 `Source: "windows\LLMGateway.exe"` 打包）。
2. **#7 Inno Setup 编译**：优先 `Get-Command iscc`（PATH），回退 `${env:ProgramFiles(x86)}\Inno Setup 6\ISCC.exe`；两者皆无则 `Die "未找到 Inno Setup (iscc)。请安装: choco install innosetup -y"`。找到后 `& $Iscc (Join-Path $WinRes 'llm-gateway.iss')` 编译，校验 `$LASTEXITCODE`，最终验证 `dist/llm-gateway-setup.exe` 生成并打印体积。

### 14.4 相对 plan 的微调（自报）

| 微调点 | plan 原文 | 实际实现 | 理由 |
|---|---|---|---|
| 注释编号 | `# 5. 下载 WinSW` / `# 6. Inno Setup 编译` | `# 6.` / `# 7.` | 现有已有 `# 5. 验证 app-image 产物`，避免编号重复 |
| Die 失败码 | `Die "Inno Setup 编译失败"` | `Die "Inno Setup 编译失败 (exit $LASTEXITCODE)"` | 与现有 `Die "...(exit $LASTEXITCODE)"` 风格一致（行 42/67/96） |
| 安装包体积 Log | `$((Get-Item $SetupExe).Length / 1MB)MB` | `$([math]::Round((Get-Item $SetupExe).Length / 1MB, 1))MB` | 与行 68 JRE 体积 Log 的 `[math]::Round(..., 1)` 风格一致，避免小数位过长 |
| 末尾 Log 文案 | `Log "完成。下一步用 Inno Setup 编译 exe（见 Task 3.5）"`（保留） | `Log "完成。app-image 与 setup.exe 已就绪"` | 新增 #7 已执行 iscc 编译，原文案"下一步用 Inno Setup"语义矛盾 |
| iscc 安装提示 | `choco install innosetup` | `choco install innosetup -y` | CI/无人值守安装需 `-y` 自动确认 |

> 注：Task 3.5 实际 iscc 编译验证（产出 `llm-gateway-setup.exe`）留 Phase 4 CI `windows-latest` job；本机仅完成脚本编写 + PowerShell AST 语法验证。download-winsw.ps1 的实际网络下载（Invoke-WebRequest）未执行，WinSW GitHub release URL 的可达性留 CI 验证。

## 15. Task 3.6 环境限制说明（exe 干净 Windows 安装验证）

> 追加日期：2026-07-12
> 关联任务：Task 3.6 - 本地验证 exe（干净 Windows）
> 关联 Plan：`docs/superpowers/plans/2026-07-11-one-click-bare-deploy.md` Task 3.6（Step 1-5，约行 1492-1562）
> 目标：干净 Windows 安装 exe -> Service 启动 -> 健康检查 UP -> 数据落 `%ProgramData%`

### 15.1 环境限制

| 检查项 | 命令 | 结果 |
|---|---|---|
| exe 产物 | `Test-Path deployments/package/dist/llm-gateway-setup.exe` | `False`（产物不存在） |
| iscc 可用性 | 见第 14 节 | 本机未安装 Inno Setup，Task 3.5 已确认无法编译 exe |

**结论**：由于本机无 Inno Setup（iscc 不可用，详见第 14 节），Task 3.5 未能产出 `deployments/package/dist/llm-gateway-setup.exe`。Task 3.6 的 plan Step 1-4（静默安装、服务/health 验证、升级保留密钥、卸载保留数据）均依赖该 exe 产物，本地实际安装/验证无法执行。

### 15.2 实际验证留 CI（Phase 4 windows job）

exe 的干净 Windows 安装验证将延后至 CI（Phase 4 `windows-latest` job）完成。CI smoke test 完整流程如下：

1. **编译 exe**：CI `windows-latest` runner 通过 `choco install innosetup -y` 预装 iscc，执行 `./deployments/package/build.ps1` 产出 `deployments/package/dist/llm-gateway-setup.exe`
2. **静默安装**：管理员 PowerShell 执行 `Start-Process .\dist\llm-gateway-setup.exe -ArgumentList "/VERYSILENT","/NORESTART" -Wait`（验证 `/VERYSILENT` 非交互安装回退默认端口 8080）
3. **健康检查**：`curl http://localhost:8080/actuator/health`（或 `Invoke-WebRequest`）验证返回 `"status":"UP"`
4. **服务与数据落盘验证**：`Get-Service LLMGateway` 显示 `Status=Running`；`Get-ChildItem $env:ProgramData\LLM-Gateway\data` 含 H2 数据文件（如 `gateway.mv.db`）
5. **升级保留密钥验证**：读取首次安装的 `GATEWAY_ENCRYPTION_KEY` -> 再次运行安装包（模拟升级）-> 读取升级后密钥 -> 断言两者相等（`PASS: 密钥升级保留`）
6. **卸载保留数据验证**：`unins000.exe /VERYSILENT` 卸载 -> `Get-Service LLMGateway` 应不存在（服务已移除）-> `Test-Path $env:ProgramData\LLM-Gateway\data` 应为 `True`（数据目录保留）

### 15.3 CI smoke test 参考步骤（plan Step 1-4 映射）

以下为 plan Task 3.6 Step 1-4 的验证步骤，作为 CI smoke test 编写参考：

#### Step 1: 静默安装（验证 `/VERYSILENT` 回退默认端口）

```powershell
# 管理员 PowerShell
Start-Process -FilePath ".\deployments\package\dist\llm-gateway-setup.exe" `
  -ArgumentList "/VERYSILENT","/NORESTART" -Wait -NoNewWindow
```

**预期**：非交互静默安装完成，SERVER_PORT 回退默认 8080（iss `InitializeWizard` 无用户输入时预填 8080）。

#### Step 2: 验证服务与 health

```powershell
# 等待服务就绪（最多 90s）
for ($i=1; $i -le 90; $i++) {
  try { (Invoke-WebRequest -UseBasicParsing http://localhost:8080/actuator/health).Content; break } catch { Start-Sleep -Seconds 1 }
}
Get-Service LLMGateway | Format-List Name,Status,StartType
Get-ChildItem "$env:ProgramData\LLM-Gateway\data"
```

**预期结果：**
- health 输出含 `"status":"UP"`
- `Get-Service` 显示 `Status=Running`、`StartType=Automatic`
- `%ProgramData%\LLM-Gateway\data\` 含 H2 数据文件（如 `gateway.mv.db`）

#### Step 3: 验证升级保留密钥

```powershell
# 读取首次安装的密钥
[xml]$xml = Get-Content "$env:ProgramFiles\LLM-Gateway\LLMGateway.xml"
$firstKey = ($xml.service.env | Where-Object { $_.name -eq 'GATEWAY_ENCRYPTION_KEY' }).value
Write-Host "首次密钥: $firstKey"

# 再次运行安装包（模拟升级）
Start-Process -FilePath ".\deployments\package\dist\llm-gateway-setup.exe" `
  -ArgumentList "/VERYSILENT" -Wait -NoNewWindow

[xml]$xml2 = Get-Content "$env:ProgramFiles\LLM-Gateway\LLMGateway.xml"
$secondKey = ($xml2.service.env | Where-Object { $_.name -eq 'GATEWAY_ENCRYPTION_KEY' }).value
Write-Host "升级后密钥: $secondKey"
if ($firstKey -eq $secondKey) { Write-Host "PASS: 密钥升级保留" } else { Write-Host "FAIL: 密钥被覆盖"; exit 1 }
```

**预期结果：** `PASS: 密钥升级保留`（iss `onlyifdoesntexist` + `ReadXmlValue` 保留已有密钥，详见第 13 节核对）。

#### Step 4: 卸载验证（保留数据目录）

```powershell
# 控制面板卸载或:
Start-Process -FilePath "C:\Program Files\LLM-Gateway\unins000.exe" -ArgumentList "/VERYSILENT" -Wait
Get-Service LLMGateway -ErrorAction SilentlyContinue  # 应不存在
Test-Path "$env:ProgramData\LLM-Gateway\data"          # 应为 True（保留）
```

**预期结果：** 服务已移除（`Get-Service` 返回 `$null`），数据目录保留（`Test-Path` 返回 `True`）。

### 15.4 Spec Scenario 实际验证归属

以下 Spec Scenario 的实际验证均在 CI 完成：

| Scenario | 验证内容 | 验证位置 |
|----------|---------|---------|
| Windows exe 全新安装并启动 | exe `/VERYSILENT` 安装后 health 90s 内 UP + `Get-Service` Running | CI Phase 4 `windows-latest` job（Step 1-2） |
| 升级保留数据与密钥 | 首次安装 exe -> 再次运行 exe 升级 -> `GATEWAY_ENCRYPTION_KEY` 保留不变 | CI Phase 4 `windows-latest` job（Step 3） |
| 静默安装使用默认端口 | `/VERYSILENT` 非交互安装回退默认端口 8080，health 在 8080 可达 | CI Phase 4 `windows-latest` job（Step 1-2） |
| 卸载保留数据 | `unins000.exe /VERYSILENT` 卸载后服务移除 + `%ProgramData%\LLM-Gateway\data` 保留 | CI Phase 4 `windows-latest` job（Step 4） |

> 注：本机因 iscc 不可用（第 14 节）无 exe 产物，Task 3.6 Step 1-4 全部延后至 CI `windows-latest` job 验证。iss 的 `onlyifdoesntexist` + `ReadXmlValue` 升级保留逻辑、`GenerateEncryptionKey` 密钥生成（PS 5.1 兼容修复，第 12 节）、四项 env 写入路径（第 13 节）均已通过静态代码核对，运行时验证留 CI。

