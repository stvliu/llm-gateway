# 用户与 UserApiKey 关系重构 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现用户维度的密钥管理 — 支持按用户查询 Key、新增"我的密钥"页面、用户列表页"管理密钥"弹窗（创建/查看/复制/吊销）、创建 Key 时 userId 权限校验。

**Architecture:** 后端新增 `findByUserId` 查询 + Response DTO 增加 `teamName` 冗余字段 + UserController 新增两个端点 + 创建时权限校验。前端新增"我的密钥"页面 + 用户列表页"管理密钥"弹窗 + 侧边栏入口。

**Tech Stack:** Java 21 + Spring Boot 3.5.x + JPA + Sa-Token | React + Ant Design + React Query + React Router

---

## 文件结构

### 后端 — 新建文件

| 文件 | 职责 |
|------|------|
| `gateway-boot/src/test/java/com/codingas/gateway/infrastructure/team/gateway/UserApiKeyGatewayImplTest.java` | Gateway 实现测试 |
| `gateway-boot/src/test/java/com/codingas/gateway/application/userapikey/UserApiKeyServiceImplTest.java` | Service 测试 |
| `gateway-boot/src/test/java/com/codingas/gateway/adapter/api/UserControllerTest.java` | Controller 测试 |

### 后端 — 修改文件

| 文件 | 变更 |
|------|------|
| `domain/team/gateway/UserApiKeyGateway.java` | 新增 `findByUserId(Long userId)` 方法 |
| `infrastructure/team/gateway/database/repository/UserApiKeyRepository.java` | 新增 `findByUserId(Long userId)` 查询方法 |
| `infrastructure/team/gateway/UserApiKeyGatewayImpl.java` | 实现 `findByUserId()` |
| `application/userapikey/dto/UserApiKeyResponse.java` | 新增 `teamName` 字段 |
| `application/userapikey/dto/UserApiKeyDetailResponse.java` | 新增 `teamName` 字段 |
| `application/userapikey/UserApiKeyServiceImpl.java` | 映射 `teamName`；新增 `findByUserId()` 方法；创建时校验 userId 权限 |
| `adapter/api/UserController.java` | 新增 `GET /me/api-keys` 和 `GET /users/{userId}/api-keys` 端点 |
| `adapter/api/TeamController.java` | 创建 Key 时校验 userId 权限 |

### 前端 — 新建文件

| 文件 | 职责 |
|------|------|
| `gateway-console/src/pages/MyApiKeys/index.tsx` | "我的密钥"页面 |
| `gateway-console/src/components/UserApiKeyManageModal.tsx` | 用户维度密钥管理弹窗（复用逻辑） |

### 前端 — 修改文件

| 文件 | 变更 |
|------|------|
| `gateway-console/src/types/team.ts` | 新增 `teamName` 字段到 UserApiKey/UserApiKeyDetail |
| `gateway-console/src/services/api/team.ts` | 新增 `fetchMyApiKeys()` 和 `fetchUserApiKeys()` API 函数 |
| `gateway-console/src/services/query/useTeams.ts` | 新增 `useMyApiKeys` 和 `useUserApiKeys` hooks |
| `gateway-console/src/pages/Users/index.tsx` | 操作列增加"管理密钥"按钮 |
| `gateway-console/src/components/layout/AppLayout.tsx` | 侧边栏增加"我的密钥"入口 |
| `gateway-console/src/router/index.tsx` | 新增 `/my-api-keys` 路由 |

---

## Task 1: 后端 — UserApiKeyGateway.findByUserId() 方法

**Files:**
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/domain/team/gateway/UserApiKeyGateway.java`
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/team/gateway/database/repository/UserApiKeyRepository.java`
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/team/gateway/UserApiKeyGatewayImpl.java`

- [ ] **Step 1: 在 UserApiKeyGateway 接口新增 findByUserId 方法**

```java
// UserApiKeyGateway.java — 在 findByProductId 方法后新增

/**
 * 根据用户 ID 查询所有 API Key
 */
List<UserApiKey> findByUserId(Long userId);
```

- [ ] **Step 2: 在 UserApiKeyRepository 新增 JPA 查询方法**

```java
// UserApiKeyRepository.java — 在现有查询方法后新增

List<UserApiKeyDo> findByUserId(Long userId);
```

- [ ] **Step 3: 在 UserApiKeyGatewayImpl 实现 findByUserId**

```java
// UserApiKeyGatewayImpl.java — 在 findByProductId 方法后新增

@Override
public List<UserApiKey> findByUserId(Long userId) {
    return userApiKeyRepository.findByUserId(userId).stream()
        .map(this::toEntity)
        .toList();
}
```

- [ ] **Step 4: 编译验证**

Run: `cd /mnt/e/workspace/llm-gateway && ./mvnw compile -pl gateway-boot -q`
Expected: BUILD SUCCESS

- [ ] **Step 5: 提交**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/domain/team/gateway/UserApiKeyGateway.java \
  gateway-boot/src/main/java/com/codingas/gateway/infrastructure/team/gateway/database/repository/UserApiKeyRepository.java \
  gateway-boot/src/main/java/com/codingas/gateway/infrastructure/team/gateway/UserApiKeyGatewayImpl.java
git commit -m "feat: UserApiKeyGateway 新增 findByUserId 查询方法"
```

---

## Task 2: 后端 — Response DTO 增加 teamName 字段

**Files:**
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/application/userapikey/dto/UserApiKeyResponse.java`
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/application/userapikey/dto/UserApiKeyDetailResponse.java`
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/application/userapikey/UserApiKeyServiceImpl.java`

- [ ] **Step 1: UserApiKeyResponse 新增 teamName 字段**

当前 `UserApiKeyResponse` 是 record，字段列表：`id, teamId, userId, productId, keyPrefix, name, models, quotaLimit, state, createdAt`。其中 `quotaLimit` 为 `Long`，`state` 为 `String`。

在 `teamId` 后新增 `teamName`：

```java
// UserApiKeyResponse.java
public record UserApiKeyResponse(
    Long id,
    Long teamId,
    String teamName,
    Long userId,
    Long productId,
    String keyPrefix,
    String name,
    List<String> models,
    Long quotaLimit,
    String state,
    Instant createdAt
) {}
```

- [ ] **Step 2: UserApiKeyDetailResponse 新增 teamName 字段**

在 `teamId` 后新增 `teamName`：

```java
// UserApiKeyDetailResponse.java
public record UserApiKeyDetailResponse(
    Long id,
    Long teamId,
    String teamName,
    Long userId,
    Long productId,
    String keyPrefix,
    String apiKeyPlain,
    String name,
    List<String> models,
    Long quotaLimit,
    String state,
    Instant createdAt,
    Instant updatedAt
) {}
```

- [ ] **Step 3: UserApiKeyServiceImpl 映射 teamName**

在 `UserApiKeyServiceImpl` 中新增注入 `TeamGateway`，修改 `toResponse()` 和 `toDetailResponse()` 方法映射 `teamName`：

```java
// 新增注入（在现有 userApiKeyGateway 后）
private final TeamGateway teamGateway;

// 修改构造函数以注入 TeamGateway
// 如果使用 @RequiredArgsConstructor，只需新增 final 字段即可

// 修改 toResponse — 在 teamId 后新增 teamName
private UserApiKeyResponse toResponse(UserApiKey apiKey) {
    String teamName = teamGateway.findById(apiKey.getTeamId())
        .map(Team::getName)
        .orElse(null);
    return new UserApiKeyResponse(
        apiKey.getId(),
        apiKey.getTeamId(),
        teamName,           // 新增
        apiKey.getUserId(),
        apiKey.getProductId(),
        apiKey.getKeyPrefix(),
        apiKey.getName(),
        apiKey.getModels(),
        apiKey.getQuotaLimit(),
        apiKey.getState() != null ? apiKey.getState().name() : null,
        apiKey.getCreatedAt()
    );
}

// 修改 toDetailResponse — 在 teamId 后新增 teamName
private UserApiKeyDetailResponse toDetailResponse(UserApiKey apiKey) {
    String teamName = teamGateway.findById(apiKey.getTeamId())
        .map(Team::getName)
        .orElse(null);
    return new UserApiKeyDetailResponse(
        apiKey.getId(),
        apiKey.getTeamId(),
        teamName,           // 新增
        apiKey.getUserId(),
        apiKey.getProductId(),
        apiKey.getKeyPrefix(),
        apiKey.getKeyPlain(),
        apiKey.getName(),
        apiKey.getModels(),
        apiKey.getQuotaLimit(),
        apiKey.getState() != null ? apiKey.getState().name() : null,
        apiKey.getCreatedAt(),
        apiKey.getUpdatedAt()
    );
}
```

注意：`toCreateResponse()` 方法内部调用 `toDetailResponse()`，因此也会自动包含 `teamName`。`UserApiKeyCreateResponse` 无需修改，它从 `toDetailResponse()` 获取数据。

- [ ] **Step 4: 编译验证**

Run: `cd /mnt/e/workspace/llm-gateway && ./mvnw compile -pl gateway-boot -q`
Expected: BUILD SUCCESS

- [ ] **Step 5: 提交**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/application/userapikey/dto/UserApiKeyResponse.java \
  gateway-boot/src/main/java/com/codingas/gateway/application/userapikey/dto/UserApiKeyDetailResponse.java \
  gateway-boot/src/main/java/com/codingas/gateway/application/userapikey/UserApiKeyServiceImpl.java
git commit -m "feat: UserApiKeyResponse/UserApiKeyDetailResponse 新增 teamName 冗余字段"
```

---

## Task 3: 后端 — UserApiKeyServiceImpl 新增 findByUserId 方法

**Files:**
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/application/userapikey/UserApiKeyServiceImpl.java`

- [ ] **Step 1: 在 UserApiKeyServiceImpl 新增 findByUserId 方法**

```java
/**
 * 根据用户 ID 查询所有 API Key
 */
public List<UserApiKeyResponse> findByUserId(Long userId) {
    List<UserApiKey> apiKeys = userApiKeyGateway.findByUserId(userId);
    return apiKeys.stream()
        .map(this::toResponse)
        .toList();
}
```

- [ ] **Step 2: 编译验证**

Run: `cd /mnt/e/workspace/llm-gateway && ./mvnw compile -pl gateway-boot -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/application/userapikey/UserApiKeyServiceImpl.java
git commit -m "feat: UserApiKeyServiceImpl 新增 findByUserId 方法"
```

---

## Task 4: 后端 — 创建 Key 时校验 userId 权限

**Files:**
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/application/userapikey/UserApiKeyServiceImpl.java`
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/adapter/api/TeamController.java`

- [ ] **Step 1: 在 UserApiKeyServiceImpl.create() 方法中增加 userId 权限校验**

在 `create()` 方法中，设置 `userId` 后增加权限校验逻辑。注意：现有 `create()` 方法返回 `UserApiKeyCreateResponse`（包含明文 Key），需要保留此返回类型。

```java
/**
 * 创建用户 API Key，校验 userId 权限
 *
 * @param request 创建请求（需包含 userId）
 * @param currentUserId 当前登录用户 ID
 * @param isAdmin 是否为管理员
 * @return 创建结果（包含仅展示一次的明文 Key）
 */
public UserApiKeyCreateResponse create(UserApiKeyCreateRequest request, Long currentUserId, boolean isAdmin) {
    // 权限校验：普通用户只能为自己创建 Key
    if (!isAdmin && !request.userId().equals(currentUserId)) {
        throw new org.springframework.security.access.AccessDeniedException("普通用户只能为自己创建密钥");
    }

    // 管理员校验：userId 必须是该团队成员（后续可扩展）

    // 生成明文 Key
    String plainKey = generateRawKey();
    String keyPrefix = plainKey.substring(0, Math.min(8, plainKey.length()));

    UserApiKey apiKey = new UserApiKey();
    apiKey.setTeamId(request.teamId());
    apiKey.setUserId(request.userId());
    apiKey.setProductId(request.productId());
    apiKey.setKeyPlain(plainKey);
    apiKey.setKeyPrefix(keyPrefix);
    apiKey.setName(request.name());
    apiKey.setModels(request.models());
    apiKey.setQuotaLimit(request.quotaLimit());
    apiKey.setState(UserApiKeyState.ACTIVE);

    UserApiKey saved = userApiKeyGateway.save(apiKey);
    log.info("Created UserApiKey: id={}, teamId={}, userId={}", saved.getId(), saved.getTeamId(), saved.getUserId());

    return new UserApiKeyCreateResponse(saved.getId(), keyPrefix, plainKey);
}
```

注意：`UserApiKeyCreateRequest` 需要新增 `userId` 字段（当前只有 `teamId, productId, name, models, quotaLimit`）。修改如下：

```java
// UserApiKeyCreateRequest.java — 新增 userId 字段
public record UserApiKeyCreateRequest(
    @NotNull(message = "团队 ID 不能为空")
    Long teamId,
    @NotNull(message = "用户 ID 不能为空")
    Long userId,
    @NotNull(message = "产品 ID 不能为空")
    Long productId,
    @NotBlank(message = "密钥名称不能为空")
    String name,
    List<String> models,
    Long quotaLimit
) {}
```

同时保留原有的无 userId 的 `create()` 方法签名用于兼容，或者直接修改所有调用点。

- [ ] **Step 2: 修改 TeamController 传递当前用户信息**

在 TeamController 的创建 Key 端点中，从 `request.getAttribute("userId")` 获取当前用户 ID，从 Sa-Token 获取角色信息。同时修改 `UserApiKeyCreateRequest` 的构建方式以包含 `userId`：

```java
// TeamController.java — 修改 createApiKey 方法
@PostMapping("/{teamId}/api-keys")
public ResponseEntity<UserApiKeyCreateResponse> createApiKey(
        @PathVariable Long teamId,
        @RequestBody UserApiKeyCreateRequest request,
        HttpServletRequest httpRequest) {
    Long currentUserId = (Long) httpRequest.getAttribute("userId");
    boolean isAdmin = StpUtil.hasRole("ADMIN");
    // 构建包含 userId 的请求（前端传入 userId）
    UserApiKeyCreateRequest fullRequest = new UserApiKeyCreateRequest(
        teamId, request.userId(), request.productId(),
        request.name(), request.models(), request.quotaLimit()
    );
    UserApiKeyCreateResponse response = userApiKeyService.create(fullRequest, currentUserId, isAdmin);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
}
```

需要在 TeamController 中添加 `import cn.dev33.satoken.stp.StpUtil;` 和 `import jakarta.servlet.http.HttpServletRequest;`。

- [ ] **Step 3: 编译验证**

Run: `cd /mnt/e/workspace/llm-gateway && ./mvnw compile -pl gateway-boot -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: 提交**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/application/userapikey/UserApiKeyServiceImpl.java \
  gateway-boot/src/main/java/com/codingas/gateway/adapter/api/TeamController.java
git commit -m "feat: 创建 Key 时校验 userId 权限 — 普通用户只能为自己创建"
```

---

## Task 5: 后端 — UserController 新增用户维度端点

**Files:**
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/adapter/api/UserController.java`

- [ ] **Step 1: 在 UserController 新增两个端点**

```java
// UserController.java — 新增 import
import com.codingas.gateway.application.userapikey.UserApiKeyServiceImpl;
import com.codingas.gateway.application.userapikey.dto.UserApiKeyResponse;
import jakarta.servlet.http.HttpServletRequest;

// 新增注入（在现有注入后添加）
private final UserApiKeyServiceImpl userApiKeyService;

/**
 * 查询当前登录用户的所有 API Key
 */
@GetMapping("/me/api-keys")
public ResponseEntity<List<UserApiKeyResponse>> getMyApiKeys(HttpServletRequest request) {
    Long userId = (Long) request.getAttribute("userId");
    List<UserApiKeyResponse> apiKeys = userApiKeyService.findByUserId(userId);
    return ResponseEntity.ok(apiKeys);
}

/**
 * 查询指定用户的所有 API Key（仅 ADMIN）
 */
@GetMapping("/{userId}/api-keys")
public ResponseEntity<List<UserApiKeyResponse>> getUserApiKeys(@PathVariable Long userId) {
    List<UserApiKeyResponse> apiKeys = userApiKeyService.findByUserId(userId);
    return ResponseEntity.ok(apiKeys);
}
```

注意：`GET /{userId}/api-keys` 端点需要 ADMIN 权限。如果 Sa-Token 的角色拦截器已配置，则无需额外代码。否则需要在方法上添加 `@SaCheckRole("ADMIN")` 注解。

- [ ] **Step 2: 编译验证**

Run: `cd /mnt/e/workspace/llm-gateway && ./mvnw compile -pl gateway-boot -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/adapter/api/UserController.java
git commit -m "feat: UserController 新增 GET /me/api-keys 和 GET /users/{userId}/api-keys 端点"
```

---

## Task 6: 后端 — 编写单元测试

**Files:**
- Create: `gateway-boot/src/test/java/com/codingas/gateway/application/userapikey/UserApiKeyServiceImplTest.java`
- Create: `gateway-boot/src/test/java/com/codingas/gateway/adapter/api/UserControllerTest.java`

- [ ] **Step 1: 编写 UserApiKeyServiceImplTest**

```java
package com.codingas.gateway.application.userapikey;

import com.codingas.gateway.application.userapikey.dto.UserApiKeyCreateRequest;
import com.codingas.gateway.application.userapikey.dto.UserApiKeyCreateResponse;
import com.codingas.gateway.application.userapikey.dto.UserApiKeyResponse;
import com.codingas.gateway.domain.team.entity.Team;
import com.codingas.gateway.domain.security.entity.UserApiKey;
import com.codingas.gateway.domain.team.enums.TeamState;
import com.codingas.gateway.domain.team.enums.UserApiKeyState;
import com.codingas.gateway.domain.team.gateway.TeamGateway;
import com.codingas.gateway.domain.security.service.UserApiKeyGateway;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserApiKeyServiceImpl 测试")
class UserApiKeyServiceImplTest {

    @Mock
    private UserApiKeyGateway userApiKeyGateway;

    @Mock
    private TeamGateway teamGateway;

    private UserApiKeyServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UserApiKeyServiceImpl(userApiKeyGateway, teamGateway);
    }

    @Nested
    @DisplayName("findByUserId 方法测试")
    class FindByUserIdTests {

        @Test
        @DisplayName("返回指定用户的所有 Key，包含 teamName")
        void findByUserId_returnsKeysWithTeamName() {
            // given
            UserApiKey apiKey = new UserApiKey();
            apiKey.setId(1L);
            apiKey.setTeamId(100L);
            apiKey.setUserId(1L);
            apiKey.setProductId(200L);
            apiKey.setKeyPrefix("sk-test1");
            apiKey.setName("测试 Key");
            apiKey.setState(UserApiKeyState.ACTIVE);

            Team team = new Team();
            team.setId(100L);
            team.setName("默认团队");
            team.setState(TeamState.ACTIVE);

            when(userApiKeyGateway.findByUserId(1L)).thenReturn(List.of(apiKey));
            when(teamGateway.findById(100L)).thenReturn(Optional.of(team));

            // when
            List<UserApiKeyResponse> result = service.findByUserId(1L);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).teamName()).isEqualTo("默认团队");
        }

        @Test
        @DisplayName("用户无 Key 时返回空列表")
        void findByUserId_noKeys_returnsEmptyList() {
            when(userApiKeyGateway.findByUserId(999L)).thenReturn(List.of());

            List<UserApiKeyResponse> result = service.findByUserId(999L);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("create 方法权限校验测试")
    class CreatePermissionTests {

        @Test
        @DisplayName("普通用户为自己创建 Key — 成功")
        void create_normalUser_self_success() {
            UserApiKeyCreateRequest request = new UserApiKeyCreateRequest(
                100L, 1L, 200L, "测试 Key", List.of(), null
            );

            UserApiKey apiKey = new UserApiKey();
            apiKey.setId(1L);
            apiKey.setTeamId(100L);
            apiKey.setUserId(1L);
            apiKey.setProductId(200L);
            apiKey.setName("测试 Key");
            apiKey.setState(UserApiKeyState.ACTIVE);

            when(userApiKeyGateway.save(any(UserApiKey.class))).thenReturn(apiKey);

            UserApiKeyCreateResponse result = service.create(request, 1L, false);

            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("普通用户为他人创建 Key — 抛出异常")
        void create_normalUser_otherUser_throwsException() {
            UserApiKeyCreateRequest request = new UserApiKeyCreateRequest(
                100L, 2L, 200L, "测试 Key", List.of(), null
            );

            assertThatThrownBy(() -> service.create(request, 1L, false))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        }

        @Test
        @DisplayName("管理员为他人创建 Key — 成功")
        void create_admin_otherUser_success() {
            UserApiKeyCreateRequest request = new UserApiKeyCreateRequest(
                100L, 2L, 200L, "测试 Key", List.of(), null
            );

            UserApiKey apiKey = new UserApiKey();
            apiKey.setId(1L);
            apiKey.setTeamId(100L);
            apiKey.setUserId(2L);
            apiKey.setProductId(200L);
            apiKey.setName("测试 Key");
            apiKey.setState(UserApiKeyState.ACTIVE);

            when(userApiKeyGateway.save(any(UserApiKey.class))).thenReturn(apiKey);

            UserApiKeyCreateResponse result = service.create(request, 1L, true);

            assertThat(result).isNotNull();
        }
    }
}
```

- [ ] **Step 2: 运行 UserApiKeyServiceImplTest**

Run: `cd /mnt/e/workspace/llm-gateway && ./mvnw test -pl gateway-boot -Dtest="UserApiKeyServiceImplTest" -q`
Expected: 所有测试通过

- [ ] **Step 3: 编写 UserControllerTest**

```java
package com.codingas.gateway.adapter.api;

import com.codingas.gateway.application.userapikey.UserApiKeyServiceImpl;
import com.codingas.gateway.application.userapikey.dto.UserApiKeyResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.bean.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@DisplayName("UserController 测试")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserApiKeyServiceImpl userApiKeyService;

    // 注意：需要 mock 其他 UserController 依赖的 bean

    @Test
    @DisplayName("GET /me/api-keys — 返回当前用户的 Key 列表")
    void getMyApiKeys_returnsCurrentUserKeys() throws Exception {
        UserApiKeyResponse response = new UserApiKeyResponse(
            1L, 100L, "默认团队", 1L, 200L, "sk-test", "测试 Key",
            List.of(), null, "ACTIVE", Instant.now()
        );
        when(userApiKeyService.findByUserId(1L)).thenReturn(List.of(response));

        // 注意：需要模拟认证状态，具体取决于 Sa-Token 集成方式
        // mockMvc.perform(get("/api/v1/me/api-keys"))
        //     .andExpect(status().isOk())
        //     .andExpect(jsonPath("$[0].teamName").value("默认团队"));
    }
}
```

注意：Controller 测试需要根据 Sa-Token 集成方式调整认证模拟。如果 Sa-Token 在 `@WebMvcTest` 中不易模拟，可以改为使用 `@SpringBootTest` + `MockMvc` 或直接测试 Service 层。

- [ ] **Step 4: 运行测试**

Run: `cd /mnt/e/workspace/llm-gateway && ./mvnw test -pl gateway-boot -Dtest="UserApiKeyServiceImplTest,UserControllerTest" -q`
Expected: Service 测试通过，Controller 测试根据 Sa-Token 集成调整

- [ ] **Step 5: 提交**

```bash
git add gateway-boot/src/test/java/com/codingas/gateway/application/userapikey/UserApiKeyServiceImplTest.java \
  gateway-boot/src/test/java/com/codingas/gateway/adapter/api/UserControllerTest.java
git commit -m "test: UserApiKeyServiceImpl 和 UserController 单元测试"
```

---

## Task 7: 前端 — 类型定义和 API 函数更新

**Files:**
- Modify: `gateway-console/src/types/team.ts`
- Modify: `gateway-console/src/services/api/team.ts`
- Modify: `gateway-console/src/services/query/useTeams.ts`

- [ ] **Step 1: 更新 TypeScript 类型定义**

在 `team.ts` 中为 `UserApiKey` 和 `UserApiKeyDetail` 新增 `teamName` 字段：

```typescript
// UserApiKey 接口 — 在 teamId 后新增 teamName
export interface UserApiKey {
  id: number;
  teamId: number;
  teamName: string;  // 新增
  userId: number;
  productId: number;
  keyPrefix: string;
  name: string;
  models: string[];
  quotaLimit: number | null;
  state: string;
  createdAt: string;
}

// UserApiKeyDetail 接口 — 在 teamId 后新增 teamName
export interface UserApiKeyDetail {
  id: number;
  teamId: number;
  teamName: string;  // 新增
  userId: number;
  productId: number;
  keyPrefix: string;
  apiKeyPlain: string;
  name: string;
  models: string[];
  quotaLimit: number | null;
  state: string;
  createdAt: string;
  updatedAt: string;
}
```

- [ ] **Step 2: 新增 API 函数**

在 `team.ts` API 服务中新增：

```typescript
// 获取当前用户的 API Key 列表
export async function fetchMyApiKeys(): Promise<UserApiKey[]> {
  const { data } = await apiClient.get<UserApiKey[]>('/api/v1/me/api-keys');
  return data;
}

// 获取指定用户的 API Key 列表（仅 ADMIN）
export async function fetchUserApiKeys(userId: number): Promise<UserApiKey[]> {
  const { data } = await apiClient.get<UserApiKey[]>(`/api/v1/users/${userId}/api-keys`);
  return data;
}
```

- [ ] **Step 3: 新增 React Query hooks**

在 `useTeams.ts` 中新增：

```typescript
/** 查询当前用户的 API Key 列表 */
export function useMyApiKeys() {
  return useQuery({
    queryKey: ['my-api-keys'],
    queryFn: fetchMyApiKeys,
  });
}

/** 查询指定用户的 API Key 列表（仅 ADMIN） */
export function useUserApiKeys(userId: number) {
  return useQuery({
    queryKey: ['user-api-keys', userId],
    queryFn: () => fetchUserApiKeys(userId),
    enabled: !!userId,
  });
}
```

- [ ] **Step 4: 验证前端编译**

Run: `cd /mnt/e/workspace/llm-gateway/gateway-console && npx tsc --noEmit`
Expected: 无类型错误

- [ ] **Step 5: 提交**

```bash
git add gateway-console/src/types/team.ts \
  gateway-console/src/services/api/team.ts \
  gateway-console/src/services/query/useTeams.ts
git commit -m "feat: 前端类型和 API 新增 teamName、fetchMyApiKeys、fetchUserApiKeys"
```

---

## Task 8: 前端 — 新增"我的密钥"页面

**Files:**
- Create: `gateway-console/src/pages/MyApiKeys/index.tsx`

- [ ] **Step 1: 创建 MyApiKeys 页面组件**

```tsx
import { useState } from 'react';
import { Table, Button, Space, Tag, Card, Typography, App, Popconfirm, Form, Input, InputNumber, Select, Modal } from 'antd';
import { PlusOutlined, CopyOutlined, KeyOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import type { UserApiKey, CreateUserApiKeyRequest } from '@/types/team';
import { useMyApiKeys, useCreateUserApiKey, useDeleteUserApiKey } from '@/services/query/useTeams';
import { useProviders } from '@/services/query/useProviders';
import { useProducts } from '@/services/query/useProducts';
import { useTeams } from '@/services/query/useTeams';

const { Text, Paragraph, Title } = Typography;

/** 按团队分组展示 Key */
function groupByTeam(apiKeys: UserApiKey[]): Record<string, { teamName: string; keys: UserApiKey[] }> {
  const groups: Record<string, { teamName: string; keys: UserApiKey[] }> = {};
  for (const key of apiKeys) {
    const teamId = String(key.teamId);
    if (!groups[teamId]) {
      groups[teamId] = { teamName: key.teamName ?? `团队 ${key.teamId}`, keys: [] };
    }
    groups[teamId].keys.push(key);
  }
  return groups;
}

export default function MyApiKeysPage() {
  const { t } = useTranslation('teams');
  const { message } = App.useApp();
  const [createOpen, setCreateOpen] = useState(false);
  const [createdKeyInfo, setCreatedKeyInfo] = useState<{ keyPrefix: string; apiKeyPlain: string } | null>(null);
  const [form] = Form.useForm();
  const [selectedProviderId, setSelectedProviderId] = useState<number | null>(null);

  const { data: apiKeys, isLoading } = useMyApiKeys();
  const createMutation = useCreateUserApiKey();
  const deleteMutation = useDeleteUserApiKey();

  const { data: providersData } = useProviders();
  const providers = providersData?.items ?? [];
  const { data: products } = useProducts(selectedProviderId ?? 0);
  const { data: teams } = useTeams();

  const handleCreate = async () => {
    try {
      const values = await form.validateFields();
      const req: CreateUserApiKeyRequest = {
        teamId: values.teamId,
        userId: values.userId,
        productId: values.productId,
        name: values.name,
        models: values.models,
        quotaLimit: values.quotaLimit,
      };
      const result = await createMutation.mutateAsync({ teamId: values.teamId, data: req });
      setCreatedKeyInfo({ keyPrefix: result.keyPrefix, apiKeyPlain: result.apiKeyPlain });
      setCreateOpen(false);
      form.resetFields();
      setSelectedProviderId(null);
    } catch {
      // 表单验证失败
    }
  };

  const handleDelete = async (teamId: number, keyId: number) => {
    await deleteMutation.mutateAsync({ teamId, id: keyId });
    message.success(t('apiKey.deleteSuccess', { defaultValue: '密钥已删除' }));
  };

  const handleCopy = (apiKeyPlain: string) => {
    navigator.clipboard.writeText(apiKeyPlain);
    message.success(t('apiKey.copied', { defaultValue: '已复制到剪贴板' }));
  };

  const stateColorMap: Record<string, string> = {
    ACTIVE: 'green',
    INACTIVE: 'orange',
    DELETED: 'red',
  };

  const columns = [
    {
      title: t('apiKey.name', { defaultValue: '名称' }),
      dataIndex: 'name',
      key: 'name',
    },
    {
      title: t('apiKey.prefix', { defaultValue: '前缀' }),
      dataIndex: 'keyPrefix',
      key: 'keyPrefix',
      render: (prefix: string) => <Text code>{prefix}...</Text>,
    },
    {
      title: t('apiKey.models', { defaultValue: '可用模型' }),
      dataIndex: 'models',
      key: 'models',
      render: (models: string[]) =>
        models?.length > 0
          ? models.map((m) => <Tag key={m}>{m}</Tag>)
          : <Tag>{t('apiKey.allModels', { defaultValue: '全部' })}</Tag>,
    },
    {
      title: t('apiKey.quotaLimit', { defaultValue: '额度限制' }),
      dataIndex: 'quotaLimit',
      key: 'quotaLimit',
      render: (v: number | null) => v ?? t('apiKey.unlimited', { defaultValue: '无限制' }),
    },
    {
      title: t('apiKey.state', { defaultValue: '状态' }),
      dataIndex: 'state',
      key: 'state',
      render: (state: string) => <Tag color={stateColorMap[state]}>{state}</Tag>,
    },
    {
      title: t('apiKey.actions', { defaultValue: '操作' }),
      key: 'actions',
      render: (_: unknown, record: UserApiKey) => (
        <Space>
          <Button type="link" size="small" icon={<CopyOutlined />} onClick={() => handleCopy(record.keyPrefix)}>
            {t('apiKey.copy', { defaultValue: '复制' })}
          </Button>
          {record.state !== 'DELETED' && (
            <Popconfirm
              title={t('apiKey.deleteConfirm', { defaultValue: '确定删除此密钥？' })}
              onConfirm={() => handleDelete(record.teamId, record.id)}
            >
              <Button type="link" size="small" danger>
                {t('apiKey.delete', { defaultValue: '删除' })}
              </Button>
            </Popconfirm>
          )}
        </Space>
      ),
    },
  ];

  const groups = groupByTeam(apiKeys ?? []);

  return (
    <div style={{ padding: 24 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
        <Title level={4} style={{ margin: 0 }}>
          <KeyOutlined /> {t('myApiKeys.title', { defaultValue: '我的密钥' })}
        </Title>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => setCreateOpen(true)}>
          {t('apiKey.create', { defaultValue: '创建密钥' })}
        </Button>
      </div>

      {Object.entries(groups).map(([teamId, group]) => (
        <Card
          key={teamId}
          title={group.teamName}
          style={{ marginBottom: 16 }}
          size="small"
        >
          <Table
            columns={columns}
            dataSource={group.keys.filter((k) => k.state !== 'DELETED')}
            rowKey="id"
            loading={isLoading}
            size="small"
            pagination={false}
          />
        </Card>
      ))}

      {apiKeys?.length === 0 && !isLoading && (
        <Card>
          <Text type="secondary">{t('myApiKeys.empty', { defaultValue: '暂无密钥' })}</Text>
        </Card>
      )}

      {/* 创建密钥弹窗 */}
      <Modal
        title={t('apiKey.createTitle', { defaultValue: '创建用户密钥' })}
        open={createOpen}
        onOk={handleCreate}
        onCancel={() => { setCreateOpen(false); form.resetFields(); setSelectedProviderId(null); }}
        confirmLoading={createMutation.isPending}
      >
        <Form form={form} layout="vertical">
          <Form.Item
            name="teamId"
            label={t('apiKey.selectTeam', { defaultValue: '选择团队' })}
            rules={[{ required: true, message: t('apiKey.teamRequired', { defaultValue: '请选择团队' }) }]}
          >
            <Select
              placeholder={t('apiKey.selectTeamPlaceholder', { defaultValue: '请选择归属团队' })}
              options={teams.map((t) => ({ label: t.name, value: t.id }))}
            />
          </Form.Item>
          <Form.Item
            name="providerId"
            label={t('apiKey.selectProvider', { defaultValue: '选择供应商' })}
            rules={[{ required: true, message: t('apiKey.providerRequired', { defaultValue: '请选择供应商' }) }]}
          >
            <Select
              placeholder={t('apiKey.selectProviderPlaceholder', { defaultValue: '请先选择供应商' })}
              options={providers.map((p) => ({ label: p.providerName, value: p.id }))}
              onChange={(value: number) => setSelectedProviderId(value)}
            />
          </Form.Item>
          <Form.Item
            name="productId"
            label={t('apiKey.selectProduct', { defaultValue: '选择产品' })}
            rules={[{ required: true, message: t('apiKey.productRequired', { defaultValue: '请选择产品' }) }]}
          >
            <Select
              placeholder={t('apiKey.selectProductPlaceholder', { defaultValue: '请选择关联的产品' })}
              options={(products ?? []).map((p) => ({ label: p.name, value: p.id }))}
              disabled={!selectedProviderId}
            />
          </Form.Item>
          <Form.Item name="name" label={t('apiKey.name', { defaultValue: '名称' })}
            rules={[{ required: true, message: t('apiKey.nameRequired', { defaultValue: '请输入密钥名称' }) }]}
          >
            <Input placeholder={t('apiKey.namePlaceholder', { defaultValue: '例如：开发环境密钥' })} />
          </Form.Item>
          <Form.Item name="models" label={t('apiKey.models', { defaultValue: '可用模型' })}
            extra={t('apiKey.modelsExtra', { defaultValue: '留空表示允许访问所有模型' })}
          >
            <Select mode="tags" placeholder={t('apiKey.modelsPlaceholder', { defaultValue: '输入模型名称后按回车' })} />
          </Form.Item>
          <Form.Item name="quotaLimit" label={t('apiKey.quotaLimit', { defaultValue: '额度限制' })}
            extra={t('apiKey.quotaExtra', { defaultValue: '留空表示无限制' })}
          >
            <InputNumber min={0} style={{ width: '100%' }} />
          </Form.Item>
        </Form>
      </Modal>

      {/* 创建成功 — 显示明文 Key */}
      <Modal
        title={t('apiKey.createdTitle', { defaultValue: '密钥创建成功' })}
        open={!!createdKeyInfo}
        onOk={() => setCreatedKeyInfo(null)}
        onCancel={() => setCreatedKeyInfo(null)}
        okText={t('apiKey.createdOk', { defaultValue: '我已保存' })}
        cancelButtonProps={{ style: { display: 'none' } }}
      >
        <Paragraph type="warning" style={{ marginBottom: 12 }}>
          {t('apiKey.createdHint', { defaultValue: '请立即复制并保存此密钥，关闭后将无法再次查看！' })}
        </Paragraph>
        <Input.TextArea value={createdKeyInfo?.apiKeyPlain ?? ''} readOnly rows={3} />
        <Button
          icon={<CopyOutlined />}
          style={{ marginTop: 8 }}
          onClick={() => handleCopy(createdKeyInfo?.apiKeyPlain ?? '')}
        >
          {t('apiKey.copy', { defaultValue: '复制密钥' })}
        </Button>
      </Modal>
    </div>
  );
}
```

- [ ] **Step 2: 验证前端编译**

Run: `cd /mnt/e/workspace/llm-gateway/gateway-console && npx tsc --noEmit`
Expected: 无类型错误

- [ ] **Step 3: 提交**

```bash
git add gateway-console/src/pages/MyApiKeys/index.tsx
git commit -m "feat: 新增'我的密钥'页面 — 按团队分组展示，支持创建/复制/删除"
```

---

## Task 9: 前端 — 用户列表页"管理密钥"弹窗

**Files:**
- Create: `gateway-console/src/components/UserApiKeyManageModal.tsx`
- Modify: `gateway-console/src/pages/Users/index.tsx`

- [ ] **Step 1: 创建 UserApiKeyManageModal 组件**

此组件与 `Teams/UserApiKeyManageModal.tsx` 类似，但数据来源是 `useUserApiKeys(userId)`，且按团队分组展示。支持创建/查看/复制/吊销。

```tsx
import { useState } from 'react';
import { Modal, Table, Button, Space, Tag, Popconfirm, Form, Input, InputNumber, Select, Typography, App, Card } from 'antd';
import { PlusOutlined, DeleteOutlined, CopyOutlined, KeyOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import type { UserApiKey, CreateUserApiKeyRequest } from '@/types/team';
import { useUserApiKeys, useCreateUserApiKey, useDeleteUserApiKey, useTeams } from '@/services/query/useTeams';
import { useProviders } from '@/services/query/useProviders';
import { useProducts } from '@/services/query/useProducts';

const { Text, Paragraph } = Typography;

interface Props {
  userId: number;
  open: boolean;
  onClose: () => void;
}

/** 按团队分组 */
function groupByTeam(apiKeys: UserApiKey[]): Record<string, { teamName: string; keys: UserApiKey[] }> {
  const groups: Record<string, { teamName: string; keys: UserApiKey[] }> = {};
  for (const key of apiKeys) {
    const teamId = String(key.teamId);
    if (!groups[teamId]) {
      groups[teamId] = { teamName: key.teamName ?? `团队 ${key.teamId}`, keys: [] };
    }
    groups[teamId].keys.push(key);
  }
  return groups;
}

export default function UserApiKeyManageModal({ userId, open, onClose }: Props) {
  const { t } = useTranslation('teams');
  const { message } = App.useApp();
  const [form] = Form.useForm();
  const [createOpen, setCreateOpen] = useState(false);
  const [createdKeyInfo, setCreatedKeyInfo] = useState<{ keyPrefix: string; apiKeyPlain: string } | null>(null);
  const [selectedProviderId, setSelectedProviderId] = useState<number | null>(null);

  const { data: apiKeys, isLoading } = useUserApiKeys(userId);
  const createMutation = useCreateUserApiKey();
  const deleteMutation = useDeleteUserApiKey();

  const { data: providersData } = useProviders();
  const providers = providersData?.items ?? [];
  const { data: products } = useProducts(selectedProviderId ?? 0);
  const { data: teams } = useTeams();

  const handleCreate = async () => {
    try {
      const values = await form.validateFields();
      const req: CreateUserApiKeyRequest = {
        teamId: values.teamId,
        userId,
        productId: values.productId,
        name: values.name,
        models: values.models,
        quotaLimit: values.quotaLimit,
      };
      const result = await createMutation.mutateAsync({ teamId: values.teamId, data: req });
      setCreatedKeyInfo({ keyPrefix: result.keyPrefix, apiKeyPlain: result.apiKeyPlain });
      setCreateOpen(false);
      form.resetFields();
      setSelectedProviderId(null);
    } catch {
      // 表单验证失败
    }
  };

  const handleDelete = async (teamId: number, keyId: number) => {
    await deleteMutation.mutateAsync({ teamId, id: keyId });
    message.success(t('apiKey.deleteSuccess', { defaultValue: '密钥已删除' }));
  };

  const stateColorMap: Record<string, string> = {
    ACTIVE: 'green',
    INACTIVE: 'orange',
    DELETED: 'red',
  };

  const columns = [
    { title: t('apiKey.name', { defaultValue: '名称' }), dataIndex: 'name', key: 'name' },
    {
      title: t('apiKey.prefix', { defaultValue: '前缀' }), dataIndex: 'keyPrefix', key: 'keyPrefix',
      render: (prefix: string) => <Text code>{prefix}...</Text>,
    },
    {
      title: t('apiKey.models', { defaultValue: '可用模型' }), dataIndex: 'models', key: 'models',
      render: (models: string[]) =>
        models?.length > 0 ? models.map((m) => <Tag key={m}>{m}</Tag>) : <Tag>{t('apiKey.allModels', { defaultValue: '全部' })}</Tag>,
    },
    {
      title: t('apiKey.state', { defaultValue: '状态' }), dataIndex: 'state', key: 'state',
      render: (state: string) => <Tag color={stateColorMap[state]}>{state}</Tag>,
    },
    {
      title: t('apiKey.actions', { defaultValue: '操作' }), key: 'actions',
      render: (_: unknown, record: UserApiKey) => (
        <Space>
          <Button type="link" size="small" icon={<CopyOutlined />}
            onClick={() => { navigator.clipboard.writeText(record.keyPrefix); message.success(t('apiKey.copied', { defaultValue: '已复制' })); }}>
            {t('apiKey.copy', { defaultValue: '复制' })}
          </Button>
          {record.state !== 'DELETED' && (
            <Popconfirm title={t('apiKey.deleteConfirm', { defaultValue: '确定删除此密钥？' })} onConfirm={() => handleDelete(record.teamId, record.id)}>
              <Button type="link" size="small" danger icon={<DeleteOutlined />}>{t('apiKey.delete', { defaultValue: '删除' })}</Button>
            </Popconfirm>
          )}
        </Space>
      ),
    },
  ];

  const groups = groupByTeam(apiKeys ?? []);

  return (
    <>
      <Modal
        title={<Space><KeyOutlined />{t('userApiKeys.manageTitle', { defaultValue: '管理密钥' })}</Space>}
        open={open}
        onCancel={onClose}
        width={800}
        footer={null}
      >
        <div style={{ marginBottom: 16 }}>
          <Button type="primary" icon={<PlusOutlined />} onClick={() => setCreateOpen(true)}>
            {t('apiKey.create', { defaultValue: '创建密钥' })}
          </Button>
        </div>

        {Object.entries(groups).map(([teamId, group]) => (
          <Card key={teamId} title={group.teamName} style={{ marginBottom: 12 }} size="small">
            <Table
              columns={columns}
              dataSource={group.keys.filter((k) => k.state !== 'DELETED')}
              rowKey="id"
              loading={isLoading}
              size="small"
              pagination={false}
            />
          </Card>
        ))}

        {apiKeys?.length === 0 && !isLoading && (
          <Text type="secondary">{t('userApiKeys.empty', { defaultValue: '该用户暂无密钥' })}</Text>
        )}
      </Modal>

      {/* 创建密钥弹窗 */}
      <Modal
        title={t('apiKey.createTitle', { defaultValue: '创建用户密钥' })}
        open={createOpen}
        onOk={handleCreate}
        onCancel={() => { setCreateOpen(false); form.resetFields(); setSelectedProviderId(null); }}
        confirmLoading={createMutation.isPending}
      >
        <Form form={form} layout="vertical">
          <Form.Item name="teamId" label={t('apiKey.selectTeam', { defaultValue: '选择团队' })}
            rules={[{ required: true, message: t('apiKey.teamRequired', { defaultValue: '请选择团队' }) }]}>
            <Select placeholder={t('apiKey.selectTeamPlaceholder', { defaultValue: '请选择归属团队' })}
              options={teams.map((t) => ({ label: t.name, value: t.id }))} />
          </Form.Item>
          <Form.Item name="providerId" label={t('apiKey.selectProvider', { defaultValue: '选择供应商' })}
            rules={[{ required: true, message: t('apiKey.providerRequired', { defaultValue: '请选择供应商' }) }]}>
            <Select placeholder={t('apiKey.selectProviderPlaceholder', { defaultValue: '请先选择供应商' })}
              options={providers.map((p) => ({ label: p.providerName, value: p.id }))}
              onChange={(value: number) => setSelectedProviderId(value)} />
          </Form.Item>
          <Form.Item name="productId" label={t('apiKey.selectProduct', { defaultValue: '选择产品' })}
            rules={[{ required: true, message: t('apiKey.productRequired', { defaultValue: '请选择产品' }) }]}>
            <Select placeholder={t('apiKey.selectProductPlaceholder', { defaultValue: '请选择关联的产品' })}
              options={(products ?? []).map((p) => ({ label: p.name, value: p.id }))}
              disabled={!selectedProviderId} />
          </Form.Item>
          <Form.Item name="name" label={t('apiKey.name', { defaultValue: '名称' })}
            rules={[{ required: true, message: t('apiKey.nameRequired', { defaultValue: '请输入密钥名称' }) }]}>
            <Input placeholder={t('apiKey.namePlaceholder', { defaultValue: '例如：开发环境密钥' })} />
          </Form.Item>
          <Form.Item name="models" label={t('apiKey.models', { defaultValue: '可用模型' })}>
            <Select mode="tags" placeholder={t('apiKey.modelsPlaceholder', { defaultValue: '输入模型名称后按回车' })} />
          </Form.Item>
          <Form.Item name="quotaLimit" label={t('apiKey.quotaLimit', { defaultValue: '额度限制' })}>
            <InputNumber min={0} style={{ width: '100%' }} />
          </Form.Item>
        </Form>
      </Modal>

      {/* 创建成功弹窗 */}
      <Modal
        title={t('apiKey.createdTitle', { defaultValue: '密钥创建成功' })}
        open={!!createdKeyInfo}
        onOk={() => setCreatedKeyInfo(null)}
        onCancel={() => setCreatedKeyInfo(null)}
        okText={t('apiKey.createdOk', { defaultValue: '我已保存' })}
        cancelButtonProps={{ style: { display: 'none' } }}
      >
        <Paragraph type="warning" style={{ marginBottom: 12 }}>
          {t('apiKey.createdHint', { defaultValue: '请立即复制并保存此密钥，关闭后将无法再次查看！' })}
        </Paragraph>
        <Input.TextArea value={createdKeyInfo?.apiKeyPlain ?? ''} readOnly rows={3} />
        <Button icon={<CopyOutlined />} style={{ marginTop: 8 }}
          onClick={() => { navigator.clipboard.writeText(createdKeyInfo?.apiKeyPlain ?? ''); message.success(t('apiKey.copied', { defaultValue: '已复制到剪贴板' })); }}>
          {t('apiKey.copy', { defaultValue: '复制密钥' })}
        </Button>
      </Modal>
    </>
  );
}
```

- [ ] **Step 2: 在 Users/index.tsx 增加"管理密钥"按钮**

在用户列表的操作列中增加"管理密钥"按钮：

```tsx
// 新增 import
import UserApiKeyManageModal from '@/components/UserApiKeyManageModal';

// 在组件内新增 state
const [manageKeyUserId, setManageKeyUserId] = useState<number | null>(null);

// 在操作列的 Space 中增加按钮
<Button
  type="link"
  size="small"
  icon={<KeyOutlined />}
  onClick={() => setManageKeyUserId(record.id)}
>
  {t('user.manageApiKeys', { defaultValue: '管理密钥' })}
</Button>

// 在组件末尾增加弹窗
<UserApiKeyManageModal
  userId={manageKeyUserId ?? 0}
  open={!!manageKeyUserId}
  onClose={() => setManageKeyUserId(null)}
/>
```

- [ ] **Step 3: 验证前端编译**

Run: `cd /mnt/e/workspace/llm-gateway/gateway-console && npx tsc --noEmit`
Expected: 无类型错误

- [ ] **Step 4: 提交**

```bash
git add gateway-console/src/components/UserApiKeyManageModal.tsx \
  gateway-console/src/pages/Users/index.tsx
git commit -m "feat: 用户列表页增加'管理密钥'弹窗 — 支持创建/查看/复制/吊销"
```

---

## Task 10: 前端 — 路由和侧边栏导航

**Files:**
- Modify: `gateway-console/src/router/index.tsx`
- Modify: `gateway-console/src/components/layout/AppLayout.tsx`

- [ ] **Step 1: 新增 /my-api-keys 路由**

在路由配置中新增：

```tsx
// router/index.tsx — 新增 import
import MyApiKeys from '@/pages/MyApiKeys';

// 在路由列表中新增
{
  path: '/my-api-keys',
  element: <MyApiKeys />,
}
```

- [ ] **Step 2: 侧边栏增加"我的密钥"入口**

在 AppLayout.tsx 的菜单项中，在"团队管理"后新增：

```tsx
{
  key: '/my-api-keys',
  icon: <KeyOutlined />,
  label: '我的密钥',
}
```

- [ ] **Step 3: 验证前端编译**

Run: `cd /mnt/e/workspace/llm-gateway/gateway-console && npx tsc --noEmit`
Expected: 无类型错误

- [ ] **Step 4: 提交**

```bash
git add gateway-console/src/router/index.tsx \
  gateway-console/src/components/layout/AppLayout.tsx
git commit -m "feat: 新增'我的密钥'路由和侧边栏导航入口"
```

---

## Task 11: 前端 — 创建表单 userId 输入改为团队成员选择器

**Files:**
- Modify: `gateway-console/src/pages/Teams/UserApiKeyManageModal.tsx`

- [ ] **Step 1: 替换 userId InputNumber 为团队成员选择器**

在团队 Key 管理弹窗的创建表单中，将现有的 `userId` InputNumber 替换为团队成员选择器。

需要先获取当前团队成员列表。在组件中新增：

```tsx
// 新增 import
import { useTeamMembers } from '@/services/query/useTeams';

// 在组件内获取团队成员
const { data: members } = useTeamMembers(team.id);

// 替换 userId Form.Item
<Form.Item
  name="userId"
  label={t('apiKey.selectUser', { defaultValue: '选择用户' })}
  rules={[{ required: true, message: t('apiKey.userRequired', { defaultValue: '请选择用户' }) }]}
>
  <Select
    placeholder={t('apiKey.selectUserPlaceholder', { defaultValue: '请选择密钥所属用户' })}
    options={(members ?? []).map((m) => ({
      label: `${m.username} (ID: ${m.id})`,
      value: m.id,
    }))}
  />
</Form.Item>
```

注意：需要确认 `useTeamMembers` hook 是否存在。如果不存在，需要新增 API 函数和 hook，或者使用 `useUsers` 获取团队成员。

- [ ] **Step 2: 验证前端编译**

Run: `cd /mnt/e/workspace/llm-gateway/gateway-console && npx tsc --noEmit`
Expected: 无类型错误

- [ ] **Step 3: 提交**

```bash
git add gateway-console/src/pages/Teams/UserApiKeyManageModal.tsx
git commit -m "feat: 团队 Key 创建表单 userId 改为团队成员选择器"
```

---

## Task 12: 集成测试和最终验证

**Files:** 无新文件

- [ ] **Step 1: 后端编译和测试**

Run: `cd /mnt/e/workspace/llm-gateway && ./mvnw clean test -pl gateway-boot -q`
Expected: 所有测试通过

- [ ] **Step 2: 前端编译**

Run: `cd /mnt/e/workspace/llm-gateway/gateway-console && npx tsc --noEmit`
Expected: 无类型错误

- [ ] **Step 3: 启动后端验证 API**

Run: `cd /mnt/e/workspace/llm-gateway && ./mvnw spring-boot:run -pl gateway-boot`

验证端点：
- `GET /api/v1/me/api-keys` — 返回当前用户 Key 列表（含 teamName）
- `GET /api/v1/users/{userId}/api-keys` — 返回指定用户 Key 列表（仅 ADMIN）
- `POST /api/v1/teams/{teamId}/api-keys` — 创建时校验 userId 权限

- [ ] **Step 4: 启动前端验证页面**

Run: `cd /mnt/e/workspace/llm-gateway/gateway-console && npm run dev`

验证：
- 侧边栏出现"我的密钥"入口
- `/my-api-keys` 页面按团队分组展示 Key
- 用户列表页"管理密钥"弹窗正常工作
- 团队 Key 创建表单使用团队成员选择器

- [ ] **Step 5: 最终提交（如有修复）**

```bash
git add -A
git commit -m "fix: 集成测试修复"
```
