---
change: inherited-permission-model
design-doc: docs/superpowers/specs/2026-05-30-inherited-permission-model-design.md
base-ref: 9b3cb87c6dd5c1ba55c9f41d054fac4bda303a10
---

# 纯继承式权限模型实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 移除 API Key 级别的渠道权限，改为完全继承团队渠道权限

**Architecture:** 权限链路变为 `UserApiKey → User → Team → Channels`。ChannelSelector 注入团队渠道过滤，无权限自然无可用通道。删除 user_api_key_channels 表及全部关联代码。

**Tech Stack:** Java 21, Spring Boot 3.5.x, JPA, React/Ant Design (前端)

---

### Task 1: 数据库迁移 — 删除 user_api_key_channels 表

**Files:**
- Create: `gateway-boot/src/main/resources/db/migration/V43__drop_user_api_key_channels.sql`

- [ ] **Step 1: 创建迁移脚本**

```sql
-- V43: 删除 API Key 渠道关联表（权限改为团队继承）
DROP TABLE IF EXISTS user_api_key_channels;
```

- [ ] **Step 2: 提交**

```bash
git add gateway-boot/src/main/resources/db/migration/V43__drop_user_api_key_channels.sql
git commit -m "feat: 迁移脚本 V43 删除 user_api_key_channels 表"
```

---

### Task 2: 删除渠道关联 DO 和 Repository

**Files:**
- Delete: `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/iam/gateway/database/dataobject/UserApiKeyChannelDo.java`
- Delete: `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/iam/gateway/database/repository/UserApiKeyChannelRepository.java`

- [ ] **Step 1: 删除 UserApiKeyChannelDo.java**

整个文件删除（28 行）。

- [ ] **Step 2: 删除 UserApiKeyChannelRepository.java**

整个文件删除（27 行）。

- [ ] **Step 3: 提交**

```bash
git add -u
git commit -m "refactor: 删除 UserApiKeyChannelDo 和 UserApiKeyChannelRepository"
```

---

### Task 3: 清理 UserApiKeyGateway 接口和实现

**Files:**
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/domain/iam/gateway/UserApiKeyGateway.java:28-29`
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/iam/gateway/UserApiKeyGatewayImpl.java:8,31,78-87,93-95,99-102,117-119`

- [ ] **Step 1: UserApiKeyGateway 接口 — 删除 findIdsByChannelId**

删除第 28-29 行：
```java
/** 查询关联某渠道的 Key ID 列表 */
List<Long> findIdsByChannelId(Long channelId);
```

- [ ] **Step 2: UserApiKeyGatewayImpl — 删除 channelRepository 依赖**

删除第 8 行 import：
```java
import com.codingas.gateway.infrastructure.iam.gateway.database.dataobject.UserApiKeyChannelDo;
```

删除第 31 行字段：
```java
private final UserApiKeyChannelRepository channelRepository;
```

- [ ] **Step 3: UserApiKeyGatewayImpl — 删除 save 中渠道关联逻辑**

删除第 78-87 行（`if (userApiKey.getChannelIds() != null) { ... }` 块）。

- [ ] **Step 4: UserApiKeyGatewayImpl — 删除 delete 中渠道关联清理**

删除第 93-95 行：
```java
channelRepository.deleteByUserApiKeyId(userApiKey.getId());
```

- [ ] **Step 5: UserApiKeyGatewayImpl — 删除 findIdsByChannelId 方法**

删除第 99-102 行：
```java
@Override
public List<Long> findIdsByChannelId(Long channelId) {
    return channelRepository.findUserApiKeyIdByChannelId(channelId);
}
```

- [ ] **Step 6: UserApiKeyGatewayImpl — 删除 toEntity 中渠道加载**

删除第 117-119 行：
```java
List<Long> channelIds = channelRepository.findChannelIdByUserApiKeyId(dataObject.getId());
entity.setChannelIds(channelIds);
```

- [ ] **Step 7: 提交**

```bash
git add -u
git commit -m "refactor: UserApiKeyGateway 清理渠道关联逻辑"
```

---

### Task 4: 清理 DTO — 移除 channelIds/channels/ChannelBrief

**Files:**
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/application/userapikey/dto/UserApiKeyCreateRequest.java`
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/application/userapikey/dto/UserApiKeyUpdateRequest.java`
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/application/userapikey/dto/UserApiKeyResponse.java`
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/application/userapikey/dto/UserApiKeyDetailResponse.java`
- Delete: `gateway-boot/src/main/java/com/codingas/gateway/application/userapikey/dto/ChannelBrief.java`

- [ ] **Step 1: UserApiKeyCreateRequest — 移除 channelIds**

将 record 改为：
```java
public record UserApiKeyCreateRequest(
        @NotNull(message = "用户 ID 不能为空")
        Long userId,
        @NotBlank(message = "密钥名称不能为空")
        String name,
        List<String> models,
        Long quotaLimit
) {}
```
删除 `@NotEmpty` import（如不再需要）。

- [ ] **Step 2: UserApiKeyUpdateRequest — 移除 channelIds**

将 record 改为：
```java
public record UserApiKeyUpdateRequest(
        String name,
        List<String> models,
        Long quotaLimit,
        UserApiKeyState state
) {}
```

- [ ] **Step 3: UserApiKeyResponse — 移除 channels 和 ChannelBrief**

将 record 改为：
```java
public record UserApiKeyResponse(
        Long id,
        Long userId,
        String keyPrefix,
        String name,
        List<String> models,
        Long quotaLimit,
        UserApiKeyState state,
        Instant createdAt,
        Instant updatedAt
) {}
```
删除 `ChannelBrief` import。

- [ ] **Step 4: UserApiKeyDetailResponse — 移除 channels 和 ChannelBrief**

将 record 改为：
```java
public record UserApiKeyDetailResponse(
        Long id,
        Long userId,
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
删除 `ChannelBrief` import。

- [ ] **Step 5: 删除 ChannelBrief.java**

整个文件删除。

- [ ] **Step 6: 提交**

```bash
git add -u
git commit -m "refactor: DTO 移除 channelIds/channels/ChannelBrief"
```

---

### Task 5: 清理 UserApiKeyServiceImpl

**Files:**
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/application/userapikey/UserApiKeyServiceImpl.java`

- [ ] **Step 1: 移除 ChannelGateway 依赖和 ChannelBrief import**

删除 `ChannelGateway` 字段和构造函数参数，删除 `ChannelBrief` import。

- [ ] **Step 2: create() — 移除 setChannelIds**

删除第 49 行 `apiKey.setChannelIds(request.channelIds());`。
修改第 58-59 行日志，移除 channelIds。

- [ ] **Step 3: update() — 移除 channelIds 更新**

删除第 94-96 行：
```java
if (request.channelIds() != null) {
    apiKey.setChannelIds(request.channelIds());
}
```

- [ ] **Step 4: toResponse() — 移除 channelIds/channels**

修改为：
```java
private UserApiKeyResponse toResponse(UserApiKey apiKey) {
    return new UserApiKeyResponse(
            apiKey.getId(),
            apiKey.getUserId(),
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

- [ ] **Step 5: toDetailResponse() — 移除 channelIds/channels**

修改为：
```java
private UserApiKeyDetailResponse toDetailResponse(UserApiKey apiKey) {
    return new UserApiKeyDetailResponse(
            apiKey.getId(),
            apiKey.getUserId(),
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

- [ ] **Step 6: 删除 toChannelBriefs() 方法**

删除第 154-162 行整个方法。

- [ ] **Step 7: 提交**

```bash
git add -u
git commit -m "refactor: UserApiKeyServiceImpl 移除渠道关联逻辑"
```

---

### Task 6: Gateway 便捷方法 — findTeamIdByUserId / findChannelIdsByTeamId

**Files:**
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/domain/team/gateway/UserTeamGateway.java`
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/team/gateway/UserTeamGatewayImpl.java`
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/domain/team/gateway/TeamChannelGateway.java`
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/team/gateway/TeamChannelGatewayImpl.java`

- [ ] **Step 1: UserTeamGateway 接口新增 findTeamIdByUserId**

```java
/**
 * 查找用户所属的团队 ID（业务层限制单团队，返回第一个）
 */
Long findTeamIdByUserId(Long userId);
```

- [ ] **Step 2: UserTeamGatewayImpl 实现 findTeamIdByUserId**

```java
@Override
public Long findTeamIdByUserId(Long userId) {
    return userTeamRepository.findByUserId(userId).stream()
            .findFirst()
            .map(UserTeamDo::getTeamId)
            .orElse(null);
}
```

- [ ] **Step 3: TeamChannelGateway 接口新增 findChannelIdsByTeamId**

```java
/**
 * 查找团队关联的渠道 ID 列表
 */
List<Long> findChannelIdsByTeamId(Long teamId);
```

- [ ] **Step 4: TeamChannelGatewayImpl 实现 findChannelIdsByTeamId**

在实现中通过 `teamChannelRepository.findByTeamId(teamId)` 获取 TeamChannelDo 列表，提取 channelId。

- [ ] **Step 5: 提交**

```bash
git add -u
git commit -m "feat: Gateway 新增 findTeamIdByUserId 和 findChannelIdsByTeamId 便捷方法"
```

---

### Task 7: ChannelSelector 注入团队渠道过滤

**Files:**
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/application/proxy/routing/ChannelSelector.java`

- [ ] **Step 1: 修改 ChannelSelector.select 签名和逻辑**

新增 `UserTeamGateway` 和 `TeamChannelGateway` 依赖。修改方法签名：

```java
public ChannelModel select(Long modelId, Long userId) {
    // 获取用户团队渠道集合
    Long teamId = userTeamGateway.findTeamIdByUserId(userId);
    List<Long> teamChannelIds = teamId != null
            ? teamChannelGateway.findChannelIdsByTeamId(teamId)
            : List.of();

    List<ChannelModel> channelModels = channelModelGateway.findActiveByModelId(modelId);
    if (channelModels.isEmpty()) {
        throw new ResourceNotFoundException("ChannelModel", modelId);
    }

    // 过滤：只保留团队渠道内的 ChannelModel
    List<ChannelModel> permittedModels = teamChannelIds.isEmpty()
            ? List.of()
            : channelModels.stream()
                    .filter(cm -> teamChannelIds.contains(cm.getChannelId()))
                    .toList();

    // 再过滤活跃 Channel
    List<Long> channelIds = permittedModels.stream().map(ChannelModel::getChannelId).toList();
    List<Channel> activeChannels = channelGateway.findByIds(channelIds).stream()
            .filter(ch -> ch.getState() == ChannelState.ACTIVE)
            .toList();
    Set<Long> activeChannelIds = activeChannels.stream().map(Channel::getId).collect(Collectors.toSet());

    List<ChannelModel> activeModels = permittedModels.stream()
            .filter(cm -> activeChannelIds.contains(cm.getChannelId()))
            .toList();

    if (activeModels.isEmpty()) {
        throw new ResourceNotFoundException("ChannelModel", modelId);
    }

    return activeModels.getFirst();
}
```

- [ ] **Step 2: 提交**

```bash
git add -u
git commit -m "feat: ChannelSelector 注入团队渠道过滤"
```

---

### Task 8: RoutingResolver 和 ChatDispatchServiceImpl 传递 userId

**Files:**
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/application/proxy/routing/RoutingResolver.java`
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/application/proxy/ChatDispatchServiceImpl.java`

- [ ] **Step 1: RoutingResolver.resolve 增加 userId 参数**

修改签名：
```java
public RoutingContext resolve(String modelName, Protocol protocol, Long userId) {
```

修改第 39 行调用：
```java
ChannelModel channelModel = channelSelector.select(model.getId(), userId);
```

- [ ] **Step 2: ChatDispatchServiceImpl — 传递 userId**

修改第 61 行：
```java
RoutingContext ctx = routingResolver.resolve(request.getModel(), inboundProtocol, identity.userId());
```

修改第 110 行（stream dispatch）：
```java
RoutingContext ctx = routingResolver.resolve(request.getModel(), inboundProtocol, identity.userId());
```

- [ ] **Step 3: 提交**

```bash
git add -u
git commit -m "feat: RoutingResolver 和 ChatDispatchServiceImpl 传递 userId"
```

---

### Task 9: ModelDiscoveryService 改为通过团队查渠道

**Files:**
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/application/model/ModelDiscoveryService.java`
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/adapter/api/ModelDiscoveryController.java`

- [ ] **Step 1: ModelDiscoveryService — 新增 userId 参数，改为团队渠道查询**

修改方法签名：`getVisibleModels(Long apiKeyId)` → `getVisibleModels(Long userId)`

新增 `UserTeamGateway` 和 `TeamChannelGateway` 依赖。

修改逻辑：
```java
public ModelDiscoveryResponse getVisibleModels(Long userId) {
    Long teamId = userTeamGateway.findTeamIdByUserId(userId);
    if (teamId == null) {
        return new ModelDiscoveryResponse("list", List.of());
    }

    List<Long> channelIds = teamChannelGateway.findChannelIdsByTeamId(teamId);
    if (channelIds.isEmpty()) {
        return new ModelDiscoveryResponse("list", List.of());
    }

    List<Model> visibleModels = channelIds.stream()
            .flatMap(channelId -> channelModelGateway.findActiveByChannelId(channelId).stream())
            .map(cm -> modelGateway.findById(cm.getModelId()).orElse(null))
            .filter(m -> m != null && ModelState.ACTIVE.equals(m.getState()))
            .distinct()
            .toList();

    // ... 构建 ModelItem 列表（同原逻辑）
}
```

- [ ] **Step 2: ModelDiscoveryController — 传递 userId 而非 credentialId**

修改第 37 行：
```java
return modelDiscoveryService.getVisibleModels(identity.userId());
```

- [ ] **Step 3: 提交**

```bash
git add -u
git commit -m "feat: ModelDiscoveryService 改为通过用户团队查渠道"
```

---

### Task 10: TeamController — 新增渠道管理端点 + 清理 createApiKey

**Files:**
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/adapter/api/TeamController.java`

- [ ] **Step 1: 新增团队渠道管理端点**

注入 `TeamChannelGateway` 和 `ChannelGateway`。

```java
/** 查询团队的渠道列表 */
@GetMapping("/{teamId}/channels")
public List<Long> listChannels(@PathVariable Long teamId) {
    return teamChannelGateway.findChannelIdsByTeamId(teamId);
}

/** 更新团队的渠道列表（全量替换） */
@PutMapping("/{teamId}/channels")
public ResponseEntity<Void> updateChannels(
        @PathVariable Long teamId,
        @RequestBody List<Long> channelIds) {
    teamChannelGateway.deleteByTeamId(teamId);
    for (Long channelId : channelIds) {
        teamChannelGateway.save(new TeamChannel(teamId, channelId));
    }
    return ResponseEntity.ok().build();
}
```

- [ ] **Step 2: createApiKey — 移除 channelIds 参数**

修改第 133-136 行：
```java
UserApiKeyCreateRequest fixedRequest = new UserApiKeyCreateRequest(
        targetUserId, request.name(),
        request.models(), request.quotaLimit()
);
```

- [ ] **Step 3: 提交**

```bash
git add -u
git commit -m "feat: TeamController 新增渠道管理端点，移除 createApiKey 的 channelIds"
```

---

### Task 11: 前端 — 类型清理 + API 新增 + 渠道管理弹窗

**Files:**
- Modify: `gateway-console/src/types/team.ts`
- Modify: `gateway-console/src/services/api/team.ts`
- Create: `gateway-console/src/pages/Teams/ChannelManageModal.tsx`
- Modify: `gateway-console/src/pages/Teams/index.tsx`

- [ ] **Step 1: types/team.ts — 清理类型**

删除 `ChannelBrief` 接口。
修改 `UserApiKey`：删除 `teamId` 字段。
修改 `UserApiKeyDetail`：删除 `channels` 字段，删除 `extends UserApiKey`（直接内联字段）。
修改 `CreateUserApiKeyRequest`：删除 `teamId`、`productIds` 字段。

- [ ] **Step 2: services/api/team.ts — 新增渠道管理方法**

```typescript
/** 查询团队的渠道列表 */
listChannels: (teamId: number) =>
  api.get<number[]>(`/teams/${teamId}/channels`),

/** 更新团队的渠道列表 */
updateChannels: (teamId: number, channelIds: number[]) =>
  api.put<void>(`/teams/${teamId}/channels`, channelIds),
```

- [ ] **Step 3: 新建 ChannelManageModal.tsx**

团队渠道管理弹窗：
- 从 channelApi 获取所有渠道列表
- 从 teamApi.listChannels(teamId) 获取当前团队渠道
- Checkbox 列表展示，勾选 = 团队可访问
- 保存调用 teamApi.updateChannels(teamId, channelIds)
- 提示文案："配置该团队可访问的渠道，团队成员的 API Key 将继承这些渠道权限"

- [ ] **Step 4: Teams/index.tsx — 新增"渠道管理"按钮**

操作列新增按钮，打开 ChannelManageModal。

- [ ] **Step 5: 提交**

```bash
git add -u
git add gateway-console/src/pages/Teams/ChannelManageModal.tsx
git commit -m "feat: 前端团队渠道管理弹窗 + 类型清理"
```

---

### Task 12: 构建验证

- [ ] **Step 1: 运行构建**

```bash
./mvnw clean install -DskipTests
```

预期：BUILD SUCCESS

- [ ] **Step 2: 提交最终状态**

如有编译错误，修复后提交。
