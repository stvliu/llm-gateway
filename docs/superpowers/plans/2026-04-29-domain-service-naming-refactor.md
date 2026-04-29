# Domain Service 命名重构计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 Domain 层所有服务类重命名为 `XxxDomainService` 模式，与 Application 层的 `XxxService` 模式区分

**Architecture:** Domain 服务类 + 后缀 DomainService，Application 服务保持 XxxService。需同步更新所有引用（包括测试文件、文档）。

**Tech Stack:** Java 21, Spring Boot 3.5.x, JPA

---

## 影响范围

### Domain 层（需重命名，13个文件）
```
src/main/java/com/codingas/gateway/domain/security/service/
├── AuthenticationService.java           → AuthenticationDomainService.java
├── RateLimitService.java               → RateLimitDomainService.java
├── RbacService.java                    → RbacDomainService.java
├── AuditService.java                   → AuditDomainService.java
├── ApiKeyEncryptionService.java        → ApiKeyEncryptionDomainService.java
├── BruteForceProtectionService.java    → BruteForceProtectionDomainService.java
├── NotificationService.java            → NotificationDomainService.java
├── DefaultNotificationService.java     → DefaultNotificationDomainService.java
└── IpBlocklistService.java             → IpBlocklistDomainService.java

src/main/java/com/codingas/gateway/domain/router/service/
├── ModelRouterService.java             → ModelRouterDomainService.java
├── ModelService.java                   → ModelDomainService.java
└── ProviderService.java               → ProviderDomainService.java
```

### Application 层（命名良好，无需修改）
```
src/main/java/com/codingas/gateway/application/
├── auth/AuthService.java
├── auth/AuthServiceImpl.java
├── chat/ChatService.java
├── chat/ChatServiceImpl.java
├── apikey/ApiKeyService.java
├── apikey/ApiKeyServiceImpl.java
├── model/ModelService.java
├── model/ModelServiceImpl.java
├── provider/ProviderService.java
├── provider/ProviderServiceImpl.java
├── role/RoleService.java
├── role/RoleServiceImpl.java
├── tokenlimit/TokenLimitService.java
├── tokenlimit/TokenLimitServiceImpl.java
├── user/UserService.java
└── user/UserServiceImpl.java
```

---

## Task 1: 重命名 security domain 服务（9个文件）

**Files:**
- Modify: `src/main/java/com/codingas/gateway/domain/security/service/AuthenticationService.java`
- Modify: `src/main/java/com/codingas/gateway/domain/security/service/RateLimitService.java`
- Modify: `src/main/java/com/codingas/gateway/domain/security/service/RbacService.java`
- Modify: `src/main/java/com/codingas/gateway/domain/security/service/AuditService.java`
- Modify: `src/main/java/com/codingas/gateway/domain/security/service/ApiKeyEncryptionService.java`
- Modify: `src/main/java/com/codingas/gateway/domain/security/service/BruteForceProtectionService.java`
- Modify: `src/main/java/com/codingas/gateway/domain/security/service/NotificationService.java`
- Modify: `src/main/java/com/codingas/gateway/domain/security/service/DefaultNotificationService.java`
- Modify: `src/main/java/com/codingas/gateway/domain/security/service/IpBlocklistService.java`
- Grep: `src/test/` (查找所有引用这些服务的地方)

- [ ] **Step 1: 备份当前文件列表**

```bash
cd /mnt/e/workspace/llm-gateway
git status --short src/main/java/com/codingas/gateway/domain/security/service/
```

- [ ] **Step 2: 重命名 AuthenticationService.java → AuthenticationDomainService.java**

```bash
cd /mnt/e/workspace/llm-gateway
git mv src/main/java/com/codingas/gateway/domain/security/service/AuthenticationService.java \
       src/main/java/com/codingas/gateway/domain/security/service/AuthenticationDomainService.java
```

- [ ] **Step 3: 重命名 RateLimitService.java → RateLimitDomainService.java**

```bash
git mv src/main/java/com/codingas/gateway/domain/security/service/RateLimitService.java \
       src/main/java/com/codingas/gateway/domain/security/service/RateLimitDomainService.java
```

- [ ] **Step 4: 重命名 RbacService.java → RbacDomainService.java**

```bash
git mv src/main/java/com/codingas/gateway/domain/security/service/RbacService.java \
       src/main/java/com/codingas/gateway/domain/security/service/RbacDomainService.java
```

- [ ] **Step 5: 重命名 AuditService.java → AuditDomainService.java**

```bash
git mv src/main/java/com/codingas/gateway/domain/security/service/AuditService.java \
       src/main/java/com/codingas/gateway/domain/security/service/AuditDomainService.java
```

- [ ] **Step 6: 重命名 ApiKeyEncryptionService.java → ApiKeyEncryptionDomainService.java**

```bash
git mv src/main/java/com/codingas/gateway/domain/security/service/ApiKeyEncryptionService.java \
       src/main/java/com/codingas/gateway/domain/security/service/ApiKeyEncryptionDomainService.java
```

- [ ] **Step 7: 重命名 BruteForceProtectionService.java → BruteForceProtectionDomainService.java**

```bash
git mv src/main/java/com/codingas/gateway/domain/security/service/BruteForceProtectionService.java \
       src/main/java/com/codingas/gateway/domain/security/service/BruteForceProtectionDomainService.java
```

- [ ] **Step 8: 重命名 NotificationService.java → NotificationDomainService.java**

```bash
git mv src/main/java/com/codingas/gateway/domain/security/service/NotificationService.java \
       src/main/java/com/codingas/gateway/domain/security/service/NotificationDomainService.java
```

- [ ] **Step 9: 重命名 DefaultNotificationService.java → DefaultNotificationDomainService.java**

```bash
git mv src/main/java/com/codingas/gateway/domain/security/service/DefaultNotificationService.java \
       src/main/java/com/codingas/gateway/domain/security/service/DefaultNotificationDomainService.java
```

- [ ] **Step 10: 重命名 IpBlocklistService.java → IpBlocklistDomainService.java**

```bash
git mv src/main/java/com/codingas/gateway/domain/security/service/IpBlocklistService.java \
       src/main/java/com/codingas/gateway/domain/security/service/IpBlocklistDomainService.java
```

- [ ] **Step 11: 更新类名（逐一修改每个文件内的 class 声明）**

对每个重命名的文件，修改类声明：
```java
// AuthenticationService.java → AuthenticationDomainService.java
public class AuthenticationDomainService {
```

- [ ] **Step 12: 搜索并更新所有引用**

```bash
# 查找所有引用
grep -r "AuthenticationService\|RateLimitService\|RbacService\|AuditService\|ApiKeyEncryptionService\|BruteForceProtectionService\|NotificationService\|DefaultNotificationService\|IpBlocklistService" \
  src/main/java src/test/java --include="*.java" -l
```

- [ ] **Step 13: 更新引用（使用 sed 批量替换）**

```bash
# 批量替换（每个服务逐一处理）
sed -i 's/AuthenticationService/AuthenticationDomainService/g' \
  $(grep -rl "AuthenticationService" src/ --include="*.java")
# ... 对其他服务重复
```

- [ ] **Step 14: 验证编译**

```bash
cd /mnt/e/workspace/llm-gateway
./mvnw compile -q 2>&1 | head -50
```

- [ ] **Step 15: 提交变更**

```bash
git add -A
git commit -m "refactor: rename security domain services to *DomainService suffix"
```

---

## Task 2: 重命名 router domain 服务（3个文件）

**Files:**
- Modify: `src/main/java/com/codingas/gateway/domain/router/service/ModelRouterService.java`
- Modify: `src/main/java/com/codingas/gateway/domain/router/service/ModelService.java`
- Modify: `src/main/java/com/codingas/gateway/domain/router/service/ProviderService.java`

- [ ] **Step 1: 重命名 ModelRouterService.java → ModelRouterDomainService.java**

```bash
cd /mnt/e/workspace/llm-gateway
git mv src/main/java/com/codingas/gateway/domain/router/service/ModelRouterService.java \
       src/main/java/com/codingas/gateway/domain/router/service/ModelRouterDomainService.java
```

- [ ] **Step 2: 重命名 ModelService.java → ModelDomainService.java**

```bash
git mv src/main/java/com/codingas/gateway/domain/router/service/ModelService.java \
       src/main/java/com/codingas/gateway/domain/router/service/ModelDomainService.java
```

- [ ] **Step 3: 重命名 ProviderService.java → ProviderDomainService.java**

```bash
git mv src/main/java/com/codingas/gateway/domain/router/service/ProviderService.java \
       src/main/java/com/codingas/gateway/domain/router/service/ProviderDomainService.java
```

- [ ] **Step 4: 更新类名（逐一修改每个文件内的 class 声明）**

```java
// ModelRouterService.java → ModelRouterDomainService.java
public class ModelRouterDomainService {
```

- [ ] **Step 5: 搜索并更新所有引用**

```bash
grep -r "ModelRouterService\|ModelService\|ProviderService" \
  src/main/java src/test/java --include="*.java" -l
```

注意：`ProviderService` 与 Application 层的 `ProviderService` 重名，需要确认是否都要重命名。

**重要**：Domain 层 `ProviderService` 重命名为 `ProviderDomainService`，Application 层保持 `ProviderService`。

- [ ] **Step 6: 验证编译**

```bash
./mvnw compile -q 2>&1 | head -50
```

- [ ] **Step 7: 提交变更**

```bash
git add -A
git commit -m "refactor: rename router domain services to *DomainService suffix"
```

---

## Task 3: 更新测试文件中的引用

**Files:**
- Modify: `src/test/java/...` (所有引用了重命名服务的测试文件)

- [ ] **Step 1: 查找所有测试文件中的引用**

```bash
grep -r "AuthenticationDomainService\|RateLimitDomainService\|RbacDomainService\|AuditDomainService\|ApiKeyEncryptionDomainService\|BruteForceProtectionDomainService\|NotificationDomainService\|DefaultNotificationDomainService\|IpBlocklistDomainService\|ModelRouterDomainService\|ModelDomainService\|ProviderDomainService" \
  src/test --include="*.java" -l
```

- [ ] **Step 2: 验证测试编译**

```bash
./mvnw test-compile -q 2>&1 | head -50
```

- [ ] **Step 3: 运行测试验证**

```bash
./mvnw test -q 2>&1 | tail -30
```

- [ ] **Step 4: 提交**

```bash
git add -A
git commit -m "test: update test references to domain services"
```

---

## Task 4: 更新文档（如果需要）

**Files:**
- Modify: `docs/constitution.md` (已更新，见 Task 5.3)
- Modify: `docs/spec.md` (已更新，见 7.2 服务层命名规范)

---

## Task 5: 最终验证

- [ ] **Step 1: 完整编译**

```bash
./mvnw clean compile -q 2>&1
```

Expected: 无错误

- [ ] **Step 2: 完整测试**

```bash
./mvnw test -q 2>&1 | tail -30
```

Expected: 所有测试通过

- [ ] **Step 3: 检查是否有遗漏的旧名称**

```bash
grep -r "Service\.class" src/main/java/com/codingas/gateway/domain/ --include="*.java" | grep -v DomainService
```

Expected: 无结果

- [ ] **Step 4: 提交最终变更**

```bash
git add -A
git commit -m "refactor: complete domain service naming convention (*DomainService)"
```

---

## 注意事项

1. **使用 git mv**：保持 git history 完整
2. **批量处理**：每组服务（security/router）统一提交
3. **先编译再测试**：确保没有遗漏的引用
4. **检查 Application 层**：确保没有意外引用 Domain 服务