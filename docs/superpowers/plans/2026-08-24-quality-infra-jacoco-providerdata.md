# 质量基建：jacoco 全模块 + provider-data 补真测试 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** jacoco 覆盖率扩展到全部 Maven 模块并聚合报告（恢复覆盖率 DoD 可验证），补齐 `gateway-provider-data` 的 9 个 JPA GatewayImpl 单元测试（当前覆盖率 0）。

**Architecture:** 根 pom 的 `<build><plugins>` 声明 jacoco-maven-plugin（`prepare-agent` + `report` executions，从根 pluginManagement 取配置与 excludes），所有模块继承执行；根 pom 加 `report-aggregate` execution（phase `verify`）聚合全模块报告。`gateway-boot` 移除自身的 jacoco 声明（从根继承，避免 executions 重复）。`provider-data` 9 个 GatewayImpl 各补 mock Repository 单元测试（验证 CRUD 委托 + model↔DO 转换正确性）。

**Tech Stack:** Maven 多模块（扁平聚合）、JaCoCo 0.8.12、JUnit 5 + Mockito + AssertJ

## Global Constraints

- 全量 `./mvnw clean install` 每任务末尾必须绿（含测试）
- 每任务独立提交，commit message 中文
- 行为不变：只加测试与构建配置，不改业务逻辑
- **jacoco 根 pom 声明**（`<build><plugins>`，继承所有模块）：

  ```xml
  <plugin>
      <groupId>org.jacoco</groupId>
      <artifactId>jacoco-maven-plugin</artifactId>
      <executions>
          <execution>
              <goals>
                  <goal>prepare-agent</goal>
              </goals>
          </execution>
          <execution>
              <id>report</id>
              <phase>test</phase>
              <goals>
                  <goal>report</goal>
              </goals>
          </execution>
          <execution>
              <id>report-aggregate</id>
              <phase>verify</phase>
              <goals>
                  <goal>report-aggregate</goal>
              </goals>
          </execution>
      </executions>
  </plugin>
  ```

  （excludes 配置从根 pluginManagement 继承：entity/dataobject/dto/enums/exception/config 等。）
- **gateway-boot**：`<build><plugins>` 的 jacoco 声明删除（从根继承，避免 executions 合并重复）
- **provider-data 9 个 GatewayImpl**（`providerdata/gateway/`）：Provider、Model、ModelInstance、Channel、ChannelCredential、ChannelEndpoint、ChannelOperationLog、PlanCatalog、PlanModelCatalog 各一个 `XxxGatewayImplTest`
- **测试模式**（Q3）：mock Repository → 调 GatewayImpl 方法 → 验证 Repository 委托 + model↔DO 转换（字段映射正确）；覆盖 GatewayImpl 全部 public 方法（含 toDo/toEntity 转换路径与空值/缺省分支）
- 覆盖率 DoD（核心 ≥90% / 规则引擎 ≥85% / 适配器 ≥80%）作为**验证目标**（Q4 汇总报告确认，不设 jacoco check 门槛阻断——当前各模块覆盖率未知，先出报告再评估门槛）
- freeze 基线已由 P4 硬规则化取代，质量基建不含 freeze 入库

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

Expected: BUILD SUCCESS，全部测试绿（master `138fd949`；surefire 828 + failsafe 48）。

- [ ] **Step 2: 确认无失败**

---

## Task 2: jacoco 全模块 + report-aggregate

**Files:**
- Modify: 根 `pom.xml`（`<build><plugins>` 加 jacoco 声明，含 report-aggregate）
- Modify: `gateway-boot/pom.xml`（删 `<build><plugins>` 的 jacoco 声明）

**Interfaces:**
- Consumes: 无
- Produces: 全模块 jacoco 报告 + 根聚合报告

- [ ] **Step 1: 根 pom 加 jacoco build 声明**

根 `pom.xml` 的 `<build><plugins>` 按 Global Constraints 的代码添加 jacoco（prepare-agent + report + report-aggregate）。注意：excludes 从 pluginManagement 继承——若 pluginManagement 的 `<configuration>` 未被 build/plugins 继承（build/plugins 有独立 configuration 时不继承），确保 excludes 生效（验证报告是否排除 dataobject 等）。

- [ ] **Step 2: gateway-boot 删 jacoco 声明**

`gateway-boot/pom.xml` `<build><plugins>` 的 jacoco 块删除（从根继承）。

- [ ] **Step 3: 全量构建 + 验证报告**

```bash
./mvnw clean install
```

Expected: BUILD SUCCESS。然后验证：

```bash
# 各模块应有 jacoco 报告
find . -path "*/target/site/jacoco/jacoco.csv" -not -path "*worktrees*" | wc -l
# 根聚合报告
ls target/site/jacoco-aggregate/jacoco.csv 2>/dev/null
```

Expected: 模块报告数 ≥ 30（多数模块）；聚合报告存在。若个别模块无报告（如无测试类），记录说明。

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "build: jacoco 覆盖全模块 + report-aggregate 聚合报告（质量基建）
Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

## Task 3: provider-data 补真测试（9 个 GatewayImpl）

**Files:**
- Create（9 个测试）：`gateway-provider/provider-data/src/test/java/com/codingas/gateway/providerdata/gateway/`
  - `ProviderGatewayImplTest.java`、`ModelGatewayImplTest.java`、`ModelInstanceGatewayImplTest.java`、`ChannelGatewayImplTest.java`、`ChannelCredentialGatewayImplTest.java`、`ChannelEndpointGatewayImplTest.java`、`ChannelOperationLogGatewayImplTest.java`、`PlanCatalogGatewayImplTest.java`、`PlanModelCatalogGatewayImplTest.java`

**Interfaces:**
- Consumes: 现有 GatewayImpl（ProviderGatewayImpl 等）+ 领域模型（Provider/Model/Channel 等）+ DO（ProviderDo 等）+ Repository
- Produces: 9 个 GatewayImpl 的单元测试（mock Repository，覆盖全部 public 方法）

- [ ] **Step 1: 测试模式确认**

以 `ProviderGatewayImplTest` 为样板（brief 有完整代码），其余 8 个按同模式编写（方法清单从各 GatewayImpl 接口读取）。

`ProviderGatewayImplTest.java`（模式样板）：

```java
package com.codingas.gateway.providerdata.gateway;

import com.codingas.gateway.provider.vendor.Provider;
import com.codingas.gateway.providerdata.dataobject.ProviderDo;
import com.codingas.gateway.providerdata.repository.ProviderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ProviderGatewayImpl 单元测试：mock Repository 验证委托与 model↔DO 转换
 */
@ExtendWith(MockitoExtension.class)
class ProviderGatewayImplTest {

    @Mock
    private ProviderRepository providerRepository;

    @InjectMocks
    private ProviderGatewayImpl gateway;

    private Provider sampleProvider(Long id, String code, String name) {
        Provider p = new Provider();
        p.setId(id);
        p.setCode(code);
        p.setName(name);
        return p;
    }

    @Test
    void save_delegatesToRepositoryAndConvertsBack() {
        Provider provider = sampleProvider(1L, "openai", "OpenAI");
        ProviderDo savedDo = new ProviderDo();
        savedDo.setId(1L);
        when(providerRepository.save(any(ProviderDo.class))).thenAnswer(inv -> inv.getArgument(0));
        Provider result = gateway.save(provider);
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getCode()).isEqualTo("openai");
        verify(providerRepository).save(any(ProviderDo.class));
    }

    @Test
    void findById_returnsConvertedEntityWhenPresent() {
        ProviderDo doObj = new ProviderDo();
        doObj.setId(1L);
        doObj.setCode("openai");
        when(providerRepository.findById(1L)).thenReturn(Optional.of(doObj));
        Optional<Provider> result = gateway.findById(1L);
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1L);
        assertThat(result.get().getCode()).isEqualTo("openai");
    }

    @Test
    void findById_returnsEmptyWhenAbsent() {
        when(providerRepository.findById(99L)).thenReturn(Optional.empty());
        assertThat(gateway.findById(99L)).isEmpty();
    }

    // ... 其余方法（findByCode/findByName/findAll/findAllActive 等）按同模式补齐，覆盖全部 public 方法
}
```

**注意**：DO 的 setter 以实际 `XxxDo` 字段为准（@Data/手动 setter）；领域模型字段以实际 `Provider`/`Channel` 等为准（@DomainEntity 原型 Bean + setter）。若 DO/领域模型字段名与样板不同，以实际为准适配。转换验证必须覆盖「双向映射」（toDo 写字段 + toEntity 读字段）——即 save 验证写、findById 验证读。

- [ ] **Step 2: 编写 9 个测试**

每个 GatewayImpl 一个测试类，mock 对应 Repository，覆盖该 GatewayImpl 的**全部 public 方法**（含转换、空值、缺省分支）。DO/领域模型 setter 以实际为准。

- [ ] **Step 3: 运行 provider-data 测试**

```bash
./mvnw test -pl gateway-provider/provider-data
```

Expected: 全部 PASS（若 mock 的 Repository 方法签名与实现不符，按编译/运行错误修正测试）。

- [ ] **Step 4: 全量构建验证**

```bash
./mvnw clean install
```

Expected: BUILD SUCCESS，全部测试绿。

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "test: provider-data 9 个 JPA GatewayImpl 补 mock 单元测试（转换+委托全覆盖，质量基建）
Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

## Task 4: 覆盖率验证 + 全量回归

**Files:**
- 无（验证性）

**Interfaces:**
- Consumes: Task 2/3 产物
- Produces: 覆盖率汇总确认；回归绿

- [ ] **Step 1: 全量构建（含聚合报告）**

```bash
./mvnw clean install
```

Expected: BUILD SUCCESS（聚合报告在 `target/site/jacoco-aggregate/`）。

- [ ] **Step 2: 覆盖率汇总**

从聚合报告 `target/site/jacoco-aggregate/jacoco.csv` 统计各核心模块覆盖率（`com.codingas.gateway.<域>` 包，排除 dataobject/dto/enums/entity）：记录 provider/iam/usage/security/audit/alert/resilience/proxy/stats/protocol 的核心类行覆盖率。对照 DoD（核心 ≥90%）——若个别模块未达标，如实记录（不设 check 阻断，作为后续补测试依据）。

- [ ] **Step 3: 全量回归确认**

```bash
./mvnw clean install
```

Expected: BUILD SUCCESS，全部测试绿（surefire + failsafe 含 Q3 新增的 provider-data 测试）。

- [ ] **Step 4: Commit（如有）**

若 Q3 后有剩余未提交改动（如聚合报告生成物——target 通常 gitignored，无需提交），确认工作树干净；无改动则跳过提交。

---

## Self-Review 记录

**Spec 覆盖对照**：
- jacoco 全模块 + report-aggregate → Task 2
- provider-data 补真测试（9 个 GatewayImpl）→ Task 3
- 覆盖率验证（DoD 目标）→ Task 4
- freeze 入库不适用（P4 已硬规则化）→ Global Constraints

**Placeholder 扫描**：jacoco 配置完整；测试样板完整（ProviderGatewayImplTest 全代码）；其余 8 个测试按模式 + 实际字段适配（明确说明）。

**Type/命名一致性**：
- jacoco 根 pom 声明（prepare-agent/report/report-aggregate）与 pluginManagement excludes 继承 ✓
- 9 个测试类名与 GatewayImpl 一一对应 ✓
- 测试用 mock Repository + InjectMocks（与 CountPortTest 模式一致）✓

**风险**：
- 根 pom build/plugins jacoco 与 boot 现有声明重复 → Task 2 Step 2 删 boot 声明
- excludes 继承不生效 → Task 2 Step 1 验证
- DO/领域模型字段与样板不符 → Task 3 Step 1 注明以实际为准
- 覆盖率不达标 → Task 4 如实记录（不设 check 阻断）
