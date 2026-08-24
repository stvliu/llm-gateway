# P4.5 Maven 坐标两级命名体系 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 落地 2026-08-24 确认的两级命名体系——`gateway-` 前缀标识「构建聚合/根工具模块」，域内业务模块用短名（核心子模块 artifactId 去前缀），域父 POM 用根 groupId + 无 parent 后缀。

**Architecture:** 域父 POM `com.codingas.gateway : gateway-<域>`（根 groupId，无 parent 后缀）；核心子模块 `com.codingas.gateway.<域> : <域>`（去 `gateway-` 前缀）；data/starter `<域>-data` / `<域>-starter`；协议插件 `com.codingas.gateway.protocol : protocol-openai/anthropic/gemini`；根工具/应用模块（common/boot/cli/simulator/web）不变。目录本为短名（`provider/`），去前缀后**目录 = artifactId 天然一致**（无需改目录）。纯 pom 坐标重构：包名/Java 代码不变，仅 groupId/artifactId/parent 引用连锁替换（XML 标签级精确替换防误伤）。

**Tech Stack:** Maven 多模块（域父 POM 层级聚合）、Java 21

## Global Constraints

- 全量 `./mvnw clean install` 每任务末尾必须绿（含测试）
- 每任务独立提交，commit message 中文
- 行为不变：包名/Java 代码/依赖方向不变，仅 Maven 坐标改名
- **坐标映射**（替换以 XML 标签级精确匹配 `<artifactId>X</artifactId>`，天然避免子串误伤）：

  | 域 | 域父 POM（groupId→根） | 核心 | data | starter |
  |---|---|---|---|---|
  | protocol | `gateway-protocol-parent`→`gateway-protocol` | `gateway-protocol`→`protocol` | — | — |
  | provider | `gateway-provider-parent`→`gateway-provider` | `gateway-provider`→`provider` | `gateway-provider-data`→`provider-data` | `gateway-provider-starter`→`provider-starter` |
  | iam | 同上模式 | `gateway-iam`→`iam` | `gateway-iam-data`→`iam-data` | `gateway-iam-starter`→`iam-starter` |
  | usage | | `gateway-usage`→`usage` | `gateway-usage-data`→`usage-data` | `gateway-usage-starter`→`usage-starter` |
  | security | | `gateway-security`→`security` | `gateway-security-data`→`security-data` | `gateway-security-starter`→`security-starter` |
  | audit | | `gateway-audit`→`audit` | `gateway-audit-data`→`audit-data` | `gateway-audit-starter`→`audit-starter` |
  | alert | | `gateway-alert`→`alert` | `gateway-alert-data`→`alert-data` | `gateway-alert-starter`→`alert-starter` |
  | resilience | | `gateway-resilience`→`resilience` | `gateway-resilience-data`→`resilience-data` | `gateway-resilience-starter`→`resilience-starter` |
  | proxy | | `gateway-proxy`→`proxy` | — | `gateway-proxy-starter`→`proxy-starter` |
  | stats | | `gateway-stats`→`stats` | — | `gateway-stats-starter`→`stats-starter` |

- 协议插件：`gateway-protocol-openai`→`protocol-openai`、`gateway-protocol-anthropic`→`protocol-anthropic`、`gateway-protocol-gemini`→`protocol-gemini`
- **不改**：`gateway-project`（根 pom）、`gateway-common`、`gateway-boot`、`gateway-cli`、`gateway-simulator`、`gateway-web`
- **groupId 变化**：仅域父 POM 自身 groupId（`com.codingas.gateway.<域>` → `com.codingas.gateway`）与其 29 个子模块的 `<parent>` 块 groupId；子模块自身 groupId（域 groupId）不变
- **name 标签**：保持友好名（如 "Gateway Provider"）不改——「模块名与 artifactId 一致」由目录名满足（`provider/` ↔ `provider`），name 是展示名（若需对齐另议）
- 2 个遗留死 `*IT` 改名（N3）：boot 的 `ChannelProvisionTransactionalIT`、`ChannelHealthRepositoryIT` → `*IntegrationTest`（failsafe 拾取）

---

## Task 1: 基线验证

**Files:**
- 无

**Interfaces:**
- Consumes: 无
- Produces: 基线全绿

- [ ] **Step 1: 全量构建 + 测试**

```bash
cd /e/workspace/llm-gateway
./mvnw clean install
```

Expected: BUILD SUCCESS，全部测试绿（master `98ee1f49`）。

- [ ] **Step 2: 确认无失败**

若有失败先排查（systematic-debugging）。

---

## Task 2: 坐标改名（原子切换）

> **原子切换**：域父 POM 改名 ↔ 子模块 parent 引用 ↔ 依赖声明必须同一任务完成——任一中断构建必失败（parent 或依赖坐标解析不到）。按「先域父 → 再子模块自身 → 再依赖声明 → 全量验证」推进，**最终 `./mvnw clean install` 全绿**。

**Files:**
- Modify: 10 个域父 POM（`gateway-<域>/pom.xml`）
- Modify: 29 个域内子模块 pom
- Modify: 根 pom + 全部引用改名 artifactId 的 pom（依赖声明）

**Interfaces:**
- Consumes: 无
- Produces: 全部坐标按 Global Constraints 映射落地，构建绿

- [ ] **Step 1: 域父 POM 改名（10 个）**

每域父 POM（如 `gateway-provider/pom.xml`）：

```xml
<!-- 改前 -->
<groupId>com.codingas.gateway.provider</groupId>
<artifactId>gateway-provider-parent</artifactId>

<!-- 改后 -->
<groupId>com.codingas.gateway</groupId>
<artifactId>gateway-provider</artifactId>
```

（协议域：`com.codingas.gateway.protocol` + `gateway-protocol-parent` → `com.codingas.gateway` + `gateway-protocol`。其余 9 域同构。）

- [ ] **Step 2: 子模块 parent 引用（29 个）**

每个域内子模块 pom 的 `<parent>` 块（如 `gateway-provider/provider/pom.xml`）：

```xml
<!-- 改前 -->
<parent>
    <groupId>com.codingas.gateway.provider</groupId>
    <artifactId>gateway-provider-parent</artifactId>
    ...
<!-- 改后 -->
<parent>
    <groupId>com.codingas.gateway</groupId>
    <artifactId>gateway-provider</artifactId>
    ...
```

（relativePath `../pom.xml` 不变。）

- [ ] **Step 3: 子模块自身 artifactId 去前缀（29 个）**

每个子模块 `<artifactId>` 按 Global Constraints 映射改名（如 `gateway-provider` → `provider`、`gateway-provider-data` → `provider-data`、`gateway-provider-starter` → `provider-starter`；协议插件 `gateway-protocol-openai` → `protocol-openai` 等）。子模块自身 groupId（域 groupId）不变。

- [ ] **Step 4: 全部依赖声明 XML 标签级替换**

对全仓所有 pom（含根 pom、boot、各模块依赖声明），按 Global Constraints 映射做 **XML 标签级精确替换**（`<artifactId>gateway-provider</artifactId>` → `<artifactId>provider</artifactId>` 等）。用脚本批量处理（建议按映射表逐一 sed 精确标签替换，先长后短也可，但标签级匹配本身防误伤）。替换后验证：

```bash
grep -rn "gateway-provider\|gateway-iam\|gateway-usage\|gateway-security\|gateway-audit\|gateway-alert\|gateway-resilience\|gateway-proxy\|gateway-stats\|gateway-protocol" --include="pom.xml" . | grep -v target
```

Expected: 仅剩合法引用——`gateway-project`（根）、`gateway-common/boot/cli/simulator/web`（根工具模块，不改）、各域父 POM 的新 artifactId（如 `gateway-provider` 是父，合法）。

- [ ] **Step 5: 全量构建验证**

```bash
./mvnw clean install
```

Expected: BUILD SUCCESS（Maven 坐标全部解析；ArchUnit 7 条铁律按包名不受影响仍绿；所有测试绿）。

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "build: Maven 坐标两级命名体系——核心子模块去前缀 + 域父 POM 根 groupId 无 parent 后缀（P4.5）
Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

## Task 3: 设计文档落地 + 死 IT 改名 + 全量回归

**Files:**
- Modify: `docs/superpowers/specs/2026-08-21-gateway-jmix-style-modularization-design.md`
- Modify: `gateway-boot/src/test/.../adapter/api/ChannelProvisionTransactionalIT.java`、`ChannelHealthRepositoryIT.java`（改名 `*IntegrationTest`）

**Interfaces:**
- Consumes: Task 2 全部产物
- Produces: 设计文档与实现一致；死测试拾取；回归绿

- [ ] **Step 1: 设计文档 §4.5 更新**

- 删除「2026-08-24 决策（命名规范，待执行）」中的「待执行」标注 → 改为「已执行（2026-08-24 P4.5）」
- 清理「2026-08-24 决策（偏离原方案）」中与命名决策冲突的过渡表述（域父 POM `gateway-<域>-parent` → 更新为 `gateway-<域>`，标注命名决策为准）
- §4.5 groupId 表同步更新（域父 POM 用根 groupId、核心模块 artifactId 去前缀）

- [ ] **Step 2: 2 个死 *IT 改名**

`gateway-boot/src/test/java/com/codingas/gateway/adapter/api/ChannelProvisionTransactionalIT.java` → `ChannelProvisionTransactionalIntegrationTest.java`；`ChannelHealthRepositoryIT.java` → `ChannelHealthRepositoryIntegrationTest.java`（类名/文件名同步，failsafe `*IntegrationTest` 拾取）。

- [ ] **Step 3: 全量回归**

```bash
./mvnw clean install
```

Expected: BUILD SUCCESS，全部测试绿（含新拾取的 2 个集成测试；ArchUnit 铁律仍绿）。

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "docs: 命名规范决策落地（§4.5 两级命名体系已执行）+ 死测试改名拾取（P4.5）
Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

## Self-Review 记录

**Spec 覆盖对照**：
- 域父 POM 根 groupId + 无 parent → Task 2 Step 1
- 核心子模块去前缀 → Task 2 Step 3
- data/starter/插件同名模式 → Task 2 Step 3
- 依赖声明连锁替换 → Task 2 Step 4
- 目录=artifactId（天然一致，无需改目录）→ Global Constraints
- 设计文档落地 + 过渡清理 → Task 3 Step 1
- 死 IT 改名 → Task 3 Step 2

**Placeholder 扫描**：坐标映射表完整；替换策略明确；无 TBD。

**Type/命名一致性**：
- 映射表（gateway-provider→provider 等）与设计文档 §4.5 决策一致 ✓
- 根工具模块（common/boot/cli/simulator/web/project）不改 ✓
- name 标签不改（目录名满足「模块名=artifactId」）✓
- XML 标签级替换防子串误伤（gateway-provider 不误伤 gateway-provider-data）✓

**风险**：
- 坐标改名中断 → 原子切换（Task 2 单任务完成）
- 子串误伤 → 标签级精确匹配
- ArchUnit 按包名不受影响 ✓
- 域父 POM 改名后与核心子模块（gateway-provider vs provider）不同名——无坐标冲突
