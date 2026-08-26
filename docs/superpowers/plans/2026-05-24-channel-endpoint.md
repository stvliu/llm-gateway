# ChannelEndpoint 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 引入 ChannelEndpoint 实体，解决"一个渠道多协议端点"问题，消除凭证/模型/额度冗余

**Architecture:** Channel 失去 endpointUrl/protocol 字段，下沉到独立实体 ChannelEndpoint；路由时先选 Channel，再从 endpoints 中按入站协议匹配 ChannelEndpoint，无匹配则降级做跨协议转换

**Tech Stack:** Java 21, Spring Boot 3.5.x, JPA/Hibernate, Flyway, H2(dev)/PostgreSQL(prod)

---

## 文件结构

| 操作 | 文件路径 | 职责 |
|------|---------|------|
| 新增 | `domain/supply/entity/ChannelEndpoint.java` | ChannelEndpoint 实体 |
| 新增 | `domain/supply/enums/ChannelEndpointState.java` | 端点状态枚举 (ACTIVE/DISABLED) |
| 新增 | `domain/supply/gateway/ChannelEndpointGateway.java` | 端点持久化接口 |
| 修改 | `domain/supply/entity/Channel.java` | 删除 endpointUrl/protocol 字段 |
| 修改 | `domain/supply/valueobject/RoutingContext.java` | 新增 channelEndpointId/upstreamProtocol/needsProtocolAdaptation |
| 修改 | `domain/supply/service/ChannelDomainService.java` | 新增 resolveEndpoint 方法 |
| 修改 | `domain/supply/gateway/ChannelGateway.java` | 删除 protocol 相关查询方法 |
| 修改 | `infrastructure/supply/gateway/database/dataobject/ChannelDo.java` | 删除 endpointUrl/protocol 列 |
| 修改 | `infrastructure/supply/gateway/ChannelGatewayImpl.java` | 移除 protocol 相关转换/查询 |
| 修改 | `infrastructure/supply/gateway/database/repository/ChannelRepository.java` | 删除 protocol 相关查询方法 |
| 新增 | `infrastructure/supply/gateway/database/dataobject/ChannelEndpointDo.java` | 端点数据对象 |
| 新增 | `infrastructure/supply/gateway/database/repository/ChannelEndpointRepository.java` | 端点 JPA Repository |
| 新增 | `infrastructure/supply/gateway/ChannelEndpointGatewayImpl.java` | 端点 Gateway 实现 |
| 修改 | `application/proxy/SupplyRoutingService.java` | resolve 增加 resolveEndpoint 步骤 |
| 修改 | `application/proxy/ProxyServiceImpl.java` | 适配 RoutingContext 新字段 |
| 修改 | `application/channel/ChannelServiceImpl.java` | 适配 Channel 字段变更 |
| 修改 | `application/channel/dto/ChannelRequest.java` | 移除 endpointUrl/protocol |
| 修改 | `application/channel/dto/ChannelResponse.java` | 移除 endpointUrl/protocol，新增 endpoints |
| 新增 | `application/channel/dto/ChannelEndpointRequest.java` | 端点创建/更新请求 |
| 新增 | `application/channel/dto/ChannelEndpointResponse.java` | 端点响应 |
| 修改 | `adapter/api/ChannelController.java` | 新增端点管理端点 |
| 修改 | `application/init/DataInitializer.java` | 适配 ChannelEndpoint 创建 |
| 修改 | `db/migration/V35__supply_domain_refactor.sql` | 重写：含 channel_endpoints 表 + 数据迁移 |
| 修改 | `test/.../SupplyRoutingServiceTest.java` | 适配 ChannelEndpoint |
| 修改 | `test/.../ProxyServiceTest.java` | 适配 RoutingContext 新字段 |
| 新增 | `test/.../ChannelDomainServiceTest.java` | resolveEndpoint 测试 |
| 新增 | `test/.../ChannelEndpointGatewayImplTest.java` | Gateway 实现测试 |

---

### Task 1: 新增 ChannelEndpoint 实体和枚举

**Files:**
- Create: `gateway-boot/src/main/java/com/codingas/gateway/domain/supply/entity/ChannelEndpoint.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/domain/supply/enums/ChannelEndpointState.java`
- Test: `gateway-boot/src/test/java/com/codingas/gateway/domain/supply/entity/ChannelEndpointTest.java`

- [ ] **Step 1: 写 ChannelEndpointState 枚举的测试**

```java
package com.codingas.gateway.domain.supply.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ChannelEndpointState 枚举测试")
class ChannelEndpointStateTest {

    @Test
    @DisplayName("fromCode — 正常值")
    void fromCode_valid() {
        assertThat(ChannelEndpointState.fromCode("ACTIVE")).isEqualTo(ChannelEndpointState.ACTIVE);
        assertThat(ChannelEndpointState.fromCode("DISABLED")).isEqualTo(ChannelEndpointState.DISABLED);
    }

    @Test
    @DisplayName("fromCode — 大小写不敏感")
    void fromCode_caseInsensitive() {
        assertThat(ChannelEndpointState.fromCode("active")).isEqualTo(ChannelEndpointState.ACTIVE);
    }

    @Test
    @DisplayName("fromCode — 无效值抛异常")
    void fromCode_invalid() {
        assertThatThrownBy(() -> ChannelEndpointState.fromCode("INVALID"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("isAvailable — ACTIVE 返回 true")
    void isAvailable_active() {
        assertThat(ChannelEndpointState.ACTIVE.isAvailable()).isTrue();
    }

    @Test
    @DisplayName("isAvailable — DISABLED 返回 false")
    void isAvailable_disabled() {
        assertThat(ChannelEndpointState.DISABLED.isAvailable()).isFalse();
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `./mvnw test -pl gateway-boot -Dtest=ChannelEndpointStateTest -DfailIfNoTests=false -q`
Expected: FAIL — 类不存在

- [ ] **Step 3: 实现 ChannelEndpointState 枚举**

```java
package com.codingas.gateway.domain.supply.enums;

/**
 * 渠道端点状态枚举
 */
public enum ChannelEndpointState {

    ACTIVE("active"),
    DISABLED("disabled");

    private final String code;

    ChannelEndpointState(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static ChannelEndpointState fromCode(String code) {
        for (ChannelEndpointState state : values()) {
            if (state.code.equalsIgnoreCase(code)) {
                return state;
            }
        }
        throw new IllegalArgumentException("Unknown channel endpoint state: " + code);
    }

    /**
     * 判断端点是否可用
     */
    public boolean isAvailable() {
        return this == ACTIVE;
    }
}
```

- [ ] **Step 4: 写 ChannelEndpoint 实体的测试**

```java
package com.codingas.gateway.domain.supply.entity;

import com.codingas.gateway.domain.supply.enums.ChannelEndpointState;
import com.codingas.gateway.domain.supply.enums.Protocol;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ChannelEndpoint 实体测试")
class ChannelEndpointTest {

    @Test
    @DisplayName("创建 ChannelEndpoint — 默认 ACTIVE")
    void create_defaultState() {
        ChannelEndpoint endpoint = new ChannelEndpoint();
        endpoint.setChannelId(1L);
        endpoint.setProtocol(Protocol.OPENAI);
        endpoint.setEndpointUrl("https://api.openai.com");

        assertThat(endpoint.getChannelId()).isEqualTo(1L);
        assertThat(endpoint.getProtocol()).isEqualTo(Protocol.OPENAI);
        assertThat(endpoint.getEndpointUrl()).isEqualTo("https://api.openai.com");
        assertThat(endpoint.getState()).isEqualTo(ChannelEndpointState.ACTIVE);
    }

    @Test
    @DisplayName("isAvailable — ACTIVE 返回 true")
    void isAvailable_active() {
        ChannelEndpoint endpoint = new ChannelEndpoint();
        endpoint.setState(ChannelEndpointState.ACTIVE);
        assertThat(endpoint.isAvailable()).isTrue();
    }

    @Test
    @DisplayName("isAvailable — DISABLED 返回 false")
    void isAvailable_disabled() {
        ChannelEndpoint endpoint = new ChannelEndpoint();
        endpoint.setState(ChannelEndpointState.DISABLED);
        assertThat(endpoint.isAvailable()).isFalse();
    }

    @Test
    @DisplayName("禁用端点")
    void disable() {
        ChannelEndpoint endpoint = new ChannelEndpoint();
        endpoint.setState(ChannelEndpointState.ACTIVE);
        endpoint.disable();
        assertThat(endpoint.getState()).isEqualTo(ChannelEndpointState.DISABLED);
        assertThat(endpoint.isAvailable()).isFalse();
    }

    @Test
    @DisplayName("启用端点")
    void enable() {
        ChannelEndpoint endpoint = new ChannelEndpoint();
        endpoint.setState(ChannelEndpointState.DISABLED);
        endpoint.enable();
        assertThat(endpoint.getState()).isEqualTo(ChannelEndpointState.ACTIVE);
        assertThat(endpoint.isAvailable()).isTrue();
    }
}
```

- [ ] **Step 5: 运行测试确认失败**

Run: `./mvnw test -pl gateway-boot -Dtest=ChannelEndpointTest -DfailIfNoTests=false -q`
Expected: FAIL — 类不存在

- [ ] **Step 6: 实现 ChannelEndpoint 实体**

```java
package com.codingas.gateway.domain.supply.entity;

import com.codingas.gateway.common.entity.BaseEntity;
import com.codingas.gateway.common.entity.DomainEntity;
import com.codingas.gateway.domain.supply.enums.ChannelEndpointState;
import com.codingas.gateway.domain.supply.enums.Protocol;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 渠道端点实体
 *
 * <p>一个 ChannelEndpoint 声明一个协议端点——只回答"用什么协议、调哪个 URL"。</p>
 * <p>一个 Channel 可拥有多个 ChannelEndpoint（如火山引擎 Coding Plan 同时提供 OpenAI 和 Anthropic 两个端点）。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@DomainEntity
public class ChannelEndpoint extends BaseEntity {

    /** 所属渠道 ID */
    private Long channelId;

    /** 协议类型 */
    private Protocol protocol;

    /** 端点 URL */
    private String endpointUrl;

    /** 端点状态 */
    private ChannelEndpointState state = ChannelEndpointState.ACTIVE;

    /**
     * 判断端点是否可用
     */
    public boolean isAvailable() {
        return ChannelEndpointState.ACTIVE.equals(state);
    }

    /**
     * 禁用端点
     */
    public void disable() {
        this.state = ChannelEndpointState.DISABLED;
    }

    /**
     * 启用端点
     */
    public void enable() {
        this.state = ChannelEndpointState.ACTIVE;
    }
}
```

- [ ] **Step 7: 运行测试确认通过**

Run: `./mvnw test -pl gateway-boot -Dtest="ChannelEndpointStateTest,ChannelEndpointTest" -DfailIfNoTests=false -q`
Expected: PASS

- [ ] **Step 8: 提交**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/domain/supply/entity/ChannelEndpoint.java \
       gateway-boot/src/main/java/com/codingas/gateway/domain/supply/enums/ChannelEndpointState.java \
       gateway-boot/src/test/java/com/codingas/gateway/domain/supply/enums/ChannelEndpointStateTest.java \
       gateway-boot/src/test/java/com/codingas/gateway/domain/supply/entity/ChannelEndpointTest.java
git commit -m "feat(supply): 新增 ChannelEndpoint 实体和 ChannelEndpointState 枚举"
```

---

### Task 2: 新增 ChannelEndpointGateway 接口

**Files:**
- Create: `gateway-boot/src/main/java/com/codingas/gateway/domain/supply/gateway/ChannelEndpointGateway.java`

- [ ] **Step 1: 定义 ChannelEndpointGateway 接口**

```java
package com.codingas.gateway.domain.supply.gateway;

import com.codingas.gateway.domain.supply.entity.ChannelEndpoint;
import com.codingas.gateway.domain.supply.enums.Protocol;

import java.util.List;
import java.util.Optional;

/**
 * 渠道端点持久化接口
 */
public interface ChannelEndpointGateway {

    /**
     * 保存端点
     */
    ChannelEndpoint save(ChannelEndpoint endpoint);

    /**
     * 根据 ID 查找端点
     */
    Optional<ChannelEndpoint> findById(Long id);

    /**
     * 根据渠道 ID 查找所有端点
     */
    List<ChannelEndpoint> findByChannelId(Long channelId);

    /**
     * 根据渠道 ID 查找活跃端点
     */
    List<ChannelEndpoint> findActiveByChannelId(Long channelId);

    /**
     * 根据渠道 ID 和协议查找端点
     */
    Optional<ChannelEndpoint> findByChannelIdAndProtocol(Long channelId, Protocol protocol);

    /**
     * 查询所有端点
     */
    List<ChannelEndpoint> findAll();

    /**
     * 删除端点
     */
    void deleteById(Long id);
}
```

- [ ] **Step 2: 提交**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/domain/supply/gateway/ChannelEndpointGateway.java
git commit -m "feat(supply): 新增 ChannelEndpointGateway 接口"
```

---

### Task 3: Channel 删除 endpointUrl/protocol 字段

**Files:**
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/domain/supply/entity/Channel.java:30-34`
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/domain/supply/gateway/ChannelGateway.java:37-47`
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/supply/gateway/database/dataobject/ChannelDo.java:23-28`
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/supply/gateway/database/repository/ChannelRepository.java:18-22`
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/supply/gateway/ChannelGatewayImpl.java:46-48,57-59,100-105,120-125`
- Test: `gateway-boot/src/test/java/com/codingas/gateway/domain/supply/entity/ChannelTest.java`

- [ ] **Step 1: 写 Channel 实体测试（验证字段移除）**

```java
package com.codingas.gateway.domain.supply.entity;

import com.codingas.gateway.domain.supply.enums.ChannelState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Channel 实体测试")
class ChannelTest {

    @Test
    @DisplayName("Channel 不再包含 endpointUrl 和 protocol 字段")
    void channel_hasNoEndpointOrProtocolFields() {
        Channel channel = new Channel();
        channel.setId(1L);
        channel.setProviderId(100L);
        channel.setName("Test Channel");
        channel.setBillingMode(com.codingas.gateway.domain.supply.enums.BillingMode.PAY_AS_YOU_GO);
        channel.setState(ChannelState.ACTIVE);

        // 验证 Channel 可以正常创建，不含 endpointUrl/protocol
        assertThat(channel.getId()).isEqualTo(1L);
        assertThat(channel.getProviderId()).isEqualTo(100L);
        assertThat(channel.getName()).isEqualTo("Test Channel");
        assertThat(channel.isAvailable()).isTrue();
    }
}
```

- [ ] **Step 2: 修改 Channel 实体 — 删除 endpointUrl/protocol 字段**

将 `Channel.java` 中：
```java
    /** 单一端点 URL */
    private String endpointUrl;

    /** 单一协议类型 */
    private Protocol protocol;
```
删除，同时删除 `import com.codingas.gateway.domain.supply.enums.Protocol;`，并更新 Javadoc：

```java
package com.codingas.gateway.domain.supply.entity;

import com.codingas.gateway.common.entity.BaseEntity;
import com.codingas.gateway.common.entity.DomainEntity;
import com.codingas.gateway.domain.supply.enums.BillingMode;
import com.codingas.gateway.domain.supply.enums.ChannelState;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;

/**
 * 渠道实体
 *
 * <p>渠道是逻辑接入点——一个渠道就是一个套餐/一个计费单位/一组模型。</p>
 * <p>协议端点已下沉到 ChannelEndpoint，Channel 不再持有 endpointUrl/protocol。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@DomainEntity
@Slf4j
public class Channel extends BaseEntity {

    private Long providerId;

    private String name;

    /** 计费模式 */
    private BillingMode billingMode;

    /** 配额限制（Token 数） */
    private Long quotaLimit;

    private Integer priority;

    private Integer weight;

    private Integer timeout;

    private Integer maxRetries;

    private ChannelState state = ChannelState.ACTIVE;

    @Override
    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public Instant getUpdatedAt() {
        return updatedAt;
    }

    /**
     * 检查渠道是否可用
     */
    public boolean isAvailable() {
        return ChannelState.ACTIVE.equals(state);
    }
}
```

- [ ] **Step 3: 修改 ChannelGateway — 删除 protocol 相关查询方法**

从 `ChannelGateway.java` 中删除：
```java
    List<Channel> findByProtocol(Protocol protocol);
    List<Channel> findActiveByProviderIdAndProtocol(Long providerId, Protocol protocol);
```
同时删除 `import com.codingas.gateway.domain.supply.enums.Protocol;`

- [ ] **Step 4: 修改 ChannelDo — 删除 endpointUrl/protocol 列**

从 `ChannelDo.java` 中删除：
```java
    @Column(name = "endpoint_url", length = 512)
    private String endpointUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "protocol", length = 32)
    private com.codingas.gateway.domain.supply.enums.Protocol protocol;
```

- [ ] **Step 5: 修改 ChannelRepository — 删除 protocol 相关查询**

从 `ChannelRepository.java` 中删除：
```java
    List<ChannelDo> findByProtocol(Protocol protocol);
    List<ChannelDo> findByProviderIdAndProtocolAndState(Long providerId, Protocol protocol, ChannelState state);
```
同时删除 `import com.codingas.gateway.domain.supply.enums.Protocol;`

- [ ] **Step 6: 修改 ChannelGatewayImpl — 移除 protocol 相关方法及转换**

1. 删除 `findByProtocol` 和 `findActiveByProviderIdAndProtocol` 方法实现
2. 在 `toEntity` 方法中删除 `entity.setEndpointUrl(doObj.getEndpointUrl())` 和 `entity.setProtocol(doObj.getProtocol())`
3. 在 `toDo` 方法中删除 `doObj.setEndpointUrl(entity.getEndpointUrl())` 和 `doObj.setProtocol(entity.getProtocol())`

- [ ] **Step 7: 运行测试确认通过**

Run: `./mvnw test -pl gateway-boot -Dtest=ChannelTest -DfailIfNoTests=false -q`
Expected: PASS

- [ ] **Step 8: 提交**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/domain/supply/entity/Channel.java \
       gateway-boot/src/main/java/com/codingas/gateway/domain/supply/gateway/ChannelGateway.java \
       gateway-boot/src/main/java/com/codingas/gateway/infrastructure/supply/gateway/database/dataobject/ChannelDo.java \
       gateway-boot/src/main/java/com/codingas/gateway/infrastructure/supply/gateway/database/repository/ChannelRepository.java \
       gateway-boot/src/main/java/com/codingas/gateway/infrastructure/supply/gateway/ChannelGatewayImpl.java \
       gateway-boot/src/test/java/com/codingas/gateway/domain/supply/entity/ChannelTest.java
git commit -m "refactor(supply): Channel 删除 endpointUrl/protocol 字段，下沉到 ChannelEndpoint"
```

---

### Task 4: 实现 ChannelEndpoint 基础设施层

**Files:**
- Create: `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/supply/gateway/database/dataobject/ChannelEndpointDo.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/supply/gateway/database/repository/ChannelEndpointRepository.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/supply/gateway/ChannelEndpointGatewayImpl.java`
- Test: `gateway-boot/src/test/java/com/codingas/gateway/infrastructure/supply/gateway/ChannelEndpointGatewayImplTest.java`

- [ ] **Step 1: 实现 ChannelEndpointDo**

```java
package com.codingas.gateway.infrastructure.supply.gateway.database.dataobject;

import com.codingas.gateway.infrastructure.common.BaseDo;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 渠道端点数据对象
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "channel_endpoints")
public class ChannelEndpointDo extends BaseDo {

    @Column(name = "channel_id", nullable = false)
    private Long channelId;

    @Enumerated(EnumType.STRING)
    @Column(name = "protocol", nullable = false, length = 32)
    private com.codingas.gateway.domain.supply.enums.Protocol protocol;

    @Column(name = "endpoint_url", nullable = false, length = 512)
    private String endpointUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false, length = 16)
    private com.codingas.gateway.domain.supply.enums.ChannelEndpointState state;
}
```

- [ ] **Step 2: 实现 ChannelEndpointRepository**

```java
package com.codingas.gateway.infrastructure.supply.gateway.database.repository;

import com.codingas.gateway.domain.supply.enums.ChannelEndpointState;
import com.codingas.gateway.domain.supply.enums.Protocol;
import com.codingas.gateway.infrastructure.supply.gateway.database.dataobject.ChannelEndpointDo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 渠道端点 Repository
 */
public interface ChannelEndpointRepository extends JpaRepository<ChannelEndpointDo, Long> {

    List<ChannelEndpointDo> findByChannelId(Long channelId);

    List<ChannelEndpointDo> findByChannelIdAndState(Long channelId, ChannelEndpointState state);

    Optional<ChannelEndpointDo> findByChannelIdAndProtocol(Long channelId, Protocol protocol);
}
```

- [ ] **Step 3: 实现 ChannelEndpointGatewayImpl**

```java
package com.codingas.gateway.infrastructure.supply.gateway;

import com.codingas.gateway.domain.supply.entity.ChannelEndpoint;
import com.codingas.gateway.domain.supply.enums.ChannelEndpointState;
import com.codingas.gateway.domain.supply.enums.Protocol;
import com.codingas.gateway.domain.supply.gateway.ChannelEndpointGateway;
import com.codingas.gateway.infrastructure.supply.gateway.database.dataobject.ChannelEndpointDo;
import com.codingas.gateway.infrastructure.supply.gateway.database.repository.ChannelEndpointRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * 渠道端点持久化实现
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ChannelEndpointGatewayImpl implements ChannelEndpointGateway {

    private final ChannelEndpointRepository channelEndpointRepository;

    @Override
    public ChannelEndpoint save(ChannelEndpoint endpoint) {
        ChannelEndpointDo doObj = toDo(endpoint);
        ChannelEndpointDo saved = channelEndpointRepository.save(doObj);
        return toEntity(saved);
    }

    @Override
    public Optional<ChannelEndpoint> findById(Long id) {
        return channelEndpointRepository.findById(id).map(this::toEntity);
    }

    @Override
    public List<ChannelEndpoint> findByChannelId(Long channelId) {
        return channelEndpointRepository.findByChannelId(channelId)
                .stream().map(this::toEntity).toList();
    }

    @Override
    public List<ChannelEndpoint> findActiveByChannelId(Long channelId) {
        return channelEndpointRepository.findByChannelIdAndState(channelId, ChannelEndpointState.ACTIVE)
                .stream().map(this::toEntity).toList();
    }

    @Override
    public Optional<ChannelEndpoint> findByChannelIdAndProtocol(Long channelId, Protocol protocol) {
        return channelEndpointRepository.findByChannelIdAndProtocol(channelId, protocol)
                .map(this::toEntity);
    }

    @Override
    public List<ChannelEndpoint> findAll() {
        return channelEndpointRepository.findAll()
                .stream().map(this::toEntity).toList();
    }

    @Override
    public void deleteById(Long id) {
        channelEndpointRepository.deleteById(id);
    }

    private ChannelEndpoint toEntity(ChannelEndpointDo doObj) {
        ChannelEndpoint entity = new ChannelEndpoint();
        entity.setId(doObj.getId());
        entity.setChannelId(doObj.getChannelId());
        entity.setProtocol(doObj.getProtocol());
        entity.setEndpointUrl(doObj.getEndpointUrl());
        entity.setState(doObj.getState());
        entity.setCreatedBy(doObj.getCreatedBy());
        entity.setUpdatedBy(doObj.getUpdatedBy());
        entity.setCreatedAt(doObj.getCreatedAt());
        entity.setUpdatedAt(doObj.getUpdatedAt());
        return entity;
    }

    private ChannelEndpointDo toDo(ChannelEndpoint entity) {
        ChannelEndpointDo doObj = new ChannelEndpointDo();
        doObj.setId(entity.getId());
        doObj.setChannelId(entity.getChannelId());
        doObj.setProtocol(entity.getProtocol());
        doObj.setEndpointUrl(entity.getEndpointUrl());
        doObj.setState(entity.getState() != null ? entity.getState() : ChannelEndpointState.ACTIVE);
        doObj.setCreatedBy(entity.getCreatedBy());
        doObj.setUpdatedBy(entity.getUpdatedBy());
        return doObj;
    }
}
```

- [ ] **Step 4: 编译确认**

Run: `./mvnw compile -pl gateway-boot -q`
Expected: BUILD SUCCESS

- [ ] **Step 5: 提交**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/infrastructure/supply/gateway/database/dataobject/ChannelEndpointDo.java \
       gateway-boot/src/main/java/com/codingas/gateway/infrastructure/supply/gateway/database/repository/ChannelEndpointRepository.java \
       gateway-boot/src/main/java/com/codingas/gateway/infrastructure/supply/gateway/ChannelEndpointGatewayImpl.java
git commit -m "feat(supply): 实现 ChannelEndpoint 基础设施层（Do/Repository/GatewayImpl）"
```

---

### Task 5: 更新 RoutingContext 不可变对象

**Files:**
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/domain/supply/valueobject/RoutingContext.java`
- Test: `gateway-boot/src/test/java/com/codingas/gateway/domain/supply/valueobject/RoutingContextTest.java`

- [ ] **Step 1: 写 RoutingContext 新字段测试**

```java
package com.codingas.gateway.domain.supply.valueobject;

import com.codingas.gateway.domain.supply.enums.Protocol;
import com.codingas.gateway.domain.supply.enums.RoutingStrategy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RoutingContext 不可变对象测试")
class RoutingContextTest {

    @Test
    @DisplayName("构建含 channelEndpointId 和 upstreamProtocol 的路由上下文")
    void build_withEndpointFields() {
        RoutingContext ctx = new RoutingContext(
                100L,                   // channelId
                200L,                   // channelEndpointId
                "https://api.openai.com", // endpointUrl
                Protocol.OPENAI,        // upstreamProtocol
                "sk-test",              // providerApiKey
                60,                     // timeout
                false                   // needsProtocolAdaptation
        );

        assertThat(ctx.channelId()).isEqualTo(100L);
        assertThat(ctx.channelEndpointId()).isEqualTo(200L);
        assertThat(ctx.endpointUrl()).isEqualTo("https://api.openai.com");
        assertThat(ctx.upstreamProtocol()).isEqualTo(Protocol.OPENAI);
        assertThat(ctx.needsProtocolAdaptation()).isFalse();
    }

    @Test
    @DisplayName("跨协议场景 — needsProtocolAdaptation 为 true")
    void build_needsProtocolAdaptation() {
        RoutingContext ctx = new RoutingContext(
                100L, 200L,
                "https://api.anthropic.com",
                Protocol.ANTHROPIC,
                "sk-test", 60, true
        );

        assertThat(ctx.needsProtocolAdaptation()).isTrue();
        assertThat(ctx.upstreamProtocol()).isEqualTo(Protocol.ANTHROPIC);
    }
}
```

- [ ] **Step 2: 更新 RoutingContext record**

```java
package com.codingas.gateway.domain.supply.valueobject;

import com.codingas.gateway.domain.supply.enums.Protocol;

/**
 * 路由上下文不可变对象
 *
 * <p>携带请求路由所需的全部信息。</p>
 */
public record RoutingContext(
        Long channelId,
        Long channelEndpointId,
        String endpointUrl,
        Protocol upstreamProtocol,
        String providerApiKey,
        Integer timeout,
        boolean needsProtocolAdaptation
) {}
```

注意：移除了原来的 `Protocol protocol`、`RoutingStrategy strategy` 字段，替换为 `channelEndpointId`、`upstreamProtocol`、`needsProtocolAdaptation`。

- [ ] **Step 3: 运行测试确认通过**

Run: `./mvnw test -pl gateway-boot -Dtest=RoutingContextTest -DfailIfNoTests=false -q`
Expected: PASS

- [ ] **Step 4: 提交**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/domain/supply/valueobject/RoutingContext.java \
       gateway-boot/src/test/java/com/codingas/gateway/domain/supply/valueobject/RoutingContextTest.java
git commit -m "refactor(supply): RoutingContext 增加 channelEndpointId/upstreamProtocol/needsProtocolAdaptation"
```

---

### Task 6: 更新 ChannelDomainService — 增加 resolveEndpoint

**Files:**
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/domain/supply/service/ChannelDomainService.java`
- Test: `gateway-boot/src/test/java/com/codingas/gateway/domain/supply/service/ChannelDomainServiceTest.java`

- [ ] **Step 1: 写 resolveEndpoint 测试**

```java
package com.codingas.gateway.domain.supply.service;

import com.codingas.gateway.domain.supply.entity.Channel;
import com.codingas.gateway.domain.supply.entity.ChannelEndpoint;
import com.codingas.gateway.domain.supply.enums.ChannelEndpointState;
import com.codingas.gateway.domain.supply.enums.Protocol;
import com.codingas.gateway.domain.supply.gateway.ChannelEndpointGateway;
import com.codingas.gateway.domain.supply.gateway.ChannelGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChannelDomainService 测试")
class ChannelDomainServiceTest {

    @Mock
    private ChannelGateway channelGateway;

    @Mock
    private ChannelEndpointGateway channelEndpointGateway;

    private ChannelDomainService service;

    @BeforeEach
    void setUp() {
        service = new ChannelDomainService(channelGateway, channelEndpointGateway);
    }

    @Nested
    @DisplayName("resolveEndpoint 测试")
    class ResolveEndpointTests {

        @Test
        @DisplayName("优先匹配同名协议端点 — 入站 ANTHROPIC → 选 ANTHROPIC 端点")
        void resolveEndpoint_matchSameProtocol() {
            Channel channel = new Channel();
            channel.setId(1L);

            ChannelEndpoint anthropicEndpoint = new ChannelEndpoint();
            anthropicEndpoint.setId(10L);
            anthropicEndpoint.setProtocol(Protocol.ANTHROPIC);
            anthropicEndpoint.setEndpointUrl("https://api.anthropic.com");
            anthropicEndpoint.setState(ChannelEndpointState.ACTIVE);

            ChannelEndpoint openaiEndpoint = new ChannelEndpoint();
            openaiEndpoint.setId(11L);
            openaiEndpoint.setProtocol(Protocol.OPENAI);
            openaiEndpoint.setEndpointUrl("https://api.openai.com");
            openaiEndpoint.setState(ChannelEndpointState.ACTIVE);

            when(channelEndpointGateway.findActiveByChannelId(1L))
                    .thenReturn(List.of(openaiEndpoint, anthropicEndpoint));

            ChannelEndpoint result = service.resolveEndpoint(channel, Protocol.ANTHROPIC);

            assertThat(result.getId()).isEqualTo(10L);
            assertThat(result.getProtocol()).isEqualTo(Protocol.ANTHROPIC);
        }

        @Test
        @DisplayName("无匹配端点时降级选第一个可用端点 — 入站 OPENAI → 只有 ANTHROPIC 端点")
        void resolveEndpoint_fallbackToFirstAvailable() {
            Channel channel = new Channel();
            channel.setId(1L);

            ChannelEndpoint anthropicEndpoint = new ChannelEndpoint();
            anthropicEndpoint.setId(10L);
            anthropicEndpoint.setProtocol(Protocol.ANTHROPIC);
            anthropicEndpoint.setEndpointUrl("https://api.anthropic.com");
            anthropicEndpoint.setState(ChannelEndpointState.ACTIVE);

            when(channelEndpointGateway.findActiveByChannelId(1L))
                    .thenReturn(List.of(anthropicEndpoint));

            ChannelEndpoint result = service.resolveEndpoint(channel, Protocol.OPENAI);

            assertThat(result.getId()).isEqualTo(10L);
            assertThat(result.getProtocol()).isEqualTo(Protocol.ANTHROPIC);
        }

        @Test
        @DisplayName("渠道无可用端点时抛出异常")
        void resolveEndpoint_noAvailableEndpoint_throwsException() {
            Channel channel = new Channel();
            channel.setId(1L);

            when(channelEndpointGateway.findActiveByChannelId(1L))
                    .thenReturn(List.of());

            assertThatThrownBy(() -> service.resolveEndpoint(channel, Protocol.OPENAI))
                    .isInstanceOf(com.codingas.gateway.domain.supply.exception.ChannelException.class)
                    .hasMessageContaining("CHANNEL_NO_ENDPOINT");
        }
    }
}
```

- [ ] **Step 2: 修改 ChannelDomainService — 增加 channelEndpointGateway 依赖和 resolveEndpoint 方法**

更新 `ChannelDomainService.java`：

```java
package com.codingas.gateway.domain.supply.service;

import com.codingas.gateway.domain.supply.entity.Channel;
import com.codingas.gateway.domain.supply.entity.ChannelEndpoint;
import com.codingas.gateway.domain.supply.enums.ChannelEndpointState;
import com.codingas.gateway.domain.supply.enums.ChannelState;
import com.codingas.gateway.domain.supply.enums.Protocol;
import com.codingas.gateway.domain.supply.exception.ChannelException;
import com.codingas.gateway.domain.supply.gateway.ChannelEndpointGateway;
import com.codingas.gateway.domain.supply.gateway.ChannelGateway;
import com.codingas.gateway.domain.supply.valueobject.RoutingContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 渠道管理服务
 *
 * <p>封装渠道相关的核心业务逻辑。</p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ChannelDomainService {

    private final ChannelGateway channelGateway;
    private final ChannelEndpointGateway channelEndpointGateway;

    /**
     * 创建渠道
     */
    public Channel create(Channel channel) {
        if (channelGateway.existsByProviderIdAndName(channel.getProviderId(), channel.getName())) {
            throw new ChannelException("CHANNEL_NAME_DUPLICATE", "同一供应商下渠道名已存在: " + channel.getName());
        }
        return channelGateway.save(channel);
    }

    /**
     * 更新渠道
     */
    public Channel update(Channel channel) {
        return channelGateway.save(channel);
    }

    /**
     * 启用渠道
     */
    public Channel enable(Long id) {
        Channel channel = channelGateway.findById(id)
                .orElseThrow(() -> new ChannelException("CHANNEL_NOT_FOUND", "渠道不存在: " + id));
        channel.setState(ChannelState.ACTIVE);
        return channelGateway.save(channel);
    }

    /**
     * 禁用渠道
     */
    public Channel disable(Long id) {
        Channel channel = channelGateway.findById(id)
                .orElseThrow(() -> new ChannelException("CHANNEL_NOT_FOUND", "渠道不存在: " + id));
        channel.setState(ChannelState.DISABLED);
        return channelGateway.save(channel);
    }

    /**
     * 软删除渠道
     */
    public void delete(Long id) {
        Channel channel = channelGateway.findById(id)
                .orElseThrow(() -> new ChannelException("CHANNEL_NOT_FOUND", "渠道不存在: " + id));
        channel.setState(ChannelState.DELETED);
        channelGateway.save(channel);
    }

    /**
     * 查找路由上下文
     */
    public Optional<RoutingContext> findRoutingContext(Long channelId) {
        return channelGateway.findRoutingContext(channelId);
    }

    /**
     * 查找指定供应商的活跃渠道
     */
    public List<Channel> findActiveByProviderId(Long providerId) {
        return channelGateway.findByProviderId(providerId).stream()
                .filter(Channel::isAvailable)
                .toList();
    }

    /**
     * 根据入站协议解析渠道端点
     *
     * <p>优先匹配同名协议端点，无匹配则降级选第一个可用端点。</p>
     *
     * @param channel 渠道实体
     * @param inboundProtocol 入站请求的协议类型
     * @return 匹配的 ChannelEndpoint
     * @throws ChannelException 渠道无可用端点
     */
    public ChannelEndpoint resolveEndpoint(Channel channel, Protocol inboundProtocol) {
        List<ChannelEndpoint> activeEndpoints = channelEndpointGateway.findActiveByChannelId(channel.getId());

        if (activeEndpoints.isEmpty()) {
            throw new ChannelException("CHANNEL_NO_ENDPOINT",
                    "渠道无可用端点: channelId=" + channel.getId());
        }

        // 优先匹配同名协议端点
        Optional<ChannelEndpoint> matched = activeEndpoints.stream()
                .filter(e -> e.getProtocol() == inboundProtocol)
                .findFirst();

        if (matched.isPresent()) {
            log.debug("Endpoint protocol matched: channelId={}, endpointId={}, protocol={}",
                    channel.getId(), matched.get().getId(), inboundProtocol);
            return matched.get();
        }

        // 降级：选第一个可用端点
        ChannelEndpoint fallback = activeEndpoints.get(0);
        log.info("Endpoint protocol fallback: channelId={}, inbound={}, fallback={}",
                channel.getId(), inboundProtocol, fallback.getProtocol());
        return fallback;
    }
}
```

- [ ] **Step 3: 运行测试确认通过**

Run: `./mvnw test -pl gateway-boot -Dtest=ChannelDomainServiceTest -DfailIfNoTests=false -q`
Expected: PASS

- [ ] **Step 4: 提交**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/domain/supply/service/ChannelDomainService.java \
       gateway-boot/src/test/java/com/codingas/gateway/domain/supply/service/ChannelDomainServiceTest.java
git commit -m "feat(supply): ChannelDomainService 增加 resolveEndpoint 方法"
```

---

### Task 7: 更新 SupplyRoutingService — 集成 resolveEndpoint

**Files:**
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/application/proxy/SupplyRoutingService.java`
- Modify: `gateway-boot/src/test/java/com/codingas/gateway/application/proxy/SupplyRoutingServiceTest.java`

- [ ] **Step 1: 更新 SupplyRoutingService.resolve — 增加 resolveEndpoint 步骤**

修改 `SupplyRoutingService.java` 中的 `resolve(Long userApiKeyId, String model, String protocol)` 方法：

```java
    /**
     * 基于 userApiKeyId 解析路由上下文
     */
    public RoutingContext resolve(Long userApiKeyId, String model, String protocol) {
        // 1. 查找 UserApiKey
        UserApiKey userApiKey = userApiKeyGateway.findById(userApiKeyId)
                .orElseThrow(() -> new ResourceNotFoundException("UserApiKey", userApiKeyId));

        // 2. 校验 Key 级别模型权限
        if (!userApiKeyDomainService.canAccessModel(userApiKey, model)) {
            throw new ResourceNotFoundException("Model", model);
        }

        // 3. 在关联渠道中匹配 model
        Channel channel = matchChannel(userApiKey.getChannelIds(), model);

        // 4. 选择 ChannelCredential
        ChannelCredential credential = selectChannelCredential(channel.getId());
        if (credential == null) {
            throw new ResourceNotFoundException("ChannelCredential", channel.getId());
        }

        String plainApiKey = credential.getApiKeyPlain();
        if (plainApiKey == null || plainApiKey.isBlank()) {
            throw new ResourceNotFoundException("ChannelCredential", credential.getId());
        }

        // 5. 解析协议端点（新增步骤）
        Protocol inboundProtocol = Protocol.fromCode(protocol);
        ChannelEndpoint endpoint = channelDomainService.resolveEndpoint(channel, inboundProtocol);
        boolean needsAdaptation = endpoint.getProtocol() != inboundProtocol;

        // 6. 获取 Provider 信息
        Provider provider = providerGateway.findById(channel.getProviderId())
                .orElseThrow(() -> new ResourceNotFoundException("Provider", channel.getProviderId()));

        // 7. 构建路由上下文
        return new RoutingContext(
                channel.getId(),
                endpoint.getId(),
                endpoint.getEndpointUrl(),
                endpoint.getProtocol(),
                plainApiKey,
                channel.getTimeout(),
                needsAdaptation
        );
    }
```

需要新增 import：
```java
import com.codingas.gateway.domain.supply.entity.ChannelEndpoint;
import com.codingas.gateway.domain.supply.enums.Protocol;
```

同时更新 `resolve(Identity identity, String model, String protocol)` 方法：
```java
    public RoutingContext resolve(Identity identity, String model, String protocol) {
        return resolve(identity.credentialId(), model, protocol);
    }
```

- [ ] **Step 2: 更新 SupplyRoutingServiceTest 适配新 RoutingContext 字段**

更新测试中的 `createChannel` 方法移除 endpointUrl/protocol，更新 `RoutingContext` 断言：

```java
    private Channel createChannel(Long id, Long providerId, String name, ChannelState state) {
        Channel channel = new Channel();
        channel.setId(id);
        channel.setProviderId(providerId);
        channel.setName(name);
        channel.setState(state);
        channel.setBillingMode(BillingMode.PAY_AS_YOU_GO);
        channel.setTimeout(60);
        return channel;
    }
```

更新断言：
```java
    assertThat(ctx.channelId()).isEqualTo(100L);
    assertThat(ctx.upstreamProtocol()).isEqualTo(Protocol.OPENAI);
    assertThat(ctx.needsProtocolAdaptation()).isFalse();
    assertThat(ctx.timeout()).isEqualTo(60);
```

需要在测试中 mock `channelDomainService.resolveEndpoint`：
```java
    ChannelEndpoint endpoint = new ChannelEndpoint();
    endpoint.setId(200L);
    endpoint.setChannelId(100L);
    endpoint.setProtocol(Protocol.OPENAI);
    endpoint.setEndpointUrl("https://api.openai.com");
    endpoint.setState(ChannelEndpointState.ACTIVE);

    when(channelDomainService.resolveEndpoint(any(Channel.class), eq(Protocol.OPENAI)))
            .thenReturn(endpoint);
```

- [ ] **Step 3: 运行测试确认通过**

Run: `./mvnw test -pl gateway-boot -Dtest=SupplyRoutingServiceTest -DfailIfNoTests=false -q`
Expected: PASS

- [ ] **Step 4: 提交**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/application/proxy/SupplyRoutingService.java \
       gateway-boot/src/test/java/com/codingas/gateway/application/proxy/SupplyRoutingServiceTest.java
git commit -m "feat(supply): SupplyRoutingService 集成 resolveEndpoint 步骤"
```

---

### Task 8: 更新 ProxyServiceImpl — 适配 RoutingContext 新字段

**Files:**
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/application/proxy/ProxyServiceImpl.java`
- Modify: `gateway-boot/src/test/java/com/codingas/gateway/application/proxy/ProxyServiceTest.java`

- [ ] **Step 1: 更新 ProxyServiceImpl 中 RoutingContext 字段引用**

修改 `proxy` 和 `proxyStream` 方法中的 RoutingContext 引用：

```java
    @Override
    public ProtocolResponse proxy(ProtocolRequest request, Identity identity, RoutingStrategy strategy) {
        RoutingContext context = supplyRoutingService.resolve(
                identity, request.getModel(), request.getProtocol());

        log.info("Proxy request routed: model={}, channelId={}, endpointId={}, upstreamProtocol={}, endpoint={}",
                request.getModel(), context.channelId(), context.channelEndpointId(),
                context.upstreamProtocol(), context.endpointUrl());

        String protocolName = context.upstreamProtocol() != null
                ? context.upstreamProtocol().getCode() : "openai";
        int timeoutSeconds = context.timeout() != null ? context.timeout() : 60;

        // 跨协议场景：根据 needsProtocolAdaptation 决定是否做请求/响应转换
        ProtocolGateway gateway = protocolGatewayFactory.create(
                protocolName, context.endpointUrl(), context.providerApiKey(), timeoutSeconds);

        ProtocolRequest gatewayRequest = convertRequestIfNeeded(request, protocolName);
        ProtocolResponse response = gateway.chat(gatewayRequest);

        return convertResponseIfNeeded(response, protocolName, request.getProtocol());
    }
```

`proxyStream` 方法类似更新日志和 protocolName 取值。

- [ ] **Step 2: 更新 ProxyServiceTest 适配新 RoutingContext**

更新测试中的 `testOpenAIContext` 和 `testAnthropicContext`：

```java
    testOpenAIContext = new RoutingContext(
            10L, 20L, "https://api.openai.com", Protocol.OPENAI, "sk-test-key", 60, false);

    testAnthropicContext = new RoutingContext(
            10L, 21L, "https://api.anthropic.com", Protocol.ANTHROPIC, "sk-ant-key", 60, false);
```

- [ ] **Step 3: 运行测试确认通过**

Run: `./mvnw test -pl gateway-boot -Dtest=ProxyServiceTest -DfailIfNoTests=false -q`
Expected: PASS

- [ ] **Step 4: 提交**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/application/proxy/ProxyServiceImpl.java \
       gateway-boot/src/test/java/com/codingas/gateway/application/proxy/ProxyServiceTest.java
git commit -m "refactor(supply): ProxyServiceImpl 适配 RoutingContext 新字段"
```

---

### Task 9: 更新 ChannelGatewayImpl — 移除 findRoutingContext 和 protocol 相关逻辑

**Files:**
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/supply/gateway/ChannelGatewayImpl.java`

- [ ] **Step 1: 更新 ChannelGatewayImpl**

1. 删除 `findRoutingContext` 方法实现（路由上下文现在由 SupplyRoutingService 直接构建）
2. 删除 `findByProtocol` 和 `findActiveByProviderIdAndProtocol` 方法实现
3. 更新 `toEntity` 和 `toDo` 移除 endpointUrl/protocol 字段映射

- [ ] **Step 2: 更新 ChannelGateway 接口**

从 `ChannelGateway.java` 中删除：
```java
    Optional<RoutingContext> findRoutingContext(Long channelId);
```
同时删除 `import com.codingas.gateway.domain.supply.valueobject.RoutingContext;`

- [ ] **Step 3: 编译确认**

Run: `./mvnw compile -pl gateway-boot -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: 提交**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/infrastructure/supply/gateway/ChannelGatewayImpl.java \
       gateway-boot/src/main/java/com/codingas/gateway/domain/supply/gateway/ChannelGateway.java
git commit -m "refactor(supply): ChannelGatewayImpl 移除 findRoutingContext 和 protocol 相关方法"
```

---

### Task 10: 更新应用层 Channel DTO 和 Service — 适配 ChannelEndpoint

**Files:**
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/application/channel/dto/ChannelRequest.java`
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/application/channel/dto/ChannelResponse.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/application/channel/dto/ChannelEndpointRequest.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/application/channel/dto/ChannelEndpointResponse.java`
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/application/channel/ChannelServiceImpl.java`
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/application/channel/ChannelService.java`

- [ ] **Step 1: 更新 ChannelRequest — 移除 endpointUrl/protocol**

```java
package com.codingas.gateway.application.channel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 渠道创建/更新请求
 */
@Data
public class ChannelRequest {

    @NotNull(message = "供应商 ID 不能为空")
    private Long providerId;

    @NotBlank(message = "渠道名称不能为空")
    private String name;

    /** 计费模式 */
    @NotBlank(message = "计费模式不能为空")
    private String billingMode;

    /** 配额限制（Token 数） */
    private Long quotaLimit;

    private Integer priority;

    private Integer weight;

    private Integer timeout;

    private Integer maxRetries;
}
```

- [ ] **Step 2: 更新 ChannelResponse — 替换 endpointUrl/protocol 为 endpoints 列表**

```java
package com.codingas.gateway.application.channel.dto;

import lombok.Data;

import java.time.Instant;
import java.util.List;

/**
 * 渠道响应
 */
@Data
public class ChannelResponse {

    private Long id;

    private Long providerId;

    private String providerName;

    private String name;

    private String billingMode;

    private Long quotaLimit;

    private Integer priority;

    private Integer weight;

    private Integer timeout;

    private Integer maxRetries;

    private String state;

    private List<ChannelEndpointResponse> endpoints;

    private Instant createdAt;

    private Instant updatedAt;
}
```

- [ ] **Step 3: 创建 ChannelEndpointRequest**

```java
package com.codingas.gateway.application.channel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 渠道端点创建/更新请求
 */
@Data
public class ChannelEndpointRequest {

    @NotNull(message = "协议类型不能为空")
    private String protocol;

    @NotBlank(message = "端点 URL 不能为空")
    private String endpointUrl;
}
```

- [ ] **Step 4: 创建 ChannelEndpointResponse**

```java
package com.codingas.gateway.application.channel.dto;

import lombok.Data;

import java.time.Instant;

/**
 * 渠道端点响应
 */
@Data
public class ChannelEndpointResponse {

    private Long id;

    private Long channelId;

    private String protocol;

    private String endpointUrl;

    private String state;

    private Instant createdAt;

    private Instant updatedAt;
}
```

- [ ] **Step 5: 更新 ChannelServiceImpl — 适配 Channel 字段变更，增加端点管理方法**

在 `ChannelServiceImpl` 中：
1. 注入 `ChannelEndpointGateway`
2. `create` 方法不再设置 endpointUrl/protocol
3. `update` 方法不再设置 endpointUrl/protocol
4. `toResponse` 方法改为查询渠道下的端点列表，设置到 `ChannelResponse.endpoints`
5. 新增 `addEndpoint`、`removeEndpoint`、`enableEndpoint`、`disableEndpoint` 方法

关键更新：
```java
    private final ChannelEndpointGateway channelEndpointGateway;

    // toResponse 方法更新
    private ChannelResponse toResponse(Channel channel) {
        ChannelResponse response = new ChannelResponse();
        response.setId(channel.getId());
        response.setProviderId(channel.getProviderId());
        providerGateway.findById(channel.getProviderId())
            .ifPresent(p -> response.setProviderName(p.getName()));
        response.setName(channel.getName());
        response.setBillingMode(channel.getBillingMode().getCode());
        response.setQuotaLimit(channel.getQuotaLimit());
        response.setPriority(channel.getPriority());
        response.setWeight(channel.getWeight());
        response.setTimeout(channel.getTimeout());
        response.setMaxRetries(channel.getMaxRetries());
        response.setState(channel.getState().getCode());
        // 查询端点列表
        response.setEndpoints(
            channelEndpointGateway.findByChannelId(channel.getId()).stream()
                .map(this::toEndpointResponse)
                .toList()
        );
        response.setCreatedAt(channel.getCreatedAt());
        response.setUpdatedAt(channel.getUpdatedAt());
        return response;
    }

    private ChannelEndpointResponse toEndpointResponse(ChannelEndpoint endpoint) {
        ChannelEndpointResponse resp = new ChannelEndpointResponse();
        resp.setId(endpoint.getId());
        resp.setChannelId(endpoint.getChannelId());
        resp.setProtocol(endpoint.getProtocol().getCode());
        resp.setEndpointUrl(endpoint.getEndpointUrl());
        resp.setState(endpoint.getState().getCode());
        resp.setCreatedAt(endpoint.getCreatedAt());
        resp.setUpdatedAt(endpoint.getUpdatedAt());
        return resp;
    }

    /**
     * 添加渠道端点
     */
    @Transactional
    public ChannelEndpointResponse addEndpoint(Long channelId, ChannelEndpointRequest request) {
        Channel channel = channelGateway.findById(channelId)
                .orElseThrow(() -> new IllegalArgumentException("渠道不存在: " + channelId));

        Protocol protocol = Protocol.fromCode(request.getProtocol());
        if (channelEndpointGateway.findByChannelIdAndProtocol(channelId, protocol).isPresent()) {
            throw new IllegalArgumentException("渠道下已存在该协议端点: " + request.getProtocol());
        }

        ChannelEndpoint endpoint = new ChannelEndpoint();
        endpoint.setChannelId(channelId);
        endpoint.setProtocol(protocol);
        endpoint.setEndpointUrl(request.getEndpointUrl());
        endpoint.setState(ChannelEndpointState.ACTIVE);

        ChannelEndpoint saved = channelEndpointGateway.save(endpoint);
        log.info("Added endpoint to channel: channelId={}, endpointId={}, protocol={}",
                channelId, saved.getId(), protocol);
        return toEndpointResponse(saved);
    }

    /**
     * 删除渠道端点
     */
    @Transactional
    public void removeEndpoint(Long channelId, Long endpointId) {
        ChannelEndpoint endpoint = channelEndpointGateway.findById(endpointId)
                .orElseThrow(() -> new IllegalArgumentException("端点不存在: " + endpointId));
        if (!endpoint.getChannelId().equals(channelId)) {
            throw new IllegalArgumentException("端点不属于该渠道");
        }
        channelEndpointGateway.deleteById(endpointId);
        log.info("Removed endpoint: channelId={}, endpointId={}", channelId, endpointId);
    }

    /**
     * 启用渠道端点
     */
    @Transactional
    public ChannelEndpointResponse enableEndpoint(Long channelId, Long endpointId) {
        ChannelEndpoint endpoint = channelEndpointGateway.findById(endpointId)
                .orElseThrow(() -> new IllegalArgumentException("端点不存在: " + endpointId));
        if (!endpoint.getChannelId().equals(channelId)) {
            throw new IllegalArgumentException("端点不属于该渠道");
        }
        endpoint.enable();
        ChannelEndpoint saved = channelEndpointGateway.save(endpoint);
        return toEndpointResponse(saved);
    }

    /**
     * 禁用渠道端点
     */
    @Transactional
    public ChannelEndpointResponse disableEndpoint(Long channelId, Long endpointId) {
        ChannelEndpoint endpoint = channelEndpointGateway.findById(endpointId)
                .orElseThrow(() -> new IllegalArgumentException("端点不存在: " + endpointId));
        if (!endpoint.getChannelId().equals(channelId)) {
            throw new IllegalArgumentException("端点不属于该渠道");
        }
        endpoint.disable();
        ChannelEndpoint saved = channelEndpointGateway.save(endpoint);
        return toEndpointResponse(saved);
    }
```

- [ ] **Step 6: 更新 ChannelService 接口**

新增端点管理方法签名：
```java
    ChannelEndpointResponse addEndpoint(Long channelId, ChannelEndpointRequest request);
    void removeEndpoint(Long channelId, Long endpointId);
    ChannelEndpointResponse enableEndpoint(Long channelId, Long endpointId);
    ChannelEndpointResponse disableEndpoint(Long channelId, Long endpointId);
```

- [ ] **Step 7: 编译确认**

Run: `./mvnw compile -pl gateway-boot -q`
Expected: BUILD SUCCESS

- [ ] **Step 8: 提交**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/application/channel/
git commit -m "feat(supply): Channel DTO/Service 适配 ChannelEndpoint，新增端点管理方法"
```

---

### Task 11: 更新 ChannelController — 新增端点管理 API

**Files:**
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/adapter/api/ChannelController.java`

- [ ] **Step 1: 更新 ChannelController — 新增端点管理端点**

```java
package com.codingas.gateway.adapter.api;

import com.codingas.gateway.application.channel.ChannelService;
import com.codingas.gateway.application.channel.dto.*;
import com.codingas.gateway.domain.supply.enums.BillingMode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 渠道管理 REST 控制器
 */
@RestController
@RequestMapping("/api/v1/channels")
@RequiredArgsConstructor
public class ChannelController {

    private final ChannelService channelService;

    @PostMapping
    public ResponseEntity<ChannelResponse> create(@Valid @RequestBody ChannelRequest request) {
        ChannelResponse response = channelService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ChannelResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody ChannelRequest request) {
        ChannelResponse response = channelService.update(id, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ChannelResponse> getById(@PathVariable Long id) {
        ChannelResponse response = channelService.getById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<ChannelResponse>> getByProviderId(
            @RequestParam Long providerId,
            @RequestParam(required = false) String billingMode) {
        List<ChannelResponse> responses;
        if (billingMode != null) {
            responses = channelService.getByProviderIdAndBillingMode(
                providerId, BillingMode.fromCode(billingMode));
        } else {
            responses = channelService.getByProviderId(providerId);
        }
        return ResponseEntity.ok(responses);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        channelService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ===== 端点管理 =====

    @PostMapping("/{channelId}/endpoints")
    public ResponseEntity<ChannelEndpointResponse> addEndpoint(
            @PathVariable Long channelId,
            @Valid @RequestBody ChannelEndpointRequest request) {
        ChannelEndpointResponse response = channelService.addEndpoint(channelId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{channelId}/endpoints/{endpointId}")
    public ResponseEntity<Void> removeEndpoint(
            @PathVariable Long channelId,
            @PathVariable Long endpointId) {
        channelService.removeEndpoint(channelId, endpointId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{channelId}/endpoints/{endpointId}/enable")
    public ResponseEntity<ChannelEndpointResponse> enableEndpoint(
            @PathVariable Long channelId,
            @PathVariable Long endpointId) {
        ChannelEndpointResponse response = channelService.enableEndpoint(channelId, endpointId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{channelId}/endpoints/{endpointId}/disable")
    public ResponseEntity<ChannelEndpointResponse> disableEndpoint(
            @PathVariable Long channelId,
            @PathVariable Long endpointId) {
        ChannelEndpointResponse response = channelService.disableEndpoint(channelId, endpointId);
        return ResponseEntity.ok(response);
    }
}
```

- [ ] **Step 2: 编译确认**

Run: `./mvnw compile -pl gateway-boot -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/adapter/api/ChannelController.java
git commit -m "feat(supply): ChannelController 新增端点管理 API（CRUD + 启用/禁用）"
```

---

### Task 12: 更新 DataInitializer — 适配 ChannelEndpoint 创建

**Files:**
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/application/init/DataInitializer.java`

- [ ] **Step 1: 更新 DataInitializer**

将 Channel 创建时的 endpointUrl/protocol 设置改为创建 ChannelEndpoint：

```java
package com.codingas.gateway.application.init;

import com.codingas.gateway.domain.supply.entity.Channel;
import com.codingas.gateway.domain.supply.entity.ChannelCredential;
import com.codingas.gateway.domain.supply.entity.ChannelEndpoint;
import com.codingas.gateway.domain.supply.entity.ModelSpec;
import com.codingas.gateway.domain.supply.entity.Provider;
import com.codingas.gateway.domain.supply.enums.BillingMode;
import com.codingas.gateway.domain.supply.enums.ChannelEndpointState;
import com.codingas.gateway.domain.supply.enums.ChannelState;
import com.codingas.gateway.domain.supply.enums.CredentialState;
import com.codingas.gateway.domain.supply.enums.ModelSpecState;
import com.codingas.gateway.domain.supply.enums.Protocol;
import com.codingas.gateway.domain.supply.enums.ProviderState;
import com.codingas.gateway.domain.supply.gateway.ChannelCredentialGateway;
import com.codingas.gateway.domain.supply.gateway.ChannelEndpointGateway;
import com.codingas.gateway.domain.supply.gateway.ChannelGateway;
import com.codingas.gateway.domain.supply.gateway.ModelSpecGateway;
import com.codingas.gateway.domain.supply.gateway.ProviderGateway;
import com.codingas.gateway.domain.iam.entity.UserApiKey;
import com.codingas.gateway.domain.iam.enums.UserApiKeyState;
import com.codingas.gateway.domain.iam.gateway.UserApiKeyGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 开发环境数据初始化器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final ProviderGateway providerGateway;
    private final ModelSpecGateway modelSpecGateway;
    private final ChannelGateway channelGateway;
    private final ChannelEndpointGateway channelEndpointGateway;
    private final ChannelCredentialGateway channelCredentialGateway;
    private final UserApiKeyGateway userApiKeyGateway;

    @Override
    @Transactional
    public void run(String... args) {
        if (providerGateway.count() > 0) {
            log.info("Data already initialized, skipping...");
            return;
        }

        log.info("Initializing development data...");

        // ===== 1. 创建 Provider =====
        Provider openai = createProvider("OpenAI", "https://api.openai.com");
        Provider anthropic = createProvider("Anthropic", "https://api.anthropic.com");
        Provider deepseek = createProvider("DeepSeek", "https://api.deepseek.com");

        // ===== 2. 创建 ModelSpec =====
        createModelSpec(openai.getId(), "gpt-4o", "GPT-4o", 128000);
        createModelSpec(openai.getId(), "gpt-4o-mini", "GPT-4o Mini", 128000);
        createModelSpec(openai.getId(), "gpt-3.5-turbo", "GPT-3.5 Turbo", 16385);
        createModelSpec(anthropic.getId(), "claude-sonnet-4-20250514", "Claude Sonnet 4", 200000);
        createModelSpec(anthropic.getId(), "claude-3-5-haiku-20241022", "Claude 3.5 Haiku", 200000);
        createModelSpec(deepseek.getId(), "deepseek-chat", "DeepSeek Chat", 64000);
        createModelSpec(deepseek.getId(), "deepseek-reasoner", "DeepSeek Reasoner", 64000);

        // ===== 3. 创建 Channel =====
        Channel openaiChannel = createChannel(openai.getId(), "OpenAI Standard");
        Channel anthropicChannel = createChannel(anthropic.getId(), "Anthropic Standard");
        Channel deepseekChannel = createChannel(deepseek.getId(), "DeepSeek Standard");

        // ===== 4. 创建 ChannelEndpoint =====
        createEndpoint(openaiChannel.getId(), Protocol.OPENAI, "https://api.openai.com");
        createEndpoint(anthropicChannel.getId(), Protocol.ANTHROPIC, "https://api.anthropic.com");
        createEndpoint(deepseekChannel.getId(), Protocol.OPENAI, "https://api.deepseek.com");

        // ===== 5. 创建 ChannelCredential =====
        createChannelCredential(openaiChannel.getId(), "sk-openai-dev-key-001");
        createChannelCredential(anthropicChannel.getId(), "sk-ant-anthropic-dev-key-001");
        createChannelCredential(deepseekChannel.getId(), "sk-deepseek-dev-key-001");

        // ===== 6. 创建 UserApiKey =====
        UserApiKey userApiKey = new UserApiKey();
        userApiKey.setName("开发测试密钥");
        userApiKey.setUserId(1L);
        userApiKey.setChannelIds(List.of(openaiChannel.getId(), anthropicChannel.getId(), deepseekChannel.getId()));
        userApiKey.setModels(null);
        userApiKeyGateway.save(userApiKey);

        log.info("Development data initialized successfully!");
        log.info("  Providers: 3 (OpenAI, Anthropic, DeepSeek)");
        log.info("  Models: 7");
        log.info("  Channels: 3");
        log.info("  ChannelEndpoints: 3");
        log.info("  UserApiKeys: 1");
    }

    private Provider createProvider(String name, String baseUrl) {
        Provider provider = new Provider();
        provider.setName(name);
        provider.setBaseUrl(baseUrl);
        provider.setState(ProviderState.ACTIVE);
        return providerGateway.save(provider);
    }

    private void createModelSpec(Long providerId, String providerModelId, String displayName, int contextWindow) {
        ModelSpec modelSpec = new ModelSpec();
        modelSpec.setProviderModelId(providerModelId);
        modelSpec.setDisplayName(displayName);
        modelSpec.setContextWindow(contextWindow);
        modelSpec.setState(ModelSpecState.ACTIVE);
        modelSpecGateway.save(modelSpec);
    }

    private Channel createChannel(Long providerId, String name) {
        Channel channel = new Channel();
        channel.setProviderId(providerId);
        channel.setName(name);
        channel.setBillingMode(BillingMode.PAY_AS_YOU_GO);
        channel.setState(ChannelState.ACTIVE);
        return channelGateway.save(channel);
    }

    private void createEndpoint(Long channelId, Protocol protocol, String url) {
        ChannelEndpoint endpoint = new ChannelEndpoint();
        endpoint.setChannelId(channelId);
        endpoint.setProtocol(protocol);
        endpoint.setEndpointUrl(url);
        endpoint.setState(ChannelEndpointState.ACTIVE);
        channelEndpointGateway.save(endpoint);
    }

    private void createChannelCredential(Long channelId, String plainApiKey) {
        ChannelCredential credential = new ChannelCredential();
        credential.setChannelId(channelId);
        credential.setApiKeyPlain(plainApiKey);
        credential.setApiKeyPrefix(plainApiKey.substring(0, Math.min(8, plainApiKey.length())));
        credential.setName("default");
        credential.setState(CredentialState.ACTIVE);
        channelCredentialGateway.save(credential);
    }
}
```

- [ ] **Step 2: 编译确认**

Run: `./mvnw compile -pl gateway-boot -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/application/init/DataInitializer.java
git commit -m "refactor(supply): DataInitializer 适配 ChannelEndpoint，不再在 Channel 上设置 endpointUrl/protocol"
```

---

### Task 13: Flyway 迁移 — 重写 V35 并创建 V37

**Files:**
- Modify: `gateway-boot/src/main/resources/db/migration/V35__supply_domain_refactor.sql`
- Create: `gateway-boot/src/main/resources/db/migration/V37__channel_endpoints.sql`

注意：V35 已存在但当前有 bug 导致应用无法启动。需要重写 V35 确保在 H2 和 PostgreSQL 上都能正确执行，然后 V37 处理 ChannelEndpoint 相关的 DDL 变更。

- [ ] **Step 1: 重写 V35 — 供给域表/列重命名**

```sql
-- ==========================================
-- V35: 供给域重构 — 表/列重命名
-- 注意：必须兼容 H2 和 PostgreSQL
-- ==========================================

-- 1. products → channels
ALTER TABLE products RENAME TO channels;

-- 2. product_api_keys → channel_credentials
ALTER TABLE product_api_keys RENAME TO channel_credentials;

-- 3. product_models → channel_models
ALTER TABLE product_models RENAME TO channel_models;

-- 4. models → model_specs
ALTER TABLE models RENAME TO model_specs;

-- 5. 渠道表列重命名
ALTER TABLE channels RENAME COLUMN type TO billing_mode;
ALTER TABLE channels RENAME COLUMN enabled TO state;
ALTER TABLE channels RENAME COLUMN api_key TO credential_preview;

-- 6. 渠道凭证表列重命名
ALTER TABLE channel_credentials RENAME COLUMN product_id TO channel_id;
ALTER TABLE channel_credentials RENAME COLUMN api_key TO encrypted_api_key;
ALTER TABLE channel_credentials RENAME COLUMN is_default TO is_default;
ALTER TABLE channel_credentials RENAME COLUMN enabled TO state;

-- 7. 渠道模型表列重命名
ALTER TABLE channel_models RENAME COLUMN product_id TO channel_id;
ALTER TABLE channel_models RENAME COLUMN model_id TO model_spec_id;

-- 8. 用户 API Key 关联列重命名
ALTER TABLE user_api_keys RENAME COLUMN product_ids TO channel_ids;
```

- [ ] **Step 2: 创建 V37 — channel_endpoints 表 + channels 列变更**

```sql
-- ==========================================
-- V37: ChannelEndpoint 实体
-- 1. 创建 channel_endpoints 表
-- 2. 迁移 channels 表的 endpoint_url/protocol 数据到 channel_endpoints
-- 3. 删除 channels 表的 endpoint_url/protocol 列
-- ==========================================

-- 1. 创建 channel_endpoints 表
CREATE TABLE channel_endpoints (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    channel_id BIGINT NOT NULL,
    protocol VARCHAR(32) NOT NULL,
    endpoint_url VARCHAR(512) NOT NULL,
    state VARCHAR(16) DEFAULT 'ACTIVE',
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_channel_endpoint UNIQUE (channel_id, protocol)
);

CREATE INDEX idx_channel_endpoints_channel ON channel_endpoints(channel_id);
CREATE INDEX idx_channel_endpoints_protocol ON channel_endpoints(protocol);

-- 2. 迁移现有数据
INSERT INTO channel_endpoints (channel_id, protocol, endpoint_url, state, created_at, updated_at)
SELECT id, protocol, endpoint_url, 'ACTIVE', created_at, updated_at
FROM channels
WHERE endpoint_url IS NOT NULL;

-- 3. 删除 channels 表的 endpoint_url 和 protocol 列
ALTER TABLE channels DROP COLUMN endpoint_url;
ALTER TABLE channels DROP COLUMN protocol;
```

- [ ] **Step 3: 删除 H2 缓存数据，启动验证**

Run: `rm -f gateway-boot/data/gateway.mv.db gateway-boot/data/gateway.trace.db 2>/dev/null; ./mvnw spring-boot:run -pl gateway-boot -q 2>&1 | head -50`

如果启动成功（看到 "Started" 日志），则确认通过。

- [ ] **Step 4: 提交**

```bash
git add gateway-boot/src/main/resources/db/migration/V35__supply_domain_refactor.sql \
       gateway-boot/src/main/resources/db/migration/V37__channel_endpoints.sql
git commit -m "feat(supply): 重写 V35 迁移 + 新增 V37 channel_endpoints 表和数据迁移"
```

---

### Task 14: 全量测试 + 启动验证

**Files:** 无新增/修改，仅验证

- [ ] **Step 1: 清理 H2 缓存**

Run: `rm -f gateway-boot/data/gateway.mv.db gateway-boot/data/gateway.trace.db 2>/dev/null`

- [ ] **Step 2: 全量测试**

Run: `./mvnw clean test -pl gateway-boot`
Expected: BUILD SUCCESS，所有测试通过

- [ ] **Step 3: 应用启动验证**

Run: `./mvnw spring-boot:run -pl gateway-boot -q 2>&1 | head -30`
Expected: 看到 "Started" 日志

- [ ] **Step 4: 提交（如有修复）**

```bash
git add -A && git commit -m "fix(supply): 全量测试和启动验证修复"
```

---

## 成功标准

1. 一个 Coding Plan 套餐只需创建一个 Channel + 两个 ChannelEndpoint，无凭证/模型冗余
2. 入站协议匹配时直接调用对应端点，无跨协议转换开销
3. 入站协议不匹配时自动降级到跨协议转换，不报错
4. 已有的单协议渠道迁移后功能不受影响
5. 全量测试通过，应用正常启动