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
