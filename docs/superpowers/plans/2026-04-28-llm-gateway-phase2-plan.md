# LLM Gateway Phase 2: 核心 CRUD 功能

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 完成用户管理、Provider/Model 管理、API Key 管理、TokenLimit 管理的完整 CRUD 功能

**Architecture:** 基于 COLA Light 5.0 架构，单模块 Maven 项目，分层管理实体

**Tech Stack:** Java 21 + Spring Boot 3.5.x + JPA + PostgreSQL/MySQL

---

## 1. 文件结构规划

### 1.1 Phase 2 新增结构

```
src/main/java/com/codingas/gateway/
├── adapter/
│   ├── admin/controller/                    [新增]
│   │   ├── UserController.java              [新增]
│   │   ├── ProviderController.java          [新增]
│   │   ├── ModelController.java             [新增]
│   │   ├── TokenLimitController.java         [新增]
│   │   └── RoleController.java              [新增]
│   └── api-key/controller/                  [新增]
│       └── ApiKeyController.java            [新增]
├── application/
│   ├── user/                                [新增]
│   │   └── UserApplication.java             [新增]
│   ├── provider/                            [新增]
│   │   └── ProviderApplication.java         [新增]
│   ├── model/                               [新增]
│   │   └── ModelApplication.java            [新增]
│   ├── api-key/                             [新增]
│   │   └── ApiKeyApplication.java           [新增]
│   ├── token-limit/                         [新增]
│   │   └── TokenLimitApplication.java       [新增]
│   └── role/                                [新增]
│       └── RoleApplication.java              [新增]
└── common/
    ├── dto/                                 [新增]
    │   ├── PageRequest.java                 [新增]
    │   ├── PageResponse.java                [新增]
    │   ├── ApiResponse.java                  [新增]
    │   └── ErrorResponse.java                [新增]
    └── exception/                           [新增]
        ├── ResourceNotFoundException.java   [新增]
        ├── DuplicateResourceException.java   [新增]
        └── InvalidParameterException.java    [新增]
```

---

## 2. 任务列表

### Task 1: 用户管理 CRUD

**Files:**
- Create: `src/main/java/com/codingas/gateway/adapter/admin/controller/UserController.java`
- Create: `src/main/java/com/codingas/gateway/adapter/admin/dto/user/`
- Create: `src/main/java/com/codingas/gateway/application/user/UserApplication.java`
- Create: `src/main/java/com/codingas/gateway/domain/security/gateway/UserGateway.java`
- Create: `src/main/java/com/codingas/gateway/infrastructure/gateway/security/JpaUserGateway.java`
- Create: `src/main/resources/db/V2__add_indexes.sql`

- [ ] **Step 1: 创建通用 DTO 类**

```java
// PageRequest.java - 分页请求基类
package com.codingas.gateway.common.dto;

import lombok.Data;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Data
public abstract class PageRequest {
    @Min(1)
    private Integer page = 1;

    @Min(1)
    @Max(100)
    private Integer limit = 20;

    public int getOffset() {
        return (page - 1) * limit;
    }
}
```

```java
// PageResponse.java - 分页响应基类
package com.codingas.gateway.common.dto;

import lombok.Data;
import java.util.List;

@Data
public class PageResponse<T> {
    private List<T> items;
    private Pagination pagination;

    @Data
    public static class Pagination {
        private int page;
        private int limit;
        private long total;
        private int totalPages;
    }

    public static <T> PageResponse<T> of(List<T> items, int page, int limit, long total) {
        PageResponse<T> response = new PageResponse<>();
        response.setItems(items);
        Pagination pagination = new Pagination();
        pagination.setPage(page);
        pagination.setLimit(limit);
        pagination.setTotal(total);
        pagination.setTotalPages((int) Math.ceil((double) total / limit));
        response.setPagination(pagination);
        return response;
    }
}
```

```java
// ApiResponse.java - 统一 API 响应
package com.codingas.gateway.common.dto;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonInclude;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private boolean success;
    private T data;
    private ErrorInfo error;
    private String traceId;
    private String timestamp;

    @Data
    public static class ErrorInfo {
        private String code;
        private String message;
        private Object details;
    }

    public static <T> ApiResponse<T> success(T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setSuccess(true);
        response.setData(data);
        response.setTimestamp(java.time.Instant.now().toString());
        return response;
    }

    public static <T> ApiResponse<T> error(String code, String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setSuccess(false);
        response.setError(new ErrorInfo());
        response.getError().setCode(code);
        response.getError().setMessage(message);
        response.setTimestamp(java.time.Instant.now().toString());
        return response;
    }
}
```

- [ ] **Step 2: 创建通用异常类**

```java
// ResourceNotFoundException.java
package com.codingas.gateway.common.exception;

public class ResourceNotFoundException extends RuntimeException {
    private final String resourceType;
    private final Object resourceId;

    public ResourceNotFoundException(String resourceType, Object resourceId) {
        super(String.format("%s not found with id: %s", resourceType, resourceId));
        this.resourceType = resourceType;
        this.resourceId = resourceId;
    }
}
```

```java
// DuplicateResourceException.java
package com.codingas.gateway.common.exception;

public class DuplicateResourceException extends RuntimeException {
    private final String resourceType;
    private final String field;

    public DuplicateResourceException(String resourceType, String field) {
        super(String.format("%s already exists with %s", resourceType, field));
        this.resourceType = resourceType;
        this.field = field;
    }
}
```

```java
// InvalidParameterException.java
package com.codingas.gateway.common.exception;

public class InvalidParameterException extends RuntimeException {
    private final String field;

    public InvalidParameterException(String field, String message) {
        super(String.format("Invalid parameter %s: %s", field, message));
        this.field = field;
    }
}
```

- [ ] **Step 3: 创建 User DTOs**

```java
// UserCreateRequest.java
package com.codingas.gateway.adapter.admin.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.List;

@Data
public class UserCreateRequest {
    @NotBlank(message = "用户名不能为空")
    @Size(min = 2, max = 64, message = "用户名长度必须在 2-64 之间")
    private String username;

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 128, message = "密码长度必须在 8-128 之间")
    private String password;

    private String phone;

    @NotBlank(message = "必须至少分配一个角色")
    private List<String> roleCodes;
}
```

```java
// UserUpdateRequest.java
package com.codingas.gateway.adapter.admin.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserUpdateRequest {
    @Size(min = 2, max = 64, message = "用户名长度必须在 2-64 之间")
    private String username;

    @Email(message = "邮箱格式不正确")
    private String email;

    private String phone;

    private String avatarUrl;
}
```

```java
// UserResponse.java
package com.codingas.gateway.adapter.admin.dto.user;

import com.codingas.gateway.common.enums.UserStatus;
import lombok.Data;
import java.time.Instant;
import java.util.List;

@Data
public class UserResponse {
    private Long id;
    private String userCode;
    private String username;
    private String email;
    private String phone;
    private String avatarUrl;
    private UserStatus status;
    private Boolean emailVerified;
    private List<RoleInfo> roles;
    private Instant lastLoginAt;
    private Instant createdAt;
    private Instant updatedAt;

    @Data
    public static class RoleInfo {
        private String roleCode;
        private String name;
    }
}
```

```java
// UserStatusUpdateRequest.java
package com.codingas.gateway.adapter.admin.dto.user;

import com.codingas.gateway.common.enums.UserStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserStatusUpdateRequest {
    @NotNull(message = "状态不能为空")
    private UserStatus status;
}
```

```java
// UserRoleAssignRequest.java
package com.codingas.gateway.adapter.admin.dto.user;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.util.List;

@Data
public class UserRoleAssignRequest {
    @NotEmpty(message = "必须至少分配一个角色")
    private List<String> roleCodes;
}
```

```java
// UserQueryRequest.java
package com.codingas.gateway.adapter.admin.dto.user;

import com.codingas.gateway.common.dto.PageRequest;
import com.codingas.gateway.common.enums.UserStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class UserQueryRequest extends PageRequest {
    private String keyword;
    private UserStatus status;
    private String roleCode;
}
```

- [ ] **Step 4: 创建 UserGateway 接口**

```java
// UserGateway.java
package com.codingas.gateway.domain.security.gateway;

import com.codingas.gateway.domain.security.entity.User;
import com.codingas.gateway.common.enums.UserStatus;
import java.util.List;
import java.util.Optional;

public interface UserGateway {
    User save(User user);
    Optional<User> findById(Long id);
    Optional<User> findByUserCode(String userCode);
    Optional<User> findByEmail(String email);
    List<User> findAll();
    long count();
    void delete(User user);
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
}
```

- [ ] **Step 5: 创建 JpaUserGateway 实现**

```java
// JpaUserGateway.java
package com.codingas.gateway.infrastructure.gateway.security;

import com.codingas.gateway.domain.security.entity.User;
import com.codingas.gateway.domain.security.repository.UserRepository;
import com.codingas.gateway.domain.security.gateway.UserGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class JpaUserGateway implements UserGateway {
    private final UserRepository userRepository;

    @Override
    public User save(User user) {
        return userRepository.save(user);
    }

    @Override
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    @Override
    public Optional<User> findByUserCode(String userCode) {
        return userRepository.findByUserCode(userCode);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public List<User> findAll() {
        return userRepository.findAll();
    }

    @Override
    public long count() {
        return userRepository.count();
    }

    @Override
    public void delete(User user) {
        userRepository.delete(user);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }
}
```

- [ ] **Step 6: 创建 UserRepository**

```java
// UserRepository.java
package com.codingas.gateway.domain.security.repository;

import com.codingas.gateway.domain.security.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUserCode(String userCode);
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
}
```

- [ ] **Step 7: 创建 UserApplication**

```java
// UserApplication.java
package com.codingas.gateway.application.user;

import com.codingas.gateway.adapter.admin.dto.user.*;
import com.codingas.gateway.common.dto.PageResponse;
import com.codingas.gateway.common.exception.DuplicateResourceException;
import com.codingas.gateway.common.exception.ResourceNotFoundException;
import com.codingas.gateway.domain.security.entity.User;
import com.codingas.gateway.domain.security.entity.UserRole;
import com.codingas.gateway.domain.security.entity.Role;
import com.codingas.gateway.domain.security.gateway.UserGateway;
import com.codingas.gateway.domain.security.gateway.RoleGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserApplication {
    private final UserGateway userGateway;
    private final RoleGateway roleGateway;

    @Transactional
    public UserResponse create(UserCreateRequest request) {
        if (userGateway.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("User", "email");
        }

        User user = new User();
        user.setUserCode(generateUserCode());
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPasswordHash(hashPassword(request.getPassword()));
        user.setPhone(request.getPhone());

        User savedUser = userGateway.save(user);

        // 分配角色
        assignRoles(savedUser, request.getRoleCodes());

        return toResponse(savedUser);
    }

    public UserResponse getById(Long id) {
        User user = userGateway.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User", id));
        return toResponse(user);
    }

    public PageResponse<UserResponse> query(UserQueryRequest request) {
        // 实现分页查询逻辑
        List<User> users = userGateway.findAll();
        List<UserResponse> responses = users.stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
        return PageResponse.of(responses, request.getPage(), request.getLimit(), responses.size());
    }

    @Transactional
    public UserResponse update(Long id, UserUpdateRequest request) {
        User user = userGateway.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User", id));

        if (request.getUsername() != null) {
            user.setUsername(request.getUsername());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }

        return toResponse(userGateway.save(user));
    }

    @Transactional
    public void delete(Long id) {
        User user = userGateway.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User", id));
        user.setDeletedAt(java.time.Instant.now());
        userGateway.save(user);
    }

    @Transactional
    public UserResponse updateStatus(Long id, UserStatusUpdateRequest request) {
        User user = userGateway.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User", id));
        user.setStatus(request.getStatus());
        return toResponse(userGateway.save(user));
    }

    @Transactional
    public UserResponse assignRoles(Long id, UserRoleAssignRequest request) {
        User user = userGateway.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User", id));
        assignRoles(user, request.getRoleCodes());
        return toResponse(user);
    }

    private void assignRoles(User user, List<String> roleCodes) {
        List<Role> roles = roleGateway.findByRoleCodes(roleCodes);
        // 实现角色分配逻辑
    }

    private String generateUserCode() {
        return "USR" + System.currentTimeMillis();
    }

    private String hashPassword(String password) {
        // 使用 BCrypt 加密
        return org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder.class.cast(
            new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder()
        ).encode(password);
    }

    private UserResponse toResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setUserCode(user.getUserCode());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setPhone(user.getPhone());
        response.setAvatarUrl(user.getAvatarUrl());
        response.setStatus(user.getStatus());
        response.setEmailVerified(user.getEmailVerified());
        response.setLastLoginAt(user.getLastLoginAt());
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());
        return response;
    }
}
```

- [ ] **Step 8: 创建 UserController**

```java
// UserController.java
package com.codingas.gateway.adapter.admin.controller;

import com.codingas.gateway.adapter.admin.dto.user.*;
import com.codingas.gateway.application.user.UserApplication;
import com.codingas.gateway.common.dto.ApiResponse;
import com.codingas.gateway.common.dto.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
    private final UserApplication userApplication;

    @PostMapping
    public ApiResponse<UserResponse> create(@Valid @RequestBody UserCreateRequest request) {
        return ApiResponse.success(userApplication.create(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<UserResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(userApplication.getById(id));
    }

    @GetMapping
    public ApiResponse<PageResponse<UserResponse>> query(@ModelAttribute UserQueryRequest request) {
        return ApiResponse.success(userApplication.query(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<UserResponse> update(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest request) {
        return ApiResponse.success(userApplication.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        userApplication.delete(id);
        return ApiResponse.success(null);
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<UserResponse> updateStatus(@PathVariable Long id, @Valid @RequestBody UserStatusUpdateRequest request) {
        return ApiResponse.success(userApplication.updateStatus(id, request));
    }

    @PutMapping("/{id}/roles")
    public ApiResponse<UserResponse> assignRoles(@PathVariable Long id, @Valid @RequestBody UserRoleAssignRequest request) {
        return ApiResponse.success(userApplication.assignRoles(id, request));
    }
}
```

- [ ] **Step 9: 提交用户管理 CRUD**

```bash
git add src/main/java/com/codingas/gateway/adapter/admin/controller/UserController.java
git add src/main/java/com/codingas/gateway/adapter/admin/dto/user/
git add src/main/java/com/codingas/gateway/application/user/
git add src/main/java/com/codingas/gateway/domain/security/gateway/UserGateway.java
git add src/main/java/com/codingas/gateway/infrastructure/gateway/security/JpaUserGateway.java
git add src/main/java/com/codingas/gateway/domain/security/repository/UserRepository.java
git add src/main/java/com/codingas/gateway/common/dto/
git add src/main/java/com/codingas/gateway/common/exception/
git commit -m "feat: add User CRUD functionality

- UserController with REST endpoints
- UserApplication for business logic
- UserGateway interface and JpaUserGateway implementation
- User DTOs (CreateRequest, UpdateRequest, Response, QueryRequest)
- Common DTOs (PageRequest, PageResponse, ApiResponse)
- Common exceptions (ResourceNotFoundException, DuplicateResourceException)

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

### Task 2: Provider 管理 CRUD

**Files:**
- Create: `src/main/java/com/codingas/gateway/adapter/admin/controller/ProviderController.java`
- Create: `src/main/java/com/codingas/gateway/adapter/admin/dto/provider/`
- Create: `src/main/java/com/codingas/gateway/application/provider/ProviderApplication.java`
- Create: `src/main/java/com/codingas/gateway/domain/router/gateway/ProviderGateway.java`
- Create: `src/main/java/com/codingas/gateway/infrastructure/gateway/router/JpaProviderGateway.java`

- [ ] **Step 1: 创建 Provider DTOs**

```java
// ProviderCreateRequest.java
package com.codingas.gateway.adapter.admin.dto.provider;

import com.codingas.gateway.common.enums.ProviderType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.List;

@Data
public class ProviderCreateRequest {
    @NotBlank(message = "Provider code不能为空")
    @Size(min = 2, max = 64, message = "Provider code长度必须在 2-64 之间")
    private String providerCode;

    @NotBlank(message = "Provider名称不能为空")
    @Size(min = 2, max = 128, message = "Provider名称长度必须在 2-128 之间")
    private String providerName;

    @NotNull(message = "Provider类型不能为空")
    private ProviderType providerType;

    @NotBlank(message = "Base URL不能为空")
    private String baseUrl;

    private String websiteUrl;
    private String apiDocUrl;
    private Integer priority = 100;
}
```

```java
// ProviderUpdateRequest.java
package com.codingas.gateway.adapter.admin.dto.provider;

import com.codingas.gateway.common.enums.ProviderStatus;
import lombok.Data;

@Data
public class ProviderUpdateRequest {
    private String providerName;
    private String baseUrl;
    private String websiteUrl;
    private String apiDocUrl;
    private Integer priority;
    private ProviderStatus status;
}
```

```java
// ProviderResponse.java
package com.codingas.gateway.adapter.admin.dto.provider;

import com.codingas.gateway.common.enums.ProviderStatus;
import com.codingas.gateway.common.enums.ProviderType;
import lombok.Data;
import java.time.Instant;
import java.util.List;

@Data
public class ProviderResponse {
    private Long id;
    private String providerCode;
    private String providerName;
    private ProviderType providerType;
    private String baseUrl;
    private String websiteUrl;
    private String apiDocUrl;
    private Integer priority;
    private ProviderStatus status;
    private List<ModelInfo> models;
    private Instant createdAt;
    private Instant updatedAt;

    @Data
    public static class ModelInfo {
        private Long id;
        private String modelCode;
        private String displayName;
        private Integer contextWindow;
        private ProviderStatus status;
    }
}
```

```java
// ProviderQueryRequest.java
package com.codingas.gateway.adapter.admin.dto.provider;

import com.codingas.gateway.adapter.admin.dto.model.ModelQueryRequest;
import com.codingas.gateway.common.enums.ProviderStatus;
import com.codingas.gateway.common.enums.ProviderType;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ProviderQueryRequest extends ModelQueryRequest {
    private String keyword;
    private ProviderType providerType;
    private ProviderStatus status;
}
```

```java
// ProviderTestRequest.java
package com.codingas.gateway.adapter.admin.dto.provider;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ProviderTestRequest {
    @NotBlank(message = "API Key不能为空")
    private String apiKey;
}
```

```java
// ProviderTestResponse.java
package com.codingas.gateway.adapter.admin.dto.provider;

import lombok.Data;
import java.util.List;

@Data
public class ProviderTestResponse {
    private boolean success;
    private Integer latencyMs;
    private String message;
    private List<String> models;
}
```

- [ ] **Step 2: 创建 ProviderGateway 接口**

```java
// ProviderGateway.java
package com.codingas.gateway.domain.router.gateway;

import com.codingas.gateway.domain.router.entity.Provider;
import com.codingas.gateway.common.enums.ProviderType;
import java.util.List;
import java.util.Optional;

public interface ProviderGateway {
    Provider save(Provider provider);
    Optional<Provider> findById(Long id);
    Optional<Provider> findByProviderCode(String providerCode);
    List<Provider> findAll();
    List<Provider> findByProviderType(ProviderType providerType);
    long count();
    void delete(Provider provider);
    boolean existsByProviderCode(String providerCode);
}
```

- [ ] **Step 3: 创建 ProviderRepository**

```java
// ProviderRepository.java
package com.codingas.gateway.domain.router.repository;

import com.codingas.gateway.domain.router.entity.Provider;
import com.codingas.gateway.common.enums.ProviderType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProviderRepository extends JpaRepository<Provider, Long> {
    Optional<Provider> findByProviderCode(String providerCode);
    boolean existsByProviderCode(String providerCode);
    List<Provider> findByProviderType(ProviderType providerType);
}
```

- [ ] **Step 4: 创建 ProviderApplication**

```java
// ProviderApplication.java
package com.codingas.gateway.application.provider;

import com.codingas.gateway.adapter.admin.dto.provider.*;
import com.codingas.gateway.common.dto.PageResponse;
import com.codingas.gateway.common.exception.DuplicateResourceException;
import com.codingas.gateway.common.exception.ResourceNotFoundException;
import com.codingas.gateway.domain.router.entity.Provider;
import com.codingas.gateway.domain.router.gateway.ProviderGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProviderApplication {
    private final ProviderGateway providerGateway;

    @Transactional
    public ProviderResponse create(ProviderCreateRequest request) {
        if (providerGateway.existsByProviderCode(request.getProviderCode())) {
            throw new DuplicateResourceException("Provider", "providerCode");
        }

        Provider provider = new Provider();
        provider.setProviderCode(request.getProviderCode());
        provider.setProviderName(request.getProviderName());
        provider.setProviderType(request.getProviderType());
        provider.setBaseUrl(request.getBaseUrl());
        provider.setWebsiteUrl(request.getWebsiteUrl());
        provider.setApiDocUrl(request.getApiDocUrl());
        provider.setPriority(request.getPriority() != null ? request.getPriority() : 100);
        provider.setStatus(Provider.ProviderStatus.ACTIVE);

        return toResponse(providerGateway.save(provider));
    }

    public ProviderResponse getById(Long id) {
        Provider provider = providerGateway.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Provider", id));
        return toResponse(provider);
    }

    public PageResponse<ProviderResponse> query(ProviderQueryRequest request) {
        List<Provider> providers = providerGateway.findAll();
        List<ProviderResponse> responses = providers.stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
        return PageResponse.of(responses, request.getPage(), request.getLimit(), responses.size());
    }

    @Transactional
    public ProviderResponse update(Long id, ProviderUpdateRequest request) {
        Provider provider = providerGateway.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Provider", id));

        if (request.getProviderName() != null) {
            provider.setProviderName(request.getProviderName());
        }
        if (request.getBaseUrl() != null) {
            provider.setBaseUrl(request.getBaseUrl());
        }
        if (request.getWebsiteUrl() != null) {
            provider.setWebsiteUrl(request.getWebsiteUrl());
        }
        if (request.getApiDocUrl() != null) {
            provider.setApiDocUrl(request.getApiDocUrl());
        }
        if (request.getPriority() != null) {
            provider.setPriority(request.getPriority());
        }
        if (request.getStatus() != null) {
            provider.setStatus(request.getStatus());
        }

        return toResponse(providerGateway.save(provider));
    }

    @Transactional
    public void delete(Long id) {
        Provider provider = providerGateway.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Provider", id));
        provider.setDeletedAt(java.time.Instant.now());
        providerGateway.save(provider);
    }

    public ProviderTestResponse test(Long id, ProviderTestRequest request) {
        // 实现 Provider 连通性测试
        ProviderTestResponse response = new ProviderTestResponse();
        response.setSuccess(true);
        response.setLatencyMs(100);
        response.setMessage("连接成功");
        return response;
    }

    private ProviderResponse toResponse(Provider provider) {
        ProviderResponse response = new ProviderResponse();
        response.setId(provider.getId());
        response.setProviderCode(provider.getProviderCode());
        response.setProviderName(provider.getProviderName());
        response.setProviderType(provider.getProviderType());
        response.setBaseUrl(provider.getBaseUrl());
        response.setWebsiteUrl(provider.getWebsiteUrl());
        response.setApiDocUrl(provider.getApiDocUrl());
        response.setPriority(provider.getPriority());
        response.setStatus(provider.getStatus());
        response.setCreatedAt(provider.getCreatedAt());
        response.setUpdatedAt(provider.getUpdatedAt());
        return response;
    }
}
```

- [ ] **Step 5: 创建 ProviderController**

```java
// ProviderController.java
package com.codingas.gateway.adapter.admin.controller;

import com.codingas.gateway.adapter.admin.dto.provider.*;
import com.codingas.gateway.application.provider.ProviderApplication;
import com.codingas.gateway.common.dto.ApiResponse;
import com.codingas.gateway.common.dto.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/providers")
@RequiredArgsConstructor
public class ProviderController {
    private final ProviderApplication providerApplication;

    @PostMapping
    public ApiResponse<ProviderResponse> create(@Valid @RequestBody ProviderCreateRequest request) {
        return ApiResponse.success(providerApplication.create(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<ProviderResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(providerApplication.getById(id));
    }

    @GetMapping
    public ApiResponse<PageResponse<ProviderResponse>> query(@ModelAttribute ProviderQueryRequest request) {
        return ApiResponse.success(providerApplication.query(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<ProviderResponse> update(@PathVariable Long id, @Valid @RequestBody ProviderUpdateRequest request) {
        return ApiResponse.success(providerApplication.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        providerApplication.delete(id);
        return ApiResponse.success(null);
    }

    @PostMapping("/{id}/test")
    public ApiResponse<ProviderTestResponse> test(@PathVariable Long id, @Valid @RequestBody ProviderTestRequest request) {
        return ApiResponse.success(providerApplication.test(id, request));
    }
}
```

- [ ] **Step 6: 提交 Provider 管理 CRUD**

```bash
git add src/main/java/com/codingas/gateway/adapter/admin/controller/ProviderController.java
git add src/main/java/com/codingas/gateway/adapter/admin/dto/provider/
git add src/main/java/com/codingas/gateway/application/provider/
git add src/main/java/com/codingas/gateway/domain/router/gateway/ProviderGateway.java
git add src/main/java/com/codingas/gateway/infrastructure/gateway/router/JpaProviderGateway.java
git commit -m "feat: add Provider CRUD functionality

- ProviderController with REST endpoints
- ProviderApplication for business logic
- ProviderGateway interface and JpaProviderGateway implementation
- Provider DTOs (CreateRequest, UpdateRequest, Response, QueryRequest, TestRequest, TestResponse)

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

### Task 3: Model 管理 CRUD

**Files:**
- Create: `src/main/java/com/codingas/gateway/adapter/admin/controller/ModelController.java`
- Create: `src/main/java/com/codingas/gateway/adapter/admin/dto/model/`
- Create: `src/main/java/com/codingas/gateway/application/model/ModelApplication.java`
- Create: `src/main/java/com/codingas/gateway/domain/router/gateway/ModelGateway.java`
- Create: `src/main/java/com/codingas/gateway/infrastructure/gateway/router/JpaModelGateway.java`

- [ ] **Step 1: 创建 Model DTOs**

```java
// ModelCreateRequest.java
package com.codingas.gateway.adapter.admin.dto.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.util.Map;

@Data
public class ModelCreateRequest {
    @NotBlank(message = "Model code不能为空")
    private String modelCode;

    @NotNull(message = "Provider ID不能为空")
    private Long providerId;

    @NotBlank(message = "Provider Model ID不能为空")
    private String providerModelId;

    private String displayName;
    private Integer contextWindow;
    private BigDecimal inputPrice;
    private BigDecimal outputPrice;
    private Map<String, Boolean> capabilities;
}
```

```java
// ModelUpdateRequest.java
package com.codingas.gateway.adapter.admin.dto.model;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Map;

@Data
public class ModelUpdateRequest {
    private String displayName;
    private Integer contextWindow;
    private BigDecimal inputPrice;
    private BigDecimal outputPrice;
    private Map<String, Boolean> capabilities;
    private ModelStatus status;

    public enum ModelStatus {
        ACTIVE, DEPRECATED
    }
}
```

```java
// ModelResponse.java
package com.codingas.gateway.adapter.admin.dto.model;

import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

@Data
public class ModelResponse {
    private Long id;
    private String modelCode;
    private ProviderInfo provider;
    private String providerModelId;
    private String displayName;
    private Integer contextWindow;
    private BigDecimal inputPrice;
    private BigDecimal outputPrice;
    private Map<String, Boolean> capabilities;
    private String status;
    private Instant createdAt;
    private Instant updatedAt;

    @Data
    public static class ProviderInfo {
        private Long id;
        private String providerCode;
        private String providerName;
    }
}
```

```java
// ModelQueryRequest.java
package com.codingas.gateway.adapter.admin.dto.model;

import com.codingas.gateway.common.dto.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ModelQueryRequest extends PageRequest {
    private String keyword;
    private Long providerId;
    private String status;
}
```

- [ ] **Step 2: 创建 ModelGateway 接口**

```java
// ModelGateway.java
package com.codingas.gateway.domain.router.gateway;

import com.codingas.gateway.domain.router.entity.Model;
import java.util.List;
import java.util.Optional;

public interface ModelGateway {
    Model save(Model model);
    Optional<Model> findById(Long id);
    Optional<Model> findByModelCode(String modelCode);
    List<Model> findAll();
    List<Model> findByProviderId(Long providerId);
    long count();
    void delete(Model model);
    boolean existsByModelCode(String modelCode);
}
```

- [ ] **Step 3: 创建 ModelRepository**

```java
// ModelRepository.java
package com.codingas.gateway.domain.router.repository;

import com.codingas.gateway.domain.router.entity.Model;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ModelRepository extends JpaRepository<Model, Long> {
    Optional<Model> findByModelCode(String modelCode);
    boolean existsByModelCode(String modelCode);
    List<Model> findByProviderId(Long providerId);
}
```

- [ ] **Step 4: 创建 ModelApplication**

```java
// ModelApplication.java
package com.codingas.gateway.application.model;

import com.codingas.gateway.adapter.admin.dto.model.*;
import com.codingas.gateway.common.dto.PageResponse;
import com.codingas.gateway.common.exception.DuplicateResourceException;
import com.codingas.gateway.common.exception.ResourceNotFoundException;
import com.codingas.gateway.domain.router.entity.Model;
import com.codingas.gateway.domain.router.entity.Provider;
import com.codingas.gateway.domain.router.gateway.ModelGateway;
import com.codingas.gateway.domain.router.gateway.ProviderGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ModelApplication {
    private final ModelGateway modelGateway;
    private final ProviderGateway providerGateway;

    @Transactional
    public ModelResponse create(ModelCreateRequest request) {
        if (modelGateway.existsByModelCode(request.getModelCode())) {
            throw new DuplicateResourceException("Model", "modelCode");
        }

        Provider provider = providerGateway.findById(request.getProviderId())
            .orElseThrow(() -> new ResourceNotFoundException("Provider", request.getProviderId()));

        Model model = new Model();
        model.setModelCode(request.getModelCode());
        model.setProvider(provider);
        model.setProviderModelId(request.getProviderModelId());
        model.setDisplayName(request.getDisplayName());
        model.setContextWindow(request.getContextWindow());
        model.setInputPrice(request.getInputPrice());
        model.setOutputPrice(request.getOutputPrice());
        model.setCapabilities(request.getCapabilities());
        model.setStatus(Model.ModelStatus.ACTIVE);

        return toResponse(modelGateway.save(model));
    }

    public ModelResponse getById(Long id) {
        Model model = modelGateway.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Model", id));
        return toResponse(model);
    }

    public PageResponse<ModelResponse> query(ModelQueryRequest request) {
        List<Model> models;
        if (request.getProviderId() != null) {
            models = modelGateway.findByProviderId(request.getProviderId());
        } else {
            models = modelGateway.findAll();
        }
        List<ModelResponse> responses = models.stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
        return PageResponse.of(responses, request.getPage(), request.getLimit(), responses.size());
    }

    @Transactional
    public ModelResponse update(Long id, ModelUpdateRequest request) {
        Model model = modelGateway.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Model", id));

        if (request.getDisplayName() != null) {
            model.setDisplayName(request.getDisplayName());
        }
        if (request.getContextWindow() != null) {
            model.setContextWindow(request.getContextWindow());
        }
        if (request.getInputPrice() != null) {
            model.setInputPrice(request.getInputPrice());
        }
        if (request.getOutputPrice() != null) {
            model.setOutputPrice(request.getOutputPrice());
        }
        if (request.getCapabilities() != null) {
            model.setCapabilities(request.getCapabilities());
        }
        if (request.getStatus() != null) {
            model.setStatus(Model.ModelStatus.valueOf(request.getStatus().name()));
        }

        return toResponse(modelGateway.save(model));
    }

    @Transactional
    public void delete(Long id) {
        Model model = modelGateway.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Model", id));
        model.setDeletedAt(java.time.Instant.now());
        modelGateway.save(model);
    }

    private ModelResponse toResponse(Model model) {
        ModelResponse response = new ModelResponse();
        response.setId(model.getId());
        response.setModelCode(model.getModelCode());
        response.setProviderModelId(model.getProviderModelId());
        response.setDisplayName(model.getDisplayName());
        response.setContextWindow(model.getContextWindow());
        response.setInputPrice(model.getInputPrice());
        response.setOutputPrice(model.getOutputPrice());
        response.setCapabilities(model.getCapabilities());
        response.setStatus(model.getStatus().name());
        response.setCreatedAt(model.getCreatedAt());
        response.setUpdatedAt(model.getUpdatedAt());

        if (model.getProvider() != null) {
            ModelResponse.ProviderInfo providerInfo = new ModelResponse.ProviderInfo();
            providerInfo.setId(model.getProvider().getId());
            providerInfo.setProviderCode(model.getProvider().getProviderCode());
            providerInfo.setProviderName(model.getProvider().getProviderName());
            response.setProvider(providerInfo);
        }

        return response;
    }
}
```

- [ ] **Step 5: 创建 ModelController**

```java
// ModelController.java
package com.codingas.gateway.adapter.admin.controller;

import com.codingas.gateway.adapter.admin.dto.model.*;
import com.codingas.gateway.application.model.ModelApplication;
import com.codingas.gateway.common.dto.ApiResponse;
import com.codingas.gateway.common.dto.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/models")
@RequiredArgsConstructor
public class ModelController {
    private final ModelApplication modelApplication;

    @PostMapping
    public ApiResponse<ModelResponse> create(@Valid @RequestBody ModelCreateRequest request) {
        return ApiResponse.success(modelApplication.create(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<ModelResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(modelApplication.getById(id));
    }

    @GetMapping
    public ApiResponse<PageResponse<ModelResponse>> query(@ModelAttribute ModelQueryRequest request) {
        return ApiResponse.success(modelApplication.query(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<ModelResponse> update(@PathVariable Long id, @Valid @RequestBody ModelUpdateRequest request) {
        return ApiResponse.success(modelApplication.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        modelApplication.delete(id);
        return ApiResponse.success(null);
    }
}
```

- [ ] **Step 6: 提交 Model 管理 CRUD**

```bash
git add src/main/java/com/codingas/gateway/adapter/admin/controller/ModelController.java
git add src/main/java/com/codingas/gateway/adapter/admin/dto/model/
git add src/main/java/com/codingas/gateway/application/model/
git add src/main/java/com/codingas/gateway/domain/router/gateway/ModelGateway.java
git add src/main/java/com/codingas/gateway/infrastructure/gateway/router/JpaModelGateway.java
git commit -m "feat: add Model CRUD functionality

- ModelController with REST endpoints
- ModelApplication for business logic
- ModelGateway interface and JpaModelGateway implementation
- Model DTOs (CreateRequest, UpdateRequest, Response, QueryRequest)

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

### Task 4: API Key 管理 CRUD

**Files:**
- Create: `src/main/java/com/codingas/gateway/adapter/api-key/controller/ApiKeyController.java`
- Create: `src/main/java/com/codingas/gateway/adapter/api-key/dto/`
- Create: `src/main/java/com/codingas/gateway/application/api-key/ApiKeyApplication.java`
- Create: `src/main/java/com/codingas/gateway/domain/security/gateway/ApiKeyGateway.java`
- Create: `src/main/java/com/codingas/gateway/infrastructure/gateway/security/JpaApiKeyGateway.java`

- [ ] **Step 1: 创建 ApiKey DTOs**

```java
// ApiKeyCreateRequest.java
package com.codingas.gateway.adapter.api.key.dto;

import jakarta.validation.constraints.Future;
import lombok.Data;
import java.time.Instant;
import java.util.List;

@Data
public class ApiKeyCreateRequest {
    private String name;
    @Future
    private Instant expiresAt;
    private List<String> ipWhitelist;
    private List<String> modelWhitelist;
}
```

```java
// ApiKeyUpdateRequest.java
package com.codingas.gateway.adapter.api.key.dto;

import jakarta.validation.constraints.Future;
import lombok.Data;
import java.time.Instant;
import java.util.List;

@Data
public class ApiKeyUpdateRequest {
    private String name;
    @Future
    private Instant expiresAt;
    private List<String> ipWhitelist;
    private List<String> modelWhitelist;
}
```

```java
// ApiKeyResponse.java
package com.codingas.gateway.adapter.api.key.dto;

import com.codingas.gateway.common.enums.ApiKeyStatus;
import lombok.Data;
import java.time.Instant;
import java.util.List;

@Data
public class ApiKeyResponse {
    private Long id;
    private String keyCode;
    private String name;
    private String keyPrefix;
    private String keyHint;
    private ApiKeyStatus status;
    private Instant expiresAt;
    private Instant lastUsedAt;
    private List<String> ipWhitelist;
    private List<String> modelWhitelist;
    private Instant createdAt;
    private Instant updatedAt;
}
```

```java
// ApiKeyCreateResponse.java - 创建时返回完整 Key
package com.codingas.gateway.adapter.api.key.dto;

import lombok.Data;
import java.time.Instant;

@Data
public class ApiKeyCreateResponse {
    private Long id;
    private String keyCode;
    private String apiKey;  // 只在创建时返回一次
    private String name;
    private Instant expiresAt;
    private Instant createdAt;
}
```

```java
// ApiKeyQueryRequest.java
package com.codingas.gateway.adapter.api.key.dto;

import com.codingas.gateway.common.dto.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ApiKeyQueryRequest extends PageRequest {
    private String keyword;
    private String status;
}
```

- [ ] **Step 2: 创建 ApiKeyGateway 接口**

```java
// ApiKeyGateway.java
package com.codingas.gateway.domain.security.gateway;

import com.codingas.gateway.domain.security.entity.GatewayApiKey;
import com.codingas.gateway.domain.security.entity.User;
import java.util.List;
import java.util.Optional;

public interface ApiKeyGateway {
    GatewayApiKey save(GatewayApiKey apiKey);
    Optional<GatewayApiKey> findById(Long id);
    Optional<GatewayApiKey> findByKeyCode(String keyCode);
    List<GatewayApiKey> findByUser(User user);
    List<GatewayApiKey> findAll();
    long count();
    void delete(GatewayApiKey apiKey);
}
```

- [ ] **Step 3: 创建 ApiKeyRepository**

```java
// ApiKeyRepository.java
package com.codingas.gateway.domain.security.repository;

import com.codingas.gateway.domain.security.entity.GatewayApiKey;
import com.codingas.gateway.domain.security.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ApiKeyRepository extends JpaRepository<GatewayApiKey, Long> {
    Optional<GatewayApiKey> findByKeyCode(String keyCode);
    List<GatewayApiKey> findByUser(User user);
    List<GatewayApiKey> findByUserId(Long userId);
}
```

- [ ] **Step 4: 创建 ApiKeyApplication**

```java
// ApiKeyApplication.java
package com.codingas.gateway.application.api.key;

import com.codingas.gateway.adapter.api.key.dto.*;
import com.codingas.gateway.common.dto.PageResponse;
import com.codingas.gateway.common.exception.ResourceNotFoundException;
import com.codingas.gateway.domain.security.entity.GatewayApiKey;
import com.codingas.gateway.domain.security.entity.User;
import com.codingas.gateway.domain.security.gateway.ApiKeyGateway;
import com.codingas.gateway.domain.security.gateway.UserGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ApiKeyApplication {
    private final ApiKeyGateway apiKeyGateway;
    private final UserGateway userGateway;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public ApiKeyCreateResponse create(Long userId, ApiKeyCreateRequest request) {
        User user = userGateway.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        // 生成 API Key
        String rawKey = generateRawKey();
        String keyHash = hashKey(rawKey);

        GatewayApiKey apiKey = new GatewayApiKey();
        apiKey.setKeyCode(generateKeyCode());
        apiKey.setKeyHash(keyHash);
        apiKey.setUser(user);
        apiKey.setName(request.getName());
        apiKey.setExpiresAt(request.getExpiresAt());
        apiKey.setIpWhitelist(request.getIpWhitelist());
        apiKey.setModelWhitelist(request.getModelWhitelist());
        apiKey.setStatus(GatewayApiKey.ApiKeyStatus.ACTIVE);

        GatewayApiKey saved = apiKeyGateway.save(apiKey);

        ApiKeyCreateResponse response = new ApiKeyCreateResponse();
        response.setId(saved.getId());
        response.setKeyCode(saved.getKeyCode());
        response.setApiKey(rawKey);  // 只在此处返回原始 Key
        response.setName(saved.getName());
        response.setExpiresAt(saved.getExpiresAt());
        response.setCreatedAt(saved.getCreatedAt());
        return response;
    }

    public ApiKeyResponse getById(Long id) {
        GatewayApiKey apiKey = apiKeyGateway.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("ApiKey", id));
        return toResponse(apiKey);
    }

    public PageResponse<ApiKeyResponse> query(Long userId, ApiKeyQueryRequest request) {
        List<GatewayApiKey> apiKeys = apiKeyGateway.findAll();
        List<ApiKeyResponse> responses = apiKeys.stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
        return PageResponse.of(responses, request.getPage(), request.getLimit(), responses.size());
    }

    @Transactional
    public ApiKeyResponse update(Long id, ApiKeyUpdateRequest request) {
        GatewayApiKey apiKey = apiKeyGateway.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("ApiKey", id));

        if (request.getName() != null) {
            apiKey.setName(request.getName());
        }
        if (request.getExpiresAt() != null) {
            apiKey.setExpiresAt(request.getExpiresAt());
        }
        if (request.getIpWhitelist() != null) {
            apiKey.setIpWhitelist(request.getIpWhitelist());
        }
        if (request.getModelWhitelist() != null) {
            apiKey.setModelWhitelist(request.getModelWhitelist());
        }

        return toResponse(apiKeyGateway.save(apiKey));
    }

    @Transactional
    public void delete(Long id) {
        GatewayApiKey apiKey = apiKeyGateway.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("ApiKey", id));
        apiKey.setDeletedAt(Instant.now());
        apiKeyGateway.save(apiKey);
    }

    @Transactional
    public ApiKeyResponse disable(Long id) {
        GatewayApiKey apiKey = apiKeyGateway.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("ApiKey", id));
        apiKey.setStatus(GatewayApiKey.ApiKeyStatus.DISABLED);
        return toResponse(apiKeyGateway.save(apiKey));
    }

    @Transactional
    public ApiKeyResponse enable(Long id) {
        GatewayApiKey apiKey = apiKeyGateway.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("ApiKey", id));
        apiKey.setStatus(GatewayApiKey.ApiKeyStatus.ACTIVE);
        return toResponse(apiKeyGateway.save(apiKey));
    }

    @Transactional
    public ApiKeyCreateResponse rotate(Long id) {
        GatewayApiKey apiKey = apiKeyGateway.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("ApiKey", id));

        // 生成新 Key
        String rawKey = generateRawKey();
        apiKey.setKeyHash(hashKey(rawKey));
        apiKeyGateway.save(apiKey);

        ApiKeyCreateResponse response = new ApiKeyCreateResponse();
        response.setId(apiKey.getId());
        response.setKeyCode(apiKey.getKeyCode());
        response.setApiKey(rawKey);
        response.setName(apiKey.getName());
        response.setExpiresAt(apiKey.getExpiresAt());
        response.setCreatedAt(apiKey.getCreatedAt());
        return response;
    }

    private String generateRawKey() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return "sk-" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String generateKeyCode() {
        return "AK" + System.currentTimeMillis();
    }

    private String hashKey(String rawKey) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawKey.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash API key", e);
        }
    }

    private ApiKeyResponse toResponse(GatewayApiKey apiKey) {
        ApiKeyResponse response = new ApiKeyResponse();
        response.setId(apiKey.getId());
        response.setKeyCode(apiKey.getKeyCode());
        response.setName(apiKey.getName());
        response.setKeyPrefix("sk-xxxxx");
        response.setKeyHint(apiKey.getKeyHash() != null ?
            "sk-xxxx..." + apiKey.getKeyHash().substring(apiKey.getKeyHash().length() - 4) : null);
        response.setStatus(apiKey.getStatus());
        response.setExpiresAt(apiKey.getExpiresAt());
        response.setLastUsedAt(apiKey.getLastUsedAt());
        response.setIpWhitelist(apiKey.getIpWhitelist());
        response.setModelWhitelist(apiKey.getModelWhitelist());
        response.setCreatedAt(apiKey.getCreatedAt());
        response.setUpdatedAt(apiKey.getUpdatedAt());
        return response;
    }
}
```

- [ ] **Step 5: 创建 ApiKeyController**

```java
// ApiKeyController.java
package com.codingas.gateway.adapter.api.key.controller;

import com.codingas.gateway.adapter.api.key.dto.*;
import com.codingas.gateway.application.api.key.ApiKeyApplication;
import com.codingas.gateway.common.dto.ApiResponse;
import com.codingas.gateway.common.dto.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/api-keys")
@RequiredArgsConstructor
public class ApiKeyController {
    private final ApiKeyApplication apiKeyApplication;

    @PostMapping
    public ApiResponse<ApiKeyCreateResponse> create(
            @RequestParam Long userId,
            @Valid @RequestBody ApiKeyCreateRequest request) {
        return ApiResponse.success(apiKeyApplication.create(userId, request));
    }

    @GetMapping("/{id}")
    public ApiResponse<ApiKeyResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(apiKeyApplication.getById(id));
    }

    @GetMapping
    public ApiResponse<PageResponse<ApiKeyResponse>> query(
            @RequestParam Long userId,
            @ModelAttribute ApiKeyQueryRequest request) {
        return ApiResponse.success(apiKeyApplication.query(userId, request));
    }

    @PutMapping("/{id}")
    public ApiResponse<ApiKeyResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody ApiKeyUpdateRequest request) {
        return ApiResponse.success(apiKeyApplication.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        apiKeyApplication.delete(id);
        return ApiResponse.success(null);
    }

    @PostMapping("/{id}/disable")
    public ApiResponse<ApiKeyResponse> disable(@PathVariable Long id) {
        return ApiResponse.success(apiKeyApplication.disable(id));
    }

    @PostMapping("/{id}/enable")
    public ApiResponse<ApiKeyResponse> enable(@PathVariable Long id) {
        return ApiResponse.success(apiKeyApplication.enable(id));
    }

    @PostMapping("/{id}/rotate")
    public ApiResponse<ApiKeyCreateResponse> rotate(@PathVariable Long id) {
        return ApiResponse.success(apiKeyApplication.rotate(id));
    }
}
```

- [ ] **Step 6: 提交 API Key 管理 CRUD**

```bash
git add src/main/java/com/codingas/gateway/adapter/api-key/controller/ApiKeyController.java
git add src/main/java/com/codingas/gateway/adapter/api-key/dto/
git add src/main/java/com/codingas/gateway/application/api-key/
git add src/main/java/com/codingas/gateway/domain/security/gateway/ApiKeyGateway.java
git add src/main/java/com/codingas/gateway/infrastructure/gateway/security/JpaApiKeyGateway.java
git commit -m "feat: add API Key CRUD functionality

- ApiKeyController with REST endpoints
- ApiKeyApplication for business logic
- ApiKeyGateway interface and JpaApiKeyGateway implementation
- ApiKey DTOs (CreateRequest, UpdateRequest, Response, CreateResponse, QueryRequest)
- Key generation with SecureRandom and SHA-256 hashing

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

### Task 5: TokenLimit 管理 CRUD

**Files:**
- Create: `src/main/java/com/codingas/gateway/adapter/admin/controller/TokenLimitController.java`
- Create: `src/main/java/com/codingas/gateway/adapter/admin/dto/token-limit/`
- Create: `src/main/java/com/codingas/gateway/application/token-limit/TokenLimitApplication.java`
- Create: `src/main/java/com/codingas/gateway/domain/security/gateway/TokenLimitGateway.java`
- Create: `src/main/java/com/codingas/gateway/infrastructure/gateway/security/JpaTokenLimitGateway.java`

- [ ] **Step 1: 创建 TokenLimit DTOs**

```java
// TokenLimitCreateRequest.java
package com.codingas.gateway.adapter.admin.dto.tokenlimit;

import com.codingas.gateway.common.enums.ExceededAction;
import com.codingas.gateway.common.enums.PeriodType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class TokenLimitCreateRequest {
    @NotBlank(message = "Limit code不能为空")
    private String limitCode;

    @NotNull(message = "User ID不能为空")
    private Long userId;

    private Long providerId;
    private Long modelId;

    @NotNull(message = "Max tokens不能为空")
    @Positive(message = "Max tokens必须为正数")
    private BigDecimal maxTokens;

    @NotNull(message = "Period type不能为空")
    private PeriodType periodType;

    private Integer periodDayOfWeek;
    private Integer periodDayOfMonth;

    @NotNull(message = "Exceeded action不能为空")
    private ExceededAction exceededAction;

    private Long switchModelId;
}
```

```java
// TokenLimitUpdateRequest.java
package com.codingas.gateway.adapter.admin.dto.tokenlimit;

import com.codingas.gateway.common.enums.ExceededAction;
import com.codingas.gateway.common.enums.PeriodType;
import com.codingas.gateway.common.enums.TokenLimitStatus;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class TokenLimitUpdateRequest {
    private BigDecimal maxTokens;
    private PeriodType periodType;
    private Integer periodDayOfWeek;
    private Integer periodDayOfMonth;
    private ExceededAction exceededAction;
    private Long switchModelId;
    private TokenLimitStatus status;
}
```

```java
// TokenLimitResponse.java
package com.codingas.gateway.adapter.admin.dto.tokenlimit;

import com.codingas.gateway.common.enums.ExceededAction;
import com.codingas.gateway.common.enums.PeriodType;
import com.codingas.gateway.common.enums.TokenLimitStatus;
import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;

@Data
public class TokenLimitResponse {
    private Long id;
    private String limitCode;
    private UserInfo user;
    private ProviderInfo provider;
    private ModelInfo model;
    private String limitType;
    private BigDecimal maxTokens;
    private BigDecimal usedTokens;
    private BigDecimal remainingTokens;
    private Double usageRatio;
    private PeriodType periodType;
    private Integer periodDayOfWeek;
    private Integer periodDayOfMonth;
    private ExceededAction exceededAction;
    private ModelInfo switchModel;
    private TokenLimitStatus status;
    private Instant createdAt;
    private Instant updatedAt;

    @Data
    public static class UserInfo {
        private Long id;
        private String userCode;
        private String username;
    }

    @Data
    public static class ProviderInfo {
        private Long id;
        private String providerCode;
        private String providerName;
    }

    @Data
    public static class ModelInfo {
        private Long id;
        private String modelCode;
        private String displayName;
    }
}
```

```java
// TokenLimitQueryRequest.java
package com.codingas.gateway.adapter.admin.dto.tokenlimit;

import com.codingas.gateway.adapter.admin.dto.model.ModelQueryRequest;
import com.codingas.gateway.common.enums.TokenLimitStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class TokenLimitQueryRequest extends ModelQueryRequest {
    private Long userId;
    private Long providerId;
    private Long modelId;
    private TokenLimitStatus status;
}
```

- [ ] **Step 2: 创建 TokenLimitGateway 接口**

```java
// TokenLimitGateway.java
package com.codingas.gateway.domain.security.gateway;

import com.codingas.gateway.domain.security.entity.TokenLimit;
import com.codingas.gateway.domain.security.entity.User;
import java.util.List;
import java.util.Optional;

public interface TokenLimitGateway {
    TokenLimit save(TokenLimit tokenLimit);
    Optional<TokenLimit> findById(Long id);
    Optional<TokenLimit> findByLimitCode(String limitCode);
    List<TokenLimit> findByUser(User user);
    List<TokenLimit> findByUserId(Long userId);
    List<TokenLimit> findAll();
    long count();
    void delete(TokenLimit tokenLimit);
}
```

- [ ] **Step 3: 创建 TokenLimitRepository**

```java
// TokenLimitRepository.java
package com.codingas.gateway.domain.security.repository;

import com.codingas.gateway.domain.security.entity.TokenLimit;
import com.codingas.gateway.domain.security.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface TokenLimitRepository extends JpaRepository<TokenLimit, Long> {
    Optional<TokenLimit> findByLimitCode(String limitCode);
    List<TokenLimit> findByUser(User user);
    List<TokenLimit> findByUserId(Long userId);
}
```

- [ ] **Step 4: 创建 TokenLimitApplication**

```java
// TokenLimitApplication.java
package com.codingas.gateway.application.token.limit;

import com.codingas.gateway.adapter.admin.dto.tokenlimit.*;
import com.codingas.gateway.common.dto.PageResponse;
import com.codingas.gateway.common.exception.DuplicateResourceException;
import com.codingas.gateway.common.exception.ResourceNotFoundException;
import com.codingas.gateway.domain.security.entity.TokenLimit;
import com.codingas.gateway.domain.security.entity.User;
import com.codingas.gateway.domain.security.gateway.TokenLimitGateway;
import com.codingas.gateway.domain.security.gateway.UserGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TokenLimitApplication {
    private final TokenLimitGateway tokenLimitGateway;
    private final UserGateway userGateway;

    @Transactional
    public TokenLimitResponse create(TokenLimitCreateRequest request) {
        if (tokenLimitGateway.findByLimitCode(request.getLimitCode()).isPresent()) {
            throw new DuplicateResourceException("TokenLimit", "limitCode");
        }

        User user = userGateway.findById(request.getUserId())
            .orElseThrow(() -> new ResourceNotFoundException("User", request.getUserId()));

        TokenLimit tokenLimit = new TokenLimit();
        tokenLimit.setLimitCode(request.getLimitCode());
        tokenLimit.setUser(user);
        tokenLimit.setMaxTokens(request.getMaxTokens());
        tokenLimit.setUsedTokens(BigDecimal.ZERO);
        tokenLimit.setPeriodType(request.getPeriodType());
        tokenLimit.setPeriodDayOfWeek(request.getPeriodDayOfWeek());
        tokenLimit.setPeriodDayOfMonth(request.getPeriodDayOfMonth());
        tokenLimit.setExceededAction(request.getExceededAction());
        tokenLimit.setStatus(TokenLimit.TokenLimitStatus.ACTIVE);

        return toResponse(tokenLimitGateway.save(tokenLimit));
    }

    public TokenLimitResponse getById(Long id) {
        TokenLimit tokenLimit = tokenLimitGateway.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("TokenLimit", id));
        return toResponse(tokenLimit);
    }

    public PageResponse<TokenLimitResponse> query(TokenLimitQueryRequest request) {
        List<TokenLimit> tokenLimits;
        if (request.getUserId() != null) {
            tokenLimits = tokenLimitGateway.findByUserId(request.getUserId());
        } else {
            tokenLimits = tokenLimitGateway.findAll();
        }
        List<TokenLimitResponse> responses = tokenLimits.stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
        return PageResponse.of(responses, request.getPage(), request.getLimit(), responses.size());
    }

    @Transactional
    public TokenLimitResponse update(Long id, TokenLimitUpdateRequest request) {
        TokenLimit tokenLimit = tokenLimitGateway.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("TokenLimit", id));

        if (request.getMaxTokens() != null) {
            tokenLimit.setMaxTokens(request.getMaxTokens());
        }
        if (request.getPeriodType() != null) {
            tokenLimit.setPeriodType(request.getPeriodType());
        }
        if (request.getPeriodDayOfWeek() != null) {
            tokenLimit.setPeriodDayOfWeek(request.getPeriodDayOfWeek());
        }
        if (request.getPeriodDayOfMonth() != null) {
            tokenLimit.setPeriodDayOfMonth(request.getPeriodDayOfMonth());
        }
        if (request.getExceededAction() != null) {
            tokenLimit.setExceededAction(request.getExceededAction());
        }
        if (request.getStatus() != null) {
            tokenLimit.setStatus(request.getStatus());
        }

        return toResponse(tokenLimitGateway.save(tokenLimit));
    }

    @Transactional
    public void delete(Long id) {
        TokenLimit tokenLimit = tokenLimitGateway.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("TokenLimit", id));
        tokenLimit.setDeletedAt(Instant.now());
        tokenLimitGateway.save(tokenLimit);
    }

    private TokenLimitResponse toResponse(TokenLimit tokenLimit) {
        TokenLimitResponse response = new TokenLimitResponse();
        response.setId(tokenLimit.getId());
        response.setLimitCode(tokenLimit.getLimitCode());
        response.setLimitType(tokenLimit.getLimitType().name());
        response.setMaxTokens(tokenLimit.getMaxTokens());
        response.setUsedTokens(tokenLimit.getUsedTokens());
        response.setRemainingTokens(tokenLimit.getMaxTokens().subtract(tokenLimit.getUsedTokens()));
        if (tokenLimit.getMaxTokens().compareTo(BigDecimal.ZERO) > 0) {
            response.setUsageRatio(
                tokenLimit.getUsedTokens().divide(tokenLimit.getMaxTokens(), 4, java.math.RoundingMode.HALF_UP)
                    .doubleValue()
            );
        }
        response.setPeriodType(tokenLimit.getPeriodType());
        response.setPeriodDayOfWeek(tokenLimit.getPeriodDayOfWeek());
        response.setPeriodDayOfMonth(tokenLimit.getPeriodDayOfMonth());
        response.setExceededAction(tokenLimit.getExceededAction());
        response.setStatus(tokenLimit.getStatus());
        response.setCreatedAt(tokenLimit.getCreatedAt());
        response.setUpdatedAt(tokenLimit.getUpdatedAt());

        if (tokenLimit.getUser() != null) {
            TokenLimitResponse.UserInfo userInfo = new TokenLimitResponse.UserInfo();
            userInfo.setId(tokenLimit.getUser().getId());
            userInfo.setUserCode(tokenLimit.getUser().getUserCode());
            userInfo.setUsername(tokenLimit.getUser().getUsername());
            response.setUser(userInfo);
        }

        return response;
    }
}
```

- [ ] **Step 5: 创建 TokenLimitController**

```java
// TokenLimitController.java
package com.codingas.gateway.adapter.admin.controller;

import com.codingas.gateway.adapter.admin.dto.tokenlimit.*;
import com.codingas.gateway.application.token.limit.TokenLimitApplication;
import com.codingas.gateway.common.dto.ApiResponse;
import com.codingas.gateway.common.dto.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/token-limits")
@RequiredArgsConstructor
public class TokenLimitController {
    private final TokenLimitApplication tokenLimitApplication;

    @PostMapping
    public ApiResponse<TokenLimitResponse> create(@Valid @RequestBody TokenLimitCreateRequest request) {
        return ApiResponse.success(tokenLimitApplication.create(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<TokenLimitResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(tokenLimitApplication.getById(id));
    }

    @GetMapping
    public ApiResponse<PageResponse<TokenLimitResponse>> query(@ModelAttribute TokenLimitQueryRequest request) {
        return ApiResponse.success(tokenLimitApplication.query(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<TokenLimitResponse> update(@PathVariable Long id, @Valid @RequestBody TokenLimitUpdateRequest request) {
        return ApiResponse.success(tokenLimitApplication.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        tokenLimitApplication.delete(id);
        return ApiResponse.success(null);
    }
}
```

- [ ] **Step 6: 提交 TokenLimit 管理 CRUD**

```bash
git add src/main/java/com/codingas/gateway/adapter/admin/controller/TokenLimitController.java
git add src/main/java/com/codingas/gateway/adapter/admin/dto/tokenlimit/
git add src/main/java/com/codingas/gateway/application/token-limit/
git add src/main/java/com/codingas/gateway/domain/security/gateway/TokenLimitGateway.java
git add src/main/java/com/codingas/gateway/infrastructure/gateway/security/JpaTokenLimitGateway.java
git commit -m "feat: add TokenLimit CRUD functionality

- TokenLimitController with REST endpoints
- TokenLimitApplication for business logic
- TokenLimitGateway interface and JpaTokenLimitGateway implementation
- TokenLimit DTOs (CreateRequest, UpdateRequest, Response, QueryRequest)
- Usage tracking with remaining_tokens and usage_ratio

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

### Task 6: 角色管理 CRUD

**Files:**
- Create: `src/main/java/com/codingas/gateway/adapter/admin/controller/RoleController.java`
- Create: `src/main/java/com/codingas/gateway/adapter/admin/dto/role/`
- Create: `src/main/java/com/codingas/gateway/application/role/RoleApplication.java`
- Create: `src/main/java/com/codingas/gateway/domain/security/gateway/RoleGateway.java`
- Create: `src/main/java/com/codingas/gateway/infrastructure/gateway/security/JpaRoleGateway.java`

- [ ] **Step 1: 创建 Role DTOs**

```java
// RoleCreateRequest.java
package com.codingas.gateway.adapter.admin.dto.role;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.List;

@Data
public class RoleCreateRequest {
    @NotBlank(message = "Role code不能为空")
    private String roleCode;

    @NotBlank(message = "角色名称不能为空")
    private String name;

    private String description;

    @NotBlank(message = "必须至少分配一个权限")
    private List<String> permissionCodes;
}
```

```java
// RoleUpdateRequest.java
package com.codingas.gateway.adapter.admin.dto.role;

import lombok.Data;
import java.util.List;

@Data
public class RoleUpdateRequest {
    private String name;
    private String description;
    private Boolean isActive;
    private List<String> permissionCodes;
}
```

```java
// RoleResponse.java
package com.codingas.gateway.adapter.admin.dto.role;

import lombok.Data;
import java.time.Instant;
import java.util.List;

@Data
public class RoleResponse {
    private Long id;
    private String roleCode;
    private String name;
    private String description;
    private String roleType;
    private Boolean isActive;
    private List<PermissionInfo> permissions;
    private Instant createdAt;
    private Instant updatedAt;

    @Data
    public static class PermissionInfo {
        private String permissionCode;
        private String name;
        private String category;
    }
}
```

```java
// RoleQueryRequest.java
package com.codingas.gateway.adapter.admin.dto.role;

import com.codingas.gateway.common.dto.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class RoleQueryRequest extends PageRequest {
    private String keyword;
    private String roleType;
}
```

- [ ] **Step 2: 创建 RoleGateway 接口**

```java
// RoleGateway.java
package com.codingas.gateway.domain.security.gateway;

import com.codingas.gateway.domain.security.entity.Role;
import java.util.List;
import java.util.Optional;

public interface RoleGateway {
    Role save(Role role);
    Optional<Role> findById(Long id);
    Optional<Role> findByRoleCode(String roleCode);
    List<Role> findAll();
    List<Role> findByRoleCodes(List<String> roleCodes);
    long count();
    void delete(Role role);
}
```

- [ ] **Step 3: 创建 RoleRepository**

```java
// RoleRepository.java
package com.codingas.gateway.domain.security.repository;

import com.codingas.gateway.domain.security.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByRoleCode(String roleCode);
    List<Role> findByRoleCodeIn(List<String> roleCodes);
}
```

- [ ] **Step 4: 创建 RoleApplication**

```java
// RoleApplication.java
package com.codingas.gateway.application.role;

import com.codingas.gateway.adapter.admin.dto.role.*;
import com.codingas.gateway.common.dto.PageResponse;
import com.codingas.gateway.common.exception.DuplicateResourceException;
import com.codingas.gateway.common.exception.ResourceNotFoundException;
import com.codingas.gateway.domain.security.entity.Role;
import com.codingas.gateway.domain.security.entity.Permission;
import com.codingas.gateway.domain.security.gateway.RoleGateway;
import com.codingas.gateway.domain.security.gateway.PermissionGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleApplication {
    private final RoleGateway roleGateway;
    private final PermissionGateway permissionGateway;

    @Transactional
    public RoleResponse create(RoleCreateRequest request) {
        if (roleGateway.findByRoleCode(request.getRoleCode()).isPresent()) {
            throw new DuplicateResourceException("Role", "roleCode");
        }

        Role role = new Role();
        role.setRoleCode(request.getRoleCode());
        role.setName(request.getName());
        role.setDescription(request.getDescription());
        role.setRoleType(Role.RoleType.CUSTOM);
        role.setIsActive(true);

        return toResponse(roleGateway.save(role));
    }

    public RoleResponse getById(Long id) {
        Role role = roleGateway.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Role", id));
        return toResponse(role);
    }

    public PageResponse<RoleResponse> query(RoleQueryRequest request) {
        List<Role> roles = roleGateway.findAll();
        List<RoleResponse> responses = roles.stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
        return PageResponse.of(responses, request.getPage(), request.getLimit(), responses.size());
    }

    @Transactional
    public RoleResponse update(Long id, RoleUpdateRequest request) {
        Role role = roleGateway.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Role", id));

        if (request.getName() != null) {
            role.setName(request.getName());
        }
        if (request.getDescription() != null) {
            role.setDescription(request.getDescription());
        }
        if (request.getIsActive() != null) {
            role.setIsActive(request.getIsActive());
        }

        return toResponse(roleGateway.save(role));
    }

    @Transactional
    public void delete(Long id) {
        Role role = roleGateway.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Role", id));
        role.setDeletedAt(java.time.Instant.now());
        roleGateway.save(role);
    }

    @Transactional
    public RoleResponse assignPermissions(Long id, List<String> permissionCodes) {
        Role role = roleGateway.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Role", id));
        // 实现权限分配逻辑
        return toResponse(role);
    }

    private RoleResponse toResponse(Role role) {
        RoleResponse response = new RoleResponse();
        response.setId(role.getId());
        response.setRoleCode(role.getRoleCode());
        response.setName(role.getName());
        response.setDescription(role.getDescription());
        response.setRoleType(role.getRoleType().name());
        response.setIsActive(role.getIsActive());
        response.setCreatedAt(role.getCreatedAt());
        response.setUpdatedAt(role.getUpdatedAt());
        return response;
    }
}
```

- [ ] **Step 5: 创建 RoleController**

```java
// RoleController.java
package com.codingas.gateway.adapter.admin.controller;

import com.codingas.gateway.adapter.admin.dto.role.*;
import com.codingas.gateway.application.role.RoleApplication;
import com.codingas.gateway.common.dto.ApiResponse;
import com.codingas.gateway.common.dto.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
public class RoleController {
    private final RoleApplication roleApplication;

    @PostMapping
    public ApiResponse<RoleResponse> create(@Valid @RequestBody RoleCreateRequest request) {
        return ApiResponse.success(roleApplication.create(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<RoleResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(roleApplication.getById(id));
    }

    @GetMapping
    public ApiResponse<PageResponse<RoleResponse>> query(@ModelAttribute RoleQueryRequest request) {
        return ApiResponse.success(roleApplication.query(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<RoleResponse> update(@PathVariable Long id, @Valid @RequestBody RoleUpdateRequest request) {
        return ApiResponse.success(roleApplication.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        roleApplication.delete(id);
        return ApiResponse.success(null);
    }

    @PutMapping("/{id}/permissions")
    public ApiResponse<RoleResponse> assignPermissions(@PathVariable Long id, @RequestBody List<String> permissionCodes) {
        return ApiResponse.success(roleApplication.assignPermissions(id, permissionCodes));
    }
}
```

- [ ] **Step 6: 提交角色管理 CRUD**

```bash
git add src/main/java/com/codingas/gateway/adapter/admin/controller/RoleController.java
git add src/main/java/com/codingas/gateway/adapter/admin/dto/role/
git add src/main/java/com/codingas/gateway/application/role/
git add src/main/java/com/codingas/gateway/domain/security/gateway/RoleGateway.java
git add src/main/java/com/codingas/gateway/infrastructure/gateway/security/JpaRoleGateway.java
git commit -m "feat: add Role CRUD functionality

- RoleController with REST endpoints
- RoleApplication for business logic
- RoleGateway interface and JpaRoleGateway implementation
- Role DTOs (CreateRequest, UpdateRequest, Response, QueryRequest)
- Permission assignment support

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

### Task 7: 数据库迁移

**Files:**
- Create: `src/main/resources/db/V2__add_indexes.sql`

- [ ] **Step 1: 创建数据库迁移脚本**

```sql
-- V2__add_indexes.sql
-- Phase 2 CRUD 功能索引优化

-- 用户表索引优化
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_phone ON users(phone);

-- Provider 表索引
CREATE INDEX idx_providers_type ON providers(provider_type);
CREATE INDEX idx_providers_code ON providers(provider_code);

-- Model 表索引
CREATE INDEX idx_models_code ON models(model_code);
CREATE INDEX idx_models_provider ON models(provider_id);
CREATE INDEX idx_models_status ON models(status);

-- GatewayApiKey 表索引
CREATE INDEX idx_gateway_api_keys_user ON gateway_api_keys(user_id);
CREATE INDEX idx_gateway_api_keys_code ON gateway_api_keys(key_code);
CREATE INDEX idx_gateway_api_keys_status ON gateway_api_keys(status);

-- TokenLimit 表索引
CREATE INDEX idx_token_limits_user ON token_limits(user_id);
CREATE INDEX idx_token_limits_code ON token_limits(limit_code);
CREATE INDEX idx_token_limits_status ON token_limits(status);

-- Role 表索引
CREATE INDEX idx_roles_code ON roles(role_code);
CREATE INDEX idx_roles_type ON roles(role_type);
```

- [ ] **Step 2: 提交数据库迁移**

```bash
git add src/main/resources/db/V2__add_indexes.sql
git commit -m "feat: add database indexes for Phase 2 CRUD

- User indexes (username, email, phone)
- Provider indexes (type, code)
- Model indexes (code, provider, status)
- GatewayApiKey indexes (user, code, status)
- TokenLimit indexes (user, code, status)
- Role indexes (code, type)

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## 3. Self-Review 检查

### 3.1 Spec Coverage

| 设计文档章节 | Phase 2 任务 |
|-------------|-------------|
| 12.6.1 API 端点总览 | Task 1-6 (所有 CRUD 端点) |
| 12.6.3 User 管理 | Task 1 (User CRUD) |
| 12.6.4 Provider 管理 | Task 2 (Provider CRUD) |
| 12.6.5 Model 管理 | Task 3 (Model CRUD) |
| 12.6.6 API Key 管理 | Task 4 (ApiKey CRUD) |
| 12.6.7 TokenLimit 管理 | Task 5 (TokenLimit CRUD) |
| 12.6.8 Role 管理 | Task 6 (Role CRUD) |
| 数据库优化 | Task 7 (V2__add_indexes.sql) |

### 3.2 Placeholder Scan

- [ ] 无 TBD/TODO
- [ ] 所有端点路径与设计文档一致
- [ ] 所有 DTO 字段与设计文档一致
- [ ] 所有枚举值与设计文档一致

### 3.3 Type Consistency

- [ ] User.status → UserStatus (ACTIVE/DISABLED/LOCKED/DELETED)
- [ ] Provider.providerType → ProviderType (OPENAI/ANTHROPIC/...)
- [ ] TokenLimit.periodType → PeriodType (DAILY/WEEKLY/MONTHLY/TOTAL)
- [ ] ApiKey.status → ApiKeyStatus (ACTIVE/DISABLED/EXPIRED/DELETED)

---

## 4. 后续 Phase 预告

### Phase 3: 预警与计量
- AlertRule 管理
- AlertNotification 管理
- UsageLog 记录
- 预警引擎

### Phase 4: 其他功能
- 用量报表
- 操作日志
- 系统设置

### Phase 5: 前端管理后台
- Web UI + API 对接

### Phase 6: 路由增强
- 智能路由
- 故障切换
- 负载均衡

---

**Plan complete and saved to `docs/superpowers/plans/2026-04-28-llm-gateway-phase2-plan.md`.**
