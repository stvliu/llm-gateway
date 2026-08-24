# P4.5 Jmix 命名规范严格对齐 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 严格对齐 Jmix 模块命名规范：统一 groupId（`com.codingas.gateway`）、artifactId 保持带前缀（`gateway-<域>`）、目录保持 Jmix 式短名（`provider/` ≠ artifactId）、**去掉域父 POM**（扁平聚合，Jmix 无父 POM 概念）、包名不变（`com.codingas.gateway.<域>` = groupId + 子域）。

**Architecture:** 2026-08-24 三轮决策收敛为最终形态——① 核心子模块**不去前缀**（artifactId 保持 `gateway-<域>`，Jmix `jmix-security` 模式）；② 统一 groupId `com.codingas.gateway`（Jmix 统一 `io.jmix` 模式，推翻 §4.4 按域 groupId）；③ **去掉 10 个域父 POM**（Jmix 无父 POM，用 Gradle settings 聚合；我们退回 Maven 扁平聚合——根 pom 直接列全部 45 模块，子模块 parent 回根 pom）；④ 目录短名不动（`provider/` 等，Jmix 式）；⑤ 包名不变。纯 pom 坐标重构：groupId 统一 + 删域父 POM + parent/依赖引用连锁调整，Java 代码/包名零改动。

**Tech Stack:** Maven 多模块（扁平聚合）、Java 21

## Global Constraints

- 全量 `./mvnw clean install` 每任务末尾必须绿（含测试）
- 每任务独立提交，commit message 中文
- 行为不变：包名/Java 代码/依赖方向不变，仅 Maven 坐标与聚合结构
- **groupId 统一**：所有模块（含 starter/插件/根工具）groupId → `com.codingas.gateway`（原按域 `com.codingas.gateway.<域>` 的全部改）
- **artifactId 不变**：核心 `gateway-provider` 等、data `gateway-provider-data` 等、starter `gateway-provider-starter` 等、协议插件 `gateway-protocol-openai/anthropic/gemini`、根工具 `gateway-common/boot/cli/simulator/web`、根 pom `gateway-project`——**全部保持现状**
- **删除 10 个域父 POM**：`gateway-<域>/pom.xml`（gateway-protocol/provider/iam/usage/security/audit/alert/resilience/proxy/stats 各一个）
- **根 pom `<modules>` 展开**：从 10 域 pom + 4 根模块 → 全部 45 个模块（域目录下子模块路径，如 `gateway-provider/provider`、`gateway-provider/provider-data`、`gateway-provider/provider-starter`）
- **29 个子模块 parent 回根 pom**：groupId `com.codingas.gateway`、artifactId `gateway-project`、version `${revision}`、relativePath `../../pom.xml`（域目录下子模块的根 pom 相对路径）
- **根目录模块**（gateway-common/boot/cli/simulator/web）parent 已指向根 pom——不变
- 包名（`com.codingas.gateway.provider` 等 Java 包）不变——与 groupId + 子域一致（Jmix 模式）
- 域独立构建替代：`./mvnw install -pl gateway-provider/provider,gateway-provider/provider-data,gateway-provider/provider-starter -am`（域父 POM 删除后无 `cd 进域构建`）
- 2 个遗留死 `*IT` 改名（T3）：boot 的 `ChannelProvisionTransactionalIT`、`ChannelHealthRepositoryIT` → `*IntegrationTest`
- 设计文档 §4.4 groupId 表 + §4.5 命名/域父 POM 决策重写（T3）

---

## Task 1: 基线验证

**Files:**
- 无

**Interfaces:**
- Consumes: 无
- Produces: 基线全绿

- [ ] **Step 1: 全量构建 + 测试**

```bash
cd /e/workspace/llm-gateway/.claude/worktrees/p45-naming
./mvnw clean install
```

Expected: BUILD SUCCESS，全部测试绿（基线 `1c07e8c3`——含 P4.5 旧计划提交，构建不受影响）。

- [ ] **Step 2: 确认无失败**

---

## Task 2: 统一 groupId + 去掉域父 POM（原子切换）

> **原子切换**：groupId 统一 ↔ 删域父 POM ↔ 子模块 parent 回根 ↔ 根 pom modules 展开 ↔ 依赖声明 groupId 必须同一任务完成——任一中断构建必失败（坐标解析不到）。最终 `./mvnw clean install` 全绿。

**Files:**
- Delete: 10 个域父 POM（`gateway-<域>/pom.xml`，含 gateway-protocol/provider/iam/usage/security/audit/alert/resilience/proxy/stats）
- Modify: 根 `pom.xml`（`<modules>` 展开为全部 45 模块）
- Modify: 29 个域内子模块 pom（parent 回根 + 自身 groupId 统一 + 依赖声明 groupId）
- Modify: 根目录模块 pom（gateway-common/boot/cli/simulator/web 的依赖声明 groupId）

**Interfaces:**
- Consumes: 无
- Produces: 全部模块统一 groupId `com.codingas.gateway`、无域父 POM、扁平聚合构建绿

- [ ] **Step 1: 删除 10 个域父 POM**

删除 `gateway-protocol/pom.xml`、`gateway-provider/pom.xml`、`gateway-iam/pom.xml`、`gateway-usage/pom.xml`、`gateway-security/pom.xml`、`gateway-audit/pom.xml`、`gateway-alert/pom.xml`、`gateway-resilience/pom.xml`、`gateway-proxy/pom.xml`、`gateway-stats/pom.xml`（域目录保留为纯目录容器，其子模块 pom 保留）。

- [ ] **Step 2: 29 个子模块 parent 回根 pom**

每个域内子模块 pom 的 `<parent>`（当前指向域父 POM）：

```xml
<!-- 改前（以 provider 核心为例） -->
<parent>
    <groupId>com.codingas.gateway.provider</groupId>
    <artifactId>gateway-provider-parent</artifactId>
    <version>${revision}</version>
    <relativePath>../pom.xml</relativePath>
</parent>

<!-- 改后 -->
<parent>
    <groupId>com.codingas.gateway</groupId>
    <artifactId>gateway-project</artifactId>
    <version>${revision}</version>
    <relativePath>../../pom.xml</relativePath>
</parent>
```

（29 个域内子模块同规则：groupId 改根、artifactId 改 `gateway-project`、relativePath 改 `../../pom.xml`。）

- [ ] **Step 3: 子模块自身 groupId 统一**

29 个域内子模块 + 根目录模块的自身 `<groupId>`（当前按域 `com.codingas.gateway.<域>`）→ `com.codingas.gateway`。**artifactId 一律不动**。

- [ ] **Step 4: 全部依赖声明 groupId 统一**

对所有 pom 的依赖声明做 groupId 替换：`<groupId>com.codingas.gateway.<域></groupId>` → `<groupId>com.codingas.gateway</groupId>`（provider/iam/usage/security/audit/alert/resilience/proxy/stats/protocol/common 各域 groupId）。用脚本批量替换。替换后验证：

```bash
grep -rn "com.codingas.gateway\." --include="pom.xml" . | grep -v target
```

Expected: 无输出（无 `com.codingas.gateway.<域>` 残留 groupId；`com.codingas.gateway` 是唯一 groupId）。

- [ ] **Step 5: 根 pom modules 展开**

根 `pom.xml` `<modules>` 从 10 域 pom + 4 根模块 → 全部 45 个模块路径：

```xml
<modules>
    <module>gateway-common</module>
    <module>gateway-protocol/protocol</module>
    <module>gateway-protocol/protocol-openai</module>
    <module>gateway-protocol/protocol-anthropic</module>
    <module>gateway-protocol/protocol-gemini</module>
    <module>gateway-provider/provider</module>
    <module>gateway-provider/provider-data</module>
    <module>gateway-provider/provider-starter</module>
    <!-- ... 其余 7 域同构（core/data/starter） + proxy/stats（core/starter） -->
    <module>gateway-boot</module>
    <module>gateway-web</module>
    <module>gateway-cli</module>
    <module>gateway-simulator</module>
</modules>
```

（以各域目录下实际子模块路径为准，域父 POM 条目删除。）

- [ ] **Step 6: 全量构建验证**

```bash
./mvnw clean install
```

Expected: BUILD SUCCESS（Maven 坐标全部解析；ArchUnit 7 条铁律按包名不受影响仍绿；全部测试绿）。若 `gateway-web` 或其他模块有指向域父 POM 的引用（不应有——无模块依赖父 POM 作 artifact），按报错清理。

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "build: Jmix 命名规范对齐——统一 groupId + 去掉域父 POM 扁平聚合（P4.5）
Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

## Task 3: 设计文档重写 + 死 IT 改名 + 全量回归

**Files:**
- Modify: `docs/superpowers/specs/2026-08-21-gateway-jmix-style-modularization-design.md`
- Modify: `gateway-boot/src/test/.../adapter/api/ChannelProvisionTransactionalIT.java`、`ChannelHealthRepositoryIT.java`（改名 `*IntegrationTest`）

**Interfaces:**
- Consumes: Task 2 全部产物
- Produces: 设计文档与实现一致（Jmix 对齐）；死测试拾取；回归绿

- [ ] **Step 1: 设计文档 §4.4 groupId 表重写**

§4.4 的「groupId 按域划分」表 → 重写为**统一 groupId**：

```markdown
**groupId（统一，仿 Jmix 统一 `io.jmix`）**：所有模块统一 `com.codingas.gateway`。包名仍按域（`com.codingas.gateway.<域>` = groupId + 子域，与 Jmix `io.jmix.security` 同模式）。
```

- [ ] **Step 2: 设计文档 §4.5 命名/域父 POM 决策重写**

- 删除「2026-08-24 决策（偏离原方案）：域目录增加中间父 POM」与「2026-08-24 决策（命名规范，待执行）」两条——均被以下最终决策取代
- 新增最终决策记录：

```markdown
> **2026-08-24 最终决策（Jmix 命名规范严格对齐）**：① 统一 groupId `com.codingas.gateway`（所有模块）；② artifactId 带 `gateway-` 前缀（核心 `gateway-<域>`、绑定 `gateway-<域>-data`、装配 `gateway-<域>-starter`，仿 Jmix `jmix-security`/`-data`/`-starter`）；③ 目录用 Jmix 式短名（`<域>/` ≠ artifactId）；④ **去掉域父 POM**（Jmix 无父 POM，Maven 扁平聚合——根 pom 直接聚合全部模块，子模块 parent 指向根 pom）；⑤ 包名 `com.codingas.gateway.<域>`（= groupId + 子域，不变）。原「按域 groupId」「核心去前缀」「域父 POM 层级聚合」决策全部废弃。
```

- 目录树域目录条目去掉「含域父 POM」标注（域目录为纯目录容器）
- 同步更新 CLAUDE.md 中相关的模块/groupId 描述（如有）

- [ ] **Step 3: 2 个死 *IT 改名**

`ChannelProvisionTransactionalIT.java` → `ChannelProvisionTransactionalIntegrationTest.java`；`ChannelHealthRepositoryIT.java` → `ChannelHealthRepositoryIntegrationTest.java`（类名/文件名同步，failsafe `*IntegrationTest` 拾取）。

- [ ] **Step 4: 全量回归**

```bash
./mvnw clean install
```

Expected: BUILD SUCCESS，全部测试绿（含新拾取的 2 个集成测试；ArchUnit 铁律仍绿）。

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "docs: Jmix 命名规范决策落地（统一 groupId + 去域父 POM）+ 死测试改名拾取（P4.5）
Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

## Self-Review 记录

**Spec 覆盖对照**：
- 统一 groupId（全部模块 com.codingas.gateway）→ Task 2 Step 3/4
- artifactId 保持带前缀（不动）→ Global Constraints
- 删除 10 个域父 POM → Task 2 Step 1
- 29 子模块 parent 回根 → Task 2 Step 2
- 根 pom modules 展开 45 模块 → Task 2 Step 5
- 目录短名不动 → Global Constraints
- 设计文档 §4.4/§4.5 重写 → Task 3 Step 1/2
- 死 IT 改名 → Task 3 Step 3

**Placeholder 扫描**：替换规则明确；无 TBD。

**Type/命名一致性**：
- artifactId 全保持（gateway-provider 等）✓
- groupId 统一 com.codingas.gateway ✓
- parent 引用（根 pom gateway-project）✓
- 包名 com.codingas.gateway.<域> 不变（= groupId + 子域，Jmix 模式）✓

**风险**：
- 原子切换中断 → Task 2 单任务完成
- groupId 替换遗漏 → Step 4 grep 验证
- 根 pom modules 展开遗漏 → Maven 报 "child module not found" 或模块不构建，按报错补齐
- 删除域父 POM 后无人依赖它（无模块把它当 artifact）——验证构建即可
