# 域级父 POM（层级聚合）实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 给每个域目录增加中间父 POM（层级聚合）：域 pom 聚合域内子模块，子模块 parent 指向域 pom，实现「cd 进域目录可独立构建该域」与域级模块组织。

**Architecture:** 10 个域目录（gateway-protocol/provider/iam/usage/security/audit/alert/resilience/proxy/stats）各建一个 `pom.xml`（域父 POM：parent=根 pom、groupId=域 groupId、artifactId=`gateway-<域>-parent`、packaging=pom、modules=域内子模块目录）；域内 30 个子模块的 parent 从根 pom 改为域 pom（relativePath `../../pom.xml` → `../pom.xml`）；根 pom 的 `<modules>` 从直接列 30 个子模块改为列 10 个域 pom + 4 个根目录模块（common/boot/cli/simulator）。`${revision}` 沿 parent chain（根→域→子模块）传播。**用户 2026-08-24 拍板**（偏离设计文档 §4.5 原「根 pom 直接聚合子模块」方案，需同步更新设计文档）。行为不变（构建结构重构，不改任何代码逻辑）。

**Tech Stack:** Maven 多模块（CI-friendly `${revision}` + flatten-maven-plugin）、Java 21

## Global Constraints

- 全量 `./mvnw clean install` 每任务末尾必须绿（含测试）
- 每任务独立提交，commit message 中文
- 行为不变：只改 pom 结构（parent/modules），不改任何 Java 代码、依赖坐标、插件配置
- **10 个域父 POM**（域 groupId + `gateway-<域>-parent`）：

  | 域目录 | 域 pom groupId | artifactId | modules（域内子模块目录） |
  |---|---|---|---|
  | gateway-protocol | `com.codingas.gateway.protocol` | `gateway-protocol-parent` | protocol、protocol-openai、protocol-anthropic、protocol-gemini |
  | gateway-provider | `com.codingas.gateway.provider` | `gateway-provider-parent` | provider、provider-data、provider-starter |
  | gateway-iam | `com.codingas.gateway.iam` | `gateway-iam-parent` | iam、iam-data、iam-starter |
  | gateway-usage | `com.codingas.gateway.usage` | `gateway-usage-parent` | usage、usage-data、usage-starter |
  | gateway-security | `com.codingas.gateway.security` | `gateway-security-parent` | security、security-data、security-starter |
  | gateway-audit | `com.codingas.gateway.audit` | `gateway-audit-parent` | audit、audit-data、audit-starter |
  | gateway-alert | `com.codingas.gateway.alert` | `gateway-alert-parent` | alert、alert-data、alert-starter |
  | gateway-resilience | `com.codingas.gateway.resilience` | `gateway-resilience-parent` | resilience、resilience-data、resilience-starter |
  | gateway-proxy | `com.codingas.gateway.proxy` | `gateway-proxy-parent` | proxy、proxy-starter |
  | gateway-stats | `com.codingas.gateway.stats` | `gateway-stats-parent` | stats、stats-starter |

- 域 pom：`parent` = 根 pom（groupId `com.codingas.gateway`、artifactId `gateway-project`、version `${revision}`、relativePath `../../pom.xml`）
- **30 个域内子模块** parent 改为：groupId=域 groupId、artifactId=`gateway-<域>-parent`、version `${revision}`、relativePath `../pom.xml`（改 parent 块，其余不动）
- **根 pom `<modules>` 改为**：10 个域 pom（`<module>gateway-provider</module>` 等）+ 4 个根目录模块（`gateway-common`、`gateway-boot`、`gateway-cli`、`gateway-simulator`）
- **gateway-common/boot/cli/simulator 不进任何域 pom**（底座/应用/工具模块保持根目录直接挂根 pom，设计文档 §4.5）
- 根 pom 的 groupId/artifactId（`com.codingas.gateway:gateway-project`）与 revision 定义不动
- `${revision}` 沿根→域→子模块 parent chain 解析（域 pom 不重复定义 revision）
- 设计文档 `docs/superpowers/specs/2026-08-21-gateway-jmix-style-modularization-design.md` §4.5 需同步更新（域 pom 决策记录）

---

## Task 1: 基线验证

**Files:**
- 无

**Interfaces:**
- Consumes: 无
- Produces: 基线全绿，回归基准

- [ ] **Step 1: 全量构建 + 测试**

```bash
cd /e/workspace/llm-gateway
./mvnw clean install
```

Expected: BUILD SUCCESS，全部测试绿（master `5b23dac0`）。

- [ ] **Step 2: 确认无失败**

若有失败先排查（systematic-debugging），不进入下一任务。

---

## Task 2: 10 个域父 POM 创建 + 30 个子模块 parent 调整

**Files:**
- Create（10 个域 pom）：
  - `gateway-protocol/pom.xml`、`gateway-provider/pom.xml`、`gateway-iam/pom.xml`、`gateway-usage/pom.xml`、`gateway-security/pom.xml`、`gateway-audit/pom.xml`、`gateway-alert/pom.xml`、`gateway-resilience/pom.xml`、`gateway-proxy/pom.xml`、`gateway-stats/pom.xml`
- Modify（30 个域内子模块 pom 的 `<parent>` 块）：
  - `gateway-protocol/protocol/pom.xml`、`gateway-protocol/protocol-openai/pom.xml`、`gateway-protocol/protocol-anthropic/pom.xml`、`gateway-protocol/protocol-gemini/pom.xml`
  - `gateway-provider/provider/pom.xml`、`gateway-provider/provider-data/pom.xml`、`gateway-provider/provider-starter/pom.xml`
  - `gateway-iam/iam/pom.xml`、`gateway-iam/iam-data/pom.xml`、`gateway-iam/iam-starter/pom.xml`
  - `gateway-usage/usage/pom.xml`、`gateway-usage/usage-data/pom.xml`、`gateway-usage/usage-starter/pom.xml`
  - `gateway-security/security/pom.xml`、`gateway-security/security-data/pom.xml`、`gateway-security/security-starter/pom.xml`
  - `gateway-audit/audit/pom.xml`、`gateway-audit/audit-data/pom.xml`、`gateway-audit/audit-starter/pom.xml`
  - `gateway-alert/alert/pom.xml`、`gateway-alert/alert-data/pom.xml`、`gateway-alert/alert-starter/pom.xml`
  - `gateway-resilience/resilience/pom.xml`、`gateway-resilience/resilience-data/pom.xml`、`gateway-resilience/resilience-starter/pom.xml`
  - `gateway-proxy/proxy/pom.xml`、`gateway-proxy/proxy-starter/pom.xml`
  - `gateway-stats/stats/pom.xml`、`gateway-stats/stats-starter/pom.xml`

**Interfaces:**
- Consumes: 无
- Produces: 10 个域 pom + 30 个子模块 parent 指向域 pom（根 pom modules 未变，构建仍绿）

- [ ] **Step 1: 创建 10 个域父 POM**

每域一个（以 provider 为例，其余按 Global Constraints 表替换 groupId/artifactId/modules/name/description）：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.codingas.gateway</groupId>
        <artifactId>gateway-project</artifactId>
        <version>${revision}</version>
        <relativePath>../../pom.xml</relativePath>
    </parent>

    <groupId>com.codingas.gateway.provider</groupId>
    <artifactId>gateway-provider-parent</artifactId>
    <packaging>pom</packaging>
    <name>Gateway Provider Parent</name>
    <description>Provider 域父 POM：聚合核心/绑定/装配子模块，支持域内独立构建</description>

    <modules>
        <module>provider</module>
        <module>provider-data</module>
        <module>provider-starter</module>
    </modules>
</project>
```

各域 modules 列表见 Global Constraints 表（protocol 域 4 个、provider/iam/usage/security/audit/alert/resilience 各 3 个、proxy/stats 各 2 个）。

- [ ] **Step 2: 调整 30 个子模块的 parent 块**

每个域内子模块 pom 的 `<parent>` 块（当前为根 pom + relativePath `../../pom.xml`）改为域 pom。以 `gateway-provider/provider/pom.xml` 为例：

```xml
<!-- 修改前 -->
<parent>
    <groupId>com.codingas.gateway</groupId>
    <artifactId>gateway-project</artifactId>
    <version>${revision}</version>
    <relativePath>../../pom.xml</relativePath>
</parent>

<!-- 修改后 -->
<parent>
    <groupId>com.codingas.gateway.provider</groupId>
    <artifactId>gateway-provider-parent</artifactId>
    <version>${revision}</version>
    <relativePath>../pom.xml</relativePath>
</parent>
```

其余 29 个子模块同规则（groupId/artifactId 按所属域，relativePath 一律 `../pom.xml`）。**只改 parent 块**，其余 `<dependencies>`/`<build>` 等一律不动。

- [ ] **Step 3: 全量构建验证（根 modules 未切，子模块仍被根聚合）**

```bash
./mvnw clean install
```

Expected: BUILD SUCCESS（根 pom modules 仍列 30 个子模块路径，子模块通过域 pom 解析 parent——域 pom 作为 parent 文件被 Maven 读取即可，不需在根 modules 中）。若某子模块 parent 修改遗漏，Maven 会报 "Non-resolvable parent POM"，按报错修正。

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "build: 10 个域父 POM + 30 子模块 parent 指向域 pom（层级聚合，P2 结构优化）
Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

## Task 3: 根 pom modules 切换 + 设计文档 §4.5 更新 + 域独立构建验证 + 全量回归

**Files:**
- Modify: `pom.xml`（`<modules>` 改为 10 个域 pom + 4 个根目录模块）
- Modify: `docs/superpowers/specs/2026-08-21-gateway-jmix-style-modularization-design.md`（§4.5「Maven 影响」段 + 目录树标注）

**Interfaces:**
- Consumes: Task 2 的 10 个域 pom
- Produces: 根 reactor 为 10 域 + 4 根模块；设计文档记录决策；域独立构建可行

- [ ] **Step 1: 根 pom modules 切换**

`pom.xml` 的 `<modules>` 替换为：

```xml
<modules>
    <module>gateway-common</module>
    <module>gateway-protocol</module>
    <module>gateway-provider</module>
    <module>gateway-iam</module>
    <module>gateway-usage</module>
    <module>gateway-security</module>
    <module>gateway-audit</module>
    <module>gateway-alert</module>
    <module>gateway-resilience</module>
    <module>gateway-proxy</module>
    <module>gateway-stats</module>
    <module>gateway-boot</module>
    <module>gateway-cli</module>
    <module>gateway-simulator</module>
</modules>
```

（去掉原 30 个子模块路径条目，替换为 10 个域目录名；gateway-common/boot/cli/simulator 保持原样。）

- [ ] **Step 2: 全量构建验证**

```bash
./mvnw clean install
```

Expected: BUILD SUCCESS（reactor 现在为 10 域 pom + 4 根模块；域 pom 递归聚合域内子模块；`${revision}`/flatten 沿新 parent chain 正常）。若 flatten-maven-plugin 或 `${revision}` 解析异常，定位并修复（域 pom 不定义 revision，沿 parent chain 从根解析；flatten 对中间层级 pom 正常生成 .flattened-pom.xml）。

- [ ] **Step 3: 域独立构建验证（方案 B 收益点）**

```bash
# 从根先确保本地仓库有跨域依赖（Step 2 已全量安装）
cd gateway-provider
../mvnw install
cd ..
```

Expected: BUILD SUCCESS（域 pom 作为 reactor 入口，构建 provider/consumer-data/provider-starter 三模块；跨域依赖 common/protocol 从本地仓库解析）。同理可抽查 `gateway-protocol`（4 模块）与 `gateway-proxy`（2 模块）。

- [ ] **Step 4: 设计文档 §4.5 更新**

`docs/superpowers/specs/2026-08-21-gateway-jmix-style-modularization-design.md`：
- §4.5「**Maven 影响**」段末尾追加决策记录：

```markdown
> **2026-08-24 决策（偏离原方案）**：域目录增加中间父 POM（层级聚合）——每个域目录一个 `pom.xml`（`gateway-<域>-parent`，parent=根 pom，packaging=pom，聚合域内子模块），子模块 parent 指向域 pom（`relativePath=../pom.xml`），根 pom `<modules>` 只聚合 10 个域 pom + 4 个根目录模块（common/boot/cli/simulator）。收益：cd 进域目录可独立构建该域；域级模块组织语义化。成本：pom 层级 +1、14 个新 pom 文件。原「根 pom 直接聚合子模块、relativePath=../../pom.xml」方案废弃。
```

- 目录树中域目录条目（如 `gateway-provider/`）旁加注 `（含域父 POM）`：

```
gateway-provider/                 # 供给域目录（含域父 POM，聚合 provider/provider-data/provider-starter）
```

- [ ] **Step 5: 全量回归**

```bash
./mvnw clean install
```

Expected: BUILD SUCCESS，全部测试绿。

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "build: 根 pom 聚合切换为 10 域 pom + 4 根模块，域独立构建验证 + 设计文档决策记录
Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

## Self-Review 记录

**Spec 覆盖对照**：
- 10 个域父 POM（层级聚合，域 pom 聚合域内子模块）→ Task 2 Step 1
- 30 个子模块 parent 指向域 pom → Task 2 Step 2
- 根 pom modules 切换（10 域 + 4 根模块）→ Task 3 Step 1
- 域独立构建验证（cd 进域目录）→ Task 3 Step 3
- 设计文档 §4.5 决策记录 → Task 3 Step 4
- gateway-common/boot/cli/simulator 保持根目录 → Global Constraints + Task 3 Step 1

**Placeholder 扫描**：域 pom 模板完整；30 子模块为同规则替换（已给 before/after 示例 + 规则说明）；无 TBD。

**Type/命名一致性**：
- 域 pom artifactId `gateway-<域>-parent` 与子模块 artifactId（`gateway-provider` 等）不冲突（不同坐标）✓
- 域 pom groupId=域 groupId，子模块 groupId 已按域（§4.4）✓
- parent chain：根（`com.codingas.gateway:gateway-project`）→ 域（`com.codingas.gateway.<域>:gateway-<域>-parent`）→ 子模块 ✓
- `${revision}` 由根定义（`pom.xml:84`），沿 parent chain 传播，域 pom 不重复定义 ✓

**风险**：
- flatten-maven-plugin 与中间层级兼容性 → Task 3 Step 2 验证
- `${revision}` 沿新链解析 → Task 2 Step 3 / Task 3 Step 2 验证
- 子模块 parent 修改遗漏 → Maven "Non-resolvable parent POM" 报错暴露（Task 2 Step 3 处理路径已写）
- gateway-boot/common 等根目录模块 parent 无 relativePath（默认 ../pom.xml）——保持不动 ✓
