---
change: key-application-binding
design-doc: docs/superpowers/specs/2026-07-06-key-application-binding-design.md
base-ref: aa8439cb7998b50631aafbb6aef8eb6ae8f372c0
---

# Key-Application Binding 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 补齐 spec 已规定但实现缺失的 UserApiKey.applicationId 管理面能力（创建必填、补绑/转移、响应字段、按应用查询），修复重置密码 404，前端对齐 Application 锚点模型，删除 Application 防悬空。

**Architecture:** 后端按 DTO → Gateway → Service → Controller 分层推进，Application 存在性校验放 Service 层注入 ApplicationGateway（findById 返回 null 表示不存在，沿用现有约定），删除冲突复用 GatewayRequestException（400 BAD_REQUEST，属 4xx Conflict 语义，不新建异常类）。前端按类型 → API → 页面组件 → 入口跳转推进。路由回归测试验证带 applicationId 的 Key 能解析出非空候选集，回归本次根因。

**Tech Stack:** Java 21 + Spring Boot 3.5.x + JPA + Mockito + JUnit 5；React + TypeScript + Ant Design + TanStack Query + Vite。

**Design Doc:** `docs/superpowers/specs/2026-07-06-key-application-binding-design.md`

**Tasks 清单:** `openspec/changes/key-application-binding/tasks.md`（7 组 22 任务）

---

## 关键设计决策（实施前必读）

1. **ApplicationGateway.findById 返回 `Application`（非 Optional）**，null 表示不存在。Design Doc 中 `orElseThrow(ResourceNotFound)` 的写法需调整为 `if (applicationGateway.findById(id) == null) throw`。沿用 `ApplicationServiceImpl` 现有风格：抛 `GatewayRequestException("APPLICATION_NOT_FOUND", "应用不存在: " + id)`。
2. **不新建 ConflictException**。`ApplicationServiceImpl.delete` 检测到 Key 引用时抛 `GatewayRequestException("APPLICATION_HAS_API_KEYS", "应用下还有 API Key，请先转移或删除")`，由 `GlobalExceptionHandler` 映射为 400 BAD_REQUEST（4xx）。前端通过 code 字段识别冲突。这与 Design Doc "4xx Conflict" 语义一致，避免扩大改动范围。
3. **UserApiKeyCreateRequest 加必填字段是 BREAKING**，前端唯一调用方需同步改造。无 CLI/外部调用方。
4. **重置密码返回明文**，用 record `ResetPasswordResponse(String newPassword)` 封装，不持久化明文，HTTPS 传输，与 UserApiKeyCreate 模式一致。
5. **存量 null Key 不自动迁移**，补绑入口上线后管理员手动处理。
6. **DTO record 字段顺序**：`applicationId` 统一插在 `userId` 之后（Response/DetailResponse）或 `userId` 之后（CreateRequest），`UpdateRequest` 为 `(applicationId, name)`。

---

## 文件结构总览

### 后端（gateway-boot）

| 文件 | 操作 | 职责 |
|------|------|------|
| `application/userapikey/dto/UserApiKeyCreateRequest.java` | 改 | 加 `@NotNull Long applicationId` |
| `application/userapikey/dto/UserApiKeyUpdateRequest.java` | 改 | 加可选 `Long applicationId` |
| `application/userapikey/dto/UserApiKeyResponse.java` | 改 | 加 `Long applicationId` |
| `application/userapikey/dto/UserApiKeyDetailResponse.java` | 改 | 加 `Long applicationId` |
| `application/userapikey/dto/UserApiKeyCreateResponse.java` | 不改 | 保持不变 |
| `application/userapikey/UserApiKeyService.java` | 改 | 加 `findByApplicationId(Long)` 方法 |
| `application/userapikey/UserApiKeyServiceImpl.java` | 改 | 注入 ApplicationGateway；create/update 校验+落库 applicationId；响应映射；加 findByApplicationId |
| `domain/iam/gateway/UserApiKeyGateway.java` | 改 | 加 `findByApplicationId(Long)` 接口方法 |
| `infrastructure/iam/gateway/UserApiKeyGatewayImpl.java` | 改 | 实现 findByApplicationId |
| `infrastructure/iam/gateway/database/repository/UserApiKeyRepository.java` | 改 | 加派生查询 `findByApplicationId` |
| `adapter/api/ApplicationController.java` | 改 | 注入 UserApiKeyService；加 GET /applications/{id}/api-keys |
| `application/application/ApplicationServiceImpl.java` | 改 | 注入 UserApiKeyGateway；delete 前置校验 |
| `adapter/api/UserController.java` | 改 | 加 POST /users/{id}/reset-password |
| `application/user/UserService.java` | 改 | 加 `resetPassword(Long)` 方法 |
| `application/user/UserServiceImpl.java` | 改 | 实现 resetPassword |
| `application/user/dto/ResetPasswordResponse.java` | 新建 | record 封装一次性明文 |
| `test/.../userapikey/UserApiKeyServiceImplTest.java` | 改 | 加 ApplicationGateway mock；更新所有 DTO 构造；加 applicationId 校验 case |
| `test/.../application/ApplicationServiceImplTest.java` | 新建 | delete 有 Key 冲突、无引用正常删 |
| `test/.../user/UserServiceImplTest.java` | 改/补 | resetPassword 16 位 + 内建拒绝 + 不存在 404 |
| `test/.../adapter/api/ApplicationControllerTest.java` | 改/补 | GET /applications/{id}/api-keys、DELETE 冲突 |
| `test/.../adapter/api/UserControllerTest.java` | 改/补 | reset-password 成功 + 内建拒绝 |
| `test/.../proxy/routing/RoutingResolverTest.java` | 改/补 | 带 applicationId 的路由返回非空候选集（回归根因） |

### 前端（gateway-console）

| 文件 | 操作 | 职责 |
|------|------|------|
| `types/userApiKey.ts` | 改 | UserApiKey/CreateUserApiKeyRequest/UpdateUserApiKeyRequest 加 applicationId |
| `services/api/userApiKey.ts` | 改 | 移除 rotate；加 listByApplication |
| `services/api/user.ts` | 改 | resetPassword 返回类型改为 `{ newPassword: string }` |
| `services/query/useUserApiKeys.ts` | 改 | 加 useUserApiKeysByApplication hook（可选，按应用查询） |
| `pages/ApiKeys/DownstreamKeysTable.tsx` | 改 | 创建表单加 Application Select；列表加列；顶部加筛选；URL ?applicationId= 初始化 |
| `pages/Users/UserApiKeyModal.tsx` | 改 | 删团队继承 Alert；加 Application Select；编辑补绑 |
| `pages/Applications/index.tsx` | 改 | 行操作加查看 Key |

---

## Task 1: 后端 DTO 扩展 applicationId 字段

**Files:**
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/application/userapikey/dto/UserApiKeyCreateRequest.java`
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/application/userapikey/dto/UserApiKeyUpdateRequest.java`
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/application/userapikey/dto/UserApiKeyResponse.java`
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/application/userapikey/dto/UserApiKeyDetailResponse.java`

- [x] **Step 1: 改 UserApiKeyCreateRequest，加 applicationId 字段**

```java
package com.codingas.gateway.application.userapikey.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 创建用户 API Key 请求
 *
 * @param userId        用户 ID
 * @param applicationId 应用 ID（权限锚点，创建时必填）
 * @param name          密钥名称
 */
public record UserApiKeyCreateRequest(
        @NotNull(message = "用户 ID 不能为空")
        Long userId,
        @NotNull(message = "应用 ID 不能为空")
        Long applicationId,
        @NotBlank(message = "密钥名称不能为空")
        String name
) {
}
```

- [x] **Step 2: 改 UserApiKeyUpdateRequest，加可选 applicationId**

```java
package com.codingas.gateway.application.userapikey.dto;

/**
 * 更新用户 API Key 请求
 *
 * @param applicationId 应用 ID（可选，非 null 时表示补绑/转移）
 * @param name          密钥名称（可选）
 */
public record UserApiKeyUpdateRequest(
        Long applicationId,
        String name
) {
}
```

- [x] **Step 3: 改 UserApiKeyResponse，加 applicationId 字段**

```java
package com.codingas.gateway.application.userapikey.dto;

import java.time.Instant;

/**
 * 用户 API Key 响应
 *
 * @param id            主键
 * @param userId        用户 ID
 * @param applicationId 应用 ID（权限锚点）
 * @param keyPrefix     Key 前缀
 * @param keyPlain      明文 Key（仅创建/详情返回）
 * @param name          密钥名称
 * @param createdAt     创建时间
 * @param updatedAt     更新时间
 */
public record UserApiKeyResponse(
        Long id,
        Long userId,
        Long applicationId,
        String keyPrefix,
        String keyPlain,
        String name,
        Instant createdAt,
        Instant updatedAt
) {
}
```

- [x] **Step 4: 改 UserApiKeyDetailResponse，加 applicationId 字段**

```java
package com.codingas.gateway.application.userapikey.dto;

import java.time.Instant;

/**
 * 用户 API Key 详情响应（含明文 Key，仅创建时和详情页返回）
 *
 * @param id            主键
 * @param userId        用户 ID
 * @param applicationId 应用 ID（权限锚点）
 * @param keyPrefix     Key 前缀
 * @param keyPlain      明文 Key
 * @param name          密钥名称
 * @param createdAt     创建时间
 * @param updatedAt     更新时间
 */
public record UserApiKeyDetailResponse(
        Long id,
        Long userId,
        Long applicationId,
        String keyPrefix,
        String keyPlain,
        String name,
        Instant createdAt,
        Instant updatedAt
) {
}
```

- [x] **Step 5: 编译验证 DTO 改动不破坏 record 结构**

Run: `./mvnw -pl gateway-boot compile -q`
Expected: 编译失败，提示 `UserApiKeyServiceImpl` 中 `toResponse/toDetailResponse` 缺参数、`UserApiKeyServiceImplTest` 中 `new UserApiKeyCreateRequest(...)` 参数数量不匹配。这是预期的——后续 Task 3/4 会修复。仅确认 DTO 本身无语法错误。

- [x] **Step 6: 提交 DTO 改动**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/application/userapikey/dto/
git commit -m "feat(user-api-key): DTO 扩展 applicationId 字段

- CreateRequest 加 @NotNull applicationId（BREAKING）
- UpdateRequest 加可选 applicationId（补绑/转移）
- Response/DetailResponse 加 applicationId 字段

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

## Task 2: 后端 Gateway 加 findByApplicationId

**Files:**
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/domain/iam/gateway/UserApiKeyGateway.java`
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/iam/gateway/database/repository/UserApiKeyRepository.java`
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/iam/gateway/UserApiKeyGatewayImpl.java`

- [x] **Step 1: UserApiKeyGateway 接口加方法**

在 `UserApiKeyGateway.java` 的 `findByUserId` 后加：

```java
    /** 按应用 ID 查找（管理面：应用详情页查看其下 Key） */
    List<UserApiKey> findByApplicationId(Long applicationId);
```

- [x] **Step 2: UserApiKeyRepository 加派生查询**

在 `UserApiKeyRepository.java` 加方法（参照现有 `findByUserId` 的 @Query 风格，过滤 deleted=false）：

```java
    @Query("SELECT u FROM UserApiKeyDo u WHERE u.applicationId = :applicationId AND u.deleted = false")
    List<UserApiKeyDo> findByApplicationId(@Param("applicationId") Long applicationId);
```

注：`import org.springframework.data.repository.query.Param;` 已存在。

- [x] **Step 3: UserApiKeyGatewayImpl 实现 findByApplicationId**

在 `UserApiKeyGatewayImpl.java` 的 `findByUserId` 后加：

```java
    @Override
    public List<UserApiKey> findByApplicationId(Long applicationId) {
        return repository.findByApplicationId(applicationId).stream()
                .map(this::toEntity)
                .toList();
    }
```

注：`toEntity/toDataObject` 已映射 applicationId（line 99/124），无需改。

- [x] **Step 4: 编译验证**

Run: `./mvnw -pl gateway-boot compile -q`
Expected: Gateway/Repository/Impl 编译通过（Service 层尚未调用新方法，不影响）。

- [x] **Step 5: 提交**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/domain/iam/gateway/UserApiKeyGateway.java \
        gateway-boot/src/main/java/com/codingas/gateway/infrastructure/iam/gateway/database/repository/UserApiKeyRepository.java \
        gateway-boot/src/main/java/com/codingas/gateway/infrastructure/iam/gateway/UserApiKeyGatewayImpl.java
git commit -m "feat(user-api-key): Gateway 加 findByApplicationId 查询

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

## Task 3: 后端 UserApiKeyServiceImpl 校验与映射（TDD）

**Files:**
- Modify: `gateway-boot/src/test/java/com/codingas/gateway/application/userapikey/UserApiKeyServiceImplTest.java`
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/application/userapikey/UserApiKeyService.java`
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/application/userapikey/UserApiKeyServiceImpl.java`

- [x] **Step 1: 先写失败测试 — 更新 UserApiKeyServiceImplTest**

替换 `gateway-boot/src/test/java/com/codingas/gateway/application/userapikey/UserApiKeyServiceImplTest.java` 全文为：

```java
package com.codingas.gateway.application.userapikey;

import com.codingas.gateway.application.userapikey.dto.*;
import com.codingas.gateway.domain.application.entity.Application;
import com.codingas.gateway.domain.application.gateway.ApplicationGateway;
import com.codingas.gateway.domain.iam.service.UserApiKeyGenerator;
import com.codingas.gateway.domain.iam.service.GeneratedApiKey;
import com.codingas.gateway.domain.iam.entity.UserApiKey;
import com.codingas.gateway.domain.iam.gateway.UserApiKeyGateway;
import com.codingas.gateway.common.exception.GatewayRequestException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * UserApiKeyServiceImpl 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserApiKeyServiceImpl 测试")
class UserApiKeyServiceImplTest {

    @Mock
    private UserApiKeyGateway userApiKeyGateway;

    @Mock
    private UserApiKeyGenerator userApiKeyGenerator;

    @Mock
    private ApplicationGateway applicationGateway;

    @InjectMocks
    private UserApiKeyServiceImpl service;

    private static final Long USER_ID = 50L;
    private static final Long APPLICATION_ID = 7L;
    private static final Long API_KEY_ID = 100L;

    @Nested
    @DisplayName("create 方法测试")
    class CreateTests {

        @Test
        @DisplayName("创建密钥成功 — applicationId 落库")
        void create_success_setsApplicationId() {
            GeneratedApiKey generated = new GeneratedApiKey("sk-abc1xxxxx", "sk-abc1");
            when(userApiKeyGenerator.generate()).thenReturn(generated);
            Application app = new Application();
            app.setId(APPLICATION_ID);
            when(applicationGateway.findById(APPLICATION_ID)).thenReturn(app);

            UserApiKey saved = createSampleApiKey();
            saved.setApplicationId(APPLICATION_ID);
            when(userApiKeyGateway.save(any(UserApiKey.class))).thenReturn(saved);

            UserApiKeyCreateRequest request = new UserApiKeyCreateRequest(
                    USER_ID, APPLICATION_ID, "test-key"
            );
            UserApiKeyCreateResponse response = service.create(request);

            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(API_KEY_ID);
            assertThat(response.apiKeyPlain()).startsWith("sk-");
            verify(userApiKeyGateway).save(argThat(key ->
                    key.getUserId().equals(USER_ID)
                            && key.getApplicationId().equals(APPLICATION_ID)
            ));
        }

        @Test
        @DisplayName("applicationId 引用不存在 — 抛 GatewayRequestException")
        void create_applicationNotFound_throws() {
            when(applicationGateway.findById(APPLICATION_ID)).thenReturn(null);

            UserApiKeyCreateRequest request = new UserApiKeyCreateRequest(
                    USER_ID, APPLICATION_ID, "test-key"
            );
            assertThatThrownBy(() -> service.create(request))
                    .isInstanceOf(GatewayRequestException.class)
                    .hasMessageContaining("应用不存在");
            verify(userApiKeyGateway, never()).save(any());
        }

        @Test
        @DisplayName("ApiKeyGenerator 碰撞超限抛异常时，create 也抛异常")
        void create_generatorFails_throwsException() {
            Application app = new Application();
            app.setId(APPLICATION_ID);
            when(applicationGateway.findById(APPLICATION_ID)).thenReturn(app);
            when(userApiKeyGenerator.generate())
                    .thenThrow(new IllegalStateException("无法生成唯一的 API Key，请重试"));

            UserApiKeyCreateRequest request = new UserApiKeyCreateRequest(
                    USER_ID, APPLICATION_ID, "test-key"
            );
            assertThatThrownBy(() -> service.create(request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("无法生成唯一的 API Key");
        }
    }

    @Nested
    @DisplayName("update 方法测试")
    class UpdateTests {

        @Test
        @DisplayName("补绑 applicationId — 校验存在并落库")
        void update_rebindApplicationId() {
            UserApiKey apiKey = createSampleApiKey();
            when(userApiKeyGateway.findById(API_KEY_ID)).thenReturn(Optional.of(apiKey));
            Application app = new Application();
            app.setId(99L);
            when(applicationGateway.findById(99L)).thenReturn(app);
            when(userApiKeyGateway.save(any(UserApiKey.class))).thenAnswer(inv -> inv.getArgument(0));

            UserApiKeyUpdateRequest request = new UserApiKeyUpdateRequest(99L, null);
            UserApiKeyResponse response = service.update(API_KEY_ID, request);

            assertThat(response).isNotNull();
            assertThat(response.applicationId()).isEqualTo(99L);
            verify(userApiKeyGateway).save(argThat(key -> key.getApplicationId().equals(99L)));
        }

        @Test
        @DisplayName("补绑 applicationId 引用不存在 — 抛 GatewayRequestException")
        void update_applicationNotFound_throws() {
            UserApiKey apiKey = createSampleApiKey();
            when(userApiKeyGateway.findById(API_KEY_ID)).thenReturn(Optional.of(apiKey));
            when(applicationGateway.findById(99L)).thenReturn(null);

            UserApiKeyUpdateRequest request = new UserApiKeyUpdateRequest(99L, null);
            assertThatThrownBy(() -> service.update(API_KEY_ID, request))
                    .isInstanceOf(GatewayRequestException.class)
                    .hasMessageContaining("应用不存在");
        }

        @Test
        @DisplayName("仅更新名称 — applicationId 不变")
        void update_nameOnly() {
            UserApiKey apiKey = createSampleApiKey();
            apiKey.setApplicationId(APPLICATION_ID);
            when(userApiKeyGateway.findById(API_KEY_ID)).thenReturn(Optional.of(apiKey));
            when(userApiKeyGateway.save(any(UserApiKey.class))).thenAnswer(inv -> inv.getArgument(0));

            UserApiKeyUpdateRequest request = new UserApiKeyUpdateRequest(null, "new-name");
            UserApiKeyResponse response = service.update(API_KEY_ID, request);

            assertThat(response.name()).isEqualTo("new-name");
            assertThat(response.applicationId()).isEqualTo(APPLICATION_ID);
        }

        @Test
        @DisplayName("密钥不存在 — 抛异常")
        void update_notFound() {
            when(userApiKeyGateway.findById(API_KEY_ID)).thenReturn(Optional.empty());

            UserApiKeyUpdateRequest request = new UserApiKeyUpdateRequest(null, "updated");
            assertThatThrownBy(() -> service.update(API_KEY_ID, request))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("findByApplicationId 方法测试")
    class FindByApplicationIdTests {

        @Test
        @DisplayName("查询应用下的所有 Key")
        void findByApplicationId_success() {
            UserApiKey apiKey = createSampleApiKey();
            apiKey.setApplicationId(APPLICATION_ID);
            when(userApiKeyGateway.findByApplicationId(APPLICATION_ID)).thenReturn(List.of(apiKey));

            List<UserApiKeyResponse> responses = service.findByApplicationId(APPLICATION_ID);

            assertThat(responses).hasSize(1);
            assertThat(responses.get(0).applicationId()).isEqualTo(APPLICATION_ID);
        }

        @Test
        @DisplayName("应用下无 Key 返回空列表")
        void findByApplicationId_empty() {
            when(userApiKeyGateway.findByApplicationId(APPLICATION_ID)).thenReturn(List.of());

            List<UserApiKeyResponse> responses = service.findByApplicationId(APPLICATION_ID);

            assertThat(responses).isEmpty();
        }
    }

    @Nested
    @DisplayName("响应映射测试")
    class ResponseMappingTests {

        @Test
        @DisplayName("toResponse 含 applicationId")
        void getById_responseContainsApplicationId() {
            UserApiKey apiKey = createSampleApiKey();
            apiKey.setApplicationId(APPLICATION_ID);
            when(userApiKeyGateway.findById(API_KEY_ID)).thenReturn(Optional.of(apiKey));

            UserApiKeyResponse response = service.getById(API_KEY_ID);

            assertThat(response.applicationId()).isEqualTo(APPLICATION_ID);
        }
    }

    private UserApiKey createSampleApiKey() {
        UserApiKey apiKey = new UserApiKey();
        apiKey.setId(API_KEY_ID);
        apiKey.setUserId(USER_ID);
        apiKey.setKeyPlain("sk-abc1xxxxx");
        apiKey.setKeyPrefix("sk-abc1");
        apiKey.setName("test-key");
        return apiKey;
    }
}
```

- [x] **Step 2: 运行测试验证失败**

Run: `./mvnw -pl gateway-boot test -Dtest=UserApiKeyServiceImplTest -q`
Expected: 编译失败（`UserApiKeyCreateRequest` 构造参数不匹配、`UserApiKeyService` 无 `findByApplicationId` 方法、`UserApiKeyResponse` 无 `applicationId` 访问器、`UserApiKeyServiceImpl` 未注入 `ApplicationGateway`）。这是 Red 阶段。

- [x] **Step 3: UserApiKeyService 接口加 findByApplicationId**

在 `UserApiKeyService.java` 的 `findAllNonDeleted` 后加：

```java
    /** 按应用 ID 查询 Key（管理面：应用详情页查看其下 Key） */
    List<UserApiKeyResponse> findByApplicationId(Long applicationId);
```

- [x] **Step 4: UserApiKeyServiceImpl 注入 ApplicationGateway + 实现校验/映射/查询**

替换 `UserApiKeyServiceImpl.java` 全文为：

```java
package com.codingas.gateway.application.userapikey;

import com.codingas.gateway.application.userapikey.dto.UserApiKeyCreateRequest;
import com.codingas.gateway.application.userapikey.dto.UserApiKeyCreateResponse;
import com.codingas.gateway.application.userapikey.dto.UserApiKeyDetailResponse;
import com.codingas.gateway.application.userapikey.dto.UserApiKeyResponse;
import com.codingas.gateway.application.userapikey.dto.UserApiKeyUpdateRequest;
import com.codingas.gateway.common.exception.GatewayRequestException;
import com.codingas.gateway.domain.application.gateway.ApplicationGateway;
import com.codingas.gateway.domain.iam.service.GeneratedApiKey;
import com.codingas.gateway.domain.iam.service.UserApiKeyGenerator;
import com.codingas.gateway.domain.iam.entity.UserApiKey;
import com.codingas.gateway.domain.iam.gateway.UserApiKeyGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 用户 API Key 应用服务实现
 *
 * <p>applicationId 为权限锚点：create 必填并校验 Application 存在；
 * update 支持补绑/转移（非 null 时校验存在）。</p>
 */
@Service
public class UserApiKeyServiceImpl implements UserApiKeyService {

    private static final Logger log = LoggerFactory.getLogger(UserApiKeyServiceImpl.class);

    private final UserApiKeyGateway userApiKeyGateway;
    private final UserApiKeyGenerator userApiKeyGenerator;
    private final ApplicationGateway applicationGateway;

    public UserApiKeyServiceImpl(UserApiKeyGateway userApiKeyGateway,
                                 UserApiKeyGenerator userApiKeyGenerator,
                                 ApplicationGateway applicationGateway) {
        this.userApiKeyGateway = userApiKeyGateway;
        this.userApiKeyGenerator = userApiKeyGenerator;
        this.applicationGateway = applicationGateway;
    }

    @Override
    @Transactional
    public UserApiKeyCreateResponse create(UserApiKeyCreateRequest request) {
        // 校验 Application 存在（applicationId 为权限锚点，引用必须有效）
        validateApplicationExists(request.applicationId());

        GeneratedApiKey generated = userApiKeyGenerator.generate();

        UserApiKey apiKey = new UserApiKey();
        apiKey.setUserId(request.userId());
        apiKey.setApplicationId(request.applicationId());
        apiKey.setKeyPrefix(generated.keyPrefix());
        apiKey.setKeyPlain(generated.plainKey());
        apiKey.setName(request.name());

        UserApiKey saved = userApiKeyGateway.save(apiKey);
        log.info("Created UserApiKey: id={}, userId={}, applicationId={}",
                saved.getId(), saved.getUserId(), saved.getApplicationId());

        return new UserApiKeyCreateResponse(saved.getId(), generated.keyPrefix(), generated.plainKey());
    }

    @Override
    public List<UserApiKeyResponse> findByUserId(Long userId) {
        return userApiKeyGateway.findByUserId(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public List<UserApiKeyResponse> findAllNonDeleted() {
        return userApiKeyGateway.findAllNonDeleted().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public List<UserApiKeyResponse> findByApplicationId(Long applicationId) {
        return userApiKeyGateway.findByApplicationId(applicationId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public UserApiKeyResponse getById(Long id) {
        UserApiKey apiKey = userApiKeyGateway.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("API Key 不存在: " + id));
        return toResponse(apiKey);
    }

    @Override
    public UserApiKeyDetailResponse getDetailById(Long id) {
        UserApiKey apiKey = userApiKeyGateway.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("API Key 不存在: " + id));
        return toDetailResponse(apiKey);
    }

    @Override
    @Transactional
    public UserApiKeyResponse update(Long id, UserApiKeyUpdateRequest request) {
        UserApiKey apiKey = userApiKeyGateway.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("API Key 不存在: " + id));

        // 补绑/转移 applicationId（非 null 时校验存在）
        if (request.applicationId() != null) {
            validateApplicationExists(request.applicationId());
            apiKey.setApplicationId(request.applicationId());
        }
        if (request.name() != null) {
            apiKey.setName(request.name());
        }

        UserApiKey saved = userApiKeyGateway.save(apiKey);
        log.info("Updated UserApiKey: id={}, applicationId={}", saved.getId(), saved.getApplicationId());
        return toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        UserApiKey apiKey = userApiKeyGateway.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("API Key 不存在: " + id));
        userApiKeyGateway.delete(apiKey);
        log.info("Deleted UserApiKey: id={}", id);
    }

    /**
     * 校验 Application 存在
     *
     * <p>ApplicationGateway.findById 返回 null 表示不存在（沿用现有约定）。</p>
     *
     * @param applicationId 应用 ID
     * @throws GatewayRequestException 应用不存在时抛 APPLICATION_NOT_FOUND
     */
    private void validateApplicationExists(Long applicationId) {
        if (applicationGateway.findById(applicationId) == null) {
            throw new GatewayRequestException("APPLICATION_NOT_FOUND", "应用不存在: " + applicationId);
        }
    }

    private UserApiKeyResponse toResponse(UserApiKey apiKey) {
        return new UserApiKeyResponse(
                apiKey.getId(),
                apiKey.getUserId(),
                apiKey.getApplicationId(),
                apiKey.getKeyPrefix(),
                apiKey.getKeyPlain(),
                apiKey.getName(),
                apiKey.getCreatedAt(),
                apiKey.getUpdatedAt()
        );
    }

    private UserApiKeyDetailResponse toDetailResponse(UserApiKey apiKey) {
        return new UserApiKeyDetailResponse(
                apiKey.getId(),
                apiKey.getUserId(),
                apiKey.getApplicationId(),
                apiKey.getKeyPrefix(),
                apiKey.getKeyPlain(),
                apiKey.getName(),
                apiKey.getCreatedAt(),
                apiKey.getUpdatedAt()
        );
    }
}
```

- [x] **Step 5: 运行测试验证通过**

Run: `./mvnw -pl gateway-boot test -Dtest=UserApiKeyServiceImplTest -q`
Expected: 所有测试 PASS（Green 阶段）。

- [x] **Step 6: 提交**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/application/userapikey/ \
        gateway-boot/src/test/java/com/codingas/gateway/application/userapikey/UserApiKeyServiceImplTest.java
git commit -m "feat(user-api-key): Service 层校验 applicationId 引用并映射响应

- 注入 ApplicationGateway，create/update 校验 Application 存在
- toResponse/toDetailResponse 映射 applicationId
- 加 findByApplicationId 实现
- 单测覆盖：创建落库、引用不存在被拒、补绑转移、响应含 applicationId

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

## Task 4: 后端 ApplicationServiceImpl.delete 前置校验（TDD）

**Files:**
- Create: `gateway-boot/src/test/java/com/codingas/gateway/application/application/ApplicationServiceImplTest.java`
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/application/application/ApplicationServiceImpl.java`

- [x] **Step 1: 先写失败测试 — 新建 ApplicationServiceImplTest**

```java
package com.codingas.gateway.application.application;

import com.codingas.gateway.application.application.dto.ApplicationRequest;
import com.codingas.gateway.common.exception.GatewayRequestException;
import com.codingas.gateway.domain.application.entity.Application;
import com.codingas.gateway.domain.application.entity.ApplicationChannel;
import com.codingas.gateway.domain.application.entity.ApplicationState;
import com.codingas.gateway.domain.application.gateway.ApplicationChannelGateway;
import com.codingas.gateway.domain.application.gateway.ApplicationGateway;
import com.codingas.gateway.domain.iam.entity.UserApiKey;
import com.codingas.gateway.domain.iam.gateway.UserApiKeyGateway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * ApplicationServiceImpl 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ApplicationServiceImpl 测试")
class ApplicationServiceImplTest {

    @Mock
    private ApplicationGateway applicationGateway;

    @Mock
    private ApplicationChannelGateway applicationChannelGateway;

    @Mock
    private UserApiKeyGateway userApiKeyGateway;

    @InjectMocks
    private ApplicationServiceImpl service;

    private static final Long APP_ID = 7L;

    @Nested
    @DisplayName("delete 方法测试")
    class DeleteTests {

        @Test
        @DisplayName("应用下有 Key 引用 — 抛 GatewayRequestException(APPLICATION_HAS_API_KEYS)")
        void delete_hasApiKeys_throwsConflict() {
            UserApiKey key = new UserApiKey();
            key.setId(100L);
            key.setApplicationId(APP_ID);
            when(userApiKeyGateway.findByApplicationId(APP_ID)).thenReturn(List.of(key));

            assertThatThrownBy(() -> service.delete(APP_ID))
                    .isInstanceOf(GatewayRequestException.class)
                    .hasMessageContaining("API Key");
            verify(applicationGateway, never()).deleteById(any());
            verify(applicationChannelGateway, never()).deleteByApplicationId(any());
        }

        @Test
        @DisplayName("应用下无 Key 引用 — 正常删除（级联清理渠道授权）")
        void delete_noApiKeys_deletesCascade() {
            when(userApiKeyGateway.findByApplicationId(APP_ID)).thenReturn(List.of());

            assertThatCode(() -> service.delete(APP_ID)).doesNotThrowAnyException();

            verify(applicationChannelGateway).deleteByApplicationId(APP_ID);
            verify(applicationGateway).deleteById(APP_ID);
        }
    }
}
```

- [x] **Step 2: 运行测试验证失败**

Run: `./mvnw -pl gateway-boot test -Dtest=ApplicationServiceImplTest -q`
Expected: 失败——`ApplicationServiceImpl` 未注入 `UserApiKeyGateway`，`@InjectMocks` 无法注入，`delete` 不校验直接删。Red 阶段。

- [x] **Step 3: ApplicationServiceImpl 注入 UserApiKeyGateway + delete 前置校验**

修改 `ApplicationServiceImpl.java`：

3a. 加 import：

```java
import com.codingas.gateway.domain.iam.gateway.UserApiKeyGateway;
```

3b. 加字段（在 `applicationChannelGateway` 后）：

```java
    private final UserApiKeyGateway userApiKeyGateway;
```

3c. 替换 `delete` 方法为：

```java
    @Override
    @Transactional
    public void delete(Long id) {
        // 前置校验：应用下有 API Key 引用时拒绝删除，避免悬空引用
        if (!userApiKeyGateway.findByApplicationId(id).isEmpty()) {
            throw new GatewayRequestException("APPLICATION_HAS_API_KEYS",
                    "应用下还有 API Key，请先转移或删除");
        }
        // 级联清理渠道授权关联，避免孤儿数据
        applicationChannelGateway.deleteByApplicationId(id);
        applicationGateway.deleteById(id);
        log.info("Deleted application: id={}", id);
    }
```

注：`@RequiredArgsConstructor` 会自动注入新字段。

- [x] **Step 4: 运行测试验证通过**

Run: `./mvnw -pl gateway-boot test -Dtest=ApplicationServiceImplTest -q`
Expected: PASS（Green）。

- [x] **Step 5: 提交**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/application/application/ApplicationServiceImpl.java \
        gateway-boot/src/test/java/com/codingas/gateway/application/application/ApplicationServiceImplTest.java
git commit -m "feat(application): delete 前置校验 UserApiKey 引用

- 注入 UserApiKeyGateway，有 Key 引用时抛 APPLICATION_HAS_API_KEYS
- 单测覆盖冲突拒绝与无引用正常删除

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

## Task 5: 后端 UserController + UserService 重置密码（TDD）

**Files:**
- Create: `gateway-boot/src/main/java/com/codingas/gateway/application/user/dto/ResetPasswordResponse.java`
- Modify: `gateway-boot/src/test/java/com/codingas/gateway/application/user/UserServiceImplTest.java`
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/application/user/UserService.java`
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/application/user/UserServiceImpl.java`
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/adapter/api/UserController.java`

- [x] **Step 1: 新建 ResetPasswordResponse record**

```java
package com.codingas.gateway.application.user.dto;

/**
 * 重置密码响应（一次性返回明文，不持久化）
 *
 * @param newPassword 新密码明文（HTTPS 传输，仅本次返回）
 */
public record ResetPasswordResponse(String newPassword) {
}
```

- [x] **Step 2: 先写失败测试 — 在 UserServiceImplTest 中加 resetPassword 测试类**

在 `UserServiceImplTest.java` 中加 import：

```java
import com.codingas.gateway.application.user.dto.ResetPasswordResponse;
import com.codingas.gateway.domain.iam.exception.ForbiddenException;
import com.codingas.gateway.common.exception.ResourceNotFoundException;
```

在测试类内部加嵌套类（参照现有测试风格）：

```java
    @Nested
    @DisplayName("resetPassword 方法测试")
    class ResetPasswordTests {

        @Test
        @DisplayName("重置成功 — 返回 16 位明文且更新哈希")
        void resetPassword_success_returns16CharPlain() {
            User user = new User();
            user.setId(1L);
            user.setBuiltin(false);
            when(userGateway.findById(1L)).thenReturn(java.util.Optional.of(user));
            when(passwordEncoder.encode(any())).thenReturn("hashed");

            ResetPasswordResponse response = service.resetPassword(1L);

            assertThat(response.newPassword()).hasSize(16);
            // 排除易混字符 O/0/I/1/l
            assertThat(response.newPassword()).matches("[ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789]{16}");
            verify(userGateway).save(argThat(u -> "hashed".equals(u.getPasswordHash())));
        }

        @Test
        @DisplayName("内建用户拒绝重置")
        void resetPassword_builtin_throws() {
            User user = new User();
            user.setId(2L);
            user.setBuiltin(true);
            when(userGateway.findById(2L)).thenReturn(java.util.Optional.of(user));

            assertThatThrownBy(() -> service.resetPassword(2L))
                    .isInstanceOf(ForbiddenException.class);
            verify(userGateway, never()).save(any());
        }

        @Test
        @DisplayName("用户不存在 — 抛 ResourceNotFoundException")
        void resetPassword_notFound_throws() {
            when(userGateway.findById(99L)).thenReturn(java.util.Optional.empty());

            assertThatThrownBy(() -> service.resetPassword(99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
```

注：`passwordEncoder` 与 `userGateway` mock 已存在于现有测试类。若现有类未 import `argThat`/`verify`，补充 `import static org.mockito.ArgumentMatchers.argThat;` 与 `import static org.mockito.Mockito.*;`。

- [x] **Step 3: 运行测试验证失败**

Run: `./mvnw -pl gateway-boot test -Dtest=UserServiceImplTest -q`
Expected: 编译失败——`UserService` 无 `resetPassword` 方法。Red 阶段。

- [x] **Step 4: UserService 接口加 resetPassword**

在 `UserService.java` 的 `changePassword` 后加：

```java
    /**
     * 重置密码（管理员触发）
     *
     * <p>生成 16 位随机密码（排除易混字符 O/0/I/1/l），更新哈希，
     * 一次性返回明文。禁止重置内建用户密码。</p>
     *
     * @param userId 用户 ID
     * @return 含一次性明文的响应
     */
    ResetPasswordResponse resetPassword(Long userId);
```

加 import：`import com.codingas.gateway.application.user.dto.ResetPasswordResponse;`

- [x] **Step 5: UserServiceImpl 实现 resetPassword**

5a. 加 import：

```java
import com.codingas.gateway.application.user.dto.ResetPasswordResponse;
import java.security.SecureRandom;
```

5b. 加常量与方法（放在 `changePassword` 后、`logout` 前）：

```java
    /** 重置密码字符集：排除易混字符 O/0/I/1/l */
    private static final String PASSWORD_CHARS =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
    private static final int RESET_PASSWORD_LENGTH = 16;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * 重置密码（管理员触发）
     */
    @Override
    @Transactional
    public ResetPasswordResponse resetPassword(Long userId) {
        User user = userGateway.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        if (user.isBuiltin()) {
            throw new ForbiddenException("不允许重置系统内建用户的密码");
        }

        // 生成 16 位随机密码（排除易混字符）
        StringBuilder plain = new StringBuilder(RESET_PASSWORD_LENGTH);
        for (int i = 0; i < RESET_PASSWORD_LENGTH; i++) {
            plain.append(PASSWORD_CHARS.charAt(SECURE_RANDOM.nextInt(PASSWORD_CHARS.length())));
        }
        String plainPassword = plain.toString();

        user.setPasswordHash(passwordEncoder.encode(plainPassword));
        userGateway.save(user);
        log.info("Reset password for user: id={}", userId);

        return new ResetPasswordResponse(plainPassword);
    }
```

- [x] **Step 6: UserController 加 POST /users/{id}/reset-password**

在 `UserController.java` 的 `listUserApiKeys` 前（或 `assignRoles` 后）加：

```java
    /**
     * 重置用户密码
     *
     * @param id 用户 ID
     * @return 含一次性明文密码的响应
     */
    @PostMapping("/{id}/reset-password")
    public ResetPasswordResponse resetPassword(@PathVariable Long id) {
        return userService.resetPassword(id);
    }
```

加 import：`import com.codingas.gateway.application.user.dto.ResetPasswordResponse;`

- [x] **Step 7: 运行测试验证通过**

Run: `./mvnw -pl gateway-boot test -Dtest=UserServiceImplTest -q`
Expected: PASS（Green）。

- [x] **Step 8: 提交**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/application/user/ \
        gateway-boot/src/main/java/com/codingas/gateway/adapter/api/UserController.java \
        gateway-boot/src/test/java/com/codingas/gateway/application/user/UserServiceImplTest.java
git commit -m "feat(user): 重置密码端点（16 位排除易混字符，一次性返回明文）

- UserService.resetPassword：SecureRandom 生成 16 位，禁内建用户
- UserController POST /users/{id}/reset-password
- 单测覆盖：16 位+字符集、内建拒绝、不存在 404

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

## Task 6: 后端 ApplicationController GET /applications/{id}/api-keys

**Files:**
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/adapter/api/ApplicationController.java`

- [x] **Step 1: ApplicationController 注入 UserApiKeyService + 加端点**

1a. 加 import：

```java
import com.codingas.gateway.application.userapikey.UserApiKeyService;
import com.codingas.gateway.application.userapikey.dto.UserApiKeyResponse;
```

1b. 由于类用 `@RequiredArgsConstructor`，加 final 字段：

```java
    private final ApplicationService applicationService;
    private final UserApiKeyService userApiKeyService;
```

1c. 在 `delete` 方法后、`listChannels` 前加：

```java
    /**
     * 查询应用下的所有 API Key
     *
     * @param id 应用 ID
     * @return 该应用下的 API Key 响应列表
     */
    @GetMapping("/{id}/api-keys")
    public List<UserApiKeyResponse> listApiKeys(@PathVariable Long id) {
        return userApiKeyService.findByApplicationId(id);
    }
```

- [x] **Step 2: 编译验证**

Run: `./mvnw -pl gateway-boot compile -q`
Expected: 编译通过。

- [x] **Step 3: 提交**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/adapter/api/ApplicationController.java
git commit -m "feat(application): GET /applications/{id}/api-keys 端点

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

## Task 7: 后端集成测试 + 路由回归测试

**Files:**
- Modify: `gateway-boot/src/test/java/com/codingas/gateway/adapter/api/UserControllerTest.java`
- Modify or Create: `gateway-boot/src/test/java/com/codingas/gateway/adapter/api/ApplicationControllerTest.java`
- Modify: `gateway-boot/src/test/java/com/codingas/gateway/application/proxy/routing/RoutingResolverTest.java`

- [x] **Step 1: UserControllerTest 加 reset-password 端点测试**

参照现有 `UserControllerTest` 的测试风格（@WebMvcTest 或 @SpringBootTest @AutoConfigureMockMvc）。加：

```java
    @Test
    @DisplayName("POST /users/{id}/reset-password 成功 — 返回 16 位明文")
    void resetPassword_success() throws Exception {
        // given: mock userService.resetPassword 返回 16 位明文
        when(userService.resetPassword(1L))
                .thenReturn(new ResetPasswordResponse("AbcdefghijKmnp23"));

        // when/then
        mockMvc.perform(post("/api/v1/users/1/reset-password"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.newPassword").value("AbcdefghijKmnp23"));
    }

    @Test
    @DisplayName("POST /users/{id}/reset-password 内建用户 — 403")
    void resetPassword_builtin_forbidden() throws Exception {
        when(userService.resetPassword(2L))
                .thenThrow(new ForbiddenException("不允许重置系统内建用户的密码"));

        mockMvc.perform(post("/api/v1/users/2/reset-password"))
                .andExpect(status().isForbidden());
    }
```

注：根据现有 `UserControllerTest` 的实际基类与 mock 方式调整（若用 @MockBean 则用 @MockBean；若用 @WebMvcTest 则需 @Import 异常处理器）。`ForbiddenException` 由 `IamExceptionHandler` 映射为 403。

- [x] **Step 2: ApplicationControllerTest 加 GET /api-keys 与 DELETE 冲突测试**

若 `ApplicationControllerTest.java` 不存在则新建（参照 UserControllerTest 风格）：

```java
    @Test
    @DisplayName("GET /applications/{id}/api-keys — 返回该应用下的 Key 列表")
    void listApiKeys_success() throws Exception {
        UserApiKeyResponse key = new UserApiKeyResponse(
                100L, 50L, 7L, "sk-abc1", null, "test-key", null, null);
        when(userApiKeyService.findByApplicationId(7L)).thenReturn(List.of(key));

        mockMvc.perform(get("/api/v1/applications/7/api-keys"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].applicationId").value(7));
    }

    @Test
    @DisplayName("DELETE /applications/{id} 有 Key 引用 — 400 Conflict")
    void delete_hasApiKeys_conflict() throws Exception {
        doThrow(new GatewayRequestException("APPLICATION_HAS_API_KEYS",
                "应用下还有 API Key，请先转移或删除"))
                .when(applicationService).delete(7L);

        mockMvc.perform(delete("/api/v1/applications/7"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("APPLICATION_HAS_API_KEYS"));
    }
```

注：根据现有 `ApiResponse` 序列化结构调整 jsonPath（`$.data` 或 `$.items`）。若现有测试用 `@SpringBootTest @AutoConfigureMockMvc`，保持一致。

- [x] **Step 3: 路由回归测试 — RoutingResolverTest 加带 applicationId 的 case**

在 `RoutingResolverTest.java` 的 `ResolveTests` 嵌套类中加：

```java
        @Test
        @DisplayName("带 applicationId 的路由 — 返回非空候选集（回归根因）")
        void resolveCandidates_withApplicationId_returnsNonEmpty() {
            // given: applicationId 非空，Application 存在
            Model model = new Model();
            model.setId(1L);
            model.setModelName("gpt-4o");

            ModelInstance modelInstance = new ModelInstance();
            modelInstance.setId(10L);
            modelInstance.setChannelId(100L);
            modelInstance.setModelId(1L);
            modelInstance.setState(ModelInstance.State.ACTIVE);

            Channel channel = new Channel();
            channel.setId(100L);
            channel.setName("openai-main");
            channel.setState(ChannelState.ACTIVE);
            channel.setTimeout(30);

            ChannelEndpoint endpoint = new ChannelEndpoint();
            endpoint.setId(50L);
            endpoint.setChannelId(100L);
            endpoint.setEndpointUrl("https://api.openai.com/v1");
            endpoint.setProtocol(Protocol.OPENAI);

            Application app = new Application();
            app.setId(7L);
            app.setTimeout(45);

            when(modelMatcher.match("gpt-4o")).thenReturn(model);
            when(instanceSelector.select(1L, 7L, 50L, "USER", RoutingStrategy.WEIGHTED, Protocol.OPENAI))
                    .thenReturn(List.of(modelInstance));
            when(applicationGateway.findById(7L)).thenReturn(app);
            when(credentialResolver.resolve(100L)).thenReturn("sk-test-key");
            when(endpointResolver.resolve(100L, Protocol.OPENAI)).thenReturn(endpoint);
            when(channelGateway.findById(100L)).thenReturn(Optional.of(channel));

            // when: 带 applicationId=7L 调用 resolveCandidates
            List<RoutingContext> candidates = routingResolver.resolveCandidates(
                    "gpt-4o", Protocol.OPENAI, 7L, 50L, "USER", RoutingStrategy.WEIGHTED);

            // then: 返回非空候选集（回归根因：applicationId 为 null 时返回空集）
            assertThat(candidates).isNotEmpty();
            assertThat(candidates.get(0).channelId()).isEqualTo(100L);
            // 应用级 timeout 覆盖渠道默认
            assertThat(candidates.get(0).timeout()).isEqualTo(45);
        }
```

注：`import com.codingas.gateway.domain.application.entity.Application;` 与 `import java.util.List;` 已存在于该测试文件。`Application.setTimeout(45)` 验证 applicationId 非空时应用级配置生效。

- [x] **Step 4: 运行所有新增测试**

Run: `./mvnw -pl gateway-boot test -Dtest="UserControllerTest,ApplicationControllerTest,RoutingResolverTest" -q`
Expected: 全部 PASS。

- [x] **Step 5: 提交**

```bash
git add gateway-boot/src/test/java/com/codingas/gateway/adapter/api/UserControllerTest.java \
        gateway-boot/src/test/java/com/codingas/gateway/adapter/api/ApplicationControllerTest.java \
        gateway-boot/src/test/java/com/codingas/gateway/application/proxy/routing/RoutingResolverTest.java
git commit -m "test: 集成测试 + 路由回归

- UserController reset-password 成功+内建拒绝
- ApplicationController GET /api-keys + DELETE 冲突
- RoutingResolver 带 applicationId 返回非空候选集（回归根因）

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

## Task 8: 后端全量测试 + 修复残留

**Files:**
- 可能涉及：调用 `new UserApiKeyCreateRequest(...)` / `new UserApiKeyUpdateRequest(...)` 的其他位置（如 `SampleDataLoader`、其他测试）

- [x] **Step 1: 全量编译 + 测试**

Run: `./mvnw -pl gateway-boot test -q`
Expected: 可能有编译失败——其他调用 `UserApiKeyCreateRequest`/`UserApiKeyUpdateRequest` 构造器的位置（如 `SampleDataLoader`、其他测试）需更新参数顺序。

- [x] **Step 2: 搜索并修复所有 DTO 构造调用**

Run: `grep -rn "new UserApiKeyCreateRequest\|new UserApiKeyUpdateRequest" gateway-boot/src/`

对每个命中位置补 applicationId 参数：
- `SampleDataLoader` 中创建 sample Key 时补 applicationId（从 sample 数据中取对应应用 ID）
- 其他测试中构造请求时补 applicationId（用 mock 应用 ID）

- [x] **Step 3: 重新运行全量测试**

Run: `./mvnw -pl gateway-boot test -q`
Expected: 全部 PASS。

- [x] **Step 4: 提交（如有修复）**

```bash
git add gateway-boot/
git commit -m "fix: 适配 UserApiKeyCreateRequest/UpdateRequest 新签名

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

## Task 9: 前端类型与 API 层

**Files:**
- Modify: `gateway-console/src/types/userApiKey.ts`
- Modify: `gateway-console/src/services/api/userApiKey.ts`
- Modify: `gateway-console/src/services/api/user.ts`

- [x] **Step 1: types/userApiKey.ts 加 applicationId + 修正注释**

替换全文为：

```typescript
/**
 * 用户 API Key 相关类型（与后端 UserApiKeyResponse 一致）
 *
 * 一个 Key 归属一个用户，并挂载到具体应用（applicationId）作为权限锚点——
 * 通过应用-渠道授权关系（ApplicationChannel）继承渠道访问权限。
 * applicationId 为 null 时权限路由返回空集，Key 不可用。
 */

/** 用户 API Key */
export interface UserApiKey {
  id: number;
  userId: number;
  applicationId: number | null;
  keyPrefix: string;
  keyPlain: string;
  name: string;
  deleted: boolean;
  createdAt: string;
  updatedAt: string;
}

/** 用户 API Key 详情 */
export interface UserApiKeyDetail extends UserApiKey {
}

/** 创建用户 API Key 请求 */
export interface CreateUserApiKeyRequest {
  userId: number;
  applicationId: number;
  name: string;
}

/** 更新用户 API Key 请求 */
export interface UpdateUserApiKeyRequest {
  applicationId?: number;
  name?: string;
}

/** 创建用户 API Key 响应 */
export interface CreateUserApiKeyResponse {
  id: number;
  keyPrefix: string;
  keyPlain: string;
}
```

- [x] **Step 2: services/api/userApiKey.ts 移除 rotate + 加 listByApplication**

替换 `rotate` 方法为 `listByApplication`：

```typescript
import { api } from './client';
import type {
  UserApiKey,
  UserApiKeyDetail,
  CreateUserApiKeyRequest,
  CreateUserApiKeyResponse,
  UpdateUserApiKeyRequest,
} from '@/types/userApiKey';

/**
 * 用户 API Key 管理接口
 */
export const userApiKeyApi = {
  /** 获取指定用户的 API Key 列表 */
  listByUser: (userId: number) =>
    api.get<UserApiKey[]>(`/users/${userId}/api-keys`),

  /** 查询所有 API Key（管理员用） */
  listAll: () =>
    api.get<UserApiKey[]>('/user-api-keys'),

  /** 按应用查询 API Key（应用详情页/筛选用） */
  listByApplication: (applicationId: number) =>
    api.get<UserApiKey[]>(`/applications/${applicationId}/api-keys`),

  /** 获取 API Key 详情 */
  getDetail: (id: number) =>
    api.get<UserApiKeyDetail>(`/user-api-keys/${id}/detail`),

  /** 创建用户 API Key */
  create: (data: CreateUserApiKeyRequest) =>
    api.post<CreateUserApiKeyResponse>('/user-api-keys', data),

  /** 更新用户 API Key（含补绑 applicationId） */
  update: (id: number, data: UpdateUserApiKeyRequest) =>
    api.put<UserApiKey>(`/user-api-keys/${id}`, data),

  /** 删除用户 API Key */
  delete: (id: number) =>
    api.delete<void>(`/user-api-keys/${id}`),
};
```

- [x] **Step 3: services/api/user.ts resetPassword 返回类型修正**

将 `resetPassword` 的返回类型从 `api.post<void>` 改为 `api.post<{ newPassword: string }>`：

```typescript
  /** 重置密码（返回一次性明文） */
  resetPassword: (id: number) =>
    api.post<{ newPassword: string }>(`/users/${id}/reset-password`),
```

- [x] **Step 4: 类型检查**

Run: `cd gateway-console && npm run typecheck` （或 `npx tsc --noEmit`）
Expected: 可能报错——`DownstreamKeysTable.tsx`/`UserApiKeyModal.tsx` 中 `create` 调用未传 applicationId。这是预期的，Task 10/11 会修复。

- [x] **Step 5: 提交**

```bash
git add gateway-console/src/types/userApiKey.ts \
        gateway-console/src/services/api/userApiKey.ts \
        gateway-console/src/services/api/user.ts
git commit -m "feat(console): 类型/API 层加 applicationId，移除 rotate 死代码

- types/userApiKey.ts: UserApiKey/Create/Update 加 applicationId
- services/api/userApiKey.ts: 移除 rotate，加 listByApplication
- services/api/user.ts: resetPassword 返回 { newPassword }

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

## Task 10: 前端 DownstreamKeysTable 改造

**Files:**
- Modify: `gateway-console/src/pages/ApiKeys/DownstreamKeysTable.tsx`
- Modify: `gateway-console/src/services/query/useUserApiKeys.ts`（加按应用查询 hook，可选）

- [x] **Step 1: useUserApiKeys.ts 加 useUserApiKeysByApplication hook（可选）**

若希望按应用筛选走服务端查询，加：

```typescript
/** 按应用查询 API Key */
export function useUserApiKeysByApplication(applicationId: number | undefined) {
  return useQuery({
    queryKey: ['userApiKeys', 'application', applicationId],
    queryFn: () => userApiKeyApi.listByApplication(applicationId!),
    enabled: applicationId != null,
  });
}
```

若前端纯客户端筛选（用 `useAllUserApiKeys` + filter），可跳过此步。下方 DownstreamKeysTable 用客户端筛选方案。

- [x] **Step 2: DownstreamKeysTable 改造 — 创建表单加 Application Select、列表加列、按应用筛选、URL 初始化**

替换 `DownstreamKeysTable.tsx` 全文为：

```tsx
import { useState, useCallback, useMemo, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import { Table, Button, Popconfirm, App, Input, Typography, Modal, Form, Select, Card } from 'antd';
import { PlusOutlined, DeleteOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useAllUserApiKeys, useDeleteUserApiKey, useCreateUserApiKey } from '@/services/query/useUserApiKeys';
import { useUsers } from '@/services/query/useUsers';
import { useApplications } from '@/services/query/useApplications';
import { MaskedKeyDisplay } from '@/components/MaskedKeyDisplay';
import type { UserApiKey } from '@/types/userApiKey';
import type { User } from '@/types/user';
import type { Application } from '@/types/application';

const { Text } = Typography;

export default function DownstreamKeysTable() {
  const { t } = useTranslation('apiKeys');
  const { message } = App.useApp();
  const [searchParams, setSearchParams] = useSearchParams();

  const { data: keys, isLoading } = useAllUserApiKeys();
  const { data: usersData } = useUsers({ size: 200 });
  const { data: applications } = useApplications();
  const deleteMutation = useDeleteUserApiKey();
  const createMutation = useCreateUserApiKey();
  const [search, setSearch] = useState('');

  // 从 URL ?applicationId= 初始化应用筛选
  const [applicationFilter, setApplicationFilter] = useState<number | undefined>(
    () => {
      const v = searchParams.get('applicationId');
      return v ? Number(v) : undefined;
    }
  );

  // 创建弹窗
  const [formVisible, setFormVisible] = useState(false);
  const [form] = Form.useForm();
  const [creating, setCreating] = useState(false);
  const [createdKeyPlain, setCreatedKeyPlain] = useState<string | null>(null);

  const userMap = useMemo(() => {
    const map = new Map<number, User>();
    usersData?.items?.forEach((u: User) => map.set(u.id, u));
    return map;
  }, [usersData]);

  const applicationMap = useMemo(() => {
    const map = new Map<number, Application>();
    applications?.forEach((a: Application) => map.set(a.id, a));
    return map;
  }, [applications]);

  // URL 同步：applicationId 变化时写回 URL
  useEffect(() => {
    if (applicationFilter == null) {
      searchParams.delete('applicationId');
    } else {
      searchParams.set('applicationId', String(applicationFilter));
    }
    setSearchParams(searchParams, { replace: true });
  }, [applicationFilter]);

  const filtered = useMemo(() => (keys ?? []).filter((k: UserApiKey) => {
    if (applicationFilter != null && k.applicationId !== applicationFilter) return false;
    if (!search) return true;
    const q = search.toLowerCase();
    return k.keyPrefix?.toLowerCase().includes(q) || k.name?.toLowerCase().includes(q);
  }), [keys, applicationFilter, search]);

  const handleRevoke = useCallback(async (id: number) => {
    try {
      await deleteMutation.mutateAsync(id);
      message.success(t('revoked', { defaultValue: 'Key 已吊销' }));
    } catch {
      message.error(t('revokeFailed', { defaultValue: '吊销失败' }));
    }
  }, [deleteMutation, message, t]);

  const handleAdd = () => {
    setCreatedKeyPlain(null);
    form.resetFields();
    // 若当前有应用筛选，预填创建表单的应用
    if (applicationFilter != null) {
      form.setFieldsValue({ applicationId: applicationFilter });
    }
    setFormVisible(true);
  };

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      setCreating(true);

      const result = await createMutation.mutateAsync({
        userId: values.userId,
        applicationId: values.applicationId,
        name: values.name,
      });
      setCreatedKeyPlain(result.keyPlain);
      message.success(t('createSuccess', { defaultValue: 'Key 创建成功' }));
      form.resetFields();
    } catch (error: unknown) {
      if (error && typeof error === 'object' && 'errorFields' in error) return;
      message.error(t('createFailed', { defaultValue: '创建失败' }));
    } finally {
      setCreating(false);
    }
  };

  const columns = useMemo(() => [
    {
      title: t('keyPrefix', { defaultValue: 'Key' }),
      dataIndex: 'keyPlain',
      key: 'keyPlain',
      width: 200,
      render: (_: string, record: UserApiKey) => (
        <MaskedKeyDisplay
          keyPlain={record.keyPlain}
          mode="readonly"
          size="small"
        />
      ),
    },
    {
      title: t('keyName', { defaultValue: '名称' }),
      dataIndex: 'name',
      key: 'name',
      width: 100,
    },
    {
      title: t('application', { defaultValue: '所属应用' }),
      dataIndex: 'applicationId',
      key: 'applicationId',
      width: 120,
      render: (appId: number | null) => {
        if (appId == null) return <Text type="warning">未绑定</Text>;
        const app = applicationMap.get(appId);
        return app ? `${app.name} (${appId})` : `应用 ${appId}`;
      },
    },
    {
      title: t('user', { defaultValue: '所属用户' }),
      dataIndex: 'userId',
      key: 'userId',
      width: 80,
      render: (userId: number) => {
        const user = userMap.get(userId);
        return user ? `${user.username} (${userId})` : `用户 ${userId}`;
      },
    },
    {
      title: t('createdAt', { defaultValue: '创建时间' }),
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 80,
      render: (val: string) => (val ? new Date(val).toLocaleString('zh-CN') : <Text type="secondary">-</Text>),
    },
    {
      title: t('actions', { defaultValue: '操作' }),
      key: 'actions',
      width: 40,
      render: (_: unknown, record: UserApiKey) => (
        <Popconfirm
          title={t('confirmRevoke', { defaultValue: '确定吊销此 Key？' })}
          onConfirm={() => handleRevoke(record.id)}
        >
          <Button type="link" size="small" danger icon={<DeleteOutlined />} />
        </Popconfirm>
      ),
    },
  ], [t, handleRevoke, userMap, applicationMap]);

  return (
    <div>
      <Card title={t('title')}>
        <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between', gap: 8, flexWrap: 'wrap' }}>
          <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
            <Input.Search
              placeholder={t('searchKeys', { defaultValue: '搜索 Key 前缀/名称...' })}
              style={{ width: 280 }}
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              allowClear
            />
            <Select
              placeholder={t('filterByApplication', { defaultValue: '按应用筛选' })}
              value={applicationFilter}
              onChange={setApplicationFilter}
              allowClear
              showSearch
              style={{ width: 200 }}
              filterOption={(input, option) =>
                (option?.label as string ?? '').toLowerCase().includes(input.toLowerCase())
              }
              options={(applications ?? []).map((a: Application) => ({
                label: `${a.name} (${a.id})`,
                value: a.id,
              }))}
            />
          </div>
          <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
            {t('createKey', { defaultValue: '创建 API Key' })}
          </Button>
        </div>

        <Table
          dataSource={filtered}
          columns={columns}
          rowKey="id"
          size="middle"
          loading={isLoading}
          pagination={{ pageSize: 15, showSizeChanger: true }}
          locale={{ emptyText: t('noKeys', { defaultValue: '暂无 API Key' }) }}
        />
      </Card>

      <Modal
        title={t('createKey', { defaultValue: '创建 API Key' })}
        open={formVisible}
        onOk={handleSubmit}
        onCancel={() => setFormVisible(false)}
        confirmLoading={creating}
        okText={t('create', { defaultValue: '创建' })}
        width={520}
        destroyOnClose
      >
        {createdKeyPlain && (
          <div style={{ marginBottom: 16, padding: 12, background: '#f6ffed', border: '1px solid #b7eb8f', borderRadius: 6 }}>
            <div style={{ marginBottom: 4, fontWeight: 500 }}>{t('createSuccess', { defaultValue: 'Key 创建成功' })}</div>
            <code style={{ wordBreak: 'break-all', fontSize: 13 }}>{createdKeyPlain}</code>
            <div style={{ marginTop: 4, color: '#999', fontSize: 12 }}>{t('oneTimeHint', { defaultValue: '此密钥仅显示一次，关闭后无法再次查看' })}</div>
          </div>
        )}

        <Form form={form} layout="vertical">
          <Form.Item name="userId" label={t('user', { defaultValue: '所属用户' })} rules={[{ required: true, message: '请选择用户' }]}>
            <Select
              showSearch
              placeholder="搜索并选择用户"
              filterOption={(input, option) =>
                (option?.label as string ?? '').toLowerCase().includes(input.toLowerCase())
              }
              options={(usersData?.items ?? []).map((u: User) => ({
                label: `${u.username} (${u.id})`,
                value: u.id,
              }))}
            />
          </Form.Item>

          <Form.Item
            name="applicationId"
            label={t('application', { defaultValue: '所属应用' })}
            rules={[{ required: true, message: '请选择应用' }]}
            extra="Key 的渠道访问权限由应用-渠道授权关系决定"
          >
            <Select
              showSearch
              placeholder="选择应用（权限锚点）"
              filterOption={(input, option) =>
                (option?.label as string ?? '').toLowerCase().includes(input.toLowerCase())
              }
              options={(applications ?? []).map((a: Application) => ({
                label: `${a.name} (${a.id})`,
                value: a.id,
              }))}
            />
          </Form.Item>

          <Form.Item name="name" label={t('keyName', { defaultValue: '名称' })} rules={[{ required: true, message: '请输入名称' }]}>
            <Input placeholder="例如：生产环境 Key" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
```

- [x] **Step 3: 类型检查 + 构建**

Run: `cd gateway-console && npm run build`
Expected: 构建通过（可能需补 `useApplications` 的 import 路径，参照现有用法）。

- [x] **Step 4: 提交**

```bash
git add gateway-console/src/pages/ApiKeys/DownstreamKeysTable.tsx \
        gateway-console/src/services/query/useUserApiKeys.ts
git commit -m "feat(console): DownstreamKeysTable 加 Application Select/列/筛选

- 创建表单加 Application Select（必填）
- 列表加所属应用列
- 顶部加按应用筛选 Select
- URL ?applicationId= 初始化筛选

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

## Task 11: 前端 UserApiKeyModal 改造

**Files:**
- Modify: `gateway-console/src/pages/Users/UserApiKeyModal.tsx`

- [x] **Step 1: UserApiKeyModal 删团队继承 Alert + 加 Application Select + 编辑补绑**

修改 `UserApiKeyModal.tsx`：

1a. 加 import：

```typescript
import { useApplications } from '@/services/query/useApplications';
```

1b. 在组件内加 hook 与 applicationMap：

```typescript
  const { data: applications } = useApplications();
  const applicationMap = useMemo(() => {
    const map = new Map<number, { id: number; name: string }>();
    applications?.forEach((a) => map.set(a.id, a));
    return map;
  }, [applications]);
```

（同时从 `react` 引入 `useMemo`，确认顶部 import 已含。）

1c. `handleEdit` 中补 applicationId 初始值：

```typescript
  const handleEdit = (key: UserApiKey) => {
    setEditingKey(key);
    setCreatedKey(null);
    form.setFieldsValue({
      name: key.name,
      applicationId: key.applicationId ?? undefined,
    });
    setShowForm(true);
  };
```

1d. `handleSubmit` 中创建/更新带 applicationId：

```typescript
      if (editingKey) {
        const request: UpdateUserApiKeyRequest = {
          name: values.name,
          applicationId: values.applicationId,
        };
        await userApiKeyApi.update(editingKey.id, request);
        message.success(t('apiKey.updateSuccess'));
      } else {
        const request: CreateUserApiKeyRequest = {
          userId,
          applicationId: values.applicationId,
          name: values.name,
        };
        const result = await userApiKeyApi.create(request)
        setCreatedKey(result.keyPlain)
        message.success(t('apiKey.createSuccess'))
      }
```

1e. 删除「团队继承」Alert（替换 `{!showForm && (...)}` 块为）：

```tsx
      {!showForm && (
        <div style={{ marginBottom: 12 }}>
          <Button type="primary" onClick={handleCreate}>{t('addApiKey')}</Button>
        </div>
      )}
```

1f. 表单加 Application Select（在 name 字段前）：

```tsx
            <Form.Item
              name="applicationId"
              label={t('apiKey.application', { defaultValue: '所属应用' })}
              rules={[{ required: true, message: t('apiKey.applicationRequired', { defaultValue: '请选择应用' }) }]}
              extra={t('apiKey.applicationHint', { defaultValue: 'Key 的渠道权限由应用-渠道授权决定' })}
            >
              <Select
                showSearch
                placeholder={t('apiKey.applicationPlaceholder', { defaultValue: '选择应用（权限锚点）' })}
                filterOption={(input, option) =>
                  (option?.label as string ?? '').toLowerCase().includes(input.toLowerCase())
                }
                options={(applications ?? []).map((a) => ({
                  label: `${a.name} (${a.id})`,
                  value: a.id,
                }))}
              />
            </Form.Item>
```

1g. 表格加「所属应用」列（在 name 列后）：

```tsx
    {
      title: t('apiKey.application', { defaultValue: '所属应用' }),
      dataIndex: 'applicationId',
      key: 'applicationId',
      render: (appId: number | null) => {
        if (appId == null) return '未绑定';
        const app = applicationMap.get(appId);
        return app ? app.name : `应用 ${appId}`;
      },
    },
```

- [x] **Step 2: 类型检查 + 构建**

Run: `cd gateway-console && npm run build`
Expected: 构建通过。

- [x] **Step 3: 提交**

```bash
git add gateway-console/src/pages/Users/UserApiKeyModal.tsx
git commit -m "feat(console): UserApiKeyModal 删团队继承 Alert + 加 Application Select

- 删除已归档的团队继承权限说明 Alert
- 创建表单加 Application Select（必填）
- 编辑表单支持补绑 applicationId
- 列表加所属应用列

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

## Task 12: 前端 Applications 页加「查看 Key」入口

**Files:**
- Modify: `gateway-console/src/pages/Applications/index.tsx`

- [x] **Step 1: Applications/index.tsx 加查看 Key 行操作**

1a. 加 import：

```typescript
import { useNavigate } from 'react-router-dom';
import { KeyOutlined } from '@ant-design/icons';
```

1b. 组件内加 navigate：

```typescript
  const navigate = useNavigate();
```

1c. 在 `columns` 的 `actions` 列中，`SafetyOutlined`（渠道授权）按钮后加「查看 Key」按钮：

```tsx
          <Tooltip title={t('application.viewKeys', { defaultValue: '查看 Key' })}>
            <Button
              type="text"
              size="small"
              icon={<KeyOutlined />}
              onClick={() => navigate(`/keys?applicationId=${record.id}`)}
            />
          </Tooltip>
```

注：放在 `SafetyOutlined` Tooltip 之后、`canWrite` 块之前，使所有有读权限的用户可见。

- [x] **Step 2: 类型检查 + 构建**

Run: `cd gateway-console && npm run build`
Expected: 构建通过。若 `/keys` 路由不存在，确认路由配置（通常在 `router.tsx` 或 `App.tsx`）。若路由路径不同（如 `/api-keys`），调整 `navigate` 目标。

- [x] **Step 3: 提交**

```bash
git add gateway-console/src/pages/Applications/index.tsx
git commit -m "feat(console): Applications 页加查看 Key 入口

行操作加图标按钮跳转 /keys?applicationId=<id> 触发筛选

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

## Task 13: 验证与收尾

**Files:**
- 无新文件，仅运行验证

- [x] **Step 1: 后端全量测试**

Run: `./mvnw -pl gateway-boot test -q`
Expected: 全部 PASS，覆盖率满足核心服务层 ≥90% / 规则引擎 ≥85% / 适配器层 ≥80%（参照 CLAUDE.md）。

- [x] **Step 2: 前端构建 + 测试**

Run: `cd gateway-console && npm run build && npm test`
Expected: 构建通过，现有测试不回归。若 `ApplicationFormModal.test.tsx` 因新增列失败，调整测试快照。

- [ ] **Step 3: 端到端手验**

启动后端 `./mvnw spring-boot:run -pl gateway-boot` + 前端 `cd gateway-console && npm run dev`，验证：

1. 创建 Application「demo-app」
2. 创建 UserApiKey：选择用户 + 选择 demo-app + 命名 → 创建成功，明文显示一次
3. 在 Applications 页点击「查看 Key」→ 跳转 `/keys?applicationId=<id>`，列表仅显示该应用下的 Key
4. 用该 Key 调 `POST /v1/chat/completions`（带 Authorization）→ 路由成功返回响应（验证根因修复）
5. 删除有 Key 的 Application → 返回 400 + code=APPLICATION_HAS_API_KEYS
6. 重置非内建用户密码 → 返回 16 位明文；重置内建用户 → 403

- [x] **Step 4: 提交验证记录（可选）**

如有验证脚本或截图，记录到 `openspec/changes/key-application-binding/` 下。无需提交代码。

---

## Self-Review

### Spec 覆盖

- Design Doc「后端 DTO」5 个 record → Task 1 ✓
- Design Doc「UserApiKeyServiceImpl」注入 ApplicationGateway + create/update 校验 + 响应映射 → Task 3 ✓
- Design Doc「UserApiKeyGateway + Impl」findByApplicationId + Repository 派生查询 → Task 2 ✓
- Design Doc「ApplicationController」GET /api-keys → Task 6 ✓
- Design Doc「ApplicationServiceImpl.delete」前置校验 → Task 4 ✓
- Design Doc「UserController + UserService resetPassword」16 位排除易混 + 哈希 + 一次性明文 + 禁内建 → Task 5 ✓
- Design Doc「前端 types/userApiKey.ts」加字段 + 修正注释 → Task 9 ✓
- Design Doc「services/api/userApiKey.ts」移除 rotate + 加 listByApplication → Task 9 ✓
- Design Doc「DownstreamKeysTable」Application Select + 列 + 筛选 + URL → Task 10 ✓
- Design Doc「UserApiKeyModal」删 Alert + Application Select + 补绑 → Task 11 ✓
- Design Doc「Applications/index.tsx」查看 Key 跳转 → Task 12 ✓
- Design Doc「services/api/user.ts」resetPassword 保留 → Task 9（修正返回类型）✓
- 测试策略 4 类单测/集成 + 路由回归 → Task 3/4/5/7 ✓
- tasks.md 7 组 22 任务全覆盖 ✓

### 占位符扫描

无 TBD/TODO/"实现细节后补"。每个代码块均给出完整可粘贴内容。

### 类型一致性

- `UserApiKeyCreateRequest(userId, applicationId, name)` —— Task 1 定义，Task 3/8 测试与 SampleDataLoader 适配
- `UserApiKeyUpdateRequest(applicationId, name)` —— Task 1 定义，Task 3 测试与 Task 11 前端调用一致
- `UserApiKeyResponse(id, userId, applicationId, keyPrefix, keyPlain, name, createdAt, updatedAt)` —— Task 1 定义，Task 3 toResponse 映射一致
- `UserApiKeyService.findByApplicationId(Long)` —— Task 3 接口与 Task 6 Controller 调用一致
- `UserService.resetPassword(Long)` 返回 `ResetPasswordResponse(String newPassword)` —— Task 5 定义，Task 9 前端 `{ newPassword: string }` 一致
- `APPLICATION_HAS_API_KEYS` code 字符串 —— Task 4 抛出与 Task 7 测试断言一致
- 前端 `applicationId: number | null`（UserApiKey）/ `number`（CreateRequest）/ `number?`（UpdateRequest）—— Task 9 与 Task 10/11 使用一致

---

## 执行交接

计划已保存至 `docs/superpowers/plans/2026-07-06-key-application-binding.md`。两种执行方式：

**1. Subagent-Driven（推荐）** - 每个 Task 派发独立 subagent，Task 间审查，快速迭代

**2. Inline Execution** - 在当前会话用 executing-plans 批量执行，带检查点审查

选择哪种方式？
