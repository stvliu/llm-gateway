# one-click-bare-deploy 验证报告

> **change**: one-click-bare-deploy
> **分支**: feature/20260711/one-click-bare-deploy
> **base-ref**: f4b92150
> **验证日期**: 2026-07-13
> **验证人**: verify subagent
> **验证模式**: full（openspec-verify-change 7 项检查）

## Summary Scorecard

| 维度 | 状态 | 说明 |
|------|------|------|
| **Completeness** | PASS | tasks 28/28 [x]；delta spec 8 个 Requirement 全部有实现证据 |
| **Correctness** | PASS | D10 修复正确（UNKNOWN 视为 UP，测试覆盖）；Windows 用 exe 符合 Spec Patch；密钥生成安全（openssl rand / RandomNumberGenerator） |
| **Coherence** | PASS | design.md D1-D9 + Design Doc D1-D10 一致；delta spec 与 Design Doc 无矛盾；D10 修复已在 Design Doc 记录 |

**整体状态: PASS（ready for archive）**

## 7 项验证详情

### 1. tasks.md 全部完成（28/28 [x]）- PASS

```
grep -c '^- \[ \]' tasks.md  => 0   （未勾选）
grep -c '^- \[x\]' tasks.md  => 28  （已勾选）
```

提交历史清晰：每个 task 验收勾选 commit + 实现 commit + reviewer 修复 commit 齐全（`git log f4b92150..HEAD --oneline` 显示完整链路）。

### 2. 实现符合 design.md 高层设计决策（D1-D9）- PASS

| 决策 | 实现证据 | 文件 |
|------|---------|------|
| D1 jpackage 打包 | `build.sh`/`build.ps1` 调用 jpackage，jlink 精简 JRE（19 模块） | `deployments/package/build.sh:53-58`, `build.ps1:66` |
| D2 默认 local profile | `--java-options "-Dspring.profiles.active=local"` | `build.sh:82`, `build.ps1:94` |
| D3 DB_URL 数据目录外部化 | env 文件 `DB_URL=jdbc:h2:file:/var/lib/llm-gateway/gateway;...`；WinSW xml `DB_URL` 指向 `%ProgramData%\LLM-Gateway\data\gateway` | `linux/postinst:56`, `windows/LLMGateway.xml:13` |
| D4 端口交互 debconf/Inno Setup | debconf templates + config（Linux）；Inno Setup 端口输入页 `PortPage`（Windows）；不校验占用 | `linux/llm-gateway.templates`, `windows/llm-gateway.iss:76-107` |
| D5 密钥自动生成升级保留 | `openssl rand -base64 32`（Linux）；PowerShell `RandomNumberGenerator` 32 字节 base64（Windows）；已存在则保留 | `linux/postinst:46`, `windows/llm-gateway.iss:110-133` |
| D6 服务注册 systemd/WinSW | systemd unit + `EnvironmentFile` + `Restart=on-failure`；WinSW xml `<onfailure action="restart">` | `linux/llm-gateway.service`, `windows/LLMGateway.xml:30-32` |
| D7 CI matrix ubuntu/windows | `release.yml` package job `matrix: [ubuntu-latest, windows-latest]` | `.github/workflows/release.yml:117-124` |
| D8 jpackage + JarLauncher | `--main-jar gateway-boot-<ver>.jar --main-class org.springframework.boot.loader.launch.JarLauncher` | `build.sh:79-80`, `build.ps1:92-93` |
| D9 Inno Setup exe + WinSW（放弃 msi） | jpackage `--type app-image` + WinSW + Inno Setup `iscc` 编译 | `build.ps1:84-95`, `windows/llm-gateway.iss` |

### 3. 实现符合 Design Doc（D1-D10，含 D10 修复）- PASS

D1-D9 同上。D10 修复验证：

**D10 修复点 1 - ProviderRegistryHealthIndicator 逻辑调整**:

`gateway-boot/src/main/java/com/codingas/gateway/infrastructure/actuator/ProviderRegistryHealthIndicator.java:41`:
```java
// 只有明确 DOWN 才视为不健康；UNKNOWN（初始态/无流量）视为健康
if (state.status() == Status.DOWN) {
    anyDown = true;
}
```
- 新逻辑：有任何 Provider 明确 `Status.DOWN` -> 整体 DOWN；全部 UP/UNKNOWN -> 整体 UP。
- 符合 D10"只有 provider 明确 DOWN 才整体 DOWN；UNKNOWN 视为 UP"。

**D10 修复点 2 - 测试覆盖**:

`ProviderRegistryHealthIndicatorTest.java` 5 个用例：
- `allProvidersUp_returnsUp` - 全 UP -> UP ✓
- `partialDown_returnsDown` - 部分 DOWN -> DOWN ✓
- `allDown_returnsDown` - 全 DOWN -> DOWN ✓
- `noProviders_returnsDown` - 无 Provider -> DOWN（预存在逻辑，D10 未改）
- `unknownProviders_treatedAsUp` - UNKNOWN -> UP ✓（D10 核心验证用例）

**D10 修复点 3 - Redis env 禁用**:

- Linux env 文件: `MANAGEMENT_HEALTH_REDIS_ENABLED=false`（`postinst:62`, `postinst-rpm:53`）
- Windows WinSW xml: `<env name="MANAGEMENT_HEALTH_REDIS_ENABLED" value="false"/>`（`LLMGateway.xml:17`）
- build.sh/build.ps1 `--java-options` 也含 `-Dmanagement.health.redis.enabled=false`（双重保险）

**D10 修复点 4 - spike 验证证据**:

`spike-report.md` 5.3 节记录：修复后 `/actuator/health` 返回 HTTP 200，`providerRegistry` 组件 UP（openai/anthropic 均 UNKNOWN 但整体 UP），整体 status UP。

### 4. 能力规格场景全部通过（delta spec 8 个 Requirement）- PASS（静态核对）

| Requirement | Scenario | 实现证据 | 运行时验证状态 |
|-------------|----------|---------|---------------|
| Linux deb 部署 | 全新安装并启动 | postinst 注册 systemd + 生成密钥 + enable --now + 启动 | 留 CI smoke test（release.yml deb 容器验证） |
| Linux deb 部署 | 升级保留数据与密钥 | postinst 检测 `GATEWAY_ENCRYPTION_KEY` 已存在则保留；env 文件 conffile；数据目录不碰 | 留 CI/用户本地升级测试 |
| Linux deb 部署 | 卸载保留数据 | postrm `remove` 清 unit 保留数据；`purge` 才清数据 | 留 CI/用户本地 |
| Linux rpm 部署 | 全新安装并启动 | postinst-rpm 等价逻辑（无 debconf，端口从 env 或 SERVER_PORT 默认） | 留 CI smoke test（release.yml rpm 容器验证） |
| Windows exe 部署 | 全新安装并启动 | Inno Setup [Run] `winsw install` + `winsw start`；WinSW xml 配环境变量 | 留 CI smoke test（release.yml windows 静默安装验证） |
| Windows exe 部署 | 升级保留数据与密钥 | `[Files]` xml `onlyifdoesntexist`；`CurStepChanged` 读已有密钥/端口不覆盖；`UsePreviousAppDir=yes` | 留用户本地升级测试 |
| Windows exe 部署 | 静默安装默认端口 | `/VERYSILENT` 支持；`PortPage.Values[0]` 默认 8080 | 留 CI smoke test |
| 端口交互设置 | Linux debconf | templates + config 脚本，默认 8080，非交互回退 | 留 CI/用户本地 |
| 端口交互设置 | 非交互安装默认端口 | debconf Default: 8080；`DEBIAN_FRONTEND=noninteractive` 回退 | 留 CI/用户本地 |
| 端口交互设置 | 端口冲突运行时暴露 | 不校验占用；systemd `Restart=on-failure` / WinSW `onfailure restart` | 留用户本地 |
| 端口交互设置 | Windows 安装向导设置端口 | Inno Setup `PortPage` 输入框，校验数字范围 1-65535 | 留 CI/用户本地 |
| 加密密钥持久化 | 全新安装生成密钥 | `openssl rand -base64 32` / `RandomNumberGenerator` 32 字节 base64 | 留 CI/用户本地 |
| 加密密钥持久化 | 升级保留密钥 | postinst 检测已存在保留；Inno Setup 读已有不覆盖 | 留用户本地升级测试 |
| 数据目录外部化 | 数据落标准目录 | DB_URL 指向 `/var/lib/llm-gateway/`（Linux）/ `%ProgramData%\LLM-Gateway\data\`（Windows） | 留 CI smoke test |
| CI 多平台打包 | release 产出多平台包 | release.yml package job matrix + finalize 挂 Release | 留用户发布（推 tag 触发） |
| Docker 资产修复 | docker-compose 正常构建 | Dockerfile 路径改 `gateway-boot`；compose context 改根目录 + gateway-console 服务 | 留 CI/用户本地（docker 环境） |

**说明**: Scenario 的逻辑实现全部到位（静态核对 + spike 验证 health UP 已覆盖核心场景）。实际运行时验证（deb/rpm/exe 构建产物安装 + health UP + 数据落盘）受 Windows 环境限制（无 docker/iscc/dpkg-deb/rpm），留 CI smoke test 或用户本地完成。

### 5. proposal.md 目标已满足（Why/What Changes）- PASS

**Why 目标**:
1. 修复 Docker 失配（Dockerfile/compose 旧多模块路径） -> Dockerfile 改 `gateway-boot` 单模块，compose context 改根目录 ✓
2. 补齐非 Docker 部署形态（deb/rpm/exe） -> jpackage 打包流水线完整 ✓

**What Changes 全部实现**:
- jpackage 打包流水线（deb/rpm/exe + jlink 精简 JRE + local profile）✓
- 服务注册与生命周期（systemd + postinst/prerm/postrm；WinSW + Inno Setup）✓
- 安装时配置（debconf/Inno Setup 端口 -> SERVER_PORT；密钥自动生成；DB_URL 数据目录外部化）✓
- CI 打包 job（release.yml package job matrix）✓
- Docker 修复（Dockerfile + docker-compose）✓

**偏差说明**: proposal.md 第 36 行说"不涉及 gateway-boot 业务 Java 源码"，实际修改了 `ProviderRegistryHealthIndicator.java`。这是 build 阶段 spike 发现的预存在设计缺陷，经用户确认作为中等变更修复，Design Doc D10 已完整记录（actuator 基础设施，非业务逻辑）。属有记录的偏差，不影响目标达成。

### 6. delta spec 与 design doc 无矛盾 - PASS

| 对照点 | delta spec | Design Doc | 一致性 |
|--------|-----------|------------|--------|
| Windows 安装包类型 | "Windows exe 安装包"（Inno Setup + WinSW） | D9 "放弃 msi，改 Inno Setup exe + WinSW" | 一致 ✓ |
| 端口交互 | debconf / Inno Setup 向导 -> SERVER_PORT | D4 同 | 一致 ✓ |
| 密钥持久化 | 首次生成，升级不变 | D5 同 | 一致 ✓ |
| 数据目录 | DB_URL 指向标准目录 | D3 同（Design Doc 补充完整 URL） | 一致 ✓ |
| 服务注册 | systemd / Windows Service | D6 同 | 一致 ✓ |
| CI 多平台 | release tag 触发，ubuntu/windows 各产出 | D7 同 | 一致 ✓ |
| Docker 修复 | Dockerfile/compose 路径失配修复 | Design Doc "Docker 资产修复"章节 | 一致 ✓ |
| health UP 场景 | Scenario 要求 60 秒内 UP | D10 修复 health indicator + Redis env | 一致（D10 为 spec 场景提供实现保障）✓ |

### 7. Design Doc 可定位 - PASS

文件存在: `docs/superpowers/specs/2026-07-11-one-click-bare-deploy-design.md`（278 行，status: final）
.comet.yaml `design_doc` 字段指向同一路径。

## Issues by Priority

### CRITICAL（0 项）

无。

### WARNING（0 项）

无。

### SUGGESTION（3 项）

1. **proposal.md "不涉及业务 Java 源码"与 D10 修复的偏差**
   - 文件: `openspec/changes/one-click-bare-deploy/proposal.md:36`
   - 现状: proposal 原文说"不涉及 gateway-boot 业务 Java 源码"，实际修改了 `ProviderRegistryHealthIndicator.java`。
   - 评估: D10 修复是 actuator 基础设施（非业务逻辑），build 阶段经用户确认，Design Doc D10 已完整记录。属有记录的偏差。
   - 建议: 归档前可在 proposal Impact 章节补充一句说明 D10 修复导致的 Java 源码变更（非必须，Design Doc 已覆盖）。

2. **`noProviders_returnsDown` 边界与 D10 语义的关系**
   - 文件: `gateway-boot/src/test/java/.../ProviderRegistryHealthIndicatorTest.java:77-84`
   - 现状: 无 Provider 时（`allStatuses.isEmpty()`）返回 DOWN。D10 修复聚焦"有 Provider 但全 UNKNOWN"场景，未改空集合分支。
   - 评估: spike 验证全新安装时 `allStatuses` 非空（内置 openai/anthropic provider），此分支实际不触发。不影响 delta spec "全新安装 health UP" 场景。
   - 建议: 如需更严格对齐 D10"无流量≠不健康"语义，可考虑空集合也返回 UP（当前 DOWN 是预存在逻辑，保留也无害）。

3. **base_ref 记录差异（信息性）**
   - 文件: `openspec/changes/one-click-bare-deploy/.comet.yaml:13`
   - 现状: .comet.yaml `base_ref: 1aaa0c01`（master archive commit），任务说明 base-ref `f4b92150`（official-website 文档站 commit）。
   - 评估: 两者均在 master 主线上，f4b92150 是本 feature 分支的实际起点。diff 范围一致（51 files, 6877 insertions），不影响验证结论。
   - 建议: 无需处理，记录差异即可。

## 环境限制说明（实际验证留 CI/用户）

以下 Scenario 的实际运行时验证受 Windows 环境限制（无 docker/iscc/dpkg-deb/rpm），已通过**静态核对 + spike 验证**覆盖实现正确性，实际运行时验证留 CI smoke test 或用户本地：

| 留验证项 | 验证方式 | 覆盖现状 |
|---------|---------|---------|
| deb 实际构建 + 干净 Ubuntu 安装 + health UP + 数据落盘 | CI release.yml `Smoke test - deb`（systemd-ubuntu 容器） | 静态核对 postinst/build.sh + spike health UP 证据 |
| rpm 实际构建 + Rocky Linux 安装 + health UP | CI release.yml `Smoke test - rpm`（systemd-rockylinux 容器） | 静态核对 postinst-rpm/build.sh |
| exe 实际构建 + Windows 静默安装 + service 启动 + health UP | CI release.yml `Smoke test - exe`（windows runner /VERYSILENT） | 静态核对 iss/build.ps1 + spike app-image 启动验证 |
| release tag 端到端触发 + 产物挂 Release | 用户发布时推 tag | release.yml finalize job 配置已核对 |
| docker-compose up 正常构建拉起 | CI/用户本地（docker 环境） | Dockerfile/compose 静态核对 + gateway-console Dockerfile 存在 |
| 升级保留密钥/数据端到端 | 用户本地 VM 升级测试 | 脚本逻辑静态核对（conffile/onlyifdoesntexist） |

**核心结论**: spike 已验证技术路线可行（jpackage + fat jar 启动成功，health UP，含 D10 修复后的 providerRegistry UP 证据）。环境限制不影响实现正确性判断。

## Final Assessment

**状态: PASS - ready for archive**

- **Completeness**: tasks 28/28，delta spec 8 个 Requirement 全部实现，证据充分。
- **Correctness**: D10 修复正确（代码 + 测试 + spike 证据），Windows exe 符合 Spec Patch，密钥生成安全，各 Scenario 逻辑实现到位。
- **Coherence**: design.md D1-D9 + Design Doc D1-D10 一致，delta spec 与 Design Doc 无矛盾，D10 修复已记录。
- **CRITICAL**: 0
- **WARNING**: 0
- **SUGGESTION**: 3（均为非阻塞性改进建议）

实现完整、正确、一致，可进入 archive 阶段。运行时 Scenario 验证（deb/rpm/exe 构建安装 + health UP）留 CI smoke test 或用户本地，符合环境限制说明。
