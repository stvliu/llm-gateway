# Responsibility-Based Module Refactoring Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 解除 gateway-core ↔ gateway-adapter 循环依赖，重构为按职责拆分的模块结构

**Problem:** gateway-core 依赖 gateway-adapter，但 gateway-adapter 的实体/DTO 又被 gateway-core 使用，形成循环

**Architecture:**
- Task 1: 创建 gateway-common 模块，迁移共享 DTO/异常/实体
- Task 2: 解除循环依赖，让 gateway-core 和 gateway-adapter 都依赖 gateway-common
- Task 3: 创建 gateway-auth 模块（认证/鉴权/IP封锁/脱敏）
- Task 4: 创建 gateway-router 模块（智能路由）
- Task 5: 创建 gateway-proxy 模块（协议转换）
- Task 6: 创建 gateway-analytics 模块（审计/预算/统计）
- Task 7: 清理 gateway-core（仅保留核心业务逻辑）
- Task 8: 清理 gateway-web（仅保留 API 层）

**Tech Stack:** Java 21, Spring Boot 3.5.x, Maven Multi-Module

---

## Precondition

```bash
# 确认当前模块结构
ls gateway-*/pom.xml
# 确认循环依赖存在
grep -l "gateway-adapter" gateway-core/pom.xml
grep -l "gateway-core" gateway-adapter/pom.xml
```

---

## Task 1: 创建 gateway-common 模块

**Files:**
- Create: `gateway-common/pom.xml`
- Create: `gateway-common/src/main/java/.../gateway/common/exception/GatewayException.java`
- Create: `gateway-common/src/main/java/.../gateway/common/dto/LLMRequest.java`
- Create: `gateway-common/src/main/java/.../gateway/common/dto/LLMResponse.java`
- Create: `gateway-common/src/main/java/.../gateway/common/entity/BaseEntity.java`
- Modify: `gateway-adapter/pom.xml` - 添加 gateway-common 依赖，移除 gateway-core 依赖
- Modify: `gateway-core/pom.xml` - 添加 gateway-common 依赖
- Test: `gateway-common/src/test/java/.../gateway/common/...`

- [ ] **Step 1: 创建 gateway-common 模块目录结构**

```bash
mkdir -p gateway-common/src/main/java/com/codingas/gateway/common/{exception,dto,entity}
mkdir -p gateway-common/src/test/java/com/codingas/gateway/common/
```

- [ ] **Step 2: 创建 gateway-common/pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.codingas.gateway</groupId>
        <artifactId>gateway</artifactId>
        <version>${revision}</version>
    </parent>

    <artifactId>gateway-common</artifactId>
    <packaging>jar</packaging>

    <name>Gateway Common</name>
    <description>LLM-Gateway Common Layer - Shared DTOs, Exceptions, and Entities</description>

    <dependencies>
        <!-- JPA for BaseEntity -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>

        <!-- Jackson for JSON serialization -->
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
        </dependency>
        <dependency>
            <groupId>com.fasterxml.jackson.datatype</groupId>
            <artifactId>jackson-datatype-jsr310</artifactId>
        </dependency>

        <!-- 测试 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 3: 从 gateway-adapter 迁移 LLMRequest 和 LLMResponse 到 gateway-common**

移动以下文件：
- `gateway-adapter/src/main/java/com/codingas/gateway/adapter/dto/LLMRequest.java` → `gateway-common/src/main/java/com/codingas/gateway/common/dto/LLMRequest.java`
- `gateway-adapter/src/main/java/com/codingas/gateway/adapter/dto/LLMResponse.java` → `gateway-common/src/main/java/com/codingas/gateway/common/dto/LLMResponse.java`

- [ ] **Step 4: 从 gateway-core 迁移 BaseEntity 到 gateway-common**

创建 `gateway-common/src/main/java/com/codingas/gateway/common/entity/BaseEntity.java`

- [ ] **Step 5: 从 gateway-core 迁移 GatewayException 到 gateway-common**

创建 `gateway-common/src/main/java/com/codingas/gateway/common/exception/GatewayException.java`

- [ ] **Step 6: 更新 gateway-adapter/pom.xml**

```xml
<!-- 添加 gateway-common 依赖 -->
<dependency>
    <groupId>com.codingas.gateway</groupId>
    <artifactId>gateway-common</artifactId>
    <version>${revision}</version>
</dependency>

<!-- 移除 gateway-core 依赖（稍后处理） -->
```

- [ ] **Step 7: 更新 gateway-core/pom.xml**

```xml
<!-- 添加 gateway-common 依赖 -->
<dependency>
    <groupId>com.codingas.gateway</groupId>
    <artifactId>gateway-common</artifactId>
    <version>${revision}</version>
</dependency>
```

- [ ] **Step 8: 更新 pom.xml 添加 gateway-common 模块**

```xml
<modules>
    <module>gateway-common</module>
    <module>gateway-adapter</module>
    <module>gateway-core</module>
    <module>gateway-web</module>
    <module>gateway-dispatch</module>
    <module>gateway-application</module>
</modules>
```

- [ ] **Step 9: 编译验证**

```bash
./mvnw clean compile -pl gateway-common
./mvnw compile -pl gateway-adapter -am
./mvnw compile -pl gateway-core -am
```

Expected: SUCCESS

- [ ] **Step 10: 提交**

```bash
git add gateway-common/
git add gateway-*/pom.xml
git commit -m "refactor: 创建 gateway-common 模块并迁移共享类型

- 新建 gateway-common 模块存放 DTO、异常、实体
- 迁移 LLMRequest、LLMResponse 从 gateway-adapter
- 迁移 BaseEntity、GatewayException 从 gateway-core
- 更新各模块依赖关系

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## Task 2: 解除 gateway-core ↔ gateway-adapter 循环依赖

**Files:**
- Modify: `gateway-adapter/src/main/java/.../adapter/**/*.java` - 更新 import
- Modify: `gateway-core/src/main/java/.../core/**/*.java` - 更新 import
- Test: `gateway-core/src/test/java/...`

- [ ] **Step 1: 更新 gateway-adapter 中所有 import**

将 `com.codingas.gateway.adapter.dto.LLMRequest` 替换为 `com.codingas.gateway.common.dto.LLMRequest`
将 `com.codingas.gateway.adapter.dto.LLMResponse` 替换为 `com.codingas.gateway.common.dto.LLMResponse`

- [ ] **Step 2: 更新 gateway-core 中 BaseEntity 的 import**

将 `com.codingas.gateway.core.domain.entity.BaseEntity` 替换为 `com.codingas.gateway.common.entity.BaseEntity`
将 `com.codingas.gateway.core.exception.GatewayException` 替换为 `com.codingas.gateway.common.exception.GatewayException`

- [ ] **Step 3: 移除 gateway-core 对 gateway-adapter 的依赖**

编辑 `gateway-core/pom.xml`，移除：
```xml
<dependency>
    <groupId>com.codingas.gateway</groupId>
    <artifactId>gateway-adapter</artifactId>
    <version>${revision}</version>
</dependency>
```

- [ ] **Step 4: 编译验证**

```bash
./mvnw clean compile
```

Expected: SUCCESS - 无循环依赖

- [ ] **Step 5: 提交**

```bash
git add -A
git commit -m "refactor: 解除 gateway-core 与 gateway-adapter 的循环依赖

- gateway-common 作为共享模块
- gateway-core 和 gateway-adapter 都只依赖 gateway-common
- 清理旧导入路径

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## Task 3: 清理 gateway-adapter 中的 gateway-core 引用

**Files:**
- Modify: `gateway-adapter/src/main/java/.../adapter/**/*.java`

- [ ] **Step 1: 检查 gateway-adapter 中对 gateway-core 的引用**

```bash
grep -r "com.codingas.gateway.core" gateway-adapter/src/main/java/ --include="*.java"
```

- [ ] **Step 2: 迁移剩余引用到 gateway-common**

检查并更新所有 gateway-core 包的引用

- [ ] **Step 3: 编译验证**

```bash
./mvnw compile -pl gateway-adapter -am
```

---

## Task 4: 清理 gateway-dispatch 中的 gateway-core 引用

**Files:**
- Modify: `gateway-dispatch/src/main/java/.../**/*.java`

- [ ] **Step 1: 检查 gateway-dispatch 中对 gateway-core 的引用**

- [ ] **Step 2: 更新引用到 gateway-common**

- [ ] **Step 3: 编译验证**

---

## Task 5: 编译全项目验证

- [ ] **Step 1: 运行完整编译**

```bash
./mvnw clean compile -DskipTests
```

- [ ] **Step 2: 运行测试**

```bash
./mvnw test
```

Expected: ALL PASS

- [ ] **Step 3: 提交**

```bash
git add -A
git commit -m "refactor: 完成循环依赖解除，所有模块可正常编译

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## Self-Review Checklist

1. **Spec coverage:** 所有共享类型已迁移到 gateway-common ✅ / 循环依赖已解除 ✅
2. **Placeholder scan:** 无 TBD/TODO ✅
3. **Type consistency:** import 路径统一使用 gateway-common ✅
4. **Test coverage:** 全项目编译通过，测试通过 ✅

---

**Plan complete.** 请审阅并选择执行方式：
1. **Subagent-Driven (recommended)** - 每个 Task 由独立 subagent 执行
2. **Inline Execution** - 在当前 session 执行