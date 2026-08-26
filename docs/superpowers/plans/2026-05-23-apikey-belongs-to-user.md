# 密钥归属重构：密钥归于用户 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 UserApiKey 的归属从团队改为用户，并在用户列表页增加密钥管理入口

**Architecture:** UserApiKey 去掉 teamId，密钥归属用户、权限由 productIds 定义；团队通过 UserTeam 关系间接影响密钥（预算约束、可见性）；认证链路中 teamId 改为从 userId 推导；前端新增 UserApiKeyModal 组件

**Tech Stack:** Java 21 + Spring Boot 3.5.x + JPA + Sa-Token（后端），React + Ant Design + React Query + i18next（前端）

---

## 文件结构

### 后端 — 修改
- `gateway-boot/src/main/java/com/codingas/gateway/domain/team/entity/UserApiKey.java` — 删除 teamId 字段
- `gateway-boot/src/main/java/com/codingas/gateway/domain/team/gateway/UserApiKeyGateway.java` — 删除 findByTeamId / countByTeamId
- `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/team/gateway/UserApiKeyGatewayImpl.java` — 删除 teamId 映射和 findByTeamId
- `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/team/gateway/database/dataobject/UserApiKeyDo.java` — 删除 teamId 列
- `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/team/gateway/database/repository/UserApiKeyRepository.java` — 删除 findByTeamId / countByTeamId
- `gateway-boot/src/main/java/com/codingas/gateway/application/userapikey/dto/UserApiKeyCreateRequest.java` — 删除 teamId 字段
- `gateway-boot/src/main/java/com/codingas/gateway/application/userapikey/dto/UserApiKeyResponse.java` — 删除 teamId 字段
- `gateway-boot/src/main/java/com/codingas/gateway/application/userapikey/dto/UserApiKeyDetailResponse.java` — 删除 teamId 字段
- `gateway-boot/src/main/java/com/codingas/gateway/application/userapikey/dto/UserApiKeySummaryResponse.java` — 删除 teamId 字段
- `gateway-boot/src/main/java/com/codingas/gateway/application/userapikey/UserApiKeyService.java` — 删除 listByTeamId / getDetailByIdAndTeamId
- `gateway-boot/src/main/java/com/codingas/gateway/application/userapikey/UserApiKeyServiceImpl.java` — 删除 teamId 逻辑
- `gateway-boot/src/main/java/com/codingas/gateway/adapter/api/UserApiKeyController.java` — 删除 team/{teamId} 端点
- `gateway-boot/src/main/java/com/codingas/gateway/adapter/api/TeamController.java` — API Key 端点改为通过成员推导
- `gateway-boot/src/main/java/com/codingas/gateway/domain/security/service/UserAuthResult.java` — 删除 teamId 字段
- `gateway-boot/src/main/java/com/codingas/gateway/domain/security/service/AuthenticationDomainService.java` — 不再从 Key 取 teamId
- `gateway-boot/src/main/java/com/codingas/gateway/domain/proxy/entity/RoutingContext.java` — 删除 teamId 字段
- `gateway-boot/src/main/java/com/codingas/gateway/application/proxy/ProductRoutingService.java` — 不再设置 teamId
- `gateway-boot/src/main/java/com/codingas/gateway/domain/usage/event/TokenUsedEvent.java` — 保留 teamId（从 UserTeam 推导填入）
- `gateway-boot/src/main/java/com/codingas/gateway/common/event/AuditEvent.java` — 保留 teamId（从 UserTeam 推导填入）
- `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/init/DataInitializer.java` — 删除 setTeamId

### 后端 — 新增
- `gateway-boot/src/main/resources/db/migration/V32__drop_team_id_from_user_api_keys.sql` — 数据库迁移

### 后端 — 测试修改
- `gateway-boot/src/test/java/com/codingas/gateway/application/userapikey/UserApiKeyServiceImplTest.java`
- `gateway-boot/src/test/java/com/codingas/gateway/domain/security/service/AuthenticationDomainServiceTest.java`
- `gateway-boot/src/test/java/com/codingas/gateway/domain/security/service/UserAuthResultTest.java`
- `gateway-boot/src/test/java/com/codingas/gateway/adapter/api/TeamControllerUserApiKeyTest.java`

### 前端 — 修改
- `gateway-console/src/types/team.ts` — UserApiKey / CreateUserApiKeyRequest 删除 teamId
- `gateway-console/src/services/api/team.ts` — createApiKey body 删除 teamId
- `gateway-console/src/services/query/useTeams.ts` — 创建 Key mutation 删除 teamId
- `gateway-console/src/pages/Teams/UserApiKeyManageModal.tsx` — 创建表单去掉 teamId
- `gateway-console/src/locales/zh-CN/users.json` — 补充密钥管理文案
- `gateway-console/src/locales/en-US/users.json` — 补充密钥管理文案

### 前端 — 新增
- `gateway-console/src/pages/Users/UserApiKeyModal.tsx` — 用户密钥管理弹窗
- `gateway-console/src/services/api/userApiKey.ts` — 用户维度 API Key API
- `gateway-console/src/services/query/useUserApiKeys.ts` — 用户维度 API Key hooks

---

### Task 1: 后端实体 — UserApiKey 删除 teamId

**Files:**
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/domain/team/entity/UserApiKey.java`

- [ ] **Step 1: 删除 teamId 字段及相关 getter/setter**

删除以下代码：
```java
private Long teamId;
```
```java
public Long getTeamId() { return teamId; }
public void setTeamId(Long teamId) { this.teamId = teamId; }
```

同时更新 `toString()` 方法，删除 `teamId=` 部分：
```java
return "UserApiKey{" +
        "id=" + id +
        ", userId=" + userId +
        ", productIds=" + productIds +
        ", keyPrefix='" + keyPrefix + '\'' +
        ", name='" + name + '\'' +
        ", state=" + state +
        '}';
```

- [ ] **Step 2: 编译验证**

Run: `cd E:/workspace/llm-gateway && ./mvnw compile -pl gateway-boot -q 2>&1 | head -30`
Expected: 编译错误（因为其他文件还在引用 teamId），确认修改已生效

- [ ] **Step 3: 提交**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/domain/team/entity/UserApiKey.java
git commit -m "refactor(domain): UserApiKey 移除 teamId 字段，密钥归于用户"
```

---

### Task 2: 后端 Gateway 接口 — 删除 teamId 查询方法

**Files:**
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/domain/team/gateway/UserApiKeyGateway.java`

- [ ] **Step 1: 删除 findByTeamId 和 countByTeamId 方法声明**

从 `UserApiKeyGateway` 接口中删除：
```java
/** 按团队 ID 查找 */
List<UserApiKey> findByTeamId(Long teamId);
```
```java
/** 按团队 ID 统计 */
long countByTeamId(Long teamId);
```

保留的方法：`findById`, `findByUserId`, `findByKeyPrefix`, `save`, `deleteById`, `findIdsByProductId`

- [ ] **Step 2: 提交**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/domain/team/gateway/UserApiKeyGateway.java
git commit -m "refactor(domain): UserApiKeyGateway 移除 teamId 查询方法"
```

---

### Task 3: 后端基础设施层 — 删除 teamId 映射和查询实现

**Files:**
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/team/gateway/database/dataobject/UserApiKeyDo.java`
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/team/gateway/database/repository/UserApiKeyRepository.java`
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/team/gateway/UserApiKeyGatewayImpl.java`

- [ ] **Step 1: UserApiKeyDo 删除 teamId 列**

从 `UserApiKeyDo` 中删除：
```java
@Column(name = "team_id", nullable = false)
private Long teamId;
```
```java
public Long getTeamId() { return teamId; }
public void setTeamId(Long teamId) { this.teamId = teamId; }
```

- [ ] **Step 2: UserApiKeyRepository 删除 findByTeamId 和 countByTeamId**

从 `UserApiKeyRepository` 中删除：
```java
List<UserApiKeyDo> findByTeamId(Long teamId);
```
```java
long countByTeamId(Long teamId);
```

- [ ] **Step 3: UserApiKeyGatewayImpl 删除 teamId 相关实现**

从 `UserApiKeyGatewayImpl` 中删除：
```java
@Override
public List<UserApiKey> findByTeamId(Long teamId) {
    return repository.findByTeamId(teamId).stream()
            .map(this::toEntity)
            .toList();
}
```
```java
@Override
public long countByTeamId(Long teamId) {
    return repository.countByTeamId(teamId);
}
```

在 `toEntity()` 方法中删除：
```java
entity.setTeamId(dataObject.getTeamId());
```

在 `toDataObject()` 方法中删除：
```java
dataObject.setTeamId(entity.getTeamId());
```

- [ ] **Step 4: 编译验证**

Run: `cd E:/workspace/llm-gateway && ./mvnw compile -pl gateway-boot -q 2>&1 | head -30`
Expected: 编译错误（应用层仍引用 teamId），确认基础设施层修改完成

- [ ] **Step 5: 提交**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/infrastructure/team/gateway/database/dataobject/UserApiKeyDo.java
git add gateway-boot/src/main/java/com/codingas/gateway/infrastructure/team/gateway/database/repository/UserApiKeyRepository.java
git add gateway-boot/src/main/java/com/codingas/gateway/infrastructure/team/gateway/UserApiKeyGatewayImpl.java
git commit -m "refactor(infra): UserApiKey 数据层移除 team_id 列和查询"
```

---

### Task 4: 后端 DTO — 删除 teamId 字段

**Files:**
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/application/userapikey/dto/UserApiKeyCreateRequest.java`
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/application/userapikey/dto/UserApiKeyResponse.java`
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/application/userapikey/dto/UserApiKeyDetailResponse.java`
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/application/userapikey/dto/UserApiKeySummaryResponse.java`

- [ ] **Step 1: UserApiKeyCreateRequest 删除 teamId**

重构前：
```java
public record UserApiKeyCreateRequest(
        @NotNull(message = "团队 ID 不能为空")
        Long teamId,
        @NotNull(message = "用户 ID 不能为空")
        Long userId,
        @NotEmpty(message = "至少需要关联一个产品")
        List<Long> productIds,
        @NotBlank(message = "密钥名称不能为空")
        String name,
        List<String> models,
        Long quotaLimit
) {}
```

重构后：
```java
public record UserApiKeyCreateRequest(
        @NotNull(message = "用户 ID 不能为空")
        Long userId,
        @NotEmpty(message = "至少需要关联一个产品")
        List<Long> productIds,
        @NotBlank(message = "密钥名称不能为空")
        String name,
        List<String> models,
        Long quotaLimit
) {}
```

- [ ] **Step 2: UserApiKeyResponse 删除 teamId**

重构前：
```java
public record UserApiKeyResponse(
        Long id,
        Long teamId,
        Long userId,
        ...
) {}
```

重构后：
```java
public record UserApiKeyResponse(
        Long id,
        Long userId,
        List<Long> productIds,
        List<ProductBrief> products,
        String keyPrefix,
        String name,
        List<String> models,
        Long quotaLimit,
        UserApiKeyState state,
        Instant createdAt,
        Instant updatedAt
) {}
```

- [ ] **Step 3: UserApiKeyDetailResponse 删除 teamId**

重构前：
```java
public record UserApiKeyDetailResponse(
        Long id,
        Long teamId,
        Long userId,
        ...
) {}
```

重构后：
```java
public record UserApiKeyDetailResponse(
        Long id,
        Long userId,
        List<Long> productIds,
        List<ProductBrief> products,
        String keyPrefix,
        String keyPlain,
        String name,
        List<String> models,
        Long quotaLimit,
        UserApiKeyState state,
        Instant createdAt,
        Instant updatedAt
) {}
```

- [ ] **Step 4: UserApiKeySummaryResponse 删除 teamId**

重构前：
```java
public record UserApiKeySummaryResponse(
        Long id,
        Long teamId,
        Long userId,
        ...
) {}
```

重构后：
```java
public record UserApiKeySummaryResponse(
        Long id,
        Long userId,
        List<Long> productIds,
        String keyPrefix,
        String name,
        List<String> models,
        Long quotaLimit,
        UserApiKeyState state,
        Instant createdAt,
        Instant updatedAt
) {}
```

- [ ] **Step 5: 提交**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/application/userapikey/dto/
git commit -m "refactor(dto): UserApiKey DTO 移除 teamId 字段"
```

---

### Task 5: 后端管理服务 — 删除 teamId 逻辑

**Files:**
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/application/userapikey/UserApiKeyService.java`
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/application/userapikey/UserApiKeyServiceImpl.java`

- [ ] **Step 1: UserApiKeyService 接口删除 teamId 相关方法**

删除：
```java
List<UserApiKeyResponse> listByTeamId(Long teamId);
```
```java
UserApiKeyDetailResponse getDetailByIdAndTeamId(Long id, Long teamId);
```

保留：`create`, `findByUserId`, `getById`, `getDetailById`, `update`, `delete`

- [ ] **Step 2: UserApiKeyServiceImpl 删除 teamId 相关实现**

删除 `listByTeamId` 方法：
```java
@Override
public List<UserApiKeyResponse> listByTeamId(Long teamId) {
    return userApiKeyGateway.findByTeamId(teamId).stream()
            .map(this::toResponse)
            .toList();
}
```

删除 `getDetailByIdAndTeamId` 方法：
```java
@Override
public UserApiKeyDetailResponse getDetailByIdAndTeamId(Long id, Long teamId) {
    UserApiKey apiKey = userApiKeyGateway.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("UserApiKey not found: id=" + id));
    if (!apiKey.getTeamId().equals(teamId)) {
        throw new IllegalArgumentException("UserApiKey does not belong to team: apiKeyId=" + id + ", teamId=" + teamId);
    }
    return toDetailResponse(apiKey);
}
```

修改 `create` 方法 — 不再设置 teamId：
```java
@Override
@Transactional
public UserApiKeyCreateResponse create(UserApiKeyCreateRequest request) {
    String plainKey = generateRawKey();
    String keyPrefix = plainKey.substring(0, Math.min(8, plainKey.length()));

    UserApiKey apiKey = new UserApiKey();
    apiKey.setUserId(request.userId());
    apiKey.setProductIds(request.productIds());
    apiKey.setKeyPrefix(keyPrefix);
    apiKey.setKeyPlain(plainKey);
    apiKey.setName(request.name());
    apiKey.setModels(request.models());
    apiKey.setQuotaLimit(request.quotaLimit());
    apiKey.setState(UserApiKeyState.ACTIVE);

    UserApiKey saved = userApiKeyGateway.save(apiKey);
    log.info("Created UserApiKey: id={}, userId={}, productIds={}",
            saved.getId(), saved.getUserId(), saved.getProductIds());

    return new UserApiKeyCreateResponse(saved.getId(), keyPrefix, plainKey);
}
```

修改 `toResponse` 方法 — 删除 teamId 参数：
```java
private UserApiKeyResponse toResponse(UserApiKey apiKey) {
    return new UserApiKeyResponse(
            apiKey.getId(),
            apiKey.getUserId(),
            apiKey.getProductIds(),
            toProductBriefs(apiKey.getProductIds()),
            apiKey.getKeyPrefix(),
            apiKey.getName(),
            apiKey.getModels(),
            apiKey.getQuotaLimit(),
            apiKey.getState(),
            apiKey.getCreatedAt(),
            apiKey.getUpdatedAt()
    );
}
```

修改 `toDetailResponse` 方法 — 删除 teamId 参数：
```java
private UserApiKeyDetailResponse toDetailResponse(UserApiKey apiKey) {
    return new UserApiKeyDetailResponse(
            apiKey.getId(),
            apiKey.getUserId(),
            apiKey.getProductIds(),
            toProductBriefs(apiKey.getProductIds()),
            apiKey.getKeyPrefix(),
            apiKey.getKeyPlain(),
            apiKey.getName(),
            apiKey.getModels(),
            apiKey.getQuotaLimit(),
            apiKey.getState(),
            apiKey.getCreatedAt(),
            apiKey.getUpdatedAt()
    );
}
```

- [ ] **Step 3: 编译验证**

Run: `cd E:/workspace/llm-gateway && ./mvnw compile -pl gateway-boot -q 2>&1 | head -30`
Expected: 编译错误（Controller 层仍引用 teamId），确认应用层修改完成

- [ ] **Step 4: 提交**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/application/userapikey/
git commit -m "refactor(app): UserApiKeyService 移除 teamId 逻辑"
```

---

### Task 6: 后端 Controller 层 — 删除团队维度 API

**Files:**
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/adapter/api/UserApiKeyController.java`
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/adapter/api/TeamController.java`

- [ ] **Step 1: UserApiKeyController 删除 team/{teamId} 端点**

删除：
```java
@GetMapping("/team/{teamId}")
public ResponseEntity<List<UserApiKeyResponse>> listByTeam(@PathVariable Long teamId) {
    return ResponseEntity.ok(userApiKeyService.listByTeamId(teamId));
}
```

- [ ] **Step 2: TeamController 重写 API Key 端点**

TeamController 中 API Key 部分改为：团队维度的 Key 列表通过查询团队成员的 Key 来推导。

替换 `listApiKeys` 方法：
```java
/**
 * 查询团队下所有成员的 API Key
 * <p>通过团队成员 userId 查询各自的 Key</p>
 */
@GetMapping("/{teamId}/api-keys")
public List<UserApiKeyResponse> listApiKeys(@PathVariable Long teamId) {
    List<Long> memberUserIds = teamService.getMemberUserIds(teamId);
    return memberUserIds.stream()
            .flatMap(userId -> userApiKeyService.findByUserId(userId).stream())
            .toList();
}
```

替换 `getApiKey` 方法 — 不再校验团队归属：
```java
/**
 * 查询单个 API Key 详情
 */
@GetMapping("/{teamId}/api-keys/{id}")
public UserApiKeyDetailResponse getApiKey(@PathVariable Long teamId, @PathVariable Long id) {
    return userApiKeyService.getDetailById(id);
}
```

替换 `createApiKey` 方法 — 不再需要 teamId：
```java
/**
 * 创建用户 API Key
 *
 * <p>管理员可以为团队下任意用户创建 Key，普通成员只能为自己创建。</p>
 */
@PostMapping("/{teamId}/api-keys")
public ResponseEntity<UserApiKeyCreateResponse> createApiKey(
        @PathVariable Long teamId,
        @Valid @RequestBody UserApiKeyCreateRequest request) {
    Long currentUserId = StpUtil.getLoginIdAsLong();
    boolean isAdmin = StpUtil.hasRole("ADMIN");

    Long targetUserId = request.userId() != null ? request.userId() : currentUserId;
    if (!isAdmin && !targetUserId.equals(currentUserId)) {
        throw new IllegalArgumentException("无权为其他用户创建 API Key");
    }

    UserApiKeyCreateResponse response = userApiKeyService.create(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
}
```

替换 `updateApiKey` 和 `deleteApiKey` — 不再传 teamId 到 service：
```java
@PutMapping("/{teamId}/api-keys/{id}")
public UserApiKeyResponse updateApiKey(
        @PathVariable Long teamId,
        @PathVariable Long id,
        @Valid @RequestBody UserApiKeyUpdateRequest request) {
    return userApiKeyService.update(id, request);
}

@DeleteMapping("/{teamId}/api-keys/{id}")
public ResponseEntity<Void> deleteApiKey(@PathVariable Long teamId, @PathVariable Long id) {
    userApiKeyService.delete(id);
    return ResponseEntity.noContent().build();
}
```

注意：需要在 `TeamService` 中添加 `getMemberUserIds(Long teamId)` 方法（见 Task 7）。

- [ ] **Step 3: 提交**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/adapter/api/UserApiKeyController.java
git add gateway-boot/src/main/java/com/codingas/gateway/adapter/api/TeamController.java
git commit -m "refactor(adapter): Controller 层移除团队维度 API Key 查询"
```

---

### Task 7: 后端 TeamService — 添加 getMemberUserIds 方法

**Files:**
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/application/team/TeamService.java`
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/application/team/TeamServiceImpl.java`

- [ ] **Step 1: TeamService 接口添加方法**

```java
/** 获取团队成员的用户 ID 列表 */
List<Long> getMemberUserIds(Long teamId);
```

- [ ] **Step 2: TeamServiceImpl 实现方法**

需要在 TeamServiceImpl 中注入 UserTeamGateway（如果还没有的话），通过 teamId 查询 UserTeam 列表，提取 userId：

```java
@Override
public List<Long> getMemberUserIds(Long teamId) {
    return userTeamGateway.findByTeamId(teamId).stream()
            .map(UserTeam::getUserId)
            .toList();
}
```

如果 `UserTeamGateway` 尚未有 `findByTeamId` 方法，需要同步添加。检查 `UserTeamGateway` 接口和实现。如果已存在则直接使用。

- [ ] **Step 3: 提交**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/application/team/
git commit -m "feat(app): TeamService 添加 getMemberUserIds 方法"
```

---

### Task 8: 后端认证链路 — UserAuthResult 删除 teamId

**Files:**
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/domain/security/service/UserAuthResult.java`
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/domain/security/service/AuthenticationDomainService.java`

- [ ] **Step 1: UserAuthResult 删除 teamId**

重构前：
```java
public record UserAuthResult(
        Long userId,
        String role,
        Long userApiKeyId,
        Long teamId
) {
    public static UserAuthResult newArch(Long userId, String role, Long userApiKeyId, Long teamId) {
        return new UserAuthResult(userId, role, userApiKeyId, teamId);
    }
}
```

重构后：
```java
public record UserAuthResult(
        Long userId,
        String role,
        Long userApiKeyId
) {
    /** 创建新架构认证结果 */
    public static UserAuthResult newArch(Long userId, String role, Long userApiKeyId) {
        return new UserAuthResult(userId, role, userApiKeyId);
    }
}
```

- [ ] **Step 2: AuthenticationDomainService 不再设置 teamId**

修改 `authenticateUser` 方法返回：
```java
return UserAuthResult.newArch(
        userApiKey.getUserId(),
        "user",
        userApiKey.getId()
);
```

- [ ] **Step 3: 提交**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/domain/security/service/UserAuthResult.java
git add gateway-boot/src/main/java/com/codingas/gateway/domain/security/service/AuthenticationDomainService.java
git commit -m "refactor(domain): UserAuthResult 移除 teamId，认证不再绑定团队"
```

---

### Task 9: 后端路由上下文 — RoutingContext 删除 teamId

**Files:**
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/domain/proxy/entity/RoutingContext.java`
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/application/proxy/ProductRoutingService.java`

- [ ] **Step 1: RoutingContext 删除 teamId 字段**

从 `RoutingContext` 中删除：
```java
private Long teamId;
```
```java
public Builder teamId(Long teamId) { context.teamId = teamId; return this; }
```
```java
public Long getTeamId() { return teamId; }
```

- [ ] **Step 2: ProductRoutingService 不再设置 teamId**

修改 `resolve` 方法构建 RoutingContext 的部分：
```java
return RoutingContext.builder()
        .providerId(product.getProviderId())
        .providerName(provider.getName())
        .productId(product.getId())
        .productType(product.getProductType())
        .userApiKeyId(userApiKey.getId())
        .model(model)
        .protocol(resolved.protocolName)
        .providerApiKey(plainApiKey)
        .providerApiKeyId(apiKey.getId())
        .endpoint(resolved.endpointUrl)
        .build();
```

- [ ] **Step 3: 检查 RoutingContext.getTeamId() 的其他引用并修复**

Run: `cd E:/workspace/llm-gateway && grep -rn "getTeamId\|\.teamId" gateway-boot/src/main/java/ | grep -v "UserApiKey" | grep -v "UserTeam" | grep -v "Team.java"`

逐一修复所有引用（主要是 UsageLogDo 中的 teamId 设置和 AuditEvent 中的 teamId 设置，这些保留但改为从 userId 推导）。

- [ ] **Step 4: 提交**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/domain/proxy/entity/RoutingContext.java
git add gateway-boot/src/main/java/com/codingas/gateway/application/proxy/ProductRoutingService.java
git commit -m "refactor(proxy): RoutingContext 移除 teamId，路由不再依赖团队"
```

---

### Task 10: 后端 DataInitializer — 删除 setTeamId

**Files:**
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/init/DataInitializer.java`

- [ ] **Step 1: 修改 initUserApiKey 方法**

重构前：
```java
private void initUserApiKey(User user, Team team, Product product, String keyPlain, String name) {
    if (user == null || team == null || product == null) {
        log.warn("Missing dependencies, skipping UserApiKey creation: {}", name);
        return;
    }

    if (userApiKeyGateway.countByTeamId(team.getId()) > 0) {
        List<UserApiKey> existing = userApiKeyGateway.findByTeamId(team.getId());
        boolean exists = existing.stream().anyMatch(k -> name.equals(k.getName()));
        if (exists) {
            log.info("UserApiKey '{}' already exists in team '{}', skipping", name, team.getName());
            return;
        }
    }

    UserApiKey apiKey = new UserApiKey();
    apiKey.setTeamId(team.getId());
    apiKey.setUserId(user.getId());
    apiKey.setProductIds(List.of(product.getId()));
    apiKey.setKeyPlain(keyPlain);
    apiKey.setName(name);
    apiKey.setState(UserApiKeyState.ACTIVE);

    UserApiKey saved = userApiKeyGateway.save(apiKey);
    log.info("Created UserApiKey: {} (id={}, prefix={}, team={}, user={})",
            name, saved.getId(), saved.getKeyPrefix(), team.getName(), user.getUsername());
}
```

重构后（去重改为按 userId + name 检查）：
```java
private void initUserApiKey(User user, Product product, String keyPlain, String name) {
    if (user == null || product == null) {
        log.warn("Missing dependencies, skipping UserApiKey creation: {}", name);
        return;
    }

    List<UserApiKey> existing = userApiKeyGateway.findByUserId(user.getId());
    boolean exists = existing.stream().anyMatch(k -> name.equals(k.getName()));
    if (exists) {
        log.info("UserApiKey '{}' already exists for user '{}', skipping", name, user.getUsername());
        return;
    }

    UserApiKey apiKey = new UserApiKey();
    apiKey.setUserId(user.getId());
    apiKey.setProductIds(List.of(product.getId()));
    apiKey.setKeyPlain(keyPlain);
    apiKey.setName(name);
    apiKey.setState(UserApiKeyState.ACTIVE);

    UserApiKey saved = userApiKeyGateway.save(apiKey);
    log.info("Created UserApiKey: {} (id={}, prefix={}, user={})",
            name, saved.getId(), saved.getKeyPrefix(), user.getUsername());
}
```

同时修改所有调用 `initUserApiKey` 的地方，去掉 `team` 参数。

- [ ] **Step 2: 提交**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/infrastructure/init/DataInitializer.java
git commit -m "refactor(data): DataInitializer 移除 UserApiKey.teamId"
```

---

### Task 11: 后端数据库迁移脚本

**Files:**
- Create: `gateway-boot/src/main/resources/db/migration/V32__drop_team_id_from_user_api_keys.sql`

- [ ] **Step 1: 创建迁移脚本**

```sql
-- 密钥归于用户，移除 team_id 列
ALTER TABLE user_api_keys DROP COLUMN team_id;
```

- [ ] **Step 2: 提交**

```bash
git add gateway-boot/src/main/resources/db/migration/V32__drop_team_id_from_user_api_keys.sql
git commit -m "refactor(db): 迁移脚本 — user_api_keys 移除 team_id 列"
```

---

### Task 12: 后端测试修复

**Files:**
- Modify: `gateway-boot/src/test/java/com/codingas/gateway/application/userapikey/UserApiKeyServiceImplTest.java`
- Modify: `gateway-boot/src/test/java/com/codingas/gateway/domain/security/service/AuthenticationDomainServiceTest.java`
- Modify: `gateway-boot/src/test/java/com/codingas/gateway/domain/security/service/UserAuthResultTest.java`
- Modify: `gateway-boot/src/test/java/com/codingas/gateway/adapter/api/TeamControllerUserApiKeyTest.java`

- [ ] **Step 1: 修复 UserApiKeyServiceImplTest**

修改 `create_success` 测试：
```java
@Test
@DisplayName("创建密钥成功")
void create_success() {
    UserApiKey saved = createSampleApiKey();
    when(userApiKeyGateway.save(any(UserApiKey.class))).thenReturn(saved);

    UserApiKeyCreateRequest request = new UserApiKeyCreateRequest(
            USER_ID, List.of(PRODUCT_ID), "test-key", List.of("gpt-4o"), 100000L
    );
    UserApiKeyCreateResponse response = service.create(request);

    assertThat(response).isNotNull();
    assertThat(response.apiKeyPlain()).startsWith("sk-");
    assertThat(response.id()).isEqualTo(API_KEY_ID);
    verify(userApiKeyGateway).save(argThat(key ->
            key.getUserId().equals(USER_ID)
    ));
}
```

删除 `ListByTeamIdTests` 整个嵌套类。

修改 `createSampleApiKey` 辅助方法 — 删除 `setTeamId`：
```java
private UserApiKey createSampleApiKey() {
    UserApiKey apiKey = new UserApiKey();
    apiKey.setId(API_KEY_ID);
    apiKey.setUserId(USER_ID);
    apiKey.setProductIds(List.of(PRODUCT_ID));
    apiKey.setKeyPlain("sk-abc1xxxxx");
    apiKey.setKeyPrefix("sk-abc1");
    apiKey.setName("test-key");
    apiKey.setModels(List.of("gpt-4o"));
    apiKey.setQuotaLimit(100000L);
    apiKey.setState(UserApiKeyState.ACTIVE);
    return apiKey;
}
```

修改 `findByUserId_success` 断言 — 删除 `teamId()` 断言：
```java
assertThat(responses.get(0).userId()).isEqualTo(USER_ID);
```

- [ ] **Step 2: 修复 AuthenticationDomainServiceTest**

修改 `authenticateUser_success` 测试 — 删除 `setTeamId`：
```java
UserApiKey userApiKey = new UserApiKey();
userApiKey.setId(101L);
userApiKey.setUserId(1L);
userApiKey.setKeyPlain(apiKey);
userApiKey.setKeyPrefix("sk-test1");
userApiKey.setKeyHash("hashed-test-key");
userApiKey.setState(UserApiKeyState.ACTIVE);
```

修改断言 — 删除 `teamId()`：
```java
assertThat(result.userId()).isEqualTo(1L);
assertThat(result.role()).isEqualTo("user");
assertThat(result.userApiKeyId()).isEqualTo(101L);
```

- [ ] **Step 3: 修复 UserAuthResultTest**

修改 `create_newArch_success` 测试：
```java
@Test
@DisplayName("创建新架构认证结果成功")
void create_newArch_success() {
    UserAuthResult result = UserAuthResult.newArch(1L, "USER", 101L);

    assertThat(result.userId()).isEqualTo(1L);
    assertThat(result.role()).isEqualTo("USER");
    assertThat(result.userApiKeyId()).isEqualTo(101L);
}
```

修改 `equals_sameValues_equal` 测试：
```java
UserAuthResult result1 = UserAuthResult.newArch(1L, "ADMIN", 100L);
UserAuthResult result2 = UserAuthResult.newArch(1L, "ADMIN", 100L);
```

修改 `equals_differentValues_notEqual` 测试：
```java
UserAuthResult result1 = UserAuthResult.newArch(1L, "ADMIN", 100L);
UserAuthResult result2 = UserAuthResult.newArch(2L, "USER", 101L);
```

修改 `toString_containsAllFields` 测试：
```java
UserAuthResult result = UserAuthResult.newArch(1L, "ADMIN", 100L);

String str = result.toString();

assertThat(str).contains("userId=1");
assertThat(str).contains("role=ADMIN");
assertThat(str).contains("userApiKeyId=100");
```

- [ ] **Step 4: 修复 TeamControllerUserApiKeyTest**

修改 `listApiKeys_success` — `listByTeamId` 已删除，改为测试新的列表逻辑：
```java
@Test
@DisplayName("查询团队密钥列表")
void listApiKeys_success() {
    when(teamService.getMemberUserIds(TEAM_ID)).thenReturn(List.of(USER_ID));
    when(userApiKeyService.findByUserId(USER_ID)).thenReturn(List.of());

    List<UserApiKeyResponse> result = controller.listApiKeys(TEAM_ID);

    assertThat(result).isEmpty();
}
```

修改 `getApiKey_success` — `getDetailByIdAndTeamId` 已删除，改为 `getDetailById`：
```java
@Test
@DisplayName("查询密钥详情")
void getApiKey_success() {
    UserApiKeyDetailResponse detailResponse = new UserApiKeyDetailResponse(
            API_KEY_ID, USER_ID, List.of(PRODUCT_ID), List.of(),
            "sk-abc1", "sk-abc1xxxxx", "test-key",
            List.of("gpt-4o"), 100000L, UserApiKeyState.ACTIVE,
            Instant.now(), Instant.now()
    );
    when(userApiKeyService.getDetailById(API_KEY_ID)).thenReturn(detailResponse);

    UserApiKeyDetailResponse result = controller.getApiKey(TEAM_ID, API_KEY_ID);

    assertThat(result.id()).isEqualTo(API_KEY_ID);
    assertThat(result.keyPlain()).isEqualTo("sk-abc1xxxxx");
}
```

修改 `createApiKey_success` — `UserApiKeyCreateRequest` 不再含 teamId：
```java
@Test
@DisplayName("创建密钥")
void createApiKey_success() {
    try (MockedStatic<StpUtil> stpUtilMock = org.mockito.Mockito.mockStatic(StpUtil.class)) {
        stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(USER_ID);
        stpUtilMock.when(() -> StpUtil.hasRole("ADMIN")).thenReturn(false);
        UserApiKeyCreateRequest request = new UserApiKeyCreateRequest(
                USER_ID, List.of(PRODUCT_ID), "test-key", List.of("gpt-4o"), 100000L
        );
        UserApiKeyCreateResponse createResponse = new UserApiKeyCreateResponse(
                API_KEY_ID, "sk-abc1", "sk-abc1xxxxx"
        );
        when(userApiKeyService.create(any(UserApiKeyCreateRequest.class)))
                .thenReturn(createResponse);

        var result = controller.createApiKey(TEAM_ID, request);

        assertThat(result.getStatusCode().value()).isEqualTo(201);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().id()).isEqualTo(API_KEY_ID);
        verify(userApiKeyService).create(any(UserApiKeyCreateRequest.class));
    }
}
```

修改 `updateApiKey_success` — `UserApiKeyResponse` 不再含 teamId：
```java
@Test
@DisplayName("更新密钥")
void updateApiKey_success() {
    UserApiKeyUpdateRequest request = new UserApiKeyUpdateRequest(
            "updated-name", List.of(PRODUCT_ID), List.of("claude-3-5-sonnet"), null, null
    );
    UserApiKeyResponse updateResponse = new UserApiKeyResponse(
            API_KEY_ID, USER_ID, List.of(PRODUCT_ID), List.of(),
            "sk-abc1", "updated-name",
            List.of("claude-3-5-sonnet"), 100000L, UserApiKeyState.ACTIVE,
            Instant.now(), Instant.now()
    );
    when(userApiKeyService.update(any(), any(UserApiKeyUpdateRequest.class)))
            .thenReturn(updateResponse);

    UserApiKeyResponse result = controller.updateApiKey(TEAM_ID, API_KEY_ID, request);

    assertThat(result).isNotNull();
}
```

- [ ] **Step 5: 运行测试验证**

Run: `cd E:/workspace/llm-gateway && ./mvnw test -pl gateway-boot -q 2>&1 | tail -20`
Expected: 所有测试通过

- [ ] **Step 6: 提交**

```bash
git add gateway-boot/src/test/
git commit -m "fix(test): 修复因 teamId 移除导致的测试用例"
```

---

### Task 13: 后端编译与测试全量验证

**Files:** 无新增

- [ ] **Step 1: 全量编译**

Run: `cd E:/workspace/llm-gateway && ./mvnw clean compile -pl gateway-boot -q 2>&1 | tail -10`
Expected: BUILD SUCCESS

- [ ] **Step 2: 全量测试**

Run: `cd E:/workspace/llm-gateway && ./mvnw test -pl gateway-boot -q 2>&1 | tail -20`
Expected: BUILD SUCCESS，所有测试通过

- [ ] **Step 3: 提交（如有遗漏修复）**

如有编译或测试错误，修复后提交。

---

### Task 14: 前端类型定义 — 删除 teamId

**Files:**
- Modify: `gateway-console/src/types/team.ts`

- [ ] **Step 1: UserApiKey 删除 teamId**

从 `UserApiKey` 接口中删除：
```typescript
teamId: number;
```

- [ ] **Step 2: CreateUserApiKeyRequest 删除 teamId**

从 `CreateUserApiKeyRequest` 接口中删除：
```typescript
teamId: number;
```

- [ ] **Step 3: 提交**

```bash
git add gateway-console/src/types/team.ts
git commit -m "refactor(types): 前端 UserApiKey 移除 teamId"
```

---

### Task 15: 前端 API 服务 — 删除 teamId 参数

**Files:**
- Modify: `gateway-console/src/services/api/team.ts`
- Modify: `gateway-console/src/services/query/useTeams.ts`

- [ ] **Step 1: team.ts API 删除 createApiKey 中的 teamId**

`createApiKey` 的请求体不再需要 `teamId`：
```typescript
/** 创建用户 API Key */
createApiKey: (teamId: number, data: Omit<CreateUserApiKeyRequest, 'teamId'>) =>
    api.post<CreateUserApiKeyResponse>(`/teams/${teamId}/api-keys`, data),
```

- [ ] **Step 2: useTeams.ts mutation 删除 teamId 传递**

修改 `useCreateUserApiKey`：
```typescript
export function useCreateUserApiKey() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ teamId, data }: { teamId: number; data: Omit<CreateUserApiKeyRequest, 'teamId'> }) =>
      teamApi.createApiKey(teamId, data),
    onSuccess: (_data, { teamId }) => {
      qc.invalidateQueries({ queryKey: teamKeys.apiKeys(teamId) });
    },
  });
}
```

- [ ] **Step 3: 提交**

```bash
git add gateway-console/src/services/api/team.ts gateway-console/src/services/query/useTeams.ts
git commit -m "refactor(api): 前端 API 调用移除 teamId"
```

---

### Task 16: 前端 Teams 页面密钥管理调整

**Files:**
- Modify: `gateway-console/src/pages/Teams/UserApiKeyManageModal.tsx`

- [ ] **Step 1: 删除创建表单中的 teamId 传递**

修改 `handleSubmit` 中创建请求的构造：
```typescript
const request: Omit<CreateUserApiKeyRequest, 'teamId'> = {
    userId: values.userId,
    productIds: values.productIds,
    name: values.name,
    models: values.models,
    quotaLimit: values.quotaLimit,
};
const result = await teamApi.createApiKey(team.id, request);
```

- [ ] **Step 2: 提交**

```bash
git add gateway-console/src/pages/Teams/UserApiKeyManageModal.tsx
git commit -m "fix(ui): Teams 密钥管理弹窗移除 teamId"
```

---

### Task 17: 前端新增用户维度 API Key API 服务

**Files:**
- Create: `gateway-console/src/services/api/userApiKey.ts`
- Create: `gateway-console/src/services/query/useUserApiKeys.ts`

- [ ] **Step 1: 创建 userApiKey.ts API 服务**

```typescript
import { api } from './client';
import type { UserApiKey, CreateUserApiKeyRequest, UpdateUserApiKeyRequest } from '@/types/team';

/** 用户维度 API Key 接口（不含团队上下文） */
export const userApiKeyApi = {
  /** 查询指定用户的所有 API Key */
  listByUserId: (userId: number) =>
    api.get<UserApiKey[]>(`/users/${userId}/api-keys`),

  /** 查询当前用户的 API Key */
  listMyApiKeys: () =>
    api.get<UserApiKey[]>('/me/api-keys'),

  /** 创建用户 API Key */
  create: (data: Omit<CreateUserApiKeyRequest, 'teamId'>) =>
    api.post<{ id: number; keyPrefix: string; apiKeyPlain: string }>('/user-api-keys', data),

  /** 更新用户 API Key */
  update: (id: number, data: UpdateUserApiKeyRequest) =>
    api.put<UserApiKey>(`/user-api-keys/${id}`, data),

  /** 删除用户 API Key */
  delete: (id: number) =>
    api.delete<void>(`/user-api-keys/${id}`),
};
```

- [ ] **Step 2: 创建 useUserApiKeys.ts hooks**

```typescript
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { userApiKeyApi } from '@/services/api/userApiKey';
import type { UpdateUserApiKeyRequest } from '@/types/team';

/** 用户 API Key Query Keys */
export const userApiKeyKeys = {
  all: ['userApiKeys'] as const,
  byUser: (userId: number) => [...userApiKeyKeys.all, userId] as const,
  my: ['me', 'api-keys'] as const,
};

/** 查询指定用户的 API Key 列表 */
export function useUserApiKeys(userId: number) {
  return useQuery({
    queryKey: userApiKeyKeys.byUser(userId),
    queryFn: () => userApiKeyApi.listByUserId(userId),
    enabled: userId > 0,
  });
}

/** 创建用户 API Key */
export function useCreateUserApiKey() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data: Parameters<typeof userApiKeyApi.create>[0]) =>
      userApiKeyApi.create(data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: userApiKeyKeys.all });
    },
  });
}

/** 更新用户 API Key */
export function useUpdateUserApiKey() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: UpdateUserApiKeyRequest }) =>
      userApiKeyApi.update(id, data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: userApiKeyKeys.all });
    },
  });
}

/** 删除用户 API Key */
export function useDeleteUserApiKey() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => userApiKeyApi.delete(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: userApiKeyKeys.all });
    },
  });
}
```

- [ ] **Step 3: 提交**

```bash
git add gateway-console/src/services/api/userApiKey.ts gateway-console/src/services/query/useUserApiKeys.ts
git commit -m "feat(frontend): 新增用户维度 API Key API 服务和 hooks"
```

---

### Task 18: 前端新增 UserApiKeyModal 组件

**Files:**
- Create: `gateway-console/src/pages/Users/UserApiKeyModal.tsx`

- [ ] **Step 1: 创建 UserApiKeyModal 组件**

```tsx
import { useState } from 'react';
import { Modal, Table, Button, Space, Tag, Form, Input, Select, InputNumber, App } from 'antd';
import { PlusOutlined, CopyOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useUserApiKeys, useCreateUserApiKey, useUpdateUserApiKey, useDeleteUserApiKey } from '@/services/query/useUserApiKeys';
import { productApi } from '@/services/api/product';
import type { UserApiKey } from '@/types/team';
import type { Product } from '@/types/product';

interface UserApiKeyModalProps {
  userId: number;
  username: string;
  open: boolean;
  onClose: () => void;
}

export default function UserApiKeyModal({ userId, username, open, onClose }: UserApiKeyModalProps) {
  const { t } = useTranslation('users');
  const { modal, message } = App.useApp();
  const { data: apiKeys, isLoading } = useUserApiKeys(userId);
  const createMutation = useCreateUserApiKey();
  const updateMutation = useUpdateUserApiKey();
  const deleteMutation = useDeleteUserApiKey();

  const [formVisible, setFormVisible] = useState(false);
  const [editingKey, setEditingKey] = useState<UserApiKey | null>(null);
  const [products, setProducts] = useState<Product[]>([]);
  const [createdKey, setCreatedKey] = useState<string | null>(null);
  const [form] = Form.useForm();

  const loadProducts = async () => {
    try {
      const res = await productApi.listAll();
      setProducts(res);
    } catch {
      // 静默处理
    }
  };

  const handleAdd = async () => {
    setEditingKey(null);
    form.resetFields();
    setCreatedKey(null);
    await loadProducts();
    setFormVisible(true);
  };

  const handleEdit = async (record: UserApiKey) => {
    setEditingKey(record);
    setCreatedKey(null);
    await loadProducts();
    form.setFieldsValue({
      name: record.name,
      productIds: record.productIds,
      models: record.models,
      quotaLimit: record.quotaLimit,
      state: record.state,
    });
    setFormVisible(true);
  };

  const handleDelete = (id: number) => {
    modal.confirm({
      title: t('apiKey.deleteConfirm'),
      okType: 'danger',
      onOk: () => deleteMutation.mutateAsync(id),
    });
  };

  const handleCopy = (text: string) => {
    navigator.clipboard.writeText(text);
    message.success(t('apiKey.copySuccess'));
  };

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      if (editingKey) {
        await updateMutation.mutateAsync({
          id: editingKey.id,
          data: {
            name: values.name,
            productIds: values.productIds,
            models: values.models,
            quotaLimit: values.quotaLimit,
            state: values.state,
          },
        });
        message.success(t('apiKey.updateSuccess', { ns: 'common' }));
      } else {
        const result = await createMutation.mutateAsync({
          userId,
          productIds: values.productIds,
          name: values.name,
          models: values.models,
          quotaLimit: values.quotaLimit,
        });
        setCreatedKey(result.apiKeyPlain);
        message.success(t('apiKey.createSuccess'));
      }
      setFormVisible(false);
    } catch (error: unknown) {
      if (error && typeof error === 'object' && 'errorFields' in error) return;
      message.error(editingKey ? t('apiKey.updateFailed') : t('apiKey.createFailed'));
    }
  };

  const productOptions = products.map((p) => ({
    label: p.productName,
    value: p.id,
  }));

  const columns = [
    {
      title: t('apiKey.name'),
      dataIndex: 'name',
      key: 'name',
    },
    {
      title: t('apiKey.key'),
      dataIndex: 'keyPrefix',
      key: 'keyPrefix',
      render: (prefix: string) => <code>{prefix}...</code>,
    },
    {
      title: t('apiKey.state'),
      dataIndex: 'state',
      key: 'state',
      render: (state: string) => (
        <Tag color={state === 'ACTIVE' ? 'green' : 'red'}>
          {state === 'ACTIVE' ? t('state.active') : t('state.disabled')}
        </Tag>
      ),
    },
    {
      title: t('actions.label', { ns: 'common' }),
      key: 'actions',
      width: 120,
      render: (_: unknown, record: UserApiKey) => (
        <Space>
          {createdKey && (
            <Button type="text" size="small" icon={<CopyOutlined />} onClick={() => handleCopy(createdKey)} />
          )}
          <Button type="text" size="small" onClick={() => handleEdit(record)}>
            {t('actions.edit', { ns: 'common' })}
          </Button>
          <Button type="text" size="small" danger onClick={() => handleDelete(record.id)}>
            {t('actions.delete', { ns: 'common' })}
          </Button>
        </Space>
      ),
    },
  ];

  return (
    <>
      <Modal
        title={t('apiKey.manageTitle', { username })}
        open={open}
        onCancel={onClose}
        width={720}
        footer={null}
      >
        <div style={{ marginBottom: 12 }}>
          <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
            {t('addApiKey')}
          </Button>
        </div>

        <Table
          columns={columns}
          dataSource={apiKeys || []}
          rowKey="id"
          loading={isLoading}
          pagination={false}
          size="small"
        />
      </Modal>

      <Modal
        title={editingKey ? t('apiKey.editTitle') : t('addApiKey')}
        open={formVisible}
        onOk={handleSubmit}
        onCancel={() => setFormVisible(false)}
        confirmLoading={createMutation.isPending || updateMutation.isPending}
        width={480}
      >
        {createdKey && (
          <div style={{
            marginBottom: 16, padding: 12,
            background: '#f6ffed', border: '1px solid #b7eb8f', borderRadius: 6,
          }}>
            <div style={{ marginBottom: 4, fontWeight: 500 }}>{t('apiKey.createSuccess')}</div>
            <code style={{ wordBreak: 'break-all', fontSize: 13 }}>{createdKey}</code>
            <div style={{ marginTop: 4, color: '#999', fontSize: 12 }}>{t('apiKey.onlyOnceWarning')}</div>
          </div>
        )}

        <Form form={form} layout="vertical">
          <Form.Item name="name" label={t('apiKey.name')} rules={[{ required: true }]}>
            <Input placeholder={t('apiKey.namePlaceholder')} />
          </Form.Item>
          <Form.Item name="productIds" label={t('apiKey.products')} rules={[{ required: true }]}>
            <Select
              mode="multiple"
              placeholder={t('apiKey.productsPlaceholder')}
              options={productOptions}
              optionFilterProp="label"
              maxTagCount="responsive"
            />
          </Form.Item>
          <Form.Item name="models" label={t('apiKey.models')}>
            <Select mode="tags" placeholder={t('apiKey.modelsPlaceholder')} tokenSeparators={[',']} />
          </Form.Item>
          <Form.Item name="quotaLimit" label={t('apiKey.quotaLimit')}>
            <InputNumber style={{ width: '100%' }} placeholder={t('apiKey.quotaPlaceholder')} min={0} />
          </Form.Item>
          {editingKey && (
            <Form.Item name="state" label={t('apiKey.state')}>
              <Select options={[
                { label: t('state.active'), value: 'ACTIVE' },
                { label: t('state.disabled'), value: 'DISABLED' },
              ]} />
            </Form.Item>
          )}
        </Form>
      </Modal>
    </>
  );
}
```

- [ ] **Step 2: 提交**

```bash
git add gateway-console/src/pages/Users/UserApiKeyModal.tsx
git commit -m "feat(frontend): 新增用户密钥管理弹窗组件"
```

---

### Task 19: 前端用户列表页 — 增加密钥管理入口

**Files:**
- Modify: `gateway-console/src/pages/Users/index.tsx`

- [ ] **Step 1: 导入 UserApiKeyModal 并添加状态**

在文件顶部导入：
```typescript
import UserApiKeyModal from './UserApiKeyModal';
```

在组件内部添加状态（替换现有的 `KeyOutlined` 点击处理）：
```typescript
const [apiKeyUser, setApiKeyUser] = useState<{ id: number; username: string } | null>(null);
```

- [ ] **Step 2: 修改操作列的密钥按钮**

替换当前 `KeyOutlined` 按钮的 `onClick`：
```tsx
<Button type="text" icon={<KeyOutlined />} onClick={() => setApiKeyUser({ id: record.id, username: record.username })} />
```

- [ ] **Step 3: 在 return 中添加 UserApiKeyModal**

在 `</Card>` 之后添加：
```tsx
{apiKeyUser && (
  <UserApiKeyModal
    userId={apiKeyUser.id}
    username={apiKeyUser.username}
    open={true}
    onClose={() => setApiKeyUser(null)}
  />
)}
```

- [ ] **Step 4: 移除 handleResetPassword 相关代码**

由于 `KeyOutlined` 原本绑定的是重置密码操作，现在改为密钥管理。重置密码功能如果仍需要，可以另外添加按钮。当前将 `KeyOutlined` 改为密钥管理入口，移除 `handleResetPassword` 和 `resetPasswordMutation`（如果不再需要）。

如果 `resetPasswordMutation` 和 `handleResetPassword` 没有其他地方使用，删除它们。保留 `useResetPassword` 导入以防其他地方需要。

- [ ] **Step 5: 提交**

```bash
git add gateway-console/src/pages/Users/index.tsx
git commit -m "feat(frontend): 用户列表操作列增加密钥管理入口"
```

---

### Task 20: 前端 i18n 补充密钥管理文案

**Files:**
- Modify: `gateway-console/src/locales/zh-CN/users.json`
- Modify: `gateway-console/src/locales/en-US/users.json`

- [ ] **Step 1: 补充中文文案**

在 `users.json` 的 `apiKey` 部分补充：
```json
{
  "title": "用户管理",
  "userList": "用户列表",
  "apiKeyList": "API Key 列表",
  "addUser": "新增用户",
  "addApiKey": "新增 API Key",
  "searchPlaceholder": "搜索用户名或邮箱",
  "user": {
    "username": "用户名",
    "email": "邮箱",
    "role": "角色",
    "state": "状态",
    "createdAt": "创建时间",
    "password": "密码"
  },
  "apiKey": {
    "name": "名称",
    "key": "Key 值",
    "state": "状态",
    "createdAt": "创建时间",
    "copySuccess": "API Key 已复制",
    "createSuccess": "创建成功，请立即复制保存 Key 值",
    "updateSuccess": "更新成功",
    "createFailed": "创建失败",
    "updateFailed": "更新失败",
    "deleteConfirm": "确定要删除该 API Key 吗？此操作不可恢复。",
    "manageTitle": "{{username}} 的 API Key 管理",
    "editTitle": "编辑 API Key",
    "namePlaceholder": "例如：生产环境 Key",
    "products": "关联产品",
    "productsPlaceholder": "选择关联的产品",
    "models": "可用模型",
    "modelsPlaceholder": "留空表示允许所有模型，或输入模型名称",
    "quotaLimit": "额度限制",
    "quotaPlaceholder": "留空表示不限制",
    "onlyOnceWarning": "此密钥仅显示一次，关闭后无法再次查看"
  },
  "role": {
    "ADMIN": "管理员",
    "USER": "普通用户"
  },
  "state": {
    "active": "正常",
    "disabled": "停用",
    "locked": "锁定"
  }
}
```

- [ ] **Step 2: 补充英文文案**

在 `users.json` 的 `apiKey` 部分补充：
```json
{
  "title": "User Management",
  "userList": "User List",
  "apiKeyList": "API Key List",
  "addUser": "Add User",
  "addApiKey": "Add API Key",
  "searchPlaceholder": "Search username or email",
  "user": {
    "username": "Username",
    "email": "Email",
    "role": "Role",
    "state": "Status",
    "createdAt": "Created At",
    "password": "Password"
  },
  "apiKey": {
    "name": "Name",
    "key": "Key Value",
    "state": "Status",
    "createdAt": "Created At",
    "copySuccess": "API Key copied",
    "createSuccess": "Created successfully, please copy and save the key immediately",
    "updateSuccess": "Updated successfully",
    "createFailed": "Creation failed",
    "updateFailed": "Update failed",
    "deleteConfirm": "Are you sure you want to delete this API Key? This action cannot be undone.",
    "manageTitle": "API Keys for {{username}}",
    "editTitle": "Edit API Key",
    "namePlaceholder": "e.g. Production Key",
    "products": "Associated Products",
    "productsPlaceholder": "Select associated products",
    "models": "Available Models",
    "modelsPlaceholder": "Leave empty for all models, or enter model names",
    "quotaLimit": "Quota Limit",
    "quotaPlaceholder": "Leave empty for unlimited",
    "onlyOnceWarning": "This key will only be shown once and cannot be retrieved after closing"
  },
  "role": {
    "ADMIN": "Admin",
    "USER": "User"
  },
  "state": {
    "active": "Active",
    "disabled": "Disabled",
    "locked": "Locked"
  }
}
```

- [ ] **Step 3: 提交**

```bash
git add gateway-console/src/locales/
git commit -m "feat(i18n): 补充用户密钥管理相关文案"
```

---

### Task 21: 前端全量验证

**Files:** 无新增

- [ ] **Step 1: 前端编译验证**

Run: `cd E:/workspace/llm-gateway/gateway-console && npx tsc --noEmit 2>&1 | head -30`
Expected: 无类型错误

- [ ] **Step 2: 修复可能遗漏的类型错误**

如果发现 `teamId` 引用遗漏，逐一修复。

- [ ] **Step 3: 提交（如有修复）**

```bash
git add -A gateway-console/src/
git commit -m "fix(frontend): 修复 teamId 移除后的类型错误"
```

---

### Task 22: 全端端到端验证

**Files:** 无新增

- [ ] **Step 1: 后端全量编译+测试**

Run: `cd E:/workspace/llm-gateway && ./mvnw clean verify -pl gateway-boot -q 2>&1 | tail -20`
Expected: BUILD SUCCESS

- [ ] **Step 2: 前端编译**

Run: `cd E:/workspace/llm-gateway/gateway-console && npx tsc --noEmit`
Expected: 无错误

- [ ] **Step 3: 最终提交（如有遗漏修复）**

修复任何遗漏问题后提交。

---

## 自检

**1. Spec 覆盖检查：**
- UserApiKey 删除 teamId → Task 1-5, 8-9
- Gateway 删除 findByTeamId → Task 2-3
- DTO 删除 teamId → Task 4
- Service 删除 teamId 逻辑 → Task 5
- Controller 调整 → Task 6
- 认证链路调整 → Task 8
- 路由上下文调整 → Task 9
- 数据库迁移 → Task 11
- DataInitializer → Task 10
- 测试修复 → Task 12
- 前端类型/API → Task 14-15
- 前端 Teams 调整 → Task 16
- 前端 UserApiKeyModal → Task 17-18
- 用户列表入口 → Task 19
- i18n → Task 20
- 全量验证 → Task 21-22

**2. 占位符扫描：** 无 TBD/TODO/待实现

**3. 类型一致性：** UserApiKey 无 teamId → DTO 无 teamId → 前端类型无 teamId → API 调用无 teamId，全链路一致