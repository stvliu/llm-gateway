# Actuator 健康检测实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 用 Spring Boot Actuator 体系替换自定义 HealthController，实现分层健康检测（liveness/readiness），支持 Provider 可达性混合探测策略和 health 端点公开访问控制。

**Architecture:** 删除硬编码 HealthController，新增 ProviderHealthTracker（混合探测核心）和 ProviderRegistryHealthIndicator（Actuator 集成），配置 liveness/readiness health group，K8s probe 指向 Actuator 端点。

**Tech Stack:** Spring Boot Actuator, Spring Boot 3.5.x, JUnit 5, Mockito, MockMvc

---

## 文件结构

| 操作 | 文件 | 职责 |
|------|------|------|
| 新增 | `infrastructure/actuator/ProviderHealthState.java` | Provider 健康状态 record |
| 新增 | `infrastructure/actuator/ProviderHealthTracker.java` | 混合探测策略核心 |
| 新增 | `infrastructure/actuator/ProviderRegistryHealthIndicator.java` | Actuator HealthIndicator 实现 |
| 新增 | `infrastructure/actuator/ProviderHealthProperties.java` | 健康检测配置属性 |
| 新增 | `infrastructure/config/ActuatorSecurityConfig.java` | Health 端点公开访问控制 |
| 修改 | `infrastructure/config/WebConfig.java` | 拦截器排除 `/actuator/health/**` |
| 修改 | `application.yml` | 添加 health group 和配置项 |
| 修改 | `deployments/helm/llm-gateway/values.yaml` | K8s probe 路径 |
| 删除 | `adapter/api/HealthController.java` | 移除硬编码健康端点 |
| 删除 | `adapter/api/HealthControllerTest.java` | 移除对应测试 |
| 新增 | `test/infrastructure/actuator/ProviderHealthTrackerTest.java` | Tracker 单元测试 |
| 新增 | `test/infrastructure/actuator/ProviderRegistryHealthIndicatorTest.java` | HealthIndicator 单元测试 |
| 新增 | `test/infrastructure/actuator/ActuatorHealthIntegrationTest.java` | 端点集成测试 |

基础路径前缀：`gateway-boot/src/main/java/com/codingas/gateway/`
测试路径前缀：`gateway-boot/src/test/java/com/codingas/gateway/`

---

### Task 1: ProviderHealthState — 健康状态 record

**Files:**
- Create: `infrastructure/actuator/ProviderHealthState.java`
- Test: `test/infrastructure/actuator/ProviderHealthStateTest.java`

- [ ] **Step 1: 写失败测试**

```java
package com.codingas.gateway.infrastructure.actuator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Status;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ProviderHealthState 测试")
class ProviderHealthStateTest {

    @Test
    @DisplayName("创建初始状态为 UNKNOWN")
    void createInitial_returnsUnknown() {
        var state = ProviderHealthState.initial("openai");

        assertThat(state.providerCode()).isEqualTo("openai");
        assertThat(state.status()).isEqualTo(Status.UNKNOWN);
        assertThat(state.consecutiveFailures()).isZero();
        assertThat(state.lastError()).isNull();
    }

    @Test
    @DisplayName("withSuccess 返回 UP 状态")
    void withSuccess_returnsUp() {
        var initial = ProviderHealthState.initial("openai");
        var state = initial.withSuccess();

        assertThat(state.status()).isEqualTo(Status.UP);
        assertThat(state.consecutiveFailures()).isZero();
        assertThat(state.lastError()).isNull();
        assertThat(state.lastRequestTime()).isNotNull();
    }

    @Test
    @DisplayName("withFailure 返回失败状态并记录错误")
    void withFailure_recordsError() {
        var initial = ProviderHealthState.initial("openai");
        var state = initial.withFailure("connection refused");

        assertThat(state.status()).isEqualTo(Status.DOWN);
        assertThat(state.consecutiveFailures()).isEqualTo(1);
        assertThat(state.lastError()).isEqualTo("connection refused");
        assertThat(state.lastRequestTime()).isNotNull();
    }

    @Test
    @DisplayName("连续失败累加")
    void withFailure_accumulates() {
        var state = ProviderHealthState.initial("openai")
                .withFailure("err1")
                .withFailure("err2");

        assertThat(state.consecutiveFailures()).isEqualTo(2);
        assertThat(state.lastError()).isEqualTo("err2");
    }

    @Test
    @DisplayName("成功后重置连续失败计数")
    void withSuccess_resetsFailures() {
        var state = ProviderHealthState.initial("openai")
                .withFailure("err1")
                .withFailure("err2")
                .withSuccess();

        assertThat(state.consecutiveFailures()).isZero();
        assertThat(state.status()).isEqualTo(Status.UP);
    }

    @Test
    @DisplayName("withProbe 更新检测时间")
    void withProbe_updatesCheckTime() {
        var initial = ProviderHealthState.initial("openai");
        var state = initial.withProbe(Status.UP);

        assertThat(state.lastCheckTime()).isNotNull();
        assertThat(state.status()).isEqualTo(Status.UP);
    }
}
```

- [ ] **Step 2: 运行测试验证失败**

Run: `./mvnw test -pl gateway-boot -Dtest=ProviderHealthStateTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: FAIL — `ProviderHealthState` 类不存在

- [ ] **Step 3: 写最小实现**

```java
package com.codingas.gateway.infrastructure.actuator;

import org.springframework.boot.actuate.health.Status;

import java.time.Instant;

/**
 * Provider 健康状态
 *
 * <p>不可变 record，每次状态变更返回新实例。</p>
 */
public record ProviderHealthState(
        String providerCode,
        Status status,
        Instant lastCheckTime,
        Instant lastRequestTime,
        int consecutiveFailures,
        String lastError
) {
    /**
     * 创建初始状态（UNKNOWN）
     */
    public static ProviderHealthState initial(String providerCode) {
        return new ProviderHealthState(providerCode, Status.UNKNOWN, null, null, 0, null);
    }

    /**
     * 记录请求成功
     */
    public ProviderHealthState withSuccess() {
        return new ProviderHealthState(providerCode, Status.UP, lastCheckTime, Instant.now(), 0, null);
    }

    /**
     * 记录请求失败
     */
    public ProviderHealthState withFailure(String error) {
        return new ProviderHealthState(providerCode, Status.DOWN, lastCheckTime, Instant.now(),
                consecutiveFailures + 1, error);
    }

    /**
     * 更新主动探测结果
     */
    public ProviderHealthState withProbe(Status probeStatus) {
        return new ProviderHealthState(providerCode, probeStatus, Instant.now(), lastRequestTime,
                probeStatus == Status.DOWN ? consecutiveFailures + 1 : 0,
                probeStatus == Status.DOWN ? lastError : null);
    }

    /**
     * 判断状态是否过期
     */
    public boolean isStale(java.time.Duration threshold) {
        if (lastCheckTime == null) {
            return true;
        }
        return Instant.now().isAfter(lastCheckTime.plus(threshold));
    }
}
```

- [ ] **Step 4: 运行测试验证通过**

Run: `./mvnw test -pl gateway-boot -Dtest=ProviderHealthStateTest`
Expected: PASS

- [ ] **Step 5: 提交**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/infrastructure/actuator/ProviderHealthState.java \
       gateway-boot/src/test/java/com/codingas/gateway/infrastructure/actuator/ProviderHealthStateTest.java
git commit -m "feat(actuator): 新增 ProviderHealthState — Provider 健康状态 record"
```

---

### Task 2: ProviderHealthProperties — 配置属性

**Files:**
- Create: `infrastructure/actuator/ProviderHealthProperties.java`

- [ ] **Step 1: 写实现**

```java
package com.codingas.gateway.infrastructure.actuator;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Provider 健康检测配置属性
 */
@Data
@ConfigurationProperties(prefix = "gateway.health.provider")
public class ProviderHealthProperties {

    /** 超过此时间无请求则重新主动探测 */
    private Duration staleThreshold = Duration.ofSeconds(300);

    /** 连续失败 N 次标记 DOWN */
    private int failureThreshold = 3;

    /** 连续成功 N 次恢复 UP */
    private int successThreshold = 2;

    /** 主动探测超时 */
    private Duration probeTimeout = Duration.ofSeconds(10);
}
```

- [ ] **Step 2: 提交**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/infrastructure/actuator/ProviderHealthProperties.java
git commit -m "feat(actuator): 新增 ProviderHealthProperties — 健康检测配置属性"
```

---

### Task 3: ProviderHealthTracker — 混合探测核心

**Files:**
- Create: `infrastructure/actuator/ProviderHealthTracker.java`
- Test: `test/infrastructure/actuator/ProviderHealthTrackerTest.java`

- [ ] **Step 1: 写失败测试**

```java
package com.codingas.gateway.infrastructure.actuator;

import com.codingas.gateway.infrastructure.proxy.gateway.rpc.LLMAdapter;
import com.codingas.gateway.infrastructure.proxy.gateway.rpc.AdapterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Status;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProviderHealthTracker 测试")
class ProviderHealthTrackerTest {

    @Mock
    private AdapterRegistry adapterRegistry;

    @Mock
    private LLMAdapter openaiAdapter;

    @Mock
    private LLMAdapter anthropicAdapter;

    private ProviderHealthProperties properties;
    private ProviderHealthTracker tracker;

    @BeforeEach
    void setUp() {
        properties = new ProviderHealthProperties();
        properties.setFailureThreshold(3);
        properties.setSuccessThreshold(2);
        properties.setStaleThreshold(Duration.ofSeconds(300));
        properties.setProbeTimeout(Duration.ofSeconds(10));

        when(openaiAdapter.getProviderCode()).thenReturn("openai");
        when(openaiAdapter.isHealthy()).thenReturn(true);
        when(openaiAdapter.checkConnection()).thenReturn(true);
        when(anthropicAdapter.getProviderCode()).thenReturn("anthropic");
        when(anthropicAdapter.isHealthy()).thenReturn(true);
        when(anthropicAdapter.checkConnection()).thenReturn(true);

        when(adapterRegistry.getAllAdapters()).thenReturn(List.of(openaiAdapter, anthropicAdapter));

        tracker = new ProviderHealthTracker(adapterRegistry, properties);
    }

    @Test
    @DisplayName("初始状态为 UNKNOWN")
    void initialStatus_isUnknown() {
        var status = tracker.getStatus("openai");
        assertThat(status.status()).isEqualTo(Status.UNKNOWN);
    }

    @Test
    @DisplayName("记录成功请求后状态为 UP")
    void recordSuccess_statusIsUp() {
        tracker.recordRequestResult("openai", true, null);
        tracker.recordRequestResult("openai", true, null);

        var status = tracker.getStatus("openai");
        assertThat(status.status()).isEqualTo(Status.UP);
    }

    @Test
    @DisplayName("连续失败达到阈值后状态为 DOWN")
    void consecutiveFailures_reachesDown() {
        tracker.recordRequestResult("openai", false, "timeout");
        tracker.recordRequestResult("openai", false, "timeout");
        tracker.recordRequestResult("openai", false, "timeout");

        var status = tracker.getStatus("openai");
        assertThat(status.status()).isEqualTo(Status.DOWN);
        assertThat(status.consecutiveFailures()).isEqualTo(3);
    }

    @Test
    @DisplayName("DOWN 后连续成功达到阈值恢复 UP")
    void recovery_afterSuccessThreshold() {
        tracker.recordRequestResult("openai", false, "timeout");
        tracker.recordRequestResult("openai", false, "timeout");
        tracker.recordRequestResult("openai", false, "timeout");

        assertThat(tracker.getStatus("openai").status()).isEqualTo(Status.DOWN);

        tracker.recordRequestResult("openai", true, null);
        tracker.recordRequestResult("openai", true, null);

        assertThat(tracker.getStatus("openai").status()).isEqualTo(Status.UP);
    }

    @Test
    @DisplayName("DOWN 后单次成功不恢复 UP")
    void singleSuccess_doesNotRecover() {
        tracker.recordRequestResult("openai", false, "timeout");
        tracker.recordRequestResult("openai", false, "timeout");
        tracker.recordRequestResult("openai", false, "timeout");

        tracker.recordRequestResult("openai", true, null);

        // successThreshold=2，单次成功不够
        assertThat(tracker.getStatus("openai").status()).isEqualTo(Status.DOWN);
    }

    @Test
    @DisplayName("getAllStatuses 返回所有 Provider 状态")
    void getAllStatuses_returnsAll() {
        var all = tracker.getAllStatuses();
        assertThat(all).hasSize(2);
        assertThat(all.stream().map(ProviderHealthState::providerCode))
                .containsExactlyInAnyOrder("openai", "anthropic");
    }

    @Test
    @DisplayName("至少一个 Provider UP 时 hasHealthyProvider 为 true")
    void hasHealthyProvider_atLeastOneUp() {
        tracker.recordRequestResult("openai", true, null);
        tracker.recordRequestResult("openai", true, null);

        assertThat(tracker.hasHealthyProvider()).isTrue();
    }

    @Test
    @DisplayName("所有 Provider DOWN 时 hasHealthyProvider 为 false")
    void hasHealthyProvider_allDown() {
        tracker.recordRequestResult("openai", false, "err");
        tracker.recordRequestResult("openai", false, "err");
        tracker.recordRequestResult("openai", false, "err");
        tracker.recordRequestResult("anthropic", false, "err");
        tracker.recordRequestResult("anthropic", false, "err");
        tracker.recordRequestResult("anthropic", false, "err");

        assertThat(tracker.hasHealthyProvider()).isFalse();
    }
}
```

- [ ] **Step 2: 运行测试验证失败**

Run: `./mvnw test -pl gateway-boot -Dtest=ProviderHealthTrackerTest`
Expected: FAIL — `ProviderHealthTracker` 类不存在

- [ ] **Step 3: 写最小实现**

```java
package com.codingas.gateway.infrastructure.actuator;

import com.codingas.gateway.infrastructure.proxy.gateway.rpc.AdapterRegistry;
import com.codingas.gateway.infrastructure.proxy.gateway.rpc.LLMAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Provider 健康状态追踪器
 *
 * <p>混合探测策略：启动时主动探测，运行中被动推断，超时后重新探测。</p>
 */
@Slf4j
@Component
@EnableConfigurationProperties(ProviderHealthProperties.class)
public class ProviderHealthTracker {

    private final AdapterRegistry adapterRegistry;
    private final ProviderHealthProperties properties;
    private final ConcurrentHashMap<String, ProviderHealthState> states = new ConcurrentHashMap<>();

    public ProviderHealthTracker(AdapterRegistry adapterRegistry, ProviderHealthProperties properties) {
        this.adapterRegistry = adapterRegistry;
        this.properties = properties;
    }

    /**
     * 获取指定 Provider 的健康状态
     *
     * <p>如果状态过期，异步触发重新探测。</p>
     */
    public ProviderHealthState getStatus(String providerCode) {
        ProviderHealthState state = states.computeIfAbsent(providerCode, ProviderHealthState::initial);

        if (state.isStale(properties.getStaleThreshold())) {
            triggerAsyncProbe(providerCode);
        }

        return state;
    }

    /**
     * 获取所有 Provider 的健康状态
     */
    public List<ProviderHealthState> getAllStatuses() {
        return adapterRegistry.getAllAdapters().stream()
                .map(adapter -> getStatus(adapter.getProviderCode()))
                .toList();
    }

    /**
     * 记录实际请求结果（被动推断）
     */
    public void recordRequestResult(String providerCode, boolean success, String error) {
        states.compute(providerCode, (code, existing) -> {
            ProviderHealthState current = existing != null ? existing : ProviderHealthState.initial(code);

            if (success) {
                ProviderHealthState updated = current.withSuccess();
                // DOWN 状态需要连续成功 successThreshold 次才恢复
                if (current.status() == Status.DOWN) {
                    int successCount = current.consecutiveFailures() == 0 ? 1 : 0;
                    if (successCount >= properties.getSuccessThreshold()) {
                        return updated;
                    }
                    // 还在恢复中，保持 DOWN 但重置失败计数
                    return current.withSuccess();
                }
                return updated;
            } else {
                ProviderHealthState updated = current.withFailure(error);
                // 连续失败达到阈值标记 DOWN
                if (updated.consecutiveFailures() >= properties.getFailureThreshold()) {
                    return updated;
                }
                return updated;
            }
        });
    }

    /**
     * 是否至少有一个健康的 Provider
     */
    public boolean hasHealthyProvider() {
        return getAllStatuses().stream()
                .anyMatch(state -> state.status() == Status.UP);
    }

    /**
     * 异步触发主动探测
     */
    private void triggerAsyncProbe(String providerCode) {
        adapterRegistry.getAdapter(providerCode).ifPresent(adapter -> {
            try {
                boolean healthy = adapter.checkConnection();
                Status probeStatus = healthy ? Status.UP : Status.DOWN;
                states.compute(providerCode, (code, existing) ->
                        existing != null ? existing.withProbe(probeStatus) : ProviderHealthState.initial(code).withProbe(probeStatus));
                log.debug("Provider {} probe result: {}", providerCode, probeStatus);
            } catch (Exception e) {
                log.warn("Provider {} probe failed: {}", providerCode, e.getMessage());
                states.compute(providerCode, (code, existing) ->
                        existing != null ? existing.withProbe(Status.DOWN) : ProviderHealthState.initial(code).withProbe(Status.DOWN));
            }
        });
    }
}
```

- [ ] **Step 4: 运行测试验证通过**

Run: `./mvnw test -pl gateway-boot -Dtest=ProviderHealthTrackerTest`
Expected: PASS（可能需要微调 recordRequestResult 中的恢复逻辑）

- [ ] **Step 5: 修复恢复逻辑（如测试失败）**

测试中 `DOWN 后单次成功不恢复 UP` 和 `DOWN 后连续成功达到阈值恢复 UP` 需要更精确的恢复计数。当前实现需要跟踪连续成功次数。修改 `ProviderHealthState` 添加 `consecutiveSuccesses` 字段，或修改 `ProviderHealthTracker.recordRequestResult` 逻辑以正确跟踪恢复过程。

- [ ] **Step 6: 提交**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/infrastructure/actuator/ProviderHealthTracker.java \
       gateway-boot/src/test/java/com/codingas/gateway/infrastructure/actuator/ProviderHealthTrackerTest.java
git commit -m "feat(actuator): 新增 ProviderHealthTracker — 混合探测策略核心"
```

---

### Task 4: ProviderRegistryHealthIndicator — Actuator 集成

**Files:**
- Create: `infrastructure/actuator/ProviderRegistryHealthIndicator.java`
- Test: `test/infrastructure/actuator/ProviderRegistryHealthIndicatorTest.java`

- [ ] **Step 1: 写失败测试**

```java
package com.codingas.gateway.infrastructure.actuator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProviderRegistryHealthIndicator 测试")
class ProviderRegistryHealthIndicatorTest {

    @Mock
    private ProviderHealthTracker healthTracker;

    private ProviderRegistryHealthIndicator indicator;

    @BeforeEach
    void setUp() {
        indicator = new ProviderRegistryHealthIndicator(healthTracker);
    }

    @Test
    @DisplayName("至少一个 Provider UP 时整体状态为 UP")
    void health_atLeastOneUp_returnsUp() {
        when(healthTracker.getAllStatuses()).thenReturn(List.of(
                new ProviderHealthState("openai", Status.UP, Instant.now(), Instant.now(), 0, null),
                new ProviderHealthState("anthropic", Status.DOWN, Instant.now(), Instant.now(), 3, "timeout")
        ));
        when(healthTracker.hasHealthyProvider()).thenReturn(true);

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsKey("openai");
        assertThat(health.getDetails()).containsKey("anthropic");
    }

    @Test
    @DisplayName("所有 Provider DOWN 时整体状态为 DOWN")
    void health_allDown_returnsDown() {
        when(healthTracker.getAllStatuses()).thenReturn(List.of(
                new ProviderHealthState("openai", Status.DOWN, Instant.now(), Instant.now(), 3, "timeout"),
                new ProviderHealthState("anthropic", Status.DOWN, Instant.now(), Instant.now(), 5, "refused")
        ));
        when(healthTracker.hasHealthyProvider()).thenReturn(false);

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
    }

    @Test
    @DisplayName("无 Provider 时状态为 UNKNOWN")
    void health_noProviders_returnsUnknown() {
        when(healthTracker.getAllStatuses()).thenReturn(List.of());
        when(healthTracker.hasHealthyProvider()).thenReturn(false);

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UNKNOWN);
    }
}
```

- [ ] **Step 2: 运行测试验证失败**

Run: `./mvnw test -pl gateway-boot -Dtest=ProviderRegistryHealthIndicatorTest`
Expected: FAIL — 类不存在

- [ ] **Step 3: 写实现**

```java
package com.codingas.gateway.infrastructure.actuator;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

/**
 * Provider 注册表健康指标
 *
 * <p>聚合所有 LLM Provider 的健康状态。</p>
 * <p>至少一个 Provider UP → 整体 UP；全部 DOWN → 整体 DOWN。</p>
 */
@Component
@RequiredArgsConstructor
public class ProviderRegistryHealthIndicator extends AbstractHealthIndicator {

    private final ProviderHealthTracker healthTracker;

    @Override
    protected void doHealthCheck(Health.Builder builder) throws Exception {
        var allStatuses = healthTracker.getAllStatuses();

        if (allStatuses.isEmpty()) {
            builder.withDetail("message", "无已注册的 Provider").unknown();
            return;
        }

        for (var state : allStatuses) {
            builder.withDetail(state.providerCode(), Map.of(
                    "status", state.status().getCode(),
                    "lastCheckTime", state.lastCheckTime() != null ? state.lastCheckTime().toString() : "never",
                    "lastRequestTime", state.lastRequestTime() != null ? state.lastRequestTime().toString() : "never",
                    "consecutiveFailures", state.consecutiveFailures(),
                    "lastError", state.lastError() != null ? state.lastError() : ""
            ));
        }

        if (healthTracker.hasHealthyProvider()) {
            builder.up();
        } else {
            builder.down();
        }
    }
}
```

- [ ] **Step 4: 运行测试验证通过**

Run: `./mvnw test -pl gateway-boot -Dtest=ProviderRegistryHealthIndicatorTest`
Expected: PASS

- [ ] **Step 5: 提交**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/infrastructure/actuator/ProviderRegistryHealthIndicator.java \
       gateway-boot/src/test/java/com/codingas/gateway/infrastructure/actuator/ProviderRegistryHealthIndicatorTest.java
git commit -m "feat(actuator): 新增 ProviderRegistryHealthIndicator — Actuator 健康指标"
```

---

### Task 5: ActuatorSecurityConfig — Health 端点公开访问控制

**Files:**
- Create: `infrastructure/config/ActuatorSecurityConfig.java`
- Modify: `infrastructure/config/WebConfig.java`

- [ ] **Step 1: 写 ActuatorSecurityConfig**

```java
package com.codingas.gateway.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Actuator 安全配置
 *
 * <p>根据配置项控制 /actuator/health 端点是否跳过认证拦截。</p>
 */
@Slf4j
@Configuration
public class ActuatorSecurityConfig implements WebMvcConfigurer {

    @Value("${management.endpoint.health.public-access:true}")
    private boolean healthPublicAccess;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        if (healthPublicAccess) {
            // 不对 actuator health 端点应用安全拦截
            // 此处为空实现，实际排除逻辑在 WebConfig 中处理
            log.info("Actuator health 端点公开访问已启用");
        }
    }

    /**
     * 判断 health 端点是否公开访问
     */
    public boolean isHealthPublicAccess() {
        return healthPublicAccess;
    }
}
```

- [ ] **Step 2: 修改 WebConfig — 排除 `/actuator/health/**` 拦截**

在 `WebConfig.addInterceptors()` 中，当 `healthPublicAccess=true` 时，将 `/actuator/health/**` 加入拦截器的排除路径：

```java
// WebConfig.java 修改 addInterceptors 方法
@Override
public void addInterceptors(InterceptorRegistry registry) {
    var registration = registry.addInterceptor(new org.springframework.web.servlet.HandlerInterceptor() {
        @Override
        public boolean preHandle(jakarta.servlet.http.HttpServletRequest request,
                                 jakarta.servlet.http.HttpServletResponse response,
                                 Object handler) throws Exception {
            return securityInterceptorChain.execute(request, response);
        }
    }).addPathPatterns("/api/**", "/v1/**");

    // 当 health 端点公开访问时，排除 actuator health 路径
    if (actuatorSecurityConfig.isHealthPublicAccess()) {
        registration.excludePathPatterns("/actuator/health/**");
    }
}
```

需要在 `WebConfig` 中注入 `ActuatorSecurityConfig`。

- [ ] **Step 3: 提交**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/infrastructure/config/ActuatorSecurityConfig.java \
       gateway-boot/src/main/java/com/codingas/gateway/infrastructure/config/WebConfig.java
git commit -m "feat(actuator): 新增 ActuatorSecurityConfig — health 端点公开访问控制"
```

---

### Task 6: application.yml — 配置 health group 和属性

**Files:**
- Modify: `gateway-boot/src/main/resources/application.yml`

- [ ] **Step 1: 更新 application.yml**

在 `management` 部分添加 health group 配置和 public-access 配置。在 `gateway` 部分添加 health provider 配置：

```yaml
# 可观测性配置（修改）
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics,traces
      base-path: /actuator
  endpoint:
    health:
      show-details: when_authorized
      public-access: true
      group:
        liveness:
          include: ping
        readiness:
          include: db, providerRegistry
  metrics:
    tags:
      application: ${spring.application.name}
  tracing:
    sampling:
      probability: 1.0

# 网关配置（追加）
gateway:
  llm:
    timeout-seconds: 30
  health:
    provider:
      stale-threshold: 300s
      failure-threshold: 3
      success-threshold: 2
      probe-timeout: 10s
```

- [ ] **Step 2: 验证应用启动**

Run: `./mvnw spring-boot:run -pl gateway-boot -Dspring-boot.run.profiles=local`
验证：访问 `/actuator/health/liveness` 和 `/actuator/health/readiness` 返回正确 JSON

- [ ] **Step 3: 提交**

```bash
git add gateway-boot/src/main/resources/application.yml
git commit -m "feat(actuator): 配置 health group (liveness/readiness) 和 Provider 健康检测属性"
```

---

### Task 7: Helm values.yaml — K8s probe 路径

**Files:**
- Modify: `deployments/helm/llm-gateway/values.yaml`

- [ ] **Step 1: 更新 probe 路径**

```yaml
livenessProbe:
  enabled: true
  path: /actuator/health/liveness  # 原 /api/v1/health
  initialDelaySeconds: 60
  periodSeconds: 10
  timeoutSeconds: 5
  failureThreshold: 6

readinessProbe:
  enabled: true
  path: /actuator/health/readiness  # 原 /api/v1/health
  initialDelaySeconds: 30
  periodSeconds: 5
  timeoutSeconds: 3
  failureThreshold: 3
```

- [ ] **Step 2: 提交**

```bash
git add deployments/helm/llm-gateway/values.yaml
git commit -m "feat(helm): K8s probe 路径指向 Actuator liveness/readiness 端点"
```

---

### Task 8: 删除 HealthController

**Files:**
- Delete: `adapter/api/HealthController.java`
- Delete: `test/adapter/api/HealthControllerTest.java`

- [ ] **Step 1: 删除文件**

```bash
git rm gateway-boot/src/main/java/com/codingas/gateway/adapter/api/HealthController.java \
       gateway-boot/src/test/java/com/codingas/gateway/adapter/api/HealthControllerTest.java
```

- [ ] **Step 2: 检查是否有其他代码引用 HealthController**

Run: `grep -r "HealthController" gateway-boot/src/ --include="*.java"`
Expected: 无结果

- [ ] **Step 3: 验证编译通过**

Run: `./mvnw compile -pl gateway-boot`
Expected: BUILD SUCCESS

- [ ] **Step 4: 提交**

```bash
git commit -m "refactor(actuator): 删除硬编码 HealthController，由 Actuator 接管"
```

---

### Task 9: 集成测试 — Actuator 端点验证

**Files:**
- Create: `test/infrastructure/actuator/ActuatorHealthIntegrationTest.java`

- [ ] **Step 1: 写集成测试**

```java
package com.codingas.gateway.infrastructure.actuator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Actuator 健康端点集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Actuator 健康端点集成测试")
class ActuatorHealthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("/actuator/health/liveness 返回 200 且包含 ping")
    void liveness_returnsOkWithPing() throws Exception {
        mockMvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components.ping").exists());
    }

    @Test
    @DisplayName("/actuator/health/readiness 返回 200 且包含 db 和 providerRegistry")
    void readiness_returnsOkWithDbAndProvider() throws Exception {
        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.db").exists())
                .andExpect(jsonPath("$.components.providerRegistry").exists());
    }

    @Test
    @DisplayName("/actuator/health 返回完整健康信息")
    void health_returnsFullDetails() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.components").exists());
    }
}
```

- [ ] **Step 2: 运行集成测试**

Run: `./mvnw test -pl gateway-boot -Dtest=ActuatorHealthIntegrationTest`
Expected: PASS

- [ ] **Step 3: 提交**

```bash
git add gateway-boot/src/test/java/com/codingas/gateway/infrastructure/actuator/ActuatorHealthIntegrationTest.java
git commit -m "test(actuator): 新增 Actuator 健康端点集成测试"
```

---

### Task 10: 全量验证与最终提交

- [ ] **Step 1: 运行全量测试**

Run: `./mvnw test -pl gateway-boot`
Expected: BUILD SUCCESS

- [ ] **Step 2: 验证应用启动和端点可用**

Run: `./mvnw spring-boot:run -pl gateway-boot -Dspring-boot.run.profiles=local`

手动验证：
- `curl http://localhost:8080/actuator/health/liveness` → `{"status":"UP",...}`
- `curl http://localhost:8080/actuator/health/readiness` → `{"status":"UP",...,"components":{"db":...,"providerRegistry":...}}`
- `curl http://localhost:8080/actuator/health` → 完整健康信息

- [ ] **Step 3: 验证 public-access=false 时 health 端点需认证**

设置 `management.endpoint.health.public-access=false`，重启应用，验证 `/actuator/health` 被安全拦截器拦截。

- [ ] **Step 4: 最终提交（如有遗漏修复）**

```bash
git add -A
git commit -m "feat(actuator): Spring Boot Actuator 健康检测完整实现"
```

---

## 验证清单

- [ ] `/actuator/health/liveness` 只含 ping，返回 UP
- [ ] `/actuator/health/readiness` 含 db + providerRegistry
- [ ] `/actuator/health` 含完整详情
- [ ] Provider DOWN 时不影响 liveness（Pod 不重启）
- [ ] Provider DOWN 时 readiness DOWN（Pod 不接收流量）
- [ ] 至少一个 Provider UP 时 readiness UP
- [ ] `public-access=true` 时 health 端点无需认证
- [ ] `public-access=false` 时 health 端点需认证
- [ ] K8s probe 路径指向 Actuator
- [ ] 旧 HealthController 已删除
- [ ] 全量测试通过
